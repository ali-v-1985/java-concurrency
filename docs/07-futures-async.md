# Futures & Async Programming

## 🚀 **What is Asynchronous Programming?**

Asynchronous programming allows you to:
- **Start operations without waiting** - Begin tasks and continue with other work
- **Handle results later** - Process results when they become available
- **Compose operations** - Chain and combine async operations
- **Improve responsiveness** - Don't block on slow operations

### **Traditional Synchronous vs Asynchronous**

```java
// ❌ Synchronous - Blocks calling thread
public String processTradeSync() {
    String riskCheck = performRiskCheck();      // Blocks for 500ms
    String marketData = fetchMarketData();      // Blocks for 300ms  
    String execution = executeOrder();          // Blocks for 200ms
    return "Trade completed";                   // Total: 1000ms
}

// ✅ Asynchronous - Non-blocking
public CompletableFuture<String> processTradeAsync() {
    return CompletableFuture
        .supplyAsync(() -> performRiskCheck())    // 500ms in parallel
        .thenCompose(risk -> CompletableFuture
            .supplyAsync(() -> fetchMarketData())  // 300ms in parallel
            .thenCombine(
                CompletableFuture.supplyAsync(() -> executeOrder()), // 200ms in parallel
                (data, execution) -> "Trade completed"               // Total: ~500ms
            )
        );
}
```

---

## 🎯 **Future Interface - Basic Async Operations**

### **Future Basics**

```java
public void demonstrateBasicFuture() throws InterruptedException, ExecutionException {
    logger.info("=== Demonstrating Basic Future ===");
    
    ExecutorService executor = Executors.newFixedThreadPool(3);
    
    // Submit task and get Future
    Future<String> future = executor.submit(() -> {
        logger.info("🔄 Background task starting in thread: {}", 
            Thread.currentThread().getName());
        
        try {
            Thread.sleep(2000); // Simulate long-running operation
            return "Risk analysis completed for portfolio";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Risk analysis interrupted";
        }
    });
    
    logger.info("📋 Task submitted, continuing with other work...");
    
    // Do other work while background task runs
    for (int i = 1; i <= 3; i++) {
        logger.info("⚡ Main thread: Doing other work step {}", i);
        Thread.sleep(500);
    }
    
    // Check if task is done
    if (future.isDone()) {
        logger.info("✅ Background task completed!");
    } else {
        logger.info("⏳ Background task still running, waiting for result...");
    }
    
    // Get result (blocks until complete)
    String result = future.get(); // Can also use get(timeout, unit)
    logger.info("📄 Background task result: {}", result);
    
    executor.shutdown();
    logger.info("Basic Future demonstration completed");
}
```

### **Future with Timeout and Cancellation**

```java
public void demonstrateFutureTimeout() throws InterruptedException {
    logger.info("=== Demonstrating Future Timeout and Cancellation ===");
    
    ExecutorService executor = Executors.newFixedThreadPool(2);
    
    // Fast task
    Future<String> fastTask = executor.submit(() -> {
        try {
            Thread.sleep(500);
            return "Fast market data fetch completed";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Fast task interrupted";
        }
    });
    
    // Slow task  
    Future<String> slowTask = executor.submit(() -> {
        try {
            Thread.sleep(5000); // Very slow
            return "Slow external API call completed";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Slow task interrupted";
        }
    });
    
    try {
        // Get fast result
        String fastResult = fastTask.get(1, TimeUnit.SECONDS);
        logger.info("✅ Fast task: {}", fastResult);
        
        // Try to get slow result with timeout
        String slowResult = slowTask.get(2, TimeUnit.SECONDS);
        logger.info("✅ Slow task: {}", slowResult);
        
    } catch (TimeoutException e) {
        logger.warn("⏰ Slow task timed out, cancelling...");
        
        // Cancel the slow task
        boolean cancelled = slowTask.cancel(true); // true = interrupt if running
        logger.info("🛑 Slow task cancellation: {}", cancelled ? "successful" : "failed");
        
    } catch (ExecutionException e) {
        logger.error("❌ Task execution error: {}", e.getCause().getMessage());
    }
    
    executor.shutdown();
    logger.info("Future timeout demonstration completed");
}
```

---

## ⚡ **CompletableFuture - Advanced Async Programming**

### **CompletableFuture Creation**

