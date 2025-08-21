/* =======================================================================
   SMARTSHOP — CONSOLIDATED SCHEMA & SAFE SEED (SQL Server / SSMS 19)
   - Idempotent: chạy nhiều lần vẫn đúng.
   - Không xóa dữ liệu thật. Không DROP table hàng loạt.
   - Bổ sung cột/Index/Constraint nếu thiếu.
   - Seed quyền, vai trò, danh mục, user admin qua MERGE/IF NOT EXISTS.
   - Seed sản phẩm bằng MERGE (không delete trước).
   - Ảnh tự sinh đường dẫn nhưng KHÔNG ghi đè ảnh đang có.
   - Admin: admin / Admin@12345 (BCrypt lưu trong DB).
   ======================================================================= */

/* 0) Create DB if missing + USE */
IF DB_ID('SmartShop') IS NULL
BEGIN
    CREATE DATABASE SmartShop;
END;
GO
USE SmartShop;
GO

/* 1) Create tables if missing (chuẩn cuối) */
IF OBJECT_ID('dbo.Roles','U') IS NULL
CREATE TABLE dbo.Roles(
  name        NVARCHAR(20)  NOT NULL PRIMARY KEY,
  description NVARCHAR(200) NULL
);
GO

IF OBJECT_ID('dbo.Permissions','U') IS NULL
CREATE TABLE dbo.Permissions(
  code        NVARCHAR(50)  NOT NULL PRIMARY KEY,
  description NVARCHAR(200) NULL
);
GO

IF OBJECT_ID('dbo.Users','U') IS NULL
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
  reset_code    NVARCHAR(6)   NULL,
  created_at    DATETIME2     NOT NULL DEFAULT SYSDATETIME()
);
GO

IF OBJECT_ID('dbo.Categories','U') IS NULL
CREATE TABLE dbo.Categories(
  id   INT IDENTITY(1,1) PRIMARY KEY,
  name NVARCHAR(100) NOT NULL
);
GO

IF OBJECT_ID('dbo.Products','U') IS NULL
CREATE TABLE dbo.Products(
  id            INT IDENTITY(1,1) PRIMARY KEY,
  name          NVARCHAR(150) NOT NULL,
  brand         NVARCHAR(50)  NULL,
  color         NVARCHAR(30)  NULL,
  description   NVARCHAR(MAX) NULL,
  image_url     NVARCHAR(400) NULL,
  price         DECIMAL(18,2) NOT NULL,
  category_id   INT           NOT NULL,
  stock         INT           NOT NULL DEFAULT 0,
  sold          INT           NOT NULL DEFAULT 0,
  rating        DECIMAL(3,2)  NOT NULL DEFAULT 0,
  updated_at    DATETIME2     NOT NULL DEFAULT SYSDATETIME()
);
GO

IF OBJECT_ID('dbo.Orders','U') IS NULL
CREATE TABLE dbo.Orders(
  id           INT IDENTITY(1,1) PRIMARY KEY,
  user_id      INT           NOT NULL,
  total_amount DECIMAL(18,2) NOT NULL,
  status       NVARCHAR(20)  NOT NULL DEFAULT 'PENDING', -- PENDING, PAID, SHIPPED, COMPLETED, CANCELED
  created_at   DATETIME2     NOT NULL DEFAULT SYSDATETIME()
);
GO

IF OBJECT_ID('dbo.OrderItems','U') IS NULL
CREATE TABLE dbo.OrderItems(
  id         INT IDENTITY(1,1) PRIMARY KEY,
  order_id   INT NOT NULL,
  product_id INT NOT NULL,
  quantity   INT NOT NULL CHECK (quantity > 0),
  unit_price DECIMAL(18,2) NOT NULL
);
GO

IF OBJECT_ID('dbo.Reviews','U') IS NULL
CREATE TABLE dbo.Reviews(
  id         INT IDENTITY(1,1) PRIMARY KEY,
  product_id INT NOT NULL,
  user_id    INT NOT NULL,
  rating     INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
  comment    NVARCHAR(1000) NULL,
  created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);
GO

IF OBJECT_ID('dbo.RolePermissions','U') IS NULL
CREATE TABLE dbo.RolePermissions(
  role NVARCHAR(20) NOT NULL,
  perm NVARCHAR(50) NOT NULL,
  CONSTRAINT PK_RolePerm PRIMARY KEY(role,perm)
);
GO

