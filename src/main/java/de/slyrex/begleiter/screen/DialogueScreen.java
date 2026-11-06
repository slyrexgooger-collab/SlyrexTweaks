package de.slyrex.begleiter.screen;

import de.slyrex.begleiter.CompanionEntity;
import de.slyrex.begleiter.Config;
import de.slyrex.begleiter.Dialogues;
import de.slyrex.begleiter.Voice;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;

/** Dialogfenster mit Porträt, Text und Antwort-Buttons. */
public class DialogueScreen extends Screen {
    public record Option(String label, Runnable action) {}

    private static final int PANEL_W = 340;
    private static final int PANEL_H = 90;

    private final CompanionEntity companion;
    private String npcText = "";
    private List<Option> options = List.of();
    private boolean started = false;

    public DialogueScreen(CompanionEntity companion) {
        super(Text.literal("Dialog"));
        this.companion = companion;
    }

    /** Neuen Dialogschritt anzeigen. */
    public void show(String text, Voice voice, List<Option> opts) {
        this.npcText = text;
        this.options = opts;
        voice.play();
        if (this.client != null) this.clearAndInit();
    }

    @Override
    protected void init() {
        if (!started) {
            started = true;
            Dialogues.greet(this); // ruft show() -> clearAndInit() auf
            return;
        }
        int panelX = (this.width - PANEL_W) / 2;
        int panelY = Math.max(10, this.height / 2 - 110);
        int top = panelY + PANEL_H + 10;

        int colW = 165;
        int gap = 10;
        int rows = (options.size() + 1) / 2;
        for (int i = 0; i < options.size(); i++) {
            Option opt = options.get(i);
            int col = options.size() <= 3 ? 0 : i % 2;
            int row = options.size() <= 3 ? i : i / 2;
            int w = options.size() <= 3 ? PANEL_W : colW;
            int x = options.size() <= 3 ? panelX : panelX + col * (colW + gap);
            this.addDrawableChild(ButtonWidget.builder(Text.literal(opt.label()), b -> opt.action().run())
                    .dimensions(x, top + row * 24, w, 20).build());
        }
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.renderBackground(ctx, mouseX, mouseY, delta);
        int panelX = (this.width - PANEL_W) / 2;
        int panelY = Math.max(10, this.height / 2 - 110);
        ctx.fill(panelX, panelY, panelX + PANEL_W, panelY + PANEL_H, 0xCC101018);
        int border = Config.get().tsundere ? 0xFFFF77CC : 0xFF55CCFF;
        ctx.drawStrokedRectangle(panelX, panelY, PANEL_W, PANEL_H, border);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        int panelX = (this.width - PANEL_W) / 2;
        int panelY = Math.max(10, this.height / 2 - 110);

        // Porträt des Begleiters (folgt der Maus)
        if (companion != null && !companion.isRemoved()) {
            InventoryScreen.drawEntity(ctx, panelX + 4, panelY + 4, panelX + 70, panelY + PANEL_H - 4,
                    32, 0.0625f, mouseX, mouseY, companion);
        }

        int textX = panelX + 78;
        ctx.drawTextWithShadow(this.textRenderer, companion != null ? companion.getDisplayName()
                : Text.literal(Config.get().displayName), textX, panelY + 8, 0xFFFFFFFF);
        if (Config.get().tsundere) {
            String hearts = "♥ " + Config.get().affection;
            ctx.drawTextWithShadow(this.textRenderer, hearts,
                    panelX + PANEL_W - 8 - this.textRenderer.getWidth(hearts), panelY + 8, 0xFFFF77CC);
        }
        ctx.drawWrappedTextWithShadow(this.textRenderer, Text.literal(npcText), textX, panelY + 24,
                PANEL_W - 78 - 10, 0xFFE0E0E0);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
