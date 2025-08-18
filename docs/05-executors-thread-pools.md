# Executors & Thread Pools

## 🏊 **What are Thread Pools?**

Thread pools are **collections of pre-created threads** that:
- **Reuse threads** - Avoid expensive thread creation/destruction
- **Control concurrency** - Limit the number of concurrent threads
- **Manage lifecycle** - Handle thread lifecycle automatically
- **Improve performance** - Reduce overhead and resource consumption

### **ExecutorService** is the main interface for thread pool management in Java.

---

## 🎯 **Benefits of Thread Pools**

| Without Thread Pools | With Thread Pools |
|---|---|
| ❌ Create new thread per task | ✅ Reuse existing threads |
| ❌ Expensive thread creation | ✅ Pre-created threads ready to work |
| ❌ Uncontrolled thread count | ✅ Limited, configurable thread count |
| ❌ Manual lifecycle management | ✅ Automatic lifecycle management |
| ❌ Poor resource utilization | ✅ Optimal resource utilization |

---

## 🏗️ **Types of Thread Pools**

### **1. FixedThreadPool - Fixed Number of Threads**

```java
public void demonstrateFixedThreadPool() throws InterruptedException {
    logger.info("=== Demonstrating FixedThreadPool ===");
    
    // Create fixed pool with 4 threads and custom thread factory
    ExecutorService fixedPool = Executors.newFixedThreadPool(4, new ThreadFactory() {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "OrderProcessor-" + threadNumber.getAndIncrement());
            t.setDaemon(false); // Ensure threads are not daemon threads
            return t;
        }
    });
    
    List<Future<String>> futures = new ArrayList<>();
    
    // Submit 10 order processing tasks
    for (int i = 1; i <= 10; i++) {
        final int orderId = i;
        
        Future<String> future = fixedPool.submit(() -> {
            String threadName = Thread.currentThread().getName();
            logger.info("📋 Processing order {} in thread {}", orderId, threadName);
            
            try {
                // Simulate order processing time
                Thread.sleep(500 + (long) (Math.random() * 1000));
                
                String result = String.format("Order %d processed by %s", orderId, threadName);
                logger.info("✅ {}", result);
                return result;
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "Order " + orderId + " interrupted";
            }
        });
        
        futures.add(future);
    }
    
    // Collect results
    for (Future<String> future : futures) {
        try {
            String result = future.get(); // Blocks until task completes
            logger.info("📄 Result: {}", result);
        } catch (Exception e) {
            logger.error("Error getting result: {}", e.getMessage());
        }
    }
    
    // Proper shutdown
    fixedPool.shutdown(); // No new tasks accepted
    
    if (!fixedPool.awaitTermination(10, TimeUnit.SECONDS)) {
        logger.warn("Pool did not terminate gracefully, forcing shutdown");
        fixedPool.shutdownNow(); // Force shutdown
    }
    
    logger.info("FixedThreadPool demonstration completed");
}
```

**✅ FixedThreadPool Characteristics:**
- **Fixed size** - Always maintains exact number of threads
- **Bounded queue** - Uses unbounded LinkedBlockingQueue
- **Predictable resource usage** - Known thread count
- **Good for** - CPU-intensive tasks with known load

---

### **2. CachedThreadPool - Dynamic Thread Creation**

```java
public void demonstrateCachedThreadPool() throws InterruptedException {
    logger.info("=== Demonstrating CachedThreadPool ===");
    
    // Creates threads as needed, reuses idle threads
    ExecutorService cachedPool = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "MarketData-" + System.currentTimeMillis());
        t.setDaemon(false);
        return t;
    });
    
    // Submit bursts of tasks to see dynamic thread creation
    for (int burst = 1; burst <= 3; burst++) {
        logger.info("🚀 Starting burst {} of market data requests", burst);
        
        List<Future<String>> burstFutures = new ArrayList<>();
        
        // Submit 5 tasks in quick succession
        for (int i = 1; i <= 5; i++) {
            final int requestId = i;
            final int burstId = burst;
            
            Future<String> future = cachedPool.submit(() -> {
                String threadName = Thread.currentThread().getName();
                logger.info("📡 Burst {}: Fetching market data {} in thread {}", 
                    burstId, requestId, threadName);
                
                try {
                    // Simulate I/O operation (perfect for cached pool)
                    Thread.sleep(200 + (long) (Math.random() * 300));
                    
                    String result = String.format("MarketData-%d-%d from %s", 
                        burstId, requestId, threadName);
                    logger.info("📈 {}", result);
                    return result;
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return "Request interrupted";
                }
            });
            
            burstFutures.add(future);
        }
        
        // Wait for burst to complete
        for (Future<String> future : burstFutures) {
            try {
                future.get();
            } catch (Exception e) {
                logger.error("Error in burst task: {}", e.getMessage());
            }
        }
        
        logger.info("✅ Burst {} completed", burst);
        
        // Pause between bursts to see thread reuse
        Thread.sleep(1000);
    }
    
    cachedPool.shutdown();
    cachedPool.awaitTermination(5, TimeUnit.SECONDS);
    
    logger.info("CachedThreadPool demonstration completed");
}
```

