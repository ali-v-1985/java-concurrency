# Concurrency Patterns

## 🏗️ **What are Concurrency Patterns?**

Concurrency patterns are **proven solutions** to common problems in concurrent programming. They provide:

- 📐 **Structure** - Organized ways to coordinate threads
- 🛡️ **Safety** - Avoid race conditions and deadlocks
- 🚀 **Performance** - Efficient resource utilization
- 🔄 **Reusability** - Solutions that work across different scenarios

These patterns are the building blocks of robust concurrent systems!

---

## 🏭 **Producer-Consumer Pattern**

### **Problem:** You have threads that **generate work** (producers) and threads that **process work** (consumers). They need to coordinate without tight coupling.

### **Implementation with BlockingQueue**

```java
public void demonstrateProducerConsumer() throws InterruptedException {
    logger.info("=== Demonstrating Producer-Consumer Pattern ===");
    
    BlockingQueue<Trade> orderQueue = new LinkedBlockingQueue<>(10);
    AtomicInteger processedOrders = new AtomicInteger(0);
    AtomicBoolean producerFinished = new AtomicBoolean(false);
    
    // Producer - generates orders
    Thread orderProducer = new Thread(() -> {
        try {
            String[] symbols = {"AAPL", "GOOGL", "TSLA", "MSFT", "NVDA"};
            
            for (int i = 0; i < 15; i++) {
                String symbol = symbols[i % symbols.length];
                BigDecimal price = new BigDecimal(100 + Math.random() * 500);
                int quantity = (int) (10 + Math.random() * 100);
                TradeType type = (i % 2 == 0) ? TradeType.BUY : TradeType.SELL;
                
                Trade trade = (type == TradeType.BUY) 
                    ? Trade.buy(symbol, price, quantity, "client" + (i % 3))
                    : Trade.sell(symbol, price, quantity, "client" + (i % 3));
                
                orderQueue.put(trade); // ← Blocks if queue is full (backpressure)
                logger.info("📥 Producer: Generated order {}", trade.id());
                
                Thread.sleep(100); // Simulate order generation rate
            }
            
            producerFinished.set(true);
            logger.info("🏁 Producer finished generating orders");
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }, "OrderProducer");
    
    // Multiple consumers - process orders
    ExecutorService consumers = Executors.newFixedThreadPool(3);
    
    for (int i = 0; i < 3; i++) {
        final int consumerId = i;
        consumers.submit(() -> {
            try {
                while (!producerFinished.get() || !orderQueue.isEmpty()) {
                    Trade trade = orderQueue.poll(500, TimeUnit.MILLISECONDS);
                    if (trade != null) {
                        logger.info("📤 Consumer-{}: Processing order {}", consumerId, trade.id());
                        
                        // Simulate order processing
                        Thread.sleep(200 + (int)(Math.random() * 300));
                        
                        processedOrders.incrementAndGet();
                        logger.info("✅ Consumer-{}: Completed order {}", consumerId, trade.id());
                    }
                }
                logger.info("🔚 Consumer-{} finished", consumerId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
    
    // Start processing
    orderProducer.start();
    
    // Wait for completion
    orderProducer.join();
    consumers.shutdown();
    consumers.awaitTermination(10, TimeUnit.SECONDS);
    
    logger.info("📊 Total orders processed: {}", processedOrders.get());
}
```

**🎯 Producer-Consumer Benefits:**
- 🔄 **Decoupling** - Producers and consumers don't know about each other
- 🚦 **Flow control** - Queue provides automatic backpressure
- 📈 **Scalability** - Easy to add more producers or consumers
- 🛡️ **Thread safety** - BlockingQueue handles synchronization

---

## 🔄 **Work Stealing Pattern**

### **Problem:** Distribute work efficiently among threads, allowing idle threads to "steal" work from busy threads.

### **Implementation with ForkJoinPool**

