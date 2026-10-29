"""Draws the Cali Climates launcher icon and writes every file that comes from it.

One description of the artwork (a 108 x 108 canvas: California as a window onto a sun, snow-capped peaks and three
ridges, on a map of the Pacific and the land around it) produces:
  - the adaptive icon's vector layers: ic_launcher_background.xml, ic_launcher_foreground.xml, the monochrome layer
    for Android 13 themed icons, and ic_launcher_vector.xml (both layers in one file, used by the intro screen);
  - the fallback launcher images for Android 7 (mipmap-*/ic_launcher.webp and ic_launcher_round.webp);
  - the 512 px Play Store icon, which the README artwork (make_art.py) also reads.

Needs Pillow. Run from this folder: python3 make_icon.py   (then python3 make_art.py to refresh the README artwork).
The vector files are generated: change this script, not them. Launchers show only the middle two-thirds of the canvas
(a 72 unit circle), so the state is drawn to fit inside it and the map bleeds out to the edges.
"""
import os

import math
from PIL import Image, ImageDraw, ImageFilter

# ---------- helpers ----------
def lerp(a,b,t): return tuple(int(a[i]+(b[i]-a[i])*t) for i in range(3))
def hexc(c): return "#FF%02X%02X%02X"%c
def alpha_hex(c,a): return "#%02X%02X%02X%02X"%(int(a*255),c[0],c[1],c[2])
def catmull(pts,steps=14):
    out=[]; P=[pts[0]]+pts+[pts[-1]]
    for i in range(1,len(P)-2):
        p0,p1,p2,p3=P[i-1],P[i],P[i+1],P[i+2]
        for s in range(steps):
            t=s/steps
            out.append(tuple(0.5*((2*p1[k])+(-p0[k]+p2[k])*t+(2*p0[k]-5*p1[k]+4*p2[k]-p3[k])*t*t+(-p0[k]+3*p1[k]-3*p2[k]+p3[k])*t**3) for k in (0,1)))
    out.append(pts[-1]); return out
def bez(pts):
    P=[pts[0]]+pts+[pts[-1]]; d=f"M{pts[0][0]:.1f},{pts[0][1]:.1f}"
    for i in range(1,len(P)-2):
        p0,p1,p2,p3=P[i-1],P[i],P[i+1],P[i+2]
        c1=(p1[0]+(p2[0]-p0[0])/6,p1[1]+(p2[1]-p0[1])/6); c2=(p2[0]-(p3[0]-p1[0])/6,p2[1]-(p3[1]-p1[1])/6)
        d+=f" C{c1[0]:.1f},{c1[1]:.1f} {c2[0]:.1f},{c2[1]:.1f} {p2[0]:.1f},{p2[1]:.1f}"
    return d
def poly_d(pts,close=True): return "M"+" L".join(f"{x:.1f},{y:.1f}" for x,y in pts)+(" Z" if close else "")
def circle_d(cx,cy,r): return f"M{cx-r:.1f},{cy:.1f} a{r:.1f},{r:.1f} 0 1,0 {2*r:.1f},0 a{r:.1f},{r:.1f} 0 1,0 {-2*r:.1f},0 Z"

# ---------- geography ----------
COAST=[(-124.21,42.00),(-124.41,40.44),(-123.65,38.80),(-123.00,38.00),(-122.51,37.77),(-122.20,37.00),(-121.85,36.80),(-121.90,36.30),(-120.85,35.30),(-120.47,34.45),(-119.70,34.40),(-119.10,34.10),(-118.85,34.02),(-118.40,33.74),(-117.60,33.38),(-117.25,32.72),(-117.12,32.53)]
BORDER=[(-117.12,32.53),(-114.72,32.72),(-114.63,35.00),(-120.00,39.00),(-120.00,42.00),(-124.21,42.00)]
K=math.cos(math.radians(37.5)); HEIGHT=60.0
# The state's outline: white along the coast (against the blue ocean), dark brown along the land borders (against the tan land)
OUTLINE_COAST=(255,255,255); OUTLINE_LAND=(0x2E,0x1D,0x12); OUTLINE_ALPHA=255; OUTLINE_W=1.8
_all=COAST+BORDER[1:]
_xs=[p[0]*K for p in _all]; _ys=[p[1] for p in _all]
_sc=HEIGHT/(max(_ys)-min(_ys)); _mx=(max(_xs)+min(_xs))/2; _my=(max(_ys)+min(_ys))/2
def proj(p): return (54+(p[0]*K-_mx)*_sc, 54-(p[1]-_my)*_sc)
COAST_XY=[proj(p) for p in COAST]; STATE_XY=[proj(p) for p in _all]; LAND_XY=[proj(p) for p in BORDER]

