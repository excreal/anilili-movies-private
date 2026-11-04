# AnimepaheDL codebase review

Date: 2026-11-03. Scope: current working tree, including existing uncommitted changes. This review does not change application code.

This report records the original review snapshot. The subsequent fixes, requested feature implementation, sorting-only correction, and current verification results are tracked in [implementation-progress.md](implementation-progress.md).

## Scope and confidence

Reviewed the Kotlin application, download and export pipelines, persistence, session handling, Compose screens/theme, manifest, build configuration, test inventory, and project documentation. Findings below are static code findings unless explicitly described as a runtime verification item. No live-site download, device UI session, penetration test, or native-binary reverse engineering was performed. Existing screenshots and historical test artifacts are not proof of current behavior. Dependency versions alone are not evidence of a vulnerability; this report makes no CVE claims.

Priority P1 means address before relying on unattended downloads; P2 means meaningful reliability, performance, or security hardening; P3 means usability or maintainability improvement. Confidence describes the code evidence, not whether a device reproduction has been completed.

## Architecture and strengths

The app uses Compose and a large AndroidViewModel for search, selection, settings, recovery, and library state. OkHttp/Jsoup resolve sources; Media3 downloads HLS, while Android DownloadManager handles direct files. Room stores export records and operation intents, mirrored in memory. ExportService remuxes cached HLS with bundled ARM64 FFmpeg and publishes media through MediaStore or a document provider.

Useful existing defenses include cancellable HTTP calls, explicit source/audio intersection, persistent deletion intents, per-item locks, retained sources after export failure, duration checks, domain-scoped cookie retrieval, disabled WebView file/content access, private service components, debug-only diagnostics, and a loopback cache server with a random token and bounded worker queue. These are substantial foundations to preserve.

## Reliability and correctness findings

### R1 — Failed persistence writes leave authoritative memory ahead of disk (P1, high confidence)

Evidence: `DownloadStore.kt`, `put`, `remove`, `operation`, `schedule`, and `flush`.

Every mutation changes the in-memory maps before its database operation completes. Failed writes are captured in one global exception slot; `flush()` clears that exception without retrying or rolling back the failed mutation. A later flush can succeed even though an earlier record or deletion intent was never persisted. Multiple failures are collapsed into the first exception.

Impact: after disk-full or database errors, the UI and service can act on records that disappear or revert after process death. Recovery guarantees depend on durable intent, so this is more serious than a stale UI.

Suggested fix: make critical mutations suspending transactions that return success only after commit, then publish the committed snapshot. Alternatively maintain an ordered durable retry journal and keep the store unhealthy until failed writes are reconciled. Associate errors with individual operations rather than a globally consumed flag.

Validation: inject a failed write, perform another successful write/flush, restart the store, and assert that neither publication nor deletion proceeded on an uncommitted intent.

### R2 — Cold-start direct completion can be missed (P1, high confidence)

Evidence: `AnimeApplication.onCreate` initializes DownloadStore asynchronously; `DownloadCompleteReceiver.onReceive` immediately checks `Exports.all`.

When Android starts a fresh process for DOWNLOAD_COMPLETE, the memory snapshot may still be empty. The receiver then exits without starting export. Opening the app later provides recovery, but unattended saving is lost.

Suggested fix: use `goAsync()` with bounded asynchronous initialization and always finish the pending result, or hand off to a supported persistent job that awaits store readiness and reconciles completed direct downloads. Check the broadcast action and confirm the system download state as part of reconciliation.

Validation: complete a tracked direct download with the app process absent and deliberately delay Room initialization; verify publication without launching the activity.

### R3 — Direct enqueue has a process-death orphan window (P1, high confidence)

Evidence: `DownloadRepository.kt`, `enqueueLocked`: system `enqueue()` precedes `Exports.put`, the direct operation record, and `flush`.

A crash after Android accepts the request but before app metadata is durable leaves an app-owned temporary download with no tracked identity. There is no general startup reconciliation for that case.

