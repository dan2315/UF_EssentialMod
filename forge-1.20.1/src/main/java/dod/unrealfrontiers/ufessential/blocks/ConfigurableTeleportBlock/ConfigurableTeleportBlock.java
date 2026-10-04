package dod.unrealfrontiers.ufessential.blocks.ConfigurableTeleportBlock;

import dod.unrealfrontiers.ufessential.Network.RedirectMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ConfigurableTeleportBlock extends Block implements EntityBlock {
    public ConfigurableTeleportBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos blockPos, @NotNull BlockState blockState) {
        return new ConfigurableTeleportBlockEntity(blockPos, blockState);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState blockState, Entity entity) {
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            if (!player.isCrouching()) return;
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ConfigurableTeleportBlockEntity portal) {
                executeCommand(player, portal.getCommand());
            }
        }
    }

    private void executeCommand(ServerPlayer player, String cmd) {
        if (cmd == null || cmd.isEmpty()) return;

        if (cmd.startsWith("redirect:")) {
            String serverId = cmd.substring(9);
            RedirectMethod.ForwardPlayerToServer(player, serverId);

        } else if (cmd.startsWith("tp:")) {
            String dimId = cmd.substring(3);
            ResourceKey<Level> targetDim = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(dimId));
            ServerLevel targetLevel = player.server.getLevel(targetDim);
            if (targetLevel != null) {
                player.changeDimension(targetLevel);
            }
        }
    }
}
