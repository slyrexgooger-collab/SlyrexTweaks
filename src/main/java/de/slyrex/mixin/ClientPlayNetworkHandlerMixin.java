package de.slyrex.mixin;

import de.slyrex.module.TotemCounter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Totem-Pops (Status 35) und Tode (Status 3) anderer Spieler mitzaehlen. */
@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
	@Inject(method = "onEntityStatus", at = @At("TAIL"))
	private void slyrex$onEntityStatus(EntityStatusS2CPacket packet, CallbackInfo ci) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.world == null || !mc.isOnThread()) return;
		Entity e = packet.getEntity(mc.world);
		if (e != null) TotemCounter.onEntityStatus(e, packet.getStatus());
	}
}
