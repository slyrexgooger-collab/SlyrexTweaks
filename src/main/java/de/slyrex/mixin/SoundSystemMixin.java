package de.slyrex.mixin;

import de.slyrex.module.SoundMuffler;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Sound-Muffler: Lautstaerke einzelner Sounds anpassen und gehoerte Sounds merken. */
@Mixin(SoundSystem.class)
public abstract class SoundSystemMixin {
	@Inject(method = "getAdjustedVolume(Lnet/minecraft/client/sound/SoundInstance;)F", at = @At("RETURN"), cancellable = true)
	private void slyrex$volume(SoundInstance sound, CallbackInfoReturnable<Float> cir) {
		float f = SoundMuffler.factor(sound);
		if (f != 1f) cir.setReturnValue(cir.getReturnValue() * f);
	}

	@Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)Lnet/minecraft/client/sound/SoundSystem$PlayResult;", at = @At("HEAD"))
	private void slyrex$heard(SoundInstance sound, CallbackInfoReturnable<?> cir) {
		SoundMuffler.heard(sound);
	}
}
