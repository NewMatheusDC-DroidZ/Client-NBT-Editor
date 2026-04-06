package com.trusted.systemnbteditor.mixin;

import com.trusted.systemnbteditor.data.ModConfig;
import com.trusted.systemnbteditor.util.AntiCrashLogManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class OtherAntiCrashMixin {
    @Inject(method = "onEntityStatus", at = @At("HEAD"), cancellable = true)
    private void onEntityStatus(EntityStatusS2CPacket packet, CallbackInfo ci) {
        ModConfig config = ModConfig.getInstance();
        if (!config.cancelFireworks) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        Entity entity = packet.getEntity(client.world);
        if (entity instanceof FireworkRocketEntity && config.cancelFireworks) {
            AntiCrashLogManager.addLog("Blocked Firework Status Update.");
            ci.cancel();
            return;
        }

        // Logic check for elder guardian (status code 60 is Elder Guardian effect)
        if (packet.getStatus() == 60 && config.cancelElderGuardian) {
            AntiCrashLogManager.addLog("Blocked Elder Guardian Status Update (Effect 60).");
            ci.cancel();
        }
    }
}
