# Stat Tracker / 统计追踪器

为 **Together in Spire**(杀戮尖塔联机 mod)设计的团队贡献统计工具。实时追踪每位玩家的伤害、debuff/buff 归因与团队支援数据,附带可拖拽的游戏内面板。

A team contribution tracker for **Together in Spire** (Slay the Spire multiplayer mod). Tracks per-player damage, debuff/buff attribution, and team support stats, with an in-game draggable overlay panel.

Steam 创意工坊 / Steam Workshop: https://steamcommunity.com/sharedfiles/filedetails/?id=3721416376

## 功能 / Features

- **单人/多人伤害追踪** — 实时追踪每个玩家在战斗中造成的直接伤害
- **Debuff 伤害归因** — 易伤/中毒等 debuff 带来的额外伤害归属于施放者
- **团队贡献统计** — 追踪给队友提供的护盾和增益
- **伤害占比** — 显示每个玩家在团队总伤害中的百分比
- **4 个视图切换** — 当前战斗 / 整局统计 / Buff 统计 / 战斗日志
- **可拖拽面板** — 点击标题区拖动调整位置

- **Per-player damage tracking** — each player's direct damage in combat
- **Debuff attribution** — Vulnerable/Poison bonus damage attributed to the applier
- **Team contribution** — shields and buffs given to teammates
- **Damage percentage** — each player's share of total team damage
- **4 views** — Current Combat / Run Stats / Buff Stats / Event Log
- **Draggable panel** — click the title to reposition

## 快捷键 / Controls

| 键 / Key | 功能 / Action |
|----------|---------------|
| F5 | 显示/隐藏面板 / Toggle panel on/off |
| F4 | 切换视图 / Switch views |

## 依赖 / Requirements

- [ModTheSpire](https://steamcommunity.com/workshop/filedetails/?id=1605060445)
- [BaseMod](https://steamcommunity.com/workshop/filedetails/?id=1605833019)
- [Together in Spire](https://steamcommunity.com/sharedfiles/filedetails/?id=2384072973)

## 构建 / Building

本 mod 目标为 Java 8。用 `build.sh` 构建(脚本内引用了 JDK 与依赖 jar 路径):

The mod targets Java 8. Build with `build.sh` (uses the JDK and dependency jars referenced inside the script):

```bash
bash build.sh
```

游戏本体核心与创意工坊依赖 jar 在 `build.gradle` / `build.sh` 中以绝对路径引用;若你的安装路径不同,请相应修改。完整的构建与发版流程见 `AGENTS.md`。

Dependencies (game core + workshop mods) are referenced by absolute path in `build.gradle` / `build.sh`; adjust the paths if your install is elsewhere. See `AGENTS.md` for the full build & release workflow.

## 伤害归因机制 / How damage attribution works

通过 patch `AbstractMonster.damage` / `AbstractPlayer.damage` 捕获伤害(见 `patches/DamagePatch.java`)。归因取决于 `DamageInfo`:

- `NORMAL` — 归属攻击者(联机中通过 P2P 身份识别玩家)。
- `THORNS`(owner 非空)— 闪电球、荆棘、火焰屏障、Panache(花式)、Thousand Cuts(千刀万剐):归属 owner。
- `THORNS`(**owner 为 null**)— 调用 `DamageAllEnemiesAction(null, matrix, THORNS, ...)` 的遗物/能力:冥河之烬、开信刀、水银沙漏、石历、自燃、火焰吐息、欧米茄。通过 `resolveBurnPlayer()` 归属本地玩家(检查其是否持有对应遗物/能力)。
- `HP_LOSS` — 中毒、扼喉等。
- 怪物 debuff 来源(尸爆、炸弹)— 通过 `DebuffTracker` 归属施放该 debuff 的玩家。

Damage is captured by patching `AbstractMonster.damage` / `AbstractPlayer.damage` (see `patches/DamagePatch.java`). Attribution depends on the `DamageInfo`:

- `NORMAL` — attributed to the attacking creature (player via P2P identity in MP).
- `THORNS` with a non-null owner — Lightning orbs, Thorns, Flame Barrier, Panache, Thousand Cuts: attributed to the owner.
- `THORNS` with **null owner** — relics/powers that call `DamageAllEnemiesAction(null, matrix, THORNS, ...)`: Charon's Ashes, Letter Opener, Mercury Hourglass, Stone Calendar, Combust, Fire Breathing, Omega. Attributed to the local player via `resolveBurnPlayer()` (which checks for the owning relic/power).
- `HP_LOSS` — poison, choke, etc.
- Monster-debuff-sourced (Corpse Explosion, The Bomb) — attributed to the player who applied the debuff via `DebuffTracker`.

联机下,每个客户端只计算**自己**造成的伤害(含自己的遗物伤害),再通过 TIS `ExtraData`(`st_dmg`)把战斗/回合总量广播给队友。远端总量是**覆盖**而非累加本地估算,因此不会重复计数。

In multiplayer, each client computes its **own** damage locally (including its own relic damage) and broadcasts combat/turn totals to teammates via TIS `ExtraData` (`st_dmg`). Remote totals replace (not accumulate) the local estimate, so there is no double counting.

## 许可 / License

MIT,见 [LICENSE](LICENSE)。
MIT, see [LICENSE](LICENSE).
