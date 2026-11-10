package com.example.animepahedl

import com.example.animepahedl.metadata.*
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.*
import org.junit.Test

class SiteMetadataApiTest {
    @Test fun extractedMetadataFlowsIntoPersistableAppDetailModel() {
        val details = MetadataDetails(
            MetadataRecord("id", "https://sample.test/title", "Title", description = "Overview", year = 2024,
                genres = listOf(MetadataTerm("Drama")), ratings = listOf(MetadataRating(8.5, 10.0, 120, "Source")),
                people = listOf(MetadataPerson("Lee", "director"))),
            episodes = listOf(MetadataEpisode("ep1", "1", "Episode 1", "https://sample.test/ep1",
                description = "Episode summary", subtitles = listOf(MetadataSubtitle("https://sample.test/sub.vtt", "English", "en"))))
        ).toAppMetadata()
        assertEquals("Overview", details.description)
        assertEquals(2024, details.year)
        assertEquals("Drama", details.genres.single())
        assertEquals(8.5, details.ratings.single().value!!, 0.0)
        assertEquals("Lee", details.people.single().name)
        assertEquals("Episode summary", details.episodes.single().description)
        assertEquals("English · en", details.episodes.single().subtitles.single())
    }

    @Test fun schemaParsingKeepsPrimaryTitleSeparateFromRelatedTitle() {
        val html = """
            <html><head><link rel="canonical" href="https://sample.test/title/1">
            <script type="application/ld+json">{"@graph":[
              {"@type":"TVSeries","url":"https://sample.test/title/1","name":"Main","genre":["Mystery"],"numberOfEpisodes":12,"aggregateRating":{"ratingValue":"8.2","bestRating":"10","ratingCount":"450"}},
              {"@type":"Movie","url":"https://sample.test/title/2","name":"Related","genre":"Romance","aggregateRating":{"ratingValue":"3.1"}}
            ]}</script></head><body><h1>Main</h1></body></html>
        """.trimIndent()
        val result = metadataFromHtml(html, "https://sample.test/title/1")
        assertEquals("Main", result.title)
        assertEquals(listOf("Mystery"), result.genres.map { it.name })
        assertEquals(8.2, result.ratings.single().value!!, 0.0)
        assertEquals(450L, result.ratings.single().votes)
        assertEquals(12, result.episodeCount)
    }

    @Test fun publicPayloadRetainsUnknownFieldsButStripsCredentialFields() {
        val json = JsonParser.parseString("""{"newField":[1,{"label":"ok"}],"cookie":"secret","csrfToken":"private","password":"pw","id":"public"}""")
        val payload = metadataPayload("fixture", "https://sample.test/api?token=secret&id=5", json)
        assertTrue(payload.json.contains("newField"))
        assertTrue(payload.json.contains("public"))
        assertFalse(payload.json.contains("cookie"))
        assertFalse(payload.json.contains("csrfToken"))
        assertFalse(payload.json.contains("password"))
        assertFalse(payload.url.contains("token"))
    }

    @Test fun eroticMvFeedPreservesRatingsDatesAndSeparatesCounts() {
        val xml = """<?xml version="1.0"?><rss version="2.0" xmlns:content="http://purl.org/rss/1.0/modules/content/"><channel><title>Feed</title><item><title>Film (2023)</title><link>https://eroticmv.com/film/</link><guid>https://eroticmv.com/?p=7</guid><pubDate>Mon, 01 Jan 2024 00:00:00 GMT</pubDate><content:encoded>&lt;p&gt;Description&lt;/p&gt;&lt;p&gt;Total: 2 Average: 4.5&lt;/p&gt;</content:encoded></item></channel></rss>"""
        val item = parseEroticMvMetadataFeed(xml, "https://eroticmv.com/feed/").items.single()
        assertEquals("Film (2023)", item.title)
        assertEquals(2023, item.year)
        assertEquals("Mon, 01 Jan 2024 00:00:00 GMT", item.releaseDate)
        assertTrue(item.ratings.any { it.value == 4.5 })
    }

