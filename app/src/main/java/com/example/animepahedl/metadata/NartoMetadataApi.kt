package com.example.animepahedl.metadata

import com.example.animepahedl.*

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/** Public Narto HTML/JSON metadata, separate from playback resolution and the UI card model. */
internal class NartoMetadataApi(cookieHeader: ((String) -> String)? = null) : SiteMetadataApi {
    override val site = MetadataSite.NARTO
    private val http = MetadataHttp(setOf("narto-drama.com", "www.narto-drama.com", "edge.narto-drama.com"), cookieHeader)
    private val origin = "https://edge.narto-drama.com/".toHttpUrl()

    override suspend fun browse(query: MetadataBrowseQuery): MetadataPage {
        require(query.page > 0) { "Page must be positive." }
        val allowed = setOf("lang", "type", "category", "tag", "genre", "provider", "tab-provider", "type_context", "q", "limit", "only_tab", "pinedrama_category_id", "flareflow_class_id")
        require(query.filters.keys.all { it in allowed || it.matches(Regex("tab_pages\\[[a-zA-Z0-9_-]+]")) }) { "Unsupported Narto metadata filter." }
        require(query.url == null || (query.query.isBlank() && query.filters.isEmpty() && query.page == 1)) { "A page URL already carries its search, filters and pagination." }
        val provider = query.filters["provider"] ?: query.filters["tab-provider"]
        if (provider != null && query.url == null) {
            val parameters = query.filters.filterKeys { it !in setOf("provider", "tab-provider", "lang") }.toMutableMap()
            if (query.query.isNotBlank()) parameters["q"] = query.query.trim()
            val payload = providerSections(provider, query.filters["lang"] ?: "en-US", query.page, parameters)
            return jsonPage(com.google.gson.JsonParser.parseString(payload.json).asJsonObject, payload.url, payload)
        }
        require(query.filters.keys.all { it in setOf("lang", "type", "category", "tag", "genre", "limit") }) { "This filter requires provider browse." }
        require(!(query.filters.containsKey("tag") && query.filters.containsKey("genre"))) { "Use one tag or genre route at a time." }
        require(query.query.isBlank() || (!query.filters.containsKey("tag") && !query.filters.containsKey("genre"))) { "Search and taxonomy routes are separate endpoints." }
        require(!query.filters.containsKey("limit") || query.query.isNotBlank()) { "The limit filter applies to search only." }
        val target = query.url?.let(http::checked) ?: origin.newBuilder()
            .apply {
                when {
                    query.query.isNotBlank() -> encodedPath("/search").addQueryParameter("q", query.query.trim())
                    query.filters["tag"] != null -> addPathSegment("tag").addPathSegment(query.filters.getValue("tag"))
                    query.filters["genre"] != null -> addPathSegment("genre").addPathSegment(query.filters.getValue("genre"))
                }
            }
            .addQueryParameter("lang", query.filters["lang"] ?: "en-US")
            .apply {
                if (query.page > 1) addQueryParameter("page", query.page.toString())
                query.filters.filterKeys { it !in setOf("lang", "tag", "genre") }.forEach { (key, value) -> addQueryParameter(key, value) }
            }.build()
        val response = http.get(target.toString())
        if (response.text.trimStart().startsWith("{")) return jsonPage(response.json().asJsonObject, response.url, response.payload("searchOrProviderPage"))
        val doc = response.document()
        val cards = doc.select("article.card[data-watch-url]").mapNotNull(::card)
        val heroes = doc.select(".nd-hero-card").mapNotNull(::hero)
        val related = related(doc)
        val generic = metadataFromDocument(doc)
        val items = cards.ifEmpty { schemaListItems(doc) }.ifEmpty { related }
        if (items.isEmpty() && heroes.isEmpty() && doc.select("section.grid,.no-results,.empty-state,.search-no-results").isEmpty())
            throw java.io.IOException("Narto returned an unrecognized metadata page.")
        val next = doc.select("a.pager-link, a[rel=next]").firstOrNull {
            it.text().equals("Next", true) || it.attr("rel") == "next"
        }?.absUrl("href")?.let(::safePage)
        val sections = buildList {
            if (heroes.isNotEmpty()) add(MetadataSection("Featured", heroes, response.url))
            if (items.isNotEmpty()) add(MetadataSection(doc.selectFirst("h1")?.text().orEmpty().ifBlank { "Browse" }, items, response.url))
        }
        return MetadataPage(items, nextUrl = next, sections = sections,
            filters = taxonomyFrom(doc).groups, payloads = generic.payloads + pageFacts(doc, response.url))
    }

