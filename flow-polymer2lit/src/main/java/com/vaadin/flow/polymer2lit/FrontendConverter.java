/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
        try (InputStream resourceAsStream = getClass()
                .getResourceAsStream(CONVERTER_EXECUTABLE_PATH)) {
            Files.copy(resourceAsStream, converterTempPath,
                    StandardCopyOption.REPLACE_EXISTING);
        }
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
