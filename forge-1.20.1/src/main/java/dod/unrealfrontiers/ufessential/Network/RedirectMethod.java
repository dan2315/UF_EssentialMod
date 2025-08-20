package dod.unrealfrontiers.ufessential.Network;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class RedirectMethod {
    public static void ForwardPlayerToServer(ServerPlayer player, String serverId) {
        player.connection.disconnect(Component.literal("[redirect]" + serverId));
    }
}
