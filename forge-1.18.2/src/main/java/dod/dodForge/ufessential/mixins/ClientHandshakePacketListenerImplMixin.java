package dod.dodForge.ufessential.mixins;

import dod.dodForge.ufessential.Network.Connection.ClientboundReadyToAuthPacket;
import dod.dodForge.ufessential.Network.Connection.ServerboundTokensPacket;
import dod.dodForge.ufessential.UFEssential;
import dod.dodForge.ufessential.mixininterfaces.ClientHandshakePacketListenerMixin;
import dod.dodForge.ufessential.utils.TokenStorage;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ClientHandshakePacketListenerImpl.class)
public class ClientHandshakePacketListenerImplMixin implements ClientHandshakePacketListenerMixin
{
    @Shadow @Final private Connection connection;

    @Unique @Override
    public void handleGreeting(ClientboundReadyToAuthPacket packet) {
        try {
            var tokens = TokenStorage.loadTokens();
            connection.send(new ServerboundTokensPacket(tokens[0], tokens[1]));
            UFEssential.LOGGER.warn("[UF] Tokens sent to verify");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onDisconnect(Component p_130552_) {

    }

    @Override
    public Connection getConnection() {
        return null;
    }
}