/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.generator.metadata.ComponentBasicType;

/**
 * Test component generator utility methods
 */
public class ComponentGeneratorUtilsTest {

    @Test
    public void formatStringToValidJavaIdentifier_ReturnsCamelCase() {
        String name = "test_method-for_component";
        String convertedName = ComponentGeneratorUtils
                .formatStringToValidJavaIdentifier(name);

        Assert.assertEquals(
                "Name was not converted to camelCase in the correct way",
                "testMethodForComponent", convertedName);
    }

    @Test
    public void formatReservedWordToValidJavaIdentifier_returnsPreffixed() {
        String name = "for";
        String convertedName = ComponentGeneratorUtils
                .formatStringToValidJavaIdentifier(name);

        Assert.assertEquals("_for", convertedName);
    }

    @Test
    public void formatIgnoredReservedWordToValidJavaIdentifier_returnsCamelCase() {
        String name = "for";
        String convertedName = ComponentGeneratorUtils
                .formatStringToValidJavaIdentifier(name, true);

        Assert.assertEquals("for", convertedName);

        convertedName = ComponentGeneratorUtils
                .generateMethodNameForProperty("get", name);

        Assert.assertEquals("getFor", convertedName);
    }

    @Test
    public void formatStringWithNoConnectorPunctuation_returnsSameString() {
        String name = "testMethod$name";
        String convertedName = ComponentGeneratorUtils
                .formatStringToValidJavaIdentifier(name);

        Assert.assertEquals(
                "Name was not converted to camelCase in the correct way", name,
                convertedName);
    }

    @Test
    public void generateValidJavaClassName_ReturnsCamelCase() {
        String name = "test_method-for_component";
        String convertedName = ComponentGeneratorUtils
                .generateValidJavaClassName(name);

        Assert.assertEquals(
                "Name was not converted to camelCase in the correct way",
                "TestMethodForComponent", convertedName);
    }

    @Test
    public void gettersAndSettersAreGeneratedAsCamelCase() {
        String property = "my_property";

        String formattedProperty = ComponentGeneratorUtils
                .generateMethodNameForProperty("get", property);

        Assert.assertEquals("Getter was converted wrong.", "getMyProperty",
                formattedProperty);

        formattedProperty = ComponentGeneratorUtils
                .generateMethodNameForProperty("set", property);

        Assert.assertEquals("Setter was converted wrong.", "setMyProperty",
                formattedProperty);
    }

    @Test(expected = AssertionError.class)
    public void propertyFormatterFailsAssertionForNullPrefix() {
        String property = "my_property";

        String formatted = ComponentGeneratorUtils
                .generateMethodNameForProperty(null, property);

        Assert.assertEquals("", "MyProperty", formatted);
    }

    @Test
    public void formatStringToComment() {
        String simpleComment = "simple comment";

        String formatted = ComponentGeneratorUtils
                .formatStringToJavaComment(simpleComment);

        Assert.assertEquals("/*\n * simple comment\n */\n", formatted);

        String multiLineComment = "this\nis\na\nmulti\nline\ncomment";

        formatted = ComponentGeneratorUtils
                .formatStringToJavaComment(multiLineComment);
        Assert.assertEquals(
                "/*\n * this\n * is\n * a\n * multi\n * line\n * comment\n */\n",
                formatted);
    }

    @Test(expected = AssertionError.class)
    public void formatStringToCommentAssertionForNullPrefix() {
        ComponentGeneratorUtils.formatStringToJavaComment(null);
    }

    @Test
    public void convertFilePathToPackage() {
        Assert.assertEquals("some.directory.structure",
                ComponentGeneratorUtils.convertFilePathToPackage(
                        "/some/directory/structure/my-file.html"));

        Assert.assertEquals("some.directory.structure",
                ComponentGeneratorUtils.convertFilePathToPackage(
                        "\\some\\directory\\structure\\my-file.html"));

        Assert.assertEquals("some.directory.structure",
                ComponentGeneratorUtils.convertFilePathToPackage(
                        "some directory structure/my-file.html"));

        Assert.assertEquals("some.directory.structure",
                ComponentGeneratorUtils.convertFilePathToPackage(
                        "/SOME_DIRECTORY-STRUCTURE/MY-FILE.html"));

        Assert.assertEquals("c.my.documents.something",
                ComponentGeneratorUtils.convertFilePathToPackage(
                        "C:/My Documents/Something/My-File.html"));

        Assert.assertEquals("_42", ComponentGeneratorUtils
                .convertFilePathToPackage("42/my-file.html"));
    }

    @Test
    public void convertFilePathToEmptyPackage() {
        Assert.assertEquals("", ComponentGeneratorUtils
                .convertFilePathToPackage("my-file.html"));

        Assert.assertEquals("", ComponentGeneratorUtils
                .convertFilePathToPackage("/my-file.html"));

        Assert.assertEquals("",
                ComponentGeneratorUtils.convertFilePathToPackage("/ / / /"));
    }

    @Test
    public void convertCamelCaseToHyphens() {
        Assert.assertNull(
                ComponentGeneratorUtils.convertCamelCaseToHyphens(null));

        Assert.assertEquals("some-thing",
                ComponentGeneratorUtils.convertCamelCaseToHyphens("someThing"));

        Assert.assertEquals("some-thing",
                ComponentGeneratorUtils.convertCamelCaseToHyphens("SomeThing"));

        Assert.assertEquals("s-o-m-e-t-h-i-n-g",
                ComponentGeneratorUtils.convertCamelCaseToHyphens("SOMETHING"));

        Assert.assertEquals("",
                ComponentGeneratorUtils.convertCamelCaseToHyphens(""));

        Assert.assertEquals("i",
                ComponentGeneratorUtils.convertCamelCaseToHyphens("I"));

        Assert.assertEquals("am-i",
                ComponentGeneratorUtils.convertCamelCaseToHyphens("AmI"));
    }

    @Test(expected = AssertionError.class)
    public void convertFilePathToPackageForNullDirectory() {
        ComponentGeneratorUtils.convertFilePathToPackage(null);
    }

    @Test
    public void generateGetterForValue_nullIsChecked() {
        Assert.assertEquals(
                "String value = getElement().getProperty(\"value\");return value == null ? getEmptyValue() : value;",
                ComponentGeneratorUtils.generateElementApiValueGetterForType(
                        ComponentBasicType.STRING, "value"));

        Assert.assertEquals("return getElement().getProperty(\"value\", 0.0);",
                ComponentGeneratorUtils.generateElementApiValueGetterForType(
                        ComponentBasicType.NUMBER, "value"));

        Assert.assertEquals(
                "return getElement().getProperty(\"boolean\", false);",
                ComponentGeneratorUtils.generateElementApiValueGetterForType(
                        ComponentBasicType.BOOLEAN, "boolean"));

        Assert.assertEquals(
                "Object array = getElement().getPropertyRaw(\"array\");return (JsonArray) (array == null ? getEmptyValue() : array);",
                ComponentGeneratorUtils.generateElementApiValueGetterForType(
                        ComponentBasicType.ARRAY, "array"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void generateGetterForEmptyValueProperty_exceptionIsRaised() {
        ComponentGeneratorUtils.generateElementApiValueGetterForType(
                ComponentBasicType.STRING, "");
    }

    @Test(expected = NullPointerException.class)
    public void generateGetterForNullValueProperty_exceptionIsRaised() {
        ComponentGeneratorUtils.generateElementApiValueGetterForType(
                ComponentBasicType.STRING, null);
    }
}
