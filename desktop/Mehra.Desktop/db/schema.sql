-- ============================================================
-- MEHRA 1.1 — schema for SQL Server (any version, 2008+ works).
-- Safe to re-run: every statement is idempotent (IF NOT EXISTS).
-- Run once after installing SQL Server (Express / LocalDB / any).
-- ============================================================
IF DB_ID(N'Mehra') IS NULL
BEGIN
    CREATE DATABASE [Mehra];
END
GO
USE [Mehra];
GO

IF OBJECT_ID(N'dbo.users', N'U') IS NULL
CREATE TABLE dbo.users (
    id              UNIQUEIDENTIFIER CONSTRAINT DF_users_id DEFAULT NEWID() PRIMARY KEY,
    fullName        NVARCHAR(100)  NOT NULL,
    username        NVARCHAR(50)   NOT NULL CONSTRAINT UQ_users_username UNIQUE,
    passwordHash    NVARCHAR(128)  NOT NULL,
    salt            NVARCHAR(64)   NOT NULL,
    roleCode        NVARCHAR(30)   NOT NULL DEFAULT N'RECEPTION',
    roleTitle       NVARCHAR(60)   NOT NULL DEFAULT N'',
    phone           NVARCHAR(30)   NULL,
    isActive        BIT            NOT NULL DEFAULT 1,
    lastLoginAtUtc  DATETIME2      NULL,
    createdAtUtc    DATETIME2      NOT NULL DEFAULT SYSUTCDATETIME()
);
GO

IF OBJECT_ID(N'dbo.rooms', N'U') IS NULL
CREATE TABLE dbo.rooms (
    id                UNIQUEIDENTIFIER CONSTRAINT DF_rooms_id DEFAULT NEWID() PRIMARY KEY,
    roomNumber        NVARCHAR(10)  NOT NULL CONSTRAINT UQ_rooms_number UNIQUE,
    floor             INT           NOT NULL DEFAULT 1,
    capacity          INT           NOT NULL DEFAULT 2,
    roomTypeName      NVARCHAR(60)  NOT NULL DEFAULT N'',
    ratePerNightRial  BIGINT        NOT NULL DEFAULT 0,
    status            NVARCHAR(20)  NOT NULL DEFAULT N'FREE',
    currentGuestName  NVARCHAR(100) NULL
);
GO

IF OBJECT_ID(N'dbo.guests', N'U') IS NULL
CREATE TABLE dbo.guests (
    id             UNIQUEIDENTIFIER CONSTRAINT DF_guests_id DEFAULT NEWID() PRIMARY KEY,
    firstName      NVARCHAR(60)  NOT NULL DEFAULT N'',
    lastName       NVARCHAR(60)  NOT NULL DEFAULT N'',
    phone          NVARCHAR(30)  NULL,
    city           NVARCHAR(60)  NULL,
    balanceRial    BIGINT        NOT NULL DEFAULT 0,
    totalStays     INT           NOT NULL DEFAULT 0,
    lastStayAtUtc  DATETIME2     NULL
);
GO

IF OBJECT_ID(N'dbo.reservations', N'U') IS NULL
CREATE TABLE dbo.reservations (
    id           UNIQUEIDENTIFIER CONSTRAINT DF_res_id DEFAULT NEWID() PRIMARY KEY,
    code         NVARCHAR(20)  NOT NULL,
    guestName    NVARCHAR(120) NOT NULL DEFAULT N'',
    guestPhone   NVARCHAR(30)  NULL,
    roomNumber   NVARCHAR(10)  NULL,
    checkInDate  DATE          NOT NULL,
    checkOutDate DATE          NOT NULL,
    guestsCount  INT           NOT NULL DEFAULT 1,
    totalRial    BIGINT        NOT NULL DEFAULT 0,
    paidRial     BIGINT        NOT NULL DEFAULT 0,
    status       NVARCHAR(20)  NOT NULL DEFAULT N'PENDING'
);
GO

IF OBJECT_ID(N'dbo.stays', N'U') IS NULL
CREATE TABLE dbo.stays (
    id               UNIQUEIDENTIFIER CONSTRAINT DF_stays_id DEFAULT NEWID() PRIMARY KEY,
    reservationCode  NVARCHAR(20)  NOT NULL DEFAULT N'',
    guestName        NVARCHAR(120) NOT NULL DEFAULT N'',
    roomNumber       NVARCHAR(10)  NULL,
    checkInAtUtc     DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME(),
    plannedCheckOut  DATE          NULL,
    guestsCount      INT           NOT NULL DEFAULT 1,
    status           NVARCHAR(20)  NOT NULL DEFAULT N'IN_HOUSE'
);
GO

