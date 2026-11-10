# Graph Report - AnililiMovies  (2026-11-10)

## Corpus Check
- 308 files · ~277,457 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 40 file(s) not represented in the graph (top: .xml 22, .properties 4, (none) 3)

## Summary
- 4711 nodes · 11382 edges · 239 communities (157 shown, 82 thin omitted)
- Extraction: 94% EXTRACTED · 6% INFERRED · 0% AMBIGUOUS · INFERRED: 669 edges (avg confidence: 0.86)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `8e42e9af`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- AnililiError
- LibraryScreens.kt
- AniListModels.kt
- ExternalPlayer.kt
- InPlayerEpisodeList.kt
- PlayerGestureControls.kt
- PlayerSurface.kt
- SettingsStore
- PlayerControlsScaffold
- NostrUpdateSource.kt
- CaptionStyle
- PlayerSurface
- MidnightDialog.kt
- PlayerGestureControls.kt
- AppBackdrop.kt
- TvNativeTextField
- .all
- AppDeviceProfile.kt
- MediaCache.kt
- CastRoutes.kt
- ui/components/AnililiSilhouette.kt
- SiteDownloadSources.kt
- PlayerSettingsSheet.kt
- .operation
- .manager
- ErrorNotice.kt
- PlaybackBuffer.kt
- SettingsStore.kt
- PlaybackService.kt
- LibraryNavigationTest
- SearchScreenTest.kt
- AppSymbol
- KisskhResilienceTest.kt
- AppDeviceProfile.kt
- KisskhCatalog.kt
- SubtitleTrackMerge.kt
- Composable
- DownloaderViewModel.kt
- developer_notices.py
- LibraryModels.kt
- SubtitleCueLayout.kt
- SourceFilterLanguage
- ExternalSubtitleFiles.kt
- SleepTimer.kt
- SiteDownloadSeries
- BoundedUrlTokenRegistry
- .serve
- DownloadStore
- ExistingOutput.kt
- PreferredAudioLanguage
- siteDownloadCookieHeader
- SessionBrowser.kt
- SiteMetadataApi.kt
- ExportService.kt
- Category
- AudioOutputFormat.kt
- TvFocusTarget.kt
- AnimepaheDL codebase review
- .verifyExport
- SubtitleItem
- Listener
- LocalStreamProxy.kt
- CastButton
- KisskhResilienceTest
- ArtworkBitmapLoader.kt
- SubtitleDelay.kt
- AnimepaheDL resume state
- FloatingNavigationBar.kt
- ProgressiveExportTest.kt
- MegaPlayCdnHealth.kt
- EroticMvMetadataApi.kt
- Anilili UI reference audit
- DeletionTestProvider.java
- PlaybackDiagnostics.kt
- FocusFill.kt
- MidnightHelper
- AppIcon
- AppDns
- PreferredSubtitleLanguage
- ErrorReporting.kt
- OpenSubtitlesPanel.kt
- AppColorScheme.kt
- CatalogViewModel.kt
- SiteMetadataApi
- SiteDownloadEpisode
- ErrorNotice.kt
- AnimeRepository.kt
- instrumentationregistry
- SubtitleSearchLanguage
- SiteDownloadSources.kt
- LibraryTools.kt
- ResolvedMedia
- ThreadTraceListener.java
- SearchScreenTest.kt
- LocalStreamProxy
- LocalStreamProxyService.kt
- RemoteDownloadSheet
- TvFocusTargetNode
- .page
- CatalogViewModel.kt
- TimeOfDay.kt
- AnnouncementSource.kt
- .switchPlaybackPlayer
- FlixcloudPlaylistDataSource.kt
- Test
- metadataFromDocument
- GlassSection
- ThemeAccent
- Test
- Listener
- CaptionPosition
- .check
- Callback
- SourceOptions.kt
- ErrorDomain
- NativeTvTextWatcher
- decodeRelayFrame
- WebSocketListener
- AppGraph
- Test
- AnnouncementInputBoundsTest
- SecureTokenStore
- Security and privacy findings
- ThemePreset
- LocalStreamProxyHeaderPolicyTest
- ForwardingPlayer
- SiteDownloadViewModel.kt
- SkipTimingStatus
- TvPlayerControl
- CatalogScreen
- downloadLocation
- DownloadNotifications.kt
- UI and usability findings
- RelatedLayout
- .configureRequestHeaders
- .onCreate
- c
- DpadLongPress.kt
- DownloadRow
- ReferencePlaybackRegistry
- ForegroundCheck
- LegacyTrustAnchors.kt
- KissKH metadata API
- TvTextInputType
- EpisodeDownloads
- .write
- MidnightSettingSwitchRow
- PlayerSystemBars.kt
- AnnouncementManager.kt
- CatalogItem
- AnimeRepository.kt
- .row
- Real-device verification
- size
- ErrorAction
- HlsPlaylistRewrite.kt
- NextEpisodeTrigger
- CatalogPosterCard
- URI
- MidnightNoticeTone
- CategoryAudioDecision
- Reliability and correctness findings
- Browse, play and download
- .init
- TvPlayerBackTarget
- SavedFilesWatcher.kt
- FailureReason
- selectCachedHlsVariant
- cached
- ProviderBenchmark
- ClockFormat
- DownloadQuality
- PlayerAspectRatio
- TransferListener
- UI parity and usability checklist
- CatalogHero
- TransferState
- isExportDurationComplete
- OrderedWrites
- SiteDownloadRepositoryTest.kt
- Anilili feature audit checklist
- .focusSearch
- .transform
- backdropBlur
- ReferenceChromeTokens.kt
- DownloadAction
- Review implementation status
- PosterElement
- UiState.kt
- normalizeOutputFolder
- X509TrustManager
- .legacyIdentity
- UiScale
- .event
- DesignSystem.kt
- Dramatip metadata API
- ExampleUnitTest.kt
- EroticMV metadata API
- TvEpisodeChangeFocus
- Device UI verification
- Developer announcements
- AGENTS.md
- components/README.md
- docs/KISSKH.md
- THIRD_PARTY_NOTICES.md
- make-export-benchmark.sh
- verificationPage
- ExampleInstrumentedTest.kt
- GestureDragAxis

## God Nodes (most connected - your core abstractions)
1. `SettingsStore` - 157 edges
2. `PlayerSurface()` - 135 edges
3. `AnililiError` - 114 edges
4. `PlaybackService` - 45 edges
5. `rememberFocusFill()` - 44 edges
6. `SiteDownloadSeries` - 34 edges
7. `SourceFilterLanguage` - 33 edges
8. `DiagnosticsLog` - 30 edges
9. `LocalStreamProxy` - 30 edges
10. `ExportRecord` - 28 edges

