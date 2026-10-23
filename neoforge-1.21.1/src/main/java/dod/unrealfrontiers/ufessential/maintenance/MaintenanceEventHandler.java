package dod.unrealfrontiers.ufessential.maintenance;

import dod.unrealfrontiers.ufessential.UFEssential;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;

/**
 * Fallback for players who were already connecting when maintenance was switched on; everyone else is turned away
 * earlier by {@link MaintenanceCheckTask}.
 */
public class MaintenanceEventHandler {

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        MaintenanceManager maintenanceManager = MaintenanceManager.get(player.server);
        if (!maintenanceManager.canJoin(player.server, player.getGameProfile())) {
            player.connection.disconnect(maintenanceManager.getMaintenanceMessage());
            UFEssential.LOGGER.info("Player {} was disconnected due to maintenance mode", player.getName().getString());
        }
    }
}
