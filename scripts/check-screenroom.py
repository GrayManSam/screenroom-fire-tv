"""Check Screenroom source files without building an APK or using a device."""

from collections import Counter
from html.parser import HTMLParser
from pathlib import Path
import re
import subprocess
import sys
from urllib.parse import unquote, urlsplit
from xml.etree import ElementTree


HUB = Path(__file__).resolve().parents[1] / "hub"
CSS_URL = re.compile(r"""url\(\s*['"]?([^'")]+)""")
JS_ID = re.compile(r"""getElementById\(\s*['"]([^'"]+)['"]\s*\)""")


class ScreenroomPage(HTMLParser):
    def __init__(self) -> None:
        super().__init__()
        self.assets: list[str] = []
        self.ids: Counter[str] = Counter()
        self.label_ids: set[str] = set()

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        attributes = dict(attrs)
        if element_id := attributes.get("id"):
            self.ids[element_id] += 1
        for name in ("aria-labelledby", "aria-describedby"):
            self.label_ids.update((attributes.get(name) or "").split())
        asset_attribute = {"link": "href", "script": "src", "img": "src"}.get(tag)
        if asset_attribute and (asset := attributes.get(asset_attribute)):
            self.assets.append(asset)
        self.assets.extend(CSS_URL.findall(attributes.get("style") or ""))


def check_assets(asset_dir: Path) -> list[str]:
    page = ScreenroomPage()
    page.feed((asset_dir / "index.html").read_text(encoding="utf-8"))
    page.close()
    css = (asset_dir / "screenroom.css").read_text(encoding="utf-8")
    javascript = (asset_dir / "screenroom.js").read_text(encoding="utf-8")
    problems = []
    for reference in sorted(set(page.assets + CSS_URL.findall(css))):
        url = urlsplit(reference.strip())
        if not url.path and not (url.scheme or url.netloc):
            continue
        target = (asset_dir / unquote(url.path)).resolve()
        if url.scheme or url.netloc or not target.is_relative_to(asset_dir):
            problems.append(f"Asset must be bundled under assets/: {reference}")
        elif not target.is_file():
            problems.append(f"Missing bundled asset: {reference}")

    for name, count in sorted(page.ids.items()):
        if count > 1:
            problems.append(f"Duplicate HTML id: {name}")
    for name in sorted((page.label_ids | set(JS_ID.findall(javascript))) - page.ids.keys()):
        problems.append(f"Referenced HTML id is missing: {name}")
    return problems


def main() -> int:
    try:
        problems = check_assets(HUB / "assets")
        for filename in ("AndroidManifest.xml", "res/drawable/banner.xml"):
            ElementTree.parse(HUB / filename)
        subprocess.run(["sh", "-n", HUB / "build.sh"], check=True)
        subprocess.run(["node", "--check", HUB / "assets/screenroom.js"], check=True)
    except (OSError, ValueError, ElementTree.ParseError, subprocess.CalledProcessError) as error:
        problems = [f"Could not complete Screenroom checks: {error}"]

    if problems:
        print("\n".join(problems), file=sys.stderr)
        return 1
    print("Screenroom assets, HTML references, Android XML, and script syntax passed.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
