package com.trusted.systemnbteditor.mixin;

import net.minecraft.text.TranslatableTextContent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TranslatableTextContent.class)
public interface TranslatableTextContentAccessor {
    @Accessor("key")
    String getKey();

    @Accessor("args")
    Object[] getArgs();
}
