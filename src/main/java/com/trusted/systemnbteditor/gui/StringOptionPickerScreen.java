package com.trusted.systemnbteditor.gui;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * A generic string option picker, modeled after ComponentPickerScreen but without icons.
 * Used for animation, sound_event, has_consume_particles, play_sound etc.
 */
public class StringOptionPickerScreen extends Screen {
    private final Screen parent;
    private final Consumer<String> callback;
    private final String title2;
    private final List<String> allOptions;
    private List<String> filteredOptions;
    private String selected;

    private float scrollTarget = 0;
    private float scrollSmooth = 0;
    private int scrollOffset = 0;
    private static final float LERP = 0.12f;

    private static final int POPUP_WIDTH = 320;
    private static final int POPUP_HEIGHT = 240;
    private static final int ITEM_HEIGHT = 20;
    private static final int VISIBLE_ITEMS = 8;
    private static final int LIST_HEIGHT = VISIBLE_ITEMS * ITEM_HEIGHT;

    private TextFieldWidget searchField;

    public StringOptionPickerScreen(Screen parent, String title, List<String> options, String currentValue, Consumer<String> callback) {
        super(Text.of(title));
        this.parent = parent;
        this.title2 = title;
        this.allOptions = new ArrayList<>(options);
        this.filteredOptions = new ArrayList<>(options);
        this.selected = currentValue;
        this.callback = callback;
    }

    @Override
    protected void init() {
        super.init();
        this.filteredOptions = new ArrayList<>(allOptions);

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int popupX = centerX - POPUP_WIDTH / 2;
        int popupY = centerY - POPUP_HEIGHT / 2;

        this.searchField = new TextFieldWidget(this.textRenderer, popupX + 10, popupY + 25, POPUP_WIDTH - 20, 20, Text.of("Search"));
        this.searchField.setChangedListener(this::onSearchChanged);
        this.addDrawableChild(this.searchField);

        this.addDrawableChild(ButtonWidget.builder(Text.of("Done"), button -> {
            if (selected != null) {
                callback.accept(selected);
            }
            close();
        }).dimensions(popupX + 50, popupY + POPUP_HEIGHT - 30, 100, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.of("Cancel"), button -> {
            close();
        }).dimensions(popupX + POPUP_WIDTH - 150, popupY + POPUP_HEIGHT - 30, 100, 20).build());
    }

    private void onSearchChanged(String query) {
        this.scrollTarget = 0;
        this.scrollSmooth = 0;
        this.scrollOffset = 0;
        String q = query.toLowerCase();
        this.filteredOptions = allOptions.stream()
                .filter(s -> s.toLowerCase().contains(q))
                .collect(Collectors.toList());
    }

    @Override
    public void tick() {
        super.tick();
        int totalH = filteredOptions.size() * ITEM_HEIGHT;
        int maxScroll = Math.max(0, totalH - LIST_HEIGHT);
        if (scrollTarget < 0) scrollTarget = 0;
        if (scrollTarget > maxScroll) scrollTarget = maxScroll;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int totalH = filteredOptions.size() * ITEM_HEIGHT;
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
                if (dataIndex >= 0 && dataIndex < filteredOptions.size()) {
                    selected = filteredOptions.get(dataIndex);
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

        context.drawText(this.textRenderer, title2, popupX + 10, popupY + 10, 0xFFFFFFFF, false);

        int listY = popupY + 50;
        context.enableScissor(popupX + 8, listY, popupX + POPUP_WIDTH - 8, listY + LIST_HEIGHT);

        for (int i = 0; i < filteredOptions.size(); i++) {
            int ry = listY + i * ITEM_HEIGHT - scrollOffset;
            if (ry + ITEM_HEIGHT < listY || ry > listY + LIST_HEIGHT) continue;

            String option = filteredOptions.get(i);
            boolean isSelected = option.equals(selected);
            boolean isHovered = mouseX >= popupX + 8 && mouseX < popupX + POPUP_WIDTH - 8 && mouseY >= listY && mouseY < listY + LIST_HEIGHT && mouseY >= ry && mouseY < ry + ITEM_HEIGHT;

            if (isSelected) {
                context.fill(popupX + 8, ry, popupX + POPUP_WIDTH - 16, ry + ITEM_HEIGHT, 0x60FFFFFF);
            } else if (isHovered) {
                context.fill(popupX + 8, ry, popupX + POPUP_WIDTH - 16, ry + ITEM_HEIGHT, 0x20FFFFFF);
            }

            if (option.startsWith("minecraft:")) {
                String suffix = option.substring("minecraft:".length());
                int textX = popupX + 14;
                int textY = ry + 6;
                context.drawText(textRenderer, Text.literal("minecraft:").formatted(net.minecraft.util.Formatting.GREEN), textX, textY, 0xFF55FF55, false);
                int prefixW = textRenderer.getWidth("minecraft:");
                context.drawText(textRenderer, Text.literal(suffix), textX + prefixW, textY, isSelected ? 0xFFFFFFFF : 0xFFAAAAAA, false);
            } else {
                context.drawText(textRenderer, Text.literal(option), popupX + 14, ry + 6, isSelected ? 0xFFFFFFFF : 0xFF55FFFF, false);
            }
        }
        context.disableScissor();

        // Scrollbar
        if (filteredOptions.size() * ITEM_HEIGHT > LIST_HEIGHT) {
            int totalH = filteredOptions.size() * ITEM_HEIGHT;
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
