package cromocon.spectralascension.mixin;

import cromocon.spectralascension.item.SAEnchantCap;
import de.dafuqs.spectrum.helpers.SpectrumEnchantmentHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * La copia degli incantesimi dalle bowl dell'Enchanter all'oggetto centrale (e il suo costo in XP) passa da
 * {@code EnchanterBlockEntity.getHighestEnchantmentsInItemBowls} → {@code collectHighestEnchantments} (unico chiamante,
 * verificato nel jar di Spectrum 1.12.9). Lì, per OGNI stack delle bowl (libri compresi, oggetti di qualunque mod), i
 * livelli dei 42 incantesimi della tabella di {@link SAEnchantCap} sono tagliati al massimo ATM10: un libro Sharpness
 * XII dà Sharpness IX, un oggetto T9 con Sharpness XII pure. Gli incantesimi fuori tabella (Spectrum, altre mod) non
 * sono toccati. Il costo in XP segue i livelli tagliati (stessa strada).
 * <p>
 * shortcut: qui il Wind Burst intrinseco delle nostre mazze NON è tolto (niente {@code MaceWindBurst.extractable}), di
 * proposito: i Gilded Book non si craftano, si trovano soltanto, e il tetto ATM10 tiene il libro a Wind Burst VIII,
 * mai XI (decisione utente 2026-11-27).
 */
@Mixin(value = SpectrumEnchantmentHelper.class, remap = false)
public abstract class SpectrumEnchantmentHelperMixin {

    @Redirect(method = "collectHighestEnchantments",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getEnchantmentsForCrafting(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/enchantment/ItemEnchantments;"))
    private static ItemEnchantments spectral_ascension$capToAtm10(ItemStack stack) {
        return SAEnchantCap.clamp(EnchantmentHelper.getEnchantmentsForCrafting(stack));
    }
}
