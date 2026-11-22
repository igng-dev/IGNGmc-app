#!/bin/sh
# POSIX privacy & credential checker (equivalent to scripts/check-privacy.ps1).
# Usage:
#   scripts/check-privacy.sh         Scan staged files (default, for pre-commit)
#   scripts/check-privacy.sh --all   Scan all git-tracked files (for CI / full audit)
set -eu

SCAN_ALL=0
if [ "${1:-}" = "--all" ]; then
  SCAN_ALL=1
elif [ -n "${1:-}" ]; then
  echo "Usage: $0 [--all]" >&2
  exit 2
fi

BAD_NAMES="local.properties keystore.properties release-signing.properties"
BAD_EXT=".jks .keystore .p12 .pfx .pem .key .der .apk .aab"
BIN_EXT=".png .jpg .jpeg .gif .webp .ico .apk .aab .jar .class .bin .jks .keystore .p12 .pfx .der"

# High-confidence credential token prefixes / private key headers (checked on all text files, incl. examples)
TOKEN_PAT='(github_pat_[A-Za-z0-9_]{20,}|ghp_[A-Za-z0-9_]{20,}|AKIA[0-9A-Z]{16}|AIza[0-9A-Za-z_-]{20,}|xox[baprs]-[A-Za-z0-9-]{20,}|BEGIN (RSA|OPENSSH|EC|DSA) PRIVATE KEY)'
# Hardcoded secret assignments (skipped on *.example / *.example.* template files)
ASSIGN_PAT='(^|[^^])(password|passwd|secret|api[_-]?key|access[_-]?token|private[_-]?key)[[:space:]]*[:=][[:space:]]*("[^")][^"]{7,}"|'\''[^'\'')][^'\'']{7,}'\'')'

list_files() {
  if [ "$SCAN_ALL" = "1" ]; then
    git ls-files
  else
    git diff --cached --name-only --diff-filter=ACMRT
  fi
}

read_blob() {
  if [ "$SCAN_ALL" = "1" ]; then
    [ -f "$1" ] && cat "$1"
  else
    git show --no-ext-diff --textconv ":$1" 2>/dev/null
  fi
}

check_all() {
  count=0
  failed=0
  while IFS= read -r f; do
    [ -n "$f" ] || continue
    count=$((count + 1))
    base=$(printf '%s\n' "$f" | sed 's#.*/##')
    ext=""
    case "$base" in
      *.*) ext=$(printf '%s\n' "$base" | sed 's#.*\.#.#' | tr 'A-Z' 'a-z') ;;
    esac

    case " $BAD_NAMES " in
      *" $base "*) echo "forbidden file: $f" >&2; failed=1; continue ;;
    esac
    case "$base" in
      .env|.env.*)
        if [ "$base" != ".env.example" ]; then
          echo "forbidden env file: $f" >&2; failed=1; continue
        fi
        ;;
    esac
    case "$f" in
      android/keystore/*|*/keystore/*)
        echo "forbidden keystore path: $f" >&2; failed=1; continue
        ;;
    esac
    if [ -n "$ext" ]; then
      case " $BAD_EXT " in
        *" $ext "*) echo "forbidden extension: $f" >&2; failed=1; continue ;;
      esac
      case " $BIN_EXT " in
        *" $ext "*) continue ;;
      esac
    fi

    is_example=0
    case "$base" in
      *.example|*.example.*) is_example=1 ;;
    esac

    if read_blob "$f" | grep -E -q "$TOKEN_PAT"; then
      echo "secret token or private key header: $f" >&2
      failed=1
      continue
    fi
    if [ "$is_example" = "0" ] && read_blob "$f" | grep -Ei -q "$ASSIGN_PAT"; then
      echo "secret-like assignment: $f" >&2
      failed=1
    fi
  done

  if [ "$failed" != "0" ]; then
    echo "privacy check FAILED" >&2
    return 1
  fi
  scope="staged"
  [ "$SCAN_ALL" = "1" ] && scope="tracked"
  echo "privacy check passed ($count $scope file(s) scanned)"
}

list_files | check_all
