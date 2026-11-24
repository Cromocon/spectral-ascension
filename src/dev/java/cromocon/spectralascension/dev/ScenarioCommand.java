package cromocon.spectralascension.dev;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.nio.file.Path;

/**
 * Comando CLIENT (lo gestisce il client, non serve essere op): /sa scenario list | run &lt;nome...&gt; | stop.
 * Esiste solo nella mod di sviluppo.
 */
public final class ScenarioCommand {
    private ScenarioCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("sa").then(Commands.literal("scenario")
                .then(Commands.literal("list").executes(ScenarioCommand::list))
                .then(Commands.literal("stop").executes(c -> {
                    if (!ScenarioRunner.get().isRunning()) {
                        c.getSource().sendFailure(Component.literal("Nessuno scenario in corso"));
                        return 0;
                    }
                    ScenarioRunner.get().stop("/sa scenario stop");
                    c.getSource().sendSuccess(() -> Component.literal("Scenario fermato"), false);
                    return 1;
                }))
                .then(Commands.literal("run").then(Commands.argument("nomi", StringArgumentType.greedyString())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(ScenarioFiles.list().keySet(), b))
                        .executes(ScenarioCommand::run)))));
    }

    private static int list(CommandContext<CommandSourceStack> c) {
        Map<String, Path> all = ScenarioFiles.list();
        if (all.isEmpty()) {
            c.getSource().sendFailure(Component.literal("Nessuno scenario in " + ScenarioFiles.dirs()));
            return 0;
        }
        c.getSource().sendSuccess(() -> Component.literal("Scenari (" + all.size() + "):"), false);
        all.forEach((n, p) -> c.getSource().sendSuccess(() -> Component.literal(" - " + n + "  (" + p + ")"), false));
        return all.size();
    }

    private static int run(CommandContext<CommandSourceStack> c) {
        List<String> names = new ArrayList<>();
        for (String n : StringArgumentType.getString(c, "nomi").split("[,\\s]+")) {
            if (n.isBlank()) continue;
            if (n.equals("all")) names.addAll(ScenarioFiles.list().keySet());
            else names.add(n);
        }
        String err = ScenarioRunner.get().start(names, false);
        if (err != null) {
            c.getSource().sendFailure(Component.literal(err));
            return 0;
        }
        return 1;
    }
}
