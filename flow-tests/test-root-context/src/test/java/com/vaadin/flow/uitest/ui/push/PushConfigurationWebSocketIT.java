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
package com.vaadin.flow.uitest.ui.push;

import java.util.Locale;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.By;

import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.shared.ui.Transport;
import com.vaadin.flow.testcategory.IgnoreOSGi;

@Category(IgnoreOSGi.class)
public class PushConfigurationWebSocketIT extends PushConfigurationTest {

    @Test
    public void testWebsocket() throws InterruptedException {
        findElement(By.id("transport"))
                .findElement(By.id(
                        Transport.WEBSOCKET.name().toLowerCase(Locale.ENGLISH)))
                .click();

        findElement(By.id("push-mode"))
                .findElement(By.id(
                        PushMode.AUTOMATIC.name().toLowerCase(Locale.ENGLISH)))
                .click();

        Assert.assertThat(getStatusText(),
                CoreMatchers.containsString("fallbackTransport: long-polling"));
        Assert.assertThat(getStatusText(),
                CoreMatchers.containsString("transport: websocket"));

        waitForServerCounterToUpdate();

        Assert.assertEquals(Transport.WEBSOCKET.getIdentifier(),
                getTransport());
    }
}
