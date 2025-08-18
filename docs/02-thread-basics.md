# Thread Basics

## 🧵 **What are Threads?**

Threads are **lightweight units of execution** that allow programs to perform multiple tasks concurrently. In Java, threads enable:

- **Concurrent execution** - Multiple operations happening at the same time
- **Responsive applications** - UI remains responsive while background work happens
- **Better resource utilization** - Take advantage of multiple CPU cores
- **Asynchronous processing** - Handle I/O operations without blocking

---

## 🛠️ **Thread Creation Methods**

### **Method 1: Extending Thread Class**

```java
class OrderProcessorThread extends Thread {
    private final String processorName;
    
    public OrderProcessorThread(String name) {
        this.processorName = name;
    }
    
    @Override
    public void run() {
        logger.info("OrderProcessor {} starting in thread {}", 
            processorName, Thread.currentThread().getName());
        
        // Simulate order processing
        for (int i = 1; i <= 5; i++) {
            logger.info("OrderProcessor {}: Processing order #{}", processorName, i);
            
            try {
                Thread.sleep(500); // Simulate processing time
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.info("OrderProcessor {} was interrupted", processorName);
                return;
            }
        }
        
        logger.info("OrderProcessor {} completed all orders", processorName);
    }
}

// Usage
OrderProcessorThread processor1 = new OrderProcessorThread("Processor-1");
processor1.start(); // ← IMPORTANT: Use start(), not run()!
```

### **Method 2: Implementing Runnable**

```java
class OrderProcessorRunnable implements Runnable {
    private final String processorName;
    
    public OrderProcessorRunnable(String name) {
        this.processorName = name;
    }
    
    @Override
    public void run() {
        logger.info("Runnable OrderProcessor {} starting in thread {}", 
            processorName, Thread.currentThread().getName());
        
        // Process orders
        for (int i = 1; i <= 3; i++) {
            logger.info("Runnable OrderProcessor {}: Processing order #{}", processorName, i);
            
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.info("Runnable OrderProcessor {} was interrupted", processorName);
                return;
            }
        }
        
        logger.info("Runnable OrderProcessor {} completed", processorName);
    }
}

// Usage
OrderProcessorRunnable processor = new OrderProcessorRunnable("Processor-2");
Thread thread = new Thread(processor);
thread.start();
```

### **Method 3: Lambda Expressions (Most Common)**

```java
// Simple lambda
Thread thread = new Thread(() -> {
    logger.info("Lambda thread {} processing trade", Thread.currentThread().getName());
    
    try {
        // Simulate trade processing
        for (int i = 1; i <= 3; i++) {
            logger.info("Processing trade step {}", i);
            Thread.sleep(200);
        }
        logger.info("Trade processing completed");
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        logger.info("Trade processing was interrupted");
    }
});

thread.setName("TradeProcessor-Lambda");
thread.start();

// With parameters
String symbol = "AAPL";
int quantity = 100;

Thread marketDataProcessor = new Thread(() -> {
    logger.info("Processing market data for {} shares of {}", quantity, symbol);
    
    try {
        Thread.sleep(400);
        logger.info("Market data processing completed for {}", symbol);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}, "MarketData-" + symbol);

marketDataProcessor.start();
```

---

## 🔄 **Thread States**

Java threads go through different states during their lifecycle:

### **Thread State Diagram**
```
NEW → RUNNABLE → BLOCKED/WAITING/TIMED_WAITING → RUNNABLE → TERMINATED
```

### **Demonstrating Thread States**

```java
public void demonstrateThreadStates() throws InterruptedException {
    Thread orderThread = new Thread(() -> {
        logger.info("Order thread started, state: {}", Thread.currentThread().getState());
        
        try {
            // RUNNABLE state
            logger.info("Order thread about to sleep, state: {}", Thread.currentThread().getState());
            
            // TIMED_WAITING state
            Thread.sleep(2000);
            
            logger.info("Order thread finished sleeping, state: {}", Thread.currentThread().getState());
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.info("Order thread was interrupted");
        }
        
        logger.info("Order thread about to exit, state: {}", Thread.currentThread().getState());
    }, "OrderProcessor-States");
    
    // NEW state
    logger.info("Thread state after creation: {}", orderThread.getState()); // NEW
    
    orderThread.start();
    
    // RUNNABLE state (might be RUNNABLE or already TIMED_WAITING)
    logger.info("Thread state after start(): {}", orderThread.getState()); // RUNNABLE
    
    Thread.sleep(100); // Let thread start and begin sleeping
    
    // TIMED_WAITING state
    logger.info("Thread state while sleeping: {}", orderThread.getState()); // TIMED_WAITING
    
    orderThread.join(); // Wait for thread to complete
    
    // TERMINATED state
    logger.info("Thread state after join(): {}", orderThread.getState()); // TERMINATED
}
```

