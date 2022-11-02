package com.vaadin.polymer2lit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.server.frontend.FrontendTools;
import com.vaadin.flow.server.frontend.FrontendUtils;

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
                converterTempPath);
    }

    @Override
    public void close() throws IOException {
        // cleanup by deleting all temp files
        Files.deleteIfExists(converterTempPath);
        Files.deleteIfExists(tempDirPath);
    }

    public int convertFile(Path filePath)
            throws IOException, InterruptedException {
        return convertFile(filePath, false, false);
    }

    public int convertFile(Path filePath, boolean useLit1,
            boolean disableOptionalChaining)
            throws IOException, InterruptedException {
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

        ProcessBuilder builder = FrontendUtils.createProcessBuilder(command);
        builder.inheritIO();
        Process process = builder.start();
        return process.waitFor();
    }
}
