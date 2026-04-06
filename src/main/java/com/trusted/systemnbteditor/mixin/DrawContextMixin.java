package com.trusted.systemnbteditor.mixin;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.trusted.systemnbteditor.util.MixinState;
import com.trusted.systemnbteditor.util.NbtSyntaxHighlighter;

@Mixin(DrawContext.class)
public abstract class DrawContextMixin {

    private static boolean isHighlighting = false;

    @Inject(method = "drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;IIIZ)V", at = @At("HEAD"), cancellable = true)
    private void onDrawOrderedText(TextRenderer textRenderer, OrderedText orderedText, int x, int y, int color,
            boolean shadow, CallbackInfo ci) {
        if ((MixinState.isRenderingNbtEditor || MixinState.isRenderingAntiCrashLog || MixinState.isRenderingTellraw) && !isHighlighting) {
            isHighlighting = true;
            try {
                // Reconstruct string from OrderedText
                StringBuilder sb = new StringBuilder();
                orderedText.accept((index, style, codePoint) -> {
                    sb.append((char) codePoint);
                    return true;
                });
                String content = sb.toString();

                // Format with syntax highlighting
                Text highlighted = NbtSyntaxHighlighter.format(content);

                // Draw the highlighted version
                // This will likely call the String or Text overload of drawText
                ((DrawContext) (Object) this).drawText(textRenderer, highlighted, x, y, color, shadow);

                // Cancel the original call to prevent double-rendering
                ci.cancel();
            } finally {
                isHighlighting = false;
            }
        }
    }

    @Inject(method = "fill(IIIII)V", at = @At("HEAD"), cancellable = true)
    private void onFill(int x1, int y1, int x2, int y2, int color, CallbackInfo ci) {
        if (MixinState.isRenderingEditorContents) {
            int width = Math.abs(x2 - x1);
            if (width > 2) {
                ci.cancel();
            }
        } else if (MixinState.isRenderingNbtEditor) {
             // System.out.println("DrawContextMixin: Fill called but isRenderingEditorContents is false. Width: " + Math.abs(x2 - x1));
        }
    }

    @Inject(method = "fillGradient(IIIIII)V", at = @At("HEAD"), cancellable = true)
    private void onFillGradient(int startX, int startY, int endX, int endY, int colorStart, int colorEnd, CallbackInfo ci) {
        if (MixinState.isRenderingEditorContents) {
            int width = Math.abs(endX - startX);
            // System.out.println("DrawContextMixin: Checking fillGradient of width " + width);
            if (width > 2) {
                // System.out.println("DrawContextMixin: Cancelling fillGradient of width " + width);
                ci.cancel();
            }
        }
    }
}
