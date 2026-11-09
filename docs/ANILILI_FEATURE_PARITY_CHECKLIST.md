# Anilili feature audit checklist

Scope: reusable Anilili functionality checked against `/root/Anilili/app/src/main/java/com/anilili` and the current Video Downloader app. This is an ongoing audit; items below distinguish implemented behavior from candidate work.

## Playback and personal library

- [x] Continue Watching shows title, resume episode/time, progress, and downloaded count.
- [x] Remove a title from Continue Watching without deleting its saved-show entry or resume data.
- [x] Mark a title completed; the existing watched-state summary reflects completion.
- [x] Save shows to Library from details and the player; Library has separate Saved shows and Continue watching rows.
- [x] Resume an offline saved episode and prefer a valid downloaded copy when one exists.
- [x] Player uses the original Anilili player implementation for non-short-drama media. KissKH playback forces FIT to avoid cropping unusual aspect ratios while respecting global scale settings elsewhere.
- [x] Player controls hide after inactivity; episode selection uses a bounded grid picker.
- [ ] Device verification for save/remove/completed, online-to-download resume, player aspect ratios, and subtitle selection (ADB device was unavailable at last check).

## Download management

- [x] Download queue supports pause/resume/retry/remove and per-episode status.
- [x] Download cards expose watched/resume status and last-watched episode.
- [x] Existing-output lookup is folder-scoped and matches full episode/quality/audio names.
- [x] Existing-output recovery now checks a plausible MP4/WebM container signature and retains pending output URI metadata.
- [ ] Confirm container checks on representative real provider outputs and SAF document providers.

## Source, search, and metadata

- [x] Dramatip, Kiss18, Narto, EroticMV, and KissKH catalogs provide source-specific search/browse parsing.
- [x] Genre/category/tag metadata is rendered as interactive chips where the source publishes safe filter routes; EroticMV actor/director routes are also supported.
- [x] EroticMV details retain synopsis, ratings, vote count, people, tags/categories, and movie poster thumbnail separately from banner artwork.
- [x] Overview prefers the full detail synopsis over a shorter Open Graph summary.
- [x] Provider playback refreshes an expired media link at most once per episode after failure; direct URLs do not loop through provider resolution.
- [x] Narto parser accepts opaque signed stream URLs when the page advertises HLS mode.
- [ ] Confirm Narto live episode playback on-device after a fresh source resolution.
- [ ] Review remaining metadata terms against the source pages and fill any missing fields in per-source API docs.
- [ ] Investigate KissKH’s banner-as-thumbnail presentation separately; user asked to leave its browsing/artwork behavior unchanged for now.

## Artwork and reliability

- [x] Catalog cards recover a failed poster once from source detail metadata; Dramatip expired transformed URLs use a validated public original URL.
- [x] Image HTTP has bounded concurrency, connection retry, per-host scheduling, image cache, and source-scoped headers.
- [ ] Recheck Dramatip lazy-image and CDN responses on a device during long scroll sessions.
- [ ] Verify intermittent image failures for every source on a connected device.

## Settings and appearance from Anilili

- [x] Settings include player, subtitles, downloads, servers, accounts/sync, notifications, content, appearance, data, app, and about categories.
- [ ] Compare every relevant setting against Anilili and record keep/adapt/omit decisions in this checklist.
- [ ] Decide whether the Downloader needs Anilili-style theme presets, custom colors, backdrop controls, and app icon matching; current app has a theme model but not the full Anilili appearance suite.
- [ ] Decide whether multi-server ordering/disable controls apply to the Downloader’s direct provider sources. Anilili controls anime playback providers; these sources currently resolve through their own site-specific playback API.

## Release gate

- [ ] `git diff --check`, full unit suite, Android-test compilation, and release build pass.
- [ ] Review the final diff for accidental source regressions and verify the APK signature/build outputs.
- [ ] Run device checks when ADB is connected; report unavailable visual checks accurately.
