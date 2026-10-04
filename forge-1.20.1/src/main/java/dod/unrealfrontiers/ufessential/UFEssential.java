package dod.unrealfrontiers.ufessential;

import com.mojang.logging.LogUtils;
import dod.unrealfrontiers.ufessential.Network.NetworkHandler;
import dod.unrealfrontiers.ufessential.Network.RedirectClientHandler;
import dod.unrealfrontiers.ufessential.blocks.ModBlockEntities;
import dod.unrealfrontiers.ufessential.blocks.ModBlocks;
import dod.unrealfrontiers.ufessential.commands.RedirectCommand;
import dod.unrealfrontiers.ufessential.maintenance.MaintenanceCommands;
import dod.unrealfrontiers.ufessential.maintenance.MaintenanceEventHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(UFEssential.MODID)
public class UFEssential {
    public static final String MODID = "ufessential";
    public static final Logger LOGGER = LogUtils.getLogger();

    public UFEssential() {
        MinecraftForge.EVENT_BUS.register(this);
        LOGGER.warn("[UF] Unreal Frontiers init");
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        
        MinecraftForge.EVENT_BUS.register(new MaintenanceEventHandler());

        if (FMLEnvironment.dist == Dist.CLIENT) {
            MinecraftForge.EVENT_BUS.register(RedirectClientHandler.class);
        }
        var bus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.BLOCKS.register(bus);
        ModBlockEntities.BLOCK_ENTITIES.register(bus);
    }

    private void setup(FMLCommonSetupEvent event) {
        NetworkHandler.register();
        LOGGER.info("Common setup event passed");
    }
    
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        LOGGER.info("Registering commands");
        MaintenanceCommands.register(event.getDispatcher());
        RedirectCommand.register(event.getDispatcher());
    }
}