```java
public void demonstrateWorkStealing() throws InterruptedException {
    logger.info("=== Demonstrating Work Stealing Pattern ===");
    
    // Create ForkJoinPool for work stealing
    ForkJoinPool workStealingPool = new ForkJoinPool(4);
    
    try {
        List<Trade> largeBatch = generateTrades(20);
        
        // Create a work-stealing task
        TradeProcessingTask task = new TradeProcessingTask(largeBatch);
        Integer totalProcessed = workStealingPool.invoke(task);
        
        logger.info("Work stealing completed. Total trades processed: {}", totalProcessed);
        
    } finally {
        workStealingPool.shutdown();
        if (!workStealingPool.awaitTermination(5, TimeUnit.SECONDS)) {
            workStealingPool.shutdownNow();
        }
    }
}

/**
 * RecursiveTask for ForkJoinPool - implements divide-and-conquer
 */
private static class TradeProcessingTask extends RecursiveTask<Integer> {
    private static final int THRESHOLD = 3;  // ← When to stop dividing
    private final List<Trade> trades;
    
    public TradeProcessingTask(List<Trade> trades) {
        this.trades = trades;
    }
    
    @Override
    protected Integer compute() {
        if (trades.size() <= THRESHOLD) {
            // Base case: process directly
            return processTrades(trades);
        }
        
        // Divide: Split the work in half
        int mid = trades.size() / 2;
        TradeProcessingTask leftTask = new TradeProcessingTask(trades.subList(0, mid));
        TradeProcessingTask rightTask = new TradeProcessingTask(trades.subList(mid, trades.size()));
        
        // Conquer: Fork left task asynchronously
        leftTask.fork();                    // ← Async execution (may be stolen by idle thread)
        int rightResult = rightTask.compute(); // ← Sync execution in current thread
        int leftResult = leftTask.join();      // ← Wait for async result
        
        // Combine: Merge results
        return leftResult + rightResult;
    }
    
    private int processTrades(List<Trade> trades) {
        for (Trade trade : trades) {
            try {
                Thread.sleep(100); // Simulate processing
                logger.info("🔄 Processed trade {} in thread {}", 
                    trade.id(), Thread.currentThread().getName());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return trades.size();
    }
}
```

**⚖️ Work Stealing Benefits:**
- ⚖️ **Load balancing** - Idle threads steal work from busy threads
- 🚀 **CPU utilization** - All cores stay busy
- 📈 **Scalability** - Adapts to available processors
- 🔄 **Self-organizing** - No manual work distribution needed

---

## 🎭 **Actor Model Pattern**

### **Problem:** Coordinate state changes across multiple components without shared mutable state and complex locking.

### **Simple Actor Implementation**

