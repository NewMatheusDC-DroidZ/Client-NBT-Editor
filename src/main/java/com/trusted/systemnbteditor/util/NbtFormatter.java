package com.trusted.systemnbteditor.util;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.nbt.*;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.text.NumberFormat;
import java.util.Map;
import java.util.regex.Pattern;

public class NbtFormatter {

    public static record FormatterResult(Text text, boolean isSuccess) {}

    @FunctionalInterface
    public interface Impl {
        Text format(String str) throws CommandSyntaxException;
        default FormatterResult formatSafely(String str) {
            try {
                return new FormatterResult(format(str), true);
            } catch (Exception e) {
                return new FormatterResult(Text.literal(str).formatted(Formatting.RED), false);
            }
        }
    }

    public static Impl FORMATTER = NbtFormatter::formatElement;

    private static final SimpleCommandExceptionType TRAILING_DATA = new SimpleCommandExceptionType(Text.translatable("argument.nbt.trailing"));
    private static final SimpleCommandExceptionType EXPECTED_KEY = new SimpleCommandExceptionType(Text.translatable("argument.nbt.expected.key"));
    private static final SimpleCommandExceptionType EXPECTED_VALUE = new SimpleCommandExceptionType(Text.translatable("argument.nbt.expected.value"));
    private static final DynamicCommandExceptionType ARRAY_INVALID = new DynamicCommandExceptionType(type -> Text.translatable("argument.nbt.array.invalid", type));
    
    private static final Formatting NAME_COLOR = Formatting.AQUA;
    private static final Formatting STRING_COLOR = Formatting.GREEN;
    private static final Formatting NUMBER_COLOR = Formatting.GOLD;
    private static final Formatting TYPE_SUFFIX_COLOR = Formatting.RED;

    public static final Map<String, Number> SPECIAL_NUMS = Map.of(
            "NaNd", Double.NaN,
            "Infinityd", Double.POSITIVE_INFINITY,
            "-Infinityd", Double.NEGATIVE_INFINITY,
            "NaNf", Float.NaN,
            "Infinityf", Float.POSITIVE_INFINITY,
            "-Infinityf", Float.NEGATIVE_INFINITY);

    public static Text formatElement(StringReader reader) throws CommandSyntaxException {
        NbtFormatter formatter = new NbtFormatter(reader);
        MutableText output = formatter.parseElement();
        output.append(formatter.skipWhitespace());
        if (reader.canRead())
            throw TRAILING_DATA.createWithContext(reader);
        return output;
    }
    
    public static Text formatElement(String str) throws CommandSyntaxException {
        return formatElement(new StringReader(str));
    }

    private StringReader reader;

    private NbtFormatter(StringReader reader) {
        this.reader = reader;
    }

    private MutableText skipWhitespace() {
        StringBuilder output = new StringBuilder();
        while (reader.canRead() && Character.isWhitespace(reader.peek()))
            output.append(reader.read());
        return Text.literal(output.toString());
    }

    private String readStringUntil(char terminator) throws CommandSyntaxException {
        StringBuilder result = new StringBuilder();
        while (reader.canRead()) {
            char c = reader.read();
            if (c == '\\') {
                if (!reader.canRead())
                    throw CommandSyntaxException.BUILT_IN_EXCEPTIONS
                            .readerInvalidEscape()
                            .createWithContext(reader, String.valueOf(c));
                result.append('\\').append(reader.read());
                continue;
            }
            if (c == terminator) {
                return result.toString();
            }
            result.append(c);
        }
        throw CommandSyntaxException.BUILT_IN_EXCEPTIONS
                .readerExpectedEndOfQuote()
                .createWithContext(reader);
    }

    private String readString() throws CommandSyntaxException {
        if (!reader.canRead()) {
            return "";
        }
        final char next = reader.peek();
        if (StringReader.isQuotedStringStart(next)) {
            reader.skip();
            return next + readStringUntil(next) + next;
        }
        return reader.readUnquotedString();
    }

    private MutableText readString(Formatting color) throws CommandSyntaxException {
        MutableText output = Text.literal("");
        output.append(this.skipWhitespace());
        if (!this.reader.canRead()) {
            throw EXPECTED_KEY.createWithContext(this.reader);
        }
        String str = this.readString();
        if (str.isEmpty())
            return null;
        output.append(Text.literal(str).formatted(color));
        return output;
    }

