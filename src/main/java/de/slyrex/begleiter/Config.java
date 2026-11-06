package de.slyrex.begleiter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Einstellungen, gespeichert in config/begleiter.json */
public final class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Config instance = new Config();

    public boolean enabled = true;
    public boolean following = true;
    public boolean tsundere = false;
    public boolean ambientChat = true;
    public String displayName = "Yuki";
    public String skinName = "";
    /** Zuneigung, ca. -10 bis 30 */
    public int affection = 0;

    public static Config get() {
        return instance;
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("slyrex").resolve("begleiter.json");
    }

    public static void load() {
        Path p = path();
        if (Files.exists(p)) {
            try (Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
                Config c = GSON.fromJson(r, Config.class);
                if (c != null) instance = c;
            } catch (Exception e) {
                BegleiterClient.LOGGER.warn("Konnte begleiter.json nicht lesen", e);
            }
        }
        if (instance.displayName == null || instance.displayName.isBlank()) instance.displayName = "Yuki";
        if (instance.skinName == null) instance.skinName = "";
        save();
    }

    public static void save() {
        try {
            Files.createDirectories(path().getParent());
        } catch (IOException ignored) {
        }
        try (Writer w = Files.newBufferedWriter(path(), StandardCharsets.UTF_8)) {
            GSON.toJson(instance, w);
        } catch (IOException e) {
            BegleiterClient.LOGGER.warn("Konnte begleiter.json nicht speichern", e);
        }
    }

    public static void addAffection(int amount) {
        instance.affection = Math.max(-10, Math.min(30, instance.affection + amount));
        save();
    }
}
