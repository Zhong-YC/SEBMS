# API v1 固定契约

服务127.0.0.1:8080，前端127.0.0.1:5173代理/api。JSON响应 {code:0,message:"成功",data:...}。失败使用非2xx和 {code:HTTP状态,message:"中文原因",data:null}。
认证使用后端 HttpSession cookie，Axios withCredentials；同源代理。状态修改请求防跨站，检查Origin仅允许本地5173/8080（无Origin CLI允许）。勿在前端伪造权限。
POST /api/auth/login {username,password} => User
GET /api/auth/me => User；POST /api/auth/logout => null
PUT /api/auth/profile {displayName,password?} => User（仅自己的资料，不能修改角色、状态、用户名）
User {id,username,displayName,role:ADMIN|USER,enabled:boolean}，不返回密码。
GET /api/categories => Category[] {id,name}
POST /api/categories {name} => Category；PUT /api/categories/{id} {name}；DELETE 同路径（仅ADMIN）
GET /api/equipment?keyword=&categoryId= => Equipment[]
Equipment {id,name,description,categoryId,categoryName,totalQuantity,availableQuantity,enabled:boolean}
POST /api/equipment {name,description,categoryId,totalQuantity,enabled} => Equipment
PUT /api/equipment/{id} 同创建字段 => Equipment；DELETE 同路径仅无借用历史时（有历史提示停用）。管理员接口。
POST /api/borrows {equipmentId} => Borrow
POST /api/borrows/{id}/return => Borrow（本人、BORROWING）
GET /api/borrows?status=&keyword=&startDate=&endDate= => Borrow[]（USER始终仅本人；ADMIN全部，keyword匹配器材/用户名，日期YYYY-MM-DD）
Borrow {id,userId,username,displayName,equipmentId,equipmentName,borrowTime,returnTime,status:BORROWING|RETURNED}
GET /api/users => User[]；POST /api/users {username,password,displayName,role,enabled} => User；PUT /api/users/{id} {displayName,role,enabled,password?} => User；DELETE 同路径（ADMIN，保留自己与至少1管理员，有历史不能删除）
GET /api/stats => {totalQuantity,equipmentTypes,availableQuantity,borrowedQuantity,userCount}（ADMIN）
GET /api/health => {status:"UP"}（公开，遵循统一响应）
日期ISO本地日期时间；空归还时间null。输入必须trim及长度范围校验，总量非负整数，库存不可前端传入。
演示账号 admin/Admin123! 与 student/Student123!，仅演示数据初始化使用，README说明。

长度范围：username 3–30（ASCII字母数字下划线），password 8–64，displayName 1–50，category.name 1–50，equipment.name 1–100、description 0–500，totalQuantity整数0–1000000。修改时空password表示不修改。