    private MutableText readQuotedString() throws CommandSyntaxException {
        MutableText output = Text.literal("");
        if (!reader.canRead())
            return output;

        char quote = reader.peek();
        if (!StringReader.isQuotedStringStart(quote))
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS
                    .readerExpectedStartOfQuote()
                    .createWithContext(reader);

        reader.skip();
        output.append(Text.literal(readStringUntil(quote)));
        return output;
    }

    private Map.Entry<Boolean, MutableText> readComma() {
        MutableText output = Text.literal("");
        output.append(this.skipWhitespace());
        if (this.reader.canRead() && this.reader.peek() == ',') {
            output.append(Text.literal(this.reader.read() + ""));
            output.append(this.skipWhitespace());
            return Map.entry(true, output);
        } else
            return Map.entry(false, output);
    }

    private MutableText readArray() throws CommandSyntaxException {
        MutableText output = Text.literal("");
        while (this.reader.peek() != ']') {
            output.append(this.parseElement());
            Map.Entry<Boolean, MutableText> comma = this.readComma();
            output.append(comma.getValue());
            if (!comma.getKey())
                break;
            if (this.reader.canRead())
                continue;
            throw EXPECTED_VALUE.createWithContext(this.reader);
        }
        output.append(this.expect(']'));
        return output;
    }

    private MutableText parseElement() throws CommandSyntaxException {
        MutableText output = Text.literal("");
        output.append(skipWhitespace());
        if (!this.reader.canRead()) {
            throw EXPECTED_VALUE.createWithContext(this.reader);
        }
        char c = this.reader.peek();
        if (c == '{') {
            output.append(this.parseCompound());
        } else if (c == '[') {
            output.append(this.parseArray());
        } else {
            output.append(this.parseElementPrimitive());
        }
        return output;
    }

    private MutableText parseCompound() throws CommandSyntaxException {
        MutableText output = Text.literal("");
        output.append(this.expect('{'));
        while (this.reader.canRead() && this.reader.peek() != '}') {
            int i = this.reader.getCursor();
            MutableText string = this.readString(NAME_COLOR);
            if (string == null) {
                this.reader.setCursor(i);
                throw EXPECTED_KEY.createWithContext(this.reader);
            }
            output.append(string);
            output.append(this.expect(':'));
            output.append(this.parseElement());
            Map.Entry<Boolean, MutableText> comma = this.readComma();
            output.append(comma.getValue());
            if (!comma.getKey() || reader.peek() == '}')
                break;
            if (this.reader.canRead())
                continue;
            throw EXPECTED_KEY.createWithContext(this.reader);
        }
        output.append(this.expect('}'));
        return output;
    }

    private MutableText parseArray() throws CommandSyntaxException {
        if (this.reader.canRead(3) && !StringReader.isQuotedStringStart(this.reader.peek(1))
                && this.reader.peek(2) == ';') {
            return this.parseElementPrimitiveArray();
        }
        return this.parseList();
    }

    private MutableText parseElementPrimitiveArray() throws CommandSyntaxException {
        MutableText output = Text.literal("");
        output.append(this.expect('['));
        int i = this.reader.getCursor();
        char c = this.reader.read();
        output.append(Text.literal(c + "").formatted(TYPE_SUFFIX_COLOR));
        output.append(Text.literal(this.reader.read() + ""));
        output.append(this.skipWhitespace());
        if (!this.reader.canRead()) {
            throw EXPECTED_VALUE.createWithContext(this.reader);
        }
        if (c == 'B' || c == 'L' || c == 'I') {
            output.append(this.readArray());
            return output;
        }
        this.reader.setCursor(i);
        throw ARRAY_INVALID.createWithContext(this.reader, String.valueOf(c));
    }

    private MutableText parseList() throws CommandSyntaxException {
        MutableText output = Text.literal("");
        output.append(this.expect('['));
        output.append(this.skipWhitespace());
        if (!this.reader.canRead()) {
            throw EXPECTED_VALUE.createWithContext(this.reader);
        }
        while (this.reader.peek() != ']') {
            MutableText nbtElement = this.parseElement();
            output.append(nbtElement);
            Map.Entry<Boolean, MutableText> comma = this.readComma();
            output.append(comma.getValue());
            if (!comma.getKey() || reader.peek() == ']')
                break;
            if (this.reader.canRead())
                continue;
            throw EXPECTED_VALUE.createWithContext(this.reader);
        }
        output.append(this.expect(']'));
        return output;
    }

