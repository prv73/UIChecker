import sys
import io
import base64
from pathlib import Path

from PySide6.QtWidgets import (
    QApplication, QMainWindow, QWidget, QVBoxLayout, QHBoxLayout,
    QPushButton, QLabel, QFileDialog, QScrollArea, QFrame, QGridLayout,
    QSizePolicy, QMenuBar, QMenu, QMessageBox
)
from PySide6.QtCore import (
    Qt, QTimer, QPropertyAnimation, QEasingCurve, Property, QRectF, QSize, QPoint
)
from PySide6.QtGui import (
    QPainter, QColor, QLinearGradient, QBrush, QPen, QFont, QFontMetrics,
    QPixmap, QImage, QAction, QPainterPath, QFontDatabase, QIcon
)

from analysis import analyze, generate_example

# ── Palette (monochrome dark) ─────────────────────────
BG         = QColor(0x08, 0x08, 0x08)
CARD       = QColor(0x1a, 0x1a, 0x1a)
CARD_HOVER = QColor(0x24, 0x24, 0x24)
BORDER     = QColor(0x2e, 0x2e, 0x2e)
TEXT       = QColor(0xf0, 0xf0, 0xf0)
TEXT_SUB   = QColor(0x88, 0x88, 0x88)
TEXT_MUTED = QColor(0x55, 0x55, 0x55)
ACCENT     = QColor(0xcc, 0xcc, 0xcc)
GREEN      = QColor(0xbb, 0xbb, 0xbb)
PURPLE     = QColor(0xaa, 0xaa, 0xaa)
YELLOW     = QColor(0x99, 0x99, 0x99)
ORANGE     = QColor(0x88, 0x88, 0x88)
RED        = QColor(0x77, 0x77, 0x77)

CAT_COLORS = {
    "contrast": GREEN, "color": ACCENT, "brightness": PURPLE,
}
CAT_LABELS = {
    "contrast": "Color & Contrast", "color": "Color Palette", "brightness": "Brightness",
}

def resolve_font(app):
    families = ["Segoe UI", "Segoe UI Variable Text", "Microsoft Sans Serif", "Tahoma", "Arial"]
    db = QFontDatabase()
    for f in families:
        if f in db.families():
            return f
    return "Segoe UI"

FONT = "Segoe UI"


def grade_of(score, max_score):
    p = (score / max_score * 100) if max_score > 0 else 0
    if p >= 90: return ("Excellent", GREEN)
    if p >= 70: return ("Good", ACCENT)
    if p >= 50: return ("Fair", YELLOW)
    if p >= 30: return ("Poor", ORANGE)
    return ("Bad", RED)


# ── Custom Widgets ─────────────────────────────────────

class RoundedPanel(QFrame):
    def __init__(self, bg=CARD, border=BORDER, radius=12):
        super().__init__()
        self._bg = bg
        self._border = border
        self._radius = radius
        self.setAttribute(Qt.WA_StyledBackground, False)

    def paintEvent(self, event):
        p = QPainter(self)
        p.setRenderHint(QPainter.Antialiasing)
        path = QPainterPath()
        path.addRoundedRect(QRectF(self.rect()), self._radius, self._radius)
        p.fillPath(path, self._bg)
        if self._border:
            p.setPen(QPen(self._border, 1))
            p.drawPath(path)


class RoundedButton(QPushButton):
    def __init__(self, text, bg, on_click=None):
        super().__init__(text)
        self._bg = bg
        self._hover_amt = 0.0
        self._hover_timer = QTimer(self)
        self._hover_timer.setInterval(10)
        self._hover_timer.timeout.connect(self._tick_hover)
        self._hovering = False
        self.clicked.connect(on_click) if on_click else None
        self.setCursor(Qt.PointingHandCursor)
        self.setFixedHeight(36)
        self.setAttribute(Qt.WA_StyledBackground, False)

    def enterEvent(self, event):
        self._hovering = True
        self._hover_timer.start()

    def leaveEvent(self, event):
        self._hovering = False
        self._hover_timer.start()

    def _tick_hover(self):
        step = 0.12
        if self._hovering:
            self._hover_amt = min(1.0, self._hover_amt + step)
            if self._hover_amt >= 1.0:
                self._hover_timer.stop()
        else:
            self._hover_amt = max(0.0, self._hover_amt - step)
            if self._hover_amt <= 0.0:
                self._hover_timer.stop()
        self.update()

    def paintEvent(self, event):
        p = QPainter(self)
        p.setRenderHint(QPainter.Antialiasing)
        w, h = self.width(), self.height()
        r = 10

        path = QPainterPath()
        path.addRoundedRect(0, 0, w, h, r, r)
        p.fillPath(path, self._bg)

        if self._hover_amt > 0:
            glow = QColor(self._bg)
            glow.setAlpha(int(30 * self._hover_amt))
            gpath = QPainterPath()
            gpath.addRoundedRect(2, 2, w - 4, h - 4, r, r)
            p.fillPath(gpath, glow)

        p.setPen(Qt.white)
        font = QFont(FONT, 13, QFont.Bold)
        p.setFont(font)
        p.drawText(self.rect(), Qt.AlignCenter, self.text())
        p.end()


