package com.trusted.systemnbteditor.gui;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;

public class ContainerScreenHandler extends GenericContainerScreenHandler {
    
    public static final int SYNC_ID = 42;
    private final int rowCount;

    public ContainerScreenHandler(int rows, PlayerInventory playerInventory) {
        this(rows, playerInventory, new SimpleInventory(rows * 9));
    }

    public ContainerScreenHandler(int rows, PlayerInventory playerInventory, Inventory inventory) {
        super(getHandlerType(rows), SYNC_ID, playerInventory, inventory, rows);
        this.rowCount = rows;
        this.slots.replaceAll(ContainerScreenHandlerSlot::new);
    }

    private static ScreenHandlerType<GenericContainerScreenHandler> getHandlerType(int rows) {
        return switch (rows) {
            case 1 -> ScreenHandlerType.GENERIC_9X1;
            case 2 -> ScreenHandlerType.GENERIC_9X2;
            case 3 -> ScreenHandlerType.GENERIC_9X3;
            case 4 -> ScreenHandlerType.GENERIC_9X4;
            case 5 -> ScreenHandlerType.GENERIC_9X5;
            case 6 -> ScreenHandlerType.GENERIC_9X6;
            default -> throw new IllegalArgumentException("Invalid row count: " + rows);
        };
    }

    @Override
    public boolean canUse(net.minecraft.entity.player.PlayerEntity player) {
        return true;
    }

