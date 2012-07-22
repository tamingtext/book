echo off
SETLOCAL enabledelayedexpansion

@rem JAVA_HOME=C:\Program Files\Java\jre1.6.0_10

for %%i in (..\target\*.jar) do set CLASSPATH=!CLASSPATH!;%%i
for %%i in (..\target\dependency\*.jar) do set CLASSPATH=!CLASSPATH!;%%i

set MEM_ARGS=-Xms512m -Xmx512m

echo on
"%JAVA_HOME%\bin\java" %MEM_ARGS% -classpath "%CLASSPATH%" com.tamingtext.qa.WikipediaWexIndexer %*

ENDLOCAL
