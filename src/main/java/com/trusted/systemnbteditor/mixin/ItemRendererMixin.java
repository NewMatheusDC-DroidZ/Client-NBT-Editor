package com.trusted.systemnbteditor.mixin;

import com.trusted.systemnbteditor.data.ModConfig;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemRenderer.class)
public class ItemRendererMixin {

    @ModifyVariable(method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/render/model/BakedModel;)V", 
                    at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private VertexConsumerProvider modifyVertexConsumersModelMode(VertexConsumerProvider originalConsumers, ItemStack stack) {
        if (!ModConfig.getInstance().enchantGlintFix || !stack.hasGlint() || !(stack.getItem() instanceof BlockItem)) {
            return originalConsumers;
        }

        return layer -> ItemRenderer.getItemGlintConsumer(originalConsumers, layer, true, true);
    }
}
