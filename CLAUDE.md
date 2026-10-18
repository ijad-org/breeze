# Breeze — CLAUDE.md

Breeze (`com.ijad.breeze`) is an ad-free, account-free Android IR remote for air conditioners.
It is written in Kotlin and Jetpack Compose, and the UI is a pixel port of a Figma Make prototype.

> **Continuing work?** Read `docs/HANDOFF.md` first. It lists what is verified, what still needs on-device checks, and the open items.

## Build & run

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home   # JDK 17. Gradle 8.9 can't run on Android Studio's bundled JDK 25.
export ANDROID_HOME=~/Library/Android/sdk
./gradlew assembleDebug --no-daemon
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

- minSdk 26, target/compile 35, AGP 8.7, Kotlin 2.0, Compose BOM 2024.12.01. Versions are in `gradle/libs.versions.toml`.
- Repositories go through aliyun mirrors (`settings.gradle.kts`). Avoid adding new dependencies unless needed.
- JVM unit tests: `./gradlew testDebugUnitTest`. They cover `LgIrCodec`, `SamsungIrCodec`, `CarrierIrCodec`, `DaikinIrCodec`, `MitsubishiIrCodec`, `VoltasIrCodec`, `PanasonicIrCodec`, `HaierIrCodec`, `GreeIrCodec` and `CoolixIrCodec` frames, `BrandIr` routing, `TimerScheduler` time math and `StoreJson` (device/timer JSON, including legacy JSON). `ScreenRenderTest` renders screens to PNGs and only runs when asked (see "Verifying UI" below).

## Layout

```
app/src/main/java/com/ijad/breeze/
  MainActivity.kt          theme resolution + edge-to-edge system bar style
  data/                    Models (AcBrand, AcDevice, RemoteState, enums), AppRepository (DataStore), StoreJson (device/timer JSON)
  ir/                      IrTransmitter (ConsumerIrManager), BrandIr (pattern lookup), LgIrCodec (real LG 28-bit), SamsungIrCodec (real Samsung 21-byte), CarrierIrCodec (Carrier 64-bit), DaikinIrCodec (Daikin 280/216-bit), MitsubishiIrCodec (Mitsubishi 144-bit), VoltasIrCodec (Voltas 80-bit), PanasonicIrCodec (Panasonic 216-bit and AC32), HaierIrCodec (Haier 112/176/72/160-bit), GreeIrCodec (Gree 64-bit), CoolixIrCodec (Coolix 24-bit)
  timer/                   TimerScheduler (AlarmManager), TimerReceiver (fires IR + notification), BootReceiver
  ui/theme/                Breeze tokens: mode tints, surfaces, ink(alpha), Plus Jakarta Sans type
  ui/components/           Prototype primitives: GlassCard, RoundButton, BreezeToggle, FanSteps, AppBar, ModeChip…
  ui/<screen>/             splash (Welcome), brand, pairing (Find code), remote, timer, settings, legal
  ui/navigation/NavGraph   Navigation Compose routes + push slide transitions
```

State lives in `AppRepository`, which is DataStore Preferences. There is no DI, ViewModel layer or network.

## Design source of truth

- `docs/source/src/App.tsx` holds every screen of the Figma Make React prototype, and `docs/source/src/index.css` holds its keyframes. Take tokens, spacing, copy and timings from here.
- `docs/design/*.png` are screenshots of each screen in light and dark. `docs/design/README.md` and `NOTES.md` are the handoff notes.
  - Those notes mention `source/extracted/...`, which is stale. The real path is `docs/source/src/`.
- `docs/source/` is **reference only**. Don't edit it or run pnpm/vite there. Its own `CLAUDE.md` and `AGENTS.md` describe the Figma Make sandbox, not this app.
- When changing UI, compare against the matching PNG in both themes.

### Tokens (from `MODES`, `getBg`, `gss`, `tc` in App.tsx)

