package uichecker.checks;

import java.awt.Color;
import java.util.*;
import java.util.regex.Pattern;

public class ContrastCheck {
    private static final Pattern RGBA = Pattern.compile("rgba?\\((\\d+),\\s*(\\d+),\\s*(\\d+)");
    private static final Pattern HEX6 = Pattern.compile("#([0-9a-fA-F]{2})([0-9a-fA-F]{2})([0-9a-fA-F]{2})");
    private static final Pattern HEX3 = Pattern.compile("#([0-9a-fA-F])([0-9a-fA-F])([0-9a-fA-F])");

    @SuppressWarnings("unchecked")
    public static Map<String, Object> check(Map<String, Object> data) {
        var details = new ArrayList<Map<String, Object>>();
        int score = 0;
        int max = 20;

        var allColors = (List<String>) data.getOrDefault("allColors", new ArrayList<>());
        var unique = new LinkedHashSet<Color>();
        for (var c : allColors) {
            var parsed = parseColor(c);
            if (parsed != null) unique.add(parsed);
        }
        var colorList = new ArrayList<>(unique);

        if (colorList.size() >= 4) {
            int good = 0;
            int total = 0;
            var white = Color.WHITE;
            var black = Color.BLACK;
            for (int i = 0; i < colorList.size(); i++) {
                for (int j = i + 1; j < colorList.size(); j++) {
                    if (contrastRatio(colorList.get(i), colorList.get(j)) >= 4.5) good++;
                    total++;
                }
                if (contrastRatio(colorList.get(i), white) >= 4.5) good++;
                total++;
                if (contrastRatio(colorList.get(i), black) >= 4.5) good++;
                total++;
            }
            double ratio = total > 0 ? (double) good / total : 0;
            if (ratio >= 0.7) { score += 10; details.add(detail(true, "Good color contrast overall", Math.round(ratio * 100) + "% pairs meet WCAG AA")); }
            else if (ratio >= 0.4) { score += 6; details.add(detail(false, "Moderate color contrast issues", Math.round(ratio * 100) + "% pairs meet WCAG AA")); }
            else { score += 2; details.add(detail(false, "Poor color contrast", Math.round(ratio * 100) + "% pairs meet WCAG AA")); }
        } else {
            score += 7; details.add(detail(true, "Limited color palette", colorList.size() + " colors found"));
        }

        if (colorList.size() <= 7) { score += 5; details.add(detail(true, "Reasonable color palette size", colorList.size() + " unique colors")); }
        else if (colorList.size() <= 12) { score += 3; details.add(detail(false, "Many colors used", colorList.size() + " unique colors, consider reducing")); }
        else { score += 1; details.add(detail(false, "Too many colors", colorList.size() + " unique colors, simplify palette")); }

        score += 5; details.add(detail(true, "Color analysis completed", null));
        return Map.of("score", Math.min(score, max), "max_score", max, "details", details);
    }

    private static Color parseColor(String s) {
        if (s == null || s.equals("transparent") || s.equals("rgba(0, 0, 0, 0)")) return null;
        var m = RGBA.matcher(s);
        if (m.find()) return new Color(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)));
        m = HEX6.matcher(s);
        if (m.matches()) return new Color(Integer.parseInt(m.group(1), 16), Integer.parseInt(m.group(2), 16), Integer.parseInt(m.group(3), 16));
        m = HEX3.matcher(s);
        if (m.matches()) return new Color(Integer.parseInt(m.group(1) + m.group(1), 16), Integer.parseInt(m.group(2) + m.group(2), 16), Integer.parseInt(m.group(3) + m.group(3), 16));
        return null;
    }

    private static double relativeLuminance(Color c) {
        double r = linearize(c.getRed() / 255.0);
        double g = linearize(c.getGreen() / 255.0);
        double b = linearize(c.getBlue() / 255.0);
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    private static double linearize(double v) {
        return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
    }

    private static double contrastRatio(Color c1, Color c2) {
        double l1 = relativeLuminance(c1);
        double l2 = relativeLuminance(c2);
        double lighter = Math.max(l1, l2);
        double darker = Math.min(l1, l2);
        return (lighter + 0.05) / (darker + 0.05);
    }

    private static Map<String, Object> detail(boolean pass, String label, String detail) {
        var m = new LinkedHashMap<String, Object>();
        m.put("pass", pass);
        m.put("label", label);
        if (detail != null) m.put("detail", detail);
        return m;
    }
}
