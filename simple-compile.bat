@echo off
echo Compiling Simple Java Concurrency Demo (no external dependencies)...
echo.

mkdir build\classes 2>nul

echo Compiling model classes...
javac -d build\classes src\main\java\me\valizadeh\practices\model\*.java

if %ERRORLEVEL% neq 0 (
    echo ERROR: Model compilation failed
    exit /b 1
)

echo Compiling SimpleDemo...
javac -d build\classes -cp build\classes src\main\java\me\valizadeh\practices\SimpleDemo.java

if %ERRORLEVEL% neq 0 (
    echo ERROR: SimpleDemo compilation failed
    exit /b 1
)

echo.
echo ✅ Compilation successful!
echo To run the simple demo: java -cp build\classes me.valizadeh.practices.SimpleDemo
echo.