# ---------- palette ----------
LAND=((0xCF,0xC3,0xA0),(0xA8,0x98,0x70)); CONTOUR=(120,96,60)
OCEAN=((0x0A,0x3A,0x66),(0x2F,0x86,0xBB))
SKY=[(0.0,(0xBA,0xE6,0xFD)),(0.42,(0x02,0x84,0xC7)),(1.0,(0x08,0x20,0x3E))]
SUN_TOP=(0xFD,0xE0,0x47); SUN_BOT=(0xF5,0x9E,0x0B)
RIDGES=[((0x3B,0x8F,0xC4),[(-8,72),(8,62),(20,46),(30,58),(40,54),(54,66),(70,60),(84,44),(96,56),(116,66)]),
        ((0x1B,0x5E,0x8E),[(-8,82),(14,72),(30,78),(48,66),(66,76),(84,70),(102,80),(116,76)]),
        ((0x0A,0x30,0x52),[(-8,94),(16,88),(40,94),(62,86),(88,92),(116,86)])]
SNOW=(0xF1,0xF8,0xFF)
SNOW_CAPS=[[(15.2,52.2),(20,46),(24.8,52.2),(22.6,51),(20,53.4),(17.4,51)],[(79.2,50.2),(84,44),(88.8,50.2),(86.6,49),(84,51.4),(81.4,49)]]
SUN=(50,47,14)
CLOUD=[(53,32,6.5),(61,29.5,8.5),(69,32.5,6)]; CLOUD_BASE=(47.5,32.5,74.5,38.5,3)
# Hills in Nevada, as bell-shaped mounds: colour, (x0, x1, base y, height); drawn back to front
HILLS=[((0x8A,0xAD,0x72),(56,80,40,9)),((0x75,0x9B,0x62),(70,94,38,8)),((0x62,0x89,0x54),(58,84,55,9.5)),((0x4B,0x73,0x45),(76,102,57,8.5))]
TREE=(0x2D,0x5A,0x3A)
TREES=[(63,34),(68,32.2),(74,33.6),(78,31.4),(84,31),(89,33.4),(66,50),(71,47.6),(78,49),(83,50.6),(88,50),(94,52)]  # base points of small triangles
def mound(x0,x1,base,h):
    w=x1-x0
    return catmull([(x0,base+6),(x0+0.12*w,base),(x0+0.3*w,base-0.7*h),(x0+0.5*w,base-h),(x0+0.7*w,base-0.7*h),(x0+0.88*w,base),(x1,base+6)])

# The bear from the state flag, walking left on a strip of grass, in the north of the state (placed in canvas units)
SHOW_BEAR=True
BEAR=[(0.00,0.50),(0.04,0.40),(0.11,0.33),(0.15,0.22),(0.18,0.12),(0.23,0.22),(0.30,0.26),(0.40,0.12),(0.52,0.14),(0.66,0.20),(0.80,0.24),(0.92,0.32),(0.97,0.38),(0.92,0.46),(0.90,0.62),(0.93,0.80),(0.95,1.00),(0.80,1.00),(0.78,0.78),(0.70,0.72),(0.58,0.74),(0.50,0.76),(0.52,1.00),(0.36,1.00),(0.34,0.76),(0.26,0.72),(0.17,0.66),(0.10,0.62),(0.03,0.58)]
BEAR_BOX=(32.6,28.0,19.0,11.0)     # x, y, width, height of the bear
BEAR_COLOR=(0x4A,0x2C,0x17); BEAR_RIM=(0xF1,0xE3,0xC8)
GRASS=(0x5F,0x86,0x52); GRASS_EDGE=(0x3E,0x63,0x3A)
def bear_xy():
    x0,y0,w,h=BEAR_BOX; return [(x0+px*w,y0+py*h) for px,py in BEAR]
def grass_xy():
    x0,y0,w,h=BEAR_BOX; return [(x0-3,y0+h-0.8),(x0+w+3,y0+h-0.8),(x0+w+3,y0+h+3.2),(x0-3,y0+h+3.2)]

def scene_xf(p,s=0.62,off=(3,1)): return (54+(p[0]-54)*s+off[0], 54+(p[1]-54)*s+off[1])

