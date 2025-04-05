package dod.dodForge.ufessential;

import com.mojang.logging.LogUtils;
import dod.dodForge.ufessential.Network.NetworkHandler;
import dod.dodForge.ufessential.utils.TokenStorage;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

@Mod(UFEssential.MODID)
public class UFEssential {
    public static final String MODID = "ufessential";
    public static final Logger LOGGER = LogUtils.getLogger();

    public UFEssential() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(FMLCommonSetupEvent event) {
        NetworkHandler.register();
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity().level.isClientSide())
        {
            String[] tokens = null;
            try {
                tokens = TokenStorage.loadTokens();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
//            TokensPacket packet = new TokensPacket(tokens[0], tokens[1]);
//            NetworkHandler.MAIN.sendToServer(packet);
        }

    }
}
