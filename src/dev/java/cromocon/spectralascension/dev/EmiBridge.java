package cromocon.spectralascension.dev;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;

/**
 * Apre la schermata ricette di EMI senza dipendere da EMI in compilazione (è solo runtimeOnly nel client di sviluppo):
 * chiamate per riflessione a {@code EmiApi.displayRecipe(EmiRecipeManager.getRecipe(id))} oppure
 * {@code EmiApi.displayRecipes(EmiStack.of(stack))}.
 */
final class EmiBridge {
    private EmiBridge() {
    }

    static void show(String recipeId, String itemId) {
        try {
            Class<?> api = Class.forName("dev.emi.emi.api.EmiApi");
            if (recipeId != null) {
                Object manager = api.getMethod("getRecipeManager").invoke(null);
                // si passa dall'interfaccia pubblica: la classe concreta (EmiRecipes$Manager) non è accessibile dal nostro modulo
                Class<?> managerClass = Class.forName("dev.emi.emi.api.recipe.EmiRecipeManager");
                Object recipe = managerClass.getMethod("getRecipe", ResourceLocation.class).invoke(manager, ResourceLocation.parse(recipeId));
                if (recipe == null) throw new IllegalArgumentException("EMI non conosce la ricetta " + recipeId);
                Class<?> recipeClass = Class.forName("dev.emi.emi.api.recipe.EmiRecipe");
                api.getMethod("displayRecipe", recipeClass).invoke(null, recipe);
                return;
            }
            if (itemId == null) throw new IllegalArgumentException("per la schermata ricette serve \"recipe\" o \"item\"");
            Class<?> stackClass = Class.forName("dev.emi.emi.api.stack.EmiStack");
            Class<?> ingredientClass = Class.forName("dev.emi.emi.api.stack.EmiIngredient");
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId)));
            Object emiStack = stackClass.getMethod("of", ItemStack.class).invoke(null, stack);
            Method display = api.getMethod("displayRecipes", ingredientClass);
            display.invoke(null, emiStack);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("EMI non è caricato (build con -PnoRecipeViewers?)");
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("chiamata a EMI fallita: " + (e.getCause() != null ? e.getCause() : e));
        }
    }
}