| Mode | Tint      | Icon (Material Symbols) |
|------|-----------|-------------------------|
| Cool | `#4F9DFF` | ac_unit                 |
| Heat | `#FF9A4D` | local_fire_department   |
| Dry  | `#B28DFF` | water_drop              |
| Fan  | `#4FD1A5` | mode_fan (custom vector `ic_mode_fan`) |
| Auto | `#94A3B8` | auto_mode               |

- Surfaces: light `#F4F7FB`, dark `#0E1116`. Text: ink `rgba(14,17,22,a)` in light and `rgba(244,247,251,a)` in dark. Use `ink(alpha)`.
- Background: a radial gradient of the screen's mode tint at the top. Light uses α .17. Dark uses α .52, plus a bottom-right α .14 glow.
- Glass card: light fill `white@.72` with a `white@.85` border; dark fill `white@.07` with a `white@.10` border; radius 24. An optional accent fill is used instead (α .08 light, .10 dark). No real blur, because only gradients sit behind the cards.
- The typeface is Plus Jakarta Sans (bundled in `res/font`, OFL). Type scale: 64/800 temperature, 28/800 hero title, 20/700 app bar, 15/500 body, 11–12/700 uppercase labels.
- Motion: screen push slide 300ms `cubic-bezier(.35,0,.2,1)`; welcome rings 3s staggered .75s; find-code pulse 2.2s; dial arc 350ms.
- Screen tint: Welcome, Find code and Timer use Cool. Brand and Settings use Auto. Remote uses the current mode, and Auto/slate when powered off.

## IR status

