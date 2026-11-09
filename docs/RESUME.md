# AnimepaheDL resume state

Updated: 2026-11-06

## Latest instructions and status

The user requested a compact Downloads UI, persistent background link preparation
with automatic retry, progress indicators on collapsed show groups, external
playback using the system default player, and library updates after files are
deleted outside the app. They also requested a crash/performance review followed
by debug and signed release builds with bundled FFmpeg.

Implementation and review are complete. Final debug and signed release builds
succeeded (`./build.sh all`); release vital lint passed. The release-only Gson/R8
persistence issue was fixed in `app/src/main/keepRules/rules.keep`, and final
mapping confirms the worker and persisted model names are retained. Both APK
signatures and bundled ARM64 FFmpeg checksums passed. The release signing
certificate matches `.signing/animepahedl-release.p12`; ZIP alignment passed.
Public HTTP resume plus complete-file SHA-256 passed for both published APKs.

No emulator may be installed or started. The server has no GUI or connected
Android device. The user will perform runtime tests; no test suites have been run
for the current changes. Debug/release builds and artifact checks are authorized.
Preserve all uncommitted changes; do not commit without a request.

## Changes in the latest task

- `PreparationJobs.kt`: WorkManager 2.11.2 owns separate `episodes` and `sites`
  batches. Journals and generation tokens are in the existing private Room
  operations table. Network constraints, foreground notifications, bounded
  attempts, exponential retry, pause/resume/discard, and process recovery are
  independent of ViewModel lifetime. Real session verification still needs the
  user's browser action. Generation checks prevent cancelled work restoring an
  obsolete batch. Status observation retries storage errors rather than crashing.
- `DownloaderViewModel.kt` / `SiteDownloadViewModel.kt` now observe and control
  that work instead of running network preparation loops themselves.
- Downloads UI removes the duplicate collection heading and large preparation
  banners. Search/sort share one compact row. Show cards use two lines and smaller
  thumbnails. Touch targets remain usable; font scaling is not disabled.
- `DownloadGroupProgress.kt` provides active-phase, byte-weighted download
  progress, saving/checking status, queued/paused/network/failure states. Collapsed
  groups show a circular indicator and percentage where known.
- `DownloadLibrarySnapshot.kt` filters/sorts/groups on Default, retaining stable
  group rows where unchanged instead of doing this work on the UI thread.
- `ExternalPlayback.kt` launches ACTION_VIEW with a scoped read grant and ClipData,
  respecting the system default player. LibVLC, SavedPlayer/SavedPlayback, their
  debug test host and tests, and the unused Media3 UI dependency were removed.
- A single application ImageLoader has an 8% heap cache. Export UI notifications
  are throttled, per-segment refresh events reduced, the segment queue bounded,
  historical completed direct downloads excluded from polling, and temporary-file
  work moved off-main. Library refresh stops while the app is hidden. No claim is
  made that a device-specific crash is proven fixed without device logs/testing.
- `SavedFilesWatcher.kt` observes MediaStore and selected document trees while
  visible, plus foreground polling and a resume check. Queries are cancellable
  and off-main; standard Downloads queries are batched. Only confirmed missing
  saved files have their metadata forgotten. Permissions/provider failures retain
  records; active transfers and another user's files are never deleted.
- `build.sh` handles the migrated signing env's obsolete absolute keystore path
  by finding the same named keystore in this checkout's `.signing` directory.

## General downloader feature from the earlier task

Explore has **AnimePahe | Video links** and keeps the three bottom destinations.
The supported non-TikTok reference providers are Dramatip, Kiss18, Narto Drama,
EroticMV, and direct HTTP(S) HLS/MP4/M4V/WebM, including encoded HLS wrappers and
supported mime_type query forms. TikTok/numeric IDs/signing/Go code are excluded.

Single videos have Download video and no invented episode number in filenames.
Series accept comma/whitespace ranges; blank selects all and `none` selects zero.
Numbered watch URLs initially select that episode. Source redirects cannot
silently substitute a different requested episode.