/* 2) Bổ sung cột nếu thiếu (schema nâng cao) */
IF COL_LENGTH('dbo.Users','reset_code') IS NULL
  ALTER TABLE dbo.Users ADD reset_code NVARCHAR(6) NULL;

IF COL_LENGTH('dbo.Products','series')      IS NULL ALTER TABLE dbo.Products ADD series NVARCHAR(80) NULL;
IF COL_LENGTH('dbo.Products','model')       IS NULL ALTER TABLE dbo.Products ADD model NVARCHAR(80) NULL;
IF COL_LENGTH('dbo.Products','cpu')         IS NULL ALTER TABLE dbo.Products ADD cpu NVARCHAR(120) NULL;
IF COL_LENGTH('dbo.Products','ram')         IS NULL ALTER TABLE dbo.Products ADD ram NVARCHAR(40) NULL;
IF COL_LENGTH('dbo.Products','storage')     IS NULL ALTER TABLE dbo.Products ADD storage NVARCHAR(60) NULL;
IF COL_LENGTH('dbo.Products','display')     IS NULL ALTER TABLE dbo.Products ADD display NVARCHAR(120) NULL;
IF COL_LENGTH('dbo.Products','camera')      IS NULL ALTER TABLE dbo.Products ADD camera NVARCHAR(120) NULL;
IF COL_LENGTH('dbo.Products','battery')     IS NULL ALTER TABLE dbo.Products ADD battery NVARCHAR(120) NULL;
IF COL_LENGTH('dbo.Products','os')          IS NULL ALTER TABLE dbo.Products ADD os NVARCHAR(50) NULL;
IF COL_LENGTH('dbo.Products','weight')      IS NULL ALTER TABLE dbo.Products ADD weight NVARCHAR(40) NULL;
IF COL_LENGTH('dbo.Products','dimensions')  IS NULL ALTER TABLE dbo.Products ADD dimensions NVARCHAR(80) NULL;
GO

/* 3) FKs & Unique Indexes nếu thiếu */
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name='FK_Users_Roles')
  ALTER TABLE dbo.Users  ADD CONSTRAINT FK_Users_Roles FOREIGN KEY(role) REFERENCES dbo.Roles(name);

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name='FK_Products_Categories')
  ALTER TABLE dbo.Products ADD CONSTRAINT FK_Products_Categories FOREIGN KEY(category_id) REFERENCES dbo.Categories(id);

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name='FK_Orders_Users')
  ALTER TABLE dbo.Orders ADD CONSTRAINT FK_Orders_Users FOREIGN KEY(user_id) REFERENCES dbo.Users(id);

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name='FK_OrderItems_Orders')
  ALTER TABLE dbo.OrderItems ADD CONSTRAINT FK_OrderItems_Orders FOREIGN KEY(order_id) REFERENCES dbo.Orders(id) ON DELETE CASCADE;

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name='FK_OrderItems_Products')
  ALTER TABLE dbo.OrderItems ADD CONSTRAINT FK_OrderItems_Products FOREIGN KEY(product_id) REFERENCES dbo.Products(id);

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name='FK_Reviews_Products')
  ALTER TABLE dbo.Reviews ADD CONSTRAINT FK_Reviews_Products FOREIGN KEY(product_id) REFERENCES dbo.Products(id);

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name='FK_Reviews_Users')
  ALTER TABLE dbo.Reviews ADD CONSTRAINT FK_Reviews_Users FOREIGN KEY(user_id) REFERENCES dbo.Users(id);

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name='FK_RolePerm_Role')
  ALTER TABLE dbo.RolePermissions ADD CONSTRAINT FK_RolePerm_Role FOREIGN KEY(role) REFERENCES dbo.Roles(name);

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name='FK_RolePerm_Perm')
  ALTER TABLE dbo.RolePermissions ADD CONSTRAINT FK_RolePerm_Perm FOREIGN KEY(perm) REFERENCES dbo.Permissions(code);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name='UIX_Users_Username')
  CREATE UNIQUE INDEX UIX_Users_Username ON dbo.Users(username);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name='UIX_Categories_Name')
  CREATE UNIQUE INDEX UIX_Categories_Name ON dbo.Categories(name);
GO

/* 4) Seed Roles, Permissions, RolePermissions */
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

MERGE dbo.RolePermissions AS t USING (VALUES
('CUSTOMER','SHOP'),
('CUSTOMER','REVIEW'),
('CUSTOMER','MANAGE_SELF')
) AS s(role,perm) ON t.role=s.role AND t.perm=s.perm
WHEN NOT MATCHED THEN INSERT(role,perm) VALUES(s.role,s.perm);
GO

