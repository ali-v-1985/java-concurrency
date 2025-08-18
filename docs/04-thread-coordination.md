# Thread Coordination

## 🤝 **What is Thread Coordination?**

Thread coordination involves **synchronizing the execution** of multiple threads to:
- **Wait for conditions** - Threads wait until certain conditions are met
- **Signal events** - Notify other threads when events occur
- **Control flow** - Coordinate the order of execution
- **Share resources** - Manage access to limited resources

---

## 🚦 **Coordination Mechanisms**

### **1. CountDownLatch - One-time Event**
- Threads wait until a countdown reaches zero
- Cannot be reset - one-time use
- Perfect for "wait for initialization" scenarios

### **2. CyclicBarrier - Recurring Synchronization Point**
- Threads wait until all reach a barrier
- Can be reused multiple times
- Perfect for "everyone finish phase before starting next"

### **3. Semaphore - Resource Pool**
- Controls access to a limited number of resources
- Acquire permits before using resources
- Perfect for connection pools, rate limiting

### **4. Phaser - Advanced Multi-Phase Coordination**
- Dynamic number of parties
- Multiple phases of execution
- Perfect for complex multi-stage operations

### **5. wait()/notify() - Classic Coordination**
- Built into Object class
- Producer-consumer patterns
- Custom condition waiting

---

## ⏳ **CountDownLatch - Wait for Initialization**

### **Trade Validation Scenario**

```java
public void demonstrateCountDownLatch() throws InterruptedException {
    logger.info("=== Demonstrating CountDownLatch ===");
    
    List<Trade> trades = generateTrades(5);
    CountDownLatch validationLatch = new CountDownLatch(trades.size());
    AtomicInteger validTrades = new AtomicInteger(0);
    
    // Start validation workers
    ExecutorService validators = Executors.newFixedThreadPool(3);
    
    for (Trade trade : trades) {
        validators.submit(() -> {
            try {
                logger.info("🔍 Validating trade: {}", trade.id());
                
                // Simulate validation time
                Thread.sleep(200 + (long) (Math.random() * 300));
                
                // Simulate validation result
                boolean isValid = Math.random() > 0.2; // 80% success rate
                
                if (isValid) {
                    validTrades.incrementAndGet();
                    logger.info("✅ Trade {} is valid", trade.id());
                } else {
                    logger.warn("❌ Trade {} failed validation", trade.id());
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Validation interrupted for trade: {}", trade.id());
            } finally {
                validationLatch.countDown(); // ← Always count down, even if failed
                logger.info("📉 Validation latch count: {}", validationLatch.getCount());
            }
        });
    }
    
    // Main thread waits for all validations to complete
    logger.info("⏳ Main thread waiting for all validations to complete...");
    validationLatch.await(); // ← Blocks until count reaches 0
    
    logger.info("🏁 All validations completed! Valid trades: {}/{}", 
        validTrades.get(), trades.size());
    
    validators.shutdown();
}
```

### **Market Data Initialization**

```java
public void demonstrateSystemInitialization() throws InterruptedException {
    logger.info("=== System Initialization with CountDownLatch ===");
    
    // Components that need to initialize
    CountDownLatch initializationLatch = new CountDownLatch(3);
    
    // Database connection
    Thread dbInitializer = new Thread(() -> {
        try {
            logger.info("🗄️ Initializing database connection...");
            Thread.sleep(1000); // Simulate DB connection time
            logger.info("✅ Database connection established");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            initializationLatch.countDown();
        }
    }, "DatabaseInitializer");
    
    // Market data feed
    Thread marketDataInitializer = new Thread(() -> {
        try {
            logger.info("📡 Initializing market data feed...");
            Thread.sleep(800); // Simulate market data connection
            logger.info("✅ Market data feed connected");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            initializationLatch.countDown();
        }
    }, "MarketDataInitializer");
    
    // Risk engine
    Thread riskEngineInitializer = new Thread(() -> {
        try {
            logger.info("⚖️ Initializing risk engine...");
            Thread.sleep(600); // Simulate risk engine startup
            logger.info("✅ Risk engine ready");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            initializationLatch.countDown();
        }
    }, "RiskEngineInitializer");
    
    // Start all initializers
    dbInitializer.start();
    marketDataInitializer.start();
    riskEngineInitializer.start();
    
    // Wait for all components to initialize
    logger.info("⏳ Waiting for system initialization...");
    initializationLatch.await();
    
    logger.info("🚀 System fully initialized! Ready to process trades.");
}
```

