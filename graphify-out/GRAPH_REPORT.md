# Graph Report - AnililiMovies  (2026-11-18)

## Corpus Check
- 322 files · ~284,138 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 38 file(s) not represented in the graph (top: .xml 22, (none) 3, .ttf 3)

## Summary
- 4926 nodes · 11883 edges · 311 communities (170 shown, 141 thin omitted)
- Extraction: 94% EXTRACTED · 6% INFERRED · 0% AMBIGUOUS · INFERRED: 770 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `41701c97`
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
- CaptionAppearance.kt
- AppSymbol
- AppBackdrop.kt
- TvTextField.kt
- .all
- AppDeviceProfile.kt
- MediaCache.kt
- CastRoutes.kt
- MidnightSettings.kt
- SiteDownloadSources.kt
- DownloaderViewModel.kt
- findActivity
- OfflineDownloadService.kt
- PlayerScreenLock.kt
- PlaybackBuffer.kt
- SettingsStore.kt
- PlaybackService.kt
- LibraryNavigationTest
- SearchScreenTest.kt
- PlayerGestureControls.kt
- KisskhResilienceTest.kt
- ui/components/WaterFillLogoIndicator.kt
- FloatingNavigationBar.kt
- SubtitleTrackMerge.kt
- Composable
- OpenSubtitlesPanel.kt
- ExternalPlaybackReturns.kt
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
- SessionBrowser.kt
- SiteMetadataApi.kt
- build.sh
- SourceVariant
- CastStreamRouting
- OpenSubtitlesClient.kt
- AnimepaheDL codebase review
- .verifyExport
- MidnightHelper
- IOException
- SubtitleDelay.kt
- CastButton
- KisskhResilienceTest
- ExternalPlayer.kt
- VerificationRequired
- AnimepaheDL resume state
- KwikPlaylistDataSource.kt
- parseEroticMvMetadataPage
- .onPlayerError
- EroticMvMetadataApi.kt
- Anilili UI reference audit
- PlayerControlsScaffold
- PlaybackDiagnostics.kt
- OpenSubtitlesPanel.kt
- PlayerSettingsSheet.kt
- LocalStreamProxy.kt
- AppDns
- PreferredSubtitleLanguage
- ErrorReporting.kt
- developer_notices.py
- FfmpegRemux.kt
- SiteDownloadRepository
- SiteMetadataApi
- SiteDownloadEpisode
- LocalStreamProxy
- AnimeRepository.kt
- HlsDownloadSelection.kt
- SubtitleSearchLanguage
- ResolvedMedia
- LibraryTools.kt
- KisskhCatalog.kt
- ExternalSubtitleFiles.kt
- ArtworkRecovery
- .relay
- LocalStreamProxyService.kt
- ErrorNotice.kt
- c
- CatalogScreen
- NetworkIdentity.kt
- SiteDownloadSourcesTest
- .onCreate
- CatalogViewModel.kt
- DiagnosticsLog
- Test
- AnnouncementSource.kt
- ExternalPlayerTargets.kt
- ThemeAccent
- Test
- Listener
- EpisodeControlPlayer
- AppIcon
- AnnouncementManager.kt
- SourceOptions.kt
- ErrorDomain
- RemoteDownloadSheet
- SiteDownloadProvider
- parsePersistedExportRecord
- MidnightSettingSwitchRow
- Test
- BackdropBlur.kt
- CatalogHero
- isExportDurationComplete
- AppColorScheme.kt
- LocalStreamProxyHeaderPolicyTest
- ForwardingPlayer
- ThreadTraceListener.java
- SkipTimingStatus
- TvPlayerControl
- GlassSection
- .isChallenge
- DownloadNotifications.kt
- TvFocusTarget.kt
- SubtitleItem
- MediaComponents.kt
- c
- DpadLongPress.kt
- ClockFormat
- ReferencePlaybackRegistry
- LibraryStore
- PlayerSleepTimer.kt
- PopupGlass.kt
- EpisodeDownloads
- AppGraph
- UiScale
- AppBackdropDim
- PlayerSeriesLogo.kt
- downloadGroupProgress
- Real-device verification
- ThemeContrast
- ErrorAction
- HlsPlaylistRewrite.kt
- NextEpisodeTrigger
- CatalogPosterCard
- SavedFilesWatcher.kt
- DiscordStatus
- PlayerLayoutMetrics.kt
- AnnouncementInputBoundsTest
- Browse, play and download
- ThemePreset
- TvPlayerBackTarget
- DeletionTestProvider.java
- ExampleInstrumentedTest.kt
- KissKH metadata API
- PlaylistForeignSegments.kt
- ProviderBenchmark
- CompiledSeparately
- DownloadQuality
- PlayerAspectRatio
- .intercept
- DesignSystem.kt
- .event
- FfmpegProgressParser
- OrderedWrites
- Anilili feature audit checklist
- downloadLocation
- PlayerSystemBars.kt
- Security and privacy findings
- backdropBlur
- StreamItem
- SafeDeletion.kt
- ThemeSettings.kt
- PlayerControlIconButton
- PosterElement
- Intent
- ExistingOutputTest.kt
- SeriesLogo.kt
- subtitleReprepareState
- .event
- ReferenceChromeTokens.kt
- Dramatip metadata API
- ExampleUnitTest.kt
- PlayerSeekMarkers.kt
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
- TransferState
- DefaultQuality
- DownloadRow
- .operation
- EroticMvMetadataApi
- TvEpisodeChangeFocus
- Test
- ThemeStyle
- SessionBrowser
- DownloadAction
- SaveState
- WebSocketListener
- Review implementation status
- DiagnosticsLog
- UI and usability findings
- PlayerSurface
- DownloadRepository.kt
- ThemeBackground
- .prepareItem
- TransferListener
- .legacyIdentity
- Test
- Callback
- MenuLanguage
- Player
- Narto Drama public metadata API
- UI parity and usability checklist
- PlayerSettingsSheet.kt
- ExportedSubtitle

## God Nodes (most connected - your core abstractions)
1. `SettingsStore` - 155 edges
2. `PlayerSurface()` - 135 edges
3. `AnililiError` - 114 edges
4. `PlaybackService` - 46 edges
5. `rememberFocusFill()` - 44 edges
6. `SiteDownloadSeries` - 36 edges
7. `ExportRecord` - 33 edges
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
- `Necessary host adaptations` --references--> `ProviderCatalog`  [INFERRED]
  docs/REFERENCE_PLAYER_PORT.md → app/src/main/java/com/anilili/data/ProviderCatalog.kt
- `Verification still needed` --references--> `DownloadGroupArtworkTest`  [INFERRED]
  UI_PARITY_CHECKLIST.md → app/src/test/java/com/example/animepahedl/DownloadGroupArtworkTest.kt

