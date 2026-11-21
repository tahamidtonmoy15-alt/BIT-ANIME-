# BitAnime.pro v12.2

This repository builds the BitAnime.pro Android app from the uploaded v12 source, with the mobile/catalog refinements described below.

## Included
- Mobile-first header with horizontally scrollable genre navigation so category controls no longer force the toolbar off-screen.
- Refined hero spotlight sizing and poster layout for narrow screens.
- Separate Hindi Dubbed Anime catalogue with 251 preserved historical entries, searchable/filterable by genre and dub label.
- Original catalogue status labels are retained; they do not guarantee that every season is currently available in Hindi on every streaming service.
- AniList public GraphQL metadata, Tenrai's Jikan-compatible public anime catalogue API, and AnimeThemes OP/ED lookup remain connected.
- Native Android video handoff bridge for compatible local files and stream URLs, opening the Android chooser for installed video players.

## API keys
The app's public catalogue lookups use public endpoints and do not require private API keys. Tenrai offers an optional server key for higher throughput, but it is not embedded in this public client build. Never commit a private API key to this public repository.

## Build
Pushes to `main` or a manual workflow run create an installable debug-signed APK. Open GitHub **Actions**, select **Build BitAnime.pro v12.2 APK**, then download the `BitAnime-Pro-v12.2-APK` artifact from the successful run.
