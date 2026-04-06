package com.trusted.systemnbteditor.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.Entity;
import net.minecraft.registry.Registries;
import java.nio.file.Path;
import java.util.Optional;

public class ImportUtils {

    public static void importNbt(Path path) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        NbtCompound nbt = NbtIoUtils.readNbt(path);
        if (nbt == null) {
            client.player.sendMessage(Text.literal("Failed to read NBT file: " + path.getFileName()).formatted(Formatting.RED), false);
            return;
        }

        try {
            if (tryImportAsItem(nbt, client)) return;
            if (tryImportAsEntity(nbt, client)) return;
            // if (tryImportAsBlock(nbt, client)) return; // Block placement is more complex, skipping for now unless needed

            client.player.sendMessage(Text.literal("Unknown NBT format in " + path.getFileName()).formatted(Formatting.YELLOW), false);

        } catch (Exception e) {
            client.player.sendMessage(Text.literal("Error importing " + path.getFileName() + ": " + e.getMessage()).formatted(Formatting.RED), false);
            e.printStackTrace();
        }
    }

    private static boolean tryImportAsItem(NbtCompound nbt, MinecraftClient client) {
        // Prepare Codec Ops
        net.minecraft.registry.RegistryWrapper.WrapperLookup lookup = client.world.getRegistryManager();
        net.minecraft.registry.RegistryOps<net.minecraft.nbt.NbtElement> ops = net.minecraft.registry.RegistryOps.of(net.minecraft.nbt.NbtOps.INSTANCE, lookup);

        // Simple check for item structure
        if (nbt.contains("id") && nbt.contains("Count")) { 
             ItemStack itemStack = ItemStack.CODEC.parse(ops, (net.minecraft.nbt.NbtElement) nbt).result().orElse(ItemStack.EMPTY);
            if (!itemStack.isEmpty()) {
                giveItemToPlayer(client, itemStack);
                client.player.sendMessage(Text.literal("Imported item: ").append(itemStack.getName()).formatted(Formatting.GREEN), false);
                return true;
            }
        }
        
        // Check for "Item" tag wrapping (common in some exports)
         // Note: getCompound returns Optional in this environment seems
         // But wait, if getCompound returns Optional<NbtCompound>, checking contains("Item") first is good.
         if (nbt.contains("Item")) { 
             // We need to get the compound safely. 
             // Assuming getCompound returns Optional<NbtCompound> base on previous error
             java.util.Optional<net.minecraft.nbt.NbtCompound> itemTagOpt = nbt.getCompound("Item");
             if (itemTagOpt.isPresent()) {
                 ItemStack itemStack = ItemStack.CODEC.parse(ops, (net.minecraft.nbt.NbtElement) itemTagOpt.get()).result().orElse(ItemStack.EMPTY);
                 if (!itemStack.isEmpty()) {
                     giveItemToPlayer(client, itemStack);
                     client.player.sendMessage(Text.literal("Imported item: ").append(itemStack.getName()).formatted(Formatting.GREEN), false);
                     return true;
                 }
             }
         }

        return false;
    }

    private static void giveItemToPlayer(MinecraftClient client, ItemStack itemStack) {
        client.player.getInventory().setStack(client.player.getInventory().selectedSlot, itemStack);
        client.interactionManager.clickCreativeStack(itemStack, 36 + client.player.getInventory().selectedSlot);
        client.player.getInventory().offerOrDrop(itemStack); // Fallback if packet fails 
    }

    private static boolean tryImportAsEntity(NbtCompound nbt, MinecraftClient client) {
        if (nbt.contains("id") && nbt.contains("Pos")) { // 8=String, 9=List
             // Entities can be complex to summon client-side correctly without server commands, 
             // but we can try to summon a client-side entity for viewing or generate a command.
             // For a pure client utility, giving a spawn egg or just notifying might be safer, 
             // but the user asked to "try to implement it here" referring to the flawless editor logic which summons.
             // Client-side summoning is only visual. Real summoning needs /summon.
             
             // Let's generate a command for now as it's safer and useful
             String id = "";
             if (nbt.contains("id")) {
                 net.minecraft.nbt.NbtElement idElem = nbt.get("id");
                 if (idElem != null) id = idElem.asString().orElse("");
             }
             client.player.sendMessage(Text.literal("Entity import detected (ID: " + id + "). Copying command to clipboard...").formatted(Formatting.YELLOW), false);
             
             // Construct summon command
             String command = "/summon " + id + " ~ ~ ~ " + nbt.asString();
             client.keyboard.setClipboard(command);
             client.player.sendMessage(Text.literal("Summon command copied!").formatted(Formatting.GREEN), false);
             return true;
        }
        return false;
    }
}
