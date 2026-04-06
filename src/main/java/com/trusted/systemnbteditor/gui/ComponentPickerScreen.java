package com.trusted.systemnbteditor.gui;

import com.trusted.systemnbteditor.data.ComponentRegistry;
import com.trusted.systemnbteditor.data.ComponentRegistry.ComponentInfo;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import com.trusted.systemnbteditor.util.ComponentIconUtil;
import java.util.*;
import java.util.stream.Collectors;

public class ComponentPickerScreen extends Screen {
    private final Screen parent;
    private final ComponentSelectionCallback callback;
    private float scrollTarget = 0;
    private float scrollSmooth = 0;
    private int scrollOffset = 0;
    private ComponentInfo selectedComponent = null;
    private static final float LERP = 0.12f;
    
    private static final int POPUP_WIDTH = 320;
    private static final int POPUP_HEIGHT = 240;
    private static final int ITEM_HEIGHT = 20;
    private static final int VISIBLE_ITEMS = 8;
    private static final int LIST_HEIGHT = VISIBLE_ITEMS * ITEM_HEIGHT;

    private TextFieldWidget searchField;
    private List<ComponentInfo> filteredComponents = new ArrayList<>();
    
    public interface ComponentSelectionCallback {
        void onComponentSelected(ComponentInfo component);
    }

    public ComponentPickerScreen(Screen parent, ComponentSelectionCallback callback) {
        super(Text.of("Select Component"));
        this.parent = parent;
        this.callback = callback;
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
            if (selectedComponent != null) {
                callback.onComponentSelected(selectedComponent);
            }
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
        this.selectedComponent = null;
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
                    selectedComponent = filteredComponents.get(dataIndex);
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

        context.drawText(this.textRenderer, "Add Component", popupX + 10, popupY + 10, 0xFFFFFFFF, false);

        // List Rendering with Scissoring
        int listY = popupY + 50;
        context.enableScissor(popupX + 8, listY, popupX + POPUP_WIDTH - 8, listY + LIST_HEIGHT);
        
        for (int i = 0; i < filteredComponents.size(); i++) {
            int ry = listY + i * ITEM_HEIGHT - scrollOffset;
            if (ry + ITEM_HEIGHT < listY || ry > listY + LIST_HEIGHT) continue;

            ComponentInfo component = filteredComponents.get(i);
            boolean isSelected = component == selectedComponent;
            boolean isHovered = mouseX >= popupX + 8 && mouseX < popupX + POPUP_WIDTH - 8 && mouseY >= listY && mouseY < listY + LIST_HEIGHT && mouseY >= ry && mouseY < ry + ITEM_HEIGHT;
            
            boolean isUsed = false;
            boolean isHidden = false;
            if (parent instanceof VisualNbtEditorScreen editor) {
                isUsed = editor.isComponentPresent(component.key());
                isHidden = editor.isComponentHidden(component.key());
            }

            if (isSelected) {
                context.fill(popupX + 8, ry, popupX + POPUP_WIDTH - 16, ry + ITEM_HEIGHT, 0x60FFFFFF);
            } else if (isHovered) {
                context.fill(popupX + 8, ry, popupX + POPUP_WIDTH - 16, ry + ITEM_HEIGHT, 0x20FFFFFF);
            }

            if (isUsed) {
                context.fill(popupX + 8, ry, popupX + 12, ry + ITEM_HEIGHT, 0xFFCC0000);
            } else if (isHidden) {
                context.fill(popupX + 8, ry, popupX + 12, ry + ITEM_HEIGHT, 0xFF00AA00);
            }

            ItemStack icon = ComponentIconUtil.getIcon(component.key());
            context.drawItem(icon, popupX + 14, ry + 2);
            
            String key = component.key();
            if (key.startsWith("minecraft:")) {
                String suffix = key.substring("minecraft:".length());
                int textX = popupX + 36;
                int textY = ry + 6;
                if (isSelected) {
                    context.drawText(textRenderer, Text.literal("minecraft:").formatted(net.minecraft.util.Formatting.GREEN), textX, textY, 0xFF55FF55, false);
                    int prefixW = textRenderer.getWidth("minecraft:");
                    context.drawText(textRenderer, Text.literal(suffix), textX + prefixW, textY, 0xFFFFFFFF, false);
                } else {
                    context.drawText(textRenderer, Text.literal("minecraft:").formatted(net.minecraft.util.Formatting.GREEN), textX, textY, 0xFF55FF55, false);
                    int prefixW = textRenderer.getWidth("minecraft:");
                    context.drawText(textRenderer, Text.literal(suffix), textX + prefixW, textY, 0xFFAAAAAA, false);
                }
            } else {
                context.drawText(textRenderer, Text.literal(key), popupX + 36, ry + 6, isSelected ? 0xFFFFFFFF : 0xFF55FF55, false);
            }
        }
        context.disableScissor();

        // Scrollbar logic
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

    public boolean shouldPause() {
        return false;
    }
}
