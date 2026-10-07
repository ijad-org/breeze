import { useState, useCallback } from 'react';

// ─── Types ────────────────────────────────────────────────────────────────────
type AcMode = 'cool' | 'heat' | 'dry' | 'fan' | 'auto';
type ScreenId =
  | 'welcome' | 'brand' | 'findcode1' | 'findcode2'
  | 'remote-cool' | 'remote-heat'
  | 'timer' | 'settings' | 'components';

// ─── Constants ────────────────────────────────────────────────────────────────
const MODES: Record<AcMode, { label: string; tint: string; rgb: string; icon: string }> = {
  cool:  { label: 'Cool',  tint: '#4F9DFF', rgb: '79,157,255',  icon: 'ac_unit' },
  heat:  { label: 'Heat',  tint: '#FF9A4D', rgb: '255,154,77',  icon: 'local_fire_department' },
  dry:   { label: 'Dry',   tint: '#B28DFF', rgb: '178,141,255', icon: 'water_drop' },
  fan:   { label: 'Fan',   tint: '#4FD1A5', rgb: '79,209,165',  icon: 'mode_fan' },
  auto:  { label: 'Auto',  tint: '#94A3B8', rgb: '148,163,184', icon: 'auto_mode' },
};

const BRANDS = [
  { name: 'Samsung',    initial: 'S' },
  { name: 'LG',         initial: 'L' },
  { name: 'Daikin',     initial: 'D' },
  { name: 'Mitsubishi', initial: 'M' },
  { name: 'Voltas',     initial: 'V' },
  { name: 'Blue Star',  initial: 'B' },
  { name: 'Other',      initial: '?' },
];

// ─── Utilities ────────────────────────────────────────────────────────────────
function getBg(rgb: string, dark: boolean): React.CSSProperties {
  return dark ? {
    background: [
      `radial-gradient(ellipse 160% 52% at 50% 0%, rgba(${rgb},0.52) 0%, transparent 62%)`,
      `radial-gradient(ellipse 90% 40% at 88% 98%, rgba(${rgb},0.14) 0%, transparent 55%)`,
      '#0E1116',
    ].join(', '),
  } : {
    background: [
      `radial-gradient(ellipse 130% 46% at 50% 0%, rgba(${rgb},0.17) 0%, transparent 62%)`,
      '#F4F7FB',
    ].join(', '),
  };
}

function gss(dark: boolean, accent?: string): React.CSSProperties {
  return {
    background: dark
      ? accent ? `rgba(${accent},0.10)` : 'rgba(255,255,255,0.07)'
      : accent ? `rgba(${accent},0.08)` : 'rgba(255,255,255,0.72)',
    backdropFilter: 'blur(20px)',
    WebkitBackdropFilter: 'blur(20px)',
    border: dark
      ? '1px solid rgba(255,255,255,0.10)'
      : '1px solid rgba(255,255,255,0.85)',
    borderRadius: 24,
  };
}

function tc(dark: boolean, a = 1): string {
  return dark ? `rgba(244,247,251,${a})` : `rgba(14,17,22,${a})`;
}

// ─── Primitive Components ─────────────────────────────────────────────────────
function Ic({ n, sz = 24, style, className = '' }: { n: string; sz?: number; style?: React.CSSProperties; className?: string }) {
  return (
    <span className={`material-symbols-rounded ${className}`} style={{ fontSize: sz, lineHeight: 1, ...style }}>
      {n}
    </span>
  );
}

function RoundBtn({
  onClick, children, variant = 'primary', tint, rgb, dark,
  disabled, w, h = 52, style,
}: {
  onClick?: () => void; children: React.ReactNode;
  variant?: 'primary' | 'secondary' | 'ghost'; tint?: string; rgb?: string;
  dark?: boolean; disabled?: boolean; w?: number | string; h?: number; style?: React.CSSProperties;
}) {
  const varStyles: Record<string, React.CSSProperties> = {
    primary: { background: tint ?? '#4F9DFF', color: '#fff', boxShadow: rgb ? `0 6px 24px rgba(${rgb},0.38)` : 'none' },
    secondary: { background: dark ? 'rgba(255,255,255,0.10)' : 'rgba(0,0,0,0.07)', color: tint ?? tc(dark ?? false), border: `1px solid ${dark ? 'rgba(255,255,255,0.12)' : 'rgba(0,0,0,0.10)'}` },
    ghost: { background: 'transparent', color: tint ?? tc(dark ?? false, 0.65) },
  };
  return (
    <button
      onClick={onClick} disabled={disabled}
      style={{
        height: h, width: w ?? 'auto', padding: w ? 0 : '0 24px',
        borderRadius: 9999, border: 'none', outline: 'none', cursor: disabled ? 'not-allowed' : 'pointer',
        display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8,
        fontSize: 15, fontWeight: 600, fontFamily: "'Plus Jakarta Sans', sans-serif",
        opacity: disabled ? 0.38 : 1, transition: 'transform 0.12s, opacity 0.15s',
        ...varStyles[variant], ...style,
      }}
      onMouseDown={e => { if (!disabled) e.currentTarget.style.transform = 'scale(0.94)'; }}
      onMouseUp={e => { e.currentTarget.style.transform = 'scale(1)'; }}
      onMouseLeave={e => { e.currentTarget.style.transform = 'scale(1)'; }}
    >
      {children}
    </button>
  );
}

function Toggle({ on, onChange, tint, rgb }: { on: boolean; onChange: (v: boolean) => void; tint: string; rgb: string }) {
  return (
    <button
      className="toggle-track"
      onClick={() => onChange(!on)}
      style={{ background: on ? tint : 'rgba(148,163,184,0.4)', boxShadow: on ? `0 0 12px rgba(${rgb},0.4)` : 'none' }}
    >
      <div className="toggle-thumb" style={{ left: on ? 23 : 3 }} />
    </button>
  );
}

function FanSteps({ value, onChange, tint, dark }: { value: number; onChange: (v: number) => void; tint: string; dark: boolean }) {
  return (
    <div style={{ display: 'flex', gap: 6, width: '100%' }}>
      {[1, 2, 3, 4].map(s => (
        <button key={s} onClick={() => onChange(s)} style={{
          flex: 1, height: 8, borderRadius: 4, border: 'none', outline: 'none', cursor: 'pointer',
          background: s <= value ? tint : (dark ? 'rgba(255,255,255,0.14)' : 'rgba(0,0,0,0.10)'),
          transition: 'background 0.2s, opacity 0.2s',
          opacity: s <= value ? 1 : 0.35 + (value - s + 1) * 0.1,
        }} />
      ))}
    </div>
  );
}

// Temperature dial
const R = 96, CIRC = 2 * Math.PI * R, ARC = (240 / 360) * CIRC;