DownloadIdentity has nullable source/locator fields; original AnimePahe keys and
Room v1 JSON remain compatible. Site retries resolve their original provider.
Per-download HTTP context is private; Netscape imports honor domain/path/secure/
expiry rules per request. Headers are not leaked to unrelated redirects. Site
files use Media3; progressive export is cache-only and checks completeness.
HLS master remux selection preserves the chosen rendition's external audio.

## Signing and FFmpeg

- Actual key: `.signing/animepahedl-release.p12`; private env: `.signing/release.env`.
  Do not print credentials or alter the key. `./build.sh all` loads them.
- Expected release certificate SHA-256:
  `df6fcad0dd936146c3505209b93cfa10d6ab08e229e021ca705b3743f008a8b5`.
  It was independently exported from that key using keytool with an env password.
- Both APKs must include `lib/arm64-v8a/libffmpeg_exec.so`, 15713352 bytes, SHA-256
  `9085507b0dc32643b4d6d084a7e7d3469ef17907a7ba15c22d3997ed09c932aa`.
- Final APK signatures, release certificate identity, bundled FFmpeg hashes, and
  release ZIP alignment were verified. Final R8 mapping keeps PreparationWorker
  and the Gson model names unchanged; the configuration includes the new rules.
- Runtime test sources were added/updated but not executed. Earlier setup's 35
  passing JVM tests predate all of these feature changes.

## Environment and hosting

JDK 25; SDK `/opt/android-sdk`, API 37.0, Build Tools 36/37, NDK 28.2.13676358,
Gradle wrapper 9.8.2. `local.properties` is configured. Docker, Compose, Buildx,
and Nginx were installed and verified in earlier turns.

Nginx port 80 serves `/srv/downloads`. `debug/` and `release/` there symlink to
APK output directories. `/root` has www-data traversal ACL only. Native HTTP range
handling was verified; gzip is off and stalled writes allow 10 minutes.

Final immutable APK snapshots (retain them so ongoing downloads can resume):

- Debug: http://20.120.96.17/builds/app-debug-ccc62bfc326e.apk (49.6 MiB),
  SHA-256 `ccc62bfc326e7c4cba6ac53689fcc06c60fbf04e6fa7f77e5df6ed3ff413375c`.
- Release: http://20.120.96.17/builds/app-release-9e2dedcda0bd.apk (11.2 MiB),
  SHA-256 `9e2dedcda0bd989251f7b472c664658e05b92c19facc03707a7ad8ae1d041735`.

Artifact metadata: `/tmp/android-setup/final-apk-metadata.json`.
Final build log: `/tmp/android-setup/final-persistence-safe-build.log`.
`/debug/app-debug.apk` and `/release/app-release.apk` also point to these current
builds through the output-directory symlinks. The intermediate release snapshot
`app-release-58e80009fcac.apk` was removed because it lacked the persistence rules.

## Existing work and reference

The preexisting uncommitted FFmpeg/cache validation/export changes, fixtures,
release-signing setup and other source edits were preserved except the embedded
player that the user explicitly replaced with external playback. Source files
are not re-encoded for playback. HLS validates all cached resources and FFmpeg's
exit; audio and video duration equality is not required.

`downloader/` remains ignored for provider parity review; no unlicensed Rust code
was copied verbatim. Current bundled-tool notices are in
`docs/THIRD_PARTY_NOTICES.md` and the FFmpeg license assets.

## Reinstall recovery and selection toolbar (latest)

Prior-install MediaStore Downloads are not visible as current-app files. Downloads
now require a persisted read/write SAF folder grant before preparation/transfers.
Choose the original Download/Animepahe folder (or a single show folder). A folder
scan pauses transfers, clears only owned incomplete outputs, and adopts exact
show/episode/quality/language matches, including Android numbered filename aliases.
Queued duplicates are removed without deleting the existing video. New output
creation rejects unexpected auto-numbered names. Cancel actions preserve adopted
files; explicit saved-file deletion remains available.

Selection actions now occupy the top app bar: count, select all, delete and an
eligible-action overflow menu. The former multirow selection panel is removed.
Both build variants are built; no emulator/device or test suite is run.

