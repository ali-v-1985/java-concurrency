package me.valizadeh.practices.async;

import me.valizadeh.practices.model.MarketData;
import me.valizadeh.practices.model.Trade;
import me.valizadeh.practices.model.TradeStatus;
import me.valizadeh.practices.model.TradeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Demonstrates async programming with CompletableFuture in trading scenarios
 * Covers creation, composition, combination, exception handling, and timeouts
 */
public class AsyncTradingOperations {
    private static final Logger logger = LoggerFactory.getLogger(AsyncTradingOperations.class);
    
    /**
     * Demonstrates basic CompletableFuture creation and completion
     */
    public void demonstrateBasicCompletableFuture() throws InterruptedException, ExecutionException, TimeoutException {
        logger.info("=== Demonstrating Basic CompletableFuture ===");
        
        // Creating CompletableFuture in different ways
        
        // 1. Already completed future
        CompletableFuture<String> completedFuture = CompletableFuture.completedFuture("MARKET_OPEN");
        logger.info("Completed future result: {}", completedFuture.join());
        
        // 2. Async supplier
        CompletableFuture<MarketData> marketDataFuture = CompletableFuture.supplyAsync(() -> {
            logger.info("Fetching market data in thread: {}", Thread.currentThread().getName());
            simulateDelay(500);
            return MarketData.of("AAPL", 
                new BigDecimal("149.00"), 
                new BigDecimal("149.05"), 
                new BigDecimal("149.02"), 
                1000000L);
        });
        
        // 3. Manual completion
        CompletableFuture<String> manualFuture = new CompletableFuture<>();
        
        // Complete it from another thread
        CompletableFuture.runAsync(() -> {
            simulateDelay(300);
            manualFuture.complete("RISK_CHECK_PASSED");
        });
        
        // Wait for results
        MarketData data = marketDataFuture.get(2, TimeUnit.SECONDS);
        logger.info("Market data retrieved: {}", data);
        
        String riskResult = manualFuture.get(1, TimeUnit.SECONDS);
        logger.info("Risk check result: {}", riskResult);
    }
    
    /**
     * Demonstrates CompletableFuture chaining and transformation
     */
    public void demonstrateAsyncChaining() throws InterruptedException, ExecutionException {
        logger.info("=== Demonstrating CompletableFuture Chaining ===");
        
        // Complex trading workflow with chaining
        CompletableFuture<Trade> tradeExecutionFlow = CompletableFuture
            // Step 1: Fetch market data
            .supplyAsync(() -> {
                logger.info("🔍 Fetching market data...");
                simulateDelay(200);
                return MarketData.of("GOOGL", 
                    new BigDecimal("2800.00"), 
                    new BigDecimal("2801.00"), 
                    new BigDecimal("2800.50"), 
                    500000L);
            })
            // Step 2: Validate market conditions
            .thenApply(marketData -> {
                logger.info("📊 Validating market conditions for {}", marketData.symbol());
                simulateDelay(150);
                if (marketData.volume() < 100000) {
                    throw new RuntimeException("Insufficient liquidity");
                }
                return marketData;
            })
            // Step 3: Calculate optimal price
            .thenApply(marketData -> {
                logger.info("💰 Calculating optimal price...");
                simulateDelay(100);
                BigDecimal optimalPrice = marketData.bid().add(marketData.ask()).divide(new BigDecimal("2"));
                return new Object[]{marketData.symbol(), optimalPrice};
            })
            // Step 4: Create trade order
            .thenApply(priceData -> {
                String symbol = (String) priceData[0];
                BigDecimal price = (BigDecimal) priceData[1];
                logger.info("📝 Creating trade order for {} at ${}", symbol, price);
                simulateDelay(100);
                return Trade.buy(symbol, price, 100, "client1");
            })
            // Step 5: Execute trade
            .thenApply(trade -> {
                logger.info("⚡ Executing trade: {}", trade.id());
                simulateDelay(300);
                return trade.withStatus(TradeStatus.EXECUTED);
            });
        
        // Wait for completion
        Trade executedTrade = tradeExecutionFlow.get();
        logger.info("✅ Trade execution completed: {}", executedTrade);
    }
    
