package com.gabbymaybelle.orecutter.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Ore Cutter's crafting recipes through a real crafting table. For each of the eight ores: a block
 * of its material and a deepslate go in the grid, the result slot must show one deepslate ore, the
 * test takes it and checks both ingredients were used up. Then a single piece of the material with
 * a deepslate must make nothing, so the price stays a whole block.
 */
public class CraftingTest implements FabricClientGameTest {
	/** Ore -> the block it is crafted from, and the single piece that must not be enough. */
	static final Map<String, String[]> CRAFTED_FROM = Map.of(
			"coal", new String[] { "coal_block", "coal" }, "copper", new String[] { "raw_copper_block", "raw_copper" },
			"iron", new String[] { "raw_iron_block", "raw_iron" }, "gold", new String[] { "raw_gold_block", "raw_gold" },
			"redstone", new String[] { "redstone_block", "redstone" }, "emerald", new String[] { "emerald_block", "emerald" },
			"lapis", new String[] { "lapis_block", "lapis_lazuli" }, "diamond", new String[] { "diamond_block", "diamond" });
	/** The crafting table menu's slots: the result, then the grid from its top left. */
	private static final int RESULT = 0, GRID = 1;

	@Override
	public void runTest(ClientGameTestContext ctx) {
		List<String> failures = new ArrayList<>();
		try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			TestServerContext server = world.getServer();
			server.runCommand("gamemode survival @a");
			BlockPos table = server.computeOnServer(s -> player(s).blockPosition().offset(2, 0, 0));
			openTable(ctx, server, table);

			for (Map.Entry<String, String[]> entry : CRAFTED_FROM.entrySet()) {
				Item ore = item("deepslate_" + entry.getKey() + "_ore");
				String block = entry.getValue()[0], piece = entry.getValue()[1];
				// A whole block and a deepslate: one deepslate ore.
				fill(ctx, server, item(block));
				ItemStack result = result(ctx);
				if (!result.is(ore) || result.getCount() != 1) {
					failures.add(block + " and deepslate give " + result.getCount() + " " + BuiltInRegistries.ITEM.getKey(result.getItem()));
					continue;
				}
				ctx.runOnClient(mc -> mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, RESULT, 0, ContainerInput.PICKUP, mc.player));
				ctx.waitTicks(3);
				int carried = server.computeOnServer(s -> player(s).containerMenu.getCarried().is(ore) ? player(s).containerMenu.getCarried().getCount() : 0);
				int left = server.computeOnServer(s -> player(s).containerMenu.getSlot(GRID).getItem().getCount() + player(s).containerMenu.getSlot(GRID + 1).getItem().getCount());
				if (carried != 1 || left != 0) {
					failures.add(block + ": taking the ore should give 1 and use both ingredients, but gave " + carried + " and left " + left);
				}
				// A single piece and a deepslate: not this ore.
				fill(ctx, server, item(piece));
				if (result(ctx).is(ore)) {
					failures.add("a single " + piece + " and deepslate is enough for deepslate_" + entry.getKey() + "_ore");
				}
			}
		}
		System.out.println("[orecutter_gametest] crafting: " + CRAFTED_FROM.size() + " recipes checked, " + failures.size() + " problem(s)");
		if (!failures.isEmpty()) {
			throw new AssertionError("Failed: crafting:\n  " + String.join("\n  ", failures));
		}
	}

	/** Places a crafting table and opens it as the player would. */
	static void openTable(ClientGameTestContext ctx, TestServerContext server, BlockPos table) {
		server.runCommand("setblock " + table.getX() + " " + table.getY() + " " + table.getZ() + " minecraft:crafting_table");
		ctx.waitTicks(2);
		ctx.runOnClient(mc -> mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(table), Direction.UP, table, false)));
		ctx.waitForScreen(CraftingScreen.class);
	}

	/** Empties the player's hands, inventory and grid, then puts this item and a deepslate in the grid. */
	static void fill(ClientGameTestContext ctx, TestServerContext server, Item with) {
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			player.getInventory().clearContent();
			CraftingMenu menu = (CraftingMenu) player.containerMenu;
			menu.setCarried(ItemStack.EMPTY);
			for (int slot = GRID; slot < GRID + 9; slot++) {
				menu.getSlot(slot).set(ItemStack.EMPTY);
			}
			menu.getSlot(GRID).set(new ItemStack(with));
			menu.getSlot(GRID + 1).set(new ItemStack(item("deepslate")));
			menu.slotsChanged(player.getInventory());
			menu.broadcastChanges();
		});
		// The server works the result out and sends it over.
		ctx.waitTicks(6);
	}

	/** What the crafting table's result slot shows the player. */
	private static ItemStack result(ClientGameTestContext ctx) {
		return ctx.computeOnClient(mc -> mc.player.containerMenu.getSlot(RESULT).getItem().copy());
	}

	static Item item(String name) {
		Identifier id = Identifier.withDefaultNamespace(name);
		if (!BuiltInRegistries.ITEM.containsKey(id)) {
			throw new AssertionError("Failed: the item " + id + " exists");
		}
		return BuiltInRegistries.ITEM.getValue(id);
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}
}
