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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * Test component generator utility methods
 */
public class ComponentGeneratorUtilsTest {

    @Test
    public void formatStringToValidJavaIdentifier_ReturnsCamelCase() {
        String name = "test_method-for_component";
        String convertedName = ComponentGeneratorUtils
                .formatStringToValidJavaIdentifier(name);

        assertEquals(
                "Name was not converted to camelCase in the correct way",
                "testMethodForComponent", convertedName);
    }

    @Test
    public void formatReservedWordToValidJavaIdentifier_returnsPreffixed() {
        String name = "for";
        String convertedName = ComponentGeneratorUtils
                .formatStringToValidJavaIdentifier(name);

        assertEquals("_for", convertedName);
    }

    @Test
    public void formatIgnoredReservedWordToValidJavaIdentifier_returnsCamelCase() {
        String name = "for";
        String convertedName = ComponentGeneratorUtils
                .formatStringToValidJavaIdentifier(name, true);

        assertEquals("for", convertedName);

        convertedName = ComponentGeneratorUtils
                .generateMethodNameForProperty("get", name);

        assertEquals("getFor", convertedName);
    }

    @Test
    public void formatStringWithNoConnectorPunctuation_returnsSameString() {
        String name = "testMethod$name";
        String convertedName = ComponentGeneratorUtils
                .formatStringToValidJavaIdentifier(name);

        assertEquals(
                "Name was not converted to camelCase in the correct way", name,
                convertedName);
    }

    @Test
    public void generateValidJavaClassName_ReturnsCamelCase() {
        String name = "test_method-for_component";
        String convertedName = ComponentGeneratorUtils
                .generateValidJavaClassName(name);

        assertEquals(
                "Name was not converted to camelCase in the correct way",
                "TestMethodForComponent", convertedName);
    }

    @Test
    public void gettersAndSettersAreGeneratedAsCamelCase() {
        String property = "my_property";

        String formattedProperty = ComponentGeneratorUtils
                .generateMethodNameForProperty("get", property);

        assertEquals("Getter was converted wrong.", "getMyProperty",
                formattedProperty);

        formattedProperty = ComponentGeneratorUtils
                .generateMethodNameForProperty("set", property);

        assertEquals("Setter was converted wrong.", "setMyProperty",
                formattedProperty);
    }

    @Test(expected = AssertionError.class)
    public void propertyFormatterFailsAssertionForNullPrefix() {
        String property = "my_property";

        String formatted = ComponentGeneratorUtils
                .generateMethodNameForProperty(null, property);

        assertEquals("", "MyProperty", formatted);
    }

    @Test
    public void formatStringToComment() {
        String simpleComment = "simple comment";

        String formatted = ComponentGeneratorUtils
                .formatStringToJavaComment(simpleComment);

        assertEquals("/*\n * simple comment\n */\n", formatted);

        String multiLineComment = "this\nis\na\nmulti\nline\ncomment";

        formatted = ComponentGeneratorUtils
                .formatStringToJavaComment(multiLineComment);
        assertEquals(
                "/*\n * this\n * is\n * a\n * multi\n * line\n * comment\n */\n",
                formatted);
    }

    @Test(expected = AssertionError.class)
    public void formatStringToCommentAssertionForNullPrefix() {
        ComponentGeneratorUtils.formatStringToJavaComment(null);
    }

    @Test
    public void convertFilePathToPackage() {
        assertEquals("some.directory.structure",
                ComponentGeneratorUtils.convertFilePathToPackage(
                        "/some/directory/structure/my-file.html"));

        assertEquals("some.directory.structure",
                ComponentGeneratorUtils.convertFilePathToPackage(
                        "\\some\\directory\\structure\\my-file.html"));

        assertEquals("some.directory.structure",
                ComponentGeneratorUtils.convertFilePathToPackage(
                        "some directory structure/my-file.html"));

        assertEquals("some.directory.structure",
                ComponentGeneratorUtils.convertFilePathToPackage(
                        "/SOME_DIRECTORY-STRUCTURE/MY-FILE.html"));

        assertEquals("c.my.documents.something",
                ComponentGeneratorUtils.convertFilePathToPackage(
                        "C:/My Documents/Something/My-File.html"));

        assertEquals("_42", ComponentGeneratorUtils
                .convertFilePathToPackage("42/my-file.html"));
    }

    @Test
    public void convertFilePathToEmptyPackage() {
        assertEquals("", ComponentGeneratorUtils
                .convertFilePathToPackage("my-file.html"));

        assertEquals("", ComponentGeneratorUtils
                .convertFilePathToPackage("/my-file.html"));

        assertEquals("",
                ComponentGeneratorUtils.convertFilePathToPackage("/ / / /"));
    }

    @Test
    public void convertCamelCaseToHyphens() {
        assertNull(
                ComponentGeneratorUtils.convertCamelCaseToHyphens(null));

        assertEquals("some-thing",
                ComponentGeneratorUtils.convertCamelCaseToHyphens("someThing"));

        assertEquals("some-thing",
                ComponentGeneratorUtils.convertCamelCaseToHyphens("SomeThing"));

        assertEquals("s-o-m-e-t-h-i-n-g",
                ComponentGeneratorUtils.convertCamelCaseToHyphens("SOMETHING"));

        assertEquals("",
                ComponentGeneratorUtils.convertCamelCaseToHyphens(""));

        assertEquals("i",
                ComponentGeneratorUtils.convertCamelCaseToHyphens("I"));

        assertEquals("am-i",
                ComponentGeneratorUtils.convertCamelCaseToHyphens("AmI"));
    }

    @Test(expected = AssertionError.class)
    public void convertFilePathToPackageForNullDirectory() {
        ComponentGeneratorUtils.convertFilePathToPackage(null);
    }
}