```java
public void demonstrateActorModel() throws InterruptedException {
    logger.info("=== Demonstrating Actor Model ===");
    
    // Create actors
    PortfolioActor portfolioActor = new PortfolioActor("PortfolioActor");
    RiskManagerActor riskActor = new RiskManagerActor("RiskManagerActor");
    OrderProcessorActor orderActor = new OrderProcessorActor("OrderProcessor", portfolioActor, riskActor);
    
    // Start actors
    portfolioActor.start();
    riskActor.start();
    orderActor.start();
    
    // Send messages to actors
    List<Trade> trades = generateTrades(8);
    
    for (Trade trade : trades) {
        orderActor.tell(new ProcessOrderMessage(trade));
        Thread.sleep(200);
    }
    
    // Give actors time to process
    Thread.sleep(3000);
    
    // Query portfolio state
    orderActor.tell(new QueryPortfolioMessage("client1"));
    
    Thread.sleep(1000);
    
    // Stop actors
    portfolioActor.stop();
    riskActor.stop();
    orderActor.stop();
    
    logger.info("Actor model demonstration completed");
}

/**
 * Actor base class - encapsulates state and behavior
 */
private abstract static class Actor {
    private final String name;
    private final BlockingQueue<Object> mailbox = new LinkedBlockingQueue<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread actorThread;
    
    public Actor(String name) {
        this.name = name;
    }
    
    public void start() {
        if (running.compareAndSet(false, true)) {
            actorThread = new Thread(this::run, name);
            actorThread.start();
            logger.info("🎭 Actor {} started", name);
        }
    }
    
    public void stop() {
        running.set(false);
        if (actorThread != null) {
            actorThread.interrupt();
            try {
                actorThread.join(1000);
                logger.info("🛑 Actor {} stopped", name);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    public void tell(Object message) {  // ← Send message to actor
        if (running.get()) {
            mailbox.offer(message);
        }
    }
    
    private void run() {
        while (running.get()) {
            try {
                Object message = mailbox.poll(100, TimeUnit.MILLISECONDS);
                if (message != null) {
                    receive(message);  // ← Process message
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    protected abstract void receive(Object message);  // ← Implement in subclass
}

/**
 * Order processing actor
 */
private static class OrderProcessorActor extends Actor {
    private final PortfolioActor portfolioActor;
    private final RiskManagerActor riskActor;
    
    public OrderProcessorActor(String name, PortfolioActor portfolioActor, RiskManagerActor riskActor) {
        super(name);
        this.portfolioActor = portfolioActor;
        this.riskActor = riskActor;
    }
    
    @Override
    protected void receive(Object message) {
        if (message instanceof ProcessOrderMessage processMsg) {
            Trade trade = processMsg.trade();
            logger.info("📋 OrderProcessor: Processing trade {}", trade.id());
            
            // Send risk check
            riskActor.tell(new CheckRiskMessage(trade));
            
            // Update portfolio (simplified)
            portfolioActor.tell(new UpdatePositionMessage(trade.symbol(), 
                trade.type() == TradeType.BUY ? trade.quantity() : -trade.quantity()));
            
        } else if (message instanceof QueryPortfolioMessage queryMsg) {
            logger.info("📊 OrderProcessor: Querying portfolio for {}", queryMsg.clientId());
            // In real implementation, would query portfolio actor and return result
        }
    }
}

/**
 * Portfolio management actor
 */
private static class PortfolioActor extends Actor {
    private final Map<String, Integer> positions = new ConcurrentHashMap<>();
    
    public PortfolioActor(String name) {
        super(name);
    }
    
    @Override
    protected void receive(Object message) {
        if (message instanceof UpdatePositionMessage updateMsg) {
            String symbol = updateMsg.symbol();
            int quantity = updateMsg.quantity();
            
            positions.merge(symbol, quantity, Integer::sum);
            logger.info("💼 Portfolio: Updated position {} = {}", symbol, positions.get(symbol));
        }
    }
}

/**
 * Risk management actor
 */
private static class RiskManagerActor extends Actor {
    public RiskManagerActor(String name) {
        super(name);
    }
    
    @Override
    protected void receive(Object message) {
        if (message instanceof CheckRiskMessage riskMsg) {
            Trade trade = riskMsg.trade();
            logger.info("⚖️ RiskManager: Checking risk for trade {}", trade.id());
            
            // Simulate risk calculation
            try {
                Thread.sleep(100);
                boolean approved = Math.random() > 0.1; // 90% approval rate
                logger.info("✅ RiskManager: Trade {} {}", trade.id(), 
                    approved ? "APPROVED" : "REJECTED");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}

// Message classes
record ProcessOrderMessage(Trade trade) {}
record CheckRiskMessage(Trade trade) {}
record UpdatePositionMessage(String symbol, int quantity) {}
record QueryPortfolioMessage(String clientId) {}
```

**🎭 Actor Model Benefits:**
- 🚫 **No shared state** - Each actor has its own private state
- 📨 **Message passing** - Communication only through messages
- 🔒 **Thread safety** - Only one message processed at a time per actor
- 📈 **Scalability** - Easy to distribute across machines

---

## 🔒 **Immutable Objects Pattern**

### **Problem:** Ensure thread safety without synchronization by making objects unchangeable after creation.

### **Copy-on-Write Implementation**

```java
public void demonstrateImmutableObjects() throws InterruptedException {
    logger.info("=== Demonstrating Immutable Objects Pattern ===");
    
    // Immutable market data that can be safely shared across threads
    ImmutableMarketDataFeed dataFeed = new ImmutableMarketDataFeed();
    
    ExecutorService readers = Executors.newFixedThreadPool(4);
    
    // Multiple threads reading market data safely
    for (int i = 0; i < 4; i++) {
        final int readerId = i;
        readers.submit(() -> {
            try {
                for (int j = 0; j < 5; j++) {
                    MarketData data = dataFeed.getLatestData("AAPL");  // ← No locking needed!
                    
                    if (data != null) {
                        logger.info("📊 Reader-{}: AAPL data - Price: ${}, Volume: {}", 
                            readerId, data.lastPrice(), data.volume());
                    } else {
                        logger.info("📊 Reader-{}: No AAPL data available", readerId);
                    }
                    
                    Thread.sleep(200);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
    
    // Single writer updating
    Thread writer = new Thread(() -> {
        try {
            for (int i = 0; i < 8; i++) {
                BigDecimal newPrice = new BigDecimal(150 + Math.random() * 20);
                long newVolume = (long) (1000000 + Math.random() * 500000);
                
                dataFeed.updateMarketData("AAPL", newPrice, newVolume);
                logger.info("✍️ Writer: Updated AAPL - Price: ${}, Volume: {}", newPrice, newVolume);
                
                Thread.sleep(300);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }, "MarketDataWriter");
    
    writer.start();
    
    Thread.sleep(4000); // Let readers and writer work
    
    writer.interrupt();
    readers.shutdown();
    readers.awaitTermination(2, TimeUnit.SECONDS);
    
    logger.info("Immutable objects demonstration completed");
}

/**
 * Immutable Market Data Feed using copy-on-write
 */
private static class ImmutableMarketDataFeed {
    private final AtomicReference<Map<String, MarketData>> dataRef = 
        new AtomicReference<>(new ConcurrentHashMap<>());
    
    public MarketData getLatestData(String symbol) {
        return dataRef.get().get(symbol);  // ← Safe to read without locking
    }
    
    public void updateMarketData(String symbol, BigDecimal price, long volume) {
        Map<String, MarketData> currentData = dataRef.get();
        Map<String, MarketData> newData = new ConcurrentHashMap<>(currentData);  // ← Copy
        
        MarketData newMarketData = MarketData.of(symbol, price, price, price, volume);
        newData.put(symbol, newMarketData);
        
        dataRef.set(newData);  // ← Atomic replacement of entire map
    }
}
```

