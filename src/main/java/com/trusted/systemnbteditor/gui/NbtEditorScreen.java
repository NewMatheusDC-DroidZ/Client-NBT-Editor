package com.trusted.systemnbteditor.gui;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.command.argument.NbtCompoundArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.*;
import net.minecraft.registry.*;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.util.Identifier;
import com.mojang.brigadier.suggestion.Suggestions;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import net.minecraft.client.render.RenderLayer;
import com.mojang.blaze3d.systems.RenderSystem;

import java.util.function.Supplier;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Environment(EnvType.CLIENT)
public class NbtEditorScreen extends Screen {
    private final ItemStack originalStack;
    private final Screen parent;
    private ItemStack previewStack;
    private EditBoxWidget editor;
    private ButtonWidget saveButton;
    private ButtonWidget previewButton;
    private ButtonWidget addComponentButton;
    private ButtonWidget setThemeButton;
    private ButtonWidget indentationButton;
    private ButtonWidget importButton;
    private int indentationLevel = 4;
    private Identifier themeTextureId;
    private NativeImageBackedTexture themeTexture;
    private int themeTextureWidth = 1;
    private int themeTextureHeight = 1;
 
    private static final Identifier ADD_ICON = Identifier.of("system_nbt_editor", "textures/gui/add.png");
    private static final Identifier COPY_COMMAND_ICON = Identifier.of("system_nbt_editor", "textures/gui/copy_command.png");
    private ButtonWidget copyCommandButton;

    private long saveSuccessTime = 0;
    private static final ItemStack BARRIER_STACK = new ItemStack(Items.BARRIER);
    
    private GameProfile loadedProfile = null;
    private ItemStack playerHeadStack = ItemStack.EMPTY;
    private int targetSlotId = -1;
    private final java.util.function.Consumer<ItemStack> saveCallback;

    public NbtEditorScreen(ItemStack itemStack) {
        this(null, itemStack, -1, null);
    }

    public NbtEditorScreen(ItemStack itemStack, int targetSlotId) {
        this(null, itemStack, targetSlotId, null);
    }

    public NbtEditorScreen(ItemStack itemStack, int targetSlotId, java.util.function.Consumer<ItemStack> saveCallback) {
        this(null, itemStack, targetSlotId, saveCallback);
    }

    public NbtEditorScreen(Screen parent, ItemStack itemStack, int targetSlotId, java.util.function.Consumer<ItemStack> saveCallback) {
        super(Text.of("NBT Editor"));
        this.parent = parent;
        this.originalStack = itemStack;
        this.previewStack = itemStack.copy();
        this.targetSlotId = targetSlotId;
        this.saveCallback = saveCallback;
    }

    private net.minecraft.client.gui.screen.ChatInputSuggestor suggestor;
    private TextFieldWidget numUuidField;
    private TextFieldWidget playerNameField;
    private ButtonWidget convertButton;

    @Override
    protected void init() {
        super.init();

        int editorX = 20;
        int editorY = 40;
        int editorWidth = this.width - 100;
        int editorHeight = this.height - 80;

        String initialString = "{}";

        if (this.client != null && this.client.world != null) {
            try {
                RegistryWrapper.WrapperLookup lookup = this.client.world.getRegistryManager();
                RegistryOps<NbtElement> ops = RegistryOps.of(NbtOps.INSTANCE, lookup);
                NbtElement element = ItemStack.CODEC.encodeStart(ops, this.originalStack).getOrThrow();
                String raw = element.toString();
                initialString = com.trusted.systemnbteditor.util.NbtUtils.prettyPrintNbt(raw, this.indentationLevel);
            } catch (Exception e) {
                initialString = "{}";
            }
        }

        this.editor = new EditBoxWidget.Builder()
                .x(editorX)
                .y(editorY)
                .build(this.textRenderer, editorWidth, editorHeight, Text.of("NBT Code"));

        this.editor.setMaxLength(Integer.MAX_VALUE);
        this.editor.setText(initialString);
        this.editor.setChangeListener(this::updatePreview);

        this.addSelectableChild(this.editor);

        this.saveButton = ButtonWidget.builder(Text.literal("Save").formatted(Formatting.GREEN), button -> save())
                .dimensions(20, 10, 100, 20)
                .build();
        this.addDrawableChild(this.saveButton);

        this.previewButton = ButtonWidget.builder(Text.of("Preview item"), button -> openPreview())
                .dimensions(130, 10, 100, 20)
                .build();
        this.addDrawableChild(this.previewButton);

        this.setThemeButton = ButtonWidget.builder(Text.of("Set Theme"), button -> chooseTheme())
                .dimensions(20, this.height - 30, 80, 20)
                .build();
        // this.addDrawableChild(this.setThemeButton); // Hidden as requested

        this.indentationButton = ButtonWidget.builder(getIndentationText(), button -> toggleIndentation())
                .dimensions(105, this.height - 30, 100, 20)
                .build();
        this.addDrawableChild(this.indentationButton);

        this.importButton = ButtonWidget.builder(Text.literal("Import").formatted(Formatting.AQUA), button -> openImportPicker())
                .dimensions(20, this.height - 30, 80, 20)
                .build();
        this.addDrawableChild(this.importButton);
 
        this.copyCommandButton = ButtonWidget.builder(Text.empty(), button -> copyGiveCommand())
                .dimensions(240, 10, 20, 20)
                .tooltip(Tooltip.of(Text.literal("Click to copy to /give command")))
                .build();
        this.addDrawableChild(this.copyCommandButton);
 
        this.addComponentButton = ButtonWidget
                .builder(Text.empty(), button -> openComponentPicker())
                .dimensions(this.width - 140, this.height - 30, 20, 20)
                .build();
        this.addDrawableChild(this.addComponentButton);



        int startX = 270;
        int currentX = startX;
        
        int nameFieldWidth = 100;
        this.playerNameField = new TextFieldWidget(this.textRenderer, currentX, 10, nameFieldWidth, 20,
                Text.of(""));
        this.playerNameField.setMaxLength(16);
        this.addDrawableChild(this.playerNameField);
        currentX += nameFieldWidth + 10; 

        int headBoxWidth = 20; 
        currentX += headBoxWidth + 10; 

        int convertBtnWidth = 60; // Standard size
        this.convertButton = ButtonWidget
                .builder(Text.literal("Convert").formatted(Formatting.BLUE), button -> performConversion())
                .dimensions(currentX, 10, convertBtnWidth, 20)
                .build();
        this.addDrawableChild(this.convertButton);
        currentX += convertBtnWidth + 10;

        int uuidFieldWidth = 100;
        this.numUuidField = new TextFieldWidget(this.textRenderer, currentX, 10, uuidFieldWidth, 20,
                Text.of(""));
        this.numUuidField.setMaxLength(256);
        this.addDrawableChild(this.numUuidField);

        currentX += uuidFieldWidth + 10;


        this.addDrawableChild(ButtonWidget.builder(Text.of("Escape"), button -> this.close())
                .dimensions(this.width - 110, this.height - 30, 100, 20)
                .build());
 
        setupSuggestor(editorX, editorY, editorWidth, editorHeight);
        loadThemeTexture();
    }
 
    private void copyGiveCommand() {
        if (this.client != null && this.client.player != null && this.client.world != null) {
            RegistryWrapper.WrapperLookup lookup = this.client.world.getRegistryManager();
            String command = com.trusted.systemnbteditor.util.NbtUtils.generateGiveCommand(this.previewStack, lookup);
            if (!command.isEmpty()) {
                this.client.keyboard.setClipboard(command);
                this.client.player.sendMessage(Text.literal("Command copied to clipboard!").formatted(Formatting.GREEN), true);
            }
        }
    }
 

