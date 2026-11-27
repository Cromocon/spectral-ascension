package cromocon.spectralascension.dev;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.klikli_dev.modonomicon.api.ModonomiconAPI;
import com.klikli_dev.modonomicon.api.multiblock.Multiblock;
import com.klikli_dev.modonomicon.book.Book;
import com.klikli_dev.modonomicon.book.conditions.context.BookConditionContext;
import com.klikli_dev.modonomicon.book.entries.BookEntry;
import com.klikli_dev.modonomicon.bookstate.BookUnlockStateManager;
import com.klikli_dev.modonomicon.client.gui.BookGuiManager;
import com.klikli_dev.modonomicon.client.gui.book.BookAddress;
import com.klikli_dev.modonomicon.data.BookDataManager;
import com.mojang.brigadier.StringReader;
import de.dafuqs.revelationary.ClientRevelationHolder;
import de.dafuqs.revelationary.api.advancements.AdvancementHelper;
import de.dafuqs.spectrum.api.recipe.GatedRecipe;
import de.dafuqs.spectrum.recipe.pedestal.PedestalRecipe;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.commands.arguments.item.ItemPredicateArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.regex.Pattern;

/** Le azioni degli scenari. Ogni azione diventa un {@link Exec}; vedi tools/SCENARI.md per il formato. */
public final class ScenarioActions {
    private ScenarioActions() {
    }

    static Exec create(String action, JsonObject s, ScenarioRunner runner) throws Exception {
        return switch (action) {
            case "command" -> command(s);
            case "wait" -> wait(s);
            case "screenshot" -> screenshot(s);
            case "screenshot_series" -> screenshotSeries(s);
            case "hold" -> hold(s);
            case "look" -> look(s);
            case "hide_hud" -> client(() -> {
                // value: nasconde l'HUD (hotbar, mirino, chat) lasciando visibile la mano;
                // hand=false: nasconde anche la mano (come F1, options.hideGui)
                boolean on = Scenario.bool(s, "value", true);
                boolean hand = Scenario.bool(s, "hand", true);
                runner.setHudHidden(on);
                Minecraft.getInstance().options.hideGui = on && !hand;
                return StepResult.ok("hud nascosto=" + on + ", mano visibile=" + !(on && !hand));
            });
            case "first_person" -> camera(CameraType.FIRST_PERSON);
            case "third_person" -> camera(Scenario.str(s, "view", "back").equals("front") ? CameraType.THIRD_PERSON_FRONT : CameraType.THIRD_PERSON_BACK);
            case "open_screen" -> openScreen(s);
            case "jei_dump" -> client(() -> {
                try {
                    String r = JeiDev.dumpFusion(Scenario.str(s, "modid", "spectral_ascension"), true);
                    return StepResult.ok(r);
                } catch (RuntimeException | LinkageError e) {
                    return StepResult.fail(String.valueOf(e));
                }
            });
            case "close_screen" -> client(() -> {
                Minecraft.getInstance().setScreen(null);
                return StepResult.ok("schermata chiusa");
            });
            case "gui_click" -> GuiClick.click(s);
            case "assert_screen" -> GuiClick.assertScreen(s);
            case "use_item" -> useItem(s, runner);
            case "use_on_block" -> useOnBlock(s);
            case "sneak" -> sneak(s, runner);
            case "move" -> move(s, runner);
            case "release_use" -> client(() -> {
                Minecraft mc = Minecraft.getInstance();
                runner.forceKey(mc.options.keyUse, false); // il client rilascia l'uso al tick successivo
                return StepResult.ok("tasto destro rilasciato");
            });
            case "fly" -> {
                // Volo creativo (per restare in aria dopo un tp e inquadrare dall'alto). Il client lo spegne da solo
                // se il giocatore tocca terra: va usato dopo il tp; si riapplica per qualche tick.
                boolean on = Scenario.bool(s, "value", true);
                yield new Exec() {
                    int t;

                    @Override
                    public StepResult poll(ScenarioRunner r) {
                        LocalPlayer p = Minecraft.getInstance().player;
                        if (on && !p.getAbilities().mayfly) return StepResult.fail("il volo richiede la modalità creativa");
                        if (p.getAbilities().flying != on) {
                            p.getAbilities().flying = on;
                            p.onUpdateAbilities(); // manda lo stato al server
                        }
                        if (t++ < 3) return null;
                        return StepResult.of(p.getAbilities().flying == on, "volo=" + p.getAbilities().flying);
                    }
                };
            }
            case "assert_advancement" -> assertAdvancement(s);
            case "assert_item" -> assertItem(s);
            case "assert_block" -> assertBlock(s);
            case "assert_multiblock" -> assertMultiblock(s);
            case "assert_recipe_unlocked" -> assertRecipe(s);
            case "assert_book_entry" -> assertBookEntry(s);
            case "assert_revealed" -> assertRevealed(s);
            case "assert_log" -> assertLog(s, runner);
            case "build" -> build(s);
            case "reset_book" -> {
                ResourceLocation bookId = rl(Scenario.str(s, "book", "spectrum:guidebook"));
                // Due fasi distanziate: il messaggio di sync di Modonomicon tiene un riferimento allo stato vivo e viene
                // codificato in un altro thread; aggiornarlo subito dopo il reset fa fallire la codifica (NPE) e
                // disconnette il giocatore.
                yield onServer((server, sp) -> {
                    Book b = BookDataManager.get().getBook(bookId);
                    if (b == null) return StepResult.fail("libro inesistente: " + bookId);
                    BookUnlockStateManager.get().resetFor(sp, b); // dimentica sblocchi e letture (e sincronizza)
                    return StepResult.ok("");
                }).then(r -> Exec.delay(10, () -> StepResult.ok(""))
                        .then(r2 -> onServer((server, sp) -> {
                            BookUnlockStateManager.get().updateAndSyncFor(sp); // ricalcola dagli advancement attuali
                            return StepResult.ok("sblocchi di " + bookId + " ricalcolati da zero");
                        }), false), false);
            }
            case "client_command" -> client(() -> {
                // comando lato client (es. /sa scenario list), come se lo scrivesse il giocatore in chat
                String c = Scenario.req(s, "command");
                Minecraft.getInstance().player.connection.sendCommand(c.startsWith("/") ? c.substring(1) : c);
                return StepResult.ok("/" + c + " inviato (l'esito va in chat e nel log)");
            });
            case "log" -> client(() -> StepResult.ok(Scenario.str(s, "text", "")));
            case "clear_chat" -> client(() -> {
                // i messaggi di chat coprono il testo sopra la hotbar e il basso delle schermate
                Minecraft.getInstance().gui.getChat().clearMessages(false);
                return StepResult.ok("chat svuotata");
            });
            case "book_focus" -> BookFocus.create(s);
            case "buy_hint" -> buyHint(s);
            case "track_projectile" -> trackProjectile(s);
            case "fov" -> client(() -> setFov(s));
            case "mine" -> MineActions.mine(s);
            case "count_blocks" -> MineActions.countBlocks(s);
            case "area_prediction" -> client(() -> {
                cromocon.spectralascension.client.WorkstaffAreaPrediction.enabled = Scenario.bool(s, "value", true);
                return StepResult.ok("previsione dell'area della workstaff sul client: " + (cromocon.spectralascension.client.WorkstaffAreaPrediction.enabled ? "on" : "off"));
            });
            default -> throw new IllegalArgumentException("azione sconosciuta: " + action);
        };
    }

