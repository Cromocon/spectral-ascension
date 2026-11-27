package cromocon.spectralascension.item;

import cromocon.spectralascension.SpectralAscension;
import cromocon.spectralascension.registry.SAComponents;
import de.dafuqs.spectrum.components.WorkstaffComponent;
import de.dafuqs.spectrum.items.tools.WorkstaffItem;
import de.dafuqs.spectrum.items.tools.WorkstaffItem.GUIToggle;
import de.dafuqs.spectrum.registries.SpectrumDataComponentTypes;
import de.dafuqs.spectrum.registries.SpectrumEnchantmentKeys;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import javax.annotation.Nullable;

/**
 * Le voci del menu della workstaff T7+ ({@code WorkstaffMenu} / {@code SAWorkstaffScreen}): quelle di Spectrum
 * ({@link GUIToggle}, inoltrate a {@link WorkstaffItem#applyToggle}) più le nostre (area 7×7 e 9×9, Foundry e Inventory
 * Insertion). Viaggiano in rete come ordinale di QUESTO enum ({@code WorkstaffOptionPayload}): Spectrum può cambiare i
 * suoi ordinali senza toccarci.
 * <p>
 * Tutto il controllo lato server sta in {@link #apply}: oggetto (una nostra workstaff) e tier (un client modificato non
 * deve ottenere il 9×9 col T7 o Foundry col T6). Il menu aggiunge il controllo d'identità dello stack in mano.
 * <p>
 * Foundry contro Silk Touch (si escludono): scegliendo Silk Touch Foundry si toglie ma resta "accesa" nel componente
 * {@link WorkstaffDrops} (sospesa) e torna da sola scegliendo Fortune o Resonance; accendendo Foundry con Silk Touch
 * attivo si passa prima a Fortune (col livello ricordato da Spectrum), poi si mette Foundry.
 */
public enum WorkstaffOption {
    SELECT_SILK_TOUCH(GUIToggle.SELECT_SILK_TOUCH, SATier.T6),
    SELECT_FORTUNE(GUIToggle.SELECT_FORTUNE, SATier.T6),
    SELECT_RESONANCE(GUIToggle.SELECT_RESONANCE, SATier.T6),
    SELECT_1x1(GUIToggle.SELECT_1x1, SATier.T6),
    SELECT_3x3(GUIToggle.SELECT_3x3, SATier.T6),
    SELECT_5x5(GUIToggle.SELECT_5x5, SATier.T6),
    /** Area 7×7 (spectrum:aoe = 3): dal T7. */
    SELECT_7x7(null, SATier.T7),
    /** Area 9×9 (spectrum:aoe = 4): solo Alloy (T9). */
    SELECT_9x9(null, SATier.ALLOY),
    ENABLE_RIGHT_CLICK_ACTIONS(GUIToggle.ENABLE_RIGHT_CLICK_ACTIONS, SATier.T6),
    DISABLE_RIGHT_CLICK_ACTIONS(GUIToggle.DISABLE_RIGHT_CLICK_ACTIONS, SATier.T6),
    ENABLE_PROJECTILES(GUIToggle.ENABLE_PROJECTILES, SATier.T6),
    DISABLE_PROJECTILES(GUIToggle.DISABLE_PROJECTILES, SATier.T6),
    FOUNDRY_ON(null, SATier.T7),
    FOUNDRY_OFF(null, SATier.T7),
    INSERTION_ON(null, SATier.T8),
    INSERTION_OFF(null, SATier.T8);

    /** Prefisso dei messaggi sopra la hotbar delle nostre voci. */
    private static final String MESSAGE = "item." + SpectralAscension.MOD_ID + ".workstaff.message.";

    @Nullable
    private final GUIToggle spectrum;
    private final SATier minTier;

    WorkstaffOption(@Nullable GUIToggle spectrum, SATier minTier) {
        this.spectrum = spectrum;
        this.minTier = minTier;
    }

    /** La voce è permessa alla workstaff di questo tier (stesso controllo della schermata e del server). */
    public boolean allowedFor(SATier tier) {
        return tier.ordinal() >= minTier.ordinal();
    }

    /** Decodifica un ordinale arrivato dalla rete: null se fuori dai limiti. */
    @Nullable
    public static WorkstaffOption byIndex(int index) {
        WorkstaffOption[] values = values();
        return index >= 0 && index < values.length ? values[index] : null;
    }

