package cromocon.spectralascension.gametest;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import cromocon.spectralascension.SpectralAscension;
import cromocon.spectralascension.item.BidentEnchantments;
import cromocon.spectralascension.item.BidentMode;
import cromocon.spectralascension.item.SAArmorItem;
import cromocon.spectralascension.item.SABident;
import cromocon.spectralascension.menu.BidentMenu;
import cromocon.spectralascension.item.SAArmorSet;
import cromocon.spectralascension.item.SATier;
import cromocon.spectralascension.item.SATools;
import cromocon.spectralascension.registry.SAComponents;
import cromocon.spectralascension.registry.SAItems;
import de.dafuqs.revelationary.advancement_criteria.AdvancementGottenCriterion;
import de.dafuqs.spectrum.api.item.ActivatableItem;
import de.dafuqs.spectrum.api.ink.color.InkColors;
import de.dafuqs.spectrum.api.item.Preenchanted;
import de.dafuqs.spectrum.api.recipe.GatedRecipe;
import de.dafuqs.spectrum.attachment_types.AzureDikeAttachmentType;
import de.dafuqs.spectrum.blocks.fusion_shrine.FusionShrineBlockEntity;
import de.dafuqs.spectrum.items.tools.WorkstaffItem;
import de.dafuqs.spectrum.recipe.GatedSpectrumRecipe;
import de.dafuqs.spectrum.recipe.fusion_shrine.FusionShrineRecipe;
import de.dafuqs.spectrum.recipe.pedestal.PedestalRecipe;
import de.dafuqs.spectrum.recipe.pedestal.PedestalRecipeTier;
import de.dafuqs.spectrum.registries.SpectrumAdvancements;
import de.dafuqs.spectrum.registries.SpectrumBlocks;
import de.dafuqs.spectrum.registries.SpectrumDataComponentTypes;
import de.dafuqs.spectrum.registries.SpectrumEnchantmentKeys;
import de.dafuqs.spectrum.registries.SpectrumItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.DeferredItem;
import top.theillusivec4.curios.api.CuriosApi;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 * Test automatici eseguiti da {@code ./gradlew runGameTestServer}: avviano un server di test con Spectrum,
 * Allthemodium e la nostra mod, poi controllano oggetti, ricette e advancement.
 * Sono attivi solo quando la proprietà {@code neoforge.enabledGameTestNamespaces} include la nostra mod
 * (la imposta build.gradle), quindi in gioco normale non esistono.
 */