---

## 🔄 **CyclicBarrier - Synchronized Phases**

### **Multi-Phase Trade Settlement**

```java
public void demonstrateCyclicBarrier() throws InterruptedException, BrokenBarrierException {
    logger.info("=== Demonstrating CyclicBarrier ===");
    
    final int NUM_SETTLEMENT_WORKERS = 4;
    
    // Barrier that waits for all workers + barrier action
    CyclicBarrier settlementBarrier = new CyclicBarrier(NUM_SETTLEMENT_WORKERS, () -> {
        logger.info("🎯 Barrier reached! All workers completed settlement phase");
        logger.info("📊 Starting reconciliation phase...");
    });
    
    List<Trade> trades = generateTrades(12);
    ExecutorService settlementWorkers = Executors.newFixedThreadPool(NUM_SETTLEMENT_WORKERS);
    
    for (int workerId = 0; workerId < NUM_SETTLEMENT_WORKERS; workerId++) {
        final int id = workerId;
        
        settlementWorkers.submit(() -> {
            try {
                // Process trades in batches
                for (int phase = 0; phase < 3; phase++) {
                    logger.info("💼 Worker-{}: Starting settlement phase {}", id, phase + 1);
                    
                    // Process trades assigned to this worker
                    int startIndex = id * 3;
                    int endIndex = Math.min(startIndex + 3, trades.size());
                    
                    for (int i = startIndex; i < endIndex; i++) {
                        if (i < trades.size()) {
                            Trade trade = trades.get(i);
                            logger.info("💰 Worker-{}: Settling trade {} in phase {}", 
                                id, trade.id(), phase + 1);
                            
                            // Simulate settlement processing
                            Thread.sleep(100 + (long) (Math.random() * 200));
                        }
                    }
                    
                    logger.info("✅ Worker-{}: Completed phase {}, waiting for others...", 
                        id, phase + 1);
                    
                    // Wait for all workers to complete this phase
                    settlementBarrier.await(); // ← All workers must reach here before continuing
                    
                    logger.info("🔄 Worker-{}: All workers completed phase {}, continuing...", 
                        id, phase + 1);
                }
                
                logger.info("🏁 Worker-{}: All settlement phases completed", id);
                
            } catch (InterruptedException | BrokenBarrierException e) {
                Thread.currentThread().interrupt();
                logger.error("Worker-{} interrupted: {}", id, e.getMessage());
            }
        });
    }
    
    settlementWorkers.shutdown();
    settlementWorkers.awaitTermination(30, TimeUnit.SECONDS);
    
    logger.info("🎉 All settlement phases completed successfully!");
}
```

---

## 🎫 **Semaphore - Resource Pool Management**

### **Limited Market Data Connections**

```java
public void demonstrateSemaphore() throws InterruptedException {
    logger.info("=== Demonstrating Semaphore ===");
    
    // Only 3 concurrent market data connections allowed
    Semaphore marketDataConnections = new Semaphore(3);
    AtomicInteger activeConnections = new AtomicInteger(0);
    
    ExecutorService dataRequestors = Executors.newFixedThreadPool(8);
    
    // Simulate 8 concurrent requests for market data
    for (int i = 0; i < 8; i++) {
        final int requestId = i;
        
        dataRequestors.submit(() -> {
            try {
                logger.info("📡 Request-{}: Attempting to acquire market data connection...", requestId);
                
                // Try to acquire a permit (connection)
                marketDataConnections.acquire(); // ← Blocks if no permits available
                
                int active = activeConnections.incrementAndGet();
                logger.info("🔗 Request-{}: Acquired connection! Active connections: {}", 
                    requestId, active);
                
                // Simulate market data request
                Thread.sleep(500 + (long) (Math.random() * 1000));
                
                logger.info("📈 Request-{}: Market data received for symbols AAPL, GOOGL, TSLA", 
                    requestId);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Request-{} interrupted", requestId);
            } finally {
                // Always release the permit
                int active = activeConnections.decrementAndGet();
                marketDataConnections.release(); // ← Release permit back to pool
                logger.info("🔓 Request-{}: Released connection. Active connections: {}", 
                    requestId, active);
            }
        });
    }
    
    dataRequestors.shutdown();
    dataRequestors.awaitTermination(15, TimeUnit.SECONDS);
    
    logger.info("Available permits: {}", marketDataConnections.availablePermits());
}
```

