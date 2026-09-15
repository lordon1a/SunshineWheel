'use strict';

/* Sunshine Wheel web editor — no build step, single file. */

const LS_KEY = 'sunshinewheel.session';

const state = {
  session: localStorage.getItem(LS_KEY) || '',
  wheels: {},
  current: null,
  dirty: false,
  tab: 'general',
  materials: [],
  meta: null,
  rewardEdit: -1,
};

const FALLBACK_EFFECTS = [
  { id: 'NONE', label: 'None' },
  { id: 'FIREWORK', label: 'Firework' },
  { id: 'ROCKET', label: 'Rocket' },
  { id: 'SPIRAL', label: 'Spiral' },
];

const TABS = [
  { id: 'general', label: 'General', icon: '🛠', fields: [
    { key: 'name', label: 'Display name (MiniMessage)', type: 'text' },
    { key: 'subtitle', label: 'Subtitle (MiniMessage)', type: 'text' },
    { key: 'enabled', label: 'Wheel enabled', type: 'bool' },
    { key: 'permission', label: 'Spin permission (blank = everyone)', type: 'text' },
    { key: 'broadcastWin', label: 'Broadcast wins server-wide', type: 'bool' },
  ]},
  { id: 'location', label: 'Location', icon: '📍', fields: [
    { key: 'world', label: 'World', type: 'world' },
    { key: 'x', label: 'X', type: 'number', step: 0.1 },
    { key: 'y', label: 'Y', type: 'number', step: 0.1 },
    { key: 'z', label: 'Z', type: 'number', step: 0.1 },
    { key: 'yaw', label: 'Yaw', type: 'number', step: 90 },
  ], hint: 'Tip: move the wheel in place with /wheel move <id> in-game.' },
  { id: 'rewards', label: 'Rewards', icon: '🎁', type: 'rewards' },
  { id: 'animation', label: 'Animation', icon: '▶', fields: [
    { key: 'duration', label: 'Duration (ticks)', type: 'range', min: 20, max: 600, step: 5 },
    { key: 'revolutions', label: 'Revolutions', type: 'range', min: 1, max: 30, step: 0.5 },
    { key: 'style', label: 'Motion style', type: 'style' },
  ]},
  { id: 'cost', label: 'Cost & Ticket', icon: '🎟', fields: [
    { key: 'moneyCost', label: 'Money cost (0 = off, needs Vault)', type: 'number', min: 0, step: 1 },
    { key: 'xpCost', label: 'XP level cost (0 = off)', type: 'number', min: 0, max: 10000 },
    { key: 'ticketItem', label: 'Ticket item (blank = none)', type: 'material' },
    { key: 'ticketAmount', label: 'Ticket amount', type: 'number', min: 1, max: 64 },
    { key: 'costCommands', label: 'Console commands before each spin (one per line, %player% %wheel%)', type: 'lines' },
  ]},
  { id: 'cooldown', label: 'Cooldown', icon: '⏱', fields: [
    { key: 'cooldownSeconds', label: 'Cooldown (seconds, 0 = off)', type: 'number', min: 0, max: 86400 },
  ]},
  { id: 'appearance', label: 'Appearance', icon: '✨', fields: [
    { key: 'pixelSize', label: 'Pixel size (blocks)', type: 'number', step: 0.001, min: 0.005, max: 0.2 },
    { key: 'imageSize', label: 'Image size (px)', type: 'number', min: 32, max: 256, step: 8 },
    { key: 'frameColor', label: 'Frame color', type: 'color' },
    { key: 'hubColor', label: 'Hub color', type: 'color' },
    { key: 'pointerColor', label: 'Pointer color', type: 'color' },
  ]},
  { id: 'items', label: 'Reward Items', icon: '⚙', fields: [
    { key: 'itemsUpright', label: 'Items stay upright', type: 'bool' },
    { key: 'itemScale', label: 'Item scale', type: 'range', min: 0.05, max: 2, step: 0.05 },
  ]},
  { id: 'holograms', label: 'Holograms', icon: '💬', fields: [
    { key: 'showHologram', label: 'Show floating title', type: 'bool' },
    { key: 'hologramOffset', label: 'Height offset (blocks)', type: 'number', step: 0.05, min: -5, max: 5 },
  ]},
  { id: 'sounds', label: 'Sounds', icon: '♪', fields: [
    { key: 'soundStart', label: 'Start sound', type: 'text' },
    { key: 'soundTick', label: 'Tick sound', type: 'text' },
    { key: 'soundLanding', label: 'Landing sound', type: 'text' },
    { key: 'soundWin', label: 'Win sound (default)', type: 'text' },
  ], hint: 'Format: minecraft:sound.name volume pitch — blank or "none" disables.' },
  { id: 'effects', label: 'Effects', icon: '✦', fields: [
    { key: 'winEffect', label: 'Win celebration', type: 'wineffect' },
    { key: 'spinParticle', label: 'Ambient spin particle (blank = none)', type: 'text' },
    { key: 'spinParticleCount', label: 'Ambient particle count', type: 'number', min: 0, max: 500 },
    { key: 'particleWin', label: 'Win burst particle', type: 'text' },
    { key: 'particleCount', label: 'Win burst count', type: 'number', min: 0, max: 500 },
  ]},
];

