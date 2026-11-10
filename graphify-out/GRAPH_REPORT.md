# Graph Report - AnililiMovies  (2026-11-10)

## Corpus Check
- 308 files · ~277,254 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 40 file(s) not represented in the graph (top: .xml 22, .properties 4, (none) 3)

## Summary
- 4695 nodes · 11391 edges · 237 communities (160 shown, 77 thin omitted)
- Extraction: 94% EXTRACTED · 6% INFERRED · 0% AMBIGUOUS · INFERRED: 670 edges (avg confidence: 0.86)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `ce52c4fc`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- AnililiError
- LibraryScreens.kt
- AniListModels.kt
- ExternalPlayer.kt
- InPlayerEpisodeList.kt
- CastUi.kt
- PlayerSurface.kt
- SettingsStore
- TvPlayerControls.kt
- NostrUpdateSource.kt
- CaptionStyle
- PlayerSurface
- MidnightDialog.kt
- PlayerGestureControls.kt
- AppBackdrop.kt
- TvTextField.kt
- .all
- AppDeviceProfile.kt
- MediaCache.kt
- CastRoutes.kt
- ui/components/AnililiSilhouette.kt
- SiteDownloadSources.kt
- PlayerSettingsSheet.kt
- .operation
- .manager
- PlayerScreenLock.kt
- PlaybackBuffer.kt
- SettingsStore.kt
- PlaybackService.kt
- dispatchers
- SearchScreenTest.kt
- AppSymbol
- CaptionAppearance.kt
- KisskhCatalog.kt
- SubtitleTrackMerge.kt
- Composable
- DownloaderViewModel.kt
- OpenSubtitlesClient.kt
- LibraryModels.kt
- SubtitleCueLayout.kt
- SourceFilterLanguage
- ExternalSubtitleFiles.kt
- SleepTimer.kt
- SiteDownloadSeries
- BoundedUrlTokenRegistry
- FfmpegRemux.kt
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
- SubtitleCueTimes.kt
- PlaybackDiagnostics.kt
- FocusFill.kt
- MidnightHelper
- AppIcon
- AppDns
- PreferredSubtitleLanguage
- ErrorReporting.kt
- PopupGlass.kt
- AppColorScheme.kt
- CatalogViewModel.kt
- SiteMetadataApi
- SiteDownloadEpisode
- ErrorNotice.kt
- Anime
- OpenSubtitlesPanel.kt
- SubtitleSearchLanguage
- SiteShowMetadata
- LibraryTools.kt
- ResolvedMedia
- DeletionTestProvider.java
- FlixcloudPlaylistDataSource.kt
- LocalStreamProxy
- LocalStreamProxyService.kt
- RemoteDownloadSheet
- SafeDeletion.kt
- .page
- HlsExportTest.kt
- LibraryNavigationTest
- instrumentationregistry
- ThreadTraceListener.java
- .switchPlaybackPlayer
- KwikPlaylistDataSource.kt
- Test
- AutoDns
- BackdropBlur.kt
- ThemeAccent
- Test
- Listener
- EpisodeControlPlayer
- .check
- OperationRecovery.kt
- SourceOptions.kt
- ErrorDomain
- UniqueStreamKeyDataSource
- MetadataPage
- MetadataHttp
- SavedFilesWatcher.kt
- Test
- AnimeRepository.kt
- SecureTokenStore
- DohProvider
- ThemePreset
- LocalStreamProxyHeaderPolicyTest
- ForwardingPlayer
- AutoSkipCountdown.kt
- SkipTimingStatus
- TvPlayerControl
- ResultSort
- CatalogScreen
- downloadLocation
- DownloadNotifications.kt
- NetworkIdentity.kt
- ThemeSettings.kt
- .configureRequestHeaders
- .onCreate
- c
- DpadLongPress.kt
- DownloadRow
- ReferencePlaybackRegistry
- ForegroundCheck
- LegacyTrustAnchors.kt
- DefaultQuality
- PosterElement
- EpisodeDownloads
- .write
- MidnightSettingSwitchRow
- PlayerSystemBars.kt
- AnnouncementManager.kt
- CatalogItem
- ContinueWatchingRow
- .row
- Real-device verification
- GlassSection
- ErrorAction
- HlsPlaylistRewrite.kt
- NextEpisodeTrigger
- CatalogPosterCard
- DownloadRepository
- parseEroticMvMetadataPage
- X509TrustManager
- Reliability and correctness findings
- Browse, play and download
- .init
- TvPlayerBackTarget
- CatalogPage
- FailureReason
- selectCachedHlsVariant
- .isChallenge
- ProviderBenchmark
- ClockFormat
- DownloadQuality
- PlayerAspectRatio
- TransferListener
- verificationPage
- CatalogHero
- TransferState
- isExportDurationComplete
- OrderedWrites
- SiteDownloadRepositoryTest.kt
- Anilili feature audit checklist
- MenuLanguage
- ThemeStyle
- DiagnosticsLog
- .transform
- Theme.kt
- Base64Compat
- Application
- backdropBlur
- ReferenceChromeTokens.kt
- DownloadAction
- SaveState
- Security and privacy findings
- ExampleInstrumentedTest.kt
- DiscordStatus
- RelatedLayout
- AppBackdropDim
- ThemeBackground
- ThemeContrast
- UiState.kt
- normalizeOutputFolder
- CancellableHttpTest.kt
- X509TrustManager
- .legacyIdentity
- KissKH metadata API
- PlaylistForeignSegments.kt
- .event
- DesignSystem.kt
- EditionProviderBoundaryTest
- Dramatip metadata API
- InterceptionRetry
- ContentObserver
- Midnight
- findActivity
- ExampleUnitTest.kt
- build.sh
- EroticMV metadata API
- UI parity and usability checklist
- GestureDragAxis
- Availability
- Device UI verification
- Developer announcements
- AGENTS.md
- components/README.md
- docs/KISSKH.md
- THIRD_PARTY_NOTICES.md
- make-export-benchmark.sh

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
- `Download playback-state correction (current source)` --references--> `WatchProgressBar()`  [INFERRED]
  docs/UI_REFERENCE_CHECKLIST.md → app/src/main/java/com/anilili/ui/components/Common.kt
