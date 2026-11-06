package de.slyrex.mixin;

import de.slyrex.module.StreamerMode;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Streamer-Modus: F3 verhaelt sich wie mit "reduzierten Debug-Infos" (keine Koordinaten). */
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientReducedDebugMixin {
	@Inject(method = "hasReducedDebugInfo", at = @At("HEAD"), cancellable = true)
	private void slyrex$reduced(CallbackInfoReturnable<Boolean> cir) {
		if (StreamerMode.hideDebug()) cir.setReturnValue(true);
	}
}
