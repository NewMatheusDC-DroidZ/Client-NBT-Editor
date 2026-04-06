package com.trusted.systemnbteditor.gui;

import com.trusted.systemnbteditor.data.ModConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.*;


import java.util.function.Supplier;

/**
 * Config screen that visually resembles the NBT Editor's scrollable config layout.
 * Uses a manual scroll + custom draw approach to avoid ElementListWidget mapping issues.
 */
public class SystemNbtEditorConfigScreen extends Screen {

    private static final int ROW_HEIGHT = 22;
    private static final int CATEGORY_HEIGHT = 24;
    private static final int LEFT_MARGIN = 40;
    private static final int PADDING = 4;

    private final Screen parent;
    private final ModConfig config = ModConfig.getInstance();

    // Scroll state
    private int scrollOffset = 0;
    private int contentHeight = 0;

    // Config rows (built in init)
    private final List<ConfigRow> rows = new ArrayList<>();

    // Widgets that need mouse/keyboard delegation
    private final List<ButtonWidget> buttons = new ArrayList<>();
    private ConfigSliderWidget scrollSlider;
    private int listTop, listBottom;

    public SystemNbtEditorConfigScreen(Screen parent) {
        super(Text.of("NBT Editor Config"));
        this.parent = parent;
    }

    // ── categories ──────────────────────────────────────────────────────────────

    private static final List<String> ENCHANT_MODES  = List.of("NEVER","NOT_MAX","NOT_EXACT","ALWAYS");
    private static final List<String> ITEM_SIZES = List.of("HIDDEN","AUTO","BYTE","KILOBYTE","MEGABYTE","GIGABYTE",
            "AUTO_COMPRESSED","BYTE_COMPRESSED","KILOBYTE_COMPRESSED","MEGABYTE_COMPRESSED","GIGABYTE_COMPRESSED");

    @Override
    protected void init() {
        super.init();
        rows.clear();
        buttons.clear();
        scrollOffset = 0;

        listTop    = 32;
        listBottom = this.height - 40;

        // ── Minecraft Tweaks ──
        rows.add(new CategoryRow("Minecraft Tweaks"));
        rows.add(new KeybindRow("Editor Keybind",
                () -> config.editorKeybind,
                v  -> { config.editorKeybind = v; ModConfig.save(); }));
        rows.add(new ToggleRow("Show Editor Suggestions",
                () -> config.showEditorSuggestions ? "Enabled" : "Disabled",
                () -> { config.showEditorSuggestions = !config.showEditorSuggestions; ModConfig.save(); }));
        rows.add(new ToggleRow("Tooltip Overflow Fix",
                () -> config.tooltipOverflowFix ? "Enabled" : "Disabled",
                () -> { config.tooltipOverflowFix = !config.tooltipOverflowFix; ModConfig.save(); }));
        rows.add(new CycleRow("Max Enchant Level Display", ENCHANT_MODES,
                () -> config.maxEnchantLevelDisplay,
                v  -> { config.maxEnchantLevelDisplay = v; ModConfig.save(); }));
        rows.add(new ToggleRow("Enchant Number Type",
                () -> config.useArabicEnchantLevels ? "Arabic" : "Roman",
                () -> { config.useArabicEnchantLevels = !config.useArabicEnchantLevels; ModConfig.save(); }));
        rows.add(new ToggleRow("No Slot Restrictions",
                () -> config.noSlotRestrictions ? "Enabled" : "Disabled",
                () -> { config.noSlotRestrictions = !config.noSlotRestrictions; ModConfig.save(); }));
        rows.add(new ToggleRow("Enchant Glint Fix",
                () -> config.enchantGlintFix ? "Enabled" : "Disabled",
                () -> { config.enchantGlintFix = !config.enchantGlintFix; ModConfig.save(); }));

        // ── GUIs ──
        rows.add(new CategoryRow("GUIs"));
        rows.add(new CycleRow("Item Size", ITEM_SIZES,
                () -> config.itemSize,
                v  -> { config.itemSize = v; ModConfig.save(); }));
        rows.add(new ToggleRow("Special Numbers",
                () -> config.specialNumbers ? "Enabled" : "Disabled",
                () -> { config.specialNumbers = !config.specialNumbers; ModConfig.save(); }));
        rows.add(new SliderRow("Scroll Speed"));

        // ── build button widgets for all rows ──
        int rowWidth = Math.min(460, this.width - LEFT_MARGIN * 2);
        int btnW = 140;
        int btnX = LEFT_MARGIN + rowWidth - btnW;

        for (ConfigRow row : rows) row.build(btnX, btnW, this);

        // ── Done button ──
        this.addDrawableChild(ButtonWidget.builder(Text.of("Done"), b -> close())
                .dimensions(this.width / 2 - 100, this.height - 30, 200, 20)
                .build());

        recomputeContentHeight();
    }

