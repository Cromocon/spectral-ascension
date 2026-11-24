package cromocon.spectralascension.item;

import de.dafuqs.spectrum.api.ink.InkPowered;
import cromocon.spectralascension.menu.WorkstaffMenu;
import de.dafuqs.spectrum.items.tools.BedrockAxeItem;
import de.dafuqs.spectrum.items.tools.BedrockBowItem;
import de.dafuqs.spectrum.items.tools.BedrockFishingRodItem;
import de.dafuqs.spectrum.items.tools.BedrockHoeItem;
import de.dafuqs.spectrum.items.tools.BedrockShearsItem;
import de.dafuqs.spectrum.items.tools.BedrockShovelItem;
import de.dafuqs.spectrum.items.tools.BedrockSwordItem;
import de.dafuqs.spectrum.items.tools.GlassCrestCrossbowItem;
import de.dafuqs.spectrum.items.tools.GlassCrestGreatswordItem;
import de.dafuqs.spectrum.items.tools.GlassCrestWorkstaffItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

import java.util.List;
import java.util.Map;

/**
 * Attrezzi e armi del set base. Ognuno estende la versione bedrock di Spectrum, così eredita i suoi
 * comportamenti (es. l'arco con lo zoom, la canna che pesca nella lava) e cambia solo gli incantesimi
 * preimpostati, cioè quelli mostrati nel creativo.
 * Gli incantesimi che il giocatore riceve davvero li mette la ricetta (result.components.enchantments).
 */
public final class SATools {
    private SATools() {
    }

    /**
     * Workstaff: è il piccone di ogni tier. Estende la workstaff glass crest di Spectrum, così eredita il menu
     * (Shift + tasto destro: Silk Touch / Fortune / Resonance, area 1×1 / 3×3 / 5×5, azioni col tasto destro,
     * proiettili), lo scavo ad area e i proiettili. Velocità, danno e incantabilità vengono dal nostro tier.
     * <p>
     * Fortune è la selezione di default del menu. Il livello lo ricorda WorkstaffItem.applyToggle nel componente
     * spectrum:workstaff quando si passa a Silk Touch o Resonance: tornando a Fortune rimette lo stesso livello.
     * <p>
     * <b>Dal T7 il menu è il nostro</b> ({@link WorkstaffMenu}, schermata SAWorkstaffScreen, voci {@link WorkstaffOption}):
     * lo stesso di Spectrum più l'area 7×7 (T7+) e 9×9 (Alloy) e la sotto-griglia "Drop" con Foundry (T7+) e Inventory
     * Insertion (T8+). Il T6 tiene il menu di Spectrum. L'area massima per tier ({@link #maxAoERange}) vale anche lato
     * server ({@link #getAoERange}): uno stack con spectrum:aoe troppo grande scava solo l'area del suo tier.
     */
    public static final class Workstaff extends GlassCrestWorkstaffItem {
        private final Map<ResourceKey<Enchantment>, Integer> enchantments;
        private final SATier tier;

        /**
         * SOLO PER I GAMETEST: se non è null, sostituisce {@link SATier#freeAoE()} (per provare l'area gratis su un
         * tier a pagamento, o viceversa). Il flag vero è in SATier.ALLOY (workstaff Prismatic).
         */
        public static volatile Boolean freeAoEOverrideForTests;

        public Workstaff(SATier tier, int attackDamage, float attackSpeed, Properties properties,
                         Map<ResourceKey<Enchantment>, Integer> enchantments) {
            super(tier, attackDamage, attackSpeed, properties);
            this.tier = tier;
            this.enchantments = Map.copyOf(enchantments);
        }

        public SATier tier() {
            return tier;
        }

        /** Dal T7 Shift + tasto destro apre il nostro menu (WorkstaffMenu) invece di quello di Spectrum. */
        public boolean hasOwnMenu() {
            return tier.ordinal() >= SATier.T7.ordinal();
        }

        /** Raggio massimo dell'area per tier: T6 2 (5×5), T7 e T8 3 (7×7), Alloy 4 (9×9). */
        public int maxAoERange() {
            return switch (tier) {
                case T6 -> 2;
                case T7, T8 -> 3;
                case ALLOY -> 4;
            };
        }

        /**
         * spectrum:aoe limitato al massimo del tier: lo usano lo scavo (AoEBreakingTool.afterBreakingBlock), il costo
         * (canUseAoE) e il tooltip di Spectrum, quindi un componente troppo grande (comando, client modificato, altra
         * mod) scava e costa come l'area massima del tier.
         */
        @Override
        public int getAoERange(ItemStack stack) {
            return Math.max(0, Math.min(super.getAoERange(stack), maxAoERange()));
        }

