package de.slyrex.begleiter.mixin;

import de.slyrex.begleiter.CompanionManager;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Fängt Rechts-/Linksklick auf den Begleiter ab, damit nichts zum Server geschickt wird. */
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    private void begleiter$onUse(CallbackInfo ci) {
        MinecraftClient self = (MinecraftClient) (Object) this;
        if (CompanionManager.isTargeted(self)) {
            CompanionManager.openDialogue(self);
            ci.cancel();
        }
    }

    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void begleiter$onAttack(CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient self = (MinecraftClient) (Object) this;
        if (CompanionManager.isTargeted(self)) {
            CompanionManager.onHit(self);
            cir.setReturnValue(false);
        }
    }
}
