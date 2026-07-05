package com.swlab.aetherisnova;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.PermissionRequest;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends Activity {

    private static final int REQ_WEB_PERMISSIONS = 7001;

    private WebView webView;
    private EditText addressBar;
    private FrameLayout stage;
    private LinearLayout homeOverlay;
    private PermissionRequest pendingPermissionRequest;

    private final int voidColor = Color.rgb(5, 9, 20);
    private final int deepColor = Color.rgb(7, 16, 31);
    private final int panelColor = Color.rgb(11, 22, 40);
    private final int highColor = Color.rgb(21, 40, 69);
    private final int orbitColor = Color.rgb(66, 232, 255);
    private final int nebulaColor = Color.rgb(90, 124, 255);
    private final int textColor = Color.rgb(246, 250, 255);
    private final int softTextColor = Color.rgb(184, 199, 218);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applySystemBars();
        buildInterface();
        configureWebView();

        Uri startUri = getIntent() != null ? getIntent().getData() : null;
        if (startUri != null) {
            openUrl(startUri.toString());
        } else {
            showHome();
        }
    }

    private void applySystemBars() {
        Window window = getWindow();
        if (window != null) {
            window.setStatusBarColor(voidColor);
            window.setNavigationBarColor(deepColor);
            if (Build.VERSION.SDK_INT >= 23) {
                window.getDecorView().setSystemUiVisibility(0);
            }
        }
    }

    private void buildInterface() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(voidColor);
        root.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));

        root.addView(createTopBar());

        stage = new FrameLayout(this);
        stage.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1));

        webView = new WebView(this);
        webView.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        stage.addView(webView);

        homeOverlay = createHomeOverlay();
        stage.addView(homeOverlay);

        root.addView(stage);
        root.addView(createBottomBar());

        setContentView(root);
    }

    private View createTopBar() {
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(12), dp(10), dp(12), dp(8));
        top.setBackgroundColor(voidColor);
        top.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(72)));

        TextView mark = new TextView(this);
        mark.setText("A");
        mark.setGravity(Gravity.CENTER);
        mark.setTextColor(voidColor);
        mark.setTypeface(Typeface.DEFAULT_BOLD);
        mark.setTextSize(20);
        mark.setBackground(makeRound(orbitColor, dp(18), 0, 0));
        LinearLayout.LayoutParams markParams = new LinearLayout.LayoutParams(dp(42), dp(42));
        markParams.rightMargin = dp(10);
        top.addView(mark, markParams);

        addressBar = new EditText(this);
        addressBar.setSingleLine(true);
        addressBar.setTextColor(textColor);
        addressBar.setHintTextColor(softTextColor);
        addressBar.setTextSize(14);
        addressBar.setHint("Pesquisar ou digitar endereço");
        addressBar.setPadding(dp(16), 0, dp(16), 0);
        addressBar.setSelectAllOnFocus(true);
        addressBar.setImeOptions(EditorInfo.IME_ACTION_GO);
        addressBar.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_URI);
        addressBar.setBackground(makeRound(highColor, dp(26), orbitColor, 1));
        addressBar.setOnEditorActionListener((v, actionId, event) -> {
            boolean enter = event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP;
            if (actionId == EditorInfo.IME_ACTION_GO || enter) {
                openUrl(addressBar.getText().toString());
                return true;
            }
            return false;
        });
        top.addView(addressBar, new LinearLayout.LayoutParams(0, dp(46), 1));

        TextView go = chip("Ir");
        go.setOnClickListener(v -> openUrl(addressBar.getText().toString()));
        LinearLayout.LayoutParams goParams = new LinearLayout.LayoutParams(dp(54), dp(42));
        goParams.leftMargin = dp(10);
        top.addView(go, goParams);

        return top;
    }

    private LinearLayout createHomeOverlay() {
        LinearLayout home = new LinearLayout(this);
        home.setOrientation(LinearLayout.VERTICAL);
        home.setGravity(Gravity.CENTER_HORIZONTAL);
        home.setPadding(dp(22), dp(34), dp(22), dp(20));
        home.setBackgroundColor(voidColor);
        home.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));

        TextView brand = new TextView(this);
        brand.setText("Aetheris Nova");
        brand.setTextColor(textColor);
        brand.setTextSize(32);
        brand.setTypeface(Typeface.DEFAULT_BOLD);
        brand.setGravity(Gravity.CENTER);
        home.addView(brand, new LinearLayout.LayoutParams(-1, -2));

        TextView subtitle = new TextView(this);
        subtitle.setText("Navegação própria. Interface orbital. Base limpa.");
        subtitle.setTextColor(softTextColor);
        subtitle.setTextSize(14);
        subtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(-1, -2);
        subParams.topMargin = dp(8);
        home.addView(subtitle, subParams);

        EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setTextColor(textColor);
        search.setHintTextColor(softTextColor);
        search.setTextSize(16);
        search.setHint("Buscar na web");
        search.setPadding(dp(18), 0, dp(18), 0);
        search.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        search.setBackground(makeRound(highColor, dp(30), orbitColor, 1));
        search.setOnEditorActionListener((v, actionId, event) -> {
            boolean enter = event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP;
            if (actionId == EditorInfo.IME_ACTION_SEARCH || enter) {
                openUrl(search.getText().toString());
                return true;
            }
            return false;
        });
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(-1, dp(58));
        searchParams.topMargin = dp(34);
        home.addView(search, searchParams);

        LinearLayout row1 = quickRow();
        row1.addView(quick("Google", "https://www.google.com"));
        row1.addView(quick("YouTube", "https://www.youtube.com"));
        home.addView(row1);

        LinearLayout row2 = quickRow();
        row2.addView(quick("GitHub", "https://github.com"));
        row2.addView(quick("Duck", "https://duckduckgo.com"));
        home.addView(row2);

        TextView note = new TextView(this);
        note.setText("0.1.0 Foundation • a nova base oficial do Aetheris");
        note.setTextColor(softTextColor);
        note.setTextSize(12);
        note.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(-1, -2);
        noteParams.topMargin = dp(26);
        home.addView(note, noteParams);

        return home;
    }

    private LinearLayout quickRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(58));
        params.topMargin = dp(14);
        row.setLayoutParams(params);
        return row;
    }

    private TextView quick(String label, String url) {
        TextView view = chip(label);
        view.setTextSize(14);
        view.setOnClickListener(v -> openUrl(url));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(50), 1);
        params.leftMargin = dp(6);
        params.rightMargin = dp(6);
        view.setLayoutParams(params);
        return view;
    }

    private View createBottomBar() {
        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setGravity(Gravity.CENTER);
        bottom.setPadding(dp(10), dp(8), dp(10), dp(10));
        bottom.setBackgroundColor(deepColor);
        bottom.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(68)));

        TextView back = nav("‹");
        back.setOnClickListener(v -> {
            if (webView.canGoBack()) webView.goBack();
        });

        TextView forward = nav("›");
        forward.setOnClickListener(v -> {
            if (webView.canGoForward()) webView.goForward();
        });

        TextView home = nav("⌂");
        home.setOnClickListener(v -> showHome());

        TextView reload = nav("↻");
        reload.setOnClickListener(v -> webView.reload());

        TextView menu = nav("☰");
        menu.setOnClickListener(v -> showMenu());

        bottom.addView(back);
        bottom.addView(forward);
        bottom.addView(home);
        bottom.addView(reload);
        bottom.addView(menu);

        return bottom;
    }

    private TextView chip(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setGravity(Gravity.CENTER);
        view.setTextColor(voidColor);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setBackground(makeRound(orbitColor, dp(22), 0, 0));
        return view;
    }

    private TextView nav(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(24);
        view.setGravity(Gravity.CENTER);
        view.setTextColor(textColor);
        view.setBackground(makeRound(panelColor, dp(22), Color.rgb(40, 73, 108), 1));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(48), 1);
        params.leftMargin = dp(4);
        params.rightMargin = dp(4);
        view.setLayoutParams(params);
        return view;
    }

    private android.graphics.drawable.GradientDrawable makeRound(int color, int radius, int strokeColor, int strokeWidthDp) {
        android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        if (strokeWidthDp > 0) {
            drawable.setStroke(dp(strokeWidthDp), strokeColor);
        }
        return drawable;
    }

    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setSupportMultipleWindows(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);

        CookieManager.getInstance().setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= 21) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        }

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleUrl(request.getUrl().toString());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUrl(url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                addressBar.setText(url == null ? "" : url);
                super.onPageFinished(view, url);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                handleWebPermissionRequest(request);
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
            }
        });

        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> startDownload(url, userAgent, contentDisposition, mimeType));
    }

    private void showHome() {
        addressBar.setText("");
        homeOverlay.setVisibility(View.VISIBLE);
    }

    private void openUrl(String input) {
        String url = normalizeInput(input);
        homeOverlay.setVisibility(View.GONE);
        addressBar.setText(url);
        webView.loadUrl(url);
    }

    private String normalizeInput(String input) {
        String value = input == null ? "" : input.trim();
        if (value.isEmpty()) return "https://www.google.com";

        String lower = value.toLowerCase(Locale.US);
        if (lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("file://")) {
            return value;
        }

        if (value.contains(".") && !value.contains(" ")) {
            return "https://" + value;
        }

        return "https://www.google.com/search?q=" + Uri.encode(value);
    }

    private boolean handleUrl(String url) {
        if (url == null) return false;

        Uri uri;
        try {
            uri = Uri.parse(url);
        } catch (Exception e) {
            return false;
        }

        String scheme = uri.getScheme();
        if (scheme == null) return false;

        String normalized = scheme.toLowerCase(Locale.US);

        if ("http".equals(normalized) || "https".equals(normalized)) {
            if (isYoutube(uri)) {
                return openWithPackageOrStay(uri, "com.google.android.youtube");
            }
            return false;
        }

        if ("intent".equals(normalized)) {
            return openIntentUrl(url);
        }

        if ("tel".equals(normalized) || "mailto".equals(normalized) || "sms".equals(normalized) || "smsto".equals(normalized) || "geo".equals(normalized) || "market".equals(normalized)) {
            return openExternal(new Intent(Intent.ACTION_VIEW, uri));
        }

        return false;
    }

    private boolean isYoutube(Uri uri) {
        String host = uri.getHost();
        if (host == null) return false;
        host = host.toLowerCase(Locale.US);
        return host.equals("youtu.be") || host.equals("youtube.com") || host.equals("www.youtube.com") || host.equals("m.youtube.com") || host.equals("music.youtube.com");
    }

    private boolean openWithPackageOrStay(Uri uri, String packageName) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.setPackage(packageName);
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            startActivity(intent);
            return true;
        } catch (Exception notInstalled) {
            return openStore(packageName);
        }
    }

    private boolean openStore(String packageName) {
        try {
            Intent market = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName));
            market.setPackage("com.android.vending");
            startActivity(market);
            return true;
        } catch (Exception e) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)));
                return true;
            } catch (Exception ignored) {
                return false;
            }
        }
    }

    private boolean openIntentUrl(String url) {
        try {
            Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            intent.setComponent(null);
            intent.setSelector(null);
            return openExternal(intent);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean openExternal(Intent intent) {
        try {
            startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Nenhum app encontrado para abrir esse link", Toast.LENGTH_SHORT).show();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void handleWebPermissionRequest(PermissionRequest request) {
        if (Build.VERSION.SDK_INT < 21) return;

        boolean needsCamera = false;
        boolean needsAudio = false;

        for (String resource : request.getResources()) {
            if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)) needsCamera = true;
            if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)) needsAudio = true;
        }

        boolean cameraOk = !needsCamera || checkSelfPermissionCompat(Manifest.permission.CAMERA);
        boolean audioOk = !needsAudio || checkSelfPermissionCompat(Manifest.permission.RECORD_AUDIO);

        if (cameraOk && audioOk) {
            request.grant(request.getResources());
            return;
        }

        pendingPermissionRequest = request;
        if (Build.VERSION.SDK_INT >= 23) {
            requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, REQ_WEB_PERMISSIONS);
        } else {
            request.deny();
        }
    }

    private boolean checkSelfPermissionCompat(String permission) {
        if (Build.VERSION.SDK_INT < 23) return true;
        return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_WEB_PERMISSIONS && pendingPermissionRequest != null) {
            boolean granted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            }

            if (granted) {
                pendingPermissionRequest.grant(pendingPermissionRequest.getResources());
            } else {
                pendingPermissionRequest.deny();
            }
            pendingPermissionRequest = null;
        }
    }

    private void startDownload(String url, String userAgent, String contentDisposition, String mimeType) {
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setMimeType(mimeType);
            request.addRequestHeader("User-Agent", userAgent);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            String filename = URLUtil.guessFileName(url, contentDisposition, mimeType);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            if (dm != null) {
                dm.enqueue(request);
                Toast.makeText(this, "Download iniciado", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Não foi possível iniciar o download", Toast.LENGTH_SHORT).show();
        }
    }

    private void showMenu() {
        String[] items = new String[]{"Copiar link", "Recarregar", "Página inicial", "Sobre Aetheris Nova"};
        new AlertDialog.Builder(this)
                .setTitle("Aetheris")
                .setItems(items, (dialog, which) -> {
                    if (which == 0) copyCurrentUrl();
                    if (which == 1) webView.reload();
                    if (which == 2) showHome();
                    if (which == 3) showAbout();
                })
                .show();
    }

    private void copyCurrentUrl() {
        String url = webView.getUrl();
        if (url == null) url = addressBar.getText().toString();
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("Aetheris URL", url));
            Toast.makeText(this, "Link copiado", Toast.LENGTH_SHORT).show();
        }
    }

    private void showAbout() {
        new AlertDialog.Builder(this)
                .setTitle("Aetheris Nova")
                .setMessage("Aetheris Nova 0.1.0 Foundation\\nNova base oficial, limpa e criada para uma interface própria.")
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    public void onBackPressed() {
        if (homeOverlay != null && homeOverlay.getVisibility() == View.VISIBLE) {
            super.onBackPressed();
            return;
        }
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            showHome();
        }
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}
