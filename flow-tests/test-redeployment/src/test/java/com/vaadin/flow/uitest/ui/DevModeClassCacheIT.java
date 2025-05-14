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
package com.vaadin.flow.uitest.ui;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

import net.jcip.annotations.NotThreadSafe;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;

import com.vaadin.testbench.TestBenchElement;

@NotThreadSafe
public class DevModeClassCacheIT extends AbstractReloadIT {

    private static final Path VIEW_PATH = Path.of("com", "vaadin", "flow",
            "uitest", "ui", "reloadaddedviews");

    private static final String NEW_CLASS_WITH_ROUTE = """
            package com.vaadin.flow.uitest.ui.reloadaddedviews;
            import com.vaadin.flow.component.html.Div;
            import com.vaadin.flow.router.Route;
            @Route("compiled-at-runtime")
            public class MyView extends Div {}
            """;

    private static final String NEW_CLASS_WITHOUT_ROUTE = """
            package com.vaadin.flow.uitest.ui.reloadaddedviews;
            import com.vaadin.flow.component.html.Div;
            public class MyView extends Div {}
            """;

    @Override
    protected String getTestPath() {
        return super.getTestPath().replace("/view", "");
    }

    @Before
    @After
    public void removeTemporaryView() throws IOException {
        Path baseDir = new File(System.getProperty("user.dir", ".")).toPath();
        FileUtils.deleteDirectory(baseDir.resolve(Path.of("target", "classes"))
                .resolve(VIEW_PATH).toFile());
        FileUtils
                .deleteDirectory(baseDir.resolve(Path.of("src", "main", "java"))
                        .resolve(VIEW_PATH).toFile());
    }

    @Test
    public void testDevModeClassCachePopulated() {
        open();

        waitForElementPresent(By.id("last-span"));

        reloadAndWait();

        waitForElementPresent(By.id("last-span"));

        List<TestBenchElement> allSpans = $("span").all();

        for (int i = 1; i < 5; i++) {
            String[] value = allSpans.get(i).getText().split(":");
            Assert.assertTrue("Expected " + value[0] + " to be greater than 0.",
                    Integer.parseInt(value[1]) > 0);
        }

        Assert.assertEquals("Unexpected cached route packages.",
                "com.vaadin.flow.uitest.ui",
                allSpans.get(5).getText().split(":")[1]);

        // Ensure newly created classes in packages not previously used for
        // routes are correctly added to the cache
        createOrUpdateViewReloadAndWait(true);

        waitForElementPresent(By.id("last-span"));

        allSpans = $("span").all();

        for (int i = 1; i < 5; i++) {
            String[] value = allSpans.get(i).getText().split(":");
            Assert.assertTrue("Expected " + value[0] + " to be greater than 0.",
                    Integer.parseInt(value[1]) > 0);
        }

        Assert.assertEquals("Unexpected cached route packages.",
                "com.vaadin.flow.uitest.ui,com.vaadin.flow.uitest.ui.reloadaddedviews",
                allSpans.get(5).getText().split(":")[1]);

        // Modify the class to remove Route annotation and ensure the package is
        // removed from route packages
        createOrUpdateViewReloadAndWait(false);
        waitForElementPresent(By.id("last-span"));
        allSpans = $("span").all();
        Assert.assertEquals("Unexpected cached route packages.",
                "com.vaadin.flow.uitest.ui",
                allSpans.get(5).getText().split(":")[1]);

        // Modify the class to add Route annotation and ensure the package is
        // once again added to route packages
        createOrUpdateViewReloadAndWait(true);
        waitForElementPresent(By.id("last-span"));
        allSpans = $("span").all();
        Assert.assertEquals("Unexpected cached route packages.",
                "com.vaadin.flow.uitest.ui,com.vaadin.flow.uitest.ui.reloadaddedviews",
                allSpans.get(5).getText().split(":")[1]);
    }

    // create or modify class on the fly
    protected void createOrUpdateViewReloadAndWait(boolean withRoute) {
        String viewId = getViewId();

        Path baseDir = new File(System.getProperty("user.dir", ".")).toPath();
        Path outputPath = baseDir.resolve(Path.of("target", "classes"));
        Path sourcePath = baseDir.resolve(Path.of("src", "main", "java"))
                .resolve(VIEW_PATH);
        Path sourceFile = sourcePath.resolve("MyView.java");
        try {
            Files.createDirectories(sourcePath);
            Files.writeString(sourceFile,
                    withRoute ? NEW_CLASS_WITH_ROUTE : NEW_CLASS_WITHOUT_ROUTE,
                    StandardOpenOption.WRITE, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        int result = compiler.run(null, null, null, "-d", outputPath.toString(),
                "-sourcepath", sourcePath.toString(), sourceFile.toString());
        Assert.assertEquals("Failed to compile " + sourceFile, 0, result);

        waitUntil(driver -> {
            try {
                return !getViewId().equals(viewId);
            } catch (StaleElementReferenceException e) {
                return false;
            }
        });
    }
}
