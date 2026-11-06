package de.slyrex.auktion;

import de.slyrex.auktion.gui.AuctionEndScreen;
import de.slyrex.auktion.gui.AuctionHud;
import de.slyrex.auktion.gui.AuctionOverviewScreen;
import de.slyrex.auktion.gui.AuctionSetupScreen;
import de.slyrex.auktion.gui.WinAnimation;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AuktionsMod {
	public static final String MOD_ID = de.slyrex.Slyrex.MOD_ID;
	public static final Logger LOGGER = LoggerFactory.getLogger("Slyrex/Auktion");

	private static KeyBinding openKey;

	private AuktionsMod() {}

	public static boolean enabled() {
		return de.slyrex.SlyrexConfig.get().isEnabled("auktion");
	}

	public static void init(KeyBinding.Category category) {
		AuctionConfig.load();

		openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.slyrex.auktion", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_O, category));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openKey.wasPressed()) if (enabled()) openMenu(client);
			AuctionManager m = AuctionManager.get();
			m.tick(client);
			// Nach der Gewinner-Animation den "Auktion beendet"-Bildschirm zeigen
			if (m.endScreenPending() && !WinAnimation.active()
					&& (client.currentScreen == null || client.currentScreen instanceof AuctionOverviewScreen)) {
				m.consumeEndScreen();
				if (AuctionConfig.get().openEndScreen && client.player != null) client.setScreen(new AuctionEndScreen());
			}
		});

		// Nur SYSTEM-Nachrichten des Servers koennen echte Zahlungen sein.
		ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> !enabled() || AuctionManager.get().onGameMessage(message, overlay));

		// Signierte Spieler-Chatnachrichten werden NIE als Zahlung gezaehlt, nur als Fake gemeldet.
		ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) ->
				{ if (enabled()) AuctionManager.get().onPlayerChat(message); });

		HudElementRegistry.addLast(Identifier.of(MOD_ID, "auction_hud"), (ctx, tc) -> { if (enabled()) AuctionHud.render(ctx, tc); });
		HudElementRegistry.addLast(Identifier.of(MOD_ID, "win_animation"), WinAnimation::renderHud);

		
	}

	public static void openMenu(MinecraftClient client) {
		if (client.player == null) return;
		AuctionManager m = AuctionManager.get();
		if (m.hasSession()) {
			boolean over = m.phase() == AuctionManager.Phase.ENDED || m.phase() == AuctionManager.Phase.CANCELLED;
			client.setScreen(over ? new AuctionEndScreen() : new AuctionOverviewScreen());
			return;
		}
		ItemStack held = client.player.getMainHandStack();
		if (held.isEmpty()) {
			AuctionManager.info(Text.literal("Nimm das Item, das du versteigern willst, in die Hand und drücke die Taste erneut.")
					.formatted(Formatting.YELLOW));
			return;
		}
		client.setScreen(new AuctionSetupScreen(held));
	}
}