## Surprising Connections (you probably didn't know these)
- `API methods and routing` --references--> `MetadataPage`  [INFERRED]
  docs/api/NARTO.md → app/src/main/java/com/example/animepahedl/metadata/SiteMetadataApi.kt
- `Download playback-state correction (current source)` --references--> `WatchProgressBar()`  [INFERRED]
  docs/UI_REFERENCE_CHECKLIST.md → app/src/main/java/com/anilili/ui/components/Common.kt
- `Browse and filtering` --references--> `MetadataHttp`  [INFERRED]
  docs/api/KISS18.md → app/src/main/java/com/example/animepahedl/metadata/SiteMetadataApi.kt
- `Verification still needed` --references--> `DownloadGroupArtworkTest`  [INFERRED]
  UI_PARITY_CHECKLIST.md → app/src/test/java/com/example/animepahedl/DownloadGroupArtworkTest.kt
- `Necessary host adaptations` --references--> `PlaybackService`  [INFERRED]
  docs/REFERENCE_PLAYER_PORT.md → app/src/main/java/com/anilili/playback/PlaybackService.kt

## Import Cycles
- None detected.

## Communities (239 total, 82 thin omitted)

### Community 0 - "AnililiError"
Cohesion: 0.02
Nodes (98): AnililiError, ACCOUNT_LIBRARY_LOAD_FAILED, ACCOUNT_LIST_ENTRY_DELETE_FAILED, ACCOUNT_LIST_UPDATE_FAILED, ACCOUNT_LOGIN_ABANDONED, ACCOUNT_LOGIN_PAGE_FAILED, ACCOUNT_PROFILE_LOAD_FAILED, ACCOUNT_PROGRESS_UPDATE_FAILED (+90 more)

### Community 1 - "LibraryScreens.kt"
Cohesion: 0.08
Nodes (6): ReferenceSettingsIndex(), SettingsSectionCard(), SettingsSectionTile, onPause(), onResume(), openFromIntent()

### Community 2 - "AniListModels.kt"
Cohesion: 0.06
Nodes (78): AiringCountEntry, AiringCountMedia, AiringCountPage, AiringDayCount, AiringSchedule, AnimeStat, AppNotification, CoverImage (+70 more)

### Community 3 - "ExternalPlayer.kt"
Cohesion: 0.09
Nodes (19): LibraryStore, ExternalPlaybackOutcome, externalPlaybackOutcome(), ExternalPlaybackReturns, ExternalPlaybackSession, ExternalPlaybackRequest, externalPlaybackRequest(), externalPlayerHeaderPairs() (+11 more)

### Community 4 - "InPlayerEpisodeList.kt"
Cohesion: 0.05
Nodes (42): EpisodeItem, EpisodeLayout, GRID, LIST, EpisodeOrder, NEWEST_FIRST, OLDEST_FIRST, focusFill() (+34 more)

### Community 5 - "PlayerGestureControls.kt"
Cohesion: 0.04
Nodes (7): BubbleSpec, volumeIcon(), PlayerLockButton(), PlayerLockOverlay(), playerTypography(), rememberTimedSkipPopup(), BubbleSpec

### Community 6 - "PlayerSurface.kt"
Cohesion: 0.04
Nodes (31): playbackFailureEntry(), httpFailureSummary(), shouldRetryDecoderAtLowerVideoResolution(), adaptiveVideoHeightCeiling(), applyAudioTrack(), applyCategoryAudioPreference(), applyPreferredAudioLanguage(), applyPreferredSubtitleLanguage() (+23 more)

### Community 8 - "PlayerControlsScaffold"
Cohesion: 0.06
Nodes (36): bufferedFraction(), BufferedSeekTrack(), controlsSeekAnchorFor(), PlayerActionPill(), PlayerActionRow(), playerActionRowScale(), PlayerBackArrow(), PlayerChromeBox() (+28 more)

### Community 9 - "NostrUpdateSource.kt"
Cohesion: 0.16
Nodes (7): hexToBytes(), NostrEvent, NostrUpdateSource, RelayFrameGate, RelayScanResult, toHexString(), toNostrEvent()

### Community 10 - "CaptionStyle"
Cohesion: 0.05
Nodes (31): CaptionBackgroundColor, BLACK, GRAY, NAVY, WHITE, CaptionEdgeStyle, DROP_SHADOW, NONE (+23 more)

### Community 11 - "PlayerSurface"
Cohesion: 0.04
Nodes (39): PlayerContentScale, CROP, FILL, FIT, rememberScreenReaderActive(), CompiledSeparately(), Routes, AutoSkipCountdown (+31 more)

### Community 12 - "MidnightDialog.kt"
Cohesion: 0.05
Nodes (4): EqualizerWaveIndicator(), MidnightSettingSpotlight, PlayerAccentScope(), PageSearchField()

### Community 13 - "PlayerGestureControls.kt"
Cohesion: 0.07
Nodes (35): formatPlayerTime(), applyBrightness(), applyVolume(), Brightness, canSlideSeek(), canStartHoldSpeed(), GestureIndicatorSide, gestureIndicatorSide() (+27 more)

### Community 14 - "AppBackdrop.kt"
Cohesion: 0.06
Nodes (17): AppUiScale(), UnscaledUi(), SeriesLogo(), blurLine(), boxBlur(), PopupBackdrop, popupGlass(), AppBackdrop (+9 more)

### Community 15 - "TvNativeTextField"
Cohesion: 0.20
Nodes (5): beginNativeTvEditing(), hideNativeTvKeyboard(), isImeVisible(), SafeTvEditText, TvNativeTextField()

### Community 16 - ".all"
Cohesion: 0.12
Nodes (8): DeletionRecoveryTest, consumeCancellable(), AndroidDownloadRepository, adoptExistingVideo(), clearPendingVideo(), reconcileExistingVideos(), Exports, R2 — Cold-start direct completion can be missed (P1, high confidence)

### Community 17 - "AppDeviceProfile.kt"
Cohesion: 0.07
Nodes (32): isTelevision(), readTelevisionSignals(), TelevisionSignal, televisionSignal(), CHOSEN, LEANBACK_FEATURE, LEANBACK_LAUNCHER, NO_TOUCHSCREEN (+24 more)

### Community 18 - "MediaCache.kt"
Cohesion: 0.08
Nodes (12): VideoCacheSize, AUTOMATIC, GB_1, GB_2, MB_256, MB_512, OFF, ExoDatabase (+4 more)

