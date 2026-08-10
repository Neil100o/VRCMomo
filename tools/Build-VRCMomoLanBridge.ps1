param(
    [switch]$Clean
)

$ErrorActionPreference = "Stop"
$toolsDirectory = $PSScriptRoot
$repositoryRoot = Split-Path -Parent $toolsDirectory
$buildDirectory = Join-Path $toolsDirectory "build"
$distDirectory = Join-Path $toolsDirectory "dist"
$outputPath = Join-Path $repositoryRoot "downloads\VRCMomo-LAN-Bridge.exe"
$scriptPath = Join-Path $toolsDirectory "vrcmomo_lan_bridge.py"
$exporterPath = Join-Path $toolsDirectory "export_vrcx_activity.py"

if ($Clean) {
    Remove-Item -Recurse -Force $buildDirectory, $distDirectory -ErrorAction SilentlyContinue
}

python -m PyInstaller --noconfirm --clean --onefile --console `
    --name "VRCMomo-LAN-Bridge" `
    --add-data "$exporterPath;." `
    --distpath $distDirectory `
    --workpath $buildDirectory `
    --specpath $buildDirectory `
    $scriptPath

Copy-Item (Join-Path $distDirectory "VRCMomo-LAN-Bridge.exe") $outputPath -Force
Write-Host "Created $outputPath"
