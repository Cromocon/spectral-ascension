package cromocon.spectralascension.registry;

import cromocon.spectralascension.SpectralAscension;
import cromocon.spectralascension.item.BidentMode;
import cromocon.spectralascension.item.SAArmorItem;
import cromocon.spectralascension.item.SABident;
import cromocon.spectralascension.item.SATier;
import cromocon.spectralascension.item.SATools;
import cromocon.spectralascension.item.WorkstaffDrops;
import de.dafuqs.spectrum.registries.SpectrumEnchantmentKeys;
import de.dafuqs.spectrum.registries.SpectrumToolTiers;
import de.dafuqs.fractal.api.CreativeSubTabEvent;
import de.dafuqs.spectrum.api.item.Preenchanted;
import de.dafuqs.spectrum.api.item_group.ItemGroupIDs;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Tutti gli item della mod. Un {@link DeferredRegister} raccoglie le "promesse" di item:
 * NeoForge li crea al momento giusto dell'avvio.
 * <p>
 * Incantesimi preimpostati: lo stesso incantesimo caratteristico della versione bedrock di Spectrum,
 * al livello della regola del tetto ATM10 ({@link cromocon.spectralascension.item.SAEnchantCap}, 2026-11-27):
 * T6 tetto - 2, T7 tetto - 1, T8 tetto, T9 tetto + 2. Gli incantesimi secondari (Quick Charge, Sniping, Lure,
 * Foundry, Inventory Insertion) hanno livelli propri.
 * DEVONO coincidere con quelli delle ricette (tools/gen_data.py): lo verificano i GameTest.
 */
