"""Generates the README's artwork (banner, divider, footer icon, placeholder screens) with Pillow, from the app's own files.

Run from this folder:  python3 make_art.py
It reads the app's launcher icon, the statewide terrain picture and California's region shapes (from StateUsaCA.kt)
and writes PNGs next to this file. Fonts: DejaVu Sans Bold and Liberation Sans (Linux paths); edit FONT_* for another
machine. The banner carries the app's name and tagline as pixels, so a rename means running this again.
"""
import os, re
from PIL import Image, ImageDraw, ImageFilter, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, "..", ".."))
SRC = os.path.join(ROOT, "app", "src", "main")
ICON = os.path.join(SRC, "ic_launcher-playstore.png")
RELIEF = os.path.join(SRC, "assets", "terrain", "california.webp")
STATE = os.path.join(SRC, "java", "io", "github", "genesjc", "climatefinder", "domain", "model", "StateUsaCA.kt")
FONT_BOLD = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
FONT_TEXT = "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf"

NAME = "Cali Climates"
TAGLINE = "Explore California's climates,"
TAGLINE2 = "region by region."
# What the app does that is worth a second look: design choices, not libraries (the README's badge row lists those).
# Each chip is its own image, linked to its row in the README's "Design choices" table: a small one in a row under the
# banner (chip-top-*.png) and a larger one in the table (chip-*.png). They are drawn at the size they show, because some
# viewers ignore the img height attribute.
# (name, colour, file name stem); the order is the order they sit in, and the order of the table's rows
CHIPS = [
    ("Live NWS Readings", (14, 116, 144), "live-nws"),
    ("Terrain Relief", (146, 94, 40), "terrain-relief"),
    ("Distance-Aware Labels", (20, 120, 100), "distance-aware-labels"),
    ("Elevation Bands", (91, 74, 140), "elevation-bands"),
    ("Smooth Transitions", (170, 92, 60), "smooth-transitions"),
    ("One State, Two Views", (60, 90, 160), "one-state-two-views"),
]
SLATE, SLATE2, SKY = (20, 56, 100), (36, 112, 168), (125, 211, 252)  # a mid blue: the icon's dark lower half must stand out
BOUNDS = (-124.5, -114.1, 31.9, 43.0)  # the statewide picture: lon min, lon max, lat min, lat max

def font(path, size): return ImageFont.truetype(path, size)

def gradient(w, h, c0, c1):
    img = Image.new("RGB", (w, h))
    px = img.load()
    for y in range(h):
        for x in range(w):
            t = (x / w * 0.65 + (1 - y / h) * 0.35)
            px[x, y] = tuple(int(c0[i] + (c1[i] - c0[i]) * t) for i in range(3))
    return img

def region_shapes():
    """{region id: [(lon, lat), ...]} and {region id: colour} read from StateUsaCA.kt."""
    text = open(STATE, encoding="utf-8").read()
    points = {m.group(1): (float(m.group(2)), float(m.group(3)))
              for m in re.finditer(r"val (\w+) = (-?\d+\.\d+)f to (-?\d+\.\d+)f", text)}
    start = text.index("override val regionShapes")
    block = text[text.index("mapOf(", start):text.index("override val pickerMap")]
    shapes = {}
    for m in re.finditer(r'"(\w+)" to listOf\((.*?)\)(?=\s*,\s*"|\s*\))', block, re.S):
        pts = []
        for tok in re.finditer(r"(-?\d+\.\d+)f to (-?\d+\.\d+)f|(\w+)", m.group(2)):
            if tok.group(1): pts.append((float(tok.group(1)), float(tok.group(2))))
            elif tok.group(3) in points: pts.append(points[tok.group(3)])
        shapes[m.group(1)] = pts
    colors = {}
    for m in re.finditer(r'id = "(\w+)".*?colorHex = "(#[0-9A-Fa-f]{6})"', text, re.S):
        colors[m.group(1)] = tuple(int(m.group(2)[i:i + 2], 16) for i in (1, 3, 5))
    return shapes, colors

