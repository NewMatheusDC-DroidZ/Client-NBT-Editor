package com.trusted.systemnbteditor.gui;

import com.trusted.systemnbteditor.data.ModConfig;
import com.trusted.systemnbteditor.util.SkidCacheManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.ArrayList;

public class SkidScreen extends Screen {
    private final Screen parent;
    private ButtonWidget toggleButton;
    private ButtonWidget sizeLimitButton;
    private double scrollOffset = 0;
    private final List<Text> formattedLogs = new ArrayList<>();

    public SkidScreen(Screen parent) {
        super(Text.of("Skid Utility"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        
        int centerX = this.width / 2;
        int boxHeight = (int) (this.height * 0.6);
        int boxY = this.height - boxHeight - 40;

        this.toggleButton = ButtonWidget.builder(getToggleButtonText(), button -> {
            ModConfig config = ModConfig.getInstance();
            if (!config.skidFeatureEnabled && !TriviaScreen.skidUnlocked && !config.adminPermissions) {
                if (this.client != null) {
                    this.client.setScreen(new TriviaScreen(this, 
                        () -> {
                            TriviaScreen.skidUnlocked = true;
                            ModConfig configInner = ModConfig.getInstance();
                            configInner.skidFeatureEnabled = true;
                            SkidCacheManager.forceSkidCurrentPlayers();
                            ModConfig.save();
                            this.client.setScreen(this);
                            updateToggleButton();
                        },
                        () -> this.client.setScreen(this), true));
                }
                return;
            }
            config.skidFeatureEnabled = !config.skidFeatureEnabled;
            if (config.skidFeatureEnabled) {
                SkidCacheManager.forceSkidCurrentPlayers();
            }
            ModConfig.save();
            updateToggleButton();
        }).dimensions((this.width - 150) / 2, 25, 150, 20).build();
        this.addDrawableChild(this.toggleButton);

        // Keep after game restart button
        this.addDrawableChild(ButtonWidget.builder(getKeepRestartText(), button -> {
            ModConfig config = ModConfig.getInstance();
            config.keepSkidOnRestart = !config.keepSkidOnRestart;
            ModConfig.save();
            button.setMessage(getKeepRestartText());
        }).dimensions((this.width + 160) / 2, 25, 120, 20).build());

        // Admin Permissions button (top left)
        String myName = this.client != null && this.client.player != null ? this.client.player.getName().getString() : "";
        boolean canClickAdmin = myName.equalsIgnoreCase("Scientify") || myName.equalsIgnoreCase("TrustedSystem");
        
        this.addDrawableChild(ButtonWidget.builder(getAdminPermissionsText(), button -> {
            if (canClickAdmin) {
                ModConfig config = ModConfig.getInstance();
                config.adminPermissions = !config.adminPermissions;
                ModConfig.save();
                button.setMessage(getAdminPermissionsText());
            }
        }).dimensions(5, 5, 150, 20).build());

        this.sizeLimitButton = ButtonWidget.builder(getSizeLimitButtonText(), button -> {
            ModConfig config = ModConfig.getInstance();
            if (config.skidMaxItemSize == 100000) {
                config.skidMaxItemSize = 250000;
            } else if (config.skidMaxItemSize == 250000) {
                config.skidMaxItemSize = 1000000;
            } else if (config.skidMaxItemSize == 1000000) {
                config.skidMaxItemSize = 4000000;
            } else {
                config.skidMaxItemSize = 100000; // loop back
            }
            ModConfig.save();
            updateSizeLimitButton();
        }).dimensions(this.width - 110, 5, 105, 20).build();
        this.addDrawableChild(this.sizeLimitButton);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Clear Cache").formatted(Formatting.DARK_RED), button -> {
            SkidCacheManager.clearCache();
            updateFormattedLogs();
        }).dimensions((this.width - 115) / 2, boxY - 25, 115, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("View Items").formatted(Formatting.DARK_GREEN), button -> {
            if (this.client != null) {
                this.client.setScreen(new SkidViewScreen(this));
            }
        }).dimensions((this.width - 115) / 2, boxY - 45, 115, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.of("Back"), button -> {
            if (this.client != null) {
                this.client.setScreen(this.parent);
            }
        }).dimensions(centerX - 50, this.height - 30, 100, 20).build());
        
