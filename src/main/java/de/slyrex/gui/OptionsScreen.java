package de.slyrex.gui;

import de.slyrex.SlyrexConfig;
import de.slyrex.auktion.Theme;
import de.slyrex.auktion.gui.AnimatedScreen;
import de.slyrex.auktion.gui.Ui;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Einfaches, einheitliches Einstellungsfenster: links Beschriftung, rechts Knopf/Eingabe.
 * Module beschreiben ihre Optionen ueber die Builder-Methoden.
 */
public class OptionsScreen extends AnimatedScreen {
	protected static final int W = 360, ROW = 22;

	private final Screen parent;
	private final String heading;
	private final String hint;
	private final List<Entry> entries = new ArrayList<>();
	private final List<Footer> footers = new ArrayList<>();
	private int page;
	private int x0, y0, h, perPage;
	private final List<int[]> labelRows = new ArrayList<>();
	private final List<String> labelTexts = new ArrayList<>();

	private interface Entry {}

	private record Toggle(String label, String tip, BooleanSupplier get, Consumer<Boolean> set) implements Entry {}

	private record Cycle(String label, String tip, int[] values, IntSupplier get, IntConsumer set, IntFunction<String> fmt) implements Entry {}

	private record Choice(String label, String tip, String[] values, String[] names, Supplier<String> get, Consumer<String> set) implements Entry {}

	private record Field(String label, String tip, int max, Supplier<String> get, Consumer<String> set) implements Entry {}

	private record Action(String label, String button, String tip, Runnable run) implements Entry {}

	private record Header(String label) implements Entry {}

	private record Footer(String label, Runnable run) {}

	public OptionsScreen(Screen parent, String heading, String hint) {
		super(Text.literal(heading));
		this.parent = parent;
		this.heading = heading;
		this.hint = hint;
	}

	public OptionsScreen toggle(String label, String tip, BooleanSupplier get, Consumer<Boolean> set) {
		entries.add(new Toggle(label, tip, get, set));
		return this;
	}

	public OptionsScreen cycle(String label, String tip, int[] values, IntSupplier get, IntConsumer set, IntFunction<String> fmt) {
		entries.add(new Cycle(label, tip, values, get, set, fmt));
		return this;
	}

	public OptionsScreen choice(String label, String tip, String[] values, String[] names, Supplier<String> get, Consumer<String> set) {
		entries.add(new Choice(label, tip, values, names, get, set));
		return this;
	}

	public OptionsScreen field(String label, String tip, int max, Supplier<String> get, Consumer<String> set) {
		entries.add(new Field(label, tip, max, get, set));
		return this;
	}

	public OptionsScreen action(String label, String button, String tip, Runnable run) {
		entries.add(new Action(label, button, tip, run));
		return this;
	}

	public OptionsScreen header(String label) {
		entries.add(new Header(label));
		return this;
	}

	/** Zusaetzlicher Knopf in der Fusszeile (z.B. HUD-Editor). */
	public OptionsScreen footer(String label, Runnable run) {
		footers.add(new Footer(label, run));
		return this;
	}

