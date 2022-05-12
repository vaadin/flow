/*
 * Copyright 2000-2022 Vaadin Ltd.
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
package com.vaadin.flow.plugin.maven;

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

        public TestTaskGenerateOpenAPI(File applicationProperties,
                File javaSourceFolder, ClassLoader classLoader, File output) {
            super();
            this.output = output;
        }

        @Override
        public void execute() {
            writeFile(output, "{}");
        }
    }

}
