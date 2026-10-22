package dod.unrealfrontiers.ufessential.skins;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** /ufskin refresh|clear &lt;player&gt; */
public final class SkinCommands {
    private SkinCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ufskin")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("refresh")
                        .then(Commands.argument("player", EntityArgument.player()).executes(SkinCommands::refresh)))
                .then(Commands.literal("clear")
                        .then(Commands.argument("player", EntityArgument.player()).executes(SkinCommands::clear))));
    }

    private static int refresh(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        if (!SkinManager.isActive()) {
            source.sendFailure(Component.literal("Skins are off (dedicated server, skins.enabled and skins.apiKey required)."));
            return 0;
        }
        String name = player.getGameProfile().getName();
        SkinManager.refresh(player).whenCompleteAsync((skin, error) -> {
            if (error != null) {
                source.sendFailure(Component.literal("Could not fetch the skin of " + name + ": " + (error.getCause() != null ? error.getCause() : error)));
            } else if (skin.isEmpty()) {
                source.sendSuccess(() -> Component.literal(name + " has no active skin."), true);
            } else {
                source.sendSuccess(() -> Component.literal("Skin of " + name + " refreshed."), true);
            }
        }, player.server);
        return 1;
    }

    private static int clear(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        SkinManager.clear(player);
        String name = player.getGameProfile().getName();
        ctx.getSource().sendSuccess(() -> Component.literal("Skin of " + name + " removed until a refresh or rejoin."), true);
        return 1;
    }
}
