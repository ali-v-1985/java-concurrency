package me.valizadeh.practices.collections;

import me.valizadeh.practices.model.OrderBookEntry;
import me.valizadeh.practices.model.Trade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Demonstrates concurrent collections in trading system scenarios:
 * ConcurrentHashMap, CopyOnWriteArrayList, BlockingQueues, ConcurrentLinkedQueue
 */
public class TradingConcurrentCollections {
    private static final Logger logger = LoggerFactory.getLogger(TradingConcurrentCollections.class);
    
    /**
     * Demonstrates ConcurrentHashMap for real-time position tracking
     */
    public void demonstrateConcurrentHashMap() throws InterruptedException {
        logger.info("=== Demonstrating ConcurrentHashMap for Position Tracking ===");
        
        // Thread-safe map for tracking positions across multiple trading threads
        ConcurrentHashMap<String, AtomicInteger> positions = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, BigDecimal> portfolioValues = new ConcurrentHashMap<>();
        
        // Initialize some positions
        positions.put("AAPL", new AtomicInteger(0));
        positions.put("GOOGL", new AtomicInteger(0));
        positions.put("TSLA", new AtomicInteger(0));
        
        portfolioValues.put("client1", new BigDecimal("100000"));
        portfolioValues.put("client2", new BigDecimal("150000"));
        portfolioValues.put("client3", new BigDecimal("200000"));
        
        ExecutorService traders = Executors.newFixedThreadPool(4);
        
        // Simulate multiple traders updating positions concurrently
        for (int i = 0; i < 10; i++) {
            final int tradeId = i;
            traders.submit(() -> {
                String symbol = Arrays.asList("AAPL", "GOOGL", "TSLA").get(tradeId % 3);
                int quantity = (int) (10 + Math.random() * 50);
                boolean isBuy = Math.random() > 0.5;
                
                // Atomic update of position
                AtomicInteger position = positions.get(symbol);
                int newPosition = isBuy ? 
                    position.addAndGet(quantity) : 
                    position.addAndGet(-quantity);
                
                logger.info("Trade {}: {} {} shares of {} - New position: {}", 
                    tradeId, isBuy ? "BUY" : "SELL", quantity, symbol, newPosition);
                
                // Demonstrate compute methods
                String clientId = "client" + ((tradeId % 3) + 1);
                BigDecimal tradeValue = new BigDecimal(quantity * 100); // Simplified
                
                portfolioValues.compute(clientId, (key, value) -> {
                    BigDecimal newValue = isBuy ? value.subtract(tradeValue) : value.add(tradeValue);
                    logger.info("Client {} portfolio updated: {} -> {}", key, value, newValue);
                    return newValue;
                });
            });
        }
        
        traders.shutdown();
        traders.awaitTermination(5, TimeUnit.SECONDS);
        
        // Final positions
        logger.info("Final positions:");
        positions.forEach((symbol, position) -> 
            logger.info("{}: {} shares", symbol, position.get()));
        
        logger.info("Final portfolio values:");
        portfolioValues.forEach((client, value) -> 
            logger.info("{}: ${}", client, value));
        
        // Demonstrate advanced ConcurrentHashMap operations
        demonstrateAdvancedConcurrentHashMapOperations(positions);
    }
    
    /**
     * Demonstrates advanced ConcurrentHashMap operations
     */
    private void demonstrateAdvancedConcurrentHashMapOperations(ConcurrentHashMap<String, AtomicInteger> positions) {
        logger.info("=== Advanced ConcurrentHashMap Operations ===");
        
        // Parallel operations (Java 8+)
        long totalShares = positions.values().parallelStream()
            .mapToLong(AtomicInteger::get)
            .sum();
        logger.info("Total shares across all positions: {}", totalShares);
        
        // Search operations
        String highestPosition = positions.search(1, (key, value) -> 
            value.get() > 50 ? key : null);
        if (highestPosition != null) {
            logger.info("Found position > 50 shares: {}", highestPosition);
        }
        
        // Reduce operations
        int maxPosition = positions.reduce(1, 
            (key, value) -> value.get(),
            Integer::max);
        logger.info("Maximum position size: {}", maxPosition);
        
        // ForEach operations
        positions.forEach(1, (key, value) -> {
            if (value.get() < 0) {
                logger.warn("Short position detected: {} = {}", key, value.get());
            }
        });
    }
    
