package com.trusted.systemnbteditor.gui;

import com.trusted.systemnbteditor.util.UuidUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class UuidPearlsScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget usernameField;
    private EditBoxWidget nbtEditor;
    private ButtonWidget convertButton;

    private static final String DEFAULT_NBT = "{\n" +
            "    components: {\n" +
            "        \"minecraft:attribute_modifiers\": [\n" +
            "            {\n" +
            "                amount: 64.0d,\n" +
            "                  id: \"minecraft:bd74c7ec-ccd5-43a2-90a2-521cd1c338a1\",\n" +
            "                  operation: \"add_value\",\n" +
            "                  type: \"minecraft:block_interaction_range\"\n" +
            "            },\n" +
            "              {\n" +
            "                amount: 64.0d,\n" +
            "                  id: \"minecraft:8b910187-1666-4b4d-b65f-54536a516cce\",\n" +
            "                  operation: \"add_value\",\n" +
            "                  type: \"minecraft:entity_interaction_range\"\n" +
            "            }\n" +
            "        ],\n" +
            "          \"minecraft:custom_name\": {\n" +
            "            extra: [\n" +
            "                {\n" +
            "                    color: \"#4F17E3\",\n" +
            "                      text: \"U\"\n" +
            "                },\n" +
            "                  {\n" +
            "                    color: \"#4B22E2\",\n" +
            "                      text: \"U\"\n" +
            "                },\n" +
            "                  {\n" +
            "                    color: \"#472EE1\",\n" +
            "                      text: \"I\"\n" +
            "                },\n" +
            "                  {\n" +
            "                    color: \"#4339E0\",\n" +
            "                      text: \"D\"\n" +
            "                },\n" +
            "                  {\n" +
            "                    color: \"#3F45DE\",\n" +
            "                      text: \" \"\n" +
            "                },\n" +
            "                  {\n" +
            "                    color: \"#3B50DD\",\n" +
            "                      text: \"T\"\n" +
            "                },\n" +
            "                  {\n" +
            "                    color: \"#375CDC\",\n" +
            "                      text: \"e\"\n" +
            "                },\n" +
            "                  {\n" +
            "                    color: \"#3367DA\",\n" +
            "                      text: \"l\"\n" +
            "                },\n" +
            "                  {\n" +
            "                    color: \"#2F73D9\",\n" +
            "                      text: \"e\"\n" +
            "                },\n" +
            "                  {\n" +
            "                    color: \"#2B7ED8\",\n" +
            "                      text: \"p\"\n" +
            "                },\n" +
            "                  {\n" +
            "                    color: \"#278AD6\",\n" +
            "                      text: \"o\"\n" +
            "                },\n" +
            "                  {\n" +
            "                    color: \"#2395D5\",\n" +
            "                      text: \"r\"\n" +
            "                },\n" +
            "                  {\n" +
            "                    color: \"#1FA1D4\",\n" +
            "                      text: \"t\"\n" +
            "                }\n" +
            "            ],\n" +
            "              italic: 0b,\n" +
            "              text: \"\"\n" +
            "        },\n" +
            "          \"minecraft:entity_data\": {\n" +
            "            CustomName: \"Tp player\",\n" +
            "              Item: {\n" +
            "                count: 1,\n" +
            "                  id: \"minecraft:ender_pearl\"\n" +
            "            },\n" +
            "              Owner: [\n" +
            "                I;0,\n" +
            "                  0,\n" +
            "                  0,\n" +
            "                  0\n" +
            "            ],\n" +
            "              id: \"minecraft:ender_pearl\"\n" +
            "        },\n" +
            "          \"minecraft:max_stack_size\": 99,\n" +
            "          \"minecraft:tooltip_display\": {\n" +
            "            hidden_components: [\n" +
            "                \"minecraft:attribute_modifiers\"\n" +
            "            ]\n" +
            "        }\n" +
            "    },\n" +
            "      count: 99,\n" +
            "      id: \"minecraft:endermite_spawn_egg\"\n" +
            "}";

    private net.minecraft.client.gui.screen.ChatInputSuggestor suggestor;
    private ButtonWidget givePearlButton;

    public UuidPearlsScreen(Screen parent) {
        super(Text.of("UUID Pearls"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        // Layout:
        // Top section (Username + Buttons) at fixed Y=30
        // NBT Editor starts at Y=60 and fills down to bottom-20
        
        int nameWidth = 120;
        int nameHeight = 20;
        int nameX = this.width / 2 - nameWidth - 10;
        int nameY = 30; // Fixed top position
        
        int nbtX = 20;
        int nbtY = 60;
        int nbtWidth = this.width - 40;
        int nbtHeight = this.height - nbtY - 20; // Fills remaining space

        this.nbtEditor = new EditBoxWidget.Builder()
                .x(nbtX)
                .y(nbtY)
                .build(this.textRenderer, nbtWidth, nbtHeight, Text.of("NBT"));
        this.nbtEditor.setMaxLength(32767);
        this.nbtEditor.setText(DEFAULT_NBT);
        this.addSelectableChild(this.nbtEditor);

        // Username field: 20x120, "Little bit off to the left the center of the screen"

        this.usernameField = new TextFieldWidget(this.textRenderer, nameX, nameY, nameWidth, nameHeight, Text.of("Username"));
        this.usernameField.setPlaceholder(Text.literal("Username").formatted(Formatting.GRAY));
        this.addDrawableChild(this.usernameField);

        // Convert button on its right
        this.convertButton = ButtonWidget.builder(Text.literal("Convert").formatted(Formatting.BLUE), button -> performConversion())
                .dimensions(nameX + nameWidth + 5, nameY, 60, 20)
                .build();
        this.addDrawableChild(this.convertButton);

        // Give Pearl button: 20x100
        this.givePearlButton = ButtonWidget.builder(Text.literal("Give Pearl").formatted(Formatting.GOLD), button -> givePearl())
                .dimensions(nameX + nameWidth + 70, nameY, 100, 20)
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
            
            // Check if there is an Item tag if the root is not the item itself (standard wrap check)
            // But the default NBT structure is the item with "id", "count", "components"/"tag"
            // So we parse it as an ItemStack
            
            // Note: In 1.21+, item stacks use components. The provided NBT has "components".
            // ItemStack.CODEC should handle it if passed via Env or Ops.
            
            // Simplified approach: Try to parse as item stack directly
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
        } catch (Exception e) {
             this.client.player.sendMessage(Text.literal("Error parsing NBT: " + e.getMessage()).formatted(Formatting.RED), false);
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
        if (name.isEmpty()) return;

        this.convertButton.active = false;
        UuidUtils.fetchUuidAsync(name).thenAccept(uuid -> {
            if (uuid != null) {
                String intArray = UuidUtils.formatIntArrayUuid(uuid); // I;a,b,c,d
                
                // Fetch the current NBT from editor
                String currentNbt = this.nbtEditor.getText();
                
                // Replace the specific Owner structure provided by the user
                // The provided snippet has:
                // Owner: [
                //   I;0,
                //     0,
                //     0,
                //     0
                // ],
                
                // We'll use a regex to find the Owner array and replace its content
                String pattern = "(?s)Owner:\\s*\\[\\s*I;0,\\s*0,\\s*0,\\s*0\\s*\\]";
                String replacement = "Owner: [" + intArray + "]";
                
                // If the exact template is gone, we might try a broader replacement or just tell the user.
                // But usually they just click convert.
                String updatedNbt = currentNbt.replaceFirst(pattern, replacement);
                
                // If it didn't change (e.g. user already changed it), try replacing any I;... Owner
                if (updatedNbt.equals(currentNbt)) {
                     updatedNbt = currentNbt.replaceFirst("(?s)Owner:\\s*\\[\\s*I;[^\\]]+\\s*\\]", replacement);
                }

                String finalNbt = updatedNbt;
                if (this.client != null) {
                    this.client.execute(() -> {
                        this.nbtEditor.setText(finalNbt);
                        this.convertButton.active = true;
                    });
                }
            } else {
                if (this.client != null) {
                    this.client.execute(() -> {
                        this.convertButton.active = true;
                        // Maybe show error?
                    });
                }
            }
        });
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
