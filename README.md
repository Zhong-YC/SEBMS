# SEBMS 体育器材借用管理系统

毕业设计项目：基于 Spring Boot 的体育器材借用管理系统的设计与实现。

## 本机启动

也可以直接双击项目根目录的 `start.cmd` 启动，双击 `stop.cmd` 停止前后端。

在项目目录打开 PowerShell：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start.ps1
```

打开 http://127.0.0.1:5173 。演示账号：管理员 `admin / Admin123!`，普通用户 `student / Student123!`。只供本机毕业设计演示，正式部署前更换账号密码和会话安全配置。

修改代码后后端需要重新构建并重启，可使用 `start.ps1 -Build` 构建（先停止已有后端）。前端开发服务器自动更新。

停止前后端：`powershell -ExecutionPolicy Bypass -File .\scripts\stop.ps1`。数据库保持运行。
回归测试：`powershell -ExecutionPolicy Bypass -File .\scripts\test.ps1`（只使用独立测试库）。

## 环境与数据

- Java17：当前 `D:\java\jdk`，可设置 `SEBMS_JAVA_HOME`。
- Maven3.9、Node22、npm10。
- MySQL8：当前 `D:\JavaWeb\mysql-8.0.41-winx64`，可设置 `SEBMS_MYSQL_HOME`。
- 独立实例监听 `127.0.0.1:3307`，数据在 `.local/mysql-data`。应用库 `sebms`、测试库 `sebms_test`。
- `.local/secrets.json` 存放随机生成的本机开发数据库凭据，已被Git忽略，勿分享。
- 首次在新机器上使用，先运行 `scripts/setup-db.ps1`；现有机器已由初始化流程创建。
- 不要删除 `.local/mysql-data`，那里保存实际开发数据。

## 项目结构

`backend/` 后端与数据库脚本；`frontend/` 前端；`docs/` 需求、接口、测试记录；`scripts/` 启动验收脚本；`STATUS.md` 当前进度。

需求和接口以 `docs/REQUIREMENTS.md`、`docs/API.md` 为准。版本兼容依据：[Spring Boot系统要求](https://docs.spring.io/spring-boot/3.5/system-requirements.html)、[MyBatis兼容表](https://mybatis.org/spring-boot-starter/mybatis-spring-boot-autoconfigure/)。

## 业务范围

普通用户查器材、借还及查询本人记录、修改本人资料；管理员维护用户/分类/器材，查全部记录和统计。每条借用记录1件，线下交接同步登记。没有预约、审批、支付、罚款或扫码功能。

库存条件更新与记录修改放在同一事务中，归还检查归属和状态。第一阶段已通过验收，实际测试结果见 [测试报告](docs/TEST-REPORT.md)，当前进度见 [STATUS.md](STATUS.md)。


## 演示顺序

1. 使用管理员账号登录，在器材管理查看或新增器材。
2. 退出后用student登录，选择器材登记借用；在我的借还中登记归还。
3. 再以管理员登录，查看全部记录与库存统计。

页面中带“验收”的数据来自真实测试，关联记录已经归还并保留。重启后端后需重新登录。
建表和可重复执行的演示数据见backend/sql/；后端测试说明见backend/TESTING.md。


## GitHub仓库

公开仓库：https://github.com/Zhong-YC/SEBMS 。
提交记录见 [版本归档说明](docs/GIT-HISTORY.md)。本机数据库数据、私有凭据、依赖缓存及构建产物均由Git忽略规则排除。
