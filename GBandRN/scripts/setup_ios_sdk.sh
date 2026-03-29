#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────
# setup_ios_sdk.sh
# Downloads the VeepooBleSDK.framework from HBandSDK GitHub
# and places it in ios/Frameworks/ so the Podfile can pick it up.
#
# Run once before `pod install`:
#   chmod +x scripts/setup_ios_sdk.sh
#   ./scripts/setup_ios_sdk.sh
# ─────────────────────────────────────────────────────────────
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
FRAMEWORKS_DIR="$ROOT_DIR/ios/Frameworks"
FRAMEWORK_PATH="$FRAMEWORKS_DIR/VeepooBleSDK.framework"

if [ -d "$FRAMEWORK_PATH" ]; then
  echo "✓ VeepooBleSDK.framework already exists — skipping download."
  exit 0
fi

echo "→ Downloading iOS_Ble_SDK from GitHub..."
TMP_DIR=$(mktemp -d)
git clone --depth=1 https://github.com/HBandSDK/iOS_Ble_SDK.git "$TMP_DIR/iOS_Ble_SDK" 2>&1

echo "→ Looking for VeepooBleSDK.framework..."
# Find the latest framework version
FRAMEWORK=$(find "$TMP_DIR/iOS_Ble_SDK" -name "VeepooBleSDK.framework" -type d | sort | tail -1)

if [ -z "$FRAMEWORK" ]; then
  echo "✗ Could not find VeepooBleSDK.framework in the repo."
  echo "  Please manually download it from: https://github.com/HBandSDK/iOS_Ble_SDK"
  echo "  and place the VeepooBleSDK.framework folder in: ios/Frameworks/"
  rm -rf "$TMP_DIR"
  exit 1
fi

echo "→ Copying framework to ios/Frameworks/..."
cp -R "$FRAMEWORK" "$FRAMEWORKS_DIR/"
rm -rf "$TMP_DIR"

echo "✓ Done! VeepooBleSDK.framework is ready."
echo ""
echo "Next steps:"
echo "  cd ios && pod install"
