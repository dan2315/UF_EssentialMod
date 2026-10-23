package dod.unrealfrontiers.ufessential.maintenance;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import java.util.List;

public class MaintenanceCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> maintenanceCommand = Commands.literal("maintenance")
            .requires(source -> source.hasPermission(2)) // Requires permission level 2 (ops)

            // /maintenance and /maintenance status: show whether it's on
            .executes(context -> showStatus(context.getSource()))
            .then(Commands.literal("status")
                .executes(context -> showStatus(context.getSource())))

            .then(Commands.literal("on")
                .executes(context -> setMaintenanceMode(context.getSource(), true)))
            .then(Commands.literal("off")
                .executes(context -> setMaintenanceMode(context.getSource(), false)))
            .then(Commands.literal("toggle")
                .executes(context -> setMaintenanceMode(context.getSource(),
                        !MaintenanceManager.get(context.getSource().getServer()).isMaintenanceModeEnabled())))

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

    private static int showStatus(CommandSourceStack source) {
        MaintenanceManager maintenanceManager = MaintenanceManager.get(source.getServer());
        boolean enabled = maintenanceManager.isMaintenanceModeEnabled();
        int allowed = maintenanceManager.getAllowedPlayers().size();
        source.sendSuccess(() -> Component.literal("Maintenance mode is " + (enabled ? "ON" : "OFF")
                + ". Allowed besides ops: " + allowed + " player(s), see /maintenance list."), false);
        return enabled ? 1 : 0;
    }

    private static int setMaintenanceMode(CommandSourceStack source, boolean enabled) {
        MinecraftServer server = source.getServer();
        MaintenanceManager maintenanceManager = MaintenanceManager.get(server);

        if (maintenanceManager.isMaintenanceModeEnabled() == enabled) {
            source.sendFailure(Component.literal("Maintenance mode is already " + (enabled ? "on" : "off") + "."));
            return 0;
        }

        maintenanceManager.setMaintenanceMode(enabled);
        maintenanceManager.applyMotd(server);
        if (enabled) {
            int kicked = maintenanceManager.kickDisallowedPlayers(server);
            source.sendSuccess(() -> Component.literal("Maintenance mode enabled. Disconnected " + kicked + " player(s)."), true);
        } else {
            source.sendSuccess(() -> Component.literal("Maintenance mode disabled."), true);
        }
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