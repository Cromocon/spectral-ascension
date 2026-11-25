package cromocon.spectralascension.item;

import de.dafuqs.spectrum.api.item.AzureDikeItem;
import de.dafuqs.spectrum.attachment_types.AzureDikeAttachmentType;
import de.dafuqs.spectrum.items.trinkets.AzureDikeAmuletItem;
import de.dafuqs.spectrum.registries.SpectrumItems;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;

import java.util.List;

/**
 * Il bonus del set completo: con i 4 pezzi Spectral Ascension indossati il giocatore ha l'Azure Dike di tutti
 * gli accessori Dike di Spectrum non potenziati, e gli accessori Dike negli slot Curios si disattivano.
 * <p>
 * Spectrum calcola il Dike solo dagli slot Curios ({@link AzureDikeAttachmentType#recalculate}). Il nostro mixin
 * (mixin/AzureDikeAttachmentTypeMixin) intercetta quel calcolo: se il set è indossato usa {@link #applySetDike}
 * e salta i Curios. Il ricalcolo parte quando cambiano gli accessori (Spectrum) o l'armatura ({@link #onEquipmentChange}).
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

    private static volatile DikeStats stats;

    private SAArmorSet() {
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
    public static DikeStats dikeStats() {
        DikeStats cached = stats;
        if (cached != null) {
            return cached;
        }
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
        cached = new DikeStats(
                Math.round(maxAzureDike * maxAzureDikeMultiplier),
                (int) Math.max(1, AzureDikeAttachmentType.BASE_RECHARGE_DELAY_TICKS / rechargeSpeedModifier),
                (int) Math.max(1, AzureDikeAttachmentType.BASE_RECHARGE_DELAY_TICKS_AFTER_DAMAGE / rechargeDelayAfterDamageModifier));
        stats = cached;
        return cached;
    }

    /** Imposta il Dike del set (solo lato server) e lo manda ai client. La carica attuale resta, fino al nuovo massimo. */
    public static void applySetDike(LivingEntity entity) {
        DikeStats s = dikeStats();
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
