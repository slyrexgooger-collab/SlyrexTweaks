package de.slyrex.module;

import de.slyrex.SlyrexConfig;
import de.slyrex.SlyrexConfig.ChatBox;
import de.slyrex.auktion.Theme;
import de.slyrex.gui.OptionsScreen;
import de.slyrex.hud.HudEditorScreen;
import de.slyrex.hud.SlyrexHud;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Bis zu drei zusaetzliche Chat-Boxen mit eigenen Filterregeln. */
public final class SecondChat {
	public static final String ID = "secondchat";
	private static final int MAX = 100;

	private record Line(Text text, long time) {}

	@SuppressWarnings("unchecked")
	private static final Deque<Line>[] LINES = new Deque[]{new ArrayDeque<>(), new ArrayDeque<>(), new ArrayDeque<>()};

	/** Vorlagen fuer Filterregeln */
	public static final String[][] TEMPLATES = {
			{"Server", "STARTS_WITH", "[Server],Server »,[System],[Info],[Broadcast]"},
			{"Teleport", "CONTAINS", "teleportieren,teleport to you,teleport to them,tpa-anfrage,hat dir eine anfrage"},
			{"Privat", "CONTAINS", "-> mir,-> me,» mir,flüstert,whispers,[msg]"},
			{"Clan", "CONTAINS", "[Clan],Clan »,(Clan),[C]"},
			{"Erwähnungen", "CONTAINS", "%me%"},
	};

	private SecondChat() {}

	public static boolean enabled() {
		return SlyrexConfig.get().isEnabled(ID);
	}

	private static List<ChatBox> boxes() {
		return SlyrexConfig.get().secondChat.boxes;
	}

	public static void init() {
		for (int i = 0; i < 3; i++) {
			final int idx = i;
			SlyrexHud.register("chatbox" + i, "Chat-Box " + (i + 1), 0.62f, 0.05f + i * 0.28f,
					() -> enabled() && idx < boxes().size() && boxes().get(idx).enabled,
					() -> idx < boxes().size() ? boxes().get(idx).width : 200,
					() -> idx < boxes().size() ? 12 + boxes().get(idx).lines * 9 : 40,
					(ctx, x, y, preview) -> render(ctx, idx, x, y, preview));
		}
	}

	/**
	 * Prueft eine eingehende Nachricht. Gibt true zurueck, wenn sie aus dem normalen Chat entfernt werden soll.
	 */
	public static boolean onMessage(Text message) {
		if (!enabled()) return false;
		String raw = message.getString();
		String low = raw.toLowerCase(Locale.ROOT);
		boolean remove = false;
		for (int i = 0; i < boxes().size() && i < 3; i++) {
			ChatBox b = boxes().get(i);
			if (!b.enabled || !matches(b, raw, low)) continue;
			Deque<Line> d = LINES[i];
			d.addLast(new Line(message, System.currentTimeMillis()));
			while (d.size() > MAX) d.removeFirst();
			if (b.removeFromMain) remove = true;
		}
		return remove;
	}

	private static boolean matches(ChatBox b, String raw, String low) {
		String me = MinecraftClient.getInstance().getSession().getUsername();
		for (String p : SlyrexConfig.split(b.patterns)) {
			String pat = p.replace("%me%", me);
			switch (b.mode) {
				case "STARTS_WITH" -> {
					if (low.startsWith(pat.toLowerCase(Locale.ROOT))) return true;
				}
				case "REGEX" -> {
					try {
						if (Pattern.compile(pat, Pattern.CASE_INSENSITIVE).matcher(raw).find()) return true;
					} catch (Exception ignored) {
					}
				}
				default -> {
					if (low.contains(pat.toLowerCase(Locale.ROOT))) return true;
				}
			}
		}
		return false;
	}

