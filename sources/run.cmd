@echo off
setlocal

set "EXEC_ARGS="
if "%~1" NEQ "" (
    set "EXEC_ARGS=-Dexec.args="
    :loop_args
    if "%~1" NEQ "" (
        set "EXEC_ARGS=%EXEC_ARGS%%~1 "
        shift
        goto :loop_args
    )
    rem Remove trailing space
    set "EXEC_ARGS=%EXEC_ARGS:~0,-1%"
)

mvn -e -q compile exec:java -Dexec.args="%EXEC_ARGS%"

if %errorlevel% neq 0 (
    echo Command failed with error code %errorlevel%.
)

endlocal
pause