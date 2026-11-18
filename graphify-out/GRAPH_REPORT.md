# Graph Report - AnililiMovies  (2026-11-18)

## Corpus Check
- 322 files · ~284,702 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 38 file(s) not represented in the graph (top: .xml 22, (none) 3, .ttf 3)

## Summary
- 4942 nodes · 11926 edges · 312 communities (174 shown, 138 thin omitted)
- Extraction: 93% EXTRACTED · 7% INFERRED · 0% AMBIGUOUS · INFERRED: 783 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `61bd01db`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- AnililiError
- LibraryScreens.kt
- AniListModels.kt
- AudioOutputFormat.kt
- InPlayerEpisodeList.kt
- TvPlayerControls.kt
- PlayerSurface.kt
- SettingsStore
- SubtitleItem
- NostrUpdateSource.kt
- CaptionStyle
- Listener
- PlayerSettingsSheet.kt
- OpenSubtitlesPanel.kt
- AppBackdrop.kt
- SafeTvEditText
- .all
- AppDeviceProfile.kt
- MediaCache.kt
- CastRoutes.kt
- MidnightSettings.kt
- SiteDownloadSources.kt
- DownloaderViewModel.kt
- PlayerGestureControls
- OfflineDownloadService.kt
- CaptionAppearance.kt
- PlaybackBuffer.kt
- SettingsStore.kt
- Listener
- LibraryNavigationTest
- SearchScreenTest.kt
- MediaVolumeSlider
- ui/components/WaterFillLogoIndicator.kt
- FloatingNavigationBar.kt
- SubtitleTrackMerge.kt
- MidnightDialog.kt
- OperationRecovery.kt
- ExternalPlayer.kt
- LibraryModels.kt
- SubtitleCueLayout.kt
- SourceFilterLanguage
- SiteDownloadPreparation
- SleepTimer.kt
- SiteDownloadSeries
- BoundedUrlTokenRegistry
- SecureTokenStore
- DownloadStore
- ExistingOutput.kt
- PreferredAudioLanguage
- SiteDownloadCookieStore
- AppIcon
- SiteMetadataApi.kt
- build.sh
- PipeModels.kt
- CastStreamRouting
- AutoSkipCountdown.kt
- AnimepaheDL codebase review
- .verifyExport
- ProviderSubtitles.kt
- OpenSubtitlesAuthManager.kt
- SubtitleDelay.kt
- CastButton
- KisskhResilienceTest
- PopupGlass.kt
- VerificationRequired
- AnimepaheDL resume state
- KwikPlaylistDataSource.kt
- parseEroticMvMetadataPage
- AnimeRepository.kt
- EroticMvMetadataApi.kt
- Anilili UI reference audit
- FlixcloudPlaylistDataSource.kt
- PlaybackDiagnostics.kt
- PlayerControlsScaffold
- MidnightSelectors.kt
- LocalStreamProxy.kt
- AppDns
- PreferredSubtitleLanguage
- ErrorReporting.kt
- developer_notices.py
- SiteDownloadRepository
- SiteMetadataApi
- SiteDownloadEpisode
- LocalStreamProxy
- Anime
- .write
- SubtitleSearchLanguage
- ResolvedMedia
- LibraryTools.kt
- KisskhCatalog.kt
- ExternalSubtitleFiles.kt
- ArtworkRecovery
- .relay
- AppDeviceProfile.kt
- HlsDownloadSelection.kt
- PlatformErrorCodes.kt
- .serve
- NetworkIdentity.kt
- SiteDownloadSourcesTest
- .onCreate
- CatalogViewModel.kt
- ArtworkBitmapLoader.kt
- Test
- CastRoute
- MediaCache
- ThemeAccent
- Test
- UniqueStreamKeyDataSource
- EpisodeControlPlayer
- SheetSections
- CatalogScreen
- SourceOptions.kt
- ErrorDomain
- RemoteDownloadSheet
- VideoCacheSize
- parsePersistedExportRecord
- ResultSort
- Test
- GestureZone
- AnnouncementSource.kt
- Page
- AppColorScheme.kt
- LocalStreamProxyHeaderPolicyTest
- ForwardingPlayer
- ThreadTraceListener.java
- SkipTimingStatus
- TvPlayerControl
- BackdropBlur.kt
- .isChallenge
- DownloadNotifications.kt
- TvFocusTarget.kt
- AnnouncementInputBoundsTest
- PlaybackBufferSize
- c
- DpadLongPress.kt
- CatalogPosterCard
- ReferencePlaybackRegistry
- FailureReason
- ExoDatabase.kt
- TransferState
- historyAfterRemoteProgress
- AppGraph
- UiScale
- AppBackdropDim
- sameVideoFile
- TransferState
- Real-device verification
- ThemeContrast
- ErrorAction
- HlsPlaylistRewrite.kt
- NextEpisodeTrigger
- RelatedLayout
- SavedFilesWatcher.kt
- TvNativeTextField
- PanelHeader
- CatalogHero
- Browse, play and download
- ThemePreset
- TvPlayerBackTarget
- DeletionTestProvider.java
- ExampleInstrumentedTest.kt
- UiState.kt
- decodeRelayFrame
- ProviderBenchmark
- normalizeOutputFolder
- DownloadQuality
- PlayerAspectRatio
- WatchedFilter
- IOException
- .event
- FfmpegProgressParser
- OrderedWrites
- DataSource
- Anilili feature audit checklist
- .validatesContainerSignatureAndMinimumSize
- downloadLocation
- HlsSizeEstimator.kt
- DiagnosticsLog
- backdropBlur
- NativeTvTextWatcher
- SelectedHlsDownloadTest
- SafeDeletion.kt
- ThemeSettings.kt
- .transform
- PosterElement
- .onCreate
- ExistingOutputTest.kt
- TimeOfDay.kt
- Theme.kt
- .event
- Base64Compat
- Dramatip metadata API
- ExampleUnitTest.kt
- verificationPage
- parseEroticMvMetadataFeed
- X509TrustManager
- Device UI verification
- Developer announcements
- AGENTS.md
- components/README.md
- docs/KISSKH.md
- THIRD_PARTY_NOTICES.md
- make-export-benchmark.sh
- SiteMetadataApi
- ReferenceChromeTokens.kt
- DownloadAction
- DefaultQuality
- DownloadRow
- SaveState
- Viewer
- Callback
- .operation
- EroticMvMetadataApi
- TvEpisodeChangeFocus
- CaptionSections
- Test
- ThemeStyle
- appSiteDownloadRepository
- Kind
- .intercept
- TvTextInputType
- DesignSystem.kt
- Narto Drama public metadata API
- UI parity and usability checklist
- DiagnosticsLog
- findActivity
- PlayerSurface
- DownloadRepository.kt
- ThemeBackground
- AnnouncementManager.kt
- .legacyIdentity
- Callback
- MenuLanguage

