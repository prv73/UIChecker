package uichecker.checks;

import java.util.*;
import static uichecker.CheckUtils.detail;

public class ResponsivenessCheck {
    @SuppressWarnings("unchecked")
    public static Map<String, Object> check(Map<String, Object> data, Map<String, Object> perf) {
        var details = new ArrayList<Map<String, Object>>();
        int score = 0;
        int max = 20;

        var vp = (String) data.get("viewportMeta");
        boolean hasMQ = Boolean.TRUE.equals(data.get("hasMediaQueries"));
        var images = (List<Map<String, Object>>) data.getOrDefault("images", new ArrayList<>());
        long loadTime = ((Number) perf.getOrDefault("load_time", 0)).longValue();
        long totalSize = ((Number) perf.getOrDefault("total_size", 0)).longValue();

        if (vp != null) {
            score += 5;
            if (vp.contains("width=device-width") && vp.contains("initial-scale")) {
                score += 3;
                details.add(detail(true, "Proper viewport configuration", "width=device-width, initial-scale", null));
            } else {
                details.add(detail(true, "Viewport meta tag present", vp, null));
            }
        } else {
            score += 1;
            details.add(detail(false, "Missing viewport meta tag", null, "Add <meta name='viewport' content='width=device-width, initial-scale=1'> to enable mobile-friendly rendering"));
        }

        if (hasMQ) { score += 5; details.add(detail(true, "Uses responsive media queries", "CSS media queries detected", null)); }
        else { score += 0; details.add(detail(false, "No media queries found", null, "Add CSS @media breakpoints for mobile, tablet, and desktop screen sizes")); }

        long largeImgs = images.stream().filter(img -> ((Number) img.getOrDefault("width", 0)).doubleValue() > 1000).count();
        if (largeImgs == 0) { score += 3; details.add(detail(true, "No overly large images", "Good for responsive design", null)); }
        else { score += 1; details.add(detail(false, "Large images found", largeImgs + " image(s) wider than 1000px", "Use responsive images with srcset and sizes attributes, or serve appropriately sized images per breakpoint")); }

        if (loadTime < 2000) { score += 4; details.add(detail(true, "Fast page load", loadTime + "ms", null)); }
        else if (loadTime < 5000) { score += 2; details.add(detail(false, "Moderate load time", loadTime + "ms (target < 2s)", "Optimize: compress images, minify CSS/JS, enable browser caching")); }
        else { score += 0; details.add(detail(false, "Slow page load", loadTime + "ms (target < 2s)", "Reduce server response time, lazy-load images, defer JavaScript, use a CDN")); }

        if (totalSize > 0) {
            long kb = totalSize / 1024;
            if (kb < 1000) { score += 3; details.add(detail(true, "Reasonable page size", kb + "KB", null)); }
            else { score += 0; details.add(detail(false, "Large page size", kb + "KB (target < 1MB)", "Reduce page weight: compress resources, remove unused CSS/JS, optimize images")); }
        }

        return Map.of("score", Math.min(score, max), "max_score", max, "details", details);
    }

}
