package cromocon.spectralascension.dev;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ScreenshotEvent;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Esegue gli scenari un passo alla volta sul thread del client (un passo per tick, i passi lunghi restano "in corso"
 * finché non finiscono). Il lavoro lato server (comandi, controlli su advancement e inventario, costruzioni) passa per
 * il server integrato con {@code server.submit}. Gli screenshot si scattano a fine frame (RenderFrameEvent.Post).
 */
public final class ScenarioRunner {
    public static final Logger LOG = LogUtils.getLogger();
    public static final String TAG = "[SA-SCENARIO]";
    public static final String AUTO_PROPERTY = "spectral_ascension.autoScenario";
    private static final int READY_TICKS = 60;            // tick di mondo stabile prima di partire in automatico
    private static final int WORLD_TIMEOUT_TICKS = 20 * 240; // automatico: se il mondo non arriva in 4 minuti si chiude
    private static final int DEFAULT_STEP_TIMEOUT = 20 * 30;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final ScenarioRunner INSTANCE = new ScenarioRunner();

    public static ScenarioRunner get() {
        return INSTANCE;
    }

    // --- stato ---
    private final Deque<String> queue = new ArrayDeque<>();
    private final Map<KeyMapping, Boolean> forcedKeys = new LinkedHashMap<>();
    @Nullable private Scenario scenario;
    private int stepIndex;
    @Nullable private Exec exec;
    private int stepTicks;
    private int stepTimeout;
    private JsonObject stepJson = new JsonObject();
    private final List<String> stepShots = new ArrayList<>();
    @Nullable private JsonObject scenarioReport;
    @Nullable private JsonObject report;
    private boolean autoMode;
    private boolean autoStarted;
    private boolean autoDone;
    private int readyTicks;
    private int waitingTicks;
    private int stopCountdown = -1;
    private long logOffset;
    @Nullable private Shot shot;
    private boolean hudHidden;

    private final boolean autoRequested;
    private final String autoValue;

    private ScenarioRunner() {
        autoValue = System.getProperty(AUTO_PROPERTY, "").trim();
        autoRequested = !autoValue.isEmpty();
        if (autoRequested) {
            LOG.info("{} avvio automatico richiesto: {}={}", TAG, AUTO_PROPERTY, autoValue);
        }
    }

    public boolean isHudHidden() {
        return hudHidden;
    }

    public void setHudHidden(boolean hidden) {
        hudHidden = hidden;
    }

    public boolean isRunning() {
        return scenario != null || !queue.isEmpty();
    }

    @Nullable public String currentName() {
        return scenario == null ? null : scenario.name;
    }

    // ================================================================ avvio / arresto

    /** Avvia una lista di scenari (comando o automatico). Ritorna un messaggio d'errore o null. */
    @Nullable public String start(List<String> names, boolean auto) {
        if (isRunning()) return "c'è già uno scenario in corso (" + currentName() + "): usa /sa scenario stop";
        if (names.isEmpty()) return "nessuno scenario da eseguire";
        autoMode = auto;
        queue.clear();
        queue.addAll(names);
        report = new JsonObject();
        report.addProperty("started", LocalDateTime.now().toString());
        report.addProperty("mode", auto ? "auto" : "comando");
        report.addProperty("complete", false);
        report.add("scenarios", new JsonArray());
        LOG.info("{} START {} scenari: {}", TAG, names.size(), String.join(", ", names));
        nextScenario();
        return null;
    }

    public void stop(String why) {
        if (!isRunning()) return;
        LOG.info("{} STOP richiesto: {}", TAG, why);
        queue.clear();
        if (scenario != null) {
            if (scenarioReport != null) scenarioReport.addProperty("aborted", why);
            endScenario();
        }
        finishAll();
    }

    public void onLogout() {
        if (isRunning()) stop("uscita dal mondo");
    }

    // ================================================================ eventi

