SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

SET ANSI_PADDING ON
GO

CREATE TABLE [LogTypes](
	[ID] [binary](16) NOT NULL,
	[Name] [nvarchar](32) NOT NULL,
	[Severity] [smallint] NOT NULL,
	[Life] [bigint] NOT NULL,
	[M1Label] [nvarchar](32) NULL,
	[M2Label] [nvarchar](32) NULL,
	[M3Label] [nvarchar](32) NULL,
	[M4Label] [nvarchar](32) NULL,
	[S1Label] [nvarchar](32) NULL,
	[S2Label] [nvarchar](32) NULL,
	[S3Label] [nvarchar](32) NULL,
	[S4Label] [nvarchar](32) NULL,
	[T1Label] [nvarchar](32) NULL,
	[T2Label] [nvarchar](32) NULL,
 CONSTRAINT [PK_LogTypes] PRIMARY KEY NONCLUSTERED 
(
	[ID] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON)
)

GO

SET ANSI_PADDING OFF
GO


CREATE UNIQUE NONCLUSTERED INDEX [IX_LogTypes_Name] ON [LogTypes] 
(
	[Name] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON)
GO

ALTER TABLE [LogTypes] ADD  CONSTRAINT [DF_LogTypes_Life]  DEFAULT ((-1)) FOR [Life]
GO