        /**
         * Shift + tasto destro: dal T7 apre il nostro menu (sul server, con la mano da cui è stato aperto). Senza Shift,
         * o col T6, è il comportamento di Spectrum (menu di Spectrum, azioni col tasto destro, proiettili).
         */
        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
            if (hasOwnMenu() && user.isShiftKeyDown()) {
                if (user instanceof ServerPlayer serverPlayer) {
                    WorkstaffMenu.open(serverPlayer, hand);
                }
                return InteractionResultHolder.consume(user.getItemInHand(hand));
            }
            return super.use(level, user, hand);
        }

        /** L'area di questo tier è gratis (SATier.freeAoE: solo Alloy). */
        public boolean hasFreeAoE() {
            Boolean override = freeAoEOverrideForTests;
            return override != null ? override : tier.freeAoE();
        }

        /** La workstaff di Spectrum esce con Fortune IV: la nostra con gli incantesimi del tier. */
        @Override
        public Map<ResourceKey<Enchantment>, Integer> getDefaultEnchantments() {
            return enchantments;
        }

        /**
         * Scavo ad area. Spectrum lo chiama da SpectrumEventListeners.onBlockBreak (BlockEvent.BreakEvent) per ogni
         * blocco rotto con la workstaff in mano, e AoEHelper rompe i blocchi dell'area con gameMode.destroyBlock, che
         * lancia di nuovo BreakEvent per OGNUNO di essi. Il secondo giro non scava (AoEHelper ha una guardia), ma
         * WorkstaffItem.canUseAoE nel frattempo ha già tolto l'inchiostro: in sopravvivenza un colpo 3×3 costava
         * 3 + 8×3 = 27 White Ink invece di 3, un 5×5 9 + 24×9 = 225 invece di 9 (in creativo l'inchiostro è gratis e
         * non si vedeva). Qui i blocchi rotti dall'area stessa non passano più da canUseAoE.
         */
        @Override
        public void afterBreakingBlock(LevelAccessor level, BlockPos pos, Player player, ItemStack stack) {
            if (breakingArea) {
                return;
            }
            breakingArea = true;
            try {
                super.afterBreakingBlock(level, pos, player, stack);
            } finally {
                breakingArea = false;
            }
        }

        /**
         * In sopravvivenza l'area si paga in White Ink per blocco rotto a mano ({@link #aoeInkCost}: 3 per il 3×3, 9 per
         * il 5×5 e per il 7×7) e richiede l'Ink sbloccato (spectrum:milestones/unlock_ink_use); in creativo è gratis.
         * Non chiama più WorkstaffItem.canUseAoE, che costerebbe 3^raggio (27 per il 7×7). Senza inchiostro Spectrum
         * scava un solo blocco senza dire niente: qui lo dice sopra la hotbar.
         */
        @Override
        public boolean canUseAoE(Player player, ItemStack stack) {
            int range = getAoERange(stack);
            if (range <= 0) {
                return false;
            }
            if (hasFreeAoE()) {
                // Alloy: nessun Ink e nessuno sblocco dell'Ink richiesto, basta aver scelto un'area (3×3 ... 9×9).
                return true;
            }
            boolean ok = InkPowered.tryDrainEnergy(player, BASE_COST_PER_AOE_MINING_RANGE_INCREMENT.color(), aoeInkCost(range));
            if (!ok && player instanceof ServerPlayer serverPlayer) {
                // InkPowered.tryDrainEnergy fallisce anche con la palette piena se manca unlock_ink_use: diciamo quale dei due.
                String key = InkPowered.canUse(serverPlayer) ? "aoe_no_ink" : "aoe_ink_locked";
                serverPlayer.displayClientMessage(Component.translatable("item.spectral_ascension.workstaff." + key,
                        aoeInkCost(range)).withStyle(ChatFormatting.RED), true);
            }
            return ok;
        }

