package cromocon.spectralascension.client;

import cromocon.spectralascension.SpectralAscension;
import cromocon.spectralascension.item.SABident;
import cromocon.spectralascension.item.SATools;
import cromocon.spectralascension.registry.SAItems;
import cromocon.spectralascension.registry.SAMenus;
import de.dafuqs.spectrum.items.tools.MalachiteCrossbowItem;
import de.dafuqs.spectrum.items.tools.SpectrumFishingRodItem;
import de.dafuqs.spectrum.registries.SpectrumItems;
import de.dafuqs.spectrum.registries.client.SpectrumModelLayerLocations;
import de.dafuqs.spectrum.registries.client.SpectrumModelPredicateProviders;
import de.dafuqs.spectrum.render.armor.BedrockArmorModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.EnumMap;
import java.util.Map;

/**
 * Codice solo client (Dist.CLIENT): sul server questa classe non viene caricata.
 * <p>
 * I "predicati" dei modelli sono numeri che il gioco calcola a ogni frame e che i JSON dei modelli usano
 * negli "overrides" per scegliere quale texture mostrare (es. l'arco teso). Copiano la logica di
 * Spectrum (SpectrumModelPredicateProviders), che è privata e registrata solo per i suoi oggetti.
 */
@EventBusSubscriber(modid = SpectralAscension.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SAClient {
    private SAClient() {
    }

    /**
     * Resource pack opzionale «Eternal Classics» (cartella `resourcepacks/spectral_ascension_eternal_classics` nel jar,
     * come fa Spectrum con il suo). Non è attivo di default: si accende dal menu dei resource pack.
     */
    @SubscribeEvent
    public static void onAddPackFinders(AddPackFindersEvent event) {
        event.addPackFinders(
                ResourceLocation.fromNamespaceAndPath(SpectralAscension.MOD_ID, "resourcepacks/spectral_ascension_eternal_classics"),
                PackType.CLIENT_RESOURCES,
                Component.literal("Spectral Ascension: Eternal Classics"),
                PackSource.BUILT_IN,
                false,
                Pack.Position.TOP);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // Tooltip dell'armatura: Dike del set con gli stivali indossati da chi guarda (SAArmorSet.tooltipSetDike).
        cromocon.spectralascension.item.SAArmorSet.setTooltipViewer(() -> net.minecraft.client.Minecraft.getInstance().player);
        event.enqueueWork(() -> {
            // Predicati dei modelli per tipo d'oggetto, su tutti i tier (SAItems.equipment()).
            for (DeferredItem<? extends Item> holder : SAItems.equipment()) {
                Item item = holder.get();
                if (item instanceof SATools.Bow) registerBow(item);
                if (item instanceof SATools.FishingRod) registerFishingRod(item);
                if (item instanceof SATools.Workstaff || item instanceof SATools.UltraGreatsword
                        || item instanceof SATools.Crossbow || item instanceof SABident) registerOversized(item);
                if (item instanceof SATools.Crossbow) registerCrossbow(item);
                if (item instanceof SABident) registerBidentThrowing(item);
            }
        });
    }

    /** Schermate dei menu aperti dal server: bident (BidentMenu) e workstaff T7+ (WorkstaffMenu). */
    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(SAMenus.BIDENT.get(), BidentScreen::new);
        event.register(SAMenus.WORKSTAFF.get(), SAWorkstaffScreen::new);
    }

    /**
     * Armatura: usa il modello 3D dell'armatura bedrock di Spectrum (con la nostra texture, da
     * SAArmorItem.getArmorTexture), come fa Spectrum in SpectrumClientEventListeners. Il mantello lo aggiunge
     * già Spectrum (BedrockCapeRenderer) a ogni BedrockArmorItem.
     */
    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            private final Map<EquipmentSlot, HumanoidModel<LivingEntity>> models = new EnumMap<>(EquipmentSlot.class);

            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
                return models.computeIfAbsent(slot, s -> new BedrockArmorModel(
                        Minecraft.getInstance().getEntityModels().bakeLayer(SpectrumModelLayerLocations.BEDROCK_LAYER), s));
            }
        }, SAItems.armor().stream().map(DeferredItem::get).toArray(Item[]::new));
    }

    private static void registerBow(Item bow) {
        ItemProperties.register(bow, ResourceLocation.withDefaultNamespace("pull"), (stack, level, entity, seed) ->
                entity == null || entity.getUseItem() != stack ? 0.0F
                        : (float) (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / 20.0F);
        ItemProperties.register(bow, ResourceLocation.withDefaultNamespace("pulling"), (stack, level, entity, seed) ->
                entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
    }

    /**
     * Oggetti "oversized" (texture 32×32 in mano, 16×16 nell'inventario). Il mixin ItemRendererMixin di Spectrum
     * risolve gli override di QUALSIASI item col seed MAGIC_OVERSIZED_SEED quando lo disegna in mano: il predicato
     * vale 1 solo in quel caso e il modello sceglie <id>_oversized.
     */
    private static void registerOversized(Item item) {
        ItemProperties.register(item, ResourceLocation.withDefaultNamespace("oversized"), (stack, level, entity, seed) ->
                seed == SpectrumModelPredicateProviders.MAGIC_OVERSIZED_SEED ? 1.0F : 0.0F);
    }

    /**
     * Balestra glass crest (copia di registerMalachiteCrossbowPredicates): pull/pulling durante la carica, charged
     * quando è carica e projectile per il tipo di proiettile (0,1 razzo, 0,2..0,7 frecce di vetro), che i JSON usano
     * per scegliere tra gli stati standby, pulling_0..2, arrow, firework, glass, topaz, amethyst, citrine, onyx, moonstone.
     */
    private static void registerCrossbow(Item crossbow) {
        ItemProperties.register(crossbow, ResourceLocation.withDefaultNamespace("pull"), (stack, level, entity, seed) ->
                entity == null || CrossbowItem.isCharged(stack) ? 0.0F
                        : (float) (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / (float) CrossbowItem.getChargeDuration(stack, entity));
        ItemProperties.register(crossbow, ResourceLocation.withDefaultNamespace("pulling"), (stack, level, entity, seed) ->
                entity != null && entity.isUsingItem() && entity.getUseItem() == stack && !CrossbowItem.isCharged(stack) ? 1.0F : 0.0F);
        ItemProperties.register(crossbow, ResourceLocation.withDefaultNamespace("charged"), (stack, level, entity, seed) ->
                entity != null && CrossbowItem.isCharged(stack) ? 1.0F : 0.0F);
        ItemProperties.register(crossbow, ResourceLocation.withDefaultNamespace("projectile"), (stack, level, entity, seed) -> {
            ItemStack projectile = MalachiteCrossbowItem.getFirstProjectile(stack);
            if (projectile.isEmpty()) return 0.0F;
            if (projectile.is(Items.FIREWORK_ROCKET)) return 0.1F;
            if (projectile.is(SpectrumItems.MALACHITE_GLASS_ARROW)) return 0.2F;
            if (projectile.is(SpectrumItems.TOPAZ_GLASS_ARROW)) return 0.3F;
            if (projectile.is(SpectrumItems.AMETHYST_GLASS_ARROW)) return 0.4F;
            if (projectile.is(SpectrumItems.CITRINE_GLASS_ARROW)) return 0.5F;
            if (projectile.is(SpectrumItems.ONYX_GLASS_ARROW)) return 0.6F;
            if (projectile.is(SpectrumItems.MOONSTONE_GLASS_ARROW)) return 0.7F;
            return 0.0F;
        });
    }

    /**
     * Bident (copia di registerBidentThrowingItemPredicate): 0,5 mentre si carica il lancio → modello
     * _oversized_throwing. Il bident lanciato lo disegna BidentEntityRenderer col seed oversized e senza entità
     * (predicato 0) → modello _oversized.
     */
    private static void registerBidentThrowing(Item bident) {
        ItemProperties.register(bident, ResourceLocation.withDefaultNamespace("bident_throwing"), (stack, level, entity, seed) ->
                entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 0.5F : 0.0F);
    }

    private static void registerFishingRod(Item rod) {
        ItemProperties.register(rod, ResourceLocation.withDefaultNamespace("cast"), (stack, level, entity, seed) -> {
            if (entity == null) {
                return 0.0F;
            }
            boolean mainHand = entity.getMainHandItem() == stack;
            boolean offHand = entity.getOffhandItem() == stack && !(entity.getMainHandItem().getItem() instanceof SpectrumFishingRodItem);
            return (mainHand || offHand) && entity instanceof Player player && player.fishing != null ? 1.0F : 0.0F;
        });
    }
}