### Community 19 - "CastRoutes.kt"
Cohesion: 0.06
Nodes (7): CastRoute, CastRoutes, GoogleCastRoute, SessionAvailabilityListener, mediaClientOrNull(), shouldGiveUpOnCastLoad(), MiruroCastMediaItemConverter

### Community 20 - "ui/components/AnililiSilhouette.kt"
Cohesion: 0.08
Nodes (19): silhouetteFrame(), silhouetteMask(), cappedFrameSteps(), DetailedWaterFillLogo(), liquidLevel(), LoopHandover, PlainWaterFillLogo(), rememberLiquidClock() (+11 more)

### Community 21 - "SiteDownloadSources.kt"
Cohesion: 0.15
Nodes (18): Page, SiteDownloadRepository, cleanProviderTitle(), decodeEroticMedia(), hostMatchesDomain(), isHlsMedia(), isSupportedVideoFile(), parseProviderTitleAndPoster() (+10 more)

### Community 22 - "PlayerSettingsSheet.kt"
Cohesion: 0.10
Nodes (23): MediaVolumeSlider(), PlayerPanelTheme(), CaptionSections(), formatSubtitleDelay(), midnightSliderColors(), PanelHeader(), PlayerAdjustButton(), PlayerAdjustRow() (+15 more)

### Community 23 - ".operation"
Cohesion: 0.19
Nodes (3): reconcileDirectEnqueues(), PreparationJobs, PreparationWorker

### Community 24 - ".manager"
Cohesion: 0.10
Nodes (5): DownloadRetryService, MediaVerification, OfflineDownloads, Listener, OfflineDownloadService

### Community 25 - "ErrorNotice.kt"
Cohesion: 0.07
Nodes (9): Midnight, NextEpisodeAction(), NextEpisodeCard(), playerClockAnchor(), PlayerClockReadout(), playerClockText(), rememberClockMinute(), PlayerPauseOverlay() (+1 more)

### Community 26 - "PlaybackBuffer.kt"
Cohesion: 0.08
Nodes (15): PlaybackBufferSize, AUTOMATIC, LARGE, MEDIUM, SMALL, VERY_LARGE, causeChain(), platformErrorCode() (+7 more)

### Community 27 - "SettingsStore.kt"
Cohesion: 0.04
Nodes (24): coercePlaybackSpeed(), coercePlayerBrightness(), decodeHiddenPosterElements(), decodePersistentCaptionDelays(), decodeSourceFilterLanguages(), DefaultQuality, AUTO, HIGHEST (+16 more)

### Community 28 - "PlaybackService.kt"
Cohesion: 0.08
Nodes (3): CastRouteStatus, PlaybackCacheBypassGate, PlaybackService

### Community 29 - "LibraryNavigationTest"
Cohesion: 0.15
Nodes (6): DownloadSelectionTest, LibraryNavigationTest, AppSettings, Verification plan, Device verification, FFmpeg

### Community 30 - "SearchScreenTest.kt"
Cohesion: 0.08
Nodes (25): 10. Collection summary gives count text precedence over its heading — P2, layout risk, 11. Text-heavy dialogs lack explicit scrolling — P2, layout risk, 12. Image-error copy does not adapt to thumbnail size — P3, layout risk, 13. Verification banner's View action can leave the user on Settings/About — P2, confirmed, 14. Search, empty states, and error treatments vary without shared components — P2, confirmed design divergence, 15. Completion and destructive-action terminology is inconsistent — P2, confirmed, 16. Spacing, chevrons, and text hierarchy are locally defined — P3, confirmed design divergence, 1. Explore search actions consume the input's usable width — P1, layout risk (+17 more)

### Community 31 - "AppSymbol"
Cohesion: 0.10
Nodes (18): AppSymbol(), DestinationSummary(), DownloadReferenceEpisode, DownloadReferenceEpisodeRow(), DownloadReferenceSeries, DownloadReferenceSeriesCard(), ReferenceCornerBadge(), WatchProgressBar() (+10 more)

### Community 32 - "KisskhResilienceTest.kt"
Cohesion: 0.10
Nodes (4): artworkRecoveryHostPolicy(), isAllowedArtworkRecoveryUrl(), ArtworkRecoveryPolicyTest, X509TrustManager

### Community 33 - "AppDeviceProfile.kt"
Cohesion: 0.18
Nodes (3): focusHalo(), focusHighlight(), phoneRailPosterWidth()

### Community 34 - "KisskhCatalog.kt"
Cohesion: 0.31
Nodes (11): detail(), json(), JsonResult, list(), localizeSubtitles(), playbackSubtitles(), Redirect, series() (+3 more)

### Community 35 - "SubtitleTrackMerge.kt"
Cohesion: 0.14
Nodes (30): assDialogueLine(), AssDocument, assPlayRes(), assRecord(), assStyleLine(), assTime(), decodeEntities(), formatFields() (+22 more)

### Community 36 - "Composable"
Cohesion: 0.14
Nodes (24): CoverScreenWithDialogWindow(), MidnightBodyTopStop(), MidnightConfirmDialog(), MidnightConnectionStatus(), MidnightDialogFooter(), MidnightDialogFrame(), MidnightDialogHost(), MidnightFilledAction() (+16 more)

### Community 37 - "DownloaderViewModel.kt"
Cohesion: 0.05
Nodes (7): AnimeApplication, copyCancellable(), normalizeCachedByteRanges(), details(), Index, kObjects(), kString()

### Community 38 - "developer_notices.py"
Cohesion: 0.08
Nodes (23): isJwtExpired(), jwtLongClaim(), normalizeSubtitleLanguages(), OpenSubtitleDownload, OpenSubtitlesClient, OpenSubtitleSearchResult, parseSearchResults(), broadcast() (+15 more)

### Community 39 - "LibraryModels.kt"
Cohesion: 0.13
Nodes (25): capResumePoints(), DismissedRemoteRow, episodeKey(), EpisodeResume, EpisodeStop, historyAfterEpisodeToggled(), historyAfterEpisodeWatched(), historyAfterRemoteProgress() (+17 more)

### Community 40 - "SubtitleCueLayout.kt"
Cohesion: 0.17
Nodes (15): autoStackedRow(), box(), CueBox, CueLayoutTextOutput, dedupeSimultaneousCues(), dropVectorDrawings(), flattenAutoStackedCues(), isAutoStack() (+7 more)

### Community 41 - "SourceFilterLanguage"
Cohesion: 0.06
Nodes (29): encodeSourceFilterLanguages(), SourceFilterLanguage, ARABIC, BENGALI, CHINESE, DUTCH, ENGLISH, FRENCH (+21 more)