## God Nodes (most connected - your core abstractions)
1. `SettingsStore` - 155 edges
2. `PlayerSurface()` - 135 edges
3. `AnililiError` - 114 edges
4. `PlaybackService` - 46 edges
5. `rememberFocusFill()` - 44 edges
6. `SiteDownloadSeries` - 36 edges
7. `ExportRecord` - 35 edges
8. `SourceFilterLanguage` - 33 edges
9. `LocalStreamProxy` - 30 edges
10. `DiagnosticsLog` - 29 edges

## Surprising Connections (you probably didn't know these)
- `API methods and routing` --references--> `MetadataPage`  [INFERRED]
  docs/api/NARTO.md → app/src/main/java/com/example/animepahedl/metadata/SiteMetadataApi.kt
- `Download playback-state correction (current source)` --references--> `WatchProgressBar()`  [INFERRED]
  docs/UI_REFERENCE_CHECKLIST.md → app/src/main/java/com/anilili/ui/components/Common.kt
- `Verification still needed` --references--> `SiteDownloadSourcesTest`  [INFERRED]
  UI_PARITY_CHECKLIST.md → app/src/test/java/com/example/animepahedl/SiteDownloadSourcesTest.kt
- `Necessary host adaptations` --references--> `PlaybackService`  [INFERRED]
  docs/REFERENCE_PLAYER_PORT.md → app/src/main/java/com/anilili/playback/PlaybackService.kt
- `Necessary host adaptations` --references--> `ProviderCatalog`  [INFERRED]
  docs/REFERENCE_PLAYER_PORT.md → app/src/main/java/com/anilili/data/ProviderCatalog.kt

## Import Cycles
- None detected.

## Communities (312 total, 138 thin omitted)

### Community 0 - "AnililiError"
Cohesion: 0.02
Nodes (98): AnililiError, ACCOUNT_LIBRARY_LOAD_FAILED, ACCOUNT_LIST_ENTRY_DELETE_FAILED, ACCOUNT_LIST_UPDATE_FAILED, ACCOUNT_LOGIN_ABANDONED, ACCOUNT_LOGIN_PAGE_FAILED, ACCOUNT_PROFILE_LOAD_FAILED, ACCOUNT_PROGRESS_UPDATE_FAILED (+90 more)

### Community 1 - "LibraryScreens.kt"
Cohesion: 0.05
Nodes (29): PlayerFillerSkip, PlayerFillerTag(), AppSymbol(), DestinationSummary(), DownloadReferenceEpisode, DownloadReferenceEpisodeRow(), DownloadReferenceSeries, DownloadReferenceSeriesCard() (+21 more)

### Community 2 - "AniListModels.kt"
Cohesion: 0.09
Nodes (50): AiringDayCount, AiringSchedule, CoverImage, CustomListNamesSerializer, DiscoverOptions, DiscoverOptionsData, FavouriteAnimeConnection, FuzzyDate (+42 more)

### Community 3 - "AudioOutputFormat.kt"
Cohesion: 0.15
Nodes (7): AudioOutputFormat, AudioOutputMonitor, AudioRoute, audioRoute(), DECODED, PASSTHROUGH, Listener

### Community 4 - "InPlayerEpisodeList.kt"
Cohesion: 0.06
Nodes (37): EpisodeItem, EpisodeLayout, GRID, LIST, EpisodeOrder, NEWEST_FIRST, OLDEST_FIRST, EpisodeFillerBadge() (+29 more)

### Community 5 - "TvPlayerControls.kt"
Cohesion: 0.06
Nodes (13): AppUiScale(), UnscaledUi(), bufferedFraction(), PlayerDownloadAction, playerTypography(), formatTvPlayerTime(), TvControlButton(), TvPlayerBackArrow() (+5 more)

### Community 6 - "PlayerSurface.kt"
Cohesion: 0.04
Nodes (35): playbackFailureEntry(), httpFailureSummary(), adaptiveVideoHeightCeiling(), applyAudioTrack(), applyCaptionEdgeInset(), applyCaptionStyle(), applyCategoryAudioPreference(), applyContentAspectRatio() (+27 more)

### Community 8 - "SubtitleItem"
Cohesion: 0.16
Nodes (19): SubtitleItem, loadSubtitleCueTimes(), loadSubtitleText(), nearestSubtitleCues(), parseArrowCueTimes(), parseAssCueTimes(), parseSubtitleCueTimes(), parseTimestampMs() (+11 more)

### Community 9 - "NostrUpdateSource.kt"
Cohesion: 0.16
Nodes (7): hexToBytes(), NostrEvent, NostrUpdateSource, RelayFrameGate, RelayScanResult, toHexString(), toNostrEvent()

### Community 10 - "CaptionStyle"
Cohesion: 0.04
Nodes (30): CaptionBackgroundColor, BLACK, GRAY, NAVY, WHITE, CaptionEdgeStyle, DROP_SHADOW, NONE (+22 more)

### Community 11 - "Listener"
Cohesion: 0.13
Nodes (5): contentAspectRatio(), diagnosticSummary(), Listener, preferredAudioTrackCodes(), stateName()

### Community 12 - "PlayerSettingsSheet.kt"
Cohesion: 0.03
Nodes (10): tvFocusHandoff(), tvFocusTarget(), playerClockAnchor(), PlayerClockReadout(), playerClockText(), rememberClockMinute(), GestureDragAxis, Horizontal (+2 more)

### Community 13 - "OpenSubtitlesPanel.kt"
Cohesion: 0.09
Nodes (6): focusFill(), defaultOpenSubtitlesLanguages(), openSubtitlesEpisodeField(), OpenSubtitlesPanel(), SubtitleResultRow(), PageSearchField()

### Community 14 - "AppBackdrop.kt"
Cohesion: 0.11
Nodes (10): AppBackdrop, appCanvas(), appCanvasFade(), canvasColor(), centreCrop(), drawAppCanvas(), drawBackdropSlice(), hasAppBackdrop() (+2 more)

### Community 15 - "SafeTvEditText"
Cohesion: 0.21
Nodes (4): beginNativeTvEditing(), hideNativeTvKeyboard(), isImeVisible(), SafeTvEditText

### Community 16 - ".all"
Cohesion: 0.12
Nodes (12): DeletionRecoveryTest, DownloadIdentity, adoptExistingVideo(), clearPendingVideo(), reconcileExistingVideos(), ExportProgress, ExportRecord, Exports (+4 more)

### Community 17 - "AppDeviceProfile.kt"
Cohesion: 0.09
Nodes (25): isTelevision(), readTelevisionSignals(), TelevisionSignal, televisionSignal(), CHOSEN, LEANBACK_FEATURE, LEANBACK_LAUNCHER, NO_TOUCHSCREEN (+17 more)

### Community 19 - "CastRoutes.kt"
Cohesion: 0.12
Nodes (4): CastRoutes, GoogleCastRoute, SessionAvailabilityListener, mediaClientOrNull()

### Community 20 - "MidnightSettings.kt"
Cohesion: 0.09
Nodes (8): EqualizerWaveIndicator(), MidnightSettingRow(), MidnightSettingRowScaffold(), MidnightSettingSpotlight, MidnightSettingSpotlightMark(), MidnightSettingsSection(), MidnightSettingSwitchRow(), MidnightSwitch()

