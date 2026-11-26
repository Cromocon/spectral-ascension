package cromocon.spectralascension.gametest;

import cromocon.spectralascension.SpectralAscension;
import cromocon.spectralascension.item.BidentMode;
import cromocon.spectralascension.item.PrismaticTrail;
import cromocon.spectralascension.item.SABident;
import cromocon.spectralascension.item.SATier;
import cromocon.spectralascension.item.SATools;
import cromocon.spectralascension.item.WorkstaffOption;
import de.dafuqs.spectrum.entity.entity.BidentBaseEntity;
import de.dafuqs.spectrum.entity.entity.BidentMirrorImageEntity;
import de.dafuqs.spectrum.items.tools.GlassCrestCrossbowItem;
import de.dafuqs.spectrum.items.tools.WorkstaffItem;
import de.dafuqs.spectrum.registries.SpectrumDataComponentTypes;
import de.dafuqs.spectrum.registries.SpectrumEnchantmentKeys;
import de.dafuqs.spectrum.registries.SpectrumItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static cromocon.spectralascension.gametest.SATestUtil.*;

/**
 * Funzioni degli oggetti come le usa un giocatore, per ogni tier (T6, T7, T8, Alloy): cesoie (sheep, foglie, ragnatele,
 * dispenser), ascia, pala, zappa, spada e spadone (sweep, ground slam), arco, canna, workstaff (cosa scava, Silk
 * Touch), bident (lancio, Fractal, Riptide, Loyalty) e balestra (munizioni, Piercing, Quick Charge, overcharge).
 * Ogni test raccoglie tutti i problemi di tutti i tier e li scrive nel log prima di fallire.
 */
