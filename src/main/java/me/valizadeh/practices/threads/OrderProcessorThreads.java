package me.valizadeh.practices.threads;

import me.valizadeh.practices.model.Trade;
import me.valizadeh.practices.model.TradeStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Demonstrates basic thread operations in the context of order processing
 * Covers thread creation, lifecycle, interruption, and common pitfalls
 */
public class OrderProcessorThreads {
    private static final Logger logger = LoggerFactory.getLogger(OrderProcessorThreads.class);
    private final AtomicInteger processedOrders = new AtomicInteger(0);
    
    /**
     * Demonstrates different ways to create threads for order processing
     */
    public void demonstrateThreadCreation() throws InterruptedException {
        logger.info("=== Demonstrating Thread Creation Methods ===");
        
        // Method 1: Extending Thread class
        class OrderProcessorThread extends Thread {
            private final Trade trade;
            
            public OrderProcessorThread(Trade trade) {
                super("OrderProcessor-" + trade.id());
                this.trade = trade;
            }
            
            @Override
            public void run() {
                logger.info("Processing trade {} in thread {}", trade.id(), getName());
                simulateOrderProcessing(trade);
                processedOrders.incrementAndGet();
            }
        }
        
        // Method 2: Implementing Runnable
        class OrderProcessorRunnable implements Runnable {
            private final Trade trade;
            
            public OrderProcessorRunnable(Trade trade) {
                this.trade = trade;
            }
            
            @Override
            public void run() {
                logger.info("Processing trade {} in thread {}", trade.id(), Thread.currentThread().getName());
                simulateOrderProcessing(trade);
                processedOrders.incrementAndGet();
            }
        }
        
        // Method 3: Lambda expression
        Trade trade1 = Trade.buy("AAPL", new BigDecimal("150.00"), 100, "client1");
        Trade trade2 = Trade.sell("GOOGL", new BigDecimal("2800.00"), 50, "client2");
        Trade trade3 = Trade.buy("TSLA", new BigDecimal("800.00"), 75, "client3");
        
        // Create threads using different methods
        Thread thread1 = new OrderProcessorThread(trade1);
        Thread thread2 = new Thread(new OrderProcessorRunnable(trade2), "OrderProcessor-" + trade2.id());
        Thread thread3 = new Thread(() -> {
            logger.info("Processing trade {} in lambda thread {}", trade3.id(), Thread.currentThread().getName());
            simulateOrderProcessing(trade3);
            processedOrders.incrementAndGet();
        }, "OrderProcessor-" + trade3.id());
        
        // Start all threads
        thread1.start();
        thread2.start();
        thread3.start();
        
        // Wait for completion
        thread1.join();
        thread2.join();
        thread3.join();
        
        logger.info("All orders processed. Total: {}", processedOrders.get());
    }
    
    /**
     * Demonstrates thread states and lifecycle
     */
    public void demonstrateThreadStates() throws InterruptedException {
        logger.info("=== Demonstrating Thread States ===");
        
        Trade trade = Trade.buy("MSFT", new BigDecimal("300.00"), 200, "client4");
        
        Thread orderThread = new Thread(() -> {
            try {
                logger.info("Thread state in run(): {}", Thread.currentThread().getState());
                
                // Simulate waiting for market data
                synchronized (this) {
                    logger.info("Thread entering WAITING state");
                    wait(1000); // This will actually timeout, but demonstrates TIMED_WAITING
                }
                
                simulateOrderProcessing(trade);
            } catch (InterruptedException e) {
                logger.info("Thread was interrupted during processing");
                Thread.currentThread().interrupt();
            }
        }, "StateDemo-" + trade.id());
        
        logger.info("Thread state after creation: {}", orderThread.getState()); // NEW
        
        orderThread.start();
        logger.info("Thread state after start(): {}", orderThread.getState()); // RUNNABLE
        
        Thread.sleep(100);
        logger.info("Thread state during execution: {}", orderThread.getState()); // Could be RUNNABLE or TIMED_WAITING
        
        orderThread.join();
        logger.info("Thread state after completion: {}", orderThread.getState()); // TERMINATED
    }
    
    /**
     * Demonstrates thread interruption for order cancellation
     */
    public void demonstrateThreadInterruption() throws InterruptedException {
        logger.info("=== Demonstrating Thread Interruption ===");
        
        Trade trade = Trade.buy("NVDA", new BigDecimal("500.00"), 300, "client5");
        
        Thread longRunningOrder = new Thread(() -> {
            logger.info("Starting long-running order processing for trade {}", trade.id());
            
            try {
                for (int i = 0; i < 10; i++) {
                    // Check for interruption
                    if (Thread.currentThread().isInterrupted()) {
                        logger.info("Order processing interrupted for trade {}", trade.id());
                        return;
                    }
                    
                    logger.info("Processing step {} for trade {}", i + 1, trade.id());
                    Thread.sleep(300); // This will throw InterruptedException if interrupted
                }
                
                logger.info("Order processing completed for trade {}", trade.id());
                
            } catch (InterruptedException e) {
                logger.info("Order processing interrupted via exception for trade {}", trade.id());
                Thread.currentThread().interrupt(); // Restore interrupted status
            }
        }, "LongOrder-" + trade.id());
        
        longRunningOrder.start();
        
        // Let it run for a bit
        Thread.sleep(1000);
        
        // Interrupt the thread (simulate order cancellation)
        logger.info("Cancelling order - interrupting thread");
        longRunningOrder.interrupt();
        
        longRunningOrder.join();
        logger.info("Order cancellation handling complete");
    }
    
