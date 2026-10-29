"""Draws the Cali Climates launcher icon and writes every file that comes from it.

One description of the artwork (a 108 x 108 canvas: a sun, a small cloud and snow-capped mountains over a green hill,
in a cartoon style: thick dark-navy outlines, flat colours with one soft gradient each, on a blue sky) produces:
  - the adaptive icon's vector layers: ic_launcher_background.xml (the sky), ic_launcher_foreground.xml (everything
    in front of it), ic_launcher_monochrome.xml (Android 13 themed icons) and ic_launcher_vector.xml (both layers in one
    file, used by the intro screen);
  - the fallback launcher images for Android 7 (mipmap-*/ic_launcher.webp and ic_launcher_round.webp);
  - the 512 px Play Store icon, which the README artwork (make_art.py) also reads.

Needs Pillow. Run from this folder: python3 make_icon.py   (then python3 make_art.py to refresh the README artwork).
The vector files are generated: change this script, not them. Launchers show only the middle two-thirds of the canvas
(a 72 unit circle or rounded square), so everything that matters sits inside it and the sky bleeds out to the edges.
"""
import math
import os
from PIL import Image, ImageDraw, ImageFilter

# ---------- palette and shapes (108 x 108 units) ----------
OUT = (0x0F, 0x2A, 0x52)                         # the outline colour: a deep navy
OW = 1.7                                         # outline width outside a filled shape
SKY = [(0.0, (0x4F, 0xB4, 0xF0)), (0.55, (0x1D, 0x6F, 0xC2)), (1.0, (0x0B, 0x3A, 0x78))]
GLOW = (58, 42, 44, (0xBF, 0xE8, 0xFF), 0.45)    # centre x, y, radius, colour, strongest opacity
SUN = (58, 45, 15)
SUN_GRAD = ((0xFF, 0xE0, 0x5A), (0xFF, 0xA3, 0x1A)); RAY = (0xFF, 0xC8, 0x3D)
RAY_FROM, RAY_TO, RAY_OUTLINE, RAY_CORE = 3.4, 9.0, 6.0, 2.8
MOUNTAINS = [  # one big mountain, back left: outline points, light face, shaded face, apex x
    ([(-8, 92), (30, 34), (68, 92)], (0x35, 0xB5, 0x7A), (0x1F, 0x86, 0x5C), 30),
]
SNOW_CAPS = [(30, 34, 15, 12)]                   # apex x, apex y, width, height
SNOW = (0xFF, 0xFF, 0xFF)
HILL = [(-10, 92), (20, 88), (54, 91), (86, 88), (118, 92)]; HILL_COLOR = (0x1F, 0x8F, 0x62)
CLOUD_CIRCLES = [(52, 73, 6.2), (60.5, 66.5, 8.8), (71, 68, 7.8), (79.5, 73, 5.8)]
CLOUD_BASE = (45, 73, 85.5, 80, 3.5)                 # x0, y0, x1, y1, corner radius
CLOUD_TOP, CLOUD_BOTTOM = (0xFF, 0xFF, 0xFF), (0xBF, 0xDC, 0xF5)
LINE = 2.6                                       # width of a centred outline on a polygon


def lerp(a, b, t): return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))
def hexc(c): return "#FF%02X%02X%02X" % c
def alpha_hex(c, a): return "#%02X%02X%02X%02X" % (int(a * 255), c[0], c[1], c[2])


def catmull(pts, steps=14):
    out = []; Q = [pts[0]] + pts + [pts[-1]]
    for i in range(1, len(Q) - 2):
        p0, p1, p2, p3 = Q[i - 1], Q[i], Q[i + 1], Q[i + 2]
        for s in range(steps):
            t = s / steps
            out.append(tuple(0.5 * ((2 * p1[j]) + (-p0[j] + p2[j]) * t + (2 * p0[j] - 5 * p1[j] + 4 * p2[j] - p3[j]) * t * t
                                    + (-p0[j] + 3 * p1[j] - 3 * p2[j] + p3[j]) * t ** 3) for j in (0, 1)))
    out.append(pts[-1]); return out