Suggested fix: persist a preparation intent with a unique staging filename before enqueue; record the returned system ID durably and reconcile interrupted intents against app-owned DownloadManager destinations on startup. Do not adopt unrelated system downloads.

Validation: terminate at each boundary and verify exactly one visible download and eventual cleanup.

### R4 — HLS validation does not require expected audio (P1, high confidence)

Evidence: `FfmpegRemux.kt` uses optional `-map 0:a:0?`; `HlsExporter.kt`, `validateMp4Tracks`, requires video but only validates audio tracks that exist.

A video-only output can pass even when the source is expected to contain audio, and successful export removes the cached source. Existing checks for shortened video do not establish preservation of all expected tracks.

Suggested fix: derive expected tracks from the chosen source/renditions and require matching output video/audio presence and timelines. Continue supporting intentionally silent video through an explicit source expectation.

Validation: supply a source with audio and an output with full-duration video but no audio; require rejection and source retention.

### R5 — Final destination validation is weaker than staging validation (P1, high confidence)

Evidence: `ExportService.export` checks the staging MP4, but final HLS output only needs positive reported size and one readable byte. Direct publication likewise checks positive copied bytes and a readable first byte. Both MediaStore publication calls ignore the update count.

A faulty provider that truncates accepted data or refuses publication without throwing can be treated as successful, followed by source deletion.

Suggested fix: compare final length to expected copied length where supported; validate the final media descriptor or a suitable integrity measure before source eviction. Treat zero-row MediaStore publication as failure. Account explicitly for providers that cannot report size instead of assuming zero is corruption.

Validation: provider fixtures for short writes, stale size, unknown size, unreadable output, and zero-row publication.

### R6 — Unhandled action failures can crash the app (P2, high confidence)

Evidence: `DownloaderViewModel.retryExport`, `pauseDownload`, and `resumeDownload` launch coroutines without exception handling. `retrySave` calls `DownloadStore.flush`; service starts can also throw.

Suggested fix: centralize action execution with cancellation preservation, typed error handling, per-row busy state, and a retryable explanation. Mirror the handling already present in `retryDownload`.

Validation: inject persistence and service-start failures and verify the activity survives with an actionable row error.

### R7 — Retry can discard the only reference to unfinished output (P2, high confidence)

Evidence: `DownloadRepository.enqueueLocked` copies a previous record with `pendingUri = null`. Export cleanup deliberately retains pendingUri if provider deletion fails.

Transfer retry must not erase ownership of a pending file that still exists. Otherwise cleanup and later deletion cannot find it.

Suggested fix: preserve unresolved output ownership separately from the active attempt; clear it only after confirmed deletion. Model attempts and owned outputs independently.

Validation: fail pending-output deletion, invoke fresh-source retry, then verify that the old output remains tracked and can be removed later.

### R8 — Export service waits throughout transfers (P2, high confidence)

Evidence: enqueue calls `Exports.start`; `ExportService.onStartCommand` starts foreground operation and polls once per second while any pending transfer is active, including paused direct transfers.

Impact: a second foreground service/notification exists while no export work is ready; unnecessary provider queries and background lifetime accumulate.

Suggested fix: start export when a transfer becomes complete, plus startup reconciliation. Stop when no ready export exists; use engine completion callbacks rather than keeping export alive for network transfer.

Validation: a long paused download should not leave a “Waiting to save” service running indefinitely.

### R9 — Wake-lock lifetime does not match export stages (P2, high confidence)

Evidence: `ExportService.export` acquires a 30-minute lock; release occurs only in `onDestroy`, while remux and copy each allow 30 minutes plus validation.

The lock may remain held while the service waits between jobs, or expire before a long export finishes.

Suggested fix: acquire/release in the actual work scope with `finally`, with a bounded policy covering active stages. Test screen-off remux and slow document-provider copy separately.

