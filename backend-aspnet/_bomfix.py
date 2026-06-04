import os

# BOM을 추가할 텍스트 소스 확장자(.NET/웹 소스 + 문서/설정)
TEXT_EXT = {
    '.cs', '.aspx', '.ascx', '.asax', '.cshtml', '.config',
    '.csproj', '.html', '.htm', '.md', '.json', '.css', '.js', '.txt'
}
TEXT_NAMES = {'.gitignore'}

SKIP_DIRS = {'bin', 'obj', '.vs', 'packages'}
SKIP_FILES = {'_bomfix.py', '_bomcheck.py'}

BOM = b'\xef\xbb\xbf'
fixed = []

for root, dirs, files in os.walk('.'):
    dirs[:] = [d for d in dirs if d not in SKIP_DIRS]
    for fn in files:
        if fn in SKIP_FILES:
            continue
        ext = os.path.splitext(fn)[1].lower()
        if ext not in TEXT_EXT and fn not in TEXT_NAMES:
            continue
        p = os.path.join(root, fn)
        with open(p, 'rb') as f:
            data = f.read()
        if data[:3] == BOM:
            continue                 # 이미 BOM
        if not any(b > 0x7F for b in data):
            continue                 # ASCII만 → 불필요
        with open(p, 'wb') as f:
            f.write(BOM + data)
        fixed.append(p.replace(os.sep, '/'))

print("=== BOM 추가 완료 파일 ===")
for p in sorted(fixed):
    print("  " + p)
print("  total = %d" % len(fixed))
