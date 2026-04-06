package com.trusted.systemnbteditor.gui;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class EnchPicker extends Screen {
    private final Screen parent;
    private final BiConsumer<RegistryEntry<Enchantment>, Integer> callback;
    private float scrollTarget = 0;
    private float scrollSmooth = 0;
    private int scrollOffset = 0;
    private RegistryEntry<Enchantment> selectedEnchantment = null;
    private static final float LERP = 0.12f;

    private static final int POPUP_WIDTH = 320;
    private static final int POPUP_HEIGHT = 240;
    private static final int ITEM_HEIGHT = 16;
    private static final int VISIBLE_ITEMS = 10;
    private static final int LIST_HEIGHT = VISIBLE_ITEMS * ITEM_HEIGHT;

    private TextFieldWidget searchField;
    private TextFieldWidget levelField;
    private ButtonWidget addButton;

    private List<RegistryEntry<Enchantment>> allEnchantments = new ArrayList<>();
    private List<RegistryEntry<Enchantment>> filteredEnchantments = new ArrayList<>();

    public EnchPicker(Screen parent, BiConsumer<RegistryEntry<Enchantment>, Integer> callback) {
        super(Text.of("Pick Enchantment"));
        this.parent = parent;
        this.callback = callback;
    }

    @Override
    protected void init() {
        super.init();

        if (this.client != null && this.client.world != null) {
            this.allEnchantments.clear();
            this.client.world.getRegistryManager().getOptional(RegistryKeys.ENCHANTMENT)
                .ifPresent(r -> r.streamEntries().forEach(allEnchantments::add));
            this.allEnchantments.sort(Comparator.comparing(e -> e.getKey().get().getValue().toString()));
            this.filteredEnchantments = new ArrayList<>(this.allEnchantments);
        }

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int popupX = centerX - POPUP_WIDTH / 2;
        int popupY = centerY - POPUP_HEIGHT / 2;

        // Search Field
        this.searchField = new TextFieldWidget(this.textRenderer, popupX + 10, popupY + 25, POPUP_WIDTH - 20, 20, Text.of("Search..."));
        this.searchField.setPlaceholder(Text.literal("Type to search...").formatted(Formatting.GRAY));
        this.searchField.setChangedListener(this::onSearchChanged);
        this.addDrawableChild(this.searchField);

        // Level Field
        this.levelField = new TextFieldWidget(this.textRenderer, popupX + 45, popupY + POPUP_HEIGHT - 30, 40, 20, Text.literal("1"));
        this.levelField.setText("1");
        this.addDrawableChild(this.levelField);

        // Add Button
        this.addButton = this.addDrawableChild(ButtonWidget.builder(Text.of("Add"), button -> {
            if (selectedEnchantment != null) {
                int level = 1;
                try { level = Integer.parseInt(levelField.getText()); } catch (Exception ignored) {}
                callback.accept(selectedEnchantment, level);
            }
        }).dimensions(popupX + POPUP_WIDTH - 80, popupY + POPUP_HEIGHT - 30, 70, 20).build());

        this.setInitialFocus(this.searchField);
    }

    private void onSearchChanged(String query) {
        String q = query.toLowerCase();
        this.filteredEnchantments = allEnchantments.stream()
            .filter(e -> {
                String id = e.getKey().get().getValue().toString().toLowerCase();
                String name = e.value().description().getString().toLowerCase();
                return id.contains(q) || name.contains(q);
            })
            .collect(Collectors.toList());
        this.scrollTarget = 0;
        this.scrollSmooth = 0;
        this.scrollOffset = 0;
        this.selectedEnchantment = null;
    }

    @Override
    public void tick() {
        super.tick();
        int totalH = filteredEnchantments.size() * ITEM_HEIGHT;
        int maxScroll = Math.max(0, totalH - LIST_HEIGHT);
        if (scrollTarget < 0) scrollTarget = 0;
        if (scrollTarget > maxScroll) scrollTarget = maxScroll;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int totalH = filteredEnchantments.size() * ITEM_HEIGHT;
        if (totalH > LIST_HEIGHT) {
            scrollTarget -= (float) (verticalAmount * ITEM_HEIGHT * 2);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private int draggingSlider = 0; // 0=none, 1=slider

    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        double mouseX = click.x();
        double mouseY = click.y();
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int popupX = centerX - POPUP_WIDTH / 2;
        int popupY = centerY - POPUP_HEIGHT / 2;
        int listY = popupY + 50;

        if (click.button() == 0) {
            // Slider click
            if (filteredEnchantments.size() * ITEM_HEIGHT > LIST_HEIGHT) {
                int trackX = popupX + POPUP_WIDTH - 8;
                if (mouseX >= trackX && mouseX <= trackX + 4 && mouseY >= listY && mouseY <= listY + LIST_HEIGHT) {
                    draggingSlider = 1;
                    updateSliderPos(mouseY);
                    return true;
                }
            }
            
            // Row click
            if (mouseX >= popupX + 8 && mouseX <= popupX + POPUP_WIDTH - 8 && mouseY >= listY && mouseY <= listY + LIST_HEIGHT) {
                float relY = (float) (mouseY - listY + scrollOffset);
                int dataIndex = (int) (relY / ITEM_HEIGHT);
                if (dataIndex >= 0 && dataIndex < filteredEnchantments.size()) {
                    selectedEnchantment = filteredEnchantments.get(dataIndex);
                    return true;
                }
            }
        }
        return super.mouseClicked(click, bl);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (click.button() == 0) draggingSlider = 0;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        if (click.button() == 0 && draggingSlider != 0) {
            updateSliderPos(click.y());
            return true;
        }
        return super.mouseDragged(click, dx, dy);
    }

    private void updateSliderPos(double mouseY) {
        int centerY = this.height / 2;
        int popupY = centerY - POPUP_HEIGHT / 2;
        int listY = popupY + 50;
        float rel = (float)(mouseY - listY) / LIST_HEIGHT;
        int totalH = filteredEnchantments.size() * ITEM_HEIGHT;
        scrollTarget = rel * (totalH - LIST_HEIGHT);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
        context.fill(0, 0, this.width, this.height, 0x80000000);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        scrollSmooth += (scrollTarget - scrollSmooth) * LERP;
        if (Math.abs(scrollSmooth - scrollTarget) < 0.1) scrollSmooth = scrollTarget;
        scrollOffset = Math.round(scrollSmooth);

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int popupX = centerX - POPUP_WIDTH / 2;
        int popupY = centerY - POPUP_HEIGHT / 2;

        context.fill(popupX, popupY, popupX + POPUP_WIDTH, popupY + POPUP_HEIGHT, 0xFF050510);
        int borderColor = 0xFF4040a0;
        context.fill(popupX, popupY, popupX + POPUP_WIDTH, popupY + 1, borderColor);
        context.fill(popupX, popupY + POPUP_HEIGHT - 1, popupX + POPUP_WIDTH, popupY + POPUP_HEIGHT, borderColor);
        context.fill(popupX, popupY, popupX + 1, popupY + POPUP_HEIGHT, borderColor);
        context.fill(popupX + POPUP_WIDTH - 1, popupY, popupX + POPUP_WIDTH, popupY + POPUP_HEIGHT, borderColor);

        context.drawText(this.textRenderer, "Pick Enchantment", popupX + 10, popupY + 10, 0xFFFFFFFF, false);

        int listY = popupY + 50;
        int rowAqua = 0xFF55FFFF;
        
        context.enableScissor(popupX + 8, listY, popupX + POPUP_WIDTH - 8, listY + LIST_HEIGHT);
        for (int i = 0; i < filteredEnchantments.size(); i++) {
            int ry = listY + i * ITEM_HEIGHT - scrollOffset;
            if (ry + ITEM_HEIGHT < listY || ry > listY + LIST_HEIGHT) continue;

            RegistryEntry<Enchantment> e = filteredEnchantments.get(i);
            boolean hovered = mouseX >= popupX + 8 && mouseX < popupX + POPUP_WIDTH - 8 && mouseY >= listY && mouseY < listY + LIST_HEIGHT && mouseY >= ry && mouseY < ry + ITEM_HEIGHT;
            boolean selected = e == selectedEnchantment;

            if (selected) {
                context.fill(popupX + 8, ry, popupX + POPUP_WIDTH - 16, ry + ITEM_HEIGHT, 0x60FFFFFF);
            } else if (hovered) {
                context.fill(popupX + 8, ry, popupX + POPUP_WIDTH - 16, ry + ITEM_HEIGHT, 0x20FFFFFF);
            }

            Text label = e.value().description();
            context.drawText(this.textRenderer, label, popupX + 15, ry + 4, selected ? 0xFFFFFFFF : rowAqua, false);
            
            ItemStack iconStack = getRepresentativeItem(e);
            if (!iconStack.isEmpty()) {
                context.drawItem(iconStack, popupX + POPUP_WIDTH - 30, ry);
            }
        }
        context.disableScissor();

        if (filteredEnchantments.size() * ITEM_HEIGHT > LIST_HEIGHT) {
            int totalH = filteredEnchantments.size() * ITEM_HEIGHT;
            int thumbH = Math.max(6, (int)((float)LIST_HEIGHT * LIST_HEIGHT / totalH));
            int thumbY = listY + (int)((float)scrollSmooth * (LIST_HEIGHT - thumbH) / (totalH - LIST_HEIGHT));
            int trackX = popupX + POPUP_WIDTH - 8;
            context.fill(trackX, listY, trackX + 4, listY + LIST_HEIGHT, 0xFF333333);
            context.fill(trackX, thumbY, trackX + 4, thumbY + thumbH, 0xFF8888CC);
        }

        context.drawText(this.textRenderer, "Level:", popupX + 10, popupY + POPUP_HEIGHT - 25, 0xFFA0A0A0, false);
        super.render(context, mouseX, mouseY, delta);
    }

    private ItemStack getRepresentativeItem(RegistryEntry<Enchantment> ench) {
        if (ench == null || ench.getKey().isEmpty()) return new ItemStack(Items.BOOK);
        String id = ench.getKey().get().getValue().getPath().toLowerCase();
        if (id.contains("sharpness") || id.contains("smite") || id.contains("fire_aspect") || 
            id.contains("bane_of_arthropods") || id.contains("knockback") || id.contains("sweeping_edge") || 
            id.contains("looting")) {
            return new ItemStack(Items.NETHERITE_SWORD);
        }
        if (id.contains("efficiency") || id.contains("fortune") || id.contains("silk_touch")) {
            return new ItemStack(Items.NETHERITE_PICKAXE);
        }
        if (id.contains("density") || id.contains("breach") || id.contains("wind_burst")) {
            return new ItemStack(Items.MACE);
        }
        if (id.contains("lunge")) {
            ItemStack spear = new ItemStack(net.minecraft.registry.Registries.ITEM.get(net.minecraft.util.Identifier.tryParse("bettercombat:netherite_spear")));
            if (spear.isEmpty() || spear.getItem() == Items.AIR) {
                spear = new ItemStack(net.minecraft.registry.Registries.ITEM.get(net.minecraft.util.Identifier.tryParse("simplyswords:netherite_spear")));
            }
            if (spear.isEmpty() || spear.getItem() == Items.AIR) {
                spear = new ItemStack(Items.TRIDENT);
            }
            return spear;
        }
        if (id.contains("feather_falling") || id.contains("depth_strider") || id.contains("frost_walker") || id.contains("soul_speed") || id.contains("walker")) {
            return new ItemStack(Items.NETHERITE_BOOTS);
        }
        if (id.contains("swift_sneak")) {
            return new ItemStack(Items.NETHERITE_LEGGINGS);
        }
        if (id.contains("protection") || id.contains("thorns")) {
            return new ItemStack(Items.NETHERITE_CHESTPLATE);
        }
        if (id.contains("respiration") || id.contains("aqua_affinity")) {
            return new ItemStack(Items.NETHERITE_HELMET);
        }
        if (id.contains("power") || id.contains("punch") || id.contains("flame") || id.contains("infinity")) {
            return new ItemStack(Items.BOW);
        }
        if (id.contains("quick_charge") || id.contains("multishot") || id.contains("piercing")) {
            return new ItemStack(Items.CROSSBOW);
        }
        if (id.contains("riptide") || id.contains("impaling") || id.contains("channeling") || id.contains("loyalty")) {
            return new ItemStack(Items.TRIDENT);
        }
        if (id.contains("unbreaking") || id.contains("mending") || id.contains("vanishing") || id.contains("binding")) {
            return new ItemStack(Items.ENCHANTED_BOOK);
        }
        return new ItemStack(Items.BOOK);
    }

    @Override
    public void close() {
        if (this.client != null) this.client.setScreen(parent);
    }
}