# ---------- background ops ----------
def waves():
    """Pairs of short arcs in the ocean (clear of the coast): returns lists of points."""
    out=[]
    coast_x=lambda y: min(COAST_XY,key=lambda q:abs(q[1]-y))[0]
    for row in range(8):
        y=17+row*11
        for col in range(5):
            x=6+col*13+(row%2)*6
            if x+8 < coast_x(y)-5:
                for dy in (0,2.6):
                    out.append([(x+t*0.9, y+dy+1.1*math.sin(t/1.5*math.pi)) for t in range(0,9)])
    return out
def contours():
    ls=[]
    for i in range(9):
        base=-10+i*14
        ls.append([(x,base+8*math.sin(x/13+i)+6*math.sin(x/7.3+i*2)) for x in range(-10,125,6)])
    return ls

# ---------- Pillow backend ----------
def render(S=1080,mask=None,layers="both"):
    k=S/108
    def P(pts): return [(x*k,y*k) for x,y in pts]
    img=Image.new("RGBA",(S,S))
    d=ImageDraw.Draw(img)
    if layers in ("both","bg"):
        for y in range(S): d.line([(0,y),(S,y)],fill=lerp(LAND[0],LAND[1],y/S)+(255,))
        ov=Image.new("RGBA",(S,S),(0,0,0,0)); od=ImageDraw.Draw(ov)
        for ln in contours(): od.line(P(ln),fill=CONTOUR+(60,),width=max(1,int(0.6*k)),joint="curve")
        img=Image.alpha_composite(img,ov)
        # ocean (west of the coast)
        ocean=[(-30,-30),(COAST_XY[0][0],-30)]+COAST_XY+[(COAST_XY[-1][0],140),(-30,140)]
        grad=Image.new("RGBA",(S,S)); gd=ImageDraw.Draw(grad)
        for x in range(S): gd.line([(x,0),(x,S)],fill=lerp(OCEAN[0],OCEAN[1],min(1,x/(70*k)))+(255,))
        m=Image.new("L",(S,S),0); ImageDraw.Draw(m).polygon(P(ocean),fill=255)
        img=Image.composite(grad,img,m)
        wv=Image.new("RGBA",(S,S),(0,0,0,0)); wd=ImageDraw.Draw(wv)
        for w in waves(): wd.line(P(w),fill=(255,255,255,95),width=max(1,int(0.8*k)),joint="curve")
        img=Image.alpha_composite(img,wv)
        ImageDraw.Draw(img).line(P(COAST_XY),fill=(255,255,255,120),width=int(2.4*k),joint="curve")
        # hills and trees
        hv=Image.new("RGBA",(S,S),(0,0,0,0)); hd=ImageDraw.Draw(hv)
        for col,(x0,x1,bs,h) in HILLS:
            pts=mound(x0,x1,bs,h); hd.polygon(P(pts+[(x1,bs+30),(x0,bs+30)]),fill=col+(255,)) if False else hd.polygon(P(pts),fill=col+(255,))
        for tx,ty in TREES: hd.polygon(P([(tx,ty-3.0),(tx-1.5,ty+0.4),(tx+1.5,ty+0.4)]),fill=TREE+(255,))
        # keep the hills out of the state (they sit east of its border)
        sm=Image.new("L",(S,S),255); ImageDraw.Draw(sm).polygon(P(STATE_XY),fill=0)
        hv.putalpha(Image.composite(hv.getchannel("A"),Image.new("L",(S,S),0),sm))
        img=Image.alpha_composite(img,hv)
    if layers in ("both","fg"):
        sh=Image.new("RGBA",(S,S),(0,0,0,0)); ImageDraw.Draw(sh).polygon(P([(x+0.8,y+1.6) for x,y in STATE_XY]),fill=(0,0,0,110))
        img=Image.alpha_composite(img,sh.filter(ImageFilter.GaussianBlur(1.4*k)))
        sc=Image.new("RGBA",(S,S),(0,0,0,0)); sd=ImageDraw.Draw(sc)
        y0,y1=scene_xf((0,0))[1],scene_xf((0,108))[1]
        for y in range(int(y0*k),int(y1*k)):
            t=(y/k-y0)/(y1-y0)
            c=SKY[-1][1]
            for (o0,c0),(o1,c1) in zip(SKY,SKY[1:]):
                if t<=o1: c=lerp(c0,c1,(t-o0)/(o1-o0)); break
            sd.line([(0,y),(S,y)],fill=c+(255,))
        cx,cy,r=SUN; cx2,cy2=scene_xf((cx,cy)); r2=r*0.62
        sun=Image.new("RGBA",(S,S),(0,0,0,0)); sud=ImageDraw.Draw(sun)
        for yy in range(int((cy2-r2)*k),int((cy2+r2)*k)+1):
            t=(yy/k-(cy2-r2))/(2*r2); sud.line([(0,yy),(S,yy)],fill=lerp(SUN_TOP,SUN_BOT,min(max(t,0),1))+(255,))
        mm=Image.new("L",(S,S),0); ImageDraw.Draw(mm).ellipse([(cx2-r2)*k,(cy2-r2)*k,(cx2+r2)*k,(cy2+r2)*k],fill=255)
        sc=Image.alpha_composite(sc,Image.composite(sun,Image.new("RGBA",(S,S),(0,0,0,0)),mm)); sd=ImageDraw.Draw(sc)
        for col,line in RIDGES:
            pts=[scene_xf(p) for p in catmull(line)]
            sd.polygon(P(pts+[(pts[-1][0],130),(pts[0][0],130)]),fill=col+(255,))
        for cap in SNOW_CAPS: sd.polygon(P([scene_xf(p) for p in cap]),fill=SNOW+(255,))
        cl=Image.new("RGBA",(S,S),(0,0,0,0)); cd=ImageDraw.Draw(cl)
        for x,y,rr in CLOUD:
            a,b=scene_xf((x,y)); r3=rr*0.62; cd.ellipse([(a-r3)*k,(b-r3)*k,(a+r3)*k,(b+r3)*k],fill=(255,255,255,245))
        x0,yy0,x1,yy1,rad=CLOUD_BASE; a0,b0=scene_xf((x0,yy0)); a1,b1=scene_xf((x1,yy1))
        cd.rounded_rectangle([a0*k,b0*k,a1*k,b1*k],radius=rad*0.62*k,fill=(255,255,255,245))
        sc=Image.alpha_composite(sc,cl)
        if SHOW_BEAR:
            bl=Image.new("RGBA",(S,S),(0,0,0,0)); bd=ImageDraw.Draw(bl)
            bd.polygon(P(grass_xy()),fill=GRASS+(255,)); bd.line(P(grass_xy()[:2]),fill=GRASS_EDGE+(255,),width=int(0.8*k))
            bd.polygon(P(bear_xy()),fill=BEAR_COLOR+(255,),outline=BEAR_RIM+(255,))
            sc=Image.alpha_composite(sc,bl)
        wm=Image.new("L",(S,S),0); ImageDraw.Draw(wm).polygon(P(STATE_XY),fill=255)
        img=Image.alpha_composite(img,Image.composite(sc,Image.new("RGBA",(S,S),(0,0,0,0)),wm))
        od=ImageDraw.Draw(img)
        od.line(P(LAND_XY),fill=OUTLINE_LAND+(OUTLINE_ALPHA,),width=int(OUTLINE_W*k),joint="curve")
        od.line(P(COAST_XY),fill=OUTLINE_COAST+(OUTLINE_ALPHA,),width=int(OUTLINE_W*k),joint="curve")
        for q in (COAST_XY[0],COAST_XY[-1]):   # round the ends where the two colours meet
            for col in (OUTLINE_LAND,OUTLINE_COAST):
                r=OUTLINE_W*k/2; od.ellipse([q[0]*k-r,q[1]*k-r,q[0]*k+r,q[1]*k+r],fill=col+(OUTLINE_ALPHA,))
    if mask=="circle72":
        a=int(18*k); b=int(90*k); img=img.crop((a,a,b,b))
        m=Image.new("L",img.size,0); ImageDraw.Draw(m).ellipse([0,0,img.size[0]-1,img.size[1]-1],fill=255); img.putalpha(m)
    return img


