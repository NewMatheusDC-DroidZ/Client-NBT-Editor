package com.trusted.systemnbteditor.clientchest;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.serialization.Dynamic;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.datafixer.TypeReferences;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.trusted.systemnbteditor.util.NbtIoUtils;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

public class ClientChestManager {
    public static final int PAGE_SIZE = 54;
    public static final int PAGE_COUNT = 100;
    private static final int CURRENT_DATA_VERSION = 4556; // 1.21.11

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    private static final ClientChestManager INSTANCE = new ClientChestManager();

    private final File folder;

    public static ClientChestManager getInstance() {
        return INSTANCE;
    }

    private ClientChestManager() {
        this.folder = new File(FabricLoader.getInstance().getGameDir().toFile(), "ClientChest");
        ensureFolder();
        importSeedPageIfNeeded();
    }

    public File getFolder() {
        return folder;
    }

    public int getPageCount() {
        return PAGE_COUNT;
    }

    public int[] getNearestPois(int page) {
        int prev = -1;
        int next = -1;
        for (int i = page - 1; i >= 0; i--) {
            if (hasPageData(i)) {
                prev = i;
                break;
            }
        }
        for (int i = page + 1; i < PAGE_COUNT; i++) {
            if (hasPageData(i)) {
                next = i;
                break;
            }
        }
        return new int[] {prev, next};
    }

    public ItemStack[] loadPage(int page) {
        ItemStack[] output = createEmptyItems();
        File file = getPageFile(page);
        if (!file.exists()) return output;

        try {
            NbtCompound root = NbtIoUtils.readNbt(file.toPath());
            if (root == null) return output;

            int sourceVersion = root.getInt("DataVersion").orElse(CURRENT_DATA_VERSION);
            
            NbtElement maybeItems = root.get("items");
            if (maybeItems == null) maybeItems = root.get("Items");
            
            if (maybeItems instanceof NbtList list) {
                // If it's a list with "Slot" tags, use slot-aware loading
                boolean hasSlotIndices = false;
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i) instanceof NbtCompound raw && (raw.contains("Slot") || raw.contains("slot"))) {
                        hasSlotIndices = true;
                        break;
                    }
                }

                if (hasSlotIndices) {
                    for (int i = 0; i < list.size(); i++) {
                        if (list.get(i) instanceof NbtCompound raw) {
                            int slot = readSlot(raw);
                            if (slot >= 0 && slot < PAGE_SIZE) {
                                output[slot] = decodeStackWithRecovery(normalizeItemCompound(raw), sourceVersion);
                            }
                        }
                    }
                } else {
                    // Positional loading
                    for (int i = 0; i < PAGE_SIZE && i < list.size(); i++) {
                        output[i] = decodeStackWithRecovery(list.get(i), sourceVersion);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load Client Chest page {}", page, e);
        }
        return output;
    }

    public void savePage(int page, ItemStack[] items) {
        ensureFolder();

        try {
            NbtCompound root = new NbtCompound();
            root.putInt("DataVersion", CURRENT_DATA_VERSION);
            NbtList list = new NbtList();
            for (int i = 0; i < PAGE_SIZE; i++) {
                ItemStack stack = (items != null && i < items.length && items[i] != null) ? items[i] : ItemStack.EMPTY;
                list.add(encodeStack(stack));
            }
            root.put("items", list);

            File target = getPageFile(page);
            File tmp = new File(folder, "saving_page" + page + "_" + System.currentTimeMillis() + ".nbt");
            
            try (FileOutputStream fos = new FileOutputStream(tmp)) {
                NbtIo.writeCompressed(root, fos);
            }
            
            if (target.exists()) target.delete();
            if (!tmp.renameTo(target)) {
                // Manual move if rename fails
                try (FileInputStream fis = new FileInputStream(tmp); FileOutputStream fos = new FileOutputStream(target)) {
                    fis.transferTo(fos);
                }
                tmp.delete();
            }
            LOGGER.info("Saved Client Chest page {} (compressed)", page);
        } catch (Exception e) {
            LOGGER.error("Failed to save Client Chest page {}", page, e);
        }
    }

    private ItemStack decodeStackWithRecovery(NbtElement itemNbt, int sourceVersion) {
        if (itemNbt == null) return ItemStack.EMPTY;
        if (itemNbt instanceof NbtCompound cmp && cmp.isEmpty()) return ItemStack.EMPTY;

        ItemStack direct = decodeStack(itemNbt);
        if (!direct.isEmpty()) return direct;

        if (itemNbt instanceof NbtCompound cmp && sourceVersion > 0 && sourceVersion < CURRENT_DATA_VERSION) {
            try {
                NbtCompound fixed = update(TypeReferences.ITEM_STACK, cmp, sourceVersion);
                ItemStack updated = decodeStack(fixed);
                if (!updated.isEmpty()) return updated;
            } catch (Exception ignored) {
            }
        }

        if (itemNbt instanceof NbtCompound cmp) {
            ItemStack idFallback = decodeByIdOnly(cmp);
            if (!idFallback.isEmpty()) return idFallback;
        }

        return createCorruptedBarrier();
    }

