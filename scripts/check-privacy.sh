#!/bin/sh
# POSIX port of scripts/check-privacy.ps1, used when PowerShell is unavailable.
# Must stay equivalent to the .ps1: forbidden paths + secret-like content in staged blobs.
set -eu

BAD_NAMES="local.properties keystore.properties"
BAD_EXT=".jks .keystore .p12 .pfx"
BIN_EXT=".png .jpg .jpeg .gif .webp .ico .apk .jar .class .bin .jks .keystore"
PAT="(github_pat_[A-Za-z0-9_]{20,}|ghp_[A-Za-z0-9_]{20,}|AKIA[0-9A-Z]{16}|AIza[0-9A-Za-z_-]{20,}|xox[baprs]-[A-Za-z0-9-]{20,}|BEGIN (RSA|OPENSSH|EC|DSA) PRIVATE KEY|(password|passwd|secret|api[_-]?key|access[_-]?token|private[_-]?key)[[:space:]]*[:=][[:space:]]*[\"'][^\"']{8,}[\"'])"

violations=0
git diff --cached --name-only --diff-filter=ACMRT | while IFS= read -r f; do
  [ -n "$f" ] || continue
  base=$(printf '%s\n' "$f" | sed 's#.*/##')
  ext=$(printf '%s\n' "$base" | sed 's#.*\.#.#' | tr 'A-Z' 'a-z')
  case " $BAD_NAMES " in *" $base "*) echo "forbidden file: $f"; exit 2;; esac
  case " $BAD_EXT " in *" $ext "*) echo "forbidden file: $f"; exit 2;; esac
  case " $BIN_EXT " in *" $ext "*) continue;; esac
  if git show --no-ext-diff --textconv ":$f" 2>/dev/null | grep -Ei -q "$PAT"; then
    echo "secret-like content: $f"; exit 2
  fi
done
rc=$?
if [ $rc -ne 0 ]; then
  echo "privacy check FAILED (exit $rc)" >&2
  exit 1
fi
echo "privacy check passed ($(git diff --cached --name-only --diff-filter=ACMRT | grep -c . || true) file(s) scanned)"
