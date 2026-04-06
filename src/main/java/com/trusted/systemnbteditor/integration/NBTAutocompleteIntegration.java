package com.trusted.systemnbteditor.integration;

import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.argument.ItemStringReader;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.Identifier;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class NBTAutocompleteIntegration {
    private static Class<?> managerClass;
    private static Method loadFromName;
    private static Map<Suggestion, String> subtextMap;
    private static boolean initialized = false;

    private static void init() {
        if (initialized) return;
        try {
            managerClass = Class.forName("com.mt1006.nbt_ac.autocomplete.NbtSuggestionManager");
            loadFromName = managerClass.getMethod("loadFromName", String.class, String.class, SuggestionsBuilder.class, boolean.class);
            Field subtextField = managerClass.getField("subtextMap");
            subtextMap = (Map<Suggestion, String>) subtextField.get(null);
            initialized = true;
        } catch (ClassNotFoundException e) {
            initialized = false;
        } catch (Exception e) {
            e.printStackTrace();
            initialized = false;
        }
    }

    public static boolean isLoaded() {
        init();
        return initialized;
    }

    private static StringRange shiftRange(StringRange range, int shift) {
        return new StringRange(range.getStart() + shift, range.getEnd() + shift);
    }

    private static Suggestion shiftSuggestion(Suggestion suggestion, int shift) {
        Suggestion shiftedSuggestion = new Suggestion(shiftRange(suggestion.getRange(), shift), suggestion.getText(), suggestion.getTooltip());
        if (subtextMap != null) {
            subtextMap.put(shiftedSuggestion, subtextMap.remove(suggestion));
        }
        return shiftedSuggestion;
    }

    public static CompletableFuture<Suggestions> getSuggestions(String type, String id, String pathStr, String value, int cursor) {
        boolean components = type.equals("item");
        String fullValue = value.substring(0, Math.min(cursor, value.length()));
        String tag = pathStr + fullValue;
        int fieldStart = pathStr.length();
        
        SuggestionsBuilder builder = new SuggestionsBuilder(tag, fieldStart);
        CompletableFuture<Suggestions> suggestionsFuture;

        if (isLoaded()) {
            if (components && pathStr.isEmpty()) {
                try {
                    var world = MinecraftClient.getInstance().world;
                    if (world != null) {
                        var lookup = world.getRegistryManager();
                        suggestionsFuture = new ItemStringReader(lookup).getSuggestions(builder);
                    } else {
                        suggestionsFuture = loadFromName(type + "/" + id, tag, builder);
                    }
                } catch (Exception e) {
                    suggestionsFuture = loadFromName(type + "/" + id, tag, builder);
                }
            } else {
                suggestionsFuture = loadFromName(type + "/" + id, tag, builder);
            }
        } else {
            // Fallback suggestions if external mod is not loaded
            if (components && pathStr.isEmpty()) {
                // Suggest component keys from ComponentRegistry
                for (com.trusted.systemnbteditor.data.ComponentRegistry.ComponentInfo info : com.trusted.systemnbteditor.data.ComponentRegistry.getAllComponents()) {
                    builder.suggest(info.key());
                    if (!info.key().startsWith("minecraft:")) {
                        builder.suggest("minecraft:" + info.key());
                    }
                }
                suggestionsFuture = builder.buildFuture();
            } else if (components && pathStr.startsWith("[")) {
                 // Suggest values for specific components if known
                 String key = pathStr.substring(1).replace("=", "");
                 if (key.endsWith("max_stack_size")) {
                     builder.suggest("64");
                     builder.suggest("16");
                     builder.suggest("1");
                 } else if (key.endsWith("unbreakable")) {
                     builder.suggest("{}");
                 } else if (key.endsWith("custom_name") || key.endsWith("item_name")) {
                     builder.suggest("\"\"");
                     builder.suggest("{\"text\":\"\"}");
                 } else if (key.endsWith("lore")) {
                     builder.suggest("[]");
                 } else if (key.endsWith("rarity")) {
                     builder.suggest("\"common\"");
                     builder.suggest("\"uncommon\"");
                     builder.suggest("\"rare\"");
                     builder.suggest("\"epic\"");
                 }
                 suggestionsFuture = builder.buildFuture();
            } else {
                suggestionsFuture = builder.buildFuture();
            }
        }

        return suggestionsFuture.thenApply(suggestions -> {
            // Regex-based filtering for suggestions
            java.util.regex.Pattern pattern = null;
            String regexStr = fullValue;
            try {
                if (regexStr.contains("*") || regexStr.contains("?") || regexStr.contains("[") || regexStr.contains("(")) {
                    String regex = regexStr.replace("*", ".*").replace("?", ".");
                    if (!regex.startsWith(".*")) regex = "^" + regex;
                    pattern = java.util.regex.Pattern.compile(regex, java.util.regex.Pattern.CASE_INSENSITIVE);
                }
            } catch (Exception ignored) {}

            final java.util.regex.Pattern finalPattern = pattern;
            List<Suggestion> filteredSuggestions = suggestions.getList().stream()
                .filter(suggestion -> {
                    String text = suggestion.getText();
                    if (finalPattern != null) {
                        return finalPattern.matcher(text).find();
                    }
                    return text.toLowerCase().contains(fullValue.toLowerCase());
                })
                .map(suggestion -> shiftSuggestion(suggestion, -fieldStart))
                .collect(Collectors.toList());
            
            return new Suggestions(shiftRange(suggestions.getRange(), -fieldStart), filteredSuggestions);
        });
    }

    private static CompletableFuture<Suggestions> loadFromName(String name, String tag, SuggestionsBuilder builder) {
        try {
            return (CompletableFuture<Suggestions>) loadFromName.invoke(null, name, tag, builder, false);
        } catch (Exception e) {
            return builder.buildFuture();
        }
    }

    public static CompletableFuture<Suggestions> getItemSuggestions(String itemId, String nbt, SuggestionsBuilder builder) {
        return getSuggestions("item", itemId, "", nbt, builder.getRemaining().length());
    }

    public static CompletableFuture<Suggestions> getSuggestions(String itemId, String nbt, SuggestionsBuilder builder) {
        return getSuggestions("item", itemId, "", nbt, builder.getRemaining().length());
    }
}
