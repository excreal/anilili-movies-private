package com.anilili.ui.nav
internal object Routes {
    const val EXTRA_ROUTE = "com.example.animepahedl.REFERENCE_PLAYER_ROUTE"
    fun watch(animeId: Int, provider: String, category: String, episode: String) = "reference-player/$animeId/$episode"
}
