# Real-device verification

Device: TECNO KM7, Android 15 (API 35), 1080×2400. Connected over ADB.
Verification delegated to a cheaper subagent at the user's request.

## Found and fixed

The initial signed release installed but crashed on first drawing. The crash buffer
reported `Modifier.haze nodes can not draw Modifier.hazeChild nodes`.
The Home header sampled a parent blur source containing itself. The hero now owns
a separate Haze source; navigation samples the page source. Rebuilt and installed
as a signed update without clearing application data.

## Verified so far

- Signed update installation: successful.
- Corrected app remains running; Home artwork/carousel/cards render.
- Show details load and report 48 episodes for the tested title.
- Episodes tab renders numbered entries.
- Downloads empty state renders.
- Library empty state renders.
- No new AndroidRuntime crash observed in those corrected-build checks.

## Saved shows update

Latest installed release: `app-release-3a0dfcab03ae.apk`.
Library now separates explicit saved shows from unfinished playback history.
Player and details offer save/unsave; saved metadata persists in app preferences.
Playback/local download records are independent from bookmarks.

Player autoplay, episode selection, bookmarking and persistence checks are ongoing.
Not yet verified: real download/save completion, concurrent downloads, long playback,
background process recovery, or all providers. No existing saved files deleted.

## Final saved-shows verification (cheaper subagent)

Passed on the installed signed release:
- Player Save show changed to Unsave show.
- Video playback position advanced from 0:08 to 0:25 without another play tap.
- Episode picker changed the player to Episode 2 (1:43 duration).
- Library showed Saved shows (1), Continue watching (1), and Chained Together.
- After non-destructive force-stop/relaunch, the saved entry remained.
- Home displayed the Chained Together Continue Watching / Resume entry.
- No crash observed during these flows.

Evidence: `/tmp/player_saved.xml`, `/tmp/librarySaved.xml`,
`/tmp/librarySaved.png`, `/tmp/persist.png`; prior screen captures are listed in
agent reports. ChatGPT's floating window/keyboard intermittently obscured the
right side, so this is not a complete unobstructed screenshot comparison.
Settings, completed download/export, concurrent downloads and all providers were
not verified in this pass. Verification agent stopped device interaction.

### Current pending release: player controls and download playback state
Source/build changes include controls fading after three seconds, translucent player gradients,
numbered episode grid with 25-episode ranges, persistent completed flags and resume progress on
Downloads, reference neutral card color, and Play/Resume wording. These changes have not yet
been verified on the phone: `adb devices -l` currently returns no device. Earlier device results
above apply to the earlier installed release only.
ADB reconnect to the previously paired endpoint `100.118.88.70:33335` timed out.
No new APK installation or phone UI verification has been claimed. Completion marks apply
from this release onward; the prior implementation discarded completed positions, so old
watched episodes cannot be recovered automatically. External-player viewing can be marked
using Mark as watched / Mark as unwatched in each downloaded episode's menu.

Current signed release built successfully with release lint:
`/tmp/android-setup/download-reference-release.log`.
Signature/certificate, zip alignment, native exporter hash and hosted byte-range download verified.
APK: http://20.120.96.17/builds/app-release-126872045e39.apk
SHA-256: `126872045e395c5efbc1aa9e59101a77c7f1060a059da702198a1fe1a8bdfc23`.
ADB remains disconnected; this release has not been installed or visually verified on the phone.

### Offline-preferred playback (pending device verification)
RemoteDramaPlayer now checks completed exported records by source/show/episode identity or
original episode locator, validates read permission/nonempty content, and plays the saved URI
before attempting any online resolution. Incomplete records are excluded. If local playback
fails, the shared surface retries that episode online once. Both sources retain the original
episode locator as the history key. Pause writes immediately; failed preparation before READY
does not overwrite prior progress. ADB still lists no device; no runtime pass is claimed.

