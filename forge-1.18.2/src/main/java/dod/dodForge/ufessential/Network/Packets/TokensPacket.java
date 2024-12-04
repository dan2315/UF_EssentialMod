package dod.dodForge.ufessential.Network.Packets;

import dod.dodForge.ufessential.UFEssential;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

import dod.dodForge.ufessential.Auth.AuthService;

public class TokensPacket {
    private final String accessToken;
    private final String refreshToken;

    public TokensPacket(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public static void encode(TokensPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.accessToken);
        buffer.writeUtf(packet.refreshToken);
    }

    public static TokensPacket decode(FriendlyByteBuf buffer) {
        return new TokensPacket(buffer.readUtf(), buffer.readUtf());
    }

    public static void handle(TokensPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() != null) {
                UFEssential.LOGGER.warn("OnLogin MessageReceived");
                try {
                    if (!AuthService.isAuthenticated(packet.accessToken, packet.refreshToken)) {
                        context.getSender().connection.disconnect(new TextComponent("Проблема с авторизацией, попробуйте перезайти через лаунчер"));
                    }
                    else {
                        context.getSender().sendMessage(new TextComponent("[§cServer§r] Добро пожаловать,§l{" + context.getSender().getName() + "}§r"), context.getSender().getUUID());
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        context.setPacketHandled(true);
    }
}
