package me.valizadeh.practices.synchronization;

import me.valizadeh.practices.model.Portfolio;
import me.valizadeh.practices.model.Trade;
import me.valizadeh.practices.model.TradeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.StampedLock;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Demonstrates synchronization primitives: synchronized, volatile, locks
 * in the context of portfolio management
 */
public class PortfolioSynchronization {
    private static final Logger logger = LoggerFactory.getLogger(PortfolioSynchronization.class);
    
    // Volatile for simple flags and references
    private volatile boolean marketHours = true;
    private volatile BigDecimal currentMarketValue = BigDecimal.ZERO;
    
    // Synchronized portfolio manager
    private final SynchronizedPortfolioManager syncManager = new SynchronizedPortfolioManager();
    
    // Lock-based portfolio manager
    private final LockBasedPortfolioManager lockManager = new LockBasedPortfolioManager();
    
    // ReadWrite lock portfolio manager
    private final ReadWriteLockPortfolioManager rwLockManager = new ReadWriteLockPortfolioManager();
    
    // Stamped lock portfolio manager
    private final StampedLockPortfolioManager stampedLockManager = new StampedLockPortfolioManager();
    
    /**
     * Demonstrates volatile keyword for visibility guarantees
     */
    public void demonstrateVolatile() throws InterruptedException {
        logger.info("=== Demonstrating Volatile Keyword ===");
        
        // Market status updater
        Thread marketUpdater = new Thread(() -> {
            try {
                for (int i = 0; i < 5; i++) {
                    Thread.sleep(200);
                    marketHours = !marketHours;
                    currentMarketValue = currentMarketValue.add(new BigDecimal("1000"));
                    logger.info("Market status updated: hours={}, value={}", marketHours, currentMarketValue);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        // Market status reader
        Thread marketReader = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                // Volatile reads guarantee visibility of latest writes
                logger.info("Reading market: hours={}, value={}", marketHours, currentMarketValue);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        
        marketUpdater.start();
        marketReader.start();
        
        marketUpdater.join();
        marketReader.join();
    }
    
    /**
     * Demonstrates synchronized methods and blocks
     */
    public void demonstrateSynchronized() throws InterruptedException {
        logger.info("=== Demonstrating Synchronized ===");
        
        Portfolio portfolio = new Portfolio("client1", new BigDecimal("100000"));
        
        // Multiple threads executing trades concurrently
        Thread[] traders = new Thread[3];
        for (int i = 0; i < traders.length; i++) {
            final int traderId = i;
            traders[i] = new Thread(() -> {
                for (int j = 0; j < 5; j++) {
                    Trade trade = Trade.buy("AAPL", new BigDecimal("150"), 10, "client1");
                    syncManager.executeTrade(portfolio, trade);
                    
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }, "Trader-" + traderId);
        }
        
        for (Thread trader : traders) {
            trader.start();
        }
        
        for (Thread trader : traders) {
            trader.join();
        }
        
        logger.info("Final portfolio state: {}", portfolio);
    }
    
    /**
     * Demonstrates ReentrantLock usage
     */
    public void demonstrateReentrantLock() throws InterruptedException {
        logger.info("=== Demonstrating ReentrantLock ===");
        
        Portfolio portfolio = new Portfolio("client2", new BigDecimal("50000"));
        
        Thread[] traders = new Thread[2];
        for (int i = 0; i < traders.length; i++) {
            final int traderId = i;
            traders[i] = new Thread(() -> {
                for (int j = 0; j < 3; j++) {
                    Trade trade = (j % 2 == 0) 
                        ? Trade.buy("GOOGL", new BigDecimal("2800"), 1, "client2")
                        : Trade.sell("GOOGL", new BigDecimal("2810"), 1, "client2");
                    
                    try {
                        lockManager.executeTrade(portfolio, trade);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    
                    try {
                        Thread.sleep(150);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }, "LockTrader-" + traderId);
        }
        
        for (Thread trader : traders) {
            trader.start();
        }
        
        for (Thread trader : traders) {
            trader.join();
        }
        
        logger.info("Final portfolio with ReentrantLock: {}", portfolio);
    }
    
    /**
     * Demonstrates ReadWriteLock for read-heavy scenarios
     */
    public void demonstrateReadWriteLock() throws InterruptedException {
        logger.info("=== Demonstrating ReadWriteLock ===");
        
        Portfolio portfolio = new Portfolio("client3", new BigDecimal("75000"));
        
        // Many readers, few writers
        Thread writer = new Thread(() -> {
            for (int i = 0; i < 3; i++) {
                Trade trade = Trade.buy("TSLA", new BigDecimal("800"), 5, "client3");
                rwLockManager.executeTrade(portfolio, trade);
                
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "Writer");
        
        Thread[] readers = new Thread[4];
        for (int i = 0; i < readers.length; i++) {
            final int readerId = i;
            readers[i] = new Thread(() -> {
                for (int j = 0; j < 8; j++) {
                    BigDecimal value = rwLockManager.getPortfolioValue(portfolio);
                    logger.info("Reader-{} read portfolio value: {}", readerId, value);
                    
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }, "Reader-" + readerId);
        }
        
        writer.start();
        for (Thread reader : readers) {
            reader.start();
        }
        
        writer.join();
        for (Thread reader : readers) {
            reader.join();
        }
    }
    
    /**
     * Demonstrates StampedLock for optimistic reading
     */
    public void demonstrateStampedLock() throws InterruptedException {
        logger.info("=== Demonstrating StampedLock ===");
        
        Portfolio portfolio = new Portfolio("client4", new BigDecimal("60000"));
        
        Thread writer = new Thread(() -> {
            for (int i = 0; i < 2; i++) {
                Trade trade = Trade.buy("NVDA", new BigDecimal("500"), 3, "client4");
                stampedLockManager.executeTrade(portfolio, trade);
                
                try {
                    Thread.sleep(400);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "StampedWriter");
        
        Thread[] optimisticReaders = new Thread[3];
        for (int i = 0; i < optimisticReaders.length; i++) {
            final int readerId = i;
            optimisticReaders[i] = new Thread(() -> {
                for (int j = 0; j < 6; j++) {
                    BigDecimal value = stampedLockManager.getPortfolioValueOptimistic(portfolio);
                    logger.info("OptimisticReader-{} read value: {}", readerId, value);
                    
                    try {
                        Thread.sleep(120);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }, "OptimisticReader-" + readerId);
        }
        
        writer.start();
        for (Thread reader : optimisticReaders) {
            reader.start();
        }
        
        writer.join();
        for (Thread reader : optimisticReaders) {
            reader.join();
        }
    }
    
    public void runAllDemos() throws InterruptedException {
        demonstrateVolatile();
        TimeUnit.SECONDS.sleep(1);
        
        demonstrateSynchronized();
        TimeUnit.SECONDS.sleep(1);
        
        demonstrateReentrantLock();
        TimeUnit.SECONDS.sleep(1);
        
        demonstrateReadWriteLock();
        TimeUnit.SECONDS.sleep(1);
        
        demonstrateStampedLock();
    }
    
    // Inner classes for different synchronization approaches
    
    private static class SynchronizedPortfolioManager {
        
        public synchronized void executeTrade(Portfolio portfolio, Trade trade) {
            logger.info("Executing synchronized trade: {}", trade.id());
            
            BigDecimal tradeValue = trade.price().multiply(new BigDecimal(trade.quantity()));
            
            if (trade.type() == TradeType.BUY) {
                if (portfolio.getCash().compareTo(tradeValue) >= 0) {
                    portfolio.setCash(portfolio.getCash().subtract(tradeValue));
                    portfolio.updatePosition(trade.symbol(), trade.quantity());
                    logger.info("BUY executed: {} shares of {} for ${}", 
                        trade.quantity(), trade.symbol(), tradeValue);
                } else {
                    logger.warn("Insufficient funds for BUY trade: {}", trade.id());
                }
            } else {
                if (portfolio.getPosition(trade.symbol()) >= trade.quantity()) {
                    portfolio.setCash(portfolio.getCash().add(tradeValue));
                    portfolio.updatePosition(trade.symbol(), -trade.quantity());
                    logger.info("SELL executed: {} shares of {} for ${}", 
                        trade.quantity(), trade.symbol(), tradeValue);
                } else {
                    logger.warn("Insufficient shares for SELL trade: {}", trade.id());
                }
            }
        }
    }
    
    private static class LockBasedPortfolioManager {
        private final Lock lock = new ReentrantLock();
        
        public void executeTrade(Portfolio portfolio, Trade trade) throws InterruptedException {
            // Demonstrate tryLock with timeout
            if (lock.tryLock(1, TimeUnit.SECONDS)) {
                try {
                    logger.info("Acquired lock for trade: {}", trade.id());
                    
                    BigDecimal tradeValue = trade.price().multiply(new BigDecimal(trade.quantity()));
                    
                    if (trade.type() == TradeType.BUY) {
                        if (portfolio.getCash().compareTo(tradeValue) >= 0) {
                            portfolio.setCash(portfolio.getCash().subtract(tradeValue));
                            portfolio.updatePosition(trade.symbol(), trade.quantity());
                            logger.info("Lock-based BUY executed: {}", trade.id());
                        }
                    } else {
                        if (portfolio.getPosition(trade.symbol()) >= trade.quantity()) {
                            portfolio.setCash(portfolio.getCash().add(tradeValue));
                            portfolio.updatePosition(trade.symbol(), -trade.quantity());
                            logger.info("Lock-based SELL executed: {}", trade.id());
                        }
                    }
                    
                    // Simulate processing time
                    Thread.sleep(50);
                    
                } finally {
                    lock.unlock();
                    logger.info("Released lock for trade: {}", trade.id());
                }
            } else {
                logger.warn("Could not acquire lock for trade: {} - timeout", trade.id());
            }
        }
    }
    
    private static class ReadWriteLockPortfolioManager {
        private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
        private final Lock readLock = rwLock.readLock();
        private final Lock writeLock = rwLock.writeLock();
        
        public void executeTrade(Portfolio portfolio, Trade trade) {
            writeLock.lock();
            try {
                logger.info("Write lock acquired for trade: {}", trade.id());
                
                BigDecimal tradeValue = trade.price().multiply(new BigDecimal(trade.quantity()));
                
                if (trade.type() == TradeType.BUY) {
                    if (portfolio.getCash().compareTo(tradeValue) >= 0) {
                        portfolio.setCash(portfolio.getCash().subtract(tradeValue));
                        portfolio.updatePosition(trade.symbol(), trade.quantity());
                        logger.info("RW-lock BUY executed: {}", trade.id());
                    }
                } else {
                    if (portfolio.getPosition(trade.symbol()) >= trade.quantity()) {
                        portfolio.setCash(portfolio.getCash().add(tradeValue));
                        portfolio.updatePosition(trade.symbol(), -trade.quantity());
                        logger.info("RW-lock SELL executed: {}", trade.id());
                    }
                }
                
                try {
                    Thread.sleep(100); // Simulate processing
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
            } finally {
                writeLock.unlock();
            }
        }
        
        public BigDecimal getPortfolioValue(Portfolio portfolio) {
            readLock.lock();
            try {
                // Multiple readers can access simultaneously
                BigDecimal totalValue = portfolio.getCash();
                for (var entry : portfolio.getPositions().entrySet()) {
                    // Simulate price lookup
                    BigDecimal estimatedPrice = new BigDecimal("100"); // Simplified
                    totalValue = totalValue.add(estimatedPrice.multiply(new BigDecimal(entry.getValue())));
                }
                
                try {
                    Thread.sleep(50); // Simulate calculation time
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                return totalValue;
            } finally {
                readLock.unlock();
            }
        }
    }
    
    private static class StampedLockPortfolioManager {
        private final StampedLock stampedLock = new StampedLock();
        
        public void executeTrade(Portfolio portfolio, Trade trade) {
            long stamp = stampedLock.writeLock();
            try {
                logger.info("Stamped write lock acquired for trade: {}", trade.id());
                
                BigDecimal tradeValue = trade.price().multiply(new BigDecimal(trade.quantity()));
                
                if (trade.type() == TradeType.BUY) {
                    if (portfolio.getCash().compareTo(tradeValue) >= 0) {
                        portfolio.setCash(portfolio.getCash().subtract(tradeValue));
                        portfolio.updatePosition(trade.symbol(), trade.quantity());
                        logger.info("Stamped BUY executed: {}", trade.id());
                    }
                } else {
                    if (portfolio.getPosition(trade.symbol()) >= trade.quantity()) {
                        portfolio.setCash(portfolio.getCash().add(tradeValue));
                        portfolio.updatePosition(trade.symbol(), -trade.quantity());
                        logger.info("Stamped SELL executed: {}", trade.id());
                    }
                }
                
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
            } finally {
                stampedLock.unlockWrite(stamp);
            }
        }
        
        public BigDecimal getPortfolioValueOptimistic(Portfolio portfolio) {
            long stamp = stampedLock.tryOptimisticRead();
            
            // Read data optimistically
            BigDecimal cash = portfolio.getCash();
            var positions = portfolio.getPositions();
            
            // Check if the optimistic read was valid
            if (!stampedLock.validate(stamp)) {
                // Fall back to regular read lock
                logger.info("Optimistic read failed, falling back to read lock");
                stamp = stampedLock.readLock();
                try {
                    cash = portfolio.getCash();
                    positions = portfolio.getPositions();
                } finally {
                    stampedLock.unlockRead(stamp);
                }
            } else {
                logger.info("Optimistic read succeeded");
            }
            
            // Calculate total value
            BigDecimal totalValue = cash;
            for (var entry : positions.entrySet()) {
                BigDecimal estimatedPrice = new BigDecimal("100");
                totalValue = totalValue.add(estimatedPrice.multiply(new BigDecimal(entry.getValue())));
            }
            
            return totalValue;
        }
    }
}
