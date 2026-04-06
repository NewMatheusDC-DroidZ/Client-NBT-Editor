package com.trusted.systemnbteditor.mixin;


import com.trusted.systemnbteditor.data.ModConfig;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ InventoryScreen.class, CreativeInventoryScreen.class })
public abstract class InventoryScreenMixin extends HandledScreen<ScreenHandler> {

    public InventoryScreenMixin(ScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void addInventoryButtons(CallbackInfo ci) {
        int openEditorWidth = this.textRenderer.getWidth("Open Editor") + 20;
        int utilityFeaturesWidth = this.textRenderer.getWidth("Utility Features") + 20;
        int clientChestWidth = this.textRenderer.getWidth("Client Chest") + 20;
        int buttonHeight = 20;
        int padding = 5;

        int openEditorX = this.width - openEditorWidth - padding;
        int utilityFeaturesX = openEditorX - utilityFeaturesWidth - padding;
        int y = this.height - buttonHeight - padding;
        int clientChestX = padding;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Utility Features").formatted(Formatting.DARK_RED), (button) -> {
            if (this.client != null) {
                this.client.setScreen(new com.trusted.systemnbteditor.gui.UtilityMenuScreen(this));
            }
        }).dimensions(utilityFeaturesX, y, utilityFeaturesWidth, buttonHeight).build());

        this.addDrawableChild(ButtonWidget.builder(Text.of("Open Editor"), (button) -> {
            if (this.client != null && this.client.player != null) {
                if (this.client.player.getMainHandStack().isEmpty()) {
                    return;
                }
                
                int selected = this.client.player.getInventory().selectedSlot;
                // Standard index for Hotbar in PlayerScreenHandler is selected + 36
                net.minecraft.screen.slot.Slot slot = this.handler.getSlot(selected + 36);
                
                java.util.function.Consumer<ItemStack> callback = (newStack) -> {
                    if (this.client != null) {
                        this.client.setScreen(this);
                        slot.setStack(ItemStack.EMPTY);
                        slot.setStack(newStack.copy());
                        if (this.client.getNetworkHandler() != null) {
                            this.client.getNetworkHandler().sendPacket(
                                new net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket(slot.id, ItemStack.EMPTY));
                            this.client.getNetworkHandler().sendPacket(
                                new net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket(slot.id, newStack.copy()));
                        }
                    }
                };
                
                this.client.setScreen(new com.trusted.systemnbteditor.gui.NbtSelectionScreen(this.client.player.getMainHandStack(), slot.id, callback));
            }
        }).dimensions(openEditorX, y, openEditorWidth, buttonHeight).build());

        ButtonWidget clientChestButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("Client Chest").formatted(Formatting.DARK_AQUA), (button) -> {
            if (this.client != null) {
                this.client.setScreen(new com.trusted.systemnbteditor.gui.ClientChestScreen(this));
            }
        }).dimensions(clientChestX, y, clientChestWidth, buttonHeight).build());
        clientChestButton.active = ModConfig.getInstance().clientChestButtonVisible;
    }
}
