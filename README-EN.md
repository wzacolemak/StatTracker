# Stat Tracker

A team contribution tracker for **Together in Spire**, the Slay the Spire multiplayer mod. It records player damage, debuff attribution, and shields or buffs given to teammates.

Steam Workshop: <https://steamcommunity.com/sharedfiles/filedetails/?id=3721416376>

中文文档：[README.md](README.md)

## Features

- Track each player's damage for the current combat and the full run
- Attribute bonus damage from Vulnerable, Poison, and similar debuffs to the player who applied them
- Record block and buffs given to teammates
- Show each player's share of total team damage
- Switch between combat, run, buff, and event-log views
- Drag the overlay to a different position

## Controls

| Key | Action |
| --- | --- |
| F5 | Show or hide the statistics panel |
| F4 | Switch views |

## Requirements

- [ModTheSpire](https://steamcommunity.com/workshop/filedetails/?id=1605060445)
- [BaseMod](https://steamcommunity.com/workshop/filedetails/?id=1605833019)
- [Together in Spire](https://steamcommunity.com/sharedfiles/filedetails/?id=2384072973)

## Building

The project targets Java 8. Before building, copy the configuration template and fill in the paths for your installation:

```bash
cp .env.example .env
```

Set these variables in `.env`:

- `STS_JDK8`: Java 8 home directory
- `STS_GAME`: Slay the Spire installation directory
- `STS_WORKSHOP`: Steam Workshop mod directory

Build with the shell script:

```bash
bash build.sh
```

`.env` is for local use and must not be committed. `.env.example` is the versioned template. Gradle is also supported:

```bash
./gradlew build
```

## Damage attribution

`DamagePatch` intercepts damage events and assigns them based on `DamageInfo`:

- `NORMAL`: assigned to the attacking creature. In multiplayer, Together in Spire identifies the player.
- `THORNS` with an owner: assigned to the owner, including Lightning orbs, Thorns, Flame Barrier, Panache, and Thousand Cuts.
- `THORNS` without an owner: covers Charon's Ashes, Letter Opener, Mercury Hourglass, Stone Calendar, Combust, Fire Breathing, and Omega.
- `HP_LOSS`: covers Poison, Choke, and similar health-loss effects.
- Monster debuffs such as Corpse Explosion and The Bomb: assigned to the player who applied the debuff.

In multiplayer, each client calculates local-player damage and shares combat and turn totals through Together in Spire's ExtraData. Remote totals replace the local cache instead of being added to it, preventing double counting.

## Development notes

The game and mod dependencies are not included in this repository. The build reads `desktop-1.0.jar`, ModTheSpire, BaseMod, StSLib, and Together in Spire from the paths configured in `.env`.

## License

MIT. See [LICENSE](LICENSE).