    /** Two HTML requests at most: the detail and its first episode's complete episodeItemsRaw. */
    override suspend fun details(url: String): MetadataDetails {
        val target = http.checked(url)
        val (slug, _) = dramaRoute(target, SiteDownloadProvider.NARTO)
        val detailUrl = target.newBuilder().encodedPath("/detail/watch/$slug").build().toString()
        val response = http.get(detailUrl)
        val doc = response.document()
        val base = record(doc)
        var episodes = episodeAnchors(doc)
        var payloads = base.payloads + pageFacts(doc, response.url)
        if (episodes.isNotEmpty()) {
            // The page carries all episode metadata in one document; never request every episode.
            val hydration = runCatching { http.get(episodes.first().url) }
            hydration.exceptionOrNull()?.let { if (it is kotlinx.coroutines.CancellationException) throw it }
            hydration.getOrNull()?.let { page ->
                val raw = episodeItems(page.text)
                if (raw.isNotEmpty()) episodes = raw.map { episodeRecord(it, page.url) }
                payloads = payloads + metadataFromDocument(page.document()).payloads +
                    raw.takeIf { it.isNotEmpty() }?.let { listOf(metadataPayload("episodeItemsRaw", page.url, com.google.gson.JsonArray().apply { it.forEach(::add) })) }.orEmpty() +
                    publicPlayerConfiguration(page.text, page.url)
            }
            if (hydration.isFailure) payloads += metadataPayload("metadataAvailability", detailUrl, JsonObject().apply {
                addProperty("episodeHydration", "unavailable; episode anchors retained")
            })
        }
        val counts = base.attributes.toMutableMap()
        if (episodes.any { "is_playable" in it.attributes }) counts["playableEpisodeCount"] = episodes.count { it.attributes["is_playable"] == "true" }.toString()
        return MetadataDetails(base.copy(attributes = counts), episodes, related(doc), payloads = payloads)
    }

    override suspend fun episode(url: String): MetadataEpisode {
        val target = http.checked(url)
        val (_, number) = dramaRoute(target, SiteDownloadProvider.NARTO)
        require(number != null) { "Select a Narto episode URL." }
        val response = http.get(target.toString())
        val doc = response.document()
        val raw = episodeItems(response.text).firstOrNull { it.dramaInt("route_episode_number") == number }
            ?: throw java.io.IOException("Narto did not expose metadata for this episode.")
        val generic = metadataFromDocument(doc)
        val item = episodeRecord(raw, response.url)
        return item.copy(description = doc.selectFirst(".desktop-intro,.info-intro,.movie-desc")?.text()?.takeIf(String::isNotBlank) ?: generic.description,
            releaseDate = generic.releaseDate, durationSeconds = generic.durationSeconds,
            images = (item.images + generic.images).distinctBy { it.role to it.url },
            payloads = item.payloads + generic.payloads + publicPlayerConfiguration(response.text, response.url))
    }

    override suspend fun taxonomy(): MetadataTaxonomy {
        val response = http.get(origin.newBuilder().addQueryParameter("lang", "en-US").build().toString())
        val doc = response.document()
        val taxonomy = taxonomyFrom(doc)
        val providerPayload = providerSections("")
        val data = com.google.gson.JsonParser.parseString(providerPayload.json).asJsonObject
        val providers = data.dramaArray("providers").mapNotNull { value ->
            val item = value.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
            item.dramaString("key").takeIf(String::isNotBlank)?.let { key -> MetadataTerm(item.dramaString("label"), key,
                origin.newBuilder().addQueryParameter("lang", "en-US").addQueryParameter("tab-provider", key).build().toString()) }
        }
        return taxonomy.copy(groups = taxonomy.groups + ("providers" to providers),
            payloads = metadataFromDocument(doc).payloads + pageFacts(doc, response.url) + providerPayload)
    }

