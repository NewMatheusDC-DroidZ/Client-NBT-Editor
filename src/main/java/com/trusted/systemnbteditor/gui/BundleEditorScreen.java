package com.trusted.systemnbteditor.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class BundleEditorScreen extends GenericContainerScreen {
    private static int PAGE = 0;
    private static int MAX_PAGES = 20;
    private static boolean autosaveOnExit = true;

    private final Screen parent;
    private final ItemStack originalStack;
    private final int slotId;
    private final Consumer<ItemStack> saveCallback;
    private final List<ItemStack> allItems;
    private boolean dirty = false;
    private boolean discardChanges = false;

    private TextFieldWidget pageField;
    private TextFieldWidget maxPagesField;
    private ButtonWidget autosaveToggleButton;
    private ButtonWidget primaryActionButton;

    public BundleEditorScreen(Screen parent, ItemStack stack, int slotId, Consumer<ItemStack> saveCallback) {
        super(new ContainerScreenHandler(6,
                        net.minecraft.client.MinecraftClient.getInstance().player.getInventory()),
                net.minecraft.client.MinecraftClient.getInstance().player.getInventory(),
                Text.literal("Bundle Editor"));
        this.parent = parent;
        this.originalStack = stack.copy();
        this.slotId = slotId;
        this.saveCallback = saveCallback;
        this.allItems = new ArrayList<>();
        
        loadItemsFromStack(stack);
        loadCurrentPage();
    }

    private void loadItemsFromStack(ItemStack stack) {
        BundleContentsComponent bundle = stack.get(DataComponentTypes.BUNDLE_CONTENTS);
        if (bundle != null) {
            for (ItemStack s : bundle.stream().toList()) {
                allItems.add(s.copy());
            }
        }
    }

    private void loadCurrentPage() {
        Inventory inv = this.handler.getInventory();
        inv.clear();
        int start = PAGE * 54;
        for (int i = 0; i < 54; i++) {
            if (start + i < allItems.size()) {
                inv.setStack(i, allItems.get(start + i).copy());
            }
        }
    }

    private void saveCurrentPage() {
        Inventory inv = this.handler.getInventory();
        int start = PAGE * 54;
        // Ensure allItems is large enough
        while (allItems.size() < start + 54) {
            allItems.add(ItemStack.EMPTY);
        }
        for (int i = 0; i < 54; i++) {
            allItems.set(start + i, inv.getStack(i).copy());
        }
    }

    @Override
    protected void init() {
        super.init();

        int leftX = this.x - 100;
        int topY = this.y;

        // Page Switcher
        this.pageField = this.addDrawableChild(new TextFieldWidget(this.textRenderer, leftX + 24, topY + 22, 35, 16, Text.literal("Page")));
        this.pageField.setMaxLength(4);
        this.pageField.setText(String.valueOf(PAGE + 1));
        this.pageField.setChangedListener(str -> {
            if (str == null || str.isBlank()) return;
            try {
                int parsed = Integer.parseInt(str.trim()) - 1;
                if (parsed >= 0 && parsed < MAX_PAGES && parsed != PAGE) {
                    openPage(parsed);
                }
            } catch (NumberFormatException ignored) {}
        });

        this.addDrawableChild(ButtonWidget.builder(Text.literal("<"), button -> openPage(PAGE - 1))
                .dimensions(leftX, topY + 20, 20, 20).build()).active = PAGE > 0;
        this.addDrawableChild(ButtonWidget.builder(Text.literal(">"), button -> openPage(PAGE + 1))
                .dimensions(leftX + 63, topY + 20, 20, 20).build()).active = PAGE < MAX_PAGES - 1;

        // Jump buttons
        this.addDrawableChild(ButtonWidget.builder(Text.literal("<<"), button -> openPage(0))
                .dimensions(leftX, topY + 44, 39, 20).build()).active = PAGE > 0;
        this.addDrawableChild(ButtonWidget.builder(Text.literal(">>"), button -> openPage(MAX_PAGES - 1))
                .dimensions(leftX + 44, topY + 44, 39, 20).build()).active = PAGE < MAX_PAGES - 1;

        // Max Pages Box
        this.addDrawableChild(new TextFieldWidget(this.textRenderer, leftX, topY + 80, 83, 16, Text.of("Max Pages Label")))
                .setPlaceholder(Text.of("Max Pages")); // This is just for positioning/visual? No, user wants text.
        
        this.maxPagesField = this.addDrawableChild(new TextFieldWidget(this.textRenderer, leftX + 45, topY + 100, 35, 16, Text.literal("Max Pages")));
        this.maxPagesField.setText(String.valueOf(MAX_PAGES));
        this.maxPagesField.setChangedListener(str -> {
            if (str == null || str.isBlank()) return;
            try {
                int parsed = Integer.parseInt(str.trim());
                if (parsed >= 1 && parsed <= 100) { // Safety limit 100
                    MAX_PAGES = parsed;
                    if (PAGE >= MAX_PAGES) openPage(MAX_PAGES - 1);
                }
            } catch (NumberFormatException ignored) {}
        });

        // Saving Buttons
        autosaveToggleButton = this.addDrawableChild(ButtonWidget.builder(getAutosaveLabel(), button -> {
            autosaveOnExit = !autosaveOnExit;
            button.setMessage(getAutosaveLabel());
            rebuildPrimaryActionButton();
        }).dimensions(this.width - 170, this.height - 48, 160, 20).build());

        rebuildPrimaryActionButton();
    }

    private void openPage(int page) {
        if (page < 0 || page >= MAX_PAGES || page == PAGE) return;
        saveCurrentPage();
        PAGE = page;
        loadCurrentPage();
        if (this.pageField != null) this.pageField.setText(String.valueOf(PAGE + 1));
        // We don't recreate the screen, just refresh slots/widgets if needed.
        // Actually, some widgets (active state) need update.
        this.init();
    }

    private Text getAutosaveLabel() {
        return Text.literal("Autosave: " + (autosaveOnExit ? "ON" : "OFF"))
                .formatted(autosaveOnExit ? Formatting.GREEN : Formatting.DARK_RED);
    }

    private void rebuildPrimaryActionButton() {
        if (primaryActionButton != null) this.remove(primaryActionButton);
        if (autosaveOnExit) {
            primaryActionButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("Exit without Saving"), button -> exitWithoutSaving())
                    .dimensions(this.width - 170, this.height - 26, 160, 20).build());
        } else {
            primaryActionButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("Save").formatted(Formatting.GREEN), button -> saveAndClose())
                    .dimensions(this.width - 170, this.height - 26, 160, 20).build());
        }
    }

    private void saveAndClose() {
        save(true);
        if (this.client != null) this.client.setScreen(this.parent);
    }

    private void exitWithoutSaving() {
        this.discardChanges = true;
        if (this.parent instanceof net.minecraft.client.gui.screen.Screen) {
             // In NbtSelectionScreen, it doesn't need explicit rollback because we haven't touched originalStack
             // except via saveCallback which we won't call.
        }
        if (this.client != null) this.client.setScreen(this.parent);
    }

    private void save(boolean isClosing) {
        saveCurrentPage();
        ItemStack newStack = originalStack.copy();
        List<ItemStack> filtered = allItems.stream().filter(s -> !s.isEmpty()).collect(Collectors.toList());
        newStack.set(DataComponentTypes.BUNDLE_CONTENTS, new BundleContentsComponent(filtered));

        if (this.parent instanceof NbtSelectionScreen sel) sel.refresh(newStack);
        else if (this.parent instanceof VisualNbtEditorScreen vis) vis.refresh(newStack);

        if (isClosing && this.saveCallback != null) {
            this.saveCallback.accept(newStack);
        } else if (this.saveCallback == null) {
            applyToGame(newStack);
        }
    }

    private void applyToGame(ItemStack stack) {
        if (this.client == null || this.client.player == null) return;
        int packetSlot = (this.slotId != -1) ? this.slotId : (36 + this.client.player.getInventory().selectedSlot);
        
        System.out.println("[BundleEditor] Applying stack to game. Slot ID: " + packetSlot + " (Hand Slot: " + this.client.player.getInventory().selectedSlot + ")");
        System.out.println("[BundleEditor] Component Size: " + (stack.get(DataComponentTypes.BUNDLE_CONTENTS) != null ? stack.get(DataComponentTypes.BUNDLE_CONTENTS).size() : 0));
        
        // --- Real-time update fix ---
        // Update LOCAL inventory too so the changes appear immediately.
        net.minecraft.entity.player.PlayerInventory inv = this.client.player.getInventory();
        if (packetSlot >= 36 && packetSlot <= 44) {
            // Hotbar Slots 0-8
            inv.setStack(packetSlot - 36, stack);
        } else if (packetSlot == 45) {
            // Offhand
            inv.setStack(40, stack);
        } else if (packetSlot >= 9 && packetSlot <= 35) {
            // Main Inventory Slots 9-35
            inv.setStack(packetSlot, stack);
        } else if (packetSlot >= 5 && packetSlot <= 8) {
            // Armor Slots 0-3 (indexed 36-39 in PlayerInventory)
            inv.setStack(39 - (packetSlot - 5), stack);
        }
        // ---------------------------

        if (this.client.getNetworkHandler() != null) {
            this.client.getNetworkHandler().sendPacket(new CreativeInventoryActionC2SPacket(packetSlot, stack));
            System.out.println("[BundleEditor] CreativeInventoryActionC2SPacket sent.");
        } else {
            System.err.println("[BundleEditor] ERROR: NetworkHandler is null!");
        }
    }

    @Override
    protected void onMouseClick(Slot slot, int invSlot, int button, SlotActionType actionType) {
        if (slot != null && invSlot != -999) {
            this.handler.onSlotClick(slot.id, button, actionType, this.client.player);
        } else {
            this.handler.onSlotClick(invSlot, button, actionType, this.client.player);
        }
        dirty = true;
    }

    @Override
    public void close() {
        if (autosaveOnExit && !discardChanges) save(true);
        if (this.client != null) this.client.setScreen(this.parent);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if ((pageField != null && pageField.keyPressed(input)) || (maxPagesField != null && maxPagesField.keyPressed(input))) return true;
        if (this.client.options.inventoryKey.matchesKey(input)) {
            close();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawTextWithShadow(this.textRenderer, Text.literal("Page " + (PAGE + 1) + " / " + MAX_PAGES), this.x - 90, this.y + 70, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, Text.literal("Max Pages"), this.x - 115, this.y + 104, 0xFFAAAA00);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }
}
