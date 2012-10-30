/****** Object:  Table [Drugs]    Script Date: 06/10/2012 22:31:27 ******/
SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

SET ANSI_PADDING ON
GO

CREATE TABLE [Drugs](
	[ID] [binary](16) NOT NULL,
	[Name] [nvarchar](256) NOT NULL,
	[GenericName] [nvarchar](256) NOT NULL,
	[PatientID] [binary](16) NULL
) ON [PRIMARY]

GO

SET ANSI_PADDING OFF
GO

