package io.customer.messagingpush.util

import android.os.Bundle
import io.customer.sdk.CustomerIOImplementation

interface PushTrackingUtil {
    fun parseLaunchedActivityForTracking(bundle: Bundle): Boolean

    companion object {
        const val DELIVERY_ID_KEY = "CIO-Delivery-ID"
        const val DELIVERY_TOKEN_KEY = "CIO-Delivery-Token"
    }
}

class PushTrackingUtilImpl(
    private val customerIOImplementation: CustomerIOImplementation
) : PushTrackingUtil {

    override fun parseLaunchedActivityForTracking(bundle: Bundle): Boolean {
        val deliveryId = bundle.getString(PushTrackingUtil.DELIVERY_ID_KEY)
        val deliveryToken = bundle.getString(PushTrackingUtil.DELIVERY_TOKEN_KEY)

        if (deliveryId == null || deliveryToken == null) return false

        customerIOImplementation.trackMetric(
            deliveryID = deliveryId,
            deviceToken = deliveryToken,
            event = "opened"
        )

        return true
    }
}