    /**
     * Applica la voce allo stack (sul server). Ritorna false, senza toccare niente, se lo stack non è una nostra
     * workstaff o se il suo tier non permette la voce.
     */
    public static boolean apply(Player player, ItemStack stack, WorkstaffOption option) {
        if (!(stack.getItem() instanceof SATools.Workstaff staff) || !option.allowedFor(staff.tier())) {
            return false;
        }
        HolderLookup.Provider registries = player.level().registryAccess();
        switch (option) {
            case SELECT_SILK_TOUCH -> {
                // Foundry e Silk Touch si escludono: Foundry si toglie prima (resta accesa nel componente = sospesa),
                // altrimenti applyToggle rifiuterebbe Silk Touch ("conflicting enchantments").
                boolean suspends = staff.tier().ordinal() >= SATier.T7.ordinal()
                        && has(stack, SpectrumEnchantmentKeys.FOUNDRY) && !has(stack, Enchantments.SILK_TOUCH);
                setLevel(registries, stack, SpectrumEnchantmentKeys.FOUNDRY, 0);
                WorkstaffItem.applyToggle(player, stack, GUIToggle.SELECT_SILK_TOUCH);
                syncDropEnchantments(registries, stack);
                if (suspends && has(stack, Enchantments.SILK_TOUCH)) {
                    player.displayClientMessage(Component.translatable(MESSAGE + "silk_touch_foundry_paused"), true);
                }
            }
            case SELECT_FORTUNE, SELECT_RESONANCE -> {
                // applyToggle toglie Silk Touch: Foundry, se era accesa, torna.
                WorkstaffItem.applyToggle(player, stack, option.spectrum);
                syncDropEnchantments(registries, stack);
            }
            case SELECT_7x7 -> {
                stack.set(SpectrumDataComponentTypes.AOE, 3);
                player.displayClientMessage(Component.translatable(MESSAGE + "7x7"), true);
            }
            case SELECT_9x9 -> {
                stack.set(SpectrumDataComponentTypes.AOE, 4);
                player.displayClientMessage(Component.translatable(MESSAGE + "9x9"), true);
            }
            case FOUNDRY_ON -> {
                stack.set(SAComponents.WORKSTAFF_DROPS.get(), drops(stack).withFoundry(true));
                boolean switched = false;
                if (has(stack, Enchantments.SILK_TOUCH)) {
                    // Decisione dell'utente: con Silk Touch attivo si passa prima a Fortune, poi si accende Foundry.
                    WorkstaffItem.applyToggle(player, stack, GUIToggle.SELECT_FORTUNE);
                    switched = !has(stack, Enchantments.SILK_TOUCH);
                }
                syncDropEnchantments(registries, stack);
                player.displayClientMessage(Component.translatable(MESSAGE + (switched ? "foundry_on_fortune" : "foundry_on")), true);
            }
            case FOUNDRY_OFF -> {
                stack.set(SAComponents.WORKSTAFF_DROPS.get(), drops(stack).withFoundry(false));
                syncDropEnchantments(registries, stack);
                player.displayClientMessage(Component.translatable(MESSAGE + "foundry_off"), true);
            }
            case INSERTION_ON, INSERTION_OFF -> {
                boolean on = option == INSERTION_ON;
                stack.set(SAComponents.WORKSTAFF_DROPS.get(), drops(stack).withInsertion(on));
                syncDropEnchantments(registries, stack);
                player.displayClientMessage(Component.translatable(MESSAGE + (on ? "insertion_on" : "insertion_off")), true);
            }
            default -> WorkstaffItem.applyToggle(player, stack, option.spectrum);
        }
        return true;
    }

    /** Gli interruttori dello stack (default: tutto acceso). */
    public static WorkstaffDrops drops(ItemStack stack) {
        return stack.getOrDefault(SAComponents.WORKSTAFF_DROPS.get(), WorkstaffDrops.DEFAULT);
    }

    /** Foundry accesa ma sospesa perché è selezionato Silk Touch (per il tooltip e la schermata). */
    public static boolean foundryPaused(ItemStack stack) {
        return stack.getItem() instanceof SATools.Workstaff staff && staff.tier().ordinal() >= SATier.T7.ordinal()
                && drops(stack).foundry() && has(stack, Enchantments.SILK_TOUCH);
    }

