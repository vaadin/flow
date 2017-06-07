/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.generator;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

/**
 * Class with utility methods for the code generation process.
 */
public final class ComponentGeneratorUtils {

    private ComponentGeneratorUtils() {
    }

    /**
     * Creates a valid Java class name based on the ES6 class name.
     * 
     * @param webcomponentClassName
     *            The ES6 class name.
     * @return A valid Java class name.
     */
    public static String generateValidJavaClassName(
            String webcomponentClassName) {
        return StringUtils.capitalize(
                formatStringToValidJavaIdentifier(webcomponentClassName));
    }

    /**
     * Formats a given name (which can be a property name, function name or
     * event name) to a valid Java identifier.
     * 
     * @param name
     *            The name of the property that would be exposed to Java code.
     * @return A valid Java identifier based on the input name.
     * 
     * @see Character#isJavaIdentifierStart(char)
     * @see Character#isJavaIdentifierPart(char)
     */
    public static String formatStringToValidJavaIdentifier(String name) {
        String trimmed = name.trim();
        StringBuilder sb = new StringBuilder();
        if (!Character.isJavaIdentifierStart(trimmed.charAt(0))) {
            sb.append('_');
        }
        for (char c : trimmed.toCharArray()) {
            if (!Character.isJavaIdentifierPart(c)) {
                sb.append('_');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Generates the directory structure based on a package name.
     * 
     * @param basePath
     *            The base path of the directory to be generated.
     * @param packageName
     *            The name of the package to be converted to directories.
     * @param createDirectories
     *            <code>true</code> to actually create the directories on the
     *            disk, <code>false</code> to just generate the File objects
     *            without creating them.
     * @return a new File that represents the final directory of the package,
     *         with basePath as root.
     * 
     * @throws IOException
     *             When createDirectories is <code>true</code> and the target
     *             directory cannot be created.
     */
    public static File convertPackageToDirectory(File basePath,
            String packageName, boolean createDirectories) throws IOException {
        File directory = new File(basePath,
                packageName.replace('.', File.separatorChar));
        if (createDirectories && !directory.isDirectory()
                && !directory.mkdirs()) {
            throw new IOException(
                    "Directory \"" + directory + "\" could not be created.");
        }
        return directory;
    }

}