**✅ CachedThreadPool Characteristics:**
- **Dynamic sizing** - Creates threads as needed
- **Thread reuse** - Reuses threads idle for 60 seconds
- **No queue** - Uses SynchronousQueue (direct handoff)
- **Good for** - I/O-intensive tasks with variable load

---

### **3. SingleThreadExecutor - Sequential Processing**

```java
public void demonstrateSingleThreadExecutor() throws InterruptedException {
    logger.info("=== Demonstrating SingleThreadExecutor ===");
    
    // Guarantees sequential execution of tasks
    ExecutorService singlePool = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "AuditLogger");
        t.setDaemon(false);
        return t;
    });
    
    // Submit audit logging tasks that must be processed in order
    for (int i = 1; i <= 8; i++) {
        final int logId = i;
        
        singlePool.submit(() -> {
            String threadName = Thread.currentThread().getName();
            logger.info("📝 Audit log entry {} being written by {}", logId, threadName);
            
            try {
                // Simulate log writing time
                Thread.sleep(200);
                
                logger.info("✍️ Audit log entry {} completed", logId);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Audit log entry {} interrupted", logId);
            }
        });
    }
    
    logger.info("⏳ All audit tasks submitted, waiting for sequential completion...");
    
    singlePool.shutdown();
    singlePool.awaitTermination(10, TimeUnit.SECONDS);
    
    logger.info("SingleThreadExecutor demonstration completed");
}
```

**✅ SingleThreadExecutor Characteristics:**
- **Single thread** - All tasks execute in one thread
- **Sequential execution** - Tasks processed in order
- **Thread replacement** - Creates new thread if current one dies
- **Good for** - Audit logs, sequential processing, order-dependent tasks

---

### **4. ScheduledThreadPool - Time-Based Execution**

```java
public void demonstrateScheduledThreadPool() throws InterruptedException {
    logger.info("=== Demonstrating ScheduledThreadPool ===");
    
    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3, r -> {
        Thread t = new Thread(r, "Scheduler-" + System.currentTimeMillis() % 1000);
        t.setDaemon(false);
        return t;
    });
    
    // 1. One-time delayed execution
    logger.info("📅 Scheduling one-time market close check in 2 seconds...");
    ScheduledFuture<?> delayedTask = scheduler.schedule(() -> {
        logger.info("🔔 Market close check executed at {}", 
            new java.util.Date().toString());
    }, 2, TimeUnit.SECONDS);
    
    // 2. Fixed rate execution (every 1 second)
    logger.info("📅 Scheduling fixed-rate heartbeat every 1 second...");
    ScheduledFuture<?> fixedRateTask = scheduler.scheduleAtFixedRate(() -> {
        logger.info("💓 Heartbeat - {} - Thread: {}", 
            new java.util.Date().toString(), Thread.currentThread().getName());
    }, 1, 1, TimeUnit.SECONDS); // 1 second initial delay, then every 1 second
    
    // 3. Fixed delay execution (1 second after previous completion)
    logger.info("📅 Scheduling fixed-delay price updates with 300ms delay...");
    ScheduledFuture<?> fixedDelayTask = scheduler.scheduleWithFixedDelay(() -> {
        try {
            String threadName = Thread.currentThread().getName();
            logger.info("📊 Price update starting - Thread: {}", threadName);
            
            // Simulate variable processing time
            long processingTime = 100 + (long) (Math.random() * 200);
            Thread.sleep(processingTime);
            
            logger.info("📈 Price update completed in {}ms - Thread: {}", 
                processingTime, threadName);
                
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }, 500, 300, TimeUnit.MILLISECONDS); // 500ms initial delay, 300ms between completions
    
    // Let scheduled tasks run for 8 seconds
    Thread.sleep(8000);
    
    // Cancel periodic tasks
    logger.info("🛑 Cancelling periodic tasks...");
    fixedRateTask.cancel(false);
    fixedDelayTask.cancel(false);
    
    // Wait for delayed task to complete
    try {
        delayedTask.get(3, TimeUnit.SECONDS);
    } catch (Exception e) {
        logger.error("Error waiting for delayed task: {}", e.getMessage());
    }
    
    scheduler.shutdown();
    scheduler.awaitTermination(2, TimeUnit.SECONDS);
    
    logger.info("ScheduledThreadPool demonstration completed");
}
```