IF OBJECT_ID(N'dbo.transactions', N'U') IS NULL
CREATE TABLE dbo.transactions (
    id            UNIQUEIDENTIFIER CONSTRAINT DF_tx_id DEFAULT NEWID() PRIMARY KEY,
    code          NVARCHAR(20)  NOT NULL DEFAULT N'',
    type          NVARCHAR(20)  NOT NULL DEFAULT N'RECEIVE',
    amountRial    BIGINT        NOT NULL DEFAULT 0,
    method        NVARCHAR(20)  NOT NULL DEFAULT N'CASH',
    description   NVARCHAR(300) NULL,
    guestName     NVARCHAR(120) NULL,
    roomNumber    NVARCHAR(10)  NULL,
    operatorName  NVARCHAR(100) NULL,
    createdAtUtc  DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME(),
    isReversed    BIT           NOT NULL DEFAULT 0
);
GO

IF OBJECT_ID(N'dbo.housekeeping_tasks', N'U') IS NULL
CREATE TABLE dbo.housekeeping_tasks (
    id              UNIQUEIDENTIFIER CONSTRAINT DF_hk_id DEFAULT NEWID() PRIMARY KEY,
    roomNumber      NVARCHAR(10)  NULL,
    type            NVARCHAR(20)  NOT NULL DEFAULT N'DAILY',
    status          NVARCHAR(20)  NOT NULL DEFAULT N'PENDING',
    assignedToName  NVARCHAR(100) NULL,
    notes           NVARCHAR(300) NULL,
    createdAtUtc    DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME(),
    finishedAtUtc   DATETIME2     NULL
);
GO

IF OBJECT_ID(N'dbo.maintenance_tickets', N'U') IS NULL
CREATE TABLE dbo.maintenance_tickets (
    id            UNIQUEIDENTIFIER CONSTRAINT DF_mt_id DEFAULT NEWID() PRIMARY KEY,
    code          NVARCHAR(20)  NOT NULL DEFAULT N'',
    roomNumber    NVARCHAR(10)  NULL,
    title         NVARCHAR(200) NOT NULL DEFAULT N'',
    description   NVARCHAR(500) NULL,
    priority      NVARCHAR(20)  NOT NULL DEFAULT N'NORMAL',
    status        NVARCHAR(20)  NOT NULL DEFAULT N'OPEN',
    assigneeName  NVARCHAR(100) NULL,
    costRial      BIGINT        NULL,
    reporterName  NVARCHAR(100) NULL,
    createdAtUtc  DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME(),
    resolvedAtUtc DATETIME2     NULL
);
GO

IF OBJECT_ID(N'dbo.notifications', N'U') IS NULL
CREATE TABLE dbo.notifications (
    id           UNIQUEIDENTIFIER CONSTRAINT DF_notif_id DEFAULT NEWID() PRIMARY KEY,
    title        NVARCHAR(200) NOT NULL DEFAULT N'',
    body         NVARCHAR(500) NULL,
    severity     NVARCHAR(20)  NOT NULL DEFAULT N'LOW',
    kind         NVARCHAR(30)  NULL,
    createdAtUtc DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME(),
    isRead       BIT           NOT NULL DEFAULT 0
);
GO

-- Bootstrap admin (username: admin — change the password after first login).
-- password = Admin@1234  (SHA-256 of salt+password, salt = 'mehra-seed')
IF NOT EXISTS (SELECT 1 FROM dbo.users WHERE username = N'admin')
BEGIN
    DECLARE @salt NVARCHAR(64) = N'mehra-seed';
    DECLARE @hash NVARCHAR(128) = CONVERT(NVARCHAR(128),
        LOWER(CONVERT(VARCHAR(128), HASHBYTES('SHA2_256', @salt + N'Admin@1234'), 2)));
    INSERT INTO dbo.users (fullName, username, passwordHash, salt, roleCode, roleTitle, isActive)
    VALUES (N'مدیر کل', N'admin', @hash, @salt, N'ADMIN', N'مدیر کل', 1);
END
GO
