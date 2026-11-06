package de.slyrex.begleiter;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

/**
 * Stimmen/Geräusche des Begleiters. Die Sounds sind in assets/begleiter/sounds.json
 * definiert (standardmäßig hochgepitchte Vanilla-Sounds) und können per
 * Ressourcenpaket durch eigene .ogg-Dateien ersetzt werden.
 */
public enum Voice {
    NONE(null),
    TALK("voice.talk"),
    HAPPY("voice.happy"),
    HMPH("voice.hmph"),
    ANGRY("voice.angry"),
    EMBARRASSED("voice.embarrassed"),
    SAD("voice.sad");

    private final String id;

    Voice(String id) {
        this.id = id;
    }

    public void play() {
        if (id == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        Entity source = CompanionManager.get();
        if (source == null) source = mc.player;
        if (source == null) return;

        // Im normalen Modus nur leises Plappern, die "echten" Geräusche gibt's im Tsundere-Modus
        String soundId = Config.get().tsundere ? id : "voice.talk";
        float volume = Config.get().tsundere ? 1.0f : 0.5f;

        SoundEvent event = SoundEvent.of(Identifier.of(BegleiterClient.MOD_ID, soundId));
        mc.getSoundManager().play(new PositionedSoundInstance(event, SoundCategory.NEUTRAL, volume, 1.0f,
                SoundInstance.createRandom(), source.getX(), source.getEyeY(), source.getZ()));
    }
}
