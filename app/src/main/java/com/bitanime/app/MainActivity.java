package com.bitanime.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.view.View;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewClientCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MainActivity extends Activity {
    private static final int FILE_CHOOSER_REQUEST = 4102;
    private static final long SHARED_FILE_MAX_AGE_MS = 48L * 60L * 60L * 1000L;

    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;
    private final Map<String, ExportSession> exportSessions = new ConcurrentHashMap<>();

    private static final class ExportSession {
        final File file;
        final FileOutputStream output;
        final String mimeType;

        ExportSession(File file, FileOutputStream output, String mimeType) {
            this.file = file;
            this.output = output;
            this.mimeType = mimeType;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(7, 7, 9));
        getWindow().setNavigationBarColor(Color.rgb(7, 7, 9));
        getWindow().getDecorView().setSystemUiVisibility(0);

        pruneOldSharedFiles();

        webView = new WebView(this);
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webView.setBackgroundColor(Color.rgb(7, 7, 9));

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);

        WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
            .build();

        webView.setWebViewClient(new WebViewClientCompat() {
            @Override
            public android.webkit.WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String host = uri.getHost();
                String scheme = uri.getScheme();
                if ("https".equalsIgnoreCase(scheme)
                    && "appassets.androidplatform.net".equalsIgnoreCase(host)) {
                    return false;
                }
                if ("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme)) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, uri));
                    } catch (Exception ignored) {
                        Toast.makeText(MainActivity.this, "No app can open this link.", Toast.LENGTH_SHORT).show();
                    }
                }
                return true;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(
                WebView view,
                ValueCallback<Uri[]> callback,
                FileChooserParams params
            ) {
                if (filePathCallback != null) filePathCallback.onReceiveValue(null);
                filePathCallback = callback;

                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"video/*", "image/*"});
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);

                try {
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST);
                } catch (Exception e) {
                    filePathCallback = null;
                    callback.onReceiveValue(null);
                    return false;
                }
                return true;
            }
        });

        webView.addJavascriptInterface(new VideoExportBridge(), "BitAnimeAndroid");
        setContentView(webView);

        if (savedInstanceState == null) {
            webView.loadUrl("https://appassets.androidplatform.net/assets/index.html");
        } else {
            webView.restoreState(savedInstanceState);
        }
    }

    private void openVideoChooser(ExportSession session) {
        try {
            Uri contentUri = FileProvider.getUriForFile(
                this,
                getPackageName() + ".fileprovider",
                session.file
            );

            Intent viewIntent = new Intent(Intent.ACTION_VIEW);
            viewIntent.setDataAndType(contentUri, session.mimeType);
            viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            viewIntent.setClipData(ClipData.newUri(getContentResolver(), "Anime episode", contentUri));

            Intent chooser = Intent.createChooser(viewIntent, "Open video with");
            chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(chooser);
        } catch (Exception e) {
            Toast.makeText(this, "No compatible video player was found.", Toast.LENGTH_LONG).show();
        }
    }

    private void pruneOldSharedFiles() {
        File folder = new File(getCacheDir(), "shared-videos");
        File[] files = folder.listFiles();
        if (files == null) return;
        long cutoff = System.currentTimeMillis() - SHARED_FILE_MAX_AGE_MS;
        for (File file : files) {
            if (file.isFile() && file.lastModified() < cutoff) {
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }
        }
    }

    private static String safeFileName(String rawName) {
        String name = rawName == null ? "anime_episode.mp4" : rawName.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) name = name.substring(slash + 1);
        name = name.replaceAll("[^A-Za-z0-9._-]", "_");
        if (name.isEmpty() || name.equals(".") || name.equals("..")) name = "anime_episode.mp4";
        if (!name.contains(".")) name += ".mp4";
        return name;
    }

    private static String safeVideoMimeType(String rawType) {
        if (rawType == null) return "video/*";
        String type = rawType.trim().toLowerCase();
        if (type.startsWith("video/") && type.matches("[a-z0-9!#$&^_.+-]+/[a-z0-9!#$&^_.+-]+")) {
            return type;
        }
        return "video/*";
    }

    private final class VideoExportBridge {
        @JavascriptInterface
        public String beginVideoExport(String requestedName, String requestedMimeType) {
            try {
                File folder = new File(getCacheDir(), "shared-videos");
                if (!folder.exists() && !folder.mkdirs()) return "";

                String id = UUID.randomUUID().toString().replace("-", "");
                File outputFile = new File(folder, id + "_" + safeFileName(requestedName));
                FileOutputStream output = new FileOutputStream(outputFile, false);
                ExportSession session = new ExportSession(
                    outputFile,
                    output,
                    safeVideoMimeType(requestedMimeType)
                );
                exportSessions.put(id, session);
                return id;
            } catch (IOException e) {
                return "";
            }
        }

        @JavascriptInterface
        public boolean appendVideoExportChunk(String id, String base64Chunk) {
            ExportSession session = exportSessions.get(id);
            if (session == null || base64Chunk == null) return false;
            try {
                byte[] bytes = Base64.decode(base64Chunk, Base64.NO_WRAP);
                synchronized (session) {
                    session.output.write(bytes);
                }
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        @JavascriptInterface
        public boolean finishVideoExport(String id) {
            ExportSession session = exportSessions.remove(id);
            if (session == null) return false;
            try {
                synchronized (session) {
                    session.output.flush();
                    session.output.close();
                }
                if (!session.file.exists() || session.file.length() == 0) {
                    //noinspection ResultOfMethodCallIgnored
                    session.file.delete();
                    return false;
                }
                runOnUiThread(() -> openVideoChooser(session));
                return true;
            } catch (IOException e) {
                //noinspection ResultOfMethodCallIgnored
                session.file.delete();
                return false;
            }
        }

        @JavascriptInterface
        public void cancelVideoExport(String id) {
            ExportSession session = exportSessions.remove(id);
            if (session == null) return;
            try {
                synchronized (session) {
                    session.output.close();
                }
            } catch (IOException ignored) {
            }
            //noinspection ResultOfMethodCallIgnored
            session.file.delete();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST && filePathCallback != null) {
            Uri[] results = null;
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                results = new Uri[]{data.getData()};
            }
            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (webView != null) webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        for (Map.Entry<String, ExportSession> entry : exportSessions.entrySet()) {
            ExportSession session = exportSessions.remove(entry.getKey());
            if (session != null) {
                try { session.output.close(); } catch (IOException ignored) {}
                //noinspection ResultOfMethodCallIgnored
                session.file.delete();
            }
        }
        if (webView != null) {
            webView.removeJavascriptInterface("BitAnimeAndroid");
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