- `Verification plan` --references--> `DownloadSelectionTest`  [INFERRED]
  docs/ui-consistency-and-wrapping-audit.md → app/src/androidTest/java/com/example/animepahedl/DownloadSelectionTest.kt
- `Device verification` --references--> `HlsExportTest`  [INFERRED]
  README.md → app/src/androidTest/java/com/example/animepahedl/HlsExportTest.kt
- `Verification plan` --references--> `LibraryNavigationTest`  [INFERRED]
  docs/ui-consistency-and-wrapping-audit.md → app/src/androidTest/java/com/example/animepahedl/LibraryNavigationTest.kt
- `Necessary host adaptations` --references--> `ProviderCatalog`  [INFERRED]
  docs/REFERENCE_PLAYER_PORT.md → app/src/main/java/com/anilili/data/ProviderCatalog.kt

## Import Cycles
- None detected.

## Communities (237 total, 77 thin omitted)

### Community 0 - "AnililiError"
Cohesion: 0.02
Nodes (98): AnililiError, ACCOUNT_LIBRARY_LOAD_FAILED, ACCOUNT_LIST_ENTRY_DELETE_FAILED, ACCOUNT_LIST_UPDATE_FAILED, ACCOUNT_LOGIN_ABANDONED, ACCOUNT_LOGIN_PAGE_FAILED, ACCOUNT_PROFILE_LOAD_FAILED, ACCOUNT_PROGRESS_UPDATE_FAILED (+90 more)

### Community 1 - "LibraryScreens.kt"
Cohesion: 0.07
Nodes (7): EpisodeArtwork(), ReferenceSettingsIndex(), SettingsSectionCard(), SettingsSectionTile, onPause(), onResume(), openFromIntent()

### Community 2 - "AniListModels.kt"
Cohesion: 0.06
Nodes (78): AiringCountEntry, AiringCountMedia, AiringCountPage, AiringDayCount, AiringSchedule, AnimeStat, AppNotification, CoverImage (+70 more)

### Community 3 - "ExternalPlayer.kt"
Cohesion: 0.05
Nodes (42): LibraryStore, ExternalPlaybackOutcome, externalPlaybackOutcome(), ExternalPlaybackReturns, ExternalPlaybackSession, ExternalPlaybackRequest, externalPlaybackRequest(), externalPlayerHeaderPairs() (+34 more)

### Community 4 - "InPlayerEpisodeList.kt"
Cohesion: 0.06
Nodes (38): EpisodeItem, EpisodeLayout, GRID, LIST, EpisodeOrder, NEWEST_FIRST, OLDEST_FIRST, rememberFocusFill() (+30 more)

### Community 5 - "CastUi.kt"
Cohesion: 0.06
Nodes (8): BubbleSpec, playerClockAnchor(), PlayerClockReadout(), playerClockText(), rememberClockMinute(), playerTypography(), rememberTimedSkipPopup(), BubbleSpec

### Community 6 - "PlayerSurface.kt"
Cohesion: 0.05
Nodes (28): adaptiveVideoHeightCeiling(), applyAudioTrack(), applyCategoryAudioPreference(), applyTextTrack(), applyVideoHeight(), applyVideoHeightCeiling(), CaptionDefaults, captionDefaultsAfterChoice() (+20 more)

### Community 8 - "TvPlayerControls.kt"
Cohesion: 0.07
Nodes (28): bufferedFraction(), BufferedSeekTrack(), controlsSeekAnchorFor(), formatPlayerTime(), PlayerActionPill(), PlayerActionRow(), playerActionRowScale(), PlayerBackArrow() (+20 more)

### Community 9 - "NostrUpdateSource.kt"
Cohesion: 0.07
Nodes (22): Announcement, announcementApplies(), AnnouncementFetchResult, AnnouncementSource, compareAppVersions(), parseAnnouncement(), readBoundedAnnouncementBody(), sanitizeAnnouncementUrl() (+14 more)

### Community 10 - "CaptionStyle"
Cohesion: 0.04
Nodes (31): CaptionBackgroundColor, BLACK, GRAY, NAVY, WHITE, CaptionEdgeStyle, DROP_SHADOW, NONE (+23 more)

### Community 11 - "PlayerSurface"
Cohesion: 0.04
Nodes (45): PlayerContentScale, CROP, FILL, FIT, signsMergeDialogueLabel(), rememberScreenReaderActive(), CompiledSeparately(), Routes (+37 more)

### Community 12 - "MidnightDialog.kt"
Cohesion: 0.06
Nodes (3): EqualizerWaveIndicator(), MidnightSettingSpotlight, PlayerAccentScope()

### Community 13 - "PlayerGestureControls.kt"
Cohesion: 0.07
Nodes (35): applyBrightness(), applyVolume(), Brightness, canSlideSeek(), canStartHoldSpeed(), GestureIndicatorSide, gestureIndicatorSide(), Left (+27 more)

### Community 14 - "AppBackdrop.kt"
Cohesion: 0.07
Nodes (17): AppBackdrop, appCanvas(), appCanvasFade(), canvasColor(), centreCrop(), drawAppCanvas(), drawBackdropSlice(), hasAppBackdrop() (+9 more)

### Community 15 - "TvTextField.kt"
Cohesion: 0.07
Nodes (14): beginNativeTvEditing(), disableSelectionActionMode(), Callback, hideNativeTvKeyboard(), isImeVisible(), moveNativeFocusSafely(), NativeTvTextWatcher, SafeTvEditText (+6 more)

### Community 16 - ".all"
Cohesion: 0.12
Nodes (10): DeletionRecoveryTest, AppSettings, AndroidDownloadRepository, DownloadIdentity, adoptExistingVideo(), clearPendingVideo(), reconcileExistingVideos(), ExportRecord (+2 more)

### Community 17 - "AppDeviceProfile.kt"
Cohesion: 0.08
Nodes (28): isTelevision(), readTelevisionSignals(), TelevisionSignal, televisionSignal(), CHOSEN, LEANBACK_FEATURE, LEANBACK_LAUNCHER, NO_TOUCHSCREEN (+20 more)

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
Cohesion: 0.10
Nodes (27): Page, SiteDownloadRepository, chooseMediaVariant(), cleanProviderTitle(), decodeEroticMedia(), eroticVideoSource(), hostMatchesDomain(), isHlsMedia() (+19 more)