/* 5) Seed Categories */
MERGE dbo.Categories AS t
USING (VALUES (N'Smartphone'),(N'Laptop'),(N'Phụ kiện')) AS s(name)
ON t.name=s.name
WHEN NOT MATCHED THEN INSERT(name) VALUES(s.name);
GO

/* 6) Seed admin & sample user (BCrypt) */
IF NOT EXISTS (SELECT 1 FROM dbo.Users WHERE username='admin')
INSERT INTO dbo.Users(username,password_hash,email,phone,full_name,role,active)
VALUES('admin',
'$2b$12$Mu34wHUDAZBuJkQ5HLY08ecPeYiFCFMnzCz.D6i7ME2U3k3NOovAu', -- Admin@12345
'admin@local','0000000001',N'Administrator','ADMIN',1);
ELSE
UPDATE dbo.Users
   SET password_hash='$2b$12$Mu34wHUDAZBuJkQ5HLY08ecPeYiFCFMnzCz.D6i7ME2U3k3NOovAu',
       role='ADMIN', active=1,
       email=COALESCE(email,'admin@local'),
       phone=COALESCE(phone,'0000000001'),
       full_name=COALESCE(full_name,N'Administrator'),
       reset_token=NULL, reset_expires=NULL, reset_code=NULL
 WHERE username='admin';

IF NOT EXISTS (SELECT 1 FROM dbo.Users WHERE username='user1')
INSERT INTO dbo.Users(username,password_hash,email,phone,full_name,role,active)
VALUES('user1',
'$2b$12$YIFMQIF5CeVkriyDDAPyzOchyxXn0BI2.e7oYO65bKl0BxgKufcRq', -- User@12345
'user1@local','0000000002',N'User One','CUSTOMER',1);
GO

/* 7) Helpful indexes */
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name='IX_Products_Category')
  CREATE INDEX IX_Products_Category ON dbo.Products(category_id);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name='IX_Products_Price')
  CREATE INDEX IX_Products_Price ON dbo.Products(price);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name='IX_Orders_UserDate')
  CREATE INDEX IX_Orders_UserDate ON dbo.Orders(user_id, created_at);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name='IX_Users_CreatedAt')
  CREATE INDEX IX_Users_CreatedAt ON dbo.Users(created_at);
GO

/* 8) Big product seed via MERGE (không xóa dữ liệu hiện có) */

DECLARE @Seed TABLE(
  name NVARCHAR(200),
  brand NVARCHAR(50),
  color NVARCHAR(30),
  description NVARCHAR(MAX),
  image_url NVARCHAR(400),
  price DECIMAL(18,2),
  category NVARCHAR(50),
  stock INT,
  sold  INT,
  rating DECIMAL(3,2)
);

/* ---- Smartphones: Apple 11→16, Samsung, Xiaomi, OPPO, Honor, vivo, Realme, OnePlus ---- */
INSERT INTO @Seed VALUES
(N'iPhone 11',N'Apple',NULL,N'Chip A13, 4GB/64GB, 6.1" LCD, 12+12MP, 3110mAh',NULL, 8990000,N'Smartphone',60,10,4.70),
(N'iPhone 11 Pro',N'Apple',NULL,N'A13, OLED 5.8", 12+12+12MP',NULL,12990000,N'Smartphone',40,8,4.70),
(N'iPhone 11 Pro Max',N'Apple',NULL,N'A13, OLED 6.5", 12+12+12MP',NULL,14990000,N'Smartphone',35,7,4.70),

(N'iPhone 12 mini',N'Apple',NULL,N'A14, 4GB/64GB, 5.4" OLED',NULL,11990000,N'Smartphone',50,6,4.60),
(N'iPhone 12',N'Apple',NULL,N'A14, 6.1" OLED',NULL,12990000,N'Smartphone',70,12,4.60),
(N'iPhone 12 Pro',N'Apple',NULL,N'A14, 6GB/128GB, 6.1" 60Hz',NULL,16990000,N'Smartphone',45,9,4.65),
(N'iPhone 12 Pro Max',N'Apple',NULL,N'A14, 6.7" 60Hz',NULL,18990000,N'Smartphone',40,8,4.68),

