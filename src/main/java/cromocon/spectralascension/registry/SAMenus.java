package cromocon.spectralascension.registry;

import cromocon.spectralascension.SpectralAscension;
import cromocon.spectralascension.menu.BidentMenu;
import cromocon.spectralascension.menu.WorkstaffMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** I nostri tipi di menu (schermate aperte dal server). La schermata client si registra in SAClient. */
public final class SAMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, SpectralAscension.MOD_ID);

    /** Menu di selezione della modalità del bident (come quello della workstaff di Spectrum). */
    public static final DeferredHolder<MenuType<?>, MenuType<BidentMenu>> BIDENT =
            MENUS.register("bident", () -> IMenuTypeExtension.create(BidentMenu::fromNetwork));

    /** Menu della workstaff T7+ (quello di Spectrum più 7×7/9×9, Foundry e Inventory Insertion). */
    public static final DeferredHolder<MenuType<?>, MenuType<WorkstaffMenu>> WORKSTAFF =
            MENUS.register("workstaff", () -> IMenuTypeExtension.create(WorkstaffMenu::fromNetwork));

    private SAMenus() {
    }

    public static void register(IEventBus modBus) {
        MENUS.register(modBus);
    }
}