### Community 22 - "PlayerSettingsSheet.kt"
Cohesion: 0.10
Nodes (23): MediaVolumeSlider(), PlayerPanelTheme(), CaptionSections(), formatSubtitleDelay(), midnightSliderColors(), PanelHeader(), PlayerAdjustButton(), PlayerAdjustRow() (+15 more)

### Community 23 - ".operation"
Cohesion: 0.15
Nodes (7): reconcileDirectEnqueues(), PreparationJobs, PreparationWorker, appSiteDownloadRepository(), isPendingRow(), pausePending(), resumePending()

### Community 24 - ".manager"
Cohesion: 0.08
Nodes (5): DownloadRetryService, MediaVerification, OfflineDownloads, Listener, OfflineDownloadService

### Community 25 - "PlayerScreenLock.kt"
Cohesion: 0.08
Nodes (6): NextEpisodeAction(), NextEpisodeCard(), PlayerPauseOverlay(), PlayerLockButton(), PlayerLockOverlay(), ScrollHintColumn()

### Community 26 - "PlaybackBuffer.kt"
Cohesion: 0.08
Nodes (15): PlaybackBufferSize, AUTOMATIC, LARGE, MEDIUM, SMALL, VERY_LARGE, causeChain(), platformErrorCode() (+7 more)

### Community 27 - "SettingsStore.kt"
Cohesion: 0.06
Nodes (16): coercePlaybackSpeed(), coercePlayerBrightness(), decodeHiddenPosterElements(), decodePersistentCaptionDelays(), decodeSourceFilterLanguages(), defaultQualityForDevice(), deviceDefaultQuality(), DownloadDestination (+8 more)

### Community 28 - "PlaybackService.kt"
Cohesion: 0.07
Nodes (3): CastRouteStatus, PlaybackCacheBypassGate, PlaybackService

### Community 29 - "dispatchers"
Cohesion: 0.09
Nodes (4): OpenSubtitlesAuthManager, OpenSubtitlesCredentials, artworkRecoveryHostPolicy(), isAllowedArtworkRecoveryUrl()

### Community 30 - "SearchScreenTest.kt"
Cohesion: 0.06
Nodes (27): SearchScreenTest, 10. Collection summary gives count text precedence over its heading — P2, layout risk, 11. Text-heavy dialogs lack explicit scrolling — P2, layout risk, 12. Image-error copy does not adapt to thumbnail size — P3, layout risk, 13. Verification banner's View action can leave the user on Settings/About — P2, confirmed, 14. Search, empty states, and error treatments vary without shared components — P2, confirmed design divergence, 15. Completion and destructive-action terminology is inconsistent — P2, confirmed, 16. Spacing, chevrons, and text hierarchy are locally defined — P3, confirmed design divergence (+19 more)

### Community 31 - "AppSymbol"
Cohesion: 0.09
Nodes (20): AppSymbol(), DestinationSummary(), DownloadReferenceEpisode, DownloadReferenceEpisodeRow(), DownloadReferenceSeries, DownloadReferenceSeriesCard(), ReferenceCornerBadge(), WatchProgressBar() (+12 more)

### Community 33 - "CaptionAppearance.kt"
Cohesion: 0.08
Nodes (7): AppUiScale(), UnscaledUi(), SeriesLogo(), PlayerLayoutMetrics, playerLayoutMetrics(), PlayerWidthTier, PlayerSeriesName()

### Community 34 - "KisskhCatalog.kt"
Cohesion: 0.12
Nodes (16): detail(), json(), JsonResult, list(), localizeSubtitles(), playbackSubtitles(), Redirect, series() (+8 more)

### Community 35 - "SubtitleTrackMerge.kt"
Cohesion: 0.13
Nodes (33): assDialogueLine(), AssDocument, assPlayRes(), assRecord(), assStyleLine(), assTime(), buildSignsMerge(), decodeEntities() (+25 more)

### Community 36 - "Composable"
Cohesion: 0.12
Nodes (28): CaptionAppearanceDialog(), CoverScreenWithDialogWindow(), MidnightBodyTopStop(), MidnightConfirmDialog(), MidnightDialogFooter(), MidnightDialogFrame(), MidnightDialogHost(), MidnightFilledAction() (+20 more)

### Community 37 - "DownloaderViewModel.kt"
Cohesion: 0.08
Nodes (7): AnnouncementDialog(), loadBookmarks(), toggleBookmark(), details(), Index, kObjects(), kString()

### Community 38 - "OpenSubtitlesClient.kt"
Cohesion: 0.10
Nodes (6): isJwtExpired(), jwtLongClaim(), normalizeSubtitleLanguages(), OpenSubtitleDownload, OpenSubtitlesClient, parseSearchResults()

### Community 39 - "LibraryModels.kt"
Cohesion: 0.13
Nodes (25): capResumePoints(), DismissedRemoteRow, episodeKey(), EpisodeResume, EpisodeStop, historyAfterEpisodeToggled(), historyAfterEpisodeWatched(), historyAfterRemoteProgress() (+17 more)

### Community 40 - "SubtitleCueLayout.kt"
Cohesion: 0.12
Nodes (20): CaptionPosition, BOTTOM, TOP, autoStackedRow(), box(), CueBox, CueLayoutTextOutput, dedupeSimultaneousCues() (+12 more)

### Community 41 - "SourceFilterLanguage"
Cohesion: 0.06
Nodes (29): encodeSourceFilterLanguages(), SourceFilterLanguage, ARABIC, BENGALI, CHINESE, DUTCH, ENGLISH, FRENCH (+21 more)

### Community 42 - "ExternalSubtitleFiles.kt"
Cohesion: 0.13
Nodes (19): AppGraph, Profiles, OpenSubtitleSearchResult, ExportedSubtitle, AttachedSubtitle, bomCharset(), decodeSubtitleBytes(), displayName() (+11 more)

### Community 43 - "SleepTimer.kt"
Cohesion: 0.11
Nodes (12): Counting, EndOfEpisode, formatSleepRemaining(), Off, sleepFadeLevel(), SleepTimer, State, rememberSleepRemaining() (+4 more)

### Community 44 - "SiteDownloadSeries"
Cohesion: 0.12
Nodes (4): OnlineDramaHistoryTest, PlaybackHistory, SavedShowsStore, SiteDownloadSeries

