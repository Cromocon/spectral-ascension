package cromocon.spectralascension.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import cromocon.spectralascension.SpectralAscension;
import cromocon.spectralascension.item.SAArmorItem;
import cromocon.spectralascension.item.SATier;
import cromocon.spectralascension.item.SAEnchantCap;
import cromocon.spectralascension.item.SATools;
import cromocon.spectralascension.registry.SAItems;
import de.dafuqs.fractal.api.CreativeSubTab;
import de.dafuqs.fractal.interfaces.ICreativeTabParent;
import de.dafuqs.spectrum.api.color.ColorRegistry;
import de.dafuqs.spectrum.api.ink.color.InkColors;
import de.dafuqs.spectrum.api.item.Preenchanted;
import de.dafuqs.spectrum.api.item_group.ItemGroupIDs;
import de.dafuqs.spectrum.attachment_types.AzureDikeAttachmentType;
import de.dafuqs.spectrum.registries.SpectrumItemGroups;
import de.dafuqs.spectrum.api.item.ExperienceStorageItem;
import de.dafuqs.spectrum.inventories.BedrockAnvilScreenHandler;
import de.dafuqs.spectrum.recipe.enchanter.EnchantmentUpgradeRecipe;
import de.dafuqs.spectrum.registries.SpectrumAdvancements;
import de.dafuqs.spectrum.registries.SpectrumRecipeTypes;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import de.dafuqs.spectrum.blocks.enchanter.EnchanterBlockEntity;
import de.dafuqs.spectrum.blocks.item_bowl.ItemBowlBlockEntity;
import de.dafuqs.spectrum.registries.SpectrumBlocks;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import de.dafuqs.spectrum.registries.SpectrumItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.AABB;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.DeferredItem;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cromocon.spectralascension.gametest.SATestUtil.*;

/**
 * Registrazione, asset e casi limite di tutti i 45 oggetti: nomi en/it, modelli e texture di ogni stato, armatura a
 * 20 fotogrammi, colore Ink, sotto-tab Equipment del creativo, tooltip sul server dedicato, classi caricabili senza
 * client, indistruttibilità con Unbreaking/Mending, incudine, morte con drop e raccolta, respawn.
 */
