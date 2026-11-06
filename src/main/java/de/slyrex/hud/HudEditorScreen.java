package de.slyrex.hud;

import de.slyrex.SlyrexConfig;
import de.slyrex.auktion.Theme;
import de.slyrex.auktion.gui.Ui;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** HUD-Editor: Elemente mit der Maus verschieben, mit Raster-, Rand- und Mitte-Einrasten. */
public class HudEditorScreen extends Screen {
	private static final int SNAP = 6;
	private final Screen parent;
	private SlyrexHud.Element dragging;
	private int grabX, grabY;
	private boolean grid = true;
	private int guideX = -1, guideY = -1;

	public HudEditorScreen(Screen parent) {
		super(Text.literal("HUD-Editor"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int cx = width / 2;
		addDrawableChild(ButtonWidget.builder(Text.literal("Raster: " + (grid ? "§aAN" : "§cAUS")), b -> {
			grid = !grid;
			b.setMessage(Text.literal("Raster: " + (grid ? "§aAN" : "§cAUS")));
		}).dimensions(cx - 154, height - 24, 100, 20).build());
		addDrawableChild(ButtonWidget.builder(Text.literal("Alles zurücksetzen"), b -> {
			for (SlyrexHud.Element e : SlyrexHud.all()) e.resetPos();
			SlyrexConfig.save();
		}).dimensions(cx - 50, height - 24, 100, 20).build());
		addDrawableChild(ButtonWidget.builder(Text.literal("§lFertig"), b -> close())
				.dimensions(cx + 54, height - 24, 100, 20).build());
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		ctx.fill(0, 0, width, height, 0x50000000);
		if (grid) {
			for (int x = 0; x < width; x += 20) ctx.fill(x, 0, x + 1, height, 0x14FFFFFF);
			for (int y = 0; y < height; y += 20) ctx.fill(0, y, width, y + 1, 0x14FFFFFF);
		}
		ctx.fill(width / 2, 0, width / 2 + 1, height, 0x30FFFFFF);
		ctx.fill(0, height / 2, width, height / 2 + 1, 0x30FFFFFF);
		if (dragging != null) {
			if (guideX >= 0) ctx.fill(guideX, 0, guideX + 1, height, Theme.accent());
			if (guideY >= 0) ctx.fill(0, guideY, width, guideY + 1, Theme.accent());
		}

		for (SlyrexHud.Element e : SlyrexHud.all()) {
			if (!e.visible()) continue;
			int[] p = e.pos();
			boolean hover = inside(e, mouseX, mouseY);
			ctx.fill(p[0] - 1, p[1] - 1, p[0] + e.width() + 1, p[1] + e.height() + 1, hover || e == dragging ? 0x40FFFFFF : 0x20FFFFFF);
			e.render(ctx, true);
			Ui.outline(ctx, p[0] - 1, p[1] - 1, p[0] + e.width() + 1, p[1] + e.height() + 1,
					e == dragging ? Theme.accent() : Theme.border());
			if (hover && dragging == null) {
				ctx.drawTooltip(textRenderer, Text.literal(e.name + " §7(Rechtsklick: zurücksetzen)"), mouseX, mouseY);
			}
		}
		Ui.textCenter(ctx, textRenderer, "§lHUD-Editor §7– Elemente mit der Maus ziehen, Rechtsklick = zurücksetzen", width / 2, height - 38, Theme.accent());
		boolean any = SlyrexHud.all().stream().anyMatch(SlyrexHud.Element::visible);
		if (!any) Ui.textCenter(ctx, textRenderer, "Keine HUD-Elemente aktiv – schalte Module im Slyrex-Menü an.",
				width / 2, height / 2 - 20, Theme.muted());
		super.render(ctx, mouseX, mouseY, delta);
	}

	@Override
	public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
		// eigener, durchsichtiger Hintergrund in render()
	}

	private static boolean inside(SlyrexHud.Element e, double mx, double my) {
		int[] p = e.pos();
		return mx >= p[0] && mx < p[0] + e.width() && my >= p[1] && my < p[1] + e.height();
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		if (super.mouseClicked(click, doubled)) return true;
		for (int i = SlyrexHud.all().size() - 1; i >= 0; i--) {
			SlyrexHud.Element e = SlyrexHud.all().get(i);
			if (!e.visible() || !inside(e, click.x(), click.y())) continue;
			if (click.button() == 1) {
				e.resetPos();
				SlyrexConfig.save();
				return true;
			}
			int[] p = e.pos();
			dragging = e;
			grabX = (int) click.x() - p[0];
			grabY = (int) click.y() - p[1];
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseDragged(Click click, double dx, double dy) {
		if (dragging == null) return super.mouseDragged(click, dx, dy);
		int w = dragging.width(), h = dragging.height();
		int x = (int) click.x() - grabX, y = (int) click.y() - grabY;
		guideX = -1;
		guideY = -1;
		// Raender und Mitte
		if (Math.abs(x) < SNAP) { x = 0; guideX = 0; }
		if (Math.abs(x + w - width) < SNAP) { x = width - w; guideX = width - 1; }
		if (Math.abs(x + w / 2 - width / 2) < SNAP) { x = width / 2 - w / 2; guideX = width / 2; }
		if (Math.abs(y) < SNAP) { y = 0; guideY = 0; }
		if (Math.abs(y + h - height) < SNAP) { y = height - h; guideY = height - 1; }
		if (Math.abs(y + h / 2 - height / 2) < SNAP) { y = height / 2 - h / 2; guideY = height / 2; }
		if (grid && guideX < 0) x = Math.round(x / 5f) * 5;
		if (grid && guideY < 0) y = Math.round(y / 5f) * 5;
		x = Math.max(0, Math.min(width - w, x));
		y = Math.max(0, Math.min(height - h, y));
		dragging.setPos(x, y);
		return true;
	}

	@Override
	public boolean mouseReleased(Click click) {
		if (dragging != null) {
			dragging = null;
			SlyrexConfig.save();
			return true;
		}
		return super.mouseReleased(click);
	}

	@Override
	public void close() {
		SlyrexConfig.save();
		if (client != null) client.setScreen(parent);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
