package de.slyrex.module;

import de.slyrex.SlyrexConfig;
import de.slyrex.auktion.Theme;
import de.slyrex.gui.OptionsScreen;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.HitResult;

import java.util.List;

/** Eigenes Fadenkreuz mit Farbpalette, Verlauf, Farbe bei Ziel in Reichweite und Treffer-Feedback. */
public final class Crosshair {
	public static final String ID = "crosshair";
	private static long lastHit;

	private Crosshair() {}

	private static SlyrexConfig.Crosshair c() {
		return SlyrexConfig.get().crosshair;
	}

	public static boolean enabled() {
		return SlyrexConfig.get().isEnabled(ID);
	}

	public static void init() {
		if (!SlyrexConfig.get().modules.containsKey(ID)) SlyrexConfig.get().modules.put(ID, false);
		HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, vanilla -> (ctx, tc) -> {
			MinecraftClient mc = MinecraftClient.getInstance();
			if (!enabled() || mc.getDebugHud().shouldShowDebugHud() || mc.player == null || mc.player.isSpectator()
					|| !mc.options.getPerspective().isFirstPerson()) {
				vanilla.render(ctx, tc);
				return;
			}
			draw(ctx, ctx.getScaledWindowWidth() / 2, ctx.getScaledWindowHeight() / 2, false);
		});
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
			if (world.isClient()) lastHit = System.currentTimeMillis();
			return ActionResult.PASS;
		});
	}

	private static int[] palette() {
		List<String> list = SlyrexConfig.split(c().colors);
		if (list.isEmpty()) return new int[]{0xFFFFFF};
		int[] out = new int[Math.min(5, list.size())];
		for (int i = 0; i < out.length; i++) out[i] = Theme.parse(list.get(i), 0xFFFFFF);
		return out;
	}

	/** Farbe an Position t (0..1) entlang des Fadenkreuzes. */
	private static int colorAt(int[] pal, float t, boolean preview) {
		SlyrexConfig.Crosshair c = c();
		if (!preview && c.hitColorEnabled && System.currentTimeMillis() - lastHit < 250) return Theme.parse(c.hitColor, 0xFFAA00);
		if (!preview && c.reachColorEnabled) {
			MinecraftClient mc = MinecraftClient.getInstance();
			if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY) {
				return Theme.parse(c.reachColor, 0xFF5555);
			}
		}
		if (pal.length == 1) return pal[0];
		switch (c.mode) {
			case "GRADIENT" -> {
				float p = t * (pal.length - 1);
				int i = Math.min(pal.length - 2, (int) p);
				return Theme.mix(pal[i], pal[i + 1], p - i);
			}
			case "CYCLE" -> {
				float p = (System.currentTimeMillis() % 4000) / 4000f * pal.length;
				int i = (int) p % pal.length;
				return Theme.mix(pal[i], pal[(i + 1) % pal.length], p - (int) p);
			}
			default -> {
				return pal[0];
			}
		}
	}

	public static void draw(DrawContext ctx, int cx, int cy, boolean preview) {
		SlyrexConfig.Crosshair c = c();
		int[] pal = palette();
		int t = Math.max(1, c.thickness), len = Math.max(1, c.size), gap = Math.max(0, c.gap);
		int half = t / 2;
		int total = gap + len;
		if (c.outline) {
			int o = 0xA0000000;
			ctx.fill(cx - half - 1, cy - total - 1, cx - half + t + 1, cy - gap + 1, o);
			ctx.fill(cx - half - 1, cy + gap - 1 + (t % 2 == 0 ? 0 : 1), cx - half + t + 1, cy + total + 1 + (t % 2 == 0 ? 0 : 1), o);
			ctx.fill(cx - total - 1, cy - half - 1, cx - gap + 1, cy - half + t + 1, o);
			ctx.fill(cx + gap - 1 + (t % 2 == 0 ? 0 : 1), cy - half - 1, cx + total + 1 + (t % 2 == 0 ? 0 : 1), cy - half + t + 1, o);
		}
		int off = t % 2 == 0 ? 0 : 1;
		for (int i = 0; i < len; i++) {
			float f = len == 1 ? 0 : i / (float) (len - 1);
			int col = 0xFF000000 | colorAt(pal, 1 - f, preview);
			int d = gap + i;
			ctx.fill(cx - half, cy - d - 1, cx - half + t, cy - d, col);          // oben
			ctx.fill(cx - half, cy + d + off, cx - half + t, cy + d + off + 1, col); // unten
			ctx.fill(cx - d - 1, cy - half, cx - d, cy - half + t, col);          // links
			ctx.fill(cx + d + off, cy - half, cx + d + off + 1, cy - half + t, col); // rechts
		}
		if (c.dot) ctx.fill(cx - half, cy - half, cx - half + t, cy - half + t, 0xFF000000 | colorAt(pal, 0, preview));
	}

	public static Screen settings(Screen parent) {
		SlyrexConfig.Crosshair c = c();
		return new OptionsScreen(parent, "Fadenkreuz", null) {
			@Override
			protected int labelWidth() {
				return W - 170 - 54;
			}

			@Override
			protected void renderExtra(DrawContext ctx, int mx, int my, int x0, int y0, int w, int h) {
				// Vorschau zwischen Beschriftung und Knoepfen
				int px = x0 + w - 8 - 150 - 52, py = y0 + 26;
				ctx.fill(px, py, px + 46, py + 46, 0xC0303840);
				draw(ctx, px + 23, py + 23, true);
			}
		}
				.field("Farben", "1–5 Hex-Farben, kommagetrennt, z.B. #FF0000,#FFFF00,#00FF00", 64, () -> c.colors, v -> c.colors = v)
				.choice("Modus", "Bei mehreren Farben: Verlauf entlang der Linien oder Farbwechsel",
						new String[]{"SOLID", "GRADIENT", "CYCLE"}, new String[]{"Einfarbig", "Verlauf", "Farbwechsel"},
						() -> c.mode, v -> c.mode = v)
				.cycle("Länge", null, OptionsScreenRange.of(1, 12), () -> c.size, v -> c.size = v, v -> v + " px")
				.cycle("Abstand", null, OptionsScreenRange.of(0, 8), () -> c.gap, v -> c.gap = v, v -> v + " px")
				.cycle("Dicke", null, OptionsScreenRange.of(1, 4), () -> c.thickness, v -> c.thickness = v, v -> v + " px")
				.toggle("Punkt in der Mitte", null, () -> c.dot, v -> c.dot = v)
				.toggle("Schwarzer Rand", null, () -> c.outline, v -> c.outline = v)
				.toggle("Farbe bei Ziel", "Andere Farbe, wenn ein Spieler/Mob in Reichweite anvisiert ist",
						() -> c.reachColorEnabled, v -> c.reachColorEnabled = v)
				.field("Ziel-Farbe", null, 7, () -> c.reachColor, v -> c.reachColor = v)
				.toggle("Treffer-Farbe", "Kurz andere Farbe nach einem Schlag", () -> c.hitColorEnabled, v -> c.hitColorEnabled = v)
				.field("Treffer-Farbe", null, 7, () -> c.hitColor, v -> c.hitColor = v);
	}

	/** Kleine Hilfe fuer Zahlenbereiche. */
	static final class OptionsScreenRange {
		static int[] of(int from, int to) {
			int[] out = new int[to - from + 1];
			for (int i = 0; i < out.length; i++) out[i] = from + i;
			return out;
		}
	}
}
