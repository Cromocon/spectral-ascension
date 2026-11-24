package cromocon.spectralascension.network;

import cromocon.spectralascension.SpectralAscension;
import cromocon.spectralascension.item.PrismaticTrail;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * "Questa freccia lascia la scia prismatica": dal server al client, alla comparsa della freccia per un giocatore
 * (vedi {@link PrismaticTrail} e {@code ServerEntityMixin}). Sul client mette lo stesso attachment sull'entità.
 */
public record PrismaticTrailPayload(int entityId) implements CustomPacketPayload {
    public static final Type<PrismaticTrailPayload> TYPE = new Type<>(SpectralAscension.id("prismatic_trail"));
    public static final StreamCodec<ByteBuf, PrismaticTrailPayload> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(PrismaticTrailPayload::new, PrismaticTrailPayload::entityId);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Mod bus: registra il pacchetto (solo server → client). */
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, STREAM_CODEC, PrismaticTrailPayload::handle);
    }

    /** Sul client (thread principale): la freccia è appena stata aggiunta dallo stesso bundle. */
    private static void handle(PrismaticTrailPayload payload, IPayloadContext context) {
        Entity entity = context.player().level().getEntity(payload.entityId());
        if (entity != null) {
            entity.setData(PrismaticTrail.MARKER, true);
        }
    }
}
