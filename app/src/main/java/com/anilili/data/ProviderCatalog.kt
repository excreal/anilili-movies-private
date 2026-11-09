package com.anilili.data

/** Provider capabilities adapted to this host's real media sources. */
internal object ProviderCatalog {
    // These sources do not advertise anime SUB/DUB semantics. Native track selection and
    // explicit audio-language preferences remain available without assuming Japanese audio.
    val multiAudioProviders = emptySet<String>()
    fun isEmbed(provider: String): Boolean = false
    fun label(provider: String): String = when (provider.uppercase()) {
        "KISSKH" -> "KissKH"; "EROTICMV" -> "EroticMV"; else -> provider
    }
}
