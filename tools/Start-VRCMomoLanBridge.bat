@echo off
setlocal
python "%~dp0vrcmomo_lan_bridge.py" %*
if errorlevel 1 pause
