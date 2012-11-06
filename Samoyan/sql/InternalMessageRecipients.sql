SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

SET ANSI_PADDING ON
GO

CREATE TABLE [InternalMessageRecipients](
	[ID] [binary](16) NOT NULL,
	[InternalMessageID] [binary](16) NOT NULL,
	[RecipientUserID] [binary](16) NOT NULL,
	[RecipientDeletedFlag] [bit] NOT NULL,
 CONSTRAINT [PK_InternalMessageRecipients] PRIMARY KEY NONCLUSTERED 
(
	[ID] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON)
)

GO

SET ANSI_PADDING OFF
GO


CREATE NONCLUSTERED INDEX [IX_InternalMessageRecipients_InternalMessageID] ON [InternalMessageRecipients] 
(
	[InternalMessageID] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON)
GO


CREATE NONCLUSTERED INDEX [IX_InternalMessageRecipients_RecipientUserID] ON [InternalMessageRecipients] 
(
	[RecipientUserID] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON)
GO

ALTER TABLE [InternalMessageRecipients] ADD  CONSTRAINT [DF_InternalMessageRecipients_RecipientDeletedFlag]  DEFAULT ((0)) FOR [RecipientDeletedFlag]
GO

