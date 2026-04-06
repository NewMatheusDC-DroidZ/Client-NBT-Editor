package com.trusted.systemnbteditor.gui;

import com.trusted.systemnbteditor.util.ComponentIconUtil;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DyedColorPickerScreen extends Screen {
    private final Screen parent;
    private final ColorSelectionCallback callback;
    
    private int currentColor = 0xFFFFFF;
    private final Set<DyeEntry> selectedDyes = new HashSet<>();
    private TextFieldWidget hexField;

    private static final int POPUP_WIDTH = 320;
    private static final int POPUP_HEIGHT = 320;
    private static final int ITEM_HEIGHT = 20;
    private static final int LIST_HEIGHT = 160;
    private static final float LERP = 0.12f;

    private final List<DyeEntry> dyes = new ArrayList<>();
    private float scrollTarget = 0;
    private float scrollSmooth = 0;

    public interface ColorSelectionCallback {
        void onColorSelected(int color);
    }

    private record DyeEntry(String id, String name, int color, ItemStack icon) {}

    public DyedColorPickerScreen(Screen parent, int initialColor, ColorSelectionCallback callback) {
        super(Text.of("Select Dyed Color"));
        this.parent = parent;
        this.callback = callback;
        this.currentColor = initialColor;

        initDyes();
    }

    private void initDyes() {
        addDye("white", "White", 0xF9FFFE, Items.WHITE_DYE);
        addDye("orange", "Orange", 0xF9801D, Items.ORANGE_DYE);
        addDye("magenta", "Magenta", 0xC74EBD, Items.MAGENTA_DYE);
        addDye("light_blue", "Light Blue", 0x3AB3DA, Items.LIGHT_BLUE_DYE);
        addDye("yellow", "Yellow", 0xFED83D, Items.YELLOW_DYE);
        addDye("lime", "Lime", 0x80C71F, Items.LIME_DYE);
        addDye("pink", "Pink", 0xF38BAA, Items.PINK_DYE);
        addDye("gray", "Gray", 0x474F52, Items.GRAY_DYE);
        addDye("light_gray", "Light Gray", 0x9D9D97, Items.LIGHT_GRAY_DYE);
        addDye("cyan", "Cyan", 0x169C9C, Items.CYAN_DYE);
        addDye("purple", "Purple", 0x8932B8, Items.PURPLE_DYE);
        addDye("blue", "Blue", 0x3C44AA, Items.BLUE_DYE);
        addDye("brown", "Brown", 0x835432, Items.BROWN_DYE);
        addDye("green", "Green", 0x5E7C16, Items.GREEN_DYE);
        addDye("red", "Red", 0xB02E26, Items.RED_DYE);
        addDye("black", "Black", 0x1D1D21, Items.BLACK_DYE);
    }

    private void addDye(String id, String name, int color, net.minecraft.item.Item item) {
        dyes.add(new DyeEntry(id, name, color, item.getDefaultStack()));
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int popupX = centerX - POPUP_WIDTH / 2;
        int popupY = centerY - POPUP_HEIGHT / 2;

        this.hexField = new TextFieldWidget(this.textRenderer, popupX + 20, popupY + POPUP_HEIGHT - 65, POPUP_WIDTH - 40, 20, Text.of("Hex Code"));
        this.hexField.setPlaceholder(Text.literal("Insert Hex Code").formatted(Formatting.DARK_GRAY));
        this.hexField.setChangedListener(this::onHexChanged);
        this.hexField.setMaxLength(7);
        this.addDrawableChild(this.hexField);

        this.addDrawableChild(ButtonWidget.builder(Text.of("Done"), button -> {
            callback.onColorSelected(currentColor);
            this.close();
        }).dimensions(popupX + 40, popupY + POPUP_HEIGHT - 30, 100, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.of("Cancel"), button -> this.close())
                .dimensions(popupX + POPUP_WIDTH - 140, popupY + POPUP_HEIGHT - 30, 100, 20).build());
    }

    private void onHexChanged(String hex) {
        if (hex.isEmpty()) return;
        try {
            String clean = hex.startsWith("#") ? hex.substring(1) : hex;
            if (clean.length() == 6) {
                this.currentColor = Integer.parseInt(clean, 16);
                this.selectedDyes.clear(); // Hex overrides dye selection visual
            }
        } catch (Exception e) {}
    }

    private void updateBlendedColor() {
        if (selectedDyes.isEmpty()) return;
        
        int r = 0, g = 0, b = 0;
        int max = 0;
        
        for (DyeEntry dye : selectedDyes) {
            int dr = (dye.color >> 16) & 0xFF;
            int dg = (dye.color >> 8) & 0xFF;
            int db = dye.color & 0xFF;
            max += Math.max(dr, Math.max(dg, db));
            r += dr;
            g += dg;
            b += db;
        }

        r /= selectedDyes.size();
        g /= selectedDyes.size();
        b /= selectedDyes.size();
        
        float average = (float)max / (float)selectedDyes.size();
        float currentAverage = (float)Math.max(r, Math.max(g, b));
        float multiplier = average / currentAverage;
        
        r = (int)((float)r * multiplier);
        g = (int)((float)g * multiplier);
        b = (int)((float)b * multiplier);
        
        this.currentColor = (Math.min(255, r) << 16) | (Math.min(255, g) << 8) | Math.min(255, b);
        
        // Prevent recursive listener firing that clears selection
        String hexStr = String.format("%06X", currentColor);
        if (!hexField.getText().equalsIgnoreCase(hexStr) && !hexField.getText().equalsIgnoreCase("#" + hexStr)) {
            hexField.setText(hexStr);
        }
    }

    @Override
    public void tick() {
        super.tick();
        int maxScroll = Math.max(0, (dyes.size() * ITEM_HEIGHT) - LIST_HEIGHT);
        if (scrollTarget < 0) scrollTarget = 0;
        if (scrollTarget > maxScroll) scrollTarget = maxScroll;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollTarget -= (float) (verticalAmount * ITEM_HEIGHT * 1.5f);
        return true;
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

            if (mouseX >= popupX + 20 && mouseX < popupX + POPUP_WIDTH - 20 && mouseY >= listY && mouseY < listY + LIST_HEIGHT) {
                int index = (int) ((mouseY - listY + Math.round(scrollSmooth)) / ITEM_HEIGHT);
                if (index >= 0 && index < dyes.size()) {
                    DyeEntry dye = dyes.get(index);
                    if (selectedDyes.contains(dye)) {
                        selectedDyes.remove(dye);
                    } else {
                        selectedDyes.add(dye);
                    }
                    updateBlendedColor();
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

        context.drawCenteredTextWithShadow(textRenderer, "Dyed Color Selection", centerX, popupY + 10, 0xFFFFFFFF);

        // Preview Color Box
        context.fill(popupX + POPUP_WIDTH - 40, popupY + 10, popupX + POPUP_WIDTH - 10, popupY + 25, 0xFF000000 | currentColor);
        context.drawText(textRenderer, "Result:", popupX + POPUP_WIDTH - 85, popupY + 14, 0xFFAAAAAA, false);

        int listY = popupY + 40;
        context.enableScissor(popupX + 15, listY, popupX + POPUP_WIDTH - 15, listY + LIST_HEIGHT);
        
        for (int i = 0; i < dyes.size(); i++) {
            int ry = listY + i * ITEM_HEIGHT - Math.round(scrollSmooth);
            if (ry + ITEM_HEIGHT < listY || ry > listY + LIST_HEIGHT) continue;

            DyeEntry entry = dyes.get(i);
            boolean isSelected = selectedDyes.contains(entry);
            boolean isHovered = mouseX >= popupX + 20 && mouseX < popupX + POPUP_WIDTH - 20 && mouseY >= listY && mouseY < listY + LIST_HEIGHT && mouseY >= ry && mouseY < ry + ITEM_HEIGHT;

            if (isSelected) {
                context.fill(popupX + 20, ry, popupX + POPUP_WIDTH - 20, ry + ITEM_HEIGHT, 0x4000AA00);
            } else if (isHovered) {
                context.fill(popupX + 20, ry, popupX + POPUP_WIDTH - 20, ry + ITEM_HEIGHT, 0x20FFFFFF);
            }

            context.drawItem(entry.icon(), popupX + 25, ry + 2);
            context.drawText(textRenderer, entry.name(), popupX + 50, ry + 6, isSelected ? 0xFFFFFFFF : 0xFFAAAAAA, false);
            
            // Color dot
            context.fill(popupX + POPUP_WIDTH - 35, ry + 6, popupX + POPUP_WIDTH - 25, ry + 16, 0xFF000000 | entry.color());
        }
        context.disableScissor();

        super.render(context, mouseX, mouseY, delta);
    }

    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }
}
