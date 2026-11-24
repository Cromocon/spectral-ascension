package cromocon.spectralascension.client;

import cromocon.spectralascension.SpectralAscension;
import cromocon.spectralascension.item.PrismaticTrail;
import de.dafuqs.spectrum.api.ink.color.InkColors;
import de.dafuqs.spectrum.particle.SpectrumParticleTypes;
import de.dafuqs.spectrum.particle.effect.DynamicParticleEffect;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.joml.Vector3f;

/**
 * Solo client: le scintille della scia delle frecce del T9 (vedi {@link PrismaticTrail}).
 * <p>
 * Particella: {@link DynamicParticleEffect} di Spectrum con lo sprite {@link SpectrumParticleTypes#SHOOTING_STAR}, cioè la
 * scintilla che Spectrum fa cadere dietro le stelle cadenti (ShootingStarEntity.playFallingParticles): lo sprite è quasi
 * bianco, quindi prende bene il colore, e brilla al buio ("glowing"). Il T9 si fa con le Colorful Shooting Star: è la
 * stessa scia. Colore casuale per ogni scintilla fra i colori dell'Ink di Spectrum ({@link InkColors}), senza quelli
 * scuri (nero, grigi, marrone) che sulla scia sembravano sporco.
 * <p>
 * Densità: una scintilla ogni {@link #SPACING} blocchi del tratto percorso nell'ultimo tick, al massimo
 * {@link #MAX_PER_TICK} a tick; vita breve. Rispetta l'impostazione "Particelle" del client (addParticle normale).
 * La freccia piantata (o ferma) non si muove: niente scintille.
 */
@EventBusSubscriber(modid = SpectralAscension.MOD_ID, value = Dist.CLIENT) // game bus (default)
public final class PrismaticTrailParticles {
    private static final double SPACING = 0.5;
    private static final int MAX_PER_TICK = 12;
    /** Spostamento minimo in un tick (al quadrato) per considerare la freccia in volo. */
    private static final double MIN_MOVE_SQR = 0.01 * 0.01;
    /** Letti al primo uso (non alla registrazione dell'evento, che avviene mentre le mod si stanno ancora costruendo). */
    private static Vector3f[] colors;

    private PrismaticTrailParticles() {
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof AbstractArrow arrow)) return;
        Level level = arrow.level();
        if (!level.isClientSide() || !PrismaticTrail.isMarked(arrow)) return;
        Vec3 from = new Vec3(arrow.xo, arrow.yo, arrow.zo);
        Vec3 to = arrow.position();
        double moved = from.distanceToSqr(to);
        if (moved < MIN_MOVE_SQR) return;
        if (colors == null) {
            colors = new Vector3f[]{
                    InkColors.CYAN.getColorVec(), InkColors.LIGHT_BLUE.getColorVec(), InkColors.BLUE.getColorVec(),
                    InkColors.PURPLE.getColorVec(), InkColors.MAGENTA.getColorVec(), InkColors.PINK.getColorVec(),
                    InkColors.RED.getColorVec(), InkColors.ORANGE.getColorVec(), InkColors.YELLOW.getColorVec(),
                    InkColors.LIME.getColorVec(), InkColors.GREEN.getColorVec(), InkColors.WHITE.getColorVec()};
        }
        RandomSource random = arrow.getRandom();
        int count = Mth.clamp((int) Math.ceil(Math.sqrt(moved) / SPACING), 1, MAX_PER_TICK);
        for (int i = 0; i < count; i++) {
            Vec3 p = from.lerp(to, (i + random.nextDouble()) / count)
                    .add((random.nextDouble() - 0.5) * 0.15, (random.nextDouble() - 0.5) * 0.15, (random.nextDouble() - 0.5) * 0.15);
            Vector3f color = colors[random.nextInt(colors.length)];
            float scale = 1.0F + random.nextFloat() * 0.6F;
            int lifetime = 12 + random.nextInt(10);
            var effect = new DynamicParticleEffect(SpectrumParticleTypes.SHOOTING_STAR, 0.0F, color, scale, lifetime, false, true);
            level.addParticle(effect, p.x, p.y, p.z,
                    (random.nextDouble() - 0.5) * 0.02, (random.nextDouble() - 0.5) * 0.02, (random.nextDouble() - 0.5) * 0.02);
        }
    }
}