def bez(pts):
    Q = [pts[0]] + pts + [pts[-1]]; d = f"M{pts[0][0]:.1f},{pts[0][1]:.1f}"
    for i in range(1, len(Q) - 2):
        p0, p1, p2, p3 = Q[i - 1], Q[i], Q[i + 1], Q[i + 2]
        c1 = (p1[0] + (p2[0] - p0[0]) / 6, p1[1] + (p2[1] - p0[1]) / 6)
        c2 = (p2[0] - (p3[0] - p1[0]) / 6, p2[1] - (p3[1] - p1[1]) / 6)
        d += f" C{c1[0]:.1f},{c1[1]:.1f} {c2[0]:.1f},{c2[1]:.1f} {p2[0]:.1f},{p2[1]:.1f}"
    return d


def poly_d(pts, close=True): return "M" + " L".join(f"{x:.1f},{y:.1f}" for x, y in pts) + (" Z" if close else "")
def circle_d(cx, cy, r): return f"M{cx - r:.1f},{cy:.1f} a{r:.1f},{r:.1f} 0 1,0 {2 * r:.1f},0 a{r:.1f},{r:.1f} 0 1,0 {-2 * r:.1f},0 Z"


def rrect_d(x0, y0, x1, y1, r):
    """A rounded rectangle drawn counter-clockwise on screen, like circle_d: shapes that overlap in one path must wind the
    same way, or Android's non-zero fill leaves a hole where they cross."""
    return (f"M{x0 + r:.1f},{y0:.1f} a{r:.1f},{r:.1f} 0 0,0 {-r:.1f},{r:.1f} V{y1 - r:.1f} a{r:.1f},{r:.1f} 0 0,0 {r:.1f},{r:.1f} "
            f"H{x1 - r:.1f} a{r:.1f},{r:.1f} 0 0,0 {r:.1f},{-r:.1f} V{y0 + r:.1f} a{r:.1f},{r:.1f} 0 0,0 {-r:.1f},{-r:.1f} Z")


def rays():
    out = []
    cx, cy, R = SUN
    for i in range(8):
        a = i * math.pi / 4 + math.pi / 8
        out.append(((cx + (R + RAY_FROM) * math.cos(a), cy + (R + RAY_FROM) * math.sin(a)),
                    (cx + (R + RAY_TO) * math.cos(a), cy + (R + RAY_TO) * math.sin(a))))
    return out


def snow_cap(ax, ay, w, h):
    return [(ax, ay), (ax + w / 2, ay + h), (ax + w * 0.2, ay + h * 0.7), (ax, ay + h * 1.05), (ax - w * 0.2, ay + h * 0.7), (ax - w / 2, ay + h)]


def shade(pts, apex_x): return [(apex_x, pts[1][1]), pts[2], (apex_x + 8, pts[2][1])]
def hill_pts(): return catmull(HILL)


