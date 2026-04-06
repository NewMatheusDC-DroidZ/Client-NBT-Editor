package com.trusted.systemnbteditor.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import java.io.File;
import java.lang.reflect.Method;

public class NbtSelectionScreen extends Screen {
    private ItemStack stack;   // mutable — child editors call refresh() after saving
    private final int slotId;
    private final java.util.function.Consumer<ItemStack> saveCallback;
    private ButtonWidget bundleButton;
    private ButtonWidget clientChestVisibilityButton;

    public NbtSelectionScreen(ItemStack stack, int slotId) {
        this(stack, slotId, null);
    }

    public NbtSelectionScreen(ItemStack stack, int slotId, java.util.function.Consumer<ItemStack> saveCallback) {
        super(Text.of("NBT Selection"));
        this.stack = stack;
        this.slotId = slotId;
        this.saveCallback = saveCallback;
    }

    private boolean hasBeenUpdated = false;

    /** Called by child editors (Container, Enchantment, Visual) after they save. */
    public void refresh(ItemStack updated) {
        this.stack = updated;
        this.hasBeenUpdated = true;
    }

    @Override
    public void close() {
        if (this.saveCallback != null && hasBeenUpdated) {
            this.saveCallback.accept(this.stack);
        }
        if (this.client != null) this.client.setScreen(null);
    }

    @Override
    protected void init() {
        super.init();

        int buttonWidth = 140;
        int buttonHeight = 20;
        int startX = 25; // Directly next to the sidebar
        int startY = 10; // Right at the top
        int spacing = 8;

        // Button 1: String NBT Editor
        this.addDrawableChild(ButtonWidget.builder(Text.of("String NBT Editor"), button -> {
            if (this.client != null) this.client.setScreen(new NbtEditorScreen(this, this.stack, this.slotId, this.saveCallback));
        }).dimensions(startX, startY, buttonWidth, buttonHeight).build());

        // Button 2: Component Editor
        int compEdWidth = 90;
        int simpleWidth = buttonWidth - compEdWidth;
        this.addDrawableChild(ButtonWidget.builder(Text.of("Component Editor"), button -> {
            if (this.client != null) this.client.setScreen(new VisualNbtEditorScreen(this, this.stack, this.slotId, this.saveCallback));
        }).dimensions(startX, startY + (buttonHeight + spacing), compEdWidth, buttonHeight).build());

        // Button 2.5: Simple Editor
        this.addDrawableChild(ButtonWidget.builder(Text.of("Simple"), button -> {
            if (this.client != null) this.client.setScreen(new SimpleNbtEditorScreen(this, this.stack, this.slotId, this.saveCallback));
        }).dimensions(startX + compEdWidth, startY + (buttonHeight + spacing), simpleWidth, buttonHeight).build());

        // Button 3: Container
        this.addDrawableChild(ButtonWidget.builder(Text.of("Container"), button -> {
            if (this.client != null) this.client.setScreen(new ContainerNbtEditorScreen(this, this.stack, this.slotId, this.saveCallback));
        }).dimensions(startX, startY + 2 * (buttonHeight + spacing), buttonWidth, buttonHeight).build());

        // Button 3.5: Use Remainder
        int useRemainderX = startX + buttonWidth + spacing;
        int useRemainderY = startY + 2 * (buttonHeight + spacing);
        this.addDrawableChild(ButtonWidget.builder(Text.of("Use Remainder"), button -> {
            if (this.client != null) this.client.setScreen(new UseRemainderScreen(this, this.stack, this.slotId, this.saveCallback));
        }).dimensions(useRemainderX, useRemainderY, buttonWidth, buttonHeight).build());

        this.bundleButton = this.addDrawableChild(ButtonWidget.builder(getBundleButtonLabel(), button -> onBundleButtonPressed())
                .dimensions(useRemainderX + buttonWidth + 10, useRemainderY, 60, 20).build());

        // Button 4: Enchantments
        this.addDrawableChild(ButtonWidget.builder(Text.of("Enchantments"), button -> {
            if (this.client != null) this.client.setScreen(new EnchantmentNbtEditorScreen(this, this.stack, this.slotId, this.saveCallback));
        }).dimensions(startX, startY + 3 * (buttonHeight + spacing), buttonWidth, buttonHeight).build());

        // Button 5: Entity Data
        this.addDrawableChild(ButtonWidget.builder(Text.of("Entity Data"), button -> {
            if (this.client != null) this.client.setScreen(new EntityDataMenuScreen(this, this.stack, this.slotId, this.saveCallback));
        }).dimensions(startX, startY + 4 * (buttonHeight + spacing), buttonWidth, buttonHeight).build());

        // Escape Button
        this.addDrawableChild(ButtonWidget.builder(Text.of("Escape"), button -> this.close())
                .dimensions(this.width - 110, this.height - 30, 100, 20)
                .build());

        // Config Button - icon drawn manually in render()
        this.addDrawableChild(ButtonWidget.builder(Text.of(""), button -> {
            if (this.client != null) this.client.setScreen(new SystemNbtEditorConfigScreen(this));
        }).dimensions(10, this.height - 30, 20, 20).build());

        this.clientChestVisibilityButton = this.addDrawableChild(ButtonWidget.builder(getClientChestVisibilityLabel(), button -> {
            com.trusted.systemnbteditor.data.ModConfig config = com.trusted.systemnbteditor.data.ModConfig.getInstance();
            config.clientChestButtonVisible = !config.clientChestButtonVisible;
            com.trusted.systemnbteditor.data.ModConfig.save();
            button.setMessage(getClientChestVisibilityLabel());
        }).dimensions(40, this.height - 30, 180, 20).build());

        // Import Button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Import").formatted(Formatting.AQUA), button -> openImportPicker())
                .dimensions(10, this.height - 55, 60, 20).build());
        
