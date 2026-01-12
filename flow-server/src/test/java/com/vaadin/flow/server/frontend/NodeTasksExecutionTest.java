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
package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.file.AccumulatorPathVisitor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.tests.util.MockOptions;

import static com.vaadin.flow.server.Constants.TARGET;

/**
 * Test that commands in NodeTasks are always executed in a predefined order.
 */
@RunWith(Parameterized.class)
public class NodeTasksExecutionTest {

    private static final String DEV_SERVER_VITE = "VITE";

    @Parameterized.Parameters(name = "{0}")
    public static Collection<String> devServers() {
        return List.of(DEV_SERVER_VITE);
    }

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Parameterized.Parameter
    public String devServerImpl;

    private NodeTasks nodeTasks;
    private List<FallibleCommand> commandsMock;
    private List<Class<? extends FallibleCommand>> commandsOrder;
    private List<Class<? extends FallibleCommand>> executionOrder;
    private List<FallibleCommand> commands;

    private Options options;

    @Before
    public void init() throws Exception {

        // Make a builder that doesn't add any commands.
        ClassFinder.DefaultClassFinder finder = new ClassFinder.DefaultClassFinder(
                Collections.singleton(this.getClass()));
        options = new MockOptions(finder, temporaryFolder.getRoot())
                .withBuildDirectory(TARGET)
                .withFrontendDirectory(temporaryFolder.getRoot());
        options.withProductionMode(false);

        nodeTasks = new NodeTasks(options);
        commandsOrder = getCommandOrder(nodeTasks);
        executionOrder = new ArrayList<>(commandsOrder.size());
        commandsMock = mockCommandsRandomOrder(commandsOrder, executionOrder);

        commands = getCommands(nodeTasks);

        // With Vite we always have two default tasks that cannot be removed
        // by configuration options
        commands.clear();

        Assert.assertEquals("No commands should be added initially, "
                + "update mock builder so that we don't automatically add any tasks!",
                0, commands.size());
    }

    private static List<Class<? extends FallibleCommand>> getCommandOrder(
            NodeTasks nodeTasks) throws NoSuchFieldException, SecurityException,
            IllegalArgumentException, IllegalAccessException {
        // get the private list of task execution order
        final Field commandOrderField = NodeTasks.class
                .getDeclaredField("commandOrder");
        commandOrderField.setAccessible(true);
        return (List<Class<? extends FallibleCommand>>) commandOrderField
                .get(nodeTasks);
    }

    private static List<FallibleCommand> getCommands(NodeTasks nodeTasks)
            throws NoSuchFieldException, SecurityException,
            IllegalArgumentException, IllegalAccessException {
        // get the private commands list
        final Field commandsField = NodeTasks.class
                .getDeclaredField("commands");
        commandsField.setAccessible(true);
        return (List<FallibleCommand>) commandsField.get(nodeTasks);
    }

    private void createFeatureFlagsFile(String contents) throws IOException {
        Files.writeString(temporaryFolder
                .newFile(FeatureFlags.PROPERTIES_FILENAME).toPath(), contents);
    }

