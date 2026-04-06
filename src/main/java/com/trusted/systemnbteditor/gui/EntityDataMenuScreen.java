package com.trusted.systemnbteditor.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.component.DataComponentTypes;

import java.util.function.Consumer;

public class EntityDataMenuScreen extends Screen {
    private final Screen parent;
    private ItemStack stack;
    private final int slotId;
    private final Consumer<ItemStack> saveCallback;

    public EntityDataMenuScreen(Screen parent, ItemStack stack, int slotId, Consumer<ItemStack> saveCallback) {
        super(Text.of("Entity Data"));
        this.parent = parent;
        this.stack = stack;
        this.slotId = slotId;
        this.saveCallback = saveCallback;
    }

    @Override
    protected void init() {
        super.init();

        int buttonWidth = 140;
        int buttonHeight = 20;
        int centerX = this.width / 2 - buttonWidth / 2;
        int startY = 40;
        int spacing = 10;

        // Villager Trades Button
        boolean isVillagerEgg = stack.getItem() == Items.VILLAGER_SPAWN_EGG;
        boolean hasEntityData = stack.contains(DataComponentTypes.ENTITY_DATA);
        
        ButtonWidget tradesBtn = ButtonWidget.builder(Text.of("Villager Trades"), button -> {
            if (this.client != null) {
                this.client.setScreen(new VillagerTradeEditorScreen(this, this.stack, this.slotId, updated -> {
                    // Keep this menu state in sync so reopening editor uses latest data.
                    this.stack = updated.copy();
                    if (this.parent instanceof NbtSelectionScreen nss) {
                        nss.refresh(updated.copy());
                    }
                    if (this.saveCallback != null) {
                        this.saveCallback.accept(updated.copy());
                    }
                }));
            }
        }).dimensions(centerX, startY, buttonWidth, buttonHeight).build();
        
        // As per request: "All of this can even happen, if the player is editing a villager spawn egg, otherwise the button Villager Trades won't work."
        tradesBtn.active = isVillagerEgg;
        
        this.addDrawableChild(tradesBtn);

        // Back Button
        this.addDrawableChild(ButtonWidget.builder(Text.of("Back"), button -> close())
                .dimensions(10, this.height - 30, 80, 20).build());
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
        context.fill(0, 0, this.width, this.height, 0x80000000);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 15, 0xFFFFFFFF);
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }
}
