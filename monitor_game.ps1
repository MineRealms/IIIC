# Monitor Minecraft game log for mod loading and errors
$logPath = "G:\MinecraftGames\CTNH-BaopuEdition\.minecraft\versions\GregTech Odyssey\logs\latest.log"

Write-Host "=== Monitoring Minecraft Log ===" -ForegroundColor Cyan
Write-Host "Log file: $logPath" -ForegroundColor Gray
Write-Host ""

$lastSize = 0
$startTime = Get-Date

while ($true) {
    if (Test-Path $logPath) {
        $currentSize = (Get-Item $logPath).Length

        if ($currentSize -gt $lastSize) {
            $content = Get-Content $logPath -Tail 100

            # Check for mod loading
            $modLoading = $content | Select-String -Pattern "integratedindustrialcraft|Integrated Industrial" -CaseSensitive:$false
            if ($modLoading) {
                Write-Host "[MOD] " -ForegroundColor Green -NoNewline
                $modLoading | ForEach-Object { Write-Host $_.Line }
            }

            # Check for XaerosWorldMap integration
            $xaeromap = $content | Select-String -Pattern "xaeromap|XaerosWorldMap|pollution.*overlay" -CaseSensitive:$false
            if ($xaeromap) {
                Write-Host "[XAERO] " -ForegroundColor Cyan -NoNewline
                $xaeromap | ForEach-Object { Write-Host $_.Line }
            }

            # Check for mixin application
            $mixin = $content | Select-String -Pattern "GuiMapMixin|GuiMapAccessor|PollutionOverlay" -CaseSensitive:$false
            if ($mixin) {
                Write-Host "[MIXIN] " -ForegroundColor Yellow -NoNewline
                $mixin | ForEach-Object { Write-Host $_.Line }
            }

            # Check for errors
            $errors = $content | Select-String -Pattern "ERROR|FATAL|crash|exception" -CaseSensitive:$false
            if ($errors) {
                Write-Host "[ERROR] " -ForegroundColor Red -NoNewline
                $errors | ForEach-Object { Write-Host $_.Line -ForegroundColor Red }
            }

            # Check for warnings
            $warnings = $content | Select-String -Pattern "WARN.*integratedindustrialcraft|WARN.*xaeromap" -CaseSensitive:$false
            if ($warnings) {
                Write-Host "[WARN] " -ForegroundColor Yellow -NoNewline
                $warnings | ForEach-Object { Write-Host $_.Line -ForegroundColor Yellow }
            }

            $lastSize = $currentSize
        }
    }

    # Check if game is still running
    $elapsed = (Get-Date) - $startTime
    if ($elapsed.TotalMinutes -gt 10) {
        Write-Host "`n=== Monitoring timeout (10 minutes) ===" -ForegroundColor Yellow
        break
    }

    Start-Sleep -Milliseconds 500
}

Write-Host "`n=== Monitoring stopped ===" -ForegroundColor Cyan
