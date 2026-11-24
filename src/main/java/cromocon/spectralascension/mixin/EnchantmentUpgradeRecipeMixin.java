package cromocon.spectralascension.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import cromocon.spectralascension.item.SAEnchantCap;
import de.dafuqs.spectrum.recipe.enchanter.EnchantmentUpgradeRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Ricette {@code spectrum:enchantment_upgrade}: il livello massimo è {@code levels.size() + 1}
 * ({@code getLevelCap}, usato da {@code matches}, dal clamp del risultato in {@code EnchanterBlockEntity} e dalle
 * schermate EMI/REI). Per i 42 incantesimi della tabella di {@link SAEnchantCap} non supera il tetto ATM10: Thorns e
 * Swift Sneak (ricette fino a VI) si fermano a V; gli altri hanno già ricette sotto il tetto. Fuori tabella resta
 * quello di Spectrum. L'overenchanting si sblocca con {@code spectrum:milestones/unlock_overenchanting_with_enchanter},
 * ridefinito da noi (tools/gen_data.py).
 */
@Mixin(value = EnchantmentUpgradeRecipe.class, remap = false)
public abstract class EnchantmentUpgradeRecipeMixin {

    @ModifyReturnValue(method = "getLevelCap", at = @At("RETURN"))
    private int spectral_ascension$capToAtm10(int original) {
        try {
            var cap = SAEnchantCap.tableCap(((EnchantmentUpgradeRecipe) (Object) this).getEnchantment());
            return cap.isPresent() ? Math.min(original, cap.getAsInt()) : original;
        } catch (UnsupportedOperationException e) { // ricetta di datagen senza Holder: nessun tetto
            return original;
        }
    }
}