## Import Cycles
- None detected.

## Communities (311 total, 141 thin omitted)

### Community 0 - "AnililiError"
Cohesion: 0.02
Nodes (98): AnililiError, ACCOUNT_LIBRARY_LOAD_FAILED, ACCOUNT_LIST_ENTRY_DELETE_FAILED, ACCOUNT_LIST_UPDATE_FAILED, ACCOUNT_LOGIN_ABANDONED, ACCOUNT_LOGIN_PAGE_FAILED, ACCOUNT_PROFILE_LOAD_FAILED, ACCOUNT_PROGRESS_UPDATE_FAILED (+90 more)

### Community 1 - "LibraryScreens.kt"
Cohesion: 0.07
Nodes (8): ReferenceSettingsIndex(), SettingsSectionCard(), SettingsSectionTile, onPause(), onResume(), onStart(), onStop(), openFromIntent()

### Community 2 - "AniListModels.kt"
Cohesion: 0.06
Nodes (78): AiringCountEntry, AiringCountMedia, AiringCountPage, AiringDayCount, AiringSchedule, AnimeStat, AppNotification, CoverImage (+70 more)

### Community 3 - "AudioOutputFormat.kt"
Cohesion: 0.14
Nodes (7): AudioOutputFormat, AudioOutputMonitor, AudioRoute, audioRoute(), DECODED, PASSTHROUGH, Listener

### Community 4 - "InPlayerEpisodeList.kt"
Cohesion: 0.06
Nodes (38): EpisodeItem, EpisodeLayout, GRID, LIST, EpisodeOrder, NEWEST_FIRST, OLDEST_FIRST, rememberFocusFill() (+30 more)

### Community 5 - "TvPlayerControls.kt"
Cohesion: 0.07
Nodes (10): bufferedFraction(), playerTypography(), formatTvPlayerTime(), TvControlButton(), TvPlayerBackArrow(), TvPlayerControls(), tvPointerSeekMs(), tvScrubStepMs() (+2 more)

### Community 6 - "PlayerSurface.kt"
Cohesion: 0.05
Nodes (21): adaptiveVideoHeightCeiling(), applyTextTrack(), applyVideoHeight(), applyVideoHeightCeiling(), availableVideoHeights(), CaptionDefaults, captionDefaultsAfterChoice(), categoryAudioRank() (+13 more)

### Community 8 - "SubtitleItem"
Cohesion: 0.39
Nodes (10): loadSubtitleCueTimes(), loadSubtitleText(), nearestSubtitleCues(), parseArrowCueTimes(), parseAssCueTimes(), parseSubtitleCueTimes(), parseTimestampMs(), stripMarkup() (+2 more)

### Community 9 - "NostrUpdateSource.kt"
Cohesion: 0.16
Nodes (11): decodeRelayFrame(), EndOfStoredEvents, Event, NostrEvent, NostrUpdateSource, RelayFrame, RelayFrameDecision, RelayFrameGate (+3 more)

### Community 10 - "CaptionStyle"
Cohesion: 0.05
Nodes (29): CaptionBackgroundColor, BLACK, GRAY, NAVY, WHITE, CaptionEdgeStyle, DROP_SHADOW, NONE (+21 more)

### Community 11 - "Listener"
Cohesion: 0.12
Nodes (5): contentAspectRatio(), diagnosticSummary(), Listener, preferredAudioTrackCodes(), stateName()

### Community 12 - "CaptionAppearance.kt"
Cohesion: 0.06
Nodes (6): captionOffset(), rememberSystemUses24Hour(), ContentObserver, BubbleSpec, rememberTimedSkipPopup(), BubbleSpec

### Community 13 - "AppSymbol"
Cohesion: 0.12
Nodes (18): AppSymbol(), DestinationSummary(), DownloadReferenceEpisode, DownloadReferenceEpisodeRow(), DownloadReferenceSeries, DownloadReferenceSeriesCard(), ReferenceCornerBadge(), WatchProgressBar() (+10 more)

### Community 14 - "AppBackdrop.kt"
Cohesion: 0.11
Nodes (9): AppBackdrop, appCanvasFade(), canvasColor(), centreCrop(), drawAppCanvas(), drawBackdropSlice(), hasAppBackdrop(), ProvideAppBackdrop() (+1 more)

### Community 15 - "TvTextField.kt"
Cohesion: 0.07
Nodes (14): beginNativeTvEditing(), disableSelectionActionMode(), Callback, hideNativeTvKeyboard(), isImeVisible(), moveNativeFocusSafely(), NativeTvTextWatcher, SafeTvEditText (+6 more)

### Community 16 - ".all"
Cohesion: 0.07
Nodes (26): DeletionRecoveryTest, consumeCancellable(), DownloadIdentity, matchesQueuedVideo(), adoptExistingVideo(), clearPendingVideo(), reconcileExistingVideos(), DownloadCompleteReceiver (+18 more)

### Community 17 - "AppDeviceProfile.kt"
Cohesion: 0.08
Nodes (28): isTelevision(), readTelevisionSignals(), TelevisionSignal, televisionSignal(), CHOSEN, LEANBACK_FEATURE, LEANBACK_LAUNCHER, NO_TOUCHSCREEN (+20 more)

### Community 18 - "MediaCache.kt"
Cohesion: 0.09
Nodes (12): VideoCacheSize, AUTOMATIC, GB_1, GB_2, MB_256, MB_512, OFF, ExoDatabase (+4 more)

### Community 19 - "CastRoutes.kt"
Cohesion: 0.06
Nodes (7): CastRoute, CastRoutes, GoogleCastRoute, SessionAvailabilityListener, mediaClientOrNull(), shouldGiveUpOnCastLoad(), MiruroCastMediaItemConverter

### Community 20 - "MidnightSettings.kt"
Cohesion: 0.07
Nodes (5): AppUiScale(), UnscaledUi(), EqualizerWaveIndicator(), MidnightSettingSpotlight, PlayerAccentScope()

### Community 21 - "SiteDownloadSources.kt"
Cohesion: 0.14
Nodes (18): toAppMetadata(), chooseMediaVariant(), EroticVideoSource, eroticVideoSource(), jsonAfterMarker(), jsonArrayOrEmpty(), jsonInt(), jsonString() (+10 more)

### Community 22 - "DownloaderViewModel.kt"
Cohesion: 0.11
Nodes (7): copyCancellable(), loadBookmarks(), toggleBookmark(), details(), Index, kObjects(), kString()

### Community 24 - "OfflineDownloadService.kt"
Cohesion: 0.08
Nodes (10): DownloadRetryService, forResolvedMedia(), isExpiredMediaResponse(), isTransientTransferHttpStatus(), MediaVerification, mediaVerificationEvents(), OfflineDownloads, Listener (+2 more)

