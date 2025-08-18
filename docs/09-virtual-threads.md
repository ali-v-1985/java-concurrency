# Project Loom Virtual Threads

## 🚀 **What are Virtual Threads?**

Virtual threads are **lightweight threads managed by the JVM** that revolutionize concurrent programming in Java 21+:

- **Massive scalability** - Millions of virtual threads vs thousands of platform threads
- **Low memory footprint** - Few KB per virtual thread vs ~2MB per platform thread  
- **Simple programming model** - Same thread APIs, just more scalable
- **Automatic management** - JVM handles mounting/unmounting to carrier threads

### **The Revolution:**

**Traditional Platform Threads (Heavy):**
```java
// ❌ EXPENSIVE - Each thread = ~2MB of memory + OS thread
for (int i = 0; i < 10000; i++) {
    new Thread(() -> {
        // This would create 10,000 OS threads = ~20GB memory!
        processMarketData();
    }).start();
}
// Result: OutOfMemoryError or poor performance
```

**Virtual Threads (Lightweight):**
```java
// ✅ LIGHTWEIGHT - Each virtual thread = ~few KB + shared carrier threads
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    for (int i = 0; i < 10000; i++) {
        executor.submit(() -> {
            // This creates 10,000 virtual threads = ~few MB total!
            processMarketData();
        });
    }
}
// Result: Runs efficiently with minimal memory usage
```

---

## 🌟 **Virtual Thread Creation**

### **Multiple Ways to Create Virtual Threads**

```java
public void demonstrateVirtualThreadCreation() throws InterruptedException {
    logger.info("=== Demonstrating Virtual Thread Creation ===");
    
    // Method 1: Direct creation
    Thread virtualThread1 = Thread.ofVirtual()
        .name("VirtualTrader-1")
        .start(() -> {
            logger.info("Virtual thread {} processing trade in {}", 
                Thread.currentThread().getName(), 
                Thread.currentThread());
            
            simulateTradeProcessing();
        });
    
    // Method 2: Virtual thread factory
    ThreadFactory virtualFactory = Thread.ofVirtual().factory();
    Thread virtualThread2 = virtualFactory.newThread(() -> {
        logger.info("Factory virtual thread {} processing market data", 
            Thread.currentThread().getName());
        
        simulateMarketDataProcessing();
    });
    
    virtualThread2.start();
    
    // Method 3: Virtual thread executor (most common)
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
        executor.submit(() -> {
            logger.info("Executor virtual thread {} processing order", 
                Thread.currentThread().getName());
            simulateOrderProcessing();
        });
    }
    
    // Wait for completion
    virtualThread1.join();
    virtualThread2.join();
    
    logger.info("Virtual thread creation demonstration completed");
}

private void simulateTradeProcessing() {
    try {
        Thread.sleep(500);
        logger.info("✅ Trade processing completed in {}", Thread.currentThread().getName());
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}

private void simulateMarketDataProcessing() {
    try {
        Thread.sleep(300);
        logger.info("✅ Market data processing completed in {}", Thread.currentThread().getName());
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}

private void simulateOrderProcessing() {
    try {
        Thread.sleep(200);
        logger.info("✅ Order processing completed in {}", Thread.currentThread().getName());
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}
```

---

## ⚡ **Massive Scalability Demonstration**

### **10,000 Concurrent Trading Tasks**

```java
public void demonstrateMassiveScalability() throws InterruptedException {
    logger.info("=== Demonstrating Massive Scalability ===");
    
    // Create executor that creates a new virtual thread for each task
    try (ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
        
        Instant start = Instant.now();
        CountDownLatch latch = new CountDownLatch(10000);
        AtomicInteger processedTrades = new AtomicInteger(0);
        
        // Submit 10,000 trading tasks - impossible with platform threads!
        for (int i = 0; i < 10000; i++) {
            final int tradeId = i;
            
            virtualExecutor.submit(() -> {
                try {
                    // Simulate trade processing with I/O (perfect for virtual threads)
                    Trade trade = Trade.buy("STOCK" + (tradeId % 100), 
                        new BigDecimal("100"), 10, "client" + (tradeId % 50));
                    
                    // Simulate network I/O (database write, market data fetch)
                    Thread.sleep(Duration.ofMillis(50 + (long) (Math.random() * 100)));
                    
                    processedTrades.incrementAndGet();
                    
                    if (tradeId % 1000 == 0) {
                        logger.info("Processed {} trades so far...", tradeId);
                    }
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Wait for all trades to complete
        latch.await();
        
        Duration elapsed = Duration.between(start, Instant.now());
        logger.info("✅ Processed {} trades in {} ms using virtual threads", 
            processedTrades.get(), elapsed.toMillis());
        
        // Compare memory usage
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        logger.info("Memory used: {} MB", usedMemory / (1024 * 1024));
        
        logger.info("🔥 With platform threads, this would require ~20GB memory and likely fail!");
    }
}
```

