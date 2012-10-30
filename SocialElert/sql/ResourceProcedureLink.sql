SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

SET ANSI_PADDING ON
GO

CREATE TABLE [ResourceProcedureLink](
	[ResourceID] [binary](16) NOT NULL,
	[ProcedureID] [binary](16) NOT NULL,
	[Rank] [smallint] NOT NULL,
 CONSTRAINT [PK_ResourceProcedureLink] PRIMARY KEY NONCLUSTERED 
(
	[ResourceID] ASC,
	[ProcedureID] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON)
)

GO

SET ANSI_PADDING OFF
GO


CREATE NONCLUSTERED INDEX [IX_ResourceProcedureLink_ProcedureID] ON [ResourceProcedureLink] 
(
	[ProcedureID] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON)
GO


CREATE NONCLUSTERED INDEX [IX_ResourceProcedureLink_ResourceID] ON [ResourceProcedureLink] 
(
	[ResourceID] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON)
GO