### R10 — Cancellation deadlines are not hard limits on all provider/native operations (P2, risk requiring device/provider reproduction)

Evidence: validation wraps blocking MediaMetadataRetriever/MediaExtractor work in `withTimeout` and IO context, but the extraction loop does not check cancellation. Document creation/query calls and stream acquisition precede cancellable copy handlers.

Coroutine timeout alone cannot guarantee prompt interruption of these blocking calls. A stalled provider can delay service shutdown or deletion despite the advertised deadline.

Suggested fix: use provider cancellation signals where supported, cancellation-aware iteration and descriptor closure, and bounded cleanup. Describe limits honestly for APIs that cannot be interrupted. Test with a deliberately stalled provider.

## Security and privacy findings

### S1 — Cache server reads unbounded lines before checking their length (P2, high confidence)

Evidence: `FfmpegRemux.kt`, `CachedHlsServer.serve`, uses BufferedReader.readLine then checks 8 KiB/16 KiB limits. Token validation follows header parsing.

A local process able to connect to the loopback listener can send a very long unterminated line. The reader allocates before rejection; continuous traffic also defeats a simple idle read timeout. The random token prevents authorized cache reads but does not protect this parsing stage.

Suggested fix: bounded byte-level request/header parsing, total request deadline, early request-target/token rejection, and connection limits. Keep the loopback binding and token.

Validation: oversized request line, oversized individual header, slow continuously streamed header, bad token, and worker saturation; verify bounded memory and export recovery.

### S2 — Remote response bodies and pagination lack resource bounds (P2, high confidence)

Evidence: `AnimeRepository.request` reads the full body into a String; `details` trusts `last_page`; recursive glot response traversal builds lists.

A malformed or compromised upstream can consume excessive memory or keep the app fetching indefinitely. This is a resource-exhaustion risk, not evidence of device code execution.

Suggested fix: enforce separate response-byte limits for HTML/JSON, total operation deadlines, plausible pagination limits, repeated-page detection, and bounded JSON traversal. Reject invalid responses with a recoverable message.

### S3 — Verification completion accepts unrelated HTTPS pages (P2, high confidence)

Evidence: SessionBrowser permits navigation to any HTTPS origin and closes successfully after two ready checks; readiness checks page content but does not require the intended destination origin.

An ad/redirect can be interpreted as successful verification for the original operation. The visible address helps, but does not bind success to the expected site. There is no JavaScript bridge in the inspected code, so this is not a demonstrated native-code compromise.

Suggested fix: bind automatic completion to the expected origin and an explicit set of legitimate verification transitions; show off-origin navigation clearly and require deliberate continuation when appropriate.

### S4 — Session reset can race background network users (P2, high confidence)

Evidence: `clearSessionData` only guards foreground search/details/source/preparation flags. HLS transfers, direct downloads, artwork requests, and retries may still use cookies.

Cookies may be repopulated by active responses after clearing; Android DownloadManager requests already contain a copied Cookie header. “Session cleared” therefore overstates the guarantee.

Suggested fix: coordinate reset with all network owners, cancel or pause relevant requests, invalidate outstanding cookie writes by session generation, and explain the effect on already queued direct downloads. Make the UI busy guard reflect the same policy.

### S5 — Native artifact verification checks size, not identity (P2, high confidence)

Evidence: `app/build.gradle.kts`, verifyBundledFfmpeg, only checks file length although README records an expected SHA-256.

Suggested fix: enforce the expected checksum in packaging; retain version, source/build provenance, supported ABI, and license materials together. Add a repeatable native update procedure. This finding does not assert the present binary is malicious or vulnerable.

### S6 — Diagnostic redaction is incomplete (P3, high confidence)

Evidence: `DebugTrace.safeUrl` removes query and fragment but preserves path/userinfo; error summaries and exception strings are logged independently.

Signed paths or echoed URLs can still reach debug logs. Suggested fix: structured error fields, origin-only or allowlisted paths, removal of userinfo, and redaction of free text before export/sharing. Add tests with secrets in query, path, and server error bodies.

