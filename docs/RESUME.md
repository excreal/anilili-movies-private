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
