package com.henriquesebastiao.prumo.feature.garage

import com.henriquesebastiao.prumo.core.network.NetworkJson
import com.henriquesebastiao.prumo.core.network.decodeOrNull
import com.henriquesebastiao.prumo.core.network.testing.Fixtures
import com.henriquesebastiao.prumo.core.network.testing.OpenApiContract
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GarageSummaryTest {
    private fun summary(): JsonObject = NetworkJson.parseToJsonElement(Fixtures.read("dashboard.json"))
        .jsonObject["modules"]!!.jsonArray
        .map { it.jsonObject }
        .first { it["module"]!!.jsonPrimitive.content == GarageFeature.ID }

    @Test
    fun theRealSummaryDecodes() {
        val decoded = decodeOrNull(GarageSummaryDto.serializer(), summary())!!
        assertEquals(2, decoded.vehiclesCount)
        val next = decoded.nextDue!!
        assertEquals("Honda CG 160 Fan", next.vehicle.name)
        assertEquals(220L, next.remainingKm)
        assertEquals("km", next.primary)
    }

    @Test
    fun theDtosFollowTheContract() {
        OpenApiContract.assertReads(GarageSummaryDto.serializer().descriptor, "GarageSummary")
        OpenApiContract.assertReads(NextDueDto.serializer().descriptor, "NextDue")
        OpenApiContract.assertReads(VehicleRefDto.serializer().descriptor, "VehicleRef")
    }

    @Test
    fun aSummaryThatDoesNotFitIsNull() {
        assertNull(decodeOrNull(GarageSummaryDto.serializer(), JsonObject(emptyMap())))
    }
}