### Community 21 - "SiteDownloadSources.kt"
Cohesion: 0.09
Nodes (29): toAppMetadata(), cleanProviderTitle(), EroticVideoSource, eroticVideoSource(), hostMatchesDomain(), isSupportedVideoFile(), jsonAfterMarker(), jsonArrayOrEmpty() (+21 more)

### Community 22 - "DownloaderViewModel.kt"
Cohesion: 0.07
Nodes (8): copyCancellable(), consumeCancellable(), loadBookmarks(), toggleBookmark(), details(), Index, kObjects(), kString()

### Community 23 - "PlayerGestureControls"
Cohesion: 0.12
Nodes (17): applyBrightness(), Brightness, canSlideSeek(), canStartHoldSpeed(), GestureLevel, playerDoubleTapSeekTarget(), PlayerDoubleTapZone, playerDoubleTapZone() (+9 more)

### Community 24 - "OfflineDownloadService.kt"
Cohesion: 0.05
Nodes (11): EpisodeDownloads, DownloadRetryService, forResolvedMedia(), isExpiredMediaResponse(), isTransientTransferHttpStatus(), MediaVerification, mediaVerificationEvents(), offlineCacheFactory() (+3 more)

### Community 25 - "CaptionAppearance.kt"
Cohesion: 0.08
Nodes (7): EpisodeArtwork(), NextEpisodeAction(), NextEpisodeCard(), PlayerPauseOverlay(), PlayerLockButton(), PlayerLockOverlay(), ScrollHintColumn()

### Community 26 - "PlaybackBuffer.kt"
Cohesion: 0.15
Nodes (5): AdjustableLoadControl, playbackBufferBytes(), PlaybackBufferPolicy, playbackBufferPolicy(), progressiveConstrainedPolicy()

### Community 27 - "SettingsStore.kt"
Cohesion: 0.06
Nodes (16): coercePlaybackSpeed(), coercePlayerBrightness(), decodeHiddenPosterElements(), decodePersistentCaptionDelays(), decodeSourceFilterLanguages(), defaultQualityForDevice(), deviceDefaultQuality(), DiscordStatus (+8 more)

### Community 28 - "Listener"
Cohesion: 0.15
Nodes (3): Listener, stateName(), shouldRetryDecoderAtLowerVideoResolution()

### Community 29 - "LibraryNavigationTest"
Cohesion: 0.16
Nodes (5): LibraryNavigationTest, AppSettings, downloadsPendingIntent(), Device verification, FFmpeg

### Community 30 - "SearchScreenTest.kt"
Cohesion: 0.05
Nodes (28): DownloadSelectionTest, SearchScreenTest, 10. Collection summary gives count text precedence over its heading — P2, layout risk, 11. Text-heavy dialogs lack explicit scrolling — P2, layout risk, 12. Image-error copy does not adapt to thumbnail size — P3, layout risk, 13. Verification banner's View action can leave the user on Settings/About — P2, confirmed, 14. Search, empty states, and error treatments vary without shared components — P2, confirmed design divergence, 15. Completion and destructive-action terminology is inconsistent — P2, confirmed (+20 more)

### Community 31 - "MediaVolumeSlider"
Cohesion: 0.16
Nodes (11): formatPlayerTime(), applyVolume(), GestureLevelIndicator(), MediaVolumeSlider(), PlaybackGestureIndicator(), PlayerBrightnessOverride(), readVolume(), resetPlayerBrightness() (+3 more)

### Community 33 - "ui/components/WaterFillLogoIndicator.kt"
Cohesion: 0.07
Nodes (21): silhouetteFrame(), silhouetteMask(), BubbleSpec, cappedFrameSteps(), DetailedWaterFillLogo(), liquidLevel(), LoopHandover, PlainWaterFillLogo() (+13 more)

### Community 34 - "FloatingNavigationBar.kt"
Cohesion: 0.10
Nodes (9): chromeFade(), DownloaderNavigation(), barBlur(), barGlass(), barRim(), FloatingNavDisc(), FloatingNavIcon(), FloatingNavigationBar() (+1 more)

### Community 35 - "SubtitleTrackMerge.kt"
Cohesion: 0.15
Nodes (30): assDialogueLine(), AssDocument, assPlayRes(), assRecord(), assStyleLine(), assTime(), decodeEntities(), formatFields() (+22 more)

### Community 36 - "MidnightDialog.kt"
Cohesion: 0.08
Nodes (35): rememberFocusFill(), CaptionAppearanceDialog(), DetailLine(), ErrorCodeChip(), ErrorDetailsDialog(), failureAnnotated(), MidnightErrorNotice(), CoverScreenWithDialogWindow() (+27 more)

### Community 37 - "OperationRecovery.kt"
Cohesion: 0.14
Nodes (9): Details, Download, OperationFailure, OperationId, Preparation, presented(), Search, Sources (+1 more)

### Community 38 - "ExternalPlayer.kt"
Cohesion: 0.05
Nodes (27): LibraryStore, ExternalPlaybackOutcome, externalPlaybackOutcome(), ExternalPlaybackReturns, ExternalPlaybackSession, ExternalPlaybackRequest, externalPlaybackRequest(), externalPlayerHeaderPairs() (+19 more)

### Community 39 - "LibraryModels.kt"
Cohesion: 0.14
Nodes (24): capResumePoints(), DismissedRemoteRow, episodeKey(), EpisodeResume, EpisodeStop, historyAfterEpisodeToggled(), historyAfterEpisodeWatched(), HistoryEntry (+16 more)

### Community 40 - "SubtitleCueLayout.kt"
Cohesion: 0.14
Nodes (18): CaptionPosition, BOTTOM, TOP, autoStackedRow(), box(), CueBox, CueLayoutTextOutput, dedupeSimultaneousCues() (+10 more)

### Community 41 - "SourceFilterLanguage"
Cohesion: 0.06
Nodes (29): encodeSourceFilterLanguages(), SourceFilterLanguage, ARABIC, BENGALI, CHINESE, DUTCH, ENGLISH, FRENCH (+21 more)

### Community 42 - "SiteDownloadPreparation"
Cohesion: 0.19
Nodes (13): IndividualPreparationRecoveryTest, PreparationStatus, mergePreparedQueueRows(), pendingEpisodeRowId(), aggregatePreparationStatus(), batches(), inheritLegacyPreparationState(), parseSiteDownloadPreparation() (+5 more)

### Community 43 - "SleepTimer.kt"
Cohesion: 0.13
Nodes (12): Counting, EndOfEpisode, formatSleepRemaining(), Off, sleepFadeLevel(), SleepTimer, State, rememberSleepRemaining() (+4 more)

### Community 44 - "SiteDownloadSeries"
Cohesion: 0.08
Nodes (8): OnlineDramaHistoryTest, ContinueWatchingRow(), LibraryScreen(), PlaybackHistory, SavedShowsStore, rememberSeriesPlaybackSummary(), SeriesPlaybackSummary, SiteDownloadSeries

### Community 45 - "BoundedUrlTokenRegistry"
Cohesion: 0.14
Nodes (5): BoundedUrlTokenRegistry, ProxyActiveResources, stopProxyResources(), TrackedProxyResources, UrlRegistryCapacityException