### Community 25 - "PlayerScreenLock.kt"
Cohesion: 0.07
Nodes (9): EpisodeArtwork(), NextEpisodeAction(), NextEpisodeCard(), PlayerFillerSkip, PlayerFillerTag(), PlayerPauseOverlay(), PlayerLockButton(), PlayerLockOverlay() (+1 more)

### Community 26 - "PlaybackBuffer.kt"
Cohesion: 0.10
Nodes (11): PlaybackBufferSize, AUTOMATIC, LARGE, MEDIUM, SMALL, VERY_LARGE, AdjustableLoadControl, playbackBufferBytes() (+3 more)

### Community 27 - "SettingsStore.kt"
Cohesion: 0.08
Nodes (12): coercePlaybackSpeed(), coercePlayerBrightness(), decodeHiddenPosterElements(), decodePersistentCaptionDelays(), decodeSourceFilterLanguages(), defaultQualityForDevice(), deviceDefaultQuality(), DownloadDestination (+4 more)

### Community 28 - "PlaybackService.kt"
Cohesion: 0.07
Nodes (5): CastRouteStatus, HostPlaybackSnapshot, PlaybackService, close(), select()

### Community 29 - "LibraryNavigationTest"
Cohesion: 0.10
Nodes (8): DownloadSelectionTest, LibraryNavigationTest, AppSettings, normalizeOutputFolder(), downloadsPendingIntent(), OutputFolderTest, Device verification, FFmpeg

### Community 30 - "SearchScreenTest.kt"
Cohesion: 0.06
Nodes (27): SearchScreenTest, 10. Collection summary gives count text precedence over its heading — P2, layout risk, 11. Text-heavy dialogs lack explicit scrolling — P2, layout risk, 12. Image-error copy does not adapt to thumbnail size — P3, layout risk, 13. Verification banner's View action can leave the user on Settings/About — P2, confirmed, 14. Search, empty states, and error treatments vary without shared components — P2, confirmed design divergence, 15. Completion and destructive-action terminology is inconsistent — P2, confirmed, 16. Spacing, chevrons, and text hierarchy are locally defined — P3, confirmed design divergence (+19 more)

### Community 31 - "PlayerGestureControls.kt"
Cohesion: 0.06
Nodes (39): formatPlayerTime(), applyBrightness(), applyVolume(), Brightness, canSlideSeek(), canStartHoldSpeed(), GestureDragAxis, Horizontal (+31 more)

### Community 33 - "ui/components/WaterFillLogoIndicator.kt"
Cohesion: 0.08
Nodes (19): silhouetteFrame(), silhouetteMask(), cappedFrameSteps(), DetailedWaterFillLogo(), liquidLevel(), LoopHandover, PlainWaterFillLogo(), rememberLiquidClock() (+11 more)

### Community 34 - "FloatingNavigationBar.kt"
Cohesion: 0.09
Nodes (11): AutoSkipButtonBounds(), autoSkipSweep(), chromeFade(), DownloaderNavigation(), barBlur(), barGlass(), barRim(), FloatingNavDisc() (+3 more)

### Community 35 - "SubtitleTrackMerge.kt"
Cohesion: 0.13
Nodes (34): assDialogueLine(), AssDocument, assPlayRes(), assRecord(), assStyleLine(), assTime(), buildSignsMerge(), decodeEntities() (+26 more)

### Community 36 - "Composable"
Cohesion: 0.12
Nodes (26): CaptionAppearanceDialog(), CoverScreenWithDialogWindow(), MidnightBodyTopStop(), MidnightConfirmDialog(), MidnightConnectionStatus(), MidnightDialogFooter(), MidnightDialogFrame(), MidnightDialogHost() (+18 more)

### Community 37 - "OpenSubtitlesPanel.kt"
Cohesion: 0.13
Nodes (5): defaultOpenSubtitlesLanguages(), openSubtitlesEpisodeField(), OpenSubtitlesPanel(), SubtitleResultRow(), PageSearchField()

### Community 38 - "ExternalPlaybackReturns.kt"
Cohesion: 0.22
Nodes (6): ExternalPlaybackOutcome, externalPlaybackOutcome(), ExternalPlaybackReturns, ExternalPlaybackSession, shouldSyncAniListProgress(), withExportProgressDeadline()

### Community 39 - "LibraryModels.kt"
Cohesion: 0.11
Nodes (26): capResumePoints(), DismissedRemoteRow, episodeKey(), EpisodeResume, EpisodeStop, historyAfterEpisodeToggled(), historyAfterEpisodeWatched(), historyAfterRemoteProgress() (+18 more)

### Community 40 - "SubtitleCueLayout.kt"
Cohesion: 0.12
Nodes (20): CaptionPosition, BOTTOM, TOP, autoStackedRow(), box(), CueBox, CueLayoutTextOutput, dedupeSimultaneousCues() (+12 more)

### Community 41 - "SourceFilterLanguage"
Cohesion: 0.06
Nodes (29): encodeSourceFilterLanguages(), SourceFilterLanguage, ARABIC, BENGALI, CHINESE, DUTCH, ENGLISH, FRENCH (+21 more)

### Community 42 - "SiteDownloadPreparation"
Cohesion: 0.14
Nodes (13): IndividualPreparationRecoveryTest, PreparationStatus, mergePreparedQueueRows(), pendingEpisodeRowId(), aggregatePreparationStatus(), batches(), inheritLegacyPreparationState(), parseSiteDownloadPreparation() (+5 more)

### Community 43 - "SleepTimer.kt"
Cohesion: 0.12
Nodes (11): Counting, EndOfEpisode, formatSleepRemaining(), Off, sleepFadeLevel(), SleepTimer, State, rememberSleepRemaining() (+3 more)

### Community 44 - "SiteDownloadSeries"
Cohesion: 0.09
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
Cohesion: 0.14
Nodes (11): existingOutput(), ExistingOutputIndex, ExistingVideo, existingVideoName(), findExistingOutput(), hasOutputFolderAccess(), isPlausibleExistingVideo(), OutputFolderAccessRequired (+3 more)

### Community 49 - "PreferredAudioLanguage"
Cohesion: 0.10
Nodes (18): PreferredAudioLanguage, ARABIC, AUTOMATIC, BENGALI, CHINESE, ENGLISH, FRENCH, GERMAN (+10 more)

### Community 50 - "SiteDownloadCookieStore"
Cohesion: 0.13
Nodes (8): NativeHeaderContext, normalizeSiteDownloadCookieFile(), siteDownloadCookieHeader(), SiteDownloadCookieStore, validCookiePair(), validSiteDownloadCookieInput(), NativeRequestHeaderPolicyTest, SiteDownloadCookieTest

