# Java Memory Model (JMM)

## 🧠 **What is the Java Memory Model?**

The Java Memory Model (JMM) defines:
- **How threads interact** through memory
- **When writes become visible** to other threads  
- **What guarantees** Java provides for concurrent access
- **Ordering rules** for memory operations

### **🔍 Key Concepts:**

- **Visibility** - When changes made by one thread become visible to others
- **Ordering** - The sequence in which memory operations appear to execute
- **Happens-before** - Rules that guarantee memory visibility and ordering
- **Atomicity** - Operations that appear to happen instantaneously

---

## 🚨 **The Visibility Problem**

### **Stale Reads - The Classic Problem**

```java
private boolean marketOpen = false;
private MarketData latestData;

// Thread 1: Data updater
Thread writer = new Thread(() -> {
    try {
        Thread.sleep(100); // Simulate data loading
        latestData = MarketData.of("AAPL", new BigDecimal("150.00"), 
                                  new BigDecimal("150.00"), new BigDecimal("150.00"), 1000000L);
        marketOpen = true; // ← This write might not be visible to other threads!
        logger.info("Writer: Market opened with data: {}", latestData);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
});

// Thread 2: Data reader  
Thread reader = new Thread(() -> {
    int attempts = 0;
    while (!marketOpen && attempts < 10) { // ← Might never see marketOpen = true!
        attempts++;
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            break;
        }
    }
    
    if (marketOpen) {
        logger.info("Reader: Market is open! Latest data: {}", latestData);
    } else {
        logger.warn("Reader: Never saw market open - visibility issue!");
    }
});
```

**🚨 Problem:** The reader thread might never see `marketOpen = true` because:
- Each thread has its own **CPU cache**
- Writes might stay in cache without reaching main memory
- No guarantee when (or if) other threads will see the change

---

## ✅ **Solution 1: Volatile Variables**

### **Volatile Ensures Visibility**

```java
private volatile boolean marketOpenVolatile = false;
private volatile MarketData latestDataVolatile;

// Thread 1: Writer
Thread writer = new Thread(() -> {
    try {
        Thread.sleep(100);
        latestDataVolatile = MarketData.of("AAPL", new BigDecimal("150.00"), 
                                         new BigDecimal("150.00"), new BigDecimal("150.00"), 1000000L);
        marketOpenVolatile = true; // ← volatile write - immediately visible to all threads
        logger.info("Writer: Market opened (volatile) with data: {}", latestDataVolatile);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
});

// Thread 2: Reader
Thread reader = new Thread(() -> {
    int attempts = 0;
    while (!marketOpenVolatile && attempts < 10) { // ← volatile read - sees latest value
        attempts++;
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            break;
        }
    }
    
    logger.info("Reader: Market status (volatile): {}, Data: {}", 
        marketOpenVolatile, latestDataVolatile);
});
```

**✅ volatile guarantees:**
- **Visibility** - Writes are immediately visible to all threads
- **Ordering** - Prevents reordering around volatile operations  
- **No caching** - Reads/writes go directly to main memory

---

## 🔒 **Solution 2: Synchronized Blocks**

### **Synchronized Provides Mutual Exclusion + Visibility**

```java
private boolean marketOpenSync = false;
private MarketData latestDataSync;
private final Object lockObject = new Object();

// Thread 1: Writer
Thread writer = new Thread(() -> {
    try {
        Thread.sleep(100);
        
        synchronized (lockObject) { // ← Synchronized block
            latestDataSync = MarketData.of("AAPL", new BigDecimal("150.00"), 
                                         new BigDecimal("150.00"), new BigDecimal("150.00"), 1000000L);
            marketOpenSync = true; // ← Changes visible when lock is released
            logger.info("Writer: Market opened (synchronized) with data: {}", latestDataSync);
        }
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
});

// Thread 2: Reader
Thread reader = new Thread(() -> {
    boolean isOpen = false;
    MarketData data = null;
    
    for (int attempts = 0; attempts < 10 && !isOpen; attempts++) {
        synchronized (lockObject) { // ← Synchronized read
            isOpen = marketOpenSync;
            data = latestDataSync;
        }
        
        if (!isOpen) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    logger.info("Reader: Market status (synchronized): {}, Data: {}", isOpen, data);
});
```

