@echo off

for %%A in ("%~dp0\..") do set "DIR=%%~fA"

if exist "%DIR%\dist\ROSMOP.jar" ( 
java -cp "%DIR%\dist\ROSMOP.jar" rosmop.Main %1
) else (
echo Binaries are not created. Call "ant" first.
)