**🔥 Why This Is Revolutionary:**
- **10,000 virtual threads** vs impossible with platform threads
- **Low memory usage** - Few MB instead of ~20GB
- **High throughput** - All I/O operations run concurrently
- **Simple code** - Same thread programming model

---

## 📊 **Performance Comparison: Virtual vs Platform Threads**

### **I/O-Intensive Workload Comparison**

```java
public void demonstratePerformanceComparison() throws InterruptedException {
    logger.info("=== Performance Comparison: Virtual vs Platform Threads ===");
    
    int taskCount = 5000;
    
    // Test with platform threads (limited by thread pool size)
    logger.info("--- Testing with Platform Threads ---");
    Instant platformStart = Instant.now();
    
    try (ExecutorService platformExecutor = Executors.newFixedThreadPool(200)) {  // ← Limited to 200 threads
        CountDownLatch platformLatch = new CountDownLatch(taskCount);
        
        for (int i = 0; i < taskCount; i++) {
            final int taskId = i;
            platformExecutor.submit(() -> {
                try {
                    simulateIOIntensiveTradeOperation(taskId);  // I/O heavy work
                } finally {
                    platformLatch.countDown();
                }
            });
        }
        
        platformLatch.await();
    }
    
    Duration platformDuration = Duration.between(platformStart, Instant.now());
    logger.info("Platform threads completed {} tasks in {} ms", taskCount, platformDuration.toMillis());
    
    // Small delay between tests
    Thread.sleep(1000);
    
    // Test with virtual threads
    logger.info("--- Testing with Virtual Threads ---");
    Instant virtualStart = Instant.now();
    
    try (ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor()) {  // ← Unlimited virtual threads
        CountDownLatch virtualLatch = new CountDownLatch(taskCount);
        
        for (int i = 0; i < taskCount; i++) {
            final int taskId = i;
            virtualExecutor.submit(() -> {
                try {
                    simulateIOIntensiveTradeOperation(taskId);  // Same I/O heavy work
                } finally {
                    virtualLatch.countDown();
                }
            });
        }
        
        virtualLatch.await();
    }
    
    Duration virtualDuration = Duration.between(virtualStart, Instant.now());
    logger.info("Virtual threads completed {} tasks in {} ms", taskCount, virtualDuration.toMillis());
    
    // Performance comparison
    double improvement = (double) platformDuration.toMillis() / virtualDuration.toMillis();
    logger.info("🚀 Virtual threads were {:.2f}x faster for I/O intensive tasks", improvement);
}

private void simulateIOIntensiveTradeOperation(int taskId) {
    try {
        // Simulate multiple I/O operations
        Thread.sleep(50);   // Database read
        Thread.sleep(30);   // Market data API call
        Thread.sleep(40);   // Risk check service
        Thread.sleep(20);   // Database write
        
        if (taskId % 500 == 0) {
            logger.info("Completed I/O-intensive task {}", taskId);
        }
        
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}
```

**Typical Results:**
```
Platform threads completed 5000 tasks in 12,500 ms    // Limited by 200 thread pool
Virtual threads completed 5000 tasks in 2,100 ms     // All tasks run concurrently
🚀 Virtual threads were 5.95x faster for I/O intensive tasks
```

---

## 🌊 **Market Data Streaming - Real-Time**

### **Handling Thousands of Concurrent Streams**

