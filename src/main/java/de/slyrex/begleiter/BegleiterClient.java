package de.slyrex.begleiter;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BegleiterClient {
    public static final String MOD_ID = de.slyrex.Slyrex.MOD_ID;
    public static final Logger LOGGER = LoggerFactory.getLogger("Slyrex/Begleiter");

    public static KeyBinding menuKey;
    public static KeyBinding talkKey;

    private BegleiterClient() {}

    public static void init(KeyBinding.Category category) {
        Config.load();

        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.slyrex.begleiter_menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, category));
        talkKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.slyrex.begleiter_talk", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, category));

        ClientTickEvents.END_CLIENT_TICK.register(CompanionManager::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> CompanionManager.forget());

        String skin = Config.get().skinName;
        if (skin != null && !skin.isBlank()) {
            SkinManager.load(skin);
        }
    }
}