    /** GET-only extension for public provider browse sections, retained even when UI ignores it. */
    suspend fun providerSections(provider: String, language: String = "en-US", page: Int = 1,
        parameters: Map<String, String> = emptyMap()): MetadataPayload {
        require((provider.isEmpty() || provider.matches(Regex("[a-z0-9_-]+"))) && page > 0)
        val supported = setOf("q", "type_context", "only_tab", "pinedrama_category_id", "flareflow_class_id")
        require(parameters.keys.all { it in supported || it.matches(Regex("tab_pages\\[[a-zA-Z0-9_-]+]")) }) { "Unsupported Narto provider filter." }
        val target = origin.newBuilder().encodedPath("/home/providers/sections")
            .addQueryParameter("provider", provider).addQueryParameter("lang", language)
            .addQueryParameter("target_lang", language)
            .apply { parameters.filterKeys { it !in setOf("provider", "lang", "target_lang", "page") }.forEach { (k, v) -> addQueryParameter(k, v) } }.build()
        val response = http.get(target.toString())
        val data = response.json().asJsonObject
        require(data.dramaString("ok") == "true") { "Narto provider metadata is unavailable." }
        if (page > 1 && parameters.keys.none { it.startsWith("tab_pages[") }) {
            val pages = data.dramaArray("sections").mapNotNull { it.takeIf { v -> v.isJsonObject }?.asJsonObject?.dramaString("tab_key") }.filter(String::isNotBlank)
            val paged = target.newBuilder().apply { pages.forEach { addQueryParameter("tab_pages[$it]", page.toString()) } }.build()
            return http.get(paged.toString()).payload("providerSections")
        }
        return response.payload("providerSections")
    }


    private fun jsonPage(data: JsonObject, url: String, payload: MetadataPayload): MetadataPage {
        require(data.dramaString("ok") == "true" && (data.has("items") || data.has("sections"))) { "Narto returned an invalid metadata page." }
        val sections = data.dramaArray("sections").mapNotNull { value ->
            val section = value.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
            MetadataSection(section.dramaString("tab_label", "tab_key"), section.dramaArray("items").mapNotNull { jsonCard(it, url) }, url)
        }
        val direct = data.dramaArray("items").mapNotNull { jsonCard(it, url) }
        val items = (direct + sections.flatMap { it.items }).distinctBy { it.url }
        val providers = data.dramaArray("providers").mapNotNull { value -> value.takeIf { it.isJsonObject }?.asJsonObject?.let {
            MetadataTerm(it.dramaString("label"), it.dramaString("key"))
        } }
        val filters = if (providers.isEmpty()) emptyMap() else mapOf("providers" to providers)
        return MetadataPage(items, sections = sections, filters = filters, payloads = listOf(payload))
    }

    private fun jsonCard(value: JsonElement, base: String): MetadataRecord? {
        val data = value.takeIf { it.isJsonObject }?.asJsonObject ?: return null
        val title = data.dramaString("title")
        val url = base.toHttpUrl().resolve(data.dramaString("url", "watch_url"))?.toString()?.let(::safePage) ?: return null
        if (title.isBlank()) return null
        val poster = data.dramaString("poster_url").takeIf(String::isNotBlank)?.let { base.toHttpUrl().resolve(it)?.toString() }
        val language = data.dramaString("language_code").takeIf(String::isNotBlank)
        val tags = (data.dramaArray("tags") + data.dramaArray("tag_names")).mapNotNull {
            it.takeIf { node -> node.isJsonPrimitive }?.asString?.takeIf(String::isNotBlank)?.let { name -> MetadataTerm(name) }
        }.distinctBy { it.name }
        return MetadataRecord(data.dramaString("id", "book_id").ifBlank { url }, url, title,
            description = data.dramaString("description").takeIf(String::isNotBlank),
            images = poster?.let { listOf(MetadataImage(it, "poster")) }.orEmpty(), tags = tags,
            categories = data.dramaString("category_name").takeIf(String::isNotBlank)?.let { listOf(MetadataTerm(it)) }.orEmpty(),
            languages = listOfNotNull(language),
            attributes = data.entrySet().filter { it.value.isJsonPrimitive }.associate { it.key to it.value.asString },
            payloads = listOf(metadataPayload("catalogueItem", base, data)))
    }

