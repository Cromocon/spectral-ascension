package cromocon.spectralascension.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import cromocon.spectralascension.SpectralAscension;
import cromocon.spectralascension.item.SATier;
import cromocon.spectralascension.item.SATools;
import cromocon.spectralascension.registry.SAItems;
import de.dafuqs.spectrum.api.item.Preenchanted;
import de.dafuqs.spectrum.api.recipe.GatedRecipe;
import de.dafuqs.spectrum.recipe.fusion_shrine.FusionShrineRecipe;
import de.dafuqs.spectrum.registries.SpectrumToolTiers;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.GrindstoneEvent;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cromocon.spectralascension.gametest.SATestUtil.*;

/**
 * Mazze (2026-11-25): la mazza di bedrock (nostra, Spectrum non ne ha una) e le mazze T6-T9. Oggetto, ricetta «come la
 * spada bedrock» e sblocco della mazza di bedrock; catena di craft vanilla → bedrock → T6 → T9 con gli incantesimi
 * conservati; Wind Burst ai livelli giusti (IV, V, VI, VII, XI) e più forte salendo di tier; smash attack (più danno
 * cadendo) e indistruttibilità.
 */
@GameTestHolder(SpectralAscension.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SAMaceTests {
    private static final String EMPTY = "empty";
    private static final String BEDROCK_UNLOCK = "spectrum:unlocks/equipment/bedrock_tools";

    private SAMaceTests() {
    }

    /** Mazza → Wind Burst atteso, dalla bedrock all'Alloy (tools/gen_data.py: BEDROCK_MACE, T6 → T9_ENCHANTMENTS). */
    private static Map<Item, Integer> windBurstLevels() {
        Map<Item, Integer> out = new LinkedHashMap<>();
        out.put(SAItems.BEDROCK_MACE.get(), 4);
        out.put(SAItems.TRICHROMATIC_MACE.get(), 5);
        out.put(SAItems.UMBRAL_MACE.get(), 6);
        out.put(SAItems.ARGENT_MACE.get(), 7);
        out.put(SAItems.PRISMATIC_MACE.get(), 11);
        return out;
    }

    private static Holder<Enchantment> ench(GameTestHelper helper, net.minecraft.resources.ResourceKey<Enchantment> key) {
        return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }

    private static FusionShrineRecipe fusion(GameTestHelper helper, String path) {
        var holder = helper.getLevel().getServer().getRecipeManager().byKey(ResourceLocation.parse(path));
        return holder.isPresent() && holder.get().value() instanceof FusionShrineRecipe r ? r : null;
    }

    private static JsonObject resourceJson(String path) throws IOException {
        var p = ModList.get().getModFileById(SpectralAscension.MOD_ID).getFile().findResource(path.split("/"));
        return JsonParser.parseString(Files.readString(p)).getAsJsonObject();
    }

    /**
     * Mazza di bedrock come gli attrezzi bedrock di Spectrum: indistruttibile, UNCOMMON, resistente al fuoco, fuori da
     * #minecraft:enchantable/durability (niente Unbreaking/Mending), nessuna riparazione, Wind Burst IV, danno 5 + bonus
     * della bedrock, velocità 0,6. Ricetta = quella della spada bedrock di Spectrum (stesso liquido, tempo, xp e
     * materiali, base minecraft:mace), stesso sblocco degli attrezzi bedrock di Spectrum (nessun advancement
     * nostro), usato anche dalla pagina del libro (checklist §A).
     */
    @GameTest(template = EMPTY)
    public static void bedrock_mace_item(GameTestHelper helper) throws IOException {
        List<String> problems = new ArrayList<>(), report = new ArrayList<>();
        Item mace = SAItems.BEDROCK_MACE.get();
        ItemStack stack = enchanted(helper, mace);
        if (!(mace instanceof SATools.Mace) || !(mace instanceof Preenchanted)) problems.add("non è una SATools.Mace preincantata");
        if (!stack.has(DataComponents.UNBREAKABLE)) problems.add("manca il componente unbreakable");
        if (!stack.has(DataComponents.FIRE_RESISTANT)) problems.add("non resiste al fuoco");
        if (stack.get(DataComponents.RARITY) != Rarity.UNCOMMON) problems.add("rarità " + stack.get(DataComponents.RARITY) + " invece di UNCOMMON");
        if (stack.getMaxStackSize() != 1) problems.add("si impila a " + stack.getMaxStackSize());
        if (stack.is(ItemTags.DURABILITY_ENCHANTABLE)) problems.add("è in #minecraft:enchantable/durability");
        for (var tag : List.of(ItemTags.MACE_ENCHANTABLE, ItemTags.WEAPON_ENCHANTABLE, ItemTags.FIRE_ASPECT_ENCHANTABLE, Tags.Items.TOOLS_MACE)) {
            if (!stack.is(tag)) problems.add("non è in #" + tag.location());
        }
        if (stack.supportsEnchantment(ench(helper, Enchantments.UNBREAKING))) problems.add("accetta Unbreaking");
        if (stack.supportsEnchantment(ench(helper, Enchantments.MENDING))) problems.add("accetta Mending");
        for (var key : List.of(Enchantments.WIND_BURST, Enchantments.DENSITY, Enchantments.BREACH, Enchantments.FIRE_ASPECT, Enchantments.SMITE)) {
            if (!stack.supportsEnchantment(ench(helper, key))) problems.add("non accetta " + key.location());
        }
        if (mace.isValidRepairItem(stack, new ItemStack(Items.BREEZE_ROD))) problems.add("si ripara con la breeze rod");
        if (stack.isDamageableItem()) problems.add("ha la durata");
        ServerPlayer player = mockPlayer(helper);
        stack.hurtAndBreak(1000, helper.getLevel(), player, i -> problems.add("si è rotta"));
        if (stack.getDamageValue() != 0) problems.add("danneggiata a " + stack.getDamageValue());
        if (!SAItems.defaultEnchantments(mace).equals(Map.of(Enchantments.WIND_BURST, 4)))
            problems.add("incantesimi preimpostati " + SAItems.defaultEnchantments(mace) + " invece di Wind Burst IV");
        if (stack.getEnchantmentValue() != SpectrumToolTiers.BEDROCK.getEnchantmentValue())
            problems.add("incantabilità " + stack.getEnchantmentValue() + " invece di quella della bedrock " + SpectrumToolTiers.BEDROCK.getEnchantmentValue());
        double[] dmg = {1}, spd = {4};
        stack.forEachModifier(EquipmentSlot.MAINHAND, (attr, mod) -> {
            if (attr.equals(Attributes.ATTACK_DAMAGE)) dmg[0] += mod.amount();
            if (attr.equals(Attributes.ATTACK_SPEED)) spd[0] += mod.amount();
        });
        double wantDmg = 1 + SATools.Mace.VANILLA_DAMAGE + SpectrumToolTiers.BEDROCK.getAttackDamageBonus();
        if (Math.abs(dmg[0] - wantDmg) > 0.01) problems.add("danno totale " + dmg[0] + " invece di " + wantDmg);
        if (Math.abs(spd[0] - 0.6) > 0.01) problems.add("velocità d'attacco " + spd[0] + " invece di 0,6");
        report.add(String.format(java.util.Locale.ROOT, "mazza di bedrock: danno %.1f, velocità %.2f, incantabilità %d", dmg[0], spd[0], stack.getEnchantmentValue()));

        // Ricetta: come spectrum:fusion_shrine/bedrock/sword, con la mazza vanilla come base.
        FusionShrineRecipe ours = fusion(helper, "spectral_ascension:fusion_shrine/bedrock/bedrock_mace");
        FusionShrineRecipe sword = fusion(helper, "spectrum:fusion_shrine/bedrock/sword");
        if (ours == null || sword == null) {
            problems.add("ricetta mancante: nostra " + (ours != null) + ", spada bedrock di Spectrum " + (sword != null));
        } else {
            var a = ours.getIngredientStacks();
            var b = sword.getIngredientStacks();
            if (a.isEmpty() || !a.getFirst().test(new ItemStack(Items.MACE))) problems.add("il primo ingrediente non è minecraft:mace");
            if (a.size() != b.size()) problems.add(a.size() + " ingredienti invece di " + b.size() + " (spada bedrock)");
            else for (int i = 1; i < a.size(); i++) {
                ItemStack want = b.get(i).getItems().findFirst().orElse(ItemStack.EMPTY);
                if (a.get(i).getCount() != b.get(i).getCount() || !a.get(i).test(want.copyWithCount(64)))
                    problems.add("ingrediente " + (i + 1) + ": " + a.get(i).getCount() + " invece di " + b.get(i).getCount() + " " + want.getItem());
            }
            var fa = BuiltInRegistries.FLUID.getKey(ours.getFluid().getStacks()[0].getFluid());
            var fb = BuiltInRegistries.FLUID.getKey(sword.getFluid().getStacks()[0].getFluid());
            if (!fa.equals(fb) || !fa.toString().equals("spectrum:midnight_solution")) problems.add("liquido " + fa + " invece di " + fb);
            if (ours.getCraftingTime() != 1200 || ours.getCraftingTime() != sword.getCraftingTime()) problems.add("tempo " + ours.getCraftingTime());
            if (Math.abs(ours.getExperience() - 16f) > 1e-4 || Math.abs(ours.getExperience() - sword.getExperience()) > 1e-4) problems.add("xp " + ours.getExperience());
            if (!ours.areYieldAndEfficiencyUpgradesDisabled()) problems.add("manca disable_yield_upgrades (checklist D2)");
            ItemStack out = ours.getResultItem(helper.getLevel().registryAccess());
            if (!out.is(mace) || out.getEnchantments().getLevel(ench(helper, Enchantments.WIND_BURST)) != 4 || out.getEnchantments().size() != 1)
                problems.add("la ricetta produce " + out + " " + out.getEnchantments());
            var required = ((GatedRecipe<?>) ours).getRequiredAdvancement();
            if (required.isEmpty() || !required.get().toString().equals(BEDROCK_UNLOCK)) problems.add("required_advancement " + required);
        }
        // Sblocco: quello di Spectrum, come la spada bedrock; il nostro advancement dedicato non deve più esistere.
        var advs = helper.getLevel().getServer().getAdvancements();
        if (advs.get(ResourceLocation.parse("spectral_ascension:unlocks/equipment/bedrock/bedrock_mace")) != null)
            problems.add("esiste ancora l'advancement dedicato unlocks/equipment/bedrock/bedrock_mace");
        if (advs.get(ResourceLocation.parse(BEDROCK_UNLOCK)) == null) problems.add("advancement mancante " + BEDROCK_UNLOCK);
        // Voce del libro: stessa condizione della ricetta, sotto gli attrezzi bedrock di Spectrum.
        JsonObject entry = resourceJson("data/spectrum/modonomicon/books/guidebook/entries/equipment/spectral_ascension_bedrock_mace.json");
        if (!entry.getAsJsonObject("condition").get("advancement_id").getAsString().equals(BEDROCK_UNLOCK)) problems.add("la voce del libro ha un'altra condizione");
        if (!entry.getAsJsonArray("parents").get(0).getAsJsonObject().get("entry").getAsString().equals("spectrum:equipment/tools_and_armor/bedrock_tools"))
            problems.add("la voce del libro non discende dagli attrezzi bedrock");
        boolean page = false;
        for (var e : entry.getAsJsonArray("pages")) {
            JsonObject p = e.getAsJsonObject();
            if (p.has("recipe_id") && p.get("recipe_id").getAsString().equals("spectral_ascension:fusion_shrine/bedrock/bedrock_mace")
                    && p.getAsJsonObject("condition").get("advancement_id").getAsString().equals(BEDROCK_UNLOCK)) page = true;
        }
        if (!page) problems.add("nessuna pagina del libro con la ricetta e lo stesso sblocco");
        finish(helper, "mazza di bedrock", problems, report);
    }

    /**
     * Craft veri alla Fusion Shrine (SACraftTests.fusionCraft): mazza vanilla con Density III, Fire Aspect II e Wind
     * Burst I → bedrock → T6 → T7 → T8 → T9. A ogni passo Wind Burst è quello del tier e Density e Fire Aspect restano;
     * il risultato è sempre indistruttibile. Poi una mazza di bedrock con Wind Burst VII dà una mazza T6 con VII (vince il
     * livello più alto, GatedSpectrumRecipeMixin), non V.
     */
    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void mace_chain_craft(GameTestHelper helper) {
        List<String> problems = new ArrayList<>(), report = new ArrayList<>();
        Holder<Enchantment> wind = ench(helper, Enchantments.WIND_BURST), density = ench(helper, Enchantments.DENSITY),
                fire = ench(helper, Enchantments.FIRE_ASPECT);
        ItemStack previous = new ItemStack(Items.MACE);
        previous.set(DataComponents.ENCHANTMENTS, new ItemEnchantments.Mutable(ItemEnchantments.EMPTY) {{
            set(density, 3);
            set(fire, 2);
            set(wind, 1);
        }}.toImmutable());
        List<String> recipes = List.of("bedrock/bedrock_mace", "t6/trichromatic_mace", "t7/umbral_mace", "t8/argent_mace", "t9/prismatic_mace");
        List<Integer> levels = List.copyOf(windBurstLevels().values());
        List<String> line = new ArrayList<>(List.of("mace " + previous.getEnchantments()));
        int row = 0;
        for (int i = 0; i < recipes.size(); i++) {
            String id = recipes.get(i);
            FusionShrineRecipe recipe = fusion(helper, "spectral_ascension:fusion_shrine/" + id);
            if (recipe == null) {
                problems.add(id + ": ricetta mancante");
                break;
            }
            BlockPos pos = SAFunctionTests.slot(helper, row++);
            ItemStack result = SACraftTests.fusionCraft(helper, recipe, previous, pos, problems, id);
            if (result.isEmpty()) break;
            Item want = recipe.getResultItem(helper.getLevel().registryAccess()).getItem();
            if (!result.is(want)) problems.add(id + ": il craft dà " + result);
            if (!result.has(DataComponents.UNBREAKABLE)) problems.add(id + ": dopo il craft non è indistruttibile");
            var e = result.getEnchantments();
            if (e.getLevel(wind) != levels.get(i)) problems.add(id + ": Wind Burst " + e.getLevel(wind) + " invece di " + levels.get(i));
            if (e.getLevel(density) != 3) problems.add(id + ": Density " + e.getLevel(density) + " invece di 3 (persa nella fusione)");
            if (e.getLevel(fire) != 2) problems.add(id + ": Fire Aspect " + e.getLevel(fire) + " invece di 2 (perso nella fusione)");
            line.add(BuiltInRegistries.ITEM.getKey(result.getItem()).getPath() + " " + e);
            previous = result;
        }
        report.add(String.join(" → ", line));
        // Livello più alto sulla base: vince sul result della ricetta.
        FusionShrineRecipe t6 = fusion(helper, "spectral_ascension:fusion_shrine/t6/trichromatic_mace");
        if (t6 != null) {
            ItemStack strong = enchanted(helper, SAItems.BEDROCK_MACE.get());
            strong.enchant(wind, 7);
            ItemStack out = SACraftTests.fusionCraft(helper, t6, strong, SAFunctionTests.slot(helper, row), problems, "trichromatic_mace (Wind Burst VII)");
            if (!out.isEmpty() && out.getEnchantments().getLevel(wind) != 7)
                problems.add("mazza di bedrock con Wind Burst VII → T6 con Wind Burst " + out.getEnchantments().getLevel(wind) + " invece di VII");
            report.add("mazza di bedrock Wind Burst VII → T6 Wind Burst " + out.getEnchantments().getLevel(wind));
        }
        finish(helper, "catena della mazza", problems, report);
    }

    /** Spawn di uno zombie fermo con 1000 di vita, per misurare il danno. */
    private static Zombie dummy(GameTestHelper helper, Vec3 pos) {
        Zombie z = EntityType.ZOMBIE.create(helper.getLevel());
        z.moveTo(pos.x, pos.y, pos.z, 0, 0);
        z.setNoAi(true);
        z.setPersistenceRequired();
        z.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
        z.setHealth(1000);
        helper.getLevel().addFreshEntity(z);
        return z;
    }

    /** Giocatore finto con la mazza in mano (attributi applicati subito, barra d'attacco carica). */
    private static ServerPlayer wielder(GameTestHelper helper, BlockPos feet, ItemStack mace) {
        ServerPlayer player = mockPlayer(helper);
        aim(player, feet, 0, 0);
        player.setItemInHand(InteractionHand.MAIN_HAND, mace);
        player.getMainHandItem().forEachModifier(EquipmentSlot.MAINHAND,
                (attr, mod) -> player.getAttribute(attr).addOrUpdateTransientModifier(mod));
        chargeAttack(player);
        return player;
    }

    /**
     * Colpo cadendo (caduta 5 blocchi) a uno zombie fermo: velocità orizzontale dello zombie subito dopo. La folata di
     * Wind Burst (esplosione di vento centrata su chi colpisce) lo spinge via, tanto più forte quanto più alto il livello.
     * Si misura sullo zombie perché il giocatore finto dei test non è nel mondo e l'esplosione non lo vede.
     */
    private static double pushOnTarget(GameTestHelper helper, BlockPos base, ItemStack mace) {
        ServerPlayer player = wielder(helper, base.above(2), mace);
        player.setOnGround(false);
        player.fallDistance = 5.0F;
        player.setDeltaMovement(0, -1.0, 0);
        Zombie target = dummy(helper, Vec3.atBottomCenterOf(base.offset(0, 0, 1)));
        player.attack(target);
        Vec3 v = target.getDeltaMovement();
        return Math.sqrt(v.x * v.x + v.z * v.z);
    }

    /**
     * Wind Burst ai livelli giusti: preimpostato e nel risultato della ricetta (IV, V, VI, VII, XI); la mazza lo accetta.
     * Poi in gioco: un colpo cadendo fa scattare la folata, che spinge via il bersaglio molto più di una mazza senza
     * incantesimi, e sempre più forte salendo di tier.
     */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void mace_wind_burst(GameTestHelper helper) {
        List<String> problems = new ArrayList<>(), report = new ArrayList<>();
        Holder<Enchantment> wind = ench(helper, Enchantments.WIND_BURST);
        var access = helper.getLevel().registryAccess();
        Map<String, String> recipes = Map.of("bedrock_mace", "bedrock", "trichromatic_mace", "t6", "umbral_mace", "t7",
                "argent_mace", "t8", "prismatic_mace", "t9");
        int row = 0;
        BlockPos first = SAFunctionTests.slot(helper, row++);
        // Senza incantesimi: la mazza vanilla. Le nostre hanno sempre il Wind Burst intrinseco (SATools.Mace.ensureWindBurst,
        // rimesso anche quando la mazza entra in mano), quindi non possono fare da riferimento.
        double baseline = pushOnTarget(helper, first, new ItemStack(Items.MACE));
        clearAround(helper, first, 5);
        report.add(String.format(java.util.Locale.ROOT, "mazza vanilla senza incantesimi: spinta sul bersaglio %.3f", baseline));
        double last = baseline;
        for (var e : windBurstLevels().entrySet()) {
            Item mace = e.getKey();
            String id = id(mace);
            int want = e.getValue();
            if (!SAItems.defaultEnchantments(mace).equals(Map.of(Enchantments.WIND_BURST, want)))
                problems.add(id + ": preimpostati " + SAItems.defaultEnchantments(mace) + " invece di Wind Burst " + want);
            ItemStack stack = enchanted(helper, mace);
            if (stack.getEnchantments().getLevel(wind) != want) problems.add(id + ": lo stack preincantato ha Wind Burst " + stack.getEnchantments().getLevel(wind));
            if (!stack.supportsEnchantment(wind)) problems.add(id + ": non accetta Wind Burst");
            FusionShrineRecipe recipe = fusion(helper, "spectral_ascension:fusion_shrine/" + recipes.get(id) + "/" + id);
            int inRecipe = recipe == null ? -1 : recipe.getResultItem(access).getEnchantments().getLevel(wind);
            if (inRecipe != want) problems.add(id + ": la ricetta dà Wind Burst " + inRecipe + " invece di " + want);

            BlockPos base = SAFunctionTests.slot(helper, row++);
            double push = pushOnTarget(helper, base, stack);
            report.add(String.format(java.util.Locale.ROOT, "%s: Wind Burst %d, spinta sul bersaglio %.3f", id, want, push));
            if (!(push > baseline + 0.3)) problems.add(id + ": spinta " + push + " contro " + baseline + " senza incantesimi (Wind Burst non è scattato)");
            else if (!(push > last)) problems.add(id + ": spinta " + push + " non maggiore del livello precedente " + last);
            last = Math.max(last, push);
            clearAround(helper, base, 5);
        }
        finish(helper, "Wind Burst delle mazze", problems, report);
    }

    /**
     * Smash attack: la stessa mazza fa più danno colpendo mentre cade (5 blocchi: +16 nella vanilla, più il critico) che
     * da terra; dopo il colpo la caduta è azzerata (niente danno da caduta) e la mazza non si è consumata.
     */
    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void mace_smash_attack(GameTestHelper helper) {
        List<String> problems = new ArrayList<>(), report = new ArrayList<>();
        int row = 0;
        for (Item mace : windBurstLevels().keySet()) {
            String id = id(mace);
            BlockPos base = SAFunctionTests.slot(helper, row++);
            // Da terra
            ServerPlayer ground = wielder(helper, base, enchanted(helper, mace));
            ground.setOnGround(true);
            ground.fallDistance = 0;
            Zombie a = dummy(helper, Vec3.atBottomCenterOf(base.offset(0, 0, 1)));
            ground.attack(a);
            float onGround = 1000 - a.getHealth();
            // Cadendo da 5 blocchi
            ServerPlayer falling = wielder(helper, base.above(2), enchanted(helper, mace));
            falling.setOnGround(false);
            falling.fallDistance = 5.0F;
            falling.setDeltaMovement(0, -1.0, 0);
            Zombie b = dummy(helper, Vec3.atBottomCenterOf(base.offset(1, 0, 1)));
            falling.attack(b);
            float smash = 1000 - b.getHealth();
            report.add(String.format(java.util.Locale.ROOT, "%s: da terra -%.1f, cadendo da 5 blocchi -%.1f", id, onGround, smash));
            if (!(onGround > 0)) problems.add(id + ": da terra nessun danno");
            if (!(smash >= onGround + 10)) problems.add(id + ": cadendo -" + smash + ", da terra -" + onGround + " (atteso almeno +10 dalla smash attack)");
            if (falling.fallDistance != 0) problems.add(id + ": dopo la smash attack la caduta è " + falling.fallDistance + " invece di 0");
            if (falling.getMainHandItem().getDamageValue() != 0 || !falling.getMainHandItem().is(mace))
                problems.add(id + ": dopo i colpi la mazza è " + falling.getMainHandItem() + " danno " + falling.getMainHandItem().getDamageValue());
            clearAround(helper, base, 5);
        }
        finish(helper, "smash attack delle mazze", problems, report);
    }

    /** Le mazze dei tier: stessa classe, indistruttibili, fuori da enchantable/durability, nessuna riparazione. */
    @GameTest(template = EMPTY)
    public static void tier_maces_unbreakable(GameTestHelper helper) {
        List<String> problems = new ArrayList<>(), report = new ArrayList<>();
        ServerPlayer player = mockPlayer(helper);
        for (SATier tier : List.of(SATier.T6, SATier.T7, SATier.T8, SATier.ALLOY)) {
            Item mace = SAFunctionTests.item(tier, "mace");
            ItemStack stack = enchanted(helper, mace);
            if (!(mace instanceof SATools.Mace)) problems.add(id(mace) + ": non è una SATools.Mace");
            if (stack.get(DataComponents.RARITY) != Rarity.EPIC) problems.add(id(mace) + ": rarità " + stack.get(DataComponents.RARITY));
            if (stack.is(ItemTags.DURABILITY_ENCHANTABLE)) problems.add(id(mace) + ": è in #minecraft:enchantable/durability");
            if (mace.isValidRepairItem(stack, new ItemStack(Items.BREEZE_ROD))) problems.add(id(mace) + ": si ripara con la breeze rod");
            stack.hurtAndBreak(1000, helper.getLevel(), player, i -> problems.add(id(mace) + ": si è rotta"));
            if (stack.getDamageValue() != 0) problems.add(id(mace) + ": danneggiata a " + stack.getDamageValue());
            if (stack.getEnchantmentValue() != tier.getEnchantmentValue()) problems.add(id(mace) + ": incantabilità " + stack.getEnchantmentValue());
            report.add(id(mace) + ": indistruttibile, incantabilità " + stack.getEnchantmentValue());
        }
        finish(helper, "mazze dei tier indistruttibili", problems, report);
    }

    /**
     * Wind Burst sempre presente (2026-11-27, SATools.Mace.ensureWindBurst): uno stack da /give (ItemInput, lo stesso
     * percorso del comando), da NBT (caricamento) o da {@code new ItemStack} ha il Wind Burst del tier, una sola voce,
     * visibile nel tooltip. Preincantato + copia: nessun doppione. Un livello più alto resta; uno più basso sale al
     * livello intrinseco; /give con altri incantesimi li tiene e aggiunge Wind Burst. La mazza vanilla resta senza.
     * Mola: il risultato tiene Wind Burst intrinseco (toglie il resto) e non dà XP per quel livello. Incudine: libro
     * Wind Burst VIII su una mazza di bedrock → mai sotto IV.
     */
    @GameTest(template = EMPTY)
    public static void mace_wind_burst_always(GameTestHelper helper) throws Exception {
        List<String> problems = new ArrayList<>(), report = new ArrayList<>();
        var access = helper.getLevel().registryAccess();
        Holder<Enchantment> wind = ench(helper, Enchantments.WIND_BURST), density = ench(helper, Enchantments.DENSITY);
        ServerPlayer player = mockPlayer(helper);
        player.experienceLevel = 1000;
        for (var e : windBurstLevels().entrySet()) {
            Item mace = e.getKey();
            String id = id(mace);
            int want = e.getValue();
            ItemStack given = new ItemInput(mace.builtInRegistryHolder(), DataComponentPatch.EMPTY).createItemStack(1, false);
            check(problems, id + " /give", given, wind, want);
            ItemStack loaded = ItemStack.parseOptional(access, (net.minecraft.nbt.CompoundTag) net.minecraft.nbt.TagParser.parseTag(
                    "{id:\"" + BuiltInRegistries.ITEM.getKey(mace) + "\",count:1}"));
            check(problems, id + " da NBT", loaded, wind, want);
            check(problems, id + " new ItemStack", new ItemStack(mace), wind, want);
            check(problems, id + " preincantata + copia", enchanted(helper, mace).copy(), wind, want);
            // Tooltip: la riga dell'incantesimo c'è.
            String line = Enchantment.getFullname(wind, want).getString();
            boolean shown = given.getTooltipLines(Item.TooltipContext.of(helper.getLevel()), null, TooltipFlag.NORMAL).stream()
                    .anyMatch(c -> c.getString().equals(line));
            if (!shown) problems.add(id + ": il tooltip non mostra «" + line + "»");
            // /give con un altro incantesimo: resta, e Wind Burst si aggiunge.
            ItemStack withDensity = new ItemInput(mace.builtInRegistryHolder(), DataComponentPatch.builder()
                    .set(DataComponents.ENCHANTMENTS, new ItemEnchantments.Mutable(ItemEnchantments.EMPTY) {{ set(density, 3); }}.toImmutable())
                    .build()).createItemStack(1, false);
            if (withDensity.getEnchantments().getLevel(density) != 3 || withDensity.getEnchantments().getLevel(wind) != want)
                problems.add(id + ": /give con Density III dà " + withDensity.getEnchantments());
            // Livello più alto: resta. Più basso: sale.
            ItemStack high = new ItemStack(mace);
            high.enchant(wind, want + 1);
            if (high.copy().getEnchantments().getLevel(wind) != want + 1) problems.add(id + ": Wind Burst " + (want + 1) + " abbassato dalla copia");
            ItemStack low = new ItemStack(mace);
            low.set(DataComponents.ENCHANTMENTS, new ItemEnchantments.Mutable(ItemEnchantments.EMPTY) {{ set(wind, 1); }}.toImmutable());
            if (low.copy().getEnchantments().getLevel(wind) != want) problems.add(id + ": Wind Burst I non risale a " + want + " nella copia");
            // Mola: via Density, Wind Burst resta; XP solo per Density.
            ItemStack toGrind = new ItemStack(mace);
            toGrind.enchant(density, 3);
            var grind = new GrindstoneMenu(0, player.getInventory(), ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(BlockPos.ZERO)));
            grind.getSlot(0).set(toGrind.copy());
            ItemStack ground = grind.getSlot(2).getItem();
            if (!ground.is(mace) || ground.getEnchantments().getLevel(wind) != want || ground.getEnchantments().getLevel(density) != 0)
                problems.add(id + ": la mola dà " + ground + " " + ground.getEnchantments());
            int xpBoth = grindXp(player, toGrind), xpWindOnly = grindXp(player, new ItemStack(mace));
            int densityCost = density.value().getMinCost(3);
            if (xpWindOnly != 0) problems.add(id + ": la mola dà " + xpWindOnly + " XP per il solo Wind Burst intrinseco");
            if (xpBoth < Math.ceil(densityCost / 2.0) || xpBoth > densityCost) problems.add(id + ": XP della mola " + xpBoth + " fuori da Density III (" + densityCost + ")");
            report.add(id + ": /give, NBT e copia con Wind Burst " + want + "; mola → " + ground.getEnchantments().size()
                    + " incantesimi, XP " + xpBoth + " (solo Wind Burst: " + xpWindOnly + ")");
        }
        // Incudine: libro Wind Burst VIII sulla mazza di bedrock (IV). Senza Apothic l'incudine taglia a III e
        // AnvilMenuMixin rimette IV; con Apothic (tetto VIII) sale a VIII. Mai sotto IV.
        ItemStack book = SAEdgeCaseTests.book(wind, 8);
        ItemStack anv = SAEdgeCaseTests.anvil(player, new ItemStack(SAItems.BEDROCK_MACE.get()), book);
        if (anv.getEnchantments().getLevel(wind) < 4) problems.add("incudine: mazza di bedrock + libro VIII dà Wind Burst " + anv.getEnchantments().getLevel(wind));
        report.add("incudine: mazza di bedrock + libro Wind Burst VIII → " + anv.getEnchantments().getLevel(wind));
        // La mazza vanilla resta com'è.
        if (new ItemStack(Items.MACE).getEnchantments().getLevel(wind) != 0) problems.add("la mazza vanilla ha Wind Burst");
        finish(helper, "Wind Burst sempre presente", problems, report);
    }

    private static void check(List<String> problems, String what, ItemStack stack, Holder<Enchantment> wind, int want) {
        var e = stack.getEnchantments();
        if (e.getLevel(wind) != want || e.size() != 1) problems.add(what + ": " + e + " invece del solo Wind Burst " + want);
    }

    /** XP della mola come la consegnerebbe a chi prende il risultato (evento NeoForge, listener compresi). */
    private static int grindXp(ServerPlayer player, ItemStack top) {
        var event = new GrindstoneEvent.OnTakeItem(ContainerLevelAccess.NULL, player, top, ItemStack.EMPTY, 999);
        NeoForge.EVENT_BUS.post(event);
        return event.getXp();
    }
}
