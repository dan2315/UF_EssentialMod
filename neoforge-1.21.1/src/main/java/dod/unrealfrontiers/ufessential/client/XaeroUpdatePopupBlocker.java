package dod.unrealfrontiers.ufessential.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Never shows the "mods are outdated" popup of Xaero's Minimap / World Map, as if "Don't show again" had been pressed.
 * The modpack pins the versions, so players can't update them anyway. The screen's constructor already marks the
 * notification as shown for this session, so it isn't retried after we cancel it.
 * Matched by class name, so Xaero's mods stay optional.
 */
@OnlyIn(Dist.CLIENT)
public class XaeroUpdatePopupBlocker {
    private static final String UPDATE_SCREEN = "xaero.lib.client.gui.GuiUpdateAll";

    @SubscribeEvent
    public static void onOpenScreen(ScreenEvent.Opening event) {
        if (event.getNewScreen() != null && event.getNewScreen().getClass().getName().equals(UPDATE_SCREEN)) {
            event.setCanceled(true);
        }
    }
}
