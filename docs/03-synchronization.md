# Synchronization Primitives

## 🔒 **What is Synchronization?**

Synchronization is the coordination of multiple threads to:
- **Prevent race conditions** - Ensure thread-safe access to shared data
- **Maintain data consistency** - Keep shared state in a valid state
- **Control access** - Manage when and how threads access resources
- **Provide visibility** - Ensure changes are visible across threads

---

## 🛡️ **Types of Synchronization**

### **1. Intrinsic Locks (synchronized)**
- Built into every Java object
- Provides mutual exclusion and visibility
- Simple but can be inflexible

### **2. Explicit Locks (java.util.concurrent.locks)**
- More flexible than intrinsic locks
- Support timeouts, interruption, multiple conditions
- Better performance in some scenarios

### **3. Atomic Variables**
- Lock-free thread safety
- High performance for simple operations
- Built-in compare-and-swap operations

---

## 🔐 **Synchronized Keyword**

### **Synchronized Methods**

```java
public class Portfolio {
    private double cashBalance;
    private Map<String, Integer> stockPositions = new HashMap<>();
    
    // Method-level synchronization
    public synchronized void executeTrade(Trade trade) {
        logger.info("Executing trade: {}", trade.id());
        
        if (trade.type() == TradeType.BUY) {
            double cost = trade.price().doubleValue() * trade.quantity();
            
            if (cashBalance >= cost) {
                cashBalance -= cost;
                stockPositions.merge(trade.symbol(), trade.quantity(), Integer::sum);
                logger.info("BUY executed: {} shares of {} for ${}", 
                    trade.quantity(), trade.symbol(), cost);
            } else {
                logger.warn("Insufficient funds for trade: {}", trade.id());
            }
            
        } else { // SELL
            Integer currentPosition = stockPositions.get(trade.symbol());
            
            if (currentPosition != null && currentPosition >= trade.quantity()) {
                double proceeds = trade.price().doubleValue() * trade.quantity();
                cashBalance += proceeds;
                stockPositions.merge(trade.symbol(), -trade.quantity(), Integer::sum);
                logger.info("SELL executed: {} shares of {} for ${}", 
                    trade.quantity(), trade.symbol(), proceeds);
            } else {
                logger.warn("Insufficient shares for trade: {}", trade.id());
            }
        }
        
        logger.info("Portfolio after trade - Cash: ${}, Positions: {}", 
            cashBalance, stockPositions);
    }
    
    public synchronized double getCashBalance() {
        return cashBalance;
    }
    
    public synchronized Map<String, Integer> getPositions() {
        return new HashMap<>(stockPositions); // Return copy for thread safety
    }
}
```

### **Synchronized Blocks**

```java
public class Portfolio {
    private double cashBalance;
    private Map<String, Integer> stockPositions = new HashMap<>();
    private final Object cashLock = new Object();
    private final Object positionsLock = new Object();
    
    public void executeTrade(Trade trade) {
        if (trade.type() == TradeType.BUY) {
            double cost = trade.price().doubleValue() * trade.quantity();
            
            // Fine-grained locking - only lock cash when needed
            synchronized (cashLock) {
                if (cashBalance >= cost) {
                    cashBalance -= cost;
                } else {
                    logger.warn("Insufficient funds for trade: {}", trade.id());
                    return;
                }
            }
            
            // Separate lock for positions
            synchronized (positionsLock) {
                stockPositions.merge(trade.symbol(), trade.quantity(), Integer::sum);
            }
            
            logger.info("BUY executed: {} shares of {} for ${}", 
                trade.quantity(), trade.symbol(), cost);
        }
    }
    
    public double getCashBalance() {
        synchronized (cashLock) {
            return cashBalance;
        }
    }
}
```

---

## ⚡ **Volatile vs Synchronized**

### **Volatile - Visibility Only**

```java
public class MarketDataCache {
    private volatile boolean marketOpen = false;
    private volatile MarketData latestData;
    private volatile long lastUpdateTime;
    
    // Volatile ensures visibility but NOT atomicity
    public void updateMarketStatus(boolean isOpen) {
        marketOpen = isOpen; // ✅ Visible to all threads immediately
        lastUpdateTime = System.currentTimeMillis(); // ✅ Visible immediately
    }
    
    public boolean isMarketOpen() {
        return marketOpen; // ✅ Always reads latest value
    }
    
    // ❌ NOT thread-safe for complex operations
    public void incrementCounter() {
        // This is NOT atomic even with volatile!
        // counter++; // ← RACE CONDITION if multiple threads call this
    }
}
```

