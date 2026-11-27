package cromocon.spectralascension.mixin;

import cromocon.spectralascension.item.PrismaticTrail;
import cromocon.spectralascension.network.PrismaticTrailPayload;
import cromocon.spectralascension.network.ProjectileVelocityPayload;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.bundle.PacketAndPayloadAcceptor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/**
 * ServerEntity è l'oggetto del server che manda ai client lo stato di un'entità (comparsa, posizione, velocità).
 * Per le frecce veloci delle nostre armi aggiunge, DOPO i pacchetti vanilla, la velocità esatta
 * ({@link ProjectileVelocityPayload}): quella vanilla è tagliata a ±3,9 per componente e farebbe partire storta la
 * freccia disegnata dal client. Alla comparsa manda anche il marcatore della scia delle frecce T9
 * ({@link PrismaticTrailPayload}).
 */
@Mixin(ServerEntity.class)
public abstract class ServerEntityMixin {
    @Shadow
    @Final
    private Entity entity;

    @Shadow
    @Final
    private Consumer<Packet<?>> broadcast;

    /** Comparsa dell'entità per un giocatore: il pacchetto finisce nello stesso bundle, dopo AddEntity e SetEntityMotion. */
    @Inject(method = "sendPairingData", at = @At("TAIL"))
    private void spectral_ascension$exactVelocityOnSpawn(ServerPlayer player, PacketAndPayloadAcceptor<ClientGamePacketListener> acceptor,
                                                         CallbackInfo ci) {
        if (ProjectileVelocityPayload.needsExactVelocity(entity)) {
            acceptor.accept(new ProjectileVelocityPayload(entity));
        }
        // scia delle frecce T9: il marcatore del server (PrismaticTrail) arriva al client con la comparsa della freccia
        if (PrismaticTrail.isMarked(entity)) {
            acceptor.accept(new PrismaticTrailPayload(entity.getId()));
        }
    }

    @Shadow
    private Vec3 lastSentMovement;

    /** lastSentMovement e hurtMarked all'inizio di sendChanges, per sapere se la vanilla ha mandato un SetEntityMotion. */
    @Unique
    private Vec3 spectral_ascension$movementBefore;
    @Unique
    private boolean spectral_ascension$hurtMarkedBefore;

    @Inject(method = "sendChanges", at = @At("HEAD"))
    private void spectral_ascension$rememberMotion(CallbackInfo ci) {
        spectral_ascension$movementBefore = lastSentMovement;
        spectral_ascension$hurtMarkedBefore = entity.hurtMarked;
    }

    /**
     * Solo nei tick in cui la vanilla ha mandato un SetEntityMotion (tagliato): lastSentMovement riassegnato (confronto
     * per identità: la vanilla lo cambia solo quando la velocità è cambiata davvero) o hurtMarked. Prima partiva a
     * ogni tick finché una componente superava il limite, anche per le frecce piantate, che tengono come
     * deltaMovement il vettore dell'impatto.
     */
    @Inject(method = "sendChanges", at = @At("TAIL"))
    private void spectral_ascension$exactVelocityOnUpdate(CallbackInfo ci) {
        boolean vanillaSent = lastSentMovement != spectral_ascension$movementBefore || spectral_ascension$hurtMarkedBefore;
        spectral_ascension$movementBefore = null;
        if (vanillaSent && ProjectileVelocityPayload.needsExactVelocity(entity)) {
            broadcast.accept(new ClientboundCustomPayloadPacket(new ProjectileVelocityPayload(entity)));
            ProjectileVelocityPayload.onSentForTest(entity);
        }
    }
}
