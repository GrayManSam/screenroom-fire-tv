package com.geekedsilicon.tvhub;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.view.Display;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Locale;
import android.view.KeyEvent;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import java.io.IOException;

public final class HubActivity extends Activity {
    private static final String ORIGIN = "https://screenroom.invalid";
    private static final String[] NAMES = {"MovieBox", "Jellyfin", "YouTube"};
    private static final String[] PACKAGES = {
        "com.movieboxpro.androidtv", "org.jellyfin.androidtv", "com.amazon.firetv.youtube"
    };
    private WebView page;
    private ToneGenerator clickTone;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        page = new WebView(this);
        page.setBackgroundColor(0xff080910);
        page.getSettings().setJavaScriptEnabled(true);
        page.getSettings().setAllowFileAccess(false);
        page.getSettings().setAllowContentAccess(false);
        page.setWebViewClient(new LocalAssets());
        page.addJavascriptInterface(new AppActions(), "Screenroom");
        setContentView(page);
        page.loadUrl(ORIGIN + "/index.html");
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        String action;
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_DPAD_LEFT: action = "navigate('left')"; break;
            case KeyEvent.KEYCODE_DPAD_RIGHT: action = "navigate('right')"; break;
            case KeyEvent.KEYCODE_DPAD_UP: action = "navigate('up')"; break;
            case KeyEvent.KEYCODE_DPAD_DOWN: action = "navigate('down')"; break;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER: action = "activate()"; break;
            default: return super.dispatchKeyEvent(event);
        }
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            page.evaluateJavascript("window." + action, null);
        }
        return true;
    }

    @Override public void onBackPressed() {
        page.evaluateJavascript("document.getElementById('preferences').hidden", result -> {
            if ("false".equals(result)) {
                page.evaluateJavascript("window.closeSettings()", null);
            } else {
                finish();
            }
        });
    }

    private class LocalAssets extends WebViewClient {
        @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            return true;
        }

        @Override public WebResourceResponse shouldInterceptRequest(
            WebView view, WebResourceRequest request
        ) {
            String url = request.getUrl().toString();
            String type;
            if (url.equals(ORIGIN + "/index.html")) {
                type = "text/html";
            } else if (url.equals(ORIGIN + "/screenroom.css")) {
                type = "text/css";
            } else if (url.equals(ORIGIN + "/screenroom.js")) {
                type = "text/javascript";
            } else if (url.matches(java.util.regex.Pattern.quote(ORIGIN) + "/icons/[a-z0-9-]+\\.svg")) {
                type = "image/svg+xml";
            } else {
                return new WebResourceResponse("text/plain", "UTF-8", null);
            }

            try {
                return new WebResourceResponse(
                    type, "UTF-8", getAssets().open(url.substring(ORIGIN.length() + 1))
                );
            } catch (IOException error) {
                return new WebResourceResponse("text/plain", "UTF-8", null);
            }
        }
    }

    private class AppActions {
        @JavascriptInterface public boolean soundsEnabled() {
            return getPreferences(MODE_PRIVATE).getBoolean("interfaceSounds", true);
        }

        @JavascriptInterface public void setSoundsEnabled(boolean enabled) {
            getPreferences(MODE_PRIVATE).edit().putBoolean("interfaceSounds", enabled).apply();
        }

        @JavascriptInterface public void playClick() {
            runOnUiThread(() -> {
                if (!soundsEnabled()) return;
                try {
                    if (clickTone == null) {
                        clickTone = new ToneGenerator(AudioManager.STREAM_MUSIC, 14);
                    }
                    clickTone.startTone(ToneGenerator.TONE_PROP_BEEP, 42);
                } catch (RuntimeException ignored) {
                    // Interface sounds are optional on devices without an active audio route.
                }
            });
        }

        @JavascriptInterface public boolean highContrast() {
            return getPreferences(MODE_PRIVATE).getBoolean("highContrast", false);
        }

        @JavascriptInterface public void setHighContrast(boolean enabled) {
            getPreferences(MODE_PRIVATE).edit().putBoolean("highContrast", enabled).apply();
        }

        @JavascriptInterface public String appearance() {
            return getPreferences(MODE_PRIVATE).getString("appearance", "app");
        }

        @JavascriptInterface public void setAppearance(String value) {
            if ("app".equals(value) || "indigo".equals(value)
                    || "ocean".equals(value) || "ember".equals(value)) {
                getPreferences(MODE_PRIVATE).edit().putString("appearance", value).apply();
            }
        }

        @JavascriptInterface public String textSize() {
            return getPreferences(MODE_PRIVATE).getString("textSize", "standard");
        }

        @JavascriptInterface public void setTextSize(String value) {
            if ("standard".equals(value) || "large".equals(value) || "extra-large".equals(value)) {
                getPreferences(MODE_PRIVATE).edit().putString("textSize", value).apply();
            }
        }

        @JavascriptInterface public String audioOutputs() {
            AudioManager audio = getSystemService(AudioManager.class);
            if (audio == null) return "Unavailable";
            Set<String> outputs = new LinkedHashSet<>();
            for (AudioDeviceInfo device : audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
                switch (device.getType()) {
                    case AudioDeviceInfo.TYPE_HDMI: outputs.add("HDMI"); break;
                    case AudioDeviceInfo.TYPE_HDMI_ARC: outputs.add("HDMI ARC"); break;
                    case AudioDeviceInfo.TYPE_BLUETOOTH_A2DP: outputs.add("Bluetooth"); break;
                    case AudioDeviceInfo.TYPE_BUILTIN_SPEAKER: outputs.add("Built-in speaker"); break;
                    case AudioDeviceInfo.TYPE_WIRED_HEADPHONES:
                    case AudioDeviceInfo.TYPE_WIRED_HEADSET: outputs.add("Wired headphones"); break;
                    case AudioDeviceInfo.TYPE_USB_DEVICE:
                    case AudioDeviceInfo.TYPE_USB_HEADSET: outputs.add("USB audio"); break;
                    default: outputs.add("Other output");
                }
            }
            return outputs.isEmpty() ? "None reported" : String.join(" · ", outputs);
        }

        @JavascriptInterface public String displayOutput() {
            Display.Mode mode = getWindowManager().getDefaultDisplay().getMode();
            return String.format(Locale.US, "%d × %d at %.2f Hz",
                mode.getPhysicalWidth(), mode.getPhysicalHeight(), mode.getRefreshRate());
        }

        @JavascriptInterface public String displayHdr() {
            Display.HdrCapabilities capabilities =
                getWindowManager().getDefaultDisplay().getHdrCapabilities();
            if (capabilities == null) return "Unavailable";

            List<String> formats = new ArrayList<>();
            for (int type : capabilities.getSupportedHdrTypes()) {
                switch (type) {
                    case Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION:
                        formats.add("Dolby Vision");
                        break;
                    case Display.HdrCapabilities.HDR_TYPE_HDR10:
                        formats.add("HDR10");
                        break;
                    case Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS:
                        formats.add("HDR10+");
                        break;
                    case Display.HdrCapabilities.HDR_TYPE_HLG:
                        formats.add("HLG");
                        break;
                    default:
                        formats.add("Other HDR");
                }
            }
            return formats.isEmpty() ? "None reported" : String.join(" · ", formats);
        }

        @JavascriptInterface public String mediaVolume() {
            AudioManager audio = getSystemService(AudioManager.class);
            if (audio == null) return "Unavailable";
            if (audio.isVolumeFixed()) return "Controlled by TV or receiver";
            return audio.getStreamVolume(AudioManager.STREAM_MUSIC) + " of "
                + audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        }

        @JavascriptInterface public void openApp(int index) {
            if (index >= 0 && index < PACKAGES.length) {
                runOnUiThread(() -> open(index));
            }
        }
    }

    private void open(int index) {
        PackageManager manager = getPackageManager();
        Intent intent = manager.getLeanbackLaunchIntentForPackage(PACKAGES[index]);
        if (intent == null) intent = manager.getLaunchIntentForPackage(PACKAGES[index]);
        if (intent != null) {
            try {
                startActivity(intent);
                return;
            } catch (ActivityNotFoundException | SecurityException ignored) {
                // The installed app may have changed since activity resolution.
            }
        }
        Toast.makeText(this, NAMES[index] + " couldn’t open", Toast.LENGTH_SHORT).show();
    }

    @Override protected void onDestroy() {
        page.removeJavascriptInterface("Screenroom");
        page.destroy();
        if (clickTone != null) clickTone.release();
        super.onDestroy();
    }
}
