package cromocon.spectralascension.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

/**
 * I materiali degli attrezzi per tier ("Tier" in Minecraft: velocità di scavo, danno, incantabilità,
 * blocchi che NON può scavare con drop).
 * <p>
 * Valori PROVVISORI da ricerca/07 §5.3: stanno sopra ATM nello stesso tier e sotto il tier ATM successivo.
 * Da bilanciare in gioco.
 * <p>
 * Il mining level usa direttamente i tag di Allthemodium {@code c:incorrect_for_<metallo>_tool}:
 * T6 (Trichromatic) scava come un attrezzo allthemodium, T7 (Umbral) come uno di vibranium (decisione dell'utente:
 * esattamente il tag del vibranium di ATM, anche se così non scava il minerale di unobtainium).
 * <p>
 * <b>Regola dei danni: +25% per tier.</b> Ogni tier colpisce il 25% in più del precedente (fattore
 * {@link #DAMAGE_FACTOR}), misurato sul danno TOTALE di una spada: 4 (base) + bonus del tier + 1 (mano del giocatore).
 * Il bonus del tier (attackDamageBonus) si ricava così: {@code round(19 × 1,25^(n-6)) - 5}, e gli altri attrezzi
 * (ascia, spadone, workstaff, che hanno un danno base proprio) lo sommano. Il T6 è il riferimento (spada 19).
 * <pre>
 *   tier   spada (totale)   bonus tier   fattore calcolato
 *   T6     19               14           1,00 (riferimento)
 *   T7     24 (23,75)       19           1,25
 *   T8     30 (29,69)       25           1,5625
 *   ALLOY  37 (37,11)       32           1,953125
 * </pre>
 * Le armi con proiettile (balestra: la velocità della freccia moltiplica il danno) seguono lo stesso 1,25 sui loro
 * parametri in SAItems: velocità 2,0 → 2,5 → 3,1 → 3,9 (arrotondata). Il bident (danno ASSOLUTO) NON segue più il +25%:
 * parametro 15 → 18 → 21 → 24 (tooltip 16 / 19 / 22 / 25), con perforazione 36% / 39% / 42% / 45%.
 * <p>
 * <b>Area della workstaff gratis dall'Alloy:</b> {@link #freeAoE()} (vero solo per ALLOY). Vedi SATools.Workstaff.
 */
public enum SATier implements Tier {
    T6("trichromatic", 16.0F, 14.0F, 90, "c:incorrect_for_allthemodium_tool", "c:ingots/allthemodium", false),
    // T7: esattamente il tag del vibranium di ATM (decisione dell'utente). Nei tag di Allthemodium 3.2.1 un attrezzo
    // scava il minerale del metallo SUCCESSIVO: c:incorrect_for_allthemodium_tool esclude #c:needs_vibranium_tool
    // (minerale di unobtainium) ma non #c:needs_allthemodium_tool (minerale di vibranium), e così via. Quindi il T6
    // scava il vibranium, il T7 l'unobtainium e il T8 la pietra antica, come i picconi ATM dello stesso metallo
    // (GameTest SAFunctionTests.workstaff_mining). Un commento precedente diceva il contrario: era sbagliato.
    T7("umbral", 17.0F, 19.0F, 105, "c:incorrect_for_vibranium_tool", "c:ingots/vibranium", false),
    // T8 e ALLOY: i bonus seguono la regola +25% (spada 30 e 37), vedi la tabella sopra. T8 (Argent): oggetti in SAItems.
    T8("argent", 20.0F, 25.0F, 130, "c:incorrect_for_unobtainium_tool", "c:ingots/unobtainium", false),
    // ALLOY (Prismatic, «T9» in tools/): area della workstaff gratis (freeAoE = true). Oggetti in SAItems (PRISMATIC_*).
    // Scava come gli attrezzi Alloy di ATM (c:incorrect_for_allthemodiumalloy_tool). Ingrediente di riparazione
    // nominale (oggetti indistruttibili): la lega unobtainium-allthemodium.
    ALLOY("prismatic", 36.0F, 32.0F, 200, "c:incorrect_for_allthemodiumalloy_tool", "c:ingots/unobtainium_allthemodium_alloy", true);

    /** Fattore di danno tra un tier e il successivo (+25%), vedi la tabella sopra. */
    public static final float DAMAGE_FACTOR = 1.25F;

    private final String idPrefix;
    private final float speed;
    private final float attackDamageBonus;
    private final int enchantmentValue;
    private final TagKey<Block> incorrectBlocks;
    private final TagKey<Item> repairTag;
    private final boolean freeAoE;

    SATier(String idPrefix, float speed, float attackDamageBonus, int enchantmentValue, String incorrectBlocks, String repairTag,
           boolean freeAoE) {
        this.idPrefix = idPrefix;
        this.speed = speed;
        this.attackDamageBonus = attackDamageBonus;
        this.enchantmentValue = enchantmentValue;
        this.incorrectBlocks = BlockTags.create(ResourceLocation.parse(incorrectBlocks));
        this.repairTag = ItemTags.create(ResourceLocation.parse(repairTag));
        this.freeAoE = freeAoE;
    }

    /** Prefisso degli id degli oggetti di questo tier (il nome del tier), es. "trichromatic" → trichromatic_axe. */
    public String idPrefix() {
        return idPrefix;
    }

    /**
     * Lo scavo ad area della workstaff di questo tier non costa Ink (vero solo per ALLOY). Per T6, T7 e T8 si paga in
     * White Ink (SATools.Workstaff.aoeInkCost).
     */
    public boolean freeAoE() {
        return freeAoE;
    }

    /** Nominale: gli oggetti sono indistruttibili (componente unbreakable), come la bedrock di Spectrum. */
    @Override
    public int getUses() {
        return Integer.MAX_VALUE;
    }

    @Override
    public float getSpeed() {
        return speed;
    }

    @Override
    public float getAttackDamageBonus() {
        return attackDamageBonus;
    }

    @Override
    public TagKey<Block> getIncorrectBlocksForDrops() {
        return incorrectBlocks;
    }

    @Override
    public int getEnchantmentValue() {
        return enchantmentValue;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(repairTag);
    }
}