    // ================================================================ campo visivo (fov)

    /** FOV e "effetti FOV" originali, salvati alla prima azione fov dello scenario; null = niente da ripristinare. */
    private static Integer savedFov;
    private static Double savedFovEffect;

    /**
     * fov: imposta il campo visivo (gradi, 30-110) e, con {@code effects} (default 0), la scala degli effetti FOV
     * (volo, scatto). Serve alle inquadrature "da foto prodotto": FOV stretto da lontano, fuori dalla portata del
     * giocatore (niente contorno del blocco mirato). Non si salva su options.txt: resetFov() lo rimette com'era a
     * fine scenario.
     */
    private static StepResult setFov(JsonObject s) {
        Minecraft mc = Minecraft.getInstance();
        int fov = Math.max(30, Math.min(110, Scenario.integer(s, "value", 70)));
        double eff = s.has("effects") ? s.get("effects").getAsDouble() : 0.0;
        if (savedFov == null) {
            savedFov = mc.options.fov().get();
            savedFovEffect = mc.options.fovEffectScale().get();
        }
        mc.options.fov().set(fov);
        mc.options.fovEffectScale().set(Math.max(0.0, Math.min(1.0, eff)));
        return StepResult.ok("fov=" + fov + " effetti=" + mc.options.fovEffectScale().get() + " (originale " + savedFov + ")");
    }

    /** Rimette il FOV di prima della prima azione fov (chiamato da ScenarioRunner.resetClientState). */
    static void resetFov() {
        if (savedFov == null) return;
        Minecraft mc = Minecraft.getInstance();
        mc.options.fov().set(savedFov);
        mc.options.fovEffectScale().set(savedFovEffect);
        savedFov = null;
        savedFovEffect = null;
    }

    // ================================================================ voci-indizio (spectrum:hint)

    /**
     * buy_hint: preme il pulsante «Uncover Insight» di una pagina spectrum:hint della voce aperta (come il clic del
     * giocatore: BookHintPageRenderer.paymentButtonClicked, che controlla l'inventario e manda il pacchetto al server).
     * {@code index} = quale pagina hint fra quelle visibili (0 = sinistra, 1 = destra). Per riflessione: i renderer
     * stanno in campi di BookEntryScreen o delle sue sottoclassi, e il metodo è protected.
     */
    private static Exec buyHint(JsonObject s) {
        int index = Scenario.integer(s, "index", 0);
        int survivalTimeout = Scenario.integer(s, "survival_timeout", 100);
        return new Exec() {
            int waited = 0;

            @Override
            public int expectedTicks() {
                return survivalTimeout;
            }

            @Override
            public StepResult poll(ScenarioRunner runner) {
                var mc = Minecraft.getInstance();
                // paymentButtonClicked paga gratis se il CLIENT è creativo: prima si aspetta che il gamemode
                // sincronizzato arrivi (il comando gamemode gira sul server, il client lo sa qualche tick dopo)
                var gm = mc.gameMode == null ? null : mc.gameMode.getPlayerMode();
                boolean survival = mc.player != null && !mc.player.isCreative()
                        && gm == net.minecraft.world.level.GameType.SURVIVAL;
                if (!survival) {
                    if (waited++ < survivalTimeout) return null;
                    return StepResult.fail("il client è ancora creativo/non survival dopo " + survivalTimeout
                            + " tick (isCreative=" + (mc.player != null && mc.player.isCreative()) + ", gamemode client=" + gm
                            + "): il pulsante pagherebbe gratis, acquisto non valido");
                }
                return click();
            }

            private StepResult click() {
                var mc = Minecraft.getInstance();
                var screen = mc.screen;
                if (screen == null) return StepResult.fail("nessuna schermata aperta (serve open_screen guidebook con la voce)");
                java.util.List<Object> found = new java.util.ArrayList<>();
                try {
                    for (Class<?> c = screen.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
                        for (var f : c.getDeclaredFields()) {
                            if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                            f.setAccessible(true);
                            Object v = f.get(screen);
                            if (v != null && v.getClass().getSimpleName().equals("BookHintPageRenderer")) found.add(v);
                        }
                    }
                    if (index >= found.size()) return StepResult.fail("pagine hint trovate sulla schermata: " + found.size());
                    Object r = found.get(index);
                    // costo della pagina: renderer.page (campo di BookPageRenderer) -> getCost()
                    de.dafuqs.spectrum.api.recipe.IngredientStack cost = null;
                    for (Class<?> c = r.getClass(); c != null && cost == null; c = c.getSuperclass()) {
                        try {
                            var pf = c.getDeclaredField("page");
                            pf.setAccessible(true);
                            if (pf.get(r) instanceof de.dafuqs.spectrum.compat.modonomicon.pages.BookHintPage hp) cost = hp.getCost();
                        } catch (NoSuchFieldException ignored) {
                        }
                    }
                    if (cost == null || cost.isEmpty() || cost.getCount() <= 0)
                        return StepResult.fail("il costo letto dalla pagina hint è VUOTO (count="
                                + (cost == null ? "null" : cost.getCount()) + "): il pagamento sarebbe gratis. "
                                + "BookHintPage.fromJson legge la chiave \"amount\", non \"cost\"");
                    int before = countCost(cost);
                    var m = r.getClass().getDeclaredMethod("paymentButtonClicked", net.minecraft.client.gui.components.Button.class);
                    m.setAccessible(true);
                    m.invoke(r, (Object) null);
                    var rp = r.getClass().getDeclaredField("revealProgress");
                    rp.setAccessible(true);
                    int after = countCost(cost);
                    return StepResult.ok("pulsante premuto sulla pagina hint " + index + ", revealProgress=" + rp.getInt(r)
                            + " (-1 = non pagata: oggetti mancanti); costo=" + cost.getCount()
                            + ", oggetti del costo nell'inventario CLIENT prima=" + before + " dopo=" + after
                            + " (il server toglie gli oggetti dopo il pacchetto: controllare con assert_item)");
                } catch (ReflectiveOperationException e) {
                    return StepResult.fail("riflessione su BookHintPageRenderer fallita: " + e);
                }
            }
        };
    }

    /** Oggetti dell'inventario client che soddisfano l'ingrediente del costo (somma delle quantità). */
    private static int countCost(de.dafuqs.spectrum.api.recipe.IngredientStack cost) {
        var player = Minecraft.getInstance().player;
        if (player == null) return -1;
        int n = 0;
        for (ItemStack st : player.getInventory().items) if (!st.isEmpty() && cost.getIngredient().test(st)) n += st.getCount();
        for (ItemStack st : player.getInventory().offhand) if (!st.isEmpty() && cost.getIngredient().test(st)) n += st.getCount();
        return n;
    }

    // ================================================================ proiettili

