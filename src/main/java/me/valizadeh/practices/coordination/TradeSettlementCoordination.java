package me.valizadeh.practices.coordination;

import me.valizadeh.practices.model.Trade;
import me.valizadeh.practices.model.TradeStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Demonstrates thread coordination mechanisms in trade settlement scenarios:
 * CountDownLatch, CyclicBarrier, Semaphore, Phaser
 */
public class TradeSettlementCoordination {
    private static final Logger logger = LoggerFactory.getLogger(TradeSettlementCoordination.class);
    
    /**
     * Demonstrates CountDownLatch for waiting until all trade validations complete
     */
    public void demonstrateCountDownLatch() throws InterruptedException {
        logger.info("=== Demonstrating CountDownLatch for Trade Validation ===");
        
        List<Trade> trades = List.of(
            Trade.buy("AAPL", new BigDecimal("150.00"), 100, "client1"),
            Trade.sell("GOOGL", new BigDecimal("2800.00"), 50, "client2"),
            Trade.buy("TSLA", new BigDecimal("800.00"), 75, "client3")
        );
        
        // Wait for all validations to complete before settlement
        CountDownLatch validationLatch = new CountDownLatch(trades.size());
        List<Boolean> validationResults = new ArrayList<>();
        
        // Start validation threads
        for (Trade trade : trades) {
            new Thread(() -> {
                try {
                    boolean isValid = validateTrade(trade);
                    synchronized (validationResults) {
                        validationResults.add(isValid);
                    }
                    logger.info("Validation completed for trade: {} - Valid: {}", trade.id(), isValid);
                } finally {
                    validationLatch.countDown(); // Signal completion
                }
            }, "Validator-" + trade.id()).start();
        }
        
        // Main thread waits for all validations
        logger.info("Waiting for all trade validations to complete...");
        validationLatch.await(); // Block until count reaches zero
        
        logger.info("All validations complete. Results: {}", validationResults);
        
        // Now proceed with settlement
        long validTrades = validationResults.stream().mapToLong(v -> v ? 1 : 0).sum();
        logger.info("Proceeding to settle {} valid trades", validTrades);
    }
    
