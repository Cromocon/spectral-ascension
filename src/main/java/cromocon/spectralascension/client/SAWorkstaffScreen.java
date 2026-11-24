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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Menu della workstaff T7+ ({@link WorkstaffMenu}): la stessa griglia a croce di {@code WorkstaffScreen} di Spectrum
 * (griglie ricostruite qui: quelle di Spectrum sono private), più due sotto-griglie raggiunte dal CENTRO, che in
 * Spectrum è vuoto:
 * <pre>
 * principale:  centro chiudi, su Area, destra tasto destro on/off, giù proiettili on/off, sinistra Incantesimi
 * Area:        su 1x1, sinistra 3x3, destra 5x5, giù indietro, centro "Area più grande" →
 *   Area più grande: su 7x7, destra 9x9 (solo Alloy, altrimenti vuota), giù indietro
 * Incantesimi: su Silk Touch, sinistra Fortune, giù Resonance, destra indietro, centro "Drop" →
 *   Drop:        su Foundry on/off, sinistra Inventory Insertion on/off (solo T8+, altrimenti vuota), giù indietro
 * </pre>
 * Le voci compaiono solo se il tier le permette (lo stesso controllo, {@link WorkstaffOption#allowedFor}, lo rifà il
 * server). Clic su una voce = come {@code WorkstaffScreen.select}: pacchetto al server ({@link WorkstaffOptionPayload}),
 * suono, chiusura. Le voci nuove hanno un tooltip (costo dell'area, cosa fanno Foundry e Inventory Insertion).
 */
public class SAWorkstaffScreen extends QuickNavigationGridScreen<WorkstaffMenu> {
    /** Copia di QuickNavigationGridScreen.SQUARE_OFFSETS (privato): su, destra, giù, sinistra; caselle 32×32. */
    private static final int[][] SQUARE_OFFSETS = {{-20, -80}, {40, -20}, {-20, 40}, {-80, -20}};
    private static final int CLICK_SIZE = 32;
    private static final String GUI = "gui." + SpectralAscension.MOD_ID + ".workstaff_menu.";

    /** Tooltip per griglia: indice 0 centro, 1 su, 2 destra, 3 giù, 4 sinistra (null = nessuno). */
    private final Map<Grid, List<Component>[]> tooltips = new IdentityHashMap<>();

    public SAWorkstaffScreen(WorkstaffMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        ItemStack stack = inventory.player.getItemInHand(menu.hand());
        SATier tier = stack.getItem() instanceof SATools.Workstaff staff ? staff.tier() : SATier.T7;
        boolean free = stack.getItem() instanceof SATools.Workstaff staff && staff.hasFreeAoE();
        WorkstaffDrops drops = WorkstaffOption.drops(stack);
        boolean silk = WorkstaffOption.has(stack, Enchantments.SILK_TOUCH);

        // --- Area più grande: 7x7 (T7+), 9x9 (solo Alloy) ---
        GridEntry nine = WorkstaffOption.SELECT_9x9.allowedFor(tier)
                ? GridEntry.text(Component.literal("9x9"), Component.translatable(GUI + "9x9"), s -> select(WorkstaffOption.SELECT_9x9))
                : GridEntry.EMPTY;
        Grid largeGrid = new Grid(GridEntry.EMPTY,
                GridEntry.text(Component.literal("7x7"), Component.translatable(GUI + "7x7"), s -> select(WorkstaffOption.SELECT_7x7)),
                nine, GridEntry.BACK, GridEntry.EMPTY);
        tooltip(largeGrid, 1, Component.translatable(GUI + "7x7"), cost(free, 3));
        if (nine != GridEntry.EMPTY) {
            tooltip(largeGrid, 2, Component.translatable(GUI + "9x9"), cost(true, 4));
        }

        // --- Area: come Spectrum, col centro che porta all'area più grande ---
        Grid rangeGrid = new Grid(
                GridEntry.item(Items.DEEPSLATE, Component.translatable(GUI + "larger_area"), s -> this.selectGrid(largeGrid)),
                GridEntry.text(Component.literal("1x1"), Component.translatable("item.spectrum.workstaff.gui.1x1"), s -> select(WorkstaffOption.SELECT_1x1)),
                GridEntry.text(Component.literal("5x5"), Component.translatable("item.spectrum.workstaff.gui.5x5"), s -> select(WorkstaffOption.SELECT_5x5)),
                GridEntry.BACK,
                GridEntry.text(Component.literal("3x3"), Component.translatable("item.spectrum.workstaff.gui.3x3"), s -> select(WorkstaffOption.SELECT_3x3)));
        tooltip(rangeGrid, 0, Component.translatable(GUI + "larger_area"),
                Component.translatable(GUI + (nine != GridEntry.EMPTY ? "larger_area.tooltip_9x9" : "larger_area.tooltip_7x7"))
                        .withStyle(ChatFormatting.GRAY));

        // --- Drop: Foundry (T7+), Inventory Insertion (T8+) ---
        GridEntry foundry = drops.foundry()
                ? GridEntry.item(Items.BLAST_FURNACE, Component.translatable(GUI + "disable_foundry"), s -> select(WorkstaffOption.FOUNDRY_OFF))
                : GridEntry.item(Items.FURNACE, Component.translatable(GUI + "enable_foundry"), s -> select(WorkstaffOption.FOUNDRY_ON));
        boolean insertionAllowed = WorkstaffOption.INSERTION_ON.allowedFor(tier);
        GridEntry insertion = !insertionAllowed ? GridEntry.EMPTY : drops.insertion()
                ? GridEntry.item(Items.ENDER_CHEST, Component.translatable(GUI + "disable_insertion"), s -> select(WorkstaffOption.INSERTION_OFF))
                : GridEntry.item(Items.CHEST, Component.translatable(GUI + "enable_insertion"), s -> select(WorkstaffOption.INSERTION_ON));
        Grid dropGrid = new Grid(GridEntry.EMPTY, foundry, GridEntry.EMPTY, GridEntry.BACK, insertion);
        List<Component> foundryTip = new ArrayList<>(List.of(
                Component.translatable("enchantment.spectrum.foundry").withStyle(ChatFormatting.GOLD),
                Component.translatable(GUI + "foundry.tooltip").withStyle(ChatFormatting.GRAY)));
        if (drops.foundry() && silk) {
            foundryTip.add(Component.translatable(GUI + "foundry.tooltip_paused").withStyle(ChatFormatting.YELLOW));
        } else if (!drops.foundry() && silk) {
            foundryTip.add(Component.translatable(GUI + "foundry.tooltip_to_fortune").withStyle(ChatFormatting.YELLOW));
        }
        tooltips.computeIfAbsent(dropGrid, g -> newTips())[1] = foundryTip;
        if (insertionAllowed) {
            tooltip(dropGrid, 4, Component.translatable("enchantment.spectrum.inventory_insertion").withStyle(ChatFormatting.GOLD),
                    Component.translatable(GUI + "insertion.tooltip").withStyle(ChatFormatting.GRAY));
        }

        // --- Incantesimi: come Spectrum, col centro che porta ai drop ---
        // Il centro NON ha etichetta: sulla stessa riga quella lunga di sinistra ("Switch to Fortune", centrata sotto la
        // casella) la toccherebbe. Il nome ("Drops") e la descrizione sono nel tooltip del centro.
        Grid enchantmentGrid = new Grid(
                GridEntry.item(Items.RAW_IRON, Component.empty(), s -> this.selectGrid(dropGrid)),
                GridEntry.item(Items.FEATHER, Component.translatable("item.spectrum.workstaff.gui.silk_touch"), s -> select(WorkstaffOption.SELECT_SILK_TOUCH)),
                GridEntry.BACK,
                GridEntry.item(SpectrumItems.RESONANCE_SHARD.get(), Component.translatable("item.spectrum.workstaff.gui.resonance"), s -> select(WorkstaffOption.SELECT_RESONANCE)),
                GridEntry.item(SpectrumBlocks.FOUR_LEAF_CLOVER.asItem(), Component.translatable("item.spectrum.workstaff.gui.fortune"), s -> select(WorkstaffOption.SELECT_FORTUNE)));
        tooltip(enchantmentGrid, 0, Component.translatable(GUI + "drops"),
                Component.translatable(GUI + (insertionAllowed ? "drops.tooltip_t8" : "drops.tooltip_t7")).withStyle(ChatFormatting.GRAY));
        if (silk && drops.foundry()) {
            tooltip(enchantmentGrid, 1, Component.translatable("item.spectrum.workstaff.gui.silk_touch"),
                    Component.translatable(GUI + "foundry.tooltip_paused").withStyle(ChatFormatting.YELLOW));
        } else if (drops.foundry()) {
            tooltip(enchantmentGrid, 1, Component.translatable("item.spectrum.workstaff.gui.silk_touch"),
                    Component.translatable(GUI + "silk_touch.tooltip_pauses_foundry").withStyle(ChatFormatting.GRAY));
        }

        // --- Principale: come la glass crest di Spectrum ---
        boolean canTill = stack.getItem() instanceof SATools.Workstaff staff && staff.canTill(stack);
        GridEntry rightClick = canTill
                ? GridEntry.item(Items.WOODEN_HOE, Component.translatable("item.spectrum.workstaff.gui.disable_right_click_actions"), s -> select(WorkstaffOption.DISABLE_RIGHT_CLICK_ACTIONS))
                : GridEntry.item(SpectrumItems.MULTITOOL.get(), Component.translatable("item.spectrum.workstaff.gui.enable_right_click_actions"), s -> select(WorkstaffOption.ENABLE_RIGHT_CLICK_ACTIONS));
        GridEntry projectiles = GlassCrestWorkstaffItem.canShoot(stack)
                ? GridEntry.item(Items.SPECTRAL_ARROW, Component.translatable("item.spectrum.workstaff.gui.disable_projectiles"), s -> select(WorkstaffOption.DISABLE_PROJECTILES))
                : GridEntry.item(Items.ARROW, Component.translatable("item.spectrum.workstaff.gui.enable_projectiles"), s -> select(WorkstaffOption.ENABLE_PROJECTILES));
        this.gridStack.push(new Grid(
                GridEntry.CLOSE,
                GridEntry.item(Items.STONE, Component.translatable("item.spectrum.workstaff.gui.range_group"), s -> this.selectGrid(rangeGrid)),
                rightClick,
                projectiles,
                GridEntry.item(Items.ENCHANTED_BOOK, Component.translatable("item.spectrum.workstaff.gui.enchantment_group"), s -> this.selectGrid(enchantmentGrid))));
    }

    /** Riga del costo: "%s White Ink per blocco" (stessa chiave del tooltip dell'oggetto) o "gratuito". */
    private static Component cost(boolean free, int range) {
        return (free ? Component.translatable("item." + SpectralAscension.MOD_ID + ".workstaff.aoe_free")
                : Component.translatable("item." + SpectralAscension.MOD_ID + ".workstaff.aoe_cost", SATools.Workstaff.aoeInkCost(range)))
                .withStyle(ChatFormatting.GRAY);
    }

    @SuppressWarnings("unchecked")
    private static List<Component>[] newTips() {
        return new List[5];
    }

    private void tooltip(Grid grid, int index, Component... lines) {
        tooltips.computeIfAbsent(grid, g -> newTips())[index] = List.of(lines);
    }

    /** Come WorkstaffScreen.select: la scelta va al server, suono di selezione, il menu si chiude. */
    private static void select(WorkstaffOption option) {
        PacketDistributor.sendToServer(new WorkstaffOptionPayload(option.ordinal()));
        Minecraft client = Minecraft.getInstance();
        client.level.playSound(null, client.player.blockPosition(), SpectrumSoundEvents.PAINTBRUSH_SELECT, SoundSource.NEUTRAL, 0.6F, 1.0F);
        client.player.closeContainer();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        List<Component>[] tips = tooltips.get(current());
        int hovered = hovered(mouseX, mouseY);
        if (tips != null && hovered >= 0 && tips[hovered] != null) {
            graphics.renderComponentTooltip(font, tips[hovered], mouseX, mouseY);
        }
    }

    /** Casella sotto il mouse (stesse aree cliccabili di QuickNavigationGridScreen.mouseClicked): 0 centro, 1-4, -1 nessuna. */
    private int hovered(int mouseX, int mouseY) {
        int x = mouseX - leftPos - imageWidth / 2;
        int y = mouseY - topPos - imageHeight / 2;
        int center = gridStack.size() == 1 ? 5 : 14;
        if (Math.abs(x) <= center && Math.abs(y) <= center) {
            return 0;
        }
        for (int i = 0; i < SQUARE_OFFSETS.length; i++) {
            int[] o = SQUARE_OFFSETS[i];
            if (x >= o[0] && x <= o[0] + CLICK_SIZE && y >= o[1] && y <= o[1] + CLICK_SIZE) {
                return i + 1;
            }
        }
        return -1;
    }
}
