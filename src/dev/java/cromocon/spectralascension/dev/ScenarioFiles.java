package cromocon.spectralascension.dev;

import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Dove stanno gli scenari:
 * <ol>
 *   <li>{@code <cartella del gioco>/scenarios/*.json} (run/client/scenarios): scenari locali, vincono in caso di nome uguale;</li>
 *   <li>la cartella della property {@code spectral_ascension.scenarioDir} (build.gradle: src/dev/resources/scenarios),
 *       letta direttamente dai sorgenti, quindi le modifiche valgono senza ricompilare.</li>
 * </ol>
 * I preset di {@code build} stanno in {@code presets/} dentro le stesse cartelle.
 */
public final class ScenarioFiles {
    public static final String DIR_PROPERTY = "spectral_ascension.scenarioDir";

    private ScenarioFiles() {
    }

    /** Cartelle in ordine di priorità. */
    public static List<Path> dirs() {
        List<Path> out = new ArrayList<>();
        out.add(Minecraft.getInstance().gameDirectory.toPath().resolve("scenarios").toAbsolutePath());
        String prop = System.getProperty(DIR_PROPERTY, "");
        if (!prop.isBlank()) out.add(Path.of(prop).toAbsolutePath());
        return out;
    }

    /** nome → file, in ordine alfabetico. */
    public static Map<String, Path> list() {
        Map<String, Path> out = new TreeMap<>();
        List<Path> dirs = dirs();
        for (int i = dirs.size() - 1; i >= 0; i--) { // le cartelle prioritarie sovrascrivono
            Path d = dirs.get(i);
            if (!Files.isDirectory(d)) continue;
            try (Stream<Path> s = Files.list(d)) {
                s.filter(p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith(".json"))
                        .forEach(p -> out.put(p.getFileName().toString().replaceFirst("\\.json$", ""), p));
            } catch (IOException ignored) {
            }
        }
        return out;
    }

    /** Un file relativo (preset o struttura): cerca in presets/ e nella cartella base di ogni cartella scenari. */
    public static Path resolve(String rel, boolean preset) throws IOException {
        String name = preset && !rel.endsWith(".json") ? rel + ".json" : rel;
        for (Path d : dirs()) {
            Path p = preset ? d.resolve("presets").resolve(name) : d.resolve(name);
            if (Files.isRegularFile(p)) return p;
        }
        Path abs = Path.of(rel);
        if (abs.isAbsolute() && Files.isRegularFile(abs)) return abs;
        throw new IOException((preset ? "preset" : "file") + " \"" + rel + "\" non trovato in " + dirs());
    }
}