### **Synchronized - Atomicity + Visibility**

```java
public class ThreadSafeCounter {
    private int counter = 0; // Don't need volatile with synchronized
    
    // Synchronized ensures atomicity AND visibility
    public synchronized void increment() {
        counter++; // ✅ Atomic operation - no race condition
    }
    
    public synchronized int getValue() {
        return counter; // ✅ Guaranteed to see latest value
    }
    
    // Can also use synchronized blocks
    public void add(int value) {
        synchronized (this) {
            counter += value; // ✅ Atomic operation
        }
    }
}
```

---

## 🔧 **Explicit Locks (ReentrantLock)**

### **Basic ReentrantLock Usage**

```java
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class PortfolioWithLocks {
    private double cashBalance = 10000.0;
    private Map<String, Integer> stockPositions = new HashMap<>();
    private final Lock lock = new ReentrantLock();
    
    public void executeTrade(Trade trade) {
        // Try to acquire lock with timeout
        try {
            if (lock.tryLock(1, TimeUnit.SECONDS)) {
                try {
                    logger.info("Lock acquired for trade: {}", trade.id());
                    
                    if (trade.type() == TradeType.BUY) {
                        executeBuyOrder(trade);
                    } else {
                        executeSellOrder(trade);
                    }
                    
                } finally {
                    lock.unlock(); // ← ALWAYS unlock in finally block
                    logger.info("Lock released for trade: {}", trade.id());
                }
            } else {
                logger.warn("Could not acquire lock for trade: {}", trade.id());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Thread interrupted while waiting for lock");
        }
    }
    
    public boolean tryExecuteTradeImmediately(Trade trade) {
        // Non-blocking attempt to acquire lock
        if (lock.tryLock()) {
            try {
                logger.info("Immediate lock acquired for trade: {}", trade.id());
                executeBuyOrder(trade);
                return true;
            } finally {
                lock.unlock();
            }
        } else {
            logger.info("Lock not available for immediate execution: {}", trade.id());
            return false;
        }
    }
    
    private void executeBuyOrder(Trade trade) {
        double cost = trade.price().doubleValue() * trade.quantity();
        if (cashBalance >= cost) {
            cashBalance -= cost;
            stockPositions.merge(trade.symbol(), trade.quantity(), Integer::sum);
            logger.info("BUY executed: {} shares of {} for ${}", 
                trade.quantity(), trade.symbol(), cost);
        }
    }
    
    private void executeSellOrder(Trade trade) {
        Integer currentPosition = stockPositions.get(trade.symbol());
        if (currentPosition != null && currentPosition >= trade.quantity()) {
            double proceeds = trade.price().doubleValue() * trade.quantity();
            cashBalance += proceeds;
            stockPositions.merge(trade.symbol(), -trade.quantity(), Integer::sum);
            logger.info("SELL executed: {} shares of {} for ${}", 
                trade.quantity(), trade.symbol(), proceeds);
        }
    }
}
```

---

## 📖 **ReadWriteLock - Optimized for Read-Heavy Scenarios**

```java
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MarketDataService {
    private final Map<String, MarketData> marketDataCache = new HashMap<>();
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock = rwLock.readLock();
    private final Lock writeLock = rwLock.writeLock();
    
    // Multiple threads can read simultaneously
    public MarketData getMarketData(String symbol) {
        readLock.lock();
        try {
            MarketData data = marketDataCache.get(symbol);
            logger.info("Read market data for {} by thread {}", 
                symbol, Thread.currentThread().getName());
            return data;
        } finally {
            readLock.unlock();
        }
    }
    
    // Only one thread can write at a time  
    public void updateMarketData(String symbol, MarketData newData) {
        writeLock.lock();
        try {
            marketDataCache.put(symbol, newData);
            logger.info("Updated market data for {} to ${} by thread {}", 
                symbol, newData.lastPrice(), Thread.currentThread().getName());
            
            // Simulate processing time
            Thread.sleep(50);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            writeLock.unlock();
        }
    }
    
    // Read all data (multiple readers can do this simultaneously)
    public Map<String, MarketData> getAllMarketData() {
        readLock.lock();
        try {
            logger.info("Reading all market data by thread {}", 
                Thread.currentThread().getName());
            return new HashMap<>(marketDataCache); // Return copy
        } finally {
            readLock.unlock();
        }
    }
    
    // Bulk update (exclusive write access)
    public void bulkUpdateMarketData(Map<String, MarketData> updates) {
        writeLock.lock();
        try {
            logger.info("Bulk updating {} symbols by thread {}", 
                updates.size(), Thread.currentThread().getName());
            
            marketDataCache.putAll(updates);
            
            // Simulate bulk processing time
            Thread.sleep(200);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            writeLock.unlock();
        }
    }
}
```

