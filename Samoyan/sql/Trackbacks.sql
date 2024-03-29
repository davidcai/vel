SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

SET ANSI_PADDING ON
GO

CREATE TABLE [Trackbacks](
	[ID] [binary](16) NOT NULL,
	[Channel] [nvarchar](8) NOT NULL,
	[Addressee] [nvarchar](128) NOT NULL,
	[RoundRobin] [int] NOT NULL,
	[Created] [bigint] NOT NULL,
	[ExternalID] [nvarchar](128) NULL,
 CONSTRAINT [PK_Trackbacks] PRIMARY KEY CLUSTERED 
(
	[ID] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON)
)

GO

SET ANSI_PADDING OFF
GO


CREATE NONCLUSTERED INDEX [IX_Trackbacks_Addressee] ON [Trackbacks] 
(
	[Channel] ASC,
	[Addressee] ASC,
	[RoundRobin] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON)
GO


CREATE NONCLUSTERED INDEX [IX_Trackbacks_Created] ON [Trackbacks] 
(
	[Created] DESC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON)
GO


CREATE NONCLUSTERED INDEX [IX_Trackbacks_ExternalID] ON [Trackbacks] 
(
	[ExternalID] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON)
GO

ALTER TABLE [Trackbacks] ADD  CONSTRAINT [DF_Trackback_RoundRobin]  DEFAULT ((0)) FOR [RoundRobin]
GO