```java
public void demonstrateCompletableFutureCreation() {
    logger.info("=== Demonstrating CompletableFuture Creation ===");
    
    // 1. Already completed future
    CompletableFuture<String> immediateResult = CompletableFuture.completedFuture("MARKET_OPEN");
    logger.info("✅ Immediate result: {}", immediateResult.join());
    
    // 2. Async supplier (runs in ForkJoinPool.commonPool())
    CompletableFuture<String> asyncSupplier = CompletableFuture.supplyAsync(() -> {
        logger.info("🔄 Fetching market data in thread: {}", Thread.currentThread().getName());
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "Market data: AAPL $150.00";
    });
    
    // 3. Async runnable (no return value)
    CompletableFuture<Void> asyncRunnable = CompletableFuture.runAsync(() -> {
        logger.info("🔄 Performing audit log in thread: {}", Thread.currentThread().getName());
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        logger.info("📝 Audit log entry created");
    });
    
    // 4. Manual completion
    CompletableFuture<String> manualFuture = new CompletableFuture<>();
    
    // Complete it after some delay
    CompletableFuture.runAsync(() -> {
        try {
            Thread.sleep(800);
            manualFuture.complete("RISK_CHECK_PASSED");
        } catch (InterruptedException e) {
            manualFuture.completeExceptionally(e);
        }
    });
    
    // Get all results
    logger.info("📄 Async supplier result: {}", asyncSupplier.join());
    asyncRunnable.join(); // Wait for completion
    logger.info("📄 Manual completion result: {}", manualFuture.join());
    
    logger.info("CompletableFuture creation demonstration completed");
}
```

### **Chaining Operations - thenApply, thenAccept, thenRun**

```java
public void demonstrateChaining() {
    logger.info("=== Demonstrating CompletableFuture Chaining ===");
    
    // Chain multiple transformations
    CompletableFuture<String> tradingPipeline = CompletableFuture
        .supplyAsync(() -> {
            logger.info("🔄 Step 1: Fetching market data...");
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "AAPL:150.00";
        })
        .thenApply(marketData -> {
            logger.info("🔄 Step 2: Analyzing market data: {}", marketData);
            String[] parts = marketData.split(":");
            double price = Double.parseDouble(parts[1]);
            String analysis = price > 140 ? "BULLISH" : "BEARISH";
            return parts[0] + ":" + analysis;
        })
        .thenApply(analysis -> {
            logger.info("🔄 Step 3: Generating trading signal: {}", analysis);
            String[] parts = analysis.split(":");
            String signal = parts[1].equals("BULLISH") ? "BUY" : "SELL";
            return parts[0] + ":" + signal;
        })
        .thenApply(signal -> {
            logger.info("🔄 Step 4: Creating order: {}", signal);
            String[] parts = signal.split(":");
            return String.format("ORDER_%s_%s_100", parts[1], parts[0]);
        });
    
    // Different types of chaining
    CompletableFuture<Void> notificationPipeline = tradingPipeline
        .thenAccept(order -> {
            logger.info("📧 Step 5: Sending notification for order: {}", order);
        })
        .thenRun(() -> {
            logger.info("📝 Step 6: Updating audit log");
        });
    
    // Wait for completion
    String finalOrder = tradingPipeline.join();
    notificationPipeline.join();
    
    logger.info("✅ Trading pipeline completed with order: {}", finalOrder);
    logger.info("Chaining demonstration completed");
}
```

### **Composition - thenCompose for Dependent Async Operations**

```java
public void demonstrateComposition() {
    logger.info("=== Demonstrating CompletableFuture Composition ===");
    
    CompletableFuture<String> tradingWorkflow = CompletableFuture
        .supplyAsync(() -> {
            logger.info("🔄 Step 1: Authenticating client...");
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "client123";
        })
        .thenCompose(clientId -> {
            logger.info("🔄 Step 2: Fetching positions for client: {}", clientId);
            return CompletableFuture.supplyAsync(() -> {
                try {
                    Thread.sleep(400);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return clientId + ":AAPL=100,GOOGL=50";
            });
        })
        .thenCompose(positions -> {
            logger.info("🔄 Step 3: Calculating portfolio value for positions: {}", positions);
            return CompletableFuture.supplyAsync(() -> {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                // Simulate portfolio calculation
                return positions + ":VALUE=$25000";
            });
        })
        .thenCompose(portfolio -> {
            logger.info("🔄 Step 4: Generating portfolio report: {}", portfolio);
            return CompletableFuture.supplyAsync(() -> {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "PORTFOLIO_REPORT:" + portfolio;
            });
        });
    
    String finalReport = tradingWorkflow.join();
    logger.info("✅ Portfolio workflow completed: {}", finalReport);
    logger.info("Composition demonstration completed");
}
```

