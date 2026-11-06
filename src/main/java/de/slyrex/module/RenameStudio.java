package de.slyrex.module;

import de.slyrex.SlyrexConfig;
import de.slyrex.auktion.Theme;
import de.slyrex.auktion.gui.AnimatedScreen;
import de.slyrex.auktion.gui.Ui;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.util.List;

/** Farbverlaeufe und Stile fuer Item-Namen mit Live-Vorschau. Erzeugt Text/Befehl fuer den Rename-Befehl des Servers. */
public final class RenameStudio {
	public static final String ID = "rename";
	private static final String[] FORMATS = {"AMPERSAND_HEX", "SECTION", "MINIMESSAGE"};
	private static final String[] FORMAT_NAMES = {"&#RRGGBB", "&x&R&R&G&G&B&B", "MiniMessage"};

	private RenameStudio() {}

	private static SlyrexConfig.Rename c() {
		return SlyrexConfig.get().rename;
	}

	static int[] colors() {
		List<String> l = SlyrexConfig.split(c().colors);
		if (l.isEmpty()) return new int[]{0xFFFFFF};
		int[] out = new int[Math.min(5, l.size())];
		for (int i = 0; i < out.length; i++) out[i] = Theme.parse(l.get(i), 0xFFFFFF);
		return out;
	}

	static int colorAt(int[] pal, int i, int n) {
		if (pal.length == 1 || n <= 1) return pal[0];
		float p = i / (float) (n - 1) * (pal.length - 1);
		int k = Math.min(pal.length - 2, (int) p);
		return Theme.mix(pal[k], pal[k + 1], p - k);
	}

	public static Text preview() {
		SlyrexConfig.Rename c = c();
		int[] pal = colors();
		String t = c.text == null ? "" : c.text;
		MutableText out = Text.empty();
		int n = t.codePointCount(0, t.length());
		int i = 0;
		for (int off = 0; off < t.length(); ) {
			int cp = t.codePointAt(off);
			String ch = new String(Character.toChars(cp));
			int col = colorAt(pal, i, n);
			out.append(Text.literal(ch).styled(s -> s.withColor(TextColor.fromRgb(col)).withBold(c.bold).withItalic(c.italic)));
			off += Character.charCount(cp);
			i++;
		}
		return out;
	}

	public static String output() {
		SlyrexConfig.Rename c = c();
		int[] pal = colors();
		String t = c.text == null ? "" : c.text;
		StringBuilder b = new StringBuilder();
		if ("MINIMESSAGE".equals(c.format)) {
			if (c.bold) b.append("<b>");
			if (c.italic) b.append("<i>");
			if (pal.length == 1) b.append("<color:").append(Theme.hex(pal[0])).append('>').append(t).append("</color>");
			else {
				b.append("<gradient");
				for (int p : pal) b.append(':').append(Theme.hex(p));
				b.append('>').append(t).append("</gradient>");
			}
			return b.toString();
		}
		String style = (c.bold ? "&l" : "") + (c.italic ? "&o" : "");
		int n = t.codePointCount(0, t.length());
		int i = 0;
		for (int off = 0; off < t.length(); ) {
			int cp = t.codePointAt(off);
			String hex = String.format("%06X", colorAt(pal, i, n));
			if (cp != ' ') {
				if ("SECTION".equals(c.format)) {
					b.append("&x");
					for (char h : hex.toCharArray()) b.append('&').append(h);
				} else b.append("&#").append(hex);
				b.append(style);
			}
			b.appendCodePoint(cp);
			off += Character.charCount(cp);
			i++;
		}
		return b.toString();
	}

	public static Screen settings(Screen parent) {
		return new StudioScreen(parent);
	}

	static final class StudioScreen extends AnimatedScreen {
		private static final int W = 360, H = 210;
		private final Screen parent;
		private int x0, y0;

		StudioScreen(Screen parent) {
			super(Text.literal("Rename-Studio"));
			this.parent = parent;
		}