def california(height):
    """The statewide relief with the six regions tinted and outlined, like the app's region picker."""
    relief = Image.open(RELIEF).convert("RGBA")
    w, h = relief.size
    shapes, colors = region_shapes()
    def xy(p): return ((p[0] - BOUNDS[0]) / (BOUNDS[1] - BOUNDS[0]) * w, (BOUNDS[3] - p[1]) / (BOUNDS[3] - BOUNDS[2]) * h)
    tint = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(tint)
    for rid, pts in shapes.items():
        poly = [xy(p) for p in pts]
        d.polygon(poly, fill=colors.get(rid, (200, 200, 200)) + (200,))
    for rid, pts in shapes.items():
        poly = [xy(p) for p in pts]
        d.line(poly + [poly[0]], fill=(58, 42, 30, 220), width=5, joint="curve")
    out = Image.alpha_composite(relief, tint)
    return out.resize((int(w * height / h), height), Image.LANCZOS)

def chip(text, fill, size=24, pad=(18, 9)):
    f = font(FONT_BOLD, size)
    bbox = f.getbbox(text)
    line = f.getbbox("Agy,")  # the same height for every chip, whatever its letters
    w, h = bbox[2] - bbox[0] + pad[0] * 2, line[3] - line[1] + pad[1] * 2 + 4
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle((0, 0, w - 1, h - 1), radius=h // 2, fill=fill)
    d.text((pad[0] - bbox[0], pad[1] - line[1] + 1), text, font=f, fill=(255, 255, 255))
    return img

def shadowed(base, layer, xy, blur=18, offset=(0, 14), opacity=110):
    sh = Image.new("RGBA", base.size, (0, 0, 0, 0))
    mask = layer.split()[-1].point(lambda v: opacity if v > 0 else 0)
    sh.paste((0, 0, 0, 255), (xy[0] + offset[0], xy[1] + offset[1]), mask)
    base.alpha_composite(sh.filter(ImageFilter.GaussianBlur(blur)))
    base.alpha_composite(layer, xy)

def banner(path):
    W, H = 1600, 520
    img = gradient(W, H, SLATE, SLATE2).convert("RGBA")
    d = ImageDraw.Draw(img)
    for gx in range(40, W, 64):  # a faint dotted grid, like a map graticule
        for gy in range(40, H, 64):
            d.ellipse((gx - 2, gy - 2, gx + 2, gy + 2), fill=(255, 255, 255, 22))
    icon = Image.open(ICON).convert("RGBA").resize((250, 250), Image.LANCZOS)
    mask = Image.new("L", icon.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, 249, 249), radius=56, fill=255)
    icon.putalpha(Image.composite(icon.split()[-1], Image.new("L", icon.size, 0), mask))
    # A light rim and a soft glow, so the icon's dark lower half does not melt into the background
    glow = Image.new("RGBA", img.size, (0, 0, 0, 0))
    ImageDraw.Draw(glow).rounded_rectangle((88 - 6, 135 - 6, 88 + 256, 135 + 256), radius=62, fill=(190, 230, 255, 120))
    img.alpha_composite(glow.filter(ImageFilter.GaussianBlur(14)))
    shadowed(img, icon, (88, 135), opacity=70)
    ImageDraw.Draw(img).rounded_rectangle((88 - 2, 135 - 2, 88 + 251, 135 + 251), radius=58, outline=(220, 240, 255, 235), width=4)
    x = 380
    d.text((x, 138), NAME, font=font(FONT_BOLD, 92), fill=(255, 255, 255))
    d.rectangle((x, 258, x + 150, 266), fill=(251, 191, 36))
    d.text((x, 286), TAGLINE, font=font(FONT_TEXT, 36), fill=(203, 225, 245))
    d.text((x, 330), TAGLINE2, font=font(FONT_TEXT, 36), fill=(203, 225, 245))
    ca = california(470)
    shadowed(img, ca, (W - ca.width - 120, 25), blur=22, opacity=130)
    img.convert("RGB").save(path, optimize=True)

