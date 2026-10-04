package dod.unrealfrontiers.ufessential.blocks.ConfigurableTeleportBlock;

import dod.unrealfrontiers.ufessential.blocks.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ConfigurableTeleportBlockEntity extends BlockEntity {
    private String command = "";

    public ConfigurableTeleportBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONFIGURABLE_PORTAL_BE.get(), pos, state);
    }

    public void setCommand(String cmd) {
        this.command = cmd;
        setChanged();
    }

    public String getCommand() {
        return command;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("PortalCommand", command);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("PortalCommand")) {
            command = tag.getString("PortalCommand");
        }
    }
}