Latest verified artifacts (supersede links above):
- Release: http://20.120.96.17/builds/app-release-49db41d7637c.apk
- Debug: http://20.120.96.17/builds/app-debug-6be01473328c.apk

Both passed APK signature and 16KiB zip alignment verification; release certificate
matches the local .signing key. Both include the expected arm64 FFmpeg executable.
Release persisted model/worker names are retained. Public HTTP range/resume downloads
returned 206 and matched each full APK SHA-256. Build log:
`/tmp/android-setup/reuse-guarded-build.log`. Device behavior remains user-tested.

## UI consistency cleanup (November 7)

Removed the homepage promotional card and oversized branding. Empty initial search
moves toward the center; entered queries/results retain top positioning. Search
and URL inputs use outlined rounded fields. Reduced result/detail poster sizes,
collapsed synopsis to three lines with Read more, and hid range inputs until
Select range is tapped. The episode footer is one row containing folder access
and Download (count), replacing the three-row block. Settings use aligned icon,
text and trailing-control columns with compact 64dp minimum rows. Destination
summaries are single-line folder buttons; full paths remain in the dialog.
Checkbox rows in Video links use the whole card as their single click target.
No emulator installed or launched; validation consists of builds/static review
and APK/signing/native/HTTP checks, with device testing left to the user.

Verified UI-cleanup artifacts, superseding earlier builds:
- Release: http://20.120.96.17/builds/app-release-5c0a77ba8e57.apk
- Debug: http://20.120.96.17/builds/app-debug-632634991f15.apk

`build.sh all` succeeded (release lint included). APK signatures, release-key
certificate match, bundled arm64 FFmpeg hash, zip alignment, R8 persisted names,
and public HTTP resume/full-file SHA-256 were verified. Build log:
`/tmp/android-setup/ui-cleanup-build.log`. No device visual validation performed.

## Shared Material UI components

Moved choice menus, destination controls, thumbnails/loading/empty states and
settings rows into `components/`. Shared AppCard surfaces animate size changes;
episode range controls use AnimatedVisibility and synopsis expansion animates.
Explore uses a smaller initial offset with a search heading and hides sort until
results exist. Theme surfaces have a darker neutral hierarchy. Main screens use
shared card shapes. No emulator or device visual validation has been performed.

## Custom downloader only

User requested removing AnimePahe search/downloads and using /root/Anihili theme;
the actual reference directory is /root/Anilili. Removed search/details composables,
source tabs, anime preparation worker implementation and anime-specific settings
entries. Custom downloader is the only home screen. Legacy anime preparation is
discarded at startup and new/resumed/retried legacy transfers are rejected.
Existing saved-file records remain usable; shared legacy model classes retain
serialization compatibility. App label is Video Downloader; package and key remain
unchanged for updates. Theme adapts Anilili's default AMOLED monochrome palette
and copies its phone typography; Appearance settings are not ported.

## Remote catalog browsing and Anilili display integration

Explore defaults to Dramatip popular, with source selection, debounced search,
poster grid, spotlight pager, pagination where offered, detail-to-download and
secondary Paste link. Shared SiteCatalog is an internal application API, not a
public hosted REST server. Dramatip uses /category/popular and /search?q=;
Kiss18 searches its downloaded catalogue-browse.mjs snapshot; Narto uses edge
home/search JSON-LD. EroticMV WordPress browsing is implemented but server access
was HTTP403 and requires further device verification. TikTok remains excluded.

Copied/adapted Anilili silhouette, water-fill loader and launch splash, phone poster
cards and typography, plus downloads artwork/row styling. Background APIs do not
execute provider JavaScript. Existing saved files and signed update identity retained.

EroticMV follow-up: browser-style Accept/Language/Sec-Fetch and Referer yielded
HTTP200 for home and ?s=love. No cookies were issued in that probe. Shared other-site
transport now retains scoped session cookies and merges imported cookies;
EroticMV warms home before first search. Pagination /page/2/?s=love verified.
Dramatip transport likewise retains cookies and uses browser navigation headers.

