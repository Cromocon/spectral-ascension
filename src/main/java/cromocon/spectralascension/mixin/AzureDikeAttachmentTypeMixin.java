package cromocon.spectralascension.mixin;

import cromocon.spectralascension.compat.SACurios;
import cromocon.spectralascension.item.SAArmorSet;
import de.dafuqs.spectrum.attachment_types.AzureDikeAttachmentType;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Modifica il calcolo dell'Azure Dike di Spectrum, che normalmente guarda solo gli accessori negli slot Curios.
 * <p>
 * Un mixin "inietta" codice in una classe di un'altra mod mentre il gioco la carica. Qui aggiungiamo codice
 * all'INIZIO (HEAD) di {@code AzureDikeAttachmentType.recalculate}: se il set completo è indossato impostiamo il
 * Dike del set e annulliamo il resto del metodo, così gli accessori Curios non contano (sono "disattivati").
 * {@code remap = false}: la classe è di Spectrum, non di Minecraft, quindi i nomi non vanno tradotti.
 */
@Mixin(value = AzureDikeAttachmentType.class, remap = false)
public abstract class AzureDikeAttachmentTypeMixin {

    @Inject(method = "recalculate", at = @At("HEAD"), cancellable = true)
    private static void spectral_ascension$armorSetDike(LivingEntity entity, CallbackInfo ci) {
        if (entity.level().isClientSide()) {
            return;
        }
        if (SAArmorSet.isFullSetWorn(entity)) {
            SAArmorSet.applySetDike(entity);
            ci.cancel();
        } else if (!SACurios.hasInventory(entity)) {
            // Senza Curios Spectrum non tocca il Dike: togliamo noi quello del set.
            SAArmorSet.clearDike(entity);
        }
    }
}
