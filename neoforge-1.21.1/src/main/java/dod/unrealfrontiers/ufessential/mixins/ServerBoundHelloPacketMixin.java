package dod.unrealfrontiers.ufessential.mixins;

import dod.unrealfrontiers.ufessential.Network.Utils.DataToSendOnServerboundHello;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ServerboundHelloPacket.class)
public abstract class ServerBoundHelloPacketMixin {

    @Shadow @Final private UUID profileId;

    @Inject(method = "write", at = @At("RETURN"))
    public void additionalWrite(FriendlyByteBuf buff, CallbackInfo ci) {
        var data = DataToSendOnServerboundHello.Consume(profileId);
        if (data == null) return;

        buff.writeUtf(data);
    }
}
