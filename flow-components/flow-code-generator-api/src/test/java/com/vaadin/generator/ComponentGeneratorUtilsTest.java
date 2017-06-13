package com.vaadin.generator;

import org.junit.Assert;
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

        Assert.assertEquals(
                "Name was not converted to camelCase in the correct way",
                "testMethodForComponent", convertedName);
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

    @Test(expected = AssertionError.class)
    public void convertFilePathToPackageForNullDirectory() {
        ComponentGeneratorUtils.convertFilePathToPackage(null);
    }
}
