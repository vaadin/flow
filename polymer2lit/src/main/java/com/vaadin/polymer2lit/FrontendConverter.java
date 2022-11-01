package com.vaadin.polymer2lit;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.server.frontend.FrontendToolsLocator;
import com.vaadin.flow.server.frontend.FrontendUtils;

public class FrontendConverter implements AutoCloseable {
    private static final String CONVERTER_EXECUTABLE_PATH = "/META-INF/frontend/generated/convert.js";

    private final FrontendToolsLocator frontendToolsLocator = new FrontendToolsLocator();

    private Path tempDirPath;

    private Path converterTempPath;

    public FrontendConverter() throws IOException {
        tempDirPath = Files.createTempDirectory("converter");
        converterTempPath = tempDirPath.resolve("converter.js");
        Files.copy(getClass().getResourceAsStream(CONVERTER_EXECUTABLE_PATH),
                converterTempPath);
    }

    @Override
    public void close() throws IOException {
        // cleanup by deleting all temp files
        Files.deleteIfExists(converterTempPath);
        Files.deleteIfExists(tempDirPath);
    }

    public int convertFile(Path filePath) throws IOException, InterruptedException {
        String nodeExecutablePath = findNodeExecutable();

        List<String> command = new ArrayList<>();
        command.add(nodeExecutablePath);
        command.add(converterTempPath.toFile().getAbsolutePath());
        command.add(filePath.toFile().getAbsolutePath());

        ProcessBuilder builder = FrontendUtils.createProcessBuilder(command);
        builder.inheritIO();
        Process process = builder.start();
        return process.waitFor();
    }

    private String findNodeExecutable() {
        String nodeExecutableName = FrontendUtils.isWindows() ? "node.exe"
                : "node";
        File nodeExecutableFile = frontendToolsLocator
                .tryLocateTool(nodeExecutableName).orElse(null);
        if (nodeExecutableFile == null) {
            throw new IllegalStateException(
                    "Node.js executable file was not found. Please, make sure Node.js is installed globally.");
        }

        return nodeExecutableFile.getAbsolutePath();
    }
}
