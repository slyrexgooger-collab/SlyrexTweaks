package de.slyrex.begleiter.screen;

import de.slyrex.begleiter.CompanionManager;
import de.slyrex.begleiter.Config;
import de.slyrex.begleiter.SkinManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

/** Einstellungen (Taste K): Name, Skin, Tsundere-Modus usw. */
public class SettingsScreen extends Screen {
    private final @Nullable Screen parent;
    private TextFieldWidget nameField;
    private TextFieldWidget skinField;

    public SettingsScreen(@Nullable Screen parent) {
        super(Text.literal("Begleiter-Einstellungen"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        Config cfg = Config.get();
        int cx = this.width / 2;
        int y = 40;

        nameField = new TextFieldWidget(this.textRenderer, cx - 100, y + 12, 200, 20, Text.literal("Name"));
        nameField.setMaxLength(32);
        nameField.setText(cfg.displayName);
        this.addDrawableChild(nameField);
        y += 44;

        skinField = new TextFieldWidget(this.textRenderer, cx - 100, y + 12, 130, 20, Text.literal("Skin"));
        skinField.setMaxLength(16);
        skinField.setText(cfg.skinName);
        this.addDrawableChild(skinField);
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Laden"), b -> {
            String s = skinField.getText().trim();
            cfg.skinName = s;
            Config.save();
            if (s.isEmpty()) SkinManager.reset();
            else SkinManager.load(s);
        }).dimensions(cx + 34, y + 12, 66, 20).build());
        y += 56;

        this.addDrawableChild(CyclingButtonWidget.onOffBuilder(cfg.tsundere)
                .build(cx - 100, y, 200, 20, Text.literal("Tsundere-Modus"), (b, v) -> {
                    cfg.tsundere = v;
                    Config.save();
                }));
        y += 24;
        this.addDrawableChild(CyclingButtonWidget.onOffBuilder(cfg.enabled)
                .build(cx - 100, y, 200, 20, Text.literal("Begleiter aktiv"), (b, v) -> {
                    cfg.enabled = v;
                    Config.save();
                }));
        y += 24;
        this.addDrawableChild(CyclingButtonWidget.onOffBuilder(cfg.following)
                .build(cx - 100, y, 200, 20, Text.literal("Folgt mir"), (b, v) -> {
                    cfg.following = v;
                    Config.save();
                }));
        y += 24;
        this.addDrawableChild(CyclingButtonWidget.onOffBuilder(cfg.ambientChat)
                .build(cx - 100, y, 200, 20, Text.literal("Spontanes Gerede"), (b, v) -> {
                    cfg.ambientChat = v;
                    Config.save();
                }));
        y += 24;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Begleiter zu mir holen"), b -> {
            if (CompanionManager.get() != null && this.client.player != null) {
                CompanionManager.get().teleportNear(this.client.player);
            }
        }).dimensions(cx - 100, y, 200, 20).build());
        y += 24;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Zuneigung zurücksetzen"), b -> {
            cfg.affection = 0;
            Config.save();
        }).dimensions(cx - 100, y, 200, 20).build());
        y += 30;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Fertig"), b -> this.close())
                .dimensions(cx - 100, y, 200, 20).build());
    }

    @Override
    public void close() {
        String name = nameField.getText().trim();
        if (!name.isEmpty()) Config.get().displayName = name;
        Config.save();
        this.client.setScreen(parent);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        int cx = this.width / 2;
        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, cx, 15, 0xFFFFFFFF);
        ctx.drawTextWithShadow(this.textRenderer, "Name des Begleiters", cx - 100, 40, 0xFFA0A0A0);
        ctx.drawTextWithShadow(this.textRenderer, "Minecraft-Name für den Skin (leer = Standard)", cx - 100, 84, 0xFFA0A0A0);
        ctx.drawTextWithShadow(this.textRenderer, SkinManager.getStatus(), cx - 100, 120, 0xFFFFFFFF);
        ctx.drawTextWithShadow(this.textRenderer, "Zuneigung: " + Config.get().affection, cx + 104, 146, 0xFFFF77CC);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
