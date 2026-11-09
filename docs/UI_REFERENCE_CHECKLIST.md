# Anilili UI reference audit

Reference: `/root/Anilili/app/src/main/java/com/anilili/ui`.
Checked means source implementation inspected and ported/mapped, **not** device screenshot verified.

## Theme and chrome
- [x] Phone typography: `theme/Type.kt` → downloader `ui/theme/Type.kt`.
- [x] Midnight role mapping: `theme/Midnight.kt` → `ReferenceChromeTokens.kt`.
- [x] Original navigation renderer copied: `components/FloatingNavigationBar.kt` (same filename).
- [x] Original nav constants: 56dp height, 12dp margins, 6dp padding, 22dp icons, 48dp idle slots, 35dp selected capsule, 20dp outer corners.
- [x] Original Haze glass: 22dp blur, .62 tint, .04 noise, .33 input scale; original painted fallback and rim gradients.
- [x] Five destination slots; Library uses real playback history, Settings remains fifth.
- [x] Original header gradient copied: `components/ChromeFade.kt`, .86/.48/transparent stops.
- [x] Original source/genre-chip geometry: 16x8dp inset, .30 frosted tint/.52 painted fill, .18 border, 50% corners.
- [x] Header wordmark asset copied at original 22dp height; 48dp action targets.
- [x] Original hero gradient restored: .30/.12/.45/canvas at 0/.42/.72/1.
- [x] Shared Haze source connected to scrolling content; floating navigation overlays content.

## Home and Browse
- [x] `home/HomeScreen.kt` hero/rail structure → CatalogHero/CatalogHomeSections.
- [x] Original centered art crop and six-page carousel; real provider metadata only.
- [x] In-place Expand/Carousel, three-column phone layout; expanded rows remain lazy.
- [x] Continue Watching uses persisted real playback history.
- [x] `components/AnimeCard.kt` geometry → CatalogPosterCard; actual metadata adapter.
- [x] `search/SearchScreen.kt` header/search/filter/source/results arrangement → CatalogScreen.
- [x] Fixed Browse source-label/chip/divider overlap: one Column owns all children.
- [x] Browse three-column phone/four-column tablet layout and paging.

## Downloads, details and settings
- [x] `downloads/DownloadsScreen.kt` title/search/filter/grid/detail structure → LibraryScreens.
- [x] Source-derived series/episode/artwork/badge render functions → DownloadReferenceCards.
- [x] Episode actions retain source ordering, artwork dimensions, text spacing and actual resume labels.
- [x] Pending batch entries visible immediately, with preparation controls routed correctly.
- [x] Detail hero/tab/action geometry and SeasonDownloadDialog sheet → SiteDownloadScreen.
- [x] Sheet quality/range/episode-selection controls backed by provider data.
- [x] Settings header/grouped row arrangement mapped to available downloader settings.

## Player, imagery and states
- [x] Player uses custom legible controls; one bounded decoder, autoplay/next/selector/resume.
- [x] Slim seek track and accessible 44dp gesture target.
- [x] Persistent bounded thumbnail cache, canonical keys and detail-artwork recovery.
- [x] Responsive srcset parsing corrected; expired public-CDN URLs normalized safely.
- [x] White/black system splash replaces purple artwork; Anilili loader/splash geometry retained.
- [x] Loading/error/empty/access-denied states retained with actual actions.

## Explicit backend adaptations
- Provider chips replace anime genres; unsupported ratings, seasons, audio tracks and calendars are not fabricated.
- Header actions open actual downloads/link controls rather than anime notification/calendar features.
- Media3 short-drama controls adapt portrait playback; the anime player's full feature set is not copied.
- Appearance settings for arbitrary generated palettes/TV devices have not been ported; the reference default phone palette is used.

## Verification still required
- [ ] Device screenshots against the user's reference at matching font/display scale.
- [ ] Android 12+ live blur appearance and GPU behavior on the user's device.
- [ ] Below-Android-12 painted fallback appearance.
- [ ] Full device interaction pass: search/collapse, expanded rows, download range sheet, queue controls, saved/online autoplay and resume.

## Complete active shared-component inventory

