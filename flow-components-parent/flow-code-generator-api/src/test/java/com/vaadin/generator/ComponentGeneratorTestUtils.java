/*
 * Copyright 2000-2019 Vaadin Ltd.
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Assert;

/**
 * Utility class for {@link ComponentGenerator} unit tests.
 */
public class ComponentGeneratorTestUtils {

    private ComponentGeneratorTestUtils() {
    }

    /**
     * Assert that a given generated class implements the given interface.
     *
     * @param generatedClass
     *            the full generated class string
     * @param className
     *            the class name of the generated class
     * @param interfaceToBeImplemented
     *            the interface that the generated class should implement
     */
    public static void assertClassImplementsInterface(String generatedClass,
            String className, Class<?> interfaceToBeImplemented) {
        Pattern pattern = Pattern.compile("\\s*public\\s+class\\s+" + className
                + ".*\\s+extends\\s+.+\\s+implements\\s+([^\\{]+)\\{");
        Matcher matcher = pattern.matcher(generatedClass);
        Assert.assertTrue("Wrong class declaration", matcher.find());

        String interfaces = matcher.group(1);
        Assert.assertTrue(interfaceToBeImplemented.getSimpleName()
                + " interface not found in the class definition: " + interfaces,
                interfaces.contains(interfaceToBeImplemented.getSimpleName()));
    }

    /**
     * Removes indentations from a given source code string.
     *
     * @param sourceCode
     *            the source string to remove indentation from
     * @return a new source string with indentations removed
     */
    public static String removeIndentation(String sourceCode) {
        return sourceCode.replaceAll("\\s\\s+", " ");
    }
}
