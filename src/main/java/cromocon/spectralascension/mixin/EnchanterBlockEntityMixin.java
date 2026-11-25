package cromocon.spectralascension.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.dafuqs.revelationary.api.advancements.AdvancementHelper;
import de.dafuqs.spectrum.blocks.enchanter.EnchanterBlockEntity;
import de.dafuqs.spectrum.registries.SpectrumAdvancements;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Spectrum salva {@code canOwnerOverenchant} nell'Enchanter solo in {@code setOwner} (quando un giocatore lo usa):
 * dopo aver ottenuto l'advancement {@code spectrum:milestones/unlock_overenchanting_with_enchanter} (che da noi si
 * sblocca trovando un libro o un oggetto non nostro oltre il massimo vanilla) un Enchanter già piazzato continuerebbe a rifiutare
 * l'overenchanting finché qualcuno non lo tocca di nuovo. Le due letture del campo (scelta della ricetta in
 * {@code calculateCurrentRecipe}, controllo prima di craftare in {@code checkRecipeRequirements}) diventano
 * {@code valore salvato || il proprietario ha l'advancement adesso}. Proprietario offline o sconosciuto: resta il
 * valore salvato. Quando un Enchanter fermo rileva lo sblocco, aggiorna l'inventario virtuale per rivalutare la ricetta.
 */
@Mixin(value = EnchanterBlockEntity.class, remap = false)
public abstract class EnchanterBlockEntityMixin {
    @Shadow protected boolean canOwnerOverenchant;
    @Shadow protected boolean inventoryChanged;
    @Shadow protected int currentItemProcessingTime;
    @Shadow private RecipeHolder<?> currentRecipe;

    @Unique
    private boolean spectral_ascension$observedOverenchanting;

    @Inject(method = "serverTick", at = @At("HEAD"))
    private static void spectral_ascension$refreshOnUnlock(Level level, BlockPos pos, BlockState state,
                                                          EnchanterBlockEntity enchanter, CallbackInfo ci) {
        var mixin = (EnchanterBlockEntityMixin) (Object) enchanter;
        boolean unlocked = !mixin.canOwnerOverenchant && mixin.currentRecipe == null
                && mixin.currentItemProcessingTime < 0 && enchanter.getItem(0).has(DataComponents.STORED_ENCHANTMENTS)
                && spectral_ascension$ownerHasUnlock(enchanter, level);
        if (unlocked && !mixin.spectral_ascension$observedOverenchanting && !mixin.inventoryChanged) {
            enchanter.inventoryChanged();
        }
        mixin.spectral_ascension$observedOverenchanting = unlocked;
    }

    @WrapOperation(method = {"calculateCurrentRecipe", "checkRecipeRequirements"}, require = 2,
            at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
                    target = "Lde/dafuqs/spectrum/blocks/enchanter/EnchanterBlockEntity;canOwnerOverenchant:Z"))
    private static boolean spectral_ascension$ownerOverenchantsNow(EnchanterBlockEntity enchanter, Operation<Boolean> original) {
        if (original.call(enchanter)) return true;
        var level = enchanter.getLevel();
        if (level == null || level.isClientSide()) return false;
        return spectral_ascension$ownerHasUnlock(enchanter, level);
    }

    @Unique
    private static boolean spectral_ascension$ownerHasUnlock(EnchanterBlockEntity enchanter, Level level) {
        Player owner = enchanter.getOwnerIfOnline(level);
        return owner != null && AdvancementHelper.hasAdvancement(owner, SpectrumAdvancements.OVERENCHANTING);
    }
}
