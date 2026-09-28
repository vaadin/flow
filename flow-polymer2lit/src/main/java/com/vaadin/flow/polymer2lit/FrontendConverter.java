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
package com.vaadin.flow.polymer2lit;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;

import com.vaadin.flow.internal.FileIOUtils;
import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.internal.FrontendUtils.CommandExecutionException;
import com.vaadin.flow.server.frontend.FrontendTools;

/**
 * A converter that converts Polymer-based {@code *.js} source files to Lit.
 *
 * Effectively, this is a wrapper around the {@code convert.ts} script.
 */
public class FrontendConverter implements AutoCloseable {
    private static final String CONVERTER_EXECUTABLE_PATH = "/META-INF/frontend/generated/convert.js";

    private final FrontendTools frontendTools;

    private final Path tempDirPath;

    private final Path converterTempPath;

    /**
     * Creates a converter that runs the bundled {@code convert.js} script with
     * the Node.js executable provided by the given tools.
     * <p>
     * The script is unpacked into a temporary directory that is removed again
     * by {@link #close()}, so the converter has to be closed after use.
     *
     * @param frontendTools
     *            the tools used to locate the Node.js executable
     * @throws IOException
     *             if the temporary directory or the script copy cannot be
     *             created
     */
    public FrontendConverter(FrontendTools frontendTools) throws IOException {
        this.frontendTools = frontendTools;
        this.tempDirPath = Files.createTempDirectory("converter");
        this.converterTempPath = tempDirPath.resolve("converter.js");
        try (InputStream resourceAsStream = getClass()
                .getResourceAsStream(CONVERTER_EXECUTABLE_PATH)) {
            Files.copy(resourceAsStream, converterTempPath,
                    StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Override
    public void close() throws IOException {
        // cleanup by deleting all temp files
        FileIOUtils.delete(converterTempPath);
        FileIOUtils.delete(tempDirPath);
    }

    /**
     * Converts a single Polymer-based {@code *.js} file to Lit in place.
     * <p>
     * Files that do not contain {@code PolymerElement} are left untouched.
     *
     * @param filePath
     *            the file to convert
     * @param useLit1
     *            {@code true} to generate Lit 1 compatible output,
     *            {@code false} to target the current Lit version
     * @param disableOptionalChaining
     *            {@code true} to avoid the optional chaining operator in the
     *            generated output, for tooling that cannot parse it
     * @return {@code true} if the file was converted, {@code false} if it is
     *         not a Polymer file
     * @throws IOException
     *             if the file cannot be read or written
     * @throws InterruptedException
     *             if waiting for the converter process is interrupted
     * @throws CommandExecutionException
     *             if the converter process fails
     */
    public boolean convertFile(Path filePath, boolean useLit1,
            boolean disableOptionalChaining) throws IOException,
            InterruptedException, CommandExecutionException {
        if (!readFile(filePath).contains("PolymerElement")) {
            return false;
        }

        List<String> command = new ArrayList<>();
        command.add(this.frontendTools.getNodeExecutable());
        command.add(this.converterTempPath.toFile().getAbsolutePath());
        command.add(filePath.toFile().getAbsolutePath());

        if (useLit1) {
            command.add("-1");
        }

        if (disableOptionalChaining) {
            command.add("-disable-optional-chaining");
        }

        FrontendUtils.executeCommand(command);
        return true;
    }

    private String readFile(Path filePath) throws IOException {
        try (FileInputStream stream = new FileInputStream(filePath.toFile())) {
            return IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
    }
}