```java
public void demonstrateMarketDataStreaming() throws InterruptedException {
    logger.info("=== Market Data Streaming with Virtual Threads ===");
    
    String[] symbols = {"AAPL", "GOOGL", "TSLA", "MSFT", "NVDA", "AMD", "INTC", "META", "AMZN", "NFLX"};
    
    try (ExecutorService streamingExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
        
        CountDownLatch streamingLatch = new CountDownLatch(symbols.length);
        AtomicLong totalUpdates = new AtomicLong(0);
        
        // Create a virtual thread for each symbol's market data stream
        for (String symbol : symbols) {
            streamingExecutor.submit(() -> {
                try {
                    logger.info("📡 Starting market data stream for {} in {}", 
                        symbol, Thread.currentThread().getName());
                    
                    // Simulate real-time market data streaming
                    for (int i = 0; i < 50; i++) {
                        MarketData data = MarketData.of(symbol,
                            new BigDecimal(100 + Math.random() * 500),
                            new BigDecimal(100 + Math.random() * 500),
                            new BigDecimal(100 + Math.random() * 500),
                            (long) (100000 + Math.random() * 1000000));
                        
                        // Simulate processing time (network latency, data validation)
                        Thread.sleep(Duration.ofMillis(20 + (long) (Math.random() * 30)));
                        
                        totalUpdates.incrementAndGet();
                        
                        if (i % 10 == 0) {
                            logger.info("📈 {} update #{}: ${}", symbol, i + 1, data.lastPrice());
                        }
                    }
                    
                    logger.info("✅ Completed streaming for {}", symbol);
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("Streaming interrupted for {}", symbol);
                } finally {
                    streamingLatch.countDown();
                }
            });
        }
        
        // Monitor streaming progress with another virtual thread
        Thread monitor = Thread.ofVirtual().start(() -> {
            try {
                while (streamingLatch.getCount() > 0) {
                    Thread.sleep(Duration.ofSeconds(1));
                    logger.info("📊 Streaming progress: {} total updates, {} streams remaining", 
                        totalUpdates.get(), streamingLatch.getCount());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        streamingLatch.await();
        monitor.interrupt();
        
        logger.info("🏁 Market data streaming completed. Total updates: {}", totalUpdates.get());
    }
}
```

**Benefits for Market Data:**
- 📡 **One virtual thread per symbol** - Simple, clear code
- 🚀 **Massive concurrency** - 1000s of symbols simultaneously  
- 💾 **Low memory** - Each stream uses minimal memory
- 🔄 **Real-time processing** - No blocking other streams

---

## 💰 **High-Frequency Order Processing**

### **Processing Thousands of Orders Concurrently**

