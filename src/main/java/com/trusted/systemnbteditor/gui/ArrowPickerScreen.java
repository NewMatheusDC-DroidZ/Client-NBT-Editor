package com.trusted.systemnbteditor.gui;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.potion.Potion;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ArrowPickerScreen extends Screen {
    private final Screen parent;
    private final Consumer<ItemStack> callback;
    private float scrollTarget = 0;
    private float scrollSmooth = 0;
    private int scrollOffset = 0;
    private ItemStack selectedArrow = null;
    private static final float LERP = 0.12f;

    private static final int POPUP_WIDTH = 300;
    private static final int POPUP_HEIGHT = 220;
    private static final int ITEM_HEIGHT = 20;
    private static final int VISIBLE_ITEMS = 8;
    private static final int LIST_HEIGHT = VISIBLE_ITEMS * ITEM_HEIGHT;

    private List<ItemStack> arrows = new ArrayList<>();

    public ArrowPickerScreen(Screen parent, Consumer<ItemStack> callback) {
        super(Text.of("Pick Arrow Type"));
        this.parent = parent;
        this.callback = callback;
    }

    @Override
    protected void init() {
        super.init();
        loadArrows();

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int popupX = centerX - POPUP_WIDTH / 2;
        int popupY = centerY - POPUP_HEIGHT / 2;

        this.addDrawableChild(ButtonWidget.builder(Text.of("Select"), button -> {
            if (selectedArrow != null) {
                callback.accept(selectedArrow);
            }
        }).dimensions(popupX + POPUP_WIDTH - 80, popupY + POPUP_HEIGHT - 30, 70, 20).build());
        
        this.addDrawableChild(ButtonWidget.builder(Text.of("Cancel"), button -> {
            close();
        }).dimensions(popupX + 10, popupY + POPUP_HEIGHT - 30, 70, 20).build());
    }

    private void loadArrows() {
        arrows.clear();
        arrows.add(new ItemStack(Items.ARROW));
        arrows.add(new ItemStack(Items.SPECTRAL_ARROW));
        
        if (this.client != null && this.client.world != null) {
            String[] optimalPotions = {
                "minecraft:strong_healing", "minecraft:strong_harming", "minecraft:strong_poison",
                "minecraft:strong_regeneration", "minecraft:strong_strength", "minecraft:strong_swiftness",
                "minecraft:strong_slowness", "minecraft:strong_leaping", "minecraft:strong_turtle_master",
                "minecraft:long_fire_resistance", "minecraft:long_invisibility", "minecraft:long_night_vision",
                "minecraft:long_water_breathing", "minecraft:long_weakness", "minecraft:long_slow_falling",
                "minecraft:long_oozing", "minecraft:long_weaving", "minecraft:long_wind_charged",
                "minecraft:long_infested", "minecraft:luck"
            };
            
            var registry = this.client.world.getRegistryManager().getOptional(RegistryKeys.POTION);
            if (registry.isPresent()) {
                for (String pId : optimalPotions) {
                    registry.get().getOptional(net.minecraft.registry.RegistryKey.of(RegistryKeys.POTION, Identifier.of(pId))).ifPresent(entry -> {
                        ItemStack tArrow = new ItemStack(Items.TIPPED_ARROW);
                        tArrow.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(entry));
                        arrows.add(tArrow);
                    });
                }
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        int totalH = arrows.size() * ITEM_HEIGHT;
        int maxScroll = Math.max(0, totalH - LIST_HEIGHT);
        if (scrollTarget < 0) scrollTarget = 0;
        if (scrollTarget > maxScroll) scrollTarget = maxScroll;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int totalH = arrows.size() * ITEM_HEIGHT;
        if (totalH > LIST_HEIGHT) {
            scrollTarget -= (float) (verticalAmount * ITEM_HEIGHT * 2);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private int draggingSlider = 0;

    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        double mouseX = click.x();
        double mouseY = click.y();
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int popupX = centerX - POPUP_WIDTH / 2;
        int popupY = centerY - POPUP_HEIGHT / 2;
        int listY = popupY + 30;

        if (click.button() == 0) {
            if (arrows.size() * ITEM_HEIGHT > LIST_HEIGHT) {
                int trackX = popupX + POPUP_WIDTH - 8;
                if (mouseX >= trackX && mouseX <= trackX + 4 && mouseY >= listY && mouseY <= listY + LIST_HEIGHT) {
                    draggingSlider = 1;
                    updateSliderPos(mouseY);
                    return true;
                }
            }
            if (mouseX >= popupX + 8 && mouseX <= popupX + POPUP_WIDTH - 8 && mouseY >= listY && mouseY <= listY + LIST_HEIGHT) {
                float relY = (float) (mouseY - listY + scrollOffset);
                int dataIndex = (int) (relY / ITEM_HEIGHT);
                if (dataIndex >= 0 && dataIndex < arrows.size()) {
                    selectedArrow = arrows.get(dataIndex);
                    return true;
                }
            }
        }
        return super.mouseClicked(click, bl);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (click.button() == 0) draggingSlider = 0;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        if (click.button() == 0 && draggingSlider != 0) {
            updateSliderPos(click.y());
            return true;
        }
        return super.mouseDragged(click, dx, dy);
    }

    private void updateSliderPos(double mouseY) {
        int centerY = this.height / 2;
        int popupY = centerY - POPUP_HEIGHT / 2;
        int listY = popupY + 30;
        float rel = (float)(mouseY - listY) / LIST_HEIGHT;
        int totalH = arrows.size() * ITEM_HEIGHT;
        scrollTarget = rel * (totalH - LIST_HEIGHT);
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

        context.drawText(this.textRenderer, "Pick Arrow Type", popupX + 10, popupY + 10, 0xFFFFFFFF, false);

        int listY = popupY + 30;
        int rowAqua = 0xFF55FFFF;
        
        context.enableScissor(popupX + 8, listY, popupX + POPUP_WIDTH - 8, listY + LIST_HEIGHT);
        for (int i = 0; i < arrows.size(); i++) {
            int ry = listY + i * ITEM_HEIGHT - scrollOffset;
            if (ry + ITEM_HEIGHT < listY || ry > listY + LIST_HEIGHT) continue;

            ItemStack arr = arrows.get(i);
            boolean hovered = mouseX >= popupX + 8 && mouseX < popupX + POPUP_WIDTH - 8 && mouseY >= listY && mouseY < listY + LIST_HEIGHT && mouseY >= ry && mouseY < ry + ITEM_HEIGHT;
            boolean selected = arr == selectedArrow;

            if (selected) {
                context.fill(popupX + 8, ry, popupX + POPUP_WIDTH - 16, ry + ITEM_HEIGHT, 0x60FFFFFF);
            } else if (hovered) {
                context.fill(popupX + 8, ry, popupX + POPUP_WIDTH - 16, ry + ITEM_HEIGHT, 0x20FFFFFF);
            }

            Text label = arr.getName();
            context.drawText(this.textRenderer, label, popupX + 35, ry + 6, selected ? 0xFFFFFFFF : rowAqua, false);
            context.drawItem(arr, popupX + 12, ry + 2);
        }
        context.disableScissor();

        if (arrows.size() * ITEM_HEIGHT > LIST_HEIGHT) {
            int totalH = arrows.size() * ITEM_HEIGHT;
            int thumbH = Math.max(6, (int)((float)LIST_HEIGHT * LIST_HEIGHT / totalH));
            int thumbY = listY + (int)((float)scrollSmooth * (LIST_HEIGHT - thumbH) / (totalH - LIST_HEIGHT));
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
}
