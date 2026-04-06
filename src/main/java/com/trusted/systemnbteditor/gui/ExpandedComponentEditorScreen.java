package com.trusted.systemnbteditor.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import com.trusted.systemnbteditor.util.MixinState;
import com.trusted.systemnbteditor.data.ModConfig;
import java.util.function.Consumer;

public class ExpandedComponentEditorScreen extends Screen {
    private final Screen parent;
    private final String componentKey;
    private final String initialValue;
    private final Consumer<String> onDone;
    private EditBoxWidget editor;

    public ExpandedComponentEditorScreen(Screen parent, String componentKey, String initialValue, Consumer<String> onDone) {
        super(Text.of("Edit " + componentKey));
        this.parent = parent;
        this.componentKey = componentKey;
        this.initialValue = initialValue;
        this.onDone = onDone;
    }

    @Override
    protected void init() {
        super.init();

        int boxX = 20;
        int boxY = 40;
        int boxWidth = this.width - 40;
        int boxHeight = this.height - 80;

        this.editor = new EditBoxWidget.Builder()
                .x(boxX)
                .y(boxY)
                .build(this.textRenderer, boxWidth, boxHeight, Text.of("Component NBT"));
        
        this.editor.setMaxLength(Integer.MAX_VALUE);
        this.editor.setText(initialValue);
        this.addSelectableChild(this.editor);

        // Done Button: Bottom Left
        int btnWidth = 100;
        int btnHeight = 20;
        int btnX = 20;
        int btnY = this.height - 30;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Done").formatted(Formatting.GREEN), button -> {
            onDone.accept(this.editor.getText());
            if (this.client != null) this.client.setScreen(this.parent);
        }).dimensions(btnX, btnY, btnWidth, btnHeight).build());

        // Cancel/Back Button: Bottom Right-ish
        this.addDrawableChild(ButtonWidget.builder(Text.of("Cancel"), button -> {
             if (this.client != null) this.client.setScreen(this.parent);
        }).dimensions(this.width - 120, btnY, 100, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 10, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Component: " + componentKey).formatted(Formatting.GRAY), this.width / 2, 25, 0xFFAAAAAA);

        boolean showBackground = ModConfig.getInstance().showEditorBackground;
        MixinState.isRenderingNbtEditor = true;
        if (!showBackground) {
            MixinState.isRenderingEditorContents = true;
        }

        this.editor.render(context, mouseX, mouseY, delta);

        if (!showBackground) {
             int bgBorderColor = -6250336;
             int bx = this.editor.getX();
             int by = this.editor.getY();
             int bw = this.editor.getWidth();
             int bh = this.editor.getHeight();
             context.fill(bx, by, bx + bw, by + 1, bgBorderColor);
             context.fill(bx, by + bh - 1, bx + bw, by + bh, bgBorderColor);
             context.fill(bx, by, bx + 1, by + bh, bgBorderColor);
             context.fill(bx + bw - 1, by, bx + bw, by + bh, bgBorderColor);
        }

        MixinState.isRenderingEditorContents = false;
        MixinState.isRenderingNbtEditor = false;
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
