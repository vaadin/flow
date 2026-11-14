/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.viteapp;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.junit.Before;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

import com.vaadin.flow.testutil.ChromeBrowserTest;

abstract public class ViteDevModeIT extends ChromeBrowserTest {

    @Before
    public void openView() {
        getDriver().get(getRootURL());
        waitForDevServer();
        getCommandExecutor().waitForVaadin();
        waitUntil(ExpectedConditions
                .presenceOfElementLocated(By.id("loadAndShowJson")), 300);
    }

    /**
     * Returns the url the dev server serves the given file of the project from.
     *
     * @param fileInProject
     *            the path of the file, relative to the project folder
     * @return the url of the file on the dev server
     * @throws IOException
     *             if the project folder can not be resolved
     */
    protected URL getFsUrl(String fileInProject) throws IOException {
        // For Windows, the URLs should be like
        // http://localhost:8888/VAADIN/@fs/C:/Code/flow/flow-tests/test-frontend/vite-basics/target/vaadin-dev-server-settings.json

        String currentPath = new File(".").getCanonicalPath().replace("\\",
                "/");
        if (!currentPath.startsWith("/")) {
            currentPath = "/" + currentPath;
        }
        if (currentPath.endsWith("/")) {
            currentPath = currentPath.substring(0, currentPath.length() - 1);
        }
        return new URL(getRootURL() + "/VAADIN/@fs" + currentPath + "/"
                + fileInProject);
    }
}
