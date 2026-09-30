# Breeze

**Breeze** is a calm, ad-free Android IR remote for air conditioners — **Kotlin**, **Jetpack Compose**, soft blue elevated UI.

Point your phone, pick a brand, probe power codes until the AC responds, then control temperature, mode, fan, and swing from a pocket remote.

> **Separate from [AC Remote Easy](https://github.com/ijad-org/ac-remote-easy).** Breeze is a new app (`com.ijad.breeze`) with its own Figma-inspired light UI. It is not a rename or fork rename of AC Remote Easy.

> **Disclaimer:** Breeze is an independent open-source project. It is **not affiliated with, endorsed by, or sponsored by** Samsung, LG, Daikin, Mitsubishi, Voltas, Blue Star, Carrier, Haier, Panasonic, or any other AC manufacturer. Brand names identify compatible device categories only.

## Features (v1)

| Area | Status |
|------|--------|
| Splash / welcome | Working |
| Brand select + search | Working (10 brands) |
| Find-your-code pairing | Working UI; LG sends real classic 28-bit probes |
| Remote | Temp dial, Cool/Heat/Dry/Fan, fan + swing, presets (stub IR) |
| Settings | Paired ACs, local notification toggles, Light/Dark/System, About |
| Storage | DataStore (brand, configIndex, name) |
| Ads / accounts | None |

## IR

- Android **ConsumerIrManager** (`TRANSMIT_IR`, feature optional)
- **LG**: classic 28-bit frames @ 38 kHz via `LgIrCodec` (adapted from AC Remote Easy)
- **Other brands**: placeholder chirps / Toast *"codes coming"* for v1
- Empty banner when `hasIrEmitter()` is false

## Requirements

- JDK 17
- minSdk **26**, targetSdk / compileSdk **35**
- Phone with IR blaster preferred for transmit testing

## Build

```bash
source /workspace/android-build-env.sh   # or set JAVA_HOME / ANDROID_HOME
./gradlew assembleDebug --no-daemon
```

Debug APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## License

MIT — see [LICENSE](LICENSE).
