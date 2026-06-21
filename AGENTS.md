# AGENTS.md

## Overview

This folder is the **Forge 1.12.2** port of FairAnvil, built to run on RLCraft / Dregora
servers. FairAnvil makes anvils and enchanting tables charge a **flat XP-point cost**
instead of whole scaling levels, and removes the anvil's **"Too Expensive!"** cap. The
cost no longer balloons just because the player's level is high.

The original **Bukkit/Spigot 1.17.1** plugin lives at the repo root (`../src`, `../pom.xml`)
and is a separate Maven project. This `forge/` project shares the concept and the
`com.scorp.fairanvil` package name but is otherwise independent (different MC version,
build system, and a duplicated `Experience.java`).

## Build & run

**Must build with a Java 8 JDK.** ForgeGradle 2.3 / Forge 1.12.2 will not build on newer
JDKs. The machine's `JAVA_HOME` may point at a newer JDK, so override it. From this
`forge/` folder in PowerShell:

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-8.0.482.8-hotspot"
.\gradlew.bat build
```

- Run from the `forge/` folder. The Gradle 4.10.3 wrapper is committed, so no global Gradle install is needed.
- First build downloads Gradle/Minecraft/Forge and decompiles (several minutes); later builds are fast.
- Output jar: `build/libs/FairAnvil-Forge-1.12.2-1.0.jar` (ignore the `-sources.jar`).
- `build` is finalized by `copyToRLCraft`, which copies the jar into a hardcoded local RLCraft `mods/` dir. Override with `-PmodsDir=...`, or run the copy alone with `gradlew copyToRLCraft`.
- Builds against Forge `1.12.2-14.23.5.2847`; the jar runs on any `14.23.5.x` 1.12.2 server.
- If `stable_39` mappings fail to download, switch `mappings` in `build.gradle` to `snapshot_20171003`.

## Code layout

`src/main/java/com/scorp/fairanvil/`

- `FairAnvilMod.java` - `@Mod` entry point: loads config, registers the command, and intercepts anvil/enchanting-table right-clicks.
- `ContainerFairAnvil.java` - custom anvil container reusing vanilla combine/repair math without the 40-level cap.
- `ContainerFairEnchantment.java` - custom enchanting container that refunds vanilla's level cost and charges the flat point equivalent.
- `FairAnvilInteractionObject.java` / `FairEnchantInteractionObject.java` - `IInteractionObject` hooks used to open the custom containers.
- `CommandFairAnvil.java` - `/fairanvil <toggle|anvil|enchant|status>` (op / permission level 2); `toggle` is the master switch.
- `Experience.java` - XP <-> level math helpers (mirror of the Bukkit target's copy).

`src/main/resources/mcmod.info` - mod metadata (version/mcversion expanded at build time).

## How it works

1. On right-click of an anvil/enchanting table the server cancels the vanilla interaction and opens the matching custom container.
2. The anvil container reuses vanilla combine/repair math but skips the 40-level cap and charges points via `Experience.getExpFromLevel(cost)`.
3. The enchanting container lets vanilla roll the enchant, then refunds the levels vanilla took and charges the flat point equivalent.
4. The GUI still shows vanilla's "level cost"; the actual deduction is the cheaper flat point equivalent, matching the original plugin.

Config `config/fairanvil.cfg` (all default `true`):

- `fairAnvil` - flat anvil cost + removed cap.
- `fairEnchant` - flat enchanting-table cost.
- `hideTooExpensive` - clamp the displayed/charged cost below 40 so the vanilla client never shows "Too Expensive!".

## Conventions & constraints

- Compile against **Java 8** (`sourceCompatibility`/`targetCompatibility` = 1.8). Do not use Java 9+ APIs or syntax.
- **Server-side only**: `acceptableRemoteVersions = "*"`, and no blocks/items/GUIs are registered. Keep all logic server-side; vanilla and modpack clients connect with no extra download.
- `build/` and `.gradle/` are generated build artifacts - do not edit them. Real sources live under `src/`.
- The version string lives in `build.gradle` (`version`) and `FairAnvilMod.VERSION`; keep them in sync.

## Caveats (RLCraft / Dregora)

- RLCraft heavily modifies progression; verify behavior against the exact pack version.
- If another mod also intercepts anvil/enchanting-table right-clicks, event ordering matters.
- The enchanting table still enforces vanilla's level requirement to unlock a slot (e.g. level 30 for the top option); only the consumed amount becomes a flat cost. If RLCraft gates it differently, set `fairEnchant=false` and keep just the fair anvil.
