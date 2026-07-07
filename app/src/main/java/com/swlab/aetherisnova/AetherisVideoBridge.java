package com.swlab.aetherisnova;

import android.content.Context;
import android.webkit.JavascriptInterface;

public final class AetherisVideoBridge {

    private static String videoFilterMode = "off";
    private final Context context;

    public AetherisVideoBridge(Context context) {
        this.context = context;
    }

    @JavascriptInterface
    public String getVideoFilterMode() {
        return videoFilterMode;
    }

    @JavascriptInterface
    public String getVideoFilterLabelFromJs() {
        return getVideoFilterLabel();
    }

    public static void setVideoFilterMode(String mode) {
        if (mode == null) {
            videoFilterMode = "off";
            return;
        }

        switch (mode) {
            case "gray":
            case "cool":
            case "contrast":
            case "orbit":
            case "off":
                videoFilterMode = mode;
                break;
            default:
                videoFilterMode = "off";
                break;
        }
    }

    public static String getVideoFilterModeValue() {
        return videoFilterMode;
    }

    public static String getVideoFilterLabel() {
        switch (videoFilterMode) {
            case "gray":
                return "Cinza";
            case "cool":
                return "Frio";
            case "contrast":
                return "Contraste";
            case "orbit":
                return "Orbit leve";
            case "off":
            default:
                return "Desligado";
        }
    }
}