(N'iPhone 13 mini',N'Apple',NULL,N'A15, 5.4" 60Hz',NULL,12990000,N'Smartphone',45,9,4.70),
(N'iPhone 13',N'Apple',NULL,N'A15, 6.1" 60Hz',NULL,13990000,N'Smartphone',90,20,4.75),
(N'iPhone 13 Pro',N'Apple',NULL,N'A15, 6.1" 120Hz',NULL,18990000,N'Smartphone',40,9,4.80),
(N'iPhone 13 Pro Max',N'Apple',NULL,N'A15, 6.7" 120Hz',NULL,20990000,N'Smartphone',35,8,4.85),

(N'iPhone 14',N'Apple',NULL,N'A15, 6GB/128GB, 6.1" OLED',NULL,16990000,N'Smartphone',80,18,4.70),
(N'iPhone 14 Plus',N'Apple',NULL,N'A15, 6.7" OLED',NULL,18990000,N'Smartphone',70,15,4.72),
(N'iPhone 14 Pro',N'Apple',NULL,N'A16, 6.1" 120Hz, 48MP',NULL,23990000,N'Smartphone',50,11,4.85),
(N'iPhone 14 Pro Max',N'Apple',NULL,N'A16, 6.7" 120Hz, 48MP',NULL,26990000,N'Smartphone',45,10,4.90),

(N'iPhone 15',N'Apple',NULL,N'A16, USB-C, 6.1" OLED',NULL,19990000,N'Smartphone',90,25,4.80),
(N'iPhone 15 Plus',N'Apple',NULL,N'A16, USB-C, 6.7" OLED',NULL,21990000,N'Smartphone',80,20,4.80),
(N'iPhone 15 Pro',N'Apple',NULL,N'A17 Pro, 6.1" 120Hz, Titanium',NULL,25990000,N'Smartphone',60,14,4.88),
(N'iPhone 15 Pro Max',N'Apple',NULL,N'A17 Pro, 6.7" 120Hz, 5x',NULL,29990000,N'Smartphone',55,12,4.92),

(N'iPhone 16',N'Apple',NULL,N'A18, 8GB/128GB, 6.1"',NULL,22990000,N'Smartphone',90,10,4.70),
(N'iPhone 16 Plus',N'Apple',NULL,N'A18, 6.7"',NULL,24990000,N'Smartphone',80,9,4.72),
(N'iPhone 16 Pro',N'Apple',NULL,N'A18 Pro, 6.3" 120Hz',NULL,28990000,N'Smartphone',65,7,4.85),
(N'iPhone 16 Pro Max',N'Apple',NULL,N'A18 Pro, 6.9" 120Hz, 5x',NULL,32990000,N'Smartphone',55,6,4.90),

(N'Galaxy S22',N'Samsung',NULL,N'SD 8 Gen1, 6.1" 120Hz',NULL,13990000,N'Smartphone',80,15,4.60),
(N'Galaxy S23',N'Samsung',NULL,N'SD 8 Gen2, 6.1" 120Hz',NULL,17990000,N'Smartphone',80,20,4.70),
(N'Galaxy S24',N'Samsung',NULL,N'Gen3/Exynos2400, 6.2" 120Hz',NULL,20990000,N'Smartphone',70,10,4.75),
(N'Galaxy S24+',N'Samsung',NULL,N'Gen3/Exynos2400, 6.7" 120Hz',NULL,24990000,N'Smartphone',60,8,4.78),
(N'Galaxy S24 Ultra',N'Samsung',NULL,N'Gen3, 12GB/512GB, S-Pen',NULL,30990000,N'Smartphone',50,9,4.85),
(N'Galaxy A55',N'Samsung',NULL,N'Exynos 1480, 6.6" 120Hz',NULL, 8990000,N'Smartphone',120,22,4.50),

(N'Xiaomi 13',N'Xiaomi',NULL,N'SD 8 Gen2, 6.36" 120Hz',NULL,12990000,N'Smartphone',70,14,4.6),
(N'Xiaomi 13 Pro',N'Xiaomi',NULL,N'SD 8 Gen2, 12/256',NULL,17990000,N'Smartphone',50,9,4.7),
(N'Xiaomi 14',N'Xiaomi',NULL,N'SD 8 Gen3, 12/256',NULL,17990000,N'Smartphone',60,10,4.75),
(N'Xiaomi 14 Ultra',N'Xiaomi',NULL,N'SD 8 Gen3, Leica',NULL,27990000,N'Smartphone',40,7,4.85),
(N'POCO F6',N'Xiaomi',NULL,N'8s Gen3, 12/256',NULL, 9990000,N'Smartphone',110,18,4.6),
(N'Redmi Note 13 Pro',N'Xiaomi',NULL,N'Dimensity 6080, 8/256',NULL, 7990000,N'Smartphone',150,30,4.5),

