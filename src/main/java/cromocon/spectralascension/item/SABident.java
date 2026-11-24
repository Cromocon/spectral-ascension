package cromocon.spectralascension.item;

import cromocon.spectralascension.SpectralAscension;
import cromocon.spectralascension.menu.BidentMenu;
import cromocon.spectralascension.registry.SAComponents;
import de.dafuqs.revelationary.api.advancements.AdvancementHelper;
import de.dafuqs.spectrum.api.ink.InkAmount;
import de.dafuqs.spectrum.api.ink.InkPowered;
import de.dafuqs.spectrum.api.ink.color.InkColor;
import de.dafuqs.spectrum.api.ink.color.InkColors;
import de.dafuqs.spectrum.api.render.SlotBackgroundEffectProvider;
import de.dafuqs.spectrum.helpers.SpectrumEnchantmentHelper;
import de.dafuqs.spectrum.items.tools.FerociousBidentItem;
import de.dafuqs.spectrum.items.tools.FractalBidentItem;
import de.dafuqs.spectrum.items.tools.MalachiteBidentItem;
import de.dafuqs.spectrum.registries.SpectrumAdvancements;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Bident tricromatico: fonde il Ferocious e il Fractal glass crest di Spectrum in un solo oggetto con tre modalità
 * (componente {@link SAComponents#BIDENT_MODE}): spento, Ferocious, Fractal. Shift + tasto destro apre un menu per sceglierla
 * ({@link BidentMenu}), come la workstaff di Spectrum.
 * <p>
 * Estende {@link MalachiteBidentItem} (lancio come BidentEntity, perforazione armatura, incantesimi extra da tridente)
 * con {@code hasActiveAbilities = false}: così la classe di Spectrum non usa il proprio interruttore on/off
 * (componente spectrum:activated) e non aggiunge le sue righe "abilitato/disabilitato". Le abilità dei due glass crest
 * sono riscritte qui, uguali agli originali (FerociousBidentItem, FractalBidentItem) ma attive solo nella loro modalità.
 * Estendere uno dei due glass crest avrebbe ereditato metà del comportamento legato a spectrum:activated e i suoi
 * tooltip, che non si possono togliere.
 * <p>
 * Incantesimi come la workstaff: ogni modalità ha il suo set, che si scambia cambiando modalità (vedi
 * {@link BidentEnchantments}). Da spento restano solo gli incantesimi di base (Impaling X e ciò che si aggiunge da
 * spento): è un tridente normale.
 */
public class SABident extends MalachiteBidentItem implements SlotBackgroundEffectProvider, InkPowered {
    /** Costi e valori dei glass crest di Spectrum (costanti pubbliche delle loro classi). */
    private static final InkAmount RIPTIDE_COST = FerociousBidentItem.RIPTIDE_COST;
    private static final InkAmount MIRROR_IMAGE_COST = FractalBidentItem.MIRROR_IMAGE_COST;
    private static final float FRACTAL_THROW_SPEED = 5.0F;

    private final SATier tier;
    private final Map<ResourceKey<Enchantment>, Integer> enchantments;

    public SABident(SATier tier, Properties properties, double attackSpeed, double damage, float armorPierce, float protPierce,
                    Map<ResourceKey<Enchantment>, Integer> enchantments) {
        super(properties, attackSpeed, damage, armorPierce, protPierce, false);
        this.tier = tier;
        this.enchantments = Map.copyOf(enchantments);
    }

    public static BidentMode getMode(ItemStack stack) {
        return stack.getOrDefault(SAComponents.BIDENT_MODE.get(), BidentMode.OFF);
    }

    /** Cambia solo la modalità, senza scambiare gli incantesimi (per quello c'è {@link #applyMode}). */
    public static void setMode(ItemStack stack, BidentMode mode) {
        stack.set(SAComponents.BIDENT_MODE.get(), mode);
    }

    @Override
    public Map<ResourceKey<Enchantment>, Integer> getDefaultEnchantments() {
        return enchantments;
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return tier.getEnchantmentValue();
    }

    /** Indistruttibile: la classe di Spectrum accetterebbe la malachite pura (hardcoded), qui niente riparazione. */
    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack ingredient) {
        return false;
    }

    /**
     * Shift + tasto destro: apre il menu di selezione della modalità ({@link BidentMenu}), come la workstaff di
     * Spectrum. Ritorna "consume" su entrambi i lati senza chiamare super.use: il lancio e Riptide non partono (non
     * inizia l'uso dell'oggetto). Senza Shift si carica il lancio come sempre.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (player instanceof ServerPlayer serverPlayer) {
                BidentMenu.open(serverPlayer, hand);
            }
            return InteractionResultHolder.consume(stack);
        }
        return super.use(level, player, hand);
    }

    /**
     * Scelta dal menu, sul server (la chiama {@link BidentMenu#select} quando arriva BidentModePayload). Controlla che
     * nella mano ci sia un nostro bident e che l'indice sia una modalità, poi applica la modalità con
     * {@link #applyMode} e la mostra sopra la hotbar. Ritorna false (senza cambiare niente) se la richiesta non è valida.
     */
    public static boolean selectMode(Player player, InteractionHand hand, int modeIndex) {
        BidentMode mode = BidentMode.byIndex(modeIndex);
        ItemStack stack = player.getItemInHand(hand);
        if (mode == null || !(stack.getItem() instanceof SABident)) {
            return false;
        }
        applyMode(stack, player, mode);
        player.displayClientMessage(modeLine(mode), true);
        return true;
    }

    /**
     * Passa alla modalità indicata scambiando i set di incantesimi (se è già quella, non cambia niente). Sharpness e
     * Impaling (e ogni altra coppia in conflitto) possono stare insieme solo se il giocatore ha l'advancement di
     * Spectrum che lo permette all'Enchanter, come fa la workstaff; altrimenti l'incantesimo in conflitto resta
     * memorizzato ma non attivo. Solo sul server: lo scambio dipende dagli advancement del giocatore, il client riceve
     * lo stack aggiornato.
     */
    public static void applyMode(ItemStack stack, Player player, BidentMode mode) {
        BidentEnchantments.switchMode(stack, mode, player.level().registryAccess(), canApplyConflicting(player));
    }

    public static boolean canApplyConflicting(@Nullable Player player) {
        return player != null && AdvancementHelper.hasAdvancement(player, SpectrumAdvancements.APPLY_CONFLICTING_ENCHANTMENTS);
    }

    // ---------- Ferocious ----------

    /** In modalità Ferocious c'è sempre almeno Riptide I, come nel Ferocious glass crest. */
    @Override
    public int getRiptideLevel(HolderLookup.Provider lookup, ItemStack stack) {
        int level = SpectrumEnchantmentHelper.getLevel(lookup, Enchantments.RIPTIDE, stack);
        return getMode(stack) == BidentMode.FEROCIOUS ? Math.max(level, FerociousBidentItem.BUILTIN_RIPTIDE_LEVEL) : level;
    }

    /** Ferocious: Riptide anche fuori dall'acqua, pagando Ink bianco. Nelle altre modalità vale la regola normale. */
    @Override
    public boolean canStartRiptide(Player player, ItemStack stack) {
        if (getMode(stack) == BidentMode.FEROCIOUS) {
            return super.canStartRiptide(player, stack) || InkPowered.tryDrainEnergy(player, RIPTIDE_COST);
        }
        return super.canStartRiptide(player, stack);
    }

    /** Copia di FerociousBidentItem.onUseTick: durante lo spin paga Ink ogni 10 tick e ferisce chi è vicino. */
    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        super.onUseTick(level, user, stack, remainingUseTicks);
        if (getMode(stack) != BidentMode.FEROCIOUS || !user.isAutoSpinAttack() || !(user instanceof Player player)) {
            return;
        }
        int useTime = this.getUseDuration(stack, user) - remainingUseTicks;
        if (useTime % 10 == 0) {
            if (InkPowered.tryDrainEnergy(player, RIPTIDE_COST)) {
                stack.hurtAndBreak(1, user, LivingEntity.getSlotForHand(user.getUsedItemHand()));
            } else {
                user.releaseUsingItem();
                return;
            }
        }
        yeetPlayer(player, getRiptideLevel(level.registryAccess(), stack) / 128F - 0.75F);
        player.startAutoSpinAttack(20, 12.0F, stack);
        for (LivingEntity around : level.getEntities(EntityTypeTest.forClass(LivingEntity.class), player.getBoundingBox().inflate(2), LivingEntity::isAlive)) {
            if (around != player) {
                around.hurt(level.damageSources().playerAttack(player), 2);
            }
        }
    }

    // ---------- Fractal ----------

    /** Fractal: lancia una copia illusoria pagando Ink bianco, e il bident resta in mano. */
    @Override
    public boolean isThrownAsMirrorImage(ItemStack stack, ServerLevel level, Player player) {
        return getMode(stack) == BidentMode.FRACTAL && InkPowered.tryDrainEnergy(player, MIRROR_IMAGE_COST);
    }

    @Override
    public float getThrowSpeed(ItemStack stack) {
        return getMode(stack) == BidentMode.FRACTAL ? FRACTAL_THROW_SPEED : super.getThrowSpeed(stack);
    }

    /** Unione dei due glass crest: quelli del bident di malachite più Efficiency e Power del Fractal. */
    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        return super.supportsEnchantment(stack, enchantment) || enchantment.is(Enchantments.EFFICIENCY) || enchantment.is(Enchantments.POWER);
    }

    // ---------- Ink, bordo dello slot, tooltip ----------

    @Override
    public List<InkColor> getUsedColors(ItemStack stack) {
        return List.of(InkColors.WHITE);
    }

    @Override
    public SlotEffect backgroundType(@Nullable Player player, ItemStack stack) {
        InkAmount cost = switch (getMode(stack)) {
            case OFF -> null;
            case FEROCIOUS -> RIPTIDE_COST;
            case FRACTAL -> MIRROR_IMAGE_COST;
        };
        return cost != null && InkPowered.hasAvailableInk(player, cost) ? SlotEffect.BORDER_FADE : SlotEffect.NONE;
    }

    /** Arancione come il Ferocious, viola come il Fractal. */
    @Override
    public int getBackgroundColor(@Nullable Player player, ItemStack stack, float tickDelta) {
        return getMode(stack) == BidentMode.FRACTAL ? InkColors.PURPLE_COLOR : InkColors.ORANGE_COLOR;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        BidentMode mode = getMode(stack);
        tooltip.add(modeLine(mode));
        // Le righe descrittive sono quelle dei glass crest di Spectrum (già tradotte).
        String spectrumKey = switch (mode) {
            case OFF -> null;
            case FEROCIOUS -> "item.spectrum.ferocious_glass_crest_bident.tooltip";
            case FRACTAL -> "item.spectrum.fractal_glass_crest_bident.tooltip";
        };
        if (spectrumKey != null) {
            for (String suffix : List.of("", "2", "3")) {
                tooltip.add(Component.translatable(spectrumKey + suffix).withStyle(ChatFormatting.GRAY));
            }
        }
        // Incantesimi memorizzati: il set delle altre modalità, e quelli del set attivo non attivi (in conflitto).
        BidentEnchantments stored = stack.get(SAComponents.BIDENT_ENCHANTMENTS.get());
        if (stored != null) {
            for (BidentMode other : List.of(BidentMode.FEROCIOUS, BidentMode.FRACTAL)) {
                ItemEnchantments set = other == mode ? stored.inactive(mode) : stored.set(other);
                if (set.isEmpty()) {
                    continue;
                }
                String key = other == mode ? "inactive" : "stored";
                tooltip.add(Component.translatable("item." + SpectralAscension.MOD_ID + ".bident." + key,
                                Component.translatable("item." + SpectralAscension.MOD_ID + ".bident.mode." + other.getSerializedName())
                                        .withStyle(other.color()),
                                enchantmentList(set))
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        tooltip.add(Component.translatable("item." + SpectralAscension.MOD_ID + ".bident.switch_hint")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        // addInkPoweredTooltip è solo client (@OnlyIn): sul server dedicato non esiste e darebbe NoSuchMethodError se
        // qualche mod chiede il tooltip lì (GameTest SAEdgeCaseTests.dedicated_server).
        if (mode != BidentMode.OFF && FMLEnvironment.dist.isClient()) {
            addInkPoweredTooltip(stack, tooltip);
        }
    }

    /** "Looting III, Riptide II": nomi degli incantesimi col livello, separati da virgole. */
    private static Component enchantmentList(ItemEnchantments enchantments) {
        MutableComponent list = Component.empty();
        boolean first = true;
        for (Object2IntMap.Entry<Holder<Enchantment>> e : enchantments.entrySet()) {
            if (!first) {
                list.append(", ");
            }
            list.append(Enchantment.getFullname(e.getKey(), e.getIntValue()));
            first = false;
        }
        return list;
    }

    /** "Modalità: Ferocious", col colore della modalità. Usata dal tooltip e dal messaggio sopra la hotbar. */
    public static Component modeLine(BidentMode mode) {
        return Component.translatable("item." + SpectralAscension.MOD_ID + ".bident.mode",
                        Component.translatable("item." + SpectralAscension.MOD_ID + ".bident.mode." + mode.getSerializedName())
                                .withStyle(mode.color()))
                .withStyle(ChatFormatting.GRAY);
    }
}
