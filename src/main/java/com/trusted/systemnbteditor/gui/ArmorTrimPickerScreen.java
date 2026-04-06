package com.trusted.systemnbteditor.gui;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import java.util.ArrayList;
import java.util.List;

public class ArmorTrimPickerScreen extends Screen {
    private final Screen parent;
    private final TrimSelectionCallback callback;
    
    private String selectedPattern = null;
    private String selectedMaterial = null;

    private static final int POPUP_WIDTH = 400;
    private static final int POPUP_HEIGHT = 280;
    private static final int ITEM_HEIGHT = 20;
    private static final int LIST_HEIGHT = 180;
    private static final float LERP = 0.12f;

    private final List<TrimEntry> patterns = new ArrayList<>();
    private final List<TrimEntry> materials = new ArrayList<>();

    private float patternScrollTarget = 0;
    private float patternScrollSmooth = 0;
    private float materialScrollTarget = 0;
    private float materialScrollSmooth = 0;

    public interface TrimSelectionCallback {
        void onTrimSelected(String pattern, String material);
    }

    private record TrimEntry(String id, String name, ItemStack icon) {}

    public ArmorTrimPickerScreen(Screen parent, String initialPattern, String initialMaterial, TrimSelectionCallback callback) {
        super(Text.of("Select Armor Trim"));
        this.parent = parent;
        this.callback = callback;
        this.selectedPattern = initialPattern;
        this.selectedMaterial = initialMaterial;

        initData();
    }

    private void initData() {
        // Patterns
        addPattern("coast", "Coast", Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE);
        addPattern("dune", "Dune", Items.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE);
        addPattern("eye", "Eye", Items.EYE_ARMOR_TRIM_SMITHING_TEMPLATE);
        addPattern("host", "Host", Items.HOST_ARMOR_TRIM_SMITHING_TEMPLATE);
        addPattern("raiser", "Raiser", Items.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE);
        addPattern("rib", "Rib", Items.RIB_ARMOR_TRIM_SMITHING_TEMPLATE);
        addPattern("sentry", "Sentry", Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE);
        addPattern("shaper", "Shaper", Items.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE);
        addPattern("silence", "Silence", Items.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE);
        addPattern("snout", "Snout", Items.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE);
        addPattern("spire", "Spire", Items.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE);
        addPattern("tide", "Tide", Items.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE);
        addPattern("vex", "Vex", Items.VEX_ARMOR_TRIM_SMITHING_TEMPLATE);
        addPattern("ward", "Ward", Items.WARD_ARMOR_TRIM_SMITHING_TEMPLATE);
        addPattern("wayfinder", "Wayfinder", Items.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE);
        addPattern("wild", "Wild", Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE);
        addPattern("flow", "Flow", Items.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE);
        addPattern("bolt", "Bolt", Items.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE);

        // Materials
        addMaterial("amethyst", "Amethyst", Items.AMETHYST_SHARD);
        addMaterial("copper", "Copper", Items.COPPER_INGOT);
        addMaterial("diamond", "Diamond", Items.DIAMOND);
        addMaterial("emerald", "Emerald", Items.EMERALD);
        addMaterial("gold", "Gold", Items.GOLD_INGOT);
        addMaterial("iron", "Iron", Items.IRON_INGOT);
        addMaterial("lapis", "Lapis", Items.LAPIS_LAZULI);
        addMaterial("netherite", "Netherite", Items.NETHERITE_INGOT);
        addMaterial("quartz", "Quartz", Items.QUARTZ);
        addMaterial("redstone", "Redstone", Items.REDSTONE);
    }

    private void addPattern(String id, String name, net.minecraft.item.Item item) {
        patterns.add(new TrimEntry("minecraft:" + id, name, item.getDefaultStack()));
    }

    private void addMaterial(String id, String name, net.minecraft.item.Item item) {
        materials.add(new TrimEntry("minecraft:" + id, name, item.getDefaultStack()));
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int popupX = centerX - POPUP_WIDTH / 2;
        int popupY = centerY - POPUP_HEIGHT / 2;

        this.addDrawableChild(ButtonWidget.builder(Text.of("Done"), button -> {
            if (selectedPattern != null && selectedMaterial != null) {
                callback.onTrimSelected(selectedPattern, selectedMaterial);
            }
            this.close();
        }).dimensions(popupX + 50, popupY + POPUP_HEIGHT - 30, 120, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.of("Cancel"), button -> this.close())
                .dimensions(popupX + POPUP_WIDTH - 170, popupY + POPUP_HEIGHT - 30, 120, 20).build());
    }