**🔒 Immutable Objects Benefits:**
- 🔒 **Thread safe by design** - No synchronization needed
- 🚀 **Fast reads** - No locking overhead
- 🛡️ **No race conditions** - Objects cannot change
- 📸 **Snapshot consistency** - Readers see consistent state

---

## 🏊 **Thread Pool Pattern**

### **Problem:** Manage a pool of reusable threads efficiently without creating/destroying threads constantly.

### **Custom Thread Pool Implementation**

```java
public void demonstrateThreadPoolPattern() throws InterruptedException {
    logger.info("=== Demonstrating Thread Pool Pattern ===");
    
    // Custom thread pool implementation
    CustomTradingThreadPool customPool = new CustomTradingThreadPool(4);
    
    // Submit trading tasks
    for (int i = 1; i <= 12; i++) {
        final int taskId = i;
        
        customPool.submit(() -> {
            String threadName = Thread.currentThread().getName();
            logger.info("📋 Task {} starting in thread {}", taskId, threadName);
            
            try {
                // Simulate trading work
                Thread.sleep(500 + (long) (Math.random() * 1000));
                logger.info("✅ Task {} completed in thread {}", taskId, threadName);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Task {} interrupted", taskId);
            }
        });
        
        // Monitor pool status
        if (i % 3 == 0) {
            logger.info("📊 Pool status - Active: {}, Queue: {}", 
                customPool.getActiveThreads(), customPool.getQueueSize());
        }
        
        Thread.sleep(200);
    }
    
    logger.info("⏳ All tasks submitted, waiting for completion...");
    Thread.sleep(8000);
    
    customPool.shutdown();
    
    logger.info("Thread pool pattern demonstration completed");
}

/**
 * Custom Thread Pool implementation
 */
private static class CustomTradingThreadPool {
    private final BlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<>();
    private final List<Thread> workers = new ArrayList<>();
    private final AtomicInteger activeThreads = new AtomicInteger(0);
    private volatile boolean shutdown = false;
    
    public CustomTradingThreadPool(int poolSize) {
        for (int i = 0; i < poolSize; i++) {
            Thread worker = new Thread(this::runWorker, "TradingWorker-" + i);
            workers.add(worker);
            worker.start();  // ← Start worker threads immediately
        }
    }
    
    public void submit(Runnable task) {
        if (!shutdown) {
            taskQueue.offer(task);  // ← Add task to queue
        }
    }
    
    private void runWorker() {
        while (!shutdown) {
            try {
                Runnable task = taskQueue.poll(100, TimeUnit.MILLISECONDS);
                if (task != null) {
                    activeThreads.incrementAndGet();
                    try {
                        task.run();  // ← Execute task
                    } finally {
                        activeThreads.decrementAndGet();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    public void shutdown() {
        shutdown = true;
        for (Thread worker : workers) {
            worker.interrupt();
        }
    }
    
    public int getActiveThreads() { return activeThreads.get(); }
    public int getQueueSize() { return taskQueue.size(); }
}
```

**♻️ Thread Pool Benefits:**
- ♻️ **Thread reuse** - Avoid expensive thread creation/destruction
- 🎛️ **Resource control** - Limit number of concurrent threads
- 📊 **Monitoring** - Track active threads and queue size
- 🚦 **Flow control** - Queue provides backpressure

---

## 🚀 **Future/Promise Pattern**

### **Problem:** Handle asynchronous operations and their results elegantly.

