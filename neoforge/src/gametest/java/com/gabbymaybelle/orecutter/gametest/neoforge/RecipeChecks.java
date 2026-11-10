package com.gabbymaybelle.orecutter.gametest.neoforge;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SelectableRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.neoforged.fml.ModList;

/** The checks behind {@link OreCutterGameTests}. */
final class RecipeChecks {
	/** The ores with a deepslate twin. Written out here, not read from pack/, so a lost file fails. */
	private static final List<String> ORES = List.of("coal", "copper", "iron", "gold", "redstone", "emerald", "lapis", "diamond");

	/** What each deepslate ore is crafted from, with one deepslate: a whole block of its material, and the single piece that must not be enough. */
	private static final Map<String, String[]> CRAFTED_FROM = Map.of(
			"coal", new String[] { "coal_block", "coal" }, "copper", new String[] { "raw_copper_block", "raw_copper" },
			"iron", new String[] { "raw_iron_block", "raw_iron" }, "gold", new String[] { "raw_gold_block", "raw_gold" },
			"redstone", new String[] { "redstone_block", "redstone" }, "emerald", new String[] { "emerald_block", "emerald" },
			"lapis", new String[] { "lapis_block", "lapis_lazuli" }, "diamond", new String[] { "diamond_block", "diamond" });

	/** The metals whose ore can also be crafted from the smelted block. */
	private static final Map<String, String> SMELTED_BLOCK = Map.of("iron", "iron_block", "gold", "gold_block", "copper", "copper_block");

	private RecipeChecks() {
	}

	/**
	 * What the crafting table does: a block of the material and a deepslate make exactly one of
	 * that deepslate ore, and a single piece of the material with a deepslate does not.
	 */
	static void crafts(GameTestHelper helper) {
		RecipeManager manager = helper.getLevel().getServer().getRecipeManager();
		List<String> wrong = new ArrayList<>();
		for (String ore : ORES) {
			Item result = item("deepslate_" + ore + "_ore");
			String block = CRAFTED_FROM.get(ore)[0], piece = CRAFTED_FROM.get(ore)[1];
			ItemStack crafted = craft(helper, manager, block);
			if (!crafted.is(result) || crafted.getCount() != 1) {
				wrong.add(block + " and deepslate give " + crafted.getCount() + " " + BuiltInRegistries.ITEM.getKey(crafted.getItem()) + ", not 1 deepslate_" + ore + "_ore");
			}
			// The three metals also take their smelted block, the one most players reach for first.
			String smelted = SMELTED_BLOCK.get(ore);
			if (smelted != null && !craft(helper, manager, smelted).is(result)) {
				wrong.add(smelted + " and deepslate don't give deepslate_" + ore + "_ore");
			}
			if (craft(helper, manager, piece).is(result)) {
				wrong.add("a single " + piece + " and deepslate is enough for deepslate_" + ore + "_ore");
			}
		}
		helper.assertTrue(wrong.isEmpty(), wrong.size() + " problem(s): " + String.join("; ", wrong));
		System.out.println("[orecutter_gametest] " + ORES.size() + " crafting recipes checked, each costs a whole block");
		helper.succeed();
	}

	/** What the crafting grid gives for this item beside one deepslate (empty when nothing). */
	private static ItemStack craft(GameTestHelper helper, RecipeManager manager, String with) {
		CraftingInput input = CraftingInput.of(2, 1, List.of(new ItemStack(item(with)), new ItemStack(item("deepslate"))));
		return manager.getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel())
				.map(holder -> ((RecipeHolder<CraftingRecipe>) holder).value().assemble(input)).orElse(ItemStack.EMPTY);
	}

	/** Every recipe file in pack/ is a recipe the game has, and there are three per ore. */
	static void recipesLoaded(GameTestHelper helper) {
		Path recipes = Path.of(System.getProperty("orecutter.expect")).resolve("data/orecutter/recipe");
		helper.assertTrue(Files.isDirectory(recipes), "no recipe folder at " + recipes);
		RecipeManager manager = helper.getLevel().getServer().getRecipeManager();
		int files = 0;
		List<String> missing = new ArrayList<>();
		try (Stream<Path> list = Files.walk(recipes)) {
			for (Path file : list.filter(p -> p.toString().endsWith(".json")).toList()) {
				files++;
				String name = recipes.relativize(file).toString().replace('\\', '/').replace(".json", "");
				if (manager.byKey(ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath("orecutter", name))).isEmpty()) {
					missing.add(name);
				}
			}
		} catch (IOException e) {
			throw new IllegalStateException("couldn't read the recipes in " + recipes, e);
		}
		helper.assertTrue(files == ORES.size() * 3, files + " recipe files in pack/, not " + ORES.size() * 3 + " (three per ore)");
		helper.assertTrue(missing.isEmpty(), "not in the game: " + String.join(", ", missing));
		// A run with other mods names them, so jars that didn't load fail here instead of passing without them.
		for (String id : System.getProperty("orecutter.expectMods", "").split(",")) {
			helper.assertTrue(id.isBlank() || ModList.get().isLoaded(id.strip()), "the mod " + id.strip() + " should be loaded for this run");
		}
		System.out.println("[orecutter_gametest] " + files + " recipes in the game");
		helper.succeed();
	}

	/**
	 * What the stonecutter does with each ore: it offers the twin exactly once, cutting gives one
	 * twin for one ore, and (with no other mods loaded) it offers nothing else. Both directions.
	 */
	static void cuts(GameTestHelper helper) {
		RecipeManager manager = helper.getLevel().getServer().getRecipeManager();
		boolean alone = System.getProperty("orecutter.expectMods", "").isBlank();
		List<String> wrong = new ArrayList<>();
		int cuts = 0;
		for (String ore : ORES) {
			for (boolean toDeepslate : new boolean[] { true, false }) {
				String from = (toDeepslate ? "" : "deepslate_") + ore + "_ore";
				String to = (toDeepslate ? "deepslate_" : "") + ore + "_ore";
				ItemStack input = new ItemStack(item(from));
				List<ItemStack> results = new ArrayList<>();
				for (SelectableRecipe.SingleInputEntry<StonecutterRecipe> entry : manager.stonecutterRecipes().selectByInput(input).entries()) {
					entry.recipe().recipe().ifPresent(holder -> results.add(holder.value().assemble(new SingleRecipeInput(input))));
				}
				List<ItemStack> twins = results.stream().filter(stack -> stack.is(item(to))).toList();
				if (twins.size() != 1) {
					wrong.add(from + " offers " + to + " " + twins.size() + " times");
				} else if (twins.getFirst().getCount() != 1) {
					wrong.add(from + " gives " + twins.getFirst().getCount() + " " + to + ", not 1");
				}
				if (alone && results.size() != twins.size()) {
					wrong.add(from + " also offers " + results.stream().filter(stack -> !stack.is(item(to)))
							.map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()).toList());
				}
				cuts++;
			}
		}
		helper.assertTrue(wrong.isEmpty(), wrong.size() + " problem(s): " + String.join("; ", wrong));
		System.out.println("[orecutter_gametest] " + cuts + " cuts checked" + (alone ? ", nothing else offered" : ""));
		helper.succeed();
	}

	private static Item item(String name) {
		Identifier id = Identifier.withDefaultNamespace(name);
		if (!BuiltInRegistries.ITEM.containsKey(id)) {
			throw new IllegalStateException("no item " + id);
		}
		return BuiltInRegistries.ITEM.getValue(id);
	}
}
