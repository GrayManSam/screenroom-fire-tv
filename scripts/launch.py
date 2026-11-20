"""Open a known app on an authorized Fire TV device."""

import argparse
import re
import subprocess

from inventory import adb_shell


APP_PACKAGES = {
    "jellyfin": "org.jellyfin.androidtv",
    "moviebox": "com.movieboxpro.androidtv",
    "projectivy": "com.spocky.projengmenu",
    "screenroom": "com.geekedsilicon.tvhub",
    "youtube": "com.amazon.firetv.youtube",
}
ACTIVITY_NAME = re.compile(r"[A-Za-z0-9_.$]+")


def resolve_launcher(serial: str, package: str) -> str:
    for category in ("LEANBACK_LAUNCHER", "LAUNCHER"):
        response = adb_shell(
            serial,
            "cmd", "package", "resolve-activity", "--brief",
            "-a", "android.intent.action.MAIN",
            "-c", f"android.intent.category.{category}",
            "-p", package,
        )
        for line in reversed(response.splitlines()):
            owner, separator, activity = line.partition("/")
            if owner == package and separator and ACTIVITY_NAME.fullmatch(activity):
                return line
    raise ValueError(f"No enabled launcher activity for {package}")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("serial", help="ADB device serial or LAN address with port")
    parser.add_argument("app", choices=APP_PACKAGES)
    parser.add_argument(
        "--resolve-only", action="store_true", help="Print the activity without opening it"
    )
    args = parser.parse_args()
    if not args.serial.strip():
        parser.error("an explicit ADB serial is required")

    try:
        activity = resolve_launcher(args.serial, APP_PACKAGES[args.app])
        if args.resolve_only:
            print(activity)
            return
        # ADB shell joins arguments for the device shell. Android class names
        # may contain a dollar sign, so quote the validated component.
        response = adb_shell(args.serial, "am", "start", "-W", "-n", f"'{activity}'")
        if "Status: ok" not in response.splitlines():
            raise ValueError(response or f"Fire TV did not confirm launch of {activity}")
        print(response)
    except subprocess.CalledProcessError as error:
        parser.exit(1, f"ADB failed: {error.stderr.strip() or error.stdout.strip() or error}\n")
    except (OSError, subprocess.TimeoutExpired, ValueError) as error:
        parser.exit(1, f"{error}\n")


if __name__ == "__main__":
    main()
