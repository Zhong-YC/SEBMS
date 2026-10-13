param([switch]$Build)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path $PSScriptRoot -Parent
Set-Location $projectRoot
$env:JAVA_HOME=if($env:SEBMS_JAVA_HOME){$env:SEBMS_JAVA_HOME}else{'D:\java\jdk'}
$java=Join-Path $env:JAVA_HOME 'bin/java.exe'
if(!(Test-Path $java)){throw '请设置 SEBMS_JAVA_HOME 为 JDK17 或更高版本目录。'}
$env:Path="$env:JAVA_HOME\bin;$env:Path"
if(!(Test-Path '.local/secrets.json')){throw '请先运行 scripts/setup-db.ps1。'}
$secret=Get-Content '.local/secrets.json' -Raw | ConvertFrom-Json
$env:DB_URL='jdbc:mysql://127.0.0.1:3307/sebms?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai'
$env:DB_USER='sebms';$env:DB_PASSWORD=$secret.dbPassword
function Test-Port($port){$c=[System.Net.Sockets.TcpClient]::new();try{$t=$c.ConnectAsync('127.0.0.1',$port);return ($t.Wait(700) -and $c.Connected)}catch{return $false}finally{$c.Dispose()}}
$mysqlBase=if($env:SEBMS_MYSQL_HOME){$env:SEBMS_MYSQL_HOME}else{'D:\JavaWeb\mysql-8.0.41-winx64'}
if(!(Test-Port 3307)){
 Start-Process -FilePath "$mysqlBase/bin/mysqld.exe" -ArgumentList ('"--defaults-file='+$projectRoot+'/.local/mysql.ini"') -WindowStyle Hidden | Out-Null
 for($i=0;$i -lt 30 -and !(Test-Port 3307);$i++){Start-Sleep -Milliseconds 500}
 if(!(Test-Port 3307)){throw 'MySQL启动失败，查看.local/logs/mysql.log'}
}
if($Build -or !(Get-ChildItem 'backend/target/*.jar' -ErrorAction SilentlyContinue)){
 Push-Location backend
 try{& mvn.cmd -s settings.xml -DskipTests package; if($LASTEXITCODE -ne 0){throw '后端构建失败'}}finally{Pop-Location}
}
if(!(Test-Path 'frontend/node_modules')){Push-Location frontend;try{& npm.cmd ci;if($LASTEXITCODE -ne 0){throw '前端依赖安装失败'}}finally{Pop-Location}}
if($Build){Push-Location frontend;try{& npm.cmd run build;if($LASTEXITCODE -ne 0){throw '前端构建失败'}}finally{Pop-Location}}
if(!(Test-Port 8080)){
 $jar=(Get-ChildItem 'backend/target/*.jar' | Where-Object Name -NotLike '*original*' | Select-Object -First 1).FullName
 $p=Start-Process -FilePath $java -ArgumentList @('-jar',('"'+$jar+'"')) -WorkingDirectory $projectRoot -RedirectStandardOutput "$projectRoot/.local/logs/backend.log" -RedirectStandardError "$projectRoot/.local/logs/backend-error.log" -WindowStyle Hidden -PassThru
 $p.Id | Set-Content '.local/backend.pid'
}
if(!(Test-Port 5173)){
 $node=(Get-Command node.exe).Source
 $p=Start-Process -FilePath $node -ArgumentList @(('"'+$projectRoot+'/frontend/node_modules/vite/bin/vite.js"'),'--host','127.0.0.1') -WorkingDirectory "$projectRoot/frontend" -RedirectStandardOutput "$projectRoot/.local/logs/frontend.log" -RedirectStandardError "$projectRoot/.local/logs/frontend-error.log" -WindowStyle Hidden -PassThru
 $p.Id | Set-Content '.local/frontend.pid'
}
for($i=0;$i -lt 60;$i++){try{$r=Invoke-RestMethod 'http://127.0.0.1:8080/api/health' -TimeoutSec 2;if($r.code -eq 0){break}}catch{};Start-Sleep -Milliseconds 500}
try{$r=Invoke-RestMethod 'http://127.0.0.1:8080/api/health' -TimeoutSec 2}catch{throw '后端未就绪，请查看.local/logs/backend-error.log和backend.log'}
for($i=0;$i -lt 30 -and !(Test-Port 5173);$i++){Start-Sleep -Milliseconds 500}
if(!(Test-Port 5173)){throw '前端未就绪，请查看.local/logs/frontend-error.log'}
Write-Host 'SEBMS 已启动：http://127.0.0.1:5173'
Write-Host '演示账号：admin / Admin123!；student / Student123!'
