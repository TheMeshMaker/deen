"""Builds Deen's launcher icon and status-bar icon.

The mark is the letter nun (ن), the last letter of دين: its bowl is a crescent and its diamond dot a
star, inside a thin eight-pointed star, on the emerald gradient. Writes an HTML preview plus the
vector drawables (108x108 adaptive-icon viewport; safe zone r=33 around 54,54).

Usage (from the repository root):
  python -I tools/icon/build_icon.py <preview dir> app/src/main/res \
      core/alarm/src/main/res/drawable/ic_stat_deen.xml core/designsystem/src/main/res/drawable/deen_mark.xml
"""
import math
import sys

OUT_DIR = sys.argv[1]
RES = sys.argv[2] if len(sys.argv) > 2 else None


def f(v):
    return f"{v:.2f}".rstrip("0").rstrip(".")


def crescent(c1, r1, c2, r2):
    """Outer circle c1/r1 minus the offset circle c2/r2 (c2 inside c1's upper part): one closed path."""
    (x1, y1), (x2, y2) = c1, c2
    d = math.hypot(x2 - x1, y2 - y1)
    a = (r1 * r1 - r2 * r2 + d * d) / (2 * d)
    h = math.sqrt(r1 * r1 - a * a)
    xm, ym = x1 + a * (x2 - x1) / d, y1 + a * (y2 - y1) / d
    p1 = (xm + h * (y2 - y1) / d, ym - h * (x2 - x1) / d)
    p2 = (xm - h * (y2 - y1) / d, ym + h * (x2 - x1) / d)
    return (f"M{f(p1[0])},{f(p1[1])} A{f(r1)},{f(r1)} 0 1,0 {f(p2[0])},{f(p2[1])} "
            f"A{f(r2)},{f(r2)} 0 1,1 {f(p1[0])},{f(p1[1])} Z")


def star8(cx, cy, r_out, r_in_ratio=0.7654, rot=22.5):
    pts = []
    r_in = r_out * r_in_ratio
    for i in range(16):
        r = r_out if i % 2 == 0 else r_in
        ang = math.radians(rot + i * 22.5 - 90)
        pts.append((cx + r * math.cos(ang), cy + r * math.sin(ang)))
    return "M" + " L".join(f"{f(x)},{f(y)}" for x, y in pts) + " Z"


def star8_ring(cx, cy, r_out, width):
    """The star's outline as a filled ring (outer star minus a smaller star), so it scales like a shape."""
    inner = r_out - width / math.sin(math.radians(67.5))
    return star8(cx, cy, r_out) + " " + star8(cx, cy, inner)


def diamond(cx, cy, half_w, half_h):
    return f"M{f(cx)},{f(cy - half_h)} L{f(cx + half_w)},{f(cy)} L{f(cx)},{f(cy + half_h)} L{f(cx - half_w)},{f(cy)} Z"


CX, CY = 54.0, 54.0
# Bowl: outer circle minus an inner one shifted up and a little right, so the stroke is thickest
# low on the left like a pen-drawn nun, thinning to two points.
BOWL_R = 15.5
BOWL_C = (CX, CY + 2.0)
INNER_R = 13.9
INNER_C = (CX + 1.2, CY - 3.1)
bowl = crescent(BOWL_C, BOWL_R, INNER_C, INNER_R)
dot = diamond(CX + 0.8, CY - 7.6, 3.6, 4.2)
ring = star8_ring(CX, CY, 31.0, 1.7)

GOLD_A, GOLD_B = "#FFF4D6", "#E3B858"
BG_A, BG_B = "#03392B", "#0E8A62"


def svg_icon(mask=None, mono=False, size=216):
    fg = "#FFFFFF" if mono else "url(#gold)"
    clip = {
        None: "",
        "circle": '<clipPath id="m"><circle cx="54" cy="54" r="36"/></clipPath>',
        "squircle": '<clipPath id="m"><rect x="18" y="18" width="72" height="72" rx="22"/></clipPath>',
    }[mask]
    use_clip = ' clip-path="url(#m)"' if mask else ""
    bg = '<rect width="108" height="108" fill="#222"/>' if mono else (
        '<rect width="108" height="108" fill="url(#bg)"/><rect width="108" height="108" fill="url(#glow)"/>')
    return f"""<svg xmlns="http://www.w3.org/2000/svg" width="{size}" height="{size}" viewBox="18 18 72 72">
<defs>
<linearGradient id="bg" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="{BG_A}"/><stop offset="1" stop-color="{BG_B}"/></linearGradient>
<radialGradient id="glow" cx="54" cy="46" r="40" gradientUnits="userSpaceOnUse"><stop offset="0" stop-color="#7FFFD0" stop-opacity="0.22"/><stop offset="1" stop-color="#7FFFD0" stop-opacity="0"/></radialGradient>
<linearGradient id="gold" x1="40" y1="24" x2="70" y2="84" gradientUnits="userSpaceOnUse"><stop offset="0" stop-color="{GOLD_A}"/><stop offset="1" stop-color="{GOLD_B}"/></linearGradient>
{clip}
</defs>
<g{use_clip}>{bg}
<path d="{ring}" fill="{fg}" fill-rule="evenodd" opacity="0.9"/>
<path d="{bowl}" fill="{fg}"/>
<path d="{dot}" fill="{fg}"/>
</g></svg>"""


