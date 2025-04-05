package dod.dodForge.ufessential;

import com.mojang.logging.LogUtils;
import dod.dodForge.ufessential.Network.NetworkHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
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
    }

    private void setup(FMLCommonSetupEvent event) {
        NetworkHandler.register();
        LOGGER.info("Common setup event passed");
    }
}
