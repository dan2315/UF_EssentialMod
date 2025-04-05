package dod.dodForge.ufessential;

import com.mojang.logging.LogUtils;
import dod.dodForge.ufessential.Network.NetworkHandler;
import dod.dodForge.ufessential.maintenance.MaintenanceCommands;
import dod.dodForge.ufessential.maintenance.MaintenanceEventHandler;
import dod.dodForge.ufessential.maintenance.MaintenanceManager;
import dod.dodForge.ufessential.maintenance.VelocityMessenger;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(UFEssential.MODID)
public class UFEssential {
    public static final String MODID = "ufessential";
    public static final Logger LOGGER = LogUtils.getLogger();

    public UFEssential() {
        MinecraftForge.EVENT_BUS.register(this);
        LOGGER.warn("[UF] Unreal Frontiers init");
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        
        // Register the maintenance event handler
        MinecraftForge.EVENT_BUS.register(new MaintenanceEventHandler());
    }

    private void setup(FMLCommonSetupEvent event) {
        NetworkHandler.register();
        LOGGER.info("Common setup event passed");
    }
    
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        LOGGER.info("Registering maintenance commands");
        MaintenanceCommands.register(event.getDispatcher());
    }
}
