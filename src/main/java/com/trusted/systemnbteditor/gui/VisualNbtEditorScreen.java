package com.trusted.systemnbteditor.gui;

import com.trusted.systemnbteditor.util.NbtFormatter;
import com.trusted.systemnbteditor.util.StyleUtil;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.component.ComponentMap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryOps;
import com.trusted.systemnbteditor.gui.widget.SuggestingTextFieldWidget;
import net.minecraft.client.gui.widget.EditBoxWidget;
import com.trusted.systemnbteditor.util.ComponentIconUtil;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.text.Text;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.component.ComponentType;

import java.util.*;
import java.util.function.Consumer;

import java.lang.reflect.Method;

public class VisualNbtEditorScreen extends Screen {
    private final Screen parent;
    private ItemStack stack;
    private final int slotId;
    private final Consumer<ItemStack> saveCallback;

    private SuggestingTextFieldWidget itemIdField;
    private TextFieldWidget countField;

    private final List<String> visibleComponentKeys = new ArrayList<>();
    private final Map<String, ClickableWidget> componentEditors = new LinkedHashMap<>();

    // Scrolling
    private int   scrollTarget  = 0;
    private int   scrollOffset  = 0;
    private float scrollSmooth  = 0;
    private static final float LERP = 0.12f;
    private float sliderVal     = 0;
    private boolean draggingSlider = false;

    // Cursor blink
    private int cursorBlink = 0;
    private String draggingEditorKey = null;

    private static final int EDITOR_HEIGHT  = 60; // Increased for wrapping
    private static final int ITEM_HEIGHT    = 22;
    private static final int LIST_Y         = 60; 
    private static final int EDITOR_X       = 20;
    private static final int LINE_H         = 10;

    private static final Identifier ADD_ICON = Identifier.of("system_nbt_editor", "textures/gui/add.png");
    private static final Identifier COPY_ICON = Identifier.of("system_nbt_editor", "gui/copy_cmd.png");
    private static String componentClipboard = null;

