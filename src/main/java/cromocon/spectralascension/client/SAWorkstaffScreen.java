package cromocon.spectralascension.client;

import cromocon.spectralascension.SpectralAscension;
import cromocon.spectralascension.item.SATier;
import cromocon.spectralascension.item.SATools;
import cromocon.spectralascension.item.WorkstaffDrops;
import cromocon.spectralascension.item.WorkstaffOption;
import cromocon.spectralascension.menu.WorkstaffMenu;
import cromocon.spectralascension.network.WorkstaffOptionPayload;
import de.dafuqs.spectrum.inventories.QuickNavigationGridScreen;
import de.dafuqs.spectrum.items.tools.GlassCrestWorkstaffItem;
import de.dafuqs.spectrum.registries.SpectrumBlocks;
import de.dafuqs.spectrum.registries.SpectrumItems;
import de.dafuqs.spectrum.registries.SpectrumSoundEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Menu della workstaff T7+ ({@link WorkstaffMenu}): l'aspetto della griglia "quick navigation" di Spectrum (stesse
 * caselle e testi), ma con le voci sui vertici di un poligono regolare ({@link WorkstaffMenuLayout}: primo vertice in
 * alto, poi in senso orario; 2 voci sinistra/destra) e, nelle sotto-pagine, "Indietro" al centro in una casella piccola.
 * La griglia di Spectrum ha posti fissi (su/destra/giù/sinistra) e metodi di disegno non accessibili: è ridisegnata qui.
 * <pre>
 * principale:  su Area, destra tasto destro on/off, giù proiettili on/off, sinistra Incantesimi; centro (puntino) chiude
 * Area:        su 1x1, destra 5x5, giù "Area più grande", sinistra 3x3
 *   Area più grande: 7x7, 9x9 (solo Alloy; senza, 7x7 da sola in alto)
 * Incantesimi: su Silk Touch, destra Resonance, giù "Drop", sinistra Fortune
 *   Drop:        Foundry on/off, Inventory Insertion on/off (solo T8+; senza, Foundry da sola in alto)
 * </pre>
 * Le voci compaiono solo se il tier le permette (lo stesso controllo, {@link WorkstaffOption#allowedFor}, lo rifà il
 * server). Clic su una voce = come {@code WorkstaffScreen.select}: pacchetto al server ({@link WorkstaffOptionPayload}),
 * suono, chiusura. Tasti di movimento: la voce nella direzione premuta ({@link WorkstaffMenuLayout#pick}); Drop/Inventario:
 * il centro; tasto destro: indietro (nella principale chiude). Le voci nuove hanno un tooltip.
 */
public class SAWorkstaffScreen extends AbstractContainerScreen<WorkstaffMenu> {
    private static final String GUI = "gui." + SpectralAscension.MOD_ID + ".workstaff_menu.";
    private static final Component CONTROLS_1 = Component.translatable("gui.spectrum.quick_navigation.controls1");
    private static final Component CONTROLS_2 = Component.translatable("gui.spectrum.quick_navigation.controls2");

    /** Una voce: icona (oggetto) o testo nella casella, etichetta sotto, azione, tooltip (vuoto = nessuno). */
    private record Entry(@Nullable ItemStack icon, @Nullable Component inner, Component label, Runnable action, List<Component> tooltip) {
    }

    /** Una pagina: voci in senso orario dal vertice in alto; main = griglia principale (centro che chiude). */
    private record Page(List<Entry> options, boolean main) {
    }

    private final Deque<Page> pages = new ArrayDeque<>();
    private final Entry back = new Entry(null, Component.literal("\u2190"), Component.translatable(GUI + "back"), this::back, List.of());

    public SAWorkstaffScreen(WorkstaffMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageHeight = 256; // come QuickNavigationGridScreen
        ItemStack stack = inventory.player.getItemInHand(menu.hand());
        SATier tier = stack.getItem() instanceof SATools.Workstaff staff ? staff.tier() : SATier.T7;
        boolean free = stack.getItem() instanceof SATools.Workstaff staff && staff.hasFreeAoE();
        WorkstaffDrops drops = WorkstaffOption.drops(stack);
        boolean silk = WorkstaffOption.has(stack, Enchantments.SILK_TOUCH);

        // --- Area più grande: 7x7 (T7+), 9x9 (solo Alloy) ---
        List<Entry> large = new ArrayList<>();
        large.add(text("7x7", Component.translatable(GUI + "7x7"), () -> select(WorkstaffOption.SELECT_7x7),
                Component.translatable(GUI + "7x7"), cost(free, 3)));
        boolean nine = WorkstaffOption.SELECT_9x9.allowedFor(tier);
        if (nine) {
            large.add(text("9x9", Component.translatable(GUI + "9x9"), () -> select(WorkstaffOption.SELECT_9x9),
                    Component.translatable(GUI + "9x9"), cost(true, 4)));
        }
        Page largePage = new Page(large, false);

        // --- Area: come Spectrum, più l'area più grande ---
        Page rangePage = new Page(List.of(
                text("1x1", Component.translatable("item.spectrum.workstaff.gui.1x1"), () -> select(WorkstaffOption.SELECT_1x1)),
                text("5x5", Component.translatable("item.spectrum.workstaff.gui.5x5"), () -> select(WorkstaffOption.SELECT_5x5)),
                item(Items.DEEPSLATE, Component.translatable(GUI + "larger_area"), () -> open(largePage),
                        Component.translatable(GUI + "larger_area"),
                        Component.translatable(GUI + (nine ? "larger_area.tooltip_9x9" : "larger_area.tooltip_7x7")).withStyle(ChatFormatting.GRAY)),
                text("3x3", Component.translatable("item.spectrum.workstaff.gui.3x3"), () -> select(WorkstaffOption.SELECT_3x3))), false);

        // --- Drop: Foundry (T7+), Inventory Insertion (T8+) ---
        List<Component> foundryTip = new ArrayList<>(List.of(
                Component.translatable("enchantment.spectrum.foundry").withStyle(ChatFormatting.GOLD),
                Component.translatable(GUI + "foundry.tooltip").withStyle(ChatFormatting.GRAY)));
        if (drops.foundry() && silk) {
            foundryTip.add(Component.translatable(GUI + "foundry.tooltip_paused").withStyle(ChatFormatting.YELLOW));
        } else if (!drops.foundry() && silk) {
            foundryTip.add(Component.translatable(GUI + "foundry.tooltip_to_fortune").withStyle(ChatFormatting.YELLOW));
        }
        Component[] foundryLines = foundryTip.toArray(Component[]::new);
        List<Entry> dropEntries = new ArrayList<>();
        dropEntries.add(drops.foundry()
                ? item(Items.BLAST_FURNACE, Component.translatable(GUI + "disable_foundry"), () -> select(WorkstaffOption.FOUNDRY_OFF), foundryLines)
                : item(Items.FURNACE, Component.translatable(GUI + "enable_foundry"), () -> select(WorkstaffOption.FOUNDRY_ON), foundryLines));
        boolean insertionAllowed = WorkstaffOption.INSERTION_ON.allowedFor(tier);
        if (insertionAllowed) {
            Component[] tip = {Component.translatable("enchantment.spectrum.inventory_insertion").withStyle(ChatFormatting.GOLD),
                    Component.translatable(GUI + "insertion.tooltip").withStyle(ChatFormatting.GRAY)};
            dropEntries.add(drops.insertion()
                    ? item(Items.ENDER_CHEST, Component.translatable(GUI + "disable_insertion"), () -> select(WorkstaffOption.INSERTION_OFF), tip)
                    : item(Items.CHEST, Component.translatable(GUI + "enable_insertion"), () -> select(WorkstaffOption.INSERTION_ON), tip));
        }
        Page dropPage = new Page(dropEntries, false);

        // --- Incantesimi: come Spectrum, più i drop ---
        Component[] silkTip = !drops.foundry() ? new Component[0] : new Component[]{
                Component.translatable("item.spectrum.workstaff.gui.silk_touch"),
                Component.translatable(GUI + (silk ? "foundry.tooltip_paused" : "silk_touch.tooltip_pauses_foundry"))
                        .withStyle(silk ? ChatFormatting.YELLOW : ChatFormatting.GRAY)};
        Page enchantmentPage = new Page(List.of(
                item(Items.FEATHER, Component.translatable("item.spectrum.workstaff.gui.silk_touch"), () -> select(WorkstaffOption.SELECT_SILK_TOUCH), silkTip),
                item(SpectrumItems.RESONANCE_SHARD.get(), Component.translatable("item.spectrum.workstaff.gui.resonance"), () -> select(WorkstaffOption.SELECT_RESONANCE)),
                item(Items.RAW_IRON, Component.translatable(GUI + "drops"), () -> open(dropPage),
                        Component.translatable(GUI + "drops"),
                        Component.translatable(GUI + (insertionAllowed ? "drops.tooltip_t8" : "drops.tooltip_t7")).withStyle(ChatFormatting.GRAY)),
                item(SpectrumBlocks.FOUR_LEAF_CLOVER.asItem(), Component.translatable("item.spectrum.workstaff.gui.fortune"), () -> select(WorkstaffOption.SELECT_FORTUNE))), false);

        // --- Principale: come la glass crest di Spectrum ---
        boolean canTill = stack.getItem() instanceof SATools.Workstaff staff && staff.canTill(stack);
        pages.push(new Page(List.of(
                item(Items.STONE, Component.translatable("item.spectrum.workstaff.gui.range_group"), () -> open(rangePage)),
                canTill
                        ? item(Items.WOODEN_HOE, Component.translatable("item.spectrum.workstaff.gui.disable_right_click_actions"), () -> select(WorkstaffOption.DISABLE_RIGHT_CLICK_ACTIONS))
                        : item(SpectrumItems.MULTITOOL.get(), Component.translatable("item.spectrum.workstaff.gui.enable_right_click_actions"), () -> select(WorkstaffOption.ENABLE_RIGHT_CLICK_ACTIONS)),
                GlassCrestWorkstaffItem.canShoot(stack)
                        ? item(Items.SPECTRAL_ARROW, Component.translatable("item.spectrum.workstaff.gui.disable_projectiles"), () -> select(WorkstaffOption.DISABLE_PROJECTILES))
                        : item(Items.ARROW, Component.translatable("item.spectrum.workstaff.gui.enable_projectiles"), () -> select(WorkstaffOption.ENABLE_PROJECTILES)),
                item(Items.ENCHANTED_BOOK, Component.translatable("item.spectrum.workstaff.gui.enchantment_group"), () -> open(enchantmentPage))), true));
    }

    private static Entry item(Item item, Component label, Runnable action, Component... tooltip) {
        return new Entry(item.getDefaultInstance(), null, label, action, List.of(tooltip));
    }

    private static Entry text(String inner, Component label, Runnable action, Component... tooltip) {
        return new Entry(null, Component.literal(inner), label, action, List.of(tooltip));
    }

    /** Riga del costo: "%s White Ink per blocco" (stessa chiave del tooltip dell'oggetto) o "gratuito". */
    private static Component cost(boolean free, int range) {
        return (free ? Component.translatable("item." + SpectralAscension.MOD_ID + ".workstaff.aoe_free")
                : Component.translatable("item." + SpectralAscension.MOD_ID + ".workstaff.aoe_cost", SATools.Workstaff.aoeInkCost(range)))
                .withStyle(ChatFormatting.GRAY);
    }

    /** Come WorkstaffScreen.select: la scelta va al server, suono di selezione, il menu si chiude. */
    private static void select(WorkstaffOption option) {
        PacketDistributor.sendToServer(new WorkstaffOptionPayload(option.ordinal()));
        Minecraft client = Minecraft.getInstance();
        client.level.playSound(null, client.player.blockPosition(), SpectrumSoundEvents.PAINTBRUSH_SELECT, SoundSource.NEUTRAL, 0.6F, 1.0F);
        client.player.closeContainer();
    }

    private void switchSound() {
        minecraft.level.playSound(null, minecraft.player.blockPosition(), SpectrumSoundEvents.PAINTBRUSH_SWITCH, SoundSource.NEUTRAL, 0.5F, 1.0F);
    }

    /** Come QuickNavigationGridScreen.selectGrid. */
    private void open(Page page) {
        switchSound();
        pages.push(page);
    }

    /** Come QuickNavigationGridScreen.back: pagina precedente, o chiude dalla principale. */
    private void back() {
        switchSound();
        if (pages.size() == 1) {
            minecraft.player.closeContainer();
        } else {
            pages.pop();
        }
    }

    /** Il centro: nella principale chiude (come GridEntry.CLOSE di Spectrum), altrove torna indietro. */
    private void center() {
        if (pages.peek().main()) {
            onClose();
        } else {
            back();
        }
    }

    /** Vertici della pagina corrente (relativi al centro) e posizione del testo dei comandi, per questa finestra. */
    private record Layout(int cx, int cy, int[][] v, int controlsY) {
    }

    private Layout layout() {
        Page page = pages.peek();
        int[] labels = page.options().stream().mapToInt(e -> font.width(e.label())).toArray();
        int centerLabel = page.main() ? -1 : font.width(back.label());
        int cx = leftPos + imageWidth / 2, cy = topPos + imageHeight / 2;
        // sotto resta posto per le due righe dei comandi (3 px sotto le etichette, y e y + 10, alte 9); se non c'è (finestra bassa, es. 1280×720
        // a GUI 3 = 240 px), le voci stanno almeno nella finestra e i comandi si stringono in fondo
        int r = WorkstaffMenuLayout.radius(labels, centerLabel, new int[]{-cx, -cy, width - cx - 1, height - cy - 22});
        if (r < 0) r = WorkstaffMenuLayout.radius(labels, centerLabel, new int[]{-cx, -cy, width - cx - 1, height - cy - 1});
        // shortcut: finestra minuscola, nessun raggio va bene: si sovrappone al raggio minimo, come la griglia di Spectrum
        if (r < 0) r = WorkstaffMenuLayout.MIN_RADIUS;
        int bottom = WorkstaffMenuLayout.bottom(WorkstaffMenuLayout.rects(r, labels, centerLabel));
        int controlsY = Math.min(Math.max(topPos + 228, cy + bottom + 3), height - 19);
        return new Layout(cx, cy, WorkstaffMenuLayout.vertices(labels.length, r), controlsY);
    }

    /** Voce sotto il mouse: indice della voce, options.size() = centro, -1 nessuna. */
    private int hovered(Layout l, double mouseX, double mouseY) {
        Page page = pages.peek();
        double x = mouseX - l.cx(), y = mouseY - l.cy();
        for (int i = 0; i < l.v().length; i++) {
            if (Math.abs(x - l.v()[i][0]) <= WorkstaffMenuLayout.BOX / 2 && Math.abs(y - l.v()[i][1]) <= WorkstaffMenuLayout.BOX / 2) {
                return i;
            }
        }
        int c = (page.main() ? WorkstaffMenuLayout.DOT : WorkstaffMenuLayout.SMALL) / 2;
        return Math.abs(x) <= c && Math.abs(y) <= c ? l.v().length : -1;
    }

    private void activate(int index) {
        if (index == pages.peek().options().size()) {
            center();
        } else if (index >= 0) {
            pages.peek().options().get(index).action().run();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1) {
            back();
            return true;
        }
        int index = hovered(layout(), mouseX, mouseY);
        if (button == 0 && index >= 0) {
            activate(index);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        Options options = minecraft.options;
        int dx = 0, dy = 0;
        if (options.keyLeft.matches(keyCode, scanCode)) dx = -1;
        else if (options.keyRight.matches(keyCode, scanCode)) dx = 1;
        else if (options.keyUp.matches(keyCode, scanCode)) dy = -1;
        else if (options.keyDown.matches(keyCode, scanCode)) dy = 1;
        else if (options.keyDrop.matches(keyCode, scanCode) || options.keyInventory.matches(keyCode, scanCode)) {
            center();
            return true;
        } else {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        activate(WorkstaffMenuLayout.pick(layout().v(), dx, dy));
        return true;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
    }

    /** Niente titolo né "Inventario" (come QuickNavigationGridScreen): il disegno è in render. */
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        Layout l = layout();
        Page page = pages.peek();
        int color = QuickNavigationGridScreen.TEXT_COLOR;
        // stesse coordinate nella texture e stessi scostamenti delle caselle di QuickNavigationGridScreen.GridEntry
        if (page.main()) {
            graphics.blit(QuickNavigationGridScreen.BACKGROUND, l.cx() - 5, l.cy() - 5, 0, 0, 10, 10);
        } else {
            int x = l.cx() - 14, y = l.cy() - 14;
            graphics.blit(QuickNavigationGridScreen.BACKGROUND, x, y, 48, 0, 28, 28);
            graphics.drawCenteredString(font, back.inner(), x + 14, y + 10, color);
            graphics.drawCenteredString(font, back.label(), x + 14, y + 34, color);
        }
        for (int i = 0; i < l.v().length; i++) {
            Entry e = page.options().get(i);
            int x = l.cx() + l.v()[i][0] - 19, y = l.cy() + l.v()[i][1] - 19;
            graphics.blit(QuickNavigationGridScreen.BACKGROUND, x, y, 10, 0, 38, 38);
            if (e.icon() != null) {
                graphics.renderItem(e.icon(), x + 10, y + 10);
            } else {
                graphics.drawCenteredString(font, e.inner(), x + 19, y + 15, color);
            }
            graphics.drawCenteredString(font, e.label(), x + 19, y + 40, color);
        }
        graphics.drawCenteredString(font, CONTROLS_1, l.cx(), l.controlsY(), color);
        graphics.drawCenteredString(font, CONTROLS_2, l.cx(), l.controlsY() + 10, color);

        int hovered = hovered(l, mouseX, mouseY);
        if (hovered >= 0 && hovered < l.v().length && !page.options().get(hovered).tooltip().isEmpty()) {
            graphics.renderComponentTooltip(font, page.options().get(hovered).tooltip(), mouseX, mouseY);
        }
    }
}
