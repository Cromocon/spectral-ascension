package cromocon.spectralascension.network;

import cromocon.spectralascension.SpectralAscension;
import cromocon.spectralascension.item.SATools;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Velocità ESATTA di un proiettile, dal server al client.
 * <p>
 * Perché serve: i pacchetti vanilla che portano la velocità di un'entità al client
 * ({@code ClientboundAddEntityPacket} alla comparsa e {@code ClientboundSetEntityMotionPacket} poi) tagliano ogni
 * componente a ±3,9 blocchi/tick. Le frecce delle nostre balestre partono a 6,3 (T6) e 7,9 (T7) blocchi/tick: il
 * client riceveva un vettore tagliato componente per componente, quindi più lento e con una DIREZIONE diversa
 * (più inclinato verso l'asse minore), e la freccia disegnata partiva storta finché il pacchetto di posizione del
 * server (ogni 20 tick per le frecce) non la rimetteva sulla traiettoria vera. Sul server il tiro era già dritto.
 * Questo pacchetto, mandato subito dopo quelli vanilla da {@code ServerEntityMixin}, corregge la velocità del client.
 */
public record ProjectileVelocityPayload(int entityId, double x, double y, double z) implements CustomPacketPayload {
    public static final Type<ProjectileVelocityPayload> TYPE = new Type<>(SpectralAscension.id("projectile_velocity"));
    public static final StreamCodec<ByteBuf, ProjectileVelocityPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ProjectileVelocityPayload::entityId,
            ByteBufCodecs.DOUBLE, ProjectileVelocityPayload::x,
            ByteBufCodecs.DOUBLE, ProjectileVelocityPayload::y,
            ByteBufCodecs.DOUBLE, ProjectileVelocityPayload::z,
            ProjectileVelocityPayload::new);
    /** Limite dei pacchetti vanilla, per componente (ClientboundSetEntityMotionPacket / ClientboundAddEntityPacket). */
    public static final double VANILLA_LIMIT = 3.9;

    public ProjectileVelocityPayload(Entity entity) {
        this(entity.getId(), entity.getDeltaMovement().x, entity.getDeltaMovement().y, entity.getDeltaMovement().z);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Il client ha bisogno della velocità esatta? Solo per le frecce tirate dalle nostre armi (balestre e archi della
     * mod) e solo se almeno una componente supera il limite dei pacchetti vanilla: altrimenti quelli bastano.
     */
    public static boolean needsExactVelocity(Entity entity) {
        if (!(entity instanceof AbstractArrow arrow) || arrow.isRemoved()) return false;
        ItemStack weapon = arrow.getWeaponItem();
        if (weapon == null || !(weapon.getItem() instanceof SATools.Crossbow || weapon.getItem() instanceof SATools.Bow)) return false;
        Vec3 v = arrow.getDeltaMovement();
        return Math.abs(v.x) > VANILLA_LIMIT || Math.abs(v.y) > VANILLA_LIMIT || Math.abs(v.z) > VANILLA_LIMIT;
    }

    /** Mod bus: registra il pacchetto (solo server → client). */
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, STREAM_CODEC, ProjectileVelocityPayload::handle);
    }

    /** Sul client (thread principale): come handleSetEntityMotion vanilla, ma senza il taglio a ±3,9. */
    private static void handle(ProjectileVelocityPayload payload, IPayloadContext context) {
        Entity entity = context.player().level().getEntity(payload.entityId());
        if (entity != null) {
            entity.lerpMotion(payload.x(), payload.y(), payload.z());
        }
    }
}
