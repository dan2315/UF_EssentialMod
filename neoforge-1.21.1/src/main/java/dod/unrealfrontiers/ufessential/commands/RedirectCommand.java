package dod.unrealfrontiers.ufessential.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dod.unrealfrontiers.ufessential.Network.RedirectMethod;
import dod.unrealfrontiers.ufessential.blocks.ConfigurableTeleportBlock.ConfigurableTeleportBlockEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

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

        dispatcher.register(Commands.literal("setportal")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("center", BlockPosArgument.blockPos())
                        .then(Commands.argument("cmd", StringArgumentType.string())
                                .executes(ctx -> {
                                    BlockPos center = BlockPosArgument.getLoadedBlockPos(ctx, "center");
                                    String cmd = StringArgumentType.getString(ctx, "cmd");
                                    ServerLevel level = ctx.getSource().getLevel();

                                    int radius = 2; // 5x5x5 = radius 2 around center
                                    int count = 0;

                                    for (int dx = -radius; dx <= radius; dx++) {
                                        for (int dy = -radius; dy <= radius; dy++) {
                                            for (int dz = -radius; dz <= radius; dz++) {
                                                BlockPos pos = center.offset(dx, dy, dz);
                                                BlockEntity be = level.getBlockEntity(pos);
                                                if (be instanceof ConfigurableTeleportBlockEntity portal) {
                                                    portal.setCommand(cmd);
                                                    count++;
                                                }
                                            }
                                        }
                                    }

                                    int finalCount = count;
                                    ctx.getSource().sendSuccess(
                                            () -> Component.literal("Configured " + finalCount + " portals with: " + cmd),
                                            true
                                    );

                                    return count;
                                }))));
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
