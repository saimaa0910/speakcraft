# Builds the SpeakCraft PDF books from the HTML sources in this folder.
# Requirements: Google Chrome or Microsoft Edge (headless printing). No other tools.
# Usage (PowerShell):  & "f:\speakcraft\docs\books\src\build_books.ps1"

$ErrorActionPreference = "Stop"
$src = Split-Path -Parent $MyInvocation.MyCommand.Path
$out = Split-Path -Parent $src

$browser = @(
  "C:\Program Files\Google\Chrome\Application\chrome.exe",
  "C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe",
  "C:\Program Files\Microsoft\Edge\Application\msedge.exe"
) | Where-Object { Test-Path $_ } | Select-Object -First 1
if (-not $browser) { throw "Chrome or Edge not found." }

# 1. Assemble Book 2: insert the Part III-IV fragment at its marker.
$b2 = Join-Path $src "book2_system_design.html"
$frag = Join-Path $src "book2_part3_4_fragment.html"
$b2full = Join-Path $src "book2_full.html"
$main = [System.IO.File]::ReadAllText($b2, [System.Text.Encoding]::UTF8)
$part = [System.IO.File]::ReadAllText($frag, [System.Text.Encoding]::UTF8)
$part = $part -replace "<!-- APPEND-HERE -->", ""
if ($main -notmatch "<!-- PART-III-IV-HERE -->") { throw "Marker PART-III-IV-HERE missing in book2_system_design.html" }
$main = $main.Replace("<!-- PART-III-IV-HERE -->", $part).Replace("<!-- APPEND-HERE -->", "")
[System.IO.File]::WriteAllText($b2full, $main, (New-Object System.Text.UTF8Encoding($false)))

# 2. Render.
$jobs = @(
  @{ In = (Join-Path $src "book1_product_planning.html"); Out = (Join-Path $out "SpeakCraft_Book1_Product_Planning.pdf") },
  @{ In = $b2full;                                        Out = (Join-Path $out "SpeakCraft_Book2_System_Design_and_Delivery.pdf") }
)
foreach ($j in $jobs) {
  if (Test-Path $j.Out) { Remove-Item $j.Out -Force }
  $uri = ([System.Uri]$j.In).AbsoluteUri
  $p = Start-Process -FilePath $browser -ArgumentList @("--headless=new", "--disable-gpu", "--no-pdf-header-footer", "--print-to-pdf=`"$($j.Out)`"", $uri) -PassThru -WindowStyle Hidden
  $p.WaitForExit(180000) | Out-Null
  $deadline = (Get-Date).AddSeconds(60)
  while (-not (Test-Path $j.Out) -and (Get-Date) -lt $deadline) { Start-Sleep -Milliseconds 500 }
  if (-not (Test-Path $j.Out)) { throw "PDF not produced: $($j.Out)" }
  $s = [System.Text.Encoding]::ASCII.GetString([System.IO.File]::ReadAllBytes($j.Out))
  $pages = ([regex]::Matches($s, "/Type\s*/Page[^s]")).Count
  "{0}  {1:N0} KB  {2} pages" -f (Split-Path -Leaf $j.Out), ((Get-Item $j.Out).Length / 1KB), $pages
}