### Community 46 - "SecureTokenStore"
Cohesion: 0.13
Nodes (3): legacyTokenKeyForSlot(), SecureTokenStore, SecureTokenStoreSlotTest

### Community 47 - "DownloadStore"
Cohesion: 0.14
Nodes (5): DownloadDao, DownloadDatabase, DownloadStore, PendingOperation, StoredDownload

### Community 48 - "ExistingOutput.kt"
Cohesion: 0.21
Nodes (8): existingOutput(), ExistingOutputIndex, ExistingVideo, existingVideoName(), findExistingOutput(), hasOutputFolderAccess(), isPlausibleExistingVideo(), OutputFolderAccessRequired

### Community 49 - "PreferredAudioLanguage"
Cohesion: 0.10
Nodes (18): PreferredAudioLanguage, ARABIC, AUTOMATIC, BENGALI, CHINESE, ENGLISH, FRENCH, GERMAN (+10 more)

### Community 50 - "SiteDownloadCookieStore"
Cohesion: 0.08
Nodes (13): NativeHeaderContext, DeferredDownloader, MediaLinkExpired, Downloader, normalizeSiteDownloadCookieFile(), siteDownloadCookieHeader(), SiteDownloadCookieStore, validCookiePair() (+5 more)

### Community 51 - "AppIcon"
Cohesion: 0.10
Nodes (21): AppIcon, Back, Bookmark, BookmarkFilled, Browse, Check, CheckCircle, Close (+13 more)

### Community 52 - "SiteMetadataApi.kt"
Cohesion: 0.25
Nodes (18): Document, metadataDurationSeconds(), metadataFromDocument(), metadataFromHtml(), MetadataDetails, MetadataEpisode, metadataEpisode(), MetadataImage (+10 more)

### Community 53 - "build.sh"
Cohesion: 0.21
Nodes (3): LegacyTrustAnchors, build.sh script, usage()

### Community 54 - "PipeModels.kt"
Cohesion: 0.08
Nodes (23): Category, DUB, SUB, EpisodesResult, hasUsableWindow(), playsInWebView(), ProviderData, SkipTimes (+15 more)

### Community 55 - "CastStreamRouting"
Cohesion: 0.15
Nodes (4): CastStreamRoutingTest, CastStreamRouting, ProxyProbeUrls, StreamCorsProxy

### Community 56 - "AutoSkipCountdown.kt"
Cohesion: 0.23
Nodes (5): AutoSkipButtonBounds(), AutoSkipCountdown, autoSkipCountdownLabel(), autoSkipSweep(), runAutoSkipCountdown()

### Community 57 - "AnimepaheDL codebase review"
Cohesion: 0.06
Nodes (31): AnimepaheDL codebase review, Architecture and strengths, P1 — Full-library reconstruction on frequent progress events (P2), P2 — Details availability repeatedly copies/scans records (P2), P3 — Batch source selection scales poorly (P2), P4 — Parallel exporter implementations obscure production behavior (P3), P5 — Persistence schema and orchestration are difficult to evolve (P3), Performance and maintainability (+23 more)

### Community 58 - ".verifyExport"
Cohesion: 0.10
Nodes (11): CacheServerTest, HlsExportTest, TimerTask, TimerTask, CachedHlsMedia, CachedHlsServer, ffmpegRemuxCachedHls(), normalizeCachedByteRanges() (+3 more)

### Community 59 - "ProviderSubtitles.kt"
Cohesion: 0.16
Nodes (4): fetchProviderSubtitleBytes(), normalizeKisskhSubtitles(), providerSubtitleRequest(), ProviderSubtitles

### Community 61 - "SubtitleDelay.kt"
Cohesion: 0.13
Nodes (5): SubtitleDelay, SubtitleDelayRenderer, SubtitleDelayRenderersFactory, SubtitleDelaySeed, subtitleDelaySeed()

### Community 62 - "CastButton"
Cohesion: 0.14
Nodes (6): applyCastTextTrack(), CastButton(), openSystemCastPicker(), ThemedMediaRouteChooserDialogFragment, ThemedMediaRouteControllerDialogFragment, ThemedMediaRouteDialogFactory

### Community 64 - "PopupGlass.kt"
Cohesion: 0.15
Nodes (5): blurLine(), boxBlur(), PopupBackdrop, popupGlass(), rememberPopupBackdrop()

### Community 65 - "VerificationRequired"
Cohesion: 0.16
Nodes (3): PreparationResponsivenessTest, SearchRecoveryTest, AnimeApplication

### Community 66 - "AnimepaheDL resume state"
Cohesion: 0.08
Nodes (24): ADB device test + bookmark feature, AnimepaheDL resume state, Changes in the latest task, Current follow-up release, Current Nginx release artifact (2026-11-11), Custom downloader only, Dedicated details and download-range dialog, Environment and hosting (+16 more)

### Community 67 - "KwikPlaylistDataSource.kt"
Cohesion: 0.14
Nodes (8): hlsIv(), KwikPlaylistDataSource, kwikProgressiveMp4(), rewriteKwikPlaylistIvs(), hostOf(), playlistForeignHosts(), playlistSegmentCensus(), segmentHosts()

### Community 68 - "parseEroticMvMetadataPage"
Cohesion: 0.22
Nodes (8): EroticMvMetadataApi, eroticTaxonomy(), parseEroticMvMetadataPage(), browse(), MetadataBrowseQuery, MetadataPage, metadataPayload(), MetadataSection

### Community 69 - "AnimeRepository.kt"
Cohesion: 0.14
Nodes (7): AnimeRepository, AnimeRepository, AnimeNetwork, HttpFailure, isMediaPath(), toHttpUrlOrNullSafe(), VerificationRequired

### Community 70 - "EroticMvMetadataApi.kt"
Cohesion: 0.25
Nodes (15): absoluteErotic(), cleanEroticDescription(), eroticCard(), eroticDurationSeconds(), eroticImages(), eroticImdb(), eroticRatings(), eroticSiteUrl() (+7 more)

### Community 71 - "Anilili UI reference audit"
Cohesion: 0.12
Nodes (16): Anilili UI reference audit, Automated source comparison, Build/package verification, Complete active shared-component inventory, Download playback-state correction (current source), Downloads, details and settings, Explicit backend adaptations, Home and Browse (+8 more)

### Community 72 - "FlixcloudPlaylistDataSource.kt"
Cohesion: 0.23
Nodes (5): decodeFlixcloudPlaylist(), decodeFlixcloudSegment(), FlixcloudPlaylistDataSource, matchesAt(), unwrapFlixcloudSegmentInPlace()

### Community 74 - "PlayerControlsScaffold"
Cohesion: 0.06
Nodes (28): FocusFillState, SeriesLogo(), BufferedSeekTrack(), controlsSeekAnchorFor(), PlayerActionPill(), PlayerActionRow(), playerActionRowScale(), PlayerBackArrow() (+20 more)

