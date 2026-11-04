// Device frame for Prumo mocks: AppTheme (seed + mode) + status bar + optional keyboard + gesture handle.
// Screens render inside a flex column (position: relative), below the 40dp status bar.
function PrumoPhone({ mode = 'light', seed = '#6A4F86', keyboard, statusBg, scrim = false, width = 412, height = 915, tablet = false, noHandle = false, children }) {
  const DS = window.HenriqueSebastiOAndroidDesignSystem_5ac522 || {};
  const AppTheme = DS.AppTheme || ((p) => <div style={p.style}>{p.children}</div>);
  const ms = (n, s = 18) => <span className="ms" style={{ fontSize: s, fontVariationSettings: 'var(--icon-fvs-filled)' }}>{n}</span>;
  const handle = <div style={{ width: 108, height: 4, borderRadius: 2, background: 'var(--md-on-surface)', opacity: 0.4 }} />;
  return (
    <div style={{ width, height, borderRadius: tablet ? 32 : 28, overflow: 'hidden', boxShadow: '0 2px 12px rgba(0,0,0,.14)', flex: 'none' }}>
      <AppTheme seed={seed} mode={mode} style={{ width, height, position: 'relative', overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
        <div style={{ flex: 'none', height: tablet ? 32 : 40, padding: '0 24px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', font: '500 14px/20px var(--font-ui)', background: statusBg || 'transparent', position: 'relative', zIndex: 2 }}>
          <span>14:30</span>
          <span style={{ display: 'flex', gap: 4 }}>{ms('wifi')}{ms('signal_cellular_alt')}{ms('battery_5_bar')}</span>
          {scrim && <div style={{ position: 'absolute', inset: 0, background: 'var(--scrim)' }} />}
        </div>
        <div style={{ flex: 1, minHeight: 0, position: 'relative', overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>{children}</div>
        {keyboard === 'numpad' && <PrumoNumpad handle={handle} />}
        {keyboard === 'text' && <PrumoTextKeys handle={handle} />}
        {!keyboard && !noHandle && (
          <div style={{ position: 'absolute', left: 0, right: 0, bottom: 0, height: 24, display: 'flex', alignItems: 'center', justifyContent: 'center', pointerEvents: 'none', zIndex: 5 }}>{handle}</div>
        )}
      </AppTheme>
    </div>
  );
}

const keyBase = { height: 46, borderRadius: 'var(--md-sys-shape-corner-small)', display: 'grid', placeItems: 'center', font: '400 22px/1 var(--font-ui)', color: 'var(--md-on-surface)' };
const keyStd = { ...keyBase, background: 'var(--md-surface-container-lowest)' };
const keyFn = { ...keyBase, background: 'var(--md-surface-container-highest)' };
const keyGo = { ...keyBase, borderRadius: 23, background: 'var(--md-primary)', color: 'var(--md-on-primary)' };
const ico = (n) => <span className="ms" style={{ fontSize: 22 }}>{n}</span>;

function PrumoNumpad({ handle }) {
  const rows = [['1', '2', '3', ['−']], ['4', '5', '6', [ico('space_bar')]], ['7', '8', '9', [ico('backspace')]], [',', '0', '.', ['go']]];
  return (
    <div style={{ flex: 'none', background: 'var(--md-surface-container-high)', padding: '8px 6px 0', display: 'flex', flexDirection: 'column', gap: 6 }}>
      {rows.map((r, i) => (
        <div key={i} style={{ display: 'grid', gridTemplateColumns: 'repeat(3, minmax(0,1fr)) minmax(0,0.8fr)', gap: 6 }}>
          {r.map((k, j) => Array.isArray(k)
            ? <div key={j} style={k[0] === 'go' ? keyGo : keyFn}>{k[0] === 'go' ? ico('check') : k[0]}</div>
            : <div key={j} style={keyStd}>{k}</div>)}
        </div>
      ))}
      <div style={{ height: 24, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>{handle}</div>
    </div>
  );
}

function PrumoTextKeys({ handle }) {
  const k = { ...keyStd, height: 44, font: '400 20px/1 var(--font-ui)' };
  const row = (s, pad = 0) => (
    <div style={{ display: 'flex', gap: 5, padding: `0 ${pad}px` }}>
      {s.split('').map((c) => <div key={c} style={{ ...k, flex: 1 }}>{c}</div>)}
    </div>
  );
  return (
    <div style={{ flex: 'none', background: 'var(--md-surface-container-high)', padding: '8px 5px 0', display: 'flex', flexDirection: 'column', gap: 8 }}>
      {row('qwertyuiop')}
      {row('asdfghjkl', 18)}
      <div style={{ display: 'flex', gap: 5 }}>
        <div style={{ ...keyFn, height: 44, flex: 1.5 }}>{ico('shift')}</div>
        {'zxcvbnm'.split('').map((c) => <div key={c} style={{ ...k, flex: 1 }}>{c}</div>)}
        <div style={{ ...keyFn, height: 44, flex: 1.5 }}>{ico('backspace')}</div>
      </div>
      <div style={{ display: 'flex', gap: 5 }}>
        <div style={{ ...keyFn, height: 44, flex: 1.5, font: '500 14px/1 var(--font-ui)' }}>?123</div>
        <div style={{ ...k, flex: 1 }}>@</div>
        <div style={{ ...k, flex: 5 }} />
        <div style={{ ...k, flex: 1 }}>.</div>
        <div style={{ ...keyGo, height: 44, borderRadius: 22, flex: 1.5 }}>{ico('arrow_forward')}</div>
      </div>
      <div style={{ height: 24, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>{handle}</div>
    </div>
  );
}

Object.assign(window, { PrumoPhone, PrumoNumpad, PrumoTextKeys });
