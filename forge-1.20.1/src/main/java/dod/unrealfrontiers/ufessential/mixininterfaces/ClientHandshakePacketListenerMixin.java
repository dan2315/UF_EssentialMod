package dod.unrealfrontiers.ufessential.mixininterfaces;

import dod.unrealfrontiers.ufessential.Network.Connection.ClientboundReadyToAuthPacket;

public interface ClientHandshakePacketListenerMixin
{
   void handleGreeting(ClientboundReadyToAuthPacket packet);
}