### Community 75 - "MidnightSelectors.kt"
Cohesion: 0.14
Nodes (15): CaptionChoiceRow(), MidnightChoiceRow(), midnightFocus(), MidnightHelper(), midnightInitialFocus(), MidnightRowScaffold(), MidnightSectionLabel(), midnightSelectable() (+7 more)

### Community 76 - "LocalStreamProxy.kt"
Cohesion: 0.16
Nodes (7): dispatchAcceptedSocket(), namedDaemonThreadFactory(), newBoundedProxyRequestExecutor(), ProxyRequest, readLine(), readRequest(), RequestHeaderBudget

### Community 77 - "AppDns"
Cohesion: 0.06
Nodes (13): AppDns, DohProvider, ADGUARD, AUTO, CANADIAN_SHIELD, CLOUDFLARE, CLOUDFLARE_SECURITY, DNS_SB (+5 more)

### Community 78 - "PreferredSubtitleLanguage"
Cohesion: 0.10
Nodes (17): PreferredSubtitleLanguage, ARABIC, AUTOMATIC, CHINESE, ENGLISH, FRENCH, GERMAN, HINDI (+9 more)

### Community 79 - "ErrorReporting.kt"
Cohesion: 0.13
Nodes (10): AnililiException, AnililiFailure, anililiFailure(), asFailure(), classifyTransport(), isCancellation(), raise(), report() (+2 more)

### Community 80 - "developer_notices.py"
Cohesion: 0.06
Nodes (29): isJwtExpired(), jwtLongClaim(), healthHost(), isMegaPlayMedia(), MegaPlayCdnHealth, Callback, MegaPlayCdnHealthInterceptor, MegaPlayHealthState (+21 more)

### Community 82 - "SiteDownloadRepository"
Cohesion: 0.21
Nodes (8): Page, SiteDownloadRepository, chooseMediaVariant(), decodeEroticMedia(), mediaHeight(), MediaVariant, siteDownloadInput(), SiteDownloadRepositoryTest

### Community 83 - "SiteMetadataApi"
Cohesion: 0.11
Nodes (14): MetadataSite, EROTICMV, KISSKH, SiteMetadataApi, SiteMetadataApis, KissKH metadata API, Query/API use, Scope and completeness limits (+6 more)

### Community 84 - "SiteDownloadEpisode"
Cohesion: 0.29
Nodes (5): chronologicalSiteEpisodes(), downloadEpisodeDisplayOrder(), parseSiteDownloadSelection(), SiteDownloadEpisode, SiteDownloadSelectionSafetyTest

### Community 85 - "LocalStreamProxy"
Cohesion: 0.26
Nodes (4): Entry, LocalStreamProxy, RootHeaderContext, ServerEndpoint

### Community 86 - "Anime"
Cohesion: 0.25
Nodes (7): AnimeRepository, Anime, AnimeDetails, AnimeRepository, Episode, text(), PreparationRecord

### Community 88 - "SubtitleSearchLanguage"
Cohesion: 0.11
Nodes (17): SubtitleSearchLanguage, ARABIC, CHINESE, ENGLISH, FOLLOW_DEVICE, FRENCH, GERMAN, HINDI (+9 more)

### Community 89 - "ResolvedMedia"
Cohesion: 0.16
Nodes (9): isHls(), isHlsMimeType(), mediaRequestHeaders(), MediaRequests, normalizedMediaType(), isHlsMedia(), siteVideoMimeType(), ResolvedMedia (+1 more)

### Community 90 - "LibraryTools.kt"
Cohesion: 0.18
Nodes (7): classifyFailure(), diagnostics(), inspectStorage(), QueueOrder, StorageReport, verifyCopy(), OutputStream

### Community 91 - "KisskhCatalog.kt"
Cohesion: 0.32
Nodes (11): detail(), json(), JsonResult, list(), localizeSubtitles(), playbackSubtitles(), Redirect, series() (+3 more)

### Community 92 - "ExternalSubtitleFiles.kt"
Cohesion: 0.18
Nodes (15): ExportedSubtitle, AttachedSubtitle, bomCharset(), decodeSubtitleBytes(), displayName(), downloadOpenSubtitle(), exportedSubtitleMimeType(), extensionFor() (+7 more)

### Community 94 - ".relay"
Cohesion: 0.27
Nodes (4): BodyHead, ProgressOutputStream, readBody(), readHead()

### Community 95 - "AppDeviceProfile.kt"
Cohesion: 0.17
Nodes (3): focusHalo(), focusHighlight(), phoneRailPosterWidth()

### Community 96 - "HlsDownloadSelection.kt"
Cohesion: 0.28
Nodes (6): cachedHlsMediaPlaylists(), CachedHlsPlaylist, readHlsPlaylist(), resetCachedHlsPlaylists(), selectedHlsDownloader(), selectedHlsStreamKeys()

### Community 97 - "PlatformErrorCodes.kt"
Cohesion: 0.17
Nodes (7): causeChain(), platformErrorCode(), platformErrorName(), technicalDetail(), iso3Language(), selectedTextLanguage(), syncableSubtitle()

### Community 98 - ".serve"
Cohesion: 0.15
Nodes (6): InputStream, copyExactly(), parseUnknownTotalRange(), readBytesBounded(), selectCachedHlsVariant(), HlsVariantSelectionTest

### Community 102 - ".onCreate"
Cohesion: 0.06
Nodes (5): CastRouteStatus, PlaybackCacheBypassGate, PlaybackService, PlaybackStatus, AnnouncementDialog()

### Community 103 - "CatalogViewModel.kt"
Cohesion: 0.07
Nodes (20): Cached, catalogCacheJson(), CatalogFailureState, catalogFailureState(), catalogPageForSource(), CatalogSource, EROTICMV, KISSKH (+12 more)

### Community 105 - "Test"
Cohesion: 0.20
Nodes (3): parseByteRange(), SearchVerificationGate, RecoveryPolicyTest

### Community 107 - "MediaCache"
Cohesion: 0.33
Nodes (3): CacheStorage, MediaCache, videoCacheBudget()

### Community 108 - "ThemeAccent"
Cohesion: 0.13
Nodes (13): ThemeAccent, BLOOD_RED, CUSTOM, CYAN, EMERALD, GOLD, MONOCHROME, NEON_GREEN (+5 more)

### Community 109 - "Test"
Cohesion: 0.19
Nodes (3): requireNoHttpUserInfo(), LocalStreamProxyResourceLimitsTest, Callback

### Community 111 - "EpisodeControlPlayer"
Cohesion: 0.14
Nodes (4): applyPreferredAudioLanguage(), applyPreferredSubtitleLanguage(), EpisodeControlPlayer, preferredLanguageCodes()

### Community 112 - "SheetSections"
Cohesion: 0.29
Nodes (11): midnightSliderColors(), PlayerAdjustButton(), PlayerAdjustRow(), PlayerQualityOption, PlayerSettingsSheet(), PlayerSliderIcon(), PlayerSliderRow(), PlayerSliderValue() (+3 more)

### Community 113 - "CatalogScreen"
Cohesion: 0.17
Nodes (3): CatalogScreen(), catalogHomeSections(), WebSocketListener

