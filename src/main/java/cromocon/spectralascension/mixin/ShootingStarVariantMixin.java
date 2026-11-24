package cromocon.spectralascension.mixin;

import de.dafuqs.spectrum.blocks.shooting_star.ShootingStar;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Tipi di shooting star di Spectrum: con i pesi vanilla (su 8) Colorful uscirebbe il 25%. Qui i pesi sono su 20:
 * Fiery 10%, Pristine 10%, Glistering 10%, Colorful 35%, Gemstone 35%.
 */
@Mixin(ShootingStar.Variant.class)
public abstract class ShootingStarVariantMixin {
    /** Pesi su 20, nell'ordine Fiery, Pristine, Glistering, Colorful, Gemstone. */
    private static final int WEIGHT_FIERY = 2, WEIGHT_PRISTINE = 2, WEIGHT_GLISTERING = 2, WEIGHT_COLORFUL = 7, WEIGHT_GEMSTONE = 7;
    private static final int WEIGHT_TOTAL = WEIGHT_FIERY + WEIGHT_PRISTINE + WEIGHT_GLISTERING + WEIGHT_COLORFUL + WEIGHT_GEMSTONE;

    @Inject(method = "getWeightedRandomType", at = @At("HEAD"), cancellable = true)
    private static void spectral_ascension$weightedType(RandomSource random, CallbackInfoReturnable<ShootingStar.Variant> cir) {
        int r = random.nextInt(WEIGHT_TOTAL);
        ShootingStar.Variant result;
        if ((r -= WEIGHT_FIERY) < 0) result = ShootingStar.Variant.FIERY;
        else if ((r -= WEIGHT_PRISTINE) < 0) result = ShootingStar.Variant.PRISTINE;
        else if ((r -= WEIGHT_GLISTERING) < 0) result = ShootingStar.Variant.GLISTERING;
        else if ((r -= WEIGHT_COLORFUL) < 0) result = ShootingStar.Variant.COLORFUL;
        else result = ShootingStar.Variant.GEMSTONE;
        cir.setReturnValue(result);
    }
}