const $ = (selector) => document.querySelector(selector);

function el(tag, attrs = {}, ...children) {
  const node = document.createElement(tag);
  for (const [key, value] of Object.entries(attrs)) {
    if (key === 'class') node.className = value;
    else if (key === 'text') node.textContent = value;
    else if (key.startsWith('on')) node.addEventListener(key.slice(2), value);
    else if (value !== null && value !== undefined) node.setAttribute(key, value);
  }
  children.flat().forEach((child) => {
    if (child !== null && child !== undefined) {
      node.append(child.nodeType ? child : document.createTextNode(String(child)));
    }
  });
  return node;
}

function stripTags(value) {
  return String(value || '').replace(/<[^>]+>/g, '').trim();
}

function resolveChance(name, percent) {
  return String(name || '').replace(/<chance>/gi, percent.toFixed(2) + '%');
}

function toast(message, isError = false) {
  const node = $('#toast');
  node.textContent = message;
  node.classList.remove('hidden');
  node.classList.toggle('err', isError);
  clearTimeout(toast.timer);
  toast.timer = setTimeout(() => node.classList.add('hidden'), 3200);
}

function setStatus(text, kind = 'ok') {
  const pill = $('#status');
  pill.textContent = text;
  pill.className = 'pill ' + kind;
}

function markDirty() {
  state.dirty = true;
  setStatus('unsaved', 'dirty');
}

// --- API ---

async function api(path, options = {}) {
  let response;
  try {
    response = await fetch('api/' + path, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        ...(state.session ? { Authorization: 'Bearer ' + state.session } : {}),
        ...(options.headers || {}),
      },
    });
  } catch (error) {
    throw new Error('Cannot reach the server (connection error).');
  }
  const text = await response.text();
  const type = response.headers.get('content-type') || '';
  let data = {};
  if (text && type.includes('application/json')) {
    try { data = JSON.parse(text); } catch { data = {}; }
  }
  if (response.status === 401) {
    logout();
    throw new Error('Session expired, please sign in again.');
  }
  if (!response.ok) {
    throw new Error(data.error || ('Server error (HTTP ' + response.status + ')'));
  }
  if (text && !type.includes('application/json')) {
    throw new Error('Unexpected response format (proxy issue?).');
  }
  return data;
}

async function authenticate(token) {
  const data = await api('auth', { method: 'POST', body: JSON.stringify({ token }) });
  state.session = data.session;
  localStorage.setItem(LS_KEY, data.session);
}

async function logout() {
  const session = state.session;
  state.session = '';
  localStorage.removeItem(LS_KEY);
  if (session) {
    try {
      await fetch('api/logout', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: 'Bearer ' + session },
      });
    } catch (error) {
      // Sunucu zaten kapatmış olabilir; istemci tarafı temizlik yeterli.
    }
  }
  showAuth();
}

function showAuth(message) {
  $('#auth').classList.remove('hidden');
  $('#topbar').classList.add('hidden');
  $('#app').classList.add('hidden');
  if (message) $('#auth-message').textContent = message;
}

function showApp() {
  $('#auth').classList.add('hidden');
  $('#topbar').classList.remove('hidden');
  $('#app').classList.remove('hidden');
}

// --- loading ---

async function boot() {
  const match = location.hash.match(/token=([A-Za-z0-9_-]+)/);
  if (match) {
    history.replaceState(null, '', location.pathname + location.search);
    try {
      await authenticate(match[1]);
    } catch (error) {
      showAuth(error.message);
      return;
    }
  }
  if (!state.session) {
    showAuth();
    return;
  }
  try {
    state.meta = await api('meta');
    state.materials = await api('materials');
    await loadWheels();
    showApp();
  } catch (error) {
    showAuth(error.message);
  }
}

async function loadWheels() {
  const data = await api('wheels');
  state.wheels = {};
  data.wheels.forEach((wheel) => { state.wheels[wheel.id] = wheel; });
  if (!state.current || !state.wheels[state.current]) {
    const ids = Object.keys(state.wheels);
    state.current = ids.length ? ids[0] : null;
  }
  renderWheelList();
  renderEditor();
}

function currentDef() {
  return state.current ? state.wheels[state.current] : null;
}

