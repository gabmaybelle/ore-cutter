# Progress

## Where things stand

- **What it does:** the eight ores with a deepslate twin (coal, copper, iron, gold, redstone,
  emerald, lapis, diamond) swap between their stone and deepslate looks in the stonecutter, one
  for one, both ways. Data only: 16 stonecutting recipes in `pack/`, wrapped as a Fabric jar, a
  NeoForge jar and a data pack zip. Minecraft 26.3.
- **Broken:** nothing known.
- **Next:** the first release. Locked scope: the 16 stonecutter recipes, in two jars and one data
  pack zip, and nothing else. Still to do before it: the Fabric in-game test's first run (CI), the
  icon (Gabby's art), and a short hand play of the release jars. New ideas go to a later version.
- **Later, only if players ask:** a crafting-grid swap for players without a stonecutter; recipes
  for other mods' ores that have deepslate twins.
- **Never:** crafting ore from raw material or ingots. One raw iron in gives a block that Fortune
  mines back into several, so any such recipe is a farm. Nether ores and ancient debris have no
  deepslate twin, so they are left alone.

## Sessions

| Date | Session | What happened |
|---|---|---|
| 2026-11-10 | The whole mod | The repo: `pack/` holds the 16 recipes and `pack.mcmeta`; `fabric` and `neoforge` have no code and wrap that folder with their loader's metadata; `./gradlew datapack` zips the same folder. On Fabric the jar needs Fabric API, which is what reads a mod's recipes into the game. Both jars and the zip build. NeoForge game tests on vanilla's test server (no window): every recipe file in `pack/` is in the game and there are 16 of them; for each ore, both ways, the stonecutter offers the twin exactly once, a cut gives one twin for one ore, and nothing else is offered: 2 tests pass, 16 recipes, 16 cuts. Proof run: with one recipe file taken out, both tests fail ("15 recipe files", "iron_ore offers deepslate_iron_ore 0 times"), and pass again with it back. Beside Cozy Core 0.12.1 and JEI 31.14.0.72 (both required to be loaded) the same 2 tests pass. Fabric and NeoForge dedicated servers load the jars with 0 problem lines and list Ore Cutter's data, on their own and beside Cozy Core and JEI; a server with only the data pack zip loads it with 0 problem lines. The Fabric in-game test (a real stonecutter screen: the twin is offered once, a real mouse click picks it, the result slot holds one, taking it leaves 63 of 64 ore) compiles and waits for its first CI run, as do the CI load tests on Linux. |
