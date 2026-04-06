package com.trusted.systemnbteditor.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EnchantmentNbtEditorScreen extends Screen {
    private final Screen parent;
    private final ItemStack stack;
    private final int slotId;
    private final List<EnchantmentEntry> enchantments = new ArrayList<>();
    private final List<Integer> initialLevels = new ArrayList<>();
    private int initialEnchantmentCount = 0;
    
    private int scrollOffset = 0;
    private static final int VISIBLE_ROWS = 6;
    private static final int ROW_HEIGHT = 30;

    private static class EnchantmentEntry {
        RegistryEntry<Enchantment> enchantment;
        int level;
        TextFieldWidget levelField;

        EnchantmentEntry(RegistryEntry<Enchantment> enchantment, int level) {
            this.enchantment = enchantment;
            this.level = level;
        }
    }

    private final java.util.function.Consumer<ItemStack> saveCallback;

    public EnchantmentNbtEditorScreen(Screen parent, ItemStack stack, int slotId) {
        this(parent, stack, slotId, null);
    }

    public EnchantmentNbtEditorScreen(Screen parent, ItemStack stack, int slotId, java.util.function.Consumer<ItemStack> saveCallback) {
        super(Text.of("Enchantment Editor"));
        this.parent = parent;
        this.stack = stack;
        this.slotId = slotId;
        this.saveCallback = saveCallback;
        loadEnchantments();
    }

    private void loadEnchantments() {
        enchantments.clear();
        ItemEnchantmentsComponent component = stack.get(DataComponentTypes.ENCHANTMENTS);
        if (component != null) {
            for (Map.Entry<RegistryEntry<Enchantment>, Integer> entry : component.getEnchantmentEntries()) {
                enchantments.add(new EnchantmentEntry(entry.getKey(), entry.getValue()));
                initialLevels.add(entry.getValue());
            }
        }
        initialEnchantmentCount = enchantments.size();
    }

    @Override
    protected void init() {
        super.init();

        int startX = 50;
        int startY = 40;
        int width = this.width - 100;

        for (int i = 0; i < VISIBLE_ROWS; i++) {
            int index = i + scrollOffset;
            if (index < enchantments.size()) {
                EnchantmentEntry entry = enchantments.get(index);
                
                int fieldX = 180; // Align closer to the name
                TextFieldWidget field = new TextFieldWidget(this.textRenderer, fieldX, startY + i * ROW_HEIGHT, 50, 20, Text.empty());
                field.setText(String.valueOf(entry.level));
                field.setChangedListener(s -> {
                    try { entry.level = Integer.parseInt(s); } catch (Exception ignored) {}
                });
                addDrawableChild(field);
                entry.levelField = field;

                final int finalIndex = index;
                addDrawableChild(ButtonWidget.builder(Text.literal("X").formatted(Formatting.RED), btn -> {
                    enchantments.remove(finalIndex);
                    clearAndInit();
                }).dimensions(fieldX + 55, startY + i * ROW_HEIGHT, 20, 20).build());
            }
        }

        // Add Enchantment
        addDrawableChild(ButtonWidget.builder(Text.literal("Add Enchantment").formatted(Formatting.GOLD), btn -> openEnchantmentSelector())
                .dimensions(this.width / 2 - 110, this.height - 60, 100, 20).build());

        // Save
        addDrawableChild(ButtonWidget.builder(Text.literal("Save").formatted(Formatting.GREEN), btn -> save())
                .dimensions(this.width / 2 + 10, this.height - 60, 100, 20).build());

        // Back
        addDrawableChild(ButtonWidget.builder(Text.of("Back"), btn -> close())
                .dimensions(this.width / 2 - 50, this.height - 30, 100, 20).build());
    }



    private boolean hasUnsavedChanges() {
        if (enchantments.size() != initialEnchantmentCount) return true;
        for (int i = 0; i < enchantments.size(); i++) {
            if (enchantments.get(i).level != initialLevels.get(i)) return true;
        }
        return false;
    }

    private void openEnchantmentSelector() {
        if (this.client != null) {
            this.client.setScreen(new EnchPicker(this, (ench, level) -> {
                enchantments.add(new EnchantmentEntry(ench, level));
                clearAndInit();
                this.client.setScreen(this);
            }));
        }
    }

    private void save() {
        ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(ItemEnchantmentsComponent.DEFAULT);
        for (EnchantmentEntry entry : enchantments) {
            builder.add(entry.enchantment, entry.level);
        }
        
        ItemStack newStack = stack.copy();
        newStack.set(DataComponentTypes.ENCHANTMENTS, builder.build());
        
        // Apply change to parent if applicable
        if (parent instanceof NbtSelectionScreen sel) {
            sel.refresh(newStack);
        } else if (parent instanceof VisualNbtEditorScreen vis) {
            vis.refresh(newStack);
        }

        if (this.saveCallback != null) {
            this.saveCallback.accept(newStack);
            this.forceClose();
            return; 
        }
        
        if (this.client != null && this.client.player != null && this.client.getNetworkHandler() != null) {
            // If opened from selection hub, save directly to inventory
            int packetSlot;
            if (this.slotId != -1) {
                packetSlot = this.slotId;
            } else {
                packetSlot = this.client.player.getInventory().selectedSlot + 36;
            }
            
            // Update client-side to prevent visual reverts
            updateClientInventory(packetSlot, newStack);
            
            if (this.client.getNetworkHandler() != null) {
                this.client.getNetworkHandler().sendPacket(new net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket(packetSlot, newStack));
            }
            this.client.player.sendMessage(Text.literal("Enchantments Saved!").formatted(net.minecraft.util.Formatting.GREEN), true);
        }
        this.forceClose();
    }

    private void updateClientInventory(int slot, ItemStack newStack) {
        if (this.client == null || this.client.player == null) return;
        
        // 1. Update Player Inventory
        if (slot >= 36 && slot <= 44) { // Hotbar
            this.client.player.getInventory().setStack(slot - 36, newStack.copy());
        } else if (slot >= 9 && slot <= 35) { // Main Inventory
            this.client.player.getInventory().setStack(slot, newStack.copy());
        } else if (slot >= 5 && slot <= 8) { // Armor
            this.client.player.getInventory().setStack(39 - (slot - 5), newStack.copy());
        } else if (slot == 45) { // Offhand
            this.client.player.getInventory().setStack(40, newStack.copy());
        }

        // 2. Update Screen Handler Slots
        if (this.client.player.currentScreenHandler != null) {
            for (net.minecraft.screen.slot.Slot s : this.client.player.currentScreenHandler.slots) {
                if (s.id == slot) {
                    s.setStack(newStack.copy());
                }
            }
        }
    }

    protected void clearAndInit() {
        this.clearChildren();
        this.init();
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
        // Dim world background
        context.fill(0, 0, this.width, this.height, 0x80000000);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 10, 0xFFFFFFFF);

        int startX = 20;
        int startY = 40;
        for (int i = 0; i < VISIBLE_ROWS; i++) {
            int index = i + scrollOffset;
            if (index < enchantments.size()) {
                EnchantmentEntry entry = enchantments.get(index);
                
                // Always numerical: Name + " " + level
                Text name = Text.literal(entry.enchantment.value().description().getString() + " " + entry.level);
                
                context.drawTextWithShadow(this.textRenderer, name, startX, startY + i * ROW_HEIGHT + 6, 0xFFFFFFFF);
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollOffset = Math.max(0, Math.min(enchantments.size() - VISIBLE_ROWS, scrollOffset - (int)verticalAmount));
        clearAndInit();
        return true;
    }

    @Override
    public void close() {
        if (hasUnsavedChanges()) {
            if (this.client != null) {
                this.client.setScreen(new ConfirmExitScreen(this));
            }
        } else {
            forceClose();
        }
    }

    protected void forceClose() {
        if (this.client != null) this.client.setScreen(this.parent);
    }

    private class ConfirmExitScreen extends Screen {
        private final EnchantmentNbtEditorScreen parentEditor;

        protected ConfirmExitScreen(EnchantmentNbtEditorScreen parentEditor) {
            super(Text.of("Unsaved Changes"));
            this.parentEditor = parentEditor;
        }

        @Override
        protected void init() {
            int centerX = this.width / 2;
            int centerY = this.height / 2;

            addDrawableChild(ButtonWidget.builder(Text.literal("Discard Changes").formatted(Formatting.RED), btn -> {
                parentEditor.forceClose();
            }).dimensions(centerX - 105, centerY + 10, 100, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("Save and exit"), btn -> {
                parentEditor.save();
            }).dimensions(centerX + 5, centerY + 10, 100, 20).build());
        }

        @Override
        public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
            super.renderBackground(context, mouseX, mouseY, delta);
            context.fill(0, 0, this.width, this.height, 0x80000000);
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            super.render(context, mouseX, mouseY, delta);
            context.drawCenteredTextWithShadow(this.textRenderer, "Leave? You have unsaved changes.", this.width / 2, this.height / 2 - 10, 0xFFFFFFFF);
        }

        @Override
        public void close() {
            if (this.client != null) this.client.setScreen(this.parentEditor);
        }
    }
}
