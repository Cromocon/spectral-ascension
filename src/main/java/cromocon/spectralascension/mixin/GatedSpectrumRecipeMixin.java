package cromocon.spectralascension.mixin;

import cromocon.spectralascension.item.SAEnchantCap;
import cromocon.spectralascension.item.SATools;
import cromocon.spectralascension.item.WorkstaffOption;
import de.dafuqs.spectrum.recipe.GatedSpectrumRecipe;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * copy_components di Spectrum scarta gli incantesimi dell'input che il risultato ha già (addOrUpgradeEnchantment li
 * giudica incompatibili con se stessi): vince sempre il livello del result, anche se l'input è più alto (una spada
 * bedrock con Sharpness IX uscirebbe con Sharpness VII). Per i NOSTRI risultati lo correggiamo: per lo stesso
 * incantesimo vince il livello più alto, senza sommare (VII + VII = VII). Non tocca gli altri incantesimi.
 * <p>
 * Fra tier Spectral la workstaff conserva la modalità di scavo selezionata. Foundry e Inventory Insertion vengono
 * poi allineati agli interruttori copiati da Spectrum, così Foundry resta sospesa se è attivo Silk Touch.
 */
@Mixin(value = GatedSpectrumRecipe.class, remap = false)
public abstract class GatedSpectrumRecipeMixin {

    @Inject(method = "copyComponents", at = @At("RETURN"))
    private static void spectral_ascension$keepHigherLevel(ItemStack output, ItemStack input, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack result = cir.getReturnValue();
        if (!SAEnchantCap.isOurs(result)) {
            return;
        }
        if (result.getItem() instanceof SATools.Workstaff) {
            if (input.getItem() instanceof SATools.Workstaff) {
                WorkstaffOption.preserveMiningMode(input, result);
            }
            WorkstaffOption.syncDropEnchantments(null, result);
        }
        ItemEnchantments.Mutable merged = new ItemEnchantments.Mutable(result.getTagEnchantments());
        boolean changed = false;
        for (Object2IntMap.Entry<Holder<Enchantment>> e : input.getTagEnchantments().entrySet()) {
            int current = merged.getLevel(e.getKey());
            if (current > 0 && e.getIntValue() > current) {
                merged.set(e.getKey(), e.getIntValue());
                changed = true;
            }
        }
        // Tutto in place su result, che È il valore di ritorno: niente setReturnValue.
        if (changed) {
            EnchantmentHelper.setEnchantments(result, merged.toImmutable());
        }
    }
}
