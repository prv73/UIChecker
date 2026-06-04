import io
import math
import base64
from PIL import Image


def linearize(c):
    c = c / 255.0
    return c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4


def relative_luminance(r, g, b):
    return 0.2126 * linearize(r) + 0.7152 * linearize(g) + 0.0722 * linearize(b)


def detail(pass_, label, detail_text=None, suggestion=None):
    d = {"pass": pass_, "label": label}
    if detail_text:
        d["detail"] = detail_text
    if suggestion:
        d["suggestion"] = suggestion
    return d


def analyze(image_bytes):
    img = Image.open(io.BytesIO(image_bytes)).convert("RGB")
    w, h = img.size

    step = max(1, min(w, h) // 60)
    pixels = []
    for y in range(0, h, step):
        for x in range(0, w, step):
            pixels.append(img.getpixel((x, y)))

    total_pixels = len(pixels)
    if total_pixels == 0:
        return empty_result(image_bytes, img)

    # Contrast: local contrast ratios between adjacent sampled pixels
    ratios = []
    for i in range(1, len(pixels)):
        r1, g1, b1 = pixels[i - 1]
        r2, g2, b2 = pixels[i]
        l1 = relative_luminance(r1, g1, b1)
        l2 = relative_luminance(r2, g2, b2)
        lighter = max(l1, l2)
        darker = min(l1, l2)
        if darker > 0.001:
            ratios.append((lighter + 0.05) / (darker + 0.05))

    avg_contrast = sum(ratios) / len(ratios) if ratios else 1.0

    # Color palette: quantized unique colors (8-bit per channel quantization)
    unique = set()
    for r, g, b in pixels:
        unique.add(((r // 32) << 16) | ((g // 32) << 8) | (b // 32))
    num_colors = len(unique)

    # Brightness balance
    bright = sum(1 for r, g, b in pixels if relative_luminance(r, g, b) > 0.5)
    bright_ratio = bright / total_pixels

    # Scoring
    contrast_score = min(10, max(0, int(avg_contrast * 1.8)))
    color_score = min(10, max(0, 10 - abs(num_colors - 8) // 2))
    brightness_score = min(10, max(0, 10 - int(abs(bright_ratio - 0.5) * 18)))

    total_score = contrast_score + color_score + brightness_score

    buf = io.BytesIO()
    img.save(buf, format="PNG")
    ss_b64 = base64.b64encode(buf.getvalue()).decode()

    contrast_details = [
        detail(avg_contrast >= 2.5, "Local contrast ratio", f"{avg_contrast:.1f}:1",
               "Increase contrast between adjacent elements for better readability" if avg_contrast < 2.5 else None),
        detail(avg_contrast >= 4.0, "Strong contrast areas", "WCAG AA minimum is 4.5:1",
               "Ensure text and UI elements meet WCAG AA contrast requirements" if avg_contrast < 4.0 else None),
    ]

    palette_details = [
        detail(num_colors <= 15, "Color palette size", f"{num_colors} colors",
               "Reduce palette to 15 or fewer distinct colors for a cleaner design" if num_colors > 15 else None),
        detail(num_colors >= 3, "Sufficient color variety", f"{num_colors} colors",
               "Add more color variation to avoid a monotonous design" if num_colors < 3 else None),
    ]

    balance_details = [
        detail(0.25 <= bright_ratio <= 0.75, "Brightness balance", f"{bright_ratio * 100:.0f}% light",
               "Aim for a balanced mix of light and dark areas" if bright_ratio < 0.25 or bright_ratio > 0.75 else None),
        detail(bright_ratio > 0.05, "Not overly dark", f"{bright_ratio * 100:.0f}% light pixels",
               "The image appears very dark; consider lightening some areas" if bright_ratio <= 0.05 else None),
    ]

    categories = {
        "contrast": {"score": contrast_score, "max_score": 10, "details": contrast_details},
        "color": {"score": color_score, "max_score": 10, "details": palette_details},
        "brightness": {"score": brightness_score, "max_score": 10, "details": balance_details},
    }

    return {
        "title": "Screenshot Analysis",
        "total_score": total_score,
        "max_score": 30,
        "categories": categories,
        "screenshot": ss_b64,
    }


def empty_result(image_bytes, img):
    buf = io.BytesIO()
    img.save(buf, format="PNG")
    ss_b64 = base64.b64encode(buf.getvalue()).decode()
    return {
        "title": "Screenshot Analysis",
        "total_score": 0,
        "max_score": 30,
        "categories": {
            "contrast": {"score": 0, "max_score": 10, "details": [detail(False, "Could not sample pixels")]},
            "color": {"score": 0, "max_score": 10, "details": [detail(False, "Could not sample pixels")]},
            "brightness": {"score": 0, "max_score": 10, "details": [detail(False, "Could not sample pixels")]},
        },
        "screenshot": ss_b64,
    }


def generate_example_image():
    w, h = 800, 600
    img = Image.new("RGB", (w, h), (0xf0, 0xf0, 0xf0))

    from PIL import ImageDraw
    d = ImageDraw.Draw(img)

    # Header bar
    d.rectangle([0, 0, w, 56], fill=(0x1a, 0x1a, 0x1a))

    # Sidebar
    d.rectangle([0, 56, 200, h], fill=(0x2a, 0x2a, 0x2a))
    for i in range(5):
        y = 78 + i * 50
        d.rounded_rectangle([20, y, 180, y + 34], 8, fill=(0x44, 0x44, 0x44))
    d.rounded_rectangle([20, 78, 180, 112], 8, fill=(0x3a, 0x6e, 0xa5))

    # Content cards
    cx, cy = 220, 76

    # Card 1 - wide
    d.rounded_rectangle([cx, cy, cx + 550, cy + 160], 12, fill=(0xff, 0xff, 0xff))
    d.rounded_rectangle([cx + 16, cy + 16, cx + 196, cy + 34], 4, fill=(0x33, 0x33, 0x33))
    d.rounded_rectangle([cx + 16, cy + 44, cx + 336, cy + 54], 4, fill=(0x33, 0x33, 0x33))
    d.rounded_rectangle([cx + 16, cy + 60, cx + 296, cy + 70], 4, fill=(0x33, 0x33, 0x33))
    d.rounded_rectangle([cx + 16, cy + 76, cx + 316, cy + 86], 4, fill=(0x33, 0x33, 0x33))
    # Progress bars
    d.rounded_rectangle([cx + 380, cy + 40, cx + 530, cy + 48], 4, fill=(0x4c, 0xaf, 0x50))
    d.rounded_rectangle([cx + 380, cy + 60, cx + 500, cy + 68], 4, fill=(0x21, 0x96, 0xf3))
    d.rounded_rectangle([cx + 380, cy + 80, cx + 470, cy + 88], 4, fill=(0xff, 0x98, 0x00))

    # Card 2 - half
    d.rounded_rectangle([cx, cy + 180, cx + 265, cy + 360], 12, fill=(0xff, 0xff, 0xff))
    d.rounded_rectangle([cx + 16, cy + 196, cx + 156, cy + 212], 4, fill=(0x33, 0x33, 0x33))
    for i in range(4):
        d.rounded_rectangle([cx + 16, cy + 226 + i * 30, cx + 236, cy + 234 + i * 30], 4, fill=(0x33, 0x33, 0x33))
    # Mini line chart
    pts = [(cx + 16, cy + 310), (cx + 60, cy + 290), (cx + 100, cy + 310),
           (cx + 140, cy + 280), (cx + 180, cy + 295), (cx + 220, cy + 270)]
    for i in range(len(pts) - 1):
        d.line([pts[i], pts[i + 1]], fill=(0x21, 0x96, 0xf3), width=2)

    # Card 3 - half right
    d.rounded_rectangle([cx + 285, cy + 180, cx + 550, cy + 360], 12, fill=(0xff, 0xff, 0xff))
    d.rounded_rectangle([cx + 301, cy + 196, cx + 421, cy + 212], 4, fill=(0x33, 0x33, 0x33))
    # Doughnut chart (arc segments)
    d.pieslice([cx + 350, cy + 230, cx + 430, cy + 310], 0, 130, fill=(0x4c, 0xaf, 0x50))
    d.pieslice([cx + 350, cy + 230, cx + 430, cy + 310], 130, 230, fill=(0x21, 0x96, 0xf3))
    d.pieslice([cx + 350, cy + 230, cx + 430, cy + 310], 230, 290, fill=(0xff, 0x98, 0x00))
    d.pieslice([cx + 350, cy + 230, cx + 430, cy + 310], 290, 360, fill=(0xe0, 0xe0, 0xe0))
    d.ellipse([cx + 365, cy + 245, cx + 415, cy + 295], fill=(0xff, 0xff, 0xff))
    # Legend
    d.rounded_rectangle([cx + 301, cy + 240, cx + 337, cy + 252], 4, fill=(0x4c, 0xaf, 0x50))
    d.rounded_rectangle([cx + 301, cy + 258, cx + 337, cy + 270], 4, fill=(0x21, 0x96, 0xf3))
    d.rounded_rectangle([cx + 301, cy + 276, cx + 337, cy + 288], 4, fill=(0xff, 0x98, 0x00))

    # Card 4 - bottom wide
    d.rounded_rectangle([cx, cy + 380, cx + 550, cy + 500], 12, fill=(0xff, 0xff, 0xff))
    d.rounded_rectangle([cx + 16, cy + 396, cx + 176, cy + 412], 4, fill=(0x33, 0x33, 0x33))
    d.rounded_rectangle([cx + 16, cy + 424, cx + 536, cy + 448], 4, fill=(0xe0, 0xe0, 0xe0))
    for i in range(3):
        d.rounded_rectangle([cx + 16, cy + 456 + i * 14, cx + 536, cy + 466 + i * 14], 3, fill=(0xe0, 0xe0, 0xe0))

    return img


def generate_example():
    img = generate_example_image()
    buf = io.BytesIO()
    img.save(buf, format="PNG")
    ss_b64 = base64.b64encode(buf.getvalue()).decode()

    categories = {
        "contrast": {
            "score": 9, "max_score": 10,
            "details": [
                detail(True, "Strong local contrast", "7.5:1"),
                detail(True, "WCAG AA compliant areas", "Exceeds 4.5:1"),
            ]
        },
        "color": {
            "score": 8, "max_score": 10,
            "details": [
                detail(True, "Balanced palette", "10 colors"),
                detail(True, "Good color variety", "Warm & cool tones"),
            ]
        },
        "brightness": {
            "score": 9, "max_score": 10,
            "details": [
                detail(True, "Good brightness balance", "55% light"),
                detail(True, "Not overly dark or light", "Balanced"),
            ]
        },
    }

    return {
        "title": "Example Dashboard (26/30)",
        "total_score": 26,
        "max_score": 30,
        "categories": categories,
        "screenshot": ss_b64,
    }
