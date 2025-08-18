package me.valizadeh.practices.executors;

import me.valizadeh.practices.model.Trade;
import me.valizadeh.practices.model.TradeStatus;
import me.valizadeh.practices.model.TradeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Demonstrates ExecutorService and thread pools in order execution scenarios
 * Covers different types of thread pools, task submission, and proper shutdown
 */
public class OrderExecutionEngine {
    private static final Logger logger = LoggerFactory.getLogger(OrderExecutionEngine.class);
    private final AtomicInteger executedTrades = new AtomicInteger(0);
    
    /**
     * Demonstrates FixedThreadPool for consistent order processing capacity
     */
    public void demonstrateFixedThreadPool() throws InterruptedException {
        logger.info("=== Demonstrating FixedThreadPool for Order Processing ===");
        
        // Fixed pool of 4 threads for predictable resource usage
        ExecutorService fixedPool = Executors.newFixedThreadPool(4, 
            new ThreadFactory() {
                private final AtomicInteger threadNumber = new AtomicInteger(1);
                
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "OrderProcessor-" + threadNumber.getAndIncrement());
                    t.setDaemon(false); // Ensure JVM waits for completion
                    return t;
                }
            });
        
        List<Trade> trades = generateTrades(12);
        
        // Submit all trades for processing
        for (Trade trade : trades) {
            fixedPool.submit(() -> {
                processOrder(trade);
                executedTrades.incrementAndGet();
            });
        }
        
        // Proper shutdown sequence
        fixedPool.shutdown(); // Reject new tasks
        logger.info("Fixed thread pool shutdown initiated");
        
        if (!fixedPool.awaitTermination(10, TimeUnit.SECONDS)) {
            logger.warn("Pool did not terminate gracefully, forcing shutdown");
            fixedPool.shutdownNow();
        }
        
        logger.info("Fixed thread pool processing completed. Executed trades: {}", executedTrades.get());
    }
    
    /**
     * Demonstrates CachedThreadPool for variable load scenarios
     */
    public void demonstrateCachedThreadPool() throws InterruptedException {
        logger.info("=== Demonstrating CachedThreadPool for Variable Load ===");
        
        ExecutorService cachedPool = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "CachedProcessor-" + Thread.currentThread().getId());
            t.setDaemon(false);
            return t;
        });
        
        // Simulate burst of orders followed by quiet period
        List<Trade> burstTrades = generateTrades(8);
        
        logger.info("Submitting burst of {} orders", burstTrades.size());
        for (Trade trade : burstTrades) {
            cachedPool.submit(() -> {
                processOrder(trade);
                executedTrades.incrementAndGet();
            });
        }
        
        // Wait a bit
        Thread.sleep(1000);
        
        // Second smaller burst
        List<Trade> secondBurst = generateTrades(3);
        logger.info("Submitting second burst of {} orders", secondBurst.size());
        for (Trade trade : secondBurst) {
            cachedPool.submit(() -> {
                processOrder(trade);
                executedTrades.incrementAndGet();
            });
        }
        
        gracefulShutdown(cachedPool, "CachedThreadPool");
    }
    
    /**
     * Demonstrates SingleThreadExecutor for sequential processing
     */
    public void demonstrateSingleThreadExecutor() throws InterruptedException {
        logger.info("=== Demonstrating SingleThreadExecutor for Sequential Processing ===");
        
        ExecutorService singlePool = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "SequentialProcessor");
            t.setDaemon(false);
            return t;
        });
        
        List<Trade> priorityTrades = generateTrades(5);
        
        logger.info("Processing {} priority trades sequentially", priorityTrades.size());
        for (Trade trade : priorityTrades) {
            singlePool.submit(() -> {
                logger.info("Sequential processing: {}", trade.id());
                processOrder(trade);
                executedTrades.incrementAndGet();
            });
        }
        
        gracefulShutdown(singlePool, "SingleThreadExecutor");
    }
    
    /**
     * Demonstrates ScheduledThreadPool for time-based order execution
     */
    public void demonstrateScheduledThreadPool() throws InterruptedException {
        logger.info("=== Demonstrating ScheduledThreadPool for Scheduled Orders ===");
        
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3, r -> {
            Thread t = new Thread(r, "ScheduledProcessor-" + System.currentTimeMillis());
            t.setDaemon(false);
            return t;
        });
        
        List<Trade> scheduledTrades = generateTrades(4);
        
        // Schedule orders at different times
        for (int i = 0; i < scheduledTrades.size(); i++) {
            final Trade trade = scheduledTrades.get(i);
            final int delay = (i + 1) * 500; // Staggered execution
            
            scheduler.schedule(() -> {
                logger.info("Executing scheduled order: {}", trade.id());
                processOrder(trade);
                executedTrades.incrementAndGet();
            }, delay, TimeUnit.MILLISECONDS);
        }
        
        // Schedule a recurring market data update
        ScheduledFuture<?> marketDataTask = scheduler.scheduleAtFixedRate(() -> {
            logger.info("📊 Market data update at {}", System.currentTimeMillis());
        }, 100, 300, TimeUnit.MILLISECONDS);
        
        // Let it run for a while
        Thread.sleep(2500);
        
        // Cancel recurring task
        marketDataTask.cancel(false);
        logger.info("Market data updates cancelled");
        
        gracefulShutdown(scheduler, "ScheduledThreadPool");
    }
    
    /**
     * Demonstrates custom ThreadPoolExecutor with detailed configuration
     */
    public void demonstrateCustomThreadPoolExecutor() throws InterruptedException {
        logger.info("=== Demonstrating Custom ThreadPoolExecutor ===");
        
        // Custom thread pool with specific configuration
        ThreadPoolExecutor customPool = new ThreadPoolExecutor(
            2,                              // corePoolSize
            5,                              // maximumPoolSize
            60L,                            // keepAliveTime
            TimeUnit.SECONDS,               // time unit
            new LinkedBlockingQueue<>(10),  // workQueue with capacity
            new ThreadFactory() {           // custom thread factory
                private final AtomicInteger threadNumber = new AtomicInteger(1);
                
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "CustomProcessor-" + threadNumber.getAndIncrement());
                    t.setDaemon(false);
                    t.setPriority(Thread.NORM_PRIORITY);
                    return t;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy() // rejection policy
        );
        
        // Monitor pool state
        Thread monitor = new Thread(() -> {
            while (!customPool.isTerminated()) {
                try {
                    logger.info("Pool state - Active: {}, Pool size: {}, Queue size: {}, Completed: {}", 
                        customPool.getActiveCount(),
                        customPool.getPoolSize(),
                        customPool.getQueue().size(),
                        customPool.getCompletedTaskCount());
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "PoolMonitor");
        
        monitor.start();
        
        // Submit more tasks than queue capacity to test rejection policy
        List<Trade> trades = generateTrades(20);
        
        for (Trade trade : trades) {
            try {
                customPool.submit(() -> {
                    processOrder(trade);
                    executedTrades.incrementAndGet();
                });
            } catch (RejectedExecutionException e) {
                logger.warn("Trade {} rejected due to pool capacity", trade.id());
            }
        }
        
        gracefulShutdown(customPool, "CustomThreadPoolExecutor");
        monitor.interrupt();
        monitor.join();
    }
    
    /**
     * Demonstrates ForkJoinPool for recursive order matching
     */
    public void demonstrateForkJoinPool() throws InterruptedException {
        logger.info("=== Demonstrating ForkJoinPool for Order Matching ===");
        
        ForkJoinPool forkJoinPool = new ForkJoinPool(4);
        
        List<Trade> buyOrders = generateBuyTrades(10);
        List<Trade> sellOrders = generateSellTrades(10);
        
        // Create a recursive task for order matching
        OrderMatchingTask matchingTask = new OrderMatchingTask(buyOrders, sellOrders);
        
        try {
            List<String> matches = forkJoinPool.invoke(matchingTask);
            logger.info("Order matching completed. Matches found: {}", matches.size());
            matches.forEach(match -> logger.info("Match: {}", match));
        } finally {
            forkJoinPool.shutdown();
            if (!forkJoinPool.awaitTermination(5, TimeUnit.SECONDS)) {
                forkJoinPool.shutdownNow();
            }
        }
    }
    
    /**
     * Demonstrates using the common ForkJoinPool (used by parallel streams)
     */
    public void demonstrateCommonForkJoinPool() {
        logger.info("=== Demonstrating Common ForkJoinPool ===");
        
        List<Trade> trades = generateTrades(20);
        
        // Process trades using parallel streams (uses common ForkJoinPool)
        long processedCount = trades.parallelStream()
            .peek(trade -> logger.info("Processing {} in thread {}", 
                trade.id(), Thread.currentThread().getName()))
            .mapToLong(trade -> {
                processOrder(trade);
                return 1L;
            })
            .sum();
        
        logger.info("Parallel stream processing completed. Processed: {}", processedCount);
        executedTrades.addAndGet((int) processedCount);
    }
    
    /**
     * Demonstrates proper exception handling in executors
     */
    public void demonstrateExceptionHandling() throws InterruptedException {
        logger.info("=== Demonstrating Exception Handling in Executors ===");
        
        ExecutorService pool = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "ExceptionHandler-" + System.currentTimeMillis());
            t.setUncaughtExceptionHandler((thread, exception) -> {
                logger.error("Uncaught exception in thread {}: {}", thread.getName(), exception.getMessage());
            });
            return t;
        });
        
        // Submit tasks that may throw exceptions
        for (int i = 0; i < 5; i++) {
            final int taskId = i;
            pool.submit(() -> {
                if (taskId == 2) {
                    throw new RuntimeException("Simulated processing error for task " + taskId);
                }
                
                Trade trade = Trade.buy("TEST", new BigDecimal("100"), 10, "client" + taskId);
                processOrder(trade);
                executedTrades.incrementAndGet();
                logger.info("Task {} completed successfully", taskId);
            });
        }
        
        gracefulShutdown(pool, "ExceptionHandling pool");
    }
    
    private void processOrder(Trade trade) {
        try {
            // Simulate order processing time
            Thread.sleep((long) (100 + Math.random() * 200));
            
            logger.info("Processed {} {} {} shares of {} at ${} in thread {}", 
                trade.type(), trade.quantity(), trade.symbol(), trade.price(),
                trade.clientId(), Thread.currentThread().getName());
                
        } catch (InterruptedException e) {
            logger.warn("Order processing interrupted for {}", trade.id());
            Thread.currentThread().interrupt();
        }
    }
    
    private void gracefulShutdown(ExecutorService executor, String poolName) throws InterruptedException {
        executor.shutdown();
        logger.info("{} shutdown initiated", poolName);
        
        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
            logger.warn("{} did not terminate gracefully, forcing shutdown", poolName);
            executor.shutdownNow();
            
            if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                logger.error("{} did not terminate even after forced shutdown", poolName);
            }
        }
        
        logger.info("{} shutdown completed", poolName);
    }
    
    private List<Trade> generateTrades(int count) {
        List<Trade> trades = new ArrayList<>();
        String[] symbols = {"AAPL", "GOOGL", "TSLA", "MSFT", "NVDA", "AMD", "INTC"};
        
        for (int i = 0; i < count; i++) {
            String symbol = symbols[i % symbols.length];
            BigDecimal price = new BigDecimal(100 + Math.random() * 500);
            int quantity = (int) (10 + Math.random() * 100);
            TradeType type = (i % 2 == 0) ? TradeType.BUY : TradeType.SELL;
            
            Trade trade = (type == TradeType.BUY) 
                ? Trade.buy(symbol, price, quantity, "client" + (i % 5))
                : Trade.sell(symbol, price, quantity, "client" + (i % 5));
                
            trades.add(trade);
        }
        
        return trades;
    }
    
    private List<Trade> generateBuyTrades(int count) {
        return generateTrades(count).stream()
            .map(t -> Trade.buy(t.symbol(), t.price(), t.quantity(), t.clientId()))
            .toList();
    }
    
    private List<Trade> generateSellTrades(int count) {
        return generateTrades(count).stream()
            .map(t -> Trade.sell(t.symbol(), t.price(), t.quantity(), t.clientId()))
            .toList();
    }
    
    public void runAllDemos() throws InterruptedException {
        demonstrateFixedThreadPool();
        TimeUnit.SECONDS.sleep(1);
        
        demonstrateCachedThreadPool();
        TimeUnit.SECONDS.sleep(1);
        
        demonstrateSingleThreadExecutor();
        TimeUnit.SECONDS.sleep(1);
        
        demonstrateScheduledThreadPool();
        TimeUnit.SECONDS.sleep(1);
        
        demonstrateCustomThreadPoolExecutor();
        TimeUnit.SECONDS.sleep(1);
        
        demonstrateForkJoinPool();
        TimeUnit.SECONDS.sleep(1);
        
        demonstrateCommonForkJoinPool();
        TimeUnit.SECONDS.sleep(1);
        
        demonstrateExceptionHandling();
        
        logger.info("Total executed trades across all demos: {}", executedTrades.get());
    }
    
    /**
     * RecursiveTask for demonstrating ForkJoinPool
     */
    private static class OrderMatchingTask extends RecursiveTask<List<String>> {
        private static final int THRESHOLD = 5;
        private final List<Trade> buyOrders;
        private final List<Trade> sellOrders;
        
        public OrderMatchingTask(List<Trade> buyOrders, List<Trade> sellOrders) {
            this.buyOrders = buyOrders;
            this.sellOrders = sellOrders;
        }
        
        @Override
        protected List<String> compute() {
            List<String> matches = new ArrayList<>();
            
            if (buyOrders.size() <= THRESHOLD || sellOrders.size() <= THRESHOLD) {
                // Direct computation for small lists
                return matchOrders(buyOrders, sellOrders);
            }
            
            // Divide and conquer
            int buyMid = buyOrders.size() / 2;
            int sellMid = sellOrders.size() / 2;
            
            OrderMatchingTask task1 = new OrderMatchingTask(
                buyOrders.subList(0, buyMid), 
                sellOrders.subList(0, sellMid)
            );
            OrderMatchingTask task2 = new OrderMatchingTask(
                buyOrders.subList(buyMid, buyOrders.size()), 
                sellOrders.subList(sellMid, sellOrders.size())
            );
            
            // Fork tasks
            task1.fork();
            List<String> result2 = task2.compute();
            List<String> result1 = task1.join();
            
            // Combine results
            matches.addAll(result1);
            matches.addAll(result2);
            
            return matches;
        }
        
        private List<String> matchOrders(List<Trade> buys, List<Trade> sells) {
            List<String> matches = new ArrayList<>();
            
            for (Trade buy : buys) {
                for (Trade sell : sells) {
                    if (buy.symbol().equals(sell.symbol()) && 
                        buy.price().compareTo(sell.price()) >= 0) {
                        matches.add(String.format("Matched %s: BUY@%s with SELL@%s", 
                            buy.symbol(), buy.price(), sell.price()));
                        break; // One match per buy order for simplicity
                    }
                }
            }
            
            return matches;
        }
    }
}
