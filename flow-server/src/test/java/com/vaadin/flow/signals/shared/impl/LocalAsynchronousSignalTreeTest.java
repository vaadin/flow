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
package com.vaadin.flow.signals.shared.impl;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.signals.SignalCommand;
import com.vaadin.flow.signals.TestUtil;
import com.vaadin.flow.signals.shared.impl.SignalTree.Type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LocalAsynchronousSignalTreeTest {

    @Test
    void type_isAsynchronous() {
        LocalAsynchronousSignalTree tree = new LocalAsynchronousSignalTree();
        assertEquals(Type.ASYNCHRONOUS, tree.type());
    }

    @Test
    void submit_singleCommand_eventuallyConfirmed() throws Exception {
        LocalAsynchronousSignalTree tree = new LocalAsynchronousSignalTree();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<CommandResult> result = new AtomicReference<>();

        SignalCommand command = TestUtil.writeRootValueCommand();
        tree.commitSingleCommand(command, r -> {
            result.set(r);
            latch.countDown();
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS),
                "Confirmation did not arrive in time");
        assertTrue(result.get().accepted());
    }

    @Test
    void submitted_afterCommand_immediatelyUpdated() {
        LocalAsynchronousSignalTree tree = new LocalAsynchronousSignalTree();

        tree.commitSingleCommand(TestUtil.writeRootValueCommand());

        assertNotNull(TestUtil.readSubmittedRootValue(tree));
    }

    @Test
    void confirmed_afterCommand_eventuallyUpdated() throws Exception {
        LocalAsynchronousSignalTree tree = new LocalAsynchronousSignalTree();
        CountDownLatch latch = new CountDownLatch(1);

        tree.commitSingleCommand(TestUtil.writeRootValueCommand(),
                r -> latch.countDown());

        assertTrue(latch.await(5, TimeUnit.SECONDS),
                "Confirmation did not arrive in time");
        assertNotNull(TestUtil.readConfirmedRootValue(tree));
    }

    @Test
    void multipleCommands_allEventuallyConfirmed() throws Exception {
        LocalAsynchronousSignalTree tree = new LocalAsynchronousSignalTree();
        CountDownLatch latch = new CountDownLatch(3);

        for (int i = 0; i < 3; i++) {
            tree.commitSingleCommand(
                    TestUtil.writeRootValueCommand("value" + i),
                    r -> latch.countDown());
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS),
                "Not all confirmations arrived in time");
    }
}
