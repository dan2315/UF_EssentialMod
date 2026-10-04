package dod.unrealfrontiers.ufessential.mixins;

import dod.unrealfrontiers.ufessential.Network.ReconnectManager;
import dod.unrealfrontiers.ufessential.UFEssential;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.ReadTimeoutHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.InetSocketAddress;


@Mixin(Connection.class)
public class ConnectionMixin {
    @Shadow private Channel channel;

    @Inject(method = "connect", at = @At("RETURN"))
    private static void afterConnect(InetSocketAddress socketAddress, boolean useNative, Connection connection, CallbackInfoReturnable<ChannelFuture> cir) {
        ChannelFuture future = cir.getReturnValue();
        future.addListener(f -> {
            if (future.isSuccess()) {
                Channel channel = future.channel();
                if (channel.pipeline().get("timeout") != null) {
                    channel.pipeline().replace("timeout", "timeout", new ReadTimeoutHandler(1));
                }
            }
        });
    }

    @Inject(method = "setListener", at = @At("RETURN"))
    private void afterSetListener(PacketListener packetListener, CallbackInfo ci)
    {
        if (channel == null) return;

        if (packetListener instanceof ClientGamePacketListener) {
            Minecraft.getInstance().execute(() -> {
                if (channel.pipeline().get("timeout") != null) {
                    channel.pipeline().replace("timeout", "timeout", new ReadTimeoutHandler(30));
                }
            });
        }
    }

    @Inject(method = "exceptionCaught", at = @At("HEAD"))
    void beforeExceptionCaught(ChannelHandlerContext channelHandlerContext, Throwable throwable, CallbackInfo ci) {
        UFEssential.LOGGER.warn("beforeExceptionCaught");
        if (throwable instanceof ReadTimeoutException) {
            Minecraft mc = Minecraft.getInstance();
            ServerData serverData = mc.getCurrentServer();
            UFEssential.LOGGER.warn("Server data is {}", serverData);

            if (serverData != null) {
                ReconnectManager.tryReconnect(mc, serverData);
            }
            channelHandlerContext.close();
        }
    }

    @Inject(method = "channelActive", at = @At("HEAD"))
    void beforeChannelActive(ChannelHandlerContext channelHandlerContext, CallbackInfo ci) {
        UFEssential.LOGGER.warn("beforeChannelActive");
        ReconnectManager.resetAttempts();
    }

}
