package cromocon.spectralascension.gametest;

import cromocon.spectralascension.SpectralAscension;
import cromocon.spectralascension.item.SAEnchantCap;
import cromocon.spectralascension.item.SATier;
import de.dafuqs.spectrum.recipe.enchanter.EnchantmentUpgradeRecipe;
import de.dafuqs.spectrum.registries.SpectrumAdvancements;
import de.dafuqs.spectrum.registries.SpectrumItems;
import de.dafuqs.spectrum.registries.SpectrumRecipeTypes;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import static cromocon.spectralascension.gametest.SATestUtil.*;

/**
 * Tetto degli incantesimi con il VERO Apothic Enchanting caricato (dev: {@code ./gradlew runGameTestServer -Papothic},
 * jar scaricati da Gradle da Modrinth). Senza Apothic questi test non vengono nemmeno registrati (il generatore
 * restituisce una lista vuota): la run normale resta identica.
 *
 * <p>Batch separato ({@code sa_apothic}) perché l'Enchanter si piazza nello stesso punto dei test di SAEdgeCaseTests.
 */
@GameTestHolder(SpectralAscension.MOD_ID)
public final class SAApothicTests {
    private static final String APOTHIC = "apothic_enchanting";

    private SAApothicTests() {
    }

    @GameTestGenerator
    public static Collection<TestFunction> apothicTests() {
        if (!ModList.get().isLoaded(APOTHIC)) return List.of();
        return List.of(
                test("apothic_max_levels_match_table", SAApothicTests::maxLevelsMatchTable),
                test("apothic_extraction_tome", SAApothicTests::extractionTome),
                test("apothic_prismatic_web", SAApothicTests::prismaticWeb),
                test("apothic_vanilla_anvil", SAApothicTests::vanillaAnvil),
                test("apothic_enchanter_extracted_book", SAApothicTests::enchanterExtractedBook),
                test("apothic_overenchanting", SAApothicTests::overenchanting));
    }

    private static TestFunction test(String name, Consumer<GameTestHelper> body) {
        return new TestFunction("sa_apothic", name, SpectralAscension.MOD_ID + ":empty", 200, 0L, true, body);
    }

    private static Holder<Enchantment> ench(GameTestHelper helper, ResourceKey<Enchantment> key) {
        return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }

    /** Massimo di Apothic a runtime: dev.shadowsoffire.apothic_enchanting.asm.EnchHooks.getMaxLevel (config ATM10). */
    private static int apothicMax(Enchantment enchantment) {
        try {
            Method m = Class.forName("dev.shadowsoffire.apothic_enchanting.asm.EnchHooks").getMethod("getMaxLevel", Enchantment.class);
            return (int) m.invoke(null, enchantment);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("EnchHooks.getMaxLevel non raggiungibile", e);
        }
    }

    private static void prismaticWeb(GameTestHelper helper) {
        var player = mockPlayer(helper);
        var curse = ench(helper, Enchantments.VANISHING_CURSE);
        var sharpness = ench(helper, Enchantments.SHARPNESS);
        ItemStack sword = enchanted(helper, SAFunctionTests.item(SATier.ALLOY, "sword"));
        sword.enchant(curse, 1);
        Item web = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(APOTHIC, "prismatic_web"));
        helper.assertTrue(web != Items.AIR, "Prismatic Web mancante");

