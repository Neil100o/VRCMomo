@echo off
setlocal
python "%~dp0export_vrcx_activity.py" %*
if errorlevel 1 pause
