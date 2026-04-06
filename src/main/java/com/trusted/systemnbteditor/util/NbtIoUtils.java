package com.trusted.systemnbteditor.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;

public class NbtIoUtils {
    public static final File EXPORT_DIR = new File(MinecraftClient.getInstance().runDirectory, "exported_nbt");

    public static void exportNbt(NbtCompound nbt, String name) {
        if (!EXPORT_DIR.exists()) {
            EXPORT_DIR.mkdirs();
        }

        // Sanitize name
        String safeName = name.replaceAll("[^a-zA-Z0-9\\.\\-_]", "_");
        if (!safeName.endsWith(".nbt")) {
            safeName += ".nbt";
        }

        File file = new File(EXPORT_DIR, safeName);
        
        // Avoid overwriting by appending numbers
        int count = 1;
        while (file.exists()) {
             String baseName = safeName.substring(0, safeName.length() - 4);
             file = new File(EXPORT_DIR, baseName + "_" + count + ".nbt");
             count++;
        }

        try (FileOutputStream out = new FileOutputStream(file)) {
            NbtIo.writeCompressed(nbt, out);
            MinecraftClient.getInstance().player.sendMessage(
                Text.literal("Exported to " + file.getName()).formatted(Formatting.GREEN), false);
        } catch (IOException e) {
            e.printStackTrace();
            MinecraftClient.getInstance().player.sendMessage(
                Text.literal("Failed to export: " + e.getMessage()).formatted(Formatting.RED), false);
        }
    }

    public static NbtCompound readNbt(Path path) {
        File file = path.toFile();
        try (FileInputStream in = new FileInputStream(file)) {
             // Try compressed first
             try {
                return NbtIo.readCompressed(in, net.minecraft.nbt.NbtSizeTracker.ofUnlimitedBytes());
             } catch (Exception e) {
                 // Try uncompressed if compressed fails (reset stream? need new stream)
             }
        } catch (IOException e) {
        }
        
        // Retry uncompressed
        try (FileInputStream in = new FileInputStream(file)) {
            java.io.DataInputStream dis = new java.io.DataInputStream(in);
            net.minecraft.nbt.NbtElement element = NbtIo.read(dis, net.minecraft.nbt.NbtSizeTracker.ofUnlimitedBytes());
            if (element instanceof NbtCompound compound) {
                return compound;
            }
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static long getItemSize(NbtCompound nbt) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            NbtIo.write(nbt, dos);
            return baos.size();
        } catch (Exception e) {
            return -1;
        }
    }

    public static void giveItem(NbtCompound nbt) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        try {
            RegistryWrapper.WrapperLookup lookup = client.world.getRegistryManager();
            RegistryOps<NbtElement> ops = RegistryOps.of(NbtOps.INSTANCE, lookup);
            
            ItemStack stack = ItemStack.CODEC.parse(ops, nbt).result().orElse(ItemStack.EMPTY);
            if (stack.isEmpty()) {
                client.player.sendMessage(Text.literal("Error: Invalid item NBT").formatted(Formatting.RED), false);
                return;
            }

            giveItemStack(stack);
        } catch (Exception e) {
            client.player.sendMessage(Text.literal("Import Error: " + e.getMessage()).formatted(Formatting.RED), false);
        }
    }

    public static void giveItemStack(ItemStack stack) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        PlayerInventory inventory = player.getInventory();
        
        // Find best slot: 
        // 1. Existing stackable slot
        // 2. Empty slot
        // 3. Current selected slot (fallback)
        int targetSlot = -1;
        
        // Try to find a stackable slot first
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack existing = inventory.getStack(i);
            if (!existing.isEmpty() && ItemStack.areItemsAndComponentsEqual(existing, stack) && existing.getCount() < existing.getMaxCount()) {
                targetSlot = i;
                break;
            }
        }
        
        // Then try empty slot
        if (targetSlot == -1) {
            targetSlot = inventory.getEmptySlot();
        }
        
        // Fallback to current slot
        if (targetSlot == -1) {
            targetSlot = inventory.selectedSlot;
        }

        inventory.setStack(targetSlot, stack.copy());
        
        // Creative packets use container slot indices:
        // Hotbar (0-8) -> 36-44
        // Inventory (9-35) -> 9-35
        // Offhand (45) -> 45
        int containerSlot = targetSlot;
        if (targetSlot >= 0 && targetSlot < 9) {
            containerSlot = targetSlot + 36;
        }

        if (client.getNetworkHandler() != null) {
            client.getNetworkHandler().sendPacket(new CreativeInventoryActionC2SPacket(containerSlot, stack));
            player.sendMessage(Text.literal("Imported item into slot " + (targetSlot + 1)).formatted(Formatting.GREEN), true);
        }
    }
}
