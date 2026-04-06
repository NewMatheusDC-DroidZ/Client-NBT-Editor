package com.trusted.systemnbteditor.mixin;

import com.trusted.systemnbteditor.util.ImportUtils;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;
import java.util.List;

@Mixin(Screen.class)
public class ScreenMixin {

    @Inject(method = "onFilesDropped", at = @At("HEAD"))
    private void onFilesDropped(List<Path> paths, CallbackInfo info) {
        for (Path path : paths) {
            ImportUtils.importNbt(path);
        }
    }
}
