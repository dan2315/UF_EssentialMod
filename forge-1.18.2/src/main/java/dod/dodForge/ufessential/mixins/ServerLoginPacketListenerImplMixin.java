package dod.dodForge.ufessential.mixins;

import dod.dodForge.ufessential.Auth.AuthService;
import dod.dodForge.ufessential.Network.Connection.ClientboundReadyToAuthPacket;
import dod.dodForge.ufessential.Network.Connection.ServerboundTokensPacket;
import dod.dodForge.ufessential.UFEssential;
import dod.dodForge.ufessential.mixininterfaces.ServerLoginPacketListenerMixin;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


import java.lang.reflect.Field;

@Mixin(ServerLoginPacketListenerImpl.class)
public abstract class ServerLoginPacketListenerImplMixin implements ServerLoginPacketListenerMixin {


    @Shadow
    public void disconnect(Component reason){}

    @Shadow @Final private Connection connection;

    @Inject(method = "handleHello", at = @At(value = "TAIL"))
    private void afterHandleHello(ServerboundHelloPacket packet, CallbackInfo ci)
    {
        if(getState().equals("NEGOTIATING"))
        {
            setState("KEY");
            connection.send(new ClientboundReadyToAuthPacket());
            UFEssential.LOGGER.warn("[UF] Ready to authenticate");
        }
    }

    @Override
    public void handleTokens(ServerboundTokensPacket packet) {
        try {
            if (!AuthService.isAuthenticated(packet.getAccessToken(), packet.getRefreshToken())) {
                disconnect(new TextComponent("Не удалось подтвердить аккаунт UnrealFrontiers. Перезайдите ещё раз через лаунчер"));
            }
            else {
                setState("NEGOTIATING");
                UFEssential.LOGGER.warn("[UF] Successfully authorized");
            }
        } catch (Exception e) {
            disconnect(new TextComponent("А вы знали, что Unhahdled Exception это когда хуй знает, что произошло. Так вот, UNHADLED_EXCEPTION"));
        }


    }

    private void setState(String enumConstantName) {
        try {
            Class<?> stateEnumClass = Class.forName("net.minecraft.server.network.ServerLoginPacketListenerImpl$State");
            Object targetState = Enum.valueOf((Class<Enum>) stateEnumClass, enumConstantName);
            Field stateField = this.getClass().getDeclaredField("state");
            stateField.setAccessible(true);
            stateField.set(this, targetState);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set state enum value", e);
        }
    }

    public String getState() {
        try {
            Field stateField = this.getClass().getDeclaredField("state");
            stateField.setAccessible(true);  // Allow access to private fields
            Object stateValue = stateField.get(this);

            // If it's an enum, we can get its name directly
            return stateValue instanceof Enum ? ((Enum<?>) stateValue).name() : stateValue.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get 'state' field value", e);
        }
    }

}
