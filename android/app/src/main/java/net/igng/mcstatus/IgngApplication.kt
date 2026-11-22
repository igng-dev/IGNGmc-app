package net.igng.mcstatus

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import net.igng.mcstatus.core.data.IgngAppContainer
import net.igng.mcstatus.core.model.SiteEndpoints

class IgngApplication : Application(), SingletonImageLoader.Factory {
    lateinit var container: IgngAppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = IgngAppContainer(
            appContext = this,
            endpoints = SiteEndpoints(
                apiBaseUrl = BuildConfig.API_BASE_URL,
                mcBaseUrl = BuildConfig.MC_BASE_URL,
                dashBaseUrl = BuildConfig.DASH_BASE_URL,
                ssoBaseUrl = BuildConfig.SSO_BASE_URL,
                chatBaseUrl = BuildConfig.CHAT_BASE_URL,
                wikiBaseUrl = BuildConfig.WIKI_BASE_URL,
                mapRelayUrl = BuildConfig.MAP_RELAY_URL,
            ),
            clientVersion = BuildConfig.VERSION_NAME,
        )
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader = container.imageLoader
}
