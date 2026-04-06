package com.trusted.systemnbteditor.mixin;

import com.trusted.systemnbteditor.data.ModConfig;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ClientPlayNetworkHandler.class)
public class ChatSanityMixin {
    
    @ModifyVariable(method = "sendChatMessage", at = @At("HEAD"), argsOnly = true)
    private String onSendChatMessage(String content) {
        if (ModConfig.getInstance().sectionFix && content != null) {
            return content.replace("§", "");
        }
        return content;
    }

    @ModifyVariable(method = "sendChatCommand", at = @At("HEAD"), argsOnly = true)
    private String onSendChatCommand(String command) {
        if (ModConfig.getInstance().sectionFix && command != null) {
            return command.replace("§", "");
        }
        return command;
    }
}