@GameTestHolder(SpectralAscension.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SAFunctionTests {
    private static final String EMPTY = "empty";
    private static final List<SATier> TIERS = List.of(SATier.T6, SATier.T7, SATier.T8, SATier.ALLOY);

    private SAFunctionTests() {
    }

    /** L'oggetto di un tier, es. item(T7, "shears") = umbral_shears. */
    static Item item(SATier tier, String kind) {
        Item item = BuiltInRegistries.ITEM.get(SpectralAscension.id(tier.idPrefix() + "_" + kind));
        if (item == Items.AIR) throw new IllegalStateException("oggetto mancante " + tier.idPrefix() + "_" + kind);
        return item;
    }

    private static <T extends Mob> T spawn(GameTestHelper helper, EntityType<T> type, Vec3 pos) {
        T mob = type.create(helper.getLevel());
        mob.moveTo(pos.x, pos.y, pos.z, 0, 0);
        mob.setNoAi(true);
        mob.setPersistenceRequired();
        helper.getLevel().addFreshEntity(mob);
        return mob;
    }

    private static List<ItemEntity> drops(GameTestHelper helper, BlockPos pos) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2));
    }

    private static boolean dropped(GameTestHelper helper, BlockPos pos, Predicate<ItemStack> what) {
        return drops(helper, pos).stream().anyMatch(e -> what.test(e.getItem()));
    }

    private static UseOnContext useOn(ServerPlayer player, BlockPos pos, Direction face) {
        return new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), face, pos, false));
    }

    /**
     * Area libera di 5×6×5 sopra la struttura, un livello per caso (riga). Le strutture dei test distano circa 6 blocchi
     * l'una dall'altra: con raggio 2 le aree non toccano i vicini, e da y 110 in su stanno sopra quelle di SAGameTests
     * (le frecce del test della balestra le attraversano).
     */
    static BlockPos slot(GameTestHelper helper, int row) {
        BlockPos base = helper.absolutePos(new BlockPos(0, 110 + 8 * row, 0));
        for (BlockPos p : BlockPos.betweenClosed(base.offset(-2, -1, -2), base.offset(2, 5, 2))) {
            helper.getLevel().setBlockAndUpdate(p, p.getY() == base.getY() - 1 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
        }
        return base;
    }

    // ---------- Statistiche ----------

    /**
     * Danno totale (attributo + 1 della mano) e velocità d'attacco attesi per ogni arma e attrezzo, dalla tabella di
     * SATier e SAItems (regola +25% per tier); velocità della freccia della balestra 2,0 / 2,5 / 3,1 / 3,9 (× la glass crest
     * 1,5 come base); incantabilità del tier.
     */
    /** Tipi di shooting star: pesi 10/10/10/35/35 % (mixin ShootingStarVariantMixin), a seed fisso, ±1 punto. */
    @GameTest(template = EMPTY)
    public static void shooting_star_weights(GameTestHelper helper) {
        final int n = 200000;
        net.minecraft.util.RandomSource random = net.minecraft.util.RandomSource.create(12345L);
        java.util.EnumMap<de.dafuqs.spectrum.blocks.shooting_star.ShootingStar.Variant, Integer> counts = new java.util.EnumMap<>(de.dafuqs.spectrum.blocks.shooting_star.ShootingStar.Variant.class);
        for (int i = 0; i < n; i++) counts.merge(de.dafuqs.spectrum.blocks.shooting_star.ShootingStar.Variant.getWeightedRandomType(random), 1, Integer::sum);
        Map<de.dafuqs.spectrum.blocks.shooting_star.ShootingStar.Variant, Double> target = Map.of(
                de.dafuqs.spectrum.blocks.shooting_star.ShootingStar.Variant.FIERY, 10.0,
                de.dafuqs.spectrum.blocks.shooting_star.ShootingStar.Variant.PRISTINE, 10.0,
                de.dafuqs.spectrum.blocks.shooting_star.ShootingStar.Variant.GLISTERING, 10.0,
                de.dafuqs.spectrum.blocks.shooting_star.ShootingStar.Variant.COLORFUL, 35.0,
                de.dafuqs.spectrum.blocks.shooting_star.ShootingStar.Variant.GEMSTONE, 35.0);
        List<String> problems = new ArrayList<>();
        for (var e : target.entrySet()) {
            double pct = counts.getOrDefault(e.getKey(), 0) * 100.0 / n;
            SpectralAscension.LOGGER.info("[shooting_star_weights] {} = {}% (atteso {}%)", e.getKey(), pct, e.getValue());
            if (Math.abs(pct - e.getValue()) > 1.0) problems.add(e.getKey() + " " + pct + "% invece di " + e.getValue() + "%");
        }
        if (!problems.isEmpty()) helper.fail("pesi shooting star fuori dal ±1%: " + problems);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void stats_table(GameTestHelper helper) {
        List<String> problems = new ArrayList<>(), report = new ArrayList<>();
        // tipo → {danno T6, T7, T8, Alloy, velocità d'attacco}
        Map<String, double[]> table = Map.of(
                "sword", new double[]{19, 24, 30, 37, 1.6},
                "axe", new double[]{20, 25, 31, 39, 1.0},
                "shovel", new double[]{16, 20, 25, 31, 1.0},
                "hoe", new double[]{17, 21, 27, 33, 4.0},
                "ultra_greatsword", new double[]{20, 25, 31, 39, 1.2},
                "workstaff", new double[]{16, 20, 25, 31, 1.2},
                // mazza (2026-11-25): come lo spadone, velocità vanilla 0,6
                "mace", new double[]{20, 25, 31, 39, 0.6},
                // bident: il parametro "danno" di SAItems (15 / 18 / 21 / 24, NON segue il +25%) è l'attributo; col +1 della
                // mano 16 / 19 / 22 / 25
                "bident", new double[]{16, 19, 22, 25, 1.8});
        int speedCol = TIERS.size();
        for (var e : table.entrySet()) {
            for (int t = 0; t < TIERS.size(); t++) {
                Item it = item(TIERS.get(t), e.getKey());
                ItemStack st = new ItemStack(it);
                double[] dmg = {1}, spd = {4};
                st.forEachModifier(net.minecraft.world.entity.EquipmentSlot.MAINHAND, (attr, mod) -> {
                    if (attr.equals(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)) dmg[0] += mod.amount();
                    if (attr.equals(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED)) spd[0] += mod.amount();
                });
                if (Math.abs(dmg[0] - e.getValue()[t]) > 0.01) problems.add(id(it) + ": danno totale " + dmg[0] + " invece di " + e.getValue()[t]);
                if (Math.abs(spd[0] - e.getValue()[speedCol]) > 0.01) problems.add(id(it) + ": velocità d'attacco " + spd[0] + " invece di " + e.getValue()[speedCol]);
                report.add(String.format(java.util.Locale.ROOT, "%s: danno %.1f, velocità %.2f", id(it), dmg[0], spd[0]));
            }
        }
        double[] arrow = {2.0, 2.5, 3.1, 3.9};
        for (int t = 0; t < TIERS.size(); t++) {
            var c = (SATools.Crossbow) item(TIERS.get(t), "crossbow");
            ServerPlayer p = mockPlayer(helper);
            float v = c.getProjectileVelocityModifier(new ItemStack(c), p);
            // GlassCrestCrossbowItem: modificatore 1,0 della glass crest = velocità 1,5... il nostro scala di velocity/1,5
            float glass = SpectrumItems.GLASS_CREST_CROSSBOW.get().getProjectileVelocityModifier(new ItemStack(SpectrumItems.GLASS_CREST_CROSSBOW.get()), p);
            double ratio = v / glass;
            if (Math.abs(ratio - arrow[t] / 1.5) > 0.01) problems.add(id(c) + ": velocità della freccia " + ratio + " × glass crest invece di " + (arrow[t] / 1.5));
            report.add(String.format(java.util.Locale.ROOT, "%s: freccia × %.3f rispetto alla glass crest", id(c), ratio));
        }
        for (var holder : cromocon.spectralascension.registry.SAItems.equipment()) {
            Item it = holder.get();
            SATier tier = TIERS.stream().filter(t -> id(it).startsWith(t.idPrefix() + "_")).findFirst().orElseThrow();
            int ev = new ItemStack(it).getEnchantmentValue();
            if (ev != tier.getEnchantmentValue()) problems.add(id(it) + ": incantabilità " + ev + " invece di " + tier.getEnchantmentValue());
        }
        finish(helper, "tabella delle statistiche", problems, report);
    }

    // ---------- Cesoie ----------

    /**
     * Cesoie in mano: tosano la pecora (Player.interactOn, come il clic destro), raccolgono foglie e ragnatele intere
     * (in sopravvivenza le foglie e la ragnatela cadono solo con le cesoie) e tagliano più in fretta della mano.
     */
    @GameTest(template = EMPTY)
    public static void shears_use(GameTestHelper helper) {
        List<String> problems = new ArrayList<>(), report = new ArrayList<>();
        int row = 0;
        for (SATier tier : TIERS) {
            Item shears = item(tier, "shears");
            String id = id(shears);
            BlockPos base = slot(helper, row++);
            ServerPlayer player = mockPlayer(helper);
            aim(player, base, 0, 0);
            player.setItemInHand(InteractionHand.MAIN_HAND, enchanted(helper, shears));
            Sheep sheep = spawn(helper, EntityType.SHEEP, Vec3.atBottomCenterOf(base.offset(0, 0, 1)));
            var result = player.interactOn(sheep, InteractionHand.MAIN_HAND);
            if (!sheep.isSheared()) problems.add(id + ": la pecora non è stata tosata (" + result + ")");
            if (!dropped(helper, base, s -> s.is(Items.WHITE_WOOL))) problems.add(id + ": nessuna lana a terra dopo la tosatura");
            clearAround(helper, base, 4);
            for (Block block : List.of(Blocks.OAK_LEAVES, Blocks.COBWEB, Blocks.VINE)) {
                BlockPos pos = base.offset(2, 0, 0);
                BlockState state = block == Blocks.VINE ? Blocks.VINE.defaultBlockState().setValue(net.minecraft.world.level.block.VineBlock.UP, true)
                        : block.defaultBlockState();
                if (block == Blocks.VINE) helper.getLevel().setBlockAndUpdate(pos.above(), Blocks.STONE.defaultBlockState());
                helper.getLevel().setBlockAndUpdate(pos, state);
                float speed = player.getMainHandItem().getDestroySpeed(state);
                if (!(speed > 1.0F)) problems.add(id + ": velocità su " + block + " = " + speed);
                player.gameMode.destroyBlock(pos);
                if (!dropped(helper, pos, s -> s.is(block.asItem()))) problems.add(id + ": rompendo " + block + " non cade il blocco intero");
                helper.getLevel().setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
                clearAround(helper, pos, 3);
            }
            report.add(id + ": pecora tosata, foglie / ragnatela / liane raccolte");
        }
        finish(helper, "cesoie in mano", problems, report);
    }

    /**
     * Cesoie in un dispenser davanti a una pecora: quando il dispenser scatta (DispenserBlock.dispenseFrom, quello che
     * fa il segnale di redstone) la tosano e restano dentro, come le cesoie vanilla e bedrock.
     */
    @GameTest(template = EMPTY)
    public static void shears_dispenser(GameTestHelper helper) throws ReflectiveOperationException {
        List<String> problems = new ArrayList<>(), report = new ArrayList<>();
        var dispense = DispenserBlock.class.getDeclaredMethod("dispenseFrom", ServerLevel.class, BlockState.class, BlockPos.class);
        dispense.setAccessible(true);
        int row = 0;
        for (Item shears : List.of(Items.SHEARS, item(SATier.T6, "shears"), item(SATier.T7, "shears"), item(SATier.T8, "shears"),
                item(SATier.ALLOY, "shears"))) {
            String id = id(shears);
            BlockPos base = slot(helper, row++);
            BlockPos disp = base.offset(0, 0, -2);
            BlockState state = Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.SOUTH);
            helper.getLevel().setBlockAndUpdate(disp, state);
            var be = (DispenserBlockEntity) helper.getLevel().getBlockEntity(disp);
            be.setItem(0, enchanted(helper, shears));
            Sheep sheep = spawn(helper, EntityType.SHEEP, Vec3.atBottomCenterOf(disp.south()));
            dispense.invoke(Blocks.DISPENSER, helper.getLevel(), state, disp);
            boolean kept = be.getItem(0).is(shears);
            if (!sheep.isSheared()) problems.add(id + ": il dispenser non ha tosato la pecora (cesoie " + (kept ? "ancora dentro" : "lanciate fuori") + ")");
            if (!kept) problems.add(id + ": il dispenser ha sparato fuori le cesoie invece di usarle");
            report.add(id + ": pecora " + (sheep.isSheared() ? "tosata" : "NON tosata") + ", cesoie " + (kept ? "rimaste dentro" : "uscite"));
            clearAround(helper, base, 3);
        }
        finish(helper, "cesoie nel dispenser", problems, report);
    }

    // ---------- Ascia, pala, zappa ----------

    /**
     * Clic destro: l'ascia scortica il tronco, la pala fa il sentiero sull'erba, la zappa ara la terra. In più ogni
     * attrezzo scava il suo blocco più in fretta della mano (tronco, terra, foglie) e la zappa raccoglie il grano maturo.
     */
    @GameTest(template = EMPTY)
    public static void tool_right_click(GameTestHelper helper) {
        List<String> problems = new ArrayList<>(), report = new ArrayList<>();
        int row = 0;
        record Case(String kind, BlockState from, BlockState expected, BlockState mined) {
        }
        List<Case> cases = List.of(
                new Case("axe", Blocks.OAK_LOG.defaultBlockState(), Blocks.STRIPPED_OAK_LOG.defaultBlockState(), Blocks.OAK_LOG.defaultBlockState()),
                new Case("shovel", Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.DIRT_PATH.defaultBlockState(), Blocks.DIRT.defaultBlockState()),
                new Case("hoe", Blocks.DIRT.defaultBlockState(), Blocks.FARMLAND.defaultBlockState(), Blocks.OAK_LEAVES.defaultBlockState()),
                new Case("workstaff", Blocks.OAK_LOG.defaultBlockState(), Blocks.STRIPPED_OAK_LOG.defaultBlockState(), Blocks.STONE.defaultBlockState()));
        for (SATier tier : TIERS) {
            BlockPos base = slot(helper, row++);
            ServerPlayer player = mockPlayer(helper);
            aim(player, base, 0, 0);
            int col = -2;
            for (Case c : cases) {
                Item tool = item(tier, c.kind());
                String id = id(tool);
                player.setItemInHand(InteractionHand.MAIN_HAND, enchanted(helper, tool));
                BlockPos pos = base.offset(col++, 0, 2);
                helper.getLevel().setBlockAndUpdate(pos, c.from());
                var result = tool.useOn(useOn(player, pos, Direction.UP));
                BlockState now = helper.getLevel().getBlockState(pos);
                if (now.getBlock() != c.expected().getBlock()) problems.add(id + ": clic destro su " + c.from().getBlock() + " dà " + now.getBlock() + " (" + result + ")");
                float speed = player.getMainHandItem().getDestroySpeed(c.mined());
                if (!(speed >= tier.getSpeed())) problems.add(id + ": velocità su " + c.mined().getBlock() + " = " + speed + " invece di almeno " + tier.getSpeed());
                report.add(id + ": " + c.from().getBlock() + " → " + now.getBlock() + ", velocità " + speed);
            }
            // Zappa: grano maturo → grano e semi.
            Item hoe = item(tier, "hoe");
            player.setItemInHand(InteractionHand.MAIN_HAND, enchanted(helper, hoe));
            BlockPos farm = base.offset(2, 0, 2);
            helper.getLevel().setBlockAndUpdate(farm.below(), Blocks.FARMLAND.defaultBlockState());
            helper.getLevel().setBlockAndUpdate(farm, ((CropBlock) Blocks.WHEAT).getStateForAge(7));
            player.gameMode.destroyBlock(farm);
            if (!dropped(helper, farm, s -> s.is(Items.WHEAT))) problems.add(id(hoe) + ": il grano maturo non dà grano");
            clearAround(helper, base, 5);
        }
        finish(helper, "clic destro e scavo degli attrezzi", problems, report);
    }

    // ---------- Spada e spadone ----------

    /**
     * Colpo pieno da fermo a uno zombie con altri due accanto: la spada e lo spadone feriscono il bersaglio e, con lo
     * sweep, anche i vicini. Il danno del bersaglio è circa quello dell'attributo (meno l'armatura dello zombie).
     */
    @GameTest(template = EMPTY)
    public static void sword_sweep(GameTestHelper helper) {
        List<String> problems = new ArrayList<>(), report = new ArrayList<>();
        int row = 0;
        for (SATier tier : TIERS) {
            for (String kind : List.of("sword", "ultra_greatsword")) {
                Item weapon = item(tier, kind);
                String id = id(weapon);
                BlockPos base = slot(helper, row++);
                ServerPlayer player = mockPlayer(helper);
                aim(player, base, 0, 0);
                player.setItemInHand(InteractionHand.MAIN_HAND, enchanted(helper, weapon));
                // gli attributi dell'oggetto in mano entrano col tick: qui li applichiamo subito
                player.getMainHandItem().forEachModifier(net.minecraft.world.entity.EquipmentSlot.MAINHAND,
                        (attr, mod) -> player.getAttribute(attr).addOrUpdateTransientModifier(mod));
                player.setOnGround(true);
                chargeAttack(player);
                var target = spawn(helper, EntityType.ZOMBIE, Vec3.atBottomCenterOf(base.offset(0, 0, 2)));
                var left = spawn(helper, EntityType.ZOMBIE, Vec3.atBottomCenterOf(base.offset(1, 0, 2)));
                var right = spawn(helper, EntityType.ZOMBIE, Vec3.atBottomCenterOf(base.offset(-1, 0, 2)));
                for (var z : List.of(target, left, right)) {
                    z.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000);
                    z.setHealth(1000);
                }
                player.attack(target);
                float dmg = 1000 - target.getHealth();
                float sl = 1000 - left.getHealth(), sr = 1000 - right.getHealth();
                double attr = player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
                if (!(dmg > attr * 0.5)) problems.add(id + ": danno al bersaglio " + dmg + " (attributo " + attr + ")");
                if (!(sl > 0 && sr > 0)) problems.add(id + ": niente sweep sui vicini (" + sl + ", " + sr + ")");
                report.add(String.format(java.util.Locale.ROOT, "%s: bersaglio -%.1f (attributo %.1f), vicini -%.1f / -%.1f", id, dmg, attr, sl, sr));
                clearAround(helper, base, 5);
            }
        }
        finish(helper, "sweep di spada e spadone", problems, report);
    }

    /**
     * Ground slam dello spadone (clic destro tenuto: GlassCrestGreatswordItem): con l'Ink sbloccato e la palette
     * piena, alla fine della carica ferisce gli zombie attorno e scala White Ink.
     */
    @GameTest(template = EMPTY)
    public static void greatsword_ground_slam(GameTestHelper helper) {
        List<String> problems = new ArrayList<>(), report = new ArrayList<>();
        int row = 0;
        for (SATier tier : TIERS) {
            var sword = (SATools.UltraGreatsword) item(tier, "ultra_greatsword");
            String id = id(sword);
            BlockPos base = slot(helper, row++);
            ServerPlayer player = mockPlayer(helper);
            aim(player, base, 0, 0);
            unlockInk(helper, player);
            player.getInventory().setItem(9, fullPalette());
            ItemStack stack = enchanted(helper, sword);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            var z = spawn(helper, EntityType.ZOMBIE, Vec3.atBottomCenterOf(base.offset(2, 0, 0)));
            float before = z.getHealth();
            var res = sword.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            if (!player.isUsingItem()) problems.add(id + ": il ground slam non parte (" + res.getResult() + ")");
            sword.finishUsingItem(player.getMainHandItem(), helper.getLevel(), player);
            player.stopUsingItem();
            if (!(z.getHealth() < before)) problems.add(id + ": lo zombie a 2 blocchi non è stato colpito dal ground slam");
            report.add(id + ": zombie " + before + " → " + z.getHealth());
            clearAround(helper, base, 6);
        }
        finish(helper, "ground slam", problems, report);
    }

    // ---------- Arco e canna ----------

    /**
     * Arco: con una freccia nell'inventario, caricato 20 tick e rilasciato tira una freccia del giocatore (la freccia si
     * consuma), più veloce di un arco vanilla nelle stesse condizioni; senza frecce non parte nemmeno.
     */
    @GameTest(template = EMPTY)
    public static void bow_shoot(GameTestHelper helper) {
        List<String> problems = new ArrayList<>(), report = new ArrayList<>();
        int row = 0;
        double vanilla = shootBow(helper, Items.BOW, slot(helper, row++), problems);
        for (SATier tier : TIERS) {
            Item bow = item(tier, "bow");
            double v = shootBow(helper, bow, slot(helper, row++), problems);
            if (!(v > vanilla)) problems.add(id(bow) + ": freccia a " + v + " b/t, non più veloce dell'arco vanilla (" + vanilla + ")");
            report.add(String.format(java.util.Locale.ROOT, "%s: freccia a %.2f b/t (vanilla %.2f)", id(bow), v, vanilla));
            // senza frecce
            ServerPlayer empty = mockPlayer(helper);
            empty.setItemInHand(InteractionHand.MAIN_HAND, enchanted(helper, bow));
            bow.use(helper.getLevel(), empty, InteractionHand.MAIN_HAND);
            if (empty.isUsingItem()) problems.add(id(bow) + ": si carica senza frecce in sopravvivenza");
        }
        finish(helper, "arco", problems, report);
    }

    private static double shootBow(GameTestHelper helper, Item bow, BlockPos base, List<String> problems) {
        ServerPlayer player = mockPlayer(helper);
        aim(player, base, 0, 0);
        player.getInventory().setItem(9, new ItemStack(Items.ARROW, 2));
        ItemStack stack = enchanted(helper, bow);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        bow.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        if (!player.isUsingItem()) {
            problems.add(id(bow) + ": non si carica con una freccia nell'inventario");
            return 0;
        }
        stack.releaseUsing(helper.getLevel(), player, stack.getUseDuration(player) - 20);
        player.stopUsingItem();
        var arrows = helper.getLevel().getEntitiesOfClass(AbstractArrow.class, new AABB(base).inflate(4), a -> a.getOwner() == player);
        if (arrows.size() != 1) {
            problems.add(id(bow) + ": frecce tirate " + arrows.size());
            return 0;
        }
        if (player.getInventory().countItem(Items.ARROW) != 1) problems.add(id(bow) + ": la freccia non è stata consumata");
        double v = arrows.getFirst().getDeltaMovement().length();
        clearAround(helper, base, 5);
        return v;
    }

    /**
     * Scia delle frecce T9 ({@link PrismaticTrail}), lato server: le frecce tirate dall'arco e dalla balestra Prismatic
     * (anche spettrali, tipped, le tre del Multishot con Piercing) hanno il marcatore che il client usa per le
     * scintille; quelle dell'arco vanilla, dell'arco e della balestra T8 no. Le particelle si guardano nello scenario
     * client {@code scia_frecce}.
     */
    @GameTest(template = EMPTY)
    public static void prismatic_trail_marker(GameTestHelper helper) {
        List<String> problems = new ArrayList<>(), report = new ArrayList<>();
        var registry = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        int row = 0;
        record Shot(String what, ItemStack weapon, ItemStack ammo, boolean expected) {
        }
        ItemStack multishot = chained(helper, SATier.ALLOY, "crossbow");
        multishot.enchant(registry.getOrThrow(Enchantments.MULTISHOT), 1);
        List<Shot> shots = List.of(
                new Shot("arco vanilla", new ItemStack(Items.BOW), new ItemStack(Items.ARROW), false),
                new Shot("arco T8", enchanted(helper, item(SATier.T8, "bow")), new ItemStack(Items.ARROW), false),
                new Shot("arco T9", enchanted(helper, item(SATier.ALLOY, "bow")), new ItemStack(Items.ARROW), true),
                new Shot("arco T9 + freccia spettrale", enchanted(helper, item(SATier.ALLOY, "bow")), new ItemStack(Items.SPECTRAL_ARROW), true),
                new Shot("arco T9 + freccia tipped", enchanted(helper, item(SATier.ALLOY, "bow")),
                        net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.TIPPED_ARROW, net.minecraft.world.item.alchemy.Potions.SLOWNESS), true),
                new Shot("balestra T8", chained(helper, SATier.T8, "crossbow"), new ItemStack(Items.ARROW), false),
                new Shot("balestra T9", chained(helper, SATier.ALLOY, "crossbow"), new ItemStack(Items.ARROW), true),
                new Shot("balestra T9 + freccia di vetro", chained(helper, SATier.ALLOY, "crossbow"), new ItemStack(SpectrumItems.TOPAZ_GLASS_ARROW.get()), true),
                new Shot("balestra T9 + Multishot", multishot, new ItemStack(Items.ARROW), true));
        for (Shot shot : shots) {
            BlockPos base = slot(helper, row++);
            ServerPlayer player = mockPlayer(helper);
            aim(player, base, 0, -10);
            player.getInventory().setItem(9, shot.ammo().copy());
            ItemStack stack = shot.weapon();
            Item weapon = stack.getItem();
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            weapon.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            if (weapon instanceof CrossbowItem) {
                stack.releaseUsing(helper.getLevel(), player, 0);
                player.stopUsingItem();
                if (!CrossbowItem.isCharged(player.getMainHandItem())) {
                    problems.add(shot.what() + ": la balestra non si carica");
                    continue;
                }
                weapon.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            } else {
                stack.releaseUsing(helper.getLevel(), player, stack.getUseDuration(player) - 20);
                player.stopUsingItem();
            }
            var arrows = helper.getLevel().getEntitiesOfClass(AbstractArrow.class, new AABB(base).inflate(6), a -> a.getOwner() == player);
            int expectedCount = stack == multishot ? 3 : 1;
            if (arrows.size() != expectedCount) problems.add(shot.what() + ": frecce tirate " + arrows.size() + " invece di " + expectedCount);
            long marked = arrows.stream().filter(PrismaticTrail::isMarked).count();
            if (shot.expected() && marked != arrows.size()) problems.add(shot.what() + ": frecce con la scia " + marked + "/" + arrows.size());
            if (!shot.expected() && marked != 0) problems.add(shot.what() + ": " + marked + " frecce con la scia (non dovrebbero averla)");
            report.add(shot.what() + ": " + arrows.size() + " frecce (" + arrows.stream().map(a -> BuiltInRegistries.ENTITY_TYPE.getKey(a.getType()).getPath()).distinct().toList()
                    + "), con la scia " + marked);
            clearAround(helper, base, 8);
        }
        finish(helper, "scia delle frecce T9", problems, report);
    }

    /**
     * Canna: il clic destro lancia l'amo (player.fishing), il secondo lo recupera. L'amo prende Luck of the Sea e Lure
     * dagli incantesimi della canna (campi luck e lureSpeed di FishingHook).
     */
    @GameTest(template = EMPTY)
    public static void fishing_rod_cast(GameTestHelper helper) throws ReflectiveOperationException {
        List<String> problems = new ArrayList<>(), report = new ArrayList<>();
        // La canna bedrock (e quindi la nostra) lancia un SpectrumFishingHook, che ha i suoi campi luck e lureSpeed.
        Field luck = de.dafuqs.spectrum.entity.entity.SpectrumFishingHook.class.getDeclaredField("luck");
        Field lure = de.dafuqs.spectrum.entity.entity.SpectrumFishingHook.class.getDeclaredField("lureSpeed");
        luck.setAccessible(true);
        lure.setAccessible(true);
        var registry = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        int row = 0;
        for (SATier tier : TIERS) {
            Item rod = item(tier, "fishing_rod");
            String id = id(rod);
            BlockPos base = slot(helper, row++);
            ServerPlayer player = mockPlayer(helper);
            aim(player, base, 0, 0);
            ItemStack stack = chained(helper, tier, "fishing_rod");
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            rod.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            FishingHook hook = player.fishing;
            if (!(hook instanceof de.dafuqs.spectrum.entity.entity.SpectrumFishingHook)) {
                problems.add(id + ": il lancio non crea l'amo di Spectrum (" + hook + ")");
                continue;
            }
            int expLuck = stack.getEnchantments().getLevel(registry.getOrThrow(Enchantments.LUCK_OF_THE_SEA));
            int expLure = stack.getEnchantments().getLevel(registry.getOrThrow(Enchantments.LURE));
            int l = luck.getInt(hook), s = lure.getInt(hook);
            if (l < expLuck) problems.add(id + ": luck dell'amo " + l + " invece di " + expLuck);
            if (expLure > 0 && s <= 0) problems.add(id + ": lure dell'amo " + s + " con Lure " + expLure);
            rod.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            if (player.fishing != null) problems.add(id + ": il secondo clic non recupera l'amo");
            report.add(id + ": amo lanciato (luck " + l + ", lure " + s + ") e recuperato");
            clearAround(helper, base, 6);
        }
        finish(helper, "canna da pesca", problems, report);
    }

    // ---------- Workstaff ----------

    /**
     * Workstaff = piccone del tier: raccoglie (drop corretto) i blocchi giusti e scava in fretta pietra, terra, tronchi.
     * Minerali ATM: T6 l'allthemodium ma non il vibranium, T7 il vibranium ma non l'unobtainium, T8 e Alloy anche l'unobtainium.
     * Rotture vere: ossidiana e ancient debris cadono, con Silk Touch (dal menu) la pietra cade come pietra.
     */
    @GameTest(template = EMPTY)
    public static void workstaff_mining(GameTestHelper helper) {
        List<String> problems = new ArrayList<>(), report = new ArrayList<>();
        // Come gli attrezzi ATM dello stesso metallo (tag c:incorrect_for_<metallo>_tool di Allthemodium 3.2.1): il
        // piccone di allthemodium scava il vibranium, quello di vibranium l'unobtainium, quello di unobtainium la
        // pietra antica; i blocchi di lega (c:needs_allthemodiumalloy_tool... vuoto) e la pietra antica restano sopra.
        Map<SATier, Map<String, Boolean>> ores = Map.of(
                SATier.T6, Map.of("allthemodium:allthemodium_ore", true, "allthemodium:vibranium_ore", true,
                        "allthemodium:unobtainium_ore", false, "allthemodium:ancient_stone_bricks", false),
                SATier.T7, Map.of("allthemodium:allthemodium_ore", true, "allthemodium:vibranium_ore", true,
                        "allthemodium:unobtainium_ore", true, "allthemodium:ancient_stone_bricks", false),
                SATier.T8, Map.of("allthemodium:allthemodium_ore", true, "allthemodium:vibranium_ore", true,
                        "allthemodium:unobtainium_ore", true, "allthemodium:ancient_stone_bricks", true),
                // Alloy: tag degli attrezzi Alloy di ATM (c:incorrect_for_allthemodiumalloy_tool), scava tutto come il T8.
                SATier.ALLOY, Map.of("allthemodium:allthemodium_ore", true, "allthemodium:vibranium_ore", true,
                        "allthemodium:unobtainium_ore", true, "allthemodium:ancient_stone_bricks", true));
        List<Block> must = List.of(Blocks.STONE, Blocks.DEEPSLATE, Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN, Blocks.ANCIENT_DEBRIS,
                Blocks.DIAMOND_ORE, Blocks.NETHERITE_BLOCK, Blocks.IRON_BLOCK);
        List<Block> fast = List.of(Blocks.STONE, Blocks.DIRT, Blocks.GRAVEL, Blocks.SAND, Blocks.OAK_LOG, Blocks.OAK_PLANKS, Blocks.OBSIDIAN);
        int row = 0;
        for (SATier tier : TIERS) {
            var staff = (SATools.Workstaff) item(tier, "workstaff");
            String id = id(staff);
            ItemStack stack = enchanted(helper, staff);
            for (Block b : must) {
                if (!stack.isCorrectToolForDrops(b.defaultBlockState())) problems.add(id + ": non raccoglie " + BuiltInRegistries.BLOCK.getKey(b));
            }
            for (Block b : fast) {
                float speed = stack.getDestroySpeed(b.defaultBlockState());
                if (!(speed >= tier.getSpeed())) problems.add(id + ": velocità su " + BuiltInRegistries.BLOCK.getKey(b) + " = " + speed);
            }
            for (var e : ores.get(tier).entrySet()) {
                Block ore = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(e.getKey()));
                if (ore == Blocks.AIR) {
                    problems.add("minerale inesistente " + e.getKey());
                    continue;
                }
                boolean ok = stack.isCorrectToolForDrops(ore.defaultBlockState());
                if (ok != e.getValue()) problems.add(id + ": " + e.getKey() + (ok ? " raccolto (non dovrebbe)" : " non raccolto"));
            }
            // rotture vere
            BlockPos base = slot(helper, row++);
            ServerPlayer player = mockPlayer(helper);
            aim(player, base, 0, 0);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            // T7+ (dal 2026-11-19): Foundry (T7+) e Inventory Insertion (T8+) spente dal menu, qui si prova lo scavo di
            // base; i drop fusi e messi nell'inventario li prova SAWorkstaffMenuTests.workstaff_foundry_insertion_drops.
            if (staff.hasOwnMenu()) {
                WorkstaffOption.apply(player, stack, WorkstaffOption.FOUNDRY_OFF);
                WorkstaffOption.apply(player, stack, WorkstaffOption.INSERTION_OFF); // rifiutata (e innocua) sul T7
            }
            for (Block b : List.of(Blocks.OBSIDIAN, Blocks.ANCIENT_DEBRIS)) {
                BlockPos pos = base.offset(0, 0, 2);
                helper.getLevel().setBlockAndUpdate(pos, b.defaultBlockState());
                player.gameMode.destroyBlock(pos);
                if (!dropped(helper, pos, s -> s.is(b.asItem()))) problems.add(id + ": rompendo " + BuiltInRegistries.BLOCK.getKey(b) + " non cade niente");
                clearAround(helper, pos, 3);
            }
            BlockPos pos = base.offset(0, 0, 2);
            helper.getLevel().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
            player.gameMode.destroyBlock(pos);
            if (!dropped(helper, pos, s -> s.is(Items.COBBLESTONE))) problems.add(id + ": con Fortune la pietra non dà cobblestone");
            clearAround(helper, pos, 3);
            WorkstaffItem.applyToggle(player, player.getMainHandItem(), WorkstaffItem.GUIToggle.SELECT_SILK_TOUCH);
            helper.getLevel().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
            player.gameMode.destroyBlock(pos);
            if (!dropped(helper, pos, s -> s.is(Items.STONE))) problems.add(id + ": con Silk Touch la pietra non cade intera");
            clearAround(helper, pos, 3);
            report.add(id + ": raccoglie " + must.size() + " blocchi, minerali ATM come atteso, Fortune/Silk Touch ok");
        }
        finish(helper, "scavo della workstaff", problems, report);
    }

    // ---------- Bident ----------

    /**
     * Lancio del bident (clic destro tenuto 20 tick e rilasciato), con Ink sbloccato e palette piena:
     * <ul>
     * <li>spento: parte un BidentEntity col bident, che lascia l'inventario (sopravvivenza);</li>
     * <li>Fractal: parte una copia illusoria (BidentMirrorImageEntity, non raccoglibile) e il bident resta in mano;</li>
     * <li>Ferocious fuori dall'acqua: Riptide pagato in Ink, il giocatore parte in spin e il bident non viene lanciato;</li>
     * <li>Loyalty: l'entità porta il livello di Loyalty del bident.</li>
     * </ul>
     */
    @GameTest(template = EMPTY)
    public static void bident_throw(GameTestHelper helper) throws ReflectiveOperationException {
        List<String> problems = new ArrayList<>(), report = new ArrayList<>();
        Field loyaltyField = ThrownTrident.class.getDeclaredField("ID_LOYALTY");
        loyaltyField.setAccessible(true);
        @SuppressWarnings("unchecked")
        EntityDataAccessor<Byte> loyalty = (EntityDataAccessor<Byte>) loyaltyField.get(null);
        var registry = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        int row = 0;
        for (SATier tier : TIERS) {
            SABident bident = (SABident) item(tier, "bident");
            String id = id(bident);
            for (BidentMode mode : BidentMode.values()) {
                BlockPos base = slot(helper, row++);
                ServerPlayer player = mockPlayer(helper);
                aim(player, base, 0, -10);
                unlockInk(helper, player);
                player.getInventory().setItem(9, fullPalette());
                ItemStack stack = enchanted(helper, bident);
                SABident.applyMode(stack, player, mode);
                if (mode == BidentMode.OFF) stack.enchant(registry.getOrThrow(Enchantments.LOYALTY), 3);
                player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                bident.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
                if (!player.isUsingItem()) {
                    problems.add(id + " " + mode + ": il clic destro non carica il lancio");
                    continue;
                }
                stack.releaseUsing(helper.getLevel(), player, stack.getUseDuration(player) - 20);
                player.stopUsingItem();
                var thrown = helper.getLevel().getEntitiesOfClass(BidentBaseEntity.class, new AABB(base).inflate(6), e -> e.getOwner() == player);
                boolean inHand = player.getMainHandItem().is(bident);
                switch (mode) {
                    case OFF -> {
                        if (thrown.size() != 1 || thrown.getFirst() instanceof BidentMirrorImageEntity) problems.add(id + " spento: entità lanciate " + thrown);
                        else if (thrown.getFirst().getEntityData().get(loyalty) != 3) problems.add(id + ": Loyalty dell'entità " + thrown.getFirst().getEntityData().get(loyalty) + " invece di 3");
                        if (inHand) problems.add(id + " spento: il bident lanciato è ancora in mano");
                        if (!thrown.isEmpty() && thrown.getFirst().pickup != AbstractArrow.Pickup.ALLOWED) problems.add(id + " spento: il bident lanciato non si può raccogliere");
                    }
                    case FRACTAL -> {
                        if (thrown.size() != 1 || !(thrown.getFirst() instanceof BidentMirrorImageEntity)) problems.add(id + " Fractal: entità lanciate " + thrown);
                        if (!inHand) problems.add(id + " Fractal: il bident non è rimasto in mano");
                    }
                    case FEROCIOUS -> {
                        if (!thrown.isEmpty()) problems.add(id + " Ferocious: fuori dall'acqua ha lanciato invece del Riptide");
                        if (!player.isAutoSpinAttack()) problems.add(id + " Ferocious: niente Riptide fuori dall'acqua con l'Ink");
                        if (!inHand) problems.add(id + " Ferocious: il bident non è rimasto in mano");
                    }
                }
                report.add(id + " " + mode + ": lanciati " + thrown.size() + ", in mano " + inHand + ", spin " + player.isAutoSpinAttack());
                clearAround(helper, base, 8);
            }
        }
        finish(helper, "lancio del bident", problems, report);
    }

    // ---------- Balestra ----------

    /**
     * Balestra: carica (clic destro tenuto fino alla fine e rilasciato) con ognuna delle munizioni (freccia, razzo e le
     * sei frecce di vetro di Spectrum), poi il tiro: proiettile del tipo giusto; le frecce hanno il Piercing della
     * balestra. Quick Charge: la carica dura meno di quella vanilla.
     */
    @GameTest(template = EMPTY)
    public static void crossbow_ammo(GameTestHelper helper) {
        List<String> problems = new ArrayList<>(), report = new ArrayList<>();
        List<Item> ammo = List.of(Items.ARROW, Items.FIREWORK_ROCKET, SpectrumItems.MALACHITE_GLASS_ARROW.get(), SpectrumItems.TOPAZ_GLASS_ARROW.get(),
                SpectrumItems.AMETHYST_GLASS_ARROW.get(), SpectrumItems.CITRINE_GLASS_ARROW.get(), SpectrumItems.ONYX_GLASS_ARROW.get(),
                SpectrumItems.MOONSTONE_GLASS_ARROW.get());
        var registry = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        int row = 0;
        for (SATier tier : TIERS) {
            var crossbow = (SATools.Crossbow) item(tier, "crossbow");
            String id = id(crossbow);
            ServerPlayer probe = mockPlayer(helper);
            int vanillaCharge = CrossbowItem.getChargeDuration(new ItemStack(Items.CROSSBOW), probe);
            int charge = CrossbowItem.getChargeDuration(chained(helper, tier, "crossbow"), probe);
            if (!(charge < vanillaCharge)) problems.add(id + ": carica " + charge + " tick, non meno della vanilla " + vanillaCharge);
            List<String> line = new ArrayList<>();
            for (Item a : ammo) {
                BlockPos base = slot(helper, row);
                ServerPlayer player = mockPlayer(helper);
                aim(player, base, 0, -10);
                // il razzo si carica dalla mano secondaria, come nella balestra vanilla (dall'inventario solo frecce)
                if (a == Items.FIREWORK_ROCKET) player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(a));
                else player.getInventory().setItem(9, new ItemStack(a));
                ItemStack stack = chained(helper, tier, "crossbow");
                player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                crossbow.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
                stack.releaseUsing(helper.getLevel(), player, 0);
                player.stopUsingItem();
                String what = id + " + " + BuiltInRegistries.ITEM.getKey(a).getPath();
                if (!CrossbowItem.isCharged(player.getMainHandItem())) {
                    problems.add(what + ": non si carica");
                    continue;
                }
                crossbow.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
                var shot = helper.getLevel().getEntitiesOfClass(Projectile.class, new AABB(base).inflate(6), p -> p.getOwner() == player);
                if (shot.size() != 1) {
                    problems.add(what + ": proiettili " + shot.size());
                } else if (a == Items.FIREWORK_ROCKET) {
                    if (!(shot.getFirst() instanceof FireworkRocketEntity)) problems.add(what + ": tira " + shot.getFirst().getType());
                } else if (shot.getFirst() instanceof AbstractArrow arrow) {
                    int piercing = stack.getEnchantments().getLevel(registry.getOrThrow(Enchantments.PIERCING));
                    if (arrow.getPierceLevel() != piercing) problems.add(what + ": Piercing della freccia " + arrow.getPierceLevel() + " invece di " + piercing);
                    if (a != Items.ARROW && BuiltInRegistries.ENTITY_TYPE.getKey(arrow.getType()).getNamespace().equals("minecraft"))
                        problems.add(what + ": tira una freccia vanilla");
                } else {
                    problems.add(what + ": tira " + shot.getFirst().getType());
                }
                if (CrossbowItem.isCharged(player.getMainHandItem())) problems.add(what + ": resta carica dopo il tiro");
                line.add(BuiltInRegistries.ITEM.getKey(a).getPath() + (shot.size() == 1 ? " → " + BuiltInRegistries.ENTITY_TYPE.getKey(shot.getFirst().getType()).getPath() : " ✗"));
                clearAround(helper, base, 8);
            }
            row++;
            report.add(id + ": carica " + charge + " tick (vanilla " + vanillaCharge + "); " + String.join(", ", line));
        }
        finish(helper, "munizioni della balestra", problems, report);
    }

    /**
     * Overcharge (Shift + clic destro tenuto con la balestra carica, 100 White Ink): rilasciato a metà salva circa il 50%
     * di overcharge, e la freccia tirata dopo va più veloce di quella senza overcharge.
     */
    @GameTest(template = EMPTY)
    public static void crossbow_overcharge(GameTestHelper helper) {
        List<String> problems = new ArrayList<>(), report = new ArrayList<>();
        int row = 0;
        for (SATier tier : TIERS) {
            var crossbow = (SATools.Crossbow) item(tier, "crossbow");
            String id = id(crossbow);
            double[] speeds = new double[2];
            for (int over = 0; over < 2; over++) {
                BlockPos base = slot(helper, row++);
                ServerPlayer player = mockPlayer(helper);
                aim(player, base, 0, -10);
                unlockInk(helper, player);
                player.getInventory().setItem(9, fullPalette());
                ItemStack stack = enchanted(helper, crossbow);
                stack.set(DataComponents.CHARGED_PROJECTILES, net.minecraft.world.item.component.ChargedProjectiles.of(new ItemStack(Items.ARROW)));
                player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                if (over == 1) {
                    player.setShiftKeyDown(true);
                    crossbow.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
                    player.setShiftKeyDown(false);
                    if (!player.isUsingItem()) problems.add(id + ": l'overcharge non parte");
                    stack.releaseUsing(helper.getLevel(), player, stack.getUseDuration(player) / 2);
                    player.stopUsingItem();
                    float oc = GlassCrestCrossbowItem.getOvercharge(stack);
                    if (Math.abs(oc - 0.5F) > 0.05F) problems.add(id + ": overcharge " + oc + " invece di 0,5");
                }
                crossbow.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
                var shot = helper.getLevel().getEntitiesOfClass(AbstractArrow.class, new AABB(base).inflate(10), p -> p.getOwner() == player);
                speeds[over] = shot.isEmpty() ? 0 : shot.getFirst().getDeltaMovement().length();
                if (shot.isEmpty()) problems.add(id + (over == 1 ? " con" : " senza") + " overcharge: nessuna freccia");
                clearAround(helper, base, 12);
            }
            if (!(speeds[1] > speeds[0] * 1.3)) problems.add(id + ": freccia con overcharge " + speeds[1] + " non più veloce di " + speeds[0]);
            report.add(String.format(java.util.Locale.ROOT, "%s: freccia %.2f b/t, con overcharge al 50%% %.2f b/t", id, speeds[0], speeds[1]));
        }
        finish(helper, "overcharge della balestra", problems, report);
    }

    /**
     * Sniping di Spectrum (spectrum:sniping, max 2, solo in conflitto con Multishot): le balestre di tutti i tier lo
     * accettano (#spectrum:enchantable/sniping contiene #minecraft:enchantable/crossbow); T8 lo ha a I e T9 a II di
     * serie, con Quick Charge IV. Spectrum lo applica con il CrossbowShootingCallback di Arrowhead (chiamato da
     * CrossbowItem.shootProjectile): velocità del proiettile × (1 + 0,25 × livello). Misura sulla balestra T9 con
     * Sniping 0, I e II (freccia già caricata, stessi altri incantesimi, media di 10 colpi): rapporti attesi 1,25 e 1,5
     * (tolleranza 0,02).
     */
    @GameTest(template = EMPTY)
    public static void crossbow_sniping(GameTestHelper helper) {
        List<String> problems = new ArrayList<>(), report = new ArrayList<>();
        var registry = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var sniping = registry.getOrThrow(SpectrumEnchantmentKeys.SNIPING);
        var quickCharge = registry.getOrThrow(Enchantments.QUICK_CHARGE);
        Map<SATier, int[]> want = Map.of(SATier.T6, new int[]{0, 3}, SATier.T7, new int[]{0, 4}, SATier.T8, new int[]{1, 4}, SATier.ALLOY, new int[]{2, 4});
        for (SATier tier : TIERS) {
            var crossbow = item(tier, "crossbow");
            ItemStack stack = enchanted(helper, crossbow);
            if (!stack.supportsEnchantment(sniping)) problems.add(id(crossbow) + ": non accetta Sniping");
            int sn = stack.getEnchantments().getLevel(sniping), qc = stack.getEnchantments().getLevel(quickCharge);
            if (sn != want.get(tier)[0] || qc != want.get(tier)[1])
                problems.add(id(crossbow) + ": di serie Sniping " + sn + " e Quick Charge " + qc + " invece di " + want.get(tier)[0] + " e " + want.get(tier)[1]);
        }
        var crossbow = (SATools.Crossbow) item(SATier.ALLOY, "crossbow");
        // media di SHOTS colpi per livello: la dispersione casuale (gaussiana sommata alla direzione prima di scalare)
        // cambia la lunghezza del vettore di circa l'1% a colpo
        final int SHOTS = 10;
        double[] speeds = new double[3];
        for (int level = 0; level <= 2; level++) {
            BlockPos base = slot(helper, level);
            int hits = 0;
            for (int n = 0; n < SHOTS; n++) {
                ServerPlayer player = mockPlayer(helper);
                aim(player, base, 0, -10);
                ItemStack stack = enchanted(helper, crossbow);
                var mutable = new net.minecraft.world.item.enchantment.ItemEnchantments.Mutable(stack.getEnchantments());
                mutable.set(sniping, level);
                stack.set(DataComponents.ENCHANTMENTS, mutable.toImmutable());
                stack.set(DataComponents.CHARGED_PROJECTILES, net.minecraft.world.item.component.ChargedProjectiles.of(new ItemStack(Items.ARROW)));
                player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                crossbow.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
                var shot = helper.getLevel().getEntitiesOfClass(AbstractArrow.class, new AABB(base).inflate(10), p -> p.getOwner() == player);
                if (!shot.isEmpty()) {
                    speeds[level] += shot.getFirst().getDeltaMovement().length();
                    hits++;
                }
                clearAround(helper, base, 12);
            }
            if (hits != SHOTS) problems.add(id(crossbow) + " Sniping " + level + ": " + hits + " frecce su " + SHOTS);
            speeds[level] = hits == 0 ? 0 : speeds[level] / hits;
        }
        for (int level = 1; level <= 2; level++) {
            double ratio = speeds[0] > 0 ? speeds[level] / speeds[0] : 0, expected = 1 + 0.25 * level;
            if (Math.abs(ratio - expected) > 0.02) problems.add(id(crossbow) + " Sniping " + level + ": velocità ×" + ratio + " invece di ×" + expected);
        }
        report.add(String.format(java.util.Locale.ROOT, "%s (media di 10 colpi): freccia con Sniping 0 %.3f b/t, I %.3f b/t (×%.3f), II %.3f b/t (×%.3f)", id(crossbow),
                speeds[0], speeds[1], speeds[1] / speeds[0], speeds[2], speeds[2] / speeds[0]));
        finish(helper, "Sniping della balestra", problems, report);
    }

    // ---------- Mano secondaria ----------

    /**
     * Oggetti nella mano secondaria: l'arco tira, la canna lancia l'amo, il bident si lancia, la balestra carica si
     * spara e le cesoie tosano, tutti con la mano principale vuota.
     */
    @GameTest(template = EMPTY)
    public static void offhand_use(GameTestHelper helper) {
        List<String> problems = new ArrayList<>(), report = new ArrayList<>();
        int row = 0;
        for (SATier tier : TIERS) {
            BlockPos base = slot(helper, row++);
            ServerLevel level = helper.getLevel();
            // arco
            ServerPlayer p1 = mockPlayer(helper);
            aim(p1, base, 0, -10);
            p1.getInventory().setItem(9, new ItemStack(Items.ARROW));
            Item bow = item(tier, "bow");
            ItemStack bowStack = enchanted(helper, bow);
            p1.setItemInHand(InteractionHand.OFF_HAND, bowStack);
            bow.use(level, p1, InteractionHand.OFF_HAND);
            bowStack.releaseUsing(level, p1, bowStack.getUseDuration(p1) - 20);
            p1.stopUsingItem();
            if (level.getEntitiesOfClass(AbstractArrow.class, new AABB(base).inflate(6), a -> a.getOwner() == p1).size() != 1)
                problems.add(id(bow) + ": dalla mano secondaria non tira");
            // canna
            ServerPlayer p2 = mockPlayer(helper);
            aim(p2, base, 0, -10);
            Item rod = item(tier, "fishing_rod");
            p2.setItemInHand(InteractionHand.OFF_HAND, enchanted(helper, rod));
            rod.use(level, p2, InteractionHand.OFF_HAND);
            if (p2.fishing == null) problems.add(id(rod) + ": dalla mano secondaria non lancia l'amo");
            // bident spento
            ServerPlayer p3 = mockPlayer(helper);
            aim(p3, base, 0, -10);
            SABident bident = (SABident) item(tier, "bident");
            ItemStack bStack = enchanted(helper, bident);
            SABident.applyMode(bStack, p3, BidentMode.OFF);
            p3.setItemInHand(InteractionHand.OFF_HAND, bStack);
            bident.use(level, p3, InteractionHand.OFF_HAND);
            bStack.releaseUsing(level, p3, bStack.getUseDuration(p3) - 20);
            p3.stopUsingItem();
            if (level.getEntitiesOfClass(BidentBaseEntity.class, new AABB(base).inflate(6), e -> e.getOwner() == p3).size() != 1)
                problems.add(id(bident) + ": dalla mano secondaria non si lancia");
            if (!p3.getOffhandItem().isEmpty()) problems.add(id(bident) + ": lanciato dalla mano secondaria ma è ancora lì");
            // balestra carica
            ServerPlayer p4 = mockPlayer(helper);
            aim(p4, base, 0, -10);
            Item crossbow = item(tier, "crossbow");
            ItemStack cStack = enchanted(helper, crossbow);
            cStack.set(DataComponents.CHARGED_PROJECTILES, net.minecraft.world.item.component.ChargedProjectiles.of(new ItemStack(Items.ARROW)));
            p4.setItemInHand(InteractionHand.OFF_HAND, cStack);
            crossbow.use(level, p4, InteractionHand.OFF_HAND);
            if (level.getEntitiesOfClass(AbstractArrow.class, new AABB(base).inflate(10), a -> a.getOwner() == p4).size() != 1)
                problems.add(id(crossbow) + ": dalla mano secondaria non spara");
            // cesoie
            ServerPlayer p5 = mockPlayer(helper);
            aim(p5, base, 0, 0);
            Item shears = item(tier, "shears");
            p5.setItemInHand(InteractionHand.OFF_HAND, enchanted(helper, shears));
            Sheep sheep = spawn(helper, EntityType.SHEEP, Vec3.atBottomCenterOf(base.offset(0, 0, 1)));
            p5.interactOn(sheep, InteractionHand.OFF_HAND);
            if (!sheep.isSheared()) problems.add(id(shears) + ": dalla mano secondaria non tosa");
            report.add(tier + ": arco, canna, bident, balestra e cesoie nella mano secondaria provati");
            clearAround(helper, base, 12);
        }
        finish(helper, "mano secondaria", problems, report);
    }

    /**
     * L'oggetto come esce dalla catena di ricette T6 → T7 → T8 → Alloy: alla Fusion Shrine copy_components porta avanti gli
     * incantesimi dei tier precedenti e per lo stesso incantesimo vince il livello più alto (GatedSpectrumRecipeMixin).
     * Es. la balestra T7 ha il Quick Charge IV della T6 anche se la ricetta T7 dà solo Piercing VIII.
     */
    static ItemStack chained(GameTestHelper helper, SATier tier, String kind) {
        var registry = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        ItemStack stack = new ItemStack(item(tier, kind));
        var ench = new net.minecraft.world.item.enchantment.ItemEnchantments.Mutable(net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
        var manager = helper.getLevel().getServer().getRecipeManager();
        for (SATier t : TIERS) {
            // il Pedestal non conserva gli incantesimi della base: da lì si riparte dai soli incantesimi della ricetta
            String tierId = "t" + (t.ordinal() + 6);
            if (manager.byKey(SpectralAscension.id("fusion_shrine/" + tierId + "/" + t.idPrefix() + "_" + kind)).isEmpty()) {
                ench = new net.minecraft.world.item.enchantment.ItemEnchantments.Mutable(net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
            }
            for (var e : cromocon.spectralascension.registry.SAItems.defaultEnchantments(item(t, kind)).entrySet()) {
                var h = registry.getOrThrow(e.getKey());
                ench.set(h, Math.max(ench.getLevel(h), e.getValue()));
            }
            if (t == tier) break;
        }
        stack.set(DataComponents.ENCHANTMENTS, ench.toImmutable());
        return stack;
    }

    /** Usato da altri test: entità viva attorno a un punto. */
    static List<LivingEntity> living(GameTestHelper helper, BlockPos pos, double r) {
        return helper.getLevel().getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(r), Entity::isAlive);
    }
}
