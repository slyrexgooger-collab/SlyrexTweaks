package de.slyrex.module;

import de.slyrex.SlyrexConfig;
import de.slyrex.auktion.Theme;
import de.slyrex.gui.OptionsScreen;
import de.slyrex.hud.HudEditorScreen;
import de.slyrex.hud.SlyrexHud;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Zaehlt Totems im Inventar und beobachtete Totem-Pops anderer Spieler. */
public final class TotemCounter {
	public static final String ID = "totem";
	private static final ItemStack ICON = new ItemStack(Items.TOTEM_OF_UNDYING);
	/** Name -> {Pops, letzte Zeit} */
	private static final Map<String, long[]> POPS = new LinkedHashMap<>();

	private TotemCounter() {}

	private static SlyrexConfig.Totem c() {
		return SlyrexConfig.get().totem;
	}

	public static boolean enabled() {
		return SlyrexConfig.get().isEnabled(ID);
	}

	public static void init() {
		SlyrexHud.register("totem", "Totem-Zähler", 0.02f, 0.45f, TotemCounter::enabled,
				() -> Math.round(width() * c().scale / 100f), () -> Math.round(height(true) * c().scale / 100f),
				TotemCounter::render);
	}

	/** Aufgerufen aus dem Netzwerk-Mixin bei Entity-Status 35 (Totem ausgeloest) bzw. 3 (Tod). */
	public static void onEntityStatus(Entity entity, byte status) {
		if (!(entity instanceof PlayerEntity p)) return;
		String name = p.getName().getString();
		if (status == 35) {
			long[] v = POPS.computeIfAbsent(name, k -> new long[2]);
			v[0]++;
			v[1] = System.currentTimeMillis();
		} else if (status == 3 && c().resetOnDeath) {
			POPS.remove(name);
		}
	}

	public static void reset() {
		POPS.clear();
	}

	public static void tick() {
		int min = c().autoResetMinutes;
		if (min <= 0 || POPS.isEmpty()) return;
		long limit = System.currentTimeMillis() - min * 60_000L;
		POPS.values().removeIf(v -> v[1] < limit);
	}

	private static int countTotems() {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.player == null) return 0;
		PlayerInventory inv = mc.player.getInventory();
		int n = 0;
		for (int i = 0; i < inv.size(); i++) {
			ItemStack s = inv.getStack(i);
			if (s.isOf(Items.TOTEM_OF_UNDYING)) n += s.getCount();
		}
		return n;
	}

	private static List<Map.Entry<String, long[]>> topPops() {
		List<Map.Entry<String, long[]>> list = new ArrayList<>(POPS.entrySet());
		list.sort((a, b) -> Long.compare(b.getValue()[1], a.getValue()[1]));
		return list.subList(0, Math.min(list.size(), c().maxPlayers));
	}

	private static int width() {
		return 110;
	}

	private static int height(boolean preview) {
		int rows = c().showInventory ? 1 : 0;
		if (c().showPops && (preview || !POPS.isEmpty())) rows += 1 + Math.max(preview ? 2 : 0, topPops().size());
		return 6 + Math.max(1, rows) * 11;
	}

	private static void render(DrawContext ctx, int x, int y, boolean preview) {
		MinecraftClient mc = MinecraftClient.getInstance();
		var tr = mc.textRenderer;
		float s = c().scale / 100f;
		ctx.getMatrices().pushMatrix();
		ctx.getMatrices().translate(x, y);
		ctx.getMatrices().scale(s, s);
		int w = width(), h = height(preview);
		ctx.fill(0, 0, w, h, Theme.background());
		ctx.fill(0, 0, 2, h, Theme.accent());
		int ty = 4;
		if (c().showInventory) {
			int n = preview && mc.player == null ? 3 : countTotems();
			ctx.drawItem(ICON, 4, ty - 4);
			int col = n == 0 ? Theme.bad() : n <= 2 ? 0xFFFFDD55 : Theme.good();
			ctx.drawTextWithShadow(tr, "Totems: " + n, 22, ty + 1, col);
			ty += 14;
		}
		if (c().showPops && (preview || !POPS.isEmpty())) {
			ctx.drawTextWithShadow(tr, "§lPops", 6, ty, Theme.accent());
			ty += 11;
			List<Map.Entry<String, long[]>> list = topPops();
			if (list.isEmpty() && preview) {
				ctx.drawTextWithShadow(tr, "Alex", 6, ty, Theme.text());
				ctx.drawTextWithShadow(tr, "×3", w - 6 - tr.getWidth("×3"), ty, Theme.accent());
				ty += 11;
				ctx.drawTextWithShadow(tr, "Steve", 6, ty, Theme.text());
				ctx.drawTextWithShadow(tr, "×1", w - 6 - tr.getWidth("×1"), ty, Theme.accent());
			}
			for (Map.Entry<String, long[]> e : list) {
				String cnt = "×" + e.getValue()[0];
				ctx.drawTextWithShadow(tr, tr.trimToWidth(e.getKey(), w - 30), 6, ty, Theme.text());
				ctx.drawTextWithShadow(tr, cnt, w - 6 - tr.getWidth(cnt), ty, Theme.accent());
				ty += 11;
			}
		}
		ctx.getMatrices().popMatrix();
	}

	public static Screen settings(Screen parent) {
		SlyrexConfig.Totem c = c();
		OptionsScreen[] self = new OptionsScreen[1];
		self[0] = new OptionsScreen(parent, "Totem-Zähler", "Position: HUD-Editor")
				.toggle("Totems im Inventar", null, () -> c.showInventory, v -> c.showInventory = v)
				.toggle("Pops anderer Spieler", "Zählt, wie oft Spieler in deiner Nähe ein Totem auslösen",
						() -> c.showPops, v -> c.showPops = v)
				.cycle("Max. Spieler", null, new int[]{1, 3, 5, 8, 10}, () -> c.maxPlayers, v -> c.maxPlayers = v, v -> v + "")
				.cycle("Größe", null, new int[]{50, 75, 100, 125, 150, 200}, () -> c.scale, v -> c.scale = v, v -> v + " %")
				.cycle("Auto-Reset", "Pops vergessen, wenn ein Spieler so lange nicht gepoppt hat",
						new int[]{0, 1, 2, 5, 10, 30}, () -> c.autoResetMinutes, v -> c.autoResetMinutes = v,
						v -> v == 0 ? "Nie" : v + " Min.")
				.toggle("Reset beim Tod", "Pops eines Spielers löschen, wenn er stirbt", () -> c.resetOnDeath, v -> c.resetOnDeath = v)
				.action("Pops zurücksetzen", "Jetzt zurücksetzen", null, TotemCounter::reset)
				.footer("HUD-Editor", () -> MinecraftClient.getInstance().setScreen(new HudEditorScreen(self[0])));
		return self[0];
	}
}
