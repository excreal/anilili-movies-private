# KissKH metadata API

Implementation: `metadata/KisskhMetadataApi.kt`, common `SiteMetadataApi` contract. Audited live on 2026-11-08, with `https://kisskh.do/`, its public Angular scripts and read-only public JSON requests. No remote JavaScript executes. The app's Korean-TV restriction applies to listing, search, sections, detail and episode metadata.

## Verified endpoints and public data

| Surface | Read-only request | Retained data |
| --- | --- | --- |
| Korean TV list | `/api/DramaList/List?page=1&pageSize=12&type=1&country=2&sub=0&status=0&order=1` | Envelope `page`, `pageSize`, `totalCount`, `data`; each row's `id`, `title`, `thumbnail`, `episodesCount`, `label`, `favoriteID`, and future fields as original JSON. |
| Full title | `/api/DramaList/Drama/7977?isq=false` | `description`, `releaseDate`, `trailer`, `country`, `status`, `type`, `nextEpDateID`, `episodes`, `episodesCount`, `label`, `favoriteID`, `thumbnail`, `id`, `title`. All fields survive in payloads. |
| Episode inventory | Full-title `episodes[]` | Exact public `id`, decimal `number`, opaque `sub` value, future keys. Stable per-episode locator URLs retain fractional bonus episode numbers. |
| Episode video descriptor | `/api/DramaList/Episode/{episodeId}.png?err=false&ts=null&time=null&kkey=…` | JSON only: `Video`, `Video_tmp`, `ThirdParty`, `Type`, `id`, `dataSaver`, `a`, `b`, `dType`, future fields. Media bodies are not fetched. Public playback URLs are ephemeral. |
| Subtitle descriptors | `/api/Sub/{episodeId}?kkey=…` | `src`, `label`, `land`, `default` and future keys. Mapped to typed URL, label, language, file format and default. No caption file download/localization in this metadata API. |
| Featured | `/api/DramaList/Show` | Source order and image/title rows. |
| Recently updated | `/api/DramaList/LastUpdate?ispc=false` | Full returned public rows. |
| Upcoming | `/api/DramaList/Upcoming?ispc=false` | Full returned public rows, including source release labels and zero-episode titles. |
| Most searched | `/api/DramaList/MostSearch?ispc=false` | Full returned public rows and source order. |
| Top rated | `/api/DramaList/TopRating?ispc=false` | Ranked rows; **no numeric rating or vote count is returned**. No invented rating is attached. |
| Korea most viewed | `/api/DramaList/MostView?ispc=false&c=2` | Full returned public rows and source order; no invented view count. |
| Explore filters | Home → advertised `runtime.*.js` → chunk 780 | The live literal `this.chips` array with labels, numeric values and state; original parsed JSON is retained. Runtime hash is discovered, not hardcoded. No script evaluation. |

All listed browse/detail/section/filter samples returned HTTP200. The sampled Korean show was Twinkling Watermelon, title7977, episode136523. Sample files are under `/tmp/kisskh-metadata`; these temporary research captures are not packaged app assets. The public website's subtitle token uses a different GUID from its video token; both use the existing verified `kisskhRequestKey` implementation. Token query parameters are excluded from retained payload provenance URLs.

## Structured mapping

- Records expose stable KissKH ID, canonical title link, full synopsis, actual source thumbnail, country, status, media type, release date/year and episode count.
- Trailer URL and advertised next-episode countdown link are typed links when nonempty. `label`, `nextEpDateID`, `favoriteID` and all scalar source fields remain accessible as attributes. `favoriteID` is not treated as a favorite count or score.
- The source thumbnail is often a landscape TMDB `w1000_and_h563_face` image. It is tagged `thumbnail`; the API does not claim that an independently supplied poster or banner exists. No source-specific episode snapshot appeared in the inspected title/episode descriptor responses.
- Episode descriptors expose only secure public subtitle URLs, source languages/default flags, and a format derived from the descriptor URL. Video URL attributes are exposed only when they pass the same HTTPS, credential-free, HLS/direct-file checks as playback; the complete source JSON remains in its sanitized raw payload. The sampled episode advertised English, Indonesian, Malay, Arabic and Khmer. Available languages vary per episode; opaque inventory `sub` values remain in the original episode payload and `attributes`, with a separate `hasSubtitleHint` boolean only when `sub > 0`. This hint does not claim a language or track count.
- Optional video/subtitle descriptor failures preserve the episode record with `videoMetadataStatus=unavailable` or `subtitleMetadataStatus=unavailable`. Successful descriptor reads set `available`, including a genuinely empty subtitle list. Cancellation propagates.
- Actual genres, cast, crew, studios, alternate-title arrays, per-episode duration, rating values and votes were not supplied by the inspected public API/client. They remain empty, rather than being scraped from unrelated third parties or guessed from the synopsis. Additional future response keys still survive in JSON payloads.