class ScoreMeter(QWidget):
    def __init__(self, parent=None):
        super().__init__(parent)
        self._progress = 0.0
        self.target_score = 0
        self.target_max = 100
        self.meter_color = ACCENT
        self.setFixedSize(140, 130)

    def get_progress(self):
        return self._progress

    def set_progress(self, val):
        self._progress = val
        self.update()

    progress = Property(float, get_progress, set_progress)

    def paintEvent(self, event):
        p = QPainter(self)
        p.setRenderHint(QPainter.Antialiasing)
        cx, cy = self.width() / 2, self.height() / 2
        r = 52
        sw = 9

        pct = (self._progress * self.target_score / self.target_max) if self.target_max > 0 else 0
        angle = int(pct * 360 * 16)

        glow_c = QColor(self.meter_color)
        glow_c.setAlpha(20)
        p.setPen(QPen(glow_c, sw + 6, Qt.RoundCap))
        p.drawArc(QRectF(cx - r - 3, cy - r - 3, r * 2 + 6, r * 2 + 6), 90 * 16, -angle)

        p.setPen(QPen(QColor(0x2a, 0x2a, 0x2a), sw, Qt.RoundCap))
        p.drawArc(QRectF(cx - r, cy - r, r * 2, r * 2), 0, 360 * 16)

        p.setPen(QPen(self.meter_color, sw, Qt.RoundCap))
        p.drawArc(QRectF(cx - r, cy - r, r * 2, r * 2), 90 * 16, -angle)

        p.setPen(TEXT)
        font = QFont(FONT, 28, QFont.Bold)
        p.setFont(font)
        p.drawText(QRectF(0, cy - 20, self.width(), 40), Qt.AlignCenter, str(self.target_score))
        p.end()


class DetailRow(QWidget):
    def __init__(self, passed, text, hint=None, suggestion=None):
        super().__init__()
        self.setFixedHeight(20)
        layout = QHBoxLayout(self)
        layout.setContentsMargins(0, 0, 0, 0)
        layout.setSpacing(6)

        icon = QLabel("\u2713" if passed else "\u2717")
        icon.setFont(QFont(FONT, 13, QFont.Bold))
        icon.setStyleSheet(f"color: {'#bbb' if passed else '#777'}; background: transparent;")
        if suggestion:
            icon.setToolTip(f"<div style='width:300px;padding:6px;'>{suggestion}</div>")
        layout.addWidget(icon)

        full = text + ("  \u2014  " + hint if hint else "")
        lbl = QLabel(full)
        lbl.setFont(QFont(FONT, 11))
        lbl.setStyleSheet(f"color: {'#f0f0f0' if passed else '#888888'}; background: transparent;")
        layout.addWidget(lbl)
        layout.addStretch()


