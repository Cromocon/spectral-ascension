package cromocon.spectralascension.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.dafuqs.spectrum.inventories.BedrockAnvilScreenHandler;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Bedrock Anvil di Spectrum: {@code createResult} taglia il livello risultante a {@code Enchantment.getMaxLevel()} (max
 * del JSON, Apothic non tocca questa classe) con due chiamate, confronto e assegnazione. Un oggetto che ha già un
 * livello più alto (T9 Sharpness XII, o un oggetto qualsiasi oltre il massimo) combinato con un libro dello stesso
 * incantesimo verrebbe ABBASSATO (XII → V). Qui il massimo usato diventa {@code max(getMaxLevel(), livello che lo
 * stack di SINISTRA ha già)}: il livello non scende mai sotto quello dell'oggetto di partenza, e non sale oltre le
 * regole normali (con livelli uguali ≤ massimo il risultato è quello di sempre). Vale per tutti gli oggetti.
 *
 * <p>Niente variabili locali catturate: l'incantesimo è il receiver della chiamata, lo stack di sinistra è lo slot 0
 * del menu (FIRST_INPUT_SLOT_INDEX), e il livello si cerca per identità del valore tra i suoi incantesimi.
 */
@Mixin(value = BedrockAnvilScreenHandler.class, remap = false)
public abstract class BedrockAnvilScreenHandlerMixin {

    @WrapOperation(method = "createResult",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/Enchantment;getMaxLevel()I"),
            require = 2, allow = 2)
    private int spectral_ascension$neverLower(Enchantment enchantment, Operation<Integer> original) {
        int max = original.call(enchantment);
        var left = ((AbstractContainerMenu) (Object) this).getSlot(0).getItem();
        for (var e : EnchantmentHelper.getEnchantmentsForCrafting(left).entrySet()) {
            if (e.getKey().value() == enchantment) return Math.max(max, e.getIntValue());
        }
        return max;
    }
}
