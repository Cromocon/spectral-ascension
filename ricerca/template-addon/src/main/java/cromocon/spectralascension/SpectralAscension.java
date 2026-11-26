package cromocon.spectralascension;

import cromocon.spectralascension.registry.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.resources.ResourceLocation;

@Mod(SpectralAscension.MOD_ID)
public final class SpectralAscension {
    public static final String MOD_ID = "spectral_ascension";

    public SpectralAscension(IEventBus modBus, ModContainer container) {
        ModItems.register(modBus);                                      // mod bus: registrazione
        NeoForge.EVENT_BUS.addListener(ModItems::addToSpectrumSubTabs); // game bus: sotto-tab di Spectrum
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
