/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.tests.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Objects;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.communication.IndexHtmlRequestHandlerTest;
import com.vaadin.flow.server.frontend.FrontendUtils;

import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_FRONTEND_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.INDEX_HTML;
import static com.vaadin.flow.server.frontend.FrontendUtils.THEME_IMPORTS_NAME;
import static com.vaadin.flow.server.frontend.FrontendUtils.WEB_COMPONENT_HTML;
import static java.nio.charset.StandardCharsets.UTF_8;

public class TestUtil {
    public static void assertArrays(Object[] actualObjects,
            Object[] expectedObjects) {
        Assert.assertEquals(
                "Actual contains a different number of values than was expected",
                expectedObjects.length, actualObjects.length);

        for (int i = 0; i < actualObjects.length; i++) {
            Object actual = actualObjects[i];
            Object expected = expectedObjects[i];

            Assert.assertEquals("Item[" + i + "] does not match", expected,
                    actual);
        }

    }

    public static void assertIterableEquals(Iterable<?> iterable1,
            Iterable<?> iterable2) {
        Iterator<?> i1 = iterable1.iterator();
        Iterator<?> i2 = iterable2.iterator();

        while (i1.hasNext()) {
            Object o1 = i1.next();
            if (!i2.hasNext()) {
                Assert.fail(
                        "The second iterable contains fewer items than the first. The object "
                                + o1 + " has no match in the second iterable.");
            }
            Object o2 = i2.next();
            Assert.assertEquals(o1, o2);
        }
        if (i2.hasNext()) {
            Assert.fail(
                    "The second iterable contains more items than the first. The object "
                            + i2.next()
                            + " has no match in the first iterable.");
        }
    }

    /**
     * Checks whether a weak reference is garbage collected. This methods also
     * tries to force collection of the reference by doing a few iterations of
     * {@link System#gc()}.
     *
     * @param ref
     *            the weak reference to check
     * @return <code>true</code> if the reference has been collected,
     *         <code>false</code> if the reference is still reachable
     * @throws InterruptedException
     *             if interrupted while waiting for garbage collection to finish
     */
    public static boolean isGarbageCollected(WeakReference<?> ref)
            throws InterruptedException {
        for (int i = 0; i < 5; i++) {
            System.gc();
            if (ref.get() == null) {
                return true;
            }
        }
        return false;
    }

    public static void createIndexHtmlStub(File projectRootFolder)
            throws IOException {
        createStubFileInFrontend(projectRootFolder, INDEX_HTML);
    }

    public static void createWebComponentHtmlStub(File projectRootFolder)
            throws IOException {
        createStubFileInFrontend(projectRootFolder, WEB_COMPONENT_HTML);
    }

    public static void createStubFileInFrontend(File projectRootFolder,
            String stubFileName) throws IOException {
        try (InputStream indexStream = IndexHtmlRequestHandlerTest.class
                .getClassLoader()
                .getResourceAsStream("frontend/" + stubFileName)) {
            String indexHtmlContent = IOUtils
                    .toString(Objects.requireNonNull(indexStream), UTF_8);
            File indexHtml = new File(
                    new File(projectRootFolder, DEFAULT_FRONTEND_DIR),
                    stubFileName);
            FileUtils.forceMkdirParent(indexHtml);
            FileUtils.writeStringToFile(indexHtml, indexHtmlContent, UTF_8);
        }
    }

    public static void createStatsJsonStub(File projectRootFolder)
            throws IOException {
        String content = "{\"npmModules\": {}, "
                + "\"entryScripts\": [\"foo.js\"], "
                + "\"packageJsonHash\": \"42\","
                + "\"indexHtmlGenerated\": []}";
        createStubFile(projectRootFolder, "target/"
                + Constants.DEV_BUNDLE_LOCATION + "/config/stats.json",
                content);
    }

    public static void createStylesCssStubInBundle(File projectRootFolder,
            String themeName, String content) throws IOException {
        createStubFile(
                projectRootFolder, "target/" + Constants.DEV_BUNDLE_LOCATION
                        + "/assets/themes/" + themeName + "/styles.css",
                content);
    }

    public static void createThemeJs(File projectRootFolder)
            throws IOException {
        String content = "import {applyTheme as _applyTheme} from './theme-my-theme.generated.js';"
                + "export const applyTheme = _applyTheme;";
        createStubFile(projectRootFolder, FrontendUtils.DEFAULT_FRONTEND_DIR
                + "generated/" + THEME_IMPORTS_NAME, content);
    }

    public static void createStyleCssStubInFrontend(File projectRootFolder,
            String themeName, String content) throws IOException {
        createStubFile(projectRootFolder, FrontendUtils.DEFAULT_FRONTEND_DIR
                + "themes/" + themeName + "/styles.css", content);
    }

    public static void createStubFile(File projectRootFolder,
            String relativePath, String content) throws IOException {
        File stub = new File(projectRootFolder, relativePath);
        FileUtils.forceMkdirParent(stub);
        FileUtils.writeStringToFile(stub, content, UTF_8);
    }
}
