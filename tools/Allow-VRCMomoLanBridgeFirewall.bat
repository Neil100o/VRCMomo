@echo off
setlocal

:: The bridge uses TCP 38671 for paired activity transfer and UDP 38672 for
:: local discovery.  Restrict both rules to Windows Private networks only.
net session >nul 2>&1
if not "%errorlevel%"=="0" (
  powershell -NoProfile -Command "Start-Process -FilePath '%~f0' -Verb RunAs"
  exit /b
)

netsh advfirewall firewall delete rule name="VRCMomo LAN Bridge TCP" >nul 2>&1
netsh advfirewall firewall delete rule name="VRCMomo LAN Bridge Discovery" >nul 2>&1
netsh advfirewall firewall add rule name="VRCMomo LAN Bridge TCP" dir=in action=allow protocol=TCP localport=38671 profile=private >nul
netsh advfirewall firewall add rule name="VRCMomo LAN Bridge Discovery" dir=in action=allow protocol=UDP localport=38672 profile=private >nul

echo VRCMomo LAN Bridge is allowed on Private networks.
echo Keep the bridge window open, then use the phone to sync again.
pause
