# Graph Report - AnililiMovies  (2026-11-18)

## Corpus Check
- 322 files · ~284,138 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 38 file(s) not represented in the graph (top: .xml 22, (none) 3, .ttf 3)

## Summary
- 4927 nodes · 11883 edges · 298 communities (163 shown, 135 thin omitted)
- Extraction: 94% EXTRACTED · 6% INFERRED · 0% AMBIGUOUS · INFERRED: 770 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `03659ed6`
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
- MegaPlayCdnHealth.kt
- AppBackdrop.kt
- TvTextField.kt
- .all
- AppDeviceProfile.kt
- MediaCache.kt
- CastRoutes.kt
- MidnightSettings.kt
- SiteDownloadSources.kt
- DownloaderViewModel.kt
- PlayerGestureControls
- OfflineDownloadService.kt
- InPlayerEpisodeList.kt
- PlaybackBuffer.kt
- SettingsStore.kt
- PlaybackService.kt
- LibraryNavigationTest
- SearchScreenTest.kt
- PlayerGestureControls.kt
- ui/components/WaterFillLogoIndicator.kt
- FloatingNavigationBar.kt
- SubtitleTrackMerge.kt
- MidnightDialog.kt
- OperationRecovery.kt
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
- ProxyActiveResources
- SiteMetadataApi.kt
- build.sh
- SourceVariant
- CastStreamRouting
- AutoSkipCountdown.kt
- AnimepaheDL codebase review
- .verifyExport
- CaptionAppearance.kt
- IOException
- SubtitleDelay.kt
- CastButton
- KisskhResilienceTest
- ExternalPlayer.kt
- VerificationRequired
- SessionBrowser.kt
- KwikPlaylistDataSource.kt
- metadataFromDocument
- PlaybackSeekPolicy.kt
- EroticMvMetadataApi.kt
- Anilili UI reference audit
- SearchScreenTest.kt
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
- PlatformErrorCodes.kt
- .serve
- NetworkIdentity.kt
- SiteDownloadSourcesTest
- .onCreate
- CatalogViewModel.kt
- DiagnosticsLog
- Test
- SleepTimer.kt
- MediaCache
- ThemeAccent
- Test
- AdjustableLoadControl
- CaptionPosition
- ForegroundCheck
- .check
- SourceOptions.kt
- ErrorDomain
- RemoteDownloadSheet
- VideoCacheSize
- parsePersistedExportRecord
- .capture
- Test
- GestureZone
- selectCachedHlsVariant
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
- PlaybackBufferSize
- c
- DpadLongPress.kt
- ClockFormat
- ReferencePlaybackRegistry
- LibraryStore
- ExoDatabase.kt
- c
- EpisodeDownloads
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
- DiscordStatus
- PlayerLayoutMetrics.kt
- cached
- Browse, play and download
- ThemePreset
- TvPlayerBackTarget
- DeletionTestProvider.java
- ExampleInstrumentedTest.kt
- UiState.kt
- PlaylistForeignSegments.kt
- ProviderBenchmark
- normalizeOutputFolder
- DownloadQuality
- PlayerAspectRatio
- PlayerDoubleTapZone
- .captionBodyReadIsCancelledWithItsCoroutine
- .event
- FfmpegProgressParser
- OrderedWrites
- Anilili feature audit checklist
- .validatesContainerSignatureAndMinimumSize
- downloadLocation
- Security and privacy findings
- backdropBlur
- SafeDeletion.kt
- ThemeSettings.kt
- PosterElement
- Intent
- ExistingOutputTest.kt
- ReferenceVideoPlayer.kt
- .event
- Dramatip metadata API
- ExampleUnitTest.kt
- EroticMV metadata API
- X509TrustManager
- Device UI verification
- Developer announcements
- AGENTS.md
- components/README.md
- docs/KISSKH.md
- THIRD_PARTY_NOTICES.md
- make-export-benchmark.sh
- SiteMetadataApi
- DefaultQuality
- DownloadRow
- .operation
- EroticMvMetadataApi
- TvEpisodeChangeFocus
- Test
- ThemeStyle
- Review implementation status
- DiagnosticsLog
- UI and usability findings
- PlayerSurface
- DownloadRepository.kt
- ThemeBackground
- AnnouncementManager.kt
- TransferListener
- .legacyIdentity
- Test
- Callback
- MenuLanguage
- Player
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
- `Necessary host adaptations` --references--> `LibraryStore`  [INFERRED]
  docs/REFERENCE_PLAYER_PORT.md → app/src/main/java/com/anilili/data/library/LibraryStore.kt

## Import Cycles
- None detected.

## Communities (298 total, 135 thin omitted)

### Community 0 - "AnililiError"
Cohesion: 0.02
Nodes (98): AnililiError, ACCOUNT_LIBRARY_LOAD_FAILED, ACCOUNT_LIST_ENTRY_DELETE_FAILED, ACCOUNT_LIST_UPDATE_FAILED, ACCOUNT_LOGIN_ABANDONED, ACCOUNT_LOGIN_PAGE_FAILED, ACCOUNT_PROFILE_LOAD_FAILED, ACCOUNT_PROGRESS_UPDATE_FAILED (+90 more)

### Community 1 - "LibraryScreens.kt"
Cohesion: 0.02
Nodes (55): AppIcon, Back, Bookmark, BookmarkFilled, Browse, Check, CheckCircle, Close (+47 more)

### Community 2 - "AniListModels.kt"
Cohesion: 0.06
Nodes (78): AiringCountEntry, AiringCountMedia, AiringCountPage, AiringDayCount, AiringSchedule, AnimeStat, AppNotification, CoverImage (+70 more)

### Community 3 - "AudioOutputFormat.kt"
Cohesion: 0.15
Nodes (7): AudioOutputFormat, AudioOutputMonitor, AudioRoute, audioRoute(), DECODED, PASSTHROUGH, Listener

