@REM ----------------------------------------------------------------------------
@REM Maven Start Up Batch script for CDG Freelance Project
@REM ----------------------------------------------------------------------------
@echo off
set ERROR_CODE=0

if exist "%~dp0..\..\..\.m2\wrapper\dists\apache-maven-3.9.12-bin\5nmfsn99br87k5d4ajlekdq10k\apache-maven-3.9.12\bin\mvn.cmd" (
    call "%~dp0..\..\..\.m2\wrapper\dists\apache-maven-3.9.12-bin\5nmfsn99br87k5d4ajlekdq10k\apache-maven-3.9.12\bin\mvn.cmd" %*
    goto end
)

where mvn >nul 2>nul
if %ERRORLEVEL% equ 0 (
    call mvn %*
    goto end
)

set MAVEN_FALLBACK="C:\Users\ilyas\.m2\wrapper\dists\apache-maven-3.9.12-bin\5nmfsn99br87k5d4ajlekdq10k\apache-maven-3.9.12\bin\mvn.cmd"
if exist %MAVEN_FALLBACK% (
    call %MAVEN_FALLBACK% %*
    goto end
)

echo Error: Maven could not be found.
exit /b 1

:end
exit /b %ERRORLEVEL%
