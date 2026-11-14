package dod.unrealfrontiers.ufessential.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dod.unrealfrontiers.ufessential.Network.ServerTransfer;
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

/**
 * {@code /redirect to <server>} and {@code /redirect <players> to <server>}: sends players to another server of the
 * network ({@link ServerTransfer}). {@code /setportal <pos> <cmd>} configures the portal blocks around a position.
 */
public class RedirectCommand {
    private static final SuggestionProvider<CommandSourceStack> SERVERS =
            (ctx, builder) -> ServerTransfer.suggestServers(builder);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("redirect")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("to")
                                .then(Commands.argument("serverId", StringArgumentType.word())
                                        .suggests(SERVERS)
                                        .executes(RedirectCommand::redirectSelf)))
                        .then(Commands.argument("targets", GameProfileArgument.gameProfile())
                                .then(Commands.literal("to")
                                        .then(Commands.argument("serverId", StringArgumentType.word())
                                                .suggests(SERVERS)
                                                .executes(RedirectCommand::redirectOthers))))
        );

        // A rift: /setrift <pos> <x|z> <cmd> [width height]; pos is its bottom centre, x|z the axis it lies along
        dispatcher.register(Commands.literal("setrift")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .then(Commands.argument("axis", StringArgumentType.word())
                                .suggests((ctx, builder) -> builder.suggest("x").suggest("z").buildFuture())
                                .then(Commands.argument("cmd", StringArgumentType.string())
                                        .executes(ctx -> placeRift(ctx, 3, 5))
                                        .then(Commands.argument("width", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 9))
                                                .then(Commands.argument("height", com.mojang.brigadier.arguments.IntegerArgumentType.integer(2, 12))
                                                        .executes(ctx -> placeRift(ctx,
                                                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "width"),
                                                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "height")))))))));

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

    private static int placeRift(CommandContext<CommandSourceStack> ctx, int width, int height) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
        String axis = StringArgumentType.getString(ctx, "axis");
        String cmd = StringArgumentType.getString(ctx, "cmd");
        if (!axis.equals("x") && !axis.equals("z")) {
            ctx.getSource().sendFailure(Component.literal("Ось: x или z (вдоль какой оси лежит разлом)"));
            return 0;
        }
        ServerLevel level = ctx.getSource().getLevel();
        level.setBlock(pos, dod.unrealfrontiers.ufessential.blocks.ModBlocks.RIFT.get().defaultBlockState()
                .setValue(dod.unrealfrontiers.ufessential.blocks.Rift.RiftBlock.AXIS,
                        axis.equals("x") ? net.minecraft.core.Direction.Axis.X : net.minecraft.core.Direction.Axis.Z), 3);
        if (level.getBlockEntity(pos) instanceof dod.unrealfrontiers.ufessential.blocks.Rift.RiftBlockEntity rift) {
            rift.configure(cmd, width, height);
        }
        ctx.getSource().sendSuccess(() -> Component.literal("Разлом " + width + "x" + height + " на " + pos.toShortString() + ": " + cmd), true);
        return 1;
    }

    private static int redirectSelf(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String serverId = StringArgumentType.getString(ctx, "serverId");
        ServerTransfer.send(player, serverId, failure -> ctx.getSource().sendFailure(failure));
        return 1;
    }

    private static int redirectOthers(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(ctx, "targets");
        String serverId = StringArgumentType.getString(ctx, "serverId");

        int sent = 0;
        for (GameProfile profile : profiles) {
            ServerPlayer player = ctx.getSource().getServer().getPlayerList().getPlayer(profile.getId());
            if (player == null) {
                ctx.getSource().sendFailure(Component.literal(profile.getName() + " не в игре"));
                continue;
            }
            String name = profile.getName();
            ServerTransfer.send(player, serverId, failure -> ctx.getSource().sendFailure(Component.literal(name + ": ").append(failure)));
            sent++;
        }
        int finalSent = sent;
        if (sent > 0) ctx.getSource().sendSuccess(() -> Component.literal("Переносим на " + serverId + ": " + finalSent), true);
        return sent;
    }
}
