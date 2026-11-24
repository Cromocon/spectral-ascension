package cromocon.spectralascension.client;

import cromocon.spectralascension.SpectralAscension;
import cromocon.spectralascension.item.BidentMode;
import cromocon.spectralascension.item.SABident;
import cromocon.spectralascension.menu.BidentMenu;
import cromocon.spectralascension.network.BidentModePayload;
import de.dafuqs.spectrum.inventories.QuickNavigationGridScreen;
import de.dafuqs.spectrum.registries.SpectrumSoundEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Menu del bident: la stessa griglia "quick navigation" della workstaff di Spectrum (stessa finestra, stesse caselle
 * da {@code spectrum:textures/gui/quick_navigation.png}, stessi suoni, tasti di movimento per spostarsi, Drop o
 * Inventario per il centro), con tre voci:
 * <pre>
 *            [Spento]
 * [Ferocious]   (x)   [Fractal]
 * </pre>
 * Il centro chiude, il tasto destro del mouse torna indietro (cioè chiude), come nella workstaff. Icone: la barriera per "spento";
 * Ferocious e Fractal mostrano il bident del tier che il giocatore ha in mano, con i suoi colori, dentro una cornice arancione
 * (Ferocious) o viola (Fractal) con la forma della casella di Spectrum, solo se la voce è attiva: è colorata
 * solo la casella di Ferocious o Fractal se è la modalità attiva; con Off nessuna casella è colorata.
 * Il nome della voce attiva è nel suo colore; ogni
 * voce ha un tooltip di una riga con l'effetto. Clic su una voce = come {@code WorkstaffScreen.select}: pacchetto al
 * server, suono PAINTBRUSH_SELECT, chiusura.
 */
public class BidentScreen extends QuickNavigationGridScreen<BidentMenu> {
    /**
     * Angoli delle caselle grandi rispetto al centro della griglia: copia di QuickNavigationGridScreen.SQUARE_OFFSETS
     * (privato), nell'ordine su, destra, giù, sinistra. Le caselle cliccabili sono 32×32, lo sfondo 38×38.
     */
    private static final int[][] SQUARE_OFFSETS = {{-20, -80}, {40, -20}, {-20, 40}, {-80, -20}};
    private static final int UP = 0, RIGHT = 1, LEFT = 3;
    private static final int CLICK_SIZE = 32;
    private static final int BOX_SIZE = 38;

    /** Casella di ogni modalità (indice in SQUARE_OFFSETS). */
    private static final Map<BidentMode, Integer> POSITION = new EnumMap<>(Map.of(
            BidentMode.OFF, UP, BidentMode.FEROCIOUS, LEFT, BidentMode.FRACTAL, RIGHT));

    private static final ResourceLocation FRAME =
            ResourceLocation.fromNamespaceAndPath(SpectralAscension.MOD_ID, "textures/gui/bident_frame.png");

    private final BidentMode active;
    /** Il bident in mano (una copia da 1, senza glint) per le icone di Ferocious e Fractal. */
    private final ItemStack iconStack;

