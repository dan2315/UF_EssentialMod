package dod.unrealfrontiers.ufessential.mixininterfaces;

import dod.unrealfrontiers.ufessential.Network.Connection.ClientboundReadyToAuthPacket;
import net.minecraft.network.PacketListener;

public interface ClientHandshakePacketListenerMixin extends PacketListener
{
   void handleGreeting(ClientboundReadyToAuthPacket packet);
}