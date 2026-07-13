# Stat Tracker / 统计追踪器

A team contribution tracker for **Together in Spire** (Slay the Spire multiplayer mod).
Tracks per-player damage, debuff/buff attribution, and team support stats, with an
in-game draggable overlay panel.

为 **Together in Spire**（杀戮尖塔联机mod）设计的团队贡献统计mod，实时追踪每位玩家的
伤害、debuff/buff 归因与团队支援数据，附带可拖拽的游戏内面板。

Steam Workshop: https://steamcommunity.com/sharedfiles/filedetails/?id=3721416376

## Features / 功能

- **Per-player damage tracking** — each player's direct damage in combat
- **Debuff attribution** — Vulnerable/Poison bonus damage attributed to the applier
- **Team contribution** — shields and buffs given to teammates
- **Damage percentage** — each player's share of total team damage
- **4 views** — Current Combat / Run Stats / Buff Stats / Event Log
- **Draggable panel** — click the title to reposition

## Controls / 快捷键

| Key | Action |
|-----|--------|
| F5  | Toggle panel on/off |
| F4  | Switch views |

## Requirements / 依赖

- [ModTheSpire](https://steamcommunity.com/workshop/filedetails/?id=1605060445)
- [BaseMod](https://steamcommunity.com/workshop/filedetails/?id=1605833019)
- [Together in Spire](https://steamcommunity.com/sharedfiles/filedetails/?id=2384072973)

## Building / 构建

The mod targets Java 8. Build with `build.sh` (uses the JDK and dependency jars
referenced inside the script):

```bash
bash build.sh
```

Dependencies (game core + workshop mods) are referenced by absolute path in
`build.gradle` / `build.sh`; adjust the paths if your install is elsewhere.
See [AGENTS.md](AGENTS.md) for the full build & release workflow.

## How damage attribution works

Damage is captured by patching `AbstractMonster.damage` / `AbstractPlayer.damage`
(see `patches/DamagePatch.java`). Attribution depends on the `DamageInfo`:

- `NORMAL` — attributed to the attacking creature (player via P2P identity in MP).
- `THORNS` with a non-null owner — Lightning orbs, Thorns, Flame Barrier, Panache,
  Thousand Cuts: attributed to the owner.
- `THORNS` with **null owner** — relics/powers that call
  `DamageAllEnemiesAction(null, matrix, THORNS, ...)`: Charon's Ashes, Letter Opener,
  Mercury Hourglass, Stone Calendar, Combust, Fire Breathing, Omega. Attributed to
  the local player via `resolveBurnPlayer()` (which checks for the owning relic/power).
- `HP_LOSS` — poison, choke, etc.
- Monster-debuff-sourced (Corpse Explosion, The Bomb) — attributed to the player who
  applied the debuff via `DebuffTracker`.

In multiplayer, each client computes its **own** damage locally (including its own
relic damage) and broadcasts combat/turn totals to teammates via TIS `ExtraData`
(`st_dmg`). Remote totals replace (not accumulate) the local estimate, so there is
no double counting.

## License / 许可

MIT. See [LICENSE](LICENSE).