function renderWheelList() {
  const list = $('#wheel-list');
  list.replaceChildren();
  const ids = Object.keys(state.wheels);
  $('#wheel-count').textContent = ids.length + (ids.length === 1 ? ' wheel' : ' wheels');
  ids.forEach((id) => {
    const def = state.wheels[id];
    const item = el('li', {
      class: id === state.current ? 'active' : '',
      onclick: () => selectWheel(id),
    }, el('span', { text: id }), el('span', { class: 'n', text: (def.rewards || []).length + ' rewards' }));
    list.append(item);
  });
}

function selectWheel(id) {
  if (state.dirty && !confirm('You have unsaved changes. Discard them and switch?')) return;
  state.current = id;
  state.rewardEdit = -1;
  state.dirty = false;
  setStatus('ready', 'ok');
  renderWheelList();
  renderEditor();
}

// --- editor ---

function renderEditor() {
  const def = currentDef();
  $('#wheel-title').textContent = def ? def.id : '— no wheel selected —';
  document.title = def ? def.id + ' — Sunshine Wheel' : 'Sunshine Wheel Editor';
  renderNav();
  renderPanel();
  renderPreview();
}

function renderNav() {
  const nav = $('#side-nav');
  nav.replaceChildren();
  TABS.forEach((tab) => {
    nav.append(el('button', {
      class: tab.id === state.tab ? 'active' : '',
      onclick: () => {
        if (state.tab !== tab.id) {
          state.tab = tab.id;
          renderNav();
          renderPanel();
        }
      },
    }, el('span', { class: 'ico', text: tab.icon }), el('span', { text: tab.label })));
  });
}

function renderPanel() {
  const def = currentDef();
  const panel = $('#panels');
  panel.replaceChildren();
  if (!def) {
    panel.append(el('p', { class: 'dim', text: 'Select a wheel on the left, or add a new one.' }));
    return;
  }
  const tab = TABS.find((item) => item.id === state.tab);
  if (tab.type === 'rewards') {
    renderRewards(panel, def);
    return;
  }
  tab.fields.forEach((field) => panel.append(renderField(def, field)));
  if (tab.hint) panel.append(el('p', { class: 'dim', text: tab.hint }));
}

