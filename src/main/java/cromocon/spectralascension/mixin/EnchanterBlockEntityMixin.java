package cromocon.spectralascension.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.dafuqs.revelationary.api.advancements.AdvancementHelper;
import de.dafuqs.spectrum.blocks.enchanter.EnchanterBlockEntity;
import de.dafuqs.spectrum.registries.SpectrumAdvancements;
import net.minecraft.world.entity.player.Player;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Spectrum salva {@code canOwnerOverenchant} nell'Enchanter solo in {@code setOwner} (quando un giocatore lo usa):
 * dopo aver ottenuto l'advancement {@code spectrum:milestones/unlock_overenchanting_with_enchanter} (che da noi si
 * sblocca trovando un libro o un oggetto non nostro oltre il massimo vanilla) un Enchanter già piazzato continuerebbe a rifiutare
 * l'overenchanting finché qualcuno non lo tocca di nuovo. Le due letture del campo (scelta della ricetta in
 * {@code calculateCurrentRecipe}, controllo prima di craftare in {@code checkRecipeRequirements}) diventano
 * {@code valore salvato || il proprietario ha l'advancement adesso}. Proprietario offline o sconosciuto: resta il
 * valore salvato. {@code require = 2}: se Spectrum cambia una delle due letture, il gioco non parte.
 */
@Mixin(value = EnchanterBlockEntity.class, remap = false)
public abstract class EnchanterBlockEntityMixin {

    @WrapOperation(method = {"calculateCurrentRecipe", "checkRecipeRequirements"}, require = 2,
            at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
                    target = "Lde/dafuqs/spectrum/blocks/enchanter/EnchanterBlockEntity;canOwnerOverenchant:Z"))
    private static boolean spectral_ascension$ownerOverenchantsNow(EnchanterBlockEntity enchanter, Operation<Boolean> original) {
        if (original.call(enchanter)) return true;
        var level = enchanter.getLevel();
        if (level == null || level.isClientSide()) return false;
        Player owner = enchanter.getOwnerIfOnline(level);
        return owner != null && AdvancementHelper.hasAdvancement(owner, SpectrumAdvancements.OVERENCHANTING);
    }
}
