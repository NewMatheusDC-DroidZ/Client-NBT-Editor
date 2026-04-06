package com.trusted.systemnbteditor.mixin;

import com.trusted.systemnbteditor.data.ModConfig;
import com.trusted.systemnbteditor.util.AntiCrashLogManager;
import com.trusted.systemnbteditor.util.TranslationProtectionUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.ArrayList;

@Mixin(ItemStack.class)
public class ItemAntiCrashMixin {
    @Inject(method = "getName", at = @At("RETURN"), cancellable = true)
    private void onGetName(CallbackInfoReturnable<Text> cir) {
        ModConfig config = ModConfig.getInstance();
        Text text = cir.getReturnValue();
        if (text == null) return;

        // Check complexity first to avoid explosion in getString()
        boolean translationProtection = config.translationProtectionMode != ModConfig.TranslationProtectionMode.OFF;
        if (translationProtection || config.itemNameLimitEnabled || config.tridentProtectionEnabled) {
            if (TranslationProtectionUtil.isOverlyComplex(text)) {
                ItemStack stack = (ItemStack)(Object)this;
                AntiCrashLogManager.addLog("Blocked Complex Item: " + stack.getComponents().toString());
                cir.setReturnValue(Text.literal("[Blocked Translation]").formatted(net.minecraft.util.Formatting.RED));
                return;
            }
        }

        // 2. Name Length Limit
        if (config.itemNameLimitEnabled) {
            if (text.getString().length() > config.lengthThreshold) {
                ItemStack stack = (ItemStack)(Object)this;
                AntiCrashLogManager.addLog("Blocked Long Item Name: " + stack.getComponents().toString());
                cir.setReturnValue(Text.literal("[Long Name Blocked]").formatted(net.minecraft.util.Formatting.RED));
            }
        }
    }

    @Inject(method = "getTooltip", at = @At("RETURN"), cancellable = true)
    private void onGetTooltip(net.minecraft.item.Item.TooltipContext context, PlayerEntity player, TooltipType type, CallbackInfoReturnable<List<Text>> cir) {
        ModConfig config = ModConfig.getInstance();
        boolean translationProtection = config.translationProtectionMode != ModConfig.TranslationProtectionMode.OFF;
        if (!config.itemTooltipLimitEnabled && !translationProtection) return;

        List<Text> tooltip = cir.getReturnValue();
        if (tooltip == null || tooltip.isEmpty()) return;

        boolean modified = false;
        List<Text> newTooltip = new ArrayList<>();
        
        // 1. Tooltip Line Count Limit
        int maxLines = config.itemTooltipThreshold;
        int count = 0;
        
        for (Text line : tooltip) {
            if (config.itemTooltipLimitEnabled && count >= maxLines) {
                newTooltip.add(Text.literal("... [Tooltip Too Long]").formatted(net.minecraft.util.Formatting.RED));
                modified = true;
                break;
            }
            
            // 2. Translation Protection for Tooltip Lines
            if ((translationProtection || config.tridentProtectionEnabled) && TranslationProtectionUtil.isOverlyComplex(line)) {
                ItemStack stack = (ItemStack)(Object)this;
                AntiCrashLogManager.addLog("Blocked Complex Tooltip Line in: " + stack.getComponents().toString());
                newTooltip.add(Text.literal("[Blocked Translation]").formatted(net.minecraft.util.Formatting.RED));
                modified = true;
            } else if (config.itemNameLimitEnabled && line.getString().length() > config.lengthThreshold) {
                // Reuse itemNameLimit for line length if enabled
                ItemStack stack = (ItemStack)(Object)this;
                AntiCrashLogManager.addLog("Blocked Long Tooltip Line in: " + stack.getComponents().toString());
                newTooltip.add(Text.literal("[Line Too Long]").formatted(net.minecraft.util.Formatting.RED));
                modified = true;
            } else {
                newTooltip.add(line);
            }
            count++;
        }
        
        // 3. Optional Item Size Append from NBT Editor Tweak
        if (config.itemSize != null && !config.itemSize.equals("HIDDEN") && !config.itemSize.equals("AUTO")) {
            boolean compressed = config.itemSize.contains("COMPRESSED");
            java.util.OptionalLong loadingSize = com.trusted.systemnbteditor.util.ItemSizeUtil.getItemSize((ItemStack)(Object)this, compressed);
            String displaySize;
            net.minecraft.util.Formatting sizeFormat;
            if (loadingSize.isEmpty()) {
                displaySize = "...";
                sizeFormat = net.minecraft.util.Formatting.GRAY;
            } else {
                long size = loadingSize.getAsLong();
                int magnitude = 1;
                if (config.itemSize.startsWith("KILOBYTE")) magnitude = 1000;
                else if (config.itemSize.startsWith("MEGABYTE")) magnitude = 1000000;
                else if (config.itemSize.startsWith("GIGABYTE")) magnitude = 1000000000;

                if (magnitude == 1) displaySize = String.valueOf(size);
                else displaySize = String.format("%.1f", (double) size / magnitude);

                switch (magnitude) {
                    case 1 -> { displaySize += "B"; sizeFormat = net.minecraft.util.Formatting.GREEN; }
                    case 1000 -> { displaySize += "KB"; sizeFormat = net.minecraft.util.Formatting.YELLOW; }
                    case 1000000 -> { displaySize += "MB"; sizeFormat = net.minecraft.util.Formatting.RED; }
                    case 1000000000 -> { displaySize += "GB"; sizeFormat = net.minecraft.util.Formatting.DARK_RED; }
                    default -> sizeFormat = net.minecraft.util.Formatting.WHITE;
                }
            }
            net.minecraft.text.TextColor sizeColor = sizeFormat != null ? net.minecraft.text.TextColor.fromFormatting(sizeFormat) :
                    net.minecraft.text.TextColor.fromRgb(java.awt.Color.HSBtoRGB((System.currentTimeMillis() % 1000) / 1000.0f, 1, 1));
            
            String label = config.itemSize.contains("COMPRESSED") ? "Compressed Size: " : "Size: ";
            newTooltip.add(Text.literal(label).formatted(net.minecraft.util.Formatting.DARK_GRAY)
                .append(Text.literal(displaySize).styled(style -> style.withColor(sizeColor))));
            modified = true;
        }

        if (modified) {
            cir.setReturnValue(newTooltip);
        }
    }
}
