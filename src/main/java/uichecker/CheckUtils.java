package uichecker;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.Map;

public class CheckUtils {

    public static Map<String, Object> detail(boolean pass, String label, String detail, String suggestion) {
        var m = new LinkedHashMap<String, Object>();
        m.put("pass", pass);
        m.put("label", label);
        if (detail != null) m.put("detail", detail);
        if (suggestion != null) m.put("suggestion", suggestion);
        return m;
    }

    public static double relativeLuminance(Color c) {
        double r = linearize(c.getRed() / 255.0);
        double g = linearize(c.getGreen() / 255.0);
        double b = linearize(c.getBlue() / 255.0);
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    public static double linearize(double v) {
        return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
    }
}
