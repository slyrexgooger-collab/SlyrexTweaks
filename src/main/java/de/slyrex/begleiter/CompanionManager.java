package de.slyrex.begleiter;

import com.mojang.authlib.GameProfile;
import de.slyrex.begleiter.screen.DialogueScreen;
import de.slyrex.begleiter.screen.SettingsScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Spawnt/entfernt den Begleiter und kümmert sich um spontane Reaktionen. */
public final class CompanionManager {
    private static final int ENTITY_ID = -13374242;
    private static final UUID UUID_ = UUID.nameUUIDFromBytes("begleiter-companion".getBytes());

    private static CompanionEntity companion;

    private static int idleTimer = 600;
    private static int reactCooldown = 0;
    private static int hitCooldown = 0;
    private static int stareTicks = 0;
    private static int lookAwayTicks = 0;
    private static int lastOwnerHurt = 0;
    private static boolean wasNight = false;

    public static CompanionEntity get() {
        return companion;
    }

    public static boolean isLookingAway() {
        return lookAwayTicks > 0;
    }

    public static void forget() {
        companion = null;
    }


    public static void tick(MinecraftClient mc) {
        while (BegleiterClient.menuKey.wasPressed()) {
            mc.setScreen(new SettingsScreen(mc.currentScreen));
        }
        while (BegleiterClient.talkKey.wasPressed()) {
            if (companion != null && mc.player != null && companion.distanceTo(mc.player) < 8) {
                openDialogue(mc);
            }
        }

        if (mc.world == null || mc.player == null) {
            companion = null;
            return;
        }
        if (!Config.get().enabled) {
            despawn();
            return;
        }
        if (companion == null || companion.isRemoved() || companion.getEntityWorld() != mc.world) {
            spawn(mc);
        }
        if (mc.isPaused()) return;
        tickReactions(mc);
    }

    private static void spawn(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        CompanionEntity c = new CompanionEntity(mc.world, new GameProfile(UUID_, "Begleiter"));
        c.setId(ENTITY_ID);
        companion = c;
        c.refreshPositionAndAngles(p.getX(), p.getY(), p.getZ(), p.getYaw(), 0f);
        c.teleportNear(p);
        mc.world.addEntity(c);
    }

    private static void despawn() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (companion != null && mc.world != null && companion.getEntityWorld() == mc.world) {
            mc.world.removeEntity(companion.getId(), Entity.RemovalReason.DISCARDED);
        }
        companion = null;
    }

    // ------------------------------------------------------------------ Interaktion

    public static boolean isTargeted(MinecraftClient mc) {
        return companion != null && mc.crosshairTarget instanceof EntityHitResult hit && hit.getEntity() == companion;
    }

    public static void openDialogue(MinecraftClient mc) {
        if (companion == null) return;
        mc.setScreen(new DialogueScreen(companion));
    }

    public static void onHit(MinecraftClient mc) {
        if (companion == null || hitCooldown > 0) return;
        hitCooldown = 20;
        mc.player.swingHand(Hand.MAIN_HAND);
        companion.animateDamage(mc.player.getYaw());
        Config.addAffection(-1);
        if (Config.get().tsundere) {
            say(Lines.pick(Lines.HIT_TSUNDERE), Voice.ANGRY);
            lookAwayTicks = 60;
        } else {
            say(Lines.pick(Lines.HIT_NORMAL), Voice.SAD);
        }
    }

    // ------------------------------------------------------------------ Sprechen

    public static void say(String text, Voice voice) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.inGameHud == null) return;
        MutableText msg = Text.literal("<").append(companion != null ? companion.getDisplayName()
                : Text.literal(Config.get().displayName)).append("> ").append(Text.literal(text).formatted(Formatting.WHITE));
        mc.inGameHud.getChatHud().addMessage(msg);
        voice.play();
        if (companion != null) companion.swingHand(Hand.MAIN_HAND);
    }

    // ------------------------------------------------------------------ Spontane Reaktionen

    private static void tickReactions(MinecraftClient mc) {
        if (companion == null) return;
        ClientPlayerEntity p = mc.player;
        Config cfg = Config.get();
        boolean t = cfg.tsundere;

        if (reactCooldown > 0) reactCooldown--;
        if (hitCooldown > 0) hitCooldown--;
        if (lookAwayTicks > 0) lookAwayTicks--;

        double dist = companion.distanceTo(p);

        // Spieler wurde verletzt
        if (p.hurtTime > 0 && lastOwnerHurt == 0 && reactCooldown == 0 && dist < 16) {
            if (p.getHealth() < 6) say(Lines.pick(t ? Lines.LOW_HP_TSUNDERE : Lines.LOW_HP_NORMAL), t ? Voice.EMBARRASSED : Voice.SAD);
            else say(Lines.pick(t ? Lines.HURT_TSUNDERE : Lines.HURT_NORMAL), t ? Voice.HMPH : Voice.TALK);
            reactCooldown = 300;
        }
        lastOwnerHurt = p.hurtTime;

        // Tsundere: Spieler starrt sie an
        if (t && dist < 6 && isLookedAt(p)) {
            if (++stareTicks == 60 && reactCooldown == 0) {
                say(Lines.pick(Lines.STARE_TSUNDERE), Voice.EMBARRASSED);
                lookAwayTicks = 80;
                reactCooldown = 400;
            }
        } else {
            stareTicks = 0;
        }

        // Nacht bricht an
        long time = mc.world.getTimeOfDay() % 24000L;
        boolean night = time >= 13000 && time < 23000;
        if (night && !wasNight && dist < 24) {
            say(Lines.pick(t ? Lines.NIGHT_TSUNDERE : Lines.NIGHT_NORMAL), t ? Voice.HMPH : Voice.TALK);
        }
        wasNight = night;

        // Zufälliges Gerede
        if (cfg.ambientChat && dist < 16 && --idleTimer <= 0) {
            idleTimer = 1200 + ThreadLocalRandom.current().nextInt(2400); // 1-3 Minuten
            if (reactCooldown == 0) {
                if (t) say(Lines.pick(Lines.idleTsundere()), Lines.randomTsundereVoice());
                else say(Lines.pick(Lines.IDLE_NORMAL), Voice.TALK);
            }
        }
    }

    private static boolean isLookedAt(ClientPlayerEntity p) {
        Vec3d look = p.getRotationVec(1f).normalize();
        Vec3d toCompanion = companion.getEyePos().subtract(p.getEyePos()).normalize();
        return look.dotProduct(toCompanion) > 0.985;
    }
}
