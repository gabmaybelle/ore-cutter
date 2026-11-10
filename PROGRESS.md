# Progress

## Where things stand

- **What it does:** two things for the eight ores with a deepslate twin (coal, copper, iron, gold,
  redstone, emerald, lapis, diamond). Each deepslate ore is crafted from a block of its material (iron, gold and copper: the raw or the smelted block)
  and one deepslate (shapeless). Each ore also swaps with its twin in the stonecutter, one for
  one, both ways. Data only: 8 crafting and 16 stonecutting recipes in `pack/`, wrapped as a
  Fabric jar, a NeoForge jar and a data pack zip. Minecraft 26.3.
- **Broken:** nothing known.
- **Next:** the listings (Gabby, by hand). Then a port to Minecraft 1.21.1 once it is listed and
  has been played, if players are there. New ideas go to a later version.
- **Later, only if players ask:** crafting the stone ores too; recipes for other mods' ores that
  have deepslate twins.
- **The price rule:** a crafted ore costs a whole block of its material (9 pieces). The most a
  Fortune III pickaxe drops from one ore is 4 for coal, iron, gold, emerald and diamond and 8 for
  redstone, so those can never pay back; copper averages under 9; lapis can return a little more
  than it cost, which Gabby accepted. A cheaper recipe (one piece per ore) would be a farm, so the
  tests check a single piece is not enough. Nether ores and ancient debris have no deepslate
  twin, so they are left alone.

## Sessions

| Date | Session | What happened |
|---|---|---|
| 2026-11-10 | The whole mod | The repo: `pack/` holds the 16 recipes and `pack.mcmeta`; `fabric` and `neoforge` have no code and wrap that folder with their loader's metadata; `./gradlew datapack` zips the same folder. On Fabric the jar needs Fabric API, which is what reads a mod's recipes into the game. Both jars and the zip build. NeoForge game tests on vanilla's test server (no window): every recipe file in `pack/` is in the game and there are 16 of them; for each ore, both ways, the stonecutter offers the twin exactly once, a cut gives one twin for one ore, and nothing else is offered: 2 tests pass, 16 recipes, 16 cuts. Proof run: with one recipe file taken out, both tests fail ("15 recipe files", "iron_ore offers deepslate_iron_ore 0 times"), and pass again with it back. Beside Cozy Core 0.12.1 and JEI 31.14.0.72 (both required to be loaded) the same 2 tests pass. Fabric and NeoForge dedicated servers load the jars with 0 problem lines and list Ore Cutter's data, on their own and beside Cozy Core and JEI; a server with only the data pack zip loads it with 0 problem lines. The Fabric in-game test (a real stonecutter screen: the twin is offered once, a real mouse click picks it, the result slot holds one, taking it leaves 63 of 64 ore) compiles and waits for its first CI run, as do the CI load tests on Linux. |
| 2026-11-10 | First CI run and the icon | CI, all eleven jobs pass: the Fabric in-game test cut all 16 ways through the real stonecutter screen with 0 problems, on its own and beside Cozy Core and JEI; the NeoForge game tests the same; Fabric and NeoForge servers load on their own and beside both mods, and a plain vanilla server loads the data pack zip and lists it as enabled. Gabby chose the icon: a deepslate emerald ore block, as the inventory shows it, on transparency. It is `pack/pack.png`, so the data pack and both jars carry the same picture. Both jars and the zip rebuild with it; the NeoForge game tests still pass and both dedicated servers still load with 0 problem lines. The mod page's pictures come from a scene in the Fabric game test (`Screenshots`, only with `-Pscreenshots`): a crafting table, barrel and stonecutter with one emerald ore block beside them, the stonecutter screen offering the deepslate twin, and the same workstation with the block swapped. The Fabric in-game test passed again in the same run (16 cuts, 0 problems). |
| 2026-11-10 | Craftable deepslate ores | The request behind the mod was for craftable deepslate ores, so Gabby added crafting and set the price: a block of the material plus one deepslate, for all eight (8 shapeless recipes; the stonecutter swaps stay). NeoForge game tests, 3 pass: 24 recipes in the game; 16 cuts; and for each ore the block and a deepslate craft exactly one deepslate ore while a single piece and a deepslate do not. Proof run: with the diamond recipe changed to take one diamond, the crafting test fails both ways (the block makes nothing, a single diamond is enough), and passes with it back. Fabric in-game tests, in a real game: the stonecutter test as before (16 cuts, 0 problems) and a new one at a real crafting table (the result slot shows one deepslate ore, taking it uses up both ingredients, a single piece makes nothing): 8 recipes, 0 problems. The page pictures gain the crafting table with a block of emerald and a deepslate. README and the in-game description now lead with crafting. |
| 2026-11-10 | Hand play: either metal block | Gabby's hand play on a Fabric server, joined with a plain vanilla game (so only the server needs the mod): coal, redstone, emerald, lapis and diamond crafted; iron, gold and copper did not, because she used the smelted blocks and the recipes took only the raw ones. Those three now take either block (the cost is the same: nine ingots are nine raw pieces smelted). Both tests check the smelted block too; the NeoForge game tests pass (3 of 3). She then crafted all eight on the Fabric and the NeoForge server, each joined with a plain vanilla game. |
| 2026-11-10 | Release | The release commit: CI passes all eleven jobs with the either-block recipes (the Fabric in-game test crafts from the smelted blocks too), the hand play is done on both loaders, and the jars and data pack zip are rebuilt from this commit for the GitHub release. |
