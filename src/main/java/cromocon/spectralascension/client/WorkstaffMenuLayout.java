package cromocon.spectralascension.client;

import java.util.ArrayList;
import java.util.List;

/**
 * Geometria del menu della workstaff ({@link SAWorkstaffScreen}), senza classi del client (la prova anche il gametest
 * {@code workstaff_menu_layout} sul server). Coordinate relative al centro della schermata, y verso il basso.
 * <p>
 * Le N voci stanno sui vertici di un N-gono regolare, il primo in alto e poi in senso orario (2 voci: sinistra e
 * destra; 1 voce: in alto). Caselle come quelle di Spectrum: voci 38×38 con l'etichetta 21 px sotto il centro, centro
 * piccolo 28×28 (etichetta 20 px sotto), o il puntino 10×10 della griglia principale (senza etichetta).
 */
public final class WorkstaffMenuLayout {
    public static final int BOX = 38;
    public static final int SMALL = 28;
    public static final int DOT = 10;
    public static final int MIN_RADIUS = 60;
    public static final int MAX_RADIUS = 120;
    private static final int LINE = 9;

    private WorkstaffMenuLayout() {
    }

    /** Vertici [dx, dy] delle n voci a distanza r dal centro (arrotondati all'intero). */
    public static int[][] vertices(int n, int r) {
        int[][] v = new int[n][];
        for (int i = 0; i < n; i++) {
            double a = n == 2 ? Math.PI * (1 - i) : -Math.PI / 2 + 2 * Math.PI * i / n;
            v[i] = new int[]{(int) Math.round(r * Math.cos(a)), (int) Math.round(r * Math.sin(a))};
        }
        return v;
    }

    /**
     * Voce verso cui punta il tasto di movimento (direzione dx, dy dal centro): quella con l'angolo più piccolo, sotto
     * i 90°. -1 (si resta fermi) se non ce n'è nessuna o se due sono a pari merito (es. giù nel triangolo).
     */
    public static int pick(int[][] v, int dx, int dy) {
        int best = -1;
        double bestAngle = 90;
        boolean tie = false;
        for (int i = 0; i < v.length; i++) {
            double len = Math.hypot(v[i][0], v[i][1]);
            if (len == 0) continue;
            double angle = Math.toDegrees(Math.acos((v[i][0] * dx + v[i][1] * dy) / len / Math.hypot(dx, dy)));
            if (angle < bestAngle - 1) {
                best = i;
                bestAngle = angle;
                tie = false;
            } else if (best >= 0 && Math.abs(angle - bestAngle) <= 1) {
                tie = true;
            }
        }
        return tie ? -1 : best;
    }

    /**
     * Rettangoli {x0, y0, x1, y1} di caselle ed etichette al raggio r: labels = larghezze delle etichette delle voci,
     * centerLabel = larghezza dell'etichetta del centro piccolo, -1 = puntino senza etichetta (griglia principale).
     */
    public static List<int[]> rects(int r, int[] labels, int centerLabel) {
        List<int[]> out = new ArrayList<>();
        int[][] v = vertices(labels.length, r);
        for (int i = 0; i < v.length; i++) {
            out.add(box(v[i][0], v[i][1], BOX));
            out.add(label(v[i][0], v[i][1] + BOX / 2 + 2, labels[i]));
        }
        if (centerLabel < 0) {
            out.add(box(0, 0, DOT));
        } else {
            out.add(box(0, 0, SMALL));
            out.add(label(0, SMALL / 2 + 6, centerLabel));
        }
        return out;
    }

    /** Due rettangoli si toccano o si sovrappongono (toccarsi conta: resta almeno 1 px fra loro). */
    public static boolean overlap(List<int[]> rects) {
        for (int i = 0; i < rects.size(); i++) {
            for (int j = i + 1; j < rects.size(); j++) {
                int[] a = rects.get(i), b = rects.get(j);
                if (a[0] <= b[2] && b[0] <= a[2] && a[1] <= b[3] && b[1] <= a[3]) return true;
            }
        }
        return false;
    }

    /**
     * Il raggio più piccolo (da {@link #MIN_RADIUS}) a cui niente si tocca e tutto sta in bounds {minX, minY, maxX,
     * maxY}; -1 se nessuno va bene.
     */
    public static int radius(int[] labels, int centerLabel, int[] bounds) {
        for (int r = MIN_RADIUS; r <= MAX_RADIUS; r++) {
            List<int[]> rects = rects(r, labels, centerLabel);
            if (!overlap(rects) && rects.stream().allMatch(a -> a[0] >= bounds[0] && a[1] >= bounds[1] && a[2] <= bounds[2] && a[3] <= bounds[3])) {
                return r;
            }
        }
        return -1;
    }

    /** Fondo del rettangolo più basso (per mettere il testo dei comandi sotto le etichette). */
    public static int bottom(List<int[]> rects) {
        return rects.stream().mapToInt(a -> a[3]).max().orElse(0);
    }

    private static int[] box(int x, int y, int size) {
        return new int[]{x - size / 2, y - size / 2, x - size / 2 + size - 1, y - size / 2 + size - 1};
    }

    /** Come drawCenteredString: parte da x - w / 2; 3 px di margine ai lati, così due etichette vicine non si leggono come una. */
    private static int[] label(int x, int y, int w) {
        return new int[]{x - w / 2 - 3, y, x - w / 2 + w + 2, y + LINE - 1};
    }
}
