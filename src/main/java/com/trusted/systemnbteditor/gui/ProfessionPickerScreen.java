package com.trusted.systemnbteditor.gui;

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
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ProfessionPickerScreen extends Screen {
    private static final int POPUP_WIDTH = 320;
    private static final int POPUP_HEIGHT = 240;
    private static final int ITEM_HEIGHT = 16;
    private static final int VISIBLE_ITEMS = 10;
    private static final int LIST_HEIGHT = VISIBLE_ITEMS * ITEM_HEIGHT;
    private static final float LERP = 0.12f;

    private final Screen parent;
    private final Consumer<String> callback;

    private final List<ProfessionEntry> allProfessions = new ArrayList<>();
    private List<ProfessionEntry> filteredProfessions = new ArrayList<>();
    private ProfessionEntry selectedProfession;

    private TextFieldWidget searchField;
    private float scrollTarget;
    private float scrollSmooth;
    private int scrollOffset;
    private int draggingSlider = 0;

    private static class ProfessionEntry {
        final String id;
        final String label;
        final ItemStack icon;

        ProfessionEntry(String id, String label, ItemStack icon) {
            this.id = id;
            this.label = label;
            this.icon = icon;
        }
    }

    public ProfessionPickerScreen(Screen parent, String currentProfession, Consumer<String> callback) {
        super(Text.literal("Pick Profession"));
        this.parent = parent;
        this.callback = callback;
        fillProfessions();
        this.filteredProfessions = new ArrayList<>(this.allProfessions);
        this.selectedProfession = this.allProfessions.stream()
            .filter(p -> p.id.equals(currentProfession))
            .findFirst()
            .orElse(this.allProfessions.get(0));
    }

    private void fillProfessions() {
        this.allProfessions.clear();
        this.allProfessions.add(new ProfessionEntry("minecraft:none", "None", new ItemStack(Items.EMERALD)));
        this.allProfessions.add(new ProfessionEntry("minecraft:armorer", "Armorer", new ItemStack(Items.BLAST_FURNACE)));
        this.allProfessions.add(new ProfessionEntry("minecraft:butcher", "Butcher", new ItemStack(Items.SMOKER)));
        this.allProfessions.add(new ProfessionEntry("minecraft:cartographer", "Cartographer", new ItemStack(Items.CARTOGRAPHY_TABLE)));
        this.allProfessions.add(new ProfessionEntry("minecraft:cleric", "Cleric", new ItemStack(Items.BREWING_STAND)));
        this.allProfessions.add(new ProfessionEntry("minecraft:farmer", "Farmer", new ItemStack(Items.COMPOSTER)));
        this.allProfessions.add(new ProfessionEntry("minecraft:fisherman", "Fisherman", new ItemStack(Items.BARREL)));
        this.allProfessions.add(new ProfessionEntry("minecraft:fletcher", "Fletcher", new ItemStack(Items.FLETCHING_TABLE)));
        this.allProfessions.add(new ProfessionEntry("minecraft:leatherworker", "Leatherworker", new ItemStack(Items.CAULDRON)));
        this.allProfessions.add(new ProfessionEntry("minecraft:librarian", "Librarian", new ItemStack(Items.LECTERN)));
        this.allProfessions.add(new ProfessionEntry("minecraft:mason", "Mason", new ItemStack(Items.STONECUTTER)));
        this.allProfessions.add(new ProfessionEntry("minecraft:shepherd", "Shepherd", new ItemStack(Items.LOOM)));
        this.allProfessions.add(new ProfessionEntry("minecraft:toolsmith", "Toolsmith", new ItemStack(Items.SMITHING_TABLE)));
        this.allProfessions.add(new ProfessionEntry("minecraft:weaponsmith", "Weaponsmith", new ItemStack(Items.GRINDSTONE)));
        this.allProfessions.add(new ProfessionEntry("minecraft:nitwit", "Nitwit", new ItemStack(Items.BROWN_WOOL)));
    }

    @Override
    protected void init() {
        super.init();
        int popupX = this.width / 2 - POPUP_WIDTH / 2;
        int popupY = this.height / 2 - POPUP_HEIGHT / 2;

        this.searchField = new TextFieldWidget(this.textRenderer, popupX + 10, popupY + 25, POPUP_WIDTH - 20, 20, Text.literal("Search"));
        this.searchField.setPlaceholder(Text.literal("Type to search...").formatted(Formatting.GRAY));
        this.searchField.setChangedListener(this::onSearchChanged);
        this.addDrawableChild(this.searchField);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Apply"), button -> {
            if (selectedProfession != null) {
                this.callback.accept(selectedProfession.id);
            }
            close();
        }).dimensions(popupX + POPUP_WIDTH - 80, popupY + POPUP_HEIGHT - 30, 70, 20).build());

        this.setInitialFocus(this.searchField);
    }

    private void onSearchChanged(String query) {
        String q = query.toLowerCase(Locale.ROOT);
        this.filteredProfessions = allProfessions.stream()
            .filter(p -> p.id.toLowerCase(Locale.ROOT).contains(q) || p.label.toLowerCase(Locale.ROOT).contains(q))
            .collect(Collectors.toList());
        this.scrollTarget = 0;
        this.scrollSmooth = 0;
        this.scrollOffset = 0;
        if (selectedProfession != null && !filteredProfessions.contains(selectedProfession)) {
            selectedProfession = null;
        }
    }

    @Override
    public void tick() {
        super.tick();
        int totalH = filteredProfessions.size() * ITEM_HEIGHT;
        int maxScroll = Math.max(0, totalH - LIST_HEIGHT);
        if (scrollTarget < 0) scrollTarget = 0;
        if (scrollTarget > maxScroll) scrollTarget = maxScroll;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int totalH = filteredProfessions.size() * ITEM_HEIGHT;
        if (totalH > LIST_HEIGHT) {
            scrollTarget -= (float) (verticalAmount * ITEM_HEIGHT * 2);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        double mouseX = click.x();
        double mouseY = click.y();
        int popupX = this.width / 2 - POPUP_WIDTH / 2;
        int popupY = this.height / 2 - POPUP_HEIGHT / 2;
        int listY = popupY + 50;

        if (click.button() == 0) {
            if (filteredProfessions.size() * ITEM_HEIGHT > LIST_HEIGHT) {
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
                if (dataIndex >= 0 && dataIndex < filteredProfessions.size()) {
                    selectedProfession = filteredProfessions.get(dataIndex);
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
        int popupY = this.height / 2 - POPUP_HEIGHT / 2;
        int listY = popupY + 50;
        float rel = (float) (mouseY - listY) / LIST_HEIGHT;
        int totalH = filteredProfessions.size() * ITEM_HEIGHT;
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
        if (Math.abs(scrollSmooth - scrollTarget) < 0.1f) scrollSmooth = scrollTarget;
        scrollOffset = Math.round(scrollSmooth);

        int popupX = this.width / 2 - POPUP_WIDTH / 2;
        int popupY = this.height / 2 - POPUP_HEIGHT / 2;
        int listY = popupY + 50;

        context.fill(popupX, popupY, popupX + POPUP_WIDTH, popupY + POPUP_HEIGHT, 0xFF050510);
        int borderColor = 0xFF4040A0;
        context.fill(popupX, popupY, popupX + POPUP_WIDTH, popupY + 1, borderColor);
        context.fill(popupX, popupY + POPUP_HEIGHT - 1, popupX + POPUP_WIDTH, popupY + POPUP_HEIGHT, borderColor);
        context.fill(popupX, popupY, popupX + 1, popupY + POPUP_HEIGHT, borderColor);
        context.fill(popupX + POPUP_WIDTH - 1, popupY, popupX + POPUP_WIDTH, popupY + POPUP_HEIGHT, borderColor);

        context.drawText(this.textRenderer, "Pick Profession", popupX + 10, popupY + 10, 0xFFFFFFFF, false);

        context.enableScissor(popupX + 8, listY, popupX + POPUP_WIDTH - 8, listY + LIST_HEIGHT);
        for (int i = 0; i < filteredProfessions.size(); i++) {
            int ry = listY + i * ITEM_HEIGHT - scrollOffset;
            if (ry + ITEM_HEIGHT < listY || ry > listY + LIST_HEIGHT) continue;

            ProfessionEntry p = filteredProfessions.get(i);
            boolean hovered = mouseX >= popupX + 8 && mouseX < popupX + POPUP_WIDTH - 8 && mouseY >= listY && mouseY < listY + LIST_HEIGHT && mouseY >= ry && mouseY < ry + ITEM_HEIGHT;
            boolean selected = p == selectedProfession;

            if (selected) {
                context.fill(popupX + 8, ry, popupX + POPUP_WIDTH - 16, ry + ITEM_HEIGHT, 0x60FFFFFF);
            } else if (hovered) {
                context.fill(popupX + 8, ry, popupX + POPUP_WIDTH - 16, ry + ITEM_HEIGHT, 0x20FFFFFF);
            }

            context.drawText(this.textRenderer, p.label, popupX + 15, ry + 4, selected ? 0xFFFFFFFF : 0xFF55FFFF, false);
            context.drawItem(p.icon, popupX + POPUP_WIDTH - 30, ry);
        }
        context.disableScissor();

        if (filteredProfessions.size() * ITEM_HEIGHT > LIST_HEIGHT) {
            int totalH = filteredProfessions.size() * ITEM_HEIGHT;
            int thumbH = Math.max(6, (int) ((float) LIST_HEIGHT * LIST_HEIGHT / totalH));
            int thumbY = listY + (int) (scrollSmooth * (LIST_HEIGHT - thumbH) / (totalH - LIST_HEIGHT));
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