| Local component | Reference / disposition |
|---|---|
| FloatingNavigationBar | Original source copied; imports/token provider mapped |
| DownloaderNavigation | Destination adapter only; five real routes |
| ChromeFade | Original source copied; default canvas fade adapter |
| ReferenceBackdropBlur | Original backdropBlur body copied; Android capability check |
| ReferenceChromeTokens | Midnight-to-Material role adapter |
| ReferenceHomeChrome | HomeTopChrome / GenrePill phone geometry; provider callbacks |
| CatalogHero | HeroPager/HeroCard data adaptation; original hero gradient |
| CatalogHomeSections | MediaRail rows and in-place grids; outer lazy ownership |
| CatalogPosterCard | AnimeCard artwork/title geometry; provider metadata and recovery |
| ContinueWatchingRow | Home continue-watching layout adapted to actual local history |
| DownloadReferenceCards | Source-derived DownloadSeriesCard/EpisodeRow/EpisodeRowArtwork/CornerBadge |
| MediaComponents.Thumbnail | Generic cached image adapter; not a full screen |
| DramaPlayerSurface | Deliberately custom portrait player; anime player is landscape-oriented |
| RemoteDramaPlayer / ShortDramaPlayer | Source/local media adapters to shared portrait player |
| AppLaunchSplash / WaterFillLogoIndicator / AnililiSilhouette | Source animation and path geometry adapted |
| SettingsRow | Downloader settings adapter to reference row arrangement |
| DestinationSummary | Folder-grant-specific control; no identical anime-model binding |
| DesignSystem.AppCard | Generic error surface only; not used for catalog/download cards |
| Legacy ChoiceMenu/Loading/EmptyState and old rail renderer | Unused approximation renderers removed |

The Settings/About and folder/session/diagnostic dialogs expose downloader-specific
controls; they are not claimed to be verbatim copies of anime-account/server dialogs.
Notifications/calendar are not simulated. Their header slots invoke real downloader
actions. Live glass and the source render geometry are copied; those data bindings
are intentional adaptations.

## Automated source comparison

Rendering bodies compared after stripping package/import lines and blank lines:
- `FloatingNavigationBar.kt`: exact match; SHA-256 prefix `84e66f57c78d6a4b`.
- `ChromeFade.kt`: exact match; SHA-256 prefix `f6d8cbd381e29ae6`.

This verifies source identity for those renderers, not device pixels or runtime blur.

## Build/package verification
- [x] Debug and signed release build: `/tmp/android-setup/ui-checklist-final-build.log`.
- [x] Release lint passes.
- [x] APK signature / signing certificate, native exporter hash and zip alignment verified.
- [x] Hosted download byte-range resume and full-file SHA-256 verified.

Latest release: http://20.120.96.17/builds/app-release-cce6c8e65683.apk
Latest debug: http://20.120.96.17/builds/app-debug-6e6f513011d1.apk

