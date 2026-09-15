/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import java.util.concurrent.atomic.AtomicReference;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.test.AbstractChromeIT;

@QuarkusIntegrationTest
public class InstantiatorTestIT extends AbstractChromeIT {

    @Test
    public void navigationCorrectlyHandlesProxiedViews() {
        open();

        String prevUuid = null;
        AtomicReference<String> prevCounter = new AtomicReference<>("");
        for (int i = 0; i < 5; i++) {
            String uuid = waitUntil(d -> d.findElement(By.id("COMPONENT_ID")))
                    .getText();

            waitUntil(d -> !prevCounter.get()
                    .equals(d.findElement(By.id("CLICK_COUNTER")).getText()));

            if (prevUuid != null) {
                Assert.assertEquals("UUID should not have been changed",
                        prevUuid, uuid);
            } else {
                prevUuid = uuid;
            }
            String counter = findElement(By.id("CLICK_COUNTER")).getText();
            Assert.assertEquals(
                    "Parameter and counter should have the same value",
                    "P:" + i + ", C:" + i, counter);

            prevCounter.set(counter);

            $("a").first().click();
        }
    }

    @Override
    protected String getTestPath() {
        return "/proxied";
    }

    private void checkLogs() {
        checkLogsForErrors(msg -> msg.contains("webpack-internal:")
                && msg.contains("VaadinDevmodeGizmo") && msg.contains("Event"));
    }
}
