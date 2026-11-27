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
     * Numero di fotogrammi dell'armatura indossata per tier (textures/armor/<prefisso>_armor_0..N-1.png): solo l'Alloy
     * Prismatic e' animato (40 fotogrammi, ciclo di 10 s); T6, T7 e T8 hanno un solo fotogramma (_0) e non ne calcolano.
     * Deve coincidere con TIERS di tools/install_textures.py, che li installa; tools/validate.py legge questa mappa e
     * controlla i file.
     */
    private static final Map<SATier, Integer> ARMOR_FRAMES = Map.of(
            SATier.T6, 1,
            SATier.T7, 1,
            SATier.T8, 1,
            SATier.ALLOY, 40);
    /** Tick di gioco per ogni fotogramma dell'armatura animata (15 tick = 0,75 s). */
    private static final int ARMOR_FRAME_TICKS = 15;

    private final ResourceLocation[] textures;
    private final int armorFrames;
    private final SATier tier;

    public SAArmorItem(SATier tier, Holder<ArmorMaterial> material, Type type, Properties properties,
                       Map<ResourceKey<Enchantment>, Integer> enchantments) {
        super(material, type, properties);
        this.enchantments = Map.copyOf(enchantments);
        this.tier = tier;
        // Stesso layout 128×128 di spectrum:textures/armor/bedrock_armor.png.
        // Tutte le ResourceLocation dei fotogrammi, calcolate una volta sola: getArmorTexture non ne crea a ogni frame.
        int n = ARMOR_FRAMES.get(tier); // copre tutti i SATier
        this.textures = new ResourceLocation[n];
        for (int i = 0; i < n; i++) this.textures[i] = SpectralAscension.id("textures/armor/" + tier.idPrefix() + "_armor_" + i + ".png");
        this.armorFrames = textures.length;
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
        // Un solo fotogramma (T6-T8): nessun calcolo. Senza entita' (anteprime, GUI) si usa il fotogramma 0;
        // altrimenti si anima col tempo di gioco.
        if (armorFrames == 1 || entity == null) return textures[0];
        return textures[(int) ((entity.level().getGameTime() / ARMOR_FRAME_TICKS) % armorFrames)];
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        // Dike reale del set: per gli stivali il loro potenziamento, per gli altri pezzi quello degli stivali indossati.
        tooltip.add(Component.translatable("item." + SpectralAscension.MOD_ID + ".armor.tooltip.set_dike",
                Component.literal(String.valueOf(SAArmorSet.tooltipSetDike(stack))).withStyle(ChatFormatting.GREEN)).withStyle(ChatFormatting.BLUE));
        int bonus = SAArmorSet.bonus(stack);
        if (bonus > 0) {
            tooltip.add(Component.translatable("item." + SpectralAscension.MOD_ID + ".armor.tooltip.dike_bonus",
                    Component.literal("+" + SAArmorSet.setIncrease(bonus)).withStyle(ChatFormatting.GREEN)).withStyle(ChatFormatting.BLUE));
        }
        tooltip.add(Component.translatable("item." + SpectralAscension.MOD_ID + ".armor.tooltip.trinkets_disabled").withStyle(ChatFormatting.GRAY));
    }
}