    /**
     * Demonstrates common thread pitfalls
     */
    public void demonstrateThreadPitfalls() throws InterruptedException {
        logger.info("=== Demonstrating Thread Pitfalls ===");
        
        Trade trade = Trade.sell("AMD", new BigDecimal("100.00"), 150, "client6");
        
        // Pitfall 1: Calling run() instead of start()
        logger.info("Pitfall 1: Calling run() directly (synchronous execution)");
        Thread wrongThread = new Thread(() -> {
            logger.info("This runs in thread: {}", Thread.currentThread().getName());
            simulateOrderProcessing(trade);
        }, "WrongUsage-" + trade.id());
        
        logger.info("Current thread before run(): {}", Thread.currentThread().getName());
        wrongThread.run(); // This runs synchronously in the current thread!
        logger.info("Current thread after run(): {}", Thread.currentThread().getName());
        
        // Correct way
        logger.info("Correct way: Calling start() (asynchronous execution)");
        Thread correctThread = new Thread(() -> {
            logger.info("This runs in thread: {}", Thread.currentThread().getName());
            simulateOrderProcessing(trade);
        }, "CorrectUsage-" + trade.id());
        
        correctThread.start();
        correctThread.join();
        
        // Pitfall 2: Not handling InterruptedException properly
        logger.info("Pitfall 2: Poor interrupt handling");
        Thread poorInterruptHandling = new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // BAD: Swallowing the exception without restoring interrupt status
                logger.warn("Interrupted, but not handling properly");
                // Should do: Thread.currentThread().interrupt();
            }
        }, "PoorInterrupt");
        
        poorInterruptHandling.start();
        poorInterruptHandling.interrupt();
        poorInterruptHandling.join();
    }
    
    /**
     * Demonstrates proper thread management with resource cleanup
     */
    public void demonstrateProperThreadManagement() throws InterruptedException {
        logger.info("=== Demonstrating Proper Thread Management ===");
        
        class ManagedOrderProcessor extends Thread {
            private volatile boolean shutdown = false;
            private final Trade trade;
            
            public ManagedOrderProcessor(Trade trade) {
                super("ManagedProcessor-" + trade.id());
                this.trade = trade;
            }
            
            @Override
            public void run() {
                try {
                    logger.info("Starting managed processing for trade {}", trade.id());
                    
                    while (!shutdown && !isInterrupted()) {
                        // Do some work
                        logger.info("Processing chunk for trade {}", trade.id());
                        Thread.sleep(200);
                        
                        // Simulate completion condition
                        if (Math.random() > 0.7) {
                            break;
                        }
                    }
                    
                } catch (InterruptedException e) {
                    logger.info("Processing interrupted for trade {}", trade.id());
                    Thread.currentThread().interrupt();
                } finally {
                    // Cleanup resources
                    logger.info("Cleaning up resources for trade {}", trade.id());
                }
            }
            
            public void shutdown() {
                shutdown = true;
                interrupt();
            }
        }
        
        Trade trade = Trade.buy("INTC", new BigDecimal("50.00"), 500, "client7");
        ManagedOrderProcessor processor = new ManagedOrderProcessor(trade);
        
        processor.start();
        
        // Let it run for a bit
        Thread.sleep(800);
        
        // Graceful shutdown
        processor.shutdown();
        processor.join(2000); // Wait up to 2 seconds
        
        if (processor.isAlive()) {
            logger.warn("Thread didn't shutdown gracefully");
        } else {
            logger.info("Thread shutdown gracefully");
        }
    }
    
    private void simulateOrderProcessing(Trade trade) {
        try {
            // Simulate processing time
            Thread.sleep((long) (200 + Math.random() * 300));
            logger.info("Completed processing trade: {} - {} {} shares of {} at ${}",
                trade.id(), trade.type(), trade.quantity(), trade.symbol(), trade.price());
        } catch (InterruptedException e) {
            logger.info("Order processing interrupted for trade {}", trade.id());
            Thread.currentThread().interrupt();
        }
    }
    
    public void runAllDemos() throws InterruptedException {
        demonstrateThreadCreation();
        TimeUnit.SECONDS.sleep(1);
        
        demonstrateThreadStates();
        TimeUnit.SECONDS.sleep(1);
        
        demonstrateThreadInterruption();
        TimeUnit.SECONDS.sleep(1);
        
        demonstrateThreadPitfalls();
        TimeUnit.SECONDS.sleep(1);
        
        demonstrateProperThreadManagement();
        
        logger.info("Total orders processed in this demo: {}", processedOrders.get());
    }
}
