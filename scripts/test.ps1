$ErrorActionPreference='Stop'
$projectRoot=Split-Path $PSScriptRoot -Parent
Set-Location $projectRoot
$env:JAVA_HOME=if($env:SEBMS_JAVA_HOME){$env:SEBMS_JAVA_HOME}else{'D:\java\jdk'}
$env:Path="$env:JAVA_HOME\bin;$env:Path"
$secret=Get-Content '.local/secrets.json' -Raw | ConvertFrom-Json
$env:TEST_DB_URL='jdbc:mysql://127.0.0.1:3307/sebms_test?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai'
$env:TEST_DB_USER='sebms';$env:TEST_DB_PASSWORD=$secret.dbPassword
Push-Location backend
try{& mvn.cmd -s settings.xml test;if($LASTEXITCODE -ne 0){throw '后端测试失败'}}finally{Pop-Location}
Push-Location frontend
try{& npm.cmd run build;if($LASTEXITCODE -ne 0){throw '前端构建失败'}}finally{Pop-Location}
Write-Host '测试与构建完成。HTTP验收另执行 scripts/smoke_test.py，需要已启动应用及Python3。'