### **Combination - Parallel Execution with Results**

```java
public void demonstrateCombination() {
    logger.info("=== Demonstrating CompletableFuture Combination ===");
    
    // Parallel market data fetching
    CompletableFuture<String> aaplData = CompletableFuture.supplyAsync(() -> {
        logger.info("📊 Fetching AAPL data in thread: {}", Thread.currentThread().getName());
        try {
            Thread.sleep(800);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "AAPL:$150.00";
    });
    
    CompletableFuture<String> googlData = CompletableFuture.supplyAsync(() -> {
        logger.info("📊 Fetching GOOGL data in thread: {}", Thread.currentThread().getName());
        try {
            Thread.sleep(600);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "GOOGL:$120.00";
    });
    
    // Combine two futures
    CompletableFuture<String> combinedAnalysis = aaplData.thenCombine(googlData, (aapl, googl) -> {
        logger.info("🔄 Combining market data: {} and {}", aapl, googl);
        return String.format("PORTFOLIO_ANALYSIS: %s, %s", aapl, googl);
    });
    
    // Multiple parallel operations
    CompletableFuture<String> marketAnalysis = CompletableFuture.supplyAsync(() -> {
        logger.info("📈 Performing market analysis...");
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "MARKET_SENTIMENT:BULLISH";
    });
    
    CompletableFuture<String> riskCheck = CompletableFuture.supplyAsync(() -> {
        logger.info("⚖️ Performing risk check...");
        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "RISK_LEVEL:LOW";
    });
    
    // Wait for all to complete
    CompletableFuture<Void> allAnalytics = CompletableFuture.allOf(
        combinedAnalysis, marketAnalysis, riskCheck
    );
    
    CompletableFuture<String> finalReport = allAnalytics.thenApply(v -> {
        // All futures completed, collect results
        String combined = combinedAnalysis.join();
        String market = marketAnalysis.join();
        String risk = riskCheck.join();
        
        return String.format("TRADING_REPORT: [%s] [%s] [%s]", combined, market, risk);
    });
    
    String report = finalReport.join();
    logger.info("✅ Final trading report: {}", report);
    
    // Demonstrate anyOf - first to complete wins
    logger.info("🏃 Demonstrating anyOf - fastest data source wins:");
    
    CompletableFuture<String> source1 = CompletableFuture.supplyAsync(() -> {
        try {
            Thread.sleep(300);
            return "DATA_FROM_SOURCE_1";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "SOURCE_1_INTERRUPTED";
        }
    });
    
    CompletableFuture<String> source2 = CompletableFuture.supplyAsync(() -> {
        try {
            Thread.sleep(200); // Faster
            return "DATA_FROM_SOURCE_2";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "SOURCE_2_INTERRUPTED";
        }
    });
    
    CompletableFuture<String> source3 = CompletableFuture.supplyAsync(() -> {
        try {
            Thread.sleep(400);
            return "DATA_FROM_SOURCE_3";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "SOURCE_3_INTERRUPTED";
        }
    });
    
    CompletableFuture<Object> fastestResult = CompletableFuture.anyOf(source1, source2, source3);
    String fastest = (String) fastestResult.join();
    logger.info("🏆 Fastest data source result: {}", fastest);
    
    logger.info("Combination demonstration completed");
}
```

### **Exception Handling in CompletableFuture**

