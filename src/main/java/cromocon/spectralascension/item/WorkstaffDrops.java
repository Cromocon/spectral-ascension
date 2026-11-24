package cromocon.spectralascension.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Interruttori dei drop della workstaff dal T7 (componente {@code spectral_ascension:workstaff_drops}), scelti nel menu
 * (sotto-griglia "Drop"): Foundry (T7+) e Inventory Insertion (T8+). Dicono cosa VUOLE il giocatore; gli incantesimi
 * veri sullo stack li allinea {@link WorkstaffOption#syncDropEnchantments}.
 * <p>
 * {@code foundry = true} con Silk Touch selezionato = Foundry "sospesa": l'incantesimo non c'è (Foundry e Silk Touch
 * si escludono), e torna da solo quando si passa a Fortune o Resonance. Il componente lo copia anche il
 * {@code copy_components} della Fusion Shrine, quindi passa da un tier al successivo.
 *
 * @param foundry   Foundry accesa (default sì)
 * @param insertion Inventory Insertion accesa (default sì; conta solo dal T8)
 */
public record WorkstaffDrops(boolean foundry, boolean insertion) {
    /** Default di ogni workstaff T7+ appena craftata o data con /give: tutto acceso. */
    public static final WorkstaffDrops DEFAULT = new WorkstaffDrops(true, true);

    public static final Codec<WorkstaffDrops> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.optionalFieldOf("foundry", true).forGetter(WorkstaffDrops::foundry),
            Codec.BOOL.optionalFieldOf("insertion", true).forGetter(WorkstaffDrops::insertion)
    ).apply(i, WorkstaffDrops::new));

    public static final StreamCodec<ByteBuf, WorkstaffDrops> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, WorkstaffDrops::foundry,
            ByteBufCodecs.BOOL, WorkstaffDrops::insertion,
            WorkstaffDrops::new);

    public WorkstaffDrops withFoundry(boolean value) {
        return new WorkstaffDrops(value, insertion);
    }

    public WorkstaffDrops withInsertion(boolean value) {
        return new WorkstaffDrops(foundry, value);
    }
}
