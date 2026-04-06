package com.trusted.systemnbteditor.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import com.trusted.systemnbteditor.util.MixinState;
import com.trusted.systemnbteditor.data.ModConfig;
import java.io.File;
import java.lang.reflect.Method;

public class InfiniCommandScreen extends Screen {
    private final Screen parent;
    private EditBoxWidget commandEditBox;
    private ButtonWidget runButton;
    private long errorTime = 0;

    private Identifier themeTextureId;
    private NativeImageBackedTexture themeTexture;
    private int themeTextureWidth = 1;
    private int themeTextureHeight = 1;

    public InfiniCommandScreen(Screen parent) {
        super(Text.of("InfiniCommand"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        int boxX = 20;
        int boxY = 20;
        int boxWidth = this.width - 40;
        int boxHeight = this.height - 60; // Leave space for button at bottom

        this.commandEditBox = new EditBoxWidget.Builder()
                .x(boxX)
                .y(boxY)
                .build(this.textRenderer, boxWidth, boxHeight, Text.of("Command"));
        
        this.commandEditBox.setMaxLength(32500); // Support large commands
        this.addSelectableChild(this.commandEditBox);

        // Run Button: Bottom Right, 20x100
        int btnWidth = 100;
        int btnHeight = 20;
        int btnX = this.width - 20 - btnWidth;
        int btnY = this.height - 10 - btnHeight;

        this.runButton = ButtonWidget.builder(Text.of("Run"), button -> runCommand())
                .dimensions(btnX, btnY, btnWidth, btnHeight)
                .build();
        this.addDrawableChild(this.runButton);
        
        // Back Button
        this.addDrawableChild(ButtonWidget.builder(Text.of("Back"), button -> {
             if (this.client != null) this.client.setScreen(this.parent);
        }).dimensions(20, btnY, 100, 20).build());

        loadThemeTexture();
    }

    private void loadThemeTexture() {
        if (this.client == null) return;
        String themeName = ModConfig.getInstance().currentTheme;
        if (themeName == null || themeName.isEmpty()) return;
        
        File themesDir = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("system_nbt_editor/themes").toFile();
        File themeFile = new File(themesDir, themeName);
        
        if (themeFile.exists()) {
            try (java.io.FileInputStream fis = new java.io.FileInputStream(themeFile)) {
                NativeImage image = NativeImage.read(fis);
                this.themeTextureWidth = image.getWidth();
                this.themeTextureHeight = image.getHeight();
                
                if (this.themeTexture != null) this.themeTexture.close();
                String texturePath = "theme_" + themeName.toLowerCase().replaceAll("[^a-z0-9]", "_");
                this.themeTexture = new NativeImageBackedTexture(() -> texturePath, image);
                this.themeTextureId = Identifier.of("system_nbt_editor", texturePath);
                this.client.getTextureManager().registerTexture(this.themeTextureId, this.themeTexture);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (themeTextureId != null) {
            int x = commandEditBox.getX();
            int y = commandEditBox.getY();
            int w = commandEditBox.getWidth();
            int h = commandEditBox.getHeight();
            
            try {
                Method shaderColorMethod = com.mojang.blaze3d.systems.RenderSystem.class.getMethod("setShaderColor", float.class, float.class, float.class, float.class);
                shaderColorMethod.invoke(null, 0.6f, 0.6f, 0.6f, 1.0f);
            } catch (Exception e) {}
            
            try {
                for (Method m : context.getClass().getMethods()) {
                    if (m.getName().equals("drawTexture") || m.getName().equals("method_51450")) {
                        Class<?>[] params = m.getParameterTypes();
                        if (params.length >= 9 && params[0] == Identifier.class) {
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

        boolean showBackground = ModConfig.getInstance().showEditorBackground;
        MixinState.isRenderingNbtEditor = true;
        if (!showBackground) {
            MixinState.isRenderingEditorContents = true;
        }

        this.commandEditBox.render(context, mouseX, mouseY, delta);

        if (!showBackground) {
             int bgBorderColor = -6250336;
             int bx = this.commandEditBox.getX();
             int by = this.commandEditBox.getY();
             int bw = this.commandEditBox.getWidth();
             int bh = this.commandEditBox.getHeight();
             context.fill(bx, by, bx + bw, by + 1, bgBorderColor);
             context.fill(bx, by + bh - 1, bx + bw, by + bh, bgBorderColor);
             context.fill(bx, by, bx + 1, by + bh, bgBorderColor);
             context.fill(bx + bw - 1, by, bx + bw, by + bh, bgBorderColor);
        }

        MixinState.isRenderingEditorContents = false;
        MixinState.isRenderingNbtEditor = false;
        
        if (System.currentTimeMillis() - errorTime < 2000) {
             this.runButton.setMessage(Text.literal("Wrong Command").formatted(Formatting.RED));
        } else {
             this.runButton.setMessage(Text.of("Run"));
        }
    }

    private void runCommand() {
        String content = this.commandEditBox.getText().trim();
        if (content.isEmpty()) return;

        // Strip newlines to make it a single line command, but preserve multi-spaces for NBT safety
        content = content.replace("\r", "").replace("\n", " ").trim();

        try {
            if (content.startsWith("/")) {
                content = content.substring(1);
            }
            
            if (this.client != null && this.client.player != null) {
                int len = content.length();
                String preview = len > 50 ? content.substring(0, 47) + "..." : content;
                this.client.player.sendMessage(Text.literal("Executing (" + len + " chars): /" + preview).formatted(Formatting.GRAY), false);
            }

            if (this.client != null && this.client.getNetworkHandler() != null) {
                this.client.getNetworkHandler().sendChatCommand(content);
            }
        } catch (Exception e) {
            triggerError();
        }
    }

    private void triggerError() {
        this.errorTime = System.currentTimeMillis();
        this.runButton.setMessage(Text.literal("Wrong Command").formatted(Formatting.RED));
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }
}
