import os

rows = []
for root, dirs, files in os.walk('.'):
    dirs[:] = [d for d in dirs if d not in ('bin', 'obj', '.vs')]
    for fn in files:
        if fn == '_bomcheck.py':
            continue
        p = os.path.join(root, fn)
        try:
            with open(p, 'rb') as f:
                data = f.read()
        except Exception:
            continue
        bom = data[:3] == b'\xef\xbb\xbf'
        nonascii = any(b > 0x7F for b in data)
        rows.append((bom, nonascii, p.replace(os.sep, '/')))

risk = sorted([r for r in rows if r[1] and not r[0]])
have = sorted([r for r in rows if r[0]])
asciionly = [r for r in rows if not r[1]]

print("=== [위험] 한글 포함 & BOM 없음 (VS/csc 깨짐) ===")
for bom, na, p in risk:
    print("  " + p)
print("  total = %d" % len(risk))
print()
print("=== [정상] BOM 있음 ===")
for bom, na, p in have:
    print("  " + p)
print("  total = %d" % len(have))
print()
print("=== [무관] ASCII only ===")
print("  total = %d" % len(asciionly))
