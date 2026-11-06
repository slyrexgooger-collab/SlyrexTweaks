package de.slyrex.module;

import com.google.gson.Gson;
import de.slyrex.SlyrexConfig;
import de.slyrex.SlyrexConfig.Preset;
import de.slyrex.auktion.Theme;
import de.slyrex.auktion.gui.AnimatedScreen;
import de.slyrex.auktion.gui.Ui;
import de.slyrex.mixin.HandledScreenAccessor;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Gespeicherte Inventar-Layouts mit Geister-Items und Anzeige fehlender Items. */
public final class FightPresets {
	public static final String ID = "presets";
	private static final Gson GSON = new Gson();
	private static final String[] ICONS = {"minecraft:totem_of_undying", "minecraft:end_crystal", "minecraft:netherite_sword",
			"minecraft:mace", "minecraft:netherite_axe", "minecraft:splash_potion", "minecraft:golden_apple", "minecraft:bow",
			"minecraft:respawn_anchor", "minecraft:ender_pearl", "minecraft:trident", "minecraft:wind_charge"};

	private FightPresets() {}

	public static boolean enabled() {
		return SlyrexConfig.get().isEnabled(ID);
	}

	private static SlyrexConfig.Presets c() {
		return SlyrexConfig.get().presets;
	}

	public static Preset active() {
		for (Preset p : c().list) if (p.name.equals(c().active)) return p;
		return null;
	}

	static Item item(String id) {
		if (id == null || id.isEmpty()) return Items.AIR;
		Identifier ident = Identifier.tryParse(id);
		return ident == null || !Registries.ITEM.containsId(ident) ? Items.AIR : Registries.ITEM.get(ident);
	}

