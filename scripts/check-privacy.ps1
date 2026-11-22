param(
    [switch]$All
)

$ErrorActionPreference = "Stop"

$root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$tokenRegex = '(github_pat_[A-Za-z0-9_]{20,}|ghp_[A-Za-z0-9_]{20,}|AKIA[0-9A-Z]{16}|AIza[0-9A-Za-z_-]{20,}|xox[baprs]-[A-Za-z0-9-]{20,}|BEGIN (RSA|OPENSSH|EC|DSA) PRIVATE KEY)'
$assignRegex = '(?i)(^|[^^])(password|passwd|secret|api[_-]?key|access[_-]?token|private[_-]?key)\s*[:=]\s*("[^")][^"]{7,}"|''[^'')][^'']{7,}'')'
$badNames = @("local.properties", "keystore.properties", "release-signing.properties")
$badExtensions = @(".jks", ".keystore", ".p12", ".pfx", ".pem", ".key", ".der", ".apk", ".aab")
$binaryExtensions = @('.png', '.jpg', '.jpeg', '.gif', '.webp', '.ico', '.apk', '.aab', '.jar', '.class', '.bin', '.jks', '.keystore', '.p12', '.pfx', '.der')

if ($All) {
    $scanIndex = $false
    $relativeFiles = @(git -C $root ls-files)
} else {
    $stagedFiles = @(git -C $root diff --cached --name-only --diff-filter=ACMRT)
    $scanIndex = $stagedFiles.Count -gt 0
    $relativeFiles = if ($scanIndex) { $stagedFiles } else { @(git -C $root ls-files -co --exclude-standard) }
}

$violations = @()
foreach ($relativePath in $relativeFiles) {
    if ([string]::IsNullOrWhiteSpace($relativePath)) { continue }
    $normalizedPath = $relativePath -replace '\\', '/'
    $file = Get-Item -LiteralPath (Join-Path $root $relativePath) -Force -ErrorAction SilentlyContinue
    $fileName = Split-Path -Leaf $relativePath
    $extension = [IO.Path]::GetExtension($relativePath).ToLowerInvariant()

    if ($badNames -contains $fileName -or $badExtensions -contains $extension) {
        $violations += "forbidden file: $relativePath"
        continue
    }
    if (($fileName -eq ".env" -or $fileName.StartsWith(".env.")) -and $fileName -ne ".env.example") {
        $violations += "forbidden env file: $relativePath"
        continue
    }
    if ($normalizedPath -match '(^|/)keystore/') {
        $violations += "forbidden keystore path: $relativePath"
        continue
    }
    if ($extension -notin $binaryExtensions) {
        if ($scanIndex) {
            $content = git -C $root show --no-ext-diff --textconv ":$relativePath" 2>$null | Out-String
        } elseif ($file -and $file.Length -lt 5MB) {
            $content = Get-Content -LiteralPath $file.FullName -Raw -ErrorAction SilentlyContinue
        } else {
            $content = ''
        }
        if ($content -cmatch $tokenRegex) {
            $violations += "secret token or private key header: $relativePath"
            continue
        }
        $isExample = ($fileName -like "*.example") -or ($fileName -like "*.example.*")
        if (-not $isExample -and $content -match $assignRegex) {
            $violations += "secret-like assignment: $relativePath"
        }
    }
}

if ($violations.Count -gt 0) {
    $violations | ForEach-Object { Write-Error $_ }
    exit 1
}

$scope = if ($All) { 'tracked files' } elseif ($scanIndex) { 'staged files' } else { 'working tree files' }
Write-Host "Privacy check passed: $($relativeFiles.Count) $scope checked."
