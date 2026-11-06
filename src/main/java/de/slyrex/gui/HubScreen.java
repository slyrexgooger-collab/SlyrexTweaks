package de.slyrex.gui;

import de.slyrex.Module;
import de.slyrex.Slyrex;
import de.slyrex.SlyrexConfig;
import de.slyrex.auktion.Theme;
import de.slyrex.auktion.gui.AnimatedScreen;
import de.slyrex.auktion.gui.DesignScreen;
import de.slyrex.auktion.gui.Ui;
import de.slyrex.hud.HudEditorScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Das gemeinsame Slyrex-Menue mit allen Modulen. */
public class HubScreen extends AnimatedScreen {
	private static final int CARD_H = 46, GAP = 6;
	private static boolean favoritesOnly;
	private static int page;
	private static String search = "";

	private int x0, y0, w, h, cols, cardW, perPage;
	private final List<Module> shown = new ArrayList<>();
	private final List<int[]> cardPos = new ArrayList<>();

	public HubScreen() {
		super(Text.literal("Slyrex"));
	}

	@Override
	protected void init() {
		w = Math.min(width - 12, 460);
		h = Math.min(height - 8, 300);
		x0 = (width - w) / 2;
		y0 = Math.max(4, (height - h) / 2);
		cols = w >= 380 ? 2 : 1;
		cardW = (w - 16 - (cols - 1) * GAP) / cols;
		int rows = Math.max(1, (h - 52 - 30) / (CARD_H + GAP));
		perPage = rows * cols;

		shown.clear();
		String q = search.toLowerCase(Locale.ROOT);
		for (Module m : Slyrex.modules()) {
			if (favoritesOnly && !m.favorite()) continue;
			if (!q.isEmpty() && !m.name.toLowerCase(Locale.ROOT).contains(q)
					&& !m.description.toLowerCase(Locale.ROOT).contains(q)) continue;
			shown.add(m);
		}
		int pages = Math.max(1, (shown.size() + perPage - 1) / perPage);
		page = Math.max(0, Math.min(page, pages - 1));

		// Kopfzeile
		int tx = x0 + 70;
		addDrawableChild(ButtonWidget.builder(Text.literal(favoritesOnly ? "Alle" : "§c❤ §fFavoriten"), b -> {
			favoritesOnly = !favoritesOnly;
			page = 0;
			clearAndInit();
		}).dimensions(tx, y0 + 6, 74, 18).build());
		TextFieldWidget searchField = new TextFieldWidget(textRenderer, tx + 78, y0 + 6, Math.max(60, w - 70 - 78 - 8 - 150), 18, Text.literal("Suche"));
		searchField.setText(search);
		searchField.setPlaceholder(Text.literal("§7Suchen …"));
		searchField.setChangedListener(s -> {
			if (!s.equals(search)) {
				search = s;
				page = 0;
				clearAndInit();
				// Fokus nach Neuaufbau behalten
				for (var c : children()) if (c instanceof TextFieldWidget tf) { setFocused(tf); tf.setFocused(true); }
			}
		});
		addDrawableChild(searchField);
		addDrawableChild(ButtonWidget.builder(Text.literal("Design"), b -> client.setScreen(new DesignScreen(this)))
				.tooltip(Tooltip.of(Text.literal("Farben und Deckkraft für alle Slyrex-Fenster")))
				.dimensions(x0 + w - 8 - 146, y0 + 6, 70, 18).build());
		addDrawableChild(ButtonWidget.builder(Text.literal("HUD-Editor"), b -> client.setScreen(new HudEditorScreen(this)))
				.dimensions(x0 + w - 8 - 72, y0 + 6, 72, 18).build());

		// Karten
		cardPos.clear();
		int start = page * perPage;
		for (int i = start; i < Math.min(shown.size(), start + perPage); i++) {
			Module m = shown.get(i);
			int idx = i - start;
			int cx = x0 + 8 + (idx % cols) * (cardW + GAP);
			int cy = y0 + 32 + (idx / cols) * (CARD_H + GAP);
			cardPos.add(new int[]{cx, cy});
			int by = cy + CARD_H - 19;
			int bx = cx + cardW - 4;
			addDrawableChild(ButtonWidget.builder(Text.literal(m.favorite() ? "§c❤" : "§7❤"), b -> {
				m.toggleFavorite();
				clearAndInit();
			}).tooltip(Tooltip.of(Text.literal("Favorit"))).dimensions(bx - 18, by, 18, 16).build());
			bx -= 20;
			if (m.hasSettings()) {
				addDrawableChild(ButtonWidget.builder(Text.literal("⚙"), b -> {
					var s = m.settings(this);
					if (s != null) client.setScreen(s);
				}).tooltip(Tooltip.of(Text.literal("Einstellungen"))).dimensions(bx - 18, by, 18, 16).build());
				bx -= 20;
			}
			addDrawableChild(ButtonWidget.builder(Text.literal(m.enabled() ? "§aAN" : "§cAUS"), b -> {
				m.toggle();
				b.setMessage(Text.literal(m.enabled() ? "§aAN" : "§cAUS"));
			}).dimensions(bx - 34, by, 34, 16).build());
		}

		// Fusszeile
		int by = y0 + h - 24;
		if (pages > 1) {
			addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> {
				page = (page - 1 + pages) % pages;
				clearAndInit();
			}).dimensions(x0 + 8, by, 20, 18).build());
			addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> {
				page = (page + 1) % pages;
				clearAndInit();
			}).dimensions(x0 + 30, by, 20, 18).build());
		}
		addDrawableChild(ButtonWidget.builder(Text.literal("§lSchließen"), b -> close())
				.dimensions(x0 + w - 8 - 80, by, 80, 18).build());
	}

	@Override
	protected void renderPanel(DrawContext ctx, int mouseX, int mouseY, float delta) {
		Ui.window(ctx, x0, y0, x0 + w, y0 + h);
		ctx.fill(x0 + 8, y0 + 28, x0 + w - 8, y0 + 29, Theme.border());
		ctx.fill(x0 + 8, y0 + h - 28, x0 + w - 8, y0 + h - 27, Theme.border());
		int start = page * perPage;
		for (int i = 0; i < cardPos.size(); i++) {
			Module m = shown.get(start + i);
			int[] p = cardPos.get(i);
			boolean hover = mouseX >= p[0] && mouseX < p[0] + cardW && mouseY >= p[1] && mouseY < p[1] + CARD_H;
			ctx.fill(p[0], p[1], p[0] + cardW, p[1] + CARD_H, hover ? Theme.cardHover() : Theme.card());
			ctx.fill(p[0], p[1], p[0] + 2, p[1] + CARD_H, m.enabled() ? Theme.accent() : 0xFF555555);
		}
	}

	@Override
	protected void renderContent(DrawContext ctx, int mouseX, int mouseY, float delta) {
		// Titel mit Farbverlauf
		String title = "SLYREX";
		int tx = x0 + 10;
		for (int i = 0; i < title.length(); i++) {
			float t = (float) i / (title.length() - 1);
			float wave = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 400.0 + i * 0.6);
			int col = 0xFF000000 | Theme.mix(Theme.accentRgb(), 0xFFFFFF, 0.15f + 0.35f * wave * (1 - t * 0.5f));
			String ch = String.valueOf(title.charAt(i));
			ctx.drawTextWithShadow(textRenderer, Text.literal(ch).styled(s -> s.withBold(true)), tx, y0 + 11, col);
			tx += textRenderer.getWidth(Text.literal(ch).styled(s -> s.withBold(true)));
		}

		int start = page * perPage;
		for (int i = 0; i < cardPos.size(); i++) {
			Module m = shown.get(start + i);
			int[] p = cardPos.get(i);
			Ui.bigItem(ctx, m.icon, p[0] + 7, p[1] + 6, 1.25f);
			int textW = cardW - 36 - 4;
			Ui.text(ctx, textRenderer, textRenderer.trimToWidth(m.name, textW), p[0] + 32, p[1] + 5, Theme.text());
			Ui.text(ctx, textRenderer, textRenderer.trimToWidth(m.description, cardW - 36 - 4), p[0] + 32, p[1] + 16,
					Theme.muted());
		}
		if (shown.isEmpty()) {
			Ui.textCenter(ctx, textRenderer, favoritesOnly ? "Noch keine Favoriten – klicke auf ❤ bei einem Modul." : "Nichts gefunden.",
					x0 + w / 2, y0 + h / 2 - 4, Theme.muted());
		}
		int pages = Math.max(1, (shown.size() + perPage - 1) / perPage);
		String info = (pages > 1 ? "Seite " + (page + 1) + "/" + pages + "  ·  " : "") + "Slyrex " + Slyrex.version();
		Ui.text(ctx, textRenderer, info, x0 + (pages > 1 ? 56 : 8), y0 + h - 19, Theme.muted());

		for (int i = 0; i < cardPos.size(); i++) {
			int[] p = cardPos.get(i);
			if (mouseX >= p[0] && mouseX < p[0] + cardW && mouseY >= p[1] && mouseY < p[1] + 16) {
				Module m = shown.get(start + i);
				ctx.drawTooltip(textRenderer, List.of(Text.literal(m.name).styled(s -> s.withBold(true)), Text.literal("§7" + m.description)),
						mouseX, mouseY);
			}
		}
	}

	@Override
	public void close() {
		SlyrexConfig.save();
		super.close();
	}
}
