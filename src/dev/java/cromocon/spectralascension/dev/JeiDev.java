package cromocon.spectralascension.dev;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.IFocusFactory;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Solo sviluppo: plugin JEI che conserva il runtime e permette di (a) contare le ricette della Fusion Shrine
 * mostrate da SpectrumJEI, (b) aprire la schermata ricette di JEI su un oggetto. Le righe di log hanno il prefisso [SA-JEI].
 */
@JeiPlugin
public class JeiDev implements IModPlugin {
    private static final Logger LOG = LogUtils.getLogger();
    private static volatile IJeiRuntime runtime;

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath("spectral_ascension_dev", "jei_dev");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime r) {
        runtime = r;
        LOG.info("[SA-JEI] runtime JEI disponibile");
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
    }

    static boolean available() {
        return runtime != null;
    }

    /** Conta (e logga) le ricette della categoria Fusion Shrine di SpectrumJEI. Ritorna "visibili/totali (nostre visibili/nostre totali)". */
    static String dumpFusion(String modid, boolean verbose) {
        IJeiRuntime r = runtime;
        if (r == null) throw new IllegalStateException("runtime JEI non disponibile");
        IRecipeManager rm = r.getRecipeManager();
        List<IRecipeCategory<?>> cats = rm.createRecipeCategoryLookup().includeHidden().get().toList();
        IRecipeCategory<?> cat = cats.stream().filter(c -> c.getRecipeType().getUid().getPath().contains("fusion_shrine")).findFirst()
                .orElseThrow(() -> new IllegalStateException("categoria Fusion Shrine non trovata fra " + cats.size() + " categorie: "
                        + cats.stream().map(c -> c.getRecipeType().getUid().toString()).collect(Collectors.joining(","))));
        LOG.info("[SA-JEI] categoria {} titolo='{}'", cat.getRecipeType().getUid(), cat.getTitle().getString());
        return dump(rm, cat.getRecipeType(), modid, verbose);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static String dump(IRecipeManager rm, RecipeType type, String modid, boolean verbose) {
        List<Object> all = (List<Object>) rm.createRecipeLookup(type).includeHidden().get().collect(Collectors.toList());
        List<Object> visible = (List<Object>) rm.createRecipeLookup(type).get().collect(Collectors.toList());
        int ours = 0, oursVisible = 0;
        for (Object o : all) {
            if (!(o instanceof RecipeHolder<?> h)) continue;
            if (!h.id().getNamespace().equals(modid)) continue;
            ours++;
            boolean vis = visible.contains(o);
            if (vis) oursVisible++;
            Recipe<?> rec = h.value();
            ItemStack out = rec.getResultItem(Minecraft.getInstance().level.registryAccess());
            String ing = rec.getIngredients().stream().map(i -> {
                ItemStack[] it = i.getItems();
                return it.length == 0 ? "?" : BuiltInRegistries.ITEM.getKey(it[0].getItem()).getPath() + (it.length > 1 ? "|+" + (it.length - 1) : "");
            }).collect(Collectors.joining(", "));
            String fluid = "";
            String extra = "";
            try {
                Object f = rec.getClass().getMethod("getFluid").invoke(rec);
                fluid = String.valueOf(((net.neoforged.neoforge.fluids.crafting.FluidIngredient) f).getStacks().length > 0
                        ? ((net.neoforged.neoforge.fluids.crafting.FluidIngredient) f).getStacks()[0].getFluid().getFluidType().getDescriptionId() : "none");
                extra = " time=" + rec.getClass().getMethod("getCraftingTime").invoke(rec) + " xp=" + rec.getClass().getMethod("getExperience").invoke(rec);
                var stacks = (List<de.dafuqs.spectrum.api.recipe.IngredientStack>) rec.getClass().getMethod("getIngredientStacks").invoke(rec);
                ing = stacks.stream().map(is -> {
                    ItemStack[] m = is.getItems().toArray(ItemStack[]::new);
                    return (m.length == 0 ? "?" : BuiltInRegistries.ITEM.getKey(m[0].getItem()).getPath()) + " x" + is.getCount();
                }).collect(Collectors.joining(", "));
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
            if (verbose) LOG.info("[SA-JEI] recipe {} visible={} out={} x{} ing=[{}] fluid={}{}", h.id(), vis,
                    BuiltInRegistries.ITEM.getKey(out.getItem()), out.getCount(), ing, fluid, extra);
        }
        String res = "nostre " + modid + ": totali=" + ours + " visibili=" + oursVisible + "; categoria totali=" + all.size() + " visibili=" + visible.size();
        LOG.info("[SA-JEI] TOTAL {}", res);
        return res;
    }

    /** Apre JEI sulla ricetta della Fusion Shrine (SpectrumJEI) che produce l'oggetto. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static void showOutput(String itemId) {
        IJeiRuntime r = runtime;
        if (r == null) throw new IllegalStateException("runtime JEI non disponibile");
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
        ItemStack stack = new ItemStack(item);
        IRecipeManager rm = r.getRecipeManager();
        IRecipeCategory cat = rm.createRecipeCategoryLookup().includeHidden().get()
                .filter(c -> c.getRecipeType().getUid().getPath().contains("fusion_shrine")).findFirst()
                .orElseThrow(() -> new IllegalStateException("categoria Fusion Shrine non trovata"));
        var regAccess = Minecraft.getInstance().level.registryAccess();
        List<Object> found = ((java.util.stream.Stream<Object>) rm.createRecipeLookup(cat.getRecipeType()).includeHidden().get())
                .filter(o -> o instanceof RecipeHolder<?> h && h.value().getResultItem(regAccess).is(item)).collect(Collectors.toList());
        if (found.isEmpty()) throw new IllegalStateException("nessuna ricetta Fusion Shrine con risultato " + itemId);
        Object recipe = found.get(0);
        IFocusFactory ff = r.getJeiHelpers().getFocusFactory();
        r.getRecipesGui().showRecipes(cat, List.of(recipe), List.of(ff.createFocus(RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, stack)));
    }
}
