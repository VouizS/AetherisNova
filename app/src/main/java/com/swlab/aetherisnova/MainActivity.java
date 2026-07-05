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
import android.graphics.Bitmap;
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
import android.widget.ProgressBar;
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
    private ProgressBar loadProgress;
    private TextView pageState;
    private TextView homeStatus;
    private PermissionRequest pendingPermissionRequest;

    private String currentTitle = "Aetheris";
    private String currentUrl = "";
    private boolean pageIsLoading = false;

    private final int voidColor = Color.rgb(3, 8, 20);
    private final int nightColor = Color.rgb(6, 17, 31);
    private final int deepColor = Color.rgb(9, 24, 43);
    private final int glassColor = Color.argb(210, 12, 24, 42);
    private final int glassSoftColor = Color.argb(165, 18, 34, 56);
    private final int glassHighColor = Color.argb(230, 23, 45, 74);
    private final int orbitColor = Color.rgb(68, 232, 255);
    private final int orbitSoftColor = Color.rgb(154, 245, 255);
    private final int auroraColor = Color.rgb(108, 125, 255);
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

        shell.addView(createTopArea());

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

    private View createTopArea() {
        LinearLayout topArea = new LinearLayout(this);
        topArea.setOrientation(LinearLayout.VERTICAL);
        topArea.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));

        topArea.addView(createTopBar());

        loadProgress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        loadProgress.setMax(100);
        loadProgress.setProgress(0);
        loadProgress.setVisibility(View.INVISIBLE);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(-1, dp(3));
        progressParams.leftMargin = dp(62);
        progressParams.rightMargin = dp(72);
        progressParams.topMargin = dp(4);
        topArea.addView(loadProgress, progressParams);

        pageState = new TextView(this);
        pageState.setText("Página inicial");
        pageState.setTextColor(softTextColor);
        pageState.setTextSize(11);
        pageState.setSingleLine(true);
        pageState.setGravity(Gravity.CENTER);
        pageState.setVisibility(View.GONE);
        LinearLayout.LayoutParams stateParams = new LinearLayout.LayoutParams(-1, dp(18));
        stateParams.leftMargin = dp(64);
        stateParams.rightMargin = dp(72);
        topArea.addView(pageState, stateParams);

        return topArea;
    }

    private View createTopBar() {
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(58)));

        TextView mark = new TextView(this);
        mark.setText("A");
        mark.setGravity(Gravity.CENTER);
        mark.setTextColor(voidColor);
        mark.setTypeface(Typeface.DEFAULT_BOLD);
        mark.setTextSize(19);
        mark.setBackground(circleGradient());
        mark.setElevation(dp(8));
        mark.setOnClickListener(v -> showHome());
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

        TextView statusTitle = smallLabel("Aetheris Nova 0.1.2");
        statusTitle.setTextColor(orbitSoftColor);
        statusTitle.setTypeface(Typeface.DEFAULT_BOLD);
        status.addView(statusTitle);

        homeStatus = smallLabel("Interação do navegador melhorada: progresso, estado de página e menu expandido.");
        LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(-1, -2);
        bodyParams.topMargin = dp(6);
        status.addView(homeStatus, bodyParams);

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
            else showHome();
        });

        TextView forward = dockButton("›");
        forward.setOnClickListener(v -> {
            if (webView.canGoForward()) webView.goForward();
            else toast("Sem próxima página");
        });

        TextView home = dockButton("⌂");
        home.setOnClickListener(v -> showHome());

        TextView reload = dockButton("↻");
        reload.setOnClickListener(v -> {
            if (homeOverlay.getVisibility() == View.VISIBLE) {
                toast("Página inicial");
            } else {
                webView.reload();
            }
        });

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
        return new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{
                        Color.rgb(3, 8, 20),
                        Color.rgb(5, 13, 28),
                        Color.rgb(7, 20, 35)
                }
        );
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
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        if (Build.VERSION.SDK_INT >= 21) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }

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
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                pageIsLoading = true;
                currentUrl = url == null ? "" : url;
                addressBar.setText(currentUrl);
                updatePageState("Carregando…");
                setProgressVisible(true, 5);
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                pageIsLoading = false;
                currentUrl = url == null ? "" : url;
                addressBar.setText(currentUrl);
                updatePageState(currentTitle);
                setProgressVisible(false, 100);
                super.onPageFinished(view, url);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                pageIsLoading = false;
                setProgressVisible(false, 0);
                updatePageState("Erro ao carregar");
                toast("Erro ao carregar página");
                super.onReceivedError(view, errorCode, description, failingUrl);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                setProgressVisible(newProgress < 100, newProgress);
                if (newProgress < 100) {
                    updatePageState("Carregando " + newProgress + "%");
                } else {
                    updatePageState(currentTitle);
                }
                super.onProgressChanged(view, newProgress);
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                if (title != null && !title.trim().isEmpty()) {
                    currentTitle = title.trim();
                    updatePageState(currentTitle);
                }
                super.onReceivedTitle(view, title);
            }

            @Override
            public void onPermissionRequest(PermissionRequest request) {
                handleWebPermissionRequest(request);
            }
        });

        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> startDownload(url, userAgent, contentDisposition, mimeType));
    }

    private void setProgressVisible(boolean visible, int progress) {
        if (loadProgress == null) return;
        loadProgress.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        int safeProgress = Math.max(0, Math.min(100, progress));
        loadProgress.setProgress(safeProgress);
    }

    private void updatePageState(String text) {
        if (pageState == null) return;
        String safeText = text == null || text.trim().isEmpty() ? "Aetheris" : text.trim();
        pageState.setText(safeText);
        pageState.setVisibility(homeOverlay != null && homeOverlay.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
    }

    private void showHome() {
        currentTitle = "Página inicial";
        currentUrl = "";
        addressBar.setText("");
        setProgressVisible(false, 0);
        if (pageState != null) {
            pageState.setVisibility(View.GONE);
        }
        homeOverlay.setVisibility(View.VISIBLE);
    }

    private void openUrl(String input) {
        String url = normalizeInput(input);
        homeOverlay.setVisibility(View.GONE);
        updatePageState("Preparando navegação…");
        addressBar.setText(url);
        currentUrl = url;
        webView.loadUrl(url);
    }

    private String normalizeInput(String input) {
        String value = input == null ? "" : input.trim();
        if (value.isEmpty()) return "https://www.google.com";

        String lower = value.toLowerCase(Locale.US);

        if ("google".equals(lower)) return "https://www.google.com";
        if ("youtube".equals(lower)) return "https://www.youtube.com";
        if ("github".equals(lower)) return "https://github.com";
        if ("duck".equals(lower) || "duckduckgo".equals(lower)) return "https://duckduckgo.com";

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
            toast("Nenhum app encontrado para abrir esse link");
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
                toast("Download iniciado");
            }
        } catch (Exception e) {
            toast("Não foi possível iniciar o download");
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
        if (current == null || current.trim().isEmpty() || homeOverlay.getVisibility() == View.VISIBLE) current = "Página inicial";
        url.setText(current);
        url.setTextColor(softTextColor);
        url.setTextSize(12);
        url.setSingleLine(true);
        LinearLayout.LayoutParams urlParams = new LinearLayout.LayoutParams(-1, -2);
        urlParams.topMargin = dp(4);
        urlParams.bottomMargin = dp(12);
        panel.addView(url, urlParams);

        panel.addView(menuRow("Nova busca", "Abrir a home do Aetheris", () -> {
            dialog.dismiss();
            showHome();
        }));

        panel.addView(menuRow("Copiar link", "Copiar endereço atual", () -> {
            dialog.dismiss();
            copyCurrentUrl();
        }));

        panel.addView(menuRow("Compartilhar", "Enviar link para outro app", () -> {
            dialog.dismiss();
            shareCurrentUrl();
        }));

        panel.addView(menuRow("Recarregar", "Atualizar a página aberta", () -> {
            dialog.dismiss();
            if (homeOverlay.getVisibility() == View.VISIBLE) toast("Página inicial");
            else webView.reload();
        }));

        panel.addView(menuRow("Abrir downloads", "Ver arquivos baixados no Android", () -> {
            dialog.dismiss();
            openDownloads();
        }));

        panel.addView(menuRow("Estado da página", currentTitle, () -> {
            dialog.dismiss();
            toast(currentTitle);
        }));

        panel.addView(menuRow("Sobre", "Versão 0.1.2 Browser Interaction Core", () -> {
            dialog.dismiss();
            toast("Aetheris Nova 0.1.2 — Browser Interaction Core");
        }));

        dialog.setContentView(panel);

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
        captionView.setSingleLine(true);
        LinearLayout.LayoutParams capParams = new LinearLayout.LayoutParams(-1, -2);
        capParams.topMargin = dp(3);
        row.addView(captionView, capParams);

        return row;
    }

    private void copyCurrentUrl() {
        String url = webView.getUrl();
        if (url == null || url.trim().isEmpty()) url = addressBar.getText().toString();
        if (url == null || url.trim().isEmpty()) url = "Página inicial";
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("Aetheris URL", url));
            toast("Link copiado");
        }
    }

    private void shareCurrentUrl() {
        String url = webView.getUrl();
        if (url == null || url.trim().isEmpty()) url = addressBar.getText().toString();
        if (url == null || url.trim().isEmpty()) {
            toast("Nenhum link para compartilhar");
            return;
        }

        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, url);
        try {
            startActivity(Intent.createChooser(send, "Compartilhar com"));
        } catch (Exception e) {
            toast("Não foi possível compartilhar");
        }
    }

    private void openDownloads() {
        try {
            Intent intent = new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS);
            startActivity(intent);
        } catch (Exception e) {
            toast("Abra a pasta Download no gerenciador de arquivos");
        }
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

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}
