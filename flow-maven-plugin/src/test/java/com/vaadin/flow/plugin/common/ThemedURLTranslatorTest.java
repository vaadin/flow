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
package com.vaadin.flow.plugin.common;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import com.vaadin.flow.plugin.TestUtils;

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
     * - one component annotated with CustomTheme2 which is not navigation
     * target
     *
     * So: only one theme CustomTheme1 is discovered and this theme rewrites
     * "src/" to "theme/myTheme".
     *
     * @throws IOException
     */
    @Test
    public void applyTheme_oneThemeIsDiscovered_urlsAreRewritten()
            throws IOException {
        ClassPathIntrospector introspector = new ClassPathIntrospector(
                TestUtils.getTestResource(
                        "annotation-extractor-test/flow-server-1.0-SNAPSHOT.jar"),
                TestUtils.getTestResource(
                        "annotation-extractor-test/themes-one-active-theme.jar"),
                TestUtils.getTestResource(
                        "annotation-extractor-test/flow-data-1.0-SNAPSHOT.jar")) {
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
     * @throws IOException
     */
    @Test
    public void applyTheme_noThemeIsDiscovered_urlsAreRewritten()
            throws IOException {
        ClassPathIntrospector introspector = new ClassPathIntrospector(
                TestUtils.getTestResource(
                        "annotation-extractor-test/flow-server-1.0-SNAPSHOT.jar"),
                TestUtils.getTestResource(
                        "annotation-extractor-test/flow-data-1.0-SNAPSHOT.jar")) {
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
    public void applyTheme_twoThemeIsDiscovered_throws() throws IOException {
        ClassPathIntrospector introspector = new ClassPathIntrospector(
                TestUtils.getTestResource(
                        "annotation-extractor-test/flow-server-1.0-SNAPSHOT.jar"),
                TestUtils.getTestResource(
                        "annotation-extractor-test/themes-two-themes.jar"),
                TestUtils.getTestResource(
                        "annotation-extractor-test/flow-data-1.0-SNAPSHOT.jar")) {
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
    public void applyTheme_when_annotation_on_a_routerLayout() throws Exception {
        ClassPathIntrospector introspector = new ClassPathIntrospector(
                TestUtils.getTestResource(
                        "annotation-extractor-test/flow-server-1.0-SNAPSHOT.jar"),
                TestUtils.getTestResource(
                        "annotation-extractor-test/RouterLayoutTheme.jar")) {
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