    /**
     * Allinea gli incantesimi Foundry e Inventory Insertion dello stack al componente {@link WorkstaffDrops} e al tier:
     * Foundry c'è se il tier è T7+, è accesa e non c'è Silk Touch; Inventory Insertion c'è se il tier è T8+ ed è accesa.
     * Sotto il tier non tocca niente (un T6 con Foundry messa all'incudine la tiene). Usato dal menu e dopo la Fusion
     * Shrine (GatedSpectrumRecipeMixin), che rimette gli incantesimi del result anche se il pezzo di partenza li aveva
     * spenti. {@code registries} può essere null: si prende quello del server, e se manca si può solo togliere.
     */
    public static void syncDropEnchantments(@Nullable HolderLookup.Provider registries, ItemStack stack) {
        if (!(stack.getItem() instanceof SATools.Workstaff staff)) {
            return;
        }
        if (registries == null && ServerLifecycleHooks.getCurrentServer() != null) {
            registries = ServerLifecycleHooks.getCurrentServer().registryAccess();
        }
        WorkstaffDrops drops = drops(stack);
        if (staff.tier().ordinal() >= SATier.T7.ordinal()) {
            setLevel(registries, stack, SpectrumEnchantmentKeys.FOUNDRY,
                    drops.foundry() && !has(stack, Enchantments.SILK_TOUCH) ? 1 : 0);
        }
        if (staff.tier().ordinal() >= SATier.T8.ordinal()) {
            setLevel(registries, stack, SpectrumEnchantmentKeys.INVENTORY_INSERTION, drops.insertion() ? 1 : 0);
        }
    }

    /** Conserva la modalità selezionata fra tier. La Fortune della ricetta resta disponibile nel menu. */
    public static void preserveMiningMode(ItemStack input, ItemStack result) {
        if (has(input, Enchantments.FORTUNE)) return;

        var inputEnchantments = input.getTagEnchantments();
        var activeModes = inputEnchantments.keySet().stream()
                .filter(WorkstaffOption::isAlternateMiningMode).toList();
        if (activeModes.size() != 1) return;

        var selectedMode = activeModes.getFirst();
        if (!result.supportsEnchantment(selectedMode)) return;

        var resultEnchantments = result.getTagEnchantments();
        boolean suspendsFoundry = selectedMode.is(Enchantments.SILK_TOUCH)
                && result.getItem() instanceof SATools.Workstaff staff && staff.tier().ordinal() >= SATier.T7.ordinal();
        if (resultEnchantments.keySet().stream()
                .filter(holder -> !holder.equals(selectedMode) && !holder.is(Enchantments.FORTUNE))
                .filter(holder -> !suspendsFoundry || !holder.is(SpectrumEnchantmentKeys.FOUNDRY))
                .anyMatch(holder -> !Enchantment.areCompatible(holder, selectedMode))) return;

        int fortuneLevel = resultEnchantments.keySet().stream().filter(holder -> holder.is(Enchantments.FORTUNE))
                .mapToInt(resultEnchantments::getLevel).findFirst().orElse(0);
        result.update(SpectrumDataComponentTypes.WORKSTAFF, WorkstaffComponent.DEFAULT, component ->
                new WorkstaffComponent(component.canTill(), component.canShoot(), Math.max(component.fortuneLevel(), fortuneLevel)));
        EnchantmentHelper.updateEnchantments(result, enchantments -> {
            enchantments.removeIf(holder -> holder.is(Enchantments.FORTUNE));
            enchantments.upgrade(selectedMode, inputEnchantments.getLevel(selectedMode));
        });
    }

    private static boolean isAlternateMiningMode(Holder<Enchantment> enchantment) {
        return enchantment.is(Enchantments.SILK_TOUCH) || enchantment.is(SpectrumEnchantmentKeys.RESONANCE);
    }

    /** Lo stack ha l'incantesimo (senza bisogno del registro). */
    public static boolean has(ItemStack stack, ResourceKey<Enchantment> key) {
        return stack.getTagEnchantments().keySet().stream().anyMatch(h -> h.is(key));
    }

    /** Mette (livello > 0, se manca) o toglie (0) un incantesimo; senza registro può solo togliere. */
    private static void setLevel(@Nullable HolderLookup.Provider registries, ItemStack stack, ResourceKey<Enchantment> key, int level) {
        if (level <= 0) {
            if (has(stack, key)) {
                EnchantmentHelper.updateEnchantments(stack, m -> m.removeIf(h -> h.is(key)));
            }
        } else if (!has(stack, key) && registries != null) {
            var holder = registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
            EnchantmentHelper.updateEnchantments(stack, m -> m.set(holder, level));
        }
    }
}
