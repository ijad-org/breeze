# Breeze: handoff and pending work

Last updated 2026-10-08. The design port and the missing features are done and pushed: `afafae5`, then `fc6b99c`.
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

Do these in order. Each needs a running device; see "Environment" below first.

1. **Preset highlight survives navigation** (`fc6b99c`, `RemoteScreen.kt`: `preset` uses `rememberSaveable`).
   Tap Eco on the Remote → open Timer → back. Expected: Eco is still highlighted.
2. **Live power sync** (`fc6b99c`, the `LaunchedEffect(activeDevice.state.poweredOn)` in `RemoteScreen.kt`).
   Set "Turn off in 00:05" and stay on the Remote. When it fires, the power button and UI should grey out without leaving the screen.
   With exact alarms off, the alarm can be late. Grant them for testing with `adb shell appops set com.ijad.breeze SCHEDULE_EXACT_ALARM allow`.
3. **"Turn on at" timer.** Pick a time a few minutes ahead. The summary should read "... turns on at HH:MM" (with "tomorrow" if that time has already passed today). When it fires, the AC state becomes on with its saved mode, temperature and fan.
4. **Reboot rescheduling** (`timer/BootReceiver.kt`). Set a timer, reboot, then check `adb shell dumpsys alarm | grep -A3 com.ijad.breeze`. Timers whose time passed during the reboot should be dropped.
5. **Second AC and room switching.** Pair a second AC through Settings → Add AC. Change its mode and temperature, switch rooms from the Remote's name dropdown, and check that each AC restores its own state. Kill and relaunch the app: it should open straight on the Remote with no Welcome flash.
6. **Rename dialog** (Settings → pencil). The new name should show in Settings and on the Remote header.
7. **Hints.** With Eco tips on, Cool below 22° shows the green tip. With Temperature alerts on, ≤17° or ≥29° shows the red warning. Both auto-dismiss after about 4.5 s.
8. **Remaining dark-mode screens versus PNGs:** Heat dark (`11b-remote-heat-dark.png`), Settings scrolled (`05d`), Timer, Brand, Find code, Welcome.
9. **Reduced motion.** Set Developer options → animator duration scale to "Animation off", then relaunch. Screen transitions, the welcome rings, the fan spin and the find-code pulse should all be static.
10. **Predictive back** (Android 14+ gesture). Swipe back slowly from Settings. The clip-reveal (`screen()` in `NavGraph.kt`) should track the gesture without glitches.
11. **Small screens.** On a short device (for example 360×640 dp), the Remote's upper area scrolls and the Presets sheet stays pinned. Check that nothing overlaps.

## Open items and known limitations (not bugs)

- **IR for non-LG brands is a placeholder.** `BrandIr` returns null for control, so the Remote toasts "Codes coming for X" once per AC. Real codecs (Samsung, Daikin, …) would go in `ir/` next to `LgIrCodec`.
- **Presets are combinations.** Sleep (+1°, low fan), Eco (Cool 26°, auto fan) and Turbo (Cool 18°, high fan) are plain mode/temperature/fan settings, because the LG classic codec has no sleep or turbo bits.
- **Timers are app-side.** The phone must be pointed at the AC when the alarm fires. Without the exact-alarm permission (denied by default on Android 14+), the Timer screen shows "Allow exact timing", and alarms otherwise run inexact.
- **Slow debug cold start** (about 25 s observed, but on an overloaded host, so it isn't a reliable number). Consider a release build check and a baseline profile.
- **No automated tests.** Candidates: JVM unit tests for `TimerScheduler.afterDuration` / `nextTimeOfDay`, `AppRepository` JSON round-trip (including old device JSON without `state`), and `LgIrCodec` checksums. Optionally add Roborazzi screenshot tests to compare against `docs/design/*.png` without an emulator.

## Environment notes

- Build with JDK 17: `JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home`. Gradle 8.9 can't run on Android Studio's bundled JDK 25. If the Kotlin daemon dies, add `-Pkotlin.compiler.execution.strategy=in-process`.
- **emulator-5554 (`medium_phone`) is shared with other agents.** Ask before using it, always pass `-s emulator-5554`, and never run `adb kill-server`. There is a second AVD (`Medium_Phone_API_37.0`), but the host has 8 GB of RAM and swap was nearly full, so running two emulators at once is not viable.
- The emulator currently has Breeze installed, the theme set to Dark, and exact alarms allowed for Breeze via appops. Restart the emulator if it shows "System UI / Process system isn't responding".