    /**
     * Demonstrates async composition with thenCompose
     */
    public void demonstrateAsyncComposition() throws InterruptedException, ExecutionException {
        logger.info("=== Demonstrating Async Composition ===");
        
        CompletableFuture<String> portfolioAnalysis = CompletableFuture
            .supplyAsync(() -> {
                logger.info("🔍 Analyzing portfolio...");
                simulateDelay(200);
                return "client1";
            })
            .thenCompose(clientId -> {
                // This returns another CompletableFuture
                logger.info("📈 Fetching positions for client: {}", clientId);
                return CompletableFuture.supplyAsync(() -> {
                    simulateDelay(300);
                    return clientId + ":POSITIONS_LOADED";
                });
            })
            .thenCompose(positionData -> {
                logger.info("💹 Calculating portfolio value...");
                return CompletableFuture.supplyAsync(() -> {
                    simulateDelay(250);
                    return positionData + ":VALUE_CALCULATED";
                });
            });
        
        String result = portfolioAnalysis.get();
        logger.info("Portfolio analysis completed: {}", result);
    }
    
    /**
     * Demonstrates combining multiple async operations
     */
    public void demonstrateAsyncCombination() throws InterruptedException, ExecutionException {
        logger.info("=== Demonstrating Async Combination ===");
        
        // Multiple independent async operations
        CompletableFuture<MarketData> aaplData = CompletableFuture.supplyAsync(() -> {
            logger.info("📊 Fetching AAPL data...");
            simulateDelay(300);
            return MarketData.of("AAPL", new BigDecimal("150.00"), new BigDecimal("150.05"), 
                new BigDecimal("150.02"), 1200000L);
        });
        
        CompletableFuture<MarketData> googlData = CompletableFuture.supplyAsync(() -> {
            logger.info("📊 Fetching GOOGL data...");
            simulateDelay(250);
            return MarketData.of("GOOGL", new BigDecimal("2800.00"), new BigDecimal("2801.00"), 
                new BigDecimal("2800.50"), 800000L);
        });
        
        CompletableFuture<String> riskCheck = CompletableFuture.supplyAsync(() -> {
            logger.info("🛡️ Performing risk check...");
            simulateDelay(200);
            return "RISK_APPROVED";
        });
        
        // Combine two futures with thenCombine
        CompletableFuture<String> marketAnalysis = aaplData.thenCombine(googlData, (aapl, googl) -> {
            logger.info("🔍 Analyzing market correlation...");
            simulateDelay(100);
            BigDecimal aaplPrice = aapl.lastPrice();
            BigDecimal googlPrice = googl.lastPrice();
            return String.format("CORRELATION_ANALYSIS:AAPL@%s:GOOGL@%s", aaplPrice, googlPrice);
        });
        
        // Combine all three with allOf
        CompletableFuture<String> tradingDecision = CompletableFuture.allOf(marketAnalysis, riskCheck)
            .thenApply(v -> {
                logger.info("🎯 Making trading decision...");
                String analysis = marketAnalysis.join();
                String risk = riskCheck.join();
                return String.format("DECISION:BUY_SIGNAL:%s:%s", analysis, risk);
            });
        
        String decision = tradingDecision.get();
        logger.info("Trading decision: {}", decision);
    }
    
