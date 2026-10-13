# 后端自动化集成测试

## 执行方式

在 `backend` 目录中使用 Java 17 运行。未指定测试数据库时，默认使用独立的 H2 内存数据库（MySQL 兼容模式），用于快速验证。

```powershell
$env:JAVA_HOME = 'D:\java\jdk'
& 'D:\JavaWeb\apache-maven-3.9.4\bin\mvn.cmd' -s settings.xml test '-DargLine=-Dfile.encoding=UTF-8'
```

MySQL 验证必须使用独立测试库。测试类每次测试前清理测试夹具，仅接受名称以 `_test` 结尾的 MySQL 数据库，不能传入项目业务库 `sebms`。本地项目实例的复现方式如下；数据库密码仅从私有配置读取到当前进程，不写入代码和报告：

```powershell
$env:JAVA_HOME = 'D:\java\jdk'
$testSecrets = Get-Content '..\.local\secrets.json' -Raw | ConvertFrom-Json
$env:TEST_DB_URL = 'jdbc:mysql://127.0.0.1:3307/sebms_test?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false'
$env:TEST_DB_USER = 'sebms'
$env:TEST_DB_PASSWORD = $testSecrets.dbPassword
try {
    & 'D:\JavaWeb\apache-maven-3.9.4\bin\mvn.cmd' -s settings.xml test '-DargLine=-Dfile.encoding=UTF-8'
} finally {
    Remove-Item Env:TEST_DB_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:TEST_DB_URL -ErrorAction SilentlyContinue
    Remove-Item Env:TEST_DB_USER -ErrorAction SilentlyContinue
}
```

## 覆盖内容

`BackendIntegrationTest` 在完整 Spring Boot 应用上下文中通过 MockMvc 调用实际 API，使用真实 MyBatis SQL 和数据库事务，不模拟数据库。

- 登录、注销、未登录拒绝、统一错误结构、密码不返回、资料修改及旧会话失效。
- 来源校验、参数类型和边界、每项管理员写入接口及用户/统计查询权限。
- 借用库存不足、记录归属、普通用户查询隔离、重复归还、总量调整、搜索、统计和日期边界。
- 两个线程同时借用最后一件器材，只有一次成功；两个会话同时归还同一记录，只恢复一件库存。
- 在真实数据库增加临时 CHECK 约束，使扣库存后的借用记录插入失败，确认库存和记录一起回滚，测试后移除约束。
- 模拟库存异常，使归还记录更新后的库存恢复失败，确认状态与归还时间回滚，然后修正夹具并确认可再次归还。
- 两个管理员同时降级或同时停用，仍保留至少一个启用的管理员；禁止删除自己及有历史的关联记录。
- Java 演示初始化和 SQL 演示初始化均可重复执行；SQL 演示账号可登录，重复初始化保留已改密码、姓名、借用记录及库存。

## 已执行结果

2026-10-13，Java 17.0.11 / Spring Boot 3.5.8：

| 数据库 | 测试数量 | 失败 | 错误 | 跳过 | 测试用时 |
| --- | ---: | ---: | ---: | ---: | ---: |
| MySQL，127.0.0.1:3307/sebms_test | 17 | 0 | 0 | 0 | 19.43 秒 |
| H2，MySQL 兼容模式 | 17 | 0 | 0 | 0 | 18.68 秒 |

Maven 原始报告每次写入 `target/surefire-reports`；本次两种数据库的独立报告保存在 `target/test-evidence/mysql` 与 `target/test-evidence/h2`，执行日志分别为 `target/test-evidence/mysql-test.log` 和 `target/test-evidence/h2-test.log`。报告覆盖服务端集成行为；浏览器联调验收由项目总测试报告记录。
