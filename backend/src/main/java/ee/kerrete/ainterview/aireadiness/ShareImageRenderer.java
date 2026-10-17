package ee.kerrete.ainterview.aireadiness;

import ee.kerrete.ainterview.aireadiness.AssessmentModels.AssessmentResult;
import ee.kerrete.ainterview.aireadiness.AssessmentModels.RiskBand;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Jagamispilt 1200×630 (LinkedIni, Slacki jt eelvaade): isiku asendatavuse skoor, amet ja riskiaste, või
 * üldine kutse testile, kui tulemust pole. Värvid ja astmed on samad mis tulemuse lehel. Konteineris on vaja
 * fonte (Dockerfile: fontconfig + ttf-dejavu).
 */
@Component
public class ShareImageRenderer {

    static final int WIDTH = 1200;
    static final int HEIGHT = 630;

    private static final Color CANVAS = new Color(0x0b0d12);
    private static final Color FG = new Color(0xeef0f3);
    private static final Color MUTED = new Color(0x9aa2b1);
    private static final Color LINE = new Color(0x252a35);
    private static final Color ACCENT = new Color(0xc6f432);
    private static final Map<RiskBand, Color> BAND_COLORS = Map.of(
            RiskBand.LOW, new Color(0x3ddc97), RiskBand.MODERATE, new Color(0xffd23f),
            RiskBand.HIGH, new Color(0xff8c42), RiskBand.VERY_HIGH, new Color(0xff4d6d));
    private static final Map<RiskBand, String> BAND_LABELS = Map.of(
            RiskBand.LOW, "Low exposure", RiskBand.MODERATE, "Moderate exposure",
            RiskBand.HIGH, "High exposure", RiskBand.VERY_HIGH, "Very high exposure");
    private static final String FAMILY = "DejaVu Sans";
    private static final String MONO = "DejaVu Sans Mono";

    private volatile byte[] generic;

    /** Isiku tulemuse pilt. */
    public byte[] render(AssessmentResult r) {
        BufferedImage img = canvas();
        Graphics2D g = graphics(img);
        try {
            header(g);
            Color band = BAND_COLORS.getOrDefault(r.band(), ACCENT);

            g.setFont(new Font(MONO, Font.PLAIN, 22));
            g.setColor(MUTED);
            g.drawString("REPLACEABILITY BY AI", 80, 168);

            g.setFont(new Font(FAMILY, Font.BOLD, 210));
            g.setColor(band);
            String score = String.valueOf(r.replaceabilityScore());
            g.drawString(score, 70, 360);
            int scoreWidth = g.getFontMetrics().stringWidth(score);
            g.setFont(new Font(FAMILY, Font.BOLD, 56));
            g.setColor(MUTED);
            g.drawString("/100", 80 + scoreWidth, 360);

            int x = 640;
            g.setFont(new Font(FAMILY, Font.BOLD, 46));
            g.setColor(FG);
            int y = 196;
            for (String line : wrap(g.getFontMetrics(), r.professionName(), 480, 3)) {
                g.drawString(line, x, y);
                y += 56;
            }
            pill(g, BAND_LABELS.getOrDefault(r.band(), ""), band, x, y + 6);

            scale(g, r.replaceabilityScore(), 80, 440, 1040);

            g.setFont(new Font(FAMILY, Font.BOLD, 34));
            g.setColor(FG);
            g.drawString("What's yours?", 80, 560);
            g.setFont(new Font(FAMILY, Font.PLAIN, 30));
            g.setColor(ACCENT);
            String cta = "Free 3-minute test → careerrisk.ee";
            g.drawString(cta, WIDTH - 80 - g.getFontMetrics().stringWidth(cta), 560);
        } finally {
            g.dispose();
        }
        return png(img);
    }

    /** Üldine pilt, kui link on vigane või päringuid on liiga palju. Arvutatakse üks kord. */
    public byte[] generic() {
        byte[] cached = generic;
        if (cached != null) {
            return cached;
        }
        BufferedImage img = canvas();
        Graphics2D g = graphics(img);
        try {
            header(g);
            g.setFont(new Font(FAMILY, Font.BOLD, 76));
            g.setColor(FG);
            g.drawString("How much of your job", 80, 270);
            g.drawString("can AI already do?", 80, 360);
            g.setFont(new Font(FAMILY, Font.PLAIN, 34));
            g.setColor(MUTED);
            g.drawString("Task by task, for 900+ jobs. Free, 3 minutes, no account.", 80, 430);
            g.setFont(new Font(FAMILY, Font.BOLD, 34));
            g.setColor(ACCENT);
            g.drawString("careerrisk.ee", 80, 540);
        } finally {
            g.dispose();
        }
        generic = png(img);
        return generic;
    }

    private static BufferedImage canvas() {
        return new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
    }

    private static Graphics2D graphics(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setColor(CANVAS);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        return g;
    }

    private static void header(Graphics2D g) {
        g.setColor(ACCENT);
        g.fill(new Ellipse2D.Double(80, 66, 16, 16));
        g.setFont(new Font(FAMILY, Font.BOLD, 30));
        g.setColor(FG);
        g.drawString("careerrisk.ee", 106, 85);
    }

    private static void pill(Graphics2D g, String text, Color color, int x, int y) {
        if (text.isEmpty()) {
            return;
        }
        g.setFont(new Font(FAMILY, Font.BOLD, 26));
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(text) + 40;
        g.setColor(color);
        g.setStroke(new BasicStroke(2.5f));
        g.draw(new RoundRectangle2D.Double(x, y, w, 50, 50, 50));
        g.drawString(text, x + 20, y + 34);
    }

    /** Skaala 0–100 astmete värvides (piirid 30, 50, 70) ja marker skoori kohal. */
    private static void scale(Graphics2D g, int score, int x, int y, int width) {
        int[] edges = {0, 30, 50, 70, 100};
        RiskBand[] bands = {RiskBand.LOW, RiskBand.MODERATE, RiskBand.HIGH, RiskBand.VERY_HIGH};
        for (int i = 0; i < bands.length; i++) {
            int from = x + width * edges[i] / 100;
            int to = x + width * edges[i + 1] / 100;
            g.setColor(BAND_COLORS.get(bands[i]));
            g.fillRect(from + (i == 0 ? 0 : 3), y, to - from - (i == 0 ? 0 : 3), 16);
        }
        int mx = x + width * Math.max(0, Math.min(100, score)) / 100;
        g.setColor(FG);
        g.fillRect(mx - 3, y - 12, 6, 40);
        g.setColor(LINE);
        g.drawLine(x, y + 44, x + width, y + 44);
    }

    /** Murrab teksti ridadeks laiusega maxWidth; viimane rida lõpeb "…", kui tekst ei mahu. */
    static List<String> wrap(FontMetrics fm, String text, int maxWidth, int maxLines) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split("\\s+")) {
            String next = line.isEmpty() ? word : line + " " + word;
            if (fm.stringWidth(next) <= maxWidth || line.isEmpty()) {
                line = new StringBuilder(next);
            } else {
                lines.add(line.toString());
                line = new StringBuilder(word);
            }
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        if (lines.size() > maxLines) {
            List<String> cut = new ArrayList<>(lines.subList(0, maxLines));
            String last = cut.get(maxLines - 1);
            while (!last.isEmpty() && fm.stringWidth(last + "…") > maxWidth) {
                last = last.substring(0, last.length() - 1);
            }
            cut.set(maxLines - 1, last.stripTrailing() + "…");
            return cut;
        }
        return lines;
    }

    private static byte[] png(BufferedImage img) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(img, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
