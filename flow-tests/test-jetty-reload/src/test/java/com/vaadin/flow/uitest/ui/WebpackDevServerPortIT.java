/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui;

import net.jcip.annotations.NotThreadSafe;
import org.apache.commons.lang3.math.NumberUtils;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;

import com.vaadin.flow.testutil.ChromeBrowserTest;

@NotThreadSafe
public class WebpackDevServerPortIT extends ChromeBrowserTest {

    @Test
    public void webpackDevServerPortShouldBeReusedOnReload() {
        open();

        String initialUUID = findElement(
                By.id(WebpackDevServerPortView.UUID_ID)).getText();
        Assert.assertEquals(36, initialUUID.length());

        String initialPort = findElement(
                By.id(WebpackDevServerPortView.WEBPACK_PORT_ID)).getText();
        Assert.assertTrue(NumberUtils.isDigits(initialPort));

        // trigger jetty reload
        findElement(By.id(WebpackDevServerPortView.TRIGGER_RELOAD_ID)).click();

        // keep refreshing until page comes back (the UUID has changed)
        boolean reconnected = false;
        for (int i = 0; i < 10; i++) {
            getDriver().navigate().refresh();
            try {
                waitUntil(driver -> !initialUUID.equals(
                        findElement(By.id(WebpackDevServerPortView.UUID_ID))
                                .getText()),
                        2);
            } catch (TimeoutException e) {
                continue;
            }
            reconnected = true;
            break;
        }
        Assert.assertTrue(reconnected);

        // then the port number is unchanged
        String port = findElement(
                By.id(WebpackDevServerPortView.WEBPACK_PORT_ID)).getText();
        Assert.assertEquals(initialPort, port);

    }
}
