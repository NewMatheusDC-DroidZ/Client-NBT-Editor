package com.trusted.systemnbteditor.util;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NbtSyntaxHighlighter {

    // Pattern breakdown:
    // Group 1 (Key): ([a-zA-Z0-9_]+|"[^"]*")(?=\s*:) -> Matches word or quoted
    // string followed by colon
    // Group 2 (String): ("(?:[^"\\]|\\.)*") -> Matches quoted string ensuring
    // escaped quotes are handled
    // Group 3 (Boolean): \b(true|false)\b -> Matches boolean
    // Group 4 (Number): (-?(?:\d+\.?\d*|\.\d+)) -> Value part of number
    // Group 5 (Suffix): ([dDfFsSbBlLiI]?)\b -> Optional type suffix
    // Group 6 (Syntax): ([{}[\]:,;]) -> Matches syntax chars including ;

    private static final Pattern NBT_PATTERN = Pattern.compile(
            "(\\\"(?:[^\\\\\\\"]|\\\\.)*\\\")(?=\\s*:)|" + // Group 1: Quoted Keys
                    "(\\\"(?:[^\\\\\\\"]|\\\\.)*\\\")|" + // Group 2: Strings
                    "([a-zA-Z0-9_]+)(?=\\s*:)|" + // Group 3: Unquoted Keys
                    "\\b(true|false)\\b|" + // Group 4: Booleans
                    "(-?(?:\\d+\\.?\\d*|\\.\\d+))([dDfFsSbBlLiI]?)\\b|" + // Group 5: Num value, Group 6: Suffix
                    "([{}\\[\\]:,;])|" + // Group 7: Syntax
                    "\\b(black|dark_blue|dark_green|dark_aqua|dark_red|dark_purple|gold|gray|dark_gray|blue|green|aqua|red|light_purple|yellow|white)\\b|" + // Group 8: Colors
                    "([a-z0-9_.-]+:[a-z0-9_./-]+)" // Group 9: Namespaces
    );

    public static Text format(String nbt) {
        MutableText root = Text.empty();
        Matcher matcher = NBT_PATTERN.matcher(nbt);
        int lastEnd = 0;

        while (matcher.find()) {
            // Append un-matched text as AQUA (No more white/gray)
            if (matcher.start() > lastEnd) {
                root.append(Text.literal(nbt.substring(lastEnd, matcher.start())).formatted(Formatting.AQUA));
            }

            if (matcher.group(1) != null || matcher.group(3) != null) {
                root.append(Text.literal(matcher.group()).formatted(Formatting.AQUA)); // Keys -> Aqua
            } else if (matcher.group(2) != null) {
                String str = matcher.group(2);
                if (str.contains(":")) {
                    root.append(Text.literal(str).formatted(Formatting.AQUA)); // Quoted namespaces -> Aqua
                } else {
                    root.append(Text.literal(str).formatted(Formatting.GREEN)); // Strings -> Green
                }
            } else if (matcher.group(4) != null) {
                root.append(Text.literal(matcher.group()).formatted(Formatting.GOLD)); // Booleans -> Gold
            } else if (matcher.group(5) != null) {
                // Number value in Gold
                root.append(Text.literal(matcher.group(5)).formatted(Formatting.GOLD));
                // Optional suffix in Gold (Uniform)
                if (matcher.group(6) != null && !matcher.group(6).isEmpty()) {
                    root.append(Text.literal(matcher.group(6)).formatted(Formatting.GOLD));
                }
            } else if (matcher.group(7) != null) {
                root.append(Text.literal(matcher.group()).formatted(Formatting.AQUA)); // Syntax -> Aqua
            } else if (matcher.group(8) != null) {
                root.append(Text.literal(matcher.group()).formatted(Formatting.GOLD)); // Color Names -> Gold
            } else if (matcher.group(9) != null) {
                root.append(Text.literal(matcher.group()).formatted(Formatting.AQUA)); // Namespaces -> Aqua
            }
            lastEnd = matcher.end();
        }

        // Append remaining text as AQUA
        if (lastEnd < nbt.length()) {
            root.append(Text.literal(nbt.substring(lastEnd)).formatted(Formatting.AQUA));
        }

        return root;
    }
}
