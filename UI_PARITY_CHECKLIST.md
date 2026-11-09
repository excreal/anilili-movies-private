# UI parity and usability checklist

Reference: `/root/Anilili` source, plus the Android Developers accessibility and system-gesture guidance reviewed on 2026-11-09.

## Screen and component review

- [x] Home hero, source chips, popular/discover rails, poster cards, and floating navigation.
- [x] Browse title, search, source selector, results grid, loading/error/empty states, and pagination.
- [x] Details hero, bookmark, Play/Resume action, download action, metadata/tags, overview expansion, and episode rows.
- [x] Short-drama player title/save controls, paused state, idle auto-hide, seek bar, and episode picker.
- [x] Normal player integration remains on Anilili's shared player surface; its layout and playback controls were not changed. Gesture zones now leave Android system-navigation areas clear.
- [x] Downloads overview cards, filters, per-episode actions, progress, watched/resume labels, selection, and removal flows.
- [x] Library continue-watching and saved-show rows, including remove and mark-completed actions.
- [x] Settings index/search, player/download/subtitle/notification sections, and appearance.
- [x] Source management follows Anilili's settings structure: separate source visibility and source-order rows open focused pickers. The earlier inline toggle-and-arrows list has been removed.
- [x] Continue Watching cards use Anilili's 174 dp phone width, 16:9 thumbnail, 10 dp spacing, title scale, and 30% image scrim, with this app's requested resume and download status retained.
- [x] KissKH catalog cards preserve its banner artwork with the same card widths and grid column counts as other sources; only the image aspect ratio changes to landscape. Source chips retain the reference pill visual height while Compose expands their accessible touch area.
- [x] Detail page header includes the reference "Details" title between Back and Save.
- [x] Heterogeneous Home and Browse grid items declare stable content types so Compose reuses like-for-like hero, section, banner, poster, and status compositions.
- [x] Announcement dialog and notification permission/status actions.
- [x] Shared card/search/navigation styles and interactive target sizes in the changed UI.

## Corrections made in this pass

- Short-drama chrome now fades after three idle seconds while paused; the center Play action stays visible. The player takes the active app theme, including its accent.
- Short-drama back/bookmark and seek touch areas are at least 48 dp. The episode selector remains paged in groups of 25 for long series.
- Brightness and volume drags ignore the system back edges, bottom home-gesture area, and top notification-shade strip.
- Appearance presets now change the app-wide theme. Source chips can be reordered and hidden from Home and Browse; one source must remain enabled. Settings exposes those actions in separate picker dialogs, matching Anilili's server settings layout.
- KissKH's banner images no longer get cropped into portrait cards. Their card widths/grid density stay the same as other sources, and older cached items are normalized to the wide aspect too.
- The details page only shows More when the overview text overflows, and its control is an explicit button.
- Download poster removal controls keep their compact appearance but have a 48 dp touch target. Browse source chips and detail tabs have larger touch areas.
- Download title cards now prefer artwork saved with the series identity over episode-level thumbnails; EroticMV identities store the normal poster variant when metadata exposes it. Episode rows retain their episode artwork.
- Home section layout controls use the same grid/list icons as Anilili. Browse source-shortcut visibility has a state-specific accessibility label.

## Verification still needed

- [x] `:app:compileDebugKotlin` succeeds.
- [x] Narto playback parsing recognizes both the opaque signed HLS fixture and the Flash Sale episode's opaque MP4 source (`SiteDownloadSourcesTest`).
- [x] Download group poster selection prefers the series poster for every source and falls back to episode artwork only when series artwork is unavailable (`DownloadGroupArtworkTest`).
- [ ] Device visual and interaction pass. `adb devices -l` currently reports no connected device, so screenshots and live gesture checks are unavailable in this workspace.
- [ ] Recheck the rendered layout on a narrow phone and a landscape device after installing the release APK.
