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
- JVM unit tests: `./gradlew testDebugUnitTest`. They cover `LgIrCodec` frames, `TimerScheduler` time math and `StoreJson` (device/timer JSON, including legacy JSON). UI still has to be verified on an emulator or device (see "Verifying UI" below).

## Layout

```
app/src/main/java/com/ijad/breeze/
  MainActivity.kt          theme resolution + edge-to-edge system bar style
  data/                    Models (AcBrand, AcDevice, RemoteState, enums), AppRepository (DataStore), StoreJson (device/timer JSON)
  ir/                      IrTransmitter (ConsumerIrManager), BrandIr (pattern lookup), LgIrCodec (real LG 28-bit)
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
- Other brands send a placeholder chirp during pairing, and control shows "Codes coming for X". Don't present these as working.
- Presets (Sleep/Eco/Turbo) are combinations of mode, temp and fan, because the LG codec has no sleep or turbo bits.
- Timers are app-side alarms. The phone must still be pointed at the AC when the alarm fires.

## Conventions

- Compose only, no XML layouts. Use the primitives in `ui/components` rather than raw Material components, so the prototype look stays consistent.
- Read colours from the theme helpers (`ink`, `LocalBreezeDark`, `ModeStyle`), not hard-coded hex values in screens.
- Persist anything user-visible (devices, per-device remote state, settings, timer) through `AppRepository`. Device JSON must stay backward compatible, so use `opt*` with defaults.
- Brand names are only for compatibility. Keep the "not affiliated" disclaimer (README, Terms screen).

## Verifying UI

Use the `android-cli` skill to boot an emulator, install the APK and take screenshots. Compare them side by side with `docs/design/*.png` in light and dark. The emulator has no IR blaster, so the "No IR blaster" banner and toasts are expected there.