### Community 4 - "InPlayerEpisodeList.kt"
Cohesion: 0.08
Nodes (35): EpisodeItem, EpisodeLayout, GRID, LIST, EpisodeOrder, NEWEST_FIRST, OLDEST_FIRST, rememberFocusFill() (+27 more)

### Community 5 - "TvPlayerControls.kt"
Cohesion: 0.05
Nodes (34): AppUiScale(), UnscaledUi(), bufferedFraction(), BufferedSeekTrack(), controlsSeekAnchorFor(), PlayerActionPill(), PlayerActionRow(), playerActionRowScale() (+26 more)

### Community 6 - "PlayerSurface.kt"
Cohesion: 0.06
Nodes (21): adaptiveVideoHeightCeiling(), applyTextTrack(), applyVideoHeight(), applyVideoHeightCeiling(), availableVideoHeights(), CaptionDefaults, captionDefaultsAfterChoice(), categoryAudioRank() (+13 more)

### Community 8 - "SubtitleItem"
Cohesion: 0.39
Nodes (10): loadSubtitleCueTimes(), loadSubtitleText(), nearestSubtitleCues(), parseArrowCueTimes(), parseAssCueTimes(), parseSubtitleCueTimes(), parseTimestampMs(), stripMarkup() (+2 more)

### Community 9 - "NostrUpdateSource.kt"
Cohesion: 0.07
Nodes (22): Announcement, announcementApplies(), AnnouncementFetchResult, AnnouncementSource, compareAppVersions(), parseAnnouncement(), readBoundedAnnouncementBody(), sanitizeAnnouncementUrl() (+14 more)

### Community 10 - "CaptionStyle"
Cohesion: 0.05
Nodes (23): CaptionBackgroundColor, BLACK, GRAY, NAVY, WHITE, CaptionEdgeStyle, DROP_SHADOW, NONE (+15 more)

### Community 11 - "Listener"
Cohesion: 0.09
Nodes (9): playbackFailureEntry(), httpFailureSummary(), contentAspectRatio(), diagnosticSummary(), host(), isLocalMedia(), Listener, preferredAudioTrackCodes() (+1 more)

### Community 12 - "CaptionAppearance.kt"
Cohesion: 0.05
Nodes (13): rememberSystemUses24Hour(), ContentObserver, BubbleSpec, playerClockAnchor(), PlayerClockReadout(), playerClockText(), rememberClockMinute(), playedFraction() (+5 more)

### Community 13 - "MegaPlayCdnHealth.kt"
Cohesion: 0.18
Nodes (6): healthHost(), isMegaPlayMedia(), MegaPlayCdnHealth, Callback, MegaPlayCdnHealthInterceptor, MegaPlayHealthState

### Community 14 - "AppBackdrop.kt"
Cohesion: 0.06
Nodes (20): blurLine(), boxBlur(), PopupBackdrop, popupGlass(), rememberPopupBackdrop(), AppBackdrop, appCanvas(), appCanvasFade() (+12 more)

### Community 15 - "TvTextField.kt"
Cohesion: 0.07
Nodes (14): beginNativeTvEditing(), disableSelectionActionMode(), Callback, hideNativeTvKeyboard(), isImeVisible(), moveNativeFocusSafely(), NativeTvTextWatcher, SafeTvEditText (+6 more)

### Community 16 - ".all"
Cohesion: 0.11
Nodes (15): DeletionRecoveryTest, AppSettings, resume(), DownloadIdentity, matchesQueuedVideo(), adoptExistingVideo(), clearPendingVideo(), reconcileExistingVideos() (+7 more)

### Community 17 - "AppDeviceProfile.kt"
Cohesion: 0.07
Nodes (35): isTelevision(), readTelevisionSignals(), TelevisionSignal, televisionSignal(), CHOSEN, LEANBACK_FEATURE, LEANBACK_LAUNCHER, NO_TOUCHSCREEN (+27 more)

### Community 19 - "CastRoutes.kt"
Cohesion: 0.06
Nodes (7): CastRoute, CastRoutes, GoogleCastRoute, SessionAvailabilityListener, mediaClientOrNull(), shouldGiveUpOnCastLoad(), MiruroCastMediaItemConverter

### Community 20 - "MidnightSettings.kt"
Cohesion: 0.08
Nodes (14): EqualizerWaveIndicator(), MidnightNoticeTone, ERROR, INFO, SUCCESS, WARNING, MidnightChoicePicker(), MidnightSettingRow() (+6 more)

### Community 21 - "SiteDownloadSources.kt"
Cohesion: 0.15
Nodes (16): toAppMetadata(), chooseMediaVariant(), jsonAfterMarker(), jsonArrayOrEmpty(), jsonInt(), jsonString(), matchesInspection(), mediaHeight() (+8 more)

### Community 22 - "DownloaderViewModel.kt"
Cohesion: 0.10
Nodes (8): consumeCancellable(), loadBookmarks(), toggleBookmark(), normalizeCachedByteRanges(), details(), Index, kObjects(), kString()

### Community 23 - "PlayerGestureControls"
Cohesion: 0.16
Nodes (15): formatPlayerTime(), Brightness, canSlideSeek(), canStartHoldSpeed(), GestureLevel, GestureLevelIndicator(), PlaybackGestureIndicator(), playerDoubleTapSeekTarget() (+7 more)

### Community 24 - "OfflineDownloadService.kt"
Cohesion: 0.09
Nodes (4): DownloadRetryService, OfflineDownloads, Listener, OfflineDownloadService

### Community 25 - "InPlayerEpisodeList.kt"
Cohesion: 0.05
Nodes (16): OpenSubtitleSearchResult, episodeWatchFraction(), EpisodeArtwork(), episodeArtworkFallback(), episodeArtworkImage(), NextEpisodeAction(), NextEpisodeCard(), defaultOpenSubtitlesLanguages() (+8 more)

### Community 26 - "PlaybackBuffer.kt"
Cohesion: 0.20
Nodes (4): playbackBufferBytes(), PlaybackBufferPolicy, playbackBufferPolicy(), progressiveConstrainedPolicy()

