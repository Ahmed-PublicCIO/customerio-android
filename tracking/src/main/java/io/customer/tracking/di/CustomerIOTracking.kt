package io.customer.tracking.di

import io.customer.base.internal.InternalCustomerIOApi
import io.customer.datapipeline.DataPipelineModuleConfig
import io.customer.datapipeline.ModuleDataPipeline
import io.customer.sdk.CustomerIO

@OptIn(InternalCustomerIOApi::class)
fun CustomerIO.Builder.build(): CustomerIO {
    return trackingBuild {
        addCustomerIOModule(
            ModuleDataPipeline(
                DataPipelineModuleConfig.Builder(writeKey = writeKey, configuration = {
                    this.flushInterval = it.backgroundQueueMinNumberOfTasks
                }).build()
            )
        )
    }
}
