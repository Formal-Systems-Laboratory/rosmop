@echo off

for %%A in ("%~dp0\..") do set "DIR=%%~fA"
setlocal
cd %DIR%\src\rosmop\parser\main_parser
javacc rosmopparser.jj
endlocal