---

## 🚀 **StampedLock - High Performance Lock**

```java
import java.util.concurrent.locks.StampedLock;

public class HighPerformanceMarketData {
    private final Map<String, MarketData> dataMap = new HashMap<>();
    private final StampedLock stampedLock = new StampedLock();
    
    // Optimistic read - fastest option
    public MarketData getMarketDataOptimistic(String symbol) {
        long stamp = stampedLock.tryOptimisticRead();
        
        // Read data optimistically (might be inconsistent)
        MarketData data = dataMap.get(symbol);
        
        // Check if data is still valid
        if (!stampedLock.validate(stamp)) {
            // Optimistic read failed, fall back to regular read lock
            stamp = stampedLock.readLock();
            try {
                data = dataMap.get(symbol);
                logger.info("Fallback to read lock for {}", symbol);
            } finally {
                stampedLock.unlockRead(stamp);
            }
        } else {
            logger.info("Optimistic read succeeded for {}", symbol);
        }
        
        return data;
    }
    
    // Regular read lock
    public MarketData getMarketDataWithReadLock(String symbol) {
        long stamp = stampedLock.readLock();
        try {
            return dataMap.get(symbol);
        } finally {
            stampedLock.unlockRead(stamp);
        }
    }
    
    // Write lock
    public void updateMarketData(String symbol, MarketData newData) {
        long stamp = stampedLock.writeLock();
        try {
            dataMap.put(symbol, newData);
            logger.info("Updated {} with write lock", symbol);
        } finally {
            stampedLock.unlockWrite(stamp);
        }
    }
    
    // Try to upgrade read lock to write lock
    public boolean tryUpdateMarketData(String symbol, MarketData newData) {
        long stamp = stampedLock.readLock();
        try {
            // Check if update is needed
            MarketData current = dataMap.get(symbol);
            if (current == null || !current.lastPrice().equals(newData.lastPrice())) {
                
                // Try to upgrade to write lock
                long writeStamp = stampedLock.tryConvertToWriteLock(stamp);
                if (writeStamp != 0L) {
                    // Successfully upgraded
                    stamp = writeStamp;
                    dataMap.put(symbol, newData);
                    logger.info("Successfully upgraded lock and updated {}", symbol);
                    return true;
                } else {
                    // Upgrade failed, release read lock and try write lock
                    stampedLock.unlockRead(stamp);
                    stamp = stampedLock.writeLock();
                    dataMap.put(symbol, newData);
                    logger.info("Lock upgrade failed, used write lock for {}", symbol);
                    return true;
                }
            }
            return false; // No update needed
        } finally {
            if (StampedLock.isWriteLockStamp(stamp)) {
                stampedLock.unlockWrite(stamp);
            } else {
                stampedLock.unlockRead(stamp);
            }
        }
    }
}
```

---

## 📊 **Lock Comparison**

| Lock Type | Pros | Cons | Best For |
|---|---|---|---|
| **synchronized** | ✅ Simple, JVM optimized | ❌ No timeout, not interruptible | Simple mutual exclusion |
| **ReentrantLock** | ✅ Timeout, interruptible, fair | ❌ More complex | When you need flexibility |
| **ReadWriteLock** | ✅ Multiple readers | ❌ Write starvation possible | Read-heavy workloads |
| **StampedLock** | ✅ Optimistic reads, best performance | ❌ Most complex | High-performance scenarios |

---

## ⚠️ **Synchronization Pitfalls**

### **❌ Deadlock Example**