HEAD='''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
'''
def grad(x0,y0,x1,y1,stops):
    items="\n".join(f'                <item android:offset="{o}" android:color="{hexc(c)}" />' for o,c in stops)
    return f'''        <aapt:attr name="android:fillColor">
            <gradient
                android:type="linear"
                android:startX="{x0:.1f}"
                android:startY="{y0:.1f}"
                android:endX="{x1:.1f}"
                android:endY="{y1:.1f}">
{items}
            </gradient>
        </aapt:attr>
'''
def fill(d,color,alpha=None,comment=None):
    c=hexc(color) if alpha is None else alpha_hex(color,alpha)
    return (f"    <!-- {comment} -->\n" if comment else "")+f'    <path\n        android:fillColor="{c}"\n        android:pathData="{d}" />\n'
def gfill(d,g,comment=None):
    return (f"    <!-- {comment} -->\n" if comment else "")+f'    <path android:pathData="{d}">\n{g}    </path>\n'
def stroke(d,color,alpha,width,comment=None):
    return (f"    <!-- {comment} -->\n" if comment else "")+f'    <path\n        android:pathData="{d}"\n        android:strokeColor="{alpha_hex(color,alpha)}"\n        android:strokeWidth="{width}"\n        android:strokeLineCap="round"\n        android:strokeLineJoin="round" />\n'

