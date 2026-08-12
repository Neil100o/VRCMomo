@echo off
setlocal
python -c "import qrcode; from PIL import ImageTk" >nul 2>nul
if errorlevel 1 (
    echo Installing QR pairing helper from official PyPI...
    python -m pip install --user --index-url https://pypi.org/simple --disable-pip-version-check "qrcode[pil]>=7.4"
)
if errorlevel 1 (
    echo QR helper installation failed. Check your network or Python installation.
    pause
    exit /b 1
)
python "%~dp0vrcmomo_lan_bridge.py" %*
if errorlevel 1 pause
