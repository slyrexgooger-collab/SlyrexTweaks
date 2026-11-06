package de.slyrex.module;

import de.slyrex.SlyrexConfig;
import de.slyrex.auktion.Theme;
import de.slyrex.auktion.gui.AnimatedScreen;
import de.slyrex.auktion.gui.Ui;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Lautstaerke einzelner Sounds einstellen oder stummschalten. */
public final class SoundMuffler {
	public static final String ID = "sounds";
	private static final int[] STEPS = {0, 10, 25, 50, 75, 100};
	/** Zuletzt gehoerte Sounds (neueste zuerst) */
	private static final LinkedHashSet<String> RECENT = new LinkedHashSet<>();

	private SoundMuffler() {}

	public static boolean enabled() {
		return SlyrexConfig.get().isEnabled(ID);
	}

	private static Map<String, Integer> volumes() {
		return SlyrexConfig.get().sounds.volumes;
	}

	public static float factor(SoundInstance sound) {
		if (!enabled() || volumes().isEmpty()) return 1f;
		Identifier id = sound.getId();
		if (id == null) return 1f;
		Integer v = volumes().get(id.toString());
		return v == null ? 1f : Math.max(0, Math.min(100, v)) / 100f;
	}

	public static void heard(SoundInstance sound) {
		if (sound == null || sound.getId() == null) return;
		String id = sound.getId().toString();
		synchronized (RECENT) {
			RECENT.remove(id);
			RECENT.add(id);
			if (RECENT.size() > 60) RECENT.remove(RECENT.iterator().next());
		}
	}

	public static Screen settings(Screen parent) {
		return new MufflerScreen(parent);
	}

	static final class MufflerScreen extends AnimatedScreen {
		private static final int W = 380, ROW = 20;
		private final Screen parent;
		private String query = "";
		private boolean all;
		private int page;
		private int x0, y0, h, perPage;
		private final List<String> rows = new ArrayList<>();

		MufflerScreen(Screen parent) {
			super(Text.literal("Sound-Muffler"));
			this.parent = parent;
		}

		private List<String> candidates() {
			Set<String> set = new LinkedHashSet<>(volumes().keySet());
			List<String> recent;
			synchronized (RECENT) {
				recent = new ArrayList<>(RECENT);
			}
			for (int i = recent.size() - 1; i >= 0; i--) set.add(recent.get(i));
			if (all || !query.isEmpty()) {
				for (Identifier id : Registries.SOUND_EVENT.getIds()) set.add(id.toString());
			}
			List<String> out = new ArrayList<>();
			String q = query.toLowerCase(Locale.ROOT);
			for (String s : set) if (q.isEmpty() || s.contains(q)) out.add(s);
			return out;
		}

