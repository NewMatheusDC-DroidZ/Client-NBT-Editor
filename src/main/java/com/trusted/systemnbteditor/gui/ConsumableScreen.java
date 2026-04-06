package com.trusted.systemnbteditor.gui;

import com.mojang.brigadier.StringReader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryOps;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.function.Consumer;

/**
 * Main editor screen for the minecraft:consumable data component.
 * Persists all values in fields to prevent state resets when navigating sub-screens.
 */
public class ConsumableScreen extends Screen {
    private final Screen parent;
    private final ItemStack stack;
    private final Consumer<ItemStack> saveCallback;

    // Persisted values (survive init() re-calls)
    private String consumeSeconds = "1.6";
    private String animation = "eat";
    private String soundEvent = "minecraft:entity.generic.eat";
    private String hasConsumeParticles = "true";
    private boolean clearAllEffects = false;
    private Set<String> removeEffects = new LinkedHashSet<>();
    private List<ApplyEffectPickerScreen.EffectEntry> applyEffects = new ArrayList<>();
    private String teleportRandomly = "";
    private String playSound = "";
    private boolean initialized = false;

    // Widgets (recreated in init)
    private TextFieldWidget consumeSecondsField;
    private TextFieldWidget teleportField;

    // Animation options
    private static final List<String> ANIMATION_OPTIONS = List.of(
        "none", "eat", "drink", "block", "bow", "spear", "crossbow", "spyglass", "toot_horn", "brush"
    );

    public ConsumableScreen(Screen parent, ItemStack stack, Consumer<ItemStack> saveCallback) {
        super(Text.of("Consumable Editor"));
        this.parent = parent;
        this.stack = stack.copy();
        this.saveCallback = saveCallback;
        parseExistingComponent();
    }

    private void parseExistingComponent() {
        try {
            String snbt = getComponentSnbt("minecraft:consumable");
            if (snbt == null) return;

            // Parse consume_seconds
            if (snbt.contains("consume_seconds:")) {
                String sub = extractField(snbt, "consume_seconds:");
                consumeSeconds = sub.replace("f", "").replace("d", "").trim();
            }

            // Parse animation
            if (snbt.contains("animation:")) {
                String sub = extractField(snbt, "animation:");
                animation = sub.replace("\"", "").replace("'", "").trim();
            }

            // Parse sound (could be sound_event or sound)
            if (snbt.contains("sound:\"") || snbt.contains("sound:'")) {
                String sub = extractField(snbt, "sound:");
                soundEvent = sub.replace("\"", "").replace("'", "").trim();
            }

            // Parse has_consume_particles
            if (snbt.contains("has_consume_particles:")) {
                String sub = extractField(snbt, "has_consume_particles:");
                hasConsumeParticles = sub.replace("b", "").trim();
            }

            // Parse on_consume_effects
            if (snbt.contains("on_consume_effects:")) {
                parseOnConsumeEffects(snbt);
            }
        } catch (Exception e) {
            System.err.println("[ConsumableScreen] Error parsing existing consumable: " + e.getMessage());
        }
        initialized = true;
    }

    private void parseOnConsumeEffects(String snbt) {
        // Basic parsing for clear_all_effects, teleport_randomly, play_sound
        if (snbt.contains("clear_all_effects")) {
            clearAllEffects = true;
        }
        if (snbt.contains("teleport_randomly")) {
            try {
                String sub = snbt.substring(snbt.indexOf("diameter:") + 9);
                if (sub.contains(",")) sub = sub.substring(0, sub.indexOf(","));
                if (sub.contains("}")) sub = sub.substring(0, sub.indexOf("}"));
                teleportRandomly = sub.replace("f", "").replace("d", "").trim();
            } catch (Exception e) {}
        }
    }

