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
package com.vaadin.flow.polymer2lit;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;

import com.vaadin.flow.server.frontend.FrontendTools;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.server.frontend.FrontendUtils.CommandExecutionException;

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

    public FrontendConverter(FrontendTools frontendTools) throws IOException {
        this.frontendTools = frontendTools;
        this.tempDirPath = Files.createTempDirectory("converter");
        this.converterTempPath = tempDirPath.resolve("converter.js");
        Files.copy(getClass().getResourceAsStream(CONVERTER_EXECUTABLE_PATH),
                converterTempPath, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public void close() throws IOException {
        // cleanup by deleting all temp files
        Files.deleteIfExists(converterTempPath);
        Files.deleteIfExists(tempDirPath);
    }

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
