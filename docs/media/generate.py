import math, os
OUT = os.path.dirname(os.path.abspath(__file__))
COLS = ["#FF5A5F","#FFB400","#3DDC97","#2EC4F1","#7B61FF","#FF7AC6","#FFD23F","#1FB57A"]

def wheel(cx, cy, r, weights=None, labels=None, rot=0, frame="#2B1B0E", hub="#FFD23F"):
    n = len(weights) if weights else 8
    w = weights or [1]*n
    tot = sum(w); a = rot - 90; parts = []
    for i, wi in enumerate(w):
        sweep = 360*wi/tot
        a0, a1 = math.radians(a), math.radians(a+sweep)
        x0, y0 = cx+r*math.cos(a0), cy+r*math.sin(a0)
        x1, y1 = cx+r*math.cos(a1), cy+r*math.sin(a1)
        large = 1 if sweep > 180 else 0
        parts.append(f'<path d="M{cx},{cy} L{x0:.1f},{y0:.1f} A{r},{r} 0 {large} 1 {x1:.1f},{y1:.1f} Z" fill="{COLS[i%len(COLS)]}" stroke="{frame}" stroke-width="{r*0.03:.1f}" stroke-linejoin="round"/>')
        if labels:
            m = math.radians(a+sweep/2); lr = r*0.66
            tx, ty = cx+lr*math.cos(m), cy+lr*math.sin(m)
            parts.append(f'<text x="{tx:.1f}" y="{ty:.1f}" font-size="{r*0.11:.0f}" fill="#1a1208" font-weight="900" text-anchor="middle" dominant-baseline="middle" transform="rotate({(a+sweep/2+180) if 90<((a+sweep/2)%360)<270 else (a+sweep/2):.1f} {tx:.1f} {ty:.1f})">{labels[i]}</text>')
        a += sweep
    s = "".join(parts)
    # frame + studs + hub + pointer
    studs = "".join(f'<circle cx="{cx+r*1.07*math.cos(math.radians(k*30)):.1f}" cy="{cy+r*1.07*math.sin(math.radians(k*30)):.1f}" r="{r*0.035:.1f}" fill="#FFE9A8"/>' for k in range(12))
    return (f'<circle cx="{cx}" cy="{cy}" r="{r*1.14}" fill="{frame}"/>'
            f'<circle cx="{cx}" cy="{cy}" r="{r*1.14}" fill="none" stroke="#FFB400" stroke-width="{r*0.025}"/>'
            + s + studs +
            f'<circle cx="{cx}" cy="{cy}" r="{r*0.17}" fill="{hub}" stroke="{frame}" stroke-width="{r*0.04}"/>'
            f'<circle cx="{cx}" cy="{cy}" r="{r*0.06}" fill="{frame}"/>'
            f'<path d="M{cx-r*0.13},{cy-r*1.26} L{cx+r*0.13},{cy-r*1.26} L{cx},{cy-r*0.98} Z" fill="#FFFFFF" stroke="{frame}" stroke-width="{r*0.03}" stroke-linejoin="round"/>')

BG = '''<defs>
<radialGradient id="bg" cx="50%" cy="45%" r="75%"><stop offset="0" stop-color="#3A2470"/><stop offset="0.55" stop-color="#1B1036"/><stop offset="1" stop-color="#0C0718"/></radialGradient>
<radialGradient id="glow" cx="50%" cy="50%" r="50%"><stop offset="0" stop-color="#FFB400" stop-opacity="0.55"/><stop offset="1" stop-color="#FFB400" stop-opacity="0"/></radialGradient>
<pattern id="grid" width="48" height="48" patternUnits="userSpaceOnUse"><path d="M48 0H0V48" fill="none" stroke="#ffffff" stroke-opacity="0.04" stroke-width="2"/></pattern>
</defs>'''
FONT = "font-family=\"'Segoe UI Black','Arial Black',Impact,sans-serif\""

def sparkles(pts):
    return "".join(f'<path d="M{x},{y-s} L{x+s*0.25},{y-s*0.25} L{x+s},{y} L{x+s*0.25},{y+s*0.25} L{x},{y+s} L{x-s*0.25},{y+s*0.25} L{x-s},{y} L{x-s*0.25},{y-s*0.25} Z" fill="{c}"/>' for x,y,s,c in pts)

