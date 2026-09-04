@echo off
rem vaadin-dev.cmd - launches the PowerShell port beside this file.
rem
rem A shim and nothing else: the contract, including the exit codes agents depend
rem on, lives in vaadin-dev.ps1. -ExecutionPolicy Bypass because this is project
rem tooling checked into the repository, not a downloaded script, and a machine
rem policy of Restricted would otherwise make it unusable.
rem
rem Installed by "mvn vaadin:install-dev-cli" and meant to be committed, like
rem mvnw. Rewritten by that goal whenever it changes, so edits here do not
rem survive an upgrade.
setlocal
set "VAADIN_DEV_PS1=%~dp0vaadin-dev.ps1"
where /q pwsh.exe
rem Tested with "if errorlevel", not "&&": with && the failure branch would also
rem run whenever pwsh itself exited non-zero, which is every failed apply.
if errorlevel 1 (
    powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%VAADIN_DEV_PS1%" %*
) else (
    pwsh.exe -NoProfile -ExecutionPolicy Bypass -File "%VAADIN_DEV_PS1%" %*
)
rem %ERRORLEVEL% is expanded while this line is parsed, before endlocal runs, so
rem the exit code survives the environment being restored.
endlocal & exit /b %ERRORLEVEL%
