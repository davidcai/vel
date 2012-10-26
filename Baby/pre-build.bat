if not exist bin mkdir bin

xcopy ..\Samoyan\lib\*.jar bin\WEB-INF\lib /d /y /i
xcopy ..\Samoyan\bin\*.* bin /d /y /s /e /i

xcopy lib\*.jar bin\WEB-INF\lib /d /y /i
xcopy web\*.* bin /y /s /e /i
