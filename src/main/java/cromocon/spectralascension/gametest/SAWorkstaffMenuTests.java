package cromocon.spectralascension.gametest;

import cromocon.spectralascension.SpectralAscension;
import cromocon.spectralascension.item.SATier;
import cromocon.spectralascension.item.SATools;
import cromocon.spectralascension.item.WorkstaffDrops;
import cromocon.spectralascension.item.WorkstaffOption;
import cromocon.spectralascension.menu.WorkstaffMenu;
import cromocon.spectralascension.registry.SAComponents;
import cromocon.spectralascension.registry.SAItems;
import de.dafuqs.spectrum.api.ink.color.InkColors;
import de.dafuqs.spectrum.components.WorkstaffComponent;
import de.dafuqs.spectrum.inventories.WorkstaffScreenHandler;
import de.dafuqs.spectrum.recipe.GatedSpectrumRecipe;
import de.dafuqs.spectrum.recipe.fusion_shrine.FusionShrineRecipe;
import de.dafuqs.spectrum.registries.SpectrumDataComponentTypes;
import de.dafuqs.spectrum.registries.SpectrumEnchantmentKeys;
import de.dafuqs.spectrum.registries.SpectrumItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static cromocon.spectralascension.gametest.SATestUtil.*;

/**
 * Menu della workstaff T7+ (WorkstaffMenu, WorkstaffOption, componente spectral_ascension:workstaff_drops): apertura
 * (T6 col menu di Spectrum, T7+ col nostro), ogni voce lato server con i controlli di tier, identità e limiti, Foundry
 * contro Silk Touch, Inventory Insertion, aree 7×7 / 9×9 con costo e tetto per tier, drop fusi e messi nell'inventario
 * (anche dai blocchi dell'area), incantesimi dei risultati delle ricette e della Fusion Shrine. La schermata (client)
 * non si prova qui: c'è lo scenario workstaff_menu.
 */