    public void onTickPre() {
        if (forcedKeys.isEmpty()) return;
        forcedKeys.forEach(KeyMapping::setDown); // KeyMapping.setAll() li riallinea alla tastiera vera: li si forza ogni tick
    }

    public void onTickPost() {
        Minecraft mc = Minecraft.getInstance();
        if (stopCountdown >= 0) {
            if (stopCountdown-- == 0) {
                LOG.info("{} automatico finito: chiudo il gioco", TAG);
                mc.stop();
            }
            return;
        }
        if (autoRequested && !autoStarted) {
            autoWaitForWorld(mc);
            return;
        }
        if (scenario == null) return;
        if (mc.player == null || mc.level == null) {
            stop("giocatore non presente");
            return;
        }
        try {
            if (exec == null) {
                if (stepIndex >= scenario.steps.size()) {
                    endScenario();
                    nextScenario();
                    return;
                }
                beginStep();
                if (exec == null) return;
            }
            StepResult r = exec.poll(this);
            stepTicks++;
            if (r == null && stepTicks > stepTimeout) {
                r = StepResult.fail("timeout dopo " + stepTicks + " tick");
            }
            if (r != null) finishStep(r);
        } catch (Throwable t) {
            LOG.error("{} errore interno", TAG, t);
            if (exec != null) finishStep(StepResult.fail("eccezione: " + t));
            else stop("eccezione: " + t);
        }
    }

    /** Fine del frame: il framebuffer principale contiene l'immagine completa (mondo + HUD + schermate). */
    public void onFrameEnd() {
        Shot s = shot;
        if (s == null || s.started) return;
        if (s.framesToWait-- > 0) return;
        s.started = true;
        Minecraft mc = Minecraft.getInstance();
        File target = mc.gameDirectory.toPath().resolve(Screenshot.SCREENSHOT_DIR).resolve(s.relPath).toAbsolutePath().normalize().toFile();
        target.getParentFile().mkdirs();
        target.delete();
        s.file = target;
        Screenshot.grab(mc.gameDirectory, s.relPath, mc.getMainRenderTarget(), msg -> {
            s.message = msg.getString();
            s.done = true;
        });
    }

