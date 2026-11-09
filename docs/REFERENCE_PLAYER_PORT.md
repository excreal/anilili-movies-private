# Original Anilili player port

The original source is `/root/Anilili/app/src/main/java/com/anilili` and its mobile source set. EroticMV and KissKH use the actual `PlayerSurface`, controls, gesture handlers, subtitle renderer, panels, media session/service and external-player implementation. Short-drama sources keep their vertical player.

## Review checklist

- [x] Original renderer and controls copied, with only host R/BuildConfig imports substituted.
- [x] Original seek bar, play/pause, skip/previous/next, quality/audio, speed and settings panels.
- [x] Original touch gestures, lock, sleep timer, fullscreen and TV controls.
- [x] Original captions selection/import, styling, timing and OpenSubtitles search; account dialog copied from SettingsScreen.
- [x] Host subtitle settings supplies the viewer's own OpenSubtitles API key and encrypted account login. KissKH captions require no OpenSubtitles account.
- [x] Original media session, Cast and external-player source copied; manifest/service/FileProvider integration supplied.
- [x] Original PiP ambient, readiness and activity parameters connected.
- [x] Local file first, with online retry on local playback failure when launched online.
- [x] Online/offline progress uses the same stable episode locator; final progress is captured and watched state retained.
- [x] All resolved quality choices and provider caption tracks reach the original player.
- [x] Final signed release compilation and release lint completed, including subtitle settings; artifact status recorded below.
- [x] Targeted JVM smoke: real KissKH English file, 790 cues; three legacy cipher modes; timestamp normalization/top positioning; HTML rejection.
- [x] Cheaper verification subagent reviewed progress, subtitle, PiP and local-first bindings without source edits.
- [ ] Physical device playback, visual comparison, PiP, Cast and external-player return. ADB reports no connected devices.

## Necessary host adaptations

`ReferenceVideoPlayer` replaces Anilili's anime backend/WatchViewModel with the existing catalog, download, history and library stores. Source-specific anime tracking/skip providers have no corresponding metadata for these catalogs. `AppGraph`, `ProviderCatalog`, `LibraryStore`, `Profiles`, Routes, diagnostics and EpisodeDownloads are host bridges. `PlaybackBuffer` has Media3 1.11 interface updates; `PlaybackService` adds host progress capture. OpenSubtitles panel instructions point to the host's subtitle settings. The original mobile cleartext network policy is retained for local Cast/external-player HTTP proxying; notice signatures remain verified.

KissKH captions are private persistent VTT files referenced by download metadata, not embedded into exported MP4 files. FileProvider makes them available to the original external-player handoff. Existing downloaded videos without caption metadata fetch missing captions when available.

## File inventory

Equality below normalizes line endings and the host R/BuildConfig/MainActivity import substitutions only. Selected-declaration files are intentionally smaller than the original unrelated feature files.

