/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.base.devserver;

import com.vaadin.flow.server.frontend.EndpointGeneratorTaskFactory;
import com.vaadin.flow.server.frontend.FallibleCommand;
import com.vaadin.flow.server.frontend.TaskGenerateEndpoint;
import com.vaadin.flow.server.frontend.TaskGenerateOpenAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/**
 * A test factory that creates endpoint tasks.
 *
 * @see BuildFrontendMojoTest
 */
public class TestEndpointGeneratorTaskFactory
        implements EndpointGeneratorTaskFactory {
    @Override
    public TaskGenerateEndpoint createTaskGenerateEndpoint(
            File applicationProperties, File openApi, File outputFolder,
            File frontendDirectory) {
        return new TestTaskGenerateEndpoint(applicationProperties, openApi,
                outputFolder, frontendDirectory);
    }

    @Override
    public TaskGenerateOpenAPI createTaskGenerateOpenAPI(File properties,
            File javaSourceFolder, ClassLoader classLoader, File output) {
        return new TestTaskGenerateOpenAPI(properties, javaSourceFolder,
                classLoader, output);
    }

    /**
     * An abstract parent for the test endpoints generator tasks.
     */
    static private abstract class AbstractTestTaskEndpointGenerator
            implements FallibleCommand {
        AbstractTestTaskEndpointGenerator() {
        }

        /**
         * Utility method to write a text file with the specified string
         * content. Creates the file and the containing directories if they did
         * not previously exist.
         *
         * @param file
         *            the file to write
         * @param content
         */
        protected void writeFile(File file, CharSequence content) {
            try {
                Files.createDirectories(file.getParentFile().toPath());
                Files.writeString(file.toPath(), content,
                        StandardCharsets.UTF_8, StandardOpenOption.CREATE);
            } catch (IOException e) {
                log().debug("Unable to write file", e);
            }

        }

        Logger log() {
            return LoggerFactory
                    .getLogger(AbstractTestTaskEndpointGenerator.class);
        }
    }

    /**
     * The test task that generates the TypeScript endpoint files.
     */
    static private class TestTaskGenerateEndpoint extends
            AbstractTestTaskEndpointGenerator implements TaskGenerateEndpoint {

        private final File outputFolder;

        public TestTaskGenerateEndpoint(File applicationProperties,
                File openApi, File outputFolder, File frontendDirectory) {
            super();
            this.outputFolder = outputFolder;
        }

        @Override
        public void execute() {
            writeFile(new File(outputFolder, "connect-client.default.ts"),
                    "// fake test client");
            writeFile(new File(outputFolder, "MyEndpoint.ts"),
                    "// fake test endpoint");
        }
    }

    /**
     * The test task that generates the OpenAPI.json file.
     */
    static private class TestTaskGenerateOpenAPI extends
            AbstractTestTaskEndpointGenerator implements TaskGenerateOpenAPI {

        private final File output;

        private final File javaSourceFolder;

        public TestTaskGenerateOpenAPI(File applicationProperties,
                File javaSourceFolder, ClassLoader classLoader, File output) {
            super();
            this.javaSourceFolder = javaSourceFolder;
            this.output = output;
        }

        @Override
        public void execute() {
            if (Objects
                    .requireNonNull(javaSourceFolder.listFiles()).length > 0) {
                writeFile(output, "{}");
            }
        }
    }

}
