package com.example.animepahedl

import com.example.animepahedl.metadata.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assert.assertThrows
import org.junit.Test

class NartoPaginationTest {
    private val base = "https://edge.narto-drama.com/home/providers/sections?provider=dramawave&lang=en-US&target_lang=ko-KR&type_context=all&flareflow_class_id=4&tab_pages%5Balpha%5D=2&tab_pages%5Bbeta%5D=3"

    private fun api(body: String, requests: MutableList<String> = mutableListOf()) = NartoMetadataApi(injectedGet = { url ->
        requests += url
        MetadataResponse(url, body, "application/json")
    })

    @Test fun sectionsExposeIndependentPaginationStateAndProviderFilters() = runBlocking {
        val requests = mutableListOf<String>()
        val api = api("""{"ok":true,"tab_pages":{"alpha":2,"beta":3},"sections":[
            {"tab_key":"alpha","tab_label":"Alpha","page":2,"has_prev":true,"fallback":false,"warnings":["limited"],"items":[{"id":"a","title":"A","url":"/detail/watch/a"}]},
            {"tab_key":"beta","tab_label":"Beta","page":3,"has_prev":true,"items":[{"id":"b","title":"B","url":"/detail/watch/b"}]}
        ],"provider_filters":{"type":[{"id":"short","label":"Short"}],"category":{"3":"Romance"}}} """.trim(), requests)

        val page = api.browse(MetadataBrowseQuery(url = base))

        assertEquals(1, requests.size)
        assertEquals(listOf("alpha", "beta"), page.sections.map { it.tabKey })
        assertEquals(listOf(2, 3), page.sections.map { it.page })
        assertEquals(listOf(true, true), page.sections.map { it.hasPrevious })
        assertEquals(false, page.sections.first().fallback!!.asBoolean)
        assertEquals(listOf("limited"), page.sections.first().warnings)
        assertEquals(mapOf("alpha" to 2, "beta" to 3), page.tabPages)
        assertEquals("Short", page.providerFilters.getValue("type").single().name)
        assertEquals("3", page.providerFilters.getValue("category").single().id)
        assertTrue(page.sections[0].nextUrl!!.contains("only_tab=alpha"))
        assertTrue(page.sections[0].nextUrl!!.contains("tab_pages%5Balpha%5D=3"))
        assertTrue(page.sections[0].nextUrl!!.contains("tab_pages%5Bbeta%5D=3"))
        assertTrue(page.sections[0].nextUrl!!.contains("target_lang=ko-KR"))
        assertTrue(page.sections[0].nextUrl!!.contains("flareflow_class_id=4"))
        assertTrue(page.sections[1].nextUrl!!.contains("only_tab=beta"))
        assertNull("global continuation is ambiguous for multiple sections", page.nextUrl)
    }

    @Test fun liveEmptyObjectDefaultsAndNestedProviderFiltersAreParsedSafely() = runBlocking {
        val body = """{"ok":true,"tab_pages":[],"provider_filters":{"pinedrama":{"categories":[{"category_id":7,"category_name":"Romance"}]},"flareflow":{"classes":[{"class_id":4,"name":"Short"}]}},"sections":[{"tab_key":"alpha","page":1,"warning":"Using a fallback","warnings":["rate limited"],"items":[{"id":"a","title":"A","url":"/detail/watch/a"}]}]}"""

        val page = api(body).browse(MetadataBrowseQuery(url = base))

        assertTrue(page.tabPages.isEmpty())
        assertEquals("Romance", page.providerFilters.getValue("pinedrama_category_id").single().name)
        assertEquals("7", page.providerFilters.getValue("pinedrama_category_id").single().id)
        assertEquals("Short", page.filters.getValue("flareflow_class_id").single().name)
        assertEquals("4", page.filters.getValue("flareflow_class_id").single().id)
        assertEquals(listOf("rate limited", "Using a fallback"), page.sections.single().warnings)
        assertTrue(page.payloads.single().json.contains("category_id"))
    }

