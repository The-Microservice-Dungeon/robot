package com.msd.testUtil

import com.msd.application.EventType
import com.msd.application.dto.BlockEventDTO
import com.msd.application.dto.MovementEventDTO
import com.msd.domain.DomainEvent
import com.msd.planet.application.PlanetDTO
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertAll
import java.util.*

class EventChecker {

    fun checkHeaders(
        expectedTransactionId: UUID,
        expectedEventType: EventType,
        domainEvent: DomainEvent<*>
    ) {
        assertAll(
            "Check header correct",
            {
                assertEquals(expectedTransactionId.toString(), domainEvent.transactionId)
            },
            {
                assertEquals(expectedEventType.eventString, domainEvent.type)
            }
        )
    }

    fun checkMovementPaylod(
        expectedSuccess: Boolean,
        expectedMessage: String,
        expectedRemainingEnergy: Int?,
        expectedPlanetDTO: PlanetDTO?,
        expectedRobots: List<UUID>,
        payload: MovementEventDTO
    ) {
        assertAll(
            "assert movement payload correct",
            {
                assertEquals(expectedSuccess, payload.success)
            },
            {
                assertEquals(expectedMessage, payload.message)
            },
            {
                assertEquals(expectedRemainingEnergy, payload.remainingEnergy)
            },
            {
                assertEquals(expectedPlanetDTO, payload.planet)
            },
            {
                assertEquals(expectedRobots, payload.robots)
            }
        )
    }

    fun checkBlockPayload(expectedSuccess: Boolean, expectedMessage: String ,expectedPlanetId: UUID?, expectedRemainingEnergy: Int?, payload: BlockEventDTO) {
        assertAll("assert blocking payload correct",
            {
                assertEquals(expectedSuccess, payload.success)
            },
            {
                assertEquals(expectedMessage, payload.message)
            },
            {
                assertEquals(expectedPlanetId, payload.planetId)
            },
            {
                assertEquals(expectedRemainingEnergy, payload.remainingEnergy)
            }
        )
    }
}