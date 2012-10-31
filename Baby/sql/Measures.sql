SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

SET ANSI_PADDING ON
GO

CREATE TABLE [Measures](
	[ID] [binary](16) NOT NULL,
	[Label] [nvarchar](64) NOT NULL, 
	[UnitTypeID] [binary](16) NOT NULL,
	[ForMother] [bit] NOT NULL, 
	[ForPreconception] [bit] NOT NULL, 
	[ForPregnancy] [bit] NOT NULL, 
	[ForInfancy] [bit] NOT NULL, 
	[MinValue] [int] NULL, 
	[MaxValue] [int] NULL, 
	[DefValue] [int] NULL,  
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