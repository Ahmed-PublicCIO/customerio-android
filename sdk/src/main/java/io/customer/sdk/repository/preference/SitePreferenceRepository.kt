package io.customer.sdk.repository.preference

import android.content.Context
import androidx.core.content.edit
import io.customer.sdk.CustomerIOConfig
import io.customer.sdk.extensions.getDate
import io.customer.sdk.extensions.putDate
import java.util.Date

interface SitePreferenceRepository {
    fun saveIdentifier(identifier: String)
    fun removeIdentifier()
    fun getIdentifier(): String?

    fun saveAnonymousId(anonymousId: String)
    fun removeAnonymousId()
    fun getAnonymousId(): String?
    fun saveAnonymousProfileId(anonymousProfileId: String)
    fun removeAnonymousProfileId()
    fun getAnonymousProfileId(): String?

    fun saveDeviceToken(token: String)
    fun getDeviceToken(): String?

    fun clearAll()

    var httpRequestsPauseEnds: Date?
}

internal class SitePreferenceRepositoryImpl(
    context: Context,
    private val config: CustomerIOConfig
) : BasePreferenceRepository(context), SitePreferenceRepository {

    override val prefsName: String by lazy {
        "io.customer.sdk.${context.packageName}.${config.siteId}"
    }

    companion object {
        private const val KEY_IDENTIFIER = "identifier"
        private const val KEY_ANONYMOUS_ID = "anonymous_id"
        private const val KEY_ANONYMOUS_PROFILE = "anonymous_profile_id"
        private const val KEY_DEVICE_TOKEN = "device_token"
        private const val KEY_HTTP_PAUSE_ENDS = "http_pause_ends"
    }

    override fun saveIdentifier(identifier: String) {
        prefs.edit { putString(KEY_IDENTIFIER, identifier) }
    }

    override fun removeIdentifier() {
        prefs.edit { remove(KEY_IDENTIFIER) }
    }

    override fun getIdentifier(): String? {
        return prefs.getString(KEY_IDENTIFIER, null)
    }

    override fun saveAnonymousId(anonymousId: String) {
        prefs.edit { putString(KEY_ANONYMOUS_ID, anonymousId) }
    }

    override fun removeAnonymousId() {
        prefs.edit { remove(KEY_ANONYMOUS_ID) }
    }

    override fun getAnonymousId(): String? {
        return prefs.getString(KEY_ANONYMOUS_ID, null)
    }

    override fun saveAnonymousProfileId(anonymousProfileId: String) {
        prefs.edit { putString(KEY_ANONYMOUS_PROFILE, anonymousProfileId) }
    }

    override fun removeAnonymousProfileId() {
        prefs.edit { remove(KEY_ANONYMOUS_PROFILE) }
    }

    override fun getAnonymousProfileId(): String? {
        return prefs.getString(KEY_ANONYMOUS_PROFILE, null)
    }

    override fun saveDeviceToken(token: String) {
        prefs.edit { putString(KEY_DEVICE_TOKEN, token) }
    }

    override fun getDeviceToken(): String? {
        return runCatching { prefs.getString(KEY_DEVICE_TOKEN, null) }.getOrNull()
    }

    override var httpRequestsPauseEnds: Date?
        get() = prefs.getDate(KEY_HTTP_PAUSE_ENDS)
        set(value) {
            prefs.edit { putDate(KEY_HTTP_PAUSE_ENDS, value) }
        }
}
