package cromocon.spectralascension.client;

import cromocon.spectralascension.item.WorkstaffArea;
import cromocon.spectralascension.mixin.client.ClientLevelAccessor;
import de.dafuqs.spectrum.api.item.AoEBreakingTool;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Previsione sul client dello scavo ad area della workstaff. Il server rompe tutta l'area nella stessa chiamata del
 * blocco mirato (BreakEvent → AoEBreakingTool.afterBreakingBlock), ma il client prevedeva solo il blocco mirato: con lo
 * scavo istantaneo quello spariva subito e gli altri un attimo dopo, quando arrivavano gli aggiornamenti dal server.
 *
 * <p>Il mixin su MultiPlayerGameMode.destroyBlock calcola l'area all'inizio (col blocco mirato ancora presente, come il
 * server, che la calcola prima di toglierlo: il raycast deve colpire la stessa faccia) e la toglie alla fine se il
 * blocco mirato è stato rotto. destroyBlock gira sempre dentro MultiPlayerGameMode.startPrediction, quindi ogni
 * setBlock entra nella previsione aperta: alla conferma (ack) del server BlockStatePredictionHandler rimette lo stato
 * vero dei blocchi che il server non ha rotto. Niente particelle o suoni: li manda il server (evento 2001).
 */
public final class WorkstaffAreaPrediction {
    private WorkstaffAreaPrediction() {
    }

    /** Interruttore per il confronto nel client di sviluppo (azione {@code area_prediction} degli scenari). */
    public static boolean enabled = true;

    /** Blocchi che il server romperà insieme a {@code center}; vuota se non c'è niente da prevedere con certezza. */
    public static List<BlockPos> predict(ClientLevel level, LocalPlayer player, GameType mode, BlockPos center) {
        if (!enabled || player == null || level == null) {
            return List.of();
        }
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof AoEBreakingTool tool) || !WorkstaffArea.predictCanUseAoE(player, stack)) {
            return List.of();
        }
        Direction side = WorkstaffArea.targetedSide(player, center); // come il server: solo il blocco nel mirino
        if (side == null) {
            return List.of();
        }
        Predicate<BlockState> filter = WorkstaffArea.minableFilter(stack);
        List<BlockPos> out = new ArrayList<>();
        for (BlockPos p : WorkstaffArea.positions(center, side, tool.getAoERange(stack))) {
            if (WorkstaffArea.wouldBreak(player, mode, stack, level, p, filter)) {
                out.add(p);
            }
        }
        return out;
    }

    /** Toglie i blocchi previsti, come il client toglie il blocco mirato (onDestroyedByPlayer → fluido o aria, flag 11). */
    public static void apply(ClientLevel level, List<BlockPos> positions) {
        if (!((ClientLevelAccessor) level).spectral_ascension$predictionHandler().isPredicting()) {
            return; // fuori da una previsione il server non rimetterebbe i blocchi sbagliati: meglio non toccare niente
        }
        for (BlockPos p : positions) {
            level.setBlock(p, level.getFluidState(p).createLegacyBlock(), 11);
        }
    }
}