### Community 27 - "SettingsStore.kt"
Cohesion: 0.07
Nodes (10): coercePlaybackSpeed(), coercePlayerBrightness(), decodeHiddenPosterElements(), decodePersistentCaptionDelays(), decodeSourceFilterLanguages(), DownloadDestination, APP_ONLY, BOTH (+2 more)

### Community 28 - "PlaybackService.kt"
Cohesion: 0.08
Nodes (3): Listener, stateName(), shouldRetryDecoderAtLowerVideoResolution()

### Community 29 - "LibraryNavigationTest"
Cohesion: 0.12
Nodes (6): DownloadSelectionTest, LibraryNavigationTest, downloadsPendingIntent(), Verification plan, Device verification, FFmpeg

### Community 30 - "SearchScreenTest.kt"
Cohesion: 0.08
Nodes (25): 10. Collection summary gives count text precedence over its heading — P2, layout risk, 11. Text-heavy dialogs lack explicit scrolling — P2, layout risk, 12. Image-error copy does not adapt to thumbnail size — P3, layout risk, 13. Verification banner's View action can leave the user on Settings/About — P2, confirmed, 14. Search, empty states, and error treatments vary without shared components — P2, confirmed design divergence, 15. Completion and destructive-action terminology is inconsistent — P2, confirmed, 16. Spacing, chevrons, and text hierarchy are locally defined — P3, confirmed design divergence, 1. Explore search actions consume the input's usable width — P1, layout risk (+17 more)

### Community 31 - "PlayerGestureControls.kt"
Cohesion: 0.09
Nodes (10): applyBrightness(), applyVolume(), GestureDragAxis, Horizontal, Vertical, PlayerBrightnessOverride(), readBrightness(), readVolume() (+2 more)

### Community 33 - "ui/components/WaterFillLogoIndicator.kt"
Cohesion: 0.08
Nodes (19): silhouetteFrame(), silhouetteMask(), cappedFrameSteps(), DetailedWaterFillLogo(), liquidLevel(), LoopHandover, PlainWaterFillLogo(), rememberLiquidClock() (+11 more)

### Community 34 - "FloatingNavigationBar.kt"
Cohesion: 0.08
Nodes (14): backdropBlur(), glassEnabled(), chromeFade(), DownloaderNavigation(), barBlur(), barGlass(), barRim(), FloatingNavDisc() (+6 more)

### Community 35 - "SubtitleTrackMerge.kt"
Cohesion: 0.13
Nodes (33): assDialogueLine(), AssDocument, assPlayRes(), assRecord(), assStyleLine(), assTime(), buildSignsMerge(), decodeEntities() (+25 more)

### Community 36 - "MidnightDialog.kt"
Cohesion: 0.09
Nodes (19): CoverScreenWithDialogWindow(), MidnightBodyTopStop(), MidnightConfirmDialog(), MidnightConnectionStatus(), MidnightDialogFrame(), MidnightDialogHost(), MidnightFilledAction(), MidnightMediaHeader() (+11 more)

### Community 37 - "OperationRecovery.kt"
Cohesion: 0.15
Nodes (8): Details, Download, OperationId, Preparation, presented(), Search, Sources, suppress()

### Community 38 - "ExternalPlaybackReturns.kt"
Cohesion: 0.20
Nodes (6): ExternalPlaybackOutcome, externalPlaybackOutcome(), ExternalPlaybackReturns, ExternalPlaybackSession, shouldSyncAniListProgress(), withExportProgressDeadline()

### Community 39 - "LibraryModels.kt"
Cohesion: 0.11
Nodes (26): capResumePoints(), DismissedRemoteRow, episodeKey(), EpisodeResume, EpisodeStop, historyAfterEpisodeToggled(), historyAfterEpisodeWatched(), historyAfterRemoteProgress() (+18 more)

### Community 40 - "SubtitleCueLayout.kt"
Cohesion: 0.17
Nodes (15): autoStackedRow(), box(), CueBox, CueLayoutTextOutput, dedupeSimultaneousCues(), dropVectorDrawings(), flattenAutoStackedCues(), isAutoStack() (+7 more)

### Community 41 - "SourceFilterLanguage"
Cohesion: 0.06
Nodes (29): encodeSourceFilterLanguages(), SourceFilterLanguage, ARABIC, BENGALI, CHINESE, DUTCH, ENGLISH, FRENCH (+21 more)

### Community 42 - "SiteDownloadPreparation"
Cohesion: 0.19
Nodes (15): PreparationStatus, mergePreparedQueueRows(), pendingEpisodeRowId(), aggregatePreparationStatus(), batches(), chronologicalSiteEpisodes(), inheritLegacyPreparationState(), parseSiteDownloadPreparation() (+7 more)

### Community 43 - "SleepTimer.kt"
Cohesion: 0.20
Nodes (10): Counting, EndOfEpisode, formatSleepRemaining(), Off, SleepTimer, State, rememberSleepRemaining(), SleepTimerSection() (+2 more)

### Community 44 - "SiteDownloadSeries"
Cohesion: 0.08
Nodes (7): OnlineDramaHistoryTest, LibraryScreen(), PlaybackHistory, SavedShowsStore, rememberSeriesPlaybackSummary(), SeriesPlaybackSummary, SiteDownloadSeries

### Community 46 - "SecureTokenStore"
Cohesion: 0.13
Nodes (3): legacyTokenKeyForSlot(), SecureTokenStore, SecureTokenStoreSlotTest

### Community 47 - "DownloadStore"
Cohesion: 0.14
Nodes (5): DownloadDao, DownloadDatabase, DownloadStore, PendingOperation, StoredDownload

### Community 48 - "ExistingOutput.kt"
Cohesion: 0.25
Nodes (8): existingOutput(), ExistingOutputIndex, ExistingVideo, existingVideoName(), findExistingOutput(), hasOutputFolderAccess(), isPlausibleExistingVideo(), OutputFolderAccessRequired

