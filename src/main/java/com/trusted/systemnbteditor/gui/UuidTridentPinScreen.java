package com.trusted.systemnbteditor.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class UuidTridentPinScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget pinField;
    private ButtonWidget confirmButton;
    private static final String PIN_OBF = "\u0012\u0011\u0014\u0017";
    private static String getPin() {
        StringBuilder sb = new StringBuilder();
        for (char c : PIN_OBF.toCharArray()) sb.append((char) (c ^ 0x20));
        return sb.toString();
    }

    public UuidTridentPinScreen(Screen parent) {
        super(Text.of("Trident Crash PIN Gateway"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        String msg = "Message 1e310 on Discord to get the PIN to get access to the Trident Crash";
        int boxWidth = this.textRenderer.getWidth(msg) + 10;
        int boxHeight = 20;
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.pinField = new TextFieldWidget(this.textRenderer, centerX - boxWidth / 2, centerY - boxHeight / 2, boxWidth, boxHeight, Text.of("PIN"));
        this.pinField.setMaxLength(boxWidth);
        this.pinField.setPlaceholder(Text.literal(msg).formatted(Formatting.GRAY));
        this.addDrawableChild(this.pinField);

        this.confirmButton = ButtonWidget.builder(Text.of("Confirm"), button -> {
            if (this.pinField.getText().equals(getPin())) {
                if (this.client != null) {
                    this.client.setScreen(new UuidUtilityTridentScreen(this.parent));
                }
            } else {
                if (this.client != null && this.client.player != null) {
                    this.client.player.sendMessage(Text.literal("Incorrect PIN!").formatted(Formatting.RED), true);
                }
            }
        }).dimensions(centerX - 50, centerY + 20, 100, 20).build();
        this.addDrawableChild(this.confirmButton);

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
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
    }

/*
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) { // Enter keys
            this.confirmButton.onPress();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
*/
}