(N'OPPO Find X7',N'OPPO',NULL,N'Dimensity 9300, 12/256',NULL,20990000,N'Smartphone',45,8,4.7),
(N'OPPO Find N3 Flip',N'OPPO',NULL,N'Dimensity 9200, gập dọc',NULL,23990000,N'Smartphone',25,4,4.7),
(N'OPPO Reno12',N'OPPO',NULL,N'Dimensity 7300, 12/256',NULL,10990000,N'Smartphone',90,16,4.6),
(N'OPPO Reno11',N'OPPO',NULL,N'Dimensity 7050, 8/256',NULL, 8990000,N'Smartphone',120,22,4.5),
(N'OPPO A79',N'OPPO',NULL,N'Dimensity 6020, 8/128',NULL, 5990000,N'Smartphone',140,28,4.4),

(N'Honor Magic6',N'Honor',NULL,N'SD 8 Gen3, 12/256',NULL,16990000,N'Smartphone',60,11,4.6),
(N'Honor Magic6 Pro',N'Honor',NULL,N'SD 8 Gen3, 16/512',NULL,21990000,N'Smartphone',45,8,4.7),
(N'Honor 200 Pro',N'Honor',NULL,N'8s Gen3, 12/512',NULL,12990000,N'Smartphone',70,13,4.6),
(N'Honor 90',N'Honor',NULL,N'SD 7 Gen1, 12/256',NULL, 8990000,N'Smartphone',120,20,4.5),
(N'Honor X9b',N'Honor',NULL,N'SD 6 Gen1, 8/256',NULL, 7490000,N'Smartphone',120,24,4.4),

(N'vivo X100',N'vivo',NULL,N'Dimensity 9300, 12/256',NULL,16990000,N'Smartphone',60,12,4.6),
(N'vivo X100 Pro',N'vivo',NULL,N'Dimensity 9300, 16/512',NULL,20990000,N'Smartphone',45,8,4.7),
(N'vivo X90 Pro+',N'vivo',NULL,N'Dimensity 9200, 12/256',NULL,18990000,N'Smartphone',40,7,4.6),
(N'vivo V30',N'vivo',NULL,N'SD 7 Gen3, 12/256',NULL, 9990000,N'Smartphone',110,20,4.5),
(N'vivo Y36',N'vivo',NULL,N'SD 680, 8/128',NULL, 4990000,N'Smartphone',150,35,4.3),

(N'Realme GT 6',N'Realme',NULL,N'8s Gen3, 12/256',NULL,11990000,N'Smartphone',90,18,4.6),
(N'Realme 12 Pro+',N'Realme',NULL,N'7s Gen2, 8/256',NULL, 9990000,N'Smartphone',100,20,4.5),
(N'Realme 11 Pro+',N'Realme',NULL,N'Dimensity 7050, 8/256',NULL, 8990000,N'Smartphone',120,24,4.5),
(N'Realme C55',N'Realme',NULL,N'Helio G88, 6/128',NULL, 3990000,N'Smartphone',180,40,4.2),
(N'Realme Narzo 70',N'Realme',NULL,N'Dimensity 6100+, 6/128',NULL, 4490000,N'Smartphone',160,32,4.3),

(N'OnePlus 12',N'OnePlus',NULL,N'SD 8 Gen3, 16/512',NULL,18990000,N'Smartphone',60,12,4.7),
(N'OnePlus 12R',N'OnePlus',NULL,N'SD 8 Gen2, 16/256',NULL,12990000,N'Smartphone',70,14,4.6),
(N'OnePlus 11',N'OnePlus',NULL,N'SD 8 Gen2, 12/256',NULL,13990000,N'Smartphone',60,12,4.6),
(N'OnePlus Nord 3',N'OnePlus',NULL,N'Dimensity 9000, 12/256',NULL, 8990000,N'Smartphone',110,22,4.5),
(N'OnePlus Nord CE4',N'OnePlus',NULL,N'SD 7 Gen2, 8/256',NULL, 7490000,N'Smartphone',120,24,4.4);