- **LG** sends real classic 28-bit frames at 38 kHz (`LgIrCodec`). Power, mode, temp, fan and swing work.
- **Samsung** sends real 21-byte "extended" frames at 38 kHz (`SamsungIrCodec`, from IRremoteESP8266 `ir_Samsung`). Every frame is extended, so it sets power together with mode, temp, fan and swing. Swing is part of the state, so toggling it resends the whole state.
- **Carrier** sends full-state CARRIER_AC64 frames at 38 kHz, LSB first (`CarrierIrCodec`, from IRremoteESP8266 `ir_Carrier`). Power, temp, fan and swing work. The protocol only has Heat, Cool and Fan, so Dry and Auto are sent as Cool. Swing is a state bit, so a swing toggle resends the whole state.
- **Daikin** sends full-state frames at 38 kHz (`DaikinIrCodec`): power, mode, temp, fan and vertical swing in every press. Two protocols: `DAIKIN` 280-bit (ARC4xx remotes) and `DAIKIN216` (ARC433B69). Pairing has 4 codes (on, off per protocol), and `configIndex / 2` picks the protocol. Out-of-range indices fall back to 280-bit.
- **Mitsubishi** sends full-state MITSUBISHI_AC 144-bit frames at 38 kHz, LSB first, twice per press like the real remote (`MitsubishiIrCodec`, from IRremoteESP8266 `ir_Mitsubishi`). Power, mode, temp, fan and swing work. Swing on is the vane's swing setting, and swing off is its auto position. A swing toggle resends the whole state.
- **Voltas** sends full-state VOLTAS 80-bit frames at 38 kHz, MSB first, with no header (`VoltasIrCodec`, from IRremoteESP8266 `ir_Voltas`, 122LZF remote). Power, temp, fan and swing work. The protocol has no Auto mode, so Auto is sent as Cool. Like the real remote, Dry always sends 24° and low fan, and Fan mode sends high fan instead of Auto. A swing toggle resends the whole state.
- **Panasonic** sends full-state PANASONIC_AC 216-bit frames at 36.7 kHz, LSB first, in an 8-byte and a 19-byte section (`PanasonicIrCodec`, from IRremoteESP8266 `ir_Panasonic`). Power, mode, temp, fan and swing work. Pairing has 14 codes (two per model: DKE, NKE, JKE, LKE, RKR, CKP, then the separate 32-bit `PANASONIC_AC32` protocol), and `configIndex / 2` picks the model. Fan mode always sends 27°, like the library. CKP and AC32 toggle power, so `BrandIr.controlPattern` takes `powerChanged` (from `AcCommands.sendState`'s previous state) and they send the toggle only then. Their two probes are a toggle, then the same frame without one. If the AC's real power drifts from the app's (someone used the physical remote), one power tap resyncs it.
- **Haier** sends full-state frames at 38 kHz, MSB first, after a double header (`HaierIrCodec`, from IRremoteESP8266 `ir_Haier`). Four protocols: `HAIER_AC_YRW02` (YR-W02), `HAIER_AC176` (V9014557), `HAIER_AC` (HSU07-HEA03) and `HAIER_AC160` (KFR-26GW). Pairing has 8 codes (on, off per protocol), and `configIndex / 2` picks the protocol. Each frame names a button: Power for control and Swing for a swing toggle.
- **Gree** sends full-state GREE 64-bit frames at 38 kHz, LSB first, as two 4-byte blocks (`GreeIrCodec`, from IRremoteESP8266 `ir_Gree`). Variants are YAW1F, YBOFB and BlueStar. BlueStar is YAW1F with byte 2 bit 4 set, as in every published Blue Star capture. Auto mode always sends 25°, and Dry always sends low fan.
- **Coolix** sends 24-bit COOLIX frames at 38 kHz, MSB first, each byte followed by its inverse, twice per press (`CoolixIrCodec`, from IRremoteESP8266 `ir_Coolix`). Off is a fixed frame, and swing is a toggle command like LG's.
- **Blue Star** pairs against Gree BlueStar, Gree YAW1F and Coolix (6 codes, `configIndex / 2`). The Blue Star 104-bit "Heavy" protocol has only one raw example, so it isn't covered.
- **Carrier** pairing has 7 codes: 5 CARRIER_AC64 probes, then Coolix on and off for Midea-built units (`configIndex >= 5`).
- **Other** steps through every supported variant (`BrandIr.SWEEP`, 21 codes, one Cool 24° ON probe each (a power toggle for CKP and AC32)), and `configIndex` is the index into that list, so only ever append to it.
- **Older pairings.** Each device stores `codeListVersion` (`Brands.CODE_LIST_VERSION` at pairing, 0 for JSON saved before the field existed). `BrandIr.needsNewCode` is true for an older pairing on a brand whose code picks the protocol (Carrier, Daikin, Panasonic, Haier, Blue Star, Other). The Remote then shows a "Find this AC's code again" banner. "Find code" re-pairs that same AC (`Routes.pairing(brandId, repairId)`), keeping its name, state and timer, and "It works" stores the current version. If a change ever makes stored codes pick a different protocol, bump `CODE_LIST_VERSION`.
- An unknown brand id has no probes, and control shows "Codes coming for X".
- Presets (Sleep/Eco/Turbo) are combinations of mode, temp and fan, because the codecs don't send sleep or turbo bits.
- Timers are app-side alarms. The phone must still be pointed at the AC when the alarm fires.

## Conventions

- Compose only, no XML layouts. Use the primitives in `ui/components` rather than raw Material components, so the prototype look stays consistent.
- Read colours from the theme helpers (`ink`, `LocalBreezeDark`, `ModeStyle`), not hard-coded hex values in screens.
- Persist anything user-visible (devices, per-device remote state, settings, timer) through `AppRepository`. Device JSON must stay backward compatible, so use `opt*` with defaults.
- Brand names are only for compatibility. Keep the "not affiliated" disclaimer (README, Terms screen).

## Verifying UI

Use the `android-cli` skill to boot an emulator, install the APK and take screenshots. Compare them side by side with `docs/design/*.png` in light and dark. The emulator has no IR blaster, so the "No IR blaster" banner and toasts are expected there.

Without an emulator (no KVM, e.g. in a cloud container), `ScreenRenderTest` renders screens on the JVM with Robolectric native graphics, at the PNGs' 394×853 size: `./gradlew testDebugUnitTest -Pbreeze.screenshots=true --tests '*ScreenRenderTest*'`. The output goes to `app/build/screenshots`. It also checks that the Welcome and Find code animations move, and freeze under reduced motion. There is no status bar in these renders, so content sits higher than in the design PNGs.
