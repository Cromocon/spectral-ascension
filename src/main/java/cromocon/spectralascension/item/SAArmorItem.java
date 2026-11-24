package cromocon.spectralascension.item;

import cromocon.spectralascension.SpectralAscension;
import de.dafuqs.spectrum.items.armor.BedrockArmorItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.List;
import java.util.Map;

/**
 * Un pezzo dell'armatura. Estende l'armatura bedrock di Spectrum, così eredita il modello 3D, il mantello
 * (BedrockCapeRenderer controlla {@code instanceof BedrockArmorItem}), i Piglin neutrali e il fatto di non
 * essere riparabile. Cambia la texture del modello indossato e gli incantesimi preimpostati.
 * <p>
 * Il Dike del set completo e la disattivazione degli accessori sono in {@link SAArmorSet}.
 */
public final class SAArmorItem extends BedrockArmorItem {
    private final Map<ResourceKey<Enchantment>, Integer> enchantments;
    /**
     * Numero di fotogrammi dell'armatura indossata animata per tier (textures/armor/<prefisso>_armor_0..N-1.png):
     * T6 Castone 20, T7 Umbral 20, T8 Argent 20 (ciclo di 5 s), Alloy Prismatic 40 (ciclo di 10 s). Deve coincidere con TIERS di
     * tools/install_textures.py, che li installa; tools/validate.py legge questa mappa e controlla i file.
     */
    private static final Map<SATier, Integer> ARMOR_FRAMES = Map.of(
            SATier.T6, 20,
            SATier.T7, 20,
            SATier.T8, 20,
            SATier.ALLOY, 40);
    /** Fotogrammi per un tier senza set animato (non dovrebbe servire: ogni tier con armatura è nella mappa). */
    private static final int DEFAULT_ARMOR_FRAMES = 1;
    /** Tick di gioco per ogni fotogramma, uguale per tutti i tier (5 tick = 0,25 s, come le icone). */
    private static final int ARMOR_FRAME_TICKS = 5;

    private final String texturePrefix;
    private final int armorFrames;
    private final SATier tier;

    public SAArmorItem(SATier tier, Holder<ArmorMaterial> material, Type type, Properties properties,
                       Map<ResourceKey<Enchantment>, Integer> enchantments) {
        super(material, type, properties);
        this.enchantments = Map.copyOf(enchantments);
        this.tier = tier;
        // Stesso layout 128×128 di spectrum:textures/armor/bedrock_armor.png.
        this.texturePrefix = "textures/armor/" + tier.idPrefix() + "_armor";
        this.armorFrames = ARMOR_FRAMES.getOrDefault(tier, DEFAULT_ARMOR_FRAMES);
    }

    /** Il tier del pezzo. */
    public SATier tier() {
        return tier;
    }

    /** Fotogrammi dell'armatura indossata di questo pezzo (usato anche dai GameTest). */
    public int armorFrames() {
        return armorFrames;
    }

    @Override
    public Map<ResourceKey<Enchantment>, Integer> getDefaultEnchantments() {
        return enchantments;
    }

    /** Chiamata a ogni frame di render. Usata sia dal modello dell'armatura sia dal mantello di Spectrum. */
    @Override
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        // Senza entità (anteprime, GUI) si usa il fotogramma 0; altrimenti si anima col tempo di gioco.
        int frame = entity == null ? 0 : (int) ((entity.level().getGameTime() / ARMOR_FRAME_TICKS) % armorFrames);
        return SpectralAscension.id(texturePrefix + "_" + frame + ".png");
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("item." + SpectralAscension.MOD_ID + ".armor.tooltip.set_dike",
                Component.literal(String.valueOf(SAArmorSet.dikeStats().max())).withStyle(ChatFormatting.GREEN)).withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.translatable("item." + SpectralAscension.MOD_ID + ".armor.tooltip.trinkets_disabled").withStyle(ChatFormatting.GRAY));
    }
}
