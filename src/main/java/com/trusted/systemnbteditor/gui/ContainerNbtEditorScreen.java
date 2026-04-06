package com.trusted.systemnbteditor.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.TypedEntityData;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Formatting;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

public class ContainerNbtEditorScreen extends GenericContainerScreen {

    private enum ContainerMode { NATURAL, SINGLE, BUNDLE, FRAME, SPAWN_EGG }

    private static final ItemStack RED_PANE;
    static {
        RED_PANE = new ItemStack(Items.RED_STAINED_GLASS_PANE);
        RED_PANE.set(DataComponentTypes.CUSTOM_NAME, Text.literal(" "));
        RED_PANE.set(DataComponentTypes.TOOLTIP_DISPLAY,
                new TooltipDisplayComponent(true, new LinkedHashSet<>()));
    }

    private static final Identifier[] EQUIPMENT_TEXTURES = new Identifier[] {
            Identifier.of("minecraft", "container/slot/helmet"),
            Identifier.of("minecraft", "container/slot/chestplate"),
            Identifier.of("minecraft", "container/slot/leggings"),
            Identifier.of("minecraft", "container/slot/boots"),
            Identifier.of("minecraft", "container/slot/saddle"),
            Identifier.of("minecraft", "container/slot/horse_armor"),
            Identifier.of("minecraft", "container/slot/sword"),
            Identifier.of("minecraft", "container/slot/shield")
    };

    private final net.minecraft.client.gui.screen.Screen parent;
    private ItemStack stack;
    private final ItemStack originalStack;
    private final int slotId;
    private final Inventory containerInventory;
    private final java.util.function.Consumer<ItemStack> saveCallback;

    private final ContainerMode mode;
    private final int allowedSlotCount;
    private final Identifier[] slotTextures;
    private final boolean isBrushable;
    private final boolean isItemFrame;
    private final boolean isSpawnEgg;
    private final String spawnEntityId;
    private boolean discardChanges;
    private static boolean autosaveOnExit = true;
    private ButtonWidget autosaveToggleButton;
    private ButtonWidget primaryActionButton;

    public ContainerNbtEditorScreen(net.minecraft.client.gui.screen.Screen parent, ItemStack stack, int slotId) {
        this(parent, stack, slotId, null);
    }

    public ContainerNbtEditorScreen(net.minecraft.client.gui.screen.Screen parent, ItemStack stack, int slotId,
                                    java.util.function.Consumer<ItemStack> saveCallback) {
        super(new ContainerScreenHandler(3,
                      net.minecraft.client.MinecraftClient.getInstance().player.getInventory()),
              net.minecraft.client.MinecraftClient.getInstance().player.getInventory(),
              Text.of("Container Editor"));
        this.parent = parent;
        this.stack = stack;
        this.originalStack = stack.copy();
        this.slotId = slotId;
        this.saveCallback = saveCallback;
        this.containerInventory = this.handler.getInventory();

        this.mode = detectMode(stack);
        this.isSpawnEgg = this.mode == ContainerMode.SPAWN_EGG;
        this.spawnEntityId = this.isSpawnEgg ? resolveSpawnEntityId(stack) : "";

        int extraSpawnSlots = this.isSpawnEgg ? getSpawnAdditionalSlots(this.spawnEntityId) : 0;
        this.allowedSlotCount = this.isSpawnEgg ? (8 + extraSpawnSlots) : getVanillaSlotCount(stack);
        this.slotTextures = buildSlotTextures(this.mode, this.allowedSlotCount);
        ((ContainerScreenHandler) this.handler).setSlotTextures(this.slotTextures);

        this.isBrushable = isBrushableBlock(stack);
        this.isItemFrame = isItemFrameItem(stack);
        this.discardChanges = false;

        loadItems();
        fillPanes();
    }

