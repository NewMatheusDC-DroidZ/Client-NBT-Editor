package com.trusted.systemnbteditor.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.Formatting;
import com.trusted.systemnbteditor.data.ModConfig;

public class SecretFeaturesScreen extends Screen {
    private final Screen parent;
    private ModConfig config;
    
    // Text field widgets for limits
    private TextFieldWidget particleLimitField;
    private TextFieldWidget entityLimitField;
    private TextFieldWidget entityNameLimitField;
    private TextFieldWidget itemNameLimitField;
    private TextFieldWidget itemTooltipLimitField;
    private TextFieldWidget chatMessageLimitField;
    private TextFieldWidget entityScaleLimitField;
   
    // Toggle button references
    private ButtonWidget particleToggle;
    private ButtonWidget entityToggle;
    private ButtonWidget entityNameToggle;
    private ButtonWidget itemNameToggle;
    private ButtonWidget itemTooltipToggle;
    private ButtonWidget chatMessageToggle;
    private ButtonWidget entityScaleToggle;
    private ButtonWidget fireworkToggle;
    private ButtonWidget elderGuardianToggle;
    private ButtonWidget translationToggle;
    private ButtonWidget tridentToggle;

    public SecretFeaturesScreen(Screen parent) {
        super(Text.of("Advanced AntiCrash Features"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        config = ModConfig.getInstance();
        
        int labelX = 20;
        int buttonX = 180;
        int fieldX = 230;
        int startY = 40;
        int spacing = 35;
        int currentY = startY;
        
        // 1. Particle Limit
        addLabelWidget("Particle Limit", labelX, currentY);
        particleToggle = addDrawableChild(ButtonWidget.builder(
            Text.of(config.particleLimitEnabled ? "ON" : "OFF").copy().formatted(
                config.particleLimitEnabled ? Formatting.GREEN : Formatting.RED),
            btn -> {
                config.particleLimitEnabled = !config.particleLimitEnabled;
                btn.setMessage(Text.of(config.particleLimitEnabled ? "ON" : "OFF").copy().formatted(
                    config.particleLimitEnabled ? Formatting.GREEN : Formatting.RED));
            }).dimensions(buttonX, currentY, 40, 20).build());
        
        particleLimitField = createLimitField(fieldX, currentY, String.valueOf(config.particleThreshold));
        particleLimitField.setChangedListener(s -> {
            try { config.particleThreshold = Integer.parseInt(s); } catch (Exception ignored) {}
        });
        addDrawableChild(particleLimitField);
        currentY += spacing;
        
        // 2. Entity Limit
        addLabelWidget("Entity Limit", labelX, currentY);
        entityToggle = addDrawableChild(ButtonWidget.builder(
            Text.of(config.entityLimitEnabled ? "ON" : "OFF").copy().formatted(
                config.entityLimitEnabled ? Formatting.GREEN : Formatting.RED),
            btn -> {
                config.entityLimitEnabled = !config.entityLimitEnabled;
                btn.setMessage(Text.of(config.entityLimitEnabled ? "ON" : "OFF").copy().formatted(
                    config.entityLimitEnabled ? Formatting.GREEN : Formatting.RED));
            }).dimensions(buttonX, currentY, 40, 20).build());
        
        entityLimitField = createLimitField(fieldX, currentY, String.valueOf(config.entityThreshold));
        entityLimitField.setChangedListener(s -> {
            try { config.entityThreshold = Integer.parseInt(s); } catch (Exception ignored) {}
        });
        addDrawableChild(entityLimitField);
        currentY += spacing;
        
        // 3. Entity Name Limit
        addLabelWidget("Entity Name Limit", labelX, currentY);
        entityNameToggle = addDrawableChild(ButtonWidget.builder(
            Text.of(config.entityNameLimitEnabled ? "ON" : "OFF").copy().formatted(
                config.entityNameLimitEnabled ? Formatting.GREEN : Formatting.RED),
            btn -> {
                config.entityNameLimitEnabled = !config.entityNameLimitEnabled;
                btn.setMessage(Text.of(config.entityNameLimitEnabled ? "ON" : "OFF").copy().formatted(
                    config.entityNameLimitEnabled ? Formatting.GREEN : Formatting.RED));
            }).dimensions(buttonX, currentY, 40, 20).build());
        
        entityNameLimitField = createLimitField(fieldX, currentY, String.valueOf(config.lengthThreshold));
        entityNameLimitField.setChangedListener(s -> {
            try { config.lengthThreshold = Integer.parseInt(s); } catch (Exception ignored) {}
        });
        addDrawableChild(entityNameLimitField);
        currentY += spacing;
        
        // 4. Item Name Limit
        addLabelWidget("Item Name Limit", labelX, currentY);
        itemNameToggle = addDrawableChild(ButtonWidget.builder(
            Text.of(config.itemNameLimitEnabled ? "ON" : "OFF").copy().formatted(
                config.itemNameLimitEnabled ? Formatting.GREEN : Formatting.RED),
            btn -> {
                config.itemNameLimitEnabled = !config.itemNameLimitEnabled;
                btn.setMessage(Text.of(config.itemNameLimitEnabled ? "ON" : "OFF").copy().formatted(
                    config.itemNameLimitEnabled ? Formatting.GREEN : Formatting.RED));
            }).dimensions(buttonX, currentY, 40, 20).build());
        
        itemNameLimitField = createLimitField(fieldX, currentY, String.valueOf(config.lengthThreshold));
        itemNameLimitField.setChangedListener(s -> {
            try { config.lengthThreshold = Integer.parseInt(s); } catch (Exception ignored) {}
        });
        addDrawableChild(itemNameLimitField);
        currentY += spacing;
        
        // 5. Item Tooltip/Lore Limit
        addLabelWidget("Item Tooltip/Lore Limit", labelX, currentY);
        itemTooltipToggle = addDrawableChild(ButtonWidget.builder(
            Text.of(config.itemTooltipLimitEnabled ? "ON" : "OFF").copy().formatted(
                config.itemTooltipLimitEnabled ? Formatting.GREEN : Formatting.RED),
            btn -> {
                config.itemTooltipLimitEnabled = !config.itemTooltipLimitEnabled;
                btn.setMessage(Text.of(config.itemTooltipLimitEnabled ? "ON" : "OFF").copy().formatted(
                    config.itemTooltipLimitEnabled ? Formatting.GREEN : Formatting.RED));
            }).dimensions(buttonX, currentY, 40, 20).build());
        
        itemTooltipLimitField = createLimitField(fieldX, currentY, String.valueOf(config.itemTooltipThreshold));
        itemTooltipLimitField.setChangedListener(s -> {
            try { config.itemTooltipThreshold = Integer.parseInt(s); } catch (Exception ignored) {}
        });
        addDrawableChild(itemTooltipLimitField);
        currentY += spacing;
        
        // 6. Chat Message Limit
        addLabelWidget("Chat Message Limit", labelX, currentY);
        chatMessageToggle = addDrawableChild(ButtonWidget.builder(
            Text.of(config.chatMessageLimitEnabled ? "ON" : "OFF").copy().formatted(
                config.chatMessageLimitEnabled ? Formatting.GREEN : Formatting.RED),
            btn -> {
                config.chatMessageLimitEnabled = !config.chatMessageLimitEnabled;
                btn.setMessage(Text.of(config.chatMessageLimitEnabled ? "ON" : "OFF").copy().formatted(
                    config.chatMessageLimitEnabled ? Formatting.GREEN : Formatting.RED));
            }).dimensions(buttonX, currentY, 40, 20).build());
        
        chatMessageLimitField = createLimitField(fieldX, currentY, String.valueOf(config.chatMessageThreshold));
        chatMessageLimitField.setChangedListener(s -> {
            try { config.chatMessageThreshold = Integer.parseInt(s); } catch (Exception ignored) {}
        });
        addDrawableChild(chatMessageLimitField);
        currentY += spacing;
        
        // 7. Entity Scale Limit
        addLabelWidget("Entity Scale Limit", labelX, currentY);
        entityScaleToggle = addDrawableChild(ButtonWidget.builder(
            Text.of(config.entityScaleLimitEnabled ? "ON" : "OFF").copy().formatted(
                config.entityScaleLimitEnabled ? Formatting.GREEN : Formatting.RED),
            btn -> {
                config.entityScaleLimitEnabled = !config.entityScaleLimitEnabled;
                btn.setMessage(Text.of(config.entityScaleLimitEnabled ? "ON" : "OFF").copy().formatted(
                    config.entityScaleLimitEnabled ? Formatting.GREEN : Formatting.RED));
            }).dimensions(buttonX, currentY, 40, 20).build());
        
        entityScaleLimitField = createLimitField(fieldX, currentY, String.valueOf(config.entityScaleThreshold));
        entityScaleLimitField.setChangedListener(s -> {
            try { config.entityScaleThreshold = Float.parseFloat(s); } catch (Exception ignored) {}
        });
        addDrawableChild(entityScaleLimitField);
        currentY += spacing;
        
        // 8. Firework Render Cancel (no limit field)
        addLabelWidget("Firework Render Cancel", labelX, currentY);
        fireworkToggle = addDrawableChild(ButtonWidget.builder(
            Text.of(config.cancelFireworks ? "ON" : "OFF").copy().formatted(
                config.cancelFireworks ? Formatting.GREEN : Formatting.RED),
            btn -> {
                config.cancelFireworks = !config.cancelFireworks;
                btn.setMessage(Text.of(config.cancelFireworks ? "ON" : "OFF").copy().formatted(
                    config.cancelFireworks ? Formatting.GREEN : Formatting.RED));
            }).dimensions(buttonX, currentY, 40, 20).build());
        currentY += spacing;
        
        // 9. Elder Guardian Particle Cancel (no limit field)
        addLabelWidget("Elder Guardian Particle Cancel", labelX, currentY);
        elderGuardianToggle = addDrawableChild(ButtonWidget.builder(
            Text.of(config.cancelElderGuardian ? "ON" : "OFF").copy().formatted(
                config.cancelElderGuardian ? Formatting.GREEN : Formatting.RED),
            btn -> {
                config.cancelElderGuardian = !config.cancelElderGuardian;
                btn.setMessage(Text.of(config.cancelElderGuardian ? "ON" : "OFF").copy().formatted(
                    config.cancelElderGuardian ? Formatting.GREEN : Formatting.RED));
            }).dimensions(buttonX, currentY, 40, 20).build());
        currentY += spacing;
        
        addLabelWidget("Translation Protection", labelX, currentY);
        translationToggle = addDrawableChild(ButtonWidget.builder(
            getModeText(config.translationProtectionMode),
            btn -> {
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
                btn.setMessage(getModeText(config.translationProtectionMode));
            }).dimensions(buttonX, currentY, 60, 20).build()); // Slightly wider for "SIMPLE"
        currentY += spacing;

        // 11. Trident Protection
        addLabelWidget("Trident Protection", labelX, currentY);
        tridentToggle = addDrawableChild(ButtonWidget.builder(
            Text.of(config.tridentProtectionEnabled ? "ON" : "OFF").copy().formatted(
                config.tridentProtectionEnabled ? Formatting.GREEN : Formatting.RED),
            btn -> {
                config.tridentProtectionEnabled = !config.tridentProtectionEnabled;
                btn.setMessage(Text.of(config.tridentProtectionEnabled ? "ON" : "OFF").copy().formatted(
                    config.tridentProtectionEnabled ? Formatting.GREEN : Formatting.RED));
            }).dimensions(buttonX, currentY, 40, 20).build());
        
        // Save Button (bottom right corner)
        this.addDrawableChild(ButtonWidget.builder(Text.of("Save"), button -> {
            ModConfig.save();
            if (this.client != null) this.client.setScreen(this.parent);
        }).dimensions(this.width - 110, this.height - 30, 100, 20).build());
    }
    
    private void addLabelWidget(String text, int x, int y) {
        TextFieldWidget label = new TextFieldWidget(textRenderer, x, y, 150, 20, Text.of(text));
        label.setText(text);
        label.setEditable(false);
        label.setFocusUnlocked(false);
        addDrawableChild(label);
    }
    
    private TextFieldWidget createLimitField(int x, int y, String initialValue) {
        TextFieldWidget field = new TextFieldWidget(textRenderer, x, y, 60, 20, Text.of("Limit"));
        field.setText(initialValue);
        field.setPlaceholder(Text.of("Limit"));
        return field;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Background
        context.fill(0, 0, this.width, this.height, 0xC0000000);
        super.render(context, mouseX, mouseY, delta);
        
        // Title
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 15, 0xFFFFFF);
    }

    @Override
    public void close() {
        ModConfig.save();
        if (this.client != null) this.client.setScreen(this.parent);
    }

    private Text getModeText(ModConfig.TranslationProtectionMode mode) {
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
        return Text.literal(mode.name()).formatted(color);
    }
}