## UI and usability findings

### U1 — Failed episodes are labeled “Queued” in details (P2)

`DownloaderViewModel.episodeAvailability` reports Saved for a URI and Queued for every other record, without consulting transfer, export, or deletion state. It also takes the first matching episode regardless of quality/audio.

Use the canonical DownloadRow state and identify variants. Validate failed, paused, verifying, saving, deleting, and multiple-variant cases.

### U2 — Search results can belong to an earlier query (P2)

`startSearch` leaves results/searched unchanged until success. If the next query fails, old results remain under the new input, with an error but no indication of which query produced them.

Track a successful-query label, or clear results on query transition. Distinguish loading previous results from results for the submitted query. Test success A followed by failed B.

### U3 — Documented year/type filters are absent (P3)

README describes year/type filtering; SearchScreen passes literal All years/All types to filterResults and only renders Sort. ViewModel still has unused year/type state.

Restore accessible filter controls or remove the obsolete state, tests that imply UI availability, and documentation claim. The current code does not expose the advertised feature.

### U4 — Library grouping parses presentation text (P2)

LibraryScreens groups by `title.substringBefore(" · ")` and parses episode numbers from split strings. Titles containing the delimiter and distinct shows sharing a display title can merge or mislabel rows.

Add show ID, show title, episode number, and variant fields to DownloadRow; use stable show identity for grouping and numeric episode metadata for sorting. Keep title parsing only in legacy migration.

### U5 — Settings failures use success-like styling (P3)

SettingsScreen displays all settingsMessage values with the primary color, including “Cannot use folder”. Use typed success/error state, appropriate semantics, and a recovery action. Test provider permission denial.

### U6 — Accessibility/localization coverage needs expansion (P3, verification gap)

Most screen text is inline English rather than resources/plurals. Episode cards have a decorative checkbox but click semantics rather than explicit selection semantics at the row level. Downloads group checkboxes do not express partial selection as indeterminate.

Extract resources/plurals, make episode rows selectable with checked state, use tri-state group selection, and test TalkBack focus order. Verify 200% font scale, narrow split-screen, landscape, RTL, light/dark contrast, long titles, and dialogs; no current-device visual pass was performed here.

## Performance and maintainability

### P1 — Full-library reconstruction on frequent progress events (P2)

OfflineDownloads emits at roughly 250 ms intervals plus transfer ends. Every refresh rebuilds rows from the index, queries direct downloads individually, copies export maps, and scans other state. Export copy progress can emit for every 256 KiB. `refreshAgain` coalesces overlap but does not impose a maximum refresh rate. Downloads composition groups/sorts the collection again.

Throttle rendered progress independently from terminal events; retain a stable metadata map and update only affected progress rows; batch direct-ID queries and cache grouping. Benchmark a large library while transfers run, measuring frame time, allocations, query counts, and battery use before choosing optimizations.

### P2 — Details availability repeatedly copies/scans records (P2)

Every composed episode calls episodeAvailability, which copies Exports.all then scans it. Build an episode-to-state index when download state changes. Validate with a long-running show and a large saved library.

### P3 — Batch source selection scales poorly (P2)

Details fetches all episode pages sequentially before showing usable data. Source checking creates one coroutine per selected episode, although Semaphore(3) limits network concurrency. Preparation then resolves sources again, including episodes already tracked.

Load episodes incrementally, use bounded worker queues, and skip known identities before expensive playlist resolution where quality/audio identity is already known. Cache only short-lived source metadata; do not assume signed media URLs remain valid. Preserve the shared-options guarantee.

### P4 — Parallel exporter implementations obscure production behavior (P3)

Production ExportService calls ffmpegRemuxCachedHls, while HlsExporter and DirectHlsRemuxer retain a separate Transformer/muxer path. The latter has distinct assumptions, including a relative-URI resolver that appends a segment to the playlist path rather than resolving against its directory.