	public static void init() {
		ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
			if (!(screen instanceof InventoryScreen inv)) return;
			ScreenEvents.afterRender(screen).register((s, ctx, mx, my, delta) -> {
				if (enabled() && c().showGhosts && active() != null) renderOverlay(ctx, inv);
			});
		});
	}

	public static Preset capture(String name) {
		MinecraftClient mc = MinecraftClient.getInstance();
		Preset p = new Preset();
		p.name = name;
		if (mc.player == null) return p;
		PlayerInventory inv = mc.player.getInventory();
		for (int i = 0; i < 41 && i < inv.size(); i++) {
			ItemStack s = inv.getStack(i);
			p.slots[i] = s.isEmpty() ? "" : Registries.ITEM.getId(s.getItem()).toString();
		}
		ItemStack hotbar0 = inv.getStack(0);
		if (!hotbar0.isEmpty()) p.icon = Registries.ITEM.getId(hotbar0.getItem()).toString();
		return p;
	}

	private static void renderOverlay(DrawContext ctx, InventoryScreen screen) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.player == null) return;
		Preset p = active();
		PlayerInventory own = mc.player.getInventory();
		HandledScreenAccessor acc = (HandledScreenAccessor) screen;
		int ox = acc.slyrex$getX(), oy = acc.slyrex$getY();
		int wrong = 0;
		for (Slot slot : screen.getScreenHandler().slots) {
			if (slot.inventory != own) continue;
			int idx = slot.getIndex();
			if (idx < 0 || idx >= 41) continue;
			Item want = item(p.slots[idx]);
			if (want == Items.AIR) continue;
			ItemStack have = slot.getStack();
			if (have.isOf(want)) continue;
			int sx = ox + slot.x, sy = oy + slot.y;
			if (have.isEmpty()) {
				ctx.drawItem(new ItemStack(want), sx, sy);
				ctx.fill(sx, sy, sx + 16, sy + 16, 0x90000000);
			} else {
				Ui.outline(ctx, sx - 1, sy - 1, sx + 17, sy + 17, 0xFFFF5555);
			}
			wrong++;
		}

		// Fehlende Items insgesamt
		Map<Item, Integer> need = new LinkedHashMap<>();
		for (String id : p.slots) {
			Item it = item(id);
			if (it != Items.AIR) need.merge(it, 1, Integer::sum);
		}
		Map<Item, Integer> missing = new LinkedHashMap<>();
		for (Map.Entry<Item, Integer> e : need.entrySet()) {
			int slotsWith = 0;
			for (int i = 0; i < own.size(); i++) if (own.getStack(i).isOf(e.getKey())) slotsWith++;
			if (slotsWith < e.getValue()) missing.put(e.getKey(), e.getValue() - slotsWith);
		}
		var tr = mc.textRenderer;
		int px = ox - 84, py = oy;
		if (px < 2) return;
		int ph = 26 + Math.max(1, Math.min(6, missing.size())) * 18;
		ctx.fill(px, py, px + 80, py + ph, Theme.background());
		ctx.fill(px, py, px + 80, py + 2, Theme.accent());
		ctx.drawTextWithShadow(tr, tr.trimToWidth(p.name, 74), px + 4, py + 5, Theme.accent());
		ctx.drawTextWithShadow(tr, wrong == 0 ? "§a✔ Sortiert" : "§e" + wrong + " Slots falsch", px + 4, py + 15, Theme.text());
		int y = py + 26;
		if (missing.isEmpty()) {
			ctx.drawTextWithShadow(tr, "§7Nichts fehlt", px + 4, y + 4, Theme.muted());
		}
		int shown = 0;
		for (Map.Entry<Item, Integer> e : missing.entrySet()) {
			if (shown++ >= 6) break;
			ctx.drawItem(new ItemStack(e.getKey()), px + 4, y);
			ctx.drawTextWithShadow(tr, "§cfehlt ×" + e.getValue(), px + 22, y + 4, Theme.text());
			y += 18;
		}
	}

	static String export(Preset p) {
		return "SLYREX:" + Base64.getEncoder().encodeToString(GSON.toJson(p).getBytes(StandardCharsets.UTF_8));
	}

	static Preset importCode(String code) {
		try {
			String s = code.trim();
			if (s.startsWith("SLYREX:")) s = s.substring(7);
			Preset p = GSON.fromJson(new String(Base64.getDecoder().decode(s), StandardCharsets.UTF_8), Preset.class);
			if (p == null || p.slots == null) return null;
			if (p.slots.length != 41) {
				String[] n = new String[41];
				System.arraycopy(p.slots, 0, n, 0, Math.min(41, p.slots.length));
				p.slots = n;
			}
			return p;
		} catch (Exception e) {
			return null;
		}
	}

	public static Screen settings(Screen parent) {
		return new PresetScreen(parent);
	}

	static final class PresetScreen extends AnimatedScreen {
		private static final int W = 360, ROW = 22;
		private final Screen parent;
		private int x0, y0, h, page, perPage;
		private String message = "";
		private String newName = "";

		PresetScreen(Screen parent) {
			super(Text.literal("Fight-Presets"));
			this.parent = parent;
		}

		@Override
		protected void init() {
			h = Math.min(height - 8, 260);
			x0 = (width - W) / 2;
			y0 = Math.max(4, (height - h) / 2);
			perPage = Math.max(1, (h - 52 - 58) / ROW);
			List<Preset> list = c().list;
			int pages = Math.max(1, (list.size() + perPage - 1) / perPage);
			page = Math.max(0, Math.min(page, pages - 1));

			TextFieldWidget name = new TextFieldWidget(textRenderer, x0 + 8, y0 + 24, W - 16 - 150, 18, Text.literal("Name"));
			name.setPlaceholder(Text.literal("§7Name, z.B. Crystal"));
			name.setMaxLength(24);
			name.setText(newName);
			name.setChangedListener(s -> newName = s);
			addDrawableChild(name);
			addDrawableChild(ButtonWidget.builder(Text.literal("Inventar speichern"), b -> {
				String n = newName.isBlank() ? "Preset " + (list.size() + 1) : newName.trim();
				list.removeIf(p -> p.name.equals(n));
				list.add(capture(n));
				c().active = n;
				newName = "";
				message = "§aGespeichert: " + n;
				SlyrexConfig.save();
				clearAndInit();
			}).tooltip(Tooltip.of(Text.literal("Speichert dein aktuelles Inventar-Layout"))).dimensions(x0 + W - 8 - 146, y0 + 24, 146, 18).build());

			int y = y0 + 50;
			for (int i = page * perPage; i < Math.min(list.size(), (page + 1) * perPage); i++) {
				Preset p = list.get(i);
				boolean act = p.name.equals(c().active);
				int bx = x0 + W - 8;
				addDrawableChild(ButtonWidget.builder(Text.literal("§c✕"), b -> {
					list.remove(p);
					if (act) c().active = "";
					SlyrexConfig.save();
					clearAndInit();
				}).tooltip(Tooltip.of(Text.literal("Löschen"))).dimensions(bx - 20, y, 20, 18).build());
				addDrawableChild(ButtonWidget.builder(Text.literal("Export"), b -> {
					client.keyboard.setClipboard(export(p));
					message = "§aCode in Zwischenablage kopiert";
				}).dimensions(bx - 72, y, 50, 18).build());
				addDrawableChild(ButtonWidget.builder(Text.literal("Icon"), b -> {
					int k = 0;
					for (int j = 0; j < ICONS.length; j++) if (ICONS[j].equals(p.icon)) k = (j + 1) % ICONS.length;
					p.icon = ICONS[k];
					SlyrexConfig.save();
				}).dimensions(bx - 112, y, 38, 18).build());
				addDrawableChild(ButtonWidget.builder(Text.literal(act ? "§aAktiv" : "Aktivieren"), b -> {
					c().active = act ? "" : p.name;
					SlyrexConfig.save();
					clearAndInit();
				}).dimensions(bx - 180, y, 66, 18).build());
				y += ROW;
			}

			int by = y0 + h - 26;
			int bx = x0 + 8;
			if (pages > 1) {
				addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> { page = (page - 1 + pages) % pages; clearAndInit(); })
						.dimensions(bx, by, 20, 20).build());
				addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> { page = (page + 1) % pages; clearAndInit(); })
						.dimensions(bx + 22, by, 20, 20).build());
				bx += 46;
			}
			addDrawableChild(ButtonWidget.builder(Text.literal("Import"), b -> {
				Preset p = importCode(client.keyboard.getClipboard());
				if (p == null) {
					message = "§cKein gültiger Code in der Zwischenablage";
				} else {
					list.removeIf(q -> q.name.equals(p.name));
					list.add(p);
					message = "§aImportiert: " + p.name;
					SlyrexConfig.save();
				}
				clearAndInit();
			}).tooltip(Tooltip.of(Text.literal("Code aus der Zwischenablage einfügen"))).dimensions(bx, by, 56, 20).build());
			addDrawableChild(ButtonWidget.builder(Text.literal("Geister: " + (c().showGhosts ? "§aAN" : "§cAUS")), b -> {
				c().showGhosts = !c().showGhosts;
				SlyrexConfig.save();
				b.setMessage(Text.literal("Geister: " + (c().showGhosts ? "§aAN" : "§cAUS")));
			}).tooltip(Tooltip.of(Text.literal("Fehlende Items als Schatten im Inventar anzeigen"))).dimensions(bx + 60, by, 84, 20).build());
			addDrawableChild(ButtonWidget.builder(Text.literal("§lFertig"), b -> close()).dimensions(x0 + W - 8 - 70, by, 70, 20).build());
		}

		@Override
		public void close() {
			SlyrexConfig.save();
			if (client != null) client.setScreen(parent);
		}

		@Override
		protected void renderPanel(DrawContext ctx, int mouseX, int mouseY, float delta) {
			Ui.window(ctx, x0, y0, x0 + W, y0 + h);
			int y = y0 + 50;
			List<Preset> list = c().list;
			for (int i = page * perPage; i < Math.min(list.size(), (page + 1) * perPage); i++) {
				ctx.fill(x0 + 8, y - 1, x0 + W - 8, y + 19, list.get(i).name.equals(c().active) ? Theme.cardHover() : Theme.card());
				y += ROW;
			}
		}

		@Override
		protected void renderContent(DrawContext ctx, int mouseX, int mouseY, float delta) {
			Ui.text(ctx, textRenderer, "§lFight-Presets", x0 + 8, y0 + 8, Theme.accent());
			Ui.textRight(ctx, textRenderer, "Aktives Preset wird im Inventar angezeigt", x0 + W - 8, y0 + 8, Theme.muted());
			int y = y0 + 50;
			List<Preset> list = c().list;
			for (int i = page * perPage; i < Math.min(list.size(), (page + 1) * perPage); i++) {
				Preset p = list.get(i);
				ctx.drawItem(new ItemStack(item(p.icon)), x0 + 11, y + 1);
				Ui.text(ctx, textRenderer, textRenderer.trimToWidth(p.name, W - 220), x0 + 32, y + 5, Theme.text());
				y += ROW;
			}
			if (list.isEmpty()) Ui.textCenter(ctx, textRenderer, "Sortiere dein Inventar und klicke „Inventar speichern“.",
					x0 + W / 2, y0 + 70, Theme.muted());
			if (!message.isEmpty()) Ui.textCenter(ctx, textRenderer, message, x0 + W / 2, y0 + h - 40, Theme.text());
		}
	}
}
