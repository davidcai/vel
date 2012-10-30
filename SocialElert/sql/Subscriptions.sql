SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

SET ANSI_PADDING ON
GO

CREATE TABLE [Subscriptions](
	[ID] [binary](16) NOT NULL,
	[UserID] [binary](16) NOT NULL,
	[AreaID] [binary](16) NOT NULL,
	[AdvanceNotice] [smallint] NOT NULL,
	[OriginalDate] [bigint] NULL,
	[AcceptOtherPhysician] [bit] NOT NULL,
	[VerifiedBy] [binary](16) NULL,
	[VerifiedDate] [bigint] NULL,
	[CreatedDate] [bigint] NOT NULL,
	[Urgent] [bit] NOT NULL,
	[Duration] [smallint] NOT NULL,
	[Finalized] [bit] NOT NULL,
	[Removed] [bit] NOT NULL,
 CONSTRAINT [PK_Subscriptions] PRIMARY KEY NONCLUSTERED 
(
	[ID] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON)
)

GO

SET ANSI_PADDING OFF
GO


CREATE CLUSTERED INDEX [IX_Subscriptions_UserID] ON [Subscriptions] 
(
	[UserID] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON)
GO


CREATE NONCLUSTERED INDEX [IX_Subscriptions_CreatedDate] ON [Subscriptions] 
(
	[CreatedDate] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON)
GO

ALTER TABLE [Subscriptions] ADD  CONSTRAINT [DF_Subscriptions_Finalized]  DEFAULT ((0)) FOR [Finalized]
GO

ALTER TABLE [Subscriptions] ADD  CONSTRAINT [DF_Subscriptions_Removed]  DEFAULT ((0)) FOR [Removed]
GO

ALTER TABLE [Subscriptions] ADD  CONSTRAINT [DF_Subscriptions_Expired]  DEFAULT ((0)) FOR [Expired]
GO

