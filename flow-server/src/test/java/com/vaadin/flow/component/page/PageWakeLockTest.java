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
package com.vaadin.flow.component.page;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.nodefeature.ElementListenerMap;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PageWakeLockTest {

    @Test
    void requestWakeLock_executesCorrectJs() {
        MockUI ui = new MockUI();
        Page page = new Page(ui);

        page.requestWakeLock();

        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        assertTrue(invocations.stream()
                .anyMatch(i -> i.getInvocation().getExpression()
                        .contains("window.Vaadin.Flow.wakeLock.request")));
    }

    @Test
    void releaseWakeLock_executesCorrectJs() {
        MockUI ui = new MockUI();
        Page page = new Page(ui);

        page.releaseWakeLock();

        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        assertTrue(invocations.stream()
                .anyMatch(i -> i.getInvocation().getExpression()
                        .contains("window.Vaadin.Flow.wakeLock.release()")));
    }

    @Test
    void isWakeLockActive_executesCorrectJs() {
        MockUI ui = new MockUI();
        Page page = new Page(ui);

        page.isWakeLockActive();

        List<PendingJavaScriptInvocation> invocations = ui
                .dumpPendingJsInvocations();
        assertTrue(invocations.stream()
                .anyMatch(i -> i.getInvocation().getExpression()
                        .contains("window.Vaadin.Flow.wakeLock.isActive()")));
    }

    @Test
    void addWakeLockReleaseListener_notifiedOnRelease() {
        MockUI ui = new MockUI();
        Page page = new Page(ui);

        AtomicInteger count = new AtomicInteger();
        page.addWakeLockReleaseListener(count::incrementAndGet);

        fireWakeLockReleaseEvent(ui);
        assertTrue(count.get() == 1);
    }

    private void fireWakeLockReleaseEvent(MockUI ui) {
        ObjectNode eventData = JacksonUtils.createObjectNode();
        ui.getElement().getNode().getFeature(ElementListenerMap.class)
                .fireEvent(new DomEvent(ui.getElement(),
                        "vaadin-wakelock-release", eventData));
    }
}
