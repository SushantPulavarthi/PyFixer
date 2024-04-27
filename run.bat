@echo off
if not exist ".\build\install\PyFixer" (
    gradlew installDist
    .\build\install\PyFixer\bin\PyFixer %1
) else (
    .\build\install\PyFixer\bin\PyFixer %1
)
