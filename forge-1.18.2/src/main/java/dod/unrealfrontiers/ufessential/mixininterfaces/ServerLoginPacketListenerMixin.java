package dod.unrealfrontiers.ufessential.mixininterfaces;

import dod.unrealfrontiers.ufessential.Network.Connection.ServerboundTokensPacket;
import net.minecraft.network.protocol.game.ServerPacketListener;

public interface ServerLoginPacketListenerMixin extends ServerPacketListener {

    void handleTokens(ServerboundTokensPacket packet);

}
