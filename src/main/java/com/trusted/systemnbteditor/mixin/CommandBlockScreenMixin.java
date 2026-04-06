package com.trusted.systemnbteditor.mixin;

import com.trusted.systemnbteditor.data.ModConfig;
import net.minecraft.client.gui.screen.ingame.CommandBlockScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CommandBlockScreen.class)
public class CommandBlockScreenMixin {
    @Inject(method = "updateCommandBlock", at = @At("HEAD"))
    private void onUpdateCommandBlock(CallbackInfo ci) {
        ModConfig config = ModConfig.getInstance();
        if (!config.commandBlockLimitEnabled) return;

        TextFieldWidget commandField = ((CommandAccessor) this).getCommandTextField();
        if (commandField != null) {
            String command = commandField.getText();
            if (command.length() > config.commandBlockThreshold) {
                commandField.setText(command.substring(0, config.commandBlockThreshold));
            }
        }
    }
}
