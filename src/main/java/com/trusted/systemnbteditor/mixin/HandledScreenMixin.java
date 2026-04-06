package com.trusted.systemnbteditor.mixin;

import com.trusted.systemnbteditor.gui.NbtEditorScreen;
import com.trusted.systemnbteditor.gui.NbtSelectionScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.awt.*;

@Mixin(value = HandledScreen.class, priority = 20)
public abstract class HandledScreenMixin extends Screen {

    @Shadow(aliases = {"field_2779", "focusedSlot", "hoveredSlot"})
    protected Slot focusedSlot;

    protected HandledScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(KeyInput input, CallbackInfoReturnable<Boolean> cir) {
        try {
            int key = input.key();
            int modifiers = input.modifiers();
            
            // Log EVERY key press in HandledScreen for debugging
            System.out.println("[NBT Editor] HandledScreen.keyPressed: key=" + key + ", scancode=" + input.scancode() + ", mods=" + modifiers);

            com.trusted.systemnbteditor.data.ModConfig config = com.trusted.systemnbteditor.data.ModConfig.getInstance();
            if (config.editorKeybind == null || config.editorKeybind.isEmpty()) return;
            
            boolean allPressed = true;
            for (int k : config.editorKeybind) {
                if (key != k && !InputUtil.isKeyPressed(this.client.getWindow(), k)) {
                    allPressed = false;
                    break;
                }
            }
            if (allPressed) {
                System.out.println("[NBT Editor] Shift+Space detected!");
                
                Slot hovered = this.focusedSlot;
                
                // Fallback reflection just in case Shadow failed
                if (hovered == null) {
                    hovered = systemNbtEditor$getHoveredSlotFallback();
                }

                if (hovered != null) {
                    System.out.println("[NBT Editor] Hovered slot found: id=" + hovered.id + ", inventory=" + hovered.inventory.getClass().getSimpleName());
                    if (hovered.hasStack()) {
                        ItemStack stack = hovered.getStack();
                        int networkSlotId = getNetworkSlotId(hovered);
                        
                        System.out.println("[NBT Editor] Item found: " + stack.getItem() + " (Network ID: " + networkSlotId + ")");

                        if (networkSlotId != -1 && this.client != null) {
                            System.out.println("[NBT Editor] Setting screen to NbtSelectionScreen...");
                            
                            // Generate a robust callback that works for ANY slot in ANY screen
                            final Slot finalHovered = hovered;
                            java.util.function.Consumer<ItemStack> callback = (newStack) -> {
                                if (this.client != null) {
                                    // 1. Switch back to the original screen to ensure the right handler is active
                                    this.client.setScreen(this);
                                    
                                    // 2. Force replacement in the edited slot: clear old, then set new
                                    finalHovered.setStack(ItemStack.EMPTY);
                                    finalHovered.setStack(newStack.copy());
                                    
                                    // 3. Send clear + set packets for deterministic replacement
                                    if (this.client.getNetworkHandler() != null) {
                                        this.client.getNetworkHandler().sendPacket(
                                            new net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket(finalHovered.id, ItemStack.EMPTY));
                                        this.client.getNetworkHandler().sendPacket(
                                            new net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket(finalHovered.id, newStack.copy()));
                                    }
                                    
                                    System.out.println("[NBT Editor] Callback executed: slot=" + finalHovered.id + ", item=" + newStack.getItem());
                                }
                            };
                            
                            this.client.setScreen(new NbtSelectionScreen(stack, networkSlotId, callback));
                            cir.setReturnValue(true);
                        } else {
                            System.out.println("[NBT Editor] Cannot open: networkSlotId=" + networkSlotId + ", client=" + (this.client != null));
                        }
                    } else {
                        System.out.println("[NBT Editor] Hovered slot is empty.");
                    }
                } else {
                    System.out.println("[NBT Editor] No hovered slot found (even after fallback).");
                }
            }
        } catch (Throwable t) {
            System.err.println("[NBT Editor] CRITICAL ERROR in onKeyPressed!");
            t.printStackTrace();
        }
    }

    private Slot systemNbtEditor$getHoveredSlotFallback() {
        try {
            // Try all possible names via reflection
            String[] names = {"focusedSlot", "hoveredSlot", "field_2779"};
            for (String name : names) {
                try {
                    java.lang.reflect.Field f = HandledScreen.class.getDeclaredField(name);
                    f.setAccessible(true);
                    Slot slot = (Slot) f.get(this);
                    if (slot != null) return slot;
                } catch (NoSuchFieldException ignored) {}
            }
            
            // Last resort: search by type
            for (java.lang.reflect.Field f : HandledScreen.class.getDeclaredFields()) {
                if (Slot.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    Slot slot = (Slot) f.get(this);
                    if (slot != null) return slot;
                }
            }
        } catch (Throwable t) {
            // Ignore reflection errors in fallback
        }
        return null;
    }

    private int getNetworkSlotId(Slot slot) {
        return slot.id;
    }
}
