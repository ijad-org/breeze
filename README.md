# Breeze

**Breeze** is a calm, ad-free Android IR remote for air conditioners — **Kotlin**, **Jetpack Compose**, soft blue elevated UI.

Point your phone, pick a brand, probe power codes until the AC responds, then control temperature, mode, fan, and swing from a pocket remote.

> **Separate from [AC Remote Easy](https://github.com/ijad-org/ac-remote-easy).** Breeze is a new app (`com.ijad.breeze`) with its own Figma-inspired light UI. It is not a rename or fork rename of AC Remote Easy.

> **Disclaimer:** Breeze is an independent open-source project. It is **not affiliated with, endorsed by, or sponsored by** Samsung, LG, Daikin, Mitsubishi, Voltas, Blue Star, Carrier, Haier, Panasonic, or any other AC manufacturer. Brand names identify compatible device categories only.

## Features (v1)

The UI is a port of the Figma Make prototype. See `docs/source/src/App.tsx` and `docs/design/*.png`.

| Area | Status |
|------|--------|
| Welcome | Expanding rings + spinning fan, matching the prototype animations |
| Brand select + search | Working (10 brands) |
| Find-your-code pairing | Pulsing power probe, prev/next codes, then a "Name this AC" sheet. LG, Samsung, Carrier and Daikin send real probes |
| Remote | Mode-tinted dial and background, Cool/Heat/Dry/Fan/Auto, 4-step fan (Low/Mid/High/Auto), swing, greyed power-off state, per-AC state saved |
| Presets | Sleep (+1°, low fan), Eco (Cool 26°, auto fan), Turbo (Cool 18°, high fan) |
| Timer | "Turn off in" / "Turn on at" via AlarmManager. Sends IR when it fires (phone must face the AC), optional reminder notification, survives reboot |
| Settings | My ACs (rename, remove with confirmation, add), notification toggles, Light/Dark/System, About, Privacy Policy, Terms |
| Hints | Eco tips (Cool below 22°) and temperature alerts (≤17° or ≥29°) when enabled |
| Storage | DataStore (devices + remote state, timers, settings) |
| Ads / accounts | None |

## IR

- Android **ConsumerIrManager** (`TRANSMIT_IR`, feature optional)
- **LG**: classic 28-bit frames @ 38 kHz via `LgIrCodec` (adapted from AC Remote Easy)
- **Samsung**: 21-byte extended frames @ 38 kHz via `SamsungIrCodec` (protocol from IRremoteESP8266 `ir_Samsung`)
- **Carrier**: 64-bit full-state frames @ 38 kHz via `CarrierIrCodec` (CARRIER_AC64, after IRremoteESP8266). Dry and Auto are sent as Cool
- **Daikin**: full-state frames @ 38 kHz via `DaikinIrCodec`, in two protocols picked at pairing: 280-bit (ARC4xx remotes) and 216-bit (ARC433B69). Layouts follow IRremoteESP8266
- **Other brands**: placeholder chirps / Toast *"codes coming"* for v1
- Empty banner when `hasIrEmitter()` is false

## Requirements

- JDK 17
- minSdk **26**, targetSdk / compileSdk **35**
- Phone with IR blaster preferred for transmit testing

## Build

```bash
export JAVA_HOME=/path/to/jdk-17   # Gradle 8.9 needs JDK 17/21, not 25
export ANDROID_HOME=~/Library/Android/sdk
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