    private MutableText parseElementPrimitive() throws CommandSyntaxException {
        MutableText output = Text.literal("");
        output.append(skipWhitespace());

        int start = reader.getCursor();
        if (!reader.canRead())
            throw EXPECTED_VALUE.createWithContext(reader);

        char c = reader.peek();
        if (StringReader.isQuotedStringStart(c)) {
            output.append(Text.literal(String.valueOf(c)).formatted(STRING_COLOR));
            output.append(readQuotedString().formatted(STRING_COLOR));
            output.append(Text.literal(String.valueOf(c)).formatted(STRING_COLOR));
            return output;
        }

        String token = reader.readUnquotedString();
        if (token.isEmpty()) {
            reader.setCursor(start);
            throw EXPECTED_VALUE.createWithContext(reader);
        }

        if (reader.canRead() && reader.peek() == '(') {
            return parseFunctionCall(token);
        }

        return parsePrimitive(token);
    }

    private MutableText parseFunctionCall(String name) throws CommandSyntaxException {
        MutableText out = Text.literal(name).formatted(NAME_COLOR);
        out.append(expect('('));

        while (reader.canRead() && reader.peek() != ')') {
            out.append(parseElement());
            Map.Entry<Boolean, MutableText> comma = readComma();
            out.append(comma.getValue());
            if (!comma.getKey())
                break;
        }

        out.append(expect(')'));
        return out;
    }

    private MutableText parsePrimitive(String input) {
        String lower = input.toLowerCase(java.util.Locale.ROOT);

        if ("true".equals(lower) || "false".equals(lower)) {
            return Text.literal(input).formatted(NUMBER_COLOR);
        }
        if (SPECIAL_NUMS.containsKey(input)) {
            return Text.literal(input.substring(0, input.length() - 1))
                    .formatted(NUMBER_COLOR)
                    .append(Text.literal(input.substring(input.length() - 1))
                            .formatted(TYPE_SUFFIX_COLOR));
        }

        try {
            NumberKind kind = classifyNumber(input);

            if(kind != null) {
                int suffixLen = switch (kind) {
                    case FLOAT, DOUBLE, BYTE, SHORT, INT, LONG -> 1;
                    case UBYTE, USHORT, UINT, ULONG -> 2;
                    default -> 0;
                };

                if (suffixLen > 0 && input.length() > suffixLen) {
                    return Text.literal(input.substring(0, input.length() - suffixLen))
                            .formatted(NUMBER_COLOR)
                            .append(Text.literal(input.substring(input.length() - suffixLen))
                                    .formatted(TYPE_SUFFIX_COLOR));
                }
            }
            try {
                NumberFormat.getInstance().parse(input);
                return Text.literal(input).formatted(NUMBER_COLOR);
            } catch (Exception ignored) {
            }
        } catch (Exception ignored) {}
        return Text.literal(input).formatted(STRING_COLOR);

    }
    private NumberKind classifyNumber(String s) {
        String lower = s.toLowerCase(java.util.Locale.ROOT);

        if (lower.startsWith("0b")) return NumberKind.BINARY;
        if (lower.startsWith("0x")) return NumberKind.HEX;

        if (lower.contains("e") || lower.contains(".")) {
            if (lower.endsWith("f")) return NumberKind.FLOAT;
            return NumberKind.DOUBLE;
        }

        if (lower.endsWith("ub")) return NumberKind.UBYTE;
        if (lower.endsWith("us")) return NumberKind.USHORT;
        if (lower.endsWith("ui")) return NumberKind.UINT;
        if (lower.endsWith("ul")) return NumberKind.ULONG;

        if (lower.endsWith("sb")) return NumberKind.BYTE;
        if (lower.endsWith("ss")) return NumberKind.SHORT;
        if (lower.endsWith("si")) return NumberKind.INT;
        if (lower.endsWith("sl")) return NumberKind.LONG;

        if (lower.endsWith("b")) return NumberKind.BYTE;
        if (lower.endsWith("s")) return NumberKind.SHORT;
        if (lower.endsWith("l")) return NumberKind.LONG;

        return null;
    }
    private enum NumberKind {
        INT, FLOAT, DOUBLE,
        BYTE, SHORT, LONG,
        UBYTE, USHORT, UINT, ULONG,
        BINARY, HEX
    }

    private MutableText expect(char c) throws CommandSyntaxException {
        MutableText output = Text.literal("");
        output.append(skipWhitespace());
        this.reader.expect(c);
        output.append(Text.literal(c + ""));
        return output;
    }

}
