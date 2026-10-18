# Breeze: handoff and pending work

Last updated 2026-10-18. The design port and the missing features are done and pushed: `afafae5`, then `fc6b99c`. Mitsubishi and Voltas codecs were added on 2026-10-18, then Panasonic and Haier codecs, Gree and Coolix codecs for Blue Star (and Coolix for Midea-built Carriers), and a protocol sweep for Other. Every brand in the list now sends real frames.
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
4. **Real IR on a Carrier unit.** `CarrierIrCodec` matches IRremoteESP8266's CARRIER_AC64 known states and a real capture in unit tests, but has not been tried on hardware. Many Carrier units (especially Midea-built ones) use a different protocol, so a unit that ignores all 5 probes is probably not AC64. Codes 6 and 7 are now Coolix (Midea) on and off for those.
5. **Real IR on a Daikin unit.** Neither protocol has been tried on hardware. The ARC433B69 encoder reproduces a real remote capture byte for byte (IRremoteESP8266 issue #689). The 280-bit frames are checked against that library's timings and reset state, not against a capture.
6. **Real IR on a Mitsubishi unit.** `MitsubishiIrCodec` reproduces a real MITSUBISHI_AC capture from IRremoteESP8266's tests byte for byte, but hasn't been tried on hardware. Mitsubishi Electric also uses 136-bit, 112-bit and Mitsubishi Heavy protocols, so a unit that ignores all 5 probes probably isn't on the 144-bit one.
7. **Real IR on a Voltas unit.** `VoltasIrCodec` matches IRremoteESP8266's reset state and a real 122LZF capture (apart from the remote's WiFi bit, which the app leaves off), but hasn't been tried on hardware.
8. **Real IR on a Panasonic unit.** `PanasonicIrCodec` reproduces a real JKE capture (IRremoteESP8266 #525) byte for byte, and its AC32 sender reproduces the library's `sendPanasonicAC32` timings exactly. A real CKP capture (#544) matches in mode, temp, fan, vanes and model bytes. A real AC32 capture (#1307) taken in "auto" carried mode 5, while the library (and the app) send 6. If an AC32 unit ignores Auto, try 5. A real DKE capture (#1024) matches except byte 13, where that remote also sets the on/off timer flags at the "unset" time; the app leaves them clear. If a DKE unit ignores frames, try setting those flags (`0x0E`). Not tried on hardware. 36.7 kHz is within what ConsumerIrManager phones accept, but that hasn't been checked on a device either.
9. **Real IR on a Haier unit.** `HaierIrCodec` reproduces real HSU07-HEA03 (IRremoteESP8266 #668) and 160-bit (#1804) captures byte for byte. A real 176-bit capture (#1480) matches apart from an undocumented vane value. YR-W02 is checked against the library layout and real-capture checksums. Not tried on hardware. Health (ionizer) is sent off, as the library's `IRac` does.
10. **Real IR on Blue Star, Gree and Coolix units.** `GreeIrCodec`'s BlueStar variant reproduces all 16 published Blue Star codes (techantidote.com: on, off and 16–30°, first 4 bytes, since that capture tool only read 32 bits). Plain Gree matches IRremoteESP8266's real #814 states byte for byte. `CoolixIrCodec` matches a real remote's sequence (#722) and the Blue Star off capture (#498). None has been tried on hardware. Blue Star "Heavy" (104-bit) units won't respond to any code.
11. **Find code and Welcome on a device.** On 2026-10-18 they were rendered on the JVM (`ScreenRenderTest`, see CLAUDE.md "Verifying UI") and compared with `03-find-code.png` and `01-welcome.png`. Layout, copy and colours match, and "Code 1 of 6" (Blue Star), "Code 1 of 7" (Carrier) and "Code 1 of 21" (Other, dark) show correctly. The pulse and rings move frame to frame, and freeze under reduced motion, after the `rememberFrameMillis` change. Not yet seen on a device or emulator.

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

### Fixed on 2026-10-18

- **Open-source licenses screen.** Settings → About now has "Open-source licenses" (`LicensesScreen`). It shows the full SIL OFL 1.1 for the bundled Plus Jakarta Sans (which the OFL requires to ship with the font), Apache 2.0 for AndroidX, Kotlin, kotlinx.coroutines, Okio and Guava ListenableFuture (everything else in `releaseRuntimeClasspath`), and Breeze's MIT license, from `res/raw`. It credits IRremoteESP8266 (LGPL-2.1) for the protocol layouts; whether the ported codecs themselves need LGPL terms is still open. The unused copy in `assets/licenses/` was removed. Checked in `ScreenRenderTest` (the OFL row opens and shows the text) and in the release APK (all three files survive resource shrinking).
- **Target API 36.** Google Play requires it for new apps and updates since 2026-08-31. `compileSdk`/`targetSdk` are 36, AGP 8.7.3 became 8.13.2 and Gradle 8.9 became 8.13. Edge-to-edge and predictive back were already in place. On 600dp-wide and larger screens Android 16 ignores the portrait lock, so content is capped at a centred 480dp (`phoneWidth()`). `ScreenRenderTest` renders the Remote and Find code on a 1280×800dp landscape tablet and Welcome on an 841×701dp unfolded foldable; phone renders are byte-identical to before. **Not yet seen on a device**: a tablet or foldable emulator should check rotation (the activity is recreated, and state comes back from DataStore and `rememberSaveable`) and fold/unfold.

- **Lint errors.** `lintDebug` reported 3 errors, now 0 (34 warnings remain). Two were `ProduceStateDoesNotAssignValue` false positives on the Welcome and Find code frame loops, now replaced by `rememberFrameMillis`. The third is the deliberate `currentBackStack` use for predictive back, now suppressed in place with the reason.

### Fixed on 2026-10-09

- **Cold-start flash.** The Remote drew its "no AC" empty state for a frame before DataStore loaded, because `devices` started as `emptyList()`. `NavGraph` now collects devices and the active id as one snapshot that starts as null, and draws the Remote only after it loads. This also prevents a wrong-room frame with several ACs.
- **Predictive back didn't track the gesture.** The manifest was missing `android:enableOnBackInvokedCallback="true"`. Also, `screen()` only treated a screen as popping after the pop was committed. It now also counts "exiting while still on top" (an uncommitted predictive back).
- **Launcher icon.** The manifest pointed at a plain layer-list, so the adaptive icon was never used. It now uses `@mipmap/ic_launcher`, a new design based on the Welcome emblem (Cool rings and the `mode_fan` disc on the dark surface with a Cool glow), plus a monochrome layer for themed icons.
- **Welcome rings under reduced motion** sat at rest (scale 1), so all four stacked into one bright 160dp ring that looked nothing like the design. They now freeze at evenly spread phases (`ReducedMotionRingFrame`), which looks like a still of the animation.

- **Launch splash ignored the in-app theme.** With the app set to Dark on a light system, the splash was light for a moment. `MainActivity` now passes the theme to `UiModeManager.setApplicationNightMode` (API 31+), and the manifest handles `uiMode` so the switch recomposes in place instead of recreating the activity. Checked on emulator-5554: app Dark on light system gives a dark splash, Light on dark gives a light one, System follows the system, and switching in Settings stays on Settings in the same process.

## Open items and known limitations (not bugs)

- **Other uses the protocol sweep.** Pairing tries every supported variant (`BrandIr.SWEEP`) and `configIndex` stores the index. Only append to `SWEEP`, or paired ACs will switch protocol. A new codec goes in `ir/` next to the existing `*IrCodec` files, plus a `Codec` in `BrandIr` (and a `SWEEP` entry at the end).
- **ACs paired before a brand got its codec** keep their old placeholder code index. For Panasonic and Haier an index past the probe list falls back to the first variant (DKE, YR-W02). For Blue Star and Other the index now picks a variant that was never tested against that AC. Since 2026-10-18 the Remote asks those ACs to find their code again (see CLAUDE.md "Older pairings"). Code lists weren't versioned before, so every older Carrier, Daikin, Panasonic, Haier, Blue Star and Other AC is asked once, including ones paired after their codec landed. "It works" dismisses it for good. The banner was checked in JVM renders (`ScreenRenderTest`, light and dark) but not yet on a device, including the re-pair flow back to the Remote.
- **Carrier has no Dry or Auto.** CARRIER_AC64 only encodes Heat, Cool and Fan. Dry and Auto are sent as Cool while the UI still shows the chosen mode.
- **Blue Star "Heavy" (104-bit) isn't covered.** Published Blue Star captures are Gree frames (techantidote.com), the Coolix off frame (IRremoteESP8266 #498), and a 104-bit frame IRremoteESP8266 calls "Bluestar Heavy" (#1593). For Heavy there is only `sendBluestarHeavy`, a raw sender with one example frame (13 bytes, remote D716LXM0535A2400313) and no documented fields. A Heavy encoder needs several captures from such a remote, each with one setting changed. The Blue Star Gree capture also only covered the first 4 bytes, so bytes 4–7 are the library defaults.
- **Coolix swing is a toggle.** Like LG, the app sends the swing command on a swing tap. If the unit's swing was changed with its own remote, the app's swing state is inverted until the next tap.
- **Panasonic CKP and AC32 toggle power.** The app sends the toggle only when its own power state changes (`AcCommands.sendState` compares with the previous state). If the AC was switched with its physical remote, the app's state is inverted until the user taps power once more. Nothing in IR can read the AC's real state.
- **Voltas has no Auto mode, and Dry is fixed.** Auto is sent as Cool. In Dry the protocol forces 24° and low fan, so the AC ignores the temperature and fan shown in the UI.
- **Daikin covers two of its protocols.** Only the 280-bit and 216-bit protocols are covered. Others such as Daikin2 (ARC477A1), 160, 176, 152, 128 and 64 aren't. A Daikin AC that was paired before this change has a placeholder code index, so it falls back to the 280-bit protocol. Re-pairing picks the right one.
- **Presets are combinations.** Sleep (+1°, low fan), Eco (Cool 26°, auto fan) and Turbo (Cool 18°, high fan) are plain mode/temperature/fan settings, because the LG classic codec has no sleep or turbo bits.
- **Timers are app-side.** The phone must be pointed at the AC when the alarm fires. Without the exact-alarm permission (denied by default on Android 14+), the Timer screen shows "Allow exact timing", and alarms otherwise run inexact.
- **System splash on Android 8–11** still follows the system theme, because `setApplicationNightMode` only exists on API 31+.
- **Cold start is fine in release; emulator numbers are host-bound.** Checked 2026-10-09 on emulator-5554 (API 36), release build signed with the debug key. With a quiet host, time to first frame is about 0.5–0.7 s with or without the library baseline profile (installed via the AGP `.dm`), with no measurable difference. When the Mac swaps, every mode jumps to 3–5 s. A Perfetto trace showed that time is CPU starvation and RenderThread waits, not app code (verification about 3 ms). "Fully drawn" (`ReportDrawnWhen`, gated on stored settings and the Remote's device snapshot) lands 0.6–0.8 s after the first frame. Notes:
  - `androidx.profileinstaller` is pinned to 1.4.1 (2026-10-18). The 1.3.1 that Compose pulls in only knows APIs up to 34, so it rejected API 36 ("unsupported ART version") and sideloaded APKs on Android 15 and 16 got no profile. 1.4.1 writes the S format for every API from 31 up. Play installs weren't affected. Not yet re-checked on the emulator with `adb shell dumpsys package dexopt`.
  - Release has R8 and resource shrinking on (2026-10-18). Dex went from about 42 MB (mostly `material-icons-extended`) to 2.4 MB, and the unsigned APK from 11.9 MB to 1.7 MB. R8 reported no missing classes, and no custom keep rules were needed: `MainActivity` and both receivers keep their names, DataStore's protobuf rules are applied, and the dex still builds `AcMode`, `FanSpeed`, `ThemeMode` and `TimerAction` with their original names, so saved state and timers stay readable. **Still needs a UI pass on a device or emulator** with a release build (signed with the debug key): every screen, pairing, a timer firing, and an upgrade over a non-minified install with saved ACs. The cold-start numbers above were measured before R8.
  - Measuring with `am force-stop` re-delivers `BOOT_COMPLETED` on API 35+ (about 220 ms on the main thread in traces). That's an artifact of the method, not a user-facing cost.
  - Trustworthy numbers need a physical phone or a Macrobenchmark module.
- **Unit tests cover only pure logic.** `app/src/test` has JVM tests for `LgIrCodec`, `SamsungIrCodec`, `CarrierIrCodec`, `DaikinIrCodec`, `MitsubishiIrCodec`, `VoltasIrCodec`, `PanasonicIrCodec`, `HaierIrCodec`, `GreeIrCodec` and `CoolixIrCodec` (frames, checksums, clamping, real captures), `BrandIr` routing (including the sweep), `TimerScheduler.afterDuration` / `nextTimeOfDay` (rollover, DST) and `StoreJson` (round-trip, legacy device JSON without `state`). Run them with `./gradlew testDebugUnitTest`. `ScreenRenderTest` (Robolectric, opt-in with `-Pbreeze.screenshots=true`) renders Welcome and Find code to PNGs and checks that their animations move. It doesn't compare pixels against `docs/design`, because the design PNGs include a phone frame and status bar. Compare by eye.

## Environment notes

- Linux/cloud containers: install the SDK with `cmdline-tools` (`platforms;android-36`, `build-tools;35.0.0`) and set `sdk.dir` in `local.properties`. JDK 21 also builds there.
- Build with JDK 17 on the Mac: `JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home`. Gradle 8.13 can't run on Android Studio's bundled JDK 25. If the Kotlin daemon dies, add `-Pkotlin.compiler.execution.strategy=in-process`.
- **emulator-5554 (`medium_phone`) is shared with other agents.** Ask before using it, always pass `-s emulator-5554`, and never run `adb kill-server`. There is a second AVD (`Medium_Phone_API_37.0`), but the host has 8 GB of RAM and swap was nearly full, so running two emulators at once is not viable.
- The emulator currently has Breeze installed, the theme set to Dark, and exact alarms allowed for Breeze via appops. Restart the emulator if it shows "System UI / Process system isn't responding".
