package dod.unrealfrontiers.ufessential.blocks;

import dod.unrealfrontiers.ufessential.UFEssential;
import dod.unrealfrontiers.ufessential.blocks.ConfigurableTeleportBlock.ConfigurableTeleportBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(UFEssential.MODID);

    public static final DeferredBlock<Block> CONFIGURABLE_PORTAL =
            BLOCKS.register("configurable_teleport",
                    () -> new ConfigurableTeleportBlock(
                            BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                                    .lightLevel(state -> 11)
                                    .strength(-1.0F, 3600000.0F) // unbreakable
                    ));
}
