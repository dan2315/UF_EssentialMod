package dod.unrealfrontiers.ufessential.Network.Connection;

import dod.unrealfrontiers.ufessential.UFEssential;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server asks the client for its Unreal Frontiers tokens (sent during the configuration phase).
 */
public record ClientboundReadyToAuthPayload() implements CustomPacketPayload {
    public static final Type<ClientboundReadyToAuthPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(UFEssential.MODID, "ready_to_auth"));
    public static final StreamCodec<FriendlyByteBuf, ClientboundReadyToAuthPayload> STREAM_CODEC =
            StreamCodec.unit(new ClientboundReadyToAuthPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