## Download playback-state correction (current source)
- [x] Re-read original `DownloadsScreen.EpisodeRow` and `Common.WatchProgressBar`.
- [x] Map `surfaceVariant` to the reference AMOLED `containerHigh` (neutral #1B1B1B); the omitted role inherited Material purple.
- [x] Persist completed episodes separately from resume positions; completion survives later playback saves.
- [x] Show Watched/check and partial playback progress on download rows; keep transfer progress separate.
- [x] Remove fallback Original/Best subtitles, and use the saved-file location label.
- [x] Details primary action uses Play/Resume, including shows opened from Library.
- [x] Replace Source and session with Link options; remove repeated content-type footer.
- [ ] Verify these changes on the connected phone (ADB currently disconnected).
- [x] `WatchProgressBar` copied directly from Anilili `components/Common.kt`, with playback fraction supplied by our history adapter.
- [x] Download overflow supports Mark as watched / Mark as unwatched for external-player use.
- [x] Details Back/Save overlay uses the copied `chromeFade(overHero = true)` over edge-to-edge artwork; no opaque Details bar.
- [x] Details tab says Overview; queue text names episodes and remaining preparation rather than links/session jargon.

Current release: http://20.120.96.17/builds/app-release-126872045e39.apk
Release build/lint, signature/certificate/alignment and hosted download resume passed.
WatchProgressBar rendering body compared verbatim with the Anilili source: exact match.
Device installation and screenshots remain pending because the paired ADB endpoint timed out.

## Shared online/downloaded playback
- [x] Online and downloaded players use `SiteDownloadEpisode.url` / `DownloadIdentity.locator` for the same resume entry.
- [x] Pausing saves immediately; lifecycle pause, close and periodic saves remain.
- [x] Playback opened from catalog/Library checks exported episode identity and readable content URI before resolving online media.
- [x] Each next episode repeats that check; incomplete downloads are not selected.
- [x] A local playback failure falls back online once for that episode in the current session.
- [x] Sources failing before ready do not overwrite an existing resume entry with zero.
- [ ] Device verification: online pause → download → local resume; mixed local/remote next episodes; missing/corrupt local file fallback.

## Library and settings follow-up
- [x] Library shows Continue watching and Saved shows as separate horizontal rows, both visible.
- [x] Continue-watching cards show exact episode/time, playback progress and downloaded/total count.
- [x] Saved cards show resume/watched state and downloaded/total count; details show the same summary.
- [x] Refresh history after both downloaded and online playback closes; register downloaded shows in history.
- [x] Settings index ports the source's eleven categories, outlined Material icons, three columns,
      26dp glyphs, 16dp/6dp tile padding, 8dp icon/title gap and 16dp tile corners.
- [x] Phone-only settings adapter omits television focus-fill/badging. Search filters category titles.
- [x] Actual downloader settings remain connected under their respective categories.
- [x] Player autoplay/next controls persist; Notifications exposes announcements and Android permission/channels.
- [x] Source-derived signed Nostr notices use a separate project key and announcement tag.
- [ ] Full Anilili subsection functionality is not ported: this app has no AniList/MAL accounts,
      subtitle appearance controls, generated theme picker or new-episode subscription service.
      Those category pages describe current available behavior rather than expose fake switches.
- [ ] Latest Library/Settings/device notifications still need phone verification; ADB disconnected.
- [x] Settings search uses the copied phone PageSearchField renderer (TV focus adapter omitted).
- [x] Copied the original announcement notification drawable; JNI verifier and Android loader kept in R8.
- [x] Library counts refresh after the initial asynchronous download-store snapshot completes.
- [x] Cheaper-model subagent source review: no blocking defects; no runtime checks claimed.

Current signed release: http://20.120.96.17/builds/app-release-87fa24f49bd3.apk
SHA-256: `87fa24f49bd39966e352f8bd87d0c83aa750940587703f11bbf2fee5e872d166`.
Final build/lint passed (`/tmp/android-setup/final-library-settings-notices-release.log`).
Signature/certificate, zip alignment, bundled native hash, JNI retention, key exclusion and hosted download resume verified.
Device install/visual testing remains pending: ADB is disconnected.

## Notice delivery and KissKH follow-up
- [x] Removed Link options and its details-row entry entirely.
- [x] Added signed-file notice mirror with original pinned-key/hash/signature checks; relay fallback remains.
- [x] Source failures now retry and show feedback; manual checks report checking/no-new/verification/fetch/blocked outcomes.
- [x] Check global notification permission and individual channel importance before marking a notice delivered.
- [x] Expired mirror falls through; publisher atomically refreshes mirror on real announcement broadcasts only.
- [x] Added KissKH source with Korean TV-only catalog parameters and South Korea/TVSeries detail validation.
- [x] KissKH search uses cached complete Korean index; global website Search ignored country in live probes.
- [x] KissKH episode resolution follows the public web-client request token and validates episode membership.
- [x] Compiled request-token function matched the website token on a Korean episode; CDN returned valid MP4 data with byte-range support.
- [x] Preserve decimal bonus-episode labels, selection and progress identity.
- [x] Local playback retains full known series metadata instead of replacing it with only downloaded episodes.
- [ ] Device visual/notification/playback checks remain pending because ADB is disconnected.

Current signed release: http://20.120.96.17/builds/app-release-c41d8f223cff.apk
SHA-256: `c41d8f223cfff56f0abaa590d829f77903857793c4794c1a701498169d5bd666`.
Build/lint passed: `/tmp/android-setup/kisskh-delivery-history-final-release.log`.
Signature/certificate/native bundle/zip alignment and hosted byte-range download verified.
Fresh test notice `test-delivery-1794122711` is signed, live on the mirror and expires after one day.
Device installation/notification receipt remains unverified: ADB disconnected.


## Original non-drama player and KissKH captions
- [x] Actual Anilili renderer, control panels, gestures, TV controls and playback services copied for EroticMV/KissKH.
- [x] Host catalog, local downloads, shared resume/watched history, save and download actions connected.
- [x] Original caption import/styling/timing and optional OpenSubtitles account/search retained.
- [x] KissKH website subtitle API and separate request-token GUID inspected; all returned supported language tracks localized.
- [x] Website SRT/VTT and legacy cue transformations reproduced; real SRT plus three encrypted-format smoke checks passed.
- [x] Offline captions persist outside cache and remain referenced by saved episode metadata.
- [x] Cheaper subagent source review completed; no blocking issue found.
- [ ] Physical-device visual, subtitles, PiP, Cast and external-player return checks (ADB disconnected).

See [source inventory and adaptation checklist](REFERENCE_PLAYER_PORT.md). Build artifact status follows after final packaging.


## Player-port release verification (2026-11-08)
Signed APK: http://20.120.96.17/builds/app-release-99bae75377e1.apk
SHA-256: `99bae75377e180836f4a3af7329032d26e9c143cbfbc20bb6b389ea18a19bae3` (22,186,942 bytes).
Final release compilation and lint passed: `/tmp/android-setup/reference-player-final-release.log`. Certificate, signature, ZIP alignment, unchanged FFmpeg and hosted byte-range download checks passed. Compiled subtitle smoke passed all five sampled live language files and three legacy decoding formats. Device install/playback/visual/PiP/Cast checks remain pending: `adb devices -l` is empty.
