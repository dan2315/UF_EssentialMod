package dod.dodForge.ufessential.Network.Connection;

import dod.dodForge.ufessential.UFEssential;
import dod.dodForge.ufessential.mixininterfaces.ClientHandshakePacketListenerMixin;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.login.ClientLoginPacketListener;

public class ClientboundReadyToAuthPacket implements Packet<ClientLoginPacketListener> {

    public ClientboundReadyToAuthPacket() {

    }

    public ClientboundReadyToAuthPacket(FriendlyByteBuf friendlyByteBuf) {
    }

    @Override
    public void write(FriendlyByteBuf buf) {

    }

    @Override
    public void handle(ClientLoginPacketListener listener) {
        UFEssential.LOGGER.warn("[UF] Get REDY TO AUUUUUUUUTH");
        ((ClientHandshakePacketListenerMixin)listener).handleGreeting(this);
    }
}
