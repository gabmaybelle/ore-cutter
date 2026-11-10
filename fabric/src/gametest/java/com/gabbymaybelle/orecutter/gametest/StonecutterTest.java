package com.gabbymaybelle.orecutter.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.StonecutterScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SelectableRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Ore Cutter through the real stonecutter screen, the way a player meets it. For each of the eight
 * ores, both ways round: a stack goes in the stonecutter, the twin must be on offer exactly once,
 * the test clicks it with the real mouse, checks the result slot holds one twin, takes it out and
 * checks it landed in the inventory and used up an ore.
 */
public class StonecutterTest implements FabricClientGameTest {
	/** The ores with a deepslate twin. Written out here, not read from pack/, so a lost file fails. */
	private static final List<String> ORES = List.of("coal", "copper", "iron", "gold", "redstone", "emerald", "lapis", "diamond");
	/** The stonecutter screen's recipe buttons: 4 columns, 3 rows, 16 x 18 pixels, from (52, 14). */
	private static final int COLUMNS = 4, ROWS = 3, BUTTON_W = 16, BUTTON_H = 18, RECIPES_X = 52, RECIPES_Y = 14;

	@Override
	public void runTest(ClientGameTestContext ctx) {
		int files = recipeFiles();
		check(files == ORES.size() * 3, files + " recipe files in pack/, not " + ORES.size() * 3 + " (three per ore)");
		List<String> failures = new ArrayList<>();
		int cuts = 0;
		try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			TestServerContext server = world.getServer();
			server.runCommand("gamemode survival @a");
			BlockPos cutter = server.computeOnServer(s -> player(s).blockPosition().offset(2, 0, 0));
			server.runCommand("setblock " + cutter.getX() + " " + cutter.getY() + " " + cutter.getZ() + " minecraft:stonecutter");
			ctx.waitTicks(2);
			ctx.runOnClient(mc -> mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
					new BlockHitResult(Vec3.atCenterOf(cutter), Direction.UP, cutter, false)));
			ctx.waitForScreen(StonecutterScreen.class);

