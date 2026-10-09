# Breeze: handoff and pending work

Last updated 2026-10-09. The design port and the missing features are done and pushed: `afafae5`, then `fc6b99c`.
This file lists what is still **unverified or open**, so a fresh session can continue without the previous chat.
Read `CLAUDE.md` first for build setup, layout and design tokens.

## State of the work

The Figma Make design (`docs/source/src/App.tsx`, `docs/design/*.png`) is ported to every screen.
These features exist: AC naming after pairing, per-AC saved remote state, Auto mode and Auto fan, presets, a real alarm-based timer, Privacy and Terms screens, delete confirmation, and eco/temperature hints.
The prototype's animations use the exact timings from `index.css`.

### Verified on the emulator (emulator-5554, API 36)

- Light-theme screens versus PNGs: Welcome, Brand, Find code, Name sheet, Remote in all 5 modes, power-off, Timer ("Turn off in"), Settings.
- Dark theme: Remote (Cool) and Settings. Status bar icons follow the in-app theme.
- Push transition (new screen slides in from the right over the static one) and back transition (destination slides in from the left while the popped screen is clipped away). Captured with the animator scale at 10–30×.
- "Turn off in" timer end to end: exact alarm fired, power was saved as off, the timer was removed, and the "Living Room turned off" notification was posted (IR reported "No IR blaster", which is expected on the emulator).
- Privacy page and the delete confirmation dialog.

## Pending: verify on a device or emulator

Items 1 to 11 from the previous plan, and the Welcome, Find code and missed-timer checks, were done on 2026-10-09 (emulator-5554, API 36). See "Verified on 2026-10-09" below. These are still open:

1. **New launcher icon on Samsung One UI.** Themed icons are verified on Pixel Launcher (below). The outer ring ends at r≈32.6dp, inside the 33dp safe zone, so a squircle mask shouldn't clip it, but this hasn't been seen on a Samsung launcher.
2. **Real IR on an LG unit**, still never tried on hardware with an IR blaster.
3. **Real IR on a Samsung unit.** `SamsungIrCodec` matches IRremoteESP8266's reference on/off frames in unit tests, but hasn't been tried on hardware. It always sends the 21-byte extended form, which carries power and settings together. If a unit ignores repeated extended frames while already on, send the 14-byte normal form when power hasn't changed.

### Verified on 2026-10-09

- Preset highlight survives Remote → Timer → back.
- Live power sync: "Turn off in 00:05" fired with the Remote open. The UI greyed out in place, the timer row cleared and the notification was posted.
- "Turn on at": the summary says "tomorrow" for a time that has passed. At 09:15 the AC came on with its saved mode, temperature and fan.
- Reboot: a real reboot rescheduled the pending exact alarm about 20 s after boot.
- Second AC (Office) paired through Settings → Add AC. Each AC keeps its own state across room switches, and the stored JSON matches. Kill and relaunch opens on the active AC.
- Rename shows in Settings and on the Remote header.
- Hints: Eco tip below 22° in Cool, red warning at 17°. Both clear after about 4.5 s.
- Dark mode versus PNGs: Heat (`11b`) and Settings (`05c`, `05d`) match. Brand, Timer and Find code look right (there are no dark PNGs for them).
- Reduced motion: screen transitions are instant and the fan icon is static.
- Predictive back: the clip-reveal follows a slow drag, cancel restores the screen, and commit pops. Push and back-key transitions are unchanged.
- Small screen (945×1680 px, 360×640 dp): the Remote's upper area scrolls with the Presets sheet pinned, preset chips scroll sideways, and the Timer screen fits.
- Welcome in dark (system dark after clearing data) matches the light PNG's layout, and the rings animate. Under reduced motion the rings and fan are frozen (identical frames below the status bar).
- Find code under reduced motion: the pulse is off and frames are identical over a full 2.2 s period.
- Themed icons (Pixel Launcher, API 36): with Wallpaper & style → Themed icons on, Breeze is drawn from its monochrome layer (rings and fan) and tinted like the system apps. A light rim around it in the dock is the launcher's "Predicted app" ring, not part of the icon.
- Missed timer after reboot: "Turn off in 00:05" was due at 10:20:45 and the device rebooted at 10:20:29, finishing at 10:22:20. After boot the missed timer was gone from the store with no alarm and no notification. A pending "Turn on at 07:00" on the other AC was rescheduled.

