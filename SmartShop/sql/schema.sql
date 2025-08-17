-- SmartShop FULL schema for SQL Server (SSMS 19)
-- Safe to run multiple times (idempotent-ish). Uses GO to split batches.

/* 0) Create DB if missing */
IF DB_ID('SmartShop') IS NULL
BEGIN
    CREATE DATABASE SmartShop;
END
GO

/* 1) Use DB */
USE SmartShop;
GO

/* 2) Drop views first (if exist) */
IF OBJECT_ID('dbo.v_ProductCountByCategory', 'V') IS NOT NULL DROP VIEW dbo.v_ProductCountByCategory;
IF OBJECT_ID('dbo.v_NewCustomersByMonth', 'V') IS NOT NULL DROP VIEW dbo.v_NewCustomersByMonth;
IF OBJECT_ID('dbo.v_NewCustomersByDay', 'V') IS NOT NULL DROP VIEW dbo.v_NewCustomersByDay;
IF OBJECT_ID('dbo.v_BestSellers', 'V') IS NOT NULL DROP VIEW dbo.v_BestSellers;
IF OBJECT_ID('dbo.v_RevenueByDay', 'V') IS NOT NULL DROP VIEW dbo.v_RevenueByDay;
GO

/* 3) Drop tables in FK order */
IF OBJECT_ID('dbo.RolePermissions') IS NOT NULL DROP TABLE dbo.RolePermissions;
IF OBJECT_ID('dbo.Permissions')     IS NOT NULL DROP TABLE dbo.Permissions;
IF OBJECT_ID('dbo.Roles')           IS NOT NULL DROP TABLE dbo.Roles;

IF OBJECT_ID('dbo.OrderItems') IS NOT NULL DROP TABLE dbo.OrderItems;
IF OBJECT_ID('dbo.Orders')     IS NOT NULL DROP TABLE dbo.Orders;
IF OBJECT_ID('dbo.Reviews')    IS NOT NULL DROP TABLE dbo.Reviews;
IF OBJECT_ID('dbo.Products')   IS NOT NULL DROP TABLE dbo.Products;
IF OBJECT_ID('dbo.Categories') IS NOT NULL DROP TABLE dbo.Categories;
IF OBJECT_ID('dbo.Users')      IS NOT NULL DROP TABLE dbo.Users;
GO

/* 4) Create tables */
CREATE TABLE dbo.Roles(
  name        NVARCHAR(20)  NOT NULL PRIMARY KEY,
  description NVARCHAR(200) NULL
);
GO

CREATE TABLE dbo.Permissions(
  code        NVARCHAR(50)  NOT NULL PRIMARY KEY,
  description NVARCHAR(200) NULL
);
GO

CREATE TABLE dbo.Users(
  id            INT IDENTITY(1,1) PRIMARY KEY,
  username      NVARCHAR(50)  NOT NULL UNIQUE,
  password_hash NVARCHAR(255) NOT NULL,
  email         NVARCHAR(100) NULL,
  phone         NVARCHAR(20)  NULL,
  full_name     NVARCHAR(100) NULL,
  role          NVARCHAR(20)  NOT NULL DEFAULT 'CUSTOMER',
  active        BIT           NOT NULL DEFAULT 1,
  reset_token   NVARCHAR(100) NULL,
  reset_expires DATETIME2     NULL,
  created_at    DATETIME2     NOT NULL DEFAULT SYSDATETIME()
);
GO

CREATE TABLE dbo.Categories(
  id   INT IDENTITY(1,1) PRIMARY KEY,
  name NVARCHAR(100) NOT NULL
);
GO

CREATE TABLE dbo.Products(
  id           INT IDENTITY(1,1) PRIMARY KEY,
  name         NVARCHAR(150) NOT NULL,
  brand        NVARCHAR(50)  NULL,
  color        NVARCHAR(30)  NULL,
  description  NVARCHAR(MAX) NULL,
  image_url    NVARCHAR(400) NULL,
  price        DECIMAL(18,2) NOT NULL,
  category_id  INT           NOT NULL,
  stock        INT           NOT NULL DEFAULT 0,
  sold         INT           NOT NULL DEFAULT 0,
  rating       DECIMAL(3,2)  NOT NULL DEFAULT 0,
  updated_at   DATETIME2     NOT NULL DEFAULT SYSDATETIME(),
  CONSTRAINT FK_Products_Categories FOREIGN KEY(category_id) REFERENCES dbo.Categories(id)
);
GO

CREATE TABLE dbo.Orders(
  id           INT IDENTITY(1,1) PRIMARY KEY,
  user_id      INT           NOT NULL,
  total_amount DECIMAL(18,2) NOT NULL,
  status       NVARCHAR(20)  NOT NULL DEFAULT 'PENDING', -- PENDING, PAID, SHIPPED, COMPLETED, CANCELED
  created_at   DATETIME2     NOT NULL DEFAULT SYSDATETIME(),
  CONSTRAINT FK_Orders_Users FOREIGN KEY(user_id) REFERENCES dbo.Users(id)
);
GO

