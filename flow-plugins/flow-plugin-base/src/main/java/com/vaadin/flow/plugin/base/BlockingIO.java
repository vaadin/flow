package com.vaadin.flow.plugin.base;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plugin-internal utility providing a dedicated executor for blocking IO tasks
 * (e.g. consuming process stdout/stderr, executing external commands).
 * Avoids using {@code ForkJoinPool.commonPool()} for blocking operations to
 * prevent starvation in small-core environments.
 *
 * For internal use in Flow plugins only. Subject to change without notice.
 */
public final class BlockingIO {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlockingIO.class);

    private static final String PROP_USE_COMMON_POOL =
            "vaadin.frontend.io.useCommonPool";
    private static final String PROP_VIRTUAL_THREADS =
            "vaadin.frontend.io.virtualThreads";
    private static final String PROP_CORE_SIZE =
            "vaadin.frontend.io.corePoolSize";
    private static final String PROP_MAX_SIZE =
            "vaadin.frontend.io.maxPoolSize";
    private static final String PROP_KEEPALIVE =
            "vaadin.frontend.io.keepAliveSeconds";

    // Singleton instance, lazily initialized on first use
    private static volatile ExecutorService executor;

    private BlockingIO() {
    }

    /**
     * Returns the dedicated executor for blocking IO tasks.
     */
    public static Executor getExecutor() {
        ExecutorService exec = executor;
        if (exec == null) {
            synchronized (BlockingIO.class) {
                exec = executor;
                if (exec == null) {
                    executor = exec = createExecutor();
                }
            }
        }
        return exec;
    }

    private static ExecutorService createExecutor() {
        // Optional diagnostic override to force common pool usage
        if (Boolean.getBoolean(PROP_USE_COMMON_POOL)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("BlockingIO (plugins): using ForkJoinPool.commonPool() due to system property {}=true", PROP_USE_COMMON_POOL);
            }
            // Diagnostic override: use the JVM-wide common pool (not recommended for blocking IO)
            return java.util.concurrent.ForkJoinPool.commonPool();
        }

        boolean virtualPreferred = getBoolean(PROP_VIRTUAL_THREADS, true);
        if (virtualPreferred) {
            ExecutorService executorService = tryCreateVirtualThreadExecutor();
            if (executorService != null) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("BlockingIO (plugins): using virtual threads executor (Executors.newVirtualThreadPerTaskExecutor)");
                }
                return executorService;
            }
        }

        // Fallback: bounded daemon thread pool with conservative defaults
        int cpus = Math.max(1, Runtime.getRuntime().availableProcessors());
        int core = getInt(PROP_CORE_SIZE, Math.min(8, Math.max(2, cpus)));
        int max = getInt(PROP_MAX_SIZE, Math.min(64, Math.max(4, cpus * 4)));
        long keepAliveSec = getLong(PROP_KEEPALIVE, 60L);

        ThreadFactory tf = new NamedDaemonThreadFactory("vaadin-frontend-io-");

        // Choose queue: prefer SynchronousQueue for responsive scaling when max > core,
        // otherwise a bounded queue sized relative to max.
        boolean useSynchronous = max > core;
        ThreadPoolExecutor tpExecutor = new ThreadPoolExecutor(
                core,
                max,
                keepAliveSec,
                TimeUnit.SECONDS,
                useSynchronous ? new SynchronousQueue<>()
                        : new LinkedBlockingQueue<>(Math.max(1024, max * 1024)),
                tf,
                new ThreadPoolExecutor.CallerRunsPolicy());
        tpExecutor.allowCoreThreadTimeOut(true);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("BlockingIO (plugins): using bounded thread pool (core={}, max={}, keepAliveSeconds={}, queue={})",
                    core, max, keepAliveSec, useSynchronous ? "SynchronousQueue" : "LinkedBlockingQueue");
        }

        // Ensure non-daemon pools are shut down (ours are daemon, but be safe)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                tpExecutor.shutdown();
                // best-effort
                boolean terminated = tpExecutor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } finally {
                tpExecutor.shutdownNow();
            }
        }, "vaadin-frontend-io-shutdown"));

        return tpExecutor;
    }

    /**
     * Attempts to create a virtual-thread-per-task executor via reflection
     * to keep compilation compatible with older JDKs (e.g., Java 17).
     */
    private static ExecutorService tryCreateVirtualThreadExecutor() {
        try {
            // Executors.newVirtualThreadPerTaskExecutor()
            Method m = Executors.class.getMethod("newVirtualThreadPerTaskExecutor");
            Object exec = m.invoke(null);
            return (ExecutorService) exec;
        } catch (ReflectiveOperationException | LinkageError e) {
            return null; // Not available on this JDK
        }
    }

    private static boolean getBoolean(String key, boolean def) {
        String v = System.getProperty(key);
        if (v == null || v.isEmpty()) {
            return def;
        }
        return Boolean.parseBoolean(v);
    }

    private static int getInt(String key, int def) {
        String v = System.getProperty(key);
        if (v == null || v.isEmpty()) {
            return def;
        }
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static long getLong(String key, long def) {
        String v = System.getProperty(key);
        if (v == null || v.isEmpty()) {
            return def;
        }
        try {
            return Long.parseLong(v);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static final class NamedDaemonThreadFactory implements ThreadFactory {
        private final String prefix;
        private final AtomicLong seq = new AtomicLong(1);

        NamedDaemonThreadFactory(String prefix) {
            this.prefix = Objects.requireNonNull(prefix);
        }

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, prefix + seq.getAndIncrement());
            thread.setDaemon(true);
            // Lower priority to reduce interference with build threads
            if (thread.getPriority() > Thread.NORM_PRIORITY - 1) {
                thread.setPriority(Math.max(Thread.MIN_PRIORITY + 1, Thread.NORM_PRIORITY - 1));
            }
            return thread;
        }
    }
}
