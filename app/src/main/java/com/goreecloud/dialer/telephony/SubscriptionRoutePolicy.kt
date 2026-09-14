package com.goreecloud.dialer.telephony

/**
 * Minimal process-local description of a currently routable carrier subscription.
 *
 * The policy deliberately uses stable subscription IDs rather than labels so a UI can
 * present carrier/SIM names without those presentation values becoming routing authority.
 */
data class ActiveSubscription(
    val subscriptionId: Int,
)

sealed interface SubscriptionRouteDecision {
    /** Android Telecom remains authoritative for emergency routing. */
    data object DeferEmergencyToPlatform : SubscriptionRouteDecision

    /** Route through this exact active subscription. */
    data class UseSubscription(val subscriptionId: Int) : SubscriptionRouteDecision

    /** Multiple valid routes exist and the user must choose one explicitly. */
    data class RequiresUserSelection(val subscriptionIds: List<Int>) : SubscriptionRouteDecision

    /** No safe route can be selected from the currently accepted state. */
    data class Unavailable(val reason: String) : SubscriptionRouteDecision
}

/**
 * Fail-closed multi-SIM route policy.
 *
 * Important invariants:
 * - an explicit user selection is never silently replaced by another SIM;
 * - one active SIM may be selected automatically because there is no competing route;
 * - multiple active SIMs require explicit selection;
 * - emergency calls never receive an experimental subscription override.
 */
object SubscriptionRoutePolicy {
    fun decide(
        activeSubscriptions: List<ActiveSubscription>,
        explicitlySelectedSubscriptionId: Int?,
        isEmergencyCall: Boolean,
    ): SubscriptionRouteDecision {
        if (isEmergencyCall) return SubscriptionRouteDecision.DeferEmergencyToPlatform

        val activeIds = activeSubscriptions
            .map { it.subscriptionId }
            .distinct()
            .sorted()

        if (explicitlySelectedSubscriptionId != null) {
            return if (explicitlySelectedSubscriptionId in activeIds) {
                SubscriptionRouteDecision.UseSubscription(explicitlySelectedSubscriptionId)
            } else {
                SubscriptionRouteDecision.Unavailable(
                    "Selected subscription is no longer active; choose an available SIM",
                )
            }
        }

        return when (activeIds.size) {
            0 -> SubscriptionRouteDecision.Unavailable("No active carrier subscription is available")
            1 -> SubscriptionRouteDecision.UseSubscription(activeIds.single())
            else -> SubscriptionRouteDecision.RequiresUserSelection(activeIds)
        }
    }
}
