package cromocon.spectralascension.dev;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;

import java.lang.reflect.Field;
import java.util.Locale;

/**
 * Azioni sulle schermate aperte (GUI): {@code gui_click} (clic o solo mouse sopra un punto della schermata) e
 * {@code assert_screen} (quale schermata è aperta). Vedi tools/SCENARI.md.
 * <p>
 * Le coordinate sono quelle della GUI (scalate, come in {@code Screen.mouseClicked}): {@code offset} [dx, dy] dal
 * centro della finestra (default), oppure {@code pos} [x, y] assolute. Il mouse viene spostato davvero
 * (MouseHandler.xpos/ypos, per riflessione come in BookFocus), così i tooltip compaiono nello screenshot successivo.
 */
final class GuiClick {
    private GuiClick() {
    }

    /**
     * gui_click: aspetta (fino a {@code within} tick, 40) che sia aperta una schermata, il cui nome di classe contiene
     * {@code screen} se indicato; poi sposta il mouse sul punto e, se {@code click} (true), fa clic con {@code button}
     * (0 = sinistro) chiamando mouseClicked e mouseReleased della schermata, come fa il gioco.
     */
    static Exec click(JsonObject s) {
        String want = Scenario.str(s, "screen", null);
        boolean click = Scenario.bool(s, "click", true);
        int button = Scenario.integer(s, "button", 0);
        int within = Scenario.integer(s, "within", 40);
        return new Exec() {
            int t;

            @Override
            public StepResult poll(ScenarioRunner runner) {
                Minecraft mc = Minecraft.getInstance();
                Screen screen = mc.screen;
                if (screen == null || (want != null && !screen.getClass().getName().contains(want))) {
                    if (t++ < within) return null;
                    return StepResult.fail("schermata " + (want == null ? "" : want + " ") + "non aperta (aperta: "
                            + (screen == null ? "nessuna" : screen.getClass().getName()) + ")");
                }
                double[] p = point(s, mc.getWindow());
                Window w = mc.getWindow();
                try {
                    Field fx = MouseHandler.class.getDeclaredField("xpos");
                    Field fy = MouseHandler.class.getDeclaredField("ypos");
                    fx.setAccessible(true);
                    fy.setAccessible(true);
                    fx.setDouble(mc.mouseHandler, p[0] * w.getScreenWidth() / (double) w.getGuiScaledWidth());
                    fy.setDouble(mc.mouseHandler, p[1] * w.getScreenHeight() / (double) w.getGuiScaledHeight());
                } catch (ReflectiveOperationException e) {
                    return StepResult.fail("riflessione su MouseHandler fallita: " + e);
                }
                String where = String.format(Locale.ROOT, "(%.0f,%.0f) in %s", p[0], p[1], screen.getClass().getSimpleName());
                if (!click) return StepResult.ok("mouse su " + where);
                boolean handled = screen.mouseClicked(p[0], p[1], button);
                screen.mouseReleased(p[0], p[1], button);
                Screen after = mc.screen;
                return StepResult.ok("clic " + button + " su " + where + (handled ? "" : " (non gestito)")
                        + ", dopo: " + (after == null ? "nessuna schermata" : after.getClass().getSimpleName()));
            }

            @Override
            public int expectedTicks() {
                return within;
            }
        };
    }

    /**
     * assert_screen: la schermata aperta ha nel nome di classe {@code screen} ("none" = nessuna schermata). Ripete il
     * controllo per {@code within} tick (20), perché le schermate del server arrivano con un pacchetto.
     */
    static Exec assertScreen(JsonObject s) {
        String want = Scenario.req(s, "screen");
        int within = Scenario.integer(s, "within", 20);
        return new Exec() {
            int t;

            @Override
            public StepResult poll(ScenarioRunner runner) {
                Screen screen = Minecraft.getInstance().screen;
                boolean ok = want.equals("none") ? screen == null : screen != null && screen.getClass().getName().contains(want);
                String open = screen == null ? "nessuna schermata" : screen.getClass().getName();
                if (ok) return StepResult.ok("aperta: " + open);
                if (t++ < within) return null;
                return StepResult.fail("attesa " + want + ", aperta: " + open);
            }

            @Override
            public int expectedTicks() {
                return within;
            }
        };
    }

    private static double[] point(JsonObject s, Window w) {
        if (s.has("pos")) {
            JsonArray a = s.getAsJsonArray("pos");
            return new double[]{a.get(0).getAsDouble(), a.get(1).getAsDouble()};
        }
        double dx = 0, dy = 0;
        if (s.has("offset")) {
            JsonArray a = s.getAsJsonArray("offset");
            dx = a.get(0).getAsDouble();
            dy = a.get(1).getAsDouble();
        }
        return new double[]{w.getGuiScaledWidth() / 2.0 + dx, w.getGuiScaledHeight() / 2.0 + dy};
    }
}