### **Rate Limiting Example**

```java
public void demonstrateRateLimiting() throws InterruptedException {
    logger.info("=== Rate Limiting with Semaphore ===");
    
    // Allow maximum 2 API calls per second
    Semaphore rateLimiter = new Semaphore(2);
    
    // Background thread that replenishes permits every second
    ScheduledExecutorService permitReplenisher = Executors.newScheduledThreadPool(1);
    permitReplenisher.scheduleAtFixedRate(() -> {
        int releasedPermits = 2 - rateLimiter.availablePermits();
        if (releasedPermits > 0) {
            rateLimiter.release(releasedPermits);
            logger.info("🔄 Replenished {} permits. Available: {}", 
                releasedPermits, rateLimiter.availablePermits());
        }
    }, 1, 1, TimeUnit.SECONDS);
    
    ExecutorService apiCallers = Executors.newFixedThreadPool(5);
    
    // Make 10 API calls (should be rate limited)
    for (int i = 0; i < 10; i++) {
        final int callId = i;
        
        apiCallers.submit(() -> {
            try {
                logger.info("🌐 API Call-{}: Waiting for rate limit permit...", callId);
                rateLimiter.acquire();
                
                logger.info("📞 API Call-{}: Making external API call...", callId);
                Thread.sleep(100); // Simulate API call
                logger.info("✅ API Call-{}: Completed", callId);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
    
    Thread.sleep(8000); // Let it run for 8 seconds
    
    permitReplenisher.shutdown();
    apiCallers.shutdown();
}
```

---

## 🎭 **Phaser - Advanced Multi-Phase Operations**

### **Multi-Stage Order Processing**

```java
public void demonstratePhaser() throws InterruptedException {
    logger.info("=== Demonstrating Phaser ===");
    
    final int NUM_PARTICIPANTS = 3;
    Phaser settlementPhaser = new Phaser(NUM_PARTICIPANTS);
    
    List<Trade> trades = generateTrades(9);
    ExecutorService processors = Executors.newFixedThreadPool(NUM_PARTICIPANTS);
    
    for (int processorId = 0; processorId < NUM_PARTICIPANTS; processorId++) {
        final int id = processorId;
        
        processors.submit(() -> {
            try {
                // Phase 1: Risk Assessment
                logger.info("⚖️ Processor-{}: Phase 1 - Risk Assessment", id);
                processTradesForPhase(trades, id, NUM_PARTICIPANTS, "Risk Assessment");
                
                logger.info("✅ Processor-{}: Phase 1 complete, waiting for others...", id);
                settlementPhaser.arriveAndAwaitAdvance(); // ← Wait for all to complete Phase 1
                
                // Phase 2: Trade Validation
                logger.info("🔍 Processor-{}: Phase 2 - Trade Validation", id);
                processTradesForPhase(trades, id, NUM_PARTICIPANTS, "Trade Validation");
                
                logger.info("✅ Processor-{}: Phase 2 complete, waiting for others...", id);
                settlementPhaser.arriveAndAwaitAdvance(); // ← Wait for all to complete Phase 2
                
                // Phase 3: Settlement
                logger.info("💰 Processor-{}: Phase 3 - Settlement", id);
                processTradesForPhase(trades, id, NUM_PARTICIPANTS, "Settlement");
                
                logger.info("✅ Processor-{}: Phase 3 complete, waiting for others...", id);
                settlementPhaser.arriveAndAwaitAdvance(); // ← Wait for all to complete Phase 3
                
                logger.info("🏁 Processor-{}: All phases completed", id);
                
                // Deregister from phaser (no longer participating)
                settlementPhaser.arriveAndDeregister();
                
            } catch (Exception e) {
                logger.error("Processor-{} error: {}", id, e.getMessage());
            }
        });
    }
    
    processors.shutdown();
    processors.awaitTermination(20, TimeUnit.SECONDS);
    
    logger.info("Phaser final state - Phase: {}, Registered parties: {}", 
        settlementPhaser.getPhase(), settlementPhaser.getRegisteredParties());
}

private void processTradesForPhase(List<Trade> trades, int processorId, int totalProcessors, String phaseName) {
    int tradesPerProcessor = trades.size() / totalProcessors;
    int startIndex = processorId * tradesPerProcessor;
    int endIndex = (processorId == totalProcessors - 1) ? trades.size() : startIndex + tradesPerProcessor;
    
    for (int i = startIndex; i < endIndex; i++) {
        Trade trade = trades.get(i);
        logger.info("📋 Processor-{}: {} for trade {}", processorId, phaseName, trade.id());
        
        try {
            Thread.sleep(50 + (long) (Math.random() * 100)); // Simulate processing
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
    }
}
```