### Community 114 - "SourceOptions.kt"
Cohesion: 0.33
Nodes (6): audioLabel(), commonSourceOptions(), EpisodeSource, parseEpisodeSources(), selectEpisodeSource(), SourceOptionsTest

### Community 115 - "ErrorDomain"
Cohesion: 0.14
Nodes (14): ErrorDomain, ACCOUNT, APP, BACKUP, DIAGNOSTICS, DOWNLOAD, EXPORT, LINK (+6 more)

### Community 116 - "RemoteDownloadSheet"
Cohesion: 0.19
Nodes (8): Thumbnail(), DownloadSectionLabel(), MetadataTermChips(), RemoteDetailHero(), RemoteDownloadSheet(), SiteDownloadCookiesDialog(), SiteDownloadScreen(), SiteMetadataHighlights()

### Community 117 - "VideoCacheSize"
Cohesion: 0.22
Nodes (7): VideoCacheSize, AUTOMATIC, GB_1, GB_2, MB_256, MB_512, OFF

### Community 118 - "parsePersistedExportRecord"
Cohesion: 0.23
Nodes (10): boolean(), identity(), media(), nonnegativeLong(), objectOrNull(), parsePersistedExportRecord(), string(), stringOrNull() (+2 more)

### Community 119 - "ResultSort"
Cohesion: 0.21
Nodes (8): findMediaUrl(), ResultSort, NEWEST, OLDEST, RELEVANCE, TITLE, sortResults(), AnimeRepositoryTest

### Community 121 - "GestureZone"
Cohesion: 0.22
Nodes (9): GestureIndicatorSide, gestureIndicatorSide(), Left, Right, GestureZone, Brightness, Volume, playerGestureZone() (+1 more)

### Community 122 - "AnnouncementSource.kt"
Cohesion: 0.35
Nodes (7): Announcement, announcementApplies(), AnnouncementFetchResult, AnnouncementSource, compareAppVersions(), parseAnnouncement(), sanitizeAnnouncementUrl()

### Community 123 - "Page"
Cohesion: 0.20
Nodes (11): AiringCountEntry, AiringCountMedia, AiringCountPage, GqlAiringCountsResponse, GqlHomeCollectionsResponse, GqlScheduleResponse, HomeCollectionsData, Page (+3 more)

### Community 128 - "SkipTimingStatus"
Cohesion: 0.15
Nodes (12): SkipReportState, FAILED, IDLE, SENDING, SENT, SkipTimingStatus, ANISKIP, CHECKING (+4 more)

### Community 129 - "TvPlayerControl"
Cohesion: 0.15
Nodes (13): TvPlayerControl, CAPTIONS, EPISODES, FORWARD, FULLSCREEN, MUTE, MY_LIST, NEXT (+5 more)

### Community 131 - "BackdropBlur.kt"
Cohesion: 0.16
Nodes (9): GlassSection, BROWSE, DETAILS, HOME, NAVIGATION, POPUPS, REFRESH, backdropBlur() (+1 more)

### Community 132 - ".isChallenge"
Cohesion: 0.27
Nodes (3): SessionChecks, verificationOriginMatches(), SessionChecksTest

### Community 134 - "TvFocusTarget.kt"
Cohesion: 0.15
Nodes (4): rememberTvFocusTarget(), TvFocusTarget, TvFocusTargetElement, TvFocusTargetNode

### Community 136 - "PlaybackBufferSize"
Cohesion: 0.25
Nodes (6): PlaybackBufferSize, AUTOMATIC, LARGE, MEDIUM, SMALL, VERY_LARGE

### Community 138 - "c"
Cohesion: 0.33
Nodes (4): deadLoadExhausted(), isStreamRefusal(), refusalEndsStream(), StreamRefusalLoadErrorPolicy

### Community 139 - "DpadLongPress.kt"
Cohesion: 0.29
Nodes (5): dpadClickable(), DpadPress, CLICK, LONG_CLICK, DpadPressTracker

### Community 141 - "ReferencePlaybackRegistry"
Cohesion: 0.13
Nodes (5): HostPlaybackSnapshot, Binding, ReferencePlaybackRegistry, close(), select()

### Community 142 - "FailureReason"
Cohesion: 0.22
Nodes (9): FailureReason, DELETION, EXPIRED_SOURCE, FOLDER_ACCESS, INCOMPATIBLE_EXPORT, NETWORK, STORAGE_FULL, UNKNOWN (+1 more)

### Community 144 - "TransferState"
Cohesion: 0.22
Nodes (9): TransferState, COMPLETE, DOWNLOADING, FAILED, PAUSED, QUEUED, REMOVING, RESOLVING (+1 more)

### Community 146 - "historyAfterRemoteProgress"
Cohesion: 0.46
Nodes (3): historyAfterRemoteProgress(), remoteRowEpisode(), RemoteProgressMergeTest

### Community 147 - "AppGraph"
Cohesion: 0.18
Nodes (7): AppGraph, Profiles, File inventory, Necessary host adaptations, Original Anilili player port, Player-port release verification (2026-11-08), Review checklist

### Community 150 - "UiScale"
Cohesion: 0.33
Nodes (4): UiScale, LARGE, MEDIUM, SMALL

### Community 151 - "AppBackdropDim"
Cohesion: 0.33
Nodes (4): AppBackdropDim, LIGHT, MEDIUM, STRONG

### Community 153 - "TransferState"
Cohesion: 0.27
Nodes (6): DownloadGroupProgress, downloadGroupProgress(), Availability, SAVED, STAGING, DownloadGroupProgressTest

### Community 154 - "Real-device verification"
Cohesion: 0.18
Nodes (10): Current pending release: player controls and download playback state, Final saved-shows verification (cheaper subagent), Found and fixed, Library and Settings/announcements release, Notice failure follow-up and Korean-only KissKH, Offline-preferred playback (pending device verification), Player-port release verification (2026-11-08), Real-device verification (+2 more)

### Community 155 - "ThemeContrast"
Cohesion: 0.33
Nodes (4): ThemeContrast, HIGH, MEDIUM, STANDARD

### Community 156 - "ErrorAction"
Cohesion: 0.20
Nodes (9): ErrorAction, CHANGE_SERVER, CHOOSE_FILE, FREE_SPACE, OPEN_SETTINGS, RETRY, RETRY_EXPORT, SEND_DIAGNOSTICS (+1 more)

### Community 157 - "HlsPlaylistRewrite.kt"
Cohesion: 0.42
Nodes (7): isDeclaredHlsPlaylist(), looksLikeHlsPlaylist(), replaceUri(), resolveAndRegister(), rewriteHlsPlaylist(), rewriteLine(), rewriteUriAttribute()

### Community 158 - "NextEpisodeTrigger"
Cohesion: 0.20
Nodes (10): EmbedAutoAdvanceStep, embedAutoAdvanceStep(), ADVANCE, ARM, WAIT, NextEpisodeTrigger, nextEpisodeTrigger(), AFTER_CREDITS (+2 more)

### Community 159 - "RelatedLayout"
Cohesion: 0.33
Nodes (3): RelatedLayout, LIST, TITLES

