# Stat Tracker

A team contribution tracker for **Together in Spire** (Slay the Spire multiplayer mod). Tracks per-player damage, debuff/buff attribution, and team support stats, with an in-game draggable overlay panel.

Steam Workshop: https://steamcommunity.com/sharedfiles/filedetails/?id=3721416376

中文文档见 [README.md](README.md)。

## Features

- Per-player damage tracking — each player's direct damage in combat
- Debuff attribution — Vulnerable/Poison bonus damage attributed to the applier
- Team contribution — shields and buffs given to teammates
- Damage percentage — each player's share of total team damage
- 4 views — Current Combat / Run Stats / Buff Stats / Event Log
- Draggable panel — click the title to reposition

## Controls

| Key | Action |
|-----|--------|
| F5  | Toggle panel on/off |
| F4  | Switch views |

## Requirements

- [ModTheSpire](https://steamcommunity.com/workshop/filedetails/?id=1605060445)
- [BaseMod](https://steamcommunity.com/workshop/filedetails/?id=1605833019)
- [Together in Spire](https://steamcommunity.com/sharedfiles/filedetails/?id=2384072973)

## Building

The mod targets Java 8. Local paths (JDK8, game dir, workshop dir) live in `.env`, which is not committed. Copy the template before your first build:

```bash
cp .env.example .env
# then edit .env with your own paths
```

Then build:

```bash
bash build.sh
```

It works without `.env` too — the script falls back to a few common default paths, but compilation fails if they don't match. `AGENTS.md` (not committed; kept locally) documents the full build and release workflow.

## How damage attribution works

Damage is captured by patching `AbstractMonster.damage` / `AbstractPlayer.damage` (see `patches/DamagePatch.java`). Attribution depends on the `DamageInfo`:

- `NORMAL` — attributed to the attacking creature (the player, via P2P identity in MP).
- `THORNS` with a non-null owner — Lightning orbs, Thorns, Flame Barrier, Panache, Thousand Cuts: attributed to the owner.
- `THORNS` with a null owner — relics/powers that call `DamageAllEnemiesAction(null, matrix, THORNS, ...)`: Charon's Ashes, Letter Opener, Mercury Hourglass, Stone Calendar, Combust, Fire Breathing, Omega. Attributed to the local player through `resolveBurnPlayer()`, which checks whether that player holds the relic/power.
- `HP_LOSS` — poison, choke, and similar.
- Monster-debuff-sourced damage (Corpse Explosion, The Bomb) — attributed through `DebuffTracker` to whoever applied the debuff.

In multiplayer, each client only computes its own damage (including its own relic damage), then broadcasts combat and turn totals to teammates via TIS `ExtraData` (`st_dmg`). Remote totals overwrite rather than add to the local estimate, so nothing gets counted twice.

## License

MIT, see [LICENSE](LICENSE).
