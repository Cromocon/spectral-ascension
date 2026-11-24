package cromocon.spectralascension.dev;

/** Esito di un passo: riuscito o no, con una riga di dettagli leggibile da un agente. */
public record StepResult(boolean ok, String detail) {
    public static StepResult ok(String detail) {
        return new StepResult(true, detail);
    }

    public static StepResult fail(String detail) {
        return new StepResult(false, detail);
    }

    public static StepResult of(boolean ok, String detail) {
        return new StepResult(ok, detail);
    }
}
