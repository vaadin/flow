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
package com.vaadin.flow.server.frontend;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Extracts import statements from the JS Polymer 3 source code.
 *
 * @author Vaadin Ltd
 * @since 2.0
 */
class ImportExtractor implements Serializable {

    private static final String IMPORT = "import";
    private static final String FROM = "from";

    private static final Pattern COMMENTS_PATTERN = Pattern
            .compile("(?:/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/)|(?://.*)");

    private final String content;

    ImportExtractor(String polymerCode) {
        content = polymerCode;
    }

    List<String> getImportedPaths() {
        List<String> imports = new ArrayList<>();

        String strippedContent = removeComments();
        int indexFrom;
        int index = 0;
        while (index >= 0) {
            indexFrom = index;
            index = strippedContent.indexOf(IMPORT, indexFrom);
            if (index < 0) {
                break;
            }
            String betweenImports = strippedContent.substring(indexFrom, index)
                    .trim();
            if (!betweenImports.isEmpty() && !betweenImports.equals(";")) {
                break;
            }

            indexFrom = index + IMPORT.length();
            index = strippedContent.indexOf(";", indexFrom);
            if (index < 0) {
                index = strippedContent.indexOf("\n", indexFrom);
            }
            if (index < 0) {
                index = strippedContent.length();
            }
            String importStatement = strippedContent.substring(indexFrom, index)
                    .trim();

            int fromIndex = importStatement.indexOf(FROM);
            String path = importStatement;
            if (fromIndex >= 0) {
                path = importStatement.substring(fromIndex + FROM.length(),
                        importStatement.length()).trim();
            }
            imports.add(strip(path));
        }
        return imports;
    }

    /**
     * Removes comments (block comments and line comments) from the JS code.
     * <p>
     * Note that this is not really a correct way to do this: this will remove
     * comments also if they are inside strings. But this is not important here
     * in this class since we care only about import statements where this is
     * fine.
     *
     * @return the code with removed comments
     */
    String removeComments() {
        return COMMENTS_PATTERN.matcher(content).replaceAll("");
    }

    /**
     * Strips quotes out of string: leading and trailing.
     *
     * @param jsString
     *            a JS string
     * @return the same string without quotes
     */
    private String strip(String jsString) {
        if (jsString.startsWith("'")) {
            return jsString.substring(1, jsString.length() - 1).replace("\\'",
                    "'");
        } else if (jsString.startsWith("\"") && jsString.length() > 2) {
            return jsString.substring(1, jsString.length() - 1).replace("\\\"",
                    "\"");
        }
        return jsString;
    }

}
