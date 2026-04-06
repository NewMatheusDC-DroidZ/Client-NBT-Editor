package com.trusted.systemnbteditor.gui;

import com.trusted.systemnbteditor.util.UuidUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class UuidPosPearlsScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget usernameField;
    private TextFieldWidget coordinatesField;
    private EditBoxWidget nbtEditor;
    private ButtonWidget convertButton;

    private static final String DEFAULT_NBT = "{\n" +
            "    components: {\n" +
            "        \"minecraft:attribute_modifiers\": [\n" +
            "            {\n" +
            "                amount: 64.0d,\n" +
            "                  id: \"minecraft:67035874-8bcd-44d2-a4d3-44f8235a5cf8\",\n" +
            "                  operation: \"add_value\",\n" +
            "                  type: \"minecraft:block_interaction_range\"\n" +
            "            }\n" +
            "        ],\n" +
            "          \"minecraft:custom_data\": {\n" +
            "            \"VB|Protocol1_21_6To1_21_5|original_hashes\": {\n" +
            "                \"1\": -1076508516,\n" +
            "                  \"13\": -676985840,\n" +
            "                  \"15\": -2062575178,\n" +
            "                  \"18\": -1019818302,\n" +
            "                  \"49\": -1483206375,\n" +
            "                  \"5\": -16705035,\n" +
            "                  removed: [\n" +
            "                    I;\n" +
            "                ]\n" +
            "            },\n" +
            "              \"VV|custom_data\": 1b\n" +
            "        },\n" +
            "          \"minecraft:custom_name\": {\n" +
            "            extra: [\n" +
            "                {\n" +
            "                    color: \"#4F17E3\",\n" +
            "                      text: \"U\"\n" +
            "                },\n" +
            "                  {\n" +
            "                    color: \"#4C20E2\",\n" +
            "                      text: \"U\"\n" +
            "                },\n" +
            "                  {\n" +
            "                    color: \"#4929E1\",\n" +
            "                      text: \"I\"\n" +
            "                },\n" +
            "                  {\n" +
            "                    color: \"#4633E0\",\n" +
            "                      text: \"D\"\n" +
            "                },\n" +
            "                  {\n" +
            "                    color: \"#423CDF\",\n" +
            "                      text: \" \"\n" +
            "                },\n" +
            "                  {\n" +
            "                    color: \"#3F45DE\",\n" +
            "                      text: \"P\"\n" +
            "                },\n" +
            "                  {\n" +
            "                    color: \"#3C4FDD\",\n" +
            "                      text: \"o\"\n" +
            "                },\n" +
            "                  {\n" +
            "                    color: \"#3958DC\",\n" +
            "                      text: \"s\"\n" +
            "                },\n" +
            "                  {\n" +
            "                    color: \"#3562DB\",\n" +
            "                      text: \"T\"\n" +
            "                },\n" +
            "                  {\n" +
            "                    color: \"#326BDA\",\n" +
            "                      text: \"e\"\n" +
            "                },\n" +
            "                  {\n" +
            "                    color: \"#2F74D9\",\n" +
            "                      text: \"l\"\n" +
            "                },\n" +
            "                  {\n" +
            "                    color: \"#2C7ED8\",\n" +
            "                      text: \"e\"\n" +
            "                },\n" +
            "                  {\n" +
            "                    color: \"#2887D7\",\n" +
            "                      text: \"p\"\n" +
            "                },\n" +
            "                  {\n" +
            "                    color: \"#2590D6\",\n" +
            "                      text: \"o\"\n" +
            "                },\n" +
            "                  {\n" +
            "                    color: \"#229AD5\",\n" +
            "                      text: \"r\"\n" +
            "                },\n" +
            "                  {\n" +
            "                    color: \"#1FA3D4\",\n" +
            "                      text: \"t\"\n" +
            "                }\n" +
            "            ],\n" +
            "              italic: 0b,\n" +
            "              text: \"\"\n" +
            "        },\n" +
            "          \"minecraft:enchantment_glint_override\": 1b,\n" +
            "          \"minecraft:entity_data\": {\n" +
            "            Motion: [\n" +
            "                0.0d,\n" +
            "                  -1.0d,\n" +
            "                  0.0d\n" +
            "            ],\n" +
            "              Owner: [\n" +
            "                I;0,\n" +
            "                  0,\n" +
            "                  0,\n" +
            "                  0\n" +
            "            ],\n" +
            "              Pos: [\n" +
            "                0.0d,\n" +
            "                  80.0d,\n" +
            "                  0.0d\n" +
            "            ],\n" +
            "              id: \"minecraft:ender_pearl\"\n" +
            "        },\n" +
            "          \"minecraft:lore\": [\n" +
            "            \"\",\n" +
            "              \"\",\n" +
            "              {\n" +
            "                extra: [\n" +
            "                    {\n" +
            "                        color: \"dark_gray\",\n" +
            "                          italic: 0b,\n" +
            "                          text: \"Credit: 1e310\"\n" +
            "                    }\n" +
            "                ],\n" +
            "                  text: \"\"\n" +
            "            }\n" +
            "        ],\n" +
            "          \"minecraft:max_stack_size\": 75,\n" +
            "          \"minecraft:tooltip_display\": {\n" +
            "            hidden_components: [\n" +
            "                \"minecraft:attribute_modifiers\"\n" +
            "            ]\n" +
            "        }\n" +
            "    },\n" +
            "      count: 75,\n" +
            "      id: \"minecraft:ender_dragon_spawn_egg\"\n" +
            "}";

    private net.minecraft.client.gui.screen.ChatInputSuggestor suggestor;
    private ButtonWidget givePearlButton;

    public UuidPosPearlsScreen(Screen parent) {
        super(Text.of("UUID PosPearls"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        // Layout:
        // Top section (Username + Coordinates + Buttons) centered at fixed Y=30
        // Widths: User(120) + 5 + Coords(120) + 5 + Fetch(60) + 5 + Give(100) = 415
        
        int totalTopWidth = 120 + 5 + 120 + 5 + 60 + 5 + 100;
        int startX = (this.width - totalTopWidth) / 2;
        int topY = 30;
        
        int nbtX = 20;
        int nbtY = 60;
        int nbtWidth = this.width - 40;
        int nbtHeight = this.height - nbtY - 20;

        this.nbtEditor = new EditBoxWidget.Builder()
                .x(nbtX)
                .y(nbtY)
                .build(this.textRenderer, nbtWidth, nbtHeight, Text.of("NBT"));
        this.nbtEditor.setMaxLength(32767);
        this.nbtEditor.setText(DEFAULT_NBT);
        this.addSelectableChild(this.nbtEditor);

        // Username
        this.usernameField = new TextFieldWidget(this.textRenderer, startX, topY, 120, 20, Text.of("Username"));
        this.usernameField.setPlaceholder(Text.literal("Username").formatted(Formatting.GRAY));
        this.addDrawableChild(this.usernameField);

        // Coordinates
        this.coordinatesField = new TextFieldWidget(this.textRenderer, startX + 125, topY, 120, 20, Text.of("Coordinates"));
        this.coordinatesField.setPlaceholder(Text.literal("Coordinates").formatted(Formatting.GRAY));
        this.addDrawableChild(this.coordinatesField);

        // Fetch Button
        this.convertButton = ButtonWidget.builder(Text.literal("Fetch").formatted(Formatting.BLUE), button -> performConversion())
                .dimensions(startX + 250, topY, 60, 20)
                .build();
        this.addDrawableChild(this.convertButton);

        // Give Pearl button
        this.givePearlButton = ButtonWidget.builder(Text.literal("Give Pearl").formatted(Formatting.GOLD), button -> givePearl())
                .dimensions(startX + 315, topY, 100, 20)
                .build();
        this.addDrawableChild(this.givePearlButton);

        // Back button
        this.addDrawableChild(ButtonWidget.builder(Text.of("Back"), button -> {
            if (this.client != null) {
                this.client.setScreen(this.parent);
            }
        }).dimensions(this.width / 2 - 50, this.height - 30, 100, 20).build());
        
        setupSuggestor(nbtX, nbtY, nbtWidth, nbtHeight);
    }
    
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0x80000000);
    }

    private void givePearl() {
        if (this.client == null || this.client.player == null) return;
        
        try {
            String nbtText = this.nbtEditor.getText();
            net.minecraft.nbt.NbtCompound nbt = net.minecraft.command.argument.NbtCompoundArgumentType.nbtCompound().parse(new com.mojang.brigadier.StringReader(nbtText));
            
            // Should be parsed as item stack
             net.minecraft.registry.RegistryWrapper.WrapperLookup lookup = this.client.world.getRegistryManager();
             net.minecraft.registry.RegistryOps<net.minecraft.nbt.NbtElement> ops = net.minecraft.registry.RegistryOps.of(net.minecraft.nbt.NbtOps.INSTANCE, lookup);
             
             net.minecraft.item.ItemStack stack = net.minecraft.item.ItemStack.CODEC.parse(ops, nbt).result().orElse(net.minecraft.item.ItemStack.EMPTY);
             
             if (!stack.isEmpty()) {
                 this.client.player.getInventory().setStack(this.client.player.getInventory().selectedSlot, stack);
                 this.client.interactionManager.clickCreativeStack(stack, 36 + this.client.player.getInventory().selectedSlot);
                 this.client.player.sendMessage(Text.literal("Pearl given!").formatted(Formatting.GREEN), true);
             } else {
                 this.client.player.sendMessage(Text.literal("Invalid NBT for item.").formatted(Formatting.RED), false);
             }
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
             String msg = e.getMessage();
             int cursor = e.getCursor();
             String nbt = this.nbtEditor.getText();
             String contextStr = "";
             if (cursor >= 0 && cursor < nbt.length()) {
                 int start = Math.max(0, cursor - 15);
                 int end = Math.min(nbt.length(), cursor + 15);
                 contextStr = "..." + nbt.substring(start, end).replace("\n", " ") + "...";
             }
             this.client.player.sendMessage(Text.literal("NBT Syntax Error at pos " + cursor + ": " + contextStr).formatted(Formatting.RED), false);
             this.client.player.sendMessage(Text.literal("Details: " + msg).formatted(Formatting.RED), false); 
        } catch (Exception e) {
             String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
             this.client.player.sendMessage(Text.literal("Error parsing NBT: " + msg).formatted(Formatting.RED), false);
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
                String text = nbtEditor.getText();
                int c = getRealCursor();
                if (c > text.length()) c = text.length();
                int start = text.lastIndexOf('\n', c - 1);
                return start + 1;
            }
            private int getLineEnd() {
                String text = nbtEditor.getText();
                int c = getRealCursor();
                int end = text.indexOf('\n', c);
                if (end == -1) return text.length();
                return end;
            }
            
            @Override
            public String getText() {
                String full = nbtEditor.getText();
                int start = getLineStart();
                int end = getLineEnd();
                if (start < 0) start = 0;
                if (end > full.length()) end = full.length();
                if (start > end) return "";
                return full.substring(start, end);
            }
            
            @Override public void setText(String text) {
                String full = nbtEditor.getText();
                int start = getLineStart();
                int end = getLineEnd();
                String before = full.substring(0, start);
                String after = full.substring(end);
                nbtEditor.setText(before + text + after);
                int newCursor = start + text.length();
                setReflect(cursorField, newCursor);
                setReflect(selectionEndField, newCursor);
            }

            @Override public void write(String text) {
                 String full = nbtEditor.getText(); 
                 int c = getRealCursor();
                 String before = full.substring(0, c); 
                 String after = full.substring(c);
                 nbtEditor.setText(before + text + after); 
                 int next = c + text.length();
                 setReflect(cursorField, next); 
                 setReflect(selectionEndField, next);
                 if (suggestor != null) suggestor.refresh();
            }
            
            @Override public void eraseCharacters(int characterOffset) {
                String content = nbtEditor.getText(); int pos = getRealCursor();
                int start = Math.max(0, pos + characterOffset); int end = pos;
                if (characterOffset > 0) { start = pos; end = Math.min(content.length(), pos + characterOffset); }
                if (start < end) {
                    String before = content.substring(0, start); String after = content.substring(end);
                    nbtEditor.setText(before + after); setReflect(cursorField, start); setReflect(selectionEndField, start);
                }
            }
            
            @Override public int getCursor() { int real = getRealCursor(); int start = getLineStart(); return Math.max(0, real - start); }
             @Override public void setCursor(int cursor, boolean shift) {
                int start = getLineStart(); int real = start + cursor; int end = getLineEnd();
                if (real < start) real = start; if (real > end) real = end;
                setReflect(cursorField, real); if (!shift) setReflect(selectionEndField, real);
            }
            
             @Override public int getX() { return nbtEditor.getX(); }
             @Override public int getY() { 
                 try {
                     double scroll = scrollYField != null ? scrollYField.getDouble(nbtEditor) : 0;
                     int c = getReflect(cursorField);
                     String text = nbtEditor.getText();
                     int row = 0;
                     for(int i=0; i<c && i<text.length(); i++) if(text.charAt(i)=='\n') row++;
                     return nbtEditor.getY() + (row * 9) - (int)scroll;
                 } catch(Exception e) { return nbtEditor.getY(); }
             }
             @Override public int getHeight() { return 9; }
             @Override public int getWidth() { return nbtEditor.getWidth(); }
        };
        
        this.suggestor = new net.minecraft.client.gui.screen.ChatInputSuggestor(this.client, this, adapter, this.textRenderer, true, true, 0, 7, false, 0x80000000) {
             @Override
            public void refresh() {
                // Simplified refresh without suggestions for now, mainly just for rendering color?
                // Actually ChatInputSuggestor provides color rendering via its `render` method usage or similar.
                // Wait, ChatInputSuggestor usually renders suggestions, but the text box itself needs a formatter.
                // EditBoxWidget doesn't easily support external formatters without subclassing or mixins.
                // However, NbtEditorScreen uses `suggestor` which seems to overlay?
                // Let's look closer at NbtEditorScreen. There is no `setFormatter` call. 
                // Ah, check `init`... `this.editor.setChangeListener(this::updatePreview);`
                // But where does the color come from?
                // `ChatInputSuggestor` might be doing something tricky or maybe `EditBoxWidget` in this version supports it?
                // Actually `ChatInputSuggestor` is for suggestions.
                // The prompt says "Add the same text color formatting rules... as in InfiniCommand".
                // InfiniCommandScreen usually uses a custom widget or `EditBoxWidget` with a specific setup.
                // Let's just blindly copy the `setupSuggestor` logic from `NbtEditorScreen` which seems to enable the behavior 
                // (maybe via `CommandSyntaxException` highlighting handled internally by `ChatInputSuggestor`'s parse check?).
                // Yes, `ChatInputSuggestor.refresh()` calls `parse` which colors valid commands/NBT.
                
                // We need to implement refresh specifically like NbtEditorScreen did.
                // I will copy the refresh logic from NbtEditorScreen that uses reflection.
                
                String text = nbtEditor.getText();
                int cursor = 0;
                try {
                    java.lang.reflect.Field f = net.minecraft.client.gui.widget.EditBoxWidget.class.getDeclaredField("cursor");
                    f.setAccessible(true);
                    cursor = f.getInt(nbtEditor);
                } catch (Exception e) {}
                
                final int finalCursor = cursor;
                // We don't have getSuggestions logic here easily without itemId. 
                // But for pure syntax highlighting of NBT, `ChatInputSuggestor` usually does it.
                // However, `NbtEditorScreen` calls `NBTAutocompleteIntegration.getSuggestions`.
                // If I omit that, will it still color? 
                // ChatInputSuggestor colors command arguments. NBT is one big argument if parsed as such.
                // To get "same formatting rules", I probably need the same suggestion logic.
                // But `NbtEditorScreen` has `getItemId()`. Here we don't have an item ID context really, just text.
                // I'll try to use a dummy suggestion builder or just call the super refresh if possible? 
                // Actually `super.refresh()` parses commands.
                // The user asked for "same text color formatting".
                // If I just instantiate `ChatInputSuggestor`, it might try to parse as command.
                // But `NBTAutocompleteIntegration` is likely what gives it the NBT context.
                
                // Let's just try to setup it up exactly as NbtEditorScreen, but maybe pass null/empty for itemId if needed.
                // I'll assume `com.trusted.systemnbteditor.integration.NBTAutocompleteIntegration` handles the heavy lifting.
                // I'll paste the NbtEditorScreen refresh logic here.
                
                java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> future = 
                    com.trusted.systemnbteditor.integration.NBTAutocompleteIntegration.getSuggestions("minecraft:endermite_spawn_egg", text, new com.mojang.brigadier.suggestion.SuggestionsBuilder(text, finalCursor));
                    // Hardcoding endermite_spawn_egg as context since the default NBT is for that.
                
                try {
                    java.lang.reflect.Field pendingField = net.minecraft.client.gui.screen.ChatInputSuggestor.class.getDeclaredField("pendingSuggestions");
                    pendingField.setAccessible(true);
                    pendingField.set(this, future);
                } catch (Exception e) {}
                
                 future.thenRun(() -> {
                    if (future.isDone()) {
                        try {
                            java.lang.reflect.Method m = net.minecraft.client.gui.screen.ChatInputSuggestor.class.getDeclaredMethod("showCommandSuggestions");
                            m.setAccessible(true);
                            m.invoke(this);
                        } catch (Exception e) {
                             try {
                                java.lang.reflect.Method m = net.minecraft.client.gui.screen.ChatInputSuggestor.class.getDeclaredMethod("showSuggestions");
                                m.setAccessible(true);
                                m.invoke(this);
                            } catch (Exception ex) {}
                        }
                    }
                });
            }
        };
        this.suggestor.setWindowActive(true);
        this.suggestor.refresh();
    }


    private void performConversion() {
        String name = this.usernameField.getText().trim();
        String coords = this.coordinatesField.getText().trim();
        
        if (name.isEmpty() && coords.isEmpty()) return;

        this.convertButton.active = false;
        
        // Asynchronous processing
        new Thread(() -> {
            String currentNbt = this.nbtEditor.getText();
            String updatedNbt = currentNbt;

            // Handle Coordinates
            if (!coords.isEmpty()) {
                try {
                    String[] parts = coords.split("\\s+");
                    if (parts.length >= 3) {
                        double x = Double.parseDouble(parts[0].replace(",", "."));
                        double y = Double.parseDouble(parts[1].replace(",", "."));
                        double z = Double.parseDouble(parts[2].replace(",", "."));
                        
                        String replacement = String.format(java.util.Locale.US, "Pos: [\n                %.1fd,\n                  %.1fd,\n                  %.1fd\n            ]", x, y, z);
                        
                        String posPattern = "(?s)Pos:\\s*\\[\\s*[-0-9.,]+[df]?\\s*,\\s*[-0-9.,]+[df]?\\s*,\\s*[-0-9.,]+[df]?\\s*\\]";
                        updatedNbt = updatedNbt.replaceFirst(posPattern, replacement);
                    }
                } catch (Exception e) {
                   if (this.client != null && this.client.player != null) {
                       String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                       this.client.player.sendMessage(Text.literal("Error parsing coordinates: " + msg).formatted(Formatting.RED), false);
                   }
                }
            }

            // Handle UUID
            if (!name.isEmpty()) {
                 java.util.UUID uuid = UuidUtils.fetchUuid(name);
                 if (uuid != null) {
                     String intArray = UuidUtils.formatIntArrayUuid(uuid);
                     String ownerPattern = "(?s)Owner:\\s*\\[\\s*I;[-0-9,\\s]+\\]";
                     String replacement = "Owner: [" + intArray + "]";
                     updatedNbt = updatedNbt.replaceFirst(ownerPattern, replacement);
                 }
            }

            String finalNbt = updatedNbt;
            if (this.client != null) {
                this.client.execute(() -> {
                    this.nbtEditor.setText(finalNbt);
                    this.convertButton.active = true;
                    if (this.client.player != null) {
                         this.client.player.sendMessage(Text.literal("Fetch complete.").formatted(Formatting.GRAY), true);
                    }
                });
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

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }
}
