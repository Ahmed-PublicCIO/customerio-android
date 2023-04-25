package io.customer.sdk.repository

import io.customer.sdk.CustomerIOConfig
import io.customer.sdk.data.model.CustomAttributes
import io.customer.sdk.hooks.HooksManager
import io.customer.sdk.hooks.ModuleHook
import io.customer.sdk.queue.Queue
import io.customer.sdk.repository.preference.SitePreferenceRepository
import io.customer.sdk.util.Logger

interface ProfileRepository {
    fun setAnonymousId(anonymousId: String?)
    fun identify(identifier: String, attributes: CustomAttributes)
    fun clearIdentify()
    fun addCustomProfileAttributes(attributes: CustomAttributes)
}

internal class ProfileRepositoryImpl(
    private val config: CustomerIOConfig,
    private val deviceRepository: DeviceRepository,
    private val sitePreferenceRepository: SitePreferenceRepository,
    private val backgroundQueue: Queue,
    private val logger: Logger,
    private val hooksManager: HooksManager
) : ProfileRepository {

    override fun setAnonymousId(anonymousId: String?) {
        anonymousId?.let {
            if (config.shouldAllowAnonymousMessaging) {
                logger.info("setting anonymousId $it for profile")
                // identify profile
                identify(it, mapOf("anonymous_profile" to true))
                // store anonymous profile id
                sitePreferenceRepository.saveAnonymousProfileId(it)
            } else {
                logger.info("setting anonymousId $it, but anonymous messaging is disabled")
                sitePreferenceRepository.saveAnonymousId(it)
            }
        }
    }

    override fun identify(identifier: String, attributes: CustomAttributes) {
        val profileAttributes = attributes.toMutableMap()

        logger.info("identify profile $identifier")
        logger.debug("identify profile $identifier, $profileAttributes")

        val currentlyIdentifiedProfileIdentifier = sitePreferenceRepository.getIdentifier()

        val currentlyAnonymousProfileId = sitePreferenceRepository.getAnonymousProfileId()

        // The SDK calls identify() with the already identified profile for changing profile attributes.
        val isChangingIdentifiedProfile =
            currentlyIdentifiedProfileIdentifier != null && currentlyIdentifiedProfileIdentifier != identifier

        val isFirstTimeIdentifying = currentlyIdentifiedProfileIdentifier == null

        currentlyIdentifiedProfileIdentifier?.let { currentlyIdentifiedProfileIdentifier ->
            if (isChangingIdentifiedProfile) {
                logger.info("changing profile from id $currentlyIdentifiedProfileIdentifier to $identifier")

                logger.debug("deleting device token before identifying new profile")
                deviceRepository.deleteDeviceToken()
                logger.debug("deleting anonymousId before identifying new profile")
                sitePreferenceRepository.removeAnonymousId()
            }
        }

        val anonymousId = sitePreferenceRepository.getAnonymousId()

        if (anonymousId != null) {
            logger.debug("adding anonymousId to profile attributes")
            profileAttributes["anonymous_id"] = anonymousId
        }

        val queueStatus = backgroundQueue.queueIdentifyProfile(
            identifier,
            currentlyIdentifiedProfileIdentifier,
            profileAttributes
        )

        // Don't modify the state of the SDK's data until we confirm we added a queue task successfully. This could put the Customer.io API
        // out-of-sync with the SDK's state and cause many future HTTP errors.
        // Therefore, if adding the task to the queue failed, ignore the request and fail early.
        if (!queueStatus.success) {
            logger.debug("failed to add identify task to queue")
            return
        }

        // merge profile
        if (currentlyAnonymousProfileId != null && isChangingIdentifiedProfile) {
            logger.debug("merging anonymous profile $currentlyAnonymousProfileId with identified profile $identifier")

            val queueStatus =
                backgroundQueue.queueMergeProfiles(identifier, currentlyAnonymousProfileId)

            if (queueStatus.success) {
                logger.debug("removing anonymous profile id from device storage")
                sitePreferenceRepository.removeAnonymousProfileId()
            }
        }

        logger.debug("storing identifier on device storage $identifier")
        sitePreferenceRepository.saveIdentifier(identifier)

        hooksManager.onHookUpdate(
            hook = ModuleHook.ProfileIdentifiedHook(identifier)
        )

        if (isFirstTimeIdentifying || isChangingIdentifiedProfile) {
            logger.debug("first time identified or changing identified profile")

            sitePreferenceRepository.getDeviceToken()?.let {
                logger.debug("automatically registering device token to newly identified profile")
                deviceRepository.registerDeviceToken(
                    it,
                    emptyMap()
                ) // no new attributes but default ones to pass so pass empty.
            }
        }
    }

    override fun addCustomProfileAttributes(attributes: CustomAttributes) {
        logger.debug("adding profile attributes request made")

        val currentlyIdentifiedProfileId = sitePreferenceRepository.getIdentifier()

        if (currentlyIdentifiedProfileId == null) {
            logger.debug("no profile is currently identified. ignoring request to add attributes to a profile")
            return
        }

        identify(currentlyIdentifiedProfileId, attributes = attributes)
    }

    override fun clearIdentify() {
        logger.debug("clearing identified profile request made")

        val currentlyIdentifiedProfileId = sitePreferenceRepository.getIdentifier()

        if (currentlyIdentifiedProfileId == null) {
            logger.info("no profile is currently identified. ignoring request to clear identified profile")
            return
        }

        // notify hooks about identifier being cleared
        hooksManager.onHookUpdate(
            ModuleHook.BeforeProfileStoppedBeingIdentified(
                identifier = currentlyIdentifiedProfileId
            )
        )

        // delete token from profile to prevent sending the profile pushes when they are not identified in the SDK.
        deviceRepository.deleteDeviceToken()

        // delete identified from device storage to not associate future SDK calls to this profile
        logger.debug("clearing profile from device storage")
        sitePreferenceRepository.removeIdentifier()
        sitePreferenceRepository.removeAnonymousId()
    }
}