    private fun record(doc: Document): MetadataRecord {
        val generic = metadataFromDocument(doc)
        val visibleTitle = doc.selectFirst(".movie-title,.desktop-title,.info-title,h1")?.text()?.takeIf(String::isNotBlank)
        val poster = doc.selectFirst(".movie-meta img.poster")?.let { image(it, "poster") }
        val count = Regex("(\\d+)\\s*Episodes?", RegexOption.IGNORE_CASE).find(doc.selectFirst(".movie-sub,.episode-panel h2")?.text().orEmpty())?.groupValues?.get(1)?.toIntOrNull()
        val id = doc.selectFirst("meta[name=nd-current-movie-id]")?.attr("content")?.takeIf(String::isNotBlank) ?: generic.id
        val attributes = generic.attributes.toMutableMap()
        count?.let { attributes["declaredEpisodeCount"] = it.toString() }
        Regex("Playable:\\s*(\\d+)\\s*\\|\\s*Total DB:\\s*(\\d+)").find(doc.html())?.let {
            attributes["playableEpisodeCountAtDetailRender"] = it.groupValues[1]
            attributes["databaseEpisodeCount"] = it.groupValues[2]
        }
        val linkedTerms = doc.select(".movie-tags a[href]").mapNotNull(::term).distinctBy { it.url ?: it.name }
        val classified = classifyNartoLinkedTerms(linkedTerms)
        return generic.copy(id = id, title = visibleTitle ?: generic.title,
            description = doc.selectFirst(".movie-desc")?.text()?.takeIf(String::isNotBlank) ?: generic.description,
            images = (listOfNotNull(poster) + generic.images).distinctBy { it.role to it.url },
            genres = (generic.genres + classified.genres).distinctBy { it.url ?: it.name },
            categories = (generic.categories + classified.categories).distinctBy { it.url ?: it.name },
            tags = (generic.tags + classified.tags).distinctBy { it.url ?: it.name },
            episodeCount = count ?: generic.episodeCount, attributes = attributes)
    }

    private fun card(element: Element): MetadataRecord? {
        val url = safePage(element.absUrl("data-watch-url")) ?: return null
        val title = element.attr("data-movie-title").ifBlank { element.selectFirst(".title")?.text().orEmpty() }
        if (title.isBlank()) return null
        val count = Regex("\\d+").find(element.selectFirst(".episode-badge")?.text().orEmpty())?.value?.toIntOrNull()
        val extra = element.attributes().asList().filter { it.key.startsWith("data-") }.associate { it.key to it.value } +
            mapOf("visibleMeta" to element.selectFirst(".movie-meta-line")?.text().orEmpty())
        return MetadataRecord(element.attr("data-movie-id").ifBlank { url }, url, title,
            images = listOfNotNull(element.selectFirst("img.poster,img")?.let { image(it, "poster") }),
            episodeCount = count,
            genres = element.select(".movie-tag[href]").mapNotNull(::term).filter { it.url?.contains("/genre/") == true },
            categories = element.select(".movie-tag[href]").mapNotNull(::term).filter { it.url?.contains("/category/") == true },
            tags = element.select(".movie-tag[href]").mapNotNull(::term).filter { it.url?.contains("/tag/") == true }, attributes = extra)
    }

    private fun hero(element: Element): MetadataRecord? {
        val url = element.selectFirst("a.nd-hero-art-link[href],a[href*=/detail/watch/]")?.absUrl("href")?.let(::safePage) ?: return null
        val title = element.selectFirst(".nd-hero-title")?.text().orEmpty()
        if (title.isBlank()) return null
        val facts = element.select(".nd-hero-facts span").map { it.text() }
        return MetadataRecord(url, url, title,
            description = element.selectFirst(".nd-hero-desc")?.text()?.takeIf(String::isNotBlank),
            images = listOfNotNull(element.selectFirst("img.nd-hero-art")?.let { image(it, "featured-poster") }),
            episodeCount = facts.firstOrNull()?.let { Regex("\\d+").find(it)?.value?.toIntOrNull() },
            tags = facts.drop(1).map { MetadataTerm(it) }, attributes = mapOf("featuredOrder" to element.attr("data-index")))
    }

