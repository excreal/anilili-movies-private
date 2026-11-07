# Shared Material components

Screens consume these components instead of duplicating surface and control styling.
DesignSystem owns animated cards; ChoiceMenu owns selection controls;
DestinationSummary owns folder access; SettingsRow owns aligned settings entries;
MediaComponents owns image, loading, and empty states. Theme colors and typography
remain in ui/theme. Preserve Material minimum touch targets and avoid adding
per-frame download animations: animate structural expansion only.
