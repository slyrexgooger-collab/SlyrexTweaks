package de.slyrex.module;

import de.slyrex.SlyrexConfig;
import de.slyrex.gui.OptionsScreen;
import net.minecraft.client.gui.screen.Screen;

import java.util.Locale;
import java.util.regex.Pattern;

/** Blendet Werbung im Chat aus (Streams, Handel, TP-Werbung, Links). Echte TPA-Anfragen bleiben sichtbar. */
public final class Adblocker {
	public static final String ID = "adblock";
	private static final Pattern LINKS = Pattern.compile(
			"(?i)(https?://|www\\.|discord\\.gg|dsc\\.gg|\\b[a-z0-9-]{2,}\\.(de|com|net|org|eu|gg|tv|io|me|ly)\\b)");
	private static final Pattern STREAMS = Pattern.compile(
			"(?i)(twitch|youtube|yt\\.|tiktok|kick\\.com|\\blive\\b|stream|\\bsub(s|t)?\\b|follow)");
	private static final Pattern TRADE = Pattern.compile(
			"(?i)(\\bverkaufe?\\b|\\bkaufe?\\b|\\bbiete\\b|\\bsuche\\b|\\btausche?\\b|\\bwts\\b|\\bwtb\\b|\\bselling\\b|\\bbuying\\b|"
					+ "günstig|billig|angebot|\\bpreis\\b|/ah\\b|/shop\\b|\\bpw\\b|\\bpayment\\b)");
	private static final Pattern TP_ADS = Pattern.compile(
			"(?i)(tpa\\s*(zu\\s*)?mir|tp\\s*(zu\\s*)?mir|/tpa\\s+\\S+|free\\s*tp|tp\\s*gratis|kostenlos(er|e)?\\s*tp|tpa\\s*me)");
	/** Systemnachrichten echter Teleport-Anfragen – nie filtern */
	private static final Pattern REAL_TPA = Pattern.compile(
			"(?i)(möchte sich zu dir teleportieren|möchte, dass du dich zu|has requested to teleport|"
					+ "requested that you teleport|anfrage (gesendet|erhalten)|/tpaccept|/tpdeny)");

	public static int blocked;

	private Adblocker() {}

	private static SlyrexConfig.Adblock c() {
		return SlyrexConfig.get().adblock;
	}

	public static boolean enabled() {
		return SlyrexConfig.get().isEnabled(ID);
	}

	/** true = Nachricht ausblenden */
	public static boolean shouldBlock(String raw, boolean playerChat) {
		if (!enabled() || raw == null || raw.isBlank()) return false;
		SlyrexConfig.Adblock c = c();
		String msg = raw.trim();
		for (String ex : SlyrexConfig.split(c.exceptions)) {
			if (msg.toLowerCase(Locale.ROOT).startsWith(ex.toLowerCase(Locale.ROOT))) return false;
		}
		// Systemnachrichten nur pruefen, wenn sie wie Spieler-Chat aussehen ("Name: text" / "Name » text")
		String body = msg;
		if (!playerChat) {
			if (REAL_TPA.matcher(msg).find()) return false;
			int sep = indexOfSeparator(msg);
			if (sep < 0) return false;
			body = msg.substring(sep);
		}
		boolean hit = (c.links && LINKS.matcher(body).find())
				|| (c.streams && STREAMS.matcher(body).find())
				|| (c.trade && TRADE.matcher(body).find())
				|| (c.tpAds && TP_ADS.matcher(body).find());
		if (!hit) {
			String low = body.toLowerCase(Locale.ROOT);
			for (String w : SlyrexConfig.split(c.customWords)) {
				if (low.contains(w.toLowerCase(Locale.ROOT))) {
					hit = true;
					break;
				}
			}
		}
		if (hit) blocked++;
		return hit;
	}

	private static int indexOfSeparator(String msg) {
		int best = -1;
		for (String sep : new String[]{": ", "» ", ">> ", "> ", "| "}) {
			int i = msg.indexOf(sep);
			if (i > 0 && i < 48 && (best < 0 || i < best)) best = i + sep.length();
		}
		return best;
	}

	public static Screen settings(Screen parent) {
		SlyrexConfig.Adblock c = c();
		return new OptionsScreen(parent, "Adblocker", null)
				.toggle("Links / Discord", null, () -> c.links, v -> c.links = v)
				.toggle("Livestreams", "Twitch, YouTube, TikTok, \"live\" …", () -> c.streams, v -> c.streams = v)
				.toggle("Handel", "Verkaufe, kaufe, biete, suche, /ah …", () -> c.trade, v -> c.trade = v)
				.toggle("TP / TPA-Werbung", "\"tpa zu mir\", \"free tp\" … – echte Anfragen bleiben sichtbar",
						() -> c.tpAds, v -> c.tpAds = v)
				.field("Eigene Wörter", "Kommagetrennt, Groß-/Kleinschreibung egal", 256, () -> c.customWords, v -> c.customWords = v)
				.field("Ausnahmen (Ränge)", "Nachrichten, die damit beginnen, werden nie gefiltert", 256,
						() -> c.exceptions, v -> c.exceptions = v)
				.action("Ausgeblendet", "§7" + blocked + " Nachrichten", "Seit dem Spielstart", () -> {});
	}
}
