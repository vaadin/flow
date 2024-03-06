/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.frontend;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.internal.StringUtil;

/**
 * Extracts import statements from the JS Polymer 3 source code.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 2.0
 */
class ImportExtractor implements Serializable {

    private static final String IMPORT = "import";
    private static final String FROM = "from";

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
     *
     * @return the code with removed comments
     */
    String removeComments() {
        return StringUtil.removeComments(content, true);
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