/* ---- Laptops: Apple/ASUS/Lenovo/Acer/Dell/HP/MSI/Razer/Alienware ---- */
INSERT INTO @Seed VALUES
(N'MacBook Air 13 M2 (8/256)', N'Apple',NULL,N'M2, 8/256, 13.6" Retina, 1.24kg',NULL,24990000,N'Laptop',40,5,4.8),
(N'MacBook Air 15 M3 (8/256)', N'Apple',NULL,N'M3, 8/256, 15.3"',NULL,29990000,N'Laptop',35,4,4.8),
(N'MacBook Pro 14 M3 Pro (16/512)',N'Apple',NULL,N'M3 Pro, 16/512, 14.2" 120Hz',NULL,49990000,N'Laptop',20,3,4.9),
(N'MacBook Pro 16 M3 Max (32/1TB)',N'Apple',NULL,N'M3 Max, 32/1TB, 16.2" 120Hz',NULL,79990000,N'Laptop',10,1,5.0),

(N'ASUS ROG Strix G16 (i9/16/1TB/RTX4060)',N'ASUS',NULL,N'ROG Strix, i9, 16/1TB, RTX4060, 16" 165Hz',NULL,42990000,N'Laptop',15,2,4.8),
(N'ASUS ROG Zephyrus G14 (R9/16/1TB/RTX4060)',N'ASUS',NULL,N'ROG Zephyrus, R9, RTX4060, 14" 165Hz',NULL,44990000,N'Laptop',12,2,4.9),
(N'ASUS TUF Gaming F15 (i7/16/512/RTX4050)',N'ASUS',NULL,N'TUF, i7, RTX4050, 15.6" 144Hz',NULL,27990000,N'Laptop',25,4,4.7),
(N'ASUS Zenbook 14 OLED (i7/16/512)',N'ASUS',NULL,N'Zenbook, i7, 16/512, OLED 2.8K',NULL,25990000,N'Laptop',30,5,4.8),
(N'ASUS Vivobook 15 (i5/8/512)',N'ASUS',NULL,N'Vivobook, i5, 8/512, 15.6" FHD',NULL,15990000,N'Laptop',40,6,4.6),

(N'Lenovo ThinkPad X1 Carbon Gen 11 (i7/16/512)',N'Lenovo',NULL,N'ThinkPad X1, i7, 16/512, 14" 2.8K',NULL,38990000,N'Laptop',20,3,4.9),
(N'Lenovo ThinkPad T14 (i5/16/512)',N'Lenovo',NULL,N'ThinkPad T14, i5, 16/512',NULL,26990000,N'Laptop',25,3,4.7),
(N'Lenovo Legion 5 Pro 16 (R7/16/1TB/RTX4070)',N'Lenovo',NULL,N'Legion 5 Pro, R7, 1TB, RTX4070',NULL,47990000,N'Laptop',12,2,4.9),
(N'Lenovo Legion Slim 7 (R7/16/1TB/RTX4060)',N'Lenovo',NULL,N'Legion Slim, R7, RTX4060',NULL,42990000,N'Laptop',12,2,4.8),
(N'Lenovo IdeaPad 5 14 (R5/16/512)',N'Lenovo',NULL,N'IdeaPad 5, R5, 16/512, 14" FHD',NULL,16990000,N'Laptop',35,5,4.6),
(N'Lenovo LOQ 15 (i7/16/512/RTX4050)',N'Lenovo',NULL,N'LOQ, i7, RTX4050',NULL,25990000,N'Laptop',20,3,4.7),

(N'Acer Nitro 5 2023 (i7/16/512/RTX4050)',N'Acer',NULL,N'Nitro 5, i7, RTX4050',NULL,28990000,N'Laptop',20,3,4.7),
(N'Acer Predator Helios 16 (i9/32/1TB/RTX4080)',N'Acer',NULL,N'Predator Helios 16, i9, RTX4080, 240Hz',NULL,62990000,N'Laptop',8,1,4.9),
(N'Acer Swift 3 (i5/8/512)',N'Acer',NULL,N'Swift 3, i5, 8/512, 14" FHD',NULL,15990000,N'Laptop',40,6,4.6),
(N'Acer Swift X 14 (i7/16/1TB/RTX4050)',N'Acer',NULL,N'Swift X 14, i7, RTX4050',NULL,34990000,N'Laptop',12,2,4.8),
(N'Acer Aspire 7 (R5/8/512/GTX1650)',N'Acer',NULL,N'Aspire 7, R5, GTX1650',NULL,14990000,N'Laptop',30,5,4.5),