    @Test fun followingSectionContinuationMakesExactlyOneRequestAndAdvancesOnlyItsTab() = runBlocking {
        val requests = mutableListOf<String>()
        val api = api("""{"ok":true,"sections":[{"tab_key":"alpha","page":4,"items":[{"id":"a","title":"A","url":"/detail/watch/a"}]}]}""", requests)
        val continuation = "https://edge.narto-drama.com/home/providers/sections?provider=dramawave&lang=en-US&target_lang=en-US&type_context=all&only_tab=alpha&tab_pages%5Balpha%5D=4&tab_pages%5Bbeta%5D=8"

        val page = api.browse(MetadataBrowseQuery(url = continuation))

        assertEquals(listOf(continuation), requests)
        assertEquals("https://edge.narto-drama.com/home/providers/sections?provider=dramawave&lang=en-US&target_lang=en-US&type_context=all&tab_pages%5Bbeta%5D=8&only_tab=alpha&tab_pages%5Balpha%5D=5", page.nextUrl)
    }

    @Test fun emptyClampedRepeatedAndSearchPagesDoNotOfferContinuation() = runBlocking {
        val empty = api("""{"ok":true,"sections":[{"tab_key":"alpha","page":2,"items":[]}] }""")
            .browse(MetadataBrowseQuery(url = base))
        assertNull(empty.sections.single().nextUrl)

        val clamped = api("""{"ok":true,"tab_pages":{"alpha":500},"sections":[{"tab_key":"alpha","page":500,"items":[{"id":"a","title":"A","url":"/detail/watch/a"}]}]}""")
            .browse(MetadataBrowseQuery(url = base.replace("=2", "=99999")))
        assertNull(clamped.sections.single().nextUrl)

        val nonProgress = api("""{"ok":true,"sections":[{"tab_key":"alpha","page":1,"items":[{"id":"a","title":"A","url":"/detail/watch/a"}]}]}""")
            .browse(MetadataBrowseQuery(url = base.replace("=2", "=2")))
        assertNull(nonProgress.sections.single().nextUrl)

        val search = api("""{"ok":true,"sections":[{"tab_key":"alpha","page":2,"items":[{"id":"a","title":"A","url":"/detail/watch/a"}]}]}""")
            .browse(MetadataBrowseQuery(url = "$base&q=term"))
        assertNull(search.sections.single().nextUrl)

        val login = api("""{"ok":true,"login_required":true,"sections":[]} """)
            .browse(MetadataBrowseQuery(url = "https://edge.narto-drama.com/search?q=term"))
        assertTrue(login.items.isEmpty())
        assertTrue(login.sections.isEmpty())
        assertTrue(login.payloads.single().json.contains("login_required"))
    }

    @Test fun providerSearchAndMissingTabDiscoveryDoNotIssuePaginationGet() = runBlocking {
        val requests = mutableListOf<String>()
        val noTabs = api("""{"ok":true,"tab_pages":[],"provider_filters":[],"sections":[]}""", requests)
        noTabs.providerSections("dramawave", page = 2)
        assertEquals("A missing tab list must not trigger a redundant discovery request.", 1, requests.size)

        val searchRequests = mutableListOf<String>()
        val search = api("""{"ok":true,"search_mode":true,"search_query":"term","sections":[{"tab_key":"alpha","page":1,"items":[{"id":"a","title":"A","url":"/detail/watch/a"}]}]}""", searchRequests)
        val payload = search.providerSections("dramawave", page = 2)
        assertEquals(1, searchRequests.size)
        assertTrue(payload.json.contains("search_mode"))
        assertThrows(IllegalArgumentException::class.java) { runBlocking { search.providerSections("dramawave", page = 2, parameters = mapOf("q" to "term")) } }
        Unit
    }

    @Test fun defaultBrowseSearchFieldsDoNotSuppressDiscoveryOrContinuation() = runBlocking {
        val requests = mutableListOf<String>()
        val body = """{"ok":true,"search_mode":false,"search_query":"","sections":[{"tab_key":"alpha","page":2,"items":[{"id":"a","title":"A","url":"/detail/watch/a"}]}]}"""
        val api = api(body, requests)

        api.providerSections("dramawave", page = 2)
        assertEquals("Default browse with empty search fields should discover then request tab pages.", 2, requests.size)
        assertTrue(requests.last().contains("tab_pages%5Balpha%5D=2"))

        requests.clear()
        val page = api.browse(MetadataBrowseQuery(url = base))
        assertEquals(1, requests.size)
        assertNotNull(page.sections.single().nextUrl)
        Unit
    }
}