**✅ synchronized provides:**
- **Mutual exclusion** - Only one thread can execute the block at a time
- **Visibility** - Changes made inside synchronized block are visible when lock is released
- **Ordering** - Operations inside synchronized blocks appear atomic

---

## 🔗 **Happens-Before Relationships**

### **Thread.join() Creates Happens-Before**

```java
private MarketData[] sharedData = new MarketData[1];

// Data loader thread
Thread dataLoader = new Thread(() -> {
    try {
        Thread.sleep(200); // Simulate data loading
        sharedData[0] = MarketData.of("GOOGL", new BigDecimal("120.00"), 
                                    new BigDecimal("120.00"), new BigDecimal("120.00"), 500000L);
        logger.info("Data loader: Loaded market data: {}", sharedData[0]);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
});

logger.info("Main thread: Starting data loader...");
dataLoader.start();

// join() creates happens-before relationship
dataLoader.join(); // ← join() happens-before subsequent reads

// This read is guaranteed to see the write from dataLoader thread
logger.info("Main thread: Data after join: {}", sharedData[0]);
```

**🔗 Happens-before rules guarantee:**
- **Program order** - Operations in a single thread happen in program order
- **Monitor lock** - Unlock happens-before subsequent lock on same monitor
- **Volatile** - Write to volatile happens-before subsequent read of same volatile
- **Thread start** - Thread.start() happens-before any action in started thread
- **Thread join** - Actions in thread happen-before Thread.join() returns
- **Transitivity** - If A happens-before B and B happens-before C, then A happens-before C

---

## 📊 **Volatile vs Synchronized Comparison**

| Aspect | volatile | synchronized |
|---|---|---|
| **Visibility** | ✅ Guaranteed | ✅ Guaranteed |
| **Atomicity** | ❌ No (except for reads/writes) | ✅ Yes (for entire block) |
| **Blocking** | ❌ Never blocks | ✅ Can block waiting for lock |
| **Performance** | 🚀 Very fast | 🐌 Slower (lock overhead) |
| **Use case** | Simple flags, references | Complex operations, multiple variables |

---

## 🎯 **When to Use Each Approach**

### **Use `volatile` when:**
```java
// Simple flags
private volatile boolean shutdownRequested = false;

// References to immutable objects
private volatile MarketData latestMarketData;

// Simple counters (with atomic operations)
private volatile long lastUpdateTimestamp;
```

### **Use `synchronized` when:**
```java
// Multiple related variables
private final Object lock = new Object();
private double totalValue;
private int shareCount;

public void updatePortfolio(double value, int shares) {
    synchronized (lock) {
        // Both variables updated atomically
        totalValue += value;
        shareCount += shares;
    }
}

// Complex operations
public synchronized void transferFunds(Account from, Account to, double amount) {
    from.withdraw(amount);  // These operations must be atomic together
    to.deposit(amount);
}
```

### **Use happens-before relationships when:**
```java
// Initialization patterns
Thread initializer = new Thread(() -> {
    // Initialize shared data structures
    initializeMarketData();
});

initializer.start();
initializer.join(); // ← Happens-before guarantee

// Now safe to use initialized data
processMarketData();
```

---

## 🔑 **Key Takeaways**

1. **Visibility is not guaranteed** by default between threads
2. **`volatile`** ensures visibility for single variables
3. **`synchronized`** provides both visibility and atomicity
4. **Happens-before relationships** create memory ordering guarantees
5. **Choose the right tool:**
   - `volatile` for simple flags and references
   - `synchronized` for complex operations
   - Happens-before for initialization patterns

## 📚 **Related Topics**
- [Thread Basics](./02-thread-basics.md)
- [Synchronization Primitives](./03-synchronization.md)
- [Concurrent Collections](./06-concurrent-collections.md)

---

The Java Memory Model is the foundation of all concurrent programming in Java. Understanding visibility, ordering, and happens-before relationships is crucial for writing correct concurrent code!
