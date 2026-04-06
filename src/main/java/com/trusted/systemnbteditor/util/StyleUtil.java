package com.trusted.systemnbteditor.util;

import net.minecraft.text.Style;
import net.minecraft.util.Formatting;
import java.util.Objects;

public class StyleUtil {
    
    public static final Style RESET_STYLE = Style.EMPTY.withColor(Formatting.WHITE)
            .withBold(false).withItalic(false).withUnderline(false).withStrikethrough(false).withObfuscated(false);
    
    public static boolean identical(Style a, Style b) {
        return Objects.equals(a.getColor(), b.getColor()) &&
                a.isBold() == b.isBold() &&
                a.isItalic() == b.isItalic() &&
                a.isUnderlined() == b.isUnderlined() &&
                a.isStrikethrough() == b.isStrikethrough() &&
                a.isObfuscated() == b.isObfuscated() &&
                Objects.equals(a.getClickEvent(), b.getClickEvent()) &&
                Objects.equals(a.getHoverEvent(), b.getHoverEvent()) &&
                Objects.equals(a.getInsertion(), b.getInsertion()) &&
                Objects.equals(a.getFont(), b.getFont());
    }
    
    public static boolean hasFormatting(Style style, Formatting formatting) {
        return identical(style, style.withFormatting(formatting));
    }
    
    public static boolean hasFormatting(Style style, Style base) {
        return !identical(style.withParent(base), base);
    }
}
