package com.trusted.systemnbteditor.gui;

import com.trusted.systemnbteditor.data.ComponentRegistry.ComponentInfo;
import com.mojang.brigadier.StringReader;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import java.io.File;
import java.lang.reflect.Method;

public class ComponentEditorScreen extends Screen {
    private final Screen parent;
    private final ComponentInfo component;
    private final ComponentSaveCallback callback;
    private EditBoxWidget editor;
    private Identifier themeTextureId;
    private NativeImageBackedTexture themeTexture;
    private int themeTextureWidth = 1;
    private int themeTextureHeight = 1;
    private boolean draggingScrollbar = false;

    public interface ComponentSaveCallback {
        void onComponentSaved(String componentKey, String componentValue);
    }

    public ComponentEditorScreen(Screen parent, ComponentInfo component, ComponentSaveCallback callback) {
        super(Text.of("Edit Component: " + component.displayName()));
        this.parent = parent;
        this.component = component;
        this.callback = callback;
    }

    @Override
    protected void init() {
        super.init();
        loadThemeTexture();

        // Create editor - smaller than main NBT editor
        int editorX = this.width / 4;
        int editorY = 60;
        int editorWidth = this.width / 2;
        int editorHeight = this.height - 150;

        this.editor = new EditBoxWidget.Builder()
                .x(editorX)
                .y(editorY)
                .build(this.textRenderer, editorWidth, editorHeight, Text.of("Component Value"));

        this.editor.setMaxLength(Integer.MAX_VALUE);
        this.editor.setText(component.defaultValue());

        this.addSelectableChild(this.editor);

        // Save Component button
        int buttonY = this.height - 60;
        this.addDrawableChild(ButtonWidget.builder(Text.of("Save Component"), button -> {
            String value = this.editor.getText().trim();

            // Validate NBT syntax
            if (isValidNbt(value)) {
                callback.onComponentSaved(component.key(), value);
                this.close();
            } else {
                // Show error (for now, just don't close)
                // TODO: Add error message display
            }
        }).dimensions(this.width / 2 - 60, buttonY, 120, 20).build());

        // Cancel button
        this.addDrawableChild(ButtonWidget.builder(Text.of("Cancel"), button -> {
            this.close();
        }).dimensions(this.width / 2 - 60, buttonY + 25, 120, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.of("Toggle Black"), button -> {
            com.trusted.systemnbteditor.data.ModConfig config = com.trusted.systemnbteditor.data.ModConfig.getInstance();
            config.showEditorBackground = !config.showEditorBackground;
            com.trusted.systemnbteditor.data.ModConfig.save();
            System.out.println("Debug: Toggle Black clicked (Component). New state: " + config.showEditorBackground);
            if (this.client != null && this.client.player != null) {
                this.client.player.sendMessage(Text.literal("Editor Background: " + (config.showEditorBackground ? "Solid Black" : "Transparent Theme")).formatted(net.minecraft.util.Formatting.YELLOW), true);
            }
        }).dimensions(this.width - 100, this.height - 30, 80, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (themeTextureId != null) {
            int x = editor.getX();
            int y = editor.getY();
            int w = editor.getWidth();
            int h = editor.getHeight();
            
            try {
                Method shaderColorMethod = com.mojang.blaze3d.systems.RenderSystem.class.getMethod("setShaderColor", float.class, float.class, float.class, float.class);
                // Dim by 40% = 60% brightness
                shaderColorMethod.invoke(null, 0.6f, 0.6f, 0.6f, 1.0f);
            } catch (Exception e) {}
            
            try {
                for (Method m : context.getClass().getMethods()) {
                    if (m.getName().equals("drawTexture") || m.getName().equals("method_51450")) {
                        Class<?>[] params = m.getParameterTypes();
                        if (params.length >= 9 && params[0] == Identifier.class) {
                            // "Cover" logic
                            float ratioW = (float) w / themeTextureWidth;
                            float ratioH = (float) h / themeTextureHeight;
                            float scale = Math.max(ratioW, ratioH);
                            
                            float visibleW = w / scale;
                            float visibleH = h / scale;
                            float u = (themeTextureWidth - visibleW) / 2f;
                            float v = (themeTextureHeight - visibleH) / 2f;
                            
                            m.invoke(context, themeTextureId, x, y, u, v, w, h, themeTextureWidth, themeTextureHeight);
                            break;
                        } else if (params.length >= 7 && params[0] == Identifier.class) {
                             m.invoke(context, themeTextureId, x, y, 0, 0, w, h);
                             break;
                        }
                    }
                }
            } catch (Exception e) {}
            
            try {
                Method shaderColorMethod = com.mojang.blaze3d.systems.RenderSystem.class.getMethod("setShaderColor", float.class, float.class, float.class, float.class);
                shaderColorMethod.invoke(null, 1.0f, 1.0f, 1.0f, 1.0f);
            } catch (Exception e) {}
        }

        super.render(context, mouseX, mouseY, delta);
        
        boolean showBackground = com.trusted.systemnbteditor.data.ModConfig.getInstance().showEditorBackground;

        com.trusted.systemnbteditor.util.MixinState.isRenderingNbtEditor = true;
        if (!showBackground) {
            com.trusted.systemnbteditor.util.MixinState.isRenderingEditorContents = true;
        }
        this.editor.render(context, mouseX, mouseY, delta);
        com.trusted.systemnbteditor.util.MixinState.isRenderingEditorContents = false;
        com.trusted.systemnbteditor.util.MixinState.isRenderingNbtEditor = false;

        // Render scrollbar
        renderEditorScrollbar(context, this.editor);

        // Manual border if transparent
        if (!showBackground) {
             int borderColor = -6250336;
             int bx = this.editor.getX();
             int by = this.editor.getY();
             int bw = this.editor.getWidth();
             int bh = this.editor.getHeight();
             context.fill(bx, by, bx + bw, by + 1, borderColor); // Top
             context.fill(bx, by + bh - 1, bx + bw, by + bh, borderColor); // Bottom
             context.fill(bx, by, bx + 1, by + bh, borderColor); // Left
             context.fill(bx + bw - 1, by, bx + bw, by + bh, borderColor); // Right
        }

        // Draw title
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 10, 0xFFFFFFFF);

        // Draw component key
        String keyText = "Component: " + component.key();
        context.drawCenteredTextWithShadow(this.textRenderer, keyText, this.width / 2, 30, 0xFFAAAAAA);

        // Draw hint
        String hint = "Enter the component value as NBT (e.g., " + component.defaultValue() + ")";
        context.drawCenteredTextWithShadow(this.textRenderer, hint, this.width / 2, 45, 0xFF888888);
    }

    private void renderEditorScrollbar(DrawContext context, EditBoxWidget editor) {
        try {
            java.lang.reflect.Field editBoxField = EditBoxWidget.class.getDeclaredField("editBox");
            editBoxField.setAccessible(true);
            Object editBox = editBoxField.get(editor);
            
            java.lang.reflect.Method getScrollAmount = editBox.getClass().getMethod("getScrollAmount");
            java.lang.reflect.Method getMaxScroll = editBox.getClass().getMethod("getMaxScroll");
            
            double scrollAmount = (double) getScrollAmount.invoke(editBox);
            double maxScroll = (double) getMaxScroll.invoke(editBox);
            
            if (maxScroll > 0) {
                int padding = 2;
                int sbX = editor.getX() + editor.getWidth() - 6;
                int sbY = editor.getY() + padding;
                int sbW = 4;
                int sbH = editor.getHeight() - (padding * 2);
                
                context.fill(sbX, sbY, sbX + sbW, sbY + sbH, 0x88000000);
                
                int sliderH = Math.max(4, (int)((sbH * sbH) / (maxScroll + sbH)));
                int sliderY = sbY + (int)((scrollAmount / maxScroll) * (sbH - sliderH));
                int color = draggingScrollbar ? 0xFFFFFFFF : 0xFFAAAAAA;
                context.fill(sbX, sliderY, sbX + sbW, sliderY + sliderH, color);
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
        if (button == 0) {
            int padding = 2;
            int sbX = editor.getX() + editor.getWidth() - 10;
            int sbY = editor.getY() + padding;
            int sbW = 10;
            int sbH = editor.getHeight() - (padding * 2);
            
            if (mouseX >= sbX && mouseX <= editor.getX() + editor.getWidth() && mouseY >= sbY && mouseY <= sbY + sbH) {
                draggingScrollbar = true;
                updateScrollPos(mouseY);
                return true;
            }
        }
        return super.mouseClicked(click, bl);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (click.button() == 0) renderingScrollbarPlaceholder(); // dummy call to use internal state
        draggingScrollbar = false;
        return super.mouseReleased(click);
    }
    
    // helper to avoid unused warning or just simple reset
    private void renderingScrollbarPlaceholder() {}

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (click.button() == 0 && draggingScrollbar) {
            updateScrollPos(click.y());
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    private void updateScrollPos(double mouseY) {
        try {
            java.lang.reflect.Field editBoxField = EditBoxWidget.class.getDeclaredField("editBox");
            editBoxField.setAccessible(true);
            Object editBox = editBoxField.get(editor);
            
            java.lang.reflect.Method getMaxScroll = editBox.getClass().getMethod("getMaxScroll");
            java.lang.reflect.Method setScrollAmount = editBox.getClass().getMethod("setScrollAmount", double.class);
            
            double maxScroll = (double) getMaxScroll.invoke(editBox);
            if (maxScroll <= 0) return;
            
            int padding = 2;
            int sbY = editor.getY() + padding;
            int sbH = editor.getHeight() - (padding * 2);
            
            float val = Math.min(1f, Math.max(0f, (float)(mouseY - sbY) / sbH));
            setScrollAmount.invoke(editBox, (double)val * maxScroll);
        } catch (Exception ignored) {}
    }

    private boolean isValidNbt(String nbt) {
        try {
            // Check if it's a valid NBT string (compound or element)
            // We simulate wrapping it in a compound to parse robustly, similar to how
            // mergeComponent does
            String wrappedValue = "{\"temp\":" + nbt + "}";
            net.minecraft.command.argument.NbtCompoundArgumentType.nbtCompound().parse(new StringReader(wrappedValue));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void executeThemeReload() {
        if (this.client != null) {
            this.client.execute(this::loadThemeTexture);
        }
    }

    private void loadThemeTexture() {
        if (this.client == null) return;
        String themeName = com.trusted.systemnbteditor.data.ModConfig.getInstance().currentTheme;
        if (themeName == null || themeName.isEmpty()) return;
        
        File themesDir = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("system_nbt_editor/themes").toFile();
        File themeFile = new File(themesDir, themeName);
        
        if (themeFile.exists()) {
            System.out.println("ThemeLoader: Loading Component Theme " + themeFile.getAbsolutePath());
            try (java.io.FileInputStream fis = new java.io.FileInputStream(themeFile)) {
                NativeImage image = NativeImage.read(fis);
                this.themeTextureWidth = image.getWidth();
                this.themeTextureHeight = image.getHeight();
                System.out.println("ThemeLoader: Component Image size " + themeTextureWidth + "x" + themeTextureHeight);
                
                if (this.themeTexture != null) this.themeTexture.close();
                String texturePath = "theme_comp_" + themeName.toLowerCase().replaceAll("[^a-z0-9]", "_");
                this.themeTexture = new NativeImageBackedTexture(() -> texturePath, image);
                this.themeTextureId = Identifier.of("system_nbt_editor", texturePath);
                this.client.getTextureManager().registerTexture(this.themeTextureId, this.themeTexture);
                System.out.println("ThemeLoader: Registered Component " + this.themeTextureId);
            } catch (Exception e) {
                System.out.println("ThemeLoader: Component ERROR - " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