page = "<html><body style='background:#ddd;display:flex;gap:24px;flex-wrap:wrap;padding:24px'>"
for mask in ("circle", "squircle"):
    page += svg_icon(mask)
page += svg_icon("circle", size=48) + svg_icon("squircle", size=48) + svg_icon("circle", mono=True) + svg_icon(None)
page += "</body></html>"
open(f"{OUT_DIR}/nun_preview.html", "w", encoding="utf-8").write(page)

if RES:
    head = '<?xml version="1.0" encoding="utf-8"?>\n'
    open(f"{RES}/drawable/ic_launcher_background.xml", "w", encoding="utf-8", newline="\n").write(head + f"""<!-- Emerald gradient (the default theme's header) with a soft light behind the mark. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path android:pathData="M0,0 L108,0 L108,108 L0,108 Z">
        <aapt:attr name="android:fillColor">
            <gradient
                android:endX="108"
                android:endY="108"
                android:startX="0"
                android:startY="0"
                android:type="linear">
                <item
                    android:color="#FF{BG_A[1:]}"
                    android:offset="0" />
                <item
                    android:color="#FF{BG_B[1:]}"
                    android:offset="1" />
            </gradient>
        </aapt:attr>
    </path>
    <path android:pathData="M0,0 L108,0 L108,108 L0,108 Z">
        <aapt:attr name="android:fillColor">
            <gradient
                android:centerX="54"
                android:centerY="46"
                android:gradientRadius="40"
                android:type="radial">
                <item
                    android:color="#387FFFD0"
                    android:offset="0" />
                <item
                    android:color="#007FFFD0"
                    android:offset="1" />
            </gradient>
        </aapt:attr>
    </path>
</vector>
""")

    def gold():
        return f"""        <aapt:attr name="android:fillColor">
            <gradient
                android:endX="70"
                android:endY="84"
                android:startX="40"
                android:startY="24"
                android:type="linear">
                <item
                    android:color="#FF{GOLD_A[1:]}"
                    android:offset="0" />
                <item
                    android:color="#FF{GOLD_B[1:]}"
                    android:offset="1" />
            </gradient>
        </aapt:attr>"""

    open(f"{RES}/drawable/ic_launcher_foreground.xml", "w", encoding="utf-8", newline="\n").write(head + f"""<!--
  The letter nun (ن), the last letter of دين: its bowl is a crescent and its diamond dot a star,
  inside an eight-pointed star outline. Generated by tools/icon/build_icon.py; edit the script.
-->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillAlpha="0.9"
        android:fillType="evenOdd"
        android:pathData="{ring}">
{gold()}
    </path>
    <path android:pathData="{bowl}">
{gold()}
    </path>
    <path android:pathData="{dot}">
{gold()}
    </path>
</vector>
""")
    open(f"{RES}/drawable/ic_launcher_monochrome.xml", "w", encoding="utf-8", newline="\n").write(head + f"""<!-- Themed-icon version of ic_launcher_foreground (one colour, chosen by the launcher). -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#FFFFFFFF"
        android:fillType="evenOdd"
        android:pathData="{ring}" />
    <path
        android:fillColor="#FFFFFFFF"
        android:pathData="{bowl}" />
    <path
        android:fillColor="#FFFFFFFF"
        android:pathData="{dot}" />
</vector>
""")
    # Status-bar icon (24dp): the nun alone, scaled to fill the frame.
    k, ref_y = 0.56, 50.5
    def t(x, y):
        return 12 + (x - CX) * k, 12 + (y - ref_y) * k
    sb = crescent(t(*BOWL_C), BOWL_R * k, t(*INNER_C), INNER_R * k)
    dx, dy = t(CX + 0.8, CY - 7.6)
    sd = diamond(dx, dy, 3.6 * k * 1.1, 4.2 * k * 1.1)
    open(sys.argv[3], "w", encoding="utf-8", newline="\n").write(head + f"""<!-- Status-bar icon: the nun (ن) of the launcher icon, white on transparent. Generated with the launcher icon. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FFFFFFFF"
        android:pathData="{sb}" />
    <path
        android:fillColor="#FFFFFFFF"
        android:pathData="{sd}" />
</vector>
""")
if len(sys.argv) > 4:
    # The mark alone (gold, no background) for in-app use, cropped to the star.
    open(sys.argv[4], "w", encoding="utf-8", newline="\n").write(head + f"""<!-- Deen's mark (the launcher icon without its background). Generated by tools/icon/build_icon.py. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="66dp"
    android:height="66dp"
    android:viewportWidth="66"
    android:viewportHeight="66">
    <group
        android:translateX="-21"
        android:translateY="-21">
        <path
            android:fillAlpha="0.9"
            android:fillType="evenOdd"
            android:pathData="{ring}">
{gold()}
        </path>
        <path android:pathData="{bowl}">
{gold()}
        </path>
        <path android:pathData="{dot}">
{gold()}
        </path>
    </group>
</vector>
""")
print("ok")