### Community 45 - "BoundedUrlTokenRegistry"
Cohesion: 0.13
Nodes (5): BoundedUrlTokenRegistry, ProxyActiveResources, stopProxyResources(), TrackedProxyResources, UrlRegistryCapacityException

### Community 46 - "FfmpegRemux.kt"
Cohesion: 0.12
Nodes (7): consumeCancellable(), CachedHlsServer, InputStream, copyExactly(), normalizeCachedByteRanges(), parseUnknownTotalRange(), readBytesBounded()

### Community 47 - "DownloadStore"
Cohesion: 0.14
Nodes (5): DownloadDao, DownloadDatabase, DownloadStore, PendingOperation, StoredDownload

### Community 48 - "ExistingOutput.kt"
Cohesion: 0.13
Nodes (11): existingOutput(), ExistingOutputIndex, ExistingVideo, existingVideoName(), findExistingOutput(), hasOutputFolderAccess(), isPlausibleExistingVideo(), OutputFolderAccessRequired (+3 more)

### Community 49 - "PreferredAudioLanguage"
Cohesion: 0.08
Nodes (21): PreferredAudioLanguage, ARABIC, AUTOMATIC, BENGALI, CHINESE, ENGLISH, FRENCH, GERMAN (+13 more)

### Community 50 - "siteDownloadCookieHeader"
Cohesion: 0.15
Nodes (7): NativeHeaderContext, normalizeSiteDownloadCookieFile(), siteDownloadCookieHeader(), validCookiePair(), validSiteDownloadCookieInput(), NativeRequestHeaderPolicyTest, SiteDownloadCookieTest

### Community 51 - "SessionBrowser.kt"
Cohesion: 0.15
Nodes (3): DebugTrace, SessionBrowser(), WebViewClient

### Community 52 - "SiteMetadataApi.kt"
Cohesion: 0.22
Nodes (20): Document, eroticTaxonomy(), metadataDurationSeconds(), metadataFromDocument(), metadataFromHtml(), MetadataDetails, MetadataEpisode, metadataEpisode() (+12 more)

### Community 53 - "ExportService.kt"
Cohesion: 0.10
Nodes (7): ExistingOutputTest, IntegrityTest, DownloadCompleteReceiver, ExportProgress, onBind(), onCreate(), savingNotification()

### Community 54 - "Category"
Cohesion: 0.13
Nodes (14): Category, DUB, SUB, EpisodesResult, ProviderData, SourceVariant, DUB, GENERIC_SUB (+6 more)

### Community 55 - "AudioOutputFormat.kt"
Cohesion: 0.15
Nodes (7): AudioOutputFormat, AudioOutputMonitor, AudioRoute, audioRoute(), DECODED, PASSTHROUGH, Listener

### Community 56 - "TvFocusTarget.kt"
Cohesion: 0.15
Nodes (6): rememberTvFocusTarget(), tvFocusHandoff(), TvFocusTarget, tvFocusTarget(), TvFocusTargetElement, TvFocusTargetNode

### Community 57 - "AnimepaheDL codebase review"
Cohesion: 0.08
Nodes (24): AnimepaheDL codebase review, Architecture and strengths, P1 — Full-library reconstruction on frequent progress events (P2), P2 — Details availability repeatedly copies/scans records (P2), P3 — Batch source selection scales poorly (P2), P4 — Parallel exporter implementations obscure production behavior (P3), P5 — Persistence schema and orchestration are difficult to evolve (P3), Performance and maintainability (+16 more)

### Community 58 - ".verifyExport"
Cohesion: 0.16
Nodes (5): HlsExportTest, TimerTask, TimerTask, ffmpegRemuxCachedHls(), requireCachedHlsSegments()

### Community 59 - "SubtitleItem"
Cohesion: 0.11
Nodes (15): hasUsableWindow(), playsInWebView(), SkipTimes, SourcesResult, StreamItem, SubtitleItem, ProviderCatalog, kwikProgressiveMp4() (+7 more)

### Community 60 - "Listener"
Cohesion: 0.08
Nodes (10): playbackFailureEntry(), httpFailureSummary(), shouldRetryDecoderAtLowerVideoResolution(), contentAspectRatio(), diagnosticSummary(), host(), isLocalMedia(), Listener (+2 more)

### Community 61 - "LocalStreamProxy.kt"
Cohesion: 0.13
Nodes (11): BodyHead, dispatchAcceptedSocket(), namedDaemonThreadFactory(), newBoundedProxyRequestExecutor(), ProxyRequest, readBody(), readHead(), readLine() (+3 more)

### Community 62 - "CastButton"
Cohesion: 0.11
Nodes (7): applyCastTextTrack(), CastButton(), Callback, openSystemCastPicker(), ThemedMediaRouteChooserDialogFragment, ThemedMediaRouteControllerDialogFragment, ThemedMediaRouteDialogFactory

### Community 65 - "SubtitleDelay.kt"
Cohesion: 0.13
Nodes (5): SubtitleDelay, SubtitleDelayRenderer, SubtitleDelayRenderersFactory, SubtitleDelaySeed, subtitleDelaySeed()

### Community 66 - "AnimepaheDL resume state"
Cohesion: 0.09
Nodes (23): ADB device test + bookmark feature, AnimepaheDL resume state, Changes in the latest task, Current follow-up release, Custom downloader only, Dedicated details and download-range dialog, Environment and hosting, Existing work and reference (+15 more)

### Community 67 - "FloatingNavigationBar.kt"
Cohesion: 0.20
Nodes (8): DownloaderNavigation(), barBlur(), barGlass(), barRim(), FloatingNavDisc(), FloatingNavIcon(), FloatingNavigationBar(), FloatingNavItem

### Community 69 - "MegaPlayCdnHealth.kt"
Cohesion: 0.18
Nodes (6): healthHost(), isMegaPlayMedia(), MegaPlayCdnHealth, Callback, MegaPlayCdnHealthInterceptor, MegaPlayHealthState

### Community 70 - "EroticMvMetadataApi.kt"
Cohesion: 0.25
Nodes (18): absoluteErotic(), cleanEroticDescription(), eroticCard(), eroticDurationSeconds(), eroticImages(), eroticImdb(), eroticRatings(), eroticSiteUrl() (+10 more)

### Community 71 - "Anilili UI reference audit"
Cohesion: 0.09
Nodes (20): File inventory, Original Anilili player port, Player-port release verification (2026-11-08), Review checklist, Anilili UI reference audit, Automated source comparison, Build/package verification, Complete active shared-component inventory (+12 more)

