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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Copies migrated files preserving their hierarchy and modifies import path if
 * necessary.
 *
 * @author Vaadin Ltd
 * @since 2.0
 */
public class CopyMigratedResourcesStep extends AbstractCopyResourcesStep {

    /**
     * Performs the actual files copy using {@link FileTreeHandler} semantic.
     * <p>
     * The implementation does:
     * <ul>
     * <li>Ignores everything inside {@code node_modules} directory.
     * <li>Ignores temporary files like bower.json, package.json,
     * package-lock.json.
     * <li>Modifies the content of the file if necessary.
     * </ul>
     *
     * The modification of content is done because of modulizer: it adds imports
     * which sometimes (not always) starts with {@code "node_mouldes/"} prefix.
     * The same file may contain one import with this prefix and another import
     * without it. I consider this as a modulizer bug. There should not be any
     * {@code "node_mouldes/"} prefix and it's removed from the import path if
     * it's there.
     *
     */
    private static class CopyMigratedFiles implements FileTreeHandler {

        private static final String NODE_MODULES = "/node_modules/";
        private static final String IMPORT = "import";

        private final Path sourceRoot;
        private final Set<String> allowedDirectoryNames;

        private CopyMigratedFiles(Path sourceRoot,
                Set<String> allowedDirectoryNames) {
            this.sourceRoot = sourceRoot;
            this.allowedDirectoryNames = allowedDirectoryNames;
        }

        @Override
        public boolean handle(Path source, Path target) throws IOException {
            if (!acceptPath(source)) {
                return false;
            }
            if (source.toFile().isDirectory()) {
                return true;
            }
            List<String> lines = Files.readAllLines(source);
            StringBuilder content = new StringBuilder();
            for (String line : lines) {
                content.append(rewriteImport(line)).append("\n");
            }
            Files.write(target, Collections.singletonList(content.toString()));
            return true;
        }

        private boolean acceptPath(Path path) {
            if (sourceRoot.equals(path)) {
                return true;
            }
            Path relative = sourceRoot.relativize(path);
            String name = relative.getNameCount() <= 1 ? relative.toString()
                    : relative.getName(0).toString();
            // filter out temporary files which were created by us or by
            // bower/node
            if (BOWER_COMPONENTS.equals(name) || "node_modules".equals(name)
                    || "bower.json".equals(name) || "package.json".equals(name)
                    || "package-lock.json".equals(name)) {
                return false;
            }
            // filter out probable unexpected files which are created by
            // modulizer
            if (".gitignore".equals(name)) {
                return false;
            }
            if (!allowedDirectoryNames.contains(name)
                    && new File(sourceRoot.toFile(), name).isDirectory()) {
                return false;
            }
            return true;
        }

        private String rewriteImport(String line) {
            String importLine = line.trim();
            if (!importLine.startsWith(IMPORT) || !importLine.endsWith(";")) {
                return line;
            }
            char quote = importLine.charAt(line.length() - 2);
            if (quote != '\'' && quote != '"') {
                return line;
            }
            int index = importLine.substring(0, importLine.length() - 2)
                    .lastIndexOf(quote);
            String suffix = importLine.substring(index);
            if (suffix.startsWith(quote + NODE_MODULES)) {
                return importLine.substring(0, index) + quote + importLine
                        .substring(index + NODE_MODULES.length() + 1);
            }
            return line;
        }

    }

    /**
     * Create a new instance of copy migrated files step.
     *
     * @param target
     *            the target directory
     * @param source
     *            the source directory
     * @param allowedDirectoryNames
     *            the directory names which are allowed to be copied
     */
    public CopyMigratedResourcesStep(File target, File source,
            Set<String> allowedDirectoryNames) {
        super(target, new File[] { source },
                new CopyMigratedFiles(source.toPath(), allowedDirectoryNames));
    }

}
