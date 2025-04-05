package dod.dodForge.ufessential.maintenance;

import dod.dodForge.ufessential.UFEssential;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class VelocityNotifier {
    private static final String MAINTENANCE_FLAG_FILE = "maintenance.flag";
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static boolean initialized = false;
    private static Path maintenanceFlagPath;

    public static void initialize(MinecraftServer server) {
        if (initialized) {
            return;
        }
        
        maintenanceFlagPath = Paths.get(server.getServerDirectory().getPath(), MAINTENANCE_FLAG_FILE);
        
        scheduler.scheduleAtFixedRate(() -> updateMaintenanceFlag(), 0, 30, TimeUnit.SECONDS);
        
        initialized = true;
        UFEssential.LOGGER.info("VelocityNotifier initialized, maintenance flag path: {}", maintenanceFlagPath);
    }

    public static void updateMaintenanceFlag() {
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) {
                return;
            }
            
            MaintenanceManager manager = MaintenanceManager.get(server);
            boolean maintenanceMode = manager.isMaintenanceModeEnabled();
            
            if (maintenanceMode) {
                // Create the maintenance flag file
                try (FileWriter writer = new FileWriter(maintenanceFlagPath.toFile())) {
                    writer.write("maintenance=true\n");
                    writer.write("message=" + manager.getMaintenanceMessage().getString() + "\n");
                    writer.write("timestamp=" + System.currentTimeMillis() + "\n");
                }
                UFEssential.LOGGER.debug("Maintenance flag file created/updated");
            } else {
                // Delete the flag file if it exists
                Files.deleteIfExists(maintenanceFlagPath);
                UFEssential.LOGGER.debug("Maintenance flag file deleted");
            }
        } catch (IOException e) {
            UFEssential.LOGGER.error("Error updating maintenance flag file", e);
        }
    }
    
    /**
     * Shuts down the notifier
     */
    public static void shutdown() {
        if (!initialized) {
            return;
        }
        
        scheduler.shutdown();
        try {
            // Delete the flag file on shutdown
            Files.deleteIfExists(maintenanceFlagPath);
        } catch (IOException e) {
            UFEssential.LOGGER.error("Error deleting maintenance flag file during shutdown", e);
        }
        
        initialized = false;
    }
} 