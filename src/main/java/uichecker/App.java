package uichecker;

import uichecker.checks.*;
import uichecker.screenshot.ScreenshotAnalyzer;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.ScreenshotType;
import com.microsoft.playwright.options.WaitUntilState;

public class App {
    // ── Palette ──────────────────────────────────────────────
    static final Color
        BG           = new Color(0x080808),
        SURFACE      = new Color(0x111111),
        CARD         = new Color(0x1a1a1a),
        CARD_HOVER   = new Color(0x242424),
        BORDER       = new Color(0x2e2e2e),
        TEXT          = new Color(0xf0f0f0),
        TEXT_SUB      = new Color(0x888888),
        TEXT_MUTED    = new Color(0x555555),
        ACCENT       = new Color(0xcccccc),
        ACCENT_GLOW  = new Color(0xffffff, true),
        GREEN         = new Color(0xbbbbbb),
        PURPLE        = new Color(0xaaaaaa),
        YELLOW        = new Color(0x999999),
        ORANGE        = new Color(0x888888),
        RED           = new Color(0x777777),
        PINK          = new Color(0x666666),
        TEAL          = new Color(0xbbbbbb);

    static final Map<String, Color> CAT_COLORS = Map.of(
        "readability", GREEN, "accessibility", ACCENT, "contrast", PURPLE,
        "layout", YELLOW, "responsiveness", ORANGE, "color", PINK, "brightness", TEAL);
    static final Map<String, String> CAT_LABELS = Map.of(
        "readability", "Readability", "accessibility", "Accessibility", "contrast", "Color & Contrast",
        "layout", "Layout & Spacing", "responsiveness", "Responsiveness", "color", "Color Palette", "brightness", "Brightness");

    private static final String FONT = System.getProperty("os.name").contains("Linux") ? "SansSerif" : "Segoe UI";

    // ── State ────────────────────────────────────────────────
    private JFrame frame;
    private JPanel root;
    private JPanel resultsScrollOuter;
    private JLabel statusIcon, statusText;
    private JTextField urlField;
    private JLabel fileLabel;
    private java.io.File selectedFile;
    private javax.swing.Timer animateTimer;
    private float animProgress;
    private int animTargetScore, animMaxScore;
    private Color animColor;
    private JPanel meterContainer;
    private boolean analyzing;

    // ── Entry ────────────────────────────────────────────────
    public static void main(String[] a) {
        System.setProperty("sun.awt.windows.darkMode", "true");
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(App::new);
    }

    App() {
        frame = new JFrame("UI Checker");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1020, 800);
        frame.setMinimumSize(new Dimension(700, 560));
        frame.setLocationRelativeTo(null);

        var cp = frame.getContentPane();
        cp.setBackground(BG);

        root = new JPanel(new BorderLayout());
        root.setBackground(BG);
        root.setBorder(new EmptyBorder(0, 0, 0, 0));

        root.add(headerPanel(), BorderLayout.NORTH);
        root.add(bodyPanel(), BorderLayout.CENTER);
        root.add(footerPanel(), BorderLayout.SOUTH);

        cp.add(root);
        frame.setVisible(true);