        updateFormattedLogs();
    }

    private void updateToggleButton() {
        if (this.toggleButton != null) {
            this.toggleButton.setMessage(getToggleButtonText());
        }
    }

    private void updateSizeLimitButton() {
        if (this.sizeLimitButton != null) {
            this.sizeLimitButton.setMessage(getSizeLimitButtonText());
        }
    }

    private Text getSizeLimitButtonText() {
        int size = ModConfig.getInstance().skidMaxItemSize;
        String sizeStr;
        Formatting color;
        
        if (size == 100000) {
            sizeStr = "100KB";
            color = Formatting.GREEN;
        } else if (size == 250000) {
            sizeStr = "250KB";
            color = Formatting.YELLOW;
        } else if (size == 1000000) {
            sizeStr = "1MB";
            color = Formatting.GOLD;
        } else {
            sizeStr = "4MB";
            color = Formatting.DARK_RED;
        }
        
        return Text.literal("Max Size: " + sizeStr).formatted(color);
    }

    private Text getToggleButtonText() {
        boolean enabled = ModConfig.getInstance().skidFeatureEnabled;
        return Text.literal("Skid Items: " + (enabled ? "ON" : "OFF"))
                .formatted(enabled ? Formatting.GREEN : Formatting.RED);
    }

    private Text getKeepRestartText() {
        boolean enabled = ModConfig.getInstance().keepSkidOnRestart;
        return Text.literal("Keep after restart: " + (enabled ? "ON" : "OFF"))
                .formatted(enabled ? Formatting.GREEN : Formatting.RED);
    }

    private Text getAdminPermissionsText() {
        boolean enabled = ModConfig.getInstance().adminPermissions;
        return Text.literal("Admin Permissions: " + (enabled ? "ON" : "OFF"))
                .formatted(enabled ? Formatting.GREEN : Formatting.RED);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollOffset = Math.max(0, scrollOffset - verticalAmount * 10);
        return true;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 15, 0xFFFFFF);

        int boxWidth = (int) (this.width * 0.9);
        int boxHeight = (int) (this.height * 0.6);
        int boxX = (this.width - boxWidth) / 2;
        int boxY = this.height - boxHeight - 40;

        context.fill(boxX - 1, boxY - 1, boxX + boxWidth + 1, boxY + boxHeight + 1, 0xFFFFFFFF);
        context.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xFF000000);

        context.enableScissor(boxX, boxY, boxX + boxWidth, boxY + boxHeight);
        int y = boxY + 5 - (int)scrollOffset;
        
        if (formattedLogs.isEmpty()) {
            context.drawTextWithShadow(this.textRenderer, Text.of("No backups captured yet."), boxX + 5, y, 0xFFFFFFFF);
        } else {
            for (Text line : formattedLogs) {
                if (y + 10 > boxY && y < boxY + boxHeight) {
                    context.drawTextWithShadow(this.textRenderer, line, boxX + 5, y, 0xFFFFFFFF);
                }
                y += 10;
            }
        }
        
        context.disableScissor();
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    private Text parseColorCodes(String text) {
        if (text == null) return Text.empty();
        net.minecraft.text.MutableText result = Text.empty();
        
        String[] parts = text.split("(?=§)");
        for (String part : parts) {
            if (part.startsWith("§") && part.length() > 1) {
                char code = part.charAt(1);
                Formatting formattingType = Formatting.byCode(code);
                String content = part.substring(2);
                if (formattingType != null) {
                    result.append(Text.literal(content).formatted(formattingType));
                } else {
                    result.append(Text.literal(content));
                }
            } else {
                result.append(Text.literal(part));
            }
        }
        return result;
    }

    private void updateFormattedLogs() {
        formattedLogs.clear();
        int maxWidth = (int) (this.width * 0.9) - 10; 
        List<String> logs = SkidCacheManager.getLogs();

        if (logs.isEmpty()) {
            formattedLogs.add(Text.of("No backups captured yet."));
        } else {
            for (String log : logs) {
                Text parsedLine = parseColorCodes(log);
                formattedLogs.add(parsedLine);
            }
        }
    }
}
