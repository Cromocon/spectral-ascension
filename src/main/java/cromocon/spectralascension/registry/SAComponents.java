package cromocon.spectralascension.registry;

import cromocon.spectralascension.SpectralAscension;
import cromocon.spectralascension.item.BidentEnchantments;
import cromocon.spectralascension.item.BidentMode;
import cromocon.spectralascension.item.WorkstaffDrops;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * I nostri "data component": dati salvati nello stack (come gli incantesimi), nel namespace della mod.
 * "persistent" = salvato nel mondo, "networkSynchronized" = mandato al client (serve a tooltip e messaggi).
 */
public final class SAComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, SpectralAscension.MOD_ID);

    /** Modalità del bident tricromatico (spento / Ferocious / Fractal). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BidentMode>> BIDENT_MODE =
            COMPONENTS.registerComponentType("bident_mode",
                    builder -> builder.persistent(BidentMode.CODEC).networkSynchronized(BidentMode.STREAM_CODEC));

    /** Incantesimi memorizzati del bident tricromatico: base e set delle modalità (vedi {@link BidentEnchantments}). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BidentEnchantments>> BIDENT_ENCHANTMENTS =
            COMPONENTS.registerComponentType("bident_enchantments",
                    builder -> builder.persistent(BidentEnchantments.CODEC).networkSynchronized(BidentEnchantments.STREAM_CODEC));

    /**
     * Interruttori Foundry / Inventory Insertion della workstaff T7+ scelti nel menu (vedi {@link WorkstaffDrops}).
     * Lo copia il copy_components della Fusion Shrine, come spectrum:workstaff e spectrum:aoe.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<WorkstaffDrops>> WORKSTAFF_DROPS =
            COMPONENTS.registerComponentType("workstaff_drops",
                    builder -> builder.persistent(WorkstaffDrops.CODEC).networkSynchronized(WorkstaffDrops.STREAM_CODEC));

    /**
     * Potenziamento a Ink dello Shieldgrasp Amulet ereditato dagli stivali (2026-11-27, vedi SAArmorSet): Dike
     * dell'amuleto SOPRA il suo minimo (2), quindi 0-10. Lo mette FusionShrineRecipeMixin sugli stivali T6; i T7+ lo
     * ricevono dal copy_components. Assente = 0 (amuleto vuoto o svuotato).
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> DIKE_BONUS =
            COMPONENTS.registerComponentType("dike_bonus",
                    builder -> builder.persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    private SAComponents() {
    }

    public static void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
    }
}
