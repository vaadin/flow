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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * Step which copies resources from provided collection of directories to the
 * target folder. It keeps the files hierarchical structure.
 * <p>
 * The content of copied file is modified to correct URI in the import.
 *
 * @author Vaadin Ltd
 *
 */
public class CopyResourcesStep extends AbstractCopyResourcesStep {

    private static class HtmlImportRewriter implements ContentModifier {

        private final Collection<String> bowerComponents;
        private final File targetDir;

        private HtmlImportRewriter(File target,
                Collection<String> bowerComponents) {
            this.bowerComponents = bowerComponents;
            targetDir = target;
        }

        @Override
        public boolean accept(Path source, Path target) throws IOException {
            if (Files.isDirectory(source)) {
                return true;
            }
            if (source.getFileName().toString().toLowerCase(Locale.ENGLISH)
                    .endsWith(".html")) {
                Files.write(target, Collections
                        .singletonList(adjustImports(source.toFile(), target)));
                return true;
            }
            if (source.getFileName().toString().toLowerCase(Locale.ENGLISH)
                    .endsWith(".css")) {
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                return true;
            }
            return false;
        }

        private String adjustImports(File file, Path target)
                throws IOException {
            Document doc = Jsoup.parse(file, StandardCharsets.UTF_8.name());
            Element head = doc.head();
            Element body = doc.body();
            StringBuilder result = new StringBuilder();
            for (Element child : head.children()) {
                String elementHtml = child.outerHtml();
                if ("link".equalsIgnoreCase(child.tagName())
                        && child.hasAttr("rel")
                        && "import".equals(child.attr("rel"))
                        && child.hasAttr("href")) {
                    String href = child.attr("href");
                    int index = href.indexOf(BOWER_COMPONENTS);
                    if (index != -1) {
                        href = href.substring(index);
                        addBowerComponent(href);
                        child.attr("href", pathToTarget(target) + href);
                        elementHtml = child.outerHtml();
                    }
                }
                result.append(elementHtml).append('\n');
            }
            result.append(body.html());
            return result.toString();
        }

        private String pathToTarget(Path target) {
            String path = target.relativize(targetDir.toPath()).toString();
            if (path.length() >= 2) {
                path = path.substring(2);
            }
            if (path.isEmpty()) {
                return path;
            }
            if (path.charAt(0) == '/') {
                path = path.substring(1);
            }
            if (!path.endsWith("/")) {
                return path + "/";
            }
            return path;
        }

        private void addBowerComponent(String uri) {
            if (bowerComponents == null) {
                return;
            }
            assert uri.startsWith(BOWER_COMPONENTS);
            String path = uri.substring(BOWER_COMPONENTS.length());
            if (path.charAt(0) != '/') {
                return;
            }
            path = path.substring(1);
            int index = path.indexOf('/');
            bowerComponents.add(path.substring(0, index));
        }
    }

    private final Set<String> bowerComponents;

    /**
     * Creates a new instance.
     *
     * @param target
     *            the target directory
     * @param resourceFolders
     *            an array of source folders
     */
    public CopyResourcesStep(File target, String[] resourceFolders) {
        this(target, resourceFolders, new HashSet<>());
    }

    private CopyResourcesStep(File target, String[] resourceFolders,
            Set<String> bowerComponents) {
        super(target, resourceFolders,
                new HtmlImportRewriter(target, bowerComponents));
        this.bowerComponents = bowerComponents;
    }

    @Override
    public Map<String, List<String>> copyResources() throws IOException {
        bowerComponents.clear();
        return super.copyResources();
    }

    /**
     * Gets imported bower components found in copied resources.
     * <p>
     * The value is available only after {@link #copyResources()} method
     * execution.
     *
     * @return a set of imported bower components
     */
    public Set<String> getBowerComponents() {
        return Collections.unmodifiableSet(bowerComponents);
    }

}
