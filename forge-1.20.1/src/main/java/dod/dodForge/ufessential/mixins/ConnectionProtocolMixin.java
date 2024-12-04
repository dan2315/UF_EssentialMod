package dod.dodForge.ufessential.mixins;

import dod.dodForge.ufessential.Network.Connection.ClientboundReadyToAuthPacket;
import dod.dodForge.ufessential.Network.Connection.ServerboundTokensPacket;
import dod.dodForge.ufessential.UFEssential;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.function.Function;


@Mixin(ConnectionProtocol.class)
public class ConnectionProtocolMixin {

    @Inject(method = "<clinit>", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ConnectionProtocol;values()[Lnet/minecraft/network/ConnectionProtocol;"))
    private static void registerPackets(CallbackInfo ci) {

        try {
            for (ConnectionProtocol protocolEntry : ConnectionProtocol.values()) {
                UFEssential.LOGGER.warn("[UF] Found value {} in ENUM", protocolEntry.name());
            }

            ConnectionProtocol protocol = ConnectionProtocol.LOGIN;

            Field flowsField = ConnectionProtocol.class.getDeclaredField("f_129574_"); // flows
            flowsField.setAccessible(true);
            
            Object clientboundPacketSet = ((Map<?,?>) flowsField.get(protocol)).get(PacketFlow.CLIENTBOUND);
            Object serverboundPacketSet = ((Map<?,?>) flowsField.get(protocol)).get(PacketFlow.SERVERBOUND);

            Method addPacketMethod = clientboundPacketSet.getClass().getDeclaredMethod("m_178330_", Class.class, Function.class);
            addPacketMethod.setAccessible(true);

            addPacketMethod.invoke(clientboundPacketSet, ClientboundReadyToAuthPacket.class, (Function<FriendlyByteBuf, ClientboundReadyToAuthPacket>) ClientboundReadyToAuthPacket::new);
            addPacketMethod.invoke(serverboundPacketSet, ServerboundTokensPacket.class, (Function<FriendlyByteBuf, ServerboundTokensPacket>) ServerboundTokensPacket::new);
            
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
