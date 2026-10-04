package dod.unrealfrontiers.ufessential.mixins;

import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.network.DisconnectionDetails;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DisconnectedScreen.class)
public interface DisconnectedScreenMixin {
        // The reason moved into DisconnectionDetails in 1.21
        @Accessor("details")
        DisconnectionDetails getDetails();
}
