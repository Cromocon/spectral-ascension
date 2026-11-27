package cromocon.spectralascension.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cromocon.spectralascension.registry.SAComponents;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * Incantesimi "memorizzati" del bident tricromatico (componente spectral_ascension:bident_enchantments), gestiti
 * come la workstaff di Spectrum che ricorda il livello di Fortune quando passi a Silk Touch.
 * <p>
 * Il componente minecraft:enchantments contiene sempre gli incantesimi <b>attivi</b>; qui c'è il resto:
 * <ul>
 *   <li>{@code base}: gli incantesimi dello stato spento (di partenza l'Impaling del tier), attivi in tutte le modalità.
 *       Da spento la base è proprio minecraft:enchantments, quindi qui è vuota.</li>
 *   <li>{@code ferocious}, {@code fractal}: il set completo di ogni modalità, sempre memorizzato. Entrando in una
 *       modalità si applica sopra la base (stesso incantesimo: vince il livello più alto); un incantesimo in conflitto
 *       senza l'advancement di Spectrum resta nel set ma non attivo.</li>
 *   <li>{@code applied}: gli incantesimi scritti entrando nella modalità attuale (vuoto da spento). Uscendo si
 *       confrontano con quelli attuali per capire cosa ha fatto il giocatore nel frattempo.</li>
 * </ul>
 * Componente assente = base dell'oggetto (Impaling del tier), set vuoti, applicati = base: come un bident appena creato.
 */
public record BidentEnchantments(ItemEnchantments base, ItemEnchantments ferocious, ItemEnchantments fractal,
                                 ItemEnchantments applied) {
    /** Formato su disco: quattro liste di incantesimi, come minecraft:enchantments. */
    public static final Codec<BidentEnchantments> CODEC = RecordCodecBuilder.create(i -> i.group(
            ItemEnchantments.CODEC.optionalFieldOf("base", ItemEnchantments.EMPTY).forGetter(BidentEnchantments::base),
            ItemEnchantments.CODEC.optionalFieldOf("ferocious", ItemEnchantments.EMPTY).forGetter(BidentEnchantments::ferocious),
            ItemEnchantments.CODEC.optionalFieldOf("fractal", ItemEnchantments.EMPTY).forGetter(BidentEnchantments::fractal),
            ItemEnchantments.CODEC.optionalFieldOf("applied", ItemEnchantments.EMPTY).forGetter(BidentEnchantments::applied)
    ).apply(i, BidentEnchantments::new));
    /** Formato in rete: serve al tooltip del client. */
    public static final StreamCodec<RegistryFriendlyByteBuf, BidentEnchantments> STREAM_CODEC = StreamCodec.composite(
            ItemEnchantments.STREAM_CODEC, BidentEnchantments::base,
            ItemEnchantments.STREAM_CODEC, BidentEnchantments::ferocious,
            ItemEnchantments.STREAM_CODEC, BidentEnchantments::fractal,
            ItemEnchantments.STREAM_CODEC, BidentEnchantments::applied,
            BidentEnchantments::new);

    /** Il set memorizzato di una modalità (vuoto per lo stato spento, che non ha un set). */
    public ItemEnchantments set(BidentMode mode) {
        return switch (mode) {
            case OFF -> ItemEnchantments.EMPTY;
            case FEROCIOUS -> ferocious;
            case FRACTAL -> fractal;
        };
    }

    /** Copia con il set di una modalità sostituito (per OFF non cambia niente). */
    public BidentEnchantments withSet(BidentMode mode, ItemEnchantments set) {
        return switch (mode) {
            case OFF -> this;
            case FEROCIOUS -> new BidentEnchantments(base, set, fractal, applied);
            case FRACTAL -> new BidentEnchantments(base, ferocious, set, applied);
        };
    }

    /** Un incantesimo del set era stato applicato entrando nella modalità (supera la base ed è finito sull'oggetto). */
    private boolean wasApplied(Holder<Enchantment> enchantment, int setLevel) {
        return setLevel > base.getLevel(enchantment) && applied.getLevel(enchantment) == setLevel;
    }

    /**
     * Gli incantesimi del set della modalità attiva che non sono attivi perché in conflitto (senza l'advancement):
     * il tooltip li mostra come "non attivi". Quelli non migliori della base non contano: c'è già la base.
     */
    public ItemEnchantments inactive(BidentMode active) {
        ItemEnchantments.Mutable out = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        for (Object2IntMap.Entry<Holder<Enchantment>> e : set(active).entrySet()) {
            if (e.getIntValue() > base.getLevel(e.getKey()) && !wasApplied(e.getKey(), e.getIntValue())) {
                out.set(e.getKey(), e.getIntValue());
            }
        }
        return out.toImmutable();
    }

    // ---------- Lettura e scrittura sullo stack ----------

    /** I dati dello stack; se il componente manca, la base (e gli applicati) sono quelli dell'oggetto (Impaling del tier). */
    public static BidentEnchantments get(ItemStack stack, HolderLookup.Provider lookup) {
        BidentEnchantments data = stack.get(SAComponents.BIDENT_ENCHANTMENTS.get());
        if (data != null) {
            return data;
        }
        ItemEnchantments base = defaultBase(stack, lookup);
        return new BidentEnchantments(base, ItemEnchantments.EMPTY, ItemEnchantments.EMPTY, base);
    }

    /** Gli incantesimi di base dell'oggetto (Preenchanted.getDefaultEnchantments: Impaling VIII per il T6). */
    public static ItemEnchantments defaultBase(ItemStack stack, HolderLookup.Provider lookup) {
        if (!(stack.getItem() instanceof SABident bident)) {
            return ItemEnchantments.EMPTY;
        }
        var registry = lookup.lookupOrThrow(Registries.ENCHANTMENT);
        ItemEnchantments.Mutable base = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        bident.getDefaultEnchantments().forEach((key, level) -> registry.get(key).ifPresent(e -> base.set(e, level)));
        return base.toImmutable();
    }

    private static ItemEnchantments active(ItemStack stack) {
        return stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
    }

    // ---------- Cambio di modalità ----------

    /**
     * Passa da una modalità all'altra scambiando gli incantesimi. Il chiamante decide {@code allowConflicts}
     * (regola di Spectrum: il giocatore ha l'advancement APPLY_CONFLICTING_ENCHANTMENTS).
     * <ol>
     *   <li>Si esce dalla modalità attuale. Da spento: la base sono gli incantesimi attivi (ciò che si aggiunge da
     *       spento diventa base). Da una modalità, confrontando gli attivi con quelli applicati all'ingresso:
     *       <ul>
     *         <li>invariato: base e set restano com'erano;</li>
     *         <li>tolto (mola): sparisce dalla base e, se era stato applicato dal set, anche dal set;</li>
     *         <li>potenziato o aggiunto: se l'incantesimo è nella base il nuovo livello va nella base (vale per
     *             tutte le modalità), altrimenti nel set della modalità.</li>
     *       </ul></li>
     *   <li>Si entra nella nuova: base + il suo set (stesso incantesimo: livello più alto); quelli in conflitto
     *       senza l'advancement restano nel set, non attivi.</li>
     * </ol>
     */
    public static void switchMode(ItemStack stack, BidentMode to, HolderLookup.Provider lookup, boolean allowConflicts) {
        BidentMode from = SABident.getMode(stack);
        if (from == to) {
            return;
        }
        ItemEnchantments current = active(stack);
        BidentEnchantments data = get(stack, lookup);

        // 1. Uscita dalla modalità attuale.
        ItemEnchantments base;
        if (from == BidentMode.OFF) {
            base = current;
        } else {
            ItemEnchantments oldSet = data.set(from);
            ItemEnchantments.Mutable newBase = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            ItemEnchantments.Mutable newSet = new ItemEnchantments.Mutable(oldSet);
            for (Object2IntMap.Entry<Holder<Enchantment>> e : data.base().entrySet()) {
                if (current.getLevel(e.getKey()) > 0) {
                    newBase.set(e.getKey(), e.getIntValue());
                }
            }
            for (Object2IntMap.Entry<Holder<Enchantment>> e : oldSet.entrySet()) {
                if (current.getLevel(e.getKey()) == 0 && data.wasApplied(e.getKey(), e.getIntValue())) {
                    newSet.set(e.getKey(), 0); // tolto dalla mola
                }
            }
            for (Object2IntMap.Entry<Holder<Enchantment>> e : current.entrySet()) {
                if (e.getIntValue() == data.applied().getLevel(e.getKey())) {
                    continue; // invariato
                }
                if (data.base().getLevel(e.getKey()) > 0) {
                    newBase.set(e.getKey(), e.getIntValue());
                } else {
                    newSet.set(e.getKey(), e.getIntValue());
                }
            }
            base = newBase.toImmutable();
            data = data.withSet(from, newSet.toImmutable());
        }

        // 2. Entrata nella nuova modalità.
        ItemEnchantments.Mutable result = new ItemEnchantments.Mutable(base);
        if (to != BidentMode.OFF) {
            for (Object2IntMap.Entry<Holder<Enchantment>> e : data.set(to).entrySet()) {
                tryApply(result, e.getKey(), e.getIntValue(), allowConflicts);
            }
        }
        ItemEnchantments active = result.toImmutable();
        boolean off = to == BidentMode.OFF;
        stack.set(DataComponents.ENCHANTMENTS, active);
        stack.set(SAComponents.BIDENT_ENCHANTMENTS.get(), new BidentEnchantments(off ? ItemEnchantments.EMPTY : base,
                data.ferocious(), data.fractal(), off ? ItemEnchantments.EMPTY : active));
        SABident.setMode(stack, to);
    }

    /**
     * Aggiunge un incantesimo se è migliore di quello presente e non è in conflitto con gli altri (a meno di
     * {@code allowConflicts}), come SpectrumEnchantmentHelper.addOrUpgradeEnchantment. Ritorna se l'ha aggiunto.
     */
    static boolean tryApply(ItemEnchantments.Mutable target, Holder<Enchantment> enchantment, int level, boolean allowConflicts) {
        if (level <= target.getLevel(enchantment)) {
            return false;
        }
        if (!allowConflicts) {
            for (Holder<Enchantment> other : target.keySet()) {
                if (!other.equals(enchantment) && !Enchantment.areCompatible(other, enchantment)) {
                    return false;
                }
            }
        }
        target.set(enchantment, level);
        return true;
    }

    // ---------- Fusione ----------

    /**
     * Incantesimi del bident uscito dalla Fusion Shrine (lo chiama FusionShrineRecipeMixin, solo per il nostro
     * bident): base dell'oggetto (Impaling del tier), set Ferocious dal primo ingrediente (attivo: il bident esce in modalità
     * Ferocious), set Fractal dal secondo (memorizzato). Come copy_components di Spectrum, un incantesimo entra solo
     * se il bident lo accetta, è migliore della base e non è in conflitto con la base e il resto del suo set; i
     * conflitti sono ammessi se il proprietario della Fusion Shrine ha l'advancement APPLY_CONFLICTING_ENCHANTMENTS.
     * Gli altri si perdono, come nella fusione di Spectrum.
     */
    public static ItemStack applyFusion(ItemStack result, ItemEnchantments ferociousInput, ItemEnchantments fractalInput,
                                        HolderLookup.Provider lookup, boolean allowConflicts) {
        ItemEnchantments base = defaultBase(result, lookup);
        ItemEnchantments ferocious = fusionSet(result, base, ferociousInput, allowConflicts);
        ItemEnchantments fractal = fusionSet(result, base, fractalInput, allowConflicts);
        ItemEnchantments.Mutable active = new ItemEnchantments.Mutable(base);
        for (Object2IntMap.Entry<Holder<Enchantment>> e : ferocious.entrySet()) {
            active.set(e.getKey(), e.getIntValue());
        }
        ItemEnchantments applied = active.toImmutable();
        result.set(DataComponents.ENCHANTMENTS, applied);
        result.set(SAComponents.BIDENT_ENCHANTMENTS.get(), new BidentEnchantments(base, ferocious, fractal, applied));
        SABident.setMode(result, BidentMode.FEROCIOUS);
        return result;
    }

    /** Il set di una modalità ricavato da un ingrediente: solo ciò che si aggiunge davvero alla base. */
    private static ItemEnchantments fusionSet(ItemStack result, ItemEnchantments base, ItemEnchantments input, boolean allowConflicts) {
        ItemEnchantments.Mutable combined = new ItemEnchantments.Mutable(base);
        ItemEnchantments.Mutable set = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        for (Object2IntMap.Entry<Holder<Enchantment>> e : input.entrySet()) {
            if (result.supportsEnchantment(e.getKey()) && tryApply(combined, e.getKey(), e.getIntValue(), allowConflicts)) {
                set.set(e.getKey(), e.getIntValue());
            }
        }
        return set.toImmutable();
    }
}
