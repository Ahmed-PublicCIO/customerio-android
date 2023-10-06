package io.customer.datapipeline

import com.segment.analytics.kotlin.core.Configuration
import io.customer.sdk.CustomerIO

fun CustomerIO.Builder.build(configuration: Configuration.() -> Unit = {}): CustomerIO.Builder {
    return this.apply {
        addCustomerIOModule(
            ModuleDataPipeline(
                DataPipelineModuleConfig.Builder(writeKey, configuration).build()
            )
        )
    }
}
