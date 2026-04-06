package com.trusted.systemnbteditor.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.registry.RegistryOps;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.TypedEntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.predicate.component.ComponentMapPredicate;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.MathHelper;
import net.minecraft.village.TradedItem;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class VillagerTradeEditorScreen extends Screen {
    private final Screen parent;
    private final ItemStack stack;
    private final int slotId;
    private final Consumer<ItemStack> saveCallback;

    private List<TradeOffer> trades = new ArrayList<>();
    private int selectedTradeIndex = -1;

    private static final int BG_WIDTH = 276;
    private static final int BG_HEIGHT = 166;

    private static final int BUY1_X = 136, BUY1_Y = 37;
    private static final int BUY2_X = 162, BUY2_Y = 37;
    private static final int SELL_X = 220, SELL_Y = 37;
    private static final int[] MAX_USES_PRESETS = {99, 100, 1000, Integer.MAX_VALUE};
    private static final int TITLE_COLOR = 0xFFFFFFFF; // White for Dark Mode
    private static final int LABEL_COLOR = 0xFFAAAAAA; // Light Grey

    // Primary texture: user-provided pack texture; fallback: vanilla texture.
    private static final Identifier GUI_PATH = Identifier.of("system_nbt_editor", "textures/gui/container/villager.png");
    private static final Identifier TRADE_ARROW = Identifier.of("system_nbt_editor", "textures/gui/sprites/container/villager/trade_arrow.png");
    private static final Identifier TRADE_ARROW_OUT_OF_STOCK = Identifier.of("system_nbt_editor", "textures/gui/sprites/container/villager/trade_arrow_out_of_stock.png");
    private static final Identifier GUI_PATH_FALLBACK = Identifier.of("minecraft", "textures/gui/container/villager.png");
    private static final Identifier TRADE_ARROW_FALLBACK = Identifier.of("minecraft", "textures/gui/sprites/container/villager/trade_arrow.png");

    private int villagerLevel = 1;
    private int villagerXp = 0;
    private String villagerProfession = "minecraft:none";
    private boolean villagerNoAi = false;
    private boolean villagerInvulnerable = false;
    private boolean villagerGlowing = false;

    private ButtonWidget professionButton;
    private ButtonWidget noAiButton;
    private ButtonWidget invulnerableButton;
    private ButtonWidget glowingButton;
    private ButtonWidget maxUsesMinusButton;
    private ButtonWidget maxUsesPlusButton;

    private static class TradeOffer {
        ItemStack buy = ItemStack.EMPTY;
        ItemStack buyB = ItemStack.EMPTY;
        ItemStack sell = ItemStack.EMPTY;
        int maxUses = 99;
        int uses = 0;
        int xp = 2;
        float priceMultiplier = 0.05f;

        boolean isEmpty() {
            return buy.isEmpty() && buyB.isEmpty() && sell.isEmpty();
        }
    }

    public VillagerTradeEditorScreen(Screen parent, ItemStack stack, int slotId, Consumer<ItemStack> saveCallback) {
        super(Text.literal("Villager Trade Editor"));
        this.parent = parent;
        this.stack = stack.copy();
        this.slotId = slotId;
        this.saveCallback = saveCallback;
    }

    @Override
    protected void init() {
        super.init();
        if (trades.isEmpty()) {
            loadFromStack();
        }

        int x = (this.width - BG_WIDTH) / 2;
        int y = (this.height - BG_HEIGHT) / 2;
        
        int btnW = 80;
        int btnH = 20;
        int padding = 5;
        
        // Save right down corner
        int saveX = this.width - btnW - padding;
        int saveY = this.height - btnH - padding;

        // Back centered bottom
        int backX = this.width / 2 - btnW / 2;
        int backY = this.height - btnH - padding;

        // Top-left toggle area
        int profX = padding;
        int profY = padding;
        int noaiX = padding;
        int noaiY = padding + btnH + padding;
        int invulX = padding;
        int invulY = noaiY + btnH + padding;
        int glowingX = padding;
        int glowingY = invulY + btnH + padding;

        // Add Button (+) left of trades
        this.addDrawableChild(ButtonWidget.builder(Text.literal("+").formatted(Formatting.GREEN), button -> {
            trades.add(new TradeOffer());
            selectedTradeIndex = trades.size() - 1;
        }).dimensions(x - 22, y + 17, 20, 20).build());

        // Level buttons (visible adjustment)
        this.addDrawableChild(ButtonWidget.builder(Text.literal("-"), button -> {
             villagerLevel = Math.max(1, villagerLevel - 1);
        }).dimensions(x + 220, y + 16, 12, 12).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("+"), button -> {
             villagerLevel = Math.min(5, villagerLevel + 1);
        }).dimensions(x + 235, y + 16, 12, 12).build());

        // Back and Save Buttons
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Back"), button -> close())
                .dimensions(backX, backY, btnW, btnH).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Save").formatted(Formatting.GREEN), button -> save())
                .dimensions(saveX, saveY, btnW, btnH).build());

        this.maxUsesMinusButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("-"), button -> {
            if (selectedTradeIndex >= 0 && selectedTradeIndex < trades.size()) {
                TradeOffer cur = trades.get(selectedTradeIndex);
                cur.maxUses = previousMaxUsesPreset(cur.maxUses);
            }
        }).dimensions(x + BUY1_X + 48, y + 54, 12, 12).build());
        this.maxUsesPlusButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("+"), button -> {
            if (selectedTradeIndex >= 0 && selectedTradeIndex < trades.size()) {
                TradeOffer cur = trades.get(selectedTradeIndex);
                cur.maxUses = nextMaxUsesPreset(cur.maxUses);
            }
        }).dimensions(x + BUY1_X + 62, y + 54, 12, 12).build());

        // Toggle buttons
        this.professionButton = this.addDrawableChild(ButtonWidget.builder(getProfessionButtonText(), button -> {
            if (this.client != null) {
                this.client.setScreen(new ProfessionPickerScreen(this, villagerProfession, professionId -> {
                    this.villagerProfession = professionId;
                    if (this.professionButton != null) {
                        this.professionButton.setMessage(getProfessionButtonText());
                    }
                }));
            }
        }).dimensions(profX, profY, btnW, btnH).build());

        this.noAiButton = this.addDrawableChild(ButtonWidget.builder(getNoAiButtonText(), button -> {
            villagerNoAi = !villagerNoAi;
            if (this.noAiButton != null) {
                this.noAiButton.setMessage(getNoAiButtonText());
            }
        }).dimensions(noaiX, noaiY, btnW, btnH).build());

        this.invulnerableButton = this.addDrawableChild(ButtonWidget.builder(getInvulnerableButtonText(), button -> {
            villagerInvulnerable = !villagerInvulnerable;
            if (this.invulnerableButton != null) {
                this.invulnerableButton.setMessage(getInvulnerableButtonText());
            }
        }).dimensions(invulX, invulY, btnW + 30, btnH).build());

        this.glowingButton = this.addDrawableChild(ButtonWidget.builder(getGlowingButtonText(), button -> {
            villagerGlowing = !villagerGlowing;
            if (this.glowingButton != null) {
                this.glowingButton.setMessage(getGlowingButtonText());
            }
        }).dimensions(glowingX, glowingY, btnW + 30, btnH).build());
    }

    private void loadFromStack() {
        trades.clear();
        try {
            if (stack.contains(DataComponentTypes.ENTITY_DATA)) {
                Object edObj = stack.get(DataComponentTypes.ENTITY_DATA);
                NbtCompound nbt = null;
                if (edObj instanceof TypedEntityData<?> ted) {
                    nbt = ted.copyNbtWithoutId();
                } else if (edObj instanceof NbtComponent comp) {
                    nbt = comp.copyNbt();
                }

                if (nbt != null) {
                    NbtCompound offersComp = getCompoundAny(nbt, "Offers", "offers");
                    if (offersComp != null) {
                        NbtList recipes = getListAny(offersComp, "Recipes", "recipes");
                        if (recipes != null) {
                            for (int i = 0; i < recipes.size(); i++) {
                                NbtCompound recipeNbt = getListCompound(recipes, i);
                                if (recipeNbt == null) continue;
                                TradeOffer offer = new TradeOffer();
                                offer.buy = parseBuyItem(getCompoundAny(recipeNbt, "buy", "Buy"));
                                offer.buyB = parseBuyItem(getCompoundAny(recipeNbt, "buyB", "buyb", "BuyB"));
                                offer.sell = parseItem(getCompound(recipeNbt, "sell"));
                                offer.maxUses = getInt(recipeNbt, "maxUses", 99);
                                offer.uses = getInt(recipeNbt, "uses", 0);
                                offer.xp = getInt(recipeNbt, "xp", 2);
                                offer.priceMultiplier = getFloat(recipeNbt, "priceMultiplier", 0.05f);
                                trades.add(offer);
                            }
                        }
                    }
                    
                    NbtCompound villagerDataComp = getCompoundAny(nbt, "VillagerData", "villager_data", "villagerData");
                    if (villagerDataComp != null) {
                        this.villagerLevel = getInt(villagerDataComp, "level", 1);
                        this.villagerProfession = getString(villagerDataComp, "profession", "minecraft:none");
                    }
                    this.villagerXp = getInt(nbt, "Xp", 0);
                    this.villagerNoAi = getBoolean(nbt, "NoAI",
                            getBoolean(nbt, "NoAi",
                                    getBoolean(nbt, "no_ai", false)));
                    this.villagerInvulnerable = getBoolean(nbt, "Invulnerable",
                            getBoolean(nbt, "invulnerable", false));
                    this.villagerGlowing = getBoolean(nbt, "Glowing",
                            getBoolean(nbt, "glowing", false));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (trades.isEmpty()) trades.add(new TradeOffer());
        selectedTradeIndex = 0;
    }

    private NbtCompound getCompound(NbtCompound nbt, String key) {
        try {
            return nbt.getCompound(key).orElse(null);
        } catch (Exception e) { return null; }
    }

    private NbtCompound getCompoundAny(NbtCompound nbt, String... keys) {
        for (String key : keys) {
            NbtCompound value = getCompound(nbt, key);
            if (value != null) return value;
        }
        return null;
    }

    private NbtCompound getListCompound(NbtList list, int index) {
        try {
            return list.getCompound(index).orElse(null);
        } catch (Exception e) { return null; }
    }

    private NbtList getList(NbtCompound nbt, String key) {
        try {
            return nbt.getList(key).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private NbtList getListAny(NbtCompound nbt, String... keys) {
        for (String key : keys) {
            NbtList value = getList(nbt, key);
            if (value != null) return value;
        }
        return null;
    }

    private int getInt(NbtCompound nbt, String key, int def) {
        try {
            return nbt.getInt(key).orElse(def);
        } catch (Exception e) { return def; }
    }

    private float getFloat(NbtCompound nbt, String key, float def) {
        try {
            return nbt.getFloat(key).orElse(def);
        } catch (Exception e) { return def; }
    }

    private String getString(NbtCompound nbt, String key, String def) {
        try {
            return nbt.getString(key).orElse(def);
        } catch (Exception e) {
            return def;
        }
    }

    private boolean getBoolean(NbtCompound nbt, String key, boolean def) {
        try {
            return nbt.getBoolean(key).orElse(def);
        } catch (Exception e) {
            return def;
        }
    }

    private ItemStack parseItem(NbtCompound nbt) {
        if (nbt == null || nbt.isEmpty()) return ItemStack.EMPTY;
        try {
            if (this.client != null && this.client.world != null) {
                var lookup = this.client.world.getRegistryManager();
                var ops = RegistryOps.of(net.minecraft.nbt.NbtOps.INSTANCE, lookup);
                ItemStack parsed = ItemStack.CODEC.parse(ops, nbt).result().orElse(ItemStack.EMPTY);
                if (!parsed.isEmpty()) return parsed;
            }
        } catch (Exception ignored) {}
        return parseLooseStack(nbt);
    }

    private ItemStack parseBuyItem(NbtCompound nbt) {
        if (nbt == null || nbt.isEmpty()) return ItemStack.EMPTY;
        try {
            if (this.client != null && this.client.world != null) {
                var lookup = this.client.world.getRegistryManager();
                var ops = RegistryOps.of(net.minecraft.nbt.NbtOps.INSTANCE, lookup);
                ItemStack fromTradedItem = TradedItem.CODEC.parse(ops, nbt)
                        .result()
                        .map(TradedItem::itemStack)
                        .map(ItemStack::copy)
                        .orElse(ItemStack.EMPTY);
                if (!fromTradedItem.isEmpty()) return fromTradedItem;
            }
        } catch (Exception ignored) {}
        // Compatibility fallback for older saved data using full ItemStack structure.
        ItemStack fallback = parseItem(nbt);
        if (!fallback.isEmpty()) return fallback;
        return parseLooseStack(nbt);
    }

    private ItemStack parseLooseStack(NbtCompound nbt) {
        try {
            String id = getString(nbt, "id", "");
            if (id.isEmpty()) return ItemStack.EMPTY;
            int count = getInt(nbt, "count", getInt(nbt, "Count", 1));
            net.minecraft.util.Identifier itemId = net.minecraft.util.Identifier.of(id);
            Item item = Registries.ITEM.get(itemId);
            if (item == null) return ItemStack.EMPTY;
            return new ItemStack(item, MathHelper.clamp(count, 1, 99));
        } catch (Exception ignored) {
            return ItemStack.EMPTY;
        }
    }

    private NbtElement encodeBuyItem(RegistryOps<NbtElement> ops, ItemStack stack) {
        try {
            TradedItem traded = new TradedItem(
                    stack.getRegistryEntry(),
                    Math.max(1, stack.getCount()),
                    ComponentMapPredicate.of(stack.getComponents())
            );
            NbtElement encoded = TradedItem.CODEC.encodeStart(ops, traded).result().orElse(null);
            if (encoded != null) return encoded;
        } catch (Exception ignored) {}
        // Guaranteed-safe fallback: keep at least id+count so trade never disappears.
        try {
            TradedItem minimal = new TradedItem(
                    stack.getRegistryEntry(),
                    Math.max(1, stack.getCount()),
                    ComponentMapPredicate.EMPTY
            );
            NbtElement encoded = TradedItem.CODEC.encodeStart(ops, minimal).result().orElse(null);
            if (encoded != null) return encoded;
        } catch (Exception ignored) {}
        // Compatibility fallback if traded-item encoding fails for any reason.
        return ItemStack.CODEC.encodeStart(ops, stack).result().orElse(null);
    }

    private void save() {
        trades.removeIf(TradeOffer::isEmpty);
        try {
            NbtCompound entityNbt = new NbtCompound();
            EntityType<?> type = EntityType.VILLAGER;

            if (stack.contains(DataComponentTypes.ENTITY_DATA)) {
                Object edObj = stack.get(DataComponentTypes.ENTITY_DATA);
                if (edObj instanceof TypedEntityData<?> ted) {
                    try {
                        Method m = ted.getClass().getMethod("copyNbt");
                        entityNbt = (NbtCompound) m.invoke(ted);
                    } catch (Exception ex) {
                        entityNbt = ted.copyNbtWithoutId();
                        entityNbt.putString("id", "minecraft:villager");
                    }
                    type = (EntityType<?>) ted.getType();
                } else if (edObj instanceof NbtComponent comp) {
                    entityNbt = comp.copyNbt();
                    if (!entityNbt.contains("id")) {
                        entityNbt.putString("id", "minecraft:villager");
                    }
                }
            }

            if (this.client != null && this.client.world != null) {
                var lookup = this.client.world.getRegistryManager();
                var ops = RegistryOps.of(net.minecraft.nbt.NbtOps.INSTANCE, lookup);

                NbtList recipes = new NbtList();
                for (TradeOffer offer : trades) {
                    NbtCompound recipeNbt = new NbtCompound();
                    if (!offer.buy.isEmpty()) {
                        NbtElement buy = encodeBuyItem(ops, offer.buy);
                        if (buy != null) recipeNbt.put("buy", buy);
                    }
                    if (!offer.buyB.isEmpty()) {
                        NbtElement buyB = encodeBuyItem(ops, offer.buyB);
                        if (buyB != null) recipeNbt.put("buyB", buyB);
                    }
                    NbtElement sell = ItemStack.CODEC.encodeStart(ops, offer.sell).result().orElse(null);
                    if (sell != null) recipeNbt.put("sell", sell);
                    recipeNbt.putInt("maxUses", offer.maxUses);
                    recipeNbt.putInt("uses", offer.uses);
                    recipeNbt.putInt("xp", offer.xp);
                    recipeNbt.putFloat("priceMultiplier", offer.priceMultiplier);
                    if (recipeNbt.contains("buy") && recipeNbt.contains("sell")) {
                        recipes.add(recipeNbt);
                    }
                }
                
                NbtCompound offers = getCompound(entityNbt, "Offers");
                if (offers == null) offers = new NbtCompound();
                offers.put("Recipes", recipes);
                entityNbt.put("Offers", offers);
                
                NbtCompound villagerData = getCompound(entityNbt, "VillagerData");
                if (villagerData == null) villagerData = new NbtCompound();
                villagerData.putInt("level", villagerLevel);
                villagerData.putString("profession", villagerProfession);
                if (!villagerData.contains("type")) {
                    villagerData.putString("type", "minecraft:plains");
                }
                entityNbt.put("VillagerData", villagerData);
                entityNbt.putInt("Xp", villagerXp);
                entityNbt.putBoolean("NoAI", villagerNoAi);
                entityNbt.putBoolean("Invulnerable", villagerInvulnerable);
                entityNbt.putBoolean("Glowing", villagerGlowing);
                entityNbt.remove("NoAi");
                entityNbt.remove("no_ai");
                entityNbt.remove("invulnerable");
                entityNbt.remove("glowing");

                TypedEntityData<?> updated = TypedEntityData.create(type, entityNbt);
                stack.set(DataComponentTypes.ENTITY_DATA, (TypedEntityData) updated);
                boolean fastApplied = applyFastPlayerInventoryUpdate(stack.copy());
                if (saveCallback != null) {
                    // Use caller-provided save path first; it has the most accurate slot context.
                    saveCallback.accept(stack.copy());
                } else if (!fastApplied) {
                    // Fallback for standalone editing flows.
                    applyToEditedSlotImmediately(stack);
                }
                close();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Do NOT call super.renderBackground() - it causes "Can only blur once per frame" crash in this mod's context
        context.fill(0, 0, this.width, this.height, 0x80000000);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        int x = (this.width - BG_WIDTH) / 2;
        int y = (this.height - BG_HEIGHT) / 2;

        // Use 512, 256 for texture coordinates to fix stretching
        // Draw 276x166 region from a 512x256 texture
        boolean drawn = tryDrawTexture(context, GUI_PATH, x, y, BG_WIDTH, BG_HEIGHT, BG_WIDTH, BG_HEIGHT, 512, 256)
            || tryDrawTexture(context, GUI_PATH_FALLBACK, x, y, BG_WIDTH, BG_HEIGHT, BG_WIDTH, BG_HEIGHT, 512, 256);
        if (!drawn) {
             context.fill(x, y, x + BG_WIDTH, y + BG_HEIGHT, 0xFF1E1E1E);
             context.fill(x + 100, y, x + 101, y + BG_HEIGHT, 0xFF3F3F3F);
        }

        context.drawText(this.textRenderer, Text.translatable("merchant.trades"), x + 5, y + 6, TITLE_COLOR, false);
        context.drawText(this.textRenderer, this.title, x + 107, y + 6, TITLE_COLOR, false);
        context.drawText(this.textRenderer, Text.translatable("container.inventory"), x + 107, y + 74, TITLE_COLOR, false);
        
        int listY = y + 18;
        TradeOffer hoverOffer = null;
        int hoverItemIdx = -1; 
        
        for (int i = 0; i < trades.size(); i++) {
            if (listY > y + BG_HEIGHT - 22) break;
            if (selectedTradeIndex == i) context.fill(x + 5, listY, x + 95, listY + 20, 0x40FFFFFF);
            TradeOffer offer = trades.get(i);
            
            if (!offer.buyB.isEmpty()) {
                 drawItemWithOverlay(context, offer.buy, x + 6, listY + 2);
                 drawItemWithOverlay(context, offer.buyB, x + 24, listY + 2);
                 if (offer.uses >= offer.maxUses) {
                     tryDrawTexture(context, TRADE_ARROW_OUT_OF_STOCK, x + 44, listY + 5, 10, 9, 10, 9, 10, 9);
                 } else {
                     tryDrawTexture(context, TRADE_ARROW, x + 44, listY + 5, 10, 9, 10, 9, 10, 9);
                 }
                 drawItemWithOverlay(context, offer.sell, x + 58, listY + 2);
            } else {
                 drawItemWithOverlay(context, offer.buy, x + 15, listY + 2);
                 if (offer.uses >= offer.maxUses) {
                     tryDrawTexture(context, TRADE_ARROW_OUT_OF_STOCK, x + 35, listY + 5, 10, 9, 10, 9, 10, 9);
                 } else {
                     tryDrawTexture(context, TRADE_ARROW, x + 35, listY + 5, 10, 9, 10, 9, 10, 9);
                 }
                 drawItemWithOverlay(context, offer.sell, x + 54, listY + 2);
            }
            context.drawText(this.textRenderer, "X", x + 85, listY + 6, 0xFFFF5555, false);
            
            if (!offer.buyB.isEmpty()) {
                if (mouseX >= x + 6 && mouseX <= x + 22 && mouseY >= listY + 2 && mouseY <= listY + 18) { hoverOffer = offer; hoverItemIdx = 0; }
                if (mouseX >= x + 24 && mouseX <= x + 40 && mouseY >= listY + 2 && mouseY <= listY + 18) { hoverOffer = offer; hoverItemIdx = 1; }
                if (mouseX >= x + 58 && mouseX <= x + 74 && mouseY >= listY + 2 && mouseY <= listY + 18) { hoverOffer = offer; hoverItemIdx = 2; }
            } else {
                if (mouseX >= x + 15 && mouseX <= x + 31 && mouseY >= listY + 2 && mouseY <= listY + 18) { hoverOffer = offer; hoverItemIdx = 0; }
                if (mouseX >= x + 54 && mouseX <= x + 70 && mouseY >= listY + 2 && mouseY <= listY + 18) { hoverOffer = offer; hoverItemIdx = 2; }
            }
            
            listY += 22;
        }

        if (selectedTradeIndex >= 0 && selectedTradeIndex < trades.size()) {
            TradeOffer cur = trades.get(selectedTradeIndex);
            renderSlot(context, x + BUY1_X, y + BUY1_Y, cur.buy, mouseX, mouseY);
            renderSlot(context, x + BUY2_X, y + BUY2_Y, cur.buyB, mouseX, mouseY);
            renderSlot(context, x + SELL_X, y + SELL_Y, cur.sell, mouseX, mouseY);
            
            // Draw out of stock if needed (else vanilla texture under handles the grey arrow)
            if (cur.uses >= cur.maxUses) {
                tryDrawTexture(context, TRADE_ARROW_OUT_OF_STOCK, x + 186, y + 42, 28, 21, 10, 9, 10, 9);
            }
            
            context.drawText(this.textRenderer, "Max: " + cur.maxUses, x + BUY1_X, y + 56, LABEL_COLOR, false);
            
            if (!cur.buy.isEmpty() && isMouseOver(mouseX, mouseY, x + BUY1_X, y + BUY1_Y)) context.drawItemTooltip(this.textRenderer, cur.buy, (int)mouseX, (int)mouseY);
            if (!cur.buyB.isEmpty() && isMouseOver(mouseX, mouseY, x + BUY2_X, y + BUY2_Y)) context.drawItemTooltip(this.textRenderer, cur.buyB, (int)mouseX, (int)mouseY);
            if (!cur.sell.isEmpty() && isMouseOver(mouseX, mouseY, x + SELL_X, y + SELL_Y)) context.drawItemTooltip(this.textRenderer, cur.sell, (int)mouseX, (int)mouseY);
        }

        if (this.noAiButton != null) {
            context.drawText(this.textRenderer, "<-------- Recommended",
                    this.noAiButton.getX() + this.noAiButton.getWidth() + 8,
                    this.noAiButton.getY() + 6,
                    0xFF66FF66,
                    false);
        }

        if (this.client != null && this.client.player != null) {
            var inv = this.client.player.getInventory();
            for (int i = 0; i < 9; i++) renderSlot(context, x + 108 + i * 18, y + 142, inv.getStack(i), mouseX, mouseY);
            for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) renderSlot(context, x + 108 + c * 18, y + 84 + r * 18, inv.getStack(c + r * 9 + 9), mouseX, mouseY);
            
            for (int i = 0; i < 9; i++) {
                ItemStack s = inv.getStack(i);
                if (!s.isEmpty() && isMouseOver(mouseX, mouseY, x + 108 + i * 18, y + 142)) context.drawItemTooltip(this.textRenderer, s, (int)mouseX, (int)mouseY);
            }
            for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) {
                ItemStack s = inv.getStack(c + r * 9 + 9);
                if (!s.isEmpty() && isMouseOver(mouseX, mouseY, x + 108 + c * 18, y + 84 + r * 18)) context.drawItemTooltip(this.textRenderer, s, (int)mouseX, (int)mouseY);
            }

            ItemStack cursor = this.client.player.currentScreenHandler.getCursorStack();
            if (!cursor.isEmpty()) drawItemWithOverlay(context, cursor, mouseX - 8, mouseY - 8);
        }

        if (hoverOffer != null) {
            ItemStack hovered = hoverItemIdx == 0 ? hoverOffer.buy : (hoverItemIdx == 1 ? hoverOffer.buyB : hoverOffer.sell);
            if (!hovered.isEmpty()) context.drawItemTooltip(this.textRenderer, hovered, (int)mouseX, (int)mouseY);
        }

        context.drawText(this.textRenderer, "L: " + villagerLevel, x + 250, y + 17, TITLE_COLOR, false);

        super.render(context, mouseX, mouseY, delta);
    }

    private boolean tryDrawTexture(DrawContext context, Identifier icon, int x, int y, int width, int height, int regionW, int regionH, int txW, int txH) {
        try {
            // Corrected UV mapping: draw exactly 'regionW' and 'regionH' pixels from texture at 0,0 as 'width'x'height' on screen
            context.drawTexture(RenderPipelines.GUI_TEXTURED, icon, x, y, 0.0F, 0.0F, width, height, regionW, regionH, txW, txH);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void renderSlot(DrawContext context, int x, int y, ItemStack s, int mx, int my) {
        if (mx >= x && mx < x + 16 && my >= y && my < y + 16) context.fill(x, y, x + 16, y + 16, 0x40FFFFFF);
        if (!s.isEmpty()) drawItemWithOverlay(context, s, x, y);
    }

    private void drawItemWithOverlay(DrawContext context, ItemStack stack, int x, int y) {
        context.drawItem(stack, x, y);
        try {
            context.drawStackOverlay(this.textRenderer, stack, x, y, null);
            return;
        } catch (Exception ignored) {}
        try {
            Method m = context.getClass().getMethod(
                    "drawStackOverlay",
                    net.minecraft.client.font.TextRenderer.class,
                    ItemStack.class,
                    int.class,
                    int.class
            );
            m.invoke(context, this.textRenderer, stack, x, y);
        } catch (Exception ignored) {}
    }

    private Text getNoAiButtonText() {
        if (villagerNoAi) {
            return Text.literal("NoAi: True").formatted(Formatting.GREEN);
        }
        return Text.literal("NoAi: False").formatted(Formatting.DARK_RED);
    }

    private Text getInvulnerableButtonText() {
        if (villagerInvulnerable) {
            return Text.literal("Invulnerable: ON").formatted(Formatting.GREEN);
        }
        return Text.literal("Invulnerable: OFF").formatted(Formatting.DARK_RED);
    }

    private Text getGlowingButtonText() {
        if (villagerGlowing) {
            return Text.literal("Glowing: ON").formatted(Formatting.GREEN);
        }
        return Text.literal("Glowing: OFF").formatted(Formatting.DARK_RED);
    }

    private Text getProfessionButtonText() {
        return Text.literal("Profession").formatted(Formatting.BLUE);
    }

    private int nextMaxUsesPreset(int current) {
        for (int preset : MAX_USES_PRESETS) {
            if (current < preset) {
                return preset;
            }
        }
        return MAX_USES_PRESETS[MAX_USES_PRESETS.length - 1];
    }

    private int previousMaxUsesPreset(int current) {
        for (int i = MAX_USES_PRESETS.length - 1; i >= 0; i--) {
            int preset = MAX_USES_PRESETS[i];
            if (current > preset) {
                return preset;
            }
        }
        return MAX_USES_PRESETS[0];
    }

    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        double mx = click.x(), my = click.y();
        int button = click.button();
        int x = (this.width - BG_WIDTH) / 2, y = (this.height - BG_HEIGHT) / 2;
        int listY = y + 18;
        
        if (button == 2) {
             if (this.client != null && this.client.player != null) {
                 ItemStack pick = getStackUnderMouse(mx, my, x, y);
                 if (!pick.isEmpty()) {
                     ItemStack clone = pick.copy();
                     clone.setCount(clone.getMaxCount());
                     this.client.player.currentScreenHandler.setCursorStack(clone);
                     return true;
                 }
             }
        }

        for (int i = 0; i < trades.size(); i++) {
            if (mx >= x + 85 && mx <= x + 95 && my >= listY && my <= listY + 20) { trades.remove(i); if (selectedTradeIndex >= trades.size()) selectedTradeIndex = trades.size() - 1; return true; }
            if (mx >= x + 5 && mx <= x + 85 && my >= listY && my <= listY + 20) { selectedTradeIndex = i; return true; }
            listY += 22;
        }
        if (selectedTradeIndex >= 0 && selectedTradeIndex < trades.size()) {
            TradeOffer cur = trades.get(selectedTradeIndex);
            if (isMouseOver(mx, my, x + BUY1_X, y + BUY1_Y)) { handleSlot(cur.buy, s -> cur.buy = s); return true; }
            if (isMouseOver(mx, my, x + BUY2_X, y + BUY2_Y)) { handleSlot(cur.buyB, s -> cur.buyB = s); return true; }
            if (isMouseOver(mx, my, x + SELL_X, y + SELL_Y)) { handleSlot(cur.sell, s -> cur.sell = s); return true; }
        }
        
        // Inventory Clicks
        if (this.client != null && this.client.player != null) {
            var inv = this.client.player.getInventory();
            for (int i = 0; i < 9; i++) {
                if (isMouseOver(mx, my, x + 108 + i * 18, y + 142)) { 
                    final int fi = i; 
                    handleSlot(inv.getStack(fi), s -> inv.setStack(fi, s)); 
                    return true; 
                }
            }
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 9; c++) {
                    if (isMouseOver(mx, my, x + 108 + c * 18, y + 84 + r * 18)) { 
                        int fi = c + r * 9 + 9; 
                        handleSlot(inv.getStack(fi), s -> inv.setStack(fi, s)); 
                        return true; 
                    }
                }
            }
        }
        
        return super.mouseClicked(click, bl);
    }

    private void handleSlot(ItemStack slot, Consumer<ItemStack> setter) {
        if (this.client != null && this.client.player != null) {
            ItemStack cursor = this.client.player.currentScreenHandler.getCursorStack();
            this.client.player.currentScreenHandler.setCursorStack(slot.copy());
            setter.accept(cursor.copy());
        }
    }

    private ItemStack getStackUnderMouse(double mx, double my, int x, int y) {
        if (selectedTradeIndex >= 0 && selectedTradeIndex < trades.size()) {
            TradeOffer cur = trades.get(selectedTradeIndex);
            if (isMouseOver(mx, my, x + BUY1_X, y + BUY1_Y)) return cur.buy;
            if (isMouseOver(mx, my, x + BUY2_X, y + BUY2_Y)) return cur.buyB;
            if (isMouseOver(mx, my, x + SELL_X, y + SELL_Y)) return cur.sell;
        }

        int listY = y + 18;
        for (int i = 0; i < trades.size(); i++) {
            if (listY > y + BG_HEIGHT - 22) break;
            TradeOffer offer = trades.get(i);
            
            if (!offer.buyB.isEmpty()) {
                if (mx >= x + 6 && mx <= x + 22 && my >= listY + 2 && my <= listY + 18) return offer.buy;
                if (mx >= x + 24 && mx <= x + 40 && my >= listY + 2 && my <= listY + 18) return offer.buyB;
                if (mx >= x + 58 && mx <= x + 74 && my >= listY + 2 && my <= listY + 18) return offer.sell;
            } else {
                if (mx >= x + 15 && mx <= x + 31 && my >= listY + 2 && my <= listY + 18) return offer.buy;
                if (mx >= x + 54 && mx <= x + 70 && my >= listY + 2 && my <= listY + 18) return offer.sell;
            }
            listY += 22;
        }

        if (this.client != null && this.client.player != null) {
            var inv = this.client.player.getInventory();
            for (int i = 0; i < 9; i++) {
                if (isMouseOver(mx, my, x + 108 + i * 18, y + 142)) return inv.getStack(i);
            }
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 9; c++) {
                    if (isMouseOver(mx, my, x + 108 + c * 18, y + 84 + r * 18)) return inv.getStack(c + r * 9 + 9);
                }
            }
        }
        return ItemStack.EMPTY;
    }

    private boolean isMouseOver(double mx, double my, int x, int y) { return mx >= x && mx <= x + 16 && my >= y && my <= y + 16; }

    private int resolveTargetSlot() {
        if (this.client == null || this.client.player == null) {
            return this.slotId;
        }
        int selectedHotbarSlot = this.client.player.getInventory().selectedSlot + 36;
        if (this.slotId == -1) return selectedHotbarSlot;
        // Per request: villager egg must never be written to offhand.
        if (this.slotId == 45) return selectedHotbarSlot;
        return this.slotId;
    }

    private boolean applyFastPlayerInventoryUpdate(ItemStack updatedStack) {
        if (this.client == null || this.client.player == null) return false;
        if (this.slotId != -1 && (this.slotId < 5 || this.slotId > 45)) {
            // Unknown external container slot id, leave it to callback flow.
            return false;
        }
        int packetSlot = resolveTargetSlot();

        // Force replacement: clear previous stack first, then write updated stack.
        setLocalSlotStack(packetSlot, ItemStack.EMPTY);
        setLocalSlotStack(packetSlot, updatedStack.copy());
        sendCreativeReplace(packetSlot, updatedStack.copy());
        return true;
    }

    private void applyToEditedSlotImmediately(ItemStack updatedStack) {
        if (this.client == null || this.client.player == null) return;
        int packetSlot = resolveTargetSlot();
        // Force replacement: clear previous stack first, then write updated stack.
        setLocalSlotStack(packetSlot, ItemStack.EMPTY);
        setLocalSlotStack(packetSlot, updatedStack.copy());
        sendCreativeReplace(packetSlot, updatedStack.copy());
    }

    private void setLocalSlotStack(int packetSlot, ItemStack value) {
        if (this.client == null || this.client.player == null) return;

        if (packetSlot >= 36 && packetSlot <= 44) {
            this.client.player.getInventory().setStack(packetSlot - 36, value.copy());
        } else if (packetSlot >= 9 && packetSlot <= 35) {
            this.client.player.getInventory().setStack(packetSlot, value.copy());
        } else if (packetSlot >= 5 && packetSlot <= 8) {
            this.client.player.getInventory().setStack(39 - (packetSlot - 5), value.copy());
        } else {
            int selected = this.client.player.getInventory().selectedSlot;
            this.client.player.getInventory().setStack(selected, value.copy());
        }

        if (this.client.player.currentScreenHandler != null) {
            for (Slot s : this.client.player.currentScreenHandler.slots) {
                if (s.id == packetSlot) {
                    s.setStack(value.copy());
                }
            }
        }
    }

    private void sendCreativeReplace(int packetSlot, ItemStack updatedStack) {
        if (this.client == null) return;

        if (this.client.interactionManager != null) {
            this.client.interactionManager.clickCreativeStack(ItemStack.EMPTY, packetSlot);
            this.client.interactionManager.clickCreativeStack(updatedStack.copy(), packetSlot);
        }
        if (this.client.getNetworkHandler() != null) {
            this.client.getNetworkHandler().sendPacket(new CreativeInventoryActionC2SPacket(packetSlot, ItemStack.EMPTY));
            this.client.getNetworkHandler().sendPacket(new CreativeInventoryActionC2SPacket(packetSlot, updatedStack.copy()));
        }
    }

    @Override
    public void close() { if (this.client != null) this.client.setScreen(parent); }
}
