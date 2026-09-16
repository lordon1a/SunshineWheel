# Changelog

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
