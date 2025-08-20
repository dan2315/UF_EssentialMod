package dod.unrealfrontiers.ufessential.mixins;

import dod.unrealfrontiers.ufessential.Network.Connection.ClientboundReadyToAuthPacket;
import dod.unrealfrontiers.ufessential.Network.Connection.ServerboundTokensPacket;
import dod.unrealfrontiers.ufessential.mixininterfaces.ClientHandshakePacketListenerMixin;
import dod.unrealfrontiers.ufessential.utils.TokenStorage;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ClientHandshakePacketListenerImpl.class)
public class ClientHandshakePacketListenerImplMixin implements ClientHandshakePacketListenerMixin
{
    @Shadow @Final private Connection connection;

    @Override
    public void handleGreeting(ClientboundReadyToAuthPacket packet) {
        try {
            var tokens = TokenStorage.loadTokens();
            connection.send(new ServerboundTokensPacket(tokens[0], tokens[1]));
        } catch (Exception e) {
            throw new RuntimeException("[UF] Произошла ошибка при отправке ауф данных на сервер: " + e);
        }
    }
}