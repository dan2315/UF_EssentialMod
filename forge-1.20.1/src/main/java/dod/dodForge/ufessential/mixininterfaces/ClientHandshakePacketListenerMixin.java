package dod.dodForge.ufessential.mixininterfaces;

import dod.dodForge.ufessential.Network.Connection.ClientboundReadyToAuthPacket;
import net.minecraft.network.PacketListener;

public interface ClientHandshakePacketListenerMixin
{
   void handleGreeting(ClientboundReadyToAuthPacket packet);
}