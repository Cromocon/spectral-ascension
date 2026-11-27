package cromocon.spectralascension.item;

import cromocon.spectralascension.SpectralAscension;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;

/**
 * FTB Ultimine con la workstaff in mano: il limite di Ultimine (max_blocks, 64 di default) si divide con l'area. Con
 * un'area di N blocchi (1, 9, 25, 49, 81) Ultimine ne rompe al massimo 64 − N oltre al blocco mirato, che è il centro
 * dell'area: in tutto restano circa 64 blocchi. I blocchi di Ultimine non fanno partire altre aree
 * (WorkstaffArea.targetedSide).
 *
 * <p>Usa l'attributo pubblico di Ultimine {@code ftbultimine:max_blocks_modifier} (FTBUltimineServerConfig.getMaxBlocks:
 * limite + valore dell'attributo, minimo 0), messo come modificatore della mano principale: niente dipendenza di
 * compilazione. Senza Ultimine l'attributo non esiste e non succede niente.
 */
@EventBusSubscriber(modid = SpectralAscension.MOD_ID)
public final class WorkstaffUltimine {
    private WorkstaffUltimine() {
    }

    public static final ResourceLocation MAX_BLOCKS_ATTRIBUTE =
            ResourceLocation.fromNamespaceAndPath("ftbultimine", "max_blocks_modifier");
    public static final ResourceLocation MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(SpectralAscension.MOD_ID, "workstaff_area_ultimine");

    /** Quanto togliere al limite di Ultimine: −(N − 1), dove N sono i blocchi dell'area (il centro conta già per Ultimine). */
    public static int maxBlocksModifier(ItemStack stack) {
        return stack.getItem() instanceof SATools.Workstaff staff
                ? 1 - WorkstaffArea.areaSize(staff.getAoERange(stack)) : 0;
    }

    @SubscribeEvent
    static void onItemAttributes(ItemAttributeModifierEvent event) {
        int amount = maxBlocksModifier(event.getItemStack());
        if (amount == 0) {
            return;
        }
        BuiltInRegistries.ATTRIBUTE.getHolder(MAX_BLOCKS_ATTRIBUTE).ifPresent(attribute -> event.addModifier(attribute,
                new AttributeModifier(MODIFIER_ID, amount, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND));
    }
}