Document and test the supported production path; remove unused implementations or clearly isolate experimental/test code. Do not count tests of an alternate exporter as production FFmpeg coverage. If retaining DirectHlsRemuxer, use standards-compliant URI resolution and bounded segment buffers.

### P5 — Persistence schema and orchestration are difficult to evolve (P3)

Room stores JSON metadata with schema export disabled; multiple singleton maps/services and a 668-line ViewModel distribute state transitions. Add exported Room schemas, explicit migration fixtures, typed export states and owned-output records, and interfaces for storage/engines. Split search, library, and settings state only after stabilizing transition behavior.

## Recommended features grounded in existing functionality

These are product proposals, not claims about current capabilities or third-party product recommendations.

1. **Storage preflight and recovery center.** Build on temporaryBytes, destination settings, and retry-save: show required staging/final space, destination accessibility, retained failed caches, and actionable cleanup. Never delete resumable media automatically without an explicit retention policy.
2. **Network policy controls.** Add Wi-Fi/unmetered-only and roaming preferences consistently across Media3 and DownloadManager. Show why a job is waiting. Current connection/parallelism settings control throughput, not data-cost policy.
3. **Queue management.** Add priority/reordering, pause eligible items, retry failed transfers, and retry failed saves as distinct batch actions with eligibility counts. Keep engine-specific limitations visible.
4. **Reliable library repair.** Detect missing/inaccessible files separately; offer relocate/relink and metadata-only removal. Existing refresh removes confirmed missing rows but cannot help relink moved media.
5. **Select missing episodes.** Build on range selection and episodeAvailability; allow selecting episodes without a saved/active matching variant. Define how failed jobs and alternate audio/quality count.
6. **Bookmarks and recent searches.** Persist lightweight show identities and search history, with explicit clearing controls. This reduces repeated navigation without introducing automatic scraping.
7. **Local playback continuity.** Save playback position and offer next episode using numeric metadata. Decide whether to provide an internal player for saved files or continue external playback with its limitations clearly explained.
8. **Actionable notifications.** Identify the show/episode being saved, provide eligible pause/resume actions, and distinguish transfer completion from final-file availability.
9. **Privacy-safe diagnostic bundle.** Include app/build/device versions, engine states, structured failure codes, and redacted stage timings. Preview contents before sharing; omit cookies and signed URLs.
10. **Source health feedback.** Distinguish site challenge, resolver failure, expired media, transport failure, and folder failure. Include last successful verification time without promising that browser verification guarantees HTTP access.

## Suggested implementation order and acceptance gates

First establish durable state transitions and cold-start reconciliation (R1–R3), then strengthen output validation and ownership cleanup (R4–R7). Next fix service/wake-lock lifetime, bounded parsing, reset coordination, and per-action error handling. Follow with canonical UI metadata/state and progress performance work. Ship optional features after these foundations.

Release gates should include process death at enqueue/publication boundaries; disk-full database writes; revoked, slow, unknown-size, and refusing document providers; direct and HLS completion while closed; missing expected audio; long-duration screen-off export; malformed cache-server requests; large-library scrolling during transfers; and accessible navigation at large font scale. Run optimized release behavior as well as debug fixtures.

## Verification record

Attempted `bash ./gradlew testDebugUnitTest lintDebug --console=plain`. Execution details are recorded separately below after the process completes. Device tests and live-site compatibility were not run. The review does not establish absence of other defects, exploitable dependency vulnerabilities, or correctness of the bundled native executable.

Verification outcome: all 34 local unit tests passed (11 suites, zero failures/errors/skips). Lint reached analysis but did not produce a final report during the review; the review's Gradle invocation was stopped, so lint is **incomplete**, not passing. See `codebase-review-checks-2026-11-03.log`. `git diff --check` reported no whitespace errors. No application fixes were made, and pre-existing working-tree changes were preserved.