    private String extractField(String snbt, String fieldPrefix) {
        int start = snbt.indexOf(fieldPrefix) + fieldPrefix.length();
        String sub = snbt.substring(start);
        if (sub.startsWith("\"")) {
            int end = sub.indexOf("\"", 1);
            return sub.substring(0, end + 1);
        }
        if (sub.startsWith("'")) {
            int end = sub.indexOf("'", 1);
            return sub.substring(0, end + 1);
        }
        if (sub.contains(",")) sub = sub.substring(0, sub.indexOf(","));
        if (sub.contains("}")) sub = sub.substring(0, sub.indexOf("}"));
        return sub.trim();
    }

    private String getComponentSnbt(String key) {
        ComponentType type = Registries.DATA_COMPONENT_TYPE.get(Identifier.tryParse(key));
        if (type != null && stack.getComponents().contains(type)) {
            Object val = stack.get(type);
            try {
                if (this.client != null && this.client.world != null) {
                    var lookup = this.client.world.getRegistryManager();
                    var ops = RegistryOps.of(net.minecraft.nbt.NbtOps.INSTANCE, lookup);
                    NbtElement nbt = (NbtElement) type.getCodecOrThrow().encodeStart(ops, val).getOrThrow();
                    return nbt.toString();
                }
            } catch (Exception e) {}
            if (val != null) return val.toString();
        }
        return null;
    }

