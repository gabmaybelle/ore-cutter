package com.gabbymaybelle.orecutter.gametest;

import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.inventory.StonecutterScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Takes the pictures for the mod page at 1920x1080: a small workstation (crafting table, barrel,
 * stonecutter) with one emerald ore block beside it (before), the stonecutter screen offering the
 * deepslate twin, and the same workstation with the block swapped (after).
 * Only runs with `./gradlew :fabric:runClientGameTest -Pscreenshots`; the pictures land in
 * fabric/build/run/clientGameTest/screenshots.
 */
public class Screenshots implements FabricClientGameTest {
	static boolean enabled() {
		return Boolean.getBoolean("orecutter.screenshots");
	}

	@Override
	public void runTest(ClientGameTestContext ctx) {
		if (!enabled()) {
			return;
		}
		ctx.getInput().resizeWindow(1920, 1080);
		ctx.runOnClient(mc -> {
			mc.options.guiScale().set(3);
			mc.resizeGui();
		});
		try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			TestServerContext server = world.getServer();
			// A little cellar high above the ground, open to the noon sun so it needs no lamps to be seen.
			for (String command : List.of(
					"gamemode survival @a", "time set noon", "weather clear", "gamerule spawn_mobs false",
					"forceload add -16 -16 16 16",
					"fill -5 199 -3 5 199 7 minecraft:spruce_planks",
					"fill -5 200 7 5 206 7 minecraft:deepslate_bricks",
					"fill -5 200 -3 -5 206 6 minecraft:deepslate_bricks",
					"fill 5 200 -3 5 206 6 minecraft:deepslate_bricks",
					"fill -5 200 -3 5 206 -3 minecraft:deepslate_bricks",
					// The workstation, in a row along the back wall, and one ore block beside it.
					"setblock -2 200 6 minecraft:crafting_table", "setblock -1 200 6 minecraft:barrel[facing=up]",
					"setblock 0 200 6 minecraft:stonecutter[facing=north]", "setblock 2 200 6 minecraft:emerald_ore",
					"tp @a 0.5 200 2.5 0 14", "clear @a")) {
				server.runCommand(command);
			}
			ctx.waitTicks(40);
			world.getConnection().waitForChunksRender();
			scene(ctx, "1_before_emerald_ore");

			// The stonecutter screen: emerald ore in, the deepslate twin picked.
			BlockPos cutter = new BlockPos(0, 200, 6);
			server.runCommand("tp @a 0.5 200 4.0 0 35");
			ctx.waitTicks(10);
			ctx.runOnClient(mc -> {
				if (mc.gui.hud.isHidden()) {
					mc.gui.hud.toggle();
				}
				mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(cutter), Direction.UP, cutter, false));
			});
			ctx.waitForScreen(StonecutterScreen.class);
			StonecutterTest.putInInput(ctx, server, Items.EMERALD_ORE);
			server.runCommand("give @a minecraft:deepslate_emerald_ore 6");
			server.runCommand("give @a minecraft:diamond_ore 3");
			server.runCommand("give @a minecraft:iron_ore 9");
			ctx.waitTicks(5);
			List<Identifier> offered = ctx.computeOnClient(StonecutterTest::offered);
			StonecutterTest.select(ctx, offered.indexOf(Identifier.withDefaultNamespace("deepslate_emerald_ore")));
			ctx.waitTicks(10);
			// No chat, no "new recipe" toast, and an empty hand in the corner.
			ctx.runOnClient(mc -> {
				mc.gui.hud.getChat().clearMessages(false);
				mc.gui.toastManager().clear();
				mc.player.getInventory().setSelectedSlot(8);
			});
			ctx.waitTicks(10);
			ctx.takeScreenshot("2_stonecutter_emerald_ore");
			ctx.runOnClient(mc -> mc.player.closeContainer());
			ctx.waitTicks(5);

			// The crafting table: a block of emerald and a deepslate make a deepslate emerald ore.
			server.runCommand("tp @a -1.5 200 4.0 0 35");
			ctx.waitTicks(10);
			CraftingTest.openTable(ctx, server, new BlockPos(-2, 200, 6));
			CraftingTest.fill(ctx, server, CraftingTest.item("emerald_block"));
			server.runCommand("give @a minecraft:emerald_block 3");
			server.runCommand("give @a minecraft:deepslate 16");
			server.runCommand("give @a minecraft:diamond_block 2");
			ctx.waitTicks(10);
			ctx.runOnClient(mc -> {
				mc.gui.hud.getChat().clearMessages(false);
				mc.gui.toastManager().clear();
				mc.player.getInventory().setSelectedSlot(8);
			});
			ctx.waitTicks(10);
			ctx.takeScreenshot("4_crafting_deepslate_emerald_ore");
			ctx.runOnClient(mc -> mc.player.closeContainer());
			ctx.waitTicks(5);

			// The same workstation with the ore swapped for its twin.
			server.runCommand("setblock 2 200 6 minecraft:deepslate_emerald_ore");
			server.runCommand("clear @a");
			server.runCommand("tp @a 0.5 200 2.5 0 14");
			ctx.waitTicks(40);
			world.getConnection().waitForChunksRender();
			scene(ctx, "3_after_deepslate_emerald_ore");
		}
	}

	/** A picture of the world alone: no hotbar, hand, chat or crosshair. */
	private static void scene(ClientGameTestContext ctx, String name) {
		ctx.runOnClient(mc -> {
			if (!mc.gui.hud.isHidden()) {
				mc.gui.hud.toggle();
			}
			mc.gui.hud.getChat().clearMessages(false);
		});
		ctx.waitTicks(10);
		ctx.takeScreenshot(name);
	}
}
