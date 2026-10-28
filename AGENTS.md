# AGENTS.md

> **Maintenance**: Keep this file current. When you change build steps, config keys, cost/charge
> logic, or discover a new RLCraft/mod interaction, update the relevant section in the same change.

## Overview

This folder is the **Forge 1.12.2** port of FairAnvil, built to run on RLCraft / Dregora
servers. FairAnvil makes anvils and enchanting tables charge their cost as **XP points counted from
level 0** instead of taking whole levels off the top of the player's current level, so the
price no longer balloons just because the player's level is high. Since 1.2 the anvil's
**level cost itself is untouched**: it is whatever the pack computes without FairAnvil.

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
- Output jar: `build/libs/FairAnvil-Forge-1.12.2-<version>.jar`, currently `1.2` (ignore the `-sources.jar`).
- `build` is finalized by `copyToRLCraft`, which copies the jar into a hardcoded local RLCraft `mods/` dir. Override with `-PmodsDir=...`, or run the copy alone with `gradlew copyToRLCraft`.
- Builds against Forge `1.12.2-14.23.5.2847`; the jar runs on any `14.23.5.x` 1.12.2 server.
- If `stable_39` mappings fail to download, switch `mappings` in `build.gradle` to `snapshot_20171003`.

## Code layout

`src/main/java/com/scorp/fairanvil/`

- `FairAnvilMod.java` - `@Mod` entry point: loads config, registers the command, and intercepts anvil/enchanting-table right-clicks.
- `ContainerFairAnvil.java` - custom anvil container: calls vanilla `updateRepairOutput` (so all anvil mixins/events apply) and swaps only the output slot, which charges points.
- `LegacyRepairCost.java` - one-time rebase of items worked by FairAnvil <= 1.1 onto EnchantmentControl's cost scaling (see below).
- `ContainerFairEnchantment.java` - custom enchanting container that refunds vanilla's level cost and charges the flat point equivalent.
- `FairAnvilInteractionObject.java` / `FairEnchantInteractionObject.java` - `IInteractionObject` hooks used to open the custom containers.
- `CommandFairAnvil.java` - `/fairanvil <toggle|anvil|enchant|status>` (op / permission level 2); `toggle` is the master switch.
- `Experience.java` - XP <-> level math helpers (mirror of the Bukkit target's copy).

`src/main/resources/mcmod.info` - mod metadata (version/mcversion expanded at build time).

## How it works

1. On right-click of an anvil/enchanting table the server cancels the vanilla interaction and opens the matching custom container.
2. The anvil container calls `super.updateRepairOutput()` and never computes a cost itself. The resulting `maximumCost` (levels) is charged as `Experience.getExpFromLevel(cost)` points. There is no clamp.
3. The enchanting container lets vanilla roll the enchant, then refunds the levels vanilla took and charges the flat point equivalent.
4. The GUI still shows vanilla's "level cost"; the actual deduction is the cheaper flat point equivalent, matching the original plugin.
5. **Up to 1.1 FairAnvil re-implemented the anvil maths in its own `updateRepairOutput()`.** That silently bypassed every
   mixin other mods put on `ContainerRepair#updateRepairOutput` - in Dregora, EnchantmentControl's QUADRATIC prior-work
   scaling, `AnvilCount` tracking, Never Too Expensive, free book combination and zero-cost-if-unchanged - so items got
   vanilla's doubling penalty (RepairCost 2^n-1, 32767 after 15 uses, tooltip "0 Anvil uses (32767)"), and a 39-level
   clamp (`hideTooExpensive`, removed in 1.2) hid it. **Never override the cost logic again; wrap it.** 1.1 also added the
   input prior-work penalty to `AnvilUpdateEvent`-supplied recipes; 1.2 drops that (vanilla/Forge don't). On each update
   `LegacyRepairCost` rebases input items that have RepairCost `2^n-1` and no `AnvilCount` to EnchantmentControl's cost
   for `n` uses (by reflection; skipped if EC is absent or `enableAnvilScalingChange` is off).
6. `FairOutputSlot.onTake()` posts Forge's `AnvilRepairEvent` (via `ForgeHooks.onAnvilRepair`) **before** clearing the input slots, exactly like vanilla's output slot, and uses the returned break chance (default 0.12) for anvil damage. Since 1.1. Without it, mods that refund inputs on that event lose items: Enchanter Tools' Enchanted Inkwell (book left + inkwell right gives an enchanted book, and the mod restores the inkwell and the rest of the book stack the next tick) was eaten along with the whole book stack (reported on the Nightmare server 2026-10-19).

Config `config/fairanvil.cfg` (all default `true`):

- `fairAnvil` - anvil cost charged as points from level 0.
- `fairEnchant` - flat enchanting-table cost.

## Conventions & constraints

- Compile against **Java 8** (`sourceCompatibility`/`targetCompatibility` = 1.8). Do not use Java 9+ APIs or syntax.
- **Server-side only**: `acceptableRemoteVersions = "*"`, and no blocks/items/GUIs are registered. Keep all logic server-side; vanilla and modpack clients connect with no extra download.
- `build/` and `.gradle/` are generated build artifacts - do not edit them. Real sources live under `src/`.
- The version string lives in `build.gradle` (`version`) and `FairAnvilMod.VERSION`; keep them in sync.

## Caveats (RLCraft / Dregora)

- RLCraft heavily modifies progression; verify behavior against the exact pack version.
- If another mod also intercepts anvil/enchanting-table right-clicks, event ordering matters.
- The enchanting table still enforces vanilla's level requirement to unlock a slot (e.g. level 30 for the top option); only the consumed amount becomes a flat cost. If RLCraft gates it differently, set `fairEnchant=false` and keep just the fair anvil.
- **Client/server cost**: the client computes the cost with its own `ContainerRepair` (same pack mods), and since 1.2 the server uses the identical vanilla path, so the displayed level cost matches what is charged (as points from level 0).

## Debugging cost issues

- `updateRepairOutput()` and `FairOutputSlot.onTake()` are the two places that decide and charge cost. When a cost looks wrong, temporary `System.out.println` lines there surface in the server `logs/latest.log` (FML routes stdout through Log4j with `immediateFlush=true`, so lines appear immediately; `latest.log` holds only the current run and is rotated to a dated `.gz` on restart).
- Log the entry snapshot of both input slots plus each early-`return` branch: cost surprises are usually an early bail-out (incompatible enchant, or the `AnvilUpdateEvent`-supplied-output path) rather than the main combine math.
