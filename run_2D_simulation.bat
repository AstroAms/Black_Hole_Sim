@echo off
title 2D Relativistic Light Deflection Simulation
cd /d "%~dp0"
echo ========================================================
echo Launching 2D Relativistic Gravitational Lensing Simulation
echo ========================================================
java BlackHoleSimulation
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Simulation exited with code %ERRORLEVEL%.
    pause
)
