SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

SET ANSI_PADDING ON
GO

CREATE TABLE [InternalMessages](
	[ID] [binary](16) NOT NULL,
	[Subject] [nvarchar](256) NULL,
	[SenderUserID] [binary](16) NULL,
	[CreatedDate] [bigint] NOT NULL,
	[ReadFlag] [bit] NOT NULL,
	[ImportantFlag] [bit] NOT NULL,
	[SenderDeletedFlag] [bit] NOT NULL,
	[ThreadID] [binary](16) NOT NULL,
 CONSTRAINT [PK_InternalMessages] PRIMARY KEY NONCLUSTERED 
(
	[ID] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON)
)

GO

SET ANSI_PADDING OFF
GO


CREATE CLUSTERED INDEX [IX_InternalMessages_CreatedDate] ON [InternalMessages] 
(
	[CreatedDate] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON)
GO


CREATE NONCLUSTERED INDEX [IX_InternalMessages_SenderUserID] ON [InternalMessages] 
(
	[SenderUserID] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON)
GO


CREATE NONCLUSTERED INDEX [IX_InternalMessages_ThreadID] ON [InternalMessages] 
(
	[ThreadID] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON)
GO

ALTER TABLE [InternalMessages] ADD  CONSTRAINT [DF_InternalMessages_ReadFlag]  DEFAULT ((0)) FOR [ReadFlag]
GO

ALTER TABLE [InternalMessages] ADD  CONSTRAINT [DF_InternalMessages_ImportantFlag]  DEFAULT ((0)) FOR [ImportantFlag]
GO

ALTER TABLE [InternalMessages] ADD  CONSTRAINT [DF_InternalMessages_DeletedFlag]  DEFAULT ((0)) FOR [SenderDeletedFlag]
GO

