package io.customer.sdk

import android.app.Activity
import android.content.pm.PackageManager
import io.customer.sdk.data.model.CustomAttributes
import io.customer.sdk.data.request.MetricEvent
import io.customer.sdk.extensions.getScreenNameFromActivity
import io.customer.sdk.module.CustomerIOModule
import io.customer.sdk.repository.DeviceRepository
import io.customer.sdk.repository.ProfileRepository
import io.customer.sdk.repository.TrackRepository

class ModuleTracking(
    override val moduleConfig: TrackingModuleConfig
) : CustomerIOModule<TrackingModuleConfig>, CustomerIOInstance {
    override val moduleName: String
        get() = "Tracking"

    override fun initialize() {}

    private val trackRepository: TrackRepository
        get() = moduleConfig.diGraph.trackRepository

    private val deviceRepository: DeviceRepository
        get() = moduleConfig.diGraph.deviceRepository

    private val profileRepository: ProfileRepository
        get() = moduleConfig.diGraph.profileRepository

    private val siteId: String
        get() = moduleConfig.diGraph.sdkConfig.siteId

    private val sdkVersion: String
        get() = Version.version

    /**
     * Identify a customer (aka: Add or update a profile).
     * [Learn more](https://customer.io/docs/identifying-people/) about identifying a customer in Customer.io
     * Note: You can only identify 1 profile at a time in your SDK. If you call this function multiple times,
     * the previously identified profile will be removed. Only the latest identified customer is persisted.
     * @param identifier ID you want to assign to the customer.
     * This value can be an internal ID that your system uses or an email address.
     * [Learn more](https://customer.io/docs/api/#operation/identify)
     * @return Action<Unit> which can be accessed via `execute` or `enqueue`
     */
    override fun identify(identifier: String) = this.identify(identifier, emptyMap())

    /**
     * Identify a customer (aka: Add or update a profile).
     * [Learn more](https://customer.io/docs/identifying-people/) about identifying a customer in Customer.io
     * Note: You can only identify 1 profile at a time in your SDK. If you call this function multiple times,
     * the previously identified profile will be removed. Only the latest identified customer is persisted.
     * @param identifier ID you want to assign to the customer.
     * This value can be an internal ID that your system uses or an email address.
     * [Learn more](https://customer.io/docs/api/#operation/identify)
     * @param attributes Map of <String, IdentityAttributeValue> to be added
     * @return Action<Unit> which can be accessed via `execute` or `enqueue`
     */
    override fun identify(
        identifier: String,
        attributes: CustomAttributes
    ) = profileRepository.identify(identifier, attributes)

    /**
     * Track an event
     * [Learn more](https://customer.io/docs/events/) about events in Customer.io
     * @param name Name of the event you want to track.
     */
    override fun track(name: String) = this.track(name, emptyMap())

    /**
     * Track an event
     * [Learn more](https://customer.io/docs/events/) about events in Customer.io
     * @param name Name of the event you want to track.
     * @param attributes Optional event body in Map format used as JSON object
     * @return Action<Unit> which can be accessed via `execute` or `enqueue`
     */
    override fun track(
        name: String,
        attributes: CustomAttributes
    ) = trackRepository.track(name, attributes)

    /**
     * Track screen
     * @param name Name of the screen you want to track.
     * @return Action<Unit> which can be accessed via `execute` or `enqueue`
     */
    override fun screen(name: String) = this.screen(name, emptyMap())

    /**
     * Track screen
     * @param name Name of the screen you want to track.
     * @param attributes Optional event body in Map format used as JSON object
     * @return Action<Unit> which can be accessed via `execute` or `enqueue`
     */
    override fun screen(
        name: String,
        attributes: CustomAttributes
    ) = trackRepository.screen(name, attributes)

    /**
     * Track activity screen, `label` added for this activity in `manifest` will be utilized for tracking
     * @param activity Instance of the activity you want to track.
     * @return Action<Unit> which can be accessed via `execute` or `enqueue`
     */
    fun screen(activity: Activity) = this.screen(activity, emptyMap())

    /**
     * Track activity screen, `label` added for this activity in `manifest` will be utilized for tracking
     * @param activity Instance of the activity you want to track.
     * @param attributes Optional event body in Map format used as JSON object
     * @return Action<Unit> which can be accessed via `execute` or `enqueue`
     */
    fun screen(
        activity: Activity,
        attributes: CustomAttributes
    ) = recordScreenViews(activity, attributes)

    /**
     * Stop identifying the currently persisted customer. All future calls to the SDK will no longer
     * be associated with the previously identified customer.
     * Note: If you simply want to identify a *new* customer, this function call is optional. Simply
     * call `identify()` again to identify the new customer profile over the existing.
     * If no profile has been identified yet, this function will ignore your request.
     */
    override fun clearIdentify() {
        profileRepository.clearIdentify()
    }

    /**
     * Register a new device token with Customer.io, associated with the current active customer. If there
     * is no active customer, this will fail to register the device
     */
    fun registerDeviceToken(deviceToken: String) =
        deviceRepository.registerDeviceToken(deviceToken, deviceAttributes)

    /**
     * Delete the currently registered device token
     */
    fun deleteDeviceToken() = deviceRepository.deleteDeviceToken()

    /**
     * Track a push metric
     */
    override fun trackMetric(
        deliveryID: String,
        event: String,
        deviceToken: String
    ) = trackRepository.trackMetric(
        deliveryID = deliveryID,
        event = MetricEvent.getEvent(event) ?: MetricEvent.delivered,
        deviceToken = deviceToken
    )

    /**
     * Use to provide attributes to the currently identified profile.
     *
     * Note: If there is not a profile identified, this request will be ignored.
     */
    var profileAttributes: CustomAttributes = emptyMap()
        set(value) {
            profileRepository.addCustomProfileAttributes(value)
        }

    /**
     * Use to provide additional and custom device attributes
     * apart from the ones the SDK is programmed to send to customer workspace.
     */
    var deviceAttributes: CustomAttributes = emptyMap()
        set(value) {
            field = value

            deviceRepository.addCustomDeviceAttributes(value)
        }

    override val registeredDeviceToken: String?
        get() = deviceRepository.getDeviceToken()

    private fun recordScreenViews(activity: Activity, attributes: CustomAttributes) {
        val packageManager = activity.packageManager
        return try {
            val info = packageManager.getActivityInfo(
                activity.componentName,
                PackageManager.GET_META_DATA
            )
            val activityLabel = info.loadLabel(packageManager)

            val screenName = activityLabel.toString().ifEmpty {
                activity::class.java.simpleName.getScreenNameFromActivity()
            }
            screen(screenName, attributes)
        } catch (e: PackageManager.NameNotFoundException) {
            // if `PackageManager.NameNotFoundException` is thrown, is that a bug in the SDK or a problem with the customer's app?
            // We may want to decide to log this as an SDK error, log it so customer notices it to fix it themselves, or we do nothing because this exception might not be a big issue.
            // ActionUtils.getErrorAction(ErrorResult(error = ErrorDetail(message = "Activity Not Found: $e")))
        } catch (e: Exception) {
            // Should we log exceptions that happen? Ignore them? How rare is an exception happen in this function?
            // ActionUtils.getErrorAction(ErrorResult(error = ErrorDetail(message = "Unable to track, $activity")))
        }
    }
}
