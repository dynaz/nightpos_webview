#!/usr/bin/env python3
"""
Generate simple PNG icons for the NightPOS Local Printer extension.
Requires: pip install Pillow

Run from chrome_extension/ directory:
    python generate_icons.py
"""

try:
    from PIL import Image, ImageDraw, ImageFont
except ImportError:
    print("ERROR: Pillow not installed. Run: pip install Pillow")
    raise SystemExit(1)

import os

SIZES  = [16, 48, 128]
BG     = (28, 22, 45)        # dark purple
ACCENT = (124, 92, 191)      # purple accent
WHITE  = (232, 224, 245)

def make_icon(size):
    img  = Image.new("RGBA", (size, size), BG)
    draw = ImageDraw.Draw(img)

    # Rounded rect background (printer body)
    r  = max(2, size // 8)
    p  = max(2, size // 6)
    draw.rounded_rectangle([p, p, size - p, size - p], radius=r, fill=ACCENT)

    # Printer slot (dark rectangle in lower half)
    slot_h = max(2, size // 6)
    slot_y = size // 2
    slot_x = size // 4
    draw.rectangle(
        [slot_x, slot_y, size - slot_x, slot_y + slot_h],
        fill=BG,
    )

    # Paper strip coming out
    paper_w = size // 3
    paper_x = (size - paper_w) // 2
    draw.rectangle(
        [paper_x, slot_y - slot_h, paper_x + paper_w, slot_y],
        fill=WHITE,
    )

    return img

os.makedirs("icons", exist_ok=True)
for sz in SIZES:
    icon = make_icon(sz)
    path = f"icons/icon{sz}.png"
    icon.save(path)
    print(f"  Created {path}")

print("Done.")