### **Trading Workflow with CompletableFuture**

```java
public void demonstrateFuturePromisePattern() throws InterruptedException, ExecutionException {
    logger.info("=== Demonstrating Future/Promise Pattern ===");
    
    ExecutorService executor = Executors.newFixedThreadPool(4);
    
    try {
        // Create async trading workflow
        CompletableFuture<String> tradingWorkflow = CompletableFuture
            .supplyAsync(() -> {
                logger.info("🔐 Step 1: Authenticating user...");
                try { Thread.sleep(300); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                return "USER_AUTH_SUCCESS";
            }, executor)
            .thenCompose(auth -> {
                logger.info("💰 Step 2: Checking account balance...");
                return CompletableFuture.supplyAsync(() -> {
                    try { Thread.sleep(400); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    return auth + ":BALANCE_SUFFICIENT";
                }, executor);
            })
            .thenCompose(balance -> {
                logger.info("⚖️ Step 3: Performing risk check...");
                return CompletableFuture.supplyAsync(() -> {
                    try { Thread.sleep(250); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    return balance + ":RISK_APPROVED";
                }, executor);
            })
            .thenCompose(risk -> {
                logger.info("📊 Step 4: Fetching market data...");
                return CompletableFuture.supplyAsync(() -> {
                    try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    return risk + ":MARKET_DATA_OK";
                }, executor);
            })
            .thenApply(marketData -> {
                logger.info("🎯 Step 5: Executing trade...");
                try { Thread.sleep(300); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                return marketData + ":TRADE_EXECUTED";
            })
            .exceptionally(throwable -> {
                logger.error("❌ Trading workflow failed: {}", throwable.getMessage());
                return "TRADE_FAILED:" + throwable.getMessage();
            });
        
        // Get result
        String result = tradingWorkflow.get(5, TimeUnit.SECONDS);
        logger.info("✅ Trading workflow result: {}", result);
        
        // Demonstrate parallel futures
        logger.info("🔄 Demonstrating parallel operations...");
        
        CompletableFuture<String> portfolioAnalysis = CompletableFuture.supplyAsync(() -> {
            logger.info("📊 Analyzing portfolio...");
            try { Thread.sleep(600); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            return "PORTFOLIO_HEALTHY";
        }, executor);
        
        CompletableFuture<String> marketAnalysis = CompletableFuture.supplyAsync(() -> {
            logger.info("📈 Analyzing market trends...");
            try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            return "MARKET_BULLISH";
        }, executor);
        
        CompletableFuture<String> riskAnalysis = CompletableFuture.supplyAsync(() -> {
            logger.info("⚖️ Analyzing risk factors...");
            try { Thread.sleep(400); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            return "RISK_LOW";
        }, executor);
        
        // Combine all analyses
        CompletableFuture<String> combinedAnalysis = CompletableFuture.allOf(
            portfolioAnalysis, marketAnalysis, riskAnalysis
        ).thenApply(v -> {
            String portfolio = portfolioAnalysis.join();
            String market = marketAnalysis.join();
            String risk = riskAnalysis.join();
            return String.format("ANALYSIS_COMPLETE: %s | %s | %s", portfolio, market, risk);
        });
        
        String analysisResult = combinedAnalysis.get(2, TimeUnit.SECONDS);
        logger.info("📋 Combined analysis: {}", analysisResult);
        
    } catch (TimeoutException e) {
        logger.error("⏰ Operation timed out: {}", e.getMessage());
    } finally {
        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.SECONDS);
    }
}
```

---

## 📊 **Pattern Comparison**

| Pattern | Problem Solved | Key Benefit | Use When |
|---|---|---|---|
| **Producer-Consumer** | Coordinate work generation and processing | Decoupling + Flow control | Different speeds of producers/consumers |
| **Work Stealing** | Load balance parallel work | CPU utilization | Divide-and-conquer algorithms |
| **Actor Model** | Manage distributed state | No shared state | Complex state interactions |
| **Immutable Objects** | Thread safety without locks | Fast, safe reads | Read-heavy scenarios |
| **Thread Pool** | Manage thread lifecycle | Resource efficiency | Task-based parallelism |
| **Future/Promise** | Handle async operations | Non-blocking results | Async workflows |

---

## 🎯 **When to Use Each Pattern**

### **Producer-Consumer**
```java
// ✅ Perfect for:
// - Pipeline architectures
// - Different processing speeds
// - Buffering between stages

BlockingQueue<Order> orderQueue = new LinkedBlockingQueue<>();
// Producers add orders, consumers process them
```

