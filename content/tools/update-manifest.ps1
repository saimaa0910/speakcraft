# Recomputes app/src/main/assets/content/manifest.json (counts and SHA-256 per file; ARC-03 §6.11, V-19)
# after a content change. Run from anywhere: powershell -File content/tools/update-manifest.ps1 [-BumpVersion]
# packVersion must increase for any content change that ships (+1 with -BumpVersion).
param([switch]$BumpVersion)
$ErrorActionPreference = "Stop"
$repo = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$dir = Join-Path $repo "app\src\main\assets\content"
$manifestPath = Join-Path $dir "manifest.json"
$old = Get-Content $manifestPath -Raw | ConvertFrom-Json

function Read-Items($path) {
  # Windows PowerShell 5.1 returns a JSON array as one object: enumerate it so each record is counted.
  $parsed = Get-Content $path -Raw -Encoding UTF8 | ConvertFrom-Json
  foreach ($item in $parsed) { $item }
}
function Measure-Items($items) {
  [ordered]@{
    total          = $items.Count
    ACTIVE         = @($items | Where-Object { $_.status -eq "ACTIVE" }).Count
    PENDING_REVIEW = @($items | Where-Object { $_.licenceStatus -eq "PENDING_REVIEW" }).Count
  }
}

$techniques = @(Read-Items (Join-Path $dir "techniques.json"))
$tips = @(Read-Items (Join-Path $dir "tips.json"))
$activities = @(Get-ChildItem (Join-Path $dir "activities") -Filter *.json | Sort-Object Name | ForEach-Object { Read-Items $_.FullName })

$files = Get-ChildItem $dir -Recurse -File | Where-Object { $_.Name -ne "manifest.json" } | ForEach-Object {
  [ordered]@{
    path   = $_.FullName.Substring($dir.Length + 1).Replace("\", "/")
    sha256 = (Get-FileHash $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
  }
} | Sort-Object { $_.path }

$manifest = [ordered]@{
  packId            = $old.packId
  packVersion       = if ($BumpVersion) { [int]$old.packVersion + 1 } else { [int]$old.packVersion }
  schemaVersion     = [int]$old.schemaVersion
  minAppVersionCode = [int]$old.minAppVersionCode
  createdAt         = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
  counts            = [ordered]@{ techniques = Measure-Items $techniques; activities = Measure-Items $activities; tips = Measure-Items $tips }
  files             = @($files)
}
$json = ($manifest | ConvertTo-Json -Depth 6) -replace "`r`n", "`n"
[IO.File]::WriteAllText($manifestPath, $json + "`n", (New-Object System.Text.UTF8Encoding $false))
"manifest.json updated: packVersion $($manifest.packVersion), $($files.Count) files"