# ---------- Pillow backend ----------
def render(S=1080, layers="both"):
    k = S / 108
    def P(pts): return [(x * k, y * k) for x, y in pts]
    clear = lambda: Image.new("RGBA", (S, S), (0, 0, 0, 0))
    img = clear()
    def put(layer): img.alpha_composite(layer)
    def masked(fill_layer, mask): return Image.composite(fill_layer, clear(), mask)
    def vband(c0, c1, y0, y1):
        im = clear(); d = ImageDraw.Draw(im)
        for yy in range(S):          # the colour holds its end values above and below the band, as the vector gradients do
            d.line([(0, yy), (S, yy)], fill=lerp(c0, c1, min(1, max(0, (yy / k - y0) / (y1 - y0)))) + (255,))
        return im
    if layers in ("both", "bg"):
        sky = clear(); d = ImageDraw.Draw(sky)
        for y in range(S):
            t = y / S
            for (o0, c0), (o1, c1) in zip(SKY, SKY[1:]):
                if t <= o1: c = lerp(c0, c1, (t - o0) / (o1 - o0)); break
            d.line([(0, y), (S, y)], fill=c + (255,))
        put(sky)
        gx, gy, gr, gc, ga = GLOW; glow = clear(); gd = ImageDraw.Draw(glow)
        for i in range(14, 0, -1):
            r = gr * (i / 14); gd.ellipse([(gx - r) * k, (gy - r) * k, (gx + r) * k, (gy + r) * k], fill=gc + (int(255 * ga * (1 - i / 14)) + 8,))
        put(glow)
    if layers in ("both", "fg"):
        d = ImageDraw.Draw(img)
        for pts, light, dark, apex in MOUNTAINS:
            d.polygon(P(pts), fill=light + (255,))
            sh = clear(); ImageDraw.Draw(sh).polygon(P(shade(pts, apex)), fill=dark + (255,))
            put(sh); d = ImageDraw.Draw(img)
            p = P(pts); d.line(p + [p[0]], fill=OUT + (255,), width=int(LINE * k), joint="curve")
        for ax, ay, w, h in SNOW_CAPS:
            p = P(snow_cap(ax, ay, w, h)); d.polygon(p, fill=SNOW + (255,)); d.line(p + [p[0]], fill=OUT + (255,), width=int(1.2 * k), joint="curve")
        d = ImageDraw.Draw(img)
        for (x0, y0), (x1, y1) in rays():
            for width, col in ((RAY_OUTLINE, OUT), (RAY_CORE, RAY)):
                d.line([(x0 * k, y0 * k), (x1 * k, y1 * k)], fill=col + (255,), width=int(width * k))
                for qx, qy in ((x0, y0), (x1, y1)):
                    r = width / 2; d.ellipse([(qx - r) * k, (qy - r) * k, (qx + r) * k, (qy + r) * k], fill=col + (255,))
        cx, cy, R = SUN
        d.ellipse([(cx - R - OW) * k, (cy - R - OW) * k, (cx + R + OW) * k, (cy + R + OW) * k], fill=OUT + (255,))
        m = Image.new("L", (S, S), 0); ImageDraw.Draw(m).ellipse([(cx - R) * k, (cy - R) * k, (cx + R) * k, (cy + R) * k], fill=255)
        put(masked(vband(SUN_GRAD[0], SUN_GRAD[1], cy - R, cy + R), m))
        gl = clear(); ImageDraw.Draw(gl).ellipse([(cx - 8) * k, (cy - 9) * k, (cx - 2) * k, (cy - 4) * k], fill=(255, 255, 255, 120))
        put(gl.filter(ImageFilter.GaussianBlur(0.6 * k)))
        d = ImageDraw.Draw(img)
        d = ImageDraw.Draw(img)
        hp = P(hill_pts() + [(118, 120), (-10, 120)]); d.polygon(hp, fill=HILL_COLOR + (255,)); d.line(hp + [hp[0]], fill=OUT + (255,), width=int(LINE * k), joint="curve")
        x0, y0, x1, y1, rad = CLOUD_BASE
        for cx2, cy2, r in CLOUD_CIRCLES: d.ellipse([(cx2 - r - OW) * k, (cy2 - r - OW) * k, (cx2 + r + OW) * k, (cy2 + r + OW) * k], fill=OUT + (255,))
        d.rounded_rectangle([(x0 - OW) * k, (y0 - OW) * k, (x1 + OW) * k, (y1 + OW) * k], radius=(rad + OW) * k, fill=OUT + (255,))
        cm = Image.new("L", (S, S), 0); cd = ImageDraw.Draw(cm)
        for cx2, cy2, r in CLOUD_CIRCLES: cd.ellipse([(cx2 - r) * k, (cy2 - r) * k, (cx2 + r) * k, (cy2 + r) * k], fill=255)
        cd.rounded_rectangle([x0 * k, y0 * k, x1 * k, y1 * k], radius=rad * k, fill=255)
        put(masked(vband(CLOUD_TOP, CLOUD_BOTTOM, 62, 80), cm))
    return img


# ---------- Android vector backend ----------
HEAD = '''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
'''


