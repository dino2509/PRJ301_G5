
/* =======================================================================
   SmartShop — MERGED UPDATE (idempotent, data-safe)
   Purpose:
   - Harmonize base schema (SmartShop.sql) and update add-ons (SmartShop_update.sql).
   - Fix errors: FK_UserRoles_Roles, missing UserRoles, slug columns, vBestSellingProducts (qty), etc.
   - Preserve all existing data. No DROP. Only CREATE/ALTER with guards.
   Target: SQL Server (SSMS 19+)
   ======================================================================= */
SET NOCOUNT ON;

IF DB_ID('SmartShop') IS NULL
BEGIN
  RAISERROR('Database SmartShop not found.', 16, 1);
  RETURN;
END
GO

USE SmartShop;
GO

/* ------------------------------------------------------------
   1) Roles: add surrogate key id and unique index if missing
   ------------------------------------------------------------ */
IF OBJECT_ID('dbo.Roles','U') IS NULL
BEGIN
  CREATE TABLE dbo.Roles (
    name NVARCHAR(50) NOT NULL PRIMARY KEY,
    description NVARCHAR(200) NULL
  );
END
GO

IF COL_LENGTH('dbo.Roles','id') IS NULL
  ALTER TABLE dbo.Roles ADD id INT IDENTITY(1,1) NOT NULL;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_Roles_id' AND object_id = OBJECT_ID('dbo.Roles'))
  CREATE UNIQUE INDEX UX_Roles_id ON dbo.Roles(id);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_Roles_name' AND object_id = OBJECT_ID('dbo.Roles'))
BEGIN
  IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE parent_object_id = OBJECT_ID('dbo.Roles') AND type = 'PK')
    ALTER TABLE dbo.Roles ADD CONSTRAINT PK_Roles PRIMARY KEY(name);
END
GO

/* Seed basic roles */
IF NOT EXISTS (SELECT 1 FROM dbo.Roles WHERE name='ADMIN')    INSERT INTO dbo.Roles(name,description) VALUES('ADMIN',N'Quản trị');
IF NOT EXISTS (SELECT 1 FROM dbo.Roles WHERE name='CUSTOMER') INSERT INTO dbo.Roles(name,description) VALUES('CUSTOMER',N'Người dùng');
GO

/* ------------------------------------------------------------
   2) Users: ensure table exists and baseline columns
   ------------------------------------------------------------ */
IF OBJECT_ID('dbo.Users','U') IS NULL
BEGIN
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
    created_at    DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME()
  );
END
GO

/* ------------------------------------------------------------
   3) UserRoles: create junction and backfill from Users.role
   ------------------------------------------------------------ */
IF OBJECT_ID('dbo.UserRoles','U') IS NULL
BEGIN
  CREATE TABLE dbo.UserRoles(
    user_id INT NOT NULL,
    role_id INT NOT NULL,
    CONSTRAINT PK_UserRoles PRIMARY KEY(user_id, role_id),
    CONSTRAINT FK_UserRoles_Users FOREIGN KEY(user_id) REFERENCES dbo.Users(id) ON DELETE CASCADE,
    CONSTRAINT FK_UserRoles_Roles FOREIGN KEY(role_id) REFERENCES dbo.Roles(id) ON DELETE CASCADE
  );
END
GO

/* Backfill current assignments */
;WITH UR AS (
  SELECT u.id AS user_id, r.id AS role_id
  FROM dbo.Users u
  JOIN dbo.Roles r ON r.name = u.role
)
INSERT INTO dbo.UserRoles(user_id, role_id)
SELECT ur.user_id, ur.role_id
FROM UR ur
LEFT JOIN dbo.UserRoles x ON x.user_id = ur.user_id AND x.role_id = ur.role_id
WHERE x.user_id IS NULL;
GO

/* ------------------------------------------------------------
   4) Categories: add slug + created_at when missing, fill data
   ------------------------------------------------------------ */
IF OBJECT_ID('dbo.Categories','U') IS NULL
BEGIN
  CREATE TABLE dbo.Categories(
    id   INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(100) NOT NULL
  );
END
GO

IF COL_LENGTH('dbo.Categories','slug') IS NULL
  ALTER TABLE dbo.Categories ADD slug NVARCHAR(120) NULL;
GO

