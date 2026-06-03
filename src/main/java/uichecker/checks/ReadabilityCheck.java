package uichecker.checks;

import java.util.*;

public class ReadabilityCheck {
    @SuppressWarnings("unchecked")
    public static Map<String, Object> check(Map<String, Object> data) {
        var details = new ArrayList<Map<String, Object>>();
        int score = 0;
        int max = 20;

        var fontSizes = ((List<Number>) data.getOrDefault("allFontSizes", new ArrayList<>()))
                .stream().mapToDouble(Number::doubleValue).filter(s -> s > 0 && s < 100).toArray();
        var lineHeights = ((List<Number>) data.getOrDefault("allLineHeights", new ArrayList<>()))
                .stream().mapToDouble(Number::doubleValue).toArray();
        var headings = (List<Map<String, Object>>) data.getOrDefault("headingLevels", new ArrayList<>());

        double avg = fontSizes.length > 0 ? Arrays.stream(fontSizes).average().orElse(0) : 0;
        double min = fontSizes.length > 0 ? Arrays.stream(fontSizes).min().orElse(0) : 0;

        if (avg >= 16) { score += 6; details.add(detail(true, "Average font size is adequate", String.format("%.1fpx", avg), null)); }
        else if (avg >= 14) { score += 4; details.add(detail(false, "Average font size could be larger", String.format("%.1fpx (recommend >= 16px)", avg), "Increase base font size to 16px or larger for body text")); }
        else { score += 2; details.add(detail(false, "Text is too small", String.format("%.1fpx (recommend >= 16px)", avg), "Set body font-size to at least 16px for comfortable reading")); }

        if (min >= 12) { score += 3; details.add(detail(true, "Minimum font size is readable", String.format("%.1fpx", min), null)); }
        else { score += 1; details.add(detail(false, "Some text is very small", String.format("%.1fpx (minimum 12px)", min), "Ensure no text is smaller than 12px, especially in footers and disclaimers")); }

        if (lineHeights.length > 0) {
            double avgLh = Arrays.stream(lineHeights).average().orElse(0);
            if (avgLh >= 1.5) { score += 5; details.add(detail(true, "Line spacing is comfortable", String.format("%.2fx", avgLh), null)); }
            else if (avgLh >= 1.3) { score += 3; details.add(detail(false, "Line spacing could be improved", String.format("%.2fx (recommend >= 1.5x)", avgLh), "Set line-height to 1.5x or more for comfortable reading")); }
            else { score += 1; details.add(detail(false, "Line spacing is too tight", String.format("%.2fx (recommend >= 1.5x)", avgLh), "Increase line-height to at least 1.5x to improve readability")); }
        } else {
            score += 3; details.add(detail(true, "No text elements to check", null, null));
        }

        if (!headings.isEmpty()) {
            boolean hasH1 = headings.stream().anyMatch(h -> ((Number) h.get("level")).intValue() == 1);
            var levels = headings.stream().mapToInt(h -> ((Number) h.get("level")).intValue()).toArray();
            boolean seq = true;
            for (int i = 1; i < levels.length; i++) {
                if (levels[i] > levels[i-1] + 1) { seq = false; break; }
            }
            if (hasH1) { score += 3; details.add(detail(true, "Page has a main heading (H1)", null, null)); }
            else { score += 1; details.add(detail(false, "Missing H1 heading", null, "Add a single H1 heading that describes the page content")); }
            if (seq && headings.size() > 0) { score += 3; details.add(detail(true, "Heading hierarchy is logical", "H1 -> H2 -> H3...", null)); }
            else { score += 1; details.add(detail(false, "Heading hierarchy skips levels", null, "Don't skip heading levels (e.g., don't jump from H2 to H4)")); }
        } else {
            score += 2; details.add(detail(false, "No headings found", "Use headings to structure content", "Add heading elements (H1-H6) to structure your content"));        }

        return Map.of("score", Math.min(score, max), "max_score", max, "details", details);
    }

    private static Map<String, Object> detail(boolean pass, String label, String detail, String suggestion) {
        var m = new LinkedHashMap<String, Object>();
        m.put("pass", pass);
        m.put("label", label);
        if (detail != null) m.put("detail", detail);
        if (suggestion != null) m.put("suggestion", suggestion);
        return m;
    }
}
