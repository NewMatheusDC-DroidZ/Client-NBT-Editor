package com.trusted.systemnbteditor.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class UtilityMenuScreen extends Screen {
    private final Screen parent;

    private ButtonWidget nameCustomisationButton;

    public UtilityMenuScreen(Screen parent) {
        super(Text.of("Utility Menu"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        int gridStartX = this.width / 2 - 155; // Center-ish
        int gridStartY = this.height / 2 - 20;

        int buttonWidth = 100;
        int buttonHeight = 20;
        int padding = 5;

        // 3x3 Grid
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int x = gridStartX + col * (buttonWidth + padding);
                int y = gridStartY + row * (buttonHeight + padding);

                if (row == 0 && col == 0) {
                    // First button: InfiniCommand
                    this.addDrawableChild(ButtonWidget.builder(Text.literal("InfiniCommand").formatted(Formatting.GRAY), button -> {
                        if (this.client != null) this.client.setScreen(new InfiniCommandScreen(this));
                    }).dimensions(x, y, buttonWidth, buttonHeight).build());
                } else if (row == 0 && col == 1) {
                    // Second button: UUID Stuff
                    this.addDrawableChild(ButtonWidget.builder(Text.literal("UUID Stuff").formatted(Formatting.BLUE), button -> {
                        if (this.client != null) this.client.setScreen(new UuidStuffScreen(this));
                    }).dimensions(x, y, buttonWidth, buttonHeight).build());
                } else if (row == 0 && col == 2) {
                    // Third button: AntiCrash
                    this.addDrawableChild(ButtonWidget.builder(Text.literal("AntiCrash").formatted(Formatting.GOLD), button -> {
                        if (this.client != null) this.client.setScreen(new AntiCrashScreen(this));
                    }).dimensions(x, y, buttonWidth, buttonHeight).build());
                } else if (row == 1 && col == 0) {
                    // Row 1, Col 0: Danger Zone
                     this.addDrawableChild(ButtonWidget.builder(Text.literal("Danger Zone").formatted(Formatting.RED), button -> {
                        if (this.client != null) this.client.setScreen(new DangerZoneScreen(this));
                    }).dimensions(x, y, buttonWidth, buttonHeight).build());
                } else if (row == 1 && col == 1) {
                    // Row 1, Col 1: Name Customisation (Light Blue)
                    this.addDrawableChild(ButtonWidget.builder(Text.literal("Name Customisation").formatted(Formatting.AQUA), button -> {
                        if (this.client != null) this.client.setScreen(new NameCustomisationScreen(this));
                    }).dimensions(x, y, buttonWidth, buttonHeight).build());
                } else if (row == 1 && col == 2) {
                    // Row 1, Col 2: Tellraw (Yellow)
                    this.addDrawableChild(ButtonWidget.builder(Text.literal("Tellraw").formatted(Formatting.YELLOW), button -> {
                        if (this.client != null) this.client.setScreen(new TellrawScreen(this));
                    }).dimensions(x, y, buttonWidth, buttonHeight).build());
                } else if (row == 2 && col == 0) {
                    // Row 2, Col 0: Librarian Preloading (Dim Green)
                    this.addDrawableChild(ButtonWidget.builder(Text.literal("Librarian Preload").formatted(Formatting.DARK_GREEN), button -> {
                        if (this.client != null) this.client.setScreen(new LibrarianPreloadScreen(this, net.minecraft.item.ItemStack.EMPTY, -1, null));
                    }).dimensions(x, y, buttonWidth, buttonHeight).build());
                } else if (row == 2 && col == 1) {
                    // Row 2, Col 1: Skid (Black)
                    this.addDrawableChild(ButtonWidget.builder(Text.literal("Skid").formatted(Formatting.BLACK), button -> {
                        if (this.client != null) {
                            this.client.setScreen(new SkidScreen(this));
                        }
                    }).dimensions(x, y, buttonWidth, buttonHeight).build());
                } else {
                    // Empty buttons
                    this.addDrawableChild(ButtonWidget.builder(Text.of(""), button -> {}).dimensions(x, y, buttonWidth, buttonHeight).build());
                }
            }
        }

        // Back button
        this.addDrawableChild(ButtonWidget.builder(Text.of("Back"), button -> {
            if (this.client != null) this.client.setScreen(this.parent);
        }).dimensions(this.width / 2 - 50, this.height - 30, 100, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }
}
