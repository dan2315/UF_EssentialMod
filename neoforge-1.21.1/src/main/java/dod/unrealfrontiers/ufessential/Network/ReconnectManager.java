package dod.unrealfrontiers.ufessential.Network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;

import java.net.InetSocketAddress;

public class ReconnectManager {
    private static boolean reconnecting = false;
    private static int attempt = 0;

    public static void tryReconnect(Minecraft mc, ServerData serverData) {
        if (reconnecting) return;
        reconnecting = true;


        if (attempt >= 5) {
            mc.execute(() -> mc.setScreen(
                    new DisconnectedScreen(new TitleScreen(),
                            Component.literal("Disconnected"),
                            Component.literal("Failed after 5 attempts"))));
            return;
        } else {
            mc.execute(() -> {
                try {
                    String[] addressElements = serverData.ip.split(":");
                    ServerAddress serverAddress = new ServerAddress(addressElements[0], Integer.parseInt(addressElements[1]));
                    mc.setScreen(null);
                    ConnectScreen.startConnecting(new TitleScreen(), mc, serverAddress, serverData, false, null);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    reconnecting = false;
                }
            });
        }


        attempt++;
    }

    public static void resetAttempts() {
        attempt = 0;
    }
}
