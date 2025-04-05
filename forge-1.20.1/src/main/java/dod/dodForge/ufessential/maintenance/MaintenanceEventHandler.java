package dod.dodForge.ufessential.maintenance;

import dod.dodForge.ufessential.UFEssential;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraft.network.chat.Component;

public class MaintenanceEventHandler {

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            UFEssential.LOGGER.error("Server is null during player login");
            return;
        }

        MaintenanceManager maintenanceManager = MaintenanceManager.get(server);
        if (!maintenanceManager.isMaintenanceModeEnabled()) {
            return;
        }

        if (server.getPlayerList().isOp(player.getGameProfile())) {
            UFEssential.LOGGER.info("Player {} allowed to join during maintenance (op)", player.getName().getString());
            return;
        }

        if (maintenanceManager.isPlayerAllowed(player.getName().getString())) {
            UFEssential.LOGGER.info("Player {} allowed to join during maintenance (whitelist)", player.getName().getString());
            return;
        }

        player.connection.disconnect(maintenanceManager.getMaintenanceMessage());
        UFEssential.LOGGER.info("Player {} was disconnected due to maintenance mode", player.getName().getString());
    }
} 