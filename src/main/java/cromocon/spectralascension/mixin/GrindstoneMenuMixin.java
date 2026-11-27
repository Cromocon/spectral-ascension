package cromocon.spectralascension.mixin;

import cromocon.spectralascension.item.MaceWindBurst;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mola: una nostra mazza esce con Wind Burst al livello intrinseco (vedi {@link MaceWindBurst}). Iniezione a RETURN di
 * {@code computeResult} (singolo oggetto o fusione di due): il risultato è già una copia, la si modifica sul posto.
 */
@Mixin(GrindstoneMenu.class)
public abstract class GrindstoneMenuMixin {

    @Inject(method = "computeResult", at = @At("RETURN"))
    private void spectral_ascension$keepMaceWindBurst(ItemStack inputItem, ItemStack additionalItem, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack result = cir.getReturnValue();
        if (result != null && !result.isEmpty()) MaceWindBurst.keepOnGrindstone(result, inputItem, additionalItem);
    }
}
