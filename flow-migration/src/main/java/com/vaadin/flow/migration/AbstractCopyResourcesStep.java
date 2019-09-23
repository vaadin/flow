/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.migration;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Step which copies resources from provided collection of directories to the
 * target folder. It keeps the files hierarchical structure.
 * <p>
 * Depending on provided parameter the content of copied file may be modified.
 *
 * @author Vaadin Ltd
 * @since 2.0
 */
public abstract class AbstractCopyResourcesStep {
    protected static final String BOWER_COMPONENTS = "bower_components";

    protected interface FileTreeHandler {
        /**
         * Handles a {@code source} file or folder.
         * <p>
         * Return value is used to indicate whether the file of folder has been
         * handled or not.
         *
         * @param source
         *            the source file or directory
         * @param target
         *            the target file or directory
         * @return {@code true} if the file should be handled, if the
         *         {@code source} is directory then it will be skipped in case
         *         {@code false} is returned
         * @throws IOException
         */
        boolean handle(Path source, Path target) throws IOException;
    }

    private static class CopyFileVisitor extends SimpleFileVisitor<Path> {

        private final Path sourceRoot;
        private final Path targetRoot;
        private final FileTreeHandler writer;

        private List<String> paths = new ArrayList<>();

        private CopyFileVisitor(Path sourceRoot, Path targetRoot,
                FileTreeHandler fileProducer) {
            this.sourceRoot = sourceRoot;
            this.targetRoot = targetRoot;
            this.writer = fileProducer;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                throws IOException {
            Path target = getTarget(file);
            getLogger().debug("Writing content to '{}'", target.toString());
            if (writer.handle(file, target)) {
                paths.add(getRelativePath(target, targetRoot));
            }
            return super.visitFile(file, attrs);
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir,
                BasicFileAttributes attrs) throws IOException {
            Path target = getTarget(dir);
            if (!writer.handle(dir, target)) {
                return FileVisitResult.SKIP_SUBTREE;
            }
            if (!target.toFile().exists()) {
                getLogger().debug("Creating a new {} directory",
                        target.toString());
                Files.createDirectory(getTarget(dir));
            } else {
                getLogger().debug(
                        "Directory/file {} already exists, skipping its creation",
                        target.toString());
            }
            return super.preVisitDirectory(dir, attrs);
        }

        List<String> getVisitedPaths() {
            return paths;
        }

        private Path getTarget(Path source) {
            Path relativePath = sourceRoot.relativize(source);
            String topLevel = relativePath.getName(0).toString();
            if ("frontend".equals(topLevel)) {
                if (relativePath.getNameCount() == 1) {
                    return targetRoot;
                }
                relativePath = relativePath.subpath(1,
                        relativePath.getNameCount());
            }
            return targetRoot.resolve(relativePath);
        }
    }

    private final File target;
    private final List<File> resources;
    private final FileTreeHandler handler;

    /**
     * Creates a new instance.
     *
     * @param target
     *            the target directory
     * @param sourceFolders
     *            an array of source folders
     * @param handler
     *            a strategy which handles the files in the source directories
     */
    public AbstractCopyResourcesStep(File target, File[] sourceFolders,
            FileTreeHandler handler) {
        this.target = target;
        resources = Arrays.asList(sourceFolders);
        this.handler = handler;
    }

    /**
     * Copies resources.
     * <p>
     * Collects imported bower components using provided {@code bowerComponents}
     * if it's not {@code null}.
     *
     * @return a map where the key is the resource folder and the value is the
     *         list of paths inside the {@code target} directory which has been
     *         copied
     * @throws IOException
     */
    public Map<String, List<String>> copyResources() throws IOException {
        if (target.exists() && !target.isDirectory()) {
            throw new IOException("Target path " + target.getPath()
                    + " exists and is not a directory");
        }
        if (!target.exists()) {
            FileUtils.forceMkdir(target);
        }
        getLogger().debug("Use {} as source folders to copy", resources);
        Map<String, List<String>> allResources = new HashMap<>();

        for (File resourceFolder : resources) {
            getLogger().debug("Copy resources from {} to {}", resourceFolder,
                    target.getPath());
            allResources.put(resourceFolder.getPath(),
                    doCopyResources(resourceFolder));
        }
        return allResources;
    }

    private List<String> doCopyResources(File source) throws IOException {
        CopyFileVisitor visitor = new CopyFileVisitor(source.toPath(),
                target.toPath(), handler);
        Files.walkFileTree(source.toPath(), visitor);
        return visitor.getVisitedPaths();
    }

    /**
     * Constructs a relative path between the {@code source} path and a
     * {@code against}.
     *
     * @param source
     *            the path which needs to be relativize
     * @param against
     *            the path to relativize against this path
     * @return
     */
    protected static String getRelativePath(Path source, Path against) {
        Path relativize = against.relativize(source);
        return StreamSupport.stream(relativize.spliterator(), false)
                .map(Path::toString).collect(Collectors.joining("/"));
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(AbstractCopyResourcesStep.class);
    }
}
