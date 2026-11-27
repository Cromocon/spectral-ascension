package cromocon.spectralascension.item;

import net.minecraft.core.Holder;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.neoforge.event.GrindstoneEvent;

/**
 * Wind Burst intrinseco delle nostre mazze (2026-11-27, {@link SATools.Mace#ensureWindBurst}): la mazza lo riavrebbe
 * comunque alla prima copia, quindi mola e incudine lo trattano come parte dell'oggetto, non come incantesimo tolto.
 * <ul>
 *   <li>Mola: il risultato tiene Wind Burst al livello intrinseco ({@code GrindstoneMenuMixin}); l'XP non conta quel
 *       livello ({@link #onGrindstoneTake}), altrimenti mola + rigenerazione = XP infinita.</li>
 *   <li>Incudine, libro estratto da una nostra mazza (Extraction Tome di Apothic, {@link SAEnchantCap#onAnvilUpdate}):
 *       Wind Burst entra nel libro solo se supera il livello intrinseco di almeno 2 ({@link #extractable}), altrimenti
 *       libri Wind Burst infiniti o la scalata di un livello alla volta.</li>
 * </ul>
 * Il tetto ATM10 (Wind Burst VIII) vale per i libri, come per tutti gli altri incantesimi; la mazza T9 resta a XI.
 * <p>
 * L'Enchanter di Spectrum con un Gilded Book NON passa da {@link #extractable}, di proposito (decisione utente
 * 2026-11-27): i Gilded Book non si craftano, si trovano soltanto, e la copia è comunque tagliata al tetto ATM10
 * ({@code SpectrumEnchantmentHelperMixin}), quindi da una mazza T9 esce Wind Burst VIII, mai XI.
 */
public final class MaceWindBurst {
    private MaceWindBurst() {
    }

    private static int intrinsic(ItemStack stack) {
        return stack.getItem() instanceof SATools.Mace mace ? mace.intrinsicWindBurst() : 0;
    }

    /** Risultato della mola: se è una nostra mazza, Wind Burst torna al livello intrinseco (stesso Holder dell'input). */
    public static void keepOnGrindstone(ItemStack result, ItemStack top, ItemStack bottom) {
        int want = intrinsic(result);
        if (want <= 0) return;
        Holder<Enchantment> wind = find(top);
        if (wind == null) wind = find(bottom);
        if (wind == null) return;
        ItemEnchantments.Mutable fixed = new ItemEnchantments.Mutable(EnchantmentHelper.getEnchantmentsForCrafting(result));
        if (fixed.getLevel(wind) >= want) return;
        fixed.set(wind, want);
        EnchantmentHelper.setEnchantments(result, fixed.toImmutable());
    }

    private static Holder<Enchantment> find(ItemStack stack) {
        for (var e : EnchantmentHelper.getEnchantmentsForCrafting(stack).entrySet()) {
            if (e.getKey().is(Enchantments.WIND_BURST)) return e.getKey();
        }
        return null;
    }

    /**
     * XP della mola ricalcolata come la vanilla (GrindstoneMenu: somma dei getMinCost degli incantesimi non maledizione,
     * poi metà + caso fino all'altra metà), ma per le nostre mazze Wind Burst vale solo la parte sopra il livello
     * intrinseco. Se nessun input è una nostra mazza non si tocca niente.
     */
    public static void onGrindstoneTake(GrindstoneEvent.OnTakeItem event) {
        ItemStack top = event.getTopItem(), bottom = event.getBottomItem();
        if (intrinsic(top) <= 0 && intrinsic(bottom) <= 0) return;
        int total = xpFrom(top) + xpFrom(bottom);
        RandomSource random = event.getPlayer() != null ? event.getPlayer().getRandom() : RandomSource.create();
        int half = (int) Math.ceil(total / 2.0);
        event.setXp(total > 0 ? half + random.nextInt(half) : 0);
    }

    private static int xpFrom(ItemStack stack) {
        int base = intrinsic(stack), xp = 0;
        for (var e : EnchantmentHelper.getEnchantmentsForCrafting(stack).entrySet()) {
            Holder<Enchantment> holder = e.getKey();
            if (holder.is(EnchantmentTags.CURSE)) continue;
            int cost = holder.value().getMinCost(e.getIntValue());
            if (base > 0 && holder.is(Enchantments.WIND_BURST)) {
                cost = e.getIntValue() > base ? Math.max(0, cost - holder.value().getMinCost(base)) : 0;
            }
            xp += cost;
        }
        return xp;
    }

    /**
     * Libro estratto da una nostra mazza: Wind Burst resta solo se il livello della mazza supera quello intrinseco + 1
     * (es. una mazza di bedrock, IV, portata a VI dà un libro VI; a V non dà Wind Burst, come una appena craftata).
     * Con «supera l'intrinseco» bastava: mazza T6 (V) + libro V = VI, Extraction Tome → libro VI, e così via un livello
     * alla volta.
     * Restituisce la stessa istanza se non c'è niente da togliere.
     */
    public static ItemEnchantments extractable(ItemStack source, ItemEnchantments stored) {
        int base = intrinsic(source);
        if (base <= 0) return stored;
        int onMace = 0;
        for (var e : EnchantmentHelper.getEnchantmentsForCrafting(source).entrySet()) {
            if (e.getKey().is(Enchantments.WIND_BURST)) onMace = e.getIntValue();
        }
        if (onMace > base + 1) return stored;
        ItemEnchantments.Mutable out = null;
        for (var e : stored.entrySet()) {
            if (e.getKey().is(Enchantments.WIND_BURST)) {
                out = new ItemEnchantments.Mutable(stored);
                out.set(e.getKey(), 0);
            }
        }
        return out == null ? stored : out.toImmutable();
    }
}