        // Entrance animation
        var entranceTimer = new javax.swing.Timer(16, null);
        entranceTimer.addActionListener(e -> {
            animProgress = Math.min(1, animProgress + .04f);
            root.repaint();
            if (animProgress >= 1) entranceTimer.stop();
        });
        entranceTimer.start();
    }

    // ═══════════════════════════════════════════════════════════
    //  HEADER
    // ═══════════════════════════════════════════════════════════
    private JPanel headerPanel() {
        var p = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                var g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth(), h = getHeight();
                var gp = new GradientPaint(0, 0, new Color(0x080808), w/2f, h, new Color(0x151515));
                g2.setPaint(gp); g2.fillRect(0, 0, w, h);
                // accent line
                g2.setColor(new Color(ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(), (int)(60*animProgress)));
                g2.fillRect(0, h-2, (int)(w*animProgress), 2);
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(32, 24, 28, 24));

        var c = new GridBagConstraints();

        var title = new JLabel("UI Checker");
        title.setFont(new Font(FONT, Font.BOLD, 30));
        title.setForeground(TEXT);
        p.add(title, c);

        c.gridy = 1; c.insets = new Insets(4, 0, 0, 0);
        var sub = new JLabel("Analyze any UI for readability, accessibility, and visual quality");
        sub.setFont(new Font(FONT, Font.PLAIN, 13));
        sub.setForeground(TEXT_SUB);
        p.add(sub, c);

        return p;
    }

    // ═══════════════════════════════════════════════════════════
    //  BODY
    // ═══════════════════════════════════════════════════════════
    private JPanel bodyPanel() {
        var p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(16, 24, 8, 24));

        var sidebar = sidebarPanel();
        sidebar.setPreferredSize(new Dimension(340, 0));
        p.add(sidebar, BorderLayout.WEST);

        var right = new JPanel(new BorderLayout());
        right.setOpaque(false);
        var sep = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                var g2 = (Graphics2D) g.create();
                g2.setColor(new Color(0x2e2e2e));
                g2.fillRect(0, 0, 1, getHeight());
                g2.dispose();
            }
        };
        sep.setOpaque(false);
        sep.setPreferredSize(new Dimension(1, 1));
        right.add(sep, BorderLayout.WEST);
        right.add(centerPanel(), BorderLayout.CENTER);
        p.add(right, BorderLayout.CENTER);

        return p;
    }

    // ── Sidebar ───────────────────────────────────────────────
    private JPanel sidebarPanel() {
        var p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(0, 0, 0, 20));

        var gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;
        gc.gridy = 0; gc.weighty = 0;
        p.add(sidebarSections(), gc);

        gc.gridy = 1; gc.weighty = 1; gc.fill = GridBagConstraints.BOTH;
        p.add(new JPanel() {{ setOpaque(false); }}, gc);

        return p;
    }

    // ── Combined sidebar sections ─────────────────────────────
    private JPanel sidebarSections() {
        var card = new RoundedPanel(CARD, BORDER, 12);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(12, 14, 12, 14));

        // URL section
        var urlTitle = new JLabel("\uD83C\uDF10  URL Analysis");
        urlTitle.setFont(new Font(FONT, Font.BOLD, 12));
        urlTitle.setForeground(TEXT);
        urlTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(urlTitle);
        card.add(Box.createVerticalStrut(6));

        urlField = new JTextField("https://") {
            @Override protected void paintComponent(Graphics g) {
                var g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isFocusOwner() ? new Color(0x222222) : new Color(0x181818));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                if (isFocusOwner()) {
                    g2.setColor(new Color(200, 200, 200, 40));
                    g2.setStroke(new BasicStroke(2));
                    g2.drawRoundRect(1, 1, getWidth()-3, getHeight()-3, 10, 10);
                }
                super.paintComponent(g);
                g2.dispose();
            }
        };
        urlField.setOpaque(false);
        urlField.setFont(new Font(FONT, Font.PLAIN, 13));
        urlField.setForeground(TEXT);
        urlField.setCaretColor(ACCENT);
        urlField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x2e2e2e), 1, true),
            new EmptyBorder(8, 12, 8, 12)));
        urlField.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { urlField.repaint(); }
            public void focusLost(FocusEvent e) { urlField.repaint(); }
        });
        urlField.setMaximumSize(new Dimension(9999, 36));
        urlField.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(urlField);

        card.add(Box.createVerticalStrut(6));

        card.add(new RoundedButton("Analyze URL", ACCENT, () -> {
            var u = urlField.getText().trim();
            if (!u.isEmpty()) analyzeUrl(u);
        }));

        // Divider
        card.add(Box.createVerticalStrut(10));
        card.add(new JPanel() {
            { setOpaque(false); setMaximumSize(new Dimension(9999, 1)); }
            @Override protected void paintComponent(Graphics g) {
                var g2 = (Graphics2D) g.create();
                g2.setColor(new Color(0x2e2e2e));
                g2.fillRect(0, 0, getWidth(), 1);
                g2.dispose();
            }
        });
        card.add(Box.createVerticalStrut(10));

        // Screenshot section
        var imgTitle = new JLabel("\uD83D\uDDBC\uFE0F  Screenshot");
        imgTitle.setFont(new Font(FONT, Font.BOLD, 12));
        imgTitle.setForeground(TEXT);
        imgTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(imgTitle);
        card.add(Box.createVerticalStrut(6));

        fileLabel = new JLabel("  \uD83D\uDCC1  No file selected") {
            @Override protected void paintComponent(Graphics g) {
                var g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0x181818));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(new Color(0x2e2e2e));
                g2.setStroke(new BasicStroke(1));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, 10);
                super.paintComponent(g);
                g2.dispose();
            }
        };
        fileLabel.setOpaque(false);
        fileLabel.setFont(new Font(FONT, Font.PLAIN, 13));
        fileLabel.setForeground(TEXT_SUB);
        fileLabel.setBorder(new EmptyBorder(8, 12, 8, 12));
        fileLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        fileLabel.setMaximumSize(new Dimension(9999, 36));
        fileLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        fileLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                var fd = new java.awt.FileDialog(frame, "Select Screenshot", java.awt.FileDialog.LOAD);
                fd.setVisible(true);
                if (fd.getFile() != null) {
                    selectedFile = new java.io.File(fd.getDirectory(), fd.getFile());
                    fileLabel.setText("  \uD83D\uDCF7  " + selectedFile.getName());
                    fileLabel.setForeground(TEXT);
                }
            }
        });
        card.add(fileLabel);

        card.add(Box.createVerticalStrut(6));

        card.add(new RoundedButton("Analyze Image", ACCENT, () -> {
            if (selectedFile != null) analyzeImage(selectedFile);
        }));

        return card;
    }

    // ── URL Section (sidebar) ─────────────────────────────────

    // ── Center (loading / status) ─────────────────────────────
    private JPanel centerPanel() {
        var p = new JPanel(new BorderLayout());
        p.setOpaque(false);

        // Status / loading
        var status = new JPanel(new GridBagLayout());
        status.setOpaque(false);
        status.setBorder(new EmptyBorder(60, 0, 40, 0));

        statusIcon = new JLabel("\uD83D\uDD0D");
        statusIcon.setFont(new Font(System.getProperty("os.name").contains("Linux") ? "SansSerif" : "Segoe UI Emoji", Font.PLAIN, 36));
        var sg = new GridBagConstraints();
        status.add(statusIcon, sg);

        sg.gridy = 1; sg.insets = new Insets(12, 0, 0, 0);
        statusText = new JLabel("Enter a URL or upload a screenshot to begin");
        statusText.setFont(new Font(FONT, Font.PLAIN, 14));
        statusText.setForeground(TEXT_SUB);
        status.add(statusText, sg);

        p.add(status, BorderLayout.NORTH);

        // Results area
        resultsScrollOuter = new JPanel(new BorderLayout());
        resultsScrollOuter.setOpaque(false);
        resultsScrollOuter.setVisible(false);
        p.add(resultsScrollOuter, BorderLayout.CENTER);

        return p;
    }

    // ── Footer ────────────────────────────────────────────────
    private JPanel footerPanel() {
        var p = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(12, 0, 16, 0));
        var l = new JLabel("UI Checker  \u00b7  Automated UI Analysis");
        l.setFont(new Font(FONT, Font.PLAIN, 11));
        l.setForeground(TEXT_MUTED);
        p.add(l);
        return p;
    }

    // ═══════════════════════════════════════════════════════════
    //  ANALYSIS
    // ═══════════════════════════════════════════════════════════
    private void setLoading(boolean loading, String msg) {
        analyzing = loading;
        statusIcon.setText(loading ? "\u23F3" : "\uD83D\uDD0D");
        statusText.setText(msg);
        if (loading) resultsScrollOuter.setVisible(false);
    }

    private void analyzeUrl(String url) {
        setLoading(true, "Launching browser...");
        CompletableFuture.supplyAsync(() -> {
            try (var pw = Playwright.create()) {
                var b = pw.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
                var ctx = b.newContext(new Browser.NewContextOptions().setViewportSize(1280, 720));
                var p = ctx.newPage();
                var perf = new LinkedHashMap<String, Object>();

                long t0 = System.currentTimeMillis();
                p.navigate(url, new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE).setTimeout(30000.0));
                perf.put("load_time", System.currentTimeMillis() - t0);

                var res = (List<Map<String, Object>>) p.evaluate(
                    "JSON.parse(JSON.stringify(performance.getEntriesByType('resource').map(e=>({transferSize:e.transferSize||0}))))");
                var rl = res != null ? res : new ArrayList<Map<String, Object>>();
                perf.put("resource_count", rl.size());
                perf.put("total_size", rl.stream().mapToLong(r -> ((Number) r.getOrDefault("transferSize", 0)).longValue()).sum());

                var ss = p.screenshot(new Page.ScreenshotOptions().setType(ScreenshotType.PNG).setFullPage(false));
                var ssb = Base64.getEncoder().encodeToString(ss);
                var dom = (Map<String, Object>) p.evaluate(JS_EXTRACTOR);
                b.close();

                var cat = new LinkedHashMap<String, Object>();
                cat.put("readability", ReadabilityCheck.check(dom));
                cat.put("accessibility", AccessibilityCheck.check(dom));
                cat.put("contrast", ContrastCheck.check(dom));
                cat.put("layout", LayoutCheck.check(dom));
                cat.put("responsiveness", ResponsivenessCheck.check(dom, perf));

                long total = 0;
                for (var c : cat.values()) total += ((Number) ((Map) c).get("score")).longValue();

                var r = new LinkedHashMap<String, Object>();
                r.put("type", "url"); r.put("title", dom.getOrDefault("title", url));
                r.put("url", url); r.put("total_score", (int) total);
                r.put("max_score", 100); r.put("categories", cat);
                r.put("performance", perf); r.put("screenshot", ssb);
                return r;
            } catch (Exception e) {
                var err = new LinkedHashMap<String, Object>();
                err.put("error", "Analysis failed: " + e.getMessage());
                return err;
            }
        }).thenAccept(r -> SwingUtilities.invokeLater(() -> showResult(r)));
    }

    private void analyzeImage(java.io.File file) {
        setLoading(true, "Analyzing screenshot...");
        CompletableFuture.supplyAsync(() -> {
            try {
                return ScreenshotAnalyzer.analyze(java.nio.file.Files.readAllBytes(file.toPath()));
            } catch (Exception e) {
                var err = new LinkedHashMap<String, Object>();
                err.put("error", "Failed: " + e.getMessage());
                return err;
            }
        }).thenAccept(r -> SwingUtilities.invokeLater(() -> showResult(r)));
    }

    // ═══════════════════════════════════════════════════════════
    //  RESULT DISPLAY
    // ═══════════════════════════════════════════════════════════
    @SuppressWarnings("unchecked")
    private void showResult(Map<String, Object> result) {
        if (result.containsKey("error")) {
            setLoading(false, "\u2717  " + result.get("error"));
            return;
        }

        analyzing = false;
        var total = ((Number) result.getOrDefault("total_score", 0)).intValue();
        var max = ((Number) result.getOrDefault("max_score", 100)).intValue();
        var cats = (Map<String, Object>) result.getOrDefault("categories", Map.of());
        var ss = (String) result.get("screenshot");
        var title = (String) result.getOrDefault("title", "Analysis");
        var url = (String) result.get("url");
        var perf = (Map<String, Object>) result.get("performance");

        statusIcon.setText("\u2728");
        statusText.setText("Analysis complete");
        statusText.setForeground(TEXT_SUB);

        resultsScrollOuter.removeAll();
        resultsScrollOuter.setVisible(true);

        var scroll = new JScrollPane();
        scroll.setBorder(null);
        scroll.setBackground(BG);
        scroll.getViewport().setBackground(BG);
        scroll.getVerticalScrollBar().setUnitIncrement(12);
        scroll.getVerticalScrollBar().setBackground(new Color(0x111111));
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(6, Integer.MAX_VALUE));

        var content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(BG);
        content.setBorder(new EmptyBorder(4, 0, 20, 0));

        // Score header card
        content.add(scoreHeaderCard(title, url, total, max));

        // Screenshot card
        if (ss != null && !ss.isEmpty()) {
            try {
                var img = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(ss)));
                if (img != null) { content.add(Box.createVerticalStrut(16)); content.add(screenshotCard(img, total, max)); }
            } catch (Exception ignored) {}
        }

        // Categories
        content.add(Box.createVerticalStrut(16));
        content.add(categoriesGrid(cats));

        // Performance
        if (perf != null) {
            content.add(Box.createVerticalStrut(16));
            content.add(perfCard(perf));
        }

        scroll.setViewportView(content);
        resultsScrollOuter.add(scroll, BorderLayout.CENTER);
        resultsScrollOuter.revalidate();
        resultsScrollOuter.repaint();

        // Animate score meter
        var grade = gradeOf(total, max);
        animTargetScore = total;
        animMaxScore = max;
        animColor = grade.color;
        animProgress = 0;
        if (animateTimer != null) animateTimer.stop();
        animateTimer = new javax.swing.Timer(16, null);
        animateTimer.addActionListener(e -> {
            animProgress = Math.min(1, animProgress + .035f);
            if (meterContainer != null) meterContainer.repaint();
            if (animProgress >= 1) animateTimer.stop();
        });
        animateTimer.start();
    }

    // ── Score Header Card ─────────────────────────────────────
    private JPanel scoreHeaderCard(String title, String url, int score, int max) {
        var card = new RoundedPanel(CARD, BORDER, 14);
        card.setLayout(new BorderLayout(16, 0));
        card.setBorder(new EmptyBorder(20, 24, 20, 24));

        var textCol = new JPanel();
        textCol.setLayout(new BoxLayout(textCol, BoxLayout.Y_AXIS));
        textCol.setOpaque(false);

        var t = new JLabel(title);
        t.setFont(new Font(FONT, Font.BOLD, 20));
        t.setForeground(TEXT);
        textCol.add(t);

        if (url != null) {
            var u = new JLabel(url);
            u.setFont(new Font(FONT, Font.PLAIN, 12));
            u.setForeground(ACCENT);
            textCol.add(Box.createVerticalStrut(2));
            textCol.add(u);
        }

        var grade = gradeOf(score, max);
        var gradeLbl = new JLabel(grade.label.toUpperCase());
        gradeLbl.setFont(new Font(FONT, Font.BOLD, 13));
        gradeLbl.setForeground(grade.color);

        var details = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        details.setOpaque(false);
        details.add(gradeLbl);
        textCol.add(Box.createVerticalStrut(6));
        textCol.add(details);

        card.add(textCol, BorderLayout.CENTER);

        meterContainer = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                var g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                int cx = getWidth()/2, cy = getHeight()/2, r = 52, sw = 9;
                double pct = animMaxScore > 0 ? Math.min(animProgress, (double) animTargetScore / animMaxScore) : 0;
                double angle = pct * 360;

                // Glow
                g2.setColor(new Color(animColor.getRed(), animColor.getGreen(), animColor.getBlue(), 20));
                g2.setStroke(new BasicStroke(sw + 6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(new Arc2D.Double(cx-r-3, cy-r-3, r*2+6, r*2+6, 90, -angle, Arc2D.OPEN));

                // Track
                g2.setColor(new Color(0x2a2a2a));
                g2.setStroke(new BasicStroke(sw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(new Arc2D.Double(cx-r, cy-r, r*2, r*2, 0, 360, Arc2D.OPEN));

                // Arc
                g2.setColor(animColor);
                g2.setStroke(new BasicStroke(sw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(new Arc2D.Double(cx-r, cy-r, r*2, r*2, 90, -angle, Arc2D.OPEN));

                // Score text
                g2.setColor(TEXT);
                g2.setFont(new Font(FONT, Font.BOLD, 28));
                var fm = g2.getFontMetrics();
                var sv = String.valueOf(animTargetScore);
                g2.drawString(sv, cx - fm.stringWidth(sv)/2f, cy + 10);

                g2.dispose();
            }
        };
        meterContainer.setPreferredSize(new Dimension(140, 130));
        meterContainer.setOpaque(false);

        card.add(meterContainer, BorderLayout.EAST);

        return card;
    }

    // ── Screenshot Card ───────────────────────────────────────
    private JPanel screenshotCard(BufferedImage img, int score, int max) {
        var card = new RoundedPanel(CARD, BORDER, 14);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(0, 0, 0, 0));

        int mw = 860, w = img.getWidth(), h = img.getHeight();
        if (w > mw) { h = h * mw / w; w = mw; }
        var scaled = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        var g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(img, 0, 0, w, h, null);
        g.dispose();

        var lbl = new JLabel(new ImageIcon(scaled));
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(lbl, BorderLayout.CENTER);

        return card;
    }

    // ── Categories Grid ───────────────────────────────────────
    @SuppressWarnings("unchecked")
    private JPanel categoriesGrid(Map<String, Object> cats) {
        var grid = new JPanel(new GridLayout(0, 3, 12, 12));
        grid.setOpaque(false);

        for (var e : cats.entrySet()) {
            var key = e.getKey();
            var cat = (Map<String, Object>) e.getValue();
            var s = ((Number) cat.getOrDefault("score", 0)).intValue();
            var m = ((Number) cat.getOrDefault("max_score", 10)).intValue();
            var dets = (List<Map<String, Object>>) cat.getOrDefault("details", List.of());
            var col = CAT_COLORS.getOrDefault(key, ACCENT);
            var lab = CAT_LABELS.getOrDefault(key, key);
            grid.add(categoryCard(lab, col, s, m, dets));
        }

        var w = new JPanel(new BorderLayout());
        w.setOpaque(false);
        w.add(grid, BorderLayout.NORTH);
        return w;
    }

    private JPanel categoryCard(String title, Color color, int score, int max, List<Map<String, Object>> details) {
        var card = new JPanel() {
            boolean hover;
            @Override protected void paintComponent(Graphics g) {
                var g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hover ? CARD_HOVER : CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(BORDER);
                g2.setStroke(new BasicStroke(1));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 12, 12);
                g2.dispose();
            }
        };
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(16, 18, 16, 18));
        card.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { card.hover = true; card.repaint(); }
            public void mouseExited(MouseEvent e) { card.hover = false; card.repaint(); }
        });

        // Header row
        var hdr = new JPanel(new BorderLayout());
        hdr.setOpaque(false);

        var tl = new JLabel(title);
        tl.setFont(new Font(FONT, Font.BOLD, 13));
        tl.setForeground(color);
        hdr.add(tl, BorderLayout.WEST);

        hdr.add(miniMeter(score, max, color), BorderLayout.EAST);
        card.add(hdr);

        // Details
        if (!details.isEmpty()) {
            card.add(Box.createVerticalStrut(10));
            for (var d : details) {
                var pass = Boolean.TRUE.equals(d.get("pass"));
                var dt = (String) d.get("label");
                var dh = (String) d.get("detail");
                card.add(detailRow(pass, dt, dh));
                card.add(Box.createVerticalStrut(4));
            }
        }

        return card;
    }

    private JPanel detailRow(boolean pass, String text, String hint) {
        var p = new JPanel(new BorderLayout(6, 0));
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(700, 20));

        var icon = new JLabel(pass ? "\u2713" : "\u2717");
        icon.setFont(new Font(FONT, Font.BOLD, 13));
        icon.setForeground(pass ? GREEN : RED);
        p.add(icon, BorderLayout.WEST);

        var full = hint != null ? text + "  \u2014  " + hint : text;
        var lbl = new JLabel(full);
        lbl.setFont(new Font(FONT, Font.PLAIN, 11));
        lbl.setForeground(pass ? TEXT : TEXT_SUB);
        p.add(lbl, BorderLayout.CENTER);

        return p;
    }

    // ── Performance Card ──────────────────────────────────────
    private JPanel perfCard(Map<String, Object> perf) {
        var card = new RoundedPanel(CARD, BORDER, 14);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(18, 22, 18, 22));

        var title = new JLabel("Performance");
        title.setFont(new Font(FONT, Font.BOLD, 12));
        title.setForeground(TEXT_MUTED);
        card.add(title);
        card.add(Box.createVerticalStrut(14));

        var grid = new JPanel(new FlowLayout(FlowLayout.LEFT, 32, 0));
        grid.setOpaque(false);

        var lt = ((Number) perf.getOrDefault("load_time", 0)).longValue();
        var rc = ((Number) perf.getOrDefault("resource_count", 0)).intValue();
        var ts = ((Number) perf.getOrDefault("total_size", 0)).longValue();

        grid.add(perfMetric(String.format("%.1fs", lt / 1000.0), "Load Time"));
        grid.add(perfMetric(String.valueOf(rc), "Resources"));
        if (ts > 0) grid.add(perfMetric(Math.round(ts / 1024.0) + "KB", "Page Size"));

        card.add(grid);
        return card;
    }

    private JPanel perfMetric(String value, String label) {
        var p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);

        var v = new JLabel(value);
        v.setFont(new Font(FONT, Font.BOLD, 22));
        v.setForeground(ACCENT);
        v.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(v);

        var l = new JLabel(label);
        l.setFont(new Font(FONT, Font.PLAIN, 11));
        l.setForeground(TEXT_MUTED);
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(l);

        return p;
    }

    // ── Mini meter (for category cards) ───────────────────────
    private JComponent miniMeter(int score, int max, Color color) {
        var p = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                var g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int cx = getWidth()/2, cy = getHeight()/2, r = 22, sw = 5;
                double pct = max > 0 ? (double) score / max : 0;
                double a = pct * 360;

                g2.setColor(new Color(0x2a2a2a));
                g2.setStroke(new BasicStroke(sw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(new Arc2D.Double(cx-r, cy-r, r*2, r*2, 0, 360, Arc2D.OPEN));

                g2.setColor(color);
                g2.setStroke(new BasicStroke(sw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(new Arc2D.Double(cx-r, cy-r, r*2, r*2, 90, -a, Arc2D.OPEN));

                g2.setColor(TEXT);
                g2.setFont(new Font(FONT, Font.BOLD, 11));
                var fm = g2.getFontMetrics();
                var sv = String.valueOf(score);
                g2.drawString(sv, cx - fm.stringWidth(sv)/2f, cy + 4);
                g2.dispose();
            }
        };
        p.setPreferredSize(new Dimension(56, 56));
        p.setOpaque(false);
        return p;
    }

    // ── Helpers ───────────────────────────────────────────────
    record Grade(String label, Color color) {}
    private Grade gradeOf(int score, int max) {
        double p = max > 0 ? (double) score / max * 100 : 0;
        if (p >= 90) return new Grade("Excellent", GREEN);
        if (p >= 70) return new Grade("Good", ACCENT);
        if (p >= 50) return new Grade("Fair", YELLOW);
        if (p >= 30) return new Grade("Poor", ORANGE);
        return new Grade("Bad", RED);
    }

    // ═══════════════════════════════════════════════════════════
    //  CUSTOM COMPONENTS
    // ═══════════════════════════════════════════════════════════

    // ── Rounded Panel ─────────────────────────────────────────
    static class RoundedPanel extends JPanel {
        final Color bg, border; final int radius;
        RoundedPanel(Color bg, Color border, int radius) {
            this.bg = bg; this.border = border; this.radius = radius;
            setOpaque(false);
        }
        @Override protected void paintComponent(Graphics g) {
            var g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            if (border != null) {
                g2.setColor(border);
                g2.setStroke(new BasicStroke(1));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, radius, radius);
            }
            g2.dispose();
        }
    }

    // ── Rounded Button with hover animations ──────────────────
    static class RoundedButton extends JButton {
        private final Color bgColor;
        boolean hover;
        float hoverAmt;

        RoundedButton(String text, Color bg, Runnable action) {
            super(text);
            this.bgColor = bg;
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setFont(new Font(FONT, Font.BOLD, 13));
            setForeground(Color.WHITE);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            var dims = new Dimension(120, 36);
            setPreferredSize(dims);
            setMinimumSize(dims);
            setMaximumSize(new Dimension(9999, 36));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            addActionListener(e -> action.run());
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hover = true; animateHover(true); }
                public void mouseExited(MouseEvent e) { hover = false; animateHover(false); }
            });
        }

        void animateHover(boolean in) {
            var hTimer = new javax.swing.Timer(10, null);
            hTimer.addActionListener(e -> {
                hoverAmt = Math.min(1, Math.max(0, hoverAmt + (in ? .12f : -.12f)));
                repaint();
                if (hoverAmt >= 1 || hoverAmt <= 0) hTimer.stop();
            });
            hTimer.start();
        }

        @Override protected void paintComponent(Graphics g) {
            var g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();

            if (hoverAmt > 0) {
                g2.setColor(new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), (int)(30 * hoverAmt)));
                g2.fillRoundRect(2, 2, w-4, h-4, 12, 12);
            }

            var c = new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue());
            g2.setColor(c);
            g2.fillRoundRect(0, 0, w, h, 10, 10);

            g2.setColor(Color.WHITE);
            g2.setFont(getFont());
            var fm = g2.getFontMetrics();
            g2.drawString(getText(), (w - fm.stringWidth(getText())) / 2f, (h + fm.getAscent() / 2f) / 2f);
            g2.dispose();
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  JS EXTRACTOR
    // ═══════════════════════════════════════════════════════════
    private static final String JS_EXTRACTOR = """
        (()=>{const h=[];document.querySelectorAll('h1,h2,h3,h4,h5,h6').forEach(e=>{const s=getComputedStyle(e);h.push({tag:e.tagName,level:parseInt(e.tagName[1]),text:(e.textContent||'').trim().substring(0,100),fontSize:parseFloat(s.fontSize)})});const im=[];document.querySelectorAll('img').forEach(e=>{const r=e.getBoundingClientRect();if(r.width>0&&r.height>0)im.push({hasAlt:e.hasAttribute('alt'),altText:e.alt,width:r.width,height:r.height,visible:r.top<window.innerHeight&&r.bottom>0})});const ii=[];document.querySelectorAll('input,select,textarea,button,a,[role="button"]').forEach(e=>{const r=e.getBoundingClientRect();if(r.width>0&&r.height>0)ii.push({tag:e.tagName,hasAriaLabel:e.hasAttribute('aria-label'),hasAriaLabelledby:e.hasAttribute('aria-labelledby'),type:e.getAttribute('type')||null,text:(e.textContent||'').trim().substring(0,50),associatedLabel:null})});document.querySelectorAll('label').forEach(l=>{const f=l.getAttribute('for');if(f){const inp=document.getElementById(f);if(inp){const d=ii.find(i=>i.tag===inp.tagName);if(d)d.associatedLabel=(l.textContent||'').trim().substring(0,50)}}});const se=[];['nav','main','header','footer','article','section','aside','figure','figcaption'].forEach(t=>{const c=document.querySelectorAll(t).length;if(c>0)se.push({tag:t,count:c})});const fs=[],lh=[],cs=new Set();document.querySelectorAll('p,li,span,div,label,a,button,input,textarea,select,td,th').forEach(e=>{const t=(e.textContent||'').trim();if(t.length>20){const s=getComputedStyle(e);const f=parseFloat(s.fontSize);if(f>0)fs.push(f);const l=s.lineHeight;if(l&&l!=='normal'){const lv=parseFloat(l);if(!isNaN(lv))lh.push(lv/f)}cs.add(s.color);cs.add(s.backgroundColor)}});const vp=document.querySelector('meta[name="viewport"]');let mq=false;try{for(const s of document.styleSheets){try{for(const r of s.cssRules||[]){if(r instanceof CSSMediaRule){mq=true;break}}}catch(e){}if(mq)break}}catch(e){}return JSON.parse(JSON.stringify({headingLevels:h,images:im,interactiveElements:ii,semanticElements:se,allFontSizes:fs,allLineHeights:lh,allColors:[...cs],viewportMeta:vp?vp.getAttribute('content'):null,hasMediaQueries:mq,bodyChildren:document.body?document.body.children.length:0,textLength:(document.body?document.body.textContent:'').length,elementCount:document.querySelectorAll('*').length,hasDoctype:document.doctype!==null,langAttr:document.documentElement?document.documentElement.lang:null,title:document.title}))})()
        """;
}
