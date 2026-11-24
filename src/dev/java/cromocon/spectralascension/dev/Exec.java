package cromocon.spectralascension.dev;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Un passo in esecuzione. Il runner chiama {@link #poll} una volta per tick del client finché non restituisce un
 * esito (null = ancora in corso).
 */
@FunctionalInterface
public interface Exec {
    @Nullable StepResult poll(ScenarioRunner runner);

    /** Durata prevista in tick (per il timeout del passo). */
    default int expectedTicks() {
        return 0;
    }

    static Exec done(StepResult r) {
        return runner -> r;
    }

    /** Aspetta un future (di solito lavoro sul thread del server integrato). */
    static Exec future(CompletableFuture<StepResult> f) {
        return runner -> {
            if (!f.isDone()) return null;
            try {
                return f.join();
            } catch (Exception e) {
                Throwable c = e.getCause() != null ? e.getCause() : e;
                return StepResult.fail("eccezione: " + c);
            }
        };
    }

    /** Esegue prima questo passo, poi (se riuscito o se {@code always}) quello costruito da {@code next}. */
    default Exec then(Function<StepResult, Exec> next, boolean always) {
        Exec first = this;
        return new Exec() {
            Exec second;

            @Override
            public StepResult poll(ScenarioRunner runner) {
                if (second == null) {
                    StepResult r = first.poll(runner);
                    if (r == null) return null;
                    if (!r.ok() && !always) return r;
                    second = next.apply(r);
                }
                return second.poll(runner);
            }

            @Override
            public int expectedTicks() {
                return first.expectedTicks() + 40;
            }
        };
    }

    /** Aspetta {@code ticks} tick, poi esegue {@code after} (sul thread del client). */
    static Exec delay(int ticks, java.util.function.Supplier<StepResult> after) {
        return new Exec() {
            int left = ticks;

            @Override
            public StepResult poll(ScenarioRunner runner) {
                if (left-- > 0) return null;
                return after.get();
            }

            @Override
            public int expectedTicks() {
                return ticks;
            }
        };
    }
}