class CategoryCard(QWidget):
    def __init__(self, title, color, score, max_score, details):
        super().__init__()
        self._hover = False
        self._details = details
        self.setAttribute(Qt.WA_StyledBackground, False)

        layout = QVBoxLayout(self)
        layout.setContentsMargins(18, 16, 18, 16)

        hdr = QHBoxLayout()
        tl = QLabel(title)
        tl.setFont(QFont(FONT, 13, QFont.Bold))
        tl.setStyleSheet(f"color: {color.name()}; background: transparent;")
        hdr.addWidget(tl)
        hdr.addStretch()
        hdr.addWidget(self._mini_meter(score, max_score, color))
        layout.addLayout(hdr)

        if details:
            layout.addSpacing(10)
            for d in details:
                layout.addWidget(DetailRow(d["pass"], d["label"], d.get("detail"), d.get("suggestion")))
                layout.addSpacing(4)

    def _mini_meter(self, score, max_score, color):
        class MiniMeter(QWidget):
            def __init__(self, sc, mx, col):
                super().__init__()
                self._sc = sc
                self._mx = mx
                self._col = col
                self.setFixedSize(56, 56)

            def paintEvent(self, event):
                p = QPainter(self)
                p.setRenderHint(QPainter.Antialiasing)
                cx, cy = 28, 28
                r, sw = 22, 5
                pct = self._sc / self._mx if self._mx > 0 else 0
                a = pct * 360

                p.setPen(QPen(QColor(0x2a, 0x2a, 0x2a), sw, Qt.RoundCap))
                p.drawArc(QRectF(cx - r, cy - r, r * 2, r * 2), 0, 360 * 16)

                p.setPen(QPen(self._col, sw, Qt.RoundCap))
                p.drawArc(QRectF(cx - r, cy - r, r * 2, r * 2), 90 * 16, -int(a * 16))

                p.setPen(TEXT)
                font = QFont(FONT, 11, QFont.Bold)
                p.setFont(font)
                p.drawText(QRectF(0, 0, 56, 56), Qt.AlignCenter, str(self._sc))
                p.end()

        return MiniMeter(score, max_score, color)

    def enterEvent(self, event):
        self._hover = True
        self.update()

    def leaveEvent(self, event):
        self._hover = False
        self.update()

    def paintEvent(self, event):
        p = QPainter(self)
        p.setRenderHint(QPainter.Antialiasing)
        bg = CARD_HOVER if self._hover else CARD
        path = QPainterPath()
        path.addRoundedRect(QRectF(self.rect()), 12, 12)
        p.fillPath(path, bg)
        p.setPen(QPen(BORDER, 1))
        p.drawPath(path)


class SuggestionsCard(QWidget):
    def __init__(self, suggestions):
        super().__init__()
        layout = QVBoxLayout(self)
        layout.setContentsMargins(22, 18, 22, 18)

        title = QLabel("Suggestions to Improve")
        title.setFont(QFont(FONT, 13, QFont.Bold))
        title.setStyleSheet(f"color: {YELLOW.name()}; background: transparent;")
        layout.addWidget(title)

        for s in suggestions:
            layout.addSpacing(8)
            lbl = QLabel("\u2192  " + s)
            lbl.setFont(QFont(FONT, 11))
            lbl.setStyleSheet(f"color: {TEXT_SUB.name()}; background: transparent;")
            layout.addWidget(lbl)


class PerfMetric(QWidget):
    def __init__(self, value, label):
        super().__init__()
        layout = QVBoxLayout(self)
        layout.setSpacing(0)
        v = QLabel(value)
        v.setFont(QFont(FONT, 22, QFont.Bold))
        v.setStyleSheet(f"color: {ACCENT.name()}; background: transparent;")
        v.setAlignment(Qt.AlignCenter)
        layout.addWidget(v)
        l = QLabel(label)
        l.setFont(QFont(FONT, 11))
        l.setStyleSheet(f"color: {TEXT_MUTED.name()}; background: transparent;")
        l.setAlignment(Qt.AlignCenter)
        layout.addWidget(l)


# ── Main Window ────────────────────────────────────────

