# Stat Tracker / 统计追踪器

为 **Together in Spire**(杀戮尖塔联机 mod)做的团队贡献统计工具。实时追踪每位玩家的伤害、debuff/buff 归因和团队支援数据,带一个可拖拽的游戏内面板。

Steam 创意工坊:https://steamcommunity.com/sharedfiles/filedetails/?id=3721416376

English Document: [README-EN.md](README-EN.md)

## 功能

- 单人/多人伤害追踪 — 实时追踪每个玩家在战斗中造成的直接伤害
- Debuff 伤害归因 — 易伤、中毒等 debuff 带来的额外伤害算到施放者头上
- 团队贡献 — 追踪给队友的护盾和增益
- 伤害占比 — 显示每个玩家占团队总伤害的百分比
- 4 个视图 — 当前战斗 / 整局统计 / Buff 统计 / 战斗日志
- 可拖拽面板 — 点标题区拖动调整位置

## 快捷键

| 键 | 功能 |
|----|------|
| F5 | 显示/隐藏面板 |
| F4 | 切换视图 |

## 依赖

- [ModTheSpire](https://steamcommunity.com/workshop/filedetails/?id=1605060445)
- [BaseMod](https://steamcommunity.com/workshop/filedetails/?id=1605833019)
- [Together in Spire](https://steamcommunity.com/sharedfiles/filedetails/?id=2384072973)

## 构建

mod 目标是 Java 8,用 `build.sh` 构建(脚本里写死了 JDK 和依赖 jar 的路径):

```bash
bash build.sh
```

游戏本体和创意工坊依赖 jar 在 `build.gradle` / `build.sh` 里都是绝对路径。你的安装路径不一样就自己改一下。完整的构建和发版流程在 `AGENTS.md`(没提交到仓库,只留在本地)。

## 伤害归因机制

通过 patch `AbstractMonster.damage` / `AbstractPlayer.damage` 捕获伤害(见 `patches/DamagePatch.java`)。怎么算取决于 `DamageInfo`:

- `NORMAL` — 算攻击者的(联机里靠 P2P 身份认人)。
- `THORNS`,owner 非空 — 闪电球、荆棘、火焰屏障、Panache(花式)、Thousand Cuts(千刀万剐):算 owner。
- `THORNS`,owner 为 null — 调 `DamageAllEnemiesAction(null, matrix, THORNS, ...)` 的遗物/能力:冥河之烬、开信刀、水银沙漏、石历、自燃、火焰吐息、欧米茄。通过 `resolveBurnPlayer()` 算到本地玩家头上(检查他有没有对应遗物/能力)。
- `HP_LOSS` — 中毒、扼喉之类。
- 怪物 debuff 来源(尸爆、炸弹)— 通过 `DebuffTracker` 算到放 debuff 的那个人头上。

联机里每个客户端只算自己造成的伤害(包括自己的遗物伤害),再把战斗和回合总量通过 TIS `ExtraData`(`st_dmg`)广播给队友。远端的总量是覆盖本地估算,不是累加,所以不会重复计数。

## 许可

MIT,见 [LICENSE](LICENSE)。
