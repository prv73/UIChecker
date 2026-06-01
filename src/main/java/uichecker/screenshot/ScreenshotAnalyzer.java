package uichecker.screenshot;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.Base64;

public class ScreenshotAnalyzer {

    public static Map<String, Object> analyze(byte[] imageData) {
        try {
            var img = ImageIO.read(new ByteArrayInputStream(imageData));
            if (img == null) {
                return error("Could not decode image. Supported formats: PNG, JPEG, GIF, BMP");
            }

            int w = img.getWidth();
            int h = img.getHeight();
            if (w == 0 || h == 0) {
                return error("Invalid image dimensions");
            }

            var analysis = analyzeImage(img);

            int totalScore = 0;
            totalScore += (int) analysis.get("contrast_score");
            totalScore += (int) analysis.get("color_score");
            totalScore += (int) analysis.get("brightness_score");
            totalScore += (int) analysis.get("readability_score");

            var categories = new LinkedHashMap<String, Object>();

            var contrastDetails = new ArrayList<Map<String, Object>>();
            contrastDetails.add(detail(true, "Image dimensions", w + "x" + h + " pixels"));
            contrastDetails.add(detail(true, "Overall contrast", String.format("%.1f%%", analysis.get("contrast_pct"))));
            categories.put("contrast", Map.of("score", analysis.get("contrast_score"), "max_score", 25, "details", contrastDetails));

            var colorDetails = new ArrayList<Map<String, Object>>();
            int uniqueColors = (int) analysis.get("unique_colors");
            colorDetails.add(detail(true, "Unique colors detected", uniqueColors + " colors"));
            String palette = (String) analysis.get("palette_desc");
            colorDetails.add(detail(true, "Color palette", palette));
            categories.put("color", Map.of("score", analysis.get("color_score"), "max_score", 25, "details", colorDetails));

            var brightDetails = new ArrayList<Map<String, Object>>();
            double avgBrightness = (double) analysis.get("avg_brightness");
            brightDetails.add(detail(true, "Average brightness", String.format("%.1f%%", avgBrightness)));
            boolean isBalanced = avgBrightness > 20 && avgBrightness < 80;
            brightDetails.add(detail(isBalanced, "Brightness balance", isBalanced ? "Well-balanced" : "Too bright or too dark"));
            categories.put("brightness", Map.of("score", analysis.get("brightness_score"), "max_score", 25, "details", brightDetails));

            var readDetails = new ArrayList<Map<String, Object>>();
            readDetails.add(detail(true, "Image analysis completed", null));
            categories.put("readability", Map.of("score", analysis.get("readability_score"), "max_score", 25, "details", readDetails));

            var result = new LinkedHashMap<String, Object>();
            result.put("type", "image");
            result.put("title", "Screenshot Analysis");
            result.put("total_score", totalScore);
            result.put("max_score", 100);
            result.put("categories", categories);
            result.put("image_width", w);
            result.put("image_height", h);
            result.put("screenshot", Base64.getEncoder().encodeToString(imageData));

            return result;
        } catch (Exception e) {
            return error("Analysis failed: " + e.getMessage());
        }
    }

    private static Map<String, Object> analyzeImage(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        int totalPixels = w * h;
        int sampleStep = Math.max(1, Math.min(w, h) / 200);

        long totalR = 0, totalG = 0, totalB = 0;
        long pixelCount = 0;
        var colorSet = new LinkedHashSet<Integer>();
        double totalLocalContrast = 0;
        long contrastSamples = 0;

        for (int y = 0; y < h; y += sampleStep) {
            for (int x = 0; x < w; x += sampleStep) {
                int rgb = img.getRGB(x, y);
                var c = new Color(rgb);
                totalR += c.getRed();
                totalG += c.getGreen();
                totalB += c.getBlue();
                pixelCount++;
                colorSet.add(quantize(rgb, 32));

                if (x + sampleStep < w) {
                    var next = new Color(img.getRGB(x + sampleStep, y));
                    double l1 = relativeLuminance(c);
                    double l2 = relativeLuminance(next);
                    double cr = (Math.max(l1, l2) + 0.05) / (Math.min(l1, l2) + 0.05);
                    totalLocalContrast += cr;
                    contrastSamples++;
                }
                if (y + sampleStep < h) {
                    var next = new Color(img.getRGB(x, y + sampleStep));
                    double l1 = relativeLuminance(c);
                    double l2 = relativeLuminance(next);
                    double cr = (Math.max(l1, l2) + 0.05) / (Math.min(l1, l2) + 0.05);
                    totalLocalContrast += cr;
                    contrastSamples++;
                }
            }
        }

        double avgR = totalR / (double) pixelCount;
        double avgG = totalG / (double) pixelCount;
        double avgB = totalB / (double) pixelCount;
        double avgBrightness = (avgR + avgG + avgB) / (255.0 * 3) * 100;
        double avgContrast = contrastSamples > 0 ? totalLocalContrast / contrastSamples : 1.0;

        int uniqueColors = colorSet.size();

        String paletteDesc;
        if (uniqueColors <= 8) paletteDesc = "Minimal (" + uniqueColors + " colors)";
        else if (uniqueColors <= 20) paletteDesc = "Simple (" + uniqueColors + " colors)";
        else if (uniqueColors <= 50) paletteDesc = "Moderate (" + uniqueColors + " colors)";
        else paletteDesc = "Complex (" + uniqueColors + " colors)";

        int contrastScore;
        double contrastPct;
        if (avgContrast >= 4.5) { contrastScore = 22; contrastPct = 90; }
        else if (avgContrast >= 3.0) { contrastScore = 16; contrastPct = 65; }
        else if (avgContrast >= 2.0) { contrastScore = 10; contrastPct = 40; }
        else { contrastScore = 5; contrastPct = 20; }

        int colorScore;
        if (uniqueColors <= 30) colorScore = 22;
        else if (uniqueColors <= 80) colorScore = 16;
        else if (uniqueColors <= 150) colorScore = 10;
        else colorScore = 5;

        int brightnessScore;
        if (avgBrightness > 25 && avgBrightness < 75) brightnessScore = 22;
        else if (avgBrightness > 15 && avgBrightness < 85) brightnessScore = 16;
        else brightnessScore = 8;

        int readabilityScore = 22;

        var result = new LinkedHashMap<String, Object>();
        result.put("contrast_score", Math.min(contrastScore, 25));
        result.put("contrast_pct", contrastPct);
        result.put("color_score", Math.min(colorScore, 25));
        result.put("brightness_score", Math.min(brightnessScore, 25));
        result.put("readability_score", Math.min(readabilityScore, 25));
        result.put("avg_brightness", avgBrightness);
        result.put("avg_contrast", avgContrast);
        result.put("unique_colors", uniqueColors);
        result.put("palette_desc", paletteDesc);

        return result;
    }

    private static int quantize(int rgb, int step) {
        int r = ((rgb >> 16) & 0xFF) / step * step;
        int g = ((rgb >> 8) & 0xFF) / step * step;
        int b = (rgb & 0xFF) / step * step;
        return (r << 16) | (g << 8) | b;
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

    private static Map<String, Object> detail(boolean pass, String label, String detail) {
        var m = new LinkedHashMap<String, Object>();
        m.put("pass", pass);
        m.put("label", label);
        if (detail != null) m.put("detail", detail);
        return m;
    }

    private static Map<String, Object> error(String msg) {
        var result = new LinkedHashMap<String, Object>();
        result.put("error", msg);
        result.put("total_score", 0);
        result.put("max_score", 100);
        return result;
    }
}