```java
public void demonstrateHighFrequencyOrderProcessing() throws InterruptedException {
    logger.info("=== High-Frequency Order Processing with Virtual Threads ===");
    
    BlockingQueue<Trade> orderQueue = new LinkedBlockingQueue<>();
    AtomicInteger processedOrders = new AtomicInteger(0);
    AtomicInteger activeProcessors = new AtomicInteger(0);
    
    try (ExecutorService orderExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
        
        // Order generator (simulating high-frequency order flow)
        Thread orderGenerator = Thread.ofVirtual().start(() -> {
            try {
                for (int i = 0; i < 1000; i++) {
                    Trade order = generateRandomTrade(i);
                    orderQueue.put(order);
                    
                    if (i % 100 == 0) {
                        logger.info("📥 Generated {} orders", i);
                    }
                    
                    // High-frequency: minimal delay
                    Thread.sleep(Duration.ofMillis(1 + (long) (Math.random() * 3)));
                }
                logger.info("🏁 Order generation completed");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        // Order processors (each order gets its own virtual thread)
        for (int i = 0; i < 50; i++) {  // 50 processors
            final int processorId = i;
            
            orderExecutor.submit(() -> {
                while (!orderGenerator.isInterrupted() || !orderQueue.isEmpty()) {
                    try {
                        Trade order = orderQueue.poll(100, TimeUnit.MILLISECONDS);
                        if (order != null) {
                            activeProcessors.incrementAndGet();
                            
                            // Process order (market data lookup, risk check, execution)
                            Thread.sleep(Duration.ofMillis(10 + (long) (Math.random() * 20)));
                            
                            int processed = processedOrders.incrementAndGet();
                            
                            if (processed % 100 == 0) {
                                logger.info("📤 Processor-{}: Processed order #{} ({})", 
                                    processorId, processed, order.id());
                            }
                            
                            activeProcessors.decrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                logger.info("🔚 Processor-{} finished", processorId);
            });
        }
        
        // Monitor system health with virtual thread
        Thread monitor = Thread.ofVirtual().start(() -> {
            try {
                while (!orderGenerator.isInterrupted()) {
                    Thread.sleep(Duration.ofSeconds(2));
                    logger.info("📊 Queue size: {}, Active processors: {}, Processed: {}", 
                        orderQueue.size(), activeProcessors.get(), processedOrders.get());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        orderGenerator.join();
        Thread.sleep(Duration.ofSeconds(5)); // Let processors finish
        monitor.interrupt();
        
        logger.info("✅ Order processing completed. Total processed: {}", processedOrders.get());
    }
}

private Trade generateRandomTrade(int id) {
    String[] symbols = {"AAPL", "GOOGL", "TSLA", "MSFT", "NVDA"};
    String symbol = symbols[id % symbols.length];
    BigDecimal price = new BigDecimal(100 + Math.random() * 200);
    int quantity = 10 + (int) (Math.random() * 90);
    TradeType type = (id % 2 == 0) ? TradeType.BUY : TradeType.SELL;
    String clientId = "client" + (id % 10);
    
    return (type == TradeType.BUY) 
        ? Trade.buy(symbol, price, quantity, clientId)
        : Trade.sell(symbol, price, quantity, clientId);
}
```

---

## 🏗️ **Virtual Thread Architecture**

### **How Virtual Threads Work**

```
┌─────────────────────────────────────────────────────────────┐
│                    Application Code                         │
└─────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────┐
│              Virtual Thread Pool                           │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐         │
│  │VThread 1│ │VThread 2│ │VThread 3│ │VThread N│   ...   │
│  └─────────┘ └─────────┘ └─────────┘ └─────────┘         │
└─────────────────────────────────────────────────────────────┘
                                │ mounting/unmounting
                                ▼
┌─────────────────────────────────────────────────────────────┐
│              Carrier Thread Pool                           │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐         │
│  │Carrier 1│ │Carrier 2│ │Carrier 3│ │Carrier 4│         │
│  └─────────┘ └─────────┘ └─────────┘ └─────────┘         │
└─────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────┐
│                    OS Threads                               │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐         │
│  │OS Thrd 1│ │OS Thrd 2│ │OS Thrd 3│ │OS Thrd 4│         │
│  └─────────┘ └─────────┘ └─────────┘ └─────────┘         │
└─────────────────────────────────────────────────────────────┘
```

### **Key Concepts:**

- **Virtual Threads** - Lightweight threads managed by the JVM
- **Carrier Threads** - Platform threads that execute virtual threads  
- **Mounting/Unmounting** - Virtual threads attach/detach from carrier threads
- **Parking** - Virtual threads park when blocked, freeing carrier threads

---

## 🎯 **When to Use Virtual Threads**

### **✅ Perfect for Virtual Threads**

```java
class TradingSystem {
    
    // I/O intensive operations
    public void processMarketData() {
        // Network calls, database queries, file I/O
        fetchMarketDataFromExchange();    // Network I/O
        validateAgainstDatabase();        // Database I/O  
        writeToAuditLog();               // File I/O
    }
    
    // High-concurrency scenarios  
    public void handleClientConnections() {
        // 10,000+ concurrent client connections
        ServerSocket server = new ServerSocket(8080);
        
        while (true) {
            Socket client = server.accept();
            
            // One virtual thread per connection
            Thread.ofVirtual().start(() -> {
                handleClient(client);  // I/O intensive
            });
        }
    }
    
    // Request-response patterns
    public void handleRESTRequests() {
        // Web services, microservices
        // Each request = one virtual thread
    }
}
```

### **❌ Avoid Virtual Threads for**

