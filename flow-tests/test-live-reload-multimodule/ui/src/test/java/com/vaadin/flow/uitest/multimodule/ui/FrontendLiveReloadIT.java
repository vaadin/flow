/*
 * Copyright 2000-2023 Vaadin Ltd.
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
package com.vaadin.flow.uitest.multimodule.ui;

import javax.annotation.concurrent.NotThreadSafe;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.StaleElementReferenceException;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.testutil.ChromeBrowserTest;

@NotThreadSafe
public class FrontendLiveReloadIT extends ChromeBrowserTest {

    private Set<Runnable> cleanup = new HashSet<>();

    @After
    public void cleanup() {
        for (Runnable r : cleanup) {
            try {
                r.run();
            } catch (Exception e) {
                LoggerFactory.getLogger(getClass()).error("Error cleaning up",
                        e);
            }
        }
    }

    @Test
    public void modifyMetaInfFrontendFile() throws IOException {
        open();

        String uiModuleFolder = $("project-folder-info").first().getText();
        File libraryFolder = new File(new File(uiModuleFolder).getParentFile(),
                "library");
        Path metainfFrontendFolder = Path.of("src", "main", "resources",
                "META-INF", "frontend", "in-frontend.js");
        Path metainfFrontendFile = libraryFolder.toPath()
                .resolve(metainfFrontendFolder);

        revertJsFileIfNeeded(metainfFrontendFile);
        this.cleanup.add(() -> revertJsFileIfNeeded(metainfFrontendFile));

        waitUntilEquals("This is the component from META-INF/frontend",
                () -> $("in-frontend").first().getText());

        modifyJsFile(metainfFrontendFile);

        waitUntilEquals(
                "This is the component from META-INF/frontend. It was modified",
                () -> $("in-frontend").first().getText());
    }

    @Test
    public void modifyMetaInfResourcesFrontendFile() throws IOException {
        open();

        String uiModuleFolder = $("project-folder-info").first().getText();
        File libraryFolder = new File(new File(uiModuleFolder).getParentFile(),
                "library");
        Path metainfResourcesFrontendFolder = Path.of("src", "main",
                "resources", "META-INF", "resources", "frontend",
                "in-resources-frontend.js");
        Path metainfResourcesFrontendFile = libraryFolder.toPath()
                .resolve(metainfResourcesFrontendFolder);

        revertJsFileIfNeeded(metainfResourcesFrontendFile);
        this.cleanup
                .add(() -> revertJsFileIfNeeded(metainfResourcesFrontendFile));

        waitUntilEquals(
                "This is the component from META-INF/resources/frontend",
                () -> $("in-resources-frontend").first().getText());

        modifyJsFile(metainfResourcesFrontendFile);

        waitUntilEquals(
                "This is the component from META-INF/resources/frontend. It was modified",
                () -> $("in-resources-frontend").first().getText());
    }

    private void waitUntilEquals(String expected, Supplier<String> supplier) {
        waitUntil(driver -> {
            try {
                return expected.equals(supplier.get());
            } catch (StaleElementReferenceException e) {
                return false;
            }
        });
    }

    private void modifyJsFile(Path path) throws UncheckedIOException {
        modify(path.toFile(), "</span>", ". It was modified</span>", true);
    }

    private void revertJsFileIfNeeded(Path path) throws UncheckedIOException {
        modify(path.toFile(), ". It was modified</span>", "</span>", false);
    }

    private void modify(File file, String from, String to,
            boolean failIfNotModified) throws UncheckedIOException {
        try {
            String content = FileUtils.readFileToString(file,
                    StandardCharsets.UTF_8);
            String newContent = content.replace(from, to);
            if (failIfNotModified) {
                Assert.assertNotEquals("Failed to update content", content,
                        newContent);
            }
            FileUtils.write(file, newContent, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

    }

}