## Vertical short-drama playback

Dramatip, Kiss18 and Narto are explicitly short-drama providers; EroticMV is movies,
direct links are generic video. Added Watch action for resolved series/video and
in-app saved playback. Both remote and local vertical pagers own one ExoPlayer,
prepare only settled episodes, FIT without cropping, pause in background, release
on close; saved playback retains External fallback. Media3 UI dependency restored.
Reference inspection: Anilili uses Media3 offline cache + Transformer export, no
bundled FFmpeg. This app retains its established FFmpeg export implementation;
full Transformer/export-engine migration has not been done.

GitHub player review: compared androidx/media demos/shortform and
clown6613/ComposeReels. Added stable remote episode keys and bounded 16MiB player
buffers with 5–15s duration targets; one active decoder, no neighboring preload
manager. Media3 #3445 documents a 1.11.1 preload memory retention issue, so nearby
preloading is deliberately absent. 'Catalog updated' reported by user was a
subagent progress notice, not an app message (user clarified).

Final custom-catalog/vertical-player APKs verified and published:
- Release: http://20.120.96.17/builds/app-release-0a7f229e825c.apk
- Debug: http://20.120.96.17/builds/app-debug-5b1a1b31e60e.apk

Build log /tmp/android-setup/shortdrama-final-build.log: BUILD SUCCESSFUL.
Verified signatures, release certificate matches .signing, bundled FFmpeg hash,
16KiB zip alignment, R8 persisted names, and public HTTP resume/full-file SHA256.
No emulator/device runtime tests run. Transformer engine migration not performed;
existing FFmpeg export retained. Anilili player architecture adapted, not a full
copy of its background MediaSessionService/custom controls.

## Dedicated details and download-range dialog

Catalog clicks now show Details with poster, provider/type, episode count, available
synopsis, Stream and Download. Source URL/session controls are secondary. Download
opens a modal with comma-separated episode numbers/ranges (e.g.1-5,9,12-20), selected
count, quality and destination. Invalid/unavailable ranges disable confirmation.
Explore now has remote selection in the app bar, search with secondary paste-link
action, compact 200dp spotlight and wider two-column phone cards with image fallback.

Further visual correction based on user screenshots: replaced generic navigation
with adapted Anilili floating bar (white selected capsule, icons, gradient/rim,
spring animation). Home uses actual MediaRail-style headings and horizontal cards;
Browse is a separate search/grid destination. Expanded grids and home share cards.
Earlier release lint failed inside Kotlin FIR/UAST analysis after sources changed
mid-build; final build reruns with source edits finished.

Final details/bottom-sheet/navigation build:
- Release http://20.120.96.17/builds/app-release-36313ed16c29.apk
- Debug http://20.120.96.17/builds/app-debug-dcce6857a915.apk

/tmp/android-setup/anilili-sheet-build.log: BUILD SUCCESSFUL, release lint passed.
Signatures/release cert, bundled FFmpeg hash, zip alignment, retained model names,
and public HTTP resumed-file hashes verified. APK dex checked for new range-sheet
strings. No device/emulator visual or interaction testing was performed. Prior Play
Protect classification remains unexplained; packaging/signature verification does
not prove the classification is cleared or a false positive.

## Poster loading, catalog caching, downloads grid

Fixed image loader inheriting AnimePahe Referer: remote artwork now uses independent
bounded HTTP, URL-scoped sessions/referrers, Dramatip no-referrer, explicit disk and
memory image caches, crossfade and stable image keys. Responsive Dramatip URLs now
prefer ~384px vs3840px and ignore flags. Kiss18 home includes fullplayable catalog
after curatedtitles. Catalog ViewModel caches up to8 metadata pages in memory and30
on disk,600items per persisted page,15min freshness,stale-display before refresh.
Home immediately draws firstpage and fetches up to2 extra pages; Browse/Discover
append near scrollend. Metadata data classes have @Keep for release persistence.
Downloads now postergrid -> separate seriesepisode list, matching Anilili structure,
with groupcounts/bytes/progress/delete confirmation and retained rowactions.

