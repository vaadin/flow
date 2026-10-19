/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.cdi.itest;

import java.util.concurrent.atomic.AtomicReference;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.cdi.itest.smoke.CdiView;
import com.vaadin.cdi.itest.smoke.ProxiedNavigationTargetView;
import com.vaadin.cdi.itest.smoke.UselessInterceptor;

@SuppressWarnings("ArquillianTooManyDeployment")
public class SmokeTest extends AbstractCdiTest {

    @Deployment(name = "noncdi", testable = false)
    public static WebArchive createCdiServletDisabledDeployment() {
        return ArchiveProvider.createWebArchive("noncdi-test", CdiView.class)
                .addAsWebInfResource(ArchiveProvider.class.getClassLoader()
                        .getResource("disablecdi-web.xml"), "web.xml");
    }

    @Deployment(name = "cdi", testable = false)
    public static WebArchive createCdiServletEnabledDeployment() {
        return ArchiveProvider.createWebArchive("cdi-test", CdiView.class,
                ProxiedNavigationTargetView.class, UselessInterceptor.class);
    }

    @Before
    public void setUp() {
        open();
        $("button").first().click();
    }

    @Test
    @OperateOnDeployment("noncdi")
    public void injectionDoesNotHappenWithDisabledCdiServlet() {
        assertLabelEquals("no CDI");
    }

    @Test
    @OperateOnDeployment("cdi")
    public void injectionHappensWithEnabledCdiServlet() {
        assertLabelEquals("hello CDI");
    }

    @Test
    @OperateOnDeployment("cdi")
    public void navigationCorrectlyHandlesProxiedViews() {
        getDriver().get(getTestURL() + "proxied");
        waitForDevServer();

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

    private void assertLabelEquals(String expected) {
        Assert.assertEquals(expected, $("label").first().getText());
    }

}