### Community 72 - "SubtitleCueTimes.kt"
Cohesion: 0.18
Nodes (13): loadSubtitleCueTimes(), loadSubtitleText(), nearestSubtitleCues(), parseArrowCueTimes(), parseAssCueTimes(), parseSubtitleCueTimes(), parseTimestampMs(), stripMarkup() (+5 more)

### Community 75 - "MidnightHelper"
Cohesion: 0.20
Nodes (13): MidnightChoiceRow(), midnightFocus(), MidnightHelper(), midnightInitialFocus(), MidnightRowScaffold(), MidnightSectionLabel(), midnightSelectable(), MidnightSelectionRow() (+5 more)

### Community 76 - "AppIcon"
Cohesion: 0.10
Nodes (21): AppIcon, Back, Bookmark, BookmarkFilled, Browse, Check, CheckCircle, Close (+13 more)

### Community 78 - "PreferredSubtitleLanguage"
Cohesion: 0.10
Nodes (17): PreferredSubtitleLanguage, ARABIC, AUTOMATIC, CHINESE, ENGLISH, FRENCH, GERMAN, HINDI (+9 more)

### Community 79 - "ErrorReporting.kt"
Cohesion: 0.14
Nodes (9): AnililiException, anililiFailure(), asFailure(), classifyTransport(), isCancellation(), raise(), report(), toFailure() (+1 more)

### Community 80 - "PopupGlass.kt"
Cohesion: 0.15
Nodes (5): blurLine(), boxBlur(), PopupBackdrop, popupGlass(), rememberPopupBackdrop()

### Community 82 - "CatalogViewModel.kt"
Cohesion: 0.18
Nodes (9): Cached, catalogPageForSource(), CatalogSource, EROTICMV, KISSKH, CatalogSourceOrder, CatalogSourcePreferences, MovieCatalogConfig (+1 more)

### Community 83 - "SiteMetadataApi"
Cohesion: 0.15
Nodes (9): MetadataSite, EROTICMV, KISSKH, SiteMetadataApi, SiteMetadataApis, Contract, Scope and verification, Supported-site data APIs (+1 more)

### Community 84 - "SiteDownloadEpisode"
Cohesion: 0.18
Nodes (12): isPreparedQueueRow(), mergePreparedQueueRows(), pendingEpisodeRowId(), batches(), chronologicalSiteEpisodes(), SiteDownloadBatch, SiteDownloadPreparation, downloadEpisodeDisplayOrder() (+4 more)

### Community 85 - "ErrorNotice.kt"
Cohesion: 0.22
Nodes (6): AnililiFailure, DetailLine(), ErrorCodeChip(), ErrorDetailsDialog(), failureAnnotated(), MidnightErrorNotice()

### Community 86 - "Anime"
Cohesion: 0.25
Nodes (7): AnimeRepository, Anime, AnimeDetails, AnimeRepository, Episode, text(), PreparationRecord

### Community 87 - "OpenSubtitlesPanel.kt"
Cohesion: 0.16
Nodes (5): defaultOpenSubtitlesLanguages(), openSubtitlesEpisodeField(), OpenSubtitlesPanel(), SubtitleResultRow(), PageSearchField()

### Community 88 - "SubtitleSearchLanguage"
Cohesion: 0.11
Nodes (17): SubtitleSearchLanguage, ARABIC, CHINESE, ENGLISH, FOLLOW_DEVICE, FRENCH, GERMAN, HINDI (+9 more)

### Community 89 - "SiteShowMetadata"
Cohesion: 0.16
Nodes (9): toAppMetadata(), MediaLinkRefreshGuard, SiteEpisodeMetadata, SiteShowImage, SiteShowMetadata, SiteShowPerson, SiteShowRating, SiteShowTerm (+1 more)

### Community 90 - "LibraryTools.kt"
Cohesion: 0.16
Nodes (8): copyCancellable(), classifyFailure(), diagnostics(), inspectStorage(), QueueOrder, StorageReport, verifyCopy(), OutputStream

### Community 91 - "ResolvedMedia"
Cohesion: 0.19
Nodes (5): mediaRequestHeaders(), MediaRequests, SiteDownloadCookieStore, ResolvedMedia, SiteDownloadDownloadTest

### Community 93 - "FlixcloudPlaylistDataSource.kt"
Cohesion: 0.23
Nodes (5): decodeFlixcloudPlaylist(), decodeFlixcloudSegment(), FlixcloudPlaylistDataSource, matchesAt(), unwrapFlixcloudSegmentInPlace()

### Community 94 - "LocalStreamProxy"
Cohesion: 0.23
Nodes (4): Entry, LocalStreamProxy, RootHeaderContext, ServerEndpoint

### Community 96 - "RemoteDownloadSheet"
Cohesion: 0.20
Nodes (7): Thumbnail(), DownloadSectionLabel(), MetadataTermChips(), RemoteDetailHero(), RemoteDownloadSheet(), SiteDownloadScreen(), SiteMetadataHighlights()

### Community 97 - "SafeDeletion.kt"
Cohesion: 0.20
Nodes (10): subtitleSidecarName(), deleteEmptyDirectoriesUpTo(), deleteOutputCompanions(), deleteOwnedMedia(), findOutputSubtitleSidecars(), isOutputSubtitleSidecar(), isPlayableOutputSubtitle(), localSubtitleUriExists() (+2 more)

### Community 98 - ".page"
Cohesion: 0.18
Nodes (6): catalogImage(), CatalogSessionCookies, EroticMvCatalog, OtherCatalogHttp, jsonInt(), jsonString()

### Community 100 - "LibraryNavigationTest"
Cohesion: 0.17
Nodes (4): DownloadSelectionTest, LibraryNavigationTest, Device verification, FFmpeg

### Community 101 - "instrumentationregistry"
Cohesion: 0.20
Nodes (4): PreparationResponsivenessTest, SearchRecoveryTest, AnimeRepository, AnimeRepository

### Community 104 - "KwikPlaylistDataSource.kt"
Cohesion: 0.22
Nodes (3): hlsIv(), KwikPlaylistDataSource, rewriteKwikPlaylistIvs()

### Community 105 - "Test"
Cohesion: 0.23
Nodes (3): parseByteRange(), SearchVerificationGate, RecoveryPolicyTest

