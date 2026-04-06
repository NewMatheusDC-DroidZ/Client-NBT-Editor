package com.trusted.systemnbteditor.gui;

import com.mojang.brigadier.StringReader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.util.Identifier;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import java.util.ArrayList;
import java.util.List;

public class ArrowsScreen extends Screen {
    private final Screen parent;
    private final ItemStack stack;
    private final java.util.function.Consumer<ItemStack> saveCallback;

    private TextFieldWidget amountField;
    private ItemStack selectedArrow = new ItemStack(Items.ARROW);
    private String savedAmount = "10";
    private boolean fieldInitialized = false;

    public ArrowsScreen(Screen parent, ItemStack stack, java.util.function.Consumer<ItemStack> saveCallback) {
        super(Text.of("Arrows Editor"));
        this.parent = parent;
        this.stack = stack.copy();
        this.saveCallback = saveCallback;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        amountField = new TextFieldWidget(this.textRenderer, centerX - 40, centerY - 40, 80, 20, Text.of("Amount"));
        amountField.setText(savedAmount);
        amountField.setChangedListener(s -> savedAmount = s);
        fieldInitialized = true;
        this.addDrawableChild(amountField);

        this.addDrawableChild(ButtonWidget.builder(Text.of("Select Arrow Type"), button -> {
            if (this.client != null) {
                this.client.setScreen(new ArrowPickerScreen(this, newArrow -> {
                    this.selectedArrow = newArrow.copy();
                    selectedArrow.setCount(1);
                    this.client.setScreen(this);
                }));
            }
        }).dimensions(centerX - 40, centerY - 10, 120, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Save").formatted(Formatting.GREEN), button -> {
            saveArrows();
        }).dimensions(centerX - 60, centerY + 30, 120, 20).build());
        
        this.addDrawableChild(ButtonWidget.builder(Text.of("Cancel"), button -> {
            close();
        }).dimensions(centerX - 60, centerY + 55, 120, 20).build());
    }

    private void saveArrows() {
        int amount = 10;
        try {
            amount = Integer.parseInt(amountField.getText());
        } catch (Exception e) {}

        if (amount > 0) {
            List<ItemStack> list = new ArrayList<>();
            for (int i = 0; i < amount; i++) {
                list.add(selectedArrow.copy());
            }
            stack.set(DataComponentTypes.CHARGED_PROJECTILES, ChargedProjectilesComponent.of(list));
        } else {
            stack.remove(DataComponentTypes.CHARGED_PROJECTILES);
        }
        
        if (saveCallback != null) {
            saveCallback.accept(stack);
        }
        close();
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

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        context.drawItem(new ItemStack(Items.CROSSBOW), centerX - 65, centerY - 38);
        
        context.drawItem(selectedArrow, centerX - 65, centerY - 8);
    }

    @Override
    public void close() {
        if (this.client != null) this.client.setScreen(parent);
    }
}