```java
public void demonstrateExceptionHandling() {
    logger.info("=== Demonstrating Exception Handling ===");
    
    // 1. handle() - Process both success and failure
    CompletableFuture<String> riskyTrade = CompletableFuture
        .supplyAsync(() -> {
            logger.info("🔄 Processing risky trade...");
            if (Math.random() < 0.5) {
                throw new RuntimeException("Market volatility too high!");
            }
            return "TRADE_EXECUTED_SUCCESSFULLY";
        })
        .handle((result, exception) -> {
            if (exception != null) {
                logger.warn("❌ Trade failed: {}", exception.getMessage());
                return "TRADE_FAILED_SAFELY";
            } else {
                logger.info("✅ Trade succeeded: {}", result);
                return result;
            }
        });
    
    logger.info("📄 Risky trade result: {}", riskyTrade.join());
    
    // 2. exceptionally() - Handle only failures
    CompletableFuture<String> orderProcessing = CompletableFuture
        .supplyAsync(() -> {
            logger.info("🔄 Processing order...");
            if (Math.random() < 0.7) {
                throw new RuntimeException("Insufficient funds!");
            }
            return "ORDER_PLACED";
        })
        .exceptionally(throwable -> {
            logger.warn("❌ Order failed: {}", throwable.getMessage());
            return "ORDER_REJECTED";
        })
        .thenApply(result -> {
            logger.info("📋 Order status: {}", result);
            return result + "_LOGGED";
        });
    
    logger.info("📄 Order processing result: {}", orderProcessing.join());
    
    // 3. whenComplete() - Side effects for both success and failure
    CompletableFuture<String> auditedOperation = CompletableFuture
        .supplyAsync(() -> {
            logger.info("🔄 Performing audited operation...");
            if (Math.random() < 0.3) {
                throw new RuntimeException("Compliance violation!");
            }
            return "OPERATION_COMPLETED";
        })
        .whenComplete((result, exception) -> {
            // This runs regardless of success or failure
            if (exception != null) {
                logger.warn("📝 Audit log: Operation failed - {}", exception.getMessage());
            } else {
                logger.info("📝 Audit log: Operation succeeded - {}", result);
            }
        });
    
    try {
        String auditResult = auditedOperation.join();
        logger.info("📄 Audited operation result: {}", auditResult);
    } catch (CompletionException e) {
        logger.error("💥 Audited operation threw exception: {}", e.getCause().getMessage());
    }
    
    // 4. Chained exception handling
    CompletableFuture<String> complexPipeline = CompletableFuture
        .supplyAsync(() -> {
            logger.info("🔄 Step 1: Validate input...");
            if (Math.random() < 0.2) {
                throw new IllegalArgumentException("Invalid input data");
            }
            return "INPUT_VALID";
        })
        .thenCompose(input -> {
            return CompletableFuture.supplyAsync(() -> {
                logger.info("🔄 Step 2: Process data...");
                if (Math.random() < 0.2) {
                    throw new RuntimeException("Processing error");
                }
                return input + "_PROCESSED";
            });
        })
        .thenApply(processed -> {
            logger.info("🔄 Step 3: Finalize result...");
            if (Math.random() < 0.2) {
                throw new RuntimeException("Finalization error");
            }
            return processed + "_FINALIZED";
        })
        .exceptionally(throwable -> {
            logger.error("❌ Pipeline failed at some stage: {}", throwable.getMessage());
            return "PIPELINE_FAILED_GRACEFULLY";
        });
    
    logger.info("📄 Complex pipeline result: {}", complexPipeline.join());
    
    logger.info("Exception handling demonstration completed");
}
```

### **Timeouts and Racing Operations**

```java
public void demonstrateTimeouts() throws InterruptedException, ExecutionException, TimeoutException {
    logger.info("=== Demonstrating Timeouts and Racing ===");
    
    // 1. orTimeout() - Complete exceptionally after timeout
    CompletableFuture<String> slowOperation = CompletableFuture
        .supplyAsync(() -> {
            logger.info("🐌 Starting slow operation...");
            try {
                Thread.sleep(3000); // Very slow
                return "SLOW_OPERATION_COMPLETED";
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "SLOW_OPERATION_INTERRUPTED";
            }
        })
        .orTimeout(1, TimeUnit.SECONDS); // Timeout after 1 second
    
    try {
        String result = slowOperation.join();
        logger.info("📄 Slow operation result: {}", result);
    } catch (CompletionException e) {
        if (e.getCause() instanceof TimeoutException) {
            logger.warn("⏰ Slow operation timed out: {}", e.getCause().getMessage());
        }
    }
    
    // 2. completeOnTimeout() - Provide default value on timeout
    CompletableFuture<String> operationWithDefault = CompletableFuture
        .supplyAsync(() -> {
            logger.info("🔄 Starting operation with fallback...");
            try {
                Thread.sleep(2000);
                return "OPERATION_COMPLETED";
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "OPERATION_INTERRUPTED";
            }
        })
        .completeOnTimeout("DEFAULT_VALUE", 800, TimeUnit.MILLISECONDS);
    
    String defaultResult = operationWithDefault.join();
    logger.info("📄 Operation with default result: {}", defaultResult);
    
    // 3. Racing multiple sources for fastest response
    logger.info("🏃 Racing multiple data sources...");
    
    CompletableFuture<String> primarySource = CompletableFuture.supplyAsync(() -> {
        try {
            Thread.sleep(600); // Usually fast
            return "DATA_FROM_PRIMARY";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "PRIMARY_INTERRUPTED";
        }
    });
    
    CompletableFuture<String> backupSource = CompletableFuture.supplyAsync(() -> {
        try {
            Thread.sleep(400); // Backup is faster today
            return "DATA_FROM_BACKUP";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "BACKUP_INTERRUPTED";
        }
    });
    
    CompletableFuture<String> cacheSource = CompletableFuture.supplyAsync(() -> {
        try {
            Thread.sleep(100); // Cache is usually fastest
            return "DATA_FROM_CACHE";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "CACHE_INTERRUPTED";
        }
    });
    
    // Race all sources, use the fastest
    CompletableFuture<String> fastestData = CompletableFuture.anyOf(
        primarySource, backupSource, cacheSource
    ).thenApply(result -> (String) result);
    
    String racingResult = fastestData.join();
    logger.info("🏆 Fastest data source: {}", racingResult);
    
    // 4. Timeout with custom executor
    ExecutorService customExecutor = Executors.newFixedThreadPool(2);
    
    try {
        CompletableFuture<String> customTimeoutOperation = CompletableFuture
            .supplyAsync(() -> {
                logger.info("🔄 Custom executor operation starting...");
                try {
                    Thread.sleep(1500);
                    return "CUSTOM_OPERATION_COMPLETED";
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return "CUSTOM_OPERATION_INTERRUPTED";
                }
            }, customExecutor)
            .orTimeout(1, TimeUnit.SECONDS);
        
        // Use get() with timeout for demonstration
        String customResult = customTimeoutOperation.get(2, TimeUnit.SECONDS);
        logger.info("📄 Custom executor result: {}", customResult);
        
    } catch (TimeoutException e) {
        logger.warn("⏰ Custom operation timed out");
    } finally {
        customExecutor.shutdown();
        customExecutor.awaitTermination(2, TimeUnit.SECONDS);
    }
    
    logger.info("Timeouts demonstration completed");
}
```

