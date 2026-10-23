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

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.node.StringNode;

import com.vaadin.flow.signals.SignalCommand;
import com.vaadin.flow.signals.SignalTestBase;
import com.vaadin.flow.signals.TestUtil;
import com.vaadin.flow.signals.shared.impl.SignalTree.Type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    void submit_dispatcherRunsTasksOutOfOrder_lastWriteWins() {
        TestExecutor dispatcher = useTestEffectDispatcher();
        LocalAsynchronousSignalTree tree = new LocalAsynchronousSignalTree();
        AtomicReference<CommandResult> firstResult = new AtomicReference<>();

        tree.commitSingleCommand(TestUtil.writeRootValueCommand("first"),
                firstResult::set);
        tree.commitSingleCommand(TestUtil.writeRootValueCommand("second"));

        dispatcher.runPendingTasksInReverseOrder();

        assertTrue(firstResult.get().accepted());
        assertEquals(new StringNode("second"),
                TestUtil.readConfirmedRootValue(tree));
        assertEquals(new StringNode("second"),
                TestUtil.readSubmittedRootValue(tree));
    }

    @Test
    void submit_concurrentDispatcher_lastWriteWins() throws Exception {
        ExecutorService executor = Executors
                .newThreadPerTaskExecutor(Thread.ofVirtual().factory());
        // Fail fast instead of timing out if a task throws
        CompletableFuture<Void> taskFailure = new CompletableFuture<>();
        useEffectDispatcher(task -> executor.execute(() -> {
            try {
                task.run();
            } catch (Throwable t) {
                taskFailure.completeExceptionally(t);
            }
        }));
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

                CompletableFuture.anyOf(CompletableFuture.allOf(first, second),
                        taskFailure).get(10, TimeUnit.SECONDS);
                assertTrue(first.join().accepted());
                assertTrue(second.join().accepted());
                assertEquals(new StringNode("new" + i),
                        TestUtil.readConfirmedRootValue(tree));
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void submit_dispatcherDropsTask_confirmedByNextTask() {
        useTestEffectDispatcher();
        LocalAsynchronousSignalTree tree = new LocalAsynchronousSignalTree();
        AtomicReference<CommandResult> firstResult = new AtomicReference<>();

        tree.commitSingleCommand(TestUtil.writeRootValueCommand("first"),
                firstResult::set);
        // Replacing the dispatcher drops the task for the first write
        TestExecutor dispatcher = useTestEffectDispatcher();
        tree.commitSingleCommand(TestUtil.writeRootValueCommand("second"));

        dispatcher.runPendingTasks();

        assertTrue(firstResult.get().accepted());
        assertEquals(new StringNode("second"),
                TestUtil.readConfirmedRootValue(tree));
    }

    @Test
    void submit_dispatcherRejectsTask_pendingBatchesConfirmedImmediately() {
        TestExecutor dispatcher = useTestEffectDispatcher();
        LocalAsynchronousSignalTree tree = new LocalAsynchronousSignalTree();
        AtomicReference<CommandResult> firstResult = new AtomicReference<>();
        AtomicReference<CommandResult> secondResult = new AtomicReference<>();

        tree.commitSingleCommand(TestUtil.writeRootValueCommand("first"),
                firstResult::set);
        useEffectDispatcher(task -> {
            throw new RejectedExecutionException();
        });
        tree.commitSingleCommand(TestUtil.writeRootValueCommand("second"),
                secondResult::set);

        assertTrue(firstResult.get().accepted());
        assertTrue(secondResult.get().accepted());
        assertEquals(new StringNode("second"),
                TestUtil.readConfirmedRootValue(tree));

        dispatcher.runPendingTasks();

        assertEquals(new StringNode("second"),
                TestUtil.readConfirmedRootValue(tree));
    }

    @Test
    void submit_immediateDispatcherAndHandlerThrows_exceptionPropagated() {
        useEffectDispatcher(Runnable::run);
        LocalAsynchronousSignalTree tree = new LocalAsynchronousSignalTree();
        IllegalStateException failure = new IllegalStateException();

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> tree.commitSingleCommand(TestUtil.writeRootValueCommand(),
                        result -> {
                            throw failure;
                        }));

        assertSame(failure, thrown);
    }

    static List<Throwable> handlerFailures() {
        return List.of(new IllegalStateException(), new AssertionError());
    }

    @ParameterizedTest
    @MethodSource("handlerFailures")
    void confirm_resultHandlersThrow_laterBatchesStillConfirmed(
            Throwable failure) {
        TestExecutor dispatcher = useTestEffectDispatcher();
        LocalAsynchronousSignalTree tree = new LocalAsynchronousSignalTree();
        AtomicReference<CommandResult> thirdResult = new AtomicReference<>();
        CommandsAndHandlers.CommandResultHandler throwingHandler = result -> {
            if (failure instanceof Error error) {
                throw error;
            }
            throw (RuntimeException) failure;
        };

        // Same instance thrown twice must not be suppressed into itself
        tree.commitSingleCommand(TestUtil.writeRootValueCommand("first"),
                throwingHandler);
        tree.commitSingleCommand(TestUtil.writeRootValueCommand("second"),
                throwingHandler);
        tree.commitSingleCommand(TestUtil.writeRootValueCommand("third"),
                thirdResult::set);

        Throwable thrown = assertThrows(failure.getClass(),
                dispatcher::runPendingTasksInReverseOrder);

        assertSame(failure, thrown);
        assertTrue(thirdResult.get().accepted());
        assertEquals(new StringNode("third"),
                TestUtil.readConfirmedRootValue(tree));
    }
}
