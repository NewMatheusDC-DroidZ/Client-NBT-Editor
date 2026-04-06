package com.trusted.systemnbteditor.gui;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Effect picker for apply_effects.
 * Single-select effect + level + hide_particles + hide_icon + ambient.
 * Allows adding multiple effect entries.
 */
public class ApplyEffectPickerScreen extends Screen {
    private final Screen parent;
    private final Consumer<List<EffectEntry>> callback;

    public record EffectEntry(String effectId, int amplifier, int duration, boolean hideParticles, boolean hideIcon, boolean ambient) {}

    private final List<EffectEntry> entries;

    // Effect list
    private List<String> allEffects = new ArrayList<>();
    private List<String> filteredEffects = new ArrayList<>();
    private String selectedEffect = null;
    private TextFieldWidget searchField;
    private TextFieldWidget levelField;
    private TextFieldWidget durationField;

    private boolean hideParticles = false;
    private boolean hideIcon = false;
    private boolean ambient = false;

    // Scrolling for effect list
    private float scrollTarget = 0;
    private float scrollSmooth = 0;
    private int scrollOffset = 0;
    private static final float LERP = 0.12f;

    private static final int POPUP_WIDTH = 380;
    private static final int POPUP_HEIGHT = 310;
    private static final int ITEM_HEIGHT = 16;
    private static final int VISIBLE_ITEMS = 7;
    private static final int LIST_HEIGHT = VISIBLE_ITEMS * ITEM_HEIGHT;

    // Scroll for entries panel
    private float entriesScrollTarget = 0;
    private float entriesScrollSmooth = 0;
    private int entriesScrollOffset = 0;

    public ApplyEffectPickerScreen(Screen parent, List<EffectEntry> initialEntries, Consumer<List<EffectEntry>> callback) {
        super(Text.of("Apply Effects Editor"));
        this.parent = parent;
        this.entries = new ArrayList<>(initialEntries != null ? initialEntries : Collections.emptyList());
        this.callback = callback;
    }

