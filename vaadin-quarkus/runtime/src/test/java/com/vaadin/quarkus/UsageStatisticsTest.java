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
package com.vaadin.quarkus;

import jakarta.enterprise.inject.spi.BeanManager;

import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.UsageStatistics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UsageStatisticsTest {

    @AfterEach
    public void cleanup() {
        UsageStatistics.resetEntries();
    }

    @Test
    public void devMode_callsUsageStatistics() {
        initMocks(false);

        // QuarkusVaadinServlet will call statistics in development mode
        // There will be other entries too to filter out
        List<UsageStatistics.UsageEntry> entries = UsageStatistics.getEntries()
                .filter(entry -> entry.getName().contains("quarkus"))
                .collect(Collectors.toList());
        assertEquals(1, entries.size(), "One quarkus call should be recorded");

        UsageStatistics.UsageEntry entry = entries.get(0);
        assertEquals("flow/quarkus", entry.getName());
    }

    @Test
    public void prodMode_noCallToUsageStatistics() {
        initMocks(true);

        // QuarkusVaadinServlet will call statistics in development mode
        // There will be other entries too to filter out
        List<UsageStatistics.UsageEntry> entries = UsageStatistics.getEntries()
                .filter(entry -> entry.getName().contains("quarkus"))
                .collect(Collectors.toList());
        assertEquals(0, entries.size(),
                "No entries should be available in production mode");
    }

    private void initMocks(boolean productionMode) {
        final QuarkusVaadinServlet servlet = mock(QuarkusVaadinServlet.class);
        final DeploymentConfiguration configuration = mock(
                DeploymentConfiguration.class);
        when(configuration.isProductionMode()).thenReturn(productionMode);
        when(configuration.getInitParameters()).thenReturn(new Properties());
        final BeanManager beanManager = mock(BeanManager.class);
        // creation of quarkus vaadin servlet service calls statistics
        new QuarkusVaadinServletService(servlet, configuration, beanManager);
    }
}