    /**
     * Demonstrates exception handling in async operations
     */
    public void demonstrateExceptionHandling() throws InterruptedException, ExecutionException, TimeoutException {
        logger.info("=== Demonstrating Exception Handling ===");
        
        // Future that may fail
        CompletableFuture<Trade> riskyTrade = CompletableFuture.supplyAsync(() -> {
            logger.info("🎲 Attempting risky trade...");
            simulateDelay(200);
            
            if (Math.random() > 0.5) {
                throw new RuntimeException("Market volatility too high");
            }
            
            return Trade.buy("VOLATILE_STOCK", new BigDecimal("100"), 50, "client1");
        });
        
        // Handle exceptions with handle()
        CompletableFuture<String> handledResult = riskyTrade.handle((trade, exception) -> {
            if (exception != null) {
                logger.warn("❌ Trade failed: {}", exception.getMessage());
                return "TRADE_FAILED:" + exception.getMessage();
            } else {
                logger.info("✅ Trade succeeded: {}", trade.id());
                return "TRADE_SUCCESS:" + trade.id();
            }
        });
        
        // Handle exceptions with exceptionally()
        CompletableFuture<Trade> recoveredTrade = CompletableFuture.<Trade>supplyAsync(() -> {
            logger.info("🎯 Attempting another risky operation...");
            simulateDelay(150);
            throw new RuntimeException("Connection timeout");
        }).exceptionally(throwable -> {
            logger.warn("🔄 Recovering from error: {}", throwable.getMessage());
            return Trade.buy("SAFE_STOCK", new BigDecimal("50"), 100, "client1");
        });
        
        // Wait for results
        try {
            String result = handledResult.get(2, TimeUnit.SECONDS);
            logger.info("Handled result: {}", result);
            
            Trade recovered = recoveredTrade.get(2, TimeUnit.SECONDS);
            logger.info("Recovered trade: {}", recovered);
        } catch (ExecutionException | TimeoutException e) {
            logger.error("Future execution failed", e);
        }
    }
    
    /**
     * Demonstrates timeout handling
     */
    public void demonstrateTimeoutHandling() {
        logger.info("=== Demonstrating Timeout Handling ===");
        
        // Slow operation that may timeout
        CompletableFuture<String> slowOperation = CompletableFuture.supplyAsync(() -> {
            logger.info("🐌 Starting slow market data fetch...");
            simulateDelay(3000); // 3 seconds
            return "DELAYED_MARKET_DATA";
        });
        
        // Apply timeout with orTimeout (Java 9+)
        CompletableFuture<String> timedOperation = slowOperation
            .orTimeout(1, TimeUnit.SECONDS)
            .exceptionally(throwable -> {
                if (throwable instanceof TimeoutException) {
                    logger.warn("⏰ Operation timed out, using cached data");
                    return "CACHED_MARKET_DATA";
                }
                logger.error("Unexpected error: {}", throwable.getMessage());
                return "ERROR_DATA";
            });
        
        // Alternative approach with completeOnTimeout (Java 9+)
        CompletableFuture<String> alternativeTimeout = CompletableFuture
            .supplyAsync(() -> {
                logger.info("🔄 Attempting alternative slow operation...");
                simulateDelay(2000);
                return "FRESH_DATA";
            })
            .completeOnTimeout("DEFAULT_DATA", 800, TimeUnit.MILLISECONDS);
        
        try {
            String result1 = timedOperation.get();
            logger.info("Timed operation result: {}", result1);
            
            String result2 = alternativeTimeout.get();
            logger.info("Alternative timeout result: {}", result2);
            
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Future failed", e);
        }
    }
    