    /** ScreenshotEvent: dà accesso all'immagine prima del salvataggio; si misura se è nera o uniforme. */
    public void onScreenshot(ScreenshotEvent e) {
        Shot s = shot;
        if (s == null || s.file == null
                || !e.getScreenshotFile().toPath().toAbsolutePath().normalize().equals(s.file.toPath())) return;
        NativeImage img = e.getImage();
        int w = img.getWidth(), h = img.getHeight();
        double sum = 0;
        int n = 0;
        Set<Integer> colors = new HashSet<>();
        for (int y = 0; y < h; y += Math.max(1, h / 64)) {
            for (int x = 0; x < w; x += Math.max(1, w / 64)) {
                int abgr = img.getPixelRGBA(x, y);
                int r = abgr & 0xFF, g = (abgr >> 8) & 0xFF, b = (abgr >> 16) & 0xFF;
                sum += (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255.0;
                colors.add(abgr & 0xFFFFFF);
                n++;
            }
        }
        s.luma = n == 0 ? 0 : sum / n;
        s.colors = colors.size();
        s.width = w;
        s.height = h;
    }

    // ================================================================ automatico

    private void autoWaitForWorld(Minecraft mc) {
        waitingTicks++;
        boolean inWorld = mc.player != null && mc.level != null && mc.getSingleplayerServer() != null;
        if (!inWorld) {
            readyTicks = 0;
            if (waitingTicks % 200 == 0) {
                LOG.info("{} in attesa del mondo... schermata: {}", TAG, mc.screen == null ? "nessuna" : mc.screen.getClass().getName());
            }
            if (waitingTicks > WORLD_TIMEOUT_TICKS) {
                LOG.error("{} il mondo non si è caricato in tempo (schermata: {}): chiudo", TAG,
                        mc.screen == null ? "nessuna" : mc.screen.getClass().getName());
                autoStarted = true;
                report = new JsonObject();
                report.addProperty("started", LocalDateTime.now().toString());
                report.addProperty("mode", "auto");
                report.addProperty("error", "mondo non caricato; schermata " + (mc.screen == null ? "nessuna" : mc.screen.getClass().getName()));
                report.add("scenarios", new JsonArray());
                finishAll();
            }
            return;
        }
        Screen screen = mc.screen;
        if (screen instanceof ReceivingLevelScreen || screen instanceof LevelLoadingScreen || screen instanceof GenericMessageScreen) {
            readyTicks = 0; // schermate di caricamento: si chiudono da sole
            return;
        }
        if (screen != null) {
            // Un menu (pausa o altro) aperto all'ingresso bloccherebbe le inquadrature: lo si chiude e lo si registra.
            LOG.info("{} schermata aperta all'ingresso nel mondo ({}): la chiudo", TAG, screen.getClass().getName());
            mc.setScreen(null);
            readyTicks = 0;
            return;
        }
        if (++readyTicks < READY_TICKS) return;
        autoStarted = true;
        List<String> names = new ArrayList<>();
        if (autoValue.equalsIgnoreCase("all")) {
            names.addAll(ScenarioFiles.list().keySet());
        } else {
            for (String n : autoValue.split("[,;\\s]+")) if (!n.isBlank()) names.add(n.trim());
        }
        String err = start(names, true);
        if (err != null) {
            LOG.error("{} {}", TAG, err);
            report = new JsonObject();
            report.addProperty("started", LocalDateTime.now().toString());
            report.addProperty("mode", "auto");
            report.addProperty("error", err);
            report.add("scenarios", new JsonArray());
            finishAll();
        }
    }

    // ================================================================ scenari e passi

    private void nextScenario() {
        scenario = null;
        String name = queue.poll();
        if (name == null) {
            finishAll();
            return;
        }
        scenarioReport = new JsonObject();
        scenarioReport.addProperty("name", name);
        scenarioReport.add("steps", new JsonArray());
        try {
            scenario = Scenario.load(name);
        } catch (Exception e) {
            LOG.error("{} {} LOAD FAIL {}", TAG, name, e.getMessage());
            scenarioReport.addProperty("ok", false);
            scenarioReport.addProperty("error", "impossibile caricare: " + e.getMessage());
            report.getAsJsonArray("scenarios").add(scenarioReport);
            writeReport(false);
            nextScenario();
            return;
        }
        scenarioReport.addProperty("file", scenario.file.toString());
        scenarioReport.addProperty("description", scenario.description);
        stepIndex = 0;
        exec = null;
        logOffset = LogCapture.mark();
        clearShots(scenario.name);
        resetClientState();
        LOG.info("{} {} BEGIN ({} passi, file {})", TAG, scenario.name, scenario.steps.size(), scenario.file);
        chat("Scenario " + scenario.name + " avviato (" + scenario.steps.size() + " passi)");
    }

    private void beginStep() {
        stepJson = scenario.steps.get(stepIndex);
        stepTicks = 0;
        stepShots.clear();
        String action = Scenario.str(stepJson, "action", "?");
        try {
            exec = ScenarioActions.create(action, stepJson, this);
            stepTimeout = Scenario.integer(stepJson, "timeout", Math.max(DEFAULT_STEP_TIMEOUT, exec.expectedTicks() + 200));
        } catch (Exception e) {
            exec = null;
            recordStep(action, StepResult.fail(e.getMessage() == null ? e.toString() : e.getMessage()));
        }
    }

    private void finishStep(StepResult r) {
        String action = Scenario.str(stepJson, "action", "?");
        exec = null;
        shot = null;
        recordStep(action, r);
    }

    private void recordStep(String action, StepResult r) {
        int n = stepIndex + 1;
        JsonObject s = new JsonObject();
        s.addProperty("n", n);
        s.addProperty("action", action);
        s.addProperty("ok", r.ok());
        s.addProperty("detail", r.detail());
        s.addProperty("ticks", stepTicks);
        JsonArray shots = new JsonArray();
        stepShots.forEach(shots::add);
        s.add("screenshots", shots);
        s.add("step", stepJson);
        scenarioReport.getAsJsonArray("steps").add(s);
        String line = String.format(Locale.ROOT, "%s %s #%d %s %s %s", TAG, scenario.name, n, action, r.ok() ? "OK" : "FAIL", r.detail());
        if (r.ok()) LOG.info(line);
        else LOG.warn(line);
        stepIndex++;
        boolean stopOnFail = Scenario.bool(stepJson, "stop_on_fail", scenario.stopOnFail);
        if (!r.ok() && stopOnFail) {
            scenarioReport.addProperty("aborted", "passo " + n + " fallito con stop_on_fail");
            stepIndex = scenario.steps.size();
        }
    }

    private void endScenario() {
        if (scenario == null || scenarioReport == null) return;
        exec = null;
        shot = null;
        int ok = 0, fail = 0;
        for (JsonElement e : scenarioReport.getAsJsonArray("steps")) {
            if (e.getAsJsonObject().get("ok").getAsBoolean()) ok++;
            else fail++;
        }
        int skipped = scenario.steps.size() - ok - fail;
        scenarioReport.addProperty("steps_ok", ok);
        scenarioReport.addProperty("steps_fail", fail);
        scenarioReport.addProperty("steps_skipped", skipped);
        scenarioReport.addProperty("ok", fail == 0 && skipped == 0);
        report.getAsJsonArray("scenarios").add(scenarioReport);
        LOG.info("{} {} END {} ({} OK, {} FAIL, {} saltati)", TAG, scenario.name, fail == 0 && skipped == 0 ? "OK" : "FAIL", ok, fail, skipped);
        chat("Scenario " + scenario.name + ": " + ok + " OK, " + fail + " FAIL" + (skipped > 0 ? ", " + skipped + " saltati" : ""));
        resetClientState();
        scenario = null;
        scenarioReport = null;
        writeReport(false);
    }

    private void finishAll() {
        scenario = null;
        exec = null;
        Path p = writeReport(true);
        report = null;
        if (p != null) chat("Report: " + p);
        if (autoMode || autoRequested && !autoDone) {
            autoDone = true;
            stopCountdown = 20; // un secondo per scrivere il log, poi chiude il gioco
        }
        autoMode = false;
    }

    /** Riporta il client a uno stato neutro: niente tasti forzati, nessuna schermata, HUD visibile, prima persona. */
    private void resetClientState() {
        Minecraft mc = Minecraft.getInstance();
        forcedKeys.keySet().forEach(k -> k.setDown(false));
        forcedKeys.clear();
        if (mc.screen != null) mc.setScreen(null);
        mc.options.hideGui = false;
        hudHidden = false;
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        ScenarioActions.resetFov();
    }

    /** Cancella gli screenshot della corsa precedente dello stesso scenario (screenshots/scenari/&lt;nome&gt;/). */
    private static void clearShots(String name) {
        Path dir = Minecraft.getInstance().gameDirectory.toPath().resolve(Screenshot.SCREENSHOT_DIR).resolve("scenari").resolve(name);
        if (!Files.isDirectory(dir)) return;
        try (var files = Files.list(dir)) {
            files.filter(p -> p.toString().endsWith(".png")).forEach(p -> p.toFile().delete());
        } catch (IOException e) {
            LOG.warn("{} impossibile svuotare {}", TAG, dir);
        }
    }

    @Nullable private Path writeReport(boolean complete) {
        if (report == null) return null;
        try {
            report.addProperty("complete", complete);
            report.addProperty("updated", LocalDateTime.now().toString());
            int ok = 0, fail = 0, scen = 0, scenOk = 0;
            for (JsonElement e : report.getAsJsonArray("scenarios")) {
                JsonObject so = e.getAsJsonObject();
                scen++;
                if (so.has("ok") && so.get("ok").getAsBoolean()) scenOk++;
                if (so.has("steps_ok")) ok += so.get("steps_ok").getAsInt();
                if (so.has("steps_fail")) fail += so.get("steps_fail").getAsInt();
            }
            JsonObject sum = new JsonObject();
            sum.addProperty("scenarios", scen);
            sum.addProperty("scenarios_ok", scenOk);
            sum.addProperty("steps_ok", ok);
            sum.addProperty("steps_fail", fail);
            report.add("summary", sum);
            Path dir = Minecraft.getInstance().gameDirectory.toPath().resolve("scenari-report").toAbsolutePath().normalize();
            Files.createDirectories(dir);
            if (!report.has("file")) {
                String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
                report.addProperty("file", dir.resolve(ts + ".json").toAbsolutePath().toString());
            }
            Path file = Path.of(report.get("file").getAsString());
            String json = GSON.toJson(report);
            Files.writeString(file, json, StandardCharsets.UTF_8);
            Files.writeString(dir.resolve("latest.json"), json, StandardCharsets.UTF_8);
            if (complete) LOG.info("{} REPORT {} ({} passi OK, {} FAIL)", TAG, file, ok, fail);
            return file;
        } catch (IOException e) {
            LOG.error("{} impossibile scrivere il report", TAG, e);
            return null;
        }
    }

    // ================================================================ servizi per le azioni

    public String scenarioName() {
        return scenario == null ? "?" : scenario.name;
    }

    public int stepNumber() {
        return stepIndex + 1;
    }

    public long logOffset() {
        return logOffset;
    }

    public void forceKey(KeyMapping key, boolean down) {
        if (down) forcedKeys.put(key, true);
        else forcedKeys.remove(key);
        key.setDown(down);
    }

    /** Chiede uno screenshot a fine frame; il file va in screenshots/scenari/&lt;scenario&gt;/&lt;NN&gt;_&lt;nome&gt;.png. */
    public Shot requestShot(String name, int frames) {
        String safe = name.replaceAll("[^A-Za-z0-9_.-]", "_");
        String rel = "scenari/" + scenarioName() + "/" + String.format(Locale.ROOT, "%02d_%s.png", stepNumber(), safe);
        shot = new Shot(rel, Math.max(1, frames));
        return shot;
    }

    public void addShot(String path) {
        stepShots.removeIf(path::equals);
        stepShots.add(path);
    }

    static void chat(String msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.displayClientMessage(Component.literal("[SA] " + msg), false);
    }

    /** Uno screenshot richiesto: lo scatta onFrameEnd, onScreenshot misura luminosità e colori. */
    public static final class Shot {
        final String relPath;
        int framesToWait;
        volatile boolean started;
        volatile boolean done;
        volatile String message = "";
        @Nullable volatile File file;
        volatile double luma = -1;
        volatile int colors = -1;
        volatile int width, height;

        Shot(String relPath, int frames) {
            this.relPath = relPath;
            this.framesToWait = frames;
        }

        /** null finché il file non è scritto. */
        @Nullable StepResult result() {
            if (!done) return null;
            File f = file;
            boolean ok = f != null && f.isFile() && f.length() > 0;
            String stats = String.format(Locale.ROOT, "%dx%d luma=%.3f colori=%d", width, height, luma, colors);
            if (!ok) return StepResult.fail("screenshot non salvato: " + message);
            if (colors >= 0 && colors <= 2) return StepResult.fail("immagine uniforme (nera/vuota?) " + f + " " + stats);
            return StepResult.ok(f.getAbsolutePath() + " " + stats);
        }
    }
}
