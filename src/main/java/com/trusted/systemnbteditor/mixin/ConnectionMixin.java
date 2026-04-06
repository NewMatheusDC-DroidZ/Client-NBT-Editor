package com.trusted.systemnbteditor.mixin;

import com.trusted.systemnbteditor.data.ModConfig;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class ConnectionMixin {
    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void onSendPacket(Packet<?> packet, CallbackInfo ci) {
        if (ModConfig.getInstance().customSpoofingEnabled) {
            if (packet instanceof CustomPayloadC2SPacket payloadPacket) {
                String namespace = payloadPacket.payload().getId().id().getNamespace();
                if (namespace.equals("system_nbt_editor") || namespace.equals("fabric")) {
                    ci.cancel();
                }
            }
        }
    }

    @Inject(method = "handlePacket", at = @At("HEAD"), cancellable = true)
    private static void onHandlePacket(Packet<?> packet, net.minecraft.network.listener.PacketListener listener, CallbackInfo ci) {
        if (ModConfig.getInstance().customSpoofingEnabled) {
            if (packet instanceof CustomPayloadS2CPacket payloadPacket) {
                String namespace = payloadPacket.payload().getId().id().getNamespace();
                if (namespace.equals("system_nbt_editor") || namespace.equals("fabric")) {
                    ci.cancel();
                }
            }
        }
    }
}
