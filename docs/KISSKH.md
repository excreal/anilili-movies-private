# KissKH Korean-drama adapter

Source: https://kisskh.do/ and its public web client (`main.c582432abad915cc.js`,
`876.939dcdf0f6df9f97.js`, `502.33bac7b53e9897b8.js`, `common.js?v=9082123`),
inspected 2026-11-08.

- Home and paging call `/api/DramaList/List` with `country=2` and `type=1`, matching
  the website's Korean TV filter. The current API sample reports 2,084 entries;
  the app reads the actual total and never hardcodes that count.
- The public `/Search` endpoint ignored `country=2` in live probes. Search therefore
  uses only a cached index loaded from the Korean list, with 500 entries per fetch,
  four concurrent fetches maximum, a 30-minute cache, and 48-result pages.
- Detail inspection calls `/api/DramaList/Drama/{id}?isq=false` and requires
  `country=South Korea` (or Korea) and `type=TVSeries`. Non-Korean pasted links
  are refused too. Episode resolution verifies the episode belongs to that series.
- Playback follows the public client's `/api/DramaList/Episode/{id}.png` request.
  `kisskhRequestKey` reproduces common.js's request token using the observed
  web-client parameters. It is a web API request token, not a media-decryption key.
- Direct HLS/MP4 responses feed the existing player/download pipeline with the
  site referrer. Signed URLs are resolved on demand; stable episode locators retain
  resume progress. Unavailable or third-party-only embed responses report an error.
- Fractional bonus episodes preserve their displayed labels in rows, selectors,
  range input, downloaded records and history. Internal episode IDs remain stable.
- Subtitles use the website's `/api/Sub/{episodeId}?kkey=…` request with its distinct subtitle GUID. The response supplies language labels and the default language; all supported tracks are normalized and passed to the original Anilili captions menu.
- The website downloads subtitle blobs, converts SRT to VTT and decodes legacy per-cue AES text files. The app follows these public subtitle transformations, including top-positioned music/annotation cues. It keeps files in persistent private storage so downloaded episodes retain captions offline.
- Source defaults are ordered first; Anilili's saved language preference takes precedence. The website's browser-only `sub_label` storage is not shared with the app.
- Third-party embed-only players remain unsupported.

Verification: live Korean list and detail requests; public-client token and compiled
Kotlin token exact match on episode 136523; its returned MP4 was readable with
valid `ftyp` bytes and HTTP byte-range support. This was not an Android playback
or full-file download test. ADB is disconnected.

Subtitle verification: compiled Kotlin normalization passed a real English SRT (790 cues), all three legacy encrypted-cue formats, top positioning, and invalid HTML rejection. No Android subtitle render or Cast test has run.
