package dod.unrealfrontiers.ufessential.maintenance;

import dod.unrealfrontiers.ufessential.UFEssential;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.configuration.ServerConfigurationPacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.neoforged.neoforge.network.configuration.ICustomConfigurationTask;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.function.Consumer;

/**
 * Turns players away during maintenance while they are still connecting, before the account check and before
 * anyone sees them join. Registered only while maintenance mode is on.
 */
public record MaintenanceCheckTask(ServerConfigurationPacketListener listener) implements ICustomConfigurationTask {
    public static final Type TYPE = new Type(ResourceLocation.fromNamespaceAndPath(UFEssential.MODID, "maintenance"));

    @Override
    public void run(Consumer<CustomPacketPayload> sender) {
        var server = ServerLifecycleHooks.getCurrentServer();
        var common = (ServerCommonPacketListenerImpl) listener;
        var profile = common.getOwner();
        MaintenanceManager maintenance = MaintenanceManager.get(server);
        if (maintenance.canJoin(server, profile)) {
            listener.finishCurrentTask(TYPE);
            return;
        }
        UFEssential.LOGGER.info("Player {} turned away: maintenance mode", profile.getName());
        common.disconnect(maintenance.getMaintenanceMessage());
    }

    @Override
    public Type type() {
        return TYPE;
    }
}
