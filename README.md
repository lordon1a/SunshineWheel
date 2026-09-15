# SunshineWheel

A fortune-wheel plugin for Paper servers with a **web editor** — no client mods needed.
Players walk up to a physical, animated wheel in the world, right-click to spin, and win prizes.

## Features

- **Physical in-world wheels** — animated spins with easing styles, tick sounds and smooth motion
- **Rewards** — names (MiniMessage + `<chance>`), weights with live percentages, per-reward
  permission / custom win message / win sound, item payout (amount, lore, glow, custom model data),
  console commands (`%player% %reward% %wheel%`), enable/disable
- **Costs** — ticket items, Vault money (balance-checked, no partial charges), XP levels,
  extra console commands; free spins per player
- **Win effects** — particle burst, firework, rocket, spiral + ambient particles while spinning
- **Appearance** — size plus frame / hub / pointer colors, hologram title
- **Player preview** — sneak + right-click (or `/wheel preview <id>`) lists rewards with chances
- **Admin tools** — `/wheel give|take`, `/wheel ticket give|take`, `/wheel stats`,
  `/wheel resetcooldown`, per-wheel permission and on/off switch, win broadcast
- **Web editor** (`/wheel web`) — full configuration from the browser with live canvas preview
  and test spins; changes apply instantly in-game
- **Integrations** — PlaceholderAPI (`%sunshinewheel_<id>_spins|wins|free|cooldown%`),
  developer events (`WheelSpinStartEvent`, `WheelSpinWinEvent`, cancellable), Vault
- **Safety** — spins blocked while a wheel is spinning, payments refunded if a spin is
  interrupted, cooldowns survive restarts

## Requirements

- Paper (or fork) 1.21+, Java 21+
- Optional: Vault + an economy plugin (money costs), PlaceholderAPI (placeholders)

## Install

1. Drop `SunshineWheel-1.0.0.jar` into `plugins/` and restart.
2. Three sample wheels (`daily`, `premium`, `vote`) are created automatically —
   place one with `/wheel move <id>`, then right-click a block.
   Or create your own: `/wheel create <id>`, then right-click a block.
3. Run `/wheel web` and open the link to configure everything from your browser.

## Language

In-game messages come from `messages.yml` (English, default). Set `language: tr`
in `config.yml` for Turkish (`messages_tr.yml`). The web editor is English.

## Commands (`/wheel`, alias `/cark`, permission `sunshinewheel.admin`)

| Command | What it does |
|---|---|
| `web` | One-time link for the web editor (10 min, single use) |
| `create <id>` / `move <id>` / `cancel` | Place or move a wheel by right-clicking a block |
| `remove <id>` / `list` / `reload` | Delete, list, reload from disk |
| `spin <id>` | Test spin |
| `preview <id>` | Show rewards with chances (players: sneak + right-click the wheel) |
| `set <id> <key> <value>` | Quick tweak (style, duration, revolutions, pixel-size, image-size, upright, item-scale) |
| `tune <key> <value>` | Text calibration without restart |
| `angle <id> <degrees>` | Debug which slice is under the pointer |
| `give|take <player> <wheel> <n>` | Grant/revoke free spins |
| `ticket <give|take> <player> <wheel> <n>` | Give/take ticket items |
| `stats [player]` | Spins, wins and free spins per wheel |
| `resetcooldown <player> <wheel>` | Clear a player's cooldown |

## Web editor hosting

The plugin serves a JSON API on `127.0.0.1:8770` (see `web:` in `config.yml`).
Proxy it over HTTPS, e.g. nginx:

```nginx
location /wheel/api/ {
    limit_req zone=wheel_api burst=20 nodelay;
    proxy_pass http://127.0.0.1:8770/api/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

Serve the `web/` folder as static files at `/wheel/` (no restart needed for UI updates).

## Resource pack

The wheel is drawn with a tiny bundled font. The plugin builds `pack.zip`
(`plugins/SunshineWheel/pack.zip`) on startup and serves it — no merging needed:

1. Copy the URL and sha1 from the console (`Resource pack ready: ...`).
2. Put them in `server.properties` as `resource-pack=` and `resource-pack-sha1=`.
3. Restart. Done.

Prefer merging? Download the same `pack.zip` and copy `assets/sunshinewheel`
into your own pack instead. See `pack:` in `config.yml` to change the port
or disable the built-in server.

## Build

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-26.0.2"
.\gradlew.bat build
```

## License

All rights reserved. See LICENSE.

## Changelog

See CHANGELOG.md.
