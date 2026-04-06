package com.trusted.systemnbteditor.mixin;

import com.trusted.systemnbteditor.data.ModConfig;
import com.trusted.systemnbteditor.util.AntiCrashLogManager;
import com.trusted.systemnbteditor.util.TranslationProtectionUtil;
import net.minecraft.text.MutableText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.util.Formatting;
import net.minecraft.text.Text;
import net.minecraft.text.MutableText;

@Mixin(TranslatableTextContent.class)
public class TranslatableTextMixin {
    @Shadow
    private String key;

    @Shadow
    private Object[] args;

    // Pattern to match positional arguments like %1$s, %2$d, %100$f, etc.
    private static final Pattern POSITIONAL_ARG_PATTERN = Pattern.compile("%([0-9]+)\\$[a-zA-Z]");

    @Inject(method = "getArg", at = @At("HEAD"), cancellable = true)
    private void onGetArg(int index, CallbackInfoReturnable<Object> cir) {
        ModConfig.TranslationProtectionMode mode = ModConfig.getInstance().translationProtectionMode;
        boolean translationProtection = mode != ModConfig.TranslationProtectionMode.OFF;
        boolean tridentProtection = ModConfig.getInstance().tridentProtectionEnabled;

        if (!translationProtection && !tridentProtection) {
            return;
        }

        // Enchantment Level Check handled in visit
        
        // Protection logic (Translation OR Trident)
        if (translationProtection || tridentProtection) {
            boolean whitelisted = TranslationProtectionUtil.isWhitelisted(this.key);

            // 1. Key Length Check (Fast fail) - Whitelisted keys can be longer
            if (this.key != null && this.key.length() > (whitelisted ? 2048 : 512)) {
                AntiCrashLogManager.addLog("Blocked Long translation Key: " + this.key);
                cir.setReturnValue(getBlockedText(mode, this.key));
                return;
            }

            // Skip strict checks for whitelisted Minecraft commands/chat
            if (!whitelisted) {
                // 2. Arg Count Check (Fast fail)
                if (this.args != null && this.args.length > 32) {
                    AntiCrashLogManager.addLog("Blocked Translatable with " + this.args.length + " args. Key: " + this.key);
                    cir.setReturnValue(getBlockedText(mode, this.key));
                    return;
                }

                // 3. Pattern-Based Detection for Positional Argument Abuse
                if (this.key != null && containsDangerousPattern(this.key)) {
                    AntiCrashLogManager.addLog("Blocked Explosive Translation Pattern: " + this.key);
                    cir.setReturnValue(getBlockedText(mode, this.key));
                    return;
                }
            }
        }
    }

    private Text getBlockedText(ModConfig.TranslationProtectionMode mode, String key) {
        if (mode == ModConfig.TranslationProtectionMode.SMART) {
            String extracted = null;
            if (this.args != null && this.args.length > 0) {
                 extracted = TranslationProtectionUtil.safelyExtractText(this.args);
            }
            return Text.literal(extracted != null && !extracted.isEmpty() ? extracted : (key != null ? key : "null"));
        } else {
            return Text.literal("[Translation Blocked]").formatted(net.minecraft.util.Formatting.RED);
        }
    }

