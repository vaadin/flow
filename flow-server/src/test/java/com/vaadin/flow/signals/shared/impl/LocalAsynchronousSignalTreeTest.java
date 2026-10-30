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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.StringNode;

import com.vaadin.flow.signals.SignalCommand;
import com.vaadin.flow.signals.SignalTestBase;
import com.vaadin.flow.signals.TestUtil;
import com.vaadin.flow.signals.shared.impl.SignalTree.Type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalAsynchronousSignalTreeTest extends SignalTestBase {

    @Test
    void type_isAsynchronous() {
        LocalAsynchronousSignalTree tree = new LocalAsynchronousSignalTree();
        assertEquals(Type.ASYNCHRONOUS, tree.type());
    }

    @Test
    void submit_singleCommand_immediatelyConfirmed() {
        LocalAsynchronousSignalTree tree = new LocalAsynchronousSignalTree();
        AtomicReference<CommandResult> result = new AtomicReference<>();

        SignalCommand command = TestUtil.writeRootValueCommand();
        tree.commitSingleCommand(command, result::set);

        assertTrue(result.get().accepted());
    }

    @Test
    void submitted_afterCommand_immediatelyUpdated() {
        LocalAsynchronousSignalTree tree = new LocalAsynchronousSignalTree();

        tree.commitSingleCommand(TestUtil.writeRootValueCommand());

        assertNotNull(TestUtil.readSubmittedRootValue(tree));
    }

    @Test
    void confirmed_afterCommand_immediatelyUpdated() {
        LocalAsynchronousSignalTree tree = new LocalAsynchronousSignalTree();

        tree.commitSingleCommand(TestUtil.writeRootValueCommand());

        assertNotNull(TestUtil.readConfirmedRootValue(tree));
    }

    @Test
    void multipleCommands_allImmediatelyConfirmed() {
        LocalAsynchronousSignalTree tree = new LocalAsynchronousSignalTree();

        for (int i = 0; i < 3; i++) {
            AtomicReference<CommandResult> result = new AtomicReference<>();
            tree.commitSingleCommand(
                    TestUtil.writeRootValueCommand("value" + i), result::set);
            assertTrue(result.get().accepted());
        }
    }

    @Test
    void submit_withAsyncDispatcher_confirmedOnlyAfterDispatch() {
        TestExecutor dispatcher = useTestEffectDispatcher();
        LocalAsynchronousSignalTree tree = new LocalAsynchronousSignalTree();

        tree.commitSingleCommand(TestUtil.writeRootValueCommand());

        assertNotNull(TestUtil.readSubmittedRootValue(tree));
        assertNull(TestUtil.readConfirmedRootValue(tree));

        dispatcher.runPendingTasks();

        assertNotNull(TestUtil.readConfirmedRootValue(tree));
    }

    @Test
    void submit_concurrentDispatcher_lastWriteWins() throws Exception {
        ExecutorService executor = Executors
                .newThreadPerTaskExecutor(Thread.ofVirtual().factory());
        useEffectDispatcher(executor);
        LocalAsynchronousSignalTree tree = new LocalAsynchronousSignalTree();

        try {
            for (int i = 0; i < 1000; i++) {
                CompletableFuture<CommandResult> first = new CompletableFuture<>();
                CompletableFuture<CommandResult> second = new CompletableFuture<>();
                tree.commitSingleCommand(TestUtil.writeRootValueCommand("old"),
                        first::complete);
                tree.commitSingleCommand(
                        TestUtil.writeRootValueCommand("new" + i),
                        second::complete);

                assertTrue(first.get(10, TimeUnit.SECONDS).accepted());
                assertTrue(second.get(10, TimeUnit.SECONDS).accepted());
                assertEquals(new StringNode("new" + i),
                        TestUtil.readConfirmedRootValue(tree));
            }
        } finally {
            executor.shutdownNow();
        }
    }
}
