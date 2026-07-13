# AGENTS.md

Guidance for AI agents (and human contributors) working on this project.

## Project overview

`StatTracker` is a Slay the Spire mod (built with BaseMod + ModTheSpire) that tracks
per-player damage and team contribution, primarily for the **Together in Spire**
multiplayer mod. Source lives under `src/main/java/damagetracker/`. The mod is
published to the Steam Workshop (published item id `3721416376`) and the source is
open on GitHub at https://github.com/wzacolemak/StatTracker.

## Build

- Compile target is Java 8 (the game runs on JRE 8). Use JDK 8 if available, e.g.
  `E:/env/Java/jdk8u202`.
- `build.sh` is the canonical build script: it compiles with `javac`, packages a jar
  with a manifest, and copies it into the mod-uploader workspace
  `E:/SteamLibrary/steamapps/common/SlayTheSpire/stat-tracker/content/StatTracker.jar`.
- Dependency jars come from the Steam Workshop (`steamapps/workshop/content/646570`):
  ModTheSpire, BaseMod, StSLib, Together in Spire. The game core is
  `desktop-1.0.jar` in the game directory.
- A local `com/` tree (ModTheSpire annotation sources) and `gradle-8.5-bin.zip`
  exist for convenience — do NOT commit them (see `.gitignore`).

## Releasing / Workshop updates

To release a new version:

1. Make code changes.
2. Bump `version` in **both**:
   - `src/main/resources/ModTheSpire.json`
   - `stat-tracker/config.json` (the workshop mod-uploader config, in the game dir)
3. Rebuild the jar and copy it to `stat-tracker/content/StatTracker.jar`.
4. Upload to the workshop:
   `java -jar E:/SteamLibrary/steamapps/common/SlayTheSpire/mod-uploader.jar upload -w stat-tracker`

## Changelog rule (IMPORTANT)

**The workshop description's changelog must keep only the latest 5 versions.**

When adding a new version entry to the `description` field in
`stat-tracker/config.json`:

- Add the new `[h2]vX.Y.Z[/h2]` block at the **top** of the changelog section.
- After adding, **delete older entries until only 5 version blocks remain**
  (the newest 5). Older releases are dropped — do not keep them.
- Keep each entry bilingual (English / Chinese), matching the existing style.
- Also set the `changeNote` field to a one-line summary of the newest version
  (Steam shows this as the per-update note).

This applies to the Steam Workshop description only. Git history / GitHub
releases are unaffected and may retain the full history.
