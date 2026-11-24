package cromocon.spectralascension.item;

import cromocon.spectralascension.SpectralAscension;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.neoforge.event.AnvilUpdateEvent;

import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Tetto ATM10 degli incantesimi (ricerca/03, PASSAGGIO «Da fare» n. 4). I nostri oggetti escono già oltre il massimo
 * (T9: ATM10 + 3, es. Sharpness XII): senza tetto si potrebbero estrarre libri Sharpness XII, applicabili ovunque.
 *
 * <p>Fonte del tetto: «max effettivo ATM10» di ricerca/09, sezioni A e B (i 42 incantesimi vanilla), cioè il valore
 * {@code Max Level} di {@code config/apotheosis/enchantments.cfg} di Apothic Enchanting nell'istanza ATM10 (default di
 * Apothic 1.6.4, nessuna modifica dell'ATM team; esclude le fonti rare come i gear set «pinnacle» di Apotheosis).
 * Tabella fissa e non lettura a runtime della config di Apothic (decisione utente): nel dev Apothic non c'è, e il test
 * deve essere deterministico.
 *
 * <p><b>Fuori tabella nessun tetto</b> ({@link #tableCap} vuoto): gli incantesimi di Spectrum e delle altre mod
 * seguono le regole loro. Dove si usa:
 * <ul>
 *   <li>Enchanter di Spectrum, copia dalle bowl ({@code SpectrumEnchantmentHelperMixin}): ogni stack nelle bowl, libri
 *       compresi e di qualunque mod, è tagliato al tetto (solo per i 42 incantesimi della tabella); XP di conseguenza;</li>
 *   <li>incudine vanilla ({@link #onAnvilUpdate}, priorità LOWEST, dopo Apothic): un libro prodotto da un NOSTRO
 *       oggetto (es. Extraction Tome di Apothic) è tagliato al tetto;</li>
 *   <li>ricette {@code spectrum:enchantment_upgrade} ({@code EnchantmentUpgradeRecipeMixin}): il livello massimo della
 *       ricetta non supera il tetto (Thorns e Swift Sneak VI → V).</li>
 * </ul>
 */
public final class SAEnchantCap {
    private SAEnchantCap() {
    }

    private static final Map<String, Integer> ATM10_MAX = Map.ofEntries(
            // A. armi, attrezzi, archi/tridenti
            Map.entry("sharpness", 9), Map.entry("smite", 10), Map.entry("bane_of_arthropods", 10),
            Map.entry("sweeping_edge", 8), Map.entry("fire_aspect", 5), Map.entry("knockback", 5),
            Map.entry("looting", 8), Map.entry("breach", 8), Map.entry("density", 10), Map.entry("wind_burst", 8),
            Map.entry("efficiency", 9), Map.entry("fortune", 8), Map.entry("silk_touch", 1),
            Map.entry("unbreaking", 8), Map.entry("mending", 1), Map.entry("power", 9), Map.entry("punch", 5),
            Map.entry("flame", 1), Map.entry("infinity", 1), Map.entry("piercing", 8), Map.entry("quick_charge", 5),
            Map.entry("multishot", 1), Map.entry("impaling", 10), Map.entry("loyalty", 9), Map.entry("riptide", 9),
            Map.entry("channeling", 1), Map.entry("protection", 8),
            // B. armature e utilità
            Map.entry("fire_protection", 9), Map.entry("blast_protection", 9), Map.entry("projectile_protection", 11),
            Map.entry("feather_falling", 11), Map.entry("thorns", 5), Map.entry("respiration", 7),
            Map.entry("aqua_affinity", 1), Map.entry("depth_strider", 7), Map.entry("frost_walker", 7),
            Map.entry("soul_speed", 7), Map.entry("swift_sneak", 5), Map.entry("luck_of_the_sea", 8),
            Map.entry("lure", 8), Map.entry("binding_curse", 1), Map.entry("vanishing_curse", 1));

    /** Gli id dei 42 incantesimi della tabella (per i test e l'advancement dell'overenchanting). */
    public static Set<ResourceLocation> tableIds() {
        return ATM10_MAX.keySet().stream().map(ResourceLocation::withDefaultNamespace).collect(Collectors.toSet());
    }

    /** Tetto ATM10 di un incantesimo della tabella; vuoto se l'incantesimo non è in tabella (= nessun tetto). */
    public static OptionalInt tableCap(Holder<Enchantment> enchantment) {
        var id = enchantment.unwrapKey().map(k -> k.location()).orElse(null);
        if (id != null && id.getNamespace().equals("minecraft")) {
            Integer cap = ATM10_MAX.get(id.getPath());
            if (cap != null) return OptionalInt.of(cap);
        }
        return OptionalInt.empty();
    }

    public static boolean isOurs(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals(SpectralAscension.MOD_ID);
    }

    /**
     * Livelli tagliati al tetto della tabella (clamp: l'incantesimo resta, col livello del tetto). Gli incantesimi fuori
     * tabella restano come sono. Restituisce la stessa istanza se non c'è niente da tagliare.
     */
    public static ItemEnchantments clamp(ItemEnchantments enchantments) {
        if (enchantments.isEmpty()) return enchantments;
        ItemEnchantments.Mutable mutable = null;
        for (var e : enchantments.entrySet()) {
            OptionalInt cap = tableCap(e.getKey());
            if (cap.isPresent() && e.getIntValue() > cap.getAsInt()) {
                if (mutable == null) mutable = new ItemEnchantments.Mutable(enchantments);
                mutable.set(e.getKey(), cap.getAsInt());
            }
        }
        return mutable == null ? enchantments : mutable.toImmutable();
    }

    /**
     * Incudine vanilla (NeoForge {@link AnvilUpdateEvent}, registrato a priorità LOWEST: gira dopo gli altri listener,
     * Apothic compreso). Se a sinistra c'è un NOSTRO oggetto e il risultato è un libro (ha
     * {@code minecraft:stored_enchantments}: es. l'Extraction Tome di Apothic, che copia tutti gli incantesimi
     * dell'oggetto su un libro), i livelli del libro sono tagliati al tetto. Un nostro oggetto in uscita (libro a destra)
     * non è toccato.
     */
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack output = event.getOutput();
        if (output.isEmpty() || !isOurs(event.getLeft())) return;
        ItemEnchantments stored = output.get(DataComponents.STORED_ENCHANTMENTS);
        if (stored == null) return;
        ItemEnchantments clamped = clamp(stored);
        if (clamped != stored) {
            ItemStack copy = output.copy();
            copy.set(DataComponents.STORED_ENCHANTMENTS, clamped);
            event.setOutput(copy);
        }
    }
}
