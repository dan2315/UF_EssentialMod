package dod.dodForge.ufessential.maintenance;

import dod.dodForge.ufessential.UFEssential;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;
import io.netty.buffer.Unpooled;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class VelocityMessenger {
    private static final String PROTOCOL_VERSION = "1";
    public static final ResourceLocation VELOCITY_CHANNEL_LOCATION = new ResourceLocation("velocity", "main");
    
    public enum MessageType {
        MAINTENANCE_STATUS(2);
        
        private final int id;
        
        MessageType(int id) {
            this.id = id;
        }
        
        public int getId() {
            return id;
        }
    }
    
    
    public enum ProxyCommunicationType {
        SERVER_STATUS,
        SERVER_COMMAND,
        MAINTENANCE_STATUS
    }
    
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static boolean initialized = false;

    public static void initialize(MinecraftServer server) {
        if (initialized) {
            return;
        }

        scheduler.scheduleAtFixedRate(() -> sendMaintenanceStatus(), 10, 30, TimeUnit.SECONDS);
        
        initialized = true;
        UFEssential.LOGGER.info("VelocityMessenger initialized, will send updates via velocity:main channel");
    }

    public static void sendMaintenanceStatus() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || server.getPlayerList().getPlayers().isEmpty()) {
            return;
        }
        
        MaintenanceManager manager = MaintenanceManager.get(server);
        boolean maintenanceMode = manager.isMaintenanceModeEnabled();
        
        try {
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            
            buffer.putInt(ProxyCommunicationType.MAINTENANCE_STATUS.ordinal());
            
            buffer.putInt(maintenanceMode ? 1 : 0);
            
            if (maintenanceMode) {
                String message = manager.getMaintenanceMessage().getString();
                byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
                buffer.putInt(messageBytes.length);
                buffer.put(messageBytes);
            } else {
                buffer.putInt(0); // No message
            }
            
            buffer.flip();
            
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            
            if (!server.getPlayerList().getPlayers().isEmpty()) {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                
                FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
                UFEssential.LOGGER.debug("Sending maintenance status to Velocity: {}", maintenanceMode);
                
                player.connection.send(new ClientboundCustomPayloadPacket(
                    VELOCITY_CHANNEL_LOCATION, 
                    buf
                ));
            }
        } catch (Exception e) {
            UFEssential.LOGGER.error("Error sending maintenance status to Velocity", e);
        }
    }
    
    public static void updateMaintenanceStatus() {
        if (!initialized) {
            return;
        }
        
        scheduler.execute(VelocityMessenger::sendMaintenanceStatus);
    }
    
    public static void shutdown() {
        if (!initialized) {
            return;
        }
        
        scheduler.shutdown();
        initialized = false;
    }
}