### Library and Settings/announcements release
Library now displays both rows simultaneously with episode/time, progress and downloaded/total
counts. Details display the same summary. Closing downloaded playback refreshes Continue watching.
The Settings category index uses source-derived Anilili tile rendering and Material outline glyphs;
sections bind available downloader behavior. Unsupported Anilili account/theme/subtitle services are
not claimed to be ported (see the checklist). The source announcement receiver/scheduler/verifier
uses a separate pinned project key and tag; publisher setup is in DEVELOPER_NOTICES.md.
Publisher local dry-run verified a genuine signature and rejected a content mutation. No broadcast
was published. Android JNI signature verification, system notification delivery/tap and all new
screens still need device validation; ADB is not connected.
The separate cheaper-model verification subagent reviewed the final bindings and found no blocking
source defects. This was not a runtime pass. Its operational caveat matches the original source:
relay fetch failures resolve as no announcement, with the next scheduled/foreground check trying
again rather than WorkManager immediate retry. Explicit checks now force a refresh.
The isValidSignedBy and computeEventIdHex method bodies compare exactly with Anilili source.
APK inspection confirmed the arm64 signature-verifier backend is packaged and private publishing
key text is absent. R8 retained the JNI entry class and Android loader.

Current signed release: http://20.120.96.17/builds/app-release-87fa24f49bd3.apk
SHA-256: `87fa24f49bd39966e352f8bd87d0c83aa750940587703f11bbf2fee5e872d166`.
Final build/lint passed (`/tmp/android-setup/final-library-settings-notices-release.log`).
Signature/certificate, zip alignment, bundled native hash, JNI retention, key exclusion and hosted download resume verified.
Device install/visual testing remains pending: ADB is disconnected.

### Notice failure follow-up and Korean-only KissKH
The phone's exact notification failure remains unconfirmed (ADB disconnected).
The original signed test event/key/signature/protocol and APK JNI retention passed
read-only/targeted checks. New code adds a verified signed-file mirror, global and
per-channel block checks, explicit check feedback, retry on transport/verification
failure, expired-mirror relay fallback and atomic publisher mirror updates. Link
options is removed from details. A separate cheaper-model review found no blockers.

KissKH is wired through catalog, detail inspection, playback and preparation/downloads.
Browsing/search are restricted to its Korean TV catalog; inspection rejects other
countries and movie types. Global website search ignored the country parameter in
live probes, so the adapter searches the cached Korean-only index instead.
Compiled Kotlin request token matched the site's JavaScript token on a Korean
episode. The returned media was direct MP4 (not HLS for that sample), with valid
ftyp bytes and HTTP range support. No phone playback/full download or subtitle
integration is claimed. Fractional episode labels and selection are preserved.
Local history registration now retains known full-series metadata when playing a
smaller downloaded subset, so 44 downloaded episodes cannot replace a known
64-episode total in Library. This also passed the cheaper-model source review.

Current signed release: http://20.120.96.17/builds/app-release-c41d8f223cff.apk
SHA-256: `c41d8f223cfff56f0abaa590d829f77903857793c4794c1a701498169d5bd666`.
Build/lint passed: `/tmp/android-setup/kisskh-delivery-history-final-release.log`.
Signature/certificate/native bundle/zip alignment and hosted byte-range download verified.
Fresh test notice `test-delivery-1794122711` is signed, live on the mirror and expires after one day.
Device installation/notification receipt remains unverified: ADB disconnected.


## Player-port release verification (2026-11-08)
Signed APK: http://20.120.96.17/builds/app-release-99bae75377e1.apk
SHA-256: `99bae75377e180836f4a3af7329032d26e9c143cbfbc20bb6b389ea18a19bae3` (22,186,942 bytes).
Final release compilation and lint passed: `/tmp/android-setup/reference-player-final-release.log`. Certificate, signature, ZIP alignment, unchanged FFmpeg and hosted byte-range download checks passed. Compiled subtitle smoke passed all five sampled live language files and three legacy decoding formats. Device install/playback/visual/PiP/Cast checks remain pending: `adb devices -l` is empty.