**✅ ScheduledThreadPool Characteristics:**
- **Time-based execution** - Delay and periodic scheduling
- **Multiple scheduling types** - One-time, fixed rate, fixed delay
- **Persistent scheduling** - Continues until cancelled
- **Good for** - Heartbeats, monitoring, periodic cleanup

---

### **5. Custom ThreadPoolExecutor - Full Control**

```java
public void demonstrateCustomThreadPool() throws InterruptedException {
    logger.info("=== Demonstrating Custom ThreadPoolExecutor ===");
    
    // Custom thread pool with detailed configuration
    ThreadPoolExecutor customPool = new ThreadPoolExecutor(
        2,                              // corePoolSize - minimum threads
        5,                              // maximumPoolSize - maximum threads  
        60L, TimeUnit.SECONDS,          // keepAliveTime - idle thread timeout
        new LinkedBlockingQueue<>(10),  // workQueue - bounded queue
        new ThreadFactory() {           // threadFactory - custom thread creation
            private final AtomicInteger threadNumber = new AtomicInteger(1);
            
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "CustomTrader-" + threadNumber.getAndIncrement());
                t.setUncaughtExceptionHandler((thread, ex) -> {
                    logger.error("Uncaught exception in thread {}: {}", 
                        thread.getName(), ex.getMessage());
                });
                return t;
            }
        },
        new ThreadPoolExecutor.CallerRunsPolicy() // rejectionHandler - what to do when full
    );
    
    // Monitor pool status
    Thread monitor = new Thread(() -> {
        try {
            while (!customPool.isShutdown()) {
                logger.info("📊 Pool status - Active: {}, Pool size: {}, Queue size: {}, Completed: {}", 
                    customPool.getActiveCount(),
                    customPool.getPoolSize(),
                    customPool.getQueue().size(),
                    customPool.getCompletedTaskCount());
                
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }, "PoolMonitor");
    
    monitor.start();
    
    // Submit many tasks to test pool behavior
    for (int i = 1; i <= 20; i++) {
        final int taskId = i;
        
        try {
            customPool.submit(() -> {
                String threadName = Thread.currentThread().getName();
                logger.info("🔄 Task {} starting in thread {}", taskId, threadName);
                
                try {
                    // Simulate work
                    Thread.sleep(500 + (long) (Math.random() * 1000));
                    logger.info("✅ Task {} completed in thread {}", taskId, threadName);
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("Task {} interrupted", taskId);
                }
            });
            
            // Add small delay to see queue building up
            Thread.sleep(50);
            
        } catch (RejectedExecutionException e) {
            logger.warn("❌ Task {} was rejected: {}", taskId, e.getMessage());
        }
    }
    
    logger.info("⏳ All tasks submitted, waiting for completion...");
    
    customPool.shutdown();
    if (!customPool.awaitTermination(30, TimeUnit.SECONDS)) {
        logger.warn("Custom pool did not terminate gracefully");
        customPool.shutdownNow();
    }
    
    monitor.interrupt();
    monitor.join();
    
    logger.info("Custom ThreadPoolExecutor demonstration completed");
}
```

**🔧 ThreadPoolExecutor Parameters:**
- **corePoolSize** - Minimum number of threads to keep alive
- **maximumPoolSize** - Maximum number of threads allowed
- **keepAliveTime** - How long excess threads wait before terminating
- **workQueue** - Queue to hold tasks before execution
- **threadFactory** - Factory to create new threads
- **rejectionHandler** - Policy for rejected tasks

---

### **6. ForkJoinPool - Work Stealing**