    @Override
    protected void init() {
        super.init();
        int startY = 35;
        int leftX = 20;
        int fieldW = 80;
        int rowH = 24;
        int row = 0;

        // consume_seconds
        consumeSecondsField = new TextFieldWidget(this.textRenderer, leftX + 130, startY + row * rowH, fieldW, 18, Text.of("Seconds"));
        consumeSecondsField.setText(consumeSeconds);
        consumeSecondsField.setChangedListener(s -> consumeSeconds = s);
        this.addDrawableChild(consumeSecondsField);
        row++;

        // animation
        this.addDrawableChild(ButtonWidget.builder(Text.literal(animation).formatted(Formatting.YELLOW), button -> {
            if (this.client != null) {
                this.client.setScreen(new StringOptionPickerScreen(this, "Pick Animation", ANIMATION_OPTIONS, animation, val -> {
                    animation = val;
                }));
            }
        }).dimensions(leftX + 130, startY + row * rowH, fieldW + 30, 18).build());
        row++;

        // sound_event
        this.addDrawableChild(ButtonWidget.builder(Text.literal(shortenId(soundEvent)).formatted(Formatting.YELLOW), button -> {
            if (this.client != null) {
                List<String> sounds = getSoundEvents();
                this.client.setScreen(new StringOptionPickerScreen(this, "Pick Sound Event", sounds, soundEvent, val -> {
                    soundEvent = val;
                }));
            }
        }).dimensions(leftX + 130, startY + row * rowH, fieldW + 50, 18).build());
        row++;

        // has_consume_particles
        this.addDrawableChild(ButtonWidget.builder(Text.literal(hasConsumeParticles).formatted(
                hasConsumeParticles.equals("true") ? Formatting.GREEN : Formatting.RED
        ), button -> {
            if (this.client != null) {
                this.client.setScreen(new StringOptionPickerScreen(this, "Has Consume Particles", List.of("true", "false"), hasConsumeParticles, val -> {
                    hasConsumeParticles = val;
                }));
            }
        }).dimensions(leftX + 130, startY + row * rowH, fieldW, 18).build());
        row++;

        // --- on_consume_effects header ---
        row++;

        // clear_all_effects
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal(clearAllEffects ? "ON" : "OFF").formatted(clearAllEffects ? Formatting.GREEN : Formatting.DARK_GRAY),
            button -> {
                clearAllEffects = !clearAllEffects;
                button.setMessage(Text.literal(clearAllEffects ? "ON" : "OFF").formatted(clearAllEffects ? Formatting.GREEN : Formatting.DARK_GRAY));
            }
        ).dimensions(leftX + 130, startY + row * rowH, 50, 18).build());
        row++;

        // remove_effects
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Select (" + removeEffects.size() + ")").formatted(Formatting.AQUA), button -> {
            if (this.client != null) {
                this.client.setScreen(new EffectPickerScreen(this, removeEffects, selected -> {
                    removeEffects = selected;
                }));
            }
        }).dimensions(leftX + 130, startY + row * rowH, fieldW + 30, 18).build());
        row++;

        // apply_effects
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Edit (" + applyEffects.size() + ")").formatted(Formatting.AQUA), button -> {
            if (this.client != null) {
                this.client.setScreen(new ApplyEffectPickerScreen(this, applyEffects, entries -> {
                    applyEffects = new ArrayList<>(entries);
                }));
            }
        }).dimensions(leftX + 130, startY + row * rowH, fieldW + 30, 18).build());
        row++;

        // teleport_randomly
        teleportField = new TextFieldWidget(this.textRenderer, leftX + 130, startY + row * rowH, fieldW, 18, Text.of("Diameter"));
        teleportField.setText(teleportRandomly);
        teleportField.setChangedListener(s -> teleportRandomly = s);
        this.addDrawableChild(teleportField);
        row++;

        // play_sound
        this.addDrawableChild(ButtonWidget.builder(Text.literal(playSound.isEmpty() ? "unset" : shortenId(playSound)).formatted(Formatting.YELLOW), button -> {
            if (this.client != null) {
                List<String> sounds = getSoundEvents();
                this.client.setScreen(new StringOptionPickerScreen(this, "Pick Play Sound", sounds, playSound, val -> {
                    playSound = val;
                }));
            }
        }).dimensions(leftX + 130, startY + row * rowH, fieldW + 50, 18).build());
        row++;

        row++;
        // Save / Cancel buttons
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Save").formatted(Formatting.GREEN), button -> {
            save();
        }).dimensions(leftX, startY + row * rowH, 80, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.of("Cancel"), button -> {
            close();
        }).dimensions(leftX + 90, startY + row * rowH, 80, 20).build());
    }

    private String shortenId(String id) {
        if (id == null || id.isEmpty()) return "unset";
        if (id.startsWith("minecraft:")) {
            String s = id.substring("minecraft:".length());
            if (s.length() > 18) s = s.substring(0, 18) + "..";
            return s;
        }
        if (id.length() > 22) return id.substring(0, 22) + "..";
        return id;
    }

    private List<String> getSoundEvents() {
        List<String> sounds = new ArrayList<>();
        try {
            for (Identifier id : Registries.SOUND_EVENT.getIds()) {
                sounds.add(id.toString());
            }
        } catch (Exception e) {}
        if (sounds.isEmpty()) {
            sounds.add("minecraft:entity.generic.eat");
            sounds.add("minecraft:entity.generic.drink");
            sounds.add("minecraft:entity.player.burp");
        }
        Collections.sort(sounds);
        return sounds;
    }

    private void save() {
        StringBuilder sb = new StringBuilder("{");
        sb.append("consume_seconds:").append(consumeSeconds).append("f");
        sb.append(",animation:\"").append(animation).append("\"");
        sb.append(",sound:\"").append(soundEvent).append("\"");
        sb.append(",has_consume_particles:").append(hasConsumeParticles.equals("true") ? "1b" : "0b");

        // Build on_consume_effects
        List<String> effectsList = new ArrayList<>();

        if (clearAllEffects) {
            effectsList.add("{type:\"minecraft:clear_all_effects\"}");
        }

        if (!removeEffects.isEmpty()) {
            StringBuilder remSb = new StringBuilder("{type:\"minecraft:remove_effects\",effects:[");
            List<String> effectIds = new ArrayList<>(removeEffects);
            for (int i = 0; i < effectIds.size(); i++) {
                remSb.append("\"").append(effectIds.get(i)).append("\"");
                if (i < effectIds.size() - 1) remSb.append(",");
            }
            remSb.append("]}");
            effectsList.add(remSb.toString());
        }

        if (!applyEffects.isEmpty()) {
            StringBuilder appSb = new StringBuilder("{type:\"minecraft:apply_effects\",effects:[");
            for (int i = 0; i < applyEffects.size(); i++) {
                ApplyEffectPickerScreen.EffectEntry e = applyEffects.get(i);
                appSb.append("{id:\"").append(e.effectId()).append("\"");
                appSb.append(",amplifier:").append(e.amplifier()).append("b");
                appSb.append(",duration:").append(e.duration());
                if (e.ambient()) appSb.append(",ambient:1b");
                if (e.hideParticles()) appSb.append(",show_particles:0b");
                if (e.hideIcon()) appSb.append(",show_icon:0b");
                appSb.append("}");
                if (i < applyEffects.size() - 1) appSb.append(",");
            }
            appSb.append("],probability:1.0f}");
            effectsList.add(appSb.toString());
        }

        if (!teleportRandomly.isEmpty()) {
            try {
                float diameter = Float.parseFloat(teleportRandomly);
                effectsList.add("{type:\"minecraft:teleport_randomly\",diameter:" + diameter + "f}");
            } catch (Exception e) {}
        }

        if (!playSound.isEmpty()) {
            effectsList.add("{type:\"minecraft:play_sound\",sound:\"" + playSound + "\"}");
        }

        if (!effectsList.isEmpty()) {
            sb.append(",on_consume_effects:[");
            sb.append(String.join(",", effectsList));
            sb.append("]");
        }

        sb.append("}");

        // Set the component
        setComponent("minecraft:consumable", sb.toString());

        if (saveCallback != null) {
            saveCallback.accept(stack);
        }
        close();
    }

    private void setComponent(String key, String snbt) {
        ComponentType type = Registries.DATA_COMPONENT_TYPE.get(Identifier.tryParse(key));
        if (type == null) return;
        try {
            if (this.client != null && this.client.world != null) {
                var lookup = this.client.world.getRegistryManager();
                var ops = RegistryOps.of(net.minecraft.nbt.NbtOps.INSTANCE, lookup);

                String wrapped = "{\"v\":" + snbt.trim() + "}";
                NbtCompound compound = net.minecraft.command.argument.NbtCompoundArgumentType.nbtCompound().parse(new StringReader(wrapped));
                NbtElement nbt = compound.get("v");

                if (nbt != null) {
                    var result = type.getCodecOrThrow().parse(ops, nbt);
                    if (result.result().isPresent()) {
                        stack.set(type, result.result().get());
                    } else if (result.error().isPresent()) {
                        System.err.println("[ConsumableScreen] Parse error: " + result.error().get());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[ConsumableScreen] Error setting component: " + e.getMessage());
            e.printStackTrace();
        }
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

        int startY = 35;
        int leftX = 20;
        int rowH = 24;
        int row = 0;

        int labelColor = 0xFFFFAA00;
        int infoColor = 0xFF888888;

        drawLabel(context, "consume_seconds", leftX, startY + row * rowH, labelColor);
        row++;
        drawLabel(context, "animation", leftX, startY + row * rowH, labelColor);
        row++;
        drawLabel(context, "sound_event", leftX, startY + row * rowH, labelColor);
        row++;
        drawLabel(context, "has_consume_particles", leftX, startY + row * rowH, labelColor);
        row++;

        // on_consume_effects header
        context.drawTextWithShadow(this.textRenderer, Text.literal("on_consume_effects").formatted(Formatting.GOLD, Formatting.UNDERLINE), leftX, startY + row * rowH + 4, 0xFFFFAA00);
        row++;

        drawLabel(context, "  clear_all_effects", leftX, startY + row * rowH, infoColor);
        row++;
        drawLabel(context, "  remove_effects", leftX, startY + row * rowH, infoColor);
        row++;
        drawLabel(context, "  apply_effects", leftX, startY + row * rowH, infoColor);
        row++;
        drawLabel(context, "  teleport_randomly", leftX, startY + row * rowH, infoColor);
        row++;
        drawLabel(context, "  play_sound", leftX, startY + row * rowH, infoColor);
    }

    private void drawLabel(DrawContext context, String text, int x, int y, int color) {
        context.drawTextWithShadow(this.textRenderer, Text.literal(text), x, y + 5, color);
    }

    @Override
    public void close() {
        if (this.client != null) this.client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