CREATE TABLE dbo.OrderItems(
  id         INT IDENTITY(1,1) PRIMARY KEY,
  order_id   INT NOT NULL,
  product_id INT NOT NULL,
  quantity   INT NOT NULL CHECK (quantity > 0),
  unit_price DECIMAL(18,2) NOT NULL,
  CONSTRAINT FK_OrderItems_Orders   FOREIGN KEY(order_id)   REFERENCES dbo.Orders(id)   ON DELETE CASCADE,
  CONSTRAINT FK_OrderItems_Products FOREIGN KEY(product_id) REFERENCES dbo.Products(id)
);
GO

CREATE TABLE dbo.Reviews(
  id         INT IDENTITY(1,1) PRIMARY KEY,
  product_id INT NOT NULL,
  user_id    INT NOT NULL,
  rating     INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
  comment    NVARCHAR(1000) NULL,
  created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
  CONSTRAINT FK_Reviews_Products FOREIGN KEY(product_id) REFERENCES dbo.Products(id),
  CONSTRAINT FK_Reviews_Users    FOREIGN KEY(user_id)    REFERENCES dbo.Users(id)
);
GO

CREATE TABLE dbo.RolePermissions(
  role NVARCHAR(20) NOT NULL,
  perm NVARCHAR(50) NOT NULL,
  CONSTRAINT PK_RolePerm PRIMARY KEY(role,perm),
  CONSTRAINT FK_RolePerm_Role FOREIGN KEY(role) REFERENCES dbo.Roles(name),
  CONSTRAINT FK_RolePerm_Perm FOREIGN KEY(perm) REFERENCES dbo.Permissions(code)
);
GO

/* 5) Constraints after tables */
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name='FK_Users_Roles')
ALTER TABLE dbo.Users ADD CONSTRAINT FK_Users_Roles
FOREIGN KEY(role) REFERENCES dbo.Roles(name);
GO

/* 6) Seed lookup data */
MERGE dbo.Roles AS t USING (VALUES
('ADMIN',N'Quản trị'),
('CUSTOMER',N'Người dùng')
) AS s(name,description) ON t.name=s.name
WHEN NOT MATCHED THEN INSERT(name,description) VALUES(s.name,s.description);
GO

MERGE dbo.Permissions AS t USING (VALUES
(N'MANAGE_PRODUCTS',N'Quản lý sản phẩm'),
(N'MANAGE_USERS',N'Quản lý tài khoản'),
(N'VIEW_ORDER_REPORTS',N'Xem thống kê đơn hàng'),
(N'SHOP',N'Xem/mua hàng'),
(N'REVIEW',N'Đánh giá sản phẩm'),
(N'MANAGE_SELF',N'Quản lý tài khoản cá nhân')
) AS s(code,description) ON t.code=s.code
WHEN NOT MATCHED THEN INSERT(code,description) VALUES(s.code,s.description);
GO

MERGE dbo.RolePermissions AS t USING (VALUES
('ADMIN','MANAGE_PRODUCTS'),
('ADMIN','MANAGE_USERS'),
('ADMIN','VIEW_ORDER_REPORTS'),
('ADMIN','SHOP'),
('ADMIN','REVIEW'),
('ADMIN','MANAGE_SELF')
) AS s(role,perm) ON t.role=s.role AND t.perm=s.perm
WHEN NOT MATCHED THEN INSERT(role,perm) VALUES(s.role,s.perm);
GO

MERGE dbo.RolePermissions AS t USING (VALUES
('CUSTOMER','SHOP'),
('CUSTOMER','REVIEW'),
('CUSTOMER','MANAGE_SELF')
) AS s(role,perm) ON t.role=s.role AND t.perm=s.perm
WHEN NOT MATCHED THEN INSERT(role,perm) VALUES(s.role,s.perm);
GO

/* 7) Seed categories and products */
IF NOT EXISTS (SELECT 1 FROM dbo.Categories)
BEGIN
  INSERT INTO dbo.Categories(name) VALUES (N'Smartphone'),(N'Phụ kiện');
END
GO

IF NOT EXISTS (SELECT 1 FROM dbo.Products)
BEGIN
  INSERT INTO dbo.Products(name,brand,color,description,image_url,price,category_id,stock,sold,rating) VALUES
   (N'iPhone 14', N'Apple',   N'Đen',  N'Apple A15',          N'https://via.placeholder.com/300x200', 19990000, 1, 50, 10, 4.70),
   (N'Galaxy S23',N'Samsung', N'Trắng',N'Snapdragon 8 Gen 2', N'https://via.placeholder.com/300x200', 18990000, 1, 40, 20, 4.60),
   (N'Xiaomi 13', N'Xiaomi',  N'Xanh', N'SD 8 Gen 2',         N'https://via.placeholder.com/300x200', 12990000, 1, 60, 15, 4.50);
END
GO

