package me.valizadeh.practices.jmm;

import me.valizadeh.practices.model.MarketData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

/**
 * Demonstrates Java Memory Model issues and solutions
 * Shows visibility problems and happens-before relationships
 */
public class MarketDataJMMDemo {
    private static final Logger logger = LoggerFactory.getLogger(MarketDataJMMDemo.class);
    
    // Problem: Without volatile, updates may not be visible to other threads
    private boolean marketOpen = false;
    private MarketData latestData;
    
    // Solution: volatile ensures visibility
    private volatile boolean marketOpenVolatile = false;
    private volatile MarketData latestDataVolatile;
    
    // Synchronized provides both mutual exclusion and visibility
    private final Object lock = new Object();
    private boolean marketOpenSync = false;
    private MarketData latestDataSync;
    
    /**
     * Demonstrates stale read problem without proper synchronization
     */
    public void demonstrateStaleReads() throws InterruptedException {
        logger.info("=== Demonstrating Stale Reads (JMM Visibility Issues) ===");
        
        // Writer thread - updates market data
        Thread writer = new Thread(() -> {
            try {
                Thread.sleep(100);
                latestData = MarketData.of("AAPL", 
                    new BigDecimal("150.00"), 
                    new BigDecimal("150.05"), 
                    new BigDecimal("150.02"), 
                    1000000L);
                marketOpen = true;
                logger.info("Writer: Market opened with data: {}", latestData);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        // Reader thread - may not see the update due to JMM
        Thread reader = new Thread(() -> {
            int attempts = 0;
            while (!marketOpen && attempts < 10) {
                attempts++;
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            if (marketOpen) {
                logger.info("Reader: Saw market open after {} attempts. Data: {}", attempts, latestData);
            } else {
                logger.warn("Reader: Never saw market open - visibility issue!");
            }
        });
        
        writer.start();
        reader.start();
        
        writer.join();
        reader.join();
        
        // Reset for next demo
        marketOpen = false;
        latestData = null;
    }
    
    /**
     * Demonstrates proper visibility with volatile
     */
    public void demonstrateVolatileVisibility() throws InterruptedException {
        logger.info("=== Demonstrating Volatile Visibility ===");
        
        Thread writer = new Thread(() -> {
            try {
                Thread.sleep(100);
                latestDataVolatile = MarketData.of("GOOGL", 
                    new BigDecimal("2800.00"), 
                    new BigDecimal("2800.50"), 
                    new BigDecimal("2800.25"), 
                    500000L);
                marketOpenVolatile = true; // volatile write - guarantees visibility
                logger.info("Writer: Market opened with volatile data: {}", latestDataVolatile);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        Thread reader = new Thread(() -> {
            int attempts = 0;
            while (!marketOpenVolatile && attempts < 10) {
                attempts++;
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            if (marketOpenVolatile) {
                logger.info("Reader: Saw volatile market open after {} attempts. Data: {}", 
                    attempts, latestDataVolatile);
            } else {
                logger.warn("Reader: Never saw volatile market open!");
            }
        });
        
        writer.start();
        reader.start();
        
        writer.join();
        reader.join();
        
        // Reset
        marketOpenVolatile = false;
        latestDataVolatile = null;
    }
    
    /**
     * Demonstrates happens-before relationship with synchronized
     */
    public void demonstrateSynchronizedVisibility() throws InterruptedException {
        logger.info("=== Demonstrating Synchronized Happens-Before ===");
        
        Thread writer = new Thread(() -> {
            try {
                Thread.sleep(100);
                synchronized (lock) {
                    latestDataSync = MarketData.of("TSLA", 
                        new BigDecimal("800.00"), 
                        new BigDecimal("800.25"), 
                        new BigDecimal("800.12"), 
                        2000000L);
                    marketOpenSync = true;
                    logger.info("Writer: Market opened with synchronized data: {}", latestDataSync);
                } // Release of lock happens-before subsequent acquisition
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        Thread reader = new Thread(() -> {
            boolean seen = false;
            int attempts = 0;
            while (!seen && attempts < 10) {
                attempts++;
                synchronized (lock) { // Acquiring lock after writer release
                    if (marketOpenSync) {
                        logger.info("Reader: Saw synchronized market open after {} attempts. Data: {}", 
                            attempts, latestDataSync);
                        seen = true;
                    }
                }
                if (!seen) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            if (!seen) {
                logger.warn("Reader: Never saw synchronized market open!");
            }
        });
        
        writer.start();
        reader.start();
        
        writer.join();
        reader.join();
    }
    
    /**
     * Demonstrates happens-before with Thread.join()
     */
    public void demonstrateJoinHappensBefore() throws InterruptedException {
        logger.info("=== Demonstrating Thread.join() Happens-Before ===");
        
        MarketData[] sharedData = new MarketData[1];
        
        Thread dataLoader = new Thread(() -> {
            try {
                Thread.sleep(200);
                sharedData[0] = MarketData.of("MSFT", 
                    new BigDecimal("300.00"), 
                    new BigDecimal("300.10"), 
                    new BigDecimal("300.05"), 
                    800000L);
                logger.info("Data loader: Loaded market data: {}", sharedData[0]);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        dataLoader.start();
        dataLoader.join(); // join() happens-before subsequent reads
        
        // This read is guaranteed to see the write from dataLoader
        logger.info("Main thread: Data after join: {}", sharedData[0]);
    }
    
    public void runAllDemos() throws InterruptedException {
        demonstrateStaleReads();
        TimeUnit.SECONDS.sleep(1);
        
        demonstrateVolatileVisibility();
        TimeUnit.SECONDS.sleep(1);
        
        demonstrateSynchronizedVisibility();
        TimeUnit.SECONDS.sleep(1);
        
        demonstrateJoinHappensBefore();
    }
}
