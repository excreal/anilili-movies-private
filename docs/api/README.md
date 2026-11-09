# Supported-site data APIs

This is an in-process Kotlin API for the Android repository, independent of its UI card models. It does not start an HTTP server or change which metadata the UI displays. The common contract, registry, shared HTML parser, and all five provider extractors live together in `app/src/main/java/com/example/animepahedl/metadata/`. Provider-specific parsing and endpoint behavior stays in that folder; the registry selects an implementation by source or URL.

## Contract

- `browse(MetadataBrowseQuery)` exposes listing/search/filter pagination, source sections and available filters.
- `details(url)` exposes the title's full record, episode inventory, related items and original public data.
- `episode(url)` exposes episode images, duration/dates, subtitle languages/URLs, availability and additional provider fields. Media resolution remains an explicit playback API operation, so a metadata query need not download video or visit every episode.
- `taxonomy()` exposes actual advertised genres/categories/tags and other source-specific filter groups.

Structured records include original/alternate titles, synopsis, role-tagged artwork and dimensions, genres, categories, tags, ratings with their published scale/vote count, people and roles, countries/languages/studios, format/status/year, release/update dates, duration/counts, external IDs and links. Providers retain additional fields as attributes and JSON payloads with source names and URLs. JSON retains arrays, objects, booleans and numeric values; additional fields are not flattened away. Site-specific documentation records supported query filters and endpoint details.

Empty/null values mean the site did not supply that information in the inspected response. A reused poster is not called a landscape banner, likes/views are not invented ratings, and inaccessible endpoints are not counted as verified. Source inventory totals and playable/locked counts remain distinct. Public signed media/image URLs can expire; callers should refresh them. Source JSON retention excludes authentication/session secrets and HTTP headers. Imported provider cookies remain in the existing cookie store and are scoped per request and redirect.

## Scope and verification

The five metadata sites are Dramatip, Kiss18, Narto Drama, EroticMV and KissKH. AnimePahe is intentionally excluded because its catalogue/API pages returned Cloudflare 403 challenges in the inspected environment; this does not affect the app's existing AnimePahe download features. Direct MP4/WebM/HLS inputs remain supported by the existing media resolver; a bare media URL does not supply a catalogue, ratings or a genre taxonomy. The user's Korean-only KissKH browsing restriction remains in force.

Each site has a dedicated subagent audit and document in this directory. Live HTTP/page/script evidence, blocked requests and parser limitations are recorded per site. No telemetry/voting/AI-subtitle-job or account-writing endpoints are invoked by metadata reads.


## Usage

```kotlin
val apis = SiteMetadataApis.forApp(context) // reuses the app's scoped site sessions
val source = apis[MetadataSite.DRAMATIP]
val listing = source.browse(MetadataBrowseQuery(filters = mapOf("category" to "popular")))
val detail = source.details(listing.items.first().url)
val episode = source.episode(detail.episodes.first().url)
val taxonomy = source.taxonomy()
// detail.record.genres / images / ratings / attributes / payloads are available
// whether or not a screen currently displays them.
```

`SiteMetadataApis()` also supports clients without an Android cookie store. Supply a `(url) -> cookieHeader` function when a source needs a viewer's already-authorized session. `forUrl`/`forProvider` select the source; direct media is deliberately routed to the existing media resolver instead of inventing catalogue metadata. Read methods suspend and perform network work off the UI thread. Keep one API instance for pagination so its session/snapshot cache is reused.
