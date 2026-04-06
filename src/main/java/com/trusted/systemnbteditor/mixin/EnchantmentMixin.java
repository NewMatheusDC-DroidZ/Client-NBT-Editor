package com.trusted.systemnbteditor.mixin;

import com.trusted.systemnbteditor.data.ModConfig;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Enchantment.class)
public class EnchantmentMixin {

    @Inject(method = "getName", at = @At("RETURN"), cancellable = true)
    private static void onGetName(RegistryEntry<Enchantment> enchantment, int level, CallbackInfoReturnable<Text> cir) {
        if (!ModConfig.getInstance().useArabicEnchantLevels &&
            (ModConfig.getInstance().maxEnchantLevelDisplay == null || ModConfig.getInstance().maxEnchantLevelDisplay.equals("NEVER"))) {
            return;
        }

        MutableText nameText = net.minecraft.text.Text.translatable(
            net.minecraft.util.Util.createTranslationKey("enchantment", enchantment.getKey().get().getValue())
        );

        if (enchantment.isIn(net.minecraft.registry.tag.EnchantmentTags.CURSE)) {
            nameText.formatted(net.minecraft.util.Formatting.RED);
        } else {
            nameText.formatted(net.minecraft.util.Formatting.GRAY);
        }

        int maxLevel = enchantment.value().getMaxLevel();

        boolean useArabic = ModConfig.getInstance().useArabicEnchantLevels;
        String maxDisplay = ModConfig.getInstance().maxEnchantLevelDisplay;

        boolean shouldShowMax = false;
        if (maxDisplay != null) {
            if (maxDisplay.equals("ALWAYS")) shouldShowMax = true;
            else if (maxDisplay.equals("NOT_EXACT") && level != maxLevel) shouldShowMax = true;
            else if (maxDisplay.equals("NOT_MAX") && level < maxLevel) shouldShowMax = true;
        }

        // Only append level text if level != 1 OR maxLevel > 1 OR shouldShowMax
        if (level != 1 || maxLevel != 1 || shouldShowMax) {
            nameText.append(ScreenTexts.SPACE);
            if (useArabic) {
                nameText.append(Text.literal(String.valueOf(level)));
            } else {
                nameText.append(Text.translatable("enchantment.level." + level));
            }
        }

        if (shouldShowMax) {
            nameText.append(Text.literal("/"));
            if (useArabic) {
                nameText.append(Text.literal(String.valueOf(maxLevel)));
            } else {
                nameText.append(Text.translatable("enchantment.level." + maxLevel));
            }
        }

        cir.setReturnValue(nameText);
    }
}