def chip_images():
    """Two PNGs per chip: chip-top-<stem>.png for the row under the banner, chip-<stem>.png for the table."""
    for text, fill, stem in CHIPS:
        chip(text, fill, size=17, pad=(12, 6)).save(os.path.join(HERE, f"chip-top-{stem}.png"), optimize=True)
        chip(text, fill, size=20, pad=(13, 7)).save(os.path.join(HERE, f"chip-{stem}.png"), optimize=True)

def divider(path):
    W, H = 1600, 10
    img = Image.new("RGB", (W, H))
    px = img.load()
    stops = [(0.0, (28, 100, 130)), (0.5, (201, 133, 126)), (1.0, (184, 115, 58))]
    for x in range(W):
        t = x / (W - 1)
        for i in range(len(stops) - 1):
            (t0, c0), (t1, c1) = stops[i], stops[i + 1]
            if t0 <= t <= t1:
                k = (t - t0) / (t1 - t0)
                for y in range(H):
                    px[x, y] = tuple(int(c0[j] + (c1[j] - c0[j]) * k) for j in range(3))
                break
    img.save(path, optimize=True)

def footer_icon(path):
    icon = Image.open(ICON).convert("RGBA").resize((180, 180), Image.LANCZOS)
    mask = Image.new("L", icon.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, 179, 179), radius=40, fill=255)
    icon.putalpha(Image.composite(icon.split()[-1], Image.new("L", icon.size, 0), mask))
    icon.save(path, optimize=True)

def placeholder(path, size, title, subtitle, phone=True):
    """A labelled 'Screenshot coming' card: phone-shaped for screens, wide for the demo slot. Never passed off as a real screen."""
    w, h = size
    img = gradient(w, h, (14, 30, 54), (30, 64, 104)).convert("RGBA")
    d = ImageDraw.Draw(img)
    d.rounded_rectangle((3, 3, w - 4, h - 4), radius=44 if phone else 24, outline=(125, 211, 252, 150), width=4)
    if phone:
        d.rounded_rectangle((w // 2 - 60, 26, w // 2 + 60, 40), radius=7, fill=(125, 211, 252, 120))  # the speaker slot
    cy = h // 2
    tf, sf, lf = font(FONT_BOLD, 40), font(FONT_TEXT, 26), font(FONT_BOLD, 24)
    for text, f, y, fill in ((title, tf, cy - 70, (255, 255, 255)), (subtitle, sf, cy - 10, (203, 225, 245)), ("Screenshot coming", lf, cy + 50, (251, 191, 36))):
        bbox = d.textbbox((0, 0), text, font=f)
        d.text(((w - (bbox[2] - bbox[0])) / 2 - bbox[0], y), text, font=f, fill=fill)
    img.convert("RGB").save(path, optimize=True)

if __name__ == "__main__":
    banner(os.path.join(HERE, "banner.png"))
    chip_images()
    divider(os.path.join(HERE, "divider.png"))
    footer_icon(os.path.join(HERE, "icon.png"))
    placeholder(os.path.join(HERE, "placeholder-home.png"), (540, 1100), "Home", "region, station tile, map")
    placeholder(os.path.join(HERE, "placeholder-elevation.png"), (540, 1100), "Elevation", "ridges, skyline, the chart")
    placeholder(os.path.join(HERE, "placeholder-regions.png"), (540, 1100), "Regions", "tap a region on the map")
    placeholder(os.path.join(HERE, "placeholder-demo.png"), (1280, 360), "A short demo", "changing region, panning the map", phone=False)
    print("wrote the banner, divider, footer icon and placeholders")
