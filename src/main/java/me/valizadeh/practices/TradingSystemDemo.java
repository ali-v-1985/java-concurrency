package me.valizadeh.practices;

import me.valizadeh.practices.async.AsyncTradingOperations;
import me.valizadeh.practices.collections.TradingConcurrentCollections;
import me.valizadeh.practices.coordination.TradeSettlementCoordination;
import me.valizadeh.practices.executors.OrderExecutionEngine;
import me.valizadeh.practices.jmm.MarketDataJMMDemo;
import me.valizadeh.practices.loom.HighFrequencyTradingLoom;
import me.valizadeh.practices.patterns.TradingConcurrencyPatterns;
import me.valizadeh.practices.synchronization.PortfolioSynchronization;
import me.valizadeh.practices.threads.OrderProcessorThreads;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 * Main demonstration application for Java Concurrency in Trading Systems
 * 
 * This comprehensive demo showcases all major Java concurrency concepts through
 * realistic trading system scenarios, including:
 * 
 * 1. Java Memory Model (JMM) - Market data visibility issues
 * 2. Thread Basics - Order processing lifecycle
 * 3. Synchronization Primitives - Portfolio management
 * 4. Thread Coordination - Trade settlement workflows
 * 5. Executors & Thread Pools - Order execution engines
 * 6. Concurrent Collections - Order books and trade history
 * 7. Futures & Async Programming - Async trading operations
 * 8. Concurrency Patterns - Trading system design patterns
 * 9. Project Loom Virtual Threads - High-frequency trading scalability
 * 
 * Each module demonstrates practical concurrency challenges and solutions
 * that senior developers encounter in real-world financial systems.
 */
public class TradingSystemDemo {
    private static final Logger logger = LoggerFactory.getLogger(TradingSystemDemo.class);
    
    public static void main(String[] args) {
        logger.info("🏛️ Starting Java Concurrency Trading System Demonstration");
        logger.info("💼 This demo covers all major concurrency concepts through trading scenarios");
        logger.info("⚡ Built with Java 21 featuring Project Loom Virtual Threads");
        logger.info("=" .repeat(80));
        
        TradingSystemDemo demo = new TradingSystemDemo();
        
        if (args.length > 0 && "interactive".equals(args[0])) {
            demo.runInteractiveDemo();
        } else {
            demo.runCompleteDemo();
        }
    }
    
    /**
     * Runs the complete demonstration of all concurrency concepts
     */
    public void runCompleteDemo() {
        Instant startTime = Instant.now();
        
        try {
            // 1. Java Memory Model Demonstration
            demonstrateJavaMemoryModel();
            
            // 2. Thread Basics
            demonstrateThreadBasics();
            
            // 3. Synchronization Primitives
            demonstrateSynchronization();
            
            // 4. Thread Coordination
            demonstrateThreadCoordination();
            
            // 5. Executors and Thread Pools
            demonstrateExecutors();
            
            // 6. Concurrent Collections
            demonstrateConcurrentCollections();
            
            // 7. Futures and Async Programming
            demonstrateAsyncProgramming();
            
            // 8. Concurrency Patterns
            demonstrateConcurrencyPatterns();
            
            // 9. Project Loom Virtual Threads
            demonstrateVirtualThreads();
            
            Duration totalTime = Duration.between(startTime, Instant.now());
            
            logger.info("=" .repeat(80));
            logger.info("🎉 Java Concurrency Trading System Demo Completed Successfully!");
            logger.info("⏱️ Total execution time: {} seconds", totalTime.getSeconds());
            logger.info("🚀 All concurrency concepts demonstrated through realistic trading scenarios");
            logger.info("📚 Key takeaways:");
            logger.info("   • Understand Java Memory Model for correct concurrent code");
            logger.info("   • Use appropriate synchronization mechanisms");
            logger.info("   • Leverage thread coordination primitives effectively");
            logger.info("   • Choose the right executor service for your use case");
            logger.info("   • Utilize concurrent collections for thread-safe data structures");
            logger.info("   • Master async programming with CompletableFuture");
            logger.info("   • Apply proven concurrency patterns");
            logger.info("   • Embrace Project Loom for massive scalability");
            
        } catch (Exception e) {
            logger.error("❌ Demo execution failed", e);
            System.exit(1);
        }
    }
    
