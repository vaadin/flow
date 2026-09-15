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
package com.vaadin.flow.quarkus.it.pushdispatch;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.test.AbstractChromeIT;

/**
 * Drives the {@link PushBlockingAfterNavView} scenario end-to-end. The test
 * first lands on {@link LandingView}, which gives Vaadin time to establish the
 * Push websocket. Then it clicks an in-app
 * {@link com.vaadin.flow.router.RouterLink RouterLink} to navigate to
 * {@code /push-blocking}. With {@code @Push(WEBSOCKET)} active, that navigation
 * message flows through the existing Push websocket on the Vert.x event loop,
 * so {@code afterNavigation} runs on the event loop. The synchronous Quarkus
 * REST client call there then exposes the dispatch-to-worker setting:
 * <ul>
 * <li>With the Quarkus default ({@code dispatch-to-worker=false}) RESTEasy
 * Reactive throws
 * {@link org.jboss.resteasy.reactive.common.core.BlockingNotAllowedException},
 * Vaadin routes to {@link PushBlockingErrorView}, and the page displays
 * {@link PushBlockingErrorView#ERROR_TEXT} — the test fails.</li>
 * <li>With the extension's default ({@code dispatch-to-worker=true})
 * afterNavigation runs on a worker, the REST call completes, and the page
 * displays {@link PushBlockingAfterNavView#READY_TEXT}.</li>
 * </ul>
 * A full browser reload doesn't reproduce the bug because Vaadin's bootstrap
 * still uses XHR for the first UIDL of a fresh page; only in-app navigation
 * over the already-open Push websocket forces the event-loop dispatch.
 * <p>
 * Extends {@link AbstractChromeIT} so that a failed assertion produces a
 * screenshot under {@code target/error-screenshots/} via
 * {@link com.vaadin.flow.test.ScreenshotsOnFailureExtension}.
 */
@QuarkusIntegrationTest
public class PushBlockingAfterNavIT extends AbstractChromeIT {

    @Override
    protected String getTestPath() {
        return "/";
    }

    @Test
    public void afterNavigationBlocking_inAppNavigationStaysHealthy() {
        // Land on the trivial route first. This gives Vaadin's bootstrap time
        // to send the first UIDL response (via XHR) and the browser to open
        // the Push websocket; the link click that follows then travels over
        // that websocket — on the event loop.
        open();
        waitForElementPresent(By.id(LandingView.LINK_ID));
        findElement(By.id(LandingView.LINK_ID)).click();
        waitForReady();
    }

    private void waitForReady() {
        waitUntil(d -> {
            // Fail fast if the error route activated.
            if (!d.findElements(By.id(PushBlockingErrorView.VIEW_ID))
                    .isEmpty()) {
                String stackTrace = d
                        .findElements(
                                By.id(PushBlockingErrorView.STACKTRACE_ID))
                        .stream().findFirst().map(elem -> elem.getText())
                        .orElse("<no stack trace captured>");
                throw new AssertionError(
                        "RESTEasy Reactive refused to block on the event loop "
                                + "(BlockingNotAllowedException routed to "
                                + PushBlockingErrorView.VIEW_ID
                                + "). This means the navigation ran on the "
                                + "Vert.x event loop — the extension default "
                                + "quarkus.websocket.dispatch-to-worker=true "
                                + "is not in effect.\n\nCaptured server stack "
                                + "trace:\n" + stackTrace);
            }
            return !d.findElements(By.id(PushBlockingAfterNavView.VIEW_ID))
                    .isEmpty()
                    && PushBlockingAfterNavView.READY_TEXT.equals(d
                            .findElement(
                                    By.id(PushBlockingAfterNavView.VIEW_ID))
                            .getText());
        }, 15);
        Assertions.assertEquals(PushBlockingAfterNavView.READY_TEXT,
                findElement(By.id(PushBlockingAfterNavView.VIEW_ID)).getText());
    }
}