(N'Dell XPS 13 (i7/16/512 OLED)',N'Dell',NULL,N'XPS 13, i7, OLED',NULL,38990000,N'Laptop',15,2,4.9),
(N'Dell XPS 15 (i7/16/1TB RTX4050)',N'Dell',NULL,N'XPS 15, i7, RTX4050',NULL,55990000,N'Laptop',10,1,4.9),
(N'Dell Inspiron 15 (i5/16/512)',N'Dell',NULL,N'Inspiron 15, i5, 16/512',NULL,17990000,N'Laptop',35,6,4.6),
(N'Dell G15 (i7/16/512/RTX4060)',N'Dell',NULL,N'G15, i7, RTX4060',NULL,30990000,N'Laptop',18,3,4.7),
(N'Alienware m16 (i9/32/2TB/RTX4080)',N'Alienware',NULL,N'm16, i9, 2TB, RTX4080, 240Hz',NULL,79990000,N'Laptop',6,1,4.9),

(N'HP Spectre x360 14 (i7/16/1TB OLED)',N'HP',NULL,N'Spectre x360 14, i7, OLED 3K, 2-in-1',NULL,38990000,N'Laptop',12,2,4.9),
(N'HP Envy x360 15 (i7/16/512)',N'HP',NULL,N'Envy x360 15, i7, 2-in-1',NULL,27990000,N'Laptop',18,3,4.7),
(N'HP Omen 16 (i7/16/1TB/RTX4070)',N'HP',NULL,N'Omen 16, i7, RTX4070',NULL,46990000,N'Laptop',10,2,4.8),
(N'HP Victus 16 (R7/16/512/RTX4050)',N'HP',NULL,N'Victus 16, R7, RTX4050',NULL,25990000,N'Laptop',22,4,4.6),
(N'HP Pavilion 15 (i5/8/512)',N'HP',NULL,N'Pavilion 15, i5, 8/512',NULL,14990000,N'Laptop',40,6,4.5),

(N'MSI Katana 15 (i7/16/512/RTX4060)',N'MSI',NULL,N'Katana 15, i7, RTX4060',NULL,28990000,N'Laptop',20,3,4.7),
(N'MSI Stealth 14 Studio (i7/16/1TB/RTX4060)',N'MSI',NULL,N'Stealth 14, i7, RTX4060',NULL,45990000,N'Laptop',10,2,4.8),
(N'MSI Modern 14 (i5/16/512)',N'MSI',NULL,N'Modern 14, i5, 16/512',NULL,15990000,N'Laptop',30,5,4.6),
(N'MSI Prestige 14 (i7/32/1TB)',N'MSI',NULL,N'Prestige 14, i7, 32/1TB',NULL,35990000,N'Laptop',12,2,4.7),
(N'MSI Delta 15 (R9/32/1TB/RX6700M)',N'MSI',NULL,N'Delta 15, R9, RX6700M',NULL,32990000,N'Laptop',10,1,4.6),

(N'Razer Blade 15 (i7/16/1TB/RTX3070)',N'Razer',NULL,N'Blade 15, i7, RTX3070, 240Hz',NULL,56990000,N'Laptop',6,1,4.8),
(N'Razer Blade 16 (i9/32/2TB/RTX4080)',N'Razer',NULL,N'Blade 16, i9, 2TB, RTX4080, 240Hz',NULL,89990000,N'Laptop',4,0,4.9);
GO

;WITH C AS (SELECT id,name FROM dbo.Categories),
SRC AS (
  SELECT s.*, c.id AS category_id
  FROM @Seed s
  JOIN C c ON c.name = s.category
)
MERGE dbo.Products AS T
USING SRC AS S
ON (T.name=S.name AND T.brand=S.brand AND T.category_id=S.category_id)
WHEN MATCHED THEN
  UPDATE SET T.price=S.price, T.stock=S.stock, T.sold=S.sold, T.rating=S.rating,
             T.description=S.description, T.image_url=COALESCE(T.image_url,S.image_url),
             T.updated_at=SYSDATETIME()
WHEN NOT MATCHED THEN
  INSERT(name,brand,color,description,image_url,price,category_id,stock,sold,rating)
  VALUES(S.name,S.brand,S.color,S.description,S.image_url,S.price,S.category_id,S.stock,S.sold,S.rating);
GO

/* 9) Auto set image_url theo quy ước, KHÔNG ghi đè nếu đã có */
DECLARE @ROOT NVARCHAR(200) = N'assets/img/products';
DECLARE @EXT  NVARCHAR(10)  = N'.jpg';
DECLARE @OVERWRITE BIT = 0; -- 0 = chỉ set khi NULL/rỗng