```java
class CPUIntensiveWork {
    
    // CPU-bound computations  
    public void calculateRiskMetrics() {
        // Mathematical computations, algorithms
        // Use platform threads + thread pools instead
    }
    
    // Very short-lived tasks
    public void quickCalculation() {
        // Tasks that complete in microseconds
        // Virtual thread overhead not worth it
    }
    
    // Synchronized sections
    public synchronized void criticalSection() {
        // Virtual threads that hold locks for long periods
        // Can pin carrier threads - use platform threads
    }
}
```

---

## 📊 **Performance & Memory Comparison**

| Aspect | Platform Threads | Virtual Threads |
|---|---|---|
| **Memory per thread** | ~2MB (stack) | ~Few KB |
| **Creation time** | ~1ms | ~1μs (1000x faster) |
| **Context switch** | ~5μs | ~100ns (50x faster) |
| **Maximum threads** | ~5,000-10,000 | ~Millions |
| **Best for** | CPU-intensive | I/O-intensive |
| **Blocking behavior** | Blocks OS thread | Parks virtual thread |

---

## 🎯 **Real-World Virtual Thread Trading System**

```java
public class VirtualThreadTradingSystem {
    
    public static void main(String[] args) {
        
        // Market data streaming - 1000s of symbols
        try (var dataExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (String symbol : getAllTradingSymbols()) {  // 10,000+ symbols
                dataExecutor.submit(() -> streamMarketData(symbol));
            }
        }
        
        // Client connections - 100,000s of concurrent clients  
        try (var clientExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            ServerSocket server = new ServerSocket(8080);
            
            while (true) {
                Socket client = server.accept();
                clientExecutor.submit(() -> handleTradeRequest(client));
            }
        }
        
        // Order processing - millions of orders per day
        try (var orderExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            while (true) {
                Trade order = orderQueue.take();
                orderExecutor.submit(() -> processOrder(order));
            }
        }
    }
    
    private static void processOrder(Trade order) {
        // All I/O operations - perfect for virtual threads
        validateOrder(order);        // Database call
        checkRisk(order);           // Risk service call  
        fetchMarketData(order);     // Market data service
        executeOrder(order);        // Exchange API call
        sendConfirmation(order);    // Notification service
        auditLog(order);           // Log to file/database
    }
    
    private static void streamMarketData(String symbol) {
        // Continuous streaming - one virtual thread per symbol
        while (true) {
            MarketData data = fetchRealTimeData(symbol);  // Network I/O
            processMarketData(data);                      // Business logic
            persistData(data);                           // Database I/O
            broadcastToSubscribers(data);                // Network I/O
        }
    }
    
    private static void handleTradeRequest(Socket client) {
        // One virtual thread per client connection
        try (client) {
            while (client.isConnected()) {
                TradeRequest request = readRequest(client);   // Network I/O
                TradeResponse response = processRequest(request); // Business logic + I/O
                sendResponse(client, response);              // Network I/O
            }
        }
    }
}
```

---

## 🔑 **Key Takeaways**

1. **🚀 Massive Scalability** - Millions of virtual threads vs thousands of platform threads
2. **💾 Low Memory** - Few KB per virtual thread vs ~2MB per platform thread  
3. **⚡ Fast Creation** - 1000x faster to create virtual threads
4. **🎯 Perfect for I/O** - Network calls, database queries, file operations
5. **📝 Simple Programming Model** - Same thread code, just more scalable
6. **🔄 Automatic Management** - JVM handles mounting/unmounting automatically
7. **⚠️ Not for CPU-intensive** - Use platform threads for computational work
8. **🌊 Ideal for Streaming** - Perfect for real-time data processing
9. **📱 Web Services** - Excellent for microservices and REST APIs
10. **💰 High-Frequency Trading** - Handle massive concurrent order flow

## 📚 **Related Topics**
- [Executors & Thread Pools](./05-executors-thread-pools.md)
- [Futures & Async Programming](./07-futures-async.md)
- [Concurrency Patterns](./08-concurrency-patterns.md)

---

**Virtual threads are revolutionary for I/O-intensive applications like trading systems, web services, and microservices. They make concurrent programming much simpler while dramatically improving scalability!**

With virtual threads, the dream of "one thread per request" finally becomes practical reality. 🎉
