package uichecker.checks;

import java.util.*;
import static uichecker.CheckUtils.detail;

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
            if (ratio >= 0.9) { score += 5; details.add(detail(true, "Images have descriptive alt text", withAlt + "/" + images.size() + " images have alt", null)); }
            else if (ratio >= 0.5) { score += 3; details.add(detail(false, "Some images missing alt text", withAlt + "/" + images.size() + " images have alt", "Add descriptive alt text to all images for screen reader compatibility")); }
            else { score += 1; details.add(detail(false, "Most images lack alt text", withAlt + "/" + images.size() + " images have alt", "Every <img> needs an alt attribute describing its content or purpose")); }
        } else {
            score += 5; details.add(detail(true, "No images to check", null, null));
        }

        if (!interactive.isEmpty()) {
            long labeled = interactive.stream().filter(el ->
                    Boolean.TRUE.equals(el.get("hasAriaLabel"))
                    || Boolean.TRUE.equals(el.get("hasAriaLabelledby"))
                    || el.get("associatedLabel") != null
                    || ("A".equals(el.get("tag")) && el.get("text") != null && !((String) el.get("text")).isBlank())
            ).count();
            double ratio = (double) labeled / interactive.size();
            if (ratio >= 0.9) { score += 4; details.add(detail(true, "Interactive elements are labeled", labeled + "/" + interactive.size() + " have labels", null)); }
            else if (ratio >= 0.6) { score += 2; details.add(detail(false, "Some interactive elements lack labels", labeled + "/" + interactive.size() + " have labels", "Add aria-label or associate a <label> element to each form control")); }
            else { score += 0; details.add(detail(false, "Many interactive elements lack labels", labeled + "/" + interactive.size() + " have labels", "Every button, input, and link needs an accessible name via aria-label, aria-labelledby, or a wrapping <label>")); }
        } else {
            score += 4; details.add(detail(true, "No interactive elements to check", null, null));
        }

        if (!semantic.isEmpty()) {
            int count = semantic.stream().mapToInt(s -> ((Number) s.get("count")).intValue()).sum();
            if (count >= 4) { score += 4; details.add(detail(true, "Good use of semantic HTML", count + " semantic elements found", null)); }
            else { score += 2; details.add(detail(false, "Limited semantic HTML", count + " semantic elements found", "Use <nav>, <main>, <header>, <footer>, <article> for better structure and screen reader navigation")); }
        } else {
            score += 0; details.add(detail(false, "No semantic HTML elements", null, "Use structural elements like <nav>, <main>, <header>, <footer>, <article>, <section>"));
        }

        if (!headings.isEmpty()) {
            long h1Count = headings.stream().filter(h -> ((Number) h.get("level")).intValue() == 1).count();
            if (h1Count == 1) { score += 3; details.add(detail(true, "Exactly one H1 heading", null, null)); }
            else if (h1Count == 0) { score += 1; details.add(detail(false, "Missing H1 heading", null, "Add one H1 heading that summarizes the page content")); }
            else { score += 1; details.add(detail(false, "Multiple H1 headings (" + h1Count + ")", null, "A page should have only one H1 heading; demote extras to H2")); }
        } else {
            score += 0; details.add(detail(false, "No heading structure", null, "Add heading elements (H1-H6) to create a content outline"));
        }

        if (lang != null && !lang.isBlank()) { score += 2; details.add(detail(true, "Language attribute set", "lang=\"" + lang + "\"", null)); }
        else { score += 0; details.add(detail(false, "Missing lang attribute", null, "Add lang=\"en\" (or your page's language) to the <html> tag for screen readers")); }

        if (doctype) { score += 2; details.add(detail(true, "DOCTYPE declared", "Standards mode enabled", null)); }
        else { score += 0; details.add(detail(false, "Missing DOCTYPE", null, "Add <!DOCTYPE html> at the top of your HTML to enable standards mode")); }

        return Map.of("score", Math.min(score, max), "max_score", max, "details", details);
    }

}
