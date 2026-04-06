package com.trusted.systemnbteditor.gui;

import com.trusted.systemnbteditor.util.UuidUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class UuidUtilityTridentScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget usernameField;
    private EditBoxWidget nbtEditor;
    private ButtonWidget convertButton;
    private ButtonWidget giveTridentButton;
    private net.minecraft.client.gui.screen.ChatInputSuggestor suggestor;

    private static final String UNICODE_BLOCK = "鬍摳뚓ᷳ钪佔グ홹囌ਜ਼鈏壣쾞铽艴⮅傰쀰抢୭췊ￎ䟤싫춫하鉐";
    private static final String LONG_UNICODE = UNICODE_BLOCK + UNICODE_BLOCK + UNICODE_BLOCK + UNICODE_BLOCK + UNICODE_BLOCK;

    private static final String DEFAULT_NBT = "{\n" +
            "    components: {\n" +
            "        \"minecraft:custom_name\": {\n" +
            "            extra: [\n" +
            "                { color: \"#4F17E3\", text: \"U\" },\n" +
            "                { color: \"#4C1FE2\", text: \"U\" },\n" +
            "                { color: \"#4928E1\", text: \"I\" },\n" +
            "                { color: \"#4631E0\", text: \"D\" },\n" +
            "                { color: \"#433ADF\", text: \" \" },\n" +
            "                { color: \"#4043DE\", text: \"U\" },\n" +
            "                { color: \"#3D4BDD\", text: \"t\" },\n" +
            "                { color: \"#3A54DC\", text: \"i\" },\n" +
            "                { color: \"#375DDB\", text: \"l\" },\n" +
            "                { color: \"#3466DA\", text: \"i\" },\n" +
            "                { color: \"#316FD9\", text: \"t\" },\n" +
            "                { color: \"#2E78D8\", text: \"y\" },\n" +
            "                { color: \"#2B80D7\", text: \" \" },\n" +
            "                { color: \"#2889D6\", text: \"T\" },\n" +
            "                { color: \"#2592D5\", text: \"r\" },\n" +
            "                { color: \"#229BD4\", text: \"i\" },\n" +
            "                { color: \"#1FA4D3\", text: \"d\" },\n" +
            "                { color: \"#1CADD2\", text: \"e\" },\n" +
            "                { color: \"#19B6D1\", text: \"n\" },\n" +
            "                { color: \"#16BFD0\", text: \"t\" }\n" +
            "            ],\n" +
            "            italic: 0b, text: \"\"\n" +
            "        },\n" +
            "        \"minecraft:enchantment_glint_override\": 1b,\n" +
            "        \"minecraft:entity_data\": {\n" +
            "            Owner: [ I;0, 0, 0, 0 ],\n" +
            "            id: \"minecraft:trident\",\n" +
            "            item: {\n" +
            "                components: {\n" +
            "                    \"minecraft:custom_name\": {\n" +
            "                        bold: 1b, italic: 1b, obfuscated: 1b, translate: \"" + "%1$s".repeat(256) + "\",\n" +
            "                        underlined: 1b,\n" +
            "                        with: [ \"" + LONG_UNICODE + LONG_UNICODE + LONG_UNICODE + "\" ]\n" +
            "                    },\n" +
            "                    \"minecraft:enchantments\": { \"minecraft:loyalty\": 3 },\n" +
            "                    \"minecraft:item_model\": \"minecraft:air\"\n" +
            "                }, count: 99\n" +
            "            }, pickup: 1\n" +
            "        },\n" +
            "        \"minecraft:item_model\": \"minecraft:trident\",\n" +
            "        \"minecraft:lore\": [\n" +
            "            \"\", \"\",\n" +
            "            { extra: [ { color: \"dark_gray\", italic: 0b, text: \"Credit: 1e310\" } ], text: \"\" }\n" +
            "        ]\n" +
            "    },\n" +
            "    count: 64, id: \"minecraft:wolf_spawn_egg\"\n" +
            "}";

    public UuidUtilityTridentScreen(Screen parent) {
        super(Text.of("UUID UtilityTrident"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        
        // Center-aligned controls: Username(120) + padding(5) + Fetch(100) + padding(5) + Give(100) = 330
        int totalTopWidth = 120 + 5 + 100 + 5 + 100;
        int startX = (this.width - totalTopWidth) / 2;
        int topY = 30;
        
        int nbtX = 20;
        int nbtY = 60;
        int nbtWidth = this.width - 40;
        int nbtHeight = this.height - nbtY - 20;

        this.nbtEditor = new EditBoxWidget.Builder()
                .x(nbtX).y(nbtY)
                .build(this.textRenderer, nbtWidth, nbtHeight, Text.of("NBT"));
        this.nbtEditor.setMaxLength(32767);
        this.nbtEditor.setText(DEFAULT_NBT);
        this.addSelectableChild(this.nbtEditor);

        this.usernameField = new TextFieldWidget(this.textRenderer, startX, topY, 120, 20, Text.of("Username"));
        this.usernameField.setPlaceholder(Text.literal("Username").formatted(Formatting.GRAY));
        this.addDrawableChild(this.usernameField);

        this.convertButton = ButtonWidget.builder(Text.literal("Fetch").formatted(Formatting.BLUE), button -> performConversion())
                .dimensions(startX + 125, topY, 100, 20).build();
        this.addDrawableChild(this.convertButton);

        this.giveTridentButton = ButtonWidget.builder(Text.literal("Give Trident").formatted(Formatting.GOLD), button -> giveTrident())
                .dimensions(startX + 230, topY, 100, 20).build();
        this.addDrawableChild(this.giveTridentButton);

        this.addDrawableChild(ButtonWidget.builder(Text.of("Back"), button -> {
            if (this.client != null) this.client.setScreen(this.parent);
        }).dimensions(this.width / 2 - 50, this.height - 30, 100, 20).build());
        
        setupSuggestor(nbtX, nbtY, nbtWidth, nbtHeight);
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
                    com.trusted.systemnbteditor.integration.NBTAutocompleteIntegration.getSuggestions("minecraft:trident", text, new com.mojang.brigadier.suggestion.SuggestionsBuilder(text, finalCursor));
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

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0x80000000);
    }

    private void giveTrident() {
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
                this.client.player.sendMessage(Text.literal("Trident given!").formatted(Formatting.GREEN), true);
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
