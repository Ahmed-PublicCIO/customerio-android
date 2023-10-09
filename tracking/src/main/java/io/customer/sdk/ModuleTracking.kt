package io.customer.sdk

import io.customer.sdk.di.CustomerIOComponent
import io.customer.sdk.module.CustomerIOModule

class ModuleTracking(
    override val moduleConfig: TrackingModuleConfig,
    private val overrideDiGraph: CustomerIOComponent?
) : CustomerIOModule<TrackingModuleConfig> {
    override val moduleName: String
        get() = "Tracking"

    override fun initialize() {
        TODO("Not yet implemented")
    }
}
