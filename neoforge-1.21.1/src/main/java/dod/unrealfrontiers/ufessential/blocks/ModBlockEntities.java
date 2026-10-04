package dod.unrealfrontiers.ufessential.blocks;

import dod.unrealfrontiers.ufessential.UFEssential;
import dod.unrealfrontiers.ufessential.blocks.ConfigurableTeleportBlock.ConfigurableTeleportBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, UFEssential.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ConfigurableTeleportBlockEntity>> CONFIGURABLE_PORTAL_BE =
            BLOCK_ENTITIES.register("configurable_portal",
                    () -> BlockEntityType.Builder.of(
                            ConfigurableTeleportBlockEntity::new,
                            ModBlocks.CONFIGURABLE_PORTAL.get()
                    ).build(null));
}
