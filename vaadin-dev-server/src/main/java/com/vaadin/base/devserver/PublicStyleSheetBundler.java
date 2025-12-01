/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.base.devserver;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.frontend.CssBundler;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.shared.ApplicationConstants;

/**
 * Utility that resolves and bundles public stylesheet resources referenced by
 * {@code @StyleSheet} URLs, in development time.
 * <p>
 * It locates the CSS entry file under common source roots that map to the
 * servlet context, and returns a single inlined CSS string with {@code @import}
 * rules resolved, using {@link CssBundler}.
 * <p>
 * The following source roots are searched under the current project folder:
 * <ul>
 * <li>src/main/resources/META-INF/resources</li>
 * <li>src/main/resources/resources</li>
 * <li>src/main/resources/static</li>
 * <li>src/main/resources/public</li>
 * <li>src/main/webapp</li>
 * </ul>
 * For internal use only. May be renamed or removed in a future release.
 */
public final class PublicStyleSheetBundler {

    private final List<File> sourceRoots;

    private PublicStyleSheetBundler(List<File> sourceRoots) {
        this.sourceRoots = sourceRoots;
    }

    /**
     * Creates a new bundler instance configured with an explicit list of source
     * roots. Only existing directories are included.
     *
     * @param roots
     *            list of root directories to search in
     * @return a configured {@link PublicStyleSheetBundler}
     */
    public static PublicStyleSheetBundler forResourceLocations(
            List<File> roots) {
        Objects.requireNonNull(roots, "roots cannot be null");
        return new PublicStyleSheetBundler(roots);
    }

    /**
     * Bundles the CSS content for the given stylesheet URL by resolving it
     * against known public source roots and inlining any {@code @import}
     * statements.
     *
     * @param publicResourcesRoot
     *            root folder for public resources
     * @param url
     *            the stylesheet URL as used in {@code @StyleSheet}; may start
     *            with {@code context://} or a leading '/'
     * @return the bundled CSS content if the entry file was found and bundled;
     *         otherwise, {@link Optional#empty()}
     */
    public Optional<String> bundle(File publicResourcesRoot, String url) {
        if (url == null || url.isBlank()) {
            return Optional.empty();
        }
        String normalized = normalizeUrl(url);
        for (File root : sourceRoots) {
            File entry = new File(root, normalized);
            if (entry.exists() && entry.isFile()) {
                try {
                    String bundled = CssBundler.inlineImportsForPublicResources(
                            publicResourcesRoot, entry);
                    return Optional.ofNullable(bundled);
                } catch (IOException e) {
                    getLogger().debug(
                            "Failed to inline CSS imports for {} in root {}",
                            entry, root, e);
                    return Optional.empty();
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Normalize an incoming stylesheet URL from @StyleSheet into a relative
     * path under the servlet context.
     */
    public static String normalizeUrl(String url) {
        url = url.trim();
        if (url.startsWith(ApplicationConstants.CONTEXT_PROTOCOL_PREFIX)) {
            url = url.substring(
                    ApplicationConstants.CONTEXT_PROTOCOL_PREFIX.length());
        }
        if (url.startsWith("/")) {
            url = url.substring(1);
        }
        // Normalize separators
        url = FrontendUtils.getUnixPath(new File(url).toPath());
        if (url.startsWith("./")) {
            url = url.substring(2);
        }
        return url;
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(PublicStyleSheetBundler.class);
    }
}
