package cromocon.spectralascension.menu;

import cromocon.spectralascension.SpectralAscension;
import cromocon.spectralascension.item.SABident;
import cromocon.spectralascension.registry.SAMenus;
import de.dafuqs.spectrum.inventories.QuickNavigationGridScreenHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

/**
 * Menu di selezione della modalità del bident (Shift + tasto destro), come il menu della workstaff di Spectrum.
 * <p>
 * Come {@code WorkstaffScreenHandler} di Spectrum estende {@link QuickNavigationGridScreenHandler} (menu senza slot):
 * il server lo apre con {@link #open}, il client mostra {@code BidentScreen} (griglia "quick navigation" di Spectrum)
 * e la scelta torna al server con {@code BidentModePayload}, che chiama {@link #select}. Non si può riusare il menu
 * della workstaff: il suo costruttore usa il tipo di menu di Spectrum, che aprirebbe la schermata della workstaff.
 * <p>
 * Il menu ricorda la mano da cui è stato aperto; il client la riceve nei dati di apertura (serve alla schermata per
 * leggere la modalità attiva).
 */
public class BidentMenu extends QuickNavigationGridScreenHandler {
    private final InteractionHand hand;

    public BidentMenu(int syncId, Inventory inventory, InteractionHand hand) {
        super(SAMenus.BIDENT.get(), syncId);
        this.hand = hand;
    }

    /** Costruttore del client: la mano arriva nei dati di apertura scritti da {@link #open}. */
    public static BidentMenu fromNetwork(int syncId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        return new BidentMenu(syncId, inventory, buf.readEnum(InteractionHand.class));
    }

    /** Sul server: apre il menu per il bident nella mano indicata. */
    public static void open(ServerPlayer player, InteractionHand hand) {
        player.openMenu(new SimpleMenuProvider((syncId, inventory, p) -> new BidentMenu(syncId, inventory, hand),
                        Component.translatable("gui." + SpectralAscension.MOD_ID + ".bident_menu")),
                buf -> buf.writeEnum(hand));
    }

    public InteractionHand hand() {
        return hand;
    }

    /** Resta aperto finché nella mano c'è un nostro bident. */
    @Override
    public boolean stillValid(Player player) {
        return player.getItemInHand(hand).getItem() instanceof SABident;
    }

    /**
     * Scelta arrivata dal client: indice della modalità ({@code BidentMode.ordinal()}). Il controllo vero (bident in
     * mano, modalità valida) lo fa {@link SABident#selectMode}. Ritorna se la modalità è stata applicata.
     */
    public boolean select(Player player, int modeIndex) {
        return SABident.selectMode(player, hand, modeIndex);
    }
}
