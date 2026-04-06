package com.trusted.systemnbteditor.gui;

import com.trusted.systemnbteditor.data.ModConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.MultilineTextWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class LibrarianPreloadScreen extends Screen {

    private final Screen parent;
    private final ItemStack stack;
    private final int slotId;
    private final java.util.function.Consumer<ItemStack> saveCallback;

    private TextFieldWidget fromBox;
    private TextFieldWidget toBox;
    private TextFieldWidget pageBox;
    
    // We don't have a reliable default MultilineTextFieldWidget that is easily editable in 1.21.11 typical mappings
    // without using specialized widgets. TextFieldWidget works best for single lines. 
    // Wait, the prompt asked for a "larger text box". We can use EditBoxWidget.
    private net.minecraft.client.gui.widget.EditBoxWidget pagesBox;

    private String totalSizeText = "Total Size: calculating...";

    public LibrarianPreloadScreen(Screen parent, ItemStack stack, int slotId, java.util.function.Consumer<ItemStack> saveCallback) {
        super(Text.of("Librarian Preloading"));
        this.parent = parent;
        this.stack = stack;
        this.slotId = slotId;
        this.saveCallback = saveCallback;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int topY = 40;

        // "From" and "To" boxes
        this.fromBox = new TextFieldWidget(this.textRenderer, centerX - 120, topY, 100, 20, Text.of("From"));
        this.fromBox.setPlaceholder(Text.of("From").copy().formatted(Formatting.DARK_GRAY));
        this.addDrawableChild(this.fromBox);

        this.toBox = new TextFieldWidget(this.textRenderer, centerX + 20, topY, 100, 20, Text.of("To"));
        this.toBox.setPlaceholder(Text.of("To").copy().formatted(Formatting.DARK_GRAY));
        this.addDrawableChild(this.toBox);

        // "Add Range" Button
        this.addDrawableChild(ButtonWidget.builder(Text.of("Add Range"), button -> {
            addRange();
        }).dimensions(centerX + 130, topY, 80, 20).build());


        int midY = topY + 40;
        // Individual Pages Text is drawn in render()
        
        // "Add Page" Button
        this.addDrawableChild(ButtonWidget.builder(Text.of("Add Page"), button -> {
            addSinglePage();
        }).dimensions(centerX - 80, midY, 80, 20).build());

        // "Page" box
        this.pageBox = new TextFieldWidget(this.textRenderer, centerX + 10, midY, 40, 20, Text.of("Page"));
        this.pageBox.setPlaceholder(Text.of("Page").copy().formatted(Formatting.DARK_GRAY));
        this.addDrawableChild(this.pageBox);


        int bottomY = midY + 40;
        
        this.pagesBox = new net.minecraft.client.gui.widget.EditBoxWidget.Builder()
                .x(centerX - 150)
                .y(bottomY)
                .build(this.textRenderer, 300, 60, Text.of("Pages"));
        this.addDrawableChild(this.pagesBox);
        
        // Initialize the pagesBox with current config
        Set<Integer> currentPages = ModConfig.getInstance().librarianPreloadedPages;
        if (currentPages != null && !currentPages.isEmpty()) {
            TreeSet<Integer> sorted = new TreeSet<>(currentPages);
            String initialText = sorted.stream().map(String::valueOf).collect(Collectors.joining(", "));
            this.pagesBox.setText(initialText);
        }

        // Save Button
        int saveY = bottomY + 70;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Save").formatted(Formatting.GREEN), button -> {
            saveAndPreload();
        }).dimensions(centerX - 50, saveY, 100, 20).build());

        // Escape Button
        this.addDrawableChild(ButtonWidget.builder(Text.of("Back"), button -> this.close())
                .dimensions(this.width - 110, this.height - 30, 100, 20)
                .build());

        // Calculate initial size
        updateTotalSize();
    }

    private void addRange() {
        try {
            int from = Integer.parseInt(this.fromBox.getText().trim());
            int to = Integer.parseInt(this.toBox.getText().trim());
            if (from <= to) {
                Set<Integer> pages = parsePagesBox();
                for (int i = from; i <= to; i++) {
                    pages.add(i);
                }
                updatePagesBox(pages);
            }
        } catch (NumberFormatException ignored) {}
    }

    private void addSinglePage() {
        try {
            int page = Integer.parseInt(this.pageBox.getText().trim());
            Set<Integer> pages = parsePagesBox();
            pages.add(page);
            updatePagesBox(pages);
        } catch (NumberFormatException ignored) {}
    }

    private Set<Integer> parsePagesBox() {
        Set<Integer> pages = new TreeSet<>();
        String text = this.pagesBox.getText().replaceAll("[^0-9, ]", "");
        String[] parts = text.split("[, ]+");
        for (String part : parts) {
            if (!part.isEmpty()) {
                try {
                    pages.add(Integer.parseInt(part));
                } catch (NumberFormatException ignored) {}
            }
        }
        return pages;
    }

    private void updatePagesBox(Set<Integer> pages) {
        String newText = pages.stream().map(String::valueOf).collect(Collectors.joining(", "));
        this.pagesBox.setText(newText);
    }

    private void saveAndPreload() {
        Set<Integer> finalPages = parsePagesBox();
        ModConfig.getInstance().librarianPreloadedPages = new HashSet<>(finalPages);
        ModConfig.save();

        if (this.client != null && this.client.player != null) {
             this.client.player.sendMessage(Text.literal("Saved Librarian pages list. Preloading now...").formatted(Formatting.YELLOW), false);
        }

        new Thread(() -> {
            if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("librarian")) {
                for (Integer page : finalPages) {
                    try {
                        Class<?> libClass = Class.forName("me.videogamesm12.librarian.Librarian");
                        java.lang.reflect.Method getInstance = libClass.getMethod("getInstance");
                        Object libInstance = getInstance.invoke(null);
                        
                        java.lang.reflect.Method getHotbarPage = libClass.getMethod("getHotbarPage", BigInteger.class);
                        Object storage = getHotbarPage.invoke(libInstance, BigInteger.valueOf(page));
                        
                        if (storage != null) {
                            java.lang.reflect.Method existsMethod = storage.getClass().getMethod("exists");
                            boolean exists = (boolean) existsMethod.invoke(storage);
                            if (exists) {
                                java.lang.reflect.Method loadMethod = storage.getClass().getMethod("librarian$load");
                                loadMethod.invoke(storage);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (this.client != null && this.client.player != null) {
                    this.client.execute(() -> {
                        this.client.player.sendMessage(Text.literal("Successfully preloaded Librarian pages!").formatted(Formatting.GREEN), false);
                        updateTotalSize(); // Refresh the size after loading
                    });
                }
            } else {
                 if (this.client != null && this.client.player != null) {
                     this.client.execute(() -> this.client.player.sendMessage(Text.literal("Librarian mod not loaded!").formatted(Formatting.RED), false));
                 }
            }
        }, "LibrarianPreloadThread").start();
    }

    private void updateTotalSize() {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("librarian")) {
            this.totalSizeText = "Librarian not installed.";
            return;
        }

        long totalBytes = 0;
        Set<Integer> currentPages = parsePagesBox();
        for (Integer page : currentPages) {
            try {
                Class<?> libClass = Class.forName("me.videogamesm12.librarian.Librarian");
                java.lang.reflect.Method getInstance = libClass.getMethod("getInstance");
                Object libInstance = getInstance.invoke(null);
                
                java.lang.reflect.Method getHotbarPage = libClass.getMethod("getHotbarPage", BigInteger.class);
                Object storage = getHotbarPage.invoke(libInstance, BigInteger.valueOf(page));
                
                if (storage != null) {
                    java.lang.reflect.Method existsMethod = storage.getClass().getMethod("exists");
                    boolean exists = (boolean) existsMethod.invoke(storage);
                    
                    if (exists) {
                        java.lang.reflect.Method getLocationMethod = storage.getClass().getMethod("librarian$getLocation");
                        java.io.File file = (java.io.File) getLocationMethod.invoke(storage);
                        if (file != null && file.exists()) {
                            totalBytes += file.length();
                        }
                    }
                }
            } catch (Exception e) {
                // Ignore
            }
        }
        
        double mb = totalBytes / (1024.0 * 1024.0);
        this.totalSizeText = String.format("Total Size: %.2fMB", mb);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 10, 0xFFFFFF);

        int centerX = this.width / 2;
        int midY = 40 + 40;
        
        // Draw "Individual Pages" text
        context.drawTextWithShadow(this.textRenderer, Text.of("Individual Pages"), centerX - 180, midY + 6, 0xFFFFFF);

        // Draw Total Size
        context.drawCenteredTextWithShadow(this.textRenderer, Text.of(this.totalSizeText), centerX, this.height - 40, Formatting.GOLD.getColorValue());
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
        context.fill(0, 0, this.width, this.height, 0x80000000);
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