def grad(kind, stops, **a):
    items = "\n".join(f'                <item android:offset="{o}" android:color="{c}" />' for o, c in stops)
    attrs = "\n".join(f'                android:{n}="{v}"' for n, v in a.items())
    return f'''        <aapt:attr name="android:fillColor">
            <gradient
                android:type="{kind}"
{attrs}>
{items}
            </gradient>
        </aapt:attr>
'''


def vgrad_attr(y0, y1, stops): return grad("linear", [(o, hexc(c)) for o, c in stops], startX=54, startY=f"{y0:.1f}", endX=54, endY=f"{y1:.1f}")
def fill(d, color, comment=None, alpha=None):
    c = hexc(color) if alpha is None else alpha_hex(color, alpha)
    return (f"    <!-- {comment} -->\n" if comment else "") + f'    <path\n        android:fillColor="{c}"\n        android:pathData="{d}" />\n'
def gfill(d, g, comment=None): return (f"    <!-- {comment} -->\n" if comment else "") + f'    <path android:pathData="{d}">\n{g}    </path>\n'
def outlined(d, color, width, comment=None, fillcolor=None):
    fc = f'\n        android:fillColor="{hexc(fillcolor)}"' if fillcolor else ""
    return (f"    <!-- {comment} -->\n" if comment else "") + f'    <path{fc}\n        android:pathData="{d}"\n        android:strokeColor="{hexc(color)}"\n        android:strokeWidth="{width}"\n        android:strokeLineCap="round"\n        android:strokeLineJoin="round" />\n'


def cloud_d():
    x0, y0, x1, y1, rad = CLOUD_BASE
    return " ".join(circle_d(*c) for c in CLOUD_CIRCLES) + " " + rrect_d(x0, y0, x1, y1, rad)


def cloud_outline_d():
    x0, y0, x1, y1, rad = CLOUD_BASE
    return " ".join(circle_d(cx, cy, r + OW) for cx, cy, r in CLOUD_CIRCLES) + " " + rrect_d(x0 - OW, y0 - OW, x1 + OW, y1 + OW, rad + OW)


def background():
    gx, gy, gr, gc, ga = GLOW
    x = HEAD + "    <!-- Cali Climates launcher icon, background layer: the sky and a soft glow. Generated by docs/images/make_icon.py. -->\n"
    x += gfill("M-10,-10 H118 V118 H-10 Z", vgrad_attr(0, 108, SKY), "Sky")
    x += gfill(circle_d(gx, gy, gr), grad("radial", [(0.0, alpha_hex(gc, ga)), (1.0, alpha_hex(gc, 0))], centerX=gx, centerY=gy, gradientRadius=gr), "Glow behind the sun")
    return x + "</vector>\n"


def foreground(standalone=True):
    x = HEAD if standalone else ""
    x += "    <!-- Foreground layer: the sun, the mountains and a small cloud, outlined in deep navy. -->\n"
    for pts, light, dark, apex in MOUNTAINS:
        x += fill(poly_d(pts), light, "Mountain")
        x += fill(poly_d(shade(pts, apex)), dark)
        x += outlined(poly_d(pts), OUT, LINE)
    for ax, ay, w, h in SNOW_CAPS:
        x += outlined(poly_d(snow_cap(ax, ay, w, h)), OUT, 1.2, "Snow cap", fillcolor=SNOW)
    cx, cy, R = SUN
    for (x0, y0), (x1, y1) in rays():
        x += (f'    <path\n        android:pathData="M{x0:.1f},{y0:.1f} L{x1:.1f},{y1:.1f}"\n        android:strokeColor="{hexc(OUT)}"\n        android:strokeWidth="{RAY_OUTLINE}"\n        android:strokeLineCap="round" />\n')
    for (x0, y0), (x1, y1) in rays():
        x += (f'    <path\n        android:pathData="M{x0:.1f},{y0:.1f} L{x1:.1f},{y1:.1f}"\n        android:strokeColor="{hexc(RAY)}"\n        android:strokeWidth="{RAY_CORE}"\n        android:strokeLineCap="round" />\n')
    x += fill(circle_d(cx, cy, R + OW), OUT, "Sun")
    x += gfill(circle_d(cx, cy, R), vgrad_attr(cy - R, cy + R, [(0.0, SUN_GRAD[0]), (1.0, SUN_GRAD[1])]))
    x += fill(f"M{cx - 8:.1f},{cy - 6.5:.1f} a3,2.5 0 1,0 6,0 a3,2.5 0 1,0 -6,0 Z", (255, 255, 255), "Gloss", alpha=0.47)
    hp = [(round(a, 1), round(b, 1)) for a, b in HILL]
    x += outlined(bez(hp) + " L118,120 L-10,120 Z", OUT, LINE, "Hill", fillcolor=HILL_COLOR)
    x += fill(cloud_outline_d(), OUT, "Cloud")
    x += f'    <group>\n        <clip-path android:pathData="{cloud_d()}" />\n'
    g = gfill("M0,50 H108 V90 H0 Z", vgrad_attr(62, 80, [(0.0, CLOUD_TOP), (1.0, CLOUD_BOTTOM)]))
    x += "".join(("    " + l if l.strip() else l) for l in g.splitlines(True))
    x += "    </group>\n"
    return x + ("</vector>\n" if standalone else "")