def background():
    x=HEAD
    x+="    <!-- Cali Climates launcher icon, background layer: California on a map. Land (muted desert) with faint\n         contour lines, the Pacific to the west of the coast with wave marks and a surf line, and hills with trees\n         in Nevada. The state itself is the foreground layer. Generated; see docs/images/README.md. -->\n"
    x+=gfill("M-10,-10 H118 V118 H-10 Z",grad(0,0,0,108,[(0.0,LAND[0]),(1.0,LAND[1])]),"Land")
    for ln in contours():
        x+=stroke(poly_d(ln,close=False),CONTOUR,60/255,0.6)
    ocean=[(-30,-30),(COAST_XY[0][0],-30)]+COAST_XY+[(COAST_XY[-1][0],140),(-30,140)]
    x+=gfill(poly_d(ocean),grad(0,0,70,0,[(0.0,OCEAN[0]),(1.0,OCEAN[1])]),"The Pacific, west of the coastline")
    for w in waves(): x+=stroke(poly_d(w,close=False),(255,255,255),95/255,0.8)
    x+=stroke(poly_d(COAST_XY,close=False),(255,255,255),120/255,2.4,"Surf line along the coast")
    x+="    <!-- Hills and trees in Nevada -->\n"
    for col,(x0,x1,bs,h) in HILLS:
        pts=[(round(a,1),round(b,1)) for a,b in catmull_ctrl(x0,x1,bs,h)]
        x+=fill(bez(pts)+" Z",col)
    for tx,ty in TREES:
        x+=fill(poly_d([(tx,ty-3.0),(tx-1.5,ty+0.4),(tx+1.5,ty+0.4)]),TREE)
    return x+"</vector>\n"

def catmull_ctrl(x0,x1,base,h):
    w=x1-x0
    return [(x0,base+6),(x0+0.12*w,base),(x0+0.3*w,base-0.7*h),(x0+0.5*w,base-h),(x0+0.7*w,base-0.7*h),(x0+0.88*w,base),(x1,base+6)]

def rrect_d(x0,y0,x1,y1,r):
    return (f"M{x0+r:.1f},{y0:.1f} H{x1-r:.1f} a{r:.1f},{r:.1f} 0 0,1 {r:.1f},{r:.1f} V{y1-r:.1f} a{r:.1f},{r:.1f} 0 0,1 {-r:.1f},{r:.1f} "
            f"H{x0+r:.1f} a{r:.1f},{r:.1f} 0 0,1 {-r:.1f},{-r:.1f} V{y0+r:.1f} a{r:.1f},{r:.1f} 0 0,1 {r:.1f},{-r:.1f} Z")

