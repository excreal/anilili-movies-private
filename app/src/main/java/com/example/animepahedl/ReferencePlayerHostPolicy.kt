package com.example.animepahedl

/** Host decisions for landscape-only providers, kept free of Android and Compose APIs. */
internal object ReferencePlayerHostPolicy {
    fun startsFullscreen(provider: SiteDownloadProvider, isTv: Boolean): Boolean =
        !provider.isShortDrama || isTv

    fun allowsFullscreenToggle(provider: SiteDownloadProvider): Boolean =
        provider.isShortDrama

    fun closesOnBack(provider: SiteDownloadProvider, fullscreen: Boolean, isTv: Boolean, inPip: Boolean): Boolean =
        !provider.isShortDrama || !fullscreen || isTv || inPip

    fun forcesLandscape(provider: SiteDownloadProvider, fullscreen: Boolean, isTv: Boolean): Boolean =
        !isTv && (!provider.isShortDrama || fullscreen)
}