		@Override
		protected void init() {
			SlyrexConfig.Rename c = c();
			x0 = (width - W) / 2;
			y0 = Math.max(4, (height - H) / 2);
			int fx = x0 + 90, fw = W - 98;
			TextFieldWidget text = new TextFieldWidget(textRenderer, fx, y0 + 24, fw, 18, Text.literal("Name"));
			text.setMaxLength(50);
			text.setText(c.text);
			text.setChangedListener(s -> c.text = s);
			addDrawableChild(text);
			TextFieldWidget cols = new TextFieldWidget(textRenderer, fx, y0 + 46, fw, 18, Text.literal("Farben"));
			cols.setMaxLength(48);
			cols.setText(c.colors);
			cols.setChangedListener(s -> c.colors = s);
			cols.setTooltip(Tooltip.of(Text.literal("1–5 Hex-Farben, kommagetrennt (Verlauf)")));
			addDrawableChild(cols);
			addDrawableChild(ButtonWidget.builder(Text.literal((c.bold ? "§a" : "§7") + "§lFett"), b -> {
				c.bold = !c.bold;
				b.setMessage(Text.literal((c.bold ? "§a" : "§7") + "§lFett"));
			}).dimensions(fx, y0 + 68, 60, 18).build());
			addDrawableChild(ButtonWidget.builder(Text.literal((c.italic ? "§a" : "§7") + "§oKursiv"), b -> {
				c.italic = !c.italic;
				b.setMessage(Text.literal((c.italic ? "§a" : "§7") + "§oKursiv"));
			}).dimensions(fx + 64, y0 + 68, 60, 18).build());
			addDrawableChild(ButtonWidget.builder(Text.literal(formatName(c.format)), b -> {
				int i = 0;
				for (int k = 0; k < FORMATS.length; k++) if (FORMATS[k].equals(c.format)) i = k;
				c.format = FORMATS[(i + 1) % FORMATS.length];
				b.setMessage(Text.literal(formatName(c.format)));
			}).tooltip(Tooltip.of(Text.literal("Format, das dein Server versteht"))).dimensions(fx + 128, y0 + 68, fw - 128, 18).build());
			TextFieldWidget cmd = new TextFieldWidget(textRenderer, fx, y0 + 90, fw, 18, Text.literal("Befehl"));
			cmd.setMaxLength(32);
			cmd.setText(c.command);
			cmd.setChangedListener(s -> c.command = s);
			addDrawableChild(cmd);

			int by = y0 + H - 26;
			addDrawableChild(ButtonWidget.builder(Text.literal("Text kopieren"), b -> client.keyboard.setClipboard(output()))
					.dimensions(x0 + 8, by, 96, 20).build());
			addDrawableChild(ButtonWidget.builder(Text.literal("In Chat einfügen"), b -> {
				SlyrexConfig.save();
				client.setScreen(new ChatScreen((c.command.isBlank() ? "" : c.command.trim() + " ") + output(), false));
			}).tooltip(Tooltip.of(Text.literal("Öffnet den Chat mit dem fertigen Befehl – du schickst ihn selbst ab")))
					.dimensions(x0 + 108, by, 110, 20).build());
			addDrawableChild(ButtonWidget.builder(Text.literal("§lFertig"), b -> close()).dimensions(x0 + W - 8 - 80, by, 80, 20).build());
		}

		private static String formatName(String f) {
			for (int k = 0; k < FORMATS.length; k++) if (FORMATS[k].equals(f)) return FORMAT_NAMES[k];
			return f;
		}

		@Override
		public void close() {
			SlyrexConfig.save();
			if (client != null) client.setScreen(parent);
		}

		@Override
		protected void renderPanel(DrawContext ctx, int mouseX, int mouseY, float delta) {
			Ui.window(ctx, x0, y0, x0 + W, y0 + H);
			Ui.card(ctx, x0 + 8, y0 + 114, x0 + W - 8, y0 + H - 32);
		}

		@Override
		protected void renderContent(DrawContext ctx, int mouseX, int mouseY, float delta) {
			Ui.text(ctx, textRenderer, "§lRename-Studio", x0 + 8, y0 + 8, Theme.accent());
			String[] labels = {"Name", "Farben", "Stil / Format", "Befehl"};
			for (int i = 0; i < labels.length; i++) Ui.text(ctx, textRenderer, labels[i], x0 + 8, y0 + 29 + i * 22, Theme.text());
			// Vorschau
			ItemStack sword = new ItemStack(net.minecraft.item.Items.NETHERITE_SWORD);
			ctx.drawItem(sword, x0 + 14, y0 + 122);
			ctx.drawTextWithShadow(textRenderer, preview(), x0 + 36, y0 + 126, 0xFFFFFFFF);
			String out = output();
			Ui.text(ctx, textRenderer, "§7Ausgabe (" + out.length() + " Zeichen):", x0 + 14, y0 + 146, Theme.muted());
			var lines = textRenderer.wrapLines(Text.literal(out), W - 28);
			int y = y0 + 157;
			for (int i = 0; i < Math.min(2, lines.size()); i++) {
				ctx.drawTextWithShadow(textRenderer, lines.get(i), x0 + 14, y, Theme.text());
				y += 10;
			}
		}
	}
}