### Community 42 - "ExternalSubtitleFiles.kt"
Cohesion: 0.18
Nodes (15): ExportedSubtitle, AttachedSubtitle, bomCharset(), decodeSubtitleBytes(), displayName(), downloadOpenSubtitle(), exportedSubtitleMimeType(), extensionFor() (+7 more)

### Community 43 - "SleepTimer.kt"
Cohesion: 0.07
Nodes (13): NetworkIdentity, Counting, EndOfEpisode, formatSleepRemaining(), Off, sleepFadeLevel(), SleepTimer, State (+5 more)

### Community 44 - "SiteDownloadSeries"
Cohesion: 0.09
Nodes (8): OnlineDramaHistoryTest, ContinueWatchingRow(), LibraryScreen(), PlaybackHistory, SavedShowsStore, rememberSeriesPlaybackSummary(), SeriesPlaybackSummary, SiteDownloadSeries

### Community 45 - "BoundedUrlTokenRegistry"
Cohesion: 0.13
Nodes (5): BoundedUrlTokenRegistry, ProxyActiveResources, stopProxyResources(), TrackedProxyResources, UrlRegistryCapacityException

### Community 46 - ".serve"
Cohesion: 0.27
Nodes (4): InputStream, copyExactly(), parseUnknownTotalRange(), readBytesBounded()

### Community 47 - "DownloadStore"
Cohesion: 0.14
Nodes (5): DownloadDao, DownloadDatabase, DownloadStore, PendingOperation, StoredDownload

### Community 48 - "ExistingOutput.kt"
Cohesion: 0.14
Nodes (11): existingOutput(), ExistingOutputIndex, ExistingVideo, existingVideoName(), findExistingOutput(), hasOutputFolderAccess(), isPlausibleExistingVideo(), OutputFolderAccessRequired (+3 more)

### Community 49 - "PreferredAudioLanguage"
Cohesion: 0.10
Nodes (18): PreferredAudioLanguage, ARABIC, AUTOMATIC, BENGALI, CHINESE, ENGLISH, FRENCH, GERMAN (+10 more)

### Community 50 - "siteDownloadCookieHeader"
Cohesion: 0.15
Nodes (7): NativeHeaderContext, normalizeSiteDownloadCookieFile(), siteDownloadCookieHeader(), validCookiePair(), validSiteDownloadCookieInput(), NativeRequestHeaderPolicyTest, SiteDownloadCookieTest

### Community 52 - "SiteMetadataApi.kt"
Cohesion: 0.24
Nodes (21): browse(), metadataFromDocument(), metadataFromHtml(), MetadataBrowseQuery, MetadataDetails, MetadataEpisode, metadataEpisode(), MetadataImage (+13 more)

### Community 53 - "ExportService.kt"
Cohesion: 0.09
Nodes (9): ExistingOutputTest, IntegrityTest, AnnouncementDialog(), downloadsPendingIntent(), DownloadCompleteReceiver, ExportProgress, onBind(), onCreate() (+1 more)

### Community 54 - "Category"
Cohesion: 0.10
Nodes (17): Category, DUB, SUB, EpisodesResult, hasUsableWindow(), ProviderData, SkipTimes, SourcesResult (+9 more)

### Community 55 - "AudioOutputFormat.kt"
Cohesion: 0.13
Nodes (7): AudioOutputFormat, AudioOutputMonitor, AudioRoute, audioRoute(), DECODED, PASSTHROUGH, Listener

### Community 56 - "TvFocusTarget.kt"
Cohesion: 0.23
Nodes (5): rememberTvFocusTarget(), tvFocusHandoff(), TvFocusTarget, tvFocusTarget(), TvFocusTargetElement

### Community 57 - "AnimepaheDL codebase review"
Cohesion: 0.17
Nodes (12): AnimepaheDL codebase review, Architecture and strengths, P1 — Full-library reconstruction on frequent progress events (P2), P2 — Details availability repeatedly copies/scans records (P2), P3 — Batch source selection scales poorly (P2), P4 — Parallel exporter implementations obscure production behavior (P3), P5 — Persistence schema and orchestration are difficult to evolve (P3), Performance and maintainability (+4 more)

### Community 58 - ".verifyExport"
Cohesion: 0.11
Nodes (7): CacheServerTest, HlsExportTest, TimerTask, TimerTask, CachedHlsServer, ffmpegRemuxCachedHls(), requireCachedHlsSegments()

### Community 59 - "SubtitleItem"
Cohesion: 0.15
Nodes (19): SubtitleItem, loadSubtitleCueTimes(), loadSubtitleText(), nearestSubtitleCues(), parseArrowCueTimes(), parseAssCueTimes(), parseSubtitleCueTimes(), parseTimestampMs() (+11 more)

### Community 60 - "Listener"
Cohesion: 0.14
Nodes (5): contentAspectRatio(), diagnosticSummary(), Listener, preferredAudioTrackCodes(), stateName()

### Community 61 - "LocalStreamProxy.kt"
Cohesion: 0.14
Nodes (10): BodyHead, dispatchAcceptedSocket(), namedDaemonThreadFactory(), newBoundedProxyRequestExecutor(), ProxyRequest, readBody(), readHead(), readLine() (+2 more)

### Community 62 - "CastButton"
Cohesion: 0.11
Nodes (7): applyCastTextTrack(), CastButton(), Callback, openSystemCastPicker(), ThemedMediaRouteChooserDialogFragment, ThemedMediaRouteControllerDialogFragment, ThemedMediaRouteDialogFactory

### Community 63 - "KisskhResilienceTest"
Cohesion: 0.10
Nodes (6): fetchProviderSubtitleBytes(), MediaSubtitle, normalizeKisskhSubtitles(), providerSubtitleRequest(), ProviderSubtitles, KisskhResilienceTest

### Community 65 - "SubtitleDelay.kt"
Cohesion: 0.15
Nodes (5): SubtitleDelay, SubtitleDelayRenderer, SubtitleDelayRenderersFactory, SubtitleDelaySeed, subtitleDelaySeed()

### Community 66 - "AnimepaheDL resume state"
Cohesion: 0.08
Nodes (23): ADB device test + bookmark feature, AnimepaheDL resume state, Changes in the latest task, Current follow-up release, Custom downloader only, Dedicated details and download-range dialog, Environment and hosting, Existing work and reference (+15 more)

### Community 67 - "FloatingNavigationBar.kt"
Cohesion: 0.11
Nodes (9): captionOffset(), DownloaderNavigation(), barBlur(), barGlass(), barRim(), FloatingNavDisc(), FloatingNavIcon(), FloatingNavigationBar() (+1 more)

