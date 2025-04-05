package dod.dodForge.ufessential.mixins;

import dod.dodForge.ufessential.Auth.AuthService;
import dod.dodForge.ufessential.Network.Connection.ClientboundReadyToAuthPacket;
import dod.dodForge.ufessential.Network.Connection.ServerboundTokensPacket;
import dod.dodForge.ufessential.UFEssential;
import dod.dodForge.ufessential.mixininterfaces.ServerLoginPacketListenerMixin;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.server.MinecraftServer;
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

    @Shadow @Final private MinecraftServer server;

    @Inject(method = "handleHello", at = @At(value = "TAIL"))
    private void afterHandleHello(ServerboundHelloPacket packet, CallbackInfo ci)
    {
        String state = getState();
        if(state.equals("NEGOTIATING"))
        {
            setState("KEY");
            connection.send(new ClientboundReadyToAuthPacket());
        }
    }

    @Override
    public void handleTokens(ServerboundTokensPacket packet) {
        UFEssential.LOGGER.info("[UF] Trying to process Tokens Packet");
        try {
            ServerLoginPacketListenerImpl listener = (ServerLoginPacketListenerImpl) (Object) this;
            String nickname = listener.gameProfile.getName();

            if (!AuthService.isAuthenticated(packet.getAccessToken(), packet.getRefreshToken(), nickname) && server.isDedicatedServer()) {
                disconnect(Component.literal("Не удалось подтвердить аккаунт UnrealFrontiers.\nНе закрывая Minecraft, перезапустите лаунчер и попробуйте подключиться ещё раз."));
            }
            else {
                setState("NEGOTIATING");
                UFEssential.LOGGER.warn("[UF] Successfully authorized");
            }
        } catch (Exception e) {
            disconnect(Component.literal("А вы знали, что Unhahdled Exception это когда хуй знает, что произошло.\nТак вот, UNHAHDLED_EXCEPTION: " + e));
        }


    }

    private void setState(String enumConstantName) {
        try {
            Class<?> stateEnumClass = Class.forName("net.minecraft.server.network.ServerLoginPacketListenerImpl$State");
            Object targetState = Enum.valueOf((Class<Enum>) stateEnumClass, enumConstantName);
            Field stateField = this.getClass().getDeclaredField("f_10019_");

            stateField.setAccessible(true);
            stateField.set(this, targetState);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set state enum value", e);
        }
    }

    public String getState() {

        try {
            Field stateField = ServerLoginPacketListenerImpl.class.getDeclaredField("f_10019_");
            stateField.setAccessible(true);  // Allow access to private fields
            Object stateValue = stateField.get(this);

            UFEssential.LOGGER.info("[UF] Successfully retrieved 'state' field value using reflection.");
            return stateValue instanceof Enum ? ((Enum<?>) stateValue).name() : stateValue.toString();

        } catch (NoSuchFieldException e) {
            UFEssential.LOGGER.warn("[UF] 'state' field not found in class ServerLoginPacketListenerImpl.", e);

        } catch (IllegalAccessException e) {
            UFEssential.LOGGER.warn("[UF] Unable to access 'state' field value due to access restrictions.", e);

        } catch (SecurityException e) {
            UFEssential.LOGGER.warn("[UF] SecurityException occurred while accessing 'state' field. Check security manager settings.", e);

        } catch (IllegalArgumentException e) {
            UFEssential.LOGGER.warn("[UF] IllegalArgumentException: The provided object may not match the field’s declaring class.", e);

        } catch (Exception e) {
            UFEssential.LOGGER.warn("[UF] Unexpected exception occurred while trying to access 'state' field.", e);
        }

        throw new RuntimeException("Failed to get 'state' field value after multiple attempts.");
    }

}
