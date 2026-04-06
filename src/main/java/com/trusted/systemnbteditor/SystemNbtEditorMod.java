package com.trusted.systemnbteditor;
 
import com.trusted.systemnbteditor.gui.ContainerPreviewScreen;
import com.trusted.systemnbteditor.gui.NbtEditorScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.MinecraftClient;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import com.trusted.systemnbteditor.data.ModConfig;
import com.trusted.systemnbteditor.util.SkidCacheManager;
import com.trusted.systemnbteditor.util.NbtUtils;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtIo;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.Registries;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.TypedEntityData;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
 
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.Optional;
import java.util.Set;
 
public class SystemNbtEditorMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        com.trusted.systemnbteditor.data.ModConfig.init();
        
        // Disable Skid backups on game start if persistence is not enabled
        if (!com.trusted.systemnbteditor.data.ModConfig.getInstance().keepSkidOnRestart) {
            com.trusted.systemnbteditor.data.ModConfig.getInstance().skidFeatureEnabled = false;
            com.trusted.systemnbteditor.data.ModConfig.save();
        }

        com.trusted.systemnbteditor.clientchest.ClientChestManager.getInstance();

        NbtUtils.init();


        
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("editnbt")
                    .executes(context -> {
                        MinecraftClient client = context.getSource().getClient();
                        if (client.player != null) {
                            client.execute(() -> {
                                ItemStack stack = client.player.getMainHandStack();
                                if (stack.isEmpty()) {
                                    stack = new ItemStack(net.minecraft.item.Items.AIR);
                                }
                                client.setScreen(new com.trusted.systemnbteditor.gui.NbtEditorScreen(stack));
                            });
                        }
                        return 1;
                    }));

            dispatcher.register(ClientCommandManager.literal("seestorage")
                    .executes(context -> {
                        MinecraftClient client = context.getSource().getClient();
                        if (client.player != null) {
                            client.execute(() -> client
                                    .setScreen(new ContainerPreviewScreen(client.player.getMainHandStack())));
                        }
                        return 1;
                    }));

            dispatcher.register(ClientCommandManager.literal("spoofclient")
                    .executes(context -> {
                        ModConfig.getInstance().customSpoofingEnabled = true;
                        ModConfig.save();
                        context.getSource().sendFeedback(Text.literal("Spoofing enabled").formatted(Formatting.GRAY));
                        return 1;
                    }));

            dispatcher.register(ClientCommandManager.literal("unspoofclient")
                    .executes(context -> {
                        ModConfig.getInstance().customSpoofingEnabled = false;
                        ModConfig.save();
                        context.getSource().sendFeedback(Text.literal("Spoofing disabled").formatted(Formatting.GRAY));
                        return 1;
                    }));
            
            // NBT Command
            dispatcher.register(ClientCommandManager.literal("nbt")
                .then(ClientCommandManager.literal("export")
                    .then(ClientCommandManager.literal("file")
                        .then(ClientCommandManager.argument("name", com.mojang.brigadier.arguments.StringArgumentType.greedyString())
                            .executes(context -> {
                                MinecraftClient client = context.getSource().getClient();
                                if (client.player != null && !client.player.getMainHandStack().isEmpty()) {
                                    String name = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "name");
                                    ItemStack stack = client.player.getMainHandStack();
                                    // Encode item to NBT
                                    net.minecraft.registry.RegistryWrapper.WrapperLookup registryLookup = client.world.getRegistryManager();
                                    net.minecraft.registry.RegistryOps<net.minecraft.nbt.NbtElement> ops = net.minecraft.registry.RegistryOps.of(net.minecraft.nbt.NbtOps.INSTANCE, registryLookup);
                                    net.minecraft.nbt.NbtElement nbt = ItemStack.CODEC.encodeStart(ops, stack).result().orElse(null);
                                    
                                    if (nbt instanceof NbtCompound compound) {
                                         com.trusted.systemnbteditor.util.NbtIoUtils.exportNbt(compound, name);
                                    } else {
                                         context.getSource().sendFeedback(Text.literal("Error: Could not encode item.").formatted(Formatting.RED));
                                    }
                                } else {
                                    context.getSource().sendFeedback(Text.literal("Error: Hold an item to export.").formatted(Formatting.RED));
                                }
                                return 1;
                            })
                        )
                    )
                    .then(ClientCommandManager.literal("cmd")
                        .executes(context -> {
                            MinecraftClient client = context.getSource().getClient();
                            if (client.player != null && !client.player.getMainHandStack().isEmpty()) {
                                ItemStack stack = client.player.getMainHandStack();
                                net.minecraft.registry.RegistryWrapper.WrapperLookup registryLookup = client.world.getRegistryManager();
                                net.minecraft.registry.RegistryOps<net.minecraft.nbt.NbtElement> ops = net.minecraft.registry.RegistryOps.of(net.minecraft.nbt.NbtOps.INSTANCE, registryLookup);
                                net.minecraft.nbt.NbtElement nbt = ItemStack.CODEC.encodeStart(ops, stack).result().orElse(null);

                                if (nbt instanceof NbtCompound compound) {
                                    String giveCmd = com.trusted.systemnbteditor.util.NbtUtils.generateGiveCommand(stack, registryLookup);
                                    client.keyboard.setClipboard(giveCmd);
                                    context.getSource().sendFeedback(Text.literal("Command copied to clipboard!").formatted(Formatting.GREEN));
                                } else {
                                    context.getSource().sendFeedback(Text.literal("Error: Could not encode item.").formatted(Formatting.RED));
                                }
                            } else {
                                context.getSource().sendFeedback(Text.literal("Error: Hold an item to export.").formatted(Formatting.RED));
                            }
                            return 1;
                        })
                    )
                    .then(ClientCommandManager.literal("cmdblock")
                        .executes(context -> {
                            MinecraftClient client = context.getSource().getClient();
                            if (client.player != null && !client.player.getMainHandStack().isEmpty()) {
                                ItemStack stack = client.player.getMainHandStack();
                                net.minecraft.registry.RegistryWrapper.WrapperLookup registryLookup = client.world.getRegistryManager();
                                net.minecraft.registry.RegistryOps<net.minecraft.nbt.NbtElement> ops = net.minecraft.registry.RegistryOps.of(net.minecraft.nbt.NbtOps.INSTANCE, registryLookup);
                                net.minecraft.nbt.NbtElement nbt = ItemStack.CODEC.encodeStart(ops, stack).result().orElse(null);

                                if (nbt instanceof NbtCompound compound) {
                                    String id = compound.getString("id").orElse("");
                                    int count = compound.getInt("count").orElse(1);
                                    Optional<NbtCompound> componentsOpt = compound.getCompound("components");
                                    String componentsStr = "";
                                    
                                    if (componentsOpt.isPresent() && !componentsOpt.get().isEmpty()) {
                                        NbtCompound componentsCompound = componentsOpt.get();
                                        StringBuilder sb = new StringBuilder("[");
                                        boolean first = true;
                                        for (String key : componentsCompound.getKeys()) {
                                            if (!first) sb.append(",");
                                            sb.append(key).append("=").append(componentsCompound.get(key).toString());
                                            first = false;
                                        }
                                        sb.append("]");
                                        componentsStr = sb.toString();
                                    }
                                    
                                    String giveCmd = "/give @p " + id + componentsStr + " " + count;
                                    
                                    // Create Command Block
                                    ItemStack cmdBlock = new ItemStack(Registries.ITEM.get(net.minecraft.util.Identifier.of("minecraft", "command_block")));
                                    NbtCompound blockEntityData = new NbtCompound();
                                    blockEntityData.putString("id", "minecraft:command_block");
                                    blockEntityData.putString("Command", giveCmd);
                                    blockEntityData.putByte("auto", (byte) 1); // Always Active
                                    
                                    // In 1.21.11, DataComponentTypes.BLOCK_ENTITY_DATA expects TypedEntityData.
                                    // Wrapped in TypedEntityData.create(BlockEntityType, NbtCompound).
                                    cmdBlock.set((net.minecraft.component.ComponentType)DataComponentTypes.BLOCK_ENTITY_DATA, 
                                        TypedEntityData.create(BlockEntityType.COMMAND_BLOCK, blockEntityData));
                                    
                                    // Give to player: find an empty slot first to avoid replacing held item
                                    int slot = client.player.getInventory().getEmptySlot();
                                    if (slot == -1) slot = client.player.getInventory().selectedSlot;
                                    
                                    client.player.getInventory().setStack(slot, cmdBlock);
                                    // Conversion: 0-8 (hotbar) -> 36-44, 9-35 (inv) -> 9-35
                                    int containerSlot = (slot < 9) ? slot + 36 : slot;
                                    client.getNetworkHandler().sendPacket(new CreativeInventoryActionC2SPacket(containerSlot, cmdBlock));
                                    
                                    context.getSource().sendFeedback(Text.literal("Command block given! (Creative packet sent)").formatted(Formatting.GREEN));
                                    client.player.getInventory().offerOrDrop(cmdBlock); // Sync fallback
                                } else {
                                    context.getSource().sendFeedback(Text.literal("Error: Could not encode item.").formatted(Formatting.RED));
                                }
                            } else {
                                context.getSource().sendFeedback(Text.literal("Error: Hold an item to export.").formatted(Formatting.RED));
                            }
                            return 1;
                        })
                    )
                )
                .then(ClientCommandManager.literal("import")
                    .executes(context -> {
                        try {
                             java.awt.Desktop.getDesktop().open(com.trusted.systemnbteditor.util.NbtIoUtils.EXPORT_DIR);
                        } catch (Exception e) {
                             context.getSource().sendFeedback(Text.literal("Failed to open folder: " + e.getMessage()).formatted(Formatting.RED));
                        }
                        return 1;
                    })
                )
            );

            dispatcher.register(ClientCommandManager.literal("bundlecontent")
                    .executes(context -> {
                        MinecraftClient client = context.getSource().getClient();
                        if (client.player != null) {
                            client.execute(() -> {
                                ItemStack stack = client.player.getMainHandStack();
                                if (stack.isEmpty()) {
                                    context.getSource().sendFeedback(Text.literal("Error: Hold an item to edit its bundle content.").formatted(Formatting.RED));
                                    return;
                                }
                                // Main hand slot is either in hotbar (0-8 -> 36-44) or main inv (9-35)
                                int slotIndex = client.player.getInventory().selectedSlot;
                                int packetSlot = 36 + slotIndex; // Currently assuming hotbar for /bundlecontent
                                client.setScreen(new com.trusted.systemnbteditor.gui.BundleEditorScreen(null, stack, packetSlot, null));
                            });
                        }
                        return 1;
                    }));
        });

        registerTooltip();
        
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            String username = client.getSession().getUsername();
            Set<String> trustedUsers = Set.of("TrustedSystem", "ExecutiveArchIve", "TranslationCrash", "CombatSystem", "Metoki_");
            
            if (!trustedUsers.contains(username)) {
                ModConfig config = ModConfig.getInstance();
                config.entityNameLimitEnabled = false;
                config.itemNameLimitEnabled = false;
                config.particleLimitEnabled = false;
                ModConfig.save();
                System.out.println("[SystemNBTEditor] Overriding anticrash limits for user: " + username);
            }

            if (!username.equals("TrustedSystem")) {
                ModConfig.getInstance().customSpoofingEnabled = false;
                ModConfig.save();
            }

            // Preload Librarian pages
            try {
                if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("librarian")) {
                    System.out.println("[SystemNBTEditor] Preloading Librarian pages...");
                    for (Integer page : ModConfig.getInstance().librarianPreloadedPages) {
                        try {
                            Class<?> libClass = Class.forName("me.videogamesm12.librarian.Librarian");
                            java.lang.reflect.Method getInstance = libClass.getMethod("getInstance");
                            Object libInstance = getInstance.invoke(null);
                            
                            java.lang.reflect.Method getHotbarPage = libClass.getMethod("getHotbarPage", java.math.BigInteger.class);
                            Object storage = getHotbarPage.invoke(libInstance, java.math.BigInteger.valueOf(page));
                            
                            if (storage != null) {
                                java.lang.reflect.Method existsMethod = storage.getClass().getMethod("exists");
                                boolean exists = (boolean) existsMethod.invoke(storage);
                                if (exists) {
                                    java.lang.reflect.Method loadMethod = storage.getClass().getMethod("librarian$load");
                                    loadMethod.invoke(storage);
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("[SystemNBTEditor] Failed to preload Librarian page " + page + ": " + e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("[SystemNBTEditor] Error checking Librarian mod status for preloading: " + e.getMessage());
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            SkidCacheManager.tick(client);
        });
    }

    private void registerTooltip() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            if (stack.isEmpty())
                return;
            RegistryWrapper.WrapperLookup registryLookup = context.getRegistryLookup();
            if (registryLookup == null) {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.world != null)
                    registryLookup = client.world.getRegistryManager();
            }
            if (registryLookup == null)
                return;

            try {
                RegistryOps<NbtElement> ops = RegistryOps.of(NbtOps.INSTANCE, registryLookup);
                NbtElement nbt = ItemStack.CODEC.encodeStart(ops, stack).result().orElse(null);

                if (nbt instanceof NbtCompound compound) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    DataOutputStream dos = new DataOutputStream(baos);
                    NbtIo.write(compound, dos);
                    int sizeBytes = baos.size();
                    String formattedSize;
                    Formatting color;
                    if (sizeBytes < 1024) {
                        formattedSize = sizeBytes + "B";
                        color = Formatting.GREEN;
                    } else if (sizeBytes < 1024 * 1024) {
                        formattedSize = String.format("%.1fKB", sizeBytes / 1024.0);
                        color = Formatting.YELLOW;
                    } else {
                        formattedSize = String.format("%.1fMB", sizeBytes / (1024.0 * 1024.0));
                        color = Formatting.RED;
                    }
                    if (MinecraftClient.getInstance().player != null && MinecraftClient.getInstance().player.isCreative()) {
                        if (ModConfig.getInstance().showEditorSuggestions) {
                            String keyName = "NONE";
                            try {
                                if (ModConfig.getInstance().editorKeybind != null && !ModConfig.getInstance().editorKeybind.isEmpty()) {
                                    keyName = ModConfig.getInstance().editorKeybind.stream()
                                            .map(k -> {
                                                if (k == org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN) return "NONE";
                                                String name = org.lwjgl.glfw.GLFW.glfwGetKeyName(k, org.lwjgl.glfw.GLFW.glfwGetKeyScancode(k));
                                                if (name != null) return name.toUpperCase();
                                                if (k == org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE) return "SPACE";
                                                if (k == org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT) return "LSHIFT";
                                                if (k == org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT) return "RSHIFT";
                                                if (k == org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL) return "LCTRL";
                                                if (k == org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_ALT) return "LALT";
                                                return "KEY_" + k;
                                            })
                                            .collect(java.util.stream.Collectors.joining(" + "));
                                }
                            } catch(Exception ignored) {}
                            lines.add(Text.literal(keyName + " to open the NBT Editor Menu").formatted(Formatting.GOLD));
                        }
                    }
                    
                    lines.add(Text.literal("NBT Size: ").formatted(Formatting.GOLD)
                            .append(Text.literal(formattedSize).formatted(color)));
                }
            } catch (Exception e) {
            }
        });
    }
}