    /**
     * Demonstrates CopyOnWriteArrayList for market data subscribers
     */
    public void demonstrateCopyOnWriteArrayList() throws InterruptedException {
        logger.info("=== Demonstrating CopyOnWriteArrayList for Market Data Subscribers ===");
        
        // Thread-safe list optimized for frequent reads, infrequent writes
        CopyOnWriteArrayList<String> marketDataSubscribers = new CopyOnWriteArrayList<>();
        
        // Add initial subscribers
        marketDataSubscribers.addAll(Arrays.asList(
            "RiskManager", "OrderEngine", "PortfolioTracker"
        ));
        
        // Market data publisher
        Thread publisher = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                // Read operation (very fast on CopyOnWriteArrayList)
                List<String> currentSubscribers = new ArrayList<>(marketDataSubscribers);
                
                BigDecimal price = new BigDecimal(150 + Math.random() * 50);
                
                for (String subscriber : currentSubscribers) {
                    logger.info("📈 Publishing AAPL@${} to subscriber: {}", price, subscriber);
                }
                
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "MarketDataPublisher");
        
        // Dynamic subscriber management
        Thread subscriberManager = new Thread(() -> {
            try {
                Thread.sleep(300);
                
                // Add new subscriber (write operation - creates new array)
                marketDataSubscribers.add("AlgoTrader");
                logger.info("➕ Added new subscriber: AlgoTrader");
                
                Thread.sleep(400);
                
                // Remove subscriber
                marketDataSubscribers.remove("RiskManager");
                logger.info("➖ Removed subscriber: RiskManager");
                
                Thread.sleep(300);
                
                // Add another subscriber
                marketDataSubscribers.add("ReportingEngine");
                logger.info("➕ Added new subscriber: ReportingEngine");
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "SubscriberManager");
        
        publisher.start();
        subscriberManager.start();
        
        publisher.join();
        subscriberManager.join();
        
        logger.info("Final subscribers: {}", marketDataSubscribers);
    }
    
    /**
     * Demonstrates BlockingQueue for producer-consumer order processing
     */
    public void demonstrateBlockingQueues() throws InterruptedException {
        logger.info("=== Demonstrating BlockingQueues for Order Processing ===");
        
        // Different types of blocking queues
        demonstrateArrayBlockingQueue();
        Thread.sleep(1000);
        
        demonstrateLinkedBlockingQueue();
        Thread.sleep(1000);
        
        demonstratePriorityBlockingQueue();
        Thread.sleep(1000);
        
        demonstrateDelayQueue();
    }
    
    private void demonstrateArrayBlockingQueue() throws InterruptedException {
        logger.info("--- ArrayBlockingQueue (Bounded) ---");
        
        BlockingQueue<Trade> orderQueue = new ArrayBlockingQueue<>(5);
        
        // Producer thread
        Thread orderGenerator = new Thread(() -> {
            try {
                for (int i = 0; i < 8; i++) {
                    Trade trade = Trade.buy("STOCK" + i, new BigDecimal("100"), 10, "client1");
                    
                    logger.info("Attempting to add trade: {}", trade.id());
                    
                    // This will block if queue is full
                    boolean added = orderQueue.offer(trade, 1, TimeUnit.SECONDS);
                    if (added) {
                        logger.info("✅ Trade added to queue: {} (Queue size: {})", 
                            trade.id(), orderQueue.size());
                    } else {
                        logger.warn("❌ Trade rejected - queue full: {}", trade.id());
                    }
                    
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "OrderGenerator");
        
        // Consumer thread
        Thread orderProcessor = new Thread(() -> {
            try {
                while (true) {
                    Trade trade = orderQueue.poll(2, TimeUnit.SECONDS);
                    if (trade == null) {
                        logger.info("No more orders to process");
                        break;
                    }
                    
                    logger.info("🔄 Processing trade: {} (Queue size: {})", 
                        trade.id(), orderQueue.size());
                    Thread.sleep(300); // Simulate processing time
                    logger.info("✅ Completed trade: {}", trade.id());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "OrderProcessor");
        
        orderGenerator.start();
        Thread.sleep(200); // Let some orders accumulate
        orderProcessor.start();
        
        orderGenerator.join();
        orderProcessor.join();
    }
    
    private void demonstrateLinkedBlockingQueue() throws InterruptedException {
        logger.info("--- LinkedBlockingQueue (Unbounded/Large capacity) ---");
        
        BlockingQueue<Trade> tradeQueue = new LinkedBlockingQueue<>();
        
        // Multiple producers
        ExecutorService producers = Executors.newFixedThreadPool(2);
        
        for (int p = 0; p < 2; p++) {
            final int producerId = p;
            producers.submit(() -> {
                try {
                    for (int i = 0; i < 4; i++) {
                        Trade trade = Trade.sell("PROD" + producerId + "_" + i, 
                            new BigDecimal("200"), 20, "client" + producerId);
                        
                        tradeQueue.put(trade); // Blocks if necessary (rarely with LinkedBlockingQueue)
                        logger.info("Producer-{} added trade: {}", producerId, trade.id());
                        Thread.sleep(150);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        
        // Single consumer
        Thread consumer = new Thread(() -> {
            try {
                int processed = 0;
                while (processed < 8) {
                    Trade trade = tradeQueue.take(); // Blocks until available
                    logger.info("Consumer processing: {} (Queue size: {})", 
                        trade.id(), tradeQueue.size());
                    Thread.sleep(200);
                    processed++;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "TradeConsumer");
        
        consumer.start();
        
        producers.shutdown();
        producers.awaitTermination(5, TimeUnit.SECONDS);
        consumer.join();
    }
    
    private void demonstratePriorityBlockingQueue() throws InterruptedException {
        logger.info("--- PriorityBlockingQueue (Priority-ordered) ---");
        
        // Priority queue that processes high-value trades first
        PriorityBlockingQueue<Trade> priorityQueue = new PriorityBlockingQueue<>(10,
            Comparator.comparing((Trade t) -> t.price().multiply(new BigDecimal(t.quantity())))
                .reversed()); // Highest value first
        
        // Add trades with different values
        List<Trade> trades = Arrays.asList(
            Trade.buy("LOW", new BigDecimal("10"), 100, "client1"),    // Value: 1000
            Trade.buy("HIGH", new BigDecimal("500"), 200, "client2"),   // Value: 100000
            Trade.buy("MED", new BigDecimal("100"), 150, "client3"),    // Value: 15000
            Trade.buy("VHIGH", new BigDecimal("1000"), 50, "client4")   // Value: 50000
        );
        
        // Add in random order
        Collections.shuffle(trades);
        for (Trade trade : trades) {
            priorityQueue.put(trade);
            logger.info("Added trade: {} (Value: ${})", trade.id(), 
                trade.price().multiply(new BigDecimal(trade.quantity())));
        }
        
        // Process in priority order
        logger.info("Processing trades by priority (highest value first):");
        while (!priorityQueue.isEmpty()) {
            Trade trade = priorityQueue.take();
            BigDecimal value = trade.price().multiply(new BigDecimal(trade.quantity()));
            logger.info("🎯 Processing priority trade: {} (Value: ${})", trade.id(), value);
            Thread.sleep(200);
        }
    }
    
    private void demonstrateDelayQueue() throws InterruptedException {
        logger.info("--- DelayQueue (Time-delayed processing) ---");
        
        DelayQueue<DelayedTrade> delayedQueue = new DelayQueue<>();
        
        // Add trades with different delays
        long now = System.currentTimeMillis();
        
        delayedQueue.put(new DelayedTrade(
            Trade.buy("IMMEDIATE", new BigDecimal("100"), 50, "client1"), 
            now)); // No delay
        delayedQueue.put(new DelayedTrade(
            Trade.buy("DELAYED_1S", new BigDecimal("200"), 75, "client2"), 
            now + 1000)); // 1 second delay
        delayedQueue.put(new DelayedTrade(
            Trade.buy("DELAYED_2S", new BigDecimal("300"), 100, "client3"), 
            now + 2000)); // 2 second delay
        
        logger.info("Added {} delayed trades", delayedQueue.size());
        
        // Process trades as they become available
        Thread processor = new Thread(() -> {
            try {
                while (!delayedQueue.isEmpty()) {
                    DelayedTrade delayedTrade = delayedQueue.take(); // Blocks until delay expires
                    logger.info("⏰ Processing delayed trade: {} at {}", 
                        delayedTrade.getTrade().id(), LocalDateTime.now());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "DelayedProcessor");
        
        processor.start();
        processor.join();
    }
    
    /**
     * Demonstrates ConcurrentLinkedQueue for non-blocking operations
     */
    public void demonstrateConcurrentLinkedQueue() throws InterruptedException {
        logger.info("=== Demonstrating ConcurrentLinkedQueue for Non-blocking Operations ===");
        
        ConcurrentLinkedQueue<OrderBookEntry> orderBook = new ConcurrentLinkedQueue<>();
        AtomicInteger addedOrders = new AtomicInteger(0);
        AtomicInteger processedOrders = new AtomicInteger(0);
        
        ExecutorService orderSubmitters = Executors.newFixedThreadPool(3);
        
        // Multiple threads adding orders non-blocking
        for (int i = 0; i < 3; i++) {
            final int submitterId = i;
            orderSubmitters.submit(() -> {
                for (int j = 0; j < 5; j++) {
                    OrderBookEntry entry = (j % 2 == 0) ?
                        OrderBookEntry.bid("STOCK", new BigDecimal(100 + j), 10, "client" + submitterId) :
                        OrderBookEntry.ask("STOCK", new BigDecimal(105 + j), 10, "client" + submitterId);
                    
                    orderBook.offer(entry); // Non-blocking
                    int count = addedOrders.incrementAndGet();
                    logger.info("Submitter-{} added order {} (Total: {})", submitterId, entry.orderId(), count);
                    
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
        }
        
        // Order processor
        Thread processor = new Thread(() -> {
            while (processedOrders.get() < 15) { // Expected total orders
                OrderBookEntry entry = orderBook.poll(); // Non-blocking
                if (entry != null) {
                    int count = processedOrders.incrementAndGet();
                    logger.info("🔄 Processed {} order: {} @ ${} (Total processed: {})", 
                        entry.side(), entry.symbol(), entry.price(), count);
                } else {
                    // No orders available, brief pause
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }, "OrderBookProcessor");
        
        processor.start();
        
        orderSubmitters.shutdown();
        orderSubmitters.awaitTermination(5, TimeUnit.SECONDS);
        processor.join();
        
        logger.info("Final queue size: {}, Added: {}, Processed: {}", 
            orderBook.size(), addedOrders.get(), processedOrders.get());
    }
    
    public void runAllDemos() throws InterruptedException {
        demonstrateConcurrentHashMap();
        TimeUnit.SECONDS.sleep(1);
        
        demonstrateCopyOnWriteArrayList();
        TimeUnit.SECONDS.sleep(1);
        
        demonstrateBlockingQueues();
        TimeUnit.SECONDS.sleep(1);
        
        demonstrateConcurrentLinkedQueue();
    }
    
    /**
     * Helper class for DelayQueue demonstration
     */
    private static class DelayedTrade implements Delayed {
        private final Trade trade;
        private final long executeTime;
        
        public DelayedTrade(Trade trade, long executeTime) {
            this.trade = trade;
            this.executeTime = executeTime;
        }
        
        public Trade getTrade() {
            return trade;
        }
        
        @Override
        public long getDelay(TimeUnit unit) {
            long delay = executeTime - System.currentTimeMillis();
            return unit.convert(delay, TimeUnit.MILLISECONDS);
        }
        
        @Override
        public int compareTo(Delayed other) {
            return Long.compare(this.executeTime, ((DelayedTrade) other).executeTime);
        }
    }
}
