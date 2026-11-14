package dod.unrealfrontiers.ufessential.blocks;

import dod.unrealfrontiers.ufessential.UFEssential;
import dod.unrealfrontiers.ufessential.blocks.ConfigurableTeleportBlock.ConfigurableTeleportBlock;
import dod.unrealfrontiers.ufessential.blocks.Rift.RiftBlock;
import net.minecraft.world.level.block.SoundType;
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

    /** A rift in space: walk into it to go where its command says ({@code /setrift}). */
    public static final DeferredBlock<RiftBlock> RIFT =
            BLOCKS.register("rift",
                    () -> new RiftBlock(BlockBehaviour.Properties.of()
                            .noCollission()
                            .noOcclusion()
                            .noLootTable()
                            .sound(SoundType.AMETHYST)
                            .lightLevel(state -> 13)
                            .strength(-1.0F, 3600000.0F)));
}
