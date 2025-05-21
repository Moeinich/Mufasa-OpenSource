package helpers;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadManager {
    private static ThreadManager instance;

    // Unified thread pool for all task types
    private final ExecutorService unifiedExecutor;
    private final ConcurrentHashMap<String, ExecutorService> adbExecutors = new ConcurrentHashMap<>();

    // Scheduled executor for periodic tasks
    private final ScheduledExecutorService scheduler;

    public static int AVAILABLE_CORES;

    private ThreadManager() {
        // Determine the number of available CPU cores
        int coreCount = Runtime.getRuntime().availableProcessors();
        AVAILABLE_CORES = coreCount;

        // Custom thread factory for naming threads
        ThreadFactory unifiedThreadFactory = new CustomThreadFactory("UnifiedPoolThread");
        ThreadFactory schedulerThreadFactory = new CustomThreadFactory("SchedulerThread");

        // Initialize the unified executor with a pool size based on CPU cores
        this.unifiedExecutor = new ThreadPoolExecutor(
                coreCount,
                coreCount * 2, // Allows for burst capacity
                60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(500),
                unifiedThreadFactory,
                new ThreadPoolExecutor.CallerRunsPolicy() // Handles saturation by running tasks in the calling thread
        );

        // Initialize the scheduler with a pool size based on CPU cores
        this.scheduler = new ScheduledThreadPoolExecutor(
                Math.max(3, coreCount / 2), // Ensure at least 2 threads
                schedulerThreadFactory
        );
        ((ScheduledThreadPoolExecutor) this.scheduler).setRemoveOnCancelPolicy(true);

        // Schedule garbage collection every 30 minutes
        scheduleGarbageCollection();
    }

    // Singleton accessor
    public static synchronized ThreadManager getInstance() {
        if (instance == null) {
            instance = new ThreadManager();
        }
        return instance;
    }

    // Accessors for thread pools
    public ExecutorService getUnifiedExecutor() {
        return unifiedExecutor;
    }

    public ScheduledExecutorService getScheduler() {
        return scheduler;
    }

    public ExecutorService getADBExecutor(String device) {
        return adbExecutors.computeIfAbsent(device, this::createADBExecutor);
    }

    // Create a new executor for a specific device
    private ExecutorService createADBExecutor(String device) {
        ThreadFactory adbThreadFactory = new CustomThreadFactory("ADBPoolThread-" + device);
        return new ThreadPoolExecutor(
                4, // Single thread per device for sequential execution
                8, // Allow a small burst capacity
                60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(50), // Bounded queue
                adbThreadFactory,
                new ThreadPoolExecutor.CallerRunsPolicy() // Handles saturation by running tasks in the calling thread
        );
    }

    // Graceful shutdown
    public void shutdown() {
        System.out.println("Shutting down schedulers");
        shutdownExecutor(scheduler, "Scheduler");
        System.out.println("Shutting down ADB executors");
        adbExecutors.forEach((device, executor) -> shutdownExecutor(executor, "ADBExecutor-" + device));
        System.out.println("Shutting down unified executors");
        shutdownExecutor(unifiedExecutor, "UnifiedExecutor");
    }

    private void shutdownExecutor(ExecutorService executor, String name) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                    System.err.println(name + " did not terminate.");
                }
            }
        } catch (InterruptedException ie) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // Method to schedule garbage collection
    private void scheduleGarbageCollection() {
        // Trigger garbage collection to ensure we clean up
        scheduler.scheduleAtFixedRate(System::gc, 30, 30, TimeUnit.MINUTES);
    }

    // Custom ThreadFactory for naming threads
    private static class CustomThreadFactory implements ThreadFactory {
        private final String baseName;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final ThreadGroup group;

        CustomThreadFactory(String baseName) {
            this.baseName = baseName;
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();
        }

        @Override
        public Thread newThread(@NotNull Runnable r) {
            Thread t = new Thread(group, r,
                    baseName + "-" + threadNumber.getAndIncrement(),
                    0);
            if (t.isDaemon())
                t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }
}