    public BidentScreen(BidentMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        ItemStack held = inventory.player.getItemInHand(menu.hand());
        this.active = SABident.getMode(held);
        this.iconStack = held.copyWithCount(1);
        // Senza il bagliore degli incantesimi: le due icone restano pulite e uguali fra loro.
        this.iconStack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, false);
        this.gridStack.push(new Grid(
                GridEntry.CLOSE,
                entry(BidentMode.OFF),
                entry(BidentMode.FRACTAL),
                GridEntry.EMPTY,
                entry(BidentMode.FEROCIOUS)));
    }

    /**
     * La barriera per Off; per le altre due una casella con un'icona vuota (Air): il bident in mano lo disegna
     * {@link #renderLabels}, perché la casella di Spectrum accetta solo un Item, non lo stack del tier.
     */
    private GridEntry entry(BidentMode mode) {
        return GridEntry.item(mode == BidentMode.OFF ? Items.BARRIER : Items.AIR, label(mode), screen -> select(mode));
    }

    /**
     * Cornice della casella con la stessa forma di quella di Spectrum (angoli più spessi): bident_frame.png è il bordo
     * chiaro della casella 38×38 di quick_navigation.png, con l'interno trasparente e le sfumature in grigio, colorato
     * qui con il colore della modalità.
     */
    private static void drawFrame(GuiGraphics graphics, int x, int y, int argb) {
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(((argb >> 16) & 0xFF) / 255F, ((argb >> 8) & 0xFF) / 255F, (argb & 0xFF) / 255F,
                ((argb >>> 24) & 0xFF) / 255F);
        try {
            graphics.blit(FRAME, x, y, 0, 0, BOX_SIZE, BOX_SIZE, BOX_SIZE, BOX_SIZE);
            graphics.flush();
        } finally {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
        }
    }

    private static int rgb(BidentMode mode) {
        Integer c = mode.color().getColor();
        return c == null ? 0xFFFFFF : c;
    }

    /** Nome sotto la casella: bianco come le voci della workstaff, nel colore della modalità se è quella attiva. */
    private Component label(BidentMode mode) {
        Component name = Component.translatable("item." + SpectralAscension.MOD_ID + ".bident.mode." + mode.getSerializedName());
        return mode == active && mode != BidentMode.OFF ? name.copy().withStyle(mode.color()) : name;
    }

    /** Come WorkstaffScreen.select: la scelta va al server, suono di selezione, il menu si chiude. */
    private static void select(BidentMode mode) {
        PacketDistributor.sendToServer(new BidentModePayload(mode.ordinal()));
        Minecraft client = Minecraft.getInstance();
        client.level.playSound(null, client.player.blockPosition(), SpectrumSoundEvents.PAINTBRUSH_SELECT, SoundSource.NEUTRAL, 0.6F, 1.0F);
        client.player.closeContainer();
    }

    /**
     * Dopo la griglia: il bident del tier nelle caselle di Ferocious e Fractal e le cornici nel colore della modalità,
     * solo attorno a Ferocious o Fractal se è la voce attiva; con Off tutte restano grigie come nella workstaff.
     */
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        for (BidentMode mode : BidentMode.values()) {
            int[] o = SQUARE_OFFSETS[POSITION.get(mode)];
            int x = imageWidth / 2 + o[0];
            int y = imageHeight / 2 + o[1];
            if (mode != BidentMode.OFF) {
                graphics.renderItem(iconStack, x + 10, y + 10);
            }
            if (mode == active && mode != BidentMode.OFF) {
                drawFrame(graphics, x, y, 0xFF000000 | rgb(mode));
            }
        }
    }

    /** Tooltip della voce sotto il mouse: nome (con "attiva") e l'effetto in una riga. */
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        BidentMode hovered = hovered(mouseX, mouseY);
        if (hovered != null) {
            String prefix = "gui." + SpectralAscension.MOD_ID + ".bident_menu.";
            Component name = Component.translatable("item." + SpectralAscension.MOD_ID + ".bident.mode." + hovered.getSerializedName())
                    .withStyle(hovered.color());
            if (hovered == active) {
                name = Component.translatable(prefix + "active", name).withStyle(hovered.color());
            }
            graphics.renderComponentTooltip(font, List.of(name,
                    Component.translatable(prefix + hovered.getSerializedName()).withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
        }
    }

    /** La modalità della casella sotto il mouse (stessa area cliccabile di QuickNavigationGridScreen.mouseClicked). */
    private BidentMode hovered(int mouseX, int mouseY) {
        int x = mouseX - leftPos - imageWidth / 2;
        int y = mouseY - topPos - imageHeight / 2;
        for (Map.Entry<BidentMode, Integer> e : POSITION.entrySet()) {
            int[] offset = SQUARE_OFFSETS[e.getValue()];
            if (x >= offset[0] && x <= offset[0] + CLICK_SIZE && y >= offset[1] && y <= offset[1] + CLICK_SIZE) {
                return e.getKey();
            }
        }
        return null;
    }
}