	@Override
	protected void init() {
		h = Math.min(height - 8, 40 + entries.size() * ROW + 34);
		h = Math.max(h, 120);
		x0 = (width - W) / 2;
		y0 = Math.max(4, (height - h) / 2);
		perPage = Math.max(1, (h - 40 - 34) / ROW);
		int pages = (entries.size() + perPage - 1) / perPage;
		page = Math.max(0, Math.min(page, pages - 1));
		labelRows.clear();
		labelTexts.clear();

		int cx = x0 + W - 8 - 150, cw = 150;
		int y = y0 + 26;
		for (int i = page * perPage; i < Math.min(entries.size(), (page + 1) * perPage); i++) {
			Entry e = entries.get(i);
			String label = null;
			if (e instanceof Toggle t) {
				label = t.label;
				addDrawableChild(tip(ButtonWidget.builder(Text.literal(onOff(t.get.getAsBoolean())), b -> {
					t.set.accept(!t.get.getAsBoolean());
					b.setMessage(Text.literal(onOff(t.get.getAsBoolean())));
					SlyrexConfig.save();
				}).dimensions(cx, y, cw, 20), t.tip).build());
			} else if (e instanceof Cycle c) {
				label = c.label;
				addDrawableChild(tip(ButtonWidget.builder(Text.literal(c.fmt.apply(c.get.getAsInt())), b -> {
					int next = c.values[0];
					int cur = c.get.getAsInt();
					for (int k = 0; k < c.values.length; k++) {
						if (c.values[k] == cur) {
							next = c.values[(k + 1) % c.values.length];
							break;
						}
						if (c.values[k] > cur) {
							next = c.values[k];
							break;
						}
					}
					c.set.accept(next);
					b.setMessage(Text.literal(c.fmt.apply(next)));
					SlyrexConfig.save();
				}).dimensions(cx, y, cw, 20), c.tip).build());
			} else if (e instanceof Choice c) {
				label = c.label;
				addDrawableChild(tip(ButtonWidget.builder(Text.literal(choiceName(c, c.get.get())), b -> {
					int idx = 0;
					for (int k = 0; k < c.values.length; k++) if (c.values[k].equalsIgnoreCase(c.get.get())) idx = k;
					String next = c.values[(idx + 1) % c.values.length];
					c.set.accept(next);
					b.setMessage(Text.literal(choiceName(c, next)));
					SlyrexConfig.save();
				}).dimensions(cx, y, cw, 20), c.tip).build());
			} else if (e instanceof Field f) {
				label = f.label;
				TextFieldWidget tf = new TextFieldWidget(textRenderer, cx + 1, y + 1, cw - 2, 18, Text.literal(f.label));
				tf.setMaxLength(f.max);
				tf.setText(f.get.get() == null ? "" : f.get.get());
				tf.setChangedListener(f.set::accept);
				if (f.tip != null) tf.setTooltip(Tooltip.of(Text.literal(f.tip)));
				addDrawableChild(tf);
			} else if (e instanceof Action a) {
				label = a.label;
				addDrawableChild(tip(ButtonWidget.builder(Text.literal(a.button), b -> a.run.run())
						.dimensions(cx, y, cw, 20), a.tip).build());
			} else if (e instanceof Header hd) {
				labelRows.add(new int[]{y, 1});
				labelTexts.add(hd.label);
			}
			if (label != null) {
				labelRows.add(new int[]{y, 0});
				labelTexts.add(label);
			}
			y += ROW;
		}

		int by = y0 + h - 26;
		int bx = x0 + 8;
		if (pages > 1) {
			addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> {
				page = (page - 1 + pages) % pages;
				clearAndInit();
			}).dimensions(bx, by, 20, 20).build());
			addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> {
				page = (page + 1) % pages;
				clearAndInit();
			}).dimensions(bx + 22, by, 20, 20).build());
			bx += 48;
		}
		for (Footer f : footers) {
			int fw = Math.max(60, textRenderer.getWidth(f.label) + 12);
			addDrawableChild(ButtonWidget.builder(Text.literal(f.label), b -> f.run.run()).dimensions(bx, by, fw, 20).build());
			bx += fw + 4;
		}
		addDrawableChild(ButtonWidget.builder(Text.literal("§lFertig"), b -> close())
				.dimensions(x0 + W - 8 - 80, by, 80, 20).build());
	}

	private static String choiceName(Choice c, String value) {
		for (int k = 0; k < c.values.length; k++) if (c.values[k].equalsIgnoreCase(value)) return c.names[k];
		return value;
	}

	private static ButtonWidget.Builder tip(ButtonWidget.Builder b, String tip) {
		return tip == null ? b : b.tooltip(Tooltip.of(Text.literal(tip)));
	}

	public static String onOff(boolean on) {
		return on ? "§aAN" : "§cAUS";
	}

	@Override
	public void close() {
		SlyrexConfig.save();
		if (client != null) client.setScreen(parent);
	}

	@Override
	protected void renderPanel(DrawContext ctx, int mouseX, int mouseY, float delta) {
		Ui.window(ctx, x0, y0, x0 + W, y0 + h);
		ctx.fill(x0 + 8, y0 + h - 30, x0 + W - 8, y0 + h - 29, Theme.border());
	}

	@Override
	protected void renderContent(DrawContext ctx, int mouseX, int mouseY, float delta) {
		Ui.text(ctx, textRenderer, "§l" + heading, x0 + 8, y0 + 8, Theme.accent());
		if (hint != null) Ui.textRight(ctx, textRenderer, textRenderer.trimToWidth(hint, W - 24 - textRenderer.getWidth(heading) - 20),
				x0 + W - 8, y0 + 8, Theme.muted());
		for (int i = 0; i < labelRows.size(); i++) {
			int[] r = labelRows.get(i);
			if (r[1] == 1) {
				Ui.text(ctx, textRenderer, "§l" + labelTexts.get(i), x0 + 8, r[0] + 6, Theme.accent());
				ctx.fill(x0 + 8, r[0] + 17, x0 + W - 8, r[0] + 18, Theme.border());
			} else {
				Ui.text(ctx, textRenderer, textRenderer.trimToWidth(labelTexts.get(i), labelWidth()), x0 + 8, r[0] + 6, Theme.text());
			}
		}
		renderExtra(ctx, mouseX, mouseY, x0, y0, W, h);
	}

	/** Platz fuer Beschriftungen (Unterklassen mit Vorschau koennen ihn verkleinern). */
	protected int labelWidth() {
		return W - 170;
	}

	/** Fuer Unterklassen: zusaetzliche Anzeige (z.B. Vorschau). */
	protected void renderExtra(DrawContext ctx, int mouseX, int mouseY, int x0, int y0, int w, int h) {}

	protected static int[] range(int from, int to, int step) {
		int[] out = new int[(to - from) / step + 1];
		for (int i = 0; i < out.length; i++) out[i] = from + i * step;
		return out;
	}
}
