package com.vaadin.base.devserver.stats;

public class StartupPerformance {

    private static final String EVENT_BEFORE_MODULE_TIME = "beforeModule";
    private static final String EVENT_MODULE_COMPILE_AND_START = "moduleCompileAndStart";
    private static final String EVENT_PACKAGEMANAGER_INSTALL_TIME_PREFIX = "packageManagerInstall";
    private static final String EVENT_DEV_SERVER_START_PREFIX = "startDevserver";

    private static boolean startupTimeReported = false;
    private static long timestampSessionStart = 0;
    private static long timestampPrepareFrontendStart = 0;

    private static long lastDevServerStartupTimeMs = 0;
    private static long lastPackageManagerInstallTimeMs = 0;
    private static String lastPackageManager = "";
    private static String lastDevServer = "";

    public static void markMavenGradleStart(long sessionStartTimestamp) {
        timestampSessionStart = sessionStartTimestamp;
    }

    public static void markPrepareFrontendStart(
            long prepareFrontendStartTimestamp) {
        timestampPrepareFrontendStart = prepareFrontendStartTimestamp;
    }

    public static void markPackageManagerInstallTime(String packageManager,
            long packageManagerInstallTimeMs) {
        lastPackageManagerInstallTimeMs = packageManagerInstallTimeMs;
        lastPackageManager = packageManager;
        DevModeUsageStatistics.collectEvent(
                EVENT_PACKAGEMANAGER_INSTALL_TIME_PREFIX + packageManager,
                packageManagerInstallTimeMs);
    }

    public static void markDevServerStartupTime(String serverName,
            long devServerStartupTimeMs) {
        lastDevServerStartupTimeMs = devServerStartupTimeMs;
        lastDevServer = serverName;
        DevModeUsageStatistics.collectEvent(
                EVENT_DEV_SERVER_START_PREFIX + serverName,
                devServerStartupTimeMs);
    }

    public static void markApplicationStarted(long applicationStartTimestamp) {
        if (startupTimeReported) {
            // We must report startup time only once as it counts time from
            // running `mvn` or `gradlew`
            return;
        }
        long beforeModuleTimeMs = timestampPrepareFrontendStart
                - timestampSessionStart;
        long lastModuleCompileAndServerStartTimeMs = applicationStartTimestamp
                - timestampPrepareFrontendStart
                - lastPackageManagerInstallTimeMs - lastDevServerStartupTimeMs;

        DevModeUsageStatistics.collectEvent(
                "startup-" + EVENT_BEFORE_MODULE_TIME, beforeModuleTimeMs);
        DevModeUsageStatistics.collectEvent(
                "startup-" + EVENT_MODULE_COMPILE_AND_START,
                lastModuleCompileAndServerStartTimeMs);
        DevModeUsageStatistics
                .collectEvent(
                        "startup-" + EVENT_PACKAGEMANAGER_INSTALL_TIME_PREFIX
                                + lastPackageManager,
                        lastPackageManagerInstallTimeMs);
        DevModeUsageStatistics.collectEvent(
                "startup-" + EVENT_DEV_SERVER_START_PREFIX + lastDevServer,
                lastDevServerStartupTimeMs);
        startupTimeReported = true;
    }

}
