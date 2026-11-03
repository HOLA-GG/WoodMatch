"""Check local Markdown links/images in the canonical wiki without network access."""
from pathlib import Path
import re
import sys
root = Path(__file__).resolve().parents[1] / 'docs/wiki'
errors = []
count = 0
for page in root.glob('*.md'):
    text = page.read_text()
    targets = re.findall(r'\]\(([^)]+)\)', text) + re.findall(r'<img[^>]+src="([^"]+)"', text)
    for target in targets:
        if '://' in target or target.startswith('#'):
            continue
        count += 1
        if not (page.parent / target.split('#')[0]).exists():
            errors.append(f'{page.name}: {target}')
print(f'{count} local links/images checked; {len(errors)} missing')
for error in errors:
    print(error)
sys.exit(bool(errors))
