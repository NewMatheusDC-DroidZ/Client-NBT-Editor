package com.trusted.systemnbteditor.gui;

import com.mojang.authlib.GameProfile;
import com.trusted.systemnbteditor.util.SkidCacheManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.input.KeyInput;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

public class SkidViewScreen extends GenericContainerScreen {
    private final Screen parent;
    private final List<UUID> players = new ArrayList<>();

    public SkidViewScreen(Screen parent) {
        super(GenericContainerScreenHandler.createGeneric9x3(
                0, net.minecraft.client.MinecraftClient.getInstance().player.getInventory(), new SimpleInventory(27)),
              net.minecraft.client.MinecraftClient.getInstance().player.getInventory(),
              Text.of("Skid - View Captured Items"));
        this.parent = parent;
        this.players.addAll(SkidCacheManager.getCapturedPlayers());
        
        // Populate the dummy slots with the heads
        for (int i = 0; i < players.size() && i < 27; i++) {
            UUID uuid = players.get(i);
            ItemStack head = SkidCacheManager.getPlayerHead(uuid);
            this.handler.getInventory().setStack(i, head);
        }
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    protected void onMouseClick(net.minecraft.screen.slot.Slot slot, int slotId, int button, SlotActionType actionType) {
        if (slot != null && slot.inventory == this.handler.getInventory() && slot.hasStack()) {
            int i = slot.getIndex();
            if (i >= 0 && i < players.size()) {
                UUID selectedPlayer = players.get(i);
                if (this.client != null) {
                    String name = SkidCacheManager.getPlayerName(selectedPlayer);
                    this.client.setScreen(new SkidPlayerItemsScreen(this, selectedPlayer, name));
                }
            }
        }
        // Cancel the actual click action so the head isn't picked up
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        } else {
            super.close();
        }
    }
}
