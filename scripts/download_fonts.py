#!/usr/bin/env python3

#  Copyright 2026 Klyph Contributors
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

import argparse
import hashlib
import re
import urllib.parse
import urllib.request
from pathlib import Path

FONT_FACE_RE = re.compile(r"@font-face\s*{([^}]*)}", re.DOTALL | re.IGNORECASE)
PROP_RE = re.compile(r"([a-zA-Z-]+)\s*:\s*([^;]+);", re.IGNORECASE)
URL_RE = re.compile(r"url\(([^)]+)\)", re.IGNORECASE)


def fetch_text(url):
    with urllib.request.urlopen(url) as resp:
        return resp.read().decode("utf-8", errors="replace")


def fetch_bytes(url):
    with urllib.request.urlopen(url) as resp:
        return resp.read()


def clean_value(value):
    value = value.strip()
    if (value.startswith("\"") and value.endswith("\"")) or (
            value.startswith("'") and value.endswith("'")
    ):
        return value[1:-1]
    return value


def parse_font_faces(css_text):
    faces = []
    for block in FONT_FACE_RE.findall(css_text):
        props = {}
        for name, value in PROP_RE.findall(block):
            props[name.strip().lower()] = value.strip()
        faces.append(props)
    return faces


def pick_url(src_value):
    if not src_value:
        return None
    urls = URL_RE.findall(src_value)
    if not urls:
        return None
    return clean_value(urls[0])


def parse_weight(value):
    if not value:
        return "normal"
    return clean_value(value).strip().lower()


def parse_style(value):
    if not value:
        return "normal"
    return clean_value(value).strip().lower()


def parse_unicode_ranges(value):
    if not value:
        return ""
    return clean_value(value).strip()


def sha1_hex(data):
    return hashlib.sha1(data).hexdigest()


def to_valid_identifier(name):
    safe = re.sub(r"[^0-9A-Za-z_]", "_", name)
    if not safe or not (safe[0].isalpha() or safe[0] == "_"):
        safe = "_" + safe
    return safe


def to_pascal_case(name):
    parts = re.split(r"[^0-9A-Za-z]+", name)
    parts = [p for p in parts if p]
    if not parts:
        return "_"
    out = "".join(p[:1].upper() + p[1:] for p in parts)
    return to_valid_identifier(out)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("css_url", help="URL of the CSS @font-face stylesheet")
    parser.add_argument(
        "--out-dir", default="composeResources/font", help="font resources dir"
    )
    parser.add_argument(
        "--kotlin-out", default="FontDescriptors.kt", help="output Kotlin file"
    )
    parser.add_argument(
        "--res-package",
        default="your.package.generated.resources",
        help="package for generated Res class",
    )
    parser.add_argument(
        "--kotlin-package",
        default="",
        help="package for generated Kotlin file",
    )
    args = parser.parse_args()

    css_url = args.css_url
    css_text = fetch_text(css_url)
    faces = parse_font_faces(css_text)

    out_dir = Path(args.out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)

    descriptors = []
    for face in faces:
        src = face.get("src")
        font_url = pick_url(src)
        if not font_url:
            continue
        font_url = urllib.parse.urljoin(css_url, font_url)
        data = fetch_bytes(font_url)

        h = sha1_hex(data)
        ext = Path(urllib.parse.urlparse(font_url).path).suffix or ".woff2"
        resource_name = to_valid_identifier(h)
        if not resource_name.startswith("_"):
            resource_name = "_" + resource_name
        filename = resource_name + ext

        target = out_dir / filename
        if not target.exists():
            target.write_bytes(data)

        family = clean_value(face.get("font-family", "Unknown"))
        weight = parse_weight(face.get("font-weight"))
        style = parse_style(face.get("font-style"))
        unicode_ranges = parse_unicode_ranges(face.get("unicode-range"))

        descriptors.append(
            {
                "resource_name": resource_name,
                "family": family,
                "weight": weight,
                "style": style,
                "ranges": unicode_ranges,
            }
        )

    kotlin_lines = []
    if args.kotlin_package:
        kotlin_lines.append(f"package {args.kotlin_package}")
        kotlin_lines.append("")
    kotlin_lines += [
        f"import {args.res_package}.Res",
        f"import {args.res_package}.*",
        "import xyz.hyli.klyph.ResourceFontDescriptor",
        "import xyz.hyli.klyph.StaticFontDescriptorProvider",
        "",
    ]

    families = {}
    for d in descriptors:
        families.setdefault(d["family"], []).append(d)

    for family, items in families.items():
        provider_name = to_pascal_case(family)
        kotlin_lines.append(f"val {provider_name} = StaticFontDescriptorProvider(")
        for d in items:
            kotlin_lines.append(
                "    ResourceFontDescriptor("
                "resource = Res.font.{res}, "
                "fontFamily = \"{family}\", "
                "weight = \"{weight}\", "
                "style = \"{style}\", "
                "unicodeRanges = \"{ranges}\""
                "),".format(
                    res=d["resource_name"],
                    family=d["family"],
                    weight=d["weight"],
                    style=d["style"],
                    ranges=d["ranges"],
                )
            )
        kotlin_lines.append(")")
    kotlin_lines.append("")

    Path(args.kotlin_out).write_text("\n".join(kotlin_lines), encoding="utf-8")

    print(f"Downloaded {len(descriptors)} font files to {out_dir}")
    print(f"Wrote Kotlin provider to {args.kotlin_out}")


if __name__ == "__main__":
    main()
