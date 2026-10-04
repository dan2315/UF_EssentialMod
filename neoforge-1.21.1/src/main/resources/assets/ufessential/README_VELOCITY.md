# UFEssential Maintenance Mode - Velocity Integration

This document explains how to integrate the maintenance mode functionality with Velocity proxy using the existing plugin messaging channel.

## How It Works

The maintenance mod sends plugin messages to Velocity through the `velocity:main` channel. These messages contain the maintenance status and message.

## Plugin Message Format

The plugin messages use the following format:

1. Communication Type (int) - The ordinal value of `ProxyCommunicationType.MAINTENANCE_STATUS`
2. Maintenance Mode (int) - 1 if enabled, 0 if disabled
3. If Maintenance Mode is enabled:
   - Message Length (int)
   - Message (UTF-8 encoded string)

## Adding Maintenance Handler to Your Velocity Plugin

To handle the maintenance messages in your existing Velocity plugin, add the following to your plugin messaging handler:

```java
// In your existing onPluginMessage handler:
@Subscribe
public void onPluginMessage(PluginMessageEvent event) {
    if (!event.getIdentifier().equals(MinecraftChannelIdentifier.create("velocity", "main"))) {
        return;
    }
    
    if (event.getSource() instanceof ServerConnection serverConnection) {
        ByteBuffer buffer = ByteBuffer.wrap(event.getData());
        
        // Get communication type
        switch (ProxyCommunicationType.values()[buffer.getInt()]) {
            case SERVER_STATUS -> {
                // Your existing server status handling
                PhaseId phase = PhaseId.values()[buffer.getInt()];
                var context = zarubaServerManager.Get(serverConnection.getServer());
                context.setStatus(phase);
            }
            case MAINTENANCE_STATUS -> {
                // New case for handling maintenance status
                String serverName = serverConnection.getServer().getServerInfo().getName();
                int isMaintenanceEnabled = buffer.getInt();
                boolean maintenanceEnabled = isMaintenanceEnabled == 1;
                
                // Update server maintenance status
                updateServerMaintenanceStatus(serverName, maintenanceEnabled);
                
                if (maintenanceEnabled) {
                    // Parse maintenance message if present
                    int messageLength = buffer.getInt();
                    if (messageLength > 0) {
                        byte[] messageBytes = new byte[messageLength];
                        buffer.get(messageBytes);
                        String message = new String(messageBytes, StandardCharsets.UTF_8);
                        
                        // Store the maintenance message
                        storeMaintenanceMessage(serverName, message);
                        logger.info("Server {} entered maintenance mode: {}", serverName, message);
                    }
                } else {
                    clearMaintenanceMessage(serverName);
                    logger.info("Server {} exited maintenance mode", serverName);
                }
            }
            // Other communication types...
        }
    }
}
```

## Implementation

Add the following methods to your Velocity plugin:

```java
// Map to track maintenance status
private final Map<String, Boolean> serverMaintenanceStatus = new ConcurrentHashMap<>();
private final Map<String, String> serverMaintenanceMessages = new ConcurrentHashMap<>();

/**
 * Update a server's maintenance status
 */
private void updateServerMaintenanceStatus(String serverName, boolean inMaintenance) {
    serverMaintenanceStatus.put(serverName, inMaintenance);
}

/**
 * Store a maintenance message for a server
 */
private void storeMaintenanceMessage(String serverName, String message) {
    serverMaintenanceMessages.put(serverName, message);
}

/**
 * Clear maintenance message for a server
 */
private void clearMaintenanceMessage(String serverName) {
    serverMaintenanceMessages.remove(serverName);
}

/**
 * Check if a server is in maintenance mode
 */
public boolean isServerInMaintenance(String serverName) {
    return serverMaintenanceStatus.getOrDefault(serverName, false);
}

/**
 * Get maintenance message for a server
 */
public String getMaintenanceMessage(String serverName) {
    return serverMaintenanceMessages.get(serverName);
}
```

## Using Maintenance Status in Your Load Balancer

In your server selection logic, check if a server is in maintenance mode:

```java
@Subscribe
public void onServerPreConnect(ServerPreConnectEvent event) {
    String targetServer = event.getOriginalServer().getServerInfo().getName();
    
    if (isServerInMaintenance(targetServer)) {
        // Don't let players connect to servers in maintenance
        // Unless they have permission
        if (!event.getPlayer().hasPermission("ufessential.maintenance.bypass")) {
            event.setResult(ServerPreConnectEvent.ServerResult.denied());
            
            // Show maintenance message to player
            String message = getMaintenanceMessage(targetServer);
            event.getPlayer().sendMessage(Component.text(message != null ? message : "Server is under maintenance"));
        }
    }
}
```

## Adding to the ProxyCommunicationType Enum

Add the `MAINTENANCE_STATUS` to your `ProxyCommunicationType` enum:

```java
public enum ProxyCommunicationType {
    SERVER_STATUS,
    SERVER_COMMAND,
    MAINTENANCE_STATUS // New type for maintenance status messages
}
``` 