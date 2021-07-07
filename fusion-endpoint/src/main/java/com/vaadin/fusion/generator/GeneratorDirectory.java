package com.vaadin.fusion.generator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.frontend.FrontendUtils;

public class GeneratorDirectory {
    private final Logger logger = LoggerFactory
            .getLogger(GeneratorDirectory.class.getName());
    private final File outputDirectory;

    public GeneratorDirectory(File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public void clean() {
        clean(Collections.emptySet());
    }

    public void clean(Set<File> files) {
        if (!outputDirectory.exists()) {
            return;
        }

        removeStaleFiles(files);
        removeEmptyDirectories();
    }

    private boolean isEmpty(File directory) {
        try (Stream<Path> entries = Files.list(directory.toPath())) {
            return !entries.findFirst().isPresent();
        } catch (IOException e) {
            logDirectoryIOException(directory, e);
            return false;
        }
    }

    private void logDirectoryIOException(File directory, IOException e) {
        logger.info(String.format(
                "Failed to access folder '%s' while cleaning generated sources.",
                directory.getAbsolutePath()), e);
    }

    private void removeEmptyDirectories() {
        try (Stream<Path> paths = Files.walk(outputDirectory.toPath())) {
            paths.map(Path::toFile).filter(File::isDirectory)
                    .filter(this::isEmpty).forEach(directory -> {
                        logger.info("Removing empty folder '{}'.",
                                directory.getAbsolutePath());
                        removeFile(directory);
                    });
        } catch (IOException e) {
            logDirectoryIOException(outputDirectory, e);
        }
    }

    private void removeFile(File file) {
        try {
            FileUtils.forceDelete(file);
        } catch (IOException e) {
            logger.info(String.format(
                    "Failed to remove '%s' while cleaning the generated folder.",
                    file.getAbsolutePath()), e);
        }
    }

    private void removeStaleFiles(Set<File> files) {
        try (Stream<Path> paths = Files.walk(outputDirectory.toPath())) {
            paths.map(Path::toFile).filter(File::isFile).filter(file -> {
                if (files.contains(file)) {
                    return false;
                }

                final String fileName = file.getName();

                return !fileName.equals(
                        VaadinConnectClientGenerator.CONNECT_CLIENT_NAME)
                        && !fileName.equals(FrontendUtils.BOOTSTRAP_FILE_NAME)
                        && !fileName.equals(FrontendUtils.THEME_IMPORTS_NAME)
                        && !fileName
                                .equals(FrontendUtils.THEME_IMPORTS_D_TS_NAME)
                        && !fileName.endsWith(".generated.js");
            }).forEach(file -> {
                logger.info("Removing stale generated file '{}'.",
                        file.getAbsolutePath());
                removeFile(file);
            });
        } catch (IOException e) {
            logDirectoryIOException(outputDirectory, e);
        }
    }

    @Override
    public String toString() {
        return outputDirectory.toString();
    }

    public Path toPath() {
        return outputDirectory.toPath();
    }
}