    /**
     * Demonstrates async operations with custom executor
     */
    public void demonstrateCustomExecutor() throws InterruptedException {
        logger.info("=== Demonstrating Custom Executor ===");
        
        // Custom executor for trading operations
        ExecutorService tradingExecutor = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "TradingWorker-" + System.currentTimeMillis());
            t.setDaemon(false);
            return t;
        });
        
        try {
            // Use custom executor for async operations
            CompletableFuture<String> customAsync = CompletableFuture
                .supplyAsync(() -> {
                    logger.info("🏭 Running in custom executor: {}", Thread.currentThread().getName());
                    simulateDelay(300);
                    return "CUSTOM_EXECUTED";
                }, tradingExecutor)
                .thenApplyAsync(result -> {
                    logger.info("🔄 Continuing in custom executor: {}", Thread.currentThread().getName());
                    simulateDelay(200);
                    return result + ":PROCESSED";
                }, tradingExecutor);
            
            String result = customAsync.get(2, TimeUnit.SECONDS);
            logger.info("Custom executor result: {}", result);
            
        } catch (ExecutionException | TimeoutException e) {
            logger.error("Custom executor operation failed", e);
        } finally {
            tradingExecutor.shutdown();
            if (!tradingExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                tradingExecutor.shutdownNow();
            }
        }
    }
    
    /**
     * Demonstrates anyOf for racing multiple operations
     */
    public void demonstrateAnyOf() throws InterruptedException, ExecutionException {
        logger.info("=== Demonstrating anyOf (Racing Operations) ===");
        
        // Multiple market data sources - use whichever responds first
        CompletableFuture<String> source1 = CompletableFuture.supplyAsync(() -> {
            logger.info("📡 Source 1 fetching data...");
            simulateDelay((int) (300 + Math.random() * 400));
            return "SOURCE1_DATA";
        });
        
        CompletableFuture<String> source2 = CompletableFuture.supplyAsync(() -> {
            logger.info("📡 Source 2 fetching data...");
            simulateDelay((int) (200 + Math.random() * 500));
            return "SOURCE2_DATA";
        });
        
        CompletableFuture<String> source3 = CompletableFuture.supplyAsync(() -> {
            logger.info("📡 Source 3 fetching data...");
            simulateDelay((int) (250 + Math.random() * 450));
            return "SOURCE3_DATA";
        });
        
        // Race all sources - use the fastest
        CompletableFuture<Object> fastest = CompletableFuture.anyOf(source1, source2, source3);
        
        Object fastestResult = fastest.get();
        logger.info("🏆 Fastest source responded with: {}", fastestResult);
    }
    
    /**
     * Demonstrates complex trading workflow with multiple async operations
     */
    public void demonstrateComplexTradingWorkflow() throws InterruptedException, ExecutionException {
        logger.info("=== Demonstrating Complex Trading Workflow ===");
        
        String symbol = "TSLA";
        
        CompletableFuture<String> complexWorkflow = CompletableFuture
            // Parallel data gathering
            .supplyAsync(() -> {
                logger.info("📊 Fetching real-time data for {}", symbol);
                simulateDelay(200);
                return MarketData.of(symbol, new BigDecimal("800.00"), new BigDecimal("800.50"), 
                    new BigDecimal("800.25"), 2000000L);
            })
            .thenCombine(
                CompletableFuture.supplyAsync(() -> {
                    logger.info("📈 Fetching technical indicators for {}", symbol);
                    simulateDelay(250);
                    return "RSI:65.5,MACD:BULLISH,BB:MIDDLE";
                }),
                (marketData, indicators) -> {
                    logger.info("🔍 Combining market data and indicators");
                    return new Object[]{marketData, indicators};
                }
            )
            .thenCompose(combined -> {
                MarketData data = (MarketData) combined[0];
                String indicators = (String) combined[1];
                
                logger.info("🧠 Running AI analysis...");
                return CompletableFuture.supplyAsync(() -> {
                    simulateDelay(400);
                    return String.format("AI_SIGNAL:BUY:%s:CONFIDENCE:85%%", data.symbol());
                });
            })
            .thenCombine(
                CompletableFuture.supplyAsync(() -> {
                    logger.info("🛡️ Performing portfolio risk check...");
                    simulateDelay(150);
                    return "RISK_APPROVED:MAX_POSITION:1000";
                }),
                (aiSignal, riskCheck) -> {
                    logger.info("🎯 Finalizing trading decision");
                    return String.format("EXECUTE_TRADE:%s:%s", aiSignal, riskCheck);
                }
            )
            .thenApply(decision -> {
                logger.info("✅ Workflow completed: {}", decision);
                return decision;
            });
        
        String finalDecision = complexWorkflow.get();
        logger.info("Complex workflow result: {}", finalDecision);
    }
    
    private void simulateDelay(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted", e);
        }
    }
    
    public void runAllDemos() throws InterruptedException, ExecutionException, TimeoutException {
        demonstrateBasicCompletableFuture();
        TimeUnit.SECONDS.sleep(1);
        
        demonstrateAsyncChaining();
        TimeUnit.SECONDS.sleep(1);
        
        demonstrateAsyncComposition();
        TimeUnit.SECONDS.sleep(1);
        
        demonstrateAsyncCombination();
        TimeUnit.SECONDS.sleep(1);
        
        demonstrateExceptionHandling();
        TimeUnit.SECONDS.sleep(1);
        
        demonstrateTimeoutHandling();
        TimeUnit.SECONDS.sleep(1);
        
        demonstrateCustomExecutor();
        TimeUnit.SECONDS.sleep(1);
        
        demonstrateAnyOf();
        TimeUnit.SECONDS.sleep(1);
        
        demonstrateComplexTradingWorkflow();
    }
}