Latest verified cache/grid builds:
- Release http://20.120.96.17/builds/app-release-470a5af2a2bd.apk
- Debug http://20.120.96.17/builds/app-debug-450874d811a8.apk

Build /tmp/android-setup/cache-grid-final-build.log succeeded, release lint passed.
APK signatures/releasekey, native FFmpeg hash, zip alignment, R8 persisted names,
HTTP byte-range resume/full-file hashes verified. Downloads header now belongs to
its grid/series page rather than duplicated outer toolbar. Device runtime and
visual behavior remain untested here; no emulator installed/launched.

## Full screen port + playback/queue fixes

User explicitly replaced approximation layouts with Anilili phone screen structures.
Home now fullbleed460dp6itemhero, actual copied headerwordmark, overlay sourcechips,
centered title/dots, ContinueWatching slot,108dp rails and INPLACE persection
Expand/Carousel3column grid (Browse separate3column grid). Downloads PageTitleBar,
PageSearch, filterrow,responsiveposters and episode page geometry ported. Detail
centeredsticky pills/actionordering/qualitytrack/episodecheckboxgeometry refined.

Online/offline now share DramaPlayerSurface: custom controls,no nativePlayerView
controller, autoplay, auto-next, episode sheet,seekbar, persistent5sec/pause/switch
resume keyed source locator. Local/remote key identityshared; Home history uses
registeredsource metadata and actual playback timestamps. Onebounded16MiBdecoder.

Every selected download immediately represented by preparationjournal placeholders;
actual transfers replace by show/episode, independentquality. Merge used in both
Library/Main selection. Placeholderpause/resume batch routedto preparation;
cancel routedgeneration-safejournalremoval, realactionsremainrepository.

Imagefix further verified:72DrTip URLs,7expiredsignedCDNs recover from samehostpublic
uploadpaths(verified200);cachedoldURLs rewritten. OneupstreamTikTokcover403 andone
expiredTLSCDN remain; noTLSbypass. ImageHTTP200HTML/JSON rejectedbeforecache.
Blacklauncherbackground andwhitevectorstartupmark replacepurpleforeground.

Final reference-screen geometry adjustments: hero header now uses actual copied
Anilili wordmark resource; sourcechips overlay artwork underheader. Browse owns
its header. All content scrolls behind floating navigation with108dp final padding.
Expanded Home section rows were moved into outerLazyGridScope to avoidcomposing
hundreds of postersatonce; same3col look and in-placeCarousel toggle retained.
Both carousel andexpandedDiscover requestmore nearend. Settings ownsreference40sp
header/groupsections; no duplicateouterbar. Playbackmetadata timestamps feedHome
ContinueWatching, unfinishedonly. Launcherforeground nowwhitevector; blackday/night
window backgrounds. Allscreensretainactualprovidermetadata, no syntheticratings.

Final verified reference-port APKs:
- Release http://20.120.96.17/builds/app-release-0dad9d1c84b9.apk
- Debug http://20.120.96.17/builds/app-debug-0312367f2449.apk

/tmp/android-setup/reference-port-final-build.log succeeded, release lintpassed.
Verified APK signatures/releasecertificate,bundledFFmpeg hash,16KiBzipalignment,
persistedR8modelnames,HTTPresumedfullfileSHA256; newUI/player/queue stringschecked
inDEX. No emulator/device runtime orvisual verification performed. PriorPlayProtect
classification remains unexplained and is not certified cleared bythesechecks.

## Overlay contrast / thumbnail follow-up

Provider chips over artwork now use explicit white/black selected colors and dark
opaque unselected surfaces, not inherited gray Material defaults. Hero title has
stronger lower gradient and textshadow. Player contrast/compactseekbar and remaining
thumbnail fallback/cache investigation delegated to ownedfiles; buildafterfrozen.

Final contrast/direct-card-port build verified:
- Release http://20.120.96.17/builds/app-release-9c731505ba4a.apk
- Debug http://20.120.96.17/builds/app-debug-7893cb3a91d2.apk