### Community 49 - "PreferredAudioLanguage"
Cohesion: 0.10
Nodes (18): PreferredAudioLanguage, ARABIC, AUTOMATIC, BENGALI, CHINESE, ENGLISH, FRENCH, GERMAN (+10 more)

### Community 50 - "SiteDownloadCookieStore"
Cohesion: 0.13
Nodes (8): NativeHeaderContext, normalizeSiteDownloadCookieFile(), siteDownloadCookieHeader(), SiteDownloadCookieStore, validCookiePair(), validSiteDownloadCookieInput(), NativeRequestHeaderPolicyTest, SiteDownloadCookieTest

### Community 51 - "ProxyActiveResources"
Cohesion: 0.24
Nodes (3): ProxyActiveResources, stopProxyResources(), TrackedProxyResources

### Community 52 - "SiteMetadataApi.kt"
Cohesion: 0.28
Nodes (18): browse(), MetadataBrowseQuery, MetadataDetails, MetadataEpisode, metadataEpisode(), MetadataImage, MetadataLink, MetadataPage (+10 more)

### Community 54 - "SourceVariant"
Cohesion: 0.18
Nodes (12): Category, DUB, SUB, SourceVariant, DUB, GENERIC_SUB, HARD_SUB, SOFT_SUB (+4 more)

### Community 55 - "CastStreamRouting"
Cohesion: 0.15
Nodes (4): CastStreamRoutingTest, CastStreamRouting, ProxyProbeUrls, StreamCorsProxy

### Community 56 - "AutoSkipCountdown.kt"
Cohesion: 0.22
Nodes (3): AutoSkipButtonBounds(), autoSkipSweep(), runAutoSkipCountdown()

### Community 57 - "AnimepaheDL codebase review"
Cohesion: 0.17
Nodes (12): AnimepaheDL codebase review, Architecture and strengths, P1 — Full-library reconstruction on frequent progress events (P2), P2 — Details availability repeatedly copies/scans records (P2), P3 — Batch source selection scales poorly (P2), P4 — Parallel exporter implementations obscure production behavior (P3), P5 — Persistence schema and orchestration are difficult to evolve (P3), Performance and maintainability (+4 more)

### Community 58 - ".verifyExport"
Cohesion: 0.10
Nodes (8): CacheServerTest, HlsExportTest, TimerTask, TimerTask, CachedHlsMedia, CachedHlsServer, ffmpegRemuxCachedHls(), requireCachedHlsSegments()

### Community 59 - "CaptionAppearance.kt"
Cohesion: 0.20
Nodes (10): CaptionAppearanceDialog(), CaptionAppearanceEditor(), CaptionChoiceRow(), captionOffset(), CaptionPlacementPad(), CaptionSample(), horizontalAnchorLabel(), marginLabel() (+2 more)

### Community 60 - "IOException"
Cohesion: 0.08
Nodes (8): OpenSubtitlesAuthManager, OpenSubtitlesCredentials, ArtworkRecovery, artworkRecoveryHostPolicy(), isAllowedArtworkRecoveryUrl(), SessionChecks, appSiteDownloadRepository(), EditionProviderBoundaryTest

### Community 61 - "SubtitleDelay.kt"
Cohesion: 0.14
Nodes (5): SubtitleDelay, SubtitleDelayRenderer, SubtitleDelayRenderersFactory, SubtitleDelaySeed, subtitleDelaySeed()

### Community 62 - "CastButton"
Cohesion: 0.14
Nodes (6): applyCastTextTrack(), CastButton(), openSystemCastPicker(), ThemedMediaRouteChooserDialogFragment, ThemedMediaRouteControllerDialogFragment, ThemedMediaRouteDialogFactory

### Community 63 - "KisskhResilienceTest"
Cohesion: 0.10
Nodes (6): fetchProviderSubtitleBytes(), MediaSubtitle, normalizeKisskhSubtitles(), providerSubtitleRequest(), ProviderSubtitles, KisskhResilienceTest

### Community 64 - "ExternalPlayer.kt"
Cohesion: 0.20
Nodes (9): ExternalPlaybackRequest, externalPlaybackRequest(), externalPlayerHeaderPairs(), externalPlayerIntent(), externalPlayerMimeType(), externalPlayerTitle(), externalShareUri(), launchExternalPlayer() (+1 more)

### Community 65 - "VerificationRequired"
Cohesion: 0.13
Nodes (5): PreparationResponsivenessTest, SearchRecoveryTest, AnimeRepository, AnimeRepository, AnimeApplication

### Community 66 - "SessionBrowser.kt"
Cohesion: 0.05
Nodes (29): verificationPage(), SessionBrowser(), WebViewClient, verificationOriginMatches(), VerificationPageTest, ADB device test + bookmark feature, AnimepaheDL resume state, Changes in the latest task (+21 more)

### Community 67 - "KwikPlaylistDataSource.kt"
Cohesion: 0.20
Nodes (3): hlsIv(), KwikPlaylistDataSource, rewriteKwikPlaylistIvs()

### Community 68 - "metadataFromDocument"
Cohesion: 0.20
Nodes (8): Document, eroticDurationSeconds(), eroticTaxonomy(), metadataDurationSeconds(), metadataFromDocument(), metadataFromHtml(), metadataPayload(), MetadataResponse

### Community 69 - "PlaybackSeekPolicy.kt"
Cohesion: 0.29
Nodes (5): numberKeyDigit(), shouldRecoverSeekError(), tvDpadSeekPresses(), tvNumberSeekTargetMs(), tvSeekTargetMs()

### Community 70 - "EroticMvMetadataApi.kt"
Cohesion: 0.17
Nodes (18): absoluteErotic(), cleanEroticDescription(), eroticCard(), eroticImages(), eroticImdb(), eroticRatings(), eroticSiteUrl(), eroticStats() (+10 more)