### Community 69 - "MegaPlayCdnHealth.kt"
Cohesion: 0.18
Nodes (6): healthHost(), isMegaPlayMedia(), MegaPlayCdnHealth, Callback, MegaPlayCdnHealthInterceptor, MegaPlayHealthState

### Community 70 - "EroticMvMetadataApi.kt"
Cohesion: 0.18
Nodes (19): absoluteErotic(), cleanEroticDescription(), eroticCard(), eroticDurationSeconds(), eroticImages(), eroticImdb(), eroticRatings(), eroticSiteUrl() (+11 more)

### Community 71 - "Anilili UI reference audit"
Cohesion: 0.09
Nodes (20): File inventory, Original Anilili player port, Player-port release verification (2026-11-08), Review checklist, Anilili UI reference audit, Automated source comparison, Build/package verification, Complete active shared-component inventory (+12 more)

### Community 75 - "MidnightHelper"
Cohesion: 0.20
Nodes (13): MidnightChoiceRow(), midnightFocus(), MidnightHelper(), midnightInitialFocus(), MidnightRowScaffold(), MidnightSectionLabel(), midnightSelectable(), MidnightSelectionRow() (+5 more)

### Community 76 - "AppIcon"
Cohesion: 0.10
Nodes (21): AppIcon, Back, Bookmark, BookmarkFilled, Browse, Check, CheckCircle, Close (+13 more)

### Community 77 - "AppDns"
Cohesion: 0.06
Nodes (13): AppDns, DohProvider, ADGUARD, AUTO, CANADIAN_SHIELD, CLOUDFLARE, CLOUDFLARE_SECURITY, DNS_SB (+5 more)

### Community 78 - "PreferredSubtitleLanguage"
Cohesion: 0.10
Nodes (17): PreferredSubtitleLanguage, ARABIC, AUTOMATIC, CHINESE, ENGLISH, FRENCH, GERMAN, HINDI (+9 more)

### Community 79 - "ErrorReporting.kt"
Cohesion: 0.13
Nodes (10): AnililiException, AnililiFailure, anililiFailure(), asFailure(), classifyTransport(), isCancellation(), raise(), report() (+2 more)

### Community 80 - "OpenSubtitlesPanel.kt"
Cohesion: 0.23
Nodes (4): defaultOpenSubtitlesLanguages(), openSubtitlesEpisodeField(), OpenSubtitlesPanel(), SubtitleResultRow()

### Community 81 - "AppColorScheme.kt"
Cohesion: 0.11
Nodes (6): AppColors, appColors(), swatch(), ThemeSpec, wallpaperSeed(), AnimepaheDLTheme()

### Community 82 - "CatalogViewModel.kt"
Cohesion: 0.29
Nodes (6): catalogPageForSource(), CatalogSource, EROTICMV, KISSKH, CatalogSourceOrder, CatalogSourcePreferences

### Community 83 - "SiteMetadataApi"
Cohesion: 0.11
Nodes (13): MetadataSite, EROTICMV, KISSKH, SiteMetadataApi, SiteMetadataApis, API methods and routing, Live audit evidence, Narto Drama public metadata API (+5 more)

### Community 84 - "SiteDownloadEpisode"
Cohesion: 0.22
Nodes (8): batches(), chronologicalSiteEpisodes(), SiteDownloadBatch, SiteDownloadPreparation, downloadEpisodeDisplayOrder(), parseSiteDownloadSelection(), SiteDownloadEpisode, SiteDownloadSelectionSafetyTest

### Community 85 - "ErrorNotice.kt"
Cohesion: 0.31
Nodes (5): DetailLine(), ErrorCodeChip(), ErrorDetailsDialog(), failureAnnotated(), MidnightErrorNotice()

### Community 86 - "AnimeRepository.kt"
Cohesion: 0.17
Nodes (11): AnimeRepository, Anime, AnimeDetails, AnimeRepository, Episode, HttpFailure, isMediaPath(), text() (+3 more)

### Community 87 - "instrumentationregistry"
Cohesion: 0.18
Nodes (4): PreparationResponsivenessTest, SearchRecoveryTest, AnimeRepository, AnimeRepository

### Community 88 - "SubtitleSearchLanguage"
Cohesion: 0.11
Nodes (17): SubtitleSearchLanguage, ARABIC, CHINESE, ENGLISH, FOLLOW_DEVICE, FRENCH, GERMAN, HINDI (+9 more)

### Community 89 - "SiteDownloadSources.kt"
Cohesion: 0.10
Nodes (20): toAppMetadata(), chooseMediaVariant(), eroticVideoSource(), jsonAfterMarker(), jsonArrayOrEmpty(), jsonInt(), jsonObjectOrEmpty(), jsonString() (+12 more)

### Community 90 - "LibraryTools.kt"
Cohesion: 0.20
Nodes (7): classifyFailure(), diagnostics(), inspectStorage(), QueueOrder, StorageReport, verifyCopy(), OutputStream

### Community 91 - "ResolvedMedia"
Cohesion: 0.14
Nodes (7): DownloadIdentity, ExportRecord, mediaRequestHeaders(), MediaRequests, SiteDownloadCookieStore, ResolvedMedia, SiteDownloadDownloadTest

### Community 94 - "LocalStreamProxy"
Cohesion: 0.26
Nodes (5): Entry, LocalStreamProxy, requireNoHttpUserInfo(), RootHeaderContext, ServerEndpoint

### Community 96 - "RemoteDownloadSheet"
Cohesion: 0.19
Nodes (8): Thumbnail(), DownloadSectionLabel(), MetadataTermChips(), RemoteDetailHero(), RemoteDownloadSheet(), SiteDownloadCookiesDialog(), SiteDownloadScreen(), SiteMetadataHighlights()

### Community 98 - ".page"
Cohesion: 0.20
Nodes (5): catalogImage(), EroticMvCatalog, OtherCatalogHttp, CatalogPage, SiteCatalog

### Community 99 - "CatalogViewModel.kt"
Cohesion: 0.20
Nodes (4): Cached, catalogCacheJson(), MovieCatalogConfig, shouldPreserveCatalogResultsOnRefresh()

### Community 102 - "AnnouncementSource.kt"
Cohesion: 0.35
Nodes (7): Announcement, announcementApplies(), AnnouncementFetchResult, AnnouncementSource, compareAppVersions(), parseAnnouncement(), sanitizeAnnouncementUrl()

