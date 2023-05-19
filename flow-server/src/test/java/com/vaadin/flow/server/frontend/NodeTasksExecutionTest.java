package com.vaadin.flow.server.frontend;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.ExecutionFailedException;

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

    @Before
    public void init() throws Exception {

        // Make a builder that doesn't add any commands.
        Options options = new Options(Mockito.mock(Lookup.class), null)
                .withBuildDirectory(TARGET);
        options.withProductionMode(false);

        nodeTasks = new NodeTasks(options);

        // get the private list of task execution order
        final Field commandOrderField = NodeTasks.class
                .getDeclaredField("commandOrder");
        commandOrderField.setAccessible(true);
        commandsOrder = (List<Class<? extends FallibleCommand>>) commandOrderField
                .get(nodeTasks);

        executionOrder = new ArrayList<>(commandsOrder.size());
        commandsMock = mockCommandsRandomOrder(commandsOrder, executionOrder);

        // get the private commands list
        final Field commandsField = NodeTasks.class
                .getDeclaredField("commands");
        commandsField.setAccessible(true);
        commands = (List<FallibleCommand>) commandsField.get(nodeTasks);

        // With Vite we always have two default tasks that cannot be removed
        // by configuration options
        commands.clear();

        Assert.assertEquals("No commands should be added initially, "
                + "update mock builder so that we don't automatically add any tasks!",
                0, commands.size());
    }

    private void createFeatureFlagsFile(String contents) throws IOException {
        Files.writeString(temporaryFolder
                .newFile(FeatureFlags.PROPERTIES_FILENAME).toPath(), contents);
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
