package de.slyrex;

import de.slyrex.auktion.AuktionsMod;
import de.slyrex.begleiter.BegleiterClient;
import de.slyrex.gui.HubScreen;
import de.slyrex.hud.SlyrexHud;
import de.slyrex.module.Adblocker;
import de.slyrex.module.Calculator;
import de.slyrex.module.Crosshair;
import de.slyrex.module.DuraGuard;
import de.slyrex.module.FakeCheck;
import de.slyrex.module.FightPresets;
import de.slyrex.module.InvseeCounter;
import de.slyrex.module.RenameStudio;
import de.slyrex.module.SecondChat;
import de.slyrex.module.SoundMuffler;
import de.slyrex.module.StreamerMode;
import de.slyrex.module.TotemCounter;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class Slyrex implements ClientModInitializer {
	public static final String MOD_ID = "slyrex";
	public static final Logger LOGGER = LoggerFactory.getLogger("Slyrex");

	private static final List<Module> MODULES = new ArrayList<>();
	private static KeyBinding hubKey, calcKey, renameKey;

	public static List<Module> modules() {
		return MODULES;
	}

	public static String version() {
		return FabricLoader.getInstance().getModContainer(MOD_ID)
				.map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("?");
	}

	@Override
	public void onInitializeClient() {
		SlyrexConfig.load();
		KeyBinding.Category category = KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));

		hubKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.slyrex.menu", InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_RIGHT_SHIFT, category));
		calcKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.slyrex.calculator", InputUtil.Type.KEYSYM,
				InputUtil.UNKNOWN_KEY.getCode(), category));
		renameKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.slyrex.rename", InputUtil.Type.KEYSYM,
				InputUtil.UNKNOWN_KEY.getCode(), category));

		// Deine Mods – zuerst, damit die Auktion jede Zahlungs-Nachricht sieht
		AuktionsMod.init(category);
		BegleiterClient.init(category);

		TotemCounter.init();
		StreamerMode.init();
		SecondChat.init();
		DuraGuard.init();
		Crosshair.init();
		InvseeCounter.init();
		Calculator.init();
		FightPresets.init();

		registerModules();
		registerEvents();
		HudElementRegistry.addLast(Identifier.of(MOD_ID, "hud"), (ctx, tc) -> SlyrexHud.renderAll(ctx));
		SlyrexConfig.save();
		LOGGER.info("Slyrex {} geladen – Rechte Shift-Taste öffnet das Menü", version());
	}

	private static void registerModules() {
		MODULES.add(Module.custom("auktion", "Auktionen", "Item in der Hand versteigern, /pay-Gebote werden erkannt (Taste O)",
				Items.GOLD_INGOT, AuktionsMod::enabled, on -> SlyrexConfig.get().setEnabled("auktion", on),
				de.slyrex.auktion.gui.DesignScreen::new));
		MODULES.add(Module.custom("begleiter", "Begleiter", "Ein Begleiter, der dir folgt und mit dir redet (K / G)",
				Items.PLAYER_HEAD, () -> de.slyrex.begleiter.Config.get().enabled,
				on -> { de.slyrex.begleiter.Config.get().enabled = on; de.slyrex.begleiter.Config.save(); },
				de.slyrex.begleiter.screen.SettingsScreen::new));
		MODULES.add(Module.of(TotemCounter.ID, "Totem-Zähler", "Totems im Inventar und Totem-Pops anderer Spieler",
				Items.TOTEM_OF_UNDYING, TotemCounter::settings));
		MODULES.add(Module.of(FightPresets.ID, "Fight-Presets", "Inventar-Layouts speichern, Geister-Items und fehlende Items",
				Items.NETHERITE_SWORD, FightPresets::settings));
		MODULES.add(Module.of(InvseeCounter.ID, "Invsee-Zähler", "Zählt Totems, Kristalle usw. in fremden Inventaren",
				Items.ENDER_EYE, InvseeCounter::settings));
		MODULES.add(Module.of(SecondChat.ID, "Zweiter Chat", "Bis zu 3 extra Chat-Boxen mit eigenen Filtern",
				Items.WRITABLE_BOOK, SecondChat::settings));
		MODULES.add(Module.of(Adblocker.ID, "Adblocker", "Blendet Werbung, Handel und TP-Spam im Chat aus",
				Items.BARRIER, Adblocker::settings));
		MODULES.add(Module.of(FakeCheck.ID, "Fake-Check", "Warnt vor Namen/Clan-Tags, die bekannten ähneln",
				Items.SPYGLASS, FakeCheck::settings));
		MODULES.add(Module.of(StreamerMode.ID, "Streamer-Modus", "Versteckt Koordinaten im F3-Menü und im Chat",
				Items.ENDER_PEARL, StreamerMode::settings));
		MODULES.add(Module.of(Crosshair.ID, "Fadenkreuz", "Eigene Farben, Verlauf, Ziel- und Treffer-Farbe",
				Items.TARGET, Crosshair::settings));
		MODULES.add(Module.of(DuraGuard.ID, "DuraGuard", "Warnt bei wenig Haltbarkeit, schützt Werkzeuge",
				Items.ANVIL, DuraGuard::settings));
		MODULES.add(Module.of(SoundMuffler.ID, "Sound-Muffler", "Lautstärke einzelner Sounds ändern oder stummschalten",
				Items.NOTE_BLOCK, SoundMuffler::settings));
		MODULES.add(Module.of(RenameStudio.ID, "Rename-Studio", "Farbverläufe für Item-Namen mit Vorschau",
				Items.NAME_TAG, RenameStudio::settings));
		MODULES.add(Module.of(Calculator.ID, "Rechner", "Taschenrechner mit k/m-Kürzeln, auch im Inventar",
				Items.COMPARATOR, Calculator::settings));
	}

	private static void registerEvents() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (hubKey.wasPressed()) if (client.currentScreen == null) client.setScreen(new HubScreen());
			while (calcKey.wasPressed()) if (client.currentScreen == null) client.setScreen(new Calculator.CalcScreen(null));
			while (renameKey.wasPressed()) if (client.currentScreen == null) client.setScreen(RenameStudio.settings(null));
			TotemCounter.tick();
			DuraGuard.tick(client);
			FakeCheck.tick(client);
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> TotemCounter.reset());

		// Systemnachrichten (Server)
		ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
			if (overlay) return true;
			if (Adblocker.shouldBlock(message.getString(), false)) return false;
			return !SecondChat.onMessage(StreamerMode.filterMessage(message));
		});
		ClientReceiveMessageEvents.MODIFY_GAME.register((message, overlay) -> overlay ? message : StreamerMode.filterMessage(message));

		// Signierter Spieler-Chat
		ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signed, sender, params, ts) -> {
			if (sender != null) FakeCheck.onChat(sender.name());
			if (Adblocker.shouldBlock(message.getString(), true)) return false;
			Text filtered = StreamerMode.filterMessage(message);
			if (SecondChat.onMessage(filtered)) return false;
			if (filtered != message) {
				MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(filtered);
				return false;
			}
			return true;
		});

		ClientSendMessageEvents.ALLOW_COMMAND.register(FakeCheck::allowCommand);

		// Slyrex-Knopf im Pausemenü
		ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
			if (screen instanceof GameMenuScreen gm && gm.shouldShowMenu()) {
				Screens.getButtons(screen).add(ButtonWidget.builder(Text.literal("§6§lSlyrex"), b -> client.setScreen(new HubScreen()))
						.tooltip(Tooltip.of(Text.literal("Slyrex-Menü (Rechte Shift-Taste)"))).dimensions(4, 4, 60, 20).build());
			}
		});
	}
}
