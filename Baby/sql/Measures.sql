SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

SET ANSI_PADDING ON
GO

CREATE TABLE [Measures](
	[ID] [binary](16) NOT NULL,
	[Label] [nvarchar](64) NOT NULL, 
	[ForMother] [bit] NOT NULL, 
	[ForPreconception] [bit] NOT NULL, 
	[ForPregnancy] [bit] NOT NULL, 
	[ForInfancy] [bit] NOT NULL, 
	[ImperialUnit] [nvarchar](16) NOT NULL,
	[MetricUnit] [nvarchar](16) NOT NULL,
	[MetricToImperialAlpha] [numeric](28, 8) NOT NULL,
	[MetricToImperialBeta] [numeric](28, 8) NOT NULL, 
	[MetricMin] [int] NOT NULL, 
	[MetricMax] [int] NOT NULL, 
 CONSTRAINT [PK_Measures] PRIMARY KEY NONCLUSTERED 
(
	[ID] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON)
)

GO

SET ANSI_PADDING OFF
GO

ALTER TABLE [Measures] ADD  CONSTRAINT [DF_Measures_ForMother]  DEFAULT ((0)) FOR [ForMother]
GO

ALTER TABLE [Measures] ADD  CONSTRAINT [DF_Measures_ForPreconception]  DEFAULT ((0)) FOR [ForPreconception]
GO

ALTER TABLE [Measures] ADD  CONSTRAINT [DF_Measures_ForPregnancy]  DEFAULT ((0)) FOR [ForPregnancy]
GO

ALTER TABLE [Measures] ADD  CONSTRAINT [DF_Measures_ForInfancy]  DEFAULT ((0)) FOR [ForInfancy]
GO