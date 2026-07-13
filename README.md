# Stat Tracker / 统计追踪器

适用于 **Together in Spire** 的《杀戮尖塔》团队贡献统计 mod。它会记录战斗中的玩家伤害、debuff 伤害归因，以及给队友提供的护盾和增益。

Steam 创意工坊：<https://steamcommunity.com/sharedfiles/filedetails/?id=3721416376>

英文文档：<https://github.com/wzacolemak/StatTracker/blob/master/README-EN.md>

## 功能

- 记录每位玩家在当前战斗和整局游戏中的伤害
- 将易伤、中毒等 debuff 产生的额外伤害归给施放者
- 记录给队友提供的护盾和增益
- 显示每位玩家对团队总伤害的贡献比例
- 提供当前战斗、整局统计、增益统计和战斗日志四种视图
- 支持拖动统计面板

## 快捷键

| 按键 | 功能 |
| --- | --- |
| F5 | 显示或隐藏统计面板 |
| F4 | 切换视图 |

## 依赖

- [ModTheSpire](https://steamcommunity.com/workshop/filedetails/?id=1605060445)
- [BaseMod](https://steamcommunity.com/workshop/filedetails/?id=1605833019)
- [Together in Spire](https://steamcommunity.com/sharedfiles/filedetails/?id=2384072973)

## 构建

项目使用 Java 8 编译。构建前，复制配置模板并填写本机路径：

```bash
cp .env.example .env
```

`.env` 中需要配置以下路径：

- `STS_JDK8`：JDK 8 根目录
- `STS_GAME`：《杀戮尖塔》安装目录
- `STS_WORKSHOP`：Steam 创意工坊 mod 文件目录

然后运行：

```bash
bash build.sh
```

`.env` 仅用于本地构建，不应提交到版本库。`.env.example` 是可提交的配置模板。项目也支持使用 Gradle 构建：

```bash
./gradlew build
```

## 伤害归因

伤害统计通过 `DamagePatch` 拦截游戏中的伤害事件，并根据 `DamageInfo` 归因：

- `NORMAL`：归给攻击者；联机模式下通过 Together in Spire 的玩家身份识别攻击者。
- 带有 owner 的 `THORNS`：归给伤害来源的 owner，例如闪电球、荆棘、火焰屏障、花式和千刀万剐。
- owner 为空的 `THORNS`：处理冥河之烬、开信刀、水银沙漏、石历、自燃、火焰吐息和欧米茄等来源。
- `HP_LOSS`：处理毒、扼喉等不属于普通攻击的生命值损失。
- 怪物身上的尸爆和炸弹等 debuff：归给施加该 debuff 的玩家。

联机模式下，每个客户端计算本地玩家的伤害，再通过 Together in Spire 的 ExtraData 同步战斗和回合统计。远程统计会覆盖本地缓存，不会重复累加。

## 开发说明

游戏本体和 mod 依赖不包含在仓库中。构建时会从 `.env` 指定的路径读取 `desktop-1.0.jar`、ModTheSpire、BaseMod、StSLib 和 Together in Spire。

## 许可证

本项目使用 MIT 许可证，详见 [LICENSE](LICENSE)。
