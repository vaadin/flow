/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.migration;

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
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeVisitor;

/**
 * Step which copies resources from provided collection of directories to the
 * target folder. It keeps the files hierarchical structure.
 * <p>
 * The content of copied file is modified to correct URI in the imports and
 * remove the comments.
 *
 * @author Vaadin Ltd
 * @since 2.0
 */
public class CopyResourcesStep extends AbstractCopyResourcesStep {

    private static class CommentRemover implements NodeVisitor {
        @Override
        public void tail(Node node, int depth) {
            if (node instanceof Comment) {
                node.remove();
            }
        }

        @Override
        public void head(Node node, int depth) {
            // no op
        }
    }

    private static class HtmlImportRewriter implements FileTreeHandler {

        private final Collection<String> bowerComponents;
        private final File targetDir;

        private HtmlImportRewriter(File target,
                Collection<String> bowerComponents) {
            this.bowerComponents = bowerComponents;
            targetDir = target;
        }

        @Override
        public boolean handle(Path source, Path target) throws IOException {
            if (source.toFile().isDirectory()) {
                return true;
            }
            if (source.getFileName().toString().toLowerCase(Locale.ENGLISH)
                    .endsWith(".html")) {
                Files.write(target, Collections
                        .singletonList(adjustContent(source.toFile(), target)));
                return true;
            }
            if (source.getFileName().toString().toLowerCase(Locale.ENGLISH)
                    .endsWith(".css")) {
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                return true;
            }
            return false;
        }

        /**
         * Rewrites import URIs in {@code file} adjusting them for
         * {@code target} path and removes HTML comments.
         *
         * @param file
         *            the source file with the content to adjust
         * @param target
         *            the target path where the content is supposed to be
         *            written
         *
         * @return the adjusted content
         */
        private String adjustContent(File file, Path target)
                throws IOException {
            Document doc = Jsoup.parse(file, StandardCharsets.UTF_8.name());
            Element head = doc.head();
            Element body = doc.body();
            StringBuilder result = new StringBuilder();
            for (Element child : head.children()) {
                String elementHtml = child.outerHtml();
                if ("link".equalsIgnoreCase(child.tagName())
                        && child.hasAttr("rel")
                        && "import".equalsIgnoreCase(child.attr("rel"))
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
            body.traverse(new CommentRemover());
            result.append(body.outerHtml());
            return result.toString();
        }

        private String pathToTarget(Path target) {
            String path = getRelativePath(targetDir.toPath(), target);
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
    public CopyResourcesStep(File target, File[] resourceFolders) {
        this(target, resourceFolders, new HashSet<>());
    }

    private CopyResourcesStep(File target, File[] resourceFolders,
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
