package de.slyrex.module;

import de.slyrex.SlyrexConfig;
import de.slyrex.auktion.Theme;
import de.slyrex.gui.OptionsScreen;
import de.slyrex.hud.HudEditorScreen;
import de.slyrex.hud.SlyrexHud;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.regex.Pattern;

/** Versteckt Koordinaten und andere Hinweise auf deinen Standort. */
public final class StreamerMode {
	public static final String ID = "streamer";
	/** Drei Zahlen hintereinander (x y z), z.B. "120 64 -300" oder "x: 120, y: 64, z: -300" */
	private static final Pattern COORDS = Pattern.compile(
			"(?i)(x\\s*[:=]?\\s*)?-?\\d{1,7}(\\.\\d+)?([\\s,/|]+)(y\\s*[:=]?\\s*)?-?\\d{1,3}(\\.\\d+)?([\\s,/|]+)(z\\s*[:=]?\\s*)?-?\\d{1,7}(\\.\\d+)?");

	private StreamerMode() {}

	public static boolean enabled() {
		return SlyrexConfig.get().isEnabled(ID);
	}

	private static SlyrexConfig.Streamer c() {
		return SlyrexConfig.get().streamer;
	}

	public static void init() {
		// Streamer-Modus ist standardmaessig aus
		if (!SlyrexConfig.get().modules.containsKey(ID)) SlyrexConfig.get().modules.put(ID, false);
		SlyrexHud.register("streamer", "Streamer-Anzeige", 0.5f, 0.0f, () -> enabled() && c().indicator,
				() -> MinecraftClient.getInstance().textRenderer.getWidth("● STREAMER-MODUS") + 8, () -> 12,
				(ctx, x, y, preview) -> {
					var tr = MinecraftClient.getInstance().textRenderer;
					ctx.fill(x, y, x + tr.getWidth("● STREAMER-MODUS") + 8, y + 12, Theme.background());
					float pulse = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 300.0);
					int red = 0xFF000000 | ((int) (150 + 105 * pulse) << 16) | 0x2020;
					ctx.drawTextWithShadow(tr, "●", x + 4, y + 2, red);
					ctx.drawTextWithShadow(tr, " STREAMER-MODUS", x + 4 + tr.getWidth("●"), y + 2, Theme.text());
				});
	}

	/** Fuer den Mixin: F3-Koordinaten verstecken (wie "reduzierte Debug-Infos"). */
	public static boolean hideDebug() {
		return enabled() && c().hideDebugInfo;
	}

	/** Koordinaten in eingehenden Chat-Nachrichten unkenntlich machen. */
	public static Text filterMessage(Text message) {
		if (!enabled() || !c().hideChatCoords) return message;
		String s = message.getString();
		if (!COORDS.matcher(s).find()) return message;
		return Text.literal(COORDS.matcher(s).replaceAll("§k###§r §7[versteckt]§r"));
	}

	public static Screen settings(Screen parent) {
		SlyrexConfig.Streamer c = c();
		OptionsScreen[] self = new OptionsScreen[1];
		self[0] = new OptionsScreen(parent, "Streamer-Modus", null)
				.toggle("F3-Koordinaten verstecken", "Zeigt im F3-Menü nur reduzierte Infos (keine Position, Chunk, Blickziel)",
						() -> c.hideDebugInfo, v -> c.hideDebugInfo = v)
				.toggle("Koordinaten im Chat", "Ersetzt Koordinaten (x y z) in Chat-Nachrichten",
						() -> c.hideChatCoords, v -> c.hideChatCoords = v)
				.toggle("Anzeige im HUD", "Kleiner roter Hinweis, dass der Modus aktiv ist",
						() -> c.indicator, v -> c.indicator = v)
				.footer("HUD-Editor", () -> MinecraftClient.getInstance().setScreen(new HudEditorScreen(self[0])));
		return self[0];
	}
}
