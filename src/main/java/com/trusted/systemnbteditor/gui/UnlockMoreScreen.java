package com.trusted.systemnbteditor.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import com.trusted.systemnbteditor.data.ModConfig;
import java.time.LocalTime;

public class UnlockMoreScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget pinField;
    private ButtonWidget accessButton;
    private String feedbackMessage = "";
    private int feedbackColor = 0xFFFFFF;
    public static boolean unlockedOnce = false;

    public UnlockMoreScreen(Screen parent) {
        super(Text.of("Unlock More"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        if ((unlockedOnce || ModConfig.getInstance().adminPermissions) && this.client != null) {
            this.client.setScreen(new SecretFeaturesScreen(this.parent));
            return;
        }

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // 20x140 text box
        this.pinField = new TextFieldWidget(this.textRenderer, centerX - 70, centerY - 30, 140, 20, Text.of("PIN"));
        this.pinField.setPlaceholder(Text.of("Enter the Code"));
        this.pinField.setMaxLength(32);
        this.addDrawableChild(this.pinField);

        // 20x100 Access button
        this.accessButton = ButtonWidget.builder(Text.of("Access"), button -> checkPin())
                .dimensions(centerX - 50, centerY + 10, 100, 20)
                .build();
        this.addDrawableChild(this.accessButton);

        // Back button
        this.addDrawableChild(ButtonWidget.builder(Text.of("Back"), button -> {
            if (this.client != null) this.client.setScreen(this.parent);
        }).dimensions(centerX - 50, this.height - 30, 100, 20).build());
    }

    private void checkPin() {
        String entered = pinField.getText().trim();
        String correctPin = getRequiredPin();

        if (entered.equals(correctPin)) {
            unlockedOnce = true;
            if (this.client != null) {
                this.client.setScreen(new SecretFeaturesScreen(this.parent));
            }
        } else {
            feedbackMessage = "Invalid Code";
            feedbackColor = 0xFFFF0000;
        }
    }

    private String getRequiredPin() {
        int hour = LocalTime.now().getHour();
        if (hour < 8) {
            return "9223";
        } else if (hour < 20) {
            return "2147483647";
        } else {
            return "32767";
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);

        if (!feedbackMessage.isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer, feedbackMessage, this.width / 2, this.height / 2 - 50, feedbackColor);
        }
    }

    @Override
    public void close() {
        if (this.client != null) this.client.setScreen(this.parent);
    }
}