        ItemStack output = SAEdgeCaseTests.anvil(player, sword, new ItemStack(web));
        helper.assertTrue(output.is(sword.getItem()), "la Prismatic Web deve restituire la spada");
        helper.assertTrue(output.getEnchantments().getLevel(curse) == 0, "la maledizione è tornata");
        helper.assertTrue(output.getEnchantments().getLevel(sharpness) == sword.getEnchantments().getLevel(sharpness),
                "la Prismatic Web ha cambiato Sharpness");
        helper.succeed();
    }

    private static ItemStack extractionTome() {
        Item tome = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(APOTHIC, "extraction_tome"));
        if (tome == Items.AIR) throw new IllegalStateException("apothic_enchanting:extraction_tome non registrato");
        return new ItemStack(tome);
    }

    private static ItemStack sword(GameTestHelper helper, Holder<Enchantment> enchantment, int level) {
        ItemStack s = new ItemStack(Items.DIAMOND_SWORD);
        s.set(DataComponents.ENCHANTMENTS, new ItemEnchantments.Mutable(ItemEnchantments.EMPTY) {{ set(enchantment, level); }}.toImmutable());
        return s;
    }

    private static ItemEnchantments stored(ItemStack book) {
        return book.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
    }

    /**
     * Massimi di Apothic a runtime (EnchHooks.getMaxLevel, la funzione che il coremod mette al posto di
     * Enchantment.getMaxLevel nell'incudine e in altre classi vanilla) contro la tabella fissa di SAEnchantCap, per tutti
     * i 42 incantesimi. Ogni differenza è un problema (la tabella non corrisponde più ad ATM10).
     */
    private static void maxLevelsMatchTable(GameTestHelper helper) {
        List<String> problems = new ArrayList<>(), report = new ArrayList<>();
        var registry = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        List<String> same = new ArrayList<>();
        for (ResourceLocation id : SAEnchantCap.tableIds().stream().sorted().toList()) {
            Holder<Enchantment> e = registry.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT, id));
            int apothic = apothicMax(e.value()), table = SAEnchantCap.tableCap(e).orElse(-1);
            if (apothic != table) problems.add(id.getPath() + ": Apothic " + apothic + ", tabella " + table);
            else same.add(id.getPath() + " " + apothic);
        }
        report.add(same.size() + "/42 uguali: " + String.join(", ", same));
        // Enchantment.getMaxLevel chiamato da fuori (nostro codice, Spectrum) NON passa da Apothic: resta il JSON.
        Holder<Enchantment> sharp = ench(helper, Enchantments.SHARPNESS);
        report.add("Sharpness: Enchantment.getMaxLevel() = " + sharp.value().getMaxLevel() + ", EnchHooks.getMaxLevel = " + apothicMax(sharp.value()));
        if (sharp.value().getMaxLevel() != 5) problems.add("Enchantment.getMaxLevel() di Sharpness è " + sharp.value().getMaxLevel() + " e non 5 (JSON)");
        finish(helper, "Apothic: massimi a runtime contro la tabella", problems, report);
    }

    /**
     * Extraction Tome vero: AnvilMenu del server, spada T9 a sinistra e tomo a destra. createResult lancia
     * l'AnvilUpdateEvent: Apothic (ExtractionTomeItem.updateAnvil) mette in uscita un libro con tutti gli incantesimi,
     * poi il nostro listener LOWEST lo taglia al tetto. Una spada vanilla con Sharpness XII NON è tagliata alla fonte.
     */
    private static void extractionTome(GameTestHelper helper) {
        List<String> problems = new ArrayList<>(), report = new ArrayList<>();
        ServerPlayer player = mockPlayer(helper);
        Holder<Enchantment> sharpness = ench(helper, Enchantments.SHARPNESS);
        for (SATier tier : List.of(SATier.T6, SATier.ALLOY)) {
            ItemStack ours = enchanted(helper, SAFunctionTests.item(tier, "sword"));
            ItemStack out = SAEdgeCaseTests.anvil(player, ours, extractionTome());
            if (!out.is(Items.ENCHANTED_BOOK)) {
                problems.add(tier + ": l'Extraction Tome non dà un libro: " + out);
                continue;
            }
            ItemEnchantments book = stored(out);
            report.add(id(ours.getItem()) + " " + ours.getEnchantments().entrySet().stream()
                    .map(e -> e.getKey().unwrapKey().orElseThrow().location().getPath() + " " + e.getIntValue()).toList()
                    + " → libro " + book.entrySet().stream()
                    .map(e -> e.getKey().unwrapKey().orElseThrow().location().getPath() + " " + e.getIntValue()).toList());
            for (var e : ours.getEnchantments().entrySet()) {
                int want = Math.min(e.getIntValue(), SAEnchantCap.tableCap(e.getKey()).orElse(Integer.MAX_VALUE));
                if (book.getLevel(e.getKey()) != want)
                    problems.add(tier + ": libro " + e.getKey().getRegisteredName() + " " + book.getLevel(e.getKey()) + " invece di " + want);
            }
            if (book.size() != ours.getEnchantments().size()) problems.add(tier + ": il libro ha " + book.size() + " incantesimi invece di " + ours.getEnchantments().size());
        }
        int t9Sharp = stored(SAEdgeCaseTests.anvil(player, enchanted(helper, SAFunctionTests.item(SATier.ALLOY, "sword")), extractionTome())).getLevel(sharpness);
        if (t9Sharp != 9) problems.add("spada T9: libro estratto con Sharpness " + t9Sharp + " invece di 9");

        ItemStack vanilla = SAEdgeCaseTests.anvil(player, sword(helper, sharpness, 12), extractionTome());
        int vanillaLevel = stored(vanilla).getLevel(sharpness);
        report.add("spada di diamante Sharpness XII (non nostra) → libro Sharpness " + vanillaLevel + " (nessun taglio alla fonte)");
        if (vanillaLevel != 12) problems.add("libro da una spada non nostra: Sharpness " + vanillaLevel + " invece di 12");
        finish(helper, "Apothic: Extraction Tome vero", problems, report);
    }

    /**
     * Incudine vanilla con Apothic (tetto di AnvilMenu = EnchHooks.getMaxLevel, Sharpness IX). Per i NOSTRI oggetti
     * AnvilMenuMixin non abbassa mai un livello: T9 Sharpness XII + libro V o XII resta XII; T6 VII + libro V resta VII;
     * T6 VII + libro VII sale a VIII (regola normale); T8 IX + libro IX resta IX. Una spada di diamante (non nostra)
     * XII + libro V scende ancora a IX: per gli altri oggetti l'incudine resta quella di Apothic.
     */
    private static void vanillaAnvil(GameTestHelper helper) {
        List<String> problems = new ArrayList<>(), report = new ArrayList<>();
        ServerPlayer player = mockPlayer(helper);
        player.experienceLevel = 1000;
        Holder<Enchantment> sharpness = ench(helper, Enchantments.SHARPNESS);
        record Case(SATier tier, int own, int book, int want) {
        }
        for (Case c : List.of(new Case(SATier.ALLOY, 12, 5, 12), new Case(SATier.ALLOY, 12, 12, 12), new Case(SATier.T6, 7, 5, 7),
                new Case(SATier.T6, 7, 7, 8), new Case(SATier.T8, 9, 9, 9))) {
            ItemStack ours = enchanted(helper, SAFunctionTests.item(c.tier(), "sword"));
            int own = ours.getEnchantments().getLevel(sharpness);
            if (own != c.own()) problems.add(id(ours.getItem()) + ": Sharpness di serie " + own + " invece di " + c.own());
            var menu = SAEdgeCaseTests.anvilMenu(player, ours, SAEdgeCaseTests.book(sharpness, c.book()));
            ItemStack out = menu.getSlot(2).getItem();
            int got = out.isEmpty() ? -1 : out.getEnchantments().getLevel(sharpness);
            report.add(id(ours.getItem()) + " Sharpness " + own + " + libro " + c.book() + " → "
                    + (out.isEmpty() ? "nessun risultato" : "Sharpness " + got + ", costo " + menu.getCost() + " livelli")
                    + (got < own ? " (ABBASSATO)" : ""));
            if (got != c.want()) problems.add(id(ours.getItem()) + " " + own + " + libro " + c.book() + ": " + got + " invece di " + c.want());
        }
        var menu = SAEdgeCaseTests.anvilMenu(player, sword(helper, sharpness, 12), SAEdgeCaseTests.book(sharpness, 5));
        ItemStack out = menu.getSlot(2).getItem();
        int got = out.isEmpty() ? -1 : out.getEnchantments().getLevel(sharpness);
        report.add("spada di diamante Sharpness 12 + libro 5 → " + (out.isEmpty() ? "nessun risultato" : "Sharpness " + got + ", costo " + menu.getCost() + " livelli"));
        if (got != 9) problems.add("spada di diamante XII + libro V: " + got + " invece di 9 (oggetti non nostri: incudine di Apothic invariata)");
        finish(helper, "Apothic: incudine vanilla", problems, report);
    }

    /**
     * Libro estratto con il tomo vero da una spada vanilla Sharpness XII (non tagliato alla fonte) messo nelle bowl
     * dell'Enchanter di Spectrum con un Gilded Book al centro: esce Sharpness IX (nostro tetto all'Enchanter).
     */
    private static void enchanterExtractedBook(GameTestHelper helper) {
        List<String> problems = new ArrayList<>(), report = new ArrayList<>();
        ServerPlayer player = mockPlayer(helper);
        Holder<Enchantment> sharpness = ench(helper, Enchantments.SHARPNESS);
        ItemStack extracted = SAEdgeCaseTests.anvil(player, sword(helper, sharpness, 12), extractionTome());
        int in = stored(extracted).getLevel(sharpness);
        if (in != 12) problems.add("il libro estratto ha Sharpness " + in + " invece di 12");
        BlockPos pos = helper.absolutePos(new BlockPos(0, 110, 40));
        var enchanter = SAEdgeCaseTests.enchanterSetup(helper, pos, new ItemStack(SpectrumItems.GILDED_BOOK.get()), extracted);
        int got = SAEdgeCaseTests.enchanterBook(enchanter).getLevel(sharpness);
        report.add("libro estratto Sharpness " + in + " nelle bowl → Gilded Book Sharpness " + got);
        if (got != 9) problems.add("Enchanter: Sharpness " + got + " invece di 9");
        finish(helper, "Apothic: Enchanter con un libro estratto", problems, report);
    }

    /**
     * Overenchanting con Apothic: la ricetta di upgrade di Sharpness si ferma a levels + 1 e mai oltre il tetto;
     * isInNormalRange usa Enchantment.getMaxLevel di Spectrum (non deviato da Apothic: resta V); l'advancement scatta
     * con un libro oltre il massimo vanilla (Sharpness VI, e il libro XII estratto con il tomo), non con uno al V.
     */
    private static void overenchanting(GameTestHelper helper) {
        List<String> problems = new ArrayList<>(), report = new ArrayList<>();
        var level = helper.getLevel();
        Holder<Enchantment> sharpness = ench(helper, Enchantments.SHARPNESS);
        EnchantmentUpgradeRecipe recipe = level.getRecipeManager().getAllRecipesFor(SpectrumRecipeTypes.ENCHANTMENT_UPGRADE).stream()
                .map(h -> h.value()).filter(r -> r.getEnchantment().is(Enchantments.SHARPNESS)).findFirst().orElse(null);
        if (recipe == null) {
            problems.add("ricetta di upgrade di Sharpness mancante");
        } else {
            int raw = recipe.getLevelData().size() + 1, cap = recipe.getLevelCap();
            report.add("ricetta Sharpness: levels + 1 = " + raw + ", getLevelCap = " + cap + ", isInNormalRange(4) = " + recipe.isInNormalRange(4)
                    + ", isInNormalRange(5) = " + recipe.isInNormalRange(5));
            if (cap != Math.min(raw, 9) || cap > 9) problems.add("tetto della ricetta di Sharpness " + cap + " invece di " + Math.min(raw, 9));
            if (!recipe.isInNormalRange(4) || recipe.isInNormalRange(5)) problems.add("isInNormalRange non segue il massimo vanilla V");
        }
        var adv = level.getServer().getAdvancements().get(SpectrumAdvancements.OVERENCHANTING);
        helper.assertTrue(adv != null, "advancement mancante");
        ServerPlayer extracted = mockPlayer(helper);
        record Book(String name, ItemStack stack, boolean unlocks) {
        }
        ItemStack fromTome = SAEdgeCaseTests.anvil(extracted, sword(helper, sharpness, 12), extractionTome());
        for (Book b : List.of(new Book("Sharpness V", SAEdgeCaseTests.book(sharpness, 5), false), new Book("Sharpness VI", SAEdgeCaseTests.book(sharpness, 6), true),
                new Book("Sharpness XII estratto con il tomo", fromTome, true))) {
            ServerPlayer player = mockPlayer(helper);
            player.getInventory().add(b.stack().copy());
            CriteriaTriggers.INVENTORY_CHANGED.trigger(player, player.getInventory(), b.stack());
            boolean done = player.getAdvancements().getOrStartProgress(adv).isDone();
            report.add("libro " + b.name() + ": overenchanting " + (done ? "sbloccato" : "no"));
            if (done != b.unlocks()) problems.add("libro " + b.name() + ": overenchanting " + (done ? "sbloccato" : "non sbloccato"));
        }
        finish(helper, "Apothic: overenchanting", problems, report);
    }
}