    @Test fun kisskhRowsKeepKoreanTitleFieldsWithoutInventingScores() {
        val api = KisskhMetadataApi()
        val record = api.javaClass.getDeclaredMethod("record", JsonObject::class.java).apply { isAccessible = true }
            .invoke(api, JsonParser.parseString("""{"id":7977,"title":"Drama","thumbnail":"https://img.test/landscape.jpg","country":"South Korea","type":"TVSeries","episodesCount":16,"releaseDate":"2025-01-01","status":"Ongoing","description":"Synopsis","trailer":"https://video.test/trailer"}""").asJsonObject) as MetadataRecord
        assertEquals("Drama", record.title)
        assertEquals("South Korea", record.countries.single())
        assertEquals(16, record.episodeCount)
        assertTrue(record.ratings.isEmpty())
        assertTrue(record.links.any { it.relation == "trailer" })
    }

    @Test fun eroticDescriptionCleanerRemovesBoilerplateAndPreservesSynopsis() {
        val single = cleanEroticDescription("Click to rate this post! [Total: 35 Average: 4.1] Also known as Malice, Malicious Description: A widow in Osaka meets a man.")
        assertEquals("A widow in Osaka meets a man.", single)
        val multiline = cleanEroticDescription("Click to rate this post! [Total: 35 Average: 4.1] Also known as Malice, Malicious Description: A widow in Osaka meets a man.\n\nThe second paragraph continues.")
        assertEquals("A widow in Osaka meets a man.\nThe second paragraph continues.", multiline)
        val synopsisMention = cleanEroticDescription("A widow in Osaka meets a man, also known as the traveler. Description: The story continues from there.")
        assertEquals("A widow in Osaka meets a man, also known as the traveler. Description: The story continues from there.", synopsisMention)
    }

    @Test fun eroticDetailsKeepDescriptionParagraphsAfterRemovingRatingAndAliasBoilerplate() {
        val details = parseEroticMvMetadataDetails(
            """<html><head><title>Film</title></head><body><main><h1>Film</h1><div class="entry-content">
                <p>Click to rate this post! [Total: 35 Average: 4.1] Also known as Alias One, Alias Two Description: A widow in Osaka meets a man.</p>
                <p>The second paragraph continues.</p>
            </div></main></body></html>""".trimIndent(),
            "https://eroticmv.com/film/"
        )
        assertEquals("A widow in Osaka meets a man.\nThe second paragraph continues.", details.record.description)
    }

    @Test fun persistedStringTermsAndLinkedTermObjectsBothDeserialize() {
        val gson = com.google.gson.Gson()
        val old = gson.fromJson("""{"genres":["Drama"],"categories":["Romance"],"tags":["Slow burn"]}""", SiteShowMetadata::class.java)
        assertEquals(listOf("Drama"), old.genres)
        assertEquals(listOf("Romance"), old.categories)
        assertEquals(listOf("Slow burn"), old.tags)
        val newer = gson.fromJson("""{"genres":["Drama"],"genreTerms":[{"name":"Drama","url":"https://eroticmv.com/category/genre/drama/"}]}""", SiteShowMetadata::class.java)
        assertEquals("Drama", newer.genres.single())
        assertEquals("https://eroticmv.com/category/genre/drama/", newer.genreTerms.single().url)
    }

    @Test fun similarTitleRoutesStayOnProviderTaxonomyPaths() {
        assertTrue(isSafeMetadataFilterRoute(CatalogSource.EROTICMV, "genre", "https://eroticmv.com/category/genre/romance/"))
        assertFalse(isSafeMetadataFilterRoute(CatalogSource.EROTICMV, "tag", "https://eroticmv.com/?redirect=https://example.com"))
        assertFalse(isSafeMetadataFilterRoute(CatalogSource.KISSKH, "category", "https://eroticmv.com/category/action/"))
    }

    @Test fun durationParserHandlesClockAndIsoValues() {
        assertEquals(3723L, metadataDurationSeconds("PT1H2M3S"))
        assertEquals(3723L, metadataDurationSeconds("1:02:03"))
        assertNull(metadataDurationSeconds("unknown"))
    }

    @Test fun metadataFacadeDoesNotExposeShortDramaAdapters() {
        val facade = SiteMetadataApis()
        listOf(
            "https://dramatip.net/series/story",
            "https://kiss18.vip/title/story",
            "https://narto-drama.com/detail/watch/story",
        ).forEach { url -> assertTrue("$url must not expose Movie metadata", runCatching { facade.forUrl(url) }.isFailure) }
    }
}
