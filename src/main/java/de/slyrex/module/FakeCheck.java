package de.slyrex.module;

import de.slyrex.SlyrexConfig;
import de.slyrex.gui.OptionsScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Warnt vor Namen und Clan-Tags, die bekannten Eintraegen aehneln (z.B. "S1yrex" statt "Slyrex").
 * Prueft NICHT, ob ein Account wirklich jemandem gehoert.
 */
public final class FakeCheck {
	public static final String ID = "fakecheck";
	private static final Set<String> WARNED = new HashSet<>();
	private static String pendingCommand;
	private static long pendingTime;
	private static int timer;

	private FakeCheck() {}

	private static SlyrexConfig.FakeCheck c() {
		return SlyrexConfig.get().fakeCheck;
	}

	public static boolean enabled() {
		return SlyrexConfig.get().isEnabled(ID);
	}

	/** Verwechselbare Zeichen vereinheitlichen. */
	static String normalize(String s) {
		StringBuilder b = new StringBuilder();
		for (char ch : s.toLowerCase(Locale.ROOT).toCharArray()) {
			switch (ch) {
				case '0' -> b.append('o');
				case '1', 'i', '!', '|' -> b.append('l');
				case '3' -> b.append('e');
				case '4', '@' -> b.append('a');
				case '5', '$' -> b.append('s');
				case '7' -> b.append('t');
				case '8' -> b.append('b');
				case '_', '-', '.' -> { }
				default -> b.append(ch);
			}
		}
		return b.toString().replace("rn", "m").replace("vv", "w");
	}

	static int distance(String a, String b) {
		int[] prev = new int[b.length() + 1], cur = new int[b.length() + 1];
		for (int j = 0; j <= b.length(); j++) prev[j] = j;
		for (int i = 1; i <= a.length(); i++) {
			cur[0] = i;
			for (int j = 1; j <= b.length(); j++) {
				int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
				cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
			}
			int[] t = prev;
			prev = cur;
			cur = t;
		}
		return prev[b.length()];
	}

	/** Liefert den Original-Eintrag, dem {@code candidate} verdaechtig aehnelt, sonst null. */
	public static String lookalike(String candidate, List<String> references) {
		if (candidate == null || candidate.isEmpty()) return null;
		String nc = normalize(candidate);
		for (String ref : references) {
			if (ref.equalsIgnoreCase(candidate)) return null; // genau der echte Name
		}
		for (String ref : references) {
			String nr = normalize(ref);
			if (nr.length() < 3) continue;
			if (nc.equals(nr) || (Math.abs(nc.length() - nr.length()) <= 1 && distance(nc, nr) <= 1)) return ref;
		}
		return null;
	}

	public static String suspiciousName(String name) {
		return lookalike(name, SlyrexConfig.split(c().trustedNames));
	}

	public static void tick(MinecraftClient mc) {
		if (!enabled() || mc.getNetworkHandler() == null || mc.player == null) return;
		if (++timer % 40 != 0) return;
		List<String> refs = SlyrexConfig.split(c().trustedNames);
		List<String> tags = SlyrexConfig.split(c().clanTags);
		if (refs.isEmpty() && tags.isEmpty()) return;
		for (PlayerListEntry e : mc.getNetworkHandler().getPlayerList()) {
			String name = e.getProfile().name();
			String ref = lookalike(name, refs);
			if (ref != null && WARNED.add(name)) {
				warn(mc, "Spieler §c" + name + "§e sieht aus wie §a" + ref + "§e – Vorsicht, möglicher Fake!");
			}
			if (e.getDisplayName() != null && !tags.isEmpty()) {
				String display = e.getDisplayName().getString();
				for (String word : display.split("[\\s\\[\\]()|]+")) {
					if (word.isBlank() || word.equalsIgnoreCase(name)) continue;
					String tag = lookalike(word, tags);
					if (tag != null && WARNED.add(name + "#" + word)) {
						warn(mc, "Clan-Tag §c" + word + "§e bei " + name + " ähnelt §a" + tag + "§e – Vorsicht!");
					}
				}
			}
		}
	}

	private static void warn(MinecraftClient mc, String text) {
		mc.player.sendMessage(Text.literal("§6[Fake-Check] §e" + text), false);
		mc.inGameHud.setOverlayMessage(Text.literal("⚠ Möglicher Fake-Account erkannt").formatted(Formatting.RED), false);
	}

	/** Spieler-Chat: Warnung anhaengen, wenn der Absender verdaechtig ist. */
	public static void onChat(String sender) {
		if (!enabled() || !c().warnChat || sender == null) return;
		String ref = suspiciousName(sender);
		MinecraftClient mc = MinecraftClient.getInstance();
		if (ref != null && mc.player != null) {
			mc.player.sendMessage(Text.literal("§6[Fake-Check] §c" + sender + "§e ist NICHT §a" + ref + "§e!"), false);
		}
	}

	/** Vor /pay, /tpa usw. an verdaechtige Namen nachfragen. true = Befehl senden. */
	public static boolean allowCommand(String command) {
		if (!enabled() || !c().confirmCommands) return true;
		String[] parts = command.trim().split("\\s+");
		if (parts.length < 2) return true;
		boolean watched = false;
		for (String cmd : SlyrexConfig.split(c().commands)) if (parts[0].equalsIgnoreCase(cmd)) watched = true;
		if (!watched) return true;
		String ref = suspiciousName(parts[1]);
		if (ref == null) return true;
		long now = System.currentTimeMillis();
		if (command.equals(pendingCommand) && now - pendingTime < 15_000) {
			pendingCommand = null;
			return true;
		}
		pendingCommand = command;
		pendingTime = now;
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.player != null) {
			mc.player.sendMessage(Text.literal("§6[Fake-Check] §c\"" + parts[1] + "\"§e ähnelt §a" + ref
					+ "§e. Befehl NICHT gesendet – innerhalb von 15 Sek. erneut senden, um zu bestätigen."), false);
		}
		return false;
	}

	public static Screen settings(Screen parent) {
		SlyrexConfig.FakeCheck c = c();
		return new OptionsScreen(parent, "Fake-Check", "Prüft nur Ähnlichkeit, nicht Echtheit")
				.field("Bekannte Namen", "Kommagetrennt, z.B. deine Freunde, Admins, Händler", 1024,
						() -> c.trustedNames, v -> { c.trustedNames = v; WARNED.clear(); })
				.field("Clan-Tags", "Kommagetrennt", 512, () -> c.clanTags, v -> { c.clanTags = v; WARNED.clear(); })
				.toggle("Warnung im Chat", "Hinweis, wenn ein ähnlicher Name schreibt", () -> c.warnChat, v -> c.warnChat = v)
				.toggle("Befehle bestätigen", "Vor /pay, /tpa usw. an ähnliche Namen nachfragen",
						() -> c.confirmCommands, v -> c.confirmCommands = v)
				.field("Geprüfte Befehle", "Kommagetrennt, ohne /", 256, () -> c.commands, v -> c.commands = v);
	}
}
