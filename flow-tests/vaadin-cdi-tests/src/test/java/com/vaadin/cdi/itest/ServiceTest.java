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

import java.io.IOException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.cdi.itest.service.BootstrapCustomizeView;
import com.vaadin.cdi.itest.service.BootstrapCustomizer;
import com.vaadin.cdi.itest.service.EventObserver;
import com.vaadin.cdi.itest.service.ServiceView;
import com.vaadin.cdi.itest.service.TestErrorHandler;
import com.vaadin.cdi.itest.service.TestSystemMessagesProvider;
import com.vaadin.flow.server.SessionDestroyEvent;
import com.vaadin.flow.server.SessionInitEvent;
import com.vaadin.flow.server.UIInitEvent;

import static com.vaadin.cdi.itest.service.ServiceView.ACTION;
import static com.vaadin.cdi.itest.service.ServiceView.EXPIRE;
import static com.vaadin.cdi.itest.service.ServiceView.FAIL;

public class ServiceTest extends AbstractCdiTest {

    @Deployment(testable = false)
    public static WebArchive deployment() {
        return ArchiveProvider.createWebArchive("services",
                BootstrapCustomizer.class, BootstrapCustomizeView.class,
                ServiceView.class, EventObserver.class, TestErrorHandler.class,
                TestSystemMessagesProvider.class);
    }

    @Before
    public void setUp() throws Exception {
        resetCounts();
    }

    @Test
    public void bootstrapCustomizedByServiceInitEventObserver() {
        getDriver().get(getTestURL() + "bootstrap");
        assertTextEquals(BootstrapCustomizer.APPENDED_TXT,
                BootstrapCustomizer.APPENDED_ID);
    }

    @Test
    public void sessionExpiredMessageCustomized() {
        open();
        click(EXPIRE);
        click(ACTION);
        assertSystemMessageEquals(TestSystemMessagesProvider.EXPIRED_BY_TEST);
    }

    @Test
    public void errorHandlerCustomized() throws IOException {
        String counter = TestErrorHandler.class.getSimpleName();
        assertCountEquals(0, counter);
        open();
        click(FAIL);
        assertCountEquals(1, counter);
    }

    @Test
    public void sessionInitEventObserved() throws IOException {
        String initCounter = SessionInitEvent.class.getSimpleName();
        assertCountEquals(0, initCounter);
        getDriver().manage().deleteAllCookies();
        open();
        assertCountEquals(1, initCounter);
    }

    @Test
    public void sessionDestroyEventObserved() throws IOException {
        String destroyCounter = SessionDestroyEvent.class.getSimpleName();
        assertCountEquals(0, destroyCounter);
        open();
        assertCountEquals(0, destroyCounter);
        click(EXPIRE);
        assertCountEquals(1, destroyCounter);
    }

    @Test
    public void uiInitEventObserved() throws IOException {
        String uiInitCounter = UIInitEvent.class.getSimpleName();
        assertCountEquals(0, uiInitCounter);
        open();
        assertCountEquals(1, uiInitCounter);
    }

    private void assertSystemMessageEquals(String expected) {
        WebElement message = waitUntil(d -> findElement(
                By.cssSelector("div.v-system-error div.message")));
        Assert.assertEquals(expected, message.getText());
    }
}
