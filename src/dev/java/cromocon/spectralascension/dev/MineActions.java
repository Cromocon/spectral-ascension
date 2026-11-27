package cromocon.spectralascension.dev;

import com.google.gson.JsonObject;
import cromocon.spectralascension.client.WorkstaffAreaPrediction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

/**
 * Scavo vero dal client e conteggio dei blocchi (anche sul ClientLevel, nello stesso tick): servono a vedere che lo
 * scavo ad area della workstaff sparisce sul client insieme al blocco mirato (WorkstaffAreaPrediction).
 */
final class MineActions {
    private MineActions() {
    }

    /**
     * mine: {@code pos}, {@code face} (faccia colpita, default north); {@code look} (true) gira prima il giocatore verso il
     * centro della faccia e aspetta 3 tick, così il server (che rifà il raycast per l'area) vede la stessa rotazione.
     * Poi MultiPlayerGameMode.startDestroyBlock come il clic sinistro. Con {@code from}/{@code to} conta subito, nello
     * stesso tick e sul ClientLevel, i blocchi {@code block} (default aria) della zona; {@code count} = numero esatto atteso.
     */
    static Exec mine(JsonObject s) {
        BlockPos pos = Scenario.pos(s, "pos");
        Direction face = Direction.byName(Scenario.str(s, "face", "north"));
        if (face == null) throw new IllegalArgumentException("faccia non valida");
        boolean aim = Scenario.bool(s, "look", true);
        Region region = s.has("from") ? Region.of(s) : null;
        int expected = Scenario.integer(s, "count", -1);
        Minecraft mc = Minecraft.getInstance();
        java.util.function.Supplier<StepResult> dig = () -> {
            BlockState before = mc.level.getBlockState(pos);
            boolean started = mc.gameMode.startDestroyBlock(pos, face);
            mc.player.swing(InteractionHand.MAIN_HAND);
            BlockState after = mc.level.getBlockState(pos);
            boolean broken = after != before && after.isAir();
            String detail = String.format(Locale.ROOT, "startDestroyBlock %s (%s) = %s, client: %s -> %s, previsione area=%s",
                    pos.toShortString(), face, started, before.getBlock().getName().getString(), after.getBlock().getName().getString(),
                    WorkstaffAreaPrediction.enabled ? "on" : "off");
            boolean ok = started;
            if (region != null) {
                int n = region.count(mc.level);
                detail += ", stesso tick sul client: " + n + "/" + region.volume() + " " + region.block;
                if (expected >= 0) ok &= n == expected;
            }
            return StepResult.of(ok, detail + (broken ? "" : " (blocco mirato non rotto)"));
        };
        if (!aim) return runner -> dig.get();
        return new Exec() {
            Exec inner;

            @Override
            public StepResult poll(ScenarioRunner runner) {
                if (inner == null) inner = aimThenDig(mc, pos, face, dig);
                return inner.poll(runner);
            }

            @Override
            public int expectedTicks() {
                return 3;
            }
        };
    }

    private static Exec aimThenDig(Minecraft mc, BlockPos pos, Direction face, java.util.function.Supplier<StepResult> dig) {
        LocalPlayer p = mc.player;
        Vec3 t = Vec3.atCenterOf(pos).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
        Vec3 d = t.subtract(p.getEyePosition());
        float yaw = (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90.0F;
        float pitch = Mth.clamp((float) -(Mth.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * Mth.RAD_TO_DEG), -90, 90);
        p.setYRot(yaw);
        p.setXRot(pitch);
        p.yRotO = yaw;
        p.xRotO = pitch;
        p.setYHeadRot(yaw);
        p.yHeadRotO = yaw;
        // il client manda la rotazione al server nel tick del giocatore: si aspetta che arrivi prima di scavare
        return Exec.delay(3, dig);
    }

    /**
     * count_blocks: conta i blocchi {@code block} (id o #tag, default minecraft:air) fra {@code from} e {@code to};
     * {@code side}: client (default, ClientLevel, subito) o server. {@code count} esatto, oppure {@code min}/{@code max}.
     */
    static Exec countBlocks(JsonObject s) {
        Region region = Region.of(s);
        int exact = Scenario.integer(s, "count", -1);
        int min = Scenario.integer(s, "min", Integer.MIN_VALUE);
        int max = Scenario.integer(s, "max", Integer.MAX_VALUE);
        java.util.function.Function<Integer, StepResult> judge = n -> StepResult.of(
                (exact < 0 || n == exact) && n >= min && n <= max,
                n + "/" + region.volume() + " " + region.block + " fra " + region.from.toShortString() + " e " + region.to.toShortString());
        if (Scenario.str(s, "side", "client").equals("server")) {
            return ScenarioActions.onServer((server, sp) -> {
                StepResult r = judge.apply(region.count(sp.serverLevel()));
                return StepResult.of(r.ok(), "server: " + r.detail());
            });
        }
        return runner -> {
            StepResult r = judge.apply(region.count(Minecraft.getInstance().level));
            return StepResult.of(r.ok(), "client: " + r.detail());
        };
    }

    private record Region(BlockPos from, BlockPos to, String block) {
        static Region of(JsonObject s) {
            return new Region(Scenario.pos(s, "from"), Scenario.pos(s, "to"), Scenario.str(s, "block", "minecraft:air"));
        }

        int volume() {
            return (Math.abs(to.getX() - from.getX()) + 1) * (Math.abs(to.getY() - from.getY()) + 1) * (Math.abs(to.getZ() - from.getZ()) + 1);
        }

        int count(Level level) {
            int n = 0;
            for (BlockPos p : BlockPos.betweenClosed(from, to)) {
                BlockState st = level.getBlockState(p);
                boolean match = block.startsWith("#")
                        ? st.is(TagKey.create(Registries.BLOCK, ResourceLocation.parse(block.substring(1))))
                        : BuiltInRegistries.BLOCK.getKey(st.getBlock()).toString().equals(block);
                if (match) n++;
            }
            return n;
        }
    }
}