### Community 52 - "SiteMetadataApi.kt"
Cohesion: 0.26
Nodes (18): Document, metadataDurationSeconds(), metadataFromDocument(), metadataFromHtml(), MetadataDetails, MetadataEpisode, metadataEpisode(), MetadataImage (+10 more)

### Community 53 - "build.sh"
Cohesion: 0.21
Nodes (3): LegacyTrustAnchors, build.sh script, usage()

### Community 54 - "SourceVariant"
Cohesion: 0.18
Nodes (12): Category, DUB, SUB, SourceVariant, DUB, GENERIC_SUB, HARD_SUB, SOFT_SUB (+4 more)

### Community 55 - "CastStreamRouting"
Cohesion: 0.15
Nodes (4): CastStreamRoutingTest, CastStreamRouting, ProxyProbeUrls, StreamCorsProxy

### Community 56 - "OpenSubtitlesClient.kt"
Cohesion: 0.11
Nodes (7): isJwtExpired(), jwtLongClaim(), normalizeSubtitleLanguages(), OpenSubtitleDownload, OpenSubtitlesClient, OpenSubtitleSearchResult, parseSearchResults()

### Community 57 - "AnimepaheDL codebase review"
Cohesion: 0.17
Nodes (12): AnimepaheDL codebase review, Architecture and strengths, P1 — Full-library reconstruction on frequent progress events (P2), P2 — Details availability repeatedly copies/scans records (P2), P3 — Batch source selection scales poorly (P2), P4 — Parallel exporter implementations obscure production behavior (P3), P5 — Persistence schema and orchestration are difficult to evolve (P3), Performance and maintainability (+4 more)

### Community 58 - ".verifyExport"
Cohesion: 0.10
Nodes (8): CacheServerTest, HlsExportTest, TimerTask, TimerTask, CachedHlsMedia, CachedHlsServer, ffmpegRemuxCachedHls(), requireCachedHlsSegments()

### Community 59 - "MidnightHelper"
Cohesion: 0.18
Nodes (15): CaptionChoiceRow(), MidnightChoiceRow(), midnightFocus(), MidnightHelper(), midnightInitialFocus(), MidnightRowScaffold(), MidnightSectionLabel(), midnightSelectable() (+7 more)

### Community 60 - "IOException"
Cohesion: 0.10
Nodes (6): OpenSubtitlesAuthManager, OpenSubtitlesCredentials, artworkRecoveryHostPolicy(), isAllowedArtworkRecoveryUrl(), appSiteDownloadRepository(), EditionProviderBoundaryTest

### Community 61 - "SubtitleDelay.kt"
Cohesion: 0.14
Nodes (5): SubtitleDelay, SubtitleDelayRenderer, SubtitleDelayRenderersFactory, SubtitleDelaySeed, subtitleDelaySeed()

### Community 62 - "CastButton"
Cohesion: 0.17
Nodes (6): applyCastTextTrack(), CastButton(), openSystemCastPicker(), ThemedMediaRouteChooserDialogFragment, ThemedMediaRouteControllerDialogFragment, ThemedMediaRouteDialogFactory

### Community 63 - "KisskhResilienceTest"
Cohesion: 0.10
Nodes (6): fetchProviderSubtitleBytes(), MediaSubtitle, normalizeKisskhSubtitles(), providerSubtitleRequest(), ProviderSubtitles, KisskhResilienceTest

### Community 64 - "ExternalPlayer.kt"
Cohesion: 0.22
Nodes (12): ExternalPlaybackRequest, externalPlaybackRequest(), externalPlayerHeaderPairs(), externalPlayerIntent(), externalPlayerMimeType(), externalPlayerResultCompleted(), externalPlayerResultDurationMs(), externalPlayerResultPositionMs() (+4 more)

### Community 65 - "VerificationRequired"
Cohesion: 0.12
Nodes (5): PreparationResponsivenessTest, SearchRecoveryTest, AnimeRepository, AnimeRepository, AnimeApplication

### Community 66 - "AnimepaheDL resume state"
Cohesion: 0.08
Nodes (24): ADB device test + bookmark feature, AnimepaheDL resume state, Changes in the latest task, Current follow-up release, Current Nginx release artifact (2026-11-11), Custom downloader only, Dedicated details and download-range dialog, Environment and hosting (+16 more)

### Community 67 - "KwikPlaylistDataSource.kt"
Cohesion: 0.20
Nodes (3): hlsIv(), KwikPlaylistDataSource, rewriteKwikPlaylistIvs()

### Community 68 - "parseEroticMvMetadataPage"
Cohesion: 0.22
Nodes (8): EroticMvMetadataApi, eroticTaxonomy(), parseEroticMvMetadataPage(), browse(), MetadataBrowseQuery, MetadataPage, metadataPayload(), MetadataSection

### Community 69 - ".onPlayerError"
Cohesion: 0.14
Nodes (9): playbackFailureEntry(), httpFailureSummary(), numberKeyDigit(), shouldRecoverSeekError(), tvDpadSeekPresses(), tvNumberSeekTargetMs(), tvSeekTargetMs(), host() (+1 more)

### Community 70 - "EroticMvMetadataApi.kt"
Cohesion: 0.25
Nodes (15): absoluteErotic(), cleanEroticDescription(), eroticCard(), eroticDurationSeconds(), eroticImages(), eroticImdb(), eroticRatings(), eroticSiteUrl() (+7 more)

### Community 71 - "Anilili UI reference audit"
Cohesion: 0.12
Nodes (16): Anilili UI reference audit, Automated source comparison, Build/package verification, Complete active shared-component inventory, Download playback-state correction (current source), Downloads, details and settings, Explicit backend adaptations, Home and Browse (+8 more)

### Community 72 - "PlayerControlsScaffold"
Cohesion: 0.22
Nodes (13): BufferedSeekTrack(), controlsSeekAnchorFor(), PlayerActionRow(), playerActionRowScale(), PlayerBackArrow(), PlayerChromeBox(), playerChromeScale(), PlayerControlsScaffold() (+5 more)

### Community 75 - "PlayerSettingsSheet.kt"
Cohesion: 0.16
Nodes (23): MediaVolumeSlider(), PlayerPanelTheme(), CaptionSections(), formatSubtitleDelay(), midnightSliderColors(), PanelHeader(), PlayerAdjustButton(), PlayerAdjustRow() (+15 more)

### Community 76 - "LocalStreamProxy.kt"
Cohesion: 0.18
Nodes (7): dispatchAcceptedSocket(), namedDaemonThreadFactory(), newBoundedProxyRequestExecutor(), ProxyRequest, readLine(), readRequest(), RequestHeaderBudget