### Community 71 - "Anilili UI reference audit"
Cohesion: 0.12
Nodes (16): Anilili UI reference audit, Automated source comparison, Build/package verification, Complete active shared-component inventory, Download playback-state correction (current source), Downloads, details and settings, Explicit backend adaptations, Home and Browse (+8 more)

### Community 75 - "PlayerSettingsSheet.kt"
Cohesion: 0.08
Nodes (38): MidnightInlineNotice(), MidnightQuietAction(), MidnightChoiceRow(), midnightFocus(), MidnightHelper(), midnightInitialFocus(), MidnightRowScaffold(), MidnightSectionLabel() (+30 more)

### Community 76 - "LocalStreamProxy.kt"
Cohesion: 0.15
Nodes (8): dispatchAcceptedSocket(), namedDaemonThreadFactory(), newBoundedProxyRequestExecutor(), ProxyRequest, readLine(), readRequest(), RequestHeaderBudget, RootHeaderPolicyInterceptor

### Community 77 - "AppDns"
Cohesion: 0.06
Nodes (13): AppDns, DohProvider, ADGUARD, AUTO, CANADIAN_SHIELD, CLOUDFLARE, CLOUDFLARE_SECURITY, DNS_SB (+5 more)

### Community 78 - "PreferredSubtitleLanguage"
Cohesion: 0.10
Nodes (17): PreferredSubtitleLanguage, ARABIC, AUTOMATIC, CHINESE, ENGLISH, FRENCH, GERMAN, HINDI (+9 more)

### Community 79 - "ErrorReporting.kt"
Cohesion: 0.14
Nodes (9): AnililiException, anililiFailure(), asFailure(), classifyTransport(), isCancellation(), raise(), report(), toFailure() (+1 more)

### Community 80 - "developer_notices.py"
Cohesion: 0.08
Nodes (22): isJwtExpired(), jwtLongClaim(), normalizeSubtitleLanguages(), OpenSubtitleDownload, OpenSubtitlesClient, parseSearchResults(), broadcast(), cmd_announce() (+14 more)

### Community 81 - "FfmpegRemux.kt"
Cohesion: 0.06
Nodes (9): IndividualPreparationRecoveryTest, SelectedHlsDownloadTest, DataSource, forResolvedMedia(), isExpiredMediaResponse(), isTransientTransferHttpStatus(), MediaVerification, mediaVerificationEvents() (+1 more)

### Community 82 - "SiteDownloadRepository"
Cohesion: 0.18
Nodes (16): isHls(), isHlsMimeType(), normalizedMediaType(), Page, SiteDownloadRepository, decodeEroticMedia(), EroticVideoSource, eroticVideoSource() (+8 more)

### Community 83 - "SiteMetadataApi"
Cohesion: 0.09
Nodes (18): MetadataSite, EROTICMV, KISSKH, SiteMetadataApi, SiteMetadataApis, KissKH metadata API, Query/API use, Scope and completeness limits (+10 more)

### Community 85 - "LocalStreamProxy"
Cohesion: 0.23
Nodes (4): Entry, LocalStreamProxy, RootHeaderContext, ServerEndpoint

### Community 86 - "AnimeRepository.kt"
Cohesion: 0.09
Nodes (20): AnimeRepository, Anime, AnimeDetails, AnimeNetwork, AnimeRepository, Episode, findMediaUrl(), HttpFailure (+12 more)

### Community 87 - "HlsDownloadSelection.kt"
Cohesion: 0.05
Nodes (17): ProgressiveExportTest, copyCancellable(), DeferredDownloader, MediaLinkExpired, cachedHlsMediaPlaylists(), CachedHlsPlaylist, readHlsPlaylist(), selectedHlsDownloader() (+9 more)

### Community 88 - "SubtitleSearchLanguage"
Cohesion: 0.11
Nodes (17): SubtitleSearchLanguage, ARABIC, CHINESE, ENGLISH, FOLLOW_DEVICE, FRENCH, GERMAN, HINDI (+9 more)

### Community 89 - "ResolvedMedia"
Cohesion: 0.21
Nodes (4): mediaRequestHeaders(), MediaRequests, ResolvedMedia, SiteDownloadDownloadTest

### Community 90 - "LibraryTools.kt"
Cohesion: 0.17
Nodes (6): reconcileDirectEnqueues(), inspectStorage(), QueueOrder, StorageReport, verifyCopy(), OutputStream

### Community 91 - "KisskhCatalog.kt"
Cohesion: 0.31
Nodes (11): detail(), json(), JsonResult, list(), localizeSubtitles(), playbackSubtitles(), Redirect, series() (+3 more)

### Community 92 - "ExternalSubtitleFiles.kt"
Cohesion: 0.22
Nodes (14): AttachedSubtitle, bomCharset(), decodeSubtitleBytes(), displayName(), downloadOpenSubtitle(), exportedSubtitleMimeType(), extensionFor(), importSubtitleFile() (+6 more)

### Community 94 - ".relay"
Cohesion: 0.26
Nodes (4): BodyHead, ProgressOutputStream, readBody(), readHead()

### Community 96 - "ErrorNotice.kt"
Cohesion: 0.22
Nodes (6): AnililiFailure, DetailLine(), ErrorCodeChip(), ErrorDetailsDialog(), failureAnnotated(), MidnightErrorNotice()

### Community 97 - "PlatformErrorCodes.kt"
Cohesion: 0.36
Nodes (4): causeChain(), platformErrorCode(), platformErrorName(), technicalDetail()

### Community 98 - ".serve"
Cohesion: 0.24
Nodes (4): InputStream, copyExactly(), parseUnknownTotalRange(), readBytesBounded()

### Community 102 - ".onCreate"
Cohesion: 0.09
Nodes (5): CastRouteStatus, PlaybackCacheBypassGate, PlaybackService, PlaybackStatus, SwappableHttpDataSourceFactory

