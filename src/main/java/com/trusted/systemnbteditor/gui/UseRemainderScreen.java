package com.trusted.systemnbteditor.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.component.type.UseRemainderComponent;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.LinkedHashSet;

public class UseRemainderScreen extends GenericContainerScreen {

    private static final int CENTER_SLOT = 13;
    private static final ItemStack RED_PANE;
    static {
        RED_PANE = new ItemStack(Items.RED_STAINED_GLASS_PANE);
        RED_PANE.set(DataComponentTypes.CUSTOM_NAME, Text.literal(" "));
        RED_PANE.set(DataComponentTypes.TOOLTIP_DISPLAY,
                new TooltipDisplayComponent(true, new LinkedHashSet<>()));
    }

    private final net.minecraft.client.gui.screen.Screen parent;
    private ItemStack stack;
    private final int slotId;
    private final Inventory containerInventory;
    private final java.util.function.Consumer<ItemStack> saveCallback;

    public UseRemainderScreen(net.minecraft.client.gui.screen.Screen parent,
                              ItemStack stack, int slotId,
                              java.util.function.Consumer<ItemStack> saveCallback) {
        super(new ContainerScreenHandler(3,
                      net.minecraft.client.MinecraftClient.getInstance().player.getInventory()),
              net.minecraft.client.MinecraftClient.getInstance().player.getInventory(),
              Text.of("Use Remainder Editor"));
        this.parent = parent;
        this.stack = stack;
        this.slotId = slotId;
        this.saveCallback = saveCallback;
        this.containerInventory = this.handler.getInventory();

        loadRemainder();
        fillPanes();
    }

    private void fillPanes() {
        for (int i = 0; i < 27; i++) {
            if (i != CENTER_SLOT) {
                containerInventory.setStack(i, RED_PANE.copy());
            }
        }
    }

    private void loadRemainder() {
        containerInventory.clear();
        UseRemainderComponent comp = stack.get(DataComponentTypes.USE_REMAINDER);
        if (comp != null) {
            ItemStack remainder = comp.convertInto();
            if (remainder != null && !remainder.isEmpty()) {
                containerInventory.setStack(CENTER_SLOT, remainder.copy());
            }
        }
    }

    private void save() { save(false); }

    private void save(boolean isClosing) {
        ItemStack newStack = stack.copy();
        ItemStack remainder = containerInventory.getStack(CENTER_SLOT);

        if (remainder.isEmpty()) {
            newStack.remove(DataComponentTypes.USE_REMAINDER);
        } else {
            newStack.set(DataComponentTypes.USE_REMAINDER, new UseRemainderComponent(remainder.copy()));
        }

        this.stack = newStack;
        applyToGame(newStack, isClosing);
    }

    private void applyToGame(ItemStack newStack, boolean isClosing) {
        if (parent instanceof NbtSelectionScreen sel) {
            sel.refresh(newStack);
        } else if (parent instanceof VisualNbtEditorScreen vis) {
            vis.refresh(newStack);
        }

        if (isClosing) {
            if (this.saveCallback != null) {
                this.saveCallback.accept(newStack);
                return;
            }
        } else {
            if (this.saveCallback != null) return;
        }

        int packetSlot = (this.slotId != -1) ? this.slotId
                : this.client.player.getInventory().selectedSlot + 36;
        updateClientInventory(packetSlot, newStack);
        if (this.client.getNetworkHandler() != null) {
            this.client.getNetworkHandler().sendPacket(
                    new CreativeInventoryActionC2SPacket(packetSlot, newStack));
        }
    }

    private void updateClientInventory(int slot, ItemStack newStack) {
        if (this.client == null || this.client.player == null) return;
        if (slot >= 36 && slot <= 44) {
            this.client.player.getInventory().setStack(slot - 36, newStack.copy());
        } else if (slot >= 9 && slot <= 35) {
            this.client.player.getInventory().setStack(slot, newStack.copy());
        } else if (slot >= 5 && slot <= 8) {
            this.client.player.getInventory().setStack(39 - (slot - 5), newStack.copy());
        } else if (slot == 45) {
            this.client.player.getInventory().setStack(40, newStack.copy());
        }
        if (this.client.player.currentScreenHandler != null) {
            for (Slot s : this.client.player.currentScreenHandler.slots) {
                if (s.id == slot) s.setStack(newStack.copy());
            }
        }
    }

    @Override
    protected void onMouseClick(Slot slot, int invSlot, int button, SlotActionType actionType) {
        if (slot != null && slot.inventory == containerInventory && slot.getIndex() != CENTER_SLOT) {
            return;
        }

        if (slot != null && invSlot != -999) {
            this.handler.onSlotClick(slot.id, button, actionType, this.client.player);
        } else {
            this.handler.onSlotClick(invSlot, button, actionType, this.client.player);
        }

        fillPanes();
        save();
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.key();
        int modifiers = input.modifiers();
        com.trusted.systemnbteditor.data.ModConfig config = com.trusted.systemnbteditor.data.ModConfig.getInstance();
        if (config.editorKeybind == null || config.editorKeybind.isEmpty()) return super.keyPressed(input);
        boolean allDown = true;
        for (int k : config.editorKeybind) {
            if (keyCode != k && !net.minecraft.client.util.InputUtil.isKeyPressed(this.client.getWindow(), k)) {
                allDown = false;
                break;
            }
        }
        if (allDown) {
            Slot hovered = this.focusedSlot;
            if (hovered != null && hovered.hasStack() && hovered.inventory == containerInventory) {
                if (hovered.getIndex() != CENTER_SLOT) return true;
                ItemStack itemToEdit = hovered.getStack();
                int index = hovered.getIndex();
                this.client.setScreen(new NbtSelectionScreen(itemToEdit, -1, updatedStack -> {
                    containerInventory.setStack(index, updatedStack);
                    fillPanes();
                    save();
                    this.client.setScreen(this);
                }));
                return true;
            }
        }

        if (this.client.options.inventoryKey.matchesKey(input)) {
            this.close();
            return true;
        }

        return super.keyPressed(input);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
        context.fill(0, 0, this.width, this.height, 0x80000000);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    public void close() {
        save(true);
        if (this.client != null) this.client.setScreen(this.parent);
    }
}