    private void drawColoredButton(DrawContext context, ButtonWidget button, int color, Identifier icon, String fallbackText) {
        int x = button.getX();
        int y = button.getY();
        int w = button.getWidth();
        int h = button.getHeight();
        
        // 1. Draw Main Opaque Background
        context.fill(x, y, x + w, y + h, color | 0xFF000000);
        
        // 2. High-Contrast 3D Beveled Depth
        // Outer highlights (white top and left)
        context.fill(x, y, x + w - 1, y + 1, 0xFFFFFFFF); // Top outer
        context.fill(x, y, x + 1, y + h - 1, 0xFFFFFFFF); // Left outer

        // Inner highlights (lighter shade of background)
        // Calculating a lighter shade: we can just use semi-transparent white
        context.fill(x + 1, y + 1, x + w - 2, y + 2, 0x80FFFFFF); // Top inner
        context.fill(x + 1, y + 1, x + 2, y + h - 2, 0x80FFFFFF); // Left inner
        
        // Outer shadows (black bottom and right)
        context.fill(x + 1, y + h - 1, x + w, y + h, 0xFF000000); // Bottom outer
        context.fill(x + w - 1, y + 1, x + w, y + h, 0xFF000000); // Right outer

        // Inner shadows (darker shade of background)
        // Calculating a darker shade: semi-transparent black
        context.fill(x + 2, y + h - 2, x + w - 1, y + h - 1, 0x80000000); // Bottom inner
        context.fill(x + w - 2, y + 2, x + w - 1, y + h - 1, 0x80000000); // Right inner

        // 3. Manually draw white pixels to mimic symbols
        int white = 0xFFFFFFFF;
        if (icon == ADD_ICON) {
            // Draw a white "+" centered
            // Vert: 2x10
            context.fill(x + 9, y + 5, x + 11, y + 15, white);
            // Horiz: 10x2
            context.fill(x + 5, y + 9, x + 15, y + 11, white);
        } else if (icon == COPY_COMMAND_ICON) {
            // Draw a white ">_" terminal prompt centered
            // ">" symbol
            context.fill(x + 6, y + 6, x + 7, y + 7, white);
            context.fill(x + 7, y + 7, x + 8, y + 8, white);
            context.fill(x + 8, y + 8, x + 9, y + 10, white);
            context.fill(x + 7, y + 10, x + 8, y + 11, white);
            context.fill(x + 6, y + 11, x + 7, y + 12, white);
            
            // "_" cursor
            context.fill(x + 10, y + 12, x + 15, y + 13, white);
        } else {
            // Fallback text for any other button
            context.drawCenteredTextWithShadow(this.textRenderer, fallbackText, x + w / 2, y + (h - 8) / 2, 0xFFFFFFFF);
        }
    }

    private boolean drawIcon(DrawContext context, Identifier icon, int x, int y) {
        String path = icon.getPath();
        
        // Variations to try for the identifier
        Identifier[] variants = {
            icon, // As provided (textures/gui/...)
            Identifier.of(icon.getNamespace(), path.replace("textures/", "")), // Without textures/
            path.endsWith(".png") ? Identifier.of(icon.getNamespace(), path.substring(0, path.length() - 4)) : icon, // No .png
            Identifier.of(icon.getNamespace(), "gui/" + icon.getPath().substring(icon.getPath().lastIndexOf('/') + 1)) // Just the filename in gui/
        };

        for (Identifier variant : variants) {
            if (tryDrawTexture(context, variant, x, y, 16, 16, 16, 16)) return true;
        }

        // Sprite system fallback
        String spriteName = path.substring(path.lastIndexOf('/') + 1).replace(".png", "");
        Identifier spriteId = Identifier.of(icon.getNamespace(), "gui/" + spriteName);
        try {
            for (Method m : context.getClass().getMethods()) {
               if (m.getName().equals("drawGuiTexture") || m.getName().equals("method_52718")) {
                   Class<?>[] params = m.getParameterTypes();
                   if (params.length >= 5 && params[0] == Identifier.class) {
                       m.invoke(context, spriteId, x, y, 16, 16);
                       return true;
                   }
               }
            }
        } catch (Exception e) {}
        
        return false;
    }

    private Object getRenderLayer(Identifier texture) {
        try {
            Class<?> rlClass = Class.forName("net.minecraft.client.render.RenderLayer");
            // Standard name
            try {
                Method m = rlClass.getMethod("getGuiTextured", Identifier.class);
                return m.invoke(null, texture);
            } catch (Exception e) {
                // Obfuscated name (method_30704 is getGuiTextured in many versions)
                Method m = rlClass.getMethod("method_30704", Identifier.class);
                return m.invoke(null, texture);
            }
        } catch (Exception e) {}
        return null;
    }

    private Object findGuiRenderLayer() {
        try {
            Class<?> rlClass = Class.forName("net.minecraft.client.render.RenderLayer");
            
            // Try known fields for GUI rendering
            String[] fields = {"GUI_TEXTURED", "field_56883", "field_31268", "field_52968", "field_53900", "field_56923"};
            for (String field : fields) {
                try {
                    java.lang.reflect.Field f = rlClass.getField(field);
                    Object val = f.get(null);
                    if (val != null) return val;
                } catch (Exception ignored) {}
            }
            
            // Try known methods
            String[] methods = {"getGuiTextured", "method_60888", "method_30704", "method_52968", "method_63445"};
            for (String method : methods) {
                try {
                    Method m = rlClass.getMethod(method);
                    Object val = m.invoke(null);
                    if (val != null) return val;
                } catch (Exception ignored) {}
            }
            
            // Try RenderPipelines fallback
            try {
                Class<?> rpClass = Class.forName("net.minecraft.client.gl.RenderPipelines");
                for (java.lang.reflect.Field f : rpClass.getFields()) {
                    if (f.getName().contains("GUI_TEXTURED") || f.getName().contains("GUI")) return f.get(null);
                }
            } catch (Exception ignored) {}
        } catch (Exception ignored) {}
        return null;
    }

