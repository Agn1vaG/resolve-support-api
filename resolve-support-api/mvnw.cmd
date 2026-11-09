@echo off
setlocal
set "MAVEN_PROJECTBASEDIR=%~dp0"
if "%MAVEN_PROJECTBASEDIR:~-1%"=="\" set "MAVEN_PROJECTBASEDIR=%MAVEN_PROJECTBASEDIR:~0,-1%"
set "WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"

if defined JAVA_HOME (
  set "JAVACMD=%JAVA_HOME%\bin\java.exe"
  if not exist "%JAVACMD%" goto java_missing
) else (
  set "JAVACMD=java.exe"
  where java.exe >nul 2>nul
  if errorlevel 1 goto java_missing
)

"%JAVACMD%" -Dmaven.multiModuleProjectDirectory="%MAVEN_PROJECTBASEDIR%" -classpath "%WRAPPER_JAR%" org.apache.maven.wrapper.MavenWrapperMain %*
exit /b %ERRORLEVEL%

:java_missing
echo Java was not found. Install JDK 21 and set JAVA_HOME or add java to PATH. 1>&2
exit /b 1
