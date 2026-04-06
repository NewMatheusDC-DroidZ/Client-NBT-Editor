package com.trusted.systemnbteditor.gui.widget;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class SuggestingTextFieldWidget extends TextFieldWidget {
    
    private final ChatInputSuggestor suggestor;
    private BiFunction<String, Integer, CompletableFuture<Suggestions>> suggestions;
    
    public SuggestingTextFieldWidget(Screen screen, int x, int y, int width, int height, Text text) {
        super(MinecraftClient.getInstance().textRenderer, x, y, width, height, text);
        
        suggestor = new ChatInputSuggestor(MinecraftClient.getInstance(), screen, this, MinecraftClient.getInstance().textRenderer, false, true, 0, 7, false, 0x80000000) {
            @Override
            public void refresh() {
                try {
                    boolean completing = getReflectBool(ChatInputSuggestor.class, this, "completingSuggestions", "field_21612");
                    if (!completing) {
                        SuggestingTextFieldWidget.this.setSuggestion(null);
                        setReflect(ChatInputSuggestor.class, this, "window", "field_21599", null);
                    }
                    
                    ((List<?>) getReflect(ChatInputSuggestor.class, this, "messages", "field_21614")).clear();
                    
                    Object window = getReflect(ChatInputSuggestor.class, this, "window", "field_21599");
                    if (window == null || !completing) {
                        CompletableFuture<Suggestions> pending;
                        if (suggestions == null)
                            pending = new SuggestionsBuilder("", 0).buildFuture();
                        else
                            pending = suggestions.apply(SuggestingTextFieldWidget.this.getText(), SuggestingTextFieldWidget.this.getCursor());
                        
                        setReflect(ChatInputSuggestor.class, this, "pendingSuggestions", "field_21597", pending);
                        
                        pending.thenRun(() -> {
                            if (!pending.isDone())
                                return;
                            try {
                                // Try common names for showCommandSuggestions
                                String[] methodNames = {"showCommandSuggestions", "method_23933", "method_23934"};
                                java.lang.reflect.Method found = null;
                                for (String mName : methodNames) {
                                    try {
                                        found = ChatInputSuggestor.class.getDeclaredMethod(mName);
                                        break;
                                    } catch (NoSuchMethodException ignored) {}
                                }
                                if (found != null) {
                                    found.setAccessible(true);
                                    found.invoke(this);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        try {
            com.mojang.brigadier.CommandDispatcher<net.minecraft.server.command.ServerCommandSource> dummyDispatcher = new com.mojang.brigadier.CommandDispatcher<>();
            ParseResults<net.minecraft.server.command.ServerCommandSource> emptyParse = dummyDispatcher.parse("", null);
            
            java.lang.reflect.Field f = ChatInputSuggestor.class.getDeclaredField("parse");
            f.setAccessible(true);
            f.set(suggestor, emptyParse);
        } catch (Exception e) {
            try {
                com.mojang.brigadier.CommandDispatcher<net.minecraft.server.command.ServerCommandSource> dummyDispatcher = new com.mojang.brigadier.CommandDispatcher<>();
                ParseResults<net.minecraft.server.command.ServerCommandSource> emptyParse = dummyDispatcher.parse("", null);

                java.lang.reflect.Field f = ChatInputSuggestor.class.getDeclaredField("field_21596");
                f.setAccessible(true);
                f.set(suggestor, emptyParse);
            } catch (Exception ex) {}
        }
    }

    private static Object getReflect(Class<?> clazz, Object obj, String... names) throws Exception {
        for (String name : names) {
            try {
                java.lang.reflect.Field f = clazz.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(obj);
            } catch (NoSuchFieldException ignored) {}
        }
        throw new NoSuchFieldException(Arrays.toString(names));
    }
    private static boolean getReflectBool(Class<?> clazz, Object obj, String... names) throws Exception {
        return (boolean) getReflect(clazz, obj, names);
    }
    private static void setReflect(Class<?> clazz, Object obj, String name, String fallback, Object val) throws Exception {
        try {
            java.lang.reflect.Field f = clazz.getDeclaredField(name);
            f.setAccessible(true);
            f.set(obj, val);
        } catch (NoSuchFieldException e) {
            java.lang.reflect.Field f = clazz.getDeclaredField(fallback);
            f.setAccessible(true);
            f.set(obj, val);
        }
    }

    @Override
    public void setChangedListener(Consumer<String> listener) {
        super.setChangedListener(str -> {
            suggestor.refresh();
            if (listener != null)
                listener.accept(str);
        });
    }
    
    public SuggestingTextFieldWidget suggest(BiFunction<String, Integer, CompletableFuture<Suggestions>> suggestions) {
        this.suggestions = suggestions;
        suggestor.refresh();
        return this;
    }
    
    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!this.isVisible()) return;

        boolean wasRendering = com.trusted.systemnbteditor.util.MixinState.isRenderingNbtEditor;
        com.trusted.systemnbteditor.util.MixinState.isRenderingNbtEditor = true;
        try {
            // Simply call super.renderWidget() to behave like a normal, vanilla text box.
            // This restores selection, copy/paste, and standard aesthetics.
            super.renderWidget(context, mouseX, mouseY, delta);
        } finally {
            com.trusted.systemnbteditor.util.MixinState.isRenderingNbtEditor = wasRendering;
        }
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        refreshSuggestor();
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        refreshSuggestor();
    }

    private void refreshSuggestor() {
        if (suggestor != null) {
            suggestor.refresh();
        }
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        suggestor.setWindowActive(focused);
        suggestor.refresh();
    }
}
