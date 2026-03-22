package com.speeduino.manager.ecu

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FirmwareHandshakeDomainTest {

    @Test
    fun `normalizes noisy speeduino signatures`() {
        val normalized = FirmwareHandshakeDomain.normalizeSignature(" speeduino   2024.02 \u0000 ???")
        assertEquals("speeduino 202402", normalized)
    }

    @Test
    fun `resolves consensus from repeated valid samples`() {
        val consensus = FirmwareHandshakeDomain.resolveConsensus(
            listOf(
                "speeduino 202402",
                "Speeduino 2024.02",
                "noise",
            ),
        )

        assertEquals("speeduino 202402", consensus.signature)
        assertEquals(2, consensus.consensusHits)
    }

    @Test
    fun `rejects invalid consensus`() {
        assertFailsWith<Exception> {
            FirmwareHandshakeDomain.validateConsensus(
                consensus = FirmwareConsensus(signature = null, consensusHits = 0),
                samples = listOf("??", "garbage"),
            )
        }
    }
}
