package dod.unrealfrontiers.ufessential;

import com.mojang.logging.LogUtils;
import dod.unrealfrontiers.ufessential.Network.NetworkHandler;
import dod.unrealfrontiers.ufessential.Network.RedirectClientHandler;
import dod.unrealfrontiers.ufessential.blocks.ModBlockEntities;
import dod.unrealfrontiers.ufessential.blocks.ModBlocks;
import dod.unrealfrontiers.ufessential.commands.RedirectCommand;
import dod.unrealfrontiers.ufessential.maintenance.MaintenanceCommands;
import dod.unrealfrontiers.ufessential.maintenance.MaintenanceEventHandler;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

@Mod(UFEssential.MODID)
public class UFEssential {
    public static final String MODID = "ufessential";
    public static final Logger LOGGER = LogUtils.getLogger();

    public UFEssential(IEventBus modEventBus, ModContainer modContainer) {
        NeoForge.EVENT_BUS.register(this);

        modContainer.registerConfig(ModConfig.Type.CLIENT, UFConfig.SPEC);
        modEventBus.addListener(UFConfig::onConfigEvent);
        LOGGER.warn("[UF] Unreal Frontiers init");

        modEventBus.addListener(NetworkHandler::onRegisterPayloads);
        modEventBus.addListener(NetworkHandler::onRegisterConfigurationTasks);

        NeoForge.EVENT_BUS.register(new MaintenanceEventHandler());

        if (FMLEnvironment.dist == Dist.CLIENT) {
            NeoForge.EVENT_BUS.register(RedirectClientHandler.class);
        }

        ModBlocks.BLOCKS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        LOGGER.info("Registering commands");
        MaintenanceCommands.register(event.getDispatcher());
        RedirectCommand.register(event.getDispatcher());
    }
}
