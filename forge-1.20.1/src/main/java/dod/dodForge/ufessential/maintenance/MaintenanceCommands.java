package dod.dodForge.ufessential.maintenance;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dod.dodForge.ufessential.UFEssential;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.List;

public class MaintenanceCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Create the main command: /maintenance
        LiteralArgumentBuilder<CommandSourceStack> maintenanceCommand = Commands.literal("maintenance")
            .requires(source -> source.hasPermission(2)) // Requires permission level 2 (ops)
            
            // Toggle command: /maintenance toggle
            .then(Commands.literal("toggle")
                .executes(context -> toggleMaintenanceMode(context.getSource())))
            
            // List commands: /maintenance list
            .then(Commands.literal("list")
                .executes(context -> listAllowedPlayers(context.getSource()))
                
                // Add player: /maintenance list add <player>
                .then(Commands.literal("add")
                    .then(Commands.argument("player", StringArgumentType.word())
                        .executes(context -> addAllowedPlayer(context.getSource(), StringArgumentType.getString(context, "player")))))
                
                // Remove player: /maintenance list remove <player>
                .then(Commands.literal("remove")
                    .then(Commands.argument("player", StringArgumentType.word())
                        .executes(context -> removeAllowedPlayer(context.getSource(), StringArgumentType.getString(context, "player"))))));
        
        // Register the command
        dispatcher.register(maintenanceCommand);
    }

    private static int toggleMaintenanceMode(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        MaintenanceManager maintenanceManager = MaintenanceManager.get(server);
        
        boolean newState = maintenanceManager.toggleMaintenanceMode();
        
        if (newState) {
            source.sendSuccess(() -> Component.literal("Maintenance mode enabled."), true);
            
            // Kick non-op, non-whitelisted players
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (!server.getPlayerList().isOp(player.getGameProfile()) && 
                    !maintenanceManager.isPlayerAllowed(player.getName().getString())) {
                    player.connection.disconnect(maintenanceManager.getMaintenanceMessage());
                    UFEssential.LOGGER.info("Player {} was disconnected due to maintenance mode being enabled", player.getName().getString());
                }
            }
        } else {
            source.sendSuccess(() -> Component.literal("Maintenance mode disabled."), true);
        }
        
        // Update Velocity about the maintenance status change
        VelocityMessenger.updateMaintenanceStatus();
        
        return 1;
    }

    private static int listAllowedPlayers(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        MaintenanceManager maintenanceManager = MaintenanceManager.get(server);
        
        List<String> allowedPlayers = maintenanceManager.getAllowedPlayers();
        
        if (allowedPlayers.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No players on the maintenance whitelist."), false);
        } else {
            source.sendSuccess(() -> Component.literal("Players allowed during maintenance mode:"), false);
            for (String player : allowedPlayers) {
                source.sendSuccess(() -> Component.literal("- " + player), false);
            }
        }
        
        return allowedPlayers.size();
    }

    private static int addAllowedPlayer(CommandSourceStack source, String playerName) {
        MinecraftServer server = source.getServer();
        MaintenanceManager maintenanceManager = MaintenanceManager.get(server);
        
        if (maintenanceManager.isPlayerAllowed(playerName)) {
            source.sendFailure(Component.literal("Player " + playerName + " is already on the maintenance whitelist."));
            return 0;
        }
        
        maintenanceManager.addAllowedPlayer(playerName);
        source.sendSuccess(() -> Component.literal("Player " + playerName + " added to the maintenance whitelist."), true);
        
        return 1;
    }

    private static int removeAllowedPlayer(CommandSourceStack source, String playerName) {
        MinecraftServer server = source.getServer();
        MaintenanceManager maintenanceManager = MaintenanceManager.get(server);
        
        if (!maintenanceManager.isPlayerAllowed(playerName)) {
            source.sendFailure(Component.literal("Player " + playerName + " is not on the maintenance whitelist."));
            return 0;
        }
        
        maintenanceManager.removeAllowedPlayer(playerName);
        source.sendSuccess(() -> Component.literal("Player " + playerName + " removed from the maintenance whitelist."), true);
        
        return 1;
    }
} 