		@Override
		protected void init() {
			h = Math.min(height - 8, 280);
			x0 = (width - W) / 2;
			y0 = Math.max(4, (height - h) / 2);
			perPage = Math.max(1, (h - 56 - 32) / ROW);
			rows.clear();
			rows.addAll(candidates());
			int pages = Math.max(1, (rows.size() + perPage - 1) / perPage);
			page = Math.max(0, Math.min(page, pages - 1));

			TextFieldWidget search = new TextFieldWidget(textRenderer, x0 + 8, y0 + 24, W - 16 - 110, 18, Text.literal("Suche"));
			search.setPlaceholder(Text.literal("§7Sound suchen, z.B. explode, crystal, totem …"));
			search.setText(query);
			search.setChangedListener(s -> {
				if (s.equals(query)) return;
				query = s;
				page = 0;
				clearAndInit();
				for (var c : children()) if (c instanceof TextFieldWidget tf) { setFocused(tf); tf.setFocused(true); }
			});
			addDrawableChild(search);
			addDrawableChild(ButtonWidget.builder(Text.literal(all ? "Alle Sounds" : "Zuletzt gehört"), b -> {
				all = !all;
				page = 0;
				clearAndInit();
			}).dimensions(x0 + W - 8 - 106, y0 + 24, 106, 18).build());

			int y = y0 + 48;
			for (int i = page * perPage; i < Math.min(rows.size(), (page + 1) * perPage); i++) {
				String id = rows.get(i);
				int vol = volumes().getOrDefault(id, 100);
				addDrawableChild(ButtonWidget.builder(Text.literal(vol == 0 ? "§cStumm" : vol + " %"), b -> {
					int cur = volumes().getOrDefault(id, 100);
					int next = STEPS[0];
					for (int k = 0; k < STEPS.length; k++) if (STEPS[k] == cur) next = STEPS[(k + 1) % STEPS.length];
					if (next == 100) volumes().remove(id);
					else volumes().put(id, next);
					SlyrexConfig.save();
					b.setMessage(Text.literal(next == 0 ? "§cStumm" : next + " %"));
				}).dimensions(x0 + W - 8 - 100, y, 60, 18).build());
				addDrawableChild(ButtonWidget.builder(Text.literal("▶"), b -> preview(id))
						.dimensions(x0 + W - 8 - 36, y, 36, 18).build());
				y += ROW;
			}

			int by = y0 + h - 26;
			if (pages > 1) {
				addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> { page = (page - 1 + pages) % pages; clearAndInit(); })
						.dimensions(x0 + 8, by, 20, 20).build());
				addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> { page = (page + 1) % pages; clearAndInit(); })
						.dimensions(x0 + 30, by, 20, 20).build());
			}
			addDrawableChild(ButtonWidget.builder(Text.literal("Alle zurücksetzen"), b -> {
				volumes().clear();
				SlyrexConfig.save();
				clearAndInit();
			}).dimensions(x0 + W / 2 - 55, by, 110, 20).build());
			addDrawableChild(ButtonWidget.builder(Text.literal("§lFertig"), b -> close()).dimensions(x0 + W - 8 - 80, by, 80, 20).build());
		}

		private static void preview(String id) {
			Identifier ident = Identifier.tryParse(id);
			if (ident == null) return;
			SoundEvent ev = SoundEvent.of(ident);
			MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.ui(ev, 1f));
		}

		@Override
		public void close() {
			SlyrexConfig.save();
			if (client != null) client.setScreen(parent);
		}

		@Override
		protected void renderPanel(DrawContext ctx, int mouseX, int mouseY, float delta) {
			Ui.window(ctx, x0, y0, x0 + W, y0 + h);
			int y = y0 + 48;
			for (int i = page * perPage; i < Math.min(rows.size(), (page + 1) * perPage); i++) {
				ctx.fill(x0 + 8, y - 1, x0 + W - 8, y + 19, (i % 2 == 0) ? Theme.card() : 0x00000000);
				y += ROW;
			}
		}

		@Override
		protected void renderContent(DrawContext ctx, int mouseX, int mouseY, float delta) {
			Ui.text(ctx, textRenderer, "§lSound-Muffler", x0 + 8, y0 + 8, Theme.accent());
			Ui.textRight(ctx, textRenderer, volumes().size() + " angepasst", x0 + W - 8, y0 + 8, Theme.muted());
			int y = y0 + 48;
			for (int i = page * perPage; i < Math.min(rows.size(), (page + 1) * perPage); i++) {
				String id = rows.get(i).replace("minecraft:", "");
				int col = volumes().containsKey(rows.get(i)) ? Theme.accent() : Theme.text();
				Ui.text(ctx, textRenderer, textRenderer.trimToWidth(id, W - 120), x0 + 12, y + 5, col);
				y += ROW;
			}
			if (rows.isEmpty()) Ui.textCenter(ctx, textRenderer, "Noch keine Sounds gehört – spiel kurz und öffne das Fenster erneut.",
					x0 + W / 2, y0 + 70, Theme.muted());
		}
	}
}
