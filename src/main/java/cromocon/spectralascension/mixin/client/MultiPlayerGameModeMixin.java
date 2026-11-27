package cromocon.spectralascension.mixin.client;

import cromocon.spectralascension.client.WorkstaffAreaPrediction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Solo client: lo scavo ad area della workstaff sparisce insieme al blocco mirato invece che un tick dopo. Vedi
 * {@link WorkstaffAreaPrediction}. HEAD calcola l'area (blocco mirato ancora presente, come sul server); RETURN la toglie
 * solo se destroyBlock ha rotto il blocco mirato.
 */
@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private GameType localPlayerMode;

    @Unique
    private List<BlockPos> spectral_ascension$area = List.of();

    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void spectral_ascension$predictArea(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        spectral_ascension$area = WorkstaffAreaPrediction.predict(minecraft.level, minecraft.player, localPlayerMode, pos);
    }

    @Inject(method = "destroyBlock", at = @At("RETURN"))
    private void spectral_ascension$breakArea(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        List<BlockPos> area = spectral_ascension$area;
        spectral_ascension$area = List.of();
        if (cir.getReturnValueZ() && !area.isEmpty() && minecraft.level != null) {
            WorkstaffAreaPrediction.apply(minecraft.level, area);
        }
    }
}