    private fun related(doc: Document) = doc.select("a.you-may-like-card[href],a.you-may-like-item[href],a.js-related-item[href],a.related-item[href]").mapNotNull { a ->
        val url = safePage(a.absUrl("href")) ?: return@mapNotNull null
        val title = a.selectFirst(".you-may-like-name,.related-title")?.text().orEmpty().ifBlank { a.selectFirst("img")?.attr("alt").orEmpty() }
        title.takeIf(String::isNotBlank)?.let { MetadataRecord(url, url, it, images = listOfNotNull(a.selectFirst("img")?.let { img -> image(img, "poster") })) }
    }.distinctBy { it.url }

    private fun schemaListItems(doc: Document): List<MetadataRecord> {
        val items = linkedMapOf<String, MetadataRecord>()
        fun visit(node: JsonElement) {
            if (node.isJsonArray) node.asJsonArray.forEach(::visit)
            if (node.isJsonObject) {
                val o = node.asJsonObject
                if (o.dramaString("@type") == "ListItem") {
                    val url = safePage(o.dramaString("url")); val name = o.dramaString("name")
                    if (url != null && name.isNotBlank() && "/detail/watch/" in url) items[url] = MetadataRecord(url, url, name,
                        images = o.dramaString("image").takeIf(String::isNotBlank)?.let { listOf(MetadataImage(it, "poster")) }.orEmpty(),
                        payloads = listOf(metadataPayload("listItem", doc.location(), o)))
                }
                o.entrySet().forEach { visit(it.value) }
            }
        }
        doc.select("script[type=application/ld+json]").forEach { runCatching { com.google.gson.JsonParser.parseString(it.data()) }.getOrNull()?.let(::visit) }
        return items.values.toList()
    }

    private fun episodeAnchors(doc: Document) = doc.select("a.episode-item[href]").mapNotNull { a ->
        val url = a.absUrl("href").let(::safePage) ?: return@mapNotNull null
        val number = runCatching { dramaRoute(url.toHttpUrl(), SiteDownloadProvider.NARTO).second }.getOrNull() ?: return@mapNotNull null
        MetadataEpisode(url, number.toString(), a.attr("title").ifBlank { a.text() }, url)
    }.distinctBy { it.number }

    private fun episodeItems(html: String): List<JsonObject> = dramaJsonAfter(html, Regex("const\\s+episodeItemsRaw\\s*=\\s*"))
        ?.takeIf { it.isJsonArray }?.asJsonArray?.filter { it.isJsonObject }?.map { it.asJsonObject }.orEmpty()

    private fun episodeRecord(raw: JsonObject, base: String): MetadataEpisode {
        val number = raw.dramaString("route_episode_number", "number")
        fun absolute(value: String) = value.takeIf(String::isNotBlank)?.let { base.toHttpUrl().resolve(it)?.toString() }
        val url = absolute(raw.dramaString("watch_url")) ?: base
        val tracks = raw.dramaArray("multi_subtitles").mapNotNull { value ->
            val o = value.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
            val trackUrl = absolute(o.dramaString("subtitle_url")) ?: return@mapNotNull null
            MetadataSubtitle(trackUrl, o.dramaString("label").ifBlank { o.dramaString("language_code") },
                o.dramaString("language_code").takeIf(String::isNotBlank), isDefault = o.dramaString("is_default") == "true",
                attributes = o.entrySet().filter { it.value.isJsonPrimitive }.associate { it.key to it.value.asString })
        }.toMutableList()
        listOf("subtitle_url", "direct_subtitle_url").forEach { field ->
            absolute(raw.dramaString(field))?.takeIf { candidate -> tracks.none { it.url == candidate } }?.let { trackUrl ->
                tracks += MetadataSubtitle(trackUrl, "Subtitle", raw.dramaString("selected_subtitle_language").takeIf(String::isNotBlank), attributes = mapOf("sourceField" to field))
            }
        }
        return MetadataEpisode(raw.dramaString("id").ifBlank { url }, number, raw.dramaString("title"), url,
            images = absolute(raw.dramaString("thumb_url"))?.let { listOf(MetadataImage(it, "episode-snapshot")) }.orEmpty(), subtitles = tracks,
            attributes = raw.entrySet().filter { it.value.isJsonPrimitive }.associate { it.key to it.value.asString },
            payloads = listOf(metadataPayload("episode", base, raw)))
    }

