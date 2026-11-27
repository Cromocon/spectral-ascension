package cromocon.spectralascension.item;

import de.dafuqs.spectrum.api.ink.InkPowered;
import de.dafuqs.spectrum.api.ink.color.InkColor;
import de.dafuqs.spectrum.api.ink.storage.InkStorageItem;
import de.dafuqs.spectrum.api.item.AoEBreakingTool;
import de.dafuqs.spectrum.items.tools.WorkstaffItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.List;
import java.util.function.Predicate;

/**
 * Area dello scavo della workstaff, in un solo posto per il server (SATools.Workstaff.afterBreakingBlock) e per la
 * previsione del client (mixin MultiPlayerGameModeMixin), così i due non possono divergere.
 *
 * <p><b>Copia di Spectrum 1.12.9</b> ({@code de.dafuqs.spectrum.helpers.AoEHelper.doAoEBlockBreaking} e
 * {@code breakBlockWithDrops}, {@code AoEBreakingTool.afterBreakingBlock}, {@code InkPowered.tryDrainEnergy}):
 * a ogni aggiornamento di Spectrum va ricontrollata. La workstaff di Spectrum (WorkstaffItem) sul server passa
 * ancora da AoEHelper: per lei la previsione è una copia, non lo stesso codice.
 */
public final class WorkstaffArea {
    private WorkstaffArea() {
    }

    /** Portata del raycast di AoEBreakingTool.afterBreakingBlock ({@code player.pick(10.0, 1.0F, false)}). */
    public static final double PICK_RANGE = 10.0;

    /**
     * Faccia del blocco {@code pos} colpita dal raycast (AoEBreakingTool.afterBreakingBlock): decide l'orientamento
     * dell'area. Null se il raycast non colpisce proprio {@code pos}: allora niente area.
     *
     * <p>L'area scatta solo per il blocco che il giocatore ha davvero nel mirino. Un vein-miner come FTB Ultimine rompe
     * i suoi blocchi uno a uno con gameMode.destroyBlock, e ognuno lancia BreakEvent → afterBreakingBlock: senza questo
     * controllo ognuno dei suoi 64 blocchi scavava un'area intera (9×9 Alloy: ~5000 blocchi in un tick). Sul server
     * vale anche {@link BreakRoot}: dentro un destroyBlock ne conta solo il primo, così i blocchi di Ultimine che il
     * raycast colpisce attraverso il buco appena scavato non fanno partire altre aree.
     */
    @Nullable
    public static Direction targetedSide(Player player, BlockPos pos) {
        if (player instanceof ServerPlayer sp && sp.gameMode instanceof BreakRoot root) {
            BlockPos first = root.spectral_ascension$rootBreak();
            if (first != null && !first.equals(pos)) {
                return null;
            }
        }
        HitResult hit = player.pick(PICK_RANGE, 1.0F, false);
        return hit instanceof BlockHitResult bhr && hit.getType() == HitResult.Type.BLOCK && bhr.getBlockPos().equals(pos)
                ? bhr.getDirection() : null;
    }

    /**
     * Implementata su ServerPlayerGameMode (mixin ServerPlayerGameModeMixin): il blocco del destroyBlock più esterno
     * in corso per questo giocatore, null se non ce n'è uno.
     */
    public interface BreakRoot {
        @Nullable
        BlockPos spectral_ascension$rootBreak();
    }

    /** Blocchi dell'area di raggio {@code radius}, centro compreso: 1, 9, 25, 49, 81. */
    public static int areaSize(int radius) {
        int side = 2 * Math.max(0, radius) + 1;
        return side * side;
    }

    /**
     * Posizioni dell'area attorno a {@code center}, centro escluso (AoEHelper.doAoEBlockBreaking +
     * removeBlocksInIteration). Sulle pareti (faccia orizzontale) l'area parte da un blocco sotto il centro e sale fino
     * a {@code 2·radius − 1} sopra, così il giocatore scava un passaggio all'altezza dei piedi.
     */
    public static List<BlockPos> positions(BlockPos center, Direction side, int radius) {
        List<BlockPos> out = new ArrayList<>();
        if (radius <= 0) {
            return out;
        }
        boolean doX = side.getStepX() == 0;
        boolean doY = side.getStepY() == 0;
        boolean doZ = side.getStepZ() == 0;
        BlockPos from = center.offset(doX ? -radius : 0, doY ? -1 : 0, doZ ? -radius : 0);
        BlockPos to = center.offset(doX ? radius : 0, doY ? radius * 2 - 1 : 0, doZ ? radius : 0);
        for (BlockPos p : BlockPos.betweenClosed(from, to)) {
            if (!p.equals(center)) {
                out.add(p.immutable());
            }
        }
        return out;
    }

