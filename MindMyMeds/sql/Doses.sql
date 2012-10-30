/****** Object:  Table [Doses]    Script Date: 06/10/2012 22:31:20 ******/
SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

SET ANSI_PADDING ON
GO

CREATE TABLE [Doses](
	[ID] [binary](16) NOT NULL,
	[PatientID] [binary](16) NOT NULL,
	[PrescriptionID] [binary](16) NOT NULL,
	[TakeDate] [bigint] NOT NULL,
	[ResolutionDate] [bigint] NULL,
	[Resolution] [smallint] NOT NULL,
	[SkipReason] [nvarchar](128) NULL,
	[SentToMobile] [nvarchar](24) NULL,
	[ShortCode] [int] NULL
) ON [PRIMARY]

GO

SET ANSI_PADDING OFF
GO

