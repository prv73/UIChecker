package uichecker.checks;

import java.util.*;

public class LayoutCheck {
    @SuppressWarnings("unchecked")
    public static Map<String, Object> check(Map<String, Object> data) {
        var details = new ArrayList<Map<String, Object>>();
        int score = 0;
        int max = 20;

        int textLen = ((Number) data.getOrDefault("textLength", 0)).intValue();
        int elemCount = ((Number) data.getOrDefault("elementCount", 0)).intValue();
        int bodyChildren = ((Number) data.getOrDefault("bodyChildren", 0)).intValue();
        var headings = (List<Map<String, Object>>) data.getOrDefault("headingLevels", new ArrayList<>());

        double density = elemCount > 0 ? (double) textLen / elemCount : 0;

        if (density > 0 && density < 200) { score += 5; details.add(detail(true, "Good content density", Math.round(density) + " characters per element", null)); }
        else if (density >= 200) { score += 2; details.add(detail(false, "High content density", Math.round(density) + " characters per element", "Break long text into paragraphs with headings, lists, and shorter sections")); }
        else { score += 3; details.add(detail(true, "Content structure looks reasonable", null, null)); }

        if (bodyChildren <= 15) { score += 5; details.add(detail(true, "Well-organized top-level structure", bodyChildren + " direct children in body", null)); }
        else if (bodyChildren <= 25) { score += 3; details.add(detail(false, "Somewhat flat structure", bodyChildren + " direct children in body", "Group related elements into <section>, <div>, or <nav> containers")); }
        else { score += 1; details.add(detail(false, "Very flat structure", bodyChildren + " direct children in body", "Restructure the page with nested containers instead of a flat layout")); }

        if (!headings.isEmpty()) {
            int sectionCount = Math.max(1, textLen / 500);
            int hCount = headings.size();
            if (hCount >= sectionCount * 0.5) { score += 5; details.add(detail(true, "Content is well-sectioned", hCount + " headings for content sections", null)); }
            else { score += 3; details.add(detail(false, "More headings would help", hCount + " headings for " + sectionCount + " estimated sections", "Add more headings to divide content into logical, scannable sections")); }
        } else {
            score += 1; details.add(detail(false, "No headings to structure content", null, "Add headings (H1-H6) to create a clear content hierarchy"));
        }

        score += 5; details.add(detail(true, "Layout analysis completed", null, null));
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