    /** Filtro dei blocchi di AoEHelper.doAoEBlockBreaking: attrezzo adatto (se serve) e velocità di scavo > 0. */
    public static Predicate<BlockState> minableFilter(ItemStack stack) {
        return state -> (!state.requiresCorrectToolForDrops() || stack.isCorrectToolForDrops(state))
                && stack.getDestroySpeed(state) > 0.0F;
    }

    /**
     * Il server romperebbe questo blocco dell'area? Condizioni di AoEHelper.breakBlockWithDrops (chunk caricato, non aria,
     * getDestroyProgress > 0, filtro) e di ServerPlayerGameMode.destroyBlock / CommonHooks.fireBlockBreak che il client
     * conosce (canAttackBlock dell'oggetto, blocchi da operatore, modalità avventura). Non può sapere se un'altra mod
     * annulla il BreakEvent (protezioni): in quel caso il client rimette il blocco alla conferma del server.
     */
    public static boolean wouldBreak(Player player, GameType mode, ItemStack stack, LevelAccessor level, BlockPos pos,
                                     Predicate<BlockState> filter) {
        if (!level.hasChunkAt(pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.getDestroyProgress(player, level, pos) <= 0.0F || !filter.test(state)) {
            return false;
        }
        if (level instanceof Level lvl) {
            if (!stack.getItem().canAttackBlock(state, lvl, pos, player)) {
                return false;
            }
            if (player.blockActionRestricted(lvl, pos, mode)) {
                return false;
            }
        }
        return !(state.getBlock() instanceof GameMasterBlock) || player.canUseGameMasterBlocks();
    }

    /**
     * Previsione di canUseAoE senza togliere Ink (solo lettura, per il client): vero solo se il server pagherà di
     * sicuro. Copia di InkPowered.tryDrainEnergy (creativo gratis, poi sblocco dell'Ink, poi mani, Curios, inventario,
     * contando solo i contenitori con {@code canDrain(true)}). Per un AoEBreakingTool che non conosciamo: falso.
     */
    public static boolean predictCanUseAoE(Player player, ItemStack stack) {
        if (!(stack.getItem() instanceof AoEBreakingTool tool)) {
            return false;
        }
        int range = tool.getAoERange(stack);
        if (range <= 0) {
            return false;
        }
        if (stack.getItem() instanceof SATools.Workstaff ours) {
            return ours.hasFreeAoE() || hasInk(player, SATools.Workstaff.AOE_INK_COLOR, SATools.Workstaff.aoeInkCost(range));
        }
        if (stack.getItem().getClass() == WorkstaffItem.class
                || stack.getItem().getClass() == de.dafuqs.spectrum.items.tools.GlassCrestWorkstaffItem.class) {
            // WorkstaffItem.canUseAoE: 3^raggio di White Ink.
            return hasInk(player, SATools.Workstaff.AOE_INK_COLOR, (long) Math.pow(SATools.Workstaff.AOE_INK_BASE, range));
        }
        return false;
    }

    private static boolean hasInk(Player player, InkColor color, long amount) {
        if (player.isCreative()) {
            return true;
        }
        if (!InkPowered.canUse(player)) {
            return false;
        }
        // Le mani sono anche in inventory.items: tryDrainEnergy le svuota al primo giro, qui non vanno contate due volte.
        Set<ItemStack> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (ItemStack s : player.getHandSlots()) {
            amount -= drainable(s, color, seen);
            if (amount <= 0) return true;
        }
        for (SlotResult r : CuriosApi.getCuriosInventory(player)
                .map(inv -> inv.findCurios(s -> s.getItem() instanceof InkStorageItem<?>))
                .orElse(List.of())) {
            amount -= drainable(r.stack(), color, seen);
            if (amount <= 0) return true;
        }
        for (ItemStack s : player.getInventory().items) {
            amount -= drainable(s, color, seen);
            if (amount <= 0) return true;
        }
        return false;
    }

    private static long drainable(ItemStack stack, InkColor color, Set<ItemStack> seen) {
        if (seen.add(stack) && stack.getItem() instanceof InkStorageItem<?> storage && storage.canDrain(true)) {
            return storage.getEnergyStorage(stack).getEnergy(color);
        }
        return 0L;
    }
}
