package io.customer.sdk

import android.app.Application
import androidx.annotation.VisibleForTesting
import io.customer.base.internal.InternalCustomerIOApi
import io.customer.sdk.di.CustomerIOComponent
import io.customer.sdk.di.CustomerIOStaticComponent
import io.customer.sdk.module.CustomerIOModule
import io.customer.sdk.module.CustomerIOModuleConfig
import io.customer.sdk.util.CioLogLevel

/**
 * Allows mocking of [CustomerIO] for your automated tests in your project. Mock [CustomerIO] to assert your code is calling functions
 * of the SDK and/or do not have the SDK run it's real implementation during automated tests.
 */
interface CustomerIOInstance

interface CustomerIOImplementation : CustomerIOModule<CustomerIOModuleConfig>, CustomerIOInstance

class CustomerIO internal constructor(
    /**
     * Strong reference to graph that other top-level classes in SDK can use `CustomerIO.instance().diGraph`.
     */
    val diGraph: CustomerIOComponent,
    private val implementation: CustomerIOImplementation
) {

    companion object {
        private var instance: CustomerIO? = null

        @InternalCustomerIOApi
        @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
        fun clearInstance() {
            instance?.let {
                instance = null
            }
        }

        @JvmStatic
        fun instance(): CustomerIO {
            return instance
                ?: throw IllegalStateException("CustomerIO.Builder::build() must be called before obtaining CustomerIO instance")
        }
    }

    class Builder @JvmOverloads constructor(
        val appContext: Application
    ) {
        private val modules: MutableMap<String, CustomerIOModule<out CustomerIOModuleConfig>> =
            mutableMapOf()
        private var implementation: CustomerIOImplementation? = null

        private var logLevel: CioLogLevel =
            CustomerIOConfig.Companion.SDKConstants.LOG_LEVEL_DEFAULT
        public var overrideDiGraph: CustomerIOComponent? =
            null // public for automated tests in non-tracking modules to override the di graph.

        fun setLogLevel(level: CioLogLevel): Builder {
            this.logLevel = level
            return this
        }

        fun setImplementation(impl: CustomerIOImplementation): Builder {
            this.implementation = impl
            return this
        }

        fun <Config : CustomerIOModuleConfig> addCustomerIOModule(module: CustomerIOModule<Config>): Builder {
            modules[module.moduleName] = module
            return this
        }

        @InternalCustomerIOApi
        fun build(): CustomerIO {
            require(implementation != null) {
                "A core implementation must be set before building CustomerIO."
            }

            val diGraph = overrideDiGraph ?: CustomerIOComponent(
                staticComponent = CustomerIOStaticComponent(),
                context = appContext
            )
            val client = CustomerIO(diGraph = diGraph, implementation = implementation!!)
            val logger = diGraph.logger

            // cleanup of old reference if it exists, so that if the SDK is re-initialized (due to wrappers different lifecycle),
            // the old instance is not kept in memory and any callbacks are unregistered
            clearInstance()

            instance = client

            diGraph.modules.putAll(modules)

            modules.forEach {
                logger.debug("initializing SDK module ${it.value.moduleName}...")
                it.value.initialize()
            }

            return client
        }
    }
}
