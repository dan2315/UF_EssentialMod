package dod.unrealfrontiers.ufessential.Network;

import dod.unrealfrontiers.ufessential.UFEssential;
import dod.unrealfrontiers.ufessential.Network.Utils.DataToSendOnServerboundHello;
import dod.unrealfrontiers.ufessential.mixins.DisconnectedScreenMixin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.bus.api.SubscribeEvent;

@OnlyIn(Dist.CLIENT)
public class RedirectClientHandler {
    static String MASTER_SERVER_HOST = "play.unrealfrontiers.xyz";
    static int MASTER_SERVER_PORT = 25565;

    @SubscribeEvent
    public static void OnOpenScreen(ScreenEvent.Opening event) {
        UFEssential.LOGGER.warn("Screen Open Event");
        if (!(event.getNewScreen() instanceof DisconnectedScreen ds)) return;
        var mc = Minecraft.getInstance();

        var reason = ((DisconnectedScreenMixin) ds).getDetails().reason();
        var text = reason.getString();

        if (!text.startsWith("[")) return;

        UFEssential.LOGGER.warn("Screen Open Event, but specified that Disconnect: {}", text);
        ServerAddress serverAddress = null;
        if (text.startsWith("[redirect]")) {
            DataToSendOnServerboundHello.Add(mc.getUser().getProfileId(), text);
            serverAddress = new ServerAddress(MASTER_SERVER_HOST, MASTER_SERVER_PORT);
            UFEssential.LOGGER.warn("Redirecting client to {}", serverAddress);
        } else if (text.startsWith("[goto]")) {
            String serverIp = text.substring("[goto]".length()).trim();
            if (serverIp.isEmpty()) return;
            String[] serverIpArray = serverIp.split(":");
            if (serverIpArray.length < 2) {
                UFEssential.LOGGER.error("Master server sent me to {}, but it's not valid address", serverIp);
                return;
            }
            serverAddress = new ServerAddress(serverIpArray[0], Integer.parseInt(serverIpArray[1]));
            UFEssential.LOGGER.warn("Connecting client to {}", serverAddress);
        }
        // Any other "[...]" disconnect message is a normal kick
        if (serverAddress == null) return;

        mc.screen = event.getScreen();
        var data = new ServerData("Server", serverAddress.getHost() + ":" + serverAddress.getPort(), ServerData.Type.OTHER);
        ConnectScreen.startConnecting(new TitleScreen(), mc, serverAddress, data, false, null);

        event.setCanceled(true);
    }
}
