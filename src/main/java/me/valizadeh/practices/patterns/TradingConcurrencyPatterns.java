package me.valizadeh.practices.patterns;

import me.valizadeh.practices.model.Trade;
import me.valizadeh.practices.model.TradeStatus;
import me.valizadeh.practices.model.TradeType;
import me.valizadeh.practices.model.MarketData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;


/**
 * Demonstrates common concurrency patterns in trading systems:
 * Producer-Consumer, Work Stealing, Future/Promise, Actor Model, Immutable Objects, Thread Pool Pattern
 */
public class TradingConcurrencyPatterns {
    private static final Logger logger = LoggerFactory.getLogger(TradingConcurrencyPatterns.class);
    
    /**
     * Demonstrates Producer-Consumer pattern for order processing
     */
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
                    
                    orderQueue.put(trade); // Blocks if queue is full
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
                            Thread.sleep((int) (200 + Math.random() * 300));
                            
                            processedOrders.incrementAndGet();
                            logger.info("✅ Consumer-{}: Completed order {}", consumerId, trade.id());
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                logger.info("🔚 Consumer-{} finished", consumerId);
            });
        }
        
        orderProducer.start();
        orderProducer.join();
        
        consumers.shutdown();
        consumers.awaitTermination(10, TimeUnit.SECONDS);
        
        logger.info("Producer-Consumer completed. Processed orders: {}", processedOrders.get());
    }
    
    /**
     * Demonstrates Work Stealing pattern with ForkJoinPool
     */
    public void demonstrateWorkStealing() throws InterruptedException {
        logger.info("=== Demonstrating Work Stealing Pattern ===");
        
        ForkJoinPool workStealingPool = new ForkJoinPool(4);
        
        List<Trade> largeBatch = generateTrades(20);
        
        try {
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
     * Demonstrates Future/Promise pattern for async trade execution
     */
    public void demonstrateFuturePromise() throws InterruptedException, ExecutionException {
        logger.info("=== Demonstrating Future/Promise Pattern ===");
        
        ExecutorService executor = Executors.newFixedThreadPool(4);
        
        // Submit multiple trades for async execution
        List<Future<Trade>> tradeFutures = new ArrayList<>();
        
        for (int i = 0; i < 8; i++) {
            final int tradeId = i;
            
            Future<Trade> tradeFuture = executor.submit(() -> {
                Trade trade = Trade.buy("STOCK" + tradeId, new BigDecimal("100"), 50, "client1");
                
                logger.info("🔄 Executing trade: {}", trade.id());
                
                // Simulate execution time
                Thread.sleep((int) (500 + Math.random() * 1000));
                
                // Random success/failure
                if (Math.random() > 0.2) {
                    logger.info("✅ Trade executed successfully: {}", trade.id());
                    return trade.withStatus(TradeStatus.EXECUTED);
                } else {
                    logger.warn("❌ Trade failed: {}", trade.id());
                    return trade.withStatus(TradeStatus.FAILED);
                }
            });
            
            tradeFutures.add(tradeFuture);
        }
        
        // Collect results as they complete
        int executed = 0, failed = 0;
        
        for (Future<Trade> future : tradeFutures) {
            try {
                Trade result = future.get(2, TimeUnit.SECONDS);
                if (result.status() == TradeStatus.EXECUTED) {
                    executed++;
                } else {
                    failed++;
                }
            } catch (TimeoutException e) {
                logger.warn("Trade execution timed out");
                future.cancel(true);
                failed++;
            }
        }
        
        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.SECONDS);
        
        logger.info("Future/Promise pattern completed. Executed: {}, Failed: {}", executed, failed);
    }
    
    /**
     * Demonstrates Actor Model pattern using queues and dedicated threads
     */
    public void demonstrateActorModel() throws InterruptedException {
        logger.info("=== Demonstrating Actor Model Pattern ===");
        
        // Create actors
        PortfolioActor portfolioActor = new PortfolioActor("PortfolioActor");
        RiskManagerActor riskActor = new RiskManagerActor("RiskManagerActor");
        OrderProcessorActor orderActor = new OrderProcessorActor("OrderProcessorActor", portfolioActor, riskActor);
        
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
     * Demonstrates Immutable Objects pattern for thread safety
     */
    public void demonstrateImmutableObjects() throws InterruptedException {
        logger.info("=== Demonstrating Immutable Objects Pattern ===");
        
        // Immutable market data that can be safely shared across threads
        ImmutableMarketDataFeed dataFeed = new ImmutableMarketDataFeed();
        
        ExecutorService readers = Executors.newFixedThreadPool(4);
        
        // Multiple threads reading market data safely
        for (int i = 0; i < 4; i++) {
            final int readerId = i;
            readers.submit(() -> {
                for (int j = 0; j < 5; j++) {
                    MarketData data = dataFeed.getLatestData("AAPL");
                    logger.info("Reader-{}: AAPL data - Price: ${}, Volume: {}", 
                        readerId, data.lastPrice(), data.volume());
                    
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
        }
        
        // Single writer updating market data
        Thread writer = new Thread(() -> {
            for (int i = 0; i < 8; i++) {
                BigDecimal newPrice = new BigDecimal(150 + Math.random() * 20);
                long newVolume = (long) (1000000 + Math.random() * 500000);
                
                dataFeed.updateMarketData("AAPL", newPrice, newVolume);
                logger.info("Writer: Updated AAPL - Price: ${}, Volume: {}", newPrice, newVolume);
                
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "MarketDataWriter");
        
        writer.start();
        
        readers.shutdown();
        readers.awaitTermination(5, TimeUnit.SECONDS);
        writer.join();
        
        logger.info("Immutable objects pattern completed");
    }
    
    /**
     * Demonstrates Thread Pool pattern with custom implementation
     */
    public void demonstrateThreadPoolPattern() throws InterruptedException {
        logger.info("=== Demonstrating Thread Pool Pattern ===");
        
        // Custom thread pool implementation
        CustomTradingThreadPool threadPool = new CustomTradingThreadPool(3);
        
        // Submit various trading tasks
        for (int i = 0; i < 10; i++) {
            final int taskId = i;
            
            threadPool.submit(() -> {
                Trade trade = Trade.buy("TASK" + taskId, new BigDecimal("100"), 10, "client1");
                logger.info("Task-{}: Processing trade {} in thread {}", 
                    taskId, trade.id(), Thread.currentThread().getName());
                
                try {
                    Thread.sleep((int) (300 + Math.random() * 500));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                logger.info("Task-{}: Completed trade {}", taskId, trade.id());
            });
        }
        
        // Monitor thread pool
        Thread monitor = new Thread(() -> {
            try {
                while (!threadPool.isShutdown()) {
                    logger.info("📊 Thread pool stats - Active: {}, Queue size: {}", 
                        threadPool.getActiveThreads(), threadPool.getQueueSize());
                    Thread.sleep(500);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "ThreadPoolMonitor");
        
        monitor.start();
        
        // Let tasks run for a while
        Thread.sleep(3000);
        
        threadPool.shutdown();
        threadPool.awaitTermination(5, TimeUnit.SECONDS);
        
        monitor.interrupt();
        monitor.join();
        
        logger.info("Thread pool pattern completed");
    }
    
    private List<Trade> generateTrades(int count) {
        List<Trade> trades = new ArrayList<>();
        String[] symbols = {"AAPL", "GOOGL", "TSLA", "MSFT", "NVDA"};
        
        for (int i = 0; i < count; i++) {
            String symbol = symbols[i % symbols.length];
            BigDecimal price = new BigDecimal(100 + Math.random() * 500);
            int quantity = (int) (10 + Math.random() * 100);
            TradeType type = (i % 2 == 0) ? TradeType.BUY : TradeType.SELL;
            
            Trade trade = (type == TradeType.BUY) 
                ? Trade.buy(symbol, price, quantity, "client" + (i % 3))
                : Trade.sell(symbol, price, quantity, "client" + (i % 3));
                
            trades.add(trade);
        }
        
        return trades;
    }
    
    public void runAllDemos() throws InterruptedException, ExecutionException {
        demonstrateProducerConsumer();
        TimeUnit.SECONDS.sleep(1);
        
        demonstrateWorkStealing();
        TimeUnit.SECONDS.sleep(1);
        
        demonstrateFuturePromise();
        TimeUnit.SECONDS.sleep(1);
        
        demonstrateActorModel();
        TimeUnit.SECONDS.sleep(1);
        
        demonstrateImmutableObjects();
        TimeUnit.SECONDS.sleep(1);
        
        demonstrateThreadPoolPattern();
    }
    
    // Supporting classes for patterns
    
    /**
     * Work Stealing Task for ForkJoinPool
     */
    private static class TradeProcessingTask extends RecursiveTask<Integer> {
        private static final int THRESHOLD = 3;
        private final List<Trade> trades;
        
        public TradeProcessingTask(List<Trade> trades) {
            this.trades = trades;
        }
        
        @Override
        protected Integer compute() {
            if (trades.size() <= THRESHOLD) {
                // Process directly
                return processTrades(trades);
            }
            
            // Split and fork
            int mid = trades.size() / 2;
            TradeProcessingTask leftTask = new TradeProcessingTask(trades.subList(0, mid));
            TradeProcessingTask rightTask = new TradeProcessingTask(trades.subList(mid, trades.size()));
            
            leftTask.fork(); // Async execution
            int rightResult = rightTask.compute(); // Sync execution
            int leftResult = leftTask.join(); // Wait for async result
            
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
                }
            }
            return trades.size();
        }
    }
    
    /**
     * Actor base class
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
        
        public void tell(Object message) {
            if (running.get()) {
                mailbox.offer(message);
            }
        }
        
        private void run() {
            while (running.get()) {
                try {
                    Object message = mailbox.poll(100, TimeUnit.MILLISECONDS);
                    if (message != null) {
                        receive(message);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        protected abstract void receive(Object message);
    }
    
    // Actor message types
    private record ProcessOrderMessage(Trade trade) {}
    private record QueryPortfolioMessage(String clientId) {}
    private record UpdatePositionMessage(String symbol, int quantity) {}
    private record CheckRiskMessage(Trade trade) {}
    
    // Concrete actors
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
    
    private static class RiskManagerActor extends Actor {
        public RiskManagerActor(String name) {
            super(name);
        }
        
        @Override
        protected void receive(Object message) {
            if (message instanceof CheckRiskMessage riskMsg) {
                Trade trade = riskMsg.trade();
                logger.info("🛡️ RiskManager: Checking risk for trade {}", trade.id());
                
                // Simulate risk check
                boolean approved = Math.random() > 0.1; // 90% approval rate
                logger.info("🛡️ RiskManager: Trade {} {}", trade.id(), 
                    approved ? "APPROVED" : "REJECTED");
            }
        }
    }
    
    /**
     * Immutable Market Data Feed
     */
    private static class ImmutableMarketDataFeed {
        private final AtomicReference<Map<String, MarketData>> dataRef = 
            new AtomicReference<>(new ConcurrentHashMap<>());
        
        public MarketData getLatestData(String symbol) {
            return dataRef.get().get(symbol);
        }
        
        public void updateMarketData(String symbol, BigDecimal price, long volume) {
            Map<String, MarketData> currentData = dataRef.get();
            Map<String, MarketData> newData = new ConcurrentHashMap<>(currentData);
            
            MarketData newMarketData = MarketData.of(symbol, price, price, price, volume);
            newData.put(symbol, newMarketData);
            
            dataRef.set(newData); // Atomic replacement
        }
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
                worker.start();
            }
        }
        
        public void submit(Runnable task) {
            if (!shutdown) {
                taskQueue.offer(task);
            }
        }
        
        private void runWorker() {
            while (!shutdown) {
                try {
                    Runnable task = taskQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (task != null) {
                        activeThreads.incrementAndGet();
                        try {
                            task.run();
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
        
        public void awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            long timeoutMillis = unit.toMillis(timeout);
            long deadline = System.currentTimeMillis() + timeoutMillis;
            
            for (Thread worker : workers) {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining > 0) {
                    worker.join(remaining);
                }
            }
        }
        
        public boolean isShutdown() {
            return shutdown;
        }
        
        public int getActiveThreads() {
            return activeThreads.get();
        }
        
        public int getQueueSize() {
            return taskQueue.size();
        }
    }
}
