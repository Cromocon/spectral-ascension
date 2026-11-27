package cromocon.spectralascension.gametest;

import cromocon.spectralascension.SpectralAscension;
import cromocon.spectralascension.item.WorkstaffArea;
import cromocon.spectralascension.item.WorkstaffUltimine;
import cromocon.spectralascension.registry.SAItems;
import de.dafuqs.spectrum.api.ink.color.InkColors;
import de.dafuqs.spectrum.registries.SpectrumDataComponentTypes;
import de.dafuqs.spectrum.registries.SpectrumItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

import static cromocon.spectralascension.gametest.SATestUtil.*;

/**
 * Area della workstaff solo per il blocco nel mirino (WorkstaffArea.targetedSide): un vein-miner come FTB Ultimine
 * rompe i suoi blocchi con gameMode.destroyBlock dentro il BreakEvent del blocco mirato, e prima ognuno faceva partire
 * un'area intera (64 × 80 blocchi con l'Alloy 9×9). Più il budget condiviso con Ultimine (WorkstaffUltimine).
 */
@GameTestHolder(SpectralAscension.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SAWorkstaffTargetTests {
    private static final String EMPTY = "empty";
    /** Muri in alto (y relativa ≥ 110), sotto quelli di SAWorkstaffMenuTests (≥ 150) e sopra quelli di SAGameTests. */
    private static final int WALL_Y = 110;

    private SAWorkstaffTargetTests() {
    }

    /** Muro 3×3 di pietra centrato su {@code center} (piano XY, come l'area su una parete: da center−1 a center+1). */
    private static List<BlockPos> wall3(GameTestHelper helper, BlockPos center) {
        List<BlockPos> out = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++)
            for (int dy = -1; dy <= 1; dy++) out.add(center.offset(dx, dy, 0));
        out.forEach(p -> helper.getLevel().setBlockAndUpdate(p, Blocks.STONE.defaultBlockState()));
        return out;
    }

    private static long left(GameTestHelper helper, List<BlockPos> blocks) {
        return blocks.stream().filter(p -> !helper.getLevel().getBlockState(p).isAir()).count();
    }

    private static ItemStack staff(GameTestHelper helper, Item item, int range) {
        ItemStack s = enchanted(helper, item);
        s.set(SpectrumDataComponentTypes.AOE, range);
        return s;
    }

    /**
     * Sopravvivenza, workstaff T7 3×3 a pagamento, Ink sbloccato e fiaschetta. Il giocatore guarda il centro del muro A;
     * destroyBlock su un blocco NON mirato (centro del muro B, di lato) rompe solo quel blocco, non fa l'area e non paga
     * Ink. Poi destroyBlock sul blocco mirato: tutta l'area di A e 3 White Ink, come prima.
     */
    @GameTest(template = EMPTY)
    public static void workstaff_area_only_targeted(GameTestHelper helper) {
        List<String> problems = new ArrayList<>(), report = new ArrayList<>();
        BlockPos feet = helper.absolutePos(new BlockPos(0, WALL_Y, 0));
        BlockPos aCenter = feet.offset(0, 1, 2);
        BlockPos bCenter = aCenter.offset(5, 0, 0);
        List<BlockPos> a = wall3(helper, aCenter), b = wall3(helper, bCenter);
        ServerPlayer player = mockPlayer(helper);
        aim(player, feet, 0.0F, 0.0F); // verso +Z, il centro di A
        unlockInk(helper, player);
        ItemStack flask = SpectrumItems.INK_FLASK.get().getDefaultInstance();
        var storage = SpectrumItems.INK_FLASK.get().getEnergyStorage(flask);
        storage.addEnergy(InkColors.WHITE, 1000);
        SpectrumItems.INK_FLASK.get().setEnergyStorage(flask, storage);
        player.getInventory().setItem(9, flask);
        player.setItemInHand(InteractionHand.MAIN_HAND, staff(helper, SAItems.UMBRAL_WORKSTAFF.get(), 1));

        boolean side = player.gameMode.destroyBlock(bCenter);
        long paidSide = 1000 - SpectrumItems.INK_FLASK.get().getEnergyStorage(flask).getEnergy(InkColors.WHITE);
        report.add("blocco non mirato: rotto " + side + ", B rimasti " + left(helper, b) + "/9, A " + left(helper, a) + "/9, Ink " + paidSide);
        expect(problems, side && helper.getLevel().getBlockState(bCenter).isAir(), "il blocco non mirato non è stato rotto");
        expect(problems, left(helper, b) == 8, "blocco non mirato: area scavata, B rimasti " + left(helper, b) + " invece di 8");
        expect(problems, left(helper, a) == 9, "blocco non mirato: toccato il muro mirato, A rimasti " + left(helper, a));
        expect(problems, paidSide == 0, "blocco non mirato: pagati " + paidSide + " White Ink invece di 0");

        player.gameMode.destroyBlock(aCenter);
        long paid = 1000 - SpectrumItems.INK_FLASK.get().getEnergyStorage(flask).getEnergy(InkColors.WHITE);
        report.add("blocco mirato: A rimasti " + left(helper, a) + "/9, Ink " + paid);
        expect(problems, left(helper, a) == 0, "blocco mirato: A rimasti " + left(helper, a) + " invece di 0");
        expect(problems, paid == 3, "blocco mirato: Ink pagato " + paid + " invece di 3");
        clearAround(helper, aCenter, 12);
        b.forEach(p -> helper.getLevel().removeBlock(p, false));
        finish(helper, "workstaff_area_only_targeted", problems, report);
    }

    // ---------- Ultimine finto ----------

    private static List<BlockPos> fakeUltimine;
    private static boolean fakeUltimineBreaking;
    private static boolean fakeUltimineRegistered;

    /**
     * Come FTB Ultimine (FTBUltimine.blockBroken, priorità HIGH di Architectury): sul BreakEvent del blocco mirato rompe
     * tutta la selezione con gameMode.destroyBlock, il blocco mirato per primo, poi annulla l'evento esterno.
     */
    private static void onBreak(BlockEvent.BreakEvent event) {
        List<BlockPos> list = fakeUltimine;
        if (list == null || fakeUltimineBreaking || !event.getPos().equals(list.getFirst())
                || !(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        fakeUltimineBreaking = true;
        try {
            for (BlockPos p : list) player.gameMode.destroyBlock(p);
        } finally {
            fakeUltimineBreaking = false;
        }
        event.setCanceled(true);
    }

    /**
     * Ultimine finto con l'Alloy 3×3 (gratis): muro A davanti, muro C subito dietro. Ultimine rompe il centro di A
     * (mirato), poi il centro di C e due suoi angoli. Il centro di A fa l'area (A sparisce tutto); i blocchi di C no,
     * anche se dopo il buco in A il raycast colpisce proprio il centro di C: di C spariscono solo i 3 blocchi di Ultimine.
     */
    @GameTest(template = EMPTY)
    public static void workstaff_area_ultimine_nested(GameTestHelper helper) {
        if (!fakeUltimineRegistered) {
            NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, SAWorkstaffTargetTests::onBreak);
            fakeUltimineRegistered = true;
        }
        List<String> problems = new ArrayList<>(), report = new ArrayList<>();
        BlockPos feet = helper.absolutePos(new BlockPos(0, WALL_Y + 8, 0));
        BlockPos aCenter = feet.offset(0, 1, 2);
        BlockPos cCenter = aCenter.offset(0, 0, 1);
        List<BlockPos> a = wall3(helper, aCenter), c = wall3(helper, cCenter);
        Player player = mockPlayer(helper);
        aim((ServerPlayer) player, feet, 0.0F, 0.0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, staff(helper, SAItems.PRISMATIC_WORKSTAFF.get(), 1));
        fakeUltimine = List.of(aCenter, cCenter, cCenter.offset(-1, -1, 0), cCenter.offset(1, 1, 0));
        try {
            ((ServerPlayer) player).gameMode.destroyBlock(aCenter);
        } finally {
            fakeUltimine = null;
        }
        report.add("Ultimine finto: A rimasti " + left(helper, a) + "/9, C " + left(helper, c) + "/9");
        expect(problems, left(helper, a) == 0, "blocco mirato dentro Ultimine: A rimasti " + left(helper, a) + " invece di 0");
        expect(problems, left(helper, c) == 6, "blocchi di Ultimine: C rimasti " + left(helper, c) + " invece di 6 (area partita da un blocco non mirato)");
        clearAround(helper, aCenter, 12);
        c.forEach(p -> helper.getLevel().removeBlock(p, false));

        // Budget condiviso con Ultimine: −(N − 1) sul limite, N = blocchi dell'area; tetto del tier (T7 con aoe 4 = 7×7).
        int[] expected = {0, -8, -24, -48, -80};
        for (int r = 0; r <= 4; r++) {
            int got = WorkstaffUltimine.maxBlocksModifier(staff(helper, SAItems.PRISMATIC_WORKSTAFF.get(), r));
            expect(problems, got == expected[r], "Alloy raggio " + r + ": modificatore " + got + " invece di " + expected[r]);
        }
        int capped = WorkstaffUltimine.maxBlocksModifier(staff(helper, SAItems.UMBRAL_WORKSTAFF.get(), 4));
        expect(problems, capped == -48, "T7 con aoe 4: modificatore " + capped + " invece di -48 (7×7)");
        boolean ultimine = ModList.get().isLoaded("ftbultimine");
        boolean hasModifier = staff(helper, SAItems.PRISMATIC_WORKSTAFF.get(), 4).getAttributeModifiers().modifiers().stream()
                .anyMatch(e -> e.modifier().id().equals(WorkstaffUltimine.MODIFIER_ID));
        expect(problems, hasModifier == ultimine, "modificatore di Ultimine " + (hasModifier ? "presente" : "assente") + " con Ultimine " + (ultimine ? "caricato" : "assente"));
        report.add("budget Ultimine " + java.util.Arrays.toString(expected) + ", modificatore sull'oggetto: " + hasModifier);
        finish(helper, "workstaff_area_ultimine_nested", problems, report);
    }

    /**
     * Alloy 9×9 (gratis): l'area rompe 80 blocchi + il mirato, ma gli eventi 2001 (particelle + suono) che passano da
     * ServerLevel.levelEvent sono al più 8 per l'area (WorkstaffArea.sendBreakEffects) + 1 del blocco mirato, non 81.
     */
    @GameTest(template = EMPTY)
    public static void workstaff_area_capped_effects(GameTestHelper helper) {
        List<String> problems = new ArrayList<>(), report = new ArrayList<>();
        BlockPos feet = helper.absolutePos(new BlockPos(0, WALL_Y + 20, 0));
        BlockPos center = feet.offset(0, 1, 2);
        List<BlockPos> wall = new ArrayList<>();
        for (int dx = -4; dx <= 4; dx++)
            for (int dy = -1; dy <= 7; dy++) wall.add(center.offset(dx, dy, 0));
        wall.forEach(p -> helper.getLevel().setBlockAndUpdate(p, Blocks.STONE.defaultBlockState()));
        ServerPlayer player = mockPlayer(helper);
        aim(player, feet, 0.0F, 0.0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, staff(helper, SAItems.PRISMATIC_WORKSTAFF.get(), 4));

        int before = WorkstaffArea.destroyEventsForTests;
        player.gameMode.destroyBlock(center);
        int events = WorkstaffArea.destroyEventsForTests - before;
        report.add("9×9: rimasti " + left(helper, wall) + "/81, eventi 2001 " + events);
        expect(problems, left(helper, wall) == 0, "9×9: rimasti " + left(helper, wall) + " blocchi invece di 0");
        expect(problems, events == WorkstaffArea.MAX_BREAK_EFFECTS + 1, "9×9: " + events + " eventi 2001 invece di 9");
        clearAround(helper, center, 16);
        finish(helper, "workstaff_area_capped_effects", problems, report);
    }

    private static void expect(List<String> problems, boolean ok, String what) {
        if (!ok) problems.add(what);
    }
}
