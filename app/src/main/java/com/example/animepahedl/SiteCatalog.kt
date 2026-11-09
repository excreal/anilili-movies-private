package com.example.animepahedl

/** Browsing adapters return the same canonical detail links accepted by the downloader. */
@androidx.annotation.Keep
internal data class CatalogItem(val title: String, val url: String, val poster: String, val wideArtwork: Boolean = false)
@androidx.annotation.Keep
internal data class CatalogPage(val items: List<CatalogItem>, val nextUrl: String? = null)
internal interface SiteCatalog {
    suspend fun home(): CatalogPage
    suspend fun search(query: String): CatalogPage
    suspend fun page(url: String): CatalogPage
    suspend fun filter(kind: String, term: SiteShowTerm): CatalogPage = page(term.url
        ?: throw java.io.IOException("This source does not provide a filter route for that term."))
}
