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
package com.vaadin.flow.quarkus.it;

import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.test.AbstractChromeIT;

/**
 * Verifies that a system property set when the application starts reaches the
 * deployment configuration. It only fails in a native image, where the Vaadin
 * servlets would otherwise be initialized while the image is built.
 */
@QuarkusIntegrationTest
@WithTestResource(RuntimeConfigurationIT.HeartbeatIntervalProperty.class)
public class RuntimeConfigurationIT extends AbstractChromeIT {

    private static final String HEARTBEAT_INTERVAL = "211";

    @Override
    protected String getTestPath() {
        return "/runtime-configuration";
    }

    @Test
    void systemPropertyAtStartup_appliedToDeploymentConfiguration() {
        open();
        waitForElementPresent(
                By.id(RuntimeConfigurationView.HEARTBEAT_INTERVAL_ID));

        Assertions.assertEquals(HEARTBEAT_INTERVAL,
                findElement(
                        By.id(RuntimeConfigurationView.HEARTBEAT_INTERVAL_ID))
                        .getText());
    }

    /**
     * Passes the heartbeat interval to the launched application as a system
     * property.
     */
    public static class HeartbeatIntervalProperty
            implements QuarkusTestResourceLifecycleManager {

        @Override
        public Map<String, String> start() {
            return Map.of("vaadin.heartbeatInterval", HEARTBEAT_INTERVAL);
        }

        @Override
        public void stop() {
        }
    }
}
