# Animepahe DL for Android

A Material 3 downloader using the search, episode API, source selection, and
Kwik/glot playlist-resolution flow from
[animepahe-dl](https://forgejo.excreal.duckdns.org/merlin/animepahe-dl).

## Use

1. Type a title to search automatically after a short pause. Results show posters and release years. Clear the field to return to the empty Explore screen.
2. Filter these results by year/type, or sort by title/year.
3. Open a result to load its synopsis and every episode API page.
4. Select individual episodes or **Select all**. Quality and audio choices are checked against the sources shared by every selected episode; choose from those available options and download.
5. Open **Downloads** for collapsible show groups, local episode search, and compact episode rows with small posters, quality/audio tags, progress, and size. Filter active/completed episodes, tap play, or use the actions menu to pause/resume HLS downloads, retry failures, or delete after confirmation.
6. Use the compact rows in **Settings** to choose the output folder, default audio (source default, Japanese, or English), connection count, or clear browser session data. Unavailable preferred audio falls back to the source default; per-show audio remains selectable.

There are no demo results or simulated downloads.

## Sessions

HTTP is attempted first. Detected Cloudflare challenges open Android WebView
for manual verification, analogous to the desktop project's WebKit fallback.
Complete verification; once the destination page is ready, the app saves the session and retries once automatically. The shield icon and **Settings → Open verification** also open the
browser manually. WebView stores the browser profile/cookies privately; HTTP
and image requests use those domain-scoped cookies and the same saved user
agent. No cookies need to be pasted and no session IDs need to be entered.

WebView and OkHttp have different TLS fingerprints. A site can reject HTTP
requests despite successful browser verification. The app reports failures;
verification is not guaranteed and is not automated or bypassed.

## Downloads

- **HLS:** Media3 downloads manifests, segments and supported HLS encryption into
  `Android/data/com.example.animepahedl/files/hls-temp`. A foreground export
  service reads the completed cache with Media3 Transformer and creates an MP4,
  preserving the HLS segment timeline and encryption handling. The previous
  concatenated-TS extraction shortcut has been removed. Before publishing,
  the app verifies video presence and compares both container duration and actual
  audio/video sample timelines with the cached playlist. If the first writer fails
  or produces a truncated track, it retries once with Media3’s compatibility writer. A truncated output fails without deleting the cached source.
  The primary writer creates fragmented MP4 to avoid repeated seek-table rebuilding during assembly and device-specific platform muxer crashes.
  The Transformer writes into the pending output document and validation reads
  that same document, avoiding an intermediate MP4 and a second full-file copy.
  Downloads distinguishes waiting, MP4 preparation and final copying, with
  progress for each active stage; HLS exports finish after validation, without
  a separate copying stage. The temporary HLS source is removed only after the
  output is validated and published. Export failures retain the source and
  expose **Retry save**. Older private HLS caches remain available for export.
- **Direct MP4/WebM:** Android DownloadManager stages files in the app's
  `files/direct-temp` directory under Android/data. Completed files are copied
  to the same output folder, then the temporary download is removed.
- **Output folder:** defaults to `Downloads/Animepahe`. Settings can choose a
  folder with Android's document picker (including a writable SD-card folder),
  or use a named subfolder under Downloads. Each new download remembers its
  destination. **Retry save** uses the current Settings destination so you can
  recover from a revoked folder permission or full storage. Videos already
  saved remain in their original folders and survive app uninstall.
- **Session reset:** Settings clears WebView cookies, website storage and browser
  cache after confirmation. It does not delete downloaded videos or staging
  media. Session clearing is disabled while a site request is running.
- **Recovery:** HLS manifests, segments, and progress persist across process crashes.
  Reopen the app to restart unfinished work using the cached data. Disconnected
  downloads wait for connectivity; Android’s persistent scheduler restarts the
  service when network requirements are met. Transient network/I/O and HTTP 408/429/5xx
  failures retry after a delay through a persisted Android job, including after process death or reopening the app; verification
  and permanent HTTP failures need user intervention. HLS pause state is saved;
  use the episode's actions menu to resume. Android DownloadManager manages
  direct-file network recovery. Android force-stop and foreground-service time
  limits can require reopening the app. Interrupted MP4 assembly restarts from
  the completed cache rather than downloading the episode again.
- Download preparation (source resolution) requires the app to remain open;
  the resulting downloads and exports are handed to background services.
  Interrupted exports resume when the app next opens. A stalled writer times out after five minutes and can use the fallback writer. Staging and final MP4
  output coexist during export, so allow space for both. Android's
  foreground-service restrictions/time limits still apply.
- Like the desktop implementation, packed player scripts are submitted to
  `https://glot.io/api/mux`. This third-party service and the site's page/API
  formats must remain compatible. Unavailable quality/audio options fail
  explicitly instead of silently choosing another language.
- No yt-dlp, Python, GTK, WebKitGTK, or ffmpeg installation is needed on Android.

Only download media you have permission to save. Live-site verification and
downloads must be tested on a device; a successful build does not prove that
the current site's protection accepts Android sessions.

## Build

Use Android SDK 37 and a compatible JDK (21 recommended):

```sh
bash ./gradlew assembleDebug testDebugUnitTest
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. Minimum Android: 15 (API 35).

## Device verification

`HlsExportTest` supplies a generated three-second H.264/AAC HLS fixture directly
to the download cache, exports it through the foreground service, checks that
the MP4 has audio/video and a valid duration, then checks temporary-cache cleanup.
A discontinuity fixture checks timestamp resets between segments, and an intact-audio/short-video
fixture ensures container duration cannot hide missing video.
`LibraryNavigationTest` checks the Downloads and Settings entry points.

```sh
bash ./gradlew connectedDebugAndroidTest
```

For the optional continuous 24-minute export benchmark, install both debug APKs
and run `bash scripts/make-export-benchmark.sh` (host ffmpeg and adb required).
The generated fixture stays outside the repository.

Settings → Download speed controls a shared pool of 2, 4, 8 (default), or 12 HLS
segment connections while keeping up to two episodes active. Changes resize the
pool immediately. Host/network limits still determine achievable throughput.

New exports use `Output folder/Show name/Show name ep01 720p.mp4` (or WebM
for direct WebM sources). Episode numbers are padded to at least two digits;
quality comes from the selected source. Existing files are not renamed.