    private static ContainerMode detectMode(ItemStack stack) {
        if (stack == null) return ContainerMode.SINGLE;
        String id = Registries.ITEM.getId(stack.getItem()).getPath().toLowerCase();

        if (id.contains("bundle") || stack.contains(DataComponentTypes.BUNDLE_CONTENTS)) return ContainerMode.BUNDLE;
        if (id.contains("item_frame")) return ContainerMode.FRAME;
        if (id.endsWith("_spawn_egg")) return ContainerMode.SPAWN_EGG;

        if (id.contains("shulker") || id.equals("chest") || id.contains("barrel")
                || id.contains("copper_chest") || id.contains("dispenser") || id.contains("dropper")
                || id.contains("hopper") || id.contains("furnace")
                || id.contains("blast_furnace") || id.contains("smoker")
                || id.contains("brewing_stand") || id.contains("crafter")
                || id.contains("chiseled_bookshelf") || id.contains("shelf")
                || id.contains("campfire") || id.contains("jukebox") || id.contains("lectern")) return ContainerMode.NATURAL;

        return ContainerMode.SINGLE;
    }

    private static int getVanillaSlotCount(ItemStack stack) {
        if (stack == null) return 1;
        String id = Registries.ITEM.getId(stack.getItem()).getPath().toLowerCase();

        if (id.contains("bundle")) return 27;
        if (id.contains("shulker") || id.equals("chest") || id.contains("barrel") || id.equals("trapped_chest") || id.contains("copper_chest")) return 27;
        if (id.contains("dispenser") || id.contains("dropper") || id.contains("crafter")) return 9;
        if (id.contains("chiseled_bookshelf")) return 6;
        if (id.contains("hopper") || id.contains("brewing_stand")) return 5;
        if (id.contains("campfire")) return 4;
        if (id.contains("furnace") || id.contains("blast_furnace") || id.contains("smoker") || id.contains("shelf")) return 3;
        if (id.contains("jukebox") || id.contains("decorated_pot") || id.contains("suspicious") || id.contains("lectern")) return 1;
        if (id.contains("item_frame")) return 1;
        return 1;
    }

    private static boolean isBrushableBlock(ItemStack stack) {
        String id = Registries.ITEM.getId(stack.getItem()).getPath().toLowerCase();
        return id.equals("suspicious_sand") || id.equals("suspicious_gravel");
    }

    private static boolean isItemFrameItem(ItemStack stack) {
        String id = Registries.ITEM.getId(stack.getItem()).getPath().toLowerCase();
        return id.contains("item_frame");
    }

    private static Identifier[] buildSlotTextures(ContainerMode mode, int allowedSlots) {
        Identifier[] textures = new Identifier[allowedSlots];
        if (mode == ContainerMode.SPAWN_EGG) {
            int max = Math.min(EQUIPMENT_TEXTURES.length, allowedSlots);
            System.arraycopy(EQUIPMENT_TEXTURES, 0, textures, 0, max);
        }
        return textures;
    }

    private String resolveSpawnEntityId(ItemStack stack) {
        String fromData = readEntityIdFromEntityData();
        if (fromData != null && !fromData.isEmpty()) return fromData;

        Identifier itemId = Registries.ITEM.getId(stack.getItem());
        String path = itemId.getPath();
        if (path.endsWith("_spawn_egg")) {
            return itemId.getNamespace() + ":" + path.substring(0, path.length() - "_spawn_egg".length());
        }
        return "minecraft:pig";
    }

    private String readEntityIdFromEntityData() {
        try {
            Object entityData = stack.get(DataComponentTypes.ENTITY_DATA);
            if (entityData == null) return null;
            NbtCompound nbt = null;
            if (entityData instanceof TypedEntityData<?> ted) nbt = ted.copyNbtWithoutId();
            else if (entityData instanceof NbtComponent comp) nbt = comp.copyNbt();
            if (nbt != null && nbt.contains("id")) return nbt.getString("id").orElse(null);
        } catch (Exception ignored) {}
        return null;
    }

    private static int getSpawnAdditionalSlots(String entityId) {
        if ("minecraft:villager".equals(entityId)) return 8;
        if ("minecraft:allay".equals(entityId)) return 1;
        return 0;
    }

    private void fillPanes() {
        for (int i = allowedSlotCount; i < 27; i++) containerInventory.setStack(i, RED_PANE.copy());
    }

