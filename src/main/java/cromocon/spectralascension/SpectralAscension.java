package cromocon.spectralascension;

import com.mojang.logging.LogUtils;
import cromocon.spectralascension.item.SAArmorSet;
import cromocon.spectralascension.item.MaceWindBurst;
import cromocon.spectralascension.item.SAEnchantCap;
import cromocon.spectralascension.item.PrismaticTrail;
import cromocon.spectralascension.network.BidentModePayload;
import cromocon.spectralascension.network.PrismaticTrailPayload;
import cromocon.spectralascension.network.ProjectileVelocityPayload;
import cromocon.spectralascension.network.WorkstaffOptionPayload;
import cromocon.spectralascension.registry.SAArmorMaterials;
import cromocon.spectralascension.registry.SAComponents;
import cromocon.spectralascension.registry.SAItems;
import cromocon.spectralascension.registry.SAMenus;
import cromocon.spectralascension.registry.SAPredicates;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraft.core.dispenser.ShearsDispenseItemBehavior;
import net.minecraft.world.level.block.DispenserBlock;
import java.util.List;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * Classe principale della mod: NeoForge la crea all'avvio perché ha l'annotazione {@link Mod}.
 * Qui colleghiamo i registri (mod bus) e gli eventi di gioco (game bus).
 */
@Mod(SpectralAscension.MOD_ID)
public final class SpectralAscension {
    public static final String MOD_ID = "spectral_ascension";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SpectralAscension(IEventBus modBus, ModContainer container) {
        SAArmorMaterials.register(modBus);                             // mod bus: materiali dell'armatura
        SAComponents.register(modBus);                                 // mod bus: data component (modalità del bident)
        SAItems.register(modBus);                                      // mod bus: registrazione degli item
        SAMenus.register(modBus);                                      // mod bus: menu (bident, workstaff T7+)
        SAPredicates.register(modBus);                                 // mod bus: predicati degli item (advancement)
        modBus.addListener(ProjectileVelocityPayload::register);       // mod bus: velocità esatta delle frecce veloci
        PrismaticTrail.register(modBus);                               // mod bus: attachment della scia delle frecce T9
        modBus.addListener(PrismaticTrailPayload::register);           // mod bus: scia delle frecce T9 al client
        NeoForge.EVENT_BUS.addListener(PrismaticTrail::onEntityJoin);  // game bus: marca le frecce tirate da arco/balestra T9
        modBus.addListener(BidentModePayload::register);               // mod bus: modalità scelta nel menu del bident
        modBus.addListener(WorkstaffOptionPayload::register);          // mod bus: voce scelta nel menu della workstaff T7+
        NeoForge.EVENT_BUS.addListener(SAItems::addToSpectrumSubTabs); // game bus: sotto-tab "Equipment" di Spectrum
        NeoForge.EVENT_BUS.addListener(SAArmorSet::onEquipmentChange); // game bus: Dike del set quando cambia l'armatura
        // game bus, LOWEST = dopo Apothic: libro estratto all'incudine da un nostro oggetto tagliato al tetto ATM10
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, SAEnchantCap::onAnvilUpdate);
        NeoForge.EVENT_BUS.addListener(MaceWindBurst::onGrindstoneTake); // game bus: XP della mola senza il Wind Burst intrinseco
        modBus.addListener(SpectralAscension::commonSetup);             // mod bus: comportamenti dei dispenser
    }

    /**
     * Cesoie nel dispenser: il gioco sceglie il comportamento del dispenser per OGGETTO (DispenserBlock.registerBehavior),
     * non per classe. Vanilla lo registra per le sue cesoie e Spectrum per le bedrock (SpectrumDispenserBehaviors):
     * senza questa riga le nostre cesoie venivano sparate fuori come un oggetto qualsiasi invece di tosare.
     * GameTest SAFunctionTests.shears_dispenser.
     */
    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            for (var shears : List.of(SAItems.TRICHROMATIC_SHEARS, SAItems.UMBRAL_SHEARS, SAItems.ARGENT_SHEARS, SAItems.PRISMATIC_SHEARS)) {
                DispenserBlock.registerBehavior(shears.get(), new ShearsDispenseItemBehavior());
            }
        });
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
