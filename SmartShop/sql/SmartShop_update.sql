-- Target: SQL Server
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

/* Ensure Roles table and column id exist */
IF OBJECT_ID('dbo.Roles','U') IS NULL
BEGIN
    CREATE TABLE dbo.Roles (
        id   INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Roles PRIMARY KEY,
        name NVARCHAR(50) NOT NULL UNIQUE
    );
END
ELSE
BEGIN
    IF COL_LENGTH('dbo.Roles','id') IS NULL
        ALTER TABLE dbo.Roles ADD id INT IDENTITY(1,1) NOT NULL;

    -- Ensure id is UNIQUE (FK can reference UNIQUE or PK)
    IF NOT EXISTS (
        SELECT 1
        FROM sys.indexes i
        WHERE i.object_id = OBJECT_ID('dbo.Roles')
          AND i.is_primary_key = 1
    )
    AND NOT EXISTS (
        SELECT 1
        FROM sys.indexes i
        WHERE i.object_id = OBJECT_ID('dbo.Roles')
          AND i.name = 'UQ_Roles_id'
    )
        ALTER TABLE dbo.Roles ADD CONSTRAINT UQ_Roles_id UNIQUE(id);

    -- Ensure name has UNIQUE
    IF NOT EXISTS (
        SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.Roles') AND name = 'UQ_Roles_name'
    )
    BEGIN
        -- If name already unique, this is a no-op; else create a unique index
        CREATE UNIQUE INDEX UQ_Roles_name ON dbo.Roles(name);
    END
END
GO

/* Users */
IF OBJECT_ID('dbo.Users','U') IS NULL
BEGIN
    CREATE TABLE dbo.Users (
        id INT IDENTITY PRIMARY KEY,
        username NVARCHAR(50) NOT NULL UNIQUE,
        email NVARCHAR(255) NULL UNIQUE,
        phone NVARCHAR(20) NULL,
        full_name NVARCHAR(100) NULL,
        password_salt VARBINARY(16) NOT NULL,
        password_hash VARBINARY(32) NOT NULL,
        status NVARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
        created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
    );
END
ELSE
BEGIN
    IF COL_LENGTH('dbo.Users','password_salt') IS NULL
        ALTER TABLE dbo.Users ADD password_salt VARBINARY(16) NULL;
    IF COL_LENGTH('dbo.Users','password_hash') IS NULL
        ALTER TABLE dbo.Users ADD password_hash VARBINARY(32) NULL;
    IF COL_LENGTH('dbo.Users','status') IS NULL
        ALTER TABLE dbo.Users ADD status NVARCHAR(20) NOT NULL CONSTRAINT DF_Users_status DEFAULT 'ACTIVE';
    IF COL_LENGTH('dbo.Users','created_at') IS NULL
        ALTER TABLE dbo.Users ADD created_at DATETIME2 NOT NULL CONSTRAINT DF_Users_created DEFAULT SYSUTCDATETIME();
END
GO

/* UserRoles */
IF OBJECT_ID('dbo.UserRoles','U') IS NULL
BEGIN
    CREATE TABLE dbo.UserRoles (
        user_id INT NOT NULL,
        role_id INT NOT NULL,
        CONSTRAINT PK_UserRoles PRIMARY KEY(user_id, role_id),
        CONSTRAINT FK_UserRoles_Users FOREIGN KEY(user_id) REFERENCES dbo.Users(id) ON DELETE CASCADE,
        CONSTRAINT FK_UserRoles_Roles FOREIGN KEY(role_id) REFERENCES dbo.Roles(id) ON DELETE CASCADE
    );
END
GO

/* Categories */
IF OBJECT_ID('dbo.Categories','U') IS NULL
BEGIN
    CREATE TABLE dbo.Categories (
        id INT IDENTITY PRIMARY KEY,
        name NVARCHAR(100) NOT NULL UNIQUE,
        slug NVARCHAR(120) NOT NULL UNIQUE,
        created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
    );
END
GO

