package com.example.animepahedl.metadata

import com.example.animepahedl.*

import android.content.Context
import androidx.annotation.Keep
import okhttp3.HttpUrl.Companion.toHttpUrl

/** Stable entry point for data callers. Instances retain each provider's HTTP/snapshot session. */
@Keep
internal class SiteMetadataApis(private val cookieHeader: ((String) -> String)? = null) {
    private val adapters by lazy { mapOf(
        MetadataSite.DRAMATIP to DramatipMetadataApi(cookieHeader),
        MetadataSite.KISS18 to Kiss18MetadataApi(cookieHeader),
        MetadataSite.NARTO to NartoMetadataApi(cookieHeader),
        MetadataSite.EROTICMV to EroticMvMetadataApi(cookieHeader),
        MetadataSite.KISSKH to KisskhMetadataApi(cookieHeader),
    ) }
    operator fun get(site: MetadataSite): SiteMetadataApi = adapters.getValue(site)
    fun forProvider(provider: SiteDownloadProvider): SiteMetadataApi = get(when (provider) {
        SiteDownloadProvider.DRAMATIP -> MetadataSite.DRAMATIP
        SiteDownloadProvider.KISS18 -> MetadataSite.KISS18
        SiteDownloadProvider.NARTO -> MetadataSite.NARTO
        SiteDownloadProvider.EROTICMV -> MetadataSite.EROTICMV
        SiteDownloadProvider.KISSKH -> MetadataSite.KISSKH
        SiteDownloadProvider.DIRECT -> throw IllegalArgumentException("A direct media URL has no site catalogue. Use the media resolver.")
    })
    fun forUrl(raw: String): SiteMetadataApi {
        val url = raw.toHttpUrl()
        require(url.isHttps && url.username.isEmpty() && url.password.isEmpty()) { "Use a supported HTTPS site URL without credentials." }
        return forProvider(siteDownloadProvider(url))
    }
    companion object {
        /** Reuses existing imported sessions; reading metadata never exports or retains cookies. */
        fun forApp(context: Context): SiteMetadataApis {
            val store = SiteDownloadCookieStore(context.applicationContext)
            return SiteMetadataApis { url ->
                val browser = android.webkit.CookieManager.getInstance().getCookie(url).orEmpty()
                val saved = store.headerFor(url)
                if (saved.isBlank()) browser else if (browser.isBlank()) saved else "$saved; $browser"
            }
        }
    }
}