def foreground(standalone=True):
    x=HEAD if standalone else ""
    x+="    <!-- California, as a window onto the scene: a sun between two snow-capped peaks over three ridges, a small cloud.\n         Drop shadow, the scene clipped to the state's outline, then a white outline. -->\n"
    x+=fill(poly_d([(a+0.8,b+1.6) for a,b in STATE_XY]),(0,0,0),110/255,"Soft shadow under the state")
    x+=f'    <group>\n        <clip-path android:pathData="{poly_d(STATE_XY)}" />\n'
    y0=scene_xf((0,0))[1]; y1=scene_xf((0,108))[1]
    sk=grad(0,y0,0,y1,SKY).replace("\n    ","\n    ")
    def ind(s): return "".join(("    "+l if l.strip() else l) for l in s.splitlines(True))
    x+=ind(gfill(f"M-10,{y0:.1f} H130 V{y1:.1f} H-10 Z",sk,"Sky"))
    cx,cy,r=SUN; a,b=scene_xf((cx,cy)); r2=r*0.62
    x+=ind(gfill(circle_d(a,b,r2),grad(0,b-r2,0,b+r2,[(0.0,SUN_TOP),(1.0,SUN_BOT)]),"Sun"))
    for col,line in RIDGES:
        pts=[scene_xf(p) for p in line]
        d=bez([(round(p[0],1),round(p[1],1)) for p in pts])+f" L{pts[-1][0]:.1f},130 L{pts[0][0]:.1f},130 Z"
        x+=ind(fill(d,col))
    for cap in SNOW_CAPS: x+=ind(fill(poly_d([scene_xf(p) for p in cap]),SNOW))
    for cxx,cyy,rr in CLOUD:
        a,b=scene_xf((cxx,cyy)); x+=ind(fill(circle_d(a,b,rr*0.62),(255,255,255),245/255))
    x0,yy0,x1,yy1,rad=CLOUD_BASE; a0,b0=scene_xf((x0,yy0)); a1,b1=scene_xf((x1,yy1))
    x+=ind(fill(rrect_d(a0,b0,a1,b1,rad*0.62),(255,255,255),245/255,"Cloud"))
    if SHOW_BEAR:
        x+=ind(fill(poly_d(grass_xy()),GRASS,comment="Grass under the bear"))
        x+=ind(fill(poly_d(bear_xy()),BEAR_COLOR,comment="The California bear, walking left"))
    x+="    </group>\n"
    x+=stroke(poly_d(LAND_XY,close=False),OUTLINE_LAND,OUTLINE_ALPHA/255,OUTLINE_W,"Outline along the land borders")
    x+=stroke(poly_d(COAST_XY,close=False),OUTLINE_COAST,OUTLINE_ALPHA/255,OUTLINE_W,"Outline along the coast")
    return x+("</vector>\n" if standalone else "")

def composite():
    bg=background().rsplit("</vector>",1)[0]
    return bg+foreground(standalone=False)+"</vector>\n"

def monochrome():
    a,b=scene_xf(SUN[:2]); r2=SUN[2]*0.62
    d=poly_d(STATE_XY)+" "+circle_d(a,b,r2)
    return HEAD+'    <!-- Themed (monochrome) icon, Android 13: the state with the sun cut out. -->\n    <path\n        android:fillColor="#FF000000"\n        android:fillType="evenOdd"\n        android:pathData="'+d+'" />\n</vector>\n'



REPO = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
RES = os.path.join(REPO, "app", "src", "main", "res")
LEGACY_SIZES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}

def write(path, text):
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(text)

def rasters():
    full = render(1080)
    core = full.crop((180, 180, 900, 900))          # the middle 72 of 108 units, which is what a launcher shows
    for density, px in LEGACY_SIZES.items():
        folder = os.path.join(RES, "mipmap-" + density)
        art = core.resize((px, px), Image.LANCZOS)
        square = art.copy()
        m = Image.new("L", (px * 4, px * 4), 0)
        ImageDraw.Draw(m).rounded_rectangle([0, 0, px * 4 - 1, px * 4 - 1], radius=int(px * 4 * 0.22), fill=255)
        square.putalpha(m.resize((px, px), Image.LANCZOS))
        square.save(os.path.join(folder, "ic_launcher.webp"), lossless=True)
        rnd = art.copy()
        c = Image.new("L", (px * 4, px * 4), 0)
        ImageDraw.Draw(c).ellipse([0, 0, px * 4 - 1, px * 4 - 1], fill=255)
        rnd.putalpha(c.resize((px, px), Image.LANCZOS))
        rnd.save(os.path.join(folder, "ic_launcher_round.webp"), lossless=True)
    core.resize((512, 512), Image.LANCZOS).save(os.path.join(REPO, "app", "src", "main", "ic_launcher-playstore.png"), optimize=True)

if __name__ == "__main__":
    drawable = os.path.join(RES, "drawable")
    write(os.path.join(drawable, "ic_launcher_background.xml"), background())
    write(os.path.join(drawable, "ic_launcher_foreground.xml"), foreground())
    write(os.path.join(drawable, "ic_launcher_vector.xml"), composite())
    write(os.path.join(drawable, "ic_launcher_monochrome.xml"), monochrome())
    rasters()
    print("wrote the icon's vector layers, the fallback launcher images and the Play Store icon")
