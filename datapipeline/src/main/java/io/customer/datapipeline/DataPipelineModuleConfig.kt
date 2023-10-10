package io.customer.datapipeline

import com.segment.analytics.kotlin.core.Configuration
import io.customer.sdk.module.CustomerIOModuleConfig

class DataPipelineModuleConfig(
    val writeKey: String,
    val configuration: Configuration.() -> Unit = {}
) : CustomerIOModuleConfig {

    class Builder(
        private val writeKey: String,
        val configuration: Configuration.() -> Unit = {}
    ) : CustomerIOModuleConfig.Builder<DataPipelineModuleConfig> {

        override fun build(): DataPipelineModuleConfig {
            return DataPipelineModuleConfig(
                writeKey = writeKey,
                configuration = configuration
            )
        }
    }
}
