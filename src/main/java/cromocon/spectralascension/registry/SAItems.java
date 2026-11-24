package cromocon.spectralascension.registry;

import cromocon.spectralascension.SpectralAscension;
import cromocon.spectralascension.item.BidentMode;
import cromocon.spectralascension.item.SAArmorItem;
import cromocon.spectralascension.item.SABident;
import cromocon.spectralascension.item.SATier;
import cromocon.spectralascension.item.SATools;
import cromocon.spectralascension.item.WorkstaffDrops;
import de.dafuqs.spectrum.registries.SpectrumEnchantmentKeys;
import de.dafuqs.fractal.api.CreativeSubTabEvent;
import de.dafuqs.spectrum.api.item.Preenchanted;
import de.dafuqs.spectrum.api.item_group.ItemGroupIDs;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
import java.util.List;
import java.util.Map;

/**
 * Tutti gli item della mod. Un {@link DeferredRegister} raccoglie le "promesse" di item:
 * NeoForge li crea al momento giusto dell'avvio.
 * <p>
 * Incantesimi preimpostati: lo stesso incantesimo caratteristico della versione bedrock di Spectrum,
 * portato al tetto del tier (T6 = massimo ATM10, da config/apotheosis/enchantments.cfg).
 * DEVONO coincidere con quelli delle ricette (tools/gen_data.py): lo verificano i GameTest.
 */
