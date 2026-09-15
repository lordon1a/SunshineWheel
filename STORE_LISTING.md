# SunshineWheel — BuiltByBit Store Listing (draft)

> Paste into the resource page. Replace [bracketed] parts before publishing.
> Screenshots/video: add 4–6 images (editor sidebar, live preview, reward editor,
> in-game wheel spinning, win celebration) + optional 60–90s demo video.

---

**SunshineWheel** — physical, animated fortune wheels your players can walk up to
and spin. No client mods. Configure everything from your browser with live preview.

## Why SunshineWheel

- 🎡 **Real in-world wheels** — spinning display-entity wheels with easing styles,
  tick sounds and buttery motion, not a chest GUI
- 🖥️ **Web editor** — full configuration from the browser: rewards, costs, effects,
  appearance, live canvas preview with test spins; changes apply instantly in-game
- 🎁 **Flexible rewards** — MiniMessage names with live `<chance>`, weights, item
  payout (amount, lore, glow, custom model data), console commands, per-reward
  permission, custom win message/sound, enable/disable
- 💰 **Fair costs** — ticket items, Vault money (balance-checked, never half-charged),
  XP levels, extra console commands, free spins, automatic refunds on interrupted spins
- 🎉 **Celebrations** — particle bursts, fireworks, rockets, spirals, ambient spin
  particles, optional server-wide win broadcast
- 👁️ **Player preview** — sneak + right-click shows rewards with live chances
- 🔧 **Admin tools** — free-spin grants, ticket give/take, stats, cooldown reset,
  per-wheel permission and on/off switch, daily/premium/vote sample wheels included
- 🔌 **Integrations** — PlaceholderAPI, Vault, cancellable developer events
- 🌍 **Languages** — English + Turkish out of the box (`messages.yml`)
- 🪶 **Lightweight** — Paper 1.21+, Java 21+, no dependencies required

## Requirements

- Paper (or fork) **1.21+**, Java **21+**
- Optional: Vault + economy (money costs), PlaceholderAPI (placeholders)

## Setup (5 minutes)

Step-by-step guide with screenshots: **https://rehber.guldal.me/wheel-setup.html**

1. Drop `SunshineWheel-1.0.0.jar` into `plugins/` and restart.
   Three sample wheels (`daily`, `premium`, `vote`) are created — place one with
   `/wheel move <id>`, then right-click a block.
2. **Resource pack (required, tiny):** on startup the plugin builds `pack.zip`
   and serves it. Copy the URL + sha1 from the console into `server.properties`
   as `resource-pack=` / `resource-pack-sha1=`, then restart.
   Prefer merging? Copy `assets/sunshinewheel` from that zip into your own pack.
3. Run `/wheel web` in-game and open the link — configure everything in your browser.

To host the web editor publicly, proxy the built-in API over HTTPS (example for nginx
is in the README) and set `web.public-url` in `config.yml`.

## Player guide

- **Right-click** a wheel to spin (tickets/money/XP are taken automatically).
- **Sneak + right-click** to preview rewards with live chances.
- Placeholders for scoreboards/menus:
  `%sunshinewheel_<id>_spins%`, `_wins`, `_free`, `_cooldown%`.

## Admin commands (`/wheel`, permission `sunshinewheel.admin`)

`web` · `create` · `move` · `remove` · `list` · `spin` · `preview` · `set` · `tune` ·
`angle` · `reload` · `give` · `take` · `ticket` · `stats` · `resetcooldown`

(Full reference with examples: README.md inside the download.)

## FAQ

**Players see boxes instead of the wheel?**
The resource pack isn't installed. Follow step 2 above — the pack is ~2 KB.

**"Made for an older version" pack warning?**
Harmless. The font works on all 1.21+ versions.

**Does it work behind Velocity/BungeeCord?**
Yes. Serve the pack over HTTP(S) and set it in `server.properties` (or your
proxy's pack manager); install the plugin on the backend Paper server.

**My server isn't English?**
In-game messages live in `messages.yml` — translate them freely; Turkish is included.

## Support

[Discord invite link] — support + suggestions. Updates and changelog with every release.

## License

All rights reserved — one purchase per network. No redistribution or resale.