	private static void render(DrawContext ctx, int idx, int x, int y, boolean preview) {
		if (idx >= boxes().size()) return;
		ChatBox b = boxes().get(idx);
		MinecraftClient mc = MinecraftClient.getInstance();
		var tr = mc.textRenderer;
		boolean chatOpen = mc.currentScreen instanceof ChatScreen;
		long now = System.currentTimeMillis();
		long fade = b.fadeSeconds * 1000L;

		// Zeilen umbrechen, neueste unten
		List<OrderedText> rows = new ArrayList<>();
		List<Float> alpha = new ArrayList<>();
		List<Line> src = new ArrayList<>(LINES[idx]);
		if (preview && src.isEmpty()) {
			src.add(new Line(Text.literal("§7Beispiel: §fNachrichten für §e" + b.name), now));
			src.add(new Line(Text.literal("§7Filter: §f" + b.patterns), now));
		}
		for (int i = src.size() - 1; i >= 0 && rows.size() < b.lines; i--) {
			Line l = src.get(i);
			float a = 1f;
			if (!chatOpen && !preview && b.fadeSeconds > 0) {
				long age = now - l.time;
				if (age > fade) break;
				if (age > fade - 1000) a = (fade - age) / 1000f;
			}
			List<OrderedText> wrapped = tr.wrapLines(l.text, b.width - 6);
			for (int k = wrapped.size() - 1; k >= 0 && rows.size() < b.lines; k--) {
				rows.add(0, wrapped.get(k));
				alpha.add(0, a);
			}
		}
		boolean show = chatOpen || preview || !rows.isEmpty();
		if (!show) return;
		int h = 12 + b.lines * 9;
		int ty = y + h - 9 * rows.size() - 1;
		if (chatOpen || preview) {
			ctx.fill(x, y, x + b.width, y + h, Theme.background());
			ctx.fill(x, y, x + b.width, y + 10, Theme.accent(0x60));
			ctx.drawTextWithShadow(tr, b.name, x + 3, y + 1, Theme.text());
		} else {
			// Nur hinter den sichtbaren Zeilen abdunkeln, wie im normalen Chat
			ctx.fill(x, ty - 1, x + b.width, y + h, 0x50000000);
			ctx.fill(x, ty - 1, x + 1, y + h, Theme.accent(0xA0));
		}
		for (int i = 0; i < rows.size(); i++) {
			int a = Math.max(8, Math.round(255 * alpha.get(i)));
			ctx.drawTextWithShadow(tr, rows.get(i), x + 3, ty, (a << 24) | 0xFFFFFF);
			ty += 9;
		}
	}

	public static void clear(int idx) {
		if (idx >= 0 && idx < 3) LINES[idx].clear();
	}

	public static Screen settings(Screen parent) {
		return boxSettings(parent, 0);
	}

	private static Screen boxSettings(Screen parent, int idx) {
		ChatBox b = boxes().get(idx);
		OptionsScreen[] self = new OptionsScreen[1];
		self[0] = new OptionsScreen(parent, "Zweiter Chat – Box " + (idx + 1), "%me% = dein Name")
				.action("Box wählen", "Box " + (idx + 1) + " von " + boxes().size() + "  »", null,
						() -> MinecraftClient.getInstance().setScreen(boxSettings(parent, (idx + 1) % boxes().size())))
				.toggle("Aktiv", null, () -> b.enabled, v -> b.enabled = v)
				.field("Name", null, 24, () -> b.name, v -> b.name = v)
				.choice("Regel", null, new String[]{"CONTAINS", "STARTS_WITH", "REGEX"},
						new String[]{"Enthält", "Beginnt mit", "Regex"}, () -> b.mode, v -> b.mode = v)
				.field("Filter", "Kommagetrennt – trifft einer zu, landet die Nachricht in dieser Box", 512,
						() -> b.patterns, v -> b.patterns = v)
				.action("Vorlage", "Vorlage laden  »", "Server, Teleport, Privat, Clan, Erwähnungen", () -> {
					int next = 0;
					for (int k = 0; k < TEMPLATES.length; k++) if (TEMPLATES[k][0].equals(b.name)) next = (k + 1) % TEMPLATES.length;
					b.name = TEMPLATES[next][0];
					b.mode = TEMPLATES[next][1];
					b.patterns = TEMPLATES[next][2];
					SlyrexConfig.save();
					MinecraftClient.getInstance().setScreen(boxSettings(parent, idx));
				})
				.toggle("Aus Hauptchat entfernen", null, () -> b.removeFromMain, v -> b.removeFromMain = v)
				.cycle("Breite", null, new int[]{140, 180, 220, 260, 300, 340}, () -> b.width, v -> b.width = v, v -> v + " px")
				.cycle("Zeilen", null, new int[]{3, 4, 5, 6, 8, 10, 12, 15}, () -> b.lines, v -> b.lines = v, v -> v + "")
				.cycle("Ausblenden nach", null, new int[]{0, 5, 10, 15, 30, 60}, () -> b.fadeSeconds, v -> b.fadeSeconds = v,
						v -> v == 0 ? "Nie" : v + " Sek.")
				.action("Verlauf", "Leeren", null, () -> clear(idx))
				.footer("HUD-Editor", () -> MinecraftClient.getInstance().setScreen(new HudEditorScreen(self[0])));
		return self[0];
	}
}
