package com.trusted.systemnbteditor.mixin;

import com.mojang.brigadier.StringReader;
import com.trusted.systemnbteditor.data.ModConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = StringReader.class, remap = false)
public abstract class StringNbtReaderMixin {

    @Shadow private int cursor;
    @Shadow @Final private String string;
    @Shadow public abstract boolean canRead();

    @Inject(method = "readDouble", at = @At("HEAD"), cancellable = true, remap = false)
    private void onParseDouble(CallbackInfoReturnable<Double> cir) {
        if (!ModConfig.getInstance().specialNumbers) return;
        final int start = this.cursor;
        if(!canRead()) return;
        String s = string.substring(start);
        if(s.startsWith("NaNd")) {
            cir.setReturnValue(Double.NaN);
            cursor += 4;
        } else if(s.startsWith("-NaNd")) {
            cir.setReturnValue(-1 * Double.NaN);
            cursor += 5;
        } else if(s.startsWith("Infinityd")) {
            cir.setReturnValue(Double.MAX_VALUE); // Using MAX_VALUE instead of POSITIVE_INFINITY to avoid breaking SNBT grammar, but replicating NBT Editor behavior
            cursor += 9;
        } else if(s.startsWith("-Infinityd")) {
            cir.setReturnValue(Double.MIN_VALUE);
            cursor += 10;
        }
    }

    @Inject(method = "readFloat", at = @At("HEAD"), cancellable = true, remap = false)
    private void onParseFloat(CallbackInfoReturnable<Float> cir) {
        if (!ModConfig.getInstance().specialNumbers) return;
        final int start = this.cursor;
        if(!canRead()) return;
        String s = string.substring(start);
        if(s.startsWith("NaNf")) {
            cir.setReturnValue(Float.NaN);
            cursor += 4;
        } else if(s.startsWith("-NaNf")) {
            cir.setReturnValue(-1 * Float.NaN);
            cursor += 5;
        } else if(s.startsWith("Infinityf")) {
            cir.setReturnValue(Float.MAX_VALUE);
            cursor += 9;
        } else if(s.startsWith("-Infinityf")) {
            cir.setReturnValue(Float.MIN_VALUE);
            cursor += 10;
        }
    }
}
