package cromocon.spectralascension.item;

import cromocon.spectralascension.compat.SACurios;
import cromocon.spectralascension.registry.SAComponents;
import de.dafuqs.spectrum.api.item.AzureDikeItem;
import de.dafuqs.spectrum.attachment_types.AzureDikeAttachmentType;
import de.dafuqs.spectrum.items.trinkets.AzureDikeAmuletItem;
import de.dafuqs.spectrum.registries.SpectrumItems;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Il bonus del set completo: con i 4 pezzi Spectral Ascension indossati il giocatore ha l'Azure Dike di tutti
 * gli accessori Dike fusi nei pezzi, e quegli stessi accessori negli slot Curios si disattivano (niente doppio conteggio).
 * Gli altri accessori Dike negli slot Curios (es. Aether-Graced Nectar Gloves, 10) restano attivi e si sommano al set
 * prima del × 2 del Core (decisione 2026-11-27). Lo Shieldgrasp Amulet conta al
 * minimo (2) più il potenziamento a Ink che aveva quando è stato fuso negli stivali T6 (componente
 * spectral_ascension:dike_bonus, 2026-11-27): set da 40 a 60, letto dagli stivali indossati da ogni giocatore.
 * <p>
 * Spectrum calcola il Dike solo dagli slot Curios ({@link AzureDikeAttachmentType#recalculate}). Il nostro mixin
 * (mixin/AzureDikeAttachmentTypeMixin) intercetta quel calcolo: se il set è indossato usa {@link #applySetDike}
 * con i soli accessori Curios non fusi. Il ricalcolo parte quando cambiano gli accessori (Spectrum) o l'armatura ({@link #onEquipmentChange}).
 */
public final class SAArmorSet {
    private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    /** Ink minima perché lo Shieldgrasp Amulet dia Dike (AzureDikeAmuletItem.maxAzureDike): vale 2. */
    private static final long SHIELDGRASP_MIN_INK = 100;

    /**
     * Dike del set: massimo, tick per ricaricare un punto, tick di attesa dopo un colpo.
     * Stessa forma dei valori che {@link AzureDikeAttachmentType#set} riceve da Spectrum.
     */
    public record DikeStats(int max, int ticksPerCharge, int rechargeDelayAfterDamage) {
    }

    /**
     * Parte fissa del set, letta una volta dagli accessori di Spectrum: somma dei Dike (amuleto al minimo), moltiplicatore
     * del Core e ricarica. Sono costanti degli oggetti: il potenziamento a Ink, che cambia da giocatore a giocatore,
     * NON è qui ma sugli stivali indossati ({@link #dikeStats(LivingEntity)}).
     */
    private record Base(Set<Item> fused, int dikeSum, float multiplier, float rechargeSpeedModifier, float rechargeDelayAfterDamageModifier) {
    }

    private static volatile Base base;

    /**
     * Chi guarda il tooltip (il giocatore del client): lo installa SAClient. Sul server resta null, così questa classe
     * non tocca mai classi solo client.
     */
    private static volatile Supplier<Player> tooltipViewer = () -> null;

    private SAArmorSet() {
    }

    public static void setTooltipViewer(Supplier<Player> viewer) {
        tooltipViewer = viewer;
    }

    /** Vero se i 4 slot dell'armatura contengono pezzi Spectral Ascension (di qualsiasi tier). */
    public static boolean isFullSetWorn(LivingEntity entity) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (!(entity.getItemBySlot(slot).getItem() instanceof SAArmorItem)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Somma gli accessori Dike come fa Spectrum in AzureDikeAttachmentType.recalculate, leggendo i valori dagli
     * oggetti stessi (così seguono eventuali cambi di Spectrum): Puff Circlet 4 + Azure Dike Ring 6 +
     * Azure Dike Belt 8 + Shieldgrasp Amulet al minimo 2 = 20, × 2 dell'Azuresque Dike Core = 40.
     * Ricarica: Ring 2,0 e Core 1,5 sulla velocità; Belt 2,5 e Core 1,5 sull'attesa dopo un colpo.
     */
    private static Base base() {
        Base cached = base;
        if (cached != null) {
            return cached;
        }
        // Gli accessori fusi nell'armatura: col set completo, negli slot Curios non contano (sono già nel set).
        List<ItemStack> trinkets = List.of(
                new ItemStack(SpectrumItems.PUFF_CIRCLET.get()),
                new ItemStack(SpectrumItems.AZURE_DIKE_RING.get()),
                new ItemStack(SpectrumItems.AZURE_DIKE_BELT.get()),
                new ItemStack(SpectrumItems.SHIELDGRASP_AMULET.get()),
                new ItemStack(SpectrumItems.AZURESQUE_DIKE_CORE.get()));
        int maxAzureDike = 0;
        float rechargeSpeedModifier = 1F;
        float rechargeDelayAfterDamageModifier = 1F;
        float maxAzureDikeMultiplier = 1F;
        for (ItemStack stack : trinkets) {
            AzureDikeItem item = (AzureDikeItem) stack.getItem();
            // Senza Ink l'amuleto darebbe 0: contiamo il suo valore minimo.
            maxAzureDike += item instanceof AzureDikeAmuletItem amulet ? amulet.getDike(SHIELDGRASP_MIN_INK) : item.maxAzureDike(stack);
            rechargeSpeedModifier += item.azureDikeRechargeSpeedModifier(stack) - 1;
            rechargeDelayAfterDamageModifier += item.rechargeDelayAfterDamageModifier(stack) - 1;
            maxAzureDikeMultiplier += item.maxAzureDikeMultiplier(stack) - 1;
        }
        cached = new Base(Set.copyOf(trinkets.stream().map(ItemStack::getItem).toList()),
                maxAzureDike, maxAzureDikeMultiplier, rechargeSpeedModifier, rechargeDelayAfterDamageModifier);
        base = cached;
        return cached;
    }

    /** Dike del set senza potenziamento (amuleto al minimo): 40. */
    public static DikeStats dikeStats() {
        return dikeStats(0);
    }

    /**
     * Dike del set con il potenziamento a Ink dell'amuleto ({@code bonus} = Dike dell'amuleto sopra il minimo, 0-10):
     * (4 + 6 + 8 + 2 + bonus) × 2 del Core, quindi 40..60. Ricarica invariata.
     */
    public static DikeStats dikeStats(int bonus) {
        return dikeStats(bonus, List.of());
    }

    /**
     * Dike del set più gli accessori Dike indossati negli slot Curios che NON sono fusi nell'armatura (quelli fusi si
     * saltano). Si combinano come in AzureDikeAttachmentType.recalculate: Dike sommati, moltiplicatori e ricarica
     * sommati come (valore - 1). Es. guanti 10 + amuleto pieno: (4 + 6 + 8 + 2 + 10 + 10) × 2 = 80.
     */
    public static DikeStats dikeStats(int bonus, List<ItemStack> curios) {
        Base b = base();
        int dike = b.dikeSum() + Math.max(0, bonus);
        float multiplier = b.multiplier();
        float speed = b.rechargeSpeedModifier();
        float delay = b.rechargeDelayAfterDamageModifier();
        for (ItemStack stack : curios) {
            if (b.fused().contains(stack.getItem()) || !(stack.getItem() instanceof AzureDikeItem item)) continue;
            dike += item.maxAzureDike(stack);
            multiplier += item.maxAzureDikeMultiplier(stack) - 1;
            speed += item.azureDikeRechargeSpeedModifier(stack) - 1;
            delay += item.rechargeDelayAfterDamageModifier(stack) - 1;
        }
        return new DikeStats(Math.round(dike * multiplier),
                (int) Math.max(1, AzureDikeAttachmentType.BASE_RECHARGE_DELAY_TICKS / speed),
                (int) Math.max(1, AzureDikeAttachmentType.BASE_RECHARGE_DELAY_TICKS_AFTER_DAMAGE / delay));
    }

    /** Dike del set per chi lo indossa: potenziamento dagli stivali nello slot FEET e accessori Curios non fusi (nessuna cache). */
    public static DikeStats dikeStats(LivingEntity entity) {
        return dikeStats(wornBonus(entity), SACurios.dikeItems(entity));
    }

    /** Potenziamento degli stivali indossati (0 se non sono nostri o non ne hanno). */
    public static int wornBonus(LivingEntity entity) {
        ItemStack boots = entity.getItemBySlot(EquipmentSlot.FEET);
        return boots.getItem() instanceof SAArmorItem ? bonus(boots) : 0;
    }

    /** Potenziamento salvato su un pezzo (componente spectral_ascension:dike_bonus; assente = 0). */
    public static int bonus(ItemStack stack) {
        return Math.max(0, stack.getOrDefault(SAComponents.DIKE_BONUS.get(), 0));
    }

    /**
     * Potenziamento di uno Shieldgrasp Amulet: il suo Dike con l'Ink che contiene (AzureDikeAmuletItem.maxAzureDike:
     * 2, 4, ... 12) meno il minimo 2 già contato nel set. Amuleto vuoto o svuotato (sotto 100 Ink = 0 Dike) → 0.
     */
    public static int amuletBonus(ItemStack amulet) {
        if (!(amulet.getItem() instanceof AzureDikeAmuletItem item)) return 0;
        int min = item.getDike(SHIELDGRASP_MIN_INK);
        return Math.max(0, item.maxAzureDike(amulet) - min);
    }

    /**
     * Dike del set da mostrare nel tooltip di un pezzo: per gli stivali il loro potenziamento; per gli altri pezzi quello
     * degli stivali indossati da chi guarda (client), altrimenti la base. Più gli accessori Curios non fusi di chi guarda.
     */
    public static int tooltipSetDike(ItemStack piece) {
        Player viewer = tooltipViewer.get();
        List<ItemStack> curios = viewer == null ? List.of() : SACurios.dikeItems(viewer);
        if (piece.getItem() instanceof SAArmorItem armor && armor.getEquipmentSlot() == EquipmentSlot.FEET) {
            return dikeStats(bonus(piece), curios).max();
        }
        return dikeStats(viewer == null ? 0 : wornBonus(viewer), curios).max();
    }

    /** Dike in più che il potenziamento degli stivali dà al set (bonus × moltiplicatore del Core). */
    public static int setIncrease(int bonus) {
        return dikeStats(bonus).max() - dikeStats(0).max();
    }

    /**
     * Imposta il Dike del set (solo lato server) e lo manda ai client. La carica attuale resta, fino al nuovo massimo.
     * Calcolato ogni volta dagli stivali indossati: cambiare stivali (anche con un potenziamento diverso) fa scattare
     * {@link #onEquipmentChange}, che ricalcola e sincronizza.
     */
    public static void applySetDike(LivingEntity entity) {
        DikeStats s = dikeStats(entity);
        AzureDikeAttachmentType attachment = entity.getData(AzureDikeAttachmentType.ATTACHMENT_TYPE);
        if (attachment.getMaxCharges() == s.max()
                && attachment.getTicksToReplenishCharge() == s.ticksPerCharge()
                && attachment.getTicksToReplenishChargeAfterGettingHit() == s.rechargeDelayAfterDamage()) {
            return;
        }
        attachment.set(s.max(), s.ticksPerCharge(), s.rechargeDelayAfterDamage(), false);
        attachment.sync(entity);
    }

    /**
     * Azzera il Dike di un'entità senza inventario Curios: lì Spectrum non ricalcola nulla e resterebbe il Dike
     * del set appena tolto. Non crea il dato se l'entità non l'ha mai avuto.
     */
    public static void clearDike(LivingEntity entity) {
        if (entity.hasData(AzureDikeAttachmentType.ATTACHMENT_TYPE)) {
            AzureDikeAttachmentType attachment = entity.getData(AzureDikeAttachmentType.ATTACHMENT_TYPE);
            if (attachment.getMaxCharges() > 0) {
                attachment.set(0, 0, 0, false);
                attachment.sync(entity);
            }
        }
    }

    /**
     * Game bus: quando un pezzo Spectral Ascension entra o esce da uno slot dell'armatura, ricalcola il Dike.
     * Copre anche login e respawn: una nuova entità parte con gli slot "precedenti" vuoti, quindi al primo tick
     * il gioco segnala ogni pezzo indossato come cambiato.
     */
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getSlot().getType() != EquipmentSlot.Type.HUMANOID_ARMOR) {
            return;
        }
        if (event.getFrom().getItem() instanceof SAArmorItem || event.getTo().getItem() instanceof SAArmorItem) {
            AzureDikeAttachmentType.recalculate(event.getEntity());
        }
    }
}
