package dod.unrealfrontiers.ufessential.Network.Connection;

import dod.unrealfrontiers.ufessential.mixininterfaces.ClientHandshakePacketListenerMixin;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.login.ClientLoginPacketListener;

public class ClientboundReadyToAuthPacket implements Packet<ClientLoginPacketListener> {
    @Override
    public void write(FriendlyByteBuf buf) {

    }

    @Override
    public void handle(ClientLoginPacketListener listener) {
        ((ClientHandshakePacketListenerMixin)listener).handleGreeting(this);
    }
}
