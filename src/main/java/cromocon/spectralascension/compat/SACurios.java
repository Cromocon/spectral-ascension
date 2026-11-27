package cromocon.spectralascension.compat;

import net.minecraft.world.entity.LivingEntity;
import top.theillusivec4.curios.api.CuriosApi;

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
}
