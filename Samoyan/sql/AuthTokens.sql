SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

SET ANSI_PADDING ON
GO

CREATE TABLE [AuthTokens](
	[ID] [binary](16) NOT NULL,
	[UserID] [binary](16) NOT NULL,
	[Life] [bigint] NOT NULL,
	[LastAccessed] [bigint] NOT NULL,
	[UserAgentSignature] [nvarchar](64) NULL,
 CONSTRAINT [PK_AuthTokens] PRIMARY KEY NONCLUSTERED 
(
	[ID] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON)
)

GO

SET ANSI_PADDING OFF
GO


CREATE NONCLUSTERED INDEX [IX_AuthTokens_UserID] ON [AuthTokens] 
(
	[UserID] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON)
GO