			for (String ore : ORES) {
				for (boolean toDeepslate : new boolean[] { true, false }) {
					String from = (toDeepslate ? "" : "deepslate_") + ore + "_ore";
					Identifier to = Identifier.withDefaultNamespace((toDeepslate ? "deepslate_" : "") + ore + "_ore");
					Item input = item(Identifier.withDefaultNamespace(from));
					Item twin = item(to);
					cuts++;
					if (!putInInput(ctx, server, input)) {
						failures.add(from + ": the stonecutter offers nothing for it");
						continue;
					}
					List<Identifier> offered = ctx.computeOnClient(StonecutterTest::offered);
					long times = offered.stream().filter(to::equals).count();
					if (times != 1) {
						failures.add(from + ": " + to.getPath() + " is offered " + times + " times (on offer: " + offered + ")");
						continue;
					}
					if (cuts == 1) {
						ctx.takeScreenshot("stonecutter_" + from);
					}
					// Pick the twin, the way a player would, and check what the result slot holds.
					select(ctx, offered.indexOf(to));
					ItemStack result = ctx.computeOnClient(mc -> ((StonecutterMenu) mc.player.containerMenu).getSlot(StonecutterMenu.RESULT_SLOT).getItem().copy());
					if (!result.is(twin) || result.getCount() != 1) {
						failures.add(from + ": picking " + to.getPath() + " gave " + result.getCount() + " " + BuiltInRegistries.ITEM.getKey(result.getItem()));
						continue;
					}
					// Take it out: one twin lands in the inventory and one ore is used up.
					ctx.runOnClient(mc -> mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId,
							StonecutterMenu.RESULT_SLOT, 0, ContainerInput.PICKUP, mc.player));
					ctx.waitTicks(3);
					int carried = server.computeOnServer(s -> player(s).containerMenu.getCarried().is(twin) ? player(s).containerMenu.getCarried().getCount() : 0);
					int left = server.computeOnServer(s -> player(s).containerMenu.getSlot(StonecutterMenu.INPUT_SLOT).getItem().getCount());
					if (carried != 1 || left != 63) {
						failures.add(from + ": one cut should give 1 " + to.getPath() + " and leave 63 ore, but gave " + carried + " and left " + left);
					}
				}
			}
		}
		System.out.println("[orecutter_gametest] stonecutter: " + cuts + " cuts checked, " + failures.size() + " problem(s)");
		check(failures.isEmpty(), "stonecutter:\n  " + String.join("\n  ", failures));
	}

	/**
	 * Puts 64 of the item in the stonecutter's input (emptying the player's hands and inventory
	 * first) and waits for the screen to offer something. False if it never does.
	 */
	static boolean putInInput(ClientGameTestContext ctx, TestServerContext server, Item item) {
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			player.getInventory().clearContent();
			StonecutterMenu menu = (StonecutterMenu) player.containerMenu;
			menu.setCarried(ItemStack.EMPTY);
			menu.getSlot(StonecutterMenu.RESULT_SLOT).set(ItemStack.EMPTY);
			menu.getSlot(StonecutterMenu.INPUT_SLOT).set(new ItemStack(item, 64));
			menu.broadcastChanges();
		});
		try {
			ctx.waitFor(mc -> mc.player.containerMenu instanceof StonecutterMenu menu
					&& menu.getSlot(StonecutterMenu.INPUT_SLOT).getItem().is(item) && menu.getNumberOfVisibleRecipes() > 0, 40);
		} catch (AssertionError e) {
			return false;
		}
		ctx.waitTicks(2);
		return true;
	}

	/** Picks the recipe button at this index: with the real mouse when it's on the first page, else as the screen would. */
	static void select(ClientGameTestContext ctx, int index) {
		if (index < COLUMNS * ROWS) {
			int[] at = ctx.computeOnClient(mc -> {
				AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) mc.gui.screen();
				int left = field(screen, "leftPos"), top = field(screen, "topPos");
				return new int[] { left + RECIPES_X + (index % COLUMNS) * BUTTON_W + BUTTON_W / 2, top + RECIPES_Y + (index / COLUMNS) * BUTTON_H + BUTTON_H / 2 };
			});
			double scale = ctx.computeOnClient(mc -> (double) mc.getWindow().getGuiScale());
			ctx.getInput().setCursorPos(at[0] * scale, at[1] * scale);
			ctx.waitTick();
			ctx.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
		} else {
			ctx.runOnClient(mc -> mc.gameMode.handleInventoryButtonClick(mc.player.containerMenu.containerId, index));
		}
		try {
			ctx.waitFor(mc -> ((StonecutterMenu) mc.player.containerMenu).getSelectedRecipeIndex() == index, 40);
		} catch (AssertionError e) {
			throw new AssertionError("Failed: " + (index < COLUMNS * ROWS ? "clicking" : "picking") + " recipe button " + index
					+ " didn't select it (the screen's buttons aren't where the test expects, or the click was ignored)", e);
		}
		// The server fills the result slot and sends it over, which can take more than a couple
		// of ticks on a busy machine. Wait for it; the caller's check reports what's there if it never comes.
		try {
			ctx.waitFor(mc -> !((StonecutterMenu) mc.player.containerMenu).getSlot(StonecutterMenu.RESULT_SLOT).getItem().isEmpty(), 40);
		} catch (AssertionError ignored) {
			// reported by the caller
		}
		ctx.waitTicks(2);
	}

	/** The outputs the stonecutter screen is offering, in button order. */
	static List<Identifier> offered(Minecraft mc) {
		StonecutterMenu menu = (StonecutterMenu) mc.player.containerMenu;
		List<Identifier> out = new ArrayList<>();
		for (SelectableRecipe.SingleInputEntry<StonecutterRecipe> entry : menu.getVisibleRecipes().entries()) {
			ItemStack stack = entry.recipe().optionDisplay().resolveForFirstStack(SlotDisplayContext.fromLevel(mc.level));
			out.add(BuiltInRegistries.ITEM.getKey(stack.getItem()));
		}
		return out;
	}

	/** How many recipe files pack/ holds (the folder both jars and the data pack are made from). */
	private static int recipeFiles() {
		Path recipes = Path.of(System.getProperty("orecutter.expect")).resolve("data/orecutter/recipe");
		try (Stream<Path> files = Files.walk(recipes)) {
			return (int) files.filter(p -> p.toString().endsWith(".json")).count();
		} catch (IOException e) {
			throw new AssertionError("couldn't read the recipes in " + recipes, e);
		}
	}

	private static Item item(Identifier id) {
		check(BuiltInRegistries.ITEM.containsKey(id), "the item " + id + " exists");
		return BuiltInRegistries.ITEM.getValue(id);
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static int field(Object screen, String name) {
		try {
			Field f = AbstractContainerScreen.class.getDeclaredField(name);
			f.setAccessible(true);
			return f.getInt(screen);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private static void check(boolean ok, String what) {
		if (!ok) {
			throw new AssertionError("Failed: " + what);
		}
	}
}
