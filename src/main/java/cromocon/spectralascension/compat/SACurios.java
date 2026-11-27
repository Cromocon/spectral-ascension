package cromocon.spectralascension.compat;

import de.dafuqs.spectrum.api.item.AzureDikeItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.List;

/**
 * Unico punto in cui la mod chiama l'API di Curios.
 * <p>
 * Perche': se Curios cambia API c'e' un solo posto da correggere, e per un eventuale port Fabric/Trinkets e'
 * l'unica classe da riscrivere. Nessun altro file deve importare {@code top.theillusivec4.curios}.
 */
public final class SACurios {

    private SACurios() {
    }

    /** {@code true} se l'entita' ha un inventario Curios (cioe' Spectrum ricalcola il suo Dike dagli slot). */
    public static boolean hasInventory(LivingEntity entity) {
        return CuriosApi.getCuriosInventory(entity).isPresent();
    }

    /** Gli accessori Azure Dike indossati negli slot Curios, cercati come fa Spectrum (vuoto senza inventario Curios). */
    public static List<ItemStack> dikeItems(LivingEntity entity) {
        return CuriosApi.getCuriosInventory(entity)
                .map(inv -> inv.findCurios(stack -> stack.getItem() instanceof AzureDikeItem).stream().map(SlotResult::stack).toList())
                .orElse(List.of());
    }
}
