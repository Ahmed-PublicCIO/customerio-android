package io.customer.sdk

import android.app.Application
import io.customer.base.internal.InternalCustomerIOApi
import io.customer.sdk.data.model.Region
import io.customer.sdk.data.store.Client
import io.customer.sdk.di.CustomerIOComponent
import io.customer.sdk.di.CustomerIOStaticComponent
import io.customer.sdk.extensions.getProperty
import io.customer.sdk.extensions.takeIfNotBlank
import io.customer.sdk.module.CustomerIOModuleConfig
import io.customer.sdk.repository.CleanupRepository
import io.customer.sdk.repository.DeviceRepository
import io.customer.sdk.repository.ProfileRepository
import io.customer.sdk.repository.TrackRepository
import io.customer.sdk.util.CioLogLevel
import io.customer.sdk.util.Seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class TrackingModuleConfig(private val diGraph: CustomerIOComponent) : CustomerIOModuleConfig {

    class Builder @JvmOverloads constructor(
        private val siteId: String,
        private val apiKey: String,
        private var region: Region = Region.US,
        private val appContext: Application
    ) {

        private var client: Client = Client.Android(Version.version)
        private var timeout = CustomerIOConfig.Companion.AnalyticsConstants.HTTP_REQUEST_TIMEOUT
        private var shouldAutoRecordScreenViews: Boolean =
            CustomerIOConfig.Companion.AnalyticsConstants.SHOULD_AUTO_RECORD_SCREEN_VIEWS
        private var autoTrackDeviceAttributes: Boolean =
            CustomerIOConfig.Companion.AnalyticsConstants.AUTO_TRACK_DEVICE_ATTRIBUTES
        private var trackingApiUrl: String? = null
        private var backgroundQueueMinNumberOfTasks: Int =
            CustomerIOConfig.Companion.AnalyticsConstants.BACKGROUND_QUEUE_MIN_NUMBER_OF_TASKS
        private var backgroundQueueSecondsDelay: Double =
            CustomerIOConfig.Companion.AnalyticsConstants.BACKGROUND_QUEUE_SECONDS_DELAY
        private var logLevel: CioLogLevel =
            CustomerIOConfig.Companion.SDKConstants.LOG_LEVEL_DEFAULT

        // added a `config` in the secondary constructor so users stick to our advised primary constructor
        // and this is used internally only.
        constructor(
            siteId: String,
            apiKey: String,
            region: Region = Region.US,
            appContext: Application,
            config: Map<String, Any?>
        ) : this(siteId, apiKey, region, appContext) {
            setupConfig(config)
        }

        @OptIn(InternalCustomerIOApi::class)
        private fun setupConfig(config: Map<String, Any?>?): Builder {
            if (config == null) return this
            config.getProperty<String>(CustomerIOConfig.Companion.Keys.LOG_LEVEL)?.takeIfNotBlank()
                ?.let { logLevel ->
                    setLogLevel(level = CioLogLevel.getLogLevel(logLevel))
                }
            config.getProperty<String>(CustomerIOConfig.Companion.Keys.TRACKING_API_URL)
                ?.takeIfNotBlank()?.let { value ->
                    setTrackingApiURL(value)
                }
            config.getProperty<Boolean>(CustomerIOConfig.Companion.Keys.AUTO_TRACK_DEVICE_ATTRIBUTES)
                ?.let { value ->
                    autoTrackDeviceAttributes(shouldTrackDeviceAttributes = value)
                }
            config.getProperty<Double>(CustomerIOConfig.Companion.Keys.BACKGROUND_QUEUE_SECONDS_DELAY)
                ?.let { value ->
                    setBackgroundQueueSecondsDelay(backgroundQueueSecondsDelay = value)
                }
            val source =
                config.getProperty<String>(CustomerIOConfig.Companion.Keys.SOURCE_SDK_SOURCE)
            val version =
                config.getProperty<String>(CustomerIOConfig.Companion.Keys.SOURCE_SDK_VERSION)

            if (!source.isNullOrBlank() && !version.isNullOrBlank()) {
                setClient(Client.fromRawValue(source = source, sdkVersion = version))
            }

            when (
                val minNumberOfTasks =
                    config[CustomerIOConfig.Companion.Keys.BACKGROUND_QUEUE_MIN_NUMBER_OF_TASKS]
            ) {
                is Int -> {
                    setBackgroundQueueMinNumberOfTasks(backgroundQueueMinNumberOfTasks = minNumberOfTasks)
                }

                is Double -> {
                    setBackgroundQueueMinNumberOfTasks(backgroundQueueMinNumberOfTasks = minNumberOfTasks.toInt())
                }
            }
            return this
        }

        fun setClient(client: Client): Builder {
            this.client = client
            return this
        }

        fun setRegion(region: Region): Builder {
            this.region = region
            return this
        }

        fun setRequestTimeout(timeout: Long): Builder {
            this.timeout = timeout
            return this
        }

        fun autoTrackScreenViews(shouldRecordScreenViews: Boolean): Builder {
            this.shouldAutoRecordScreenViews = shouldRecordScreenViews
            return this
        }

        fun autoTrackDeviceAttributes(shouldTrackDeviceAttributes: Boolean): Builder {
            this.autoTrackDeviceAttributes = shouldTrackDeviceAttributes
            return this
        }

        fun setLogLevel(level: CioLogLevel): Builder {
            this.logLevel = level
            return this
        }

        /**
         * Base URL to use for the Customer.io track API. You will more then likely not modify this value.
         * If you override this value, `Region` set when initializing the SDK will be ignored.
         */
        fun setTrackingApiURL(trackingApiUrl: String): Builder {
            this.trackingApiUrl = trackingApiUrl
            return this
        }

        /**
         * Sets the number of tasks in the background queue before the queue begins operating.
         * This is mostly used during development to test configuration is setup. We do not recommend
         * modifying this value because it impacts battery life of mobile device.
         *
         * @param backgroundQueueMinNumberOfTasks the minimum number of tasks in background queue; default 10
         */
        fun setBackgroundQueueMinNumberOfTasks(backgroundQueueMinNumberOfTasks: Int): Builder {
            this.backgroundQueueMinNumberOfTasks = backgroundQueueMinNumberOfTasks
            return this
        }

        /**
         * Sets the number of seconds to delay running queue after a task has been added to it
         *
         * @param backgroundQueueSecondsDelay time in seconds to delay events; default 30
         */
        fun setBackgroundQueueSecondsDelay(backgroundQueueSecondsDelay: Double): Builder {
            this.backgroundQueueSecondsDelay = backgroundQueueSecondsDelay
            return this
        }

        fun build(): TrackingModuleConfig {
            if (apiKey.isEmpty()) {
                throw IllegalStateException("apiKey is not defined in " + this::class.java.simpleName)
            }

            if (siteId.isEmpty()) {
                throw IllegalStateException("siteId is not defined in " + this::class.java.simpleName)
            }

            val config = CustomerIOConfig(
                client = client,
                siteId = siteId,
                apiKey = apiKey,
                region = region,
                timeout = timeout,
                autoTrackScreenViews = shouldAutoRecordScreenViews,
                autoTrackDeviceAttributes = autoTrackDeviceAttributes,
                backgroundQueueMinNumberOfTasks = backgroundQueueMinNumberOfTasks,
                backgroundQueueSecondsDelay = backgroundQueueSecondsDelay,
                backgroundQueueTaskExpiredSeconds = Seconds.fromDays(3).value,
                logLevel = logLevel,
                trackingApiUrl = trackingApiUrl
            )

            val diGraph = CustomerIOComponent(
                staticComponent = CustomerIOStaticComponent(),
                sdkConfig = config,
                context = appContext
            )
            val client = TrackingModuleConfig(diGraph)
            val logger = diGraph.logger

            appContext.registerActivityLifecycleCallbacks(diGraph.activityLifecycleCallbacks)

            client.postInitialize()

            return client
        }
    }

    private val trackRepository: TrackRepository
        get() = diGraph.trackRepository

    private val deviceRepository: DeviceRepository
        get() = diGraph.deviceRepository

    private val profileRepository: ProfileRepository
        get() = diGraph.profileRepository

    private val siteId: String
        get() = diGraph.sdkConfig.siteId

    private val sdkVersion: String
        get() = Version.version

    private val cleanupRepository: CleanupRepository
        get() = diGraph.cleanupRepository

    private fun postInitialize() {
        // run cleanup asynchronously in background to prevent taking up the main/UI thread
        CoroutineScope(diGraph.dispatchersProvider.background).launch {
            cleanupRepository.cleanup()
        }
    }
}
