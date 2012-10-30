SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

SET ANSI_PADDING ON
GO

CREATE TABLE [Articles](
	[ID] [binary](16) NOT NULL,
	[SourceURLHash] [binary](32) NULL,
	[Region] [nvarchar](64) NULL,
	[MedicalCenter] [nvarchar](64) NULL,
	[Section] [nvarchar](64) NULL,
	[TimelineFrom] [smallint] NULL,
	[TimelineTo] [smallint] NULL,
	[UpdatedDate] [bigint] NOT NULL,
	[Title] [nvarchar](128) NULL,
	[Priority] [int] NOT NULL,
 CONSTRAINT [PK_Articles] PRIMARY KEY NONCLUSTERED 
(
	[ID] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON)
)

GO

SET ANSI_PADDING OFF
GO


CREATE NONCLUSTERED INDEX [IX_Articles_Region_MedicalCenter] ON [Articles] 
(
	[Region] ASC,
	[MedicalCenter] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON)
GO


CREATE NONCLUSTERED INDEX [IX_Articles_Section] ON [Articles] 
(
	[Section] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON)
GO


CREATE NONCLUSTERED INDEX [IX_Articles_SourceURLHash] ON [Articles] 
(
	[SourceURLHash] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON)
GO


CREATE NONCLUSTERED INDEX [IX_Articles_TimelineFrom] ON [Articles] 
(
	[TimelineFrom] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON)
GO


CREATE NONCLUSTERED INDEX [IX_Articles_TimelineTo] ON [Articles] 
(
	[TimelineTo] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON)
GO


CREATE NONCLUSTERED INDEX [IX_Articles_UpdatedDate] ON [Articles] 
(
	[UpdatedDate] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON)
GO

ALTER TABLE [Articles] ADD  CONSTRAINT [DF_Articles_SortOrder]  DEFAULT ((0)) FOR [Priority]
GO