    /**
     * track_projectile: spara con l'oggetto in mano (clic destro, come use_item: una balestra carica) e segue per
     * {@code ticks} tick la freccia appena comparsa, sul CLIENT e sul server integrato (stessa entità, stesso id).
     * Per ogni tick scrive posizione e velocità dei due lati e l'angolo fra le due velocità: è quello che si vede
     * come «la freccia non va dritta» quando il client ha una velocità diversa da quella vera. Con {@code max_angle}
     * (gradi) il passo fallisce se l'angolo lo supera, dal tick {@code from} (1) in poi.
     */
    private static Exec trackProjectile(JsonObject s) {
        int ticks = Scenario.integer(s, "ticks", 8);
        int from = Scenario.integer(s, "from", 1);
        double maxAngle = Scenario.num(s, "max_angle", -1);
        Minecraft mc = Minecraft.getInstance();
        return new Exec() {
            final java.util.Set<Integer> before = new java.util.HashSet<>();
            final List<String> lines = new ArrayList<>();
            net.minecraft.world.entity.projectile.AbstractArrow clientArrow;
            Entity serverArrow;
            int waited = -1;
            int sampled = 0;
            double worst = 0;

            @Override
            public int expectedTicks() {
                return ticks + 20;
            }

            @Override
            public StepResult poll(ScenarioRunner runner) {
                LocalPlayer p = mc.player;
                if (waited < 0) {
                    for (Entity e : arrowsNear(p)) before.add(e.getId());
                    waited = 0;
                    InteractionResult res = mc.gameMode.useItem(p, InteractionHand.MAIN_HAND);
                    lines.add("tiro con " + BuiltInRegistries.ITEM.getKey(p.getMainHandItem().getItem()) + ": " + res);
                    return null;
                }
                if (clientArrow == null) {
                    clientArrow = arrowsNear(p).stream().filter(e -> !before.contains(e.getId()))
                            .min(Comparator.comparingDouble(e -> e.distanceToSqr(p))).orElse(null);
                    if (clientArrow == null) {
                        return ++waited > 20 ? StepResult.fail(String.join(" | ", lines) + " | nessuna freccia comparsa sul client") : null;
                    }
                    MinecraftServer server = mc.getSingleplayerServer();
                    ServerLevel level = server == null ? null : server.getLevel(p.level().dimension());
                    serverArrow = level == null ? null : level.getEntity(clientArrow.getId());
                    if (serverArrow == null) return StepResult.fail("freccia " + clientArrow.getId() + " non trovata sul server");
                }
                Vec3 cv = clientArrow.getDeltaMovement(), sv = serverArrow.getDeltaMovement();
                Vec3 cp = clientArrow.position(), sp = serverArrow.position();
                double angle = cv.lengthSqr() < 1e-8 || sv.lengthSqr() < 1e-8 ? 0
                        : Math.toDegrees(Math.acos(Mth.clamp(cv.dot(sv) / (cv.length() * sv.length()), -1.0, 1.0)));
                if (sampled >= from) worst = Math.max(worst, angle);
                lines.add(String.format(Locale.ROOT, "t%d client pos (%.2f, %.2f, %.2f) vel (%.3f, %.3f, %.3f) |v| %.3f; server pos (%.2f, %.2f, %.2f) "
                                + "vel (%.3f, %.3f, %.3f) |v| %.3f; angolo %.2f°, distanza %.2f",
                        sampled, cp.x, cp.y, cp.z, cv.x, cv.y, cv.z, cv.length(), sp.x, sp.y, sp.z, sv.x, sv.y, sv.z, sv.length(),
                        angle, cp.distanceTo(sp)));
                boolean stopped = clientArrow.isRemoved() || serverArrow.isRemoved() || sv.lengthSqr() < 1e-6;
                if (++sampled <= ticks && !stopped) return null;
                String detail = String.join(" | ", lines) + String.format(Locale.ROOT, " | angolo massimo client/server %.2f°", worst);
                return StepResult.of(maxAngle < 0 || worst <= maxAngle, detail + (maxAngle < 0 ? "" : " (limite " + maxAngle + "°)"));
            }
        };
    }

    private static List<net.minecraft.world.entity.projectile.AbstractArrow> arrowsNear(LocalPlayer p) {
        return p.level().getEntitiesOfClass(net.minecraft.world.entity.projectile.AbstractArrow.class, p.getBoundingBox().inflate(16));
    }

    // ================================================================ utilità

    private static Exec client(java.util.function.Supplier<StepResult> f) {
        return runner -> f.get();
    }

