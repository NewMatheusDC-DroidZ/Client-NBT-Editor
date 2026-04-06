package com.trusted.systemnbteditor.mixin;

import net.minecraft.registry.Registries;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.io.File;
import java.io.FileWriter;
import net.minecraft.item.Item;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.RegistryOps;

@Mixin(MinecraftClient.class)
public class TestMixin {
    private static boolean ran = false;
    @Inject(method = "setScreen", at = @At("HEAD"))
    private void runTests(Screen screen, CallbackInfo ci) {
        if (ran) return;
        ran = true;
        try {
            File dump = new File("components_examples.txt");
            FileWriter writer = new FileWriter(dump);
            MinecraftClient client = MinecraftClient.getInstance();
            var ops = RegistryOps.of(net.minecraft.nbt.NbtOps.INSTANCE, client.world.getRegistryManager());
            
            for (Item item : Registries.ITEM) {
                ComponentMap map = item.getComponents();
                for (var type : map.getTypes()) {
                    try {
                        Object val = map.get(type);
                        ComponentType<Object> objType = (ComponentType<Object>) type;
                        var elem = objType.getCodecOrThrow().encodeStart(ops, val).getOrThrow();
                        String id = Registries.DATA_COMPONENT_TYPE.getId(type).toString();
                        if (elem instanceof net.minecraft.nbt.NbtString ns) {
                            writer.write(id + " | " + ns.asString() + "\n");
                        } else {
                            writer.write(id + " | " + elem.toString() + "\n");
                        }
                    } catch (Exception e) {}
                }
            }
            writer.close();
            System.out.println("Dumped to " + dump.getAbsolutePath());
        } catch (Exception e) {}
    }
}
