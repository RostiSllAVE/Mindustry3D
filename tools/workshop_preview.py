"""Generate our own small Workshop preview using only the Python standard library."""
import struct
import zlib

def preview():
    size = 512
    pixels = bytearray(bytes((19, 26, 38)) * size * size)
    def rect(x, y, w, h, color):
        for row in range(max(0, y), min(size, y + h)):
            for col in range(max(0, x), min(size, x + w)):
                offset = (row * size + col) * 3
                pixels[offset:offset + 3] = bytes(color)
    glyphs = {
        'M':['10001','11011','10101','10101','10001','10001','10001'],
        'I':['111','010','010','010','010','010','111'],
        'N':['10001','11001','11001','10101','10011','10011','10001'],
        'D':['1110','1001','1001','1001','1001','1001','1110'],
        'U':['1001','1001','1001','1001','1001','1001','0110'],
        'S':['0111','1000','1000','0110','0001','0001','1110'],
        'T':['11111','00100','00100','00100','00100','00100','00100'],
        'R':['1110','1001','1001','1110','1010','1001','1001'],
        'Y':['10001','10001','01010','00100','00100','00100','00100'],
        '3':['1110','0001','0001','0110','0001','0001','1110'],
        'F':['1111','1000','1000','1110','1000','1000','1000'],
        'P':['1110','1001','1001','1110','1000','1000','1000'],
        'V':['10001','10001','10001','10001','10001','01010','00100'],
        ' ':['000']*7,
    }
    def text(value, y, scale, color):
        width = sum(len(glyphs[c][0]) + 1 for c in value) * scale
        x = (size - width) // 2
        for c in value:
            for row, line in enumerate(glyphs[c]):
                for col, bit in enumerate(line):
                    if bit == '1': rect(x + col * scale, y + row * scale, scale, scale, color)
            x += (len(glyphs[c][0]) + 1) * scale
    # Original stepped construction emblem and cross-shaped FPV craft; no game artwork.
    for i in range(3):
        x, y = 128 + i * 32, 232 - i * 38
        rect(x, y, 192, 42, (64, 106, 130))
        rect(x, y, 192, 10, (99, 202, 203))
        rect(x + 192, y + 10, 16, 32, (43, 70, 89))
    for x, y in [(170,110),(318,110),(170,172),(318,172)]:
        rect(x-14,y-14,28,28,(234,200,107));rect(x-7,y-7,14,14,(19,26,38))
    rect(184,120,148,7,(234,200,107));rect(184,155,148,7,(234,200,107))
    rect(237,117,38,48,(230,236,240))
    text('MINDUSTRY', 340, 7, (230,236,240))
    text('3D FPV', 415, 6, (99,202,203))
    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data))
    rows = b''.join(b'\0' + pixels[y * size * 3:(y + 1) * size * 3] for y in range(size))
    return b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', size, size, 8, 2, 0, 0, 0)) + chunk(b'IDAT', zlib.compress(rows, 9)) + chunk(b'IEND', b'')

if __name__ == '__main__':
    from pathlib import Path
    output = Path(__file__).resolve().parents[1] / 'assets/workshop-preview.png'
    output.parent.mkdir(exist_ok=True)
    output.write_bytes(preview())
    print(output)