    /** Lavoro sul thread del server integrato, con il ServerPlayer corrispondente al giocatore del client. */
    static Exec onServer(BiFunction<MinecraftServer, ServerPlayer, StepResult> f) {
        Minecraft mc = Minecraft.getInstance();
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null || mc.player == null) return Exec.done(StepResult.fail("serve un mondo in singleplayer (server integrato)"));
        UUID id = mc.player.getUUID();
        CompletableFuture<StepResult> fut = server.submit(() -> {
            ServerPlayer sp = server.getPlayerList().getPlayer(id);
            if (sp == null) return StepResult.fail("giocatore non trovato sul server");
            try {
                return f.apply(server, sp);
            } catch (Exception e) {
                return StepResult.fail("eccezione sul server: " + e);
            }
        });
        return Exec.future(fut);
    }

    private static ResourceLocation rl(String s) {
        ResourceLocation r = ResourceLocation.tryParse(s);
        if (r == null) throw new IllegalArgumentException("id non valido: " + s);
        return r;
    }

    // ================================================================ comandi

    /** Esegue un comando (senza "/") come il giocatore con permessi op, raccogliendo i messaggi di risposta. */
    static StepResult runCommand(MinecraftServer server, ServerPlayer sp, String cmd, boolean asServer) {
        List<String> out = new ArrayList<>();
        boolean[] failed = {false};
        boolean[] cb = {false, false};
        int[] result = {0};
        CommandSource capture = new CommandSource() {
            @Override
            public void sendSystemMessage(Component c) {
                TextColor red = TextColor.fromLegacyFormat(ChatFormatting.RED);
                if (red != null && red.equals(c.getStyle().getColor())) failed[0] = true;
                out.add(c.getString());
            }

            @Override
            public boolean acceptsSuccess() {
                return true;
            }

            @Override
            public boolean acceptsFailure() {
                return true;
            }

            @Override
            public boolean shouldInformAdmins() {
                return false;
            }
        };
        CommandSourceStack base = asServer ? server.createCommandSourceStack() : sp.createCommandSourceStack();
        CommandSourceStack src = base.withSource(capture).withPermission(4).withCallback((success, res) -> {
            cb[0] = true;
            cb[1] = success;
            result[0] = res;
        });
        String c = cmd.startsWith("/") ? cmd.substring(1) : cmd;
        server.getCommands().performPrefixedCommand(src, c);
        boolean ok = !failed[0] && (!cb[0] || cb[1]);
        String msg = String.join(" | ", out);
        return StepResult.of(ok, "/" + c + " → " + (cb[0] ? "risultato " + result[0] : "nessun risultato") + (msg.isEmpty() ? "" : ": " + msg));
    }

    private static Exec command(JsonObject s) {
        List<String> cmds = new ArrayList<>();
        JsonElement e = s.get("command");
        if (e == null) e = s.get("commands");
        if (e == null) throw new IllegalArgumentException("manca \"command\"");
        if (e.isJsonArray()) e.getAsJsonArray().forEach(x -> cmds.add(x.getAsString()));
        else cmds.add(e.getAsString());
        boolean asServer = Scenario.str(s, "as", "player").equals("server");
        String expect = Scenario.str(s, "expect", "success"); // success | fail | any
        return onServer((server, sp) -> {
            List<String> details = new ArrayList<>();
            boolean allOk = true;
            for (String c : cmds) {
                StepResult r = runCommand(server, sp, c, asServer);
                details.add(r.detail());
                allOk &= r.ok();
            }
            boolean ok = switch (expect) {
                case "fail" -> !allOk;
                case "any" -> true;
                default -> allOk;
            };
            return StepResult.of(ok, String.join(" ;; ", details));
        });
    }

    // ================================================================ attesa, screenshot, inquadratura

    private static Exec wait(JsonObject s) {
        int ticks = Scenario.integer(s, "ticks", 20);
        return Exec.delay(ticks, () -> StepResult.ok(ticks + " tick"));
    }

    private static Exec screenshot(JsonObject s) {
        String name = Scenario.str(s, "name", "shot");
        int frames = Scenario.integer(s, "frames", 3);
        boolean toasts = Scenario.bool(s, "toasts", false);
        boolean chat = Scenario.bool(s, "chat", false);
        return new Exec() {
            ScenarioRunner.Shot shot;

            @Override
            public StepResult poll(ScenarioRunner runner) {
                if (shot == null) {
                    if (!toasts) Minecraft.getInstance().getToasts().clear(); // niente "Advancement Made!" sopra l'inquadratura
                    if (!chat) Minecraft.getInstance().gui.getChat().clearMessages(false); // niente chat sopra la hotbar
                    shot = runner.requestShot(name, frames);
                }
                StepResult r = shot.result();
                if (r != null && shot.file != null) runner.addShot(shot.file.getAbsolutePath());
                return r;
            }
        };
    }

    /** N screenshot a distanza di K tick: per controllare le texture animate. */
    private static Exec screenshotSeries(JsonObject s) {
        String name = Scenario.str(s, "name", "serie");
        int count = Scenario.integer(s, "count", 6);
        int every = Math.max(1, Scenario.integer(s, "every", 2));
        return new Exec() {
            int taken = 0;
            int waitLeft = 0;
            ScenarioRunner.Shot shot;
            final List<String> stats = new ArrayList<>();
            boolean failed;

            @Override
            public StepResult poll(ScenarioRunner runner) {
                if (waitLeft > 0) {
                    waitLeft--;
                    return null;
                }
                if (shot == null) {
                    if (taken == 0) {
                    Minecraft.getInstance().getToasts().clear();
                    if (!Scenario.bool(s, "chat", false)) Minecraft.getInstance().gui.getChat().clearMessages(false);
                }
                    shot = runner.requestShot(String.format(Locale.ROOT, "%s_%02d", name, taken + 1), 1);
                }
                StepResult r = shot.result();
                if (r == null) return null;
                if (shot.file != null && shot.file.isFile()) runner.addShot(shot.file.getAbsolutePath());
                failed |= !r.ok();
                stats.add(String.format(Locale.ROOT, "%d:luma=%.3f", taken + 1, shot.luma));
                shot = null;
                taken++;
                if (taken >= count) return StepResult.of(!failed, count + " screenshot ogni " + every + " tick " + String.join(" ", stats));
                waitLeft = every - 1;
                return null;
            }

            @Override
            public int expectedTicks() {
                return count * (every + 5);
            }
        };
    }

    private static Exec camera(CameraType type) {
        return client(() -> {
            Minecraft.getInstance().options.setCameraType(type);
            return StepResult.ok("camera " + type);
        });
    }

    private static Exec hold(JsonObject s) {
        int slot = Scenario.integer(s, "slot", -1);
        String item = Scenario.str(s, "item", null);
        String hand = Scenario.str(s, "hand", "main");
        Minecraft mc = Minecraft.getInstance();
        int target = slot >= 0 ? slot : mc.player.getInventory().selected;
        if (target > 8) throw new IllegalArgumentException("slot della hotbar 0..8");
        Exec give = item == null ? Exec.done(StepResult.ok("")) : onServer((server, sp) -> runCommand(server, sp,
                "item replace entity @s " + (hand.equals("off") ? "weapon.offhand" : "hotbar." + target) + " with " + item, false));
        return give.then(r -> {
            mc.player.getInventory().selected = target; // MultiPlayerGameMode invia lo slot al server nel tick
            return Exec.delay(2, () -> {
                ItemStack held = hand.equals("off") ? mc.player.getOffhandItem() : mc.player.getMainHandItem();
                String id = BuiltInRegistries.ITEM.getKey(held.getItem()).toString();
                boolean ok = item == null || item.startsWith(id);
                return StepResult.of(ok, "slot " + target + ", in mano: " + id + (r.detail().isEmpty() ? "" : " (" + r.detail() + ")"));
            });
        }, false);
    }

    private static Exec look(JsonObject s) {
        return client(() -> {
            LocalPlayer p = Minecraft.getInstance().player;
            float yaw, pitch;
            if (s.has("at")) {
                Vec3 eye = p.getEyePosition();
                JsonArray a = s.getAsJsonArray("at");
                Vec3 t = new Vec3(a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble());
                if (Scenario.bool(s, "block_center", true)) t = t.add(0.5, 0.5, 0.5); // [x,y,z] di un blocco → il suo centro
                Vec3 d = t.subtract(eye);
                yaw = (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90.0F;
                pitch = (float) -(Mth.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * Mth.RAD_TO_DEG);
            } else {
                yaw = (float) Scenario.num(s, "yaw", p.getYRot());
                pitch = (float) Scenario.num(s, "pitch", p.getXRot());
            }
            p.setYRot(yaw);
            p.setXRot(Mth.clamp(pitch, -90, 90));
            p.yRotO = yaw;
            p.xRotO = p.getXRot();
            p.setYHeadRot(yaw);
            p.yHeadRotO = yaw;
            p.yBodyRot = yaw;
            p.yBodyRotO = yaw;
            return StepResult.ok(String.format(Locale.ROOT, "yaw=%.1f pitch=%.1f", yaw, p.getXRot()));
        });
    }

    // ================================================================ schermate

    private static Exec openScreen(JsonObject s) {
        String screen = Scenario.str(s, "screen", "inventory");
        Minecraft mc = Minecraft.getInstance();
        Runnable open = switch (screen) {
            case "inventory" -> () -> mc.setScreen(new InventoryScreen(mc.player));
            case "guidebook", "book" -> () -> {
                ResourceLocation book = rl(Scenario.str(s, "book", "spectrum:guidebook"));
                if (!BookDataManager.get().areBooksBuilt()) BookDataManager.get().tryBuildBooks(mc.level);
                Book b = BookDataManager.get().getBook(book);
                if (b == null) throw new IllegalArgumentException("libro inesistente: " + book);
                if (s.has("entry")) {
                    ResourceLocation entry = rl(Scenario.str(s, "entry", ""));
                    BookEntry e = b.getEntry(entry);
                    if (e == null) throw new IllegalArgumentException("voce del libro inesistente: " + entry);
                    BookGuiManager.get().openEntry(book, entry, Scenario.integer(s, "page", 0));
                } else if (s.has("category")) {
                    var cat = b.getCategory(rl(Scenario.str(s, "category", "")));
                    if (cat == null) throw new IllegalArgumentException("categoria inesistente: " + s.get("category"));
                    BookGuiManager.get().openBook(BookAddress.defaultFor(cat)); // openCategory da solo fallisce senza il libro aperto
                } else {
                    BookGuiManager.get().openBook(BookAddress.defaultFor(book));
                }
            };
            case "recipes", "emi" -> () -> EmiBridge.show(Scenario.str(s, "recipe", null), Scenario.str(s, "item", null));
            case "jei" -> () -> JeiDev.showOutput(Scenario.str(s, "item", ""));
            case "none" -> () -> mc.setScreen(null);
            default -> throw new IllegalArgumentException("schermata sconosciuta: " + screen + " (inventory, guidebook, recipes, none)");
        };
        return client(() -> {
            open.run();
            return StepResult.ok("");
        }).then(r -> Exec.delay(Scenario.integer(s, "wait", 5), () -> {
            String cls = mc.screen == null ? "nessuna" : mc.screen.getClass().getSimpleName();
            boolean ok = screen.equals("none") ? mc.screen == null : mc.screen != null;
            return StepResult.of(ok, "schermata aperta: " + cls);
        }), false);
    }

    // ================================================================ interazione

    private static InteractionHand hand(JsonObject s) {
        return Scenario.str(s, "hand", "main").equals("off") ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    private static Exec useItem(JsonObject s, ScenarioRunner runner) {
        Minecraft mc = Minecraft.getInstance();
        InteractionHand hand = hand(s);
        int holdTicks = Scenario.integer(s, "hold_ticks", 0);
        boolean keep = Scenario.bool(s, "keep", false); // tasto destro resta premuto fino a "release_use"
        return new Exec() {
            int t = -1;
            String first;

            @Override
            public StepResult poll(ScenarioRunner r) {
                if (t < 0) {
                    t = 0;
                    InteractionResult res = mc.gameMode.useItem(mc.player, hand);
                    if (res.shouldSwing()) mc.player.swing(hand);
                    first = "uso di " + BuiltInRegistries.ITEM.getKey(mc.player.getItemInHand(hand).getItem()) + ": " + res;
                    if (keep) {
                        r.forceKey(mc.options.keyUse, true);
                        return StepResult.of(res != InteractionResult.FAIL, first + " (tasto tenuto fino a release_use)");
                    }
                    if (holdTicks <= 0) return StepResult.of(res != InteractionResult.FAIL, first);
                    r.forceKey(mc.options.keyUse, true); // tasto destro tenuto: altrimenti il client rilascia subito l'uso
                    return null;
                }
                if (++t < holdTicks) return null;
                r.forceKey(mc.options.keyUse, false);
                return StepResult.ok(first + ", tenuto " + holdTicks + " tick (in uso: " + mc.player.isUsingItem() + ")");
            }

            @Override
            public int expectedTicks() {
                return holdTicks;
            }
        };
    }

    private static Exec useOnBlock(JsonObject s) {
        BlockPos pos = Scenario.pos(s, "pos");
        Direction face = Direction.byName(Scenario.str(s, "face", "up"));
        if (face == null) throw new IllegalArgumentException("faccia non valida");
        InteractionHand hand = hand(s);
        return client(() -> {
            Minecraft mc = Minecraft.getInstance();
            Vec3 hit = Vec3.atCenterOf(pos).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
            InteractionResult res = mc.gameMode.useItemOn(mc.player, hand, new BlockHitResult(hit, face, pos, false));
            if (res.shouldSwing()) mc.player.swing(hand);
            return StepResult.of(res != InteractionResult.FAIL, "clic destro su " + pos.toShortString() + " (" + face + "): " + res);
        });
    }

    private static Exec sneak(JsonObject s, ScenarioRunner runner) {
        boolean on = Scenario.bool(s, "value", true);
        Minecraft mc = Minecraft.getInstance();
        runner.forceKey(mc.options.keyShift, on);
        // il client manda lo stato di Shift al server nel tick del giocatore: si aspetta qualche tick
        return Exec.delay(3, () -> StepResult.of(mc.player.isShiftKeyDown() == on, "shift=" + mc.player.isShiftKeyDown()));
    }

    /**
     * move: tiene premuti i tasti di movimento come il giocatore. {@code direction}: forward, back, left, right, anche
     * combinate con "+" (es. "forward+left"), oppure "none" per rilasciare tutto; {@code sprint} (false) aggiunge il tasto
     * di scatto; {@code ticks} (20) è la durata, poi i tasti si rilasciano; con {@code keep: true} restano premuti fino al
     * prossimo move (per fotografare il giocatore mentre cammina o corre). Ogni move rilascia prima i tasti del precedente.
     */
    private static Exec move(JsonObject s, ScenarioRunner runner) {
        Minecraft mc = Minecraft.getInstance();
        String dir = Scenario.str(s, "direction", "forward").toLowerCase(Locale.ROOT);
        boolean sprint = Scenario.bool(s, "sprint", false);
        boolean keep = Scenario.bool(s, "keep", false);
        int ticks = Math.max(1, Scenario.integer(s, "ticks", 20));
        KeyMapping[] all = {mc.options.keyUp, mc.options.keyDown, mc.options.keyLeft, mc.options.keyRight, mc.options.keySprint};
        List<KeyMapping> press = new ArrayList<>();
        if (!dir.equals("none")) {
            for (String d : dir.split("\\+")) {
                switch (d.trim()) {
                    case "forward" -> press.add(mc.options.keyUp);
                    case "back" -> press.add(mc.options.keyDown);
                    case "left" -> press.add(mc.options.keyLeft);
                    case "right" -> press.add(mc.options.keyRight);
                    default -> {
                        return Exec.done(StepResult.fail("direzione sconosciuta: " + d + " (forward, back, left, right, none)"));
                    }
                }
            }
            if (sprint) press.add(mc.options.keySprint);
        }
        return new Exec() {
            int t = -1;
            Vec3 start;

            @Override
            public StepResult poll(ScenarioRunner r) {
                LocalPlayer p = mc.player;
                if (t < 0) {
                    for (KeyMapping k : all) r.forceKey(k, false);
                    if (!sprint) p.setSprinting(false); // lo scatto resta attivo finché si va avanti: senza sprint si torna al passo
                    press.forEach(k -> r.forceKey(k, true));
                    if (press.isEmpty()) return StepResult.ok("movimento fermato, tasti rilasciati");
                    start = p.position();
                    t = 0;
                }
                if (++t < (keep ? 3 : ticks)) return null;
                double dist = p.position().subtract(start).horizontalDistance();
                if (!keep) press.forEach(k -> r.forceKey(k, false));
                return StepResult.of(dist > 0.05, String.format(Locale.ROOT, "%s%s per %d tick: %.2f blocchi, sprint=%s%s",
                        dir, sprint ? " + scatto" : "", t, dist, p.isSprinting(), keep ? " (tasti ancora premuti)" : ""));
            }

            @Override
            public int expectedTicks() {
                return keep ? 3 : ticks;
            }
        };
    }

    // ================================================================ verifiche

    private static Exec assertAdvancement(JsonObject s) {
        ResourceLocation id = rl(Scenario.req(s, "id"));
        boolean expect = Scenario.bool(s, "done", true);
        return onServer((server, sp) -> {
            AdvancementHolder h = server.getAdvancements().get(id);
            if (h == null) return StepResult.fail("advancement inesistente: " + id);
            boolean done = sp.getAdvancements().getOrStartProgress(h).isDone();
            return StepResult.of(done == expect, id + " fatto=" + done + " (atteso " + expect + ")");
        }).then(r -> Exec.delay(0, () -> StepResult.of(r.ok(), r.detail() + ", client=" + AdvancementHelper.hasAdvancementClient(id))), true);
    }

    private static Exec assertItem(JsonObject s) {
        String predicate = Scenario.str(s, "predicate", null);
        String itemId = Scenario.str(s, "item", null);
        if (predicate == null && itemId == null) throw new IllegalArgumentException("serve \"item\" o \"predicate\"");
        int minCount = Scenario.integer(s, "count", 1);
        boolean exact = Scenario.bool(s, "exact", false); // quantità esatta invece di minima
        boolean present = Scenario.bool(s, "present", true);
        String slot = Scenario.str(s, "slot", "any");
        JsonObject ench = s.has("enchantments") ? s.getAsJsonObject("enchantments") : new JsonObject();
        // pos (+ radius): cerca gli oggetti a terra (ItemEntity) attorno a pos invece che nell'inventario
        BlockPos near = s.has("pos") ? Scenario.pos(s, "pos") : null;
        double radius = Scenario.num(s, "radius", 3);
        // within: ripete il controllo ogni 10 tick fino a N tick (per aspettare la fine di un craft)
        int within = Scenario.integer(s, "within", 0);
        boolean dump = Scenario.bool(s, "dump", false);
        java.util.function.Supplier<Exec> check = () -> onServer((server, sp) -> {
            java.util.function.Predicate<ItemStack> match;
            if (predicate != null) {
                try {
                    CommandBuildContext ctx = CommandBuildContext.simple(server.registryAccess(), server.getWorldData().enabledFeatures());
                    match = ItemPredicateArgument.itemPredicate(ctx).parse(new StringReader(predicate));
                } catch (Exception e) {
                    return StepResult.fail("predicato non valido: " + e.getMessage());
                }
            } else {
                Optional<Item> it = BuiltInRegistries.ITEM.getOptional(rl(itemId));
                if (it.isEmpty()) return StepResult.fail("oggetto inesistente: " + itemId);
                match = st -> st.is(it.get());
            }
            String baseId = (predicate != null ? predicate : itemId).split("[\\[{ ]")[0];
            Item baseItem = BuiltInRegistries.ITEM.getOptional(ResourceLocation.tryParse(baseId)).orElse(null);
            List<String> missing = new ArrayList<>();
            java.util.function.Predicate<ItemStack> enchOk = st -> {
                for (Map.Entry<String, JsonElement> en : ench.entrySet()) {
                    var holder = server.registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                            .getHolder(ResourceKey.create(Registries.ENCHANTMENT, rl(en.getKey())));
                    if (holder.isEmpty()) {
                        missing.add("incantesimo inesistente " + en.getKey());
                        return false;
                    }
                    int lvl = Math.max(level(st.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY), holder.get()),
                            level(st.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY), holder.get()));
                    if (lvl < en.getValue().getAsInt()) return false;
                }
                return true;
            };
            List<ItemStack> stacks = new ArrayList<>();
            String where;
            if (near != null) {
                AABB box = new AABB(Vec3.atCenterOf(near), Vec3.atCenterOf(near)).inflate(radius);
                sp.serverLevel().getEntitiesOfClass(ItemEntity.class, box).forEach(e -> stacks.add(e.getItem()));
                where = "a terra entro " + radius + " da " + near.toShortString();
            } else if (slot.equals("any")) {
                for (int i = 0; i < sp.getInventory().getContainerSize(); i++) stacks.add(sp.getInventory().getItem(i));
                where = "slot any";
            } else {
                stacks.add(sp.getItemBySlot(EquipmentSlot.byName(slot)));
                where = "slot " + slot;
            }
            int count = 0;
            List<String> seen = new ArrayList<>();
            List<String> others = new ArrayList<>();
            for (ItemStack st : stacks) {
                if (st.isEmpty()) continue;
                if (!match.test(st)) {
                    // stesso oggetto ma componenti diversi: si mostrano, così si capisce cosa non torna
                    if (baseItem != null && st.is(baseItem)) others.add(st.getCount() + "x " + st.getComponentsPatch());
                    continue;
                }
                seen.add(st.getCount() + "x " + BuiltInRegistries.ITEM.getKey(st.getItem()) + enchString(st) + (dump ? " " + st.getComponentsPatch() : ""));
                if (enchOk.test(st)) count += st.getCount();
            }
            boolean found = exact ? count == minCount : count >= minCount;
            String what = predicate != null ? predicate : itemId;
            return StepResult.of(found == present, what + " (" + where + "): trovati " + count + ", servono " + (exact ? "esattamente " : "") + minCount
                    + (present ? "" : " (atteso ASSENTE)") + (seen.isEmpty() ? "" : "; corrispondenze: " + String.join(", ", seen))
                    + (others.isEmpty() || found == present && !dump ? "" : "; stesso oggetto, componenti diversi: " + String.join(", ", others))
                    + (missing.isEmpty() ? "" : "; " + String.join(", ", missing)));
        });
        if (within <= 0) return check.get();
        return new Exec() {
            Exec current;
            int tick;
            int nextCheck;

            @Override
            public StepResult poll(ScenarioRunner runner) {
                tick++; // tick reali dall'inizio del passo, controlli compresi
                if (current == null) {
                    if (tick < nextCheck) return null;
                    current = check.get();
                }
                StepResult r = current.poll(runner);
                if (r == null) return null;
                if (r.ok() || tick >= within) return StepResult.of(r.ok(), r.detail() + " (dopo " + tick + " tick)");
                current = null;
                nextCheck = tick + 10; // ricontrolla fra 10 tick
                return null;
            }

            @Override
            public int expectedTicks() {
                return within + 20;
            }
        };
    }

    private static int level(ItemEnchantments e, Holder<Enchantment> h) {
        return e.getLevel(h);
    }

    private static String enchString(ItemStack st) {
        ItemEnchantments e = st.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (e.isEmpty()) return "";
        List<String> l = new ArrayList<>();
        e.entrySet().forEach(en -> l.add(en.getKey().unwrapKey().map(k -> k.location().toString()).orElse("?") + " " + en.getIntValue()));
        return " {" + String.join(", ", l) + "}";
    }

    private static Exec assertBlock(JsonObject s) {
        BlockPos pos = Scenario.pos(s, "pos");
        String expected = Scenario.req(s, "block");
        JsonObject props = s.has("properties") ? s.getAsJsonObject("properties") : new JsonObject();
        return onServer((server, sp) -> {
            BlockState st = sp.serverLevel().getBlockState(pos);
            String id = BuiltInRegistries.BLOCK.getKey(st.getBlock()).toString();
            boolean ok = id.equals(expected) || expected.startsWith("#") && st.is(net.minecraft.tags.TagKey.create(Registries.BLOCK, rl(expected.substring(1))));
            List<String> bad = new ArrayList<>();
            for (Map.Entry<String, JsonElement> p : props.entrySet()) {
                Property<?> prop = st.getBlock().getStateDefinition().getProperty(p.getKey());
                String v = prop == null ? "(assente)" : propValue(st, prop);
                if (!v.equals(p.getValue().getAsString())) bad.add(p.getKey() + "=" + v);
            }
            return StepResult.of(ok && bad.isEmpty(), pos.toShortString() + ": " + st + (bad.isEmpty() ? "" : " proprietà diverse: " + bad));
        });
    }

    private static <T extends Comparable<T>> String propValue(BlockState st, Property<T> p) {
        return p.getName(st.getValue(p));
    }

    /**
     * Ricetta sbloccata? Per le ricette di Spectrum (Pedestal, Fusion, ...) vale {@link GatedRecipe#canPlayerCraft}:
     * il giocatore deve avere l'advancement che sblocca il TIPO di ricetta e il {@code required_advancement} della ricetta
     * (Revelationary, AdvancementHelper). Si controlla sia sul server (craft accettato) sia sul client (ricetta mostrata).
     * Per le ricette vanilla si guarda il libro delle ricette del giocatore.
     */
    private static Exec assertRecipe(JsonObject s) {
        ResourceLocation id = rl(Scenario.req(s, "recipe"));
        boolean expect = Scenario.bool(s, "unlocked", true);
        return onServer((server, sp) -> {
            Optional<RecipeHolder<?>> h = server.getRecipeManager().byKey(id);
            if (h.isEmpty()) return StepResult.fail("ricetta inesistente: " + id);
            Recipe<?> rec = h.get().value();
            if (rec instanceof GatedRecipe<?> g) {
                boolean can = g.canPlayerCraft(sp);
                ResourceLocation type = g.getRecipeTypeUnlockIdentifier();
                String req = g.getRequiredAdvancement().map(r -> r + "=" + AdvancementHelper.hasAdvancement(sp, r)).orElse("nessuno");
                // Le ricette Pedestal sostituiscono lo sblocco del tipo con quello del loro tier (PedestalRecipe.canPlayerCraft)
                String gate = rec instanceof PedestalRecipe pr
                        ? "tier " + pr.getTier().getSerializedName() + " sbloccato=" + pr.getTier().hasUnlocked(sp)
                        : "sblocco tipo " + type + "=" + AdvancementHelper.hasAdvancement(sp, type);
                return StepResult.of(can == expect, id + " (" + g.getRecipeTypeShortID() + ") server sbloccata=" + can
                        + " (attesa " + expect + ") [" + gate + ", required_advancement " + req + "]");
            }
            boolean known = sp.getRecipeBook().contains(h.get());
            return StepResult.of(known == expect, id + " (ricetta non Spectrum) nel libro delle ricette=" + known);
        }).then(r -> Exec.delay(1, () -> {
            Minecraft mc = Minecraft.getInstance();
            Optional<RecipeHolder<?>> h = mc.level.getRecipeManager().byKey(id);
            if (h.isPresent() && h.get().value() instanceof GatedRecipe<?> g) {
                boolean can = g.canPlayerCraft(mc.player);
                return StepResult.of(r.ok() && can == expect, r.detail() + ", client=" + can);
            }
            return r;
        }), false);
    }

    private static Exec assertBookEntry(JsonObject s) {
        ResourceLocation id = rl(Scenario.req(s, "entry"));
        ResourceLocation bookId = rl(Scenario.str(s, "book", "spectrum:guidebook"));
        boolean expect = Scenario.bool(s, "unlocked", true);
        int pageIndex = Scenario.integer(s, "page", -1);
        return onServer((server, sp) -> {
            Book b = BookDataManager.get().getBook(bookId);
            if (b == null) return StepResult.fail("libro inesistente: " + bookId);
            BookEntry e = b.getEntry(id);
            if (e == null) return StepResult.fail("voce del libro inesistente: " + id);
            // Modonomicon salva gli sblocchi per giocatore e NON li toglie se l'advancement viene revocato
            // (per ripartire da zero: azione reset_book). Gli sblocchi nuovi arrivano a fine tick del server.
            if (pageIndex >= 0) {
                // una pagina della voce (indice nella lista "pages" del JSON): le pagine ricetta hanno la loro condizione
                if (pageIndex >= e.getPages().size()) return StepResult.fail(id + ": la voce ha solo " + e.getPages().size() + " pagine");
                var page = e.getPages().get(pageIndex);
                boolean pu = BookUnlockStateManager.get().isUnlockedFor(sp, page);
                boolean pc = page.getCondition().test(BookConditionContext.of(b, page), sp);
                if (!pu && pc) {
                    BookUnlockStateManager.get().updateAndSyncFor(sp);
                    pu = BookUnlockStateManager.get().isUnlockedFor(sp, page);
                }
                return StepResult.of(pu == expect, id + " pagina " + pageIndex + " (" + page.getType() + ", n. " + page.getPageNumber()
                        + ") sbloccata=" + pu + " (attesa " + expect + "), condizione ora=" + pc
                        + ", voce sbloccata=" + BookUnlockStateManager.get().isUnlockedFor(sp, e));
            }
            boolean u = BookUnlockStateManager.get().isUnlockedFor(sp, e);
            boolean cond = e.getCondition().test(BookConditionContext.of(b, e), sp);
            String extra = "";
            if (!u && cond) {
                BookUnlockStateManager.get().updateAndSyncFor(sp);
                u = BookUnlockStateManager.get().isUnlockedFor(sp, e);
                extra = " (dopo aggiornamento forzato)";
            }
            return StepResult.of(u == expect, id + " sbloccata=" + u + extra + " (attesa " + expect + "), condizione ora=" + cond);
        });
    }

    private static Exec assertRevealed(JsonObject s) {
        boolean expect = Scenario.bool(s, "revealed", true);
        return client(() -> {
            if (s.has("block")) {
                Block b = BuiltInRegistries.BLOCK.get(rl(Scenario.str(s, "block", "")));
                boolean rev = !ClientRevelationHolder.isCloaked(b.defaultBlockState());
                return StepResult.of(rev == expect, s.get("block").getAsString() + " rivelato=" + rev);
            }
            Item it = BuiltInRegistries.ITEM.get(rl(Scenario.req(s, "item")));
            boolean rev = !ClientRevelationHolder.isCloaked(it);
            return StepResult.of(rev == expect, s.get("item").getAsString() + " rivelato=" + rev);
        });
    }

    private static Exec assertLog(JsonObject s, ScenarioRunner runner) {
        Pattern re = Pattern.compile(Scenario.req(s, "regex"));
        boolean present = Scenario.bool(s, "present", true);
        int within = Scenario.integer(s, "within", 40);
        long from = runner.logOffset();
        return new Exec() {
            int t;

            @Override
            public StepResult poll(ScenarioRunner r) {
                String hit = null;
                for (String l : LogCapture.since(from)) {
                    if (!l.contains(ScenarioRunner.TAG) && re.matcher(l).find()) {
                        hit = l;
                        break;
                    }
                }
                if (hit != null && present) return StepResult.ok("trovato /" + re + "/: " + hit);
                if (t++ < within) return null;
                return StepResult.of((hit != null) == present, (hit != null ? "trovato: " + hit : "non trovato /" + re + "/")
                        + " (atteso " + (present ? "presente" : "assente") + ", righe dall'inizio dello scenario)");
            }

            @Override
            public int expectedTicks() {
                return within;
            }
        };
    }

    // ================================================================ costruzioni

    /**
     * build: piazza una struttura.
     * <ul>
     *   <li>{"preset": "pedestal_moonstone", "pos": [x,y,z]}: preset in scenarios/presets/&lt;nome&gt;.json, pos = blocco "macchina";</li>
     *   <li>{"multiblock": "spectrum:fusion_shrine", "center": [x,y,z]}: multiblock Modonomicon, center = il suo blocco '0';</li>
     *   <li>{"blocks": [{"pos": [x,y,z], "block": "minecraft:stone"}], "origin": [x,y,z]}: lista di blocchi (origin facoltativa);</li>
     *   <li>{"file": "strutture/x.json", "origin": [...]}: file con "blocks" (e/o "multiblock") come sopra.</li>
     * </ul>
     */
    private static Exec build(JsonObject s) throws IOException {
        JsonObject spec = s;
        BlockPos origin = s.has("origin") ? Scenario.pos(s, "origin") : BlockPos.ZERO;
        BlockPos center = null;
        if (s.has("preset") || s.has("file")) {
            boolean preset = s.has("preset");
            Path p = ScenarioFiles.resolve(Scenario.str(s, preset ? "preset" : "file", ""), preset);
            spec = Scenario.readJson(p);
            if (preset) {
                BlockPos pos = Scenario.pos(s, "pos");
                BlockPos off = spec.has("center_offset") ? Scenario.pos(spec, "center_offset") : BlockPos.ZERO;
                center = pos.offset(off);
                origin = pos;
            }
        }
        if (center == null && s.has("center")) center = Scenario.pos(s, "center");
        Rotation rot = rotation(Scenario.str(s, "rotation", Scenario.str(spec, "rotation", "none")));
        String mbId = Scenario.str(spec, "multiblock", null);
        JsonArray blocks = spec.has("blocks") ? spec.getAsJsonArray("blocks") : new JsonArray();
        boolean clear = Scenario.bool(s, "clear", Scenario.bool(spec, "clear", true));
        boolean validate = Scenario.bool(s, "validate", Scenario.bool(spec, "validate", true));
        BlockPos fCenter = center;
        BlockPos fOrigin = origin;
        if (mbId != null && fCenter == null) throw new IllegalArgumentException("per un multiblock serve \"center\" (o un preset con \"pos\")");
        if (mbId == null && blocks.isEmpty()) throw new IllegalArgumentException("niente da costruire: servono multiblock o blocks");
        return onServer((server, sp) -> {
            ServerLevel level = sp.serverLevel();
            List<String> details = new ArrayList<>();
            boolean ok = true;
            if (mbId != null) {
                Multiblock mb = ModonomiconAPI.get().getMultiblock(rl(mbId));
                if (mb == null) return StepResult.fail("multiblock inesistente: " + mbId);
                int placed = placeMultiblock(level, mb, fCenter, rot, clear);
                details.add(mbId + ": " + placed + " blocchi attorno a " + fCenter.toShortString());
                if (validate) {
                    StepResult v = validateMultiblock(level, mb, fCenter, rot);
                    ok = v.ok();
                    details.add(v.detail());
                }
            }
            for (JsonElement e : blocks) {
                JsonObject b = e.getAsJsonObject();
                BlockPos p = Scenario.pos(b, "pos").offset(fOrigin);
                BlockState st;
                try {
                    st = BlockStateParser.parseForBlock(level.holderLookup(Registries.BLOCK), Scenario.req(b, "block"), false).blockState();
                } catch (Exception ex) {
                    return StepResult.fail("blocco non valido " + b + ": " + ex.getMessage());
                }
                level.setBlock(p, st, Block.UPDATE_ALL);
            }
            if (!blocks.isEmpty()) details.add(blocks.size() + " blocchi dalla lista");
            return StepResult.of(ok, String.join("; ", details));
        });
    }

    private static Rotation rotation(String s) {
        return switch (s) {
            case "cw90", "clockwise_90" -> Rotation.CLOCKWISE_90;
            case "180", "clockwise_180" -> Rotation.CLOCKWISE_180;
            case "ccw90", "counterclockwise_90" -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    /**
     * Come Multiblock.place di Modonomicon, ma prima svuota l'ingombro (place salta le posizioni occupate, per esempio
     * il terreno) e piazza dal basso in alto. Per i caratteri a tag piazza il blocco "display" del JSON del multiblock.
     */
    static int placeMultiblock(ServerLevel level, Multiblock mb, BlockPos center, Rotation rot, boolean clear) {
        mb.setLevel(level);
        var results = new ArrayList<>(mb.simulate(level, center, rot, false, false).getSecond());
        results.sort(Comparator.comparingInt(r -> r.getWorldPosition().getY()));
        if (clear && !results.isEmpty()) {
            BlockPos min = results.get(0).getWorldPosition(), max = min;
            for (var r : results) {
                min = BlockPos.min(min, r.getWorldPosition());
                max = BlockPos.max(max, r.getWorldPosition());
            }
            for (BlockPos p : BlockPos.betweenClosed(min, max)) level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        BlockPos lo = center, hi = center;
        for (var r : results) {
            lo = BlockPos.min(lo, r.getWorldPosition());
            hi = BlockPos.max(hi, r.getWorldPosition());
        }
        int n = 0;
        long time = level.getGameTime();
        for (var r : results) {
            BlockState st = r.getStateMatcher().getDisplayedState(time).rotate(rot);
            if (st.isAir()) continue;
            level.setBlock(r.getWorldPosition(), st, Block.UPDATE_ALL);
            n++;
        }
        // oggetti caduti durante la costruzione (blocchi che si staccano mentre i vicini cambiano)
        level.getEntitiesOfClass(ItemEntity.class, new AABB(Vec3.atLowerCornerOf(lo), Vec3.atLowerCornerOf(hi).add(1, 1, 1)).inflate(2))
                .forEach(Entity::discard);
        return n;
    }

    /** Stesso controllo che fanno Pedestal e Fusion Shrine di Spectrum: Multiblock.validate(level, centro, rotazione). */
    static StepResult validateMultiblock(ServerLevel level, Multiblock mb, BlockPos center, Rotation rot) {
        boolean valid = mb.validate(level, center, rot);
        if (valid) return StepResult.ok("struttura VALIDA (Multiblock.validate, rotazione " + rot + ")");
        List<String> bad = new ArrayList<>();
        for (var r : mb.simulate(level, center, rot, false, false).getSecond()) {
            if (!r.test(level, rot)) {
                bad.add("'" + r.getCharacter() + "'@" + r.getWorldPosition().toShortString() + "=" + level.getBlockState(r.getWorldPosition()));
                if (bad.size() >= 8) break;
            }
        }
        return StepResult.fail("struttura NON valida; differenze: " + String.join(", ", bad));
    }

    private static Exec assertMultiblock(JsonObject s) throws IOException {
        ResourceLocation id;
        BlockPos center;
        if (s.has("preset")) {
            JsonObject spec = Scenario.readJson(ScenarioFiles.resolve(Scenario.str(s, "preset", ""), true));
            id = rl(Scenario.req(spec, "multiblock"));
            center = Scenario.pos(s, "pos").offset(spec.has("center_offset") ? Scenario.pos(spec, "center_offset") : BlockPos.ZERO);
        } else {
            id = rl(Scenario.req(s, "multiblock"));
            center = Scenario.pos(s, "center");
        }
        Rotation rot = rotation(Scenario.str(s, "rotation", "none"));
        boolean expect = Scenario.bool(s, "valid", true);
        return onServer((server, sp) -> {
            Multiblock mb = ModonomiconAPI.get().getMultiblock(id);
            if (mb == null) return StepResult.fail("multiblock inesistente: " + id);
            StepResult r = validateMultiblock(sp.serverLevel(), mb, center, rot);
            return StepResult.of(r.ok() == expect, id + " @" + center.toShortString() + ": " + r.detail());
        });
    }
}
