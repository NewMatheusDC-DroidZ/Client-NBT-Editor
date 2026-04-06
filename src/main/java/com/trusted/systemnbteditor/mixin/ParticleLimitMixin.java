package com.trusted.systemnbteditor.mixin;

import com.trusted.systemnbteditor.data.ModConfig;
import com.trusted.systemnbteditor.util.AntiCrashLogManager;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ParticleLimitMixin {
    @Inject(method = "onParticle", at = @At("HEAD"), cancellable = true)
    private void onParticle(ParticleS2CPacket packet, CallbackInfo ci) {
        ModConfig config = ModConfig.getInstance();
        if (!config.particleLimitEnabled) return;

        if (packet.getCount() > config.particleThreshold) {
            AntiCrashLogManager.addLog("Blocked Particle Spam (" + packet.getCount() + " particles, Threshold: " + config.particleThreshold + ")");
            ci.cancel();
        }
    }
}
