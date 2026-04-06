package com.trusted.systemnbteditor.mixin;

import com.trusted.systemnbteditor.data.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.gui.tooltip.TooltipPositioner;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(DrawContext.class)
public abstract class TooltipScalingMixin {

    @Inject(
            method = "drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;IILnet/minecraft/client/gui/tooltip/TooltipPositioner;Lnet/minecraft/util/Identifier;Z)V",
            at = @At("HEAD")
    )
    private void onDrawTooltipPush(TextRenderer textRenderer, List<TooltipComponent> components, int x, int y, TooltipPositioner positioner, Identifier texture, boolean hasBorder, CallbackInfo ci) {
        if (!ModConfig.getInstance().tooltipOverflowFix || components == null || components.isEmpty()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) return;

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        int width = 0;
        int height = components.size() == 1 ? -2 : 0;
        for (TooltipComponent component : components) {
            width = Math.max(width, component.getWidth(textRenderer));
            int estimatedHeight = 10; // Default text height
            String n = component.getClass().getSimpleName();
            if (n.contains("Bundle") || n.contains("Map") || n.contains("Item")) {
                estimatedHeight = 24; // Larger components
            }
            height += estimatedHeight;
        }

        width += 10;
        height += 10;

        if (width > screenWidth || height > screenHeight) {
            float scale = Math.min((float) screenWidth / width, (float) screenHeight / height);
            
            // To scale keeping it visible, we scale down everything including translations after the push.
            Object matrices = ((DrawContext) (Object) this).getMatrices();
            try {
                java.lang.reflect.Method m = matrices.getClass().getMethod("scale", float.class, float.class);
                m.invoke(matrices, scale, scale);
            } catch (Exception e) {
                try {
                    java.lang.reflect.Method m2 = matrices.getClass().getMethod("scale", float.class, float.class, float.class);
                    m2.invoke(matrices, scale, scale, 1.0f);
                } catch (Exception e2) {}
            }
        }
    }
}
