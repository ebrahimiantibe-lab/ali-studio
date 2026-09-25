#!/usr/bin/env bash
set -euo pipefail
ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
cd "$ROOT"

fail=0

# No dangerous Android permissions are needed by this offline editor.
for perm in \
  android.permission.INTERNET \
  android.permission.ACCESS_NETWORK_STATE \
  android.permission.CAMERA \
  android.permission.RECORD_AUDIO \
  android.permission.READ_SMS \
  android.permission.SEND_SMS \
  android.permission.READ_CONTACTS \
  android.permission.WRITE_CONTACTS \
  android.permission.ACCESS_FINE_LOCATION \
  android.permission.ACCESS_COARSE_LOCATION \
  android.permission.REQUEST_INSTALL_PACKAGES \
  android.permission.SYSTEM_ALERT_WINDOW \
  android.permission.QUERY_ALL_PACKAGES \
  android.permission.MANAGE_EXTERNAL_STORAGE; do
  if grep -Rqs -- "$perm" app/src/main; then
    echo "SECURITY FAIL: unexpected permission: $perm" >&2
    fail=1
  fi
done

# Block common dynamic-code / shell / network loading primitives in app source.
if grep -RInE --include='*.java' --include='*.kt' \
  'Runtime\.getRuntime\(\)\.exec|ProcessBuilder|DexClassLoader|PathClassLoader|System\.loadLibrary|System\.load\(|WebView|HttpURLConnection|URLConnection|java\.net\.Socket|DatagramSocket|Class\.forName' app/src/main; then
  echo "SECURITY FAIL: forbidden runtime/network primitive found" >&2
  fail=1
fi

# The only external URL intentionally present is the official Eitaa channel.
urls="$(grep -RhoE 'https?://[^"'"'"'[:space:]]+' app/src/main/java app/src/main/res 2>/dev/null || true)"
while IFS= read -r url; do
  [ -z "$url" ] && continue
  case "$url" in
    https://eitaa.com/ali12536|https://eitaa.com/ali12536/|http://schemas.android.com/*) ;;
    *) echo "SECURITY FAIL: unexpected URL: $url" >&2; fail=1;;
  esac
done <<< "$urls"

# No packaged executable/native artifacts in source tree.
if find app/src -type f \( -name '*.apk' -o -name '*.dex' -o -name '*.jar' -o -name '*.aar' -o -name '*.so' -o -name '*.exe' -o -name '*.dll' \) -print -quit | grep -q .; then
  echo "SECURITY FAIL: executable artifact found under app/src" >&2
  fail=1
fi

if [ "$fail" -ne 0 ]; then
  exit 1
fi

echo "SECURITY OK: offline editor checks passed."