### Fixed on 2026-10-09

- **Cold-start flash.** The Remote drew its "no AC" empty state for a frame before DataStore loaded, because `devices` started as `emptyList()`. `NavGraph` now collects devices and the active id as one snapshot that starts as null, and draws the Remote only after it loads. This also prevents a wrong-room frame with several ACs.
- **Predictive back didn't track the gesture.** The manifest was missing `android:enableOnBackInvokedCallback="true"`. Also, `screen()` only treated a screen as popping after the pop was committed. It now also counts "exiting while still on top" (an uncommitted predictive back).
- **Launcher icon.** The manifest pointed at a plain layer-list, so the adaptive icon was never used. It now uses `@mipmap/ic_launcher`, a new design based on the Welcome emblem (Cool rings and the `mode_fan` disc on the dark surface with a Cool glow), plus a monochrome layer for themed icons.
- **Welcome rings under reduced motion** sat at rest (scale 1), so all four stacked into one bright 160dp ring that looked nothing like the design. They now freeze at evenly spread phases (`ReducedMotionRingFrame`), which looks like a still of the animation.

- **Launch splash ignored the in-app theme.** With the app set to Dark on a light system, the splash was light for a moment. `MainActivity` now passes the theme to `UiModeManager.setApplicationNightMode` (API 31+), and the manifest handles `uiMode` so the switch recomposes in place instead of recreating the activity. Checked on emulator-5554: app Dark on light system gives a dark splash, Light on dark gives a light one, System follows the system, and switching in Settings stays on Settings in the same process.

## Open items and known limitations (not bugs)

- **IR for brands other than LG and Samsung is a placeholder.** `BrandIr` returns null for control, so the Remote toasts "Codes coming for X" once per AC. Real codecs (Daikin, …) would go in `ir/` next to `LgIrCodec` and `SamsungIrCodec`.
- **Presets are combinations.** Sleep (+1°, low fan), Eco (Cool 26°, auto fan) and Turbo (Cool 18°, high fan) are plain mode/temperature/fan settings, because the LG classic codec has no sleep or turbo bits.
- **Timers are app-side.** The phone must be pointed at the AC when the alarm fires. Without the exact-alarm permission (denied by default on Android 14+), the Timer screen shows "Allow exact timing", and alarms otherwise run inexact.
- **System splash on Android 8–11** still follows the system theme, because `setApplicationNightMode` only exists on API 31+.
- **Slow debug cold start** (about 25 s observed, but on an overloaded host, so it isn't a reliable number). Consider a release build check and a baseline profile.
- **Unit tests cover only pure logic.** `app/src/test` has JVM tests for `LgIrCodec` and `SamsungIrCodec` (frames, checksums, clamping), `TimerScheduler.afterDuration` / `nextTimeOfDay` (rollover, DST) and `StoreJson` (round-trip, legacy device JSON without `state`). Run them with `./gradlew testDebugUnitTest`. There are no UI tests yet. Roborazzi screenshot tests could compare against `docs/design/*.png` without an emulator.

## Environment notes

- Linux/cloud containers: install the SDK with `cmdline-tools` (`platforms;android-35`, `build-tools;34.0.0`) and set `sdk.dir` in `local.properties`. JDK 21 also builds there.
- Build with JDK 17 on the Mac: `JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home`. Gradle 8.9 can't run on Android Studio's bundled JDK 25. If the Kotlin daemon dies, add `-Pkotlin.compiler.execution.strategy=in-process`.
- **emulator-5554 (`medium_phone`) is shared with other agents.** Ask before using it, always pass `-s emulator-5554`, and never run `adb kill-server`. There is a second AVD (`Medium_Phone_API_37.0`), but the host has 8 GB of RAM and swap was nearly full, so running two emulators at once is not viable.
- The emulator currently has Breeze installed, the theme set to Dark, and exact alarms allowed for Breeze via appops. Restart the emulator if it shows "System UI / Process system isn't responding".