function renderField(def, field) {
  const value = def[field.key];
  const wrapper = el('div', { class: 'field' });
  wrapper.append(el('label', { text: field.label }));
  let input;
  switch (field.type) {
    case 'range': {
      const range = el('input', {
        type: 'range', min: field.min, max: field.max, step: field.step, value,
        oninput: () => { number.value = range.value; update(field.key, Number(range.value)); },
      });
      const number = el('input', {
        type: 'number', min: field.min, max: field.max, step: field.step, value,
        oninput: () => { range.value = number.value; update(field.key, Number(number.value)); },
      });
      input = el('div', { class: 'row-inline' }, range, number);
      break;
    }
    case 'number':
      input = el('input', {
        type: 'number', value, step: field.step, min: field.min, max: field.max,
        oninput: (event) => update(field.key, event.target.value === '' ? 0 : Number(event.target.value)),
      });
      break;
    case 'bool':
      input = el('input', {
        type: 'checkbox',
        onchange: (event) => update(field.key, event.target.checked),
      });
      input.checked = Boolean(value);
      break;
    case 'color': {
      const picker = el('input', {
        type: 'color', value: normalizeHex(value),
        oninput: () => { hex.value = picker.value.toUpperCase(); update(field.key, picker.value.toUpperCase()); },
      });
      const hex = el('input', {
        type: 'text', value: normalizeHex(value).toUpperCase(),
        oninput: (event) => { const v = event.target.value.trim(); if (/^#[0-9a-fA-F]{6}$/.test(v)) { picker.value = v; update(field.key, v.toUpperCase()); } },
      });
      input = el('div', { class: 'row-inline' }, picker, hex);
      break;
    }
    case 'lines':
      input = el('textarea', {
        oninput: (event) => update(field.key, event.target.value.split('\n').map((line) => line.trim()).filter(Boolean)),
      });
      input.value = (value || []).join('\n');
      break;
    case 'material': {
      input = el('input', {
        list: 'material-list', value: value || '', placeholder: 'e.g. DIAMOND',
        oninput: (event) => update(field.key, event.target.value.trim().toUpperCase()),
      });
      break;
    }
    case 'world': {
      input = el('select', { onchange: (event) => update(field.key, event.target.value) });
      const worlds = (state.meta && state.meta.worlds) || [];
      if (!worlds.includes(value)) input.append(el('option', { value: value || '', text: value || '' }));
      worlds.forEach((world) => input.append(el('option', { value: world, text: world })));
      input.value = value || '';
      break;
    }
    case 'style': {
      input = el('select', { onchange: (event) => update(field.key, event.target.value) });
      const styles = (state.meta && state.meta.styles) || [];
      styles.forEach((style) => input.append(el('option', { value: style.id, text: style.id + ' — ' + style.label })));
      input.value = value;
      break;
    }
    case 'wineffect': {
      input = el('select', { onchange: (event) => update(field.key, event.target.value) });
      const effects = (state.meta && state.meta.effects) || FALLBACK_EFFECTS;
      effects.forEach((fx) => input.append(el('option', { value: fx.id, text: fx.id + ' — ' + fx.label })));
      input.value = value;
      break;
    }
    default:
      input = el('input', {
        type: 'text', value: value == null ? '' : value,
        oninput: (event) => update(field.key, event.target.value),
      });
  }
  wrapper.append(input);
  return wrapper;
}

function update(key, value) {
  const def = currentDef();
  if (!def) return;
  def[key] = value;
  markDirty();
  renderPreview();
  if (key === 'name' || key === 'subtitle') $('#wheel-title').textContent = def.id;
}

// --- rewards ---

function renderRewards(panel, def) {
  const list = el('div', { class: 'reward-list', id: 'reward-list' });
  renderRewardRows(list, def);
  panel.append(list);
  panel.append(el('button', { class: 'primary', text: '+ Add Reward', onclick: addReward }));

  if (state.rewardEdit >= 0 && state.rewardEdit < def.rewards.length) {
    panel.append(renderRewardEditor(def.rewards[state.rewardEdit], state.rewardEdit));
  }
  renderDatalist();
}

function enabledRewards(def) {
  return (def.rewards || []).filter((reward) => reward.enabled !== false);
}

function weightTotal(rewards) {
  return rewards.reduce((sum, reward) => sum + Math.max(0, Number(reward.weight) || 0), 0);
}

function rewardPercent(reward, total) {
  return total > 0 ? (Math.max(0, Number(reward.weight) || 0) / total) * 100 : 0;
}

function renderRewardRows(list, def) {
  const rewards = def.rewards || (def.rewards = []);
  const total = weightTotal(enabledRewards(def));
  list.replaceChildren();
  rewards.forEach((reward, index) => {
    const on = reward.enabled !== false;
    const percent = on ? rewardPercent(reward, total) : 0;
    const row = el('div', { class: 'reward-row' + (on ? '' : ' off') },
      el('div', { class: 'swatch', style: 'background:' + normalizeHex(reward.color) }),
      el('div', { class: 'name' }, stripTags(resolveChance(reward.name, percent)) || '(unnamed)',
        el('span', { class: 'dim', text: '  ' + (reward.icon || '') }),
        reward.commands && reward.commands.length ? el('span', { class: 'dim', text: '  · ' + reward.commands.length + ' commands' }) : null,
        reward.permission ? el('span', { class: 'dim', text: '  🔒' }) : null,
        on ? null : el('span', { class: 'dim', text: '  OFF' })),
      el('div', { class: 'pct', text: percent.toFixed(1) + '%  (' + reward.weight + ')' }),
      el('div', { class: 'dim', text: normalizeHex(reward.color).toUpperCase() }),
      el('div', { class: 'ops' },
        el('button', { class: 'small', text: '▲', title: 'Move up', onclick: () => moveReward(index, -1) }),
        el('button', { class: 'small', text: '▼', title: 'Move down', onclick: () => moveReward(index, 1) }),
        el('button', { class: 'small', text: '⧉', title: 'Duplicate', onclick: () => duplicateReward(index) }),
        el('button', { class: 'small', text: '✎', title: 'Edit', onclick: () => { state.rewardEdit = index; renderPanel(); } }),
        el('button', { class: 'small danger', text: '✕', title: 'Delete', onclick: () => removeReward(index) }),
      ),
    );
    list.append(row);
  });
  if (!rewards.length) list.append(el('p', { class: 'dim', text: 'No rewards yet. Add one below.' }));
}

function renderRewardEditor(reward, index) {
  const editor = el('div', { class: 'reward-editor' });
  editor.append(el('h3', { text: 'Editing reward #' + (index + 1) }));

  editor.append(el('h4', { text: 'Properties' }));
  editor.append(el('div', { class: 'field' }, el('label', { text: 'Display name (MiniMessage, <chance> supported)' }),
    el('input', { type: 'text', value: reward.name || '', oninput: (event) => { reward.name = event.target.value; afterRewardChange(); } })));

  const weightMax = Math.max(100, Math.ceil(Number(reward.weight) || 0));
  const weightRange = el('input', {
    type: 'range', min: 0, max: weightMax, step: 1, value: reward.weight,
    oninput: () => { weightNumber.value = weightRange.value; reward.weight = Number(weightRange.value); afterRewardChange(); },
  });
  const weightNumber = el('input', {
    type: 'number', min: 0, step: 1, value: reward.weight,
    oninput: () => { weightRange.value = weightNumber.value; reward.weight = Number(weightNumber.value); afterRewardChange(); },
  });
  editor.append(el('div', { class: 'field' }, el('label', { text: 'Weight (chance — live % in the preview)' }),
    el('div', { class: 'row-inline' }, weightRange, weightNumber)));

  const color = el('input', {
    type: 'color', value: normalizeHex(reward.color),
    oninput: (event) => { hex.value = event.target.value.toUpperCase(); reward.color = event.target.value.toUpperCase(); afterRewardChange(); },
  });
  const hex = el('input', {
    type: 'text', value: normalizeHex(reward.color).toUpperCase(),
    oninput: (event) => { const value = event.target.value.trim(); if (/^#[0-9a-fA-F]{6}$/.test(value)) { color.value = value; reward.color = value.toUpperCase(); afterRewardChange(); } },
  });
  editor.append(el('div', { class: 'field' }, el('label', { text: 'Slice color' }),
    el('div', { class: 'row-inline' }, color, hex)));

  editor.append(el('div', { class: 'field' }, el('label', { text: 'Permission (blank = everyone)' }),
    el('input', { type: 'text', value: reward.permission || '', placeholder: 'e.g. vip.reward',
      oninput: (event) => { reward.permission = event.target.value.trim(); afterRewardChange(false); } })));

  editor.append(el('div', { class: 'field' }, el('label', { text: 'Win message (blank = default)' }),
    el('input', { type: 'text', value: reward.winMessage || '', placeholder: '<gold>You won <reward>!',
      oninput: (event) => { reward.winMessage = event.target.value; afterRewardChange(false); } })));

  editor.append(el('div', { class: 'field' }, el('label', { text: 'Win sound (blank = wheel default)' }),
    el('input', { type: 'text', value: reward.winSound || '', placeholder: 'minecraft:entity.player.levelup 1 1.1',
      oninput: (event) => { reward.winSound = event.target.value; afterRewardChange(false); } })));

  editor.append(el('h4', { text: 'Icon & State' }));
  editor.append(el('div', { class: 'field' }, el('label', { text: 'Material' }),
    el('input', {
      list: 'material-list', value: reward.icon || '', placeholder: 'e.g. GOLD_INGOT',
      oninput: (event) => { reward.icon = event.target.value.trim().toUpperCase(); afterRewardChange(); },
    })));

  const amount = el('input', {
    type: 'number', min: 1, max: 64, value: reward.amount || 1,
    oninput: () => { reward.amount = Math.max(1, Number(amount.value) || 1); afterRewardChange(); },
  });
  const cmd = el('input', {
    type: 'number', min: 0, max: 999999, value: reward.customModelData || 0,
    oninput: () => { reward.customModelData = Math.max(0, Number(cmd.value) || 0); afterRewardChange(false); },
  });
  editor.append(el('div', { class: 'field' }, el('label', { text: 'Amount' }), amount));
  editor.append(el('div', { class: 'field' }, el('label', { text: 'Custom model data (0 = none)' }), cmd));

  const boolRow = (label, get, set) => {
    const box = el('input', { type: 'checkbox', onchange: () => { set(box.checked); afterRewardChange(); } });
    box.checked = Boolean(get());
    return el('div', { class: 'field' }, el('label', { text: label }), box);
  };
  editor.append(boolRow('Give icon as item on win', () => reward.giveItem, (v) => { reward.giveItem = v; }));
  editor.append(boolRow('Glow', () => reward.glow, (v) => { reward.glow = v; }));
  editor.append(boolRow('Enabled', () => reward.enabled !== false, (v) => { reward.enabled = v; }));

  editor.append(el('div', { class: 'field' },
    el('div', { class: 'row-inline' },
      el('button', { text: '⧉ Pick from inventory', onclick: pickFromInventory }),
      el('button', { text: '✎ Use held item', onclick: useHeldItem }))));

  editor.append(el('h4', { text: 'Payout & Lore' }));
  const commands = el('textarea', { placeholder: 'one command per line — %player% %reward% %wheel%' });
  commands.value = (reward.commands || []).join('\n');
  commands.addEventListener('input', () => {
    reward.commands = commands.value.split('\n').map((line) => line.trim()).filter(Boolean);
    afterRewardChange(false);
  });
  editor.append(el('div', { class: 'field' }, el('label', { text: 'Console commands on win' }), commands));

  const lore = el('textarea', { placeholder: 'one lore line per line (MiniMessage)' });
  lore.value = (reward.lore || []).join('\n');
  lore.addEventListener('input', () => {
    reward.lore = lore.value.split('\n');
    afterRewardChange(false);
  });
  editor.append(el('div', { class: 'field' }, el('label', { text: 'Item lore (shown under the name)' }), lore));

  editor.append(el('h4', { text: 'Live preview' }));
  editor.append(el('div', { class: 'mini-box', id: 'reward-mini-preview' }));
  updateMiniPreview();

  editor.append(el('button', { text: 'Close', onclick: () => { state.rewardEdit = -1; renderPanel(); } }));
  return editor;
}

function updateMiniPreview() {
  const box = document.getElementById('reward-mini-preview');
  const def = currentDef();
  if (!box || !def || state.rewardEdit < 0 || state.rewardEdit >= def.rewards.length) return;
  const reward = def.rewards[state.rewardEdit];
  const total = weightTotal(enabledRewards(def));
  const on = reward.enabled !== false;
  const percent = on ? rewardPercent(reward, total) : 0;
  box.replaceChildren(
    el('div', { class: 'mini-icon', style: 'background:' + normalizeHex(reward.color) }, reward.icon || '?'),
    el('div', {},
      el('div', { class: 'dim small', text: 'THIS REWARD' }),
      el('div', { class: 'mini-name', text: stripTags(resolveChance(reward.name, percent)) || '(unnamed)' }),
      el('div', { class: 'dim small', text: percent.toFixed(2) + '% · x' + (reward.amount || 1) + ' · ' + (on ? 'ON' : 'OFF') }),
    ),
  );
}

function afterRewardChange(rerenderList = true) {
  markDirty();
  renderPreview();
  updateMiniPreview();
  if (rerenderList) {
    const list = document.getElementById('reward-list');
    if (list) renderRewardRows(list, currentDef());
  }
}

function addReward() {
  const def = currentDef();
  def.rewards.push({ name: '<white>New Reward', weight: 10, color: '#F2B544', icon: 'GOLD_INGOT',
    commands: [], permission: '', winMessage: '', winSound: '', amount: 1, giveItem: false,
    glow: false, enabled: true, lore: [], customModelData: 0 });
  state.rewardEdit = def.rewards.length - 1;
  markDirty();
  renderPanel();
  renderPreview();
}

function removeReward(index) {
  const def = currentDef();
  def.rewards.splice(index, 1);
  state.rewardEdit = -1;
  markDirty();
  renderPanel();
  renderPreview();
}

function duplicateReward(index) {
  const def = currentDef();
  const copy = JSON.parse(JSON.stringify(def.rewards[index]));
  def.rewards.splice(index + 1, 0, copy);
  state.rewardEdit = index + 1;
  markDirty();
  renderPanel();
  renderPreview();
}

function moveReward(index, direction) {
  const def = currentDef();
  const target = index + direction;
  if (target < 0 || target >= def.rewards.length) return;
  const [reward] = def.rewards.splice(index, 1);
  def.rewards.splice(target, 0, reward);
  if (state.rewardEdit === index) state.rewardEdit = target;
  markDirty();
  renderPanel();
  renderPreview();
}

async function pickFromInventory() {
  try {
    const data = await api('inventory');
    showInventoryModal(data.slots || []);
  } catch (error) {
    toast(error.message, true);
  }
}

function showInventoryModal(slots) {
  const grid = $('#modal-grid');
  grid.replaceChildren();
  $('#modal-title').textContent = 'Pick from inventory';
  if (!slots.length) grid.append(el('p', { class: 'dim', text: 'Inventory is empty (or you are offline in-game).' }));
  slots.forEach((slot) => {
    grid.append(el('button', {
      onclick: () => {
        const def = currentDef();
        const reward = def && def.rewards[state.rewardEdit];
        if (reward) {
          reward.icon = slot.icon;
          reward.amount = slot.amount;
          afterRewardChange();
          renderPanel();
        }
        hideModal();
      },
    }, el('span', { text: slot.icon }), el('span', { class: 'amt', text: 'x' + slot.amount })));
  });
  $('#modal').classList.remove('hidden');
}

function hideModal() {
  $('#modal').classList.add('hidden');
}

async function useHeldItem() {
  try {
    const data = await api('held');
    if (data.empty) {
      toast('Your hand is empty.', true);
      return;
    }
    const def = currentDef();
    const reward = def && def.rewards[state.rewardEdit];
    if (reward) {
      reward.icon = data.icon;
      reward.amount = data.amount;
      afterRewardChange();
      renderPanel();
      toast('Icon set from held item: ' + data.icon + ' x' + data.amount);
    }
  } catch (error) {
    toast(error.message, true);
  }
}

function renderDatalist() {
  let datalist = document.getElementById('material-list');
  if (!datalist) {
    datalist = el('datalist', { id: 'material-list' });
    document.body.append(datalist);
  }
  if (datalist.childElementCount !== state.materials.length) {
    datalist.replaceChildren();
    state.materials.forEach((material) => datalist.append(el('option', { value: material })));
  }
}

// --- preview ---

function normalizeHex(color) {
  const text = String(color || '#808080').replace('#', '');
  return /^[0-9a-fA-F]{6}$/.test(text) ? '#' + text : '#808080';
}

let spinAnim = null;

function renderPreview() {
  spinAnim = null;
  drawPreview(0, false);
  const result = $('#spin-result');
  if (result) result.textContent = '';
  const def = currentDef();
  const info = $('#preview-info');
  info.replaceChildren();
  if (!def) return;
  const rewards = def.rewards || [];
  const total = weightTotal(enabledRewards(def));
  info.append(el('div', { class: 'dim', text: rewards.length + ' rewards · total weight ' + round(total) }));
  rewards.forEach((reward) => {
    const on = reward.enabled !== false;
    const percent = on ? rewardPercent(reward, total) : 0;
    info.append(el('div', { class: 'pv-row' + (on ? '' : ' off') },
      el('div', { class: 'swatch', style: 'background:' + normalizeHex(reward.color) }),
      el('span', { text: stripTags(resolveChance(reward.name, percent)) || '(unnamed)' }),
      el('span', { class: 'pct', text: on ? percent.toFixed(1) + '%' : 'OFF' })));
  });
}

function drawPreview(rotation, spinning) {
  const def = currentDef();
  const canvas = $('#preview');
  if (!canvas) return;
  const ctx = canvas.getContext('2d');
  const width = canvas.width;
  const height = canvas.height;
  const cx = width / 2;
  const cy = height / 2;
  const radius = width / 2 - 46;
  ctx.clearRect(0, 0, width, height);

  const frame = normalizeHex(def && def.frameColor, '#D08A33');
  const hub = normalizeHex(def && def.hubColor, '#E2A44A');
  const pointer = normalizeHex(def && def.pointerColor, '#F2B544');
  const rewards = def ? enabledRewards(def) : [];

  ctx.beginPath();
  ctx.arc(cx, cy, radius + 5, 0, Math.PI * 2);
  ctx.fillStyle = frame;
  ctx.fill();
  ctx.lineWidth = 2;
  ctx.strokeStyle = '#0e0b16';
  ctx.stroke();

  if (!rewards.length) {
    ctx.beginPath();
    ctx.arc(cx, cy, radius, 0, Math.PI * 2);
    ctx.fillStyle = '#241a44';
    ctx.fill();
  } else {
    ctx.save();
    ctx.translate(cx, cy);
    ctx.rotate(rotation);
    const segment = (Math.PI * 2) / rewards.length;
    rewards.forEach((reward, index) => {
      ctx.beginPath();
      ctx.moveTo(0, 0);
      ctx.arc(0, 0, radius, -Math.PI / 2 + index * segment, -Math.PI / 2 + (index + 1) * segment);
      ctx.closePath();
      ctx.fillStyle = normalizeHex(reward.color);
      ctx.fill();
      ctx.lineWidth = 2;
      ctx.strokeStyle = '#0e0b16';
      ctx.stroke();
    });
    ctx.restore();
  }

  ctx.beginPath();
  ctx.arc(cx, cy, radius * 0.14, 0, Math.PI * 2);
  ctx.fillStyle = hub;
  ctx.fill();
  ctx.strokeStyle = '#0e0b16';
  ctx.lineWidth = 2;
  ctx.stroke();

  ctx.beginPath();
  ctx.moveTo(cx, cy - radius - 8);
  ctx.lineTo(cx - 10, cy - radius + 8);
  ctx.lineTo(cx + 10, cy - radius + 8);
  ctx.closePath();
  ctx.fillStyle = pointer;
  ctx.fill();
  ctx.strokeStyle = '#0e0b16';
  ctx.stroke();

  if (!spinning && rewards.length) {
    const segment = (Math.PI * 2) / rewards.length;
    const total = weightTotal(rewards);
    ctx.font = '10px sans-serif';
    ctx.textAlign = 'center';
    ctx.fillStyle = '#e8e3f5';
    ctx.strokeStyle = '#0e0b16';
    ctx.lineWidth = 3;
    rewards.forEach((reward, index) => {
      const mid = -Math.PI / 2 + (index + 0.5) * segment;
      const lx = cx + Math.cos(mid) * (radius + 24);
      const ly = cy + Math.sin(mid) * (radius + 24);
      const pct = rewardPercent(reward, total);
      const name = shortName(stripTags(resolveChance(reward.name, pct)));
      ctx.strokeText(name, lx, ly - 1);
      ctx.fillText(name, lx, ly - 1);
      ctx.strokeText(pct.toFixed(1) + '%', lx, ly + 10);
      ctx.fillText(pct.toFixed(1) + '%', lx, ly + 10);
    });
  }
}

function shortName(name) {
  return name.length > 14 ? name.slice(0, 13) + '…' : name;
}

function testSpin() {
  const def = currentDef();
  const rewards = def ? enabledRewards(def) : [];
  if (!rewards.length) {
    toast('No enabled rewards.', true);
    return;
  }
  const total = weightTotal(rewards);
  let picked = 0;
  if (total > 0) {
    let roll = Math.random() * total;
    for (let i = 0; i < rewards.length; i++) {
      roll -= Math.max(0, Number(rewards[i].weight) || 0);
      if (roll < 0) { picked = i; break; }
      picked = i;
    }
  } else {
    picked = Math.floor(Math.random() * rewards.length);
  }
  const segment = (Math.PI * 2) / rewards.length;
  const target = Math.PI * 2 * 6 + (((Math.PI * 2 - ((picked + 0.5) * segment)) % (Math.PI * 2)) + Math.PI * 2) % (Math.PI * 2);
  const duration = 3800;
  const startTime = performance.now();
  const mine = { id: Date.now() + Math.random() };
  spinAnim = mine;
  $('#spin-result').textContent = 'Spinning…';
  function frame(now) {
    if (spinAnim !== mine) return;
    const t = Math.min(1, (now - startTime) / duration);
    const eased = 1 - Math.pow(1 - t, 3);
    drawPreview(target * eased, true);
    if (t < 1) {
      requestAnimationFrame(frame);
    } else {
      spinAnim = null;
      const percent = rewardPercent(rewards[picked], total);
      $('#spin-result').textContent = stripTags(resolveChance(rewards[picked].name, percent)) + ' (' + percent.toFixed(2) + '%)';
      drawPreview(0, false);
    }
  }
  requestAnimationFrame(frame);
}

function round(value) {
  return Math.round(value * 100) / 100;
}

// --- save / delete / create ---

async function save() {
  const def = currentDef();
  if (!def) return;
  const button = $('#save');
  button.disabled = true;
  setStatus('saving…', 'dirty');
  try {
    await api('wheels/' + encodeURIComponent(def.id), { method: 'PUT', body: JSON.stringify(def) });
    state.dirty = false;
    setStatus('saved', 'ok');
    toast('Saved and applied in-game.');
  } catch (error) {
    setStatus('error', 'err');
    toast(error.message, true);
  } finally {
    button.disabled = false;
  }
}

async function removeWheel() {
  const def = currentDef();
  if (!def) return;
  if (state.dirty && !confirm('You have unsaved changes. Deleting will also discard them. Continue?')) return;
  if (!confirm('"' + def.id + '" — delete this wheel? This cannot be undone.')) return;
  const button = $('#delete');
  button.disabled = true;
  try {
    await api('wheels/' + encodeURIComponent(def.id), { method: 'DELETE' });
    state.current = null;
    state.dirty = false;
    await loadWheels();
    toast('Wheel deleted.');
  } catch (error) {
    toast(error.message, true);
  } finally {
    button.disabled = false;
  }
}

async function createWheel(event) {
  event.preventDefault();
  if (state.dirty && !confirm('You have unsaved changes. Creating a new wheel will discard them. Continue?')) return;
  const input = $('#create-id');
  const id = input.value.trim().toLowerCase();
  if (!id) return;
  const button = $('#create-form button');
  button.disabled = true;
  try {
    await api('wheels', { method: 'POST', body: JSON.stringify({ id }) });
    input.value = '';
    state.current = id;
    state.dirty = false;
    await loadWheels();
    toast('Wheel created. Set its location in-game with /wheel move ' + id + '.');
  } catch (error) {
    toast(error.message, true);
  } finally {
    button.disabled = false;
  }
}

// --- events ---

$('#auth-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  const token = $('#auth-token').value.trim();
  if (!token) return;
  try {
    await authenticate(token);
    state.meta = await api('meta');
    state.materials = await api('materials');
    await loadWheels();
    showApp();
  } catch (error) {
    $('#auth-message').textContent = error.message;
  }
});

$('#save').addEventListener('click', save);
$('#delete').addEventListener('click', removeWheel);
$('#logout').addEventListener('click', logout);
$('#create-form').addEventListener('submit', createWheel);
$('#test-spin').addEventListener('click', testSpin);
$('#modal-close').addEventListener('click', hideModal);
$('#modal').addEventListener('click', (event) => {
  if (event.target.id === 'modal') hideModal();
});

window.addEventListener('beforeunload', (event) => {
  if (state.dirty) {
    event.preventDefault();
    event.returnValue = '';
  }
});

boot();