    private void loadItems() {
        containerInventory.clear();

        if (mode == ContainerMode.BUNDLE) {
            BundleContentsComponent bundle = stack.get(DataComponentTypes.BUNDLE_CONTENTS);
            if (bundle != null) {
                int i = 0;
                for (ItemStack s : bundle.stream().toList()) {
                    if (i >= 27) break;
                    containerInventory.setStack(i++, s.copy());
                }
            }
            return;
        }

        if (mode == ContainerMode.FRAME) {
            loadFromEntityData();
            return;
        }

        if (mode == ContainerMode.SPAWN_EGG) {
            loadFromSpawnEggEntityData();
            return;
        }

        if (mode == ContainerMode.SINGLE || mode == ContainerMode.NATURAL) {
            String id = Registries.ITEM.getId(stack.getItem()).getPath();
            if (isBrushable || id.contains("jukebox") || id.contains("lectern")) {
                loadFromSpecialBlockEntity();
                return;
            }
        }

        ContainerComponent container = stack.get(DataComponentTypes.CONTAINER);
        if (container != null) {
            int i = 0;
            for (ItemStack s : container.stream().toList()) {
                if (i >= allowedSlotCount) break;
                containerInventory.setStack(i++, s.copy());
            }
        }
    }
    private void loadFromSpawnEggEntityData() {
        try {
            NbtCompound nbt = getEditableEntityDataNbt();
            if (nbt == null) return;

            if (nbt.contains("equipment")) {
                NbtCompound equipment = nbt.getCompound("equipment").orElse(new NbtCompound());
                setIfPresent(containerInventory, 0, equipment, "head");
                setIfPresent(containerInventory, 1, equipment, "chest");
                setIfPresent(containerInventory, 2, equipment, "legs");
                setIfPresent(containerInventory, 3, equipment, "boots");
                setIfPresent(containerInventory, 4, equipment, "saddle");
                setIfPresent(containerInventory, 5, equipment, "body");
                setIfPresent(containerInventory, 6, equipment, "mainhand");
                setIfPresent(containerInventory, 7, equipment, "offhand");
            } else {
                NbtList armor = nbt.getList("ArmorItems").orElse(new NbtList());
                for (int i = 0; i < Math.min(4, armor.size()); i++) {
                    containerInventory.setStack(3 - i, decodeStack(armor.getCompound(i).orElse(new NbtCompound())));
                }

                NbtList hand = nbt.getList("HandItems").orElse(new NbtList());
                for (int i = 0; i < Math.min(2, hand.size()); i++) {
                    containerInventory.setStack(6 + i, decodeStack(hand.getCompound(i).orElse(new NbtCompound())));
                }

                if (nbt.contains("SaddleItem")) containerInventory.setStack(4, decodeStack(nbt.getCompound("SaddleItem").orElse(new NbtCompound())));
                String bodyKey = nbt.contains("body_armor_item") ? "body_armor_item" : (nbt.contains("ArmorItem") ? "ArmorItem" : (nbt.contains("DecorItem") ? "DecorItem" : null));
                if (bodyKey != null) containerInventory.setStack(5, decodeStack(nbt.getCompound(bodyKey).orElse(new NbtCompound())));
            }

            int extraSlots = allowedSlotCount - 8;
            if (extraSlots > 0 && nbt.contains("Inventory")) {
                NbtList inv = nbt.getList("Inventory").orElse(new NbtList());
                for (int i = 0; i < Math.min(extraSlots, inv.size()); i++) {
                    containerInventory.setStack(8 + i, decodeStack(inv.getCompound(i).orElse(new NbtCompound())));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private void loadFromSpecialBlockEntity() {
        try {
            TypedEntityData<?> bed = stack.get(DataComponentTypes.BLOCK_ENTITY_DATA);
            if (bed == null) return;
            NbtCompound nbt = bed.copyNbtWithoutId();
            String id = Registries.ITEM.getId(stack.getItem()).getPath();
            String key = isBrushable ? "item" : "RecordItem";
            if (id.contains("lectern")) key = "Book";
            
            if (!nbt.contains(key)) return;

            RegistryWrapper.WrapperLookup lookup = net.minecraft.client.MinecraftClient.getInstance().world.getRegistryManager();
            RegistryOps<NbtElement> ops = lookup.getOps(NbtOps.INSTANCE);
            NbtCompound itemNbt = nbt.getCompound(key).orElse(new NbtCompound());
            containerInventory.setStack(0, ItemStack.CODEC.parse(ops, itemNbt).result().orElse(ItemStack.EMPTY));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private void loadFromEntityData() {
        try {
            Object entityData = stack.get(DataComponentTypes.ENTITY_DATA);
            if (entityData == null) return;
            NbtCompound nbt = null;
            if (entityData instanceof TypedEntityData<?> ted) nbt = ted.copyNbtWithoutId();
            else if (entityData instanceof NbtComponent comp) nbt = comp.copyNbt();
            if (nbt == null) return;

            RegistryWrapper.WrapperLookup lookup = net.minecraft.client.MinecraftClient.getInstance().world.getRegistryManager();
            RegistryOps<NbtElement> ops = lookup.getOps(NbtOps.INSTANCE);
            String itemKey = nbt.contains("Item") ? "Item" : (nbt.contains("item") ? "item" : null);
            if (itemKey == null) return;
            NbtCompound itemNbt = nbt.getCompound(itemKey).orElse(new NbtCompound());
            if (itemNbt.contains("item")) {
                NbtCompound nested = itemNbt.getCompound("item").orElse(null);
                if (nested != null) itemNbt = nested;
            }
            containerInventory.setStack(0, ItemStack.CODEC.parse(ops, itemNbt).result().orElse(ItemStack.EMPTY));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void save() { save(false); }

    private void save(boolean isClosing) {
        ItemStack newStack = stack.copy();

        if (mode == ContainerMode.BUNDLE) {
            List<ItemStack> items = new ArrayList<>();
            for (int i = 0; i < 27; i++) items.add(containerInventory.getStack(i).copy());
            newStack.set(DataComponentTypes.BUNDLE_CONTENTS, new BundleContentsComponent(items.stream().filter(s -> !s.isEmpty()).collect(Collectors.toList())));

        } else if (mode == ContainerMode.FRAME) {
            newStack = saveToEntityData(newStack, containerInventory.getStack(0).copy());
            newStack.remove(DataComponentTypes.CONTAINER);

        } else if (mode == ContainerMode.SPAWN_EGG) {
            newStack = saveSpawnEggEntityData(newStack);
            newStack.remove(DataComponentTypes.CONTAINER);

        } else if ((mode == ContainerMode.SINGLE || mode == ContainerMode.NATURAL)
                && (isBrushable || Registries.ITEM.getId(stack.getItem()).getPath().contains("jukebox") || Registries.ITEM.getId(stack.getItem()).getPath().contains("lectern"))) {
            newStack = saveToSpecialBlockEntity(newStack, containerInventory.getStack(0).copy());
            newStack.remove(DataComponentTypes.CONTAINER);

        } else if (mode == ContainerMode.NATURAL || mode == ContainerMode.SINGLE) {
            List<ItemStack> items = new ArrayList<>();
            for (int i = 0; i < allowedSlotCount; i++) items.add(containerInventory.getStack(i).copy());
            newStack.set(DataComponentTypes.CONTAINER, ContainerComponent.fromStacks(items));
        }

        this.stack = newStack;
        applyToGame(newStack, isClosing);
    }

    private ItemStack saveSpawnEggEntityData(ItemStack base) {
        try {
            TypedEntityData<?> existing = base.get(DataComponentTypes.ENTITY_DATA);
            NbtCompound nbt = (existing != null) ? existing.copyNbtWithoutId() : new NbtCompound();
            if (!nbt.contains("id")) nbt.putString("id", spawnEntityId);

            NbtCompound equipment = new NbtCompound();
            putIfNotEmpty(equipment, "head", containerInventory.getStack(0));
            putIfNotEmpty(equipment, "chest", containerInventory.getStack(1));
            putIfNotEmpty(equipment, "legs", containerInventory.getStack(2));
            putIfNotEmpty(equipment, "boots", containerInventory.getStack(3));
            putIfNotEmpty(equipment, "saddle", containerInventory.getStack(4));
            putIfNotEmpty(equipment, "body", containerInventory.getStack(5));
            putIfNotEmpty(equipment, "mainhand", containerInventory.getStack(6));
            putIfNotEmpty(equipment, "offhand", containerInventory.getStack(7));
            if (!equipment.isEmpty()) nbt.put("equipment", equipment); else nbt.remove("equipment");

            NbtList armor = new NbtList();
            armor.add(encodeStack(containerInventory.getStack(3)));
            armor.add(encodeStack(containerInventory.getStack(2)));
            armor.add(encodeStack(containerInventory.getStack(1)));
            armor.add(encodeStack(containerInventory.getStack(0)));
            nbt.put("ArmorItems", armor);

            NbtList hand = new NbtList();
            hand.add(encodeStack(containerInventory.getStack(6)));
            hand.add(encodeStack(containerInventory.getStack(7)));
            nbt.put("HandItems", hand);

            ItemStack saddle = containerInventory.getStack(4);
            if (!saddle.isEmpty()) nbt.put("SaddleItem", encodeStack(saddle)); else nbt.remove("SaddleItem");

            ItemStack body = containerInventory.getStack(5);
            if (!body.isEmpty()) {
                NbtElement bodyNbt = encodeStack(body);
                nbt.put("body_armor_item", bodyNbt);
                nbt.put("ArmorItem", bodyNbt.copy());
                nbt.put("DecorItem", bodyNbt.copy());
            } else {
                nbt.remove("body_armor_item");
                nbt.remove("ArmorItem");
                nbt.remove("DecorItem");
            }

            int extraSlots = allowedSlotCount - 8;
            if (extraSlots > 0) {
                NbtList inv = new NbtList();
                for (int i = 0; i < extraSlots; i++) inv.add(encodeStack(containerInventory.getStack(8 + i)));
                if (hasAnyItemInRange(8, 8 + extraSlots)) nbt.put("Inventory", inv); else nbt.remove("Inventory");
            } else {
                nbt.remove("Inventory");
            }

            EntityType<?> type = resolveSpawnEntityType(existing);
            base.set(DataComponentTypes.ENTITY_DATA, TypedEntityData.create(type, nbt));
            return base;
        } catch (Exception e) {
            e.printStackTrace();
            return base;
        }
    }
    @SuppressWarnings("unchecked")
    private ItemStack saveToEntityData(ItemStack base, ItemStack embedded) {
        try {
            TypedEntityData<?> existing = base.get(DataComponentTypes.ENTITY_DATA);
            NbtCompound nbt = (existing != null) ? existing.copyNbtWithoutId() : new NbtCompound();

            String id = Registries.ITEM.getId(base.getItem()).toString();
            nbt.putString("id", id);
            String itemKey = nbt.contains("Item") ? "Item" : (nbt.contains("item") ? "item" : "Item");
            String altItemKey = itemKey.equals("Item") ? "item" : "Item";

            if (!embedded.isEmpty()) {
                RegistryWrapper.WrapperLookup lookup = net.minecraft.client.MinecraftClient.getInstance().world.getRegistryManager();
                RegistryOps<NbtElement> ops = lookup.getOps(NbtOps.INSTANCE);
                NbtElement itemNbt = ItemStack.CODEC.encodeStart(ops, embedded).result().orElse(null);
                if (itemNbt != null) nbt.put(itemKey, itemNbt);
                nbt.remove(altItemKey);
            } else {
                nbt.remove("Item");
                nbt.remove("item");
            }

            EntityType<?> type = (existing != null) ? (EntityType<?>) existing.getType() : (id.contains("glow") ? EntityType.GLOW_ITEM_FRAME : EntityType.ITEM_FRAME);
            base.set(DataComponentTypes.ENTITY_DATA, TypedEntityData.create(type, nbt));
            return base;
        } catch (Exception e) {
            e.printStackTrace();
            return base;
        }
    }

    @SuppressWarnings("unchecked")
    private ItemStack saveToSpecialBlockEntity(ItemStack base, ItemStack embedded) {
        try {
            TypedEntityData<?> existing = base.get(DataComponentTypes.BLOCK_ENTITY_DATA);
            NbtCompound nbt = (existing != null) ? existing.copyNbtWithoutId() : new NbtCompound();
            String id = Registries.ITEM.getId(base.getItem()).getPath();
            String key = id.contains("suspicious") ? "item" : "RecordItem";
            if (id.contains("lectern")) {
                key = "Book";
                if (!nbt.contains("id")) nbt.putString("id", "minecraft:lectern");
                if (!nbt.contains("Page")) nbt.putInt("Page", -1);
            }

            if (!embedded.isEmpty()) {
                RegistryWrapper.WrapperLookup lookup = net.minecraft.client.MinecraftClient.getInstance().world.getRegistryManager();
                RegistryOps<NbtElement> ops = lookup.getOps(NbtOps.INSTANCE);
                NbtElement itemNbt = ItemStack.CODEC.encodeStart(ops, embedded).result().orElse(null);
                if (itemNbt != null) nbt.put(key, itemNbt);
            } else {
                nbt.remove(key);
            }
            
            BlockEntityType<?> type = null;
            if (existing != null) {
                try { type = (BlockEntityType<?>) existing.getType(); } catch (Exception ignored) {}
            }
            if (type == null) {
                if (id.contains("lectern")) type = BlockEntityType.LECTERN;
                else if (id.contains("jukebox")) type = BlockEntityType.JUKEBOX;
                else if (id.contains("suspicious")) type = BlockEntityType.BRUSHABLE_BLOCK;
            }
            base.set(DataComponentTypes.BLOCK_ENTITY_DATA, TypedEntityData.create(type, nbt));
            return base;
        } catch (Exception e) {
            e.printStackTrace();
            return base;
        }
    }

    private void applyToGame(ItemStack newStack, boolean isClosing) {
        if (parent instanceof NbtSelectionScreen sel) sel.refresh(newStack);
        else if (parent instanceof VisualNbtEditorScreen vis) vis.refresh(newStack);

        if (isClosing) {
            if (this.saveCallback != null) {
                this.saveCallback.accept(newStack);
                return;
            }
        } else if (this.saveCallback != null) {
            return;
        }

        int packetSlot = (this.slotId != -1) ? this.slotId : this.client.player.getInventory().selectedSlot + 36;
        updateClientInventory(packetSlot, newStack);
        if (this.client.getNetworkHandler() != null) this.client.getNetworkHandler().sendPacket(new CreativeInventoryActionC2SPacket(packetSlot, newStack));
    }

    private void updateClientInventory(int slot, ItemStack newStack) {
        if (this.client == null || this.client.player == null) return;
        if (slot >= 36 && slot <= 44) this.client.player.getInventory().setStack(slot - 36, newStack.copy());
        else if (slot >= 9 && slot <= 35) this.client.player.getInventory().setStack(slot, newStack.copy());
        else if (slot >= 5 && slot <= 8) this.client.player.getInventory().setStack(39 - (slot - 5), newStack.copy());
        else if (slot == 45) this.client.player.getInventory().setStack(40, newStack.copy());

        if (this.client.player.currentScreenHandler != null) {
            for (Slot s : this.client.player.currentScreenHandler.slots) if (s.id == slot) s.setStack(newStack.copy());
        }
    }

    @Override
    protected void onMouseClick(Slot slot, int invSlot, int button, SlotActionType actionType) {
        if (slot != null && slot.inventory == containerInventory && slot.getIndex() >= allowedSlotCount) return;

        if (slot != null && invSlot != -999) this.handler.onSlotClick(slot.id, button, actionType, this.client.player);
        else this.handler.onSlotClick(invSlot, button, actionType, this.client.player);

        fillPanes();
        save();
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.key();
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
                if (hovered.getIndex() >= allowedSlotCount) return true;
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
    protected void init() {
        super.init();
        autosaveToggleButton = this.addDrawableChild(ButtonWidget.builder(getAutosaveLabel(), button -> {
                    autosaveOnExit = !autosaveOnExit;
                    button.setMessage(getAutosaveLabel());
                    rebuildPrimaryActionButton();
                })
                .dimensions(this.width - 170, this.height - 48, 160, 20)
                .build());
        rebuildPrimaryActionButton();
    }

    private Text getAutosaveLabel() {
        return Text.literal("Autosave: " + (autosaveOnExit ? "ON" : "OFF"))
                .formatted(autosaveOnExit ? Formatting.GREEN : Formatting.DARK_RED);
    }

    private void rebuildPrimaryActionButton() {
        if (primaryActionButton != null) {
            this.remove(primaryActionButton);
        }
        if (autosaveOnExit) {
            primaryActionButton = this.addDrawableChild(
                    ButtonWidget.builder(Text.literal("Exit without Saving"), button -> exitWithoutSaving())
                            .dimensions(this.width - 170, this.height - 26, 160, 20)
                            .build()
            );
        } else {
            primaryActionButton = this.addDrawableChild(
                    ButtonWidget.builder(Text.literal("Save").formatted(Formatting.GREEN), button -> saveAndClose())
                            .dimensions(this.width - 170, this.height - 26, 160, 20)
                            .build()
            );
        }
    }

    private void saveAndClose() {
        save(true);
        if (this.client != null) this.client.setScreen(this.parent);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    public void close() {
        if (autosaveOnExit && !discardChanges) save(true);
        if (this.client != null) this.client.setScreen(this.parent);
    }

    private void exitWithoutSaving() {
        this.discardChanges = true;
        applyToGame(this.originalStack.copy(), true);
        if (this.client != null) this.client.setScreen(this.parent);
    }

    private NbtCompound getEditableEntityDataNbt() {
        Object entityData = stack.get(DataComponentTypes.ENTITY_DATA);
        if (entityData == null) return null;
        if (entityData instanceof TypedEntityData<?> ted) return ted.copyNbtWithoutId();
        if (entityData instanceof NbtComponent comp) return comp.copyNbt();
        return null;
    }

    private void setIfPresent(Inventory inv, int slot, NbtCompound source, String key) {
        if (!source.contains(key)) return;
        inv.setStack(slot, decodeStack(source.getCompound(key).orElse(new NbtCompound())));
    }

    private ItemStack decodeStack(NbtCompound itemNbt) {
        try {
            RegistryWrapper.WrapperLookup lookup = net.minecraft.client.MinecraftClient.getInstance().world.getRegistryManager();
            RegistryOps<NbtElement> ops = lookup.getOps(NbtOps.INSTANCE);
            return ItemStack.CODEC.parse(ops, itemNbt).result().orElse(ItemStack.EMPTY);
        } catch (Exception ignored) {
            return ItemStack.EMPTY;
        }
    }

    private NbtElement encodeStack(ItemStack stack) {
        try {
            RegistryWrapper.WrapperLookup lookup = net.minecraft.client.MinecraftClient.getInstance().world.getRegistryManager();
            RegistryOps<NbtElement> ops = lookup.getOps(NbtOps.INSTANCE);
            return ItemStack.CODEC.encodeStart(ops, stack == null ? ItemStack.EMPTY : stack).result().orElse(new NbtCompound());
        } catch (Exception ignored) {
            return new NbtCompound();
        }
    }

    private void putIfNotEmpty(NbtCompound parent, String key, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        parent.put(key, encodeStack(stack));
    }

    private boolean hasAnyItemInRange(int fromInclusive, int toExclusive) {
        for (int i = fromInclusive; i < toExclusive; i++) {
            ItemStack stack = containerInventory.getStack(i);
            if (stack != null && !stack.isEmpty()) return true;
        }
        return false;
    }

    private EntityType<?> resolveSpawnEntityType(TypedEntityData<?> existing) {
        if (existing != null) {
            try {
                return (EntityType<?>) existing.getType();
            } catch (Exception ignored) {}
        }
        try {
            if (spawnEntityId != null && !spawnEntityId.isBlank()) {
                String[] split = spawnEntityId.split(":", 2);
                Identifier id = split.length == 2 ? Identifier.of(split[0], split[1]) : Identifier.of("minecraft", split[0]);
                EntityType<?> type = Registries.ENTITY_TYPE.get(id);
                if (type != null) return type;
            }
        } catch (Exception ignored) {}
        return EntityType.PIG;
    }
}