    @SuppressWarnings("unchecked")
    private NbtCompound update(TypeReference typeRef, NbtCompound nbt, int oldVersion) {
        MinecraftClient client = MinecraftClient.getInstance();
        return (NbtCompound) client.getDataFixer()
                .update(typeRef, new Dynamic<>(NbtOps.INSTANCE, nbt), oldVersion, CURRENT_DATA_VERSION)
                .getValue();
    }

    private ItemStack decodeStack(NbtElement nbt) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world == null) return ItemStack.EMPTY;
            RegistryWrapper.WrapperLookup lookup = client.world.getRegistryManager();
            RegistryOps<NbtElement> ops = lookup.getOps(NbtOps.INSTANCE);
            com.mojang.serialization.DataResult<ItemStack> result = ItemStack.CODEC.parse(ops, nbt);
            if (result.error().isPresent()) {
                LOGGER.error("ItemStack parse error: " + result.error().get().message() + " for NBT: " + nbt);
            }
            return result.result().orElse(ItemStack.EMPTY);
        } catch (Exception e) {
            LOGGER.error("ClientChestManager: Exception decoding stack: ", e);
            return ItemStack.EMPTY;
        }
    }

    private NbtElement encodeStack(ItemStack stack) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world == null) return new NbtCompound();
            RegistryWrapper.WrapperLookup lookup = client.world.getRegistryManager();
            RegistryOps<NbtElement> ops = lookup.getOps(NbtOps.INSTANCE);
            
            if (stack == null || stack.isEmpty()) return new NbtCompound(); // Don't even try encoding empty stacks
            
            com.mojang.serialization.DataResult<NbtElement> result = ItemStack.CODEC.encodeStart(ops, stack);
            if (result.error().isPresent()) {
                 LOGGER.error("ClientChestManager: encode error: " + result.error().get().message());
            }
            return result.result().orElse(new NbtCompound());
        } catch (Exception e) {
            LOGGER.error("ClientChestManager: Exception encoding stack: ", e);
            return new NbtCompound();
        }
    }

    private ItemStack decodeByIdOnly(NbtCompound nbt) {
        try {
            NbtElement idObj = nbt.get("id");
            String id = idObj != null ? idObj.asString().orElse("") : "";
            if (id.isBlank()) return ItemStack.EMPTY;
            Identifier identifier = Identifier.tryParse(id);
            if (identifier == null) return ItemStack.EMPTY;
            Item item = Registries.ITEM.get(identifier);
            if (item == Items.AIR) return ItemStack.EMPTY;
            
            int count = 1;
            NbtElement countObj = nbt.get("count");
            if (countObj instanceof net.minecraft.nbt.AbstractNbtNumber num) count = num.intValue();
            
            return new ItemStack(item, Math.min(Math.max(1, count), item.getMaxCount()));
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    private NbtCompound normalizeItemCompound(NbtCompound raw) {
        if (raw == null) return new NbtCompound();
        if (raw.contains("dynamic")) raw.remove("dynamic");
        if (raw.contains("item")) {
            return raw.getCompound("item").orElse(raw);
        }
        if (raw.contains("stack")) {
            return raw.getCompound("stack").orElse(raw);
        }
        return raw;
    }

    private int readSlot(NbtCompound raw) {
        if (raw.contains("Slot")) return raw.getInt("Slot").orElse(-1);
        if (raw.contains("slot")) return raw.getInt("slot").orElse(-1);
        return -1;
    }

    private ItemStack createCorruptedBarrier() {
        ItemStack barrier = new ItemStack(Items.BARRIER);
        barrier.set(DataComponentTypes.CUSTOM_NAME, Text.literal("NBT Corrupted").fillStyle(
                Style.EMPTY.withBold(true).withItalic(false).withColor(Formatting.DARK_RED)));
        return barrier;
    }



    private void importSeedPageIfNeeded() {
        if (getPageFile(0).exists()) return;
        try {
            File bundled = new File("othermodsource/page0.nbt");
            if (!bundled.exists()) return;
            try (FileInputStream in = new FileInputStream(bundled); FileOutputStream out = new FileOutputStream(getPageFile(0))) {
                in.transferTo(out);
            }
        } catch (Exception ignored) {
        }
    }

    private boolean hasPageData(int page) {
        return getPageFile(page).exists();
    }

    private File getPageFile(int page) {
        return new File(folder, "page" + page + ".nbt");
    }

    private void ensureFolder() {
        if (!folder.exists()) folder.mkdirs();
    }

    private ItemStack[] createEmptyItems() {
        ItemStack[] output = new ItemStack[PAGE_SIZE];
        for (int i = 0; i < PAGE_SIZE; i++) output[i] = ItemStack.EMPTY;
        return output;
    }

    private boolean isAllEmpty(ItemStack[] items) {
        if (items == null) return true;
        for (int i = 0; i < Math.min(items.length, PAGE_SIZE); i++) {
            if (items[i] != null && !items[i].isEmpty()) return false;
        }
        return true;
    }

    private NbtCompound readNbt(File file) {
        return NbtIoUtils.readNbt(file.toPath());
    }
}
