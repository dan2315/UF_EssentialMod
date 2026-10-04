package dod.unrealfrontiers.ufessential.Network.Connection;

import dod.unrealfrontiers.ufessential.UFEssential;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.configuration.ICustomConfigurationTask;

import java.util.function.Consumer;

/**
 * Holds a joining player in the configuration phase until their Unreal Frontiers account is verified.
 * Replaces the 1.20.1 version, which injected custom packets into the login protocol via reflection.
 * The task finishes in {@link dod.unrealfrontiers.ufessential.Network.NetworkHandler} once the tokens are checked.
 */
public record AuthConfigurationTask() implements ICustomConfigurationTask {
    public static final Type TYPE = new Type(ResourceLocation.fromNamespaceAndPath(UFEssential.MODID, "auth"));

    @Override
    public void run(Consumer<CustomPacketPayload> sender) {
        sender.accept(new ClientboundReadyToAuthPayload());
    }

    @Override
    public Type type() {
        return TYPE;
    }
}
