package com.trusted.systemnbteditor.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtIo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;

public class ExportScreen extends Screen {
    private final Screen parent;
    private final ItemStack stack;
    private final int slotId;
    private final java.util.function.Consumer<ItemStack> saveCallback;

    private TextFieldWidget fileNameField;

    public ExportScreen(Screen parent, ItemStack stack, int slotId, java.util.function.Consumer<ItemStack> saveCallback) {
        super(Text.of("Export item to NBT"));
        this.parent = parent;
        this.stack = stack;
        this.slotId = slotId;
        this.saveCallback = saveCallback;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int startY = this.height / 2 - 30;

        this.fileNameField = new TextFieldWidget(this.textRenderer, centerX - 100, startY, 200, 20, Text.of(""));
        // Setting suggestion as ghost text
        this.fileNameField.setSuggestion(".nbt file name");
        this.fileNameField.setMaxLength(256);
        this.addDrawableChild(this.fileNameField);

        this.fileNameField.setChangedListener(text -> {
            if (!text.isEmpty()) {
                this.fileNameField.setSuggestion("");
            } else {
                this.fileNameField.setSuggestion(".nbt file name");
            }
        });

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Save Item").formatted(Formatting.GOLD), button -> {
            openExportPicker();
        }).dimensions(centerX - 50, startY + 30, 100, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.of("Cancel"), button -> {
            if (this.client != null) this.client.setScreen(this.parent);
        }).dimensions(centerX - 50, startY + 60, 100, 20).build());
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
        context.fill(0, 0, this.width, this.height, 0x80000000);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFFFF);
    }

    private static boolean isPickerOpen = false;

    private void openExportPicker() {
        if (this.client == null || this.client.player == null) return;

        if (isPickerOpen) {
            this.client.player.sendMessage(Text.literal("A file picker is already open!").formatted(Formatting.RED), false);
            return;
        }

        String defaultName = this.fileNameField.getText().trim();
        if (defaultName.isEmpty()) {
            defaultName = "exported_item";
        }
        if (!defaultName.endsWith(".nbt")) {
            defaultName += ".nbt";
        }

        final String finalDefaultName = defaultName;

        isPickerOpen = true;
        this.client.player.sendMessage(Text.literal("Starting export process...").formatted(Formatting.YELLOW), false);

        new Thread(() -> {
            try {
                System.setProperty("java.awt.headless", "false");

                if (java.awt.GraphicsEnvironment.isHeadless()) {
                    this.client.execute(() -> this.client.player.sendMessage(Text.literal("Headless mode is ACTIVE! UI blocked.").formatted(Formatting.RED), false));
                }

                String selected = null;

                // 1. Try TinyFileDialogs
                try {
                    this.client.execute(() -> this.client.player.sendMessage(Text.literal("Trying native picker (TinyFD) for save...").formatted(Formatting.GRAY), false));
                    Class<?> tinyfdClass = Class.forName("org.lwjgl.util.tinyfd.TinyFileDialogs");
                    Method targetMethod = null;
                    for (Method m : tinyfdClass.getMethods()) {
                        if (m.getName().equals("tinyfd_saveFileDialog") && m.getParameterCount() == 4) {
                            targetMethod = m;
                            break;
                        }
                    }
                    if (targetMethod != null) {
                        selected = (String) targetMethod.invoke(null, "Save NBT File", finalDefaultName, null, "NBT Files (*.nbt)");
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
                        java.awt.FileDialog dialog = new java.awt.FileDialog(dummyFrame, "Save NBT File", java.awt.FileDialog.SAVE);
                        dialog.setFile(finalDefaultName);
                        
                        dummyFrame.setVisible(true);
                        dummyFrame.setExtendedState(java.awt.Frame.ICONIFIED);
                        dummyFrame.setExtendedState(java.awt.Frame.NORMAL);
                        dialog.setVisible(true);

                        String file = dialog.getFile();
                        String dir = dialog.getDirectory();
                        if (file != null && dir != null) {
                            selected = new File(dir, file).getAbsolutePath();
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
                        chooser.setDialogTitle("Save NBT File");
                        chooser.setSelectedFile(new File(finalDefaultName));
                        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("NBT Files", "nbt"));

                        final String[] swingSelection = {null};
                        final Object lock = new Object();

                        javax.swing.SwingUtilities.invokeAndWait(() -> {
                            int result = chooser.showSaveDialog(null);
                            if (result == javax.swing.JFileChooser.APPROVE_OPTION) {
                                File file = chooser.getSelectedFile();
                                if (file != null) {
                                    swingSelection[0] = file.getAbsolutePath();
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
                            this.client.player.sendMessage(Text.literal("Saving file...").formatted(Formatting.GREEN), false);
                            String finalSelection = selection;
                            if (!finalSelection.endsWith(".nbt")) {
                                finalSelection += ".nbt";
                            }
                            handleExportFile(finalSelection);
                        });
                    }
                } else {
                    this.client.execute(() -> this.client.player.sendMessage(Text.literal("Export cancelled or all pickers failed.").formatted(Formatting.GRAY), false));
                }
            } catch (Throwable tOverall) {
                final String errOverall = tOverall.toString();
                if (this.client != null && this.client.player != null) {
                    this.client.execute(() -> this.client.player.sendMessage(Text.literal("Fatal Export Error: " + errOverall).formatted(Formatting.DARK_RED), false));
                }
                tOverall.printStackTrace();
            } finally {
                isPickerOpen = false;
            }
        }, "NbtExportThread").start();
    }

    private void handleExportFile(String pathStr) {
        if (this.client == null || this.client.player == null || this.client.world == null) return;

        try {
            RegistryWrapper.WrapperLookup lookup = this.client.world.getRegistryManager();
            RegistryOps<NbtElement> ops = RegistryOps.of(NbtOps.INSTANCE, lookup);
            
            NbtElement encoded = ItemStack.CODEC.encodeStart(ops, this.stack).getOrThrow();
            if (encoded instanceof NbtCompound compound) {
                File file = new File(pathStr);
                try (FileOutputStream out = new FileOutputStream(file)) {
                    NbtIo.writeCompressed(compound, out);
                    this.client.player.sendMessage(Text.literal("Successfully exported to " + file.getName()).formatted(Formatting.GREEN), false);
                    this.client.setScreen(this.parent);
                } catch (IOException e) {
                    this.client.player.sendMessage(Text.literal("Export Error: " + e.getMessage()).formatted(Formatting.RED), false);
                }
            } else {
                this.client.player.sendMessage(Text.literal("Export Error: Failed to encode item as NBT Compound").formatted(Formatting.RED), false);
            }
        } catch (Exception e) {
            this.client.player.sendMessage(Text.literal("Export Error: " + e.getMessage()).formatted(Formatting.RED), false);
            e.printStackTrace();
        }
    }
}
