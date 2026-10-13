$ErrorActionPreference='Stop'
$projectRoot=Split-Path $PSScriptRoot -Parent
Set-Location $projectRoot
$mysqlBase=if($env:SEBMS_MYSQL_HOME){$env:SEBMS_MYSQL_HOME}else{'D:\JavaWeb\mysql-8.0.41-winx64'}
if(Test-Path '.local/secrets.json'){Write-Host '项目数据库配置已存在，不重新初始化。';exit 0}
if(Test-Path '.local/mysql-data/auto.cnf'){throw '发现已有数据但缺少凭据，停止初始化，避免覆盖。'}
New-Item -ItemType Directory -Force '.local/mysql-data','.local/logs' | Out-Null
$dbPass=[Guid]::NewGuid().ToString('N');$rootPass=[Guid]::NewGuid().ToString('N')
@{dbPassword=$dbPass;rootPassword=$rootPass}|ConvertTo-Json|Set-Content '.local/secrets.json' -Encoding utf8
$forwardRoot=$projectRoot.Replace('\','/');$forwardMysql=$mysqlBase.Replace('\','/')
@"
[mysqld]
basedir=$forwardMysql
datadir=$forwardRoot/.local/mysql-data
port=3307
bind-address=127.0.0.1
mysqlx=0
character-set-server=utf8mb4
log-error=$forwardRoot/.local/logs/mysql.log
pid-file=$forwardRoot/.local/mysql.pid
"@|Set-Content '.local/mysql.ini' -Encoding utf8
& "$mysqlBase/bin/mysqld.exe" --no-defaults --initialize-insecure "--basedir=$mysqlBase" "--datadir=$projectRoot/.local/mysql-data" --console
if($LASTEXITCODE -ne 0){throw '数据库初始化失败，请检查日志，不要删除已有数据。'}
@"
CREATE DATABASE IF NOT EXISTS sebms CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS sebms_test CHARACTER SET utf8mb4;
CREATE USER IF NOT EXISTS 'sebms'@'localhost' IDENTIFIED BY '$dbPass';
GRANT ALL PRIVILEGES ON sebms.* TO 'sebms'@'localhost';
GRANT ALL PRIVILEGES ON sebms_test.* TO 'sebms'@'localhost';
ALTER USER 'root'@'localhost' IDENTIFIED BY '$rootPass';
"@|Set-Content '.local/init.sql' -Encoding ascii
Start-Process -FilePath "$mysqlBase/bin/mysqld.exe" -ArgumentList @(('"--defaults-file='+$projectRoot+'/.local/mysql.ini"'),('"--init-file='+$projectRoot+'/.local/init.sql"')) -WindowStyle Hidden | Out-Null
$env:MYSQL_PWD=$dbPass
try{
 for($i=0;$i -lt 30;$i++){
  & "$mysqlBase/bin/mysql.exe" '--host=127.0.0.1' '--port=3307' '--user=sebms' '--connect-timeout=1' -e 'SELECT 1;' 2>$null | Out-Null
  if($LASTEXITCODE -eq 0){Remove-Item -LiteralPath '.local/init.sql';Write-Host '独立数据库已就绪。';exit 0}
  Start-Sleep -Milliseconds 500
 }
 throw '数据库未就绪，请查看.local/logs/mysql.log。'
}finally{Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue}
