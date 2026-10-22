package dod.unrealfrontiers.ufessential.skins;

import dod.unrealfrontiers.ufessential.UFEssential;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.configuration.ServerConfigurationPacketListener;
import net.neoforged.neoforge.network.configuration.ICustomConfigurationTask;

import java.util.function.Consumer;

/**
 * Holds a joining player for at most the join timeout while their skin is fetched, so it is already in the profile
 * when the player is announced to others. Server side only: nothing is sent to the client.
 */
public record SkinConfigurationTask(ServerConfigurationPacketListener listener) implements ICustomConfigurationTask {
    public static final Type TYPE = new Type(ResourceLocation.fromNamespaceAndPath(UFEssential.MODID, "skin"));

    @Override
    public void run(Consumer<CustomPacketPayload> sender) {
        SkinManager.onConfigure(listener, () -> listener.finishCurrentTask(TYPE));
    }

    @Override
    public Type type() {
        return TYPE;
    }
}
