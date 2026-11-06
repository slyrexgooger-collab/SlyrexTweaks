package de.slyrex.module;

import de.slyrex.SlyrexConfig;
import de.slyrex.auktion.Theme;
import de.slyrex.gui.OptionsScreen;
import de.slyrex.mixin.HandledScreenAccessor;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Zaehlt ausgewaehlte Items in fremden Inventaren (z.B. /invsee) – das eigene Inventar wird ignoriert. */
public final class InvseeCounter {
	public static final String ID = "invsee";

	private InvseeCounter() {}

	public static boolean enabled() {
		return SlyrexConfig.get().isEnabled(ID);
	}

	private static SlyrexConfig.Invsee c() {
		return SlyrexConfig.get().invsee;
	}

	public static void init() {
		ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
			if (!(screen instanceof HandledScreen<?> hs) || screen instanceof InventoryScreen) return;
			ScreenEvents.afterRender(screen).register((s, ctx, mx, my, delta) -> {
				if (enabled() && titleMatches(s)) render(ctx, hs);
			});
		});
	}

	private static boolean titleMatches(Screen s) {
		List<String> words = SlyrexConfig.split(c().titleKeywords);
		if (words.isEmpty()) return true;
		String title = s.getTitle().getString().toLowerCase(Locale.ROOT);
		for (String w : words) if (title.contains(w.toLowerCase(Locale.ROOT))) return true;
		return false;
	}

	private static List<Item> items() {
		List<Item> out = new ArrayList<>();
		for (String id : SlyrexConfig.split(c().items)) {
			Identifier ident = Identifier.tryParse(id.contains(":") ? id : "minecraft:" + id);
			if (ident == null || !Registries.ITEM.containsId(ident)) continue;
			out.add(Registries.ITEM.get(ident));
			if (out.size() >= 10) break;
		}
		return out;
	}

	private static void render(DrawContext ctx, HandledScreen<?> screen) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.player == null) return;
		List<Item> items = items();
		if (items.isEmpty()) return;
		PlayerInventory own = mc.player.getInventory();
		int[] counts = new int[items.size()];
		for (Slot slot : screen.getScreenHandler().slots) {
			if (slot.inventory == own) continue;
			ItemStack st = slot.getStack();
			if (st.isEmpty()) continue;
			for (int i = 0; i < items.size(); i++) if (st.isOf(items.get(i))) counts[i] += st.getCount();
		}
		HandledScreenAccessor acc = (HandledScreenAccessor) screen;
		int w = 70, h = 8 + items.size() * 18;
		int x = acc.slyrex$getX() - w - 4;
		int y = acc.slyrex$getY();
		if (x < 2) x = acc.slyrex$getX() + acc.slyrex$getBackgroundWidth() + 4;
		var tr = mc.textRenderer;
		ctx.getMatrices().pushMatrix();
		ctx.fill(x, y, x + w, y + h, Theme.background());
		ctx.fill(x, y, x + w, y + 2, Theme.accent());
		int ty = y + 5;
		for (int i = 0; i < items.size(); i++) {
			ctx.drawItem(new ItemStack(items.get(i)), x + 4, ty);
			String n = "× " + counts[i];
			ctx.drawTextWithShadow(tr, n, x + 24, ty + 4, counts[i] == 0 ? Theme.muted() : Theme.text());
			ty += 18;
		}
		ctx.getMatrices().popMatrix();
	}

	public static Screen settings(Screen parent) {
		SlyrexConfig.Invsee c = c();
		return new OptionsScreen(parent, "Invsee-Zähler", "Eigenes Inventar zählt nicht")
				.field("Items (max. 10)", "Item-IDs kommagetrennt, z.B. totem_of_undying,end_crystal,golden_apple", 512,
						() -> c.items, v -> c.items = v)
				.field("Fenstertitel enthält", "Nur zählen, wenn der Titel eines dieser Wörter enthält. Leer = in jedem Container",
						256, () -> c.titleKeywords, v -> c.titleKeywords = v);
	}
}