```java
public void demonstrateForkJoinPool() throws InterruptedException {
    logger.info("=== Demonstrating ForkJoinPool ===");
    
    // Create ForkJoinPool with work-stealing for parallel processing
    ForkJoinPool forkJoinPool = new ForkJoinPool(4);
    
    try {
        List<Trade> largeBatch = generateTrades(100);
        
        // Create a recursive task for parallel trade matching
        TradeMatchingTask matchingTask = new TradeMatchingTask(largeBatch);
        
        logger.info("🔄 Starting parallel trade matching with {} trades", largeBatch.size());
        
        // Execute the fork-join task
        Integer totalMatched = forkJoinPool.invoke(matchingTask);
        
        logger.info("✅ Parallel trade matching completed. Total matched: {}", totalMatched);
        
    } finally {
        forkJoinPool.shutdown();
        if (!forkJoinPool.awaitTermination(10, TimeUnit.SECONDS)) {
            forkJoinPool.shutdownNow();
        }
    }
}

/**
 * RecursiveTask for ForkJoinPool - implements divide-and-conquer
 */
private static class TradeMatchingTask extends RecursiveTask<Integer> {
    private static final int THRESHOLD = 10; // When to stop dividing
    private final List<Trade> trades;
    
    public TradeMatchingTask(List<Trade> trades) {
        this.trades = trades;
    }
    
    @Override
    protected Integer compute() {
        if (trades.size() <= THRESHOLD) {
            // Base case: process directly
            return matchTrades(trades);
        }
        
        // Divide: Split the work in half
        int mid = trades.size() / 2;
        TradeMatchingTask leftTask = new TradeMatchingTask(trades.subList(0, mid));
        TradeMatchingTask rightTask = new TradeMatchingTask(trades.subList(mid, trades.size()));
        
        // Conquer: Fork left task asynchronously
        leftTask.fork(); // ← Async execution (may be stolen by idle thread)
        int rightResult = rightTask.compute(); // ← Sync execution in current thread
        int leftResult = leftTask.join(); // ← Wait for async result
        
        // Combine: Merge results
        return leftResult + rightResult;
    }
    
    private int matchTrades(List<Trade> trades) {
        int matched = 0;
        for (Trade trade : trades) {
            try {
                Thread.sleep(10); // Simulate matching algorithm
                
                // Simulate matching logic (50% match rate)
                if (Math.random() > 0.5) {
                    matched++;
                    logger.info("🎯 Matched trade {} in thread {}", 
                        trade.id(), Thread.currentThread().getName());
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return matched;
    }
}
```

**🔀 ForkJoinPool Characteristics:**
- **Work stealing** - Idle threads steal work from busy threads
- **Divide and conquer** - Splits large tasks into smaller ones
- **Optimal CPU usage** - All cores stay busy
- **Good for** - Parallel algorithms, recursive tasks

---

## 🛠️ **Proper Shutdown Patterns**

### **Graceful Shutdown**

```java
public void demonstrateGracefulShutdown() throws InterruptedException {
    logger.info("=== Demonstrating Graceful Shutdown ===");
    
    ExecutorService executor = Executors.newFixedThreadPool(3);
    
    // Submit some long-running tasks
    for (int i = 1; i <= 5; i++) {
        final int taskId = i;
        executor.submit(() -> {
            try {
                logger.info("📋 Long task {} starting", taskId);
                Thread.sleep(3000); // Simulate long work
                logger.info("✅ Long task {} completed", taskId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.info("❌ Long task {} was interrupted", taskId);
            }
        });
    }
    
    logger.info("⏳ Initiated graceful shutdown...");
    
    // 1. Disable new task submissions
    executor.shutdown();
    
    try {
        // 2. Wait for existing tasks to complete
        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
            logger.warn("⚠️ Tasks did not complete within timeout, forcing shutdown");
            
            // 3. Cancel currently executing tasks
            List<Runnable> pendingTasks = executor.shutdownNow();
            logger.info("📋 Cancelled {} pending tasks", pendingTasks.size());
            
            // 4. Wait a bit more for tasks to respond to being cancelled
            if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                logger.error("❌ Pool did not terminate after forced shutdown");
            }
        } else {
            logger.info("✅ All tasks completed gracefully");
        }
    } catch (InterruptedException e) {
        // Re-cancel if current thread was interrupted
        executor.shutdownNow();
        Thread.currentThread().interrupt();
    }
    
    logger.info("Shutdown demonstration completed");
}
```

---

## 📊 **Thread Pool Comparison**

