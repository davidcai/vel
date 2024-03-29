SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

SET ANSI_PADDING ON
GO

CREATE TABLE [Elerts](
	[ID] [binary](16) NOT NULL,
	[SubscriptionID] [binary](16) NOT NULL,
	[OpeningID] [binary](16) NOT NULL,
	[SchedulerID] [binary](16) NOT NULL,
	[PatientID] [binary](16) NOT NULL,
	[RegionID] [binary](16) NOT NULL,
	[ServiceAreaID] [binary](16) NOT NULL,
	[FacilityID] [binary](16) NOT NULL,
	[SentDate] [bigint] NOT NULL,
	[Reply] [smallint] NOT NULL,
	[ReplyChannel] [nvarchar](8) NULL,
	[ReplyDate] [bigint] NULL,
	[Decision] [smallint] NOT NULL,
	[DecisionDate] [bigint] NULL,
	[OpeningDate] [bigint] NOT NULL,
	[Hidden] [bit] NOT NULL,
 CONSTRAINT [PK_Elerts] PRIMARY KEY NONCLUSTERED 
(
	[ID] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON)
)

GO

SET ANSI_PADDING OFF
GO


CREATE NONCLUSTERED INDEX [IX_Elerts_OpeningID] ON [Elerts] 
(
	[OpeningID] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON)
GO


CREATE NONCLUSTERED INDEX [IX_Elerts_PatientID] ON [Elerts] 
(
	[PatientID] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON)
GO


CREATE NONCLUSTERED INDEX [IX_Elerts_RegionID] ON [Elerts] 
(
	[RegionID] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON)
GO


CREATE NONCLUSTERED INDEX [IX_Elerts_SchedulerID] ON [Elerts] 
(
	[SchedulerID] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON)
GO


CREATE NONCLUSTERED INDEX [IX_Elerts_ServiceAreaID] ON [Elerts] 
(
	[ServiceAreaID] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON)
GO

ALTER TABLE [Elerts] ADD  CONSTRAINT [DF_Elert_Reply]  DEFAULT ((0)) FOR [Reply]
GO

ALTER TABLE [Elerts] ADD  CONSTRAINT [DF_Elerts_Decision]  DEFAULT ((0)) FOR [Decision]
GO

ALTER TABLE [Elerts] ADD  CONSTRAINT [DF_Elerts_Hidden]  DEFAULT ((0)) FOR [Hidden]
GO