        // Export Button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Export").formatted(Formatting.GOLD), button -> {
            if (this.client != null) this.client.setScreen(new ExportScreen(this, this.stack, this.slotId, this.saveCallback));
        }).dimensions(10, this.height - 80, 60, 20).build());
    }

    private static final net.minecraft.util.Identifier SETTINGS_TEXTURE =
            net.minecraft.util.Identifier.of("systemnbteditor", "textures/gui/settings.png");

    private Text getBundleButtonLabel() {
        return Text.literal("Bundle").styled(style -> style.withColor(0x8B5A2B));
    }

    private Text getClientChestVisibilityLabel() {
        boolean enabled = com.trusted.systemnbteditor.data.ModConfig.getInstance().clientChestButtonVisible;
        return Text.literal("CChest Visibility: " + (enabled ? "ON" : "OFF"))
                .formatted(enabled ? Formatting.GREEN : Formatting.RED);
    }

    private void onBundleButtonPressed() {
        if (this.client == null) return;

        if (!this.stack.contains(DataComponentTypes.BUNDLE_CONTENTS)) {
            if (this.bundleButton != null) {
                this.bundleButton.setMessage(Text.literal("Not Bundle").styled(style -> style.withColor(0x8B5A2B)));
            }
            return;
        }

        this.bundleButton.setMessage(getBundleButtonLabel());
        this.client.setScreen(new BundleEditorScreen(this, this.stack, this.slotId, this.saveCallback));
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
        // Dim the world behind the menu
        context.fill(0, 0, this.width, this.height, 0x80000000);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 10, 0xFFFFFFFF);
        // Vertical Gray Sidebar
        int startY = 10;
        int totalHeight = 104;
        context.fill(5, startY, 15, startY + totalHeight, 0xFF707070);

        // Draw Item Display (aligned with sidebar)
        int itemX = 180;
        int itemY = 10;
        context.drawItem(this.stack, itemX, itemY);

        // Item Name Draw
        Text name = this.stack.getName();
        context.drawTextWithShadow(this.textRenderer, name, itemX + 25, itemY + 4, 0xFFFFFFFF);

        // Draw settings.png icon over the config button (simulated with pixels)
        // The button itself will automatically render when the screen renders,
        // but since we want the custom drawColoredButton look, we skip the standard widget
        // rendering and just call drawColoredButton for that specific button.
        for (net.minecraft.client.gui.Element element : this.children()) {
            if (element instanceof net.minecraft.client.gui.widget.ButtonWidget btn) {
                if (btn.getMessage().getString().isEmpty() && btn.getWidth() == 20) { // Our config button
                    drawColoredButton(context, btn, 0x555555);
                }
            }
        }
    }
    @Override
    public boolean shouldPause() {
        return false;
    }

    private void drawColoredButton(DrawContext context, net.minecraft.client.gui.widget.ButtonWidget button, int color) {
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

        // Inner highlights
        context.fill(x + 1, y + 1, x + w - 2, y + 2, 0x80FFFFFF); // Top inner
        context.fill(x + 1, y + 1, x + 2, y + h - 2, 0x80FFFFFF); // Left inner
        
        // Outer shadows (black bottom and right)
        context.fill(x + 1, y + h - 1, x + w, y + h, 0xFF000000); // Bottom outer
        context.fill(x + w - 1, y + 1, x + w, y + h, 0xFF000000); // Right outer

        // Inner shadows
        context.fill(x + 2, y + h - 2, x + w - 1, y + h - 1, 0x80000000); // Bottom inner
        context.fill(x + w - 2, y + 2, x + w - 1, y + h - 1, 0x80000000); // Right inner

        // 3. Manually draw white pixels to mimic a gear/settings icon
        int white = 0xFFFFFFFF;
        
        // Center hole
        // A 10x10 gear centered in a 20x20 button (x+5 to x+15, y+5 to y+15)
        // Cross pattern
        context.fill(x + 9, y + 4, x + 11, y + 16, white);
        context.fill(x + 4, y + 9, x + 16, y + 11, white);
        // Diagonals
        context.fill(x + 6, y + 6, x + 8, y + 8, white);
        context.fill(x + 12, y + 12, x + 14, y + 14, white);
        context.fill(x + 12, y + 6, x + 14, y + 8, white);
        context.fill(x + 6, y + 12, x + 8, y + 14, white);
        
        // Carve out center
        context.fill(x + 8, y + 8, x + 12, y + 12, color | 0xFF000000);
    }

    private static boolean isPickerOpen = false;

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
                        
                        synchronized(lock) { if (swingSelection[0] == null) lock.wait(50); }
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

            net.minecraft.nbt.NbtCompound nbt = com.trusted.systemnbteditor.util.NbtIoUtils.readNbt(file.toPath());
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
}