        /**
         * White Ink per blocco rotto a mano con l'area di raggio {@code range}: 3^raggio come Spectrum fino al 5×5
         * (3 e 9), poi fermo a 9 (decisione dell'utente: il 7×7 del T7/T8 costa come il 5×5; il 9×9 c'è solo
         * sull'Alloy, gratis).
         */
        public static long aoeInkCost(int range) {
            return (long) Math.pow(BASE_COST_PER_AOE_MINING_RANGE_INCREMENT.amount(), Math.min(range, 2));
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            super.appendHoverText(stack, context, tooltip, flag);
            int range = getAoERange(stack);
            if (range > 0) {
                tooltip.add(hasFreeAoE()
                        ? Component.translatable("item.spectral_ascension.workstaff.aoe_free").withStyle(ChatFormatting.GRAY)
                        : Component.translatable("item.spectral_ascension.workstaff.aoe_cost", aoeInkCost(range))
                        .withStyle(ChatFormatting.GRAY));
            }
            // Interruttori del menu (T7+): Foundry sospesa da Silk Touch o spenta, Inventory Insertion spenta.
            if (hasOwnMenu()) {
                WorkstaffDrops drops = WorkstaffOption.drops(stack);
                if (WorkstaffOption.foundryPaused(stack)) {
                    tooltip.add(Component.translatable("item.spectral_ascension.workstaff.foundry_paused").withStyle(ChatFormatting.GRAY));
                } else if (!drops.foundry()) {
                    tooltip.add(Component.translatable("item.spectral_ascension.workstaff.foundry_off").withStyle(ChatFormatting.DARK_GRAY));
                }
                if (tier.ordinal() >= SATier.T8.ordinal() && !drops.insertion()) {
                    tooltip.add(Component.translatable("item.spectral_ascension.workstaff.insertion_off").withStyle(ChatFormatting.DARK_GRAY));
                }
            }
        }