;WITH S AS (
  SELECT
      p.id,
      CASE c.name WHEN N'Laptop' THEN 'laptops'
                  WHEN N'Smartphone' THEN 'phones'
                  ELSE 'others' END AS cat_folder,
      LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(p.brand,' ','-'),'+','plus'),'&','-and-'),'(',''),')',''),'''',''),'.','')) AS brand_slug,
      LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(p.name ,' ','-'),'+','plus'),'&','-and-'),'(',''),')',''),'''',''),'.','')) AS name_slug
  FROM dbo.Products p
  JOIN dbo.Categories c ON c.id = p.category_id
)
UPDATE p
SET image_url = CONCAT(@ROOT,'/',s.cat_folder,'/',s.brand_slug,'/',s.name_slug,@EXT)
FROM dbo.Products p
JOIN S s ON s.id = p.id
WHERE (@OVERWRITE = 1) OR p.image_url IS NULL OR LTRIM(RTRIM(p.image_url)) = '';
GO

/* 10) Views (CREATE OR ALTER cho an toàn) */
CREATE OR ALTER VIEW dbo.v_RevenueByDay AS
SELECT CAST(created_at AS date) AS d,
       SUM(total_amount)        AS revenue
FROM dbo.Orders
WHERE status IN (N'PAID',N'SHIPPED',N'COMPLETED')
GROUP BY CAST(created_at AS date);
GO

CREATE OR ALTER VIEW dbo.v_BestSellers AS
SELECT TOP (10) p.id, p.name, SUM(oi.quantity) AS qty
FROM dbo.OrderItems oi
JOIN dbo.Products    p ON p.id = oi.product_id
GROUP BY p.id, p.name
ORDER BY qty DESC;
GO

CREATE OR ALTER VIEW dbo.v_NewCustomersByDay AS
SELECT CAST(created_at AS date) AS d,
       COUNT(*)                 AS new_customers
FROM dbo.Users
GROUP BY CAST(created_at AS date);
GO

CREATE OR ALTER VIEW dbo.v_NewCustomersByMonth AS
SELECT DATEFROMPARTS(YEAR(created_at), MONTH(created_at), 1) AS ym,
       COUNT(*)                                            AS new_customers
FROM dbo.Users
GROUP BY DATEFROMPARTS(YEAR(created_at), MONTH(created_at), 1);
GO

CREATE OR ALTER VIEW dbo.v_ProductCountByCategory AS
SELECT c.id, c.name, COUNT(p.id) AS product_count
FROM dbo.Categories c
LEFT JOIN dbo.Products p ON p.category_id = c.id
GROUP BY c.id, c.name;
GO



--Laptop

SET NOCOUNT ON;

IF DB_ID('SmartShop') IS NULL
BEGIN
    RAISERROR('Database SmartShop not found.', 16, 1);
    RETURN;
END
GO

USE SmartShop;
GO

DECLARE @ROOT NVARCHAR(200) = N'assets/img/products';
DECLARE @EXT  NVARCHAR(10)  = N'.jpg';
DECLARE @OVERWRITE BIT = 1; -- 1 = update all laptops to new pattern; 0 = only fill NULL/empty

;WITH L AS (
    SELECT
        p.id,
        -- strip trailing specs in parentheses from the product name
        basenm = LTRIM(RTRIM(CASE WHEN CHARINDEX('(', p.name) > 0
                                  THEN LEFT(p.name, CHARINDEX('(', p.name) - 1)
                                  ELSE p.name END)),
        p.brand
    FROM dbo.Products p
    JOIN dbo.Categories c ON c.id = p.category_id
    WHERE c.name = N'Laptop'
),
S AS (
    SELECT
        id,
        -- brand slug
        LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(brand,' ','-'),'+','plus'),'&','-and-'),'(',''),')',''),'''',''),'.','')) AS brand_slug,
        -- base name slug without parentheses content
        LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(basenm,' ','-'),'+','plus'),'&','-and-'),'(',''),')',''),'''',''),'.',''))   AS name_slug
    FROM L
)
UPDATE p
SET image_url = CONCAT(@ROOT, '/laptops/', s.brand_slug, '/', s.name_slug, @EXT)
FROM dbo.Products p
JOIN S s ON s.id = p.id
WHERE @OVERWRITE = 1
   OR p.image_url IS NULL
   OR LTRIM(RTRIM(p.image_url)) = '';
GO

SELECT TOP (100) id, brand, name, image_url FROM dbo.Products ORDER BY updated_at DESC, id DESC;

