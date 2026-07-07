package com.swlab.aetherisnova;

import java.nio.charset.StandardCharsets;

import java.io.InputStream;

import java.io.ByteArrayOutputStream;

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
import android.widget.ImageButton;
import android.widget.ImageView;
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
    private ImageView addressIcon;
    private FrameLayout root;
    private View homeOverlay;
    private LinearLayout topBar;
    private LinearLayout bottomDock;
    private ProgressBar loadProgress;
    private TextView pageState;
    private ImageButton reloadButton;
    private PermissionRequest pendingPermissionRequest;

    private String currentTitle = "Aetheris";
    private boolean pageIsLoading = false;

    private final int voidColor = Color.rgb(5, 9, 20);
    private final int toolbarColor = Color.argb(226, 9, 21, 37);
    private final int surfaceColor = Color.rgb(15, 27, 45);
    private final int surfaceSoft = Color.argb(210, 15, 27, 45);
    private final int orbitColor = Color.rgb(68, 232, 255);
    private final int orbitSoftColor = Color.rgb(154, 245, 255);
    private final int auroraColor = Color.rgb(108, 125, 255);
    private final int textColor = Color.rgb(246, 250, 255);
    private final int softTextColor = Color.rgb(174, 190, 210);
    private final int mutedTextColor = Color.rgb(126, 141, 163);
    private final int lineSoftColor = Color.argb(95, 68, 232, 255);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applySystemBars();
        buildInterface();
        configureWebView();

        Uri startUri = getIntent() != null ? getIntent().getData() : null;
        if (startUri != null) openUrl(startUri.toString());
        else showHome();
    }

    private void applySystemBars() {
        Window window = getWindow();
        if (window != null) {
            window.setStatusBarColor(voidColor);
            window.setNavigationBarColor(voidColor);
            if (Build.VERSION.SDK_INT >= 23) window.getDecorView().setSystemUiVisibility(0);
        }
    }

    private void buildInterface() {
        root = new FrameLayout(this);
        root.setBackgroundColor(voidColor);
        setContentView(root);

        webView = new WebView(this);
        webView.setBackgroundColor(voidColor);
        root.addView(webView, new FrameLayout.LayoutParams(-1, -1));

        homeOverlay = createHomeOverlay();
        root.addView(homeOverlay, new FrameLayout.LayoutParams(-1, -1));

        topBar = createTopBar();
        FrameLayout.LayoutParams topParams = new FrameLayout.LayoutParams(-1, dp(54), Gravity.TOP);
        topParams.setMargins(dp(10), dp(8), dp(10), 0);
        root.addView(topBar, topParams);

        loadProgress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        loadProgress.setMax(100);
        loadProgress.setVisibility(View.INVISIBLE);
        FrameLayout.LayoutParams progressParams = new FrameLayout.LayoutParams(-1, dp(2), Gravity.TOP);
        progressParams.setMargins(dp(68), dp(64), dp(68), 0);
        root.addView(loadProgress, progressParams);

        pageState = new TextView(this);
        pageState.setTextColor(mutedTextColor);
        pageState.setTextSize(11);
        pageState.setGravity(Gravity.CENTER);
        pageState.setSingleLine(true);
        pageState.setVisibility(View.GONE);
        FrameLayout.LayoutParams stateParams = new FrameLayout.LayoutParams(-1, dp(18), Gravity.TOP);
        stateParams.setMargins(dp(70), dp(66), dp(70), 0);
        root.addView(pageState, stateParams);

        bottomDock = createBottomDock();
        FrameLayout.LayoutParams dockParams = new FrameLayout.LayoutParams(-1, dp(58), Gravity.BOTTOM);
        dockParams.setMargins(dp(18), 0, dp(18), dp(14));
        root.addView(bottomDock, dockParams);
    }

    private LinearLayout createTopBar() {
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setBackgroundColor(Color.TRANSPARENT);

        TextView mark = new TextView(this);
        mark.setText("A");
        mark.setGravity(Gravity.CENTER);
        mark.setTextColor(voidColor);
        mark.setTypeface(Typeface.DEFAULT_BOLD);
        mark.setTextSize(18);
        mark.setBackground(roundGradient(orbitSoftColor, auroraColor, dp(18), Color.TRANSPARENT, 0));
        mark.setOnClickListener(v -> showHome());
        LinearLayout.LayoutParams markParams = new LinearLayout.LayoutParams(dp(44), dp(44));
        markParams.rightMargin = dp(8);
        top.addView(mark, markParams);

        LinearLayout addressShell = new LinearLayout(this);
        addressShell.setOrientation(LinearLayout.HORIZONTAL);
        addressShell.setGravity(Gravity.CENTER_VERTICAL);
        addressShell.setPadding(dp(12), 0, dp(8), 0);
        addressShell.setBackground(roundDrawable(toolbarColor, dp(22), lineSoftColor, 1));
        top.addView(addressShell, new LinearLayout.LayoutParams(0, dp(44), 1));

        addressIcon = new ImageView(this);
        addressIcon.setImageResource(R.drawable.ic_aetheris_search);
        addressIcon.setColorFilter(softTextColor);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(22), dp(22));
        iconParams.rightMargin = dp(8);
        addressShell.addView(addressIcon, iconParams);

        addressBar = new EditText(this);
        addressBar.setSingleLine(true);
        addressBar.setTextColor(textColor);
        addressBar.setHintTextColor(mutedTextColor);
        addressBar.setTextSize(14);
        addressBar.setHint("Pesquisar ou digitar endereço");
        addressBar.setPadding(0, 0, 0, 0);
        addressBar.setSelectAllOnFocus(true);
        addressBar.setImeOptions(EditorInfo.IME_ACTION_GO);
        addressBar.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_URI);
        addressBar.setBackgroundColor(Color.TRANSPARENT);
        addressBar.setOnEditorActionListener((v, actionId, event) -> {
            boolean enter = event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP;
            if (actionId == EditorInfo.IME_ACTION_GO || enter) {
                openUrl(addressBar.getText().toString());
                return true;
            }
            return false;
        });
        addressShell.addView(addressBar, new LinearLayout.LayoutParams(0, -1, 1));

        ImageButton go = iconButton(R.drawable.ic_aetheris_go, "Ir");
        go.setOnClickListener(v -> openUrl(addressBar.getText().toString()));
        LinearLayout.LayoutParams goParams = new LinearLayout.LayoutParams(dp(44), dp(44));
        goParams.leftMargin = dp(8);
        top.addView(go, goParams);

        return top;
    }

    private View createHomeOverlay() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(voidColor);

        LinearLayout home = new LinearLayout(this);
        home.setOrientation(LinearLayout.VERTICAL);
        home.setGravity(Gravity.CENTER_HORIZONTAL);
        home.setPadding(dp(16), dp(96), dp(16), dp(96));
        scroll.addView(home, new ScrollView.LayoutParams(-1, -2));

        TextView brand = new TextView(this);
        brand.setText("Aetheris");
        brand.setTextColor(textColor);
        brand.setTextSize(32);
        brand.setTypeface(Typeface.DEFAULT_BOLD);
        brand.setGravity(Gravity.CENTER);
        home.addView(brand, new LinearLayout.LayoutParams(-1, -2));

        TextView subtitle = label("Navegação limpa. Funções reais. Interface própria.", 14, softTextColor);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(-1, -2);
        subParams.topMargin = dp(8);
        home.addView(subtitle, subParams);

        LinearLayout searchCard = new LinearLayout(this);
        searchCard.setOrientation(LinearLayout.HORIZONTAL);
        searchCard.setGravity(Gravity.CENTER_VERTICAL);
        searchCard.setPadding(dp(15), dp(8), dp(8), dp(8));
        searchCard.setBackground(roundDrawable(surfaceSoft, dp(24), Color.TRANSPARENT, 0));
        LinearLayout.LayoutParams searchCardParams = new LinearLayout.LayoutParams(-1, dp(58));
        searchCardParams.topMargin = dp(24);
        home.addView(searchCard, searchCardParams);

        ImageView searchIcon = new ImageView(this);
        searchIcon.setImageResource(R.drawable.ic_aetheris_search);
        searchIcon.setColorFilter(softTextColor);
        LinearLayout.LayoutParams siParams = new LinearLayout.LayoutParams(dp(22), dp(22));
        siParams.rightMargin = dp(10);
        searchCard.addView(searchIcon, siParams);

        EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setTextColor(textColor);
        search.setHintTextColor(mutedTextColor);
        search.setTextSize(16);
        search.setHint("Buscar na web");
        search.setPadding(0, 0, dp(8), 0);
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

        ImageButton searchGo = iconButton(R.drawable.ic_aetheris_go, "Pesquisar");
        searchGo.setOnClickListener(v -> openUrl(search.getText().toString()));
        searchCard.addView(searchGo, new LinearLayout.LayoutParams(dp(42), dp(42)));

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(2);
        LinearLayout.LayoutParams gridParams = new LinearLayout.LayoutParams(-1, -2);
        gridParams.topMargin = dp(18);
        home.addView(grid, gridParams);

        grid.addView(quickTile("Google", "Busca", "https://www.google.com", R.drawable.ic_aetheris_search));
        grid.addView(quickTile("YouTube", "Vídeos", "https://www.youtube.com", R.drawable.ic_aetheris_go));
        grid.addView(quickTile("Duck", "Privacidade", "https://duckduckgo.com", R.drawable.ic_aetheris_lock));
        grid.addView(quickTile("Downloads", "Arquivos", "aetheris://downloads", R.drawable.ic_aetheris_download));

        TextView status = label("Aetheris Nova 0.1.3-r1 • shell flutuante corrigido", 12, mutedTextColor);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-1, -2);
        statusParams.topMargin = dp(20);
        home.addView(status, statusParams);

        return scroll;
    }

    private View quickTile(String title, String caption, String url, int iconRes) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.HORIZONTAL);
        tile.setGravity(Gravity.CENTER_VERTICAL);
        tile.setPadding(dp(14), dp(10), dp(14), dp(10));
        tile.setBackground(roundDrawable(surfaceSoft, dp(18), Color.TRANSPARENT, 0));
        tile.setOnClickListener(v -> {
            if ("aetheris://downloads".equals(url)) openDownloads();
            else openUrl(url);
        });

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = dp(68);
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(dp(5), dp(5), dp(5), dp(5));
        tile.setLayoutParams(params);

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(orbitSoftColor);
        LinearLayout.LayoutParams iParams = new LinearLayout.LayoutParams(dp(23), dp(23));
        iParams.rightMargin = dp(10);
        tile.addView(icon, iParams);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        tile.addView(texts, new LinearLayout.LayoutParams(0, -1, 1));

        TextView titleView = label(title, 15, textColor);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setGravity(Gravity.LEFT);
        texts.addView(titleView);

        TextView captionView = label(caption, 11, mutedTextColor);
        captionView.setGravity(Gravity.LEFT);
        texts.addView(captionView);

        return tile;
    }

    private LinearLayout createBottomDock() {
        LinearLayout dock = new LinearLayout(this);
        dock.setOrientation(LinearLayout.HORIZONTAL);
        dock.setGravity(Gravity.CENTER);
        dock.setPadding(dp(8), dp(6), dp(8), dp(6));
        dock.setBackground(roundDrawable(toolbarColor, dp(22), lineSoftColor, 1));

        ImageButton back = dockIcon(R.drawable.ic_aetheris_back, "Voltar");
        back.setOnClickListener(v -> {
            if (webView.canGoBack()) webView.goBack();
            else showHome();
        });

        ImageButton forward = dockIcon(R.drawable.ic_aetheris_forward, "Avançar");
        forward.setOnClickListener(v -> {
            if (webView.canGoForward()) webView.goForward();
            else toast("Sem próxima página");
        });

        ImageButton home = dockIcon(R.drawable.ic_aetheris_home, "Página inicial");
        home.setOnClickListener(v -> showHome());

        reloadButton = dockIcon(R.drawable.ic_aetheris_refresh, "Recarregar");
        reloadButton.setOnClickListener(v -> {
            if (pageIsLoading) {
                webView.stopLoading();
                updateLoadingUi(false, 0);
            } else if (homeOverlay.getVisibility() == View.VISIBLE) {
                toast("Página inicial");
            } else {
                webView.reload();
            }
        });

        ImageButton menu = dockIcon(R.drawable.ic_aetheris_menu, "Menu");
        menu.setOnClickListener(v -> showMenuSheet());

        dock.addView(back);
        dock.addView(forward);
        dock.addView(home);
        dock.addView(reloadButton);
        dock.addView(menu);

        return dock;
    }

    private ImageButton iconButton(int resId, String description) {
        ImageButton button = new ImageButton(this);
        button.setImageResource(resId);
        button.setColorFilter(voidColor);
        button.setContentDescription(description);
        button.setScaleType(ImageView.ScaleType.CENTER);
        button.setPadding(dp(10), dp(10), dp(10), dp(10));
        button.setBackground(roundGradient(orbitSoftColor, auroraColor, dp(18), Color.TRANSPARENT, 0));
        return button;
    }

    private ImageButton dockIcon(int resId, String description) {
        ImageButton button = new ImageButton(this);
        button.setImageResource(resId);
        button.setColorFilter(textColor);
        button.setContentDescription(description);
        button.setScaleType(ImageView.ScaleType.CENTER);
        button.setPadding(dp(13), dp(13), dp(13), dp(13));
        button.setBackgroundColor(Color.TRANSPARENT);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1);
        params.leftMargin = dp(2);
        params.rightMargin = dp(2);
        button.setLayoutParams(params);
        return button;
    }

    private TextView label(String text, int sp, int color) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextColor(color);
        label.setTextSize(sp);
        label.setGravity(Gravity.CENTER);
        return label;
    }

    private GradientDrawable roundDrawable(int color, int radius, int strokeColor, int strokeWidthDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        if (strokeWidthDp > 0) drawable.setStroke(dp(strokeWidthDp), strokeColor);
        return drawable;
    }

    private GradientDrawable roundGradient(int startColor, int endColor, int radius, int strokeColor, int strokeWidthDp) {
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{startColor, endColor});
        drawable.setCornerRadius(radius);
        if (strokeWidthDp > 0) drawable.setStroke(dp(strokeWidthDp), strokeColor);
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
        if (Build.VERSION.SDK_INT >= 21) settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);

        CookieManager.getInstance().setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= 21) CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.addJavascriptInterface(new AetherisVideoBridge(this), "AetherisBridge");

        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) { return handleUrl(request.getUrl().toString()); }
            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) { return handleUrl(url); }

            @Override public void onPageStarted(WebView view, String url, Bitmap favicon) {
                homeOverlay.setVisibility(View.GONE);
                pageIsLoading = true;
                addressBar.setText(url == null ? "" : url);
                updateAddressIcon(url);
                updatePageState("Carregando…");
                updateLoadingUi(true, 5);
                injectWebRtcFilterSoon();
                super.onPageStarted(view, url, favicon);
            }

            @Override public void onPageFinished(WebView view, String url) {
                pageIsLoading = false;
                addressBar.setText(url == null ? "" : url);
                updateAddressIcon(url);
                updatePageState(currentTitle);
                updateLoadingUi(false, 100);
                injectWebRtcFilterNow();
                super.onPageFinished(view, url);
            }

            @Override public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                pageIsLoading = false;
                updateLoadingUi(false, 0);
                updatePageState("Erro ao carregar");
                toast("Erro ao carregar página");
                super.onReceivedError(view, errorCode, description, failingUrl);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView view, int newProgress) {
                updateLoadingUi(newProgress < 100, newProgress);
                if (newProgress < 100) updatePageState("Carregando " + newProgress + "%");
                else updatePageState(currentTitle);
                super.onProgressChanged(view, newProgress);
            }

            @Override public void onReceivedTitle(WebView view, String title) {
                if (title != null && !title.trim().isEmpty()) {
                    currentTitle = title.trim();
                    updatePageState(currentTitle);
                }
                super.onReceivedTitle(view, title);
            }

            @Override public void onPermissionRequest(PermissionRequest request) { handleWebPermissionRequest(request); }
        });

        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> startDownload(url, userAgent, contentDisposition, mimeType));
    }

    private void updateLoadingUi(boolean loading, int progress) {
        pageIsLoading = loading;
        if (loadProgress != null) {
            loadProgress.setVisibility(loading ? View.VISIBLE : View.INVISIBLE);
            loadProgress.setProgress(Math.max(0, Math.min(100, progress)));
        }
        if (reloadButton != null) {
            reloadButton.setImageResource(loading ? R.drawable.ic_aetheris_stop : R.drawable.ic_aetheris_refresh);
            reloadButton.setContentDescription(loading ? "Parar carregamento" : "Recarregar");
        }
    }

    private void updatePageState(String text) {
        if (pageState == null) return;
        pageState.setText(text == null || text.trim().isEmpty() ? "Aetheris" : text.trim());
        pageState.setVisibility(homeOverlay != null && homeOverlay.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
    }

    private void updateAddressIcon(String url) {
        if (addressIcon == null) return;
        if (url != null && url.toLowerCase(Locale.US).startsWith("https://")) {
            addressIcon.setImageResource(R.drawable.ic_aetheris_lock);
            addressIcon.setColorFilter(orbitSoftColor);
        } else {
            addressIcon.setImageResource(R.drawable.ic_aetheris_search);
            addressIcon.setColorFilter(softTextColor);
        }
    }

    private void bringChromeToFront() {
        if (topBar != null) topBar.bringToFront();
        if (loadProgress != null) loadProgress.bringToFront();
        if (pageState != null) pageState.bringToFront();
        if (bottomDock != null) bottomDock.bringToFront();
    }

    private void showHome() {
        currentTitle = "Página inicial";
        addressBar.setText("");
        updateAddressIcon("");
        updateLoadingUi(false, 0);
        if (pageState != null) pageState.setVisibility(View.GONE);
        homeOverlay.setVisibility(View.VISIBLE);
        homeOverlay.setBackgroundColor(voidColor);
        homeOverlay.bringToFront();
        bringChromeToFront();
    }

    private void openUrl(String input) {
        String url = normalizeInput(input);
        homeOverlay.setVisibility(View.GONE);
        updatePageState("Preparando navegação…");
        updateAddressIcon(url);
        addressBar.setText(url);
        bringChromeToFront();
        webView.loadUrl(url);
        injectWebRtcFilterSoon();
    }

    private String normalizeInput(String input) {
        String value = input == null ? "" : input.trim();
        if (value.isEmpty()) return "https://www.google.com";
        String lower = value.toLowerCase(Locale.US);
        if ("google".equals(lower)) return "https://www.google.com";
        if ("youtube".equals(lower)) return "https://www.youtube.com";
        if ("duck".equals(lower) || "duckduckgo".equals(lower)) return "https://duckduckgo.com";
        if (lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("file://")) return value;
        if (value.contains(".") && !value.contains(" ")) return "https://" + value;
        return "https://www.google.com/search?q=" + Uri.encode(value);
    }

    private boolean handleUrl(String url) {
        if (url == null) return false;
        Uri uri;
        try { uri = Uri.parse(url); } catch (Exception e) { return false; }
        String scheme = uri.getScheme();
        if (scheme == null) return false;
        String normalized = scheme.toLowerCase(Locale.US);

        if ("http".equals(normalized) || "https".equals(normalized)) {
            if (isYoutube(uri)) return openWithPackageOrStay(uri, "com.google.android.youtube");
            return false;
        }

        if ("intent".equals(normalized)) return openIntentUrl(url);

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
        try { startActivity(intent); return true; }
        catch (ActivityNotFoundException e) { toast("Nenhum app encontrado para abrir esse link"); return true; }
        catch (Exception e) { return false; }
    }

    private void handleWebPermissionRequest(PermissionRequest request) {
        if (Build.VERSION.SDK_INT < 21) return;
        boolean needsCamera = false, needsAudio = false;
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
        if (Build.VERSION.SDK_INT >= 23) requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, REQ_WEB_PERMISSIONS);
        else request.deny();
    }

    private boolean checkSelfPermissionCompat(String permission) {
        if (Build.VERSION.SDK_INT < 23) return true;
        return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_WEB_PERMISSIONS && pendingPermissionRequest != null) {
            boolean granted = true;
            for (int result : grantResults) if (result != PackageManager.PERMISSION_GRANTED) granted = false;
            if (granted) pendingPermissionRequest.grant(pendingPermissionRequest.getResources());
            else pendingPermissionRequest.deny();
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
        panel.setPadding(dp(18), dp(14), dp(18), dp(18));
        panel.setBackground(roundDrawable(toolbarColor, dp(22), Color.TRANSPARENT, 0));

        TextView title = label("Menu do navegador", 20, textColor);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.LEFT);
        panel.addView(title);

        String current = webView.getUrl();
        if (current == null || current.trim().isEmpty() || homeOverlay.getVisibility() == View.VISIBLE) current = "Página inicial";
        TextView url = label(current, 12, mutedTextColor);
        url.setSingleLine(true);
        url.setGravity(Gravity.LEFT);
        LinearLayout.LayoutParams urlParams = new LinearLayout.LayoutParams(-1, -2);
        urlParams.topMargin = dp(4);
        urlParams.bottomMargin = dp(10);
        panel.addView(url, urlParams);

        panel.addView(menuRow(R.drawable.ic_aetheris_home, "Nova busca", "Abrir a home do Aetheris", () -> { dialog.dismiss(); showHome(); }));
        panel.addView(menuRow(R.drawable.ic_aetheris_copy, "Copiar link", "Copiar endereço atual", () -> { dialog.dismiss(); copyCurrentUrl(); }));
        panel.addView(menuRow(R.drawable.ic_aetheris_share, "Compartilhar", "Enviar link para outro app", () -> { dialog.dismiss(); shareCurrentUrl(); }));
        panel.addView(menuRow(R.drawable.ic_aetheris_refresh, "Recarregar", "Atualizar a página aberta", () -> { dialog.dismiss(); if (homeOverlay.getVisibility() == View.VISIBLE) toast("Página inicial"); else webView.reload(); }));
        panel.addView(menuRow(R.drawable.ic_aetheris_download, "Downloads", "Abrir downloads do Android", () -> { dialog.dismiss(); openDownloads(); }));
        panel.addView(menuRow(R.drawable.ic_aetheris_filter, "Efeitos de vídeo", "Filtro atual: " + AetherisVideoBridge.getVideoFilterLabel(), () -> { dialog.dismiss(); showVideoEffectsSheet(); }));
        panel.addView(menuRow(R.drawable.ic_aetheris_info, "Sobre", "Aetheris Nova 0.1.3-r1", () -> { dialog.dismiss(); toast("Aetheris Nova 0.1.3-r1"); }));

        dialog.setContentView(panel);

        dialog.setOnShowListener(d -> {
            Window w = dialog.getWindow();
            if (w != null) {
                w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                w.setDimAmount(0.45f);
                w.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                w.setGravity(Gravity.BOTTOM);
                w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        });

        dialog.show();
    }

    private View menuRow(int iconRes, String title, String caption, final Runnable action) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), dp(10), dp(10), dp(10));
        row.setBackgroundColor(Color.TRANSPARENT);
        row.setOnClickListener(v -> action.run());

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(3);
        row.setLayoutParams(params);

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(orbitSoftColor);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(24), dp(24));
        iconParams.rightMargin = dp(12);
        row.addView(icon, iconParams);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        row.addView(texts, new LinearLayout.LayoutParams(0, -2, 1));

        TextView titleView = label(title, 15, textColor);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setGravity(Gravity.LEFT);
        texts.addView(titleView);

        TextView captionView = label(caption == null ? "" : caption, 12, mutedTextColor);
        captionView.setSingleLine(true);
        captionView.setGravity(Gravity.LEFT);
        texts.addView(captionView);

        return row;
    }


    private void injectWebRtcFilterSoon() {
        if (webView == null) return;
        webView.postDelayed(() -> injectWebRtcFilterNow(), 450);
        webView.postDelayed(() -> injectWebRtcFilterNow(), 1200);
    }

    private void injectWebRtcFilterNow() {
        if (webView == null) return;
        if (Build.VERSION.SDK_INT < 19) return;

        try {
            InputStream inputStream = getAssets().open("aetheris_webrtc_filter.js");
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int length;

            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }

            inputStream.close();

            String script = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
            webView.evaluateJavascript(script, null);
        } catch (Exception ignored) {
        }
    }

    private void showVideoEffectsSheet() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(18), dp(14), dp(18), dp(18));
        panel.setBackground(roundDrawable(toolbarColor, dp(22), Color.TRANSPARENT, 0));

        TextView title = label("Efeitos de vídeo", 20, textColor);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.LEFT);
        panel.addView(title);

        TextView info = label("Experimental: tenta aplicar filtro real no vídeo enviado pelo WebRTC. Não é overlay falso.", 12, mutedTextColor);
        info.setGravity(Gravity.LEFT);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(-1, -2);
        infoParams.topMargin = dp(6);
        infoParams.bottomMargin = dp(10);
        panel.addView(info, infoParams);

        panel.addView(videoFilterRow(dialog, "Desligado", "off", "Usa câmera normal, sem modificação."));
        panel.addView(videoFilterRow(dialog, "Cinza", "gray", "Converte a câmera para preto e branco."));
        panel.addView(videoFilterRow(dialog, "Frio", "cool", "Ajuste azul/frio leve no vídeo."));
        panel.addView(videoFilterRow(dialog, "Contraste", "contrast", "Aumenta contraste de forma leve."));
        panel.addView(videoFilterRow(dialog, "Orbit leve", "orbit", "Filtro visual leve com marca orbital discreta."));

        dialog.setContentView(panel);

        dialog.setOnShowListener(d -> {
            Window w = dialog.getWindow();
            if (w != null) {
                w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                w.setDimAmount(0.45f);
                w.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                w.setGravity(Gravity.BOTTOM);
                w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        });

        dialog.show();
    }

    private View videoFilterRow(Dialog dialog, String title, String mode, String caption) {
        String current = AetherisVideoBridge.getVideoFilterModeValue();
        String label = current.equals(mode) ? caption + "  • ativo" : caption;

        return menuRow(R.drawable.ic_aetheris_filter, title, label, () -> {
            AetherisVideoBridge.setVideoFilterMode(mode);
            injectWebRtcFilterNow();
            dialog.dismiss();

            if ("off".equals(mode)) {
                toast("Efeitos de vídeo desligados");
            } else {
                toast("Filtro " + title + " ativo para próximas capturas de câmera");
            }
        });
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
        try { startActivity(Intent.createChooser(send, "Compartilhar com")); }
        catch (Exception e) { toast("Não foi possível compartilhar"); }
    }

    private void openDownloads() {
        try { startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)); }
        catch (Exception e) { toast("Abra a pasta Download no gerenciador de arquivos"); }
    }

    @Override public void onBackPressed() {
        if (homeOverlay != null && homeOverlay.getVisibility() == View.VISIBLE) {
            super.onBackPressed();
            return;
        }
        if (webView != null && webView.canGoBack()) webView.goBack();
        else showHome();
    }

    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_SHORT).show(); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