        /** Siamo dentro lo scavo ad area (solo thread del server: gli eventi di rottura arrivano tutti da lì). */
        private static boolean breakingArea;
    }

    /**
     * Ultra greatsword: estende quella glass crest di Spectrum, così eredita portata extra, danno in parte magico
     * (25%) e il ground slam col tasto destro tenuto premuto (forza = livello di Sweeping Edge, costa Ink bianco).
     * Danno e incantabilità vengono dal nostro tier; danno extra, velocità e portata sono parametri espliciti.
     */
    public static final class UltraGreatsword extends GlassCrestGreatswordItem {
        private final Map<ResourceKey<Enchantment>, Integer> enchantments;

        public UltraGreatsword(SATier tier, int attackDamage, float attackSpeed, float extraReach, Properties properties,
                               Map<ResourceKey<Enchantment>, Integer> enchantments) {
            super(tier, attackDamage, attackSpeed, extraReach, properties);
            this.enchantments = Map.copyOf(enchantments);
        }

        /** Spectrum la fa uscire con Sweeping Edge IV (GreatswordItem): la nostra con gli incantesimi del tier. */
        @Override
        public Map<ResourceKey<Enchantment>, Integer> getDefaultEnchantments() {
            return enchantments;
        }
    }

    /**
     * Balestra: estende quella glass crest di Spectrum, così eredita l'overcharge (Shift + tasto destro con la balestra
     * carica, Ink bianco; gli effetti al tiro scattano in SpectrumEventListeners con instanceof GlassCrestCrossbowItem),
     * le frecce di vetro e i modificatori della libreria Arrowhead, che qui sono più alti (valori provvisori).
     */
    public static final class Crossbow extends GlassCrestCrossbowItem {
        /** Valori della glass crest: si scalano questi per tenere l'overcharge (che li modifica a sua volta). */
        private static final float GLASS_CREST_VELOCITY = 1.5F;
        private static final float GLASS_CREST_DIVERGENCE = 0.75F;

        private final SATier tier;
        /** Velocità della freccia (glass crest 1,5; T6 2,0). */
        private final float velocity;
        /** Tempo di carica, moltiplica la durata vanilla: più basso = più veloce (glass crest 2,0; T6 1,6). */
        private final float pullTime;
        /** Divergenza, più basso = più preciso (glass crest 0,75; T6 0,63). */
        private final float divergence;
        private final Map<ResourceKey<Enchantment>, Integer> enchantments;

        public Crossbow(SATier tier, float velocity, float pullTime, float divergence, Properties properties,
                        Map<ResourceKey<Enchantment>, Integer> enchantments) {
            super(properties);
            this.tier = tier;
            this.velocity = velocity;
            this.pullTime = pullTime;
            this.divergence = divergence;
            this.enchantments = Map.copyOf(enchantments);
        }

        @Override
        public Map<ResourceKey<Enchantment>, Integer> getDefaultEnchantments() {
            return enchantments;
        }

        @Override
        public int getEnchantmentValue(ItemStack stack) {
            return tier.getEnchantmentValue();
        }

        /** Indistruttibile: MalachiteCrossbowItem accetterebbe la malachite pura (hardcoded), qui niente riparazione. */
        @Override
        public boolean isValidRepairItem(ItemStack stack, ItemStack ingredient) {
            return false;
        }

        @Override
        public float getProjectileVelocityModifier(ItemStack stack, LivingEntity shooter) {
            return super.getProjectileVelocityModifier(stack, shooter) * (velocity / GLASS_CREST_VELOCITY);
        }

        @Override
        public float getPullTimeModifier(ItemStack stack, LivingEntity shooter) {
            return pullTime;
        }

        @Override
        public float getDivergenceMod(ItemStack stack, LivingEntity shooter) {
            return super.getDivergenceMod(stack, shooter) * (divergence / GLASS_CREST_DIVERGENCE);
        }
    }

    public static final class Axe extends BedrockAxeItem {
        private final Map<ResourceKey<Enchantment>, Integer> enchantments;

        public Axe(SATier tier, Properties properties, Map<ResourceKey<Enchantment>, Integer> enchantments) {
            super(tier, properties);
            this.enchantments = Map.copyOf(enchantments);
        }

        @Override
        public Map<ResourceKey<Enchantment>, Integer> getDefaultEnchantments() {
            return enchantments;
        }
    }

    public static final class Shovel extends BedrockShovelItem {
        private final Map<ResourceKey<Enchantment>, Integer> enchantments;

        public Shovel(SATier tier, Properties properties, Map<ResourceKey<Enchantment>, Integer> enchantments) {
            super(tier, properties);
            this.enchantments = Map.copyOf(enchantments);
        }

        @Override
        public Map<ResourceKey<Enchantment>, Integer> getDefaultEnchantments() {
            return enchantments;
        }
    }

    public static final class Hoe extends BedrockHoeItem {
        private final Map<ResourceKey<Enchantment>, Integer> enchantments;

        public Hoe(SATier tier, Properties properties, Map<ResourceKey<Enchantment>, Integer> enchantments) {
            super(tier, properties);
            this.enchantments = Map.copyOf(enchantments);
        }

        @Override
        public Map<ResourceKey<Enchantment>, Integer> getDefaultEnchantments() {
            return enchantments;
        }
    }

    public static final class Sword extends BedrockSwordItem {
        private final Map<ResourceKey<Enchantment>, Integer> enchantments;

        public Sword(SATier tier, Properties properties, Map<ResourceKey<Enchantment>, Integer> enchantments) {
            super(tier, properties);
            this.enchantments = Map.copyOf(enchantments);
        }

        @Override
        public Map<ResourceKey<Enchantment>, Integer> getDefaultEnchantments() {
            return enchantments;
        }
    }

    /** Arco: eredita da quello bedrock zoom, velocità della freccia e precisione maggiori (libreria Arrowhead). */
    public static final class Bow extends BedrockBowItem {
        private final SATier tier;
        private final Map<ResourceKey<Enchantment>, Integer> enchantments;

        public Bow(SATier tier, Properties properties, Map<ResourceKey<Enchantment>, Integer> enchantments) {
            super(properties);
            this.tier = tier;
            this.enchantments = Map.copyOf(enchantments);
        }

        @Override
        public Map<ResourceKey<Enchantment>, Integer> getDefaultEnchantments() {
            return enchantments;
        }

        @Override
        public int getEnchantmentValue(ItemStack stack) {
            return tier.getEnchantmentValue();
        }
    }

    /** Canna da pesca: eredita da quella bedrock la pesca nella lava e l'amo di bedrock. */
    public static final class FishingRod extends BedrockFishingRodItem {
        private final SATier tier;
        private final Map<ResourceKey<Enchantment>, Integer> enchantments;

        public FishingRod(SATier tier, Properties properties, Map<ResourceKey<Enchantment>, Integer> enchantments) {
            super(properties);
            this.tier = tier;
            this.enchantments = Map.copyOf(enchantments);
        }

        @Override
        public Map<ResourceKey<Enchantment>, Integer> getDefaultEnchantments() {
            return enchantments;
        }

        @Override
        public int getEnchantmentValue(ItemStack stack) {
            return tier.getEnchantmentValue();
        }
    }

    public static final class Shears extends BedrockShearsItem {
        private final SATier tier;
        private final Map<ResourceKey<Enchantment>, Integer> enchantments;

        public Shears(SATier tier, Properties properties, Map<ResourceKey<Enchantment>, Integer> enchantments) {
            super(properties);
            this.tier = tier;
            this.enchantments = Map.copyOf(enchantments);
        }

        @Override
        public Map<ResourceKey<Enchantment>, Integer> getDefaultEnchantments() {
            return enchantments;
        }

        /** La versione bedrock restituisce l'incantabilità della bedrock: qui usiamo quella del nostro tier. */
        @Override
        public int getEnchantmentValue(ItemStack stack) {
            return tier.getEnchantmentValue();
        }
    }
}