public final class SAItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SpectralAscension.MOD_ID);

    /** Ordine in cui compaiono nella sotto-tab Equipment di Spectrum. */
    private static final List<DeferredItem<? extends Item>> EQUIPMENT_TAB = new ArrayList<>();

    // ---------- T6 · Tricromatico (Trichromatic) ----------
    // Incantesimi del T6 decisi dall'utente il 2026-11-18 insieme alle ricette alla Fusion Shrine con Dragonrot
    // (tools/gen_data.py): sotto il tetto ATM10.

    /**
     * Workstaff = il piccone del tier (upgrade alla Fusion Shrine della workstaff glass crest). Danno e velocità
     * d'attacco come la glass crest in SpectrumItems (1, -2,8F), sommati al bonus del nostro tier.
     * Fortune V (selezione di default del menu) ed Efficiency V.
     */
    public static final DeferredItem<SATools.Workstaff> TRICHROMATIC_WORKSTAFF = tab(ITEMS.register("trichromatic_workstaff",
            () -> new SATools.Workstaff(SATier.T6, 1, -2.8F, tool(SATier.T6),
                    Map.of(Enchantments.FORTUNE, 6, Enchantments.EFFICIENCY, 5))));

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

    // Oggetti speciali: upgrade alla Fusion Shrine delle versioni glass crest, con il loro incantesimo caratteristico
    // (quello della versione malachite, che le glass crest ereditano) al tetto T6.

    /**
     * Ultra greatsword: Sweeping Edge VI (Spectrum IV), che è anche la forza del ground slam. Danno extra e velocità
     * come la glass crest (5, -2,8F) sommati al bonus del tier; portata extra 1,5 invece di 1,0 (PROVVISORIA).
     */
    public static final DeferredItem<SATools.UltraGreatsword> TRICHROMATIC_ULTRA_GREATSWORD = tab(ITEMS.register("trichromatic_ultra_greatsword",
            () -> new SATools.UltraGreatsword(SATier.T6, 5, -2.8F, 1.5F, tool(SATier.T6),
                    Map.of(Enchantments.SWEEPING_EDGE, 6))));

    /**
     * Balestra: Quick Charge IV e Piercing V (Spectrum: Piercing V). Velocità 2,0, tempo di carica 1,6 e divergenza 0,63 PROVVISORI
     * (glass crest 1,5 / 2,0 / 0,75), vedi SATools.Crossbow.
     */
    public static final DeferredItem<SATools.Crossbow> TRICHROMATIC_CROSSBOW = tab(ITEMS.register("trichromatic_crossbow",
            () -> new SATools.Crossbow(SATier.T6, 2.0F, 1.6F, 0.63F, tool(SATier.T6), Map.of(Enchantments.QUICK_CHARGE, 3, Enchantments.PIERCING, 5))));

    /**
     * Bident: fonde Ferocious e Fractal (modalità in SABident). Impaling VI (come i due glass crest di Spectrum).
     * Valori PROVVISORI sopra il Ferocious (-2,2 / 13 / 33% / 33%): velocità -2,2, danno 15, perforazione armatura e
     * protezione 36%. Parte in modalità Ferocious, come i glass crest che escono già attivi.
     */
    public static final DeferredItem<SABident> TRICHROMATIC_BIDENT = tab(ITEMS.register("trichromatic_bident",
            () -> new SABident(SATier.T6, tool(SATier.T6).component(SAComponents.BIDENT_MODE.get(), BidentMode.FEROCIOUS),
                    -2.2, 15, 0.36F, 0.36F, Map.of(Enchantments.IMPALING, 6))));

    // Armatura: incantesimo caratteristico della bedrock (livello 5 in Spectrum) al tetto T6.
    // Il Dike del set completo è in SAArmorSet; statistiche in SAArmorMaterials.

    public static final DeferredItem<SAArmorItem> TRICHROMATIC_HELMET = tab(ITEMS.register("trichromatic_helmet",
            () -> new SAArmorItem(SATier.T6, SAArmorMaterials.TRICHROMATIC, ArmorItem.Type.HELMET, tool(SATier.T6),
                    Map.of(Enchantments.PROJECTILE_PROTECTION, 6))));

    public static final DeferredItem<SAArmorItem> TRICHROMATIC_CHESTPLATE = tab(ITEMS.register("trichromatic_chestplate",
            () -> new SAArmorItem(SATier.T6, SAArmorMaterials.TRICHROMATIC, ArmorItem.Type.CHESTPLATE, tool(SATier.T6),
                    Map.of(Enchantments.PROTECTION, 6))));

    public static final DeferredItem<SAArmorItem> TRICHROMATIC_LEGGINGS = tab(ITEMS.register("trichromatic_leggings",
            () -> new SAArmorItem(SATier.T6, SAArmorMaterials.TRICHROMATIC, ArmorItem.Type.LEGGINGS, tool(SATier.T6),
                    Map.of(Enchantments.BLAST_PROTECTION, 6))));

    public static final DeferredItem<SAArmorItem> TRICHROMATIC_BOOTS = tab(ITEMS.register("trichromatic_boots",
            () -> new SAArmorItem(SATier.T6, SAArmorMaterials.TRICHROMATIC, ArmorItem.Type.BOOTS, tool(SATier.T6),
                    Map.of(Enchantments.FIRE_PROTECTION, 6))));

    // ---------- T7 · Umbrale (Umbral) ----------
    // Un tier sopra il T6: ogni pezzo si fa dal pezzo T6 (tools/gen_data.py). Gli attrezzi hanno gli stessi parametri
    // del T6 (presi dalla versione bedrock o glass crest di Spectrum): la crescita viene da SATier.T7 (velocità di
    // scavo 17 invece di 16, bonus danno 19 invece di 14: +25% rispetto al T6, vedi SATier; incantabilità 105 invece di 90). Gli oggetti speciali hanno
    // anche valori propri, scalati come il T6 li scala rispetto alla glass crest. Valori PROVVISORI.
    // Incantesimi: stesso tetto del T6 (massimo ATM10, decisione di design).
    // Danni T7 (+25%, regola in SATier): spada 24, ascia 25, spadone 25, workstaff 20 (base 0), pala 20 (base 0), zappa 21 (base 1), bident 21 (17 × 1,25), balestra
    // velocità 2,5 (2,0 × 1,25). Arco: nessun parametro di danno (solo incantesimi), uguale al T6.
    // Workstaff T7: menu nostro (SATools.Workstaff.use → WorkstaffMenu) con Foundry e area 7×7 (2026-11-19).

    /**
     * Workstaff T7: danno base 0 invece di 1 (+25% totale, vedi SATier), velocità come la T6 (-2,8F), Fortune VII,
     * Efficiency VII e Foundry (accesa di default, componente workstaff_drops; si spegne o si sospende dal menu).
     */
    public static final DeferredItem<SATools.Workstaff> UMBRAL_WORKSTAFF = tab(ITEMS.register("umbral_workstaff",
            () -> new SATools.Workstaff(SATier.T7, 0, -2.8F, workstaffMenu(tool(SATier.T7)),
                    Map.of(Enchantments.FORTUNE, 7, Enchantments.EFFICIENCY, 7, SpectrumEnchantmentKeys.FOUNDRY, 1))));

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

    /** Ultra greatsword T7: portata extra 2,0 (glass crest 1,0, T6 1,5). PROVVISORIA. */
    public static final DeferredItem<SATools.UltraGreatsword> UMBRAL_ULTRA_GREATSWORD = tab(ITEMS.register("umbral_ultra_greatsword",
            () -> new SATools.UltraGreatsword(SATier.T7, 5, -2.8F, 2.0F, tool(SATier.T7),
                    Map.of(Enchantments.SWEEPING_EDGE, 7))));

    /** Balestra T7: velocità 2,5, tempo di carica 1,25, divergenza 0,53 (T6 2,0 / 1,6 / 0,63). PROVVISORI. */
    public static final DeferredItem<SATools.Crossbow> UMBRAL_CROSSBOW = tab(ITEMS.register("umbral_crossbow",
            () -> new SATools.Crossbow(SATier.T7, 2.5F, 1.25F, 0.53F, tool(SATier.T7), Map.of(Enchantments.PIERCING, 6, Enchantments.QUICK_CHARGE, 4))));

    /**
     * Bident T7: velocità -2,2, danno 18, perforazione armatura e protezione 39% (T6 -2,2 / 15 / 36% / 36%, Ferocious
     * -2,2 / 13 / 33% / 33%). PROVVISORI. Upgrade alla Fusion Shrine del bident T6, che porta modalità e set.
     */
    public static final DeferredItem<SABident> UMBRAL_BIDENT = tab(ITEMS.register("umbral_bident",
            () -> new SABident(SATier.T7, tool(SATier.T7).component(SAComponents.BIDENT_MODE.get(), BidentMode.FEROCIOUS),
                    -2.2, 18, 0.39F, 0.39F, Map.of(Enchantments.IMPALING, 7))));

    public static final DeferredItem<SAArmorItem> UMBRAL_HELMET = tab(ITEMS.register("umbral_helmet",
            () -> new SAArmorItem(SATier.T7, SAArmorMaterials.UMBRAL, ArmorItem.Type.HELMET, tool(SATier.T7),
                    Map.of(Enchantments.PROJECTILE_PROTECTION, 7))));

    public static final DeferredItem<SAArmorItem> UMBRAL_CHESTPLATE = tab(ITEMS.register("umbral_chestplate",
            () -> new SAArmorItem(SATier.T7, SAArmorMaterials.UMBRAL, ArmorItem.Type.CHESTPLATE, tool(SATier.T7),
                    Map.of(Enchantments.PROTECTION, 7))));

    public static final DeferredItem<SAArmorItem> UMBRAL_LEGGINGS = tab(ITEMS.register("umbral_leggings",
            () -> new SAArmorItem(SATier.T7, SAArmorMaterials.UMBRAL, ArmorItem.Type.LEGGINGS, tool(SATier.T7),
                    Map.of(Enchantments.BLAST_PROTECTION, 7))));

    public static final DeferredItem<SAArmorItem> UMBRAL_BOOTS = tab(ITEMS.register("umbral_boots",
            () -> new SAArmorItem(SATier.T7, SAArmorMaterials.UMBRAL, ArmorItem.Type.BOOTS, tool(SATier.T7),
                    Map.of(Enchantments.FIRE_PROTECTION, 7))));

    // ---------- T8 · Argenteo (Argent) ----------
    // Un tier sopra il T7, come il T7 sul T6: ogni pezzo si fa dal pezzo T7 (tools/gen_data.py). Stessi parametri di
    // Spectrum; la crescita viene da SATier.T8 (velocità di scavo 20, bonus danno 25: +25% rispetto al T7, vedi SATier;
    // incantabilità 130). Oggetti speciali scalati come il T7 sul T6. Valori PROVVISORI.
    // Incantesimi: decisi dall'utente il 2026-11-19 insieme alle ricette alla Fusion Shrine con Dragonrot (T8_SPEC in
    // tools/gen_data.py). Il T9 (Alloy) NON cambia: tiene i suoi livelli (T9_ENCHANTMENTS).
    // Danni T8 (+25%, regola in SATier): spada 30, ascia 31, spadone 31, workstaff 25 (base -1), pala 25 (base -1),
    // zappa 27 (base 1), bident 27 (round(17 × 1,25²)), balestra velocità 3,1 (tabella di SATier). Arco come T6/T7.
    // Workstaff T8: come il T7, più Inventory Insertion nel menu (2026-11-19).

    /**
     * Workstaff T8: danno base -1 (totale 25 = 16 × 1,25², vedi SATier), velocità -2,8F, Fortune VIII, Efficiency VIII,
     * Foundry e Inventory Insertion (accese di default; interruttori nel menu).
     */
    public static final DeferredItem<SATools.Workstaff> ARGENT_WORKSTAFF = tab(ITEMS.register("argent_workstaff",
            () -> new SATools.Workstaff(SATier.T8, -1, -2.8F, workstaffMenu(tool(SATier.T8)),
                    Map.of(Enchantments.FORTUNE, 8, Enchantments.EFFICIENCY, 8, SpectrumEnchantmentKeys.FOUNDRY, 1,
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

    /** Ultra greatsword T8: portata extra 2,5 (T6 1,5, T7 2,0). PROVVISORIA. */
    public static final DeferredItem<SATools.UltraGreatsword> ARGENT_ULTRA_GREATSWORD = tab(ITEMS.register("argent_ultra_greatsword",
            () -> new SATools.UltraGreatsword(SATier.T8, 5, -2.8F, 2.5F, tool(SATier.T8),
                    Map.of(Enchantments.SWEEPING_EDGE, 8))));

    /**
     * Balestra T8: velocità 3,1 (tabella di SATier), tempo di carica 1,0, divergenza 0,45 (T7 2,5 / 1,25 / 0,53).
     * Carica e divergenza PROVVISORIE (estrapolate dalla progressione glass crest → T6 → T7). Piercing VII, Quick Charge IV
     * e Sniping I (2026-11-20: con Quick Charge V la carica era istantanea e il moltiplicatore di carica non contava).
     */
    public static final DeferredItem<SATools.Crossbow> ARGENT_CROSSBOW = tab(ITEMS.register("argent_crossbow",
            () -> new SATools.Crossbow(SATier.T8, 3.1F, 1.0F, 0.45F, tool(SATier.T8),
                    Map.of(Enchantments.PIERCING, 7, Enchantments.QUICK_CHARGE, 4, SpectrumEnchantmentKeys.SNIPING, 1))));

    /**
     * Bident T8: velocità -2,2, danno 21, perforazione armatura e protezione 42% (+3 punti per
     * tier da T7). PROVVISORI. Upgrade alla Fusion Shrine del bident T7, che porta modalità e set.
     */
    public static final DeferredItem<SABident> ARGENT_BIDENT = tab(ITEMS.register("argent_bident",
            () -> new SABident(SATier.T8, tool(SATier.T8).component(SAComponents.BIDENT_MODE.get(), BidentMode.FEROCIOUS),
                    -2.2, 21, 0.42F, 0.42F, Map.of(Enchantments.IMPALING, 8))));

    public static final DeferredItem<SAArmorItem> ARGENT_HELMET = tab(ITEMS.register("argent_helmet",
            () -> new SAArmorItem(SATier.T8, SAArmorMaterials.ARGENT, ArmorItem.Type.HELMET, tool(SATier.T8),
                    Map.of(Enchantments.PROJECTILE_PROTECTION, 8))));

    public static final DeferredItem<SAArmorItem> ARGENT_CHESTPLATE = tab(ITEMS.register("argent_chestplate",
            () -> new SAArmorItem(SATier.T8, SAArmorMaterials.ARGENT, ArmorItem.Type.CHESTPLATE, tool(SATier.T8),
                    Map.of(Enchantments.PROTECTION, 8))));

    public static final DeferredItem<SAArmorItem> ARGENT_LEGGINGS = tab(ITEMS.register("argent_leggings",
            () -> new SAArmorItem(SATier.T8, SAArmorMaterials.ARGENT, ArmorItem.Type.LEGGINGS, tool(SATier.T8),
                    Map.of(Enchantments.BLAST_PROTECTION, 8))));

    public static final DeferredItem<SAArmorItem> ARGENT_BOOTS = tab(ITEMS.register("argent_boots",
            () -> new SAArmorItem(SATier.T8, SAArmorMaterials.ARGENT, ArmorItem.Type.BOOTS, tool(SATier.T8),
                    Map.of(Enchantments.FIRE_PROTECTION, 8))));

    // ---------- Alloy (T9) · Prismatico (Prismatic) ----------
    // Un tier sopra il T8, come il T8 sul T7: ogni pezzo si fa dal pezzo T8 con 1 lingotto di ciascuna delle 3 leghe di
    // Allthemodium (tools/gen_data.py, ricerca/14). Crescita da SATier.ALLOY (velocità di scavo 36, bonus danno 32: +25%
    // sul T8, vedi SATier; incantabilità 200). Area della workstaff gratis (SATier.ALLOY.freeAoE).
    // Incantesimi: tetto ATM10 + 3 (ricerca/README.md, «Tetto incantesimi»: Alloy = massimo ATM10 + 3, es. Sharpness XII),
    // cioè gli stessi del T8 di prima del 2026-11-19 + 3 livelli (congelati: non seguono il nuovo T8). Tetto ATM10 (SAEnchantCap, 2026-11-19): copia all'Enchanter tagliata per tutti gli
    // stack, libro estratto all'incudine da un nostro oggetto tagliato, Bedrock Anvil che non abbassa mai (mixin).
    // Danni (+25%, regola in SATier): spada 37, ascia 39 (base 6), spadone 39 (base 6), workstaff 31 (base -2),
    // pala 31 (base -2), zappa 33 (base 0), bident 33 (round(17 × 1,25³)), balestra velocità 3,9. Arco come gli altri.
    // PROVVISORI come nel T8: carica e divergenza della balestra, portata dello spadone, perforazione del bident.
    // Menu della workstaff Alloy: come il T8 più l'area 9×9, gratis (2026-11-19).

    /**
     * Workstaff Alloy: danno base -2 (totale 31 = 16 × 1,25³, vedi SATier), velocità -2,8F, Fortune XI, Efficiency XII,
     * Foundry e Inventory Insertion (accese di default). Area fino a 9×9, gratis.
     */
    public static final DeferredItem<SATools.Workstaff> PRISMATIC_WORKSTAFF = tab(ITEMS.register("prismatic_workstaff",
            () -> new SATools.Workstaff(SATier.ALLOY, -2, -2.8F, workstaffMenu(tool(SATier.ALLOY)),
                    Map.of(Enchantments.FORTUNE, 11, Enchantments.EFFICIENCY, 12, SpectrumEnchantmentKeys.FOUNDRY, 1,
                            SpectrumEnchantmentKeys.INVENTORY_INSERTION, 1))));

    public static final DeferredItem<SATools.Axe> PRISMATIC_AXE = tab(ITEMS.register("prismatic_axe",
            () -> new SATools.Axe(SATier.ALLOY, tool(SATier.ALLOY).attributes(AxeItem.createAttributes(SATier.ALLOY, 6.0F, -3.0F)),
                    Map.of(Enchantments.EFFICIENCY, 12))));

    public static final DeferredItem<SATools.Shovel> PRISMATIC_SHOVEL = tab(ITEMS.register("prismatic_shovel",
            () -> new SATools.Shovel(SATier.ALLOY, tool(SATier.ALLOY).attributes(ShovelItem.createAttributes(SATier.ALLOY, -2.0F, -3.0F)),
                    Map.of(Enchantments.EFFICIENCY, 12))));

    public static final DeferredItem<SATools.Hoe> PRISMATIC_HOE = tab(ITEMS.register("prismatic_hoe",
            () -> new SATools.Hoe(SATier.ALLOY, tool(SATier.ALLOY).attributes(HoeItem.createAttributes(SATier.ALLOY, 0.0F, 0.0F)),
                    Map.of(Enchantments.FORTUNE, 11))));

    public static final DeferredItem<SATools.Sword> PRISMATIC_SWORD = tab(ITEMS.register("prismatic_sword",
            () -> new SATools.Sword(SATier.ALLOY, tool(SATier.ALLOY).attributes(SwordItem.createAttributes(SATier.ALLOY, 4, -2.4F)),
                    Map.of(Enchantments.SHARPNESS, 12))));

    public static final DeferredItem<SATools.Bow> PRISMATIC_BOW = tab(ITEMS.register("prismatic_bow",
            () -> new SATools.Bow(SATier.ALLOY, tool(SATier.ALLOY), Map.of(Enchantments.POWER, 12))));

    public static final DeferredItem<SATools.FishingRod> PRISMATIC_FISHING_ROD = tab(ITEMS.register("prismatic_fishing_rod",
            () -> new SATools.FishingRod(SATier.ALLOY, tool(SATier.ALLOY), Map.of(Enchantments.LUCK_OF_THE_SEA, 11, Enchantments.LURE, 5))));

    public static final DeferredItem<SATools.Shears> PRISMATIC_SHEARS = tab(ITEMS.register("prismatic_shears",
            () -> new SATools.Shears(SATier.ALLOY, tool(SATier.ALLOY).component(DataComponents.TOOL, ShearsItem.createToolProperties()),
                    Map.of(Enchantments.EFFICIENCY, 12))));

    /** Ultra greatsword Alloy: danno base 6 (totale 39), portata extra 3,0 (T6 1,5, T7 2,0, T8 2,5). PROVVISORIA. */
    public static final DeferredItem<SATools.UltraGreatsword> PRISMATIC_ULTRA_GREATSWORD = tab(ITEMS.register("prismatic_ultra_greatsword",
            () -> new SATools.UltraGreatsword(SATier.ALLOY, 6, -2.8F, 3.0F, tool(SATier.ALLOY),
                    Map.of(Enchantments.SWEEPING_EDGE, 11))));

    /**
     * Balestra Alloy: velocità 3,9 (tabella di SATier), tempo di carica 0,8, divergenza 0,375 (T8 3,1 / 1,0 / 0,45).
     * Carica e divergenza PROVVISORIE (estrapolate dalla progressione glass crest → T6 → T7 → T8). Piercing XI, Quick
     * Charge IV e Sniping II (2026-11-20, come il T8).
     */
    public static final DeferredItem<SATools.Crossbow> PRISMATIC_CROSSBOW = tab(ITEMS.register("prismatic_crossbow",
            () -> new SATools.Crossbow(SATier.ALLOY, 3.9F, 0.8F, 0.375F, tool(SATier.ALLOY),
                    Map.of(Enchantments.PIERCING, 11, Enchantments.QUICK_CHARGE, 4, SpectrumEnchantmentKeys.SNIPING, 2))));

    /**
     * Bident Alloy: velocità -2,2, danno 24, perforazione armatura e protezione 45% (+3 punti per
     * tier). PROVVISORI. Upgrade alla Fusion Shrine del bident T8, che porta modalità e set.
     */
    public static final DeferredItem<SABident> PRISMATIC_BIDENT = tab(ITEMS.register("prismatic_bident",
            () -> new SABident(SATier.ALLOY, tool(SATier.ALLOY).component(SAComponents.BIDENT_MODE.get(), BidentMode.FEROCIOUS),
                    -2.2, 24, 0.45F, 0.45F, Map.of(Enchantments.IMPALING, 10))));

    public static final DeferredItem<SAArmorItem> PRISMATIC_HELMET = tab(ITEMS.register("prismatic_helmet",
            () -> new SAArmorItem(SATier.ALLOY, SAArmorMaterials.PRISMATIC, ArmorItem.Type.HELMET, tool(SATier.ALLOY),
                    Map.of(Enchantments.PROJECTILE_PROTECTION, 14))));

    public static final DeferredItem<SAArmorItem> PRISMATIC_CHESTPLATE = tab(ITEMS.register("prismatic_chestplate",
            () -> new SAArmorItem(SATier.ALLOY, SAArmorMaterials.PRISMATIC, ArmorItem.Type.CHESTPLATE, tool(SATier.ALLOY),
                    Map.of(Enchantments.PROTECTION, 11))));

    public static final DeferredItem<SAArmorItem> PRISMATIC_LEGGINGS = tab(ITEMS.register("prismatic_leggings",
            () -> new SAArmorItem(SATier.ALLOY, SAArmorMaterials.PRISMATIC, ArmorItem.Type.LEGGINGS, tool(SATier.ALLOY),
                    Map.of(Enchantments.BLAST_PROTECTION, 12))));

    public static final DeferredItem<SAArmorItem> PRISMATIC_BOOTS = tab(ITEMS.register("prismatic_boots",
            () -> new SAArmorItem(SATier.ALLOY, SAArmorMaterials.PRISMATIC, ArmorItem.Type.BOOTS, tool(SATier.ALLOY),
                    Map.of(Enchantments.FIRE_PROTECTION, 12))));

    private SAItems() {
    }

    private static <T extends Item> DeferredItem<T> tab(DeferredItem<T> item) {
        EQUIPMENT_TAB.add(item);
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

    public static List<DeferredItem<? extends Item>> equipment() {
        return List.copyOf(EQUIPMENT_TAB);
    }

    /** I pezzi d'armatura di un tier, nell'ordine elmo, corazza, gambali, stivali (vuoto per i tier non ancora fatti). */
    public static List<DeferredItem<SAArmorItem>> armor(SATier tier) {
        return switch (tier) {
            case T6 -> List.of(TRICHROMATIC_HELMET, TRICHROMATIC_CHESTPLATE, TRICHROMATIC_LEGGINGS, TRICHROMATIC_BOOTS);
            case T7 -> List.of(UMBRAL_HELMET, UMBRAL_CHESTPLATE, UMBRAL_LEGGINGS, UMBRAL_BOOTS);
            case T8 -> List.of(ARGENT_HELMET, ARGENT_CHESTPLATE, ARGENT_LEGGINGS, ARGENT_BOOTS);
            case ALLOY -> List.of(PRISMATIC_HELMET, PRISMATIC_CHESTPLATE, PRISMATIC_LEGGINGS, PRISMATIC_BOOTS);
        };
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
        for (DeferredItem<? extends Item> holder : EQUIPMENT_TAB) {
            ItemStack stack = new ItemStack(holder.get());
            if (holder.get() instanceof Preenchanted preenchanted) {
                stack.set(DataComponents.ENCHANTMENTS, Preenchanted.buildDefaultEnchantments(lookup, preenchanted));
            }
            event.getItemDisplayBuilder().accept(stack);
        }
    }
}
