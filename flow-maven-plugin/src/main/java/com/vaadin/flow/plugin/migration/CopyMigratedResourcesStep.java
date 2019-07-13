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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * Copies migrated files preserving their hierarchy and modifies import path if
 * necessary.
 *
 * @author Vaadin Ltd
 *
 */
public class CopyMigratedResourcesStep extends AbstractCopyResourcesStep {

    private static class CopyMigratedFiles implements ContentModifier {

        private static final String NODE_MODULES = "/node_modules/";
        private static final String IMPORT = "import";

        @Override
        public boolean accept(Path source, Path target) throws IOException {
            String name = source.getFileName().toString();
            if (BOWER_COMPONENTS.equals(name) || "node_modules".equals(name)
                    || "bower.json".equals(name) || "package.json".equals(name)
                    || "package-lock.json".equals(name)) {
                return false;
            }
            if (Files.isDirectory(source)) {
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
     *            the target firectory
     * @param source
     *            the source directory
     */
    public CopyMigratedResourcesStep(File target, File source) {
        super(target, new String[] { source.getPath() },
                new CopyMigratedFiles());
    }

}