## Query/API use

`browse(MetadataBrowseQuery(query="watermelon", filters=mapOf("status" to "2")))` searches a bounded, cached Korean-TV index; the public `/Search` endpoint ignores country and is deliberately not used. The index respects `sub`, `status`, `order`, fetches 500 rows/page, at most 50 pages, four concurrent requests, and caches up to 8 query indexes for 30 minutes. Metadata requests do not fetch every title detail/episode. `browse()` returns empty filter groups and a `taxonomy-status` payload explaining that taxonomy is loaded separately; a normal listing therefore does not wait on Explore's separately published JavaScript chunks. `taxonomy()` is the operation that reads the current web-client filter options.

Supported filters:

| Key | Accepted values |
| --- | --- |
| `type` | `1` only (TVSeries) |
| `country` | `2` only (South Korea) |
| `sub` | `0`all, `1`English, `2`Khmer, `3`Indonesian, `4`Malay, `5`Thai, `10`Arabic |
| `status` | `0`all, `1`ongoing, `2`completed, `3`upcoming |
| `order` | `1`popular, `2`last update, `3`release date |
| `pageSize` | 1–500; default48 |
| `page` | 1–10000; otherwise `query.page` |
| `section` | `featured`, `lastUpdate`, `upcoming`, `mostSearch`, `topRating`, `mostViewed` |

Unknown filter keys/unsupported values fail explicitly. `query.url` supports the returned next URL, `/Explore`, the origin or the actual list route; next URLs preserve search, section and filters. The synthetic `_search`/`_section` parameters are handled locally and never sent to the website's list endpoint. Non-Korean/non-TV detail responses and episodes belonging to another title fail validation.

Section endpoints except Korea-most-viewed are global on the website. This API intersects their IDs with the selected Korean-TV index before returning rows or retaining section JSON; a global feature section may therefore be empty. No non-Korean title is displayed through these section results.

`taxonomy()` exposes all options actually advertised by the web client, including original countries/types for reference, but attaches `allowedByAppPolicy` to country/type terms. Those options do not grant non-Korean browsing. The five source option groups and all additional future groups are retained; section identifiers are also exposed. Live taxonomy is cached for 30 minutes; a changed/unreadable source module reports an error from standalone `taxonomy()` instead of silently returning invented options. The parser does not infer a genre taxonomy or that every row is scripted.

Playback catalog requests require HTTPS, reject URL credentials, and revalidate the URL on each redirect hop before looking up cookies. Only the `kisskh.do` and `www.kisskh.do` hosts are accepted; a redirect to an unrelated host is rejected before that host can receive the scoped KissKH cookie. Episode subtitle and video descriptors are requested independently and concurrently, with cancellation propagated. Online playback localizes default/English subtitle tracks first (or the first advertised track when neither is flagged), so unrelated slow caption files do not delay the player or trigger multiple media-item preparations. Download metadata requests can still localize the full set.

## Scope and completeness limits

The API retains each available public response field, including fields unused by the UI. It does not access developer/admin APIs, account histories/favorites, Firebase credentials, post telemetry, submit ratings, create subtitle jobs, fetch third-party comments or claim that blocked/unadvertised endpoints were verified. `details()` enumerates inventory; call `episode()` for the selected episode's subtitle/video descriptors. Existing playback/download APIs remain responsible for playback resolution and persistent offline captions. Parent integration owns build and targeted runtime/parser verification; this audit did not run a build or test suite.