    /**
     * Demonstrates CountDownLatch with timeout
     */
    public void demonstrateCountDownLatchWithTimeout() throws InterruptedException {
        logger.info("=== Demonstrating CountDownLatch with Timeout ===");
        
        CountDownLatch riskCheckLatch = new CountDownLatch(2);
        
        // Fast risk check
        new Thread(() -> {
            try {
                Thread.sleep(500);
                logger.info("Fast risk check completed");
                riskCheckLatch.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "FastRiskCheck").start();
        
        // Slow risk check (simulates timeout scenario)
        new Thread(() -> {
            try {
                Thread.sleep(3000); // This will timeout
                logger.info("Slow risk check completed");
                riskCheckLatch.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "SlowRiskCheck").start();
        
        // Wait with timeout
        boolean completed = riskCheckLatch.await(2, TimeUnit.SECONDS);
        
        if (completed) {
            logger.info("All risk checks completed within timeout");
        } else {
            logger.warn("Risk checks timed out! Current count: {}", riskCheckLatch.getCount());
        }
    }
    
    /**
     * Demonstrates CyclicBarrier for coordinated settlement phases
     */
    public void demonstrateCyclicBarrier() throws InterruptedException {
        logger.info("=== Demonstrating CyclicBarrier for Settlement Phases ===");
        
        final int NUM_SETTLEMENT_WORKERS = 3;
        
        // Barrier action runs when all parties reach the barrier
        CyclicBarrier settlementBarrier = new CyclicBarrier(NUM_SETTLEMENT_WORKERS, () -> {
            logger.info("🔄 All settlement workers synchronized - starting next phase");
        });
        
        List<Trade> trades = List.of(
            Trade.buy("MSFT", new BigDecimal("300.00"), 200, "client1"),
            Trade.sell("NVDA", new BigDecimal("500.00"), 100, "client2"),
            Trade.buy("AMD", new BigDecimal("100.00"), 300, "client3")
        );
        
        // Settlement workers that need to synchronize at each phase
        Thread[] workers = new Thread[NUM_SETTLEMENT_WORKERS];
        
        for (int i = 0; i < NUM_SETTLEMENT_WORKERS; i++) {
            final int workerId = i;
            final Trade trade = trades.get(i);
            
            workers[i] = new Thread(() -> {
                try {
                    // Phase 1: Pre-settlement validation
                    logger.info("Worker-{}: Starting pre-settlement validation for {}", workerId, trade.id());
                    Thread.sleep((long) (500 + Math.random() * 500));
                    logger.info("Worker-{}: Pre-settlement validation complete", workerId);
                    
                    settlementBarrier.await(); // Wait for all workers
                    
                    // Phase 2: Settlement execution
                    logger.info("Worker-{}: Starting settlement execution for {}", workerId, trade.id());
                    Thread.sleep((long) (300 + Math.random() * 400));
                    logger.info("Worker-{}: Settlement execution complete", workerId);
                    
                    settlementBarrier.await(); // Synchronize again
                    
                    // Phase 3: Post-settlement reporting
                    logger.info("Worker-{}: Starting post-settlement reporting for {}", workerId, trade.id());
                    Thread.sleep((long) (200 + Math.random() * 300));
                    logger.info("Worker-{}: Post-settlement reporting complete", workerId);
                    
                    settlementBarrier.await(); // Final synchronization
                    
                    logger.info("Worker-{}: All phases complete for {}", workerId, trade.id());
                    
                } catch (InterruptedException | BrokenBarrierException e) {
                    logger.error("Worker-{} interrupted or barrier broken", workerId, e);
                    Thread.currentThread().interrupt();
                }
            }, "SettlementWorker-" + workerId);
        }
        
        // Start all workers
        for (Thread worker : workers) {
            worker.start();
        }
        
        // Wait for completion
        for (Thread worker : workers) {
            worker.join();
        }
        
        logger.info("All settlement phases completed successfully");
    }
    
    /**
     * Demonstrates Semaphore for limiting concurrent market data connections
     */
    public void demonstrateSemaphore() throws InterruptedException {
        logger.info("=== Demonstrating Semaphore for Market Data Connections ===");
        
        // Limit to 3 concurrent market data connections
        Semaphore marketDataConnections = new Semaphore(3);
        
        List<String> symbols = List.of("AAPL", "GOOGL", "TSLA", "MSFT", "NVDA", "AMD", "INTC");
        
        Thread[] dataFetchers = new Thread[symbols.size()];
        
        for (int i = 0; i < symbols.size(); i++) {
            final String symbol = symbols.get(i);
            
            dataFetchers[i] = new Thread(() -> {
                try {
                    logger.info("Requesting market data connection for {}", symbol);
                    
                    marketDataConnections.acquire(); // Block if no permits available
                    logger.info("✅ Market data connection acquired for {} (Available permits: {})", 
                        symbol, marketDataConnections.availablePermits());
                    
                    // Simulate market data fetching
                    Thread.sleep((long) (1000 + Math.random() * 1000));
                    
                    logger.info("Market data fetched for {}", symbol);
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    marketDataConnections.release();
                    logger.info("🔓 Market data connection released for {} (Available permits: {})", 
                        symbol, marketDataConnections.availablePermits());
                }
            }, "DataFetcher-" + symbol);
        }
        
        // Start all fetchers
        for (Thread fetcher : dataFetchers) {
            fetcher.start();
        }
        
        // Wait for completion
        for (Thread fetcher : dataFetchers) {
            fetcher.join();
        }
        
        logger.info("All market data fetching completed");
    }
    
    /**
     * Demonstrates Semaphore with tryAcquire for non-blocking behavior
     */
    public void demonstrateSemaphoreNonBlocking() throws InterruptedException {
        logger.info("=== Demonstrating Non-blocking Semaphore ===");
        
        Semaphore riskAnalysisSlots = new Semaphore(2);
        AtomicInteger completedAnalyses = new AtomicInteger(0);
        AtomicInteger rejectedAnalyses = new AtomicInteger(0);
        
        List<Trade> trades = List.of(
            Trade.buy("AAPL", new BigDecimal("150.00"), 1000, "client1"),
            Trade.sell("GOOGL", new BigDecimal("2800.00"), 500, "client2"),
            Trade.buy("TSLA", new BigDecimal("800.00"), 750, "client3"),
            Trade.sell("MSFT", new BigDecimal("300.00"), 600, "client4"),
            Trade.buy("NVDA", new BigDecimal("500.00"), 400, "client5")
        );
        
        Thread[] riskAnalyzers = new Thread[trades.size()];
        
        for (int i = 0; i < trades.size(); i++) {
            final Trade trade = trades.get(i);
            
            riskAnalyzers[i] = new Thread(() -> {
                try {
                    // Try to acquire permit with timeout
                    if (riskAnalysisSlots.tryAcquire(500, TimeUnit.MILLISECONDS)) {
                        try {
                            logger.info("🔍 Risk analysis started for trade: {}", trade.id());
                            Thread.sleep((long) (800 + Math.random() * 400));
                            logger.info("✅ Risk analysis completed for trade: {}", trade.id());
                            completedAnalyses.incrementAndGet();
                        } finally {
                            riskAnalysisSlots.release();
                        }
                    } else {
                        logger.warn("❌ Risk analysis rejected for trade: {} - no capacity", trade.id());
                        rejectedAnalyses.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "RiskAnalyzer-" + trade.id());
        }
        
        // Start all analyzers
        for (Thread analyzer : riskAnalyzers) {
            analyzer.start();
        }
        
        // Wait for completion
        for (Thread analyzer : riskAnalyzers) {
            analyzer.join();
        }
        
        logger.info("Risk analysis summary - Completed: {}, Rejected: {}", 
            completedAnalyses.get(), rejectedAnalyses.get());
    }
    
    /**
     * Demonstrates Phaser for multi-phase settlement workflow
     */
    public void demonstratePhaser() throws InterruptedException {
        logger.info("=== Demonstrating Phaser for Multi-phase Settlement ===");
        
        final int NUM_PARTICIPANTS = 4;
        Phaser settlementPhaser = new Phaser(NUM_PARTICIPANTS);
        
        List<Trade> trades = List.of(
            Trade.buy("AAPL", new BigDecimal("150.00"), 100, "client1"),
            Trade.sell("GOOGL", new BigDecimal("2800.00"), 50, "client2"),
            Trade.buy("TSLA", new BigDecimal("800.00"), 75, "client3"),
            Trade.sell("MSFT", new BigDecimal("300.00"), 120, "client4")
        );
        
        Thread[] participants = new Thread[NUM_PARTICIPANTS];
        
        for (int i = 0; i < NUM_PARTICIPANTS; i++) {
            final int participantId = i;
            final Trade trade = trades.get(i);
            
            participants[i] = new Thread(() -> {
                try {
                    // Phase 0: Trade Validation
                    logger.info("Participant-{}: Phase 0 - Validating trade {}", participantId, trade.id());
                    Thread.sleep((long) (300 + Math.random() * 200));
                    logger.info("Participant-{}: Phase 0 complete", participantId);
                    
                    int phase = settlementPhaser.arriveAndAwaitAdvance(); // Wait for phase 0 completion
                    
                    // Phase 1: Risk Check
                    logger.info("Participant-{}: Phase {} - Risk checking trade {}", participantId, phase, trade.id());
                    Thread.sleep((long) (400 + Math.random() * 300));
                    logger.info("Participant-{}: Phase {} complete", participantId, phase);
                    
                    phase = settlementPhaser.arriveAndAwaitAdvance(); // Wait for phase 1 completion
                    
                    // Phase 2: Settlement
                    logger.info("Participant-{}: Phase {} - Settling trade {}", participantId, phase, trade.id());
                    Thread.sleep((long) (500 + Math.random() * 400));
                    logger.info("Participant-{}: Phase {} complete", participantId, phase);
                    
                    settlementPhaser.arriveAndDeregister(); // Complete and leave
                    
                } catch (Exception e) {
                    logger.error("Participant-{} error", participantId, e);
                }
            }, "Participant-" + participantId);
        }
        
        // Start all participants
        for (Thread participant : participants) {
            participant.start();
        }
        
        // Monitor phaser state
        Thread monitor = new Thread(() -> {
            while (!settlementPhaser.isTerminated()) {
                try {
                    Thread.sleep(200);
                    logger.info("📊 Phaser state - Phase: {}, Registered: {}, Arrived: {}", 
                        settlementPhaser.getPhase(),
                        settlementPhaser.getRegisteredParties(),
                        settlementPhaser.getArrivedParties());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "PhaserMonitor");
        
        monitor.start();
        
        // Wait for completion
        for (Thread participant : participants) {
            participant.join();
        }
        
        monitor.interrupt();
        monitor.join();
        
        logger.info("Multi-phase settlement completed. Phaser terminated: {}", settlementPhaser.isTerminated());
    }
    
    /**
     * Demonstrates wait/notify (legacy object monitors)
     */
    public void demonstrateWaitNotify() throws InterruptedException {
        logger.info("=== Demonstrating wait/notify for Trade Processing ===");
        
        final Object tradeQueue = new Object();
        final List<Trade> pendingTrades = new ArrayList<>();
        
        // Producer - adds trades to queue
        Thread tradeProducer = new Thread(() -> {
            List<Trade> newTrades = List.of(
                Trade.buy("AAPL", new BigDecimal("150.00"), 100, "client1"),
                Trade.sell("GOOGL", new BigDecimal("2800.00"), 50, "client2"),
                Trade.buy("TSLA", new BigDecimal("800.00"), 75, "client3")
            );
            
            for (Trade trade : newTrades) {
                synchronized (tradeQueue) {
                    pendingTrades.add(trade);
                    logger.info("📥 Trade added to queue: {}", trade.id());
                    tradeQueue.notify(); // Wake up waiting consumer
                }
                
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            // Signal completion
            synchronized (tradeQueue) {
                pendingTrades.add(null); // Poison pill
                tradeQueue.notify();
            }
        }, "TradeProducer");
        
        // Consumer - processes trades from queue
        Thread tradeConsumer = new Thread(() -> {
            while (true) {
                Trade trade;
                synchronized (tradeQueue) {
                    while (pendingTrades.isEmpty()) {
                        try {
                            logger.info("⏳ Consumer waiting for trades...");
                            tradeQueue.wait(); // Release lock and wait for notification
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                    
                    trade = pendingTrades.remove(0);
                    if (trade == null) {
                        logger.info("🔚 Consumer received termination signal");
                        break;
                    }
                    
                    logger.info("📤 Processing trade: {}", trade.id());
                }
                
                // Process outside synchronized block
                try {
                    Thread.sleep(300);
                    logger.info("✅ Trade processed: {}", trade.id());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "TradeConsumer");
        
        tradeProducer.start();
        tradeConsumer.start();
        
        tradeProducer.join();
        tradeConsumer.join();
        
        logger.info("wait/notify demonstration completed");
    }
    
    private boolean validateTrade(Trade trade) {
        try {
            // Simulate validation time
            Thread.sleep((long) (200 + Math.random() * 300));
            
            // Random validation result (90% success rate)
            return Math.random() > 0.1;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    public void runAllDemos() throws InterruptedException {
        demonstrateCountDownLatch();
        TimeUnit.SECONDS.sleep(1);
        
        demonstrateCountDownLatchWithTimeout();
        TimeUnit.SECONDS.sleep(1);
        
        demonstrateCyclicBarrier();
        TimeUnit.SECONDS.sleep(1);
        
        demonstrateSemaphore();
        TimeUnit.SECONDS.sleep(1);
        
        demonstrateSemaphoreNonBlocking();
        TimeUnit.SECONDS.sleep(1);
        
        demonstratePhaser();
        TimeUnit.SECONDS.sleep(1);
        
        demonstrateWaitNotify();
    }
}
