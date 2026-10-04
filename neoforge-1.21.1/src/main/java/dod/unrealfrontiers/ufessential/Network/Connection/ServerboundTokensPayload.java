package dod.unrealfrontiers.ufessential.Network.Connection;

import dod.unrealfrontiers.ufessential.UFEssential;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client's answer to {@link ClientboundReadyToAuthPayload}. Empty strings mean the client has no tokens.
 */
public record ServerboundTokensPayload(String accessToken, String refreshToken) implements CustomPacketPayload {
    public static final Type<ServerboundTokensPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(UFEssential.MODID, "tokens"));
    public static final StreamCodec<FriendlyByteBuf, ServerboundTokensPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ServerboundTokensPayload::accessToken,
            ByteBufCodecs.STRING_UTF8, ServerboundTokensPayload::refreshToken,
            ServerboundTokensPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
