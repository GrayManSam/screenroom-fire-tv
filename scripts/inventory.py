"""Collect a read-only inventory of an authorized Fire TV device."""

import argparse
import json
from pathlib import Path
import subprocess


def adb_shell(serial: str, *args: str) -> str:
    result = subprocess.run(
        ["adb", "-s", serial, "shell", *args],
        check=True,
        capture_output=True,
        text=True,
        timeout=20,
    )
    return result.stdout.strip()


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("serial", help="ADB device serial or LAN address with port")
    parser.add_argument("--output", type=Path, help="Path for a new JSON snapshot")
    args = parser.parse_args()

    try:
        inventory = {
            "serial": args.serial,
            "model": adb_shell(args.serial, "getprop", "ro.product.model"),
            "firmware": adb_shell(args.serial, "getprop", "ro.build.display.id"),
            "home_activity": adb_shell(
                args.serial, "cmd", "package", "resolve-activity", "--brief",
                "-a", "android.intent.action.MAIN",
                "-c", "android.intent.category.HOME",
            ).splitlines(),
            "enabled_packages": sorted(
                adb_shell(args.serial, "pm", "list", "packages", "-e").splitlines()
            ),
            "disabled_packages": sorted(
                adb_shell(args.serial, "pm", "list", "packages", "-d").splitlines()
            ),
        }
        snapshot = json.dumps(inventory, indent=2)
        if args.output is None:
            print(snapshot)
        else:
            args.output.parent.mkdir(parents=True, exist_ok=True)
            with args.output.open("x", encoding="utf-8") as output:
                output.write(snapshot + "\n")
    except subprocess.CalledProcessError as error:
        parser.exit(1, f"ADB failed: {error.stderr.strip() or error}\n")
    except (OSError, subprocess.TimeoutExpired) as error:
        parser.exit(1, f"{error}\n")


if __name__ == "__main__":
    main()