### Community 108 - "ThemeAccent"
Cohesion: 0.13
Nodes (13): ThemeAccent, BLOOD_RED, CUSTOM, CYAN, EMERALD, GOLD, MONOCHROME, NEON_GREEN (+5 more)

### Community 109 - "Test"
Cohesion: 0.19
Nodes (3): requireNoHttpUserInfo(), LocalStreamProxyResourceLimitsTest, Callback

### Community 111 - "EpisodeControlPlayer"
Cohesion: 0.14
Nodes (6): applyCaptionStyle(), applyContentAspectRatio(), bindUnifiedSettingsButton(), EpisodeControlPlayer, toMedia3EdgeType(), videoMimeType()

### Community 112 - ".check"
Cohesion: 0.20
Nodes (3): AnnouncementManager, AnnouncementScheduler, NotificationPreferences()

### Community 113 - "OperationRecovery.kt"
Cohesion: 0.14
Nodes (9): Details, Download, OperationFailure, OperationId, Preparation, presented(), Search, Sources (+1 more)

### Community 114 - "SourceOptions.kt"
Cohesion: 0.28
Nodes (7): audioLabel(), audioTag(), commonSourceOptions(), EpisodeSource, parseEpisodeSources(), selectEpisodeSource(), SourceOptionsTest

### Community 115 - "ErrorDomain"
Cohesion: 0.14
Nodes (14): ErrorDomain, ACCOUNT, APP, BACKUP, DIAGNOSTICS, DOWNLOAD, EXPORT, LINK (+6 more)

### Community 117 - "MetadataPage"
Cohesion: 0.19
Nodes (8): EroticMvMetadataApi, browse(), MetadataBrowseQuery, MetadataPage, API methods and routing, Live audit evidence, Narto Drama public metadata API, Not supplied / not automatically fetched

### Community 118 - "MetadataHttp"
Cohesion: 0.16
Nodes (8): MetadataHttp, MetadataResponse, Absent or conditional information, Browse and filtering, Coverage and retained data, Kiss18 public metadata API, Sources actually visited, Verification

### Community 119 - "SavedFilesWatcher.kt"
Cohesion: 0.21
Nodes (3): missingSavedFiles(), querySavedFiles(), SavedFilesWatcher

### Community 121 - "AnimeRepository.kt"
Cohesion: 0.22
Nodes (5): AnimeNetwork, HttpFailure, isMediaPath(), toHttpUrlOrNullSafe(), VerificationRequired

### Community 123 - "DohProvider"
Cohesion: 0.17
Nodes (10): DohProvider, ADGUARD, AUTO, CANADIAN_SHIELD, CLOUDFLARE, CLOUDFLARE_SECURITY, DNS_SB, GOOGLE (+2 more)

### Community 124 - "ThemePreset"
Cohesion: 0.15
Nodes (12): ThemePreset, CRIMSON_NIGHT, CYBER, DEEP_SEA, EMBER, FOREST, GILDED, HIGH_CONTRAST (+4 more)

### Community 127 - "AutoSkipCountdown.kt"
Cohesion: 0.23
Nodes (5): AutoSkipButtonBounds(), AutoSkipCountdown, autoSkipCountdownLabel(), autoSkipSweep(), runAutoSkipCountdown()

### Community 128 - "SkipTimingStatus"
Cohesion: 0.15
Nodes (12): SkipReportState, FAILED, IDLE, SENDING, SENT, SkipTimingStatus, ANISKIP, CHECKING (+4 more)

### Community 129 - "TvPlayerControl"
Cohesion: 0.15
Nodes (13): TvPlayerControl, CAPTIONS, EPISODES, FORWARD, FULLSCREEN, MUTE, MY_LIST, NEXT (+5 more)

### Community 130 - "ResultSort"
Cohesion: 0.21
Nodes (8): findMediaUrl(), ResultSort, NEWEST, OLDEST, RELEVANCE, TITLE, sortResults(), AnimeRepositoryTest

### Community 131 - "CatalogScreen"
Cohesion: 0.15
Nodes (3): CatalogScreen(), catalogHomeSections(), ReferenceHomeChrome()

### Community 132 - "downloadLocation"
Cohesion: 0.31
Nodes (6): DownloadLocation, downloadLocation(), episodeTitle(), outputLocation(), safeFileName(), DownloadNamingTest

### Community 133 - "DownloadNotifications.kt"
Cohesion: 0.22
Nodes (3): DownloadSpeed, downloadsPendingIntent(), DownloadSpeedTest

### Community 135 - "ThemeSettings.kt"
Cohesion: 0.24
Nodes (7): accentSeedOf(), AppBackdropSource, ANIME, IMAGE, NONE, backdropUrlOf(), ProfileLook

### Community 136 - ".configureRequestHeaders"
Cohesion: 0.17
Nodes (5): HostPlaybackSnapshot, PlaybackStatus, onStart(), onStop(), close()

### Community 138 - "c"
Cohesion: 0.29
Nodes (4): deadLoadExhausted(), isStreamRefusal(), refusalEndsStream(), StreamRefusalLoadErrorPolicy

### Community 139 - "DpadLongPress.kt"
Cohesion: 0.26
Nodes (5): dpadClickable(), DpadPress, CLICK, LONG_CLICK, DpadPressTracker

### Community 140 - "DownloadRow"
Cohesion: 0.32
Nodes (6): downloadGroupArtwork(), DownloadLibrarySnapshot, downloadLibrarySnapshot(), DownloadRow, DownloadGroupArtworkTest, Verification still needed

### Community 144 - "DefaultQuality"
Cohesion: 0.18
Nodes (8): DefaultQuality, AUTO, HIGHEST, P1080, P360, P480, P720, dataSaverFallbackMessage()

### Community 145 - "PosterElement"
Cohesion: 0.20
Nodes (9): encodeHiddenPosterElements(), PosterElement, ADULT, DETAILS, DUB, NEW_EPISODES, PROGRESS, SCORE (+1 more)

### Community 148 - "MidnightSettingSwitchRow"
Cohesion: 0.33
Nodes (7): MidnightConnectionStatus(), MidnightSettingRow(), MidnightSettingRowScaffold(), MidnightSettingSpotlightMark(), MidnightSettingsSection(), MidnightSettingSwitchRow(), MidnightSwitch()

