#!/bin/sh
set -eu

: "${ANDROID_JAR:?Set ANDROID_JAR to the API 30 android.jar}"
: "${ANDROID_BUILD_TOOLS:?Set ANDROID_BUILD_TOOLS to the build-tools directory}"
: "${HUB_KEYSTORE:?Set HUB_KEYSTORE to the local signing keystore}"
project=$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd)
work=$(mktemp -d)
trap 'rm -r -- "$work"' EXIT
tools=$ANDROID_BUILD_TOOLS
mkdir -p "$work/classes" "$work/dex" "$project/../artifacts"
"$tools/aapt2" compile --dir "$project/res" -o "$work/resources.zip"
"$tools/aapt2" link -I "$ANDROID_JAR" --manifest "$project/AndroidManifest.xml" -A "$project/assets" \
    -o "$work/unsigned.apk" "$work/resources.zip"
source_file="$project/src/com/geekedsilicon/tvhub/HubActivity.java"
if [ -n "${ECJ_JAR:-}" ]; then
    java -jar "$ECJ_JAR" -8 -proc:none -classpath "$ANDROID_JAR" \
        -d "$work/classes" "$source_file"
else
    javac --release 8 -classpath "$ANDROID_JAR" -d "$work/classes" "$source_file"
fi
"$tools/d8" --min-api 26 --lib "$ANDROID_JAR" --output "$work/dex" \
    "$work/classes/com/geekedsilicon/tvhub/"*.class
(cd "$work/dex" && zip -q "$work/unsigned.apk" classes.dex)
"$tools/zipalign" -f 4 "$work/unsigned.apk" "$work/aligned.apk"
output="$project/../artifacts/screenroom.apk"
"$tools/apksigner" sign --ks "$HUB_KEYSTORE" --ks-key-alias screenroom \
    --out "$output" "$work/aligned.apk"
"$tools/apksigner" verify --verbose "$output"