---

## 🔔 **wait() / notify() - Classic Producer-Consumer**

### **Trade Queue Management**

```java
public void demonstrateWaitNotify() throws InterruptedException {
    logger.info("=== Demonstrating wait/notify ===");
    
    final Object tradeQueue = new Object();
    final List<Trade> pendingTrades = new ArrayList<>();
    final int MAX_QUEUE_SIZE = 5;
    AtomicBoolean producerFinished = new AtomicBoolean(false);
    
    // Producer thread
    Thread producer = new Thread(() -> {
        try {
            for (int i = 0; i < 10; i++) {
                Trade trade = generateTrade(i);
                
                synchronized (tradeQueue) {
                    // Wait if queue is full
                    while (pendingTrades.size() >= MAX_QUEUE_SIZE) {
                        logger.info("📦 Producer: Queue full, waiting...");
                        tradeQueue.wait(); // ← Release lock and wait for notification
                    }
                    
                    pendingTrades.add(trade);
                    logger.info("📥 Producer: Added trade {} (queue size: {})", 
                        trade.id(), pendingTrades.size());
                    
                    tradeQueue.notify(); // ← Wake up waiting consumers
                }
                
                Thread.sleep(200); // Simulate production rate
            }
            
            producerFinished.set(true);
            
            synchronized (tradeQueue) {
                tradeQueue.notifyAll(); // ← Wake up all waiting consumers
            }
            
            logger.info("🏁 Producer finished");
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }, "TradeProducer");
    
    // Consumer threads
    Thread consumer1 = new Thread(() -> {
        consumeTrades(tradeQueue, pendingTrades, producerFinished, "Consumer-1");
    }, "TradeConsumer-1");
    
    Thread consumer2 = new Thread(() -> {
        consumeTrades(tradeQueue, pendingTrades, producerFinished, "Consumer-2");
    }, "TradeConsumer-2");
    
    // Start all threads
    producer.start();
    consumer1.start();
    consumer2.start();
    
    // Wait for completion
    producer.join();
    consumer1.join();
    consumer2.join();
    
    logger.info("All producer-consumer operations completed");
}

private void consumeTrades(Object tradeQueue, List<Trade> pendingTrades, 
                          AtomicBoolean producerFinished, String consumerName) {
    try {
        while (!producerFinished.get() || !pendingTrades.isEmpty()) {
            Trade trade = null;
            
            synchronized (tradeQueue) {
                // Wait while queue is empty and producer is still running
                while (pendingTrades.isEmpty() && !producerFinished.get()) {
                    logger.info("⏳ {}: Queue empty, waiting...", consumerName);
                    tradeQueue.wait(); // ← Release lock and wait for notification
                }
                
                if (!pendingTrades.isEmpty()) {
                    trade = pendingTrades.remove(0);
                    logger.info("📤 {}: Consumed trade {} (queue size: {})", 
                        consumerName, trade.id(), pendingTrades.size());
                    
                    tradeQueue.notify(); // ← Wake up waiting producer
                }
            }
            
            if (trade != null) {
                // Process trade outside of synchronized block
                Thread.sleep(300 + (long) (Math.random() * 200));
                logger.info("✅ {}: Processed trade {}", consumerName, trade.id());
            }
        }
        
        logger.info("🔚 {} finished", consumerName);
        
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}
```

