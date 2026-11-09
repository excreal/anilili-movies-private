package com.example.animepahedl.metadata

import com.example.animepahedl.*

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import java.io.IOException

/** WordPress/VidoRev public metadata. Viewing metadata never votes, posts or starts a video. */
internal class EroticMvMetadataApi(cookieHeader: ((String) -> String)? = null) : SiteMetadataApi {
    override val site = MetadataSite.EROTICMV
    private val http = MetadataHttp(setOf("eroticmv.com", "www.eroticmv.com"), cookieHeader,
        accept = "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
    private val origin = "https://eroticmv.com/".toHttpUrl()

    override suspend fun browse(query: MetadataBrowseQuery): MetadataPage {
        val supported = setOf("genre", "country", "decade", "tag", "actor", "director", "series", "sort", "letter")
        require(query.filters.keys.all { it in supported }) { "Unsupported EroticMV metadata filter." }
        require(query.page > 0) { "Page must be positive." }
        require(query.url == null || query.query.isBlank() && query.filters.isEmpty() && query.page == 1) { "Use an explicit URL or query/filter/page, not both." }
        val paths = query.filters.filterKeys { it in setOf("genre", "country", "decade", "tag", "actor", "director", "series") }
        require(paths.size <= 1 && (paths.isEmpty() || query.query.isBlank())) { "Choose one taxonomy/person/series route; search cannot be combined with it." }
        val path = paths.entries.firstOrNull()?.let { (key, value) ->
            if (key == "series") { require(value == "true") { "series must be true." }; "/series/" }
            else {
                require(value.matches(Regex("[\\p{L}\\p{N}_.%~-]+"))) { "Invalid taxonomy slug." }
                when (key) { "genre", "country" -> "/category/$key/$value/"; "decade" -> "/category/decades/$value/"; else -> "/$key/$value/" }
            }
        } ?: if (query.filters.containsKey("sort") || query.filters.containsKey("letter")) "/category/decades/" else "/"
        val sort = query.filters["sort"]
        require(sort == null || sort in setOf("view", "like")) { "EroticMV advertises sort=view or sort=like." }
        val letter = query.filters["letter"]
        require(letter == null || letter.matches(Regex("[A-Za-z0-9]"))) { "letter must be one letter or digit." }
        val target = query.url?.let(http::checked) ?: origin.newBuilder()
            .encodedPath(if (query.page == 1) path else "${path}page/${query.page}/")
            .apply {
                if (query.query.isNotBlank()) addQueryParameter("s", query.query.trim())
                if (sort != null) addQueryParameter("archive_query", sort)
                if (letter != null) addQueryParameter("alphabet_filter", letter)
            }.build()
        val response = http.get(target.toString())
        return parseEroticMvMetadataPage(response.text, response.url)
    }

    override suspend fun details(url: String): MetadataDetails {
        val target = http.checked(url)
        require(target.encodedPath.trim('/').isNotBlank() && !target.encodedPath.startsWith("/wp-admin")) { "Choose a movie/person/series page." }
        val response = http.get(target.toString())
        return parseEroticMvMetadataDetails(response.text, response.url)
    }

    override suspend fun episode(url: String): MetadataEpisode {
        val result = details(url)
        return result.episodes.firstOrNull() ?: throw IOException("This EroticMV page did not advertise a movie player.")
    }

    override suspend fun taxonomy(): MetadataTaxonomy {
        val response = http.get(origin.toString())
        val groups = eroticTaxonomy(response.document())
        return MetadataTaxonomy(groups, listOf(metadataPayload("navigation-taxonomy", response.url,
            com.google.gson.Gson().toJsonTree(groups))))
    }

    /** The advertised public RSS is a separate read surface, not a silently substituted HTML page.
     * It can expose complete descriptions, publish dates, authors and native vote counts even when
     * the normal page requires browser verification. Explicit source feed links are accepted. */
    suspend fun feed(url: String = "https://eroticmv.com/feed/"): MetadataPage {
        val target = http.checked(url)
        require(target.pathSegments.contains("feed")) { "Choose an advertised EroticMV feed URL." }
        val response = http.get(target.toString())
        return parseEroticMvMetadataFeed(response.text, response.url)
    }
}

internal fun parseEroticMvMetadataPage(html: String, url: String): MetadataPage {
    val doc = Jsoup.parse(html, url)
    val cards = doc.select("article.post-item").mapNotNull(::eroticCard)
    if (cards.isEmpty() && doc.select(".no-results, .nothing-found").isEmpty()) throw IOException("EroticMV returned no readable metadata cards.")
    val sections = doc.select(".widget, section").mapNotNull { section ->
        val name = section.selectFirst(".widget-title, h2, h3")?.text().orEmpty()
        val items = section.select("article.post-item").mapNotNull(::eroticCard)
        if (name.isBlank() || items.isEmpty()) null else MetadataSection(name, items)
    }
    val navigation = JsonObject().apply {
        addProperty("pagesDisplay", doc.selectFirst(".wp-pagenavi .pages")?.text())
        add("pages", JsonArray().apply { doc.select(".wp-pagenavi a[href]").forEach { link -> add(JsonObject().apply {
            addProperty("label", link.text()); addProperty("url", link.absUrl("href")); addProperty("relation", link.attr("rel"))
        }) } })
    }
    val primary = doc.select("main article.post-item, #primary article.post-item").mapNotNull(::eroticCard).ifEmpty { cards }
    return MetadataPage(primary.distinctBy { it.id }, nextUrl = doc.selectFirst("a[rel=next], .nextpostslink")?.absUrl("href")?.let(::eroticSiteUrl),
        sections = sections, filters = eroticTaxonomy(doc), payloads = metadataFromDocument(doc).payloads + metadataPayload("pagination", url, navigation))
}

internal fun parseEroticMvMetadataDetails(html: String, url: String): MetadataDetails {
    val doc = Jsoup.parse(html, url)
    val base = metadataFromDocument(doc)
    val main = doc.selectFirst("main, #primary") ?: doc.body()
    val title = main.selectFirst("h1.entry-title, h1.post-title, h1")?.text().orEmpty().ifBlank { base.title.removePrefix("Watch ").removeSuffix(" - Erotic Movies") }
    val id = main.selectFirst("[id^=post-]")?.id()?.removePrefix("post-")
        ?: doc.selectFirst("[data-rater-postid]")?.attr("data-rater-postid") ?: base.id
    val body = main.selectFirst(".entry-content, .post-content, .video-description, [itemprop=description]")
    val rawBodyText = body?.text().orEmpty()
    val aliases = Regex("(?i)\\b(?:also\\s+known\\s+as|aka)\\b\\s*:?\\s*(.+?)(?=\\bdescription\\s*:|$)")
        .find(rawBodyText)?.groupValues?.get(1)?.split(Regex("\\s*,\\s*|\\s*;\\s*"))?.map(String::trim)
        ?.filter { it.isNotBlank() && it.length < 100 }.orEmpty()
    val description = body?.clone()?.apply {
        select("script,style,[class*=yasr],.star-rating,.rating,.ratings,form,.advertisement").remove()
    }?.select("p,li")?.map { cleanEroticDescription(it.text()) }
        ?.filter { it.isNotBlank() && !it.startsWith("The post ", true) }
        ?.distinct()?.joinToString("\n")?.takeIf(String::isNotBlank) ?: cleanEroticDescription(base.description.orEmpty()).ifBlank { null }
    val categories = main.select(".categories-wrap a[href], .cat-links a[href], a.category-item[href]").mapNotNull(::eroticTerm).distinctBy { it.url }
    val tags = main.select(".tags-links a[href], a[rel=tag][href]").mapNotNull(::eroticTerm).distinctBy { it.url }
    val people = main.select("a[href*=/actor/], a[href*=/director/]").mapNotNull { link ->
        val target = eroticSiteUrl(link.absUrl("href")) ?: return@mapNotNull null
        val name = link.selectFirst("h6, h5, .entry-title")?.text().orEmpty().ifBlank { link.text() }.trim()
        if (name.isBlank()) null else MetadataPerson(name, if (target.contains("/director/")) "director" else "actor", target,
            link.selectFirst("img")?.let { eroticImages(it, "person").firstOrNull()?.url })
    }.distinctBy { it.url }
    val images = (base.images + body?.select("img")?.flatMap { eroticImages(it, "content-image") }.orEmpty() +
        main.select(".video-player img, .player-wrapper img, .post-thumbnail img").flatMap { eroticImages(it, "video-poster") }).distinctBy { it.url to it.role }
    val publicPlayer = dramaJsonAfter(html, Regex("(?:var\\s+)?vidorev_jav_js_object\\s*=\\s*"))?.takeIf { it.isJsonObject }?.asJsonObject
    val player = JsonObject().apply { publicPlayer?.entrySet()?.forEach { (key, value) ->
        if (key.startsWith("single_video_") || key in setOf("single_media_subtitles", "vid_vtt_preview_vtt_file", "vid_preview_image", "player_library")) add(key, value)
    } }
    val subtitleTracks = (doc.select("track[src]").mapNotNull { track ->
        absoluteErotic(url, track.attr("src"))?.let { MetadataSubtitle(it, track.attr("label").ifBlank { track.attr("srclang") }, track.attr("srclang").takeIf(String::isNotBlank),
            track.attr("type").takeIf(String::isNotBlank), track.hasAttr("default"), mapOf("kind" to track.attr("kind"))) }
    } + player.get("single_media_subtitles")?.takeIf { it.isJsonArray }?.asJsonArray?.mapIndexedNotNull { index, value ->
        val item = value.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapIndexedNotNull null
        absoluteErotic(url, item.dramaString("src"))?.let { MetadataSubtitle(it, item.dramaString("label"), item.dramaString("srclang").takeIf(String::isNotBlank),
            isDefault = index == 0, attributes = mapOf("kind" to "source-subtitle")) }
    }.orEmpty()).distinctBy { it.url }
    val attributes = eroticStats(main).toMutableMap()
    body?.select("p,li,tr,dl")?.forEach { row ->
        val text = row.text()
        val match = Regex("^(Original title|AKA|Language|Country|Year|Runtime|Duration|Released|Studio|Production)\\s*[:：]\\s*(.+)$", RegexOption.IGNORE_CASE).matchEntire(text)
        if (match != null) attributes[match.groupValues[1]] = match.groupValues[2]
    }
    val payloads = base.payloads + listOf(metadataPayload("public-player", url, player),
        metadataPayload("visible-facts", url, com.google.gson.Gson().toJsonTree(attributes)))
    val ratings = (base.ratings + eroticRatings(main)).distinct()
    val record = base.copy(id = id, title = title, description = description, images = images,
        alternativeTitles = (base.alternativeTitles + aliases + listOfNotNull(attributes["AKA"], attributes["Original title"])).distinct(),
        categories = categories, genres = categories.filter { it.url?.contains("/category/genre/") == true }, tags = tags, episodeCount = null,
        countries = (base.countries + categories.filter { it.url?.contains("/category/country/") == true }.map { it.name }).distinct(),
        people = (base.people + people).distinctBy { it.name to it.role }, year = Regex("\\((\\d{4})\\)").find(title)?.groupValues?.get(1)?.toIntOrNull() ?: base.year,
        ratings = ratings, durationSeconds = metadataDurationSeconds(attributes["Runtime"] ?: attributes["Duration"]) ?: base.durationSeconds,
        externalIds = base.externalIds + eroticImdb(main), attributes = base.attributes + attributes, payloads = payloads,
        links = (base.links + doc.select("link[rel=alternate][href]").mapNotNull { link -> eroticSiteUrl(link.absUrl("href"))?.let { MetadataLink(it, "feed", link.attr("title")) } } +
            main.select(".preview-video[data-iframepreview]").mapNotNull { node -> eroticSiteUrl(node.absUrl("data-iframepreview"))?.let { MetadataLink(it, "preview") } }).distinct())
    val episode = MetadataEpisode(id, "1", title, record.url, description, images,
        durationSeconds = record.durationSeconds, releaseDate = record.releaseDate, subtitles = subtitleTracks,
        attributes = attributes, payloads = listOf(metadataPayload("public-player", url, player)))
    val related = main.select("article.post-item").mapNotNull(::eroticCard).filter { it.url != record.url }.distinctBy { it.id }
    val hasPlayer = player.size() > 0 || doc.select("video, #player-api-control, .player-3rdparty-control").isNotEmpty()
    return MetadataDetails(record, if (hasPlayer) listOf(episode) else emptyList(), related, payloads = payloads)
}

internal fun cleanEroticDescription(raw: String): String = org.jsoup.parser.Parser.unescapeEntities(raw, false)
    .replace(Regex("(?i)click\\s+to\\s+rate\\s+this\\s+post!?\\s*(?:\\[[^]]*])?"), " ")
    .replace(Regex("(?is)^\\s*(?:also\\s+known\\s+as|aka)\\b\\s*:?\\s*.*?\\bdescription\\s*:\\s*"), "")
    .replace(Regex("(?i)^\\s*description\\s*:\\s*"), "")
    .replace(Regex("(?i)\\[(?:total|average)\\s*:[^]]*]"), " ")
    .replace("\r\n", "\n").replace('\r', '\n')
    .replace('\u00a0', ' ').replace(Regex("[\\t\\x0B\\f ]+"), " ")
    .split('\n').map(String::trim).filter(String::isNotEmpty).joinToString("\n")

/** RSS preserves source XML fields and sanitized descriptive content, not voting nonces or forms. */
internal fun parseEroticMvMetadataFeed(xml: String, url: String): MetadataPage {
    val doc = Jsoup.parse(xml, url, Parser.xmlParser())
    require(doc.selectFirst("rss > channel") != null) { "EroticMV returned no RSS channel." }
    val items = doc.getElementsByTag("item").map { item ->
        fun field(tag: String) = item.getElementsByTag(tag).firstOrNull()?.text().orEmpty()
        val content = item.getElementsByTag("content:encoded").firstOrNull()?.text().orEmpty()
        val body = Jsoup.parseBodyFragment(content, url)
        val link = field("link")
        val guid = field("guid")
        val id = runCatching { guid.toHttpUrl().queryParameter("p") }.getOrNull() ?: guid
        val raw = JsonObject().apply { item.children().forEach { child ->
            if (child.tagName() != "content:encoded") {
                val values = getAsJsonArray(child.tagName()) ?: JsonArray().also { add(child.tagName(), it) }
                values.add(child.text())
            }
        } }
        val safeContent = body.clone().apply {
            select("script,style,form").remove()
            getAllElements().forEach { element -> element.attributes().asList().filter { attribute ->
                attribute.key.contains("nonce", true) || attribute.key.contains("token", true) || attribute.key.startsWith("on", true)
            }.forEach { element.removeAttr(it.key) } }
        }
        raw.addProperty("contentHtml", safeContent.body().html())
        val paragraphs = body.select("p").map { it.text() }.filter { it.isNotBlank() && !it.startsWith("The post ") }
        MetadataRecord(id, link, field("title"), description = paragraphs.joinToString("\n").takeIf(String::isNotBlank),
            images = body.select("img").flatMap { eroticImages(it, "content-image") },
            categories = item.getElementsByTag("category").map { MetadataTerm(it.text()) }, ratings = eroticRatings(body),
            people = listOfNotNull(field("dc:creator").takeIf(String::isNotBlank)?.let { MetadataPerson(it, "uploader") }),
            year = Regex("\\((\\d{4})\\)").find(field("title"))?.groupValues?.get(1)?.toIntOrNull(),
            releaseDate = field("pubDate").takeIf(String::isNotBlank),
            links = listOfNotNull(field("wfw:commentRss").takeIf(String::isNotBlank)?.let { MetadataLink(it, "comments-feed") }),
            attributes = mapOf("guid" to guid, "commentCount" to field("slash:comments")),
            payloads = listOf(metadataPayload("rss-item", url, raw)))
    }
    val envelope = JsonObject().apply { doc.selectFirst("channel")?.children()?.filter { it.tagName() != "item" }?.forEach { child ->
        if (child.children().isEmpty()) addProperty(child.tagName(), child.text())
    } }
    return MetadataPage(items, payloads = listOf(metadataPayload("rss-channel", url, envelope)))
}

private fun eroticCard(article: Element): MetadataRecord? {
    val anchor = article.selectFirst(".entry-title a[href], h2 a[href], h3 a[href]") ?: return null
    val url = eroticSiteUrl(anchor.absUrl("href")) ?: return null
    val title = anchor.text().trim().takeIf(String::isNotBlank) ?: return null
    val categories = article.select(".categories-wrap a[href], a.category-item[href]").mapNotNull(::eroticTerm).distinctBy { it.url }
    val attributes = eroticStats(article) + mapOf("cssClasses" to article.className())
    val payload = JsonObject().apply {
        add("facts", com.google.gson.Gson().toJsonTree(attributes))
        add("images", com.google.gson.Gson().toJsonTree(article.select("img").flatMap { eroticImages(it, "poster") }))
        article.select("[data-imdbdata]").forEach { element -> runCatching { JsonParser.parseString(element.attr("data-imdbdata")) }.getOrNull()?.let { add("imdb", it) } }
    }
    return MetadataRecord(article.id().removePrefix("post-").ifBlank { url }, url, title,
        images = article.select("img").flatMap { eroticImages(it, "poster") }.distinctBy { it.url },
        categories = categories, genres = categories.filter { it.url?.contains("/category/genre/") == true },
        countries = categories.filter { it.url?.contains("/category/country/") == true }.map { it.name },
        ratings = eroticRatings(article), durationSeconds = metadataDurationSeconds(attributes["durationDisplay"]), year = Regex("\\((\\d{4})\\)").find(title)?.groupValues?.get(1)?.toIntOrNull(), episodeCount = null,
        externalIds = eroticImdb(article), attributes = attributes, payloads = listOf(metadataPayload("card", url, payload)),
        links = article.select("[data-iframepreview]").mapNotNull { node -> eroticSiteUrl(node.absUrl("data-iframepreview"))?.let { MetadataLink(it, "preview") } })
}

private fun eroticRatings(element: Element): List<MetadataRating> = buildList {
    element.select("[data-imdbdata]").forEach { node ->
        val obj = runCatching { JsonParser.parseString(node.attr("data-imdbdata")).asJsonObject }.getOrNull()
        obj?.dramaString("rating")?.toDoubleOrNull()?.let { add(MetadataRating(it, 10.0, source = "IMDb", label = obj.dramaString("id"))) }
    }
    element.select(".yasr-rater-stars-vv[data-rating]").forEach { node ->
        val container = node.closest(".yasr-visitor-votes") ?: node.parent()
        val votes = container?.selectFirst("[id^=yasr-vv-votes-number-container]")?.text()?.toLongOrNull()
        node.attr("data-rating").toDoubleOrNull()?.let { add(MetadataRating(it, votes = votes, source = "EroticMV", label = "YASR visitor rating")) }
    }
    if (none { it.source == "EroticMV" }) element.select(".rating-average-dr").firstOrNull()?.ownText()?.trim()?.toDoubleOrNull()?.let {
        add(MetadataRating(it, source = "EroticMV", label = "Displayed visitor rating"))
    }
    if (none { it.source == "EroticMV" }) {
        Regex("(?i)Total\\s*:\\s*([0-9,]+)\\s*Average\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)")
            .find(element.text())?.let { match ->
                add(MetadataRating(match.groupValues[2].toDoubleOrNull(), votes = match.groupValues[1].replace(",", "").toLongOrNull(),
                    source = "EroticMV", label = "Published total/average"))
            }
    }
}
private fun eroticImdb(element: Element) = element.select("[data-title]").mapNotNull { node ->
    node.attr("data-title").takeIf { it.matches(Regex("tt[0-9]+")) }?.let { "imdb" to it }
}.toMap()
private fun eroticStats(element: Element): Map<String, String> = buildMap {
    for ((key, selector) in mapOf("viewsDisplay" to ".view-count", "likesDisplay" to ".like-count", "dislikesDisplay" to ".dislike-count", "commentsDisplay" to ".comment-count")) {
        element.selectFirst(selector)?.text()?.trim()?.takeIf(String::isNotBlank)?.let { value ->
            put(key, value)
            value.replace(",", "").toLongOrNull()?.let { put(key.removeSuffix("Display"), it.toString()) }
        }
    }
    element.selectFirst(".duration-text")?.clone()?.apply { select(".rating-average-dr").remove() }?.text()?.trim()?.takeIf(String::isNotBlank)?.let { put("durationDisplay", it) }
}
private fun eroticTaxonomy(doc: Document): Map<String, List<MetadataTerm>> = doc.select("a[href]").mapNotNull(::eroticTerm)
    .filter { it.url?.contains("/category/") == true || it.url?.contains("/tag/") == true }
    .distinctBy { it.url }.groupBy { term ->
        val target = term.url.orEmpty()
        when { target.contains("/category/genre/") -> "genres"; target.contains("/category/country/") -> "countries";
            target.contains("/category/decades/") -> "decades"; target.contains("/tag/") -> "tags"; else -> "categories" }
    }
private fun eroticTerm(link: Element): MetadataTerm? {
    val target = eroticSiteUrl(link.absUrl("href")) ?: return null
    val name = link.text().trim().takeIf(String::isNotBlank) ?: return null
    return MetadataTerm(name, link.attr("data-cat-id").takeIf(String::isNotBlank), target)
}
private fun eroticSiteUrl(raw: String): String? = runCatching { raw.toHttpUrl() }.getOrNull()?.takeIf {
    it.isHttps && it.host in setOf("eroticmv.com", "www.eroticmv.com") && it.username.isEmpty() && it.password.isEmpty()
}?.toString()
private fun absoluteErotic(base: String, raw: String): String? = raw.takeIf(String::isNotBlank)?.let { base.toHttpUrl().resolve(it) }
    ?.takeIf { it.scheme in setOf("http", "https") && it.username.isEmpty() && it.password.isEmpty() }?.toString()
private fun eroticImages(image: Element, role: String): List<MetadataImage> = buildList {
    val caption = image.attr("alt").takeIf(String::isNotBlank)
    for (name in listOf("data-src", "data-lazy-src", "data-original", "src")) {
        val raw = image.attr(name)
        if (!raw.contains("placeholder", true)) absoluteErotic(image.baseUri(), raw)?.let { add(MetadataImage(it, role,
            image.attr("width").toIntOrNull(), image.attr("height").toIntOrNull(), caption = caption)) }
    }
    for (name in listOf("data-srcset", "srcset")) image.attr(name).split(Regex(",\\s*(?=https?://|/)"))
        .mapNotNull { Regex("^(.+?)\\s+(\\d+)w$").matchEntire(it.trim()) }.forEach { match ->
            absoluteErotic(image.baseUri(), match.groupValues[1])?.let { add(MetadataImage(it, role, width = match.groupValues[2].toIntOrNull(), caption = caption)) }
        }
}.distinctBy { it.url to it.width }