@GameTestHolder(SpectralAscension.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SAEdgeCaseTests {
    private static final String EMPTY = "empty";

    private SAEdgeCaseTests() {
    }

    private static Path resource(String path) {
        return ModList.get().getModFileById(SpectralAscension.MOD_ID).getFile().findResource(path.split("/"));
    }

    private static JsonObject json(String path) throws IOException {
        return JsonParser.parseString(Files.readString(resource(path))).getAsJsonObject();
    }

    private static String kind(Item item) {
        String id = id(item);
        return id.substring(id.indexOf('_') + 1);
    }

    /** Stati del modello attesi per tipo (oltre al modello base): suffissi dei modelli raggiungibili dagli override. */
    private static final Map<String, List<String>> STATES = Map.of(
            "bow", List.of("_pulling_0", "_pulling_1", "_pulling_2"),
            "fishing_rod", List.of("_cast"),
            "bident", List.of("_oversized", "_oversized_throwing"),
            "workstaff", List.of("_oversized"),
            "ultra_greatsword", List.of("_oversized"),
            "crossbow", List.of("_standby_oversized", "_pulling_0", "_pulling_1", "_pulling_2", "_arrow", "_firework", "_glass",
                    "_topaz", "_amethyst", "_citrine", "_onyx", "_moonstone",
                    "_pulling_0_oversized", "_pulling_1_oversized", "_pulling_2_oversized", "_arrow_oversized", "_firework_oversized",
                    "_glass_oversized", "_topaz_oversized", "_amethyst_oversized", "_citrine_oversized", "_onyx_oversized", "_moonstone_oversized"));

    /**
     * Ogni oggetto: nome in inglese e in italiano, modello, ogni modello degli override (stati) e ogni texture nostra
     * che i modelli usano. Armatura: 20 fotogrammi indossati per tier (40 l'Alloy). Chiavi di traduzione dei tooltip.
     */
    @GameTest(template = EMPTY)
    public static void assets_and_names(GameTestHelper helper) throws IOException {
        List<String> problems = new ArrayList<>();
        JsonObject en = json("assets/spectral_ascension/lang/en_us.json");
        JsonObject it = json("assets/spectral_ascension/lang/it_it.json");
        int models = 0, textures = 0;
        for (var holder : SAItems.all()) {
            Item item = holder.get();
            String id = id(item);
            String key = item.getDescriptionId();
            if (!en.has(key) || en.get(key).getAsString().isBlank()) problems.add(id + ": manca il nome inglese " + key);
            if (!it.has(key) || it.get(key).getAsString().isBlank()) problems.add(id + ": manca il nome italiano " + key);
            if (en.has(key) && it.has(key) && en.get(key).equals(it.get(key)) && !id.contains("workstaff")) problems.add(id + ": nome italiano uguale all'inglese");
            Set<String> seen = new HashSet<>();
            List<String> todo = new ArrayList<>(List.of("spectral_ascension:item/" + id));
            while (!todo.isEmpty()) {
                String model = todo.removeLast();
                if (!seen.add(model) || !model.startsWith("spectral_ascension:")) continue;
                Path p = resource("assets/spectral_ascension/models/" + model.substring(model.indexOf(':') + 1) + ".json");
                if (!Files.exists(p)) {
                    problems.add(id + ": modello mancante " + model);
                    continue;
                }
                models++;
                JsonObject m = JsonParser.parseString(Files.readString(p)).getAsJsonObject();
                if (m.has("parent")) todo.add(m.get("parent").getAsString());
                if (m.has("textures")) {
                    for (var t : m.getAsJsonObject("textures").entrySet()) {
                        String tex = t.getValue().getAsString();
                        if (!tex.startsWith("spectral_ascension:")) continue;
                        textures++;
                        if (!Files.exists(resource("assets/spectral_ascension/textures/" + tex.substring(tex.indexOf(':') + 1) + ".png")))
                            problems.add(id + ": texture mancante " + tex + " (in " + model + ")");
                    }
                }
                if (m.has("overrides")) {
                    for (JsonElement o : m.getAsJsonArray("overrides")) todo.add(o.getAsJsonObject().get("model").getAsString());
                }
            }
            for (String state : STATES.getOrDefault(kind(item), List.of())) {
                if (!seen.contains("spectral_ascension:item/" + id + state)) problems.add(id + ": nessun override porta allo stato " + state);
            }
        }
        // Fotogrammi dell'armatura indossata: 20 per T6, T7 e T8, 40 per l'Alloy (Prismatic).
        Map<SATier, Integer> frames = Map.of(SATier.T6, 20, SATier.T7, 20, SATier.T8, 20, SATier.ALLOY, 40);
        for (var fe : frames.entrySet()) {
            SATier tier = fe.getKey();
            int n = fe.getValue();
            for (int f = 0; f < n; f++) {
                if (!Files.exists(resource("assets/spectral_ascension/textures/armor/" + tier.idPrefix() + "_armor_" + f + ".png")))
                    problems.add(tier + ": manca il fotogramma " + f + " dell'armatura indossata");
            }
            if (Files.exists(resource("assets/spectral_ascension/textures/armor/" + tier.idPrefix() + "_armor_" + n + ".png")))
                problems.add(tier + ": fotogramma " + n + " in più (l'armatura ne usa " + n + ")");
            for (var a : SAItems.armor(tier)) {
                if (a.get().armorFrames() != n) problems.add(id(a.get()) + ": " + a.get().armorFrames() + " fotogrammi invece di " + n);
            }
        }
        for (String k : List.of("armor.tooltip.set_dike", "armor.tooltip.trinkets_disabled",
                "workstaff.aoe_cost", "workstaff.aoe_free", "workstaff.aoe_no_ink", "workstaff.aoe_ink_locked",
                "workstaff.foundry_paused", "workstaff.foundry_off", "workstaff.insertion_off", "workstaff.message.7x7",
                "workstaff.message.9x9", "workstaff.message.foundry_on", "workstaff.message.foundry_on_fortune",
                "workstaff.message.foundry_off", "workstaff.message.silk_touch_foundry_paused",
                "workstaff.message.insertion_on", "workstaff.message.insertion_off",
                "bident.mode", "bident.mode.off", "bident.mode.ferocious", "bident.mode.fractal", "bident.switch_hint", "bident.stored", "bident.inactive")) {
            String full = "item.spectral_ascension." + k;
            if (!en.has(full)) problems.add("manca la chiave inglese " + full);
            if (!it.has(full)) problems.add("manca la chiave italiana " + full);
        }
        finish(helper, "nomi, modelli e texture", problems, List.of(SAItems.all().size() + " oggetti, " + models + " modelli e "
                + textures + " riferimenti a texture controllati, armatura 3 × 20 + 40 (Alloy) fotogrammi"));
    }

    /**
     * Colore Ink di ogni oggetto (effetti della Fusion Shrine, /spectrum sanity): T6 arancio, T7 nero, T8 bianco,
     * Alloy magenta (PROVVISORIO, gen_data.FUSION_INK).
     */
    @GameTest(template = EMPTY)
    public static void ink_colors(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        // bedrock (la mazza di bedrock, 2026-11-25): nero come gli attrezzi bedrock di Spectrum.
        Map<String, Object> expected = Map.of("trichromatic", InkColors.ORANGE, "umbral", InkColors.BLACK, "argent", InkColors.WHITE,
                "prismatic", InkColors.MAGENTA, "bedrock", InkColors.BLACK);
        for (var holder : SAItems.all()) {
            Item item = holder.get();
            var color = ColorRegistry.ITEM_COLORS.getInkColor(item);
            Object want = expected.get(id(item).substring(0, id(item).indexOf('_')));
            if (color.isEmpty()) problems.add(id(item) + ": nessun colore Ink");
            else if (color.get() != want) problems.add(id(item) + ": colore " + color.get() + " invece di " + want);
        }
        finish(helper, "colore Ink", problems, List.of(SAItems.all().size() + " oggetti col colore del tier"));
    }

    /** Sotto-tab Equipment del creativo di Spectrum: tutti i 65 oggetti (64 dei tier + la mazza di bedrock), già incantati. */
    @GameTest(template = EMPTY)
    public static void creative_tab(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        var params = new CreativeModeTab.ItemDisplayParameters(helper.getLevel().enabledFeatures(), false, helper.getLevel().registryAccess());
        CreativeSubTab equipment = null;
        for (CreativeSubTab sub : ((ICreativeTabParent) SpectrumItemGroups.MAIN.get()).fractal$getChildren()) {
            if (sub.getIdentifier().equals(ItemGroupIDs.SUBTAB_EQUIPMENT)) {
                sub.buildContents(params);
                equipment = sub;
            }
        }
        if (equipment == null) {
            helper.fail("sotto-tab Equipment di Spectrum non trovata");
            return;
        }
        var shown = equipment.getDisplayItems();
        for (var holder : SAItems.all()) {
            Item item = holder.get();
            var stack = shown.stream().filter(s -> s.is(item)).findFirst();
            if (stack.isEmpty()) {
                problems.add(id(item) + ": non è nella sotto-tab Equipment");
                continue;
            }
            Map<ResourceKey<Enchantment>, Integer> want = SAItems.defaultEnchantments(item);
            var registry = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            for (var e : want.entrySet()) {
                if (stack.get().getEnchantments().getLevel(registry.getOrThrow(e.getKey())) != e.getValue())
                    problems.add(id(item) + ": nel creativo senza " + e.getKey().location() + " " + e.getValue());
            }
        }
        finish(helper, "creativo", problems, List.of(SAItems.all().size() + " oggetti nella sotto-tab Equipment, incantati"));
    }

    /**
     * Ricette dell'Alloy (T9): ogni ricetta chiede 1 lingotto di ciascuna delle 3 leghe (decisione dell'utente,
     * ricerca/14), mai il Piglich Heart (decisione dell'utente 2026-11-18). Dal 2026-11-19 sono tutte (16 con la mazza) alla Fusion
     * Shrine (Dragonrot), senza polveri di gemma (quantità e tempi: SACraftTests.t9_fusion_recipes).
     */
    @GameTest(template = EMPTY)
    public static void t9_alloy_recipes(GameTestHelper helper) {
        List<String> problems = new ArrayList<>(), report = new ArrayList<>();
        List<Item> alloys = List.of("vibranium_allthemodium_alloy_ingot", "unobtainium_allthemodium_alloy_ingot", "unobtainium_vibranium_alloy_ingot")
                .stream().map(n -> BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("allthemodium", n))).toList();
        Item heart = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("allthemodium", "piglich_heart"));
        for (Item a : alloys) if (a == net.minecraft.world.item.Items.AIR) problems.add("lingotto di lega inesistente");
        if (heart == net.minecraft.world.item.Items.AIR) problems.add("allthemodium:piglich_heart inesistente");
        int pedestal = 0, fusion = 0;
        for (var holder : helper.getLevel().getServer().getRecipeManager().getRecipes()) {
            if (!holder.id().getNamespace().equals(SpectralAscension.MOD_ID) || !holder.id().getPath().contains("/t9/")) continue;
            var r = holder.value();
            List<java.util.function.Predicate<ItemStack>> ingredients = new ArrayList<>();
            if (r instanceof de.dafuqs.spectrum.recipe.pedestal.PedestalRecipe pr) {
                pedestal++;
                pr.getIngredients().forEach(i -> ingredients.add(i::test));
            } else if (r instanceof de.dafuqs.spectrum.recipe.fusion_shrine.FusionShrineRecipe fr) {
                fusion++;
                fr.getIngredientStacks().forEach(i -> ingredients.add(i::test));
            }
            for (Item a : alloys) {
                long n = ingredients.stream().filter(i -> i.test(new ItemStack(a, 64))).count();
                if (n != 1) problems.add(holder.id() + ": " + n + " caselle con " + id(a) + " invece di 1");
            }
            if (ingredients.stream().anyMatch(i -> i.test(new ItemStack(heart, 64)))) problems.add(holder.id() + ": chiede il Piglich Heart");
        }
        if (pedestal != 0 || fusion != 16) problems.add("ricette T9: " + pedestal + " Pedestal e " + fusion + " Fusion invece di 0 e 16");
        report.add("ricette Alloy: " + pedestal + " al Pedestal e " + fusion + " alla Fusion Shrine, 1 lingotto per lega, niente Piglich Heart");
        finish(helper, "ricette Alloy", problems, report);
    }

    /**
     * Voci del guidebook (checklist §A): per ogni tier la voce è sbloccata dal tier; ogni oggetto ha una pagina ricetta
     * del tipo giusto (Fusion Shrine / Pedestal) con la ricetta che esiste e la condizione = l'advancement
     * dell'oggetto, lo stesso della ricetta; tutte le chiavi di testo esistono in inglese e in italiano.
     */
    @GameTest(template = EMPTY)
    public static void guidebook_pages(GameTestHelper helper) throws IOException {
        List<String> problems = new ArrayList<>();
        JsonObject en = json("assets/spectral_ascension/lang/en_us.json");
        JsonObject it = json("assets/spectral_ascension/lang/it_it.json");
        var manager = helper.getLevel().getServer().getRecipeManager();
        int pages = 0;
        for (SATier tier : List.of(SATier.T6, SATier.T7, SATier.T8, SATier.ALLOY)) {
            String t = "t" + (tier.ordinal() + 6);
            JsonObject entry = json("data/spectrum/modonomicon/books/guidebook/entries/equipment/spectral_ascension_" + t + "_equipment.json");
            String cond = entry.getAsJsonObject("condition").get("advancement_id").getAsString();
            if (!cond.equals("spectral_ascension:" + SAGameTests.gatePath(t))) problems.add(t + ": la voce è sbloccata da " + cond);
            // L'Idea (hint) esiste SOLO per il T6 (decisione dell'utente 2026-11-23).
            boolean hint = Files.exists(resource("data/spectrum/modonomicon/books/guidebook/entries/general/spectral_ascension_" + t + "_hint.json"));
            if (t.equals("t6") && !hint) problems.add(t + ": manca la voce Idea (hint)");
            if (!t.equals("t6") && hint) problems.add(t + ": c'è ancora la voce Idea (hint), che esiste solo per il T6");
            Map<String, JsonObject> byRecipe = new java.util.HashMap<>();
            for (JsonElement e : entry.getAsJsonArray("pages")) {
                JsonObject page = e.getAsJsonObject();
                for (String field : List.of("title", "text")) {
                    if (page.has(field)) {
                        String key = page.get(field).getAsString();
                        if (!en.has(key) && !key.startsWith("item.spectrum.")) problems.add(t + ": chiave inglese mancante " + key);
                        if (!it.has(key) && !key.startsWith("item.spectrum.")) problems.add(t + ": chiave italiana mancante " + key);
                    }
                }
                if (page.has("recipe_id")) byRecipe.put(page.get("recipe_id").getAsString(), page);
                pages++;
            }
            for (var holder : SAItems.equipment()) {
                String id = id(holder.get());
                if (!id.startsWith(tier.idPrefix() + "_")) continue;
                String fusionId = "spectral_ascension:fusion_shrine/" + t + "/" + id, pedId = "spectral_ascension:pedestal/tier4/" + t + "/" + id;
                boolean fusion = manager.byKey(ResourceLocation.parse(fusionId)).isPresent();
                JsonObject page = byRecipe.get(fusion ? fusionId : pedId);
                if (page == null) {
                    problems.add(id + ": nessuna pagina del libro con la sua ricetta");
                    continue;
                }
                String type = page.get("type").getAsString();
                if (!type.equals(fusion ? "spectrum:fusion_shrine_crafting" : "spectrum:pedestal_crafting")) problems.add(id + ": pagina di tipo " + type);
                String pc = page.getAsJsonObject("condition").get("advancement_id").getAsString();
                if (!pc.equals("spectral_ascension:unlocks/equipment/" + t + "/" + id)) problems.add(id + ": pagina sbloccata da " + pc);
                if (helper.getLevel().getServer().getAdvancements().get(ResourceLocation.parse(pc)) == null) problems.add(id + ": l'advancement della pagina non esiste");
            }
        }
        finish(helper, "guidebook", problems, List.of(pages + " pagine controllate: voci per tier, una pagina ricetta per oggetto, sblocchi uguali alle ricette"));
    }

    /** Oggetto di Spectrum equivalente (per capire se un problema è nostro o ereditato). */
    private static Item spectrumTwin(Item item) {
        String kind = kind(item);
        // Mazza (anche quella di bedrock): Spectrum non ne ha una, il gemello è la mazza vanilla.
        if (kind.equals("mace")) return Items.MACE;
        String twin = switch (kind) {
            case "workstaff", "ultra_greatsword", "crossbow" -> "glass_crest_" + kind;
            case "bident" -> "ferocious_glass_crest_bident";
            default -> "bedrock_" + kind;
        };
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("spectrum", twin));
    }

    /**
     * Tooltip calcolati sul SERVER DEDICATO (il server dei GameTest lo è): qualche mod li chiede anche lì. Un nostro
     * oggetto non deve rompersi dove il suo equivalente di Spectrum funziona. Poi: tutte le classi della mod fuori da
     * client/ si caricano senza il client.
     */
    @GameTest(template = EMPTY)
    public static void dedicated_server(GameTestHelper helper) {
        List<String> problems = new ArrayList<>(), report = new ArrayList<>();
        var context = Item.TooltipContext.of(helper.getLevel());
        for (var holder : SAItems.all()) {
            Item item = holder.get();
            ItemStack stack = enchanted(helper, item);
            Throwable ours = tooltipError(stack, context);
            Throwable theirs = tooltipError(new ItemStack(spectrumTwin(item)), context);
            // Il bident, lo spadone e la balestra si rompono dentro Spectrum (ExpandedStatTooltip carica Screen,
            // addInkPoweredTooltip è solo client) esattamente come i loro glass crest: è ereditato.
            if (ours != null && theirs == null) problems.add(id(item) + ": tooltip sul server → " + ours + " (Spectrum " + id(spectrumTwin(item)) + " no)");
            else if (ours != null) report.add(id(item) + ": tooltip sul server non disponibile, come per " + id(spectrumTwin(item)) + " di Spectrum (ereditato; il log di NeoForge"
                    + " mostra «Attempted to load class ... Screen» o un metodo solo client)");
        }
        int classes = 0;
        var scan = ModList.get().getModFileById(SpectralAscension.MOD_ID).getFile().getScanResult();
        for (var c : scan.getClasses()) {
            String name = c.clazz().getClassName();
            if (!name.startsWith("cromocon.spectralascension.") || name.contains(".client.") || name.contains(".mixin.")) continue;
            try {
                Class.forName(name, true, SpectralAscension.class.getClassLoader());
                classes++;
            } catch (Throwable t) {
                problems.add("la classe " + name + " non si carica sul server: " + t);
            }
        }
        report.add(classes + " classi caricate sul server senza errori");
        finish(helper, "server dedicato", problems, report);
    }

    private static Throwable tooltipError(ItemStack stack, Item.TooltipContext context) {
        try {
            stack.getTooltipLines(context, null, TooltipFlag.NORMAL);
            return null;
        } catch (Throwable t) {
            return t;
        }
    }

    /**
     * Indistruttibilità e incantesimi: nessun oggetto accetta Unbreaking o Mending (né da solo né all'incudine), non
     * ha barra della durata e non si consuma usandolo. All'incudine un libro compatibile si aggiunge senza toccare gli
     * incantesimi che c'erano; uno in conflitto (Smite sulla spada con Sharpness) no.
     */
    @GameTest(template = EMPTY)
    public static void unbreakable_and_anvil(GameTestHelper helper) {
        List<String> problems = new ArrayList<>(), report = new ArrayList<>();
        var registry = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> unbreaking = registry.getOrThrow(Enchantments.UNBREAKING), mending = registry.getOrThrow(Enchantments.MENDING);
        ServerPlayer player = mockPlayer(helper);
        player.experienceLevel = 1000;
        for (var holder : SAItems.all()) {
            Item item = holder.get();
            String id = id(item);
            ItemStack stack = enchanted(helper, item);
            // Con Apothic (ShearsItemMixin) le cesoie accettano Unbreaking, Efficiency e Fortune: decisione dell'utente
            // (2026-11-20) di lasciarlo, le nostre cesoie sono comunque indistruttibili. Senza Apothic: rifiutato.
            boolean unbreakingOk = ModList.get().isLoaded("apothic_enchanting") && item instanceof SATools.Shears;
            if (stack.supportsEnchantment(unbreaking) != unbreakingOk) problems.add(id + (unbreakingOk
                    ? ": NON accetta Unbreaking con Apothic (atteso dal ShearsItemMixin di Apothic)" : ": accetta Unbreaking"));
            if (unbreakingOk) report.add(id + ": accetta Unbreaking con Apothic (atteso, cesoie indistruttibili comunque)");
            if (stack.supportsEnchantment(mending)) problems.add(id + ": accetta Mending");
            if (stack.isDamageableItem()) problems.add(id + ": ha la durata");
            stack.hurtAndBreak(100, helper.getLevel(), player, i -> problems.add(id + ": si è rotto"));
            if (stack.getDamageValue() != 0) problems.add(id + ": danneggiato a " + stack.getDamageValue());
            for (Holder<Enchantment> book : List.of(unbreaking, mending)) {
                if (unbreakingOk && book == unbreaking) continue;
                ItemStack out = anvil(player, stack, EnchantedBookItem.createForEnchantment(new EnchantmentInstance(book, 1)));
                if (!out.isEmpty() && out.getEnchantments().getLevel(book) > 0) problems.add(id + ": l'incudine aggiunge " + book.getRegisteredName());
            }
        }
        // libro compatibile e libro in conflitto, sulla spada di ogni tier
        for (SATier tier : List.of(SATier.T6, SATier.T7, SATier.T8, SATier.ALLOY)) {
            Item sword = SAFunctionTests.item(tier, "sword");
            ItemStack stack = enchanted(helper, sword);
            int sharp = stack.getEnchantments().getLevel(registry.getOrThrow(Enchantments.SHARPNESS));
            ItemStack out = anvil(player, stack, EnchantedBookItem.createForEnchantment(new EnchantmentInstance(registry.getOrThrow(Enchantments.LOOTING), 3)));
            if (out.isEmpty() || out.getEnchantments().getLevel(registry.getOrThrow(Enchantments.LOOTING)) != 3)
                problems.add(id(sword) + ": l'incudine non aggiunge Looting III");
            else if (out.getEnchantments().getLevel(registry.getOrThrow(Enchantments.SHARPNESS)) != sharp)
                problems.add(id(sword) + ": aggiungendo Looting, Sharpness passa da " + sharp + " a " + out.getEnchantments().getLevel(registry.getOrThrow(Enchantments.SHARPNESS)));
            // Smite e Sharpness: in conflitto in vanilla; Apothic toglie Sharpness da #minecraft:exclusive_set/damage,
            // quindi con Apothic (ATM10) l'incudine DEVE aggiungere Smite V lasciando Sharpness com'era.
            boolean apothic = ModList.get().isLoaded("apothic_enchanting");
            boolean conflict = !Enchantment.areCompatible(registry.getOrThrow(Enchantments.SHARPNESS), registry.getOrThrow(Enchantments.SMITE));
            if (conflict == apothic) problems.add("Sharpness/Smite " + (conflict ? "in conflitto" : "compatibili") + (apothic ? " con" : " senza") + " Apothic");
            out = anvil(player, stack, EnchantedBookItem.createForEnchantment(new EnchantmentInstance(registry.getOrThrow(Enchantments.SMITE), 5)));
            int smite = out.isEmpty() ? 0 : out.getEnchantments().getLevel(registry.getOrThrow(Enchantments.SMITE));
            if (conflict && smite > 0) problems.add(id(sword) + ": l'incudine aggiunge Smite nonostante Sharpness");
            if (!conflict && (smite != 5 || out.getEnchantments().getLevel(registry.getOrThrow(Enchantments.SHARPNESS)) != sharp))
                problems.add(id(sword) + ": con Apothic l'incudine dovrebbe aggiungere Smite V e lasciare Sharpness " + sharp + ": " + out.getEnchantments());
            // stesso incantesimo: l'incudine vanilla taglia al massimo (JSON V senza Apothic, IX con Apothic), ma per i
            // NOSTRI oggetti AnvilMenuMixin non scende mai sotto il livello di partenza; l'aumento per regola resta.
            Holder<Enchantment> sharpness = registry.getOrThrow(Enchantments.SHARPNESS);
            int cap = apothic ? SAEnchantCap.tableCap(sharpness).orElseThrow() : sharpness.value().getMaxLevel();
            for (int bookLevel : new int[]{5, sharp}) {
                AnvilMenu menu = anvilMenu(player, stack, book(sharpness, bookLevel));
                ItemStack same = menu.getSlot(2).getItem();
                int after = same.isEmpty() ? -1 : same.getEnchantments().getLevel(sharpness);
                int rule = bookLevel == sharp ? Math.min(sharp + 1, cap) : Math.min(Math.max(sharp, bookLevel), cap);
                int want = Math.max(sharp, rule);
                report.add(id(sword) + " Sharpness " + sharp + " + libro Sharpness " + bookLevel + " → "
                        + (same.isEmpty() ? "nessun risultato" : "Sharpness " + after + ", costo " + menu.getCost() + " livelli")
                        + (apothic ? " (con Apothic)" : " (senza Apothic)"));
                if (after != want) problems.add(id(sword) + " Sharpness " + sharp + " + libro " + bookLevel + ": " + after + " invece di " + want);
            }
        }
        // spada vanilla: comportamento invariato, XII + libro V scende al massimo (V senza Apothic, IX con Apothic)
        {
            Holder<Enchantment> sharpness = registry.getOrThrow(Enchantments.SHARPNESS);
            boolean apothic = ModList.get().isLoaded("apothic_enchanting");
            int cap = apothic ? SAEnchantCap.tableCap(sharpness).orElseThrow() : sharpness.value().getMaxLevel();
            ItemStack diamond = new ItemStack(Items.DIAMOND_SWORD);
            diamond.enchant(sharpness, 12);
            AnvilMenu menu = anvilMenu(player, diamond, book(sharpness, 5));
            ItemStack out = menu.getSlot(2).getItem();
            int after = out.isEmpty() ? -1 : out.getEnchantments().getLevel(sharpness);
            report.add("spada di diamante Sharpness 12 + libro Sharpness 5 → " + (out.isEmpty() ? "nessun risultato" : "Sharpness " + after + ", costo " + menu.getCost() + " livelli"));
            if (after != cap) problems.add("spada di diamante XII + libro V: " + after + " invece di " + cap + " (vanilla deve restare com'era)");
        }
        finish(helper, "indistruttibilità e incudine", problems, report);
    }

    // ---------- Enchanter: l'overenchanting sbloccato dopo si vede senza toccarlo (EnchanterBlockEntityMixin) ----------

    /** Fa avanzare il tick reale dell'Enchanter e controlla se ha selezionato una ricetta. */
    private static boolean enchanterHasUpgradeRecipe(EnchanterBlockEntity enchanter) {
        var level = enchanter.getLevel();
        var pos = enchanter.getBlockPos();
        EnchanterBlockEntity.serverTick(level, pos, level.getBlockState(pos), enchanter);
        try {
            Field f = EnchanterBlockEntity.class.getDeclaredField("currentRecipe");
            f.setAccessible(true);
            return f.get(enchanter) != null;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Spectrum salva canOwnerOverenchant solo in setOwner. Un Enchanter con proprietario SENZA l'advancement rifiuta un
     * upgrade oltre il massimo vanilla (Thorns IV, massimo vanilla III); l'advancement arriva SENZA richiamare setOwner:
     * lo stesso Enchanter lo accetta (V), ma Thorns V resta rifiutato (tetto ATM10). Il proprietario deve essere «online»
     * (PlayerList.getPlayer): il giocatore finto viene inserito a mano in PlayerList.playersByUUID. Poi lo si toglie
     * (offline): l'Enchanter tiene il valore salvato (nessun errore, upgrade rifiutato).
     */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void enchanter_overenchant_live(GameTestHelper helper) {
        List<String> problems = new ArrayList<>(), report = new ArrayList<>();
        var level = helper.getLevel();
        var server = level.getServer();
        var registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> thorns = registry.getOrThrow(Enchantments.THORNS);
        var adv = server.getAdvancements().get(SpectrumAdvancements.OVERENCHANTING);
        helper.assertTrue(adv != null, "advancement mancante: " + SpectrumAdvancements.OVERENCHANTING);
        var recipe = level.getRecipeManager().getAllRecipesFor(SpectrumRecipeTypes.ENCHANTMENT_UPGRADE).stream()
                .map(h -> h.value()).filter(r -> r.getEnchantment().is(Enchantments.THORNS)).findFirst().orElse(null);
        helper.assertTrue(recipe != null, "ricetta di upgrade di Thorns mancante");
        BlockPos pos = helper.absolutePos(new BlockPos(0, 110, 40));

        ServerPlayer owner = mockPlayer(helper);
        Map<java.util.UUID, ServerPlayer> byUuid;
        try {
            Field f = net.minecraft.server.players.PlayerList.class.getDeclaredField("playersByUUID");
            f.setAccessible(true);
            @SuppressWarnings("unchecked") Map<java.util.UUID, ServerPlayer> m = (Map<java.util.UUID, ServerPlayer>) f.get(server.getPlayerList());
            byUuid = m;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        byUuid.put(owner.getUUID(), owner);
        try {
            // Thorns IV al centro, gemma con XP, ingredienti dell'upgrade da IV a V nelle bowl
            var data = recipe.getLevelData().get(3);
            ItemStack gem = new ItemStack(SpectrumItems.KNOWLEDGE_GEM.get());
            ExperienceStorageItem.addStoredExperience(level.registryAccess(), gem, 1_000_000);
            ItemStack[] bowls = new ItemStack[8];
            for (int i = 0; i < 8; i++) bowls[i] = new ItemStack(data.ingredient().getItems()[0].getItem(), data.countPerBowl());
            var enchanter = enchanterSetup(helper, pos, book(thorns, 4), bowls);
            enchanter.setItem(1, gem);
            enchanter.setOwner(owner); // senza l'advancement: canOwnerOverenchant = false
            enchanter.inventoryChanged();
            if (de.dafuqs.revelationary.api.advancements.AdvancementHelper.hasAdvancement(owner, SpectrumAdvancements.OVERENCHANTING))
                problems.add("il proprietario ha già l'advancement prima del test");
            boolean before = enchanterHasUpgradeRecipe(enchanter);
            report.add("Thorns IV, proprietario senza advancement: " + (before ? "upgrade accettato" : "upgrade rifiutato"));
            if (before) problems.add("senza l'advancement l'Enchanter accetta Thorns IV → V (oltre il massimo vanilla)");

            // advancement ottenuto DOPO, senza richiamare setOwner
            ItemStack above = book(thorns, 4);
            owner.getInventory().add(above.copy());
            CriteriaTriggers.INVENTORY_CHANGED.trigger(owner, owner.getInventory(), above);
            if (!de.dafuqs.revelationary.api.advancements.AdvancementHelper.hasAdvancement(owner, SpectrumAdvancements.OVERENCHANTING))
                problems.add("il libro Thorns IV non ha sbloccato l'overenchanting");
            boolean after = enchanterHasUpgradeRecipe(enchanter);
            report.add("stesso Enchanter, advancement ottenuto dopo (senza setOwner): " + (after ? "upgrade accettato" : "upgrade rifiutato"));
            if (!after) problems.add("l'Enchanter non si accorge dell'advancement ottenuto dopo");

            // controllo: con il campo di Spectrum a true (come dopo setOwner con l'advancement) l'upgrade IV → V è accettato
            try {
                Field cf = EnchanterBlockEntity.class.getDeclaredField("canOwnerOverenchant");
                cf.setAccessible(true);
                var control = enchanterSetup(helper, pos, book(thorns, 4), bowls);
                control.setItem(1, gem);
                cf.setBoolean(control, true);
                control.inventoryChanged();
                boolean ok = enchanterHasUpgradeRecipe(control);
                report.add("controllo (campo di Spectrum a true): " + (ok ? "upgrade accettato" : "upgrade rifiutato"));
                if (!ok) problems.add("controllo: la configurazione del test non produce un upgrade valido");
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }

            // tetto ATM10: Thorns V non sale a VI
            var enchanterV = enchanterSetup(helper, pos, book(thorns, 5), bowls);
            enchanterV.setItem(1, gem);
            enchanterV.setOwner(owner);
            enchanterV.inventoryChanged();
            boolean capped = enchanterHasUpgradeRecipe(enchanterV);
            report.add("Thorns V con l'advancement: " + (capped ? "upgrade accettato" : "upgrade rifiutato (tetto V)"));
            if (capped) problems.add("Thorns V → VI accettato oltre il tetto ATM10");

            // proprietario offline: valore salvato (false), nessun errore
            var enchanterOff = enchanterSetup(helper, pos, book(thorns, 4), bowls);
            enchanterOff.setItem(1, gem);
            enchanterOff.setOwner(mockPlayer(helper)); // non è in PlayerList: offline
            enchanterOff.inventoryChanged();
            boolean offline = enchanterHasUpgradeRecipe(enchanterOff);
            report.add("proprietario offline: " + (offline ? "upgrade accettato" : "upgrade rifiutato (valore salvato)"));
            if (offline) problems.add("proprietario offline: upgrade accettato senza valore salvato");
        } finally {
            byUuid.remove(owner.getUUID());
        }
        finish(helper, "Enchanter: overenchanting sbloccato dopo", problems, report);
    }

    // ---------- Tetto ATM10 alla copia sul Gilded Book (Enchanter di Spectrum, strada A di ricerca/03) ----------

    /** Enchanter vero con le 8 Item Bowl: oggetti nelle bowl e libro al centro, come in gioco. */
    static EnchanterBlockEntity enchanterSetup(GameTestHelper helper, BlockPos pos, ItemStack center, ItemStack... bowls) {
        var level = helper.getLevel();
        level.setBlockAndUpdate(pos, SpectrumBlocks.ENCHANTER.get().defaultBlockState());
        var enchanter = (EnchanterBlockEntity) level.getBlockEntity(pos);
        for (int i = 0; i < EnchanterBlockEntity.ITEM_BOWL_OFFSETS.size(); i++) {
            BlockPos bowlPos = pos.offset(EnchanterBlockEntity.ITEM_BOWL_OFFSETS.get(i));
            level.setBlockAndUpdate(bowlPos, SpectrumBlocks.ITEM_BOWL_BASALT.get().defaultBlockState());
            ((ItemBowlBlockEntity) level.getBlockEntity(bowlPos)).setItem(0, i < bowls.length ? bowls[i].copy() : ItemStack.EMPTY);
        }
        enchanter.setItem(0, center.copy());
        enchanter.inventoryChanged();
        return enchanter;
    }

    /** Livelli che l'Enchanter copierebbe dalle bowl e libro che ne esce (esegue davvero enchantCenterItem). */
    static ItemEnchantments enchanterBook(EnchanterBlockEntity enchanter) {
        EnchanterBlockEntity.enchantCenterItem(enchanter);
        return EnchantmentHelper.getEnchantmentsForCrafting(enchanter.getItem(0));
    }

    /**
     * Tetto ATM10 alla copia: ogni nostro oggetto nelle bowl, con il suo Gilded Book al centro, dà un libro i cui livelli
     * sono al massimo quelli di ATM10 (SAEnchantCap, tabella di ricerca/09). Un T9 con Sharpness XII dà Sharpness IX,
     * mai XII; gli incantesimi sotto il tetto (T6-T8) passano com'erano.
     */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void enchanter_copy_cap_ours(GameTestHelper helper) {
        List<String> problems = new ArrayList<>(), report = new ArrayList<>();
        var registry = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        BlockPos pos = helper.absolutePos(new BlockPos(0, 110, 40));
        int row = 0;
        for (var holder : SAItems.equipment()) {
            Item item = holder.get();
            String id = id(item);
            ItemStack stack = enchanted(helper, item);
            ItemEnchantments own = stack.getEnchantments();
            if (own.isEmpty()) continue;
            var enchanter = enchanterSetup(helper, pos.above(10 * (row++ % 4)), new ItemStack(SpectrumItems.GILDED_BOOK.get()), stack);
            ItemEnchantments copied = EnchanterBlockEntity.getHighestEnchantmentsInItemBowls(enchanter);
            ItemEnchantments book = enchanterBook(enchanter);
            for (var e : own.entrySet()) {
                int cap = SAEnchantCap.tableCap(e.getKey()).orElse(Integer.MAX_VALUE); // fuori tabella: nessun tetto
                int want = Math.min(e.getIntValue(), cap);
                if (copied.getLevel(e.getKey()) != want)
                    problems.add(id + ": la copia dà " + e.getKey().getRegisteredName() + " " + copied.getLevel(e.getKey()) + " invece di " + want + " (tetto " + cap + ")");
                if (book.getLevel(e.getKey()) > cap)
                    problems.add(id + ": il libro ha " + e.getKey().getRegisteredName() + " " + book.getLevel(e.getKey()) + " sopra il tetto " + cap);
            }
            if (!enchanter.getItem(0).is(Items.ENCHANTED_BOOK) && !enchanter.getItem(0).is(SpectrumItems.GILDED_BOOK.get()))
                problems.add(id + ": il libro che esce è " + enchanter.getItem(0));
        }
        // caso esplicito: T9 spada Sharpness XII → libro Sharpness IX; T8 spada resta com'è (IX)
        Holder<Enchantment> sharpness = registry.getOrThrow(Enchantments.SHARPNESS);
        for (SATier tier : List.of(SATier.T8, SATier.ALLOY)) {
            ItemStack sword = enchanted(helper, SAFunctionTests.item(tier, "sword"));
            int own = sword.getEnchantments().getLevel(sharpness);
            int got = enchanterBook(enchanterSetup(helper, pos, new ItemStack(SpectrumItems.GILDED_BOOK.get()), sword)).getLevel(sharpness);
            report.add("spada " + tier + ": Sharpness " + own + " sull'oggetto → libro Sharpness " + got + " (tetto ATM10 " + SAEnchantCap.tableCap(sharpness).getAsInt() + ")");
            if (got != Math.min(own, 9)) problems.add("spada " + tier + ": libro Sharpness " + got + " invece di " + Math.min(own, 9));
            if (tier == SATier.ALLOY && (own != 12 || got >= 12)) problems.add("spada Alloy: oggetto " + own + ", libro " + got + " (atteso 12 → 9)");
        }
        // il costo in XP è quello del livello tagliato, non del XII
        ItemStack t9 = enchanted(helper, SAFunctionTests.item(SATier.ALLOY, "sword"));
        ItemStack vanillaNine = new ItemStack(Items.DIAMOND_SWORD);
        vanillaNine.set(DataComponents.ENCHANTMENTS, new ItemEnchantments.Mutable(ItemEnchantments.EMPTY) {{ set(sharpness, 9); }}.toImmutable());
        int costT9 = EnchanterBlockEntity.getRequiredExperienceToEnchantCenterItem(
                enchanterSetup(helper, pos, new ItemStack(SpectrumItems.GILDED_BOOK.get()), t9));
        int costNine = EnchanterBlockEntity.getRequiredExperienceToEnchantCenterItem(
                enchanterSetup(helper, pos, new ItemStack(SpectrumItems.GILDED_BOOK.get()), vanillaNine));
        report.add("costo XP: spada T9 " + costT9 + ", spada vanilla Sharpness IX " + costNine);
        if (costT9 <= 0 || costT9 > costNine)
            problems.add("il costo XP della spada T9 (" + costT9 + ") supera quello di Sharpness IX (" + costNine + ")");
        finish(helper, "Enchanter: tetto ATM10 sulla copia", problems, report);
    }

    /**
     * Oggetti vanilla e Spectrum: sotto il tetto NON cambiano. Una spada di diamante con Sharpness V e Looting III dà un
     * libro uguale; una spada di bedrock di Spectrum (Sharpness VI di serie) dà VI. Dal 2026-11-19 (decisione utente,
     * «tetto per tutti») un oggetto non nostro oltre il tetto (Sharpness XII dato con un comando) dà IX come i nostri:
     * prima restava XII.
     */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void enchanter_copy_unchanged_others(GameTestHelper helper) {
        List<String> problems = new ArrayList<>(), report = new ArrayList<>();
        var registry = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> sharpness = registry.getOrThrow(Enchantments.SHARPNESS), looting = registry.getOrThrow(Enchantments.LOOTING);
        BlockPos pos = helper.absolutePos(new BlockPos(0, 110, 40));
        ItemStack gilded = new ItemStack(SpectrumItems.GILDED_BOOK.get());

        ItemStack diamond = new ItemStack(Items.DIAMOND_SWORD);
        diamond.set(DataComponents.ENCHANTMENTS, new ItemEnchantments.Mutable(ItemEnchantments.EMPTY) {{ set(sharpness, 5); set(looting, 3); }}.toImmutable());
        ItemEnchantments book = enchanterBook(enchanterSetup(helper, pos, gilded, diamond));
        report.add("spada vanilla Sharpness V + Looting III → libro Sharpness " + book.getLevel(sharpness) + ", Looting " + book.getLevel(looting));
        if (book.getLevel(sharpness) != 5 || book.getLevel(looting) != 3) problems.add("la copia da una spada vanilla è cambiata");

        ItemStack over = new ItemStack(Items.DIAMOND_SWORD);
        over.set(DataComponents.ENCHANTMENTS, new ItemEnchantments.Mutable(ItemEnchantments.EMPTY) {{ set(sharpness, 12); }}.toImmutable());
        int overLevel = enchanterBook(enchanterSetup(helper, pos, gilded, over)).getLevel(sharpness);
        report.add("spada vanilla con Sharpness XII (comando) → libro Sharpness " + overLevel + " (tetto per tutti: prima era XII)");
        if (overLevel != 9) problems.add("un oggetto non nostro oltre il tetto dà " + overLevel + " invece di 9");

        ItemStack bedrock = enchanted(helper, SpectrumItems.BEDROCK_SWORD.get());
        int bedrockLevel = enchanterBook(enchanterSetup(helper, pos, gilded, bedrock)).getLevel(sharpness);
        report.add("spada bedrock di Spectrum (Sharpness " + bedrock.getEnchantments().getLevel(sharpness) + ") → libro Sharpness " + bedrockLevel);
        if (bedrockLevel != bedrock.getEnchantments().getLevel(sharpness) || bedrockLevel == 0) problems.add("la copia dalla spada bedrock è cambiata: " + bedrockLevel);
        finish(helper, "Enchanter: oggetti non nostri (sotto il tetto invariati)", problems, report);
    }

    static ItemStack book(Holder<Enchantment> enchantment, int level) {
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        book.set(DataComponents.STORED_ENCHANTMENTS, new ItemEnchantments.Mutable(ItemEnchantments.EMPTY) {{ set(enchantment, level); }}.toImmutable());
        return book;
    }

    /**
     * Tetto per tutti all'Enchanter: un libro incantato qualsiasi con Sharpness XII nelle bowl dà Sharpness IX, con
     * l'XP di un libro IX. Un libro con un incantesimo fuori tabella (Spectrum, Clover's Favor X, max del JSON 3) NON è
     * tagliato.
     */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void enchanter_cap_books(GameTestHelper helper) {
        List<String> problems = new ArrayList<>(), report = new ArrayList<>();
        var registry = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> sharpness = registry.getOrThrow(Enchantments.SHARPNESS);
        Holder<Enchantment> clovers = registry.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.parse("spectrum:clovers_favor")));
        BlockPos pos = helper.absolutePos(new BlockPos(0, 110, 40));
        ItemStack gilded = new ItemStack(SpectrumItems.GILDED_BOOK.get());

        int got = enchanterBook(enchanterSetup(helper, pos, gilded, book(sharpness, 12))).getLevel(sharpness);
        report.add("libro Sharpness XII nelle bowl → libro Sharpness " + got);
        if (got != 9) problems.add("libro Sharpness XII → " + got + " invece di 9");
        int costTwelve = EnchanterBlockEntity.getRequiredExperienceToEnchantCenterItem(enchanterSetup(helper, pos, gilded, book(sharpness, 12)));
        int costNine = EnchanterBlockEntity.getRequiredExperienceToEnchantCenterItem(enchanterSetup(helper, pos, gilded, book(sharpness, 9)));
        report.add("costo XP: libro XII " + costTwelve + ", libro IX " + costNine);
        if (costTwelve <= 0 || costTwelve != costNine) problems.add("costo XP del libro XII " + costTwelve + " diverso da quello del IX " + costNine);

        int cloversMax = clovers.value().getMaxLevel();
        int gotClovers = enchanterBook(enchanterSetup(helper, pos, gilded, book(clovers, 10))).getLevel(clovers);
        report.add("libro Clover's Favor X (max JSON " + cloversMax + ", fuori tabella) → libro " + gotClovers);
        if (gotClovers != 10) problems.add("un incantesimo fuori tabella viene tagliato: Clover's Favor " + gotClovers + " invece di 10");
        if (SAEnchantCap.tableCap(clovers).isPresent()) problems.add("Clover's Favor risulta nella tabella del tetto");
        finish(helper, "Enchanter: tetto per tutti i libri", problems, report);
    }

    /**
     * Estrazione all'incudine vanilla (come l'Extraction Tome di Apothic, che nel dev non c'è): l'AnvilUpdateEvent con
     * la spada T9 a sinistra e un libro con tutti i suoi incantesimi già in uscita (lo mette Apothic a priorità normale)
     * passa dal nostro listener LOWEST → il libro ha Sharpness IX. Con una spada non nostra a sinistra il libro resta
     * com'era; con una nostra spada in uscita (libro a destra) non si tocca niente.
     */
    @GameTest(template = EMPTY)
    public static void anvil_extraction_cap(GameTestHelper helper) {
        List<String> problems = new ArrayList<>(), report = new ArrayList<>();
        var registry = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> sharpness = registry.getOrThrow(Enchantments.SHARPNESS);
        ServerPlayer player = mockPlayer(helper);
        ItemStack t9 = enchanted(helper, SAFunctionTests.item(SATier.ALLOY, "sword"));
        ItemStack all = new ItemStack(Items.ENCHANTED_BOOK);
        all.set(DataComponents.STORED_ENCHANTMENTS, t9.getEnchantments());

        var event = new AnvilUpdateEvent(t9, new ItemStack(Items.BOOK), "", 0, player);
        event.setOutput(all.copy());
        NeoForge.EVENT_BUS.post(event);
        ItemEnchantments stored = event.getOutput().getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        report.add("spada T9 (Sharpness " + t9.getEnchantments().getLevel(sharpness) + ") → libro estratto Sharpness " + stored.getLevel(sharpness));
        if (stored.getLevel(sharpness) != 9) problems.add("libro estratto dalla T9: Sharpness " + stored.getLevel(sharpness) + " invece di 9");
        for (var e : t9.getEnchantments().entrySet()) {
            int want = Math.min(e.getIntValue(), SAEnchantCap.tableCap(e.getKey()).orElse(Integer.MAX_VALUE));
            if (stored.getLevel(e.getKey()) != want)
                problems.add("libro estratto: " + e.getKey().getRegisteredName() + " " + stored.getLevel(e.getKey()) + " invece di " + want);
        }

        ItemStack diamond = new ItemStack(Items.DIAMOND_SWORD);
        diamond.set(DataComponents.ENCHANTMENTS, new ItemEnchantments.Mutable(ItemEnchantments.EMPTY) {{ set(sharpness, 12); }}.toImmutable());
        var other = new AnvilUpdateEvent(diamond, new ItemStack(Items.BOOK), "", 0, player);
        other.setOutput(book(sharpness, 12));
        NeoForge.EVENT_BUS.post(other);
        int otherLevel = other.getOutput().getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY).getLevel(sharpness);
        report.add("spada vanilla Sharpness XII → libro estratto Sharpness " + otherLevel + " (non nostra: invariato)");
        if (otherLevel != 12) problems.add("libro da una spada non nostra cambiato: " + otherLevel);

        var onItem = new AnvilUpdateEvent(t9, book(registry.getOrThrow(Enchantments.LOOTING), 3), "", 0, player);
        ItemStack sword = t9.copy();
        onItem.setOutput(sword);
        NeoForge.EVENT_BUS.post(onItem);
        if (onItem.getOutput().getEnchantments().getLevel(sharpness) != t9.getEnchantments().getLevel(sharpness))
            problems.add("una nostra spada in uscita viene modificata");
        finish(helper, "incudine: libro estratto da un nostro oggetto", problems, report);
    }

    private static ItemStack bedrockAnvil(ServerPlayer player, ItemStack left, ItemStack right) {
        BedrockAnvilScreenHandler menu = new BedrockAnvilScreenHandler(0, player.getInventory(), ContainerLevelAccess.NULL);
        menu.getSlot(0).set(left.copy());
        menu.getSlot(1).set(right.copy());
        menu.createResult();
        return menu.getSlot(2).getItem().copy();
    }

    /**
     * Bedrock Anvil, «mai più basso» (tutti gli oggetti): la spada T9 (Sharpness XII) con un libro Sharpness V resta XII
     * (prima scendeva a V); con un libro XII non sale. Spada vanilla: V + V = V (tetto del JSON, come prima), IV + IV =
     * V, III + V = V; una spada vanilla con Sharpness XII (comando) + V resta XII.
     */
    @GameTest(template = EMPTY)
    public static void bedrock_anvil_never_lowers(GameTestHelper helper) {
        List<String> problems = new ArrayList<>(), report = new ArrayList<>();
        var registry = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> sharpness = registry.getOrThrow(Enchantments.SHARPNESS);
        ServerPlayer player = mockPlayer(helper);
        ItemStack t9 = enchanted(helper, SAFunctionTests.item(SATier.ALLOY, "sword"));
        int own = t9.getEnchantments().getLevel(sharpness);
        if (own != 12) problems.add("la spada T9 ha Sharpness " + own + " invece di 12");
        record Case(String name, ItemStack left, int book, int want) {
        }
        List<Case> cases = new ArrayList<>(List.of(new Case("T9 XII + libro V", t9, 5, 12), new Case("T9 XII + libro XII", t9, 12, 12)));
        for (int[] c : new int[][]{{5, 5, 5}, {4, 4, 5}, {3, 5, 5}, {12, 5, 12}}) {
            ItemStack diamond = new ItemStack(Items.DIAMOND_SWORD);
            diamond.set(DataComponents.ENCHANTMENTS, new ItemEnchantments.Mutable(ItemEnchantments.EMPTY) {{ set(sharpness, c[0]); }}.toImmutable());
            cases.add(new Case("diamante " + c[0] + " + libro " + c[1], diamond, c[1], c[2]));
        }
        for (Case c : cases) {
            ItemStack out = bedrockAnvil(player, c.left(), book(sharpness, c.book()));
            int got = out.isEmpty() ? -1 : out.getEnchantments().getLevel(sharpness);
            report.add(c.name() + " → " + (out.isEmpty() ? "nessun risultato" : "Sharpness " + got));
            if (got != c.want()) problems.add(c.name() + ": " + got + " invece di " + c.want());
        }
        finish(helper, "Bedrock Anvil: mai più basso", problems, report);
    }

    /**
     * Overenchanting: l'override di spectrum:milestones/unlock_overenchanting_with_enchanter (gen_data) ha un criterio
     * in OR per ciascuno dei 42 incantesimi della tabella. Per ognuno, un libro al massimo vanilla non lo dà, uno al
     * massimo + 1 sì (es. Sharpness V no, VI sì). Le ricette spectrum:enchantment_upgrade non superano il tetto ATM10:
     * Thorns e Swift Sneak si fermano a V (un libro Thorns IV si alza, un Thorns V no); fuori tabella come Spectrum.
     */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void overenchanting_advancement(GameTestHelper helper) {
        List<String> problems = new ArrayList<>(), report = new ArrayList<>();
        var level = helper.getLevel();
        var registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var adv = level.getServer().getAdvancements().get(SpectrumAdvancements.OVERENCHANTING);
        helper.assertTrue(adv != null, "advancement mancante: " + SpectrumAdvancements.OVERENCHANTING);
        Set<String> expected = new HashSet<>();
        for (ResourceLocation id : SAEnchantCap.tableIds()) {
            expected.add("book_" + id.getPath() + "_above_vanilla");
            expected.add("item_" + id.getPath() + "_above_vanilla");
        }
        if (!adv.value().criteria().keySet().equals(expected))
            problems.add("criteri " + adv.value().criteria().keySet() + " invece di " + expected);
        if (adv.value().requirements().requirements().size() != 1) problems.add("i criteri non sono in OR: " + adv.value().requirements());

        for (ResourceLocation id : SAEnchantCap.tableIds()) {
            Holder<Enchantment> ench = registry.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT, id));
            int vanillaMax = ench.value().getMaxLevel(); // nel dev = max del JSON vanilla
            ServerPlayer player = mockPlayer(helper);
            ItemStack atMax = book(ench, vanillaMax);
            player.getInventory().add(atMax.copy());
            CriteriaTriggers.INVENTORY_CHANGED.trigger(player, player.getInventory(), atMax);
            if (player.getAdvancements().getOrStartProgress(adv).isDone()) problems.add(id + ": il libro al massimo vanilla " + vanillaMax + " dà già l'overenchanting");
            ItemStack above = book(ench, vanillaMax + 1);
            player.getInventory().add(above.copy());
            CriteriaTriggers.INVENTORY_CHANGED.trigger(player, player.getInventory(), above);
            if (!player.getAdvancements().getOrStartProgress(adv).isDone()) problems.add(id + ": il libro di livello " + (vanillaMax + 1) + " non dà l'overenchanting");
            // oggetto incantato (non nostro, un bastone qualsiasi): stesso confine
            ServerPlayer holder = mockPlayer(helper);
            for (int lvl : new int[]{vanillaMax, vanillaMax + 1}) {
                ItemStack stick = new ItemStack(Items.STICK);
                stick.enchant(ench, lvl);
                holder.getInventory().add(stick.copy());
                CriteriaTriggers.INVENTORY_CHANGED.trigger(holder, holder.getInventory(), stick);
                boolean done = holder.getAdvancements().getOrStartProgress(adv).isDone();
                if (done != (lvl > vanillaMax)) problems.add(id + ": oggetto di livello " + lvl + (done ? " dà" : " non dà") + " l'overenchanting");
            }
            if (id.getPath().equals("sharpness")) {
                boolean has = de.dafuqs.revelationary.api.advancements.AdvancementHelper.hasAdvancement(player, SpectrumAdvancements.OVERENCHANTING);
                report.add("libro Sharpness V: niente; Sharpness VI: overenchanting " + (has ? "sbloccato" : "NON sbloccato") + " (come lo legge l'Enchanter)");
                if (!has) problems.add("AdvancementHelper non vede l'overenchanting dopo Sharpness VI");
            }
        }

        // tetto delle ricette di upgrade
        int checked = 0;
        for (var holder : level.getRecipeManager().getAllRecipesFor(SpectrumRecipeTypes.ENCHANTMENT_UPGRADE)) {
            EnchantmentUpgradeRecipe recipe = holder.value();
            Holder<Enchantment> ench = recipe.getEnchantment();
            int raw = recipe.getLevelData().size() + 1;
            var cap = SAEnchantCap.tableCap(ench);
            int want = cap.isPresent() ? Math.min(raw, cap.getAsInt()) : raw;
            checked++;
            if (recipe.getLevelCap() != want)
                problems.add(ench.getRegisteredName() + ": tetto della ricetta " + recipe.getLevelCap() + " invece di " + want);
            if (want != raw) report.add(ench.getRegisteredName() + ": ricetta fino a " + raw + ", tagliata a " + want);
        }
        report.add(checked + " ricette di upgrade controllate");
        if (checked == 0) problems.add("nessuna ricetta spectrum:enchantment_upgrade trovata");

        Holder<Enchantment> thorns = registry.getOrThrow(Enchantments.THORNS);
        var thornsRecipe = level.getRecipeManager().getAllRecipesFor(SpectrumRecipeTypes.ENCHANTMENT_UPGRADE).stream()
                .map(h -> h.value()).filter(r -> r.getEnchantment().is(Enchantments.THORNS)).findFirst().orElse(null);
        if (thornsRecipe == null) {
            problems.add("ricetta di upgrade di Thorns mancante");
        } else {
            for (int from : new int[]{4, 5}) {
                List<ItemStack> input = new ArrayList<>();
                input.add(book(thorns, from));
                ItemStack gem = new ItemStack(SpectrumItems.KNOWLEDGE_GEM.get());
                ExperienceStorageItem.addStoredExperience(level.registryAccess(), gem, 1_000_000);
                input.add(gem);
                var data = thornsRecipe.getLevelData().get(Math.min(from, thornsRecipe.getLevelData().size()) - 1);
                for (int i = 0; i < 8; i++) input.add(new ItemStack(data.ingredient().getItems()[0].getItem(), data.countPerBowl()));
                boolean matches = thornsRecipe.matches(new RecipeInput() {
                    @Override
                    public ItemStack getItem(int index) {
                        return input.get(index);
                    }

                    @Override
                    public int size() {
                        return input.size();
                    }
                }, level);
                report.add("libro Thorns " + from + " all'Enchanter (ingredienti e XP pieni): " + (matches ? "si alza" : "non si alza"));
                if (matches != (from < 5)) problems.add("Thorns " + from + ": la ricetta " + (matches ? "si applica" : "non si applica"));
            }
        }
        finish(helper, "overenchanting: advancement e tetto delle ricette", problems, report);
    }

    /**
     * Overenchanting con un OGGETTO incantato oltre il massimo vanilla (decisione utente 2026-11-24, criteri item_* con
     * spectral_ascension:not_ours): conta qualunque oggetto non nostro (vanilla, Spectrum, altre mod), non i nostri pezzi
     * T6-T9; i libri come prima. Due strade vere del gioco, senza chiamare il trigger a mano: oggetto già in inventario
     * al login (ServerPlayer.initInventoryMenu, come PlayerList.placeNewPlayer) e oggetto preso dopo (broadcastChanges
     * del menu dell'inventario, come il tick del giocatore).
     */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void overenchanting_items(GameTestHelper helper) {
        List<String> problems = new ArrayList<>(), report = new ArrayList<>();
        var level = helper.getLevel();
        var registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var adv = level.getServer().getAdvancements().get(SpectrumAdvancements.OVERENCHANTING);
        helper.assertTrue(adv != null, "advancement mancante: " + SpectrumAdvancements.OVERENCHANTING);
        Holder<Enchantment> sharpness = registry.getOrThrow(Enchantments.SHARPNESS);
        Holder<Enchantment> fortune = registry.getOrThrow(Enchantments.FORTUNE);
        Holder<Enchantment> sweeping = registry.getOrThrow(Enchantments.SWEEPING_EDGE);

        ItemStack swordVI = new ItemStack(Items.DIAMOND_SWORD);
        swordVI.enchant(sharpness, 6);
        ItemStack swordV = new ItemStack(Items.DIAMOND_SWORD);
        swordV.enchant(sharpness, 5);
        ItemStack workstaff = enchanted(helper, SAItems.TRICHROMATIC_WORKSTAFF.get());
        ItemStack glassCrest = enchanted(helper, SpectrumItems.GLASS_CREST_ULTRA_GREATSWORD.get());
        if (workstaff.getEnchantments().getLevel(fortune) != 6) problems.add("la workstaff T6 non ha Fortune VI: " + workstaff.getEnchantments());
        if (glassCrest.getEnchantments().getLevel(sweeping) != 4) problems.add("lo spadone glass crest non ha Sweeping Edge IV: " + glassCrest.getEnchantments());
        // lo spadone di malachite come esce dalla ricetta della Fusion Shrine (la glass crest ne copia i componenti)
        ItemStack malachite = level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("spectrum", "fusion_shrine/malachite/malachite_ultra_greatsword"))
                .map(h -> h.value().getResultItem(level.registryAccess()).copy()).orElse(ItemStack.EMPTY);
        if (malachite.getEnchantments().getLevel(sweeping) != 4) problems.add("lo spadone di malachite della ricetta non ha Sweeping Edge IV: " + malachite.getEnchantments());

        record Case(String name, ItemStack stack, boolean unlocks) {
        }
        for (Case c : List.of(new Case("spada di diamante Sharpness VI", swordVI, true),
                new Case("libro Sharpness VI", book(sharpness, 6), true),
                new Case("workstaff T6 (Fortune VI, nostra)", workstaff, false),
                new Case("spadone glass crest di Spectrum (Sweeping Edge IV)", glassCrest, true),
                new Case("spadone di malachite dalla ricetta (Sweeping Edge IV)", malachite, true),
                new Case("spada di diamante Sharpness V (massimo vanilla)", swordV, false))) {
            ServerPlayer atLogin = mockPlayer(helper);
            atLogin.getInventory().add(c.stack().copy());
            atLogin.initInventoryMenu();
            boolean login = atLogin.getAdvancements().getOrStartProgress(adv).isDone();

            ServerPlayer pickup = mockPlayer(helper);
            pickup.initInventoryMenu();
            boolean before = pickup.getAdvancements().getOrStartProgress(adv).isDone();
            pickup.getInventory().add(c.stack().copy());
            pickup.inventoryMenu.broadcastChanges();
            boolean picked = pickup.getAdvancements().getOrStartProgress(adv).isDone();

            report.add(c.name() + ": al login " + (login ? "sblocca" : "no") + ", preso dopo " + (picked ? "sblocca" : "no"));
            if (before) problems.add(c.name() + ": inventario vuoto ma advancement già ottenuto");
            if (login != c.unlocks()) problems.add(c.name() + " al login: " + (login ? "sblocca" : "non sblocca"));
            if (picked != c.unlocks()) problems.add(c.name() + " preso dopo: " + (picked ? "sblocca" : "non sblocca"));
        }
        finish(helper, "overenchanting: oggetti incantati oltre il massimo vanilla", problems, report);
    }

    static ItemStack anvil(ServerPlayer player, ItemStack left, ItemStack right) {
        return anvilMenu(player, left, right).getSlot(2).getItem().copy();
    }

    /** Incudine vanilla del server con il risultato già calcolato: getSlot(2) è l'uscita, getCost() il costo in livelli. */
    static AnvilMenu anvilMenu(ServerPlayer player, ItemStack left, ItemStack right) {
        AnvilMenu menu = new AnvilMenu(0, player.getInventory(), ContainerLevelAccess.NULL);
        menu.getSlot(0).set(left.copy());
        menu.getSlot(1).set(right.copy());
        menu.createResult();
        return menu;
    }

    /**
     * Morte in sopravvivenza con i 16 oggetti di un tier: cadono tutti a terra (niente sparisce), resistono alla lava e
     * al fuoco, e un altro giocatore li raccoglie. Respawn con keepInventory: l'armatura resta indossata e il set
     * ridà 40 di Dike.
     */
    @GameTest(template = EMPTY)
    public static void death_drop_respawn(GameTestHelper helper) {
        List<String> problems = new ArrayList<>(), report = new ArrayList<>();
        var level = helper.getLevel();
        int row = 0;
        for (SATier tier : List.of(SATier.T6, SATier.T7, SATier.T8, SATier.ALLOY)) {
            BlockPos base = SAFunctionTests.slot(helper, row++);
            List<Item> items = SAItems.equipment().stream().map(DeferredItem::get).filter(i -> id(i).startsWith(tier.idPrefix() + "_"))
                    .map(i -> (Item) i).toList();
            ServerPlayer player = mockPlayer(helper);
            aim(player, base, 0, 0);
            int slot = 9;
            for (Item item : items) {
                if (item instanceof SAArmorItem armor) equip(player, armor.getEquipmentSlot(), enchanted(helper, item));
                else player.getInventory().setItem(slot++, enchanted(helper, item));
            }
            level.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(false, level.getServer());
            player.die(player.damageSources().generic());
            var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(base).inflate(4));
            for (Item item : items) {
                var drop = drops.stream().filter(e -> e.getItem().is(item)).findFirst();
                if (drop.isEmpty()) {
                    problems.add(id(item) + ": alla morte non cade");
                    continue;
                }
                drop.get().hurt(level.damageSources().lava(), 100);
                drop.get().hurt(level.damageSources().inFire(), 100);
                if (drop.get().isRemoved()) problems.add(id(item) + ": a terra brucia nella lava");
            }
            ServerPlayer other = mockPlayer(helper);
            aim(other, base, 0, 0);
            int picked = 0;
            for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, new AABB(base).inflate(4))) {
                e.setNoPickUpDelay();
                e.playerTouch(other);
                if (e.isRemoved()) picked++;
            }
            if (picked < items.size()) problems.add(tier + ": raccolti " + picked + " oggetti su " + items.size());
            report.add(tier + ": " + items.size() + " oggetti caduti alla morte, immuni alla lava, " + picked + " raccolti");
            clearAround(helper, base, 5);

            // respawn con keepInventory
            ServerPlayer before = mockPlayer(helper);
            for (var a : SAItems.armor(tier)) equip(before, a.get().getEquipmentSlot(), new ItemStack(a.get()));
            level.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(true, level.getServer());
            ServerPlayer after = mockPlayer(helper);
            after.restoreFrom(before, false);
            level.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(false, level.getServer());
            for (var a : SAItems.armor(tier)) {
                if (!after.getItemBySlot(a.get().getEquipmentSlot()).is(a.get())) problems.add(tier + ": dopo il respawn manca " + id(a.get()));
            }
            // il primo tick del giocatore nuovo segnala i pezzi indossati come cambiati (SAArmorSet.onEquipmentChange)
            for (EquipmentSlot s : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
                ItemStack worn = after.getItemBySlot(s);
                net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent(after, s, ItemStack.EMPTY, worn));
            }
            float dike = after.getData(AzureDikeAttachmentType.ATTACHMENT_TYPE).getMaxCharges();
            if (dike != 40) problems.add(tier + ": dopo il respawn il Dike massimo è " + dike);
        }
        finish(helper, "morte, drop e respawn", problems, report);
    }
}