/* Products */
IF OBJECT_ID('dbo.Products','U') IS NULL
BEGIN
    CREATE TABLE dbo.Products (
        id INT IDENTITY PRIMARY KEY,
        category_id INT NULL,
        name NVARCHAR(200) NOT NULL,
        brand NVARCHAR(100) NULL,
        color NVARCHAR(50) NULL,
        description NVARCHAR(MAX) NULL,
        image_url NVARCHAR(400) NULL,
        price DECIMAL(18,2) NOT NULL,
        stock INT NOT NULL DEFAULT 0,
        sold INT NOT NULL DEFAULT 0,
        rating FLOAT NULL,
        created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
        is_active BIT NOT NULL DEFAULT 1,
        CONSTRAINT FK_Products_Categories FOREIGN KEY(category_id) REFERENCES dbo.Categories(id)
    );
    CREATE INDEX IX_Products_Name ON dbo.Products(name);
    CREATE INDEX IX_Products_Category ON dbo.Products(category_id);
END
GO

/* ProductReviews */
IF OBJECT_ID('dbo.ProductReviews','U') IS NULL
BEGIN
    CREATE TABLE dbo.ProductReviews (
        id INT IDENTITY PRIMARY KEY,
        product_id INT NOT NULL,
        user_id INT NOT NULL,
        rating TINYINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
        comment NVARCHAR(1000) NULL,
        created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
        CONSTRAINT FK_Reviews_Product FOREIGN KEY(product_id) REFERENCES dbo.Products(id) ON DELETE CASCADE,
        CONSTRAINT FK_Reviews_User FOREIGN KEY(user_id) REFERENCES dbo.Users(id) ON DELETE CASCADE,
        CONSTRAINT UQ_Review_Product_User UNIQUE(product_id, user_id)
    );
END
GO

/* Carts */
IF OBJECT_ID('dbo.Carts','U') IS NULL
BEGIN
    CREATE TABLE dbo.Carts (
        id INT IDENTITY PRIMARY KEY,
        user_id INT NOT NULL,
        status NVARCHAR(20) NOT NULL DEFAULT 'OPEN', -- OPEN, ORDERED
        created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
        CONSTRAINT FK_Carts_User FOREIGN KEY(user_id) REFERENCES dbo.Users(id) ON DELETE CASCADE
    );
    CREATE INDEX IX_Carts_User_Status ON dbo.Carts(user_id, status);
END
GO

/* CartItems */
IF OBJECT_ID('dbo.CartItems','U') IS NULL
BEGIN
    CREATE TABLE dbo.CartItems (
        id INT IDENTITY PRIMARY KEY,
        cart_id INT NOT NULL,
        product_id INT NOT NULL,
        qty INT NOT NULL CHECK (qty > 0),
        unit_price DECIMAL(18,2) NOT NULL,
        CONSTRAINT FK_CartItems_Cart FOREIGN KEY(cart_id) REFERENCES dbo.Carts(id) ON DELETE CASCADE,
        CONSTRAINT FK_CartItems_Product FOREIGN KEY(product_id) REFERENCES dbo.Products(id),
        CONSTRAINT UQ_Cart_Product UNIQUE(cart_id, product_id)
    );
END
GO

/* Orders */
IF OBJECT_ID('dbo.Orders','U') IS NULL
BEGIN
    CREATE TABLE dbo.Orders (
        id INT IDENTITY PRIMARY KEY,
        user_id INT NOT NULL,
        status NVARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELED
        total_amount DECIMAL(18,2) NOT NULL,
        shipping_name NVARCHAR(100) NOT NULL,
        shipping_phone NVARCHAR(20) NOT NULL,
        shipping_address NVARCHAR(300) NOT NULL,
        payment_method NVARCHAR(30) NOT NULL, -- COD, BankTransfer, VNPay
        created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
        CONSTRAINT FK_Orders_User FOREIGN KEY(user_id) REFERENCES dbo.Users(id)
    );
    CREATE INDEX IX_Orders_User_Status ON dbo.Orders(user_id, status);
END
GO

/* OrderItems */
IF OBJECT_ID('dbo.OrderItems','U') IS NULL
BEGIN
    CREATE TABLE dbo.OrderItems (
        id INT IDENTITY PRIMARY KEY,
        order_id INT NOT NULL,
        product_id INT NOT NULL,
        qty INT NOT NULL,
        unit_price DECIMAL(18,2) NOT NULL,
        CONSTRAINT FK_OrderItems_Order FOREIGN KEY(order_id) REFERENCES dbo.Orders(id) ON DELETE CASCADE,
        CONSTRAINT FK_OrderItems_Product FOREIGN KEY(product_id) REFERENCES dbo.Products(id)
    );
