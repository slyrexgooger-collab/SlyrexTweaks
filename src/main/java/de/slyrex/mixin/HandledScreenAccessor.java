package de.slyrex.mixin;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(HandledScreen.class)
public interface HandledScreenAccessor {
	@Accessor("x")
	int slyrex$getX();

	@Accessor("y")
	int slyrex$getY();

	@Accessor("backgroundWidth")
	int slyrex$getBackgroundWidth();

	@Accessor("backgroundHeight")
	int slyrex$getBackgroundHeight();
}
