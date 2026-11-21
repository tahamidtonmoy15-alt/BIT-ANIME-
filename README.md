# BitAnime.pro v12

This repository has been replaced with the **BitAnime.pro v12** project from the uploaded ZIP. No earlier BitAnime/NOVA app source is included.

The web app source is stored in the numbered Base64 chunks under `index-html-parts/` to keep the GitHub upload manageable. The GitHub Actions workflow restores the exact HTML file before building the Android app with Capacitor 7.

## Build APK

1. Open the **Actions** tab.
2. Select **Build BitAnime.pro v12 APK**.
3. Open the successful run and download the `BitAnime-Pro-v12-APK` artifact.
4. Extract and install `BitAnime-Pro-v12.apk`.

The workflow builds a debug-signed APK, which is installable on Android. The default Capacitor launcher icon is used because the original image assets are not part of this lean source-only upload.