def write(name, w, h, body):
    open(os.path.join(OUT, name+".svg"), "w", encoding="utf-8").write(
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="0 0 {w} {h}" {FONT}>{BG}{body}</svg>')

# icon 512
write("icon", 512, 512,
    '<rect width="512" height="512" rx="96" fill="url(#bg)"/><circle cx="256" cy="270" r="230" fill="url(#glow)"/>'
    + wheel(256, 276, 172, rot=8)
    + sparkles([(70,80,26,"#FFD23F"),(450,110,18,"#FFFFFF"),(440,440,22,"#FF7AC6")]))

def card(name, title, sub, body):
    write(name, 1920, 1080,
        '<rect width="1920" height="1080" fill="url(#bg)"/><rect width="1920" height="1080" fill="url(#grid)"/>'
        + body +
        f'<text x="110" y="250" font-size="104" fill="#FFFFFF">{title[0]}</text>'
        f'<text x="110" y="370" font-size="104" fill="#FFD23F">{title[1]}</text>'
        + "".join(f'<text x="114" y="{480+i*62}" font-size="42" fill="#CFC3F2" font-family="Segoe UI,Arial,sans-serif" font-weight="600">{line}</text>' for i,line in enumerate(sub))
        + '<text x="114" y="990" font-size="34" fill="#8E7FC0" font-family="Segoe UI,Arial,sans-serif" font-weight="700">SunshineWheel · free &amp; open source · Paper 1.21+</text>')

card("1_hero", ("Spin wheels", "in your world"),
     ["Place a real animated wheel,", "fill it with rewards,", "players right-click to spin.", "No client mods."],
     '<circle cx="1390" cy="560" r="470" fill="url(#glow)"/>' + wheel(1390, 575, 330, rot=14)
     + sparkles([(1000,170,34,"#FFD23F"),(1790,240,26,"#FFFFFF"),(1800,900,30,"#FF7AC6"),(1010,930,22,"#3DDC97")]))

card("2_odds", ("Slices follow", "the real odds"),
     ["A 5% prize gets a thin slice.", "What players see is", "what they can win.", "Left-click for the chance menu."],
     '<circle cx="1390" cy="560" r="470" fill="url(#glow)"/>'
     + wheel(1390, 575, 330, weights=[40,25,15,10,5,5], labels=["40%","25%","15%","10%","5%","5%"], rot=0))

def editor():
    x,y,W,H = 1010,150,800,800
    rows = [("Diamond x3","20%","#2EC4F1"),("1000 coins","35%","#FFB400"),("Rare key","10%","#7B61FF"),("XP bottle x16","30%","#3DDC97"),("Jackpot","5%","#FF5A5F")]
    s = f'<rect x="{x}" y="{y}" width="{W}" height="{H}" rx="28" fill="#140C2A" stroke="#4B3A86" stroke-width="3"/>'
    s += f'<rect x="{x}" y="{y}" width="{W}" height="70" rx="28" fill="#22164A"/><rect x="{x}" y="{y+40}" width="{W}" height="30" fill="#22164A"/>'
    s += "".join(f'<circle cx="{x+40+i*30}" cy="{y+35}" r="9" fill="{c}"/>' for i,c in enumerate(["#FF5A5F","#FFD23F","#3DDC97"]))
    s += f'<text x="{x+140}" y="{y+46}" font-size="28" fill="#CFC3F2" font-family="Segoe UI,Arial,sans-serif" font-weight="700">Wheel editor · daily</text>'
    for i,(n,p,c) in enumerate(rows):
        ry = y+110+i*96
        s += f'<rect x="{x+30}" y="{ry}" width="{W-60}" height="76" rx="16" fill="#1E1440"/>'
        s += f'<rect x="{x+48}" y="{ry+18}" width="40" height="40" rx="8" fill="{c}"/>'
        s += f'<text x="{x+108}" y="{ry+50}" font-size="32" fill="#FFFFFF" font-family="Segoe UI,Arial,sans-serif" font-weight="700">{n}</text>'
        s += f'<text x="{x+W-60}" y="{ry+50}" font-size="32" fill="#FFD23F" text-anchor="end" font-family="Segoe UI,Arial,sans-serif" font-weight="800">{p}</text>'
    s += f'<rect x="{x+30}" y="{y+H-130}" width="360" height="84" rx="18" fill="#3DDC97"/><text x="{x+210}" y="{y+H-76}" font-size="34" fill="#0C2A1C" text-anchor="middle">Save</text>'
    s += f'<rect x="{x+410}" y="{y+H-130}" width="360" height="84" rx="18" fill="#FFB400"/><text x="{x+590}" y="{y+H-76}" font-size="34" fill="#2B1B0E" text-anchor="middle">Test spin</text>'
    return s
card("3_editor", ("Edit in your", "browser"),
     ["Rewards, costs, colours and effects.", "Live preview and test spins.", "Saving updates the wheel", "in game right away."],
     editor())
