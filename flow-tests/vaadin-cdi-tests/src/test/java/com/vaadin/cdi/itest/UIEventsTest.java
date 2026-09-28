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

import java.util.List;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.cdi.itest.uievents.NavigationObserver;
import com.vaadin.cdi.itest.uievents.UIEventsView;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.testbench.TestBenchElement;

public class UIEventsTest extends AbstractCdiTest {

    @Deployment(testable = false)
    public static WebArchive deployment() {
        return ArchiveProvider.createWebArchive("uievents", UIEventsView.class,
                NavigationObserver.class);
    }

    @Before
    public void setUp() {
        getDriver().get(getRootURL() + "uievents");
    }

    @Test
    public void navigationEventsObserved() {
        List<TestBenchElement> events = $("div")
                .id(UIEventsView.NAVIGATION_EVENTS).$("label").all();
        Assert.assertEquals(3, events.size());
        assertEventIs(events.get(0), BeforeLeaveEvent.class);
        assertEventIs(events.get(1), BeforeEnterEvent.class);
        assertEventIs(events.get(2), AfterNavigationEvent.class);
    }

    @Test
    public void pollEventObserved() {
        waitForElementPresent(By.id(UIEventsView.POLL_FROM_CLIENT));
        assertTextEquals("true", UIEventsView.POLL_FROM_CLIENT);
    }

    private void assertEventIs(TestBenchElement eventElem,
            Class<?> eventClass) {
        Assert.assertEquals(eventClass.getSimpleName(), eventElem.getText());
    }

}
