package io.customer.sdk.di

import android.content.Context
import io.customer.sdk.api.*
import io.customer.sdk.data.store.*
import io.customer.sdk.module.CustomerIOModule
import io.customer.sdk.module.CustomerIOModuleConfig
import io.customer.sdk.queue.*
import io.customer.sdk.repository.*
import io.customer.sdk.repository.preference.SharedPreferenceRepository
import io.customer.sdk.repository.preference.SharedPreferenceRepositoryImp
import io.customer.sdk.util.*

/**
 * Configuration class to configure/initialize low-level operations and objects.
 */
class CustomerIOComponent(
    private val staticComponent: CustomerIOStaticComponent,
    val context: Context
) : DiGraph() {

    val modules: MutableMap<String, CustomerIOModule<out CustomerIOModuleConfig>> = mutableMapOf()

    internal val sharedPreferenceRepository: SharedPreferenceRepository by lazy {
        override() ?: SharedPreferenceRepositoryImp(
            context = context
        )
    }
    val dispatchersProvider: DispatchersProvider
        get() = override() ?: staticComponent.dispatchersProvider

    val logger: Logger
        get() = override() ?: staticComponent.logger

    val dateUtil: DateUtil
        get() = override() ?: DateUtilImpl()
}
