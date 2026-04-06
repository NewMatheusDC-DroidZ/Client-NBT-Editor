package com.trusted.systemnbteditor.gui;

import com.trusted.systemnbteditor.data.ComponentRegistry;
import com.trusted.systemnbteditor.data.ComponentRegistry.ComponentInfo;
import com.trusted.systemnbteditor.util.ComponentIconUtil;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MultiComponentPickerScreen extends Screen {
    private final Screen parent;
    private final MultiComponentSelectionCallback callback;
    private final Set<String> selectedComponents = new HashSet<>();
    
    private float scrollTarget = 0;
    private float scrollSmooth = 0;
    private int scrollOffset = 0;
    private static final float LERP = 0.12f;
    
    private static final int POPUP_WIDTH = 320;
    private static final int POPUP_HEIGHT = 280;
    private static final int ITEM_HEIGHT = 20;
    private static final int VISIBLE_ITEMS = 10;
    private static final int LIST_HEIGHT = VISIBLE_ITEMS * ITEM_HEIGHT;

    private TextFieldWidget searchField;
    private List<ComponentInfo> filteredComponents = new ArrayList<>();
    
    public interface MultiComponentSelectionCallback {
        void onComponentsSelected(Set<String> selectedKeys);
    }

    public MultiComponentPickerScreen(Screen parent, Set<String> initialSelection, MultiComponentSelectionCallback callback) {
        super(Text.of("Select Components to Hide"));
        this.parent = parent;
        this.callback = callback;
        if (initialSelection != null) {
            this.selectedComponents.addAll(initialSelection);
        }
    }

    @Override
    protected void init() {
        super.init();
        this.filteredComponents = new ArrayList<>(ComponentRegistry.getAllComponents());

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int popupX = centerX - POPUP_WIDTH / 2;
        int popupY = centerY - POPUP_HEIGHT / 2;

        // Search Field
        this.searchField = new TextFieldWidget(this.textRenderer, popupX + 10, popupY + 25, POPUP_WIDTH - 20, 20, Text.of("Search Component"));
        this.searchField.setChangedListener(this::onSearchChanged);
        this.addDrawableChild(this.searchField);

        // Done button
        this.addDrawableChild(ButtonWidget.builder(Text.of("Done"), button -> {
            callback.onComponentsSelected(selectedComponents);
            this.close();
        }).dimensions(popupX + 50, popupY + POPUP_HEIGHT - 30, 100, 20).build());

        // Cancel button
        this.addDrawableChild(ButtonWidget.builder(Text.of("Cancel"), button -> {
            this.close();
        }).dimensions(popupX + POPUP_WIDTH - 150, popupY + POPUP_HEIGHT - 30, 100, 20).build());
    }

    private void onSearchChanged(String query) {
        this.scrollTarget = 0;
        this.scrollSmooth = 0;
        this.scrollOffset = 0;
        String q = query.toLowerCase();
        this.filteredComponents = ComponentRegistry.getAllComponents().stream()
                .filter(c -> c.key().toLowerCase().contains(q) || c.displayName().toLowerCase().contains(q))
                .collect(Collectors.toList());
    }

    @Override
    public void tick() {
        super.tick();
        int totalH = filteredComponents.size() * ITEM_HEIGHT;
        int maxScroll = Math.max(0, totalH - LIST_HEIGHT);
        if (scrollTarget < 0) scrollTarget = 0;
        if (scrollTarget > maxScroll) scrollTarget = maxScroll;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int totalH = filteredComponents.size() * ITEM_HEIGHT;
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
                if (dataIndex >= 0 && dataIndex < filteredComponents.size()) {
                    String key = filteredComponents.get(dataIndex).key();
                    if (selectedComponents.contains(key)) {
                        selectedComponents.remove(key);
                    } else {
                        selectedComponents.add(key);
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

        context.drawText(this.textRenderer, "Select Hidden Components", popupX + 10, popupY + 10, 0xFFFFFFFF, false);

        int listY = popupY + 50;
        context.enableScissor(popupX + 8, listY, popupX + POPUP_WIDTH - 8, listY + LIST_HEIGHT);
        
        for (int i = 0; i < filteredComponents.size(); i++) {
            int ry = listY + i * ITEM_HEIGHT - scrollOffset;
            if (ry + ITEM_HEIGHT < listY || ry > listY + LIST_HEIGHT) continue;

            ComponentInfo component = filteredComponents.get(i);
            boolean isSelected = selectedComponents.contains(component.key());
            boolean isHovered = mouseX >= popupX + 8 && mouseX < popupX + POPUP_WIDTH - 8 && mouseY >= listY && mouseY < listY + LIST_HEIGHT && mouseY >= ry && mouseY < ry + ITEM_HEIGHT;
            
            if (isSelected) {
                context.fill(popupX + 8, ry, popupX + POPUP_WIDTH - 16, ry + ITEM_HEIGHT, 0x4000AA00);
            } else if (isHovered) {
                context.fill(popupX + 8, ry, popupX + POPUP_WIDTH - 16, ry + ITEM_HEIGHT, 0x20FFFFFF);
            }

            // Checkbox
            context.drawText(textRenderer, isSelected ? "[X]" : "[ ]", popupX + 12, ry + 6, isSelected ? 0xFF00FF00 : 0xFFAAAAAA, false);

            ItemStack icon = ComponentIconUtil.getIcon(component.key());
            context.drawItem(icon, popupX + 35, ry + 2);
            
            String key = component.key();
            int textX = popupX + 55;
            int textY = ry + 6;
            
            if (key.startsWith("minecraft:")) {
                String suffix = key.substring("minecraft:".length());
                context.drawText(textRenderer, Text.literal("minecraft:").formatted(Formatting.GREEN), textX, textY, 0xFF55FF55, false);
                int prefixW = textRenderer.getWidth("minecraft:");
                context.drawText(textRenderer, Text.literal(suffix), textX + prefixW, textY, isSelected ? 0xFFFFFFFF : 0xFFAAAAAA, false);
            } else {
                context.drawText(textRenderer, Text.literal(key), textX, textY, isSelected ? 0xFFFFFFFF : 0xFF55FF55, false);
            }
        }
        context.disableScissor();

        if (filteredComponents.size() * ITEM_HEIGHT > LIST_HEIGHT) {
            int totalH = filteredComponents.size() * ITEM_HEIGHT;
            int thumbH = Math.max(6, (int)((float)LIST_HEIGHT * LIST_HEIGHT / totalH));
            int thumbY = listY + (int)((float)scrollSmooth * (LIST_HEIGHT - thumbH) / (totalH - LIST_HEIGHT));
            int trackX = popupX + POPUP_WIDTH - 8;
            context.fill(trackX, listY, trackX + 4, listY + LIST_HEIGHT, 0xFF333333);
            context.fill(trackX, thumbY, trackX + 4, thumbY + thumbH, 0xFF8888CC);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }
}
