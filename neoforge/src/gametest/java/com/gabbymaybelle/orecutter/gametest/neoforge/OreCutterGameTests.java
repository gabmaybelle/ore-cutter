package com.gabbymaybelle.orecutter.gametest.neoforge;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.gamerules.GameRuleMap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Ore Cutter's NeoForge game tests: a little mod of its own that only
 * {@code :neoforge:runGameTestServer} loads, handing vanilla's test framework one test per check
 * (the checks are in {@link RecipeChecks}). Ore Cutter itself has no code, so everything here asks
 * the running server what its stonecutter would do.
 */
@Mod(OreCutterGameTests.MOD_ID)
public final class OreCutterGameTests {
	static final String MOD_ID = "orecutter_gametest";
	/** Vanilla's 1x1x1 template: these tests build nothing. */
	private static final Identifier EMPTY = Identifier.withDefaultNamespace("empty");

	/** The test functions by name; the test instance of the same name runs each. */
	private static final Map<String, Consumer<GameTestHelper>> FUNCTIONS = new LinkedHashMap<>();

	static {
		FUNCTIONS.put("every_recipe_is_in_the_game", RecipeChecks::recipesLoaded);
		FUNCTIONS.put("every_ore_cuts_to_its_twin", RecipeChecks::cuts);
		FUNCTIONS.put("every_deepslate_ore_is_crafted_from_a_block", RecipeChecks::crafts);
	}

	public OreCutterGameTests(IEventBus modBus) {
		modBus.addListener(RegisterEvent.class, OreCutterGameTests::registerFunctions);
		modBus.addListener(RegisterGameTestsEvent.class, OreCutterGameTests::registerTests);
	}

	private static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	/** Test functions live in a built-in registry, so they go in with the mod's registrations. */
	private static void registerFunctions(RegisterEvent event) {
		if (event.getRegistryKey().equals(Registries.TEST_FUNCTION)) {
			FUNCTIONS.forEach((name, function) -> event.register(Registries.TEST_FUNCTION, id(name), () -> function));
		}
	}

	/** Test instances and their environment are registered as the test world's data loads. */
	private static void registerTests(RegisterGameTestsEvent event) {
		// No rules changed: a plain world.
		Holder<TestEnvironmentDefinition<?>> plain = event.registerEnvironment(id("plain"), new TestEnvironmentDefinition.SetGameRules(GameRuleMap.of()));
		for (String name : FUNCTIONS.keySet()) {
			ResourceKey<Consumer<GameTestHelper>> function = ResourceKey.create(Registries.TEST_FUNCTION, id(name));
			event.registerTest(id(name), new FunctionGameTestInstance(function, new TestData<>(plain, EMPTY, 20, 0, true)));
		}
	}
}