Build /tmp/android-setup/reference-contrast-final-build.log succeeded, lint passed.
Player nowstrongtitlebackingscrim,whitebackicon,white/blackEpisodesbutton,4dpCanvas
seekbarwith44dpgesture/accessibilitytarget. DownloadReferenceCards.kt contains
source-derived actualcard/episoderow adapters includingrealresumehistorylabels.
Browse header/reset/source-filter/search/categories-divider/results geometry ported.
Hero490dp,title25sp,counttopright,activeindicatorpill and explicitchipcontrast.

Thumbnail follow-up found actual3reportedCDNimagesHTTP200/decodevalid, notuniformly
expired. Fixed doubleescapedresponsive-srcsetregex. ArtworkRecoveryrefreshesfailed
cardfromdetailOGonce; replacementURLpersisted forfuturevisits,canonicalv3imagecache
keysavoidoldbadentries. Allimageclientsretainboundedmemory/diskcaches; exceptions
remainerrors/placeholders, noTLSbypass. Runtime/devicevisual validation notdone.
APKsignature/releasecert,FFmpeghash,alignment,HTTPresume/SHAverified.

## Original chrome source copy + full UI audit

Copied original Anilili FloatingNavigationBar.kt and ChromeFade.kt, adaptingimports
only plus Midnight-role/appCanvasFade bridging. Added Haze1.2.2 (same asreference),
sharedscrolling hazeSource and exact22dpblur/.62tint/.04noise/.33inputscale; original
navpadding/iconslots/pillspring/paintedfallback/rim restored. Five navslots nowmap
Home/Search/Downloads/realhistoryLibrary/Settings. Header usesactualHomeTopChrome
TopAppBar64dp/statusinsets/wordmark22dp andcopiedGenrePill drawgeometry; sourcechips
retainproviderbindings. Herooriginal .30/.12/.45/canvas gradient restored.

Fixed Browse Sources overlap (siblings inLazyGriditemwereoverlaid) with oneColumn;
sourcechip32dp height/referencepadding. Fullinventoryandverificationstatus in
`docs/UI_REFERENCE_CHECKLIST.md`, distinguishingverbatimsource fromdataadaptations
and not claimingdevicevisualchecks. RemovedunusedChoiceMenu/Loading/EmptyState/old
railcomposables. Originalappnotifications/calendar controlsnotfabricated.

Final UI-audit/original-chrome build published:
- Release http://20.120.96.17/builds/app-release-cce6c8e65683.apk
- Debug http://20.120.96.17/builds/app-debug-6e6f513011d1.apk
Build /tmp/android-setup/ui-checklist-final-build.log succeeded, release lintpassed.
Signatures/releasekey,nativehash,alignment,HTTPresume/fullhashverified. Sourcebodies
FloatingNavigationBar andChromeFade compareexactlywithAnilili afterimports/package
normalization. UI_REFERENCE_CHECKLIST.md listsallactivecomponents,sourceadaptations
anduncheckeddevicechecks. No device/emulatorvisualverification performed.

## ADB device test + bookmark feature

Realdevice TECNOKM7 API35 connected. Initialrelease installed but firstdrawcrashed:
Haze header sampled containing parent source. Fixed by separate hero HazeState;
nav stillsamplespage source. Rebuilt signedrelease andinstalled; agentverifiedHome,
Details/48episodes/Episodestab,Downloadsempty,Libraryempty; nofollow-upcrashobserved.
Cheapgpt-6-luna verificationagent usedatuserexplicitrequest.

Added SavedShowsStore (persistent separatebookmarks),LibrarySavedShows/ContinueWatching,
Save/Unsave action inremote/localplayer anddetailsheader. MetadataGsonusesexisting
keptSiteDownloadSeries. New signedrelease installedsuccessfully:
http://20.120.96.17/builds/app-release-3a0dfcab03ae.apk
Build /tmp/android-setup/saved-shows-release-build.log passed incl lint. Releasekey,
FFmpeg hash,alignment,HTTPresume/fullhashverified. Device test ongoing in
`docs/DEVICE_TEST_REPORT.md`. Don'tclaimuntestedstream/downloadflowspassed.

