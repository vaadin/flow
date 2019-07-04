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
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Vaadin Ltd
 *
 */
public class CopyResourcesStep {
    private static final Logger LOGGER = LoggerFactory
            .getLogger(CopyResourcesStep.class);

    private static interface ContentModifier {
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
                paths.add(target.toString());
            }
            return super.visitFile(file, attrs);
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir,
                BasicFileAttributes attrs) throws IOException {
            Path target = getTarget(dir);
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

    private static class HtmlImportRewriter implements ContentModifier {

        @Override
        public boolean accept(Path source, Path target) throws IOException {
            if (target.toFile().exists()) {
                Files.delete(target);
            }
            if (source.getFileName().endsWith(".html")) {
                Files.write(target, Collections
                        .singletonList(adjustImports(source.toFile())));
                return true;
            }
            return false;
        }
    }

    private final File target;
    private final List<String> resources;

    public CopyResourcesStep(File target, String[] resourceFolders) {
        this.target = target;
        resources = Arrays.asList(resourceFolders);
    }

    public List<String> copyResources() throws IOException {
        if (target.exists() && !target.isDirectory()) {
            throw new IOException("Target path " + target.getPath()
                    + " exists and is not a directory");
        }
        if (!target.exists()) {
            target.mkdir();
        }
        LOGGER.debug("Use {} as source folders to copy", resources);
        List<String> allResources = new ArrayList<>();
        for (String resourceFolder : resources) {
            LOGGER.debug("Copy resources from {} to {}", resourceFolder,
                    target.getPath());
            allResources.addAll(doCopyResources(new File(resourceFolder),
                    target, new HtmlImportRewriter()));
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

    private static String adjustImports(File file) throws IOException {
        Document doc = Jsoup.parse(file, StandardCharsets.UTF_8.name());
        Element head = doc.head();
        Element body = doc.body();
        StringBuilder result = new StringBuilder();
        for (Element child : head.children()) {
            String elementHtml = child.outerHtml();
            if ("link".equalsIgnoreCase(child.tagName()) && child.hasAttr("rel")
                    && "import".equals(child.attr("rel"))
                    && child.hasAttr("href")) {
                String href = child.attr("href");
                int index = href.indexOf("bower_components");
                if (index != -1) {
                    href = href.substring(index);
                    child.attr("href", href);
                    elementHtml = child.outerHtml();
                }
            }
            result.append(elementHtml).append('\n');
        }
        result.append(body.html());
        return result.toString();
    }
}