### Community 104 - "FlixcloudPlaylistDataSource.kt"
Cohesion: 0.05
Nodes (15): decodeFlixcloudPlaylist(), decodeFlixcloudSegment(), FlixcloudPlaylistDataSource, matchesAt(), unwrapFlixcloudSegmentInPlace(), hlsIv(), KwikPlaylistDataSource, rewriteKwikPlaylistIvs() (+7 more)

### Community 105 - "Test"
Cohesion: 0.10
Nodes (12): parseByteRange(), Details, Download, OperationFailure, OperationId, Preparation, presented(), Search (+4 more)

### Community 106 - "metadataFromDocument"
Cohesion: 0.11
Nodes (11): Document, EroticMvMetadataApi, eroticTaxonomy(), MetadataHttp, MetadataResponse, Absent or conditional information, Browse and filtering, Coverage and retained data (+3 more)

### Community 107 - "GlassSection"
Cohesion: 0.25
Nodes (7): GlassSection, BROWSE, DETAILS, HOME, NAVIGATION, POPUPS, REFRESH

### Community 108 - "ThemeAccent"
Cohesion: 0.07
Nodes (24): accentSeedOf(), AppBackdropDim, LIGHT, MEDIUM, STRONG, AppBackdropSource, ANIME, IMAGE (+16 more)

### Community 111 - "CaptionPosition"
Cohesion: 0.10
Nodes (11): CaptionPosition, BOTTOM, TOP, applyCaptionEdgeInset(), applyCaptionStyle(), applyContentAspectRatio(), bindUnifiedSettingsButton(), CaptionEdgeInset (+3 more)

### Community 112 - ".check"
Cohesion: 0.18
Nodes (3): AnnouncementManager, AnnouncementScheduler, NotificationPreferences()

### Community 114 - "SourceOptions.kt"
Cohesion: 0.27
Nodes (6): audioLabel(), commonSourceOptions(), EpisodeSource, parseEpisodeSources(), selectEpisodeSource(), SourceOptionsTest

### Community 115 - "ErrorDomain"
Cohesion: 0.14
Nodes (14): ErrorDomain, ACCOUNT, APP, BACKUP, DIAGNOSTICS, DOWNLOAD, EXPORT, LINK (+6 more)

### Community 117 - "decodeRelayFrame"
Cohesion: 0.38
Nodes (5): decodeRelayFrame(), EndOfStoredEvents, Event, RelayFrame, RelayFrameDecision

### Community 119 - "AppGraph"
Cohesion: 0.20
Nodes (5): AppGraph, playsInWebView(), Profiles, ProviderCatalog, Necessary host adaptations

### Community 122 - "SecureTokenStore"
Cohesion: 0.13
Nodes (3): SecureTokenStore, CrashReporter, DiagnosticsLog

### Community 123 - "Security and privacy findings"
Cohesion: 0.29
Nodes (7): S1 — Cache server reads unbounded lines before checking their length (P2, high confidence), S2 — Remote response bodies and pagination lack resource bounds (P2, high confidence), S3 — Verification completion accepts unrelated HTTPS pages (P2, high confidence), S4 — Session reset can race background network users (P2, high confidence), S5 — Native artifact verification checks size, not identity (P2, high confidence), S6 — Diagnostic redaction is incomplete (P3, high confidence), Security and privacy findings

### Community 124 - "ThemePreset"
Cohesion: 0.07
Nodes (24): ThemeBackground, AMOLED, DARK, ThemeContrast, HIGH, MEDIUM, STANDARD, ThemePreset (+16 more)

### Community 125 - "LocalStreamProxyHeaderPolicyTest"
Cohesion: 0.18
Nodes (3): HttpOrigin, RootHeaderPolicyInterceptor, LocalStreamProxyHeaderPolicyTest

### Community 127 - "SiteDownloadViewModel.kt"
Cohesion: 0.17
Nodes (10): PreparationStatus, isPreparedQueueRow(), mergePreparedQueueRows(), pendingEpisodeRowId(), appSiteDownloadRepository(), isPendingRow(), libraryRows(), pausePending() (+2 more)

### Community 128 - "SkipTimingStatus"
Cohesion: 0.15
Nodes (12): SkipReportState, FAILED, IDLE, SENDING, SENT, SkipTimingStatus, ANISKIP, CHECKING (+4 more)

### Community 129 - "TvPlayerControl"
Cohesion: 0.15
Nodes (13): TvPlayerControl, CAPTIONS, EPISODES, FORWARD, FULLSCREEN, MUTE, MY_LIST, NEXT (+5 more)

### Community 131 - "CatalogScreen"
Cohesion: 0.15
Nodes (3): CatalogScreen(), catalogHomeSections(), ReferenceHomeChrome()

### Community 132 - "downloadLocation"
Cohesion: 0.29
Nodes (7): DownloadLocation, downloadLocation(), episodeTitle(), outputLocation(), safeFileName(), audioTag(), DownloadNamingTest

### Community 134 - "UI and usability findings"
Cohesion: 0.29
Nodes (7): U1 — Failed episodes are labeled “Queued” in details (P2), U2 — Search results can belong to an earlier query (P2), U3 — Documented year/type filters are absent (P3), U4 — Library grouping parses presentation text (P2), U5 — Settings failures use success-like styling (P3), U6 — Accessibility/localization coverage needs expansion (P3, verification gap), UI and usability findings

### Community 135 - "RelatedLayout"
Cohesion: 0.33
Nodes (3): RelatedLayout, LIST, TITLES

### Community 136 - ".configureRequestHeaders"
Cohesion: 0.25
Nodes (3): mediaRequestHeaders(), PlaybackStatus, onStart()

### Community 138 - "c"
Cohesion: 0.33
Nodes (4): deadLoadExhausted(), isStreamRefusal(), refusalEndsStream(), StreamRefusalLoadErrorPolicy

### Community 139 - "DpadLongPress.kt"
Cohesion: 0.26
Nodes (5): dpadClickable(), DpadPress, CLICK, LONG_CLICK, DpadPressTracker

### Community 140 - "DownloadRow"
Cohesion: 0.36
Nodes (5): downloadGroupArtwork(), DownloadLibrarySnapshot, downloadLibrarySnapshot(), DownloadRow, DownloadGroupArtworkTest

### Community 141 - "ReferencePlaybackRegistry"
Cohesion: 0.14
Nodes (5): HostPlaybackSnapshot, onStop(), Binding, ReferencePlaybackRegistry, close()

### Community 144 - "KissKH metadata API"
Cohesion: 0.33
Nodes (5): KissKH metadata API, Query/API use, Scope and completeness limits, Structured mapping, Verified endpoints and public data

