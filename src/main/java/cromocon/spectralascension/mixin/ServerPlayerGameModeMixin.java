package cromocon.spectralascension.mixin;

import cromocon.spectralascension.item.WorkstaffArea;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

/**
 * Ricorda il blocco del destroyBlock più esterno in corso per questo giocatore ({@link WorkstaffArea.BreakRoot}).
 * FTB Ultimine rompe i suoi blocchi con gameMode.destroyBlock DENTRO il BreakEvent del blocco mirato: così l'area della
 * workstaff riconosce i blocchi rotti da Ultimine (o dall'area stessa) e scatta solo per il blocco mirato.
 *
 * <p>Il contatore torna a zero al primo destroyBlock di un altro tick, nel caso un'eccezione abbia saltato il RETURN.
 */
@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin implements WorkstaffArea.BreakRoot {
    @Shadow
    @Final
    protected ServerPlayer player;

    @Unique
    private int spectral_ascension$depth;
    @Unique
    private BlockPos spectral_ascension$root;
    @Unique
    private int spectral_ascension$tick;

    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void spectral_ascension$enter(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        int now = player.server.getTickCount();
        if (spectral_ascension$depth <= 0 || spectral_ascension$tick != now) {
            spectral_ascension$depth = 0;
            spectral_ascension$root = pos.immutable();
            spectral_ascension$tick = now;
        }
        spectral_ascension$depth++;
    }

    @Inject(method = "destroyBlock", at = @At("RETURN"))
    private void spectral_ascension$exit(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (spectral_ascension$depth > 0) {
            spectral_ascension$depth--;
        }
    }

    @Override
    @Nullable
    public BlockPos spectral_ascension$rootBreak() {
        return spectral_ascension$depth > 0 ? spectral_ascension$root : null;
    }
}
