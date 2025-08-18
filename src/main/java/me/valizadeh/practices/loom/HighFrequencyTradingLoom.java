package me.valizadeh.practices.loom;

import me.valizadeh.practices.model.Trade;
import me.valizadeh.practices.model.TradeType;
import me.valizadeh.practices.model.MarketData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Demonstrates Project Loom Virtual Threads in high-frequency trading scenarios
 * Shows scalability, performance, and ease of use compared to traditional threads
 */
public class HighFrequencyTradingLoom {
    private static final Logger logger = LoggerFactory.getLogger(HighFrequencyTradingLoom.class);
    
    /**
     * Demonstrates basic virtual thread creation and execution
     */
    public void demonstrateBasicVirtualThreads() throws InterruptedException {
        logger.info("=== Demonstrating Basic Virtual Threads ===");
        
        // Create virtual threads directly
        Thread virtualThread1 = Thread.ofVirtual()
            .name("VirtualTrader-1")
            .start(() -> {
                logger.info("Virtual thread {} processing trade in {}", 
                    Thread.currentThread().getName(), 
                    Thread.currentThread());
                
                simulateTradeProcessing();
            });
        
        // Using virtual thread factory
        ThreadFactory virtualFactory = Thread.ofVirtual().factory();
        Thread virtualThread2 = virtualFactory.newThread(() -> {
            logger.info("Factory virtual thread {} processing market data", 
                Thread.currentThread().getName());
            
            simulateMarketDataProcessing();
        });
        
        virtualThread2.start();
        
        // Wait for completion
        virtualThread1.join();
        virtualThread2.join();
        
        logger.info("Basic virtual threads demonstration completed");
    }
    