### Community 77 - "AppDns"
Cohesion: 0.06
Nodes (13): AppDns, DohProvider, ADGUARD, AUTO, CANADIAN_SHIELD, CLOUDFLARE, CLOUDFLARE_SECURITY, DNS_SB (+5 more)

### Community 78 - "PreferredSubtitleLanguage"
Cohesion: 0.10
Nodes (17): PreferredSubtitleLanguage, ARABIC, AUTOMATIC, CHINESE, ENGLISH, FRENCH, GERMAN, HINDI (+9 more)

### Community 79 - "ErrorReporting.kt"
Cohesion: 0.10
Nodes (14): AnililiException, AnililiFailure, anililiFailure(), classifyTransport(), isCancellation(), raise(), report(), toFailure() (+6 more)

### Community 80 - "developer_notices.py"
Cohesion: 0.09
Nodes (22): healthHost(), isMegaPlayMedia(), MegaPlayCdnHealth, Callback, MegaPlayCdnHealthInterceptor, MegaPlayHealthState, broadcast(), cmd_announce() (+14 more)

### Community 81 - "FfmpegRemux.kt"
Cohesion: 0.06
Nodes (9): ProgressiveExportTest, SelectedHlsDownloadTest, DataSource, InputStream, copyExactly(), normalizeCachedByteRanges(), parseUnknownTotalRange(), readBytesBounded() (+1 more)

### Community 82 - "SiteDownloadRepository"
Cohesion: 0.20
Nodes (15): isHls(), isHlsMimeType(), MediaRequests, normalizedMediaType(), Page, SiteDownloadRepository, decodeEroticMedia(), hostMatchesDomain() (+7 more)

### Community 83 - "SiteMetadataApi"
Cohesion: 0.15
Nodes (9): MetadataSite, EROTICMV, KISSKH, SiteMetadataApi, SiteMetadataApis, Contract, Scope and verification, Supported-site data APIs (+1 more)

### Community 84 - "SiteDownloadEpisode"
Cohesion: 0.29
Nodes (5): chronologicalSiteEpisodes(), downloadEpisodeDisplayOrder(), parseSiteDownloadSelection(), SiteDownloadEpisode, SiteDownloadSelectionSafetyTest

### Community 85 - "LocalStreamProxy"
Cohesion: 0.29
Nodes (4): Entry, LocalStreamProxy, RootHeaderContext, ServerEndpoint

### Community 86 - "AnimeRepository.kt"
Cohesion: 0.10
Nodes (20): AnimeRepository, Anime, AnimeDetails, AnimeNetwork, AnimeRepository, Episode, findMediaUrl(), HttpFailure (+12 more)

### Community 87 - "HlsDownloadSelection.kt"
Cohesion: 0.06
Nodes (18): DeferredDownloader, MediaLinkExpired, cachedHlsMediaPlaylists(), CachedHlsPlaylist, readHlsPlaylist(), selectedHlsDownloader(), Downloader, selectedHlsStreamKeys() (+10 more)

### Community 88 - "SubtitleSearchLanguage"
Cohesion: 0.11
Nodes (17): SubtitleSearchLanguage, ARABIC, CHINESE, ENGLISH, FOLLOW_DEVICE, FRENCH, GERMAN, HINDI (+9 more)

### Community 89 - "ResolvedMedia"
Cohesion: 0.23
Nodes (3): mediaRequestHeaders(), ResolvedMedia, SiteDownloadDownloadTest

### Community 90 - "LibraryTools.kt"
Cohesion: 0.10
Nodes (16): classifyFailure(), FailureReason, DELETION, EXPIRED_SOURCE, FOLDER_ACCESS, INCOMPATIBLE_EXPORT, NETWORK, STORAGE_FULL (+8 more)

### Community 91 - "KisskhCatalog.kt"
Cohesion: 0.31
Nodes (11): detail(), json(), JsonResult, list(), localizeSubtitles(), playbackSubtitles(), Redirect, series() (+3 more)

### Community 92 - "ExternalSubtitleFiles.kt"
Cohesion: 0.22
Nodes (14): AttachedSubtitle, bomCharset(), decodeSubtitleBytes(), displayName(), downloadOpenSubtitle(), exportedSubtitleMimeType(), extensionFor(), importSubtitleFile() (+6 more)

### Community 94 - ".relay"
Cohesion: 0.23
Nodes (4): BodyHead, ProgressOutputStream, readBody(), readHead()

### Community 96 - "ErrorNotice.kt"
Cohesion: 0.21
Nodes (8): DetailLine(), ErrorCodeChip(), ErrorDetailsDialog(), failureAnnotated(), MidnightErrorNotice(), MidnightModal(), MidnightQuietAction(), OpenSubtitlesAccountDialog()

### Community 97 - "c"
Cohesion: 0.13
Nodes (8): causeChain(), platformErrorCode(), platformErrorName(), technicalDetail(), shouldRetryDecoderAtLowerVideoResolution(), iso3Language(), selectedTextLanguage(), syncableSubtitle()

### Community 98 - "CatalogScreen"
Cohesion: 0.15
Nodes (3): CatalogScreen(), catalogHomeSections(), ReferenceHomeChrome()

### Community 102 - ".onCreate"
Cohesion: 0.14
Nodes (3): PlaybackCacheBypassGate, PlaybackStatus, SwappableHttpDataSourceFactory

### Community 103 - "CatalogViewModel.kt"
Cohesion: 0.07
Nodes (20): Cached, catalogCacheJson(), CatalogFailureState, catalogFailureState(), catalogPageForSource(), CatalogSource, EROTICMV, KISSKH (+12 more)

### Community 104 - "DiagnosticsLog"
Cohesion: 0.05
Nodes (11): CrashReporter, DiagnosticsLog, ArtworkBitmapLoader, decodeFlixcloudPlaylist(), decodeFlixcloudSegment(), FlixcloudPlaylistDataSource, matchesAt(), unwrapFlixcloudSegmentInPlace() (+3 more)

### Community 105 - "Test"
Cohesion: 0.09
Nodes (12): parseByteRange(), Details, Download, OperationFailure, OperationId, Preparation, presented(), Search (+4 more)

### Community 106 - "AnnouncementSource.kt"
Cohesion: 0.39
Nodes (7): Announcement, announcementApplies(), AnnouncementFetchResult, AnnouncementSource, compareAppVersions(), parseAnnouncement(), sanitizeAnnouncementUrl()

### Community 107 - "ExternalPlayerTargets.kt"
Cohesion: 0.35
Nodes (7): canReceiveExternalPlayback(), externalPlayerLabel(), externalPlayerProbeIntent(), ExternalPlayerTarget, externalPlayerTargets(), orderExternalPlayerTargets(), toTarget()

