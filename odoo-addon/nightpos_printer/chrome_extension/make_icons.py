#!/usr/bin/env python3
"""Generate icons using only the Python standard library (no Pillow needed)."""
import struct, zlib, os

def png(width, height, pixels_rgba):
    def chunk(name, data):
        c = name + data
        return struct.pack('>I', len(data)) + c + struct.pack('>I', zlib.crc32(c) & 0xFFFFFFFF)
    raw = b''
    for y in range(height):
        raw += b'\x00'
        for x in range(width):
            raw += bytes(pixels_rgba[y * width + x])
    return b'\x89PNG\r\n\x1a\n' + \
           chunk(b'IHDR', struct.pack('>IIBBBBB', width, height, 8, 2, 0, 0, 0)) + \
           chunk(b'IDAT', zlib.compress(raw)) + \
           chunk(b'IEND', b'')

def make_icon(size):
    BG     = (28,  22,  45,  255)
    PURPLE = (124, 92,  191, 255)
    LIGHT  = (180, 160, 230, 255)
    WHITE  = (232, 224, 245, 255)
    DARK   = (18,  14,  30,  255)

    pixels = [BG] * (size * size)

    def set_px(x, y, color):
        if 0 <= x < size and 0 <= y < size:
            pixels[y * size + x] = color

    def fill_rect(x0, y0, x1, y1, color):
        for y in range(max(0, y0), min(size, y1)):
            for x in range(max(0, x0), min(size, x1)):
                pixels[y * size + x] = color

    def fill_rounded(x0, y0, x1, y1, r, color):
        fill_rect(x0 + r, y0,     x1 - r, y1, color)
        fill_rect(x0,     y0 + r, x1,     y1 - r, color)
        for dy in range(r):
            for dx in range(r):
                if dx * dx + dy * dy <= r * r:
                    set_px(x0 + r - 1 - dx, y0 + r - 1 - dy, color)
                    set_px(x1 - r + dx,     y0 + r - 1 - dy, color)
                    set_px(x0 + r - 1 - dx, y1 - r + dy,     color)
                    set_px(x1 - r + dx,     y1 - r + dy,     color)

    p = max(1, size // 8)
    r = max(2, size // 6)

    # Printer body
    fill_rounded(p, p, size - p, size - p, r, PURPLE)

    # Paper feed slot (dark bar across middle)
    slot_y  = size * 5 // 10
    slot_h  = max(2, size // 8)
    slot_x  = size // 5
    fill_rect(slot_x, slot_y, size - slot_x, slot_y + slot_h, DARK)

    # Paper strip coming out the top of slot
    paper_w = max(2, size // 3)
    paper_x = (size - paper_w) // 2
    paper_y = slot_y - slot_h - max(1, size // 10)
    fill_rect(paper_x, paper_y, paper_x + paper_w, slot_y, WHITE)

    # Small dots on printer body (button indicators) — only for larger sizes
    if size >= 48:
        btn_y = size * 7 // 10
        for i in range(3):
            bx = size // 3 + i * (size // 6)
            set_px(bx, btn_y, LIGHT)
            if size >= 128:
                fill_rect(bx - 1, btn_y - 1, bx + 2, btn_y + 2, LIGHT)

    return pixels

os.makedirs("icons", exist_ok=True)
for sz in [16, 48, 128]:
    pixels = make_icon(sz)
    data   = png(sz, sz, pixels)
    path   = f"icons/icon{sz}.png"
    with open(path, "wb") as f:
        f.write(data)
    print(f"  Created {path}  ({len(data)} bytes)")

print("Done — reload the extension at chrome://extensions")