    public VisualNbtEditorScreen(Screen parent, ItemStack stack, int slotId, Consumer<ItemStack> saveCallback) {
        super(Text.of("Visual NBT Editor"));
        this.parent = parent;
        this.stack  = stack;
        this.slotId = slotId;
        this.saveCallback = saveCallback;
        
        try {
            // Compare current components to item defaults to hide unmodified ones.
            // Also ignore custom_data if it only contains ViaVersion protocols.
            for (ComponentType<?> type : Registries.DATA_COMPONENT_TYPE) {
                if (type != null && stack.getComponents().contains(type)) {
                    Object currentVal = stack.get(type);
                    Object defaultVal = stack.getItem().getComponents().get(type);
                    if (shouldShowComponentByDefault(type, currentVal, defaultVal)) {
                        Identifier id = Registries.DATA_COMPONENT_TYPE.getId(type);
                        if (id != null) {
                            visibleComponentKeys.add(id.toString());
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    @Override
    protected void init() {
        super.init();
        int topY = 10;

        // Item ID field
        this.itemIdField = new SuggestingTextFieldWidget(this, 25, topY, 150, 20, Text.of("Item ID"));
        this.itemIdField.setText(Registries.ITEM.getId(stack.getItem()).toString());
        this.itemIdField.setTextPredicate(VisualNbtEditorScreen::isValidItemIdInput);
        this.itemIdField.setDrawsBackground(true);
        this.addDrawableChild(itemIdField);

        // Count field
        this.countField = new TextFieldWidget(this.textRenderer, 210, topY, 40, 20, Text.of("Count"));
        this.countField.setText(String.valueOf(stack.getCount()));
        this.countField.setDrawsBackground(true);
        this.addDrawableChild(countField);

        // Add Component Button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Add Component").formatted(Formatting.AQUA), button -> {
            this.client.setScreen(new ComponentPickerScreen(this, component -> {
                addComponent(component.key());
            }));
        }).dimensions(270, topY, 120, 20).build());

        // Paste Button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Paste").formatted(Formatting.YELLOW), button -> onPasteButtonClick())
                .dimensions(400, topY, 60, 20).build());

        initComponentEditors();

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Save").formatted(Formatting.GREEN), button -> save())
                .dimensions(10, this.height - 30, 80, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.of("Back"), button -> close())
                .dimensions(100, this.height - 30, 80, 20).build());
    }

    private void addComponent(String key) {
        if (!visibleComponentKeys.contains(key)) {
            visibleComponentKeys.add(0, key); // Add to top
            addEditor(key);
            updateEditorsPosition();
        }
    }

    private void initComponentEditors() {
        componentEditors.clear();
        for (String key : visibleComponentKeys) {
            addEditor(key);
        }
    }

    private void updateEditorsPosition() {
        // Clamp scroll if needed
        int listHeight = this.height - 100;
        int totalH = visibleComponentKeys.size() * (EDITOR_HEIGHT + 35);
        int maxScroll = Math.max(0, totalH - listHeight);
        if (scrollTarget > maxScroll) {
            scrollTarget = maxScroll;
        }
    }

    private void addEditor(String key) {
        ComponentType type = getComponentType(key);
        if (type == null) return;
        Object val = stack.get(type);
        String initialText = (val != null) ? getInitialText(key, type, val) : "";
        
        // Use EditBoxWidget for multi-line support / wrapping
        // Minecraft 1.21.x EditBoxWidget.Builder
        EditBoxWidget editor = new EditBoxWidget.Builder()
                .x(0)
                .y(0)
                .build(this.textRenderer, this.width - EDITOR_X - 60, EDITOR_HEIGHT, Text.of(key));
        
        editor.setMaxLength(Integer.MAX_VALUE);
        editor.setText(initialText);
        componentEditors.put(key, editor);
        this.addSelectableChild(editor);
    }

    private String getInitialText(String key, ComponentType type, Object val) {
        try {
            if (this.client != null && this.client.world != null) {
                var lookup = this.client.world.getRegistryManager();
                var ops = RegistryOps.of(net.minecraft.nbt.NbtOps.INSTANCE, lookup);
                var nbt = type.getCodecOrThrow().encodeStart(ops, val).getOrThrow();
                if (nbt instanceof net.minecraft.nbt.NbtString ns) return ns.asString().orElse("");
                return nbt.toString();
            }
        } catch (Exception e) {}
        return val.toString();
    }


    // ─── Tick ────────────────────────────────────────────────────────────────
 
    @Override
    public void tick() {
        super.tick();
        cursorBlink++;
    }

    // ─── Background & render ─────────────────────────────────────────────────

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
        context.fill(0, 0, this.width, this.height, 0x80000000);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Smooth scrolling
        if (Math.abs(scrollTarget - scrollSmooth) > 0.1f) {
            scrollSmooth += (scrollTarget - scrollSmooth) * LERP;
        } else {
            scrollSmooth = scrollTarget;
        }
        scrollOffset = (int)scrollSmooth;

        super.render(context, mouseX, mouseY, delta);
 
        // Title for visual confirmation
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 5, 0xFFAAAAAA);

        context.drawTextWithShadow(this.textRenderer, Text.literal("ID"), 10, 15, 0xFFAAAAAA);
        context.drawTextWithShadow(this.textRenderer, Text.literal("C"), 190, 15, 0xFFAAAAAA);