    @Test
    public void nodeTasks_notExecutedInParallel() throws Exception {
        List<String> result = new ArrayList<>();

        FallibleCommand command1 = new FallibleCommand() {
            @Override
            public void execute() throws ExecutionFailedException {
                try {
                    result.add("Start 1");
                    Thread.sleep(100);
                    result.add("End 1");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        FallibleCommand command2 = new FallibleCommand() {
            @Override
            public void execute() throws ExecutionFailedException {
                try {
                    result.add("Start 2");
                    Thread.sleep(100);
                    result.add("End 2");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        NodeTasks nodeTasks1 = Mockito.spy(new NodeTasks(options));
        getCommands(nodeTasks1).add(command1);
        Mockito.doReturn(1).when(nodeTasks1).getIndex(command1);

        NodeTasks nodeTasks2 = Mockito.spy(new NodeTasks(options));
        getCommands(nodeTasks2).add(command2);
        Mockito.doReturn(1).when(nodeTasks2).getIndex(command2);

        Thread t1 = new Thread(() -> {
            try {
                nodeTasks1.execute();
            } catch (ExecutionFailedException e) {
                e.printStackTrace();
            }
        });
        Thread t2 = new Thread(() -> {
            try {
                nodeTasks2.execute();
            } catch (ExecutionFailedException e) {
                e.printStackTrace();
            }
        });

        t1.start();
        Thread.sleep(100); // Ensure 1 starts and locks before 2 starts
        t2.start();

        t1.join();
        t2.join();

        Assert.assertEquals(List.of("Start 1", "End 1", "Start 2", "End 2"),
                result);
    }

    @Test
    public void nodeTasks_alwaysExecutedInDefinedOrder()
            throws ExecutionFailedException {

        // Assemble the command list with random order
        commands.addAll(commandsMock);

        nodeTasks.execute();

        Assert.assertEquals("Amount of tasks executed was more than expected",
                commandsOrder.size(), executionOrder.size());
        Assert.assertEquals("Tasks were executed in an unexpected order",
                commandsOrder, executionOrder);
    }

    @Test
    public void nodeTasksContainsUnlistedCommand_throwsUnknownTaskException() {
        // Assemble the command list with random order
        commands.add(commandsMock.get(0));
        commands.add(new NewTask());

        Assert.assertThrows(
                "NodeTasks execution should fail due to unknown task in execution list",
                UnknownTaskException.class, nodeTasks::execute);
    }

    @Test
    public void nodeTasks_deletesOldGeneratedFiles() throws Exception {
        options.withCleanOldGeneratedFiles(true);

        NodeTasks spiedNodeTasks = Mockito.spy(new NodeTasks(options));
        // TaskRemoveOldFrontendGeneratedFiles should be the last executed task
        Mockito.doAnswer(i -> i.getArgument(
                0) instanceof TaskRemoveOldFrontendGeneratedFiles ? 1 : 0)
                .when(spiedNodeTasks).getIndex(ArgumentMatchers.any());

        List<Path> generatedFiles = List.of(Paths.get("file.tsx"),
                Paths.get("another.js"), Paths.get("sub", "a.tsx"),
                Paths.get("sub", "nested", "b.js"));
        enqueueCreateGeneratedFilesTasks(spiedNodeTasks, generatedFiles);
        spiedNodeTasks.execute();
        assertOnlyExpectedGeneratedFilesExists(generatedFiles);

        // Simulate execution that generates different files
        generatedFiles = List.of(Paths.get("no-the-same-file.tsx"),
                Paths.get("another.js"), Paths.get("sub", "a.tsx"),
                Paths.get("sub", "b.tsx"),
                Paths.get("sub", "nested-changed", "b.js"));
        enqueueCreateGeneratedFilesTasks(spiedNodeTasks, generatedFiles);
        spiedNodeTasks.execute();
        assertOnlyExpectedGeneratedFilesExists(generatedFiles);

    }

    private void enqueueCreateGeneratedFilesTasks(NodeTasks nodeTasks,
            List<Path> generatedFiles)
            throws NoSuchFieldException, IllegalAccessException {
        List<FallibleCommand> commandList = getCommands(nodeTasks);
        commandList.clear();
        generatedFiles.stream().map(this::createGeneratedFileTask)
                .forEach(commandList::add);
        commandList.add(new TaskRemoveOldFrontendGeneratedFiles(options));
    }

    private void assertOnlyExpectedGeneratedFilesExists(
            List<Path> expectedFiles) throws IOException {
        AccumulatorPathVisitor visitor = new AccumulatorPathVisitor();
        Files.walkFileTree(options.getFrontendGeneratedFolder().toPath(),
                visitor);
        Assert.assertEquals(
                "Expect exactly currently generated files to exists",
                Set.copyOf(expectedFiles),
                Set.copyOf(visitor.relativizeFiles(
                        options.getFrontendGeneratedFolder().toPath(), false,
                        null)));
    }

    private FallibleCommand createGeneratedFileTask(Path relativePath) {
        Path resolved = options.getFrontendGeneratedFolder().toPath()
                .resolve(relativePath);
        return new FileGeneratorTask(resolved.toFile());
    }

    private static class FileGeneratorTask implements FallibleCommand {

        private final File file;

        private GeneratedFilesSupport support;

        FileGeneratorTask(File file) {
            this.file = file;
        }

        @Override
        public void setGeneratedFileSupport(GeneratedFilesSupport support) {
            this.support = support;
        }

        @Override
        public void execute() throws ExecutionFailedException {
            try {
                support.writeIfChanged(file, "test file");
            } catch (IOException e) {
                throw new ExecutionFailedException(e);
            }
        }
    }

    private class NewTask implements FallibleCommand {
        @Override
        public void execute() throws ExecutionFailedException {
        }
    }

    /**
     * Generate mocks for each command and shuffle the list to simulate random
     * add order in NodeTasks.
     *
     * @param commandsOrder
     *            commands to execute array
     * @param executionOrder
     *            array to add execution into
     * @return shuffled list of FallibleCommands to execute
     * @throws ExecutionFailedException
     *             thrown by actual commands
     */
    private List<FallibleCommand> mockCommandsRandomOrder(
            List<Class<? extends FallibleCommand>> commandsOrder,
            List<Class<? extends FallibleCommand>> executionOrder)
            throws ExecutionFailedException {

        List<FallibleCommand> commands = new ArrayList<>(commandsOrder.size());

        for (Class<? extends FallibleCommand> command : commandsOrder) {
            final FallibleCommand mock = Mockito.mock(command);
            commands.add(mock);
            // When execute() is called add executing command to execution list.
            Mockito.doAnswer(invocation -> {
                executionOrder.add(command);
                return null;
            }).when(mock).execute();
        }

        Collections.shuffle(commands);

        return commands;
    }
}