### Community 103 - "CatalogViewModel.kt"
Cohesion: 0.06
Nodes (21): Cached, catalogCacheJson(), CatalogFailureState, catalogFailureState(), catalogPageForSource(), CatalogSource, EROTICMV, KISSKH (+13 more)

### Community 104 - "DiagnosticsLog"
Cohesion: 0.05
Nodes (11): CrashReporter, DiagnosticsLog, ArtworkBitmapLoader, decodeFlixcloudPlaylist(), decodeFlixcloudSegment(), FlixcloudPlaylistDataSource, matchesAt(), unwrapFlixcloudSegmentInPlace() (+3 more)

### Community 105 - "Test"
Cohesion: 0.19
Nodes (4): parseByteRange(), OperationFailure, SearchVerificationGate, RecoveryPolicyTest

### Community 107 - "MediaCache"
Cohesion: 0.38
Nodes (3): CacheStorage, MediaCache, videoCacheBudget()

### Community 108 - "ThemeAccent"
Cohesion: 0.13
Nodes (13): ThemeAccent, BLOOD_RED, CUSTOM, CYAN, EMERALD, GOLD, MONOCHROME, NEON_GREEN (+5 more)

### Community 109 - "Test"
Cohesion: 0.19
Nodes (3): requireNoHttpUserInfo(), LocalStreamProxyResourceLimitsTest, Callback

### Community 111 - "CaptionPosition"
Cohesion: 0.10
Nodes (11): CaptionPosition, BOTTOM, TOP, applyCaptionEdgeInset(), applyCaptionStyle(), applyContentAspectRatio(), bindUnifiedSettingsButton(), CaptionEdgeInset (+3 more)

### Community 113 - ".check"
Cohesion: 0.18
Nodes (3): AnnouncementManager, AnnouncementScheduler, NotificationPreferences()

### Community 114 - "SourceOptions.kt"
Cohesion: 0.30
Nodes (6): audioLabel(), commonSourceOptions(), EpisodeSource, parseEpisodeSources(), selectEpisodeSource(), SourceOptionsTest

### Community 115 - "ErrorDomain"
Cohesion: 0.14
Nodes (14): ErrorDomain, ACCOUNT, APP, BACKUP, DIAGNOSTICS, DOWNLOAD, EXPORT, LINK (+6 more)

### Community 116 - "RemoteDownloadSheet"
Cohesion: 0.17
Nodes (15): DownloadSectionLabel(), MetadataTermChips(), RemoteDetailHero(), RemoteDownloadSheet(), SiteDownloadCookiesDialog(), SiteDownloadScreen(), SiteMetadataHighlights(), cleanProviderTitle() (+7 more)

### Community 117 - "VideoCacheSize"
Cohesion: 0.22
Nodes (7): VideoCacheSize, AUTOMATIC, GB_1, GB_2, MB_256, MB_512, OFF

### Community 118 - "parsePersistedExportRecord"
Cohesion: 0.25
Nodes (9): identity(), media(), nonnegativeLong(), objectOrNull(), parsePersistedExportRecord(), string(), stringOrNull(), jsonObjectOrEmpty() (+1 more)

### Community 119 - ".capture"
Cohesion: 0.22
Nodes (3): HostPlaybackSnapshot, close(), select()

### Community 121 - "GestureZone"
Cohesion: 0.22
Nodes (9): GestureIndicatorSide, gestureIndicatorSide(), Left, Right, GestureZone, Brightness, Volume, playerGestureZone() (+1 more)

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
Cohesion: 0.14
Nodes (6): rememberTvFocusTarget(), tvFocusHandoff(), TvFocusTarget, tvFocusTarget(), TvFocusTargetElement, TvFocusTargetNode

### Community 135 - "SubtitleItem"
Cohesion: 0.14
Nodes (9): EpisodesResult, hasUsableWindow(), playsInWebView(), ProviderData, SkipTimes, SourcesResult, SubtitleItem, ProviderCatalog (+1 more)

### Community 136 - "PlaybackBufferSize"
Cohesion: 0.25
Nodes (6): PlaybackBufferSize, AUTOMATIC, LARGE, MEDIUM, SMALL, VERY_LARGE

### Community 138 - "c"
Cohesion: 0.33
Nodes (4): deadLoadExhausted(), isStreamRefusal(), refusalEndsStream(), StreamRefusalLoadErrorPolicy

### Community 139 - "DpadLongPress.kt"
Cohesion: 0.21
Nodes (5): dpadClickable(), DpadPress, CLICK, LONG_CLICK, DpadPressTracker

### Community 140 - "ClockFormat"
Cohesion: 0.25
Nodes (5): ClockFormat, SYSTEM, TWELVE_HOUR, TWENTY_FOUR_HOUR, timeOfDayFormatter()

### Community 142 - "LibraryStore"
Cohesion: 0.26
Nodes (4): LibraryStore, externalPlayerResultCompleted(), externalPlayerResultDurationMs(), externalPlayerResultPositionMs()

### Community 144 - "c"
Cohesion: 0.33
Nodes (3): iso3Language(), selectedTextLanguage(), syncableSubtitle()

### Community 146 - "EpisodeDownloads"
Cohesion: 0.20
Nodes (5): StreamItem, EpisodeDownloads, kwikProgressiveMp4(), fetchedLike(), ReferenceResolved

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
Cohesion: 0.11
Nodes (17): DownloadGroupProgress, downloadGroupProgress(), Availability, SAVED, STAGING, TransferState, COMPLETE, DOWNLOADING (+9 more)

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
Cohesion: 0.21
Nodes (3): missingSavedFiles(), querySavedFiles(), SavedFilesWatcher

### Community 161 - "DiscordStatus"
Cohesion: 0.33
Nodes (4): DiscordStatus, DO_NOT_DISTURB, IDLE, ONLINE

### Community 162 - "PlayerLayoutMetrics.kt"
Cohesion: 0.43
Nodes (3): PlayerLayoutMetrics, playerLayoutMetrics(), PlayerWidthTier

