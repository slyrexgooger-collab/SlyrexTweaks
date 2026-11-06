package de.slyrex;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Gemeinsame Einstellungen aller Slyrex-Module, gespeichert in config/slyrex/slyrex.json */
public final class SlyrexConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private static SlyrexConfig instance = new SlyrexConfig();

	/** Modul-ID -> an/aus */
	public Map<String, Boolean> modules = new LinkedHashMap<>();
	/** Favorisierte Module (Herz) */
	public Set<String> favorites = new LinkedHashSet<>();
	/** Position von HUD-Elementen: ID -> {x, y} (Bildschirm-Pixel, links oben) */
	public Map<String, int[]> hud = new HashMap<>();

	public Totem totem = new Totem();
	public Streamer streamer = new Streamer();
	public SecondChat secondChat = new SecondChat();
	public Adblock adblock = new Adblock();
	public DuraGuard duraGuard = new DuraGuard();
	public Crosshair crosshair = new Crosshair();
	public Invsee invsee = new Invsee();
	public Sounds sounds = new Sounds();
	public FakeCheck fakeCheck = new FakeCheck();
	public Rename rename = new Rename();
	public Presets presets = new Presets();
	public Calculator calculator = new Calculator();

	public static final class Totem {
		public int scale = 100;
		public boolean showInventory = true;
		public boolean showPops = true;
		public int maxPlayers = 5;
		/** Automatisch zuruecksetzen nach X Minuten ohne Pop (0 = nie) */
		public int autoResetMinutes = 5;
		public boolean resetOnDeath = true;
	}

	public static final class Streamer {
		public boolean hideDebugInfo = true;
		public boolean hideChatCoords = true;
		public boolean indicator = true;
	}

	public static final class SecondChat {
		public List<ChatBox> boxes = new ArrayList<>();
	}

	public static final class ChatBox {
		public String name = "Chat";
		public boolean enabled = true;
		/** CONTAINS, STARTS_WITH oder REGEX */
		public String mode = "CONTAINS";
		/** Kommagetrennt; %me% = eigener Name */
		public String patterns = "";
		public boolean removeFromMain = true;
		public int width = 220;
		public int lines = 6;
		public int fadeSeconds = 15;

		public ChatBox() {}

		public ChatBox(String name, String mode, String patterns) {
			this.name = name;
			this.mode = mode;
			this.patterns = patterns;
		}
	}

	public static final class Adblock {
		public boolean links = true;
		public boolean trade = true;
		public boolean tpAds = true;
		public boolean streams = true;
		public String customWords = "";
		/** Raenge, die nie gefiltert werden (kommagetrennt, z.B. [Admin],[Mod]) */
		public String exceptions = "[Admin],[Mod],[Owner],[Team]";
		public boolean showCounter = false;
	}

	public static final class DuraGuard {
		public int threshold = 10;
		public boolean warnArmor = true;
		public boolean warnHeld = true;
		public boolean sound = true;
		public boolean blockTools = false;
	}

	public static final class Crosshair {
		/** Kommagetrennt, 1-5 Farben als #RRGGBB */
		public String colors = "#FFFFFF";
		/** SOLID, GRADIENT, CYCLE */
		public String mode = "SOLID";
		public int size = 5;
		public int gap = 2;
		public int thickness = 1;
		public boolean dot = false;
		public boolean outline = true;
		public boolean reachColorEnabled = true;
		public String reachColor = "#FF5555";
		public boolean hitColorEnabled = true;
		public String hitColor = "#FFAA00";
	}

	public static final class Invsee {
		/** Kommagetrennte Item-IDs (max. 10) */
		public String items = "minecraft:totem_of_undying,minecraft:end_crystal";
		/** Fenstertitel muss eines dieser Woerter enthalten (leer = alle Container) */
		public String titleKeywords = "invsee,inventar,inventory";
	}

	public static final class Sounds {
		/** Sound-ID -> Lautstaerke in Prozent (0 = stumm) */
		public Map<String, Integer> volumes = new LinkedHashMap<>();
	}

	public static final class FakeCheck {
		/** Namen, die du kennst/denen du vertraust (kommagetrennt) */
		public String trustedNames = "";
		/** Clan-Tags (kommagetrennt) */
		public String clanTags = "";
		public boolean confirmCommands = true;
		public boolean warnChat = true;
		public String commands = "pay,tpa,tpahere,tpaccept,trade,msg";
	}

	public static final class Rename {
		public String text = "Slyrex";
		public String colors = "#FF5555,#FFAA00";
		public boolean bold = false;
		public boolean italic = false;
		/** AMPERSAND_HEX (&#RRGGBB), SECTION (&x&R&R..), MINIMESSAGE */
		public String format = "AMPERSAND_HEX";
		public String command = "/rename";
	}

	public static final class Presets {
		public List<Preset> list = new ArrayList<>();
		public String active = "";
		public boolean showGhosts = true;
	}

	public static final class Preset {
		public String name = "Preset";
		public String icon = "minecraft:totem_of_undying";
		/** 41 Slots: 0-8 Hotbar, 9-35 Inventar, 36-39 Ruestung, 40 Offhand. Item-ID oder "" */
		public String[] slots = new String[41];
	}

	public static final class Calculator {
		public List<String> history = new ArrayList<>();
	}

	// ------------------------------------------------------------------

	public static SlyrexConfig get() {
		return instance;
	}

	public boolean isEnabled(String id) {
		return modules.getOrDefault(id, Boolean.TRUE);
	}

	public void setEnabled(String id, boolean on) {
		modules.put(id, on);
		save();
	}

	public int[] hudPos(String id, int defX, int defY) {
		int[] p = hud.get(id);
		if (p == null || p.length != 2) {
			p = new int[]{defX, defY};
			hud.put(id, p);
		}
		return p;
	}

	public static Path dir() {
		return FabricLoader.getInstance().getConfigDir().resolve("slyrex");
	}

	private static Path path() {
		return dir().resolve("slyrex.json");
	}

	public static void load() {
		Path p = path();
		if (Files.exists(p)) {
			try (Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
				SlyrexConfig c = GSON.fromJson(r, SlyrexConfig.class);
				if (c != null) instance = c;
			} catch (Exception e) {
				Slyrex.LOGGER.warn("Konnte slyrex.json nicht lesen, benutze Standardwerte", e);
			}
		}
		instance.fillDefaults();
		save();
	}

	private void fillDefaults() {
		if (modules == null) modules = new LinkedHashMap<>();
		if (favorites == null) favorites = new LinkedHashSet<>();
		if (hud == null) hud = new HashMap<>();
		if (totem == null) totem = new Totem();
		if (streamer == null) streamer = new Streamer();
		if (secondChat == null) secondChat = new SecondChat();
		if (secondChat.boxes == null) secondChat.boxes = new ArrayList<>();
		if (adblock == null) adblock = new Adblock();
		if (duraGuard == null) duraGuard = new DuraGuard();
		if (crosshair == null) crosshair = new Crosshair();
		if (invsee == null) invsee = new Invsee();
		if (sounds == null) sounds = new Sounds();
		if (sounds.volumes == null) sounds.volumes = new LinkedHashMap<>();
		if (fakeCheck == null) fakeCheck = new FakeCheck();
		if (rename == null) rename = new Rename();
		if (presets == null) presets = new Presets();
		if (presets.list == null) presets.list = new ArrayList<>();
		for (Preset pr : presets.list) {
			if (pr.slots == null || pr.slots.length != 41) {
				String[] s = new String[41];
				if (pr.slots != null) System.arraycopy(pr.slots, 0, s, 0, Math.min(41, pr.slots.length));
				pr.slots = s;
			}
		}
		if (calculator == null) calculator = new Calculator();
		if (calculator.history == null) calculator.history = new ArrayList<>();
		if (secondChat.boxes.isEmpty()) {
			secondChat.boxes.add(new ChatBox("Privat", "CONTAINS", "-> mir,-> me,» mir,flüstert,whispers,[msg]"));
			ChatBox tpa = new ChatBox("Teleport", "CONTAINS",
					"teleportieren,teleport to you,teleport to them,tpa-anfrage,hat dir eine anfrage");
			tpa.enabled = false;
			secondChat.boxes.add(tpa);
			ChatBox mention = new ChatBox("Erwähnungen", "CONTAINS", "%me%");
			mention.enabled = false;
			mention.removeFromMain = false;
			secondChat.boxes.add(mention);
		}
		while (secondChat.boxes.size() > 3) secondChat.boxes.remove(secondChat.boxes.size() - 1);
	}

	public static void save() {
		try {
			Files.createDirectories(dir());
			try (Writer w = Files.newBufferedWriter(path(), StandardCharsets.UTF_8)) {
				GSON.toJson(instance, w);
			}
		} catch (Exception e) {
			Slyrex.LOGGER.warn("Konnte slyrex.json nicht speichern", e);
		}
	}

	/** Kommagetrennte Liste in getrimmte, nicht-leere Eintraege zerlegen. */
	public static List<String> split(String s) {
		List<String> out = new ArrayList<>();
		if (s == null) return out;
		for (String part : s.split(",")) {
			String t = part.trim();
			if (!t.isEmpty()) out.add(t);
		}
		return out;
	}
}
