package me.valizadeh.practices;

import me.valizadeh.practices.model.Trade;
import me.valizadeh.practices.model.TradeType;
import me.valizadeh.practices.model.MarketData;

import java.math.BigDecimal;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.ArrayList;

/**
 * Simple demonstration of Java Concurrency concepts without external dependencies
 * Shows key concepts that can be run with just the JDK
 */
public class SimpleDemo {
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("🏛️ Java Concurrency Trading System - Simple Demo");
        System.out.println("=".repeat(60));
        
        SimpleDemo demo = new SimpleDemo();
        demo.runBasicDemos();
        
        System.out.println("\n✅ Simple demo completed successfully!");
        System.out.println("📚 This demonstrates core Java concurrency concepts");
        System.out.println("🚀 Run the full demo with logging dependencies for complete examples");
    }
    
    public void runBasicDemos() throws InterruptedException {
        demonstrateBasicThreads();
        Thread.sleep(1000);
        
        demonstrateExecutors();
        Thread.sleep(1000);
        
        demonstrateVirtualThreads();
        Thread.sleep(1000);
        
        demonstrateConcurrentCollections();
    }
    
    /**
     * Basic thread demonstration
     */
    public void demonstrateBasicThreads() throws InterruptedException {
        System.out.println("\n🧵 BASIC THREADS DEMONSTRATION");
        System.out.println("-".repeat(40));
        
        AtomicInteger processedTrades = new AtomicInteger(0);
        
        // Create multiple threads processing trades
        Thread[] traders = new Thread[3];
        
        for (int i = 0; i < traders.length; i++) {
            final int traderId = i;
            traders[i] = new Thread(() -> {
                for (int j = 0; j < 3; j++) {
                    Trade trade = Trade.buy("STOCK" + j, new BigDecimal("100"), 10, "client" + traderId);
                    
                    System.out.println("Trader-" + traderId + " processing: " + trade.id());
                    
                    try {
                        Thread.sleep(200); // Simulate processing
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    
                    processedTrades.incrementAndGet();
                    System.out.println("Trader-" + traderId + " completed: " + trade.id());
                }
            }, "Trader-" + traderId);
        }
        
        // Start all threads
        for (Thread trader : traders) {
            trader.start();
        }
        
        // Wait for completion
        for (Thread trader : traders) {
            trader.join();
        }
        
        System.out.println("✅ Basic threads completed. Processed: " + processedTrades.get() + " trades");
    }
    
    /**
     * Executor service demonstration
     */
    public void demonstrateExecutors() throws InterruptedException {
        System.out.println("\n⚙️ EXECUTOR SERVICE DEMONSTRATION");
        System.out.println("-".repeat(40));
        
        try (ExecutorService executor = Executors.newFixedThreadPool(4)) {
            
            CountDownLatch latch = new CountDownLatch(8);
            AtomicInteger executedOrders = new AtomicInteger(0);
            
            // Submit multiple trading tasks
            for (int i = 0; i < 8; i++) {
                final int orderId = i;
                
                executor.submit(() -> {
                    try {
                        Trade trade = Trade.sell("EXEC" + orderId, new BigDecimal("200"), 20, "client1");
                        
                        System.out.println("Executing order: " + trade.id() + " in thread: " + 
                            Thread.currentThread().getName());
                        
                        Thread.sleep(150); // Simulate execution
                        
                        executedOrders.incrementAndGet();
                        System.out.println("Completed order: " + trade.id());
                        
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        latch.countDown();
                    }
                });
            }
            
            latch.await(); // Wait for all tasks to complete
            System.out.println("✅ Executor service completed. Executed: " + executedOrders.get() + " orders");
        }
    }
    
    /**
     * Virtual threads demonstration (Java 21+)
     */
    public void demonstrateVirtualThreads() throws InterruptedException {
        System.out.println("\n🚀 VIRTUAL THREADS DEMONSTRATION");
        System.out.println("-".repeat(40));
        
        try (ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            
            CountDownLatch latch = new CountDownLatch(1000);
            AtomicInteger processedCount = new AtomicInteger(0);
            
            System.out.println("Starting 1000 virtual threads for market data processing...");
            
            // Submit 1000 tasks - would be impossible with platform threads
            for (int i = 0; i < 1000; i++) {
                final int taskId = i;
                
                virtualExecutor.submit(() -> {
                    try {
                        // Simulate I/O intensive market data fetch
                        Thread.sleep(50);
                        
                        MarketData data = MarketData.of("VIRT" + (taskId % 10), 
                            new BigDecimal("100"), new BigDecimal("101"), 
                            new BigDecimal("100.5"), 1000L);
                        
                        processedCount.incrementAndGet();
                        
                        if (taskId % 100 == 0) {
                            System.out.println("Processed " + taskId + " market data updates...");
                        }
                        
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        latch.countDown();
                    }
                });
            }
            
            latch.await();
            System.out.println("✅ Virtual threads completed. Processed: " + processedCount.get() + " market data updates");
        }
    }
    
    /**
     * Concurrent collections demonstration
     */
    public void demonstrateConcurrentCollections() throws InterruptedException {
        System.out.println("\n📦 CONCURRENT COLLECTIONS DEMONSTRATION");
        System.out.println("-".repeat(40));
        
        // ConcurrentHashMap for position tracking
        ConcurrentHashMap<String, AtomicInteger> positions = new ConcurrentHashMap<>();
        positions.put("AAPL", new AtomicInteger(0));
        positions.put("GOOGL", new AtomicInteger(0));
        
        // BlockingQueue for order processing
        BlockingQueue<Trade> orderQueue = new LinkedBlockingQueue<>();
        
        // Producer thread
        Thread producer = new Thread(() -> {
            try {
                for (int i = 0; i < 6; i++) {
                    Trade trade = (i % 2 == 0) 
                        ? Trade.buy("AAPL", new BigDecimal("150"), 10, "client1")
                        : Trade.sell("GOOGL", new BigDecimal("2800"), 5, "client2");
                    
                    orderQueue.put(trade);
                    System.out.println("📥 Produced order: " + trade.id());
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "OrderProducer");
        
        // Consumer thread
        Thread consumer = new Thread(() -> {
            try {
                for (int i = 0; i < 6; i++) {
                    Trade trade = orderQueue.take();
                    
                    // Update position atomically
                    AtomicInteger position = positions.get(trade.symbol());
                    if (position != null) {
                        int newPos = (trade.type() == TradeType.BUY) 
                            ? position.addAndGet(trade.quantity())
                            : position.addAndGet(-trade.quantity());
                        
                        System.out.println("📤 Processed order: " + trade.id() + 
                            " - New position for " + trade.symbol() + ": " + newPos);
                    }
                    
                    Thread.sleep(150);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "OrderConsumer");
        
        producer.start();
        consumer.start();
        
        producer.join();
        consumer.join();
        
        System.out.println("✅ Concurrent collections completed");
        System.out.println("Final positions:");
        positions.forEach((symbol, pos) -> 
            System.out.println("  " + symbol + ": " + pos.get() + " shares"));
    }
}