### Community 145 - "TvTextInputType"
Cohesion: 0.40
Nodes (5): TvTextInputType, EMAIL, NUMBER, PASSWORD, TEXT

### Community 146 - "EpisodeDownloads"
Cohesion: 0.17
Nodes (6): StreamItem, EpisodeDownloads, kwikProgressiveMp4(), availableVideoHeights(), fetchedLike(), ReferenceResolved

### Community 148 - "MidnightSettingSwitchRow"
Cohesion: 0.38
Nodes (6): MidnightSettingRow(), MidnightSettingRowScaffold(), MidnightSettingSpotlightMark(), MidnightSettingsSection(), MidnightSettingSwitchRow(), MidnightSwitch()

### Community 149 - "PlayerSystemBars.kt"
Cohesion: 0.25
Nodes (6): PlayerBars, ALL, NONE, STATUS_ONLY, playerBarsFor(), PlayerSystemBarsEffect()

### Community 150 - "AnnouncementManager.kt"
Cohesion: 0.05
Nodes (6): OpenSubtitlesAuthManager, OpenSubtitlesCredentials, findActivity(), AnnouncementWorker, build.sh script, usage()

### Community 151 - "CatalogItem"
Cohesion: 0.42
Nodes (4): CatalogFailureState, catalogFailureState(), CatalogItem, CatalogViewModelTest

### Community 152 - "AnimeRepository.kt"
Cohesion: 0.08
Nodes (14): AnimeNetwork, findMediaUrl(), ResultSort, NEWEST, OLDEST, RELEVANCE, TITLE, sortResults() (+6 more)

### Community 153 - ".row"
Cohesion: 0.40
Nodes (3): DownloadGroupProgress, downloadGroupProgress(), DownloadGroupProgressTest

### Community 154 - "Real-device verification"
Cohesion: 0.18
Nodes (10): Current pending release: player controls and download playback state, Final saved-shows verification (cheaper subagent), Found and fixed, Library and Settings/announcements release, Notice failure follow-up and Korean-only KissKH, Offline-preferred playback (pending device verification), Player-port release verification (2026-11-08), Real-device verification (+2 more)

### Community 155 - "size"
Cohesion: 0.10
Nodes (5): backdropBlur(), glassEnabled(), AutoSkipButtonBounds(), autoSkipSweep(), chromeFade()

### Community 156 - "ErrorAction"
Cohesion: 0.20
Nodes (9): ErrorAction, CHANGE_SERVER, CHOOSE_FILE, FREE_SPACE, OPEN_SETTINGS, RETRY, RETRY_EXPORT, SEND_DIAGNOSTICS (+1 more)

### Community 157 - "HlsPlaylistRewrite.kt"
Cohesion: 0.42
Nodes (7): isDeclaredHlsPlaylist(), looksLikeHlsPlaylist(), replaceUri(), resolveAndRegister(), rewriteHlsPlaylist(), rewriteLine(), rewriteUriAttribute()

### Community 158 - "NextEpisodeTrigger"
Cohesion: 0.20
Nodes (10): EmbedAutoAdvanceStep, embedAutoAdvanceStep(), ADVANCE, ARM, WAIT, NextEpisodeTrigger, nextEpisodeTrigger(), AFTER_CREDITS (+2 more)

### Community 160 - "URI"
Cohesion: 0.11
Nodes (12): subtitleSidecarName(), DownloadRepository, deleteEmptyDirectoriesUpTo(), deleteOutputCompanions(), deleteOwnedMedia(), findOutputSubtitleSidecars(), isOutputSubtitleSidecar(), isPlayableOutputSubtitle() (+4 more)

### Community 161 - "MidnightNoticeTone"
Cohesion: 0.40
Nodes (5): MidnightNoticeTone, ERROR, INFO, SUCCESS, WARNING

### Community 162 - "CategoryAudioDecision"
Cohesion: 0.50
Nodes (5): CategoryAudioDecision, categoryAudioDecision(), NotReady, Select, Settled

### Community 163 - "Reliability and correctness findings"
Cohesion: 0.20
Nodes (10): R10 — Cancellation deadlines are not hard limits on all provider/native operations (P2, risk requiring device/provider reproduction), R1 — Failed persistence writes leave authoritative memory ahead of disk (P1, high confidence), R3 — Direct enqueue has a process-death orphan window (P1, high confidence), R4 — HLS validation does not require expected audio (P1, high confidence), R5 — Final destination validation is weaker than staging validation (P1, high confidence), R6 — Unhandled action failures can crash the app (P2, high confidence), R7 — Retry can discard the only reference to unfinished output (P2, high confidence), R8 — Export service waits throughout transfers (P2, high confidence) (+2 more)

### Community 164 - "Browse, play and download"
Cohesion: 0.20
Nodes (9): Anilili Movies, Browse, play and download, Build, Downloads, Library and recovery tools, Release checks, Sessions, Supported sources (+1 more)

### Community 166 - "TvPlayerBackTarget"
Cohesion: 0.22
Nodes (9): TvPlayerBackTarget, CAPTION_APPEARANCE, CAPTIONS, tvPlayerBackTarget(), CONTROLS, EPISODE_DRAWER, NONE, OPEN_SUBTITLES (+1 more)

### Community 167 - "SavedFilesWatcher.kt"
Cohesion: 0.23
Nodes (3): missingSavedFiles(), querySavedFiles(), SavedFilesWatcher

### Community 168 - "FailureReason"
Cohesion: 0.22
Nodes (9): FailureReason, DELETION, EXPIRED_SOURCE, FOLDER_ACCESS, INCOMPATIBLE_EXPORT, NETWORK, STORAGE_FULL, UNKNOWN (+1 more)

### Community 170 - "cached"
Cohesion: 0.60
Nodes (3): opensTvPlayerControls(), cached(), Key

### Community 172 - "ClockFormat"
Cohesion: 0.25
Nodes (5): ClockFormat, SYSTEM, TWELVE_HOUR, TWENTY_FOUR_HOUR, timeOfDayFormatter()

### Community 173 - "DownloadQuality"
Cohesion: 0.12
Nodes (10): DownloadQuality, BEST, P1080, P360, P480, P720, MenuLanguage, ENGLISH (+2 more)

### Community 174 - "PlayerAspectRatio"
Cohesion: 0.25
Nodes (6): PlayerAspectRatio, ORIGINAL, RATIO_16_9, RATIO_21_9, RATIO_4_3, SQUARE

### Community 176 - "UI parity and usability checklist"
Cohesion: 0.40
Nodes (4): Corrections made in this pass, Screen and component review, UI parity and usability checklist, Verification still needed

