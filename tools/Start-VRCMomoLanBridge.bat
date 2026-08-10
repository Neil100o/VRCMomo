@echo off
setlocal
python -m pip show qrcode >nul 2>nul || python -m pip install -r "%~dp0requirements-lan-bridge.txt"
if errorlevel 1 pause & exit /b 1
python "%~dp0vrcmomo_lan_bridge.py" %*
if errorlevel 1 pause