def composite(): return background().rsplit("</vector>", 1)[0] + foreground(standalone=False) + "</vector>\n"


def monochrome():
    cx, cy, R = SUN
    x = HEAD + '    <!-- Themed (monochrome) icon, Android 13: the sun, mountain and cloud, each its own shape. -->\n'
    for d in [circle_d(cx, cy, R)] + [poly_d(p) for p, *_ in MOUNTAINS] + [circle_d(*c) for c in CLOUD_CIRCLES] + [rrect_d(*CLOUD_BASE)]:
        x += f'    <path\n        android:fillColor="#FF000000"\n        android:pathData="{d}" />\n'
    for (x0, y0), (x1, y1) in rays():
        x += f'    <path\n        android:pathData="M{x0:.1f},{y0:.1f} L{x1:.1f},{y1:.1f}"\n        android:strokeColor="#FF000000"\n        android:strokeWidth="{RAY_CORE + 0.8}"\n        android:strokeLineCap="round" />\n'
    return x + "</vector>\n"


# ---------- files ----------
REPO = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
RES = os.path.join(REPO, "app", "src", "main", "res")
LEGACY_SIZES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}


def write(path, text):
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(text)


def rasters():
    core = render(1080).crop((180, 180, 900, 900))     # the middle 72 of 108 units, which is what a launcher shows
    for density, px in LEGACY_SIZES.items():
        folder = os.path.join(RES, "mipmap-" + density)
        art = core.resize((px, px), Image.LANCZOS)
        square = art.copy(); m = Image.new("L", (px * 4, px * 4), 0)
        ImageDraw.Draw(m).rounded_rectangle([0, 0, px * 4 - 1, px * 4 - 1], radius=int(px * 4 * 0.22), fill=255)
        square.putalpha(m.resize((px, px), Image.LANCZOS)); square.save(os.path.join(folder, "ic_launcher.webp"), lossless=True)
        rnd = art.copy(); c = Image.new("L", (px * 4, px * 4), 0)
        ImageDraw.Draw(c).ellipse([0, 0, px * 4 - 1, px * 4 - 1], fill=255)
        rnd.putalpha(c.resize((px, px), Image.LANCZOS)); rnd.save(os.path.join(folder, "ic_launcher_round.webp"), lossless=True)
    core.resize((512, 512), Image.LANCZOS).save(os.path.join(REPO, "app", "src", "main", "ic_launcher-playstore.png"), optimize=True)


if __name__ == "__main__":
    drawable = os.path.join(RES, "drawable")
    write(os.path.join(drawable, "ic_launcher_background.xml"), background())
    write(os.path.join(drawable, "ic_launcher_foreground.xml"), foreground())
    write(os.path.join(drawable, "ic_launcher_vector.xml"), composite())
    write(os.path.join(drawable, "ic_launcher_monochrome.xml"), monochrome())
    rasters()
    print("wrote the icon's vector layers, the fallback launcher images and the Play Store icon")
