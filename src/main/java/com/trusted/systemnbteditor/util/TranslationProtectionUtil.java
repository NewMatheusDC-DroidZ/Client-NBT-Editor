package com.trusted.systemnbteditor.util;

import com.trusted.systemnbteditor.mixin.TranslatableTextContentAccessor;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.text.TranslatableTextContent;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class TranslationProtectionUtil {
    private static final int MAX_NODES = 500;
    private static final int MAX_DEPTH = 8;
    private static final int MAX_VISITATION_DEPTH = 12;
    
    // Relaxed limits for whitelisted translations
    private static final int WHITELIST_MAX_NODES = 2000;
    private static final int WHITELIST_MAX_DEPTH = 16;

    // Track objects currently being processed in the current thread to detect cycles
    private static final ThreadLocal<Set<Object>> RECURSION_TRACKER = ThreadLocal.withInitial(HashSet::new);
    
    // Track the current visitation depth to prevent stack overflow during rendering
    private static final ThreadLocal<Integer> VISITATION_DEPTH = ThreadLocal.withInitial(() -> 0);

    public static boolean isOverlyComplex(Object obj) {
        return isOverlyComplex(obj, null);
    }

    public static boolean isOverlyComplex(Object obj, String key) {
        Set<Object> visited = RECURSION_TRACKER.get();
        visited.clear();
        boolean whitelisted = isWhitelisted(key);
        int maxNodes = whitelisted ? WHITELIST_MAX_NODES : MAX_NODES;
        int maxDepth = whitelisted ? WHITELIST_MAX_DEPTH : MAX_DEPTH;
        
        try {
            return isOverlyComplex(obj, 0, new AtomicInteger(0), visited, maxNodes, maxDepth);
        } finally {
            visited.clear();
        }
    }

    private static boolean isOverlyComplex(Object obj, int depth, AtomicInteger totalNodes, Set<Object> visited, int maxNodes, int maxDepth) {
        if (obj == null) return false;
        
        // Cycle detection
        if (!visited.add(obj)) return true;
        
        if (totalNodes.incrementAndGet() > maxNodes) return true;
        if (depth > maxDepth) return true;

        boolean complex = false;
        if (obj instanceof TranslatableTextContent content) {
            TranslatableTextContentAccessor accessor = (TranslatableTextContentAccessor) content;
            Object[] nestedArgs = accessor.getArgs();
            if (nestedArgs != null) {
                for (Object arg : nestedArgs) {
                    if (isOverlyComplex(arg, depth + 1, totalNodes, visited, maxNodes, maxDepth)) {
                        complex = true;
                        break;
                    }
                }
            }
        } else if (obj instanceof Text text) {
            if (isOverlyComplex(text.getContent(), depth, totalNodes, visited, maxNodes, maxDepth)) {
                complex = true;
            } else {
                for (Text sibling : text.getSiblings()) {
                    if (isOverlyComplex(sibling, depth, totalNodes, visited, maxNodes, maxDepth)) {
                        complex = true;
                        break;
                    }
                }
            }
        } else if (obj instanceof Object[] array) {
            for (Object o : array) {
                if (isOverlyComplex(o, depth, totalNodes, visited, maxNodes, maxDepth)) {
                    complex = true;
                    break;
                }
            }
        }
        
        visited.remove(obj);
        return complex;
    }

    public static boolean isWhitelisted(String key) {
        if (key == null) return false;
        return key.startsWith("commands.") || 
               key.startsWith("chat.") || 
               key.startsWith("multiplayer.") || 
               key.startsWith("options.") ||
               key.startsWith("advancements.") ||
               key.startsWith("entity.") ||
               key.startsWith("block.") ||
               key.startsWith("item.");
    }
    
    public static boolean shouldInterruptVisitation() {
        return VISITATION_DEPTH.get() > MAX_VISITATION_DEPTH;
    }

    public static void enterVisitation() {
        VISITATION_DEPTH.set(VISITATION_DEPTH.get() + 1);
    }

    public static void exitVisitation() {
        int depth = VISITATION_DEPTH.get();
        if (depth > 0) {
            VISITATION_DEPTH.set(depth - 1);
        }
    }

    public static Text getProtectedText(Text original, String blockMessage) {
        if (original == null) return null;
        if (isOverlyComplex(original)) {
            return Text.literal(blockMessage).formatted(net.minecraft.util.Formatting.RED);
        }
        return original;
    }

    public static String safelyExtractText(Object obj) {
        return safelyExtractText(obj, 0);
    }

    private static String safelyExtractText(Object obj, int depth) {
        if (obj == null || depth > MAX_DEPTH) return null;

        if (obj instanceof String s) {
            // Ignore placeholders
            if (s.startsWith("%") && s.length() < 5) return null;
            return s;
        }

        if (obj instanceof Text text) {
            String result = safelyExtractText(text.getContent(), depth + 1);
            if (result != null) return result;
            
            for (Text sibling : text.getSiblings()) {
                result = safelyExtractText(sibling, depth + 1);
                if (result != null) return result;
            }
        } else if (obj instanceof TranslatableTextContent content) {
            TranslatableTextContentAccessor accessor = (TranslatableTextContentAccessor) content;
            Object[] args = accessor.getArgs();
            if (args != null) {
                for (Object arg : args) {
                    String result = safelyExtractText(arg, depth + 1);
                    if (result != null) return result;
                }
            }
            // If no args yielded a result, maybe return the key if it's not a standard format specifier?
            // But usually the key is the formatting string like "%1$s". 
            // We want the content *inside* the args.
        } else if (obj instanceof Object[] array) {
            for (Object o : array) {
                String result = safelyExtractText(o, depth + 1);
                if (result != null) return result;
            }
        } else if (obj instanceof TextContent content) {
            // Fallback for other TextContent types (Literal, Keybind, Score, Nbt, etc.)
            // We use visit to extract the string representation safely.
            // Note: TranslatableTextContent is handled above, so this won't recurse dangerously.
            final String[] found = new String[1];
            content.visit((s) -> {
                found[0] = s;
                return java.util.Optional.empty();
            });
            return found[0];
        }

        return null;
    }
}
