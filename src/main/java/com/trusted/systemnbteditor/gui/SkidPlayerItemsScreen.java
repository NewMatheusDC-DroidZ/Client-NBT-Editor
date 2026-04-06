package com.trusted.systemnbteditor.gui;

import com.trusted.systemnbteditor.util.SkidCacheManager;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

import java.util.List;
import java.util.UUID;

public class SkidPlayerItemsScreen extends GenericContainerScreen {
    private final Screen parent;
    private final UUID playerUuid;
    private final String playerName;

    public SkidPlayerItemsScreen(Screen parent, UUID playerUuid, String playerName) {
        super(GenericContainerScreenHandler.createGeneric9x6(
                0, 
                net.minecraft.client.MinecraftClient.getInstance().player.getInventory(), 
                new SimpleInventory(54)
        ), 
        net.minecraft.client.MinecraftClient.getInstance().player.getInventory(), 
        Text.literal("Skid - " + playerName + "'s Items"));
        
        this.parent = parent;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        
        List<ItemStack> items = SkidCacheManager.getItemsForPlayer(playerUuid);
        for (int i = 0; i < items.size() && i < 54; i++) {
            this.handler.getInventory().setStack(i, items.get(i).copy());
        }
    }

    @Override
    protected void onMouseClick(Slot slot, int slotId, int button, SlotActionType actionType) {
        if (this.client == null || this.client.player == null) return;
        
        net.minecraft.entity.player.PlayerInventory inv = this.client.player.getInventory();
        ItemStack[] before = new ItemStack[36];
        for(int i = 0; i < 36; i++) {
            before[i] = inv.getStack(i).copy();
        }
        
        if (slot != null && slotId != -999) {
            this.handler.onSlotClick(slot.id, button, actionType, this.client.player);
        } else {
            this.handler.onSlotClick(slotId, button, actionType, this.client.player);
        }
        
        if (this.client.getNetworkHandler() != null) {
            for (int i = 0; i < 36; i++) {
                ItemStack after = inv.getStack(i);
                if (!ItemStack.areEqual(before[i], after)) {
                    int networkSlot = (i < 9) ? i + 36 : i;
                    this.client.getNetworkHandler().sendPacket(
                            new net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket(networkSlot, after.copy())
                    );
                }
            }
        }
    }
}
