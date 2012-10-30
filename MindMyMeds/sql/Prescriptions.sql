/****** Object:  Table [Prescriptions]    Script Date: 06/10/2012 22:31:58 ******/
SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

SET ANSI_PADDING ON
GO

CREATE TABLE [Prescriptions](
	[ID] [binary](16) NOT NULL,
	[DrugID] [binary](16) NOT NULL,
	[DoctorID] [binary](16) NULL,
	[DoctorName] [nvarchar](128) NULL,
	[PatientID] [binary](16) NOT NULL,
	[Nickname] [nvarchar](32) NULL,
	[Purpose] [nvarchar](64) NULL,
	[Instructions] [nvarchar](32) NULL,
	[DosesRemaining] [int] NOT NULL,
	[NextDoseDate] [bigint] NULL,
	[FreqDays] [smallint] NOT NULL,
	[QuarterHourBitmap] [binary](12) NOT NULL,
	[DoseInfo] [nvarchar](32) NULL
) ON [PRIMARY]

GO

SET ANSI_PADDING OFF
GO