function TempDial({ temp, onInc, onDec, dark, tint, rgb, powered }: {
  temp: number; onInc: () => void; onDec: () => void;
  dark: boolean; tint: string; rgb: string; powered: boolean;
}) {
  const p = (temp - 16) / 14;
  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 20 }}>
      <div style={{ position: 'relative', width: 224, height: 224 }}>
        <svg width="224" height="224" viewBox="0 0 224 224" style={{ position: 'absolute', inset: 0 }}>
          <circle cx="112" cy="112" r={R} fill="none" strokeWidth={7} strokeLinecap="round"
            stroke={dark ? 'rgba(255,255,255,0.10)' : 'rgba(0,0,0,0.08)'}
            strokeDasharray={`${ARC} ${CIRC - ARC}`} transform="rotate(150,112,112)" />
          {powered && (
            <circle cx="112" cy="112" r={R} fill="none" strokeWidth={7} strokeLinecap="round"
              stroke={tint}
              strokeDasharray={`${Math.max(2, ARC * p)} ${CIRC}`}
              transform="rotate(150,112,112)"
              style={{ filter: `drop-shadow(0 0 8px rgba(${rgb},0.7))`, transition: 'stroke-dasharray 0.35s ease' }} />
          )}
        </svg>
        <div style={{ position: 'absolute', inset: 0, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center' }}>
          <span style={{ fontSize: 64, fontWeight: 800, lineHeight: 1, color: powered ? tc(dark) : tc(dark, 0.25), transition: 'color 0.3s' }}>
            {temp}°
          </span>
          <span style={{ fontSize: 13, fontWeight: 500, marginTop: 6, letterSpacing: '0.06em', color: powered ? tc(dark, 0.42) : tc(dark, 0.18) }}>
            Celsius
          </span>
        </div>
      </div>
      <div style={{ display: 'flex', gap: 20 }}>
        {(['remove', 'add'] as const).map((icon, i) => (
          <button key={icon} onClick={i === 0 ? onDec : onInc} disabled={!powered} style={{
            width: 48, height: 48, borderRadius: 9999, border: dark ? '1px solid rgba(255,255,255,0.12)' : '1px solid rgba(0,0,0,0.09)',
            background: dark ? 'rgba(255,255,255,0.09)' : 'rgba(0,0,0,0.06)',
            cursor: powered ? 'pointer' : 'not-allowed', outline: 'none',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            color: tc(dark, powered ? 0.85 : 0.28), transition: 'transform 0.1s', opacity: powered ? 1 : 0.38,
          }}
            onMouseDown={e => { if (powered) e.currentTarget.style.transform = 'scale(0.88)'; }}
            onMouseUp={e => { e.currentTarget.style.transform = 'scale(1)'; }}
          >
            <Ic n={icon} sz={22} />
          </button>
        ))}
      </div>
    </div>
  );
}