### Community 178 - "TransferState"
Cohesion: 0.25
Nodes (8): TransferState, COMPLETE, DOWNLOADING, FAILED, PAUSED, QUEUED, REMOVING, WAITING_NETWORK

### Community 182 - "Anilili feature audit checklist"
Cohesion: 0.25
Nodes (7): Anilili feature audit checklist, Artwork and reliability, Download management, Playback and personal library, Release gate, Settings and appearance from Anilili, Source, search, and metadata

### Community 191 - "ReferenceChromeTokens.kt"
Cohesion: 0.43
Nodes (3): appCanvasFade(), drawCanvasFade(), Midnight

### Community 192 - "DownloadAction"
Cohesion: 0.11
Nodes (17): Availability, SAVED, STAGING, DownloadAction, DELETE, PAUSE, RESUME, RETRY (+9 more)

### Community 194 - "Review implementation status"
Cohesion: 0.29
Nodes (5): Latest user corrections, Reliability and security, Review implementation status, UI, performance, and features, Verification

### Community 197 - "PosterElement"
Cohesion: 0.20
Nodes (9): encodeHiddenPosterElements(), PosterElement, ADULT, DETAILS, DUB, NEW_EPISODES, PROGRESS, SCORE (+1 more)

### Community 201 - "UiState.kt"
Cohesion: 0.33
Nodes (5): Error, Loading, rethrowIfCancellation(), Success, UiState

### Community 206 - "UiScale"
Cohesion: 0.33
Nodes (4): UiScale, LARGE, MEDIUM, SMALL

### Community 211 - "Dramatip metadata API"
Cohesion: 0.40
Nodes (4): Dramatip metadata API, Live evidence and limits, Public surfaces and methods, Verified fields retained

### Community 218 - "EroticMV metadata API"
Cohesion: 0.50
Nodes (3): Endpoints and operations, EroticMV metadata API, Fields retained

### Community 221 - "TvEpisodeChangeFocus"
Cohesion: 0.32
Nodes (8): TvEpisodeChangeFocus, tvEpisodeChangeFocus(), CONTROLS, HOLD, SURFACE, UNCHANGED, tvFocusLostTarget(), tvOverlayDismissFocus()

### Community 235 - "verificationPage"
Cohesion: 0.27
Nodes (4): verificationPage(), SessionBrowser(), verificationOriginMatches(), VerificationPageTest

### Community 266 - "GestureDragAxis"
Cohesion: 0.67
Nodes (3): GestureDragAxis, Horizontal, Vertical

## Knowledge Gaps
- **479 isolated node(s):** `Preparation`, `Search`, `Sources`, `NotReady`, `Settled` (+474 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 1241 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **82 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `DiagnosticsLog` connect `SecureTokenStore` to `ExternalPlayer.kt`, `PlayerGestureControls.kt`, `PlayerSurface.kt`, `AppBackdrop.kt`, `LegacyTrustAnchors.kt`, `MediaCache.kt`, `CastRoutes.kt`, `AnnouncementManager.kt`, `PlaybackBuffer.kt`, `SettingsStore.kt`, `PlaybackService.kt`, `SleepTimer.kt`, `TvFocusTarget.kt`, `LocalStreamProxy.kt`, `ArtworkBitmapLoader.kt`, `MegaPlayCdnHealth.kt`, `PlaybackDiagnostics.kt`, `AppDns`, `ErrorReporting.kt`, `FlixcloudPlaylistDataSource.kt`?**
  _High betweenness centrality (0.086) - this node is a cross-community bridge._
- **Are the 55 inferred relationships involving `PlayerSurface()` (e.g. with `.launch()` and `AutoSkipButtonBounds()`) actually correct?**
  _`PlayerSurface()` has 55 INFERRED edges - model-reasoned connections that need verification._
- **What connects `Preparation`, `Search`, `Sources` to the rest of the system?**
  _479 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `AnililiError` be split into smaller, more focused modules?**
  _Cohesion score 0.02 - nodes in this community are weakly interconnected._
- **Why does `SettingsStore` connect `SettingsStore` to `LibraryScreens.kt`, `AniListModels.kt`, `InPlayerEpisodeList.kt`, `PlayerGestureControls.kt`, `PlayerSurface.kt`, `RelatedLayout`, `CaptionStyle`, `PlayerSurface`, `AppBackdrop.kt`, `AppDeviceProfile.kt`, `MediaCache.kt`, `PlayerSystemBars.kt`, `AnnouncementManager.kt`, `PlayerSettingsSheet.kt`, `PlaybackBuffer.kt`, `SettingsStore.kt`, `PlaybackService.kt`, `size`, `AppDeviceProfile.kt`, `.init`, `developer_notices.py`, `SubtitleCueLayout.kt`, `SourceFilterLanguage`, `ClockFormat`, `DownloadQuality`, `PlayerAspectRatio`, `PreferredAudioLanguage`, `Category`, `FloatingNavigationBar.kt`, `PosterElement`, `AppDns`, `PreferredSubtitleLanguage`, `UiScale`, `OpenSubtitlesPanel.kt`, `AppColorScheme.kt`, `GlassSection`, `ThemeAccent`, `CaptionPosition`, `ThemePreset`?**
  _High betweenness centrality (0.077) - this node is a cross-community bridge._
- **Should `LibraryScreens.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.08179162609542356 - nodes in this community are weakly interconnected._
- **Why does `PlayerSurface()` connect `PlayerSurface` to `SkipTimingStatus`, `ExternalPlayer.kt`, `InPlayerEpisodeList.kt`, `PlayerGestureControls.kt`, `PlayerSurface.kt`, `SettingsStore`, `.configureRequestHeaders`, `PlayerControlsScaffold`, `CaptionStyle`, `PlayerGestureControls.kt`, `EpisodeDownloads`, `ui/components/AnililiSilhouette.kt`, `PlayerSettingsSheet.kt`, `ErrorNotice.kt`, `SettingsStore.kt`, `size`, `PlaybackService.kt`, `NextEpisodeTrigger`, `SubtitleTrackMerge.kt`, `TvPlayerBackTarget`, `LibraryModels.kt`, `ExternalSubtitleFiles.kt`, `SleepTimer.kt`, `Category`, `SubtitleItem`, `Listener`, `CastButton`, `SubtitleDelay.kt`, `EroticMvMetadataApi.kt`, `MidnightHelper`, `OpenSubtitlesPanel.kt`, `TvEpisodeChangeFocus`, `CaptionPosition`, `.check`, `AppGraph`?**
  _High betweenness centrality (0.057) - this node is a cross-community bridge._