| Pool Type | Threads | Queue | Best For | Pros | Cons |
|---|---|---|---|---|---|
| **Fixed** | Fixed size | Unbounded | CPU tasks, predictable load | Stable resource usage | May queue many tasks |
| **Cached** | 0 to unlimited | Direct handoff | I/O tasks, variable load | Dynamic sizing | Can create too many threads |
| **Single** | 1 | Unbounded | Sequential processing | Order guaranteed | No parallelism |
| **Scheduled** | Fixed size | Delayed queue | Time-based tasks | Precise timing | Complex scheduling overhead |
| **Custom** | Configurable | Configurable | Specific requirements | Full control | Complex configuration |
| **ForkJoin** | CPU cores | Work stealing | Parallel algorithms | Optimal CPU usage | Complex to implement |

---

## 🎯 **Best Practices**

### **1. Choose the right pool type**
```java
// CPU-intensive work
ExecutorService cpuPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

// I/O-intensive work  
ExecutorService ioPool = Executors.newCachedThreadPool();

// Sequential processing
ExecutorService sequentialPool = Executors.newSingleThreadExecutor();

// Scheduled tasks
ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
```

### **2. Always shutdown executor services**
```java
try {
    // Use executor
    executor.shutdown();
    if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
        executor.shutdownNow();
    }
} catch (InterruptedException e) {
    executor.shutdownNow();
    Thread.currentThread().interrupt();
}
```

### **3. Handle task failures**
```java
Future<String> future = executor.submit(() -> {
    if (Math.random() < 0.3) {
        throw new RuntimeException("Simulated failure");
    }
    return "Success";
});

try {
    String result = future.get();
    logger.info("Task succeeded: {}", result);
} catch (ExecutionException e) {
    logger.error("Task failed: {}", e.getCause().getMessage());
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
}
```

### **4. Use custom thread factories for debugging**
```java
ThreadFactory factory = new ThreadFactory() {
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    
    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(r, "Trading-" + threadNumber.getAndIncrement());
        t.setUncaughtExceptionHandler((thread, ex) -> {
            logger.error("Uncaught exception in {}: {}", thread.getName(), ex.getMessage());
        });
        return t;
    }
};

ExecutorService executor = Executors.newFixedThreadPool(4, factory);
```

---

## ⚠️ **Common Pitfalls**

### **❌ Resource leaks**
```java
// ❌ WRONG - Never shutdown
ExecutorService executor = Executors.newFixedThreadPool(4);
executor.submit(() -> doWork());
// Application never terminates!

// ✅ CORRECT - Always shutdown
ExecutorService executor = Executors.newFixedThreadPool(4);
try {
    executor.submit(() -> doWork());
} finally {
    executor.shutdown();
}
```

### **❌ Unbounded task submission**
```java
// ❌ WRONG - Can lead to OutOfMemoryError
ExecutorService executor = Executors.newFixedThreadPool(4);
for (int i = 0; i < 1_000_000; i++) {
    executor.submit(() -> slowTask()); // Queue grows unbounded!
}

// ✅ CORRECT - Use bounded queue or throttling
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    4, 4, 0L, TimeUnit.MILLISECONDS,
    new ArrayBlockingQueue<>(100), // ← Bounded queue
    new ThreadPoolExecutor.CallerRunsPolicy() // ← Backpressure
);
```

### **❌ Blocking in thread pool threads**
```java
// ❌ WRONG - Blocking I/O in fixed thread pool
ExecutorService executor = Executors.newFixedThreadPool(4);
executor.submit(() -> {
    // This blocks a thread for potentially long time
    readFromDatabase(); // ← Can exhaust thread pool
});

// ✅ BETTER - Use cached pool for I/O or async I/O
ExecutorService ioExecutor = Executors.newCachedThreadPool();
```

---

## 🔑 **Key Takeaways**

1. **FixedThreadPool** - CPU-intensive tasks with predictable load
2. **CachedThreadPool** - I/O-intensive tasks with variable load  
3. **SingleThreadExecutor** - Sequential processing and ordering
4. **ScheduledThreadPool** - Time-based and periodic tasks
5. **Custom ThreadPoolExecutor** - Full control over behavior
6. **ForkJoinPool** - Parallel divide-and-conquer algorithms
7. **Always shutdown** - Prevent resource leaks
8. **Handle exceptions** - Don't let tasks fail silently

## 📚 **Related Topics**
- [Thread Basics](./02-thread-basics.md)
- [Thread Coordination](./04-thread-coordination.md)
- [Futures & Async Programming](./07-futures-async.md)

---

Thread pools are essential for building scalable, efficient concurrent applications. Choose the right pool type and always manage their lifecycle properly!
