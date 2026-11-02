@echo off
title 3D Relativistic Black Hole Simulation
cd /d "%~dp0blackhole-sim"
echo ========================================================
echo Launching 3D Relativistic Black Hole Simulation (LWJGL)
echo ========================================================
mvn exec:java
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Simulation exited with code %ERRORLEVEL%.
    pause
)
