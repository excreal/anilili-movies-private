package com.example.animepahedl.metadata

import com.example.animepahedl.*

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/** Shared public standards only. Provider adapters add their verified page/API-specific fields. */
internal fun metadataFromHtml(html: String, url: String): MetadataRecord = metadataFromDocument(Jsoup.parse(html, url))

internal fun metadataFromDocument(doc: Document): MetadataRecord {
    val url = doc.location()
    fun absolute(raw: String): String? = raw.trim().takeIf(String::isNotBlank)?.let { value ->
        url.toHttpUrlOrNull()?.resolve(value)?.takeIf { it.scheme in setOf("http", "https") && it.username.isEmpty() && it.password.isEmpty() }?.toString()
    }
    val meta = linkedMapOf<String, MutableList<String>>()
    doc.select("meta[content]").forEach { node ->
        val key = node.attr("property").ifBlank { node.attr("name") }.ifBlank { node.attr("itemprop") }
        val content = node.attr("content")
        if (key.isNotBlank() && content.isNotBlank() && !key.contains("csrf", true) && !key.contains("token", true)) meta.getOrPut(key) { mutableListOf() }.add(content)
    }
    fun value(vararg names: String) = names.firstNotNullOfOrNull { meta[it]?.firstOrNull() }
    val payloads = mutableListOf<MetadataPayload>()
    val metaJson = JsonObject().apply { meta.forEach { (key, values) -> add(key, com.google.gson.Gson().toJsonTree(values)) } }
    payloads += metadataPayload("html-meta", url, metaJson)
    val nodes = mutableListOf<JsonObject>()
    fun visit(node: JsonElement) {
        when {
            node.isJsonArray -> node.asJsonArray.forEach(::visit)
            node.isJsonObject -> { nodes += node.asJsonObject; node.asJsonObject.entrySet().forEach { visit(it.value) } }
        }
    }
    doc.select("script[type=application/ld+json]").forEachIndexed { index, script ->
        runCatching { JsonParser.parseString(script.data()) }.getOrNull()?.let { data ->
            payloads += metadataPayload("json-ld-$index", url, data); visit(data)
        }
    }
    fun JsonObject.string(name: String): String? = get(name)?.takeIf { it.isJsonPrimitive }?.asString?.takeIf(String::isNotBlank)
    fun JsonObject.strings(name: String): List<String> {
        val item = get(name) ?: return emptyList()
        return (if (item.isJsonArray) item.asJsonArray.toList() else listOf(item)).mapNotNull {
            if (it.isJsonPrimitive) it.asString else if (it.isJsonObject) it.asJsonObject.string("name") else null
        }
    }
    val types = setOf("TVSeries", "Movie", "VideoObject", "CreativeWorkSeries", "Anime", "TVEpisode")
    fun JsonObject.hasMediaType() = strings("@type").any { it in types }
    val canonical = doc.selectFirst("link[rel=canonical]")?.attr("href")?.let(::absolute) ?: value("og:url")?.let(::absolute) ?: url
    val subject = nodes.filter { it.hasMediaType() }.firstOrNull { node ->
        node.string("url")?.let(::absolute)?.trimEnd('/') == canonical.trimEnd('/')
    } ?: nodes.firstOrNull { it.hasMediaType() }
    // Related title schemas stay in raw payloads; their facts must not become this title's facts.
    val title = subject?.string("name") ?: value("og:title", "twitter:title") ?: doc.selectFirst("h1")?.text()?.takeIf(String::isNotBlank) ?: doc.title()
    val description = subject?.string("description") ?: value("description", "og:description", "twitter:description")
    val images = mutableListOf<MetadataImage>()
    fun image(raw: String?, role: String, width: Int? = null, height: Int? = null, caption: String? = null) {
        if (raw != null) absolute(raw)?.let { images += MetadataImage(it, role, width, height, caption = caption) }
    }
    meta["og:image"].orEmpty().forEach { image(it, "social", value("og:image:width")?.toIntOrNull(), value("og:image:height")?.toIntOrNull(), value("og:image:alt")) }
    meta["twitter:image"].orEmpty().forEach { image(it, "social") }
    subject?.let { node ->
        for (field in listOf("image", "thumbnailUrl")) node.get(field)?.let { element ->
            (if (element.isJsonArray) element.asJsonArray.toList() else listOf(element)).forEach { value ->
                if (value.isJsonPrimitive) image(value.asString, if (field == "thumbnailUrl") "thumbnail" else "poster")
                else if (value.isJsonObject) image(value.asJsonObject.string("url") ?: value.asJsonObject.string("contentUrl"),
                    if (field == "thumbnailUrl") "thumbnail" else "poster", value.asJsonObject.string("width")?.toIntOrNull(), value.asJsonObject.string("height")?.toIntOrNull())
            }
        }
    }
    doc.select("video[poster]").forEach { image(it.attr("poster"), "video-poster") }
    val people = mutableListOf<MetadataPerson>()
    subject?.let { node ->
        for (role in listOf("actor", "director", "creator", "producer", "author")) node.get(role)?.let { element ->
            (if (element.isJsonArray) element.asJsonArray.toList() else listOf(element)).forEach { person ->
                if (person.isJsonPrimitive) people += MetadataPerson(person.asString, role)
                else if (person.isJsonObject) person.asJsonObject.string("name")?.let { name -> people += MetadataPerson(name, role,
                    person.asJsonObject.string("url")?.let(::absolute), person.asJsonObject.string("image")?.let(::absolute)) }
            }
        }
    }
    val ratings = mutableListOf<MetadataRating>()
    subject?.get("aggregateRating")?.takeIf { it.isJsonObject }?.asJsonObject?.let { rating ->
        ratings += MetadataRating(rating.string("ratingValue")?.toDoubleOrNull(), rating.string("bestRating")?.toDoubleOrNull(),
            (rating.string("ratingCount") ?: rating.string("reviewCount"))?.replace(",", "")?.toLongOrNull(),
            source = url.toHttpUrlOrNull()?.host, label = rating.string("name"))
    }
    val links = doc.select("link[rel=alternate][href]").mapNotNull { node -> absolute(node.attr("href"))?.let {
        MetadataLink(it, "alternate", node.attr("hreflang").ifBlank { node.attr("type") }.takeIf(String::isNotBlank))
    } }.toMutableList()
    subject?.strings("sameAs")?.mapNotNull(::absolute)?.forEach { links += MetadataLink(it, "sameAs") }
    subject?.get("trailer")?.let { trailer ->
        val candidates = if (trailer.isJsonArray) trailer.asJsonArray.toList() else listOf(trailer)
        candidates.forEach { node ->
            val raw = if (node.isJsonPrimitive) node.asString else if (node.isJsonObject)
                node.asJsonObject.string("contentUrl") ?: node.asJsonObject.string("embedUrl") ?: node.asJsonObject.string("url") else null
            raw?.let(::absolute)?.let { links += MetadataLink(it, "trailer") }
        }
    }
    val studios = mutableListOf<MetadataTerm>()
    subject?.get("productionCompany")?.let { companies ->
        (if (companies.isJsonArray) companies.asJsonArray.toList() else listOf(companies)).forEach { company ->
            val name = if (company.isJsonPrimitive) company.asString else if (company.isJsonObject) company.asJsonObject.string("name") else null
            name?.let { studios += MetadataTerm(it, url = company.takeIf { it.isJsonObject }?.asJsonObject?.string("url")?.let(::absolute),
                attributes = mapOf("sourceField" to "schema.productionCompany")) }
        }
    }
    val attributes = linkedMapOf<String, String>()
    subject?.entrySet()?.forEach { (key, item) -> if (item.isJsonPrimitive) attributes["schema.$key"] = item.asString }
    val keywords = subject?.strings("keywords").orEmpty().flatMap { it.split(',') }.plus(value("keywords")?.split(',').orEmpty())
    val microdata = JsonObject()
    doc.select("[itemprop]").filter { it.tagName() != "meta" }.forEach { element ->
        val key = element.attr("itemprop")
        val item = element.attr("content").ifBlank { element.attr("datetime") }.ifBlank { element.text() }.trim()
        if (item.isNotBlank()) {
            val values = microdata.getAsJsonArray(key) ?: com.google.gson.JsonArray().also { microdata.add(key, it) }
            values.add(item)
        }
    }
    if (microdata.size() > 0) payloads += metadataPayload("html-microdata", url, microdata)
    return MetadataRecord(subject?.string("identifier") ?: canonical, canonical, title,
        alternativeTitles = subject?.strings("alternateName").orEmpty(), description = description,
        images = images.distinctBy { it.url to it.role }, genres = subject?.strings("genre").orEmpty().map { MetadataTerm(it) },
        tags = keywords.map(String::trim).filter(String::isNotBlank).distinct().map { MetadataTerm(it) },
        ratings = ratings, people = people.distinct(), countries = subject?.strings("countryOfOrigin").orEmpty(),
        languages = subject?.strings("inLanguage").orEmpty(), studios = studios, format = subject?.string("@type"),
        episodeCount = subject?.string("numberOfEpisodes")?.toIntOrNull(),
        durationSeconds = metadataDurationSeconds(subject?.string("duration")),
        releaseDate = subject?.string("datePublished") ?: subject?.string("uploadDate") ?: value("article:published_time"),
        updatedAt = subject?.string("dateModified") ?: value("article:modified_time"),
        links = links.distinct(), attributes = attributes, payloads = payloads)
}

internal fun metadataDurationSeconds(raw: String?): Long? {
    if (raw.isNullOrBlank()) return null
    if (raw.startsWith("P", true)) return runCatching { java.time.Duration.parse(raw.uppercase()).seconds }.getOrNull()
    val parts = raw.trim().split(':').map { it.toLongOrNull() ?: return null }
    if (parts.size !in 1..3) return null
    return parts.fold(0L) { sum, value -> sum * 60 + value }.takeIf { it >= 0 }
}
