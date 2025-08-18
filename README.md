# Java Concurrency Trading System

A comprehensive demonstration of Java concurrency concepts through realistic trading system scenarios. This project covers all major concurrency topics essential for senior-level interviews and building high-performance financial systems.

## 🎯 Project Overview

This project implements a complete trading system using Java 21, demonstrating every aspect of Java concurrency through practical, real-world scenarios. Each concept is illustrated with working code that you might encounter in actual trading, banking, or financial technology systems.

## 🚀 Features

### Core Concurrency Concepts Covered

1. **Java Memory Model (JMM)** - Market data visibility and happens-before relationships
2. **Thread Basics** - Order processing lifecycle and thread management
3. **Synchronization Primitives** - Portfolio management with synchronized, volatile, and locks
4. **Thread Coordination** - Trade settlement with CountDownLatch, CyclicBarrier, Semaphore, Phaser
5. **Executors & Thread Pools** - Order execution engines with different pool types
6. **Concurrent Collections** - Order books and trade history with thread-safe collections
7. **Futures & Async Programming** - Non-blocking trading operations with CompletableFuture
8. **Concurrency Patterns** - Producer-Consumer, Work Stealing, Actor Model, Immutable Objects
9. **Project Loom Virtual Threads** - High-frequency trading scalability with Java 21

### Trading System Components

- **Market Data Feed** - Real-time price updates with concurrent access
- **Order Management** - Thread-safe order processing and validation
- **Portfolio Management** - Concurrent position tracking and risk management
- **Trade Settlement** - Multi-phase settlement coordination
- **Risk Management** - Concurrent risk checks and limits
- **Order Book** - Thread-safe order matching and execution
- **High-Frequency Trading** - Massive scalability with virtual threads

## 🛠️ Requirements

- **Java 21** (with Project Loom support)
- **Gradle 8.5+**
- **8GB+ RAM** (recommended for virtual thread demonstrations)

## 🚀 Quick Start

### Option 1: Simple Demo (No external dependencies)

The simple demo runs with just the JDK and demonstrates core concepts:

```bash
# Compile the simple demo
javac -d build/classes -cp "src/main/java" src/main/java/me/valizadeh/practices/model/*.java src/main/java/me/valizadeh/practices/SimpleDemo.java

# Run the simple demo
java -cp build/classes me.valizadeh.practices.SimpleDemo
```

### Option 2: Full System (With Gradle)

The complete system with all features requires Gradle and dependencies:

```bash
# With Gradle installed
./gradlew run

# Interactive mode
./gradlew run --args="interactive"

# Build only
./gradlew build
```

### Option 3: Manual Compilation Test

```bash
# Windows
.\test-compile.bat

# This will show compilation status (note: logging dependencies required for full system)
```

## 📁 Project Structure

```
src/main/java/me/valizadeh/practices/
├── TradingSystemDemo.java              # Main application (full system)
├── SimpleDemo.java                     # Simple demo (no dependencies)
├── model/                              # Trading domain models
│   ├── Trade.java                      # Immutable trade record
│   ├── MarketData.java                 # Market data record
│   ├── Portfolio.java                  # Portfolio state
│   └── OrderBookEntry.java             # Order book entry
├── jmm/                               # Java Memory Model
│   └── MarketDataJMMDemo.java         # Visibility & happens-before
├── threads/                           # Thread Basics
│   └── OrderProcessorThreads.java     # Thread lifecycle & management
├── synchronization/                   # Synchronization Primitives
│   └── PortfolioSynchronization.java  # Locks, volatile, synchronized
├── coordination/                      # Thread Coordination
│   └── TradeSettlementCoordination.java # Coordination mechanisms
├── executors/                         # Executors & Thread Pools
│   └── OrderExecutionEngine.java      # Different executor types
├── collections/                       # Concurrent Collections
│   └── TradingConcurrentCollections.java # Thread-safe collections
├── async/                            # Async Programming
│   └── AsyncTradingOperations.java    # CompletableFuture patterns
├── patterns/                         # Concurrency Patterns
│   └── TradingConcurrencyPatterns.java # Design patterns
└── loom/                            # Project Loom
    └── HighFrequencyTradingLoom.java  # Virtual threads
```

## 🎮 Demo Modules

### 1. Java Memory Model (JMM)
- **Stale reads** - Market data visibility issues
- **Volatile semantics** - Ensuring visibility
- **Happens-before relationships** - Synchronization guarantees
- **Thread.join() semantics** - Coordination visibility

### 2. Thread Basics
- **Thread creation methods** - Different ways to create threads
- **Thread states** - Lifecycle demonstration
- **Thread interruption** - Proper cancellation handling
- **Common pitfalls** - What not to do

