package com.swlab.aetherisnova;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
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
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends Activity {

    private static final int REQ_WEB_PERMISSIONS = 7001;

    private WebView webView;
    private EditText addressBar;
    private FrameLayout stage;
    private View homeOverlay;
    private PermissionRequest pendingPermissionRequest;

    private final int voidColor = Color.rgb(3, 8, 20);
    private final int nightColor = Color.rgb(6, 17, 31);
    private final int deepColor = Color.rgb(9, 24, 43);
    private final int glassColor = Color.argb(210, 12, 24, 42);
    private final int glassSoftColor = Color.argb(165, 18, 34, 56);
    private final int glassHighColor = Color.argb(230, 23, 45, 74);
    private final int orbitColor = Color.rgb(68, 232, 255);
    private final int orbitSoftColor = Color.rgb(154, 245, 255);
    private final int auroraColor = Color.rgb(108, 125, 255);
    private final int lumenColor = Color.rgb(183, 156, 255);
    private final int textColor = Color.rgb(246, 250, 255);
    private final int softTextColor = Color.rgb(174, 190, 210);
    private final int lineColor = Color.rgb(49, 92, 127);

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
            window.setNavigationBarColor(voidColor);
            if (Build.VERSION.SDK_INT >= 23) {
                window.getDecorView().setSystemUiVisibility(0);
            }
        }
    }

    private void buildInterface() {
        FrameLayout root = new FrameLayout(this);
        root.setBackground(backgroundDrawable());
        root.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));

        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setPadding(dp(12), dp(8), dp(12), dp(10));
        shell.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));

        shell.addView(createTopBar());

        stage = new FrameLayout(this);
        LinearLayout.LayoutParams stageParams = new LinearLayout.LayoutParams(-1, 0, 1);
        stageParams.topMargin = dp(6);
        stageParams.bottomMargin = dp(8);
        stage.setLayoutParams(stageParams);

        webView = new WebView(this);
        webView.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        webView.setBackgroundColor(voidColor);
        stage.addView(webView);

        homeOverlay = createHomeOverlay();
        stage.addView(homeOverlay);

        shell.addView(stage);
        shell.addView(createBottomDock());

        root.addView(shell);
        setContentView(root);
    }

    private View createTopBar() {
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(0, 0, 0, 0);
        top.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(58)));

        TextView mark = new TextView(this);
        mark.setText("A");
        mark.setGravity(Gravity.CENTER);
        mark.setTextColor(voidColor);
        mark.setTypeface(Typeface.DEFAULT_BOLD);
        mark.setTextSize(19);
        mark.setBackground(circleGradient());
        mark.setElevation(dp(8));
        LinearLayout.LayoutParams markParams = new LinearLayout.LayoutParams(dp(48), dp(48));
        markParams.rightMargin = dp(10);
        top.addView(mark, markParams);

        addressBar = new EditText(this);
        addressBar.setSingleLine(true);
        addressBar.setTextColor(textColor);
        addressBar.setHintTextColor(softTextColor);
        addressBar.setTextSize(14);
        addressBar.setHint("Pesquisar ou digitar endereço");
        addressBar.setPadding(dp(18), 0, dp(18), 0);
        addressBar.setSelectAllOnFocus(true);
        addressBar.setImeOptions(EditorInfo.IME_ACTION_GO);
        addressBar.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_URI);
        addressBar.setBackground(roundGradient(glassHighColor, glassSoftColor, dp(28), orbitColor, 1));
        addressBar.setOnEditorActionListener((v, actionId, event) -> {
            boolean enter = event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP;
            if (actionId == EditorInfo.IME_ACTION_GO || enter) {
                openUrl(addressBar.getText().toString());
                return true;
            }
            return false;
        });
        top.addView(addressBar, new LinearLayout.LayoutParams(0, dp(48), 1));

        TextView go = topAction("Ir");
        go.setOnClickListener(v -> openUrl(addressBar.getText().toString()));
        LinearLayout.LayoutParams goParams = new LinearLayout.LayoutParams(dp(58), dp(48));
        goParams.leftMargin = dp(10);
        top.addView(go, goParams);

        return top;
    }

    private View createHomeOverlay() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.TRANSPARENT);
        scroll.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));

        LinearLayout home = new LinearLayout(this);
        home.setOrientation(LinearLayout.VERTICAL);
        home.setGravity(Gravity.CENTER_HORIZONTAL);
        home.setPadding(dp(8), dp(18), dp(8), dp(22));
        scroll.addView(home, new ScrollView.LayoutParams(-1, -2));

        TextView orbit = new TextView(this);
        orbit.setText("◌");
        orbit.setTextColor(Color.argb(150, 68, 232, 255));
        orbit.setTextSize(54);
        orbit.setGravity(Gravity.CENTER);
        home.addView(orbit, new LinearLayout.LayoutParams(-1, dp(46)));

        TextView brand = new TextView(this);
        brand.setText("Aetheris");
        brand.setTextColor(textColor);
        brand.setTextSize(34);
        brand.setTypeface(Typeface.DEFAULT_BOLD);
        brand.setGravity(Gravity.CENTER);
        home.addView(brand, new LinearLayout.LayoutParams(-1, -2));

        TextView subtitle = new TextView(this);
        subtitle.setText("Glass Orbit UI • navegação limpa e própria");
        subtitle.setTextColor(softTextColor);
        subtitle.setTextSize(14);
        subtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(-1, -2);
        subParams.topMargin = dp(8);
        home.addView(subtitle, subParams);

        LinearLayout searchCard = new LinearLayout(this);
        searchCard.setOrientation(LinearLayout.HORIZONTAL);
        searchCard.setGravity(Gravity.CENTER_VERTICAL);
        searchCard.setPadding(dp(14), dp(10), dp(10), dp(10));
        searchCard.setBackground(roundGradient(glassColor, glassSoftColor, dp(32), orbitColor, 1));
        searchCard.setElevation(dp(8));
        LinearLayout.LayoutParams searchCardParams = new LinearLayout.LayoutParams(-1, dp(68));
        searchCardParams.topMargin = dp(28);
        home.addView(searchCard, searchCardParams);

        EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setTextColor(textColor);
        search.setHintTextColor(softTextColor);
        search.setTextSize(16);
        search.setHint("Buscar na web");
        search.setPadding(dp(8), 0, dp(8), 0);
        search.setBackgroundColor(Color.TRANSPARENT);
        search.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        search.setOnEditorActionListener((v, actionId, event) -> {
            boolean enter = event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP;
            if (actionId == EditorInfo.IME_ACTION_SEARCH || enter) {
                openUrl(search.getText().toString());
                return true;
            }
            return false;
        });
        searchCard.addView(search, new LinearLayout.LayoutParams(0, -1, 1));

        TextView searchGo = topAction("↗");
        searchGo.setTextSize(20);
        searchGo.setOnClickListener(v -> openUrl(search.getText().toString()));
        searchCard.addView(searchGo, new LinearLayout.LayoutParams(dp(48), dp(48)));

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(2);
        grid.setUseDefaultMargins(false);
        LinearLayout.LayoutParams gridParams = new LinearLayout.LayoutParams(-1, -2);
        gridParams.topMargin = dp(18);
        home.addView(grid, gridParams);

        grid.addView(quickTile("Google", "Busca principal", "https://www.google.com"));
        grid.addView(quickTile("YouTube", "Vídeos", "https://www.youtube.com"));
        grid.addView(quickTile("GitHub", "Código", "https://github.com"));
        grid.addView(quickTile("Duck", "Privacidade", "https://duckduckgo.com"));

        LinearLayout status = new LinearLayout(this);
        status.setOrientation(LinearLayout.VERTICAL);
        status.setPadding(dp(18), dp(16), dp(18), dp(16));
        status.setBackground(roundGradient(Color.argb(120, 12, 24, 42), Color.argb(80, 18, 34, 56), dp(26), lineColor, 1));
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-1, -2);
        statusParams.topMargin = dp(20);
        home.addView(status, statusParams);

        TextView statusTitle = smallLabel("Aetheris Nova 0.1.1");
        statusTitle.setTextColor(orbitSoftColor);
        statusTitle.setTypeface(Typeface.DEFAULT_BOLD);
        status.addView(statusTitle);

        TextView statusBody = smallLabel("Interface Glass Orbit aplicada como base visual própria. Ainda é leve, sem blur real pesado.");
        LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(-1, -2);
        bodyParams.topMargin = dp(6);
        status.addView(statusBody, bodyParams);

        return scroll;
    }

    private View quickTile(String title, String caption, String url) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER_VERTICAL);
        tile.setPadding(dp(16), dp(12), dp(16), dp(12));
        tile.setBackground(roundGradient(glassColor, glassSoftColor, dp(28), Color.argb(100, 68, 232, 255), 1));
        tile.setElevation(dp(6));
        tile.setOnClickListener(v -> openUrl(url));

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = dp(82);
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(dp(5), dp(5), dp(5), dp(5));
        tile.setLayoutParams(params);

        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextColor(textColor);
        titleView.setTextSize(16);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setGravity(Gravity.CENTER);
        tile.addView(titleView, new LinearLayout.LayoutParams(-1, -2));

        TextView captionView = new TextView(this);
        captionView.setText(caption);
        captionView.setTextColor(softTextColor);
        captionView.setTextSize(11);
        captionView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams capParams = new LinearLayout.LayoutParams(-1, -2);
        capParams.topMargin = dp(4);
        tile.addView(captionView, capParams);

        return tile;
    }

    private View createBottomDock() {
        LinearLayout dock = new LinearLayout(this);
        dock.setOrientation(LinearLayout.HORIZONTAL);
        dock.setGravity(Gravity.CENTER);
        dock.setPadding(dp(8), dp(7), dp(8), dp(7));
        dock.setBackground(roundGradient(Color.argb(225, 6, 17, 31), Color.argb(210, 9, 24, 43), dp(30), Color.argb(120, 68, 232, 255), 1));
        dock.setElevation(dp(10));

        LinearLayout.LayoutParams dockParams = new LinearLayout.LayoutParams(-1, dp(64));
        dockParams.leftMargin = dp(2);
        dockParams.rightMargin = dp(2);
        dock.setLayoutParams(dockParams);

        TextView back = dockButton("‹");
        back.setOnClickListener(v -> {
            if (webView.canGoBack()) webView.goBack();
        });

        TextView forward = dockButton("›");
        forward.setOnClickListener(v -> {
            if (webView.canGoForward()) webView.goForward();
        });

        TextView home = dockButton("⌂");
        home.setOnClickListener(v -> showHome());

        TextView reload = dockButton("↻");
        reload.setOnClickListener(v -> webView.reload());

        TextView menu = dockButton("☰");
        menu.setOnClickListener(v -> showMenuSheet());

        dock.addView(back);
        dock.addView(forward);
        dock.addView(home);
        dock.addView(reload);
        dock.addView(menu);

        return dock;
    }

    private TextView topAction(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setGravity(Gravity.CENTER);
        view.setTextColor(voidColor);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setTextSize(15);
        view.setBackground(circleGradient());
        view.setElevation(dp(8));
        return view;
    }

    private TextView dockButton(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(24);
        view.setGravity(Gravity.CENTER);
        view.setTextColor(textColor);
        view.setBackground(roundGradient(Color.argb(150, 16, 32, 54), Color.argb(110, 12, 24, 42), dp(24), Color.argb(80, 49, 92, 127), 1));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1);
        params.leftMargin = dp(4);
        params.rightMargin = dp(4);
        view.setLayoutParams(params);
        return view;
    }

    private TextView smallLabel(String text) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextColor(softTextColor);
        label.setTextSize(12);
        label.setGravity(Gravity.CENTER);
        return label;
    }

    private GradientDrawable backgroundDrawable() {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{
                        Color.rgb(3, 8, 20),
                        Color.rgb(5, 13, 28),
                        Color.rgb(7, 20, 35)
                }
        );
        return drawable;
    }

    private GradientDrawable circleGradient() {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{orbitSoftColor, orbitColor, auroraColor}
        );
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(dp(22));
        return drawable;
    }

    private GradientDrawable roundGradient(int startColor, int endColor, int radius, int strokeColor, int strokeWidthDp) {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{startColor, endColor}
        );
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

    private void showMenuSheet() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(20), dp(18), dp(20), dp(18));
        panel.setBackground(roundGradient(Color.argb(245, 12, 24, 42), Color.argb(238, 18, 34, 56), dp(30), Color.argb(120, 68, 232, 255), 1));

        TextView title = new TextView(this);
        title.setText("Aetheris");
        title.setTextColor(textColor);
        title.setTextSize(24);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        panel.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView url = new TextView(this);
        String current = webView.getUrl();
        if (current == null || current.trim().isEmpty()) current = "Página inicial";
        url.setText(current);
        url.setTextColor(softTextColor);
        url.setTextSize(12);
        url.setSingleLine(true);
        LinearLayout.LayoutParams urlParams = new LinearLayout.LayoutParams(-1, -2);
        urlParams.topMargin = dp(4);
        urlParams.bottomMargin = dp(12);
        panel.addView(url, urlParams);

        panel.addView(menuRow("Copiar link", "Copiar endereço atual", () -> {
            dialog.dismiss();
            copyCurrentUrl();
        }));

        panel.addView(menuRow("Recarregar", "Atualizar a página aberta", () -> {
            dialog.dismiss();
            webView.reload();
        }));

        panel.addView(menuRow("Página inicial", "Voltar para a home Aetheris", () -> {
            dialog.dismiss();
            showHome();
        }));

        panel.addView(menuRow("Compartilhar", "Enviar link para outro app", () -> {
            dialog.dismiss();
            shareCurrentUrl();
        }));

        panel.addView(menuRow("Sobre", "Versão 0.1.1 Glass Orbit", () -> {
            dialog.dismiss();
            showAboutSheet();
        }));

        dialog.setContentView(panel);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        dialog.setOnShowListener(d -> {
            Window w = dialog.getWindow();
            if (w != null) {
                w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                w.setDimAmount(0.55f);
                w.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                w.setGravity(Gravity.BOTTOM);
                w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        });

        dialog.show();
    }

    private View menuRow(String title, String caption, final Runnable action) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        row.setBackground(roundGradient(Color.argb(90, 16, 32, 54), Color.argb(60, 12, 24, 42), dp(22), Color.argb(55, 49, 92, 127), 1));
        row.setOnClickListener(v -> action.run());

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(8);
        row.setLayoutParams(params);

        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextColor(textColor);
        titleView.setTextSize(16);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        row.addView(titleView);

        TextView captionView = new TextView(this);
        captionView.setText(caption);
        captionView.setTextColor(softTextColor);
        captionView.setTextSize(12);
        LinearLayout.LayoutParams capParams = new LinearLayout.LayoutParams(-1, -2);
        capParams.topMargin = dp(3);
        row.addView(captionView, capParams);

        return row;
    }

    private void copyCurrentUrl() {
        String url = webView.getUrl();
        if (url == null || url.trim().isEmpty()) url = addressBar.getText().toString();
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("Aetheris URL", url));
            Toast.makeText(this, "Link copiado", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareCurrentUrl() {
        String url = webView.getUrl();
        if (url == null || url.trim().isEmpty()) url = addressBar.getText().toString();
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, url);
        try {
            startActivity(Intent.createChooser(send, "Compartilhar com"));
        } catch (Exception e) {
            Toast.makeText(this, "Não foi possível compartilhar", Toast.LENGTH_SHORT).show();
        }
    }

    private void showAboutSheet() {
        Toast.makeText(this, "Aetheris Nova 0.1.1 — Glass Orbit UI", Toast.LENGTH_LONG).show();
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
