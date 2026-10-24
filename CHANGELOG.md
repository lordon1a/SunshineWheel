# Changelog

## 1.2.0 — 2026-09-16

- Chance menu: players **left-click a wheel** to open a chest-style GUI with every
  reward, its chance percentage, weight, permission lock and payout summary
  (paginated; `menu.enabled` in `config.yml`; `/wheel menu <id>` for admins)
- Slice sizes now follow the chances: wheel segments are drawn proportional to
  reward weights (toggle per wheel with `appearance.weighted-slices`, editor
  checkbox "Slice sizes follow the chance"); the landing math, floating item
  positions and the web preview match the same boundaries
- Custom reward items: every reward can now pay out
  - a **captured item copy** (Base64/NBT — any custom item: Oraxen, ItemsAdder,
    heads, enchanted gear; capture from the web editor with "Use held item" /
    "Pick from inventory", or in-game with `/wheel setitem <wheel> <n>`), or
  - a **plugin item id**: Oraxen, ItemsAdder, MMOItems, Nexo, ExecutableItems
    (`/wheel setitem <wheel> <n> oraxen:ruby`; ids are listed in the web editor)
- Payout safety: new rewards default to "give the item on win"; rewards without
  any payout are flagged in the editor ("NO PAYOUT" + save warning) and logged
  to the console when won
- `/wheel setitem <wheel> <reward> [provider:id]` added
- Web editor: item source selector, captured item info, provider id suggestions
  (`/api/customitems`), weighted preview canvas
- `/wheel set <id> weighted-slices <true|false>` quick toggle added

## 1.1.0 — 2026-09-16

- Built-in update checker: admins are notified in-game when a new version is out
  (async, every 6 hours, can be disabled in `config.yml`)
- bStats support (opt-out as usual via `plugins/bStats/config.yml`)
- Resource pack format is now detected from the running server version, so the
  "made for an older version" warning is gone on modern servers
  (`pack.format: 0` in the config; set a number to override)
- Discord support link added to the setup guide and store page
- `/wheel web` output is friendlier: shows the exact address + paste-in key and
  links the setup guide
- Language files now merge with bundled defaults, so future updates can add new
  messages without breaking existing translations

## 1.0.0 — 2026-09-15

First public release.

- Physical animated fortune wheels (display entities, no client mods)
- Rewards: MiniMessage names with `<chance>`, weights, per-reward permission /
  custom win message / win sound, item payout (amount, lore, glow, custom model
  data), console commands, enable/disable
- Costs: ticket items, Vault money (balance-checked), XP levels, extra console
  commands, free spins per player, automatic refunds on interrupted spins
- Win effects: particle burst, firework, rocket, spiral + ambient spin particles
- Appearance: size plus frame / hub / pointer colors, hologram title
- Player reward preview (sneak + right-click, `/wheel preview`)
- Admin: ticket give/take, stats, cooldown reset, per-wheel permission and
  on/off switch, win broadcast, sample wheels (daily / premium / vote)
- Web editor with live preview and test spins; changes apply instantly
- PlaceholderAPI placeholders, cancellable developer events
- English + Turkish messages (`messages.yml`, `language:` in config)
- Built-in resource-pack server (no pack merging required)
- Paper 1.21+, Java 21+
