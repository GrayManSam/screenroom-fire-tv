# Screenroom compatibility

Screenroom is the visual layer. Use native Fire TV surfaces for system functions and open the installed apps for playback, accounts, DRM, and subtitles.

| Capability | Access | Verification |
| --- | --- | --- |
| MovieBox, Jellyfin, and YouTube | Screenroom app dock | All three were verified on v1.1. Jellyfin and return to YouTube confirmed after the v1.2 update. |
| Screenroom settings | Screenroom gear | Verified in v1.3. Sound and high contrast are app preferences. Display mode and Android media volume are live readouts. |
| Home, search, Alexa, app library, profiles, and updates | Amazon Home and Settings | Amazon still resolves as HOME. Individual features require a TV-side pass. |
| Remote volume, power, and HDMI-CEC | Fire TV remote and connected LG/audio equipment | Test on the physical setup. |
| Interface feedback | Sound button, independent of playback settings | Short local click and persisted toggle implemented. Audible output needs a listening check. |
| Accessibility | Labeled buttons, pressed state for sound, visible focus, contrast preferences, and D-pad/keyboard navigation | TV navigation tested over ADB. Screen reader and physical-remote checks remain. |
| Mobile | Stacked responsive layout with larger touch targets | 390-pixel browser viewport inspected. The mobile preview is a layout test. Phone-to-TV control would require a separate connection layer. |

Each future shortcut should open the native surface where supported. Before relying on one, test opening it, Back behavior, and a second launch. Keep streaming and remote controls available through Amazon Home throughout the rollout.

## Settings implementation

Screenroom v1.3 keeps settings inside its own interface. The Android bridge uses local preferences for Screenroom options and reads the current display mode and media stream volume from Android. The media volume reading can differ from the LG or receiver volume.

For each additional setting, identify the platform API, its permissions, and the scope of the change. Add a writable control only when the bridge can read the original value, apply the change, and read it back on the Stick. Settings such as system updates, Amazon profiles, and device-wide display or network configuration remain under Fire OS control. Full OS replacement is outside this APK's current architecture.
