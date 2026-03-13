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
package com.vaadin.flow.signals.shared;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.SignalsUnitTest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.signals.Id;
import com.vaadin.flow.signals.SignalCommand;
import com.vaadin.flow.signals.function.CommandValidator;
import com.vaadin.flow.signals.shared.impl.AsynchronousSignalTree;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that an effect bound to a shared signal recovers after out-of-order
 * background confirms temporarily disrupt the observer chain.
 * <p>
 * When background virtual threads confirm commands out of order,
 * {@code confirm()} with {@code confirmedFromHead=false} calls
 * {@code notifyObservers()} on a thread where {@code UI.getCurrent()} is null.
 * This causes the effect's re-validation to be dispatched asynchronously via
 * {@code ui.access()}. The effect recovers once {@code runPendingAccessTasks()}
 * drains the access queue, re-registering the observer so that subsequent
 * signal changes fire the effect again.
 */
public class OutOfOrderConfirmEffectTest extends SignalsUnitTest {

    /**
     * An AsynchronousSignalTree that captures confirm calls instead of
     * dispatching them to background threads.
     */
    static class CapturingAsyncTree extends AsynchronousSignalTree {
        final List<List<SignalCommand>> pendingConfirms = new ArrayList<>();

        @Override
        protected void submit(List<SignalCommand> commands) {
            pendingConfirms.add(commands);
        }

        /**
         * Confirms all pending commands in reverse order on a background thread
         * (where UI.getCurrent() returns null), simulating the virtual thread
         * scheduling race.
         */
        void confirmAllInReverseOnBackgroundThread()
                throws InterruptedException {
            List<List<SignalCommand>> toConfirm = new ArrayList<>(
                    pendingConfirms);
            pendingConfirms.clear();

            Thread bgThread = new Thread(() -> {
                // Confirm in reverse order so confirmedFromHead=false
                for (int i = toConfirm.size() - 1; i >= 0; i--) {
                    confirm(toConfirm.get(i));
                }
            });
            bgThread.start();
            bgThread.join();
        }
    }

    @Test
    public void outOfOrderConfirm_effectRecoversAfterRunPendingAccessTasks()
            throws InterruptedException {
        CapturingAsyncTree tree = new CapturingAsyncTree();

        SharedNumberSignal signal = new SharedNumberSignal(tree, Id.ZERO,
                CommandValidator.ACCEPT_ALL);
        signal.set(100.0);

        Element element = new Element("input");
        UI.getCurrent().getElement().appendChild(element);
        element.bindAttribute("max", signal.map(Object::toString));
        assertEquals("100.0", element.getAttribute("max"));

        signal.set(150.5);
        assertEquals("150.5", element.getAttribute("max"));

        // Simulate the race: confirm in reverse order on a background thread
        // where UI.getCurrent() is null. This causes the effect's observer to
        // be consumed and its re-validation to be dispatched via ui.access().
        tree.confirmAllInReverseOnBackgroundThread();

        // Simulate what the framework does at the start of the next request:
        // drain the pending access queue so the deferred effect runs.
        VaadinService.getCurrent()
                .runPendingAccessTasks(VaadinSession.getCurrent());

        // After draining the access queue, the effect has recovered and
        // re-registered its observer.
        signal.set(200.0);
        assertEquals("200.0", element.getAttribute("max"));
    }
}
