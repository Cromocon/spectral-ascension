package cromocon.spectralascension.registry;

import com.mojang.serialization.Codec;
import cromocon.spectralascension.SpectralAscension;
import cromocon.spectralascension.item.SAEnchantCap;
import net.minecraft.advancements.critereon.ItemSubPredicate;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * I nostri predicati degli item (chiavi del campo {@code predicates} di un item predicate, accanto a
 * {@code minecraft:enchantments} e simili), usabili in qualunque advancement con i trigger vanilla.
 */
public final class SAPredicates {
    public static final DeferredRegister<ItemSubPredicate.Type<?>> ITEM_SUB_PREDICATES =
            DeferredRegister.create(Registries.ITEM_SUB_PREDICATE_TYPE, SpectralAscension.MOD_ID);

    /**
     * {@code "spectral_ascension:not_ours": {}}: l'oggetto NON è della nostra mod (namespace diverso da
     * spectral_ascension). Serve all'override di spectrum:milestones/unlock_overenchanting_with_enchanter
     * (gen_data.gen_overenchanting_unlock): un oggetto incantato oltre il massimo vanilla sblocca l'overenchanting, ma
     * non i nostri pezzi T6-T9, che nascono già oltre.
     */
    public static final DeferredHolder<ItemSubPredicate.Type<?>, ItemSubPredicate.Type<NotOurs>> NOT_OURS =
            ITEM_SUB_PREDICATES.register("not_ours", () -> new ItemSubPredicate.Type<>(NotOurs.CODEC));

    public record NotOurs() implements ItemSubPredicate {
        public static final NotOurs INSTANCE = new NotOurs();
        public static final Codec<NotOurs> CODEC = Codec.unit(INSTANCE);

        @Override
        public boolean matches(ItemStack stack) {
            return !SAEnchantCap.isOurs(stack);
        }
    }

    private SAPredicates() {
    }

    public static void register(IEventBus modBus) {
        ITEM_SUB_PREDICATES.register(modBus);
    }
}