### Community 108 - "ThemeAccent"
Cohesion: 0.13
Nodes (13): ThemeAccent, BLOOD_RED, CUSTOM, CYAN, EMERALD, GOLD, MONOCHROME, NEON_GREEN (+5 more)

### Community 109 - "Test"
Cohesion: 0.19
Nodes (3): requireNoHttpUserInfo(), LocalStreamProxyResourceLimitsTest, Callback

### Community 112 - "AppIcon"
Cohesion: 0.10
Nodes (21): AppIcon, Back, Bookmark, BookmarkFilled, Browse, Check, CheckCircle, Close (+13 more)

### Community 113 - "AnnouncementManager.kt"
Cohesion: 0.09
Nodes (5): AnnouncementManager, AnnouncementScheduler, AnnouncementWorker, ForegroundCheck, NotificationPreferences()

### Community 114 - "SourceOptions.kt"
Cohesion: 0.31
Nodes (7): audioLabel(), audioTag(), commonSourceOptions(), EpisodeSource, parseEpisodeSources(), selectEpisodeSource(), SourceOptionsTest

### Community 115 - "ErrorDomain"
Cohesion: 0.14
Nodes (14): ErrorDomain, ACCOUNT, APP, BACKUP, DIAGNOSTICS, DOWNLOAD, EXPORT, LINK (+6 more)

### Community 116 - "RemoteDownloadSheet"
Cohesion: 0.36
Nodes (7): DownloadSectionLabel(), MetadataTermChips(), RemoteDetailHero(), RemoteDownloadSheet(), SiteDownloadCookiesDialog(), SiteDownloadScreen(), SiteMetadataHighlights()

### Community 117 - "SiteDownloadProvider"
Cohesion: 0.20
Nodes (9): cleanProviderTitle(), MediaLinkRefreshGuard, SiteDownloadProvider, DIRECT, DRAMATIP, EROTICMV, KISS18, KISSKH (+1 more)

### Community 118 - "parsePersistedExportRecord"
Cohesion: 0.25
Nodes (9): identity(), media(), nonnegativeLong(), objectOrNull(), parsePersistedExportRecord(), string(), stringOrNull(), jsonObjectOrEmpty() (+1 more)

### Community 119 - "MidnightSettingSwitchRow"
Cohesion: 0.38
Nodes (6): MidnightSettingRow(), MidnightSettingRowScaffold(), MidnightSettingSpotlightMark(), MidnightSettingsSection(), MidnightSettingSwitchRow(), MidnightSwitch()

### Community 124 - "AppColorScheme.kt"
Cohesion: 0.11
Nodes (6): AppColors, appColors(), swatch(), ThemeSpec, wallpaperSeed(), AnimepaheDLTheme()

### Community 128 - "SkipTimingStatus"
Cohesion: 0.15
Nodes (12): SkipReportState, FAILED, IDLE, SENDING, SENT, SkipTimingStatus, ANISKIP, CHECKING (+4 more)

### Community 129 - "TvPlayerControl"
Cohesion: 0.15
Nodes (13): TvPlayerControl, CAPTIONS, EPISODES, FORWARD, FULLSCREEN, MUTE, MY_LIST, NEXT (+5 more)

### Community 131 - "GlassSection"
Cohesion: 0.25
Nodes (7): GlassSection, BROWSE, DETAILS, HOME, NAVIGATION, POPUPS, REFRESH

### Community 134 - "TvFocusTarget.kt"
Cohesion: 0.12
Nodes (9): rememberTvFocusTarget(), tvFocusHandoff(), TvFocusTarget, tvFocusTarget(), TvFocusTargetElement, TvFocusTargetNode, opensTvPlayerControls(), cached() (+1 more)

### Community 135 - "SubtitleItem"
Cohesion: 0.14
Nodes (9): EpisodesResult, hasUsableWindow(), playsInWebView(), ProviderData, SkipTimes, SourcesResult, SubtitleItem, ProviderCatalog (+1 more)

### Community 138 - "c"
Cohesion: 0.33
Nodes (4): deadLoadExhausted(), isStreamRefusal(), refusalEndsStream(), StreamRefusalLoadErrorPolicy

### Community 139 - "DpadLongPress.kt"
Cohesion: 0.29
Nodes (5): dpadClickable(), DpadPress, CLICK, LONG_CLICK, DpadPressTracker

### Community 140 - "ClockFormat"
Cohesion: 0.25
Nodes (5): ClockFormat, SYSTEM, TWELVE_HOUR, TWENTY_FOUR_HOUR, timeOfDayFormatter()

### Community 143 - "PlayerSleepTimer.kt"
Cohesion: 0.13
Nodes (5): playerClockAnchor(), PlayerClockReadout(), playerClockText(), rememberClockMinute(), sleepTimerWarningAnchor()

### Community 144 - "PopupGlass.kt"
Cohesion: 0.19
Nodes (5): blurLine(), boxBlur(), PopupBackdrop, popupGlass(), rememberPopupBackdrop()

### Community 147 - "AppGraph"
Cohesion: 0.18
Nodes (7): AppGraph, Profiles, File inventory, Necessary host adaptations, Original Anilili player port, Player-port release verification (2026-11-08), Review checklist

### Community 150 - "UiScale"
Cohesion: 0.33
Nodes (4): UiScale, LARGE, MEDIUM, SMALL

### Community 151 - "AppBackdropDim"
Cohesion: 0.33
Nodes (4): AppBackdropDim, LIGHT, MEDIUM, STRONG

### Community 153 - "downloadGroupProgress"
Cohesion: 0.40
Nodes (3): DownloadGroupProgress, downloadGroupProgress(), DownloadGroupProgressTest

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

### Community 160 - "SavedFilesWatcher.kt"
Cohesion: 0.21
Nodes (3): missingSavedFiles(), querySavedFiles(), SavedFilesWatcher

### Community 161 - "DiscordStatus"
Cohesion: 0.15
Nodes (7): DiscordStatus, DO_NOT_DISTURB, IDLE, ONLINE, RelatedLayout, LIST, TITLES

### Community 162 - "PlayerLayoutMetrics.kt"
Cohesion: 0.53
Nodes (3): PlayerLayoutMetrics, playerLayoutMetrics(), PlayerWidthTier

### Community 164 - "Browse, play and download"
Cohesion: 0.20
Nodes (9): Anilili Movies, Browse, play and download, Build, Downloads, Library and recovery tools, Release checks, Sessions, Supported sources (+1 more)

### Community 165 - "ThemePreset"
Cohesion: 0.17
Nodes (12): ThemePreset, CRIMSON_NIGHT, CYBER, DEEP_SEA, EMBER, FOREST, GILDED, HIGH_CONTRAST (+4 more)

