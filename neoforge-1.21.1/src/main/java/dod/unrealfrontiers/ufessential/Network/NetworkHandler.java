package dod.unrealfrontiers.ufessential.Network;

import dod.unrealfrontiers.ufessential.Auth.AuthService;
import dod.unrealfrontiers.ufessential.Network.Connection.AuthConfigurationTask;
import dod.unrealfrontiers.ufessential.Network.Connection.ClientboundReadyToAuthPayload;
import dod.unrealfrontiers.ufessential.Network.Connection.ServerboundTokensPayload;
import dod.unrealfrontiers.ufessential.UFEssential;
import dod.unrealfrontiers.ufessential.utils.TokenStorage;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.neoforged.neoforge.network.event.RegisterConfigurationTasksEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.concurrent.CompletableFuture;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";

    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.configurationToClient(ClientboundReadyToAuthPayload.TYPE, ClientboundReadyToAuthPayload.STREAM_CODEC, NetworkHandler::handleReadyToAuth);
        registrar.configurationToServer(ServerboundTokensPayload.TYPE, ServerboundTokensPayload.STREAM_CODEC, NetworkHandler::handleTokens);
    }

    public static void onRegisterConfigurationTasks(RegisterConfigurationTasksEvent event) {
        var server = ServerLifecycleHooks.getCurrentServer();
        // Same as before: singleplayer and LAN skip the account check
        if (server != null && server.isDedicatedServer()) {
            event.register(new AuthConfigurationTask());
        }
    }

    // Client: answer with the tokens the launcher stored
    private static void handleReadyToAuth(ClientboundReadyToAuthPayload payload, IPayloadContext context) {
        UFEssential.LOGGER.info("[UF] Server requested authentication");
        String accessToken = "";
        String refreshToken = "";
        try {
            String[] tokens = TokenStorage.loadTokens();
            accessToken = tokens[0];
            refreshToken = tokens.length > 1 ? tokens[1] : "";
        } catch (Exception e) {
            // Send empty tokens; the server rejects them with a readable message instead of a crash
            UFEssential.LOGGER.error("[UF] Failed to load auth tokens", e);
        }
        context.reply(new ServerboundTokensPayload(accessToken, refreshToken));
    }

    // Server: verify the tokens against the backend, then let the player in or disconnect them
    private static void handleTokens(ServerboundTokensPayload payload, IPayloadContext context) {
        UFEssential.LOGGER.info("[UF] Trying to process Tokens Packet");
        String nickname = ((ServerCommonPacketListenerImpl) context.listener()).getOwner().getName();

        // The backend call can take seconds, so keep it off the server thread
        CompletableFuture.supplyAsync(() -> {
            try {
                return AuthService.isAuthenticated(payload.accessToken(), payload.refreshToken(), nickname);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).whenComplete((authenticated, error) -> context.enqueueWork(() -> {
            if (error != null) {
                UFEssential.LOGGER.error("[UF] Authentication of {} failed", nickname, error);
                context.disconnect(Component.literal("А вы знали, что Unhahdled Exception это когда хуй знает, что произошло.\nТак вот, UNHAHDLED_EXCEPTION: " + error));
            } else if (!authenticated) {
                context.disconnect(Component.literal("Не удалось подтвердить аккаунт UnrealFrontiers.\nНе закрывая Minecraft, перезапустите лаунчер и попробуйте подключиться ещё раз."));
            } else {
                UFEssential.LOGGER.warn("[UF] Successfully authorized {}", nickname);
                context.finishCurrentTask(AuthConfigurationTask.TYPE);
            }
        }));
    }
}
