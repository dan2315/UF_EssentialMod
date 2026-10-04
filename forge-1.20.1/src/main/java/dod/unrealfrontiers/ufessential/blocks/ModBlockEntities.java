package dod.unrealfrontiers.ufessential.blocks;

import dod.unrealfrontiers.ufessential.UFEssential;
import dod.unrealfrontiers.ufessential.blocks.ConfigurableTeleportBlock.ConfigurableTeleportBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, UFEssential.MODID);

    public static final RegistryObject<BlockEntityType<ConfigurableTeleportBlockEntity>> CONFIGURABLE_PORTAL_BE =
            BLOCK_ENTITIES.register("configurable_portal",
                    () -> BlockEntityType.Builder.of(
                            ConfigurableTeleportBlockEntity::new,
                            ModBlocks.CONFIGURABLE_PORTAL.get()
                    ).build(null));
}
