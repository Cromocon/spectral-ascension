package cromocon.spectralascension.item;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import javax.annotation.Nullable;

/**
 * Modalità del bident tricromatico, salvata nello stack (componente spectral_ascension:bident_mode).
 * Si sceglie dal menu del bident (Shift + tasto destro, BidentMenu); in rete viaggia l'indice ({@link #ordinal()}).
 */
public enum BidentMode implements StringRepresentable {
    /** Bident normale: nessuna abilità, nessun costo di Ink. */
    OFF("off", ChatFormatting.RED),
    /** Come il Ferocious glass crest: Riptide anche fuori dall'acqua (Ink bianco), danno a chi tocchi durante lo spin. */
    FEROCIOUS("ferocious", ChatFormatting.GOLD),
    /** Come il Fractal glass crest: lancia una copia illusoria (Ink bianco) e resta in mano, lancio più veloce. */
    FRACTAL("fractal", ChatFormatting.LIGHT_PURPLE);

    /** Formato su disco (nome in minuscolo, es. "ferocious"). */
    public static final Codec<BidentMode> CODEC = StringRepresentable.fromEnum(BidentMode::values);
    /** Formato in rete (client ↔ server): l'indice nell'enum. */
    public static final StreamCodec<ByteBuf, BidentMode> STREAM_CODEC =
            ByteBufCodecs.idMapper(i -> values()[i], BidentMode::ordinal);

    private final String name;
    private final ChatFormatting color;

    BidentMode(String name, ChatFormatting color) {
        this.name = name;
        this.color = color;
    }

    /** La modalità con questo indice ({@link #ordinal()}), o null se l'indice non è valido (pacchetto del menu). */
    @Nullable
    public static BidentMode byIndex(int index) {
        return index >= 0 && index < values().length ? values()[index] : null;
    }

    public ChatFormatting color() {
        return color;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