END
GO

/* Views */
IF OBJECT_ID('dbo.vRevenueByDay','V') IS NULL
EXEC('CREATE VIEW dbo.vRevenueByDay AS
      SELECT CAST(created_at AS DATE) AS d,
             SUM(total_amount) AS revenue
      FROM dbo.Orders
      WHERE status IN (''PROCESSING'',''SHIPPED'',''DELIVERED'')
      GROUP BY CAST(created_at AS DATE)');
GO

IF OBJECT_ID('dbo.vRevenueByMonth','V') IS NULL
EXEC('CREATE VIEW dbo.vRevenueByMonth AS
      SELECT CONCAT(YEAR(created_at),''-'',FORMAT(created_at,''MM'')) AS ym,
             SUM(total_amount) AS revenue
      FROM dbo.Orders
      WHERE status IN (''PROCESSING'',''SHIPPED'',''DELIVERED'')
      GROUP BY YEAR(created_at), FORMAT(created_at,''MM'')');
GO

IF OBJECT_ID('dbo.vBestSellingProducts','V') IS NULL
EXEC('CREATE VIEW dbo.vBestSellingProducts AS
      SELECT oi.product_id, SUM(oi.qty) AS qty
      FROM dbo.OrderItems oi
      JOIN dbo.Orders o ON oi.order_id=o.id
      WHERE o.status IN (''PROCESSING'',''SHIPPED'',''DELIVERED'')
      GROUP BY oi.product_id');
GO

IF OBJECT_ID('dbo.vTopRatedProducts','V') IS NULL
EXEC('CREATE VIEW dbo.vTopRatedProducts AS
      SELECT product_id, AVG(CAST(rating AS FLOAT)) AS avg_rating, COUNT(*) AS reviews
      FROM dbo.ProductReviews
      GROUP BY product_id');
GO

/* Seed roles */
IF NOT EXISTS (SELECT 1 FROM dbo.Roles WHERE name='ADMIN') INSERT INTO dbo.Roles(name) VALUES('ADMIN');
IF NOT EXISTS (SELECT 1 FROM dbo.Roles WHERE name='USER')  INSERT INTO dbo.Roles(name) VALUES('USER');
GO

/* Seed admin if missing */
IF NOT EXISTS (SELECT 1 FROM dbo.Users WHERE username='admin')
BEGIN
    DECLARE @salt VARBINARY(16) = CRYPT_GEN_RANDOM(16);
    DECLARE @password NVARCHAR(100) = N'admin123';
    DECLARE @hash VARBINARY(32) = HASHBYTES('SHA2_256', @salt + CONVERT(VARBINARY(200), @password));
    INSERT INTO dbo.Users(username,email,full_name,password_salt,password_hash)
    VALUES('admin','admin@example.com',N'Administrator',@salt,@hash);
END
GO

/* Grant ADMIN role */
DECLARE @adminId INT = (SELECT id FROM dbo.Users WHERE username='admin');
DECLARE @roleAdminId INT = (SELECT id FROM dbo.Roles WHERE name='ADMIN');
IF NOT EXISTS (SELECT 1 FROM dbo.UserRoles WHERE user_id=@adminId AND role_id=@roleAdminId)
    INSERT INTO dbo.UserRoles(user_id, role_id) VALUES(@adminId, @roleAdminId);
GO

/* Sample categories */
IF NOT EXISTS (SELECT 1 FROM dbo.Categories)
BEGIN
    INSERT INTO dbo.Categories(name, slug) VALUES
    (N'Smartphones','smartphones'),
    (N'Tablets','tablets'),
    (N'Accessories','accessories');
END
GO

/* Ensure each user has an OPEN cart */
MERGE dbo.Carts AS tgt
USING (SELECT id FROM dbo.Users) AS src(user_id)
ON tgt.user_id = src.user_id AND tgt.status = 'OPEN'
WHEN NOT MATCHED THEN INSERT(user_id) VALUES(src.user_id);
GO
