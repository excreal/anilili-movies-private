# Device UI verification

Device: TECNO KM7, USB ADB. Debug application and test APK installed with replacement (no app-data reset).

- Debug and test APK builds passed.
- Normal font: SearchScreenTest (2 cases), LibraryNavigationTest about/return navigation and downloads/settings reachability passed (4 tests).
- 200% system font: About/return navigation and downloads/settings reachability passed (2 tests).
- Visually inspected Explore at 200% font, including scrolled empty-state content, and Downloads at normal font with existing saved episodes.
- Found Downloads bottom-navigation label wrapping at 200%; changed it to one line with deliberate ellipsis, rebuilt, and installed the updated debug APK. This final label change has not been visually rechecked at 200%.
- Restored original system font scale (1.0).

Limitations: this is targeted testing, not the full audit matrix. Landscape, narrow-window overrides, session challenges, pending/error banner combinations, and episode-range interactions were not exercised. Automated visibility assertions do not establish absence of every clipped glyph. Existing saved videos were not played or deleted.

## Compact Downloads and simplified Explore redesign

- One poster per show; episode rows now omit thumbnails and repeated show titles.
- Saved episode rows show episode number, quality/audio/size, Play, and More; unfinished rows retain transfer feedback.
- Collection summary and selection entry share one row.
- Explore retains Sort only, with icon/keyboard search; year/type restrictions are also removed from result filtering.
- Build and unit tests passed. Three targeted device tests passed (two search tests and Downloads/Settings navigation).
- Visually inspected the installed Downloads and Explore screens at normal font size. Screenshots: `downloads-redesign.png`, `explore-redesign.png`.