### Community 163 - "cached"
Cohesion: 0.47
Nodes (3): opensTvPlayerControls(), cached(), Key

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

### Community 170 - "PlaylistForeignSegments.kt"
Cohesion: 0.70
Nodes (4): hostOf(), playlistForeignHosts(), playlistSegmentCensus(), segmentHosts()

### Community 173 - "DownloadQuality"
Cohesion: 0.25
Nodes (6): DownloadQuality, BEST, P1080, P360, P480, P720

### Community 174 - "PlayerAspectRatio"
Cohesion: 0.25
Nodes (6): PlayerAspectRatio, ORIGINAL, RATIO_16_9, RATIO_21_9, RATIO_4_3, SQUARE

### Community 176 - "PlayerDoubleTapZone"
Cohesion: 0.40
Nodes (5): PlayerDoubleTapZone, playerDoubleTapZone(), Forward, PlayPause, Rewind

### Community 178 - ".event"
Cohesion: 0.11
Nodes (14): DebugTrace, DownloadCompleteReceiver, exportTransferReady(), R10 — Cancellation deadlines are not hard limits on all provider/native operations (P2, risk requiring device/provider reproduction), R1 — Failed persistence writes leave authoritative memory ahead of disk (P1, high confidence), R2 — Cold-start direct completion can be missed (P1, high confidence), R3 — Direct enqueue has a process-death orphan window (P1, high confidence), R4 — HLS validation does not require expected audio (P1, high confidence) (+6 more)

### Community 179 - "FfmpegProgressParser"
Cohesion: 0.38
Nodes (3): FfmpegProgress, FfmpegProgressParser, FfmpegProgressTest

### Community 182 - "Anilili feature audit checklist"
Cohesion: 0.25
Nodes (7): Anilili feature audit checklist, Artwork and reliability, Download management, Playback and personal library, Release gate, Settings and appearance from Anilili, Source, search, and metadata

### Community 186 - "downloadLocation"
Cohesion: 0.29
Nodes (7): DownloadLocation, downloadLocation(), episodeTitle(), outputLocation(), safeFileName(), audioTag(), DownloadNamingTest

### Community 189 - "Security and privacy findings"
Cohesion: 0.29
Nodes (7): S1 — Cache server reads unbounded lines before checking their length (P2, high confidence), S2 — Remote response bodies and pagination lack resource bounds (P2, high confidence), S3 — Verification completion accepts unrelated HTTPS pages (P2, high confidence), S4 — Session reset can race background network users (P2, high confidence), S5 — Native artifact verification checks size, not identity (P2, high confidence), S6 — Diagnostic redaction is incomplete (P3, high confidence), Security and privacy findings

### Community 193 - "SafeDeletion.kt"
Cohesion: 0.33
Nodes (5): findOutputSubtitleSidecars(), isOutputSubtitleSidecar(), isPlayableOutputSubtitle(), localSubtitleUriExists(), outputSubtitleTrack()

### Community 194 - "ThemeSettings.kt"
Cohesion: 0.24
Nodes (7): accentSeedOf(), AppBackdropSource, ANIME, IMAGE, NONE, backdropUrlOf(), ProfileLook

### Community 197 - "PosterElement"
Cohesion: 0.20
Nodes (9): encodeHiddenPosterElements(), PosterElement, ADULT, DETAILS, DUB, NEW_EPISODES, PROGRESS, SCORE (+1 more)

### Community 206 - "ReferenceVideoPlayer.kt"
Cohesion: 0.06
Nodes (15): SeriesLogo(), TrimTransparentEdges, appColors(), ThemeSpec, wallpaperSeed(), PlayerAccentScope(), PlayerPanelTheme(), PlayerSeriesName() (+7 more)

### Community 211 - "Dramatip metadata API"
Cohesion: 0.40
Nodes (4): Dramatip metadata API, Live evidence and limits, Public surfaces and methods, Verified fields retained

### Community 218 - "EroticMV metadata API"
Cohesion: 0.50
Nodes (3): Endpoints and operations, EroticMV metadata API, Fields retained

### Community 233 - "DefaultQuality"
Cohesion: 0.17
Nodes (10): DefaultQuality, AUTO, HIGHEST, P1080, P360, P480, P720, defaultQualityForDevice() (+2 more)

### Community 234 - "DownloadRow"
Cohesion: 0.09
Nodes (23): downloadGroupArtwork(), DownloadLibrarySnapshot, downloadLibrarySnapshot(), DownloadAction, DELETE, PAUSE, RESUME, RETRY (+15 more)

### Community 254 - "EroticMvMetadataApi"
Cohesion: 0.15
Nodes (8): EroticMvMetadataApi, MetadataHttp, Absent or conditional information, Browse and filtering, Coverage and retained data, Kiss18 public metadata API, Sources actually visited, Verification

### Community 255 - "TvEpisodeChangeFocus"
Cohesion: 0.32
Nodes (8): TvEpisodeChangeFocus, tvEpisodeChangeFocus(), CONTROLS, HOLD, SURFACE, UNCHANGED, tvFocusLostTarget(), tvOverlayDismissFocus()

### Community 257 - "Test"
Cohesion: 0.36
Nodes (3): subtitleSidecarName(), deleteEmptyDirectoriesUpTo(), SafeDeletionTest

### Community 259 - "ThemeStyle"
Cohesion: 0.29
Nodes (5): ThemeStyle, NEUTRAL, TONAL, VIBRANT, VIVID

### Community 268 - "Review implementation status"
Cohesion: 0.29
Nodes (5): Latest user corrections, Reliability and security, Review implementation status, UI, performance, and features, Verification

### Community 272 - "UI and usability findings"
Cohesion: 0.29
Nodes (7): U1 — Failed episodes are labeled “Queued” in details (P2), U2 — Search results can belong to an earlier query (P2), U3 — Documented year/type filters are absent (P3), U4 — Library grouping parses presentation text (P2), U5 — Settings failures use success-like styling (P3), U6 — Accessibility/localization coverage needs expansion (P3, verification gap), UI and usability findings

