package com.trusted.systemnbteditor.gui;

import com.trusted.systemnbteditor.util.ComponentIconUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;
import net.minecraft.component.ComponentType;
import com.mojang.brigadier.StringReader;
import net.minecraft.command.argument.NbtCompoundArgumentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class SimpleNbtEditorScreen extends Screen {
    private final Screen parent;
    private ItemStack stack;
    private final int slotId;
    private final Consumer<ItemStack> saveCallback;

    private static final String ALL_HIDDEN_COMPONENTS = "\"minecraft:pot_decorations\",\"minecraft:suspicious_stew_effects\",\"minecraft:mooshroom/variant\",\"minecraft:repairable\",\"minecraft:repair_cost\",\"minecraft:map_color\",\"minecraft:sheep/color\",\"minecraft:cow/variant\",\"minecraft:consumable\",\"minecraft:custom_name\",\"minecraft:note_block_sound\",\"minecraft:charged_projectiles\",\"minecraft:max_stack_size\",\"minecraft:bees\",\"minecraft:shulker/color\",\"minecraft:enchantments\",\"minecraft:potion_duration_scale\",\"minecraft:firework_explosion\",\"minecraft:horse/variant\",\"minecraft:villager/variant\",\"minecraft:block_state\",\"minecraft:stored_enchantments\",\"minecraft:lodestone_tracker\",\"minecraft:enchantment_glint_override\",\"minecraft:food\",\"minecraft:equippable\",\"minecraft:map_post_processing\",\"minecraft:container_loot\",\"minecraft:written_book_content\",\"minecraft:llama/variant\",\"minecraft:recipes\",\"minecraft:max_damage\",\"minecraft:tool\",\"minecraft:blocks_attacks\",\"minecraft:weapon\",\"minecraft:banner_patterns\",\"minecraft:block_entity_data\",\"minecraft:rarity\",\"minecraft:unbreakable\",\"minecraft:enchantable\",\"minecraft:writable_book_content\",\"minecraft:item_name\",\"minecraft:creative_slot_lock\",\"minecraft:ominous_bottle_amplifier\",\"minecraft:container\",\"minecraft:debug_stick_state\",\"minecraft:can_place_on\",\"minecraft:wolf/collar\",\"minecraft:provides_trim_material\",\"minecraft:dyed_color\",\"minecraft:profile\",\"minecraft:base_color\",\"minecraft:use_remainder\",\"minecraft:map_id\",\"minecraft:chicken/variant\",\"minecraft:glider\",\"minecraft:painting/variant\",\"minecraft:parrot/variant\",\"minecraft:jukebox_playable\",\"minecraft:wolf/sound_variant\",\"minecraft:attribute_modifiers\",\"minecraft:salmon/size\",\"minecraft:trim\",\"minecraft:break_sound\",\"minecraft:bundle_contents\",\"minecraft:tooltip_style\",\"minecraft:can_break\",\"minecraft:potion_contents\",\"minecraft:fox/variant\",\"minecraft:lock\",\"minecraft:frog/variant\",\"minecraft:axolotl/variant\",\"minecraft:map_decorations\",\"minecraft:wolf/variant\",\"minecraft:custom_model_data\",\"minecraft:instrument\",\"minecraft:damage\",\"minecraft:bucket_entity_data\",\"minecraft:tropical_fish/pattern\",\"minecraft:custom_data\",\"minecraft:item_model\",\"minecraft:tropical_fish/base_color\",\"minecraft:intangible_projectile\",\"minecraft:tropical_fish/pattern_color\",\"minecraft:death_protection\",\"minecraft:entity_data\",\"minecraft:fireworks\",\"minecraft:damage_resistant\",\"minecraft:provides_banner_patterns\",\"minecraft:cat/collar\",\"minecraft:use_cooldown\",\"minecraft:rabbit/variant\",\"minecraft:tooltip_display\",\"minecraft:cat/variant\",\"minecraft:pig/variant\"";

    private static class ToggleOption {
        String name;
        String iconKey;
        java.util.function.Supplier<Boolean> isToggled;
        Runnable toggleAction;

        ToggleOption(String name, String iconKey, java.util.function.Supplier<Boolean> isToggled, Runnable toggleAction) {
            this.name = name;
            this.iconKey = iconKey;
            this.isToggled = isToggled;
            this.toggleAction = toggleAction;
        }
    }

    private static class TextOption {
        String name;
        String key;
        String iconKey;
        java.util.function.Supplier<String> getValue;
        java.util.function.Consumer<String> setValue;

        TextOption(String name, String key, String iconKey, java.util.function.Supplier<String> getValue, java.util.function.Consumer<String> setValue) {
            this.name = name;
            this.key = key;
            this.iconKey = iconKey;
            this.getValue = getValue;
            this.setValue = setValue;
        }
    }

    private final List<ToggleOption> toggles = new ArrayList<>();
    private final List<TextOption> numericals = new ArrayList<>();

    public SimpleNbtEditorScreen(Screen parent, ItemStack stack, int slotId, Consumer<ItemStack> saveCallback) {
        super(Text.of("Simple Component Editor"));
        this.parent = parent;
        this.stack = stack.copy();
        this.slotId = slotId;
        this.saveCallback = saveCallback;

        initToggles();
        initNumericals();
    }

    private void initToggles() {
        toggles.add(new ToggleOption("Unbreakable", "minecraft:unbreakable", 
            () -> hasComponent("minecraft:unbreakable"), 
            () -> toggleSimpleComponent("minecraft:unbreakable", "{}")
        ));
        toggles.add(new ToggleOption("Fire Resistant", "minecraft:damage_resistant", 
            () -> hasComponent("minecraft:damage_resistant"), 
            () -> toggleSimpleComponent("minecraft:damage_resistant", "{types: \"#minecraft:is_fire\"}")
        ));
        toggles.add(new ToggleOption("Glint Override", "minecraft:enchantment_glint_override", 
            () -> hasComponent("minecraft:enchantment_glint_override"), 
            () -> toggleSimpleComponent("minecraft:enchantment_glint_override", "true")
        ));
        toggles.add(new ToggleOption("Hide Tooltip", "minecraft:item_frame", 
            () -> hasTooltipKey("hide_tooltip"), 
            () -> toggleTooltipKey("hide_tooltip")
        ));
        toggles.add(new ToggleOption("Hide Additional", "minecraft:item_frame", 
            () -> hasTooltipKey("hidden_components"), 
            () -> toggleTooltipKey("hide_additional")
        ));
        toggles.add(new ToggleOption("Glider", "minecraft:glider", 
            () -> hasComponent("minecraft:glider"), 
            () -> toggleSimpleComponent("minecraft:glider", "{}")
        ));
    }

    private void initNumericals() {
        numericals.add(new TextOption("Count", null, null, 
            () -> String.valueOf(stack.getCount()), 
            val -> { try { stack.setCount(Integer.parseInt(val)); } catch(Exception e){} }
        ));
        numericals.add(new TextOption("Max stack", "minecraft:max_stack_size", "minecraft:max_stack_size", 
            () -> getComponentVal("minecraft:max_stack_size", "64"), 
            val -> setComponent("minecraft:max_stack_size", val)
        ));
        numericals.add(new TextOption("Damage", "minecraft:damage", "minecraft:damage", 
            () -> getComponentVal("minecraft:damage", "0"), 
            val -> setComponent("minecraft:damage", val)
        ));
        numericals.add(new TextOption("Max Damage", "minecraft:max_damage", "minecraft:max_damage", 
            () -> getComponentVal("minecraft:max_damage", "0"), 
            val -> setComponent("minecraft:max_damage", val)
        ));
        numericals.add(new TextOption("Use Cooldown", "minecraft:use_cooldown", "minecraft:use_cooldown", 
            () -> parseCooldownValue(getComponentSnbt("minecraft:use_cooldown")), 
            val -> setComponent("minecraft:use_cooldown", "{seconds:" + val + "f}")
        ));
        numericals.add(new TextOption("Disable Blocking", "minecraft:weapon", "minecraft:shield", 
            () -> parseWeaponValue(getComponentSnbt("minecraft:weapon")), 
            val -> setComponent("minecraft:weapon", "{disable_blocking_for_seconds:" + val + "f}")
        ));
    }

    private String parseWeaponValue(String snbt) {
        if (snbt == null) return "0.0";
        try {
            if (snbt.contains("disable_blocking_for_seconds:")) {
                String sub = snbt.substring(snbt.indexOf("disable_blocking_for_seconds:") + 29);
                if (sub.contains(",")) sub = sub.substring(0, sub.indexOf(","));
                if (sub.contains("}")) sub = sub.substring(0, sub.indexOf("}"));
                return sub.replace("f", "").trim();
            }
        } catch (Exception e) {}
        return "0.0";
    }

    private String getComponentVal(String key, String def) {
        String snbt = getComponentSnbt(key);
        return snbt != null ? snbt : def;
    }

    private String parseCooldownValue(String snbt) {
        if (snbt == null) return "0.0";
        try {
            if (snbt.contains("seconds:")) {
                String sub = snbt.substring(snbt.indexOf("seconds:") + 8);
                if (sub.contains(",")) sub = sub.substring(0, sub.indexOf(","));
                if (sub.contains("}")) sub = sub.substring(0, sub.indexOf("}"));
                return sub.replace("f", "").trim();
            }
        } catch (Exception e) {}
        return "0.0";
    }

    @Override
    protected void init() {
        super.init();

        int startY = 40;
        int rowHeight = 25;
        int leftX = 20;
        int rightX = this.width / 2 + 20;

        // Numerical Fields (Left)
        for (int i = 0; i < numericals.size(); i++) {
            TextOption opt = numericals.get(i);
            int y = startY + (i * rowHeight);

            TextFieldWidget field = new TextFieldWidget(this.textRenderer, leftX + 130, y, 60, 20, Text.of(opt.name));
            field.setText(opt.getValue.get());
            field.setChangedListener(opt.setValue);
            this.addDrawableChild(field);
        }

        // Toggles (Right)
        for (int i = 0; i < toggles.size(); i++) {
            ToggleOption opt = toggles.get(i);
            int y = startY + (i * rowHeight);

            ButtonWidget btn = ButtonWidget.builder(getToggleText(opt.isToggled.get()), button -> {
                opt.toggleAction.run();
                button.setMessage(getToggleText(opt.isToggled.get()));
            }).dimensions(rightX + 130, y, 60, 20).build();

            this.addDrawableChild(btn);
        }

        // Special Pickers (Under)
        int pickerY = startY + Math.max(numericals.size(), toggles.size()) * rowHeight + 20;

        addPickerRow("Tooltip Hides", "minecraft:item_frame", leftX, pickerY, button -> openTooltipPicker());
        addPickerRow("Armor Trim", "minecraft:trim", leftX, pickerY + rowHeight, button -> openTrimPicker());
        addPickerRow("Dyed Color", "minecraft:dyed_color", leftX, pickerY + rowHeight * 2, button -> openDyedColorPicker());

        // Standard Buttons
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Save").formatted(Formatting.GREEN), button -> save())
                .dimensions(10, this.height - 30, 80, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.of("Back"), button -> close())
                .dimensions(100, this.height - 30, 80, 20).build());

        // Arrows button next to Unbreakable
        this.addDrawableChild(ButtonWidget.builder(Text.of("Arrows"), button -> {
            if (this.client != null) {
                this.client.setScreen(new ArrowsScreen(this, this.stack, newStack -> {
                    this.stack = newStack;
                }));
            }
        }).dimensions(rightX + 215, startY, 60, 20).build());

        // Consumable button below Arrows
        this.addDrawableChild(ButtonWidget.builder(Text.of("Consumable"), button -> {
            if (this.client != null) {
                this.client.setScreen(new ConsumableScreen(this, this.stack, newStack -> {
                    this.stack = newStack;
                }));
            }
        }).dimensions(rightX + 215, startY + rowHeight, 80, 20).build());
    }

    private void addPickerRow(String name, String iconKey, int x, int y, ButtonWidget.PressAction pressAction) {
        this.addDrawableChild(ButtonWidget.builder(Text.of("Choose"), pressAction)
                .dimensions(x + 150, y, 60, 20).build());
    }

    private void openDyedColorPicker() {
        int color = 0xFFFFFF;
        String snbt = getComponentSnbt("minecraft:dyed_color");
        if (snbt != null && snbt.contains("rgb:")) {
            try {
                String sub = snbt.substring(snbt.indexOf("rgb:") + 4);
                if (sub.contains(",")) sub = sub.substring(0, sub.indexOf(","));
                if (sub.contains("}")) sub = sub.substring(0, sub.indexOf("}"));
                color = Integer.parseInt(sub.trim());
            } catch (Exception e) {}
        }

        this.client.setScreen(new DyedColorPickerScreen(this, color, (newColor) -> {
            // Support both record format and primitive format for wider compatibility
            setComponent("minecraft:dyed_color", String.valueOf(newColor));
        }));
    }

    private void openTooltipPicker() {
        Set<String> hidden = new HashSet<>();
        String snbt = getComponentSnbt("minecraft:tooltip_display");
        if (snbt != null && snbt.contains("hidden_components:[")) {
            // Very basic parsing for established components
            for (String comp : Registries.DATA_COMPONENT_TYPE.getIds().stream().map(Identifier::toString).toList()) {
                if (snbt.contains("\"" + comp + "\"")) {
                    hidden.add(comp);
                }
            }
        }

        this.client.setScreen(new MultiComponentPickerScreen(this, hidden, keys -> {
            boolean hasHideTooltip = hasTooltipKey("hide_tooltip");
            if (keys.isEmpty() && !hasHideTooltip) {
                removeComponent("minecraft:tooltip_display");
            } else {
                StringBuilder sb = new StringBuilder("{");
                if (hasHideTooltip) sb.append("hide_tooltip:1b");
                if (!keys.isEmpty()) {
                    if (hasHideTooltip) sb.append(",");
                    sb.append("hidden_components:[");
                    sb.append(String.join(",", keys.stream().map(k -> "\"" + k + "\"").toList()));
                    sb.append("]");
                }
                sb.append("}");
                setComponent("minecraft:tooltip_display", sb.toString());
            }
        }));
    }

    private void openTrimPicker() {
        String pattern = null;
        String material = null;
        String snbt = getComponentSnbt("minecraft:trim");
        if (snbt != null) {
            // Rudimentary parsing
            if (snbt.contains("pattern:\"")) {
                int start = snbt.indexOf("pattern:\"") + 9;
                pattern = snbt.substring(start, snbt.indexOf("\"", start));
            }
            if (snbt.contains("material:\"")) {
                int start = snbt.indexOf("material:\"") + 10;
                material = snbt.substring(start, snbt.indexOf("\"", start));
            }
        }

        this.client.setScreen(new ArmorTrimPickerScreen(this, pattern, material, (pat, mat) -> {
            setComponent("minecraft:trim", "{pattern:\"" + pat + "\",material:\"" + mat + "\"}");
        }));
    }

    private void toggleSimpleComponent(String key, String snbt) {
        if (hasComponent(key)) {
            removeComponent(key);
        } else {
            setComponent(key, snbt);
        }
    }

    private void toggleTooltipKey(String modType) {
        boolean hasHideTooltip = hasTooltipKey("hide_tooltip");
        boolean hasHiddenComps = hasTooltipKey("hidden_components");

        if (modType.equals("hide_tooltip")) {
            hasHideTooltip = !hasHideTooltip;
        } else if (modType.equals("hide_additional")) {
            hasHiddenComps = !hasHiddenComps;
        }

        if (!hasHideTooltip && !hasHiddenComps) {
            removeComponent("minecraft:tooltip_display");
            return;
        }

        StringBuilder sb = new StringBuilder("{");
        if (hasHideTooltip) {
            sb.append("hide_tooltip:1b");
        }
        if (hasHiddenComps) {
            if (hasHideTooltip) sb.append(",");
            sb.append("hidden_components:[").append(ALL_HIDDEN_COMPONENTS).append("]");
        }
        sb.append("}");
        
        setComponent("minecraft:tooltip_display", sb.toString());
    }

    private boolean hasTooltipKey(String matchStr) {
        String snbt = getComponentSnbt("minecraft:tooltip_display");
        if (snbt == null) return false;
        return snbt.contains(matchStr);
    }
    
    private String getComponentSnbt(String key) {
        ComponentType type = Registries.DATA_COMPONENT_TYPE.get(Identifier.tryParse(key));
        if (type != null && stack.getComponents().contains(type)) {
            Object val = stack.get(type);
            try {
                if (this.client != null && this.client.world != null) {
                    var lookup = this.client.world.getRegistryManager();
                    var ops = net.minecraft.registry.RegistryOps.of(net.minecraft.nbt.NbtOps.INSTANCE, lookup);
                    NbtElement nbt = (NbtElement) type.getCodecOrThrow().encodeStart(ops, val).getOrThrow();
                    return nbt.toString();
                }
            } catch (Exception e) {}
            return val.toString();
        }
        return null;
    }

    private Text getToggleText(boolean isOn) {
        if (isOn) {
            return Text.literal("ON").formatted(Formatting.GREEN, Formatting.BOLD);
        } else {
            return Text.literal("OFF").formatted(Formatting.DARK_GRAY, Formatting.BOLD);
        }
    }

    private boolean hasComponent(String key) {
        ComponentType<?> type = Registries.DATA_COMPONENT_TYPE.get(Identifier.tryParse(key));
        return type != null && stack.getComponents().contains(type);
    }

    private void setComponent(String key, String snbt) {
        ComponentType type = Registries.DATA_COMPONENT_TYPE.get(Identifier.tryParse(key));
        if (type == null) return;
        try {
            if (this.client != null && this.client.world != null) {
                var lookup = this.client.world.getRegistryManager();
                var ops = net.minecraft.registry.RegistryOps.of(net.minecraft.nbt.NbtOps.INSTANCE, lookup);
                
                String wrapped = "{\"v\":" + snbt.trim() + "}";
                NbtCompound compound = NbtCompoundArgumentType.nbtCompound().parse(new StringReader(wrapped));
                NbtElement nbt = compound.get("v");
                
                if (nbt != null) {
                    var result = type.getCodecOrThrow().parse(ops, nbt);
                    if (result.result().isPresent()) {
                        stack.set(type, result.result().get());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void removeComponent(String key) {
        ComponentType<?> type = Registries.DATA_COMPONENT_TYPE.get(Identifier.tryParse(key));
        if (type != null) {
            stack.remove(type);
        }
    }

    private void save() {
        if (parent instanceof NbtSelectionScreen sel) {
            sel.refresh(stack);
        }
        if (saveCallback != null) {
            saveCallback.accept(stack);
        }
        close();
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
        context.fill(0, 0, this.width, this.height, 0x80000000);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 10, 0xFFFFFFFF);

        int startY = 40;
        int rowHeight = 25;
        int leftX = 20;
        int rightX = this.width / 2 + 20;

        // Render Numerical Labels
        for (int i = 0; i < numericals.size(); i++) {
            TextOption opt = numericals.get(i);
            int y = startY + (i * rowHeight);
            ItemStack icon = opt.iconKey != null ? ComponentIconUtil.getIcon(opt.iconKey) : new ItemStack(Items.BARRIER);
            if (opt.name.equals("Count")) icon = new ItemStack(stack.getItem());
            context.drawItem(icon, leftX, y + 2);
            context.drawTextWithShadow(this.textRenderer, Text.literal(opt.name), leftX + 25, y + 6, 0xFFFFFFFF);
        }

        // Render Toggle Labels
        for (int i = 0; i < toggles.size(); i++) {
            ToggleOption opt = toggles.get(i);
            int y = startY + (i * rowHeight);
            ItemStack icon = ComponentIconUtil.getIcon(opt.iconKey);
            context.drawItem(icon, rightX, y + 2);
            context.drawTextWithShadow(this.textRenderer, Text.literal(opt.name), rightX + 25, y + 6, 0xFFFFFFFF);
        }

        // Render Picker Labels
        int pickerY = startY + Math.max(numericals.size(), toggles.size()) * rowHeight + 20;
        renderPickerLabel("Tooltip Hides", "minecraft:item_frame", leftX, pickerY, context);
        renderPickerLabel("Armor Trim", "minecraft:trim", leftX, pickerY + rowHeight, context);
        renderPickerLabel("Dyed Color", "minecraft:dyed_color", leftX, pickerY + rowHeight * 2, context);

        context.drawItem(ComponentIconUtil.getIcon("minecraft:charged_projectiles"), rightX + 195, startY + 2);
        context.drawItem(ComponentIconUtil.getIcon("minecraft:consumable"), rightX + 195, startY + rowHeight + 2);
    }

    private void renderPickerLabel(String name, String iconKey, int x, int y, DrawContext context) {
        ItemStack icon = ComponentIconUtil.getIcon(iconKey);
        context.drawTextWithShadow(this.textRenderer, Text.literal(name), x, y + 6, 0xFFFFFFFF);
        context.drawItem(icon, x + 120, y + 2);
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }
}
