package dod.dodForge.ufessential.mixininterfaces;

import dod.dodForge.ufessential.Network.Connection.ServerboundTokensPacket;
import net.minecraft.network.protocol.game.ServerPacketListener;

public interface ServerLoginPacketListenerMixin extends ServerPacketListener {

    void handleTokens(ServerboundTokensPacket packet);

}
