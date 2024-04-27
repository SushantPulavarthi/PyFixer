@echo off
set arg1=%1
set arg2=%2
if not exist ".\build\install\PyFixer"  (
    gradlew installDist
    .\build\install\PyFixer\bin\PyFixer %arg2%
) else (
    if "%arg1%"=="--rebuild" (
        gradlew installDist
        .\build\install\PyFixer\bin\PyFixer %arg2%
    ) else (
        .\build\install\PyFixer\bin\PyFixer %arg1%
    )
)
