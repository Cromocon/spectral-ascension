package cromocon.spectralascension.mixin;

import cromocon.spectralascension.item.SAEnchantCap;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Incudine vanilla: {@code createResult} taglia il livello risultante a {@code getMaxLevel()} (JSON senza Apothic,
 * {@code EnchHooks.getMaxLevel} con Apothic, il cui coremod riscrive quelle chiamate). Un NOSTRO oggetto con un livello
 * più alto (T9 Sharpness XII) combinato con un libro dello stesso incantesimo verrebbe ABBASSATO (V senza Apothic, IX
 * con Apothic). Come per la Bedrock Anvil ({@link BedrockAnvilScreenHandlerMixin}): a ogni uscita di
 * {@code createResult}, se a sinistra c'è un nostro oggetto e il risultato è lo STESSO oggetto (non un libro: l'Extraction
 * Tome di Apothic resta com'è, tagliato da {@link SAEnchantCap#onAnvilUpdate}), ogni incantesimo che il risultato ha più
 * basso di quello di partenza torna al livello di partenza. Gli aumenti per regola (VII + VII → VIII) restano; il costo
 * in XP è quello calcolato dal gioco (non toccato). Gli oggetti di altre mod e vanilla non cambiano.
 *
 * <p>Iniezione a RETURN (tutti i return, non solo l'ultimo): non dipende dalle chiamate a getMaxLevel, quindi convive
 * con il coremod di Apothic e con il suo {@code @ModifyConstant} sul limite di 40 livelli.
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {

    @Inject(method = "createResult", at = @At("RETURN"))
    private void spectral_ascension$neverLowerOurs(CallbackInfo ci) {
        var menu = (AbstractContainerMenu) (Object) this;
        ItemStack left = menu.getSlot(AnvilMenu.INPUT_SLOT).getItem();
        ItemStack result = menu.getSlot(AnvilMenu.RESULT_SLOT).getItem();
        if (left.isEmpty() || result.isEmpty() || !SAEnchantCap.isOurs(left) || !result.is(left.getItem())) return;
        ItemEnchantments before = EnchantmentHelper.getEnchantmentsForCrafting(left);
        ItemEnchantments after = EnchantmentHelper.getEnchantmentsForCrafting(result);
        ItemEnchantments.Mutable fixed = null;
        for (var e : before.entrySet()) {
            if (after.getLevel(e.getKey()) < e.getIntValue()) {
                if (fixed == null) fixed = new ItemEnchantments.Mutable(after);
                fixed.set(e.getKey(), e.getIntValue());
            }
        }
        if (fixed == null) return;
        EnchantmentHelper.setEnchantments(result, fixed.toImmutable());
        menu.broadcastChanges();
    }
}