class UICheckerWindow(QMainWindow):
    def __init__(self):
        super().__init__()
        self.setWindowTitle("UI Checker")
        self.setMinimumSize(700, 560)
        self.resize(1020, 800)

        self.selected_file = None
        self.anim_progress = 0.0
        self.anim_target_score = 0
        self.anim_max_score = 100
        self.anim_color = ACCENT
        self.meter_container = None
        self._meter_widget = None
        self._meter_timer = None

        self._build_ui()
        self._apply_styles()

        self.entrance_progress = 0.0
        self.entrance_timer = QTimer(self)
        self.entrance_timer.timeout.connect(self._animate_entrance)
        self.entrance_timer.start(16)

    def _build_ui(self):
        central = QWidget()
        self.setCentralWidget(central)
        root = QVBoxLayout(central)
        root.setContentsMargins(0, 0, 0, 0)
        root.setSpacing(0)

        mb = self.menuBar()
        mb.setStyleSheet("QMenuBar { background: #111111; border-bottom: 1px solid #2e2e2e; } "
                         "QMenuBar::item { color: #f0f0f0; padding: 4px 12px; } "
                         "QMenuBar::item:selected { background: #2a2a2a; }")
        help_menu = mb.addMenu("Help")
        about = QAction("About UIChecker", self)
        about.triggered.connect(self._show_about)
        help_menu.addAction(about)

        root.addWidget(self._make_header())
        root.addWidget(self._make_body(), 1)
        root.addWidget(self._make_footer())

    def _make_header(self):
        class Header(QWidget):
            def __init__(self, parent=None):
                super().__init__(parent)
                self.setFixedHeight(130)
                self.progress = 0.0
                self.setAttribute(Qt.WA_StyledBackground, False)

            def paintEvent(self, event):
                p = QPainter(self)
                p.setRenderHint(QPainter.Antialiasing | QPainter.SmoothPixmapTransform)
                w, h = self.width(), self.height()
                grad = QLinearGradient(0, 0, w / 2, h)
                grad.setColorAt(0, QColor(0x08, 0x08, 0x08))
                grad.setColorAt(1, QColor(0x15, 0x15, 0x15))
                p.fillRect(self.rect(), grad)

                accent = QColor(ACCENT)
                accent.setAlpha(int(60 * self.progress))
                p.fillRect(0, h - 2, int(w * self.progress), 2, accent)
                p.end()

        self._header = Header()
        layout = QVBoxLayout(self._header)
        layout.setContentsMargins(24, 32, 24, 28)

        title = QLabel("UI Checker")
        title.setFont(QFont(FONT, 30, QFont.Bold))
        title.setStyleSheet(f"color: {TEXT.name()}; background: transparent;")
        layout.addWidget(title)

        sub = QLabel("Analyze screenshots for color, contrast, and brightness quality")
        sub.setFont(QFont(FONT, 13))
        sub.setStyleSheet(f"color: {TEXT_SUB.name()}; background: transparent;")
        layout.addWidget(sub)

        return self._header

    def _make_body(self):
        body = QWidget()
        body.setAttribute(Qt.WA_StyledBackground, False)
        layout = QHBoxLayout(body)
        layout.setContentsMargins(24, 16, 24, 8)

        sidebar = self._make_sidebar()
        sidebar.setFixedWidth(340)
        layout.addWidget(sidebar)

        sep = QFrame()
        sep.setFrameShape(QFrame.VLine)
        sep.setStyleSheet("color: #2e2e2e;")
        sep.setFixedWidth(1)
        layout.addWidget(sep)

        self.center = QWidget()
        self.center.setAttribute(Qt.WA_StyledBackground, False)
        self.center_layout = QVBoxLayout(self.center)
        self.center_layout.setContentsMargins(20, 0, 0, 0)

        # Status
        status_panel = QWidget()
        status_panel.setAttribute(Qt.WA_StyledBackground, False)
        sl = QVBoxLayout(status_panel)
        sl.setContentsMargins(0, 60, 0, 40)
        sl.setAlignment(Qt.AlignCenter)

        self.status_icon = QLabel("\U0001F50D")
        self.status_icon.setFont(QFont(FONT, 36))
        self.status_icon.setAlignment(Qt.AlignCenter)
        self.status_icon.setStyleSheet("background: transparent;")
        sl.addWidget(self.status_icon)

        self.status_text = QLabel("Upload a screenshot to analyze")
        self.status_text.setFont(QFont(FONT, 14))
        self.status_text.setStyleSheet(f"color: {TEXT_SUB.name()}; background: transparent;")
        self.status_text.setAlignment(Qt.AlignCenter)
        sl.addWidget(self.status_text)

        self.center_layout.addWidget(status_panel)

        # Results scroll area
        self.results_scroll = QScrollArea()
        self.results_scroll.setWidgetResizable(True)
        self.results_scroll.setVisible(False)
        self.results_scroll.setStyleSheet(
            "QScrollArea { border: none; background: transparent; } "
            "QScrollBar:vertical { background: #111111; width: 6px; } "
            "QScrollBar::handle:vertical { background: #333333; border-radius: 3px; min-height: 30px; } "
            "QScrollBar::add-line:vertical, QScrollBar::sub-line:vertical { height: 0; }")
        self.results_scroll.viewport().setStyleSheet("background: transparent;")

        self.results_content = QWidget()
        self.results_content.setAttribute(Qt.WA_StyledBackground, False)
        self.results_layout = QVBoxLayout(self.results_content)
        self.results_layout.setContentsMargins(0, 4, 0, 20)
        self.results_scroll.setWidget(self.results_content)

        self.center_layout.addWidget(self.results_scroll, 1)

        layout.addWidget(self.center, 1)
        return body

    def _make_sidebar(self):
        card = RoundedPanel()
        layout = QVBoxLayout(card)
        layout.setContentsMargins(14, 12, 14, 12)

        img_title = QLabel("\U0001F5BC\uFE0F  Screenshot")
        img_title.setFont(QFont(FONT, 12, QFont.Bold))
        img_title.setStyleSheet(f"color: {TEXT.name()}; background: transparent;")
        layout.addWidget(img_title)
        layout.addSpacing(6)

        self.file_label = QPushButton("  \U0001F4C1  No file selected")
        self.file_label.setFont(QFont(FONT, 13))
        self.file_label.setStyleSheet(f"""
            QPushButton {{
                color: {TEXT_SUB.name()}; background: #181818; border: 1px solid #2e2e2e;
                border-radius: 10px; padding: 8px 12px; text-align: left;
            }}
            QPushButton:hover {{
                background: #222222; border: 1px solid #444444;
            }}
        """)
        self.file_label.setFixedHeight(36)
        self.file_label.setCursor(Qt.PointingHandCursor)
        self.file_label.clicked.connect(self._pick_file)
        layout.addWidget(self.file_label)

        layout.addSpacing(6)
        layout.addWidget(RoundedButton("Analyze Image", ACCENT, self._analyze_image))

        layout.addSpacing(10)
        layout.addWidget(self._divider())
        layout.addSpacing(10)

        layout.addWidget(RoundedButton("Show Example", ACCENT, self._show_example))

        return card

    def _make_footer(self):
        f = QWidget()
        f.setAttribute(Qt.WA_StyledBackground, False)
        f.setFixedHeight(32)
        layout = QHBoxLayout(f)
        layout.setAlignment(Qt.AlignCenter)
        lbl = QLabel("UI Checker  \u00b7  Screenshot Analysis")
        lbl.setFont(QFont(FONT, 11))
        lbl.setStyleSheet(f"color: {TEXT_MUTED.name()}; background: transparent;")
        layout.addWidget(lbl)
        return f

    def _divider(self):
        d = QFrame()
        d.setFrameShape(QFrame.HLine)
        d.setStyleSheet("color: #2e2e2e;")
        return d

    def _apply_styles(self):
        self.setStyleSheet(f"""
            QMainWindow, QWidget {{ background-color: {BG.name()}; color: {TEXT.name()}; }}
            QMenu {{ background-color: {CARD.name()}; border: 1px solid {BORDER.name()}; }}
            QMenu::item {{ color: {TEXT.name()}; padding: 6px 24px; }}
            QMenu::item:selected {{ background-color: {CARD_HOVER.name()}; }}
        """)

    def _animate_entrance(self):
        self.entrance_progress = min(1.0, self.entrance_progress + 0.04)
        if self._header:
            self._header.progress = self.entrance_progress
            self._header.update()
        if self.entrance_progress >= 1.0:
            self.entrance_timer.stop()

    def _pick_file(self, checked=False):
        path, _ = QFileDialog.getOpenFileName(self, "Select Screenshot", "",
                                               "Images (*.png *.jpg *.jpeg *.bmp *.gif)")
        if path:
            self.selected_file = path
            self.file_label.setText("  \U0001F4F7  " + Path(path).name)
            self.file_label.setStyleSheet(
                f"color: {TEXT.name()}; background: #181818; border: 1px solid #2e2e2e; "
                f"border-radius: 10px; padding: 8px 12px;")

    def _set_loading(self, loading, msg=None):
        if loading:
            self.status_icon.setText("\u23F3")
            self.status_icon.setFont(QFont(FONT, 36))
            self.status_text.setText(msg or "Loading...")
            self.status_text.setStyleSheet(f"color: {TEXT_SUB.name()}; background: transparent;")
            self.results_scroll.setVisible(False)
        else:
            self.status_icon.setText("\U0001F50D")
            self.status_text.setText(msg or "")
            self.status_text.setStyleSheet(f"color: {TEXT_SUB.name()}; background: transparent;")

    def _analyze_image(self, checked=False):
        if not self.selected_file:
            return
        self._set_loading(True, "Analyzing screenshot...")
        from PySide6.QtWidgets import QApplication
        QApplication.processEvents()
        QTimer.singleShot(10, self._do_analysis)

    def _do_analysis(self):
        try:
            with open(self.selected_file, "rb") as f:
                data = f.read()
            result = analyze(data)
        except Exception as e:
            result = {"error": str(e)}
        self._on_result(result)

    def _on_result(self, result):
        if result.get("error"):
            self.status_icon.setText("\u26A0")
            self.status_text.setText(f"Error: {result['error']}")
            self.status_text.setStyleSheet(f"color: {ORANGE.name()}; background: transparent;")
            self.results_scroll.setVisible(False)
            return
        self._show_result(result)

    def _show_result(self, result):
        if "error" in result:
            err = result["error"].replace("\n", "<br>")
            self.status_icon.setText("\u26A0")
            self.status_text.setText(f"<html>{err}</html>")
            self.status_text.setStyleSheet(f"color: {ORANGE.name()}; background: transparent;")
            self.results_scroll.setVisible(False)
            return

        total = result.get("total_score", 0)
        max_score = result.get("max_score", 100)
        cats = result.get("categories", {})
        ss_b64 = result.get("screenshot", "")
        title = result.get("title", "Analysis")

        self.status_icon.setText("\u2728")
        self.status_text.setText("Analysis complete")
        self.status_text.setStyleSheet(f"color: {TEXT_SUB.name()}; background: transparent;")

        # Clear previous results
        self._clear_layout(self.results_layout)

        # Score header
        grade_label, grade_color = grade_of(total, max_score)
        self.results_layout.addWidget(self._score_header(title, total, max_score, grade_label, grade_color))

        # Screenshot
        if ss_b64:
            img_data = base64.b64decode(ss_b64)
            qimg = QImage.fromData(img_data)
            if not qimg.isNull():
                pix = QPixmap.fromImage(qimg)
                mw = min(pix.width(), 860)
                pix = pix.scaledToWidth(mw, Qt.SmoothTransformation)
                sl = QLabel()
                sl.setPixmap(pix)
                sl.setAlignment(Qt.AlignCenter)
                sc = RoundedPanel()
                sc_layout = QVBoxLayout(sc)
                sc_layout.setContentsMargins(0, 0, 0, 0)
                sc_layout.addWidget(sl)
                self.results_layout.addSpacing(16)
                self.results_layout.addWidget(sc)

        # Categories
        self.results_layout.addSpacing(16)
        self.results_layout.addWidget(self._categories_grid(cats))

        # Suggestions
        suggestions = []
        for key, cat in cats.items():
            for d in cat.get("details", []):
                if not d.get("pass", True):
                    sug = d.get("suggestion")
                    if sug:
                        suggestions.append(sug)
        if suggestions:
            self.results_layout.addSpacing(16)
            sc = RoundedPanel()
            sc_layout = QVBoxLayout(sc)
            sc_layout.setContentsMargins(0, 0, 0, 0)
            sc_layout.addWidget(SuggestionsCard(suggestions))
            self.results_layout.addWidget(sc)

        self.results_layout.addStretch()
        self.results_scroll.setVisible(True)

        # Animate
        self.anim_target_score = total
        self.anim_max_score = max_score
        self.anim_color = grade_color
        self.anim_progress = 0.0
        if self._meter_timer:
            self._meter_timer.stop()
        self._meter_timer = QTimer(self)
        self._meter_timer.setInterval(16)
        self._meter_timer.timeout.connect(self._tick_meter)
        self._meter_timer.start()

    def _clear_layout(self, layout):
        while layout.count():
            item = layout.takeAt(0)
            w = item.widget()
            if w:
                w.deleteLater()

    def _score_header(self, title, score, max_score, grade_label, grade_color):
        class MeterWidget(QWidget):
            def __init__(self):
                super().__init__()
                self.setFixedSize(140, 130)
                self._progress = 0.0
                self._target = 0
                self._max = 100
                self._col = ACCENT

            def paintEvent(self, event):
                p = QPainter(self)
                p.setRenderHint(QPainter.Antialiasing)
                cx, cy = 70, 65
                r, sw = 52, 9
                pct = (self._progress * self._target / self._max) if self._max > 0 else 0
                angle = int(pct * 360 * 16)

                glow_c = QColor(self._col)
                glow_c.setAlpha(20)
                p.setPen(QPen(glow_c, sw + 6, Qt.RoundCap))
                p.drawArc(QRectF(cx - r - 3, cy - r - 3, r * 2 + 6, r * 2 + 6), 90 * 16, -angle)

                p.setPen(QPen(QColor(0x2a, 0x2a, 0x2a), sw, Qt.RoundCap))
                p.drawArc(QRectF(cx - r, cy - r, r * 2, r * 2), 0, 360 * 16)

                p.setPen(QPen(self._col, sw, Qt.RoundCap))
                p.drawArc(QRectF(cx - r, cy - r, r * 2, r * 2), 90 * 16, -angle)

                p.setPen(TEXT)
                font = QFont(FONT, 28, QFont.Bold)
                p.setFont(font)
                p.drawText(QRectF(0, cy - 20, 140, 40), Qt.AlignCenter, str(self._target))
                p.end()

        meter = MeterWidget()
        meter._target = score
        meter._max = max_score
        meter._col = grade_color
        self._meter_widget = meter

        card = RoundedPanel()
        layout = QHBoxLayout(card)
        layout.setContentsMargins(24, 20, 24, 20)

        text_col = QVBoxLayout()
        tl = QLabel(title)
        tl.setFont(QFont(FONT, 20, QFont.Bold))
        tl.setStyleSheet(f"color: {TEXT.name()}; background: transparent;")
        text_col.addWidget(tl)

        gl = QLabel(grade_label.upper())
        gl.setFont(QFont(FONT, 13, QFont.Bold))
        gl.setStyleSheet(f"color: {grade_color.name()}; background: transparent;")
        text_col.addWidget(gl)

        layout.addLayout(text_col)
        layout.addStretch()
        layout.addWidget(meter)

        return card

    def _categories_grid(self, cats):
        grid = QWidget()
        grid.setAttribute(Qt.WA_StyledBackground, False)
        gl = QGridLayout(grid)
        gl.setSpacing(12)

        items = list(cats.items())
        for i, (key, cat) in enumerate(items):
            s = cat.get("score", 0)
            m = cat.get("max_score", 10)
            dets = cat.get("details", [])
            col = CAT_COLORS.get(key, ACCENT)
            lab = CAT_LABELS.get(key, key)
            row, col_idx = divmod(i, 3)
            gl.addWidget(CategoryCard(lab, col, s, m, dets), row, col_idx)

        return grid

    def _show_example(self, checked=False):
        result = generate_example()
        self._show_result(result)

    def _show_about(self):
        text = ("UIChecker v1.0\n\n"
                "Desktop screenshot analysis tool\nfor the Open Source Hackathon.\n\n"
                "Analyzes screenshots for:\n"
                "  - Color Contrast\n"
                "  - Color Palette\n"
                "  - Brightness Balance\n\n"
                "Built with Python + PySide6 + Pillow")
        QMessageBox.information(self, "About UIChecker", text)

    def _tick_meter(self):
        self.anim_progress = min(1.0, self.anim_progress + 0.035)
        if self._meter_widget:
            self._meter_widget._progress = self.anim_progress
            self._meter_widget.update()
        if self.anim_progress >= 1.0:
            self._meter_timer.stop()


def main():
    app = QApplication(sys.argv)
    app.setStyle("Fusion")
    font_name = resolve_font(app)
    font = QFont(font_name, 10)
    font.setFamilies([font_name, "Segoe UI Emoji", "Segoe UI Symbol", "Arial"])
    font.setStyleStrategy(QFont.PreferAntialias)
    app.setFont(font)
    w = UICheckerWindow()
    w.show()
    sys.exit(app.exec())


if __name__ == "__main__":
    main()