---

## 📊 **Synchronous vs Asynchronous Comparison**

### **Performance Example**

```java
public void demonstratePerformanceComparison() throws InterruptedException, ExecutionException {
    logger.info("=== Performance Comparison: Sync vs Async ===");
    
    // Synchronous approach
    Instant syncStart = Instant.now();
    
    String riskResult = performRiskCheck();      // 500ms
    String marketResult = fetchMarketData();     // 300ms  
    String executionResult = executeOrder();     // 200ms
    
    String syncFinalResult = String.format("Sync: %s, %s, %s", riskResult, marketResult, executionResult);
    
    Duration syncDuration = Duration.between(syncStart, Instant.now());
    logger.info("📊 Synchronous approach: {} in {} ms", syncFinalResult, syncDuration.toMillis());
    
    // Asynchronous approach
    Instant asyncStart = Instant.now();
    
    CompletableFuture<String> riskFuture = CompletableFuture.supplyAsync(this::performRiskCheck);
    CompletableFuture<String> marketFuture = CompletableFuture.supplyAsync(this::fetchMarketData);
    CompletableFuture<String> executionFuture = CompletableFuture.supplyAsync(this::executeOrder);
    
    // Combine all results
    CompletableFuture<String> asyncResult = CompletableFuture.allOf(
        riskFuture, marketFuture, executionFuture
    ).thenApply(v -> {
        String risk = riskFuture.join();
        String market = marketFuture.join();
        String execution = executionFuture.join();
        return String.format("Async: %s, %s, %s", risk, market, execution);
    });
    
    String asyncFinalResult = asyncResult.get();
    Duration asyncDuration = Duration.between(asyncStart, Instant.now());
    
    logger.info("📊 Asynchronous approach: {} in {} ms", asyncFinalResult, asyncDuration.toMillis());
    
    double improvement = (double) syncDuration.toMillis() / asyncDuration.toMillis();
    logger.info("🚀 Async was {:.2f}x faster!", improvement);
}

private String performRiskCheck() {
    logger.info("⚖️ Performing risk check in thread: {}", Thread.currentThread().getName());
    try {
        Thread.sleep(500);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
    return "RISK_CHECK_PASSED";
}

private String fetchMarketData() {
    logger.info("📊 Fetching market data in thread: {}", Thread.currentThread().getName());
    try {
        Thread.sleep(300);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
    return "MARKET_DATA_FETCHED";
}

private String executeOrder() {
    logger.info("🎯 Executing order in thread: {}", Thread.currentThread().getName());
    try {
        Thread.sleep(200);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
    return "ORDER_EXECUTED";
}
```