IF COL_LENGTH('dbo.Categories','created_at') IS NULL
  ALTER TABLE dbo.Categories ADD created_at DATETIME2 NOT NULL CONSTRAINT DF_Categories_Created DEFAULT SYSUTCDATETIME();
GO

/* Fill slug for NULLs using simple slugify */
;WITH S AS (
  SELECT id, name,
         LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(LTRIM(RTRIM(name)),' ','-'),'+','plus'),'&','-and-'),'(',''),')',''),'''',''),'.','')) AS slugified
  FROM dbo.Categories
)
UPDATE c
   SET slug = s.slugified
FROM dbo.Categories c
JOIN S s ON s.id = c.id
WHERE c.slug IS NULL OR LTRIM(RTRIM(c.slug))='';
GO

/* Ensure uniqueness */
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name='UX_Categories_Slug' AND object_id=OBJECT_ID('dbo.Categories'))
  CREATE UNIQUE INDEX UX_Categories_Slug ON dbo.Categories(slug);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name='UX_Categories_Name' AND object_id=OBJECT_ID('dbo.Categories'))
  CREATE UNIQUE INDEX UX_Categories_Name ON dbo.Categories(name);
GO

/* ------------------------------------------------------------
   5) Products: ensure baseline + helpful indexes
   ------------------------------------------------------------ */
IF OBJECT_ID('dbo.Products','U') IS NULL
BEGIN
  CREATE TABLE dbo.Products(
    id            INT IDENTITY(1,1) PRIMARY KEY,
    name          NVARCHAR(150) NOT NULL,
    brand         NVARCHAR(50)  NULL,
    color         NVARCHAR(30)  NULL,
    description   NVARCHAR(MAX) NULL,
    image_url     NVARCHAR(400) NULL,
    price         DECIMAL(18,2) NOT NULL,
    category_id   INT           NULL,
    stock         INT           NOT NULL DEFAULT 0,
    sold          INT           NOT NULL DEFAULT 0,
    rating        DECIMAL(3,2)  NULL,
    created_at    DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at    DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME(),
    is_active     BIT           NOT NULL DEFAULT 1
  );
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name='FK_Products_Categories')
  ALTER TABLE dbo.Products ADD CONSTRAINT FK_Products_Categories FOREIGN KEY(category_id) REFERENCES dbo.Categories(id);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name='IX_Products_Name' AND object_id=OBJECT_ID('dbo.Products'))
  CREATE INDEX IX_Products_Name ON dbo.Products(name);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name='IX_Products_Category' AND object_id=OBJECT_ID('dbo.Products'))
  CREATE INDEX IX_Products_Category ON dbo.Products(category_id);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name='IX_Products_Price' AND object_id=OBJECT_ID('dbo.Products'))
  CREATE INDEX IX_Products_Price ON dbo.Products(price);
GO

/* ------------------------------------------------------------
   6) Orders & OrderItems: add computed column qty for compat
   ------------------------------------------------------------ */
IF OBJECT_ID('dbo.Orders','U') IS NULL
BEGIN
  CREATE TABLE dbo.Orders(
    id           INT IDENTITY(1,1) PRIMARY KEY,
    user_id      INT NOT NULL,
    total_amount DECIMAL(18,2) NOT NULL,
    status       NVARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at   DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME()
  );
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name='FK_Orders_Users')
  ALTER TABLE dbo.Orders ADD CONSTRAINT FK_Orders_Users FOREIGN KEY(user_id) REFERENCES dbo.Users(id);
GO

IF OBJECT_ID('dbo.OrderItems','U') IS NULL
BEGIN
  CREATE TABLE dbo.OrderItems(
    id         INT IDENTITY(1,1) PRIMARY KEY,
    order_id   INT NOT NULL,
    product_id INT NOT NULL,
    quantity   INT NOT NULL CHECK (quantity > 0),
    unit_price DECIMAL(18,2) NOT NULL
  );
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name='FK_OrderItems_Orders')
  ALTER TABLE dbo.OrderItems ADD CONSTRAINT FK_OrderItems_Orders FOREIGN KEY(order_id) REFERENCES dbo.Orders(id) ON DELETE CASCADE;
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name='FK_OrderItems_Products')
  ALTER TABLE dbo.OrderItems ADD CONSTRAINT FK_OrderItems_Products FOREIGN KEY(product_id) REFERENCES dbo.Products(id);
GO

/* Add computed alias column qty -> quantity if missing */
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id=OBJECT_ID('dbo.OrderItems') AND name='qty')
  ALTER TABLE dbo.OrderItems ADD qty AS (quantity);
GO

/* ------------------------------------------------------------
   7) Reviews / ProductReviews bridge
   ------------------------------------------------------------ */
IF OBJECT_ID('dbo.Reviews','U') IS NULL AND OBJECT_ID('dbo.ProductReviews','U') IS NULL
BEGIN
  -- If neither exists, create ProductReviews as canonical table
  CREATE TABLE dbo.ProductReviews(
    id         INT IDENTITY(1,1) PRIMARY KEY,
    product_id INT NOT NULL,
    user_id    INT NOT NULL,
    rating     INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment    NVARCHAR(1000) NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
  );
END
ELSE
BEGIN
  -- If Reviews exists but ProductReviews does not, create a view for compatibility
  IF OBJECT_ID('dbo.ProductReviews','U') IS NULL AND OBJECT_ID('dbo.ProductReviews','V') IS NULL AND OBJECT_ID('dbo.Reviews','U') IS NOT NULL
    EXEC('CREATE VIEW dbo.ProductReviews AS SELECT id, product_id, user_id, rating, comment, created_at FROM dbo.Reviews');
END
GO

/* ------------------------------------------------------------
   8) Carts & CartItems from update file (create if missing)
   ------------------------------------------------------------ */
IF OBJECT_ID('dbo.Carts','U') IS NULL
BEGIN
  CREATE TABLE dbo.Carts(
    id        INT IDENTITY PRIMARY KEY,
    user_id   INT NOT NULL,
    status    NVARCHAR(20) NOT NULL DEFAULT 'OPEN', -- OPEN, ORDERED, ABANDONED
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_Carts_Users FOREIGN KEY(user_id) REFERENCES dbo.Users(id) ON DELETE CASCADE
  );
END
GO

IF OBJECT_ID('dbo.CartItems','U') IS NULL
BEGIN
  CREATE TABLE dbo.CartItems(
    id         INT IDENTITY PRIMARY KEY,
    cart_id    INT NOT NULL,
    product_id INT NOT NULL,
    quantity   INT NOT NULL CHECK (quantity > 0),
    unit_price DECIMAL(18,2) NOT NULL,
    CONSTRAINT FK_CartItems_Carts    FOREIGN KEY(cart_id)    REFERENCES dbo.Carts(id) ON DELETE CASCADE,
    CONSTRAINT FK_CartItems_Products FOREIGN KEY(product_id) REFERENCES dbo.Products(id)
  );
END
GO

/* Ensure each user has an OPEN cart */
MERGE dbo.Carts AS tgt
USING (SELECT id FROM dbo.Users) AS src(user_id)
ON tgt.user_id = src.user_id AND tgt.status = 'OPEN'
WHEN NOT MATCHED THEN INSERT(user_id) VALUES(src.user_id);
GO

/* ------------------------------------------------------------
   9) Views (CREATE OR ALTER for stability)
   ------------------------------------------------------------ */
CREATE OR ALTER VIEW dbo.vBestSellingProducts AS
SELECT TOP (10)
       oi.product_id,
       SUM(oi.quantity) AS qty
FROM dbo.OrderItems oi
JOIN dbo.Orders     o ON o.id = oi.order_id
WHERE o.status IN (N'PAID',N'SHIPPED',N'COMPLETED')  -- align with base schema
GROUP BY oi.product_id
ORDER BY qty DESC;
GO

CREATE OR ALTER VIEW dbo.vTopRatedProducts AS
SELECT pr.product_id,
       AVG(CAST(pr.rating AS FLOAT)) AS avg_rating,
       COUNT(*) AS reviews
FROM dbo.ProductReviews pr
GROUP BY pr.product_id;
GO

/* Optional reporting views preserved */
CREATE OR ALTER VIEW dbo.v_RevenueByDay AS
SELECT CAST(created_at AS date) AS d,
       SUM(total_amount)        AS revenue
FROM dbo.Orders
WHERE status IN (N'PAID',N'SHIPPED',N'COMPLETED')
GROUP BY CAST(created_at AS date);
GO

/* ------------------------------------------------------------
   10) Final checks (optional)
   ------------------------------------------------------------ */
-- SELECT r.id, r.name FROM dbo.Roles r ORDER BY r.id;
-- SELECT TOP 20 * FROM dbo.UserRoles ORDER BY user_id;
-- SELECT id, name, slug FROM dbo.Categories;
-- SELECT TOP 10 * FROM dbo.vBestSellingProducts;



-- Chạy trên SQL Server (SSMS)
SET ANSI_NULLS ON; SET QUOTED_IDENTIFIER ON;
GO

-- is_active
IF COL_LENGTH('dbo.Products','is_active') IS NULL
BEGIN
    ALTER TABLE dbo.Products
      ADD is_active BIT NOT NULL
          CONSTRAINT DF_Products_is_active DEFAULT(1) WITH VALUES;
END
GO

-- updated_at
IF COL_LENGTH('dbo.Products','updated_at') IS NULL
BEGIN
    ALTER TABLE dbo.Products
      ADD updated_at DATETIME2 NOT NULL
          CONSTRAINT DF_Products_updated DEFAULT SYSUTCDATETIME() WITH VALUES;
END
GO

-- created_at
IF COL_LENGTH('dbo.Products','created_at') IS NULL
BEGIN
    ALTER TABLE dbo.Products
      ADD created_at DATETIME2 NOT NULL
          CONSTRAINT DF_Products_created DEFAULT SYSUTCDATETIME() WITH VALUES;
END
GO

-- sold
IF COL_LENGTH('dbo.Products','sold') IS NULL
BEGIN
    ALTER TABLE dbo.Products
      ADD sold INT NOT NULL
          CONSTRAINT DF_Products_sold DEFAULT(0) WITH VALUES;
END
GO

-- rating
IF COL_LENGTH('dbo.Products','rating') IS NULL
BEGIN
    ALTER TABLE dbo.Products
      ADD rating FLOAT NULL;
END
GO

-- image_url (nếu schema cũ chưa có)
IF COL_LENGTH('dbo.Products','image_url') IS NULL
BEGIN
    ALTER TABLE dbo.Products
      ADD image_url NVARCHAR(400) NULL;
END
GO

-- Index hỗ trợ tìm kiếm/lọc
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name='IX_Products_Name' AND object_id=OBJECT_ID('dbo.Products'))
    CREATE INDEX IX_Products_Name ON dbo.Products(name);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name='IX_Products_Category' AND object_id=OBJECT_ID('dbo.Products'))
    CREATE INDEX IX_Products_Category ON dbo.Products(category_id);
GO


-- Đặt lại mật khẩu admin -> admin123
DECLARE @salt VARBINARY(16) = CRYPT_GEN_RANDOM(16);
UPDATE dbo.Users
SET password_salt = @salt,
    password_hash = HASHBYTES('SHA2_256', @salt + CONVERT(VARBINARY(200), N'admin123'))
WHERE username = 'admin';


DECLARE @hasStatus bit = CASE WHEN COL_LENGTH('dbo.Users','status') IS NULL THEN 0 ELSE 1 END;

IF EXISTS (SELECT 1 FROM dbo.Users WHERE username = 'admin')
BEGIN
    DECLARE @sqlU nvarchar(max) =
        N'UPDATE dbo.Users
           SET password_hash=@p, email=@e, role=@r, active=1' +
           CASE WHEN @hasStatus=1 THEN N', status=''ACTIVE''' ELSE N'' END +
         N' WHERE username=''admin'';';
    EXEC sp_executesql @sqlU,
        N'@p nvarchar(255), @e nvarchar(255), @r nvarchar(50)',
        @p = N'admin123',
        @e = N'smartshop.g5.prj301@gmail.com',
        @r = N'ADMIN';
END
ELSE
BEGIN
    DECLARE @sqlI nvarchar(max) =
        N'INSERT INTO dbo.Users (username,password_hash,email,full_name,phone,role,active' +
           CASE WHEN @hasStatus=1 THEN N',status' ELSE N'' END + N')
          VALUES (''admin'', @p, @e, ''Administrator'', NULL, @r, 1' +
           CASE WHEN @hasStatus=1 THEN N',''ACTIVE''' ELSE N'' END + N');';
    EXEC sp_executesql @sqlI,
        N'@p nvarchar(255), @e nvarchar(255), @r nvarchar(50)',
        @p = N'admin123',
        @e = N'smartshop.g5.prj301@gmail.com',
        @r = N'ADMIN';
END
