package com.henriquesebastiao.prumo.core.data.repository

import android.app.Application
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.henriquesebastiao.prumo.core.model.AppError
import com.henriquesebastiao.prumo.core.network.ActiveSession
import com.henriquesebastiao.prumo.core.network.ApiFactory
import com.henriquesebastiao.prumo.core.network.SessionGateway
import com.henriquesebastiao.prumo.core.network.testing.NoCache
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** "Exportar dados" against a fake server: the archive goes into the file the user chose. */
@Config(application = Application::class)
@RunWith(AndroidJUnit4::class)
class DataExportRepositoryTest {
    private val server = MockWebServer()
    private val context = RuntimeEnvironment.getApplication()
    private val target = Uri.parse("content://com.android.externalstorage.documents/document/primary%3Aprumo.zip")

    @Before
    fun setUp() = server.start()

    @After
    fun tearDown() = server.close()

    private fun repository(dispatcher: kotlinx.coroutines.CoroutineDispatcher): DataExportRepository {
        val gateway = object : SessionGateway {
            override val activeSession = ActiveSession(server.url("/").toString().trimEnd('/'), "token")

            override fun onUnauthorized(token: String) = Unit
        }
        return DefaultDataExportRepository(
            context,
            ServerCalls(ApiFactory(OkHttpClient()), gateway, NoCache),
            dispatcher,
        )
    }

    @Test
    fun theArchiveIsWrittenIntoTheChosenFile() = runTest {
        val archive = byteArrayOf(0x50, 0x4b, 0x03, 0x04, 1, 2, 3)
        server.enqueue(
            MockResponse.Builder()
                .addHeader("Content-Type", "application/zip")
                .body(Buffer().write(archive))
                .build(),
        )
        val file = ByteArrayOutputStream()
        shadowOf(context.contentResolver).registerOutputStream(target, file)

        val result = repository(StandardTestDispatcher(testScheduler)).export(target)

        assertEquals(ExportResult.Saved, result)
        assertArrayEquals(archive, file.toByteArray())
        val request = server.takeRequest()
        assertEquals("/api/v1/me/export", request.url.encodedPath)
    }

    @Test
    fun anErrorOfTheServerIsItsCode() = runTest {
        server.enqueue(
            MockResponse.Builder()
                .code(500)
                .addHeader("Content-Type", "application/json")
                .body("""{"error":{"code":"internal_error","message":"boom"}}""")
                .build(),
        )

        val result = repository(StandardTestDispatcher(testScheduler)).export(target)

        assertEquals("internal_error", ((result as ExportResult.Failed).error as AppError.Api).code)
    }
}