### **Thread States Explained**

| State | Description | How to Enter |
|---|---|---|
| **NEW** | Thread created but not started | `new Thread(...)` |
| **RUNNABLE** | Thread is executing or ready to execute | `thread.start()` |
| **BLOCKED** | Thread blocked waiting for monitor lock | Waiting for `synchronized` block |
| **WAITING** | Thread waiting indefinitely | `Object.wait()`, `Thread.join()` |
| **TIMED_WAITING** | Thread waiting for specified time | `Thread.sleep()`, `Object.wait(timeout)` |
| **TERMINATED** | Thread execution completed | `run()` method finished |

---

## ⚠️ **Thread Interruption**

### **Proper Interruption Handling**

```java
public void demonstrateThreadInterruption() throws InterruptedException {
    Thread longRunningTask = new Thread(() -> {
        logger.info("Long running task started");
        
        for (int i = 1; i <= 10; i++) {
            // Check for interruption
            if (Thread.currentThread().isInterrupted()) {
                logger.info("Task was interrupted at step {}", i);
                return; // Exit gracefully
            }
            
            logger.info("Task: Completed step {}", i);
            
            try {
                Thread.sleep(500); // Simulate work
            } catch (InterruptedException e) {
                // Important: Restore interrupted status
                Thread.currentThread().interrupt();
                logger.info("Task interrupted during sleep at step {}", i);
                return; // Exit gracefully
            }
        }
        
        logger.info("Long running task completed normally");
    }, "LongRunningTask");
    
    longRunningTask.start();
    
    // Let it run for a bit
    Thread.sleep(1500);
    
    // Interrupt the task
    logger.info("Main thread: Interrupting the long running task");
    longRunningTask.interrupt();
    
    // Wait for it to finish
    longRunningTask.join();
    logger.info("Main thread: Long running task has finished");
}
```

### **🔑 Interruption Best Practices**

1. **Check `isInterrupted()`** regularly in loops
2. **Handle `InterruptedException`** properly
3. **Restore interrupted status** with `Thread.currentThread().interrupt()`
4. **Exit gracefully** when interrupted
5. **Don't ignore interruption** - it's a cancellation request

---

## 🚨 **Common Thread Pitfalls**

### **❌ Pitfall 1: Calling run() instead of start()**

```java
// ❌ WRONG - This runs in the current thread, not a new thread!
Thread thread = new Thread(() -> {
    logger.info("This runs in thread: {}", Thread.currentThread().getName());
});
thread.run(); // ← WRONG! This is just a method call

// ✅ CORRECT - This creates and starts a new thread
Thread thread = new Thread(() -> {
    logger.info("This runs in thread: {}", Thread.currentThread().getName());
});
thread.start(); // ← CORRECT! This starts a new thread
```

### **❌ Pitfall 2: Not handling InterruptedException properly**

```java
// ❌ WRONG - Ignoring interruption
Thread badThread = new Thread(() -> {
    try {
        Thread.sleep(5000);
    } catch (InterruptedException e) {
        // DON'T DO THIS - Ignoring interruption!
        logger.info("Sleep was interrupted, but continuing anyway");
    }
    
    // This code continues even though thread was interrupted
    logger.info("Continuing execution despite interruption");
});

// ✅ CORRECT - Proper interruption handling
Thread goodThread = new Thread(() -> {
    try {
        Thread.sleep(5000);
    } catch (InterruptedException e) {
        // Restore interrupted status
        Thread.currentThread().interrupt();
        logger.info("Thread was interrupted, exiting gracefully");
        return; // Exit the thread
    }
    
    logger.info("Normal execution completed");
});
```

