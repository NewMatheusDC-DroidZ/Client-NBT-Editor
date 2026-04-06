package com.trusted.systemnbteditor.gui;

import com.trusted.systemnbteditor.util.AntiCrashLogManager;
import com.trusted.systemnbteditor.util.MixinState;
import com.trusted.systemnbteditor.util.NbtFormatter;
import com.trusted.systemnbteditor.util.NbtUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import com.trusted.systemnbteditor.data.ModConfig;

import java.util.ArrayList;
import java.util.List;

public class AntiCrashScreen extends Screen {
    private final Screen parent;
    private final List<Text> formattedLogs = new ArrayList<>();
    private double scrollOffset = 0;
    private ButtonWidget modeButton;
    private ButtonWidget tridentProtectionButton;

    public AntiCrashScreen(Screen parent) {
        super(Text.of("AntiCrash"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        
        // Mode Button at the top
        updateButtonText();
        this.modeButton = ButtonWidget.builder(getModeButtonText(), button -> {
            ModConfig config = ModConfig.getInstance();
            switch (config.translationProtectionMode) {
                case OFF:
                    config.translationProtectionMode = ModConfig.TranslationProtectionMode.SIMPLE;
                    break;
                case SIMPLE:
                    config.translationProtectionMode = ModConfig.TranslationProtectionMode.SMART;
                    break;
                case SMART:
                    config.translationProtectionMode = ModConfig.TranslationProtectionMode.OFF;
                    break;
            }
            ModConfig.save();
            updateButtonText();
        }).dimensions(centerX - 100, 40, 200, 20).build();
        this.addDrawableChild(this.modeButton);

        // Trident Protection Toggle
        this.tridentProtectionButton = ButtonWidget.builder(getTridentProtectionButtonText(), button -> {
            ModConfig config = ModConfig.getInstance();
            config.tridentProtectionEnabled = !config.tridentProtectionEnabled;
            ModConfig.save();
            updateButtonText();
        }).dimensions(centerX - 100, 65, 200, 20).build();
        this.addDrawableChild(this.tridentProtectionButton);
        
        loadLogs();
        
        // Clear button beside toggle
        this.addDrawableChild(ButtonWidget.builder(Text.of("Clear Logs"), button -> {
            AntiCrashLogManager.clear();
            loadLogs();
        }).dimensions(centerX + 110, 40, 80, 20).build());

        // Back Button
        this.addDrawableChild(ButtonWidget.builder(Text.of("Back"), button -> {
            if (this.client != null) {
                this.client.setScreen(this.parent);
            }
        }).dimensions(centerX - 50, this.height - 30, 100, 20).build());

        // Unlock More Button in top right (20x100 is dimensions(x, y, 100, 20))
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Unlock More").formatted(Formatting.DARK_BLUE), button -> {
            if (this.client != null) {
                this.client.setScreen(new UnlockMoreScreen(this));
            }
        }).dimensions(this.width - 110, 10, 100, 20).build());
    }

    private void updateButtonText() {
        if (this.modeButton != null) {
            this.modeButton.setMessage(getModeButtonText());
        }
        if (this.tridentProtectionButton != null) {
            this.tridentProtectionButton.setMessage(getTridentProtectionButtonText());
        }
    }

    private Text getModeButtonText() {
        ModConfig.TranslationProtectionMode mode = ModConfig.getInstance().translationProtectionMode;
        String modeName = mode.name();
        Formatting color;
        switch (mode) {
            case OFF:
                color = Formatting.RED;
                break;
            case SIMPLE:
                color = Formatting.YELLOW;
                break;
            case SMART:
                color = Formatting.GREEN;
                break;
            default:
                color = Formatting.WHITE;
        }
        return Text.literal("Protection Mode: " + modeName).formatted(color);
    }

    private Text getTridentProtectionButtonText() {
        boolean enabled = ModConfig.getInstance().tridentProtectionEnabled;
        return Text.literal("Trident Protection: " + (enabled ? "ON" : "OFF"))
                .formatted(enabled ? Formatting.GREEN : Formatting.RED);
    }

    private void loadLogs() {
        formattedLogs.clear();
        List<String> logs = AntiCrashLogManager.getLogs();
        if (logs.isEmpty()) {
            formattedLogs.add(Text.of("No crash attempts detected."));
        } else {
            for (String log : logs) {
                String pretty = NbtUtils.prettyPrintNbt(log);
                for (String line : pretty.split("\n")) {
                    formattedLogs.add(NbtFormatter.FORMATTER.formatSafely(line).text());
                }
                formattedLogs.add(Text.literal("--------------------------------").formatted(Formatting.GRAY));
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollOffset = Math.max(0, scrollOffset - verticalAmount * 10);
        return true;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 10, 0xFFFFFF);
        
        int boxWidth = (int) (this.width * 0.9);
        int boxHeight = (int) (this.height * 0.6);
        int boxX = (this.width - boxWidth) / 2;
        int boxY = this.height - boxHeight - 40;

        context.fill(boxX - 1, boxY - 1, boxX + boxWidth + 1, boxY + boxHeight + 1, 0xFFFFFFFF);
        context.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xFF000000);

        context.enableScissor(boxX, boxY, boxX + boxWidth, boxY + boxHeight);
        int y = boxY + 5 - (int)scrollOffset;
        for (Text line : formattedLogs) {
            if (y + 10 > boxY && y < boxY + boxHeight) {
                context.drawTextWithShadow(this.textRenderer, line, boxX + 5, y, 0xFFFFFFFF);
            }
            y += 10;
        }
        context.disableScissor();
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }
}
