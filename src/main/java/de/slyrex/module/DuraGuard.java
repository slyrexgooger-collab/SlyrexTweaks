package de.slyrex.module;

import de.slyrex.SlyrexConfig;
import de.slyrex.gui.OptionsScreen;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;

import java.util.HashMap;
import java.util.Map;

/** Warnt bei niedriger Haltbarkeit und kann Werkzeuge vor dem Zerbrechen schuetzen. */
public final class DuraGuard {
	public static final String ID = "duraguard";
	private static final EquipmentSlot[] SLOTS = {EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD,
			EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
	/** Slot -> zuletzt gewarnter Item-Zustand, damit nicht jede Sekunde gewarnt wird */
	private static final Map<EquipmentSlot, String> WARNED = new HashMap<>();
	private static int timer;

	private DuraGuard() {}

	private static SlyrexConfig.DuraGuard c() {
		return SlyrexConfig.get().duraGuard;
	}

	public static boolean enabled() {
		return SlyrexConfig.get().isEnabled(ID);
	}

	public static void init() {
		AttackBlockCallback.EVENT.register((player, world, hand, pos, dir) -> check(player, world.isClient(), hand));
		UseBlockCallback.EVENT.register((player, world, hand, hit) -> check(player, world.isClient(), hand));
	}

	private static ActionResult check(PlayerEntity player, boolean client, Hand hand) {
		if (!client || !enabled() || !c().blockTools) return ActionResult.PASS;
		ItemStack s = player.getStackInHand(hand);
		if (isFarmingTool(s) && percent(s) <= c().threshold) {
			MinecraftClient.getInstance().inGameHud.setOverlayMessage(
					Text.literal("DuraGuard: " + s.getName().getString() + " hat nur noch " + percent(s) + " % – blockiert")
							.formatted(Formatting.RED), false);
			return ActionResult.FAIL;
		}
		return ActionResult.PASS;
	}

	private static boolean isFarmingTool(ItemStack s) {
		return s.isIn(ItemTags.PICKAXES) || s.isIn(ItemTags.AXES) || s.isIn(ItemTags.SHOVELS) || s.isIn(ItemTags.HOES)
				|| s.isOf(Items.SHEARS);
	}

	public static int percent(ItemStack s) {
		if (!s.isDamageable() || s.getMaxDamage() <= 0) return 100;
		return Math.round((s.getMaxDamage() - s.getDamage()) * 100f / s.getMaxDamage());
	}

	public static void tick(MinecraftClient mc) {
		if (!enabled() || mc.player == null) return;
		if (++timer % 10 != 0) return;
		for (EquipmentSlot slot : SLOTS) {
			boolean armor = slot != EquipmentSlot.MAINHAND && slot != EquipmentSlot.OFFHAND;
			if (armor ? !c().warnArmor : !c().warnHeld) continue;
			ItemStack s = mc.player.getEquippedStack(slot);
			if (s.isEmpty() || !s.isDamageable()) {
				WARNED.remove(slot);
				continue;
			}
			int pct = percent(s);
			int level = pct <= 5 ? 5 : pct <= 10 ? 10 : pct <= 20 ? 20 : 101;
			if (level > c().threshold) {
				WARNED.remove(slot);
				continue;
			}
			String key = s.getItem() + "@" + level;
			if (key.equals(WARNED.get(slot))) continue;
			WARNED.put(slot, key);
			mc.inGameHud.setOverlayMessage(Text.literal("⚠ " + s.getName().getString() + ": nur noch " + pct + " % Haltbarkeit!")
					.formatted(pct <= 5 ? Formatting.RED : Formatting.GOLD), false);
			if (c().sound) mc.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1f, pct <= 5 ? 0.6f : 1f);
		}
	}

	public static Screen settings(Screen parent) {
		SlyrexConfig.DuraGuard c = c();
		return new OptionsScreen(parent, "DuraGuard", null)
				.cycle("Warnen ab", null, new int[]{5, 10, 20}, () -> c.threshold, v -> c.threshold = v, v -> v + " %")
				.toggle("Items in der Hand", null, () -> c.warnHeld, v -> c.warnHeld = v)
				.toggle("Rüstung", null, () -> c.warnArmor, v -> c.warnArmor = v)
				.toggle("Warnton", null, () -> c.sound, v -> c.sound = v)
				.toggle("Werkzeuge blockieren", "Spitzhacke, Axt, Schaufel, Hacke und Schere unter der Grenze nicht mehr benutzen",
						() -> c.blockTools, v -> c.blockTools = v);
	}
}
