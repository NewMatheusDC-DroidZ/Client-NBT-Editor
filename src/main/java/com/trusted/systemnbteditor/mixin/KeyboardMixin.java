package com.trusted.systemnbteditor.mixin;

import com.trusted.systemnbteditor.data.ModConfig;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class KeyboardMixin {
    @Shadow @Final private MinecraftClient client;

    @Inject(method = "onKey", at = @At("HEAD"))
    private void onKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo info) {
        // Open NBT Editor with 'N' (78)
        if (key == GLFW.GLFW_KEY_N && action == GLFW.GLFW_PRESS) {
            if (client.currentScreen == null && client.player != null) {
                ItemStack stack = client.player.getMainHandStack();
                if (stack.isEmpty()) {
                    stack = net.minecraft.item.Items.AIR.getDefaultStack();
                }
                final ItemStack finalStack = stack;
                client.execute(() -> client.setScreen(new com.trusted.systemnbteditor.gui.NbtEditorScreen(finalStack)));
            }
        }
    }
}
