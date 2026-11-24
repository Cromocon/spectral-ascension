package cromocon.spectralascension.dev;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ScreenshotEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Mod di SVILUPPO (source set {@code dev}): non è nel jar di rilascio, la caricano solo runClient e runClientAuto.
 * <p>
 * Contiene lo scenario runner: legge scenari JSON, li esegue nel client (con il server integrato), salva screenshot
 * e un report. Non fa nulla da solo: parte solo con la property {@code spectral_ascension.autoScenario}
 * (avvio automatico) o con il comando {@code /sa scenario run <nome>}.
 */
@Mod(value = SADev.MOD_ID, dist = Dist.CLIENT)
public final class SADev {
    public static final String MOD_ID = "spectral_ascension_dev";

    public SADev(IEventBus modBus, ModContainer container) {
        LogCapture.install(); // per assert_log
        ScenarioRunner runner = ScenarioRunner.get();
        NeoForge.EVENT_BUS.addListener((RegisterClientCommandsEvent e) -> ScenarioCommand.register(e.getDispatcher()));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Pre e) -> runner.onTickPre());
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> runner.onTickPost());
        NeoForge.EVENT_BUS.addListener((RenderFrameEvent.Post e) -> runner.onFrameEnd());
        NeoForge.EVENT_BUS.addListener((ScreenshotEvent e) -> runner.onScreenshot(e));
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> runner.onLogout());
        NeoForge.EVENT_BUS.addListener((RenderGuiEvent.Pre e) -> {
            if (runner.isHudHidden()) e.setCanceled(true); // HUD nascosto ma mano/oggetto in mano visibili (F1 li nasconderebbe)
        });
    }
}
