package com.trusted.systemnbteditor.gui;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Multi-select effect picker for remove_effects.
 * Lists all Minecraft status effects with toggle selection.
 */
public class EffectPickerScreen extends Screen {
    private final Screen parent;
    private final Consumer<Set<String>> callback;
    private final Set<String> selectedEffects;

    private float scrollTarget = 0;
    private float scrollSmooth = 0;
    private int scrollOffset = 0;
    private static final float LERP = 0.12f;

    private static final int POPUP_WIDTH = 320;
    private static final int POPUP_HEIGHT = 260;
    private static final int ITEM_HEIGHT = 20;
    private static final int VISIBLE_ITEMS = 9;
    private static final int LIST_HEIGHT = VISIBLE_ITEMS * ITEM_HEIGHT;

    private TextFieldWidget searchField;
    private List<String> allEffects = new ArrayList<>();
    private List<String> filteredEffects = new ArrayList<>();

    public EffectPickerScreen(Screen parent, Set<String> initialSelected, Consumer<Set<String>> callback) {
        super(Text.of("Remove Effects Picker"));
        this.parent = parent;
        this.selectedEffects = new LinkedHashSet<>(initialSelected != null ? initialSelected : Collections.emptySet());
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

        this.searchField = new TextFieldWidget(this.textRenderer, popupX + 10, popupY + 25, POPUP_WIDTH - 20, 20, Text.of("Search Effect"));
        this.searchField.setChangedListener(this::onSearchChanged);
        this.addDrawableChild(this.searchField);

        this.addDrawableChild(ButtonWidget.builder(Text.of("Done"), button -> {
            callback.accept(selectedEffects);
            close();
        }).dimensions(popupX + 50, popupY + POPUP_HEIGHT - 30, 100, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.of("Cancel"), button -> {
            close();
        }).dimensions(popupX + POPUP_WIDTH - 150, popupY + POPUP_HEIGHT - 30, 100, 20).build());
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
        // Fallback if registry is empty
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
            int listY = popupY + 50;

            if (mouseX >= popupX + 8 && mouseX <= popupX + POPUP_WIDTH - 8 && mouseY >= listY && mouseY <= listY + LIST_HEIGHT) {
                float relY = (float) (mouseY - listY + scrollOffset);
                int dataIndex = (int) (relY / ITEM_HEIGHT);
                if (dataIndex >= 0 && dataIndex < filteredEffects.size()) {
                    String effect = filteredEffects.get(dataIndex);
                    if (selectedEffects.contains(effect)) {
                        selectedEffects.remove(effect);
                    } else {
                        selectedEffects.add(effect);
                    }
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

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int popupX = centerX - POPUP_WIDTH / 2;
        int popupY = centerY - POPUP_HEIGHT / 2;

        context.fill(popupX, popupY, popupX + POPUP_WIDTH, popupY + POPUP_HEIGHT, 0xFF050510);
        int borderColor = 0xFF4040a0;
        context.fill(popupX, popupY, popupX + POPUP_WIDTH, popupY + 1, borderColor);
        context.fill(popupX, popupY + POPUP_HEIGHT - 1, popupX + POPUP_WIDTH, popupY + POPUP_HEIGHT, borderColor);
        context.fill(popupX, popupY, popupX + 1, popupY + POPUP_HEIGHT, borderColor);
        context.fill(popupX + POPUP_WIDTH - 1, popupY, popupX + POPUP_WIDTH, popupY + POPUP_HEIGHT, borderColor);

        context.drawText(this.textRenderer, "Remove Effects", popupX + 10, popupY + 10, 0xFFFFFFFF, false);

        // Selected count
        String countStr = selectedEffects.size() + " selected";
        context.drawText(this.textRenderer, countStr, popupX + POPUP_WIDTH - textRenderer.getWidth(countStr) - 10, popupY + 10, 0xFF55FF55, false);

        int listY = popupY + 50;
        context.enableScissor(popupX + 8, listY, popupX + POPUP_WIDTH - 8, listY + LIST_HEIGHT);

        for (int i = 0; i < filteredEffects.size(); i++) {
            int ry = listY + i * ITEM_HEIGHT - scrollOffset;
            if (ry + ITEM_HEIGHT < listY || ry > listY + LIST_HEIGHT) continue;

            String effect = filteredEffects.get(i);
            boolean isSelected = selectedEffects.contains(effect);
            boolean isHovered = mouseX >= popupX + 8 && mouseX < popupX + POPUP_WIDTH - 8 && mouseY >= listY && mouseY < listY + LIST_HEIGHT && mouseY >= ry && mouseY < ry + ITEM_HEIGHT;

            if (isSelected) {
                context.fill(popupX + 8, ry, popupX + POPUP_WIDTH - 16, ry + ITEM_HEIGHT, 0x40FF5555);
            } else if (isHovered) {
                context.fill(popupX + 8, ry, popupX + POPUP_WIDTH - 16, ry + ITEM_HEIGHT, 0x20FFFFFF);
            }

            // Checkbox indicator
            int cbX = popupX + 12;
            int cbY = ry + 4;
            context.fill(cbX, cbY, cbX + 12, cbY + 12, 0xFF333333);
            context.fill(cbX + 1, cbY + 1, cbX + 11, cbY + 11, isSelected ? 0xFF55FF55 : 0xFF111111);
            if (isSelected) {
                context.drawText(textRenderer, "✓", cbX + 2, cbY + 2, 0xFF000000, false);
            }

            // Effect name
            if (effect.startsWith("minecraft:")) {
                String suffix = effect.substring("minecraft:".length());
                int textX = popupX + 30;
                int textY = ry + 6;
                context.drawText(textRenderer, Text.literal("minecraft:").formatted(net.minecraft.util.Formatting.GREEN), textX, textY, 0xFF55FF55, false);
                int prefixW = textRenderer.getWidth("minecraft:");
                context.drawText(textRenderer, Text.literal(suffix), textX + prefixW, textY, isSelected ? 0xFFFFFFFF : 0xFFAAAAAA, false);
            } else {
                context.drawText(textRenderer, Text.literal(effect), popupX + 30, ry + 6, isSelected ? 0xFFFFFFFF : 0xFF55FFFF, false);
            }
        }
        context.disableScissor();

        // Scrollbar
        if (filteredEffects.size() * ITEM_HEIGHT > LIST_HEIGHT) {
            int totalH = filteredEffects.size() * ITEM_HEIGHT;
            int thumbH = Math.max(6, (int) ((float) LIST_HEIGHT * LIST_HEIGHT / totalH));
            int thumbY = listY + (int) ((float) scrollSmooth * (LIST_HEIGHT - thumbH) / (totalH - LIST_HEIGHT));
            int trackX = popupX + POPUP_WIDTH - 8;
            context.fill(trackX, listY, trackX + 4, listY + LIST_HEIGHT, 0xFF333333);
            context.fill(trackX, thumbY, trackX + 4, thumbY + thumbH, 0xFF8888CC);
        }

        super.render(context, mouseX, mouseY, delta);
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