    @Override
    public void tick() {
        super.tick();
        
        // Pattern Scroll Bounds
        int maxPattern = Math.max(0, (patterns.size() * ITEM_HEIGHT) - LIST_HEIGHT);
        if (patternScrollTarget < 0) patternScrollTarget = 0;
        if (patternScrollTarget > maxPattern) patternScrollTarget = maxPattern;
        
        // Material Scroll Bounds
        int maxMaterial = Math.max(0, (materials.size() * ITEM_HEIGHT) - LIST_HEIGHT);
        if (materialScrollTarget < 0) materialScrollTarget = 0;
        if (materialScrollTarget > maxMaterial) materialScrollTarget = maxMaterial;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int centerX = this.width / 2;
        if (mouseX < centerX) {
            patternScrollTarget -= (float) (verticalAmount * ITEM_HEIGHT * 1.5f);
            return true;
        } else {
            materialScrollTarget -= (float) (verticalAmount * ITEM_HEIGHT * 1.5f);
            return true;
        }
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
            int listY = popupY + 40;

            // Pattern Column
            if (mouseX >= popupX + 10 && mouseX < centerX - 5 && mouseY >= listY && mouseY < listY + LIST_HEIGHT) {
                int index = (int) ((mouseY - listY + Math.round(patternScrollSmooth)) / ITEM_HEIGHT);
                if (index >= 0 && index < patterns.size()) {
                    selectedPattern = patterns.get(index).id();
                    return true;
                }
            }
            // Material Column
            if (mouseX >= centerX + 5 && mouseX < popupX + POPUP_WIDTH - 10 && mouseY >= listY && mouseY < listY + LIST_HEIGHT) {
                int index = (int) ((mouseY - listY + Math.round(materialScrollSmooth)) / ITEM_HEIGHT);
                if (index >= 0 && index < materials.size()) {
                    selectedMaterial = materials.get(index).id();
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
        // Smooth Lerp in Render for 60Hz+ feel
        patternScrollSmooth += (patternScrollTarget - patternScrollSmooth) * LERP;
        if (Math.abs(patternScrollSmooth - patternScrollTarget) < 0.1) patternScrollSmooth = patternScrollTarget;

        materialScrollSmooth += (materialScrollTarget - materialScrollSmooth) * LERP;
        if (Math.abs(materialScrollSmooth - materialScrollTarget) < 0.1) materialScrollSmooth = materialScrollTarget;

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

        context.drawCenteredTextWithShadow(textRenderer, "Armor Trim Selection", centerX, popupY + 10, 0xFFFFFFFF);

        context.drawText(textRenderer, "Pattern", popupX + 20, popupY + 28, 0xFFAAAAAA, false);
        context.drawText(textRenderer, "Material", centerX + 20, popupY + 28, 0xFFAAAAAA, false);

        int listY = popupY + 40;
        
        // Patterns
        context.enableScissor(popupX + 10, listY, centerX - 5, listY + LIST_HEIGHT);
        renderList(context, patterns, selectedPattern, popupX + 10, listY, centerX - 20, mouseX, mouseY, Math.round(patternScrollSmooth));
        context.disableScissor();

        // Materials
        context.enableScissor(centerX + 5, listY, popupX + POPUP_WIDTH - 10, listY + LIST_HEIGHT);
        renderList(context, materials, selectedMaterial, centerX + 5, listY, popupX + POPUP_WIDTH - 20, mouseX, mouseY, Math.round(materialScrollSmooth));
        context.disableScissor();

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderList(DrawContext context, List<TrimEntry> entries, String selected, int x, int y, int maxX, int mouseX, int mouseY, int scroll) {
        for (int i = 0; i < entries.size(); i++) {
            int ry = y + i * ITEM_HEIGHT - scroll;
            if (ry + ITEM_HEIGHT < y || ry > y + LIST_HEIGHT) continue;

            TrimEntry entry = entries.get(i);
            boolean isSelected = entry.id().equals(selected);
            boolean isHovered = mouseX >= x && mouseX < maxX && mouseY >= ry && mouseY < ry + ITEM_HEIGHT;

            if (isSelected) {
                context.fill(x, ry, maxX, ry + ITEM_HEIGHT, 0x4000AA00);
            } else if (isHovered) {
                context.fill(x, ry, maxX, ry + ITEM_HEIGHT, 0x20FFFFFF);
            }

            context.drawItem(entry.icon(), x + 2, ry + 2);
            context.drawText(textRenderer, entry.name(), x + 25, ry + 6, isSelected ? 0xFFFFFFFF : 0xFFAAAAAA, false);
        }
    }

    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }
}
