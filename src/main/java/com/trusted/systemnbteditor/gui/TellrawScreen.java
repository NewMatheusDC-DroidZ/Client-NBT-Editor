package com.trusted.systemnbteditor.gui;

import com.trusted.systemnbteditor.util.MixinState;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class TellrawScreen extends Screen {
    private final Screen parent;
    private Tab currentTab = Tab.JOIN;

    private TextFieldWidget nameField;
    private TextFieldWidget executorField;
    private TextFieldWidget oppedField;
    private EditBoxWidget commandPreview;
    private ButtonWidget runButton;

    private enum Tab {
        JOIN, LEAVE, OP
    }

    public TellrawScreen(Screen parent) {
        super(Text.of("Tellraw Utility"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int topY = 40;

        // Tabs
        addDrawableChild(ButtonWidget.builder(Text.literal("Fake Join"), btn -> { currentTab = Tab.JOIN; clearAndInit(); })
                .dimensions(centerX - 155, topY, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Fake Leave"), btn -> { currentTab = Tab.LEAVE; clearAndInit(); })
                .dimensions(centerX - 50, topY, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Fake OP"), btn -> { currentTab = Tab.OP; clearAndInit(); })
                .dimensions(centerX + 55, topY, 100, 20).build());

        int inputY = topY + 40;

        if (currentTab == Tab.JOIN || currentTab == Tab.LEAVE) {
            nameField = new TextFieldWidget(this.textRenderer, centerX - 125, inputY, 120, 20, Text.of("Username"));
            nameField.setPlaceholder(Text.literal("Username").formatted(Formatting.GRAY));
            nameField.setChangedListener(s -> updatePreview());
            addDrawableChild(nameField);

            runButton = ButtonWidget.builder(Text.literal("Run"), btn -> runCommand())
                    .dimensions(centerX + 5, inputY, 60, 20).build();
            addDrawableChild(runButton);
        } else if (currentTab == Tab.OP) {
            executorField = new TextFieldWidget(this.textRenderer, centerX - 150, inputY, 100, 20, Text.of("Executor"));
            executorField.setPlaceholder(Text.literal("Executor").formatted(Formatting.GRAY));
            executorField.setChangedListener(s -> updatePreview());
            addDrawableChild(executorField);

            oppedField = new TextFieldWidget(this.textRenderer, centerX - 45, inputY, 100, 20, Text.of("Opped"));
            oppedField.setPlaceholder(Text.literal("Opped").formatted(Formatting.GRAY));
            oppedField.setChangedListener(s -> updatePreview());
            addDrawableChild(oppedField);

            runButton = ButtonWidget.builder(Text.literal("Run"), btn -> runCommand())
                    .dimensions(centerX + 60, inputY, 60, 20).build();
            addDrawableChild(runButton);
        }

        // Preview Box
        commandPreview = new EditBoxWidget.Builder()
                .x(centerX - 250).y(inputY + 30)
                .build(this.textRenderer, 500, 40, Text.of("Command Preview"));
        commandPreview.setMaxLength(1000);
        addSelectableChild(commandPreview);

        // Back Button
        addDrawableChild(ButtonWidget.builder(Text.of("Back"), btn -> this.close())
                .dimensions(centerX - 50, this.height - 30, 100, 20).build());

        updatePreview();
    }

    private void updatePreview() {
        if (commandPreview == null) return;

        String cmd = "";
        if (currentTab == Tab.JOIN) {
            String name = (nameField != null && !nameField.getText().isEmpty()) ? nameField.getText() : "Namegoeshere";
            cmd = "/tellraw @a {\"text\":\"" + name + " joined the game\",\"color\":\"yellow\"}";
        } else if (currentTab == Tab.LEAVE) {
            String name = (nameField != null && !nameField.getText().isEmpty()) ? nameField.getText() : "Herobrine";
            cmd = "/tellraw @a {\"text\":\"" + name + " joined the game\",\"color\":\"yellow\"}";
        } else if (currentTab == Tab.OP) {
            String exec = (executorField != null && !executorField.getText().isEmpty()) ? executorField.getText() : "Executor";
            String opped = (oppedField != null && !oppedField.getText().isEmpty()) ? oppedField.getText() : "Namegoeshere";
            cmd = "/tellraw @a {\"text\":\"[" + exec + ": Made " + opped + " a server operator]\",\"italic\":true,\"color\":\"gray\"}";
        }
        commandPreview.setText(cmd);
    }

    private void runCommand() {
        if (this.client != null && this.client.player != null && commandPreview != null) {
            String fullCmd = commandPreview.getText();
            if (fullCmd.startsWith("/")) {
                this.client.player.networkHandler.sendChatCommand(fullCmd.substring(1));
            } else {
                this.client.player.networkHandler.sendChatMessage(fullCmd);
            }
            this.client.player.sendMessage(Text.literal("Command executed!").formatted(Formatting.GREEN), true);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, "Tellraw Utility", this.width / 2, 15, 0xFFFFFF00); // Yellow with full alpha

        MixinState.isRenderingTellraw = true;
        commandPreview.render(context, mouseX, mouseY, delta);
        MixinState.isRenderingTellraw = false;
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    protected void clearAndInit() {
        this.clearChildren();
        this.init();
    }
}
