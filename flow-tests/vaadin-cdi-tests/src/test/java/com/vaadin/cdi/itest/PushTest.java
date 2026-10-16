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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.SessionScoped;

import java.io.File;
import java.lang.annotation.Annotation;

import net.jcip.annotations.NotThreadSafe;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.By;

import com.vaadin.cdi.annotation.RouteScoped;
import com.vaadin.cdi.annotation.UIScoped;
import com.vaadin.cdi.annotation.VaadinServiceScoped;
import com.vaadin.cdi.annotation.VaadinSessionScoped;
import com.vaadin.cdi.itest.push.ManagedExecutorPushComponent;
import com.vaadin.cdi.itest.push.PushComponent;
import com.vaadin.cdi.itest.push.WebsocketPushView;
import com.vaadin.cdi.itest.push.WebsocketXhrPushView;

@NotThreadSafe
@Category(NeedsCDICompliantContainer.class)
public class PushTest extends AbstractCdiTest {

    @Deployment(testable = false)
    public static WebArchive deployment() {
        return ArchiveProvider.createWebArchive("push-managed",
                webArchive -> webArchive
                        .addClasses(WebsocketPushView.class,
                                WebsocketXhrPushView.class, PushComponent.class,
                                ManagedExecutorPushComponent.class)
                        .addAsResource(new File("target/classes/META-INF")));
    }

    @Test
    public void wsWithXhrBackgroundRequestAndSessionDoesNotActive() {
        getDriver().get(getTestURL() + "websocket-xhr");
        click(PushComponent.RUN_BACKGROUND);
        waitForPush();
        assertAllExceptRequestAndSessionActive();
    }

    @Test
    public void wsWithXhrForegroundAllContextsActive() {
        getDriver().get(getTestURL() + "websocket-xhr");
        click(PushComponent.RUN_FOREGROUND);
        assertContextActive(RequestScoped.class, true);
        assertContextActive(SessionScoped.class, true);
        assertContextActive(ApplicationScoped.class, true);
        assertVaadinContextsActive();
    }

    @Test
    public void wsNoXhrBackgroundRequestAndSessionDoesNotActive() {
        getDriver().get(getTestURL() + "websocket");
        click(PushComponent.RUN_BACKGROUND);
        waitForPush();
        assertAllExceptRequestAndSessionActive();
    }

    @Test
    public void wsNoXhrForegroundRequestAndSessionDoesNotActive() {
        getDriver().get(getTestURL() + "websocket");
        click(PushComponent.RUN_FOREGROUND);
        assertAllExceptRequestAndSessionActive();
    }

    private void assertAllExceptRequestAndSessionActive() {
        assertContextActive(RequestScoped.class, false);
        assertContextActive(SessionScoped.class, false);
        assertContextActive(ApplicationScoped.class, true);
        assertVaadinContextsActive();
    }

    private void assertVaadinContextsActive() {
        assertContextActive(RouteScoped.class, true);
        assertContextActive(UIScoped.class, true);
        assertContextActive(VaadinSessionScoped.class, true);
        assertContextActive(VaadinServiceScoped.class, true);
    }

    private void assertContextActive(Class<? extends Annotation> scope,
            boolean active) {
        assertTextEquals(active + "", scope.getName());
    }

    private void waitForPush() {
        waitForElementPresent(By.id(RequestScoped.class.getName()));
    }
}
