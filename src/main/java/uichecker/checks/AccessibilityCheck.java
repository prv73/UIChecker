package uichecker.checks;

import java.util.*;

public class AccessibilityCheck {
    @SuppressWarnings("unchecked")
    public static Map<String, Object> check(Map<String, Object> data) {
        var details = new ArrayList<Map<String, Object>>();
        int score = 0;
        int max = 20;

        var images = (List<Map<String, Object>>) data.getOrDefault("images", new ArrayList<>());
        var interactive = (List<Map<String, Object>>) data.getOrDefault("interactiveElements", new ArrayList<>());
        var semantic = (List<Map<String, Object>>) data.getOrDefault("semanticElements", new ArrayList<>());
        var headings = (List<Map<String, Object>>) data.getOrDefault("headingLevels", new ArrayList<>());
        var lang = (String) data.get("langAttr");
        boolean doctype = Boolean.TRUE.equals(data.get("hasDoctype"));

        if (!images.isEmpty()) {
            long withAlt = images.stream().filter(img ->
                    Boolean.TRUE.equals(img.get("hasAlt"))
                    && img.get("altText") != null
                    && !((String) img.get("altText")).isBlank()).count();
            double ratio = (double) withAlt / images.size();
            if (ratio >= 0.9) { score += 5; details.add(detail(true, "Images have descriptive alt text", withAlt + "/" + images.size())); }
            else if (ratio >= 0.5) { score += 3; details.add(detail(false, "Some images missing alt text", withAlt + "/" + images.size())); }
            else { score += 1; details.add(detail(false, "Most images lack alt text", withAlt + "/" + images.size())); }
        } else {
            score += 5; details.add(detail(true, "No images to check", null));
        }

        if (!interactive.isEmpty()) {
            long labeled = interactive.stream().filter(el ->
                    Boolean.TRUE.equals(el.get("hasAriaLabel"))
                    || Boolean.TRUE.equals(el.get("hasAriaLabelledby"))
                    || el.get("associatedLabel") != null
                    || ("A".equals(el.get("tag")) && el.get("text") != null && !((String) el.get("text")).isBlank())
            ).count();
            double ratio = (double) labeled / interactive.size();
            if (ratio >= 0.9) { score += 4; details.add(detail(true, "Interactive elements are labeled", labeled + "/" + interactive.size())); }
            else if (ratio >= 0.6) { score += 2; details.add(detail(false, "Some interactive elements lack labels", labeled + "/" + interactive.size())); }
            else { score += 0; details.add(detail(false, "Many interactive elements lack labels", labeled + "/" + interactive.size())); }
        } else {
            score += 4; details.add(detail(true, "No interactive elements to check", null));
        }

        if (!semantic.isEmpty()) {
            int count = semantic.stream().mapToInt(s -> ((Number) s.get("count")).intValue()).sum();
            if (count >= 4) { score += 4; details.add(detail(true, "Good use of semantic HTML", count + " semantic elements found")); }
            else { score += 2; details.add(detail(false, "Limited semantic HTML", "Add <nav>, <main>, <header>, <footer>")); }
        } else {
            score += 0; details.add(detail(false, "No semantic HTML elements", "Use <nav>, <main>, <header>, <footer>, <article>"));
        }

        if (!headings.isEmpty()) {
            long h1Count = headings.stream().filter(h -> ((Number) h.get("level")).intValue() == 1).count();
            if (h1Count == 1) { score += 3; details.add(detail(true, "Exactly one H1 heading", null)); }
            else if (h1Count == 0) { score += 1; details.add(detail(false, "Missing H1 heading", null)); }
            else { score += 1; details.add(detail(false, "Multiple H1 headings (" + h1Count + ")", "Should have only one H1")); }
        } else {
            score += 0; details.add(detail(false, "No heading structure", null));
        }

        if (lang != null && !lang.isBlank()) { score += 2; details.add(detail(true, "Language attribute set", "lang=\"" + lang + "\"")); }
        else { score += 0; details.add(detail(false, "Missing lang attribute", "Add lang to <html>")); }

        if (doctype) { score += 2; details.add(detail(true, "DOCTYPE declared", "Standards mode enabled")); }
        else { score += 0; details.add(detail(false, "Missing DOCTYPE", "Add <!DOCTYPE html>")); }

        return Map.of("score", Math.min(score, max), "max_score", max, "details", details);
    }

    private static Map<String, Object> detail(boolean pass, String label, String detail) {
        var m = new LinkedHashMap<String, Object>();
        m.put("pass", pass);
        m.put("label", label);
        if (detail != null) m.put("detail", detail);
        return m;
    }
}
