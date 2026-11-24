package cromocon.spectralascension.gametest;

import cromocon.spectralascension.SpectralAscension;
import cromocon.spectralascension.item.SATier;
import cromocon.spectralascension.registry.SAItems;
import de.dafuqs.spectrum.api.item.Preenchanted;
import de.dafuqs.spectrum.blocks.fusion_shrine.FusionShrineBlockEntity;
import de.dafuqs.spectrum.recipe.fusion_shrine.FusionShrineRecipe;
import de.dafuqs.spectrum.registries.SpectrumBlocks;
import de.dafuqs.spectrum.registries.SpectrumItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cromocon.spectralascension.gametest.SATestUtil.*;

/**
 * Craft veri per tutte le 60 ricette, seguendo la catena T6 → T7 → T8 → Alloy come un giocatore:
 * <ul>
 * <li>Fusion Shrine (tutti i pezzi di T6, T7, T8 e Alloy dal 2026-11-19): shrine piazzata, liquido del tier a
 * pieno, ingredienti negli slot, {@code matches} e poi {@code craft}. Il risultato esce a terra; quello del tier prima
 * fa da base al tier dopo, quindi si controlla che gli incantesimi passino (copy_components: vince il più alto).</li>
 * </ul>
 * La base di partenza di ogni catena è l'oggetto di Spectrum con un incantesimo in più (Looting/Lure/... dove
 * accettato) per vedere se sopravvive.
 */