/* 8) Seed admin and a sample user
   Hashes are BCrypt for:
   - admin -> Admin@12345
   - user1 -> User@12345
*/
IF NOT EXISTS (SELECT 1 FROM dbo.Users WHERE username='admin')
INSERT INTO dbo.Users(username,password_hash,email,phone,full_name,role,active)
VALUES('admin',
'$2b$12$Mu34wHUDAZBuJkQ5HLY08ecPeYiFCFMnzCz.D6i7ME2U3k3NOovAu',
'admin@local','0000000001',N'Administrator','ADMIN',1);
GO

IF NOT EXISTS (SELECT 1 FROM dbo.Users WHERE username='user1')
INSERT INTO dbo.Users(username,password_hash,email,phone,full_name,role,active)
VALUES('user1',
'$2b$12$YIFMQIF5CeVkriyDDAPyzOchyxXn0BI2.e7oYO65bKl0BxgKufcRq',
'user1@local','0000000002',N'User One','CUSTOMER',1);
GO

/* 9) Helpful indexes */
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name='IX_Products_Category')
CREATE INDEX IX_Products_Category ON dbo.Products(category_id);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name='IX_Products_Price')
CREATE INDEX IX_Products_Price ON dbo.Products(price);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name='IX_Orders_UserDate')
CREATE INDEX IX_Orders_UserDate ON dbo.Orders(user_id, created_at);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name='IX_Users_CreatedAt')
CREATE INDEX IX_Users_CreatedAt ON dbo.Users(created_at);
GO

/* 10) Views */
CREATE VIEW dbo.v_RevenueByDay AS
SELECT CAST(created_at AS date) AS d,
       SUM(total_amount)        AS revenue
FROM dbo.Orders
WHERE status IN (N'PAID',N'SHIPPED',N'COMPLETED')
GROUP BY CAST(created_at AS date);
GO

CREATE VIEW dbo.v_BestSellers AS
SELECT TOP (10)
       p.id,
       p.name,
       SUM(oi.quantity) AS qty
FROM dbo.OrderItems oi
JOIN dbo.Products    p ON p.id = oi.product_id
GROUP BY p.id, p.name
ORDER BY qty DESC;
GO

CREATE VIEW dbo.v_NewCustomersByDay AS
SELECT CAST(created_at AS date) AS d,
       COUNT(*)                 AS new_customers
FROM dbo.Users
GROUP BY CAST(created_at AS date);
GO

CREATE VIEW dbo.v_NewCustomersByMonth AS
SELECT DATEFROMPARTS(YEAR(created_at), MONTH(created_at), 1) AS ym,
       COUNT(*)                                            AS new_customers
FROM dbo.Users
GROUP BY DATEFROMPARTS(YEAR(created_at), MONTH(created_at), 1);
GO

CREATE VIEW dbo.v_ProductCountByCategory AS
SELECT c.id,
       c.name,
       COUNT(p.id) AS product_count
FROM dbo.Categories c
LEFT JOIN dbo.Products p ON p.category_id = c.id
GROUP BY c.id, c.name;
GO

USE SmartShop;
GO
-- Bảo đảm role tồn tại
IF NOT EXISTS (SELECT 1 FROM dbo.Roles WHERE name='ADMIN')
    INSERT INTO dbo.Roles(name,description) VALUES('ADMIN',N'Quản trị');

-- Tạo mới hoặc chuẩn hóa tài khoản admin với mật khẩu: Admin@12345
IF EXISTS (SELECT 1 FROM dbo.Users WHERE username='admin')
    UPDATE dbo.Users
      SET password_hash = '$2b$12$Mu34wHUDAZBuJkQ5HLY08ecPeYiFCFMnzCz.D6i7ME2U3k3NOovAu', -- BCrypt(Admin@12345)
          role = 'ADMIN',
          active = 1,
          email = COALESCE(email,'admin@local'),
          full_name = COALESCE(full_name,N'Administrator'),
          phone = COALESCE(phone,'0000000001'),
          reset_token = NULL,
          reset_expires = NULL
    WHERE username='admin';
ELSE
    INSERT INTO dbo.Users(username,password_hash,email,phone,full_name,role,active)
    VALUES ('admin',
            '$2b$12$Mu34wHUDAZBuJkQ5HLY08ecPeYiFCFMnzCz.D6i7ME2U3k3NOovAu', -- BCrypt(Admin@12345)
            'admin@local','0000000001',N'Administrator','ADMIN',1);
GO

-- Kiểm tra
SELECT username, role, active FROM dbo.Users WHERE username='admin';

USE SmartShop;
UPDATE dbo.Users SET password_hash='Admin@12345' WHERE username='admin';

USE SmartShop;
SELECT TOP (10) id, username,email, role, active, created_at
FROM dbo.Users ORDER BY id DESC;

USE SmartShop;
IF COL_LENGTH('dbo.Users','reset_code') IS NULL
  ALTER TABLE dbo.Users ADD reset_code NVARCHAR(6) NULL;
