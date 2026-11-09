# EroticMV metadata API

Inspected 2026-11-08. Implementation: `metadata/EroticMvMetadataApi.kt`. This is the public WordPress/VidoRev metadata surface, independent of the app's movie card model and playback resolver.

## Endpoints and operations

- `browse()` reads [home](https://eroticmv.com/), preserving the primary listing, advertised sidebar/section cards, page navigation and taxonomy. `query.query` is the advertised WordPress `/?s=...` search; its next page is `/page/2/?s=...` (verified in existing saved source HTML).
- Filters `genre`, `country`, `decade` map `/category/genre/{slug}/`, `/category/country/{slug}/`, `/category/decades/{slug}/`. `tag`, `actor`, `director` map their matching WordPress archive routes. `series=true` opens `/series/`. Only one archive selector may be combined; search cannot be combined with it.
- `sort=view` and `sort=like` map the site's real `archive_query` navigation; `letter` maps its `alphabet_filter`. Unknown filter names/values and incompatible explicit URL/filter input fail explicitly. Explicit source navigation URLs are accepted through `query.url`; positive `query.page` builds the source's `/page/{n}/` pagination.
- `details(url)` parses title/person/series HTML: original JSON-LD/meta plus rich theme facts, content imagery, people, native/IMDb ratings, player/subtitle metadata and related records. Movie pages advertise a single movie episode; person/archive pages are not assigned fake episodes.
- `episode(url)` returns the movie's one advertised player metadata record, with separate subtitle descriptors when present; it does not fetch its media.
- `taxonomy()` reads the full navigation taxonomy advertised on the home page. Genres, countries, decades, generic categories and tags remain separate.
- Additional `feed(url = "https://eroticmv.com/feed/")` reads the independently advertised public RSS. It retains all source item fields (including repeated categories) and sanitized descriptive HTML, vote counts, published dates, author/uploader, comment counts, comment-feed URLs, GUID/post ID and channel metadata. Feed XML is not silently substituted for a full detail page.

## Fields retained

| Surface | Data |
| --- | --- |
| Listing identity | WordPress post ID, canonical route, title and explicitly displayed title year |
| Artwork | Original lazy-load image URLs plus all responsive `srcset` widths, height/width/alt where supplied; social images, video posters and content/screenshot images from detail HTML; independently advertised preview iframe routes |
| Classification | Actual hierarchical genre/country/decade categories, generic categories and tags; archive/person/series links |
| Ratings | Native YASR displayed average and exact vote count when present; native scale left unknown if not advertised; independent IMDb ID and published IMDb rating on its 10-point scale; original public IMDb object retained |
| Engagement | Displayed views, likes, dislikes and comments kept separately; exact integer values mapped only when displayed exactly, e.g. `30.9K` is retained as display text, not asserted as 30,900 precise views |
| Movie facts | Full description, original/AKA fields, country/year/language/runtime/studio facts when explicitly present; JSON-LD dates/type/other facts retained even when not used in UI |
| People | Director and actor names, profile routes and image when published; uploader kept as uploader, never asserted to be director |
| Playback-related metadata | Whitelisted public `single_video_*` player fields, source variants and preview/VTT fields, `<track>` and `single_media_subtitles` source descriptors; no video body fetched |
| Future use | Public meta/schema/microdata, source card image/fact objects, pagination, selected complete player descriptive objects, RSS item/channel payloads |

## Live evidence and limitations

Previously captured browser-rendered visits succeeded for [home](https://eroticmv.com/), [I Don't Need A Man](https://eroticmv.com/i-dont-need-a-man-2023/), [genre archive](https://eroticmv.com/category/genre/asian-erotica/), [all-movies archive](https://eroticmv.com/category/decades/), most-viewed sort, [director profile](https://eroticmv.com/director/choi-jung-ja-%EC%B5%9C%EC%A0%95%EC%9E%90/), [actor profile](https://eroticmv.com/actor/yeon-joo/) and [series archive](https://eroticmv.com/series/). The sampled movie publicly shows directors, actors, synopsis, visitor average/votes, views/likes/dislikes/comment counts; those are independent metrics. Counts vary as the site receives traffic.

Direct ordinary HTTP HTML probes returned Cloudflare 403 in this environment. A local headless Chromium browser loaded the homepage once with a desktop Chrome user agent (HTTP 200), and the public RSS feed returned HTTP 200. It then received Cloudflare challenge responses for search, title detail, genre/decade archives, director/actor profiles, series, and `/wp-json/`; a homepage-to-title navigation first returned a 307 “Loading” response and then landed on the challenge. The browser used a same-site homepage referrer on subsequent navigations. This indicates that adding a referrer alone does not resolve the current challenge. Existing previously captured successful home/search HTML (`/tmp/erotic-catalog-home.html`, `/tmp/erotic-catalog-search.html`) supplied exact parser selectors and responsive/IMDb/navigation data. Fresh [RSS](https://eroticmv.com/feed/) and the public VidoRev theme script returned200. The theme script explicitly supports `single_media_subtitles` (`src`, `label`, `srclang`) and timeline-preview VTT; this establishes schema support, not that the sampled movie has a subtitle file. The Android metadata registry now merges any existing WebView cookies with the imported provider cookie store for requests, preferring the active WebView values if a cookie name exists in both. EroticMV requests use the browser-style HTML Accept header; requests continue to send the site-root Referer and the app WebView user agent. Cookie/clearance values are not copied into metadata or logs. Challenge responses still fail visibly rather than becoming empty metadata.

The page advertises WordPress `/wp-json/`, but current index, posts-by-slug, `rest_route`, oEmbed and embed probes returned403. They are documented as protected/unverified and are not a dependency of this API. The advertised feed is readable, supplies recent posts only and makes no claim to be a complete historic catalogue. Category RSS probes and general search were blocked in the current HTTP environment. No API writes, rating votes, comment posts, ad requests or telemetry calls were made.

No independent landscape banner was identified; posters are not renamed banners. Sample feed content had no poster or subtitle URLs; fields remain empty when absent. Site image URLs and episode-specific signed media can expire. Sanitized payloads exclude account cookies, JS security fields and voting nonces; unrelated global executable/admin configuration is not retained.

Saved public evidence: `/tmp/eroticmv-metadata/feed.html`, `content.html`, `theme.js`; older successful home/search HTML paths above. Parser entrypoints `parseEroticMvMetadataPage`, `parseEroticMvMetadataDetails`, `parseEroticMvMetadataFeed` permit targeted saved-response inspection. No build/test suite/device test run by this agent.
