package cromocon.spectralascension.mixin;

import cromocon.spectralascension.item.SATools;
import cromocon.spectralascension.item.WorkstaffArea;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Durante lo scavo ad area della workstaff annulla gli eventi 2001 (particelle + suono di rottura) che
 * Block.playerWillDestroy → spawnDestroyParticles manda agli altri giocatori per ogni blocco: con il 9×9 erano 80 in
 * un tick. Gli effetti dell'area li manda poi WorkstaffArea.sendBreakEffects, al massimo 8. Il blocco mirato rompe
 * dopo l'area (BreakEvent viene prima di playerWillDestroy), a guardia già chiusa: tiene i suoi.
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @Inject(method = "levelEvent", at = @At("HEAD"), cancellable = true)
    private void spectral_ascension$skipAreaDestroyEvents(Player player, int type, BlockPos pos, int data, CallbackInfo ci) {
        if (type != 2001) {
            return;
        }
        if (SATools.Workstaff.isBreakingArea()) {
            ci.cancel();
        } else {
            WorkstaffArea.destroyEventsForTests++;
        }
    }
}
