# Dramatip metadata API

Inspected live 2026-11-08. Adapter: `DramatipMetadataApi` in `app/src/main/java/com/example/animepahedl/metadata/DramatipMetadataApi.kt`.

## Public surfaces and methods

- `browse()` reads the [home page](https://dramatip.net/) including section membership, title cards and responsive image variants. Search follows `/search?q=...`; explicit `query.url` follows source-provided navigation without making a UI assumption.
- Filters: `category` maps `/category/{slug}`, `source` maps `/source/{slug}`, `language` selects the public locale prefix. Search, category, and source pages use their advertised page routes. Pages are bounded to 1–100. An unfiltered page 2 follows the homepage's own advertised next URL, which may differ from a section's “See all” link; later pages retain that continuation route and change only its page number. Repeated URLs are rejected. `nextUrl` comes from actual `rel=next` or localized next-page navigation.
- `details(url)` reads the canonical series/episode page and decodes JSON-LD plus JSON-string-encoded React Flight records. It does not resolve/fetch every episode video or require a playable first episode.
- `episode(url)` returns the requested episode's metadata and its public source descriptor/subtitle information from the page. Playback still uses the existing media resolver.
- `taxonomy()` reads the full [categories](https://dramatip.net/categories) and [sources](https://dramatip.net/sources) indexes, retaining labels, slugs, links, source icons, title counts and available language routes.

The public player JS (`/_next/static/chunks/0xjf21s0w-4s2.js`, loaded from the inspected page) also exposes read-only `/api/episode-source/{bookId}/{serial}?lang={locale}`, with optional `raw=1` and `refresh=1`. A direct API probe returned HTTP403 in this environment; embedded page descriptors were readable, so this API implementation uses the working page data instead of depending on that endpoint. It never calls the website's `/api/play` or `/api/playback-fallback` telemetry mutations.

## Verified fields retained

| Area | Data |
| --- | --- |
| Identity | book UUID, public slug, canonical/alternate language links, title, schema type |
| Description | Full JSON-LD synopsis; OG/meta synopsis retained separately in shared public payloads (can be truncated) |
| Artwork | OG/Twitter cover; rendered poster and player poster/fallback; responsive srcset widths; episode cover/fallback when advertised; source icons |
| Genres/categories | JSON-LD genres and visible full genre list; category index labels, slugs, counts and URLs |
| Ratings | Displayed numeric rating and 10-point scale; displayed votes string retained without falsely asserting a precise count from a rounded `20.4K` display |
| Sources | Distribution source name/link (e.g. MyMuse), source descriptor chain, refresh flags/expiry and quality alternatives in public payload |
| Episodes | Chapter UUID, serial number, route, lock flag, duration if nonzero, per-episode cover/fallback and qualities; original entry retained in payload |
| Subtitles | Source `subtitle` descriptor: direct or AES-GCM-wrapped URL, language, label, recognized format and explicit default flag; insecure URLs and credential-bearing URLs are rejected; `null` remains no advertised external track |
| Language | Page/series language, available locale navigation and alternate links |
| Navigation | Related cards, section membership, next-page URL, full advertised page navigation |
| Future metadata | Original JSON-LD and selected complete player/episode public objects, with provenance URLs; visible definition-list attributes and original card/image attributes |

## Live evidence and limits

The sampled [Chained Together series](https://dramatip.net/series/chained-together) and its [first episode](https://dramatip.net/series/chained-together/episode-1) advertise 64 episodes, rating 8.3/10, `20.4K votes`, MyMuse source, five genres and English language. Its synopsis in JSON-LD is longer than OG/meta description. Each episode has a UUID and lock/quality/duration fields; sampled covers are empty and duration 0, which is preserved in the raw entry but mapped to unknown duration in typed data. Episode routes also retain their own `TVEpisode` schema facts when present. Its source subtitle descriptor is null. This does not prove other titles lack subtitles.

No distinct landscape hero/banner was exposed for this sampled title. Poster images are not relabeled as landscape banners. The API retains any independently advertised generic schema artwork and player poster, and all responsive card variants. Cast, crew, production year/country, precise rating-vote count, release schedule, view totals and age/content rating were not advertised by the sampled title; absent typed fields remain empty/null. Distribution source is marked as such and is not asserted to be a production studio. More fields appearing in future public JSON objects remain accessible through payloads.

Initial bare homepage fetch returned 403; a subsequent ordinary mobile-UA page fetch succeeded. Category popular, homepage, detail, episode, category index, source index, search (`/search?q=love`), source listing (`/source/mymuse`) and public player scripts were visited. Category/source/search pagination patterns were confirmed from actual navigation links. German labels such as `Nächste Seite` are retained in pagination payloads and used when locating a next page. HTTP-200 not-found documents are rejected as missing pages. Unsupported filter names, incompatible category+source filters and search+category/source combinations are rejected explicitly. The unfiltered page 2 implementation follows the homepage's own advertised continuation. This is bounded source/API verification, not an Android device test. No credentials or imported cookies were used or captured.
