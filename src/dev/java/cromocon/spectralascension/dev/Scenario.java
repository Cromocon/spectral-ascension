package cromocon.spectralascension.dev;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import net.minecraft.core.BlockPos;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Uno scenario letto da file JSON: {"description", "stop_on_fail", "steps": [{"action": ...}, ...]}. */
public final class Scenario {
    final String name;
    final Path file;
    final String description;
    final boolean stopOnFail;
    final List<JsonObject> steps = new ArrayList<>();

    private Scenario(String name, Path file, JsonObject root) {
        this.name = name;
        this.file = file;
        this.description = str(root, "description", "");
        this.stopOnFail = bool(root, "stop_on_fail", false);
        JsonArray arr = root.getAsJsonArray("steps");
        if (arr == null) throw new IllegalArgumentException("manca l'array \"steps\"");
        for (JsonElement e : arr) {
            if (!e.isJsonObject()) throw new IllegalArgumentException("ogni passo deve essere un oggetto JSON: " + e);
            steps.add(e.getAsJsonObject());
        }
    }

    static Scenario load(String name) throws IOException {
        Path p = ScenarioFiles.list().get(name);
        if (p == null) throw new IOException("scenario \"" + name + "\" non trovato in " + ScenarioFiles.dirs());
        return new Scenario(name, p, readJson(p));
    }

    /** JSON "lenient": ammessi commenti // e virgole in più. */
    static JsonObject readJson(Path p) throws IOException {
        try (Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
            JsonReader jr = new JsonReader(r);
            jr.setLenient(true);
            JsonElement e = JsonParser.parseReader(jr);
            if (!e.isJsonObject()) throw new IOException(p + ": serve un oggetto JSON");
            return e.getAsJsonObject();
        } catch (RuntimeException e) {
            throw new IOException(p + ": JSON non valido: " + e.getMessage(), e);
        }
    }

    // --- piccoli lettori di campi ---
    static String str(JsonObject o, String k, String def) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : def;
    }

    static String req(JsonObject o, String k) {
        if (!o.has(k)) throw new IllegalArgumentException("manca il campo \"" + k + "\"");
        return o.get(k).getAsString();
    }

    static int integer(JsonObject o, String k, int def) {
        return o.has(k) ? o.get(k).getAsInt() : def;
    }

    static double num(JsonObject o, String k, double def) {
        return o.has(k) ? o.get(k).getAsDouble() : def;
    }

    static boolean bool(JsonObject o, String k, boolean def) {
        if (!o.has(k)) return def;
        JsonElement e = o.get(k);
        if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isString()) {
            String s = e.getAsString();
            return s.equalsIgnoreCase("on") || s.equalsIgnoreCase("true") || s.equalsIgnoreCase("yes");
        }
        return e.getAsBoolean();
    }

    static BlockPos pos(JsonObject o, String k) {
        if (!o.has(k)) throw new IllegalArgumentException("manca il campo \"" + k + "\" ([x, y, z])");
        return pos(o.get(k));
    }

    static BlockPos pos(JsonElement e) {
        JsonArray a = e.getAsJsonArray();
        if (a.size() != 3) throw new IllegalArgumentException("posizione non valida (serve [x, y, z]): " + a);
        return new BlockPos((int) Math.floor(a.get(0).getAsDouble()), (int) Math.floor(a.get(1).getAsDouble()), (int) Math.floor(a.get(2).getAsDouble()));
    }
}