### **Work Stealing**
```java
// ✅ Perfect for:
// - CPU-intensive parallel work
// - Divide-and-conquer algorithms
// - Recursive tasks

ForkJoinPool pool = new ForkJoinPool();
RecursiveTask<Integer> task = new ParallelComputationTask(data);
Integer result = pool.invoke(task);
```

### **Actor Model**
```java
// ✅ Perfect for:
// - Distributed systems
// - Complex state management
// - Event-driven architectures

ActorSystem system = ActorSystem.create();
ActorRef orderProcessor = system.actorOf(OrderProcessorActor.props());
orderProcessor.tell(new ProcessOrder(trade), ActorRef.noSender());
```

### **Immutable Objects**
```java
// ✅ Perfect for:
// - Configuration data
// - Shared reference data
// - Value objects

public record MarketData(String symbol, BigDecimal price, long volume) {
    // Immutable by design
}
```

### **Thread Pool**
```java
// ✅ Perfect for:
// - Task-based parallelism
// - Resource management
// - Controlling concurrency level

ExecutorService pool = Executors.newFixedThreadPool(4);
pool.submit(() -> processOrder());
```

### **Future/Promise**
```java
// ✅ Perfect for:
// - Async workflows
// - Non-blocking operations
// - Composing async operations

CompletableFuture<String> result = CompletableFuture
    .supplyAsync(() -> fetchData())
    .thenCompose(data -> processData(data))
    .thenApply(processed -> formatResult(processed));
```

---

## 🎯 **Real-World Trading System Using Multiple Patterns**

```java
public class ComprehensiveTradingSystem {
    
    // Producer-Consumer for order flow
    private final BlockingQueue<Trade> incomingOrders = new LinkedBlockingQueue<>();
    
    // Work Stealing for parallel risk calculations
    private final ForkJoinPool riskCalculationPool = new ForkJoinPool();
    
    // Actor Model for portfolio management
    private final PortfolioActor portfolioActor = new PortfolioActor();
    
    // Immutable Objects for market data
    private final ImmutableMarketDataFeed marketDataFeed = new ImmutableMarketDataFeed();
    
    // Thread Pool for order execution
    private final ExecutorService executionPool = Executors.newFixedThreadPool(8);
    
    public CompletableFuture<String> processOrder(Trade trade) {
        // Future/Promise pattern for async workflow
        return CompletableFuture
            .supplyAsync(() -> {
                // Get immutable market data
                MarketData data = marketDataFeed.getLatestData(trade.symbol());
                return validateOrder(trade, data);
            })
            .thenCompose(validation -> {
                if (validation) {
                    // Use work stealing for parallel risk calculation
                    return CompletableFuture.supplyAsync(() -> 
                        riskCalculationPool.invoke(new RiskCalculationTask(trade))
                    );
                } else {
                    return CompletableFuture.completedFuture(false);
                }
            })
            .thenCompose(riskResult -> {
                if (riskResult) {
                    // Send to actor for portfolio update
                    portfolioActor.tell(new UpdatePositionMessage(trade.symbol(), trade.quantity()));
                    
                    // Add to producer-consumer queue for execution
                    try {
                        incomingOrders.put(trade);
                        return CompletableFuture.completedFuture("ORDER_QUEUED");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return CompletableFuture.completedFuture("ORDER_INTERRUPTED");
                    }
                } else {
                    return CompletableFuture.completedFuture("ORDER_REJECTED");
                }
            });
    }
}
```

---

## 🔑 **Key Takeaways**

1. **Producer-Consumer** - Perfect for pipeline architectures and buffering
2. **Work Stealing** - Optimal for CPU-intensive parallel work  
3. **Actor Model** - Excellent for distributed state management
4. **Immutable Objects** - Thread safety without synchronization overhead
5. **Thread Pool** - Essential for managing computational resources
6. **Future/Promise** - Ideal for async workflows and composition
7. **Combine patterns** - Real systems use multiple patterns together
8. **Choose based on problem** - Each pattern solves specific concurrency challenges

## 📚 **Related Topics**
- [Thread Coordination](./04-thread-coordination.md)
- [Executors & Thread Pools](./05-executors-thread-pools.md)
- [Futures & Async Programming](./07-futures-async.md)

---

These concurrency patterns are the building blocks of robust, scalable concurrent systems. Master these patterns and you'll be able to design efficient solutions for any concurrency challenge!
