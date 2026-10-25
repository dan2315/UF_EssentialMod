package dod.unrealfrontiers.ufessential.client;

import dod.unrealfrontiers.ufessential.UFEssential;
import net.minecraft.client.OptionInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Turns on the Gliders mod's "Glider Perspective" option (third person while gliding). Gliders creates it off and
 * never saves it, so it is set again on every launch; players can still switch it off for the session in Controls.
 * Looked up by reflection, so Gliders stays optional.
 */
@OnlyIn(Dist.CLIENT)
public class GliderPerspectiveDefault {
    private static final String GLIDERS_CLIENT = "net.venturecraft.gliders.VCGlidersClient";

    @SuppressWarnings("unchecked")
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            try {
                var option = (OptionInstance<Boolean>) Class.forName(GLIDERS_CLIENT).getField("autoPerspective").get(null);
                if (option != null) {
                    option.set(true);
                }
            } catch (ClassNotFoundException e) {
                // Gliders isn't installed
            } catch (ReflectiveOperationException | ClassCastException e) {
                UFEssential.LOGGER.warn("[UF] Could not turn on Glider Perspective: {}", e.toString());
            }
        });
    }
}
