package dod.unrealfrontiers.ufessential.mixins;

import dod.unrealfrontiers.ufessential.UFEssential;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


//Needed to debug incoming packets
@Mixin(Connection.class)
public class ConnectionMixin {
    @Inject(method = "genericsFtw", at = @At("HEAD"))
    private static <T extends PacketListener> void beforeGenericFtw(Packet<T> packet, PacketListener packetListener, CallbackInfo ci)  {
        UFEssential.LOGGER.warn("----- Got some packet {}", packet.getClass().getName());
    }
}
