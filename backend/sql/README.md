# 建表与演示数据

`schema.sql` 与 `src/main/resources/schema.sql` 内容相同，为 MySQL 8 建立四张业务表、外键、库存及借还状态约束。`demo-data.sql` 仅补充缺失的演示用户、分类和器材；重复执行保留现有密码、姓名、借用记录和库存。

应用正常启动时会自动执行资源中的建表脚本，并由 `DemoData` 初始化演示数据。也可在 MySQL 客户端手动执行以下命令（先连接已经创建的项目数据库；文件路径按实际位置调整）：

```sql
USE sebms;
SOURCE E:/academic paper/SEBMS/backend/sql/schema.sql;
SOURCE E:/academic paper/SEBMS/backend/sql/demo-data.sql;
```

SQL 文件以 UTF-8 保存，客户端连接字符集使用 utf8mb4。演示账号为 `admin / Admin123!`、`student / Student123!`，数据库只存储 BCrypt 哈希。设置进程环境变量 `DEMO_DATA=false` 可关闭应用的演示初始化。SQL 不包含本地数据库账号密码，也不删除现有数据。
