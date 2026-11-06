package de.slyrex.begleiter;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.entity.player.PlayerSkinType;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.AssetInfo;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

/** Lädt den Skin eines beliebigen Minecraft-Accounts über die Mojang-API. */
public final class SkinManager {
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static volatile SkinTextures current;
    private static volatile String status = "";
    private static Identifier currentId;
    private static int counter = 0;

    private record SkinData(NativeImage image, boolean slim) {}

    public static SkinTextures getTextures() {
        return current;
    }

    public static String getStatus() {
        return status;
    }

    public static void reset() {
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.execute(() -> {
            if (currentId != null) mc.getTextureManager().destroyTexture(currentId);
            currentId = null;
            current = null;
            status = "Standard-Skin aktiv";
        });
    }

    public static CompletableFuture<Void> load(String name) {
        String clean = name.trim();
        if (!clean.matches("[A-Za-z0-9_]{1,16}")) {
            status = "§cUngültiger Name: " + clean;
            return CompletableFuture.completedFuture(null);
        }
        status = "§eLade Skin von " + clean + " ...";
        MinecraftClient mc = MinecraftClient.getInstance();
        return CompletableFuture.supplyAsync(() -> fetch(clean))
                .thenAcceptAsync(data -> {
                    if (currentId != null) mc.getTextureManager().destroyTexture(currentId);
                    Identifier id = Identifier.of(BegleiterClient.MOD_ID, "skins/" + clean.toLowerCase() + "_" + (counter++));
                    mc.getTextureManager().registerTexture(id, new NativeImageBackedTexture(() -> "begleiter skin", data.image()));
                    currentId = id;
                    current = SkinTextures.create(new AssetInfo.TextureAssetInfo(id, id), null, null,
                            data.slim() ? PlayerSkinType.SLIM : PlayerSkinType.WIDE);
                    status = "§aSkin von " + clean + " geladen!";
                }, mc)
                .exceptionally(t -> {
                    Throwable cause = t.getCause() != null ? t.getCause() : t;
                    status = "§cFehler: " + cause.getMessage();
                    BegleiterClient.LOGGER.warn("Skin konnte nicht geladen werden", cause);
                    return null;
                });
    }

    private static SkinData fetch(String name) {
        try {
            // 1) Name -> UUID
            String uuid = lookupUuid(name);
            // 2) UUID -> Profil mit Texturen
            JsonObject profile = getJson("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid);
            JsonArray props = profile.getAsJsonArray("properties");
            String texturesB64 = null;
            for (JsonElement e : props) {
                JsonObject o = e.getAsJsonObject();
                if ("textures".equals(o.get("name").getAsString())) texturesB64 = o.get("value").getAsString();
            }
            if (texturesB64 == null) throw new RuntimeException("Keine Texturen im Profil");
            JsonObject textures = JsonParser.parseString(
                    new String(Base64.getDecoder().decode(texturesB64), StandardCharsets.UTF_8))
                    .getAsJsonObject().getAsJsonObject("textures");
            if (textures == null || !textures.has("SKIN")) throw new RuntimeException("Spieler hat keinen eigenen Skin");
            JsonObject skin = textures.getAsJsonObject("SKIN");
            String url = skin.get("url").getAsString().replace("http://", "https://");
            boolean slim = skin.has("metadata")
                    && "slim".equals(skin.getAsJsonObject("metadata").get("model").getAsString());

            // 3) PNG herunterladen
            HttpResponse<InputStream> res = HTTP.send(HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(15)).GET().build(), HttpResponse.BodyHandlers.ofInputStream());
            if (res.statusCode() != 200) throw new RuntimeException("Download fehlgeschlagen (" + res.statusCode() + ")");
            NativeImage image;
            try (InputStream in = res.body()) {
                image = NativeImage.read(in);
            }
            return new SkinData(convertLegacy(image), slim);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private static String lookupUuid(String name) throws Exception {
        String[] endpoints = {
                "https://api.mojang.com/users/profiles/minecraft/" + name,
                "https://api.minecraftservices.com/minecraft/profile/lookup/name/" + name
        };
        Exception last = null;
        for (String url : endpoints) {
            try {
                return getJson(url).get("id").getAsString();
            } catch (Exception e) {
                last = e;
            }
        }
        throw new RuntimeException("Spieler '" + name + "' nicht gefunden", last);
    }

    private static JsonObject getJson(String url) throws Exception {
        HttpResponse<String> res = HTTP.send(HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(10)).header("Accept", "application/json").GET().build(),
                HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) throw new RuntimeException("HTTP " + res.statusCode());
        return JsonParser.parseString(res.body()).getAsJsonObject();
    }

    /** Alte 64x32-Skins in das 64x64-Format umwandeln (wie Vanilla). */
    private static NativeImage convertLegacy(NativeImage img) {
        if (img.getHeight() != 32) return img;
        NativeImage n = new NativeImage(64, 64, true);
        n.copyFrom(img);
        img.close();
        n.fillRect(0, 32, 64, 32, 0);
        n.copyRect(4, 16, 16, 32, 4, 4, true, false);
        n.copyRect(8, 16, 16, 32, 4, 4, true, false);
        n.copyRect(0, 20, 24, 32, 4, 12, true, false);
        n.copyRect(4, 20, 16, 32, 4, 12, true, false);
        n.copyRect(8, 20, 8, 32, 4, 12, true, false);
        n.copyRect(12, 20, 16, 32, 4, 12, true, false);
        n.copyRect(44, 16, -8, 32, 4, 4, true, false);
        n.copyRect(48, 16, -8, 32, 4, 4, true, false);
        n.copyRect(40, 20, 0, 32, 4, 12, true, false);
        n.copyRect(44, 20, -8, 32, 4, 12, true, false);
        n.copyRect(48, 20, -16, 32, 4, 12, true, false);
        n.copyRect(52, 20, -8, 32, 4, 12, true, false);
        return n;
    }
}
