package cromocon.spectralascension.registry;

import cromocon.spectralascension.SpectralAscension;
import de.dafuqs.fractal.api.CreativeSubTabEvent;
import de.dafuqs.spectrum.api.item.Preenchanted;
import de.dafuqs.spectrum.api.item_group.ItemGroupIDs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Map;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SpectralAscension.MOD_ID);

    /** Oggetto di prova: indistruttibile come la bedrock e "preincantato" con l'interfaccia di Spectrum. */
    public static final DeferredItem<TemplateItem> TEMPLATE_ITEM = ITEMS.register("template_item",
            () -> new TemplateItem(new Item.Properties()
                    .stacksTo(1)
                    .fireResistant()
                    .rarity(Rarity.EPIC)
                    .component(DataComponents.UNBREAKABLE, new Unbreakable(false))));

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    public static void addToSpectrumSubTabs(CreativeSubTabEvent event) {
        if (event.subGroup().getIdentifier().equals(ItemGroupIDs.SUBTAB_EQUIPMENT)) {
            event.getItemDisplayBuilder().accept(TEMPLATE_ITEM.get());
        }
    }

    public static class TemplateItem extends Item implements Preenchanted {
        public TemplateItem(Properties properties) { super(properties); }

        @Override
        public Map<ResourceKey<Enchantment>, Integer> getDefaultEnchantments() {
            return Map.of(Enchantments.SHARPNESS, 7); // T6 = vanilla + 2
        }
    }
}
