package com.trusted.systemnbteditor.util;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class TextUtil {
    
    public static Text substring(Text text, int start, int end) {
        MutableText output = Text.literal("");
        text.visit((style, str) -> {
            int currentI = 0; // This visitor logic needs careful adaptation if used
            // Simplified version for now or follow the original logic
            return Optional.empty();
        }, Style.EMPTY);
        // Simplified substring for now
        String s = text.getString();
        if (start < 0) start = 0;
        if (end < 0 || end > s.length()) end = s.length();
        if (start > end) return Text.literal("");
        return Text.literal(s.substring(start, end));
    }

    public static List<Text> splitText(Text text) {
        List<Text> output = new ArrayList<>();
        String s = text.getString();
        for (String line : s.split("\n")) {
            output.add(Text.literal(line));
        }
        return output;
    }

    public static Text joinLines(List<Text> lines) {
        MutableText output = Text.literal("");
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0)
                output.append("\n");
            output.append(lines.get(i));
        }
        return output;
    }

    public static boolean isTextFormatted(Text text, Style base) {
        if (StyleUtil.hasFormatting(text.getStyle(), base))
            return true;
        
        for (Text sibling : text.getSiblings()) {
            if (isTextFormatted(sibling, base))
                return true;
        }
        
        return false;
    }
}