    /**
     * Runs an interactive demonstration where users can choose specific modules
     */
    public void runInteractiveDemo() {
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
            printInteractiveMenu();
            
            try {
                int choice = Integer.parseInt(scanner.nextLine());
                
                switch (choice) {
                    case 1 -> demonstrateJavaMemoryModel();
                    case 2 -> demonstrateThreadBasics();
                    case 3 -> demonstrateSynchronization();
                    case 4 -> demonstrateThreadCoordination();
                    case 5 -> demonstrateExecutors();
                    case 6 -> demonstrateConcurrentCollections();
                    case 7 -> demonstrateAsyncProgramming();
                    case 8 -> demonstrateConcurrencyPatterns();
                    case 9 -> demonstrateVirtualThreads();
                    case 10 -> {
                        runCompleteDemo();
                        return;
                    }
                    case 0 -> {
                        logger.info("👋 Thank you for exploring Java Concurrency!");
                        return;
                    }
                    default -> logger.warn("❌ Invalid choice. Please try again.");
                }
                
                // Pause between demonstrations
                logger.info("\n⏸️ Press Enter to continue...");
                scanner.nextLine();
                
            } catch (NumberFormatException e) {
                logger.warn("❌ Please enter a valid number.");
            } catch (Exception e) {
                logger.error("❌ Error during demonstration", e);
            }
            }
        } catch (Exception e) {
            logger.error("❌ Error in interactive demo", e);
        }
    }
    
    private void printInteractiveMenu() {
        logger.info("\n" + "=".repeat(80));
        logger.info("🏛️ Java Concurrency Trading System - Interactive Demo");
        logger.info("=" .repeat(80));
        logger.info("Choose a concurrency concept to demonstrate:");
        logger.info("");
        logger.info("1. 🧠 Java Memory Model (JMM) - Visibility & Happens-Before");
        logger.info("2. 🧵 Thread Basics - Creation, Lifecycle & Management");
        logger.info("3. 🔒 Synchronization - synchronized, volatile, Locks");
        logger.info("4. 🤝 Thread Coordination - CountDownLatch, CyclicBarrier, Semaphore");
        logger.info("5. ⚙️ Executors & Thread Pools - Scalable Task Execution");
        logger.info("6. 📦 Concurrent Collections - Thread-Safe Data Structures");
        logger.info("7. 🔮 Futures & Async Programming - CompletableFuture");
        logger.info("8. 🏗️ Concurrency Patterns - Producer-Consumer, Actor Model");
        logger.info("9. 🚀 Project Loom Virtual Threads - Massive Scalability");
        logger.info("");
        logger.info("10. 🎯 Run Complete Demo (All Modules)");
        logger.info("0. 🚪 Exit");
        logger.info("");
        logger.info("Enter your choice: ");
    }
    
    private void demonstrateJavaMemoryModel() {
        try {
            logger.info("\n🧠 JAVA MEMORY MODEL DEMONSTRATION");
            logger.info("Exploring visibility issues and happens-before relationships in trading scenarios");
            logger.info("-".repeat(60));
            
            MarketDataJMMDemo jmmDemo = new MarketDataJMMDemo();
            jmmDemo.runAllDemos();
            
            logger.info("✅ Java Memory Model demonstration completed");
            
        } catch (Exception e) {
            logger.error("❌ JMM demonstration failed", e);
        }
    }
    
    private void demonstrateThreadBasics() {
        try {
            logger.info("\n🧵 THREAD BASICS DEMONSTRATION");
            logger.info("Thread creation, lifecycle, and management in order processing");
            logger.info("-".repeat(60));
            
            OrderProcessorThreads threadDemo = new OrderProcessorThreads();
            threadDemo.runAllDemos();
            
            logger.info("✅ Thread basics demonstration completed");
            
        } catch (Exception e) {
            logger.error("❌ Thread basics demonstration failed", e);
        }
    }
    
    private void demonstrateSynchronization() {
        try {
            logger.info("\n🔒 SYNCHRONIZATION DEMONSTRATION");
            logger.info("synchronized, volatile, and Lock mechanisms in portfolio management");
            logger.info("-".repeat(60));
            
            PortfolioSynchronization syncDemo = new PortfolioSynchronization();
            syncDemo.runAllDemos();
            
            logger.info("✅ Synchronization demonstration completed");
            
        } catch (Exception e) {
            logger.error("❌ Synchronization demonstration failed", e);
        }
    }
    
    private void demonstrateThreadCoordination() {
        try {
            logger.info("\n🤝 THREAD COORDINATION DEMONSTRATION");
            logger.info("CountDownLatch, CyclicBarrier, Semaphore in trade settlement");
            logger.info("-".repeat(60));
            
            TradeSettlementCoordination coordDemo = new TradeSettlementCoordination();
            coordDemo.runAllDemos();
            
            logger.info("✅ Thread coordination demonstration completed");
            
        } catch (Exception e) {
            logger.error("❌ Thread coordination demonstration failed", e);
        }
    }
    
    private void demonstrateExecutors() {
        try {
            logger.info("\n⚙️ EXECUTORS & THREAD POOLS DEMONSTRATION");
            logger.info("ExecutorService patterns in order execution engines");
            logger.info("-".repeat(60));
            
            OrderExecutionEngine executorDemo = new OrderExecutionEngine();
            executorDemo.runAllDemos();
            
            logger.info("✅ Executors demonstration completed");
            
        } catch (Exception e) {
            logger.error("❌ Executors demonstration failed", e);
        }
    }
    
    private void demonstrateConcurrentCollections() {
        try {
            logger.info("\n📦 CONCURRENT COLLECTIONS DEMONSTRATION");
            logger.info("Thread-safe collections for order books and trade history");
            logger.info("-".repeat(60));
            
            TradingConcurrentCollections collectionsDemo = new TradingConcurrentCollections();
            collectionsDemo.runAllDemos();
            
            logger.info("✅ Concurrent collections demonstration completed");
            
        } catch (Exception e) {
            logger.error("❌ Concurrent collections demonstration failed", e);
        }
    }
    
    private void demonstrateAsyncProgramming() {
        try {
            logger.info("\n🔮 ASYNC PROGRAMMING DEMONSTRATION");
            logger.info("CompletableFuture for non-blocking trading operations");
            logger.info("-".repeat(60));
            
            AsyncTradingOperations asyncDemo = new AsyncTradingOperations();
            asyncDemo.runAllDemos();
            
            logger.info("✅ Async programming demonstration completed");
            
        } catch (Exception e) {
            logger.error("❌ Async programming demonstration failed", e);
        }
    }
    
    private void demonstrateConcurrencyPatterns() {
        try {
            logger.info("\n🏗️ CONCURRENCY PATTERNS DEMONSTRATION");
            logger.info("Producer-Consumer, Work Stealing, Actor Model in trading systems");
            logger.info("-".repeat(60));
            
            TradingConcurrencyPatterns patternsDemo = new TradingConcurrencyPatterns();
            patternsDemo.runAllDemos();
            
            logger.info("✅ Concurrency patterns demonstration completed");
            
        } catch (Exception e) {
            logger.error("❌ Concurrency patterns demonstration failed", e);
        }
    }
    
    private void demonstrateVirtualThreads() {
        try {
            logger.info("\n🚀 PROJECT LOOM VIRTUAL THREADS DEMONSTRATION");
            logger.info("Massive scalability for high-frequency trading with Java 21");
            logger.info("-".repeat(60));
            
            HighFrequencyTradingLoom loomDemo = new HighFrequencyTradingLoom();
            loomDemo.runAllDemos();
            
            logger.info("✅ Virtual threads demonstration completed");
            
        } catch (Exception e) {
            logger.error("❌ Virtual threads demonstration failed", e);
        }
    }
    
    /**
     * Prints system information and concurrency capabilities
     */
    private static void printSystemInfo() {
        Runtime runtime = Runtime.getRuntime();
        
        logger.info("🖥️ System Information:");
        logger.info("   Java Version: {}", System.getProperty("java.version"));
        logger.info("   Available Processors: {}", runtime.availableProcessors());
        logger.info("   Max Memory: {} MB", runtime.maxMemory() / (1024 * 1024));
        logger.info("   Total Memory: {} MB", runtime.totalMemory() / (1024 * 1024));
        logger.info("   Free Memory: {} MB", runtime.freeMemory() / (1024 * 1024));
        
        // Check for virtual thread support
        try {
            Thread.ofVirtual().start(() -> {}).join();
            logger.info("   ✅ Virtual Threads: Supported (Project Loom)");
        } catch (Exception e) {
            logger.info("   ❌ Virtual Threads: Not supported");
        }
        
        logger.info("");
    }
    
    static {
        // Print system info on startup
        printSystemInfo();
    }
}
