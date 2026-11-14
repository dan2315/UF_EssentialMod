package dod.unrealfrontiers.ufessential.mixins;

import dod.unrealfrontiers.ufessential.Network.ReconnectManager;
import dod.unrealfrontiers.ufessential.UFEssential;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.ReadTimeoutException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// The connection keeps vanilla's 30 s read timeout in every phase. It used to be cut to 1 s until the configuration
// phase, which dropped players on any lag spike while logging in (and on every hop through a front server).
@Mixin(Connection.class)
public class ConnectionMixin {
    @Inject(method = "exceptionCaught", at = @At("HEAD"))
    void beforeExceptionCaught(ChannelHandlerContext channelHandlerContext, Throwable throwable, CallbackInfo ci) {
        if (throwable instanceof ReadTimeoutException) {
            Minecraft mc = Minecraft.getInstance();
            ServerData serverData = mc.getCurrentServer();
            UFEssential.LOGGER.info("[UF] Connection to {} timed out", serverData == null ? "?" : serverData.ip);

            if (serverData != null) {
                ReconnectManager.tryReconnect(mc, serverData);
            }
            channelHandlerContext.close();
        }
    }

    @Inject(method = "channelActive", at = @At("HEAD"))
    void beforeChannelActive(ChannelHandlerContext channelHandlerContext, CallbackInfo ci) {
        ReconnectManager.resetAttempts();
    }

}
