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
- [x] Settings index/search, player/download/subtitle/notification sections, appearance, and source management.
- [x] Announcement dialog and notification permission/status actions.
- [x] Shared card/search/navigation styles and interactive target sizes in the changed UI.

## Corrections made in this pass

- Short-drama chrome now fades after three idle seconds while paused; the center Play action stays visible. The player takes the active app theme, including its accent.
- Short-drama back/bookmark and seek touch areas are at least 48 dp. The episode selector remains paged in groups of 25 for long series.
- Brightness and volume drags ignore the system back edges, bottom home-gesture area, and top notification-shade strip.
- Appearance presets now change the app-wide theme. Source chips can be reordered and hidden from Home and Browse; one source must remain enabled.
- The details page only shows More when the overview text overflows, and its control is an explicit button.
- Download poster removal controls keep their compact appearance but have a 48 dp touch target. Browse source chips and detail tabs have larger touch areas.

## Verification still needed

- [x] `:app:compileDebugKotlin` succeeds.
- [x] Narto playback parsing recognizes both the opaque signed HLS fixture and the Flash Sale episode's opaque MP4 source (`SiteDownloadSourcesTest`).
- [ ] Device visual and interaction pass. `adb devices -l` currently reports no connected device, so screenshots and live gesture checks are unavailable in this workspace.
- [ ] Recheck the rendered layout on a narrow phone and a landscape device after installing the release APK.