### 3. Synchronization Primitives
- **synchronized methods/blocks** - Mutual exclusion
- **volatile variables** - Visibility guarantees
- **ReentrantLock** - Advanced locking
- **ReadWriteLock** - Reader-writer scenarios
- **StampedLock** - Optimistic locking

### 4. Thread Coordination
- **CountDownLatch** - Waiting for task completion
- **CyclicBarrier** - Synchronizing at checkpoints
- **Semaphore** - Resource access control
- **Phaser** - Multi-phase coordination
- **wait/notify** - Legacy coordination

### 5. Executors & Thread Pools
- **FixedThreadPool** - Consistent capacity
- **CachedThreadPool** - Variable load
- **SingleThreadExecutor** - Sequential processing
- **ScheduledThreadPool** - Time-based execution
- **Custom ThreadPoolExecutor** - Advanced configuration
- **ForkJoinPool** - Work stealing

### 6. Concurrent Collections
- **ConcurrentHashMap** - Thread-safe maps
- **CopyOnWriteArrayList** - Read-optimized lists
- **BlockingQueue variants** - Producer-consumer queues
- **ConcurrentLinkedQueue** - Non-blocking queues
- **DelayQueue** - Time-delayed processing

### 7. Async Programming
- **CompletableFuture creation** - Async task creation
- **Chaining operations** - Pipeline building
- **Combining futures** - Parallel composition
- **Exception handling** - Error management
- **Timeout handling** - Time constraints

### 8. Concurrency Patterns
- **Producer-Consumer** - Order processing queues
- **Work Stealing** - Load balancing
- **Future/Promise** - Async result handling
- **Actor Model** - Message-based concurrency
- **Immutable Objects** - Thread safety by design
- **Thread Pool Pattern** - Custom implementations

### 9. Project Loom Virtual Threads
- **Basic virtual threads** - Lightweight thread creation
- **Massive scalability** - 10,000+ concurrent operations
- **Performance comparison** - Virtual vs platform threads
- **Structured concurrency** - Hierarchical task management
- **Market data streaming** - Real-time data processing
- **Connection handling** - Massive client simulation

## 🎯 Key Learning Outcomes

After running this demo, you'll understand:

- ✅ **Java Memory Model** - How threads see memory changes
- ✅ **Synchronization** - When and how to coordinate threads
- ✅ **Thread Safety** - Building correct concurrent code
- ✅ **Performance** - Choosing the right concurrency approach
- ✅ **Scalability** - Handling massive concurrent loads
- ✅ **Best Practices** - Industry-standard patterns
- ✅ **Modern Java** - Project Loom and virtual threads
- ✅ **Real-world Application** - Financial system scenarios

## 📊 Performance Insights

The demo includes performance comparisons showing:

- **Virtual Threads** vs **Platform Threads** - 3-5x improvement for I/O-intensive tasks
- **Different Lock Types** - Performance characteristics
- **Concurrent Collections** - Throughput under contention
- **Async vs Sync** - Latency and throughput differences

## 🔧 Configuration

The demo can be customized through:

- **Thread pool sizes** - Adjust for your system
- **Task counts** - Scale demonstrations up/down
- **Simulation delays** - Speed up/slow down operations
- **Logging levels** - Control output verbosity

## 🧪 Testing

```bash
# Run all tests
./gradlew test

# Run with verbose output
./gradlew test --info
```

## 📈 Production Considerations

This demo illustrates concepts you'll use in production systems:

- **Risk Management** - Concurrent position limits
- **Order Processing** - High-throughput order matching
- **Market Data** - Real-time price distribution
- **Settlement** - Multi-party coordination
- **Reporting** - Concurrent data aggregation
- **Client Connections** - Massive concurrent users

## 🤝 Contributing

This is an educational project demonstrating concurrency concepts. Feel free to:

- Add new trading scenarios
- Implement additional concurrency patterns
- Improve performance demonstrations
- Add more comprehensive examples

## 📚 Further Reading

- [Java Concurrency in Practice](https://www.oreilly.com/library/view/java-concurrency-in/0321349601/)
- [Project Loom Documentation](https://openjdk.org/projects/loom/)
- [Java Memory Model Specification](https://docs.oracle.com/javase/specs/jls/se21/html/jls-17.html)
- [JEP 425: Virtual Threads](https://openjdk.org/jeps/425)

## 📝 License

This project is for educational purposes. Use the concepts and patterns in your own projects.

---

**Built with Java 21 • Project Loom • Gradle • ❤️**
