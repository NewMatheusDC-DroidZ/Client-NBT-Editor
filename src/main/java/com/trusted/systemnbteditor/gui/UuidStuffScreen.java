package com.trusted.systemnbteditor.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import com.trusted.systemnbteditor.data.ModConfig;

public class UuidStuffScreen extends Screen {
    private final Screen parent;

    public UuidStuffScreen(Screen parent) {
        super(Text.of("UUID Stuff"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        int buttonWidth = 120;
        int buttonHeight = 20;
        int padding = 10;

        int gridWidth = 2 * buttonWidth + padding;
        int gridHeight = 2 * buttonHeight + padding;

        int startX = this.width / 2 - gridWidth / 2;
        int startY = this.height / 2 - gridHeight / 2;

        // Button labels in blue
        Text pearlText = Text.literal("UUID Pearl").formatted(Formatting.BLUE);
        Text wolfText = Text.literal("UUID Wolf Utility").formatted(Formatting.BLUE);
        Text tridentText = Text.literal("UUID Trident Utility").formatted(Formatting.BLUE);
        Text molesText = Text.literal("UUID PosPearls").formatted(Formatting.BLUE);

        // 2x2 Grid
        // First row
        this.addDrawableChild(ButtonWidget.builder(pearlText, button -> {
            if (this.client != null) {
                this.client.setScreen(new UuidPearlsScreen(this));
            }
        })
                .dimensions(startX, startY, buttonWidth, buttonHeight).build());
        
        this.addDrawableChild(ButtonWidget.builder(wolfText, button -> {
            if (this.client != null) {
                if (ModConfig.getInstance().adminPermissions) {
                    TriviaScreen.skidUnlocked = true;
                    this.client.setScreen(new UuidWolfUtilityScreen(this));
                } else {
                    this.client.setScreen(new TriviaScreen(this, 
                        () -> this.client.setScreen(new UuidWolfUtilityScreen(this)),
                        null, false));
                }
            }
        })
                .dimensions(startX + buttonWidth + padding, startY, buttonWidth, buttonHeight).build());

        // Second row
        this.addDrawableChild(ButtonWidget.builder(tridentText, button -> {
            if (this.client != null) {
                if (ModConfig.getInstance().adminPermissions) {
                    TriviaScreen.skidUnlocked = true;
                    this.client.setScreen(new UuidUtilityTridentScreen(this));
                } else {
                    this.client.setScreen(new TriviaScreen(this, 
                        () -> this.client.setScreen(new UuidUtilityTridentScreen(this)),
                        null, false));
                }
            }
        })
                .dimensions(startX, startY + buttonHeight + padding, buttonWidth, buttonHeight).build());

        this.addDrawableChild(ButtonWidget.builder(molesText, button -> {
            if (this.client != null) {
                this.client.setScreen(new UuidPosPearlsScreen(this));
            }
        })
                .dimensions(startX + buttonWidth + padding, startY + buttonHeight + padding, buttonWidth, buttonHeight).build());

        // Back button
        this.addDrawableChild(ButtonWidget.builder(Text.of("Back"), button -> {
            if (this.client != null) {
                this.client.setScreen(this.parent);
            }
        }).dimensions(this.width / 2 - 50, this.height - 30, 100, 20).build());
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0x80000000);
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }
}