| Source file | Comparison |
| --- | --- |
| `com/anilili/data/AppGraph.kt` | host bridge, selected declarations or documented adaptation |
| `com/anilili/data/NetworkIdentity.kt` | identical after host import substitution |
| `com/anilili/data/ProviderBenchmark.kt` | identical after host import substitution |
| `com/anilili/data/ProviderCatalog.kt` | host bridge, selected declarations or documented adaptation |
| `com/anilili/data/auth/AuthManager.kt` | host bridge, selected declarations or documented adaptation |
| `com/anilili/data/auth/OpenSubtitlesAuthManager.kt` | identical after host import substitution |
| `com/anilili/data/auth/SecureTokenStore.kt` | identical after host import substitution |
| `com/anilili/data/device/TelevisionSignals.kt` | identical after host import substitution |
| `com/anilili/data/library/LibraryModels.kt` | identical after host import substitution |
| `com/anilili/data/library/LibraryStore.kt` | host bridge, selected declarations or documented adaptation |
| `com/anilili/data/model/AniListModels.kt` | identical after host import substitution |
| `com/anilili/data/model/PipeModels.kt` | identical after host import substitution |
| `com/anilili/data/profiles/Profiles.kt` | host bridge, selected declarations or documented adaptation |
| `com/anilili/data/remote/AppDns.kt` | identical after host import substitution |
| `com/anilili/data/remote/AutoDns.kt` | identical after host import substitution |
| `com/anilili/data/remote/LegacyTrustAnchors.kt` | identical after host import substitution |
| `com/anilili/data/remote/MegaPlayCdnHealth.kt` | identical after host import substitution |
| `com/anilili/data/remote/OpenSubtitlesClient.kt` | identical after host import substitution |
| `com/anilili/data/settings/CaptionStyle.kt` | identical after host import substitution |
| `com/anilili/data/settings/GlassSection.kt` | identical after host import substitution |
| `com/anilili/data/settings/SettingsStore.kt` | identical after host import substitution |
| `com/anilili/data/settings/ThemeSettings.kt` | identical after host import substitution |
| `com/anilili/diagnostics/DiagnosticsLog.kt` | host bridge, selected declarations or documented adaptation |
| `com/anilili/errors/AnililiFailure.kt` | identical after host import substitution |
| `com/anilili/errors/ErrorCatalog.kt` | identical after host import substitution |
| `com/anilili/errors/ErrorReporting.kt` | identical after host import substitution |
| `com/anilili/errors/PlatformErrorCodes.kt` | identical after host import substitution |
| `com/anilili/errors/PlaybackFailures.kt` | identical after host import substitution |
| `com/anilili/errors/TlsInterception.kt` | identical after host import substitution |
| `com/anilili/playback/ArtworkBitmapLoader.kt` | identical after host import substitution |
| `com/anilili/playback/AudioOutputFormat.kt` | identical after host import substitution |
| `com/anilili/playback/CastRoute.kt` | identical after host import substitution |
| `com/anilili/playback/CastRouteStatus.kt` | identical after host import substitution |
| `com/anilili/playback/CastRoutes.kt` | identical after host import substitution |
| `com/anilili/playback/CastStreamRouting.kt` | identical after host import substitution |
| `com/anilili/playback/EpisodeDownloads.kt` | host bridge, selected declarations or documented adaptation |
| `com/anilili/playback/EpisodeExport.kt` | host bridge, selected declarations or documented adaptation |
| `com/anilili/playback/ExoDatabase.kt` | identical after host import substitution |
| `com/anilili/playback/ExternalPlaybackReturns.kt` | identical after host import substitution |
| `com/anilili/playback/ExternalPlayer.kt` | identical after host import substitution |
| `com/anilili/playback/ExternalPlayerTargets.kt` | identical after host import substitution |
| `com/anilili/playback/ExternalSubtitleFiles.kt` | identical after host import substitution |
| `com/anilili/playback/FlixcloudPlaylistDataSource.kt` | identical after host import substitution |
| `com/anilili/playback/HlsPlaylistRewrite.kt` | identical after host import substitution |
| `com/anilili/playback/HttpFailureSummary.kt` | identical after host import substitution |
| `com/anilili/playback/KwikPlaylistDataSource.kt` | identical after host import substitution |
| `com/anilili/playback/LocalStreamProxy.kt` | identical after host import substitution |
| `com/anilili/playback/LocalStreamProxyService.kt` | identical after host import substitution |
| `com/anilili/playback/MediaCache.kt` | identical after host import substitution |
| `com/anilili/playback/MediaRequestHeaders.kt` | identical after host import substitution |
| `com/anilili/playback/MiruroCastMediaItemConverter.kt` | identical after host import substitution |
| `com/anilili/playback/NativePlaybackUserAgent.kt` | identical after host import substitution |
| `com/anilili/playback/PlaybackBuffer.kt` | host bridge, selected declarations or documented adaptation |
| `com/anilili/playback/PlaybackDiagnostics.kt` | identical after host import substitution |
| `com/anilili/playback/PlaybackService.kt` | host bridge, selected declarations or documented adaptation |
| `com/anilili/playback/PlaylistForeignSegments.kt` | identical after host import substitution |
| `com/anilili/playback/SleepTimer.kt` | identical after host import substitution |
| `com/anilili/playback/StreamCorsProxy.kt` | identical after host import substitution |
| `com/anilili/playback/StreamRefusalLoadErrorPolicy.kt` | identical after host import substitution |
| `com/anilili/playback/SubtitleCueLayout.kt` | identical after host import substitution |
| `com/anilili/playback/SubtitleCueTimes.kt` | identical after host import substitution |
| `com/anilili/playback/SubtitleDelay.kt` | identical after host import substitution |
| `com/anilili/playback/SubtitleTrackMerge.kt` | identical after host import substitution |
| `com/anilili/playback/UniqueStreamKeyDataSource.kt` | identical after host import substitution |
| `com/anilili/ui/UiState.kt` | identical after host import substitution |
| `com/anilili/ui/adaptive/Accessibility.kt` | identical after host import substitution |
| `com/anilili/ui/adaptive/AppDeviceProfile.kt` | identical after host import substitution |
| `com/anilili/ui/adaptive/DpadLongPress.kt` | identical after host import substitution |
| `com/anilili/ui/adaptive/FocusFill.kt` | identical after host import substitution |
| `com/anilili/ui/adaptive/TvFocusTarget.kt` | identical after host import substitution |
| `com/anilili/ui/adaptive/TvTextField.kt` | identical after host import substitution |
| `com/anilili/ui/adaptive/UiScale.kt` | identical after host import substitution |
| `com/anilili/ui/components/AnililiSilhouette.kt` | identical after host import substitution |
| `com/anilili/ui/components/BackdropBlur.kt` | identical after host import substitution |
| `com/anilili/ui/components/CaptionAppearance.kt` | identical after host import substitution |
| `com/anilili/ui/components/Common.kt` | host bridge, selected declarations or documented adaptation |
| `com/anilili/ui/components/CompiledSeparately.kt` | identical after host import substitution |
| `com/anilili/ui/components/EpisodeArtwork.kt` | identical after host import substitution |
| `com/anilili/ui/components/EpisodeBrowser.kt` | identical after host import substitution |
| `com/anilili/ui/components/EqualizerWaveIndicator.kt` | identical after host import substitution |
| `com/anilili/ui/components/ErrorNotice.kt` | identical after host import substitution |
| `com/anilili/ui/components/SeriesLogo.kt` | identical after host import substitution |
| `com/anilili/ui/components/TimeOfDay.kt` | identical after host import substitution |
| `com/anilili/ui/components/WaterFillLogoIndicator.kt` | identical after host import substitution |
| `com/anilili/ui/dialogs/MidnightDialog.kt` | identical after host import substitution |
| `com/anilili/ui/dialogs/MidnightSelectors.kt` | identical after host import substitution |
| `com/anilili/ui/dialogs/MidnightSettings.kt` | identical after host import substitution |
| `com/anilili/ui/dialogs/PopupGlass.kt` | identical after host import substitution |
| `com/anilili/ui/nav/Routes.kt` | host bridge, selected declarations or documented adaptation |
| `com/anilili/ui/theme/AppBackdrop.kt` | identical after host import substitution |
| `com/anilili/ui/theme/AppColorScheme.kt` | identical after host import substitution |
| `com/anilili/ui/theme/Midnight.kt` | identical after host import substitution |
| `com/anilili/ui/watch/AutoSkipCountdown.kt` | identical after host import substitution |
| `com/anilili/ui/watch/CastUi.kt` | identical after host import substitution |
| `com/anilili/ui/watch/ContextActivity.kt` | identical after host import substitution |
| `com/anilili/ui/watch/InPlayerEpisodeList.kt` | identical after host import substitution |
| `com/anilili/ui/watch/ManualSkipButtonPolicy.kt` | identical after host import substitution |
| `com/anilili/ui/watch/NextEpisodeCard.kt` | identical after host import substitution |
| `com/anilili/ui/watch/OpenSubtitlesPanel.kt` | host bridge, selected declarations or documented adaptation |
| `com/anilili/ui/watch/OutroWindowPolicy.kt` | identical after host import substitution |
| `com/anilili/ui/watch/PlaybackDecoderPolicy.kt` | identical after host import substitution |
| `com/anilili/ui/watch/PlaybackSeekPolicy.kt` | identical after host import substitution |
| `com/anilili/ui/watch/PlayerAudioSwitch.kt` | identical after host import substitution |
| `com/anilili/ui/watch/PlayerClock.kt` | identical after host import substitution |
| `com/anilili/ui/watch/PlayerControlsScaffold.kt` | identical after host import substitution |
| `com/anilili/ui/watch/PlayerFillerSkip.kt` | identical after host import substitution |
| `com/anilili/ui/watch/PlayerGestureControls.kt` | identical after host import substitution |
| `com/anilili/ui/watch/PlayerLayoutMetrics.kt` | identical after host import substitution |
| `com/anilili/ui/watch/PlayerPanelTheme.kt` | identical after host import substitution |
| `com/anilili/ui/watch/PlayerPauseOverlay.kt` | identical after host import substitution |
| `com/anilili/ui/watch/PlayerPictureInPicture.kt` | identical after host import substitution |
| `com/anilili/ui/watch/PlayerScreenLock.kt` | identical after host import substitution |
| `com/anilili/ui/watch/PlayerSeekBar.kt` | identical after host import substitution |
| `com/anilili/ui/watch/PlayerSeekMarkers.kt` | identical after host import substitution |
| `com/anilili/ui/watch/PlayerSeriesLogo.kt` | identical after host import substitution |
| `com/anilili/ui/watch/PlayerSettingsSheet.kt` | identical after host import substitution |
| `com/anilili/ui/watch/PlayerSleepTimer.kt` | identical after host import substitution |
| `com/anilili/ui/watch/PlayerSourcesSheet.kt` | identical after host import substitution |
| `com/anilili/ui/watch/PlayerSurface.kt` | identical after host import substitution |
| `com/anilili/ui/watch/PlayerSystemBars.kt` | identical after host import substitution |
| `com/anilili/ui/watch/PlayerTypography.kt` | identical after host import substitution |
| `com/anilili/ui/watch/ScrollHintColumn.kt` | identical after host import substitution |
| `com/anilili/ui/watch/SkipTimingStatus.kt` | identical after host import substitution |
| `com/anilili/ui/watch/SubtitleAttachmentPolicy.kt` | identical after host import substitution |
| `com/anilili/ui/watch/SubtitleSyncTargets.kt` | identical after host import substitution |
| `com/anilili/ui/watch/TimedSkipPopup.kt` | identical after host import substitution |
| `com/anilili/ui/watch/TvPlayerControls.kt` | identical after host import substitution |
| `com/anilili/ui/watch/TvSafeArea.kt` | identical after host import substitution |
| `com/anilili/ui/watch/WatchViewModel.kt` | host bridge, selected declarations or documented adaptation |
| `com/anilili/util/Base64Compat.kt` | identical after host import substitution |
| `com/anilili/util/CachedRegex.kt` | identical after host import substitution |


## Player-port release verification (2026-11-08)
Signed APK: http://20.120.96.17/builds/app-release-99bae75377e1.apk
SHA-256: `99bae75377e180836f4a3af7329032d26e9c143cbfbc20bb6b389ea18a19bae3` (22,186,942 bytes).
Final release compilation and lint passed: `/tmp/android-setup/reference-player-final-release.log`. Certificate, signature, ZIP alignment, unchanged FFmpeg and hosted byte-range download checks passed. Compiled subtitle smoke passed all five sampled live language files and three legacy decoding formats. Device install/playback/visual/PiP/Cast checks remain pending: `adb devices -l` is empty.