    @Override
    protected void init() {
        super.init();
        loadEffects();
        filteredEffects = new ArrayList<>(allEffects);

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int popupX = centerX - POPUP_WIDTH / 2;
        int popupY = centerY - POPUP_HEIGHT / 2;

        // Search field
        this.searchField = new TextFieldWidget(this.textRenderer, popupX + 10, popupY + 25, 160, 16, Text.of("Search"));
        this.searchField.setChangedListener(this::onSearchChanged);
        this.addDrawableChild(this.searchField);

        // Level field
        this.levelField = new TextFieldWidget(this.textRenderer, popupX + 10, popupY + 175, 60, 16, Text.of("Level"));
        this.levelField.setText("1");
        this.addDrawableChild(this.levelField);

        // Duration field (in seconds)
        this.durationField = new TextFieldWidget(this.textRenderer, popupX + 80, popupY + 175, 60, 16, Text.of("Duration"));
        this.durationField.setText("30");
        this.addDrawableChild(this.durationField);

        // Toggle buttons for hide_particles, hide_icon, ambient (green=SHOW/ON, red=HIDE/OFF)
        this.addDrawableChild(ButtonWidget.builder(getColoredToggleText("Particles", !hideParticles), button -> {
            hideParticles = !hideParticles;
            button.setMessage(getColoredToggleText("Particles", !hideParticles));
        }).dimensions(popupX + 10, popupY + 195, 90, 16).build());

        this.addDrawableChild(ButtonWidget.builder(getColoredToggleText("Icon", !hideIcon), button -> {
            hideIcon = !hideIcon;
            button.setMessage(getColoredToggleText("Icon", !hideIcon));
        }).dimensions(popupX + 105, popupY + 195, 75, 16).build());

        this.addDrawableChild(ButtonWidget.builder(getColoredToggleText("Ambient", ambient), button -> {
            ambient = !ambient;
            button.setMessage(getColoredToggleText("Ambient", ambient));
        }).dimensions(popupX + 10, popupY + 215, 80, 16).build());

        // Add button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("+ Add").formatted(Formatting.GREEN), button -> {
            if (selectedEffect != null) {
                int level = 1;
                int dur = 600;
                try { level = Integer.parseInt(levelField.getText()); } catch (Exception e) {}
                try { dur = (int)(Float.parseFloat(durationField.getText()) * 20); } catch (Exception e) {}
                entries.add(new EffectEntry(selectedEffect, level - 1, dur, hideParticles, hideIcon, ambient));
            }
        }).dimensions(popupX + 100, popupY + 215, 80, 16).build());

        // Done / Cancel
        this.addDrawableChild(ButtonWidget.builder(Text.of("Done"), button -> {
            callback.accept(entries);
            close();
        }).dimensions(popupX + 50, popupY + POPUP_HEIGHT - 30, 80, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.of("Cancel"), button -> {
            close();
        }).dimensions(popupX + POPUP_WIDTH - 130, popupY + POPUP_HEIGHT - 30, 80, 20).build());
    }

    private void loadEffects() {
        allEffects.clear();
        if (this.client != null && this.client.world != null) {
            var registry = this.client.world.getRegistryManager().getOptional(RegistryKeys.STATUS_EFFECT);
            if (registry.isPresent()) {
                for (var entry : registry.get().getEntrySet()) {
                    allEffects.add(entry.getKey().getValue().toString());
                }
            }
        }
        if (allEffects.isEmpty()) {
            String[] effects = {
                "minecraft:speed", "minecraft:slowness", "minecraft:haste", "minecraft:mining_fatigue",
                "minecraft:strength", "minecraft:instant_health", "minecraft:instant_damage",
                "minecraft:jump_boost", "minecraft:nausea", "minecraft:regeneration",
                "minecraft:resistance", "minecraft:fire_resistance", "minecraft:water_breathing",
                "minecraft:invisibility", "minecraft:blindness", "minecraft:night_vision",
                "minecraft:hunger", "minecraft:weakness", "minecraft:poison", "minecraft:wither",
                "minecraft:health_boost", "minecraft:absorption", "minecraft:saturation",
                "minecraft:glowing", "minecraft:levitation", "minecraft:luck", "minecraft:unluck",
                "minecraft:slow_falling", "minecraft:conduit_power", "minecraft:dolphins_grace",
                "minecraft:bad_omen", "minecraft:hero_of_the_village", "minecraft:darkness",
                "minecraft:trial_omen", "minecraft:raid_omen", "minecraft:wind_charged",
                "minecraft:weaving", "minecraft:oozing", "minecraft:infested"
            };
            allEffects.addAll(Arrays.asList(effects));
        }
        Collections.sort(allEffects);
    }

    private void onSearchChanged(String query) {
        this.scrollTarget = 0;
        this.scrollSmooth = 0;
        this.scrollOffset = 0;
        String q = query.toLowerCase();
        this.filteredEffects = allEffects.stream()
                .filter(s -> s.toLowerCase().contains(q))
                .collect(Collectors.toList());
    }

    @Override
    public void tick() {
        super.tick();
        int totalH = filteredEffects.size() * ITEM_HEIGHT;
        int maxScroll = Math.max(0, totalH - LIST_HEIGHT);
        if (scrollTarget < 0) scrollTarget = 0;
        if (scrollTarget > maxScroll) scrollTarget = maxScroll;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int popupX = centerX - POPUP_WIDTH / 2;
        int popupY = centerY - POPUP_HEIGHT / 2;

        // Check if scrolling the entries panel (right side)
        int entriesX = popupX + 195;
        int entriesY = popupY + 25;
        int entriesW = POPUP_WIDTH - 205;
        int entriesH = 210;
        if (mouseX >= entriesX && mouseX <= entriesX + entriesW && mouseY >= entriesY && mouseY <= entriesY + entriesH) {
            int totalH = entries.size() * 30;
            if (totalH > entriesH) {
                entriesScrollTarget -= (float) (verticalAmount * 20);
                int maxScroll = Math.max(0, totalH - entriesH);
                if (entriesScrollTarget < 0) entriesScrollTarget = 0;
                if (entriesScrollTarget > maxScroll) entriesScrollTarget = maxScroll;
                return true;
            }
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        int totalH = filteredEffects.size() * ITEM_HEIGHT;
        if (totalH > LIST_HEIGHT) {
            scrollTarget -= (float) (verticalAmount * ITEM_HEIGHT * 1.5f);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        double mouseX = click.x();
        double mouseY = click.y();
        if (click.button() == 0) {
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            int popupX = centerX - POPUP_WIDTH / 2;
            int popupY = centerY - POPUP_HEIGHT / 2;
            int listY = popupY + 45;

            // Effect list click
            if (mouseX >= popupX + 8 && mouseX <= popupX + 180 && mouseY >= listY && mouseY <= listY + LIST_HEIGHT) {
                float relY = (float) (mouseY - listY + scrollOffset);
                int dataIndex = (int) (relY / ITEM_HEIGHT);
                if (dataIndex >= 0 && dataIndex < filteredEffects.size()) {
                    selectedEffect = filteredEffects.get(dataIndex);
                    return true;
                }
            }

            // Entries panel - remove button click  
            int entriesX = popupX + 195;
            int entriesY = popupY + 25;
            int entriesW = POPUP_WIDTH - 205;
            for (int i = 0; i < entries.size(); i++) {
                int ry = entriesY + i * 30 - entriesScrollOffset;
                int removeX = entriesX + entriesW - 15;
                if (mouseX >= removeX && mouseX <= removeX + 12 && mouseY >= ry + 2 && mouseY <= ry + 14) {
                    entries.remove(i);
                    return true;
                }
            }
        }
        return super.mouseClicked(click, bl);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
        context.fill(0, 0, this.width, this.height, 0x80000000);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        scrollSmooth += (scrollTarget - scrollSmooth) * LERP;
        if (Math.abs(scrollSmooth - scrollTarget) < 0.1) scrollSmooth = scrollTarget;
        scrollOffset = Math.round(scrollSmooth);

        entriesScrollSmooth += (entriesScrollTarget - entriesScrollSmooth) * LERP;
        if (Math.abs(entriesScrollSmooth - entriesScrollTarget) < 0.1) entriesScrollSmooth = entriesScrollTarget;
        entriesScrollOffset = Math.round(entriesScrollSmooth);

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int popupX = centerX - POPUP_WIDTH / 2;
        int popupY = centerY - POPUP_HEIGHT / 2;

        // Background
        context.fill(popupX, popupY, popupX + POPUP_WIDTH, popupY + POPUP_HEIGHT, 0xFF050510);
        int borderColor = 0xFF4040a0;
        context.fill(popupX, popupY, popupX + POPUP_WIDTH, popupY + 1, borderColor);
        context.fill(popupX, popupY + POPUP_HEIGHT - 1, popupX + POPUP_WIDTH, popupY + POPUP_HEIGHT, borderColor);
        context.fill(popupX, popupY, popupX + 1, popupY + POPUP_HEIGHT, borderColor);
        context.fill(popupX + POPUP_WIDTH - 1, popupY, popupX + POPUP_WIDTH, popupY + POPUP_HEIGHT, borderColor);

        context.drawText(this.textRenderer, "Apply Effects", popupX + 10, popupY + 10, 0xFFFFFFFF, false);

        // Effect list
        int listY = popupY + 45;
        context.enableScissor(popupX + 8, listY, popupX + 180, listY + LIST_HEIGHT);
        for (int i = 0; i < filteredEffects.size(); i++) {
            int ry = listY + i * ITEM_HEIGHT - scrollOffset;
            if (ry + ITEM_HEIGHT < listY || ry > listY + LIST_HEIGHT) continue;

            String effect = filteredEffects.get(i);
            boolean isSelected = effect.equals(selectedEffect);
            boolean isHovered = mouseX >= popupX + 8 && mouseX < popupX + 180 && mouseY >= listY && mouseY < listY + LIST_HEIGHT && mouseY >= ry && mouseY < ry + ITEM_HEIGHT;

            if (isSelected) {
                context.fill(popupX + 8, ry, popupX + 178, ry + ITEM_HEIGHT, 0x60FFFFFF);
            } else if (isHovered) {
                context.fill(popupX + 8, ry, popupX + 178, ry + ITEM_HEIGHT, 0x20FFFFFF);
            }

            String displayName = effect;
            if (displayName.startsWith("minecraft:")) displayName = displayName.substring("minecraft:".length());
            context.drawText(textRenderer, Text.literal(displayName), popupX + 12, ry + 4, isSelected ? 0xFFFFFFFF : 0xFF55FFFF, false);
        }
        context.disableScissor();

        // Scrollbar for effect list
        if (filteredEffects.size() * ITEM_HEIGHT > LIST_HEIGHT) {
            int totalH = filteredEffects.size() * ITEM_HEIGHT;
            int thumbH = Math.max(6, (int) ((float) LIST_HEIGHT * LIST_HEIGHT / totalH));
            int thumbYPos = listY + (int) ((float) scrollSmooth * (LIST_HEIGHT - thumbH) / (totalH - LIST_HEIGHT));
            context.fill(popupX + 180, listY, popupX + 184, listY + LIST_HEIGHT, 0xFF333333);
            context.fill(popupX + 180, thumbYPos, popupX + 184, thumbYPos + thumbH, 0xFF8888CC);
        }

        // Labels
        context.drawText(textRenderer, "Level:", popupX + 10, popupY + 163, 0xFFAAAAAA, false);
        context.drawText(textRenderer, "Dur (sec):", popupX + 80, popupY + 163, 0xFFAAAAAA, false);

        // Entries panel (right side)
        int entriesX = popupX + 195;
        int entriesY = popupY + 25;
        int entriesW = POPUP_WIDTH - 205;
        int entriesH = 210;

        context.fill(entriesX - 2, entriesY - 2, entriesX + entriesW + 2, entriesY + entriesH + 2, 0xFF222233);
        context.drawText(textRenderer, "Added Effects:", entriesX, entriesY - 15 + 5, 0xFFFFAA00, false);

        context.enableScissor(entriesX, entriesY, entriesX + entriesW, entriesY + entriesH);
        for (int i = 0; i < entries.size(); i++) {
            EffectEntry entry = entries.get(i);
            int ry = entriesY + i * 30 - entriesScrollOffset;
            if (ry + 30 < entriesY || ry > entriesY + entriesH) continue;

            context.fill(entriesX, ry, entriesX + entriesW, ry + 28, 0xFF1A1A2E);
            context.fill(entriesX, ry + 28, entriesX + entriesW, ry + 29, 0xFF333355);

            String shortName = entry.effectId.startsWith("minecraft:") ? entry.effectId.substring("minecraft:".length()) : entry.effectId;
            context.drawText(textRenderer, shortName, entriesX + 3, ry + 3, 0xFF55FFFF, false);
            String durStr = String.format("%.1fs", entry.duration / 20.0f);
            context.drawText(textRenderer, "Lv:" + (entry.amplifier + 1) + " " + durStr, entriesX + 3, ry + 15, 0xFFAAAAAA, false);

            // Flags
            StringBuilder flags = new StringBuilder();
            if (entry.hideParticles) flags.append("P");
            if (entry.hideIcon) flags.append("I");
            if (entry.ambient) flags.append("A");
            if (flags.length() > 0) {
                context.drawText(textRenderer, flags.toString(), entriesX + entriesW - 30, ry + 15, 0xFFFFAA00, false);
            }

            // Remove X
            int removeX = entriesX + entriesW - 15;
            boolean hRemove = mouseX >= removeX && mouseX <= removeX + 12 && mouseY >= ry + 2 && mouseY <= ry + 14;
            context.fill(removeX, ry + 2, removeX + 12, ry + 14, hRemove ? 0xFFFF3333 : 0xFF660000);
            context.drawText(textRenderer, "X", removeX + 3, ry + 4, 0xFFFFFFFF, false);
        }
        context.disableScissor();

        super.render(context, mouseX, mouseY, delta);
    }

    private Text getColoredToggleText(String label, boolean isOn) {
        if (isOn) {
            return Text.literal(label + ": ").append(Text.literal("SHOW").formatted(Formatting.GREEN));
        } else {
            return Text.literal(label + ": ").append(Text.literal("HIDE").formatted(Formatting.RED));
        }
    }

    @Override
    public void close() {
        if (this.client != null) this.client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
