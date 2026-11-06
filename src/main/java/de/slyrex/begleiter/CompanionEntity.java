package de.slyrex.begleiter;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Ein rein clientseitiger "Spieler", der nur in deiner Welt existiert.
 * Andere Spieler und der Server sehen ihn nicht.
 */
public class CompanionEntity extends OtherClientPlayerEntity {
    private static final double FOLLOW_DISTANCE = 2.5;
    private static final double SPRINT_DISTANCE = 6.0;
    private static final double TELEPORT_DISTANCE = 24.0;

    public CompanionEntity(ClientWorld world, GameProfile profile) {
        super(world, profile);
        this.noClip = false; // OtherClientPlayerEntity schaltet Kollision sonst ab
    }

    @Override
    public SkinTextures getSkin() {
        SkinTextures custom = SkinManager.getTextures();
        return custom != null ? custom : DefaultSkinHelper.getSkinTextures(this.getUuid());
    }

    @Override
    public MutableText getDisplayName() {
        de.slyrex.begleiter.Config c = de.slyrex.begleiter.Config.get();
        return Text.literal(c.displayName).formatted(c.tsundere ? Formatting.LIGHT_PURPLE : Formatting.AQUA);
    }

    /** OtherClientPlayerEntity macht hier nichts – wir wollen aber Schleichen anzeigen. */
    @Override
    protected void updatePose() {
        this.setPose(this.isSneaking() ? EntityPose.CROUCHING : EntityPose.STANDING);
    }

    @Override
    public boolean shouldRenderName() {
        return true;
    }

    /** Ersetzt die normale (Netzwerk-)Bewegung durch eigene Folge-KI. */
    @Override
    public void tickMovement() {
        this.tickHandSwing();

        PlayerEntity owner = MinecraftClient.getInstance().player;
        if (owner == null || owner.getEntityWorld() != this.getEntityWorld()) return;

        de.slyrex.begleiter.Config cfg = de.slyrex.begleiter.Config.get();
        double dx = owner.getX() - this.getX();
        double dz = owner.getZ() - this.getZ();
        double distH = Math.sqrt(dx * dx + dz * dz);

        if (cfg.following && this.distanceTo(owner) > TELEPORT_DISTANCE) {
            teleportNear(owner);
            return;
        }

        // --- Horizontale Bewegung ---
        double vx = 0, vz = 0;
        boolean moving = cfg.following && distH > FOLLOW_DISTANCE;
        if (moving) {
            double speed = distH > SPRINT_DISTANCE ? 0.28 : 0.17;
            vx = dx / distH * speed;
            vz = dz / distH * speed;
        }
        this.setSprinting(moving && distH > SPRINT_DISTANCE);
        this.setSneaking(owner.isSneaking() && !moving);

        // --- Vertikale Bewegung (Schwerkraft, Springen, Schwimmen) ---
        double vy = this.getVelocity().y;
        if (this.isTouchingWater() || this.isInLava()) {
            vy = (moving || owner.getY() > this.getY() - 0.5) ? 0.06 : -0.02;
            vx *= 0.6;
            vz *= 0.6;
        } else if (this.isOnGround()) {
            vy = (moving && this.horizontalCollision) ? 0.42 : -0.0784;
        } else {
            vy = (vy - 0.08) * 0.98;
        }

        this.setVelocity(vx, vy, vz);
        this.move(MovementType.SELF, this.getVelocity());

        // --- Blickrichtung ---
        float targetYaw;
        if (moving) {
            targetYaw = (float) (MathHelper.atan2(vz, vx) * (180.0 / Math.PI)) - 90.0f;
        } else {
            targetYaw = (float) (MathHelper.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
            if (CompanionManager.isLookingAway()) targetYaw += 150.0f; // Tsundere: "Hmpf!" *dreht sich weg*
        }
        double dy = owner.getEyeY() - this.getEyeY();
        float targetPitch = moving ? 0f : (float) -(MathHelper.atan2(dy, Math.max(distH, 0.1)) * (180.0 / Math.PI));
        if (CompanionManager.isLookingAway()) targetPitch = 10f;

        float yaw = approachAngle(this.getYaw(), targetYaw, 25f);
        this.setYaw(yaw);
        this.headYaw = approachAngle(this.headYaw, targetYaw, 30f);
        this.bodyYaw = approachAngle(this.bodyYaw, yaw, moving ? 25f : 10f);
        this.setPitch(approachAngle(this.getPitch(), MathHelper.clamp(targetPitch, -60f, 60f), 10f));

        // Für Umhang/Laufanimation
        this.tickPlayerMovement();
    }

    public void teleportNear(PlayerEntity owner) {
        Vec3d look = Vec3d.fromPolar(0, owner.getYaw()).normalize();
        Vec3d[] candidates = {
                owner.getEntityPos().subtract(look.multiply(2)),
                owner.getEntityPos().add(look.rotateY((float) Math.PI / 2).multiply(2)),
                owner.getEntityPos().add(look.rotateY((float) -Math.PI / 2).multiply(2)),
                owner.getEntityPos()
        };
        Vec3d target = owner.getEntityPos();
        for (Vec3d c : candidates) {
            Box box = this.getDimensions(this.getPose()).getBoxAt(c);
            if (this.getEntityWorld().isSpaceEmpty(this, box)) {
                target = c;
                break;
            }
        }
        this.refreshPositionAndAngles(target.x, target.y, target.z, owner.getYaw(), 0f);
        this.headYaw = owner.getYaw();
        this.bodyYaw = owner.getYaw();
        this.setVelocity(Vec3d.ZERO);
    }

    private static float approachAngle(float from, float to, float maxStep) {
        float diff = MathHelper.wrapDegrees(to - from);
        return from + MathHelper.clamp(diff, -maxStep, maxStep);
    }
}
