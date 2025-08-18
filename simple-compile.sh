#!/bin/bash

echo "Compiling Simple Java Concurrency Demo (no external dependencies)..."
echo

# Create build directory
mkdir -p build/classes

echo "Compiling model classes..."
javac -d build/classes src/main/java/me/valizadeh/practices/model/*.java

if [ $? -ne 0 ]; then
    echo "ERROR: Model compilation failed"
    exit 1
fi

echo "Compiling SimpleDemo..."
javac -d build/classes -cp build/classes src/main/java/me/valizadeh/practices/SimpleDemo.java

if [ $? -ne 0 ]; then
    echo "ERROR: SimpleDemo compilation failed"
    exit 1
fi

echo
echo "✅ Compilation successful!"
echo "To run the simple demo: java -cp build/classes me.valizadeh.practices.SimpleDemo"
echo
