if not exist bin mkdir bin

xcopy lib\*.jar bin\WEB-INF\lib /d /y /i
xcopy web\*.* bin /d /y /s /e /i

