package com.trusted.systemnbteditor.gui;

import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryOps;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.command.argument.NbtCompoundArgumentType;
import com.mojang.brigadier.StringReader;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.client.MinecraftClient;

public class TestParser {
    public static void test(String key, String text) {
        try {
            var client = MinecraftClient.getInstance();
            if (client != null && client.world != null) {
                RegistryWrapper.WrapperLookup lookup = client.world.getRegistryManager();
                var ops = RegistryOps.of(NbtOps.INSTANCE, lookup);
                
                String wrapped = "{\"v\":" + text + "}";
                System.out.println("Wrapped: " + wrapped);
                
                NbtCompound compound = NbtCompoundArgumentType.nbtCompound().parse(new StringReader(wrapped));
                System.out.println("Parsed Compound: " + compound);
                
                NbtElement nbt = compound.get("v");
                System.out.println("Extracted NBT: " + nbt);
                
                if (nbt != null) {
                    ComponentType<?> type = Registries.DATA_COMPONENT_TYPE.get(net.minecraft.util.Identifier.of(key));
                    if (type != null) {
                        var result = type.getCodecOrThrow().parse(ops, nbt);
                        if (result.error().isPresent()) {
                            System.err.println("Codec Parse Error for " + key + ": " + result.error().get().toString());
                        } else {
                            System.out.println("Successfully parsed: " + result.result().get());
                        }
                    } else {
                        System.err.println("Component Type not found: " + key);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
