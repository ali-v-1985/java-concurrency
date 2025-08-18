@echo off
echo Testing Java Concurrency Trading System Compilation...
echo.

mkdir build\classes 2>nul

echo Compiling model classes...
javac -d build\classes -cp "src\main\java" src\main\java\me\valizadeh\practices\model\*.java

if %ERRORLEVEL% neq 0 (
    echo ERROR: Model compilation failed
    exit /b 1
)

echo Compiling main application...
javac -d build\classes -cp "src\main\java;build\classes" src\main\java\me\valizadeh\practices\TradingSystemDemo.java

if %ERRORLEVEL% neq 0 (
    echo ERROR: Main application compilation failed
    exit /b 1
)

echo.
echo ✅ Compilation successful!
echo To run the demo: java -cp build\classes me.valizadeh.practices.TradingSystemDemo
echo.
