package cromocon.spectralascension.dev;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Copia in memoria le righe di log (ultime 20000) per assert_log: logs/latest.log è scritto con un buffer, quindi
 * leggerlo durante la partita non è affidabile.
 */
public final class LogCapture extends AbstractAppender {
    private static final int MAX = 20000;
    private static final Deque<Line> LINES = new ArrayDeque<>();
    private static long seq;

    record Line(long n, String text) {
    }

    private LogCapture() {
        super("SAScenarioCapture", null, null, true, Property.EMPTY_ARRAY);
    }

    public static void install() {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        LogCapture app = new LogCapture();
        app.start();
        ctx.getConfiguration().addAppender(app);
        ctx.getRootLogger().addAppender(app);
    }

    @Override
    public void append(LogEvent e) {
        String text = "[" + e.getThreadName() + "/" + e.getLevel() + "] [" + e.getLoggerName() + "]: " + e.getMessage().getFormattedMessage();
        synchronized (LINES) {
            LINES.addLast(new Line(++seq, text));
            if (LINES.size() > MAX) LINES.removeFirst();
        }
    }

    /** Numero dell'ultima riga registrata (punto di partenza di uno scenario). */
    public static long mark() {
        synchronized (LINES) {
            return seq;
        }
    }

    /** Righe registrate dopo {@code from}. */
    public static List<String> since(long from) {
        List<String> out = new ArrayList<>();
        synchronized (LINES) {
            for (Line l : LINES) if (l.n() > from) out.add(l.text());
        }
        return out;
    }
}
