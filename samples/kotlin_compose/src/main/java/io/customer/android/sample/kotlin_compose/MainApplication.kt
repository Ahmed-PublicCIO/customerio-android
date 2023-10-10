package io.customer.android.sample.kotlin_compose

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import io.customer.android.sample.kotlin_compose.data.repositories.PreferenceRepository
import io.customer.android.sample.kotlin_compose.data.sdk.InAppMessageEventListener
import io.customer.datapipeline.DataPipelineModuleConfig
import io.customer.datapipeline.ModuleDataPipeline
import io.customer.messaginginapp.MessagingInAppModuleConfig
import io.customer.messaginginapp.ModuleMessagingInApp
import io.customer.sdk.CustomerIO
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@HiltAndroidApp
class MainApplication : Application() {

    @Inject
    lateinit var preferences: PreferenceRepository

    override fun onCreate() {
        super.onCreate()
        val configuration = runBlocking {
            preferences.getConfiguration().first()
        }

        CustomerIO.Builder(appContext = this).apply {
            setImplementation(
                impl = ModuleDataPipeline(
                    DataPipelineModuleConfig.Builder(writeKey = "") {
                        this.flushInterval = 1
                    }.build()
                )
            )
            addCustomerIOModule(
                ModuleMessagingInApp(
                    config = MessagingInAppModuleConfig.Builder()
                        .setEventListener(InAppMessageEventListener()).build()
                )
            )
        }.build()
    }
}