@GameTestHolder(SpectralAscension.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SACraftTests {
    private static final String EMPTY = "empty";
    private static final List<SATier> TIERS = List.of(SATier.T6, SATier.T7, SATier.T8, SATier.ALLOY);
    private static final List<String> KINDS = List.of("workstaff", "ultra_greatsword", "crossbow", "bident", "axe", "shovel", "hoe",
            "sword", "bow", "fishing_rod", "shears", "helmet", "chestplate", "leggings", "boots");

    private SACraftTests() {
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void craft_chain_all(GameTestHelper helper) {
        List<String> problems = new ArrayList<>(), report = new ArrayList<>();
        var level = helper.getLevel();
        var access = level.registryAccess();
        var manager = level.getServer().getRecipeManager();
        int row = 0;
        for (String kind : KINDS) {
            ItemStack previous = ItemStack.EMPTY;
            List<String> line = new ArrayList<>();
            for (SATier tier : TIERS) {
                Item item = SAFunctionTests.item(tier, kind);
                String id = id(item);
                String tierId = "t" + (tier.ordinal() + 6);
                var fusion = manager.byKey(SpectralAscension.id("fusion_shrine/" + tierId + "/" + id));
                BlockPos base = SAFunctionTests.slot(helper, row++ % 12);
                ItemStack result;
                if (fusion.isPresent()) {
                    result = fusionCraft(helper, (FusionShrineRecipe) fusion.get().value(), previous, base, problems, id);
                } else {
                    problems.add(id + ": nessuna ricetta");
                    break;
                }
                if (result.isEmpty()) break;
                if (!result.is(item)) problems.add(id + ": il craft dà " + result);
                if (!result.has(DataComponents.UNBREAKABLE)) problems.add(id + ": dopo il craft non è indistruttibile");
                // incantesimi del risultato: almeno quelli della ricetta
                var want = SAItems.defaultEnchantments(item);
                var reg = access.lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
                for (var e : want.entrySet()) {
                    if (result.getEnchantments().getLevel(reg.getOrThrow(e.getKey())) < e.getValue())
                        problems.add(id + ": dopo il craft " + e.getKey().location() + " " + result.getEnchantments().getLevel(reg.getOrThrow(e.getKey())) + " invece di " + e.getValue());
                }
                // con la Fusion Shrine gli incantesimi della base restano (vince il livello più alto, senza somma)
                if (fusion.isPresent() && !previous.isEmpty() && !(item instanceof cromocon.spectralascension.item.SABident)) {
                    for (var e : previous.getEnchantments().entrySet()) {
                        int lvl = result.getEnchantments().getLevel(e.getKey());
                        int expected = Math.max(e.getIntValue(), want.entrySet().stream().filter(w -> e.getKey().is(w.getKey())).mapToInt(Map.Entry::getValue).findFirst().orElse(0));
                        if (result.supportsEnchantment(e.getKey()) && lvl != expected)
                            problems.add(id + ": " + e.getKey().getRegisteredName() + " " + e.getIntValue() + " della base → " + lvl + " invece di " + expected);
                    }
                }
                line.add(id + " (Fusion)" + " " + enchList(result));
                previous = result;
            }
            report.add(String.join(" → ", line));
        }
        finish(helper, "catena di craft", problems, report);
    }

    /** Spec T7 (decisione dell'utente 2026-11-19): vibranium, polvere di onice, tick, xp, incantesimi del risultato. */
    private record T7Spec(int vibranium, int onyx, int time, float xp, Map<String, Integer> enchantments) {
    }

    private static final Map<String, T7Spec> T7_SPEC = Map.ofEntries(
            Map.entry("workstaff", new T7Spec(8, 64, 2000, 48f, Map.of("fortune", 7, "efficiency", 7, "foundry", 1))),
            Map.entry("axe", new T7Spec(8, 64, 1600, 24f, Map.of("efficiency", 8))),
            Map.entry("shovel", new T7Spec(4, 32, 1600, 24f, Map.of("efficiency", 8))),
            Map.entry("hoe", new T7Spec(4, 32, 1600, 24f, Map.of("fortune", 7))),
            Map.entry("sword", new T7Spec(6, 48, 1600, 24f, Map.of("sharpness", 8))),
            Map.entry("bow", new T7Spec(6, 48, 1600, 24f, Map.of("power", 8))),
            Map.entry("fishing_rod", new T7Spec(4, 32, 1600, 24f, Map.of("luck_of_the_sea", 7, "lure", 4))),
            Map.entry("shears", new T7Spec(2, 16, 1600, 24f, Map.of("efficiency", 8))),
            Map.entry("ultra_greatsword", new T7Spec(8, 64, 2000, 48f, Map.of("sweeping_edge", 7))),
            Map.entry("crossbow", new T7Spec(6, 48, 2000, 48f, Map.of("piercing", 6, "quick_charge", 4))),
            Map.entry("bident", new T7Spec(8, 64, 2000, 48f, Map.of("impaling", 7))),
            Map.entry("helmet", new T7Spec(5, 32, 1600, 24f, Map.of("projectile_protection", 7))),
            Map.entry("chestplate", new T7Spec(8, 64, 1600, 24f, Map.of("protection", 7))),
            Map.entry("leggings", new T7Spec(7, 64, 1600, 24f, Map.of("blast_protection", 7))),
            Map.entry("boots", new T7Spec(4, 32, 1600, 24f, Map.of("fire_protection", 7))));

    /** Spec T8 (decisione dell'utente 2026-11-19): unobtainium, polvere di moonstone, tick, xp, incantesimi del risultato. */
    private record T8Spec(int unobtainium, int moonstone, int time, float xp, Map<String, Integer> enchantments) {
    }

    private static final Map<String, T8Spec> T8_SPEC = Map.ofEntries(
            Map.entry("workstaff", new T8Spec(8, 64, 2400, 64f, Map.of("fortune", 8, "efficiency", 8, "foundry", 1, "inventory_insertion", 1))),
            Map.entry("axe", new T8Spec(8, 64, 2000, 48f, Map.of("efficiency", 9))),
            Map.entry("shovel", new T8Spec(4, 32, 2000, 48f, Map.of("efficiency", 9))),
            Map.entry("hoe", new T8Spec(4, 32, 2000, 48f, Map.of("fortune", 8))),
            Map.entry("sword", new T8Spec(6, 48, 2000, 48f, Map.of("sharpness", 9))),
            Map.entry("bow", new T8Spec(6, 48, 2000, 48f, Map.of("power", 9))),
            Map.entry("fishing_rod", new T8Spec(4, 32, 2000, 48f, Map.of("luck_of_the_sea", 8, "lure", 5))),
            Map.entry("shears", new T8Spec(2, 16, 2000, 48f, Map.of("efficiency", 9))),
            Map.entry("ultra_greatsword", new T8Spec(8, 64, 2400, 64f, Map.of("sweeping_edge", 8))),
            Map.entry("crossbow", new T8Spec(6, 48, 2400, 64f, Map.of("piercing", 7, "quick_charge", 4, "sniping", 1))),
            Map.entry("bident", new T8Spec(8, 64, 2400, 64f, Map.of("impaling", 8))),
            Map.entry("helmet", new T8Spec(5, 32, 2000, 48f, Map.of("projectile_protection", 8))),
            Map.entry("chestplate", new T8Spec(8, 64, 2000, 48f, Map.of("protection", 8))),
            Map.entry("leggings", new T8Spec(7, 64, 2000, 48f, Map.of("blast_protection", 8))),
            Map.entry("boots", new T8Spec(4, 32, 2000, 48f, Map.of("fire_protection", 8))));

    /**
     * Spec T9 (decisione dell'utente 2026-11-19): Colorful Shooting Star, Stardust, tick, xp. Gli incantesimi sono quelli
     * congelati del T9 (T9_ENCHANTMENTS in gen_data.py = SAItems.PRISMATIC_*).
     */
    private record T9Spec(int stars, int stardust, int time, float xp, Map<String, Integer> enchantments) {
    }

    private static final Map<String, T9Spec> T9_SPEC = Map.ofEntries(
            Map.entry("workstaff", new T9Spec(2, 32, 2400, 64f, Map.of("fortune", 11, "efficiency", 12, "foundry", 1, "inventory_insertion", 1))),
            Map.entry("ultra_greatsword", new T9Spec(2, 32, 2400, 64f, Map.of("sweeping_edge", 11))),
            Map.entry("bident", new T9Spec(2, 32, 2400, 64f, Map.of("impaling", 10))),
            Map.entry("crossbow", new T9Spec(1, 24, 2400, 64f, Map.of("piercing", 11, "quick_charge", 4, "sniping", 2))),
            Map.entry("axe", new T9Spec(1, 32, 2000, 48f, Map.of("efficiency", 12))),
            Map.entry("shovel", new T9Spec(1, 16, 2000, 48f, Map.of("efficiency", 12))),
            Map.entry("hoe", new T9Spec(1, 16, 2000, 48f, Map.of("fortune", 11))),
            Map.entry("sword", new T9Spec(1, 24, 2000, 48f, Map.of("sharpness", 12))),
            Map.entry("bow", new T9Spec(1, 24, 2000, 48f, Map.of("power", 12))),
            Map.entry("fishing_rod", new T9Spec(1, 16, 2000, 48f, Map.of("luck_of_the_sea", 11, "lure", 5))),
            Map.entry("shears", new T9Spec(1, 8, 2000, 48f, Map.of("efficiency", 12))),
            Map.entry("helmet", new T9Spec(1, 20, 2000, 48f, Map.of("projectile_protection", 14))),
            Map.entry("chestplate", new T9Spec(2, 32, 2000, 48f, Map.of("protection", 11))),
            Map.entry("leggings", new T9Spec(2, 28, 2000, 48f, Map.of("blast_protection", 12))),
            Map.entry("boots", new T9Spec(1, 16, 2000, 48f, Map.of("fire_protection", 12))));

    private static final Map<String, Map<String, Integer>> T6_FIXED = Map.of(
            "workstaff", Map.of("fortune", 6, "efficiency", 5), "hoe", Map.of("fortune", 6),
            "crossbow", Map.of("quick_charge", 3, "piercing", 5), "bident", Map.of("impaling", 6));

    private static Map<String, Integer> enchMap(ItemStack stack) {
        Map<String, Integer> out = new HashMap<>();
        stack.getEnchantments().entrySet().forEach(e -> out.put(e.getKey().unwrapKey().orElseThrow().location().getPath(), e.getIntValue()));
        return out;
    }

    /** Le 15 ricette T7 sono tutte Fusion Shrine (Dragonrot, pezzo T6 + vibranium + onice), niente Pedestal; T6 corretti. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void t7_fusion_recipes(GameTestHelper helper) {
        List<String> problems = new ArrayList<>(), report = new ArrayList<>();
        var level = helper.getLevel();
        var access = level.registryAccess();
        var manager = level.getServer().getRecipeManager();
        for (String kind : KINDS) {
            Item t7 = SAFunctionTests.item(SATier.T7, kind), t6 = SAFunctionTests.item(SATier.T6, kind);
            String id = id(t7);
            if (manager.byKey(SpectralAscension.id("pedestal/tier4/t7/" + id)).isPresent()) problems.add(id + ": esiste ancora la ricetta Pedestal");
            var holder = manager.byKey(SpectralAscension.id("fusion_shrine/t7/" + id));
            if (holder.isEmpty() || !(holder.get().value() instanceof FusionShrineRecipe r)) {
                problems.add(id + ": manca la ricetta Fusion Shrine");
                continue;
            }
            T7Spec spec = T7_SPEC.get(kind);
            var ing = r.getIngredientStacks();
            if (ing.size() != 3) {
                problems.add(id + ": " + ing.size() + " ingredienti invece di 3");
                continue;
            }
            if (!ing.get(0).test(new ItemStack(t6))) problems.add(id + ": il primo ingrediente non è " + id(t6));
            if (ing.get(1).getCount() != spec.vibranium()) problems.add(id + ": vibranium " + ing.get(1).getCount() + " invece di " + spec.vibranium());
            if (ing.get(1).getItems().noneMatch(i -> BuiltInRegistries.ITEM.getKey(i.getItem()).getPath().contains("vibranium")))
                problems.add(id + ": il secondo ingrediente non è vibranium");
            if (ing.get(2).getCount() != spec.onyx() || ing.get(2).getItems().noneMatch(i -> i.is(SpectrumItems.ONYX_POWDER.get())))
                problems.add(id + ": onice " + ing.get(2).getCount() + " invece di " + spec.onyx());
            if (!BuiltInRegistries.FLUID.getKey(r.getFluid().getStacks()[0].getFluid()).toString().equals("spectrum:dragonrot"))
                problems.add(id + ": il liquido non è Dragonrot");
            if (r.getCraftingTime() != spec.time()) problems.add(id + ": tempo " + r.getCraftingTime() + " invece di " + spec.time());
            if (Math.abs(r.getExperience() - spec.xp()) > 1e-4) problems.add(id + ": xp " + r.getExperience() + " invece di " + spec.xp());
            if (!r.areYieldAndEfficiencyUpgradesDisabled()) problems.add(id + ": upgrade Yield non disabilitati");
            var got = enchMap(r.getResultItem(access));
            if (!got.equals(spec.enchantments())) problems.add(id + ": incantesimi del risultato " + got + " invece di " + spec.enchantments());
            Map<String, Integer> dflt = new HashMap<>();
            SAItems.defaultEnchantments(t7).forEach((k, v) -> dflt.put(k.location().getPath(), v));
            if (!dflt.equals(spec.enchantments())) problems.add(id + ": gli incantesimi di default dell'oggetto sono " + dflt);
            report.add(id + " " + got);
        }
        for (var e : T6_FIXED.entrySet()) {
            Item t6 = SAFunctionTests.item(SATier.T6, e.getKey());
            var holder = manager.byKey(SpectralAscension.id("fusion_shrine/t6/" + id(t6)));
            if (holder.isEmpty()) {
                problems.add(id(t6) + ": manca la ricetta");
                continue;
            }
            var got = enchMap(((FusionShrineRecipe) holder.get().value()).getResultItem(access));
            if (!got.equals(e.getValue())) problems.add(id(t6) + ": incantesimi del risultato " + got + " invece di " + e.getValue());
        }
        finish(helper, "ricette T7 e T6 corrette", problems, report);
    }

    /** Le 15 ricette T8 sono tutte Fusion Shrine (Dragonrot, pezzo T7 + unobtainium + moonstone), niente Pedestal. */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void t8_fusion_recipes(GameTestHelper helper) {
        List<String> problems = new ArrayList<>(), report = new ArrayList<>();
        var level = helper.getLevel();
        var access = level.registryAccess();
        var manager = level.getServer().getRecipeManager();
        for (String kind : KINDS) {
            Item t8 = SAFunctionTests.item(SATier.T8, kind), t7 = SAFunctionTests.item(SATier.T7, kind);
            String id = id(t8);
            if (manager.byKey(SpectralAscension.id("pedestal/tier4/t8/" + id)).isPresent()) problems.add(id + ": esiste ancora la ricetta Pedestal");
            var holder = manager.byKey(SpectralAscension.id("fusion_shrine/t8/" + id));
            if (holder.isEmpty() || !(holder.get().value() instanceof FusionShrineRecipe r)) {
                problems.add(id + ": manca la ricetta Fusion Shrine");
                continue;
            }
            T8Spec spec = T8_SPEC.get(kind);
            var ing = r.getIngredientStacks();
            if (ing.size() != 3) {
                problems.add(id + ": " + ing.size() + " ingredienti invece di 3");
                continue;
            }
            if (!ing.get(0).test(new ItemStack(t7))) problems.add(id + ": il primo ingrediente non è " + id(t7));
            if (ing.get(1).getCount() != spec.unobtainium()) problems.add(id + ": unobtainium " + ing.get(1).getCount() + " invece di " + spec.unobtainium());
            if (ing.get(1).getItems().noneMatch(i -> BuiltInRegistries.ITEM.getKey(i.getItem()).getPath().contains("unobtainium")))
                problems.add(id + ": il secondo ingrediente non è unobtainium");
            if (ing.get(2).getCount() != spec.moonstone() || ing.get(2).getItems().noneMatch(i -> i.is(SpectrumItems.MOONSTONE_POWDER.get())))
                problems.add(id + ": moonstone " + ing.get(2).getCount() + " invece di " + spec.moonstone());
            if (!BuiltInRegistries.FLUID.getKey(r.getFluid().getStacks()[0].getFluid()).toString().equals("spectrum:dragonrot"))
                problems.add(id + ": il liquido non è Dragonrot");
            if (r.getCraftingTime() != spec.time()) problems.add(id + ": tempo " + r.getCraftingTime() + " invece di " + spec.time());
            if (Math.abs(r.getExperience() - spec.xp()) > 1e-4) problems.add(id + ": xp " + r.getExperience() + " invece di " + spec.xp());
            if (!r.areYieldAndEfficiencyUpgradesDisabled()) problems.add(id + ": upgrade Yield non disabilitati");
            var got = enchMap(r.getResultItem(access));
            if (!got.equals(spec.enchantments())) problems.add(id + ": incantesimi del risultato " + got + " invece di " + spec.enchantments());
            Map<String, Integer> dflt = new HashMap<>();
            SAItems.defaultEnchantments(t8).forEach((k, v) -> dflt.put(k.location().getPath(), v));
            if (!dflt.equals(spec.enchantments())) problems.add(id + ": gli incantesimi di default dell'oggetto sono " + dflt);
            report.add(id + " " + got);
        }
        finish(helper, "ricette T8", problems, report);
    }

    /**
     * Le 15 ricette T9 sono tutte Fusion Shrine (Dragonrot, pezzo T8 + 1 lingotto per ciascuna delle 3 leghe + Colorful
     * Shooting Star, una per slot perché si impilano a 1, + Stardust), niente Pedestal e niente polveri di gemma.
     */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void t9_fusion_recipes(GameTestHelper helper) {
        List<String> problems = new ArrayList<>(), report = new ArrayList<>();
        var level = helper.getLevel();
        var access = level.registryAccess();
        var manager = level.getServer().getRecipeManager();
        List<String> alloyNames = List.of("vibranium_allthemodium_alloy", "unobtainium_allthemodium_alloy", "unobtainium_vibranium_alloy");
        Item star = SpectrumBlocks.COLORFUL_SHOOTING_STAR.get().asItem();
        report.add("Colorful Shooting Star: pila massima " + new ItemStack(star).getMaxStackSize());
        for (String kind : KINDS) {
            Item t9 = SAFunctionTests.item(SATier.ALLOY, kind), t8 = SAFunctionTests.item(SATier.T8, kind);
            String id = id(t9);
            if (manager.byKey(SpectralAscension.id("pedestal/tier4/t9/" + id)).isPresent()) problems.add(id + ": esiste ancora la ricetta Pedestal");
            var holder = manager.byKey(SpectralAscension.id("fusion_shrine/t9/" + id));
            if (holder.isEmpty() || !(holder.get().value() instanceof FusionShrineRecipe r)) {
                problems.add(id + ": manca la ricetta Fusion Shrine");
                continue;
            }
            T9Spec spec = T9_SPEC.get(kind);
            var ing = r.getIngredientStacks();
            // La Colorful Shooting Star si impila a 1: ogni stella ha il suo slot (count 1), poi la Stardust.
            int expectedSize = 5 + spec.stars();
            if (ing.size() != expectedSize) {
                problems.add(id + ": " + ing.size() + " ingredienti invece di " + expectedSize);
                continue;
            }
            if (!ing.get(0).test(new ItemStack(t8))) problems.add(id + ": il primo ingrediente non è " + id(t8));
            for (int a = 0; a < 3; a++) {
                var slot = ing.get(1 + a);
                Item ingot = BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("allthemodium", alloyNames.get(a) + "_ingot"));
                if (slot.getCount() != 1) problems.add(id + ": " + alloyNames.get(a) + " " + slot.getCount() + " invece di 1");
                if (!slot.test(new ItemStack(ingot, 64))) problems.add(id + ": l'ingrediente " + (2 + a) + " non è " + alloyNames.get(a));
            }
            for (int n = 0; n < spec.stars(); n++) {
                var slot = ing.get(4 + n);
                if (slot.getCount() != 1 || slot.getItems().noneMatch(i -> i.is(star)))
                    problems.add(id + ": slot " + (5 + n) + " non è 1 Colorful Shooting Star");
            }
            var dust = ing.get(4 + spec.stars());
            if (dust.getCount() != spec.stardust() || dust.getItems().noneMatch(i -> i.is(SpectrumItems.STARDUST.get())))
                problems.add(id + ": Stardust " + dust.getCount() + " invece di " + spec.stardust());
            if (!BuiltInRegistries.FLUID.getKey(r.getFluid().getStacks()[0].getFluid()).toString().equals("spectrum:dragonrot"))
                problems.add(id + ": il liquido non è Dragonrot");
            if (r.getCraftingTime() != spec.time()) problems.add(id + ": tempo " + r.getCraftingTime() + " invece di " + spec.time());
            if (Math.abs(r.getExperience() - spec.xp()) > 1e-4) problems.add(id + ": xp " + r.getExperience() + " invece di " + spec.xp());
            if (!r.areYieldAndEfficiencyUpgradesDisabled()) problems.add(id + ": upgrade Yield non disabilitati");
            var got = enchMap(r.getResultItem(access));
            if (!got.equals(spec.enchantments())) problems.add(id + ": incantesimi del risultato " + got + " invece di " + spec.enchantments());
            Map<String, Integer> dflt = new HashMap<>();
            SAItems.defaultEnchantments(t9).forEach((k, v) -> dflt.put(k.location().getPath(), v));
            if (!dflt.equals(spec.enchantments())) problems.add(id + ": gli incantesimi di default dell'oggetto sono " + dflt);
            report.add(id + " " + got);
        }
        finish(helper, "ricette T9", problems, report);
    }

    private static String enchList(ItemStack stack) {
        List<String> out = new ArrayList<>();
        stack.getEnchantments().entrySet().forEach(e -> out.add(e.getKey().getRegisteredName().replace("minecraft:", "") + " " + e.getIntValue()));
        return out.toString();
    }

    /** La base di una ricetta: l'oggetto craftato prima o, al T6, quello di Spectrum con un incantesimo extra. */
    private static ItemStack baseStack(GameTestHelper helper, Item baseItem, ItemStack previous) {
        if (!previous.isEmpty() && previous.is(baseItem)) return previous.copy();
        ItemStack stack = new ItemStack(baseItem);
        var reg = helper.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
        var ench = new net.minecraft.world.item.enchantment.ItemEnchantments.Mutable(net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
        if (baseItem instanceof Preenchanted p) {
            Preenchanted.buildDefaultEnchantments(helper.getLevel().registryAccess(), p).entrySet().forEach(e -> ench.set(e.getKey(), e.getIntValue()));
        }
        for (var extra : List.of(net.minecraft.world.item.enchantment.Enchantments.LOOTING, net.minecraft.world.item.enchantment.Enchantments.LURE,
                net.minecraft.world.item.enchantment.Enchantments.PUNCH, net.minecraft.world.item.enchantment.Enchantments.THORNS,
                net.minecraft.world.item.enchantment.Enchantments.MULTISHOT, net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH)) {
            Holder<Enchantment> h = reg.getOrThrow(extra);
            if (stack.supportsEnchantment(h) && ench.keySet().stream().allMatch(k -> Enchantment.areCompatible(k, h)) && !(baseItem.toString().contains("workstaff"))) {
                ench.set(h, Math.min(3, h.value().getMaxLevel()));
                break;
            }
        }
        stack.set(DataComponents.ENCHANTMENTS, ench.toImmutable());
        return stack;
    }

    private static ItemStack fusionCraft(GameTestHelper helper, FusionShrineRecipe recipe, ItemStack previous, BlockPos pos,
                                         List<String> problems, String id) {
        var level = helper.getLevel();
        level.setBlockAndUpdate(pos, SpectrumBlocks.FUSION_SHRINE_BASALT.get().defaultBlockState());
        var shrine = (FusionShrineBlockEntity) level.getBlockEntity(pos);
        var fluid = recipe.getFluid().getStacks()[0].getFluid();
        shrine.getFluidTank().fill(new FluidStack(fluid, shrine.getFluidTank().getCapacity()), IFluidHandler.FluidAction.EXECUTE);
        var ingredients = recipe.getIngredientStacks();
        for (int i = 0; i < ingredients.size(); i++) {
            var ing = ingredients.get(i);
            ItemStack first = ing.getItems().findFirst().orElse(ItemStack.EMPTY);
            ItemStack put = i == 0 ? baseStack(helper, first.getItem(), previous) : first.copyWithCount(ing.getCount());
            if (i == 1 && first.is(SpectrumItems.FRACTAL_GLASS_CREST_BIDENT.get())) put = new ItemStack(first.getItem());
            shrine.setItem(i, put);
        }
        ItemStack result = ItemStack.EMPTY;
        if (!recipe.matches(shrine.getRecipeInput(), level)) {
            problems.add(id + ": la Fusion Shrine non riconosce la ricetta con gli ingredienti giusti");
        } else {
            recipe.craft(level, shrine);
            Item out = recipe.getResultItem(level.registryAccess()).getItem();
            var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(4));
            result = drops.stream().map(ItemEntity::getItem).filter(s -> s.is(out)).findFirst().map(ItemStack::copy).orElse(ItemStack.EMPTY);
            if (result.isEmpty()) problems.add(id + ": la Fusion Shrine non ha prodotto niente");
            for (int i = 0; i < shrine.getContainerSize(); i++) {
                if (!shrine.getItem(i).isEmpty()) problems.add(id + ": dopo il craft resta " + shrine.getItem(i) + " nello slot " + i);
            }
        }
        level.getEntitiesOfClass(Entity.class, new AABB(pos).inflate(4), e -> e instanceof ItemEntity || e instanceof ExperienceOrb).forEach(Entity::discard);
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        return result;
    }
}