### Community 160 - "SavedFilesWatcher.kt"
Cohesion: 0.23
Nodes (3): missingSavedFiles(), querySavedFiles(), SavedFilesWatcher

### Community 161 - "TvNativeTextField"
Cohesion: 0.25
Nodes (3): disableSelectionActionMode(), moveNativeFocusSafely(), TvNativeTextField()

### Community 162 - "PanelHeader"
Cohesion: 0.32
Nodes (6): PlayerAccentScope(), PlayerPanelTheme(), PanelHeader(), PlayerModalSheet(), PlayerOptionsSheet(), PlayerSidePanel()

### Community 164 - "Browse, play and download"
Cohesion: 0.20
Nodes (9): Anilili Movies, Browse, play and download, Build, Downloads, Library and recovery tools, Release checks, Sessions, Supported sources (+1 more)

### Community 165 - "ThemePreset"
Cohesion: 0.17
Nodes (12): ThemePreset, CRIMSON_NIGHT, CYBER, DEEP_SEA, EMBER, FOREST, GILDED, HIGH_CONTRAST (+4 more)

### Community 166 - "TvPlayerBackTarget"
Cohesion: 0.22
Nodes (9): TvPlayerBackTarget, CAPTION_APPEARANCE, CAPTIONS, tvPlayerBackTarget(), CONTROLS, EPISODE_DRAWER, NONE, OPEN_SUBTITLES (+1 more)

### Community 169 - "UiState.kt"
Cohesion: 0.33
Nodes (5): Error, Loading, rethrowIfCancellation(), Success, UiState

### Community 170 - "decodeRelayFrame"
Cohesion: 0.32
Nodes (5): decodeRelayFrame(), EndOfStoredEvents, Event, RelayFrame, RelayFrameDecision

### Community 173 - "DownloadQuality"
Cohesion: 0.25
Nodes (6): DownloadQuality, BEST, P1080, P360, P480, P720

### Community 174 - "PlayerAspectRatio"
Cohesion: 0.25
Nodes (6): PlayerAspectRatio, ORIGINAL, RATIO_16_9, RATIO_21_9, RATIO_4_3, SQUARE

### Community 176 - "WatchedFilter"
Cohesion: 0.29
Nodes (5): DiscoverFilters, WatchedFilter, COMPLETED, OFF, STARTED

### Community 177 - "IOException"
Cohesion: 0.15
Nodes (4): artworkRecoveryHostPolicy(), isAllowedArtworkRecoveryUrl(), ArtworkRecoveryPolicyTest, CancellableHttpTest

### Community 178 - ".event"
Cohesion: 0.07
Nodes (17): DebugTrace, reconcileDirectEnqueues(), DownloadCompleteReceiver, Listener, SessionBrowser(), WebViewClient, R10 — Cancellation deadlines are not hard limits on all provider/native operations (P2, risk requiring device/provider reproduction), R1 — Failed persistence writes leave authoritative memory ahead of disk (P1, high confidence) (+9 more)

### Community 179 - "FfmpegProgressParser"
Cohesion: 0.19
Nodes (5): isExportDurationComplete(), FfmpegProgress, FfmpegProgressParser, ExportValidationTest, FfmpegProgressTest

### Community 182 - "Anilili feature audit checklist"
Cohesion: 0.25
Nodes (7): Anilili feature audit checklist, Artwork and reliability, Download management, Playback and personal library, Release gate, Settings and appearance from Anilili, Source, search, and metadata

### Community 186 - "downloadLocation"
Cohesion: 0.29
Nodes (7): DownloadLocation, downloadLocation(), episodeTitle(), outputLocation(), safeFileName(), audioTag(), DownloadNamingTest

### Community 188 - "HlsSizeEstimator.kt"
Cohesion: 0.26
Nodes (3): HlsSizeEstimator, Segment, Track

### Community 193 - "SafeDeletion.kt"
Cohesion: 0.33
Nodes (5): findOutputSubtitleSidecars(), isOutputSubtitleSidecar(), isPlayableOutputSubtitle(), localSubtitleUriExists(), outputSubtitleTrack()

### Community 194 - "ThemeSettings.kt"
Cohesion: 0.22
Nodes (7): accentSeedOf(), AppBackdropSource, ANIME, IMAGE, NONE, backdropUrlOf(), ProfileLook

### Community 197 - "PosterElement"
Cohesion: 0.20
Nodes (9): encodeHiddenPosterElements(), PosterElement, ADULT, DETAILS, DUB, NEW_EPISODES, PROGRESS, SCORE (+1 more)

### Community 206 - "TimeOfDay.kt"
Cohesion: 0.09
Nodes (13): ClockFormat, SYSTEM, TWELVE_HOUR, TWENTY_FOUR_HOUR, rememberSystemUses24Hour(), ContentObserver, timeOfDayFormatter(), PlayerBars (+5 more)

### Community 207 - "Theme.kt"
Cohesion: 0.52
Nodes (4): appColors(), ThemeSpec, wallpaperSeed(), AnimepaheDLTheme()

### Community 211 - "Dramatip metadata API"
Cohesion: 0.40
Nodes (4): Dramatip metadata API, Live evidence and limits, Public surfaces and methods, Verified fields retained

### Community 218 - "parseEroticMvMetadataFeed"
Cohesion: 0.22
Nodes (6): parseEroticMvMetadataFeed(), EroticMvMetadataFixtureTest, Endpoints and operations, EroticMV metadata API, Fields retained, Live evidence and limitations

### Community 231 - "ReferenceChromeTokens.kt"
Cohesion: 0.43
Nodes (3): appCanvasFade(), drawCanvasFade(), Midnight

### Community 232 - "DownloadAction"
Cohesion: 0.29
Nodes (7): DownloadAction, DELETE, PAUSE, RESUME, RETRY, RETRY_SAVE, VERIFY

### Community 233 - "DefaultQuality"
Cohesion: 0.18
Nodes (8): DefaultQuality, AUTO, HIGHEST, P1080, P360, P480, P720, dataSaverFallbackMessage()

### Community 234 - "DownloadRow"
Cohesion: 0.36
Nodes (5): downloadGroupArtwork(), DownloadLibrarySnapshot, downloadLibrarySnapshot(), DownloadRow, DownloadGroupArtworkTest

### Community 235 - "SaveState"
Cohesion: 0.29
Nodes (7): SaveState, CHECKING, COPYING, FAILED, NONE, PREPARING, WAITING

### Community 237 - "Viewer"
Cohesion: 0.33
Nodes (6): AnimeStat, GqlViewerResponse, UserAvatar, Viewer, ViewerData, ViewerStatistics

### Community 254 - "EroticMvMetadataApi"
Cohesion: 0.16
Nodes (8): MetadataHttp, MetadataResponse, Absent or conditional information, Browse and filtering, Coverage and retained data, Kiss18 public metadata API, Sources actually visited, Verification

### Community 255 - "TvEpisodeChangeFocus"
Cohesion: 0.32
Nodes (8): TvEpisodeChangeFocus, tvEpisodeChangeFocus(), CONTROLS, HOLD, SURFACE, UNCHANGED, tvFocusLostTarget(), tvOverlayDismissFocus()

