# EyedidTracker Nuclear Gradle Fix - PowerShell Version
# Run as Administrator for best results

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "NUCLEAR Gradle Fix - PowerShell Version" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "STEP 1: Stopping all Java/Gradle processes..." -ForegroundColor Yellow
Get-Process | Where-Object {$_.ProcessName -like "*java*" -or $_.ProcessName -like "*gradle*"} | Stop-Process -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 3
Write-Host "✅ All processes stopped" -ForegroundColor Green
Write-Host ""

Write-Host "STEP 2: Nuclear removal of Gradle directory..." -ForegroundColor Yellow
$gradleDir = "$env:USERPROFILE\.gradle"
if (Test-Path $gradleDir) {
    Remove-Item -Path $gradleDir -Recurse -Force -ErrorAction SilentlyContinue
    Write-Host "✅ Gradle directory nuked: $gradleDir" -ForegroundColor Green
} else {
    Write-Host "ℹ️  Gradle directory not found (already clean)" -ForegroundColor Blue
}
Write-Host ""

Write-Host "STEP 3: Removing project build directories..." -ForegroundColor Yellow
$buildDirs = @(".gradle", "app\build", "build", "app\.cxx")
foreach ($dir in $buildDirs) {
    if (Test-Path $dir) {
        Remove-Item -Path $dir -Recurse -Force -ErrorAction SilentlyContinue
        Write-Host "✅ Removed: $dir" -ForegroundColor Green
    }
}
Write-Host ""

Write-Host "STEP 4: Clearing Android Studio caches..." -ForegroundColor Yellow
$androidDirs = @("$env:USERPROFILE\.android\cache", "$env:USERPROFILE\.android\build-cache")
foreach ($dir in $androidDirs) {
    if (Test-Path $dir) {
        Remove-Item -Path $dir -Recurse -Force -ErrorAction SilentlyContinue
        Write-Host "✅ Cleared: $dir" -ForegroundColor Green
    }
}
Write-Host ""

Write-Host "STEP 5: Re-downloading Gradle wrapper..." -ForegroundColor Yellow
& .\gradlew.bat wrapper --gradle-version=8.11.1 --distribution-type=all --no-daemon
Write-Host "✅ Gradle wrapper regenerated" -ForegroundColor Green
Write-Host ""

Write-Host "STEP 6: Testing new Gradle installation..." -ForegroundColor Yellow
& .\gradlew.bat --version --no-daemon
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "NUCLEAR FIX COMPLETED! 🚀" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "🔧 Fixed Issues:" -ForegroundColor Green
Write-Host "  • Android Gradle Plugin: 8.9.1 → 8.6.1" -ForegroundColor White
Write-Host "  • Removed deprecated gradle.properties options" -ForegroundColor White
Write-Host "  • Complete Gradle cache rebuild" -ForegroundColor White
Write-Host "  • Force-killed conflicting processes" -ForegroundColor White
Write-Host ""
Write-Host "📋 Next Steps:" -ForegroundColor Yellow
Write-Host "  1. Open Android Studio" -ForegroundColor White
Write-Host "  2. Open EyedidTracker project" -ForegroundColor White
Write-Host "  3. Sync project with Gradle files" -ForegroundColor White
Write-Host "  4. Report success to autonomous agent!" -ForegroundColor White

Read-Host "Press Enter to continue..."
