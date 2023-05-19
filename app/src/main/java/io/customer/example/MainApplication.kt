package io.customer.example

import android.app.Application
import build.gist.GistEnvironment
import build.gist.presentation.GistSdk
import io.customer.sdk.CustomerIO

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // For demo purposes only, to be removed.
        val gistSdk = GistSdk.getInstance()
        gistSdk.init(this, "a5ec106751ef4b34a0b9", "eu", GistEnvironment.PROD)

        // Set current user ID
        gistSdk.setUserToken("ABC123")

        CustomerIO.Builder(
            siteId = "YOUR-SITE-ID",
            apiKey = "YOUR-API-KEY",
            appContext = this
        ).build()
    }
}
