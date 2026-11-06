package de.slyrex.hud;

import de.slyrex.SlyrexConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/** Verwaltet alle verschiebbaren HUD-Elemente (Position wird im HUD-Editor gesetzt). */
public final class SlyrexHud {
	private SlyrexHud() {}

	public interface Renderer {
		/** Zeichnet das Element ab (x,y). preview = im Editor (Beispieldaten zeigen). */
		void render(DrawContext ctx, int x, int y, boolean preview);
	}

	public static final class Element {
		public final String id;
		public final String name;
		private final float defX, defY;
		private final BooleanSupplier visible;
		private final java.util.function.IntSupplier width, height;
		private final Renderer renderer;

		Element(String id, String name, float defX, float defY, BooleanSupplier visible,
				java.util.function.IntSupplier width, java.util.function.IntSupplier height, Renderer renderer) {
			this.id = id;
			this.name = name;
			this.defX = defX;
			this.defY = defY;
			this.visible = visible;
			this.width = width;
			this.height = height;
			this.renderer = renderer;
		}

		public boolean visible() {
			return visible.getAsBoolean();
		}

		public int width() {
			return Math.max(10, width.getAsInt());
		}

		public int height() {
			return Math.max(8, height.getAsInt());
		}

		/** Position, auf den Bildschirm begrenzt. */
		public int[] pos() {
			MinecraftClient mc = MinecraftClient.getInstance();
			int sw = mc.getWindow().getScaledWidth(), sh = mc.getWindow().getScaledHeight();
			int[] p = SlyrexConfig.get().hudPos(id, Math.round(sw * defX), Math.round(sh * defY));
			int x = Math.max(0, Math.min(sw - width(), p[0]));
			int y = Math.max(0, Math.min(sh - height(), p[1]));
			return new int[]{x, y};
		}

		public void setPos(int x, int y) {
			SlyrexConfig.get().hud.put(id, new int[]{x, y});
		}

		public void resetPos() {
			SlyrexConfig.get().hud.remove(id);
		}

		public void render(DrawContext ctx, boolean preview) {
			int[] p = pos();
			renderer.render(ctx, p[0], p[1], preview);
		}
	}

	private static final List<Element> ELEMENTS = new ArrayList<>();

	/**
	 * @param defX Standardposition als Anteil der Bildschirmbreite (0..1)
	 * @param defY Standardposition als Anteil der Bildschirmhoehe (0..1)
	 */
	public static Element register(String id, String name, float defX, float defY, BooleanSupplier visible,
			java.util.function.IntSupplier width, java.util.function.IntSupplier height, Renderer renderer) {
		Element e = new Element(id, name, defX, defY, visible, width, height, renderer);
		ELEMENTS.add(e);
		return e;
	}

	public static List<Element> all() {
		return ELEMENTS;
	}

	/** Wird von der HUD-Ebene aufgerufen. */
	public static void renderAll(DrawContext ctx) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.options.hudHidden || mc.player == null) return;
		if (mc.currentScreen instanceof HudEditorScreen) return;
		for (Element e : ELEMENTS) {
			if (e.visible()) e.render(ctx, false);
		}
	}
}
