package com.trusted.systemnbteditor.util;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.OptionalLong;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ItemSizeUtil {

    private static class ByteCountingOutputStream extends OutputStream {
        private long count;

        public long getCount() {
            return count;
        }

        @Override
        public void write(int b) {
            count++;
        }

        @Override
        public void write(byte[] b) {
            count += b.length;
        }

        @Override
        public void write(byte[] b, int off, int len) {
            count += len;
        }
    }

    private static final WeakHashMap<ItemStack, OptionalLong> uncompressedSizes = new WeakHashMap<>();
    private static final WeakHashMap<ItemStack, OptionalLong> compressedSizes = new WeakHashMap<>();

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    public static OptionalLong getItemSize(ItemStack stack, boolean compressed) {
        if (stack.getComponentChanges().isEmpty()) {
            return OptionalLong.of(calcItemSize(stack, compressed));
        }
        WeakHashMap<ItemStack, OptionalLong> sizes = (compressed ? compressedSizes : uncompressedSizes);
        OptionalLong size;
        synchronized (sizes) {
            size = sizes.get(stack);
            if (size != null) return size;
            size = OptionalLong.empty();
            sizes.put(stack, size);
        }

        EXECUTOR.execute(() -> {
            long knownSize = calcItemSize(stack, compressed);
            synchronized (sizes) {
                sizes.put(stack, OptionalLong.of(knownSize));
            }
        });

        return size;
    }

    private static long calcItemSize(ItemStack stack, boolean compressed) {
        ByteCountingOutputStream stream = new ByteCountingOutputStream();
        try {
            net.minecraft.registry.DynamicRegistryManager lookup = null;
            if (net.minecraft.client.MinecraftClient.getInstance() != null && net.minecraft.client.MinecraftClient.getInstance().world != null) {
                lookup = net.minecraft.client.MinecraftClient.getInstance().world.getRegistryManager();
            }
            if (lookup == null) return 0;
            var ops = net.minecraft.registry.RegistryOps.of(net.minecraft.nbt.NbtOps.INSTANCE, lookup);
            net.minecraft.nbt.NbtElement nbtElement = ItemStack.CODEC.encodeStart(ops, stack).getOrThrow();
            
            if (nbtElement instanceof NbtCompound nbt) {
                if (compressed) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    NbtIo.writeCompressed(nbt, out);
                    stream.write(out.toByteArray());
                } else {
                    java.io.DataOutputStream dos = new java.io.DataOutputStream(stream);
                    NbtIo.write(nbt, dos); // NbtIo.write requires DataOutput
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stream.getCount();
    }
}
