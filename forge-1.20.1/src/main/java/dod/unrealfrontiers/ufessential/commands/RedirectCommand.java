package dod.unrealfrontiers.ufessential.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dod.unrealfrontiers.ufessential.Network.RedirectMethod;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

public class RedirectCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("redirect")
                        .requires(source -> source.hasPermission(2)) // requires OP, optional
                        // case 1: self redirect
                        .then(Commands.literal("to")
                                .then(Commands.argument("serverId", StringArgumentType.string())
                                        .executes(ctx -> redirectSelf(ctx))))
                        // case 2: redirect other players
                        .then(Commands.argument("targets", GameProfileArgument.gameProfile())
                                .then(Commands.literal("to")
                                        .then(Commands.argument("serverId", StringArgumentType.string())
                                                .executes(ctx -> redirectOthers(ctx)))))
        );
    }

    private static int redirectSelf(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String serverId = StringArgumentType.getString(ctx, "serverId");

        RedirectMethod.ForwardPlayerToServer(player, serverId);
        ctx.getSource().sendSuccess(() ->
                Component.literal("Redirected yourself to " + serverId), true);
        return 1;
    }

    private static int redirectOthers(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(ctx, "targets");
        String serverId = StringArgumentType.getString(ctx, "serverId");

        for (GameProfile profile : profiles) {
            ServerPlayer player = ctx.getSource().getServer().getPlayerList().getPlayer(profile.getId());
            if (player != null) {
                RedirectMethod.ForwardPlayerToServer(player, serverId);
            } else {
                ctx.getSource().sendFailure(Component.literal("Player " + profile.getName() + " is not online."));
            }
        }
        return profiles.size();
    }
}