    private boolean tryDrawTexture(DrawContext context, Identifier icon, int x, int y, int width, int height, int textureWidth, int textureHeight) {
        try {
            // Priority 1: Direct drawTexture (Identifier, int, int, float, float, int, int, int, int)
            // No RenderLayer needed for this overload usually.
            for (Method m : context.getClass().getMethods()) {
                if (m.getName().equals("drawTexture") || m.getName().equals("method_51450")) {
                    Class<?>[] params = m.getParameterTypes();
                    if (params.length >= 7 && params[0] == Identifier.class) {
                        Object xArg = (params[1] == float.class) ? (float)x : x;
                        Object yArg = (params[2] == float.class) ? (float)y : y;
                        Object uArg = (params[3] == float.class) ? 0f : 0;
                        Object vArg = (params[4] == float.class) ? 0f : 0;
                        
                        if (params.length >= 9) {
                            m.invoke(context, icon, xArg, yArg, uArg, vArg, width, height, textureWidth, textureHeight);
                        } else {
                            m.invoke(context, icon, xArg, yArg, uArg, vArg, width, height);
                        }
                        return true;
                    }
                }
            }

            // Priority 2: With RenderLayer (Identifier-based)
            Object guiLayer = getRenderLayer(icon);
            if (guiLayer != null) {
                for (Method m : context.getClass().getMethods()) {
                    if (m.getName().equals("drawTexture") || m.getName().equals("method_51450")) {
                        Class<?>[] params = m.getParameterTypes();
                        if (params.length >= 8 && !params[0].isPrimitive() && params[1] == Identifier.class) {
                            Object xArg = (params[2] == float.class) ? (float)x : x;
                            Object yArg = (params[3] == float.class) ? (float)y : y;
                            Object uArg = (params[4] == float.class) ? 0f : 0;
                            Object vArg = (params[5] == float.class) ? 0f : 0;
                            
                            if (params.length >= 10) {
                                m.invoke(context, guiLayer, icon, xArg, yArg, uArg, vArg, width, height, textureWidth, textureHeight);
                            } else {
                                m.invoke(context, guiLayer, icon, xArg, yArg, uArg, vArg, width, height);
                            }
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {}
        return false;
    }
 
    private static boolean isPickerOpen = false;
    private void chooseTheme() {
        if (isPickerOpen) {
            if (this.client != null && this.client.player != null) {
                this.client.player.sendMessage(Text.literal("File picker is already open!").formatted(Formatting.RED), false);
            }
            return;
        }
        
        isPickerOpen = true;
        
        new Thread(() -> {
            try {
                System.out.println("ThemePicker: [START] Thread started");
                if (this.client != null && this.client.player != null) {
                    this.client.player.sendMessage(Text.literal("Initializing picker...").formatted(Formatting.YELLOW), false);
                }

                System.setProperty("java.awt.headless", "false");
                String selected = null;
                
                // 1. Try TinyFileDialogs
                try {
                    System.out.println("ThemePicker: Step 1 (TinyFD)");
                    Class<?> tinyfdClass = Class.forName("org.lwjgl.util.tinyfd.TinyFileDialogs");
                    Method targetMethod = null;
                    for (Method m : tinyfdClass.getMethods()) {
                        if (m.getName().equals("tinyfd_openFileDialog") && m.getParameterCount() == 5) {
                            targetMethod = m;
                            break;
                        }
                    }
                    if (targetMethod != null) {
                        if (this.client != null && this.client.player != null) {
                            this.client.player.sendMessage(Text.literal("Trying native picker (TinyFD)...").formatted(Formatting.GRAY), false);
                        }
                        selected = (String) targetMethod.invoke(null, "Select Theme Image", "", null, "Image Files", false);
                        System.out.println("ThemePicker: TinyFD returned " + selected);
                    } else {
                        System.out.println("ThemePicker: TinyFD method not found");
                    }
                } catch (Throwable t) {
                    System.out.println("ThemePicker: TinyFD error: " + t.toString());
                }

                // 2. Fallback to AWT
                if (selected == null) {
                    try {
                        System.out.println("ThemePicker: Step 2 (AWT)");
                        if (this.client != null && this.client.player != null) {
                            this.client.player.sendMessage(Text.literal("Trying AWT picker...").formatted(Formatting.GRAY), false);
                        }
                        java.awt.Frame dummyFrame = new java.awt.Frame();
                        dummyFrame.setAlwaysOnTop(true);
                        java.awt.FileDialog dialog = new java.awt.FileDialog(dummyFrame, "Select Theme Image", java.awt.FileDialog.LOAD);
                        dialog.setFile("*.png;*.jpg;*.jpeg");
                        dialog.requestFocus();
                        dialog.toFront();
                        dialog.setVisible(true);
                        
                        String file = dialog.getFile();
                        String directory = dialog.getDirectory();
                        if (file != null && directory != null) {
                            selected = new File(directory, file).getAbsolutePath();
                            System.out.println("ThemePicker: AWT returned " + selected);
                        }
                        dialog.dispose();
                        dummyFrame.dispose();
                    } catch (Throwable t2) {
                        System.out.println("ThemePicker: AWT error: " + t2.toString());
                        if (this.client != null && this.client.player != null) {
                            this.client.player.sendMessage(Text.literal("AWT failed: " + t2.getClass().getSimpleName()).formatted(Formatting.RED), false);
                        }
                    }
                }
                
                // 3. Final resort: Swing
                if (selected == null) {
                    try {
                        System.out.println("ThemePicker: Step 3 (Swing)");
                        if (this.client != null && this.client.player != null) {
                            this.client.player.sendMessage(Text.literal("Trying Swing picker...").formatted(Formatting.GRAY), false);
                        }
                        javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
                        javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
                        chooser.setDialogTitle("Select Theme Image");
                        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Image Files", "png", "jpg", "jpeg"));
                        int result = chooser.showOpenDialog(null);
                        if (result == javax.swing.JFileChooser.APPROVE_OPTION) {
                            selected = chooser.getSelectedFile().getAbsolutePath();
                            System.out.println("ThemePicker: Swing returned " + selected);
                        }
                    } catch (Throwable t3) {
                        System.out.println("ThemePicker: Swing error: " + t3.toString());
                        if (this.client != null && this.client.player != null) {
                            this.client.player.sendMessage(Text.literal("Swing failed: " + t3.getClass().getSimpleName()).formatted(Formatting.RED), false);
                        }
                    }
                }

                if (selected != null) {
                    File selectedFile = new File(selected);
                    File themesDir = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("system_nbt_editor/themes").toFile();
                    if (!themesDir.exists()) themesDir.mkdirs();
                    
                    String fileName = "theme_" + System.currentTimeMillis() + "_" + selectedFile.getName();
                    File destFile = new File(themesDir, fileName);
                    
                    java.nio.file.Files.copy(selectedFile.toPath(), destFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    
                    com.trusted.systemnbteditor.data.ModConfig config = com.trusted.systemnbteditor.data.ModConfig.getInstance();
                    config.currentTheme = fileName;
                    com.trusted.systemnbteditor.data.ModConfig.save();
                    
                    if (this.client != null) {
                        this.client.execute(() -> {
                            this.loadThemeTexture();
                            if (this.client.player != null) {
                                this.client.player.sendMessage(Text.literal("Theme updated successfully!").formatted(Formatting.GREEN), false);
                            }
                        });
                    }
                } else {
                    if (this.client != null && this.client.player != null) {
                        this.client.player.sendMessage(Text.literal("Picker closed/cancelled.").formatted(Formatting.GRAY), false);
                    }
                }
            } catch (Throwable e) {
                System.out.println("ThemePicker: [CRITICAL ERROR] " + e.toString());
                e.printStackTrace();
            } finally {
                isPickerOpen = false;
                System.out.println("ThemePicker: [END] Thread finished");
            }
        }, "ThemePickerThread").start();
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
            System.out.println("ThemeLoader: Loading " + themeFile.getAbsolutePath());
            try (java.io.FileInputStream fis = new java.io.FileInputStream(themeFile)) {
                NativeImage image = NativeImage.read(fis);
                this.themeTextureWidth = image.getWidth();
                this.themeTextureHeight = image.getHeight();
                System.out.println("ThemeLoader: Image size " + themeTextureWidth + "x" + themeTextureHeight);
                
                if (this.themeTexture != null) this.themeTexture.close();
                String texturePath = "theme_" + themeName.toLowerCase().replaceAll("[^a-z0-9]", "_");
                this.themeTexture = new NativeImageBackedTexture(() -> texturePath, image);
                this.themeTextureId = Identifier.of("system_nbt_editor", texturePath);
                this.client.getTextureManager().registerTexture(this.themeTextureId, this.themeTexture);
                System.out.println("ThemeLoader: Registered " + this.themeTextureId);
            } catch (Exception e) {
                System.out.println("ThemeLoader: ERROR - " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("ThemeLoader: File not found: " + themeFile.getAbsolutePath());
        }
    }

    private void setupSuggestor(int x, int y, int width, int height) {
        TextFieldWidget adapter = new TextFieldWidget(this.textRenderer, x, y, width, height, Text.of("dummy")) {
            private java.lang.reflect.Field cursorField;
            private java.lang.reflect.Field selectionEndField;
            private java.lang.reflect.Field scrollYField;

            {
                try {
                    Class<?> clazz = net.minecraft.client.gui.widget.EditBoxWidget.class;
                    try { cursorField = clazz.getDeclaredField("cursor"); } catch (NoSuchFieldException e) {}
                    if (cursorField != null) cursorField.setAccessible(true);
                    try { selectionEndField = clazz.getDeclaredField("selectionEnd"); } catch (NoSuchFieldException e) {}
                    if (selectionEndField != null) selectionEndField.setAccessible(true);
                    try { scrollYField = clazz.getDeclaredField("scrollY"); } catch (NoSuchFieldException e) {
                        try { scrollYField = clazz.getDeclaredField("scrollOffset"); } catch (Exception ex) {}
                    }
                    if (scrollYField != null) scrollYField.setAccessible(true);
                } catch (Exception e) {}
            }
            
            private void setReflect(java.lang.reflect.Field field, int val) { try { if (field != null) field.setInt(editor, val); } catch (Exception e) {} }
            private int getReflect(java.lang.reflect.Field field) { try { return field != null ? field.getInt(editor) : 0; } catch (Exception e) { return 0; } }
            
             private int getRealCursor() { return getReflect(cursorField); }
             private int getLineStart() {
                String text = editor.getText();
                int c = getRealCursor();
                if (c > text.length()) c = text.length();
                int start = text.lastIndexOf('\n', c - 1);
                return start + 1;
            }
            private int getLineEnd() {
                String text = editor.getText();
                int c = getRealCursor();
                int end = text.indexOf('\n', c);
                if (end == -1) return text.length();
                return end;
            }
            
            @Override
            public String getText() {
                String full = editor.getText();
                int start = getLineStart();
                int end = getLineEnd();
                if (start < 0) start = 0;
                if (end > full.length()) end = full.length();
                if (start > end) return "";
                return full.substring(start, end);
            }
            
            @Override public void setText(String text) {
                String full = editor.getText();
                int start = getLineStart();
                int end = getLineEnd();
                String before = full.substring(0, start);
                String after = full.substring(end);
                editor.setText(before + text + after);
                int newCursor = start + text.length();
                setReflect(cursorField, newCursor);
                setReflect(selectionEndField, newCursor);
            }

            @Override public void write(String text) {
                 String full = editor.getText(); 
                 int c = getRealCursor();
                 String before = full.substring(0, c); 
                 String after = full.substring(c);
                 editor.setText(before + text + after); 
                 int next = c + text.length();
                 setReflect(cursorField, next); 
                 setReflect(selectionEndField, next);
                // if (suggestor != null) suggestor.refresh();
            }
            
            @Override public void eraseCharacters(int characterOffset) {
                String content = editor.getText(); int pos = getRealCursor();
                int start = Math.max(0, pos + characterOffset); int end = pos;
                if (characterOffset > 0) { start = pos; end = Math.min(content.length(), pos + characterOffset); }
                if (start < end) {
                    String before = content.substring(0, start); String after = content.substring(end);
                    editor.setText(before + after); setReflect(cursorField, start); setReflect(selectionEndField, start);
                }
            }
            
            @Override public int getCursor() { int real = getRealCursor(); int start = getLineStart(); return Math.max(0, real - start); }
             @Override public void setCursor(int cursor, boolean shift) {
                int start = getLineStart(); int real = start + cursor; int end = getLineEnd();
                if (real < start) real = start; if (real > end) real = end;
                setReflect(cursorField, real); if (!shift) setReflect(selectionEndField, real);
            }
            
             @Override public int getX() { return editor.getX(); }
             @Override public int getY() { 
                 try {
                     double scroll = scrollYField != null ? scrollYField.getDouble(editor) : 0;
                     int c = getReflect(cursorField);
                     String text = editor.getText();
                     int row = 0;
                     for(int i=0; i<c && i<text.length(); i++) if(text.charAt(i)=='\n') row++;
                     return editor.getY() + (row * 9) - (int)scroll;
                 } catch(Exception e) { return editor.getY(); }
             }
             @Override public int getHeight() { return 9; }
             @Override public int getWidth() { return editor.getWidth(); }
        };
        
        /*
        this.suggestor = new net.minecraft.client.gui.screen.ChatInputSuggestor(this.client, this, adapter, this.textRenderer, true, true, 0, 7, false, 0x80000000) {
            @Override
            public void refresh() {
                String text = editor.getText();
                int cursor = 0;
                try {
                    java.lang.reflect.Field f = net.minecraft.client.gui.widget.EditBoxWidget.class.getDeclaredField("cursor");
                    f.setAccessible(true);
                    cursor = f.getInt(editor);
                } catch (Exception e) {
                     try {
                        java.lang.reflect.Field f = net.minecraft.client.gui.widget.EditBoxWidget.class.getDeclaredField("field_45417"); // cursor in some mappings
                        f.setAccessible(true);
                        cursor = f.getInt(editor);
                    } catch (Exception ex) {}
                }
                
                final int finalCursor = cursor;
                CompletableFuture<Suggestions> future = com.trusted.systemnbteditor.integration.NBTAutocompleteIntegration.getSuggestions("item", NbtEditorScreen.this.getItemId(), "", text, finalCursor);
                
                try {
                    java.lang.reflect.Field pendingField = net.minecraft.client.gui.screen.ChatInputSuggestor.class.getDeclaredField("pendingSuggestions");
                    pendingField.setAccessible(true);
                    pendingField.set(this, future);
                } catch (Exception e) {
                    try {
                        java.lang.reflect.Field pendingField = net.minecraft.client.gui.screen.ChatInputSuggestor.class.getDeclaredField("field_21597"); // Obfuscated name for pendingSuggestions in some mappings
                        pendingField.setAccessible(true);
                        pendingField.set(this, future);
                    } catch (Exception ex) {}
                }

                future.thenRun(() -> {
                    if (future.isDone()) {
                        try {
                            java.lang.reflect.Method m = net.minecraft.client.gui.screen.ChatInputSuggestor.class.getDeclaredMethod("showCommandSuggestions");
                            m.setAccessible(true);
                            m.invoke(this);
                        } catch (Exception e) {
                            try {
                                java.lang.reflect.Method m = net.minecraft.client.gui.screen.ChatInputSuggestor.class.getDeclaredMethod("showSuggestions");
                                m.setAccessible(true);
                                m.invoke(this);
                            } catch (Exception e2) {}
                        }
                    }
                });
            }
        };
        this.suggestor.setWindowActive(true);
        this.suggestor.refresh();
        */
    }

    private void performConversion() {
        String nameText = this.playerNameField.getText().trim();
        this.loadedProfile = null;
        this.playerHeadStack = ItemStack.EMPTY;

        if (!nameText.isEmpty()) {
            this.numUuidField.setText("Fetching...");
            
            com.trusted.systemnbteditor.util.UuidUtils.fetchUuidAsync(nameText).thenAccept(uuid -> {
                if (uuid != null) {
                    com.trusted.systemnbteditor.util.UuidUtils.fetchProfileWithTexturesAsync(uuid, nameText).thenAccept(profile -> {
                        if (this.client != null) {
                            this.client.execute(() -> {
                                this.numUuidField.setText(com.trusted.systemnbteditor.util.UuidUtils.formatIntArrayUuid(uuid));
                                this.loadedProfile = profile;
                                this.playerHeadStack = new ItemStack(Items.PLAYER_HEAD);
                                setProfileToStack(this.playerHeadStack, profile);
                            });
                        }
                    });
                } else {
                    if (this.client != null) {
                        this.client.execute(() -> {
                            // Offline fallback if needed, but UuidUtils.fetchUuid already handles online/offline check? 
                            // Actually UuidUtils.fetchUuid only does online. 
                            // NbtEditorScreen had: if (uuid == null) uuid = Uuids.getOfflinePlayerUuid(nameText);
                            UUID offlineUuid = net.minecraft.util.Uuids.getOfflinePlayerUuid(nameText);
                            this.numUuidField.setText(com.trusted.systemnbteditor.util.UuidUtils.formatIntArrayUuid(offlineUuid));
                        });
                    }
                }
            }).exceptionally(e -> {
                e.printStackTrace();
                if (this.client != null) {
                    this.client.execute(() -> this.numUuidField.setText("Error"));
                }
                return null;
            });
        }
    }
    

    @SuppressWarnings("unchecked")
    private void setProfileToStack(ItemStack stack, GameProfile profile) {
        try {
            // Priority 1: ProfileComponent.ofStatic(GameProfile) via reflection
            try {
                Class<?> profileComponentClass = Class.forName("net.minecraft.component.type.ProfileComponent");
                Method ofStatic = profileComponentClass.getMethod("ofStatic", GameProfile.class);
                Object profileComponent = ofStatic.invoke(null, profile);
                
                stack.set((net.minecraft.component.ComponentType)DataComponentTypes.PROFILE, profileComponent);
                return; // SUCCESS
            } catch (Exception e) {}

            // Priority 2: Constructor fallbacks
            try {
                Class<?> profileComponentClass = Class.forName("net.minecraft.component.type.ProfileComponent");
                java.lang.reflect.Constructor<?> constructor = profileComponentClass.getConstructor(GameProfile.class);
                Object profileComponent = constructor.newInstance(profile);
                stack.set((net.minecraft.component.ComponentType)DataComponentTypes.PROFILE, profileComponent);
                return;
            } catch (Exception e) {
                try {
                     Class<?> profileComponentClass = Class.forName("net.minecraft.component.type.ProfileComponent");
                     for (java.lang.reflect.Constructor<?> c : profileComponentClass.getConstructors()) {
                         if (c.getParameterCount() == 1 && c.getParameterTypes()[0].isAssignableFrom(GameProfile.class)) {
                             Object profileComponent = c.newInstance(profile);
                             stack.set((net.minecraft.component.ComponentType)DataComponentTypes.PROFILE, profileComponent);
                             return;
                         }
                     }
                } catch (Exception e2) {
                    // Try DataComponentTypes.PROFILE codec fallback (for modern Minecraft)
                    try {
                        NbtCompound nbt = new NbtCompound();
                        
                        // Reflection for ID and Name
                        try {
                            Method getId = profile.getClass().getMethod("getId"); 
                            UUID id = (UUID) getId.invoke(profile);
                            if (id != null) nbt.put("id", new NbtIntArray(net.minecraft.util.Uuids.toIntArray(id)));
                        } catch (Exception eId) {
                            try {
                                Method getId = profile.getClass().getMethod("id");
                                nbt.put("id", new NbtIntArray(net.minecraft.util.Uuids.toIntArray((UUID) getId.invoke(profile))));
                            } catch (Exception eId2) {}
                        }
                        
                        try {
                            Method getName = profile.getClass().getMethod("getName");
                            String name = (String) getName.invoke(profile);
                            if (name != null) nbt.putString("name", name);
                        } catch (Exception eName) {
                            try {
                                Method getName = profile.getClass().getMethod("name");
                                String name = (String) getName.invoke(profile);
                                if (name != null) nbt.putString("name", name);
                            } catch (Exception eName2) {}
                        }
                        
                        // Use reflection to get properties to avoid compilation errors
                        try {
                            Method getProps = null;
                            try { getProps = profile.getClass().getMethod("getProperties"); }
                            catch (Exception eProps) { getProps = profile.getClass().getMethod("properties"); }
                            
                            if (getProps != null) {
                                Object propMap = getProps.invoke(profile);
                                // The propMap is usually a MultiMap or Iterable or has values()
                                Method values = null;
                                try { values = propMap.getClass().getMethod("values"); } catch (Exception eVal) {}
                                
                                Iterable<?> propList = null;
                                if (values != null) propList = (Iterable<?>) values.invoke(propMap);
                                else if (propMap instanceof Iterable) propList = (Iterable<?>) propMap;
                                
                                if (propList != null) {
                                    NbtList props = new NbtList();
                                    for (Object p : propList) {
                                        try {
                                            Method getPName = null;
                                            try { getPName = p.getClass().getMethod("getName"); }
                                            catch (Exception eN) { getPName = p.getClass().getMethod("name"); }
                                            
                                            Method getPValue = null;
                                            try { getPValue = p.getClass().getMethod("getValue"); }
                                            catch (Exception eV) { getPValue = p.getClass().getMethod("value"); }
                                            
                                            String name = (String) getPName.invoke(p);
                                            String value = (String) getPValue.invoke(p);
                                            
                                            if ("textures".equals(name)) {
                                                NbtCompound propObj = new NbtCompound();
                                                propObj.putString("name", name);
                                                propObj.putString("value", value);
                                                try {
                                                    Method getSig = null;
                                                    try { getSig = p.getClass().getMethod("getSignature"); }
                                                    catch (Exception eSig) { getSig = p.getClass().getMethod("signature"); }
                                                    String sig = (String) getSig.invoke(p);
                                                    if (sig != null) propObj.putString("signature", sig);
                                                } catch (Exception sigEx) {}
                                                props.add(propObj);
                                            }
                                        } catch (Exception inner) {}
                                    }
                                    if (!props.isEmpty()) {
                                        nbt.put("properties", props);
                                    }
                                }
                            }
                        } catch (Exception propsEx) {}
                        
                        DataComponentTypes.PROFILE.getCodec().parse(NbtOps.INSTANCE, nbt).result().ifPresent(component -> {
                            stack.set((net.minecraft.component.ComponentType)DataComponentTypes.PROFILE, component);
                        });
                        return;
                    } catch (Exception e3) {}
                }
            }

            // Priority 3: NBT Fallback
            NbtCompound nbt = new NbtCompound();
             try {
                Method getName = profile.getClass().getMethod("getName");
                String name = (String) getName.invoke(profile);
                if (name != null) nbt.putString("name", name);
            } catch (Exception e) {}

            try {
                Method getId = profile.getClass().getMethod("getId");
                UUID id = (UUID) getId.invoke(profile);
                if (id != null) nbt.put("id", new NbtIntArray(net.minecraft.util.Uuids.toIntArray(id)));
            } catch (Exception e) {}

            try {
                NbtList propertiesList = new NbtList();
                Method getProperties = profile.getClass().getMethod("getProperties");
                Object propertyMap = getProperties.invoke(profile);
                
                if (propertyMap != null) {
                    Method valuesMethod = propertyMap.getClass().getMethod("values");
                    Iterable<?> values = (Iterable<?>) valuesMethod.invoke(propertyMap);
                    
                    for (Object prop : values) {
                        try {
                            Method pGetName = prop.getClass().getMethod("name");
                            Method pGetValue = prop.getClass().getMethod("value");
                            Method pGetSignature = prop.getClass().getMethod("signature");
                            
                            String pName = (String) pGetName.invoke(prop);
                            String pValue = (String) pGetValue.invoke(prop);
                            String pSig = (String) pGetSignature.invoke(prop);
                            
                            NbtCompound propTag = new NbtCompound();
                            if (pName != null) propTag.putString("name", pName);
                            if (pValue != null) propTag.putString("value", pValue);
                            if (pSig != null) propTag.putString("signature", pSig);
                            
                            propertiesList.add(propTag);
                        } catch (Exception inner) {
                            try {
                                Method pGetName = prop.getClass().getMethod("getName");
                                Method pGetValue = prop.getClass().getMethod("getValue");
                                Method pGetSignature = prop.getClass().getMethod("getSignature");
                                
                                String pName = (String) pGetName.invoke(prop);
                                String pValue = (String) pGetValue.invoke(prop);
                                String pSig = (String) pGetSignature.invoke(prop);
                                
                                NbtCompound propTag = new NbtCompound();
                                if (pName != null) propTag.putString("name", pName);
                                if (pValue != null) propTag.putString("value", pValue);
                                if (pSig != null) propTag.putString("signature", pSig);
                                
                                propertiesList.add(propTag);
                            } catch (Exception inner2) {}
                        }
                    }
                }
                
                if (!propertiesList.isEmpty()) {
                    nbt.put("properties", propertiesList);
                }
            } catch (Exception e) {}

            DataComponentTypes.PROFILE.getCodec().parse(NbtOps.INSTANCE, nbt).result().ifPresent(component -> {
                try {
                     stack.set((net.minecraft.component.ComponentType)DataComponentTypes.PROFILE, component);
                } catch (Exception e) {}
            });
        } catch (Throwable t) {
            t.printStackTrace();
        }
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
                shaderColorMethod.invoke(null, 0.6f, 0.6f, 0.6f, 1.0f);
            } catch (Exception e) {}
            
            tryDrawTexture(context, themeTextureId, x, y, w, h, themeTextureWidth, themeTextureHeight);
            
            try {
                Method shaderColorMethod = com.mojang.blaze3d.systems.RenderSystem.class.getMethod("setShaderColor", float.class, float.class, float.class, float.class);
                shaderColorMethod.invoke(null, 1.0f, 1.0f, 1.0f, 1.0f);
            } catch (Exception e) {}
        }

        super.render(context, mouseX, mouseY, delta);
 
        // Draw colorful buttons matching CAD Editor style but with depth
        drawColoredButton(context, addComponentButton, 0xFF00A800, ADD_ICON, "+"); // Green for Add
        drawColoredButton(context, copyCommandButton, 0xFFA8A800, COPY_COMMAND_ICON, "C"); // Yellow for Copy

        String itemId = "Item: " + getItemId();
        int labelX = this.width - this.textRenderer.getWidth(itemId) - 70;
        context.drawTextWithShadow(this.textRenderer, itemId, labelX, 15, 0xFFFFFFFF);

        if (this.numUuidField != null && this.numUuidField.getText().isEmpty() && !this.numUuidField.isFocused()) {
            context.drawTextWithShadow(this.textRenderer, "UUID", this.numUuidField.getX() + 4,
                    this.numUuidField.getY() + 6, 0xFFAAAAAA);
        }
        if (this.playerNameField != null && this.playerNameField.getText().isEmpty() && !this.playerNameField.isFocused()) {
            context.drawTextWithShadow(this.textRenderer, "Username", this.playerNameField.getX() + 4,
                    this.playerNameField.getY() + 6, 0xFFAAAAAA);
        }


        
        int headX = this.playerNameField.getX() + this.playerNameField.getWidth() + 10;
        int headY = 10;
        int headSize = 20; 
        
        // Background for the head box
        context.fill(headX, headY, headX + headSize, headY + headSize, 0xFF000000); 
        
        // Border for the head box
        int borderColor = 0xFFAAAAAA;
        context.fill(headX, headY, headX + headSize, headY + 1, borderColor); 
        context.fill(headX, headY + headSize - 1, headX + headSize, headY + headSize, borderColor); 
        context.fill(headX, headY, headX + 1, headY + headSize, borderColor); 
        context.fill(headX + headSize - 1, headY, headX + headSize, headY + headSize, borderColor); 
        
        if (this.playerHeadStack != null && !this.playerHeadStack.isEmpty()) {
             // Draw the head centered in the 20x20 box
             context.drawItem(this.playerHeadStack, headX + 2, headY + 2);
        } else {
             // If no head is selected, draw a placeholder (optional, but keep it empty for now)
        }

        boolean showBackground = com.trusted.systemnbteditor.data.ModConfig.getInstance().showEditorBackground;
        
        com.trusted.systemnbteditor.util.MixinState.isRenderingNbtEditor = true;
        if (!showBackground) {
            com.trusted.systemnbteditor.util.MixinState.isRenderingEditorContents = true;
        }
        com.trusted.systemnbteditor.util.MixinState.isRenderingNbtEditor = true;

        this.editor.render(context, mouseX, mouseY, delta);
        com.trusted.systemnbteditor.util.MixinState.isRenderingEditorContents = false;
        com.trusted.systemnbteditor.util.MixinState.isRenderingNbtEditor = false;
        
        // Manual border if transparent
        if (!showBackground) {
             int bgBorderColor = -6250336;
             int bx = this.editor.getX();
             int by = this.editor.getY();
             int bw = this.editor.getWidth();
             int bh = this.editor.getHeight();
             context.fill(bx, by, bx + bw, by + 1, bgBorderColor); // Top
             context.fill(bx, by + bh - 1, bx + bw, by + bh, bgBorderColor); // Bottom
             context.fill(bx, by, bx + 1, by + bh, bgBorderColor); // Left
             context.fill(bx + bw - 1, by, bx + bw, by + bh, bgBorderColor); // Right
        }

        float scale = 2.5f; // Reverted to large preview as requested
        int iconX = this.width - 40 - 20; 
        int iconY = 10;

        context.getMatrices().pushMatrix();
        float centerX = (float)iconX + 20.0f;
        float centerY = (float)iconY + 20.0f;
        
        context.getMatrices().translate(centerX, centerY);
        context.getMatrices().scale(scale, scale);
        
        context.getMatrices().translate(-8.0f, -8.0f);
        context.drawItem(this.previewStack, 0, 0);
        context.getMatrices().popMatrix();
        
        long timeSince = System.currentTimeMillis() - this.saveSuccessTime;
        if (timeSince < 1000 && this.saveSuccessTime != 0) {
            this.saveButton.setMessage(Text.literal("Saved").formatted(Formatting.GREEN));
        } else {
            this.saveButton.setMessage(Text.literal("Save").formatted(Formatting.GREEN));
            if (timeSince >= 1000 && this.saveSuccessTime != 0) {
                this.close();
            }
        }

        // if (this.suggestor != null) {
        //     this.suggestor.render(context, mouseX, mouseY);
        // }
    }

    @Override
    public void tick() {
        super.tick();
        /*
        if (this.suggestor != null) {
            try {
                java.lang.reflect.Method tickMethod = suggestor.getClass().getMethod("tick");
                tickMethod.invoke(suggestor);
            } catch (Exception e) {
                try {
                    // Try obfuscated name or alternative if needed, but 1.21.11 usually has tick()
                    // If it fails, we skip
                } catch (Exception ex) {}
            }
        }
        */
    }

    // Handled via SuggestionProvider in adapter/suggestor setup
    
    private void openPreview() {
        if (this.client == null || this.client.world == null) return;
        try {
            String text = this.editor.getText();
            NbtCompound newTag = NbtCompoundArgumentType.nbtCompound().parse(new StringReader(text));
            RegistryWrapper.WrapperLookup lookup = this.client.world.getRegistryManager();
            RegistryOps<NbtElement> ops = RegistryOps.of(NbtOps.INSTANCE, lookup);
            ItemStack newItem = ItemStack.CODEC.parse(ops, newTag).result().orElse(this.originalStack);
            this.client.setScreen(new ItemPreviewScreen(newItem, this));
        } catch (Exception e) {
            this.client.setScreen(new ItemPreviewScreen(this.originalStack, this));
        }
    }

    private void updatePreview(String text) {
        if (this.client == null || this.client.world == null) return;
        try {
            NbtCompound newTag = NbtCompoundArgumentType.nbtCompound().parse(new StringReader(text));
            RegistryWrapper.WrapperLookup lookup = this.client.world.getRegistryManager();
            RegistryOps<NbtElement> ops = RegistryOps.of(NbtOps.INSTANCE, lookup);
            ItemStack newItem = ItemStack.CODEC.parse(ops, newTag).result().orElse(ItemStack.EMPTY);
            this.previewStack = !newItem.isEmpty() ? newItem : BARRIER_STACK;
        } catch (Exception e) {
            this.previewStack = BARRIER_STACK;
        }
    }

    private void openComponentPicker() {
        if (this.client != null) {
            this.client.setScreen(new ComponentPickerScreen(this, component -> {
                this.client.setScreen(new ComponentEditorScreen(this, component, this::mergeComponent));
            }));
        }
    }

    private void mergeComponent(String componentKey, String componentValue) {
        try {
            String currentText = this.editor.getText();
            NbtCompound currentNbt;
            try {
                currentNbt = NbtCompoundArgumentType.nbtCompound().parse(new StringReader(currentText));
            } catch (Exception e) {
                // If parsing fails, try to start from original item + components
                currentNbt = new NbtCompound();
                currentNbt.putString("id", originalStack.getItem().getRegistryEntry().registryKey().getValue().toString());
                currentNbt.putInt("count", originalStack.getCount());
            }

            if (!currentNbt.contains("components")) {
                currentNbt.put("components", new NbtCompound());
            }

            NbtElement componentsElem = currentNbt.get("components");
            if (!(componentsElem instanceof NbtCompound)) {
                componentsElem = new NbtCompound();
                currentNbt.put("components", componentsElem);
            }
            NbtCompound components = (NbtCompound) componentsElem;
            
            // Parse new component value
            // Most component values are simple (bool, int, string) or compounds/lists
            NbtElement componentNbt;
            try {
                 String wrapper = "{\"v\":" + componentValue + "}";
                 NbtCompound temp = NbtCompoundArgumentType.nbtCompound().parse(new StringReader(wrapper));
                 componentNbt = temp.get("v");
            } catch (Exception e) {
                 // Fallback to literal string if it won't parse
                 componentNbt = NbtString.of(componentValue);
            }

            if (componentNbt != null) {
                components.put(componentKey, componentNbt);
                
                String pretty = com.trusted.systemnbteditor.util.NbtUtils.prettyPrintNbt(currentNbt.toString(), this.indentationLevel);
                this.editor.setText(pretty); 
                updatePreview(pretty); 
                
                if(client.player != null) client.player.sendMessage(Text.of("Merged component: " + componentKey), false);
            }

        } catch (Exception e) {
             if(client.player != null) client.player.sendMessage(Text.literal("Merge Error: " + e.getMessage()).formatted(Formatting.RED), false);
             e.printStackTrace();
        }
    }


    private String getItemId() {
        try {
            String text = this.editor.getText();
            NbtCompound nbt = NbtCompoundArgumentType.nbtCompound().parse(new StringReader(text));
            if (nbt.contains("id")) return nbt.get("id").toString().replaceAll("^\"|\"$", "");
        } catch (Exception e) {}
        return this.originalStack.getItem().toString();
    }

    private void save() {
         if (this.client == null || this.client.player == null) return;
         if (this.originalStack.isOf(Items.AIR)) {
             this.client.player.sendMessage(Text.literal("Cannot save changes to Air!").formatted(Formatting.RED), true);
             return;
         }

        try {
            String text = this.editor.getText();
            
            // --- SNBT Cleanup ---
            // 1. Remove trailing commas which are the #1 cause of "Expected literal" errors in SNBT
            // e.g. { "foo": 1, } -> { "foo": 1 }
            text = text.replaceAll(",\\s*([}\\]])", "$1");
            
            // 2. Smart Repair: Add missing commas between lines
            // This looks for cases like: "key": value \n "key2": value
            // and inserts a comma if missing.
            // regex: looks for a line ending in a literal/bracket/quote, followed by whitespace and a new key "..." :
            text = text.replaceAll("([\"\\d\\}\\]])\\s*\\n\\s*(\"[^\"]+\"\\s*:)", "$1,\n$2");
            
            NbtCompound newTag;
            try {
                newTag = NbtCompoundArgumentType.nbtCompound().parse(new StringReader(text));
            } catch (CommandSyntaxException e) {
                // If parsing fails, try to wrap it if it looks like just a list of components
                // Some users might type: { "minecraft:enchantments": {...} }
                // instead of the full item structure.
                this.client.player.sendMessage(Text.literal("NBT Syntax Error: " + e.getMessage()).formatted(Formatting.RED), false);
                return;
            }

            // Ensure basic item structure if it's a simple NBT compound (missing id/count)
            // Modern Minecraft ItemStack codec expects {id: "...", count: 1, components: {...}}
            // We check if it looks like a component map or a full item.
            // Items must have "id". If no "id" is present, we assume the user provided components.
            if (!newTag.contains("id")) {
                NbtCompound wrapped = new NbtCompound();
                wrapped.putString("id", this.originalStack.getItem().getRegistryEntry().registryKey().getValue().toString());
                wrapped.putInt("count", this.originalStack.getCount());
                
                // If it already has "components", use it, otherwise use the whole tag as components
                if (newTag.contains("components") && newTag.get("components") instanceof NbtCompound) {
                    wrapped.put("components", newTag.get("components"));
                } else {
                    wrapped.put("components", newTag);
                }
                newTag = wrapped;
            } else if (!newTag.contains("count")) {
                 newTag.putInt("count", this.originalStack.getCount());
            }

            RegistryWrapper.WrapperLookup lookup = this.client.world.getRegistryManager();
            RegistryOps<NbtElement> ops = RegistryOps.of(NbtOps.INSTANCE, lookup);
            
            var result = ItemStack.CODEC.parse(ops, newTag);
            if (result.error().isPresent()) {
                String errorMsg = result.error().get().message();
                this.client.player.sendMessage(Text.literal("Codec Error: " + errorMsg).formatted(Formatting.RED), false);
                return;
            }

            ItemStack newItem = result.result().orElse(ItemStack.EMPTY);

        if (!newItem.isEmpty()) {
            if (parent instanceof NbtSelectionScreen sel) {
                sel.refresh(newItem);
            }

            if (this.saveCallback != null) {
                this.saveCallback.accept(newItem);
                this.saveSuccessTime = System.currentTimeMillis();
                return;
            }

            int packetSlot;
            if (this.targetSlotId != -1) {
                packetSlot = this.targetSlotId;
                // If it's a standard inventory slot, try to update the player inventory directly too
                // This is mostly for visual consistency before server ACK
                if (packetSlot >= 36 && packetSlot <= 44) { // Hotbar
                     this.client.player.getInventory().setStack(packetSlot - 36, newItem.copy());
                } else if (packetSlot >= 9 && packetSlot <= 35) { // Main Inventory
                     this.client.player.getInventory().setStack(packetSlot, newItem.copy());
                } else if (packetSlot >= 5 && packetSlot <= 8) { // Armor
                     this.client.player.getInventory().setStack(39 - (packetSlot - 5), newItem.copy());
                } else if (packetSlot == 45) { // Offhand
                     this.client.player.getInventory().setStack(40, newItem.copy());
                }
            } else {
                int slotIndex = this.client.player.getInventory().selectedSlot;
                this.client.player.getInventory().setStack(slotIndex, newItem.copy());
                packetSlot = slotIndex + 36;
            }
            
            this.client.getNetworkHandler().sendPacket(new CreativeInventoryActionC2SPacket(packetSlot, newItem));
            this.saveSuccessTime = System.currentTimeMillis();
        } else {
            this.client.player.sendMessage(Text.literal("Save Error: Resulting item is empty").formatted(Formatting.RED), false);
        }
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            this.client.player.sendMessage(Text.literal("System Error: " + msg).formatted(Formatting.RED), false);
            e.printStackTrace();
        }
    }
    
    private Text getIndentationText() {
        if (this.indentationLevel == -1) {
            return Text.literal("Indent: ")
                    .append(Text.literal("Continuous").formatted(Formatting.GOLD));
        }
        String levelStr = this.indentationLevel > 0 ? this.indentationLevel + " spaces" : "OFF";
        Formatting color = this.indentationLevel > 0 ? Formatting.GREEN : Formatting.DARK_RED;
        return Text.literal("Indent: ")
                .append(Text.literal(levelStr).formatted(color));
    }

    private void toggleIndentation() {
        if (this.indentationLevel == 0) this.indentationLevel = 2;
        else if (this.indentationLevel == 2) this.indentationLevel = 4;
        else if (this.indentationLevel == 4) this.indentationLevel = 6;
        else if (this.indentationLevel == 6) this.indentationLevel = -1; // Continuous
        else this.indentationLevel = 0; // OFF

        this.indentationButton.setMessage(getIndentationText());
        
        String currentText = this.editor.getText();
        this.editor.setText(com.trusted.systemnbteditor.util.NbtUtils.prettyPrintNbt(currentText, this.indentationLevel));
    }

    private void openImportPicker() {
        if (this.client == null || this.client.player == null) return;

        if (isPickerOpen) {
            this.client.player.sendMessage(Text.literal("A file picker is already open!").formatted(Formatting.RED), false);
            return;
        }
        
        isPickerOpen = true;
        this.client.player.sendMessage(Text.literal("Starting import process...").formatted(Formatting.YELLOW), false);
        
        new Thread(() -> {
            try {
                // Ensure headless is off
                System.setProperty("java.awt.headless", "false");
                
                if (java.awt.GraphicsEnvironment.isHeadless()) {
                    this.client.execute(() -> this.client.player.sendMessage(Text.literal("Headless mode is ACTIVE! UI blocked.").formatted(Formatting.RED), false));
                }

                String selected = null;
                
                // 1. Try TinyFileDialogs
                try {
                    this.client.execute(() -> this.client.player.sendMessage(Text.literal("Trying native picker (TinyFD)...").formatted(Formatting.GRAY), false));
                    Class<?> tinyfdClass = Class.forName("org.lwjgl.util.tinyfd.TinyFileDialogs");
                    Method targetMethod = null;
                    for (Method m : tinyfdClass.getMethods()) {
                        if (m.getName().equals("tinyfd_openFileDialog") && m.getParameterCount() == 5) {
                            targetMethod = m;
                            break;
                        }
                    }
                    if (targetMethod != null) {
                        selected = (String) targetMethod.invoke(null, "Import NBT Files", "", null, "NBT Files (*.nbt)", true);
                    } else {
                        this.client.execute(() -> this.client.player.sendMessage(Text.literal("TinyFD method not found.").formatted(Formatting.DARK_GRAY), false));
                    }
                } catch (Throwable t) {
                    final String err = t.toString();
                    this.client.execute(() -> this.client.player.sendMessage(Text.literal("TinyFD Error: " + err).formatted(Formatting.RED), false));
                }

                // 2. Fallback to AWT
                if (selected == null) {
                    try {
                        this.client.execute(() -> this.client.player.sendMessage(Text.literal("Trying AWT picker...").formatted(Formatting.GRAY), false));
                        java.awt.Frame dummyFrame = new java.awt.Frame();
                        dummyFrame.setAlwaysOnTop(true);
                        java.awt.FileDialog dialog = new java.awt.FileDialog(dummyFrame, "Import NBT Files", java.awt.FileDialog.LOAD);
                        dialog.setFile("*.nbt");
                        dialog.setMultipleMode(true);
                        
                        // Try to force visibility/focus
                        dummyFrame.setVisible(true);
                        dummyFrame.setExtendedState(java.awt.Frame.ICONIFIED);
                        dummyFrame.setExtendedState(java.awt.Frame.NORMAL);
                        dialog.setVisible(true);
                        
                        File[] files = dialog.getFiles();
                        if (files != null && files.length > 0) {
                            StringBuilder sb = new StringBuilder();
                            for (int i = 0; i < files.length; i++) {
                                sb.append(files[i].getAbsolutePath());
                                if (i < files.length - 1) sb.append("|");
                            }
                            selected = sb.toString();
                        }
                        dialog.dispose();
                        dummyFrame.dispose();
                    } catch (Throwable t2) {
                        final String err2 = t2.toString();
                        this.client.execute(() -> this.client.player.sendMessage(Text.literal("AWT Error: " + err2).formatted(Formatting.RED), false));
                    }
                }

                // 3. Final resort: Swing
                if (selected == null) {
                    try {
                        this.client.execute(() -> this.client.player.sendMessage(Text.literal("Trying Swing picker...").formatted(Formatting.GRAY), false));
                        javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
                        final javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
                        chooser.setDialogTitle("Import NBT Files");
                        chooser.setMultiSelectionEnabled(true);
                        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("NBT Files", "nbt"));
                        
                        // Use a final array to capture the selection from the EDT
                        final String[] swingSelection = {null};
                        final Object lock = new Object();
                        
                        javax.swing.SwingUtilities.invokeAndWait(() -> {
                            int result = chooser.showOpenDialog(null);
                            if (result == javax.swing.JFileChooser.APPROVE_OPTION) {
                                File[] files = chooser.getSelectedFiles();
                                if (files != null && files.length > 0) {
                                    StringBuilder sb = new StringBuilder();
                                    for (int i = 0; i < files.length; i++) {
                                        sb.append(files[i].getAbsolutePath());
                                        if (i < files.length - 1) sb.append("|");
                                    }
                                    swingSelection[0] = sb.toString();
                                }
                            }
                            synchronized(lock) { lock.notify(); }
                        });
                        
                        synchronized(lock) { if (swingSelection[0] == null) lock.wait(50); } // Small wait if needed
                        selected = swingSelection[0];
                    } catch (Throwable t3) {
                        final String err3 = t3.toString();
                        this.client.execute(() -> this.client.player.sendMessage(Text.literal("Swing Error: " + err3).formatted(Formatting.RED), false));
                    }
                }

                if (selected != null) {
                    final String selection = selected;
                    if (this.client != null) {
                        this.client.execute(() -> {
                            this.client.player.sendMessage(Text.literal("Processing selection...").formatted(Formatting.GREEN), false);
                            String[] paths = selection.split("\\|");
                            handleImportedFiles(paths);
                        });
                    }
                } else {
                    this.client.execute(() -> this.client.player.sendMessage(Text.literal("Import cancelled or all pickers failed.").formatted(Formatting.GRAY), false));
                }
            } catch (Throwable tOverall) {
                final String errOverall = tOverall.toString();
                if (this.client != null && this.client.player != null) {
                    this.client.execute(() -> this.client.player.sendMessage(Text.literal("Fatal Import Error: " + errOverall).formatted(Formatting.DARK_RED), false));
                }
                tOverall.printStackTrace();
            } finally {
                isPickerOpen = false;
            }
        }, "NbtImportThread").start();
    }

    private void handleImportedFiles(String[] paths) {
        if (this.client == null || this.client.player == null) return;
        
        int importedCount = 0;
        for (String pathStr : paths) {
            File file = new File(pathStr);
            if (!file.exists() || !file.isFile()) continue;

            NbtCompound nbt = com.trusted.systemnbteditor.util.NbtIoUtils.readNbt(file.toPath());
            if (nbt != null) {
                long size = com.trusted.systemnbteditor.util.NbtIoUtils.getItemSize(nbt);
                if (size > 10 * 1024 * 1024) { // 10MB
                    this.client.player.sendMessage(Text.literal("Skipped " + file.getName() + ": File too large (>10MB)").formatted(Formatting.RED), false);
                    continue;
                }
                
                com.trusted.systemnbteditor.util.NbtIoUtils.giveItem(nbt);
                importedCount++;
            } else {
                this.client.player.sendMessage(Text.literal("Failed to read: " + file.getName()).formatted(Formatting.RED), false);
            }
        }
        
        if (importedCount > 0) {
            this.client.player.sendMessage(Text.literal("Successfully imported " + importedCount + " items.").formatted(Formatting.GREEN), false);
        }
    }


    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        /*
        if (this.suggestor != null && this.suggestor.mouseScrolled(verticalAmount)) {
            return true;
        }
        */
        // Speed up scrolling by multiplying the vertical amount
        double boostedScroll = verticalAmount * 2.0; 
        if (this.editor != null && this.editor.isMouseOver(mouseX, mouseY)) {
            this.editor.mouseScrolled(mouseX, mouseY, horizontalAmount, boostedScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override public boolean shouldPause() { return false; }
}