---

## 📊 **CompletableFuture Methods Summary**

| Method Category | Methods | Purpose |
|---|---|---|
| **Creation** | `completedFuture()`, `supplyAsync()`, `runAsync()` | Create futures |
| **Chaining** | `thenApply()`, `thenAccept()`, `thenRun()` | Transform results |
| **Composition** | `thenCompose()` | Chain dependent async operations |
| **Combination** | `thenCombine()`, `allOf()`, `anyOf()` | Combine multiple futures |
| **Exception Handling** | `handle()`, `exceptionally()`, `whenComplete()` | Handle errors |
| **Timeouts** | `orTimeout()`, `completeOnTimeout()` | Time-based completion |

---

## 🎯 **Best Practices**

### **1. Use appropriate methods for your use case**
```java
// Transform results
.thenApply(result -> transform(result))

// Side effects only
.thenAccept(result -> logResult(result))

// No result needed
.thenRun(() -> cleanupResources())

// Dependent async operations
.thenCompose(id -> fetchUserData(id))

// Independent parallel operations
future1.thenCombine(future2, (r1, r2) -> combine(r1, r2))
```

### **2. Handle exceptions properly**
```java
CompletableFuture<String> safeFuture = riskyOperation()
    .exceptionally(throwable -> {
        logger.error("Operation failed: {}", throwable.getMessage());
        return "FALLBACK_VALUE";
    })
    .whenComplete((result, exception) -> {
        // Cleanup resources regardless of outcome
        cleanup();
    });
```

### **3. Use timeouts for external operations**
```java
CompletableFuture<String> externalCall = CompletableFuture
    .supplyAsync(() -> callExternalService())
    .orTimeout(5, TimeUnit.SECONDS)
    .exceptionally(throwable -> "EXTERNAL_SERVICE_UNAVAILABLE");
```

### **4. Don't block in async chains**
```java
// ❌ WRONG - Blocking in async chain
CompletableFuture.supplyAsync(() -> getData())
    .thenApply(data -> {
        return expensiveBlockingOperation(data); // Blocks thread pool thread
    });

// ✅ CORRECT - Keep async operations async
CompletableFuture.supplyAsync(() -> getData())
    .thenCompose(data -> CompletableFuture.supplyAsync(() -> 
        expensiveBlockingOperation(data)
    ));
```

---

## ⚠️ **Common Pitfalls**

### **❌ Using get() instead of join()**
```java
// ❌ WRONG - Checked exceptions
try {
    String result = future.get();
} catch (InterruptedException | ExecutionException e) {
    // Handle exceptions
}

// ✅ BETTER - Unchecked exceptions (unless you need timeout)
String result = future.join();
```

### **❌ Not handling exceptions in async chains**
```java
// ❌ WRONG - Exceptions can break the chain
CompletableFuture.supplyAsync(() -> riskyOperation())
    .thenApply(result -> transform(result)); // This might never execute

// ✅ CORRECT - Handle exceptions
CompletableFuture.supplyAsync(() -> riskyOperation())
    .exceptionally(throwable -> "DEFAULT_VALUE")
    .thenApply(result -> transform(result));
```

### **❌ Blocking the calling thread unnecessarily**
```java
// ❌ WRONG - Defeats the purpose of async
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> slowOperation());
String result = future.get(); // Blocks immediately

// ✅ BETTER - Chain operations instead
CompletableFuture<Void> pipeline = CompletableFuture
    .supplyAsync(() -> slowOperation())
    .thenAccept(result -> processResult(result));
```

---

## 🔑 **Key Takeaways**

1. **Future** - Basic async operations with blocking result retrieval
2. **CompletableFuture** - Advanced async programming with chaining and composition
3. **Non-blocking chains** - Use `thenApply`, `thenCompose`, `thenCombine` for async pipelines
4. **Exception handling** - Always handle failures with `exceptionally` or `handle`
5. **Timeouts** - Use `orTimeout` and `completeOnTimeout` for external operations
6. **Parallel execution** - Use `allOf` and `anyOf` for multiple independent operations
7. **Performance** - Async programming can dramatically improve throughput for I/O-bound operations

## 📚 **Related Topics**
- [Executors & Thread Pools](./05-executors-thread-pools.md)
- [Concurrent Collections](./06-concurrent-collections.md)
- [Project Loom Virtual Threads](./09-virtual-threads.md)

---

Async programming with CompletableFuture enables building responsive, high-performance applications that can handle multiple operations concurrently without blocking!