public final class SAItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SpectralAscension.MOD_ID);

    /** Ordine in cui compaiono nella sotto-tab Equipment di Spectrum. */
    private static final List<DeferredItem<? extends Item>> EQUIPMENT_TAB = new ArrayList<>();

    /** I pezzi d'armatura per tier, nell'ordine di registrazione (elmo, corazza, gambali, stivali): li legge {@link #armor(SATier)}. */
    private static final Map<SATier, List<DeferredItem<SAArmorItem>>> ARMOR = new EnumMap<>(SATier.class);

    // ---------- Mazze (2026-11-25) ----------
    // Danno e velocità della mazza con lo stesso criterio di spade e spadoni rispetto al loro oggetto bedrock/glass crest:
    // il danno base proprio dell'oggetto (vanilla 5, SATools.Mace.VANILLA_DAMAGE) più il bonus danno del materiale, e la
    // velocità del tipo d'oggetto che non cambia fra i tier (spada -2,4, spadone -2,8, mazza vanilla -3,4 = 0,6).
    // Danno TOTALE (con il +1 della mano), regola +25% per tier di SATier:
    //   bedrock  5 + 5 (bonus della bedrock di Spectrum, config bedrock_attack_damage) + 1 = 11  (spada bedrock 4+5+1 = 10)
    //   T6       5 + 14 + 1 = 20                                                             (spadone T6 20, spada 19)
    //   T7       5 + 19 + 1 = 25  (20 × 1,25)
    //   T8       5 + 25 + 1 = 31  (20 × 1,5625 = 31,25)
    //   T9       6 + 32 + 1 = 39  (20 × 1,953 = 39,06: base 6 come lo spadone Alloy)
    // Velocità 0,6 in tutti i tier, come la vanilla. Smash attack e Wind Burst: vedi SATools.Mace.

    /**
     * Mazza di bedrock: Spectrum non ne ha una. Come i suoi attrezzi bedrock (SpectrumItems.BEDROCK_SWORD): UNCOMMON,
     * resistente al fuoco, indistruttibile, incantabilità e bonus danno della bedrock di Spectrum (SpectrumToolTiers.BEDROCK).
     * Wind Burst IV. Ricetta come la spada bedrock (tools/gen_data.py, BEDROCK_MACE). Non è nella lista {@link #equipment()}
     * (gli oggetti dei tier): entra nel creativo da {@link #addToSpectrumSubTabs}, prima di loro.
     */
    public static final DeferredItem<SATools.Mace> BEDROCK_MACE = ITEMS.register("bedrock_mace",
            () -> new SATools.Mace(SpectrumToolTiers.BEDROCK.getEnchantmentValue(),
                    mace(new Item.Properties()
                                    .stacksTo(1)
                                    .rarity(Rarity.UNCOMMON)
                                    .fireResistant()
                                    .durability(SpectrumToolTiers.BEDROCK.getUses())
                                    .component(DataComponents.UNBREAKABLE, new Unbreakable(false)),
                            SATools.Mace.VANILLA_DAMAGE + SpectrumToolTiers.BEDROCK.getAttackDamageBonus()),
                    Map.of(Enchantments.WIND_BURST, 4)));

    // ---------- T6 · Tricromatico (Trichromatic) ----------
    // Incantesimi del T6 decisi dall'utente il 2026-11-18 insieme alle ricette alla Fusion Shrine con Dragonrot
    // (tools/gen_data.py); livelli dal 2026-11-27: tetto ATM10 - 2.

    /**
     * Workstaff = il piccone del tier (upgrade alla Fusion Shrine della workstaff glass crest). Danno e velocità
     * d'attacco come la glass crest in SpectrumItems (1, -2,8F), sommati al bonus del nostro tier.
     * Fortune VI (selezione di default del menu) ed Efficiency VII.
     */
    public static final DeferredItem<SATools.Workstaff> TRICHROMATIC_WORKSTAFF = tab(ITEMS.register("trichromatic_workstaff",
            () -> new SATools.Workstaff(SATier.T6, 1, -2.8F, tool(SATier.T6),
                    Map.of(Enchantments.FORTUNE, 6, Enchantments.EFFICIENCY, 7))));

    public static final DeferredItem<SATools.Axe> TRICHROMATIC_AXE = tab(ITEMS.register("trichromatic_axe",
            () -> new SATools.Axe(SATier.T6, tool(SATier.T6).attributes(AxeItem.createAttributes(SATier.T6, 5.0F, -3.0F)),
                    Map.of(Enchantments.EFFICIENCY, 7))));

    public static final DeferredItem<SATools.Shovel> TRICHROMATIC_SHOVEL = tab(ITEMS.register("trichromatic_shovel",
            () -> new SATools.Shovel(SATier.T6, tool(SATier.T6).attributes(ShovelItem.createAttributes(SATier.T6, 1.0F, -3.0F)),
                    Map.of(Enchantments.EFFICIENCY, 7))));

    public static final DeferredItem<SATools.Hoe> TRICHROMATIC_HOE = tab(ITEMS.register("trichromatic_hoe",
            () -> new SATools.Hoe(SATier.T6, tool(SATier.T6).attributes(HoeItem.createAttributes(SATier.T6, 2.0F, 0.0F)),
                    Map.of(Enchantments.FORTUNE, 6))));

    public static final DeferredItem<SATools.Sword> TRICHROMATIC_SWORD = tab(ITEMS.register("trichromatic_sword",
            () -> new SATools.Sword(SATier.T6, tool(SATier.T6).attributes(SwordItem.createAttributes(SATier.T6, 4, -2.4F)),
                    Map.of(Enchantments.SHARPNESS, 7))));

    public static final DeferredItem<SATools.Bow> TRICHROMATIC_BOW = tab(ITEMS.register("trichromatic_bow",
            () -> new SATools.Bow(SATier.T6, tool(SATier.T6), Map.of(Enchantments.POWER, 7))));

    public static final DeferredItem<SATools.FishingRod> TRICHROMATIC_FISHING_ROD = tab(ITEMS.register("trichromatic_fishing_rod",
            () -> new SATools.FishingRod(SATier.T6, tool(SATier.T6), Map.of(Enchantments.LUCK_OF_THE_SEA, 6, Enchantments.LURE, 3))));

    public static final DeferredItem<SATools.Shears> TRICHROMATIC_SHEARS = tab(ITEMS.register("trichromatic_shears",
            () -> new SATools.Shears(SATier.T6, tool(SATier.T6).component(DataComponents.TOOL, ShearsItem.createToolProperties()),
                    Map.of(Enchantments.EFFICIENCY, 7))));

    /** Mazza T6: upgrade della mazza di bedrock. Danno totale 20, velocità 0,6 (vedi «Mazze»), Wind Burst VI. */
    public static final DeferredItem<SATools.Mace> TRICHROMATIC_MACE = tab(ITEMS.register("trichromatic_mace",
            () -> new SATools.Mace(SATier.T6.getEnchantmentValue(),
                    mace(tool(SATier.T6), SATools.Mace.VANILLA_DAMAGE + SATier.T6.getAttackDamageBonus()),
                    Map.of(Enchantments.WIND_BURST, 6))));

    // Oggetti speciali: upgrade alla Fusion Shrine delle versioni glass crest, con il loro incantesimo caratteristico
    // (quello della versione malachite, che le glass crest ereditano) al livello del T6 (tetto ATM10 - 2).

    /**
     * Ultra greatsword: Sweeping Edge VI (Spectrum IV), che è anche la forza del ground slam. Danno extra e velocità
     * come la glass crest (5, -2,8F) sommati al bonus del tier; portata extra 1,25 (glass crest 1,0, +0,25 per tier).
     */
    public static final DeferredItem<SATools.UltraGreatsword> TRICHROMATIC_ULTRA_GREATSWORD = tab(ITEMS.register("trichromatic_ultra_greatsword",
            () -> new SATools.UltraGreatsword(SATier.T6, 5, -2.8F, 1.25F, tool(SATier.T6),
                    Map.of(Enchantments.SWEEPING_EDGE, 6))));

    /**
     * Balestra: Quick Charge III e Piercing VI (Spectrum: Piercing V). Velocità 2,0, tempo di carica 1,6 e divergenza 0,63 PROVVISORI
     * (glass crest 1,5 / 2,0 / 0,75), vedi SATools.Crossbow.
     */
    public static final DeferredItem<SATools.Crossbow> TRICHROMATIC_CROSSBOW = tab(ITEMS.register("trichromatic_crossbow",
            () -> new SATools.Crossbow(SATier.T6, 2.0F, 1.6F, 0.63F, tool(SATier.T6), Map.of(Enchantments.QUICK_CHARGE, 3, Enchantments.PIERCING, 6))));

    /**
     * Bident: fonde Ferocious e Fractal (modalità in SABident). Impaling VIII (i due glass crest di Spectrum: VI).
     * Valori PROVVISORI sopra il Ferocious (-2,2 / 13 / 33% / 33%): velocità -2,2, danno 15, perforazione armatura e
     * protezione 36%. Parte in modalità Ferocious, come i glass crest che escono già attivi.
     */
    public static final DeferredItem<SABident> TRICHROMATIC_BIDENT = tab(ITEMS.register("trichromatic_bident",
            () -> new SABident(SATier.T6, tool(SATier.T6).component(SAComponents.BIDENT_MODE.get(), BidentMode.FEROCIOUS),
                    -2.2, 15, 0.36F, 0.36F, Map.of(Enchantments.IMPALING, 8))));

    // Armatura: incantesimo caratteristico della bedrock per pezzo (livello 5 in Spectrum, vedi armorPiece) al livello T6 (tetto ATM10 - 2).
    // Il Dike del set completo è in SAArmorSet; statistiche in SAArmorMaterials.

    public static final DeferredItem<SAArmorItem> TRICHROMATIC_HELMET = armorPiece(SATier.T6, SAArmorMaterials.TRICHROMATIC, ArmorItem.Type.HELMET, 9);
    public static final DeferredItem<SAArmorItem> TRICHROMATIC_CHESTPLATE = armorPiece(SATier.T6, SAArmorMaterials.TRICHROMATIC, ArmorItem.Type.CHESTPLATE, 6);
    public static final DeferredItem<SAArmorItem> TRICHROMATIC_LEGGINGS = armorPiece(SATier.T6, SAArmorMaterials.TRICHROMATIC, ArmorItem.Type.LEGGINGS, 7);
    public static final DeferredItem<SAArmorItem> TRICHROMATIC_BOOTS = armorPiece(SATier.T6, SAArmorMaterials.TRICHROMATIC, ArmorItem.Type.BOOTS, 7);

    // ---------- T7 · Umbrale (Umbral) ----------
    // Un tier sopra il T6: ogni pezzo si fa dal pezzo T6 (tools/gen_data.py). Gli attrezzi hanno gli stessi parametri
    // del T6 (presi dalla versione bedrock o glass crest di Spectrum): la crescita viene da SATier.T7 (velocità di
    // scavo 17 invece di 16, bonus danno 19 invece di 14: +25% rispetto al T6, vedi SATier; incantabilità 105 invece di 90). Gli oggetti speciali hanno
    // anche valori propri, scalati come il T6 li scala rispetto alla glass crest. Valori PROVVISORI.
    // Incantesimi: tetto ATM10 - 1 (regola 2026-11-27).
    // Danni T7 (+25%, regola in SATier): spada 24, ascia 25, spadone 25, workstaff 20 (base 0), pala 20 (base 0), zappa 21 (base 1), bident 18 (parametro, tooltip 19: fuori dal +25%, vedi SATier), balestra
    // velocità 2,5 (2,0 × 1,25). Arco: nessun parametro di danno (solo incantesimi), uguale al T6.
    // Workstaff T7: menu nostro (SATools.Workstaff.use → WorkstaffMenu) con Foundry e area 7×7 (2026-11-19).

    /**
     * Workstaff T7: danno base 0 invece di 1 (+25% totale, vedi SATier), velocità come la T6 (-2,8F), Fortune VII,
     * Efficiency VIII e Foundry (accesa di default, componente workstaff_drops; si spegne o si sospende dal menu).
     */
    public static final DeferredItem<SATools.Workstaff> UMBRAL_WORKSTAFF = tab(ITEMS.register("umbral_workstaff",
            () -> new SATools.Workstaff(SATier.T7, 0, -2.8F, workstaffMenu(tool(SATier.T7)),
                    Map.of(Enchantments.FORTUNE, 7, Enchantments.EFFICIENCY, 8, SpectrumEnchantmentKeys.FOUNDRY, 1))));

    public static final DeferredItem<SATools.Axe> UMBRAL_AXE = tab(ITEMS.register("umbral_axe",
            () -> new SATools.Axe(SATier.T7, tool(SATier.T7).attributes(AxeItem.createAttributes(SATier.T7, 5.0F, -3.0F)),
                    Map.of(Enchantments.EFFICIENCY, 8))));

    public static final DeferredItem<SATools.Shovel> UMBRAL_SHOVEL = tab(ITEMS.register("umbral_shovel",
            () -> new SATools.Shovel(SATier.T7, tool(SATier.T7).attributes(ShovelItem.createAttributes(SATier.T7, 0.0F, -3.0F)),
                    Map.of(Enchantments.EFFICIENCY, 8))));

    public static final DeferredItem<SATools.Hoe> UMBRAL_HOE = tab(ITEMS.register("umbral_hoe",
            () -> new SATools.Hoe(SATier.T7, tool(SATier.T7).attributes(HoeItem.createAttributes(SATier.T7, 1.0F, 0.0F)),
                    Map.of(Enchantments.FORTUNE, 7))));

    public static final DeferredItem<SATools.Sword> UMBRAL_SWORD = tab(ITEMS.register("umbral_sword",
            () -> new SATools.Sword(SATier.T7, tool(SATier.T7).attributes(SwordItem.createAttributes(SATier.T7, 4, -2.4F)),
                    Map.of(Enchantments.SHARPNESS, 8))));

    public static final DeferredItem<SATools.Bow> UMBRAL_BOW = tab(ITEMS.register("umbral_bow",
            () -> new SATools.Bow(SATier.T7, tool(SATier.T7), Map.of(Enchantments.POWER, 8))));

    public static final DeferredItem<SATools.FishingRod> UMBRAL_FISHING_ROD = tab(ITEMS.register("umbral_fishing_rod",
            () -> new SATools.FishingRod(SATier.T7, tool(SATier.T7), Map.of(Enchantments.LUCK_OF_THE_SEA, 7, Enchantments.LURE, 4))));

    public static final DeferredItem<SATools.Shears> UMBRAL_SHEARS = tab(ITEMS.register("umbral_shears",
            () -> new SATools.Shears(SATier.T7, tool(SATier.T7).component(DataComponents.TOOL, ShearsItem.createToolProperties()),
                    Map.of(Enchantments.EFFICIENCY, 8))));

    /** Mazza T7: danno totale 25, velocità 0,6, Wind Burst VII. */
    public static final DeferredItem<SATools.Mace> UMBRAL_MACE = tab(ITEMS.register("umbral_mace",
            () -> new SATools.Mace(SATier.T7.getEnchantmentValue(),
                    mace(tool(SATier.T7), SATools.Mace.VANILLA_DAMAGE + SATier.T7.getAttackDamageBonus()),
                    Map.of(Enchantments.WIND_BURST, 7))));

    /** Ultra greatsword T7: portata extra 1,5 (glass crest 1,0, T6 1,25). */
    public static final DeferredItem<SATools.UltraGreatsword> UMBRAL_ULTRA_GREATSWORD = tab(ITEMS.register("umbral_ultra_greatsword",
            () -> new SATools.UltraGreatsword(SATier.T7, 5, -2.8F, 1.5F, tool(SATier.T7),
                    Map.of(Enchantments.SWEEPING_EDGE, 7))));

    /** Balestra T7: velocità 2,5, tempo di carica 1,25, divergenza 0,53 (T6 2,0 / 1,6 / 0,63). PROVVISORI. */
    public static final DeferredItem<SATools.Crossbow> UMBRAL_CROSSBOW = tab(ITEMS.register("umbral_crossbow",
            () -> new SATools.Crossbow(SATier.T7, 2.5F, 1.25F, 0.53F, tool(SATier.T7), Map.of(Enchantments.PIERCING, 7, Enchantments.QUICK_CHARGE, 4))));

    /**
     * Bident T7: velocità -2,2, danno 18, perforazione armatura e protezione 39% (T6 -2,2 / 15 / 36% / 36%, Ferocious
     * -2,2 / 13 / 33% / 33%). PROVVISORI. Upgrade alla Fusion Shrine del bident T6, che porta modalità e set.
     */
    public static final DeferredItem<SABident> UMBRAL_BIDENT = tab(ITEMS.register("umbral_bident",
            () -> new SABident(SATier.T7, tool(SATier.T7).component(SAComponents.BIDENT_MODE.get(), BidentMode.FEROCIOUS),
                    -2.2, 18, 0.39F, 0.39F, Map.of(Enchantments.IMPALING, 9))));

    public static final DeferredItem<SAArmorItem> UMBRAL_HELMET = armorPiece(SATier.T7, SAArmorMaterials.UMBRAL, ArmorItem.Type.HELMET, 10);
    public static final DeferredItem<SAArmorItem> UMBRAL_CHESTPLATE = armorPiece(SATier.T7, SAArmorMaterials.UMBRAL, ArmorItem.Type.CHESTPLATE, 7);
    public static final DeferredItem<SAArmorItem> UMBRAL_LEGGINGS = armorPiece(SATier.T7, SAArmorMaterials.UMBRAL, ArmorItem.Type.LEGGINGS, 8);
    public static final DeferredItem<SAArmorItem> UMBRAL_BOOTS = armorPiece(SATier.T7, SAArmorMaterials.UMBRAL, ArmorItem.Type.BOOTS, 8);

    // ---------- T8 · Argenteo (Argent) ----------
    // Un tier sopra il T7, come il T7 sul T6: ogni pezzo si fa dal pezzo T7 (tools/gen_data.py). Stessi parametri di
    // Spectrum; la crescita viene da SATier.T8 (velocità di scavo 20, bonus danno 25: +25% rispetto al T7, vedi SATier;
    // incantabilità 130). Oggetti speciali scalati come il T7 sul T6. Valori PROVVISORI.
    // Incantesimi: decisi dall'utente il 2026-11-19 insieme alle ricette alla Fusion Shrine con Dragonrot (T8_SPEC in
    // tools/gen_data.py). Il T9 (Alloy) NON cambia: tiene i suoi livelli (T9_ENCHANTMENTS).
    // Danni T8 (+25%, regola in SATier): spada 30, ascia 31, spadone 31, workstaff 25 (base -1), pala 25 (base -1),
    // zappa 27 (base 1), bident 21 (parametro, tooltip 22: fuori dal +25%, vedi SATier), balestra velocità 3,1 (tabella di SATier). Arco come T6/T7.
    // Workstaff T8: come il T7, più Inventory Insertion nel menu (2026-11-19).

    /**
     * Workstaff T8: danno base -1 (totale 25 = 16 × 1,25², vedi SATier), velocità -2,8F, Fortune VIII, Efficiency IX,
     * Foundry e Inventory Insertion (accese di default; interruttori nel menu).
     */
    public static final DeferredItem<SATools.Workstaff> ARGENT_WORKSTAFF = tab(ITEMS.register("argent_workstaff",
            () -> new SATools.Workstaff(SATier.T8, -1, -2.8F, workstaffMenu(tool(SATier.T8)),
                    Map.of(Enchantments.FORTUNE, 8, Enchantments.EFFICIENCY, 9, SpectrumEnchantmentKeys.FOUNDRY, 1,
                            SpectrumEnchantmentKeys.INVENTORY_INSERTION, 1))));

    public static final DeferredItem<SATools.Axe> ARGENT_AXE = tab(ITEMS.register("argent_axe",
            () -> new SATools.Axe(SATier.T8, tool(SATier.T8).attributes(AxeItem.createAttributes(SATier.T8, 5.0F, -3.0F)),
                    Map.of(Enchantments.EFFICIENCY, 9))));

    public static final DeferredItem<SATools.Shovel> ARGENT_SHOVEL = tab(ITEMS.register("argent_shovel",
            () -> new SATools.Shovel(SATier.T8, tool(SATier.T8).attributes(ShovelItem.createAttributes(SATier.T8, -1.0F, -3.0F)),
                    Map.of(Enchantments.EFFICIENCY, 9))));

    public static final DeferredItem<SATools.Hoe> ARGENT_HOE = tab(ITEMS.register("argent_hoe",
            () -> new SATools.Hoe(SATier.T8, tool(SATier.T8).attributes(HoeItem.createAttributes(SATier.T8, 1.0F, 0.0F)),
                    Map.of(Enchantments.FORTUNE, 8))));

    public static final DeferredItem<SATools.Sword> ARGENT_SWORD = tab(ITEMS.register("argent_sword",
            () -> new SATools.Sword(SATier.T8, tool(SATier.T8).attributes(SwordItem.createAttributes(SATier.T8, 4, -2.4F)),
                    Map.of(Enchantments.SHARPNESS, 9))));

    public static final DeferredItem<SATools.Bow> ARGENT_BOW = tab(ITEMS.register("argent_bow",
            () -> new SATools.Bow(SATier.T8, tool(SATier.T8), Map.of(Enchantments.POWER, 9))));

    public static final DeferredItem<SATools.FishingRod> ARGENT_FISHING_ROD = tab(ITEMS.register("argent_fishing_rod",
            () -> new SATools.FishingRod(SATier.T8, tool(SATier.T8), Map.of(Enchantments.LUCK_OF_THE_SEA, 8, Enchantments.LURE, 5))));

    public static final DeferredItem<SATools.Shears> ARGENT_SHEARS = tab(ITEMS.register("argent_shears",
            () -> new SATools.Shears(SATier.T8, tool(SATier.T8).component(DataComponents.TOOL, ShearsItem.createToolProperties()),
                    Map.of(Enchantments.EFFICIENCY, 9))));

    /** Mazza T8: danno totale 31, velocità 0,6, Wind Burst VIII (= tetto ATM10). */
    public static final DeferredItem<SATools.Mace> ARGENT_MACE = tab(ITEMS.register("argent_mace",
            () -> new SATools.Mace(SATier.T8.getEnchantmentValue(),
                    mace(tool(SATier.T8), SATools.Mace.VANILLA_DAMAGE + SATier.T8.getAttackDamageBonus()),
                    Map.of(Enchantments.WIND_BURST, 8))));

    /** Ultra greatsword T8: portata extra 1,75 (T6 1,25, T7 1,5). */
    public static final DeferredItem<SATools.UltraGreatsword> ARGENT_ULTRA_GREATSWORD = tab(ITEMS.register("argent_ultra_greatsword",
            () -> new SATools.UltraGreatsword(SATier.T8, 5, -2.8F, 1.75F, tool(SATier.T8),
                    Map.of(Enchantments.SWEEPING_EDGE, 8))));

    /**
     * Balestra T8: velocità 3,1 (tabella di SATier), tempo di carica 1,0, divergenza 0,45 (T7 2,5 / 1,25 / 0,53).
     * Carica e divergenza PROVVISORIE (estrapolate dalla progressione glass crest → T6 → T7). Piercing VIII, Quick Charge IV
     * e Sniping I (2026-11-20: con Quick Charge V la carica era istantanea e il moltiplicatore di carica non contava).
     */
    public static final DeferredItem<SATools.Crossbow> ARGENT_CROSSBOW = tab(ITEMS.register("argent_crossbow",
            () -> new SATools.Crossbow(SATier.T8, 3.1F, 1.0F, 0.45F, tool(SATier.T8),
                    Map.of(Enchantments.PIERCING, 8, Enchantments.QUICK_CHARGE, 4, SpectrumEnchantmentKeys.SNIPING, 1))));

    /**
     * Bident T8: velocità -2,2, danno 21, perforazione armatura e protezione 42% (+3 punti per
     * tier da T7). PROVVISORI. Upgrade alla Fusion Shrine del bident T7, che porta modalità e set.
     */
    public static final DeferredItem<SABident> ARGENT_BIDENT = tab(ITEMS.register("argent_bident",
            () -> new SABident(SATier.T8, tool(SATier.T8).component(SAComponents.BIDENT_MODE.get(), BidentMode.FEROCIOUS),
                    -2.2, 21, 0.42F, 0.42F, Map.of(Enchantments.IMPALING, 10))));

    public static final DeferredItem<SAArmorItem> ARGENT_HELMET = armorPiece(SATier.T8, SAArmorMaterials.ARGENT, ArmorItem.Type.HELMET, 11);
    public static final DeferredItem<SAArmorItem> ARGENT_CHESTPLATE = armorPiece(SATier.T8, SAArmorMaterials.ARGENT, ArmorItem.Type.CHESTPLATE, 8);
    public static final DeferredItem<SAArmorItem> ARGENT_LEGGINGS = armorPiece(SATier.T8, SAArmorMaterials.ARGENT, ArmorItem.Type.LEGGINGS, 9);
    public static final DeferredItem<SAArmorItem> ARGENT_BOOTS = armorPiece(SATier.T8, SAArmorMaterials.ARGENT, ArmorItem.Type.BOOTS, 9);

    // ---------- Alloy (T9) · Prismatico (Prismatic) ----------
    // Un tier sopra il T8, come il T8 sul T7: ogni pezzo si fa dal pezzo T8 con 1 lingotto di ciascuna delle 3 leghe di
    // Allthemodium (tools/gen_data.py, ricerca/14). Crescita da SATier.ALLOY (velocità di scavo 36, bonus danno 32: +25%
    // sul T8, vedi SATier; incantabilità 200). Area della workstaff gratis (SATier.ALLOY.freeAoE).
    // Incantesimi: tetto ATM10 + 2 (regola 2026-11-27, es. Sharpness XI). Tetto ATM10 (SAEnchantCap, 2026-11-19): copia all'Enchanter tagliata per tutti gli
    // stack, libro estratto all'incudine da un nostro oggetto tagliato, Bedrock Anvil che non abbassa mai (mixin).
    // Danni (+25%, regola in SATier): spada 37, ascia 39 (base 6), spadone 39 (base 6), workstaff 31 (base -2),
    // pala 31 (base -2), zappa 33 (base 0), bident 24 (parametro, tooltip 25: fuori dal +25%, vedi SATier), balestra velocità 3,9. Arco come gli altri.
    // PROVVISORI come nel T8: carica e divergenza della balestra, perforazione del bident (portata dello spadone decisa il 2026-11-24).
    // Menu della workstaff Alloy: come il T8 più l'area 9×9, gratis (2026-11-19).

    /**
     * Workstaff Alloy: danno base -2 (totale 31 = 16 × 1,25³, vedi SATier), velocità -2,8F, Fortune X, Efficiency XI,
     * Foundry e Inventory Insertion (accese di default). Area fino a 9×9, gratis.
     */
    public static final DeferredItem<SATools.Workstaff> PRISMATIC_WORKSTAFF = tab(ITEMS.register("prismatic_workstaff",
            () -> new SATools.Workstaff(SATier.ALLOY, -2, -2.8F, workstaffMenu(tool(SATier.ALLOY)),
                    Map.of(Enchantments.FORTUNE, 10, Enchantments.EFFICIENCY, 11, SpectrumEnchantmentKeys.FOUNDRY, 1,
                            SpectrumEnchantmentKeys.INVENTORY_INSERTION, 1))));

    public static final DeferredItem<SATools.Axe> PRISMATIC_AXE = tab(ITEMS.register("prismatic_axe",
            () -> new SATools.Axe(SATier.ALLOY, tool(SATier.ALLOY).attributes(AxeItem.createAttributes(SATier.ALLOY, 6.0F, -3.0F)),
                    Map.of(Enchantments.EFFICIENCY, 11))));

    public static final DeferredItem<SATools.Shovel> PRISMATIC_SHOVEL = tab(ITEMS.register("prismatic_shovel",
            () -> new SATools.Shovel(SATier.ALLOY, tool(SATier.ALLOY).attributes(ShovelItem.createAttributes(SATier.ALLOY, -2.0F, -3.0F)),
                    Map.of(Enchantments.EFFICIENCY, 11))));

    public static final DeferredItem<SATools.Hoe> PRISMATIC_HOE = tab(ITEMS.register("prismatic_hoe",
            () -> new SATools.Hoe(SATier.ALLOY, tool(SATier.ALLOY).attributes(HoeItem.createAttributes(SATier.ALLOY, 0.0F, 0.0F)),
                    Map.of(Enchantments.FORTUNE, 10))));

    public static final DeferredItem<SATools.Sword> PRISMATIC_SWORD = tab(ITEMS.register("prismatic_sword",
            () -> new SATools.Sword(SATier.ALLOY, tool(SATier.ALLOY).attributes(SwordItem.createAttributes(SATier.ALLOY, 4, -2.4F)),
                    Map.of(Enchantments.SHARPNESS, 11))));

    public static final DeferredItem<SATools.Bow> PRISMATIC_BOW = tab(ITEMS.register("prismatic_bow",
            () -> new SATools.Bow(SATier.ALLOY, tool(SATier.ALLOY), Map.of(Enchantments.POWER, 11))));

    public static final DeferredItem<SATools.FishingRod> PRISMATIC_FISHING_ROD = tab(ITEMS.register("prismatic_fishing_rod",
            () -> new SATools.FishingRod(SATier.ALLOY, tool(SATier.ALLOY), Map.of(Enchantments.LUCK_OF_THE_SEA, 10, Enchantments.LURE, 5))));

    public static final DeferredItem<SATools.Shears> PRISMATIC_SHEARS = tab(ITEMS.register("prismatic_shears",
            () -> new SATools.Shears(SATier.ALLOY, tool(SATier.ALLOY).component(DataComponents.TOOL, ShearsItem.createToolProperties()),
                    Map.of(Enchantments.EFFICIENCY, 11))));

    /**
     * Mazza Alloy: danno base 6 (totale 39, come lo spadone Alloy), velocità 0,6, Wind Burst X (tetto ATM10 VIII + 2,
     * DEVE coincidere con T9_ENCHANTMENTS["mace"] di tools/gen_data.py).
     */
    public static final DeferredItem<SATools.Mace> PRISMATIC_MACE = tab(ITEMS.register("prismatic_mace",
            () -> new SATools.Mace(SATier.ALLOY.getEnchantmentValue(),
                    mace(tool(SATier.ALLOY), SATools.Mace.VANILLA_DAMAGE + 1 + SATier.ALLOY.getAttackDamageBonus()),
                    Map.of(Enchantments.WIND_BURST, 10))));

    /** Ultra greatsword Alloy: danno base 6 (totale 39), portata extra 2,0 (T6 1,25, T7 1,5, T8 1,75). */
    public static final DeferredItem<SATools.UltraGreatsword> PRISMATIC_ULTRA_GREATSWORD = tab(ITEMS.register("prismatic_ultra_greatsword",
            () -> new SATools.UltraGreatsword(SATier.ALLOY, 6, -2.8F, 2.0F, tool(SATier.ALLOY),
                    Map.of(Enchantments.SWEEPING_EDGE, 10))));

    /**
     * Balestra Alloy: velocità 3,9 (tabella di SATier), tempo di carica 0,8, divergenza 0,375 (T8 3,1 / 1,0 / 0,45).
     * Carica e divergenza PROVVISORIE (estrapolate dalla progressione glass crest → T6 → T7 → T8). Piercing X, Quick
     * Charge IV e Sniping II (2026-11-20, come il T8).
     */
    public static final DeferredItem<SATools.Crossbow> PRISMATIC_CROSSBOW = tab(ITEMS.register("prismatic_crossbow",
            () -> new SATools.Crossbow(SATier.ALLOY, 3.9F, 0.8F, 0.375F, tool(SATier.ALLOY),
                    Map.of(Enchantments.PIERCING, 10, Enchantments.QUICK_CHARGE, 4, SpectrumEnchantmentKeys.SNIPING, 2))));

    /**
     * Bident Alloy: velocità -2,2, danno 24, perforazione armatura e protezione 45% (+3 punti per
     * tier). PROVVISORI. Upgrade alla Fusion Shrine del bident T8, che porta modalità e set.
     */
    public static final DeferredItem<SABident> PRISMATIC_BIDENT = tab(ITEMS.register("prismatic_bident",
            () -> new SABident(SATier.ALLOY, tool(SATier.ALLOY).component(SAComponents.BIDENT_MODE.get(), BidentMode.FEROCIOUS),
                    -2.2, 24, 0.45F, 0.45F, Map.of(Enchantments.IMPALING, 12))));

    public static final DeferredItem<SAArmorItem> PRISMATIC_HELMET = armorPiece(SATier.ALLOY, SAArmorMaterials.PRISMATIC, ArmorItem.Type.HELMET, 13);
    public static final DeferredItem<SAArmorItem> PRISMATIC_CHESTPLATE = armorPiece(SATier.ALLOY, SAArmorMaterials.PRISMATIC, ArmorItem.Type.CHESTPLATE, 10);
    public static final DeferredItem<SAArmorItem> PRISMATIC_LEGGINGS = armorPiece(SATier.ALLOY, SAArmorMaterials.PRISMATIC, ArmorItem.Type.LEGGINGS, 11);
    public static final DeferredItem<SAArmorItem> PRISMATIC_BOOTS = armorPiece(SATier.ALLOY, SAArmorMaterials.PRISMATIC, ArmorItem.Type.BOOTS, 11);

    private SAItems() {
    }

    private static <T extends Item> DeferredItem<T> tab(DeferredItem<T> item) {
        EQUIPMENT_TAB.add(item);
        return item;
    }

    /**
     * Un pezzo d'armatura del tier: id «&lt;tier&gt;_&lt;pezzo&gt;» (es. trichromatic_boots), nel creativo e in {@link #armor(SATier)}.
     * Incantesimo caratteristico per pezzo (quello della bedrock di Spectrum), al livello dato: elmo Projectile Protection,
     * corazza Protection, gambali Blast Protection, stivali Fire Protection.
     */
    private static DeferredItem<SAArmorItem> armorPiece(SATier tier, Holder<ArmorMaterial> material, ArmorItem.Type type, int level) {
        ResourceKey<Enchantment> enchantment = switch (type) {
            case HELMET -> Enchantments.PROJECTILE_PROTECTION;
            case CHESTPLATE -> Enchantments.PROTECTION;
            case LEGGINGS -> Enchantments.BLAST_PROTECTION;
            case BOOTS -> Enchantments.FIRE_PROTECTION;
            case BODY -> throw new IllegalArgumentException("armatura per cavalli/lupi: non è dei tier");
        };
        DeferredItem<SAArmorItem> item = tab(ITEMS.register(tier.idPrefix() + "_" + type.getName(),
                () -> new SAArmorItem(tier, material, type, tool(tier), Map.of(enchantment, level))));
        ARMOR.computeIfAbsent(tier, t -> new ArrayList<>()).add(item);
        return item;
    }

    /** Proprietà comuni (attrezzi e armatura): indistruttibile e resistente al fuoco, come la bedrock di Spectrum. */
    private static Item.Properties tool(SATier tier) {
        return new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.EPIC)
                .fireResistant()
                .durability(tier.getUses())
                .component(DataComponents.UNBREAKABLE, new Unbreakable(false));
    }

    /**
     * Mazza: attributi (danno = modificatore, velocità vanilla -3,4) e componente tool della vanilla
     * (MaceItem.createToolProperties: non scava, come la mazza di Minecraft).
     */
    private static Item.Properties mace(Item.Properties properties, double damage) {
        return properties.attributes(SATools.Mace.attributes(damage, SATools.Mace.VANILLA_SPEED))
                .component(DataComponents.TOOL, MaceItem.createToolProperties());
    }

    /**
     * Workstaff T7+: interruttori del menu (Foundry, Inventory Insertion) accesi di default, così anche lo stack dato
     * con /give o preso dal creativo parte nello stato giusto (gli incantesimi li mette Preenchanted / la ricetta).
     */
    private static Item.Properties workstaffMenu(Item.Properties properties) {
        return properties.component(SAComponents.WORKSTAFF_DROPS.get(), WorkstaffDrops.DEFAULT);
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    /** Gli incantesimi preimpostati di un oggetto (vuoto se non ne ha). Usato anche dai GameTest. */
    public static Map<ResourceKey<Enchantment>, Integer> defaultEnchantments(Item item) {
        return item instanceof Preenchanted p ? p.getDefaultEnchantments() : Map.of();
    }

    /** Gli oggetti dei tier T6-T9 (16 per tier), nell'ordine del creativo. La mazza di bedrock NON c'è: vedi {@link #all()}. */
    public static List<DeferredItem<? extends Item>> equipment() {
        return List.copyOf(EQUIPMENT_TAB);
    }

    /** Tutti gli oggetti della mod nell'ordine del creativo: la mazza di bedrock, poi {@link #equipment()}. */
    public static List<DeferredItem<? extends Item>> all() {
        List<DeferredItem<? extends Item>> all = new ArrayList<>();
        all.add(BEDROCK_MACE);
        all.addAll(EQUIPMENT_TAB);
        return List.copyOf(all);
    }

    /** I pezzi d'armatura di un tier, nell'ordine elmo, corazza, gambali, stivali (registrati da {@link #armorPiece}). */
    public static List<DeferredItem<SAArmorItem>> armor(SATier tier) {
        return List.copyOf(ARMOR.getOrDefault(tier, List.of()));
    }

    /** Tutti i pezzi d'armatura di tutti i tier. */
    public static List<DeferredItem<SAArmorItem>> armor() {
        List<DeferredItem<SAArmorItem>> all = new ArrayList<>();
        for (SATier tier : SATier.values()) {
            all.addAll(armor(tier));
        }
        return List.copyOf(all);
    }

    /** Mette i nostri oggetti, già incantati, nella sotto-tab "Equipment" del creativo di Spectrum. */
    public static void addToSpectrumSubTabs(CreativeSubTabEvent event) {
        if (!event.subGroup().getIdentifier().equals(ItemGroupIDs.SUBTAB_EQUIPMENT)) {
            return;
        }
        var lookup = event.getParameters().holders();
        for (DeferredItem<? extends Item> holder : all()) {
            ItemStack stack = new ItemStack(holder.get());
            if (holder.get() instanceof Preenchanted preenchanted) {
                stack.set(DataComponents.ENCHANTMENTS, Preenchanted.buildDefaultEnchantments(lookup, preenchanted));
            }
            event.getItemDisplayBuilder().accept(stack);
        }
    }
}