    private void recomputeContentHeight() {
        contentHeight = rows.stream().mapToInt(r -> r.height()).sum();
    }

    // ── rendering ───────────────────────────────────────────────────────────────

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx, mouseX, mouseY, delta);

        ctx.drawCenteredTextWithShadow(textRenderer, this.title, this.width / 2, 10, 0xFFFFFF);

        // Clip to list area with scissor
        ctx.enableScissor(0, listTop, this.width, listBottom);

        int rowWidth = Math.min(460, this.width - LEFT_MARGIN * 2);
        int y = listTop - scrollOffset;

        for (ConfigRow row : rows) {
            if (y + row.height() > listTop && y < listBottom) {
                // Pass a padded x to make sure we don't start text off the screen or blended
                row.render(ctx, LEFT_MARGIN, y, rowWidth, mouseX, mouseY, delta);
            }
            y += row.height();
        }

        ctx.disableScissor();

        // Scrollbar
        if (contentHeight > listBottom - listTop) {
            int sbHeight = listBottom - listTop;
            int thumbH = Math.max(20, sbHeight * sbHeight / contentHeight);
            int maxScroll = contentHeight - (listBottom - listTop);
            int thumbY = listTop + (scrollOffset * (sbHeight - thumbH)) / maxScroll;
            ctx.fill(this.width - 8, listTop, this.width - 4, listBottom, 0x44FFFFFF);
            ctx.fill(this.width - 8, thumbY, this.width - 4, thumbY + thumbH, 0xAAAAAAAA);
        }

        // Render child buttons (Done, etc.)
        for (net.minecraft.client.gui.Element e : this.children()) {
            if (e instanceof ButtonWidget bw) bw.render(ctx, mouseX, mouseY, delta);
        }
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Do NOT call super.renderBackground() — it triggers a background blur that
        // conflicts with the parent screen's blur and causes "Can only blur once per frame".
        ctx.fillGradient(0, 0, this.width, this.height, 0xC0101010, 0xD0101010);
    }

    // ── input ───────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseScrolled(double mx, double my, double hx, double hy) {
        if (my >= listTop && my <= listBottom) {
            scrollOffset = clampScroll(scrollOffset - (int)(hy * 8));
            return true;
        }
        return super.mouseScrolled(mx, my, hx, hy);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean bl) {
        double mx = click.x();
        double my = click.y();
        int button = click.button();
        if (my >= listTop && my <= listBottom) {
            int rowWidth = Math.min(460, this.width - LEFT_MARGIN * 2);
            int y = listTop - scrollOffset;
            for (ConfigRow row : rows) {
                if (my >= y && my < y + row.height()) {
                    row.mouseClicked(mx, my, button, LEFT_MARGIN, y, rowWidth);
                    return true;
                }
                y += row.height();
            }
        }
        return super.mouseClicked(click, bl);
    }

    @Override
    public boolean mouseDragged(net.minecraft.client.gui.Click click, double dx, double dy) {
        double mx = click.x();
        double my = click.y();
        int button = click.button();
        int rowWidth = Math.min(460, this.width - LEFT_MARGIN * 2);
        int y = listTop - scrollOffset;
        for (ConfigRow row : rows) {
            row.mouseDragged(mx, my, button, dx, dy, LEFT_MARGIN, y, rowWidth);
            y += row.height();
        }
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(net.minecraft.client.gui.Click click) {
        double mx = click.x();
        double my = click.y();
        int button = click.button();
        int rowWidth = Math.min(460, this.width - LEFT_MARGIN * 2);
        int y = listTop - scrollOffset;
        for (ConfigRow row : rows) {
            row.mouseReleased(mx, my, button, LEFT_MARGIN, y, rowWidth);
            y += row.height();
        }
        return super.mouseReleased(click);
    }

    @Override
    public void close() {
        if (this.client != null) this.client.setScreen(this.parent);
    }

    private int clampScroll(int val) {
        int maxScroll = Math.max(0, contentHeight - (listBottom - listTop));
        return Math.max(0, Math.min(val, maxScroll));
    }

    static String fmt(String val) {
        StringBuilder sb = new StringBuilder();
        for (String p : val.toLowerCase().split("_"))
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(" ");
        return sb.toString().trim().replace(" Compressed", " (Compressed)");
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Row types
    // ────────────────────────────────────────────────────────────────────────────

    interface ConfigRow {
        int height();
        void render(DrawContext ctx, int x, int y, int w, int mx, int my, float delta);
        default void build(int btnX, int btnW, SystemNbtEditorConfigScreen screen) {}
        default void mouseClicked(double mx, double my, int btn, int x, int y, int w) {}
        default void mouseDragged(double mx, double my, int btn, double dx, double dy, int x, int y, int w) {}
        default void mouseReleased(double mx, double my, int btn, int x, int y, int w) {}
    }

    // ── Override keyPressed logic to capture keys for KeybindRow ──
    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyInput input) {
        int keyCode = input.key();
        int modifiers = input.modifiers();
        for (ConfigRow row : rows) {
            if (row instanceof KeybindRow kr && kr.listening) {
                if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE && modifiers == 0) {
                    kr.setter.accept(new java.util.ArrayList<>()); // Unbind
                    kr.listening = false;
                    return true;
                }
                java.util.List<Integer> keys = new java.util.ArrayList<>();
                if ((modifiers & org.lwjgl.glfw.GLFW.GLFW_MOD_CONTROL) != 0 && keyCode != org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL && keyCode != org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_CONTROL) keys.add(org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL);
                if ((modifiers & org.lwjgl.glfw.GLFW.GLFW_MOD_SHIFT) != 0 && keyCode != org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT && keyCode != org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT) keys.add(org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT);
                if ((modifiers & org.lwjgl.glfw.GLFW.GLFW_MOD_ALT) != 0 && keyCode != org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_ALT && keyCode != org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_ALT) keys.add(org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_ALT);
                if ((modifiers & org.lwjgl.glfw.GLFW.GLFW_MOD_SUPER) != 0 && keyCode != org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SUPER && keyCode != org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SUPER) keys.add(org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SUPER);
                keys.add(keyCode);
                
                kr.setter.accept(keys);
                kr.listening = false;
                return true;
            }
        }
        return super.keyPressed(input);
    }
    
    // ── KeybindRow implementation ──
    private static class KeybindRow implements ConfigRow {
        private final String label;
        private final Supplier<java.util.List<Integer>> getter;
        private final java.util.function.Consumer<java.util.List<Integer>> setter;
        private ButtonWidget button;
        boolean listening = false;

        public KeybindRow(String label, Supplier<java.util.List<Integer>> getter, java.util.function.Consumer<java.util.List<Integer>> setter) {
            this.label = label;
            this.getter = getter;
            this.setter = setter;
            this.button = ButtonWidget.builder(Text.of(getKeysName(getter.get())), btn -> {
                listening = true;
                btn.setMessage(Text.literal("> ").append(Text.literal(getKeysName(getter.get())).formatted(net.minecraft.util.Formatting.YELLOW, net.minecraft.util.Formatting.UNDERLINE)).append(" <"));
            }).dimensions(0, 0, 150, ROW_HEIGHT - 4).build();
        }

        @Override
        public int height() { return ROW_HEIGHT; }

        @Override
        public void render(DrawContext ctx, int x, int y, int rowWidth, int mx, int my, float delta) {
            net.minecraft.client.font.TextRenderer tr = net.minecraft.client.MinecraftClient.getInstance().textRenderer;
            ctx.drawTextWithShadow(tr, Text.of(label), x, y + (height() - tr.fontHeight) / 2, 0xFFFFFFFF);
            if (button != null) {
                int textW = tr.getWidth(label);
                button.setX(x + textW + 8);
                button.setY(y + 2);
                if (!listening) {
                    button.setMessage(Text.of(getKeysName(getter.get())));
                }
                button.render(ctx, mx, my, delta);
            }
        }

        @Override
        public void mouseClicked(double mx, double my, int b, int x, int y, int w) {
            if (b == 0 && button != null && button.isMouseOver(mx, my)) {
                listening = true;
                button.setMessage(Text.literal("> ").append(Text.literal(getKeysName(getter.get())).formatted(net.minecraft.util.Formatting.YELLOW, net.minecraft.util.Formatting.UNDERLINE)).append(" <"));
                net.minecraft.client.MinecraftClient.getInstance().getSoundManager().play(net.minecraft.client.sound.PositionedSoundInstance.master(net.minecraft.sound.SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }
        }

        private String getKeysName(java.util.List<Integer> keys) {
            if (keys == null || keys.isEmpty()) return "NONE";
            return keys.stream().map(this::getKeyName).collect(java.util.stream.Collectors.joining(" + "));
        }

        private String getKeyName(int keyCode) {
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN) return "NONE";
            try {
                String name = org.lwjgl.glfw.GLFW.glfwGetKeyName(keyCode, org.lwjgl.glfw.GLFW.glfwGetKeyScancode(keyCode));
                if (name != null) return name.toUpperCase();
                // Fallbacks
                if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE) return "SPACE";
                if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT) return "LSHIFT";
                if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT) return "RSHIFT";
                if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_TAB) return "TAB";
                if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER) return "ENTER";
            } catch (Exception ignored) {}
            return "KEY_" + keyCode;
        }
    }

    class CategoryRow implements ConfigRow {
        private final String label;
        CategoryRow(String label) { this.label = label; }

        @Override public int height() { return CATEGORY_HEIGHT; }

        @Override public void render(DrawContext ctx, int x, int y, int w, int mx, int my, float delta) {
            ctx.drawTextWithShadow(textRenderer, Text.of(label), x, y + (height() - textRenderer.fontHeight) / 2, 0xFFFFFFFF);
        }
    }

    class ToggleRow implements ConfigRow {
        private final String label;
        private final Supplier<String> stateReader;
        private final Runnable action;
        private ButtonWidget btn;

        ToggleRow(String label, Supplier<String> stateReader, Runnable action) {
            this.label = label;
            this.stateReader = stateReader;
            this.action = action;
            btn = ButtonWidget.builder(Text.of(stateReader.get()), b -> {
                action.run();
                b.setMessage(Text.of(stateReader.get()));
            }).dimensions(0, 0, 150, ROW_HEIGHT - 4).build();
        }

        @Override public int height() { return ROW_HEIGHT; }

        @Override public void render(DrawContext ctx, int x, int y, int w, int mx, int my, float delta) {
            ctx.drawTextWithShadow(textRenderer, Text.of(label), x, y + (height() - textRenderer.fontHeight) / 2, 0xFFFFFFFF);
            int textW = textRenderer.getWidth(label);
            btn.setX(x + textW + 8);
            btn.setY(y + 2);
            btn.render(ctx, mx, my, delta);
        }

        @Override public void mouseClicked(double mx, double my, int b, int x, int y, int w) {
            if (btn.isMouseOver(mx, my)) {
                action.run();
                btn.setMessage(Text.of(stateReader.get()));
            }
        }
    }

    class CycleRow implements ConfigRow {
        private final String label;
        private final List<String> values;
        private final Supplier<String> getter;
        private final java.util.function.Consumer<String> setter;
        private ButtonWidget btn;

        CycleRow(String label, List<String> values, Supplier<String> getter, java.util.function.Consumer<String> setter) {
            this.label = label; this.values = values; this.getter = getter; this.setter = setter;
        }

        private void cycle() {
            int i = values.indexOf(getter.get());
            setter.accept(values.get((i + 1) % values.size()));
            btn.setMessage(Text.of(fmt(getter.get())));
        }

        @Override public void build(int btnX, int btnW, SystemNbtEditorConfigScreen s) {
            btn = ButtonWidget.builder(Text.of(fmt(getter.get())), b -> cycle())
                    .dimensions(0, 0, btnW, ROW_HEIGHT - 4).build();
        }

        @Override public int height() { return ROW_HEIGHT; }

        @Override public void render(DrawContext ctx, int x, int y, int w, int mx, int my, float delta) {
            ctx.drawTextWithShadow(textRenderer, Text.of(label), x, y + (height() - textRenderer.fontHeight) / 2, 0xFFFFFFFF);
            int textW = textRenderer.getWidth(label);
            btn.setX(x + textW + 8);
            btn.setY(y + 2);
            btn.render(ctx, mx, my, delta);
        }

        @Override public void mouseClicked(double mx, double my, int b, int x, int y, int w) {
            if (btn.isMouseOver(mx, my)) cycle();
        }
    }

    class SliderRow implements ConfigRow {
        private final String label;
        private net.minecraft.client.gui.widget.SliderWidget slider;

        SliderRow(String label) { this.label = label; }

        @Override public void build(int btnX, int btnW, SystemNbtEditorConfigScreen s) {
            slider = new net.minecraft.client.gui.widget.SliderWidget(0, 0, btnW, ROW_HEIGHT - 4,
                    Text.of(String.format("%.2f", config.scrollSpeed)), (config.scrollSpeed - 0.5) / 9.5) {
                @Override protected void updateMessage() {
                    setMessage(Text.of(String.format("%.2f", 0.5 + value * 9.5)));
                }
                @Override protected void applyValue() {
                    config.scrollSpeed = 0.5 + value * 9.5; ModConfig.save();
                }
            };
        }

        @Override public int height() { return ROW_HEIGHT; }

        @Override public void render(DrawContext ctx, int x, int y, int w, int mx, int my, float delta) {
            ctx.drawTextWithShadow(textRenderer, Text.of(label), x, y + (height() - textRenderer.fontHeight) / 2, 0xFFFFFFFF);
            int textW = textRenderer.getWidth(label);
            slider.setX(x + textW + 8);
            slider.setY(y + 2);
            slider.render(ctx, mx, my, delta);
        }

        @Override public void mouseClicked(double mx, double my, int b, int x, int y, int w) {
            // Slider handled by render hover
        }
        @Override public void mouseDragged(double mx, double my, int b, double dx, double dy, int x, int y, int w) {
            // Slider drag needs Click API - delegated via Screen.mouseDragged
        }
        @Override public void mouseReleased(double mx, double my, int b, int x, int y, int w) {
            // Nothing needed
        }
    }

    // Dummy (not used)
    private static class ConfigSliderWidget extends net.minecraft.client.gui.widget.SliderWidget {
        ConfigSliderWidget(int x, int y, int w, int h, Text t, double v) { super(x,y,w,h,t,v); }
        @Override protected void updateMessage() {}
        @Override protected void applyValue() {}
    }
}