### Community 149 - "PlayerSystemBars.kt"
Cohesion: 0.25
Nodes (6): PlayerBars, ALL, NONE, STATUS_ONLY, playerBarsFor(), PlayerSystemBarsEffect()

### Community 151 - "CatalogItem"
Cohesion: 0.36
Nodes (4): CatalogFailureState, catalogFailureState(), CatalogItem, CatalogViewModelTest

### Community 152 - "ContinueWatchingRow"
Cohesion: 0.22
Nodes (4): ContinueWatchingRow(), LibraryScreen(), rememberSeriesPlaybackSummary(), SeriesPlaybackSummary

### Community 153 - ".row"
Cohesion: 0.40
Nodes (3): DownloadGroupProgress, downloadGroupProgress(), DownloadGroupProgressTest

### Community 154 - "Real-device verification"
Cohesion: 0.18
Nodes (10): Current pending release: player controls and download playback state, Final saved-shows verification (cheaper subagent), Found and fixed, Library and Settings/announcements release, Notice failure follow-up and Korean-only KissKH, Offline-preferred playback (pending device verification), Player-port release verification (2026-11-08), Real-device verification (+2 more)

### Community 155 - "GlassSection"
Cohesion: 0.20
Nodes (8): GlassSection, BROWSE, DETAILS, HOME, NAVIGATION, POPUPS, REFRESH, glassEnabled()

### Community 156 - "ErrorAction"
Cohesion: 0.20
Nodes (9): ErrorAction, CHANGE_SERVER, CHOOSE_FILE, FREE_SPACE, OPEN_SETTINGS, RETRY, RETRY_EXPORT, SEND_DIAGNOSTICS (+1 more)

### Community 157 - "HlsPlaylistRewrite.kt"
Cohesion: 0.42
Nodes (7): isDeclaredHlsPlaylist(), looksLikeHlsPlaylist(), replaceUri(), resolveAndRegister(), rewriteHlsPlaylist(), rewriteLine(), rewriteUriAttribute()

### Community 158 - "NextEpisodeTrigger"
Cohesion: 0.20
Nodes (10): EmbedAutoAdvanceStep, embedAutoAdvanceStep(), ADVANCE, ARM, WAIT, NextEpisodeTrigger, nextEpisodeTrigger(), AFTER_CREDITS (+2 more)

### Community 163 - "Reliability and correctness findings"
Cohesion: 0.20
Nodes (10): R10 — Cancellation deadlines are not hard limits on all provider/native operations (P2, risk requiring device/provider reproduction), R1 — Failed persistence writes leave authoritative memory ahead of disk (P1, high confidence), R3 — Direct enqueue has a process-death orphan window (P1, high confidence), R4 — HLS validation does not require expected audio (P1, high confidence), R5 — Final destination validation is weaker than staging validation (P1, high confidence), R6 — Unhandled action failures can crash the app (P2, high confidence), R7 — Retry can discard the only reference to unfinished output (P2, high confidence), R8 — Export service waits throughout transfers (P2, high confidence) (+2 more)

### Community 164 - "Browse, play and download"
Cohesion: 0.20
Nodes (9): Anilili Movies, Browse, play and download, Build, Downloads, Library and recovery tools, Release checks, Sessions, Supported sources (+1 more)

### Community 166 - "TvPlayerBackTarget"
Cohesion: 0.22
Nodes (9): TvPlayerBackTarget, CAPTION_APPEARANCE, CAPTIONS, tvPlayerBackTarget(), CONTROLS, EPISODE_DRAWER, NONE, OPEN_SUBTITLES (+1 more)

### Community 167 - "CatalogPage"
Cohesion: 0.36
Nodes (3): catalogCacheJson(), CatalogPage, SiteCatalog

### Community 168 - "FailureReason"
Cohesion: 0.22
Nodes (9): FailureReason, DELETION, EXPIRED_SOURCE, FOLDER_ACCESS, INCOMPATIBLE_EXPORT, NETWORK, STORAGE_FULL, UNKNOWN (+1 more)

### Community 172 - "ClockFormat"
Cohesion: 0.25
Nodes (5): ClockFormat, SYSTEM, TWELVE_HOUR, TWENTY_FOUR_HOUR, timeOfDayFormatter()

### Community 173 - "DownloadQuality"
Cohesion: 0.25
Nodes (6): DownloadQuality, BEST, P1080, P360, P480, P720

### Community 174 - "PlayerAspectRatio"
Cohesion: 0.25
Nodes (6): PlayerAspectRatio, ORIGINAL, RATIO_16_9, RATIO_21_9, RATIO_4_3, SQUARE

### Community 176 - "verificationPage"
Cohesion: 0.39
Nodes (3): verificationPage(), verificationOriginMatches(), VerificationPageTest

### Community 178 - "TransferState"
Cohesion: 0.25
Nodes (8): TransferState, COMPLETE, DOWNLOADING, FAILED, PAUSED, QUEUED, REMOVING, WAITING_NETWORK

### Community 182 - "Anilili feature audit checklist"
Cohesion: 0.25
Nodes (7): Anilili feature audit checklist, Artwork and reliability, Download management, Playback and personal library, Release gate, Settings and appearance from Anilili, Source, search, and metadata

### Community 183 - "MenuLanguage"
Cohesion: 0.29
Nodes (4): MenuLanguage, ENGLISH, SPANISH, SYSTEM

### Community 184 - "ThemeStyle"
Cohesion: 0.29
Nodes (5): ThemeStyle, NEUTRAL, TONAL, VIBRANT, VIVID

### Community 187 - "Theme.kt"
Cohesion: 0.52
Nodes (4): appColors(), ThemeSpec, wallpaperSeed(), AnimepaheDLTheme()

### Community 191 - "ReferenceChromeTokens.kt"
Cohesion: 0.43
Nodes (3): appCanvasFade(), drawCanvasFade(), Midnight

### Community 192 - "DownloadAction"
Cohesion: 0.29
Nodes (7): DownloadAction, DELETE, PAUSE, RESUME, RETRY, RETRY_SAVE, VERIFY

### Community 193 - "SaveState"
Cohesion: 0.29
Nodes (7): SaveState, CHECKING, COPYING, FAILED, NONE, PREPARING, WAITING

