package com.henriquesebastiao.prumo.feature.reading.data

import com.henriquesebastiao.prumo.core.network.testing.OpenApiContract
import com.henriquesebastiao.prumo.feature.reading.model.BookStatus
import org.junit.Assert.assertEquals
import org.junit.Test

/** The hand-written DTOs of Livros against the server's openapi.json (D04). */
class ReadingContractTest {
    @Test
    fun theReadDtosFollowTheContract() {
        OpenApiContract.assertReads(BookDto.serializer().descriptor, "BookRead")
        OpenApiContract.assertReads(BookDetailDto.serializer().descriptor, "BookDetail")
        OpenApiContract.assertReads(WeekPagesDto.serializer().descriptor, "WeekPages")
        OpenApiContract.assertReads(NoteDto.serializer().descriptor, "NoteRead")
    }

    @Test
    fun theWriteDtosFollowTheContract() {
        OpenApiContract.assertWrites(BookCreateDto.serializer().descriptor, "BookCreate")
        OpenApiContract.assertWrites(BookUpdateDto.serializer().descriptor, "BookUpdate")
        OpenApiContract.assertWrites(ProgressCreateDto.serializer().descriptor, "ProgressCreate")
        OpenApiContract.assertWrites(NoteCreateDto.serializer().descriptor, "NoteCreate")
        OpenApiContract.assertWrites(NoteUpdateDto.serializer().descriptor, "NoteUpdate")
    }

    @Test
    fun theStatusesAreTheServers() {
        assertEquals(OpenApiContract.enumValues("BookStatus"), BookStatus.entries.map { it.wire }.toSet())
    }
}