### Community 166 - "TvPlayerBackTarget"
Cohesion: 0.22
Nodes (9): TvPlayerBackTarget, CAPTION_APPEARANCE, CAPTIONS, tvPlayerBackTarget(), CONTROLS, EPISODE_DRAWER, NONE, OPEN_SUBTITLES (+1 more)

### Community 169 - "KissKH metadata API"
Cohesion: 0.33
Nodes (5): KissKH metadata API, Query/API use, Scope and completeness limits, Structured mapping, Verified endpoints and public data

### Community 170 - "PlaylistForeignSegments.kt"
Cohesion: 0.70
Nodes (4): hostOf(), playlistForeignHosts(), playlistSegmentCensus(), segmentHosts()

### Community 173 - "DownloadQuality"
Cohesion: 0.25
Nodes (6): DownloadQuality, BEST, P1080, P360, P480, P720

### Community 174 - "PlayerAspectRatio"
Cohesion: 0.25
Nodes (6): PlayerAspectRatio, ORIGINAL, RATIO_16_9, RATIO_21_9, RATIO_4_3, SQUARE

### Community 179 - "FfmpegProgressParser"
Cohesion: 0.38
Nodes (3): FfmpegProgress, FfmpegProgressParser, FfmpegProgressTest

### Community 182 - "Anilili feature audit checklist"
Cohesion: 0.25
Nodes (7): Anilili feature audit checklist, Artwork and reliability, Download management, Playback and personal library, Release gate, Settings and appearance from Anilili, Source, search, and metadata

### Community 186 - "downloadLocation"
Cohesion: 0.31
Nodes (6): DownloadLocation, downloadLocation(), episodeTitle(), outputLocation(), safeFileName(), DownloadNamingTest

### Community 188 - "PlayerSystemBars.kt"
Cohesion: 0.25
Nodes (6): PlayerBars, ALL, NONE, STATUS_ONLY, playerBarsFor(), PlayerSystemBarsEffect()

### Community 189 - "Security and privacy findings"
Cohesion: 0.29
Nodes (7): S1 — Cache server reads unbounded lines before checking their length (P2, high confidence), S2 — Remote response bodies and pagination lack resource bounds (P2, high confidence), S3 — Verification completion accepts unrelated HTTPS pages (P2, high confidence), S4 — Session reset can race background network users (P2, high confidence), S5 — Native artifact verification checks size, not identity (P2, high confidence), S6 — Diagnostic redaction is incomplete (P3, high confidence), Security and privacy findings

### Community 191 - "StreamItem"
Cohesion: 0.50
Nodes (4): StreamItem, kwikProgressiveMp4(), fetchedLike(), ReferenceResolved

### Community 193 - "SafeDeletion.kt"
Cohesion: 0.33
Nodes (5): findOutputSubtitleSidecars(), isOutputSubtitleSidecar(), isPlayableOutputSubtitle(), localSubtitleUriExists(), outputSubtitleTrack()

### Community 194 - "ThemeSettings.kt"
Cohesion: 0.24
Nodes (7): accentSeedOf(), AppBackdropSource, ANIME, IMAGE, NONE, backdropUrlOf(), ProfileLook

### Community 197 - "PosterElement"
Cohesion: 0.20
Nodes (9): encodeHiddenPosterElements(), PosterElement, ADULT, DETAILS, DUB, NEW_EPISODES, PROGRESS, SCORE (+1 more)

### Community 209 - "ReferenceChromeTokens.kt"
Cohesion: 0.43
Nodes (3): appCanvasFade(), drawCanvasFade(), Midnight

### Community 211 - "Dramatip metadata API"
Cohesion: 0.40
Nodes (4): Dramatip metadata API, Live evidence and limits, Public surfaces and methods, Verified fields retained

### Community 217 - "PlayerSeekMarkers.kt"
Cohesion: 0.21
Nodes (7): playedFraction(), PlayerSeekBar(), seekTargetMs(), drawPlayerSeekMarkers(), PlayerSeekMarker, playerSeekMarkerFractions(), playerSeekMarkers()

### Community 218 - "parseEroticMvMetadataFeed"
Cohesion: 0.22
Nodes (6): parseEroticMvMetadataFeed(), EroticMvMetadataFixtureTest, Endpoints and operations, EroticMV metadata API, Fields retained, Live evidence and limitations

### Community 231 - "TransferState"
Cohesion: 0.22
Nodes (9): TransferState, COMPLETE, DOWNLOADING, FAILED, PAUSED, QUEUED, REMOVING, RESOLVING (+1 more)

### Community 233 - "DefaultQuality"
Cohesion: 0.18
Nodes (8): DefaultQuality, AUTO, HIGHEST, P1080, P360, P480, P720, dataSaverFallbackMessage()

### Community 234 - "DownloadRow"
Cohesion: 0.25
Nodes (8): downloadGroupArtwork(), DownloadLibrarySnapshot, downloadLibrarySnapshot(), Availability, SAVED, STAGING, DownloadRow, DownloadGroupArtworkTest

### Community 254 - "EroticMvMetadataApi"
Cohesion: 0.16
Nodes (8): MetadataHttp, MetadataResponse, Absent or conditional information, Browse and filtering, Coverage and retained data, Kiss18 public metadata API, Sources actually visited, Verification

### Community 255 - "TvEpisodeChangeFocus"
Cohesion: 0.32
Nodes (8): TvEpisodeChangeFocus, tvEpisodeChangeFocus(), CONTROLS, HOLD, SURFACE, UNCHANGED, tvFocusLostTarget(), tvOverlayDismissFocus()

### Community 257 - "Test"
Cohesion: 0.36
Nodes (3): subtitleSidecarName(), deleteEmptyDirectoriesUpTo(), SafeDeletionTest

### Community 259 - "ThemeStyle"
Cohesion: 0.29
Nodes (5): ThemeStyle, NEUTRAL, TONAL, VIBRANT, VIVID

### Community 262 - "SessionBrowser"
Cohesion: 0.22
Nodes (5): verificationPage(), SessionBrowser(), SessionChecks, verificationOriginMatches(), VerificationPageTest

### Community 264 - "DownloadAction"
Cohesion: 0.29
Nodes (7): DownloadAction, DELETE, PAUSE, RESUME, RETRY, RETRY_SAVE, VERIFY

### Community 265 - "SaveState"
Cohesion: 0.29
Nodes (7): SaveState, CHECKING, COPYING, FAILED, NONE, PREPARING, WAITING

### Community 268 - "Review implementation status"
Cohesion: 0.29
Nodes (5): Latest user corrections, Reliability and security, Review implementation status, UI, performance, and features, Verification

