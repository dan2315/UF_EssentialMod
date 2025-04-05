package dod.dodForge.ufessential.mixininterfaces;

import dod.dodForge.ufessential.Network.Connection.ClientboundReadyToAuthPacket;

public interface ClientHandshakePacketListenerMixin
{
   void handleGreeting(ClientboundReadyToAuthPacket packet);
}