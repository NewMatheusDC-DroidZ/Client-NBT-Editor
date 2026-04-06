package com.trusted.systemnbteditor.mixin;

import com.trusted.systemnbteditor.data.ModConfig;
import com.trusted.systemnbteditor.util.AntiCrashLogManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.EntityType;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class EntityLimitMixin {
    @Inject(method = "onEntitySpawn", at = @At("HEAD"), cancellable = true)
    private void onEntitySpawn(EntitySpawnS2CPacket packet, CallbackInfo ci) {
        ModConfig config = ModConfig.getInstance();
        if (!config.entityLimitEnabled) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        if (client.world.getRegularEntityCount() > config.entityThreshold) {
            if (packet.getEntityType() != EntityType.PLAYER) {
                AntiCrashLogManager.addLog("Blocked Entity Spawn (Threshold: " + config.entityThreshold + "): " + packet.getEntityType().getUntranslatedName());
                ci.cancel();
            }
        }
    }
}