```java
// ❌ DEADLOCK - Two threads can block each other forever
public class DeadlockExample {
    private final Object lock1 = new Object();
    private final Object lock2 = new Object();
    
    public void method1() {
        synchronized (lock1) {
            logger.info("Thread {} acquired lock1", Thread.currentThread().getName());
            
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            synchronized (lock2) {
                logger.info("Thread {} acquired lock2", Thread.currentThread().getName());
            }
        }
    }
    
    public void method2() {
        synchronized (lock2) { // ← Different order!
            logger.info("Thread {} acquired lock2", Thread.currentThread().getName());
            
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            synchronized (lock1) { // ← Different order!
                logger.info("Thread {} acquired lock1", Thread.currentThread().getName());
            }
        }
    }
}

// ✅ DEADLOCK PREVENTION - Always acquire locks in same order
public class DeadlockFreeExample {
    private final Object lock1 = new Object();
    private final Object lock2 = new Object();
    
    public void method1() {
        synchronized (lock1) { // ← Always lock1 first
            synchronized (lock2) { // ← Then lock2
                // Safe operations
            }
        }
    }
    
    public void method2() {
        synchronized (lock1) { // ← Always lock1 first
            synchronized (lock2) { // ← Then lock2
                // Safe operations
            }
        }
    }
}
```

### **❌ Forgotten unlock()**

```java
// ❌ WRONG - Lock might never be released
public void badLockUsage() {
    Lock lock = new ReentrantLock();
    
    lock.lock();
    
    if (someCondition) {
        return; // ← Lock never released!
    }
    
    // More code...
    lock.unlock(); // Might never reach here
}

// ✅ CORRECT - Always use try-finally
public void goodLockUsage() {
    Lock lock = new ReentrantLock();
    
    lock.lock();
    try {
        if (someCondition) {
            return; // ← Lock will be released in finally
        }
        // More code...
    } finally {
        lock.unlock(); // ← Always executes
    }
}
```

---

## 🎯 **Best Practices**

### **1. Use the right synchronization mechanism**
```java
// Simple flags → volatile
private volatile boolean shutdown = false;

// Simple operations → synchronized
public synchronized void increment() { counter++; }

// Complex operations with timeout → ReentrantLock
if (lock.tryLock(1, TimeUnit.SECONDS)) { /* ... */ }

// Read-heavy → ReadWriteLock
ReadWriteLock rwLock = new ReentrantReadWriteLock();

// High performance → StampedLock
StampedLock lock = new StampedLock();
```

### **2. Keep synchronized sections small**
```java
// ❌ BAD - Large synchronized section
public synchronized void processLargeTask() {
    doLotsOfWork();      // Holds lock too long
    doNetworkCall();     // Blocking operation while holding lock
    doMoreWork();
}

// ✅ GOOD - Minimal synchronized section
public void processLargeTaskEfficiently() {
    doLotsOfWork();      // No lock needed
    doNetworkCall();     // No lock needed
    
    synchronized (this) {
        updateSharedState(); // Only critical section is synchronized
    }
}
```

### **3. Avoid nested locks when possible**
```java
// ❌ RISKY - Nested locks can cause deadlock
synchronized (lock1) {
    synchronized (lock2) {
        // Complex operation
    }
}

// ✅ BETTER - Single lock or lock-free approach
// Use concurrent data structures instead
ConcurrentHashMap<String, Portfolio> portfolios = new ConcurrentHashMap<>();
```

---

## 🔑 **Key Takeaways**

1. **synchronized** - Simple, built-in, but inflexible
2. **volatile** - Visibility only, perfect for flags and references
3. **ReentrantLock** - Flexible with timeout and interruption support
4. **ReadWriteLock** - Optimized for read-heavy scenarios
5. **StampedLock** - Highest performance with optimistic reads
6. **Always use try-finally** with explicit locks
7. **Keep critical sections small** - Minimize lock holding time
8. **Consistent lock ordering** - Prevent deadlocks

## 📚 **Related Topics**
- [Java Memory Model](./01-java-memory-model.md)
- [Thread Coordination](./04-thread-coordination.md)
- [Concurrent Collections](./06-concurrent-collections.md)

---

Choose the right synchronization mechanism for your use case. Start simple with `synchronized`, then move to explicit locks when you need more control!