### **❌ Pitfall 3: Thread leaks - not joining threads**

```java
// ❌ WRONG - Main thread exits without waiting for worker threads
public void processOrders() {
    for (int i = 0; i < 5; i++) {
        Thread worker = new Thread(() -> {
            // Long running task
            processLargeOrderBatch();
        });
        worker.start();
        // NOT WAITING FOR THREADS TO COMPLETE!
    }
    // Main method exits, but worker threads are still running
}

// ✅ CORRECT - Wait for all threads to complete
public void processOrdersCorrectly() throws InterruptedException {
    List<Thread> workers = new ArrayList<>();
    
    for (int i = 0; i < 5; i++) {
        Thread worker = new Thread(() -> {
            processLargeOrderBatch();
        });
        workers.add(worker);
        worker.start();
    }
    
    // Wait for all workers to complete
    for (Thread worker : workers) {
        worker.join();
    }
    
    logger.info("All order processing completed");
}
```

### **❌ Pitfall 4: Shared mutable state without synchronization**

```java
// ❌ WRONG - Race condition!
private int orderCount = 0; // Shared mutable state

public void processOrdersConcurrently() {
    for (int i = 0; i < 10; i++) {
        new Thread(() -> {
            for (int j = 0; j < 100; j++) {
                orderCount++; // ← RACE CONDITION! Not thread-safe
            }
        }).start();
    }
}

// ✅ CORRECT - Use atomic operations or synchronization
private AtomicInteger safeOrderCount = new AtomicInteger(0);

public void processOrdersSafely() {
    for (int i = 0; i < 10; i++) {
        new Thread(() -> {
            for (int j = 0; j < 100; j++) {
                safeOrderCount.incrementAndGet(); // ← Thread-safe
            }
        }).start();
    }
}
```

---

## 🎯 **Best Practices**

### **1. Use meaningful thread names**
```java
Thread thread = new Thread(task, "OrderProcessor-" + orderId);
```

### **2. Handle interruption properly**
```java
try {
    Thread.sleep(1000);
} catch (InterruptedException e) {
    Thread.currentThread().interrupt(); // Restore status
    return; // Exit gracefully
}
```

### **3. Use thread-safe data structures**
```java
// Instead of ArrayList
List<Order> orders = new CopyOnWriteArrayList<>();

// Instead of HashMap  
Map<String, Portfolio> portfolios = new ConcurrentHashMap<>();
```

### **4. Consider using ExecutorService instead of raw threads**
```java
// Instead of manual thread creation
ExecutorService executor = Executors.newFixedThreadPool(4);
executor.submit(() -> processOrder());
executor.shutdown();
```

---

## 📊 **Thread vs Runnable Comparison**

| Aspect | Extending Thread | Implementing Runnable |
|---|---|---|
| **Inheritance** | ❌ Can't extend other classes | ✅ Can extend other classes |
| **Reusability** | ❌ Thread object not reusable | ✅ Runnable can be reused |
| **Separation of concerns** | ❌ Mixed thread control with task | ✅ Clean separation |
| **Flexibility** | ❌ Limited | ✅ Can use with ExecutorService |
| **Best practice** | ❌ Generally not recommended | ✅ Preferred approach |

---

## 🔑 **Key Takeaways**

1. **Use `start()`, never `run()`** - `run()` executes in current thread
2. **Implement `Runnable`** - Better than extending `Thread`
3. **Handle interruption properly** - Restore status and exit gracefully
4. **Use meaningful thread names** - Helps with debugging
5. **Understand thread states** - NEW → RUNNABLE → BLOCKED/WAITING → TERMINATED
6. **Join threads when needed** - Don't let threads run uncontrolled
7. **Avoid shared mutable state** - Use thread-safe alternatives

## 📚 **Related Topics**
- [Java Memory Model](./01-java-memory-model.md)
- [Synchronization Primitives](./03-synchronization.md)
- [Executors & Thread Pools](./05-executors-thread-pools.md)

---

Understanding thread basics is fundamental to Java concurrency. Master these concepts before moving to more advanced synchronization and coordination mechanisms!
