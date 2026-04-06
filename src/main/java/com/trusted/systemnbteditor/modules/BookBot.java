package com.trusted.systemnbteditor.modules;

import com.trusted.systemnbteditor.data.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WritableBookContentComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.text.RawFilteredPair;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

public class BookBot {
    public static void execute(MinecraftClient client) {
        if (client.player == null) return;

        ItemStack stack = client.player.getMainHandStack();
        if (stack.getItem() != Items.WRITABLE_BOOK) {
            client.player.sendMessage(Text.literal("You must hold a Book and Quill!").formatted(Formatting.RED), true);
            return;
        }

        try {
            // Max pages is usually 100.
            // We want to exceed 100kb. 
            // 1 page = ~1024 chars in 1.21.x usually.
            // Unicode characters (like Japanese or emojis) take more space in NBT.
            List<RawFilteredPair<String>> pages = new ArrayList<>();
            
            // Generate a very long string for each page.
            // 1.21.11 limits might be present, but we try to maximize.
            StringBuilder pageBuilder = new StringBuilder();
            for (int j = 0; j < 512; j++) {
                pageBuilder.append("あ"); // 3 bytes in UTF-8
            }
            String denseText = pageBuilder.toString();

            for (int i = 0; i < 100; i++) {
                pages.add(RawFilteredPair.of(denseText));
            }

            WritableBookContentComponent component = new WritableBookContentComponent(pages);
            stack.set(DataComponentTypes.WRITABLE_BOOK_CONTENT, component);

            // Send packet to server. Creative packet is most reliable if allowed.
            // If in survival, the server might reject this if it's too large, 
            // but that's exactly what "Danger Zone" features are for.
            int slot = 36 + client.player.getInventory().selectedSlot;
            client.getNetworkHandler().sendPacket(new CreativeInventoryActionC2SPacket(slot, stack));

            client.player.sendMessage(Text.literal("Book filled! Size is roughly " + (pages.size() * denseText.length() * 3 / 1024) + " KB").formatted(Formatting.GREEN), true);
        } catch (Exception e) {
            client.player.sendMessage(Text.literal("Error filling book: " + e.getMessage()).formatted(Formatting.RED), true);
        }
    }
}
