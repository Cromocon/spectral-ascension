package cromocon.spectralascension.mixin;

import cromocon.spectralascension.item.BidentEnchantments;
import cromocon.spectralascension.item.SABident;
import de.dafuqs.spectrum.api.recipe.IngredientStack;
import de.dafuqs.spectrum.blocks.fusion_shrine.FusionShrineBlockEntity;
import de.dafuqs.spectrum.recipe.fusion_shrine.FusionShrineRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import javax.annotation.Nullable;

/**
 * Fusione del bident tricromatico: copy_components di Spectrum copia gli incantesimi solo dal primo ingrediente
 * (il Ferocious), noi vogliamo anche quelli del secondo (il Fractal) nel set Fractal memorizzato.
 * <p>
 * In {@code FusionShrineRecipe.craft} gli ingredienti vengono consumati prima di creare il risultato, quindi:
 * <ol>
 *   <li>all'INIZIO di craft (HEAD) leggiamo gli incantesimi dei due bident dall'inventario della Fusion Shrine;</li>
 *   <li>sulla chiamata a {@code spawnCraftingResultAndXP} sostituiamo lo stack del risultato (@ModifyArg, argomento 2)
 *       con quello rifatto da {@link BidentEnchantments#applyFusion}.</li>
 * </ol>
 * Per le altre ricette non cambia niente: si cattura solo se il risultato della ricetta è un {@link SABident} e il
 * primo ingrediente NON è già un nostro bident, altrimenti il campo resta null e il risultato passa invariato. Il
 * secondo caso è l'upgrade di tier (bident T6 → T7): lì copy_components porta già modalità e set di incantesimi del
 * bident di partenza, e il secondo ingrediente è un blocco di metallo, non un Fractal. Il campo vive solo durante una chiamata a craft
 * (sul thread del server) e viene azzerato subito dopo l'uso.
 */
@Mixin(value = FusionShrineRecipe.class, remap = false)
public abstract class FusionShrineRecipeMixin {

    @Shadow
    @Final
    protected ItemStack output;

    @Shadow
    public abstract List<IngredientStack> getIngredientStacks();

    /** Incantesimi [Ferocious, Fractal] letti all'inizio del craft in corso; null se la ricetta non è la nostra. */
    @Unique
    @Nullable
    private ItemEnchantments[] spectral_ascension$bidentInputs;

    @Inject(method = "craft", at = @At("HEAD"))
    private void spectral_ascension$readBidentInputs(Level world, FusionShrineBlockEntity shrine, CallbackInfo ci) {
        spectral_ascension$bidentInputs = null;
        List<IngredientStack> ingredients = getIngredientStacks();
        if (!(output.getItem() instanceof SABident) || ingredients.size() < 2) {
            return;
        }
        // Stessa ricerca di craft: per ogni ingrediente il primo slot che lo soddisfa.
        ItemEnchantments[] found = {ItemEnchantments.EMPTY, ItemEnchantments.EMPTY};
        for (int ingredient = 0; ingredient < 2; ingredient++) {
            for (int slot = 0; slot < shrine.getContainerSize(); slot++) {
                ItemStack stack = shrine.getItem(slot);
                if (ingredients.get(ingredient).test(stack)) {
                    if (ingredient == 0 && stack.getItem() instanceof SABident) {
                        return; // upgrade di tier: basta copy_components (vedi sopra)
                    }
                    found[ingredient] = stack.getTagEnchantments();
                    break;
                }
            }
        }
        spectral_ascension$bidentInputs = found;
    }

    @ModifyArg(method = "craft", index = 2, at = @At(value = "INVOKE",
            target = "Lde/dafuqs/spectrum/recipe/fusion_shrine/FusionShrineRecipe;spawnCraftingResultAndXP(Lnet/minecraft/world/level/Level;Lde/dafuqs/spectrum/blocks/fusion_shrine/FusionShrineBlockEntity;Lnet/minecraft/world/item/ItemStack;I)V"))
    private ItemStack spectral_ascension$fuseBidentSets(Level world, FusionShrineBlockEntity shrine, ItemStack result, int count) {
        ItemEnchantments[] inputs = spectral_ascension$bidentInputs;
        spectral_ascension$bidentInputs = null;
        if (inputs == null || !(result.getItem() instanceof SABident)) {
            return result;
        }
        // Regola di Spectrum: i conflitti valgono se il proprietario (l'ultimo che ha usato la Shrine) ha l'advancement.
        boolean allowConflicts = SABident.canApplyConflicting(shrine.getOwnerIfOnline(world));
        return BidentEnchantments.applyFusion(result, inputs[0], inputs[1], world.registryAccess(), allowConflicts);
    }
}
