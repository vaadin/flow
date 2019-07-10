package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.Command;

import static com.vaadin.flow.server.Constants.RESOURCES_FRONTEND_DEFAULT;

/**
 * Copies JavaScript and CSS files from JAR files into a given folder.
 */
public class TaskCopyFrontendFiles implements Command {
    private static final String CLASS_PATH_PROPERTY = "java.class.path";
    private static final String[] WILDCARD_INCLUSIONS = new String[] {
            "**/*.js", "**/*.css" };

    private File targetDirectory;

    TaskCopyFrontendFiles(File targetDirectory) {
        Objects.requireNonNull(targetDirectory,
                "Parameter 'targetDirectory' cannot be null!");

        this.targetDirectory = targetDirectory;
    }

    @Override
    public void execute() {
        List<File> collect = Stream
                .of(System.getProperty(CLASS_PATH_PROPERTY).split(";"))
                .filter(path -> path.endsWith(".jar")).map(File::new)
                .filter(File::exists).collect(Collectors.toList());

        log().info("Found {} jars to copy files from.", collect.size());

        try {
            FileUtils.forceMkdir(Objects.requireNonNull(targetDirectory));
        } catch (IOException e) {
            throw new UncheckedIOException(String.format(
                    "Failed to create directory '%s'", targetDirectory), e);
        }

        JarContentsManager jarContentsManager = new JarContentsManager();
        for (File jarFile : collect) {
            jarContentsManager.copyIncludedFilesFromJarTrimmingBasePath(jarFile,
                    RESOURCES_FRONTEND_DEFAULT, targetDirectory,
                    WILDCARD_INCLUSIONS);
        }
    }

    private static Logger log() {
        return LoggerFactory.getLogger(TaskCopyFrontendFiles.class);
    }
}
