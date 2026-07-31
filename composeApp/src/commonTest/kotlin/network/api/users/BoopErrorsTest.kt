package io.github.vrcmteam.vrcm.network.api.users

import io.github.vrcmteam.vrcm.network.supports.VRCApiException
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BoopErrorsTest {
    @Test
    fun identifiesTheKnownAlreadyBoopedResponse() {
        val error = VRCApiException(
            description = "Too Many Requests",
            code = 429,
            bodyText = "{\"error\":{\"message\":\"User already booped.\",\"status_code\":429}}",
        )

        assertTrue(error.isBoopAlreadySentError())
    }

    @Test
    fun doesNotHideOtherRateLimitFailures() {
        val error = VRCApiException(
            description = "Too Many Requests",
            code = 429,
            bodyText = "{\"error\":{\"message\":\"Rate limit exceeded\",\"status_code\":429}}",
        )

        assertFalse(error.isBoopAlreadySentError())
    }
}
