/*
 * Copyright 2000-2026 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.server;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.router.RouteData;
import com.vaadin.flow.router.internal.AbstractRouteRegistry;
import com.vaadin.flow.router.internal.RouteUtil;

/**
 * Collects the usage statistics of an application when it is started.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 */
class UsageStatisticsCollector implements Serializable {

    private static final String STATISTIC_KOTLIN = "kotlin";

    private static final String KOTLIN_METADATA_ANNOTATION = "kotlin.Metadata";

    private UsageStatisticsCollector() {
        // Only static methods here, no need to create an instance
    }

    /**
     * Collects the statistics based on the routes of the application.
     *
     * @param service
     *            the service the routes belong to
     * @param routeDataList
     *            the routes registered for the application
     */
    static void collectRouteStatistics(VaadinService service,
            List<RouteData> routeDataList) {
        if (!routeDataList.isEmpty()) {
            addRouterUsageStatistics();
        }
        addAutoLayoutUsageStatistics(service);
        addKotlinUsageStatistics(routeDataList);
    }

    /**
     * Collects the statistics of the frontend tools in use.
     *
     * @param configuration
     *            the deployment configuration of the application
     */
    static void collectFrontendToolStatistics(
            DeploymentConfiguration configuration) {
        if (configuration.isPnpmEnabled()) {
            UsageStatistics.markAsUsed("flow/pnpm", null);
        }
        if (configuration.isBunEnabled()) {
            UsageStatistics.markAsUsed("flow/bun", null);
        }
    }

    private static void addRouterUsageStatistics() {
        if (UsageStatistics.getEntries().anyMatch(
                e -> Constants.STATISTIC_ROUTING_CLIENT.equals(e.getName()))) {
            UsageStatistics.removeEntry(Constants.STATISTIC_ROUTING_CLIENT);
            UsageStatistics.markAsUsed(Constants.STATISTIC_ROUTING_HYBRID,
                    Version.getFullVersion());
        } else if (UsageStatistics.getEntries()
                .noneMatch(e -> Constants.STATISTIC_FLOW_BOOTSTRAPHANDLER
                        .equals(e.getName()))) {
            UsageStatistics.markAsUsed(Constants.STATISTIC_ROUTING_SERVER,
                    Version.getFullVersion());
        }
        UsageStatistics.markAsUsed(Constants.STATISTIC_HAS_FLOW_ROUTE, null);
    }

    private static void addAutoLayoutUsageStatistics(VaadinService service) {
        if (service.getRouteRegistry() instanceof AbstractRouteRegistry registry
                && RouteUtil.hasAutoLayout(registry)) {
            UsageStatistics.markAsUsed(Constants.STATISTIC_HAS_AUTO_LAYOUT,
                    null);
            if (RouteUtil.hasClientRouteWithAutoLayout(
                    service.getDeploymentConfiguration())) {
                UsageStatistics.markAsUsed(
                        Constants.STATISTIC_HAS_CLIENT_ROUTE_WITH_AUTO_LAYOUT,
                        null);
            }
            if (RouteUtil.hasServerRouteWithAutoLayout(registry)) {
                UsageStatistics.markAsUsed(
                        Constants.STATISTIC_HAS_SERVER_ROUTE_WITH_AUTO_LAYOUT,
                        null);
            }
        }
    }

    /**
     * Reports Kotlin usage if any of the application's views is written in
     * Kotlin.
     * <p>
     * Kotlin on the classpath is not enough, as kotlin-stdlib is a common
     * transitive dependency of Java-only projects. Instead, the routing targets
     * and their layouts are checked for the annotation that the Kotlin compiler
     * adds to every class it compiles.
     *
     * @param routeDataList
     *            the routes registered for the application
     */
    private static void addKotlinUsageStatistics(
            List<RouteData> routeDataList) {
        routeDataList.stream()
                .flatMap(routeData -> Stream.concat(
                        Stream.of(routeData.getNavigationTarget()),
                        routeData.getParentLayouts().stream()))
                .map(UsageStatisticsCollector::getKotlinMetadata)
                .filter(Objects::nonNull).findFirst()
                .ifPresent(metadata -> UsageStatistics
                        .markAsUsed(STATISTIC_KOTLIN, getKotlinVersion(
                                metadata.annotationType().getClassLoader())));
    }

    /**
     * Gets the {@code kotlin.Metadata} annotation of the given class, if the
     * class was compiled by the Kotlin compiler.
     * <p>
     * The annotation is looked up by name, as Flow does not depend on
     * kotlin-stdlib.
     *
     * @param clazz
     *            the class to check
     * @return the Kotlin metadata annotation, or {@code null} if the class was
     *         not compiled from Kotlin
     */
    private static Annotation getKotlinMetadata(Class<?> clazz) {
        return Stream.of(clazz.getAnnotations())
                .filter(annotation -> KOTLIN_METADATA_ANNOTATION
                        .equals(annotation.annotationType().getName()))
                .findFirst().orElse(null);
    }

    /**
     * Resolves the version of kotlin-stdlib in use.
     *
     * @param kotlinClassLoader
     *            the class loader that provides the Kotlin classes
     * @return the Kotlin version, or {@code unknown} if it cannot be resolved
     */
    private static String getKotlinVersion(ClassLoader kotlinClassLoader) {
        try {
            Class<?> kotlinVersion = Class.forName("kotlin.KotlinVersion", true,
                    kotlinClassLoader);
            return kotlinVersion.getField("CURRENT").get(null).toString();
        } catch (Exception e) { // NOSONAR
            getLogger().debug("Cannot resolve the Kotlin version", e);
            return "unknown";
        }
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(UsageStatisticsCollector.class);
    }
}