@GameTestHolder(SpectralAscension.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SAWorkstaffMenuTests {
    private static final String EMPTY = "empty";
    /** I muri dei test d'area stanno in alto (y relativa ≥ 150), dove gli altri test non mettono blocchi. */
    private static final int WALL_Y = 150;

    private SAWorkstaffMenuTests() {
    }

    // ---------- attrezzi ----------

    private static int level(ItemStack stack, ResourceKey<Enchantment> key) {
        int[] out = {0};
        stack.getTagEnchantments().entrySet().forEach(e -> {
            if (e.getKey().is(key)) out[0] = e.getIntValue();
        });
        return out[0];
    }

    private static String ench(ItemStack stack) {
        List<String> out = new ArrayList<>();
        stack.getTagEnchantments().entrySet().forEach(e -> out.add(e.getKey().getRegisteredName() + " " + e.getIntValue()));
        return out.toString();
    }

    /** Workstaff come esce dalla ricetta / dal creativo: incantesimi preimpostati e componente di default. */
    private static ItemStack staff(GameTestHelper helper, Item item) {
        return enchanted(helper, item);
    }

    /**
     * Come in gioco: Shift + tasto destro con la workstaff in mano apre il menu, la voce passa da WorkstaffMenu.select
     * (quello che fa il pacchetto WorkstaffOptionPayload), poi il menu si chiude. Ritorna se la voce è stata accettata.
     */
    private static boolean choose(GameTestHelper helper, ServerPlayer player, WorkstaffOption option, List<String> problems) {
        ItemStack held = player.getMainHandItem();
        player.setShiftKeyDown(true);
        held.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        player.setShiftKeyDown(false);
        if (!(player.containerMenu instanceof WorkstaffMenu menu)) {
            problems.add(id(held.getItem()) + ": Shift + tasto destro non apre WorkstaffMenu (" + player.containerMenu.getClass().getSimpleName() + ")");
            player.closeContainer();
            return false;
        }
        boolean ok = menu.select(player, option.ordinal());
        player.closeContainer();
        return ok;
    }

    private static ServerPlayer playerWith(GameTestHelper helper, ItemStack stack) {
        ServerPlayer player = mockPlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return player;
    }

    private static void expect(List<String> problems, boolean ok, String what) {
        if (!ok) problems.add(what);
    }

    // ---------- apertura del menu ----------

    /**
     * Shift + tasto destro: il T6 apre ancora il menu di Spectrum (WorkstaffScreenHandler, menu del T6 invariato), T7,
     * T8 e Alloy il nostro WorkstaffMenu. Il menu resta valido solo con lo STESSO stack in mano: sostituito con una
     * copia, le scelte si scartano.
     */
    @GameTest(template = EMPTY)
    public static void workstaff_menu_open(GameTestHelper helper) {
        List<String> problems = new ArrayList<>(), report = new ArrayList<>();
        ServerPlayer t6 = playerWith(helper, staff(helper, SAItems.TRICHROMATIC_WORKSTAFF.get()));
        t6.setShiftKeyDown(true);
        t6.getMainHandItem().getItem().use(helper.getLevel(), t6, InteractionHand.MAIN_HAND);
        expect(problems, t6.containerMenu instanceof WorkstaffScreenHandler,
                "T6: Shift + tasto destro apre " + t6.containerMenu.getClass().getSimpleName() + " invece del menu di Spectrum");
        expect(problems, !SAItems.TRICHROMATIC_WORKSTAFF.get().hasOwnMenu(), "T6: hasOwnMenu dovrebbe essere false");
        t6.closeContainer();
        report.add("T6 → " + WorkstaffScreenHandler.class.getSimpleName());

        for (Item item : List.of(SAItems.UMBRAL_WORKSTAFF.get(), SAItems.ARGENT_WORKSTAFF.get(), SAItems.PRISMATIC_WORKSTAFF.get())) {
            ItemStack stack = staff(helper, item);
            ServerPlayer player = playerWith(helper, stack);
            player.setShiftKeyDown(true);
            stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            if (!(player.containerMenu instanceof WorkstaffMenu menu)) {
                problems.add(id(item) + ": apre " + player.containerMenu.getClass().getSimpleName() + " invece di WorkstaffMenu");
                continue;
            }
            expect(problems, menu.stillValid(player), id(item) + ": menu non valido con la workstaff in mano");
            expect(problems, menu.hand() == InteractionHand.MAIN_HAND, id(item) + ": mano sbagliata");
            // Stack sostituito da una copia identica: identità persa, la scelta va scartata.
            player.setItemInHand(InteractionHand.MAIN_HAND, stack.copy());
            expect(problems, !menu.stillValid(player), id(item) + ": menu ancora valido con un altro stack in mano");
            expect(problems, !menu.select(player, WorkstaffOption.SELECT_7x7.ordinal()), id(item) + ": scelta accettata con un altro stack in mano");
            expect(problems, !player.getMainHandItem().has(SpectrumDataComponentTypes.AOE) && !stack.has(SpectrumDataComponentTypes.AOE),
                    id(item) + ": l'area è cambiata dopo una scelta rifiutata");
            player.closeContainer();
            // Senza Shift: nessun menu (azione normale di Spectrum).
            player.setShiftKeyDown(false);
            player.getMainHandItem().getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            expect(problems, !(player.containerMenu instanceof WorkstaffMenu), id(item) + ": senza Shift si apre il menu");
            report.add(id(item) + " → WorkstaffMenu");
        }
        finish(helper, "apertura del menu della workstaff", problems, report);
    }

    // ---------- voci del menu lato server ----------

    /**
     * Ogni voce lato server, attraverso il menu: aree (1×1 ... 9×9 dove permesse), tasto destro e proiettili (le voci
     * di Spectrum inoltrate a applyToggle). Rifiuti: 7×7 sul T6, 9×9 su T7 e T8, Foundry sul T6, Inventory Insertion
     * sul T7, indici fuori dai limiti, un oggetto che non è una nostra workstaff. Nei rifiuti lo stack non cambia.
     */
    @GameTest(template = EMPTY)
    public static void workstaff_menu_options(GameTestHelper helper) {
        List<String> problems = new ArrayList<>(), report = new ArrayList<>();
        // T6: il menu è quello di Spectrum, ma il server rifiuta comunque le voci T7+ (client modificato).
        ServerPlayer p6 = playerWith(helper, staff(helper, SAItems.TRICHROMATIC_WORKSTAFF.get()));
        ItemStack t6 = p6.getMainHandItem();
        ItemStack t6Before = t6.copy();
        for (WorkstaffOption o : List.of(WorkstaffOption.SELECT_7x7, WorkstaffOption.SELECT_9x9, WorkstaffOption.FOUNDRY_ON,
                WorkstaffOption.FOUNDRY_OFF, WorkstaffOption.INSERTION_ON, WorkstaffOption.INSERTION_OFF)) {
            expect(problems, !WorkstaffOption.apply(p6, t6, o), "T6: " + o + " accettata");
        }
        expect(problems, ItemStack.isSameItemSameComponents(t6, t6Before), "T6: lo stack è cambiato dopo voci rifiutate");
        expect(problems, WorkstaffOption.apply(p6, t6, WorkstaffOption.SELECT_5x5) && SAItems.TRICHROMATIC_WORKSTAFF.get().getAoERange(t6) == 2,
                "T6: 5×5 non applicata");
        // Non una nostra workstaff.
        ItemStack glass = new ItemStack(SpectrumItems.GLASS_CREST_WORKSTAFF.get());
        expect(problems, !WorkstaffOption.apply(p6, glass, WorkstaffOption.SELECT_3x3), "glass crest di Spectrum: voce accettata");
        report.add("T6: 7×7, 9×9, Foundry e Inventory Insertion rifiutate");

        for (SATier tier : List.of(SATier.T7, SATier.T8, SATier.ALLOY)) {
            Item item = SAFunctionTests.item(tier, "workstaff");
            SATools.Workstaff ws = (SATools.Workstaff) item;
            ServerPlayer player = playerWith(helper, staff(helper, item));
            ItemStack stack = player.getMainHandItem();
            String id = id(item);
            // Indici fuori dai limiti (pacchetto di un client modificato).
            player.setShiftKeyDown(true);
            item.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            player.setShiftKeyDown(false);
            if (player.containerMenu instanceof WorkstaffMenu menu) {
                for (int bad : new int[]{-1, WorkstaffOption.values().length, 9999}) {
                    expect(problems, !menu.select(player, bad), id + ": indice " + bad + " accettato");
                }
            } else {
                problems.add(id + ": menu non aperto");
            }
            player.closeContainer();
            ItemStack before = stack.copy();
            expect(problems, ItemStack.isSameItemSameComponents(stack, before), id + ": stack cambiato dagli indici fuori dai limiti");

            // Aree
            int[][] areas = {{WorkstaffOption.SELECT_3x3.ordinal(), 1}, {WorkstaffOption.SELECT_5x5.ordinal(), 2}, {WorkstaffOption.SELECT_7x7.ordinal(), 3}};
            for (int[] a : areas) {
                WorkstaffOption o = WorkstaffOption.values()[a[0]];
                expect(problems, choose(helper, player, o, problems), id + ": " + o + " rifiutata");
                expect(problems, stack.getOrDefault(SpectrumDataComponentTypes.AOE, 0) == a[1] && ws.getAoERange(stack) == a[1],
                        id + ": dopo " + o + " spectrum:aoe = " + stack.get(SpectrumDataComponentTypes.AOE));
            }
            boolean nineAllowed = tier == SATier.ALLOY;
            boolean nine = choose(helper, player, WorkstaffOption.SELECT_9x9, problems);
            expect(problems, nine == nineAllowed, id + ": 9×9 " + (nine ? "accettata" : "rifiutata"));
            expect(problems, stack.getOrDefault(SpectrumDataComponentTypes.AOE, 0) == (nineAllowed ? 4 : 3), id + ": area dopo 9×9 = " + stack.get(SpectrumDataComponentTypes.AOE));
            expect(problems, choose(helper, player, WorkstaffOption.SELECT_1x1, problems) && !stack.has(SpectrumDataComponentTypes.AOE), id + ": 1×1 non toglie l'area");

            // Inventory Insertion: solo T8+.
            boolean ins = choose(helper, player, WorkstaffOption.INSERTION_OFF, problems);
            expect(problems, ins == (tier != SATier.T7), id + ": Inventory Insertion " + (ins ? "accettata" : "rifiutata"));
            if (ins) choose(helper, player, WorkstaffOption.INSERTION_ON, problems);

            // Voci di Spectrum: tasto destro e proiettili.
            expect(problems, choose(helper, player, WorkstaffOption.DISABLE_RIGHT_CLICK_ACTIONS, problems) && !ws.canTill(stack), id + ": tasto destro non disattivato");
            expect(problems, choose(helper, player, WorkstaffOption.ENABLE_RIGHT_CLICK_ACTIONS, problems) && ws.canTill(stack), id + ": tasto destro non riattivato");
            expect(problems, choose(helper, player, WorkstaffOption.ENABLE_PROJECTILES, problems)
                    && stack.getOrDefault(SpectrumDataComponentTypes.WORKSTAFF, WorkstaffComponent.DEFAULT).canShoot(), id + ": proiettili non attivati");
            expect(problems, choose(helper, player, WorkstaffOption.DISABLE_PROJECTILES, problems)
                    && !stack.getOrDefault(SpectrumDataComponentTypes.WORKSTAFF, WorkstaffComponent.DEFAULT).canShoot(), id + ": proiettili non disattivati");
            report.add(id + ": 3×3/5×5/7×7/1×1 ok, 9×9 " + (nineAllowed ? "ok" : "rifiutata") + ", Insertion " + (tier == SATier.T7 ? "rifiutata" : "ok")
                    + ", tasto destro e proiettili ok, " + ench(stack));
        }
        finish(helper, "voci del menu della workstaff", problems, report);
    }

    /**
     * Foundry contro Silk Touch (T7, poi lo stesso sul T8 e sull'Alloy): Silk Touch toglie Foundry e la ricorda accesa
     * (sospesa); Fortune o Resonance la rimettono. Foundry spenta resta spenta passando da Silk Touch a Fortune.
     * Accendere Foundry con Silk Touch attivo passa prima a Fortune (col livello ricordato). Efficiency non cambia mai.
     */
    @GameTest(template = EMPTY)
    public static void workstaff_foundry_silk(GameTestHelper helper) {
        List<String> problems = new ArrayList<>(), report = new ArrayList<>();
        for (SATier tier : List.of(SATier.T7, SATier.T8, SATier.ALLOY)) {
            Item item = SAFunctionTests.item(tier, "workstaff");
            Map<ResourceKey<Enchantment>, Integer> dflt = SAItems.defaultEnchantments(item);
            int fortune = dflt.get(Enchantments.FORTUNE), efficiency = dflt.get(Enchantments.EFFICIENCY);
            ServerPlayer player = playerWith(helper, staff(helper, item));
            ItemStack s = player.getMainHandItem();
            String id = id(item);
            List<String> steps = new ArrayList<>();
            Runnable check = () -> {
                if (level(s, Enchantments.EFFICIENCY) != efficiency) problems.add(id + ": Efficiency " + level(s, Enchantments.EFFICIENCY) + " dopo " + steps);
                if (level(s, Enchantments.SILK_TOUCH) > 0 && level(s, SpectrumEnchantmentKeys.FOUNDRY) > 0) problems.add(id + ": Silk Touch e Foundry insieme dopo " + steps + ": " + ench(s));
            };
            expect(problems, level(s, SpectrumEnchantmentKeys.FOUNDRY) == 1 && WorkstaffOption.drops(s).equals(WorkstaffDrops.DEFAULT),
                    id + ": non parte con Foundry accesa: " + ench(s) + " " + s.get(SAComponents.WORKSTAFF_DROPS.get()));

            steps.add("Silk Touch");
            choose(helper, player, WorkstaffOption.SELECT_SILK_TOUCH, problems);
            check.run();
            expect(problems, level(s, Enchantments.SILK_TOUCH) == 1 && level(s, SpectrumEnchantmentKeys.FOUNDRY) == 0
                    && WorkstaffOption.drops(s).foundry() && WorkstaffOption.foundryPaused(s), id + ": Silk Touch non sospende Foundry: " + ench(s));

            steps.add("Fortune");
            choose(helper, player, WorkstaffOption.SELECT_FORTUNE, problems);
            check.run();
            expect(problems, level(s, Enchantments.FORTUNE) == fortune && level(s, SpectrumEnchantmentKeys.FOUNDRY) == 1 && level(s, Enchantments.SILK_TOUCH) == 0,
                    id + ": Fortune non rimette Foundry: " + ench(s));

            steps.add("Silk Touch");
            choose(helper, player, WorkstaffOption.SELECT_SILK_TOUCH, problems);
            steps.add("Resonance");
            choose(helper, player, WorkstaffOption.SELECT_RESONANCE, problems);
            check.run();
            expect(problems, level(s, SpectrumEnchantmentKeys.RESONANCE) == 1 && level(s, SpectrumEnchantmentKeys.FOUNDRY) == 1,
                    id + ": Resonance non rimette Foundry: " + ench(s));

            steps.add("Foundry off");
            choose(helper, player, WorkstaffOption.FOUNDRY_OFF, problems);
            check.run();
            expect(problems, level(s, SpectrumEnchantmentKeys.FOUNDRY) == 0 && !WorkstaffOption.drops(s).foundry(), id + ": Foundry non si spegne: " + ench(s));
            steps.add("Silk Touch");
            choose(helper, player, WorkstaffOption.SELECT_SILK_TOUCH, problems);
            steps.add("Fortune");
            choose(helper, player, WorkstaffOption.SELECT_FORTUNE, problems);
            check.run();
            expect(problems, level(s, SpectrumEnchantmentKeys.FOUNDRY) == 0 && level(s, Enchantments.FORTUNE) == fortune,
                    id + ": Foundry spenta è tornata passando da Silk Touch a Fortune: " + ench(s));

            steps.add("Silk Touch");
            choose(helper, player, WorkstaffOption.SELECT_SILK_TOUCH, problems);
            steps.add("Foundry on");
            choose(helper, player, WorkstaffOption.FOUNDRY_ON, problems);
            check.run();
            expect(problems, level(s, Enchantments.SILK_TOUCH) == 0 && level(s, Enchantments.FORTUNE) == fortune
                    && level(s, SpectrumEnchantmentKeys.FOUNDRY) == 1 && WorkstaffOption.drops(s).foundry(),
                    id + ": accendere Foundry con Silk Touch non passa a Fortune: " + ench(s));
            if (tier != SATier.T7) {
                expect(problems, level(s, SpectrumEnchantmentKeys.INVENTORY_INSERTION) == 1, id + ": Inventory Insertion persa: " + ench(s));
            }
            report.add(id + ": " + String.join(" → ", steps) + " = " + ench(s));
        }
        finish(helper, "Foundry e Silk Touch", problems, report);
    }

    /** Inventory Insertion: accesa di default dal T8, si spegne e si riaccende dal menu; sul T7 non c'è e non si accende. */
    @GameTest(template = EMPTY)
    public static void workstaff_insertion_toggle(GameTestHelper helper) {
        List<String> problems = new ArrayList<>(), report = new ArrayList<>();
        for (SATier tier : List.of(SATier.T8, SATier.ALLOY)) {
            Item item = SAFunctionTests.item(tier, "workstaff");
            ServerPlayer player = playerWith(helper, staff(helper, item));
            ItemStack s = player.getMainHandItem();
            expect(problems, level(s, SpectrumEnchantmentKeys.INVENTORY_INSERTION) == 1, id(item) + ": non parte con Inventory Insertion");
            expect(problems, choose(helper, player, WorkstaffOption.INSERTION_OFF, problems)
                    && level(s, SpectrumEnchantmentKeys.INVENTORY_INSERTION) == 0 && !WorkstaffOption.drops(s).insertion(), id(item) + ": non si spegne: " + ench(s));
            expect(problems, choose(helper, player, WorkstaffOption.INSERTION_ON, problems)
                    && level(s, SpectrumEnchantmentKeys.INVENTORY_INSERTION) == 1 && WorkstaffOption.drops(s).insertion(), id(item) + ": non si riaccende: " + ench(s));
            expect(problems, level(s, SpectrumEnchantmentKeys.FOUNDRY) == 1, id(item) + ": Foundry toccata: " + ench(s));
            report.add(id(item) + ": spenta e riaccesa");
        }
        ServerPlayer p7 = playerWith(helper, staff(helper, SAItems.UMBRAL_WORKSTAFF.get()));
        expect(problems, !choose(helper, p7, WorkstaffOption.INSERTION_ON, problems), "T7: Inventory Insertion accettata");
        expect(problems, level(p7.getMainHandItem(), SpectrumEnchantmentKeys.INVENTORY_INSERTION) == 0, "T7: ha Inventory Insertion");
        report.add("umbral_workstaff: Inventory Insertion rifiutata");
        finish(helper, "Inventory Insertion", problems, report);
    }

    // ---------- area ----------

    private record Wall(BlockPos center, List<BlockPos> blocks, long left, long paid, ServerPlayer player) {
    }

    /**
     * Muro (2r+1) × (2r+1) davanti al giocatore finto in sopravvivenza, che lo guarda al centro e rompe il blocco
     * centrale come in gioco (gameMode.destroyBlock → BreakEvent → afterBreakingBlock). Con {@code withInk} ha l'Ink
     * sbloccato e una fiaschetta di White Ink. Ritorna i blocchi rimasti e l'Ink pagato.
     */
    private static Wall mineWall(GameTestHelper helper, int row, ItemStack staff, int wallRange, BlockState block, boolean withInk) {
        var level = helper.getLevel();
        BlockPos feet = helper.absolutePos(new BlockPos(0, WALL_Y + 12 * row, 0));
        BlockPos center = feet.offset(0, 1, 2);
        // Come AoEHelper sulle pareti: da 1 sotto il blocco colpito a 2r-1 sopra; con r = 0 solo il blocco colpito.
        List<BlockPos> wall = new ArrayList<>();
        if (wallRange == 0) wall.add(center);
        for (int dx = -wallRange; dx <= wallRange && wallRange > 0; dx++)
            for (int dy = 0; dy <= 2 * wallRange; dy++) wall.add(new BlockPos(center.getX() + dx, feet.getY() + dy, center.getZ()));
        for (BlockPos pos : wall) level.setBlockAndUpdate(pos, block);
        ServerPlayer player = mockPlayer(helper);
        aim(player, feet, 0.0F, 0.0F);
        ItemStack flask = SpectrumItems.INK_FLASK.get().getDefaultInstance();
        if (withInk) {
            unlockInk(helper, player);
            var storage = SpectrumItems.INK_FLASK.get().getEnergyStorage(flask);
            storage.addEnergy(InkColors.WHITE, 1000);
            SpectrumItems.INK_FLASK.get().setEnergyStorage(flask, storage);
            player.getInventory().setItem(9, flask);
        }
        long before = SpectrumItems.INK_FLASK.get().getEnergyStorage(flask).getEnergy(InkColors.WHITE);
        player.setItemInHand(InteractionHand.MAIN_HAND, staff);
        player.gameMode.destroyBlock(center);
        long left = wall.stream().filter(pos -> !level.getBlockState(pos).isAir()).count();
        long paid = before - SpectrumItems.INK_FLASK.get().getEnergyStorage(flask).getEnergy(InkColors.WHITE);
        return new Wall(center, wall, left, paid, player);
    }

    private static void clear(GameTestHelper helper, Wall wall) {
        for (BlockPos pos : wall.blocks()) helper.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(wall.center()).inflate(12)).forEach(Entity::discard);
    }

    private static ItemStack withArea(GameTestHelper helper, Item item, int range) {
        ItemStack s = staff(helper, item);
        s.set(SpectrumDataComponentTypes.AOE, range);
        return s;
    }

    /** Il tooltip contiene la chiave di traduzione con questo primo argomento (es. aoe_cost con 9). */
    private static boolean tooltipHas(List<Component> lines, String key, Object firstArg) {
        for (Component line : lines) {
            if (line.getContents() instanceof TranslatableContents t && t.getKey().equals(key)
                    && (firstArg == null || (t.getArgs().length > 0 && String.valueOf(t.getArgs()[0]).equals(String.valueOf(firstArg))))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Aree grandi in sopravvivenza: 7×7 sul T7 e sul T8 = 49 blocchi e 9 White Ink per blocco rotto a mano (come il
     * 5×5), 9×9 sull'Alloy = 81 blocchi senza Ink né sblocco. Tetto per tier: un T7 con spectrum:aoe = 4 scava solo il
     * 7×7, un T6 con spectrum:aoe = 3 solo il 5×5 (e paga 9). Tooltip: area 7×7 e costo 9, area 9×9 gratis.
     */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void workstaff_aoe_large(GameTestHelper helper) {
        List<String> problems = new ArrayList<>(), report = new ArrayList<>();
        BlockState stone = Blocks.STONE.defaultBlockState();
        var context = Item.TooltipContext.of(helper.getLevel());
        int row = 0;
        expect(problems, SATools.Workstaff.aoeInkCost(1) == 3 && SATools.Workstaff.aoeInkCost(2) == 9 && SATools.Workstaff.aoeInkCost(3) == 9,
                "costi dell'area: 3x3 " + SATools.Workstaff.aoeInkCost(1) + ", 5x5 " + SATools.Workstaff.aoeInkCost(2) + ", 7x7 " + SATools.Workstaff.aoeInkCost(3));
        for (Item item : List.of(SAItems.UMBRAL_WORKSTAFF.get(), SAItems.ARGENT_WORKSTAFF.get())) {
            ItemStack s = withArea(helper, item, 3);
            List<Component> tip = s.getTooltipLines(context, null, TooltipFlag.NORMAL);
            expect(problems, tooltipHas(tip, "item.spectrum.workstaff.tooltip.mining_range", 7), id(item) + ": il tooltip non dice 7x7");
            expect(problems, tooltipHas(tip, "item.spectral_ascension.workstaff.aoe_cost", 9L), id(item) + ": il tooltip non dice 9 White Ink");
            // senza Ink: solo il blocco colpito
            Wall none = mineWall(helper, row++, s.copy(), 3, stone, false);
            expect(problems, none.left() == 48, id(item) + " 7x7 senza Ink: rimasti " + none.left() + " invece di 48");
            clear(helper, none);
            Wall w = mineWall(helper, row++, s, 3, stone, true);
            expect(problems, w.left() == 0, id(item) + " 7x7: rimasti " + w.left() + "/49");
            expect(problems, w.paid() == 9, id(item) + " 7x7: Ink pagato " + w.paid() + " invece di 9");
            report.add(id(item) + " 7x7: " + (49 - w.left()) + " blocchi, Ink " + w.paid());
            clear(helper, w);
        }
        // Alloy 9×9 gratis
        ItemStack p = withArea(helper, SAItems.PRISMATIC_WORKSTAFF.get(), 4);
        List<Component> tip = p.getTooltipLines(context, null, TooltipFlag.NORMAL);
        expect(problems, tooltipHas(tip, "item.spectrum.workstaff.tooltip.mining_range", 9), "prismatic_workstaff: il tooltip non dice 9x9");
        expect(problems, tooltipHas(tip, "item.spectral_ascension.workstaff.aoe_free", null), "prismatic_workstaff: il tooltip non dice gratis");
        Wall w9 = mineWall(helper, row++, p, 4, stone, false);
        expect(problems, w9.left() == 0 && w9.paid() == 0, "prismatic_workstaff 9x9: rimasti " + w9.left() + "/81, Ink " + w9.paid());
        report.add("prismatic_workstaff 9x9: " + (81 - w9.left()) + " blocchi senza Ink");
        clear(helper, w9);

        // Tetto: T7 con aoe 4 → 7×7; T8 idem; T6 con aoe 3 → 5×5.
        for (Item item : List.of(SAItems.UMBRAL_WORKSTAFF.get(), SAItems.ARGENT_WORKSTAFF.get())) {
            ItemStack s = withArea(helper, item, 4);
            expect(problems, ((SATools.Workstaff) item).getAoERange(s) == 3, id(item) + ": getAoERange con aoe 4 = " + ((SATools.Workstaff) item).getAoERange(s));
            Wall w = mineWall(helper, row++, s, 4, stone, true);
            expect(problems, w.left() == 81 - 49 && w.paid() == 9, id(item) + " con aoe 4: rimasti " + w.left() + "/81 (attesi 32), Ink " + w.paid());
            report.add(id(item) + " con spectrum:aoe=4: scava " + (81 - w.left()) + " (tetto 7x7)");
            clear(helper, w);
        }
        ItemStack t6 = withArea(helper, SAItems.TRICHROMATIC_WORKSTAFF.get(), 3);
        Wall w6 = mineWall(helper, row++, t6, 3, stone, true);
        expect(problems, w6.left() == 49 - 25 && w6.paid() == 9, "trichromatic_workstaff con aoe 3: rimasti " + w6.left() + "/49 (attesi 24), Ink " + w6.paid());
        report.add("trichromatic_workstaff con spectrum:aoe=3: scava " + (49 - w6.left()) + " (tetto 5x5)");
        clear(helper, w6);
        ItemStack p5 = withArea(helper, SAItems.PRISMATIC_WORKSTAFF.get(), 6);
        expect(problems, SAItems.PRISMATIC_WORKSTAFF.get().getAoERange(p5) == 4, "Alloy con aoe 6: tetto non a 4");
        finish(helper, "aree 7x7 e 9x9", problems, report);
    }

    // ---------- drop: Foundry e Inventory Insertion ----------

    private static int ground(GameTestHelper helper, Wall wall, Item item) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(wall.center()).inflate(12)).stream()
                .filter(e -> e.getItem().is(item)).mapToInt(e -> e.getItem().getCount()).sum();
    }

    /**
     * Foundry fonde davvero i drop: minerale di ferro rotto con la workstaff T7 → lingotti di ferro a terra, niente ferro
     * grezzo; con Foundry spenta torna il ferro grezzo. Inventory Insertion (T8) mette i lingotti nell'inventario e
     * niente a terra. Con l'area (Alloy, 3×3 di minerale di ferro, gratis) valgono entrambe anche per i blocchi rotti
     * dall'area: 9 blocchi, tutti fusi e tutti nell'inventario.
     */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void workstaff_foundry_insertion_drops(GameTestHelper helper) {
        List<String> problems = new ArrayList<>(), report = new ArrayList<>();
        BlockState ore = Blocks.IRON_ORE.defaultBlockState();
        int row = 10;
        // T7: Foundry, drop a terra
        Wall t7 = mineWall(helper, row++, staff(helper, SAItems.UMBRAL_WORKSTAFF.get()), 0, ore, false);
        int ingots = ground(helper, t7, Items.IRON_INGOT), raw = ground(helper, t7, Items.RAW_IRON);
        expect(problems, t7.left() == 0 && ingots > 0 && raw == 0, "T7 Foundry: rimasti " + t7.left() + ", lingotti a terra " + ingots + ", ferro grezzo " + raw);
        report.add("umbral_workstaff: minerale di ferro → " + ingots + " lingotti a terra, " + raw + " grezzo");
        clear(helper, t7);
        // T7 con Foundry spenta: ferro grezzo
        ItemStack off = staff(helper, SAItems.UMBRAL_WORKSTAFF.get());
        ServerPlayer p = playerWith(helper, off);
        WorkstaffOption.apply(p, off, WorkstaffOption.FOUNDRY_OFF);
        Wall t7off = mineWall(helper, row++, off, 0, ore, false);
        ingots = ground(helper, t7off, Items.IRON_INGOT);
        raw = ground(helper, t7off, Items.RAW_IRON);
        expect(problems, ingots == 0 && raw > 0, "T7 Foundry spenta: lingotti " + ingots + ", ferro grezzo " + raw);
        report.add("umbral_workstaff Foundry spenta: " + raw + " ferro grezzo");
        clear(helper, t7off);
        // T8: Foundry + Inventory Insertion, niente a terra
        Wall t8 = mineWall(helper, row++, staff(helper, SAItems.ARGENT_WORKSTAFF.get()), 0, ore, false);
        int inv = t8.player().getInventory().countItem(Items.IRON_INGOT);
        int onGround = ground(helper, t8, Items.IRON_INGOT) + ground(helper, t8, Items.RAW_IRON);
        expect(problems, inv > 0 && onGround == 0 && t8.player().getInventory().countItem(Items.RAW_IRON) == 0,
                "T8: lingotti nell'inventario " + inv + ", a terra " + onGround);
        report.add("argent_workstaff: " + inv + " lingotti nell'inventario, " + onGround + " a terra");
        clear(helper, t8);
        // Alloy, area 3×3 di minerale: tutti i blocchi dell'area fusi e nell'inventario
        Wall t9 = mineWall(helper, row++, withArea(helper, SAItems.PRISMATIC_WORKSTAFF.get(), 1), 1, ore, false);
        inv = t9.player().getInventory().countItem(Items.IRON_INGOT);
        onGround = ground(helper, t9, Items.IRON_INGOT) + ground(helper, t9, Items.RAW_IRON);
        expect(problems, t9.left() == 0, "Alloy 3x3 di minerale: rimasti " + t9.left() + "/9");
        expect(problems, inv >= 9 && onGround == 0 && t9.player().getInventory().countItem(Items.RAW_IRON) == 0,
                "Alloy 3x3: lingotti nell'inventario " + inv + " (almeno 9), a terra " + onGround);
        report.add("prismatic_workstaff 3x3 di minerale di ferro: " + inv + " lingotti nell'inventario, " + onGround + " a terra");
        clear(helper, t9);
        finish(helper, "drop di Foundry e Inventory Insertion", problems, report);
    }

    // ---------- ricette e Fusion Shrine ----------

    private static ItemStack fusionResult(ItemStack recipeOutput, ItemStack input) throws ReflectiveOperationException {
        Method copy = GatedSpectrumRecipe.class.getDeclaredMethod("copyComponents", ItemStack.class, ItemStack.class);
        copy.setAccessible(true);
        return (ItemStack) copy.invoke(null, recipeOutput.copy(), input);
    }

    private static ItemStack recipeOutput(GameTestHelper helper, String path) {
        var access = helper.getLevel().registryAccess();
        var recipe = (FusionShrineRecipe) helper.getLevel().getServer().getRecipeManager()
                .byKey(SpectralAscension.id(path)).orElseThrow().value();
        return recipe.getResultItem(access);
    }

    /**
     * Incantesimi dei risultati: le ricette T7/T8/Alloy della workstaff danno Foundry (e dal T8 Inventory Insertion)
     * oltre agli altri, come SAItems; il componente parte acceso; le workstaff accettano i due incantesimi (tag
     * enchantable di Spectrum). Fusion Shrine: un T6 in Silk Touch o Resonance conserva la modalità sul T7; un T7
     * con Foundry spenta dà un T8 senza Foundry; un T8 con Insertion spenta dà un Alloy senza Insertion. Uno stack con
     * Silk Touch e Foundry viene riallineato (Foundry sospesa).
     */
    @GameTest(template = EMPTY)
    public static void workstaff_recipe_drops(GameTestHelper helper) throws ReflectiveOperationException {
        List<String> problems = new ArrayList<>(), report = new ArrayList<>();
        var registry = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Map<String, Item> recipes = Map.of("fusion_shrine/t7/umbral_workstaff", SAItems.UMBRAL_WORKSTAFF.get(),
                "fusion_shrine/t8/argent_workstaff", SAItems.ARGENT_WORKSTAFF.get(),
                "fusion_shrine/t9/prismatic_workstaff", SAItems.PRISMATIC_WORKSTAFF.get());
        for (var e : recipes.entrySet()) {
            ItemStack out = recipeOutput(helper, e.getKey());
            boolean t8plus = !e.getValue().equals(SAItems.UMBRAL_WORKSTAFF.get());
            expect(problems, level(out, SpectrumEnchantmentKeys.FOUNDRY) == 1, e.getKey() + ": il risultato non ha Foundry: " + ench(out));
            expect(problems, level(out, SpectrumEnchantmentKeys.INVENTORY_INSERTION) == (t8plus ? 1 : 0), e.getKey() + ": Inventory Insertion " + ench(out));
            Map<ResourceKey<Enchantment>, Integer> dflt = SAItems.defaultEnchantments(e.getValue());
            expect(problems, dflt.getOrDefault(SpectrumEnchantmentKeys.FOUNDRY, 0) == 1
                    && dflt.getOrDefault(SpectrumEnchantmentKeys.INVENTORY_INSERTION, 0) == (t8plus ? 1 : 0), id(e.getValue()) + ": default di SAItems " + dflt);
            expect(problems, out.getTagEnchantments().size() == dflt.size(), e.getKey() + ": il risultato ha " + ench(out) + ", SAItems " + dflt);
            expect(problems, WorkstaffDrops.DEFAULT.equals(out.get(SAComponents.WORKSTAFF_DROPS.get())), e.getKey() + ": componente " + out.get(SAComponents.WORKSTAFF_DROPS.get()));
            expect(problems, out.supportsEnchantment(registry.getOrThrow(SpectrumEnchantmentKeys.FOUNDRY))
                    && out.supportsEnchantment(registry.getOrThrow(SpectrumEnchantmentKeys.INVENTORY_INSERTION)), id(e.getValue()) + ": non accetta Foundry o Inventory Insertion");
            report.add(e.getKey() + " → " + ench(out));
        }
        ItemStack umbralRecipe = recipeOutput(helper, "fusion_shrine/t7/umbral_workstaff");
        for (var mode : Map.of(WorkstaffOption.SELECT_SILK_TOUCH, Enchantments.SILK_TOUCH,
                WorkstaffOption.SELECT_RESONANCE, SpectrumEnchantmentKeys.RESONANCE).entrySet()) {
            var player = mockPlayer(helper);
            ItemStack t6 = staff(helper, SAItems.TRICHROMATIC_WORKSTAFF.get());
            WorkstaffOption.apply(player, t6, mode.getKey());
            expect(problems, level(t6, mode.getValue()) == 1, "T6: modalità non selezionata " + mode.getKey());
            ItemStack t7 = fusionResult(umbralRecipe, t6);
            expect(problems, t7.is(SAItems.UMBRAL_WORKSTAFF.get()) && level(t7, mode.getValue()) == 1
                    && level(t7, Enchantments.FORTUNE) == 0, "T6 → T7 perde " + mode.getKey() + ": " + ench(t7));
            boolean silkTouch = mode.getValue().equals(Enchantments.SILK_TOUCH);
            expect(problems, level(t7, SpectrumEnchantmentKeys.FOUNDRY) == (silkTouch ? 0 : 1)
                    && WorkstaffOption.foundryPaused(t7) == silkTouch,
                    "T6 → T7 ha uno stato Foundry errato con " + mode.getKey() + ": " + ench(t7));
            ItemStack customRecipe = umbralRecipe.copy();
            EnchantmentHelper.updateEnchantments(customRecipe, enchantments -> {
                enchantments.removeIf(holder -> holder.is(Enchantments.FORTUNE));
                enchantments.set(registry.getOrThrow(mode.getValue()), 2);
            });
            ItemStack customResult = fusionResult(customRecipe, t6);
            expect(problems, level(customResult, mode.getValue()) == 2,
                    "T6 → T7 ha abbassato il livello della modalità nella ricetta: " + ench(customResult));

            var otherMode = silkTouch ? SpectrumEnchantmentKeys.RESONANCE : Enchantments.SILK_TOUCH;
            EnchantmentHelper.updateEnchantments(customRecipe, enchantments -> {
                enchantments.removeIf(holder -> holder.is(mode.getValue()));
                enchantments.set(registry.getOrThrow(otherMode), 1);
            });
            ItemStack conflictResult = fusionResult(customRecipe, t6);
            expect(problems, level(conflictResult, otherMode) == 1 && level(conflictResult, mode.getValue()) == 0,
                    "T6 → T7 ha mescolato modalità incompatibili: " + ench(conflictResult));

            t6.enchant(registry.getOrThrow(otherMode), 1);
            ItemStack ambiguousResult = fusionResult(umbralRecipe, t6);
            expect(problems, level(ambiguousResult, Enchantments.FORTUNE) == level(umbralRecipe, Enchantments.FORTUNE),
                    "T6 → T7 ha selezionato una delle due modalità arbitrariamente: " + ench(ambiguousResult));

            WorkstaffOption.apply(player, t7, WorkstaffOption.SELECT_FORTUNE);
            expect(problems, level(t7, Enchantments.FORTUNE) == level(umbralRecipe, Enchantments.FORTUNE)
                    && level(t7, SpectrumEnchantmentKeys.FOUNDRY) == 1,
                    "T6 → T7 non ripristina Fortune del nuovo tier e Foundry: " + ench(t7));
        }
        // T7 con Foundry spenta → T8 senza Foundry
        ItemStack t7off = staff(helper, SAItems.UMBRAL_WORKSTAFF.get());
        WorkstaffOption.apply(mockPlayer(helper), t7off, WorkstaffOption.FOUNDRY_OFF);
        ItemStack t8 = fusionResult(recipeOutput(helper, "fusion_shrine/t8/argent_workstaff"), t7off);
        expect(problems, level(t8, SpectrumEnchantmentKeys.FOUNDRY) == 0 && !WorkstaffOption.drops(t8).foundry()
                && level(t8, SpectrumEnchantmentKeys.INVENTORY_INSERTION) == 1, "T7 con Foundry spenta → T8: " + ench(t8) + " " + t8.get(SAComponents.WORKSTAFF_DROPS.get()));
        report.add("T7 con Foundry spenta → T8 " + ench(t8));
        // T8 con Insertion spenta → Alloy senza Insertion
        ItemStack t8off = staff(helper, SAItems.ARGENT_WORKSTAFF.get());
        WorkstaffOption.apply(mockPlayer(helper), t8off, WorkstaffOption.INSERTION_OFF);
        ItemStack t9 = fusionResult(recipeOutput(helper, "fusion_shrine/t9/prismatic_workstaff"), t8off);
        expect(problems, level(t9, SpectrumEnchantmentKeys.INVENTORY_INSERTION) == 0 && level(t9, SpectrumEnchantmentKeys.FOUNDRY) == 1,
                "T8 con Insertion spenta → Alloy: " + ench(t9));
        report.add("T8 con Insertion spenta → Alloy " + ench(t9));
        // Stack con Silk Touch e Foundry (es. dall'advancement dei conflitti): riallineato, Foundry sospesa.
        ItemStack both = staff(helper, SAItems.UMBRAL_WORKSTAFF.get());
        EnchantmentHelper.updateEnchantments(both, m -> m.set(registry.getOrThrow(Enchantments.SILK_TOUCH), 1));
        WorkstaffOption.syncDropEnchantments(helper.getLevel().registryAccess(), both);
        expect(problems, level(both, SpectrumEnchantmentKeys.FOUNDRY) == 0 && WorkstaffOption.foundryPaused(both), "Silk Touch + Foundry non riallineato: " + ench(both));
        // Il componente sopravvive al salvataggio.
        var ops = helper.getLevel().registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
        ItemStack reloaded = ItemStack.CODEC.parse(ops, ItemStack.CODEC.encodeStart(ops, t8).getOrThrow()).getOrThrow();
        expect(problems, Arrays.asList(WorkstaffOption.drops(reloaded).foundry()).equals(List.of(false)), "componente perso al salvataggio");
        finish(helper, "ricette e Fusion Shrine della workstaff", problems, report);
    }
}
