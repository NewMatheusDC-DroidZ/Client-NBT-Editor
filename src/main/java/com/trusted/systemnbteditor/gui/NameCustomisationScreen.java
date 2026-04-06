package com.trusted.systemnbteditor.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

public class NameCustomisationScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget prefixField;
    private String selectedColor = null;

    private static boolean initialized = false;

    private static final String[][] COLORS = {
        {"D Red", "dark_red", "0xAA0000"},
        {"L Red", "red", "0xFF5555"},
        {"Gold", "gold", "0xFFAA00"},
        {"Yellow", "yellow", "0xFFFF55"},
        {"L Green", "green", "0x55FF55"},
        {"D Green", "dark_green", "0x00AA00"},
        {"L Aqua", "aqua", "0x55FFFF"},
        {"D Aqua", "dark_aqua", "0x00AAAA"},
        {"L Blue", "blue", "0x5555FF"},
        {"D Blue", "dark_blue", "0x0000AA"},
        {"L Purple", "light_purple", "0xFF55FF"},
        {"D Purple", "dark_purple", "0xAA00AA"},
        {"Black", "black", "0x000000"},
        {"L Gray", "gray", "0xAAAAAA"},
        {"D Gray", "dark_gray", "0x555555"},
        {"White", "white", "0xFFFFFF"}
    };

    public NameCustomisationScreen(Screen parent) {
        super(Text.of("Name Customisation"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        // Run Init Commands only once per launch
        if (!initialized && this.client != null && this.client.getNetworkHandler() != null) {
            this.client.getNetworkHandler().sendChatCommand("team add Rank");
            this.client.getNetworkHandler().sendChatCommand("team join Rank @p");
            this.client.getNetworkHandler().sendChatCommand("tag @p add Ranked");
            initialized = true;
        }

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Color Buttons Grid (4x4)
        int gridX = centerX - 105;
        int gridY = centerY - 110;
        int btnWidth = 50;
        int btnHeight = 20;
        int pad = 4;

        for (int i = 0; i < COLORS.length; i++) {
            final String colorName = COLORS[i][0];
            final String colorId = COLORS[i][1];
            final int colorVal = Integer.decode(COLORS[i][2]);
            
            int row = i / 4;
            int col = i % 4;
            
            this.addDrawableChild(ButtonWidget.builder(Text.literal(colorName).withColor(colorVal), btn -> {
                this.selectedColor = colorId;
            }).dimensions(gridX + col * (btnWidth + pad), gridY + row * (btnHeight + pad), btnWidth, btnHeight).build());
        }

        // Prefix Text Box
        this.prefixField = new TextFieldWidget(this.textRenderer, centerX - 100, centerY - 10, 200, 20, Text.of("Prefix"));
        this.prefixField.setPlaceholder(Text.literal("Name Prefix").formatted(Formatting.GRAY));
        this.addDrawableChild(this.prefixField);

        // Apply Button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Apply").formatted(Formatting.GREEN), btn -> apply())
            .dimensions(centerX - 100, centerY + 15, 200, 20).build());

        // Reset All Button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Reset All").formatted(Formatting.RED), btn -> resetAll())
            .dimensions(centerX - 100, centerY + 40, 100, 20).build());

        // Remove Prefix Button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Remove Prefix").formatted(Formatting.YELLOW), btn -> removePrefix())
            .dimensions(centerX, centerY + 40, 100, 20).build());

        // Back Button
        this.addDrawableChild(ButtonWidget.builder(Text.of("Back"), btn -> this.close())
            .dimensions(centerX - 100, centerY + 65, 200, 20).build());
    }

    private void apply() {
        if (this.client == null || this.client.getNetworkHandler() == null) return;

        if (selectedColor != null) {
            this.client.getNetworkHandler().sendChatCommand("team modify Rank color " + selectedColor);
        }

        String prefix = this.prefixField.getText().trim();
        if (!prefix.isEmpty()) {
            this.client.getNetworkHandler().sendChatCommand("team modify Rank prefix {\"text\": \"" + prefix + " \"}");
        }

        if (this.client.player != null) {
            this.client.player.sendMessage(Text.literal("Customisation Applied!").formatted(Formatting.GREEN), true);
        }
    }

    private void resetAll() {
        if (this.client == null || this.client.getNetworkHandler() == null) return;

        // Reset Color to White
        this.selectedColor = "white";
        this.client.getNetworkHandler().sendChatCommand("team modify Rank color white");

        // Remove Prefix
        this.prefixField.setText("");
        this.client.getNetworkHandler().sendChatCommand("team modify Rank prefix \"\"");

        if (this.client.player != null) {
            this.client.player.sendMessage(Text.literal("Reset All: Color White & Prefix Removed").formatted(Formatting.RED), true);
        }
    }

    private void removePrefix() {
        if (this.client == null || this.client.getNetworkHandler() == null) return;

        this.prefixField.setText("");
        this.client.getNetworkHandler().sendChatCommand("team modify Rank prefix \"\"");

        if (this.client.player != null) {
            this.client.player.sendMessage(Text.literal("Prefix Removed").formatted(Formatting.YELLOW), true);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, "Name Customisation", this.width / 2, 15, 0xFFFFFF);
        
        if (selectedColor != null) {
            context.drawTextWithShadow(this.textRenderer, "Selected: " + selectedColor, 10, this.height - 20, 0xAAAAAA);
        }
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }
}
