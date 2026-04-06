package com.trusted.systemnbteditor.mixin;

import com.trusted.systemnbteditor.data.ModConfig;
import com.trusted.systemnbteditor.util.AntiCrashLogManager;
import com.trusted.systemnbteditor.util.TranslationProtectionUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.entity.boss.BossBar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BossBarHud.class)
public class BossBarHudMixin {
    @Inject(method = "renderBossBar", at = @At("HEAD"), cancellable = true)
    private void onRenderBossBar(DrawContext context, int x, int y, BossBar bossBar, CallbackInfo ci) {
        ModConfig config = ModConfig.getInstance();
        net.minecraft.text.Text name = bossBar.getName();
        if (config.bossbarLimitEnabled && name != null) {
            boolean translationProtection = config.translationProtectionMode != ModConfig.TranslationProtectionMode.OFF;
            if ((translationProtection || config.tridentProtectionEnabled) && TranslationProtectionUtil.isOverlyComplex(name)) {
                AntiCrashLogManager.addLog("Blocked Complex BossBar Title: " + name.getString().substring(0, Math.min(name.getString().length(), 50)) + "...");
                ci.cancel();
                return;
            }
            if (name.getString().length() > config.lengthThreshold) {
                AntiCrashLogManager.addLog("Blocked Long BossBar Title (" + name.getString().length() + " chars)");
                // Completely cancel rendering of this bossbar
                ci.cancel();
            }
        }
    }
}