        int listY = LIST_Y;
        int listHeight = this.height - 100;
        renderEditors(context, listY, listHeight, mouseX, mouseY, delta);
        renderSliders(context, listY, listHeight);
    }

    // ─── Left panel ──────────────────────────────────────────────────────────

    private void renderEditors(DrawContext context, int listY, int listHeight,
                              int mouseX, int mouseY, float delta) {
        int currentY = listY - scrollOffset;

        // Scissor for the entire component list to prevent overlap with buttons or ID fields
        context.enableScissor(0, listY, this.width, listY + listHeight);
        
        for (String key : visibleComponentKeys) {
            // Component label
            int labelY = currentY + 10;
            if (labelY + 10 > listY && labelY < listY + listHeight) {
                // Draw "Component: " prefix in white, then "minecraft:suffix" with minecraft: in light green
                String prefix = "Component: ";
                int prefW = this.textRenderer.getWidth(prefix);
                context.drawTextWithShadow(this.textRenderer, Text.literal(prefix), EDITOR_X, labelY, 0xFFAAAAAA);
                if (key.startsWith("minecraft:")) {
                    String suffix = key.substring("minecraft:".length());
                    context.drawTextWithShadow(this.textRenderer, Text.literal("minecraft:").formatted(Formatting.GREEN), EDITOR_X + prefW, labelY, 0xFF55FF55);
                    int nsW = this.textRenderer.getWidth("minecraft:");
                    context.drawTextWithShadow(this.textRenderer, Text.literal(suffix), EDITOR_X + prefW + nsW, labelY, 0xFFFFFFFF);
                } else {
                    context.drawTextWithShadow(this.textRenderer, Text.literal(key), EDITOR_X + prefW, labelY, 0xFF55FF55);
                }
                
                // Draw Icon to the right of the name
                int textWidth = this.textRenderer.getWidth("Component: " + key);
                ItemStack icon = ComponentIconUtil.getIcon(key);
                int iconX = EDITOR_X + textWidth + 8;
                context.drawItem(icon, iconX, labelY - 5);
            }

            int editorY = currentY + 25;
            ClickableWidget editor = componentEditors.get(key);
            if (editor != null) {
                int ex = EDITOR_X;
                int ew = this.width - EDITOR_X - 60;
                int eh = EDITOR_HEIGHT;
                editor.setX(ex);
                editor.setY(editorY);
                editor.setWidth(ew);
                editor.visible = (editorY + eh > listY && editorY < listY + listHeight);

                if (editor.visible) {
                    boolean wasRendering = com.trusted.systemnbteditor.util.MixinState.isRenderingNbtEditor;
                    com.trusted.systemnbteditor.util.MixinState.isRenderingNbtEditor = true;
                    try {
                        editor.render(context, mouseX, mouseY, delta);
                        if (key.equals("minecraft:damage_type")) {
                            String txt = "";
                            if (editor instanceof TextFieldWidget tf) txt = tf.getText();
                            else if (editor instanceof EditBoxWidget eb) txt = eb.getText();
                            if (txt.isEmpty() && !editor.isFocused()) {
                                // 0xFFAAAAAA is Formatting.GRAY, standard for Minecraft placeholders.
                                // Temporarily disable NBT syntax highlighting via MixinState.
                                com.trusted.systemnbteditor.util.MixinState.isRenderingNbtEditor = false;
                                context.drawText(this.textRenderer, "ex. minecraft:generic_kill", ex + 4, editorY + 6, 0xFFAAAAAA, false);
                                com.trusted.systemnbteditor.util.MixinState.isRenderingNbtEditor = true;
                            }
                        }
                    } catch (Exception e) {
                        // If one editor crashes, don't crash the whole screen
                        context.drawText(this.textRenderer, "Render Err (" + key + ")", ex, editorY, 0xFFFF0000, false);
                    } finally {
                        com.trusted.systemnbteditor.util.MixinState.isRenderingNbtEditor = wasRendering;
                    }

                    // Render small scrollbar for the EditBoxWidget
                    renderEditorScrollbar(context, (EditBoxWidget)editor, key);
                    
                    // Render X Buttons (outside the editor widget)
                    renderXButtons(context, ex + ew + 5, editorY, mouseX, mouseY, key);
                }
            }
            currentY += EDITOR_HEIGHT + 35;
        }
        context.disableScissor();
    }

    private void renderEditorScrollbar(DrawContext context, EditBoxWidget editor, String key) {
        try {
            // EditBoxWidget has a protected EditBox field 'editBox' which has getScrollAmount() and getMaxScroll()
            java.lang.reflect.Field editBoxField = EditBoxWidget.class.getDeclaredField("editBox");
            editBoxField.setAccessible(true);
            Object editBox = editBoxField.get(editor);
            
            java.lang.reflect.Method getScrollAmount = editBox.getClass().getMethod("getScrollAmount");
            java.lang.reflect.Method getMaxScroll = editBox.getClass().getMethod("getMaxScroll");
            
            double scrollAmount = (double) getScrollAmount.invoke(editBox);
            double maxScroll = (double) getMaxScroll.invoke(editBox);
            
            if (maxScroll > 0) {
                int padding = 2;
                int sbX = editor.getX() + editor.getWidth() - 6;
                int sbY = editor.getY() + padding;
                int sbW = 4;
                int sbH = editor.getHeight() - (padding * 2);
                
                // Background
                context.fill(sbX, sbY, sbX + sbW, sbY + sbH, 0x88000000);
                
                // Slider
                int sliderH = Math.max(4, (int)((sbH * sbH) / (maxScroll + sbH)));
                int sliderY = sbY + (int)((scrollAmount / maxScroll) * (sbH - sliderH));
                
                int color = key.equals(draggingEditorKey) ? 0xFFFFFFFF : 0xFFAAAAAA;
                context.fill(sbX, sliderY, sbX + sbW, sliderY + sliderH, color);
            }
        } catch (Exception ignored) {}
    }

    private void renderXButtons(DrawContext context, int x, int y, int mouseX, int mouseY, String key) {
        // Row 1: Hide X and Expand E
        boolean hoverHide = mouseX >= x && mouseX < x + 20 && mouseY >= y && mouseY < y + 20;
        draw3DButton(context, x, y, 20, 20, 0xFF330000, null, "X", 0xFFFF0000, hoverHide);
        if (hoverHide) context.drawTooltip(this.textRenderer, Text.of("Hide/Remove Component"), mouseX, mouseY);

        boolean hoverExpand = mouseX >= x + 20 && mouseX < x + 40 && mouseY >= y && mouseY < y + 20;
        draw3DButton(context, x + 20, y, 20, 20, 0xFF004400, null, "E", 0xFF55FF55, hoverExpand);
        if (hoverExpand) context.drawTooltip(this.textRenderer, Text.of("Expand"), mouseX, mouseY);

        // Row 2: Erase X and Copy C
        boolean hoverErase = mouseX >= x && mouseX < x + 20 && mouseY >= y + 20 && mouseY < y + 40;
        draw3DButton(context, x, y + 20, 20, 20, 0xFF220000, null, "X", 0xFF880000, hoverErase);
        if (hoverErase) context.drawTooltip(this.textRenderer, Text.of("Erase Component"), mouseX, mouseY);

        boolean hoverCopy = mouseX >= x + 20 && mouseX < x + 40 && mouseY >= y + 20 && mouseY < y + 40;
        draw3DButton(context, x + 20, y + 20, 20, 20, 0xFF663300, COPY_ICON, null, 0, hoverCopy);
        if (hoverCopy) context.drawTooltip(this.textRenderer, Text.of("Copy Component"), mouseX, mouseY);
    }

    private void draw3DButton(DrawContext context, int x, int y, int w, int h, int color, Identifier icon, String text, int textColor, boolean hover) {
        // High-Contrast 3D Beveled Depth
        int bgColor = hover ? (color & 0x00FFFFFF) | 0xFF000000 : color | 0xFF000000;
        // Make it slightly lighter if hovered? Simplified: just use color as base
        if (hover) {
            // brighten?
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = color & 0xFF;
            r = Math.min(255, r + 40);
            g = Math.min(255, g + 40);
            b = Math.min(255, b + 40);
            bgColor = 0xFF000000 | (r << 16) | (g << 8) | b;
        }

        context.fill(x, y, x + w, y + h, bgColor);
        
        // HIGHLIGHTS (white/trans white)
        context.fill(x, y, x + w - 1, y + 1, 0xFFFFFFFF); 
        context.fill(x, y, x + 1, y + h - 1, 0xFFFFFFFF); 
        context.fill(x + 1, y + 1, x + w - 2, y + 2, 0x80FFFFFF); 
        context.fill(x + 1, y + 1, x + 2, y + h - 2, 0x80FFFFFF); 
        
        // SHADOWS (black/trans black)
        context.fill(x + 1, y + h - 1, x + w, y + h, 0xFF000000); 
        context.fill(x + w - 1, y + 1, x + w, y + h, 0xFF000000); 
        context.fill(x + 2, y + h - 2, x + w - 1, y + h - 1, 0x80000000); 
        context.fill(x + w - 2, y + 2, x + w - 1, y + h - 1, 0x80000000); 

        if (icon != null) {
            if (!drawIcon(context, icon, x + 2, y + 2)) {
                // Fallback symbol if texture fails
                context.drawText(this.textRenderer, "C", x + 7, y + 6, 0xFFFFFFFF, false);
            }
        } else if (text != null) {
            context.drawText(this.textRenderer, text, x + 7, y + 6, textColor, false);
        }
    }

    private boolean drawIcon(DrawContext context, Identifier icon, int x, int y) {
        String path = icon.getPath();
        
        // Variations to try for the identifier
        Identifier[] variants = {
            icon, 
            Identifier.of(icon.getNamespace(), path.replace("textures/", "")), 
            path.endsWith(".png") ? Identifier.of(icon.getNamespace(), path.substring(0, path.length() - 4)) : icon,
            Identifier.of(icon.getNamespace(), "gui/" + icon.getPath().substring(icon.getPath().lastIndexOf('/') + 1))
        };

        for (Identifier variant : variants) {
            if (tryDrawTexture(context, variant, x, y, 16, 16, 16, 16)) return true;
        }

        // Sprite system fallback
        String spriteName = path.substring(path.lastIndexOf('/') + 1).replace(".png", "");
        Identifier spriteId = Identifier.of(icon.getNamespace(), "gui/" + spriteName);
        try {
            for (java.lang.reflect.Method m : context.getClass().getMethods()) {
               if (m.getName().equals("drawGuiTexture") || m.getName().equals("method_52718")) {
                   Class<?>[] params = m.getParameterTypes();
                   if (params.length >= 5 && params[0] == Identifier.class) {
                       m.invoke(context, spriteId, x, y, 16, 16);
                       return true;
                   }
               }
            }
        } catch (Exception e) {}
        
        return false;
    }

    // Simplified rendering

    // ─── Sliders ─────────────────────────────────────────────────────────────

    private void renderSliders(DrawContext context, int listY, int listHeight) {
        int maxScroll = Math.max(0, visibleComponentKeys.size() * (EDITOR_HEIGHT + 35) - listHeight);
        if (maxScroll > 0) {
            sliderVal = scrollSmooth / maxScroll;
        } else {
            sliderVal = 0;
        }
        context.fill(this.width - 10, listY, this.width - 8, listY + listHeight, 0xFF222222);
        int rt = listY + (int)(sliderVal * Math.max(0, listHeight - 15));
        context.fill(this.width - 10, rt, this.width - 8, rt + 15, 0xFFCCCCCC);
    }

    // ─── Mouse input ──────────────────────────────────────────────────────────
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int listHeight = this.height - 100;
        int totalH = visibleComponentKeys.size() * (EDITOR_HEIGHT + 35);
        int maxScroll = Math.max(0, totalH - listHeight);
        
        if (totalH > listHeight) {
            scrollTarget -= (int)(verticalAmount * 6 * com.trusted.systemnbteditor.data.ModConfig.getInstance().scrollSpeed);
            if (scrollTarget < 0) scrollTarget = 0;
            if (scrollTarget > maxScroll) scrollTarget = maxScroll;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
        
        if (button == 0) {
            int listY = LIST_Y;
            int listHeight = this.height - 100;
            
            // Check scrollbar
            if (mouseX >= this.width - 15 && mouseX <= this.width && mouseY >= listY && mouseY <= listY + listHeight) {
                draggingSlider = true;
                updateSliderPos(mouseY);
                return true;
            }
 
            // Check editors for scrollbar interaction or focus
            for (Map.Entry<String, ClickableWidget> entry : componentEditors.entrySet()) {
                ClickableWidget editor = entry.getValue();
                String key = entry.getKey();
                if (editor.visible) {
                    // Check if clicking the scrollbar area
                    int padding = 2;
                    int sbX = editor.getX() + editor.getWidth() - 10; // Slightly larger for easier click
                    int sbY = editor.getY() + padding;
                    int sbW = 10;
                    int sbH = editor.getHeight() - (padding * 2);
                    
                    if (mouseX >= sbX && mouseX <= editor.getX() + editor.getWidth() && mouseY >= sbY && mouseY <= sbY + sbH) {
                        draggingEditorKey = key;
                        updateEditorScrollPos(key, mouseY);
                        return true;
                    }

                    if (editor.isMouseOver(mouseX, mouseY)) {
                        // Set focus on this editor and clear others
                        this.setFocused(null);
                        editor.setFocused(true);
                        this.setFocused(editor);
                        editor.mouseClicked(click, bl);
                        return true;
                    } else {
                        editor.setFocused(false);
                    }
                }
            }
            // Check buttons
            int currentY = listY - scrollOffset;
            for (String key : visibleComponentKeys) {
                int editorY = currentY + 25;
                int x = EDITOR_X + (this.width - EDITOR_X - 60) + 5;
                
                // Row 1
                if (mouseY >= editorY && mouseY < editorY + 20) {
                    if (mouseX >= x && mouseX < x + 20) {
                        onHideButtonClick(key);
                        return true;
                    }
                    if (mouseX >= x + 20 && mouseX < x + 40) {
                        onExpandButtonClick(key);
                        return true;
                    }
                }
                // Row 2
                if (mouseY >= editorY + 20 && mouseY < editorY + 40) {
                    if (mouseX >= x && mouseX < x + 20) {
                        onEraseButtonClick(key);
                        return true;
                    }
                    if (mouseX >= x + 20 && mouseX < x + 40) {
                        onCopyButtonClick(key);
                        return true;
                    }
                }
                currentY += EDITOR_HEIGHT + 35;
            }
        }
        return super.mouseClicked(click, bl);
    }

    private void onExpandButtonClick(String key) {
        ClickableWidget widget = componentEditors.get(key);
        String val = "";
        if (widget instanceof TextFieldWidget tf) val = tf.getText();
        else if (widget instanceof EditBoxWidget eb) val = eb.getText();

        if (this.client != null) {
            this.client.setScreen(new ExpandedComponentEditorScreen(this, key, val, newVal -> {
                if (widget instanceof TextFieldWidget tf) tf.setText(newVal);
                else if (widget instanceof EditBoxWidget eb) eb.setText(newVal);
                
                // Also update the stack immediately
                ComponentType type = getComponentType(key);
                if (type != null) {
                    Object parsed = parseValue(key, newVal);
                    if (parsed != null) stack.set(type, parsed);
                }
            }));
        }
    }

    private void onHideButtonClick(String key) {
        visibleComponentKeys.remove(key);
        componentEditors.remove(key);
        // If it was an override, we might want to keep it in NBT but stop editing it?
        // User: "the red X will hide the component"
        // Also: "for items such as the netherite sword... the red X will hide the component"
        // If it's a default component, hiding it means we stop overriding it or just stop showing the editor?
        // Usually, if you hide an editor, you probably want to revert to default value.
        stack.remove(getComponentType(key));
        updateEditorsPosition();
    }

    private void onEraseButtonClick(String key) {
        visibleComponentKeys.remove(key);
        componentEditors.remove(key);
        ComponentType type = getComponentType(key);
        // User: "the dark red X will set that component to {} and remove it from the list there."
        // We set it to empty compound.
        stack.set(type, parseValue(key, "{}"));
        updateEditorsPosition();
    }

    private void onCopyButtonClick(String key) {
        ComponentType type = getComponentType(key);
        if (type == null) return;
        Object val = stack.get(type);
        if (val == null) return;
        
        try {
            if (this.client != null && this.client.world != null) {
                var lookup = this.client.world.getRegistryManager();
                var ops    = RegistryOps.of(net.minecraft.nbt.NbtOps.INSTANCE, lookup);
                var nbt    = type.getCodecOrThrow().encodeStart(ops, val).getOrThrow();
                
                String snbt = nbt.toString();
                componentClipboard = key + "|" + snbt;
                if (this.client.keyboard != null) {
                    this.client.keyboard.setClipboard(componentClipboard);
                }
                if (this.client.player != null) {
                    this.client.player.sendMessage(Text.literal("Copied component: " + key).formatted(Formatting.GREEN), true);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void onPasteButtonClick() {
        if (componentClipboard == null) {
            // Check system clipboard for our format
            if (this.client != null && this.client.keyboard != null) {
                String clipboard = this.client.keyboard.getClipboard();
                if (clipboard != null && clipboard.contains("|")) {
                    componentClipboard = clipboard;
                }
            }
        }

        if (componentClipboard != null && componentClipboard.contains("|")) {
            String[] parts = componentClipboard.split("\\|", 2);
            String key = parts[0];
            String snbt = parts[1];

            ComponentType type = getComponentType(key);
            if (type != null) {
                Object parsed = parseValue(key, snbt);
                if (parsed != null) {
                    stack.set(type, parsed);
                    if (!visibleComponentKeys.contains(key)) {
                        visibleComponentKeys.add(0, key);
                        addEditor(key);
                    } else {
                        ClickableWidget editor = componentEditors.get(key);
                        if (editor instanceof TextFieldWidget tf) tf.setText(snbt);
                        else if (editor instanceof EditBoxWidget eb) eb.setText(snbt);
                    }
                    updateEditorsPosition();
                    if (this.client != null && this.client.player != null) {
                         this.client.player.sendMessage(Text.literal("Pasted component: " + key).formatted(Formatting.GREEN), true);
                    }
                    return;
                }
            }
        }
        
        if (this.client != null && this.client.player != null) {
            this.client.player.sendMessage(Text.literal("Nothing to paste!").formatted(Formatting.RED), true);
        }
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (click.button() == 0) {
            draggingSlider = false;
            draggingEditorKey = null;
        }
        return super.mouseReleased(click);
    }
 
    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        if (click.button() == 0) {
            if (draggingSlider) {
                updateSliderPos(click.y());
                return true;
            }
            if (draggingEditorKey != null) {
                updateEditorScrollPos(draggingEditorKey, click.y());
                return true;
            }
        }
        return super.mouseDragged(click, dx, dy);
    }

    private void updateEditorScrollPos(String key, double mouseY) {
        ClickableWidget widget = componentEditors.get(key);
        if (!(widget instanceof EditBoxWidget editor)) return;
        
        try {
            java.lang.reflect.Field editBoxField = EditBoxWidget.class.getDeclaredField("editBox");
            editBoxField.setAccessible(true);
            Object editBox = editBoxField.get(editor);
            
            java.lang.reflect.Method getMaxScroll = editBox.getClass().getMethod("getMaxScroll");
            java.lang.reflect.Method setScrollAmount = editBox.getClass().getMethod("setScrollAmount", double.class);
            
            double maxScroll = (double) getMaxScroll.invoke(editBox);
            if (maxScroll <= 0) return;
            
            int padding = 2;
            int sbY = editor.getY() + padding;
            int sbH = editor.getHeight() - (padding * 2);
            
            float val = Math.min(1f, Math.max(0f, (float)(mouseY - sbY) / sbH));
            setScrollAmount.invoke(editBox, (double)val * maxScroll);
        } catch (Exception ignored) {}
    }
    private void updateSliderPos(double mouseY) {
        int listHeight = this.height - 100;
        float val = Math.min(1f, Math.max(0f, (float)(mouseY - LIST_Y) / listHeight));
        float target = val * Math.max(0, visibleComponentKeys.size() * (EDITOR_HEIGHT + 35) - listHeight);
        scrollTarget = (int)target;
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────
 
    public void refresh(ItemStack newStack) {
        this.stack = newStack;
        visibleComponentKeys.clear();
        // Use SAME logic as constructor for "hidden default" support
        for (ComponentType<?> type : Registries.DATA_COMPONENT_TYPE) {
            if (type != null && stack.getComponents().contains(type)) {
                Object currentVal = stack.get(type);
                Object defaultVal = stack.getItem().getComponents().get(type);
                if (shouldShowComponentByDefault(type, currentVal, defaultVal)) {
                    Identifier id = Registries.DATA_COMPONENT_TYPE.getId(type);
                    if (id != null) {
                        visibleComponentKeys.add(id.toString());
                    }
                }
            }
        }
        clearAndInit();
    }

    protected void clearAndInit() {
        this.clearChildren();
        componentEditors.clear();
        init();
    }


    private void save() {
        // 1. Handle Item ID and Count changes
        String idText = itemIdField.getText();
        int countValue = 1;
        try {
            countValue = Integer.parseInt(countField.getText());
        } catch (NumberFormatException ignored) {}

        Identifier newId = Identifier.tryParse(idText);
        Item newItem = Registries.ITEM.get(newId);
        
        // Re-create the stack if ID or Count changed
        if (newItem != stack.getItem() || countValue != stack.getCount()) {
            ItemStack newStack = new ItemStack(newItem, countValue);
            // Copy all components from the OLD stack to the NEW one
            newStack.applyComponentsFrom(stack.getComponents());
            this.stack = newStack;
        }

        // 2. Apply component changes
        for (String key : visibleComponentKeys) {
            ComponentType type = getComponentType(key);
            ClickableWidget editor = componentEditors.get(key);
            if (editor != null) {
                String text = "";
                if (editor instanceof TextFieldWidget tf) text = tf.getText();
                else if (editor instanceof EditBoxWidget eb) text = eb.getText();
                
                Object parsed = parseValue(key, text);
                if (parsed != null) stack.set(type, parsed);
            }
        }

        if (parent instanceof NbtSelectionScreen sel) {
            sel.refresh(stack);
        }

        if (saveCallback != null) saveCallback.accept(stack);
        close();
    }

    private Object parseValue(String key, String text) {
        try {
            if (this.client != null && this.client.world != null) {
                var lookup = this.client.world.getRegistryManager();
                var ops    = RegistryOps.of(net.minecraft.nbt.NbtOps.INSTANCE, lookup);
                
                String trimmed = text.trim();
                net.minecraft.nbt.NbtElement nbt = null;

                // Step 1: Try parsing as raw SNBT (works for numbers, booleans, objects, arrays)
                try {
                    String wrapped = "{\"v\":" + trimmed + "}";
                    net.minecraft.nbt.NbtCompound compound = net.minecraft.command.argument.NbtCompoundArgumentType.nbtCompound().parse(new com.mojang.brigadier.StringReader(wrapped));
                    nbt = compound.get("v");
                } catch (Exception e) {
                    // Step 2: Fallback to quoted string if raw SNBT fails (e.g. spaces or unquoted strings)
                    String quoted = "\"" + trimmed.replace("\"", "\\\"") + "\"";
                    String wrapped = "{\"v\":" + quoted + "}";
                    net.minecraft.nbt.NbtCompound compound = net.minecraft.command.argument.NbtCompoundArgumentType.nbtCompound().parse(new com.mojang.brigadier.StringReader(wrapped));
                    nbt = compound.get("v");
                }

                if (nbt != null) {
                    ComponentType type = getComponentType(key);
                    var result = type.getCodecOrThrow().parse(ops, nbt);
                    if (result.error().isPresent()) {
                         System.err.println("[SystemNbtEditor] Codec Parse Error for " + key + ": " + result.error().get().toString());
                    } else {
                        return result.result().orElse(null);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[SystemNbtEditor] Exception parsing " + key + ": " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private <T> boolean shouldShowComponentByDefault(ComponentType<T> type, Object currentVal, Object defaultVal) {
        if (Objects.equals(currentVal, defaultVal)) return false;
        
        Identifier id = Registries.DATA_COMPONENT_TYPE.getId(type);
        if (id != null && id.toString().equals("minecraft:custom_data")) {
            try {
                if (this.client != null && this.client.world != null) {
                    var lookup = this.client.world.getRegistryManager();
                    var ops = RegistryOps.of(net.minecraft.nbt.NbtOps.INSTANCE, lookup);
                    net.minecraft.nbt.NbtElement nbt = type.getCodecOrThrow().encodeStart(ops, (T) currentVal).getOrThrow();
                    if (nbt instanceof net.minecraft.nbt.NbtCompound compound) {
                        boolean hasNonVV = false;
                        for (String k : compound.getKeys()) {
                            if (!k.startsWith("VV|")) {
                                hasNonVV = true;
                                break;
                            }
                        }
                        return hasNonVV;
                    }
                }
            } catch (Exception ignored) {}
        }
        return true;
    }

    public boolean isComponentPresent(String key) {
        return visibleComponentKeys.contains(key);
    }

    public boolean isComponentHidden(String key) {
        if (visibleComponentKeys.contains(key)) return false;
        ComponentType type = getComponentType(key);
        return stack.getComponents().contains(type);
    }

    private static boolean isValidItemIdInput(String text) {
        return text != null && text.matches("[a-z0-9_:\\-./]*");
    }

    private ComponentType getComponentType(String key) {
        return Registries.DATA_COMPONENT_TYPE.get(Identifier.of(key));
    }

    private Object getRenderLayer(Identifier texture) {
        try {
            Class<?> rlClass = Class.forName("net.minecraft.client.render.RenderLayer");
            try {
                Method m = rlClass.getMethod("getGuiTextured", Identifier.class);
                return m.invoke(null, texture);
            } catch (Exception e) {
                Method m = rlClass.getMethod("method_30704", Identifier.class);
                return m.invoke(null, texture);
            }
        } catch (Exception e) {}
        return null;
    }

    private boolean tryDrawTexture(DrawContext context, Identifier icon, int x, int y, int width, int height, int textureWidth, int textureHeight) {
        try {
            for (Method m : context.getClass().getMethods()) {
                if (m.getName().equals("drawTexture") || m.getName().equals("method_51450")) {
                    Class<?>[] params = m.getParameterTypes();
                    if (params.length >= 7 && params[0] == Identifier.class) {
                        Object xArg = (params[1] == float.class) ? (float)x : x;
                        Object yArg = (params[2] == float.class) ? (float)y : y;
                        Object uArg = (params[3] == float.class) ? 0f : 0;
                        Object vArg = (params[4] == float.class) ? 0f : 0;
                        if (params.length >= 9) {
                            m.invoke(context, icon, xArg, yArg, uArg, vArg, width, height, textureWidth, textureHeight);
                        } else {
                            m.invoke(context, icon, xArg, yArg, uArg, vArg, width, height);
                        }
                        return true;
                    }
                }
            }
            Object guiLayer = getRenderLayer(icon);
            if (guiLayer != null) {
                for (Method m : context.getClass().getMethods()) {
                    if (m.getName().equals("drawTexture") || m.getName().equals("method_51450")) {
                        Class<?>[] params = m.getParameterTypes();
                        if (params.length >= 8 && !params[0].isPrimitive() && params[1] == Identifier.class) {
                            Object xArg = (params[2] == float.class) ? (float)x : x;
                            Object yArg = (params[3] == float.class) ? (float)y : y;
                            Object uArg = (params[4] == float.class) ? 0f : 0;
                            Object vArg = (params[5] == float.class) ? 0f : 0;
                            if (params.length >= 10) {
                                m.invoke(context, guiLayer, icon, xArg, yArg, uArg, vArg, width, height, textureWidth, textureHeight);
                            } else {
                                m.invoke(context, guiLayer, icon, xArg, yArg, uArg, vArg, width, height);
                            }
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {}
        return false;
    }

    @Override
    public void close() {
        if (this.client != null) this.client.setScreen(parent);
    }
}
