$ErrorActionPreference='Stop'
$projectRoot=Split-Path $PSScriptRoot -Parent
foreach($name in @('backend','frontend')){
 $pidFile=Join-Path $projectRoot ".local/$name.pid"
 if(!(Test-Path $pidFile)){continue}
 $savedPid=[int](Get-Content $pidFile)
 $process=Get-CimInstance Win32_Process -Filter "ProcessId=$savedPid" -ErrorAction SilentlyContinue
 if($process -and $process.CommandLine -and $process.CommandLine.Contains($projectRoot)){
  Stop-Process -Id $savedPid
  Write-Host "已停止 $name"
 }elseif($process){Write-Warning "$name 的PID已被其他进程使用或无法核实，未终止。";continue}
 Remove-Item -LiteralPath $pidFile
}
Write-Host '数据库保留运行，数据不会删除。'