### Community 256 - "CaptionSections"
Cohesion: 0.47
Nodes (6): CaptionSections(), formatSubtitleDelay(), PlayerCaptionsSheet(), PlayerSignsToggle, SubtitleSyncChoice, SubtitleSyncState

### Community 257 - "Test"
Cohesion: 0.36
Nodes (3): subtitleSidecarName(), deleteEmptyDirectoriesUpTo(), SafeDeletionTest

### Community 259 - "ThemeStyle"
Cohesion: 0.29
Nodes (5): ThemeStyle, NEUTRAL, TONAL, VIBRANT, VIVID

### Community 262 - "Kind"
Cohesion: 0.40
Nodes (5): AppNotification, Kind, AIRING, MEDIA, SOCIAL

### Community 264 - "TvTextInputType"
Cohesion: 0.40
Nodes (5): TvTextInputType, EMAIL, NUMBER, PASSWORD, TEXT

### Community 266 - "Narto Drama public metadata API"
Cohesion: 0.40
Nodes (4): API methods and routing, Live audit evidence, Narto Drama public metadata API, Not supplied / not automatically fetched

### Community 268 - "UI parity and usability checklist"
Cohesion: 0.40
Nodes (4): Corrections made in this pass, Screen and component review, UI parity and usability checklist, Verification still needed

### Community 279 - "PlayerSurface"
Cohesion: 0.04
Nodes (40): PlayerContentScale, CROP, FILL, FIT, rememberScreenReaderActive(), CompiledSeparately(), Routes, shouldShowManualSkipButton() (+32 more)

### Community 288 - "DownloadRepository.kt"
Cohesion: 0.09
Nodes (17): DownloadRepository, isHlsTransfer(), pause(), readDownloadRows(), refresh(), resume(), byteProgress(), displayTransferTotal() (+9 more)

### Community 290 - "ThemeBackground"
Cohesion: 0.33
Nodes (3): ThemeBackground, AMOLED, DARK

### Community 300 - "AnnouncementManager.kt"
Cohesion: 0.06
Nodes (6): LocalStreamProxyService, AnnouncementManager, AnnouncementScheduler, AnnouncementWorker, ForegroundCheck, NotificationPreferences()

### Community 306 - "MenuLanguage"
Cohesion: 0.29
Nodes (4): MenuLanguage, ENGLISH, SPANISH, SYSTEM

## Knowledge Gaps
- **480 isolated node(s):** `QUEUED`, `RESOLVING`, `DOWNLOADING`, `PAUSED`, `WAITING_NETWORK` (+475 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 1306 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **138 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `SettingsStore` connect `SettingsStore` to `LibraryScreens.kt`, `BackdropBlur.kt`, `InPlayerEpisodeList.kt`, `ThemeStyle`, `PlayerSurface.kt`, `TvPlayerControls.kt`, `PlaybackBufferSize`, `NostrUpdateSource.kt`, `CaptionStyle`, `PlayerSettingsSheet.kt`, `OpenSubtitlesPanel.kt`, `AppBackdrop.kt`, `DiagnosticsLog`, `AppDeviceProfile.kt`, `MediaCache.kt`, `UiScale`, `PlayerSurface`, `AppBackdropDim`, `CaptionAppearance.kt`, `SettingsStore.kt`, `ThemeContrast`, `RelatedLayout`, `ThemeBackground`, `SubtitleCueLayout.kt`, `SourceFilterLanguage`, `DownloadQuality`, `PlayerAspectRatio`, `WatchedFilter`, `PreferredAudioLanguage`, `MenuLanguage`, `PipeModels.kt`, `OpenSubtitlesAuthManager.kt`, `ThemeSettings.kt`, `PosterElement`, `AppDns`, `TimeOfDay.kt`, `PreferredSubtitleLanguage`, `developer_notices.py`, `Theme.kt`, `AppDeviceProfile.kt`, `DefaultQuality`, `ThemeAccent`, `VideoCacheSize`?**
  _High betweenness centrality (0.080) - this node is a cross-community bridge._
- **Are the 55 inferred relationships involving `PlayerSurface()` (e.g. with `.launch()` and `AutoSkipButtonBounds()`) actually correct?**
  _`PlayerSurface()` has 55 INFERRED edges - model-reasoned connections that need verification._
- **What connects `QUEUED`, `RESOLVING`, `DOWNLOADING` to the rest of the system?**
  _480 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `AnililiError` be split into smaller, more focused modules?**
  _Cohesion score 0.02 - nodes in this community are weakly interconnected._
- **Why does `DiagnosticsLog` connect `DiagnosticsLog` to `PlayerSurface.kt`, `PlayerSettingsSheet.kt`, `AppBackdrop.kt`, `MediaCache.kt`, `CastRoutes.kt`, `CaptionAppearance.kt`, `PlaybackBuffer.kt`, `SettingsStore.kt`, `ExternalPlayer.kt`, `SleepTimer.kt`, `AnnouncementManager.kt`, `build.sh`, `OpenSubtitlesAuthManager.kt`, `CastButton`, `KwikPlaylistDataSource.kt`, `FlixcloudPlaylistDataSource.kt`, `PlaybackDiagnostics.kt`, `LocalStreamProxy.kt`, `AppDns`, `ErrorReporting.kt`, `developer_notices.py`, `ArtworkBitmapLoader.kt`, `UniqueStreamKeyDataSource`?**
  _High betweenness centrality (0.070) - this node is a cross-community bridge._
- **Should `LibraryScreens.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.048903225806451615 - nodes in this community are weakly interconnected._
- **Why does `PlayerSurface()` connect `PlayerSurface` to `CaptionSections`, `LibraryScreens.kt`, `SkipTimingStatus`, `InPlayerEpisodeList.kt`, `TvPlayerControls.kt`, `PlayerSurface.kt`, `SettingsStore`, `SubtitleItem`, `Listener`, `PlayerSettingsSheet.kt`, `OpenSubtitlesPanel.kt`, `PlayerGestureControls`, `OfflineDownloadService.kt`, `CaptionAppearance.kt`, `SettingsStore.kt`, `NextEpisodeTrigger`, `MediaVolumeSlider`, `ui/components/WaterFillLogoIndicator.kt`, `PanelHeader`, `SubtitleTrackMerge.kt`, `MidnightDialog.kt`, `ExternalPlayer.kt`, `LibraryModels.kt`, `TvPlayerBackTarget`, `SleepTimer.kt`, `AnnouncementManager.kt`, `PipeModels.kt`, `AutoSkipCountdown.kt`, `SubtitleDelay.kt`, `CastButton`, `EroticMvMetadataApi.kt`, `PlayerControlsScaffold`, `MidnightSelectors.kt`, `ExternalSubtitleFiles.kt`, `PlatformErrorCodes.kt`, `.onCreate`, `DefaultQuality`, `EpisodeControlPlayer`, `SheetSections`, `TvEpisodeChangeFocus`?**
  _High betweenness centrality (0.058) - this node is a cross-community bridge._