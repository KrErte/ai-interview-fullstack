# Käivitab backendi (8080) ja frontend-risk'i (4200) eraldi akendes ning avab AI testi.
$root = $PSScriptRoot
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$root\backend'; .\gradlew.bat bootRun"
Set-Location "$root\frontend-risk"
if (-not (Test-Path node_modules)) { npm install }
Start-Job { Start-Sleep 25; Start-Process "http://localhost:4200/ai-test" } | Out-Null
npx ng serve --port 4200
