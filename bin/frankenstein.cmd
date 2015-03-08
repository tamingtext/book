echo off
SETLOCAL enabledelayedexpansion

@rem JAVA_HOME=C:\Program Files\Java\jre1.6.0_10

for %%i in (.\target\dependency\*.jar) do set CLASSPATH=!CLASSPATH!;%%i
for %%i in (.\lib\*.jar) do set CLASSPATH=!CLASSPATH!;%%i
for %%i in (.\lib\solrj\*.jar) do set CLASSPATH=!CLASSPATH!;%%i
set CLASSPATH=!CLASSPATH!;.\target\test-classes
set MEM_ARGS=-Xms512m -Xmx1024m

echo on
"%JAVA_HOME%\bin\java" %MEM_ARGS% -classpath "%CLASSPATH%" com.tamingtext.frankenstein.Frankenstein

ENDLOCAL
