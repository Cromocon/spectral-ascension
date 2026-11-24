package cromocon.spectralascension.dev;

import com.google.gson.JsonObject;
import com.klikli_dev.modonomicon.book.Book;
import com.klikli_dev.modonomicon.book.entries.BookEntry;
import com.klikli_dev.modonomicon.client.gui.BookGuiManager;
import com.klikli_dev.modonomicon.client.gui.book.BookAddress;
import com.klikli_dev.modonomicon.client.gui.book.entry.BookEntryScreen;
import com.klikli_dev.modonomicon.client.gui.book.node.BookCategoryNodeScreen;
import com.klikli_dev.modonomicon.client.gui.book.node.BookParentNodeScreen;
import com.klikli_dev.modonomicon.data.BookDataManager;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.lang.reflect.Field;
import java.util.Locale;

/**
 * book_focus: nella mappa di una categoria del guidebook (Modonomicon) porta una voce al centro della vista e ci mette
 * sopra il mouse, così lo screenshot mostra il suo tooltip (nome + descrizione, es. «Added by Spectral Ascension»).
 * <p>
 * Apre la categoria della voce se non è già aperta. La posizione segue {@code BookCategoryNodeScreen.isEntryHovered}:
 * centro della voce = (x*30 + xOffset + 2 + 13) * zoom, con xOffset = innerWidth/2/zoom - scrollX/2.
 * scrollX/scrollY/currentZoom sono privati: si leggono e scrivono per riflessione. Il mouse si sposta scrivendo
 * MouseHandler.xpos/ypos (pixel della finestra), che il client legge a ogni frame.
 */
final class BookFocus {
    private BookFocus() {
    }

    static Exec create(JsonObject s) {
        ResourceLocation bookId = ResourceLocation.parse(Scenario.str(s, "book", "spectrum:guidebook"));
        ResourceLocation entryId = ResourceLocation.parse(Scenario.req(s, "entry"));
        boolean hover = Scenario.bool(s, "hover", true);
        return new Exec() {
            int t;
            boolean opened;

            @Override
            public StepResult poll(ScenarioRunner runner) {
                Minecraft mc = Minecraft.getInstance();
                if (!BookDataManager.get().areBooksBuilt()) BookDataManager.get().tryBuildBooks(mc.level);
                Book book = BookDataManager.get().getBook(bookId);
                if (book == null) return StepResult.fail("libro inesistente: " + bookId);
                BookEntry entry = book.getEntry(entryId);
                if (entry == null) return StepResult.fail("voce inesistente: " + entryId);
                if (mc.screen instanceof BookEntryScreen es) {
                    // Modonomicon riapre l'ultima voce letta sopra la mappa: la si chiude per tornare alla categoria
                    BookGuiManager.get().closeEntryScreen(es);
                    return null;
                }
                BookCategoryNodeScreen node = mc.screen instanceof BookParentNodeScreen p ? p.getCurrentCategoryScreen() : null;
                if (node == null || !node.getCategory().getId().equals(entry.getCategory().getId())) {
                    if (opened) return t++ < 40 ? null : StepResult.fail("categoria " + entry.getCategory().getId() + " non aperta (schermata: "
                            + (mc.screen == null ? "nessuna" : mc.screen.getClass().getSimpleName()) + ")");
                    // openBook (non openCategory): crea la schermata del libro e poi apre la categoria indicata
                    BookGuiManager.get().openBook(BookAddress.defaultFor(entry.getCategory()));
                    opened = true;
                    return null;
                }
                if (t++ < 3) return null; // un paio di frame: lo zoom si assesta
                BookParentNodeScreen parent = (BookParentNodeScreen) mc.screen;
                try {
                    float zoom = getF(node, "currentZoom");
                    int ix = parent.getInnerX(), iy = parent.getInnerY(), iw = parent.getInnerWidth(), ih = parent.getInnerHeight();
                    // scroll che porta il centro della voce al centro dell'area interna (limitato come fa Modonomicon)
                    float maxX = entry.getCategory().getMaxScrollX(), maxY = entry.getCategory().getMaxScrollY();
                    float sx = 2 * (iw / 2f / zoom - ((ix + iw / 2f) / zoom - entry.getX() * 30 - 15));
                    float sy = 2 * (ih / 2f / zoom - ((iy + ih / 2f) / zoom - entry.getY() * 30 - 15));
                    sx = Mth.clamp(sx, -maxX, maxX);
                    sy = Mth.clamp(sy, -maxY, maxY);
                    setF(node, "scrollX", sx);
                    setF(node, "scrollY", sy);
                    float hx = (entry.getX() * 30 + node.getXOffset() + 15) * zoom;
                    float hy = (entry.getY() * 30 + node.getYOffset() + 15) * zoom;
                    boolean inside = hx >= ix && hx <= ix + iw && hy >= iy && hy <= iy + ih;
                    Window w = mc.getWindow();
                    double px = hover ? hx * w.getScreenWidth() / (double) w.getGuiScaledWidth() : 0;
                    double py = hover ? hy * w.getScreenHeight() / (double) w.getGuiScaledHeight() : 0;
                    setD(mc.mouseHandler, "xpos", px);
                    setD(mc.mouseHandler, "ypos", py);
                    return StepResult.of(inside || !hover, String.format(Locale.ROOT,
                            "voce %s (%d,%d) al centro: scroll=(%.0f,%.0f) zoom=%.2f, mouse gui=(%.0f,%.0f)%s",
                            entryId, entry.getX(), entry.getY(), sx, sy, zoom, hx, hy, inside ? "" : " FUORI dall'area visibile"));
                } catch (ReflectiveOperationException e) {
                    return StepResult.fail("riflessione su Modonomicon/MouseHandler fallita: " + e);
                }
            }

            @Override
            public int expectedTicks() {
                return 50;
            }
        };
    }

    private static Field field(Class<?> c, String name) throws NoSuchFieldException {
        Field f = c.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }

    private static float getF(Object o, String name) throws ReflectiveOperationException {
        return field(o.getClass(), name).getFloat(o);
    }

    private static void setF(Object o, String name, float v) throws ReflectiveOperationException {
        field(o.getClass(), name).setFloat(o, v);
    }

    private static void setD(MouseHandler o, String name, double v) throws ReflectiveOperationException {
        field(MouseHandler.class, name).setDouble(o, v);
    }
}
