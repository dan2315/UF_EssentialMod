package dod.dodForge.ufessential.Network.Connection;

import dod.dodForge.ufessential.mixininterfaces.ServerLoginPacketListenerMixin;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.login.ServerLoginPacketListener;

public class ServerboundTokensPacket implements Packet<ServerLoginPacketListener> {
    String accessToken;
    String refreshToken;

    public ServerboundTokensPacket(String accessToken, String refreshToken)
    {
        this.accessToken = accessToken;
        this.refreshToken = accessToken;
    }

    public ServerboundTokensPacket(FriendlyByteBuf buf)
    {
        accessToken = buf.readUtf();
        refreshToken = buf.readUtf();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(accessToken);
        buf.writeUtf(refreshToken);
    }

    @Override
    public void handle(ServerLoginPacketListener listener) {
        ((ServerLoginPacketListenerMixin)listener).handleTokens(this);
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }
}
