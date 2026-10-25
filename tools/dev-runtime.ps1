# Read-only local SDK resolution for existing developer launch scripts. Never searches Steam.
$FpvDevRoot=Split-Path -Parent $PSScriptRoot
function Get-FpvDevGameDir {
    $value=$env:FPV_MINDUSTRY
    if(-not $value){$configuration=Join-Path $FpvDevRoot 'local/dev-runtime.json';if(Test-Path -LiteralPath $configuration){$value=(Get-Content -LiteralPath $configuration -Raw | ConvertFrom-Json).gameRoot}}
    if(-not $value){$value=Join-Path (Split-Path -Parent $FpvDevRoot) 'ForCodex/OriginalGameMindustry'}
    if(-not [IO.Path]::IsPathRooted($value)){$value=Join-Path $FpvDevRoot $value}
    $value=[IO.Path]::GetFullPath($value)
    if(-not(Test-Path -LiteralPath (Join-Path $value 'jre/desktop.jar'))){throw "Standalone host missing: $value. No Steam fallback is used."}
    return $value
}
function Get-FpvDevDiagnosticJar {
    $configuration=Join-Path $FpvDevRoot 'local/latest-diagnostic.json'
    $value=$null
    if(Test-Path -LiteralPath $configuration){$value=(Get-Content -LiteralPath $configuration -Raw | ConvertFrom-Json).jar}
    if(-not $value){$configuration=Join-Path $FpvDevRoot 'local/dev-runtime.json';if(Test-Path -LiteralPath $configuration){$value=(Get-Content -LiteralPath $configuration -Raw | ConvertFrom-Json).diagnosticJar}}
    if(-not $value){throw 'Build a diagnostic JAR first or configure local/dev-runtime.json diagnosticJar.'}
    if(-not [IO.Path]::IsPathRooted($value)){$value=Join-Path $FpvDevRoot $value}
    if(-not(Test-Path -LiteralPath $value -PathType Leaf)){throw "Diagnostic JAR missing: $value"}
    return [IO.Path]::GetFullPath($value)
}
