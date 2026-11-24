package cromocon.spectralascension.gametest;

import cromocon.spectralascension.SpectralAscension;
import de.dafuqs.arrowhead.api.ArrowheadCrossbow;
import de.dafuqs.spectrum.entity.entity.BedrockFishingHook;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import static cromocon.spectralascension.gametest.SATestUtil.*;

/**
 * Misure (non verifiche): tempo di carica delle balestre con Quick Charge, tempo fino alla abboccata con Lure e
 * distribuzione dei minerali di Spectrum nella Mining Dimension di Allthemodium. Ogni riga di misura nel log ha il
 * prefisso {@code [GameTest] misura:}; le asserzioni riguardano solo la sanità (niente crash, numeri plausibili).
 * Ogni test ha un suo batch, così gira da solo e non disturba gli altri.
 */
@GameTestHolder(SpectralAscension.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SAMeasureTests {
    private static final String EMPTY = "empty";

    private SAMeasureTests() {
    }

    private static void log(String format, Object... args) {
        SpectralAscension.LOGGER.info("[GameTest] misura: " + format, args);
    }

    private static Item ours(String id) {
        Item item = BuiltInRegistries.ITEM.get(SpectralAscension.id(id));
        if (item == Items.AIR) throw new IllegalStateException("oggetto mancante " + id);
        return item;
    }

    private static Field field(Class<?> type, String name) {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new IllegalStateException("campo " + name + " non trovato in " + type);
    }

    // ---------- 1. Balestra: Quick Charge ----------

    /**
     * Per ogni balestra (vanilla di riferimento, T6, T7, T8, Alloy) e per ogni livello di Quick Charge da 0 a 6 (gli altri
     * incantesimi restano quelli preimpostati): modificatore vanilla (EnchantmentHelper.modifyCrossbowChargingTime),
     * CrossbowItem.getChargeDuration (con la mixin di Arrowhead), getUseDuration e il primo tick di tenuta in cui
     * releaseUsing carica davvero la balestra.
     */
    @GameTest(template = EMPTY, batch = "sa_measure_crossbow", timeoutTicks = 200)
    public static void measure_crossbow_quick_charge(GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> quickCharge = registry.getOrThrow(Enchantments.QUICK_CHARGE);
        List<Item> crossbows = List.of(Items.CROSSBOW, ours("trichromatic_crossbow"), ours("umbral_crossbow"), ours("argent_crossbow"), ours("prismatic_crossbow"));
        BlockPos base = SAFunctionTests.slot(helper, 0);
        for (Item item : crossbows) {
            String id = id(item);
            for (int qc = 0; qc <= 6; qc++) {
                ItemStack stack = enchanted(helper, item);
                ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(stack.getEnchantments());
                int defaultQc = mutable.getLevel(quickCharge);
                mutable.set(quickCharge, qc);
                stack.set(DataComponents.ENCHANTMENTS, mutable.toImmutable());
                int storedQc = stack.getEnchantments().getLevel(quickCharge);
                ServerPlayer player = mockPlayer(helper);
                aim(player, base, 0, -10);
                float vanillaSeconds = EnchantmentHelper.modifyCrossbowChargingTime(stack, player, 1.25F);
                int vanillaTicks = (int) Math.floor(vanillaSeconds * 20.0F);
                int charge = CrossbowItem.getChargeDuration(stack, player);
                int useDuration = stack.getUseDuration(player);
                float pull = item instanceof ArrowheadCrossbow a ? a.getPullTimeModifier(stack, player) : 1.0F;
                int firstTick = -1;
                for (int held = 0; held <= 80 && firstTick < 0; held++) {
                    ItemStack s = stack.copy();
                    player.getInventory().clearContent();
                    player.getInventory().setItem(9, new ItemStack(Items.ARROW, 64));
                    player.setItemInHand(InteractionHand.MAIN_HAND, s);
                    s.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
                    s.releaseUsing(helper.getLevel(), player, useDuration - held);
                    player.stopUsingItem();
                    if (CrossbowItem.isCharged(s)) firstTick = held;
                    clearAround(helper, base, 8);
                }
                log("balestra {} QC {}{} (livello salvato {}): base vanilla {} s = {} tick, pullTime x{}, getChargeDuration = {} tick, getUseDuration = {}, primo tick di tenuta in cui si carica = {}",
                        id, qc, qc == defaultQc ? " [predefinito]" : "", storedQc, vanillaSeconds, vanillaTicks, pull, charge, useDuration, firstTick);
                helper.assertTrue(charge >= 0 && charge <= 200, id + " QC " + qc + ": carica " + charge + " tick fuori scala");
                helper.assertTrue(useDuration >= 0 && useDuration <= 220, id + " QC " + qc + ": useDuration " + useDuration + " fuori scala");
                helper.assertTrue(firstTick <= 80, id + " QC " + qc + ": non si carica mai");
            }
        }
        helper.succeed();
    }

    // ---------- 2. Canna: Lure ----------

    private record Variant(String name, boolean spectrum, int level, int lureSpeed, ServerPlayer player, List<FishingHook> hooks, int[] bite) {
    }

    /**
     * Ami veri nell'acqua (vanilla FishingHook e SpectrumFishingHook, quello delle nostre canne) con Lure 0..8: tick fino
     * alla prima abboccata (nibble &gt; 0). 20 ami per variante, tutti insieme in una vasca, per 1500 tick.
     */
    @GameTest(template = EMPTY, batch = "sa_measure_fishing", timeoutTicks = 1700)
    public static void measure_fishing_lure(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> lure = registry.getOrThrow(Enchantments.LURE);
        Field nibbleField = field(FishingHook.class, "nibble");
        Field luredField = field(FishingHook.class, "timeUntilLured");
        Field hookedField = field(FishingHook.class, "timeUntilHooked");
        Field spectrumLure = field(de.dafuqs.spectrum.entity.entity.SpectrumFishingHook.class, "lureSpeed");
        BlockPos origin = helper.absolutePos(new BlockPos(0, 150, 0));
        // I chunk attorno alla vasca devono essere forzati (e quindi ticking): il template "empty" carica solo il suo chunk e
        // gli ami fuori da lì non farebbero tick.
        for (int cx = (origin.getX() - 10) >> 4; cx <= (origin.getX() + 10) >> 4; cx++)
            for (int cz = (origin.getZ() - 10) >> 4; cz <= (origin.getZ() + 12) >> 4; cz++) level.setChunkForced(cx, cz, true);
        // vasca 13x13 di roccia con 11x11 d'acqua profonda 2, bordo a filo dell'acqua; cielo aperto sopra
        for (BlockPos p : BlockPos.betweenClosed(origin.offset(-7, -4, -7), origin.offset(7, 8, 7))) {
            level.setBlockAndUpdate(p, p.getY() < origin.getY() - 1 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
        }
        for (BlockPos p : BlockPos.betweenClosed(origin.offset(-6, -3, -6), origin.offset(6, -1, 6))) {
            boolean edge = Math.abs(p.getX() - origin.getX()) == 6 || Math.abs(p.getZ() - origin.getZ()) == 6;
            level.setBlockAndUpdate(p, edge ? Blocks.STONE.defaultBlockState() : Blocks.WATER.defaultBlockState());
        }
        BlockPos playerPos = origin.offset(0, 0, 8);
        List<Variant> variants = new ArrayList<>();
        int[] levels = {0, 1, 3, 4, 5, 6, 7, 8};
        for (boolean spectrum : new boolean[]{false, true}) {
            for (int lv : levels) {
                ServerPlayer player = mockPlayer(helper);
                aim(player, playerPos, 180, 0);
                ItemStack rod = new ItemStack(spectrum ? ours("argent_fishing_rod") : Items.FISHING_ROD);
                if (lv > 0) {
                    var m = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                    m.set(lure, lv);
                    rod.set(DataComponents.ENCHANTMENTS, m.toImmutable());
                }
                player.setItemInHand(InteractionHand.MAIN_HAND, rod);
                int speed = (int) (EnchantmentHelper.getFishingTimeReduction(level, rod, player) * 20.0F);
                List<FishingHook> hooks = new ArrayList<>();
                for (int i = 0; i < 20; i++) {
                    FishingHook hook = spectrum ? new BedrockFishingHook(player, level, 0, speed, 0, 0, 0, false, false) : new FishingHook(player, level, 0, speed);
                    hook.setPos(origin.getX() + 0.5 + (i % 5 - 2) * 2, origin.getY() - 0.4, origin.getZ() + 0.5 + (variants.size() % 5 - 2) * 2);
                    hook.setDeltaMovement(0, 0, 0);
                    level.addFreshEntity(hook);
                    hooks.add(hook);
                }
                int[] bite = new int[20];
                Arrays.fill(bite, -1);
                variants.add(new Variant((spectrum ? "SpectrumFishingHook (canne SA)" : "FishingHook vanilla") + " Lure " + lv, spectrum, lv, speed, player, hooks, bite));
            }
        }
        // verifica col lancio vero di una nostra canna T8 (Lure V predefinito)
        ServerPlayer castPlayer = mockPlayer(helper);
        aim(castPlayer, playerPos, 180, 0);
        ItemStack argent = enchanted(helper, ours("argent_fishing_rod"));
        castPlayer.setItemInHand(InteractionHand.MAIN_HAND, argent);
        argent.getItem().use(level, castPlayer, InteractionHand.MAIN_HAND);
        try {
            if (castPlayer.fishing != null) {
                log("lancio vero con argent_fishing_rod (Lure {}): lureSpeed dell'amo = {}", argent.getEnchantments().getLevel(lure), spectrumLure.getInt(castPlayer.fishing));
                castPlayer.fishing.discard();
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        AtomicInteger tick = new AtomicInteger();
        helper.onEachTick(() -> {
            int t = tick.incrementAndGet();
            for (Variant v : variants) {
                for (int i = 0; i < v.hooks().size(); i++) {
                    FishingHook h = v.hooks().get(i);
                    if (v.bite()[i] >= 0 || h.isRemoved()) continue;
                    try {
                        if (nibbleField.getInt(h) > 0) v.bite()[i] = t;
                    } catch (ReflectiveOperationException e) {
                        throw new IllegalStateException(e);
                    }
                }
            }
        });
        helper.runAfterDelay(1500, () -> {
            try {
                for (Variant v : variants) {
                    List<Integer> times = new ArrayList<>();
                    int removed = 0;
                    for (int i = 0; i < v.hooks().size(); i++) {
                        if (v.bite()[i] >= 0) times.add(v.bite()[i]);
                        if (v.hooks().get(i).isRemoved() && v.bite()[i] < 0) removed++;
                    }
                    times.sort(null);
                    FishingHook h0 = v.hooks().getFirst();
                    String stats = times.isEmpty() ? "nessuna abboccata"
                            : "min " + times.getFirst() + ", mediana " + times.get(times.size() / 2) + ", media " + String.format("%.1f", times.stream().mapToInt(Integer::intValue).average().orElse(0)) + ", max " + times.getLast();
                    log("{}: lureSpeed {} tick; abboccate {}/{} in 1500 tick ({}); ami rimossi senza abboccata {}; primo amo: timeUntilLured {} timeUntilHooked {}",
                            v.name(), v.lureSpeed(), times.size(), v.hooks().size(), stats, removed, luredField.getInt(h0), hookedField.getInt(h0));
                    helper.assertTrue(v.lureSpeed() >= 0 && v.lureSpeed() <= 2000, v.name() + ": lureSpeed " + v.lureSpeed() + " fuori scala");
                    v.hooks().forEach(FishingHook::discard);
                }
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
            for (int cx = (origin.getX() - 10) >> 4; cx <= (origin.getX() + 10) >> 4; cx++)
                for (int cz = (origin.getZ() - 10) >> 4; cz <= (origin.getZ() + 12) >> 4; cz++) level.setChunkForced(cx, cz, false);
            helper.succeed();
        });
    }

    // ---------- 3. Mining Dimension ----------

    private static final int REGION_X = 125, REGION_Z = 125, REGION_SIZE = Integer.getInteger("sa.measure.region", 16), CHUNKS_PER_TICK = 4;

    private static final List<String> SPECTRUM_ORES = List.of("spectrum:shimmerstone_ore", "spectrum:deepslate_shimmerstone_ore", "spectrum:azurite_ore",
            "spectrum:deepslate_azurite_ore", "spectrum:stratine_ore", "spectrum:paltaeria_ore");

    /**
     * Genera una regione di chunk lontano dallo spawn nella dimensione allthemodium:mining (CHUNKS_PER_TICK chunk per tick
     * per non bloccare il server) e conta i minerali di Spectrum per strato, il blocco dominante di ogni strato e i
     * minerali presenti nella dimensione.
     */
    @GameTest(template = EMPTY, batch = "sa_measure_mining", timeoutTicks = 2400)
    public static void measure_mining_dimension_ores(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("allthemodium:mining")));
        log("dimensioni caricate dal server di test (preset flat): {}", helper.getLevel().getServer().levelKeys());
        if (level == null) {
            // Il GameTestServer parte dal preset flat e carica solo overworld, nether e end: la Mining Dimension va costruita
            // a mano, con lo stesso LevelStem del datapack di Allthemodium (data/allthemodium/dimension/mining.json).
            try {
                level = createMiningLevel(helper.getLevel().getServer());
            } catch (Exception e) {
                throw new IllegalStateException("impossibile creare allthemodium:mining", e);
            }
        }
        final ServerLevel miningLevel = level;
        int minY = miningLevel.getMinBuildHeight(), maxY = miningLevel.getMaxBuildHeight(), height = maxY - minY;
        log("dimensione allthemodium:mining: y {}..{} (altezza {}), generatore {}", minY, maxY - 1, height, miningLevel.getChunkSource().getGenerator().getClass().getSimpleName());
        Map<Block, int[]> perY = new HashMap<>();
        Map<Block, Set<Long>> orePositions = new HashMap<>();
        Set<Block> tracked = new HashSet<>();
        for (String s : SPECTRUM_ORES) {
            Block b = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(s));
            if (b == Blocks.AIR) log("ATTENZIONE: il blocco {} non esiste", s);
            else tracked.add(b);
        }
        List<String> columns = new ArrayList<>();
        int total = REGION_SIZE * REGION_SIZE;
        AtomicInteger done = new AtomicInteger();
        long[] started = {System.nanoTime()};
        helper.onEachTick(() -> {
            if (done.get() >= total) return;
            for (int n = 0; n < CHUNKS_PER_TICK && done.get() < total; n++) {
                int i = done.getAndIncrement();
                int cx = REGION_X + i % REGION_SIZE, cz = REGION_Z + i / REGION_SIZE;
                ChunkAccess chunk = miningLevel.getChunk(cx, cz, ChunkStatus.FULL, true);
                LevelChunkSection[] sections = chunk.getSections();
                for (int si = 0; si < sections.length; si++) {
                    LevelChunkSection section = sections[si];
                    if (section.hasOnlyAir()) {
                        perY.computeIfAbsent(Blocks.AIR, k -> new int[height]);
                        for (int ly = 0; ly < 16; ly++) perY.get(Blocks.AIR)[(chunk.getMinSection() + si) * 16 + ly - minY] += 256;
                        continue;
                    }
                    for (int ly = 0; ly < 16; ly++) {
                        int y = (chunk.getMinSection() + si) * 16 + ly;
                        for (int lz = 0; lz < 16; lz++) {
                            for (int lx = 0; lx < 16; lx++) {
                                Block b = section.getBlockState(lx, ly, lz).getBlock();
                                perY.computeIfAbsent(b, k -> new int[height])[y - minY]++;
                                if (tracked.contains(b)) orePositions.computeIfAbsent(b, k -> new HashSet<>()).add(BlockPos.asLong(cx * 16 + lx, y, cz * 16 + lz));
                            }
                        }
                    }
                }
                if (i < 2) {
                    StringBuilder col = new StringBuilder("colonna x=" + (cx * 16 + 8) + " z=" + (cz * 16 + 8) + ":");
                    Block prev = null;
                    int start = minY;
                    for (int y = minY; y <= maxY; y++) {
                        Block b = y < maxY ? chunk.getBlockState(new BlockPos(cx * 16 + 8, y, cz * 16 + 8)).getBlock() : null;
                        if (b != prev) {
                            if (prev != null) col.append(" ").append(start).append("..").append(y - 1).append(" ").append(BuiltInRegistries.BLOCK.getKey(prev).getPath()).append(";");
                            prev = b;
                            start = y;
                        }
                    }
                    columns.add(col.toString());
                }
            }
            if (done.get() >= total) {
                report(helper, miningLevel, perY, orePositions, columns, minY, maxY, total, (System.nanoTime() - started[0]) / 1_000_000_000.0);
            }
        });
    }

    /** Crea (senza registrarlo nel server) il livello allthemodium:mining dal suo LevelStem nel datapack di Allthemodium. */
    private static ServerLevel createMiningLevel(net.minecraft.server.MinecraftServer server) throws Exception {
        var resource = server.getResourceManager().getResourceOrThrow(ResourceLocation.parse("allthemodium:dimension/mining.json"));
        com.google.gson.JsonElement json;
        try (var reader = resource.openAsReader()) {
            json = com.google.gson.JsonParser.parseReader(reader);
        }
        var ops = net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE, server.registryAccess());
        net.minecraft.world.level.dimension.LevelStem stem = net.minecraft.world.level.dimension.LevelStem.CODEC.parse(ops, json).getOrThrow(IllegalStateException::new);
        var storage = (net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess) field(net.minecraft.server.MinecraftServer.class, "storageSource").get(server);
        var executor = (java.util.concurrent.Executor) field(net.minecraft.server.MinecraftServer.class, "executor").get(server);
        var data = new net.minecraft.world.level.storage.DerivedLevelData(server.getWorldData(), server.getWorldData().overworldData());
        var listener = new net.minecraft.server.level.progress.ChunkProgressListener() {
            @Override
            public void updateSpawnPos(ChunkPos center) {
            }

            @Override
            public void onStatusChange(ChunkPos chunkPos, @javax.annotation.Nullable ChunkStatus chunkStatus) {
            }

            @Override
            public void start() {
            }

            @Override
            public void stop() {
            }
        };
        long seed = server.getWorldData().worldGenOptions().seed();
        return new ServerLevel(server, executor, storage, data, ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("allthemodium:mining")), stem, listener, false,
                net.minecraft.world.level.biome.BiomeManager.obfuscateSeed(seed), List.of(), false, null);
    }

    private static void report(GameTestHelper helper, ServerLevel level, Map<Block, int[]> perY, Map<Block, Set<Long>> orePositions,
                               List<String> columns, int minY, int maxY, int chunks, double seconds) {
        int height = maxY - minY;
        log("mining: {} chunk generati e contati in {} s (regione chunk {},{} .. +{})", chunks, String.format("%.1f", seconds), REGION_X, REGION_Z, REGION_SIZE - 1);
        columns.forEach(c -> log("mining: {}", c));
        // strati: blocco dominante per y, a tratti
        StringBuilder runs = new StringBuilder();
        Block runBlock = null;
        int runStart = minY;
        for (int y = minY; y <= maxY; y++) {
            Block best = null;
            int bestCount = -1;
            if (y < maxY) {
                for (var e : perY.entrySet()) {
                    int c = e.getValue()[y - minY];
                    if (c > bestCount) {
                        bestCount = c;
                        best = e.getKey();
                    }
                }
            }
            if (best != runBlock) {
                if (runBlock != null) runs.append("\n  y ").append(runStart).append("..").append(y - 1).append(": ").append(BuiltInRegistries.BLOCK.getKey(runBlock));
                runBlock = best;
                runStart = y;
            }
        }
        log("mining: blocco dominante per strato (su {} colonne per y):{}", chunks * 256, runs);
        // minerali di Spectrum
        for (String s : SPECTRUM_ORES) {
            Block b = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(s));
            int[] ys = perY.get(b);
            int sum = 0, lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
            if (ys != null) {
                for (int i = 0; i < height; i++) {
                    sum += ys[i];
                    if (ys[i] > 0) {
                        lo = Math.min(lo, i + minY);
                        hi = Math.max(hi, i + minY);
                    }
                }
            }
            int veins = veins(orePositions.get(b));
            log("mining: {}: {} blocchi ({} per chunk), y {}..{}, filoni connessi ~{} ({} per chunk)", s, sum, String.format("%.2f", sum / (double) chunks),
                    sum == 0 ? "-" : lo, sum == 0 ? "-" : hi, veins, String.format("%.2f", veins / (double) chunks));
            if (ys != null && sum > 0) {
                TreeMap<Integer, Integer> bands = new TreeMap<>();
                for (int i = 0; i < height; i++) if (ys[i] > 0) bands.merge(Math.floorDiv(i + minY, 16) * 16, ys[i], Integer::sum);
                log("mining: {}: blocchi per fascia di 16 y {}", s, bands);
            }
        }
        // altri minerali presenti (id con "ore" nel nome), per sanità
        TreeMap<String, Integer> ores = new TreeMap<>();
        for (var e : perY.entrySet()) {
            ResourceLocation key = BuiltInRegistries.BLOCK.getKey(e.getKey());
            if (key.getPath().contains("ore") && !key.getPath().contains("shore")) ores.put(key.toString(), Arrays.stream(e.getValue()).sum());
        }
        log("mining: tutti i blocchi 'ore' presenti (totale e per chunk): {}", ores);
        TreeMap<String, Integer> top = new TreeMap<>();
        for (var e : perY.entrySet()) top.put(BuiltInRegistries.BLOCK.getKey(e.getKey()).toString(), Arrays.stream(e.getValue()).sum());
        log("mining: composizione totale della regione: {}", top.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue()).limit(25).toList());
        helper.assertTrue(!perY.isEmpty(), "nessun blocco contato");
        helper.succeed();
    }

    /** Componenti connesse (26 vicini) di un insieme di posizioni: i filoni, circa. */
    private static int veins(Set<Long> positions) {
        if (positions == null) return 0;
        Set<Long> seen = new HashSet<>();
        int count = 0;
        for (long start : positions) {
            if (!seen.add(start)) continue;
            count++;
            ArrayDeque<Long> queue = new ArrayDeque<>();
            queue.add(start);
            while (!queue.isEmpty()) {
                long p = queue.poll();
                int x = BlockPos.getX(p), y = BlockPos.getY(p), z = BlockPos.getZ(p);
                for (int dx = -1; dx <= 1; dx++)
                    for (int dy = -1; dy <= 1; dy++)
                        for (int dz = -1; dz <= 1; dz++) {
                            long q = BlockPos.asLong(x + dx, y + dy, z + dz);
                            if (positions.contains(q) && seen.add(q)) queue.add(q);
                        }
            }
        }
        return count;
    }
}
