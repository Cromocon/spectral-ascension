package cromocon.spectralascension.registry;

import cromocon.spectralascension.SpectralAscension;
import cromocon.spectralascension.item.SATier;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;

/**
 * I materiali dell'armatura per tier: punti armatura per pezzo, toughness, resistenza al contraccolpo,
 * incantabilità e suono quando la indossi. Registrati nel NOSTRO namespace (non con
 * SpectrumArmorMaterials.register, che userebbe il namespace spectrum).
 * <p>
 * Valori decisi in ricerca/README.md (statistiche armatura): T6 6/14/10/6 = 36, toughness 5; T7 7/16/11/7 = 41, toughness 6;
 * T8 8/18/12/8 = 46, toughness 7; Alloy 9/19/13/9 = 50, toughness 7,5. Knockback resistance 0,3 a pezzo in TUTTI i tier
 * (decisione dell'utente 2026-11-19, come l'armatura bedrock di Spectrum).
 * L'armatura è indistruttibile (componente unbreakable), quindi l'ingrediente di riparazione è solo nominale.
 */
public final class SAArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, SpectralAscension.MOD_ID);

    /** Incantabilità PROVVISORIA = quella degli attrezzi del tier (SATier.T6 = 90). Da confermare. */
    public static final Holder<ArmorMaterial> TRICHROMATIC = register(SATier.T6, 6, 14, 10, 6, 5.0F, 0.3F);
    /** T7: 7/16/11/7 = 41, toughness 6 e knockback 0,3 a pezzo (ricerca/README.md). Incantabilità = SATier.T7 (105). */
    public static final Holder<ArmorMaterial> UMBRAL = register(SATier.T7, 7, 16, 11, 7, 6.0F, 0.3F);
    /** T8: 8/18/12/8 = 46, toughness 7 e knockback 0,3 a pezzo (ricerca/README.md). Incantabilità = SATier.T8 (130). */
    public static final Holder<ArmorMaterial> ARGENT = register(SATier.T8, 8, 18, 12, 8, 7.0F, 0.3F);
    /**
     * Alloy (Prismatic): 9/19/13/9 = 50 (il massimo dell'addon), toughness 7,5 e knockback 0,3 a pezzo
     * (ricerca/README.md, «Statistiche armatura»). Incantabilità = SATier.ALLOY (200).
     */
    public static final Holder<ArmorMaterial> PRISMATIC = register(SATier.ALLOY, 9, 19, 13, 9, 7.5F, 0.3F);

    private SAArmorMaterials() {
    }

    private static Holder<ArmorMaterial> register(SATier tier, int helmet, int chestplate, int leggings, int boots,
                                                  float toughness, float knockbackResistance) {
        EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        defense.put(ArmorItem.Type.HELMET, helmet);
        defense.put(ArmorItem.Type.CHESTPLATE, chestplate);
        defense.put(ArmorItem.Type.LEGGINGS, leggings);
        defense.put(ArmorItem.Type.BOOTS, boots);
        // Il "layer" serve al rendering vanilla, che qui non si usa: SAArmorItem.getArmorTexture lo sostituisce.
        List<ArmorMaterial.Layer> layers = List.of(new ArmorMaterial.Layer(SpectralAscension.id(tier.idPrefix())));
        return MATERIALS.register(tier.idPrefix(), () -> new ArmorMaterial(defense, tier.getEnchantmentValue(),
                SoundEvents.ARMOR_EQUIP_NETHERITE, tier::getRepairIngredient, layers, toughness, knockbackResistance));
    }

    public static void register(IEventBus modBus) {
        MATERIALS.register(modBus);
    }
}
