/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.plugin.common;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import com.vaadin.flow.migration.ClassPathIntrospector;
import com.vaadin.flow.plugin.TestUtils;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.scanner.ReflectionsClassFinder;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

public class ThemedURLTranslatorTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    /**
     * Classpath contains:
     *
     * - three navigation target components annotated with theme CustomTheme1
     *
     * So: only one theme CustomTheme1 is discovered and this theme rewrites
     * "src/" to "theme/myTheme".
     *
     * @throws IOException
     */
    @Test
    public void applyTheme_oneThemeIsDiscovered_urlsAreRewritten()
            throws IOException {
        ClassFinder finder = new ReflectionsClassFinder(
                TestUtils.getTestResource(TestUtils.SERVER_JAR),
                TestUtils.getTestResource(
                        "annotation-extractor-test/themes-one-theme.jar"),
                TestUtils.getTestResource(TestUtils.DATA_JAR));

        ClassPathIntrospector introspector = new ClassPathIntrospector(finder) {
        };

        Function<String, File> factory = url -> new File(
                temporaryFolder.getRoot(), url);

        String url1 = "src/component1.html"; // should be rewritten to
                                             // theme/myTheme/component1.html
        String url2 = "component2.html"; // should not be rewritten

        temporaryFolder.newFolder("src");
        temporaryFolder.newFile(url1);

        new File(temporaryFolder.newFolder("theme"), "myTheme").mkdir();
        temporaryFolder.newFile("theme/myTheme/component1.html");

        ThemedURLTranslator translator = new ThemedURLTranslator(factory,
                introspector);

        Set<String> translated = translator
                .applyTheme(new HashSet<>(Arrays.asList(url1, url2)));

        Assert.assertTrue(translated.contains("theme/myTheme/component1.html"));
        Assert.assertTrue(translated.contains("component2.html"));
    }

    /**
     * No themes in classpath.
     *
     */
    @Test
    public void applyTheme_noThemeIsDiscovered_urlsAreRewritten() {
        ClassFinder finder = new ReflectionsClassFinder(
                TestUtils.getTestResource(TestUtils.SERVER_JAR),
                TestUtils.getTestResource(TestUtils.DATA_JAR));

        ClassPathIntrospector introspector = new ClassPathIntrospector(finder) {
        };

        Function<String, File> factory = url -> new File(
                temporaryFolder.getRoot(), url);

        String url1 = "src/component1.html"; // should be rewritten to
                                             // theme/myTheme/component1.html
        String url2 = "component2.html"; // should not be rewritten

        ThemedURLTranslator translator = new ThemedURLTranslator(factory,
                introspector);

        Set<String> translated = translator
                .applyTheme(new HashSet<>(Arrays.asList(url1, url2)));

        Assert.assertTrue(translated.contains("src/component1.html"));
        Assert.assertTrue(translated.contains("component2.html"));
    }

    /**
     * Classpath contains:
     *
     * - two themes
     *
     * - two navigation target components annotated with different themes
     *
     */
    @Test
    public void applyTheme_twoThemeIsDiscovered_throws() {
        ClassFinder finder = new ReflectionsClassFinder(
                TestUtils.getTestResource(TestUtils.SERVER_JAR),
                TestUtils.getTestResource(
                        "annotation-extractor-test/themes-two-themes.jar"),
                TestUtils.getTestResource(TestUtils.DATA_JAR));

        ClassPathIntrospector introspector = new ClassPathIntrospector(finder) {
        };

        Function<String, File> factory = url -> new File(
                temporaryFolder.getRoot(), url);

        exception.expectMessage(CoreMatchers.allOf(
                CoreMatchers
                        .containsString("Multiple themes are not supported"),
                CoreMatchers.containsString(
                        "Theme 'class com.example.CustomTheme1' is discovered for classes which are navigation targets: com.example.ThemedComponent1"),
                CoreMatchers.containsString(
                        "Theme 'class com.example.CustomTheme2' is discovered for classes which are navigation targets: com.example.ThemedComponent2")));

        new ThemedURLTranslator(factory, introspector);
    }

    @Test
    public void applyTheme_when_annotation_on_a_routerLayout()
            throws Exception {

        ClassFinder finder = new ReflectionsClassFinder(
                TestUtils.getTestResource(TestUtils.SERVER_JAR),
                TestUtils.getTestResource(
                        "annotation-extractor-test/RouterLayoutTheme.jar"));

        ClassPathIntrospector introspector = new ClassPathIntrospector(finder) {
        };
        Function<String, File> factory = url -> new File(
                temporaryFolder.getRoot(), url);

        ThemedURLTranslator translator = new ThemedURLTranslator(factory,
                introspector);

        Field themeClass = translator.getClass().getDeclaredField("themeClass");
        themeClass.setAccessible(true);
        Assert.assertNotNull("No theme was found for ParentLayout!",
                themeClass.get(translator));

        String url1 = "src/component1.html"; // should be rewritten to
        // theme/myTheme/component1.html
        String url2 = "component2.html"; // should not be rewritten

        temporaryFolder.newFolder("src");
        temporaryFolder.newFile(url1);

        new File(temporaryFolder.newFolder("theme"), "myTheme").mkdir();
        temporaryFolder.newFile("theme/myTheme/component1.html");

        Set<String> translated = translator
                .applyTheme(new HashSet<>(Arrays.asList(url1, url2)));

        Assert.assertTrue(translated.contains("theme/myTheme/component1.html"));
        Assert.assertTrue(translated.contains("component2.html"));
    }
}
