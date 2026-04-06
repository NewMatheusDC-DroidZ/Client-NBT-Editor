package com.trusted.systemnbteditor.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.client.input.KeyInput;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;
import com.trusted.systemnbteditor.data.ModConfig;
import com.trusted.systemnbteditor.modules.BookBot;

public class DangerZoneScreen extends Screen {
    private final Screen parent;
    private ModConfig config;

    public DangerZoneScreen(Screen parent) {
        super(Text.of("Danger Zone"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        config = ModConfig.getInstance();
        if (config == null) return;

        int centerX = this.width / 2;
        int startY = 40;
        int spacing = 24;

        // --- Actions ---
        addDrawableChild(ButtonWidget.builder(Text.literal("Run BookBot"), btn -> {
            if (this.client != null) BookBot.execute(this.client);
        }).dimensions(centerX - 120, startY + 10, 110, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), button -> this.close())
            .dimensions(centerX + 10, startY + 10, 110, 20).build());
    }



    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, "Danger Zone", this.width / 2, 15, 0xFFFFFF);
    }
    
    @Override
    public void close() {
        ModConfig.save();
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }
}
