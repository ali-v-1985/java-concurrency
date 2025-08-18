#!/bin/bash

echo "Testing Java Concurrency Trading System Compilation..."
echo

# Create build directory
mkdir -p build/classes

echo "Compiling model classes..."
javac -d build/classes -cp "src/main/java" src/main/java/me/valizadeh/practices/model/*.java

if [ $? -ne 0 ]; then
    echo "ERROR: Model compilation failed"
    exit 1
fi

echo "Compiling main application..."
javac -d build/classes -cp "src/main/java:build/classes" src/main/java/me/valizadeh/practices/TradingSystemDemo.java

if [ $? -ne 0 ]; then
    echo "ERROR: Main application compilation failed"
    exit 1
fi

echo
echo "✅ Compilation successful!"
echo "To run the demo: java -cp build/classes me.valizadeh.practices.TradingSystemDemo"
echo
