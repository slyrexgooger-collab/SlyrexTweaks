package de.slyrex.test;

import de.slyrex.Module;
import de.slyrex.Slyrex;
import de.slyrex.SlyrexConfig;
import de.slyrex.auktion.gui.AuctionSetupScreen;
import de.slyrex.gui.HubScreen;
import de.slyrex.hud.HudEditorScreen;
import de.slyrex.module.Adblocker;
import de.slyrex.module.Calculator;
import de.slyrex.module.FightPresets;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;

public class SlyrexClientTest implements FabricClientGameTest {
	private static void check(boolean ok, String what) {
		if (!ok) throw new AssertionError(what);
	}

	@Override
	public void runTest(ClientGameTestContext ctx) {
		// Reine Logik
		check(Math.abs(Calculator.eval("1,5m - 250k * 2") - 1_000_000) < 1e-6, "Rechner k/m");
		check(Math.abs(Calculator.eval("(2+3)^2") - 25) < 1e-9, "Rechner Klammern/Potenz");
		check(Math.abs(Calculator.eval("1.500.000 / 3") - 500_000) < 1e-6, "Rechner Tausenderpunkte");
		check(Calculator.format(1234567.5).equals("1.234.567,5"), "Rechner Format: " + Calculator.format(1234567.5));
		check(Adblocker.shouldBlock("Bob: verkaufe 64 Diamanten günstig", false), "Adblock Handel");
		check(!Adblocker.shouldBlock("Bob möchte sich zu dir teleportieren. /tpaccept", false), "Adblock echte TPA");
		check(!Adblocker.shouldBlock("[Admin] Steve: twitch.tv/abc", false), "Adblock Ausnahme");
		SlyrexConfig.get().fakeCheck.trustedNames = "Slyrex,Notch";
		check("Slyrex".equals(de.slyrex.module.FakeCheck.suspiciousName("S1yrex")), "FakeCheck S1yrex");
		check(de.slyrex.module.FakeCheck.suspiciousName("Slyrex") == null, "FakeCheck echter Name");
		check(de.slyrex.module.FakeCheck.suspiciousName("Herobrine") == null, "FakeCheck fremder Name");

		// Alle Module an, damit HUD-Elemente sichtbar sind
		ctx.runOnClient(mc -> {
			for (Module m : Slyrex.modules()) if (!m.enabled()) m.toggle();
		});

		try (TestSingleplayerContext sp = ctx.worldBuilder().create()) {
			sp.getClientWorld().waitForChunksRender();
			sp.getServer().runCommand("time set day");
			sp.getServer().runCommand("give @a totem_of_undying 3");
			sp.getServer().runCommand("give @a end_crystal 64");
			sp.getServer().runCommand("give @a netherite_sword[damage=1990]");
			sp.getServer().runCommand("item replace entity @a hotbar.0 with netherite_sword[damage=1990]");
			ctx.waitTicks(40);
			sp.getServer().runCommand("tellraw @a \"[msg] Max -> mir: Hallo, kommst du?\"");
			sp.getServer().runCommand("tellraw @a \"Bob: verkaufe 64 Diamanten günstig /ah\"");
			sp.getServer().runCommand("tellraw @a \"Server: Spawn ist bei 120 64 -300\"");
			ctx.waitTicks(10);
			ctx.takeScreenshot("01-ingame-hud");

			ctx.setScreen(HubScreen::new);
			ctx.waitTicks(15);
			ctx.takeScreenshot("02-hub");

			int i = 3;
			for (Module m : Slyrex.modules()) {
				if (!m.hasSettings()) continue;
				final Module mod = m;
				ctx.setScreen(() -> mod.settings(null));
				ctx.waitTicks(10);
				ctx.takeScreenshot(String.format("%02d-%s", i++, m.id));
			}

			ctx.setScreen(() -> new HudEditorScreen(null));
			ctx.waitTicks(5);
			ctx.takeScreenshot(String.format("%02d-hud-editor", i++));
			ctx.setScreen(() -> null);

			// Preset speichern, Inventar umsortieren -> Geister-Items
			ctx.runOnClient(mc -> {
				SlyrexConfig.get().presets.list.add(FightPresets.capture("Crystal"));
				SlyrexConfig.get().presets.active = "Crystal";
			});
			sp.getServer().runCommand("clear @a end_crystal");
			ctx.waitTicks(5);
			ctx.setScreen(() -> new InventoryScreen(net.minecraft.client.MinecraftClient.getInstance().player));
			ctx.waitTicks(10);
			ctx.takeScreenshot(String.format("%02d-inventory-preset", i++));

			ctx.setScreen(() -> new AuctionSetupScreen(net.minecraft.client.MinecraftClient.getInstance().player.getMainHandStack()));
			ctx.waitTicks(10);
			ctx.takeScreenshot(String.format("%02d-auction-setup", i++));
			ctx.setScreen(() -> null);
			ctx.waitTicks(5);
		}
	}
}
