package cromocon.spectralascension.menu;

import cromocon.spectralascension.item.SATools;
import cromocon.spectralascension.item.WorkstaffOption;
import cromocon.spectralascension.registry.SAMenus;
import de.dafuqs.spectrum.inventories.QuickNavigationGridScreenHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Menu della workstaff T7+ (Shift + tasto destro, aperto da {@link SATools.Workstaff#use}): lo stesso di Spectrum più
 * area 7×7/9×9, Foundry e Inventory Insertion (voci in {@link WorkstaffOption}). Non si può riusare
 * {@code WorkstaffScreenHandler}: il suo costruttore usa il tipo di menu di Spectrum (che aprirebbe la sua schermata) e
 * il suo pacchetto accetta solo gli ordinali di {@code GUIToggle}. Il T6 continua ad aprire quello di Spectrum.
 * <p>
 * Come il menu di Spectrum, sul server ricorda lo stack in mano e resta valido solo finché quello STESSO oggetto è
 * ancora nella mano da cui è stato aperto (un altro stack, anche uguale, chiude il menu e fa scartare le scelte).
 * Il client riceve la mano nei dati di apertura (la schermata legge lo stato dallo stack in quella mano).
 */
public class WorkstaffMenu extends QuickNavigationGridScreenHandler {
    private final InteractionHand hand;
    private final ItemStack stack;

    public WorkstaffMenu(int syncId, Inventory inventory, InteractionHand hand, ItemStack stack) {
        super(SAMenus.WORKSTAFF.get(), syncId);
        this.hand = hand;
        this.stack = stack;
    }

    /** Costruttore del client: la mano arriva nei dati di apertura scritti da {@link #open}. */
    public static WorkstaffMenu fromNetwork(int syncId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        InteractionHand hand = buf.readEnum(InteractionHand.class);
        return new WorkstaffMenu(syncId, inventory, hand, inventory.player.getItemInHand(hand));
    }

    /** Sul server: apre il menu per la workstaff nella mano indicata. */
    public static void open(ServerPlayer player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        player.openMenu(new SimpleMenuProvider((syncId, inventory, p) -> new WorkstaffMenu(syncId, inventory, hand, held),
                        Component.translatable("item.spectrum.workstaff")),
                buf -> buf.writeEnum(hand));
    }

    public InteractionHand hand() {
        return hand;
    }

    /** Valido finché nella mano c'è lo STESSO stack (identità, come WorkstaffScreenHandler) ed è una workstaff T7+. */
    @Override
    public boolean stillValid(Player player) {
        return player.getItemInHand(hand) == stack && stack.getItem() instanceof SATools.Workstaff staff && staff.hasOwnMenu();
    }

    /**
     * Scelta arrivata dal client: ordinale di {@link WorkstaffOption}. Rifiutata (false, nulla cambia) se l'indice è
     * fuori dai limiti, se il menu non è più valido o se il tier non permette la voce ({@link WorkstaffOption#apply}).
     */
    public boolean select(Player player, int optionIndex) {
        WorkstaffOption option = WorkstaffOption.byIndex(optionIndex);
        return option != null && stillValid(player) && WorkstaffOption.apply(player, stack, option);
    }
}
