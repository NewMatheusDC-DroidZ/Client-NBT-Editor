package com.trusted.systemnbteditor.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class ItemPreviewScreen extends Screen {
    private final ItemStack itemStack;
    private final Screen parent;

    public ItemPreviewScreen(ItemStack itemStack, Screen parent) {
        super(Text.of("Preview"));
        this.itemStack = itemStack;
        this.parent = parent;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {

        // Draw single slot at center
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Draw a slot texture and selection outline using reflection
        Identifier slotTexture = Identifier.of("minecraft", "container/slot");
        Identifier selectionTexture = Identifier.of("minecraft", "hud/hotbar_selection");
        try {
            boolean drawn = false;
            // Try common drawGuiTexture intermediate names or the direct name
            for (java.lang.reflect.Method m : context.getClass().getMethods()) {
                if (m.getName().equals("drawGuiTexture") || m.getName().equals("method_52718")) {
                    java.lang.reflect.Parameter[] params = m.getParameters();
                    if (params.length == 5 && params[0].getType() == Identifier.class) {
                        m.invoke(context, slotTexture, centerX - 9, centerY - 9, 18, 18);
                        m.invoke(context, selectionTexture, centerX - 12, centerY - 12, 24, 24);
                        drawn = true;
                        break;
                    }
                }
            }
            if (!drawn) {
                // Fallback to a better looking slot if reflection fails
                context.fill(centerX - 9, centerY - 9, centerX + 9, centerY + 9, 0xFF373737); // Dark grey
                context.fill(centerX - 9, centerY - 9, centerX + 9, centerY - 8, 0xFF000000); // Top shadow
                context.fill(centerX - 9, centerY - 9, centerX - 8, centerY + 9, 0xFF000000); // Left shadow
                context.fill(centerX - 9, centerY + 8, centerX + 9, centerY + 9, 0xFFFFFFFF); // Bottom highlight
                context.fill(centerX + 8, centerY - 9, centerX + 9, centerY + 9, 0xFFFFFFFF); // Right highlight
            }
        } catch (Exception e) {
            context.fill(centerX - 9, centerY - 9, centerX + 9, centerY + 9, 0xFF444444);
        }

        context.drawItem(this.itemStack, centerX - 8, centerY - 8);

        // Tooltip only on hover
        if (mouseX >= centerX - 9 && mouseX <= centerX + 9 && mouseY >= centerY - 9 && mouseY <= centerY + 9) {
            context.drawItemTooltip(this.textRenderer, this.itemStack, mouseX, mouseY);
        }

        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(this.textRenderer, Text.of("Press ESC to return"), centerX, centerY + 20,
                0xFFFFFF);
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }
}
