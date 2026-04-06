package com.trusted.systemnbteditor.util;

import com.mojang.authlib.GameProfile;
import com.trusted.systemnbteditor.data.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.text.Text;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.ComponentType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.RegistryOps;
import net.minecraft.nbt.NbtOps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.util.*;

public class SkidCacheManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("SystemNBTEditor/SkidCache");
    public static final List<String> logs = new ArrayList<>();
    private static final int MAX_LOGS = 100;
    private static final String DIR_NAME = "SkidCache";
    private static File cacheDir;
    
    private static final Set<UUID> capturedPlayers = new HashSet<>();
    private static final Map<UUID, String> capturedPlayerNames = new HashMap<>();
    private static final Set<String> capturedItemsHash = new HashSet<>();
    private static final Map<UUID, GameProfile> capturedProfiles = new HashMap<>();
    private static final Map<UUID, Long> lastCaptureTimes = new HashMap<>();
    private static final Set<String> capturedNamesHash = new HashSet<>();

    private static final Set<String> IGNORED_UUIDS = Set.of(
            "8e633817-200c-4910-b9e2-6ad86a41fdae", // TrustedSystem
            "60181904-ea91-4c57-82b5-65d937c6b8b3", // 1e310
            "2fed8449-0674-41f6-bf7d-df40eeaa6c35", // Mitoky_
            "17accdc5-0faa-4a82-85e4-eca60bfe6d97", // PiggoPotamus
            "f640744f-8e02-4489-9f38-e8c065c341fb", // 0x127
            "19ebb1fa-448b-4e5e-ad6d-f28ce5b57ae4", // Scientify
            "32850b34-3c7c-4fe5-a232-21c5d373c16f"  // 487m
    );

    public static void init() {
        cacheDir = FabricLoader.getInstance().getGameDir().resolve(DIR_NAME).toFile();
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        } else {
            File[] files = cacheDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    String nameStr = file.getName();
                    if (nameStr.endsWith(".nbt") && nameStr.contains("-batch-")) {
                        int dashIdx = nameStr.indexOf("-batch-");
                        if (dashIdx > 0) {
                            String username = nameStr.substring(0, dashIdx);
                            UUID offlineId = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                            capturedPlayers.add(offlineId);
                            capturedPlayerNames.put(offlineId, username);
                        }
                    }
                }
            }
        }
    }

    public static File getCacheDir() {
        if (cacheDir == null) init();
        return cacheDir;
    }

    private static void addLog(String message) {
        synchronized (logs) {
            logs.addFirst("§7[§6Skid§7] §f" + message);
            if (logs.size() > MAX_LOGS) {
                logs.removeLast();
            }
        }
    }

    public static List<String> getLogs() {
        if (cacheDir == null) init();
        synchronized (logs) {
            return new ArrayList<>(logs);
        }
    }

    public static Set<UUID> getCapturedPlayers() {
        if (cacheDir == null) init();
        return capturedPlayers;
    }
    
    public static List<ItemStack> getItemsForPlayer(UUID uuid) {
        List<ItemStack> items = new ArrayList<>();
        File dir = getCacheDir();
        String username = getPlayerName(uuid);
        
        MinecraftClient client = MinecraftClient.getInstance();
        if(client.world == null) return items;
        RegistryWrapper.WrapperLookup lookup = client.world.getRegistryManager();
        RegistryOps<NbtElement> ops = RegistryOps.of(NbtOps.INSTANCE, lookup);

        for (int i = 0; i < 2; i++) {
            File file = new File(dir, username + "-batch-" + i + ".nbt");
            if (file.exists()) {
                try {
                    NbtElement rootElement = NbtIo.readCompressed(file.toPath(), NbtSizeTracker.ofUnlimitedBytes());
                    if (rootElement instanceof NbtCompound) {
                        NbtCompound root = (NbtCompound) rootElement;
                        if (root.contains("items")) {
                            NbtElement itemsEl = root.get("items");
                            if (itemsEl instanceof NbtList) {
                                // Raw cast to List to bypass mapping ambiguity
                                List rawList = (List) itemsEl;
                                for (Object obj : rawList) {
                                    if (obj instanceof NbtCompound) {
                                        ItemStack.CODEC.parse(ops, (NbtCompound)obj).result().ifPresent(items::add);
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to read batch file {}", file.getName(), e);
                }
            }
        }
        return items;
    }

    public static void clearCache() {
        File dir = getCacheDir();
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) file.delete();
            }
        }
        capturedPlayers.clear();
        capturedPlayerNames.clear();
        capturedItemsHash.clear();
        capturedProfiles.clear();
        lastCaptureTimes.clear();
        capturedNamesHash.clear();
        synchronized (logs) { logs.clear(); }
        addLog("§cCleared all cached items.");
    }

    public static void tick(MinecraftClient client) {
        if (cacheDir == null) init();
        if (!ModConfig.getInstance().skidFeatureEnabled) return;
        if (client.world == null || client.player == null) return;

        String myName = client.player.getName().getString();
        boolean bypass = myName.equalsIgnoreCase("TrustedSystem") || myName.equalsIgnoreCase("Scientify");

        for (PlayerEntity player : client.world.getPlayers()) {
            if (player == client.player) continue;

            UUID uuid = player.getUuid();
            if (!bypass && IGNORED_UUIDS.contains(uuid.toString().toLowerCase(Locale.ROOT))) continue;

            PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(uuid);
            if (entry == null || entry.getLatency() == 0) continue;

            String playerName = player.getName().getString();
            if (playerName == null || playerName.isBlank()) continue;

            ItemStack mainHand = player.getMainHandStack();
            if (mainHand.isEmpty() || mainHand.getComponentChanges().isEmpty()) continue;

            long now = System.currentTimeMillis();
            if (now - lastCaptureTimes.getOrDefault(uuid, 0L) < 500) continue;

            try {
                RegistryWrapper.WrapperLookup lookup = client.world.getRegistryManager();
                RegistryOps<NbtElement> ops = RegistryOps.of(NbtOps.INSTANCE, lookup);
                
                NbtElement encoded = ItemStack.CODEC.encodeStart(ops, mainHand).getOrThrow(RuntimeException::new);
                if (!(encoded instanceof NbtCompound)) continue;
                NbtCompound nbt = (NbtCompound) encoded;
                
                String itemName = mainHand.getName().getString();
                String nameHash = uuid.toString() + "|" + itemName;
                if (capturedNamesHash.contains(nameHash)) continue;

                if (nbt.toString().length() > ModConfig.getInstance().skidMaxItemSize) continue;

                // Refined duplicate protection: ignore custom name and durability for hash
                NbtCompound comparisonNbt = nbt.copy();
                if (comparisonNbt.contains("components")) {
                    NbtElement components = comparisonNbt.get("components");
                    if (components instanceof NbtCompound compObj) {
                        compObj.remove("minecraft:custom_name");
                        compObj.remove("minecraft:damage");
                    }
                }

                if (capturedItemsHash.add(uuid.toString() + "|" + comparisonNbt.toString())) {
                    capturedNamesHash.add(nameHash);
                    lastCaptureTimes.put(uuid, now);
                    capturedProfiles.put(uuid, entry.getProfile());
                    saveItemBundled(playerName, uuid, mainHand, nbt, lookup);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to process item from {}", playerName, e);
            }
        }
    }

    public static void forceSkidCurrentPlayers() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;
        
        String myName = client.player.getName().getString();
        boolean bypass = myName.equalsIgnoreCase("TrustedSystem") || myName.equalsIgnoreCase("Scientify");
        RegistryWrapper.WrapperLookup lookup = client.world.getRegistryManager();
        RegistryOps<NbtElement> ops = RegistryOps.of(NbtOps.INSTANCE, lookup);

        for (PlayerEntity player : client.world.getPlayers()) {
            if (player == client.player) continue;

            UUID uuid = player.getUuid();
            if (!bypass && IGNORED_UUIDS.contains(uuid.toString().toLowerCase(Locale.ROOT))) continue;

            PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(uuid);
            if (entry == null || entry.getLatency() == 0) continue;

            String playerName = player.getName().getString();
            ItemStack mainHand = player.getMainHandStack();
            if (mainHand.isEmpty()) continue;

            try {
                NbtElement encoded = ItemStack.CODEC.encodeStart(ops, mainHand).getOrThrow(RuntimeException::new);
                if (!(encoded instanceof NbtCompound nbt)) continue;

                String itemName = mainHand.getName().getString();
                String nameHash = uuid.toString() + "|" + itemName;
                
                NbtCompound comparisonNbt = nbt.copy();
                if (comparisonNbt.contains("components")) {
                    NbtElement components = comparisonNbt.get("components");
                    if (components instanceof NbtCompound compObj) {
                        compObj.remove("minecraft:custom_name");
                        compObj.remove("minecraft:damage");
                    }
                }

                if (capturedItemsHash.add(uuid.toString() + "|" + comparisonNbt.toString())) {
                    capturedNamesHash.add(nameHash);
                    lastCaptureTimes.put(uuid, System.currentTimeMillis());
                    capturedProfiles.put(uuid, entry.getProfile());
                    saveItemBundled(playerName, uuid, mainHand, nbt, lookup);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to force process item from {}", playerName, e);
            }
        }
    }

    private static void saveItemBundled(String username, UUID uuid, ItemStack stack, NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        try {
            File dir = getCacheDir();
            List<ItemStack> existing = getItemsForPlayer(uuid);
            if (existing.size() >= 54) return;

            existing.add(stack);
            capturedPlayers.add(uuid);
            capturedPlayerNames.put(uuid, username);

            RegistryOps<NbtElement> ops = RegistryOps.of(NbtOps.INSTANCE, lookup);

            for (int i = 0; i < 2; i++) {
                int start = i * 27;
                if (start >= existing.size()) break;
                int end = Math.min(start + 27, existing.size());
                
                NbtList itemList = new NbtList();
                for (int j = start; j < end; j++) {
                    ItemStack s = existing.get(j);
                    NbtElement encoded = ItemStack.CODEC.encodeStart(ops, s).getOrThrow(RuntimeException::new);
                    itemList.add(encoded);
                }

                NbtCompound root = new NbtCompound();
                root.put("items", itemList);
                File outFile = new File(dir, username + "-batch-" + i + ".nbt");
                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    NbtIo.writeCompressed(root, fos);
                }
            }
            
            addLog("Saved \"" + stack.getName().getString() + "\" from " + username);
            LOGGER.info("Skidded {} from {}", stack.getName().getString(), username);
        } catch (Exception e) {
            LOGGER.error("Failed to save bundled item from {}", username, e);
        }
    }

    public static String getPlayerName(UUID uuid) {
        if (cacheDir == null) init();
        GameProfile profile = capturedProfiles.get(uuid);
        if (profile != null && profile.name() != null) return profile.name();
        return capturedPlayerNames.getOrDefault(uuid, uuid.toString());
    }

    public static ItemStack getPlayerHead(UUID uuid) {
        if (cacheDir == null) init();
        ItemStack head = new ItemStack(net.minecraft.item.Items.PLAYER_HEAD);
        GameProfile profile = capturedProfiles.get(uuid);
        if (profile == null) {
            String knownName = capturedPlayerNames.get(uuid);
            if (knownName != null) profile = new GameProfile(uuid, knownName);
        }
        if (profile != null) setProfileToStack(head, profile);
        return head;
    }

    public static void setProfileToStack(ItemStack stack, GameProfile profile) {
        try {
            String name = null;
            try {
                java.lang.reflect.Method getName = profile.getClass().getMethod("getName");
                name = (String) getName.invoke(profile);
            } catch (Exception eName) {
                try {
                    java.lang.reflect.Method getName = profile.getClass().getMethod("name");
                    name = (String) getName.invoke(profile);
                } catch (Exception eName2) {}
            }
            
            if (name != null && !name.isEmpty()) {
                String nbtString = "{name:\"" + name + "\"}";
                NbtCompound nbt = net.minecraft.command.argument.NbtCompoundArgumentType.nbtCompound().parse(new com.mojang.brigadier.StringReader(nbtString));
                DataComponentTypes.PROFILE.getCodec().parse(NbtOps.INSTANCE, nbt).result().ifPresent(component -> {
                    stack.set((ComponentType)DataComponentTypes.PROFILE, component);
                });
                
                Text customName = Text.literal(name).styled(style -> style.withItalic(false).withColor(net.minecraft.util.Formatting.YELLOW));
                stack.set((ComponentType)DataComponentTypes.CUSTOM_NAME, customName);

                List<ItemStack> skiddedItems = getItemsForPlayer(profile.id());
                int count = skiddedItems.size();
                if (count > 0) {
                    net.minecraft.component.type.LoreComponent lore = new net.minecraft.component.type.LoreComponent(
                        List.of(Text.literal(count + " item(s) skidded").formatted(net.minecraft.util.Formatting.GRAY))
                    );
                    stack.set((ComponentType)DataComponentTypes.LORE, lore);
                }
            }
        } catch (Throwable t) {
            LOGGER.error("Failed to set profile to stack", t);
        }
    }
}
