# Kiss18 public metadata API

Inspected live on 2026-11-08. Implementation: `metadata/Kiss18MetadataApi.kt`; `Kiss18Catalog` supplies the consumer-facing playable catalogue. `MetadataSite.KISS18` supplies `browse`, `details`, `episode` and `taxonomy` independently of UI cards.

## Sources actually visited

| Source | What it provides |
| --- | --- |
| [Home](https://kiss18.vip/) | Featured title links, page metadata, JSON-LD WebSite/WebPage. |
| [Public browse module](https://kiss18.vip/catalogue-browse.mjs) | Complete title/episode snapshot and legacy snapshot digest. |
| [Catalogue client](https://kiss18.vip/catalogue.mjs) | Explicit duplicate-title handling and owner-approved continuation groups, aliases and availability semantics. |
| [Editorial module](https://kiss18.vip/editorial.mjs) | Public presentation title/premise/artwork, moods, final-episode hints, kind, featured/arrival IDs, related part groups, longer descriptions and video-preview metadata. |
| [SEO campaign module](https://kiss18.vip/seo-campaign.mjs) | Declared public genre collections at `/genres/mafia-romance` and `/genres/werewolf-romance`, with member IDs and editorial reasons. |
| [A New Life detail](https://kiss18.vip/title/a-new-life) | Canonical page/social metadata and breadcrumbs. |
| [Episode 1](https://kiss18.vip/watch/a-new-life/1) | Episode WebPage/breadcrumb metadata; playback chunk located through public snapshot. |
| `/playback/{playbackChunk}.json` | Matching episode's candidate IDs, stream version, provider/source, format, refreshable flag, delivery generation, direct URL or gateway/resolver metadata. |
| Sample same-site HLS master | Resolution, bandwidth/average bandwidth, codecs and `CLOSED-CAPTIONS=NONE`. No video segments fetched. |

The inspected snapshot contained 417 records and 12,336 listed episodes. These are dated observations, not constants. Its 19 non-null `video` objects supplied duration/upload time/thumbnail metadata. The catalogue had 416 responsive-cover sets and 41 nonempty alternate-artwork groups. There were 237 source synopses and 154 source-complete inventory flags. Results retain unplayable/test records so this data API does not silently throw away source information; the existing consumer catalogue may still hide those records.

## Coverage and retained data

- Stable source title and episode IDs, canonical slugs and old public-slug aliases.
- Source title plus public presentation title and alternative source titles; synopsis, source premise and public longer detail premise retained with provenance.
- Main cover, every responsive-cover URL and dimensions, alternate artwork and its responsive variants, selected featured artwork, episode thumbnail and preview-video URL.
- Original image bytes/SHA256/variant objects remain in `catalogueRecord`; the normalized image model keeps role and dimensions. Portrait artwork is not falsely labelled a landscape banner.
- Source categories exactly as advertised; public editorial `moods` become tags. The SEO campaign's two genre collections are exposed as `genres` taxonomy terms and per-record genre links. Genre membership is marked as declared campaign membership; taxonomy counts report how many declared IDs are present in the current catalogue snapshot and retain the declared and visible counts separately. These editorial groups are not inferred from category text.
- Listed episode count, playable episode count, original `inventory_complete`, per-episode playable flag and public final-episode hint. Inventory completeness is not falsely converted into a publication-completion date or status.
- Episode ordinal/title, public video GUID, duration seconds, upload date, thumbnail, source candidates and public playback-chunk reference. Optional nested values and future public fields remain in per-record/per-episode JSON payloads.
- Related numbered parts and explicit continuation links, plus a `website_canonical` relationship for duplicate Bunny records paired with a same-title Shortpeak canonical record. Raw records stay separate; inventories are not speculatively merged or renumbered by this metadata API.
- Public featured/popular/arrival/collection sections. Admin rows use only enabled rows and their `label`/`titles` fields. Sections are omitted from search, genre, and filtered pages. Final public presentation and SEO overlays supply title, premise, final-episode and rank data. Source rank is a display-placement attribute, never exposed as a numeric audience rating.
- Full matching playback-candidate JSON is exposed only when `episode()` is explicitly requested, not eagerly for every browse record. Same-site direct HLS master metadata retains codec/bandwidth/resolution renditions and advertises subtitle renditions when present. Foreign/gated resolver media is left to the existing playback adapter.
- Schema.org/social metadata from the detail page is merged into detail images/links/payloads without replacing the catalogue's stable identity.

## Browse and filtering

`browse(MetadataBrowseQuery(query=..., page=..., filters=...))` searches the complete cached source snapshot and supports `category`, `tag`, `genre`, story `kind` (`series`, `clip`, `test`, `all`), and availability selectors (`partial`, `unavailable`, `playable`) plus the `playable=true|false` boolean and `pageSize` (1–500, default 48). SEO genre routes and browse/search URLs are accepted. Pagination is local because the public site ships its full catalogue snapshot; returned continuation URLs preserve the filters and search term. `taxonomy()` returns source categories, public moods, story kinds and declared SEO genres. Unknown filter keys, unknown URL parameters, invalid kinds/booleans, invalid page sizes and nonpositive pages raise explicit errors before snapshot downloads. Requests must belong to approved HTTPS source hosts.

The snapshot/editorial data cache lasts ten minutes per adapter instance. The consumer catalogue retains one metadata adapter across page loads, and snapshot parsing, filtering and record conversion run on its injected IO dispatcher. Reads are size bounded, cancellable and source-host validated on redirect; imported cookies are scoped through `MetadataHttp`. Optional detail HTML enrichment keeps snapshot data on ordinary network/parse failures and propagates cancellation or verification challenges. No remote JavaScript executes. A small data-literal reader extracts only JSON-like objects/arrays/scalars and the site's text-cleaning wrapper, not arbitrary expressions.

## Absent or conditional information

- No public audience rating/vote score, cast/crew, country, spoken language, release year or separate age-rating fields were found in the inspected catalogue/episode schemas. These remain absent, never fabricated from titles, artwork or source rank.
- No separate subtitle endpoint, subtitle field or subtitle-selection code appeared in the inspected site catalogue/playback code. The sampled HLS explicitly says `CLOSED-CAPTIONS=NONE`. The API can return HLS `SUBTITLES` renditions when actually advertised; it does not promise captions for all titles or infer that every stream lacks them.
- No explicit landscape banner was found. All exposed artwork variants are retained with their genuine roles and dimensions for future layout choices.
- Direct media manifests on unapproved third-party hosts and media requiring refreshable gateways are not crawled by the metadata layer. Their actual source metadata is retained and the existing resolver handles playback.
- Public editorial display fields are extracted; owner/admin tooling, executable code, HTTP cookies and authentication values are not exported as normal metadata payloads. Owner curation is not presented as audience content information.

## Verification

Live HTTP checks retrieved the home, public snapshot, catalogue/editorial/client scripts, detail, episode, one playback chunk and one HLS master. The observed master supplied five rendition resolutions (198×352 through 1078×1920) with codecs/bandwidth metadata. No account was created, no video segment downloaded, no source JavaScript executed, and no test suite/device run was performed by this site agent. Parent integration compilation and targeted literal/parser checks are recorded in the aggregate API report.
