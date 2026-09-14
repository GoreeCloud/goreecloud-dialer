package com.goreecloud.dialer.telephony

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubscriptionRoutePolicyTest {
    @Test
    fun emergencyCallsAlwaysDeferToPlatformRouting() {
        val result = SubscriptionRoutePolicy.decide(
            activeSubscriptions = listOf(ActiveSubscription(1), ActiveSubscription(2)),
            explicitlySelectedSubscriptionId = 2,
            isEmergencyCall = true,
        )

        assertEquals(SubscriptionRouteDecision.DeferEmergencyToPlatform, result)
    }

    @Test
    fun explicitActiveSelectionIsPreserved() {
        val result = SubscriptionRoutePolicy.decide(
            activeSubscriptions = listOf(ActiveSubscription(4), ActiveSubscription(9)),
            explicitlySelectedSubscriptionId = 9,
            isEmergencyCall = false,
        )

        assertEquals(SubscriptionRouteDecision.UseSubscription(9), result)
    }

    @Test
    fun missingExplicitSelectionFailsClosedInsteadOfFallingBack() {
        val result = SubscriptionRoutePolicy.decide(
            activeSubscriptions = listOf(ActiveSubscription(4), ActiveSubscription(9)),
            explicitlySelectedSubscriptionId = 7,
            isEmergencyCall = false,
        )

        assertTrue(result is SubscriptionRouteDecision.Unavailable)
    }

    @Test
    fun multipleActiveSubscriptionsRequireUserSelection() {
        val result = SubscriptionRoutePolicy.decide(
            activeSubscriptions = listOf(ActiveSubscription(9), ActiveSubscription(4), ActiveSubscription(9)),
            explicitlySelectedSubscriptionId = null,
            isEmergencyCall = false,
        )

        assertEquals(
            SubscriptionRouteDecision.RequiresUserSelection(listOf(4, 9)),
            result,
        )
    }

    @Test
    fun oneActiveSubscriptionCanBeUsedWithoutAmbiguity() {
        val result = SubscriptionRoutePolicy.decide(
            activeSubscriptions = listOf(ActiveSubscription(12)),
            explicitlySelectedSubscriptionId = null,
            isEmergencyCall = false,
        )

        assertEquals(SubscriptionRouteDecision.UseSubscription(12), result)
    }

    @Test
    fun noActiveSubscriptionIsUnavailable() {
        val result = SubscriptionRoutePolicy.decide(
            activeSubscriptions = emptyList(),
            explicitlySelectedSubscriptionId = null,
            isEmergencyCall = false,
        )

        assertTrue(result is SubscriptionRouteDecision.Unavailable)
    }
}
