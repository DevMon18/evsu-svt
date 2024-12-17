@echo off
echo ===================================
echo EVSU Violation Tracker Database Setup
echo ===================================

REM Check if XAMPP MySQL exists
set XAMPP_PATH=C:\xampp
set MYSQL_PATH="%XAMPP_PATH%\mysql\bin\mysql.exe"

if not exist %MYSQL_PATH% (
    echo Error: MySQL executable not found at %MYSQL_PATH%
    echo Please make sure XAMPP is installed correctly.
    goto :error
)

REM Check if MySQL service is running
netstat -an | find "3306" > nul
if errorlevel 1 (
    echo MySQL does not appear to be running.
    echo Please start MySQL in XAMPP Control Panel first.
    goto :error
)

echo MySQL service appears to be running...

REM Database credentials
set MYSQL_USER=root
set MYSQL_PASS=

echo.
echo Creating database and tables...
echo.

REM Execute the SQL script
%MYSQL_PATH% -u %MYSQL_USER% < database.sql
if errorlevel 1 (
    echo Failed to create database and tables.
    goto :error
)

echo.
echo =====================================
echo Database setup completed successfully
echo =====================================
echo.
echo Default admin credentials:
echo Username: admin
echo Password: Admin@123
echo.
echo IMPORTANT: Please make sure to change 
echo these credentials after your first login
echo for security purposes.
echo.
echo =====================================
goto :end

:error
echo.
echo =====================================
echo       Database Setup Failed
echo =====================================
echo.
echo Please check:
echo 1. XAMPP is installed at %XAMPP_PATH%
echo 2. MySQL service is running in XAMPP Control Panel
echo 3. MySQL user 'root' has no password set
echo 4. Port 3306 is not in use by another program
echo.

:end
pause