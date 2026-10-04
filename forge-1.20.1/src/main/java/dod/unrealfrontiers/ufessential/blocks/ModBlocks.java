package dod.unrealfrontiers.ufessential.blocks;

import dod.unrealfrontiers.ufessential.UFEssential;
import dod.unrealfrontiers.ufessential.blocks.ConfigurableTeleportBlock.ConfigurableTeleportBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, UFEssential.MODID);

    public static final RegistryObject<Block> CONFIGURABLE_PORTAL =
            BLOCKS.register("configurable_teleport",
                    () -> new ConfigurableTeleportBlock(
                            BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                                    .lightLevel(state -> 11)
                                    .strength(-1.0F, 3600000.0F) // unbreakable
                    ));
}