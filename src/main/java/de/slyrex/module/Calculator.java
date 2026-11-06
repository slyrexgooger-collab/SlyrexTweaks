package de.slyrex.module;

import de.slyrex.SlyrexConfig;
import de.slyrex.auktion.Theme;
import de.slyrex.auktion.gui.AnimatedScreen;
import de.slyrex.auktion.gui.Ui;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;

/** Taschenrechner – mit k/m/b-Kuerzeln (1,5m = 1.500.000), erreichbar ueber das Inventar. */
public final class Calculator {
	public static final String ID = "calculator";

	private Calculator() {}

	public static boolean enabled() {
		return SlyrexConfig.get().isEnabled(ID);
	}

	public static void init() {
		ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
			if (!enabled() || !(screen instanceof InventoryScreen inv)) return;
			var acc = (de.slyrex.mixin.HandledScreenAccessor) inv;
			int x = acc.slyrex$getX() + acc.slyrex$getBackgroundWidth() + 2;
			int y = acc.slyrex$getY() + 2;
			Screens.getButtons(screen).add(ButtonWidget.builder(Text.literal("±"), b -> client.setScreen(new CalcScreen(screen)))
					.tooltip(Tooltip.of(Text.literal("Rechner (Slyrex)"))).dimensions(x, y, 18, 18).build());
		});
	}

	// ------------------------------------------------------------ Parser

	/** Wertet einen Ausdruck aus: + - * / % ^, Klammern, k/m/b/mrd-Kuerzel, Komma oder Punkt als Dezimalzeichen. */
	public static double eval(String expr) {
		return new Parser(expr).parse();
	}

	private static final class Parser {
		private final String s;
		private int pos;

		Parser(String expr) {
			String e = expr.toLowerCase(Locale.ROOT).replace(" ", "").replace("×", "*").replace("÷", "/").replace("x", "*");
			// "1.500.000" (Tausenderpunkte) -> 1500000, "1,5" -> 1.5
			e = e.replaceAll("(\\d)\\.(\\d{3})(?!\\d)", "$1$2").replaceAll("(\\d)\\.(\\d{3})(?!\\d)", "$1$2");
			this.s = e.replace(',', '.');
		}

		double parse() {
			double v = expression();
			if (pos < s.length()) throw new IllegalArgumentException("Unerwartet: " + s.charAt(pos));
			return v;
		}

		private boolean eat(char c) {
			if (pos < s.length() && s.charAt(pos) == c) {
				pos++;
				return true;
			}
			return false;
		}

		private double expression() {
			double v = term();
			while (true) {
				if (eat('+')) v += term();
				else if (eat('-')) v -= term();
				else return v;
			}
		}

		private double term() {
			double v = factor();
			while (true) {
				if (eat('*')) v *= factor();
				else if (eat('/')) {
					double d = factor();
					if (d == 0) throw new ArithmeticException("Division durch 0");
					v /= d;
				} else if (eat('%')) v = v * factor() / 100.0; // 20%200 = 20 % von 200
				else return v;
			}
		}

		private double factor() {
			if (eat('+')) return factor();
			if (eat('-')) return -factor();
			double v;
			if (eat('(')) {
				v = expression();
				if (!eat(')')) throw new IllegalArgumentException("Klammer fehlt");
			} else {
				int start = pos;
				while (pos < s.length() && (Character.isDigit(s.charAt(pos)) || s.charAt(pos) == '.')) pos++;
				if (start == pos) throw new IllegalArgumentException("Zahl erwartet");
				v = Double.parseDouble(s.substring(start, pos));
				if (s.startsWith("mrd", pos)) { pos += 3; v *= 1e9; }
				else if (eat('k')) v *= 1e3;
				else if (eat('m')) v *= 1e6;
				else if (eat('b')) v *= 1e9;
			}
			if (eat('^')) v = Math.pow(v, factor());
			return v;
		}
	}

	public static String format(double v) {
		if (Double.isNaN(v) || Double.isInfinite(v)) return "Fehler";
		BigDecimal bd = BigDecimal.valueOf(v).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros();
		String plain = bd.toPlainString();
		String[] parts = plain.split("\\.");
		String intPart = parts[0];
		boolean neg = intPart.startsWith("-");
		if (neg) intPart = intPart.substring(1);
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < intPart.length(); i++) {
			if (i > 0 && (intPart.length() - i) % 3 == 0) b.append('.');
			b.append(intPart.charAt(i));
		}
		return (neg ? "-" : "") + b + (parts.length > 1 ? "," + parts[1] : "");
	}

	// ------------------------------------------------------------ Fenster

	public static Screen settings(Screen parent) {
		return new CalcScreen(parent);
	}

	public static final class CalcScreen extends AnimatedScreen {
		private static final int W = 240, H = 214;
		private static final String[][] KEYS = {
				{"7", "8", "9", "/", "("},
				{"4", "5", "6", "*", ")"},
				{"1", "2", "3", "-", "k"},
				{"0", ",", "%", "+", "m"},
		};
		private final Screen parent;
		private TextFieldWidget field;
		private String result = "";
		private int x0, y0;

		public CalcScreen(Screen parent) {
			super(Text.literal("Rechner"));
			this.parent = parent;
		}

		@Override
		protected void init() {
			x0 = (width - W) / 2;
			y0 = Math.max(4, (height - H) / 2);
			String old = field == null ? "" : field.getText();
			field = new TextFieldWidget(textRenderer, x0 + 8, y0 + 22, W - 16, 18, Text.literal("Rechnung"));
			field.setMaxLength(128);
			field.setText(old);
			field.setPlaceholder(Text.literal("§7z.B. 1,5m - 250k * 2"));
			field.setChangedListener(s -> update());
			addDrawableChild(field);
			setInitialFocus(field);

			int bw = (W - 16 - 4 * 3) / 5;
			int y = y0 + 62;
			for (String[] row : KEYS) {
				int x = x0 + 8;
				for (String k : row) {
					addDrawableChild(ButtonWidget.builder(Text.literal(k), b -> {
						field.write(k);
						setFocused(field);
					}).dimensions(x, y, bw, 18).build());
					x += bw + 3;
				}
				y += 21;
			}
			int x = x0 + 8;
			addDrawableChild(ButtonWidget.builder(Text.literal("C"), b -> field.setText("")).dimensions(x, y, bw, 18).build());
			addDrawableChild(ButtonWidget.builder(Text.literal("⌫"), b -> {
				String t = field.getText();
				if (!t.isEmpty()) field.setText(t.substring(0, t.length() - 1));
			}).dimensions(x + bw + 3, y, bw, 18).build());
			addDrawableChild(ButtonWidget.builder(Text.literal("Kopieren"), b -> {
				if (!result.isEmpty() && client != null) client.keyboard.setClipboard(result.replace(".", ""));
			}).dimensions(x + 2 * (bw + 3), y, bw * 2 + 3, 18).build());
			addDrawableChild(ButtonWidget.builder(Text.literal("§l="), b -> commit()).dimensions(x + 4 * (bw + 3), y, bw, 18).build());
			addDrawableChild(ButtonWidget.builder(Text.literal("Zurück"), b -> close()).dimensions(x0 + W - 8 - 60, y0 + H - 24, 60, 18).build());
			update();
		}

		private void update() {
			String t = field.getText().trim();
			if (t.isEmpty()) {
				result = "";
				return;
			}
			try {
				result = format(eval(t));
			} catch (Exception e) {
				result = "";
			}
		}

		private void commit() {
			if (result.isEmpty()) return;
			List<String> h = SlyrexConfig.get().calculator.history;
			h.add(0, field.getText().trim() + " = " + result);
			while (h.size() > 3) h.remove(h.size() - 1);
			SlyrexConfig.save();
			field.setText(result.replace(".", ""));
		}

		@Override
		public boolean keyPressed(KeyInput input) {
			if (input.key() == GLFW.GLFW_KEY_ENTER || input.key() == GLFW.GLFW_KEY_KP_ENTER) {
				commit();
				return true;
			}
			return super.keyPressed(input);
		}

		@Override
		public void close() {
			if (client != null) client.setScreen(parent);
		}

		@Override
		protected void renderPanel(DrawContext ctx, int mouseX, int mouseY, float delta) {
			Ui.window(ctx, x0, y0, x0 + W, y0 + H);
		}

		@Override
		protected void renderContent(DrawContext ctx, int mouseX, int mouseY, float delta) {
			Ui.text(ctx, textRenderer, "§lRechner", x0 + 8, y0 + 8, Theme.accent());
			Ui.textRight(ctx, textRenderer, result.isEmpty() ? (field.getText().isBlank() ? "" : "…") : "= " + result,
					x0 + W - 8, y0 + 46, Theme.good());
			int y = y0 + 62 + 5 * 21 + 2;
			for (String line : SlyrexConfig.get().calculator.history) {
				Ui.text(ctx, textRenderer, textRenderer.trimToWidth(line, W - 80), x0 + 8, y, Theme.muted());
				y += 10;
			}
		}
	}
}
