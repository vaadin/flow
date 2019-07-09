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
package com.vaadin.flow.plugin.migration;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Step which copies resources from provided collection of directories to the
 * target folder. It keeps the files hierarchical structure.
 * <p>
 * Depending on provided parameter the content of copied file may be modified.
 *
 * @author Vaadin Ltd
 *
 */
public abstract class AbstractCopyResourcesStep {
    private static final Logger LOGGER = LoggerFactory
            .getLogger(AbstractCopyResourcesStep.class);

    protected static final String BOWER_COMPONENTS = "bower_components";

    protected static interface ContentModifier {
        /**
         * Accepts a {@code source} file or folder and copy its modified content
         * to the {@code target}.
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
        boolean accept(Path source, Path target) throws IOException;
    }

    private static class CopyFileVisitor extends SimpleFileVisitor<Path> {

        private final Path sourceRoot;
        private final Path targetRoot;
        private final ContentModifier writer;

        private List<String> paths = new ArrayList<>();

        private CopyFileVisitor(Path sourceRoot, Path targetRoot,
                ContentModifier fileProducer) {
            this.sourceRoot = sourceRoot;
            this.targetRoot = targetRoot;
            this.writer = fileProducer;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                throws IOException {
            Path target = getTarget(file);
            LOGGER.debug("Writing content to '{}'", target.toString());
            if (writer.accept(file, target)) {
                paths.add(targetRoot.relativize(target).toString());
            }
            return super.visitFile(file, attrs);
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir,
                BasicFileAttributes attrs) throws IOException {
            Path target = getTarget(dir);
            if (!writer.accept(dir, target)) {
                return FileVisitResult.SKIP_SUBTREE;
            }
            if (!target.toFile().exists()) {
                LOGGER.debug("Creating a new {} directory", target.toString());
                Files.createDirectory(getTarget(dir));
            } else {
                LOGGER.debug(
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
            return targetRoot.resolve(relativePath);
        }
    }

    private final File target;
    private final List<String> resources;
    private final ContentModifier contentModifier;

    /**
     * Creates a new instance.
     *
     * @param target
     *            the target directory
     * @param sourceFolders
     *            an array of source folders
     * @param contentModifier
     *            a strategy which rewrites the content of copied file
     */
    public AbstractCopyResourcesStep(File target, String[] sourceFolders,
            ContentModifier contentModifier) {
        this.target = target;
        resources = Arrays.asList(sourceFolders);
        this.contentModifier = contentModifier;
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
            target.mkdir();
        }
        LOGGER.debug("Use {} as source folders to copy", resources);
        Map<String, List<String>> allResources = new HashMap<>();

        for (String resourceFolder : resources) {
            LOGGER.debug("Copy resources from {} to {}", resourceFolder,
                    target.getPath());
            allResources.put(resourceFolder, doCopyResources(
                    new File(resourceFolder), target, contentModifier));
        }
        return allResources;
    }

    private List<String> doCopyResources(File source, File target,
            ContentModifier producer) throws IOException {
        CopyFileVisitor visitor = new CopyFileVisitor(source.toPath(),
                target.toPath(), producer);
        Files.walkFileTree(source.toPath(), visitor);
        return visitor.getVisitedPaths();
    }

}
