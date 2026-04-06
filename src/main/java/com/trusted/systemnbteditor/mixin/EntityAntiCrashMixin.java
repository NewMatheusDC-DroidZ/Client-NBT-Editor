package com.trusted.systemnbteditor.mixin;

import com.trusted.systemnbteditor.data.ModConfig;
import com.trusted.systemnbteditor.util.AntiCrashLogManager;
import com.trusted.systemnbteditor.util.TranslationProtectionUtil;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.predicate.NbtPredicate;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityAntiCrashMixin {

    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void onGetDisplayName(CallbackInfoReturnable<Text> cir) {
        ModConfig config = ModConfig.getInstance();
        Text text = cir.getReturnValue();
        if (text == null) return;

        // Check translation complexity first to avoid explosion in getString()
        boolean translationProtection = config.translationProtectionMode != ModConfig.TranslationProtectionMode.OFF;
        if (config.entityNameLimitEnabled || translationProtection || config.tridentProtectionEnabled) {
            if (TranslationProtectionUtil.isOverlyComplex(text)) {
                NbtCompound nbt = NbtPredicate.entityToNbt((Entity) (Object) this);
                AntiCrashLogManager.addLog("Blocked Complex Entity Name in: " + nbt.toString());
                cir.setReturnValue(Text.literal("[Blocked Translation]").formatted(net.minecraft.util.Formatting.RED));
                return;
            }
        }

        // Entity Name Length Limit
        if (config.entityNameLimitEnabled) {
            if (text.getString().length() > config.lengthThreshold) {
                NbtCompound nbt = NbtPredicate.entityToNbt((Entity) (Object) this);
                AntiCrashLogManager.addLog("Blocked Long Entity Name in: " + nbt.toString());
                cir.setReturnValue(Text.literal("[Long Name Blocked]").formatted(net.minecraft.util.Formatting.RED));
            }
        }
    }
}
