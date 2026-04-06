package com.trusted.systemnbteditor.gui;

import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;

public class ContainerScreenHandlerSlot extends Slot {
    private Identifier texture;

    public ContainerScreenHandlerSlot(Slot slot) {
        super(slot.inventory, slot.getIndex(), slot.x, slot.y);
        this.id = slot.id;
    }

    public void setTexture(Identifier texture) {
        this.texture = texture;
    }

    @Override
    public Identifier getBackgroundSprite() {
        return texture;
    }
}
