# BitAnime.pro v12.1

This repository contains the BitAnime.pro v12 project with the v12.1 mobile usability and catalogue update.

## Included in v12.1
- Mobile-first header and horizontally scrollable genre navigation, avoiding clipped controls on narrow screens.
- Refined hero spotlight layout for mobile screens, with a compact poster and readable title/details/actions.
- A separate Hindi Dubbed Anime catalogue with 251 preserved historical entries, grouped/filterable by genre and dub status.
- Smooth, lightweight UI transitions and reduced-motion support.
- Existing public catalogue/poster providers remain enabled (AniList GraphQL, Jikan and AnimeThemes). These public endpoints do not require a private API key.
- Android native video chooser bridge for compatible local-file / stream handoff to installed players such as VLC or MX Player.

The 251 catalogue entries retain the original status labels; they are not a guarantee that every title/season is currently available in Hindi on every service.

## Build
A push to `main` or a manual workflow run builds an installable debug-signed APK.
Open the GitHub **Actions** tab, select **Build BitAnime.pro v12.1 APK**, then download the `BitAnime-Pro-v12.1-APK` artifact from a successful run.
