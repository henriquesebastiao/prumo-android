package com.henriquesebastiao.prumo.core.network

import com.henriquesebastiao.prumo.core.model.SessionClient
import com.henriquesebastiao.prumo.core.model.ThemeMode
import com.henriquesebastiao.prumo.core.network.dto.ActivityItemDto
import com.henriquesebastiao.prumo.core.network.dto.DashboardDto
import com.henriquesebastiao.prumo.core.network.dto.ErrorBodyDto
import com.henriquesebastiao.prumo.core.network.dto.ErrorFieldDto
import com.henriquesebastiao.prumo.core.network.dto.ErrorResponseDto
import com.henriquesebastiao.prumo.core.network.dto.HealthDto
import com.henriquesebastiao.prumo.core.network.dto.LoginRequestDto
import com.henriquesebastiao.prumo.core.network.dto.LoginResponseDto
import com.henriquesebastiao.prumo.core.network.dto.ModuleDto
import com.henriquesebastiao.prumo.core.network.dto.ModuleListDto
import com.henriquesebastiao.prumo.core.network.dto.PasswordChangeDto
import com.henriquesebastiao.prumo.core.network.dto.PreferencesDto
import com.henriquesebastiao.prumo.core.network.dto.PreferencesUpdateDto
import com.henriquesebastiao.prumo.core.network.dto.SessionDto
import com.henriquesebastiao.prumo.core.network.dto.SessionListDto
import com.henriquesebastiao.prumo.core.network.dto.UpcomingItemDto
import com.henriquesebastiao.prumo.core.network.dto.UserDto
import com.henriquesebastiao.prumo.core.network.dto.UserUpdateDto
import com.henriquesebastiao.prumo.core.network.testing.OpenApiContract
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The hand-written DTOs against the server's openapi.json (docs/decisoes.md, D04). */
class ContractTest {
    @Test
    fun responsesTheAppReads() {
        OpenApiContract.assertReads(HealthDto.serializer().descriptor, "Health")
        OpenApiContract.assertReads(LoginResponseDto.serializer().descriptor, "LoginResponse")
        OpenApiContract.assertReads(UserDto.serializer().descriptor, "UserRead")
        OpenApiContract.assertReads(PreferencesDto.serializer().descriptor, "PreferencesRead")
        OpenApiContract.assertReads(ModuleDto.serializer().descriptor, "ModuleRead")
        OpenApiContract.assertReads(ModuleListDto.serializer().descriptor, "ModuleList")
        OpenApiContract.assertReads(SessionDto.serializer().descriptor, "SessionRead")
        OpenApiContract.assertReads(SessionListDto.serializer().descriptor, "SessionList")
        OpenApiContract.assertReads(DashboardDto.serializer().descriptor, "DashboardRead")
        OpenApiContract.assertReads(UpcomingItemDto.serializer().descriptor, "UpcomingItem")
        OpenApiContract.assertReads(ActivityItemDto.serializer().descriptor, "ActivityItem")
        OpenApiContract.assertReads(ErrorResponseDto.serializer().descriptor, "ErrorResponse")
        OpenApiContract.assertReads(ErrorBodyDto.serializer().descriptor, "ErrorBody")
        OpenApiContract.assertReads(ErrorFieldDto.serializer().descriptor, "ErrorField")
    }

    @Test
    fun requestsTheAppSends() {
        OpenApiContract.assertWrites(LoginRequestDto.serializer().descriptor, "LoginRequest")
        OpenApiContract.assertWrites(UserUpdateDto.serializer().descriptor, "UserUpdate")
        OpenApiContract.assertWrites(PasswordChangeDto.serializer().descriptor, "PasswordChange")
        OpenApiContract.assertWrites(PreferencesUpdateDto.serializer().descriptor, "PreferencesUpdate")
    }

    @Test
    fun theAppKnowsEveryValueOfTheEnumsItShows() {
        assertEquals(ThemeMode.entries.map { it.wire }.toSet(), OpenApiContract.enumValues("Theme"))
        assertEquals(SessionClient.entries.map { it.wire }.toSet(), OpenApiContract.enumValues("SessionClient"))
    }

    @Test
    fun theHealthNamesTheService() {
        val service = OpenApiContract.schema("Health")["properties"]!!.let {
            it as kotlinx.serialization.json.JsonObject
        }["service"]
        assertEquals(JsonPrimitive("prumo"), (service as kotlinx.serialization.json.JsonObject)["const"])
        assertTrue("service" in OpenApiContract.required("Health"))
    }

    @Test
    fun theHomeIsAUnionOfTheModulesTheAppKnows() {
        val schemas = OpenApiContract.properties("DashboardRead")
        assertTrue("modules" in schemas)
        // Each module's summary is checked by its :feature:* against these schemas.
        listOf("FinanceSummary", "GarageSummary", "ReadingSummary").forEach { OpenApiContract.schema(it) }
    }
}