### Community 272 - "UI and usability findings"
Cohesion: 0.29
Nodes (7): U1 — Failed episodes are labeled “Queued” in details (P2), U2 — Search results can belong to an earlier query (P2), U3 — Documented year/type filters are absent (P3), U4 — Library grouping parses presentation text (P2), U5 — Settings failures use success-like styling (P3), U6 — Accessibility/localization coverage needs expansion (P3, verification gap), UI and usability findings

### Community 279 - "PlayerSurface"
Cohesion: 0.05
Nodes (36): PlayerContentScale, CROP, FILL, FIT, rememberScreenReaderActive(), Routes, AutoSkipCountdown, autoSkipCountdownLabel() (+28 more)

### Community 288 - "DownloadRepository.kt"
Cohesion: 0.12
Nodes (12): DownloadRepository, isHlsTransfer(), pause(), readDownloadRows(), refresh(), resume(), byteProgress(), displayTransferTotal() (+4 more)

### Community 290 - "ThemeBackground"
Cohesion: 0.33
Nodes (3): ThemeBackground, AMOLED, DARK

### Community 306 - "MenuLanguage"
Cohesion: 0.29
Nodes (4): MenuLanguage, ENGLISH, SPANISH, SYSTEM

### Community 307 - "Player"
Cohesion: 0.19
Nodes (11): applyAudioTrack(), applyCategoryAudioPreference(), applyPreferredAudioLanguage(), applyPreferredSubtitleLanguage(), CategoryAudioDecision, categoryAudioDecision(), NotReady, preferredLanguageCodes() (+3 more)

### Community 309 - "Narto Drama public metadata API"
Cohesion: 0.40
Nodes (4): API methods and routing, Live audit evidence, Narto Drama public metadata API, Not supplied / not automatically fetched

### Community 310 - "UI parity and usability checklist"
Cohesion: 0.40
Nodes (4): Corrections made in this pass, Screen and component review, UI parity and usability checklist, Verification still needed

### Community 312 - "PlayerSettingsSheet.kt"
Cohesion: 0.06
Nodes (4): asFailure(), focusFill(), appCanvas(), Midnight

## Knowledge Gaps
- **480 isolated node(s):** `Preparation`, `Search`, `Sources`, `NotReady`, `Settled` (+475 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 1306 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **141 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `SettingsStore` connect `SettingsStore` to `LibraryScreens.kt`, `AniListModels.kt`, `GlassSection`, `InPlayerEpisodeList.kt`, `ThemeStyle`, `PlayerSurface.kt`, `TvPlayerControls.kt`, `CaptionStyle`, `ClockFormat`, `AppSymbol`, `CaptionAppearance.kt`, `DiagnosticsLog`, `AppBackdrop.kt`, `AppDeviceProfile.kt`, `MediaCache.kt`, `MidnightSettings.kt`, `UiScale`, `PlayerSurface`, `AppBackdropDim`, `PlaybackBuffer.kt`, `SettingsStore.kt`, `ThemeContrast`, `PlayerGestureControls.kt`, `DiscordStatus`, `ThemeBackground`, `OpenSubtitlesPanel.kt`, `SubtitleCueLayout.kt`, `SourceFilterLanguage`, `.prepareItem`, `DownloadQuality`, `PlayerAspectRatio`, `PreferredAudioLanguage`, `MenuLanguage`, `SourceVariant`, `OpenSubtitlesClient.kt`, `PlayerSettingsSheet.kt`, `PlayerSystemBars.kt`, `ThemeSettings.kt`, `PosterElement`, `AppDns`, `PreferredSubtitleLanguage`, `SeriesLogo.kt`, `DefaultQuality`, `ThemeAccent`, `BackdropBlur.kt`?**
  _High betweenness centrality (0.102) - this node is a cross-community bridge._
- **Are the 55 inferred relationships involving `PlayerSurface()` (e.g. with `.launch()` and `AutoSkipButtonBounds()`) actually correct?**
  _`PlayerSurface()` has 55 INFERRED edges - model-reasoned connections that need verification._
- **What connects `Preparation`, `Search`, `Sources` to the rest of the system?**
  _480 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `AnililiError` be split into smaller, more focused modules?**
  _Cohesion score 0.02 - nodes in this community are weakly interconnected._
- **Why does `PlayerSurface()` connect `PlayerSurface` to `SkipTimingStatus`, `InPlayerEpisodeList.kt`, `TvPlayerControls.kt`, `PlayerSurface.kt`, `SubtitleItem`, `SettingsStore`, `SubtitleItem`, `Listener`, `CaptionAppearance.kt`, `PlayerSleepTimer.kt`, `EpisodeDownloads`, `PlayerScreenLock.kt`, `SettingsStore.kt`, `PlaybackService.kt`, `NextEpisodeTrigger`, `PlayerGestureControls.kt`, `ui/components/WaterFillLogoIndicator.kt`, `FloatingNavigationBar.kt`, `SubtitleTrackMerge.kt`, `Composable`, `OpenSubtitlesPanel.kt`, `ExternalPlaybackReturns.kt`, `LibraryModels.kt`, `TvPlayerBackTarget`, `SleepTimer.kt`, `CompiledSeparately`, `Player`, `SourceVariant`, `MidnightHelper`, `SubtitleDelay.kt`, `CastButton`, `StreamItem`, `ExternalPlayer.kt`, `PlayerControlIconButton`, `.onPlayerError`, `EroticMvMetadataApi.kt`, `PlayerControlsScaffold`, `PlayerSettingsSheet.kt`, `subtitleReprepareState`, `PlayerSeekMarkers.kt`, `ExternalSubtitleFiles.kt`, `c`, `.onCreate`, `DefaultQuality`, `EpisodeControlPlayer`, `AnnouncementManager.kt`, `TvEpisodeChangeFocus`?**
  _High betweenness centrality (0.057) - this node is a cross-community bridge._
- **Should `LibraryScreens.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.06923282544774124 - nodes in this community are weakly interconnected._
- **Why does `DiagnosticsLog` connect `DiagnosticsLog` to `TvFocusTarget.kt`, `PlayerSurface.kt`, `AppBackdrop.kt`, `TvTextField.kt`, `MediaCache.kt`, `CastRoutes.kt`, `PlayerScreenLock.kt`, `PlaybackBuffer.kt`, `SettingsStore.kt`, `ExternalPlaybackReturns.kt`, `SleepTimer.kt`, `build.sh`, `IOException`, `CastButton`, `ExternalPlayer.kt`, `KwikPlaylistDataSource.kt`, `PlaybackDiagnostics.kt`, `LocalStreamProxy.kt`, `AppDns`, `ErrorReporting.kt`, `developer_notices.py`, `LocalStreamProxyService.kt`?**
  _High betweenness centrality (0.054) - this node is a cross-community bridge._