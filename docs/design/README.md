# BREEZE — Smart AC Remote App (Figma Make) — handoff v2

Source URL: https://www.figma.com/make/o35cEKnPvQ6xDeSzhZ8D3k/Smart-AC-Remote-App
Captured 2026-10-07 from the Make preview. Every PNG is cropped to the phone frame (394×853 px). The file was not edited. Theme and mode changes were only made in the live preview's runtime state.

## What changed in this update
- There's a new **Settings gear** to the left of the power button on the Remote screen. It opens a full Settings screen.
- The desktop showcase (screen tabs, theme toggle, component-sheet nav) was removed. The app is mobile-only now.
- Dark mode is now set from Settings → Appearance (Light / Dark / System).

## Screenshots
| File | Description |
|---|---|
| 01-welcome.png | Welcome/splash: concentric rings around the fan icon, "BREEZE", "Your AC, in your pocket.", Get started button |
| 02-brand-select.png | Choose your AC brand: search box plus a brand list (Daikin selected) and Continue |
| 03-find-code.png | Find your code: pulsing power button, "Code 1 of 12", next arrow, Yes it worked / Not yet |
| 04-remote-cool.png | Remote, Cool (blue #4F9DFF): room picker, **new settings gear** next to power, 24° arc dial, +/-, mode chips, Fan/Swing cards, Presets sheet |
| 05-settings-system.png | **Settings** (Appearance = System, the default): My ACs (Living Room AC, Daikin · Code 3; Bedroom AC, LG · Code 7; edit/delete icons; Add AC), Notifications toggles (Temperature alerts on, Timer reminders on, Eco tips off), Appearance segmented control, About |
| 05b-settings-light-scrolled.png | Settings, Light selected, scrolled to the bottom (About: Version 1.0.0, Privacy Policy, Terms of Service) |
| 05c-settings-dark.png | Settings, Dark selected: dark glass cards (top of the screen) |
| 05d-settings-dark-scrolled.png | Settings, Dark, scrolled to the bottom |
| 06-remote-heat.png | Remote, Heat (orange #FF9A4D) |
| 07-remote-dry.png | Remote, Dry (purple #B28DFF) |
| 08-remote-fan.png | Remote, Fan (green #4FD1A5) |
| 09-remote-auto.png | Remote, Auto (slate #94A3B8). The chip row is scrolled to show Auto |
| 10-remote-power-off.png | Remote with power off: the whole UI greys out and is disabled |
| 11-remote-cool-dark.png | Remote, Cool, dark theme |
| 11b-remote-heat-dark.png | Remote, Heat, dark theme |
| 12-timer-turn-off-in.png | Timer, "Turn off in" tab: 02:30 hour/minute steppers, Set timer |
| 12b-timer-turn-on-at.png | Timer, "Turn on at" tab |

### Not reachable or not interactive in the preview
- Settings has one screen only. No sub-panels or sheets open. Edit, delete, Add AC, Privacy Policy, Terms and the room dropdown are visual only (no handlers).
- The Find-code arrow didn't advance past "Code 1 of 12" in my clicks. The `findcode2` (left+right arrows) variant only exists in code.
- The Components sheet (`ComponentsScreen`) is still in the source, but no UI navigates to it any more. Earlier captures are in v1 (14/15-components-*.png).
- The remote's `remote-heat` screen ID is only reachable by tapping the Heat chip, which is what 06 shows.

## Animations (not visible in still PNGs)
- Welcome: 4 concentric rings expand and fade (`ring-out` 3s, staggered 0.75s). The fan icon spins (`fan-spin` 4s linear).
- Find Code: a pulsing ring around the power button (`btn-pulse` 2.2s).
- Screen navigation: push slide-in right (forward) or left (back), 0.3s cubic-bezier(0.35,0,0.2,1). The previous screen stays underneath. Disabled under prefers-reduced-motion.
- Remote: the background radial gradient crossfades on mode change, the dial arc animates, chip and toggle colours transition, and the swing knob uses a spring easing.
- Presets sheet: `slide-up` 0.35s.
- Settings: the toggle knobs and the Appearance segment slide. The theme switch re-tints every surface.
For exact timings use source/extracted/src/index.css and src/App.tsx. A short screen recording of the Make preview is recommended for motion reference.

## Source
- source/Smart AC Remote App.zip: downloaded with Make's code view → "Download code"
- source/extracted/: unzipped Vite + React + Tailwind project. All screens are in src/App.tsx (964 lines). Keyframes are in src/index.css.