    /**
     * Demonstrates virtual thread executor for massive scalability
     */
    public void demonstrateVirtualThreadExecutor() throws InterruptedException {
        logger.info("=== Demonstrating Virtual Thread Executor ===");
        
        // Create executor that creates a new virtual thread for each task
        try (ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            
            Instant start = Instant.now();
            CountDownLatch latch = new CountDownLatch(10000);
            AtomicInteger processedTrades = new AtomicInteger(0);
            
            // Submit 10,000 trading tasks - this would be impossible with platform threads
            for (int i = 0; i < 10000; i++) {
                final int tradeId = i;
                
                virtualExecutor.submit(() -> {
                    try {
                        // Simulate trade processing with I/O (market data fetch, database write)
                        Trade trade = Trade.buy("STOCK" + (tradeId % 100), 
                            new BigDecimal("100"), 10, "client" + (tradeId % 50));
                        
                        // Simulate network I/O (perfect for virtual threads)
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
        }
    }
    
    /**
     * Demonstrates virtual threads vs platform threads performance comparison
     */
    public void demonstratePerformanceComparison() throws InterruptedException {
        logger.info("=== Demonstrating Performance Comparison ===");
        
        int taskCount = 5000;
        
        // Test with platform threads (limited by thread pool size)
        logger.info("--- Testing with Platform Threads ---");
        Instant platformStart = Instant.now();
        
        try (ExecutorService platformExecutor = Executors.newFixedThreadPool(200)) {
            CountDownLatch platformLatch = new CountDownLatch(taskCount);
            
            for (int i = 0; i < taskCount; i++) {
                final int taskId = i;
                platformExecutor.submit(() -> {
                    try {
                        simulateIOIntensiveTradeOperation(taskId);
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
        
        try (ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            CountDownLatch virtualLatch = new CountDownLatch(taskCount);
            
            for (int i = 0; i < taskCount; i++) {
                final int taskId = i;
                virtualExecutor.submit(() -> {
                    try {
                        simulateIOIntensiveTradeOperation(taskId);
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
    
    /**
     * Demonstrates virtual threads with structured concurrency
     */
    public void demonstrateStructuredConcurrency() throws InterruptedException {
        logger.info("=== Demonstrating Structured Concurrency ===");
        
        // Note: This is a simplified example as Structured Concurrency is still preview
        // In real Java 21+, you would use StructuredTaskScope
        
        List<Trade> trades = generateTrades(100);
        
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            
            // Process trades in structured groups
            List<CompletableFuture<String>> futures = new ArrayList<>();
            
            for (int i = 0; i < trades.size(); i += 10) {
                final int batchStart = i;
                final int batchEnd = Math.min(i + 10, trades.size());
                final List<Trade> batch = trades.subList(batchStart, batchEnd);
                
                CompletableFuture<String> batchFuture = CompletableFuture.supplyAsync(() -> {
                    logger.info("🔄 Processing batch {}-{} in virtual thread {}", 
                        batchStart, batchEnd - 1, Thread.currentThread().getName());
                    
                    return processTradeBatch(batch);
                }, executor);
                
                futures.add(batchFuture);
            }
            
            // Wait for all batches to complete
            CompletableFuture<Void> allBatches = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]));
            
            allBatches.join();
            
            // Collect results
            List<String> results = futures.stream()
                .map(CompletableFuture::join)
                .toList();
            
            logger.info("✅ Structured concurrency completed. Processed {} batches", results.size());
            results.forEach(result -> logger.info("Batch result: {}", result));
        }
    }
    
    /**
     * Demonstrates virtual threads for market data streaming
     */
    public void demonstrateMarketDataStreaming() throws InterruptedException {
        logger.info("=== Demonstrating Market Data Streaming with Virtual Threads ===");
        
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
            
            // Monitor streaming progress
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
    
    /**
     * Demonstrates virtual threads for order book management
     */
    public void demonstrateOrderBookManagement() throws InterruptedException {
        logger.info("=== Demonstrating Order Book Management with Virtual Threads ===");
        
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
            
            // Multiple order processors (each using virtual threads)
            for (int processorId = 0; processorId < 10; processorId++) {
                final int id = processorId;
                
                orderExecutor.submit(() -> {
                    activeProcessors.incrementAndGet();
                    try {
                        logger.info("🔄 Order processor {} started in {}", id, Thread.currentThread().getName());
                        
                        while (true) {
                            Trade order = orderQueue.poll(100, TimeUnit.MILLISECONDS);
                            if (order == null) {
                                // Check if generation is complete and queue is empty
                                if (!orderGenerator.isAlive() && orderQueue.isEmpty()) {
                                    break;
                                }
                                continue;
                            }
                            
                            // Process order (simulate matching, validation, execution)
                            processOrderInOrderBook(order, id);
                            int processed = processedOrders.incrementAndGet();
                            
                            if (processed % 100 == 0) {
                                logger.info("📤 Processed {} orders", processed);
                            }
                        }
                        
                        logger.info("✅ Order processor {} completed", id);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        activeProcessors.decrementAndGet();
                    }
                });
            }
            
            // Wait for order generation to complete
            orderGenerator.join();
            
            // Wait for all processors to finish
            while (activeProcessors.get() > 0) {
                Thread.sleep(100);
            }
            
            logger.info("🏁 Order book management completed. Total processed: {}", processedOrders.get());
        }
    }
    
    /**
     * Demonstrates virtual threads handling massive concurrent connections
     */
    public void demonstrateConnectionHandling() throws InterruptedException {
        logger.info("=== Demonstrating Massive Connection Handling ===");
        
        int numberOfClients = 2000; // Simulating many trading clients
        
        try (ExecutorService connectionExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            
            CountDownLatch connectionsLatch = new CountDownLatch(numberOfClients);
            AtomicInteger activeConnections = new AtomicInteger(0);
            AtomicLong totalMessages = new AtomicLong(0);
            
            // Simulate trading clients connecting and sending orders
            for (int clientId = 0; clientId < numberOfClients; clientId++) {
                final int id = clientId;
                
                connectionExecutor.submit(() -> {
                    activeConnections.incrementAndGet();
                    try {
                        logger.info("🔌 Client {} connected in virtual thread {}", 
                            id, Thread.currentThread().getName());
                        
                        // Simulate client session (sending multiple orders)
                        for (int orderNum = 0; orderNum < 5; orderNum++) {
                            // Simulate network delay
                            Thread.sleep(Duration.ofMillis(10 + (long) (Math.random() * 50)));
                            
                            Trade order = Trade.buy("STOCK" + (id % 20), 
                                new BigDecimal("100"), 10, "client" + id);
                            
                            // Process order
                            processClientOrder(order, id);
                            totalMessages.incrementAndGet();
                        }
                        
                        if (id % 200 == 0) {
                            logger.info("📊 {} clients processed", id);
                        }
                        
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        activeConnections.decrementAndGet();
                        connectionsLatch.countDown();
                    }
                });
            }
            
            // Monitor connections
            Thread connectionMonitor = Thread.ofVirtual().start(() -> {
                try {
                    while (connectionsLatch.getCount() > 0) {
                        Thread.sleep(Duration.ofSeconds(2));
                        logger.info("📈 Active connections: {}, Total messages: {}, Remaining: {}", 
                            activeConnections.get(), totalMessages.get(), connectionsLatch.getCount());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            
            connectionsLatch.await();
            connectionMonitor.interrupt();
            
            logger.info("🏁 Connection handling completed. {} clients, {} total messages", 
                numberOfClients, totalMessages.get());
        }
    }
    
    // Helper methods
    
    private void simulateTradeProcessing() {
        try {
            Thread.sleep(Duration.ofMillis(100 + (long) (Math.random() * 200)));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void simulateMarketDataProcessing() {
        try {
            Thread.sleep(Duration.ofMillis(50 + (long) (Math.random() * 100)));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void simulateIOIntensiveTradeOperation(int taskId) {
        try {
            // Simulate multiple I/O operations
            Thread.sleep(Duration.ofMillis(20)); // Database read
            Thread.sleep(Duration.ofMillis(30)); // Network call
            Thread.sleep(Duration.ofMillis(15)); // Database write
            
            if (taskId % 500 == 0) {
                logger.info("Completed I/O intensive task {}", taskId);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private String processTradeBatch(List<Trade> batch) {
        try {
            // Simulate batch processing
            Thread.sleep(Duration.ofMillis(50 + (long) (Math.random() * 100)));
            return "BATCH_PROCESSED:" + batch.size() + "_TRADES";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "BATCH_INTERRUPTED";
        }
    }
    
    private void processOrderInOrderBook(Trade order, int processorId) {
        try {
            // Simulate order book operations
            Thread.sleep(Duration.ofMillis(5 + (long) (Math.random() * 15)));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void processClientOrder(Trade order, int clientId) {
        try {
            // Simulate order processing
            Thread.sleep(Duration.ofMillis(2 + (long) (Math.random() * 8)));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private Trade generateRandomTrade(int id) {
        String[] symbols = {"AAPL", "GOOGL", "TSLA", "MSFT", "NVDA"};
        String symbol = symbols[id % symbols.length];
        BigDecimal price = new BigDecimal(100 + Math.random() * 500);
        int quantity = (int) (10 + Math.random() * 100);
        TradeType type = (id % 2 == 0) ? TradeType.BUY : TradeType.SELL;
        
        return (type == TradeType.BUY) 
            ? Trade.buy(symbol, price, quantity, "client" + (id % 20))
            : Trade.sell(symbol, price, quantity, "client" + (id % 20));
    }
    
    private List<Trade> generateTrades(int count) {
        List<Trade> trades = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            trades.add(generateRandomTrade(i));
        }
        return trades;
    }
    
    public void runAllDemos() throws InterruptedException {
        demonstrateBasicVirtualThreads();
        TimeUnit.SECONDS.sleep(1);
        
        demonstrateVirtualThreadExecutor();
        TimeUnit.SECONDS.sleep(1);
        
        demonstratePerformanceComparison();
        TimeUnit.SECONDS.sleep(1);
        
        demonstrateStructuredConcurrency();
        TimeUnit.SECONDS.sleep(1);
        
        demonstrateMarketDataStreaming();
        TimeUnit.SECONDS.sleep(1);
        
        demonstrateOrderBookManagement();
        TimeUnit.SECONDS.sleep(1);
        
        demonstrateConnectionHandling();
    }
}