Cheaperverificationagent finisheddevicepass on release3a0dfcab03ae: playersave
changedtounsave; streamposition08->25 withoutplaytap; episodepickerEpisode2;
Librarysaved1ChainedTogether persistedafteramforce-stop/relaunch;ContinueWatching
alsoappeared. Noobservedcrash. FloatingChatGPT sometimesobscuredrightside.
Deviceinteractionstopped. Fulltestmatrix `docs/DEVICE_TEST_REPORT.md`.

## Current follow-up release
http://20.120.96.17/builds/app-release-87fa24f49bd3.apk
Shared online/offline resume, downloaded-first catalog playback with one online fallback, immediate pause saves, two Library rows with exact episode/time/counts, copied Settings index/search, and separately keyed signed Nostr developer notices. Publisher docs: docs/DEVELOPER_NOTICES.md. Private key is ignored in .signing/announcement-key.properties; never publish its contents. No announcement was broadcast. Build/lint and package checks passed; cheaper-model source review found no blockers. Device currently disconnected; no new phone pass claimed.

## Korean-only KissKH + notice delivery follow-up
http://20.120.96.17/builds/app-release-c41d8f223cff.apk
Link options removed. Added signed HTTP mirror at /srv/downloads/notices/announcement.json, IP-only cleartext network config, mandatory original signature verification, expired-mirror relay fallback, global/channel block checks, UI check outcomes and Worker retries on source failures. Publisher updates mirror atomically only on announcement broadcasts. Fresh test notice test-delivery-1794122711 published; expires in one day. ADB disconnected, no device receipt claimed. KissKH adapter restricts country2/type1, inspects only South Korea TVSeries, cached Korean-index search, public client-token MP4/HLS resolution and generic downloads. Compiled token tested against actual website JS; Korean MP4 header/range probe passed. Separate subtitles/embed hosts not integrated. Local playback metadata merged with known full series so partial download playlists do not erase full totals. Cheaper source review passed. Details docs: docs/KISSKH.md and docs/DEVICE_TEST_REPORT.md. No commit requested.


## Original non-drama player and KissKH captions completed
EroticMV and KissKH now dispatch to ReferenceVideoPlayer, which binds the actual original Anilili PlayerSurface and its dependency stack, rather than a recreated player. 131 com/anilili files present;117 compare equal after host import substitutions. Full inventory/host adaptations: docs/REFERENCE_PLAYER_PORT.md. Original services/media session, Cast, external-player return, subtitle controls/import/sync, quality/audio, gestures, TV and PiP wired to host. Local-first, shared stable-locator progress, saved actions and downloads connected; vertical sources retain original host short-drama player.
KissKH subtitles follow public /api/Sub API with separate GUID, normalize source SRT/VTT and three legacy per-cue AES formats; persist as private VTT referenced by downloaded media. Not muxed into exported MP4. All five live sampled languages and synthetic three-format compiled Kotlin smoke passed. Optional OpenSubtitles key UI and copied original account dialog in Settings/Subtitles. No Anilili API key reused. Mobile cleartext policy now matches original for local Cast/external-player proxy; signed notice authenticity checks remain.
Cheaper verification subagent source review found no blocking progress/PiP/subtitle/local-first issue. No suites, no emulator, no commit. Final build/lint and artifact verification passed. ADB disconnected; no install/device run claimed.


## Player-port release verification (2026-11-08)
Signed APK: http://20.120.96.17/builds/app-release-99bae75377e1.apk
SHA-256: `99bae75377e180836f4a3af7329032d26e9c143cbfbc20bb6b389ea18a19bae3` (22,186,942 bytes).
Final release compilation and lint passed: `/tmp/android-setup/reference-player-final-release.log`. Certificate, signature, ZIP alignment, unchanged FFmpeg and hosted byte-range download checks passed. Compiled subtitle smoke passed all five sampled live language files and three legacy decoding formats. Device install/playback/visual/PiP/Cast checks remain pending: `adb devices -l` is empty.
