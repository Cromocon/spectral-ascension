package cromocon.spectralascension.network;

import cromocon.spectralascension.SpectralAscension;
import cromocon.spectralascension.menu.WorkstaffMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Voce scelta nel menu della workstaff T7+, dal client al server (come WorkstaffToggleSelectedPayload di Spectrum, ma
 * con l'ordinale del nostro {@code WorkstaffOption}). Porta l'indice grezzo: un indice fuori dai limiti di un client
 * modificato si scarta nel controllo lato server invece di far fallire la decodifica.
 */
public record WorkstaffOptionPayload(int optionIndex) implements CustomPacketPayload {
    public static final Type<WorkstaffOptionPayload> TYPE = new Type<>(SpectralAscension.id("workstaff_option"));
    public static final StreamCodec<ByteBuf, WorkstaffOptionPayload> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(WorkstaffOptionPayload::new, WorkstaffOptionPayload::optionIndex);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Mod bus: registra il pacchetto (solo client → server). */
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, STREAM_CODEC, WorkstaffOptionPayload::handle);
    }

    /**
     * Sul server (thread principale). Accettato solo con il nostro menu della workstaff aperto; {@link WorkstaffMenu#select}
     * controlla identità dello stack in mano, indice e tier e applica la voce.
     */
    private static void handle(WorkstaffOptionPayload payload, IPayloadContext context) {
        Player player = context.player();
        if (!(player.containerMenu instanceof WorkstaffMenu menu) || !menu.select(player, payload.optionIndex())) {
            SpectralAscension.LOGGER.warn("Voce del menu della workstaff rifiutata per {}: indice {}, menu {}",
                    player.getName().getString(), payload.optionIndex(), player.containerMenu.getClass().getSimpleName());
        }
    }
}