// ─── Screen: Welcome ──────────────────────────────────────────────────────────
function WelcomeScreen({ dark, onNext }: { dark: boolean; onNext: () => void }) {
  const { tint, rgb } = MODES.cool;
  return (
    <div style={{ ...getBg(rgb, dark), height: '100%', display: 'flex', flexDirection: 'column' }}>
      <div style={{ height: 52 }} />
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', padding: '0 20px' }}>
        {/* Concentric rings */}
        <div style={{ position: 'relative', width: 200, height: 200, display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: 48 }}>
          {[1, 2, 3, 4].map(i => (
            <div key={i} className={`ring-${i}`} style={{
              position: 'absolute', width: 160, height: 160, borderRadius: '50%',
              border: `1.5px solid rgba(${rgb},${0.6 - i * 0.1})`,
            }} />
          ))}
          <div style={{ position: 'absolute', width: 110, height: 110, borderRadius: '50%', border: `1.5px solid rgba(${rgb},0.22)` }} />
          <div style={{ position: 'absolute', width: 76, height: 76, borderRadius: '50%', border: `1.5px solid rgba(${rgb},0.32)` }} />
          <div style={{
            position: 'relative', zIndex: 10, width: 60, height: 60, borderRadius: '50%',
            background: `rgba(${rgb},0.16)`, border: `1.5px solid rgba(${rgb},0.45)`,
            backdropFilter: 'blur(10px)', display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}>
            <Ic n="mode_fan" sz={30} className="fan-spin" style={{ color: tint }} />
          </div>
        </div>
        {/* Copy */}
        <div style={{ textAlign: 'center', maxWidth: 280 }}>
          <p style={{ fontSize: 11, fontWeight: 700, letterSpacing: '0.14em', textTransform: 'uppercase', color: tint, opacity: 0.8, marginBottom: 14 }}>
            Breeze
          </p>
          <h1 style={{ fontSize: 28, fontWeight: 800, lineHeight: 1.25, color: tc(dark), marginBottom: 14 }}>
            Your AC, in your pocket.
          </h1>
          <p style={{ fontSize: 16, lineHeight: 1.6, color: tc(dark, 0.55) }}>
            Works with most brands, no setup headaches.
          </p>
        </div>
      </div>
      <div style={{ padding: '0 20px 44px' }}>
        <RoundBtn onClick={onNext} tint={tint} rgb={rgb} style={{ width: '100%' }}>
          Get started
        </RoundBtn>
      </div>
    </div>
  );
}

// ─── Screen: Brand Select ─────────────────────────────────────────────────────
function BrandScreen({ dark, onNext, onBack }: { dark: boolean; onNext: () => void; onBack: () => void }) {
  const [selected, setSelected] = useState('Daikin');
  const [query, setQuery] = useState('');
  const { tint, rgb } = MODES.auto;
  const filtered = BRANDS.filter(b => b.name.toLowerCase().includes(query.toLowerCase()));

  return (
    <div style={{ ...getBg(rgb, dark), height: '100%', display: 'flex', flexDirection: 'column' }}>
      {/* App bar */}
      <div style={{ height: 52 }} />
      <div style={{ display: 'flex', alignItems: 'center', padding: '12px 8px 8px', gap: 4 }}>
        <button onClick={onBack} style={{ width: 44, height: 44, borderRadius: 9999, border: 'none', background: 'transparent', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', color: tc(dark, 0.75) }}>
          <Ic n="arrow_back" sz={22} />
        </button>
        <span style={{ fontSize: 20, fontWeight: 700, color: tc(dark), flex: 1 }}>Choose your AC brand</span>
      </div>

      {/* Search */}
      <div style={{ padding: '8px 20px 16px' }}>
        <div style={{ ...gss(dark), borderRadius: 16, display: 'flex', alignItems: 'center', gap: 10, padding: '0 14px', height: 48 }}>
          <Ic n="search" sz={20} style={{ color: tc(dark, 0.45) }} />
          <input
            value={query} onChange={e => setQuery(e.target.value)}
            placeholder="Search brands…"
            style={{ flex: 1, background: 'transparent', border: 'none', outline: 'none', fontSize: 15, fontFamily: "'Plus Jakarta Sans', sans-serif", color: tc(dark), caretColor: tint }}
          />
        </div>
      </div>

      {/* Brand list */}
      <div style={{ flex: 1, overflowY: 'auto', padding: '0 20px' }} className="no-scrollbar">
        <div style={{ ...gss(dark), padding: '4px 0', overflow: 'hidden' }}>
          {filtered.map((b, i) => {
            const sel = selected === b.name;
            return (
              <button key={b.name} onClick={() => setSelected(b.name)} style={{
                display: 'flex', alignItems: 'center', gap: 14, padding: '12px 16px', width: '100%',
                border: 'none', outline: 'none', cursor: 'pointer', textAlign: 'left',
                background: sel ? `rgba(${rgb},0.12)` : 'transparent',
                borderBottom: i < filtered.length - 1 ? (dark ? '1px solid rgba(255,255,255,0.06)' : '1px solid rgba(0,0,0,0.05)') : 'none',
                transition: 'background 0.18s',
              }}>
                <div style={{
                  width: 40, height: 40, borderRadius: '50%', flexShrink: 0,
                  background: sel ? `rgba(${rgb},0.22)` : (dark ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.08)'),
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  fontSize: 14, fontWeight: 700, color: sel ? tint : tc(dark, 0.6),
                  border: sel ? `1.5px solid rgba(${rgb},0.4)` : '1.5px solid transparent',
                  transition: 'all 0.2s',
                }}>
                  {b.initial}
                </div>
                <span style={{ flex: 1, fontSize: 15, fontWeight: sel ? 600 : 500, color: sel ? tint : tc(dark, 0.85) }}>
                  {b.name}
                </span>
                {sel
                  ? <Ic n="check_circle" sz={22} style={{ color: tint }} />
                  : <Ic n="chevron_right" sz={20} style={{ color: tc(dark, 0.3) }} />
                }
              </button>
            );
          })}
        </div>
      </div>

      <div style={{ padding: '16px 20px 44px' }}>
        <RoundBtn onClick={onNext} tint={MODES.cool.tint} rgb={MODES.cool.rgb} style={{ width: '100%' }}>
          Continue
        </RoundBtn>
      </div>
    </div>
  );
}

// ─── Screen: Find Code ────────────────────────────────────────────────────────
function FindCodeScreen({ dark, showLeft, onNext, onBack }: { dark: boolean; showLeft: boolean; onNext: () => void; onBack: () => void }) {
  const { tint, rgb } = MODES.cool;
  const codeNum = showLeft ? 3 : 1;
  return (
    <div style={{ ...getBg(rgb, dark), height: '100%', display: 'flex', flexDirection: 'column' }}>
      <div style={{ height: 52 }} />
      {/* App bar */}
      <div style={{ display: 'flex', alignItems: 'center', padding: '8px 8px 4px', justifyContent: 'space-between' }}>
        <button onClick={onBack} style={{ width: 44, height: 44, borderRadius: 9999, border: 'none', background: 'transparent', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', color: tc(dark, 0.75) }}>
          <Ic n="arrow_back" sz={22} />
        </button>
        <span style={{ fontSize: 18, fontWeight: 700, color: tc(dark) }}>Find your code</span>
        <button onClick={onNext} style={{ width: 44, height: 44, borderRadius: 9999, border: 'none', background: `rgba(${rgb},0.14)`, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', color: tint }}>
          <Ic n="check" sz={22} />
        </button>
      </div>

      {/* Instruction */}
      <p style={{ textAlign: 'center', fontSize: 14, color: tc(dark, 0.5), padding: '16px 32px 0', lineHeight: 1.55 }}>
        Point your phone at the AC and tap the power button. Advance until it responds.
      </p>

      {/* Power button + arrows */}
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 24 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 28 }}>
          {/* Left arrow */}
          <button style={{
            width: 44, height: 44, borderRadius: 9999,
            border: dark ? '1px solid rgba(255,255,255,0.12)' : '1px solid rgba(0,0,0,0.10)',
            background: dark ? 'rgba(255,255,255,0.08)' : 'rgba(255,255,255,0.6)',
            cursor: 'pointer', outline: 'none', display: 'flex', alignItems: 'center', justifyContent: 'center',
            color: tc(dark, showLeft ? 0.8 : 0.2), transition: 'opacity 0.25s',
            opacity: showLeft ? 1 : 0, pointerEvents: showLeft ? 'auto' : 'none',
          }}>
            <Ic n="arrow_back_ios" sz={18} style={{ marginLeft: 4 }} />
          </button>

          {/* Power button */}
          <div style={{ position: 'relative', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            {/* Pulsing ring */}
            <div className="pulse-ring" style={{
              '--pulse-c': `rgba(${rgb},0.4)`,
              position: 'absolute', width: 108, height: 108, borderRadius: '50%',
              border: `2px solid rgba(${rgb},0.35)`, pointerEvents: 'none',
            } as React.CSSProperties} />
            <button style={{
              width: 88, height: 88, borderRadius: '50%', border: 'none', outline: 'none', cursor: 'pointer',
              background: tint, color: '#fff',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              boxShadow: `0 0 32px rgba(${rgb},0.55), 0 8px 20px rgba(${rgb},0.4)`,
              transition: 'transform 0.12s',
            }}
              onMouseDown={e => { e.currentTarget.style.transform = 'scale(0.92)'; }}
              onMouseUp={e => { e.currentTarget.style.transform = 'scale(1)'; }}
            >
              <Ic n="power_settings_new" sz={36} />
            </button>
          </div>

          {/* Right arrow */}
          <button style={{
            width: 44, height: 44, borderRadius: 9999,
            border: dark ? '1px solid rgba(255,255,255,0.12)' : '1px solid rgba(0,0,0,0.10)',
            background: dark ? 'rgba(255,255,255,0.08)' : 'rgba(255,255,255,0.6)',
            cursor: 'pointer', outline: 'none', display: 'flex', alignItems: 'center', justifyContent: 'center',
            color: tc(dark, 0.8),
          }}>
            <Ic n="arrow_forward_ios" sz={18} />
          </button>
        </div>
        <p style={{ fontSize: 12, fontWeight: 600, letterSpacing: '0.06em', color: tc(dark, 0.45) }}>
          Code {codeNum} of 12
        </p>
      </div>

      {/* Bottom CTA */}
      <div style={{ padding: '0 20px 44px' }}>
        <p style={{ textAlign: 'center', fontSize: 15, fontWeight: 600, color: tc(dark, 0.75), marginBottom: 16 }}>
          Did your AC respond?
        </p>
        <div style={{ display: 'flex', gap: 10 }}>
          <RoundBtn onClick={onNext} tint={tint} rgb={rgb} style={{ flex: 1, fontSize: 14 }}>
            Yes, it worked
          </RoundBtn>
          <RoundBtn variant="secondary" dark={dark} style={{ flex: 1, fontSize: 14 }}>
            Not yet
          </RoundBtn>
        </div>
      </div>
    </div>
  );
}

// ─── Screen: Remote ───────────────────────────────────────────────────────────
function RemoteScreen({ dark, mode: initMode, onTimer, onSettings }: {
  dark: boolean;
  mode: AcMode;
  onTimer: () => void;
  onSettings: () => void;
}) {
  const [mode, setMode] = useState<AcMode>(initMode);
  const [temp, setTemp] = useState(24);
  const [powered, setPowered] = useState(true);
  const [fanSpeed, setFanSpeed] = useState(2);
  const [swingOn, setSwingOn] = useState(true);
  const { tint, rgb } = MODES[mode];

  const onInc = useCallback(() => setTemp(t => Math.min(30, t + 1)), []);
  const onDec = useCallback(() => setTemp(t => Math.max(16, t - 1)), []);

  const bgStyle = powered ? getBg(rgb, dark) : (dark ? { background: '#0E1116' } : { background: '#F4F7FB' });
  const effectiveTint = powered ? tint : '#94A3B8';
  const effectiveRgb = powered ? rgb : '148,163,184';

  return (
    <div style={{ ...bgStyle, height: '100%', display: 'flex', flexDirection: 'column', position: 'relative', transition: 'background 0.5s' }}>
      <div style={{ height: 52 }} />
      {/* Top bar */}
      <div style={{ display: 'flex', alignItems: 'center', padding: '8px 20px', gap: 8 }}>
        <button style={{ display: 'flex', alignItems: 'center', gap: 6, background: 'transparent', border: 'none', cursor: 'pointer', outline: 'none', flex: 1 }}>
          <span style={{ fontSize: 18, fontWeight: 700, color: tc(dark) }}>Living Room</span>
          <Ic n="expand_more" sz={20} style={{ color: tc(dark, 0.55) }} />
        </button>
        <button
          onClick={onSettings}
          aria-label="Open settings"
          style={{
            width: 44, height: 44, borderRadius: 9999, border: 'none', outline: 'none', cursor: 'pointer',
            background: 'transparent', display: 'flex', alignItems: 'center', justifyContent: 'center',
            color: tc(dark, 0.55),
          }}
        >
          <Ic n="settings" sz={22} />
        </button>
        <button onClick={() => setPowered(p => !p)} style={{
          width: 44, height: 44, borderRadius: 9999, border: 'none', outline: 'none', cursor: 'pointer',
          background: powered ? `rgba(${effectiveRgb},0.16)` : (dark ? 'rgba(255,255,255,0.08)' : 'rgba(0,0,0,0.07)'),
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          color: powered ? effectiveTint : tc(dark, 0.45), transition: 'all 0.2s',
          boxShadow: powered ? `0 0 16px rgba(${effectiveRgb},0.35)` : 'none',
        }}>
          <Ic n="power_settings_new" sz={22} />
        </button>
      </div>

      {/* Temperature dial */}
      <div style={{ display: 'flex', justifyContent: 'center', padding: '8px 20px 16px' }}>
        <TempDial temp={temp} onInc={onInc} onDec={onDec} dark={dark} tint={effectiveTint} rgb={effectiveRgb} powered={powered} />
      </div>

      {/* Mode chips */}
      <div style={{ padding: '0 20px 16px' }}>
        <div style={{ display: 'flex', gap: 8, overflowX: 'auto' }} className="no-scrollbar">
          {(Object.entries(MODES) as [AcMode, typeof MODES[AcMode]][]).map(([key, m]) => {
            const active = mode === key && powered;
            return (
              <button key={key} onClick={() => powered && setMode(key)} style={{
                display: 'flex', alignItems: 'center', gap: 6, padding: '8px 14px', borderRadius: 9999, border: 'none', outline: 'none',
                cursor: powered ? 'pointer' : 'not-allowed', whiteSpace: 'nowrap', flexShrink: 0,
                background: active ? m.tint : (dark ? 'rgba(255,255,255,0.09)' : 'rgba(0,0,0,0.07)'),
                color: active ? '#fff' : tc(dark, powered ? 0.75 : 0.35),
                fontSize: 13, fontWeight: 600, fontFamily: "'Plus Jakarta Sans', sans-serif",
                boxShadow: active ? `0 4px 14px rgba(${m.rgb},0.4)` : 'none',
                transition: 'all 0.2s', opacity: powered ? 1 : 0.45,
              }}>
                <Ic n={m.icon} sz={16} />
                {m.label}
              </button>
            );
          })}
        </div>
      </div>

      {/* Cards row: Fan Speed + Swing */}
      <div style={{ display: 'flex', gap: 12, padding: '0 20px 16px' }}>
        {/* Fan speed */}
        <div style={{ ...gss(dark, effectiveRgb), flex: 1, padding: '16px', opacity: powered ? 1 : 0.38, transition: 'opacity 0.3s' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 14 }}>
            <Ic n="mode_fan" sz={18} style={{ color: effectiveTint }} />
            <span style={{ fontSize: 12, fontWeight: 700, letterSpacing: '0.06em', textTransform: 'uppercase', color: tc(dark, 0.5) }}>Fan</span>
          </div>
          <FanSteps value={fanSpeed} onChange={v => powered && setFanSpeed(v)} tint={effectiveTint} dark={dark} />
          <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 8 }}>
            {['Low', '', 'Mid', '', 'High'].filter((_, i) => i % 2 === 0).map((l, i) => (
              <span key={i} style={{ fontSize: 10, color: tc(dark, 0.38), fontWeight: 500 }}>{l}</span>
            ))}
          </div>
        </div>
        {/* Swing */}
        <div style={{ ...gss(dark, effectiveRgb), flex: 1, padding: '16px', opacity: powered ? 1 : 0.38, transition: 'opacity 0.3s' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 14 }}>
            <Ic n="air" sz={18} style={{ color: effectiveTint }} />
            <span style={{ fontSize: 12, fontWeight: 700, letterSpacing: '0.06em', textTransform: 'uppercase', color: tc(dark, 0.5) }}>Swing</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <Toggle on={swingOn} onChange={v => powered && setSwingOn(v)} tint={effectiveTint} rgb={effectiveRgb} />
            <Ic n={swingOn ? 'swap_vert' : 'stop'} sz={20} style={{ color: swingOn ? effectiveTint : tc(dark, 0.3) }} />
          </div>
        </div>
      </div>

      {/* Bottom sheet peeking */}
      <div style={{ flex: 1 }} />
      <div style={{
        borderRadius: '24px 24px 0 0', padding: '14px 20px 28px',
        background: dark ? 'rgba(14,17,22,0.88)' : 'rgba(255,255,255,0.88)',
        backdropFilter: 'blur(28px)', WebkitBackdropFilter: 'blur(28px)',
        borderTop: dark ? '1px solid rgba(255,255,255,0.08)' : '1px solid rgba(0,0,0,0.06)',
      }}>
        <div style={{ width: 36, height: 4, borderRadius: 2, background: tc(dark, 0.18), margin: '0 auto 14px' }} />
        <p style={{ fontSize: 11, fontWeight: 700, letterSpacing: '0.1em', textTransform: 'uppercase', color: tc(dark, 0.4), marginBottom: 12 }}>
          Presets
        </p>
        <div style={{ display: 'flex', gap: 8 }}>
          {[
            { label: 'Sleep',  icon: 'bedtime' },
            { label: 'Eco',    icon: 'eco' },
            { label: 'Turbo',  icon: 'bolt' },
            { label: 'Timer',  icon: 'timer' },
          ].map(p => (
            <button key={p.label} onClick={p.label === 'Timer' ? onTimer : undefined} style={{
              display: 'flex', alignItems: 'center', gap: 6, padding: '9px 14px',
              borderRadius: 9999, border: 'none', outline: 'none', cursor: 'pointer',
              background: dark ? 'rgba(255,255,255,0.09)' : 'rgba(0,0,0,0.07)',
              color: tc(dark, powered ? 0.8 : 0.35),
              fontSize: 13, fontWeight: 600, fontFamily: "'Plus Jakarta Sans', sans-serif",
              opacity: powered ? 1 : 0.4, transition: 'opacity 0.3s',
            }}>
              <Ic n={p.icon} sz={16} />
              {p.label}
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}

// ─── Screen: Timer ────────────────────────────────────────────────────────────
function TimerScreen({ dark, onBack }: { dark: boolean; onBack: () => void }) {
  const [tab, setTab] = useState<'off' | 'on'>('off');
  const [hours, setHours] = useState(2);
  const [minutes, setMinutes] = useState(30);
  const { tint, rgb } = MODES.cool;

  const nudge = (setter: React.Dispatch<React.SetStateAction<number>>, max: number, by: number) => {
    setter(v => ((v + by) % max + max) % max);
  };

  return (
    <div style={{ ...getBg(rgb, dark), height: '100%', display: 'flex', flexDirection: 'column' }}>
      <div style={{ height: 52 }} />
      <div style={{ display: 'flex', alignItems: 'center', padding: '8px 8px 4px', gap: 4 }}>
        <button onClick={onBack} style={{ width: 44, height: 44, borderRadius: 9999, border: 'none', background: 'transparent', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', color: tc(dark, 0.75) }}>
          <Ic n="arrow_back" sz={22} />
        </button>
        <span style={{ fontSize: 20, fontWeight: 700, color: tc(dark), flex: 1 }}>Timer</span>
      </div>

      {/* Tabs */}
      <div style={{ display: 'flex', gap: 8, padding: '16px 20px 24px' }}>
        {(['off', 'on'] as const).map(t => {
          const active = tab === t;
          return (
            <button key={t} onClick={() => setTab(t)} style={{
              flex: 1, height: 44, borderRadius: 12, border: 'none', outline: 'none', cursor: 'pointer',
              background: active ? tint : (dark ? 'rgba(255,255,255,0.08)' : 'rgba(0,0,0,0.07)'),
              color: active ? '#fff' : tc(dark, 0.6),
              fontSize: 14, fontWeight: 700, fontFamily: "'Plus Jakarta Sans', sans-serif",
              boxShadow: active ? `0 4px 14px rgba(${rgb},0.38)` : 'none',
              transition: 'all 0.2s',
            }}>
              {t === 'off' ? 'Turn off in' : 'Turn on at'}
            </button>
          );
        })}
      </div>

      {/* Time picker */}
      <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
          {/* Hours */}
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 12 }}>
            <button onClick={() => nudge(setHours, 24, 1)} style={{ width: 44, height: 44, borderRadius: 9999, border: 'none', background: dark ? 'rgba(255,255,255,0.09)' : 'rgba(0,0,0,0.07)', cursor: 'pointer', outline: 'none', display: 'flex', alignItems: 'center', justifyContent: 'center', color: tc(dark, 0.7) }}>
              <Ic n="expand_less" sz={22} />
            </button>
            <span style={{ fontSize: 72, fontWeight: 800, lineHeight: 1, color: tc(dark), minWidth: 88, textAlign: 'center' }}>
              {String(hours).padStart(2, '0')}
            </span>
            <button onClick={() => nudge(setHours, 24, -1)} style={{ width: 44, height: 44, borderRadius: 9999, border: 'none', background: dark ? 'rgba(255,255,255,0.09)' : 'rgba(0,0,0,0.07)', cursor: 'pointer', outline: 'none', display: 'flex', alignItems: 'center', justifyContent: 'center', color: tc(dark, 0.7) }}>
              <Ic n="expand_more" sz={22} />
            </button>
            <span style={{ fontSize: 11, fontWeight: 700, letterSpacing: '0.1em', textTransform: 'uppercase', color: tc(dark, 0.38) }}>Hours</span>
          </div>
          {/* Colon */}
          <span style={{ fontSize: 56, fontWeight: 800, color: tc(dark, 0.25), marginBottom: 12, lineHeight: 1 }}>:</span>
          {/* Minutes */}
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 12 }}>
            <button onClick={() => nudge(setMinutes, 60, 5)} style={{ width: 44, height: 44, borderRadius: 9999, border: 'none', background: dark ? 'rgba(255,255,255,0.09)' : 'rgba(0,0,0,0.07)', cursor: 'pointer', outline: 'none', display: 'flex', alignItems: 'center', justifyContent: 'center', color: tc(dark, 0.7) }}>
              <Ic n="expand_less" sz={22} />
            </button>
            <span style={{ fontSize: 72, fontWeight: 800, lineHeight: 1, color: tc(dark), minWidth: 88, textAlign: 'center' }}>
              {String(minutes).padStart(2, '0')}
            </span>
            <button onClick={() => nudge(setMinutes, 60, -5)} style={{ width: 44, height: 44, borderRadius: 9999, border: 'none', background: dark ? 'rgba(255,255,255,0.09)' : 'rgba(0,0,0,0.07)', cursor: 'pointer', outline: 'none', display: 'flex', alignItems: 'center', justifyContent: 'center', color: tc(dark, 0.7) }}>
              <Ic n="expand_more" sz={22} />
            </button>
            <span style={{ fontSize: 11, fontWeight: 700, letterSpacing: '0.1em', textTransform: 'uppercase', color: tc(dark, 0.38) }}>Minutes</span>
          </div>
        </div>
      </div>

      <div style={{ padding: '24px 20px 44px' }}>
        <RoundBtn tint={tint} rgb={rgb} style={{ width: '100%' }}>
          Set timer
        </RoundBtn>
      </div>
    </div>
  );
}

// ─── Screen: Settings ─────────────────────────────────────────────────────────
function SettingsScreen({ dark, onBack, onDarkChange }: { dark: boolean; onBack: () => void; onDarkChange: (d: boolean | null) => void }) {
  const [notif1, setNotif1] = useState(true);
  const [notif2, setNotif2] = useState(true);
  const [notif3, setNotif3] = useState(false);
  const [appearance, setAppearance] = useState<'light' | 'dark' | 'system'>('system');
  const { tint, rgb } = MODES.auto;

  const Section = ({ title, children }: { title: string; children: React.ReactNode }) => (
    <div style={{ marginBottom: 20 }}>
      <p style={{ fontSize: 11, fontWeight: 700, letterSpacing: '0.1em', textTransform: 'uppercase', color: tc(dark, 0.38), marginBottom: 8, paddingLeft: 4 }}>{title}</p>
      <div style={{ ...gss(dark), overflow: 'hidden' }}>{children}</div>
    </div>
  );

  const Row = ({ icon, label, right, border = true }: { icon: string; label: string; right: React.ReactNode; border?: boolean }) => (
    <div style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '14px 16px', borderBottom: border ? (dark ? '1px solid rgba(255,255,255,0.06)' : '1px solid rgba(0,0,0,0.05)') : 'none' }}>
      <Ic n={icon} sz={20} style={{ color: tc(dark, 0.45) }} />
      <span style={{ flex: 1, fontSize: 15, fontWeight: 500, color: tc(dark, 0.85) }}>{label}</span>
      {right}
    </div>
  );

  const AcRow = ({ name, room, border }: { name: string; room: string; border: boolean }) => (
    <div style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '12px 16px', borderBottom: border ? (dark ? '1px solid rgba(255,255,255,0.06)' : '1px solid rgba(0,0,0,0.05)') : 'none' }}>
      <div style={{ width: 36, height: 36, borderRadius: 12, background: dark ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.07)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <Ic n="ac_unit" sz={18} style={{ color: MODES.cool.tint }} />
      </div>
      <div style={{ flex: 1 }}>
        <p style={{ fontSize: 14, fontWeight: 600, color: tc(dark, 0.9), margin: 0 }}>{name}</p>
        <p style={{ fontSize: 12, color: tc(dark, 0.45), margin: '2px 0 0' }}>{room}</p>
      </div>
      <button style={{ background: 'transparent', border: 'none', cursor: 'pointer', padding: 4 }}>
        <Ic n="edit" sz={18} style={{ color: tc(dark, 0.4) }} />
      </button>
      <button style={{ background: 'transparent', border: 'none', cursor: 'pointer', padding: 4 }}>
        <Ic n="delete" sz={18} style={{ color: '#FF4D4D' }} />
      </button>
    </div>
  );

  return (
    <div style={{ ...getBg(rgb, dark), height: '100%', display: 'flex', flexDirection: 'column' }}>
      <div style={{ height: 52 }} />
      <div style={{ display: 'flex', alignItems: 'center', padding: '8px 8px 4px', gap: 4 }}>
        <button onClick={onBack} style={{ width: 44, height: 44, borderRadius: 9999, border: 'none', background: 'transparent', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', color: tc(dark, 0.75) }}>
          <Ic n="arrow_back" sz={22} />
        </button>
        <span style={{ fontSize: 20, fontWeight: 700, color: tc(dark) }}>Settings</span>
      </div>

      <div style={{ flex: 1, overflowY: 'auto', padding: '16px 20px 44px' }} className="no-scrollbar">
        <Section title="My ACs">
          <AcRow name="Living Room AC" room="Daikin · Code 3" border={true} />
          <AcRow name="Bedroom AC" room="LG · Code 7" border={false} />
          <button style={{
            display: 'flex', alignItems: 'center', gap: 10, padding: '13px 16px', width: '100%',
            border: 'none', outline: 'none', cursor: 'pointer', background: 'transparent',
            borderTop: dark ? '1px solid rgba(255,255,255,0.06)' : '1px solid rgba(0,0,0,0.05)',
          }}>
            <Ic n="add_circle" sz={20} style={{ color: MODES.cool.tint }} />
            <span style={{ fontSize: 14, fontWeight: 600, color: MODES.cool.tint }}>Add AC</span>
          </button>
        </Section>

        <Section title="Notifications">
          <Row icon="thermostat" label="Temperature alerts" right={<Toggle on={notif1} onChange={setNotif1} tint={MODES.cool.tint} rgb={MODES.cool.rgb} />} />
          <Row icon="timer" label="Timer reminders" right={<Toggle on={notif2} onChange={setNotif2} tint={MODES.cool.tint} rgb={MODES.cool.rgb} />} />
          <Row icon="eco" label="Eco tips" right={<Toggle on={notif3} onChange={setNotif3} tint={MODES.cool.tint} rgb={MODES.cool.rgb} />} border={false} />
        </Section>

        <Section title="Appearance">
          <div style={{ padding: '12px 16px' }}>
            <div style={{ display: 'flex', background: dark ? 'rgba(255,255,255,0.08)' : 'rgba(0,0,0,0.07)', borderRadius: 12, padding: 3, gap: 2 }}>
              {(['light', 'dark', 'system'] as const).map(opt => (
                <button key={opt} onClick={() => { setAppearance(opt); if (opt !== 'system') onDarkChange(opt === 'dark'); else onDarkChange(null); }} style={{
                  flex: 1, height: 36, borderRadius: 10, border: 'none', outline: 'none', cursor: 'pointer',
                  background: appearance === opt ? (dark ? 'rgba(255,255,255,0.14)' : '#fff') : 'transparent',
                  color: tc(dark, appearance === opt ? 0.9 : 0.5),
                  fontSize: 13, fontWeight: 600, fontFamily: "'Plus Jakarta Sans', sans-serif",
                  boxShadow: appearance === opt ? (dark ? 'none' : '0 1px 6px rgba(0,0,0,0.14)') : 'none',
                  textTransform: 'capitalize', transition: 'all 0.18s',
                }}>
                  {opt.charAt(0).toUpperCase() + opt.slice(1)}
                </button>
              ))}
            </div>
          </div>
        </Section>

        <Section title="About">
          <Row icon="info" label="Version 1.0.0" right={<span style={{ fontSize: 13, color: tc(dark, 0.4) }}>v1.0.0</span>} />
          <Row icon="privacy_tip" label="Privacy Policy" right={<Ic n="chevron_right" sz={18} style={{ color: tc(dark, 0.3) }} />} />
          <Row icon="gavel" label="Terms of Service" right={<Ic n="chevron_right" sz={18} style={{ color: tc(dark, 0.3) }} />} border={false} />
        </Section>
      </div>
    </div>
  );
}

// ─── Screen: Component Sheet ──────────────────────────────────────────────────
function ComponentsScreen({ dark }: { dark: boolean }) {
  const { tint, rgb } = MODES.cool;
  const SectionTitle = ({ t }: { t: string }) => (
    <p style={{ fontSize: 11, fontWeight: 700, letterSpacing: '0.12em', textTransform: 'uppercase', color: tc(dark, 0.38), margin: '20px 0 10px' }}>{t}</p>
  );
  return (
    <div style={{ height: '100%', overflowY: 'auto', background: dark ? '#0E1116' : '#F4F7FB' }} className="no-scrollbar">
      <div style={{ padding: '64px 20px 40px' }}>
        <p style={{ fontSize: 22, fontWeight: 800, color: tc(dark), marginBottom: 4 }}>Components</p>
        <p style={{ fontSize: 13, color: tc(dark, 0.45), marginBottom: 8 }}>All reusable Breeze UI primitives</p>

        <SectionTitle t="Buttons" />
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          <RoundBtn tint={tint} rgb={rgb} style={{ width: '100%' }}>Primary — Get started</RoundBtn>
          <RoundBtn variant="secondary" dark={dark} tint={tint} rgb={rgb} style={{ width: '100%' }}>Secondary — Continue</RoundBtn>
          <RoundBtn variant="ghost" dark={dark} tint={tint} style={{ width: '100%' }}>Ghost — Learn more</RoundBtn>
          <RoundBtn tint={tint} rgb={rgb} disabled style={{ width: '100%' }}>Disabled state</RoundBtn>
        </div>

        <SectionTitle t="Mode Chips" />
        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
          {(Object.entries(MODES) as [AcMode, typeof MODES[AcMode]][]).map(([key, m]) => (
            <div key={key} style={{
              display: 'flex', alignItems: 'center', gap: 6, padding: '8px 14px',
              borderRadius: 9999, background: m.tint, color: '#fff',
              fontSize: 13, fontWeight: 600, fontFamily: "'Plus Jakarta Sans', sans-serif",
              boxShadow: `0 4px 12px rgba(${m.rgb},0.35)`,
            }}>
              <Ic n={m.icon} sz={15} />
              {m.label}
            </div>
          ))}
        </div>
        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginTop: 8 }}>
          {(Object.entries(MODES) as [AcMode, typeof MODES[AcMode]][]).map(([key, m]) => (
            <div key={key} style={{
              display: 'flex', alignItems: 'center', gap: 6, padding: '8px 14px',
              borderRadius: 9999, background: dark ? 'rgba(255,255,255,0.09)' : 'rgba(0,0,0,0.07)',
              color: tc(dark, 0.65), fontSize: 13, fontWeight: 600, fontFamily: "'Plus Jakarta Sans', sans-serif",
            }}>
              <Ic n={m.icon} sz={15} />
              {m.label}
            </div>
          ))}
        </div>

        <SectionTitle t="Glass Cards" />
        <div style={{ display: 'flex', gap: 10 }}>
          <div style={{ ...gss(false), flex: 1, padding: 16 }}>
            <p style={{ fontSize: 12, fontWeight: 700, color: tc(false, 0.5), marginBottom: 4 }}>LIGHT</p>
            <p style={{ fontSize: 14, fontWeight: 600, color: tc(false) }}>Glass Card</p>
            <p style={{ fontSize: 12, color: tc(false, 0.45), marginTop: 4 }}>backdrop-blur: 20px</p>
          </div>
          <div style={{ ...gss(true), flex: 1, padding: 16 }}>
            <p style={{ fontSize: 12, fontWeight: 700, color: tc(true, 0.5), marginBottom: 4 }}>DARK</p>
            <p style={{ fontSize: 14, fontWeight: 600, color: tc(true) }}>Glass Card</p>
            <p style={{ fontSize: 12, color: tc(true, 0.45), marginTop: 4 }}>backdrop-blur: 20px</p>
          </div>
        </div>

        <SectionTitle t="Toggle Switches" />
        <div style={{ display: 'flex', gap: 14, alignItems: 'center' }}>
          {(Object.entries(MODES) as [AcMode, typeof MODES[AcMode]][]).map(([key, m]) => (
            <Toggle key={key} on={true} onChange={() => {}} tint={m.tint} rgb={m.rgb} />
          ))}
          <Toggle on={false} onChange={() => {}} tint={tint} rgb={rgb} />
        </div>

        <SectionTitle t="Fan Speed Slider" />
        <div style={{ ...gss(dark), padding: 16, display: 'flex', flexDirection: 'column', gap: 12 }}>
          {[1, 2, 3, 4].map(v => (
            <div key={v} style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
              <span style={{ fontSize: 12, color: tc(dark, 0.5), width: 40 }}>Speed {v}</span>
              <FanSteps value={v} onChange={() => {}} tint={tint} dark={dark} />
            </div>
          ))}
        </div>

        <SectionTitle t="Brand Row" />
        <div style={{ ...gss(dark), overflow: 'hidden' }}>
          {BRANDS.slice(0, 3).map((b, i) => {
            const sel = i === 1;
            return (
              <div key={b.name} style={{
                display: 'flex', alignItems: 'center', gap: 14, padding: '12px 16px',
                background: sel ? `rgba(${MODES.cool.rgb},0.12)` : 'transparent',
                borderBottom: i < 2 ? (dark ? '1px solid rgba(255,255,255,0.06)' : '1px solid rgba(0,0,0,0.05)') : 'none',
              }}>
                <div style={{ width: 40, height: 40, borderRadius: '50%', background: sel ? `rgba(${MODES.cool.rgb},0.22)` : (dark ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.08)'), display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 14, fontWeight: 700, color: sel ? MODES.cool.tint : tc(dark, 0.6), border: sel ? `1.5px solid rgba(${MODES.cool.rgb},0.4)` : '1.5px solid transparent' }}>
                  {b.initial}
                </div>
                <span style={{ flex: 1, fontSize: 15, fontWeight: sel ? 600 : 500, color: sel ? MODES.cool.tint : tc(dark, 0.85) }}>{b.name}</span>
                {sel
                  ? <Ic n="check_circle" sz={22} style={{ color: MODES.cool.tint }} />
                  : <Ic n="chevron_right" sz={20} style={{ color: tc(dark, 0.3) }} />
                }
              </div>
            );
          })}
        </div>

        <SectionTitle t="Color Tokens" />
        <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
          {(Object.entries(MODES) as [AcMode, typeof MODES[AcMode]][]).map(([key, m]) => (
            <div key={key} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 6 }}>
              <div style={{ width: 44, height: 44, borderRadius: 14, background: m.tint, boxShadow: `0 4px 14px rgba(${m.rgb},0.45)` }} />
              <span style={{ fontSize: 10, fontWeight: 600, color: tc(dark, 0.5), textTransform: 'capitalize' }}>{key}</span>
            </div>
          ))}
          {[['Surface·L', '#F4F7FB'], ['Surface·D', '#0E1116']].map(([label, hex]) => (
            <div key={label} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 6 }}>
              <div style={{ width: 44, height: 44, borderRadius: 14, background: hex, border: dark ? '1px solid rgba(255,255,255,0.12)' : '1px solid rgba(0,0,0,0.10)' }} />
              <span style={{ fontSize: 10, fontWeight: 600, color: tc(dark, 0.5) }}>{label}</span>
            </div>
          ))}
        </div>

        <SectionTitle t="Type Scale" />
        <div style={{ ...gss(dark), padding: 16, display: 'flex', flexDirection: 'column', gap: 8 }}>
          {[
            { size: 64, weight: 800, label: '64 — Temperature' },
            { size: 28, weight: 800, label: '28 — Screen title' },
            { size: 18, weight: 700, label: '18 — Section title' },
            { size: 15, weight: 500, label: '15 — Body' },
            { size: 12, weight: 700, label: '12 — LABEL' },
          ].map(({ size, weight, label }) => (
            <div key={size} style={{ display: 'flex', alignItems: 'baseline', gap: 10 }}>
              <span style={{ fontSize: size, fontWeight: weight, lineHeight: 1.1, color: tc(dark), flexShrink: 0 }}>
                {size}
              </span>
              <span style={{ fontSize: 12, color: tc(dark, 0.4) }}>{label}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

// ─── App Shell ────────────────────────────────────────────────────────────────
const SCREEN_ORDER: ScreenId[] = [
  'welcome', 'brand', 'findcode1', 'findcode2', 'remote-cool',
  'remote-heat', 'timer', 'settings', 'components',
];

export default function App() {
  const [current, setCurrent] = useState<ScreenId>('welcome');
  const [prev, setPrev] = useState<ScreenId | null>(null);
  const [direction, setDirection] = useState<'forward' | 'back'>('forward');
  const [animating, setAnimating] = useState(false);
  const [isDark, setIsDark] = useState(false);

  const navigate = useCallback((to: ScreenId, forcedDir?: 'forward' | 'back') => {
    if (to === current || animating) return;
    const dir = forcedDir ?? (
      SCREEN_ORDER.indexOf(to) > SCREEN_ORDER.indexOf(current) ? 'forward' : 'back'
    );
    setPrev(current);
    setCurrent(to);
    setDirection(dir);
    setAnimating(true);
    setTimeout(() => {
      setPrev(null);
      setAnimating(false);
    }, 310);
  }, [current, animating]);

  const renderById = (id: ScreenId) => {
    switch (id) {
      case 'welcome':
        return <WelcomeScreen dark={isDark} onNext={() => navigate('brand', 'forward')} />;
      case 'brand':
        return <BrandScreen dark={isDark} onNext={() => navigate('findcode1', 'forward')} onBack={() => navigate('welcome', 'back')} />;
      case 'findcode1':
        return <FindCodeScreen dark={isDark} showLeft={false} onNext={() => navigate('remote-cool', 'forward')} onBack={() => navigate('brand', 'back')} />;
      case 'findcode2':
        return <FindCodeScreen dark={isDark} showLeft={true} onNext={() => navigate('remote-cool', 'forward')} onBack={() => navigate('brand', 'back')} />;
      case 'remote-cool':
        return <RemoteScreen dark={isDark} mode="cool" onTimer={() => navigate('timer', 'forward')} onSettings={() => navigate('settings', 'forward')} />;
      case 'remote-heat':
        return <RemoteScreen dark={isDark} mode="heat" onTimer={() => navigate('timer', 'forward')} onSettings={() => navigate('settings', 'forward')} />;
      case 'timer':
        return <TimerScreen dark={isDark} onBack={() => navigate('remote-cool', 'back')} />;
      case 'settings':
        return <SettingsScreen dark={isDark} onBack={() => navigate('remote-cool', 'back')} onDarkChange={v => { if (v !== null) setIsDark(v); }} />;
      case 'components':
        return <ComponentsScreen dark={isDark} />;
    }
  };

  const showcaseBg = isDark ? '#07090D' : '#DDE4F0';

  return (
    <div className="app-shell" style={{ minHeight: '100vh', background: showcaseBg, fontFamily: "'Plus Jakarta Sans', sans-serif", display: 'flex', flexDirection: 'column' }}>
      <div className="phone-stage" style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '40px 20px' }}>
        <div className="phone-frame" style={{
          width: 360, height: 800, borderRadius: 40, overflow: 'hidden', flexShrink: 0, position: 'relative',
          boxShadow: isDark
            ? '0 40px 100px rgba(0,0,0,0.7), 0 0 0 1px rgba(255,255,255,0.07), inset 0 0 0 1px rgba(255,255,255,0.04)'
            : '0 40px 100px rgba(0,0,0,0.22), 0 0 0 1px rgba(0,0,0,0.08)',
        }}>
          {/* Status bar */}
          <div style={{
            position: 'absolute', top: 0, left: 0, right: 0, height: 52, zIndex: 50,
            display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between',
            padding: '14px 24px 0',
          }}>
            <span style={{ fontSize: 12, fontWeight: 700, color: isDark ? 'rgba(244,247,251,0.7)' : 'rgba(14,17,22,0.5)' }}>9:41</span>
            <div style={{ display: 'flex', alignItems: 'center', gap: 5 }}>
              <span className="material-symbols-rounded" style={{ fontSize: 14, color: isDark ? 'rgba(244,247,251,0.7)' : 'rgba(14,17,22,0.5)' }}>signal_cellular_alt</span>
              <span className="material-symbols-rounded" style={{ fontSize: 14, color: isDark ? 'rgba(244,247,251,0.7)' : 'rgba(14,17,22,0.5)' }}>wifi</span>
              <span className="material-symbols-rounded" style={{ fontSize: 14, color: isDark ? 'rgba(244,247,251,0.7)' : 'rgba(14,17,22,0.5)' }}>battery_full</span>
            </div>
          </div>
          {/* Screen content — double-buffer for push transition */}
          <div style={{ position: 'absolute', inset: 0, overflow: 'hidden' }}>
            {/* Previous screen sits static underneath while new one slides in */}
            {animating && prev && (
              <div key={`prev-${prev}`} style={{ position: 'absolute', inset: 0, zIndex: 1 }}>
                {renderById(prev)}
              </div>
            )}
            {/* Current screen: animates in, then stays */}
            <div
              key={current}
              className={animating ? (direction === 'forward' ? 'screen-enter-forward' : 'screen-enter-back') : ''}
              style={{ position: 'absolute', inset: 0, zIndex: 2 }}
            >
              {renderById(current)}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
