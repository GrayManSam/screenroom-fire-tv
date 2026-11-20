# Screenroom

Screenroom is a standalone Android TV hub for MovieBox, Jellyfin, and YouTube. Use left and right to move between tiles and Select to open the focused app. The app needs no network permission and delegates playback to the installed clients.

The v1.1 interface is packaged from the HTML, CSS, and JavaScript in `assets/`. The Android activity renders it in Fire OS WebView and maps remote keys to the app dock.

## Check locally

From the repository root, run the offline checks with Python 3, Node.js, and a POSIX shell.

```sh
python3 scripts/check-screenroom.py
```

The check verifies bundled asset paths, HTML IDs referenced by JavaScript and accessibility labels, Android XML parsing, and shell and JavaScript syntax. Use the Fire TV compatibility checklist for runtime and remote-control tests.

## Build

Use Android SDK Platform 30, Android Build Tools 34, Java with a compiler, and a local signing key with alias `screenroom`. The script also supports an Eclipse Java compiler jar through `ECJ_JAR`.

```sh
export ANDROID_JAR=/path/to/platforms/android-30/android.jar
export ANDROID_BUILD_TOOLS=/path/to/build-tools/34.0.0
export HUB_KEYSTORE=/path/to/local/signing-keystore.p12
sh hub/build.sh
```

The signed APK is written to `artifacts/screenroom.apk`. Reuse the same local key when updating the installed package. Keep signing material out of version control.

## Open on the Stick

Launch **Screenroom** from the Fire TV apps list or use the Fedora shortcut.

```sh
python3 scripts/launch.py FIRE_TV_SERIAL screenroom
```

Amazon still handles the physical Home button. To roll back, uninstall Screenroom under **Settings → Applications → Manage Installed Applications**.

Icons are bundled from [Lucide 0.468.0](https://github.com/lucide-icons/lucide/tree/0.468.0). The license is included in `assets/icons/LICENSE`. See [Fire TV compatibility](FIRE_TV_COMPATIBILITY.md) for the current native feature coverage and device test status.

Screenroom 1.5 adds an Appearance control in Settings. Choose Follow app,
Indigo, Ocean, or Ember. The theme is saved locally and works alongside the
high-contrast preference.

Screenroom 1.6 adds saved Standard, Large, and Extra large text sizes and
lists the audio outputs Android reports. In Settings, press Down from
Text size to bring the diagnostics into view. Listed audio outputs describe
available Android endpoints, which can differ from the receiver's active route.
