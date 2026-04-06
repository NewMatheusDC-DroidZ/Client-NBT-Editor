package com.trusted.systemnbteditor.gui;

import com.trusted.systemnbteditor.clientchest.ClientChestManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ClientChestScreen extends GenericContainerScreen {
    private static int PAGE = 0;

    private final Screen parent;
    private final ClientChestManager manager;

    private TextFieldWidget pageField;
    private ButtonWidget prevPageButton;
    private ButtonWidget nextPageButton;
    private ButtonWidget prevJumpButton;
    private ButtonWidget nextJumpButton;
    private boolean dirty;

    public ClientChestScreen(Screen parent) {
        this(parent, PAGE);
    }

    public ClientChestScreen(Screen parent, int page) {
        super(new ContainerScreenHandler(6,
                        net.minecraft.client.MinecraftClient.getInstance().player.getInventory()),
                net.minecraft.client.MinecraftClient.getInstance().player.getInventory(),
                Text.literal("Client Chest"));
        this.parent = parent;
        this.manager = ClientChestManager.getInstance();
        PAGE = Math.max(0, Math.min(page, manager.getPageCount() - 1));
        this.dirty = false;
        loadCurrentPage();
    }

    @Override
    protected void init() {
        super.init();

        int leftX = this.x - 87;
        int topY = this.y;

        this.pageField = this.addDrawableChild(new TextFieldWidget(this.textRenderer, leftX + 24, topY + 22, 35, 16, Text.literal("Page")));
        this.pageField.setMaxLength(4);
        this.pageField.setText(String.valueOf(PAGE + 1));
        this.pageField.setChangedListener(str -> {
            if (str == null || str.isBlank()) return;
            try {
                int parsed = Integer.parseInt(str.trim()) - 1;
                if (parsed >= 0 && parsed < manager.getPageCount() && parsed != PAGE) {
                    openPage(parsed);
                }
            } catch (NumberFormatException ignored) {
            }
        });

        this.prevPageButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("<"), button -> openPage(PAGE - 1))
                .dimensions(leftX, topY + 20, 20, 20).build());
        this.nextPageButton = this.addDrawableChild(ButtonWidget.builder(Text.literal(">"), button -> openPage(PAGE + 1))
                .dimensions(leftX + 63, topY + 20, 20, 20).build());

        this.prevJumpButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("<<"), button -> {
                    int[] pois = manager.getNearestPois(PAGE);
                    int target = (pois[0] != -1 ? pois[0] : 0);
                    openPage(target);
                })
                .dimensions(leftX, topY + 44, 39, 20).build());
        this.nextJumpButton = this.addDrawableChild(ButtonWidget.builder(Text.literal(">>"), button -> {
                    int[] pois = manager.getNearestPois(PAGE);
                    int target = (pois[1] != -1 ? pois[1] : manager.getPageCount() - 1);
                    openPage(target);
                })
                .dimensions(leftX + 44, topY + 44, 39, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Reload Page"), button -> reloadCurrentPage())
                .dimensions(leftX, topY + 68, 83, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Clear Page").formatted(Formatting.RED), button -> clearCurrentPage())
                .dimensions(leftX, topY + 92, 83, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Open Folder").formatted(Formatting.AQUA), button -> openFolder())
                .dimensions(leftX, topY + 116, 83, 20).build());

        updateNavigationButtons();
    }

    private void loadCurrentPage() {
        ItemStack[] page = manager.loadPage(PAGE);
        for (int i = 0; i < ClientChestManager.PAGE_SIZE; i++) {
            this.handler.getInventory().setStack(i, page[i] == null ? ItemStack.EMPTY : page[i].copy());
        }
    }

    private void saveCurrentPage() {
        ItemStack[] items = new ItemStack[ClientChestManager.PAGE_SIZE];
        for (int i = 0; i < ClientChestManager.PAGE_SIZE; i++) {
            items[i] = this.handler.getInventory().getStack(i).copy();
        }
        manager.savePage(PAGE, items);
    }

    private void reloadCurrentPage() {
        if (dirty) saveCurrentPage();
        loadCurrentPage();
        dirty = false;
    }

    private void clearCurrentPage() {
        for (int i = 0; i < ClientChestManager.PAGE_SIZE; i++) {
            this.handler.getInventory().setStack(i, ItemStack.EMPTY);
        }
        dirty = true;
        saveCurrentPage();
        dirty = false;
    }

    private void openPage(int page) {
        if (page < 0 || page >= manager.getPageCount() || page == PAGE) return;
        if (dirty) saveCurrentPage();
        if (this.client != null) this.client.setScreen(new ClientChestScreen(this.parent, page));
    }

    private void updateNavigationButtons() {
        if (prevPageButton != null) prevPageButton.active = PAGE > 0;
        if (nextPageButton != null) nextPageButton.active = PAGE < manager.getPageCount() - 1;
    }

    private void openFolder() {
        try {
            java.awt.Desktop.getDesktop().open(manager.getFolder());
        } catch (Exception e) {
            if (this.client != null && this.client.player != null) {
                this.client.player.sendMessage(Text.literal("Failed to open folder: " + e.getMessage()).formatted(Formatting.RED), false);
            }
        }
    }

    @Override
    protected void onMouseClick(Slot slot, int invSlot, int button, SlotActionType actionType) {
        if (slot != null && invSlot != -999) {
            this.handler.onSlotClick(slot.id, button, actionType, this.client.player);
        } else {
            this.handler.onSlotClick(invSlot, button, actionType, this.client.player);
        }
        if (slot != null) dirty = true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (this.pageField != null && this.pageField.keyPressed(input)) return true;

        if (this.client.options.inventoryKey.matchesKey(input)) {
            close();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void close() {
        if (dirty) saveCurrentPage();
        if (this.client != null) this.client.setScreen(this.parent);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawTextWithShadow(this.textRenderer,
                Text.literal("Page " + (PAGE + 1) + " / " + manager.getPageCount()),
                this.x - 85, this.y + 142, 0xFFFFFF);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }
}