### Community 194 - "Security and privacy findings"
Cohesion: 0.29
Nodes (7): S1 — Cache server reads unbounded lines before checking their length (P2, high confidence), S2 — Remote response bodies and pagination lack resource bounds (P2, high confidence), S3 — Verification completion accepts unrelated HTTPS pages (P2, high confidence), S4 — Session reset can race background network users (P2, high confidence), S5 — Native artifact verification checks size, not identity (P2, high confidence), S6 — Diagnostic redaction is incomplete (P3, high confidence), Security and privacy findings

### Community 196 - "DiscordStatus"
Cohesion: 0.33
Nodes (4): DiscordStatus, DO_NOT_DISTURB, IDLE, ONLINE

### Community 197 - "RelatedLayout"
Cohesion: 0.33
Nodes (3): RelatedLayout, LIST, TITLES

### Community 198 - "AppBackdropDim"
Cohesion: 0.33
Nodes (4): AppBackdropDim, LIGHT, MEDIUM, STRONG

### Community 199 - "ThemeBackground"
Cohesion: 0.33
Nodes (3): ThemeBackground, AMOLED, DARK

### Community 200 - "ThemeContrast"
Cohesion: 0.33
Nodes (4): ThemeContrast, HIGH, MEDIUM, STANDARD

### Community 201 - "UiState.kt"
Cohesion: 0.33
Nodes (5): Error, Loading, rethrowIfCancellation(), Success, UiState

### Community 206 - "KissKH metadata API"
Cohesion: 0.33
Nodes (5): KissKH metadata API, Query/API use, Scope and completeness limits, Structured mapping, Verified endpoints and public data

### Community 207 - "PlaylistForeignSegments.kt"
Cohesion: 0.70
Nodes (4): hostOf(), playlistForeignHosts(), playlistSegmentCensus(), segmentHosts()

### Community 211 - "Dramatip metadata API"
Cohesion: 0.40
Nodes (4): Dramatip metadata API, Live evidence and limits, Public surfaces and methods, Verified fields retained

### Community 218 - "EroticMV metadata API"
Cohesion: 0.50
Nodes (3): Endpoints and operations, EroticMV metadata API, Fields retained

### Community 220 - "UI parity and usability checklist"
Cohesion: 0.50
Nodes (3): Corrections made in this pass, Screen and component review, UI parity and usability checklist

### Community 221 - "GestureDragAxis"
Cohesion: 0.67
Nodes (3): GestureDragAxis, Horizontal, Vertical

### Community 222 - "Availability"
Cohesion: 0.67
Nodes (3): Availability, SAVED, STAGING

## Knowledge Gaps
- **479 isolated node(s):** `CHOSEN`, `UI_MODE`, `LEANBACK_FEATURE`, `TELEVISION_FEATURE`, `LEANBACK_LAUNCHER` (+474 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 1221 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **77 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `SettingsStore` connect `SettingsStore` to `LibraryScreens.kt`, `AniListModels.kt`, `InPlayerEpisodeList.kt`, `PlayerSurface.kt`, `ThemeSettings.kt`, `TvPlayerControls.kt`, `CaptionStyle`, `PlayerSurface`, `PlayerGestureControls.kt`, `AppBackdrop.kt`, `DefaultQuality`, `AppDeviceProfile.kt`, `PosterElement`, `MediaCache.kt`, `PlayerSystemBars.kt`, `PlayerSettingsSheet.kt`, `PlaybackBuffer.kt`, `GlassSection`, `SettingsStore.kt`, `dispatchers`, `PlaybackService.kt`, `CaptionAppearance.kt`, `.init`, `OpenSubtitlesClient.kt`, `SubtitleCueLayout.kt`, `SourceFilterLanguage`, `ClockFormat`, `DownloadQuality`, `PlayerAspectRatio`, `PreferredAudioLanguage`, `Category`, `MenuLanguage`, `ThemeStyle`, `Theme.kt`, `DiscordStatus`, `RelatedLayout`, `AppBackdropDim`, `ThemeBackground`, `ThemeContrast`, `PreferredSubtitleLanguage`, `OpenSubtitlesPanel.kt`, `BackdropBlur.kt`, `ThemeAccent`, `DohProvider`, `ThemePreset`?**
  _High betweenness centrality (0.129) - this node is a cross-community bridge._
- **Are the 55 inferred relationships involving `PlayerSurface()` (e.g. with `.launch()` and `AutoSkipButtonBounds()`) actually correct?**
  _`PlayerSurface()` has 55 INFERRED edges - model-reasoned connections that need verification._
- **What connects `CHOSEN`, `UI_MODE`, `LEANBACK_FEATURE` to the rest of the system?**
  _479 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `AnililiError` be split into smaller, more focused modules?**
  _Cohesion score 0.02 - nodes in this community are weakly interconnected._
- **Why does `DiagnosticsLog` connect `DiagnosticsLog` to `ExternalPlayer.kt`, `CastUi.kt`, `PlayerSurface.kt`, `AppBackdrop.kt`, `LegacyTrustAnchors.kt`, `TvTextField.kt`, `MediaCache.kt`, `CastRoutes.kt`, `PlayerScreenLock.kt`, `PlaybackBuffer.kt`, `SettingsStore.kt`, `PlaybackService.kt`, `dispatchers`, `SleepTimer.kt`, `TvFocusTarget.kt`, `LocalStreamProxy.kt`, `ArtworkBitmapLoader.kt`, `MegaPlayCdnHealth.kt`, `PlaybackDiagnostics.kt`, `AppDns`, `ErrorReporting.kt`, `FlixcloudPlaylistDataSource.kt`, `LocalStreamProxyService.kt`, `KwikPlaylistDataSource.kt`, `UniqueStreamKeyDataSource`?**
  _High betweenness centrality (0.073) - this node is a cross-community bridge._
- **Should `LibraryScreens.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.07000686341798215 - nodes in this community are weakly interconnected._
- **Why does `AnililiError` connect `AnililiError` to `LibraryScreens.kt`, `ExternalPlayer.kt`, `PlayerSurface.kt`, `Listener`, `ErrorReporting.kt`, `ErrorDomain`, `ErrorNotice.kt`, `PlayerSettingsSheet.kt`, `OpenSubtitlesPanel.kt`, `ErrorAction`, `dispatchers`?**
  _High betweenness centrality (0.047) - this node is a cross-community bridge._