@GameTestHolder(SpectralAscension.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SAGameTests {
    /** Struttura vuota 1×1×1 in data/spectral_ascension/structure/empty.nbt. */
    private static final String EMPTY = "empty";

    private SAGameTests() {
    }

    /** Tier → nome del tier nei percorsi delle ricette e degli advancement. */
    private static String tierOf(String itemId) {
        if (itemId.startsWith("trichromatic_")) return "t6";
        if (itemId.startsWith("umbral_")) return "t7";
        if (itemId.startsWith("argent_")) return "t8";
        return "t9"; // prefisso "prismatic_" (Alloy: "t9" nei percorsi, come in tools/gen_data.py)
    }

    /** Tag di tipo attesi per ogni attrezzo (decidono quali incantesimi accetta). */
    private static final Map<String, List<TagKey<Item>>> KIND_TAGS = Map.ofEntries(
            Map.entry("workstaff", List.of(ItemTags.PICKAXES, ItemTags.AXES, ItemTags.HOES, ItemTags.MINING_ENCHANTABLE,
                    ItemTags.MINING_LOOT_ENCHANTABLE, ItemTags.create(ResourceLocation.parse("spectrum:workstaffs")))),
            Map.entry("ultra_greatsword", List.of(ItemTags.SWORDS, ItemTags.SWORD_ENCHANTABLE, ItemTags.SHARP_WEAPON_ENCHANTABLE,
                    ItemTags.create(ResourceLocation.parse("spectrum:greatswords")))),
            Map.entry("crossbow", List.of(ItemTags.CROSSBOW_ENCHANTABLE, ItemTags.create(ResourceLocation.parse("spectrum:crossbows")))),
            Map.entry("bident", List.of(ItemTags.TRIDENT_ENCHANTABLE, ItemTags.create(ResourceLocation.parse("spectrum:bidents")))),
            Map.entry("axe", List.of(ItemTags.AXES, ItemTags.MINING_ENCHANTABLE, ItemTags.SHARP_WEAPON_ENCHANTABLE)),
            Map.entry("shovel", List.of(ItemTags.SHOVELS, ItemTags.MINING_ENCHANTABLE)),
            Map.entry("hoe", List.of(ItemTags.HOES, ItemTags.MINING_ENCHANTABLE)),
            Map.entry("sword", List.of(ItemTags.SWORDS, ItemTags.SHARP_WEAPON_ENCHANTABLE)),
            Map.entry("bow", List.of(ItemTags.BOW_ENCHANTABLE)),
            Map.entry("fishing_rod", List.of(ItemTags.FISHING_ENCHANTABLE)),
            Map.entry("shears", List.of(ItemTags.MINING_ENCHANTABLE)),
            Map.entry("mace", List.of(ItemTags.MACE_ENCHANTABLE, ItemTags.WEAPON_ENCHANTABLE, ItemTags.FIRE_ASPECT_ENCHANTABLE,
                    net.neoforged.neoforge.common.Tags.Items.TOOLS_MACE)),
            Map.entry("helmet", List.of(ItemTags.HEAD_ARMOR, ItemTags.HEAD_ARMOR_ENCHANTABLE, ItemTags.ARMOR_ENCHANTABLE)),
            Map.entry("chestplate", List.of(ItemTags.CHEST_ARMOR, ItemTags.CHEST_ARMOR_ENCHANTABLE, ItemTags.ARMOR_ENCHANTABLE)),
            Map.entry("leggings", List.of(ItemTags.LEG_ARMOR, ItemTags.LEG_ARMOR_ENCHANTABLE, ItemTags.ARMOR_ENCHANTABLE)),
            Map.entry("boots", List.of(ItemTags.FOOT_ARMOR, ItemTags.FOOT_ARMOR_ENCHANTABLE, ItemTags.ARMOR_ENCHANTABLE)));

    /** Statistiche dell'armatura di un tier (ricerca/README.md): punti per pezzo, toughness e knockback a pezzo (0,3 in tutti i tier). */
    private record ArmorStats(Map<ArmorItem.Type, Integer> armor, double toughness, double knockback) {
    }

    private static final Map<String, ArmorStats> ARMOR_STATS = Map.of(
            "t6", new ArmorStats(Map.of(ArmorItem.Type.HELMET, 6, ArmorItem.Type.CHESTPLATE, 14, ArmorItem.Type.LEGGINGS, 10,
                    ArmorItem.Type.BOOTS, 6), 5.0, 0.3),
            "t7", new ArmorStats(Map.of(ArmorItem.Type.HELMET, 7, ArmorItem.Type.CHESTPLATE, 16, ArmorItem.Type.LEGGINGS, 11,
                    ArmorItem.Type.BOOTS, 7), 6.0, 0.3),
            "t8", new ArmorStats(Map.of(ArmorItem.Type.HELMET, 8, ArmorItem.Type.CHESTPLATE, 18, ArmorItem.Type.LEGGINGS, 12,
                    ArmorItem.Type.BOOTS, 8), 7.0, 0.3),
            "t9", new ArmorStats(Map.of(ArmorItem.Type.HELMET, 9, ArmorItem.Type.CHESTPLATE, 19, ArmorItem.Type.LEGGINGS, 13,
                    ArmorItem.Type.BOOTS, 9), 7.5, 0.3));
    /**
     * Fotogrammi dell'armatura indossata animata per tier (T6 Castone, T7 Umbral, T8 Argent: 1, statici; Alloy Prismatic: 40;
     * 15 tick l'uno).
     */
    private static final Map<String, Integer> ARMOR_FRAMES = Map.of("t6", 1, "t7", 1, "t8", 1, "t9", 40);
    /**
     * Sblocchi che l'advancement di un pezzo d'armatura deve richiedere: gli accessori di Spectrum nella ricetta T6,
     * il pezzo del tier precedente per T7 e T8 (checklist §A/§B).
     */
    private static final Map<String, List<String>> ARMOR_TRINKET_UNLOCKS = Map.ofEntries(
            Map.entry("trichromatic_helmet", List.of("spectrum:unlocks/trinkets/puff_circlet")),
            Map.entry("trichromatic_chestplate", List.of("spectrum:unlocks/trinkets/azure_dike_ring")),
            Map.entry("trichromatic_leggings", List.of("spectrum:unlocks/trinkets/azure_dike_belt")),
            Map.entry("trichromatic_boots", List.of("spectrum:unlocks/trinkets/shieldgrasp_amulet", "spectrum:unlocks/trinkets/azuresque_dike_core")),
            Map.entry("umbral_helmet", List.of("spectral_ascension:unlocks/equipment/t6/trichromatic_helmet")),
            Map.entry("umbral_chestplate", List.of("spectral_ascension:unlocks/equipment/t6/trichromatic_chestplate")),
            Map.entry("umbral_leggings", List.of("spectral_ascension:unlocks/equipment/t6/trichromatic_leggings")),
            Map.entry("umbral_boots", List.of("spectral_ascension:unlocks/equipment/t6/trichromatic_boots")),
            Map.entry("argent_helmet", List.of("spectral_ascension:unlocks/equipment/t7/umbral_helmet")),
            Map.entry("argent_chestplate", List.of("spectral_ascension:unlocks/equipment/t7/umbral_chestplate")),
            Map.entry("argent_leggings", List.of("spectral_ascension:unlocks/equipment/t7/umbral_leggings")),
            Map.entry("argent_boots", List.of("spectral_ascension:unlocks/equipment/t7/umbral_boots")),
            Map.entry("prismatic_helmet", List.of("spectral_ascension:unlocks/equipment/t8/argent_helmet")),
            Map.entry("prismatic_chestplate", List.of("spectral_ascension:unlocks/equipment/t8/argent_chestplate")),
            Map.entry("prismatic_leggings", List.of("spectral_ascension:unlocks/equipment/t8/argent_leggings")),
            Map.entry("prismatic_boots", List.of("spectral_ascension:unlocks/equipment/t8/argent_boots")));
    /** Tier → tier precedente, che lo sblocco del tier deve richiedere (checklist C). */
    private static final Map<String, String> PREVIOUS_TIER = Map.of("t7", "t6", "t8", "t7", "t9", "t8");
    /**
     * Tier il cui lingotto è un OR di più tag (una sola riga di requirements): il T9 (Alloy) si sblocca con uno
     * qualsiasi dei tre lingotti di lega (ricerca/04 §5.1).
     */
    private static final Map<String, List<String>> TIER_INGOTS_OR = Map.of("t9", List.of(
            "c:ingots/vibranium_allthemodium_alloy", "c:ingots/unobtainium_allthemodium_alloy", "c:ingots/unobtainium_vibranium_alloy"));
    /**
     * Lucchetti di Spectrum in più nello sblocco del tier, oltre al tier precedente e al lingotto (ricerca/04 §5.1:
     * T8 = T7 + glass crest + unobtainium).
     */
    private static final Map<String, List<String>> TIER_EXTRA_UNLOCKS = Map.of(
            "t8", List.of("spectrum:unlocks/malachite/glass_crest_tools"));
    /** Tier → prefisso degli id del tier precedente: il pezzo T7 si fa dal pezzo T6 (e il T8 dal T7) con lo stesso tipo. */
    private static final Map<String, String> PREVIOUS_PREFIX = Map.of("t7", "trichromatic_", "t8", "umbral_", "t9", "argent_");
    /** Sblocco di Spectrum di tutte le ricette bedrock (jar:data/spectrum/recipe/fusion_shrine/bedrock/*.json). */
    private static final String BEDROCK_TOOLS = "spectrum:unlocks/equipment/bedrock_tools";
    /** Liquido delle ricette Fusion Shrine per tier (T6 Dragonrot dal 2026-11-18; T7, T8 e Alloy Dragonrot dal 2026-11-19). */
    private static final Map<String, String> FUSION_FLUID = Map.of("t6", "spectrum:dragonrot",
            "t7", "spectrum:dragonrot", "t8", "spectrum:dragonrot", "t9", "spectrum:dragonrot");
    /** Dike del set: 40 punti, ricarica 40/2,5 = 16 tick a punto, attesa dopo un colpo 200/3 = 66 tick. */
    private static final int SET_DIKE = 40;
    /**
     * Oggetti fatti alla Fusion Shrine → oggetto di Spectrum che è il primo ingrediente, poi gli sblocchi di Spectrum
     * che l'advancement dell'oggetto deve richiedere (per il bident anche quello del Fractal, secondo ingrediente).
     */
    private static final Map<String, List<String>> FUSION_BASE = Map.ofEntries(
            // T6 dalla versione bedrock (dal 2026-11-18 alla Fusion Shrine con Dragonrot, prima al Pedestal): tutte le
            // ricette bedrock di Spectrum richiedono unlocks/equipment/bedrock_tools; l'armatura anche i suoi accessori.
            Map.entry("trichromatic_axe", List.of("spectrum:bedrock_axe", BEDROCK_TOOLS)),
            Map.entry("trichromatic_shovel", List.of("spectrum:bedrock_shovel", BEDROCK_TOOLS)),
            Map.entry("trichromatic_hoe", List.of("spectrum:bedrock_hoe", BEDROCK_TOOLS)),
            Map.entry("trichromatic_sword", List.of("spectrum:bedrock_sword", BEDROCK_TOOLS)),
            Map.entry("trichromatic_bow", List.of("spectrum:bedrock_bow", BEDROCK_TOOLS)),
            Map.entry("trichromatic_fishing_rod", List.of("spectrum:bedrock_fishing_rod", BEDROCK_TOOLS)),
            Map.entry("trichromatic_shears", List.of("spectrum:bedrock_shears", BEDROCK_TOOLS)),
            Map.entry("trichromatic_helmet", List.of("spectrum:bedrock_helmet", BEDROCK_TOOLS, "spectrum:unlocks/trinkets/puff_circlet")),
            Map.entry("trichromatic_chestplate", List.of("spectrum:bedrock_chestplate", BEDROCK_TOOLS, "spectrum:unlocks/trinkets/azure_dike_ring")),
            Map.entry("trichromatic_leggings", List.of("spectrum:bedrock_leggings", BEDROCK_TOOLS, "spectrum:unlocks/trinkets/azure_dike_belt")),
            Map.entry("trichromatic_boots", List.of("spectrum:bedrock_boots", BEDROCK_TOOLS,
                    "spectrum:unlocks/trinkets/shieldgrasp_amulet", "spectrum:unlocks/trinkets/azuresque_dike_core")),
            // T6 dalla versione glass crest (upgrade con copy_components).
            Map.entry("trichromatic_workstaff", List.of("spectrum:glass_crest_workstaff", "spectrum:unlocks/malachite/glass_crest_workstaff")),
            Map.entry("trichromatic_ultra_greatsword", List.of("spectrum:glass_crest_ultra_greatsword",
                    "spectrum:unlocks/malachite/glass_crest_ultra_greatsword")),
            Map.entry("trichromatic_crossbow", List.of("spectrum:glass_crest_crossbow", "spectrum:unlocks/malachite/glass_crest_crossbow")),
            Map.entry("trichromatic_bident", List.of("spectrum:ferocious_glass_crest_bident",
                    "spectrum:unlocks/malachite/ferocious_glass_crest_bident", "spectrum:unlocks/malachite/fractal_glass_crest_bident")),
            // Mazza (2026-11-25): T6 dalla NOSTRA mazza di bedrock, poi la catena dei tier come gli altri oggetti.
            Map.entry("trichromatic_mace", List.of("spectral_ascension:bedrock_mace", BEDROCK_TOOLS)),
            Map.entry("umbral_mace", List.of("spectral_ascension:trichromatic_mace", "spectral_ascension:unlocks/equipment/t6/trichromatic_mace")),
            Map.entry("argent_mace", List.of("spectral_ascension:umbral_mace", "spectral_ascension:unlocks/equipment/t7/umbral_mace")),
            Map.entry("prismatic_mace", List.of("spectral_ascension:argent_mace", "spectral_ascension:unlocks/equipment/t8/argent_mace")),
            // T7: upgrade del pezzo T6, il cui sblocco va richiesto.
            Map.entry("umbral_workstaff", List.of("spectral_ascension:trichromatic_workstaff", "spectral_ascension:unlocks/equipment/t6/trichromatic_workstaff")),
            Map.entry("umbral_ultra_greatsword", List.of("spectral_ascension:trichromatic_ultra_greatsword",
                    "spectral_ascension:unlocks/equipment/t6/trichromatic_ultra_greatsword")),
            Map.entry("umbral_crossbow", List.of("spectral_ascension:trichromatic_crossbow", "spectral_ascension:unlocks/equipment/t6/trichromatic_crossbow")),
            Map.entry("umbral_bident", List.of("spectral_ascension:trichromatic_bident", "spectral_ascension:unlocks/equipment/t6/trichromatic_bident")),
            // T7 (dal 2026-11-19 tutti alla Fusion Shrine): anche gli 11 pezzi che prima erano al Pedestal.
            Map.entry("umbral_axe", List.of("spectral_ascension:trichromatic_axe", "spectral_ascension:unlocks/equipment/t6/trichromatic_axe")),
            Map.entry("umbral_shovel", List.of("spectral_ascension:trichromatic_shovel", "spectral_ascension:unlocks/equipment/t6/trichromatic_shovel")),
            Map.entry("umbral_hoe", List.of("spectral_ascension:trichromatic_hoe", "spectral_ascension:unlocks/equipment/t6/trichromatic_hoe")),
            Map.entry("umbral_sword", List.of("spectral_ascension:trichromatic_sword", "spectral_ascension:unlocks/equipment/t6/trichromatic_sword")),
            Map.entry("umbral_bow", List.of("spectral_ascension:trichromatic_bow", "spectral_ascension:unlocks/equipment/t6/trichromatic_bow")),
            Map.entry("umbral_fishing_rod", List.of("spectral_ascension:trichromatic_fishing_rod", "spectral_ascension:unlocks/equipment/t6/trichromatic_fishing_rod")),
            Map.entry("umbral_shears", List.of("spectral_ascension:trichromatic_shears", "spectral_ascension:unlocks/equipment/t6/trichromatic_shears")),
            Map.entry("umbral_helmet", List.of("spectral_ascension:trichromatic_helmet", "spectral_ascension:unlocks/equipment/t6/trichromatic_helmet")),
            Map.entry("umbral_chestplate", List.of("spectral_ascension:trichromatic_chestplate", "spectral_ascension:unlocks/equipment/t6/trichromatic_chestplate")),
            Map.entry("umbral_leggings", List.of("spectral_ascension:trichromatic_leggings", "spectral_ascension:unlocks/equipment/t6/trichromatic_leggings")),
            Map.entry("umbral_boots", List.of("spectral_ascension:trichromatic_boots", "spectral_ascension:unlocks/equipment/t6/trichromatic_boots")),
            // T8 (dal 2026-11-19 tutti alla Fusion Shrine): upgrade del pezzo T7, il cui sblocco va richiesto.
            Map.entry("argent_workstaff", List.of("spectral_ascension:umbral_workstaff", "spectral_ascension:unlocks/equipment/t7/umbral_workstaff")),
            Map.entry("argent_ultra_greatsword", List.of("spectral_ascension:umbral_ultra_greatsword",
                    "spectral_ascension:unlocks/equipment/t7/umbral_ultra_greatsword")),
            Map.entry("argent_crossbow", List.of("spectral_ascension:umbral_crossbow", "spectral_ascension:unlocks/equipment/t7/umbral_crossbow")),
            Map.entry("argent_bident", List.of("spectral_ascension:umbral_bident", "spectral_ascension:unlocks/equipment/t7/umbral_bident")),
            Map.entry("argent_axe", List.of("spectral_ascension:umbral_axe", "spectral_ascension:unlocks/equipment/t7/umbral_axe")),
            Map.entry("argent_shovel", List.of("spectral_ascension:umbral_shovel", "spectral_ascension:unlocks/equipment/t7/umbral_shovel")),
            Map.entry("argent_hoe", List.of("spectral_ascension:umbral_hoe", "spectral_ascension:unlocks/equipment/t7/umbral_hoe")),
            Map.entry("argent_sword", List.of("spectral_ascension:umbral_sword", "spectral_ascension:unlocks/equipment/t7/umbral_sword")),
            Map.entry("argent_bow", List.of("spectral_ascension:umbral_bow", "spectral_ascension:unlocks/equipment/t7/umbral_bow")),
            Map.entry("argent_fishing_rod", List.of("spectral_ascension:umbral_fishing_rod", "spectral_ascension:unlocks/equipment/t7/umbral_fishing_rod")),
            Map.entry("argent_shears", List.of("spectral_ascension:umbral_shears", "spectral_ascension:unlocks/equipment/t7/umbral_shears")),
            Map.entry("argent_helmet", List.of("spectral_ascension:umbral_helmet", "spectral_ascension:unlocks/equipment/t7/umbral_helmet")),
            Map.entry("argent_chestplate", List.of("spectral_ascension:umbral_chestplate", "spectral_ascension:unlocks/equipment/t7/umbral_chestplate")),
            Map.entry("argent_leggings", List.of("spectral_ascension:umbral_leggings", "spectral_ascension:unlocks/equipment/t7/umbral_leggings")),
            Map.entry("argent_boots", List.of("spectral_ascension:umbral_boots", "spectral_ascension:unlocks/equipment/t7/umbral_boots")),
            // T9 (Alloy, dal 2026-11-19 tutti alla Fusion Shrine): upgrade del pezzo T8, il cui sblocco va richiesto.
            Map.entry("prismatic_workstaff", List.of("spectral_ascension:argent_workstaff", "spectral_ascension:unlocks/equipment/t8/argent_workstaff")),
            Map.entry("prismatic_ultra_greatsword", List.of("spectral_ascension:argent_ultra_greatsword",
                    "spectral_ascension:unlocks/equipment/t8/argent_ultra_greatsword")),
            Map.entry("prismatic_crossbow", List.of("spectral_ascension:argent_crossbow", "spectral_ascension:unlocks/equipment/t8/argent_crossbow")),
            Map.entry("prismatic_bident", List.of("spectral_ascension:argent_bident", "spectral_ascension:unlocks/equipment/t8/argent_bident")),
            Map.entry("prismatic_axe", List.of("spectral_ascension:argent_axe", "spectral_ascension:unlocks/equipment/t8/argent_axe")),
            Map.entry("prismatic_shovel", List.of("spectral_ascension:argent_shovel", "spectral_ascension:unlocks/equipment/t8/argent_shovel")),
            Map.entry("prismatic_hoe", List.of("spectral_ascension:argent_hoe", "spectral_ascension:unlocks/equipment/t8/argent_hoe")),
            Map.entry("prismatic_sword", List.of("spectral_ascension:argent_sword", "spectral_ascension:unlocks/equipment/t8/argent_sword")),
            Map.entry("prismatic_bow", List.of("spectral_ascension:argent_bow", "spectral_ascension:unlocks/equipment/t8/argent_bow")),
            Map.entry("prismatic_fishing_rod", List.of("spectral_ascension:argent_fishing_rod", "spectral_ascension:unlocks/equipment/t8/argent_fishing_rod")),
            Map.entry("prismatic_shears", List.of("spectral_ascension:argent_shears", "spectral_ascension:unlocks/equipment/t8/argent_shears")),
            Map.entry("prismatic_helmet", List.of("spectral_ascension:argent_helmet", "spectral_ascension:unlocks/equipment/t8/argent_helmet")),
            Map.entry("prismatic_chestplate", List.of("spectral_ascension:argent_chestplate", "spectral_ascension:unlocks/equipment/t8/argent_chestplate")),
            Map.entry("prismatic_leggings", List.of("spectral_ascension:argent_leggings", "spectral_ascension:unlocks/equipment/t8/argent_leggings")),
            Map.entry("prismatic_boots", List.of("spectral_ascension:argent_boots", "spectral_ascension:unlocks/equipment/t8/argent_boots")));

    // ---------- Controlli ----------

    /** Ritorna gli errori trovati su un oggetto del set base (lista vuota = tutto ok). */
    private static List<String> checkBaseTool(GameTestHelper helper, Item item) {
        List<String> problems = new ArrayList<>();
        MinecraftServer server = helper.getLevel().getServer();
        String itemId = BuiltInRegistries.ITEM.getKey(item).getPath();
        String kind = itemId.substring(itemId.indexOf('_') + 1);
        ItemStack stack = new ItemStack(item);

        if (!stack.has(DataComponents.UNBREAKABLE)) problems.add("manca il componente unbreakable");
        if (!stack.has(DataComponents.FIRE_RESISTANT)) problems.add("non resiste al fuoco");
        if (stack.is(ItemTags.DURABILITY_ENCHANTABLE)) problems.add("è ancora in #minecraft:enchantable/durability");
        // Le classi di balestra e bident di Spectrum accettano la malachite pura (hardcoded): da noi niente riparazione.
        if (item.isValidRepairItem(stack, new ItemStack(SpectrumItems.PURE_MALACHITE.get()))) problems.add("si ripara con la malachite pura");
        for (TagKey<Item> tag : KIND_TAGS.getOrDefault(kind, List.of())) {
            if (!stack.is(tag)) problems.add("non è in #" + tag.location());
        }

        String tier = tierOf(itemId);
        ResourceLocation unlock = SpectralAscension.id("unlocks/equipment/" + tier + "/" + itemId);
        if (server.getAdvancements().get(unlock) == null) problems.add("advancement mancante " + unlock);

        boolean fusion = FUSION_BASE.containsKey(itemId);
        ResourceLocation recipeId = SpectralAscension.id((fusion ? "fusion_shrine/" : "pedestal/tier4/") + tier + "/" + itemId);
        var holder = server.getRecipeManager().byKey(recipeId);
        if (holder.isEmpty()) {
            problems.add("ricetta mancante " + recipeId);
            return problems;
        }
        var recipe = holder.get().value();
        if (fusion) {
            if (!(recipe instanceof FusionShrineRecipe fusionRecipe)) {
                problems.add(recipeId + " non è una ricetta Fusion Shrine");
                return problems;
            }
            problems.addAll(checkFusion(helper, fusionRecipe, itemId));
        } else if (!(recipe instanceof PedestalRecipe pedestal)) {
            problems.add(recipeId + " non è una ricetta Pedestal");
            return problems;
        } else if (pedestal.getTier() != PedestalRecipeTier.COMPLEX) {
            problems.add("ricetta di tier " + pedestal.getTier() + " invece di complex");
        }
        // Dal T7 ogni pezzo parte dal pezzo del tier precedente: deve essere tra gli ingredienti e il suo sblocco
        // tra i criteri dell'advancement (checklist §A/§B), per Pedestal e Fusion Shrine.
        if (PREVIOUS_PREFIX.containsKey(tier)) {
            String previous = PREVIOUS_PREFIX.get(tier) + kind;
            Item previousItem = BuiltInRegistries.ITEM.get(SpectralAscension.id(previous));
            boolean usesPrevious = recipe instanceof PedestalRecipe pr
                    ? pr.getIngredients().stream().anyMatch(i -> i.test(new ItemStack(previousItem)))
                    : recipe instanceof FusionShrineRecipe fr && fr.getIngredientStacks().stream().anyMatch(i -> i.test(new ItemStack(previousItem)));
            if (!usesPrevious) problems.add("la ricetta non usa " + previous);
            var advPrev = server.getAdvancements().get(unlock);
            if (advPrev != null) {
                var names = advPrev.value().criteria().keySet();
                if (!names.contains("unlocked_" + previous)) problems.add("l'advancement non richiede lo sblocco di " + previous);
                if (!names.contains("unlocked_" + tier)) problems.add("l'advancement non richiede il tier " + tier);
                if (!allRequiredWithTick(advPrev.value().requirements(), names.size())) problems.add("i criteri non sono tutti obbligatori");
            }
        }

        ItemStack out = recipe.getResultItem(helper.getLevel().registryAccess());
        if (!out.is(item)) problems.add("la ricetta produce " + out + " invece di " + itemId);

        // Gli incantesimi della ricetta devono coincidere con quelli preimpostati dell'oggetto (SAItems).
        Map<ResourceKey<Enchantment>, Integer> expected = SAItems.defaultEnchantments(item);
        if (expected.isEmpty()) problems.add("nessun incantesimo preimpostato");
        ItemEnchantments ench = out.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        var registry = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        for (var e : expected.entrySet()) {
            Holder<Enchantment> h = registry.getOrThrow(e.getKey());
            if (ench.getLevel(h) != e.getValue()) {
                problems.add(e.getKey().location() + " a livello " + ench.getLevel(h) + " invece di " + e.getValue());
            }
            if (!stack.supportsEnchantment(h)) problems.add("non accetta " + e.getKey().location());
        }
        if (ench.size() != expected.size()) problems.add("incantesimi del risultato diversi da quelli preimpostati: " + ench);

        var required = ((GatedRecipe<?>) recipe).getRequiredAdvancement();
        if (required.isEmpty() || !required.get().equals(unlock)) {
            problems.add("required_advancement " + required + " invece di " + unlock + " (checklist §A)");
        }
        // L'advancement dell'oggetto richiede il tier (Pedestal Moonstone) e, per la Fusion Shrine, lo sblocco
        // dell'oggetto di Spectrum di partenza (la Fusion Shrine non ha tier: checklist D2).
        var adv = server.getAdvancements().get(unlock);
        if (adv != null && fusion) {
            var criteria = adv.value().criteria();
            List<String> missing = new ArrayList<>(FUSION_BASE.get(itemId).subList(1, FUSION_BASE.get(itemId).size()));
            boolean hasTier = false;
            for (var c : criteria.values()) {
                if (c.triggerInstance() instanceof AdvancementGottenCriterion.Conditions cond) {
                    hasTier |= cond.getAdvancementIdentifier().equals(SpectralAscension.id(gatePath(tier)));
                    missing.remove(cond.getAdvancementIdentifier().toString());
                }
            }
            if (!hasTier) problems.add("l'advancement non richiede " + gatePath(tier));
            for (String base : missing) problems.add("l'advancement non richiede " + base);
            if (!allRequiredWithTick(adv.value().requirements(), criteria.size())) problems.add("i criteri non sono tutti obbligatori (ogni lucchetto = gotten OR tick)");
        }
        return problems;
    }

    /**
     * Ogni lucchetto è una riga di requirements con due criteri in OR (advancement_gotten e il tick che controlla
     * di continuo lo stesso advancement); righe diverse restano in AND (vedi gen_data.gotten_with_tick).
     */
    private static boolean allRequiredWithTick(net.minecraft.advancements.AdvancementRequirements reqs, int criteriaCount) {
        int total = 0;
        for (var group : reqs.requirements()) {
            if (group.size() != 2) return false;
            total += group.size();
        }
        return total == criteriaCount;
    }

    /**
     * Ricetta Fusion Shrine: la base è il primo ingrediente, al massimo 7 ingredienti (liquido escluso), il liquido del
     * tier, copy_components sempre (anche sulle basi bedrock del T6), niente upgrade Yield.
     */
    private static List<String> checkFusion(GameTestHelper helper, FusionShrineRecipe recipe, String itemId) {
        List<String> problems = new ArrayList<>();
        var ingredients = recipe.getIngredientStacks();
        ItemStack base = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(FUSION_BASE.get(itemId).get(0))));
        if (base.isEmpty()) problems.add("base inesistente " + FUSION_BASE.get(itemId).get(0));
        if (ingredients.isEmpty() || !ingredients.getFirst().test(base)) {
            problems.add("il primo ingrediente non è " + FUSION_BASE.get(itemId).get(0) + " (copy_components copia da lì)");
        }
        if (ingredients.size() > 7) problems.add(ingredients.size() + " ingredienti: la Fusion Shrine ne tiene al massimo 7");
        // Il pezzo del T6 non si fa più al Pedestal: nessuna ricetta Pedestal deve produrlo (niente doppioni).
        Item result = BuiltInRegistries.ITEM.get(SpectralAscension.id(itemId));
        var access = helper.getLevel().registryAccess();
        for (var other : helper.getLevel().getServer().getRecipeManager().getRecipes()) {
            if (other.value() instanceof PedestalRecipe && other.value().getResultItem(access).is(result)) {
                problems.add("c'è ancora la ricetta Pedestal " + other.id());
            }
        }
        if (!recipe.areYieldAndEfficiencyUpgradesDisabled()) problems.add("manca disable_yield_upgrades (rischio dupe)");
        // copy_components non ha un getter: lo si legge ricodificando la ricetta come la legge Spectrum.
        var json = FusionShrineRecipe.Serializer.CODEC.codec()
                .encodeStart(helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE), recipe)
                .result().map(j -> (JsonObject) j).orElse(new JsonObject());
        boolean copies = json.has("copy_components") && json.get("copy_components").getAsBoolean();
        if (!copies) problems.add("manca copy_components");
        String fluid = FUSION_FLUID.get(tierOf(itemId));
        if (fluid != null && (!json.has("fluid") || !json.get("fluid").toString().contains(fluid))) {
            problems.add("liquido " + json.get("fluid") + " invece di " + fluid);
        }
        return problems;
    }

    /** Chiama GatedSpectrumRecipe.copyComponents (protected), la stessa fusione che fa la Fusion Shrine. */
    private static ItemStack fusionResult(ItemStack recipeOutput, ItemStack input) throws ReflectiveOperationException {
        Method copy = GatedSpectrumRecipe.class.getDeclaredMethod("copyComponents", ItemStack.class, ItemStack.class);
        copy.setAccessible(true);
        return (ItemStack) copy.invoke(null, recipeOutput.copy(), input);
    }

    /** Ritorna una descrizione dei problemi degli incantesimi di uno stack rispetto a quelli attesi ("" = ok). */
    private static String enchantmentProblems(GameTestHelper helper, ItemStack stack, Map<ResourceKey<Enchantment>, Integer> expected) {
        return enchantmentProblems(helper, stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY), expected);
    }

    private static String enchantmentProblems(GameTestHelper helper, ItemEnchantments ench, Map<ResourceKey<Enchantment>, Integer> expected) {
        var registry = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        for (var e : expected.entrySet()) {
            if (ench.getLevel(registry.getOrThrow(e.getKey())) != e.getValue()) return "atteso " + expected + ", trovato " + ench;
        }
        return ench.size() == expected.size() ? "" : "atteso " + expected + ", trovato " + ench;
    }

    /** Costruisce una lista di incantesimi (come minecraft:enchantments) da una mappa id → livello. */
    private static ItemEnchantments enchantments(GameTestHelper helper, Map<ResourceKey<Enchantment>, Integer> levels) {
        var registry = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        ItemEnchantments.Mutable ench = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        levels.forEach((key, level) -> ench.set(registry.getOrThrow(key), level));
        return ench.toImmutable();
    }

    /** Salva e ricarica uno stack (come il salvataggio del mondo): i componenti devono sopravvivere. */
    private static ItemStack reload(GameTestHelper helper, ItemStack stack) {
        var ops = helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        return ItemStack.CODEC.parse(ops, ItemStack.CODEC.encodeStart(ops, stack).getOrThrow()).getOrThrow();
    }

    /** Somma il valore di un attributo che il pezzo dà nel suo slot. */
    private static double attribute(ItemStack stack, EquipmentSlot slot, Holder<Attribute> attribute) {
        double[] total = {0};
        stack.forEachModifier(slot, (attr, modifier) -> {
            if (attr.equals(attribute)) total[0] += modifier.amount();
        });
        return total[0];
    }

    /** Controlli in più per l'armatura (quelli comuni sono in checkBaseTool). */
    private static List<String> checkArmor(GameTestHelper helper, SAArmorItem item) {
        List<String> problems = new ArrayList<>();
        ItemStack stack = new ItemStack(item);
        EquipmentSlot slot = item.getEquipmentSlot();
        String itemId = BuiltInRegistries.ITEM.getKey(item).getPath();
        String tier = tierOf(itemId);
        ArmorStats expected = ARMOR_STATS.get(tier);
        double armor = attribute(stack, slot, Attributes.ARMOR);
        double toughness = attribute(stack, slot, Attributes.ARMOR_TOUGHNESS);
        double knockback = attribute(stack, slot, Attributes.KNOCKBACK_RESISTANCE);
        if (armor != expected.armor().get(item.getType())) problems.add("armatura " + armor + " invece di " + expected.armor().get(item.getType()));
        if (Math.abs(toughness - expected.toughness()) > 1e-4) problems.add("toughness " + toughness + " invece di " + expected.toughness());
        if (Math.abs(knockback - expected.knockback()) > 1e-4) problems.add("knockback " + knockback + " invece di " + expected.knockback());
        if (stack.is(ItemTags.TRIMMABLE_ARMOR)) problems.add("è in #minecraft:trimmable_armor (la bedrock ne è esclusa)");
        if (item.armorFrames() != ARMOR_FRAMES.get(tier)) problems.add("fotogrammi dell'armatura " + item.armorFrames() + " invece di " + ARMOR_FRAMES.get(tier));

        // L'advancement di sblocco richiede gli sblocchi degli accessori usati nella ricetta (checklist §A/§B).
        var adv = helper.getLevel().getServer().getAdvancements().get(SpectralAscension.id("unlocks/equipment/" + tier + "/" + itemId));
        if (adv != null) {
            // Nomi dei criteri scritti da tools/gen_data.py ("unlocked_<ultima parte dell'id>"); gli id li controlla validate.py.
            var criteria = adv.value().criteria().keySet();
            for (String unlock : ARMOR_TRINKET_UNLOCKS.getOrDefault(itemId, List.of())) {
                String name = "unlocked_" + unlock.substring(unlock.lastIndexOf('/') + 1);
                if (!criteria.contains(name)) problems.add("l'advancement non richiede " + unlock + " (criterio " + name + ")");
            }
            if (!criteria.contains("unlocked_" + tier)) problems.add("l'advancement non richiede il tier " + tier);
            if (!allRequiredWithTick(adv.value().requirements(), criteria.size())) problems.add("i criteri non sono tutti obbligatori (ogni lucchetto = gotten OR tick)");
        }
        return problems;
    }

    /**
     * Giocatore finto con una connessione che scarta i pacchetti. Non entra nella lista dei giocatori:
     * makeMockServerPlayerInLevel lo farebbe, e le altre mod (es. Modonomicon) al login gli manderebbero pacchetti
     * che la connessione finta rifiuta. Serve comunque una connessione: NeoForge manda al giocatore stesso i dati
     * sincronizzati (come l'Azure Dike) quando cambiano.
     */
    private static ServerPlayer mockPlayer(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        GameProfile profile = new GameProfile(UUID.randomUUID(), "sa-test-player");
        ServerPlayer player = new ServerPlayer(server, helper.getLevel(), profile, ClientInformation.createDefault());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        player.connection = new ServerGamePacketListenerImpl(server, connection, player, CommonListenerCookie.createInitial(profile, false)) {
            @Override
            public void send(Packet<?> packet) {
            }

            @Override
            public void send(Packet<?> packet, @Nullable PacketSendListener listener) {
            }
        };
        return player;
    }

    /** Mette o toglie un pezzo e avvisa i listener come farebbe il tick del giocatore. */
    private static void equip(LivingEntity entity, EquipmentSlot slot, ItemStack stack) {
        ItemStack from = entity.getItemBySlot(slot).copy();
        entity.setItemSlot(slot, stack);
        NeoForge.EVENT_BUS.post(new LivingEquipmentChangeEvent(entity, slot, from, stack));
    }

    /** Percorso del lucchetto di un tier, col nome del tier (2026-11-23): unlocks/tiers/trichromatic, umbral, argent, prismatic. */
    static String gatePath(String tier) {
        return "unlocks/tiers/" + switch (tier) {
            case "t6" -> "trichromatic";
            case "t7" -> "umbral";
            case "t8" -> "argent";
            case "t9" -> "prismatic";
            default -> throw new IllegalArgumentException("tier sconosciuto: " + tier);
        };
    }

    // ---------- Test ----------

    @GameTest(template = EMPTY)
    public static void tier_unlocks(GameTestHelper helper) {
        for (String tier : List.of("t6", "t7", "t8", "t9")) {
            ResourceLocation id = SpectralAscension.id(gatePath(tier));
            var adv = helper.getLevel().getServer().getAdvancements().get(id);
            helper.assertTrue(adv != null, "advancement mancante: " + id);
            // Catena dei tier (checklist C): il T7 richiede il T6 e il T8 il T7, in AND con il lingotto del tier
            // (e, per il T8, con gli attrezzi glass crest: TIER_EXTRA_UNLOCKS).
            String previous = PREVIOUS_TIER.get(tier);
            if (previous != null) {
                List<String> required = new ArrayList<>(TIER_EXTRA_UNLOCKS.getOrDefault(tier, List.of()));
                required.add(SpectralAscension.id(gatePath(previous)).toString());
                for (String req : required) {
                    boolean found = adv.value().criteria().values().stream().anyMatch(c ->
                            c.triggerInstance() instanceof AdvancementGottenCriterion.Conditions cond
                                    && cond.getAdvancementIdentifier().toString().equals(req));
                    helper.assertTrue(found, id + " non richiede " + req);
                }
                helper.assertTrue(adv.value().requirements().requirements().size() == required.size() + 1,
                        id + ": requisiti " + adv.value().requirements());
            }
            // T9: l'ultima riga di requirements è l'OR dei tre lingotti di lega (uno qualsiasi basta).
            List<String> ingotsOr = TIER_INGOTS_OR.get(tier);
            if (ingotsOr != null) {
                var rows = adv.value().requirements().requirements();
                var last = rows.get(rows.size() - 1);
                helper.assertTrue(last.size() == ingotsOr.size(), id + ": l'OR dei lingotti ha " + last.size() + " criteri invece di " + ingotsOr.size());
                for (String tag : ingotsOr) {
                    String name = "has_" + tag.substring(tag.lastIndexOf('/') + 1) + "_ingot";
                    helper.assertTrue(last.contains(name), id + ": manca il lingotto " + tag + " nell'OR");
                }
            }
        }
        helper.succeed();
    }

    /** Controlla tutti gli oggetti della sotto-tab Equipment e riporta tutti i problemi in una volta. */
    @GameTest(template = EMPTY)
    public static void base_equipment(GameTestHelper helper) {
        List<String> report = new ArrayList<>();
        for (var holder : SAItems.equipment()) {
            for (String p : checkBaseTool(helper, holder.get())) {
                report.add(holder.getId().getPath() + ": " + p);
            }
        }
        if (!report.isEmpty()) {
            report.forEach(r -> SpectralAscension.LOGGER.error("[GameTest] {}", r));
            helper.fail(report.size() + " problemi, il primo: " + report.getFirst());
        }
        SpectralAscension.LOGGER.info("[GameTest] {} oggetti controllati senza problemi", SAItems.equipment().size());
        helper.succeed();
    }

    /** Armatura: valori degli attributi, tag e sblocchi degli accessori. */
    @GameTest(template = EMPTY)
    public static void armor_pieces(GameTestHelper helper) {
        List<String> report = new ArrayList<>();
        for (var holder : SAItems.armor()) {
            for (String p : checkArmor(helper, holder.get())) {
                report.add(holder.getId().getPath() + ": " + p);
            }
        }
        if (!report.isEmpty()) {
            report.forEach(r -> SpectralAscension.LOGGER.error("[GameTest] {}", r));
            helper.fail(report.size() + " problemi, il primo: " + report.getFirst());
        }
        helper.succeed();
    }

    /**
     * Dike del set: un giocatore finto indossa i 4 pezzi → 40 di Dike; togliendo un pezzo → 0.
     * Con un Azure Dike Ring in uno slot Curios: col set resta 40 (accessorio disattivato), senza set torna 6.
     */
    @GameTest(template = EMPTY)
    public static void armor_set_dike(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        var stats = SAArmorSet.dikeStats();
        helper.assertTrue(stats.max() == SET_DIKE, "Dike del set calcolato " + stats + " invece di " + SET_DIKE);
        helper.assertTrue(stats.ticksPerCharge() == 16 && stats.rechargeDelayAfterDamage() == 66,
                "ricarica del set " + stats + " invece di 16 / 66 tick");

        float max = 0;
        // Ogni tier col suo set completo, poi un set misto (Alloy + stivali T6: vale qualunque pezzo nostro).
        for (SATier tier : List.of(SATier.T6, SATier.T7, SATier.T8, SATier.ALLOY)) {
            for (var holder : SAItems.armor(tier)) {
                equip(player, holder.get().getEquipmentSlot(), new ItemStack(holder.get()));
            }
            max = player.getData(AzureDikeAttachmentType.ATTACHMENT_TYPE).getMaxCharges();
            helper.assertTrue(max == SET_DIKE, tier + ": con il set completo il Dike massimo è " + max + " invece di " + SET_DIKE);
            equip(player, EquipmentSlot.FEET, ItemStack.EMPTY);
            max = player.getData(AzureDikeAttachmentType.ATTACHMENT_TYPE).getMaxCharges();
            helper.assertTrue(max == 0, tier + ": senza stivali il Dike massimo è " + max + " invece di 0");
        }
        equip(player, EquipmentSlot.FEET, new ItemStack(SAItems.TRICHROMATIC_BOOTS.get()));
        max = player.getData(AzureDikeAttachmentType.ATTACHMENT_TYPE).getMaxCharges();
        helper.assertTrue(max == SET_DIKE, "set misto Alloy + stivali T6: Dike " + max + " invece di " + SET_DIKE);

        var dike = player.getData(AzureDikeAttachmentType.ATTACHMENT_TYPE);
        dike.absorbDamage(player, 2);
        dike.serverTick(player);
        int remainingDelay = dike.getCurrentRechargeDelay();
        equip(player, EquipmentSlot.HEAD, new ItemStack(SAItems.UMBRAL_HELMET.get()));
        helper.assertTrue(remainingDelay == stats.rechargeDelayAfterDamage() - 1
                        && dike.getCurrentRechargeDelay() == remainingDelay,
                "cambiare elmo con il set completo ha alterato il ritardo dell'Azure Dike");
        equip(player, EquipmentSlot.FEET, ItemStack.EMPTY);

        // Accessorio Curios: disattivato dal set, di nuovo attivo senza set.
        var curios = CuriosApi.getCuriosInventory(player);
        if (curios.isPresent() && curios.get().getStacksHandler("ring").map(h -> h.getSlots() > 0).orElse(false)) {
            curios.get().setEquippedCurio("ring", 0, new ItemStack(SpectrumItems.AZURE_DIKE_RING.get()));
            equip(player, EquipmentSlot.FEET, new ItemStack(SAItems.TRICHROMATIC_BOOTS.get()));
            AzureDikeAttachmentType.recalculate(player);
            max = player.getData(AzureDikeAttachmentType.ATTACHMENT_TYPE).getMaxCharges();
            helper.assertTrue(max == SET_DIKE, "set + Azure Dike Ring: Dike " + max + " invece di " + SET_DIKE + " (l'anello deve essere disattivato)");
            equip(player, EquipmentSlot.HEAD, ItemStack.EMPTY);
            max = player.getData(AzureDikeAttachmentType.ATTACHMENT_TYPE).getMaxCharges();
            helper.assertTrue(max == 6, "solo Azure Dike Ring: Dike " + max + " invece di 6");
            curios.get().setEquippedCurio("ring", 0, ItemStack.EMPTY);
            SpectralAscension.LOGGER.info("[GameTest] Dike del set: 40 col set T6, T7, T8, Alloy e misto, 0 senza, anello Curios disattivato col set (6 senza)");
        } else {
            SpectralAscension.LOGGER.warn("[GameTest] slot Curios 'ring' assente: controllo degli accessori disattivati saltato");
        }
        helper.succeed();
    }

    /**
     * Accessori Dike NON fusi nell'armatura (2026-11-27): col set Prismatic completo gli Aether-Graced Nectar Gloves
     * in Curios si sommano prima del × 2 del Core: (20 + 10) × 2 = 60, con amuleto pieno (bonus 10) 80. Un Azure Dike
     * Ring in più (fuso nel pettorale) non conta due volte; togliendo i guanti si torna a 40. Il ricalcolo passa da
     * onEquip/onUnequip dei guanti, come fa Curios in gioco.
     */
    @GameTest(template = EMPTY)
    public static void armor_set_dike_extra_trinkets(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        var curios = CuriosApi.getCuriosInventory(player);
        if (curios.isEmpty() || !curios.get().getStacksHandler("hands").map(h -> h.getSlots() > 0).orElse(false)) {
            helper.fail("slot Curios 'hands' assente sul giocatore finto");
            return;
        }
        for (var holder : SAItems.armor(SATier.ALLOY)) {
            equip(player, holder.get().getEquipmentSlot(), new ItemStack(holder.get()));
        }
        ItemStack gloves = new ItemStack(SpectrumItems.AETHER_GRACED_NECTAR_GLOVES.get());
        var glovesItem = (top.theillusivec4.curios.api.type.capability.ICurioItem) gloves.getItem();
        var handsSlot = new top.theillusivec4.curios.api.SlotContext("hands", player, 0, false, true);
        curios.get().setEquippedCurio("hands", 0, gloves);
        glovesItem.onEquip(handsSlot, ItemStack.EMPTY, gloves);
        float max = player.getData(AzureDikeAttachmentType.ATTACHMENT_TYPE).getMaxCharges();
        helper.assertTrue(max == 60, "set Prismatic + guanti: Dike " + max + " invece di 60");

        ItemStack boots = new ItemStack(SAItems.PRISMATIC_BOOTS.get());
        boots.set(SAComponents.DIKE_BONUS.get(), 10);
        equip(player, EquipmentSlot.FEET, boots);
        max = player.getData(AzureDikeAttachmentType.ATTACHMENT_TYPE).getMaxCharges();
        helper.assertTrue(max == 80, "set Prismatic (bonus 10) + guanti: Dike " + max + " invece di 80");

        if (curios.get().getStacksHandler("ring").map(h -> h.getSlots() > 0).orElse(false)) {
            curios.get().setEquippedCurio("ring", 0, new ItemStack(SpectrumItems.AZURE_DIKE_RING.get()));
            AzureDikeAttachmentType.recalculate(player);
            max = player.getData(AzureDikeAttachmentType.ATTACHMENT_TYPE).getMaxCharges();
            helper.assertTrue(max == 80, "set + guanti + Azure Dike Ring: Dike " + max + " invece di 80 (anello già fuso)");
            curios.get().setEquippedCurio("ring", 0, ItemStack.EMPTY);
        }

        curios.get().setEquippedCurio("hands", 0, ItemStack.EMPTY);
        glovesItem.onUnequip(handsSlot, ItemStack.EMPTY, gloves);
        max = player.getData(AzureDikeAttachmentType.ATTACHMENT_TYPE).getMaxCharges();
        helper.assertTrue(max == 60, "set Prismatic (bonus 10) senza guanti: Dike " + max + " invece di 60");
        SpectralAscension.LOGGER.info("[GameTest] Dike del set con accessori non fusi: guanti 60, con bonus 10 80, anello fuso ignorato, senza guanti 60");
        helper.succeed();
    }

    /**
     * Dike del set su un'entità senza inventario Curios (armor stand): AzureDikeAttachmentTypeMixin imposta il Dike del
     * set e lo toglie da solo, perché lì Spectrum non lo ricalcola. Set T6 completo → 40; senza stivali → 0.
     */
    @GameTest(template = EMPTY)
    public static void armor_set_dike_no_curios(GameTestHelper helper) {
        ArmorStand stand = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(1, 2, 1));
        helper.assertFalse(cromocon.spectralascension.compat.SACurios.hasInventory(stand),
                "l'armor stand ha un inventario Curios: il ramo senza Curios del mixin non è provato");
        for (var holder : SAItems.armor(SATier.T6)) {
            equip(stand, holder.get().getEquipmentSlot(), new ItemStack(holder.get()));
        }
        float max = stand.getData(AzureDikeAttachmentType.ATTACHMENT_TYPE).getMaxCharges();
        helper.assertTrue(max == SET_DIKE, "armor stand con il set T6: Dike massimo " + max + " invece di " + SET_DIKE);
        equip(stand, EquipmentSlot.FEET, ItemStack.EMPTY);
        max = stand.getData(AzureDikeAttachmentType.ATTACHMENT_TYPE).getMaxCharges();
        helper.assertTrue(max == 0, "armor stand senza stivali: Dike massimo " + max + " invece di 0");
        SpectralAscension.LOGGER.info("[GameTest] Dike su armor stand (senza Curios): 40 col set T6, 0 senza stivali");
        stand.discard();
        helper.succeed();
    }

    /**
     * Upgrade della workstaff glass crest alla Fusion Shrine: copy_components fonde gli incantesimi dell'input con
     * quelli del result. Qualunque selezione avesse la glass crest (Fortune IV, Silk Touch, Resonance), il risultato
     * ha solo Fortune V + Efficiency V e conserva l'area di scavo scelta.
     */
    @GameTest(template = EMPTY)
    public static void workstaff_fusion_merge(GameTestHelper helper) throws ReflectiveOperationException {
        var access = helper.getLevel().registryAccess();
        var registry = access.lookupOrThrow(Registries.ENCHANTMENT);
        var recipe = (FusionShrineRecipe) helper.getLevel().getServer().getRecipeManager()
                .byKey(SpectralAscension.id("fusion_shrine/t6/trichromatic_workstaff")).orElseThrow().value();
        ItemStack output = recipe.getResultItem(access);
        Map<ResourceKey<Enchantment>, Integer> expected = SAItems.defaultEnchantments(SAItems.TRICHROMATIC_WORKSTAFF.get());

        for (var selected : List.of(Enchantments.FORTUNE, Enchantments.SILK_TOUCH, SpectrumEnchantmentKeys.RESONANCE)) {
            ItemStack glassCrest = new ItemStack(SpectrumItems.GLASS_CREST_WORKSTAFF.get());
            ItemEnchantments.Mutable ench = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            ench.set(registry.getOrThrow(selected), selected == Enchantments.FORTUNE ? 4 : 1);
            glassCrest.set(DataComponents.ENCHANTMENTS, ench.toImmutable());
            glassCrest.set(SpectrumDataComponentTypes.AOE.get(), 2);
            ItemStack result = fusionResult(output, glassCrest);
            helper.assertTrue(result.is(SAItems.TRICHROMATIC_WORKSTAFF.get()), "la fusione produce " + result);
            String p = enchantmentProblems(helper, result, expected);
            helper.assertTrue(p.isEmpty(), "glass crest con " + selected.location() + ": " + p);
            helper.assertTrue(result.getOrDefault(SpectrumDataComponentTypes.AOE.get(), 0) == 2, "area di scavo 5×5 non conservata");
            helper.assertTrue(result.has(DataComponents.UNBREAKABLE), "dopo la fusione non è più indistruttibile");
        }
        SpectralAscension.LOGGER.info("[GameTest] fusione della workstaff: Fortune V + Efficiency V da Fortune IV / Silk Touch / Resonance");
        helper.succeed();
    }

    /**
     * Base bedrock alla Fusion Shrine con copy_components: si tengono gli incantesimi del pezzo bedrock e quelli del
     * result, e per lo stesso incantesimo vince il livello più alto (addOrUpgradeEnchantment non abbassa mai).
     * Spada: Sharpness V + Looting III (+ Unbreaking III, che il pezzo indistruttibile può scartare) danno Sharpness VII +
     * Looting III; un pezzo con Sharpness IX (oltre il result) resta a IX. Arco: Power superiore al result vince.
     */
    @GameTest(template = EMPTY)
    public static void bedrock_fusion_merge(GameTestHelper helper) throws ReflectiveOperationException {
        var access = helper.getLevel().registryAccess();
        var registry = access.lookupOrThrow(Registries.ENCHANTMENT);
        var manager = helper.getLevel().getServer().getRecipeManager();

        var swordRecipe = (FusionShrineRecipe) manager.byKey(SpectralAscension.id("fusion_shrine/t6/trichromatic_sword")).orElseThrow().value();
        ItemStack sword = new ItemStack(SpectrumItems.BEDROCK_SWORD.get());
        sword.set(DataComponents.ENCHANTMENTS, enchantments(helper, Map.of(Enchantments.SHARPNESS, 5,
                Enchantments.UNBREAKING, 3, Enchantments.LOOTING, 3)));
        ItemStack result = fusionResult(swordRecipe.getResultItem(access), sword);
        helper.assertTrue(result.is(SAItems.TRICHROMATIC_SWORD.get()), "la fusione produce " + result);
        Map<ResourceKey<Enchantment>, Integer> expected = new java.util.HashMap<>(Map.of(Enchantments.SHARPNESS, 7, Enchantments.LOOTING, 3));
        if (result.supportsEnchantment(registry.getOrThrow(Enchantments.UNBREAKING))) expected.put(Enchantments.UNBREAKING, 3);
        String p = enchantmentProblems(helper, result, expected);
        helper.assertTrue(p.isEmpty(), "spada bedrock Sharpness V + Looting III: " + p);

        sword.set(DataComponents.ENCHANTMENTS, enchantments(helper, Map.of(Enchantments.SHARPNESS, 9)));
        p = enchantmentProblems(helper, fusionResult(swordRecipe.getResultItem(access), sword), Map.of(Enchantments.SHARPNESS, 9));
        helper.assertTrue(p.isEmpty(), "spada bedrock Sharpness IX deve restare IX: " + p);

        var bowRecipe = (FusionShrineRecipe) manager.byKey(SpectralAscension.id("fusion_shrine/t6/trichromatic_bow")).orElseThrow().value();
        int power = SAItems.defaultEnchantments(SAItems.TRICHROMATIC_BOW.get()).getOrDefault(Enchantments.POWER, 0);
        ItemStack bow = new ItemStack(SpectrumItems.BEDROCK_BOW.get());
        bow.set(DataComponents.ENCHANTMENTS, enchantments(helper, Map.of(Enchantments.POWER, power + 2, Enchantments.PUNCH, 2)));
        ItemStack bowResult = fusionResult(bowRecipe.getResultItem(access), bow);
        p = enchantmentProblems(helper, bowResult, with(SAItems.defaultEnchantments(SAItems.TRICHROMATIC_BOW.get()),
                Map.of(Enchantments.POWER, power + 2, Enchantments.PUNCH, 2)));
        helper.assertTrue(p.isEmpty(), "arco bedrock con Power " + (power + 2) + ": " + p);
        // Livelli uguali non si sommano (niente incudine): Efficiency VII sull'ascia bedrock + Efficiency VII nel result = VII.
        var axeRecipe = (FusionShrineRecipe) manager.byKey(SpectralAscension.id("fusion_shrine/t6/trichromatic_axe")).orElseThrow().value();
        Map<ResourceKey<Enchantment>, Integer> axeBase = SAItems.defaultEnchantments(SAItems.TRICHROMATIC_AXE.get());
        helper.assertTrue(axeBase.getOrDefault(Enchantments.EFFICIENCY, 0) == 7, "il result dell'ascia non ha Efficiency VII: " + axeBase);
        ItemStack axe = new ItemStack(SpectrumItems.BEDROCK_AXE.get());
        axe.set(DataComponents.ENCHANTMENTS, enchantments(helper, Map.of(Enchantments.EFFICIENCY, 7)));
        p = enchantmentProblems(helper, fusionResult(axeRecipe.getResultItem(access), axe), axeBase);
        helper.assertTrue(p.isEmpty(), "ascia bedrock Efficiency VII + result Efficiency VII deve dare VII, non VIII: " + p);
        SpectralAscension.LOGGER.info("[GameTest] fusione bedrock: spada Sharpness VII + Looting III da Sharpness V + Looting III, Sharpness IX resta IX, Efficiency VII + VII = VII, Power più alto dell'arco vince");
        helper.succeed();
    }

    /**
     * Menu della workstaff (WorkstaffItem.applyToggle, come il pacchetto del menu): Fortune → Silk Touch →
     * Resonance → Fortune riporta Fortune V, Efficiency V resta sempre.
     */
    @GameTest(template = EMPTY)
    public static void workstaff_menu_toggle(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        var access = helper.getLevel().registryAccess();
        var registry = access.lookupOrThrow(Registries.ENCHANTMENT);
        var item = SAItems.TRICHROMATIC_WORKSTAFF.get();
        ItemStack staff = new ItemStack(item);
        staff.set(DataComponents.ENCHANTMENTS, Preenchanted.buildDefaultEnchantments(access, item));
        Map<ResourceKey<Enchantment>, Integer> expected = SAItems.defaultEnchantments(item);

        WorkstaffItem.applyToggle(player, staff, WorkstaffItem.GUIToggle.SELECT_SILK_TOUCH);
        int efficiency = expected.get(Enchantments.EFFICIENCY);
        String p = enchantmentProblems(helper, staff, Map.of(Enchantments.SILK_TOUCH, 1, Enchantments.EFFICIENCY, efficiency));
        helper.assertTrue(p.isEmpty(), "dopo Silk Touch: " + p);
        WorkstaffItem.applyToggle(player, staff, WorkstaffItem.GUIToggle.SELECT_RESONANCE);
        p = enchantmentProblems(helper, staff, Map.of(SpectrumEnchantmentKeys.RESONANCE, 1, Enchantments.EFFICIENCY, efficiency));
        helper.assertTrue(p.isEmpty(), "dopo Resonance: " + p);
        WorkstaffItem.applyToggle(player, staff, WorkstaffItem.GUIToggle.SELECT_FORTUNE);
        p = enchantmentProblems(helper, staff, expected);
        helper.assertTrue(p.isEmpty(), "tornando a Fortune: " + p);

        // Area 5×5 dal menu, ereditata dalla glass crest.
        WorkstaffItem.applyToggle(player, staff, WorkstaffItem.GUIToggle.SELECT_5x5);
        helper.assertTrue(item.getAoERange(staff) == 2, "area 5×5 non selezionata");
        helper.assertTrue(staff.supportsEnchantment(registry.getOrThrow(Enchantments.SILK_TOUCH)), "non accetta Silk Touch");
        SpectralAscension.LOGGER.info("[GameTest] menu della workstaff: Fortune V → Silk Touch → Resonance → Fortune V, Efficiency V sempre");
        helper.succeed();
    }

    /**
     * Modalità del bident scelte dal menu, come in gioco: Shift + tasto destro apre il menu (BidentMenu, senza
     * caricare il lancio) e la scelta arriva al server come in BidentModePayload (BidentMenu.select). Sceglie
     * Ferocious → Fractal → spento; in ogni modalità controlla Riptide incorporato (solo Ferocious) e velocità di
     * lancio (5 solo Fractal), e che la modalità sopravviva al salvataggio dello stack.
     */
    @GameTest(template = EMPTY)
    public static void bident_mode_cycle(GameTestHelper helper) {
        checkBidentModeCycle(helper, SAItems.TRICHROMATIC_BIDENT.get());
    }

    /** Come bident_mode_cycle, per il bident T7 (stesse meccaniche del T6). */
    @GameTest(template = EMPTY)
    public static void bident_mode_cycle_t7(GameTestHelper helper) {
        checkBidentModeCycle(helper, SAItems.UMBRAL_BIDENT.get());
    }

    /** Come bident_mode_cycle, per il bident T8 (stesse meccaniche del T6). */
    @GameTest(template = EMPTY)
    public static void bident_mode_cycle_t8(GameTestHelper helper) {
        checkBidentModeCycle(helper, SAItems.ARGENT_BIDENT.get());
    }

    /** Come bident_mode_cycle, per il bident Alloy (T9, stesse meccaniche del T6). */
    @GameTest(template = EMPTY)
    public static void bident_mode_cycle_t9(GameTestHelper helper) {
        checkBidentModeCycle(helper, SAItems.PRISMATIC_BIDENT.get());
    }

    private static void checkBidentModeCycle(GameTestHelper helper, SABident item) {
        ServerPlayer player = mockPlayer(helper);
        var access = helper.getLevel().registryAccess();
        ItemStack bident = new ItemStack(item);
        helper.assertTrue(SABident.getMode(bident) == BidentMode.FEROCIOUS, "modalità iniziale " + SABident.getMode(bident) + " invece di Ferocious");

        SABident.setMode(bident, BidentMode.OFF);
        player.setItemInHand(InteractionHand.MAIN_HAND, bident);
        player.setShiftKeyDown(true);
        for (BidentMode expected : List.of(BidentMode.FEROCIOUS, BidentMode.FRACTAL, BidentMode.OFF)) {
            chooseFromMenu(helper, player, expected);
            ItemStack held = player.getMainHandItem();
            BidentMode mode = SABident.getMode(held);
            helper.assertTrue(mode == expected, "dopo la scelta nel menu la modalità è " + mode + " invece di " + expected);
            int riptide = item.getRiptideLevel(access, held);
            int expectedRiptide = expected == BidentMode.FEROCIOUS ? 1 : 0;
            helper.assertTrue(riptide == expectedRiptide, expected + ": Riptide " + riptide + " invece di " + expectedRiptide);
            float speed = item.getThrowSpeed(held);
            float expectedSpeed = expected == BidentMode.FRACTAL ? 5.0F : 3.0F;
            helper.assertTrue(speed == expectedSpeed, expected + ": velocità di lancio " + speed + " invece di " + expectedSpeed);

            // La modalità è salvata nello stack (componente persistente).
            var ops = access.createSerializationContext(NbtOps.INSTANCE);
            ItemStack reloaded = ItemStack.CODEC.parse(ops, ItemStack.CODEC.encodeStart(ops, held).getOrThrow()).getOrThrow();
            helper.assertTrue(SABident.getMode(reloaded) == expected, expected + ": modalità persa dopo il salvataggio");
        }
        helper.assertTrue(player.getMainHandItem().supportsEnchantment(access.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.POWER)),
                "non accetta Power (come il Fractal)");
        SpectralAscension.LOGGER.info("[GameTest] modalità del bident {} dal menu: spento → Ferocious → Fractal → spento, Riptide e lancio corretti",
                BuiltInRegistries.ITEM.getKey(item).getPath());
        helper.succeed();
    }

    /**
     * Come in gioco: Shift + tasto destro con il bident nella mano principale apre il menu (senza caricare il lancio
     * né cambiare modalità), poi la scelta passa da BidentMenu.select, come il pacchetto BidentModePayload, e il
     * menu si chiude.
     */
    private static void chooseFromMenu(GameTestHelper helper, ServerPlayer player, BidentMode mode) {
        ItemStack held = player.getMainHandItem();
        BidentMode before = SABident.getMode(held);
        player.setShiftKeyDown(true);
        held.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.containerMenu instanceof BidentMenu, "Shift + tasto destro non apre il menu del bident ("
                + player.containerMenu.getClass().getSimpleName() + ")");
        helper.assertTrue(!player.isUsingItem(), "con Shift il bident non deve caricare il lancio");
        helper.assertTrue(SABident.getMode(player.getMainHandItem()) == before, "aprire il menu ha cambiato la modalità");
        BidentMenu menu = (BidentMenu) player.containerMenu;
        helper.assertTrue(menu.stillValid(player), "il menu del bident non risulta valido con il bident in mano");
        helper.assertTrue(menu.select(player, mode.ordinal()), "il menu ha rifiutato la modalità " + mode);
        player.closeContainer();
    }

    /** La modalità che seguiva nel vecchio ciclo (spento → Ferocious → Fractal → spento): l'ordine dei test. */
    private static BidentMode nextInCycle(BidentMode mode) {
        return BidentMode.values()[(mode.ordinal() + 1) % BidentMode.values().length];
    }

    /**
     * Il menu del bident rifiuta le richieste non valide (come farebbe il server con un pacchetto di un client
     * modificato): indici di modalità fuori dai limiti, e un oggetto che non è un nostro bident. In tutti i casi
     * modalità e incantesimi restano com'erano; scegliere la modalità già attiva non cambia niente.
     */
    @GameTest(template = EMPTY)
    public static void bident_menu_rejects_invalid(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        var access = helper.getLevel().registryAccess();
        SABident item = SAItems.TRICHROMATIC_BIDENT.get();
        ItemStack bident = BidentEnchantments.applyFusion(new ItemStack(item),
                enchantments(helper, FEROCIOUS_INPUT), enchantments(helper, FRACTAL_INPUT), access, false);
        player.setItemInHand(InteractionHand.MAIN_HAND, bident);
        player.setShiftKeyDown(true);
        item.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.containerMenu instanceof BidentMenu, "il menu del bident non si è aperto");
        BidentMenu menu = (BidentMenu) player.containerMenu;
        ItemStack before = player.getMainHandItem().copy();

        for (int index : new int[]{-1, BidentMode.values().length, 99, Integer.MAX_VALUE}) {
            helper.assertTrue(!menu.select(player, index), "modalità non valida accettata: indice " + index);
            helper.assertTrue(ItemStack.isSameItemSameComponents(before, player.getMainHandItem()),
                    "l'indice " + index + " ha cambiato il bident: " + player.getMainHandItem().getComponents());
        }
        // La modalità già attiva (Ferocious, uscita dalla fusione) è valida ma non cambia niente.
        helper.assertTrue(menu.select(player, BidentMode.FEROCIOUS.ordinal()), "la modalità attiva è stata rifiutata");
        helper.assertTrue(ItemStack.isSameItemSameComponents(before, player.getMainHandItem()), "riscegliere Ferocious ha cambiato il bident");

        // Un oggetto che non è un nostro bident: il menu non è più valido e la scelta viene rifiutata, anche per
        // gli altri bident (il glass crest di Spectrum).
        for (ItemStack other : List.of(new ItemStack(Items.STICK), new ItemStack(SpectrumItems.FRACTAL_GLASS_CREST_BIDENT.get()))) {
            ItemStack copy = other.copy();
            player.setItemInHand(InteractionHand.MAIN_HAND, copy);
            helper.assertTrue(!menu.stillValid(player), "il menu resta valido con " + other.getItem() + " in mano");
            helper.assertTrue(!menu.select(player, BidentMode.FRACTAL.ordinal()), "modalità applicata a " + other.getItem());
            helper.assertTrue(!SABident.selectMode(player, InteractionHand.MAIN_HAND, BidentMode.FRACTAL.ordinal()),
                    "selectMode accetta " + other.getItem());
            helper.assertTrue(ItemStack.isSameItemSameComponents(other, copy), other.getItem() + " è stato modificato: " + copy.getComponents());
        }
        // Mano vuota: niente da cambiare.
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.assertTrue(!SABident.selectMode(player, InteractionHand.MAIN_HAND, BidentMode.OFF.ordinal()), "selectMode accetta la mano vuota");
        player.closeContainer();
        SpectralAscension.LOGGER.info("[GameTest] menu del bident: rifiutati indici -1, 3, 99, MAX e oggetti non bident (bastone, glass crest, mano vuota)");
        helper.succeed();
    }

    /**
     * Fusione del bident: secondo ingrediente il Fractal; copy_components copia dal Ferocious. Logica di
     * GatedSpectrumRecipe.copyComponents: prima gli incantesimi della ricetta (Impaling VIII), poi quelli dell'input se
     * l'oggetto li accetta, non sono in conflitto e hanno livello più alto. Quindi Looting e Riptide restano, Impaling
     * VI non abbassa la X, Sharpness cade (in 1.21 è nello stesso exclusive_set/damage di Impaling) e Unbreaking cade
     * (il bident è indistruttibile, fuori da #minecraft:enchantable/durability). Questo è solo copy_components:
     * il set Fractal lo aggiunge FusionShrineRecipeMixin nel craft vero (test bident_fusion_shrine_craft).
     */
    @GameTest(template = EMPTY)
    public static void bident_fusion_merge(GameTestHelper helper) throws ReflectiveOperationException {
        var access = helper.getLevel().registryAccess();
        var registry = access.lookupOrThrow(Registries.ENCHANTMENT);
        var recipe = (FusionShrineRecipe) helper.getLevel().getServer().getRecipeManager()
                .byKey(SpectralAscension.id("fusion_shrine/t6/trichromatic_bident")).orElseThrow().value();
        var ingredients = recipe.getIngredientStacks();
        helper.assertTrue(ingredients.size() > 1 && ingredients.get(1).test(new ItemStack(SpectrumItems.FRACTAL_GLASS_CREST_BIDENT.get())),
                "il secondo ingrediente non è il Fractal glass crest bident");

        ItemStack ferocious = new ItemStack(SpectrumItems.FEROCIOUS_GLASS_CREST_BIDENT.get());
        ItemEnchantments.Mutable ench = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        ench.set(registry.getOrThrow(Enchantments.IMPALING), 4);
        ench.set(registry.getOrThrow(Enchantments.SHARPNESS), 5);
        ench.set(registry.getOrThrow(Enchantments.LOOTING), 3);
        ench.set(registry.getOrThrow(Enchantments.RIPTIDE), 2);
        ench.set(registry.getOrThrow(Enchantments.UNBREAKING), 3);
        ferocious.set(DataComponents.ENCHANTMENTS, ench.toImmutable());
        ferocious.setDamageValue(100);
        ActivatableItem.setActivated(ferocious, false);

        ItemStack result = fusionResult(recipe.getResultItem(access), ferocious);
        helper.assertTrue(result.is(SAItems.TRICHROMATIC_BIDENT.get()), "la fusione produce " + result);
        int impaling = SAItems.defaultEnchantments(SAItems.TRICHROMATIC_BIDENT.get()).get(Enchantments.IMPALING);
        Map<ResourceKey<Enchantment>, Integer> want = Map.of(Enchantments.IMPALING, impaling, Enchantments.LOOTING, 3, Enchantments.RIPTIDE, 2);
        if (sharpnessWithImpaling(helper)) want = with(want, Map.of(Enchantments.SHARPNESS, 5)); // Apothic: niente conflitto, resta
        String p = enchantmentProblems(helper, result, want);
        helper.assertTrue(p.isEmpty(), "incantesimi dopo la fusione: " + p);
        helper.assertTrue(result.has(DataComponents.UNBREAKABLE), "dopo la fusione non è più indistruttibile");
        helper.assertTrue(SABident.getMode(result) == BidentMode.FEROCIOUS, "dopo la fusione la modalità è " + SABident.getMode(result));
        SpectralAscension.LOGGER.info("[GameTest] fusione del bident: Impaling V + Looting III, Riptide II del Ferocious; Sharpness (conflitto) e Unbreaking scartati");
        helper.succeed();
    }

    // Incantesimi dei due bident usati nei test qui sotto. Il Ferocious ha anche Impaling IV (sotto il V: cade),
    // Sharpness V (in conflitto con Impaling: cade senza l'advancement) e Unbreaking III (non accettato: cade).
    private static final Map<ResourceKey<Enchantment>, Integer> FEROCIOUS_INPUT = Map.of(Enchantments.IMPALING, 4,
            Enchantments.SHARPNESS, 5, Enchantments.LOOTING, 3, Enchantments.RIPTIDE, 2, Enchantments.UNBREAKING, 3);
    private static final Map<ResourceKey<Enchantment>, Integer> FRACTAL_INPUT = Map.of(Enchantments.LOYALTY, 3,
            Enchantments.EFFICIENCY, 5, Enchantments.LOOTING, 5, Enchantments.SHARPNESS, 4, Enchantments.IMPALING, 4);
    // Con Apothic Enchanting (ATM10) Sharpness NON è in conflitto con Impaling: Apothic toglie minecraft:sharpness da
    // #minecraft:exclusive_set/damage. Allora Sharpness è un incantesimo normale dei set (V nel Ferocious, IV nel
    // Fractal), attivo anche senza l'advancement. sharpnessWithImpaling lo legge dal registro e controlla che
    // corrisponda alla presenza di Apothic (un cambio inatteso dei dati fa fallire i test).
    private static final Map<ResourceKey<Enchantment>, Integer> FEROCIOUS_SET_BASE = Map.of(Enchantments.LOOTING, 3, Enchantments.RIPTIDE, 2);
    private static final Map<ResourceKey<Enchantment>, Integer> FRACTAL_SET_BASE = Map.of(Enchantments.LOYALTY, 3, Enchantments.EFFICIENCY, 5, Enchantments.LOOTING, 5);

    /** Sharpness e Impaling compatibili (vero solo con Apothic Enchanting caricato; controllato). */
    static boolean sharpnessWithImpaling(GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        boolean compatible = Enchantment.areCompatible(registry.getOrThrow(Enchantments.SHARPNESS), registry.getOrThrow(Enchantments.IMPALING));
        boolean apothic = net.neoforged.fml.ModList.get().isLoaded("apothic_enchanting");
        helper.assertTrue(compatible == apothic, "Sharpness/Impaling " + (compatible ? "compatibili" : "in conflitto")
                + (apothic ? " con" : " senza") + " Apothic Enchanting: atteso il contrario");
        return compatible;
    }

    /** Set Ferocious atteso: senza conflitto (Apothic) anche Sharpness V. */
    private static Map<ResourceKey<Enchantment>, Integer> ferociousSet(GameTestHelper helper) {
        return sharpnessWithImpaling(helper) ? with(FEROCIOUS_SET_BASE, Map.of(Enchantments.SHARPNESS, 5)) : FEROCIOUS_SET_BASE;
    }

    /** Set Fractal atteso: senza conflitto (Apothic) anche Sharpness IV. */
    private static Map<ResourceKey<Enchantment>, Integer> fractalSet(GameTestHelper helper) {
        return sharpnessWithImpaling(helper) ? with(FRACTAL_SET_BASE, Map.of(Enchantments.SHARPNESS, 4)) : FRACTAL_SET_BASE;
    }

    /** Unione di due insiemi di incantesimi: per ognuno il livello più alto (come copy_components). */
    private static Map<ResourceKey<Enchantment>, Integer> maxLevels(Map<ResourceKey<Enchantment>, Integer> a, Map<ResourceKey<Enchantment>, Integer> b) {
        Map<ResourceKey<Enchantment>, Integer> all = new java.util.HashMap<>(a);
        b.forEach((k, v) -> all.merge(k, v, Math::max));
        return all;
    }

    /** Base + set: gli incantesimi attesi in una modalità. */
    private static Map<ResourceKey<Enchantment>, Integer> with(Map<ResourceKey<Enchantment>, Integer> base, Map<ResourceKey<Enchantment>, Integer> set) {
        Map<ResourceKey<Enchantment>, Integer> all = new java.util.HashMap<>(base);
        all.putAll(set);
        return all;
    }

    /**
     * Fusione vera alla Fusion Shrine (FusionShrineRecipe.craft con un block entity piazzato nel test): il mixin legge
     * il Fractal dall'inventario prima che venga consumato. Il bident esce in modalità Ferocious con il set Ferocious
     * attivo e il set Fractal memorizzato; senza proprietario online niente conflitti (Sharpness cade da entrambi).
     * Nell'inventario ci sono solo i due bident: craft non ricontrolla gli altri ingredienti (lo fa matches, prima).
     */
    @GameTest(template = EMPTY)
    public static void bident_fusion_shrine_craft(GameTestHelper helper) {
        var level = helper.getLevel();
        var recipe = (FusionShrineRecipe) level.getServer().getRecipeManager()
                .byKey(SpectralAscension.id("fusion_shrine/t6/trichromatic_bident")).orElseThrow().value();
        BlockPos pos = new BlockPos(0, 1, 0);
        helper.setBlock(pos, SpectrumBlocks.FUSION_SHRINE_BASALT.get());
        FusionShrineBlockEntity shrine = helper.getBlockEntity(pos);
        ItemStack ferocious = new ItemStack(SpectrumItems.FEROCIOUS_GLASS_CREST_BIDENT.get());
        ferocious.set(DataComponents.ENCHANTMENTS, enchantments(helper, FEROCIOUS_INPUT));
        ItemStack fractal = new ItemStack(SpectrumItems.FRACTAL_GLASS_CREST_BIDENT.get());
        fractal.set(DataComponents.ENCHANTMENTS, enchantments(helper, FRACTAL_INPUT));
        shrine.setItem(0, ferocious);
        shrine.setItem(1, fractal);

        recipe.craft(level, shrine);

        BlockPos abs = helper.absolutePos(pos);
        AABB area = new AABB(abs).inflate(4);
        List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, area);
        ItemStack result = drops.stream().map(ItemEntity::getItem).filter(s -> s.is(SAItems.TRICHROMATIC_BIDENT.get())).findFirst().orElse(ItemStack.EMPTY);
        boolean consumed = shrine.getItem(0).isEmpty() && shrine.getItem(1).isEmpty();
        drops.forEach(Entity::discard);
        level.getEntitiesOfClass(ExperienceOrb.class, area).forEach(Entity::discard);
        helper.setBlock(pos, Blocks.AIR);

        helper.assertTrue(!result.isEmpty(), "la Fusion Shrine non ha prodotto il bident");
        helper.assertTrue(consumed, "i due bident non sono stati consumati");
        helper.assertTrue(SABident.getMode(result) == BidentMode.FEROCIOUS, "dopo la fusione la modalità è " + SABident.getMode(result));
        Map<ResourceKey<Enchantment>, Integer> base = SAItems.defaultEnchantments(SAItems.TRICHROMATIC_BIDENT.get());
        String p = enchantmentProblems(helper, result, with(base, ferociousSet(helper)));
        helper.assertTrue(p.isEmpty(), "incantesimi attivi (Ferocious): " + p);
        BidentEnchantments stored = result.get(SAComponents.BIDENT_ENCHANTMENTS.get());
        helper.assertTrue(stored != null, "manca il componente bident_enchantments");
        p = enchantmentProblems(helper, stored.fractal(), fractalSet(helper));
        helper.assertTrue(p.isEmpty(), "set Fractal memorizzato: " + p);
        p = enchantmentProblems(helper, stored.ferocious(), ferociousSet(helper));
        helper.assertTrue(p.isEmpty(), "set Ferocious memorizzato: " + p);
        helper.assertTrue(stored.inactive(BidentMode.FEROCIOUS).isEmpty(), "Ferocious non attivi: " + stored.inactive(BidentMode.FEROCIOUS));
        helper.assertTrue(result.has(DataComponents.UNBREAKABLE), "dopo la fusione non è più indistruttibile");
        SpectralAscension.LOGGER.info("[GameTest] Fusion Shrine del bident: Ferocious attivo (Impaling VIII, Looting III, Riptide II), Fractal memorizzato (Loyalty III, Efficiency V, Looting V)");
        helper.succeed();
    }

    /**
     * Cicli di modalità scelte dal menu (come in gioco): spento → Ferocious → Fractal → spento → ... ogni
     * modalità restituisce esattamente gli stessi incantesimi, anche dopo un salvataggio. Poi: un incantesimo aggiunto
     * in Ferocious resta solo in Ferocious, uno aggiunto da spento diventa base (tutte le modalità), il potenziamento
     * in Ferocious di un incantesimo della base (Impaling VIII → XI) va nella base (tutte le modalità), un potenziamento
     * dello stesso incantesimo che è nella base e nel set (Looting) resta coerente, e togliere tutto con la mola in
     * Fractal non fa ricomparire la base né il set Fractal (niente duplicazione).
     */
    @GameTest(template = EMPTY)
    public static void bident_enchantment_cycle(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        var access = helper.getLevel().registryAccess();
        var registry = access.lookupOrThrow(Registries.ENCHANTMENT);
        SABident item = SAItems.TRICHROMATIC_BIDENT.get();
        ItemStack bident = BidentEnchantments.applyFusion(new ItemStack(item),
                enchantments(helper, FEROCIOUS_INPUT), enchantments(helper, FRACTAL_INPUT), access, false);
        player.setItemInHand(InteractionHand.MAIN_HAND, bident);

        Map<ResourceKey<Enchantment>, Integer> base = SAItems.defaultEnchantments(item);
        Map<BidentMode, Map<ResourceKey<Enchantment>, Integer>> expected = new java.util.EnumMap<>(Map.of(
                BidentMode.OFF, base, BidentMode.FEROCIOUS, with(base, ferociousSet(helper)), BidentMode.FRACTAL, with(base, fractalSet(helper))));
        // Shift + tasto destro e scelta della modalità successiva nel menu; controlla modalità e incantesimi.
        java.util.function.Consumer<String> step = when -> {
            chooseFromMenu(helper, player, nextInCycle(SABident.getMode(player.getMainHandItem())));
            ItemStack held = player.getMainHandItem();
            BidentMode mode = SABident.getMode(held);
            String p = enchantmentProblems(helper, held, expected.get(mode));
            helper.assertTrue(p.isEmpty(), when + ", modalità " + mode + ": " + p);
        };
        // Due giri completi, poi un salvataggio, poi un altro giro.
        for (int i = 0; i < 6; i++) step.accept("giro " + (i / 3 + 1));
        player.setItemInHand(InteractionHand.MAIN_HAND, reload(helper, player.getMainHandItem()));
        for (int i = 0; i < 3; i++) step.accept("dopo il salvataggio");
        helper.assertTrue(SABident.getMode(player.getMainHandItem()) == BidentMode.FEROCIOUS, "il ciclo non è tornato a Ferocious");

        // Incantato in Ferocious (es. incudine): Fire Aspect II resta nel set Ferocious.
        ItemEnchantments.Mutable added = new ItemEnchantments.Mutable(player.getMainHandItem().getTagEnchantments());
        added.set(registry.getOrThrow(Enchantments.FIRE_ASPECT), 2);
        player.getMainHandItem().set(DataComponents.ENCHANTMENTS, added.toImmutable());
        expected.put(BidentMode.FEROCIOUS, with(expected.get(BidentMode.FEROCIOUS), Map.of(Enchantments.FIRE_ASPECT, 2)));
        for (int i = 0; i < 3; i++) step.accept("con Fire Aspect in Ferocious");

        // Incantato da spento: Knockback II diventa base, quindi c'è in tutte le modalità.
        step.accept("verso Fractal");
        step.accept("verso spento");
        added = new ItemEnchantments.Mutable(player.getMainHandItem().getTagEnchantments());
        added.set(registry.getOrThrow(Enchantments.KNOCKBACK), 2);
        player.getMainHandItem().set(DataComponents.ENCHANTMENTS, added.toImmutable());
        for (BidentMode mode : BidentMode.values()) expected.put(mode, with(expected.get(mode), Map.of(Enchantments.KNOCKBACK, 2)));
        for (int i = 0; i < 3; i++) step.accept("con Knockback da spento");

        // Potenziato in Ferocious un incantesimo della base: Impaling VIII → XI (incudine) va nella base, quindi XI ovunque.
        step.accept("verso Ferocious");
        added = new ItemEnchantments.Mutable(player.getMainHandItem().getTagEnchantments());
        added.set(registry.getOrThrow(Enchantments.IMPALING), 11);
        player.getMainHandItem().set(DataComponents.ENCHANTMENTS, added.toImmutable());
        for (BidentMode mode : BidentMode.values()) expected.put(mode, with(expected.get(mode), Map.of(Enchantments.IMPALING, 11)));
        for (int i = 0; i < 3; i++) step.accept("con Impaling XI potenziato in Ferocious");
        BidentEnchantments data = player.getMainHandItem().get(SAComponents.BIDENT_ENCHANTMENTS.get());
        helper.assertTrue(data.ferocious().getLevel(registry.getOrThrow(Enchantments.IMPALING)) == 0, "Impaling XI è finito nel set Ferocious");

        // Stesso incantesimo nella base e nel set: da spento Looting II diventa base; in Fractal vale il V del set.
        step.accept("verso Fractal");
        step.accept("verso spento");
        added = new ItemEnchantments.Mutable(player.getMainHandItem().getTagEnchantments());
        added.set(registry.getOrThrow(Enchantments.LOOTING), 2);
        player.getMainHandItem().set(DataComponents.ENCHANTMENTS, added.toImmutable());
        expected.put(BidentMode.OFF, with(expected.get(BidentMode.OFF), Map.of(Enchantments.LOOTING, 2)));
        for (int i = 0; i < 3; i++) step.accept("con Looting II nella base");
        // In Ferocious (set Looting III, attivo) lo si potenzia a VI: Looting è nella base, quindi VI ovunque.
        step.accept("verso Ferocious");
        added = new ItemEnchantments.Mutable(player.getMainHandItem().getTagEnchantments());
        added.set(registry.getOrThrow(Enchantments.LOOTING), 6);
        player.getMainHandItem().set(DataComponents.ENCHANTMENTS, added.toImmutable());
        for (BidentMode mode : BidentMode.values()) expected.put(mode, with(expected.get(mode), Map.of(Enchantments.LOOTING, 6)));
        for (int i = 0; i < 3; i++) step.accept("con Looting VI potenziato in Ferocious");

        // Mola in Fractal: spariscono la base e gli incantesimi del set Fractal che erano sull'oggetto; restano il set
        // Ferocious e il Looting V del Fractal (sotto il VI della base, quindi mai sull'oggetto).
        step.accept("verso Fractal");
        player.getMainHandItem().set(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        expected.put(BidentMode.OFF, Map.of());
        expected.put(BidentMode.FRACTAL, Map.of(Enchantments.LOOTING, 5));
        expected.put(BidentMode.FEROCIOUS, with(ferociousSet(helper), Map.of(Enchantments.FIRE_ASPECT, 2)));
        // Il set Ferocious ha ancora il suo Looting III (era sotto la base, mai sull'oggetto): niente perdite.
        for (int i = 0; i < 3; i++) step.accept("dopo la mola in Fractal");
        SpectralAscension.LOGGER.info("[GameTest] incantesimi del bident: 3 giri di modalità (anche dopo il salvataggio) senza perdite né duplicati; incudine in modalità, da spento, Impaling XI e Looting nella base, mola");
        helper.succeed();
    }

    /**
     * Regola di Spectrum per i conflitti (Sharpness e Impaling): senza l'advancement APPLY_CONFLICTING_ENCHANTMENTS
     * la fusione scarta Sharpness e il cambio di modalità lo lascia memorizzato ma non attivo; con l'advancement
     * (dato al giocatore finto) si attiva insieme a Impaling VIII.
     */
    @GameTest(template = EMPTY)
    public static void bident_conflict_rule(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        var registry = access.lookupOrThrow(Registries.ENCHANTMENT);
        SABident item = SAItems.TRICHROMATIC_BIDENT.get();
        Map<ResourceKey<Enchantment>, Integer> base = SAItems.defaultEnchantments(item);

        // Fusione: con l'advancement Sharpness resta in entrambi i set, senza cade (come copy_components).
        ItemStack allowed = BidentEnchantments.applyFusion(new ItemStack(item),
                enchantments(helper, FEROCIOUS_INPUT), enchantments(helper, FRACTAL_INPUT), access, true);
        String p = enchantmentProblems(helper, allowed, with(with(base, ferociousSet(helper)), Map.of(Enchantments.SHARPNESS, 5)));
        helper.assertTrue(p.isEmpty(), "fusione con l'advancement, Ferocious: " + p);
        p = enchantmentProblems(helper, allowed.get(SAComponents.BIDENT_ENCHANTMENTS.get()).fractal(), with(fractalSet(helper), Map.of(Enchantments.SHARPNESS, 4)));
        helper.assertTrue(p.isEmpty(), "fusione con l'advancement, Fractal: " + p);
        ItemStack denied = BidentEnchantments.applyFusion(new ItemStack(item),
                enchantments(helper, FEROCIOUS_INPUT), enchantments(helper, FRACTAL_INPUT), access, false);
        p = enchantmentProblems(helper, denied, with(base, ferociousSet(helper)));
        helper.assertTrue(p.isEmpty(), "fusione senza l'advancement: " + p);

        // Cambio di modalità con un giocatore senza l'advancement: Sharpness IV resta in sospeso nel set Fractal.
        ServerPlayer player = mockPlayer(helper);
        helper.assertTrue(!SABident.canApplyConflicting(player), "il giocatore finto ha già l'advancement");
        ItemStack bident = allowed.copy();
        SABident.applyMode(bident, player, BidentMode.FRACTAL);
        p = enchantmentProblems(helper, bident, with(base, fractalSet(helper)));
        helper.assertTrue(p.isEmpty(), "Fractal senza l'advancement: " + p);
        // Con Apothic niente conflitto: Sharpness IV è già attivo, nessun incantesimo in sospeso.
        p = enchantmentProblems(helper, bident.get(SAComponents.BIDENT_ENCHANTMENTS.get()).inactive(BidentMode.FRACTAL),
                sharpnessWithImpaling(helper) ? Map.of() : Map.of(Enchantments.SHARPNESS, 4));
        helper.assertTrue(p.isEmpty(), "Sharpness non risulta memorizzato ma non attivo: " + p);
        SABident.applyMode(bident, player, BidentMode.OFF);
        SABident.applyMode(bident, player, BidentMode.FEROCIOUS); // Sharpness V ora è memorizzato ma non attivo
        p = enchantmentProblems(helper, bident, with(base, ferociousSet(helper)));
        helper.assertTrue(p.isEmpty(), "Ferocious senza l'advancement: " + p);

        // Con l'advancement: al prossimo ingresso nelle modalità Sharpness si attiva.
        var advancement = helper.getLevel().getServer().getAdvancements().get(SpectrumAdvancements.APPLY_CONFLICTING_ENCHANTMENTS);
        helper.assertTrue(advancement != null, "advancement di Spectrum mancante");
        for (String criterion : advancement.value().criteria().keySet()) player.getAdvancements().award(advancement, criterion);
        helper.assertTrue(SABident.canApplyConflicting(player), "l'advancement non risulta dato");
        SABident.applyMode(bident, player, BidentMode.FRACTAL);
        p = enchantmentProblems(helper, bident, with(with(base, fractalSet(helper)), Map.of(Enchantments.SHARPNESS, 4)));
        helper.assertTrue(p.isEmpty(), "Fractal con l'advancement: " + p);
        SABident.applyMode(bident, player, BidentMode.OFF);
        p = enchantmentProblems(helper, bident, base);
        helper.assertTrue(p.isEmpty(), "spento: " + p);
        SABident.applyMode(bident, player, BidentMode.FEROCIOUS);
        p = enchantmentProblems(helper, bident, with(with(base, ferociousSet(helper)), Map.of(Enchantments.SHARPNESS, 5)));
        helper.assertTrue(p.isEmpty(), "Ferocious con l'advancement: " + p);
        SpectralAscension.LOGGER.info("[GameTest] regola dei conflitti del bident: Sharpness con Impaling VIII solo con l'advancement di Spectrum (fusione e cambio modalità)");
        helper.succeed();
    }

    // ---------- T7 Umbral e T8 Argent ----------

    /**
     * Statistiche T7 rispetto al T6 (valori PROVVISORI in SAItems e SATier): ogni arma colpisce più forte, ogni attrezzo
     * scava più in fretta, tutto è più incantabile; balestra più rapida e precisa, spadone con più portata, bident con
     * più perforazione; armatura 41 punti in tutto.
     */
    @GameTest(template = EMPTY)
    public static void t7_stats(GameTestHelper helper) {
        checkTierStats(helper, "T7", SATier.T6, SATier.T7, List.of(
                List.of(SAItems.TRICHROMATIC_WORKSTAFF, SAItems.UMBRAL_WORKSTAFF),
                List.of(SAItems.TRICHROMATIC_AXE, SAItems.UMBRAL_AXE),
                List.of(SAItems.TRICHROMATIC_SHOVEL, SAItems.UMBRAL_SHOVEL),
                List.of(SAItems.TRICHROMATIC_HOE, SAItems.UMBRAL_HOE),
                List.of(SAItems.TRICHROMATIC_SWORD, SAItems.UMBRAL_SWORD),
                List.of(SAItems.TRICHROMATIC_ULTRA_GREATSWORD, SAItems.UMBRAL_ULTRA_GREATSWORD),
                List.of(SAItems.TRICHROMATIC_BIDENT, SAItems.UMBRAL_BIDENT),
                List.of(SAItems.TRICHROMATIC_MACE, SAItems.UMBRAL_MACE)),
                SAItems.TRICHROMATIC_CROSSBOW.get(), SAItems.UMBRAL_CROSSBOW.get(),
                19, 24, 41, "c:incorrect_for_vibranium_tool", "allthemodium:vibranium_ore");
    }

    /**
     * Statistiche T8 rispetto al T7, come t7_stats: danno +25% (spada 24 → 30), scavo, incantabilità, balestra, portata,
     * armatura 46 punti in tutto (8/18/12/8, ricerca/README.md); scava il minerale di unobtainium.
     */
    @GameTest(template = EMPTY)
    public static void t8_stats(GameTestHelper helper) {
        checkTierStats(helper, "T8", SATier.T7, SATier.T8, List.of(
                List.of(SAItems.UMBRAL_WORKSTAFF, SAItems.ARGENT_WORKSTAFF),
                List.of(SAItems.UMBRAL_AXE, SAItems.ARGENT_AXE),
                List.of(SAItems.UMBRAL_SHOVEL, SAItems.ARGENT_SHOVEL),
                List.of(SAItems.UMBRAL_HOE, SAItems.ARGENT_HOE),
                List.of(SAItems.UMBRAL_SWORD, SAItems.ARGENT_SWORD),
                List.of(SAItems.UMBRAL_ULTRA_GREATSWORD, SAItems.ARGENT_ULTRA_GREATSWORD),
                List.of(SAItems.UMBRAL_BIDENT, SAItems.ARGENT_BIDENT),
                List.of(SAItems.UMBRAL_MACE, SAItems.ARGENT_MACE)),
                SAItems.UMBRAL_CROSSBOW.get(), SAItems.ARGENT_CROSSBOW.get(),
                24, 30, 46, "c:incorrect_for_unobtainium_tool", "allthemodium:unobtainium_ore");
    }

    /**
     * Statistiche Alloy (T9) rispetto al T8, come t8_stats: danno +25% (spada 30 → 37), scavo, incantabilità, balestra,
     * portata, armatura 50 punti in tutto (9/19/13/9, ricerca/README.md); tag di scavo degli attrezzi Alloy di ATM.
     */
    @GameTest(template = EMPTY)
    public static void t9_stats(GameTestHelper helper) {
        checkTierStats(helper, "Alloy", SATier.T8, SATier.ALLOY, List.of(
                List.of(SAItems.ARGENT_WORKSTAFF, SAItems.PRISMATIC_WORKSTAFF),
                List.of(SAItems.ARGENT_AXE, SAItems.PRISMATIC_AXE),
                List.of(SAItems.ARGENT_SHOVEL, SAItems.PRISMATIC_SHOVEL),
                List.of(SAItems.ARGENT_HOE, SAItems.PRISMATIC_HOE),
                List.of(SAItems.ARGENT_SWORD, SAItems.PRISMATIC_SWORD),
                List.of(SAItems.ARGENT_ULTRA_GREATSWORD, SAItems.PRISMATIC_ULTRA_GREATSWORD),
                List.of(SAItems.ARGENT_BIDENT, SAItems.PRISMATIC_BIDENT),
                List.of(SAItems.ARGENT_MACE, SAItems.PRISMATIC_MACE)),
                SAItems.ARGENT_CROSSBOW.get(), SAItems.PRISMATIC_CROSSBOW.get(),
                30, 37, 50, "c:incorrect_for_allthemodiumalloy_tool", "allthemodium:unobtainium_ore");
    }

    /**
     * Confronto fra un tier e il precedente: coppie (pezzo precedente, pezzo nuovo), danno totale della spada atteso
     * (precedente e nuovo), punti armatura del set nuovo, tag di scavo del tier nuovo e un minerale che deve scavare.
     */
    private static void checkTierStats(GameTestHelper helper, String name, SATier prevTier, SATier tier,
                                       List<List<DeferredItem<? extends Item>>> pairs,
                                       SATools.Crossbow prevCrossbow, SATools.Crossbow crossbow,
                                       double prevSword, double sword, double armorTotal, String incorrectTag, String ore) {
        List<String> report = new ArrayList<>();
        for (var pair : pairs) {
            ItemStack a = new ItemStack(pair.get(0).get());
            ItemStack b = new ItemStack(pair.get(1).get());
            double da = attribute(a, EquipmentSlot.MAINHAND, Attributes.ATTACK_DAMAGE);
            double db = attribute(b, EquipmentSlot.MAINHAND, Attributes.ATTACK_DAMAGE);
            if (!(db > da)) report.add(pair.get(1).getId().getPath() + ": danno " + db + " non maggiore del precedente " + da);
            double sa = attribute(a, EquipmentSlot.MAINHAND, Attributes.ATTACK_SPEED);
            double sb = attribute(b, EquipmentSlot.MAINHAND, Attributes.ATTACK_SPEED);
            if (sb < sa) report.add(pair.get(1).getId().getPath() + ": velocità d'attacco " + sb + " sotto il precedente " + sa);
            if (b.getEnchantmentValue() <= a.getEnchantmentValue()) report.add(pair.get(1).getId().getPath() + ": incantabilità non maggiore");
            // Danni +25% per tier (regola in SATier), misurati sul danno totale (+1 della mano).
            double ta = da + 1, tb = db + 1;
            // Il bident NON segue più il +25% (danno 16 / 19 / 22 / 25 a scelta di design): solo la crescita sopra.
            if (!(pair.get(1).get() instanceof SABident) && Math.abs(tb / ta - SATier.DAMAGE_FACTOR) > 0.1)
                report.add(pair.get(1).getId().getPath() + ": danno " + tb + " / precedente " + ta + " = " + (tb / ta) + ", atteso circa 1,25");
        }
        // Velocità di scavo: workstaff (piccone) sulla pietra, pala sulla terra.
        var stone = Blocks.STONE.defaultBlockState();
        var dirt = Blocks.DIRT.defaultBlockState();
        if (!(new ItemStack(pairs.get(0).get(1).get()).getDestroySpeed(stone) > new ItemStack(pairs.get(0).get(0).get()).getDestroySpeed(stone)))
            report.add("workstaff " + name + " non scava la pietra più in fretta del tier precedente");
        if (!(new ItemStack(pairs.get(2).get(1).get()).getDestroySpeed(dirt) > new ItemStack(pairs.get(2).get(0).get()).getDestroySpeed(dirt)))
            report.add("pala " + name + " non scava la terra più in fretta del tier precedente");
        // Scavo: ESATTAMENTE il tag di ATM del metallo del tier (decisione dell'utente).
        if (!tier.getIncorrectBlocksForDrops().location().toString().equals(incorrectTag))
            report.add("tag di scavo " + name + ": " + tier.getIncorrectBlocksForDrops().location() + " invece di " + incorrectTag);
        var oreState = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(ore)).defaultBlockState();
        if (!new ItemStack(pairs.get(0).get(1).get()).isCorrectToolForDrops(oreState))
            report.add("la workstaff " + name + " non raccoglie " + ore);
        double swA = attribute(new ItemStack(pairs.get(4).get(0).get()), EquipmentSlot.MAINHAND, Attributes.ATTACK_DAMAGE) + 1;
        double swB = attribute(new ItemStack(pairs.get(4).get(1).get()), EquipmentSlot.MAINHAND, Attributes.ATTACK_DAMAGE) + 1;
        if (swA != prevSword || swB != sword) report.add("danno spada: " + swA + "/" + swB + " invece di " + prevSword + "/" + sword);
        // Balestra: carica più rapida del tier precedente.
        ItemStack cs = new ItemStack(crossbow);
        if (!(crossbow.getPullTimeModifier(cs, null) < prevCrossbow.getPullTimeModifier(new ItemStack(prevCrossbow), null)))
            report.add("balestra " + name + ": tempo di carica " + crossbow.getPullTimeModifier(cs, null) + " non minore del precedente");
        // Portata dello spadone (attributo entity_interaction_range).
        double ra = attribute(new ItemStack(pairs.get(5).get(0).get()), EquipmentSlot.MAINHAND, Attributes.ENTITY_INTERACTION_RANGE);
        double rb = attribute(new ItemStack(pairs.get(5).get(1).get()), EquipmentSlot.MAINHAND, Attributes.ENTITY_INTERACTION_RANGE);
        if (!(rb > ra)) report.add("ultra spadone " + name + ": portata " + rb + " non maggiore del precedente " + ra);
        // Armatura: punti in tutto, e più del tier precedente.
        double total = 0, prevTotal = 0;
        for (var holder : SAItems.armor(tier)) total += attribute(new ItemStack(holder.get()), holder.get().getEquipmentSlot(), Attributes.ARMOR);
        for (var holder : SAItems.armor(prevTier)) prevTotal += attribute(new ItemStack(holder.get()), holder.get().getEquipmentSlot(), Attributes.ARMOR);
        if (total != armorTotal) report.add("armatura " + name + ": " + total + " punti invece di " + armorTotal);
        if (!(total > prevTotal)) report.add("armatura " + name + ": " + total + " punti, non più del precedente " + prevTotal);
        if (!report.isEmpty()) {
            report.forEach(r -> SpectralAscension.LOGGER.error("[GameTest] {}", r));
            helper.fail(report.size() + " problemi, il primo: " + report.getFirst());
        }
        SpectralAscension.LOGGER.info("[GameTest] statistiche {}: danno, scavo, incantabilità, balestra, portata e armatura sopra il tier precedente", name);
        helper.succeed();
    }

    /**
     * Upgrade della workstaff T6 → T7 alla Fusion Shrine (copy_components): incantesimi del T6 conservati (alzati a
     * quelli del T7 dove più bassi, più un incantesimo aggiunto dal giocatore), area di scavo conservata, indistruttibile.
     */
    @GameTest(template = EMPTY)
    public static void workstaff_t7_upgrade(GameTestHelper helper) throws ReflectiveOperationException {
        checkWorkstaffUpgrade(helper, "fusion_shrine/t7/umbral_workstaff", SAItems.TRICHROMATIC_WORKSTAFF.get(),
                SAItems.UMBRAL_WORKSTAFF.get(), "T6 → T7");
    }

    /** Come workstaff_t7_upgrade, per T7 → T8. */
    @GameTest(template = EMPTY)
    public static void workstaff_t8_upgrade(GameTestHelper helper) throws ReflectiveOperationException {
        checkWorkstaffUpgrade(helper, "fusion_shrine/t8/argent_workstaff", SAItems.UMBRAL_WORKSTAFF.get(),
                SAItems.ARGENT_WORKSTAFF.get(), "T7 → T8");
    }

    /** Come workstaff_t7_upgrade, per T8 → Alloy (T9). */
    @GameTest(template = EMPTY)
    public static void workstaff_t9_upgrade(GameTestHelper helper) throws ReflectiveOperationException {
        checkWorkstaffUpgrade(helper, "fusion_shrine/t9/prismatic_workstaff", SAItems.ARGENT_WORKSTAFF.get(),
                SAItems.PRISMATIC_WORKSTAFF.get(), "T8 → Alloy");
    }

    private static void checkWorkstaffUpgrade(GameTestHelper helper, String recipeId, Item from, Item to, String name)
            throws ReflectiveOperationException {
        var access = helper.getLevel().registryAccess();
        var recipe = (FusionShrineRecipe) helper.getLevel().getServer().getRecipeManager()
                .byKey(SpectralAscension.id(recipeId)).orElseThrow().value();
        ItemStack input = new ItemStack(from);
        Map<ResourceKey<Enchantment>, Integer> levels = new java.util.HashMap<>(SAItems.defaultEnchantments(from));
        levels.put(Enchantments.SMITE, 3);
        input.set(DataComponents.ENCHANTMENTS, enchantments(helper, levels));
        input.set(SpectrumDataComponentTypes.AOE.get(), 2);
        ItemStack result = fusionResult(recipe.getResultItem(access), input);
        helper.assertTrue(result.is(to), "la fusione produce " + result);
        // copy_components: per ogni incantesimo vince il livello più alto fra input e ricetta (dal 2026-11-18 il T6 ha
        // incantesimi più bassi del T7, che quindi li alza; T7 → T8 restano uguali).
        Map<ResourceKey<Enchantment>, Integer> expected = maxLevels(levels, SAItems.defaultEnchantments(to));
        String p = enchantmentProblems(helper, result, expected);
        helper.assertTrue(p.isEmpty(), "incantesimi dopo l'upgrade: " + p);
        helper.assertTrue(result.getOrDefault(SpectrumDataComponentTypes.AOE.get(), 0) == 2, "area di scavo 5×5 non conservata");
        helper.assertTrue(result.has(DataComponents.UNBREAKABLE), "dopo la fusione non è più indistruttibile");
        SpectralAscension.LOGGER.info("[GameTest] upgrade workstaff {}: incantesimi (Smite III compreso) e area 5×5 conservati", name);
        helper.succeed();
    }

    /**
     * Upgrade del bident T6 → T7 con la Fusion Shrine vera (FusionShrineRecipe.craft): il bident T6 è in modalità
     * Fractal con i set di entrambe le modalità; il T7 esce nella stessa modalità, con gli stessi incantesimi attivi e
     * gli stessi set memorizzati. FusionShrineRecipeMixin NON deve intervenire (il primo ingrediente è già un nostro
     * bident): se lo facesse, il set Fractal verrebbe sostituito da quello letto dal blocco di vibranium (vuoto).
     */
    @GameTest(template = EMPTY)
    public static void bident_t7_upgrade(GameTestHelper helper) {
        checkBidentUpgrade(helper, "fusion_shrine/t7/umbral_bident", SAItems.TRICHROMATIC_BIDENT.get(),
                SAItems.UMBRAL_BIDENT.get(), "allthemodium:vibranium_ingot", "T6 → T7");
    }

    /** Come bident_t7_upgrade, per T7 → T8 (secondo ingrediente: lingotti di unobtainium). */
    @GameTest(template = EMPTY)
    public static void bident_t8_upgrade(GameTestHelper helper) {
        checkBidentUpgrade(helper, "fusion_shrine/t8/argent_bident", SAItems.UMBRAL_BIDENT.get(),
                SAItems.ARGENT_BIDENT.get(), "allthemodium:unobtainium_ingot", "T7 → T8");
    }

    /** Come bident_t7_upgrade, per T8 → Alloy (secondo ingrediente: un lingotto di lega vibranio-allthemodium). */
    @GameTest(template = EMPTY)
    public static void bident_t9_upgrade(GameTestHelper helper) {
        checkBidentUpgrade(helper, "fusion_shrine/t9/prismatic_bident", SAItems.ARGENT_BIDENT.get(),
                SAItems.PRISMATIC_BIDENT.get(), "allthemodium:vibranium_allthemodium_alloy_ingot", "T8 → Alloy");
    }

    private static void checkBidentUpgrade(GameTestHelper helper, String recipeId, SABident from, SABident to,
                                           String secondIngredient, String name) {
        ServerPlayer player = mockPlayer(helper);
        var level = helper.getLevel();
        var access = level.registryAccess();
        var recipe = (FusionShrineRecipe) level.getServer().getRecipeManager()
                .byKey(SpectralAscension.id(recipeId)).orElseThrow().value();
        helper.assertTrue(recipe.getIngredientStacks().size() > 1, "la ricetta " + recipeId + " ha un solo ingrediente");
        // Bident di partenza con i set delle due modalità, poi Fractal (la scelta del menu).
        ItemStack input = BidentEnchantments.applyFusion(new ItemStack(from),
                enchantments(helper, FEROCIOUS_INPUT), enchantments(helper, FRACTAL_INPUT), access, false);
        SABident.applyMode(input, player, BidentMode.FRACTAL);
        helper.assertTrue(SABident.getMode(input) == BidentMode.FRACTAL, "il bident di partenza non è in Fractal");
        ItemEnchantments activeBefore = input.getTagEnchantments();
        BidentEnchantments storedBefore = input.get(SAComponents.BIDENT_ENCHANTMENTS.get());

        BlockPos pos = new BlockPos(0, 1, 0);
        helper.setBlock(pos, SpectrumBlocks.FUSION_SHRINE_BASALT.get());
        FusionShrineBlockEntity shrine = helper.getBlockEntity(pos);
        shrine.setItem(0, input.copy());
        // Un pezzo del metallo del tier come secondo ingrediente: senza la correzione il mixin ne leggerebbe gli incantesimi.
        shrine.setItem(1, new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(secondIngredient)), 2));
        recipe.craft(level, shrine);

        BlockPos abs = helper.absolutePos(pos);
        AABB area = new AABB(abs).inflate(4);
        List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, area);
        ItemStack result = drops.stream().map(ItemEntity::getItem).filter(st -> st.is(to)).findFirst().orElse(ItemStack.EMPTY);
        drops.forEach(Entity::discard);
        level.getEntitiesOfClass(ExperienceOrb.class, area).forEach(Entity::discard);
        helper.setBlock(pos, Blocks.AIR);

        helper.assertTrue(!result.isEmpty(), "la Fusion Shrine non ha prodotto " + BuiltInRegistries.ITEM.getKey(to));
        helper.assertTrue(SABident.getMode(result) == BidentMode.FRACTAL, "dopo l'upgrade la modalità è " + SABident.getMode(result));
        // Attivi conservati, con gli incantesimi di serie del nuovo tier alzati al loro livello se più alti (T6 → T7:
        // Impaling VIII → X dal 2026-11-18; T7 → T8 uguali).
        Map<ResourceKey<Enchantment>, Integer> before = new java.util.HashMap<>();
        activeBefore.entrySet().forEach(e -> e.getKey().unwrapKey().ifPresent(k -> before.put(k, e.getIntValue())));
        String active = enchantmentProblems(helper, result, maxLevels(before, SAItems.defaultEnchantments(to)));
        helper.assertTrue(active.isEmpty(), "incantesimi attivi dopo l'upgrade: " + active);
        BidentEnchantments storedAfter = result.get(SAComponents.BIDENT_ENCHANTMENTS.get());
        helper.assertTrue(storedBefore != null && storedBefore.equals(storedAfter), "set memorizzati cambiati: " + storedBefore + " → " + storedAfter);
        helper.assertTrue(result.has(DataComponents.UNBREAKABLE), "dopo l'upgrade non è più indistruttibile");
        // Il menu continua a restituire i set: Fractal → spento → Ferocious (dal menu, con il bident in mano).
        player.setItemInHand(InteractionHand.MAIN_HAND, result);
        chooseFromMenu(helper, player, BidentMode.OFF);
        chooseFromMenu(helper, player, BidentMode.FEROCIOUS);
        Map<ResourceKey<Enchantment>, Integer> base = SAItems.defaultEnchantments(to);
        String p = enchantmentProblems(helper, result, with(base, ferociousSet(helper)));
        helper.assertTrue(p.isEmpty(), "in Ferocious dopo l'upgrade: " + p);
        SpectralAscension.LOGGER.info("[GameTest] upgrade bident {} alla Fusion Shrine: modalità Fractal, incantesimi attivi e set memorizzati conservati", name);
        helper.succeed();
    }

    /**
     * Esegue il controllo di coerenza di Spectrum sulla nostra mod. Il test passa sempre:
     * gli avvisi finiscono nel log con il prefisso [SANITY e li legge tools/check_log.py.
     */
    // ---------- Workstaff: area in sopravvivenza ----------

    /**
     * Scavo ad area della workstaff con un giocatore finto in SOPRAVVIVENZA (mockPlayer non è creativo), T6, T7 e T8,
     * 3×3 e 5×5. Come in gioco: il giocatore guarda il centro di un muro di pietra e lo rompe con
     * gameMode.destroyBlock, che lancia BreakEvent → SpectrumEventListeners.onBlockBreak → afterBreakingBlock.
     * <ul>
     * <li>senza White Ink (o senza l'Ink sbloccato) Spectrum scava solo il blocco colpito: era il «non funziona in
     * survival» (in creativo l'inchiostro è gratis, InkPowered.tryDrainEnergy);</li>
     * <li>con l'Ink sbloccato e una fiaschetta di White Ink sparisce tutta l'area e si pagano esattamente 3^raggio
     * Ink: prima della correzione in SATools.Workstaff.afterBreakingBlock se ne pagavano 3^raggio per OGNI blocco
     * dell'area (27 invece di 3, 225 invece di 9).</li>
     * </ul>
     */
    @GameTest(template = EMPTY)
    public static void workstaff_aoe_survival(GameTestHelper helper) {
        var level = helper.getLevel();
        var access = level.registryAccess();
        var inkUse = level.getServer().getAdvancements().get(SpectrumAdvancements.UNLOCK_INK_USE);
        helper.assertTrue(inkUse != null, "advancement mancante: " + SpectrumAdvancements.UNLOCK_INK_USE);
        List<String> report = new ArrayList<>();
        int row = 0;
        for (var item : List.of(SAItems.TRICHROMATIC_WORKSTAFF.get(), SAItems.UMBRAL_WORKSTAFF.get(), SAItems.ARGENT_WORKSTAFF.get())) {
            for (int range : new int[]{1, 2}) {
                for (boolean withInk : new boolean[]{false, true}) {
                    // un muro per caso, uno sopra l'altro (fuori dalla struttura 1×1×1, ripulito alla fine)
                    BlockPos feet = helper.absolutePos(new BlockPos(0, 4 + 7 * row++, 0));
                    BlockPos center = feet.offset(0, 1, 2);
                    int side = 2 * range + 1;
                    List<BlockPos> wall = new ArrayList<>();
                    for (int dx = -range; dx <= range; dx++)
                        for (int dy = 0; dy < side; dy++) wall.add(new BlockPos(center.getX() + dx, feet.getY() + dy, center.getZ()));
                    for (BlockPos pos : wall) level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());

                    ServerPlayer player = mockPlayer(helper);
                    helper.assertFalse(player.isCreative() || player.gameMode.isCreative(), "il giocatore finto deve essere in sopravvivenza");
                    aim(player, feet, 0.0F, 0.0F); // guarda verso +Z, il centro del muro
                    ItemStack flask = SpectrumItems.INK_FLASK.get().getDefaultInstance();
                    if (withInk) {
                        for (String c : inkUse.value().criteria().keySet()) player.getAdvancements().award(inkUse, c);
                        var storage = SpectrumItems.INK_FLASK.get().getEnergyStorage(flask);
                        storage.addEnergy(InkColors.WHITE, 1000);
                        SpectrumItems.INK_FLASK.get().setEnergyStorage(flask, storage);
                        player.getInventory().setItem(9, flask);
                    }
                    long inkBefore = SpectrumItems.INK_FLASK.get().getEnergyStorage(flask).getEnergy(InkColors.WHITE);
                    ItemStack staff = new ItemStack(item);
                    staff.set(DataComponents.ENCHANTMENTS, Preenchanted.buildDefaultEnchantments(access, item));
                    staff.set(SpectrumDataComponentTypes.AOE, range);
                    player.setItemInHand(InteractionHand.MAIN_HAND, staff);

                    boolean destroyed = player.gameMode.destroyBlock(center);
                    long left = wall.stream().filter(pos -> !level.getBlockState(pos).isAir()).count();
                    long paid = inkBefore - SpectrumItems.INK_FLASK.get().getEnergyStorage(flask).getEnergy(InkColors.WHITE);
                    String id = BuiltInRegistries.ITEM.getKey(item).getPath() + " " + side + "x" + side + (withInk ? " con Ink" : " senza Ink");
                    report.add(id + ": rimasti " + left + "/" + wall.size() + " blocchi, Ink pagato " + paid);
                    helper.assertTrue(destroyed && level.getBlockState(center).isAir(), id + ": il blocco colpito non è stato rotto");
                    if (withInk) {
                        helper.assertTrue(left == 0, id + ": l'area non è sparita, rimasti " + left + " blocchi su " + wall.size());
                        long cost = SATools.Workstaff.aoeInkCost(range);
                        helper.assertTrue(paid == cost, id + ": Ink pagato " + paid + ", atteso " + cost + " (3^raggio una volta per colpo)");
                    } else {
                        helper.assertTrue(left == wall.size() - 1, id + ": senza Ink l'area non dovrebbe scavare, rimasti " + left);
                    }
                    for (BlockPos pos : wall) level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                    level.getEntitiesOfClass(ItemEntity.class, new AABB(center).inflate(4)).forEach(Entity::discard);
                }
            }
        }
        SpectralAscension.LOGGER.info("[GameTest] area della workstaff in sopravvivenza: {}", String.join("; ", report));
        helper.succeed();
    }

    /**
     * Flag {@link SATier#freeAoE()}: solo l'Alloy ha l'area della workstaff gratis. Con il flag attivo (sostituito per
     * il test con SATools.Workstaff.freeAoEOverrideForTests su una workstaff T7) il 3×3 scava tutto senza consumare Ink,
     * anche senza l'Ink sbloccato e senza fiaschetta; con il flag spento consuma 3^raggio. Poi la workstaff Alloy vera,
     * senza override: 3×3 e 5×5 senza Ink.
     */
    @GameTest(template = EMPTY)
    public static void workstaff_free_aoe(GameTestHelper helper) {
        var level = helper.getLevel();
        var access = level.registryAccess();
        var inkUse = level.getServer().getAdvancements().get(SpectrumAdvancements.UNLOCK_INK_USE);
        helper.assertTrue(inkUse != null, "advancement mancante: " + SpectrumAdvancements.UNLOCK_INK_USE);
        for (SATier tier : SATier.values())
            helper.assertTrue(tier.freeAoE() == (tier == SATier.ALLOY), "freeAoE di " + tier + " dovrebbe essere " + (tier == SATier.ALLOY));
        var item = SAItems.UMBRAL_WORKSTAFF.get();
        List<String> report = new ArrayList<>();
        try {
            int row = 0;
            for (boolean free : new boolean[]{true, false}) {
                SATools.Workstaff.freeAoEOverrideForTests = free;
                int range = 1;
                BlockPos feet = helper.absolutePos(new BlockPos(0, 40 + 7 * row++, 0));
                BlockPos center = feet.offset(0, 1, 2);
                List<BlockPos> wall = new ArrayList<>();
                for (int dx = -range; dx <= range; dx++)
                    for (int dy = 0; dy < 3; dy++) wall.add(new BlockPos(center.getX() + dx, feet.getY() + dy, center.getZ()));
                for (BlockPos pos : wall) level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
                ServerPlayer player = mockPlayer(helper);
                helper.assertFalse(player.isCreative() || player.gameMode.isCreative(), "il giocatore finto deve essere in sopravvivenza");
                aim(player, feet, 0.0F, 0.0F);
                ItemStack flask = SpectrumItems.INK_FLASK.get().getDefaultInstance();
                if (!free) {
                    // flag spento: Ink sbloccato e fiaschetta piena, si paga
                    for (String c : inkUse.value().criteria().keySet()) player.getAdvancements().award(inkUse, c);
                    var storage = SpectrumItems.INK_FLASK.get().getEnergyStorage(flask);
                    storage.addEnergy(InkColors.WHITE, 1000);
                    SpectrumItems.INK_FLASK.get().setEnergyStorage(flask, storage);
                    player.getInventory().setItem(9, flask);
                }
                long before = SpectrumItems.INK_FLASK.get().getEnergyStorage(flask).getEnergy(InkColors.WHITE);
                ItemStack staff = new ItemStack(item);
                staff.set(DataComponents.ENCHANTMENTS, Preenchanted.buildDefaultEnchantments(access, item));
                staff.set(SpectrumDataComponentTypes.AOE, range);
                player.setItemInHand(InteractionHand.MAIN_HAND, staff);
                player.gameMode.destroyBlock(center);
                long left = wall.stream().filter(pos -> !level.getBlockState(pos).isAir()).count();
                long paid = before - SpectrumItems.INK_FLASK.get().getEnergyStorage(flask).getEnergy(InkColors.WHITE);
                report.add("flag " + (free ? "attivo" : "spento") + ": rimasti " + left + "/9, Ink pagato " + paid);
                if (free) {
                    helper.assertTrue(left == 0, "area gratis: rimasti " + left + " blocchi su 9");
                    helper.assertTrue(paid == 0, "area gratis: Ink pagato " + paid + " invece di 0");
                } else {
                    helper.assertTrue(left == 0, "area a pagamento: rimasti " + left + " blocchi su 9");
                    helper.assertTrue(paid == SATools.Workstaff.aoeInkCost(range), "area a pagamento: Ink pagato " + paid + " invece di " + SATools.Workstaff.aoeInkCost(range));
                }
                for (BlockPos pos : wall) level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                level.getEntitiesOfClass(ItemEntity.class, new AABB(center).inflate(4)).forEach(Entity::discard);
            }
        } finally {
            SATools.Workstaff.freeAoEOverrideForTests = null;
        }
        // La workstaff Alloy vera (senza override): 3×3 e 5×5 senza Ink sbloccato né fiaschetta, tutto scavato, 0 pagato.
        var prismatic = SAItems.PRISMATIC_WORKSTAFF.get();
        helper.assertTrue(prismatic.hasFreeAoE(), "la workstaff Alloy non ha l'area gratis");
        int row = 4;
        for (int range : new int[]{1, 2}) {
            int side = 2 * range + 1;
            BlockPos feet = helper.absolutePos(new BlockPos(0, 40 + 7 * row++, 0));
            BlockPos center = feet.offset(0, 1, 2);
            List<BlockPos> wall = new ArrayList<>();
            for (int dx = -range; dx <= range; dx++)
                for (int dy = 0; dy < side; dy++) wall.add(new BlockPos(center.getX() + dx, feet.getY() + dy, center.getZ()));
            for (BlockPos pos : wall) level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
            ServerPlayer player = mockPlayer(helper);
            aim(player, feet, 0.0F, 0.0F);
            ItemStack staff = new ItemStack(prismatic);
            staff.set(DataComponents.ENCHANTMENTS, Preenchanted.buildDefaultEnchantments(access, prismatic));
            staff.set(SpectrumDataComponentTypes.AOE, range);
            player.setItemInHand(InteractionHand.MAIN_HAND, staff);
            player.gameMode.destroyBlock(center);
            long left = wall.stream().filter(pos -> !level.getBlockState(pos).isAir()).count();
            report.add("prismatic_workstaff " + side + "x" + side + " senza Ink: rimasti " + left + "/" + wall.size());
            helper.assertTrue(left == 0, "workstaff Alloy " + side + "x" + side + " senza Ink: rimasti " + left + " blocchi su " + wall.size());
            for (BlockPos pos : wall) level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            level.getEntitiesOfClass(ItemEntity.class, new AABB(center).inflate(4)).forEach(Entity::discard);
        }
        SpectralAscension.LOGGER.info("[GameTest] area della workstaff gratis dal flag freeAoE: {}", String.join("; ", report));
        helper.succeed();
    }

    // ---------- Abilità a Ink in sopravvivenza con la Pigment Palette ----------

    /** Dove sta la Pigment Palette nel test {@link #ink_abilities_survival}. */
    private enum PaletteSpot { INVENTORY, HOTBAR, OFFHAND, CURIO }

    /**
     * Abilità della mod che consumano White Ink, con un giocatore finto in SOPRAVVIVENZA e una Pigment Palette piena
     * nell'inventario, nella hotbar, nella mano secondaria e nello slot Curios "palette". Sono le stesse chiamate
     * della mod (InkPowered.tryDrainEnergy, come gli oggetti di Spectrum): area della workstaff 3×3 (3 Ink), bident
     * Ferocious (Riptide fuori dall'acqua, 10), bident Fractal (copia illusoria, 25), overcharge della balestra (100) e
     * ground slam della ultra greatsword.
     * <ul>
     * <li>con spectrum:milestones/unlock_ink_use e la palette piena ogni abilità parte e scala Ink dalla palette;</li>
     * <li>senza quell'advancement nessuna parte e la palette resta piena: è la regola di Spectrum
     * (InkPowered.canUse), uguale per i suoi oggetti, ed era il «non funziona in survival»;</li>
     * <li>con la palette vuota nessuna parte e niente crasha.</li>
     * </ul>
     */
    @GameTest(template = EMPTY)
    public static void ink_abilities_survival(GameTestHelper helper) {
        var level = helper.getLevel();
        var access = level.registryAccess();
        var inkUse = level.getServer().getAdvancements().get(SpectrumAdvancements.UNLOCK_INK_USE);
        helper.assertTrue(inkUse != null, "advancement mancante: " + SpectrumAdvancements.UNLOCK_INK_USE);
        var paletteItem = SpectrumItems.PIGMENT_PALETTE.get();
        List<String> report = new ArrayList<>();
        int row = 0;
        record Case(PaletteSpot spot, boolean unlocked, boolean full) {
        }
        List<Case> cases = new ArrayList<>();
        for (PaletteSpot spot : PaletteSpot.values()) cases.add(new Case(spot, true, true));
        cases.add(new Case(PaletteSpot.INVENTORY, false, true));
        cases.add(new Case(PaletteSpot.CURIO, false, true));
        cases.add(new Case(PaletteSpot.INVENTORY, true, false));
        cases.add(new Case(PaletteSpot.CURIO, true, false));
        for (Case c : cases) {
            ServerPlayer player = mockPlayer(helper);
            helper.assertFalse(player.isCreative() || player.gameMode.isCreative(), "il giocatore finto deve essere in sopravvivenza");
            aim(player, helper.absolutePos(new BlockPos(0, 80 + 3 * row++, 0)), 0.0F, 0.0F);
            if (c.unlocked()) {
                for (String crit : inkUse.value().criteria().keySet()) player.getAdvancements().award(inkUse, crit);
            }
            helper.assertTrue(InkPoweredCheck.canUse(player) == c.unlocked(), "unlock_ink_use non come atteso");
            ItemStack palette = c.full() ? paletteItem.getFullStack() : paletteItem.getDefaultInstance();
            final ItemStack[] where = new ItemStack[1];
            switch (c.spot()) {
                case INVENTORY -> player.getInventory().setItem(20, palette);
                case HOTBAR -> player.getInventory().setItem(4, palette);
                case OFFHAND -> player.setItemInHand(InteractionHand.OFF_HAND, palette);
                case CURIO -> {
                    var curios = CuriosApi.getCuriosInventory(player);
                    boolean hasSlot = curios.isPresent() && curios.get().getStacksHandler("palette").map(h -> h.getSlots() > 0).orElse(false);
                    helper.assertTrue(hasSlot, "slot Curios 'palette' assente");
                    curios.get().setEquippedCurio("palette", 0, palette);
                }
            }
            java.util.function.Supplier<ItemStack> paletteNow = () -> switch (c.spot()) {
                case INVENTORY -> player.getInventory().getItem(20);
                case HOTBAR -> player.getInventory().getItem(4);
                case OFFHAND -> player.getItemInHand(InteractionHand.OFF_HAND);
                case CURIO -> CuriosApi.getCuriosInventory(player).flatMap(i -> i.getStacksHandler("palette"))
                        .map(h -> h.getStacks().getStackInSlot(0)).orElse(ItemStack.EMPTY);
            };
            helper.assertTrue(paletteNow.get().is(paletteItem), c + ": palette non al suo posto");
            java.util.function.LongSupplier white = () -> paletteItem.getEnergyStorage(paletteNow.get()).getEnergy(InkColors.WHITE);
            boolean shouldWork = c.unlocked() && c.full();
            String id = c.spot() + (c.unlocked() ? " sbloccato" : " NON sbloccato") + (c.full() ? " piena" : " vuota");
            List<String> line = new ArrayList<>();

            // area della workstaff 3×3 (WorkstaffItem.canUseAoE, chiamata da SpectrumEventListeners.onBlockBreak)
            ItemStack staff = new ItemStack(SAItems.TRICHROMATIC_WORKSTAFF.get());
            staff.set(SpectrumDataComponentTypes.AOE, 1);
            long before = white.getAsLong();
            boolean ok = SAItems.TRICHROMATIC_WORKSTAFF.get().canUseAoE(player, staff);
            line.add(checkInk(helper, id, "workstaff 3x3", ok, before - white.getAsLong(), shouldWork, SATools.Workstaff.aoeInkCost(1)));

            for (var bidentHolder : List.of(SAItems.TRICHROMATIC_BIDENT, SAItems.UMBRAL_BIDENT, SAItems.ARGENT_BIDENT, SAItems.PRISMATIC_BIDENT)) {
                SABident bident = bidentHolder.get();
                // Ferocious: Riptide fuori dall'acqua (MalachiteBidentItem.use → canStartRiptide)
                ItemStack fer = new ItemStack(bident);
                SABident.setMode(fer, BidentMode.FEROCIOUS);
                boolean wet = player.isInWaterOrRain();
                before = white.getAsLong();
                ok = bident.canStartRiptide(player, fer);
                if (!wet) {
                    line.add(checkInk(helper, id, bidentHolder.getId().getPath() + " ferocious", ok, before - white.getAsLong(), shouldWork, FerociousCost.RIPTIDE));
                }
                // Fractal: copia illusoria (MalachiteBidentItem.releaseUsing → isThrownAsMirrorImage)
                ItemStack fra = new ItemStack(bident);
                SABident.setMode(fra, BidentMode.FRACTAL);
                before = white.getAsLong();
                ok = bident.isThrownAsMirrorImage(fra, level, player);
                line.add(checkInk(helper, id, bidentHolder.getId().getPath() + " fractal", ok, before - white.getAsLong(), shouldWork, FerociousCost.MIRROR));
            }

            // overcharge della balestra: Shift + tasto destro con la balestra carica (GlassCrestCrossbowItem.use)
            for (var crossbowHolder : List.of(SAItems.TRICHROMATIC_CROSSBOW, SAItems.UMBRAL_CROSSBOW, SAItems.ARGENT_CROSSBOW, SAItems.PRISMATIC_CROSSBOW)) {
                ItemStack crossbow = new ItemStack(crossbowHolder.get());
                crossbow.set(DataComponents.CHARGED_PROJECTILES, net.minecraft.world.item.component.ChargedProjectiles.of(new ItemStack(Items.ARROW)));
                player.setItemInHand(InteractionHand.MAIN_HAND, crossbow);
                player.setShiftKeyDown(true);
                before = white.getAsLong();
                var result = crossbowHolder.get().use(level, player, InteractionHand.MAIN_HAND);
                ok = player.isUsingItem() && result.getResult().consumesAction();
                player.releaseUsingItem();
                player.stopUsingItem();
                player.setShiftKeyDown(false);
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                line.add(checkInk(helper, id, crossbowHolder.getId().getPath() + " overcharge", ok, before - white.getAsLong(), shouldWork, 100));
            }

            // ground slam della ultra greatsword (GlassCrestGreatswordItem.use, forza = Sweeping Edge)
            for (var swordHolder : List.of(SAItems.TRICHROMATIC_ULTRA_GREATSWORD, SAItems.UMBRAL_ULTRA_GREATSWORD, SAItems.ARGENT_ULTRA_GREATSWORD,
                    SAItems.PRISMATIC_ULTRA_GREATSWORD)) {
                var sword = swordHolder.get();
                ItemStack stack = new ItemStack(sword);
                stack.set(DataComponents.ENCHANTMENTS, Preenchanted.buildDefaultEnchantments(access, sword));
                if (sword.getGroundSlamStrength(access, stack) <= 0) {
                    line.add(swordHolder.getId().getPath() + " senza Sweeping Edge (niente ground slam)");
                    continue;
                }
                player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                before = white.getAsLong();
                var result = sword.use(level, player, InteractionHand.MAIN_HAND);
                ok = player.isUsingItem() && result.getResult().consumesAction();
                player.stopUsingItem();
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                long paid = before - white.getAsLong();
                line.add(checkInk(helper, id, swordHolder.getId().getPath() + " ground slam", ok, paid, shouldWork, shouldWork ? paid : 0));
                if (shouldWork) helper.assertTrue(paid > 0, id + ": ground slam senza Ink pagato");
            }
            report.add(id + ": " + String.join(", ", line));
            level.getEntitiesOfClass(Entity.class, new AABB(player.blockPosition()).inflate(16), e -> !(e instanceof net.minecraft.world.entity.player.Player)).forEach(Entity::discard);
        }
        report.forEach(r -> SpectralAscension.LOGGER.info("[GameTest] Ink in sopravvivenza: {}", r));
        helper.succeed();
    }

    /** Costi dei glass crest di Spectrum (costanti pubbliche). */
    private static final class FerociousCost {
        static final long RIPTIDE = de.dafuqs.spectrum.items.tools.FerociousBidentItem.RIPTIDE_COST.amount();
        static final long MIRROR = de.dafuqs.spectrum.items.tools.FractalBidentItem.MIRROR_IMAGE_COST.amount();
    }

    private static final class InkPoweredCheck {
        static boolean canUse(ServerPlayer player) {
            return de.dafuqs.spectrum.api.ink.InkPowered.canUse(player);
        }
    }

    /** Controlla un'abilità: se deve funzionare parte e paga esattamente {@code cost}, altrimenti non parte e non paga. */
    private static String checkInk(GameTestHelper helper, String id, String ability, boolean started, long paid, boolean shouldWork, long cost) {
        if (shouldWork) {
            helper.assertTrue(started, id + ": " + ability + " non è partita");
            helper.assertTrue(paid == cost, id + ": " + ability + " ha pagato " + paid + " Ink invece di " + cost);
        } else {
            helper.assertFalse(started, id + ": " + ability + " è partita senza Ink utilizzabile");
            helper.assertTrue(paid == 0, id + ": " + ability + " ha pagato " + paid + " Ink");
        }
        return ability + (started ? " ok" : " no") + " (-" + paid + ")";
    }

    // ---------- Balestra: traiettoria del proiettile ----------

    /**
     * Tiro della balestra vanilla, glass crest, T6, T7, T8 e Alloy dallo stesso punto e con la stessa mira (verso +Z, 20° in su):
     * registra posizione e velocità della freccia nei primi tick sul server e la velocità che il client riceverebbe dal
     * pacchetto vanilla (ClientboundSetEntityMotionPacket, ±3,9 per componente).
     * <ul>
     * <li>sul server la traiettoria è quella attesa (attrito 0,99 e gravità 0,05 a tick, nessuna deviazione): spawn,
     * rotazione e divergenza sono a posto;</li>
     * <li>le frecce T6/T7 superano il limite del pacchetto: il client le vedeva partire più lente e più inclinate,
     * finché il pacchetto di posizione del server non le rimetteva in linea. Per loro ProjectileVelocityPayload manda
     * la velocità esatta (il controllo dal vivo, client contro server, è lo scenario «balestra»).</li>
     * </ul>
     */
    @GameTest(template = EMPTY, timeoutTicks = 40)
    public static void crossbow_projectile_straight(GameTestHelper helper) {
        var level = helper.getLevel();
        List<Item> crossbows = List.of(net.minecraft.world.item.Items.CROSSBOW, SpectrumItems.GLASS_CREST_CROSSBOW.get(),
                SAItems.TRICHROMATIC_CROSSBOW.get(), SAItems.UMBRAL_CROSSBOW.get(), SAItems.ARGENT_CROSSBOW.get(),
                SAItems.PRISMATIC_CROSSBOW.get());
        List<net.minecraft.world.entity.projectile.AbstractArrow> arrows = new ArrayList<>();
        List<List<net.minecraft.world.phys.Vec3[]>> samples = new ArrayList<>();
        for (int i = 0; i < crossbows.size(); i++) {
            ServerPlayer player = mockPlayer(helper);
            BlockPos feet = helper.absolutePos(new BlockPos(3 * i, 30, 0));
            aim(player, feet, 0.0F, -20.0F);
            ItemStack crossbow = new ItemStack(crossbows.get(i));
            crossbow.set(DataComponents.CHARGED_PROJECTILES,
                    net.minecraft.world.item.component.ChargedProjectiles.of(new ItemStack(net.minecraft.world.item.Items.ARROW)));
            player.setItemInHand(InteractionHand.MAIN_HAND, crossbow);
            crossbow.getItem().use(level, player, InteractionHand.MAIN_HAND);
            var found = level.getEntitiesOfClass(net.minecraft.world.entity.projectile.AbstractArrow.class,
                    new AABB(feet).inflate(4), a -> a.getOwner() == player);
            helper.assertTrue(found.size() == 1, BuiltInRegistries.ITEM.getKey(crossbows.get(i)) + ": frecce tirate " + found.size());
            var arrow = found.getFirst();
            arrows.add(arrow);
            List<net.minecraft.world.phys.Vec3[]> s = new ArrayList<>();
            s.add(new net.minecraft.world.phys.Vec3[]{arrow.position(), arrow.getDeltaMovement(), player.getViewVector(1.0F),
                    new net.minecraft.world.phys.Vec3(player.getX(), player.getEyeY(), player.getZ())});
            samples.add(s);
        }
        // Divergenza: 300 tiri per balestra dalla stessa mira, angolo medio e massimo fra velocità iniziale e mira.
        List<String> spread = new ArrayList<>();
        double[] means = new double[crossbows.size()];
        for (int i = 0; i < crossbows.size(); i++) {
            ServerPlayer player = mockPlayer(helper);
            BlockPos feet = helper.absolutePos(new BlockPos(3 * i, 40, 0));
            aim(player, feet, 0.0F, -20.0F);
            double sum = 0, max = 0;
            for (int n = 0; n < 300; n++) {
                ItemStack crossbow = new ItemStack(crossbows.get(i));
                crossbow.set(DataComponents.CHARGED_PROJECTILES,
                        net.minecraft.world.item.component.ChargedProjectiles.of(new ItemStack(net.minecraft.world.item.Items.ARROW)));
                player.setItemInHand(InteractionHand.MAIN_HAND, crossbow);
                crossbow.getItem().use(level, player, InteractionHand.MAIN_HAND);
                for (var a : level.getEntitiesOfClass(net.minecraft.world.entity.projectile.AbstractArrow.class,
                        new AABB(feet).inflate(4), a -> a.getOwner() == player)) {
                    double ang = angleDeg(a.getDeltaMovement(), player.getViewVector(1.0F));
                    sum += ang;
                    max = Math.max(max, ang);
                    a.discard();
                }
            }
            spread.add(String.format(java.util.Locale.ROOT, "%s media %.2f° massimo %.2f°",
                    BuiltInRegistries.ITEM.getKey(crossbows.get(i)).getPath(), sum / 300, max));
            means[i] = sum / 300;
            helper.assertTrue(max < 2.0, BuiltInRegistries.ITEM.getKey(crossbows.get(i)) + ": divergenza massima " + max + "°");
        }
        // più precise della glass crest (divergenza 0,75), il T7 più del T6, il T8 più del T7 e l'Alloy più del T8
        // (0,63, 0,53, 0,45 e 0,375 in SAItems; 300 tiri perché i passi sono piccoli)
        helper.assertTrue(means[2] < means[1] && means[3] < means[2] && means[4] < means[3] && means[5] < means[4],
                "divergenza media non decrescente: " + String.join("; ", spread));
        SpectralAscension.LOGGER.info("[GameTest] divergenza delle balestre (300 tiri): {}", String.join("; ", spread));
        for (int t = 1; t <= 3; t++) {
            int tick = t;
            helper.runAfterDelay(t, () -> {
                for (int i = 0; i < arrows.size(); i++) {
                    samples.get(i).add(new net.minecraft.world.phys.Vec3[]{arrows.get(i).position(), arrows.get(i).getDeltaMovement()});
                }
                if (tick == 3) checkArrows(helper, crossbows, arrows, samples);
            });
        }
    }

    /**
     * Velocità esatta solo dopo un SetEntityMotion vanilla (ServerEntityMixin): una freccia veloce della balestra T6,
     * tirata in giù verso una lastra di pietra, riceve la correzione in volo (impulso forzato al tick 1, come quello che
     * la vanilla manda ogni 20 tick); piantata nella pietra, con un deltaMovement oltre il limite (il vettore
     * dell'impatto, qui forzato a 5 b/t), non riceve più pacchetti: al massimo uno quando la vanilla manda il cambio di
     * velocità, poi più niente. Prima del 2026-11-27 partiva un pacchetto a ogni tick.
     */
    @GameTest(template = EMPTY, timeoutTicks = 80)
    public static void crossbow_stuck_arrow_no_velocity_spam(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos feet = helper.absolutePos(new BlockPos(0, 80, 0));
        BlockPos pad = feet.below(40);
        // le entità si muovono (e ServerEntity.sendChanges gira) solo nei chunk entity-ticking: forziamo quelli intorno,
        // ricordando quali non lo erano già (il chunk della struttura lo forza il GameTest stesso)
        List<net.minecraft.world.level.ChunkPos> forced = new ArrayList<>();
        var centre = new net.minecraft.world.level.ChunkPos(feet);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                var c = new net.minecraft.world.level.ChunkPos(centre.x + dx, centre.z + dz);
                if (!level.getForcedChunks().contains(c.toLong()) && level.setChunkForced(c.x, c.z, true)) forced.add(c);
            }
        }
        for (BlockPos p : BlockPos.betweenClosed(pad.offset(-3, 0, -3), pad.offset(3, 0, 3))) {
            level.setBlockAndUpdate(p, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        }
        ServerPlayer player = mockPlayer(helper);
        aim(player, feet, 0.0F, 90.0F);
        ItemStack crossbow = new ItemStack(SAItems.TRICHROMATIC_CROSSBOW.get());
        crossbow.set(DataComponents.CHARGED_PROJECTILES,
                net.minecraft.world.item.component.ChargedProjectiles.of(new ItemStack(net.minecraft.world.item.Items.ARROW)));
        player.setItemInHand(InteractionHand.MAIN_HAND, crossbow);
        crossbow.getItem().use(level, player, InteractionHand.MAIN_HAND);
        var found = level.getEntitiesOfClass(net.minecraft.world.entity.projectile.AbstractArrow.class,
                new AABB(feet).inflate(4), a -> a.getOwner() == player);
        helper.assertTrue(found.size() == 1, "frecce tirate " + found.size());
        var arrow = found.getFirst();
        int[] sent = {0};
        cromocon.spectralascension.network.ProjectileVelocityPayload.testSendListener = e -> {
            if (e == arrow) sent[0]++;
        };
        List<String> problems = new ArrayList<>(), report = new ArrayList<>();
        int[] mark = new int[3];
        helper.runAfterDelay(1, () -> arrow.hasImpulse = true);
        helper.runAfterDelay(3, () -> {
            report.add("in volo: " + sent[0] + " pacchetti, y " + String.format(java.util.Locale.ROOT, "%.1f", arrow.getY() - pad.getY()) + " sopra la lastra");
            if (sent[0] < 1) problems.add("in volo nessuna velocità esatta dopo il SetEntityMotion vanilla");
            if (arrow.getY() - pad.getY() < 5) problems.add("la freccia è già quasi a terra al tick 3, controllo in volo non valido");
        });
        helper.runAfterDelay(12, () -> {
            mark[0] = sent[0];
            mark[1] = (int) Math.round(arrow.getY() * 100);
            // vettore dell'impatto sicuramente oltre il limite: setDeltaMovement non segna impulsi
            arrow.setDeltaMovement(0, -5, 0);
        });
        helper.runAfterDelay(13, () -> {
            if (Math.round(arrow.getY() * 100) != mark[1] || arrow.isRemoved()) problems.add("la freccia non è piantata nella pietra al tick 12");
            if (!cromocon.spectralascension.network.ProjectileVelocityPayload.needsExactVelocity(arrow))
                problems.add("la freccia piantata non supera il limite: il test non prova niente");
        });
        helper.runAfterDelay(60, () -> {
            try {
                int stuck = sent[0] - mark[0];
                report.add("piantata, 48 tick: " + stuck + " pacchetti (prima del 2026-11-27 uno a tick)");
                if (stuck > 1) problems.add("freccia piantata: " + stuck + " pacchetti in 48 tick invece di al massimo 1");
            } finally {
                cromocon.spectralascension.network.ProjectileVelocityPayload.testSendListener = null;
                arrow.discard();
                for (BlockPos p : BlockPos.betweenClosed(pad.offset(-3, 0, -3), pad.offset(3, 0, 3))) {
                    level.setBlockAndUpdate(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                }
                forced.forEach(c -> level.setChunkForced(c.x, c.z, false));
            }
            if (!problems.isEmpty()) helper.fail(problems.size() + " problemi, il primo: " + problems.getFirst() + " | " + String.join("; ", report));
            report.forEach(r -> SpectralAscension.LOGGER.info("[GameTest] velocità esatta: {}", r));
            helper.succeed();
        });
    }

    /**
     * Posizione e mira del giocatore finto. Anche la testa: la balestra tira lungo getViewVector, che per le entità
     * vive usa yHeadRot, e il costruttore di LivingEntity la lascia a un angolo casuale (moveTo non la tocca).
     */
    private static void aim(ServerPlayer player, BlockPos feet, float yaw, float pitch) {
        player.moveTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, yaw, pitch);
        player.setYHeadRot(yaw);
        player.yHeadRotO = yaw;
    }

    private static double angleDeg(net.minecraft.world.phys.Vec3 a, net.minecraft.world.phys.Vec3 b) {
        double cos = a.dot(b) / (a.length() * b.length());
        return Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, cos))));
    }

    private static void checkArrows(GameTestHelper helper, List<Item> crossbows,
                                    List<net.minecraft.world.entity.projectile.AbstractArrow> arrows,
                                    List<List<net.minecraft.world.phys.Vec3[]>> samples) {
        List<String> report = new ArrayList<>();
        for (int i = 0; i < arrows.size(); i++) {
            String id = BuiltInRegistries.ITEM.getKey(crossbows.get(i)).getPath();
            var s = samples.get(i);
            var v0 = s.getFirst()[1];
            var look = s.getFirst()[2];
            var eye = s.getFirst()[3];
            StringBuilder line = new StringBuilder(id + String.format(java.util.Locale.ROOT,
                    ": spawn %.2f sotto l'occhio, velocità %.3f b/t, %.2f° dalla mira;", eye.y - s.getFirst()[0].y, v0.length(), angleDeg(v0, look)));
            // traiettoria sul server: attrito e gravità cambiano solo modulo e inclinazione; in orizzontale velocità e
            // spostamenti restano nella direzione del tiro (nessuna deriva laterale)
            var h0 = new net.minecraft.world.phys.Vec3(v0.x, 0, v0.z);
            double maxDev = 0;
            for (int t = 1; t < s.size(); t++) {
                var v = s.get(t)[1];
                var step = s.get(t)[0].subtract(s.get(t - 1)[0]);
                maxDev = Math.max(maxDev, angleDeg(h0, new net.minecraft.world.phys.Vec3(v.x, 0, v.z)));
                if (step.horizontalDistanceSqr() > 1e-6) maxDev = Math.max(maxDev, angleDeg(h0, new net.minecraft.world.phys.Vec3(step.x, 0, step.z)));
                var p = s.get(t)[0];
                line.append(String.format(java.util.Locale.ROOT, " t%d pos (%.2f, %.2f, %.2f) vel (%.3f, %.3f, %.3f);",
                        t, p.x, p.y, p.z, s.get(t)[1].x, s.get(t)[1].y, s.get(t)[1].z));
            }
            helper.assertTrue(maxDev < 0.1, id + ": la freccia devia di lato sul server di " + maxDev + "°");
            // velocità come la riceve il client dal pacchetto vanilla
            var packet = new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(arrows.get(i).getId(), v0);
            var client = new net.minecraft.world.phys.Vec3(packet.getXa(), packet.getYa(), packet.getZa());
            double clientErr = angleDeg(v0, client);
            boolean exact = cromocon.spectralascension.network.ProjectileVelocityPayload.needsExactVelocity(arrows.get(i));
            line.append(String.format(java.util.Locale.ROOT,
                    " pacchetto vanilla: (%.3f, %.3f, %.3f), %.3f b/t, %.2f° di errore; velocità esatta mandata: %s",
                    client.x, client.y, client.z, client.length(), clientErr, exact));
            report.add(line.toString());
            boolean ours = crossbows.get(i) instanceof SATools.Crossbow;
            if (ours) {
                helper.assertTrue(clientErr > 1.0, id + ": atteso il taglio del pacchetto vanilla (errore " + clientErr + "°)");
                helper.assertTrue(exact, id + ": la freccia non riceverebbe la velocità esatta");
                // il pacchetto nostro porta la velocità senza tagli
                var buf = io.netty.buffer.Unpooled.buffer();
                var payload = new cromocon.spectralascension.network.ProjectileVelocityPayload(arrows.get(i));
                cromocon.spectralascension.network.ProjectileVelocityPayload.STREAM_CODEC.encode(buf, payload);
                var back = cromocon.spectralascension.network.ProjectileVelocityPayload.STREAM_CODEC.decode(buf);
                helper.assertTrue(back.equals(payload), id + ": il pacchetto della velocità non torna uguale");
            } else {
                helper.assertFalse(exact, id + ": non è nostra, niente pacchetto extra");
            }
            if (crossbows.get(i) == net.minecraft.world.item.Items.CROSSBOW) {
                helper.assertTrue(clientErr < 0.1, "balestra vanilla: il pacchetto non dovrebbe tagliare (errore " + clientErr + "°)");
            }
        }
        arrows.forEach(Entity::discard);
        report.forEach(r -> SpectralAscension.LOGGER.info("[GameTest] tiro della balestra: {}", r));
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void spectrum_sanity(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "spectrum sanity " + SpectralAscension.MOD_ID);
        helper.succeed();
    }

    /** Minerali di Spectrum nella Mining Dimension (ricerca/12 §3): placed feature caricate e biome modifier applicati
     *  alle feature del bioma allthemodium:mining (step underground_ores). */
    @GameTest(template = EMPTY)
    public static void mining_dim_ores(GameTestHelper helper) {
        var access = helper.getLevel().getServer().registryAccess();
        var placed = access.registryOrThrow(Registries.PLACED_FEATURE);
        var biomes = access.registryOrThrow(Registries.BIOME);
        var biomeKey = ResourceKey.create(Registries.BIOME, ResourceLocation.parse("allthemodium:mining"));
        var biome = biomes.getHolder(biomeKey);
        if (biome.isEmpty()) {
            helper.fail("bioma allthemodium:mining non trovato");
            return;
        }
        var step = biome.get().value().getGenerationSettings().features()
                .get(net.minecraft.world.level.levelgen.GenerationStep.Decoration.UNDERGROUND_ORES.ordinal());
        for (String ore : List.of("shimmerstone_ore", "azurite_ore", "stratine_ore", "paltaeria_ore")) {
            var key = ResourceKey.create(Registries.PLACED_FEATURE,
                    ResourceLocation.fromNamespaceAndPath(SpectralAscension.MOD_ID, "mining_dim/" + ore));
            var pf = placed.getHolder(key);
            if (pf.isEmpty()) {
                helper.fail("placed feature mancante: " + key.location());
                return;
            }
            if (!pf.get().value().feature().unwrapKey().orElseThrow().location().equals(
                    ResourceLocation.fromNamespaceAndPath("spectrum", ore))) {
                helper.fail("configured feature inattesa per " + ore);
                return;
            }
            if (step.stream().noneMatch(h -> h.is(key))) {
                helper.fail("biome modifier non applicato: " + ore + " assente da allthemodium:mining/underground_ores");
                return;
            }
        }
        helper.succeed();
    }
}