---

## 📊 **Coordination Mechanisms Comparison**

| Mechanism | Use Case | Key Feature | Reusable |
|---|---|---|---|
| **CountDownLatch** | Wait for initialization/completion | One-time countdown | ❌ No |
| **CyclicBarrier** | Synchronized phases | Reusable barrier | ✅ Yes |
| **Semaphore** | Resource pool management | Permit-based access | ✅ Yes |
| **Phaser** | Complex multi-phase operations | Dynamic participants | ✅ Yes |
| **wait/notify** | Producer-consumer patterns | Built into Object | ✅ Yes |

---

## 🎯 **When to Use Each Mechanism**

### **CountDownLatch**
```java
// ✅ Perfect for:
// - System initialization
// - Waiting for multiple tasks to complete
// - One-time events

CountDownLatch startupLatch = new CountDownLatch(3);
// Database, cache, message queue initialization
```

### **CyclicBarrier**
```java
// ✅ Perfect for:
// - Multi-phase algorithms
// - Synchronized processing rounds
// - Parallel computations with phases

CyclicBarrier barrier = new CyclicBarrier(4, () -> {
    logger.info("All threads completed this round");
});
```

### **Semaphore**
```java
// ✅ Perfect for:
// - Connection pools
// - Rate limiting
// - Resource management

Semaphore connectionPool = new Semaphore(10);
```

### **Phaser**
```java
// ✅ Perfect for:
// - Dynamic number of participants
// - Complex multi-stage operations
// - Advanced coordination patterns

Phaser phaser = new Phaser(1); // Start with 1 party (main thread)
```

### **wait/notify**
```java
// ✅ Perfect for:
// - Custom producer-consumer
// - Complex condition waiting
// - When you need fine control

synchronized (condition) {
    while (!conditionMet) {
        condition.wait();
    }
}
```

---

## ⚠️ **Common Pitfalls**

### **❌ Deadlock with wait/notify**
```java
// ❌ WRONG - Potential missed notification
if (condition) {
    wait(); // Condition might change between check and wait
}

// ✅ CORRECT - Always use while loop
while (!condition) {
    wait(); // Check condition again after waking up
}
```

### **❌ Broken CyclicBarrier**
```java
try {
    barrier.await();
} catch (BrokenBarrierException e) {
    // ❌ WRONG - Ignoring broken barrier
    logger.error("Barrier broken, but continuing anyway");
}

// ✅ CORRECT - Handle broken barrier properly
try {
    barrier.await();
} catch (BrokenBarrierException e) {
    logger.error("Barrier broken, resetting...");
    barrier.reset(); // Reset if recoverable
    return; // Or exit gracefully
}
```

### **❌ Semaphore leak**
```java
semaphore.acquire();
// ❌ WRONG - Permit never released if exception occurs
doWork();
semaphore.release();

// ✅ CORRECT - Always use try-finally
semaphore.acquire();
try {
    doWork();
} finally {
    semaphore.release();
}
```

---

## 🔑 **Key Takeaways**

1. **CountDownLatch** - One-time events and initialization
2. **CyclicBarrier** - Synchronized phases and rounds
3. **Semaphore** - Resource pools and rate limiting
4. **Phaser** - Advanced multi-phase coordination
5. **wait/notify** - Custom condition waiting and producer-consumer
6. **Always handle interruption** - Use proper exception handling
7. **Choose the right tool** - Each mechanism has specific use cases

## 📚 **Related Topics**
- [Synchronization Primitives](./03-synchronization.md)
- [Executors & Thread Pools](./05-executors-thread-pools.md)
- [Concurrency Patterns](./08-concurrency-patterns.md)

---

Thread coordination is essential for building robust concurrent systems. Choose the right coordination mechanism based on your specific synchronization needs!