### Community 279 - "PlayerSurface"
Cohesion: 0.05
Nodes (30): PlayerContentScale, CROP, FILL, FIT, signsMergeDialogueLabel(), rememberScreenReaderActive(), CompiledSeparately(), Routes (+22 more)

### Community 288 - "DownloadRepository.kt"
Cohesion: 0.07
Nodes (21): DownloadRepository, isHlsTransfer(), pause(), readDownloadRows(), refresh(), byteProgress(), classifyFailure(), displayTransferTotal() (+13 more)

### Community 290 - "ThemeBackground"
Cohesion: 0.33
Nodes (3): ThemeBackground, AMOLED, DARK

### Community 300 - "AnnouncementManager.kt"
Cohesion: 0.10
Nodes (3): AnnouncementWorker, build.sh script, usage()

### Community 306 - "MenuLanguage"
Cohesion: 0.29
Nodes (4): MenuLanguage, ENGLISH, SPANISH, SYSTEM

### Community 307 - "Player"
Cohesion: 0.17
Nodes (11): applyAudioTrack(), applyCategoryAudioPreference(), applyPreferredAudioLanguage(), applyPreferredSubtitleLanguage(), CategoryAudioDecision, categoryAudioDecision(), NotReady, preferredLanguageCodes() (+3 more)

## Knowledge Gaps
- **480 isolated node(s):** `Preparation`, `Search`, `Sources`, `NotReady`, `Settled` (+475 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 1307 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **135 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `SettingsStore` connect `SettingsStore` to `LibraryScreens.kt`, `AniListModels.kt`, `GlassSection`, `InPlayerEpisodeList.kt`, `ThemeStyle`, `PlayerSurface.kt`, `TvPlayerControls.kt`, `PlaybackBufferSize`, `NostrUpdateSource.kt`, `CaptionStyle`, `ClockFormat`, `AppBackdrop.kt`, `DiagnosticsLog`, `AppDeviceProfile.kt`, `MediaCache.kt`, `UiScale`, `PlayerSurface`, `AppBackdropDim`, `InPlayerEpisodeList.kt`, `SettingsStore.kt`, `ThemeContrast`, `RelatedLayout`, `PlayerGestureControls.kt`, `DiscordStatus`, `ThemeBackground`, `FloatingNavigationBar.kt`, `SubtitleCueLayout.kt`, `SourceFilterLanguage`, `DownloadQuality`, `PlayerAspectRatio`, `PreferredAudioLanguage`, `MenuLanguage`, `SourceVariant`, `CaptionAppearance.kt`, `ThemeSettings.kt`, `PosterElement`, `PlayerSettingsSheet.kt`, `AppDns`, `PreferredSubtitleLanguage`, `ReferenceVideoPlayer.kt`, `developer_notices.py`, `DefaultQuality`, `ThemeAccent`, `CaptionPosition`, `VideoCacheSize`?**
  _High betweenness centrality (0.080) - this node is a cross-community bridge._
- **Are the 55 inferred relationships involving `PlayerSurface()` (e.g. with `.launch()` and `AutoSkipButtonBounds()`) actually correct?**
  _`PlayerSurface()` has 55 INFERRED edges - model-reasoned connections that need verification._
- **What connects `Preparation`, `Search`, `Sources` to the rest of the system?**
  _480 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `AnililiError` be split into smaller, more focused modules?**
  _Cohesion score 0.02 - nodes in this community are weakly interconnected._
- **Why does `DiagnosticsLog` connect `DiagnosticsLog` to `TvFocusTarget.kt`, `PlayerSurface.kt`, `MegaPlayCdnHealth.kt`, `AppBackdrop.kt`, `TvTextField.kt`, `MediaCache.kt`, `CastRoutes.kt`, `InPlayerEpisodeList.kt`, `PlaybackBuffer.kt`, `SettingsStore.kt`, `ExternalPlaybackReturns.kt`, `AnnouncementManager.kt`, `build.sh`, `IOException`, `CastButton`, `ExternalPlayer.kt`, `KwikPlaylistDataSource.kt`, `PlaybackDiagnostics.kt`, `LocalStreamProxy.kt`, `AppDns`, `ErrorReporting.kt`, `SleepTimer.kt`?**
  _High betweenness centrality (0.064) - this node is a cross-community bridge._
- **Should `LibraryScreens.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.023784568249617027 - nodes in this community are weakly interconnected._
- **Why does `PlayerSurface()` connect `PlayerSurface` to `SkipTimingStatus`, `InPlayerEpisodeList.kt`, `TvPlayerControls.kt`, `PlayerSurface.kt`, `SubtitleItem`, `SettingsStore`, `SubtitleItem`, `Listener`, `CaptionAppearance.kt`, `AppBackdrop.kt`, `c`, `EpisodeDownloads`, `PlayerGestureControls`, `InPlayerEpisodeList.kt`, `SettingsStore.kt`, `NextEpisodeTrigger`, `PlayerGestureControls.kt`, `ui/components/WaterFillLogoIndicator.kt`, `SubtitleTrackMerge.kt`, `ExternalPlaybackReturns.kt`, `LibraryModels.kt`, `TvPlayerBackTarget`, `SleepTimer.kt`, `Player`, `SourceVariant`, `AutoSkipCountdown.kt`, `CaptionAppearance.kt`, `SubtitleDelay.kt`, `CastButton`, `ExternalPlayer.kt`, `PlaybackSeekPolicy.kt`, `EroticMvMetadataApi.kt`, `PlayerSettingsSheet.kt`, `ReferenceVideoPlayer.kt`, `ExternalSubtitleFiles.kt`, `.onCreate`, `DefaultQuality`, `CaptionPosition`, `.check`, `TvEpisodeChangeFocus`?**
  _High betweenness centrality (0.039) - this node is a cross-community bridge._