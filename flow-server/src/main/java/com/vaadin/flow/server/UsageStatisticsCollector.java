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
import java.util.List;

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

    private UsageStatisticsCollector() {
        // Only static methods here, no need to create an instance
    }

    /**
     * Collects the usage statistics of the given service.
     * <p>
     * Statistics are only gathered in development mode.
     *
     * @param service
     *            the service to collect the statistics of
     */
    static void collect(VaadinService service) {
        DeploymentConfiguration configuration = service
                .getDeploymentConfiguration();
        if (configuration.isProductionMode()) {
            return;
        }
        List<RouteData> routeDataList = service.getRouteRegistry()
                .getRegisteredRoutes();
        if (!routeDataList.isEmpty()) {
            addRouterUsageStatistics();
        }
        addAutoLayoutUsageStatistics(service);
        addFrontendToolUsageStatistics(configuration);
    }

    private static void addFrontendToolUsageStatistics(
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
}
