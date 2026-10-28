# FairAnvil (Forge 1.12.2)

A Forge 1.12.2 port of the FairAnvil Bukkit plugin, built to run on RLCraft / Dregora servers.

Anvils and enchanting tables charge their cost as **XP points counted from level 0** instead of
taking whole levels off the top of your current level. The anvil's level cost itself is left exactly
as the pack computes it (vanilla plus any mods that patch the anvil). The cost no longer balloons just because your
level is high, which is the whole point of "fair" anvils.

## Server-side only

This mod is designed to run on the **server only**. Players connect with their normal (vanilla or
modpack) client and need **no extra download**:

- `@Mod(acceptableRemoteVersions = "*")` lets clients without the mod connect.
- It registers **no blocks, items, or custom GUIs**. When you open an anvil/enchanting table it
  reuses the vanilla `minecraft:anvil` / `minecraft:enchanting_table` GUI; all logic is server-side.

## How it works

1. On right-click of an anvil or enchanting table, the server cancels the vanilla interaction and
   opens a custom container (`ContainerFairAnvil` / `ContainerFairEnchantment`).
2. The anvil container lets vanilla (and every mod that patches the anvil) compute the level cost, then
   charges it as points via `Experience.getExpFromLevel(cost)`.
3. The enchanting container lets vanilla roll the enchantment, then refunds the levels vanilla took
   and charges the flat point equivalent instead.

The number shown in the anvil GUI is still vanilla's "level cost"; the actual amount deducted is the
flat point equivalent, so it is cheaper than it looks (matching the original plugin).

## Config

`config/fairanvil.cfg`:

| Option             | Default | Meaning                                                              |
| ------------------ | ------- | -------------------------------------------------------------------- |
| `fairAnvil`        | `true`  | Charge the anvil cost as points from level 0.                         |
| `fairEnchant`      | `true`  | Apply fair cost on the enchanting table.                            |

## In-game command

`/fairanvil <toggle|anvil|enchant|status>` (op / permission level 2). `toggle` is a master switch.

## Building

Requires a **Java 8 JDK** (ForgeGradle 2.3 / Forge 1.12.2 will not build on newer JDKs). A Gradle
4.10.3 wrapper is already included in this folder, so you do not need Gradle installed.

Because the machine's `JAVA_HOME` may point at a newer JDK, set it to a Java 8 JDK for the build.
From this `forge/` folder in PowerShell:

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-8.0.482.8-hotspot"
.\gradlew.bat build
```

(Adjust the path to your own Java 8 install.) The first build downloads Gradle, Minecraft, and the
Forge libraries, then deobfuscates/decompiles - this takes several minutes. Subsequent builds are
fast.

The mod jar is produced at `build/libs/FairAnvil-Forge-1.12.2-1.0.jar`. Drop it into your server's
`mods/` folder. (Ignore the `-sources.jar`.)

Notes:
- This builds against Forge `1.12.2-14.23.5.2847` (the build RLCraft ships on); the compiled jar
  runs on any `14.23.5.x` server for 1.12.2.
- If the `stable_39` mappings ever fail to download, change `mappings` in `build.gradle` to
  `snapshot_20171003`.

## RLCraft / Dregora caveats (please test)

- RLCraft heavily modifies progression. This mod uses vanilla anvil/enchant maths plus
  `AnvilUpdateEvent`, so it should be compatible, but verify against your exact pack version.
- If another mod also intercepts anvil/enchanting-table right-clicks, event ordering could matter.
- The enchanting table still enforces vanilla's **level requirement** to use a slot (e.g. you must
  reach level 30 for the top option); only the amount consumed becomes a flat point cost. If RLCraft
  gates the enchanting table differently, set `fairEnchant=false` and keep just the fair anvil.
