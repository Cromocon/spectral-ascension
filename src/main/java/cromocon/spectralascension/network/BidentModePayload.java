package cromocon.spectralascension.network;

import cromocon.spectralascension.SpectralAscension;
import cromocon.spectralascension.menu.BidentMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Modalità scelta nel menu del bident, dal client al server (come WorkstaffToggleSelectedPayload di Spectrum).
 * <p>
 * Porta l'indice grezzo ({@code BidentMode.ordinal()}) e non la modalità già decodificata: un indice fuori dai limiti
 * di un client modificato si scarta nel controllo lato server invece di far fallire la decodifica.
 */
public record BidentModePayload(int modeIndex) implements CustomPacketPayload {
    public static final Type<BidentModePayload> TYPE = new Type<>(SpectralAscension.id("bident_mode"));
    public static final StreamCodec<ByteBuf, BidentModePayload> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(BidentModePayload::new, BidentModePayload::modeIndex);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Mod bus: registra il pacchetto (solo client → server). */
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, STREAM_CODEC, BidentModePayload::handle);
    }

    /**
     * Sul server (thread principale). Accettato solo con il menu del bident aperto e ancora valido (bident in mano),
     * poi {@link BidentMenu#select} controlla oggetto e modalità e applica il cambio.
     */
    private static void handle(BidentModePayload payload, IPayloadContext context) {
        Player player = context.player();
        if (!(player.containerMenu instanceof BidentMenu menu) || !menu.stillValid(player)
                || !menu.select(player, payload.modeIndex())) {
            SpectralAscension.LOGGER.warn("Modalità del bident rifiutata per {}: indice {}, menu {}",
                    player.getName().getString(), payload.modeIndex(), player.containerMenu.getClass().getSimpleName());
        }
    }
}