    @Inject(method = "visit(Lnet/minecraft/text/StringVisitable$Visitor;)Ljava/util/Optional;", at = @At("HEAD"), cancellable = true)
    private <T> void onVisit(StringVisitable.Visitor<T> visitor, CallbackInfoReturnable<Optional<T>> cir) {
        if (ModConfig.getInstance().useArabicEnchantLevels && this.key != null) {
            if (this.key.startsWith("enchantment.level.")) {
                cir.setReturnValue(visitor.accept(this.key.substring("enchantment.level.".length())));
                return;
            }
            if (this.key.contains(".enchantmentlevel")) {
                int index = this.key.indexOf(".enchantmentlevel");
                String prefix = this.key.substring(0, index);
                if (!prefix.isEmpty()) prefix = prefix.substring(0, 1).toUpperCase() + prefix.substring(1);
                String result = prefix + " " + this.key.substring(index + ".enchantmentlevel".length());
                cir.setReturnValue(visitor.accept(result));
                return;
            }
        }

        boolean translationProtection = ModConfig.getInstance().translationProtectionMode != ModConfig.TranslationProtectionMode.OFF;
        if (!translationProtection && !ModConfig.getInstance().tridentProtectionEnabled) return;

        if (TranslationProtectionUtil.shouldInterruptVisitation()) {
            AntiCrashLogManager.addLog("Interrupted Recursive Translation Visitation: " + (this.key != null ? this.key : "unknown"));
            cir.setReturnValue(Optional.empty());
            return;
        }

        TranslationProtectionUtil.enterVisitation();
        try {
            if (TranslationProtectionUtil.isOverlyComplex(this, this.key)) {
                AntiCrashLogManager.addLog("Blocked Complex Translation Visitation: " + (this.key != null ? this.key : "unknown"));
                cir.setReturnValue(Optional.empty());
            }
        } finally {
            TranslationProtectionUtil.exitVisitation();
        }
    }

    @Inject(method = "visit(Lnet/minecraft/text/StringVisitable$StyledVisitor;Lnet/minecraft/text/Style;)Ljava/util/Optional;", at = @At("HEAD"), cancellable = true)
    private <T> void onVisitStyled(StringVisitable.StyledVisitor<T> visitor, Style style, CallbackInfoReturnable<Optional<T>> cir) {
        if (ModConfig.getInstance().useArabicEnchantLevels && this.key != null) {
            if (this.key.startsWith("enchantment.level.")) {
                cir.setReturnValue(visitor.accept(style, this.key.substring("enchantment.level.".length())));
                return;
            }
            if (this.key.contains(".enchantmentlevel")) {
                int index = this.key.indexOf(".enchantmentlevel");
                String prefix = this.key.substring(0, index);
                if (!prefix.isEmpty()) prefix = prefix.substring(0, 1).toUpperCase() + prefix.substring(1);
                String result = prefix + " " + this.key.substring(index + ".enchantmentlevel".length());
                cir.setReturnValue(visitor.accept(style, result));
                return;
            }
        }

        boolean translationProtection = ModConfig.getInstance().translationProtectionMode != ModConfig.TranslationProtectionMode.OFF;
        if (!translationProtection && !ModConfig.getInstance().tridentProtectionEnabled) return;

        if (TranslationProtectionUtil.shouldInterruptVisitation()) {
            AntiCrashLogManager.addLog("Interrupted Recursive Styled Translation Visitation: " + (this.key != null ? this.key : "unknown"));
            cir.setReturnValue(Optional.empty());
            return;
        }

        TranslationProtectionUtil.enterVisitation();
        try {
            if (TranslationProtectionUtil.isOverlyComplex(this, this.key)) {
                AntiCrashLogManager.addLog("Blocked Complex Styled Translation Visitation: " + (this.key != null ? this.key : "unknown"));
                cir.setReturnValue(Optional.empty());
            }
        } finally {
            TranslationProtectionUtil.exitVisitation();
        }
    }

    private boolean containsDangerousPattern(String key) {
        Matcher matcher = POSITIONAL_ARG_PATTERN.matcher(key);
        Map<String, Integer> argCounts = new HashMap<>();
        int totalArgReferences = 0;
        
        while (matcher.find()) {
            String argNum = matcher.group(1);
            argCounts.put(argNum, argCounts.getOrDefault(argNum, 0) + 1);
            totalArgReferences++;
        }
        
        // Hardened limits:
        // Any single argument can only be referenced 4 times.
        for (int count : argCounts.values()) {
            if (count > 4) return true;
        }
        
        // Total positional references limit. The "Wolf Egg" uses 7. 
        // We'll set a strict limit of 6 to proactively block it.
        return totalArgReferences > 6;
    }
}