    @Override
    public void onSlotClick(int slotIndex, int button, net.minecraft.screen.slot.SlotActionType actionType, net.minecraft.entity.player.PlayerEntity player) {
        java.util.List<ItemStack> before = new java.util.ArrayList<>();
        for (int i = 0; i < player.getInventory().size(); i++) {
            before.add(player.getInventory().getStack(i).copy());
        }

        if (slotIndex >= 0 && slotIndex < this.slots.size()) {
            Slot slot = this.slots.get(slotIndex);
            ItemStack cursorStack = this.getCursorStack();

            if (actionType == net.minecraft.screen.slot.SlotActionType.PICKUP) {
                if (cursorStack.isEmpty()) {
                    if (slot.hasStack()) {
                        int amount = button == 0 ? slot.getStack().getCount() : (slot.getStack().getCount() + 1) / 2;
                        this.setCursorStack(slot.takeStack(amount));
                    }
                } else {
                    if (slot.hasStack()) {
                        if (ItemStack.areItemsAndComponentsEqual(cursorStack, slot.getStack())) {
                            int space = slot.getStack().getMaxCount() - slot.getStack().getCount();
                            int toAdd = Math.min(space, button == 0 ? cursorStack.getCount() : 1);
                            slot.getStack().increment(toAdd);
                            cursorStack.decrement(toAdd);
                        } else {
                            ItemStack temp = slot.getStack().copy();
                            slot.setStack(cursorStack.copy());
                            this.setCursorStack(temp);
                        }
                    } else {
                        int toAdd = button == 0 ? cursorStack.getCount() : 1;
                        slot.setStack(cursorStack.split(toAdd));
                    }
                }
            } else if (actionType == net.minecraft.screen.slot.SlotActionType.QUICK_MOVE) {
                this.quickMove(player, slotIndex);
            } else if (actionType == net.minecraft.screen.slot.SlotActionType.SWAP) {
                if (button >= 0 && button < 9) {
                    ItemStack hotbarStack = player.getInventory().getStack(button);
                    ItemStack slotStack = slot.getStack();
                    player.getInventory().setStack(button, slotStack.copy());
                    slot.setStack(hotbarStack.copy());
                }
            } else if (actionType == net.minecraft.screen.slot.SlotActionType.CLONE) {
                if (player.getAbilities().creativeMode && cursorStack.isEmpty() && slot.hasStack()) {
                    ItemStack clone = slot.getStack().copy();
                    clone.setCount(clone.getMaxCount());
                    this.setCursorStack(clone);
                }
            } else if (actionType == net.minecraft.screen.slot.SlotActionType.THROW) {
                if (cursorStack.isEmpty() && slot.hasStack()) {
                    slot.takeStack(button == 0 ? 1 : slot.getStack().getCount());
                }
            } else if (actionType == net.minecraft.screen.slot.SlotActionType.QUICK_CRAFT) {
                // Dragging logic
                int stage = button & 3;
                int dragType = button >> 2 & 3; // 0=left, 1=right, 2=middle
                if (stage == 0) {
                    // Start dragging - handled by client widgets usually
                } else if (stage == 1) {
                    // Add slot to drag
                    ItemStack cursor = this.getCursorStack();
                    if (!cursor.isEmpty()) {
                        if (dragType == 2 && player.getAbilities().creativeMode) {
                            // Middle-click: Fill with full stack
                            ItemStack clone = cursor.copy();
                            clone.setCount(clone.getMaxCount());
                            slot.setStack(clone);
                        } else if (dragType == 0) {
                            // Left-click drag: Standard distribution (handled by stage 2 usually, but we implement basic drop here)
                            // For simplicity in this client-only inventory, we just set the stack if empty
                            if (slot.getStack().isEmpty()) {
                                slot.setStack(cursor.copy());
                            }
                        } else if (dragType == 1) {
                            // Right-click drag: 1 and 1
                            if (slot.getStack().isEmpty() || ItemStack.areItemsAndComponentsEqual(slot.getStack(), cursor)) {
                                if (slot.getStack().isEmpty()) {
                                    ItemStack single = cursor.copy();
                                    single.setCount(1);
                                    slot.setStack(single);
                                } else if (slot.getStack().getCount() < slot.getStack().getMaxCount()) {
                                    slot.getStack().increment(1);
                                }
                                // cursor.decrement(1); // Normally handled at stage 2
                            }
                        }
                    }
                }
            }

            slot.markDirty();
        } else if (slotIndex == -999 && actionType == net.minecraft.screen.slot.SlotActionType.PICKUP) {
            ItemStack cursorStack = this.getCursorStack();
            if (!cursorStack.isEmpty()) {
                if (button == 0) {
                    this.setCursorStack(ItemStack.EMPTY);
                } else {
                    cursorStack.decrement(1);
                }
            }
        }

        if (player.isCreative()) {
            for (int i = 0; i < player.getInventory().size(); i++) {
                ItemStack now = player.getInventory().getStack(i);
                ItemStack old = before.get(i);
                boolean changed = false;
                if (now.isEmpty() && old.isEmpty()) changed = false;
                else if (now.isEmpty() != old.isEmpty()) changed = true;
                else if (now.getCount() != old.getCount()) changed = true;
                else if (!ItemStack.areItemsAndComponentsEqual(now, old)) changed = true;

                if (changed) {
                    int packetSlot = -1;
                    if (i >= 0 && i <= 8) packetSlot = i + 36;
                    else if (i >= 9 && i <= 35) packetSlot = i;
                    else if (i >= 36 && i <= 39) packetSlot = 8 - (i - 36);
                    else if (i == 40) packetSlot = 45;

                    if (packetSlot != -1 && net.minecraft.client.MinecraftClient.getInstance().getNetworkHandler() != null) {
                        net.minecraft.client.MinecraftClient.getInstance().getNetworkHandler().sendPacket(
                                new net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket(packetSlot, now)
                        );
                    }
                }
            }
        }
    }

    public void setSlotTextures(Identifier... textures) {
        for (int i = 0; i < textures.length && i < this.slots.size(); i++) {
            Slot slot = this.slots.get(i);
            if (slot instanceof ContainerScreenHandlerSlot custom) {
                custom.setTexture(textures[i]);
            }
        }
    }

    @Override
    public ItemStack quickMove(net.minecraft.entity.player.PlayerEntity player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = (Slot)this.slots.get(index);
        if (slot != null && slot.hasStack()) {
            ItemStack itemStack2 = slot.getStack();
            itemStack = itemStack2.copy();
            if (index < this.rowCount * 9 ? !this.insertItem(itemStack2, this.rowCount * 9, this.slots.size(), true) : !this.insertItem(itemStack2, 0, this.rowCount * 9, false)) {
                return ItemStack.EMPTY;
            }
            if (itemStack2.isEmpty()) {
                slot.setStackNoCallbacks(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }
        return itemStack;
    }
}