    private fun publicPlayerConfiguration(html: String, url: String): List<MetadataPayload> {
        val result = JsonObject()
        for (name in listOf("movieTitle", "movieLanguageCode", "currentPageLanguageCode", "moviePosterUrl", "movieShareImageUrl",
            "movieDetailUrl", "initialEpisodeDbId", "initialEpisodeNumber", "playerExtrasUrl", "checkNewEpisodesUrl",
            "subtitleBuildSupportedLanguages", "aiSubtitleAllowedLangs", "subtitleBuildEndpointTemplate")) {
            dramaJsonAfter(html, Regex("(?:const|let)\\s+${Regex.escape(name)}\\s*=\\s*"))?.let { result.add(name, it) }
        }
        return if (result.size() > 0) listOf(metadataPayload("publicPlayerConfiguration", url, result)) else emptyList()
    }

    private fun taxonomyFrom(doc: Document): MetadataTaxonomy {
        val links = doc.select("a[href]")
        fun group(predicate: (okhttp3.HttpUrl) -> Boolean) = links.filter { a -> runCatching { predicate(a.absUrl("href").toHttpUrl()) }.getOrDefault(false) }
            .mapNotNull(::term).distinctBy { it.url ?: it.name }
        val groups = linkedMapOf(
            "genres" to group { it.encodedPath.startsWith("/genre/") },
            "tags" to group { it.encodedPath.startsWith("/tag/") },
            "categories" to group { it.queryParameter("category") != null },
            "providers" to group { it.encodedPath.startsWith("/provider/") || it.queryParameter("provider") != null },
            "types" to group { it.queryParameter("type") != null },
        )
        groups["languages"] = doc.select("link[rel=alternate][hreflang][href]").map { link ->
            MetadataTerm(link.attr("hreflang"), link.attr("hreflang"), link.absUrl("href"))
        }.distinctBy { it.id }
        return MetadataTaxonomy(groups.filterValues { it.isNotEmpty() })
    }

    private fun pageFacts(doc: Document, url: String): List<MetadataPayload> {
        val facts = JsonObject()
        doc.select(".site-footer-stat,.footer-stat,.pager-text").forEachIndexed { i, element -> facts.addProperty("visibleFact$i", element.text()) }
        doc.select("[data-sections-url]").firstOrNull()?.let { element ->
            facts.add("providerExplorer", JsonObject().apply { element.attributes().asList().filter { it.key.startsWith("data-") }.forEach { addProperty(it.key, it.value) } })
        }
        return if (facts.size() > 0) listOf(metadataPayload("pageFacts", url, facts)) else emptyList()
    }

    private fun term(element: Element): MetadataTerm? {
        val url = safePage(element.absUrl("href")) ?: return null
        val title = element.text().trim().removePrefix("#")
        return title.takeIf(String::isNotBlank)?.let { MetadataTerm(it, url.toHttpUrl().pathSegments.lastOrNull()?.takeIf(String::isNotBlank), url) }
    }
    private fun image(element: Element, role: String): MetadataImage? = element.absUrl("src").takeIf(String::isNotBlank)?.let {
        MetadataImage(it, role, element.attr("width").toIntOrNull(), element.attr("height").toIntOrNull(), caption = element.attr("alt").takeIf(String::isNotBlank))
    }
    private fun safePage(raw: String): String? = runCatching { http.checked(raw).toString() }.getOrNull()
}

internal data class NartoTermGroups(val genres: List<MetadataTerm>, val categories: List<MetadataTerm>, val tags: List<MetadataTerm>)
internal fun classifyNartoLinkedTerms(terms: List<MetadataTerm>): NartoTermGroups = NartoTermGroups(
    genres = terms.filter { it.url?.toHttpUrlOrNull()?.encodedPath?.contains("/genre/") == true },
    categories = terms.filter { it.url?.toHttpUrlOrNull()?.encodedPath?.contains("/category/") == true },
    tags = terms.filter { it.url?.toHttpUrlOrNull()?.encodedPath?.contains("/tag/") == true },
)

private fun String.toHttpUrlOrNull() = runCatching { toHttpUrl() }.getOrNull()
